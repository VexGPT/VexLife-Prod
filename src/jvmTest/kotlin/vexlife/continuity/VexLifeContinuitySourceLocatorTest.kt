package vexlife.continuity

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import vexlife.conversation.DirectConversationStore

class VexLifeContinuitySourceLocatorTest {
    @Test
    fun currentGenerationLocatesAndReopensExactCanonicalConversationWithoutMutatingHome() {
        ContinuityTurnFixture().use {
            fixture ->
            val anchor =
                fixture.commit(
                    ContinuityTurnFixture.Line(
                        human =
                            "Experiment anchor: the cobalt lantern is stored beside the cedar gate.",
                        vex = "recorded",
                        at = Instant.parse("2026-11-20T08:00:00Z"),
                    ),
                    ContinuityTurnFixture.Line(
                        human = "Unrelated later turn about breakfast.",
                        vex = "noted",
                        at = Instant.parse("2026-11-20T09:00:00Z"),
                    ),
                )
                    .first()
            val backend =
                VexLifeContinuitySourceLocator.InMemoryBackend(
                    ByteArray(32) {
                        7
                    },
                )

            fixture.store.withWriterLease {
                val before =
                    fixture.homeTree()
                val rebuilt =
                    VexLifeContinuitySourceLocator.rebuild(
                        conversationOwner = this,
                        backend = backend,
                    )

                assertTrue(
                    rebuilt.wholeHistoryEventEnumerationPerformed,
                )
                assertFalse(
                    rebuilt.canonicalSourceMutationPerformed,
                )
                assertTrue(
                    rebuilt.derivedBackendMutationPerformed,
                )
                assertEquals(
                    before,
                    fixture.homeTree(),
                    "derived locator rebuild must not mutate canonical Home",
                )

                val outcome =
                    assertIs<VexLifeContinuitySourceLocator.LocateOutcome.Found>(
                        VexLifeContinuitySourceLocator.locate(
                            conversationOwner = this,
                            queryTerms =
                                listOf(
                                    "cobalt lantern",
                                ),
                            speakerScope =
                                VexLifeContinuitySourceLocator.SpeakerScope.ANY,
                            backend = backend,
                        ),
                    )

                assertEquals(
                    anchor.first.eventRef,
                    outcome.candidates
                        .single()
                        .requestEvent
                        .eventRef,
                )
                assertEquals(
                    listOf(
                        "cobalt lantern",
                    ),
                    outcome.candidates
                        .single()
                        .matchedTerms,
                )
                assertTrue(
                    outcome.exactEventFileReadCount >=
                        2,
                )
                assertFalse(
                    outcome.wholeHistoryEventEnumerationPerformed,
                    "current locator lookup must not enumerate the whole Conversation event directory",
                )
                assertEquals(
                    0,
                    outcome.derivedEntryScanCount,
                    "default locator backend must use inverted postings rather than scan derived lifetime entries",
                )
                assertTrue(
                    outcome.derivedPostingLookupCount >
                        0,
                )
                assertFalse(
                    outcome.canonicalSourceMutationPerformed,
                )
                assertEquals(
                    before,
                    fixture.homeTree(),
                    "locator hit + canonical reopen must stay read-only",
                )
            }
        }
    }

    @Test
    fun currentGenerationMissCanSayMissButStaleGenerationMustRequestFallback() {
        ContinuityTurnFixture().use {
            fixture ->
            fixture.commit(
                ContinuityTurnFixture.Line(
                    human = "Alpha source.",
                    vex = "alpha",
                    at = Instant.parse("2026-11-20T08:00:00Z"),
                ),
            )
            val backend =
                VexLifeContinuitySourceLocator.InMemoryBackend(
                    ByteArray(32) {
                        11
                    },
                )

            fixture.store.withWriterLease {
                VexLifeContinuitySourceLocator.rebuild(
                    conversationOwner = this,
                    backend = backend,
                )
                assertIs<VexLifeContinuitySourceLocator.LocateOutcome.CurrentMiss>(
                    VexLifeContinuitySourceLocator.locate(
                        conversationOwner = this,
                        queryTerms =
                            listOf(
                                "not-present",
                            ),
                        speakerScope =
                            VexLifeContinuitySourceLocator.SpeakerScope.ANY,
                        backend = backend,
                    ),
                )
            }

            fixture.commit(
                ContinuityTurnFixture.Line(
                    human = "The newly added token is saffron-orbit-91.",
                    vex = "recorded",
                    at = Instant.parse("2026-11-20T09:00:00Z"),
                ),
            )

            fixture.store.withWriterLease {
                val stale =
                    assertIs<VexLifeContinuitySourceLocator.LocateOutcome.FallbackRequired>(
                        VexLifeContinuitySourceLocator.locate(
                            conversationOwner = this,
                            queryTerms =
                                listOf(
                                    "saffron-orbit-91",
                                ),
                            speakerScope =
                                VexLifeContinuitySourceLocator.SpeakerScope.ANY,
                            backend = backend,
                        ),
                    )
                assertEquals(
                    VexLifeContinuitySourceLocator.FallbackReason.STALE,
                    stale.reason,
                )
                assertFalse(
                    stale.wholeHistoryEventEnumerationPerformed,
                )
            }
        }
    }

