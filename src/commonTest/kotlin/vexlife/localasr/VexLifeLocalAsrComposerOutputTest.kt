package vexlife.localasr

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VexLifeLocalAsrComposerOutputTest {
    @Test
    fun asSpokenWorksForEverySpeechModelButTranslationRequiresMultilingual() {
        assertTrue(
            VexLifeLocalAsrComposerOutput.supportedBy(
                modelRef =
                    VexLifeLocalAsrSelection.WhisperBaseEnglishModelRef,
                mode =
                    VexLifeLocalAsrComposerOutput.AsSpoken,
            ),
        )
        assertTrue(
            VexLifeLocalAsrComposerOutput.supportedBy(
                modelRef =
                    VexLifeLocalAsrSelection.WhisperBaseMultilingualModelRef,
                mode =
                    VexLifeLocalAsrComposerOutput.AsSpoken,
            ),
        )
        assertFalse(
            VexLifeLocalAsrComposerOutput.supportedBy(
                modelRef =
                    VexLifeLocalAsrSelection.WhisperBaseEnglishModelRef,
                mode =
                    VexLifeLocalAsrComposerOutput.TranslateToEnglish,
            ),
        )
        assertTrue(
            VexLifeLocalAsrComposerOutput.supportedBy(
                modelRef =
                    VexLifeLocalAsrSelection.WhisperBaseMultilingualModelRef,
                mode =
                    VexLifeLocalAsrComposerOutput.TranslateToEnglish,
            ),
        )
    }
}

// [VXG RealForever]
