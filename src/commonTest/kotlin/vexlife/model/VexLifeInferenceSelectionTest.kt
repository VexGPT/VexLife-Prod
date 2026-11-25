package vexlife.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vexlife.partner.VexLifePartnerConnectionProjection
import vexlife.partner.VexLifePartnerSupportQualification

class VexLifeInferenceSelectionTest {
    private fun connection(
        providerRef: String,
        modelRef: String,
    ): VexLifePartnerConnectionProjection.Projection =
        VexLifePartnerConnectionProjection.project(
            VexLifePartnerConnectionProjection.Observation(
                providerRef = providerRef,
                modelRef = modelRef,
                credential =
                    VexLifePartnerConnectionProjection.CredentialStanding.ACCEPTED,
                account =
                    VexLifePartnerConnectionProjection.AccountStanding.USABLE,
                service =
                    VexLifePartnerConnectionProjection.ServiceStanding.READY,
                modelAccess =
                    VexLifePartnerConnectionProjection.ModelAccessStanding.AVAILABLE,
                invocation =
                    VexLifePartnerConnectionProjection.InvocationStanding.NEVER_QUALIFIED,
            ),
        )

    private fun support(
        providerRef: String,
        modelRef: String,
    ): VexLifePartnerSupportQualification.Projection =
        VexLifePartnerSupportQualification.project(
            VexLifePartnerSupportQualification.Evidence(
                providerRef = providerRef,
                modelRef = modelRef,
                contractFixtureProofRef =
                    "proof.partner.contract.fixture",
                adapterConformanceProofRef =
                    "proof.partner.adapter.fixture",
                vexRelationshipConformanceProofRef =
                    "proof.partner.vex.fixture",
                liveQualificationReceiptRef =
                    null,
            ),
        )

    private fun partner(
        handle: String,
        providerRef: String,
        modelRef: String,
        targeted: Boolean,
        serving: Boolean,
    ): VexLifeModelSourceProjection.PartnerOffering =
        VexLifeModelSourceProjection.PartnerOffering(
            handle = handle,
            providerRef = providerRef,
            modelRef = modelRef,
            targeted = targeted,
            serving = serving,
            connection =
                connection(
                    providerRef = providerRef,
                    modelRef = modelRef,
                ),
            support =
                support(
                    providerRef = providerRef,
                    modelRef = modelRef,
                ),
        )

    @Test
    fun localTargetAndServingRemainOneInferenceCoordinate() {
        val models =
            VexLifeModelSourceProjection.project(
                local =
                    listOf(
                        VexLifeModelSourceProjection.LocalOffering(
                            handle = "heretic-4b",
                            modelRef = "model.heretic.4b",
                            targeted = true,
                            serving = true,
                        ),
                    ),
                partner =
                    emptyList(),
            )

        val current =
            VexLifeInferenceSelection.project(
                models,
            )

        assertEquals(
            VexLifeModelSourceProjection.SourceKind.LOCAL,
            current.targeted?.sourceKind,
        )
        assertEquals(
            current.targeted,
            current.serving,
        )
        assertFalse(
            current.switchPending,
        )
        assertFalse(
            current.sourceSwitchPending,
        )
        assertNull(
            current.targeted?.providerRef,
        )
    }

    @Test
    fun partnerTargetOverLocalServingIsOneCrossSourcePendingSwitch() {
        val models =
            VexLifeModelSourceProjection.project(
                local =
                    listOf(
                        VexLifeModelSourceProjection.LocalOffering(
                            handle = "heretic-4b",
                            modelRef = "model.heretic.4b",
                            targeted = false,
                            serving = true,
                        ),
                    ),
                partner =
                    listOf(
                        partner(
                            handle = "anthropic-sonnet",
                            providerRef = "provider.anthropic.api",
                            modelRef = "model.anthropic.sonnet",
                            targeted = true,
                            serving = false,
                        ),
                    ),
            )

        val current =
            VexLifeInferenceSelection.project(
                models,
            )

        assertEquals(
            VexLifeModelSourceProjection.SourceKind.PARTNER,
            current.targeted?.sourceKind,
        )
        assertEquals(
            "provider.anthropic.api",
            current.targeted?.providerRef,
        )
        assertEquals(
            VexLifeModelSourceProjection.SourceKind.LOCAL,
            current.serving?.sourceKind,
        )
        assertTrue(
            current.switchPending,
        )
        assertTrue(
            current.sourceSwitchPending,
        )
    }

    @Test
    fun sameSourceModelChangeIsPendingWithoutInventingSourceSwitch() {
        val models =
            VexLifeModelSourceProjection.project(
                local =
                    listOf(
                        VexLifeModelSourceProjection.LocalOffering(
                            handle = "heretic-4b",
                            modelRef = "model.heretic.4b",
                            targeted = true,
                            serving = false,
                        ),
                        VexLifeModelSourceProjection.LocalOffering(
                            handle = "heretic-9b",
                            modelRef = "model.heretic.9b",
                            targeted = false,
                            serving = true,
                        ),
                    ),
                partner =
                    emptyList(),
            )

        val current =
            VexLifeInferenceSelection.project(
                models,
            )

        assertTrue(
            current.switchPending,
        )
        assertFalse(
            current.sourceSwitchPending,
        )
    }

    @Test
    fun servingWithoutTargetDoesNotInventHumanSelectionOrPendingSwitch() {
        val models =
            VexLifeModelSourceProjection.project(
                local =
                    listOf(
                        VexLifeModelSourceProjection.LocalOffering(
                            handle = "heretic-4b",
                            modelRef = "model.heretic.4b",
                            targeted = false,
                            serving = true,
                        ),
                    ),
                partner =
                    emptyList(),
            )

        val current =
            VexLifeInferenceSelection.project(
                models,
            )

        assertNull(
            current.targeted,
        )
        assertEquals(
            "heretic-4b",
            current.serving?.handle,
        )
        assertFalse(
            current.switchPending,
        )
        assertFalse(
            current.sourceSwitchPending,
        )
    }

    @Test
    fun targetWithoutServingIsPendingButDoesNotInventPriorSource() {
        val models =
            VexLifeModelSourceProjection.project(
                local =
                    emptyList(),
                partner =
                    listOf(
                        partner(
                            handle = "openai-gpt",
                            providerRef = "provider.openai.api",
                            modelRef = "model.openai.gpt",
                            targeted = true,
                            serving = false,
                        ),
                    ),
            )

        val current =
            VexLifeInferenceSelection.project(
                models,
            )

        assertEquals(
            "openai-gpt",
            current.targeted?.handle,
        )
        assertNull(
            current.serving,
        )
        assertTrue(
            current.switchPending,
        )
        assertFalse(
            current.sourceSwitchPending,
        )
    }

    @Test
    fun targetResolutionReturnsExactCurrentRowWithoutPerformingSwitch() {
        val providerRef =
            "provider.kimi.open-platform.international"
        val models =
            VexLifeModelSourceProjection.project(
                local =
                    emptyList(),
                partner =
                    listOf(
                        partner(
                            handle = "kimi-k3",
                            providerRef = providerRef,
                            modelRef = "model.kimi.k3",
                            targeted = false,
                            serving = false,
                        ),
                    ),
            )

        val target =
            VexLifeInferenceSelection.requireTarget(
                models = models,
                handle = "kimi-k3",
            )

        assertEquals(
            "model.kimi.k3",
            target.modelRef,
        )
        assertEquals(
            providerRef,
            target.providerRef,
        )
        assertEquals(
            VexLifeModelSourceProjection.SourceKind.PARTNER,
            target.sourceKind,
        )
    }
}

// [VXG RealForever]
