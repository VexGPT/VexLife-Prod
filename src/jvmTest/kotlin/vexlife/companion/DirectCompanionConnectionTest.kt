package vexlife.companion

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticProductIdentity
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeState

class DirectCompanionConnectionTest {
    private fun qualifiedRuntime(): HereticModelConfiguration.QualifiedRuntimeEvidence =
        HereticModelConfiguration.QualifiedRuntimeEvidence(
            runtimeReceiptRef = "receipt.vexlife-prod.heretic.test",
            runtimeState = HereticRuntimeState.Qualified,
            modelRef = HereticProductIdentity.ModelRef,
            modelProfileRef = HereticProductIdentity.ModelProfileRef,
            artifactRef = HereticProductIdentity.ArtifactSetRef,
            artifactContentSetSha256 = HereticProductIdentity.ArtifactContentSetSha256,
            runtimeAdapterRef = HereticProductIdentity.RuntimeAdapterRef,
            runtimeClass = HereticProductIdentity.RuntimeClass,
            endpoint = HereticProductIdentity.Endpoint,
            requestModel = HereticProductIdentity.RequestModel,
            qualification = HereticRuntimeQualification.qualified(),
        )

    @Test
    fun qualifiedRuntimeOwnsEndpointAndModelForOneActualLoopbackInference() {
        var call: DirectCompanionConnection.HttpCall? =
            null

        val result =
            DirectCompanionConnection.invoke(
                runtime = qualifiedRuntime(),
                messages =
                    listOf(
                        DirectCompanionConnection.ProviderMessage(
                            role = "user",
                            content = "Hello Vex",
                        ),
                    ),
                transport =
                    DirectCompanionConnection.Transport {
                        observed ->
                        call =
                            observed
                        DirectCompanionConnection.HttpResult(
                            status = 200,
                            body =
                                """
                                {
                                  "model": "default_model",
                                  "choices": [
                                    {
                                      "finish_reason": "stop",
                                      "message": {
                                        "role": "assistant",
                                        "content": "Hello from Vex."
                                      }
                                    }
                                  ]
                                }
                                """.trimIndent(),
                        )
                    },
                now = {
                    Instant.parse(
                        "2026-11-05T03:20:00Z",
                    )
                },
            )

        val request =
            requireNotNull(
                call,
            )
        assertEquals(
            "http://127.0.0.1:8765/v1/chat/completions",
            request.uri.toString(),
        )
        val body =
            Json
                .parseToJsonElement(
                    request.body,
                )
                .jsonObject
        assertEquals(
            "default_model",
            body[
                "model"
            ]
                ?.jsonPrimitive
                ?.content,
        )
        assertEquals(
            16_000,
            body[
                "max_tokens"
            ]
                ?.jsonPrimitive
                ?.content
                ?.toInt(),
        )
        assertEquals(
            "Hello from Vex.",
            result.content,
        )
        assertEquals(
            "default_model",
            result.model,
        )
        assertTrue(
            result.invocationEvidence.actualHttpCall,
        )
        assertTrue(
            result.invocationEvidence.loopbackOnly,
        )
        assertTrue(
            result.invocationEvidence.observedEffects.modelRuntimeObserved,
        )
        assertTrue(
            result.invocationEvidence.observedEffects.providerOrNetworkObserved,
        )
        assertFalse(
            result.runtimeObservation.rawProviderResponsePersisted,
        )
    }

    @Test
    fun providerReportedLocalModelPathNeverBecomesProductIdentityOrPersistedRawValue() {
        val result =
            DirectCompanionConnection.invoke(
                runtime = qualifiedRuntime(),
                messages =
                    listOf(
                        DirectCompanionConnection.ProviderMessage(
                            role = "user",
                            content = "Hello",
                        ),
                    ),
                transport =
                    DirectCompanionConnection.Transport {
                        DirectCompanionConnection.HttpResult(
                            status = 200,
                            body =
                                """
                                {
                                  "model": "/Users/victor/private/model",
                                  "choices": [
                                    {
                                      "message": {
                                        "role": "assistant",
                                        "content": "Hi",
                                        "reasoning": "private reasoning"
                                      }
                                    }
                                  ]
                                }
                                """.trimIndent(),
                        )
                    },
                now = {
                    Instant.parse(
                        "2026-11-05T03:21:00Z",
                    )
                },
            )

        assertEquals(
            HereticProductIdentity.RequestModel,
            result.model,
        )
        assertTrue(
            result.runtimeObservation.reportedModelField.present,
        )
        assertEquals(
            "LOCAL_PATH_LIKE",
            result.runtimeObservation.reportedModelField.pathClass,
        )
        assertFalse(
            result.runtimeObservation.reportedModelField.rawValuePersisted,
        )
        assertTrue(
            result.runtimeObservation.reasoningTrace.present,
        )
        assertFalse(
            result.runtimeObservation.reasoningTrace.rawPersisted,
        )
        assertTrue(
            result.runtimeObservation.reasoningTrace.contentSha256
                ?.matches(
                    Regex(
                        "[0-9a-f]{64}",
                    ),
                ) ==
                true,
        )
    }

    @Test
    fun redirectAndMalformedResponsesFailAtTheConnectionBoundary() {
        for (
            result in
            listOf(
                DirectCompanionConnection.HttpResult(
                    status = 302,
                    body = "",
                    redirectLocation = "https://example.com/",
                ),
                DirectCompanionConnection.HttpResult(
                    status = 200,
                    body = "{}",
                ),
            )
        ) {
            assertFailsWith<DirectCompanionConnection.ConnectionException> {
                DirectCompanionConnection.invoke(
                    runtime = qualifiedRuntime(),
                    messages =
                        listOf(
                            DirectCompanionConnection.ProviderMessage(
                                role = "user",
                                content = "Hello",
                            ),
                        ),
                    transport =
                        DirectCompanionConnection.Transport {
                            result
                        },
                )
            }
        }
    }

    @Test
    fun callerCannotSupplyEndpointOrModelOutsideQualifiedRuntimeEvidence() {
        val runtime =
            qualifiedRuntime()

        assertEquals(
            HereticProductIdentity.Endpoint,
            runtime.endpoint,
        )
        assertEquals(
            HereticProductIdentity.RequestModel,
            runtime.requestModel,
        )

        // The Connection API contains no endpoint/model argument. Provider messages are the only
        // caller-owned provider input after qualified runtime evidence.
        val result =
            DirectCompanionConnection.invoke(
                runtime = runtime,
                messages =
                    listOf(
                        DirectCompanionConnection.ProviderMessage(
                            role = "user",
                            content = "Hello",
                        ),
                    ),
                transport =
                    DirectCompanionConnection.Transport {
                        DirectCompanionConnection.HttpResult(
                            status = 200,
                            body =
                                """
                                {
                                  "choices": [
                                    {
                                      "message": {
                                        "role": "assistant",
                                        "content": "Hi"
                                      }
                                    }
                                  ]
                                }
                                """.trimIndent(),
                        )
                    },
            )

        assertEquals(
            HereticProductIdentity.RequestModel,
            result.model,
        )
    }
}

// [VXG RealForever]
