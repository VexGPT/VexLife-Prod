package vexlife.localasr

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VexLifeLocalAsrMacBundledRuntimeRegressionTest {
    @Test
    fun bundledSeedConvergesOnceIntoStableHomeProviderAndPreservesExtendedAttributes() {
        if (System.getProperty("os.name") != "Mac OS X") {
            return
        }

        val home =
            Files.createTempDirectory(
                "vexlife-local-asr-stable-home-provider-",
            )
                .toAbsolutePath()
        val imageRoot =
            Files.createTempDirectory(
                "vexlife-local-asr-bundled-provider-seed-",
            )
                .toAbsolutePath()

        try {
            val seedRoot =
                imageRoot.resolve(
                    VexLifeLocalAsrMacArm64ProviderRealization.BundledProviderRootRelativePath,
                )
            val seedBin =
                seedRoot.resolve(
                    "bin",
                )
            Files.createDirectories(
                seedBin,
            )
            val seedExecutable =
                seedBin.resolve(
                    "whisper-server",
                )
            Files.writeString(
                seedExecutable,
                "fake-whisper-server",
            )
            Files.setPosixFilePermissions(
                seedExecutable,
                setOf(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE,
                ),
            )
            Files.writeString(
                seedRoot.resolve(
                    "provider-source.txt",
                ),
                VexLifeLocalAsrWhisperCppProcessRealization.ProviderSourceSha +
                    "\n",
            )

            val xattrName =
                "com.vexlife.test.local-asr-seed"
            val xattrValue =
                "preserve-me"
            val setXattr =
                ProcessBuilder(
                    "/usr/bin/xattr",
                    "-w",
                    xattrName,
                    xattrValue,
                    seedExecutable.toString(),
                )
                    .start()
            assertEquals(
                0,
                setXattr.waitFor(),
            )

            val first =
                VexLifeLocalAsrMacArm64ProviderRealization.convergeBundledProvider(
                    productHome =
                        home,
                    privateServiceImageRoot =
                        imageRoot,
                )

            assertTrue(
                first.observation.ready,
            )
            assertTrue(
                first.extractionPerformed,
            )
            val stableExecutable =
                Path.of(
                    first.observation.executablePath,
                )
                    .toAbsolutePath()
                    .normalize()
            assertTrue(
                stableExecutable.startsWith(
                    home,
                ),
                "Mac runtime provider must use stable Home custody rather than the per-source private image",
            )
            assertEquals(
                home.resolve(
                        VexLifeLocalAsrMacArm64ProviderRealization.StableProviderRootRelativePath,
                    )
                    .resolve(
                        "bin/whisper-server",
                    )
                    .toAbsolutePath()
                    .normalize(),
                stableExecutable,
            )

            val readXattr =
                ProcessBuilder(
                    "/usr/bin/xattr",
                    "-p",
                    xattrName,
                    stableExecutable.toString(),
                )
                    .redirectErrorStream(
                        true,
                    )
                    .start()
            val observedXattr =
                readXattr.inputStream
                    .bufferedReader()
                    .use {
                        it.readText()
                    }
                    .trim()
            assertEquals(
                0,
                readXattr.waitFor(),
            )
            assertEquals(
                xattrValue,
                observedXattr,
                "Mac provisioning must preserve extended attributes instead of laundering downloaded trust metadata",
            )

            imageRoot
                .toFile()
                .deleteRecursively()

            val reused =
                VexLifeLocalAsrMacArm64ProviderRealization.convergeBundledProvider(
                    productHome =
                        home,
                    privateServiceImageRoot =
                        imageRoot,
                )
            assertTrue(
                reused.observation.ready,
            )
            assertFalse(
                reused.extractionPerformed,
                "a Product update must reuse the exact stable Mac provider instead of copying a new per-source runtime",
            )
            assertEquals(
                "REUSED_VERIFIED_STABLE_MAC_PROVIDER",
                reused.archiveDisposition,
            )
        } finally {
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
