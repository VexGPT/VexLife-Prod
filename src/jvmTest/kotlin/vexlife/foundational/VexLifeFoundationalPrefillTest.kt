package vexlife.foundational

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import vexlife.companion.DirectCompanionCancellation
import vexlife.companion.DirectCompanionConnection
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeRealization
import vexlife.model.heretic.HereticRuntimeState

class VexLifeFoundationalPrefillTest {
    private fun runtime(): HereticModelConfiguration.QualifiedRuntimeEvidence {
        val model =
            HereticModelCatalog.Models.Qwen4B
        val realization =
            requireNotNull(
                HereticRuntimeRealization.resolve(
                    model = model,
                    target = HereticRuntimeRealization.MacArm64,
                ),
            )
        return HereticModelConfiguration.QualifiedRuntimeEvidence(
            runtimeReceiptRef = "receipt.vexlife-prod.foundation-prefill.test",
            runtimeState = HereticRuntimeState.Qualified,
            modelRef = realization.modelRef,
            realizationRef = realization.realizationRef,
            modelProfileRef = realization.modelProfileRef,
            artifactRef = realization.artifact.artifactRef,
            artifactContentSetSha256 = realization.artifact.contentSetSha256,
            runtimeAdapterRef = realization.runtime.runtimeAdapterRef,
            runtimeClass = realization.runtime.runtimeClass,
            endpoint = realization.runtime.endpoint,
            requestModel = realization.runtime.requestModel,
            qualification = HereticRuntimeQualification.qualified(),
            contextWindowTokens =
                realization.runtime.livedLaunchProfile
                    ?.contextTokens,
        )
    }

    @Test
    fun prefillReadsExactCurrentFoundationFromItsOwnerInsteadOfOwningPromptText() {
        val expected =
            VexLifeFoundationalField.project(
                reading =
                    VexLifeFoundationalField.readCurrent(),
                availableTokens =
                    VexLifeFoundationalField.KernelTokenBudget,
            )
        val expectedContent =
            assertNotNull(
                expected.systemContent,
            )
        var providerContent: String? =
            null

        val result =
            VexLifeFoundationalPrefill.perform(
                runtime = runtime(),
                cancellation =
                    DirectCompanionCancellation(),
                providerPrefill =
                    VexLifeFoundationalPrefill.ProviderPrefill {
                        currentRuntime,
                        systemContent,
                        _,
                        ->
                        providerContent =
                            systemContent
                        DirectCompanionConnection.SystemPrefixPrefillResult.Performed(
                            runtimeReceiptRef = currentRuntime.runtimeReceiptRef,
                            modelRef = currentRuntime.modelRef,
                            systemMessageSha256 =
                                requireNotNull(
                                    expected.evidence.systemMessageSha256,
                                ),
                            requestBodySha256 = "0".repeat(64),
                            promptTokens = 3_100,
                            cachedPromptTokens = 0,
                            completionTokens = 1,
                            totalMillis = 123L,
                        )
                    },
            )

        assertEquals(
            expectedContent,
            providerContent,
        )
        assertEquals(
            expected.evidence.systemMessageSha256,
            result.systemMessageSha256,
        )
        assertEquals(
            expected.evidence.bodySha256,
            result.bodySha256,
        )
        assertEquals(
            VexLifeFoundationalPrefill.Disposition.PREFILLED,
            result.disposition,
        )
        assertEquals(
            3_100,
            result.providerPrefillTokens,
        )
    }

    @Test
    fun providerProfileHoldIsAStartupOptimizationHoldNotAProductFailure() {
        val result =
            VexLifeFoundationalPrefill.perform(
                runtime = runtime(),
                cancellation =
                    DirectCompanionCancellation(),
                providerPrefill =
                    VexLifeFoundationalPrefill.ProviderPrefill {
                        _,
                        _,
                        _,
                        ->
                        DirectCompanionConnection.SystemPrefixPrefillResult.Held(
                            "PROMPT_CACHE_NOT_ADMITTED",
                        )
                    },
            )

        assertEquals(
            VexLifeFoundationalPrefill.Disposition.HELD_PROVIDER_PROFILE,
            result.disposition,
        )
        assertEquals(
            "PROMPT_CACHE_NOT_ADMITTED",
            result.heldReasonOrNull,
        )
        assertEquals(
            false,
            result.effectsPerformed,
        )
    }
}

// [VXG RealForever]
