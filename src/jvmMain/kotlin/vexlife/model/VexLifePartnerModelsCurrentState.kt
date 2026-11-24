package vexlife.model

import vexlife.partner.VexLifePartnerConfiguredCurrentness
import vexlife.partner.VexLifePartnerConnectionProjection
import vexlife.partner.VexLifePartnerConnectionSettings
import vexlife.partner.VexLifePartnerCredentialCustody
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

        val connections =
            VexLifePartnerModelProfileCatalog
                .definitions()
                .associate { profile ->
                    profile.profileRef to
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
                }

        val offerings =
            VexLifePartnerModelInventory.offerings(
                VexLifePartnerModelInventory.Input(
                    connections =
                        connections.values.toList(),
                    support = input.support,
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
}

// [VXG RealForever]