    @Test
    fun appendOnlyCatchUpKeepsGenerationCurrentWithoutWholeHistoryRebuild() {
        ContinuityTurnFixture().use {
            fixture ->
            val backend =
                VexLifeContinuitySourceLocator.InMemoryBackend(
                    secret =
                        ByteArray(32) {
                            41
                        },
                    fingerprintBindingRef =
                        "fingerprint.test.catch-up",
                )
            val first =
                fixture.commit(
                    ContinuityTurnFixture.Line(
                        human = "The amber compass is beside the cedar map.",
                        vex = "recorded",
                        at = Instant.parse("2026-11-20T08:00:00Z"),
                    ),
                )
                    .single()

            fixture.store.withWriterLease {
                val receipt =
                    VexLifeContinuitySourceLocator.catchUpCommittedTurn(
                        committedHead =
                            requireNotNull(
                                current().head,
                            ),
                        request = first.first,
                        response = first.second,
                        backend = backend,
                    )
                assertEquals(
                    VexLifeContinuitySourceLocator.CatchUpState.UPDATED,
                    receipt.state,
                )
                assertFalse(
                    receipt.wholeHistoryEventEnumerationPerformed,
                )
                assertTrue(
                    receipt.derivedBackendMutationPerformed,
                )
            }

            val second =
                fixture.commit(
                    ContinuityTurnFixture.Line(
                        human = "A later unrelated message.",
                        vex = "later",
                        at = Instant.parse("2026-11-20T09:00:00Z"),
                    ),
                )
                    .single()

            fixture.store.withWriterLease {
                val receipt =
                    VexLifeContinuitySourceLocator.catchUpCommittedTurn(
                        committedHead =
                            requireNotNull(
                                current().head,
                            ),
                        request = second.first,
                        response = second.second,
                        backend = backend,
                    )
                assertEquals(
                    VexLifeContinuitySourceLocator.CatchUpState.UPDATED,
                    receipt.state,
                )
                assertEquals(
                    2,
                    receipt.completedTurnCount,
                )
                assertFalse(
                    receipt.wholeHistoryEventEnumerationPerformed,
                )

                val located =
                    assertIs<VexLifeContinuitySourceLocator.LocateOutcome.Found>(
                        VexLifeContinuitySourceLocator.locate(
                            conversationOwner = this,
                            queryTerms =
                                listOf(
                                    "amber compass",
                                ),
                            speakerScope =
                                VexLifeContinuitySourceLocator.SpeakerScope.ANY,
                            backend = backend,
                        ),
                    )
                assertEquals(
                    first.first.eventRef,
                    located.candidates
                        .single()
                        .requestEvent
                        .eventRef,
                )
                assertFalse(
                    located.wholeHistoryEventEnumerationPerformed,
                )
                assertEquals(
                    0,
                    located.derivedEntryScanCount,
                )
                assertTrue(
                    located.derivedPostingLookupCount >
                        0,
                )

                val locatedSecond =
                    assertIs<VexLifeContinuitySourceLocator.LocateOutcome.Found>(
                        VexLifeContinuitySourceLocator.locate(
                            conversationOwner = this,
                            queryTerms =
                                listOf(
                                    "later unrelated",
                                ),
                            speakerScope =
                                VexLifeContinuitySourceLocator.SpeakerScope.HUMAN,
                            backend = backend,
                        ),
                    )
                assertEquals(
                    second.first.eventRef,
                    locatedSecond.candidates
                        .single()
                        .requestEvent
                        .eventRef,
                    "incremental catch-up must add the new turn to postings without a lifetime rebuild",
                )
                assertEquals(
                    0,
                    locatedSecond.derivedEntryScanCount,
                )
            }
        }
    }

    @Test
    fun deletingDerivedLocatorIsRebuildableAndNeverDeletesConversation() {
        ContinuityTurnFixture().use {
            fixture ->
            val anchor =
                fixture.commit(
                    ContinuityTurnFixture.Line(
                        human = "Keep the violet compass near the north window.",
                        vex = "recorded",
                        at = Instant.parse("2026-11-20T08:00:00Z"),
                    ),
                )
                    .single()
            val backend =
                VexLifeContinuitySourceLocator.InMemoryBackend(
                    ByteArray(32) {
                        13
                    },
                )

            fixture.store.withWriterLease {
                VexLifeContinuitySourceLocator.rebuild(
                    conversationOwner = this,
                    backend = backend,
                )
                backend.clear()

                val absent =
                    assertIs<VexLifeContinuitySourceLocator.LocateOutcome.FallbackRequired>(
                        VexLifeContinuitySourceLocator.locate(
                            conversationOwner = this,
                            queryTerms =
                                listOf(
                                    "violet compass",
                                ),
                            speakerScope =
                                VexLifeContinuitySourceLocator.SpeakerScope.ANY,
                            backend = backend,
                        ),
                    )
                assertEquals(
                    VexLifeContinuitySourceLocator.FallbackReason.ABSENT,
                    absent.reason,
                )

                VexLifeContinuitySourceLocator.rebuild(
                    conversationOwner = this,
                    backend = backend,
                )
                val restored =
                    assertIs<VexLifeContinuitySourceLocator.LocateOutcome.Found>(
                        VexLifeContinuitySourceLocator.locate(
                            conversationOwner = this,
                            queryTerms =
                                listOf(
                                    "violet compass",
                                ),
                            speakerScope =
                                VexLifeContinuitySourceLocator.SpeakerScope.ANY,
                            backend = backend,
                        ),
                    )
                assertEquals(
                    anchor.first.eventRef,
                    restored.candidates
                        .single()
                        .requestEvent
                        .eventRef,
                )
            }
        }
    }

    @Test
    fun backendRealizationsProduceTheSameCanonicalCandidateIdentity() {
        ContinuityTurnFixture().use {
            fixture ->
            fixture.commit(
                ContinuityTurnFixture.Line(
                    human = "The brass telescope belongs on the west shelf.",
                    vex = "recorded",
                    at = Instant.parse("2026-11-20T08:00:00Z"),
                ),
                ContinuityTurnFixture.Line(
                    human = "Later unrelated source.",
                    vex = "later",
                    at = Instant.parse("2026-11-20T09:00:00Z"),
                ),
            )
            val first =
                VexLifeContinuitySourceLocator.InMemoryBackend(
                    ByteArray(32) {
                        17
                    },
                )
            val second =
                VexLifeContinuitySourceLocator.InMemoryBackend(
                    ByteArray(32) {
                        23
                    },
                )

            fixture.store.withWriterLease {
                VexLifeContinuitySourceLocator.rebuild(
                    conversationOwner = this,
                    backend = first,
                )
                VexLifeContinuitySourceLocator.rebuild(
                    conversationOwner = this,
                    backend = second,
                )

                fun candidateRefs(
                    backend: VexLifeContinuitySourceLocator.Backend,
                ): List<Pair<String, String>> =
                    assertIs<VexLifeContinuitySourceLocator.LocateOutcome.Found>(
                        VexLifeContinuitySourceLocator.locate(
                            conversationOwner = this,
                            queryTerms =
                                listOf(
                                    "brass telescope",
                                ),
                            speakerScope =
                                VexLifeContinuitySourceLocator.SpeakerScope.ANY,
                            backend = backend,
                        ),
                    )
                        .candidates
                        .map {
                            it.requestEvent.eventRef to
                                it.responseEvent.eventRef
                        }

                assertEquals(
                    candidateRefs(
                        first,
                    ),
                    candidateRefs(
                        second,
                    ),
                    "backend realization must not change canonical candidate identity",
                )
            }
        }
    }

