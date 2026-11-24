package vexlife.partner

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import vexlife.companion.CompanionProviderMessage

/**
 * One bounded, process-current live qualification attempt for an exact Partner provider/model.
 *
 * This owner performs no model selection, serving promotion, retry loop, Conversation/Memory
 * mutation, durable qualification persistence, or capability effect. It consumes the already-owned
 * configured credential coordinate and deterministic support standing, performs at most one exact
 * protected-host inference request, and returns current evidence.
 *
 * A successful receipt is bound to the exact credential/account scope that earned it. Restart does
 * not restore this receipt from disk.
 */
object VexLifePartnerLiveQualification {
    private const val ProbePrompt =
        "Reply with a short acknowledgement."

    private const val ProbeMaxOutputTokens =
        8

    data class Receipt(
        val providerRef: String,
        val modelRef: String,
        val profileRef: String,
        val credentialRef: String,
        val accountScope: VexLifePartnerAccountScope.Selection,
        val requestBodySha256: String,
        val responseBodySha256: String,
        val providerRequestId: String?,
        val observedAt: String,
        val receiptRef: String,
    ) {
        init {
            requireRef(providerRef, "providerRef")
            requireRef(modelRef, "modelRef")
            requireRef(profileRef, "profileRef")
            requireRef(credentialRef, "credentialRef")
            requireSha256(requestBodySha256, "requestBodySha256")
            requireSha256(responseBodySha256, "responseBodySha256")
            providerRequestId?.let {
                require(
                    it.length <= 512 &&
                        '\n' !in it &&
                        '\r' !in it,
                ) {
                    "live Partner qualification provider request ID must remain bounded"
                }
            }
            require(accountScope.providerRef == providerRef) {
                "live Partner qualification account scope must bind the same provider"
            }
            require(observedAt.isNotBlank()) {
                "live Partner qualification requires one observation time"
            }
            requireRef(receiptRef, "receiptRef")
        }

        fun supportLiveQualification(): VexLifePartnerSupportQualification.LiveQualification =
            VexLifePartnerSupportQualification.LiveQualification(
                providerRef = providerRef,
                modelRef = modelRef,
                credentialRef = credentialRef,
                accountScope = accountScope,
                receiptRef = receiptRef,
            )
    }

    sealed interface Outcome {
        data class Qualified(
            val receipt: Receipt,
            val connection: VexLifePartnerConnectionProjection.Projection,
            val support: VexLifePartnerSupportQualification.Projection,
        ) : Outcome {
            init {
                require(
                    connection.providerRef == receipt.providerRef &&
                        connection.modelRef == receipt.modelRef &&
                        connection.standing ==
                        VexLifePartnerConnectionProjection.Standing.QUALIFIED_SERVING &&
                        support.providerRef == receipt.providerRef &&
                        support.modelRef == receipt.modelRef &&
                        support.standing ==
                        VexLifePartnerSupportQualification.Standing.LIVE_QUALIFIED &&
                        support.liveQualificationReceiptRef == receipt.receiptRef &&
                        support.liveQualificationCredentialRef == receipt.credentialRef &&
                        support.liveQualificationAccountScope == receipt.accountScope,
                ) {
                    "qualified Partner outcome must bind one exact live provider/model/credential relationship"
                }
            }
        }

        data class Held(
            val connection: VexLifePartnerConnectionProjection.Projection,
            val support: VexLifePartnerSupportQualification.Projection,
            val transportFailure: VexLifePartnerHostEffect.Result.TransportFailure? = null,
            val providerFailure: ExternalProviderWire.Failure? = null,
        ) : Outcome {
            init {
                require((transportFailure == null) != (providerFailure == null)) {
                    "held Partner live qualification requires exactly one transport/provider failure"
                }
                require(
                    support.standing !=
                        VexLifePartnerSupportQualification.Standing.LIVE_QUALIFIED,
                ) {
                    "failed Partner live qualification may not mint live support standing"
                }
            }
        }
    }

