package vexlife.companion

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlinx.serialization.Serializable
import vexlife.contract.VexContractJson
import vexlife.conversation.DirectConversationStore

/**
 * Conditional, read-only return to exact canonical Conversation source.
 *
 * The local model may request older source instead of answering the human. This owner parses that
 * narrow request, performs deterministic local discovery over the current canonical Conversation,
 * re-opens selected matches through DirectConversationStore exact-return authority, and forms one
 * bounded provider context for a later final inference.
 *
 * It owns no Conversation mutation, Memory admission, semantic acceptance, provider invocation,
 * filesystem-general search, web search, embedding index, or human answer.
 */
object DirectConversationSourceReturn {
    const val RequestHeader =
        "VEXLIFE_CONVERSATION_SOURCE_RETURN_REQUEST/v1"

    const val ProtocolHeader =
        "VEXLIFE_CONVERSATION_SOURCE_RETURN_PROTOCOL/v1"

    const val ContextHeader =
        "VEXLIFE_CONVERSATION_SOURCE_RETURN/v1"

    const val RequestSchema =
        "vexlife-prod.conversation-source-return-request/v1"

    const val ContextSchema =
        "vexlife-prod.conversation-source-return-context/v1"

    const val AttemptTraceSchema =
        "vexlife-prod.conversation-source-return-attempt/v1"

    const val TraceSchema =
        "vexlife-prod.conversation-source-return-trace/v1"

    const val AnySpeaker =
        "ANY"

    const val HumanSpeaker =
        "HUMAN"

    const val VexSpeaker =
        "VEX"

    const val Returned =
        "RETURNED"

    const val NoMatch =
        "NO_MATCH"

    const val HeldTokenBudget =
        "HELD_TOKEN_BUDGET"

    private const val MaximumQueryTerms =
        6

    private const val MaximumQueryTermCharacters =
        160

    private const val MaximumReturnedTurns =
        3

    @Serializable
    data class Request(
        val schemaVersion: String,
        val queryTerms: List<String>,
        val speakerScope: String,
    )

    data class Match internal constructor(
        val requestEvent: DirectConversationStore.Event,
        val responseEvent: DirectConversationStore.Event,
        val matchedTerms: List<String>,
        val matchedTermCharacters: Int,
    )

    data class Discovery internal constructor(
        val request: Request,
        val matches: List<Match>,
        val scannedCompletedTurnCount: Int,
        val wholeHistoryEventEnumerationPerformed: Boolean,
        val effectsPerformed: Boolean,
    )

    @Serializable
    private data class ProviderEvent(
        val eventRef: String,
        val eventHash: String,
        val sequence: Long,
        val eventKind: String,
        val speakerRef: String,
        val recipientRefs: List<String>,
        val content: String,
    )

    @Serializable
    private data class ProviderMatch(
        val matchedTerms: List<String>,
        val request: ProviderEvent,
        val response: ProviderEvent,
    )

    @Serializable
    private data class ProviderPacket(
        val schemaVersion: String,
        val truthClass: String,
        val state: String,
        val queryTerms: List<String>,
        val speakerScope: String,
        val matches: List<ProviderMatch>,
        val scannedCompletedTurnCount: Int,
        val wholeHistoryEventEnumerationPerformed: Boolean,
        val effectAuthorityGranted: Boolean,
        val effectsPerformed: Boolean,
    )

    data class Projection internal constructor(
        val state: String,
        val availableTokens: Int,
        val discoveredMatchCount: Int,
        val returnedMatchCount: Int,
        val estimatedSystemMessageTokens: Int,
        val systemMessageSha256: String,
        val systemContent: String,
        val sourceEventRefs: List<String>,
        val effectAuthorityGranted: Boolean,
        val effectsPerformed: Boolean,
    )

