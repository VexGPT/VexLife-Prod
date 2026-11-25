package vexlife.continuity

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vexlife.companion.DirectConversationSourceReturn

class DirectConversationContextProjectionTest {
    @Test
    fun explicitBackwardRecallFormsInspectableLexicalCues() {
        val request =
            "Back to the beginning: what exact project token did I give you?"

        assertEquals(
            DirectConversationContextProjection.Cue.EXACT_RECALL,
            DirectConversationContextProjection.cueOrNull(request),
        )
        assertTrue(
            "exact project token" in
                DirectConversationContextProjection.queryTerms(request),
        )
        assertEquals(
            DirectConversationSourceReturn.HumanSpeaker,
            DirectConversationContextProjection.speakerScope(request),
        )
    }

    @Test
    fun continuityPressureRecallKeepsTheDistinctiveSourcePhrase() {
        val request =
            "Without guessing: where was the cobalt lantern? " +
                "Use a continuity/revisit capability if the detail is not actually in view."

        assertNotNull(
            DirectConversationContextProjection.cueOrNull(request),
        )
        assertTrue(
            "cobalt lantern" in
                DirectConversationContextProjection.queryTerms(request),
        )
    }

    @Test
    fun requestBoundProjectionHasItsOwnIdentityAndKeepsSourceReturnSemanticsSeparate() {
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
                )
                    .single()