    fun qualify(
        profile: VexLifePartnerModelProfileCatalog.Definition,
        configuredProvider: VexLifePartnerConfiguredCurrentness.ProviderState,
        connection: VexLifePartnerConnectionProjection.Projection,
        support: VexLifePartnerSupportQualification.Projection,
        executor: VexLifePartnerHostEffect.Executor,
        now: () -> Instant = {
            Instant.now()
        },
    ): Outcome {
        require(
            configuredProvider.providerRef == profile.providerRef &&
                configuredProvider.coordinateStanding ==
                VexLifePartnerConfiguredCurrentness.CoordinateStanding.PRESENT &&
                configuredProvider.credentialRef != null &&
                configuredProvider.accountScope != null,
        ) {
            "live Partner qualification requires one exact current configured credential coordinate"
        }
        require(
            connection.providerRef == profile.providerRef &&
                connection.modelRef == profile.modelRef,
        ) {
            "live Partner qualification connection does not bind the selected provider/model"
        }
        require(
            support.providerRef == profile.providerRef &&
                support.modelRef == profile.modelRef &&
                support.standing ==
                VexLifePartnerSupportQualification.Standing.CONTRACT_PROVEN_LIVE_UNQUALIFIED,
        ) {
            "live Partner qualification requires deterministic support proof before provider effect"
        }

        val credentialRef =
            requireNotNull(configuredProvider.credentialRef)
        val accountScope =
            requireNotNull(configuredProvider.accountScope)
        val credential =
            VexLifePartnerCredentialCustody.Coordinate(
                providerRef = profile.providerRef,
                credentialRef = credentialRef,
            )
        val wireProvider =
            wireProvider(profile)
        val requestBody =
            encode(
                provider = wireProvider,
                request =
                    ExternalProviderWire.PreparedRequest(
                        model = profile.modelRef,
                        messages =
                            listOf(
                                CompanionProviderMessage(
                                    role = "user",
                                    content = ProbePrompt,
                                ),
                            ),
                        maxOutputTokens = ProbeMaxOutputTokens,
                        tools = emptyList(),
                    ),
            )
        val plan =
            VexLifePartnerNetworkPlan.form(
                providerRef = profile.providerRef,
                credential = credential,
                requestBody = requestBody,
                accountScope = accountScope,
            )
        val hostResult =
            VexLifePartnerHostEffect.requireExact(
                plan = plan,
                result = executor.execute(plan),
            )

        if (hostResult is VexLifePartnerHostEffect.Result.TransportFailure) {
            return Outcome.Held(
                connection =
                    transportFailureProjection(
                        connection = connection,
                        failure = hostResult,
                    ),
                support = support,
                transportFailure = hostResult,
            )
        }

        val http =
            hostResult as VexLifePartnerHostEffect.Result.Http
        if (http.status !in 200..299) {
            val failure =
                classifyFailure(
                    provider = wireProvider,
                    status = http.status,
                    body = http.body,
                )
            return heldForProviderFailure(
                connection = connection,
                support = support,
                failure = failure,
            )
        }

        val events =
            decode(
                provider = wireProvider,
                body = http.body,
            )
        events
            .filterIsInstance<ExternalProviderWire.Event.Failed>()
            .firstOrNull()
            ?.let {
                return heldForProviderFailure(
                    connection = connection,
                    support = support,
                    failure = it.failure,
                )
            }

        if (
            events.none {
                it is ExternalProviderWire.Event.Completed
            } ||
            events.any {
                it is ExternalProviderWire.Event.ToolCallReady
            }
        ) {
            return heldForProviderFailure(
                connection = connection,
                support = support,
                failure =
                    invalidResponse(
                        provider = wireProvider,
                        message =
                            "live Partner qualification requires one terminal non-tool provider response",
                    ),
            )
        }

        val observedAt =
            now().toString()
        val responseBodySha256 =
            requireNotNull(http.receipt.responseBodySha256)
        val receiptHash =
            fingerprint(
                profile.providerRef,
                profile.modelRef,
                profile.profileRef,
                credentialRef,
                accountScope.providerRef,
                accountScope.workspaceId.orEmpty(),
                plan.requestBodySha256,
                responseBodySha256,
                http.status.toString(),
                http.providerRequestId.orEmpty(),
                observedAt,
            )
        val receipt =
            Receipt(
                providerRef = profile.providerRef,
                modelRef = profile.modelRef,
                profileRef = profile.profileRef,
                credentialRef = credentialRef,
                accountScope = accountScope,
                requestBodySha256 = plan.requestBodySha256,
                responseBodySha256 = responseBodySha256,
                providerRequestId = http.providerRequestId,
                observedAt = observedAt,
                receiptRef =
                    "receipt.vexlife.partner.live." +
                        receiptHash.take(32),
            )
        val qualifiedConnection =
            VexLifePartnerConnectionProjection.project(
                VexLifePartnerConnectionProjection.Observation(
                    providerRef = profile.providerRef,
                    modelRef = profile.modelRef,
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
                ),
            )
        val qualifiedSupport =
            VexLifePartnerSupportQualification.acceptLiveQualification(
                current = support,
                live = receipt.supportLiveQualification(),
            )

        return Outcome.Qualified(
            receipt = receipt,
            connection = qualifiedConnection,
            support = qualifiedSupport,
        )
    }

