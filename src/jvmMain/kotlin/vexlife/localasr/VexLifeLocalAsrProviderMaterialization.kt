package vexlife.localasr

import java.io.BufferedInputStream
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipInputStream
import vexlife.artifact.DirectHttpsArtifactDelivery
import vexlife.filesystem.VexOwnedPath

/**
 * Native Local-ASR provider artifact owner.
 *
 * The model artifact and the provider runtime remain separate. This owner may only materialize the
 * exact native provider image declared by an already-selected platform realization.
 */
internal object VexLifeLocalAsrProviderMaterialization {
    enum class State {
        ABSENT,
        VERIFIED,
        INVALID,
    }

    data class Observation(
        val realizationRef: String,
        val providerRootPath: String,
        val executablePath: String,
        val sourceIdentityPath: String,
        val sourceSha: String?,
        val archivePath: String?,
        val state: State,
        val effectsPerformed: Boolean,
    ) {
        val ready: Boolean
            get() =
                state ==
                    State.VERIFIED
    }

    data class Result(
        val observation: Observation,
        val archiveDisposition: String?,
        val extractionPerformed: Boolean,
    ) {
        init {
            require(
                observation.ready,
            ) {
                "local ASR provider materialization result requires exact ready state"
            }
        }
    }

    fun observe(
        productHome: Path,
        realization: VexLifeLocalAsrProviderRealization.Value =
            VexLifeLocalAsrProviderRealization.current(),
    ): Observation {
        val home =
            productHome
                .toAbsolutePath()
                .normalize()
        val providerRoot =
            VexOwnedPath.resolveInside(
                home,
                realization.providerRootRelativePath,
            )
        val executable =
            providerRoot
                .resolve(
                    realization.executableRelativePath,
                )
                .normalize()
        val sourceIdentity =
            providerRoot
                .resolve(
                    realization.sourceIdentityRelativePath,
                )
                .normalize()
        require(
            executable.startsWith(
                providerRoot,
            ) &&
                sourceIdentity.startsWith(
                    providerRoot,
                ),
        ) {
            "local ASR provider realization escaped its provider root"
        }

        val sourceSha =
            if (
                Files.isRegularFile(
                    sourceIdentity,
                    LinkOption.NOFOLLOW_LINKS,
                ) &&
                !Files.isSymbolicLink(
                    sourceIdentity,
                )
            ) {
                runCatching {
                        Files.readString(
                            sourceIdentity,
                            StandardCharsets.UTF_8,
                        )
                            .trim()
                    }
                    .getOrNull()
            } else {
                null
            }

        val archivePath =
            realization.windowsArchive
                ?.let {
                    archive ->
                    providerRoot
                        .resolve(
                            "archive",
                        )
                        .resolve(
                            archive.fileName,
                        )
                        .normalize()
                }

        val providerRootInvalid =
            Files.exists(
                providerRoot,
                LinkOption.NOFOLLOW_LINKS,
            ) &&
                (
                    !Files.isDirectory(
                        providerRoot,
                        LinkOption.NOFOLLOW_LINKS,
                    ) ||
                        Files.isSymbolicLink(
                            providerRoot,
                        )
                )
        val sourceIdentityExists =
            Files.exists(
                sourceIdentity,
                LinkOption.NOFOLLOW_LINKS,
            )
        val executableExists =
            Files.exists(
                executable,
                LinkOption.NOFOLLOW_LINKS,
            )

        val sourceExact =
            sourceSha ==
                VexLifeLocalAsrProviderRealization.ProviderSourceSha
        val executableCurrent =
            Files.isRegularFile(
                executable,
                LinkOption.NOFOLLOW_LINKS,
            ) &&
                !Files.isSymbolicLink(
                    executable,
                ) &&
                (
                    realization !=
                        VexLifeLocalAsrProviderRealization.MacArm64 ||
                        Files.isExecutable(
                            executable,
                        )
                )

        val archiveCurrent =
            realization.windowsArchive
                ?.let {
                    archive ->
                    archivePath !=
                        null &&
                        observeFile(
                            archivePath,
                            expectedBytes =
                                archive.bytes,
                            expectedSha256 =
                                archive.sha256,
                        )
                }
                ?: true

        val state =
            when {
                sourceExact &&
                    executableCurrent &&
                    archiveCurrent ->
                    State.VERIFIED

                providerRootInvalid ||
                    (
                        sourceIdentityExists &&
                            !sourceExact
                    ) ||
                    (
                        executableExists &&
                            !executableCurrent
                    ) ->
                    State.INVALID

                else ->
                    // Archive directories / resumable .partial files are recoverable acquisition
                    // state, not a competing provider realization.
                    State.ABSENT
            }

        return Observation(
            realizationRef =
                realization.realizationRef,
            providerRootPath =
                providerRoot.toString(),
            executablePath =
                executable.toString(),
            sourceIdentityPath =
                sourceIdentity.toString(),
            sourceSha =
                sourceSha,
            archivePath =
                archivePath?.toString(),
            state =
                state,
            effectsPerformed =
                false,
        )
    }

