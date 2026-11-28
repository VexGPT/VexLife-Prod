package vexlife.localasr

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VexLifeLocalAsrMacBundledRuntimeRegressionTest {
    @Test
    fun activeMacProviderUsesLivedQualifiedHomeCustodyEvenWhenReleaseAlsoCarriesSeed() {
        if (System.getProperty("os.name") != "Mac OS X") {
            return
        }

        val home =
            Files.createTempDirectory(
                "vexlife-local-asr-home-provider-",
            )
                .toAbsolutePath()
        val imageRoot =
            Files.createTempDirectory(
                "vexlife-local-asr-bundled-provider-seed-",
            )
                .toAbsolutePath()
        val priorProperty =
            System.getProperty(
                VexLifeLocalAsrMacArm64ProviderRealization.PrivateServiceImageRootProperty,
            )

        try {
            fun writeProvider(root: Path): Path {
                val bin =
                    root.resolve(
                        "bin",
                    )
                Files.createDirectories(
                    bin,
                )
                val executable =
                    bin.resolve(
                        "whisper-server",
                    )
                Files.writeString(
                    executable,
                    "fake-whisper-server",
                )
                Files.setPosixFilePermissions(
                    executable,
                    setOf(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.OWNER_EXECUTE,
                    ),
                )
                Files.writeString(
                    root.resolve(
                        "provider-source.txt",
                    ),
                    VexLifeLocalAsrWhisperCppProcessRealization.ProviderSourceSha +
                        "\n",
                )
                return executable
            }

            val homeProviderRoot =
                home.resolve(
                    VexLifeLocalAsrWhisperCppProcessRealization.ProviderBaseRootRelativePath,
                )
            val homeExecutable =
                writeProvider(
                    homeProviderRoot,
                )

            val bundledProviderRoot =
                imageRoot.resolve(
                    VexLifeLocalAsrMacArm64ProviderRealization.BundledProviderRootRelativePath,
                )
            writeProvider(
                bundledProviderRoot,
            )

            System.setProperty(
                VexLifeLocalAsrMacArm64ProviderRealization.PrivateServiceImageRootProperty,
                imageRoot.toString(),
            )

            val observed =
                VexLifeLocalAsrMacArm64ProviderRealization.observeProvider(
                    productHome =
                        home,
                )

            assertTrue(
                observed.ready,
                "the exact Home-owned provider should remain the active Mac runtime",
            )
            assertEquals(
                homeExecutable
                    .toAbsolutePath()
                    .normalize(),
                Path.of(
                        observed.executablePath,
                    )
                    .toAbsolutePath()
                    .normalize(),
            )
            assertTrue(
                Path.of(
                        observed.providerRootPath,
                    )
                    .toAbsolutePath()
                    .normalize()
                    .startsWith(
                        home,
                    ),
                "downloaded release-provider provenance must not silently replace the lived-qualified Home runtime",
            )
        } finally {
            if (
                priorProperty ==
                    null
            ) {
                System.clearProperty(
                    VexLifeLocalAsrMacArm64ProviderRealization.PrivateServiceImageRootProperty,
                )
            } else {
                System.setProperty(
                    VexLifeLocalAsrMacArm64ProviderRealization.PrivateServiceImageRootProperty,
                    priorProperty,
                )
            }
            home
                .toFile()
                .deleteRecursively()
            imageRoot
                .toFile()
                .deleteRecursively()
        }
    }
}

// [VXG RealForever]