    @Test
    fun indexedBackendAvoidsDerivedLifetimeScanWhileGenericBackendPreservesFallback() {
        ContinuityTurnFixture().use {
            fixture ->
            val turns =
                fixture.commit(
                    ContinuityTurnFixture.Line(
                        human = "First ordinary note.",
                        vex = "first",
                        at = Instant.parse("2026-11-20T08:00:00Z"),
                    ),
                    ContinuityTurnFixture.Line(
                        human = "The indexed token is quartz-river-73.",
                        vex = "recorded",
                        at = Instant.parse("2026-11-20T09:00:00Z"),
                    ),
                    ContinuityTurnFixture.Line(
                        human = "Third unrelated note.",
                        vex = "third",
                        at = Instant.parse("2026-11-20T10:00:00Z"),
                    ),
                )
            val indexed =
                VexLifeContinuitySourceLocator.InMemoryBackend(
                    secret =
                        ByteArray(32) {
                            59
                        },
                    fingerprintBindingRef =
                        "fingerprint.test.inverted-index",
                )

            fixture.store.withWriterLease {
                VexLifeContinuitySourceLocator.rebuild(
                    conversationOwner = this,
                    backend = indexed,
                )
            }
            val generation =
                assertIs<VexLifeContinuitySourceLocator.BackendLoad.Ready>(
                    indexed.load(),
                )
                    .generation
            val generic =
                object : VexLifeContinuitySourceLocator.Backend {
                    override val fingerprintBindingRef =
                        indexed.fingerprintBindingRef

                    override fun fingerprint(
                        value: String,
                    ): String =
                        indexed.fingerprint(
                            value,
                        )

                    override fun load(): VexLifeContinuitySourceLocator.BackendLoad =
                        VexLifeContinuitySourceLocator.BackendLoad.Ready(
                            generation,
                        )

                    override fun replace(
                        generation: VexLifeContinuitySourceLocator.Generation,
                    ) =
                        error(
                            "generic comparison backend must remain read-only",
                        )

                    override fun clear() =
                        Unit
                }

            fixture.store.withWriterLease {
                val indexedFound =
                    assertIs<VexLifeContinuitySourceLocator.LocateOutcome.Found>(
                        VexLifeContinuitySourceLocator.locate(
                            conversationOwner = this,
                            queryTerms =
                                listOf(
                                    "quartz-river-73",
                                ),
                            speakerScope =
                                VexLifeContinuitySourceLocator.SpeakerScope.HUMAN,
                            backend =
                                indexed,
                        ),
                    )
                val genericFound =
                    assertIs<VexLifeContinuitySourceLocator.LocateOutcome.Found>(
                        VexLifeContinuitySourceLocator.locate(
                            conversationOwner = this,
                            queryTerms =
                                listOf(
                                    "quartz-river-73",
                                ),
                            speakerScope =
                                VexLifeContinuitySourceLocator.SpeakerScope.HUMAN,
                            backend =
                                generic,
                        ),
                    )

                assertEquals(
                    turns[1].first.eventRef,
                    indexedFound.candidates
                        .single()
                        .requestEvent
                        .eventRef,
                )
                assertEquals(
                    indexedFound.candidates,
                    genericFound.candidates,
                    "backend query acceleration must not change canonical candidate identity",
                )
                assertEquals(
                    0,
                    indexedFound.derivedEntryScanCount,
                )
                assertTrue(
                    indexedFound.derivedPostingLookupCount >
                        0,
                )
                assertEquals(
                    generation.entries.size,
                    genericFound.derivedEntryScanCount,
                    "generic backend preserves the prior derived-entry scan fallback",
                )
                assertEquals(
                    0,
                    genericFound.derivedPostingLookupCount,
                )
            }
        }
    }

    @Test
    fun indexedNeighborhoodReturnsExactDirectAndImmediateTypedNeighborsWithoutDerivedScan() {
        ContinuityTurnFixture().use {
            fixture ->
            val turns =
                fixture.commit(
                    ContinuityTurnFixture.Line(
                        human = "Why does the cache miss here?",
                        vex = "You seem frustrated, so I will keep this short.",
                        at = Instant.parse("2026-11-20T08:00:00Z"),
                    ),
                    ContinuityTurnFixture.Line(
                        human = "I'm not frustrated. I want exact causal detail.",
                        vex = "Understood. Here is the exact chain.",
                        at = Instant.parse("2026-11-20T08:05:00Z"),
                    ),
                    ContinuityTurnFixture.Line(
                        human = "The sapphire seed is stored here.",
                        vex = "Seed recorded.",
                        at = Instant.parse("2026-11-20T08:10:00Z"),
                    ),
                    ContinuityTurnFixture.Line(
                        human = "Unrelated trailing turn.",
                        vex = "Trailing.",
                        at = Instant.parse("2026-11-20T08:15:00Z"),
                    ),
                )
            val backend =
                VexLifeContinuitySourceLocator.InMemoryBackend(
                    secret =
                        ByteArray(32) {
                            71
                        },
                    fingerprintBindingRef =
                        "fingerprint.test.neighborhood",
                )

            fixture.store.withWriterLease {
                VexLifeContinuitySourceLocator.rebuild(
                    conversationOwner = this,
                    backend = backend,
                )

                val lexical =
                    assertIs<VexLifeContinuitySourceLocator.NeighborhoodOutcome.Ready>(
                        VexLifeContinuitySourceLocator.locateNeighborhood(
                            conversationOwner = this,
                            queries =
                                listOf(
                                    VexLifeContinuitySourceLocator.NeighborhoodQuery(
                                        queryTerms =
                                            listOf(
                                                "cache miss",
                                            ),
                                        speakerScope =
                                            VexLifeContinuitySourceLocator.SpeakerScope.HUMAN,
                                    ),
                                ),
                            backend = backend,
                        ),
                    )
                assertEquals(
                    setOf(
                        turns[0].first.turnRef,
                    ),
                    lexical.directTurnRefs,
                )
                assertEquals(
                    listOf(
                        turns[0].first.turnRef,
                        turns[1].first.turnRef,
                    ),
                    lexical.turns.map(
                        VexLifeTurnNavigation.Turn::turnRef,
                    ),
                    "lexical neighborhood includes only the direct turn and its exact next canonical neighbor",
                )
                assertEquals(
                    4,
                    lexical.totalCompletedTurnCount,
                )
                assertEquals(
                    0,
                    lexical.derivedEntryScanCount,
                )
                assertTrue(
                    lexical.derivedPostingLookupCount >
                        0,
                )
                assertEquals(
                    0,
                    lexical.derivedRefLookupCount,
                )

                val seeded =
                    assertIs<VexLifeContinuitySourceLocator.NeighborhoodOutcome.Ready>(
                        VexLifeContinuitySourceLocator.locateNeighborhood(
                            conversationOwner = this,
                            queries =
                                listOf(
                                    VexLifeContinuitySourceLocator.NeighborhoodQuery(
                                        queryTerms =
                                            emptyList(),
                                        speakerScope =
                                            VexLifeContinuitySourceLocator.SpeakerScope.ANY,
                                        seedRefs =
                                            setOf(
                                                turns[2].second.eventRef,
                                            ),
                                    ),
                                ),
                            backend = backend,
                        ),
                    )
                assertEquals(
                    setOf(
                        turns[2].first.turnRef,
                    ),
                    seeded.directTurnRefs,
                )
                assertEquals(
                    listOf(
                        turns[1].first.turnRef,
                        turns[2].first.turnRef,
                        turns[3].first.turnRef,
                    ),
                    seeded.turns.map(
                        VexLifeTurnNavigation.Turn::turnRef,
                    ),
                )
                assertEquals(
                    1,
                    seeded.derivedRefLookupCount,
                )
                assertTrue(
                    seeded.unresolvedSeedRefs.isEmpty(),
                )

                val missingSeed =
                    assertIs<VexLifeContinuitySourceLocator.NeighborhoodOutcome.Ready>(
                        VexLifeContinuitySourceLocator.locateNeighborhood(
                            conversationOwner = this,
                            queries =
                                listOf(
                                    VexLifeContinuitySourceLocator.NeighborhoodQuery(
                                        queryTerms =
                                            emptyList(),
                                        speakerScope =
                                            VexLifeContinuitySourceLocator.SpeakerScope.ANY,
                                        seedRefs =
                                            setOf(
                                                "event.vexlife.missing.seed",
                                            ),
                                    ),
                                ),
                            backend = backend,
                        ),
                    )
                assertTrue(
                    missingSeed.turns.isEmpty(),
                )
                assertEquals(
                    setOf(
                        "event.vexlife.missing.seed",
                    ),
                    missingSeed.unresolvedSeedRefs,
                )

                val corrections =
                    assertIs<VexLifeContinuitySourceLocator.NeighborhoodOutcome.Ready>(
                        VexLifeContinuitySourceLocator.locateNeighborhood(
                            conversationOwner = this,
                            queries =
                                listOf(
                                    VexLifeContinuitySourceLocator.NeighborhoodQuery(
                                        queryTerms =
                                            emptyList(),
                                        speakerScope =
                                            VexLifeContinuitySourceLocator.SpeakerScope.ANY,
                                        typedEdges =
                                            setOf(
                                                VexLifeContinuitySourceLocator.TypedEdgeKind.CORRECTS_PREVIOUS,
                                            ),
                                    ),
                                ),
                            backend = backend,
                        ),
                    )
                assertEquals(
                    setOf(
                        turns[1].first.turnRef,
                    ),
                    corrections.directTurnRefs,
                )
                assertEquals(
                    listOf(
                        turns[0].first.turnRef,
                        turns[1].first.turnRef,
                        turns[2].first.turnRef,
                    ),
                    corrections.turns.map(
                        VexLifeTurnNavigation.Turn::turnRef,
                    ),
                    "typed-edge lookup returns the correction plus exact immediate canonical neighbors",
                )
                assertEquals(
                    1,
                    corrections.derivedTypedEdgeLookupCount,
                )
                assertEquals(
                    0,
                    corrections.derivedEntryScanCount,
                )
            }
        }
    }

