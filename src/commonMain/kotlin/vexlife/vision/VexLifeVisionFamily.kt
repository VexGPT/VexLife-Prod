package vexlife.vision

import vexlife.semantic.VexFamilyContract

/**
 * Product semantic owner for the human-facing VexLife Vision relationship.
 *
 * This Family owns only the meaning of the Vision control surface. It does not own macOS Screen
 * Recording permission, physical capture, Partner/Connection realization, visual-model input,
 * Conversation Context, Memory, training, or publication.
 *
 * Enable/Disable are local human-facing arming semantics only. They do not request or establish
 * host permission, start a sensor, or make capture reachable. The only outside Vision effect owned
 * by this Family vocabulary is one explicit RequestSnapshot.
 */
object VexLifeVisionFamily :
    VexFamilyContract<
        VexLifeVisionFamily,
        VexLifeVisionFamily.State,
        VexLifeVisionFamily.Control,
        VexLifeVisionFamily.Effect
    >() {
    enum class State :
        VexFamilyContract.State<VexLifeVisionFamily> {
        Held,
        Ready,
        Enabled,
        Looking,
    }

    sealed interface Control :
        VexFamilyContract.Control<VexLifeVisionFamily> {
        data object Enable : Control

        data object Disable : Control

        data object LookNow : Control
    }

    sealed interface Effect :
        VexFamilyContract.Effect<VexLifeVisionFamily> {
        data object RequestSnapshot : Effect
    }

    enum class DisplayState :
        VexFamilyContract.DisplayState<
            VexLifeVisionFamily,
            State
        > {
        Held,
        Ready,
        Enabled,
        Looking,
    }

    override fun act(
        currentState: State,
        control: Control,
    ): Act<
        VexLifeVisionFamily,
        State,
        Effect
    >? =
        when (
            control
        ) {
            Control.Enable ->
                when (
                    currentState
                ) {
                    State.Ready ->
                        admit(
                            State.Enabled,
                        )

                    State.Held,
                    State.Enabled,
                    State.Looking -> null
                }

            Control.Disable ->
                when (
                    currentState
                ) {
                    State.Enabled ->
                        admit(
                            State.Ready,
                        )

                    State.Held,
                    State.Ready,
                    State.Looking -> null
                }

            Control.LookNow ->
                when (
                    currentState
                ) {
                    State.Enabled ->
                        stay(
                            Effect.RequestSnapshot,
                        )

                    State.Held,
                    State.Ready,
                    State.Looking -> null
                }
        }

    override fun displayStateFor(
        state: State,
    ): DisplayState =
        when (
            state
        ) {
            State.Held -> DisplayState.Held
            State.Ready -> DisplayState.Ready
            State.Enabled -> DisplayState.Enabled
            State.Looking -> DisplayState.Looking
        }
}

// [VXG RealForever]