    @Serializable
    @ConsistentCopyVisibility
    data class AttemptTrace internal constructor(
        val schemaVersion: String,
        val truthClass: String,
        val initialPromptReceiptRef: String,
        val initialPromptReceiptFingerprint: String,
        val request: Request,
        val modelOutputSha256: String,
        val runtimeObservationRef: String,
        val runtimeObservationFingerprint: String,
        val invocationRef: String,
        val invocationFingerprint: String,
        val sourceReturnState: String,
        val returnedSourceEventRefs: List<String>,
        val sourceReturnSystemMessageSha256: String,
        val rawProviderOutputPersisted: Boolean,
        val rawReasoningPersisted: Boolean,
        val effectAuthorityGranted: Boolean,
        val effectsPerformed: Boolean,
    )

    @Serializable
    @ConsistentCopyVisibility
    data class Trace internal constructor(
        val schemaVersion: String,
        val truthClass: String,
        val attempt: AttemptTrace,
        val finalPromptReceiptRef: String,
        val finalPromptReceiptFingerprint: String,
        val rawProviderOutputPersisted: Boolean,
        val rawReasoningPersisted: Boolean,
        val effectAuthorityGranted: Boolean,
        val effectsPerformed: Boolean,
    )

    fun protocolMessage(): CompanionProviderMessage =
        CompanionProviderMessage(
            role = "system",
            content =
                listOf(
                    ProtocolHeader,
                    "If exact older Conversation source that is not already present is necessary to answer the human faithfully, do not guess and do not answer yet.",
                    "Instead return exactly one source-return request using the required header followed by one JSON object.",
                    "Request JSON shape: {\"schemaVersion\":\"$RequestSchema\",\"queryTerms\":[\"distinctive exact phrase or term\"],\"speakerScope\":\"ANY|HUMAN|VEX\"}",
                    "Use 1-$MaximumQueryTerms concise distinctive query terms. Product will search only this canonical local Conversation.",
                    "If current context is sufficient, answer the human normally and do not emit the request header.",
                    "Only one source-return request is available in this human turn. A returned-source follow-up must answer from available source or preserve uncertainty; it must not request source again.",
                )
                    .joinToString(
                        "\n",
                    ),
        )

    fun parseRequestOrNull(
        modelContent: String,
    ): Request? {
        if (
            !modelContent.startsWith(
                RequestHeader,
            )
        ) {
            return null
        }

        val separator =
            modelContent.indexOf(
                '\n',
            )
        require(
            separator ==
                RequestHeader.length &&
                modelContent.substring(
                    0,
                    separator,
                ) ==
                RequestHeader,
        ) {
            "Conversation source-return request header is not exact"
        }
        val json =
            modelContent.substring(
                separator +
                    1,
            )
        require(
            json.isNotBlank() &&
                json ==
                json.trim(),
        ) {
            "Conversation source-return request requires exactly one JSON payload without surrounding prose"
        }

        return requireRequest(
            VexContractJson.decode<Request>(
                json,
            ),
        )
    }

