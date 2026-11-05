package vexlife.packaging

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
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
        val app =
            root.resolve(
                "lib/vexlife-prod.jar",
            )
        val dependency =
            root.resolve(
                "lib/kotlin-stdlib.jar",
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
            app,
            "app",
        )
        Files.writeString(
            dependency,
            "stdlib",
        )

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
                schemaVersion = VexLifeProdPrivateServiceImage.Schema,
                truthClass = VexLifeProdPrivateServiceImage.TruthClass,
                sourceSha = sourceSha,
                platform = VexLifeProdPrivateServiceImage.Platform,
                javaRuntimeMajor = VexLifeProdPrivateServiceImage.JavaRuntimeMajor,
                mainClass = VexLifeProdPrivateServiceImage.MainClass,
                launcher = VexLifeProdPrivateServiceImage.LauncherRelativePath,
                setupLauncher = VexLifeProdPrivateServiceImage.SetupLauncherRelativePath,
                runtimeModules =
                    listOf(
                        "java.base",
                        "java.net.http",
                        "jdk.httpserver",
                    ),
                payloadSha256 = payload,
                artifacts = artifacts,
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
