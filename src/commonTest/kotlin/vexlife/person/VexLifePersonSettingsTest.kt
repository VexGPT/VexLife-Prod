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
    }
}

// [VXG RealForever]
