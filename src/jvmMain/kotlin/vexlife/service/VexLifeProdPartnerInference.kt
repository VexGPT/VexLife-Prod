package vexlife.service

import java.nio.file.Path
import vexlife.companion.DirectCompanionTurn
import vexlife.conversation.VexLifeDirectMessageSemanticRelay
import vexlife.model.VexLifeInferenceCurrentState
import vexlife.model.VexLifeInferenceSelection
import vexlife.model.VexLifeInferenceSessionAdmission
import vexlife.model.VexLifeModelSourceProjection
import vexlife.model.VexLifeModels
import vexlife.model.VexLifePartnerModelInventory
import vexlife.model.VexLifePartnerModelsCurrentState
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.partner.VexLifePartnerConfiguredCurrentness
import vexlife.partner.VexLifePartnerConnectionSettingsStore
import vexlife.partner.VexLifePartnerCredentialCustody
import vexlife.partner.VexLifePartnerDeterministicSupport
import vexlife.partner.VexLifePartnerHostEffect
import vexlife.partner.VexLifePartnerLiveQualification
import vexlife.partner.VexLifePartnerModelProfileCatalog

/**
 * Service-facing composition of already-owned Partner inference relationships.
 *
 * This owner does not persist model choice, own credential bytes, perform provider fallback, mutate
 * Conversation/Memory, or create another qualification/currentness store. It only joins:
 *
 * Home-owned non-secret Partner settings
 * + protected-host credential presence/effect
 * + deterministic support evidence
 * + Models-owned staged/serving coordinates
 * + process-current live qualification
 * -> exact current serving-session admission and Partner turn realization.
 *
 * Permanent:
 *
 * SERVICE COMPOSITION != MODEL SELECTION OWNER
 * SERVICE COMPOSITION != PROVIDER FALLBACK
 * SERVICE COMPOSITION != CONVERSATION OWNER
 * HISTORICAL LIVE RECEIPT != CURRENT SERVING ADMISSION
 */