    fun ensure(
        productHome: Path,
        realization: VexLifeLocalAsrProviderRealization.Value =
            VexLifeLocalAsrProviderRealization.current(),
        transport: DirectHttpsArtifactDelivery.Transport =
            DirectHttpsArtifactDelivery.JdkTransport,
        allowRepair: Boolean = false,
    ): Result {
        var before =
            observe(
                productHome =
                    productHome,
                realization =
                    realization,
            )
        if (
            before.state ==
                State.INVALID
        ) {
            require(
                allowRepair,
            ) {
                "existing local ASR provider image is not the exact declared realization; explicit repair is required"
            }
            repairInvalidProvider(
                productHome =
                    productHome,
                realization =
                    realization,
                observation =
                    before,
            )
            before =
                observe(
                    productHome =
                        productHome,
                    realization =
                        realization,
                )
        }
        if (
            before.ready
        ) {
            return Result(
                observation =
                    before,
                archiveDisposition =
                    null,
                extractionPerformed =
                    false,
            )
        }

        val archive =
            requireNotNull(
                realization.windowsArchive,
            ) {
                "automatic local ASR provider acquisition is not available for " +
                    realization.realizationRef
            }
        val home =
            productHome
                .toAbsolutePath()
                .normalize()
        val providerRoot =
            Path.of(
                before.providerRootPath,
            )
        VexOwnedPath.ensureDirectory(
            home,
            providerRoot,
        )
        val archivePath =
            requireNotNull(
                before.archivePath,
            )
                .let(
                    Path::of,
                )
        VexOwnedPath.ensureParent(
            home,
            archivePath,
        )

        val archiveDisposition =
            if (
                Files.isRegularFile(
                    archivePath,
                    LinkOption.NOFOLLOW_LINKS,
                ) &&
                observeFile(
                    archivePath,
                    expectedBytes =
                        archive.bytes,
                    expectedSha256 =
                        archive.sha256,
                )
            ) {
                "REUSED_VERIFIED_PROVIDER_ARCHIVE"
            } else {
                Files.deleteIfExists(
                    archivePath,
                )
                DirectHttpsArtifactDelivery.download(
                    request =
                        DirectHttpsArtifactDelivery.Request(
                            url =
                                archive.url,
                            expectedSha256 =
                                archive.sha256,
                            expectedBytes =
                                archive.bytes,
                        ),
                    finalPath =
                        archivePath,
                    transport =
                        transport,
                )
                    .disposition
            }

        val imagePath =
            providerRoot
                .resolve(
                    "image",
                )
                .normalize()
        require(
            !Files.exists(
                imagePath,
                LinkOption.NOFOLLOW_LINKS,
            ),
        ) {
            "local ASR provider image already exists but is incomplete; explicit repair is required"
        }

        extractWindowsArchive(
            home =
                home,
            archivePath =
                archivePath,
            imagePath =
                imagePath,
            archive =
                archive,
        )

        val sourceIdentity =
            Path.of(
                before.sourceIdentityPath,
            )
        VexOwnedPath.ensureParent(
            home,
            sourceIdentity,
        )
        val stagingSource =
            sourceIdentity.resolveSibling(
                sourceIdentity.fileName.toString() +
                    ".staging-" +
                    UUID.randomUUID(),
            )
        Files.writeString(
            stagingSource,
            VexLifeLocalAsrProviderRealization.ProviderSourceSha +
                "\n",
            StandardCharsets.UTF_8,
        )
        try {
            Files.move(
                stagingSource,
                sourceIdentity,
                StandardCopyOption.ATOMIC_MOVE,
            )
        } catch (
            _: AtomicMoveNotSupportedException,
        ) {
            Files.deleteIfExists(
                stagingSource,
            )
            error(
                "atomic local ASR provider-source publication is unavailable",
            )
        }

        val after =
            observe(
                productHome =
                    home,
                realization =
                    realization,
            )
        require(
            after.ready,
        ) {
            "local ASR provider materialization did not converge to exact ready state"
        }

        return Result(
            observation =
                after.copy(
                    effectsPerformed =
                        true,
                ),
            archiveDisposition =
                archiveDisposition,
            extractionPerformed =
                true,
        )
    }

