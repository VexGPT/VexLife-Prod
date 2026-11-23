package vexlife.partner

/**
 * Maps one already-classified provider invocation failure into the read-only Partner connection
 * observations that the failure actually establishes.
 *
 * This projection performs no retry, credential mutation, network effect, model switch, billing
 * action, or Conversation/Memory mutation.
 *
 * Permanent:
 *
 * PROVIDER FAILURE EVIDENCE != RETRY AUTHORITY
 * HTTP STATUS != CONNECTION MEANING
 * ONE FAILED INVOCATION != ERASE EVERY INDEPENDENT CONNECTION FACT
 * UNKNOWN FAILURE != STALE READY
 */
object VexLifePartnerFailureProjection {
    fun apply(
        observation: VexLifePartnerConnectionProjection.Observation,
        failure: ExternalProviderWire.Failure,
    ): VexLifePartnerConnectionProjection.Projection {
        require(
            observation.providerRef ==
                providerRef(
                    failure.provider,
                ),
        ) {
            "provider failure evidence does not belong to this Partner connection"
        }
        require(
            observation.credential !=
                VexLifePartnerConnectionProjection.CredentialStanding.MISSING,
        ) {
            "provider invocation failure cannot be applied to a connection with no credential"
        }

        val failed =
            when (
                failure.meaning
            ) {
                ExternalProviderWire.FailureMeaning.CREDENTIAL_REJECTED ->
                    observation.copy(
                        credential =
                            VexLifePartnerConnectionProjection.CredentialStanding.REJECTED,
                        account =
                            VexLifePartnerConnectionProjection.AccountStanding.UNKNOWN,
                        service =
                            VexLifePartnerConnectionProjection.ServiceStanding.UNKNOWN,
                        modelAccess =
                            VexLifePartnerConnectionProjection.ModelAccessStanding.UNKNOWN,
                        invocation =
                            VexLifePartnerConnectionProjection.InvocationStanding.LAST_INVOCATION_FAILED,
                    )

                ExternalProviderWire.FailureMeaning.ACCOUNT_OR_BILLING_HELD ->
                    observation.copy(
                        account =
                            VexLifePartnerConnectionProjection.AccountStanding.BILLING_HELD,
                        invocation =
                            VexLifePartnerConnectionProjection.InvocationStanding.LAST_INVOCATION_FAILED,
                    )

                ExternalProviderWire.FailureMeaning.PERMISSION_HELD ->
                    observation.copy(
                        account =
                            VexLifePartnerConnectionProjection.AccountStanding.PERMISSION_HELD,
                        invocation =
                            VexLifePartnerConnectionProjection.InvocationStanding.LAST_INVOCATION_FAILED,
                    )

                ExternalProviderWire.FailureMeaning.MODEL_NOT_ACCESSIBLE ->
                    observation.copy(
                        modelAccess =
                            VexLifePartnerConnectionProjection.ModelAccessStanding.NO_ACCESS,
                        invocation =
                            VexLifePartnerConnectionProjection.InvocationStanding.LAST_INVOCATION_FAILED,
                    )

                ExternalProviderWire.FailureMeaning.RATE_LIMITED ->
                    observation.copy(
                        service =
                            VexLifePartnerConnectionProjection.ServiceStanding.RATE_LIMITED,
                        invocation =
                            VexLifePartnerConnectionProjection.InvocationStanding.LAST_INVOCATION_FAILED,
                    )

                ExternalProviderWire.FailureMeaning.PROVIDER_OVERLOADED ->
                    observation.copy(
                        service =
                            VexLifePartnerConnectionProjection.ServiceStanding.OVERLOADED,
                        invocation =
                            VexLifePartnerConnectionProjection.InvocationStanding.LAST_INVOCATION_FAILED,
                    )

                ExternalProviderWire.FailureMeaning.PROVIDER_UNAVAILABLE ->
                    observation.copy(
                        service =
                            VexLifePartnerConnectionProjection.ServiceStanding.UNAVAILABLE,
                        invocation =
                            VexLifePartnerConnectionProjection.InvocationStanding.LAST_INVOCATION_FAILED,
                    )

                ExternalProviderWire.FailureMeaning.REQUEST_INVALID,
                ExternalProviderWire.FailureMeaning.CONTEXT_LIMIT_EXCEEDED,
                ->
                    observation.copy(
                        invocation =
                            VexLifePartnerConnectionProjection.InvocationStanding.LAST_INVOCATION_FAILED,
                    )

                ExternalProviderWire.FailureMeaning.STREAM_INTERRUPTED,
                ExternalProviderWire.FailureMeaning.RESPONSE_INVALID,
                ExternalProviderWire.FailureMeaning.UNKNOWN_PROVIDER_FAILURE,
                ->
                    observation.copy(
                        service =
                            VexLifePartnerConnectionProjection.ServiceStanding.UNKNOWN,
                        invocation =
                            VexLifePartnerConnectionProjection.InvocationStanding.LAST_INVOCATION_FAILED,
                    )
            }

        return VexLifePartnerConnectionProjection.project(
            failed,
        )
    }

    private fun providerRef(
        provider: ExternalProviderWire.Provider,
    ): String =
        when (
            provider
        ) {
            ExternalProviderWire.Provider.OPENAI_RESPONSES ->
                VexLifePartnerProviderCatalog.OpenAiRef

            ExternalProviderWire.Provider.ANTHROPIC_MESSAGES ->
                VexLifePartnerProviderCatalog.AnthropicRef

            ExternalProviderWire.Provider.KIMI_CHAT_COMPLETIONS ->
                VexLifePartnerProviderCatalog.KimiInternationalRef
        }
}

// [VXG RealForever]
