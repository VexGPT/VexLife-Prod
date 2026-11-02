package vexlife.reference.compose

import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import vexlife.semantic.VexFamilyContract

/**
 * Reference-only relationship used to prove the renderer contract.
 *
 * It is not a VexLife product feature and is intentionally not wired into the Workspace reference.
 */
internal class ReferenceToggleFamily :
    VexFamilyContract<
        ReferenceToggleFamily,
        ReferenceToggleFamily.State,
        ReferenceToggleFamily.Control,
        ReferenceToggleFamily.Effect
    >() {
    enum class State :
        VexFamilyContract.State<ReferenceToggleFamily> {
        Inactive,
        Active,
    }

    sealed interface Control :
        VexFamilyContract.Control<ReferenceToggleFamily> {
        data class Select(
            val active: Boolean,
        ) : Control
    }

    sealed interface Effect :
        VexFamilyContract.Effect<ReferenceToggleFamily> {
        data class Remember(
            val active: Boolean,
        ) : Effect
    }

    sealed interface DisplayState :
        VexFamilyContract.DisplayState<
            ReferenceToggleFamily,
            State
        > {
        val checked: Boolean

        data object Inactive :
            DisplayState {
            override val checked =
                false
        }

        data object Active :
            DisplayState {
            override val checked =
                true
        }
    }

    override fun act(
        currentState: State,
        control: Control,
    ): Act<
        ReferenceToggleFamily,
        State,
        Effect
    > =
        when (
            control
        ) {
            is Control.Select -> {
                val target =
                    if (
                        control.active
                    ) {
                        State.Active
                    } else {
                        State.Inactive
                    }
                val effect =
                    Effect.Remember(
                        active =
                            control.active,
                    )

                if (
                    currentState ==
                    target
                ) {
                    stay(
                        effect,
                    )
                } else {
                    admit(
                        state =
                            target,
                        effect =
                            effect,
                    )
                }
            }
        }

    override fun displayStateFor(
        state: State,
    ): DisplayState =
        when (
            state
        ) {
            State.Inactive ->
                DisplayState.Inactive

            State.Active ->
                DisplayState.Active
        }

    override fun projectionFor(
        state: State,
    ): Projection<
        ReferenceToggleFamily,
        State,
        DisplayState
    > =
        Projection(
            state =
                state,
            displayState =
                displayStateFor(
                    state,
                ),
        )
}

/**
 * Reference renderer law:
 *
 * DisplayState -> checked presentation
 * UI interaction -> typed Control
 *
 * The Composable does not receive Family State, Store/provider, Effect, execution or Logger.
 */
@Composable
internal fun ReferenceToggleControl(
    projection: VexFamilyContract.Projection<
        ReferenceToggleFamily,
        ReferenceToggleFamily.State,
        ReferenceToggleFamily.DisplayState
    >,
    onControl: (ReferenceToggleFamily.Control) -> Unit,
) {
    Switch(
        checked =
            projection
                .displayState
                .checked,
        onCheckedChange = {
            onControl(
                ReferenceToggleFamily
                    .Control
                    .Select(
                        active =
                            it,
                    ),
            )
        },
    )
}

// [VXG RealForever]
