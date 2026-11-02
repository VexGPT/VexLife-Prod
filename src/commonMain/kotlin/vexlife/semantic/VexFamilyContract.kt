package vexlife.semantic

/**
 * Compiler-visible semantic relationship for one exact Family vocabulary.
 *
 * This P0 contract owns no live state holder, execution, Store/provider access, logging,
 * dependency construction, or UI rendering. It answers only how one typed Control relates to an
 * exact State, what immediate semantic State may be admitted, what Effect may be requested, and
 * how the State is projected outward for presentation.
 */
abstract class VexFamilyContract<
    Family,
    FamilyState,
    FamilyControl,
    FamilyEffect,
> where
    Family : VexFamilyContract<Family, FamilyState, FamilyControl, FamilyEffect>,
    FamilyState : VexFamilyContract.State<Family>,
    FamilyControl : VexFamilyContract.Control<Family>,
    FamilyEffect : VexFamilyContract.Effect<Family> {

    interface Parameter<Family>

    interface State<Family>

    interface Control<Family>

    interface Effect<Family>

    interface DisplayState<Family, State> where State : VexFamilyContract.State<Family> {
        data class StateOnly<Family, State>(
            val state: State,
        ) : DisplayState<Family, State> where State : VexFamilyContract.State<Family>
    }

    data class Projection<Family, State, out Display>(
        val state: State,
        val displayState: Display,
    ) where
        State : VexFamilyContract.State<Family>,
        Display : DisplayState<Family, State>

    data class Act<Family, State, Effect>(
        val admittedState: State?,
        val effect: Effect?,
    ) where
        State : VexFamilyContract.State<Family>,
        Effect : VexFamilyContract.Effect<Family>

    /** Pure semantic interpretation. No runtime effect may occur here. */
    protected abstract fun act(
        currentState: FamilyState,
        control: FamilyControl,
    ): Act<Family, FamilyState, FamilyEffect>?

    fun resolve(
        currentState: FamilyState,
        control: FamilyControl,
    ): Act<Family, FamilyState, FamilyEffect>? =
        act(
            currentState = currentState,
            control = control,
        )

    protected fun stay(
        effect: FamilyEffect? = null,
    ): Act<Family, FamilyState, FamilyEffect> =
        Act(
            admittedState = null,
            effect = effect,
        )

    protected fun admit(
        state: FamilyState,
        effect: FamilyEffect? = null,
    ): Act<Family, FamilyState, FamilyEffect> =
        Act(
            admittedState = state,
            effect = effect,
        )

    open fun displayStateFor(
        state: FamilyState,
    ): DisplayState<Family, FamilyState> =
        DisplayState.StateOnly<Family, FamilyState>(
            state = state,
        )

    open fun projectionFor(
        state: FamilyState,
    ): Projection<Family, FamilyState, DisplayState<Family, FamilyState>> =
        Projection(
            state = state,
            displayState = displayStateFor(state),
        )
}

// [VXG RealForever]
