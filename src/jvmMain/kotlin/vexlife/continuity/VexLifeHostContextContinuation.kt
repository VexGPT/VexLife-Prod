package vexlife.continuity

import vexlife.companion.DirectConversationSourceReturn
import vexlife.conversation.DirectConversationStore

/**
 * Experiment-only host preparation for one backward-looking human request.
 *
 * This owner does not decide semantic meaning, mutate Conversation/Memory, or answer the human.
 * It recognizes one bounded class of explicit backward-reference cues, derives inspectable lexical
 * terms from the exact current request, and reuses [DirectConversationSourceReturn] to reopen exact
 * canonical Conversation before provider inference.
 *
 * The first proof intentionally stays narrow: explicit backward-reference language after Working
 * Continuity rollover. Broader associative/context semantics remain held by the EvolveContext lane.
 */
object VexLifeHostContextContinuation {
    const val CueClass = "HOST_CONTEXT_CONTINUATION_CUE"

    enum class Cue {
        EXPLICIT_BACK_REFERENCE,
        PRIOR_OCCURRENCE,
        EXACT_RECALL,
        POINTS_BACK,
    }

    data class Prepared(
        val cue: Cue,
        val queryTerms: List<String>,
        val speakerScope: String,
        val discovery: DirectConversationSourceReturn.Discovery,
    )

    fun prepare(
        conversationOwner: DirectConversationStore.Locked,
        currentRequest: DirectConversationStore.Event,
        excludedEventRefs: Set<String> = emptySet(),
    ): Prepared? {
        require(currentRequest.eventKind == "REQUEST") {
            "host context continuation requires the exact current human request"
        }

        val cue = cueOrNull(currentRequest.content) ?: return null
        val queryTerms = queryTerms(currentRequest.content)
        if (queryTerms.isEmpty()) {
            return null
        }
        val speakerScope = speakerScope(currentRequest.content)
        val discovery =
            DirectConversationSourceReturn.discover(
                conversationOwner = conversationOwner,
                request =
                    DirectConversationSourceReturn.Request(
                        schemaVersion = DirectConversationSourceReturn.RequestSchema,
                        queryTerms = queryTerms,
                        speakerScope = speakerScope,
                    ),
                excludedEventRefs = excludedEventRefs,
            )

        if (discovery.matches.isEmpty()) {
            return null
        }

        return Prepared(
            cue = cue,
            queryTerms = queryTerms,
            speakerScope = speakerScope,
            discovery = discovery,
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
