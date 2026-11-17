package vexlife.voiceinput

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vexlife.person.VexLifePersonSettings

class VexLifeVoiceInputFamilyTest {
    @Test
    fun recognitionLanguagesCoverThePersonLanguageVocabularyExactly() {
        assertEquals(
            VexLifePersonSettings.Languages.map { it.first },
            VexLifeVoiceInputFamily.Language.entries.map { it.personLanguageCode },
        )
        assertEquals(
            VexLifePersonSettings.DefaultLanguage,
            VexLifeVoiceInputFamily.Language.English.personLanguageCode,
        )
    }

    @Test
    fun browserCapabilityFactsBecomeOneTypedFamilyState() {
        assertEquals(
            VexLifeVoiceInputFamily.Held(
                VexLifeVoiceInputFamily.HoldReason.LocalRecognitionUnavailable
            ),
            VexLifeVoiceInputFamily.observeCapability(
                personLanguageCode = "en",
                localRecognitionAvailable = false,
                microphoneApiAvailable = true,
                audioFeedbackAvailable = true,
            ),
        )
        assertEquals(
            VexLifeVoiceInputFamily.Held(
                VexLifeVoiceInputFamily.HoldReason.MicrophoneApiUnavailable
            ),
            VexLifeVoiceInputFamily.observeCapability(
                personLanguageCode = "en",
                localRecognitionAvailable = true,
                microphoneApiAvailable = false,
                audioFeedbackAvailable = true,
            ),
        )
        assertEquals(
            VexLifeVoiceInputFamily.Held(
                VexLifeVoiceInputFamily.HoldReason.AudioFeedbackUnavailable
            ),
            VexLifeVoiceInputFamily.observeCapability(
                personLanguageCode = "en",
                localRecognitionAvailable = true,
                microphoneApiAvailable = true,
                audioFeedbackAvailable = false,
            ),
        )
        assertEquals(
            VexLifeVoiceInputFamily.Ready(VexLifeVoiceInputFamily.Language.Japanese),
            VexLifeVoiceInputFamily.observeCapability(
                personLanguageCode = "ja",
                localRecognitionAvailable = true,
                microphoneApiAvailable = true,
                audioFeedbackAvailable = true,
            ),
        )
    }

    @Test
    fun controlsMeanStartAndStopAgainstExactCurrentState() {
        val language = VexLifeVoiceInputFamily.Language.English
        val begin =
            VexLifeVoiceInputFamily.resolve(
                VexLifeVoiceInputFamily.Ready(language),
                VexLifeVoiceInputFamily.Start,
            )
        assertEquals(
            VexLifeVoiceInputFamily.BeginLocalDictation(language),
            begin?.effect,
        )

        val end =
            VexLifeVoiceInputFamily.resolve(
                VexLifeVoiceInputFamily.Listening(language),
                VexLifeVoiceInputFamily.Stop,
            )
        assertEquals(VexLifeVoiceInputFamily.EndLocalDictation, end?.effect)

        assertNull(
            VexLifeVoiceInputFamily.resolve(
                VexLifeVoiceInputFamily.Held(
                    VexLifeVoiceInputFamily.HoldReason.LocalRecognitionUnavailable
                ),
                VexLifeVoiceInputFamily.Start,
            )
        )
        assertNull(
            VexLifeVoiceInputFamily.resolve(
                VexLifeVoiceInputFamily.Stopping(language),
                VexLifeVoiceInputFamily.Stop,
            )
        )
    }

    @Test
    fun displayStateCarriesKeysAndControlRefsRatherThanEnglishWords() {
        val ready =
            VexLifeVoiceInputFamily.displayStateFor(
                VexLifeVoiceInputFamily.Ready(VexLifeVoiceInputFamily.Language.English)
            )
        assertEquals("workspace.status.voice-input-ready", ready.status.key)
        assertEquals(VexLifeVoiceInputFamily.ControlStart, ready.primaryControlRef)
        assertEquals("workspace.button.voice-input-start", ready.primaryLabel?.key)
        assertTrue(!ready.active)

        val listening =
            VexLifeVoiceInputFamily.displayStateFor(
                VexLifeVoiceInputFamily.Listening(VexLifeVoiceInputFamily.Language.English)
            )
        assertEquals("workspace.status.voice-input-listening", listening.status.key)
        assertEquals(VexLifeVoiceInputFamily.ControlStop, listening.primaryControlRef)
        assertEquals("workspace.button.voice-input-stop", listening.primaryLabel?.key)
        assertTrue(listening.active)

        VexLifeVoiceInputFamily.Text.entries.forEach {
            assertTrue(it.key.startsWith("workspace."))
        }
    }

    @Test
    fun refsRemainClosedAndUnique() {
        assertEquals(8, VexLifeVoiceInputFamily.StateRefs.size)
        assertEquals(2, VexLifeVoiceInputFamily.ControlRefs.size)
        assertEquals(2, VexLifeVoiceInputFamily.EffectRefs.size)
        assertEquals(
            VexLifeVoiceInputFamily.HoldReason.entries.size,
            VexLifeVoiceInputFamily.HoldReasonRefs.size,
        )
        assertEquals(
            VexLifeVoiceInputFamily.FailureReason.entries.size,
            VexLifeVoiceInputFamily.FailureReasonRefs.size,
        )
    }
}

// [VXG RealForever]
