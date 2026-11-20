package vexlife.localasr

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VexLifeLocalAsrMacHomeRestorationTest {
    @Test
    fun activeMacProviderObservationPrefersVexHomeOverBundledApplicationProvider() {
        if (System.getProperty("os.name") != "Mac OS X") {
            return
        }

        val home = Files.createTempDirectory("vexlife-mac-home-provider-").toAbsolutePath()
        val imageRoot = Files.createTempDirectory("vexlife-mac-bundled-provider-").toAbsolutePath()
        val priorProperty =
            System.getProperty(
                VexLifeLocalAsrMacArm64ProviderRealization.PrivateServiceImageRootProperty,
            )

        try {
            val homeProviderRoot =
                home.resolve(
                    VexLifeLocalAsrWhisperCppProcessRealization.ProviderBaseRootRelativePath,
                )
            val homeBin = homeProviderRoot.resolve("bin")
            Files.createDirectories(homeBin)
            val homeExecutable = homeBin.resolve("whisper-server")
            Files.writeString(homeExecutable, "home-whisper")
            Files.setPosixFilePermissions(
                homeExecutable,
                setOf(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE,
                ),
            )
            Files.writeString(
                homeProviderRoot.resolve("provider-source.txt"),
                VexLifeLocalAsrWhisperCppProcessRealization.ProviderSourceSha + "\n",
            )

            val bundledProviderRoot =
                imageRoot.resolve(
                    VexLifeLocalAsrMacArm64ProviderRealization.BundledProviderRootRelativePath,
                )
            val bundledBin = bundledProviderRoot.resolve("bin")
            Files.createDirectories(bundledBin)
            val bundledExecutable = bundledBin.resolve("whisper-server")
            Files.writeString(bundledExecutable, "bundled-whisper")
            Files.setPosixFilePermissions(
                bundledExecutable,
                setOf(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE,
                ),
            )
            Files.writeString(
                bundledProviderRoot.resolve("provider-source.txt"),
                VexLifeLocalAsrWhisperCppProcessRealization.ProviderSourceSha + "\n",
            )

            System.setProperty(
                VexLifeLocalAsrMacArm64ProviderRealization.PrivateServiceImageRootProperty,
                imageRoot.toString(),
            )

            val observed =
                VexLifeLocalAsrMacArm64ProviderRealization.observeProvider(
                    productHome = home,
                )

            assertTrue(observed.ready)
            assertEquals(
                homeExecutable.toAbsolutePath().normalize(),
                Path.of(observed.executablePath).toAbsolutePath().normalize(),
            )
            assertTrue(
                Path.of(observed.providerRootPath)
                    .toAbsolutePath()
                    .normalize()
                    .startsWith(home),
            )
        } finally {
            if (priorProperty == null) {
                System.clearProperty(
                    VexLifeLocalAsrMacArm64ProviderRealization.PrivateServiceImageRootProperty,
                )
            } else {
                System.setProperty(
                    VexLifeLocalAsrMacArm64ProviderRealization.PrivateServiceImageRootProperty,
                    priorProperty,
                )
            }
            home.toFile().deleteRecursively()
            imageRoot.toFile().deleteRecursively()
        }
    }
}

// [VXG RealForever]