    private fun repairInvalidProvider(
        productHome: Path,
        realization: VexLifeLocalAsrProviderRealization.Value,
        observation: Observation,
    ) {
        require(
            realization.windowsArchive !=
                null,
        ) {
            "automatic local ASR provider repair is unavailable on this platform"
        }
        val home =
            productHome
                .toAbsolutePath()
                .normalize()
        val providerRoot =
            Path.of(
                observation.providerRootPath,
            )
        require(
            providerRoot.startsWith(
                home,
            ),
        ) {
            "local ASR provider repair escaped Vex Home"
        }

        val imagePath =
            providerRoot
                .resolve(
                    "image",
                )
                .normalize()
        if (
            Files.exists(
                imagePath,
                LinkOption.NOFOLLOW_LINKS,
            )
        ) {
            require(
                Files.isDirectory(
                    imagePath,
                    LinkOption.NOFOLLOW_LINKS,
                ) &&
                    !Files.isSymbolicLink(
                        imagePath,
                    ),
            ) {
                "local ASR provider repair refuses non-directory image path"
            }
            deleteTreeNoFollow(
                imagePath,
            )
        }

        val sourceIdentity =
            Path.of(
                observation.sourceIdentityPath,
            )
        if (
            Files.exists(
                sourceIdentity,
                LinkOption.NOFOLLOW_LINKS,
            )
        ) {
            require(
                Files.isRegularFile(
                    sourceIdentity,
                    LinkOption.NOFOLLOW_LINKS,
                ) &&
                    !Files.isSymbolicLink(
                        sourceIdentity,
                    ),
            ) {
                "local ASR provider repair refuses non-regular source identity"
            }
            Files.delete(
                sourceIdentity,
            )
        }

        observation.archivePath
            ?.let(
                Path::of,
            )
            ?.takeIf {
                Files.exists(
                    it,
                    LinkOption.NOFOLLOW_LINKS,
                ) &&
                    !observeFile(
                        it,
                        expectedBytes =
                            requireNotNull(
                                realization.windowsArchive,
                            ).bytes,
                        expectedSha256 =
                            realization.windowsArchive.sha256,
                    )
            }
            ?.let {
                invalidArchive ->
                require(
                    Files.isRegularFile(
                        invalidArchive,
                        LinkOption.NOFOLLOW_LINKS,
                    ) &&
                        !Files.isSymbolicLink(
                            invalidArchive,
                        ),
                ) {
                    "local ASR provider repair refuses non-regular archive path"
                }
                Files.delete(
                    invalidArchive,
                )
            }
    }

