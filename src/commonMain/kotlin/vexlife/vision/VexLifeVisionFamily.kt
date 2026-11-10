package vexlife.vision

import vexlife.semantic.VexFamilyContract

/**
 * Product semantic owner for the human-facing VexLife Vision relationship.
 *
 * This Family owns only the meaning of the Vision control surface. It does not own macOS Screen
 * Recording permission, physical capture, Partner/Connection realization, visual-model input,
 * Conversation Context, Memory, training, or publication.
 *
 * The accepted first Product relationship is one explicit Look Now request. There is no standing
 * enable/permission state in this Family: Ready means the Product route is reachable, Looking means
 * one Look Now request is in progress, and RequestSnapshot is the sole outside Vision effect.
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
        Looking,
    }

    sealed interface Control :
        VexFamilyContract.Control<VexLifeVisionFamily> {
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
            Control.LookNow ->
                when (
                    currentState
                ) {
                    State.Ready ->
                        admit(
                            state = State.Looking,
                            effect = Effect.RequestSnapshot,
                        )

                    State.Held,
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
            State.Looking -> DisplayState.Looking
        }
}

// [VXG RealForever]
