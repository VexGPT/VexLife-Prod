package vexlife.person

import kotlinx.serialization.Serializable
import vexlife.semantic.VexFamilyContract

/**
 * Product semantic owner for the person's two language choices and how they move.
 *
 * ~~~text
 * reply language    what Vex replies in (Vex is told it; Speech and dictation follow it)
 * screen language   what VexLife's own screens are written in
 * ~~~
 *
 * The law this Family exists to hold: choosing one never moves the other. Until the person chooses
 * a screen language the screens follow the reply language ([FollowingReplies]); once chosen it is
 * the person's own ([ScreenChosen]) until they hand it back ([FollowReplyLanguage]). Which screen
 * languages are complete is catalog truth the service checks before a [ChooseScreenLanguage]
 * reaches this Family; here a screen language only has to be a screen language.
 */
object VexLifeLanguageFamily :
    VexFamilyContract<
        VexLifeLanguageFamily,
        VexLifeLanguageFamily.State,
        VexLifeLanguageFamily.Control,
        VexLifeLanguageFamily.Effect,
    >() {
    sealed interface State : VexFamilyContract.State<VexLifeLanguageFamily> {
        val stateRef: String
        val replyLanguage: String
    }

    /** The screens read in the screen language the reply language implies. */
    data class FollowingReplies(override val replyLanguage: String) : State {
        init {
            VexLifePersonSettings.replyLanguageName(replyLanguage)
        }

        override val stateRef: String = StateFollowingReplies
    }

    /** The person chose the screen language; it stays theirs whatever Vex replies in. */
    data class ScreenChosen(override val replyLanguage: String, val screenLanguage: String) : State {
        init {
            VexLifePersonSettings.replyLanguageName(replyLanguage)
            VexLifePersonSettings.screenLanguageName(screenLanguage)
        }

        override val stateRef: String = StateScreenChosen
    }

    sealed interface Control : VexFamilyContract.Control<VexLifeLanguageFamily> {
        val controlRef: String
    }

    data class ChooseReplyLanguage(val code: String) : Control {
        init {
            VexLifePersonSettings.replyLanguageName(code)
        }

        override val controlRef: String = ControlChooseReplyLanguage
    }

    data class ChooseScreenLanguage(val code: String) : Control {
        init {
            VexLifePersonSettings.screenLanguageName(code)
        }

        override val controlRef: String = ControlChooseScreenLanguage
    }

    /** Hand the screens back to the reply language. */
    data object FollowReplyLanguage : Control {
        override val controlRef: String = ControlFollowReplyLanguage
    }

    sealed interface Effect : VexFamilyContract.Effect<VexLifeLanguageFamily> {
        /** Write the admitted choices to the person's settings in their Vex Home. */
        data object SaveChoices : Effect
    }

    /** What the choices are; the words for them come from catalog keys, not from here. */
    @Serializable
    data class DisplayState(
        val replyLanguage: String,
        val screenLanguage: String,
        val followsReplies: Boolean,
    ) : VexFamilyContract.DisplayState<VexLifeLanguageFamily, State>

    override fun act(
        currentState: State,
        control: Control,
    ): Act<VexLifeLanguageFamily, State, Effect>? =
        when (control) {
            is ChooseReplyLanguage ->
                if (control.code == currentState.replyLanguage) {
                    stay()
                } else {
                    admit(
                        when (currentState) {
                            is FollowingReplies -> FollowingReplies(control.code)
                            is ScreenChosen -> currentState.copy(replyLanguage = control.code)
                        },
                        Effect.SaveChoices,
                    )
                }
            is ChooseScreenLanguage ->
                when (currentState) {
                    is ScreenChosen ->
                        if (control.code == currentState.screenLanguage) {
                            stay()
                        } else {
                            admit(currentState.copy(screenLanguage = control.code), Effect.SaveChoices)
                        }
                    is FollowingReplies ->
                        admit(ScreenChosen(currentState.replyLanguage, control.code), Effect.SaveChoices)
                }
            FollowReplyLanguage ->
                when (currentState) {
                    is FollowingReplies -> stay()
                    is ScreenChosen -> admit(FollowingReplies(currentState.replyLanguage), Effect.SaveChoices)
                }
        }

    override fun displayStateFor(state: State): DisplayState =
        DisplayState(
            replyLanguage = VexLifePersonSettings.canonicalReplyLanguage(state.replyLanguage),
            screenLanguage = VexLifePersonSettings.screenLanguageFor(valueFor(state)),
            followsReplies = state is FollowingReplies,
        )

    /** The settings value in the Home, read as this Family's State. */
    fun stateFor(value: VexLifePersonSettings.Value): State =
        value.screenLanguage?.let { ScreenChosen(value.replyLanguage, it) } ?: FollowingReplies(value.replyLanguage)

    /** The State, as the settings value the Home keeps. */
    fun valueFor(state: State): VexLifePersonSettings.Value =
        VexLifePersonSettings.Value(
            VexLifePersonSettings.Schema,
            replyLanguage = state.replyLanguage,
            screenLanguage = (state as? ScreenChosen)?.screenLanguage,
        )

    const val StateFollowingReplies = "FOLLOWING_REPLIES"
    const val StateScreenChosen = "SCREEN_CHOSEN"
    const val ControlChooseReplyLanguage = "CHOOSE_REPLY_LANGUAGE"
    const val ControlChooseScreenLanguage = "CHOOSE_SCREEN_LANGUAGE"
    const val ControlFollowReplyLanguage = "FOLLOW_REPLY_LANGUAGE"

    val StateRefs: Set<String> = setOf(StateFollowingReplies, StateScreenChosen)
    val ControlRefs: Set<String> = setOf(ControlChooseReplyLanguage, ControlChooseScreenLanguage, ControlFollowReplyLanguage)
}

// [VXG RealForever]
