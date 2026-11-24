package vexlife.partner

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class VexLifePartnerFailureProjectionTest {
    private fun ready(
        providerRef: String =
            VexLifePartnerProviderCatalog.OpenAiRef,
    ): VexLifePartnerConnectionProjection.Observation =
        VexLifePartnerConnectionProjection.Observation(
            providerRef = providerRef,
            modelRef = "model.partner.fixture",
            credential =
                VexLifePartnerConnectionProjection.CredentialStanding.ACCEPTED,
            account =
                VexLifePartnerConnectionProjection.AccountStanding.USABLE,
            service =
                VexLifePartnerConnectionProjection.ServiceStanding.READY,
            modelAccess =
                VexLifePartnerConnectionProjection.ModelAccessStanding.AVAILABLE,
            invocation =
                VexLifePartnerConnectionProjection.InvocationStanding.QUALIFIED,
        )

    private fun failure(
        meaning: ExternalProviderWire.FailureMeaning,
        provider: ExternalProviderWire.Provider =
            ExternalProviderWire.Provider.OPENAI_RESPONSES,
    ): ExternalProviderWire.Failure =
        ExternalProviderWire.Failure(
            provider = provider,
            meaning = meaning,
            retry =
                ExternalProviderWire.RetryDisposition.UNKNOWN,
            httpStatus = null,
            providerType = null,
            providerCode = null,
            message = null,
        )

    @Test
    fun credentialRejectionInvalidatesCredentialDependentCurrentness() {
        val result =
            VexLifePartnerFailureProjection.apply(
                observation =
                    ready(),
                failure =
                    failure(
                        ExternalProviderWire.FailureMeaning.CREDENTIAL_REJECTED,
                    ),
            )

        assertEquals(
            VexLifePartnerConnectionProjection.CredentialStanding.REJECTED,
            result.credential,
        )
        assertEquals(
            VexLifePartnerConnectionProjection.AccountStanding.UNKNOWN,
            result.account,
        )
        assertEquals(
            VexLifePartnerConnectionProjection.ServiceStanding.UNKNOWN,
            result.service,
        )
        assertEquals(
            VexLifePartnerConnectionProjection.ModelAccessStanding.UNKNOWN,
            result.modelAccess,
        )
        assertEquals(
            VexLifePartnerConnectionProjection.Standing.CREDENTIAL_REJECTED,
            result.standing,
        )
    }

    @Test
    fun billingPermissionAndModelAccessRemainDifferentMeanings() {
        assertEquals(
            VexLifePartnerConnectionProjection.Standing.BILLING_HELD,
            VexLifePartnerFailureProjection.apply(
                ready(),
                failure(
                    ExternalProviderWire.FailureMeaning.ACCOUNT_OR_BILLING_HELD,
                ),
            ).standing,
        )
        assertEquals(
            VexLifePartnerConnectionProjection.Standing.PERMISSION_HELD,
            VexLifePartnerFailureProjection.apply(
                ready(),
                failure(
                    ExternalProviderWire.FailureMeaning.PERMISSION_HELD,
                ),
            ).standing,
        )
        assertEquals(
            VexLifePartnerConnectionProjection.Standing.MODEL_NOT_ACCESSIBLE,
            VexLifePartnerFailureProjection.apply(
                ready(),
                failure(
                    ExternalProviderWire.FailureMeaning.MODEL_NOT_ACCESSIBLE,
                ),
            ).standing,
        )
    }

    @Test
    fun rateOverloadAndOutageRemainDifferentServiceMeanings() {
        assertEquals(
            VexLifePartnerConnectionProjection.Standing.RATE_LIMITED,
            VexLifePartnerFailureProjection.apply(
                ready(),
                failure(
                    ExternalProviderWire.FailureMeaning.RATE_LIMITED,
                ),
            ).standing,
        )
        assertEquals(
            VexLifePartnerConnectionProjection.Standing.PROVIDER_OVERLOADED,
            VexLifePartnerFailureProjection.apply(
                ready(),
                failure(
                    ExternalProviderWire.FailureMeaning.PROVIDER_OVERLOADED,
                ),
            ).standing,
        )
        assertEquals(
            VexLifePartnerConnectionProjection.Standing.PROVIDER_UNAVAILABLE,
            VexLifePartnerFailureProjection.apply(
                ready(),
                failure(
                    ExternalProviderWire.FailureMeaning.PROVIDER_UNAVAILABLE,
                ),
            ).standing,
        )
    }

    @Test
    fun requestAndContextFailuresDoNotInventConnectionLoss() {
        listOf(
            ExternalProviderWire.FailureMeaning.REQUEST_INVALID,
            ExternalProviderWire.FailureMeaning.CONTEXT_LIMIT_EXCEEDED,
        ).forEach { meaning ->
            val result =
                VexLifePartnerFailureProjection.apply(
                    ready(),
                    failure(
                        meaning,
                    ),
                )
            assertEquals(
                VexLifePartnerConnectionProjection.CredentialStanding.ACCEPTED,
                result.credential,
            )
            assertEquals(
                VexLifePartnerConnectionProjection.AccountStanding.USABLE,
                result.account,
            )
            assertEquals(
                VexLifePartnerConnectionProjection.ServiceStanding.READY,
                result.service,
            )
            assertEquals(
                VexLifePartnerConnectionProjection.ModelAccessStanding.AVAILABLE,
                result.modelAccess,
            )
            assertEquals(
                VexLifePartnerConnectionProjection.Standing.LAST_INVOCATION_FAILED,
                result.standing,
            )
        }
    }

    @Test
    fun ambiguousTransportOrResponseFailureStopsClaimingReadyService() {
        listOf(
            ExternalProviderWire.FailureMeaning.STREAM_INTERRUPTED,
            ExternalProviderWire.FailureMeaning.RESPONSE_INVALID,
            ExternalProviderWire.FailureMeaning.UNKNOWN_PROVIDER_FAILURE,
        ).forEach { meaning ->
            val result =
                VexLifePartnerFailureProjection.apply(
                    ready(),
                    failure(
                        meaning,
                    ),
                )
            assertEquals(
                VexLifePartnerConnectionProjection.ServiceStanding.UNKNOWN,
                result.service,
            )
            assertEquals(
                VexLifePartnerConnectionProjection.Standing.CHECKING,
                result.standing,
            )
        }
    }

    @Test
    fun failureEvidenceMustBelongToTheExactProvider() {
        assertFailsWith<IllegalArgumentException> {
            VexLifePartnerFailureProjection.apply(
                observation =
                    ready(
                        providerRef =
                            VexLifePartnerProviderCatalog.AnthropicRef,
                    ),
                failure =
                    failure(
                        meaning =
                            ExternalProviderWire.FailureMeaning.RATE_LIMITED,
                        provider =
                            ExternalProviderWire.Provider.OPENAI_RESPONSES,
                    ),
            )
        }
    }

    @Test
    fun providerFailureCannotBeAppliedWhenNoCredentialExists() {
        val missing =
            ready()
                .copy(
                    credential =
                        VexLifePartnerConnectionProjection.CredentialStanding.MISSING,
                )

        assertFailsWith<IllegalArgumentException> {
            VexLifePartnerFailureProjection.apply(
                observation = missing,
                failure =
                    failure(
                        ExternalProviderWire.FailureMeaning.PROVIDER_UNAVAILABLE,
                    ),
            )
        }
    }

    @Test
    fun kimiFailureMapsOnlyToKimiConnectionWithoutChangingIdentity() {
        val before =
            ready(
                providerRef =
                    VexLifePartnerProviderCatalog.KimiInternationalRef,
            )
        val result =
            VexLifePartnerFailureProjection.apply(
                observation = before,
                failure =
                    failure(
                        meaning =
                            ExternalProviderWire.FailureMeaning.ACCOUNT_OR_BILLING_HELD,
                        provider =
                            ExternalProviderWire.Provider.KIMI_CHAT_COMPLETIONS,
                    ),
            )

        assertEquals(
            before.providerRef,
            result.providerRef,
        )
        assertEquals(
            before.modelRef,
            result.modelRef,
        )
        assertEquals(
            VexLifePartnerConnectionProjection.Standing.BILLING_HELD,
            result.standing,
        )
    }
}

// [VXG RealForever]
