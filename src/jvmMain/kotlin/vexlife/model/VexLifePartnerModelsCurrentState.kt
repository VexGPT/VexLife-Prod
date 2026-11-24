package vexlife.model

import vexlife.partner.VexLifePartnerConfiguredCurrentness
import vexlife.partner.VexLifePartnerConnectionProjection
import vexlife.partner.VexLifePartnerConnectionSettings
import vexlife.partner.VexLifePartnerCredentialCustody
import vexlife.partner.VexLifePartnerLiveQualification
import vexlife.partner.VexLifePartnerModelProfileCatalog
import vexlife.partner.VexLifePartnerSupportQualification

/**
 * Read-only initial Partner Models state after Home/host observation.
 *
 * This projection owns no credential mutation, provider request, model selection, or support proof.
 * It combines:
 *
 * durable non-secret connection settings
 * + protected-host credential presence
 * + source-owned Partner profile inventory
 * + externally supplied current support proof projection
 * -> Partner model offering rows
 */
object VexLifePartnerModelsCurrentState {
    data class Input(
        val settings: VexLifePartnerConnectionSettings.Value,
        val support: List<VexLifePartnerSupportQualification.Projection>,
        val targeted: VexLifePartnerModelInventory.Coordinate? = null,
        val serving: VexLifePartnerModelInventory.Coordinate? = null,
        /**
         * Process-current qualified evidence for [serving], when that exact relationship was earned
         * in this process.
         *
         * This is deliberately not restart persistence. If current configured credential/account
         * truth no longer matches the qualification, projection falls back to unqualified
         * currentness rather than replaying historical success.
         */
        val liveQualifiedServing: VexLifePartnerLiveQualification.Outcome.Qualified? = null,
    )

    data class Projection(
        val configuredProviders: List<VexLifePartnerConfiguredCurrentness.ProviderState>,
        val connectionByProfileRef: Map<String, VexLifePartnerConnectionProjection.Projection>,
        val offerings: List<VexLifeModelSourceProjection.PartnerOffering>,
    )

    fun project(
        input: Input,
        host: VexLifePartnerCredentialCustody.ProtectedHostStore,
    ): Projection {
        val providers =
            VexLifePartnerConfiguredCurrentness.observe(
                settings = input.settings,
                host = host,
            )
        val providerByRef =
            providers.associateBy {
                it.providerRef
            }

        val liveQualifiedServing =
            currentLiveQualifiedServing(
                input = input,
                providers = providers,
            )

        val connections =
            VexLifePartnerModelProfileCatalog
                .definitions()
                .associate { profile ->
                    val initial =
                        VexLifePartnerConfiguredCurrentness.initialConnection(
                            provider =
                                requireNotNull(
                                    providerByRef[
                                        profile.providerRef
                                    ],
                                ) {
                                    "Partner Models currentness lacks built-in provider state"
                                },
                            modelRef = profile.modelRef,
                        )
                    profile.profileRef to
                        if (
                            liveQualifiedServing
                                ?.receipt
                                ?.profileRef ==
                            profile.profileRef
                        ) {
                            liveQualifiedServing.connection
                        } else {
                            initial
                        }
                }

        val support =
            input.support.map {
                current ->
                if (
                    liveQualifiedServing
                        ?.support
                        ?.let {
                            it.providerRef ==
                                current.providerRef &&
                                it.modelRef ==
                                current.modelRef
                        } ==
                    true
                ) {
                    liveQualifiedServing.support
                } else {
                    current
                }
            }

        val offerings =
            VexLifePartnerModelInventory.offerings(
                VexLifePartnerModelInventory.Input(
                    connections =
                        connections.values.toList(),
                    support = support,
                    targeted = input.targeted,
                    serving = input.serving,
                ),
            )

        return Projection(
            configuredProviders = providers,
            connectionByProfileRef = connections,
            offerings = offerings,
        )
    }

    private fun currentLiveQualifiedServing(
        input: Input,
        providers: List<VexLifePartnerConfiguredCurrentness.ProviderState>,
    ): VexLifePartnerLiveQualification.Outcome.Qualified? {
        val live =
            input.liveQualifiedServing
                ?: return null
        val serving =
            requireNotNull(
                input.serving,
            ) {
                "live Partner serving qualification requires one serving coordinate"
            }
        require(
            serving.providerRef ==
                live.receipt.providerRef &&
                serving.modelRef ==
                live.receipt.modelRef,
        ) {
            "live Partner qualification does not bind the projected serving coordinate"
        }
        val profile =
            requireNotNull(
                VexLifePartnerModelProfileCatalog.resolve(
                    providerRef = serving.providerRef,
                    modelRef = serving.modelRef,
                ),
            ) {
                "live Partner qualification requires one source-owned profile"
            }
        require(
            profile.profileRef ==
                live.receipt.profileRef,
        ) {
            "live Partner qualification profile does not bind the projected serving profile"
        }

        val currentProvider =
            providers.single {
                it.providerRef ==
                    live.receipt.providerRef
            }
        return live.takeIf {
            currentProvider.coordinateStanding ==
                VexLifePartnerConfiguredCurrentness.CoordinateStanding.PRESENT &&
                currentProvider.credentialRef ==
                live.receipt.credentialRef &&
                currentProvider.accountScope ==
                live.receipt.accountScope
        }
    }
}

// [VXG RealForever]