    @Test
    fun canonicalOrdinalAndTypedNeighborSurviveAnAbandonedBranchAtTheSameSequence() {
        ContinuityTurnFixture().use {
            fixture ->
            val first =
                fixture.commit(
                    ContinuityTurnFixture.Line(
                        human = "The gap anchor is copper-lake.",
                        vex = "Anchor recorded.",
                        at = Instant.parse("2026-11-20T08:00:00Z"),
                    ),
                )
                    .single()
            val abandoned =
                fixture.store.withWriterLease {
                    beginRequest(
                        turnRef =
                            "turn.continuity.abandoned-branch",
                        requestMessageRef =
                            "message.continuity.human.abandoned-branch",
                        content =
                            "This durable request is intentionally left off-head.",
                        formedAt =
                            Instant.parse(
                                "2026-11-20T08:02:00Z",
                            ),
                    )
                }
            val second =
                fixture.commit(
                    ContinuityTurnFixture.Line(
                        human = "Actually, copper-lake needs the canonical neighbor.",
                        vex = "Canonical neighbor recorded.",
                        at = Instant.parse("2026-11-20T08:05:00Z"),
                    ),
                )
                    .single()

            assertEquals(
                abandoned.sequence,
                second.first.sequence,
                "an off-head branch and the later canonical successor lawfully reuse the same sequence coordinate from the last completed head",
            )
            assertTrue(
                abandoned.eventRef !=
                    second.first.eventRef &&
                    abandoned.eventHash !=
                        second.first.eventHash,
                "branch identity remains exact event ref/hash, not sequence alone",
            )

            fixture.store.withWriterLease {
                val snapshot =
                    current()
                val projected =
                    VexLifeTurnNavigation.project(
                        snapshot.canonicalEvents,
                    )
                assertEquals(
                    listOf(
                        0,
                        1,
                    ),
                    projected.map(
                        VexLifeTurnNavigation.Turn::ordinal,
                    ),
                    "canonical turn ordinal is completed-lineage position, not physical event sequence",
                )

                val backend =
                    VexLifeContinuitySourceLocator.InMemoryBackend(
                        secret =
                            ByteArray(32) {
                                79
                            },
                        fingerprintBindingRef =
                            "fingerprint.test.abandoned-gap",
                    )
                VexLifeContinuitySourceLocator.rebuild(
                    conversationOwner = this,
                    backend = backend,
                )
                val generation =
                    assertIs<VexLifeContinuitySourceLocator.BackendLoad.Ready>(
                        backend.load(),
                    )
                        .generation
                assertEquals(
                    2,
                    generation.canonicalCompletedTurnCount,
                )
                assertEquals(
                    listOf(
                        0,
                        1,
                    ),
                    generation.entries.map(
                        VexLifeContinuitySourceLocator.Entry::canonicalOrdinal,
                    ),
                )

                val neighborhood =
                    assertIs<VexLifeContinuitySourceLocator.NeighborhoodOutcome.Ready>(
                        VexLifeContinuitySourceLocator.locateNeighborhood(
                            conversationOwner = this,
                            queries =
                                listOf(
                                    VexLifeContinuitySourceLocator.NeighborhoodQuery(
                                        queryTerms =
                                            listOf(
                                                "gap anchor",
                                            ),
                                        speakerScope =
                                            VexLifeContinuitySourceLocator.SpeakerScope.HUMAN,
                                    ),
                                ),
                            backend = backend,
                        ),
                    )
                assertEquals(
                    listOf(
                        first.first.turnRef,
                        second.first.turnRef,
                    ),
                    neighborhood.turns.map(
                        VexLifeTurnNavigation.Turn::turnRef,
                    ),
                    "typed immediate-neighbor expansion follows canonical completed lineage despite an abandoned branch at the same sequence",
                )
                assertEquals(
                    listOf(
                        0,
                        1,
                    ),
                    neighborhood.turns.map(
                        VexLifeTurnNavigation.Turn::ordinal,
                    ),
                )
            }
        }
    }

    @Test
    fun generationContainsCoordinatesAndFingerprintsButNoConversationBodies() {
        ContinuityTurnFixture().use {
            fixture ->
            fixture.commit(
                ContinuityTurnFixture.Line(
                    human = "Private phrase: obsidian meadow.",
                    vex = "recorded",
                    at = Instant.parse("2026-11-20T08:00:00Z"),
                ),
            )
            val backend =
                VexLifeContinuitySourceLocator.InMemoryBackend(
                    ByteArray(32) {
                        29
                    },
                )

            fixture.store.withWriterLease {
                VexLifeContinuitySourceLocator.rebuild(
                    conversationOwner = this,
                    backend = backend,
                )
            }

            val generation =
                assertIs<VexLifeContinuitySourceLocator.BackendLoad.Ready>(
                    backend.load(),
                )
                    .generation
            val rendered =
                generation.toString()
                    .lowercase()

            assertFalse(
                "obsidian" in
                    rendered,
            )
            assertFalse(
                "meadow" in
                    rendered,
            )
            assertTrue(
                generation.entries
                    .single()
                    .requestFingerprints
                    .isNotEmpty(),
            )
        }
    }

