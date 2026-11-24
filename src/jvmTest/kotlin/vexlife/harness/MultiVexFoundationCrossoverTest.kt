package vexlife.harness

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import vexlife.companion.DirectConversationSourceReturn
import vexlife.continuity.DirectConversationContextProjection

class MultiVexFoundationCrossoverTest {
    @Test
    fun preservedFirstTurnMessagesAreExactAndDistinct() {
        assertEquals(
            "9abfb09999b4a882b130acba77ab2cf891e8276dd87723556bc1593643251672",
            dynamicCounterpartSha256(MultiVexFoundationCrossoverMessages.B0FirstSarah),
        )
        assertEquals(
            "a7dccb4a6256d3dad1c93b33f8366317c92b2c3424088f078a00e5d62934db5e",
            dynamicCounterpartSha256(MultiVexFoundationCrossoverMessages.B1FirstSarah),
        )
        assertNotEquals(
            MultiVexFoundationCrossoverMessages.B0FirstSarah,
            MultiVexFoundationCrossoverMessages.B1FirstSarah,
        )
    }

    @Test
    fun requestBoundConversationProjectionCueBelongsToExactMessageWording() {
        assertNull(
            DirectConversationContextProjection.cueOrNull(
                MultiVexFoundationCrossoverMessages.B0FirstSarah,
            ),
        )
        assertEquals(
            DirectConversationContextProjection.Cue.EXPLICIT_BACK_REFERENCE,
            DirectConversationContextProjection.cueOrNull(
                MultiVexFoundationCrossoverMessages.B1FirstSarah,
            ),
        )
        assertEquals(
            DirectConversationSourceReturn.HumanSpeaker,
            DirectConversationContextProjection.speakerScope(
                MultiVexFoundationCrossoverMessages.B1FirstSarah,
            ),
        )
    }

    @Test
    fun preservedMessagesRemainBoundToTheirSourceReturns() {
        assertEquals(
            "f6badd010c7b7e1ecf2a3408b0ef3241052341aceaeb346caf22884734503967",
            MultiVexFoundationCrossoverMessages.B0ReturnSha256,
        )
        assertEquals(
            "e1bbc133d345f93e09fb78a8a3e24c80b741e7b31875e9198039c68098d1c9e5",
            MultiVexFoundationCrossoverMessages.B1ReturnSha256,
        )
    }
}

// [VXG RealForever][MULTI-VEX][FOUNDATION-CROSSOVER]