    fun discover(
        conversationOwner: DirectConversationStore.Locked,
        request: Request,
        excludedEventRefs: Set<String> =
            emptySet(),
    ): Discovery {
        val admitted =
            requireRequest(
                request,
            )
        val snapshot =
            conversationOwner.current()
        val events =
            snapshot.canonicalEvents
        require(
            events.size %
                2 ==
                0,
        ) {
            "canonical Conversation source-return scan requires complete request/response pairs"
        }

        val normalizedTerms =
            admitted.queryTerms.map(
                ::normalize,
            )
        val ranked =
            buildList {
                var index =
                    0
                while (
                    index <
                        events.size
                ) {
                    val requestEvent =
                        events[index]
                    val responseEvent =
                        events[index + 1]
                    requireCompletedPair(
                        requestEvent = requestEvent,
                        responseEvent = responseEvent,
                    )

                    if (
                        requestEvent.eventRef !in
                            excludedEventRefs &&
                        responseEvent.eventRef !in
                            excludedEventRefs
                    ) {
                        val searchable =
                            when (
                                admitted.speakerScope
                            ) {
                                HumanSpeaker ->
                                    normalize(
                                        requestEvent.content,
                                    )

                                VexSpeaker ->
                                    normalize(
                                        responseEvent.content,
                                    )

                                else ->
                                    normalize(
                                        requestEvent.content +
                                            "\n" +
                                            responseEvent.content,
                                    )
                            }
                        val matched =
                            normalizedTerms.filter {
                                searchable.contains(
                                    it,
                                )
                            }

                        if (
                            matched.isNotEmpty()
                        ) {
                            add(
                                Match(
                                    requestEvent = requestEvent,
                                    responseEvent = responseEvent,
                                    matchedTerms =
                                        admitted.queryTerms.filterIndexed {
                                            termIndex,
                                            _,
                                            ->
                                            matched.contains(
                                                normalizedTerms[
                                                    termIndex
                                                ],
                                            )
                                        },
                                    matchedTermCharacters =
                                        matched.sumOf(
                                            String::length,
                                        ),
                                ),
                            )
                        }
                    }

                    index +=
                        2
                }
            }
                .sortedWith(
                    compareByDescending<Match> {
                        it.matchedTerms.size
                    }
                        .thenByDescending {
                            it.matchedTermCharacters
                        }
                        .thenByDescending {
                            it.responseEvent.sequence
                        },
                )
                .take(
                    MaximumReturnedTurns,
                )

        val matches =
            if (
                ranked.isEmpty()
            ) {
                emptyList()
            } else {
                val addresses =
                    ranked
                        .flatMap {
                            match ->
                            listOf(
                                address(
                                    match.requestEvent,
                                ),
                                address(
                                    match.responseEvent,
                                ),
                            )
                        }
                        .sortedBy(
                            DirectConversationStore.EventAddress::sequence,
                        )
                val exact =
                    conversationOwner.exactCommittedConversationEvents(
                        addresses,
                    )
                require(
                    !exact.effectsPerformed
                ) {
                    "Conversation source-return exact source re-open performed an effect"
                }
                val exactByRef =
                    exact.events.associateBy(
                        DirectConversationStore.Event::eventRef,
                    )
                ranked.map {
                    match ->
                    match.copy(
                        requestEvent =
                            requireNotNull(
                                exactByRef[
                                    match.requestEvent.eventRef
                                ],
                            ),
                        responseEvent =
                            requireNotNull(
                                exactByRef[
                                    match.responseEvent.eventRef
                                ],
                            ),
                    )
                }
                    .also {
                        exactMatches ->
                        require(
                            exactMatches.all {
                                exactMatch ->
                                exactMatch.requestEvent ==
                                    ranked.single {
                                        it.requestEvent.eventRef ==
                                            exactMatch.requestEvent.eventRef
                                    }
                                        .requestEvent &&
                                    exactMatch.responseEvent ==
                                    ranked.single {
                                        it.responseEvent.eventRef ==
                                            exactMatch.responseEvent.eventRef
                                    }
                                        .responseEvent
                            },
                        ) {
                            "Conversation source-return ranked source changed during exact canonical re-open"
                        }
                    }
            }

        return Discovery(
            request = admitted,
            matches = matches,
            scannedCompletedTurnCount =
                events.size /
                    2,
            wholeHistoryEventEnumerationPerformed = true,
            effectsPerformed = false,
        )
    }