    @Test
    fun fingerprintBindingChangeRequiresFallbackBeforeItCanBecomeCurrentMiss() {
        ContinuityTurnFixture().use {
            fixture ->
            fixture.commit(
                ContinuityTurnFixture.Line(
                    human = "The source token is mercury-bridge-42.",
                    vex = "recorded",
                    at = Instant.parse("2026-11-20T08:00:00Z"),
                ),
            )
            val original =
                VexLifeContinuitySourceLocator.InMemoryBackend(
                    secret =
                        ByteArray(32) {
                            31
                        },
                    fingerprintBindingRef =
                        "fingerprint.test.binding-a",
                )

            fixture.store.withWriterLease {
                VexLifeContinuitySourceLocator.rebuild(
                    conversationOwner = this,
                    backend = original,
                )
            }
            val generation =
                assertIs<VexLifeContinuitySourceLocator.BackendLoad.Ready>(
                    original.load(),
                )
                    .generation

            val rebound =
                object : VexLifeContinuitySourceLocator.Backend {
                    override val fingerprintBindingRef =
                        "fingerprint.test.binding-b"

                    override fun fingerprint(
                        value: String,
                    ): String =
                        error(
                            "fingerprint must not run after binding mismatch",
                        )

                    override fun load(): VexLifeContinuitySourceLocator.BackendLoad =
                        VexLifeContinuitySourceLocator.BackendLoad.Ready(
                            generation,
                        )

                    override fun replace(
                        generation: VexLifeContinuitySourceLocator.Generation,
                    ) =
                        error(
                            "replace must not be reached",
                        )

                    override fun clear() =
                        Unit
                }

            fixture.store.withWriterLease {
                val outcome =
                    assertIs<VexLifeContinuitySourceLocator.LocateOutcome.FallbackRequired>(
                        VexLifeContinuitySourceLocator.locate(
                            conversationOwner = this,
                            queryTerms =
                                listOf(
                                    "mercury-bridge-42",
                                ),
                            speakerScope =
                                VexLifeContinuitySourceLocator.SpeakerScope.ANY,
                            backend = rebound,
                        ),
                    )
                assertEquals(
                    VexLifeContinuitySourceLocator.FallbackReason.FINGERPRINT_BINDING_MISMATCH,
                    outcome.reason,
                )
            }
        }
    }

    @Test
    fun malformedLoadedSourceCoordinatesAreCorruptNotAuthoritative() {
        ContinuityTurnFixture().use {
            fixture ->
            fixture.commit(
                ContinuityTurnFixture.Line(
                    human = "The source token is jade-harbor-17.",
                    vex = "recorded",
                    at = Instant.parse("2026-11-20T08:00:00Z"),
                ),
            )
            val original =
                VexLifeContinuitySourceLocator.InMemoryBackend(
                    secret =
                        ByteArray(32) {
                            37
                        },
                    fingerprintBindingRef =
                        "fingerprint.test.malformed",
                )

            fixture.store.withWriterLease {
                VexLifeContinuitySourceLocator.rebuild(
                    conversationOwner = this,
                    backend = original,
                )
            }
            val generation =
                assertIs<VexLifeContinuitySourceLocator.BackendLoad.Ready>(
                    original.load(),
                )
                    .generation
            val malformed =
                generation.copy(
                    entries =
                        generation.entries.mapIndexed {
                            index,
                            entry,
                            ->
                            if (
                                index ==
                                    0
                            ) {
                                entry.copy(
                                    coordinates =
                                        entry.coordinates.copy(
                                            request =
                                                entry.coordinates.request.copy(
                                                    eventHash =
                                                        "0".repeat(
                                                            64,
                                                        ),
                                                ),
                                        ),
                                )
                            } else {
                                entry
                            }
                        },
                )
            val backend =
                object : VexLifeContinuitySourceLocator.Backend {
                    override val fingerprintBindingRef =
                        generation.fingerprintBindingRef

                    override fun fingerprint(
                        value: String,
                    ): String =
                        original.fingerprint(
                            value,
                        )

                    override fun load(): VexLifeContinuitySourceLocator.BackendLoad =
                        VexLifeContinuitySourceLocator.BackendLoad.Ready(
                            malformed,
                        )

                    override fun replace(
                        generation: VexLifeContinuitySourceLocator.Generation,
                    ) =
                        error(
                            "replace must not be reached",
                        )

                    override fun clear() =
                        Unit
                }

            fixture.store.withWriterLease {
                val outcome =
                    assertIs<VexLifeContinuitySourceLocator.LocateOutcome.FallbackRequired>(
                        VexLifeContinuitySourceLocator.locate(
                            conversationOwner = this,
                            queryTerms =
                                listOf(
                                    "jade-harbor-17",
                                ),
                            speakerScope =
                                VexLifeContinuitySourceLocator.SpeakerScope.ANY,
                            backend = backend,
                        ),
                    )
                assertEquals(
                    VexLifeContinuitySourceLocator.FallbackReason.SOURCE_COORDINATE_MISMATCH,
                    outcome.reason,
                )
            }
        }
    }

    @Test
    fun semanticLifecycleBindingChangeRequiresRebuildBeforeAuthoritativeMiss() {
        ContinuityTurnFixture().use {
            fixture ->
            fixture.commit(
                ContinuityTurnFixture.Line(
                    human = "The lifecycle anchor is umber-bridge-73.",
                    vex = "recorded",
                    at = Instant.parse("2026-11-20T08:00:00Z"),
                ),
            )
            val backend =
                VexLifeContinuitySourceLocator.InMemoryBackend(
                    secret =
                        ByteArray(32) {
                            61
                        },
                    fingerprintBindingRef =
                        "fingerprint.test.lifecycle-binding",
                )

            fixture.store.withWriterLease {
                VexLifeContinuitySourceLocator.rebuild(
                    conversationOwner = this,
                    backend = backend,
                    sourceLifecycleBindingRef =
                        "lifecycle.score.aaaaaaaaaaaaaaaa.bbbbbbbbbbbbbbbb",
                )

                val changed =
                    assertIs<VexLifeContinuitySourceLocator.LocateOutcome.FallbackRequired>(
                        VexLifeContinuitySourceLocator.locate(
                            conversationOwner = this,
                            queryTerms =
                                listOf(
                                    "not-present",
                                ),
                            speakerScope =
                                VexLifeContinuitySourceLocator.SpeakerScope.ANY,
                            backend = backend,
                            sourceLifecycleBindingRef =
                                "lifecycle.score.cccccccccccccccc.dddddddddddddddd",
                        ),
                    )
                assertEquals(
                    VexLifeContinuitySourceLocator.FallbackReason.SOURCE_LIFECYCLE_BINDING_MISMATCH,
                    changed.reason,
                )
            }
        }
    }

