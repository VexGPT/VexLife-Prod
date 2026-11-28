package vexlife.packaging

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vexlife.contract.VexContractJson

class VexLifeProdPrivateServiceImageTest {
    private fun sha256(
        path: Path,
    ): String {
        val digest =
            java.security.MessageDigest.getInstance(
                "SHA-256",
            )
        digest.update(
            Files.readAllBytes(
                path,
            ),
        )
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

    private fun formImage(
        root: Path,
        sourceSha: String =
            "a".repeat(
                40,
            ),
        includeVisionRuntime: Boolean =
            false,
        includeLocalAsrProvider: Boolean =
            false,
        includePartnerHost: Boolean =
            false,
        schemaVersion: String =
            when {
                includePartnerHost ->
                    VexLifeProdPrivateServiceImage.Schema

                includeLocalAsrProvider ->
                    VexLifeProdPrivateServiceImage.LocalAsrSchema

                includeVisionRuntime ->
                    VexLifeProdPrivateServiceImage.VisionSchema

                else ->
                    VexLifeProdPrivateServiceImage.LegacySchema
            },
    ): VexLifeProdPrivateServiceImage.Manifest {
        Files.createDirectories(
            root.resolve(
                "runtime/bin",
            ),
        )
        Files.createDirectories(
            root.resolve(
                "runtime/lib",
            ),
        )
        Files.createDirectories(
            root.resolve(
                "lib",
            ),
        )
        Files.createDirectories(
            root.resolve(
                "bin",
            ),
        )
        Files.createDirectories(
            root.resolve(
                "setup",
            ),
        )

        val java =
            root.resolve(
                "runtime/bin/java",
            )
        val modules =
            root.resolve(
                "runtime/lib/modules",
            )
        val launcher =
            root.resolve(
                VexLifeProdPrivateServiceImage.LauncherRelativePath,
            )
        val setupLauncher =
            root.resolve(
                VexLifeProdPrivateServiceImage.SetupLauncherRelativePath,
            )
        val setupWindow =
            root.resolve(
                VexLifeProdPrivateServiceImage.SetupWindowRelativePath,
            )
        val app =
            root.resolve(
                "lib/vexlife-prod.jar",
            )
        val dependency =
            root.resolve(
                "lib/kotlin-stdlib.jar",
            )
        val partnerHost =
            root.resolve(
                VexLifeProdPrivateServiceImage.PartnerHostExecutableRelativePath,
            )

        Files.writeString(
            java,
            "fake-java",
        )
        Files.writeString(
            modules,
            "fake-module-image",
        )
        Files.writeString(
            launcher,
            "#!/bin/bash\nexit 0\n",
        )
        Files.writeString(
            setupLauncher,
            "#!/bin/bash\nexit 0\n",
        )
        Files.writeString(
            setupWindow,
            "use framework \"AppKit\"\n-- test setup window\n",
        )
        Files.writeString(
            app,
            "app",
        )
        Files.writeString(
            dependency,
            "stdlib",
        )
        if (
            includePartnerHost
        ) {
            Files.writeString(
                partnerHost,
                "fake-partner-host",
            )
            Files.setPosixFilePermissions(
                partnerHost,
                setOf(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE,
                ),
            )
        }

        Files.setPosixFilePermissions(
            java,
            setOf(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE,
            ),
        )
        Files.setPosixFilePermissions(
            launcher,
            setOf(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE,
            ),
        )
        Files.setPosixFilePermissions(
            setupLauncher,
            setOf(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE,
            ),
        )

        val localAsrProvider =
            if (
                includeLocalAsrProvider
            ) {
                val providerRoot =
                    root.resolve(
                        VexLifeProdPrivateServiceImage.LocalAsrProviderRootRelativePath,
                    )
                val providerBin =
                    providerRoot.resolve(
                        "bin",
                    )
                Files.createDirectories(
                    providerBin,
                )
                val runtimeExecutable =
                    root.resolve(
                        VexLifeProdPrivateServiceImage.LocalAsrProviderExecutableRelativePath,
                    )
                val sourceIdentity =
                    providerRoot.resolve(
                        "provider-source.txt",
                    )
                val sourceTreeIdentity =
                    providerRoot.resolve(
                        "provider-tree.txt",
                    )
                Files.writeString(
                    runtimeExecutable,
                    "fake-whisper-server",
                )
                Files.writeString(
                    sourceIdentity,
                    "b".repeat(
                        40,
                    ) +
                        "\n",
                )
                Files.writeString(
                    sourceTreeIdentity,
                    "c".repeat(
                        40,
                    ) +
                        "\n",
                )
                Files.setPosixFilePermissions(
                    runtimeExecutable,
                    setOf(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.OWNER_EXECUTE,
                    ),
                )
                VexLifeProdPrivateServiceImage.LocalAsrProvider(
                    providerRef =
                        VexLifeProdPrivateServiceImage.LocalAsrProviderRef,
                    providerVersion =
                        VexLifeProdPrivateServiceImage.LocalAsrProviderVersion,
                    releaseDisposition =
                        VexLifeProdPrivateServiceImage.LocalAsrProviderReleaseDisposition,
                    sourceRepository =
                        VexLifeProdPrivateServiceImage.LocalAsrProviderSourceRepository,
                    sourceCommit =
                        "b".repeat(
                            40,
                        ),
                    sourceTree =
                        "c".repeat(
                            40,
                        ),
                    providerRoot =
                        VexLifeProdPrivateServiceImage.LocalAsrProviderRootRelativePath,
                    providerPayloadSha256 =
                        VexLifeProdPrivateServiceImage.payloadFingerprint(
                            providerRoot,
                        ),
                    runtimeExecutable =
                        VexLifeProdPrivateServiceImage.LocalAsrProviderExecutableRelativePath,
                    runtimeExecutableSha256 =
                        sha256(
                            runtimeExecutable,
                        ),
                )
            } else {
                null
            }

        val visionRuntime =
            if (
                includeVisionRuntime
            ) {
                val visionRoot =
                    root.resolve(
                        "vision-runtime",
                    )
                val visionBin =
                    visionRoot.resolve(
                        "bin",
                    )
                val visionHelper =
                    visionRoot.resolve(
                        "helper/VexLifeVisionSelectedScopeAgent.app/Contents/MacOS",
                    )
                Files.createDirectories(
                    visionBin,
                )
                Files.createDirectories(
                    visionHelper,
                )
                val visionManifest =
                    visionRoot.resolve(
                        "vision-runtime.json",
                    )
                val runtimeExecutable =
                    visionBin.resolve(
                        "vexlife-vision-runtime",
                    )
                val helperExecutable =
                    visionHelper.resolve(
                        "VexLifeVisionSelectedScopeAgent",
                    )
                Files.writeString(
                    visionManifest,
                    "{\"truthClass\":\"LOCAL_QUALIFIED_NOT_DISTRIBUTABLE\"}\n",
                )
                Files.writeString(
                    runtimeExecutable,
                    "fake-vision-runtime",
                )
                Files.writeString(
                    helperExecutable,
                    "fake-vision-helper",
                )
                Files.setPosixFilePermissions(
                    runtimeExecutable,
                    setOf(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.OWNER_EXECUTE,
                    ),
                )
                Files.setPosixFilePermissions(
                    helperExecutable,
                    setOf(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.OWNER_EXECUTE,
                    ),
                )
                VexLifeProdPrivateServiceImage.VisionRuntime(
                    truthClass =
                        VexLifeProdPrivateServiceImage.VisionRuntimeTruthClass,
                    releaseDisposition =
                        VexLifeProdPrivateServiceImage.VisionRuntimeReleaseDisposition,
                    sdkSourceCommit =
                        "b".repeat(
                            40,
                        ),
                    sdkSourceTree =
                        "c".repeat(
                            40,
                        ),
                    archiveSha256 =
                        "d".repeat(
                            64,
                        ),
                    manifestSha256 =
                        sha256(
                            visionManifest,
                        ),
                    runtimeExecutable =
                        VexLifeProdPrivateServiceImage.VisionRuntimeExecutableRelativePath,
                    runtimeExecutableSha256 =
                        sha256(
                            runtimeExecutable,
                        ),
                    helperExecutable =
                        VexLifeProdPrivateServiceImage.VisionRuntimeHelperExecutableRelativePath,
                    helperExecutableSha256 =
                        sha256(
                            helperExecutable,
                        ),
                )
            } else {
                null
            }

        val partnerHostDescriptor =
            if (
                includePartnerHost
            ) {
                VexLifeProdPrivateServiceImage.PartnerHost(
                    executable =
                        VexLifeProdPrivateServiceImage.PartnerHostExecutableRelativePath,
                    executableSha256 =
                        sha256(
                            partnerHost,
                        ),
                )
            } else {
                null
            }

        val artifacts =
            listOf(
                app,
                dependency,
            )
                .sortedBy {
                    it.fileName
                        .toString()
                }
                .map {
                    file ->
                    VexLifeProdPrivateServiceImage.Artifact(
                        path =
                            "lib/" +
                                file.fileName,
                        sha256 =
                            sha256(
                                file,
                            ),
                        bytes =
                            Files.size(
                                file,
                            ),
                    )
                }

        val payload =
            VexLifeProdPrivateServiceImage.payloadFingerprint(
                root,
            )
        val manifest =
            VexLifeProdPrivateServiceImage.Manifest(
                schemaVersion = schemaVersion,
                truthClass = VexLifeProdPrivateServiceImage.TruthClass,
                sourceSha = sourceSha,
                platform = VexLifeProdPrivateServiceImage.Platform,
                javaRuntimeMajor = VexLifeProdPrivateServiceImage.JavaRuntimeMajor,
                mainClass = VexLifeProdPrivateServiceImage.MainClass,
                launcher = VexLifeProdPrivateServiceImage.LauncherRelativePath,
                setupLauncher = VexLifeProdPrivateServiceImage.SetupLauncherRelativePath,
                setupWindow = VexLifeProdPrivateServiceImage.SetupWindowRelativePath,
                runtimeModules =
                    listOf(
                        "java.base",
                        "java.net.http",
                        "jdk.compiler",
                        "jdk.crypto.ec",
                        "jdk.httpserver",
                    ),
                payloadSha256 = payload,
                artifacts = artifacts,
                partnerHost = partnerHostDescriptor,
                localAsrProvider = localAsrProvider,
                visionRuntime = visionRuntime,
            )

        Files.writeString(
            root.resolve(
                VexLifeProdPrivateServiceImage.ManifestRelativePath,
            ),
            VexContractJson.encode(
                manifest,
            ) +
                "\n",
            StandardCharsets.UTF_8,
        )

        return manifest
    }

    @Test
    fun exactSourceBoundImageObservesWithoutEffects() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-private-service-image-",
            )
                .toAbsolutePath()
        val root =
            parent.resolve(
                "image",
            )
        val sourceSha =
            "a".repeat(
                40,
            )