    fun project(
        discovery: Discovery,
        availableTokens: Int,
    ): Projection {
        require(
            availableTokens in
                0..DirectPromptContext.InputTokenBudget,
        ) {
            "Conversation source-return projection allowance is outside the admitted prompt budget"
        }
        require(
            !discovery.effectsPerformed &&
                discovery.wholeHistoryEventEnumerationPerformed &&
                discovery.scannedCompletedTurnCount >=
                discovery.matches.size,
        ) {
            "Conversation source-return discovery is not one effect-free canonical scan"
        }

        if (
            discovery.matches.isEmpty()
        ) {
            val content =
                providerContent(
                    discovery = discovery,
                    state = NoMatch,
                    matches = emptyList(),
                )
            val estimate =
                DirectPromptContext.estimatedMessageTokens(
                    content,
                )
            require(
                estimate <=
                    availableTokens,
            ) {
                "Conversation source-return no-match context does not fit the supplied prompt allowance"
            }
            return Projection(
                state = NoMatch,
                availableTokens = availableTokens,
                discoveredMatchCount = 0,
                returnedMatchCount = 0,
                estimatedSystemMessageTokens = estimate,
                systemMessageSha256 =
                    sha256(
                        content,
                    ),
                systemContent = content,
                sourceEventRefs = emptyList(),
                effectAuthorityGranted = false,
                effectsPerformed = false,
            )
        }

        val selected =
            mutableListOf<Match>()
        for (
            match in
            discovery.matches
        ) {
            val candidate =
                selected +
                    match
            val content =
                providerContent(
                    discovery = discovery,
                    state = Returned,
                    matches = candidate,
                )
            val estimate =
                DirectPromptContext.estimatedMessageTokens(
                    content,
                )
            if (
                estimate >
                    availableTokens
            ) {
                break
            }
            selected +=
                match
        }

        if (
            selected.isEmpty()
        ) {
            val content =
                providerContent(
                    discovery = discovery,
                    state = HeldTokenBudget,
                    matches = emptyList(),
                )
            val estimate =
                DirectPromptContext.estimatedMessageTokens(
                    content,
                )
            require(
                estimate <=
                    availableTokens,
            ) {
                "Conversation source-return held context does not fit the supplied prompt allowance"
            }
            return Projection(
                state = HeldTokenBudget,
                availableTokens = availableTokens,
                discoveredMatchCount = discovery.matches.size,
                returnedMatchCount = 0,
                estimatedSystemMessageTokens = estimate,
                systemMessageSha256 =
                    sha256(
                        content,
                    ),
                systemContent = content,
                sourceEventRefs = emptyList(),
                effectAuthorityGranted = false,
                effectsPerformed = false,
            )
        }

        val content =
            providerContent(
                discovery = discovery,
                state = Returned,
                matches = selected,
            )
        val estimate =
            DirectPromptContext.estimatedMessageTokens(
                content,
            )
        return Projection(
            state = Returned,
            availableTokens = availableTokens,
            discoveredMatchCount = discovery.matches.size,
            returnedMatchCount = selected.size,
            estimatedSystemMessageTokens = estimate,
            systemContent = content,
            sourceEventRefs =
                selected
                    .flatMap {
                        listOf(
                            it.requestEvent.eventRef,
                            it.responseEvent.eventRef,
                        )
                    },
            effectAuthorityGranted = false,
            effectsPerformed = false,
        )
    }

