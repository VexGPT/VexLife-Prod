package vexlife.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeRealization
import vexlife.model.heretic.HereticRuntimeState
import vexlife.partner.VexLifePartnerConnectionProjection
import vexlife.partner.VexLifePartnerModelProfileCatalog
import vexlife.partner.VexLifePartnerSupportQualification

class VexLifeInferenceSessionAdmissionTest {
    private fun localRuntimeEvidence(
        model: HereticModelCatalog.Entry = HereticModelCatalog.Models.Qwen27B,
    ): HereticModelConfiguration.QualifiedRuntimeEvidence {
        val realization =
            HereticRuntimeRealization.require(
                modelRef = model.modelRef,
                target = HereticRuntimeRealization.MacArm64,
            )
        return HereticModelConfiguration.QualifiedRuntimeEvidence(
            runtimeReceiptRef = "receipt.vexlife-prod.inference-session-admission.local",
            runtimeState = HereticRuntimeState.Qualified,
            modelRef = model.modelRef,
            realizationRef = realization.realizationRef,
            modelProfileRef = realization.modelProfileRef,
            artifactRef = realization.artifact.artifactRef,
            artifactContentSetSha256 = realization.artifact.contentSetSha256,
            runtimeAdapterRef = realization.runtime.runtimeAdapterRef,
            runtimeClass = realization.runtime.runtimeClass,
            endpoint = realization.runtime.endpoint,
            requestModel = realization.runtime.requestModel,
            qualification = HereticRuntimeQualification.qualified(),
            contextWindowTokens = 32_768,
        )
    }

    private fun support(
        providerRef: String,
        modelRef: String,
        live: Boolean,
    ): VexLifePartnerSupportQualification.Projection =
        VexLifePartnerSupportQualification.project(
            VexLifePartnerSupportQualification.Evidence(
                providerRef = providerRef,
                modelRef = modelRef,
                contractFixtureProofRef = "proof.partner.contract.fixture",
                adapterConformanceProofRef = "proof.partner.adapter.fixture",
                vexRelationshipConformanceProofRef = "proof.partner.vex.fixture",
                liveQualificationReceiptRef =
                    if (
                        live
                    ) {
                        "receipt.partner.live-qualified"
                    } else {
                        null
                    },
            ),
        )

    private fun connection(
        providerRef: String,
        modelRef: String,
        qualified: Boolean,
    ): VexLifePartnerConnectionProjection.Projection =
        VexLifePartnerConnectionProjection.project(
            VexLifePartnerConnectionProjection.Observation(
                providerRef = providerRef,
                modelRef = modelRef,
                credential = VexLifePartnerConnectionProjection.CredentialStanding.ACCEPTED,
                account = VexLifePartnerConnectionProjection.AccountStanding.USABLE,
                service = VexLifePartnerConnectionProjection.ServiceStanding.READY,
                modelAccess = VexLifePartnerConnectionProjection.ModelAccessStanding.AVAILABLE,
                invocation =
                    if (
                        qualified
                    ) {
                        VexLifePartnerConnectionProjection.InvocationStanding.QUALIFIED
                    } else {
                        VexLifePartnerConnectionProjection.InvocationStanding.NEVER_QUALIFIED
                    },
            ),
        )

    private fun partnerOffering(
        targeted: Boolean,
        serving: Boolean,
        connectionQualified: Boolean = true,
        supportLive: Boolean = true,
    ): VexLifeModelSourceProjection.PartnerOffering {
        val profile =
            VexLifePartnerModelProfileCatalog.OpenAiGpt62Sol
        return VexLifeModelSourceProjection.PartnerOffering(
            handle = profile.profileRef,
            providerRef = profile.providerRef,
            modelRef = profile.modelRef,
            targeted = targeted,
            serving = serving,
            connection =
                connection(
                    providerRef = profile.providerRef,
                    modelRef = profile.modelRef,
                    qualified = connectionQualified,
                ),
            support =
                support(
                    providerRef = profile.providerRef,
                    modelRef = profile.modelRef,
                    live = supportLive,
                ),
        )
    }

    private fun current(
        localTargeted: Boolean,
        localServing: Boolean,
        partner: VexLifeModelSourceProjection.PartnerOffering? = null,
    ): VexLifeInferenceCurrentState.Projection {
        val models =
            VexLifeModelSourceProjection.project(
                local =
                    listOf(
                        VexLifeModelSourceProjection.LocalOffering(
                            handle = "heretic-27b",
                            modelRef = HereticModelCatalog.Models.Qwen27B.modelRef,
                            targeted = localTargeted,
                            serving = localServing,
                        ),
                    ),
                partner =
                    listOfNotNull(
                        partner,
                    ),
            )
        return VexLifeInferenceCurrentState.Projection(
            models = models,
            inference =
                VexLifeInferenceSelection.project(
                    models,
                ),
        )
    }

    private fun partnerCurrentness(
        offering: VexLifeModelSourceProjection.PartnerOffering,
    ): VexLifePartnerModelsCurrentState.Projection =
        VexLifePartnerModelsCurrentState.Projection(
            configuredProviders = emptyList(),
            connectionByProfileRef =
                mapOf(
                    offering.handle to
                        offering.connection,
                ),
            offerings = listOf(offering),
        )

