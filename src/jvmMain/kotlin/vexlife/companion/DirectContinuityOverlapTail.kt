package vexlife.companion

import java.time.Duration
import kotlinx.serialization.Serializable
import vexlife.continuity.VexLifeTurnNavigation
import vexlife.conversation.DirectConversationStore

/**
 * Which exact recent turns a working-checkpoint successor keeps, so the first turn after a rollover
 * reads as a continuation instead of waking up with only coordinates.
 *
 * A rollover is a prompt-residency decision, not a deletion: the checkpoint's working set says
 * where the work stands, and this tail keeps the person's and Vex's own last words so "that", a
 * tone, or an unfinished correction still lands. Older turns stay in Conversation and are reachable
 * through revisit.
 *
 * Policy (a candidate to measure, not a settled constant):
 * ```
 * floor      at least FloorPairs exact completed pairs, newest first, while each fits the admissible
 *            room; the turn that triggered the rollover is always the first one kept
 * expansion  then older pairs while the newer neighbour still leans on them (it points back, or it
 *            corrects them), the exchange was dense in human time, or the pair is small and from the
 *            same sitting — and the whole tail stays inside the expansion budget
 * contiguous the tail is always an exact suffix of the Conversation; nothing is skipped or summarised
 * ```
 *
 * When the floor cannot be met because the room is too small, that is reported as
 * [FloorState.DEGRADED_ROOM] rather than claimed. When not even the newest pair fits, the newest
 * Vex reply alone is kept as the earlier successor bridge did, and [Evidence.responseBridgeOnly]
 * says so. When not even that reply fits, the state is [FloorState.HELD_TOKEN_BUDGET]: the
 * successor carries no exact turn at all, and older source is reachable only through continuity
 * revisit and the working coordinates.
 */
object DirectContinuityOverlapTail {
    /**
     * Victor's minimum target for completed turn-pairs carried across a rollover, when admissible.
     */
    const val FloorPairs = 3

    /** Safety bound on one tail; the token budgets normally stop expansion long before this. */
    const val MaximumPairs = 16

    /** A neighbour this close in human time still belongs to the same stretch of conversation. */
    val DenseExchange: Duration = Duration.ofMinutes(10)

    /** A pair this small costs less than the confusion of losing it, within one sitting. */
    const val SmallPairTokens = 300

    /** A longer silence than this marks a return; small exchanges before it do not carry deixis. */
    val SessionGap: Duration = Duration.ofHours(2)

    /** Of the room left after fixed context, at most this share may be spent meeting the floor. */
    const val AdmissibleRoomDivisor = 2

    /** Of the same room, at most this share may be spent on the whole tail once it expands. */
    const val ExpansionRoomDivisor = 8

    enum class FloorState {
        MET,
        HISTORY_SHORTER_THAN_FLOOR,
        DEGRADED_ROOM,
        /** Not one exact event fits: zero overlap was unavoidable, and is named. */
        HELD_TOKEN_BUDGET,
    }

    enum class ExpansionReason {
        POINTS_BACK,
        CORRECTION_CHAIN,
        DENSE_EXCHANGE,
        SMALL_PAIR,
    }

    /** What the receipt records about the tail, so a reader can see what was kept and why. */
    @Serializable
    data class Evidence(
        val floorTargetPairs: Int,
        val selectedPairs: Int,
        val floorState: FloorState,
        /** One reason per pair kept beyond the floor, newest first. */
        val expansionReasons: List<ExpansionReason>,
        val selectedTokens: Int,
        val admissibleTokens: Int,
        val expansionBudgetTokens: Int,
        val responseBridgeOnly: Boolean,
        val policyRef: String = PolicyRef,
    )

    const val PolicyRef = "policy.vexlife-prod.continuity.overlap-tail/v1"

    data class Selection(
        /** Exact events in Conversation order. */
        val events: List<DirectConversationStore.Event>,
        val evidence: Evidence,
    )

