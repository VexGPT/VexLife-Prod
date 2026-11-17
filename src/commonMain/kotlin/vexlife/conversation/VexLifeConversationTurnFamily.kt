package vexlife.conversation

import kotlinx.serialization.Serializable
import vexlife.semantic.VexFamilyContract
import vexlife.text.VexLifeText

/**
 * Product semantic owner for the human-facing lifecycle of one direct Companion turn.
 *
 * This Family answers one question only: given currently observed turn-execution truth, which human
 * Control is lawful now and what Effect does that Control request?
 *
 * It does not own Conversation history, request/response persistence, provider HTTP execution,
 * capability reach, failure-recovery receipts, or the persistent Heretic runtime. Those owners
 * realize Effects and return current active-turn truth through [observe].
 */
object VexLifeConversationTurnFamily :
    VexFamilyContract<
        VexLifeConversationTurnFamily,
        VexLifeConversationTurnFamily.State,
        VexLifeConversationTurnFamily.Control,
        VexLifeConversationTurnFamily.Effect,
    >() {

    @Serializable
    data class Attempt(
        val ref: String,
    ) : VexFamilyContract.Parameter<VexLifeConversationTurnFamily> {
        init {
            require(
                ref.matches(
                    Regex(
                        "[A-Za-z0-9][A-Za-z0-9._:-]{0,220}",
                    ),
                ),
            ) {
                "Conversation turn attempt ref must be one portable stable ref"
            }
        }
    }

    sealed interface State : VexFamilyContract.State<VexLifeConversationTurnFamily> {
        val stateRef: String
    }

    data object Ready : State {
        override val stateRef: String = StateReady
    }

    data class Active(
        val attempt: Attempt,
    ) : State {
        override val stateRef: String = StateActive
    }

    data class Cancelling(
        val attempt: Attempt,
    ) : State {
        override val stateRef: String = StateCancelling
    }

    sealed interface Control : VexFamilyContract.Control<VexLifeConversationTurnFamily> {
        val controlRef: String
        val attempt: Attempt
    }

    data class Send(
        override val attempt: Attempt,
    ) : Control {
        override val controlRef: String = ControlSend
    }

    data class Stop(
        override val attempt: Attempt,
    ) : Control {
        override val controlRef: String = ControlStop
    }

    sealed interface Effect : VexFamilyContract.Effect<VexLifeConversationTurnFamily> {
        val attempt: Attempt
    }

    data class BeginTurn(
        override val attempt: Attempt,
    ) : Effect

    data class RequestCancel(
        override val attempt: Attempt,
    ) : Effect

    /**
     * One terminal cancellation reason owned by this semantic relationship. Recovery receipts may
     * serialize this ref, but they do not become the semantic owner merely because they persist it.
     */
    enum class TerminalReason(
        val ref: String,
    ) {
        CancelledByPerson(
            "TURN_CANCELLED_BY_PERSON",
        ),
    }

    enum class Text(
        val key: String,
    ) {
        Send(
            "workspace.button.conversation-send",
        ),
        Stop(
            "workspace.button.conversation-stop",
        ),
        Ready(
            "workspace.status.conversation-ready",
        ),
        Active(
            "workspace.status.conversation-active",
        ),
        Cancelling(
            "workspace.status.conversation-stopping",
        ),
    }

    @Serializable
    data class DisplayState(
        val status: VexLifeText,
        val primaryControlRef: String?,
        val primaryLabel: VexLifeText?,
        /** Required only when the current Control targets one already-active attempt. */
        val targetAttemptRef: String?,
        val active: Boolean,
        val cancelling: Boolean,
    ) : VexFamilyContract.DisplayState<VexLifeConversationTurnFamily, State> {
        init {
            require(
                (primaryControlRef == null) ==
                    (primaryLabel == null),
            ) {
                "Conversation turn DisplayState control and label must be present together"
            }
            require(
                (primaryControlRef == ControlStop) ==
                    (targetAttemptRef != null),
            ) {
                "Conversation turn Stop display must bind the exact active attempt"
            }
        }
    }

    /**
     * Interpret current execution custody into Family State.
     *
     * [activeAttemptRef] comes from the service's one active-turn slot. Cancellation-requested truth
     * comes from that attempt's request-local cancellation token. Neither is manufactured here.
     */
    fun observe(
        activeAttemptRef: String?,
        cancellationRequested: Boolean,
    ): State =
        when {
            activeAttemptRef ==
                null -> {
                require(
                    !cancellationRequested,
                ) {
                    "Conversation turn cannot be cancelling without one active attempt"
                }
                Ready
            }

            cancellationRequested ->
                Cancelling(
                    Attempt(
                        activeAttemptRef,
                    ),
                )

            else ->
                Active(
                    Attempt(
                        activeAttemptRef,
                    ),
                )
        }

    override fun act(
        currentState: State,
        control: Control,
    ): Act<VexLifeConversationTurnFamily, State, Effect>? =
        when (
            currentState
        ) {
            Ready ->
                when (
                    control
                ) {
                    is Send ->
                        stay(
                            BeginTurn(
                                control.attempt,
                            ),
                        )

                    is Stop ->
                        null
                }

            is Active ->
                when (
                    control
                ) {
                    is Send ->
                        null

                    is Stop ->
                        if (
                            control.attempt ==
                                currentState.attempt
                        ) {
                            stay(
                                RequestCancel(
                                    currentState.attempt,
                                ),
                            )
                        } else {
                            null
                        }
                }

            is Cancelling ->
                null
        }

    override fun displayStateFor(
        state: State,
    ): DisplayState =
        when (
            state
        ) {
            Ready ->
                DisplayState(
                    status =
                        VexLifeText(
                            Text.Ready.key,
                        ),
                    primaryControlRef = ControlSend,
                    primaryLabel =
                        VexLifeText(
                            Text.Send.key,
                        ),
                    targetAttemptRef = null,
                    active = false,
                    cancelling = false,
                )

            is Active ->
                DisplayState(
                    status =
                        VexLifeText(
                            Text.Active.key,
                        ),
                    primaryControlRef = ControlStop,
                    primaryLabel =
                        VexLifeText(
                            Text.Stop.key,
                        ),
                    targetAttemptRef = state.attempt.ref,
                    active = true,
                    cancelling = false,
                )

            is Cancelling ->
                DisplayState(
                    status =
                        VexLifeText(
                            Text.Cancelling.key,
                        ),
                    primaryControlRef = null,
                    primaryLabel = null,
                    targetAttemptRef = null,
                    active = true,
                    cancelling = true,
                )
        }

    fun controlForRef(
        controlRef: String,
        attempt: Attempt,
    ): Control? =
        when (
            controlRef
        ) {
            ControlSend ->
                Send(
                    attempt,
                )

            ControlStop ->
                Stop(
                    attempt,
                )

            else ->
                null
        }

    const val StateReady =
        "READY"

    const val StateActive =
        "ACTIVE"

    const val StateCancelling =
        "CANCELLING"

    const val ControlSend =
        "SEND"

    const val ControlStop =
        "STOP"

    val StateRefs =
        setOf(
            StateReady,
            StateActive,
            StateCancelling,
        )

    val ControlRefs =
        setOf(
            ControlSend,
            ControlStop,
        )
}

// [VXG RealForever]
