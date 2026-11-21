package vexlife.foundational

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import vexlife.companion.DirectCompanionCancellation
import vexlife.companion.DirectCompanionConnection
import vexlife.companion.DirectPromptContext
import vexlife.context.VexLifeContextProfile
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

    private fun extendedProfile(): VexLifeContextProfile.Effective {
        val current =
            requireNotNull(
                VexLifeFoundationalField.readCurrent() as?
                    VexLifeFoundationalField.Reading.Current,
            )
        return VexLifeContextProfile.Effective(
            profileRef = "profile.vexlife.context.foundation-prefill-test",
            revision = 3,
            definition =
                VexLifeContextProfile.Definition(
                    name = "Foundation prefill test",
                    foundationMode =
                        VexLifeContextProfile.FoundationMode.EXTEND_PRODUCT_DEFAULT,
                    foundationText =
                        "Prefer explanations that make the causal relationship visible.",
                ),
            basedOnProductFoundation =
                VexLifeContextProfile.ProductFoundationIdentity(
                    version = current.body.version,
                    bodySha256 = current.bodySha256,
                ),
            fingerprint = "b".repeat(64),
            formedAt = "2026-11-21T15:00:00Z",
            productDefault = false,
            productFoundationChangedSinceSave = false,
        )
    }

    @Test
    fun prefillReadsExactCurrentFoundationFromItsOwnerInsteadOfOwningPromptText() {
        val profile =
            extendedProfile()
        val expected =
            DirectPromptContext.projectFoundationalPrefill(
                foundationalField =
                    VexLifeFoundationalField.readCurrent(),
                contextProfile = profile,
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
                contextProfile = profile,
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
                                    expected.systemMessageSha256,
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
            expected.systemMessageSha256,
            result.systemMessageSha256,
        )
        assertEquals(
            expected.productFoundation.bodySha256,
            result.bodySha256,
        )
        assertEquals(
            profile.profileRef,
            result.contextProfileRef,
        )
        assertEquals(
            profile.fingerprint,
            result.contextProfileFingerprint,
        )
        assertEquals(
            expected.profileFoundationSystemMessageSha256,
            result.profileFoundationSystemMessageSha256,
        )
        assertEquals(
            expected.productFoundation.systemMessageSha256,
            result.productFoundationSystemMessageSha256,
        )
        kotlin.test.assertTrue(
            VexLifeFoundationalField.ProviderHeader in
                expectedContent,
        )
        kotlin.test.assertTrue(
            DirectPromptContext.ContextProfileFoundationHeader in
                expectedContent,
        )
        kotlin.test.assertTrue(
            expectedContent.indexOf(
                VexLifeFoundationalField.ProviderHeader,
            ) <
                expectedContent.indexOf(
                    DirectPromptContext.ContextProfileFoundationHeader,
                ),
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