    /**
     * Select the tail from [newestFirst], a contiguous newest-first run of completed pairs that
     * already fit [admissibleTokens]. [historyComplete] says the run reaches the first turn.
     */
    fun select(
        newestFirst: List<VexLifeTurnNavigation.Turn>,
        historyComplete: Boolean,
        admissibleTokens: Int,
        expansionBudgetTokens: Int,
        estimateTokens: (String) -> Int,
    ): Selection {
        fun cost(turn: VexLifeTurnNavigation.Turn) =
            estimateTokens(turn.request.content) + estimateTokens(turn.response.content)

        val kept = mutableListOf<VexLifeTurnNavigation.Turn>()
        var tokens = 0
        for (turn in newestFirst.take(FloorPairs)) {
            if (tokens + cost(turn) > admissibleTokens) break
            kept += turn
            tokens += cost(turn)
        }
        val floorState =
            when {
                kept.size >= FloorPairs -> FloorState.MET
                kept.size == newestFirst.size && historyComplete ->
                    FloorState.HISTORY_SHORTER_THAN_FLOOR
                else -> FloorState.DEGRADED_ROOM
            }

        val reasons = mutableListOf<ExpansionReason>()
        if (floorState == FloorState.MET) {
            while (kept.size < newestFirst.size && kept.size < MaximumPairs) {
                val newer = kept.last()
                val older = newestFirst[kept.size]
                val reason = expansionReason(older = older, newer = newer, cost = cost(older))
                if (reason == null || tokens + cost(older) > expansionBudgetTokens) break
                kept += older
                tokens += cost(older)
                reasons += reason
            }
        }

        return Selection(
            events = kept.asReversed().flatMap { listOf(it.request, it.response) },
            evidence =
                Evidence(
                    floorTargetPairs = FloorPairs,
                    selectedPairs = kept.size,
                    floorState = floorState,
                    expansionReasons = reasons,
                    selectedTokens = tokens,
                    admissibleTokens = admissibleTokens,
                    expansionBudgetTokens = expansionBudgetTokens,
                    responseBridgeOnly = false,
                ),
        )
    }

    /**
     * The tail for one successor prompt, read from the locked canonical Conversation with
     * [roomTokens] left after fixed context. Reads stay bounded: the expansion budget first, the
     * admissible room only when the floor needs it.
     */
    internal fun fromConversation(
        conversationOwner: DirectConversationStore.Locked,
        prior: DirectConversationStore.ImmediatePriorCompletedTurn,
        roomTokens: Int,
        estimateTokens: (String) -> Int,
    ): Selection {
        val admissible = maxOf(0, roomTokens / AdmissibleRoomDivisor)
        val expansionBudget = maxOf(0, roomTokens / ExpansionRoomDivisor)
        val near = conversationOwner.recentCompletedConversation(expansionBudget, estimateTokens)
        val run =
            if (near.events.size / 2 >= FloorPairs || near.wholeHistoryEventEnumerationPerformed) {
                near
            } else {
                conversationOwner.recentCompletedConversation(admissible, estimateTokens)
            }
        val pairs = VexLifeTurnNavigation.project(run.events)
        val selection =
            select(
                newestFirst = pairs.asReversed(),
                historyComplete = run.wholeHistoryEventEnumerationPerformed,
                admissibleTokens = admissible,
                expansionBudgetTokens = expansionBudget,
                estimateTokens = estimateTokens,
            )
        if (selection.events.isNotEmpty()) {
            require(
                selection.events.takeLast(2) == listOf(prior.requestEvent, prior.responseEvent)
            ) {
                "overlap tail must end with the exact turn that formed the checkpoint"
            }
            return selection
        }
        val bridgeTokens = estimateTokens(prior.responseEvent.content)
        return Selection(
            events = if (bridgeTokens <= admissible) listOf(prior.responseEvent) else emptyList(),
            evidence =
                selection.evidence.copy(
                    selectedTokens = if (bridgeTokens <= admissible) bridgeTokens else 0,
                    responseBridgeOnly = bridgeTokens <= admissible,
                    floorState =
                        if (bridgeTokens <= admissible) selection.evidence.floorState
                        else FloorState.HELD_TOKEN_BUDGET,
                ),
        )
    }

    private fun expansionReason(
        older: VexLifeTurnNavigation.Turn,
        newer: VexLifeTurnNavigation.Turn,
        cost: Int,
    ): ExpansionReason? =
        when {
            newer.correctionCue != null -> ExpansionReason.CORRECTION_CHAIN
            newer.pointsBack -> ExpansionReason.POINTS_BACK
            !VexLifeTurnNavigation.gap(older, newer).isNegative &&
                VexLifeTurnNavigation.gap(older, newer) <= DenseExchange ->
                ExpansionReason.DENSE_EXCHANGE
            cost <= SmallPairTokens && VexLifeTurnNavigation.gap(older, newer) <= SessionGap ->
                ExpansionReason.SMALL_PAIR
            else -> null
        }
}

// [VXG RealForever]
