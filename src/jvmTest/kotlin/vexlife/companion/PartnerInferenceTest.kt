package vexlife.companion

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vexlife.capability.VexLifeCapabilityAtlas
import vexlife.capability.VexLifeCapabilityRequest
import vexlife.model.VexLifeInferenceSelection
import vexlife.model.VexLifeInferenceSessionAdmission
import vexlife.model.VexLifeModelSourceProjection
import vexlife.partner.ExternalProviderWire
import vexlife.partner.VexLifePartnerAccountScope
import vexlife.partner.VexLifePartnerHostEffect
import vexlife.partner.VexLifePartnerModelProfileCatalog
import vexlife.partner.VexLifePartnerProviderCatalog

class PartnerInferenceTest {
    private val profile =
        VexLifePartnerModelProfileCatalog.OpenAiGpt62Sol

    private val credentialRef =
        "credential.vexlife.partner.partner-inference-fixture"

    private fun qualified(): VexLifeInferenceSessionAdmission.Qualified =
        VexLifeInferenceSessionAdmission.Qualified(
            coordinate =
                VexLifeInferenceSelection.Coordinate(
                    handle = profile.profileRef,
                    modelRef = profile.modelRef,
                    sourceKind =
                        VexLifeModelSourceProjection.SourceKind.PARTNER,
                    providerRef = profile.providerRef,
                ),
            profileRef = profile.profileRef,
            qualificationRef =
                "qualification.vexlife.partner.openai.fixture",
            contextWindowTokens = null,
            partnerCredentialRef = credentialRef,
            partnerAccountScope =
                VexLifePartnerAccountScope.Selection(
                    providerRef = profile.providerRef,
                ),
        )

    private fun resource(
        relative: String,
    ): String =
        requireNotNull(
            PartnerInferenceTest::class.java.getResourceAsStream(
                "/vexlife/partner/provider-contract-fixtures/" +
                    relative,
            ),
        )
            .bufferedReader()
            .use {
                it.readText()
            }

    private fun monotonic(): () -> Long {
        var value =
            1_000_000_000L
        return {
            value.also {
                value +=
                    125_000_000L
            }
        }
    }

    @Test
    fun openAiTextStageUsesProtectedHostAndReturnsTruthfulPartnerEvidence() {
        var observedPlan: vexlife.partner.VexLifePartnerNetworkPlan.Plan? =
            null
        val body =
            resource(
                "openai/responses-text.stream.txt",
            )
        val progress =
            mutableListOf<DirectCompanionConnection.Progress>()
        val result =
            PartnerInference(
                qualified = qualified(),
                executor =
                    VexLifePartnerHostEffect.Executor {
                        plan ->
                        observedPlan =
                            plan
                        VexLifePartnerHostEffect.Result.Http(
                            status = 200,
                            body = body,
                            providerRequestId =
                                "req_partner_fixture",
                            receipt =
                                VexLifePartnerHostEffect.receiptForHttp(
                                    plan = plan,
                                    status = 200,
                                    body = body,
                                ),
                        )
                    },
                now = {
                    java.time.Instant.parse(
                        "2026-11-24T21:00:00Z",
                    )
                },
                monotonicNanos = monotonic(),
            )
                .invoke(
                    CompanionInferenceConnection.Request(
                        messages =
                            listOf(
                                CompanionProviderMessage(
                                    role = "system",
                                    content = "You are Vex.",
                                ),
                                CompanionProviderMessage(
                                    role = "user",
                                    content = "Say hello.",
                                ),
                            ),
                        capabilityOffer = emptyList(),
                        conversationCacheBoundaryMessageCount = 99,
                        progressObserver =
                            DirectCompanionConnection.ProgressObserver {
                                progress +=
                                    it
                            },
                        cancellation = null,
                    ),
                )

        val plan =
            requireNotNull(
                observedPlan,
            )
        assertEquals(
            VexLifePartnerProviderCatalog.OpenAi.inferenceUrl,
            plan.requestUrl,
        )
        assertFalse(
            plan.requestBody.contains(
                credentialRef,
            ),
        )
        assertFalse(
            plan.requestBody.contains(
                "vexlife_prompt_cache_boundary",
            ),
        )
        assertEquals(
            "Hello from Vex",
            result.content,
        )
        assertEquals(
            VexLifeModelSourceProjection.SourceKind.PARTNER,
            result.observation.sourceKind,
        )
        assertEquals(
            profile.providerRef,
            result.observation.providerRef,
        )
        assertEquals(
            profile.profileRef,
            result.observation.profileRef,
        )
        assertEquals(
            false,
            result.invocationEvidence.loopbackOnly,
        )
        assertEquals(
            true,
            result.invocationEvidence.actualHttpCall,
        )
        assertEquals(
            "req_partner_fixture",
            result.invocationEvidence.providerRequestId,
        )
        assertNull(
            result.localRuntimeObservation,
        )
        assertNull(
            result.localInvocationEvidence,
        )
        assertNull(
            result.pendingPartnerCapability,
        )
        assertEquals(
            listOf(
                "Hello",
                " from Vex",
            ),
            progress.map {
                it.delta
            },
        )
    }

