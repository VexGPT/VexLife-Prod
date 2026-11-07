package vexlife.vision

import vexlife.semantic.VexFamilyContract

/**
 * Product semantic owner for the human-facing VexLife Vision relationship.
 *
 * This Family owns only the meaning of the Vision control surface. It does not own macOS Screen
 * Recording permission, physical capture, Partner/Connection realization, visual-model input,
 * Conversation Context, Memory, training, or publication.
 *
 * Current Partner architecture pressure is intentionally consumed as relationship guidance rather
 * than copied as an unearned universal root. The physical capture side remains separately owned.
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
        PermissionNeeded,
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
        data object RequestHostPermission : Effect

        data object RequestEnable : Effect

        data object RequestSnapshot : Effect
    }

    enum class DisplayState :
        VexFamilyContract.DisplayState<
            VexLifeVisionFamily,
            State
        > {
        Held,
        PermissionNeeded,
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
                    State.PermissionNeeded ->
                        stay(
                            Effect.RequestHostPermission,
                        )

                    State.Ready ->
                        stay(
                            Effect.RequestEnable,
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
                    State.PermissionNeeded,
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
                    State.PermissionNeeded,
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
            State.PermissionNeeded -> DisplayState.PermissionNeeded
            State.Ready -> DisplayState.Ready
            State.Enabled -> DisplayState.Enabled
            State.Looking -> DisplayState.Looking
        }
}

// [VXG RealForever]