    private fun extractWindowsArchive(
        home: Path,
        archivePath: Path,
        imagePath: Path,
        archive: VexLifeLocalAsrProviderRealization.WindowsArchive,
    ) {
        val staging =
            imagePath.resolveSibling(
                imagePath.fileName.toString() +
                    ".staging-" +
                    UUID.randomUUID(),
            )
        VexOwnedPath.ensureDirectory(
            home,
            requireNotNull(
                staging.parent,
            ),
        )
        Files.createDirectory(
            staging,
        )

        try {
            val seen =
                mutableSetOf<String>()

            ZipInputStream(
                    BufferedInputStream(
                        Files.newInputStream(
                            archivePath,
                        ),
                    ),
                )
                .use {
                    zip ->
                    while (
                        true
                    ) {
                        val entry =
                            zip.nextEntry
                                ?: break
                        val normalizedName =
                            normalizedArchiveEntry(
                                entry.name,
                            )
                        require(
                            seen.add(
                                normalizedName.lowercase(
                                    Locale.ROOT,
                                ),
                            ),
                        ) {
                            "local ASR provider archive contains duplicate Windows path " +
                                entry.name
                        }
                        val destination =
                            staging
                                .resolve(
                                    normalizedName,
                                )
                                .normalize()
                        require(
                            destination.startsWith(
                                staging,
                            ),
                        ) {
                            "local ASR provider archive escaped staging root"
                        }
                        if (
                            entry.isDirectory
                        ) {
                            VexOwnedPath.ensureDirectory(
                                home,
                                destination,
                            )
                        } else {
                            VexOwnedPath.ensureParent(
                                home,
                                destination,
                            )
                            Files.newOutputStream(
                                    destination,
                                )
                                .use {
                                    output ->
                                    zip.copyTo(
                                        output,
                                    )
                                }
                        }
                        zip.closeEntry()
                    }
                }

            val expectedExecutable =
                staging
                    .resolve(
                        archive.extractedRootRelativePath,
                    )
                    .resolve(
                        "whisper-server.exe",
                    )
                    .normalize()
            require(
                expectedExecutable.startsWith(
                    staging,
                ) &&
                    Files.isRegularFile(
                        expectedExecutable,
                        LinkOption.NOFOLLOW_LINKS,
                    ) &&
                    !Files.isSymbolicLink(
                        expectedExecutable,
                    ),
            ) {
                "exact Windows whisper.cpp provider archive has no whisper-server.exe"
            }

            try {
                Files.move(
                    staging,
                    imagePath,
                    StandardCopyOption.ATOMIC_MOVE,
                )
            } catch (
                _: AtomicMoveNotSupportedException,
            ) {
                error(
                    "atomic local ASR provider-image publication is unavailable",
                )
            }
        } finally {
            if (
                Files.exists(
                    staging,
                    LinkOption.NOFOLLOW_LINKS,
                )
            ) {
                deleteTreeNoFollow(
                    staging,
                )
            }
        }
    }

    private fun normalizedArchiveEntry(
        raw: String,
    ): String {
        require(
            raw.isNotBlank() &&
                !raw.startsWith(
                    "/",
                ) &&
                !raw.startsWith(
                    "\\",
                ) &&
                !Regex(
                        "^[A-Za-z]:",
                    )
                    .containsMatchIn(
                        raw,
                    ),
        ) {
            "local ASR provider archive path is absolute"
        }
        val normalized =
            raw.replace(
                '\\',
                '/',
            )
        val segments =
            normalized
                .split(
                    '/',
                )
                .filter(
                    String::isNotEmpty,
                )
        require(
            segments.isNotEmpty() &&
                segments.none {
                    it ==
                        "." ||
                        it ==
                        ".."
                },
        ) {
            "local ASR provider archive path is not portable"
        }
        return segments.joinToString(
            "/",
        )
    }

    private fun observeFile(
        path: Path,
        expectedBytes: Long,
        expectedSha256: String,
    ): Boolean {
        if (
            !Files.isRegularFile(
                path,
                LinkOption.NOFOLLOW_LINKS,
            ) ||
            Files.isSymbolicLink(
                path,
            ) ||
            Files.size(
                path,
            ) !=
                expectedBytes
        ) {
            return false
        }
        return sha256(
            path,
        ) ==
            expectedSha256
    }

    private fun sha256(
        path: Path,
    ): String {
        val digest =
            MessageDigest.getInstance(
                "SHA-256",
            )
        Files.newInputStream(
                path,
            )
            .use {
                input ->
                val buffer =
                    ByteArray(
                        1024 *
                            1024,
                    )
                while (
                    true
                ) {
                    val read =
                        input.read(
                            buffer,
                        )
                    if (
                        read <
                        0
                    ) {
                        break
                    }
                    digest.update(
                        buffer,
                        0,
                        read,
                    )
                }
            }
        return digest
            .digest()
            .joinToString(
                "",
            ) {
                "%02x".format(
                    it.toInt() and
                        0xff,
                )
            }
    }

    private fun deleteTreeNoFollow(
        root: Path,
    ) {
        Files.walk(
                root,
            )
            .use {
                paths ->
                paths
                    .sorted(
                        Comparator.reverseOrder(),
                    )
                    .forEach {
                        path ->
                        Files.deleteIfExists(
                            path,
                        )
                    }
            }
    }
}

// [VXG RealForever]
