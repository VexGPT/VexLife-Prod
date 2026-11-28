package vexlife.memory

import kotlinx.serialization.Serializable
import vexlife.semantic.VexFamilyContract
import vexlife.text.VexLifeText

/**
 * Product semantic owner for what a person is told about Vex's long-term Memory.
 *
 * Memory itself is owned elsewhere: accepted Score statements, the committed Daily Memory stratum
 * and the Living Journal projection read by [VexLifeCurrentMemoryReader]. This Family owns only the
 * human-facing meaning of that truth: whether Vex is drawing on Memory in its replies now, and if
 * not, why.
 *
 * It has no Controls yet. Product now has a bounded semantic-acceptance writer, but no ordinary
 * human/runtime Control is connected to it yet, and Score append remains separately owned. The manual
 * Daily Dream is only lawful when Score has caught up with the current Conversation, which the current
 * UI/runtime path still cannot bring about. A Rest control here would therefore still be one that
 * never becomes lawful, so Controls arrive with the next Memory integration relationship.
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

    /** Language-free: words remain keyed until the concrete presentation resolves them. */
    @Serializable
    data class DisplayState(
        val label: VexLifeText,
        val detail: VexLifeText,
    ) : VexFamilyContract.DisplayState<VexLifeMemoryFamily, State>

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
                    label =
                        VexLifeText(
                            "workspace.status.memory-current",
                        ),
                    detail =
                        when (state.rememberedCount) {
                            0 ->
                                VexLifeText(
                                    "workspace.description.memory-current-empty",
                                )

                            1 ->
                                VexLifeText(
                                    "workspace.description.memory-current-one",
                                )

                            else ->
                                VexLifeText.text(
                                    "workspace.description.memory-current-many",
                                    "count" to state.rememberedCount.toString(),
                                )
                        },
                )

            NotFormed ->
                DisplayState(
                    label =
                        VexLifeText(
                            "workspace.status.memory-not-formed",
                        ),
                    detail =
                        VexLifeText(
                            "workspace.description.memory-not-formed",
                        ),
                )

            Behind ->
                DisplayState(
                    label =
                        VexLifeText(
                            "workspace.status.memory-behind",
                        ),
                    detail =
                        VexLifeText(
                            "workspace.description.memory-behind",
                        ),
                )

            Unreadable ->
                DisplayState(
                    label =
                        VexLifeText(
                            "workspace.status.memory-unreadable",
                        ),
                    detail =
                        VexLifeText(
                            "workspace.description.memory-unreadable",
                        ),
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
