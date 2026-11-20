package vexlife.vision

import kotlinx.serialization.Serializable
import vexlife.semantic.VexFamilyContract
import vexlife.text.VexLifeText

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

    enum class HoldReason(
        val reasonRef: String,
    ) {
        /** The Product service carries no Vision runtime child. */
        RouteUnreachable(
            "ROUTE_UNREACHABLE",
        ),

        /** The runtime answered but its current-state answer could not be read. */
        RuntimeUnreadable(
            "RUNTIME_UNREADABLE",
        ),

        /** The SDK holds the selected scope because Screen safety is not current. */
        ScreenSafetyNotCurrent(
            "SCREEN_SAFETY_NOT_CURRENT",
        ),
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
        /** Accessible summary for the Vision control; composed from keyed nested parts. */
        val label: VexLifeText,
        /** The state word rendered beside the stable Vision label (Held / Ready / ...). */
        val stateLabel: VexLifeText,
        val detail: VexLifeText,
        val primaryControlRef: String?,
        val primaryLabel: VexLifeText?,
        val indicatorRef: String,
    ) : VexFamilyContract.DisplayState<VexLifeVisionFamily, State> {
        init {
            require(
                (primaryControlRef == null) == (primaryLabel == null) &&
                    indicatorRef in Indicator.entries.map { it.name }
            ) {
                "Vision DisplayState is invalid"
            }
        }
    }

    /**
     * The SDK selected-scope projection state, decoded once where the wire string arrives (the
     * service) so Family law branches on a type, never on the string.
     */
    enum class ScopeState(val wireValue: String?) {
        Held("HELD"),
        Selecting("SELECTING"),
        Engaged("ENGAGED"),
        Looking("LOOKING"),
        Ended("ENDED"),

        /** A state this Product does not know yet: held, never guessed. */
        Unrecognized(null);

        companion object {
            /** Null when the runtime reports no scope session yet. */
            fun fromWire(value: String?): ScopeState? =
                value?.let { wire -> entries.firstOrNull { it.wireValue == wire } ?: Unrecognized }
        }
    }

    /**
     * Reconcile outside truth into Family State.
     *
     * @param scopeState the decoded SDK selected-scope state, or null when the runtime reports no
     *   scope session yet.
     * @param runtimeReadable false when the runtime's PROJECT answer failed or could not be read.
     */
    fun observe(routeReachable: Boolean, runtimeReadable: Boolean, scopeState: ScopeState?): State =
        when {
            !routeReachable -> Held(HoldReason.RouteUnreachable)
            !runtimeReadable -> Held(HoldReason.RuntimeUnreadable)
            else ->
                when (scopeState) {
                    null,
                    ScopeState.Ended -> Ready
                    ScopeState.Held -> Held(HoldReason.ScreenSafetyNotCurrent)
                    ScopeState.Selecting -> Selecting
                    ScopeState.Engaged -> Engaged
                    ScopeState.Looking -> Looking
                    ScopeState.Unrecognized -> Held(HoldReason.RuntimeUnreadable)
                }
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

    private fun statusSummary(
        stateLabel: VexLifeText,
    ): VexLifeText =
        VexLifeText(
            key = "workspace.status.vision-summary",
            parts =
                mapOf(
                    "vision" to VexLifeText("workspace.label.vision"),
                    "state" to stateLabel,
                ),
        )

    override fun displayStateFor(state: State): DisplayState =
        when (state) {
            is Held -> {
                val stateLabel =
                    VexLifeText(
                        "workspace.status.vision-held",
                    )
                when (state.reason) {
                    HoldReason.RouteUnreachable ->
                        DisplayState(
                            label = statusSummary(stateLabel),
                            stateLabel = stateLabel,
                            detail =
                                VexLifeText(
                                    "workspace.description.vision-held-route-unreachable",
                                ),
                            primaryControlRef = null,
                            primaryLabel = null,
                            indicatorRef = Indicator.None.name,
                        )

                    HoldReason.RuntimeUnreadable ->
                        DisplayState(
                            label = statusSummary(stateLabel),
                            stateLabel = stateLabel,
                            detail =
                                VexLifeText(
                                    "workspace.description.vision-held-runtime-unreadable",
                                ),
                            primaryControlRef = null,
                            primaryLabel = null,
                            indicatorRef = Indicator.None.name,
                        )

                    HoldReason.ScreenSafetyNotCurrent ->
                        DisplayState(
                            label = statusSummary(stateLabel),
                            stateLabel = stateLabel,
                            detail =
                                VexLifeText(
                                    "workspace.description.vision-held-screen-safety-not-current",
                                ),
                            primaryControlRef = ControlStop,
                            primaryLabel =
                                VexLifeText(
                                    "workspace.button.vision-release-window",
                                ),
                            indicatorRef = Indicator.None.name,
                        )
                }
            }

            Ready -> {
                val stateLabel =
                    VexLifeText(
                        "workspace.status.vision-ready",
                    )
                DisplayState(
                    label = statusSummary(stateLabel),
                    stateLabel = stateLabel,
                    detail =
                        VexLifeText(
                            "workspace.description.vision-ready",
                        ),
                    primaryControlRef = ControlEngage,
                    primaryLabel =
                        VexLifeText(
                            "workspace.button.vision-choose-window",
                        ),
                    indicatorRef = Indicator.None.name,
                )
            }

            Selecting -> {
                val stateLabel =
                    VexLifeText(
                        "workspace.status.vision-selecting",
                    )
                DisplayState(
                    label = statusSummary(stateLabel),
                    stateLabel = stateLabel,
                    detail =
                        VexLifeText(
                            "workspace.description.vision-selecting",
                        ),
                    primaryControlRef = null,
                    primaryLabel = null,
                    indicatorRef = Indicator.None.name,
                )
            }

            Engaged -> {
                val stateLabel =
                    VexLifeText(
                        "workspace.status.vision-engaged",
                    )
                DisplayState(
                    label = statusSummary(stateLabel),
                    stateLabel = stateLabel,
                    detail =
                        VexLifeText(
                            "workspace.description.vision-engaged",
                        ),
                    primaryControlRef = ControlStop,
                    primaryLabel =
                        VexLifeText(
                            "workspace.button.vision-release-window",
                        ),
                    indicatorRef = Indicator.Scope.name,
                )
            }

            Looking -> {
                val stateLabel =
                    VexLifeText(
                        "workspace.status.vision-looking",
                    )
                DisplayState(
                    label = statusSummary(stateLabel),
                    stateLabel = stateLabel,
                    detail =
                        VexLifeText(
                            "workspace.description.vision-looking",
                        ),
                    primaryControlRef = null,
                    primaryLabel = null,
                    indicatorRef = Indicator.Looking.name,
                )
            }
        }

    const val StateHeld = "HELD"
    const val StateReady = "READY"
    const val StateSelecting = "SELECTING"
    const val StateEngaged = "ENGAGED"
    const val StateLooking = "LOOKING"

    const val ControlEngage = "ENGAGE"
    const val ControlStop = "STOP"

    val StateRefs: Set<String> =
        setOf(
            StateHeld,
            StateReady,
            StateSelecting,
            StateEngaged,
            StateLooking,
        )

    val ControlRefs: Set<String> =
        setOf(
            ControlEngage,
            ControlStop,
        )

    val HoldReasonRefs: Set<String> =
        HoldReason.entries
            .map(
                HoldReason::reasonRef,
            )
            .toSet()

    fun controlForRef(controlRef: String): Control? =
        when (controlRef) {
            ControlEngage -> Engage
            ControlStop -> Stop
            else -> null
        }
}

// [VXG RealForever]
