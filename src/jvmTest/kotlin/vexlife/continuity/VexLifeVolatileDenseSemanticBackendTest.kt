package vexlife.continuity

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import vexlife.conversation.DirectConversationStore

class VexLifeVolatileDenseSemanticBackendTest {
    @Test
    fun semanticNeighborhoodUsesDenseNominationThenExistingCanonicalReopen() {
        ContinuityTurnFixture().use {
            fixture ->
            val turns =
                fixture.commit(
                    ContinuityTurnFixture.Line(
                        human =
                            "The paper crane is named Meridian-7.",
                        vex =
                            "recorded",
                        at =
                            Instant.parse(
                                "2026-11-20T08:00:00Z",
                            ),
                    ),
                    ContinuityTurnFixture.Line(
                        human =
                            "The backup drive is labeled Atlas-14.",
                        vex =
                            "stored",
                        at =
                            Instant.parse(
                                "2026-11-20T08:05:00Z",
                            ),
                    ),
                )
            val encoder =
                ExactTextEncoder(
                    sourceVectors =
                        mapOf(
                            "The paper crane is named Meridian-7." to
                                floatArrayOf(
                                    1f,
                                    0f,
                                    0f,
                                ),
                            "recorded" to
                                floatArrayOf(
                                    0f,
                                    1f,
                                    0f,
                                ),
                            "The backup drive is labeled Atlas-14." to
                                floatArrayOf(
                                    0f,
                                    1f,
                                    0f,
                                ),
                            "stored" to
                                floatArrayOf(
                                    1f,
                                    0f,
                                    0f,
                                ),
                        ),
                    queryVectors =
                        mapOf(
                            listOf(
                                "bird-shaped",
                            ) to
                                floatArrayOf(
                                    1f,
                                    0f,
                                    0f,
                                ),
                        ),
                )
            val backend =
                VexLifeVolatileDenseSemanticBackend(
                    encoder =
                        encoder,
                    secret =
                        ByteArray(
                            32,
                        ) {
                            41
                        },
                    fingerprintBindingRef =
                        "fingerprint.test.volatile-dense",
                )

            fixture.store.withWriterLease {
                val before =
                    fixture.homeTree()
                VexLifeContinuitySourceLocator.rebuild(
                    conversationOwner =
                        this,
                    backend =
                        backend,
                )

                val human =
                    assertIs<VexLifeContinuitySourceLocator.NeighborhoodOutcome.Ready>(
                        VexLifeContinuitySourceLocator.locateNeighborhood(
                            conversationOwner =
                                this,
                            queries =
                                listOf(
                                    VexLifeContinuitySourceLocator.NeighborhoodQuery(
                                        queryRef =
                                            "human",
                                        queryTerms =
                                            listOf(
                                                "bird-shaped",
                                            ),
                                        speakerScope =
                                            VexLifeContinuitySourceLocator.SpeakerScope.HUMAN,
                                    ),
                                ),
                            backend =
                                backend,
                        ),
                    )
                assertEquals(
                    listOf(
                        turns[0].first.turnRef,
                        turns[1].first.turnRef,
                    ),
                    human.semanticMatches.map {
                        it.turnRef
                    },
                    "ranked semantic provenance must survive the backend boundary",
                )
                assertEquals(
                    turns[0].first.turnRef,
                    human.semanticMatches.first().turnRef,
                    "HUMAN scope must rank request-side vectors",
                )
                assertTrue(
                    human.exactEventFileReadCount >=
                        4,
                    "dense nomination must still descend through canonical Conversation",
                )
                assertEquals(
                    before,
                    fixture.homeTree(),
                    "volatile dense retrieval must not write canonical Home",
                )

                val vex =
                    assertIs<VexLifeContinuitySourceLocator.NeighborhoodOutcome.Ready>(
                        VexLifeContinuitySourceLocator.locateNeighborhood(
                            conversationOwner =
                                this,
                            queries =
                                listOf(
                                    VexLifeContinuitySourceLocator.NeighborhoodQuery(
                                        queryRef =
                                            "vex",
                                        queryTerms =
                                            listOf(
                                                "bird-shaped",
                                            ),
                                        speakerScope =
                                            VexLifeContinuitySourceLocator.SpeakerScope.VEX,
                                    ),
                                ),
                            backend =
                                backend,
                        ),
                    )
                assertEquals(
                    turns[1].first.turnRef,
                    vex.semanticMatches.first().turnRef,
                    "VEX scope must rank response-side vectors rather than borrowing HUMAN ranking",
                )
            }

            assertEquals(
                "semantic.test.exact-text.dense-cosine-v1",
                backend.semanticRepresentationBindingRef,
            )
        }
    }

