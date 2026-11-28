package vexlife.person

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class VexLifePersonSettingsTest {
    @Test
    fun replyChineseScriptIsDistinctFromSpokenChinese() {
        assertTrue(
            VexLifePersonSettings.ReplyLanguages.any {
                it.first == VexLifePersonSettings.SimplifiedChinese &&
                    it.second == "简体中文"
            },
        )
        assertTrue(
            VexLifePersonSettings.ReplyLanguages.any {
                it.first == VexLifePersonSettings.TraditionalChinese &&
                    it.second == "繁體中文"
            },
        )
        assertEquals(
            "zh",
            VexLifePersonSettings.spokenLanguageCodeForReply(
                VexLifePersonSettings.SimplifiedChinese,
            ),
        )
        assertEquals(
            "zh",
            VexLifePersonSettings.spokenLanguageCodeForReply(
                VexLifePersonSettings.TraditionalChinese,
            ),
        )
        assertEquals(
            "zh",
            VexLifePersonSettings.localizationLanguageForReply(
                VexLifePersonSettings.SimplifiedChinese,
            ),
        )
        assertEquals(
            VexLifePersonSettings.TraditionalChinese,
            VexLifePersonSettings.canonicalReplyLanguage(
                VexLifePersonSettings.LegacyChinese,
            ),
        )
        // Screens keep the script the person reads: Traditional replies (and legacy `zh` Homes,
        // which stay Traditional) are never shown Simplified screens.
        assertEquals(
            VexLifePersonSettings.TraditionalChinese,
            VexLifePersonSettings.localizationLanguageForReply(VexLifePersonSettings.TraditionalChinese),
        )
        assertEquals(
            VexLifePersonSettings.TraditionalChinese,
            VexLifePersonSettings.localizationLanguageForReply(VexLifePersonSettings.LegacyChinese),
        )
        assertEquals(
            listOf("zh", "zh-Hant"),
            VexLifePersonSettings.ScreenLanguages.map { it.first }.filter { it.startsWith("zh") },
        )
        assertEquals(
            VexLifePersonSettings.Languages.map { it.first },
            VexLifePersonSettings.ScreenLanguages.map { it.first }.filter { it != VexLifePersonSettings.TraditionalChinese },
            "the microphone's spoken languages stay one Chinese",
        )
    }

    @Test
    fun screenLanguageIsItsOwnChoiceAndFollowsTheReplyOnlyUntilChosen() {
        val following = VexLifePersonSettings.Value(VexLifePersonSettings.Schema, VexLifePersonSettings.TraditionalChinese)
        assertEquals(VexLifePersonSettings.TraditionalChinese, VexLifePersonSettings.screenLanguageFor(following))
        val chosen = following.copy(screenLanguage = "fr")
        assertEquals("fr", VexLifePersonSettings.screenLanguageFor(chosen))
        assertEquals(VexLifePersonSettings.TraditionalChinese, chosen.replyLanguage, "choosing screens never moves replies")
        assertEquals("ja", VexLifePersonSettings.screenLanguageFor(chosen.copy(replyLanguage = "ja", screenLanguage = null)))
        // A screen language is a screen language: reply-only codes are not screen choices.
        assertFailsWith<IllegalArgumentException> { following.copy(screenLanguage = VexLifePersonSettings.SimplifiedChinese) }
        assertFailsWith<IllegalArgumentException> { following.copy(screenLanguage = "xx") }
    }

    @Test
    fun aScreenLanguageSeedsAMatchingReplyLanguageWithoutTheLegacyChineseTrap() {
        // Catalog `zh` is Simplified; a stored `zh` reply would mean Traditional, so seeding never stores it.
        assertEquals(VexLifePersonSettings.SimplifiedChinese, VexLifePersonSettings.replyLanguageForScreen(VexLifePersonSettings.LegacyChinese))
        assertEquals(VexLifePersonSettings.TraditionalChinese, VexLifePersonSettings.replyLanguageForScreen(VexLifePersonSettings.TraditionalChinese))
        assertEquals("ja", VexLifePersonSettings.replyLanguageForScreen("ja"))
        assertEquals(
            VexLifePersonSettings.ScreenLanguages.map { it.first },
            VexLifePersonSettings.ScreenLanguages.map {
                VexLifePersonSettings.localizationLanguageForReply(VexLifePersonSettings.replyLanguageForScreen(it.first))
            },
            "a seeded reply language implies the screen language it was seeded from",
        )
    }
}
