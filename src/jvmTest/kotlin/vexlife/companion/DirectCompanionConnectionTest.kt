package vexlife.companion

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.net.URI
import java.nio.charset.StandardCharsets
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
                        CompanionProviderMessage(
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
        assertEquals(
            120_000L,
            request.timeoutMs,
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
            "true",
            body[
                "stream"
            ]
                ?.jsonPrimitive
                ?.content,
        )
        assertEquals(
            "1.0",
            body[
                "temperature"
            ]
                ?.jsonPrimitive
                ?.content,
        )
        assertEquals(
            "0.95",
            body[
                "top_p"
            ]
                ?.jsonPrimitive
                ?.content,
        )
        assertEquals(
            "20",
            body[
                "top_k"
            ]
                ?.jsonPrimitive
                ?.content,
        )
        assertFalse(
            body.containsKey(
                "min_p",
            ),
        )
        assertFalse(
            body.containsKey(
                "presence_penalty",
            ),
        )
        assertFalse(
            body.containsKey(
                "repetition_penalty",
            ),
        )
        assertEquals(
            "true",
            body[
                "chat_template_kwargs"
            ]
                ?.jsonObject
                ?.get(
                    "enable_thinking",
                )
                ?.jsonPrimitive
                ?.content,
        )
        assertEquals(
            DirectCompanionConnection.InvocationEvidenceSchema,
            result.invocationEvidence.schemaVersion,
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
    fun systemStreamingShapeProjectsReasoningAndContentDeltasBeforeFinalResult() {
        var streamedCall: DirectCompanionConnection.HttpCall? =
            null
        val progress =
            mutableListOf<DirectCompanionConnection.Progress>()

        val transport =
            object :
                DirectCompanionConnection.StreamingTransport {
                override fun post(
                    call: DirectCompanionConnection.HttpCall,
                ): DirectCompanionConnection.HttpResult =
                    error(
                        "streaming path must not fall back to bulk transport",
                    )

                override fun postStreaming(
                    call: DirectCompanionConnection.HttpCall,
                    onLine: (String) -> Unit,
                ): DirectCompanionConnection.StreamingHttpResult {
                    streamedCall =
                        call
                    onLine(
                        """data: {"model":"default_model","choices":[{"delta":{"role":"assistant","reasoning":"I should "}}]}""",
                    )
                    onLine(
                        "",
                    )
                    onLine(
                        """data: {"model":"default_model","choices":[{"delta":{"role":"assistant","reasoning":"compare."}}]}""",
                    )
                    onLine(
                        "",
                    )
                    onLine(
                        """data: {"model":"default_model","choices":[{"delta":{"role":"assistant","content":"The better "}}]}""",
                    )
                    onLine(
                        "",
                    )
                    onLine(
                        """data: {"model":"default_model","choices":[{"finish_reason":"stop","delta":{"role":"assistant","content":"choice."}}]}""",
                    )
                    onLine(
                        "",
                    )
                    onLine(
                        "data: [DONE]",
                    )
                    onLine(
                        "",
                    )
                    return DirectCompanionConnection.StreamingHttpResult(
                        status = 200,
                        responseBodySha256 = "a".repeat(
                            64,
                        ),
                    )
                }
            }

        val result =
            DirectCompanionConnection.invoke(
                runtime = qualifiedRuntime(),
                messages =
                    listOf(
                        CompanionProviderMessage(
                            role = "user",
                            content = "Compare these approaches",
                        ),
                    ),
                transport = transport,
                progressObserver =
                    DirectCompanionConnection.ProgressObserver {
                        progress +=
                            it
                    },
                now = {
                    Instant.parse(
                        "2026-11-10T04:40:00Z",
                    )
                },
            )

        assertEquals(
            "true",
            Json
                .parseToJsonElement(
                    requireNotNull(
                        streamedCall,
                    )
                        .body,
                )
                .jsonObject[
                    "stream"
                ]
                ?.jsonPrimitive
                ?.content,
        )
        assertEquals(
            "The better choice.",
            result.content,
        )
        assertEquals(
            "I should compare.",
            result.sealedHumanReasoning,
        )
        assertEquals(
            listOf(
                DirectCompanionConnection.ProgressKind.Reasoning,
                DirectCompanionConnection.ProgressKind.Reasoning,
                DirectCompanionConnection.ProgressKind.Content,
                DirectCompanionConnection.ProgressKind.Content,
            ),
            progress.map(
                DirectCompanionConnection.Progress::kind,
            ),
        )
        assertEquals(
            "I should ",
            progress[0].delta,
        )
        assertEquals(
            "The better ",
            progress[2].delta,
        )
    }

    @Test
    fun systemStreamingTransportRequestTimeoutDoesNotCapActiveBody() {
        val server =
            HttpServer.create(
                InetSocketAddress(
                    "127.0.0.1",
                    0,
                ),
                0,
            )
        server.createContext(
            "/v1/chat/completions",
        ) {
            exchange ->
            exchange.responseHeaders.set(
                "content-type",
                "text/event-stream",
            )
            exchange.sendResponseHeaders(
                200,
                0,
            )
            // Force response-start/header delivery before delaying the body. This proves the
            // HttpRequest timeout bounds stream establishment rather than active body lifetime.
            exchange.responseBody.flush()
            Thread.sleep(
                1_250,
            )
            exchange.responseBody.write(
                "data: [DONE]\n\n"
                    .toByteArray(
                        StandardCharsets.UTF_8,
                    ),
            )
            exchange.close()
        }
        server.start()

        try {
            val lines =
                mutableListOf<String>()
            val result =
                DirectCompanionConnection.SystemTransport.postStreaming(
                    DirectCompanionConnection.HttpCall(
                        uri =
                            URI(
                                "http://127.0.0.1:" +
                                    server.address.port +
                                    "/v1/chat/completions",
                            ),
                        body = "{}",
                        timeoutMs = 1_000,
                    ),
                ) {
                    lines +=
                        it
                }

            assertEquals(
                200,
                result.status,
            )
            assertTrue(
                "data: [DONE]" in
                    lines,
            )
        } finally {
            server.stop(
                0,
            )
        }
    }

    @Test
    fun providerReportedLocalModelPathNeverBecomesProductIdentityOrPersistedRawValue() {
        var call: DirectCompanionConnection.HttpCall? =
            null

        val result =
            DirectCompanionConnection.invoke(
                runtime = qualifiedRuntime(),
                messages =
                    listOf(
                        CompanionProviderMessage(
                            role = "user",
                            content = "Hello",
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

        val body =
            Json
                .parseToJsonElement(
                    requireNotNull(
                        call,
                    )
                        .body,
                )
                .jsonObject
        assertEquals(
            "true",
            body[
                "chat_template_kwargs"
            ]
                ?.jsonObject
                ?.get(
                    "enable_thinking",
                )
                ?.jsonPrimitive
                ?.content,
        )

        assertEquals(
            DirectCompanionConnection.InvocationEvidenceSchema,
            result.invocationEvidence.schemaVersion,
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
        assertEquals(
            "private reasoning",
            result.sealedHumanReasoning,
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
                            CompanionProviderMessage(
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
                        CompanionProviderMessage(
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
