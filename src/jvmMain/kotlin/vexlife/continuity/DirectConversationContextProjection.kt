package vexlife.continuity

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlinx.serialization.Serializable
import vexlife.companion.DirectConversationSourceReturn
import vexlife.companion.DirectPromptContext
import vexlife.contract.VexContractJson
import vexlife.conversation.DirectConversationStore

/**
 * Request-bound preparation for one backward-looking human request.
 *
 * This owner does not decide semantic meaning, mutate Conversation/Memory, or answer the human.
 * It recognizes one bounded class of explicit backward-reference cues, derives inspectable lexical
 * terms from the exact current request, and reuses [DirectConversationSourceReturn] to reopen exact
 * canonical Conversation before provider inference.
 *
 * Current Product activation remains narrow: explicit backward-reference language after Working
 * Continuity rollover. Broader associative/context semantics remain held by the EvolveContext lane.
 */
object DirectConversationContextProjection {
    const val CueClass = "REQUEST_BOUND_CONVERSATION_CONTEXT_CUE"

    const val ContextHeader =
        "VEXLIFE_CONVERSATION_CONTEXT_PROJECTION/v1"

    const val ContextSchema =
        "vexlife-prod.conversation-context-projection/v1"

    const val Returned =
        "RETURNED"

    const val HeldTokenBudget =
        "HELD_TOKEN_BUDGET"

    enum class Cue {
        EXPLICIT_BACK_REFERENCE,
        PRIOR_OCCURRENCE,
        EXACT_RECALL,
        POINTS_BACK,
    }

    enum class SourcePreparation {
        CANONICAL_SCAN,
        LOCATOR_CURRENT,
        LOCATOR_REBUILT,
    }

    data class SourceLifecycle(
        val bindingRef: String =
            VexLifeContinuitySourceLocator.UnboundSourceLifecycleBindingRef,
        val blockedEventRefs: Set<String> =
            emptySet(),
        val historicalCurrentizedEventRefs: Set<String> =
            emptySet(),
    )

    data class Prepared(
        val cue: Cue,
        val queryTerms: List<String>,
        val speakerScope: String,
        val discovery: DirectConversationSourceReturn.Discovery,
        val sourcePreparation: SourcePreparation,
        val exactEventFileReadCount: Int,
        val historicalCurrentizedEventRefs: Set<String> =
            emptySet(),
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
        val cue: String,
        val queryTerms: List<String>,
        val speakerScope: String,
        val state: String,
        val matches: List<ProviderMatch>,
        val historicalCurrentizedEventRefs: List<String>,
        val scannedCompletedTurnCount: Int,
        val wholeHistoryEventEnumerationPerformed: Boolean,
        val effectAuthorityGranted: Boolean,
        val effectsPerformed: Boolean,
    )

    data class Projection internal constructor(
        val cue: String,
        val queryTerms: List<String>,
        val speakerScope: String,
        val state: String,
        val availableTokens: Int,
        val discoveredMatchCount: Int,
        val returnedMatchCount: Int,
        val estimatedSystemMessageTokens: Int,
        val systemMessageSha256: String,
        val systemContent: String,
        val sourceEventRefs: List<String>,
        val historicalCurrentizedEventRefs: List<String>,
        val effectAuthorityGranted: Boolean,
        val effectsPerformed: Boolean,
    )