internal class VexLifeProdPartnerInference(
    productHome: Path,
    private val host: VexLifePartnerCredentialCustody.ProtectedHostStore,
    private val executor: VexLifePartnerHostEffect.Executor?,
) {
    data class Snapshot(
        val current: VexLifeInferenceCurrentState.Projection,
        val partner: VexLifePartnerModelsCurrentState.Projection,
        val admission: VexLifeInferenceSessionAdmission.Projection,
    )

    private val home =
        productHome
            .toAbsolutePath()
            .normalize()

    private val settings =
        VexLifePartnerConnectionSettingsStore(
            home,
        )

    fun observe(
        local: VexLifeModels.Projection,
        stagedTarget: VexLifeInferenceSelection.Coordinate?,
        partnerServing: VexLifeModels.PartnerServing?,
        localRuntimeEvidence: HereticModelConfiguration.QualifiedRuntimeEvidence?,
    ): Snapshot {
        val partner =
            partnerState(
                stagedTarget = stagedTarget,
                partnerServing = partnerServing,
            )
        val current =
            VexLifeInferenceCurrentState.project(
                productHome = home,
                local = local,
                partner = partner,
                stagedTargetOrNull = stagedTarget,
            )
        return Snapshot(
            current = current,
            partner = partner,
            admission =
                VexLifeInferenceSessionAdmission.project(
                    current = current,
                    local = local,
                    localRuntimeEvidence = localRuntimeEvidence,
                    partner = partner,
                ),
        )
    }

    fun qualify(
        target: VexLifeInferenceSelection.Coordinate,
    ): VexLifePartnerLiveQualification.Outcome {
        require(
            target.sourceKind ==
                VexLifeModelSourceProjection.SourceKind.PARTNER,
        ) {
            "Partner live qualification requires one staged Partner target"
        }
        val providerRef =
            requireNotNull(
                target.providerRef,
            )
        val profile =
            requireNotNull(
                VexLifePartnerModelProfileCatalog.resolve(
                    providerRef = providerRef,
                    modelRef = target.modelRef,
                ),
            ) {
                "Partner live qualification target is not one source-owned profile"
            }
        require(
            profile.profileRef ==
                target.handle,
        ) {
            "Partner live qualification target handle does not bind the source-owned profile"
        }

        val targetCoordinate =
            target.partnerCoordinate()
        val partner =
            VexLifePartnerModelsCurrentState.project(
                input =
                    VexLifePartnerModelsCurrentState.Input(
                        settings = settings.read(),
                        support =
                            VexLifePartnerDeterministicSupport.projections(),
                        targeted = targetCoordinate,
                    ),
                host = host,
            )
        val configuredProvider =
            partner.configuredProviders
                .single {
                    it.providerRef ==
                        providerRef
                }
        require(
            configuredProvider.coordinateStanding ==
                VexLifePartnerConfiguredCurrentness.CoordinateStanding.PRESENT,
        ) {
            "Partner Use requires one current configured protected credential"
        }
        val offering =
            partner.offerings
                .single {
                    it.handle ==
                        target.handle
                }

        return VexLifePartnerLiveQualification.qualify(
            profile = profile,
            configuredProvider = configuredProvider,
            connection = offering.connection,
            support = offering.support,
            executor =
                requireNotNull(
                    executor,
                ) {
                    "Partner protected-host execution is unavailable on this service realization"
                },
        )
    }

    fun partnerTextTurn(
        snapshot: Snapshot,
    ): DirectCompanionTurn.PartnerTextTurn {
        val qualified =
            requireNotNull(
                snapshot.admission.qualified,
            ) {
                "Partner turn requires one exact current qualified serving admission"
            }
        require(
            qualified.coordinate.sourceKind ==
                VexLifeModelSourceProjection.SourceKind.PARTNER,
        ) {
            "Partner turn cannot consume a Local serving admission"
        }
        val currentExecutor =
            requireNotNull(
                executor,
            ) {
                "Partner protected-host execution is unavailable on this service realization"
            }

        return DirectCompanionTurn.PartnerTextTurn(
            qualified = qualified,
            executor = currentExecutor,
            requestSemanticRelay =
                DirectCompanionTurn.SemanticRelayFactory {
                    messageRef,
                    originatorRef,
                    recipientRefs,
                    ->
                    VexLifeDirectMessageSemanticRelay.form(
                        messageRef = messageRef,
                        originatorRef = originatorRef,
                        originatorKind =
                            VexLifeDirectMessageSemanticRelay.OriginatorKind.HUMAN,
                        recipientRefs = recipientRefs,
                    )
                },
            responseSemanticRelay =
                DirectCompanionTurn.SemanticRelayFactory {
                    messageRef,
                    originatorRef,
                    recipientRefs,
                    ->
                    VexLifeDirectMessageSemanticRelay.form(
                        messageRef = messageRef,
                        originatorRef = originatorRef,
                        originatorKind =
                            VexLifeDirectMessageSemanticRelay.OriginatorKind.AI,
                        recipientRefs = recipientRefs,
                    )
                },
        )
    }

    private fun partnerState(
        stagedTarget: VexLifeInferenceSelection.Coordinate?,
        partnerServing: VexLifeModels.PartnerServing?,
    ): VexLifePartnerModelsCurrentState.Projection {
        val targeted =
            stagedTarget
                ?.takeIf {
                    it.sourceKind ==
                        VexLifeModelSourceProjection.SourceKind.PARTNER
                }
                ?.partnerCoordinate()
        val serving =
            partnerServing
                ?.coordinate
                ?.partnerCoordinate()

        return VexLifePartnerModelsCurrentState.project(
            input =
                VexLifePartnerModelsCurrentState.Input(
                    settings = settings.read(),
                    support =
                        VexLifePartnerDeterministicSupport.projections(),
                    targeted = targeted,
                    serving = serving,
                    liveQualifiedServing =
                        partnerServing
                            ?.qualification,
                ),
            host = host,
        )
    }

    private fun VexLifeInferenceSelection.Coordinate.partnerCoordinate():
        VexLifePartnerModelInventory.Coordinate {
        require(
            sourceKind ==
                VexLifeModelSourceProjection.SourceKind.PARTNER,
        ) {
            "only Partner inference coordinates have Partner inventory coordinates"
        }
        return VexLifePartnerModelInventory.Coordinate(
            providerRef =
                requireNotNull(
                    providerRef,
                ),
            modelRef = modelRef,
        )
    }

    companion object {
        fun unavailable(
            productHome: Path,
        ): VexLifeProdPartnerInference =
            VexLifeProdPartnerInference(
                productHome = productHome,
                host =
                    object :
                        VexLifePartnerCredentialCustody.ProtectedHostStore {
                        override fun store(
                            coordinate: VexLifePartnerCredentialCustody.Coordinate,
                            secret: CharArray,
                        ) {
                            error(
                                "Partner protected-host storage is unavailable",
                            )
                        }

                        override fun contains(
                            coordinate: VexLifePartnerCredentialCustody.Coordinate,
                        ): Boolean =
                            false

                        override fun delete(
                            coordinate: VexLifePartnerCredentialCustody.Coordinate,
                        ): Boolean =
                            false
                    },
                executor = null,
            )
    }
}

// [VXG RealForever]
