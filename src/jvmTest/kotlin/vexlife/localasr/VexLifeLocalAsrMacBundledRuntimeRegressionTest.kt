package vexlife.localasr

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VexLifeLocalAsrMacBundledRuntimeRegressionTest {
    @Test
    fun activeMacProviderStaysBundledWhenHomeHasNoProvider() {
        if (System.getProperty("os.name") != "Mac OS X") {
            return
        }

        val home =
            Files.createTempDirectory(
                "vexlife-local-asr-home-without-provider-",
            )
                .toAbsolutePath()
        val imageRoot =
            Files.createTempDirectory(
                "vexlife-local-asr-bundled-provider-",
            )
                .toAbsolutePath()
        val priorProperty =
            System.getProperty(
                VexLifeLocalAsrMacArm64ProviderRealization.PrivateServiceImageRootProperty,
            )

        try {
            val providerRoot =
                imageRoot.resolve(
                    VexLifeLocalAsrMacArm64ProviderRealization.BundledProviderRootRelativePath,
                )
            val providerBin =
                providerRoot.resolve(
                    "bin",
                )
            Files.createDirectories(
                providerBin,
            )
            val executable =
                providerBin.resolve(
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
                providerRoot.resolve(
                    "provider-source.txt",
                ),
                VexLifeLocalAsrWhisperCppProcessRealization.ProviderSourceSha +
                    "\n",
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
                "Mac VoiceInput must not become unavailable merely because Vex Home has no provider copy",
            )
            assertEquals(
                executable
                    .toAbsolutePath()
                    .normalize(),
                Path.of(
                        observed.executablePath,
                    )
                    .toAbsolutePath()
                    .normalize(),
            )
            assertTrue(
                !Path.of(
                        observed.providerRootPath,
                    )
                    .toAbsolutePath()
                    .normalize()
                    .startsWith(
                        home,
                    ),
                "the active Mac provider remains release-owned rather than silently rebinding to Home",
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
