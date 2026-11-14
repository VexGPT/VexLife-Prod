package vexlife.diagnostics

import kotlinx.serialization.Serializable
import vexlife.semantic.VexFamilyContract
import vexlife.text.VexLifeText

/**
 * Product semantic owner for what the person can do with VexLife's debug history.
 *
 * The meaningful chronology is always recorded; it never waits for someone to press "start
 * logging". What the person controls is only the optional higher-detail capture (short-lived
 * process and memory snapshots), clearing the debug history, and exporting one bounded ZIP to hand
 * to whoever is helping them. Clearing removes debug-owned history only: never Conversation,
 * Memory, Home identity, or any receipt that correctness depends on.
 */
object VexLifeDiagnosticsFamily :
    VexFamilyContract<
        VexLifeDiagnosticsFamily,
        VexLifeDiagnosticsFamily.State,
        VexLifeDiagnosticsFamily.Control,
        VexLifeDiagnosticsFamily.Effect,
    >() {
    sealed interface State : VexFamilyContract.State<VexLifeDiagnosticsFamily> {
        val stateRef: String
    }

    /** Normal chronology only. */
    data object Recording : State {
        override val stateRef: String = StateRecording
    }

    /** Normal chronology plus snapshots until [until] (ISO instant), after which it ends itself. */
    data class RecordingEnhanced(val until: String) : State {
        override val stateRef: String = StateRecordingEnhanced

        init {
            require(until.isNotBlank()) { "enhanced capture needs an end" }
        }
    }

    sealed interface Control : VexFamilyContract.Control<VexLifeDiagnosticsFamily> {
        val controlRef: String
    }

    data object StartEnhanced : Control {
        override val controlRef: String = ControlStartEnhanced
    }

    data object StopEnhanced : Control {
        override val controlRef: String = ControlStopEnhanced
    }

    data object ClearHistory : Control {
        override val controlRef: String = ControlClearHistory
    }

    sealed interface Effect : VexFamilyContract.Effect<VexLifeDiagnosticsFamily> {
        data object BeginEnhancedCapture : Effect

        data object EndEnhancedCapture : Effect

        /** Remove chronology files and enhanced-capture material only. */
        data object ClearDebugHistory : Effect
    }

    @Serializable
    data class ControlDisplay(val controlRef: String, val label: VexLifeText)

    /** Language-free: every word is a key resolved by the presentation. */
    @Serializable
    data class DisplayState(
        val status: VexLifeText,
        val capture: VexLifeText,
        val controls: List<ControlDisplay>,
        val enhanced: Boolean,
    ) : VexFamilyContract.DisplayState<VexLifeDiagnosticsFamily, State>

    override fun act(
        currentState: State,
        control: Control,
    ): Act<VexLifeDiagnosticsFamily, State, Effect>? =
        when {
            currentState == Recording && control == StartEnhanced -> stay(Effect.BeginEnhancedCapture)
            currentState is RecordingEnhanced && control == StopEnhanced ->
                stay(Effect.EndEnhancedCapture)
            control == ClearHistory -> stay(Effect.ClearDebugHistory)
            else -> null
        }

    override fun displayStateFor(state: State): DisplayState =
        when (state) {
            Recording ->
                DisplayState(
                    status = VexLifeText("workspace.status.diagnostics-recording"),
                    capture = VexLifeText("workspace.status.diagnostics-enhanced-off"),
                    controls =
                        listOf(
                            ControlDisplay(
                                ControlStartEnhanced,
                                VexLifeText("workspace.button.diagnostics-start-enhanced"),
                            ),
                            ControlDisplay(
                                ControlClearHistory,
                                VexLifeText("workspace.button.diagnostics-clear-history"),
                            ),
                        ),
                    enhanced = false,
                )

            is RecordingEnhanced ->
                DisplayState(
                    status = VexLifeText("workspace.status.diagnostics-recording"),
                    capture =
                        VexLifeText.text(
                            "workspace.status.diagnostics-enhanced-on",
                            "until" to state.until,
                        ),
                    controls =
                        listOf(
                            ControlDisplay(
                                ControlStopEnhanced,
                                VexLifeText("workspace.button.diagnostics-stop-enhanced"),
                            ),
                            ControlDisplay(
                                ControlClearHistory,
                                VexLifeText("workspace.button.diagnostics-clear-history"),
                            ),
                        ),
                    enhanced = true,
                )
        }

    fun controlForRef(controlRef: String): Control? =
        when (controlRef) {
            ControlStartEnhanced -> StartEnhanced
            ControlStopEnhanced -> StopEnhanced
            ControlClearHistory -> ClearHistory
            else -> null
        }

    const val StateRecording = "RECORDING"
    const val StateRecordingEnhanced = "RECORDING_ENHANCED"
    const val ControlStartEnhanced = "START_ENHANCED"
    const val ControlStopEnhanced = "STOP_ENHANCED"
    const val ControlClearHistory = "CLEAR_HISTORY"
}

// [VXG RealForever]
