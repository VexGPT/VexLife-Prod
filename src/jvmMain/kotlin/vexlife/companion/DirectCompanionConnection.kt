package vexlife.companion

import java.security.DigestInputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticProductIdentity
import vexlife.model.heretic.HereticRuntimeState

/**
 * Provider Connection for one direct local Companion inference.
 *
 * The Connection consumes already-qualified Heretic runtime truth. The current qualified-runtime
 * owners admit only the exact ordinary mlx-lm server binding whose process arguments keep
 * `enable_thinking=false` by default. Over that already-qualified runtime, this Connection ports the
 * existing server-owned explicit-open Companion relationship by selecting request-local
 * `chat_template_kwargs.enable_thinking=true` for every fresh inference.
 *
 * This reasoning-mode selection is runtime/Connection meaning, not presentation preference.
 * The Connection otherwise owns only the numeric-loopback HTTP effect plus normalization of the
 * returned provider observation/invocation evidence. It does not own Home, Conversation
 * persistence, prompt-context selection, Memory, runtime construction/lifetime, model identity,
 * human disclosure, or presentation.
 */
object DirectCompanionConnection {
    const val OutputTokenLimit =
        16_000

    const val RuntimeObservationSchema =
        "vexlife-prod.model-runtime-observation/v1"

    const val InvocationEvidenceSchema =
        "vexlife-prod.model-runtime-invocation-evidence/v1"

    private const val RequestTimeoutMs =
        30_000L

    private val JsonCodec =
        Json {
            encodeDefaults = true
            explicitNulls = true
            isLenient = false
        }

    enum class FailureCode {
        RuntimeNotQualified,
        EndpointNotLoopback,
        EndpointUnreachable,
        EndpointTimeout,
        EndpointHttpError,
        EndpointResponseInvalid,
    }

    class ConnectionException(
        val code: FailureCode,
        message: String,
        cause: Throwable? = null,
    ) : IllegalStateException(
        message,
        cause,
    )

    @Serializable
    private data class RequestPayload(
        val model: String,
        val messages: List<CompanionProviderMessage>,
        val max_tokens: Int,
        val stream: Boolean = true,
        val stream_options: Map<String, Boolean> =
            mapOf(
                "include_usage" to true,
            ),
        val temperature: Double = 1.0,
        val top_p: Double = 0.95,
        val top_k: Int = 20,
        val min_p: Double = 0.0,
        val presence_penalty: Double = 0.0,
        val repetition_penalty: Double = 1.0,
        val chat_template_kwargs: Map<String, Boolean>,
    )

    data class HttpCall(
        val uri: URI,
        val body: String,
        val timeoutMs: Long,
    )

    data class HttpResult(
        val status: Int,
        val body: String,
        val redirectLocation: String? = null,
    )

    data class StreamingHttpResult(
        val status: Int,
        val responseBodySha256: String,
        val redirectLocation: String? = null,
    )

    enum class ProgressKind {
        Reasoning,
        Content,
    }

    data class Progress(
        val kind: ProgressKind,
        val delta: String,
        val accumulatedCharacters: Int,
    )

    fun interface ProgressObserver {
        fun observe(
            progress: Progress,
        )
    }

    fun interface Transport {
        fun post(
            call: HttpCall,
        ): HttpResult
    }

    interface StreamingTransport :
        Transport {
        fun postStreaming(
            call: HttpCall,
            onLine: (String) -> Unit,
        ): StreamingHttpResult
    }

    object SystemTransport : StreamingTransport {
        private val client =
            HttpClient
                .newBuilder()
                .followRedirects(
                    HttpClient.Redirect.NEVER,
                )
                .build()

        override fun post(
            call: HttpCall,
        ): HttpResult {
            val request =
                request(
                    call,
                )
            val response =
                client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(
                        StandardCharsets.UTF_8,
                    ),
                )

            return HttpResult(
                status = response.statusCode(),
                body = response.body(),
                redirectLocation = redirectLocation(
                    response,
                ),
            )
        }