    fun prepare(
        conversationOwner: DirectConversationStore.Locked,
        currentRequest: DirectConversationStore.Event,
        excludedEventRefs: Set<String> = emptySet(),
        locatorBackend: VexLifeContinuitySourceLocator.Backend? = null,
        sourceLifecycle: () -> SourceLifecycle = {
            SourceLifecycle()
        },
    ): Prepared? {
        require(currentRequest.eventKind == "REQUEST") {
            "direct Conversation context projection requires the exact current human request"
        }

        val cue = cueOrNull(currentRequest.content) ?: return null
        val queryTerms = queryTerms(currentRequest.content)
        if (queryTerms.isEmpty()) {
            return null
        }
        val speakerScope = speakerScope(currentRequest.content)
        val currentSourceLifecycle =
            sourceLifecycle()
        val effectiveExcludedEventRefs =
            excludedEventRefs +
                currentSourceLifecycle.blockedEventRefs
        val request =
            DirectConversationSourceReturn.Request(
                schemaVersion = DirectConversationSourceReturn.RequestSchema,
                queryTerms = queryTerms,
                speakerScope = speakerScope,
            )

        if (
            locatorBackend !=
                null
        ) {
            var rebuilt =
                false
            var located =
                runCatching {
                        VexLifeContinuitySourceLocator.locate(
                            conversationOwner = conversationOwner,
                            queryTerms = queryTerms,
                            speakerScope =
                                locatorSpeakerScope(
                                    speakerScope,
                                ),
                            backend = locatorBackend,
                            excludedEventRefs =
                                effectiveExcludedEventRefs,
                            sourceLifecycleBindingRef =
                                currentSourceLifecycle.bindingRef,
                        )
                    }
                    .getOrNull()

            if (
                located is
                    VexLifeContinuitySourceLocator.LocateOutcome.FallbackRequired &&
                located.reason in
                    RebuildableLocatorFallbacks
            ) {
                rebuilt =
                    runCatching {
                            VexLifeContinuitySourceLocator.rebuild(
                                conversationOwner = conversationOwner,
                                backend = locatorBackend,
                                sourceLifecycleBindingRef =
                                    currentSourceLifecycle.bindingRef,
                                excludedEventRefs =
                                    effectiveExcludedEventRefs,
                            )
                        }
                        .isSuccess
                if (
                    rebuilt
                ) {
                    located =
                        runCatching {
                                VexLifeContinuitySourceLocator.locate(
                                    conversationOwner = conversationOwner,
                                    queryTerms = queryTerms,
                                    speakerScope =
                                        locatorSpeakerScope(
                                            speakerScope,
                                        ),
                                    backend = locatorBackend,
                                    excludedEventRefs =
                                effectiveExcludedEventRefs,
                            sourceLifecycleBindingRef =
                                currentSourceLifecycle.bindingRef,
                                )
                            }
                            .getOrNull()
                }
            }

            when (
                located
            ) {
                is VexLifeContinuitySourceLocator.LocateOutcome.Found ->
                    return preparedFromLocator(
                        cue = cue,
                        request = request,
                        located = located,
                        sourcePreparation =
                            if (
                                rebuilt
                            ) {
                                SourcePreparation.LOCATOR_REBUILT
                            } else {
                                SourcePreparation.LOCATOR_CURRENT
                            },
                        historicalCurrentizedEventRefs =
                            currentSourceLifecycle.historicalCurrentizedEventRefs,
                    )

                is VexLifeContinuitySourceLocator.LocateOutcome.CurrentMiss ->
                    return null

                else ->
                    Unit
            }
        }

        val discovery =
            DirectConversationSourceReturn.discover(
                conversationOwner = conversationOwner,
                request = request,
                excludedEventRefs =
                    effectiveExcludedEventRefs,
            )

        if (discovery.matches.isEmpty()) {
            return null
        }

        return Prepared(
            cue = cue,
            queryTerms = queryTerms,
            speakerScope = speakerScope,
            discovery = discovery,
            sourcePreparation =
                SourcePreparation.CANONICAL_SCAN,
            exactEventFileReadCount = 0,
            historicalCurrentizedEventRefs =
                currentSourceLifecycle.historicalCurrentizedEventRefs,
        )
    }

    private fun preparedFromLocator(
        cue: Cue,
        request: DirectConversationSourceReturn.Request,
        located: VexLifeContinuitySourceLocator.LocateOutcome.Found,
        sourcePreparation: SourcePreparation,
        historicalCurrentizedEventRefs: Set<String>,
    ): Prepared {
        val matches =
            located.candidates.map {
                candidate ->
                DirectConversationSourceReturn.Match(
                    requestEvent = candidate.requestEvent,
                    responseEvent = candidate.responseEvent,
                    matchedTerms = candidate.matchedTerms,
                    matchedTermCharacters =
                        candidate.matchedTerms.sumOf(
                            String::length,
                        ),
                )
            }
        require(
            matches.isNotEmpty()
        ) {
            "locator found outcome must carry at least one exact canonical Conversation match"
        }
        return Prepared(
            cue = cue,
            queryTerms = request.queryTerms,
            speakerScope = request.speakerScope,
            discovery =
                DirectConversationSourceReturn.Discovery(
                    request = request,
                    matches = matches,
                    scannedCompletedTurnCount = 0,
                    wholeHistoryEventEnumerationPerformed = false,
                    effectsPerformed = false,
                ),
            sourcePreparation = sourcePreparation,
            exactEventFileReadCount =
                located.exactEventFileReadCount,
            historicalCurrentizedEventRefs =
                historicalCurrentizedEventRefs,
        )
    }

