package vexlife.platform.windows

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vexlife.capability.VexLifeCapabilityAtlas
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
            HereticRuntimeQualification.qualified(
                vexReachQualified =
                    true,
            )
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
        assertEquals(
            HereticRuntimeQualification.VexReachQualificationRef,
            evidence.qualification.vexReachQualificationRef,
        )

        val bindingEvidence =
            VexLifeWindowsHereticRuntime.modelConfigurationEvidence(
                evidence,
            )
        assertEquals(
            realization.realizationRef,
            bindingEvidence.realizationRef,
        )
        assertEquals(
            realization.artifact.artifactRef,
            bindingEvidence.artifactRef,
        )
        assertEquals(
            realization.runtime.runtimeAdapterRef,
            bindingEvidence.runtimeAdapterRef,
        )
        assertEquals(
            realization.runtime.runtimeClass,
            bindingEvidence.runtimeClass,
        )
        assertEquals(
            16_384,
            bindingEvidence.contextWindowTokens,
        )
    }

    @Test
    fun exactCrossProcessRuntimeIsRecoverableOnlyWhenItsParentIsGone() {
        val exact =
            VexLifeWindowsHereticRuntime.classifyExistingProcess(
                endpointOccupied = true,
                endpointOwnedByReceiptProcess = true,
                receiptExact = true,
                processLive = true,
                commandExact = true,
                argumentsExact = true,
                startInstantExact = true,
                parentLive = false,
                healthExact = true,
                modelExposureExact = true,
            )
        assertEquals(
            VexLifeWindowsHereticRuntime.ExistingProcessDisposition.ABANDONED_EXACT,
            exact,
        )

        val active =
            VexLifeWindowsHereticRuntime.classifyExistingProcess(
                endpointOccupied = true,
                endpointOwnedByReceiptProcess = true,
                receiptExact = true,
                processLive = true,
                commandExact = true,
                argumentsExact = true,
                startInstantExact = true,
                parentLive = true,
                healthExact = true,
                modelExposureExact = true,
            )
        assertEquals(
            VexLifeWindowsHereticRuntime.ExistingProcessDisposition.ACTIVE_EXACT,
            active,
        )

        val ambiguous =
            VexLifeWindowsHereticRuntime.classifyExistingProcess(
                endpointOccupied = true,
                endpointOwnedByReceiptProcess = true,
                receiptExact = true,
                processLive = true,
                commandExact = true,
                argumentsExact = true,
                startInstantExact = false,
                parentLive = false,
                healthExact = true,
                modelExposureExact = true,
            )
        assertEquals(
            VexLifeWindowsHereticRuntime.ExistingProcessDisposition.HELD_UNKNOWN,
            ambiguous,
        )

        val absent =
            VexLifeWindowsHereticRuntime.classifyExistingProcess(
                endpointOccupied = false,
                endpointOwnedByReceiptProcess = false,
                receiptExact = false,
                processLive = false,
                commandExact = false,
                argumentsExact = false,
                startInstantExact = false,
                parentLive = false,
                healthExact = false,
                modelExposureExact = false,
            )
        assertEquals(
            VexLifeWindowsHereticRuntime.ExistingProcessDisposition.ABSENT,
            absent,
        )
    }

    @Test
    fun exactReceiptPidMustOwnTheRuntimeEndpointBeforeRetirementIsAdmitted() {
        val held =
            VexLifeWindowsHereticRuntime.classifyExistingProcess(
                endpointOccupied = true,
                endpointOwnedByReceiptProcess = false,
                receiptExact = true,
                processLive = true,
                commandExact = true,
                argumentsExact = true,
                startInstantExact = true,
                parentLive = false,
                healthExact = false,
                modelExposureExact = false,
            )

        assertEquals(
            VexLifeWindowsHereticRuntime.ExistingProcessDisposition.HELD_UNKNOWN,
            held,
        )
    }

    @Test
    fun windowsNativeObservationCarriesOneProcessAndListenerIdentity() {
        val observed =
            VexLifeWindowsHereticRuntime.parseNativeProcessObservation(
                """
                {
                  "processId":38264,
                  "parentProcessId":23144,
                  "parentLive":false,
                  "executablePath":"C:\\\\Users\\\\victo\\\\.vexlife\\\\runtime\\\\llama-server.exe",
                  "commandLineArguments":[
                    "C:\\\\Users\\\\victo\\\\.vexlife\\\\runtime\\\\llama-server.exe",
                    "-m",
                    "C:\\\\Users\\\\victo\\\\.vexlife\\\\model.gguf",
                    "--alias",
                    "default_model"
                  ],
                  "creationInstant":"2026-11-18T17:40:02.714Z",
                  "listenerPids":[38264]
                }
                """.trimIndent(),
            )

        assertEquals(
            38264L,
            observed.processId,
        )
        assertEquals(
            23144L,
            observed.parentProcessId,
        )
        assertFalse(
            observed.parentLive,
        )
        assertEquals(
            listOf(
                38264L,
            ),
            observed.listenerPids,
        )
        assertEquals(
            "default_model",
            observed.commandLineArguments.last(),
        )
    }

    @Test
    fun windowsVexReachQualificationEarnsAtlasListThenDerivedDescribe() {
        val home =
            Files.createTempDirectory(
                "windows-vex-reach-qualification-",
            )
                .toAbsolutePath()
        val derived =
            requireNotNull(
                VexLifeCapabilityAtlas
                    .definitions()
                    .singleOrNull {
                        definition ->
                        definition.actions
                            .singleOrNull()
                            ?.action ==
                            "test"
                    },
            )
        val providerStages =
            mutableListOf(
                """
                VEXLIFE_CAPABILITY_REQUEST/v1
                {"capability":"atlas","action":"list","arguments":{}}
                """.trimIndent(),
                """
                VEXLIFE_CAPABILITY_REQUEST/v1
                {"capability":"atlas","action":"describe","arguments":{"handle":"${derived.handle}"}}
                """.trimIndent(),
            )
        var stages =
            0

        val qualified =
            VexLifeWindowsHereticRuntime.qualifyVexReach(
                productHome = home,
                invokeStage = {
                    systemMessage,
                    userMessage,
                    maxTokens,
                    exchanges,
                    ->
                    assertTrue(
                        systemMessage.contains(
                            "VEXLIFE_CAPABILITY_REQUEST/v1",
                        ),
                    )
                    assertTrue(
                        userMessage.contains(
                            "capability atlas",
                        ),
                    )
                    assertEquals(
                        2048,
                        maxTokens,
                    )
                    assertEquals(
                        stages,
                        exchanges.size,
                    )
                    stages +=
                        1
                    if (
                        providerStages.isEmpty()
                    ) {
                        null
                    } else {
                        providerStages.removeAt(
                            0,
                        )
                    }
                },
            )

        assertTrue(
            qualified,
        )
        assertEquals(
            2,
            stages,
        )
        assertTrue(
            providerStages.isEmpty(),
        )
    }

    @Test
    fun windowsVexReachQualificationStopsDuplicateControlInsteadOfLooping() {
        val home =
            Files.createTempDirectory(
                "windows-vex-reach-duplicate-",
            )
                .toAbsolutePath()
        val repeated =
            """
            VEXLIFE_CAPABILITY_REQUEST/v1
            {"capability":"atlas","action":"list","arguments":{}}
            """.trimIndent()
        var stages =
            0

        val qualified =
            VexLifeWindowsHereticRuntime.qualifyVexReach(
                productHome = home,
                invokeStage = {
                    _,
                    _,
                    _,
                    _,
                    ->
                    stages +=
                        1
                    repeated
                },
            )

        assertFalse(
            qualified,
        )
        assertEquals(
            2,
            stages,
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
