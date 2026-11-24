package vexlife.companion

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import vexlife.model.VexLifeInferenceSessionAdmission
import vexlife.model.VexLifeModelSourceProjection
import vexlife.partner.AnthropicMessagesWire
import vexlife.partner.ExternalProviderWire
import vexlife.partner.KimiChatWire
import vexlife.partner.OpenAiResponsesWire
import vexlife.partner.PartnerCapabilityExchange
import vexlife.partner.VexLifePartnerAccountScope
import vexlife.partner.VexLifePartnerCredentialCustody
import vexlife.partner.VexLifePartnerHostEffect
import vexlife.partner.VexLifePartnerModelProfileCatalog
import vexlife.partner.VexLifePartnerNetworkPlan
import vexlife.partner.VexLifePartnerProviderCatalog

/**
 * External Partner realization of [CompanionInferenceConnection].
 *
 * The adapter composes already-owned source:
 *
 * provider/profile catalog -> pure wire codec -> no-secret network plan -> protected host effect
 * -> normalized provider events.
 *
 * It owns no credential value, model selection, Conversation, Memory, capability effect, retry,
 * canonical response commit, or durable provider conversation.
 */
internal class PartnerInference(
    private val qualified: VexLifeInferenceSessionAdmission.Qualified,
    private val credential: VexLifePartnerCredentialCustody.Coordinate,
    private val accountScope: VexLifePartnerAccountScope.Selection? = null,
    private val executor: VexLifePartnerHostEffect.Executor,
    private val now: () -> Instant = {
        Instant.now()
    },
    private val monotonicNanos: () -> Long = System::nanoTime,
) : CompanionInferenceConnection {
    class PartnerInferenceException(
        val transportFailure: VexLifePartnerHostEffect.Result.TransportFailure? = null,
        val providerFailure: ExternalProviderWire.Failure? = null,
        message: String,
    ) : IllegalStateException(
        message,
    ) {
        init {
            require(
                (transportFailure == null) !=
                    (providerFailure == null),
            ) {
                "Partner inference failure requires exactly one transport/provider classification"
            }
        }
    }

    private val coordinate =
        qualified.coordinate
            .also {
                require(
                    it.sourceKind ==
                        VexLifeModelSourceProjection.SourceKind.PARTNER,
                ) {
                    "Partner inference requires one qualified Partner serving coordinate"
                }
            }

    private val providerRef =
        requireNotNull(
            coordinate.providerRef,
        ) {
            "Partner inference requires providerRef"
        }

    private val provider =
        requireNotNull(
            VexLifePartnerProviderCatalog.resolve(
                providerRef,
            ),
        ) {
            "Partner inference requires one source-owned provider"
        }

    private val profile =
        requireNotNull(
            VexLifePartnerModelProfileCatalog.resolve(
                providerRef = providerRef,
                modelRef = coordinate.modelRef,
            ),
        ) {
            "Partner inference requires one source-owned model profile"
        }
            .also {
                require(
                    it.profileRef ==
                        qualified.profileRef &&
                        credential.providerRef ==
                        providerRef &&
                        accountScope
                            ?.providerRef ==
                            providerRef,
                ) {
                    "Partner inference qualification/profile/credential/account scope do not bind one provider"
                }
            }

    private val wireProvider =
        when (
            provider.dialect
        ) {
            VexLifePartnerProviderCatalog.Dialect.OPENAI_RESPONSES ->
                ExternalProviderWire.Provider.OPENAI_RESPONSES

            VexLifePartnerProviderCatalog.Dialect.ANTHROPIC_MESSAGES ->
                ExternalProviderWire.Provider.ANTHROPIC_MESSAGES

            VexLifePartnerProviderCatalog.Dialect.OPENAI_CHAT_COMPLETIONS -> {
                require(
                    provider.providerRef ==
                        VexLifePartnerProviderCatalog.KimiInternationalRef,
                ) {
                    "initial OpenAI-chat Partner realization is source-owned Kimi only"
                }
                ExternalProviderWire.Provider.KIMI_CHAT_COMPLETIONS
            }
        }

    override fun invoke(
        request: CompanionInferenceConnection.Request,
    ): CompanionInferenceConnection.Result {
        request.cancellation
            ?.checkpoint()

        val tools =
            if (
                profile.providerNativeToolsRepresented
            ) {
                ProviderCapabilityTools.tools(
                    request.capabilityOffer,
                )
            } else {
                emptyList()
            }
        val prepared =
            ExternalProviderWire.PreparedRequest(
                model = coordinate.modelRef,
                messages = request.messages,
                maxOutputTokens =
                    VexLifeCompanionAttentionBudget
                        .forRuntime(
                            runtimeContextWindowTokens =
                                qualified.contextWindowTokens,
                        )
                        .reservedOutputTokens,
                tools = tools,
            )
        val requestBody =
            encode(
                prepared,
            )
        val plan =
            VexLifePartnerNetworkPlan.form(
                providerRef = providerRef,
                credential = credential,
                requestBody = requestBody,
                accountScope = accountScope,
            )

        val startedNanos =
            monotonicNanos()
        val hostResult =
            executor.execute(
                plan,
            )
        val finishedNanos =
            monotonicNanos()
        request.cancellation
            ?.checkpoint()

        val totalMillis =
            (
                finishedNanos -
                    startedNanos
            )
                .coerceAtLeast(
                    0,
                ) /
                1_000_000

        val http =
            when (
                hostResult
            ) {
                is VexLifePartnerHostEffect.Result.TransportFailure ->
                    throw PartnerInferenceException(
                        transportFailure = hostResult,
                        message =
                            "Partner inference transport failed: " +
                                hostResult.code.name,
                    )

                is VexLifePartnerHostEffect.Result.Http ->
                    hostResult
            }

        if (
            http.status !in
            200..299
        ) {
            throw PartnerInferenceException(
                providerFailure =
                    classifyFailure(
                        status = http.status,
                        body = http.body,
                    ),
                message =
                    "Partner provider returned HTTP " +
                        http.status,
            )
        }

        val events =
            decode(
                http.body,
            )
        events
            .filterIsInstance<ExternalProviderWire.Event.Failed>()
            .firstOrNull()
            ?.let {
                throw PartnerInferenceException(
                    providerFailure = it.failure,
                    message =
                        "Partner provider response failed normalization: " +
                            it.failure.meaning.name,
                )
            }

        val completed =
            events
                .filterIsInstance<ExternalProviderWire.Event.Completed>()
                .lastOrNull()
                ?: throw PartnerInferenceException(
                    providerFailure =
                        invalidResponse(
                            "Partner provider response has no terminal completion",
                        ),
                    message =
                        "Partner provider response has no terminal completion",
                )
        val toolCalls =
            events
                .filterIsInstance<ExternalProviderWire.Event.ToolCallReady>()
                .map {
                    it.call
                }
        if (
            toolCalls.size >
            1
        ) {
            throw PartnerInferenceException(
                providerFailure =
                    invalidResponse(
                        "Partner provider returned more than one tool call for one serial Vex stage",
                    ),
                message =
                    "Partner provider returned parallel/multiple tool calls",
            )
        }

        val textDeltas =
            events
                .filterIsInstance<ExternalProviderWire.Event.TextDelta>()
                .map {
                    it.text
                }
        val text =
            textDeltas.joinToString(
                separator = "",
            )

        val pending =
            toolCalls
                .singleOrNull()
                ?.let {
                    call ->
                    if (
                        text.isNotBlank()
                    ) {
                        throw PartnerInferenceException(
                            providerFailure =
                                invalidResponse(
                                    "Partner provider mixed human text with a Vex capability call",
                                ),
                            message =
                                "Partner provider mixed answer text and capability control",
                        )
                    }
                    PartnerCapabilityExchange.normalize(
                        provider = wireProvider,
                        call = call,
                        offered = request.capabilityOffer,
                    )
                }

        val content =
            pending
                ?.canonicalControl
                ?: text
                    .takeIf {
                        it.isNotBlank()
                    }
                ?: throw PartnerInferenceException(
                    providerFailure =
                        invalidResponse(
                            "Partner provider completed without answer text or one capability call",
                        ),
                    message =
                        "Partner provider completed without semantic output",
                )

        var accumulated =
            0
        if (
            pending ==
            null
        ) {
            textDeltas.forEach {
                delta ->
                accumulated +=
                    delta.codePointCount(
                        0,
                        delta.length,
                    )
                request.progressObserver
                    ?.observe(
                        DirectCompanionConnection.Progress(
                            kind =
                                DirectCompanionConnection.ProgressKind.Content,
                            delta = delta,
                            accumulatedCharacters = accumulated,
                        ),
                    )
            }
        }

        val measurement =
            DirectCompanionConnection.StageMeasurement(
                stop =
                    DirectCompanionConnection.StageStop.COMPLETED,
                finishReason = completed.finishReason,
                promptTokens = null,
                cachedPromptTokens = null,
                completionTokens = null,
                firstTokenMillis = null,
                firstAnswerMillis = null,
                totalMillis = totalMillis,
                reasoningCharacters = 0,
                answerCharacters =
                    if (
                        pending ==
                        null
                    ) {
                        content.codePointCount(
                            0,
                            content.length,
                        )
                    } else {
                        0
                    },
                controlHeaderInReasoning = false,
                controlHeaderInAnswer =
                    pending !=
                        null,
            )

        val observedAt =
            now()
                .toString()
        val responseBodySha256 =
            requireNotNull(
                http.receipt.responseBodySha256,
            )
        val contentSha256 =
            sha256(
                content,
            )
        val observationFingerprint =
            fingerprint(
                "PARTNER_OBSERVATION",
                providerRef,
                qualified.qualificationRef,
                provider.origin,
                coordinate.modelRef,
                qualified.profileRef,
                observedAt,
                responseBodySha256,
                contentSha256,
                content.length.toString(),
                completed.finishReason.orEmpty(),
            )
        val observationRef =
            "evidence.vexlife-prod.partner-observation." +
                observationFingerprint.take(
                    32,
                )
        val invocationFingerprint =
            fingerprint(
                "PARTNER_INVOCATION",
                providerRef,
                qualified.qualificationRef,
                provider.origin,
                plan.requestBodySha256,
                responseBodySha256,
                observationRef,
                observationFingerprint,
                http.status.toString(),
                http.providerRequestId.orEmpty(),
                pending
                    ?.providerCallRef
                    .orEmpty(),
                observedAt,
            )

        return CompanionInferenceConnection.Result(
            content = content,
            model = coordinate.modelRef,
            sealedHumanReasoning = null,
            observation =
                CompanionInferenceConnection.Observation(
                    sourceKind =
                        VexLifeModelSourceProjection.SourceKind.PARTNER,
                    providerRef = providerRef,
                    qualificationRef = qualified.qualificationRef,
                    endpointOrigin = provider.origin,
                    modelRef = coordinate.modelRef,
                    profileRef = qualified.profileRef,
                    observedAt = observedAt,
                    responseBodySha256 = responseBodySha256,
                    contentSha256 = contentSha256,
                    contentCharacters =
                        content.codePointCount(
                            0,
                            content.length,
                        ),
                    finishReasonOrNull = completed.finishReason,
                    rawProviderResponsePersisted = false,
                    rawReasoningPersisted = false,
                    observationRef = observationRef,
                    semanticFingerprint = observationFingerprint,
                ),
            invocationEvidence =
                CompanionInferenceConnection.InvocationEvidence(
                    sourceKind =
                        VexLifeModelSourceProjection.SourceKind.PARTNER,
                    providerRef = providerRef,
                    qualificationRef = qualified.qualificationRef,
                    endpointOrigin = provider.origin,
                    requestBodySha256 = plan.requestBodySha256,
                    responseBodySha256 = responseBodySha256,
                    observationRef = observationRef,
                    observationFingerprint = observationFingerprint,
                    httpStatus = http.status,
                    actualHttpCall = http.receipt.actualNetworkEffect,
                    loopbackOnly = false,
                    providerRequestId = http.providerRequestId,
                    providerCallRef =
                        pending
                            ?.providerCallRef,
                    formedAt = observedAt,
                    invocationRef =
                        "evidence.vexlife-prod.partner-invocation." +
                            invocationFingerprint.take(
                                32,
                            ),
                    semanticFingerprint = invocationFingerprint,
                ),
            providerRequestBody = requestBody,
            measurement = measurement,
            pendingPartnerCapability = pending,
        )
    }

    private fun encode(
        request: ExternalProviderWire.PreparedRequest,
    ): String =
        when (
            wireProvider
        ) {
            ExternalProviderWire.Provider.OPENAI_RESPONSES ->
                OpenAiResponsesWire.encodeRequest(
                    request,
                )

            ExternalProviderWire.Provider.ANTHROPIC_MESSAGES ->
                AnthropicMessagesWire.encodeRequest(
                    request,
                )

            ExternalProviderWire.Provider.KIMI_CHAT_COMPLETIONS ->
                KimiChatWire.encodeRequest(
                    request,
                )
        }

    private fun decode(
        body: String,
    ): List<ExternalProviderWire.Event> {
        val stream =
            body
                .lineSequence()
                .any {
                    it.startsWith(
                        "data:",
                    ) ||
                        it.startsWith(
                            "event:",
                        )
                }

        return when (
            wireProvider
        ) {
            ExternalProviderWire.Provider.OPENAI_RESPONSES ->
                if (
                    stream
                ) {
                    OpenAiResponsesWire.decodeStream(
                        body,
                    )
                } else {
                    OpenAiResponsesWire.decodeResponseBody(
                        body,
                    )
                }

            ExternalProviderWire.Provider.ANTHROPIC_MESSAGES ->
                if (
                    stream
                ) {
                    AnthropicMessagesWire.decodeStream(
                        body,
                    )
                } else {
                    AnthropicMessagesWire.decodeResponseBody(
                        body,
                    )
                }

            ExternalProviderWire.Provider.KIMI_CHAT_COMPLETIONS ->
                if (
                    stream
                ) {
                    KimiChatWire.decodeStream(
                        body,
                    )
                } else {
                    KimiChatWire.decodeResponseBody(
                        body,
                    )
                }
        }
    }

    private fun classifyFailure(
        status: Int,
        body: String,
    ): ExternalProviderWire.Failure =
        when (
            wireProvider
        ) {
            ExternalProviderWire.Provider.OPENAI_RESPONSES ->
                OpenAiResponsesWire.classifyFailure(
                    httpStatus = status,
                    body = body,
                )

            ExternalProviderWire.Provider.ANTHROPIC_MESSAGES ->
                AnthropicMessagesWire.classifyFailure(
                    httpStatus = status,
                    body = body,
                )

            ExternalProviderWire.Provider.KIMI_CHAT_COMPLETIONS ->
                KimiChatWire.classifyFailure(
                    httpStatus = status,
                    body = body,
                )
        }

    private fun invalidResponse(
        message: String,
    ): ExternalProviderWire.Failure =
        ExternalProviderWire.Failure(
            provider = wireProvider,
            meaning =
                ExternalProviderWire.FailureMeaning.RESPONSE_INVALID,
            retry =
                ExternalProviderWire.RetryDisposition.UNKNOWN,
            httpStatus = null,
            providerType = null,
            providerCode = null,
            message = message,
        )

    private fun fingerprint(
        vararg values: String,
    ): String =
        sha256(
            values.joinToString(
                separator = "\u001f",
            ),
        )

    private fun sha256(
        value: String,
    ): String =
        MessageDigest.getInstance(
            "SHA-256",
        )
            .digest(
                value.toByteArray(
                    StandardCharsets.UTF_8,
                ),
            )
            .joinToString(
                separator = "",
            ) {
                "%02x".format(
                    it.toInt() and
                        0xff,
                )
            }
}

// [VXG RealForever]
