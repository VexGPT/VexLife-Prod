package vexlife.vision

import kotlinx.serialization.Serializable
import vexlife.semantic.VexFamilyContract

/**
 * Product semantic owner for the human-facing VexLife Vision relationship.
 *
 * This Family owns only the meaning of the Vision control surface. It does not own macOS Screen
 * Recording permission, the native picker, the selected-scope session, physical capture,
 * visual-model input, Conversation Context, Memory, training, or publication.
 *
 * The current Product relationship is the persistent **selected scope**: the person may choose one
 * window through the macOS system picker (Engage) and later release it (Stop). An engaged scope is
 * not capture: nothing is recorded, stored or sent to a model. Look Now is intentionally absent
 * from this vocabulary until the persistent-scope LOOK executor is separately earned; offering it
 * while the runtime holds LOOK would be a control that can never succeed.
 *
 * [observe] is the reconciliation boundary: it interprets the Product route reachability plus the
 * SDK's content-absent selected-scope projection (wire states) into Family State.
 */
object VexLifeVisionFamily :
    VexFamilyContract<
        VexLifeVisionFamily,
        VexLifeVisionFamily.State,
        VexLifeVisionFamily.Control,
        VexLifeVisionFamily.Effect,
    >() {
    sealed interface State : VexFamilyContract.State<VexLifeVisionFamily> {
        val stateRef: String
    }

    enum class HoldReason {
        /** The Product service carries no Vision runtime child. */
        RouteUnreachable,

        /** The runtime answered but its current-state answer could not be read. */
        RuntimeUnreadable,

        /** The SDK holds the selected scope because Screen safety is not current. */
        ScreenSafetyNotCurrent,
    }

    data class Held(val reason: HoldReason) : State {
        override val stateRef: String = StateHeld
    }

    /** Reachable, no selected scope (none yet, or the previous one ended). */
    data object Ready : State {
        override val stateRef: String = StateReady
    }

    /** The macOS picker is open for the person to choose one window. */
    data object Selecting : State {
        override val stateRef: String = StateSelecting
    }

    /** One window is selected. No capture is active. */
    data object Engaged : State {
        override val stateRef: String = StateEngaged
    }

    /**
     * The SDK reports one LOOK in progress. The Product cannot request LOOK yet; this State exists
     * so the Product never misreports SDK truth if another accepted owner starts one.
     */
    data object Looking : State {
        override val stateRef: String = StateLooking
    }

    sealed interface Control : VexFamilyContract.Control<VexLifeVisionFamily> {
        val controlRef: String
    }

    data object Engage : Control {
        override val controlRef: String = ControlEngage
    }

    data object Stop : Control {
        override val controlRef: String = ControlStop
    }

    sealed interface Effect : VexFamilyContract.Effect<VexLifeVisionFamily> {
        /** Ask the SDK to present the system picker for one window; no capture. */
        data object EngageScope : Effect

        /** Release the current (or held) selected scope. */
        data object StopScope : Effect
    }

    enum class Indicator {
        None,

        /** A window is selected; show that Vision has a scope (not that it is capturing). */
        Scope,
        Looking,
    }

    @Serializable
    data class DisplayState(
        val label: String,
        val detail: String,
        val primaryControlRef: String?,
        val primaryLabel: String?,
        val indicatorRef: String,
    ) : VexFamilyContract.DisplayState<VexLifeVisionFamily, State> {
        init {
            require(
                label.isNotBlank() &&
                    detail.isNotBlank() &&
                    (primaryControlRef == null) == (primaryLabel == null) &&
                    indicatorRef in Indicator.entries.map { it.name }
            ) {
                "Vision DisplayState is invalid"
            }
        }
    }

    /**
     * Reconcile outside truth into Family State.
     *
     * @param scopeState the SDK selected-scope projection state (`HELD`, `SELECTING`, `ENGAGED`,
     *   `LOOKING`, `ENDED`), or null when the runtime reports no scope session yet.
     * @param runtimeReadable false when the runtime's PROJECT answer failed or could not be read.
     */
    fun observe(routeReachable: Boolean, runtimeReadable: Boolean, scopeState: String?): State =
        when {
            !routeReachable -> Held(HoldReason.RouteUnreachable)
            !runtimeReadable -> Held(HoldReason.RuntimeUnreadable)
            scopeState == null || scopeState == "ENDED" -> Ready
            scopeState == "HELD" -> Held(HoldReason.ScreenSafetyNotCurrent)
            scopeState == "SELECTING" -> Selecting
            scopeState == "ENGAGED" -> Engaged
            scopeState == "LOOKING" -> Looking
            else -> Held(HoldReason.RuntimeUnreadable)
        }

    override fun act(
        currentState: State,
        control: Control,
    ): Act<VexLifeVisionFamily, State, Effect>? =
        when (control) {
            Engage ->
                when (currentState) {
                    Ready -> stay(Effect.EngageScope)
                    else -> null
                }

            Stop ->
                when (currentState) {
                    Engaged -> stay(Effect.StopScope)
                    // A held scope session can still be released so the person can choose again.
                    is Held ->
                        if (currentState.reason == HoldReason.ScreenSafetyNotCurrent) {
                            stay(Effect.StopScope)
                        } else {
                            null
                        }

                    else -> null
                }
        }

    override fun displayStateFor(state: State): DisplayState =
        when (state) {
            is Held ->
                when (state.reason) {
                    HoldReason.RouteUnreachable ->
                        DisplayState(
                            label = "Vision · Held",
                            detail =
                                "Vision isn't available because VexLife's local Vision runtime isn't running.",
                            primaryControlRef = null,
                            primaryLabel = null,
                            indicatorRef = Indicator.None.name,
                        )

                    HoldReason.RuntimeUnreadable ->
                        DisplayState(
                            label = "Vision · Held",
                            detail =
                                "VexLife couldn't read Vision's current state, so it offers nothing for now.",
                            primaryControlRef = null,
                            primaryLabel = null,
                            indicatorRef = Indicator.None.name,
                        )

                    HoldReason.ScreenSafetyNotCurrent ->
                        DisplayState(
                            label = "Vision · Held",
                            detail =
                                "macOS screen permission for Vision isn't current. Release the window, then " +
                                    "choose it again; macOS may ask you to allow screen recording.",
                            primaryControlRef = ControlStop,
                            primaryLabel = "Release Window",
                            indicatorRef = Indicator.None.name,
                        )
                }

            Ready ->
                DisplayState(
                    label = "Vision · Ready",
                    detail =
                        "Choose one window you'd like Vex to be able to see. macOS shows its own picker. " +
                            "Choosing a window doesn't capture anything; Vex can't look at it yet.",
                    primaryControlRef = ControlEngage,
                    primaryLabel = "Choose a Window…",
                    indicatorRef = Indicator.None.name,
                )

            Selecting ->
                DisplayState(
                    label = "Vision · Choosing",
                    detail =
                        "macOS is showing its window picker. Pick one window there, or cancel.",
                    primaryControlRef = null,
                    primaryLabel = null,
                    indicatorRef = Indicator.None.name,
                )

            Engaged ->
                DisplayState(
                    label = "Vision · Window chosen",
                    detail =
                        "One window is selected for Vision. Nothing is being captured, saved or sent to the " +
                            "model, and Vex can't look at it yet. Release it any time.",
                    primaryControlRef = ControlStop,
                    primaryLabel = "Release Window",
                    indicatorRef = Indicator.Scope.name,
                )

            Looking ->
                DisplayState(
                    label = "Vision · Looking",
                    detail = "Vision is reading the selected window once.",
                    primaryControlRef = null,
                    primaryLabel = null,
                    indicatorRef = Indicator.Looking.name,
                )
        }

    const val StateHeld = "HELD"
    const val StateReady = "READY"
    const val StateSelecting = "SELECTING"
    const val StateEngaged = "ENGAGED"
    const val StateLooking = "LOOKING"

    const val ControlEngage = "ENGAGE"
    const val ControlStop = "STOP"

    fun controlForRef(controlRef: String): Control? =
        when (controlRef) {
            ControlEngage -> Engage
            ControlStop -> Stop
            else -> null
        }
}

// [VXG RealForever]
