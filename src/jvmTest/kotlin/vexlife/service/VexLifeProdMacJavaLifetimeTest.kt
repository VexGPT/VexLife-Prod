package vexlife.service

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
import vexlife.packaging.VexLifeProdPrivateServiceImage
import vexlife.packaging.VexLifeProdPrivateServiceImageInstaller

class VexLifeProdMacJavaLifetimeTest {
    @Test
    fun productSourceRotationKeepsOneStableJavaRuntimeAfterProviderImageIsGone() {
        val root =
            Files.createTempDirectory(
                "vexlife-mac-java-lifetime-",
            )
                .toAbsolutePath()
        val home =
            root.resolve(
                "home",
            )
        val lifetimeRoot =
            root.resolve(
                "machine-lifetime",
            )
        val providerSource =
            "a".repeat(
                40,
            )
        val nextSource =
            "b".repeat(
                40,
            )

        try {
            CanonicalVexLifeHomeFixture.form(
                home = home,
                deviceSuffix = "java-lifetime",
                lineageSuffix = "java-lifetime",
            )
            val provider =
                installImage(
                    root = root.resolve("provider"),
                    home = home,
                    sourceSha = providerSource,
                    runtimeModules = modules(),
                )
            val next =
                installImage(
                    root = root.resolve("next"),
                    home = home,
                    sourceSha = nextSource,
                    runtimeModules = modules(),
                )
            val required =
                observe(
                    next,
                    nextSource,
                )

            val first =
                VexLifeProdMacJavaLifetime.convergeFromInstalledProvider(
                    productHome = home,
                    providerSourceSha = providerSource,
                    requiredImage = required,
                    lifetimeRoot = lifetimeRoot,
                    copier = testCopier(),
                )

            assertTrue(first.effectsPerformed)
            assertEquals(providerSource, first.providerSourceSha)
            assertTrue(
                Path.of(
                    first.javaPath,
                )
                    .startsWith(
                        lifetimeRoot,
                    ),
            )
            assertFalse(
                Path.of(
                    first.javaPath,
                )
                    .startsWith(
                        Path.of(
                            provider.imagePath,
                        ),
                    ),
            )

            // Retired Product images may disappear; stable Java custody is independent.
            Path.of(
                    provider.imagePath,
                )
                .toFile()
                .deleteRecursively()

            val observed =
                VexLifeProdMacJavaLifetime.requireCompatible(
                    requiredImage = required,
                    lifetimeRoot = lifetimeRoot,
                )
            assertEquals(first.javaPath, observed.javaPath)
            assertEquals(providerSource, observed.providerSourceSha)

            val second =
                VexLifeProdMacJavaLifetime.convergeFromInstalledProvider(
                    productHome = home,
                    providerSourceSha = nextSource,
                    requiredImage = required,
                    lifetimeRoot = lifetimeRoot,
                    copier = testCopier(),
                )
            assertFalse(second.effectsPerformed)
            assertEquals(first.javaPath, second.javaPath)
            assertEquals(
                providerSource,
                second.providerSourceSha,
                "Product source rotation must not rotate a still-compatible Java engine",
            )
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun genuineModuleExpansionRequiresOneNewStableJavaGeneration() {
        val root =
            Files.createTempDirectory(
                "vexlife-mac-java-lifetime-upgrade-",
            )
                .toAbsolutePath()
        val home =
            root.resolve(
                "home",
            )
        val lifetimeRoot =
            root.resolve(
                "machine-lifetime",
            )
        val oldSource =
            "c".repeat(
                40,
            )
        val nextSource =
            "d".repeat(
                40,
            )

        try {
            CanonicalVexLifeHomeFixture.form(
                home = home,
                deviceSuffix = "java-upgrade",
                lineageSuffix = "java-upgrade",
            )
            val old =
                installImage(
                    root = root.resolve("old"),
                    home = home,
                    sourceSha = oldSource,
                    runtimeModules =
                        listOf(
                            "java.base",
                            "java.net.http",
                            "jdk.crypto.ec",
                            "jdk.httpserver",
                        ),
                )
            val next =
                installImage(
                    root = root.resolve("next"),
                    home = home,
                    sourceSha = nextSource,
                    runtimeModules =
                        modules(),
                )
            val oldRequired =
                observe(
                    old,
                    oldSource,
                )
            val nextRequired =
                observe(
                    next,
                    nextSource,
                )

            val first =
                VexLifeProdMacJavaLifetime.convergeFromInstalledProvider(
                    productHome = home,
                    providerSourceSha = oldSource,
                    requiredImage = oldRequired,
                    lifetimeRoot = lifetimeRoot,
                    copier = testCopier(),
                )
            assertEquals(oldSource, first.providerSourceSha)

            assertFailsWith<IllegalArgumentException> {
                VexLifeProdMacJavaLifetime.requireCompatible(
                    requiredImage = nextRequired,
                    lifetimeRoot = lifetimeRoot,
                )
            }

            val upgraded =
                VexLifeProdMacJavaLifetime.convergeFromInstalledProvider(
                    productHome = home,
                    providerSourceSha = nextSource,
                    requiredImage = nextRequired,
                    lifetimeRoot = lifetimeRoot,
                    copier = testCopier(),
                )

            assertTrue(upgraded.effectsPerformed)
            assertEquals(nextSource, upgraded.providerSourceSha)
            assertTrue(upgraded.javaPath != first.javaPath)
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    private fun modules(): List<String> =
        listOf(
            "java.base",
            "java.desktop",
            "java.net.http",
            "jdk.crypto.ec",
            "jdk.httpserver",
        )

    private fun testCopier(): VexLifeProdMacJavaLifetime.RuntimeCopier =
        VexLifeProdMacJavaLifetime.RuntimeCopier {
            sourceRuntime,
            targetRuntime,
            ->
            sourceRuntime
                .toFile()
                .copyRecursively(
                    targetRuntime.toFile(),
                )
            Files.setPosixFilePermissions(
                targetRuntime.resolve(
                    "bin/java",
                ),
                Files.getPosixFilePermissions(
                    sourceRuntime.resolve(
                        "bin/java",
                    ),
                ),
            )
        }

    private fun installImage(
        root: Path,
        home: Path,
        sourceSha: String,
        runtimeModules: List<String>,
    ): VexLifeProdPrivateServiceImageInstaller.Result {
        Files.createDirectory(
            root,
        )
        formImage(
            root = root,
            sourceSha = sourceSha,
            runtimeModules = runtimeModules,
        )
        return VexLifeProdPrivateServiceImageInstaller.install(
            sourceImageRoot = root,
            productHome = home,
            expectedSourceSha = sourceSha,
        )
    }

    private fun observe(
        installed: VexLifeProdPrivateServiceImageInstaller.Result,
        sourceSha: String,
    ): VexLifeProdPrivateServiceImage.Observation =
        VexLifeProdPrivateServiceImage.observe(
            imageRoot =
                Path.of(
                    installed.imagePath,
                ),
            expectedSourceSha = sourceSha,
        )

    private fun formImage(
        root: Path,
        sourceSha: String,
        runtimeModules: List<String>,
    ) {
        Files.createDirectories(root.resolve("runtime/bin"))
        Files.createDirectories(root.resolve("runtime/lib"))
        Files.createDirectories(root.resolve("lib"))
        Files.createDirectories(root.resolve("bin"))
        Files.createDirectories(root.resolve("setup"))

        val java = root.resolve("runtime/bin/java")
        val modulesFile = root.resolve("runtime/lib/modules")
        val launcher = root.resolve(VexLifeProdPrivateServiceImage.LauncherRelativePath)
        val setupLauncher = root.resolve(VexLifeProdPrivateServiceImage.SetupLauncherRelativePath)
        val setupWindow = root.resolve(VexLifeProdPrivateServiceImage.SetupWindowRelativePath)
        val app = root.resolve("lib/vexlife-prod.jar")
        val dependency = root.resolve("lib/kotlin-stdlib.jar")

        Files.writeString(java, "fake-java-$sourceSha-" + runtimeModules.joinToString(","))
        Files.writeString(modulesFile, "fake-modules-" + runtimeModules.joinToString(","))
        Files.writeString(launcher, "#!/bin/bash\nexit 0\n")
        Files.writeString(setupLauncher, "#!/bin/bash\nexit 0\n")
        Files.writeString(setupWindow, "use framework \"AppKit\"\n-- java lifetime test\n")
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
                runtimeModules = runtimeModules.sorted(),
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

    private fun sha256(path: Path): String {
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
}

// [VXG RealForever]
