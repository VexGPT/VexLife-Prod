package vexlife.conversation

import kotlinx.serialization.Serializable
import vexlife.semantic.VexFamilyContract
import vexlife.text.VexLifeText

/**
 * Product semantic owner for how much of this thread Vex is given.
 *
 * Why it exists: while testing, the person wants Vex to start without inheriting the whole
 * Conversation, but nothing they and Vex said may be erased. Clearing thread memory therefore sets
 * a context horizon at the current completed turn: everything at or before it stays in the record
 * and on screen, and is no longer given to Vex (recent Conversation, Conversation source return, a
 * working rollover formed before it). Restoring removes the horizon. Memory pages are a separate
 * owner and are unchanged.
 */
object VexLifeThreadContextFamily :
    VexFamilyContract<
        VexLifeThreadContextFamily,
        VexLifeThreadContextFamily.State,
        VexLifeThreadContextFamily.Control,
        VexLifeThreadContextFamily.Effect,
    >() {
    sealed interface State : VexFamilyContract.State<VexLifeThreadContextFamily> {
        val stateRef: String
    }

    /** No completed turn yet: there is nothing to clear. */
    data object Empty : State {
        override val stateRef: String = StateEmpty
    }

    /** Vex is given the whole thread (as much as fits). */
    data class Whole(val exchanges: Long) : State {
        override val stateRef: String = StateWhole
    }

    /** Vex is given only what came after the horizon. */
    data class FromHorizon(
        val exchangesBehind: Long,
        val exchangesSince: Long,
        val clearedAt: String,
    ) : State {
        override val stateRef: String = StateFromHorizon
    }

    sealed interface Control : VexFamilyContract.Control<VexLifeThreadContextFamily> {
        val controlRef: String
    }

    data object Clear : Control {
        override val controlRef: String = ControlClear
    }

    data object Restore : Control {
        override val controlRef: String = ControlRestore
    }

    sealed interface Effect : VexFamilyContract.Effect<VexLifeThreadContextFamily> {
        /** Set the horizon at the current completed head. Deletes nothing. */
        data object SetHorizon : Effect

        /** Remove the horizon. Deletes nothing. */
        data object RemoveHorizon : Effect
    }

    @Serializable data class ControlDisplay(val controlRef: String, val label: VexLifeText)

    /** Language-free: every word is a key resolved by the presentation. */
    @Serializable
    data class DisplayState(val status: VexLifeText, val controls: List<ControlDisplay>) :
        VexFamilyContract.DisplayState<VexLifeThreadContextFamily, State>

    override fun act(
        currentState: State,
        control: Control,
    ): Act<VexLifeThreadContextFamily, State, Effect>? =
        when {
            control == Clear && currentState !is Empty -> stay(Effect.SetHorizon)
            control == Restore && currentState is FromHorizon -> stay(Effect.RemoveHorizon)
            else -> null
        }

    private val clear =
        ControlDisplay(ControlClear, VexLifeText("workspace.button.thread-context-clear"))
    private val restore =
        ControlDisplay(ControlRestore, VexLifeText("workspace.button.thread-context-restore"))

    override fun displayStateFor(state: State): DisplayState =
        when (state) {
            Empty -> DisplayState(VexLifeText("workspace.status.thread-context-empty"), emptyList())
            is Whole ->
                DisplayState(
                    VexLifeText.text(
                        "workspace.status.thread-context-whole",
                        "exchanges" to state.exchanges.toString(),
                    ),
                    listOf(clear),
                )
            is FromHorizon ->
                DisplayState(
                    VexLifeText.text(
                        "workspace.status.thread-context-cleared",
                        "behind" to state.exchangesBehind.toString(),
                        "since" to state.exchangesSince.toString(),
                        "at" to state.clearedAt,
                    ),
                    // Clearing again moves the horizon to the newest turn.
                    listOfNotNull(clear.takeIf { state.exchangesSince > 0 }, restore),
                )
        }

    fun controlForRef(controlRef: String): Control? =
        when (controlRef) {
            ControlClear -> Clear
            ControlRestore -> Restore
            else -> null
        }

    const val StateEmpty = "EMPTY"
    const val StateWhole = "WHOLE"
    const val StateFromHorizon = "FROM_HORIZON"
    const val ControlClear = "CLEAR_THREAD_MEMORY"
    const val ControlRestore = "RESTORE_THREAD_MEMORY"
}

// [VXG RealForever]
