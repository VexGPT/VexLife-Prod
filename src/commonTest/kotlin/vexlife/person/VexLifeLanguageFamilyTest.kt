package vexlife.person

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class VexLifeLanguageFamilyTest {
    private val family = VexLifeLanguageFamily
    private val states =
        VexLifePersonSettings.ReplyLanguages.flatMap { (reply, _) ->
            listOf<VexLifeLanguageFamily.State>(VexLifeLanguageFamily.FollowingReplies(reply)) +
                VexLifePersonSettings.ScreenLanguages.map { (screen, _) -> VexLifeLanguageFamily.ScreenChosen(reply, screen) }
        }
    private val controls =
        VexLifePersonSettings.ReplyLanguages.map { VexLifeLanguageFamily.ChooseReplyLanguage(it.first) } +
            VexLifePersonSettings.ScreenLanguages.map { VexLifeLanguageFamily.ChooseScreenLanguage(it.first) } +
            VexLifeLanguageFamily.FollowReplyLanguage

    private fun after(state: VexLifeLanguageFamily.State, control: VexLifeLanguageFamily.Control): VexLifeLanguageFamily.State {
        val act = assertNotNull(family.resolve(state, control), "$state x $control is lawful")
        return act.admittedState?.also { assertEquals(VexLifeLanguageFamily.Effect.SaveChoices, act.effect) } ?: state.also { assertEquals(null, act.effect) }
    }

    private fun screen(state: VexLifeLanguageFamily.State) = (state as? VexLifeLanguageFamily.ScreenChosen)?.screenLanguage

    @Test
    fun choosingOneLanguageNeverMovesTheOtherInAnyState() {
        for (state in states) {
            for (control in controls) {
                val next = after(state, control)
                when (control) {
                    is VexLifeLanguageFamily.ChooseReplyLanguage -> {
                        assertEquals(control.code, next.replyLanguage)
                        assertEquals(screen(state), screen(next), "$state x $control moved the screen choice")
                        assertEquals(state::class, next::class, "following or chosen is unchanged by a reply choice")
                    }
                    is VexLifeLanguageFamily.ChooseScreenLanguage -> {
                        assertEquals(control.code, screen(next))
                        assertEquals(state.replyLanguage, next.replyLanguage, "$state x $control moved the reply choice")
                    }
                    VexLifeLanguageFamily.FollowReplyLanguage -> {
                        assertEquals(VexLifeLanguageFamily.FollowingReplies(state.replyLanguage), next)
                    }
                }
            }
        }
    }

    @Test
    fun aRepeatedChoiceStaysAndSavesNothing() {
        val chosen = VexLifeLanguageFamily.ScreenChosen("ja", "fr")
        assertEquals(null, family.resolve(chosen, VexLifeLanguageFamily.ChooseReplyLanguage("ja"))!!.effect)
        assertEquals(null, family.resolve(chosen, VexLifeLanguageFamily.ChooseScreenLanguage("fr"))!!.effect)
        assertEquals(null, family.resolve(VexLifeLanguageFamily.FollowingReplies("ja"), VexLifeLanguageFamily.FollowReplyLanguage)!!.effect)
    }

    @Test
    fun theHomeValueAndTheStateAreTheSameTruth() {
        for (state in states) assertEquals(state, family.stateFor(family.valueFor(state)))
        val following = family.displayStateFor(VexLifeLanguageFamily.FollowingReplies(VexLifePersonSettings.LegacyChinese))
        assertEquals(VexLifePersonSettings.TraditionalChinese, following.replyLanguage)
        assertEquals(VexLifePersonSettings.TraditionalChinese, following.screenLanguage, "legacy zh stays Traditional on screen too")
        assertEquals(true, following.followsReplies)
        assertEquals(VexLifePersonSettings.FollowReplyLanguage, VexLifeLanguageFamily.ControlFollowReplyLanguage)
    }
}

// [VXG RealForever]
