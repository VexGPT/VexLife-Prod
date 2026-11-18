package vexlife.person

import kotlin.test.Test
import kotlin.test.assertEquals
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
}

// [VXG RealForever]