    fun formAttemptTrace(
        initialPrompt: DirectPromptContext.Receipt,
        inference: DirectCompanionConnection.Result,
        discovery: Discovery,
        projection: Projection,
    ): AttemptTrace {
        val request =
            requireNotNull(
                parseRequestOrNull(
                    inference.content,
                ),
            ) {
                "Conversation source-return attempt requires one exact model source-return request"
            }
        require(
            request ==
                discovery.request &&
                projection.state in
                setOf(
                    Returned,
                    NoMatch,
                    HeldTokenBudget,
                ) &&
                projection.discoveredMatchCount ==
                discovery.matches.size &&
                projection.returnedMatchCount <=
                projection.discoveredMatchCount &&
                !projection.effectAuthorityGranted &&
                !projection.effectsPerformed &&
                inference.runtimeObservation.contentSha256 ==
                sha256(
                    inference.content,
                ) &&
                inference.invocationEvidence.runtimeObservationRef ==
                inference.runtimeObservation.observationRef &&
                inference.invocationEvidence.runtimeObservationFingerprint ==
                inference.runtimeObservation.semanticFingerprint &&
                inference.invocationEvidence.responseBodySha256 ==
                inference.runtimeObservation.responseBodySha256 &&
                inference.invocationEvidence.actualHttpCall &&
                inference.invocationEvidence.loopbackOnly &&
                !inference.runtimeObservation.rawProviderResponsePersisted &&
                !inference.runtimeObservation.reasoningTrace.rawPersisted,
        ) {
            "Conversation source-return attempt evidence is not bound to one exact internal inference and source return"
        }

        return AttemptTrace(
            schemaVersion = AttemptTraceSchema,
            truthClass =
                "CONDITIONAL_EXACT_CONVERSATION_SOURCE_RETURN_ATTEMPT",
            initialPromptReceiptRef =
                initialPrompt.receiptRef,
            initialPromptReceiptFingerprint =
                initialPrompt.semanticFingerprint,
            request = request,
            modelOutputSha256 =
                inference.runtimeObservation.contentSha256,
            runtimeObservationRef =
                inference.runtimeObservation.observationRef,
            runtimeObservationFingerprint =
                inference.runtimeObservation.semanticFingerprint,
            invocationRef =
                inference.invocationEvidence.invocationRef,
            invocationFingerprint =
                inference.invocationEvidence.semanticFingerprint,
            sourceReturnState = projection.state,
            returnedSourceEventRefs =
                projection.sourceEventRefs,
            sourceReturnSystemMessageSha256 =
                projection.systemMessageSha256,
            rawProviderOutputPersisted = false,
            rawReasoningPersisted = false,
            effectAuthorityGranted = false,
            effectsPerformed = false,
        )
    }

    fun formTrace(
        attempt: AttemptTrace,
        finalPrompt: DirectPromptContext.Receipt,
    ): Trace {
        require(
            attempt.schemaVersion ==
                AttemptTraceSchema &&
                attempt.truthClass ==
                "CONDITIONAL_EXACT_CONVERSATION_SOURCE_RETURN_ATTEMPT" &&
                attempt.initialPromptReceiptRef.isNotBlank() &&
                attempt.initialPromptReceiptFingerprint.matches(
                    Regex(
                        "[0-9a-f]{64}",
                    ),
                ) &&
                attempt.modelOutputSha256.matches(
                    Regex(
                        "[0-9a-f]{64}",
                    ),
                ) &&
                attempt.runtimeObservationRef.isNotBlank() &&
                attempt.runtimeObservationFingerprint.matches(
                    Regex(
                        "[0-9a-f]{64}",
                    ),
                ) &&
                attempt.invocationRef.isNotBlank() &&
                attempt.invocationFingerprint.matches(
                    Regex(
                        "[0-9a-f]{64}",
                    ),
                ) &&
                attempt.sourceReturnState in
                setOf(
                    Returned,
                    NoMatch,
                    HeldTokenBudget,
                ) &&
                attempt.sourceReturnSystemMessageSha256.matches(
                    Regex(
                        "[0-9a-f]{64}",
                    ),
                ) &&
                !attempt.rawProviderOutputPersisted &&
                !attempt.rawReasoningPersisted &&
                !attempt.effectAuthorityGranted &&
                !attempt.effectsPerformed,
        ) {
            "Conversation source-return attempt trace is invalid"
        }

        return Trace(
            schemaVersion = TraceSchema,
            truthClass =
                "CONDITIONAL_EXACT_CONVERSATION_SOURCE_RETURN_TRACE",
            attempt = attempt,
            finalPromptReceiptRef =
                finalPrompt.receiptRef,
            finalPromptReceiptFingerprint =
                finalPrompt.semanticFingerprint,
            rawProviderOutputPersisted = false,
            rawReasoningPersisted = false,
            effectAuthorityGranted = false,
            effectsPerformed = false,
        )
    }