    @Test
    fun catchUpEncodesOnlyTheNewCommittedPairAndAdvancesTheSameGeneration() {
        ContinuityTurnFixture().use {
            fixture ->
            fixture.commit(
                ContinuityTurnFixture.Line(
                    human =
                        "The paper crane is named Meridian-7.",
                    vex =
                        "recorded",
                    at =
                        Instant.parse(
                            "2026-11-20T08:00:00Z",
                        ),
                ),
            )
            val encoder =
                RecordingEncoder(
                    sourceVector = {
                        text ->
                        when {
                            "Atlas-14" in
                                text ->
                                floatArrayOf(
                                    0f,
                                    1f,
                                )

                            else ->
                                floatArrayOf(
                                    1f,
                                    0f,
                                )
                        }
                    },
                    queryVector =
                        floatArrayOf(
                            0f,
                            1f,
                        ),
                )
            val backend =
                VexLifeVolatileDenseSemanticBackend(
                    encoder =
                        encoder,
                    secret =
                        ByteArray(
                            32,
                        ) {
                            52
                        },
                    fingerprintBindingRef =
                        "fingerprint.test.volatile-dense-catchup",
                )

            fixture.store.withWriterLease {
                VexLifeContinuitySourceLocator.rebuild(
                    conversationOwner =
                        this,
                    backend =
                        backend,
                )
            }
            assertEquals(
                listOf(
                    2,
                ),
                encoder.sourceBatchSizes,
                "initial rebuild must encode exactly one request/response pair",
            )

            val appended =
                fixture.commit(
                    ContinuityTurnFixture.Line(
                        human =
                            "The backup drive is labeled Atlas-14.",
                        vex =
                            "stored",
                        at =
                            Instant.parse(
                                "2026-11-20T08:05:00Z",
                            ),
                    ),
                )
                    .single()

            fixture.store.withWriterLease {
                val head =
                    requireNotNull(
                        current().head,
                    )
                val catchUp =
                    VexLifeContinuitySourceLocator.catchUpCommittedTurn(
                        committedHead =
                            head,
                        request =
                            appended.first,
                        response =
                            appended.second,
                        backend =
                            backend,
                    )
                assertEquals(
                    VexLifeContinuitySourceLocator.CatchUpState.UPDATED,
                    catchUp.state,
                )

                val lookup =
                    backend.semanticCandidateEntryIndexes(
                        queryTerms =
                            listOf(
                                "spare storage disk",
                            ),
                        speakerScope =
                            VexLifeContinuitySourceLocator.SpeakerScope.HUMAN,
                    )
                assertEquals(
                    1,
                    lookup.entryIndexes.first(),
                    "incremental semantic catch-up must make the newly committed source immediately rankable",
                )
                assertEquals(
                    head.conversationHeadSha256,
                    lookup.sourceConversationHeadSha256,
                )
            }

            assertEquals(
                listOf(
                    2,
                    2,
                ),
                encoder.sourceBatchSizes,
                "append must encode only the new request/response pair rather than lifetime source",
            )
        }
    }

    @Test
    fun failedSourceEncodingCannotAdvanceTheDelegatedLexicalGeneration() {
        val encoder =
            object :
                VexLifeVolatileDenseSemanticBackend.Encoder {
                override val representationBindingRef =
                    "semantic.test.invalid-zero"
                override val dimension =
                    2

                override fun encodeSources(
                    texts: List<String>,
                ): List<FloatArray> =
                    texts.map {
                        floatArrayOf(
                            0f,
                            0f,
                        )
                    }

                override fun encodeQuery(
                    queryTerms: List<String>,
                ): FloatArray =
                    floatArrayOf(
                        1f,
                        0f,
                    )
            }
        val backend =
            VexLifeVolatileDenseSemanticBackend(
                encoder =
                    encoder,
                secret =
                    ByteArray(
                        32,
                    ) {
                        63
                    },
                fingerprintBindingRef =
                    "fingerprint.test.volatile-dense-failure",
            )

        ContinuityTurnFixture().use {
            fixture ->
            fixture.commit(
                ContinuityTurnFixture.Line(
                    human =
                        "Source must remain canonical.",
                    vex =
                        "yes",
                    at =
                        Instant.parse(
                            "2026-11-20T08:00:00Z",
                        ),
                ),
            )

            fixture.store.withWriterLease {
                assertFailsWith<IllegalArgumentException> {
                    VexLifeContinuitySourceLocator.rebuild(
                        conversationOwner =
                            this,
                        backend =
                            backend,
                    )
                }
            }

            assertIs<VexLifeContinuitySourceLocator.BackendLoad.Absent>(
                backend.load(),
                "representation failure must not leave a lexical generation that appears current",
            )
        }
    }

