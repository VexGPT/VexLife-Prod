package vexlife.voiceinput

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VexLifeVoiceInputFamilyTest {
    @Test
    fun browserCapabilityFactsBecomeOneTypedFamilyState() {
        assertEquals(
            VexLifeVoiceInputFamily.Held(
                VexLifeVoiceInputFamily.HoldReason.LocalRecognitionUnavailable
            ),
            VexLifeVoiceInputFamily.observeCapability(
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
                localRecognitionAvailable = true,
                microphoneApiAvailable = true,
                audioFeedbackAvailable = false,
            ),
        )
        assertEquals(
            VexLifeVoiceInputFamily.Ready,
            VexLifeVoiceInputFamily.observeCapability(
                localRecognitionAvailable = true,
                microphoneApiAvailable = true,
                audioFeedbackAvailable = true,
            ),
        )
    }

    @Test
    fun controlsMeanStartAndStopAgainstExactCurrentState() {
        val begin =
            VexLifeVoiceInputFamily.resolve(
                VexLifeVoiceInputFamily.Ready,
                VexLifeVoiceInputFamily.Start,
            )
        assertEquals(
            VexLifeVoiceInputFamily.RequestingMicrophone,
            begin?.admittedState,
        )
        assertEquals(
            VexLifeVoiceInputFamily.BeginLocalDictation,
            begin?.effect,
        )

        val end =
            VexLifeVoiceInputFamily.resolve(
                VexLifeVoiceInputFamily.Listening,
                VexLifeVoiceInputFamily.Stop,
            )
        assertEquals(
            VexLifeVoiceInputFamily.Stopping,
            end?.admittedState,
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
                VexLifeVoiceInputFamily.Stopping,
                VexLifeVoiceInputFamily.Stop,
            )
        )
    }

    @Test
    fun realizationObservationsReturnThroughFamilyBeforeChangingSemanticState() {
        assertEquals(
            VexLifeVoiceInputFamily.Listening,
            VexLifeVoiceInputFamily.observeRecognitionStarted(),
        )
        assertEquals(
            VexLifeVoiceInputFamily.Failed(
                VexLifeVoiceInputFamily.FailureReason.MicrophoneUnavailable
            ),
            VexLifeVoiceInputFamily.observeFailure(
                VexLifeVoiceInputFamily.FailureReason.MicrophoneUnavailable
            ),
        )
    }

    @Test
    fun displayStateCarriesKeysAndControlRefsRatherThanEnglishWords() {
        val ready =
            VexLifeVoiceInputFamily.displayStateFor(
                VexLifeVoiceInputFamily.Ready
            )
        assertEquals("workspace.status.voice-input-ready", ready.status.key)
        assertEquals(VexLifeVoiceInputFamily.ControlStart, ready.primaryControlRef)
        assertEquals("workspace.button.voice-input-start", ready.primaryLabel?.key)
        assertTrue(!ready.active)

        val listening =
            VexLifeVoiceInputFamily.displayStateFor(
                VexLifeVoiceInputFamily.Listening
            )
        assertEquals("workspace.status.voice-input-listening", listening.status.key)
        assertEquals(VexLifeVoiceInputFamily.ControlStop, listening.primaryControlRef)
        assertEquals("workspace.button.voice-input-stop", listening.primaryLabel?.key)
        assertTrue(listening.active)

        val held =
            VexLifeVoiceInputFamily.displayStateFor(
                VexLifeVoiceInputFamily.Held(
                    VexLifeVoiceInputFamily.HoldReason.MicrophoneApiUnavailable
                )
            )
        assertEquals("workspace.status.voice-input-held-microphone-api", held.status.key)
        assertNull(held.primaryControlRef)
    }

    @Test
    fun refsRemainClosedAndUnique() {
        assertEquals(6, VexLifeVoiceInputFamily.StateRefs.size)
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
