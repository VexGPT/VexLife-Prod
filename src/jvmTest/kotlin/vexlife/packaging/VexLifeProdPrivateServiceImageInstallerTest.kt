package vexlife.packaging

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vexlife.contract.VexContractJson
import vexlife.home.CanonicalVexLifeHomeFixture

class VexLifeProdPrivateServiceImageInstallerTest {
    private fun sha256(
        path: Path,
    ): String {
        val digest =
            MessageDigest.getInstance(
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
        sourceSha: String,
    ) {
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
                schemaVersion = VexLifeProdPrivateServiceImage.LegacySchema,
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
                        "jdk.crypto.ec",
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
    }

    private fun bootstrapHome(
        productHome: Path,
    ) {
        CanonicalVexLifeHomeFixture.form(
            home = productHome,
            deviceSuffix = "image-installer-test",
            lineageSuffix = "image-installer-test",
        )
    }

    @Test
    fun missingImageInstallsAtomicallyThenExactImageReusesWithoutMutation() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-image-installer-",
            )
                .toAbsolutePath()
        val source =
            parent.resolve(
                "source-image",
            )
        val home =
            parent.resolve(
                "VexLife-Prod",
            )
        val sourceSha =
            "a".repeat(
                40,
            )

        try {
            Files.createDirectory(
                source,
            )
            formImage(
                source,
                sourceSha,
            )
            bootstrapHome(
                home,
            )

            val installed =
                VexLifeProdPrivateServiceImageInstaller.install(
                    sourceImageRoot = source,
                    productHome = home,
                    expectedSourceSha = sourceSha,
                )

            assertEquals(
                "INSTALLED_VERIFIED_PRIVATE_SERVICE_IMAGE",
                installed.state,
            )
            assertTrue(
                installed.imageMutationPerformed,
            )
            val target =
                Path.of(
                    installed.imagePath,
                )
            assertTrue(
                target.startsWith(
                    home.resolve(
                        "runtime/product-service/images",
                    ),
                ),
            )
            assertEquals(
                installed.payloadSha256,
                VexLifeProdPrivateServiceImage.observe(
                    imageRoot = target,
                    expectedSourceSha = sourceSha,
                )
                    .payloadSha256,
            )

            val reused =
                VexLifeProdPrivateServiceImageInstaller.install(
                    sourceImageRoot = source,
                    productHome = home,
                    expectedSourceSha = sourceSha,
                )

            assertEquals(
                "REUSED_VERIFIED_INSTALLED_IMAGE",
                reused.state,
            )
            assertFalse(
                reused.imageMutationPerformed,
            )
            assertEquals(
                installed.imagePath,
                reused.imagePath,
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun invalidExistingInstalledImageIsPreservedAndDoesNotBecomeRepairAuthority() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-image-installer-invalid-",
            )
                .toAbsolutePath()
        val source =
            parent.resolve(
                "source-image",
            )
        val home =
            parent.resolve(
                "VexLife-Prod",
            )
        val sourceSha =
            "a".repeat(
                40,
            )

        try {
            Files.createDirectory(
                source,
            )
            formImage(
                source,
                sourceSha,
            )
            bootstrapHome(
                home,
            )

            val installed =
                VexLifeProdPrivateServiceImageInstaller.install(
                    sourceImageRoot = source,
                    productHome = home,
                    expectedSourceSha = sourceSha,
                )
            val target =
                Path.of(
                    installed.imagePath,
                )
            val targetJar =
                target.resolve(
                    "lib/vexlife-prod.jar",
                )

            Files.writeString(
                targetJar,
                "tampered-installed-image",
            )

            assertFailsWith<IllegalArgumentException> {
                VexLifeProdPrivateServiceImageInstaller.install(
                    sourceImageRoot = source,
                    productHome = home,
                    expectedSourceSha = sourceSha,
                )
            }

            assertEquals(
                "tampered-installed-image",
                Files.readString(
                    targetJar,
                ),
            )
            assertTrue(
                Files.exists(
                    source.resolve(
                        "service-image.json",
                    ),
                ),
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun arbitraryDirectoryCannotMasqueradeAsProductHome() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-image-installer-not-home-",
            )
                .toAbsolutePath()
        val source =
            parent.resolve(
                "source-image",
            )
        val notHome =
            parent.resolve(
                "not-home",
            )
        val sourceSha =
            "a".repeat(
                40,
            )

        try {
            Files.createDirectory(
                source,
            )
            Files.createDirectory(
                notHome,
            )
            formImage(
                source,
                sourceSha,
            )

            assertFailsWith<IllegalArgumentException> {
                VexLifeProdPrivateServiceImageInstaller.install(
                    sourceImageRoot = source,
                    productHome = notHome,
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