    private fun locatorSpeakerScope(
        speakerScope: String,
    ): VexLifeContinuitySourceLocator.SpeakerScope =
        when (
            speakerScope
        ) {
            DirectConversationSourceReturn.HumanSpeaker ->
                VexLifeContinuitySourceLocator.SpeakerScope.HUMAN

            DirectConversationSourceReturn.VexSpeaker ->
                VexLifeContinuitySourceLocator.SpeakerScope.VEX

            else ->
                VexLifeContinuitySourceLocator.SpeakerScope.ANY
        }

    private val RebuildableLocatorFallbacks =
        setOf(
            VexLifeContinuitySourceLocator.FallbackReason.ABSENT,
            VexLifeContinuitySourceLocator.FallbackReason.STALE,
            VexLifeContinuitySourceLocator.FallbackReason.CORRUPT,
            VexLifeContinuitySourceLocator.FallbackReason.FINGERPRINT_BINDING_MISMATCH,
            VexLifeContinuitySourceLocator.FallbackReason.SOURCE_LIFECYCLE_BINDING_MISMATCH,
            VexLifeContinuitySourceLocator.FallbackReason.SOURCE_COORDINATE_MISMATCH,
        )

    fun project(
        prepared: Prepared,
        availableTokens: Int,
    ): Projection {
        require(
            availableTokens in
                0..DirectPromptContext.InputTokenBudget,
        ) {
            "direct Conversation context projection projection allowance is outside the admitted prompt budget"
        }
        val discovery =
            prepared.discovery
        val sourcePreparationCurrent =
            when (
                prepared.sourcePreparation
            ) {
                SourcePreparation.CANONICAL_SCAN ->
                    discovery.wholeHistoryEventEnumerationPerformed &&
                        discovery.scannedCompletedTurnCount >=
                        discovery.matches.size &&
                        prepared.exactEventFileReadCount ==
                        0

                SourcePreparation.LOCATOR_CURRENT,
                SourcePreparation.LOCATOR_REBUILT,
                ->
                    !discovery.wholeHistoryEventEnumerationPerformed &&
                        discovery.scannedCompletedTurnCount ==
                        0 &&
                        prepared.exactEventFileReadCount >=
                        discovery.matches.size *
                        2
            }
        require(
            !discovery.effectsPerformed &&
                sourcePreparationCurrent &&
                prepared.queryTerms ==
                discovery.request.queryTerms &&
                prepared.speakerScope ==
                discovery.request.speakerScope,
        ) {
            "direct Conversation context projection preparation is not bound to one effect-free canonical source preparation"
        }

        val selected =
            mutableListOf<DirectConversationSourceReturn.Match>()
        for (
            match in
            discovery.matches
        ) {
            val candidate =
                selected +
                    match
            val content =
                providerContent(
                    prepared = prepared,
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
                    prepared = prepared,
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
                "direct Conversation context projection held context does not fit the supplied prompt allowance"
            }
            return Projection(
                cue = prepared.cue.name,
                queryTerms = prepared.queryTerms,
                speakerScope = prepared.speakerScope,
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
                historicalCurrentizedEventRefs =
                    emptyList(),
                effectAuthorityGranted = false,
                effectsPerformed = false,
            )
        }

        val content =
            providerContent(
                prepared = prepared,
                state = Returned,
                matches = selected,
            )
        val estimate =
            DirectPromptContext.estimatedMessageTokens(
                content,
            )
        val sourceEventRefs =
            selected.flatMap {
                listOf(
                    it.requestEvent.eventRef,
                    it.responseEvent.eventRef,
                )
            }
        val historicalCurrentizedEventRefs =
            sourceEventRefs.filter {
                it in
                    prepared.historicalCurrentizedEventRefs
            }

        return Projection(
            cue = prepared.cue.name,
            queryTerms = prepared.queryTerms,
            speakerScope = prepared.speakerScope,
            state = Returned,
            availableTokens = availableTokens,
            discoveredMatchCount = discovery.matches.size,
            returnedMatchCount = selected.size,
            estimatedSystemMessageTokens = estimate,
            systemMessageSha256 =
                sha256(
                    content,
                ),
            systemContent = content,
            sourceEventRefs =
                sourceEventRefs,
            historicalCurrentizedEventRefs =
                historicalCurrentizedEventRefs,
            effectAuthorityGranted = false,
            effectsPerformed = false,
        )
    }

