package vexlife.semantic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VexFamilyContractTest {
    @Test
    fun parameterIdentitySurvivesControlToEffectAndDisplayIsDerivedFromState() {
        val family =
            SpecimenFamily()
        val mode =
            SpecimenFamily.Parameter.Mode(
                active = true,
            )

        val act =
            assertNotNull(
                family.resolve(
                    currentState =
                        SpecimenFamily.State.Inactive,
                    control =
                        SpecimenFamily.Control.Select(
                            mode,
                        ),
                ),
            )

        assertEquals(
            SpecimenFamily.State.Active,
            act.admittedState,
        )

        val effect =
            act.effect as
                SpecimenFamily.Effect.Persist

        assertTrue(
            effect.mode ===
                mode,
        )

        assertEquals(
            SpecimenFamily.DisplayState.Active,
            family
                .projectionFor(
                    SpecimenFamily.State.Active,
                )
                .displayState,
        )
    }

    @Test
    fun sameStateSelectionMayStillAuthorAConsequence() {
        val family =
            SpecimenFamily()
        val mode =
            SpecimenFamily.Parameter.Mode(
                active = true,
            )

        val act =
            assertNotNull(
                family.resolve(
                    currentState =
                        SpecimenFamily.State.Active,
                    control =
                        SpecimenFamily.Control.Select(
                            mode,
                        ),
                ),
            )

        assertNull(
            act.admittedState,
        )
        assertEquals(
            mode,
            (
                act.effect as
                    SpecimenFamily.Effect.Persist
            ).mode,
        )
    }

    private class SpecimenFamily :
        VexFamilyContract<
            SpecimenFamily,
            SpecimenFamily.State,
            SpecimenFamily.Control,
            SpecimenFamily.Effect
        >() {
        sealed interface Parameter :
            VexFamilyContract.Parameter<SpecimenFamily> {
            data class Mode(
                val active: Boolean,
            ) : Parameter
        }

        enum class State :
            VexFamilyContract.State<SpecimenFamily> {
            Inactive,
            Active,
        }

        sealed interface Control :
            VexFamilyContract.Control<SpecimenFamily> {
            data class Select(
                val mode: Parameter.Mode,
            ) : Control
        }

        sealed interface Effect :
            VexFamilyContract.Effect<SpecimenFamily> {
            data class Persist(
                val mode: Parameter.Mode,
            ) : Effect
        }

        sealed interface DisplayState :
            VexFamilyContract.DisplayState<
                SpecimenFamily,
                State
            > {
            data object Inactive :
                DisplayState

            data object Active :
                DisplayState
        }

        override fun act(
            currentState: State,
            control: Control,
        ): Act<
            SpecimenFamily,
            State,
            Effect
        > =
            when (
                control
            ) {
                is Control.Select -> {
                    val effect =
                        Effect.Persist(
                            mode =
                                control.mode,
                        )
                    val target =
                        if (
                            control.mode.active
                        ) {
                            State.Active
                        } else {
                            State.Inactive
                        }

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
    }
}

// [VXG RealForever]
