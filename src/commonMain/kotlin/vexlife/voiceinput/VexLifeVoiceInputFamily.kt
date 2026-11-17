package vexlife.voiceinput

import kotlinx.serialization.Serializable
import vexlife.person.VexLifePersonSettings
import vexlife.semantic.VexFamilyContract
import vexlife.text.VexLifeText

/**
 * Product semantic owner for the human-facing VoiceInput relationship.
 *
 * VoiceInput means "use the person's microphone to edit the existing Composer draft by local
 * dictation." It does not create a second Conversation payload type, commit a Send, open Vex's
 * Perception microphone channel, retain raw audio, or decide browser permission/runtime mechanics.
 *
 * Browser APIs are outside truth. Their concrete strings and objects are interpreted at the browser
 * adapter boundary into the typed State / reason vocabulary below. The Family owns what Start and
 * Stop mean against that exact State and which language-free [VexLifeText] keys reach presentation.
 */
object VexLifeVoiceInputFamily :
    VexFamilyContract<
        VexLifeVoiceInputFamily,
        VexLifeVoiceInputFamily.State,
        VexLifeVoiceInputFamily.Control,
        VexLifeVoiceInputFamily.Effect,
    >() {

    /**
     * One explicit relationship between the person's supported reply language and the browser
     * recognition locale. The reply-language owner remains [VexLifePersonSettings].
     */
    enum class Language(
        val personLanguageCode: String,
        val recognitionLocale: String,
    ) {
        English("en", "en-US"),
        Japanese("ja", "ja-JP"),
        Chinese("zh", "zh-CN"),
        Korean("ko", "ko-KR"),
        French("fr", "fr-FR"),
        German("de", "de-DE"),
        Spanish("es", "es-ES"),
        Portuguese("pt", "pt-BR"),
        Arabic("ar", "ar-SA"),
        Hindi("hi", "hi-IN"),
        ;

        companion object {
            fun forPersonLanguage(code: String): Language =
                requireNotNull(entries.firstOrNull { it.personLanguageCode == code }) {
                    "VoiceInput has no recognition locale for supported person language $code"
                }
        }
    }

    sealed interface State : VexFamilyContract.State<VexLifeVoiceInputFamily> {
        val stateRef: String
    }

    data class Ready(val language: Language) : State {
        override val stateRef: String = StateReady
    }

    sealed interface Preparing : State {
        val language: Language
    }

    data class CheckingLanguage(override val language: Language) : Preparing {
        override val stateRef: String = StateCheckingLanguage
    }

    data class InstallingLanguage(override val language: Language) : Preparing {
        override val stateRef: String = StateInstallingLanguage
    }

    data class RequestingMicrophone(override val language: Language) : Preparing {
        override val stateRef: String = StateRequestingMicrophone
    }

    data class Listening(val language: Language) : State {
        override val stateRef: String = StateListening
    }

    data class Stopping(val language: Language) : State {
        override val stateRef: String = StateStopping
    }

    data class Held(val reason: HoldReason) : State {
        override val stateRef: String = StateHeld
    }

    data class Failed(
        val language: Language,
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
        LanguageUnavailable(
            "LANGUAGE_UNAVAILABLE",
            Text.FailedLanguage,
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

    /**
     * Product-level interpretation of the browser's local-language availability observation.
     *
     * The browser adapter owns provider strings such as "available", "downloadable",
     * "downloading", and "unavailable". Those strings do not become Family State. The adapter
     * translates them into this smallest semantic observation before the Family decides whether
     * presentation meaning changes.
     */
    enum class LocalLanguageObservation {
        Available,
        InstallRequired,
        Unavailable,
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

    data class BeginLocalDictation(val language: Language) : Effect {
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
        CheckingLanguage("workspace.status.voice-input-checking-language"),
        InstallingLanguage("workspace.status.voice-input-installing-language"),
        RequestingMicrophone("workspace.status.voice-input-requesting-microphone"),
        Listening("workspace.status.voice-input-listening"),
        Stopping("workspace.status.voice-input-stopping"),
        HeldLocalRecognition("workspace.status.voice-input-held-local-recognition"),
        HeldMicrophoneApi("workspace.status.voice-input-held-microphone-api"),
        HeldAudioFeedback("workspace.status.voice-input-held-audio-feedback"),
        FailedPermission("workspace.status.voice-input-failed-permission"),
        FailedMicrophone("workspace.status.voice-input-failed-microphone"),
        FailedLanguage("workspace.status.voice-input-failed-language"),
        FailedRecognition("workspace.status.voice-input-failed-recognition"),
        FailedUnknown("workspace.status.voice-input-failed-unknown"),
    }

    /**
     * Interpret current browser capability facts without performing any browser effect.
     */
    fun observeCapability(
        personLanguageCode: String,
        localRecognitionAvailable: Boolean,
        microphoneApiAvailable: Boolean,
        audioFeedbackAvailable: Boolean,
    ): State {
        val language = Language.forPersonLanguage(personLanguageCode)
        return when {
            !localRecognitionAvailable -> Held(HoldReason.LocalRecognitionUnavailable)
            !microphoneApiAvailable -> Held(HoldReason.MicrophoneApiUnavailable)
            !audioFeedbackAvailable -> Held(HoldReason.AudioFeedbackUnavailable)
            else -> Ready(language)
        }
    }

    /**
     * Reconcile one browser-owned local-language observation into Product meaning.
     *
     * Available is intentionally a semantic no-op: a successful provider check is permission for
     * the existing BeginLocalDictation realization to continue, not proof of a new Product State.
     */
    fun observeLocalLanguage(
        language: Language,
        observation: LocalLanguageObservation,
    ): State? =
        when (observation) {
            LocalLanguageObservation.Available -> null
            LocalLanguageObservation.InstallRequired -> InstallingLanguage(language)
            LocalLanguageObservation.Unavailable ->
                Failed(
                    language,
                    FailureReason.LanguageUnavailable,
                )
        }

    /** Microphone custody is established and the realization has begun checking local language. */
    fun observeLanguageCheckStarted(
        language: Language,
    ): State =
        CheckingLanguage(language)

    /** The browser recognizer reported that local recognition actually started. */
    fun observeRecognitionStarted(
        language: Language,
    ): State =
        Listening(language)

    /** Browser/provider failure crossed the adapter boundary into one Product failure reason. */
    fun observeFailure(
        language: Language,
        reason: FailureReason,
    ): State =
        Failed(language, reason)

    override fun act(
        currentState: State,
        control: Control,
    ): Act<VexLifeVoiceInputFamily, State, Effect>? =
        when (currentState) {
            is Ready ->
                when (control) {
                    Start ->
                        admit(
                            RequestingMicrophone(currentState.language),
                            BeginLocalDictation(currentState.language),
                        )
                    Stop -> null
                }

            is Failed ->
                when (control) {
                    Start ->
                        admit(
                            RequestingMicrophone(currentState.language),
                            BeginLocalDictation(currentState.language),
                        )
                    Stop -> null
                }

            is Preparing ->
                when (control) {
                    Start -> null
                    Stop ->
                        admit(
                            Stopping(currentState.language),
                            EndLocalDictation,
                        )
                }

            is Listening ->
                when (control) {
                    Start -> null
                    Stop ->
                        admit(
                            Stopping(currentState.language),
                            EndLocalDictation,
                        )
                }

            is Stopping,
            is Held -> null
        }

    override fun displayStateFor(state: State): DisplayState =
        when (state) {
            is Ready ->
                display(
                    status = Text.Ready,
                    primary = Start to Text.Start,
                )

            is CheckingLanguage ->
                display(
                    status = Text.CheckingLanguage,
                    primary = Stop to Text.Stop,
                    attention = Attention.Overlay,
                )

            is InstallingLanguage ->
                display(
                    status = Text.InstallingLanguage,
                    primary = Stop to Text.Stop,
                    attention = Attention.Overlay,
                )

            is RequestingMicrophone ->
                display(
                    status = Text.RequestingMicrophone,
                    primary = Stop to Text.Stop,
                    attention = Attention.Overlay,
                )

            is Listening ->
                display(
                    status = Text.Listening,
                    primary = Stop to Text.Stop,
                    active = true,
                    attention = Attention.Overlay,
                )

            is Stopping ->
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
    const val StateCheckingLanguage = "CHECKING_LANGUAGE"
    const val StateInstallingLanguage = "INSTALLING_LANGUAGE"
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
            StateCheckingLanguage,
            StateInstallingLanguage,
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

    val DefaultLanguage: Language = Language.forPersonLanguage(VexLifePersonSettings.DefaultLanguage)

    init {
        require(
            Language.entries.map { it.personLanguageCode } ==
                VexLifePersonSettings.Languages.map { it.first }
        ) {
            "VoiceInput recognition locales must cover the Product reply-language vocabulary exactly"
        }
    }
}

// [VXG RealForever]
