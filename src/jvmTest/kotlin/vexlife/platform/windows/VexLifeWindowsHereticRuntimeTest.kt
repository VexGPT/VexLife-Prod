package vexlife.platform.windows

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeRealization

class VexLifeWindowsHereticRuntimeTest {
    @Test
    fun livedWindowsCpuLaunchProfileIsExactAndRequestsNoGpuOffload() {
        val realization =
            HereticRuntimeRealization.WindowsQwen4B
        val materialization =
            readyObservation(
                realization,
            )

        val arguments =
            VexLifeWindowsHereticRuntime.launchArguments(
                realization = realization,
                materialization = materialization,
            )

        assertEquals(
            listOf(
                materialization.runtimeExecutablePath,
                "-m",
                materialization.modelPath,
                "--alias",
                "default_model",
                "--host",
                "127.0.0.1",
                "--port",
                "18080",
                "--ctx-size",
                "16384",
                "--jinja",
            ),
            arguments,
        )
        assertFalse(
            "-ngl" in
                arguments,
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
        assertEquals(
            null,
            launch.gpuOffloadLayers,
        )
    }

    @Test
    fun qualifiedEvidenceNamesTheExactWindowsRealizationRatherThanMacRuntimeIdentity() {
        val realization =
            HereticRuntimeRealization.WindowsQwen4B
        val qualification =
            HereticRuntimeQualification.qualified()
        val evidence =
            VexLifeWindowsHereticRuntime.qualifiedEvidence(
                realization = realization,
                processPid = 4242L,
                qualification = qualification,
            )
        val binary =
            requireNotNull(
                realization.runtimeBinary,
            )

        assertEquals(
            "receipt.vexlife-prod.windows-heretic.4242",
            evidence.runtimeReceiptRef,
        )
        assertEquals(
            realization.realizationRef,
            evidence.realizationRef,
        )
        assertEquals(
            realization.modelRef,
            evidence.modelRef,
        )
        assertEquals(
            realization.artifact.artifactRef,
            evidence.artifactRef,
        )
        assertEquals(
            binary.binaryRef,
            evidence.runtimeBinaryRef,
        )
        assertEquals(
            binary.archiveSha256,
            evidence.runtimeArchiveSha256,
        )
        assertEquals(
            binary.executableSha256,
            evidence.runtimeExecutableSha256,
        )
        assertEquals(
            "adapter.runtime.llama-cpp.windows-x64.cpu.b10107.001",
            evidence.runtimeAdapterRef,
        )
        assertEquals(
            qualification,
            evidence.qualification,
        )
    }

    @Test
    fun launchArgumentsRejectAnotherRealizationCoordinate() {
        val realization =
            HereticRuntimeRealization.WindowsQwen4B
        val materialization =
            readyObservation(
                realization,
            )
                .copy(
                    realizationRef =
                        "realization.vexlife-prod.other",
                )

        val rejected =
            runCatching {
                VexLifeWindowsHereticRuntime.launchArguments(
                    realization = realization,
                    materialization = materialization,
                )
            }

        assertTrue(
            rejected.isFailure,
        )
    }

    private fun readyObservation(
        realization: HereticRuntimeRealization.Value,
    ) =
        VexLifeWindowsRuntimeMaterialization.Observation(
            realizationRef =
                realization.realizationRef,
            modelPath =
                "C:/VexLife-Test/model.gguf",
            modelState =
                VexLifeWindowsRuntimeMaterialization.FileState.VERIFIED,
            runtimeArchivePath =
                "C:/VexLife-Test/runtime.zip",
            runtimeArchiveState =
                VexLifeWindowsRuntimeMaterialization.FileState.VERIFIED,
            runtimeImagePath =
                "C:/VexLife-Test/runtime-image",
            runtimeImageState =
                VexLifeWindowsRuntimeMaterialization.FileState.VERIFIED,
            runtimeExecutablePath =
                "C:/VexLife-Test/runtime-image/llama-server.exe",
            runtimeExecutableState =
                VexLifeWindowsRuntimeMaterialization.FileState.VERIFIED,
            effectsPerformed =
                false,
        )
}

// [VXG RealForever]
