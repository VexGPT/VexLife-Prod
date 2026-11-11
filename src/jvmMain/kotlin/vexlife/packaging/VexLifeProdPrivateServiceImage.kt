package vexlife.packaging

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import java.security.MessageDigest
import kotlinx.serialization.Serializable
import vexlife.contract.VexContractJson
import vexlife.filesystem.VexOwnedPath

/**
 * Exact private service-image observation.
 *
 * This owner proves that one already-materialized image matches its source-bound manifest. It does
 * not build, install, start, stop, update, or select the image.
 */
object VexLifeProdPrivateServiceImage {
    const val LegacySchema =
        "vexlife-prod.private-service-image/v3"

    const val Schema =
        "vexlife-prod.private-service-image/v4"

    const val TruthClass =
        "SOURCE_BOUND_PRIVATE_SERVICE_IMAGE"

    const val Platform =
        "darwin-arm64"

    const val JavaRuntimeMajor =
        21

    const val MainClass =
        "vexlife.service.VexLifeProdServiceProcessKt"

    const val LauncherRelativePath =
        "bin/vexlife-prod-service"

    const val SetupLauncherRelativePath =
        "bin/vexlife-prod-setup"

    const val SetupWindowRelativePath =
        "setup/vexlife-prod-setup-window.applescript"

    const val ManifestRelativePath =
        "service-image.json"

    const val VisionRuntimeTruthClass =
        "LOCAL_QUALIFIED_NOT_DISTRIBUTABLE"

    const val VisionRuntimeReleaseDisposition =
        "LOCAL_ONLY_NO_RELEASE"

    const val VisionRuntimeManifestRelativePath =
        "vision-runtime/vision-runtime.json"

    const val VisionRuntimeExecutableRelativePath =
        "vision-runtime/bin/vexlife-vision-runtime"

    const val VisionRuntimeHelperExecutableRelativePath =
        "vision-runtime/helper/VexLifeVisionSystemPickerAgent.app/Contents/MacOS/VexLifeVisionSystemPickerAgent"

    private val GitSha =
        Regex(
            "[0-9a-f]{40}",
        )

    private val Sha256 =
        Regex(
            "[0-9a-f]{64}",
        )

    @Serializable
    data class Artifact(
        val path: String,
        val sha256: String,
        val bytes: Long,
    )

    @Serializable
    data class VisionRuntime(
        val truthClass: String,
        val releaseDisposition: String,
        val sdkSourceCommit: String,
        val sdkSourceTree: String,
        val archiveSha256: String,
        val manifestSha256: String,
        val runtimeExecutable: String,
        val runtimeExecutableSha256: String,
        val helperExecutable: String,
        val helperExecutableSha256: String,
    )

    @Serializable
    data class Manifest(
        val schemaVersion: String,
        val truthClass: String,
        val sourceSha: String,
        val platform: String,
        val javaRuntimeMajor: Int,
        val mainClass: String,
        val launcher: String,
        val setupLauncher: String,
        val setupWindow: String,
        val runtimeModules: List<String>,
        val payloadSha256: String,
        val artifacts: List<Artifact>,
        val visionRuntime: VisionRuntime? = null,
    )

    data class Observation(
        val imageRef: String,
        val sourceSha: String,
        val payloadSha256: String,
        val launcherPath: String,
        val setupLauncherPath: String,
        val setupWindowPath: String,
        val javaPath: String,
        val runtimeModules: List<String>,
        val visionRuntimeExecutablePathOrNull: String?,
        val visionRuntimeHelperExecutablePathOrNull: String?,
        val visionRuntimeTruthClassOrNull: String?,
        val visionRuntimeReleaseDispositionOrNull: String?,
        val visionRuntimeSdkSourceCommitOrNull: String?,
        val effectsPerformed: Boolean,
    )

