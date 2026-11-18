package vexlife.voiceinput

import kotlinx.serialization.Serializable
import vexlife.semantic.VexFamilyContract
import vexlife.text.VexLifeText

/**
 * Product semantic owner for the human-facing VoiceInput relationship.
 *
 * VoiceInput means "use the person's microphone to edit the existing Composer draft by local
 * dictation." It does not create a second Conversation payload type, commit a Send, open Vex's
 * Perception microphone channel, retain raw audio, or decide browser permission/runtime mechanics.
 *
 * Microphone APIs and the selected local speech recognizer are outside truth. Their concrete
 * provider/runtime facts are interpreted at their rightful boundaries into the typed State / reason
 * vocabulary below. The Family owns what Start and Stop mean against that exact State and which
 * language-free [VexLifeText] keys reach presentation.
 */
object VexLifeVoiceInputFamily :
    VexFamilyContract<
        VexLifeVoiceInputFamily,
        VexLifeVoiceInputFamily.State,
        VexLifeVoiceInputFamily.Control,
        VexLifeVoiceInputFamily.Effect,
    >() {

    sealed interface State : VexFamilyContract.State<VexLifeVoiceInputFamily> {
        val stateRef: String
    }

    data object Ready : State {
        override val stateRef: String = StateReady
    }

    data object RequestingMicrophone : State {
        override val stateRef: String = StateRequestingMicrophone
    }

    data object Listening : State {
        override val stateRef: String = StateListening
    }

    data object Stopping : State {
        override val stateRef: String = StateStopping
    }

    data class Held(val reason: HoldReason) : State {
        override val stateRef: String = StateHeld
    }

    data class Failed(
        val reason: FailureReason,
    ) : State {
        override val stateRef: String = StateFailed
    }

    enum class HoldReason(
        val reasonRef: String,
        val statusText: Text,
    ) {
        LocalRecognitionUnavailable(
            "LOCAL_RECOGNITION_UNAVAILABLE",
            Text.HeldLocalRecognition,
        ),
        MicrophoneApiUnavailable(
            "MICROPHONE_API_UNAVAILABLE",
            Text.HeldMicrophoneApi,
        ),
        AudioFeedbackUnavailable(
            "AUDIO_FEEDBACK_UNAVAILABLE",
            Text.HeldAudioFeedback,
        ),
    }

    enum class FailureReason(
        val reasonRef: String,
        val statusText: Text,
    ) {
        MicrophonePermissionRequired(
            "MICROPHONE_PERMISSION_REQUIRED",
            Text.FailedPermission,
        ),
        MicrophoneUnavailable(
            "MICROPHONE_UNAVAILABLE",
            Text.FailedMicrophone,
        ),
        RecognitionStopped(
            "RECOGNITION_STOPPED",
            Text.FailedRecognition,
        ),
        Unknown(
            "UNKNOWN",
            Text.FailedUnknown,
        ),
    }

    sealed interface Control : VexFamilyContract.Control<VexLifeVoiceInputFamily> {
        val controlRef: String
    }

    data object Start : Control {
        override val controlRef: String = ControlStart
    }

    data object Stop : Control {
        override val controlRef: String = ControlStop
    }

    sealed interface Effect : VexFamilyContract.Effect<VexLifeVoiceInputFamily> {
        val effectRef: String
    }

    data object BeginLocalDictation : Effect {
        override val effectRef: String = EffectBeginLocalDictation
    }

    data object EndLocalDictation : Effect {
        override val effectRef: String = EffectEndLocalDictation
    }

    @Serializable
    enum class Attention {
        None,
        Overlay,
    }

    /**
     * Human-facing meaning only. Words are resolved later from [status] / [primaryLabel] keys.
     */
    @Serializable
    data class DisplayState(
        val status: VexLifeText,
        val primaryControlRef: String?,
        val primaryLabel: VexLifeText?,
        val active: Boolean,
        val attention: Attention,
    ) : VexFamilyContract.DisplayState<VexLifeVoiceInputFamily, State> {
        init {
            require((primaryControlRef == null) == (primaryLabel == null)) {
                "VoiceInput DisplayState control and label must be present together"
            }
        }
    }

    /** Key identities are grouped here; renderer source never owns their English words. */
    enum class Text(val key: String) {
        Start("workspace.button.voice-input-start"),
        Stop("workspace.button.voice-input-stop"),
        Ready("workspace.status.voice-input-ready"),
        RequestingMicrophone("workspace.status.voice-input-requesting-microphone"),
        Listening("workspace.status.voice-input-listening"),
        Stopping("workspace.status.voice-input-stopping"),
        HeldLocalRecognition("workspace.status.voice-input-held-local-recognition"),
        HeldMicrophoneApi("workspace.status.voice-input-held-microphone-api"),
        HeldAudioFeedback("workspace.status.voice-input-held-audio-feedback"),
        FailedPermission("workspace.status.voice-input-failed-permission"),
        FailedMicrophone("workspace.status.voice-input-failed-microphone"),
        FailedRecognition("workspace.status.voice-input-failed-recognition"),
        FailedUnknown("workspace.status.voice-input-failed-unknown"),
    }

    /**
     * Interpret current local-dictation capability facts without performing any realization effect.
     *
     * Spoken-language selection is owned separately by Local ASR. This Family only needs the
     * already-reconciled fact that the selected local recognizer is usable for the current session.
     */
    fun observeCapability(
        localRecognitionAvailable: Boolean,
        microphoneApiAvailable: Boolean,
        audioFeedbackAvailable: Boolean,
    ): State =
        when {
            !localRecognitionAvailable -> Held(HoldReason.LocalRecognitionUnavailable)
            !microphoneApiAvailable -> Held(HoldReason.MicrophoneApiUnavailable)
            !audioFeedbackAvailable -> Held(HoldReason.AudioFeedbackUnavailable)
            else -> Ready
        }

    /** The selected local recognition realization actually began the microphone session. */
    fun observeRecognitionStarted(): State = Listening

    /** Browser/provider failure crossed the adapter boundary into one Product failure reason. */
    fun observeFailure(
        reason: FailureReason,
    ): State =
        Failed(reason)

    override fun act(
        currentState: State,
        control: Control,
    ): Act<VexLifeVoiceInputFamily, State, Effect>? =
        when (currentState) {
            Ready ->
                when (control) {
                    Start ->
                        admit(
                            RequestingMicrophone,
                            BeginLocalDictation,
                        )
                    Stop -> null
                }

            is Failed ->
                when (control) {
                    Start ->
                        admit(
                            RequestingMicrophone,
                            BeginLocalDictation,
                        )
                    Stop -> null
                }

            RequestingMicrophone,
            Listening,
            ->
                when (control) {
                    Start -> null
                    Stop ->
                        admit(
                            Stopping,
                            EndLocalDictation,
                        )
                }

            Stopping,
            is Held,
            -> null
        }

    override fun displayStateFor(state: State): DisplayState =
        when (state) {
            Ready ->
                display(
                    status = Text.Ready,
                    primary = Start to Text.Start,
                )

            RequestingMicrophone ->
                display(
                    status = Text.RequestingMicrophone,
                    primary = Stop to Text.Stop,
                    attention = Attention.Overlay,
                )

            Listening ->
                display(
                    status = Text.Listening,
                    primary = Stop to Text.Stop,
                    active = true,
                    attention = Attention.Overlay,
                )

            Stopping ->
                display(
                    status = Text.Stopping,
                    attention = Attention.Overlay,
                )

            is Held ->
                display(
                    status = state.reason.statusText,
                )

            is Failed ->
                display(
                    status = state.reason.statusText,
                    primary = Start to Text.Start,
                    attention = Attention.Overlay,
                )
        }

    private fun display(
        status: Text,
        primary: Pair<Control, Text>? = null,
        active: Boolean = false,
        attention: Attention = Attention.None,
    ): DisplayState =
        DisplayState(
            status = VexLifeText(status.key),
            primaryControlRef = primary?.first?.controlRef,
            primaryLabel = primary?.second?.let { VexLifeText(it.key) },
            active = active,
            attention = attention,
        )

    const val StateReady = "READY"
    const val StateRequestingMicrophone = "REQUESTING_MICROPHONE"
    const val StateListening = "LISTENING"
    const val StateStopping = "STOPPING"
    const val StateHeld = "HELD"
    const val StateFailed = "FAILED"

    const val ControlStart = "START"
    const val ControlStop = "STOP"

    const val EffectBeginLocalDictation = "BEGIN_LOCAL_DICTATION"
    const val EffectEndLocalDictation = "END_LOCAL_DICTATION"

    val StateRefs: Set<String> =
        setOf(
            StateReady,
            StateRequestingMicrophone,
            StateListening,
            StateStopping,
            StateHeld,
            StateFailed,
        )

    val ControlRefs: Set<String> = setOf(ControlStart, ControlStop)

    val EffectRefs: Set<String> = setOf(EffectBeginLocalDictation, EffectEndLocalDictation)

    val HoldReasonRefs: Set<String> = HoldReason.entries.map { it.reasonRef }.toSet()

    val FailureReasonRefs: Set<String> = FailureReason.entries.map { it.reasonRef }.toSet()

}

// [VXG RealForever]
