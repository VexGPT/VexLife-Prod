package vexlife.platform.windows

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class VexLifeWindowsPredecessorRuntimeBindingTest {
    private fun base(
        activeModelBundleRef: String? = null,
        generationRef: String? = null,
        modelProfileRef: String? = null,
    ) =
        VexLifeWindowsPredecessorRuntimeBinding.Value(
            schemaVersion = VexLifeWindowsPredecessorRuntimeBinding.SchemaV1,
            state = VexLifeWindowsPredecessorRuntimeBinding.QualifiedState,
            profileRef = VexLifeWindowsPredecessorRuntimeBinding.WindowsProfileRef,
            activeModelBundleRef = activeModelBundleRef,
            generationRef = generationRef,
            modelProfileRef = modelProfileRef,
            endpoint = VexLifeWindowsPredecessorRuntimeBinding.Endpoint,
            requestModel = VexLifeWindowsPredecessorRuntimeBinding.RequestModel,
            activeArtifactRef = VexLifeWindowsPredecessorRuntimeBinding.ActiveArtifactRef,
            runtimeDependencyRef = VexLifeWindowsPredecessorRuntimeBinding.RuntimeDependencyRef,
            runtimePid = 1234,
            runtimeExecutablePath = "C:\\Users\\test\\.vexlife\\runtime\\llama-cpp-b10107\\llama-server.exe",
            runtimeExecutableSha256 = VexLifeWindowsPredecessorRuntimeBinding.RuntimeExecutableSha256,
            runtimeExecutableSha256SourcePinned =
                VexLifeWindowsPredecessorRuntimeBinding.RuntimeExecutableSha256,
            runtimeArguments =
                listOf(
                    "-m",
                    "C:\\Users\\test\\.vexlife\\models\\Qwen_Qwen3.5-4B-Q4_K_M.gguf",
                    "--host",
                    "127.0.0.1",
                    "--port",
                    "18080",
                ),
            runtimeMaterializationRoot =
                "C:\\Users\\test\\.vexlife\\runtime\\llama-cpp-b10107",
            modelPath =
                "C:\\Users\\test\\.vexlife\\models\\Qwen_Qwen3.5-4B-Q4_K_M.gguf",
            projectorPath =
                "C:\\Users\\test\\.vexlife\\models\\mmproj-Qwen_Qwen3.5-4B-bf16.gguf",
            qualificationReceiptRef = "receipt.vexlife.initialization.synthetic",
            automaticDownload = false,
            automaticActivation = false,
        )

    @Test
    fun olderQualifiedBindingIsRecognizedWithoutPretendingItIsCurrent() {
        val observed =
            assertIs<VexLifeWindowsPredecessorRuntimeBinding.Observation.Recognized>(
                VexLifeWindowsPredecessorRuntimeBinding.evaluate(
                    base(),
                ),
            )

        assertEquals(
            VexLifeWindowsPredecessorRuntimeBinding.Currentness
                .LEGACY_QUALIFIED_NEEDS_CURRENTNESS_RECONCILIATION,
            observed.currentness,
        )
        assertEquals(
            "RELEASE_QUALIFIED",
            observed.sourceStanding,
        )
    }

    @Test
    fun fullyCurrentPredecessorFieldsAreRecognizedAsExact() {
        val observed =
            assertIs<VexLifeWindowsPredecessorRuntimeBinding.Observation.Recognized>(
                VexLifeWindowsPredecessorRuntimeBinding.evaluate(
                    base(
                        activeModelBundleRef =
                            VexLifeWindowsPredecessorRuntimeBinding.ModelBundleRef,
                        generationRef =
                            VexLifeWindowsPredecessorRuntimeBinding.GenerationRef,
                        modelProfileRef =
                            VexLifeWindowsPredecessorRuntimeBinding.ModelProfileRef,
                    ),
                ),
            )

        assertEquals(
            VexLifeWindowsPredecessorRuntimeBinding.Currentness
                .EXACT_PREDECESSOR_CURRENT_SOURCE_BINDING,
            observed.currentness,
        )
    }

    @Test
    fun foreignOrDriftedQualifiedBindingIsHeldRatherThanAdopted() {
        assertIs<VexLifeWindowsPredecessorRuntimeBinding.Observation.HeldMismatch>(
            VexLifeWindowsPredecessorRuntimeBinding.evaluate(
                base()
                    .copy(
                        runtimeDependencyRef = "runtime.other",
                    ),
            ),
        )
    }

    @Test
    fun partialCurrentnessFieldsAreHeldRatherThanFilledInByGuess() {
        assertIs<VexLifeWindowsPredecessorRuntimeBinding.Observation.HeldMismatch>(
            VexLifeWindowsPredecessorRuntimeBinding.evaluate(
                base(
                    activeModelBundleRef =
                        VexLifeWindowsPredecessorRuntimeBinding.ModelBundleRef,
                ),
            ),
        )
    }
}

// [VXG RealForever]
