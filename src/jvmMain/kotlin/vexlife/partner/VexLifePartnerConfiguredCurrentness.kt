package vexlife.partner

/**
 * Read-only restart/currentness projection for configured Partner credentials.
 *
 * Durable Home settings own only the opaque credential coordinate. Protected host custody owns secret
 * presence. This projection joins those truths without reading, repairing, deleting, or inventing a
 * secret.
 */
object VexLifePartnerConfiguredCurrentness {
    enum class CoordinateStanding {
        NOT_CONFIGURED,
        PRESENT,
        DANGLING_REF,
    }

    data class ProviderState(
        val providerRef: String,
        val credentialRef: String?,
        val accountScope: VexLifePartnerAccountScope.Selection?,
        val coordinateStanding: CoordinateStanding,
        val credentialStanding: VexLifePartnerConnectionProjection.CredentialStanding,
    ) {
        init {
            requireNotNull(
                VexLifePartnerProviderCatalog.resolve(
                    providerRef,
                ),
            ) {
                "configured Partner currentness requires one source-owned built-in provider"
            }

            when (
                coordinateStanding
            ) {
                CoordinateStanding.NOT_CONFIGURED ->
                    require(
                        credentialRef ==
                            null &&
                            accountScope ==
                            null &&
                            credentialStanding ==
                            VexLifePartnerConnectionProjection.CredentialStanding.MISSING,
                    ) {
                        "unconfigured Partner provider may not carry a durable credential coordinate"
                    }

                CoordinateStanding.PRESENT ->
                    require(
                        credentialRef !=
                            null &&
                            accountScope
                                ?.providerRef ==
                                providerRef &&
                            credentialStanding ==
                            VexLifePartnerConnectionProjection.CredentialStanding.PRESENT_UNVERIFIED,
                    ) {
                        "present Partner credential must retain exact provider/ref/account scope"
                    }

                CoordinateStanding.DANGLING_REF ->
                    require(
                        credentialRef !=
                            null &&
                            accountScope
                                ?.providerRef ==
                                providerRef &&
                            credentialStanding ==
                            VexLifePartnerConnectionProjection.CredentialStanding.MISSING,
                    ) {
                        "dangling Partner credential ref must remain visible while secret is missing"
                    }
            }
        }
    }

    fun observe(
        settings: VexLifePartnerConnectionSettings.Value,
        host: VexLifePartnerCredentialCustody.ProtectedHostStore,
    ): List<ProviderState> =
        VexLifePartnerProviderCatalog
            .definitions()
            .map { provider ->
                val configured =
                    VexLifePartnerConnectionSettings.entryFor(
                        value = settings,
                        providerRef = provider.providerRef,
                    )

                if (
                    configured ==
                    null
                ) {
                    ProviderState(
                        providerRef = provider.providerRef,
                        credentialRef = null,
                        accountScope = null,
                        coordinateStanding =
                            CoordinateStanding.NOT_CONFIGURED,
                        credentialStanding =
                            VexLifePartnerConnectionProjection.CredentialStanding.MISSING,
                    )
                } else {
                    val coordinate =
                        VexLifePartnerCredentialCustody.Coordinate(
                            providerRef = configured.providerRef,
                            credentialRef = configured.credentialRef,
                        )
                    if (
                        host.contains(
                            coordinate,
                        )
                    ) {
                        ProviderState(
                            providerRef = configured.providerRef,
                            credentialRef = configured.credentialRef,
                            accountScope = configured.accountScope,
                            coordinateStanding =
                                CoordinateStanding.PRESENT,
                            credentialStanding =
                                VexLifePartnerConnectionProjection.CredentialStanding.PRESENT_UNVERIFIED,
                        )
                    } else {
                        ProviderState(
                            providerRef = configured.providerRef,
                            credentialRef = configured.credentialRef,
                            accountScope = configured.accountScope,
                            coordinateStanding =
                                CoordinateStanding.DANGLING_REF,
                            credentialStanding =
                                VexLifePartnerConnectionProjection.CredentialStanding.MISSING,
                        )
                    }
                }
            }

    /**
     * Initial model-specific connection truth on restart.
     *
     * Provider account/service/model availability is deliberately UNKNOWN until a rightful provider
     * observation refreshes it. A historical prior success is not replayed as current truth.
     */
    fun initialConnection(
        provider: ProviderState,
        modelRef: String,
    ): VexLifePartnerConnectionProjection.Projection =
        VexLifePartnerConnectionProjection.project(
            VexLifePartnerConnectionProjection.Observation(
                providerRef = provider.providerRef,
                modelRef = modelRef,
                credential = provider.credentialStanding,
                account =
                    VexLifePartnerConnectionProjection.AccountStanding.UNKNOWN,
                service =
                    VexLifePartnerConnectionProjection.ServiceStanding.UNKNOWN,
                modelAccess =
                    VexLifePartnerConnectionProjection.ModelAccessStanding.UNKNOWN,
                invocation =
                    VexLifePartnerConnectionProjection.InvocationStanding.NEVER_QUALIFIED,
            ),
        )
}

// [VXG RealForever]