    @Test
    fun lifecycleRebuildOmitsBlockedPairsWithoutDeletingCanonicalConversation() {
        ContinuityTurnFixture().use {
            fixture ->
            val blocked =
                fixture.commit(
                    ContinuityTurnFixture.Line(
                        human = "The withdrawn anchor is scarlet-harbor-88.",
                        vex = "historical",
                        at = Instant.parse("2026-11-20T08:00:00Z"),
                    ),
                    ContinuityTurnFixture.Line(
                        human = "The current anchor is teal-orbit-24.",
                        vex = "current",
                        at = Instant.parse("2026-11-20T09:00:00Z"),
                    ),
                )
                    .first()
            val backend =
                VexLifeContinuitySourceLocator.InMemoryBackend(
                    secret =
                        ByteArray(32) {
                            67
                        },
                    fingerprintBindingRef =
                        "fingerprint.test.lifecycle-filter",
                )

            fixture.store.withWriterLease {
                val before =
                    fixture.homeTree()
                val receipt =
                    VexLifeContinuitySourceLocator.rebuild(
                        conversationOwner = this,
                        backend = backend,
                        sourceLifecycleBindingRef =
                            "lifecycle.score.1111111111111111.2222222222222222",
                        excludedEventRefs =
                            setOf(
                                blocked.first.eventRef,
                                blocked.second.eventRef,
                            ),
                    )
                val generation =
                    assertIs<VexLifeContinuitySourceLocator.BackendLoad.Ready>(
                        backend.load(),
                    )
                        .generation

                assertEquals(
                    1,
                    receipt.completedTurnCount,
                )
                assertEquals(
                    2,
                    receipt.sourceEventCount,
                )
                assertEquals(
                    "lifecycle.score.1111111111111111.2222222222222222",
                    generation.sourceLifecycleBindingRef,
                )
                assertEquals(
                    1,
                    generation.entries.size,
                )
                assertEquals(
                    before,
                    fixture.homeTree(),
                    "lifecycle filtering may remove only derived locator entries",
                )

                assertIs<VexLifeContinuitySourceLocator.LocateOutcome.CurrentMiss>(
                    VexLifeContinuitySourceLocator.locate(
                        conversationOwner = this,
                        queryTerms =
                            listOf(
                                "scarlet-harbor-88",
                            ),
                        speakerScope =
                            VexLifeContinuitySourceLocator.SpeakerScope.ANY,
                        backend = backend,
                        sourceLifecycleBindingRef =
                            generation.sourceLifecycleBindingRef,
                    ),
                )
                assertIs<VexLifeContinuitySourceLocator.LocateOutcome.Found>(
                    VexLifeContinuitySourceLocator.locate(
                        conversationOwner = this,
                        queryTerms =
                            listOf(
                                "teal-orbit-24",
                            ),
                        speakerScope =
                            VexLifeContinuitySourceLocator.SpeakerScope.ANY,
                        backend = backend,
                        sourceLifecycleBindingRef =
                            generation.sourceLifecycleBindingRef,
                    ),
                )
            }
        }
    }

    @Test
    fun semanticNeighborhoodNominationBridgesMeridianParaphraseThenReopensExactCanonicalSource() {
        ContinuityTurnFixture().use {
            fixture ->
            val turns =
                fixture.commit(
                    ContinuityTurnFixture.Line(
                        human =
                            "The paper crane is named Meridian-7.",
                        vex = "recorded",
                        at = Instant.parse("2026-11-20T08:00:00Z"),
                    ),
                    ContinuityTurnFixture.Line(
                        human = "Later unrelated turn.",
                        vex = "later",
                        at = Instant.parse("2026-11-20T08:05:00Z"),
                    ),
                )
            val backend =
                DeterministicSemanticBackend(
                    fingerprintBindingRef =
                        "fingerprint.test.semantic-meridian",
                    semanticRepresentationBindingRef =
                        "semantic.test.meridian-v1",
                )

            fixture.store.withWriterLease {
                val before =
                    fixture.homeTree()
                VexLifeContinuitySourceLocator.rebuild(
                    conversationOwner = this,
                    backend = backend,
                )

                val outcome =
                    assertIs<VexLifeContinuitySourceLocator.NeighborhoodOutcome.Ready>(
                        VexLifeContinuitySourceLocator.locateNeighborhood(
                            conversationOwner = this,
                            queries =
                                listOf(
                                    VexLifeContinuitySourceLocator.NeighborhoodQuery(
                                        queryTerms =
                                            listOf(
                                                "bird",
                                            ),
                                        speakerScope =
                                            VexLifeContinuitySourceLocator.SpeakerScope.HUMAN,
                                    ),
                                    VexLifeContinuitySourceLocator.NeighborhoodQuery(
                                        queryTerms =
                                            listOf(
                                                "bird-shaped",
                                            ),
                                        speakerScope =
                                            VexLifeContinuitySourceLocator.SpeakerScope.HUMAN,
                                    ),
                                    VexLifeContinuitySourceLocator.NeighborhoodQuery(
                                        queryTerms =
                                            listOf(
                                                "identifier",
                                            ),
                                        speakerScope =
                                            VexLifeContinuitySourceLocator.SpeakerScope.HUMAN,
                                    ),
                                ),
                            backend = backend,
                        ),
                    )

                assertEquals(
                    setOf(
                        turns[0].first.turnRef,
                    ),
                    outcome.directTurnRefs,
                )
                assertEquals(
                    setOf(
                        turns[0].first.turnRef,
                    ),
                    outcome.semanticDirectTurnRefs,
                    "semantic ranking may nominate only the source coordinate; canonical Conversation still supplies the returned turn",
                )
                assertEquals(
                    3,
                    outcome.derivedSemanticLookupCount,
                    "each alternate semantic vantage is a bounded derived lookup",
                )
                assertTrue(
                    outcome.derivedPostingLookupCount >
                        0,
                    "lexical postings are attempted before semantic nomination",
                )
                assertTrue(
                    outcome.exactEventFileReadCount >=
                        2,
                    "semantic nomination must descend through exact canonical event reopen",
                )
                assertEquals(
                    listOf(
                        turns[0].first.turnRef,
                        turns[1].first.turnRef,
                    ),
                    outcome.turns.map(
                        VexLifeTurnNavigation.Turn::turnRef,
                    ),
                    "semantic direct hit keeps the existing exact immediate-neighbor expansion law",
                )
                assertFalse(
                    outcome.wholeHistoryHeadTraversalPerformed,
                )
                assertFalse(
                    outcome.canonicalSourceMutationPerformed,
                )
                assertEquals(
                    before,
                    fixture.homeTree(),
                    "semantic nomination + canonical reopen must remain read-only",
                )

                val generation =
                    assertIs<VexLifeContinuitySourceLocator.BackendLoad.Ready>(
                        backend.load(),
                    )
                        .generation
                val renderedGeneration =
                    generation.toString()
                        .lowercase()
                assertFalse(
                    "paper crane" in
                        renderedGeneration,
                    "raw source content belongs only to ephemeral semantic ingestion, not the portable locator generation",
                )
                assertFalse(
                    "meridian-7" in
                        renderedGeneration,
                )
            }
        }
    }

