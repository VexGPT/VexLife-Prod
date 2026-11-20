package vexlife.localasr

import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.UUID
import vexlife.filesystem.VexOwnedPath
import vexlife.model.heretic.HereticRuntimeRealization

/**
 * macOS / Apple-silicon whisper.cpp provider mechanics.
 *
 * The release/private service image carries an exact offline provider seed. Runtime custody is a
 * stable Vex Home coordinate so Product source updates do not manufacture a new provider location
 * merely because the application source SHA changed.
 *
 * This intentionally preserves both earned Mac relationships:
 *
 * - fresh/offline installation can provision from the exact release-owned seed;
 * - an already-current Home provider is reused across Product updates.
 *
 * The native process inherits the Product-service working directory and receives only
 * DYLD_LIBRARY_PATH=provider/bin. Do not adopt Windows's executable-parent cwd; Windows needs that
 * for its released DLL image and Mac broke when that rule leaked into shared mechanics.
 *
 * Distribution trust remains host-owned. Provisioning copies the seed with macOS ditto, preserving
 * metadata/extended attributes rather than stripping quarantine. Stable custody reduces needless
 * re-presentation across Product updates; it does not bypass Gatekeeper or claim notarization.
 */
internal object VexLifeLocalAsrMacArm64ProviderRealization :
    VexLifeLocalAsrWhisperCppProcessRealization() {

    const val PrivateServiceImageRootProperty =
        "vexlife.privateServiceImageRoot"

    const val BundledProviderRootRelativePath =
        "local-asr-provider/whispercpp/v1.9.5"

    const val StableProviderRootRelativePath =
        VexLifeLocalAsrWhisperCppProcessRealization.ProviderBaseRootRelativePath

    override val realizationRef: String =
        "realization.vexlife-prod.local-asr.whispercpp.macos-arm64.v1.9.5"

    override val target =
        HereticRuntimeRealization.MacArm64

    override val providerRootRelativePath: String =
        StableProviderRootRelativePath

    override val executableRelativePath: String =
        "bin/whisper-server"

    override val sourceIdentityRelativePath: String =
        "provider-source.txt"

    override val acquisition:
        VexLifeLocalAsrWhisperCppProcessRealization.ArchiveAcquisition? =
        null

    override fun observeProvider(
        productHome: Path,
    ): VexLifeLocalAsrProviderMaterialization.Observation =
        VexLifeLocalAsrProviderMaterialization.observe(
            productHome =
                productHome,
            realization =
                this,
        )

    internal fun observeBundledProvider(
        privateServiceImageRoot: Path?,
    ): VexLifeLocalAsrProviderMaterialization.Observation {
        if (
            privateServiceImageRoot ==
                null
        ) {
            return VexLifeLocalAsrProviderMaterialization.Observation(
                realizationRef =
                    realizationRef,
                providerRootPath =
                    "",
                executablePath =
                    "",
                sourceIdentityPath =
                    "",
                sourceSha =
                    null,
                archivePath =
                    null,
                state =
                    VexLifeLocalAsrProviderMaterialization.State.ABSENT,
                effectsPerformed =
                    false,
            )
        }

        val imageRoot =
            privateServiceImageRoot
                .toAbsolutePath()
                .normalize()
        val providerRoot =
            imageRoot
                .resolve(
                    BundledProviderRootRelativePath,
                )
                .normalize()
        val executable =
            providerRoot
                .resolve(
                    executableRelativePath,
                )
                .normalize()
        val sourceIdentity =
            providerRoot
                .resolve(
                    sourceIdentityRelativePath,
                )
                .normalize()

        require(
            providerRoot.startsWith(
                imageRoot,
            ) &&
                executable.startsWith(
                    providerRoot,
                ) &&
                sourceIdentity.startsWith(
                    providerRoot,
                ),
        ) {
            "bundled Mac local ASR provider escaped the private service image"
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

        val executableExists =
            Files.exists(
                executable,
                LinkOption.NOFOLLOW_LINKS,
            )
        val sourceExists =
            Files.exists(
                sourceIdentity,
                LinkOption.NOFOLLOW_LINKS,
            )
        val sourceCurrent =
            sourceSha ==
                providerSourceRef
        val executableCurrent =
            executableCurrent(
                executable,
            )

        val state =
            when {
                sourceCurrent &&
                    executableCurrent ->
                    VexLifeLocalAsrProviderMaterialization.State.VERIFIED

                !sourceExists &&
                    !executableExists ->
                    VexLifeLocalAsrProviderMaterialization.State.ABSENT

                else ->
                    VexLifeLocalAsrProviderMaterialization.State.INVALID
            }

        return VexLifeLocalAsrProviderMaterialization.Observation(
            realizationRef =
                realizationRef,
            providerRootPath =
                providerRoot.toString(),
            executablePath =
                executable.toString(),
            sourceIdentityPath =
                sourceIdentity.toString(),
            sourceSha =
                sourceSha,
            archivePath =
                null,
            state =
                state,
            effectsPerformed =
                false,
        )
    }

    override fun ensureProvider(
        productHome: Path,
        allowRepair: Boolean,
    ): VexLifeLocalAsrProviderMaterialization.Result {
        val observed =
            observeProvider(
                productHome,
            )
        require(
            observed.ready,
        ) {
            "Mac local ASR provider is not current in stable Vex Home custody; Product setup must converge the release-owned seed first"
        }
        return VexLifeLocalAsrProviderMaterialization.Result(
            observation =
                observed,
            archiveDisposition =
                null,
            extractionPerformed =
                false,
        )
    }

    /**
     * Converge the exact release-owned Mac provider seed into stable Home runtime custody.
     *
     * This is setup/install work, not provider observation. The stable runtime is reused when exact;
     * an invalid owned runtime is replaced only when explicit repair authority is supplied.
     */
    internal fun convergeBundledProvider(
        productHome: Path,
        privateServiceImageRoot: Path,
        allowRepair: Boolean = false,
    ): VexLifeLocalAsrProviderMaterialization.Result {
        val home =
            VexOwnedPath.requireRoot(
                productHome
                    .toAbsolutePath()
                    .normalize(),
                "VexLife Home",
            )
        var before =
            observeProvider(
                home,
            )

        if (
            before.ready
        ) {
            return VexLifeLocalAsrProviderMaterialization.Result(
                observation =
                    before,
                archiveDisposition =
                    "REUSED_VERIFIED_STABLE_MAC_PROVIDER",
                extractionPerformed =
                    false,
            )
        }

        if (
            before.state ==
                VexLifeLocalAsrProviderMaterialization.State.INVALID
        ) {
            require(
                allowRepair,
            ) {
                "stable Mac local ASR provider is invalid; explicit repair authority is required"
            }
            deleteOwnedProviderRoot(
                home =
                    home,
                providerRoot =
                    Path.of(
                        before.providerRootPath,
                    ),
            )
            before =
                observeProvider(
                    home,
                )
        }

        require(
            before.state ==
                VexLifeLocalAsrProviderMaterialization.State.ABSENT
        ) {
            "stable Mac local ASR provider did not reach an installable state"
        }

        val seed =
            observeBundledProvider(
                privateServiceImageRoot =
                    privateServiceImageRoot,
            )
        require(
            seed.ready,
        ) {
            "release-owned Mac local ASR provider seed is not current"
        }

        val source =
            Path.of(
                seed.providerRootPath,
            )
                .toAbsolutePath()
                .normalize()
        val target =
            Path.of(
                before.providerRootPath,
            )
                .toAbsolutePath()
                .normalize()
        require(
            target.startsWith(
                home,
            ),
        ) {
            "stable Mac local ASR provider escaped Vex Home"
        }

        VexOwnedPath.ensureDirectory(
            home,
            requireNotNull(
                target.parent,
            ),
        )

        val staging =
            target.resolveSibling(
                "." +
                    target.fileName +
                    ".staging-" +
                    UUID.randomUUID(),
            )
        require(
            !Files.exists(
                staging,
                LinkOption.NOFOLLOW_LINKS,
            ),
        ) {
            "stable Mac local ASR provider staging path already exists"
        }

        try {
            copyProviderTreeWithDitto(
                source =
                    source,
                destination =
                    staging,
            )
            try {
                Files.move(
                    staging,
                    target,
                    StandardCopyOption.ATOMIC_MOVE,
                )
            } catch (
                _: AtomicMoveNotSupportedException,
            ) {
                error(
                    "atomic stable Mac local ASR provider publication is unavailable",
                )
            }

            val after =
                observeProvider(
                    home,
                )
            require(
                after.ready,
            ) {
                "stable Mac local ASR provider did not converge to exact ready state"
            }

            return VexLifeLocalAsrProviderMaterialization.Result(
                observation =
                    after.copy(
                        effectsPerformed =
                            true,
                    ),
                archiveDisposition =
                    "COPIED_VERIFIED_BUNDLED_MAC_PROVIDER",
                extractionPerformed =
                    true,
            )
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

    private fun copyProviderTreeWithDitto(
        source: Path,
        destination: Path,
    ) {
        require(
            System.getProperty(
                "os.name",
            ) ==
                "Mac OS X",
        ) {
            "Mac local ASR provider provisioning requires macOS"
        }
        require(
            Files.isDirectory(
                source,
                LinkOption.NOFOLLOW_LINKS,
            ) &&
                !Files.isSymbolicLink(
                    source,
                ),
        ) {
            "Mac local ASR provider seed must be one real directory"
        }

        val process =
            ProcessBuilder(
                "/usr/bin/ditto",
                source.toString(),
                destination.toString(),
            )
                .redirectErrorStream(
                    true,
                )
                .start()
        val output =
            process.inputStream
                .bufferedReader()
                .use {
                    it.readText()
                }
        val status =
            process.waitFor()
        require(
            status ==
                0,
        ) {
            "Mac local ASR provider copy failed: " +
                output
                    .trim()
                    .takeLast(
                        2000,
                    )
        }
    }

    private fun deleteOwnedProviderRoot(
        home: Path,
        providerRoot: Path,
    ) {
        val target =
            providerRoot
                .toAbsolutePath()
                .normalize()
        require(
            target.startsWith(
                home,
            ) &&
                target !=
                home &&
                Files.isDirectory(
                    target,
                    LinkOption.NOFOLLOW_LINKS,
                ) &&
                !Files.isSymbolicLink(
                    target,
                ),
        ) {
            "stable Mac local ASR provider repair refuses an unowned provider root"
        }
        deleteTreeNoFollow(
            target,
        )
    }

    private fun deleteTreeNoFollow(
        root: Path,
    ) {
        Files.walk(
            root,
        )
            .use {
                stream ->
                stream
                    .toList()
                    .sortedByDescending(
                        Path::getNameCount,
                    )
                    .forEach(
                        Files::deleteIfExists,
                    )
            }
    }

    override fun configureProcess(
        processBuilder: ProcessBuilder,
        executable: Path,
    ) {
        val providerBin =
            requireNotNull(
                executable.parent,
            )
                .toAbsolutePath()
                .normalize()

        // Preserve the lived-qualified Mac launch shape: the Product service owns the child cwd.
        // Mac only contributes its native-library lookup path. Windows owns a different cwd rule
        // inside its own realization because its released DLL image requires it.
        processBuilder
            .environment()["DYLD_LIBRARY_PATH"] =
            providerBin.toString()
    }

    override fun executableCurrent(
        executable: Path,
    ): Boolean =
        Files.isRegularFile(
            executable,
            LinkOption.NOFOLLOW_LINKS,
        ) &&
            !Files.isSymbolicLink(
                executable,
            ) &&
            Files.isExecutable(
                executable,
            )
}

// [VXG RealForever]