    private fun providerContent(
        discovery: Discovery,
        state: String,
        matches: List<Match>,
    ): String {
        val packet =
            ProviderPacket(
                schemaVersion = ContextSchema,
                truthClass =
                    "EXACT_CANONICAL_CONVERSATION_SOURCE_RETURN",
                state = state,
                queryTerms = discovery.request.queryTerms,
                speakerScope = discovery.request.speakerScope,
                matches =
                    matches.map {
                        match ->
                        ProviderMatch(
                            matchedTerms =
                                match.matchedTerms,
                            request =
                                providerEvent(
                                    match.requestEvent,
                                ),
                            response =
                                providerEvent(
                                    match.responseEvent,
                                ),
                        )
                    },
                scannedCompletedTurnCount =
                    discovery.scannedCompletedTurnCount,
                wholeHistoryEventEnumerationPerformed =
                    discovery.wholeHistoryEventEnumerationPerformed,
                effectAuthorityGranted = false,
                effectsPerformed = false,
            )
        return listOf(
            ContextHeader,
            "This is exact canonical local Conversation source returned because Vex explicitly requested older source. It is not Memory, semantic acceptance, or a profile of the human.",
            "Answer the current human from the returned exact source and other current context. If the return is NO_MATCH, HELD_TOKEN_BUDGET, ambiguous, or insufficient, preserve that uncertainty instead of inventing recall.",
            "Do not emit another source-return request in this human turn.",
            VexContractJson.compactEncode(
                packet,
            ),
        )
            .joinToString(
                "\n",
            )
    }

    private fun providerEvent(
        event: DirectConversationStore.Event,
    ): ProviderEvent =
        ProviderEvent(
            eventRef = event.eventRef,
            eventHash = event.eventHash,
            sequence = event.sequence,
            eventKind = event.eventKind,
            speakerRef = event.speakerRef,
            recipientRefs = event.recipientRefs,
            content = event.content,
        )

    private fun address(
        event: DirectConversationStore.Event,
    ): DirectConversationStore.EventAddress =
        DirectConversationStore.EventAddress(
            sequence = event.sequence,
            eventRef = event.eventRef,
            eventHash = event.eventHash,
        )

    private fun requireRequest(
        value: Request,
    ): Request {
        require(
            value.schemaVersion ==
                RequestSchema &&
                value.speakerScope in
                setOf(
                    AnySpeaker,
                    HumanSpeaker,
                    VexSpeaker,
                ) &&
                value.queryTerms.isNotEmpty() &&
                value.queryTerms.size <=
                MaximumQueryTerms,
        ) {
            "Conversation source-return request identity or bounds are invalid"
        }
        val normalized =
            value.queryTerms.map {
                term ->
                require(
                    term.isNotBlank() &&
                        term.length <=
                        MaximumQueryTermCharacters &&
                        !term.contains(
                            '\n',
                        ),
                ) {
                    "Conversation source-return query term is invalid"
                }
                normalize(
                    term,
                )
            }
        require(
            normalized
                .distinct()
                .size ==
                normalized.size,
        ) {
            "Conversation source-return query terms must remain unique after normalization"
        }
        return value.copy(
            queryTerms =
                value.queryTerms.map(
                    String::trim,
                ),
        )
    }

    private fun requireCompletedPair(
        requestEvent: DirectConversationStore.Event,
        responseEvent: DirectConversationStore.Event,
    ) {
        require(
            requestEvent.eventKind ==
                "REQUEST" &&
                responseEvent.eventKind ==
                "RESPONSE" &&
                responseEvent.priorEventHash ==
                requestEvent.eventHash &&
                responseEvent.sequence ==
                requestEvent.sequence +
                    1 &&
                responseEvent.turnRef ==
                requestEvent.turnRef &&
                responseEvent.companionLineageRef ==
                requestEvent.companionLineageRef &&
                responseEvent.threadRef ==
                requestEvent.threadRef &&
                responseEvent.channelRef ==
                requestEvent.channelRef,
        ) {
            "Conversation source-return scan encountered a non-canonical completed pair"
        }
    }

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

    private fun normalize(
        value: String,
    ): String =
        value
            .trim()
            .lowercase()
            .replace(
                Regex(
                    "\\s+",
                ),
                " ",
            )
}

// [VXG RealForever]
