package vexlife.continuity

import java.nio.file.Files
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class VexLifeFileBackedDenseSemanticBackendTest {
    @Test
    fun currentSnapshotReloadsWithoutReencodingSourcesAndStillExactReopens() {
        ContinuityTurnFixture().use {
            fixture ->
            val turns =
                fixture.commit(
                    ContinuityTurnFixture.Line(
                        human =
                            "SENSITIVE TARGET SOURCE BODY cobalt-anchor semantic-slot-17.",
                        vex =
                            "SENSITIVE TARGET RESPONSE BODY semantic-slot-40.",
                        at =
                            Instant.parse(
                                "2022-01-01T00:00:00Z",
                            ),
                    ),
                    ContinuityTurnFixture.Line(
                        human =
                            "Unrelated source semantic-slot-2.",
                        vex =
                            "Unrelated response semantic-slot-3.",
                        at =
                            Instant.parse(
                                "2025-01-01T00:00:00Z",
                            ),
                    ),
                )
            val firstEncoder =
                RecordingEncoder(
                    "semantic.test.file-backed.v1",
                )
            val first =
                VexLifeFileBackedDenseSemanticBackend(
                    productHome = fixture.home,
                    encoder = firstEncoder,
                    secretSource = fixedSecret(
                        11,
                    ),
                )

            fixture.store.withWriterLease {
                VexLifeContinuitySourceLocator.rebuild(
                    conversationOwner = this,
                    backend = first,
                )
            }
            assertEquals(
                listOf(
                    4,
                ),
                firstEncoder.sourceBatchSizes,
            )
            val snapshotPath =
                first.persistentSnapshotPath()
            assertTrue(
                Files.isRegularFile(
                    snapshotPath,
                ),
            )
            val snapshotText =
                Files.readString(
                    snapshotPath,
                )
            assertFalse(
                snapshotText.contains(
                    "SENSITIVE TARGET SOURCE BODY",
                ),
                "derived persistence must not retain request source bodies",
            )
            assertFalse(
                snapshotText.contains(
                    "SENSITIVE TARGET RESPONSE BODY",
                ),
                "derived persistence must not retain response source bodies",
            )

            val secondEncoder =
                RecordingEncoder(
                    "semantic.test.file-backed.v1",
                )
            val reopened =
                VexLifeFileBackedDenseSemanticBackend(
                    productHome = fixture.home,
                    encoder = secondEncoder,
                    secretSource = {
                        error(
                            "valid snapshot reload must reuse its persisted fingerprint seed",
                        )
                    },
                )
            assertIs<VexLifeContinuitySourceLocator.BackendLoad.Ready>(
                reopened.load(),
            )
            assertTrue(
                secondEncoder.sourceBatchSizes.isEmpty(),
                "restart reload must not re-encode canonical source bodies",
            )

            fixture.store.withWriterLease {
                val lexical =
                    assertIs<VexLifeContinuitySourceLocator.LocateOutcome.Found>(
                        VexLifeContinuitySourceLocator.locate(
                            conversationOwner = this,
                            queryTerms =
                                listOf(
                                    "cobalt-anchor",
                                ),
                            speakerScope =
                                VexLifeContinuitySourceLocator.SpeakerScope.HUMAN,
                            backend = reopened,
                        ),
                    )
                assertEquals(
                    turns.first().first.eventRef,
                    lexical.candidates
                        .single()
                        .requestEvent
                        .eventRef,
                )
                assertEquals(
                    0,
                    lexical.derivedEntryScanCount,
                )

                val semantic =
                    assertIs<VexLifeContinuitySourceLocator.NeighborhoodOutcome.Ready>(
                        VexLifeContinuitySourceLocator.locateNeighborhood(
                            conversationOwner = this,
                            queries =
                                listOf(
                                    VexLifeContinuitySourceLocator.NeighborhoodQuery(
                                        queryRef =
                                            "semantic-restart",
                                        queryTerms =
                                            listOf(
                                                "concept-seventeen",
                                            ),
                                        speakerScope =
                                            VexLifeContinuitySourceLocator.SpeakerScope.HUMAN,
                                    ),
                                ),
                            backend = reopened,
                        ),
                    )
                assertEquals(
                    turns.first().first.turnRef,
                    semantic.semanticMatches
                        .first()
                        .turnRef,
                )
                assertTrue(
                    semantic.exactEventFileReadCount >
                        0,
                    "persisted semantic nomination must still exact-reopen canonical Conversation",
                )
                assertFalse(
                    semantic.canonicalSourceMutationPerformed,
                )
            }

            assertTrue(
                secondEncoder.sourceBatchSizes.isEmpty(),
                "semantic lookup after restart may encode the query only",
            )
            assertEquals(
                1,
                secondEncoder.queryCalls,
            )
        }
    }

    @Test
    fun incrementalCatchUpPersistsAndReloadsTheNewCanonicalGeneration() {
        ContinuityTurnFixture().use {
            fixture ->
            val firstTurn =
                fixture.commit(
                    ContinuityTurnFixture.Line(
                        human =
                            "Initial exact source semantic-slot-4.",
                        vex =
                            "Initial response semantic-slot-5.",
                        at =
                            Instant.parse(
                                "2024-01-01T00:00:00Z",
                            ),
                    ),
                )
                    .single()
            val encoder =
                RecordingEncoder(
                    "semantic.test.file-backed.append",
                )
            val backend =
                VexLifeFileBackedDenseSemanticBackend(
                    productHome = fixture.home,
                    encoder = encoder,
                    secretSource = fixedSecret(
                        21,
                    ),
                )

            fixture.store.withWriterLease {
                VexLifeContinuitySourceLocator.rebuild(
                    conversationOwner = this,
                    backend = backend,
                )
            }
            assertEquals(
                listOf(
                    2,
                ),
                encoder.sourceBatchSizes,
            )

            val appended =
                fixture.commit(
                    ContinuityTurnFixture.Line(
                        human =
                            "append-unique semantic-slot-7.",
                        vex =
                            "append response semantic-slot-8.",
                        at =
                            Instant.parse(
                                "2025-01-01T00:00:00Z",
                            ),
                    ),
                )
                    .single()

            fixture.store.withWriterLease {
                val current =
                    requireNotNull(
                        immediatePriorCompletedTurn(),
                    )
                val catchUp =
                    VexLifeContinuitySourceLocator.catchUpCommittedTurn(
                        committedHead = current.head,
                        request = appended.first,
                        response = appended.second,
                        backend = backend,
                    )
                assertEquals(
                    VexLifeContinuitySourceLocator.CatchUpState.UPDATED,
                    catchUp.state,
                )
                assertFalse(
                    catchUp.wholeHistoryEventEnumerationPerformed,
                )
            }
            assertEquals(
                listOf(
                    2,
                    2,
                ),
                encoder.sourceBatchSizes,
                "incremental persistence must encode only the appended canonical pair",
            )

            val restartedEncoder =
                RecordingEncoder(
                    "semantic.test.file-backed.append",
                )
            val restarted =
                VexLifeFileBackedDenseSemanticBackend(
                    productHome = fixture.home,
                    encoder = restartedEncoder,
                    secretSource = {
                        error(
                            "restart must reuse persisted fingerprint seed",
                        )
                    },
                )
            val ready =
                assertIs<VexLifeContinuitySourceLocator.BackendLoad.Ready>(
                    restarted.load(),
                )
            assertEquals(
                2,
                ready.generation.completedTurnCount,
            )

            fixture.store.withWriterLease {
                val located =
                    assertIs<VexLifeContinuitySourceLocator.LocateOutcome.Found>(
                        VexLifeContinuitySourceLocator.locate(
                            conversationOwner = this,
                            queryTerms =
                                listOf(
                                    "append-unique",
                                ),
                            speakerScope =
                                VexLifeContinuitySourceLocator.SpeakerScope.HUMAN,
                            backend = restarted,
                        ),
                    )
                assertEquals(
                    appended.first.eventRef,
                    located.candidates
                        .single()
                        .requestEvent
                        .eventRef,
                )
                assertFalse(
                    located.wholeHistoryEventEnumerationPerformed,
                )
            }

            assertTrue(
                restartedEncoder.sourceBatchSizes.isEmpty(),
            )
            assertTrue(
                firstTurn.first.eventRef !=
                    appended.first.eventRef,
            )
        }
    }

    @Test
    fun corruptOrRepresentationChangedSnapshotFailsClosedAndRebuildsFromCanonicalSource() {
        ContinuityTurnFixture().use {
            fixture ->
            val turn =
                fixture.commit(
                    ContinuityTurnFixture.Line(
                        human =
                            "recovery-anchor semantic-slot-11.",
                        vex =
                            "recovery response semantic-slot-12.",
                        at =
                            Instant.parse(
                                "2023-01-01T00:00:00Z",
                            ),
                    ),
                )
                    .single()
            val initial =
                VexLifeFileBackedDenseSemanticBackend(
                    productHome = fixture.home,
                    encoder =
                        RecordingEncoder(
                            "semantic.test.file-backed.recovery-a",
                        ),
                    secretSource = fixedSecret(
                        31,
                    ),
                )
            fixture.store.withWriterLease {
                VexLifeContinuitySourceLocator.rebuild(
                    conversationOwner = this,
                    backend = initial,
                )
            }

            val validSnapshot =
                Files.readString(
                    initial.persistentSnapshotPath(),
                )
            val fingerprintMarker =
                "\"requestFingerprints\":[\""
            val fingerprintStart =
                validSnapshot.indexOf(
                    fingerprintMarker,
                ) +
                    fingerprintMarker.length
            assertTrue(
                fingerprintStart >=
                    fingerprintMarker.length,
                "persisted snapshot must contain request fingerprints",
            )
            val originalFingerprint =
                validSnapshot.substring(
                    fingerprintStart,
                    fingerprintStart +
                        16,
                )
            assertTrue(
                originalFingerprint.matches(
                    Regex(
                        "[0-9a-f]{16}",
                    ),
                ),
            )
            val replacementFingerprint =
                if (
                    originalFingerprint ==
                        "0".repeat(
                            16,
                        )
                ) {
                    "1".repeat(
                        16,
                    )
                } else {
                    "0".repeat(
                        16,
                    )
                }
            Files.writeString(
                initial.persistentSnapshotPath(),
                validSnapshot.replaceRange(
                    fingerprintStart,
                    fingerprintStart +
                        16,
                    replacementFingerprint,
                ),
            )

            val validShapeCorrupt =
                VexLifeFileBackedDenseSemanticBackend(
                    productHome = fixture.home,
                    encoder =
                        RecordingEncoder(
                            "semantic.test.file-backed.recovery-a",
                        ),
                    secretSource = fixedSecret(
                        32,
                    ),
                )
            assertIs<VexLifeContinuitySourceLocator.BackendLoad.Corrupt>(
                validShapeCorrupt.load(),
                "valid-shaped generation mutation must fail closed on the canonical generation digest",
            )
            fixture.store.withWriterLease {
                VexLifeContinuitySourceLocator.rebuild(
                    conversationOwner = this,
                    backend = validShapeCorrupt,
                )
            }
            assertIs<VexLifeContinuitySourceLocator.BackendLoad.Ready>(
                validShapeCorrupt.load(),
            )

            Files.writeString(
                validShapeCorrupt.persistentSnapshotPath(),
                "{not-valid-json",
            )
            val corrupt =
                VexLifeFileBackedDenseSemanticBackend(
                    productHome = fixture.home,
                    encoder =
                        RecordingEncoder(
                            "semantic.test.file-backed.recovery-a",
                        ),
                    secretSource = fixedSecret(
                        32,
                    ),
                )
            assertIs<VexLifeContinuitySourceLocator.BackendLoad.Corrupt>(
                corrupt.load(),
            )
            fixture.store.withWriterLease {
                val before =
                    VexLifeContinuitySourceLocator.locate(
                        conversationOwner = this,
                        queryTerms =
                            listOf(
                                "recovery-anchor",
                            ),
                        speakerScope =
                            VexLifeContinuitySourceLocator.SpeakerScope.HUMAN,
                        backend = corrupt,
                    )
                assertEquals(
                    VexLifeContinuitySourceLocator.FallbackReason.CORRUPT,
                    assertIs<VexLifeContinuitySourceLocator.LocateOutcome.FallbackRequired>(
                        before,
                    )
                        .reason,
                )

                VexLifeContinuitySourceLocator.rebuild(
                    conversationOwner = this,
                    backend = corrupt,
                )
            }
            assertIs<VexLifeContinuitySourceLocator.BackendLoad.Ready>(
                corrupt.load(),
            )

            val changedRepresentation =
                VexLifeFileBackedDenseSemanticBackend(
                    productHome = fixture.home,
                    encoder =
                        RecordingEncoder(
                            "semantic.test.file-backed.recovery-b",
                        ),
                    secretSource = fixedSecret(
                        33,
                    ),
                )
            assertIs<VexLifeContinuitySourceLocator.BackendLoad.Corrupt>(
                changedRepresentation.load(),
            )
            fixture.store.withWriterLease {
                VexLifeContinuitySourceLocator.rebuild(
                    conversationOwner = this,
                    backend = changedRepresentation,
                )
            }
            assertIs<VexLifeContinuitySourceLocator.BackendLoad.Ready>(
                changedRepresentation.load(),
            )

            val restarted =
                VexLifeFileBackedDenseSemanticBackend(
                    productHome = fixture.home,
                    encoder =
                        RecordingEncoder(
                            "semantic.test.file-backed.recovery-b",
                        ),
                    secretSource = {
                        error(
                            "rebuilt representation must be restart-loadable",
                        )
                    },
                )
            fixture.store.withWriterLease {
                val located =
                    assertIs<VexLifeContinuitySourceLocator.LocateOutcome.Found>(
                        VexLifeContinuitySourceLocator.locate(
                            conversationOwner = this,
                            queryTerms =
                                listOf(
                                    "recovery-anchor",
                                ),
                            speakerScope =
                                VexLifeContinuitySourceLocator.SpeakerScope.HUMAN,
                            backend = restarted,
                        ),
                    )
                assertEquals(
                    turn.first.eventRef,
                    located.candidates
                        .single()
                        .requestEvent
                        .eventRef,
                )
            }

            val headBeforeClear =
                fixture.store.withWriterLease {
                    requireNotNull(
                        immediatePriorCompletedTurn(),
                    )
                        .head
                        .conversationHeadSha256
                }
            restarted.clear()
            assertFalse(
                Files.exists(
                    restarted.persistentSnapshotPath(),
                ),
            )
            val headAfterClear =
                fixture.store.withWriterLease {
                    requireNotNull(
                        immediatePriorCompletedTurn(),
                    )
                        .head
                        .conversationHeadSha256
                }
            assertEquals(
                headBeforeClear,
                headAfterClear,
                "derived-store deletion must not rewrite canonical Conversation",
            )
        }
    }

    private class RecordingEncoder(
        override val representationBindingRef: String,
    ) :
        VexLifeVolatileDenseSemanticBackend.Encoder {
        override val dimension: Int =
            64

        val sourceBatchSizes =
            mutableListOf<Int>()

        var queryCalls: Int =
            0

        override fun encodeSources(
            texts: List<String>,
        ): List<FloatArray> {
            sourceBatchSizes +=
                texts.size
            return texts.map {
                text ->
                vector(
                    sourceSlot(
                        text,
                    ),
                )
            }
        }

        override fun encodeQuery(
            queryTerms: List<String>,
        ): FloatArray {
            queryCalls +=
                1
            return vector(
                if (
                    "concept-seventeen" in
                        queryTerms
                ) {
                    17
                } else {
                    queryTerms
                        .joinToString(
                            "\n",
                        )
                        .hashCode()
                        .and(
                            Int.MAX_VALUE,
                        ) %
                        dimension
                },
            )
        }

        private fun sourceSlot(
            text: String,
        ): Int {
            val marker =
                "semantic-slot-"
            val index =
                text.indexOf(
                    marker,
                )
            if (
                index >=
                    0
            ) {
                val digits =
                    text.substring(
                        index +
                            marker.length,
                    )
                        .takeWhile(
                            Char::isDigit,
                        )
                digits
                    .toIntOrNull()
                    ?.let {
                        return it %
                            dimension
                    }
            }
            return text
                .hashCode()
                .and(
                    Int.MAX_VALUE,
                ) %
                dimension
        }

        private fun vector(
            slot: Int,
        ): FloatArray =
            FloatArray(
                dimension,
            )
                .also {
                    it[
                        slot
                    ] =
                        1f
                }
    }

    private fun fixedSecret(
        byte: Int,
    ): () -> ByteArray =
        {
            ByteArray(
                32,
            ) {
                byte
                    .toByte()
            }
        }
}

// [VXG RealForever]