    @Test
    fun lexicalNeighborhoodHitDoesNotInvokeSemanticNomination() {
        ContinuityTurnFixture().use {
            fixture ->
            val turn =
                fixture.commit(
                    ContinuityTurnFixture.Line(
                        human =
                            "The paper crane is named Meridian-7.",
                        vex = "recorded",
                        at = Instant.parse("2026-11-20T08:00:00Z"),
                    ),
                )
                    .single()
            val backend =
                DeterministicSemanticBackend(
                    fingerprintBindingRef =
                        "fingerprint.test.semantic-lexical-first",
                    semanticRepresentationBindingRef =
                        "semantic.test.lexical-first-v1",
                )

            fixture.store.withWriterLease {
                VexLifeContinuitySourceLocator.rebuild(
                    conversationOwner = this,
                    backend = backend,
                )
                val outcome =
                    assertIs<VexLifeContinuitySourceLocator.NeighborhoodOutcome.Ready>(
                        VexLifeContinuitySourceLocator.locateNeighborhood(
                            conversationOwner = this,
                            queries =
                                listOf(
                                    VexLifeContinuitySourceLocator.NeighborhoodQuery(
                                        queryTerms =
                                            listOf(
                                                "paper crane",
                                            ),
                                        speakerScope =
                                            VexLifeContinuitySourceLocator.SpeakerScope.HUMAN,
                                    ),
                                ),
                            backend = backend,
                        ),
                    )

                assertEquals(
                    setOf(
                        turn.first.turnRef,
                    ),
                    outcome.directTurnRefs,
                )
                assertTrue(
                    outcome.semanticDirectTurnRefs.isEmpty(),
                )
                assertEquals(
                    0,
                    outcome.derivedSemanticLookupCount,
                    "semantic nomination must not widen an already-satisfied lexical query",
                )
                assertEquals(
                    0,
                    backend.semanticLookupInvocations,
                )
            }
        }
    }

    @Test
    fun staleReboundOrCorruptSemanticIndexFailsClosedInsteadOfBecomingNotFound() {
        ContinuityTurnFixture().use {
            fixture ->
            fixture.commit(
                ContinuityTurnFixture.Line(
                    human =
                        "The paper crane is named Meridian-7.",
                    vex = "recorded",
                    at = Instant.parse("2026-11-20T08:00:00Z"),
                ),
            )
            val backend =
                DeterministicSemanticBackend(
                    fingerprintBindingRef =
                        "fingerprint.test.semantic-fail-closed",
                    semanticRepresentationBindingRef =
                        "semantic.test.fail-closed-v1",
                )

            fixture.store.withWriterLease {
                VexLifeContinuitySourceLocator.rebuild(
                    conversationOwner = this,
                    backend = backend,
                )

                backend.forceStaleLookup =
                    true
                val stale =
                    assertIs<VexLifeContinuitySourceLocator.NeighborhoodOutcome.FallbackRequired>(
                        VexLifeContinuitySourceLocator.locateNeighborhood(
                            conversationOwner = this,
                            queries =
                                listOf(
                                    VexLifeContinuitySourceLocator.NeighborhoodQuery(
                                        queryTerms =
                                            listOf(
                                                "bird-shaped",
                                            ),
                                        speakerScope =
                                            VexLifeContinuitySourceLocator.SpeakerScope.HUMAN,
                                    ),
                                ),
                            backend = backend,
                        ),
                    )
                assertEquals(
                    VexLifeContinuitySourceLocator.FallbackReason.SEMANTIC_INDEX_STALE,
                    stale.reason,
                )

                backend.forceStaleLookup =
                    false
                backend.forceBindingMismatch =
                    true
                val rebound =
                    assertIs<VexLifeContinuitySourceLocator.NeighborhoodOutcome.FallbackRequired>(
                        VexLifeContinuitySourceLocator.locateNeighborhood(
                            conversationOwner = this,
                            queries =
                                listOf(
                                    VexLifeContinuitySourceLocator.NeighborhoodQuery(
                                        queryTerms =
                                            listOf(
                                                "bird-shaped",
                                            ),
                                        speakerScope =
                                            VexLifeContinuitySourceLocator.SpeakerScope.HUMAN,
                                    ),
                                ),
                            backend = backend,
                        ),
                    )
                assertEquals(
                    VexLifeContinuitySourceLocator.FallbackReason.SEMANTIC_REPRESENTATION_BINDING_MISMATCH,
                    rebound.reason,
                )

                backend.forceBindingMismatch =
                    false
                backend.forceCorruptIndex =
                    true
                val corrupt =
                    assertIs<VexLifeContinuitySourceLocator.NeighborhoodOutcome.FallbackRequired>(
                        VexLifeContinuitySourceLocator.locateNeighborhood(
                            conversationOwner = this,
                            queries =
                                listOf(
                                    VexLifeContinuitySourceLocator.NeighborhoodQuery(
                                        queryTerms =
                                            listOf(
                                                "bird-shaped",
                                            ),
                                        speakerScope =
                                            VexLifeContinuitySourceLocator.SpeakerScope.HUMAN,
                                    ),
                                ),
                            backend = backend,
                        ),
                    )
                assertEquals(
                    VexLifeContinuitySourceLocator.FallbackReason.SEMANTIC_INDEX_CORRUPT,
                    corrupt.reason,
                )
            }
        }
    }

