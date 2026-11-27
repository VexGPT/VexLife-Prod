package vexlife.continuity

import java.time.Duration
import java.time.Instant
import vexlife.conversation.DirectConversationStore

/**
 * What the host can know about each completed turn without interpreting it.
 *
 * One [Turn] per completed REQUEST/RESPONSE pair, keyed by the turn's own canonical `turnRef`.
 * Every field is a deterministic function of the exact canonical events, so this projection is
 * recomputed when it is needed and never stored: it cannot drift from Conversation, it adds no
 * identity of its own, and nothing in it is semantic acceptance.
 *
 * The two cues are host lexical observations of the person's own opening words:
 * - [Turn.correctionCue]: the request opens the way a correction usually opens ("actually", "no,",
 *   "that's not what I meant"). It points at the reply just before it. It is not a reading of what
 *   the person meant or felt, and it does not change that reply.
 * - [Turn.pointsBack]: the request opens by pointing at what came just before ("that", "it", "what
 *   you said"), so the earlier turn is likely needed to read it.
 *
 * Both are English-only at this revision. A request in another language yields no cue; that is a
 * missing observation, never a negative one.
 */
object VexLifeTurnNavigation {
    const val CueClass = "HOST_LEXICAL_CUE"

    data class Turn(
        val turnRef: String,
        /**
         * 0-based position within the projected events: the thread's position when the whole
         * canonical Conversation is projected, a relative position when only a recent run is.
         */
        val ordinal: Int,
        val request: DirectConversationStore.Event,
        val response: DirectConversationStore.Event,
        val requestedAt: Instant,
        val respondedAt: Instant,
        /** The opening that matched, quoted from the person's words; null when none matched. */
        val correctionCue: String?,
        val pointsBack: Boolean,
    ) {
        /** The turn whose reply this request appears to correct: always the one just before. */
        val correctsOrdinal: Int?
            get() = if (correctionCue != null && ordinal > 0) ordinal - 1 else null
    }

    /** One node per completed pair of [canonicalEvents], in Conversation order. */
    fun project(canonicalEvents: List<DirectConversationStore.Event>): List<Turn> {
        require(canonicalEvents.size % 2 == 0) {
            "turn navigation requires complete REQUEST/RESPONSE pairs"
        }
        return canonicalEvents.chunked(2).mapIndexed {
            ordinal,
            (request, response),
            ->
            projectPair(
                request = request,
                response = response,
                ordinal = ordinal,
            )
        }
    }

    /**
     * Project one exact canonical completed pair without requiring the whole Conversation.
     *
     * [ordinal] is the pair's position in the canonical completed-head lineage. It must not be
     * inferred from the physical event sequence: abandoned/off-head attempts may consume event
     * sequence coordinates without becoming completed Conversation turns.
     */
    fun projectPair(
        request: DirectConversationStore.Event,
        response: DirectConversationStore.Event,
        ordinal: Int,
    ): Turn {
        require(
            request.eventKind ==
                "REQUEST" &&
                response.eventKind ==
                    "RESPONSE" &&
                request.turnRef ==
                    response.turnRef &&
                request.sequence >=
                    0 &&
                request.sequence %
                    2 ==
                    0L &&
                response.sequence ==
                    request.sequence +
                    1 &&
                response.priorEventHash ==
                    request.eventHash
        ) {
            "turn navigation requires one exact canonical REQUEST/RESPONSE pair"
        }
        return Turn(
            turnRef =
                request.turnRef,
            ordinal =
                ordinal
                    .also {
                        require(
                            it >=
                                0
                        ) {
                            "turn navigation canonical ordinal must be non-negative"
                        }
                    },
            request =
                request,
            response =
                response,
            requestedAt =
                Instant.parse(
                    request.formedAt,
                ),
            respondedAt =
                Instant.parse(
                    response.formedAt,
                ),
            correctionCue =
                correctionCueOrNull(
                    request.content,
                ),
            pointsBack =
                pointsBack(
                    request.content,
                ),
        )
    }

    /** Human time between the end of [earlier] and the start of [later]. */
    fun gap(earlier: Turn, later: Turn): Duration =
        Duration.between(earlier.respondedAt, later.requestedAt)

    fun correctionCueOrNull(requestContent: String): String? {
        val opening = opening(requestContent)
        CorrectionOpeners.firstOrNull { opening.startsWith(it) }
            ?.let {
                return it.trim()
            }
        val head = opening.take(160)
        return CorrectionPhrases.firstOrNull { head.contains(it) }
    }

    fun pointsBack(requestContent: String): Boolean {
        val opening = opening(requestContent)
        val firstWord = opening.takeWhile { it.isLetter() || it == '\'' }
        return firstWord in PointingWords || PointingPhrases.any { opening.take(120).contains(it) }
    }

    private fun opening(content: String): String =
        content.trim().lowercase().replace('’', '\'').replace(Regex("\\s+"), " ")

    private val CorrectionOpeners =
        listOf(
            "actually",
            "no,",
            "no.",
            "no ",
            "nope",
            "not quite",
            "not exactly",
            "that's not",
            "that is not",
            "thats not",
            "i meant",
            "i mean,",
            "correction",
            "to clarify",
            "i didn't mean",
            "i did not mean",
            "i'm not ",
            "i am not ",
            "wait,",
            "hold on",
        )

    private val CorrectionPhrases =
        listOf(
            "that's not what i meant",
            "that is not what i meant",
            "not what i meant",
            "you misunderstood",
            "you misread",
            "you got that wrong",
        )

    private val PointingWords =
        setOf(
            "that",
            "that's",
            "this",
            "it",
            "it's",
            "its",
            "those",
            "these",
            "them",
            "same",
            "above",
            "and",
            "also",
            "so",
            "then",
            "why",
            "continue",
            "more",
        )

    private val PointingPhrases =
        listOf(
            "you just said",
            "what you said",
            "you mentioned",
            "as you said",
            "the last one",
            "go on",
        )
}

// [VXG RealForever]
