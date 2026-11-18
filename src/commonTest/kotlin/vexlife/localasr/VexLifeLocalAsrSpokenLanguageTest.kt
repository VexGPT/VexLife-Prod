package vexlife.localasr

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class VexLifeLocalAsrSpokenLanguageTest {
    @Test
    fun microphoneLanguageIsIndependentFromReplyLanguage() {
        assertEquals(
            "ja",
            VexLifeLocalAsrSpokenLanguage.resolvedLanguageCode(
                value =
                    VexLifeLocalAsrSpokenLanguage.Value(
                        schemaVersion =
                            VexLifeLocalAsrSpokenLanguage.Schema,
                        mode =
                            VexLifeLocalAsrSpokenLanguage.Fixed,
                        languageCode =
                            "ja",
                    ),
                replyLanguage =
                    "en",
            ),
        )
        assertEquals(
            "zh",
            VexLifeLocalAsrSpokenLanguage.resolvedLanguageCode(
                value =
                    VexLifeLocalAsrSpokenLanguage.Default,
                replyLanguage =
                    "zh",
            ),
        )
        assertEquals(
            "zh",
            VexLifeLocalAsrSpokenLanguage.resolvedLanguageCode(
                value =
                    VexLifeLocalAsrSpokenLanguage.Default,
                replyLanguage =
                    "zh-Hans",
            ),
        )
        assertEquals(
            "zh",
            VexLifeLocalAsrSpokenLanguage.resolvedLanguageCode(
                value =
                    VexLifeLocalAsrSpokenLanguage.Default,
                replyLanguage =
                    "zh-Hant",
            ),
        )
        assertEquals(
            "auto",
            VexLifeLocalAsrSpokenLanguage.resolvedLanguageCode(
                value =
                    VexLifeLocalAsrSpokenLanguage.Value(
                        schemaVersion =
                            VexLifeLocalAsrSpokenLanguage.Schema,
                        mode =
                            VexLifeLocalAsrSpokenLanguage.AutoDetect,
                    ),
                replyLanguage =
                    "en",
            ),
        )
    }

    @Test
    fun fixedModeRequiresOneSupportedLanguage() {
        assertFailsWith<IllegalArgumentException> {
            VexLifeLocalAsrSpokenLanguage.Value(
                schemaVersion =
                    VexLifeLocalAsrSpokenLanguage.Schema,
                mode =
                    VexLifeLocalAsrSpokenLanguage.Fixed,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            VexLifeLocalAsrSpokenLanguage.Value(
                schemaVersion =
                    VexLifeLocalAsrSpokenLanguage.Schema,
                mode =
                    VexLifeLocalAsrSpokenLanguage.Fixed,
                languageCode =
                    "xx",
            )
        }
    }
}

// [VXG RealForever]
