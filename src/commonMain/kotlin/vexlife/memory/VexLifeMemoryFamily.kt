package vexlife.memory

import kotlinx.serialization.Serializable
import vexlife.semantic.VexFamilyContract

/**
 * Product semantic owner for what a person is told about Vex's long-term Memory.
 *
 * Memory itself is owned elsewhere: accepted Score statements, the committed Daily Memory stratum
 * and the Living Journal projection read by [VexLifeCurrentMemoryReader]. This Family owns only the
 * human-facing meaning of that truth: whether Vex is drawing on Memory in its replies now, and if
 * not, why.
 *
 * It has no Controls yet. Memory can't be formed from inside VexLife-Prod (Score formation needs a
 * semantic-acceptance owner Product doesn't have), and the manual Daily Dream is only lawful when
 * Score has caught up with the current Conversation, which nothing in Product can bring about. A
 * Rest control here would be one that never becomes lawful, so it arrives with Memory formation.
 */
object VexLifeMemoryFamily :
    VexFamilyContract<
        VexLifeMemoryFamily,
        VexLifeMemoryFamily.State,
        VexLifeMemoryFamily.Control,
        VexLifeMemoryFamily.Effect,
    >() {
    sealed interface State : VexFamilyContract.State<VexLifeMemoryFamily> {
        val stateRef: String
    }

    /** Memory is current; [rememberedCount] accepted statements enter each prompt. */
    data class Current(val rememberedCount: Int) : State {
        init {
            require(rememberedCount >= 0) { "remembered count must be nonnegative" }
        }

        override val stateRef: String = StateCurrent
    }

    /** No Daily Memory has ever been committed in this Home. */
    data object NotFormed : State {
        override val stateRef: String = StateNotFormed
    }

    /**
     * Daily Memory exists but was formed before the current Conversation head, so the reader holds
     * it out of prompts. It is kept, not lost.
     */
    data object Behind : State {
        override val stateRef: String = StateBehind
    }

    /** The Memory owners could not be read. Nothing is assumed and nothing was changed. */
    data object Unreadable : State {
        override val stateRef: String = StateUnreadable
    }

    sealed interface Control : VexFamilyContract.Control<VexLifeMemoryFamily>

    sealed interface Effect : VexFamilyContract.Effect<VexLifeMemoryFamily>

    @Serializable
    data class DisplayState(val label: String, val detail: String) :
        VexFamilyContract.DisplayState<VexLifeMemoryFamily, State> {
        init {
            require(label.isNotBlank() && detail.isNotBlank()) { "Memory DisplayState is invalid" }
        }
    }

    /**
     * Reconcile the Memory reader's answer into Family State.
     *
     * @param readable false when the reader failed to read the Memory owners.
     * @param current true when the reader's projection is CURRENT.
     * @param heldReasons the reader's reasons when it holds Memory out of prompts.
     * @param rememberedCount pages the reader would include in the prompt.
     */
    fun observe(
        readable: Boolean,
        current: Boolean,
        heldReasons: List<String>,
        rememberedCount: Int,
    ): State =
        when {
            !readable -> Unreadable
            current -> Current(rememberedCount)
            heldReasons == listOf(ReasonNoCommittedDailyMemory) -> NotFormed
            heldReasons == listOf(ReasonDailyMemoryStale) -> Behind
            else -> Unreadable
        }

    override fun act(
        currentState: State,
        control: Control,
    ): Act<VexLifeMemoryFamily, State, Effect>? = null

    override fun displayStateFor(state: State): DisplayState =
        when (state) {
            is Current ->
                DisplayState(
                    label = "Memory · Up to date",
                    detail =
                        when (state.rememberedCount) {
                            0 -> "Vex's Memory is up to date and doesn't hold anything yet."
                            1 -> "Vex brings 1 thing it remembers into each reply."
                            else ->
                                "Vex brings ${state.rememberedCount} things it remembers into each reply."
                        },
                )

            NotFormed ->
                DisplayState(
                    label = "Memory · Not formed yet",
                    detail =
                        "Vex has no long-term Memory yet, so it won't recall earlier conversations. " +
                            "VexLife can't form new Memory on its own yet; What Vex receives shows " +
                            "exactly what each reply was given.",
                )

            Behind ->
                DisplayState(
                    label = "Memory · Behind",
                    detail =
                        "Vex's Memory was formed before your latest conversation, and VexLife can't " +
                            "bring newer conversation into it yet, so Vex isn't drawing on it for now. " +
                            "Nothing in it was lost.",
                )

            Unreadable ->
                DisplayState(
                    label = "Memory · Unavailable",
                    detail =
                        "VexLife couldn't read Vex's Memory, so it isn't being used right now. Nothing was changed.",
                )
        }

    const val StateCurrent = "CURRENT"
    const val StateNotFormed = "NOT_FORMED"
    const val StateBehind = "BEHIND"
    const val StateUnreadable = "UNREADABLE"

    const val ReasonNoCommittedDailyMemory = "NO_COMMITTED_DAILY_MEMORY"
    const val ReasonDailyMemoryStale = "DAILY_MEMORY_SOURCE_FRONTIER_STALE"

    val StateRefs: Set<String> = setOf(StateCurrent, StateNotFormed, StateBehind, StateUnreadable)
}

// [VXG RealForever]