    fun observe(
        imageRoot: Path,
        expectedSourceSha: String,
    ): Observation {
        require(
            GitSha.matches(
                expectedSourceSha,
            ),
        ) {
            "expected service-image source SHA must be one exact Git commit"
        }

        val root =
            VexOwnedPath.requireRoot(
                imageRoot,
                "VexLife-Prod private service image",
            )

        val manifestPath =
            VexOwnedPath.resolveInside(
                root,
                ManifestRelativePath,
            )

        requireRegularFile(
            root = root,
            path = manifestPath,
            label = "private service-image manifest",
        )

        val manifest =
            VexContractJson.decode<Manifest>(
                Files.readString(
                    manifestPath,
                    StandardCharsets.UTF_8,
                ),
            )

        validateManifest(
            manifest,
        )

        require(
            manifest.sourceSha ==
                expectedSourceSha,
        ) {
            "private service-image source identity is not the exact expected source"
        }

        val launcher =
            VexOwnedPath.resolveInside(
                root,
                manifest.launcher,
            )
        requireRegularFile(
            root = root,
            path = launcher,
            label = "private service-image launcher",
        )
        require(
            Files.isExecutable(
                launcher,
            ),
        ) {
            "private service-image launcher must be executable"
        }

        val setupLauncher =
            VexOwnedPath.resolveInside(
                root,
                manifest.setupLauncher,
            )
        requireRegularFile(
            root = root,
            path = setupLauncher,
            label = "private service-image setup launcher",
        )
        require(
            Files.isExecutable(
                setupLauncher,
            ),
        ) {
            "private service-image setup launcher must be executable"
        }

        val setupWindow =
            VexOwnedPath.resolveInside(
                root,
                manifest.setupWindow,
            )
        requireRegularFile(
            root = root,
            path = setupWindow,
            label = "private service-image setup window source",
        )

        val java =
            VexOwnedPath.resolveInside(
                root,
                "runtime/bin/java",
            )
        requireRegularFile(
            root = root,
            path = java,
            label = "private service-image Java runtime",
        )
        require(
            Files.isExecutable(
                java,
            ),
        ) {
            "private service-image Java runtime must be executable"
        }

        val moduleImage =
            VexOwnedPath.resolveInside(
                root,
                "runtime/lib/modules",
            )
        requireRegularFile(
            root = root,
            path = moduleImage,
            label = "private service-image runtime module image",
        )

        for (
            artifact in
            manifest.artifacts
        ) {
            val path =
                VexOwnedPath.resolveInside(
                    root,
                    artifact.path,
                )
            requireRegularFile(
                root = root,
                path = path,
                label = "private service-image artifact",
            )
            require(
                Files.size(
                    path,
                ) ==
                    artifact.bytes,
            ) {
                "private service-image artifact byte length drift: " +
                    artifact.path
            }
            require(
                sha256(
                    path,
                ) ==
                    artifact.sha256,
            ) {
                "private service-image artifact SHA-256 drift: " +
                    artifact.path
            }
        }

        manifest.visionRuntime?.let {
            visionRuntime ->
            validateVisionRuntimeFiles(
                root = root,
                visionRuntime = visionRuntime,
            )
        }

        validateSymlinkContainment(
            root,
        )

        val payload =
            payloadFingerprint(
                root,
            )
        require(
            payload ==
                manifest.payloadSha256,
        ) {
            "private service-image payload fingerprint drift"
        }

        return Observation(
            imageRef =
                "image.vexlife-prod.private-service." +
                    payload.take(
                        32,
                    ),
            sourceSha = manifest.sourceSha,
            payloadSha256 = payload,
            launcherPath = launcher.toString(),
            setupLauncherPath = setupLauncher.toString(),
            setupWindowPath = setupWindow.toString(),
            javaPath = java.toString(),
            runtimeModules = manifest.runtimeModules,
            visionRuntimeExecutablePathOrNull =
                manifest.visionRuntime?.let {
                    VexOwnedPath.resolveInside(
                        root,
                        it.runtimeExecutable,
                    )
                        .toString()
                },
            visionRuntimeHelperExecutablePathOrNull =
                manifest.visionRuntime?.let {
                    VexOwnedPath.resolveInside(
                        root,
                        it.helperExecutable,
                    )
                        .toString()
                },
            visionRuntimeTruthClassOrNull =
                manifest.visionRuntime?.truthClass,
            visionRuntimeReleaseDispositionOrNull =
                manifest.visionRuntime?.releaseDisposition,
            visionRuntimeSdkSourceCommitOrNull =
                manifest.visionRuntime?.sdkSourceCommit,
            effectsPerformed = false,
        )
    }

