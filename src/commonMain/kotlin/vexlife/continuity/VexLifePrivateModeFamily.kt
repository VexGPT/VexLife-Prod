package vexlife.continuity

import kotlinx.serialization.Serializable
import vexlife.semantic.VexFamilyContract

/**
 * Product semantic owner for whether the conversation with Vex is being kept.
 *
 * Outside Private mode everything the person and Vex say is kept in the person's Vex Home, because
 * continuity depends on it. Private mode is a conversation that is never written to disk: it lives
 * only in the running service and is gone when the person leaves it, apart from notes the person
 * and Vex both agree to keep. Vex still draws on what it already knew. Leaving records only that a
 * private conversation happened (when, how many exchanges) and the agreed notes, so Vex can be told
 * there is a gap in what it remembers and why.
 */
object VexLifePrivateModeFamily :
    VexFamilyContract<
        VexLifePrivateModeFamily,
        VexLifePrivateModeFamily.State,
        VexLifePrivateModeFamily.Control,
        VexLifePrivateModeFamily.Effect,
    >() {
    sealed interface State : VexFamilyContract.State<VexLifePrivateModeFamily> {
        val stateRef: String
    }

    /** The ordinary conversation, kept in the person's archives. */
    data object Kept : State {
        override val stateRef: String = StateKept
    }

    data class Private(val exchangeCount: Int, val keptNoteCount: Int) : State {
        init {
            require(exchangeCount >= 0 && keptNoteCount >= 0) {
                "private counts must be nonnegative"
            }
        }

        override val stateRef: String = StatePrivate
    }

    sealed interface Control : VexFamilyContract.Control<VexLifePrivateModeFamily> {
        val controlRef: String
    }

    data object Begin : Control {
        override val controlRef: String = ControlBegin
    }

    data object End : Control {
        override val controlRef: String = ControlEnd
    }

    sealed interface Effect : VexFamilyContract.Effect<VexLifePrivateModeFamily> {
        /** Open an in-memory private conversation. Nothing is written. */
        data object BeginPrivate : Effect

        /** Discard the private transcript and record only the interval and the agreed notes. */
        data object EndPrivate : Effect
    }

    @Serializable
    data class DisplayState(
        val label: String,
        val detail: String,
        val primaryControlRef: String,
        val primaryLabel: String,
        val private: Boolean,
    ) : VexFamilyContract.DisplayState<VexLifePrivateModeFamily, State> {
        init {
            require(label.isNotBlank() && detail.isNotBlank() && primaryLabel.isNotBlank()) {
                "Private mode DisplayState is invalid"
            }
        }
    }

    override fun act(
        currentState: State,
        control: Control,
    ): Act<VexLifePrivateModeFamily, State, Effect>? =
        when {
            currentState == Kept && control == Begin -> stay(Effect.BeginPrivate)
            currentState is Private && control == End -> stay(Effect.EndPrivate)
            else -> null
        }

    override fun displayStateFor(state: State): DisplayState =
        when (state) {
            Kept ->
                DisplayState(
                    label = "Kept in your archives",
                    detail =
                        "Everything you and Vex say here is kept in your Vex Home. Go private for a conversation that isn't kept.",
                    primaryControlRef = ControlBegin,
                    primaryLabel = "Go private",
                    private = false,
                )

            is Private ->
                DisplayState(
                    label = "Private",
                    detail =
                        "Nothing from this conversation is kept after you leave, unless you and Vex agree to keep a note. " +
                            "Vex still remembers what it already knew.",
                    primaryControlRef = ControlEnd,
                    primaryLabel = "Leave private mode",
                    private = true,
                )
        }

    fun controlForRef(controlRef: String): Control? =
        when (controlRef) {
            ControlBegin -> Begin
            ControlEnd -> End
            else -> null
        }

    const val StateKept = "KEPT"
    const val StatePrivate = "PRIVATE"
    const val ControlBegin = "BEGIN"
    const val ControlEnd = "END"
}

// [VXG RealForever]
