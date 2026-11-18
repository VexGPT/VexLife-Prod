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

class VexLifeProdPrivateServiceImageRetentionTest {
    @Test
    fun verifiedRetiredImageIsReclaimedWhileInvalidAndUnknownEntriesArePreserved() {
        val root = Files.createTempDirectory("vexlife-private-image-retention-").toAbsolutePath()
        val home = root.resolve("home")
        val priorSource = "a".repeat(40)
        val currentSource = "b".repeat(40)
        val invalidSource = "c".repeat(40)

        try {
            CanonicalVexLifeHomeFixture.form(
                home = home,
                deviceSuffix = "image-retention",
                lineageSuffix = "image-retention",
            )

            val prior =
                installImage(
                    root = root.resolve("prior-source"),
                    home = home,
                    sourceSha = priorSource,
                )
            val current =
                installImage(
                    root = root.resolve("current-source"),
                    home = home,
                    sourceSha = currentSource,
                )
            val invalid =
                installImage(
                    root = root.resolve("invalid-source"),
                    home = home,
                    sourceSha = invalidSource,
                )

            val invalidRoot = Path.of(invalid.imagePath)
            Files.writeString(invalidRoot.resolve("lib/vexlife-prod.jar"), "tampered")

            val imageBase = Path.of(current.imagePath).parent
            val unknown = imageBase.resolve("manual-notes")
            Files.createDirectory(unknown)
            Files.writeString(unknown.resolve("readme.txt"), "not a Product service image")

            val result =
                VexLifeProdPrivateServiceImageRetention.reclaimRetiredVerifiedImages(
                    productHome = home,
                    currentSourceSha = currentSource,
                    currentPayloadSha256 = current.payloadSha256,
                )

            assertFalse(Files.exists(Path.of(prior.imagePath)))
            assertTrue(Files.exists(Path.of(current.imagePath)))
            assertTrue(Files.exists(invalidRoot))
            assertTrue(Files.exists(unknown))
            assertEquals(listOf(priorSource), result.reclaimedSourceShas)
            assertTrue(result.reclaimedBytes > 0)
            assertTrue(result.effectsPerformed)
            assertEquals(
                VexLifeProdPrivateServiceImageRetention.EntryDisposition.HeldUnverifiedImage,
                result.entries.single { it.sourceSha == invalidSource }.disposition,
            )
            assertEquals(
                VexLifeProdPrivateServiceImageRetention.EntryDisposition.HeldUnrecognizedEntry,
                result.entries.single { it.name == "manual-notes" }.disposition,
            )
            assertEquals(
                current.payloadSha256,
                VexLifeProdPrivateServiceImage.observe(
                        imageRoot = Path.of(current.imagePath),
                        expectedSourceSha = currentSource,
                    )
                    .payloadSha256,
            )
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun currentDependencyBindingMustVerifyBeforeAnyRetiredImageIsDeleted() {
        val root = Files.createTempDirectory("vexlife-private-image-retention-current-").toAbsolutePath()
        val home = root.resolve("home")
        val priorSource = "d".repeat(40)
        val currentSource = "e".repeat(40)

        try {
            CanonicalVexLifeHomeFixture.form(
                home = home,
                deviceSuffix = "image-retention-current",
                lineageSuffix = "image-retention-current",
            )

            val prior =
                installImage(
                    root = root.resolve("prior-source"),
                    home = home,
                    sourceSha = priorSource,
                )
            val current =
                installImage(
                    root = root.resolve("current-source"),
                    home = home,
                    sourceSha = currentSource,
                )

            assertFailsWith<IllegalArgumentException> {
                VexLifeProdPrivateServiceImageRetention.reclaimRetiredVerifiedImages(
                    productHome = home,
                    currentSourceSha = currentSource,
                    currentPayloadSha256 = "f".repeat(64),
                )
            }

            assertTrue(Files.exists(Path.of(prior.imagePath)))
            assertTrue(Files.exists(Path.of(current.imagePath)))
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun exactCurrentImageAloneProducesNoMutation() {
        val root = Files.createTempDirectory("vexlife-private-image-retention-current-only-").toAbsolutePath()
        val home = root.resolve("home")
        val currentSource = "f".repeat(40)

        try {
            CanonicalVexLifeHomeFixture.form(
                home = home,
                deviceSuffix = "image-retention-only",
                lineageSuffix = "image-retention-only",
            )
            val current =
                installImage(
                    root = root.resolve("current-source"),
                    home = home,
                    sourceSha = currentSource,
                )

            val result =
                VexLifeProdPrivateServiceImageRetention.reclaimRetiredVerifiedImages(
                    productHome = home,
                    currentSourceSha = currentSource,
                    currentPayloadSha256 = current.payloadSha256,
                )

            assertFalse(result.effectsPerformed)
            assertEquals(0L, result.reclaimedBytes)
            assertEquals(emptyList(), result.reclaimedSourceShas)
            assertEquals(
                VexLifeProdPrivateServiceImageRetention.EntryDisposition.RetainedCurrent,
                result.entries.single().disposition,
            )
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    private fun installImage(
        root: Path,
        home: Path,
        sourceSha: String,
    ): VexLifeProdPrivateServiceImageInstaller.Result {
        Files.createDirectory(root)
        formImage(root, sourceSha)
        return VexLifeProdPrivateServiceImageInstaller.install(
            sourceImageRoot = root,
            productHome = home,
            expectedSourceSha = sourceSha,
        )
    }

    private fun formImage(
        root: Path,
        sourceSha: String,
    ) {
        Files.createDirectories(root.resolve("runtime/bin"))
        Files.createDirectories(root.resolve("runtime/lib"))
        Files.createDirectories(root.resolve("lib"))
        Files.createDirectories(root.resolve("bin"))
        Files.createDirectories(root.resolve("setup"))

        val java = root.resolve("runtime/bin/java")
        val modules = root.resolve("runtime/lib/modules")
        val launcher = root.resolve(VexLifeProdPrivateServiceImage.LauncherRelativePath)
        val setupLauncher = root.resolve(VexLifeProdPrivateServiceImage.SetupLauncherRelativePath)
        val setupWindow = root.resolve(VexLifeProdPrivateServiceImage.SetupWindowRelativePath)
        val app = root.resolve("lib/vexlife-prod.jar")
        val dependency = root.resolve("lib/kotlin-stdlib.jar")

        Files.writeString(java, "fake-java-$sourceSha")
        Files.writeString(modules, "fake-module-image-$sourceSha")
        Files.writeString(launcher, "#!/bin/bash\nexit 0\n")
        Files.writeString(setupLauncher, "#!/bin/bash\nexit 0\n")
        Files.writeString(setupWindow, "use framework \"AppKit\"\n-- retention test\n")
        Files.writeString(app, "app-$sourceSha")
        Files.writeString(dependency, "stdlib")

        val executablePermissions =
            setOf(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE,
            )
        Files.setPosixFilePermissions(java, executablePermissions)
        Files.setPosixFilePermissions(launcher, executablePermissions)
        Files.setPosixFilePermissions(setupLauncher, executablePermissions)

        val artifacts =
            listOf(app, dependency)
                .sortedBy { it.fileName.toString() }
                .map { file ->
                    VexLifeProdPrivateServiceImage.Artifact(
                        path = "lib/" + file.fileName,
                        sha256 = sha256(file),
                        bytes = Files.size(file),
                    )
                }

        val payload = VexLifeProdPrivateServiceImage.payloadFingerprint(root)
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
                runtimeModules = listOf("java.base", "java.net.http", "jdk.crypto.ec", "jdk.httpserver"),
                payloadSha256 = payload,
                artifacts = artifacts,
            )

        Files.writeString(
            root.resolve(VexLifeProdPrivateServiceImage.ManifestRelativePath),
            VexContractJson.encode(manifest) + "\n",
            StandardCharsets.UTF_8,
        )
    }

    private fun sha256(path: Path): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(Files.readAllBytes(path))
        return digest.digest().joinToString("") {
            "%02x".format(it.toInt() and 0xff)
        }
    }
}

// [VXG RealForever]