        override fun postStreaming(
            call: HttpCall,
            onLine: (String) -> Unit,
        ): StreamingHttpResult {
            val response =
                client.send(
                    request(
                        call,
                    ),
                    HttpResponse.BodyHandlers.ofInputStream(),
                )
            val digest =
                MessageDigest.getInstance(
                    "SHA-256",
                )

            DigestInputStream(
                response.body(),
                digest,
            )
                .bufferedReader(
                    StandardCharsets.UTF_8,
                )
                .use {
                    reader ->
                    while (
                        true
                    ) {
                        val line =
                            reader.readLine()
                                ?: break
                        if (
                            response.statusCode() in
                                200..299
                        ) {
                            onLine(
                                line,
                            )
                        }
                    }
                }

            return StreamingHttpResult(
                status = response.statusCode(),
                responseBodySha256 = hex(
                    digest.digest(),
                ),
                redirectLocation = redirectLocation(
                    response,
                ),
            )
        }

        private fun request(
            call: HttpCall,
        ): HttpRequest =
            HttpRequest
                .newBuilder(
                    call.uri,
                )
                .timeout(
                    Duration.ofMillis(
                        call.timeoutMs,
                    ),
                )
                .header(
                    "content-type",
                    "application/json",
                )
                .header(
                    "accept",
                    "text/event-stream",
                )
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        call.body,
                        StandardCharsets.UTF_8,
                    ),
                )
                .build()

        private fun redirectLocation(
            response: HttpResponse<*>,
        ): String? =
            response
                .headers()
                .firstValue(
                    "location",
                )
                .orElse(
                    null,
                )
    }

    @Serializable
    @ConsistentCopyVisibility
    data class ReportedModelField internal constructor(
        val present: Boolean,
        val valueType: String?,
        val byteLength: Int?,
        val valueSha256: String?,
        val pathClass: String?,
        val rawValuePersisted: Boolean,
    )

    @Serializable
    @ConsistentCopyVisibility
    data class ReasoningTrace internal constructor(
        val present: Boolean,
        val contentSha256: String?,
        val characterCount: Int,
        val rawPersisted: Boolean,
        val humanProjection: String,
        val modelProjection: String,
        val trainingProjection: String,
    )

    private data class ReasoningObservation(
        val trace: ReasoningTrace,
        val sealedHumanValue: String?,
    )

    private data class ProviderCompletion(
        val httpStatus: Int,
        val responseBodySha256: String,
        val content: String,
        val reportedModel: String?,
        val reasoning: ReasoningObservation,
        val finishReason: String?,
        val assistantRoleObserved: Boolean,
    )

    @Serializable
    @ConsistentCopyVisibility
    data class RuntimeObservation internal constructor(
        val schemaVersion: String,
        val truthClass: String,
        val runtimeReceiptRef: String,
        val endpointOrigin: String,
        val modelRef: String,
        val modelProfileRef: String,
        val observedAt: String,
        val responseBodySha256: String,
        val contentSha256: String,
        val contentCharacters: Int,
        val assistantRoleObserved: Boolean,
        val finishReasonOrNull: String?,
        val compatibilityModel: String,
        val reportedModelField: ReportedModelField,
        val reasoningTrace: ReasoningTrace,
        val rawProviderResponsePersisted: Boolean,
        val observationRef: String,
        val semanticFingerprint: String,
    )

    @Serializable
    @ConsistentCopyVisibility
    data class ObservedEffects internal constructor(
        val modelRuntimeObserved: Boolean,
        val providerOrNetworkObserved: Boolean,
        val nativeToolExecutionObserved: Boolean,
        val multimodalInputObserved: Boolean,
        val trainingEffectObserved: Boolean,
        val modelWeightEffectObserved: Boolean,
    )

    @Serializable
    @ConsistentCopyVisibility
    data class InvocationEvidence internal constructor(
        val schemaVersion: String,
        val truthClass: String,
        val runtimeReceiptRef: String,
        val endpointOrigin: String,
        val requestBodySha256: String,
        val responseBodySha256: String,
        val runtimeObservationRef: String,
        val runtimeObservationFingerprint: String,
        val httpStatus: Int,
        val actualHttpCall: Boolean,
        val loopbackOnly: Boolean,
        val observedEffects: ObservedEffects,
        val formedAt: String,
        val invocationRef: String,
        val semanticFingerprint: String,
    )

    @ConsistentCopyVisibility
    data class Result internal constructor(
        val content: String,
        val model: String,
        /**
         * Ephemeral human-only projection candidate. This value is never included in the durable
         * runtime observation, invocation evidence, canonical Conversation, Context, Memory,
         * training, or publication surfaces.
         */
        val sealedHumanReasoning: String?,
        val runtimeObservation: RuntimeObservation,
        val invocationEvidence: InvocationEvidence,
    )

    fun invoke(
        runtime: HereticModelConfiguration.QualifiedRuntimeEvidence,
        messages: List<CompanionProviderMessage>,
        transport: Transport = SystemTransport,
        progressObserver: ProgressObserver? = null,
        now: () -> Instant = {
            Instant.now()
        },
    ): Result {
        requireQualifiedRuntime(
            runtime,
        )
        require(
            messages.isNotEmpty(),
        ) {
            "direct Companion Connection requires at least one provider-ready message"
        }

        val origin =
            admittedOrigin(
                runtime.endpoint,
            )
        val uri =
            origin.resolve(
                "/v1/chat/completions",
            )
        val requestBody =
            JsonCodec.encodeToString(
                RequestPayload(
                    model = runtime.requestModel,
                    messages = messages,
                    max_tokens = OutputTokenLimit,
                    stream = true,
                    chat_template_kwargs =
                        mapOf(
                            "enable_thinking" to
                                true,
                        ),
                ),
            )
        val requestBodySha256 =
            sha256(
                requestBody,
            )

        val call =
            HttpCall(
                uri = uri,
                body = requestBody,
                timeoutMs = RequestTimeoutMs,
            )
        val completion =
            if (
                transport is
                    StreamingTransport
            ) {
                invokeStreaming(
                    transport = transport,
                    call = call,
                    progressObserver = progressObserver,
                )
            } else {
                invokeBulk(
                    transport = transport,
                    call = call,
                )
            }

        val content =
            completion.content
        val observedAt =
            now()
                .toString()
        val responseBodySha256 =
            completion.responseBodySha256
        val compatibilityModel =
            safeCompatibilityModel(
                completion.reportedModel,
                runtime.requestModel,
            )
        val reportedModelField =
            reportedModelField(
                completion.reportedModel,
            )
        val reasoning =
            completion.reasoning
        val finishReason =
            completion.finishReason
        val assistantRoleObserved =
            completion.assistantRoleObserved

        val observationFingerprint =
            fingerprint(
                RuntimeObservationSchema,
                runtime.runtimeReceiptRef,
                origin.toString(),
                runtime.modelRef,
                runtime.modelProfileRef,
                observedAt,
                responseBodySha256,
                sha256(
                    content,
                ),
                content.codePointCount(
                    0,
                    content.length,
                ).toString(),
                assistantRoleObserved.toString(),
                finishReason
                    ?: "",
                compatibilityModel,
                reportedModelField.valueSha256
                    ?: "",
                reportedModelField.pathClass
                    ?: "",
                reasoning.trace.contentSha256
                    ?: "",
                reasoning.trace.characterCount.toString(),
            )
        val runtimeObservation =
            RuntimeObservation(
                schemaVersion = RuntimeObservationSchema,
                truthClass = "EXTERNAL_RUNTIME_OBSERVATION",
                runtimeReceiptRef = runtime.runtimeReceiptRef,
                endpointOrigin = origin.toString(),
                modelRef = runtime.modelRef,
                modelProfileRef = runtime.modelProfileRef,
                observedAt = observedAt,
                responseBodySha256 = responseBodySha256,
                contentSha256 =
                    sha256(
                        content,
                    ),
                contentCharacters =
                    content.codePointCount(
                        0,
                        content.length,
                    ),
                assistantRoleObserved = assistantRoleObserved,
                finishReasonOrNull = finishReason,
                compatibilityModel = compatibilityModel,
                reportedModelField = reportedModelField,
                reasoningTrace = reasoning.trace,
                rawProviderResponsePersisted = false,
                observationRef =
                    "observation.vexlife-prod.model-runtime." +
                        observationFingerprint.take(
                            32,
                        ),
                semanticFingerprint = observationFingerprint,
            )

        val effects =
            ObservedEffects(
                modelRuntimeObserved = true,
                providerOrNetworkObserved = true,
                nativeToolExecutionObserved = false,
                multimodalInputObserved = false,
                trainingEffectObserved = false,
                modelWeightEffectObserved = false,
            )
        val invocationFingerprint =
            fingerprint(
                InvocationEvidenceSchema,
                runtime.runtimeReceiptRef,
                origin.toString(),
                requestBodySha256,
                responseBodySha256,
                runtimeObservation.observationRef,
                runtimeObservation.semanticFingerprint,
                completion.httpStatus.toString(),
                observedAt,
            )
        val invocation =
            InvocationEvidence(
                schemaVersion = InvocationEvidenceSchema,
                truthClass = "EXTERNAL_RUNTIME_INVOCATION_EVIDENCE",
                runtimeReceiptRef = runtime.runtimeReceiptRef,
                endpointOrigin = origin.toString(),
                requestBodySha256 = requestBodySha256,
                responseBodySha256 = responseBodySha256,
                runtimeObservationRef = runtimeObservation.observationRef,
                runtimeObservationFingerprint = runtimeObservation.semanticFingerprint,
                httpStatus = completion.httpStatus,
                actualHttpCall = true,
                loopbackOnly = true,
                observedEffects = effects,
                formedAt = observedAt,
                invocationRef =
                    "evidence.vexlife-prod.model-runtime-invocation." +
                        invocationFingerprint.take(
                            32,
                        ),
                semanticFingerprint = invocationFingerprint,
            )

        return Result(
            content = content,
            model = compatibilityModel,
            sealedHumanReasoning = reasoning.sealedHumanValue,
            runtimeObservation = runtimeObservation,
            invocationEvidence = invocation,
        )
    }

    private fun invokeBulk(
        transport: Transport,
        call: HttpCall,
    ): ProviderCompletion {
        val http =
            post(
                transport,
                call,
            )
        validateHttp(
            status = http.status,
            redirectLocation = http.redirectLocation,
        )

        val parsed =
            try {
                Json.parseToJsonElement(
                    http.body,
                )
                    .jsonObject
            } catch (
                error: Exception,
            ) {
                fail(
                    FailureCode.EndpointResponseInvalid,
                    "local Companion endpoint returned invalid JSON",
                    error,
                )
            }
        val choice =
            parsed[
                "choices"
            ]
                ?.let {
                    it as? JsonArray
                }
                ?.firstOrNull()
                ?.let {
                    it as? JsonObject
                }
                ?: fail(
                    FailureCode.EndpointResponseInvalid,
                    "local Companion response has no first choice",
                )
        val message =
            choice[
                "message"
            ] as? JsonObject
                ?: fail(
                    FailureCode.EndpointResponseInvalid,
                    "local Companion response has no assistant message",
                )
        val content =
            stringOrNull(
                message[
                    "content"
                ],
            )
                ?.takeIf(
                    String::isNotEmpty,
                )
                ?: fail(
                    FailureCode.EndpointResponseInvalid,
                    "local Companion response content is empty",
                )

        return ProviderCompletion(
            httpStatus = http.status,
            responseBodySha256 = sha256(
                http.body,
            ),
            content = content,
            reportedModel =
                stringOrNull(
                    parsed[
                        "model"
                    ],
                ),
            reasoning = reasoningObservation(
                message,
            ),
            finishReason =
                stringOrNull(
                    choice[
                        "finish_reason"
                    ],
                ),
            assistantRoleObserved =
                stringOrNull(
                    message[
                        "role"
                    ],
                ) ==
                    "assistant",
        )
    }

    private fun invokeStreaming(
        transport: StreamingTransport,
        call: HttpCall,
        progressObserver: ProgressObserver?,
    ): ProviderCompletion {
        val content =
            StringBuilder()
        val reasoning =
            StringBuilder()
        var reportedModel: String? =
            null
        var finishReason: String? =
            null
        var assistantRoleObserved =
            false
        var done =
            false

        val http =
            postStreaming(
                transport = transport,
                call = call,
            ) {
                line ->
                if (
                    line.isBlank() ||
                    line.startsWith(
                        ":",
                    )
                ) {
                    return@postStreaming
                }
                if (
                    !line.startsWith(
                        "data:",
                    )
                ) {
                    fail(
                        FailureCode.EndpointResponseInvalid,
                        "streaming Companion endpoint emitted a non-SSE line",
                    )
                }
                val data =
                    line.removePrefix(
                        "data:",
                    )
                        .trimStart()
                if (
                    data ==
                        "[DONE]"
                ) {
                    done =
                        true
                    return@postStreaming
                }

                val event =
                    try {
                        Json.parseToJsonElement(
                            data,
                        )
                            .jsonObject
                    } catch (
                        error: Exception,
                    ) {
                        fail(
                            FailureCode.EndpointResponseInvalid,
                            "streaming Companion endpoint emitted invalid SSE JSON",
                            error,
                        )
                    }

                stringOrNull(
                    event[
                        "model"
                    ],
                )
                    ?.let {
                        reportedModel =
                            it
                    }
                val choice =
                    (event[
                        "choices"
                    ] as? JsonArray)
                        ?.firstOrNull() as? JsonObject
                        ?: return@postStreaming
                stringOrNull(
                    choice[
                        "finish_reason"
                    ],
                )
                    ?.let {
                        finishReason =
                            it
                    }
                val delta =
                    choice[
                        "delta"
                    ] as? JsonObject
                        ?: return@postStreaming
                if (
                    stringOrNull(
                        delta[
                            "role"
                        ],
                    ) ==
                        "assistant"
                ) {
                    assistantRoleObserved =
                        true
                }

                val mlxReasoning =
                    stringOrNull(
                        delta[
                            "reasoning"
                        ],
                    )
                        ?.takeIf(
                            String::isNotEmpty,
                        )
                val compatibilityReasoning =
                    stringOrNull(
                        delta[
                            "reasoning_content"
                        ],
                    )
                        ?.takeIf(
                            String::isNotEmpty,
                        )
                if (
                    mlxReasoning !=
                        null &&
                        compatibilityReasoning !=
                        null &&
                        mlxReasoning !=
                        compatibilityReasoning
                ) {
                    fail(
                        FailureCode.EndpointResponseInvalid,
                        "streaming Companion endpoint exposed conflicting reasoning fields",
                    )
                }
                val reasoningDelta =
                    mlxReasoning
                        ?: compatibilityReasoning
                if (
                    reasoningDelta !=
                        null
                ) {
                    reasoning.append(
                        reasoningDelta,
                    )
                    progressObserver
                        ?.observe(
                            Progress(
                                kind = ProgressKind.Reasoning,
                                delta = reasoningDelta,
                                accumulatedCharacters =
                                    reasoning.codePointCount(),
                            ),
                        )
                }

                stringOrNull(
                    delta[
                        "content"
                    ],
                )
                    ?.takeIf(
                        String::isNotEmpty,
                    )
                    ?.let {
                        contentDelta ->
                        content.append(
                            contentDelta,
                        )
                        progressObserver
                            ?.observe(
                                Progress(
                                    kind = ProgressKind.Content,
                                    delta = contentDelta,
                                    accumulatedCharacters =
                                        content.codePointCount(),
                                ),
                            )
                    }
            }

        validateHttp(
            status = http.status,
            redirectLocation = http.redirectLocation,
        )
        if (
            !done
        ) {
            fail(
                FailureCode.EndpointResponseInvalid,
                "streaming Companion endpoint closed before [DONE]",
            )
        }
        if (
            content.isEmpty()
        ) {
            fail(
                FailureCode.EndpointResponseInvalid,
                "streaming Companion response content is empty",
            )
        }

        val reasoningValue =
            reasoning
                .toString()
                .takeIf(
                    String::isNotEmpty,
                )
        val message =
            buildMap<String, JsonElement> {
                put(
                    "content",
                    JsonPrimitive(
                        content.toString(),
                    ),
                )
                reasoningValue
                    ?.let {
                        put(
                            "reasoning",
                            JsonPrimitive(
                                it,
                            ),
                        )
                    }
            }

        return ProviderCompletion(
            httpStatus = http.status,
            responseBodySha256 = http.responseBodySha256,
            content = content.toString(),
            reportedModel = reportedModel,
            reasoning =
                reasoningObservation(
                    JsonObject(
                        message,
                    ),
                ),
            finishReason = finishReason,
            assistantRoleObserved = assistantRoleObserved,
        )
    }

    private fun post(
        transport: Transport,
        call: HttpCall,
    ): HttpResult =
        try {
            transport.post(
                call,
            )
        } catch (
            error: java.net.http.HttpTimeoutException,
        ) {
            fail(
                FailureCode.EndpointTimeout,
                "local Companion endpoint request timed out",
                error,
            )
        } catch (
            error: java.net.ConnectException,
        ) {
            fail(
                FailureCode.EndpointUnreachable,
                "local Companion endpoint could not be reached",
                error,
            )
        } catch (
            error: Exception,
        ) {
            fail(
                FailureCode.EndpointUnreachable,
                "local Companion endpoint invocation failed",
                error,
            )
        }

    private fun postStreaming(
        transport: StreamingTransport,
        call: HttpCall,
        onLine: (String) -> Unit,
    ): StreamingHttpResult =
        try {
            transport.postStreaming(
                call,
                onLine,
            )
        } catch (
            error: java.net.http.HttpTimeoutException,
        ) {
            fail(
                FailureCode.EndpointTimeout,
                "local Companion endpoint did not begin streaming before timeout",
                error,
            )
        } catch (
            error: java.net.ConnectException,
        ) {
            fail(
                FailureCode.EndpointUnreachable,
                "local Companion endpoint could not be reached",
                error,
            )
        } catch (
            error: ConnectionException,
        ) {
            throw error
        } catch (
            error: Exception,
        ) {
            fail(
                FailureCode.EndpointUnreachable,
                "local Companion streaming invocation failed",
                error,
            )
        }

    private fun validateHttp(
        status: Int,
        redirectLocation: String?,
    ) {
        if (
            status in
            300..399 ||
                redirectLocation !=
                null
        ) {
            fail(
                FailureCode.EndpointNotLoopback,
                "local Companion endpoint redirects are not admitted",
            )
        }
        if (
            status !in
            200..299
        ) {
            fail(
                FailureCode.EndpointHttpError,
                "local Companion endpoint returned HTTP $status",
            )
        }
    }

    private fun StringBuilder.codePointCount(): Int =
        toString()
            .codePointCount(
                0,
                length,
            )

    private fun requireQualifiedRuntime(
        runtime: HereticModelConfiguration.QualifiedRuntimeEvidence,
    ) {
        if (
            runtime.runtimeState !=
                HereticRuntimeState.Qualified
        ) {
            fail(
                FailureCode.RuntimeNotQualified,
                "direct Companion Connection requires one qualified Heretic runtime",
            )
        }
        require(
            runtime.modelRef ==
                HereticProductIdentity.ModelRef &&
                runtime.modelProfileRef ==
                HereticProductIdentity.ModelProfileRef &&
                runtime.endpoint ==
                HereticProductIdentity.Endpoint &&
                runtime.requestModel ==
                HereticProductIdentity.RequestModel,
        ) {
            "qualified runtime evidence no longer matches certified Heretic product identity"
        }
    }

    private fun admittedOrigin(
        endpoint: String,
    ): URI {
        val uri =
            try {
                URI(
                    endpoint,
                )
            } catch (
                error: Exception,
            ) {
                fail(
                    FailureCode.EndpointNotLoopback,
                    "qualified local Companion endpoint is invalid",
                    error,
                )
            }

        require(
            uri.scheme ==
                "http" &&
                uri.userInfo ==
                null &&
                uri.query ==
                null &&
                uri.fragment ==
                null &&
                uri.path
                    .let {
                        it.isNullOrEmpty() ||
                            it ==
                            "/"
                    } &&
                (
                    uri.host ==
                        "127.0.0.1" ||
                        uri.host ==
                        "::1"
                ) &&
                uri.port >
                0,
        ) {
            "direct Companion Connection accepts numeric-loopback HTTP origins only"
        }

        return URI(
            uri.scheme,
            null,
            uri.host,
            uri.port,
            null,
            null,
            null,
        )
    }

    private fun safeCompatibilityModel(
        reported: String?,
        fallback: String,
    ): String {
        val safe =
            reported
                ?.takeIf(
                    String::isNotEmpty,
                )
                ?.takeIf {
                    it.length <=
                        256 &&
                        !pathLike(
                            it,
                        )
                }

        return safe
            ?: fallback
    }

    private fun reportedModelField(
        value: String?,
    ): ReportedModelField {
        if (
            value ==
                null
        ) {
            return ReportedModelField(
                present = false,
                valueType = null,
                byteLength = null,
                valueSha256 = null,
                pathClass = null,
                rawValuePersisted = false,
            )
        }

        return ReportedModelField(
            present = true,
            valueType = "string",
            byteLength =
                value.toByteArray(
                    StandardCharsets.UTF_8,
                ).size,
            valueSha256 =
                sha256(
                    value,
                ),
            pathClass =
                if (
                    pathLike(
                        value,
                    )
                ) {
                    "LOCAL_PATH_LIKE"
                } else {
                    "OPAQUE_MODEL_ID"
                },
            rawValuePersisted = false,
        )
    }

    private fun reasoningObservation(
        message: JsonObject,
    ): ReasoningObservation {
        val mlx =
            stringOrNull(
                message[
                    "reasoning"
                ],
            )
                ?.takeIf(
                    String::isNotEmpty,
                )
        val compatibility =
            stringOrNull(
                message[
                    "reasoning_content"
                ],
            )
                ?.takeIf(
                    String::isNotEmpty,
                )

        if (
            mlx !=
                null &&
            compatibility !=
                null &&
            mlx !=
                compatibility
        ) {
            fail(
                FailureCode.EndpointResponseInvalid,
                "local Companion response exposed conflicting reasoning fields",
            )
        }

        val value =
            mlx
                ?: compatibility
        val trace =
            if (
                value ==
                    null
            ) {
                ReasoningTrace(
                    present = false,
                    contentSha256 = null,
                    characterCount = 0,
                    rawPersisted = false,
                    humanProjection = "SEALED_EXPLICIT_OPEN_ONLY",
                    modelProjection = "NONE",
                    trainingProjection = "NONE",
                )
            } else {
                ReasoningTrace(
                    present = true,
                    contentSha256 =
                        sha256(
                            value,
                        ),
                    characterCount =
                        value.codePointCount(
                            0,
                            value.length,
                        ),
                    rawPersisted = false,
                    humanProjection = "SEALED_EXPLICIT_OPEN_ONLY",
                    modelProjection = "NONE",
                    trainingProjection = "NONE",
                )
            }

        return ReasoningObservation(
            trace = trace,
            sealedHumanValue = value,
        )
    }

    private fun stringOrNull(
        value: JsonElement?,
    ): String? =
        when (
            value
        ) {
            null,
            JsonNull,
            ->
                null

            is JsonPrimitive ->
                if (
                    value.isString
                ) {
                    value.contentOrNull
                } else {
                    null
                }

            else ->
                null
        }

    private fun pathLike(
        value: String,
    ): Boolean =
        value.contains(
            "/",
        ) ||
            value.contains(
                "\\",
            ) ||
            Regex(
                "^[A-Za-z]:[\\\\/]",
            )
                .containsMatchIn(
                    value,
                )

    private fun sha256(
        value: String,
    ): String =
        hex(
            MessageDigest
                .getInstance(
                    "SHA-256",
                )
                .digest(
                    value.toByteArray(
                        StandardCharsets.UTF_8,
                    ),
                ),
        )

    private fun hex(
        value: ByteArray,
    ): String =
        value.joinToString(
            "",
        ) {
            "%02x".format(
                it.toInt() and
                    0xff,
            )
        }

    private fun fingerprint(
        vararg values: String,
    ): String =
        sha256(
            values.joinToString(
                separator = "\u0000",
            ),
        )

    private fun fail(
        code: FailureCode,
        message: String,
        cause: Throwable? = null,
    ): Nothing =
        throw ConnectionException(
            code = code,
            message = message,
            cause = cause,
        )
}

// [VXG RealForever]