    @Test
    fun allBuiltInPartnerDialectsTraverseTheSameInferenceSeam() {
        data class Case(
            val profile: VexLifePartnerModelProfileCatalog.Definition,
            val bodyPath: String,
            val expectedUrl: String,
            val credentialSuffix: String,
        )

        listOf(
            Case(
                profile = VexLifePartnerModelProfileCatalog.OpenAiGpt62Sol,
                bodyPath = "openai/responses-text.stream.txt",
                expectedUrl = VexLifePartnerProviderCatalog.OpenAi.inferenceUrl,
                credentialSuffix = "openai",
            ),
            Case(
                profile = VexLifePartnerModelProfileCatalog.AnthropicClaudeSonnet55,
                bodyPath = "anthropic/messages-text.json",
                expectedUrl = VexLifePartnerProviderCatalog.Anthropic.inferenceUrl,
                credentialSuffix = "anthropic",
            ),
            Case(
                profile = VexLifePartnerModelProfileCatalog.KimiK3,
                bodyPath = "kimi/chat-text.json",
                expectedUrl = VexLifePartnerProviderCatalog.KimiInternational.inferenceUrl,
                credentialSuffix = "kimi",
            ),
        ).forEach {
            case ->
            val credentialRef =
                "credential.vexlife.partner.inference-" +
                    case.credentialSuffix +
                    "-fixture"
            val qualified =
                VexLifeInferenceSessionAdmission.Qualified(
                    coordinate =
                        VexLifeInferenceSelection.Coordinate(
                            handle = case.profile.profileRef,
                            modelRef = case.profile.modelRef,
                            sourceKind =
                                VexLifeModelSourceProjection.SourceKind.PARTNER,
                            providerRef = case.profile.providerRef,
                        ),
                    profileRef = case.profile.profileRef,
                    qualificationRef =
                        "qualification.vexlife.partner." +
                            case.credentialSuffix +
                            ".fixture",
                    partnerCredentialRef = credentialRef,
                    partnerAccountScope =
                        VexLifePartnerAccountScope.Selection(
                            providerRef = case.profile.providerRef,
                        ),
                )
            var requestUrl: String? =
                null
            val body =
                resource(
                    case.bodyPath,
                )
            val result =
                PartnerInference(
                    qualified = qualified,
                    executor =
                        VexLifePartnerHostEffect.Executor {
                            plan ->
                            requestUrl =
                                plan.requestUrl
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
                    monotonicNanos = monotonic(),
                )
                    .invoke(
                        CompanionInferenceConnection.Request(
                            messages =
                                listOf(
                                    CompanionProviderMessage(
                                        role = "user",
                                        content = "Say hello.",
                                    ),
                                ),
                            capabilityOffer = emptyList(),
                            conversationCacheBoundaryMessageCount = null,
                            progressObserver = null,
                            cancellation = null,
                        ),
                    )

            assertEquals(
                case.expectedUrl,
                requestUrl,
            )
            assertEquals(
                "Hello from Vex",
                result.content,
            )
            assertEquals(
                case.profile.providerRef,
                result.invocationEvidence.providerRef,
            )
            assertFalse(
                result.providerRequestBody.contains(
                    credentialRef,
                ),
            )
        }
    }

    @Test
    fun openAiNativeToolCallKeepsProviderCallRefBesideCanonicalVexControl() {
        val body =
            resource(
                "openai/responses-tool.stream.txt",
            )
        val offered =
            VexLifeCapabilityAtlas
                .definitions()
                .filter {
                    it.actions.isNotEmpty()
                }
        val result =
            PartnerInference(
                qualified = qualified(),
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
                monotonicNanos = monotonic(),
            )
                .invoke(
                    CompanionInferenceConnection.Request(
                        messages =
                            listOf(
                                CompanionProviderMessage(
                                    role = "user",
                                    content =
                                        "Use the admitted Vex capability when needed.",
                                ),
                            ),
                        capabilityOffer = offered,
                        conversationCacheBoundaryMessageCount = null,
                        progressObserver = null,
                        cancellation = null,
                    ),
                )

        val pending =
            assertNotNull(
                result.pendingPartnerCapability,
            )
        assertEquals(
            "call_stream_fixture",
            pending.providerCallRef,
        )
        assertEquals(
            "call_stream_fixture",
            result.invocationEvidence.providerCallRef,
        )
        assertNotNull(
            VexLifeCapabilityRequest.parseOrNull(
                result.content,
            ),
        )
        assertTrue(
            result.content.startsWith(
                VexLifeCapabilityRequest.RequestHeader,
            ),
        )
    }

    @Test
    fun cancelledBeforePartnerStagePerformsNoHostEffect() {
        val cancellation =
            DirectCompanionCancellation()
        assertTrue(
            cancellation.request(),
        )
        var executed =
            false

        kotlin.test.assertFailsWith<DirectCompanionCancellation.Cancelled> {
            PartnerInference(
                qualified = qualified(),
                executor =
                    VexLifePartnerHostEffect.Executor {
                        executed =
                            true
                        error(
                            "cancelled Partner stage must not execute the host effect",
                        )
                    },
            )
                .invoke(
                    CompanionInferenceConnection.Request(
                        messages =
                            listOf(
                                CompanionProviderMessage(
                                    role = "user",
                                    content = "Hello",
                                ),
                            ),
                        capabilityOffer = emptyList(),
                        conversationCacheBoundaryMessageCount = null,
                        progressObserver = null,
                        cancellation = cancellation,
                    ),
                )
        }

        assertFalse(
            executed,
        )
    }

    @Test
    fun providerHttpFailureStaysProviderFailureInsteadOfTransportFailure() {
        val failure =
            kotlin.test.assertFailsWith<PartnerInference.PartnerInferenceException> {
                PartnerInference(
                    qualified = qualified(),
                        executor =
                        VexLifePartnerHostEffect.Executor {
                            plan ->
                            val body =
                                """
                                {
                                  "error": {
                                    "type": "authentication_error",
                                    "message": "fixture"
                                  }
                                }
                                """.trimIndent()
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
                    .invoke(
                        CompanionInferenceConnection.Request(
                            messages =
                                listOf(
                                    CompanionProviderMessage(
                                        role = "user",
                                        content = "Hello",
                                    ),
                                ),
                            capabilityOffer = emptyList(),
                            conversationCacheBoundaryMessageCount = null,
                            progressObserver = null,
                            cancellation = null,
                        ),
                    )
            }

        assertNull(
            failure.transportFailure,
        )
        assertEquals(
            ExternalProviderWire.FailureMeaning.CREDENTIAL_REJECTED,
            assertNotNull(
                failure.providerFailure,
            )
                .meaning,
        )
    }

    @Test
    fun hostTransportFailureNeverInventsProviderHttpFailure() {
        val failure =
            kotlin.test.assertFailsWith<PartnerInference.PartnerInferenceException> {
                PartnerInference(
                    qualified = qualified(),
                        executor =
                        VexLifePartnerHostEffect.Executor {
                            plan ->
                            VexLifePartnerHostEffect.Result.TransportFailure(
                                code =
                                    VexLifePartnerHostEffect.FailureCode.TLS_FAILURE,
                                detail =
                                    "fixture TLS failure",
                                receipt =
                                    VexLifePartnerHostEffect.receiptForTransportFailure(
                                        plan = plan,
                                        networkAttempted = true,
                                    ),
                            )
                        },
                )
                    .invoke(
                        CompanionInferenceConnection.Request(
                            messages =
                                listOf(
                                    CompanionProviderMessage(
                                        role = "user",
                                        content = "Hello",
                                    ),
                                ),
                            capabilityOffer = emptyList(),
                            conversationCacheBoundaryMessageCount = null,
                            progressObserver = null,
                            cancellation = null,
                        ),
                    )
            }

        assertIs<VexLifePartnerHostEffect.Result.TransportFailure>(
            failure.transportFailure,
        )
        assertNull(
            failure.providerFailure,
        )
    }
}

// [VXG RealForever]