    internal fun cueOrNull(content: String): Cue? {
        val normalized = normalizeSentence(content)

        if (
            ExactRecallPhrases.any(normalized::contains)
        ) {
            return Cue.EXACT_RECALL
        }
        if (
            PriorOccurrencePhrases.any(normalized::contains)
        ) {
            return Cue.PRIOR_OCCURRENCE
        }
        if (
            BackReferencePhrases.any(normalized::contains)
        ) {
            return Cue.EXPLICIT_BACK_REFERENCE
        }
        if (
            VexLifeTurnNavigation.pointsBack(content)
        ) {
            return Cue.POINTS_BACK
        }
        return null
    }

    /**
     * Lexical preparation only. Longer contiguous content phrases are preferred, then useful
     * single terms. This is intentionally not a meaning index.
     */
    internal fun queryTerms(content: String): List<String> {
        val normalized = normalizeSentence(content)
        val tokens =
            Token.findAll(normalized)
                .map { it.value }
                .toList()
        if (tokens.isEmpty()) {
            return emptyList()
        }

        val candidates = mutableListOf<String>()

        QuotedPhrase.findAll(content).forEach {
            val phrase =
                normalizeSentence(
                    it.groupValues[1],
                )
            if (
                phrase.length >= MinimumTermCharacters &&
                phrase.length <= MaximumTermCharacters
            ) {
                candidates += phrase
            }
        }

        val contentTokens =
            tokens.filter(::isContentToken)

        contentTokens
            .filter {
                it.contains('-') ||
                    it.any(Char::isDigit)
            }
            .sortedByDescending(String::length)
            .forEach(candidates::add)

        for (size in 3 downTo 2) {
            if (tokens.size < size) {
                continue
            }
            for (index in 0..tokens.size - size) {
                val window = tokens.subList(index, index + size)
                if (
                    window.all(::isContentToken)
                ) {
                    candidates += window.joinToString(" ")
                }
            }
        }

        contentTokens
            .sortedByDescending(String::length)
            .forEach(candidates::add)

        return candidates
            .map(String::trim)
            .filter {
                it.length in
                    MinimumTermCharacters..MaximumTermCharacters
            }
            .distinct()
            .take(MaximumTerms)
    }

    internal fun speakerScope(content: String): String {
        val normalized = " " + normalizeSentence(content) + " "

        if (
            HumanSpeakerPhrases.any(normalized::contains)
        ) {
            return DirectConversationSourceReturn.HumanSpeaker
        }
        if (
            VexSpeakerPhrases.any(normalized::contains)
        ) {
            return DirectConversationSourceReturn.VexSpeaker
        }
        return DirectConversationSourceReturn.AnySpeaker
    }