    @Test
    fun pendingPartnerTargetDoesNotStealTurnFromQualifiedLocalServingSession() {
        val partner =
            partnerOffering(
                targeted = true,
                serving = false,
            )
        val projected =
            VexLifeInferenceSessionAdmission.project(
                current =
                    current(
                        localTargeted = false,
                        localServing = true,
                        partner = partner,
                    ),
                localRuntimeEvidence = localRuntimeEvidence(),
                partner = partnerCurrentness(partner),
            )

        assertEquals(
            VexLifeInferenceSessionAdmission.Standing.QUALIFIED,
            projected.standing,
        )
        assertEquals(
            VexLifeModelSourceProjection.SourceKind.LOCAL,
            projected.qualified?.coordinate?.sourceKind,
        )
        assertEquals(
            HereticModelCatalog.Models.Qwen27B.modelRef,
            projected.qualified?.coordinate?.modelRef,
        )
        assertNull(
            projected.holdReason,
        )
    }

    @Test
    fun localServingRequiresExactQualifiedRuntimeEvidence() {
        val noEvidence =
            VexLifeInferenceSessionAdmission.project(
                current =
                    current(
                        localTargeted = true,
                        localServing = true,
                    ),
                localRuntimeEvidence = null,
                partner =
                    VexLifePartnerModelsCurrentState.Projection(
                        configuredProviders = emptyList(),
                        connectionByProfileRef = emptyMap(),
                        offerings = emptyList(),
                    ),
            )
        assertEquals(
            VexLifeInferenceSessionAdmission.HoldReason.LOCAL_RUNTIME_EVIDENCE_MISSING,
            noEvidence.holdReason,
        )

        val wrongModel =
            VexLifeInferenceSessionAdmission.project(
                current =
                    current(
                        localTargeted = true,
                        localServing = true,
                    ),
                localRuntimeEvidence =
                    localRuntimeEvidence(
                        HereticModelCatalog.Models.Qwen4B,
                    ),
                partner =
                    VexLifePartnerModelsCurrentState.Projection(
                        configuredProviders = emptyList(),
                        connectionByProfileRef = emptyMap(),
                        offerings = emptyList(),
                    ),
            )
        assertEquals(
            VexLifeInferenceSessionAdmission.HoldReason.LOCAL_RUNTIME_IDENTITY_MISMATCH,
            wrongModel.holdReason,
        )
    }

    @Test
    fun liveQualifiedPartnerServingSessionIsAdmitted() {
        val partner =
            partnerOffering(
                targeted = true,
                serving = true,
            )
        val projected =
            VexLifeInferenceSessionAdmission.project(
                current =
                    current(
                        localTargeted = false,
                        localServing = false,
                        partner = partner,
                    ),
                localRuntimeEvidence = null,
                partner = partnerCurrentness(partner),
            )

        assertEquals(
            VexLifeInferenceSessionAdmission.Standing.QUALIFIED,
            projected.standing,
        )
        assertEquals(
            VexLifePartnerModelProfileCatalog.OpenAiGpt62Sol.profileRef,
            projected.qualified?.profileRef,
        )
        assertEquals(
            "receipt.partner.live-qualified",
            projected.qualified?.qualificationRef,
        )
        assertEquals(
            VexLifeModelSourceProjection.SourceKind.PARTNER,
            projected.qualified?.coordinate?.sourceKind,
        )
    }

    @Test
    fun partnerServingRemainsHeldUntilConnectionAndSupportAreLiveQualified() {
        val readyToAttempt =
            partnerOffering(
                targeted = true,
                serving = true,
                connectionQualified = false,
                supportLive = true,
            )
        val connectionHeld =
            VexLifeInferenceSessionAdmission.project(
                current =
                    current(
                        localTargeted = false,
                        localServing = false,
                        partner = readyToAttempt,
                    ),
                localRuntimeEvidence = null,
                partner = partnerCurrentness(readyToAttempt),
            )
        assertEquals(
            VexLifeInferenceSessionAdmission.HoldReason.PARTNER_CONNECTION_NOT_QUALIFIED,
            connectionHeld.holdReason,
        )

        val liveUnqualified =
            partnerOffering(
                targeted = true,
                serving = true,
                connectionQualified = true,
                supportLive = false,
            )
        val supportHeld =
            VexLifeInferenceSessionAdmission.project(
                current =
                    current(
                        localTargeted = false,
                        localServing = false,
                        partner = liveUnqualified,
                    ),
                localRuntimeEvidence = null,
                partner = partnerCurrentness(liveUnqualified),
            )
        assertEquals(
            VexLifeInferenceSessionAdmission.HoldReason.PARTNER_SUPPORT_NOT_LIVE_QUALIFIED,
            supportHeld.holdReason,
        )
    }

    @Test
    fun noServingInferenceIsHeldWithoutInventingOneFromTarget() {
        val partner =
            partnerOffering(
                targeted = true,
                serving = false,
            )
        val projected =
            VexLifeInferenceSessionAdmission.project(
                current =
                    current(
                        localTargeted = false,
                        localServing = false,
                        partner = partner,
                    ),
                localRuntimeEvidence = localRuntimeEvidence(),
                partner = partnerCurrentness(partner),
            )

        assertEquals(
            VexLifeInferenceSessionAdmission.Standing.HELD,
            projected.standing,
        )
        assertEquals(
            VexLifeInferenceSessionAdmission.HoldReason.NO_SERVING_INFERENCE,
            projected.holdReason,
        )
        assertNull(
            projected.qualified,
        )
    }
}

// [VXG RealForever]
