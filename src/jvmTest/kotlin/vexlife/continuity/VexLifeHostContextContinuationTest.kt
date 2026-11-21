package vexlife.continuity

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vexlife.companion.DirectConversationSourceReturn

class VexLifeHostContextContinuationTest {
    @Test
    fun explicitBackwardRecallFormsInspectableLexicalCues() {
        val request =
            "Back to the beginning: what exact project token did I give you?"

        assertEquals(
            VexLifeHostContextContinuation.Cue.EXACT_RECALL,
            VexLifeHostContextContinuation.cueOrNull(request),
        )
        assertTrue(
            "exact project token" in
                VexLifeHostContextContinuation.queryTerms(request),
        )
        assertEquals(
            DirectConversationSourceReturn.HumanSpeaker,
            VexLifeHostContextContinuation.speakerScope(request),
        )
    }

    @Test
    fun continuityPressureRecallKeepsTheDistinctiveSourcePhrase() {
        val request =
            "Without guessing: where was the cobalt lantern? " +
                "Use a continuity/revisit capability if the detail is not actually in view."

        assertNotNull(
            VexLifeHostContextContinuation.cueOrNull(request),
        )
        assertTrue(
            "cobalt lantern" in
                VexLifeHostContextContinuation.queryTerms(request),
        )
    }

    @Test
    fun ordinaryForwardRequestDoesNotCreateAContinuationCue() {
        assertNull(
            VexLifeHostContextContinuation.cueOrNull(
                "What is the weather like today?",
            ),
        )
    }

    @Test
    fun preparationReopensExactCanonicalSourceWithoutWritingAnything() {
        ContinuityTurnFixture().use {
            fixture ->
            val old =
                fixture.commit(
                    ContinuityTurnFixture.Line(
                        human =
                            "Experiment anchor: the cobalt lantern is stored beside the cedar gate.",
                        vex = "recorded",
                        at = Instant.parse("2026-11-20T08:00:00Z"),
                    ),
                    ContinuityTurnFixture.Line(
                        human = "Unrelated later turn.",
                        vex = "later",
                        at = Instant.parse("2026-11-20T09:00:00Z"),
                    ),
                )
                    .first()

            fixture.store.withWriterLease {
                val current =
                    beginRequest(
                        turnRef = "turn.continuity.host-context-current",
                        requestMessageRef = "message.continuity.host-context-current",
                        content =
                            "Without guessing: where was the cobalt lantern? " +
                                "Use a continuity/revisit capability if the detail is not actually in view.",
                        formedAt = Instant.parse("2026-11-20T10:00:00Z"),
                    )
                val before = fixture.homeTree()

                val prepared =
                    assertNotNull(
                        VexLifeHostContextContinuation.prepare(
                            conversationOwner = this,
                            currentRequest = current,
                            excludedEventRefs = setOf(current.eventRef),
                        ),
                    )

                assertEquals(
                    DirectConversationSourceReturn.HumanSpeaker,
                    prepared.speakerScope,
                )
                assertTrue(
                    prepared.discovery.matches.any {
                        it.requestEvent.eventRef ==
                            old.first.eventRef &&
                            it.requestEvent.content.contains(
                                "cobalt lantern",
                            )
                    },
                )
                assertFalse(
                    prepared.discovery.effectsPerformed,
                )
                assertEquals(
                    before,
                    fixture.homeTree(),
                    "host preparation must not mutate canonical Home after the current request exists",
                )
            }
        }
    }
}

// [VXG RealForever]
