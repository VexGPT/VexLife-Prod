package vexlife.partner

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class VexLifePartnerLiveQualificationTest {
    private fun configured(
        profile: VexLifePartnerModelProfileCatalog.Definition,
        credentialRef: String =
            "credential.vexlife.partner.live-qualification-fixture",
    ): VexLifePartnerConfiguredCurrentness.ProviderState =
        VexLifePartnerConfiguredCurrentness.ProviderState(
            providerRef = profile.providerRef,
            credentialRef = credentialRef,
            accountScope =
                VexLifePartnerAccountScope.Selection(
                    providerRef = profile.providerRef,
                ),
            coordinateStanding =
                VexLifePartnerConfiguredCurrentness.CoordinateStanding.PRESENT,
            credentialStanding =
                VexLifePartnerConnectionProjection.CredentialStanding.PRESENT_UNVERIFIED,
        )

    private fun connection(
        profile: VexLifePartnerModelProfileCatalog.Definition,
        configured: VexLifePartnerConfiguredCurrentness.ProviderState,
    ): VexLifePartnerConnectionProjection.Projection =
        VexLifePartnerConfiguredCurrentness.initialConnection(
            provider = configured,
            modelRef = profile.modelRef,
        )

    private fun support(
        profile: VexLifePartnerModelProfileCatalog.Definition,
        complete: Boolean = true,
    ): VexLifePartnerSupportQualification.Projection =
        VexLifePartnerSupportQualification.project(
            VexLifePartnerSupportQualification.Evidence(
                providerRef = profile.providerRef,
                modelRef = profile.modelRef,
                contractFixtureProofRef =
                    "proof.partner.contract.fixture",
                adapterConformanceProofRef =
                    "proof.partner.adapter.fixture".takeIf {
                        complete
                    },
                vexRelationshipConformanceProofRef =
                    "proof.partner.vex.fixture".takeIf {
                        complete
                    },
            ),
        )

    private fun resource(
        relative: String,
    ): String =
        requireNotNull(
            VexLifePartnerLiveQualificationTest::class.java.getResourceAsStream(
                "/vexlife/partner/provider-contract-fixtures/$relative",
            ),
        )
            .bufferedReader()
            .use {
                it.readText()
            }

    @Test
    fun allBuiltInProfilesQualifyThroughOneCredentialBoundRelationship() {
        val cases =
            listOf(
                VexLifePartnerModelProfileCatalog.OpenAiGpt62Sol to
                    "openai/responses-text.stream.txt",
                VexLifePartnerModelProfileCatalog.AnthropicClaudeSonnet55 to
                    "anthropic/messages-text.json",
                VexLifePartnerModelProfileCatalog.KimiK3 to
                    "kimi/chat-text.json",
            )

        cases.forEach {
            (profile, fixture) ->
            val configured =
                configured(
                    profile,
                    credentialRef =
                        "credential.vexlife.partner.live." +
                            profile.providerRef,
                )
            val body =
                resource(
                    fixture,
                )
            var hostCalls =
                0
            var capturedPlan: VexLifePartnerNetworkPlan.Plan? =
                null
            val outcome =
                VexLifePartnerLiveQualification.qualify(
                    profile = profile,
                    configuredProvider = configured,
                    connection =
                        connection(
                            profile,
                            configured,
                        ),
                    support =
                        support(
                            profile,
                        ),
                    executor =
                        VexLifePartnerHostEffect.Executor {
                            plan ->
                            hostCalls +=
                                1
                            capturedPlan =
                                plan
                            VexLifePartnerHostEffect.Result.Http(
                                status = 200,
                                body = body,
                                providerRequestId =
                                    "request.live-qualification.fixture",
                                receipt =
                                    VexLifePartnerHostEffect.receiptForHttp(
                                        plan = plan,
                                        status = 200,
                                        body = body,
                                    ),
                            )
                        },
                    now = {
                        Instant.parse(
                            "2026-11-24T22:30:00Z",
                        )
                    },
                )

            assertEquals(
                1,
                hostCalls,
            )
            val qualified =
                outcome as?
                    VexLifePartnerLiveQualification.Outcome.Qualified
                    ?: error(
                        "built-in Partner profile did not live-qualify: " +
                            profile.profileRef,
                    )
            assertEquals(
                VexLifePartnerConnectionProjection.Standing.QUALIFIED_SERVING,
                qualified.connection.standing,
            )
            assertEquals(
                VexLifePartnerSupportQualification.Standing.LIVE_QUALIFIED,
                qualified.support.standing,
            )
            assertEquals(
                configured.credentialRef,
                qualified.receipt.credentialRef,
            )
            assertEquals(
                configured.accountScope,
                qualified.receipt.accountScope,
            )
            assertEquals(
                qualified.receipt.receiptRef,
                qualified.support.liveQualificationReceiptRef,
            )
            assertEquals(
                configured.credentialRef,
                qualified.support.liveQualificationCredentialRef,
            )
            assertEquals(
                configured.accountScope,
                qualified.support.liveQualificationAccountScope,
            )
            assertTrue(
                qualified.receipt.receiptRef.startsWith(
                    "receipt.vexlife.partner.live.",
                ),
            )
            val plan =
                assertNotNull(
                    capturedPlan,
                )
            assertEquals(
                VexLifePartnerNetworkPlan.RedirectPolicy.DENY,
                plan.redirectPolicy,
            )
            assertFalse(
                plan.requestBody.contains(
                    requireNotNull(
                        configured.credentialRef,
                    ),
                ),
            )
        }
    }

    @Test
    fun deterministicSupportProofIsRequiredBeforeAnyHostEffect() {
        val profile =
            VexLifePartnerModelProfileCatalog.OpenAiGpt62Sol
        val configured =
            configured(
                profile,
            )
        var hostCalls =
            0

        assertFailsWith<IllegalArgumentException> {
            VexLifePartnerLiveQualification.qualify(
                profile = profile,
                configuredProvider = configured,
                connection =
                    connection(
                        profile,
                        configured,
                    ),
                support =
                    support(
                        profile,
                        complete = false,
                    ),
                executor =
                    VexLifePartnerHostEffect.Executor {
                        plan ->
                        hostCalls +=
                            1
                        VexLifePartnerHostEffect.Result.TransportFailure(
                            code =
                                VexLifePartnerHostEffect.FailureCode.HOST_UNAVAILABLE,
                            detail = "must not run",
                            receipt =
                                VexLifePartnerHostEffect.receiptForTransportFailure(
                                    plan = plan,
                                    networkAttempted = false,
                                ),
                        )
                    },
            )
        }

        assertEquals(
            0,
            hostCalls,
        )
    }

    @Test
    fun rejectedCredentialStaysHeldAndDoesNotMintLiveSupport() {
        val profile =
            VexLifePartnerModelProfileCatalog.OpenAiGpt62Sol
        val configured =
            configured(
                profile,
            )
        val body =
            """
            {
              "error": {
                "message": "Synthetic invalid authentication fixture",
                "type": "invalid_request_error",
                "param": null,
                "code": "invalid_api_key"
              }
            }
            """.trimIndent()
        val outcome =
            VexLifePartnerLiveQualification.qualify(
                profile = profile,
                configuredProvider = configured,
                connection =
                    connection(
                        profile,
                        configured,
                    ),
                support =
                    support(
                        profile,
                    ),
                executor =
                    VexLifePartnerHostEffect.Executor {
                        plan ->
                        VexLifePartnerHostEffect.Result.Http(
                            status = 401,
                            body = body,
                            receipt =
                                VexLifePartnerHostEffect.receiptForHttp(
                                    plan = plan,
                                    status = 401,
                                    body = body,
                                ),
                        )
                    },
            )

        val held =
            outcome as?
                VexLifePartnerLiveQualification.Outcome.Held
                ?: error(
                    "rejected credential unexpectedly live-qualified",
                )
        assertEquals(
            ExternalProviderWire.FailureMeaning.CREDENTIAL_REJECTED,
            held.providerFailure?.meaning,
        )
        assertEquals(
            VexLifePartnerConnectionProjection.Standing.CREDENTIAL_REJECTED,
            held.connection.standing,
        )
        assertEquals(
            VexLifePartnerSupportQualification.Standing.CONTRACT_PROVEN_LIVE_UNQUALIFIED,
            held.support.standing,
        )
        assertEquals(
            null,
            held.support.liveQualificationReceiptRef,
        )
    }

    @Test
    fun missingProtectedCredentialReturnsNeedsCredentialWithoutNetwork() {
        val profile =
            VexLifePartnerModelProfileCatalog.OpenAiGpt62Sol
        val configured =
            configured(
                profile,
            )
        var networkAttempted =
            true
        val outcome =
            VexLifePartnerLiveQualification.qualify(
                profile = profile,
                configuredProvider = configured,
                connection =
                    connection(
                        profile,
                        configured,
                    ),
                support =
                    support(
                        profile,
                    ),
                executor =
                    VexLifePartnerHostEffect.Executor {
                        plan ->
                        networkAttempted =
                            false
                        VexLifePartnerHostEffect.Result.TransportFailure(
                            code =
                                VexLifePartnerHostEffect.FailureCode.CREDENTIAL_NOT_PRESENT,
                            detail =
                                "synthetic missing credential",
                            receipt =
                                VexLifePartnerHostEffect.receiptForTransportFailure(
                                    plan = plan,
                                    networkAttempted = false,
                                ),
                        )
                    },
            )

        val held =
            outcome as?
                VexLifePartnerLiveQualification.Outcome.Held
                ?: error(
                    "missing credential unexpectedly live-qualified",
                )
        assertFalse(
            networkAttempted,
        )
        assertEquals(
            VexLifePartnerHostEffect.FailureCode.CREDENTIAL_NOT_PRESENT,
            held.transportFailure?.code,
        )
        assertEquals(
            VexLifePartnerConnectionProjection.Standing.NEEDS_CREDENTIAL,
            held.connection.standing,
        )
        assertEquals(
            VexLifePartnerSupportQualification.Standing.CONTRACT_PROVEN_LIVE_UNQUALIFIED,
            held.support.standing,
        )
    }

    @Test
    fun unexpectedProviderToolControlCannotCountAsLiveQualification() {
        val profile =
            VexLifePartnerModelProfileCatalog.OpenAiGpt62Sol
        val configured =
            configured(
                profile,
            )
        val body =
            resource(
                "openai/responses-tool.json",
            )
        val outcome =
            VexLifePartnerLiveQualification.qualify(
                profile = profile,
                configuredProvider = configured,
                connection =
                    connection(
                        profile,
                        configured,
                    ),
                support =
                    support(
                        profile,
                    ),
                executor =
                    VexLifePartnerHostEffect.Executor {
                        plan ->
                        VexLifePartnerHostEffect.Result.Http(
                            status = 200,
                            body = body,
                            receipt =
                                VexLifePartnerHostEffect.receiptForHttp(
                                    plan = plan,
                                    status = 200,
                                    body = body,
                                ),
                        )
                    },
            )

        val held =
            outcome as?
                VexLifePartnerLiveQualification.Outcome.Held
                ?: error(
                    "tool-control response unexpectedly live-qualified",
                )
        assertEquals(
            ExternalProviderWire.FailureMeaning.RESPONSE_INVALID,
            held.providerFailure?.meaning,
        )
        assertEquals(
            VexLifePartnerSupportQualification.Standing.CONTRACT_PROVEN_LIVE_UNQUALIFIED,
            held.support.standing,
        )
    }
}

// [VXG RealForever]
