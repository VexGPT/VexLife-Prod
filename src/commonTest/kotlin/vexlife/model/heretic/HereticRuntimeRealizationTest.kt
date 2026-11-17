package vexlife.model.heretic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vexlife.platform.VexLifePlatformRealization

class HereticRuntimeRealizationTest {
    @Test
    fun everyCurrentCatalogModelHasDeliberateMacArm64Coverage() {
        assertEquals(
            HereticModelCatalog.Entries,
            HereticRuntimeRealization.modelsFor(
                HereticRuntimeRealization.MacArm64,
            ),
        )

        HereticModelCatalog.Entries.forEach { model ->
            val realization =
                HereticRuntimeRealization.require(
                    modelRef = model.modelRef,
                    target = HereticRuntimeRealization.MacArm64,
                )
            assertEquals(model.modelRef, realization.modelRef)
            assertEquals(
                model.identity.modelProfileRef,
                realization.modelProfileRef,
            )
        }
    }

    @Test
    fun windowsX64CoverageIsExplicitAndDoesNotInheritTheCatalog() {
        assertEquals(
            listOf(
                HereticModelCatalog.Models.Qwen4B,
            ),
            HereticRuntimeRealization.modelsFor(
                HereticRuntimeRealization.WindowsX64,
            ),
        )
        assertTrue(
            HereticRuntimeRealization.supports(
                modelRef = HereticModelCatalog.Models.Qwen4B.modelRef,
                target = HereticRuntimeRealization.WindowsX64,
            ),
        )
        assertFalse(
            HereticRuntimeRealization.supports(
                modelRef = HereticModelCatalog.Models.Qwen9B.modelRef,
                target = HereticRuntimeRealization.WindowsX64,
            ),
        )
        assertFalse(
            HereticRuntimeRealization.supports(
                modelRef = HereticModelCatalog.Models.Qwen27B.modelRef,
                target = HereticRuntimeRealization.WindowsX64,
            ),
        )
        assertFalse(
            HereticRuntimeRealization.supports(
                modelRef = HereticModelCatalog.Models.GptOss20B.modelRef,
                target = HereticRuntimeRealization.WindowsX64,
            ),
        )
    }

    @Test
    fun windows4BRealizationPinsTheLivedCpuRuntimeAndGguf() {
        val realization =
            HereticRuntimeRealization.WindowsQwen4B
        val binary =
            requireNotNull(
                realization.runtimeBinary,
            )

        assertEquals(
            "model-Q4_K_M.gguf",
            realization.artifact.primaryFilename,
        )
        assertEquals(
            2_790_000_800L,
            realization.artifact.expectedPrimaryBytes,
        )
        assertEquals(
            "7b95847ce1684f98d96f13bad2daaeffcfd2f95ea2ff3821ee68943590a10159",
            realization.artifact.contentSetSha256,
        )
        assertEquals(
            "llama-b10107-bin-win-cpu-x64.zip",
            binary.archiveFilename,
        )
        assertEquals(
            18_213_827L,
            binary.archiveBytes,
        )
        assertEquals(
            "52133a0a5a8f6035b1bdd2f89c3425ea8b742413d9bdb9a2dee30e3a1681b18c",
            binary.archiveSha256,
        )
        assertEquals(
            "af3e56d6bdb84a9b6bd50f6ad748809370bc29be42d58088041408b2933d80f7",
            binary.executableSha256,
        )
        assertEquals(
            "adapter.runtime.llama-cpp.windows-x64.cpu.b10107.001",
            realization.runtime.runtimeAdapterRef,
        )
        assertEquals(
            "http://127.0.0.1:18080",
            realization.runtime.endpoint,
        )
        val launch =
            requireNotNull(
                realization.runtime.livedLaunchProfile,
            )
        assertEquals(
            16_384,
            launch.contextTokens,
        )
        assertTrue(
            launch.jinja,
        )
        assertNull(
            launch.gpuOffloadLayers,
        )
    }

    @Test
    fun targetResolutionNeverFallsAcrossArchitectureOrPlatform() {
        val windowsArm64 =
            VexLifePlatformRealization.Target(
                platform =
                    VexLifePlatformRealization.PlatformFamily.WINDOWS,
                architecture =
                    VexLifePlatformRealization.Architecture.ARM64,
            )

        assertNull(
            HereticRuntimeRealization.resolve(
                modelRef = HereticModelCatalog.Models.Qwen4B.modelRef,
                target = windowsArm64,
            ),
        )
        assertNull(
            HereticRuntimeRealization.resolve(
                modelRef = HereticModelCatalog.Models.Qwen4B.modelRef,
                target =
                    VexLifePlatformRealization.Target(
                        platform =
                            VexLifePlatformRealization.PlatformFamily.ANDROID,
                        architecture =
                            VexLifePlatformRealization.Architecture.ARM64,
                    ),
            ),
        )
    }
}

// [VXG RealForever]