    @Test
    fun lookupIsBoundedToTheExistingProductDirectCandidateCeiling() {
        val encoder =
            RecordingEncoder(
                sourceVector = {
                    text ->
                    floatArrayOf(
                        text.substringAfterLast(
                            ".",
                            "0",
                        )
                            .toFloatOrNull()
                            ?: 0f,
                        1f,
                    )
                },
                queryVector =
                    floatArrayOf(
                        1f,
                        0f,
                    ),
            )
        val backend =
            VexLifeVolatileDenseSemanticBackend(
                encoder =
                    encoder,
                secret =
                    ByteArray(
                        32,
                    ) {
                        74
                    },
                fingerprintBindingRef =
                    "fingerprint.test.volatile-dense-bounded",
            )
        val entries =
            (0 until
                40)
                .map {
                    index ->
                    entry(
                        index,
                    )
                }
        val generation =
            generation(
                entries =
                    entries,
                fingerprintBindingRef =
                    backend.fingerprintBindingRef,
            )
        val sources =
            entries.indices.map {
                index ->
                VexLifeContinuitySourceLocator.SemanticSourceContent(
                    entryIndex =
                        index,
                    requestContent =
                        "request.$index",
                    responseContent =
                        "response.$index",
                )
            }

        backend.replaceWithSemanticSources(
            generation =
                generation,
            sourceContents =
                sources,
        )

        val lookup =
            backend.semanticCandidateEntryIndexes(
                queryTerms =
                    listOf(
                        "rank candidates",
                    ),
                speakerScope =
                    VexLifeContinuitySourceLocator.SpeakerScope.HUMAN,
            )

        assertEquals(
            VexLifeContinuitySourceLocator.MaximumNeighborhoodDirectTurns,
            lookup.entryIndexes.size,
        )
        assertEquals(
            lookup.entryIndexes.distinct(),
            lookup.entryIndexes,
        )
    }

    private class ExactTextEncoder(
        private val sourceVectors: Map<String, FloatArray>,
        private val queryVectors: Map<List<String>, FloatArray>,
    ) : VexLifeVolatileDenseSemanticBackend.Encoder {
        override val representationBindingRef =
            "semantic.test.exact-text"

        override val dimension =
            3

        override fun encodeSources(
            texts: List<String>,
        ): List<FloatArray> =
            texts.map {
                text ->
                requireNotNull(
                    sourceVectors[
                        text
                    ],
                ) {
                    "unexpected source text: $text"
                }
                    .copyOf()
            }

        override fun encodeQuery(
            queryTerms: List<String>,
        ): FloatArray =
            requireNotNull(
                queryVectors[
                    queryTerms
                ],
            ) {
                "unexpected query terms: $queryTerms"
            }
                .copyOf()
    }

    private class RecordingEncoder(
        private val sourceVector: (String) -> FloatArray,
        private val queryVector: FloatArray,
    ) : VexLifeVolatileDenseSemanticBackend.Encoder {
        override val representationBindingRef =
            "semantic.test.recording"

        override val dimension =
            queryVector.size

        val sourceBatchSizes =
            mutableListOf<Int>()

        override fun encodeSources(
            texts: List<String>,
        ): List<FloatArray> {
            sourceBatchSizes +=
                texts.size
            return texts.map {
                sourceVector(
                    it,
                )
            }
        }

        override fun encodeQuery(
            queryTerms: List<String>,
        ): FloatArray =
            queryVector.copyOf()
    }

    private fun generation(
        entries: List<VexLifeContinuitySourceLocator.Entry>,
        fingerprintBindingRef: String,
    ): VexLifeContinuitySourceLocator.Generation =
        VexLifeContinuitySourceLocator.Generation(
            schemaVersion =
                VexLifeContinuitySourceLocator.GenerationSchema,
            sourceConversationHeadSha256 =
                "a".repeat(
                    64,
                ),
            sourceConversationSequence =
                (
                    entries.size *
                        2 -
                        1
                )
                    .toLong(),
            fingerprintBindingRef =
                fingerprintBindingRef,
            sourceEventCount =
                entries.size *
                    2,
            completedTurnCount =
                entries.size,
            canonicalCompletedTurnCount =
                entries.size,
            entries =
                entries,
            sourceLifecycleBindingRef =
                "lifecycle.test.volatile-dense",
        )

    private fun entry(
        index: Int,
    ): VexLifeContinuitySourceLocator.Entry =
        VexLifeContinuitySourceLocator.Entry(
            turnRef =
                "turn.test.$index",
            canonicalOrdinal =
                index,
            coordinates =
                VexLifeContinuitySourceLocator.SourceCoordinates(
                    request =
                        address(
                            sequence =
                                index.toLong() *
                                    2,
                            ref =
                                "event.test.request.$index",
                        ),
                    response =
                        address(
                            sequence =
                                index.toLong() *
                                    2 +
                                    1,
                            ref =
                                "event.test.response.$index",
                        ),
                ),
            requestFingerprints =
                emptySet(),
            responseFingerprints =
                emptySet(),
            typedEdges =
                emptySet(),
        )

    private fun address(
        sequence: Long,
        ref: String,
    ): DirectConversationStore.EventAddress =
        DirectConversationStore.EventAddress(
            sequence =
                sequence,
            eventRef =
                ref,
            eventHash =
                "%064x".format(
                    sequence +
                        1,
                ),
        )
}

// [VXG RealForever]