    private fun heldForProviderFailure(
        connection: VexLifePartnerConnectionProjection.Projection,
        support: VexLifePartnerSupportQualification.Projection,
        failure: ExternalProviderWire.Failure,
    ): Outcome.Held =
        Outcome.Held(
            connection =
                VexLifePartnerFailureProjection.apply(
                    observation = observation(connection),
                    failure = failure,
                ),
            support = support,
            providerFailure = failure,
        )

    private fun observation(
        connection: VexLifePartnerConnectionProjection.Projection,
    ): VexLifePartnerConnectionProjection.Observation =
        VexLifePartnerConnectionProjection.Observation(
            providerRef = connection.providerRef,
            modelRef = connection.modelRef,
            credential = connection.credential,
            account = connection.account,
            service = connection.service,
            modelAccess = connection.modelAccess,
            invocation = connection.invocation,
        )

    private fun transportFailureProjection(
        connection: VexLifePartnerConnectionProjection.Projection,
        failure: VexLifePartnerHostEffect.Result.TransportFailure,
    ): VexLifePartnerConnectionProjection.Projection {
        val current =
            observation(connection)
        val next =
            when (failure.code) {
                VexLifePartnerHostEffect.FailureCode.CREDENTIAL_NOT_PRESENT ->
                    current.copy(
                        credential =
                            VexLifePartnerConnectionProjection.CredentialStanding.MISSING,
                        account =
                            VexLifePartnerConnectionProjection.AccountStanding.UNKNOWN,
                        service =
                            VexLifePartnerConnectionProjection.ServiceStanding.UNKNOWN,
                        modelAccess =
                            VexLifePartnerConnectionProjection.ModelAccessStanding.UNKNOWN,
                        invocation =
                            VexLifePartnerConnectionProjection.InvocationStanding.LAST_INVOCATION_FAILED,
                    )

                VexLifePartnerHostEffect.FailureCode.CREDENTIAL_PROVIDER_MISMATCH ->
                    current.copy(
                        service =
                            VexLifePartnerConnectionProjection.ServiceStanding.UNKNOWN,
                        invocation =
                            VexLifePartnerConnectionProjection.InvocationStanding.LAST_INVOCATION_FAILED,
                    )

                else ->
                    current.copy(
                        service =
                            VexLifePartnerConnectionProjection.ServiceStanding.UNKNOWN,
                        invocation =
                            VexLifePartnerConnectionProjection.InvocationStanding.LAST_INVOCATION_FAILED,
                    )
            }

        return VexLifePartnerConnectionProjection.project(next)
    }