    private fun validateSymlinkContainment(
        root: Path,
    ) {
        val canonicalRoot =
            root.toRealPath()

        Files.walk(
            root,
        )
            .use {
                stream ->
                stream.forEach {
                    path ->
                    if (
                        !Files.isSymbolicLink(
                            path,
                        )
                    ) {
                        return@forEach
                    }

                    val target =
                        Files.readSymbolicLink(
                            path,
                        )
                    require(
                        !target.isAbsolute,
                    ) {
                        "private service image cannot contain absolute symlink targets"
                    }

                    val resolved =
                        path
                            .parent
                            .resolve(
                                target,
                            )
                            .normalize()
                    require(
                        resolved.startsWith(
                            root,
                        ),
                    ) {
                        "private service image symlink escaped image root"
                    }

                    val canonicalTarget =
                        path.toRealPath()
                    require(
                        canonicalTarget.startsWith(
                            canonicalRoot,
                        ),
                    ) {
                        "private service image symlink resolves outside image root"
                    }
                }
            }
    }

    internal fun payloadFingerprint(
        root: Path,
    ): String {
        val ownedRoot =
            VexOwnedPath.requireRoot(
                root,
                "VexLife-Prod private service image",
            )
        val digest =
            MessageDigest.getInstance(
                "SHA-256",
            )

        Files.walk(
            ownedRoot,
        )
            .use {
                stream ->
                stream
                    .sorted()
                    .forEach {
                        path ->
                        val relative =
                            ownedRoot
                                .relativize(
                                    path,
                                )
                                .toString()
                                .replace(
                                    '\\',
                                    '/',
                                )

                        if (
                            relative.isEmpty() ||
                            relative ==
                            ManifestRelativePath
                        ) {
                            return@forEach
                        }

                        val permissions =
                            try {
                                Files.getPosixFilePermissions(
                                    path,
                                    LinkOption.NOFOLLOW_LINKS,
                                )
                                    .map {
                                        it.name
                                    }
                                    .sorted()
                                    .joinToString(
                                        ",",
                                    )
                            } catch (
                                _: UnsupportedOperationException,
                            ) {
                                ""
                            }

                        val descriptor =
                            when {
                                Files.isSymbolicLink(
                                    path,
                                ) ->
                                    "L\u0000" +
                                        relative +
                                        "\u0000" +
                                        permissions +
                                        "\u0000" +
                                        Files.readSymbolicLink(
                                            path,
                                        )
                                            .toString()

                                Files.isRegularFile(
                                    path,
                                    LinkOption.NOFOLLOW_LINKS,
                                ) ->
                                    "F\u0000" +
                                        relative +
                                        "\u0000" +
                                        permissions +
                                        "\u0000" +
                                        sha256(
                                            path,
                                        )

                                Files.isDirectory(
                                    path,
                                    LinkOption.NOFOLLOW_LINKS,
                                ) ->
                                    "D\u0000" +
                                        relative +
                                        "\u0000" +
                                        permissions

                                else ->
                                    error(
                                        "private service image contains unsupported filesystem entry: $relative",
                                    )
                            }

                        digest.update(
                            descriptor.toByteArray(
                                StandardCharsets.UTF_8,
                            ),
                        )
                        digest.update(
                            0.toByte(),
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

    private fun validateManifest(
        manifest: Manifest,
    ) {
        require(
            manifest.schemaVersion in
                setOf(
                    LegacySchema,
                    Schema,
                ) &&
                manifest.truthClass ==
                TruthClass &&
                GitSha.matches(
                    manifest.sourceSha,
                ) &&
                manifest.platform ==
                Platform &&
                manifest.javaRuntimeMajor ==
                JavaRuntimeMajor &&
                manifest.mainClass ==
                MainClass &&
                manifest.launcher ==
                LauncherRelativePath &&
                manifest.setupLauncher ==
                SetupLauncherRelativePath &&
                manifest.setupWindow ==
                SetupWindowRelativePath &&
                Sha256.matches(
                    manifest.payloadSha256,
                ),
        ) {
            "private service-image manifest identity drift"
        }

        require(
            manifest.runtimeModules.isNotEmpty() &&
                manifest.runtimeModules ==
                manifest.runtimeModules
                    .distinct()
                    .sorted() &&
                "java.net.http" in
                manifest.runtimeModules &&
                "jdk.httpserver" in
                manifest.runtimeModules &&
                "jdk.crypto.ec" in
                manifest.runtimeModules,
        ) {
            "private service-image runtime module set is invalid"
        }

        require(
            manifest.artifacts.isNotEmpty() &&
                manifest.artifacts
                    .map(
                        Artifact::path,
                    )
                    .distinct()
                    .size ==
                manifest.artifacts.size &&
                manifest.artifacts
                    .map(
                        Artifact::path,
                    ) ==
                manifest.artifacts
                    .map(
                        Artifact::path,
                    )
                    .sorted(),
        ) {
            "private service-image artifact catalog must be non-empty, unique and sorted"
        }

        manifest.artifacts.forEach {
            artifact ->
            require(
                artifact.path.matches(
                    Regex(
                        "lib/[A-Za-z0-9._+-]+\\.jar",
                    ),
                ) &&
                    Sha256.matches(
                        artifact.sha256,
                    ) &&
                    artifact.bytes >
                    0,
            ) {
                "private service-image artifact descriptor is invalid"
            }
        }

        require(
            manifest.schemaVersion !=
                LegacySchema ||
                manifest.visionRuntime ==
                null,
        ) {
            "legacy private service-image schema cannot claim a Vision runtime"
        }

        manifest.visionRuntime?.let {
            visionRuntime ->
            require(
                manifest.schemaVersion ==
                    Schema &&
                    visionRuntime.truthClass ==
                    VisionRuntimeTruthClass &&
                    visionRuntime.releaseDisposition ==
                    VisionRuntimeReleaseDisposition &&
                    GitSha.matches(
                        visionRuntime.sdkSourceCommit,
                    ) &&
                    GitSha.matches(
                        visionRuntime.sdkSourceTree,
                    ) &&
                    Sha256.matches(
                        visionRuntime.archiveSha256,
                    ) &&
                    Sha256.matches(
                        visionRuntime.manifestSha256,
                    ) &&
                    visionRuntime.runtimeExecutable ==
                    VisionRuntimeExecutableRelativePath &&
                    Sha256.matches(
                        visionRuntime.runtimeExecutableSha256,
                    ) &&
                    visionRuntime.helperExecutable ==
                    VisionRuntimeHelperExecutableRelativePath &&
                    Sha256.matches(
                        visionRuntime.helperExecutableSha256,
                    )
            ) {
                "private service-image Vision runtime descriptor is invalid"
            }
        }
    }

    private fun validateVisionRuntimeFiles(
        root: Path,
        visionRuntime: VisionRuntime,
    ) {
        val artifactManifest =
            VexOwnedPath.resolveInside(
                root,
                VisionRuntimeManifestRelativePath,
            )
        requireRegularFile(
            root = root,
            path = artifactManifest,
            label = "private service-image Vision runtime manifest",
        )
        require(
            sha256(
                artifactManifest,
            ) ==
                visionRuntime.manifestSha256,
        ) {
            "private service-image Vision runtime manifest SHA-256 drift"
        }

        val runtimeExecutable =
            VexOwnedPath.resolveInside(
                root,
                visionRuntime.runtimeExecutable,
            )
        requireRegularFile(
            root = root,
            path = runtimeExecutable,
            label = "private service-image Vision runtime executable",
        )
        require(
            Files.isExecutable(
                runtimeExecutable,
            ) &&
                sha256(
                    runtimeExecutable,
                ) ==
                visionRuntime.runtimeExecutableSha256,
        ) {
            "private service-image Vision runtime executable identity drift"
        }

        val helperExecutable =
            VexOwnedPath.resolveInside(
                root,
                visionRuntime.helperExecutable,
            )
        requireRegularFile(
            root = root,
            path = helperExecutable,
            label = "private service-image Vision helper executable",
        )
        require(
            Files.isExecutable(
                helperExecutable,
            ) &&
                sha256(
                    helperExecutable,
                ) ==
                visionRuntime.helperExecutableSha256,
        ) {
            "private service-image Vision helper executable identity drift"
        }
    }

    private fun requireRegularFile(
        root: Path,
        path: Path,
        label: String,
    ) {
        require(
            VexOwnedPath.observeParent(
                root,
                path,
            ) ==
                VexOwnedPath.DirectoryState.Present &&
                Files.isRegularFile(
                    path,
                    LinkOption.NOFOLLOW_LINKS,
                ) &&
                !Files.isSymbolicLink(
                    path,
                ),
        ) {
            "$label must be one real regular file inside the image"
        }
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
                        1024 * 1024,
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
}

// [VXG RealForever]
