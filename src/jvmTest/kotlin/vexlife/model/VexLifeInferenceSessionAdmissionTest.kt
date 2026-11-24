package vexlife.model

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeRealization
import vexlife.model.heretic.HereticRuntimeState
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.model.heretic.runtime.FileHereticModelConfigurationStore
import vexlife.partner.VexLifePartnerAccountScope
import vexlife.partner.VexLifePartnerConfiguredCurrentness
import vexlife.partner.VexLifePartnerConnectionProjection
import vexlife.partner.VexLifePartnerModelProfileCatalog
import vexlife.partner.VexLifePartnerSupportQualification

class VexLifeInferenceSessionAdmissionTest {
    private val gib =
        1024L * 1024L * 1024L

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

    private fun localModelsProjection(
        servingModelRef: String? = HereticModelCatalog.Models.Qwen27B.modelRef,
        evidence: HereticModelConfiguration.QualifiedRuntimeEvidence? = null,
    ): VexLifeModels.Projection {
        val parent =
            Files.createTempDirectory(
                "vexlife-inference-session-admission-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                ".vexlife",
            )
        return try {
            val fixture =
                CanonicalVexLifeHomeFixture.form(
                    home,
                )
            FileHereticModelConfigurationStore(
                home,
            )
                .selectHeretic(
                    fixture.manifest,
                )
            VexLifeModels(
                productHome = home,
                servingModelRef = {
                    servingModelRef
                },
                currentQualifiedRuntimeEvidence = {
                    evidence
                },
                totalMemoryBytes = {
                    16 * gib
                },
                runtimeRealizations =
                    HereticRuntimeRealization.Resolver(
                        HereticRuntimeRealization.TargetSource {
                            HereticRuntimeRealization.MacArm64
                        },
                    ),
            )
                .use {
                    it.projection()
                }
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
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
        credentialRef: String =
            "credential.vexlife.partner.inference-session-current",
    ): VexLifePartnerModelsCurrentState.Projection =
        VexLifePartnerModelsCurrentState.Projection(
            configuredProviders =
                listOf(
                    VexLifePartnerConfiguredCurrentness.ProviderState(
                        providerRef = offering.providerRef,
                        credentialRef = credentialRef,
                        accountScope =
                            VexLifePartnerAccountScope.Selection(
                                providerRef = offering.providerRef,
                            ),
                        coordinateStanding =
                            VexLifePartnerConfiguredCurrentness.CoordinateStanding.PRESENT,
                        credentialStanding =
                            VexLifePartnerConnectionProjection.CredentialStanding.PRESENT_UNVERIFIED,
                    ),
                ),
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
                local =
                    localModelsProjection(
                        evidence = localRuntimeEvidence(),
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
                local =
                    localModelsProjection(
                        evidence = null,
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

        val wrongEvidence =
            localRuntimeEvidence(
                HereticModelCatalog.Models.Qwen4B,
            )
        val wrongModel =
            VexLifeInferenceSessionAdmission.project(
                current =
                    current(
                        localTargeted = true,
                        localServing = true,
                    ),
                local =
                    localModelsProjection(
                        evidence = wrongEvidence,
                    ),
                localRuntimeEvidence =
                    wrongEvidence,
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
    fun localServingRejectsSameModelRefWithMismatchedRealizationIdentity() {
        val servingModel =
            HereticModelCatalog.Models.Qwen27B
        val wrongRealization =
            HereticRuntimeRealization.require(
                modelRef = HereticModelCatalog.Models.Qwen4B.modelRef,
                target = HereticRuntimeRealization.MacArm64,
            )
        val mismatched =
            HereticModelConfiguration.QualifiedRuntimeEvidence(
                runtimeReceiptRef =
                    "receipt.vexlife-prod.inference-session-admission.mismatched",
                runtimeState = HereticRuntimeState.Qualified,
                modelRef = servingModel.modelRef,
                realizationRef = wrongRealization.realizationRef,
                modelProfileRef = wrongRealization.modelProfileRef,
                artifactRef = wrongRealization.artifact.artifactRef,
                artifactContentSetSha256 = wrongRealization.artifact.contentSetSha256,
                runtimeAdapterRef = wrongRealization.runtime.runtimeAdapterRef,
                runtimeClass = wrongRealization.runtime.runtimeClass,
                endpoint = wrongRealization.runtime.endpoint,
                requestModel = wrongRealization.runtime.requestModel,
                qualification = HereticRuntimeQualification.qualified(),
            )

        val projected =
            VexLifeInferenceSessionAdmission.project(
                current =
                    current(
                        localTargeted = true,
                        localServing = true,
                    ),
                local =
                    localModelsProjection(
                        evidence = mismatched,
                    ),
                localRuntimeEvidence = mismatched,
                partner =
                    VexLifePartnerModelsCurrentState.Projection(
                        configuredProviders = emptyList(),
                        connectionByProfileRef = emptyMap(),
                        offerings = emptyList(),
                    ),
            )

        assertEquals(
            VexLifeInferenceSessionAdmission.HoldReason.LOCAL_RUNTIME_IDENTITY_MISMATCH,
            projected.holdReason,
        )
        assertNull(
            projected.qualified,
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
                local =
                    localModelsProjection(
                        servingModelRef = null,
                        evidence = null,
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
        assertEquals(
            "credential.vexlife.partner.inference-session-current",
            projected.qualified?.partnerCredentialRef,
        )
        assertEquals(
            VexLifePartnerModelProfileCatalog.OpenAiGpt62Sol.providerRef,
            projected.qualified?.partnerAccountScope?.providerRef,
        )
    }

    @Test
    fun partnerServingRequiresOneCurrentConfiguredCredentialCoordinate() {
        val partner =
            partnerOffering(
                targeted = true,
                serving = true,
            )
        val missingCredentialCurrentness =
            VexLifePartnerModelsCurrentState.Projection(
                configuredProviders = emptyList(),
                connectionByProfileRef =
                    mapOf(
                        partner.handle to
                            partner.connection,
                    ),
                offerings = listOf(partner),
            )

        val projected =
            VexLifeInferenceSessionAdmission.project(
                current =
                    current(
                        localTargeted = false,
                        localServing = false,
                        partner = partner,
                    ),
                local =
                    localModelsProjection(
                        servingModelRef = null,
                        evidence = null,
                    ),
                localRuntimeEvidence = null,
                partner = missingCredentialCurrentness,
            )

        assertEquals(
            VexLifeInferenceSessionAdmission.HoldReason.PARTNER_CURRENTNESS_MISSING,
            projected.holdReason,
        )
        assertNull(
            projected.qualified,
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
                local =
                    localModelsProjection(
                        servingModelRef = null,
                        evidence = null,
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
                local =
                    localModelsProjection(
                        servingModelRef = null,
                        evidence = null,
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
                local =
                    localModelsProjection(
                        servingModelRef = null,
                        evidence = null,
                    ),
                localRuntimeEvidence = null,
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