        try {
            Files.createDirectory(
                root,
            )
            val manifest =
                formImage(
                    root,
                    sourceSha,
                )

            val observed =
                VexLifeProdPrivateServiceImage.observe(
                    imageRoot = root,
                    expectedSourceSha = sourceSha,
                )

            assertEquals(
                sourceSha,
                observed.sourceSha,
            )
            assertEquals(
                manifest.payloadSha256,
                observed.payloadSha256,
            )
            assertEquals(
                root.resolve(
                    VexLifeProdPrivateServiceImage.LauncherRelativePath,
                )
                    .toString(),
                observed.launcherPath,
            )
            assertEquals(
                root.resolve(
                    VexLifeProdPrivateServiceImage.SetupLauncherRelativePath,
                )
                    .toString(),
                observed.setupLauncherPath,
            )
            assertEquals(
                root.resolve(
                    VexLifeProdPrivateServiceImage.SetupWindowRelativePath,
                )
                    .toString(),
                observed.setupWindowPath,
            )
            assertFalse(
                observed.effectsPerformed,
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun legacyV3ImageWithoutVisionRuntimeRemainsObservable() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-private-service-image-legacy-",
            )
                .toAbsolutePath()
        val root =
            parent.resolve(
                "image",
            )
        val sourceSha =
            "a".repeat(
                40,
            )

        try {
            Files.createDirectory(
                root,
            )
            formImage(
                root = root,
                sourceSha = sourceSha,
                schemaVersion =
                    VexLifeProdPrivateServiceImage.LegacySchema,
            )

            val observed =
                VexLifeProdPrivateServiceImage.observe(
                    imageRoot = root,
                    expectedSourceSha = sourceSha,
                )

            assertEquals(
                null,
                observed.visionRuntimeExecutablePathOrNull,
            )
            assertEquals(
                null,
                observed.visionRuntimeTruthClassOrNull,
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun currentV4SchemaRequiresVisionRuntime() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-private-service-image-v4-without-vision-",
            )
                .toAbsolutePath()
        val root =
            parent.resolve(
                "image",
            )
        val sourceSha =
            "a".repeat(
                40,
            )

        try {
            Files.createDirectory(
                root,
            )
            formImage(
                root = root,
                sourceSha = sourceSha,
                schemaVersion =
                    VexLifeProdPrivateServiceImage.VisionSchema,
                includeVisionRuntime = false,
            )

            assertFailsWith<IllegalArgumentException> {
                VexLifeProdPrivateServiceImage.observe(
                    imageRoot = root,
                    expectedSourceSha = sourceSha,
                )
            }
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun localQualifiedVisionRuntimeIsExplicitlyObserved() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-private-service-image-vision-",
            )
                .toAbsolutePath()
        val root =
            parent.resolve(
                "image",
            )
        val sourceSha =
            "a".repeat(
                40,
            )

        try {
            Files.createDirectory(
                root,
            )
            formImage(
                root = root,
                sourceSha = sourceSha,
                includeVisionRuntime = true,
            )

            val observed =
                VexLifeProdPrivateServiceImage.observe(
                    imageRoot = root,
                    expectedSourceSha = sourceSha,
                )

            assertTrue(
                requireNotNull(
                    observed.visionRuntimeExecutablePathOrNull,
                )
                    .endsWith(
                        VexLifeProdPrivateServiceImage.VisionRuntimeExecutableRelativePath,
                    ),
            )
            assertEquals(
                VexLifeProdPrivateServiceImage.VisionRuntimeTruthClass,
                observed.visionRuntimeTruthClassOrNull,
            )
            assertEquals(
                VexLifeProdPrivateServiceImage.VisionRuntimeReleaseDisposition,
                observed.visionRuntimeReleaseDispositionOrNull,
            )
            assertEquals(
                "b".repeat(
                    40,
                ),
                observed.visionRuntimeSdkSourceCommitOrNull,
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun VisionRuntimeExecutableTamperIsRejected() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-private-service-image-vision-tamper-",
            )
                .toAbsolutePath()
        val root =
            parent.resolve(
                "image",
            )
        val sourceSha =
            "a".repeat(
                40,
            )

        try {
            Files.createDirectory(
                root,
            )
            formImage(
                root = root,
                sourceSha = sourceSha,
                includeVisionRuntime = true,
            )
            Files.writeString(
                root.resolve(
                    VexLifeProdPrivateServiceImage.VisionRuntimeExecutableRelativePath,
                ),
                "tampered-vision-runtime",
            )

            assertFailsWith<IllegalArgumentException> {
                VexLifeProdPrivateServiceImage.observe(
                    imageRoot = root,
                    expectedSourceSha = sourceSha,
                )
            }
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun legacySchemaCannotClaimVisionRuntime() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-private-service-image-legacy-vision-",
            )
                .toAbsolutePath()
        val root =
            parent.resolve(
                "image",
            )
        val sourceSha =
            "a".repeat(
                40,
            )

        try {
            Files.createDirectory(
                root,
            )
            formImage(
                root = root,
                sourceSha = sourceSha,
                schemaVersion =
                    VexLifeProdPrivateServiceImage.LegacySchema,
                includeVisionRuntime = true,
            )

            assertFailsWith<IllegalArgumentException> {
                VexLifeProdPrivateServiceImage.observe(
                    imageRoot = root,
                    expectedSourceSha = sourceSha,
                )
            }
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun currentV6SchemaRequiresBundledPartnerHost() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-private-service-image-v6-without-partner-host-",
            )
                .toAbsolutePath()
        val root =
            parent.resolve(
                "image",
            )

        try {
            Files.createDirectory(
                root,
            )
            formImage(
                root = root,
                schemaVersion =
                    VexLifeProdPrivateServiceImage.Schema,
                includeLocalAsrProvider =
                    true,
                includePartnerHost =
                    false,
            )

            assertFailsWith<IllegalArgumentException> {
                VexLifeProdPrivateServiceImage.observe(
                    imageRoot = root,
                    expectedSourceSha =
                        "a".repeat(
                            40,
                        ),
                )
            }
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun bundledPartnerHostIsExplicitlyObserved() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-private-service-image-partner-host-",
            )
                .toAbsolutePath()
        val root =
            parent.resolve(
                "image",
            )

        try {
            Files.createDirectory(
                root,
            )
            formImage(
                root = root,
                includeLocalAsrProvider =
                    true,
                includePartnerHost =
                    true,
            )

            val observed =
                VexLifeProdPrivateServiceImage.observe(
                    imageRoot = root,
                    expectedSourceSha =
                        "a".repeat(
                            40,
                        ),
                )

            assertTrue(
                requireNotNull(
                    observed.partnerHostExecutablePathOrNull,
                )
                    .endsWith(
                        VexLifeProdPrivateServiceImage.PartnerHostExecutableRelativePath,
                    ),
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun bundledPartnerHostTamperIsRejected() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-private-service-image-partner-host-tamper-",
            )
                .toAbsolutePath()
        val root =
            parent.resolve(
                "image",
            )

        try {
            Files.createDirectory(
                root,
            )
            formImage(
                root = root,
                includeLocalAsrProvider =
                    true,
                includePartnerHost =
                    true,
            )
            Files.writeString(
                root.resolve(
                    VexLifeProdPrivateServiceImage.PartnerHostExecutableRelativePath,
                ),
                "tampered-partner-host",
            )

            assertFailsWith<IllegalArgumentException> {
                VexLifeProdPrivateServiceImage.observe(
                    imageRoot = root,
                    expectedSourceSha =
                        "a".repeat(
                            40,
                        ),
                )
            }
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun currentV5SchemaRequiresBundledLocalAsrProvider() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-private-service-image-v5-without-local-asr-",
            )
                .toAbsolutePath()
        val root =
            parent.resolve(
                "image",
            )
        val sourceSha =
            "a".repeat(
                40,
            )

        try {
            Files.createDirectory(
                root,
            )
            formImage(
                root = root,
                sourceSha = sourceSha,
                schemaVersion =
                    VexLifeProdPrivateServiceImage.LocalAsrSchema,
                includeLocalAsrProvider =
                    false,
            )

            assertFailsWith<IllegalArgumentException> {
                VexLifeProdPrivateServiceImage.observe(
                    imageRoot = root,
                    expectedSourceSha = sourceSha,
                )
            }
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun bundledLocalAsrProviderIsExplicitlyObserved() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-private-service-image-local-asr-",
            )
                .toAbsolutePath()
        val root =
            parent.resolve(
                "image",
            )
        val sourceSha =
            "a".repeat(
                40,
            )

        try {
            Files.createDirectory(
                root,
            )
            formImage(
                root = root,
                sourceSha = sourceSha,
                includeLocalAsrProvider =
                    true,
            )

            val observed =
                VexLifeProdPrivateServiceImage.observe(
                    imageRoot = root,
                    expectedSourceSha = sourceSha,
                )

            assertTrue(
                requireNotNull(
                    observed.localAsrProviderExecutablePathOrNull,
                )
                    .endsWith(
                        VexLifeProdPrivateServiceImage.LocalAsrProviderExecutableRelativePath,
                    ),
            )
            assertEquals(
                "b".repeat(
                    40,
                ),
                observed.localAsrProviderSourceCommitOrNull,
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun bundledLocalAsrProviderTamperIsRejected() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-private-service-image-local-asr-tamper-",
            )
                .toAbsolutePath()
        val root =
            parent.resolve(
                "image",
            )
        val sourceSha =
            "a".repeat(
                40,
            )

        try {
            Files.createDirectory(
                root,
            )
            formImage(
                root = root,
                sourceSha = sourceSha,
                includeLocalAsrProvider =
                    true,
            )
            Files.writeString(
                root.resolve(
                    VexLifeProdPrivateServiceImage.LocalAsrProviderExecutableRelativePath,
                ),
                "tampered-whisper-server",
            )

            assertFailsWith<IllegalArgumentException> {
                VexLifeProdPrivateServiceImage.observe(
                    imageRoot = root,
                    expectedSourceSha = sourceSha,
                )
            }
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun java21ImageWithoutSunEcProviderIsRejectedEvenWhenPayloadBytesAreUnchanged() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-private-service-image-tls-provider-",
            )
                .toAbsolutePath()
        val root =
            parent.resolve(
                "image",
            )
        val sourceSha =
            "a".repeat(
                40,
            )

        try {
            Files.createDirectory(
                root,
            )
            val original =
                formImage(
                    root,
                    sourceSha,
                )
            val missingTlsProvider =
                original.copy(
                    runtimeModules =
                        original.runtimeModules.filterNot {
                            it ==
                                "jdk.crypto.ec"
                        },
                )

            Files.writeString(
                root.resolve(
                    VexLifeProdPrivateServiceImage.ManifestRelativePath,
                ),
                VexContractJson.encode(
                    missingTlsProvider,
                ) +
                    "\n",
                StandardCharsets.UTF_8,
            )

            assertFailsWith<IllegalArgumentException> {
                VexLifeProdPrivateServiceImage.observe(
                    imageRoot = root,
                    expectedSourceSha = sourceSha,
                )
            }
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun imageCannotClaimDifferentExpectedSource() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-private-service-image-source-",
            )
                .toAbsolutePath()
        val root =
            parent.resolve(
                "image",
            )

        try {
            Files.createDirectory(
                root,
            )
            formImage(
                root,
                "a".repeat(
                    40,
                ),
            )

            assertFailsWith<IllegalArgumentException> {
                VexLifeProdPrivateServiceImage.observe(
                    imageRoot = root,
                    expectedSourceSha =
                        "b".repeat(
                            40,
                        ),
                )
            }
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun hashConsistentImageStillRejectsSymlinkEscapingImageRoot() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-private-service-image-symlink-",
            )
                .toAbsolutePath()
        val root =
            parent.resolve(
                "image",
            )
        val outside =
            parent.resolve(
                "outside-target",
            )
        val sourceSha =
            "a".repeat(
                40,
            )

        try {
            Files.createDirectory(
                root,
            )
            val original =
                formImage(
                    root,
                    sourceSha,
                )
            Files.writeString(
                outside,
                "outside",
            )
            Files.createSymbolicLink(
                root.resolve(
                    "runtime/lib/escape",
                ),
                Path.of(
                    "../../../outside-target",
                ),
            )

            val rewritten =
                original.copy(
                    payloadSha256 =
                        VexLifeProdPrivateServiceImage.payloadFingerprint(
                            root,
                        ),
                )
            Files.writeString(
                root.resolve(
                    VexLifeProdPrivateServiceImage.ManifestRelativePath,
                ),
                VexContractJson.encode(
                    rewritten,
                ) +
                    "\n",
                StandardCharsets.UTF_8,
            )

            assertFailsWith<IllegalArgumentException> {
                VexLifeProdPrivateServiceImage.observe(
                    imageRoot = root,
                    expectedSourceSha = sourceSha,
                )
            }
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun payloadMutationFailsEvenWhenManifestWasPreviouslyValid() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-private-service-image-tamper-",
            )
                .toAbsolutePath()
        val root =
            parent.resolve(
                "image",
            )
        val sourceSha =
            "a".repeat(
                40,
            )

        try {
            Files.createDirectory(
                root,
            )
            formImage(
                root,
                sourceSha,
            )

            Files.writeString(
                root.resolve(
                    "lib/vexlife-prod.jar",
                ),
                "tampered-app",
            )

            assertFailsWith<IllegalArgumentException> {
                VexLifeProdPrivateServiceImage.observe(
                    imageRoot = root,
                    expectedSourceSha = sourceSha,
                )
            }
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }
}

// [VXG RealForever]
