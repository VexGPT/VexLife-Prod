package vexlife.partner

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs

class VexLifePartnerHostEffectTest {
    private fun plan(
        provider: VexLifePartnerProviderCatalog.Definition =
            VexLifePartnerProviderCatalog.OpenAi,
    ): VexLifePartnerNetworkPlan.Plan =
        VexLifePartnerNetworkPlan.form(
            providerRef =
                provider.providerRef,
            credential =
                VexLifePartnerCredentialCustody.Coordinate(
                    providerRef =
                        provider.providerRef,
                    credentialRef =
                        "credential.vexlife.partner.host-effect-fixture",
                ),
            requestBody =
                """{"model":"fixture","stream":true}""",
        )

    @Test
    fun ordinaryProviderHttpErrorsRemainHttpResponsesForP1AndA5() {
        listOf(
            401,
            402,
            403,
            404,
            429,
            500,
            503,
            529,
        ).forEach { status ->
            val plan =
                plan()
            val body =
                """{"error":{"type":"fixture","message":"fixture"}}"""
            val result =
                VexLifePartnerHostEffect.Result.Http(
                    status = status,
                    body = body,
                    receipt =
                        VexLifePartnerHostEffect.receiptForHttp(
                            plan = plan,
                            status = status,
                            body = body,
                        ),
                )

            assertEquals(
                status,
                assertIs<VexLifePartnerHostEffect.Result.Http>(
                    VexLifePartnerHostEffect.requireExact(
                        plan = plan,
                        result = result,
                    ),
                ).status,
            )
        }
    }

    @Test
    fun redirectCannotBecomeAcceptedHttpResponse() {
        val plan =
            plan()
        assertFailsWith<IllegalArgumentException> {
            VexLifePartnerHostEffect.receiptForHttp(
                plan = plan,
                status = 302,
                body = "",
            )
        }
    }

    @Test
    fun redirectRejectionIsATransportFailureWithoutHttpReceipt() {
        val plan =
            plan()
        val result =
            VexLifePartnerHostEffect.Result.TransportFailure(
                code =
                    VexLifePartnerHostEffect.FailureCode.REDIRECT_REJECTED,
                detail =
                    "built-in Partner endpoint returned a redirect",
                receipt =
                    VexLifePartnerHostEffect.receiptForTransportFailure(
                        plan = plan,
                        networkAttempted = true,
                    ),
            )

        val exact =
            assertIs<VexLifePartnerHostEffect.Result.TransportFailure>(
                VexLifePartnerHostEffect.requireExact(
                    plan = plan,
                    result = result,
                ),
            )
        assertEquals(
            VexLifePartnerHostEffect.FailureCode.REDIRECT_REJECTED,
            exact.code,
        )
        assertEquals(
            null,
            exact.receipt.httpStatus,
        )
    }

    @Test
    fun helperReceiptCannotSubstituteAnotherProviderOrCredential() {
        val plan =
            plan()
        val body =
            "{}"
        val wrongProvider =
            VexLifePartnerHostEffect.Result.Http(
                status = 200,
                body = body,
                receipt =
                    VexLifePartnerHostEffect.receiptForHttp(
                        plan = plan,
                        status = 200,
                        body = body,
                    )
                        .copy(
                            providerRef =
                                VexLifePartnerProviderCatalog.AnthropicRef,
                        ),
            )
        assertFailsWith<IllegalArgumentException> {
            VexLifePartnerHostEffect.requireExact(
                plan = plan,
                result = wrongProvider,
            )
        }

        val wrongCredential =
            VexLifePartnerHostEffect.Result.Http(
                status = 200,
                body = body,
                receipt =
                    VexLifePartnerHostEffect.receiptForHttp(
                        plan = plan,
                        status = 200,
                        body = body,
                    )
                        .copy(
                            credentialRef =
                                "credential.vexlife.partner.some-other-ref",
                        ),
            )
        assertFailsWith<IllegalArgumentException> {
            VexLifePartnerHostEffect.requireExact(
                plan = plan,
                result = wrongCredential,
            )
        }
    }

    @Test
    fun executorContractNeedsOnlyTheNonSecretNetworkPlan() {
        val plan =
            plan(
                VexLifePartnerProviderCatalog.KimiInternational,
            )
        var observedPlan: VexLifePartnerNetworkPlan.Plan? =
            null
        val executor =
            VexLifePartnerHostEffect.Executor {
                observed ->
                observedPlan =
                    observed
                val body =
                    """{"id":"fixture","choices":[]}"""
                VexLifePartnerHostEffect.Result.Http(
                    status = 200,
                    body = body,
                    receipt =
                        VexLifePartnerHostEffect.receiptForHttp(
                            plan = observed,
                            status = 200,
                            body = body,
                        ),
                )
            }

        val result =
            executor.execute(
                plan,
            )

        assertEquals(
            plan,
            observedPlan,
        )
        assertFalse(
            plan.requestBody.contains(
                plan.credentialRef,
            ),
        )
        assertIs<VexLifePartnerHostEffect.Result.Http>(
            VexLifePartnerHostEffect.requireExact(
                plan = plan,
                result = result,
            ),
        )
    }

    @Test
    fun transportFailureDoesNotInventProviderHttpStatus() {
        val plan =
            plan(
                VexLifePartnerProviderCatalog.Anthropic,
            )
        val result =
            VexLifePartnerHostEffect.Result.TransportFailure(
                code =
                    VexLifePartnerHostEffect.FailureCode.TLS_FAILURE,
                detail =
                    "TLS handshake failed before an HTTP response",
                receipt =
                    VexLifePartnerHostEffect.receiptForTransportFailure(
                        plan = plan,
                        networkAttempted = true,
                    ),
            )

        val exact =
            VexLifePartnerHostEffect.requireExact(
                plan = plan,
                result = result,
            )
        assertEquals(
            null,
            exact.receipt.httpStatus,
        )
        assertEquals(
            null,
            exact.receipt.responseBodySha256,
        )
    }
}

// [VXG RealForever]