    @Test
    fun corruptOrUnsupportedLocatorNeverBecomesNotFound() {
        ContinuityTurnFixture().use {
            fixture ->
            fixture.commit(
                ContinuityTurnFixture.Line(
                    human = "Source exists.",
                    vex = "yes",
                    at = Instant.parse("2026-11-20T08:00:00Z"),
                ),
            )

            fixture.store.withWriterLease {
                val unsupported =
                    assertIs<VexLifeContinuitySourceLocator.LocateOutcome.FallbackRequired>(
                        VexLifeContinuitySourceLocator.locate(
                            conversationOwner = this,
                            queryTerms =
                                listOf(
                                    "x",
                                ),
                            speakerScope =
                                VexLifeContinuitySourceLocator.SpeakerScope.ANY,
                            backend =
                                VexLifeContinuitySourceLocator.InMemoryBackend(),
                        ),
                    )
                assertEquals(
                    VexLifeContinuitySourceLocator.FallbackReason.UNSUPPORTED_QUERY,
                    unsupported.reason,
                )

                val corruptBackend =
                    object : VexLifeContinuitySourceLocator.Backend {
                        override val fingerprintBindingRef =
                            "fingerprint.test.corrupt"

                        override fun fingerprint(
                            value: String,
                        ): String =
                            error(
                                "fingerprint must not be reached for corrupt backend",
                            )

                        override fun load(): VexLifeContinuitySourceLocator.BackendLoad =
                            VexLifeContinuitySourceLocator.BackendLoad.Corrupt(
                                "synthetic corruption",
                            )

                        override fun replace(
                            generation: VexLifeContinuitySourceLocator.Generation,
                        ) =
                            error(
                                "replace must not be reached",
                            )

                        override fun clear() =
                            Unit
                    }
                val corrupt =
                    assertIs<VexLifeContinuitySourceLocator.LocateOutcome.FallbackRequired>(
                        VexLifeContinuitySourceLocator.locate(
                            conversationOwner = this,
                            queryTerms =
                                listOf(
                                    "source exists",
                                ),
                            speakerScope =
                                VexLifeContinuitySourceLocator.SpeakerScope.ANY,
                            backend = corruptBackend,
                        ),
                    )
                assertEquals(
                    VexLifeContinuitySourceLocator.FallbackReason.CORRUPT,
                    corrupt.reason,
                )
            }
        }
    private class DeterministicSemanticBackend(
        fingerprintBindingRef: String,
        override val semanticRepresentationBindingRef: String,
    ) : VexLifeContinuitySourceLocator.SemanticIndexedBackend {
        private val lexical =
            VexLifeContinuitySourceLocator.InMemoryBackend(
                secret =
                    ByteArray(32) {
                        83
                    },
                fingerprintBindingRef =
                    fingerprintBindingRef,
            )

        private var semanticGeneration:
            VexLifeContinuitySourceLocator.Generation? =
            null

        private var semanticSources:
            List<VexLifeContinuitySourceLocator.SemanticSourceContent> =
            emptyList()

        var semanticLookupInvocations: Int =
            0
            private set

        var forceStaleLookup: Boolean =
            false

        var forceBindingMismatch: Boolean =
            false

        var forceCorruptIndex: Boolean =
            false

        override val fingerprintBindingRef: String
            get() =
                lexical.fingerprintBindingRef

        override fun fingerprint(
            value: String,
        ): String =
            lexical.fingerprint(
                value,
            )

        override fun load(): VexLifeContinuitySourceLocator.BackendLoad =
            lexical.load()

        override fun replace(
            generation: VexLifeContinuitySourceLocator.Generation,
        ) =
            error(
                "semantic backend must receive canonical source content through replaceWithSemanticSources",
            )

        override fun clear() {
            lexical.clear()
            semanticGeneration =
                null
            semanticSources =
                emptyList()
        }

        override fun candidateEntryIndexes(
            queryFingerprints: List<Set<String>>,
            speakerScope: VexLifeContinuitySourceLocator.SpeakerScope,
        ): VexLifeContinuitySourceLocator.IndexedCandidateLookup =
            lexical.candidateEntryIndexes(
                queryFingerprints =
                    queryFingerprints,
                speakerScope =
                    speakerScope,
            )

        override fun entryIndexesForRefs(
            refs: Set<String>,
        ): VexLifeContinuitySourceLocator.IndexedRefLookup =
            lexical.entryIndexesForRefs(
                refs,
            )

        override fun entryIndexesForTypedEdges(
            typedEdges: Set<VexLifeContinuitySourceLocator.TypedEdgeKind>,
        ): VexLifeContinuitySourceLocator.IndexedTypedEdgeLookup =
            lexical.entryIndexesForTypedEdges(
                typedEdges,
            )

        override fun replaceAfterAppend(
            priorGeneration: VexLifeContinuitySourceLocator.Generation,
            generation: VexLifeContinuitySourceLocator.Generation,
            appendedEntry: VexLifeContinuitySourceLocator.Entry,
        ) =
            error(
                "semantic backend append must receive canonical source content through replaceAfterAppendWithSemanticSource",
            )

        override fun replaceWithSemanticSources(
            generation: VexLifeContinuitySourceLocator.Generation,
            sourceContents: List<VexLifeContinuitySourceLocator.SemanticSourceContent>,
        ) {
            require(
                sourceContents.map(
                    VexLifeContinuitySourceLocator.SemanticSourceContent::entryIndex,
                ) ==
                    generation.entries.indices.toList(),
            ) {
                "semantic test backend requires one source payload per derived entry"
            }
            lexical.replace(
                generation,
            )
            semanticGeneration =
                generation
            semanticSources =
                sourceContents
                    .map {
                        it.copy()
                    }
        }

        override fun replaceAfterAppendWithSemanticSource(
            priorGeneration: VexLifeContinuitySourceLocator.Generation,
            generation: VexLifeContinuitySourceLocator.Generation,
            appendedEntry: VexLifeContinuitySourceLocator.Entry,
            appendedSource: VexLifeContinuitySourceLocator.SemanticSourceContent,
        ) {
            require(
                appendedSource.entryIndex ==
                    generation.entries.lastIndex,
            )
            lexical.replaceAfterAppend(
                priorGeneration =
                    priorGeneration,
                generation =
                    generation,
                appendedEntry =
                    appendedEntry,
            )
            semanticGeneration =
                generation
            semanticSources =
                semanticSources +
                    appendedSource.copy()
        }

        override fun semanticCandidateEntryIndexes(
            queryTerms: List<String>,
            speakerScope: VexLifeContinuitySourceLocator.SpeakerScope,
        ): VexLifeContinuitySourceLocator.SemanticCandidateLookup {
            semanticLookupInvocations +=
                1
            val generation =
                requireNotNull(
                    semanticGeneration,
                )
            val normalized =
                queryTerms
                    .map {
                        it.lowercase()
                    }
            val meridianRequested =
                normalized.any {
                    term ->
                    term == "bird" ||
                        term == "bird-shaped" ||
                        term == "identifier"
                }
            val meridianIndex =
                semanticSources
                    .singleOrNull {
                        source ->
                        "paper crane" in
                            source.requestContent
                                .lowercase() &&
                            "meridian-7" in
                                source.requestContent
                                    .lowercase()
                    }
                    ?.entryIndex
            val indexes =
                when {
                    forceCorruptIndex ->
                        setOf(
                            generation.entries.size +
                                7,
                        )

                    meridianRequested &&
                        meridianIndex !=
                        null ->
                        setOf(
                            meridianIndex,
                        )

                    else ->
                        emptySet()
                }

            return VexLifeContinuitySourceLocator.SemanticCandidateLookup(
                entryIndexes =
                    indexes,
                semanticLookupCount =
                    1,
                representationBindingRef =
                    if (
                        forceBindingMismatch
                    ) {
                        "semantic.test.other-binding"
                    } else {
                        semanticRepresentationBindingRef
                    },
                sourceConversationHeadSha256 =
                    if (
                        forceStaleLookup
                    ) {
                        "0".repeat(
                            64,
                        )
                    } else {
                        generation
                            .sourceConversationHeadSha256
                    },
                sourceConversationSequence =
                    generation
                        .sourceConversationSequence,
                sourceLifecycleBindingRef =
                    generation
                        .sourceLifecycleBindingRef,
            )
        }
    }

    }
}

// [VXG RealForever]
