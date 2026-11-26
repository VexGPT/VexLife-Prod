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
    }
}

// [VXG RealForever]