    private fun providerContent(
        prepared: Prepared,
        state: String,
        matches: List<DirectConversationSourceReturn.Match>,
    ): String {
        val packet =
            ProviderPacket(
                schemaVersion = ContextSchema,
                truthClass =
                    "REQUEST_BOUND_EXACT_CONVERSATION_CONTEXT_PROJECTION",
                cue = prepared.cue.name,
                queryTerms = prepared.queryTerms,
                speakerScope = prepared.speakerScope,
                state = state,
                matches =
                    matches.map {
                        match ->
                        ProviderMatch(
                            matchedTerms = match.matchedTerms,
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
                historicalCurrentizedEventRefs =
                    matches
                        .flatMap {
                            match ->
                            listOf(
                                match.requestEvent.eventRef,
                                match.responseEvent.eventRef,
                            )
                        }
                        .filter {
                            it in
                                prepared.historicalCurrentizedEventRefs
                        },
                scannedCompletedTurnCount =
                    prepared.discovery.scannedCompletedTurnCount,
                wholeHistoryEventEnumerationPerformed =
                    prepared.discovery.wholeHistoryEventEnumerationPerformed,
                effectAuthorityGranted = false,
                effectsPerformed = false,
            )
        val projectionTruth =
            when (
                state
            ) {
                Returned ->
                    "REQUEST-BOUND CONVERSATION CONTEXT TRUTH: PREPARED_AND_RETURNED. Product prepared exact canonical Conversation context because the current human request explicitly pointed backward; the returned source is included in this block."

                HeldTokenBudget ->
                    "REQUEST-BOUND CONVERSATION CONTEXT TRUTH: PREPARED_HELD_TOKEN_BUDGET. Product located older canonical Conversation context for the current request, but the discovered source did not fit the admitted prompt allowance."

                else ->
                    error(
                        "direct Conversation context projection provider state is not recognized",
                    )
            }
        val historicalCurrentizedReturned =
            matches.any {
                match ->
                match.requestEvent.eventRef in
                    prepared.historicalCurrentizedEventRefs ||
                    match.responseEvent.eventRef in
                    prepared.historicalCurrentizedEventRefs
            }
        val lifecycleTruth =
            if (
                historicalCurrentizedReturned
            ) {
                "LIFECYCLE TRUTH: one or more returned Conversation events are historical source whose semantic relationship has since been corrected or superseded by current Score/semantic authority. Preserve the historical bytes as evidence; do not present them as current Memory or current accepted meaning."
            } else {
                "LIFECYCLE TRUTH: no returned Conversation event is marked corrected/superseded by the current recall membrane."
            }
        return listOf(
            ContextHeader,
            projectionTruth,
            lifecycleTruth,
            "This block is request-bound Conversation context for the current human turn. It is not a provider-requested Conversation Source Return and does not prove that Vex asked for retrieval.",
            "Returned Conversation source is not Memory, semantic acceptance, or a profile of the human.",
            "The cue and query terms are deterministic request/source observations derived from the exact current request; they are not a claim about the human's hidden intent.",
            "Use the exact source actually present here together with other current context. If it is insufficient, preserve uncertainty and use rightful reach if needed.",
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
                    it,
                )
            }

    private fun isContentToken(value: String): Boolean =
        value.length >= 3 &&
            value !in StopWords &&
            value.any(Char::isLetterOrDigit)

    private fun normalizeSentence(value: String): String =
        value
            .lowercase()
            .replace('’', '\'')
            .replace(
                Regex("[^a-z0-9'-]+"),
                " ",
            )
            .trim()
            .replace(
                Regex("\\s+"),
                " ",
            )

    private const val MaximumTerms = 6
    private const val MinimumTermCharacters = 3
    private const val MaximumTermCharacters = 160

    private val Token =
        Regex("[a-z0-9][a-z0-9'-]*")

    private val QuotedPhrase =
        Regex("[\"“”]([^\"“”]{3,160})[\"“”]")

    private val ExactRecallPhrases =
        listOf(
            "what exact",
            "which exact",
            "exact thing",
            "exact word",
            "exact phrase",
            "exact project",
            "what did i give",
            "what did i say",
            "what did we call",
            "where was",
            "what was the",
            "what were the",
        )

    private val PriorOccurrencePhrases =
        listOf(
            "last time",
            "previous time",
            "when we were here before",
            "where did we go",
            "where did i say",
            "when did i say",
            "what did we decide",
            "what did we discuss",
        )

    private val BackReferencePhrases =
        listOf(
            "back to",
            "back at the",
            "at the beginning",
            "from the beginning",
            "earlier",
            "previously",
            "before this",
            "remember when",
            "do you remember",
            "you mentioned",
            "you said",
            "i mentioned",
            "i said",
            "retrieve older context",
            "older context",
            "older source",
            "older detail",
            "earlier correction",
        )

    private val HumanSpeakerPhrases =
        listOf(
            " did i ",
            " i say ",
            " i said ",
            " i give ",
            " i gave ",
            " i tell ",
            " i told ",
            " where did i ",
        )

    private val VexSpeakerPhrases =
        listOf(
            " you said ",
            " you mentioned ",
            " you told me ",
            " what did you ",
        )

    private val StopWords =
        setOf(
            "about",
            "actually",
            "after",
            "again",
            "and",
            "are",
            "back",
            "before",
            "beginning",
            "been",
            "being",
            "but",
            "capability",
            "continuity",
            "detail",
            "did",
            "does",
            "for",
            "from",
            "give",
            "gave",
            "had",
            "has",
            "have",
            "here",
            "guessing",
            "how",
            "into",
            "last",
            "me",
            "more",
            "our",
            "retrieve",
            "revisit",
            "remember",
            "said",
            "say",
            "that",
            "the",
            "their",
            "them",
            "then",
            "there",
            "these",
            "they",
            "this",
            "those",
            "time",
            "told",
            "use",
            "using",
            "view",
            "without",
            "was",
            "were",
            "what",
            "when",
            "where",
            "which",
            "who",
            "why",
            "with",
            "you",
            "your",
        )
}

// [VXG RealForever]
