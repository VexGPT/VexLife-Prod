package vexlife.companion

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
 * The Connection consumes already-qualified Heretic runtime truth. It owns only the numeric-loopback
 * HTTP effect plus normalization of the returned provider observation/invocation evidence.
 *
 * It does not own Home, Conversation persistence, prompt-context selection, Memory, runtime
 * construction/lifetime, model identity, or presentation.
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
        val stream: Boolean = false,
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

    fun interface Transport {
        fun post(
            call: HttpCall,
        ): HttpResult
    }

    object SystemTransport : Transport {
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
                    .POST(
                        HttpRequest.BodyPublishers.ofString(
                            call.body,
                            StandardCharsets.UTF_8,
                        ),
                    )
                    .build()

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
                redirectLocation =
                    response
                        .headers()
                        .firstValue(
                            "location",
                        )
                        .orElse(
                            null,
                        ),
            )
        }
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
        humanReasoningRequested: Boolean = false,
        transport: Transport = SystemTransport,
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
                    stream = false,
                    chat_template_kwargs =
                        mapOf(
                            "enable_thinking" to
                                humanReasoningRequested,
                        ),
                ),
            )
        val requestBodySha256 =
            sha256(
                requestBody,
            )

        val http =
            try {
                transport.post(
                    HttpCall(
                        uri = uri,
                        body = requestBody,
                        timeoutMs = RequestTimeoutMs,
                    ),
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

        if (
            http.status in
            300..399
        ) {
            fail(
                FailureCode.EndpointNotLoopback,
                "local Companion endpoint redirects are not admitted",
            )
        }

        if (
            http.status !in
            200..299
        ) {
            fail(
                FailureCode.EndpointHttpError,
                "local Companion endpoint returned HTTP ${http.status}",
            )
        }

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

        val observedAt =
            now()
                .toString()
        val responseBodySha256 =
            sha256(
                http.body,
            )
        val reportedModel =
            stringOrNull(
                parsed[
                    "model"
                ],
            )
        val compatibilityModel =
            safeCompatibilityModel(
                reportedModel,
                runtime.requestModel,
            )
        val reportedModelField =
            reportedModelField(
                reportedModel,
            )
        val reasoning =
            reasoningObservation(
                message,
            )
        val finishReason =
            stringOrNull(
                choice[
                    "finish_reason"
                ],
            )
        val assistantRoleObserved =
            stringOrNull(
                message[
                    "role"
                ],
            ) ==
                "assistant"

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
                http.status.toString(),
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
                httpStatus = http.status,
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
        MessageDigest
            .getInstance(
                "SHA-256",
            )
            .digest(
                value.toByteArray(
                    StandardCharsets.UTF_8,
                ),
            )
            .joinToString(
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