            fixture.store.withWriterLease {
                val current =
                    beginRequest(
                        turnRef = "turn.continuity.conversation-context-projection",
                        requestMessageRef = "message.continuity.conversation-context-projection",
                        content =
                            "Without guessing: where was the cobalt lantern? " +
                                "Use continuity reach if the detail is not actually in view.",
                        formedAt = Instant.parse("2026-11-20T10:00:00Z"),
                    )
                val prepared =
                    assertNotNull(
                        DirectConversationContextProjection.prepare(
                            conversationOwner = this,
                            currentRequest = current,
                            excludedEventRefs = setOf(current.eventRef),
                        ),
                    )
                val projection =
                    DirectConversationContextProjection.project(
                        prepared = prepared,
                        availableTokens = 8_000,
                    )

                assertEquals(
                    DirectConversationContextProjection.Returned,
                    projection.state,
                )
                assertTrue(
                    projection.systemContent.startsWith(
                        DirectConversationContextProjection.ContextHeader,
                    ),
                )
                assertFalse(
                    projection.systemContent.startsWith(
                        DirectConversationSourceReturn.ContextHeader,
                    ),
                )
                assertNotEquals(
                    DirectConversationSourceReturn.ContextHeader,
                    DirectConversationContextProjection.ContextHeader,
                )
                assertTrue(
                    old.first.eventRef in
                        projection.sourceEventRefs,
                )
                assertFalse(
                    projection.effectAuthorityGranted,
                )
                assertFalse(
                    projection.effectsPerformed,
                )
            }
        }
    }

    @Test
    fun currentLocatorPreparesExactOlderSourceWithoutWholeHistoryEnumeration() {
        ContinuityTurnFixture().use {
            fixture ->
            val old =
                fixture.commit(
                    ContinuityTurnFixture.Line(
                        human =
                            "Experiment anchor: the silver lantern is beside the pine gate.",
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
            val backend =
                VexLifeContinuitySourceLocator.InMemoryBackend(
                    secret =
                        ByteArray(32) {
                            43
                        },
                    fingerprintBindingRef =
                        "fingerprint.test.context-current",
                )

            fixture.store.withWriterLease {
                VexLifeContinuitySourceLocator.rebuild(
                    conversationOwner = this,
                    backend = backend,
                )
                val current =
                    beginRequest(
                        turnRef = "turn.continuity.locator-current",
                        requestMessageRef = "message.continuity.locator-current",
                        content =
                            "Without guessing: where was the silver lantern?",
                        formedAt = Instant.parse("2026-11-20T10:00:00Z"),
                    )
                val before =
                    fixture.homeTree()
                val prepared =
                    assertNotNull(
                        DirectConversationContextProjection.prepare(
                            conversationOwner = this,
                            currentRequest = current,
                            excludedEventRefs =
                                setOf(
                                    current.eventRef,
                                ),
                            locatorBackend = backend,
                        ),
                    )

                assertEquals(
                    DirectConversationContextProjection.SourcePreparation.LOCATOR_CURRENT,
                    prepared.sourcePreparation,
                )
                assertFalse(
                    prepared.discovery.wholeHistoryEventEnumerationPerformed,
                )
                assertTrue(
                    prepared.exactEventFileReadCount >=
                        2,
                )
                assertTrue(
                    prepared.discovery.matches.any {
                        it.requestEvent.eventRef ==
                            old.first.eventRef
                    },
                )
                assertEquals(
                    before,
                    fixture.homeTree(),
                    "current locator preparation must not mutate canonical Home",
                )
            }
        }
    }

    @Test
    fun absentLocatorRebuildsOnceAndReturnsExactSourceThroughTheSamePacketContract() {
        ContinuityTurnFixture().use {
            fixture ->
            fixture.commit(
                ContinuityTurnFixture.Line(
                    human =
                        "Experiment anchor: the copper telescope is beside the east window.",
                    vex = "recorded",
                    at = Instant.parse("2026-11-20T08:00:00Z"),
                ),
                ContinuityTurnFixture.Line(
                    human = "Unrelated later turn.",
                    vex = "later",
                    at = Instant.parse("2026-11-20T09:00:00Z"),
                ),
            )
            val backend =
                VexLifeContinuitySourceLocator.InMemoryBackend(
                    secret =
                        ByteArray(32) {
                            47
                        },
                    fingerprintBindingRef =
                        "fingerprint.test.context-rebuild",
                )

            fixture.store.withWriterLease {
                val current =
                    beginRequest(
                        turnRef = "turn.continuity.locator-rebuild",
                        requestMessageRef = "message.continuity.locator-rebuild",
                        content =
                            "Without guessing: where was the copper telescope?",
                        formedAt = Instant.parse("2026-11-20T10:00:00Z"),
                    )
                val before =
                    fixture.homeTree()
                val prepared =
                    assertNotNull(
                        DirectConversationContextProjection.prepare(
                            conversationOwner = this,
                            currentRequest = current,
                            excludedEventRefs =
                                setOf(
                                    current.eventRef,
                                ),
                            locatorBackend = backend,
                        ),
                    )
                val projection =
                    DirectConversationContextProjection.project(
                        prepared = prepared,
                        availableTokens = 8_000,
                    )

                assertEquals(
                    DirectConversationContextProjection.SourcePreparation.LOCATOR_REBUILT,
                    prepared.sourcePreparation,
                )
                assertFalse(
                    prepared.discovery.wholeHistoryEventEnumerationPerformed,
                )
                assertTrue(
                    prepared.exactEventFileReadCount >=
                        2,
                )
                assertEquals(
                    DirectConversationContextProjection.Returned,
                    projection.state,
                )
                assertTrue(
                    projection.systemContent.startsWith(
                        DirectConversationContextProjection.ContextHeader,
                    ),
                )
                assertEquals(
                    before,
                    fixture.homeTree(),
                    "derived locator rebuild must not mutate canonical Home",
                )
            }
        }
    }

    @Test
    fun ordinaryForwardRequestDoesNotCreateAContinuationCue() {
        assertNull(
            DirectConversationContextProjection.cueOrNull(
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
                        turnRef = "turn.continuity.conversation-context-current",
                        requestMessageRef = "message.continuity.conversation-context-current",
                        content =
                            "Without guessing: where was the cobalt lantern? " +
                                "Use a continuity/revisit capability if the detail is not actually in view.",
                        formedAt = Instant.parse("2026-11-20T10:00:00Z"),
                    )
                val before = fixture.homeTree()

                val prepared =
                    assertNotNull(
                        DirectConversationContextProjection.prepare(
                            conversationOwner = this,
                            currentRequest = current,
                            excludedEventRefs = setOf(current.eventRef),
                        ),
                    )

                assertEquals(
                    DirectConversationSourceReturn.AnySpeaker,
                    prepared.speakerScope,
                    "the recall wording does not prove which speaker owns the older source",
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
                    "request-bound projection must not mutate canonical Home after the current request exists",
                )
            }
        }
    }
}

// [VXG RealForever]