    private fun wireProvider(
        profile: VexLifePartnerModelProfileCatalog.Definition,
    ): ExternalProviderWire.Provider =
        when (profile.dialect) {
            VexLifePartnerProviderCatalog.Dialect.OPENAI_RESPONSES ->
                ExternalProviderWire.Provider.OPENAI_RESPONSES

            VexLifePartnerProviderCatalog.Dialect.ANTHROPIC_MESSAGES ->
                ExternalProviderWire.Provider.ANTHROPIC_MESSAGES

            VexLifePartnerProviderCatalog.Dialect.OPENAI_CHAT_COMPLETIONS -> {
                require(
                    profile.providerRef ==
                        VexLifePartnerProviderCatalog.KimiInternationalRef,
                ) {
                    "initial OpenAI-chat live qualification is source-owned Kimi only"
                }
                ExternalProviderWire.Provider.KIMI_CHAT_COMPLETIONS
            }
        }

    private fun encode(
        provider: ExternalProviderWire.Provider,
        request: ExternalProviderWire.PreparedRequest,
    ): String =
        when (provider) {
            ExternalProviderWire.Provider.OPENAI_RESPONSES ->
                OpenAiResponsesWire.encodeRequest(request)

            ExternalProviderWire.Provider.ANTHROPIC_MESSAGES ->
                AnthropicMessagesWire.encodeRequest(request)

            ExternalProviderWire.Provider.KIMI_CHAT_COMPLETIONS ->
                KimiChatWire.encodeRequest(request)
        }

    private fun decode(
        provider: ExternalProviderWire.Provider,
        body: String,
    ): List<ExternalProviderWire.Event> {
        val stream =
            body
                .lineSequence()
                .any {
                    it.startsWith("data:") ||
                        it.startsWith("event:")
                }

        return when (provider) {
            ExternalProviderWire.Provider.OPENAI_RESPONSES ->
                if (stream) {
                    OpenAiResponsesWire.decodeStream(body)
                } else {
                    OpenAiResponsesWire.decodeResponseBody(body)
                }

            ExternalProviderWire.Provider.ANTHROPIC_MESSAGES ->
                if (stream) {
                    AnthropicMessagesWire.decodeStream(body)
                } else {
                    AnthropicMessagesWire.decodeResponseBody(body)
                }

            ExternalProviderWire.Provider.KIMI_CHAT_COMPLETIONS ->
                if (stream) {
                    KimiChatWire.decodeStream(body)
                } else {
                    KimiChatWire.decodeResponseBody(body)
                }
        }
    }

    private fun classifyFailure(
        provider: ExternalProviderWire.Provider,
        status: Int,
        body: String,
    ): ExternalProviderWire.Failure =
        when (provider) {
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
        provider: ExternalProviderWire.Provider,
        message: String,
    ): ExternalProviderWire.Failure =
        ExternalProviderWire.Failure(
            provider = provider,
            meaning = ExternalProviderWire.FailureMeaning.RESPONSE_INVALID,
            retry = ExternalProviderWire.RetryDisposition.UNKNOWN,
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
        MessageDigest
            .getInstance("SHA-256")
            .digest(
                value.toByteArray(StandardCharsets.UTF_8),
            )
            .joinToString("") {
                "%02x".format(
                    it.toInt() and 0xff,
                )
            }

    private fun requireRef(
        value: String,
        field: String,
    ) {
        require(
            value.isNotBlank() &&
                value.length <= 512 &&
                value.none(Char::isWhitespace),
        ) {
            "$field must be one bounded non-blank reference"
        }
    }

    private fun requireSha256(
        value: String,
        field: String,
    ) {
        require(
            value.matches(
                Regex("[0-9a-f]{64}"),
            ),
        ) {
            "$field must be one SHA-256 digest"
        }
    }
}

// [VXG RealForever]
