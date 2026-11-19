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
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import vexlife.capability.VexLifeCapabilityAtlas
import vexlife.capability.VexLifeCapabilityRequest
import vexlife.contract.VexContractJson
import vexlife.model.heretic.HereticCompanionRepresentation
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticProductIdentity
import vexlife.model.heretic.HereticRuntimeRealization
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
    const val RuntimeObservationSchema =
        "vexlife-prod.model-runtime-observation/v1"

    const val InvocationEvidenceSchema =
        "vexlife-prod.model-runtime-invocation-evidence/v1"

    // Preserves the known-working browser Companion's bounded inference-establishment window.
    // With BodyHandlers.ofInputStream(), this bounds response start/headers, not active SSE body
    // consumption after the stream has been established.
    private const val RequestTimeoutMs =
        120_000L

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
        /** What the stage cost and why it stopped, when the provider answered at all. */
        val measurement: StageMeasurement? = null,
    ) : IllegalStateException(
        message,
        cause,
    )

    enum class StageStop(
        val wire: String,
    ) {
        COMPLETED("COMPLETED"),
        OUTPUT_LIMIT_DURING_REASONING("OUTPUT_LIMIT_DURING_REASONING"),
        OUTPUT_LIMIT_DURING_ANSWER("OUTPUT_LIMIT_DURING_ANSWER"),
        STREAM_CLOSED_BEFORE_DONE("STREAM_CLOSED_BEFORE_DONE"),
        ENDED_WITHOUT_ANSWER("ENDED_WITHOUT_ANSWER"),
    }

    data class StageMeasurement(
        val stop: StageStop,
        val finishReason: String?,
        val promptTokens: Int?,
        val cachedPromptTokens: Int?,
        val completionTokens: Int?,
        val firstTokenMillis: Long?,
        val firstAnswerMillis: Long?,
        val totalMillis: Long,
        val reasoningCharacters: Int,
        val answerCharacters: Int,
        val controlHeaderInReasoning: Boolean,
        val controlHeaderInAnswer: Boolean,
    ) {
        val prefillTokens: Int?
            get() = promptTokens?.let { prompt -> prompt - (cachedPromptTokens ?: 0) }

        fun refs(): Map<String, String> = buildMap {
            put("stop", stop.wire)
            finishReason?.let { put("finishReason", it) }
            promptTokens?.let { put("promptTokens", it.toString()) }
            cachedPromptTokens?.let { put("cachedPromptTokens", it.toString()) }
            prefillTokens?.let { put("prefillTokens", it.toString()) }
            completionTokens?.let { put("completionTokens", it.toString()) }
            firstTokenMillis?.let { put("firstTokenMs", it.toString()) }
            firstAnswerMillis?.let { put("firstAnswerMs", it.toString()) }
            put("totalMs", totalMillis.toString())
            put("reasoningChars", reasoningCharacters.toString())
            put("answerChars", answerCharacters.toString())
            put("controlHeaderInReasoning", controlHeaderInReasoning.toString())
            put("controlHeaderInAnswer", controlHeaderInAnswer.toString())
        }
    }

    private val ControlHeaders =
        listOf(
            VexLifeCapabilityRequest.RequestHeader,
            DirectConversationSourceReturn.RequestHeader,
        )

    private fun containsControlHeader(text: CharSequence): Boolean =
        ControlHeaders.any { text.contains(it) }

    private data class Usage(
        val promptTokens: Int?,
        val cachedPromptTokens: Int?,
        val completionTokens: Int?,
    )

    private fun usageOrNull(element: JsonElement?): Usage? {
        val usage = element as? JsonObject ?: return null
        fun count(value: JsonElement?): Int? =
            (value as? JsonPrimitive)?.contentOrNull?.toIntOrNull()
        return Usage(
            promptTokens = count(usage["prompt_tokens"]),
            cachedPromptTokens =
                count((usage["prompt_tokens_details"] as? JsonObject)?.get("cached_tokens")),
            completionTokens = count(usage["completion_tokens"]),
        )
    }

    private fun stopFor(done: Boolean, finishReason: String?, answered: Boolean): StageStop =
        when {
            !done -> StageStop.STREAM_CLOSED_BEFORE_DONE
            finishReason == "length" && !answered -> StageStop.OUTPUT_LIMIT_DURING_REASONING
            finishReason == "length" -> StageStop.OUTPUT_LIMIT_DURING_ANSWER
            !answered -> StageStop.ENDED_WITHOUT_ANSWER
            else -> StageStop.COMPLETED
        }

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
        val temperature: Double,
        val top_p: Double,
        val top_k: Int,
        val chat_template_kwargs: Map<String, JsonPrimitive>,
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
                    call = call,
                    applyOverallTimeout = true,
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
                        call = call,
                        applyOverallTimeout = true,
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
            applyOverallTimeout: Boolean,
        ): HttpRequest {
            val builder =
                HttpRequest
                    .newBuilder(
                        call.uri,
                    )

            if (
                applyOverallTimeout
            ) {
                builder.timeout(
                    Duration.ofMillis(
                        call.timeoutMs,
                    ),
                )
            }

            return builder
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
        }

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
        val measurement: StageMeasurement,
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

    @OptIn(ExperimentalSerializationApi::class)
    @Serializable
    @ConsistentCopyVisibility
    data class InvocationEvidence internal constructor(
        val schemaVersion: String,
        val truthClass: String,
        val runtimeReceiptRef: String,
        val endpointOrigin: String,
        val requestBodySha256: String,
        /** Exact native function schema offered to this provider stage, without repeating its text. */
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        val providerToolCount: Int = 0,
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        val providerToolSchemaSha256: String? = null,
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
        /**
         * Exact ephemeral HTTP request body submitted to the local model runtime.
         *
         * The durable invocation evidence retains only [InvocationEvidence.requestBodySha256].
         * This body exists only for the live caller so a human can compare VexLife's projection
         * with the actual provider request without creating a second prompt-history store.
         */
        val providerRequestBody: String,
        /** Diagnostics only; not part of durable runtime evidence fingerprints. */
        val measurement: StageMeasurement,
    )

    fun invoke(
        runtime: HereticModelConfiguration.QualifiedRuntimeEvidence,
        messages: List<CompanionProviderMessage>,
        transport: Transport = SystemTransport,
        progressObserver: ProgressObserver? = null,
        now: () -> Instant = {
            Instant.now()
        },
        /** Monotonic provider-stage timing source. */
        monotonicNanos: () -> Long = System::nanoTime,
        /**
         * The capabilities this stage may request, as the turn's current frame reports them.
         * Provider-native carriage belongs here; admission and effects remain with VexLifeCapabilityBridge.
         */
        capabilityOffer: List<VexLifeCapabilityAtlas.Definition> = emptyList(),
        model: HereticModelCatalog.Entry? = null,
    ): Result {
        val entry =
            model
                ?: requireNotNull(
                    HereticModelCatalog.entry(
                        runtime.modelRef,
                    ),
                ) {
                    "qualified runtime model is not offered by the current Product"
                }
        requireQualifiedRuntime(
            runtime = runtime,
            model = entry,
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
        val profile =
            entry.companionProfile
        val harmony =
            profile.messageProtocol ==
                HereticCompanionRepresentation.MessageProtocol.HARMONY
        val generation =
            profile.generation
        val payload =
            RequestPayload(
                model = runtime.requestModel,
                messages =
                    if (
                        profile.coalesceLeadingSystemMessages
                    ) {
                        leadingSystemAsOne(
                            messages,
                        )
                    } else {
                        messages
                    },
                max_tokens = generation.productMaxOutputTokens,
                stream = true,
                temperature = generation.sampling.temperature,
                top_p = generation.sampling.topP,
                top_k = generation.sampling.topK,
                chat_template_kwargs =
                    buildMap {
                        put("enable_thinking", JsonPrimitive(true))
                        profile.modelIdentity
                            ?.let {
                                put(
                                    "model_identity",
                                    JsonPrimitive(
                                        it,
                                    ),
                                )
                            }
                        generation.reasoningEffort
                            ?.let {
                                put(
                                    "reasoning_effort",
                                    JsonPrimitive(
                                        it.wireValue,
                                    ),
                                )
                            }
                    },
            )
        val nativeTools =
            profile.capabilityRepresentation ==
                HereticCompanionRepresentation.CapabilityRepresentation.PROVIDER_NATIVE_TOOLS
        val providerTools =
            if (nativeTools) {
                ProviderCapabilityTools.tools(
                    capabilityOffer,
                )
            } else {
                emptyList()
            }
        val providerToolSchemaSha256 =
            providerTools
                .takeIf {
                    it.isNotEmpty()
                }
                ?.let {
                    sha256(
                        VexContractJson.canonicalEncode(
                            it,
                        ),
                    )
                }
        val requestBody =
            when {
                nativeTools && harmony ->
                    harmonyRequestBody(
                        payload = payload,
                        capabilityOffer = capabilityOffer,
                    )

                nativeTools ->
                    openAiNativeRequestBody(
                        payload = payload,
                        capabilityOffer = capabilityOffer,
                    )

                else ->
                    JsonCodec.encodeToString(
                        payload,
                    )
            }
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
                    harmony = harmony,
                    nativeOpenAiTools = nativeTools && !harmony,
                    capabilityOffer = capabilityOffer,
                    monotonicNanos = monotonicNanos,
                )
            } else {
                invokeBulk(
                    transport = transport,
                    call = call,
                    nativeOpenAiTools = nativeTools && !harmony,
                    capabilityOffer = capabilityOffer,
                    monotonicNanos = monotonicNanos,
                ).let {
                    if (
                        profile.reasoningRepresentation ==
                            HereticCompanionRepresentation.ReasoningRepresentation.HARMONY_CHANNELS
                    ) {
                        harmonized(
                            completion = it,
                            capabilityOffer = capabilityOffer,
                        )
                    } else {
                        it
                    }
                }
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
                providerTools.size.toString(),
                providerToolSchemaSha256 ?: "",
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
                providerToolCount = providerTools.size,
                providerToolSchemaSha256 = providerToolSchemaSha256,
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
            providerRequestBody = requestBody,
            measurement = completion.measurement,
        )
    }

    private fun invokeBulk(
        transport: Transport,
        call: HttpCall,
        nativeOpenAiTools: Boolean = false,
        capabilityOffer: List<VexLifeCapabilityAtlas.Definition> = emptyList(),
        monotonicNanos: () -> Long,
    ): ProviderCompletion {
        val startedNanos = monotonicNanos()
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
        val finishReason =
            stringOrNull(
                choice[
                    "finish_reason"
                ],
            )
        val reasoning =
            reasoningObservation(
                message,
            )
        val answer =
            stringOrNull(
                message[
                    "content"
                ],
            )
                .orEmpty()
        val nativeControl =
            if (nativeOpenAiTools) {
                ProviderCapabilityTools.controlFromToolCalls(
                    toolCalls = message["tool_calls"],
                    offered = capabilityOffer,
                )
            } else {
                null
            }
        val providerContent =
            when {
                nativeControl != null && answer.isEmpty() -> nativeControl
                answer.isNotEmpty() -> answer
                else -> ""
            }
        val usage =
            usageOrNull(
                parsed[
                    "usage"
                ],
            )
        val measurement =
            StageMeasurement(
                stop =
                    stopFor(
                        done = true,
                        finishReason = finishReason,
                        answered = providerContent.isNotEmpty(),
                    ),
                finishReason = finishReason,
                promptTokens = usage?.promptTokens,
                cachedPromptTokens = usage?.cachedPromptTokens,
                completionTokens = usage?.completionTokens,
                firstTokenMillis = null,
                firstAnswerMillis = null,
                totalMillis = (monotonicNanos() - startedNanos) / 1_000_000,
                reasoningCharacters = reasoning.trace.characterCount,
                answerCharacters = providerContent.codePointCount(0, providerContent.length),
                controlHeaderInReasoning =
                    reasoning.sealedHumanValue?.let(::containsControlHeader) ?: false,
                controlHeaderInAnswer = containsControlHeader(providerContent),
            )
        if (
            nativeOpenAiTools &&
                finishReason ==
                    "tool_calls" &&
                nativeControl ==
                    null
        ) {
            throw ConnectionException(
                FailureCode.EndpointResponseInvalid,
                "local Companion endpoint declared tool_calls without a consumable native tool call",
                measurement = measurement,
            )
        }
        val content =
            providerContent.takeIf(
                String::isNotEmpty,
            )
                ?: throw ConnectionException(
                    FailureCode.EndpointResponseInvalid,
                    "local Companion response content/tool call is empty",
                    measurement = measurement,
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
            reasoning = reasoning,
            finishReason = finishReason,
            assistantRoleObserved =
                stringOrNull(
                    message[
                        "role"
                    ],
                ) ==
                    "assistant",
            measurement = measurement,
        )
    }

    private fun invokeStreaming(
        transport: StreamingTransport,
        call: HttpCall,
        progressObserver: ProgressObserver?,
        harmony: Boolean = false,
        nativeOpenAiTools: Boolean = false,
        capabilityOffer: List<VexLifeCapabilityAtlas.Definition> = emptyList(),
        monotonicNanos: () -> Long,
    ): ProviderCompletion {
        val startedNanos = monotonicNanos()
        var firstTokenNanos: Long? = null
        var firstAnswerNanos: Long? = null
        var usage: Usage? = null
        val content =
            StringBuilder()
        val reasoning =
            StringBuilder()
        val splitter = if (harmony) HarmonyResponseSplitter() else null
        val toolCall = HarmonyToolCall()
        var nativeToolControl: String? = null

        fun appendReasoning(delta: String) {
            if (firstTokenNanos == null) {
                firstTokenNanos = monotonicNanos()
            }
            reasoning.append(delta)
            progressObserver?.observe(
                Progress(
                    kind = ProgressKind.Reasoning,
                    delta = delta,
                    accumulatedCharacters = reasoning.codePointCount(),
                )
            )
        }

        fun appendContent(delta: String) {
            if (firstAnswerNanos == null) {
                val at = monotonicNanos()
                firstAnswerNanos = at
                if (firstTokenNanos == null) {
                    firstTokenNanos = at
                }
            }
            content.append(delta)
            progressObserver?.observe(
                Progress(
                    kind = ProgressKind.Content,
                    delta = delta,
                    accumulatedCharacters = content.codePointCount(),
                )
            )
        }

        fun route(pieces: List<HarmonyResponseSplitter.Piece>) =
            pieces.forEach {
                when (it.kind) {
                    HarmonyResponseSplitter.Kind.Thinking -> appendReasoning(it.text)
                    HarmonyResponseSplitter.Kind.Answer -> appendContent(it.text)
                    HarmonyResponseSplitter.Kind.ToolCall ->
                        if (!toolCall.accept(it)) appendReasoning(it.text)
                }
            }
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
                    splitter?.let { route(it.finish()) }
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
                usageOrNull(
                    event[
                        "usage"
                    ],
                )
                    ?.let {
                        usage = it
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
                    appendReasoning(reasoningDelta)
                }

                if (nativeOpenAiTools) {
                    ProviderCapabilityTools.controlFromToolCalls(
                        toolCalls = delta["tool_calls"],
                        offered = capabilityOffer,
                    )
                        ?.let { control ->
                            nativeToolControl = nativeToolControl ?: control
                            if (firstTokenNanos == null) {
                                firstTokenNanos = monotonicNanos()
                            }
                        }
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
                        if (splitter == null) appendContent(contentDelta)
                        else route(splitter.accept(contentDelta))
                    }
            }

        validateHttp(
            status = http.status,
            redirectLocation = http.redirectLocation,
        )

        fun millisSinceStart(nanos: Long?): Long? =
            nanos?.let {
                (it - startedNanos) /
                    1_000_000
            }

        fun measurement(
            answered: Boolean = content.isNotEmpty(),
        ): StageMeasurement =
            StageMeasurement(
                stop =
                    stopFor(
                        done = done,
                        finishReason = finishReason,
                        answered = answered,
                    ),
                finishReason = finishReason,
                promptTokens = usage?.promptTokens,
                cachedPromptTokens = usage?.cachedPromptTokens,
                completionTokens = usage?.completionTokens,
                firstTokenMillis = millisSinceStart(firstTokenNanos),
                firstAnswerMillis = millisSinceStart(firstAnswerNanos),
                totalMillis = (monotonicNanos() - startedNanos) / 1_000_000,
                reasoningCharacters = reasoning.codePointCount(),
                answerCharacters = content.codePointCount(),
                controlHeaderInReasoning = containsControlHeader(reasoning),
                controlHeaderInAnswer = containsControlHeader(content),
            )

        if (
            !done
        ) {
            throw ConnectionException(
                FailureCode.EndpointResponseInvalid,
                "streaming Companion endpoint closed before [DONE]",
                measurement = measurement(),
            )
        }
        if (
            nativeOpenAiTools &&
                finishReason ==
                    "tool_calls" &&
                nativeToolControl ==
                    null
        ) {
            throw ConnectionException(
                FailureCode.EndpointResponseInvalid,
                "streaming Companion endpoint declared tool_calls without a consumable native tool call",
                measurement = measurement(answered = false),
            )
        }
        toolCall.recipient?.let {
            recipient ->
            if (
                content.isEmpty()
            ) {
                content.append(
                    HarmonyCapabilityTools.control(
                        recipient = recipient,
                        arguments = toolCall.arguments(),
                        offered = capabilityOffer,
                    ),
                )
            } else {
                appendReasoning(
                    "\n$recipient ${toolCall.arguments()}",
                )
            }
        }
        nativeToolControl?.let { control ->
            if (content.isEmpty()) {
                content.append(control)
            } else {
                appendReasoning("\n$control")
            }
        }
        if (
            content.isEmpty()
        ) {
            throw ConnectionException(
                FailureCode.EndpointResponseInvalid,
                "streaming Companion response content is empty",
                measurement = measurement(answered = false),
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
        val stageMeasurement =
            measurement(
                answered = true,
            )

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
            measurement = stageMeasurement,
        )
    }

    /**
     * The harmony chat template keeps only the first system message (as its developer message)
     * and silently drops later ones; the leading system messages are sent as one so none of
     * what Vex is given is lost.
     */
    private class HarmonyToolCall {
        var recipient: String? = null
            private set
        private val text = StringBuilder()
        fun accept(
            piece: HarmonyResponseSplitter.Piece,
        ): Boolean {
            val to = piece.recipient ?: return false
            if (recipient != null && recipient != to) return false
            recipient = to
            text.append(piece.text)
            return true
        }
        fun arguments(): String = text.toString().trim()
    }

    private fun openAiNativeRequestBody(
        payload: RequestPayload,
        capabilityOffer: List<VexLifeCapabilityAtlas.Definition>,
    ): String {
        val fields =
            JsonCodec.encodeToJsonElement(payload).jsonObject.toMutableMap()
        fields["messages"] =
            JsonCodec.encodeToJsonElement(
                ProviderCapabilityTools.messages(payload.messages),
            )
        ProviderCapabilityTools
            .tools(capabilityOffer)
            .takeIf { it.isNotEmpty() }
            ?.let {
                fields["tools"] = JsonCodec.encodeToJsonElement(it)
            }
        return JsonCodec.encodeToString(JsonObject(fields))
    }

    private fun harmonyRequestBody(
        payload: RequestPayload,
        capabilityOffer: List<VexLifeCapabilityAtlas.Definition>,
    ): String {
        val fields =
            JsonCodec.encodeToJsonElement(payload).jsonObject.toMutableMap()
        fields["messages"] =
            JsonCodec.encodeToJsonElement(
                HarmonyCapabilityTools.messages(payload.messages),
            )
        HarmonyCapabilityTools
            .tools(capabilityOffer)
            .takeIf { it.isNotEmpty() }
            ?.let {
                fields["tools"] = JsonCodec.encodeToJsonElement(it)
            }
        return JsonCodec.encodeToString(JsonObject(fields))
    }

    private fun leadingSystemAsOne(
        messages: List<CompanionProviderMessage>,
    ): List<CompanionProviderMessage> {
        val leading = messages.takeWhile { it.role == "system" }
        if (leading.size <= 1) return messages
        return listOf(
            leading.first().copy(content = leading.joinToString("\n\n") { it.content })
        ) + messages.drop(leading.size)
    }

    /** A whole (non-streamed) harmony reply, split the same way as a streamed one. */
    private fun harmonized(
        completion: ProviderCompletion,
        capabilityOffer: List<VexLifeCapabilityAtlas.Definition>,
    ): ProviderCompletion {
        val splitter = HarmonyResponseSplitter()
        val pieces = splitter.accept(completion.content) + splitter.finish()
        val toolCall = HarmonyToolCall()
        val heldAsThinking =
            pieces.filter {
                it.kind == HarmonyResponseSplitter.Kind.ToolCall &&
                    !toolCall.accept(it)
            }
        val spoken =
            pieces
                .filter { it.kind == HarmonyResponseSplitter.Kind.Answer }
                .joinToString("") { it.text }
        val answer =
            toolCall.recipient
                ?.takeIf { spoken.isEmpty() }
                ?.let {
                    HarmonyCapabilityTools.control(
                        recipient = it,
                        arguments = toolCall.arguments(),
                        offered = capabilityOffer,
                    )
                }
                ?: spoken
        val thinking =
            (
                pieces.filter { it.kind == HarmonyResponseSplitter.Kind.Thinking } +
                    heldAsThinking
            )
                .joinToString("") { it.text } +
                toolCall.recipient
                    ?.takeIf { spoken.isNotEmpty() }
                    ?.let { "\n$it ${toolCall.arguments()}" }
                    .orEmpty()
        if (thinking.isEmpty() && answer == completion.content) return completion
        if (answer.isEmpty()) {
            fail(
                FailureCode.EndpointResponseInvalid,
                "harmony response carried no final answer",
            )
        }
        return completion.copy(
            content = answer,
            reasoning =
                reasoningObservation(
                    JsonObject(
                        buildMap {
                            put("content", JsonPrimitive(answer))
                            if (thinking.isNotEmpty()) put("reasoning", JsonPrimitive(thinking))
                        }
                    )
                ),
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
        model: HereticModelCatalog.Entry,
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
        val realization =
            requireNotNull(
                HereticRuntimeRealization.resolveByRef(
                    model = model,
                    realizationRef = runtime.realizationRef,
                ),
            ) {
                "qualified runtime realization is not offered for this Heretic model"
            }
        require(
            runtime.modelRef == model.modelRef &&
                runtime.realizationRef == realization.realizationRef &&
                runtime.modelProfileRef == realization.modelProfileRef &&
                runtime.artifactRef == realization.artifact.artifactRef &&
                runtime.artifactContentSetSha256 == realization.artifact.contentSetSha256 &&
                runtime.runtimeAdapterRef == realization.runtime.runtimeAdapterRef &&
                runtime.runtimeClass == realization.runtime.runtimeClass &&
                runtime.endpoint == realization.runtime.endpoint &&
                runtime.requestModel == realization.runtime.requestModel,
        ) {
            "qualified runtime evidence no longer matches its exact Heretic runtime realization"
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
