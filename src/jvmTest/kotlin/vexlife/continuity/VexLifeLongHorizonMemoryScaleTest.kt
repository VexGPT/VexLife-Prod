package vexlife.continuity

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import vexlife.contract.VexContractJson
import vexlife.conversation.CanonicalLivedContextFixture
import vexlife.conversation.DirectConversationAttemptIndex
import vexlife.conversation.DirectConversationStore
import vexlife.home.CanonicalVexLifeHomeFixture

/**
 * Property-gated long-horizon measurement over the real canonical Conversation + Locator owners.
 *
 * Ordinary jvmTest returns immediately. LH6 SourceOwner enables -Dvexlife.lh6.scale=true and reads
 * the emitted report. Timing values are observations only; pass/fail is relationship-based.
 */
class VexLifeLongHorizonMemoryScaleTest {
    companion object {
        private const val ScaleProperty = "vexlife.lh6.scale"
        private const val ReportProperty = "vexlife.lh6.report"
        private const val InitialCompletedTurns = 3_652
        private const val DenseDimension = 1_024
        private const val SemanticTargetOrdinal = 17
        private const val LexicalAnchor = "five-year-anchor-zero"
        private const val SemanticQuery = "conceptseventeen"
    }

    private data class Timed<T>(
        val value: T,
        val nanos: Long,
    ) {
        val millis: Double
            get() = nanos / 1_000_000.0
    }

    private data class Corpus(
        val first: Pair<DirectConversationStore.Event, DirectConversationStore.Event>,
        val semanticTarget: Pair<DirectConversationStore.Event, DirectConversationStore.Event>,
        val last: Pair<DirectConversationStore.Event, DirectConversationStore.Event>,
        val lastHead: DirectConversationStore.Head,
        val admissionWholeHistoryScans: Int,
    )

    private data class HomeStats(
        val fileCount: Long,
        val bytes: Long,
    )

    @Test
    fun fiveYearCanonicalScaleRebuildRecoveryMeasurement() {
        if (System.getProperty(ScaleProperty) != "true") {
            return
        }

        val parent =
            Files.createTempDirectory(
                "vexlife-prod-lh6-five-year-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                ".vexlife",
            )
                .also {
                    CanonicalVexLifeHomeFixture.form(
                        it,
                    )
                }
        val attemptIndex =
            DirectConversationAttemptIndex()
        val store =
            DirectConversationStore(
                home,
                attemptIndex,
            )

        try {
            val corpusTimed =
                measured {
                    formCorpus(
                        home = home,
                        store = store,
                    )
                }
            val corpus =
                corpusTimed.value
            assertEquals(
                1,
                corpus.admissionWholeHistoryScans,
                "only the empty first admission may need a whole-history fallback before the volatile attempt index becomes current",
            )

            val homeAfterCorpus =
                homeStats(
                    home,
                )

            val lexicalBackend =
                VexLifeContinuitySourceLocator.InMemoryBackend(
                    secret =
                        ByteArray(
                            32,
                        ) {
                            61
                        },
                    fingerprintBindingRef =
                        "fingerprint.test.lh6-five-year",
                )
            val lexicalRebuild =
                measured {
                    store.withWriterLease {
                        VexLifeContinuitySourceLocator.rebuild(
                            conversationOwner = this,
                            backend = lexicalBackend,
                        )
                    }
                }
            assertEquals(
                InitialCompletedTurns,
                lexicalRebuild.value.completedTurnCount,
            )
            assertTrue(
                lexicalRebuild.value.wholeHistoryEventEnumerationPerformed,
            )
            assertFalse(
                lexicalRebuild.value.canonicalSourceMutationPerformed,
            )

            val oldestLexical =
                measured {
                    store.withWriterLease {
                        VexLifeContinuitySourceLocator.locate(
                            conversationOwner = this,
                            queryTerms =
                                listOf(
                                    LexicalAnchor,
                                ),
                            speakerScope =
                                VexLifeContinuitySourceLocator.SpeakerScope.HUMAN,
                            backend = lexicalBackend,
                        )
                    }
                }
            val oldestFound =
                assertIs<VexLifeContinuitySourceLocator.LocateOutcome.Found>(
                    oldestLexical.value,
                )
            assertEquals(
                corpus.first.first.eventRef,
                oldestFound.candidates.single().requestEvent.eventRef,
            )
            assertFalse(
                oldestFound.wholeHistoryEventEnumerationPerformed,
            )
            assertEquals(
                0,
                oldestFound.derivedEntryScanCount,
            )
            assertTrue(
                oldestFound.derivedPostingLookupCount >
                    0,
            )

            val appendTimed =
                measured {
                    appendOne(
                        home = home,
                        store = store,
                        ordinal = InitialCompletedTurns,
                        at =
                            Instant.parse(
                                "2026-01-01T00:00:00Z",
                            ),
                        human =
                            "Post-five-year append zephyr-catchup-only semantic-slot-91.",
                        vex =
                            "Post-five-year response.",
                    )
                }
            val appended =
                appendTimed.value

            val stale =
                measured {
                    store.withWriterLease {
                        VexLifeContinuitySourceLocator.locate(
                            conversationOwner = this,
                            queryTerms =
                                listOf(
                                    "zephyr-catchup-only",
                                ),
                            speakerScope =
                                VexLifeContinuitySourceLocator.SpeakerScope.HUMAN,
                            backend = lexicalBackend,
                        )
                    }
                }
            assertEquals(
                VexLifeContinuitySourceLocator.FallbackReason.STALE,
                assertIs<VexLifeContinuitySourceLocator.LocateOutcome.FallbackRequired>(
                    stale.value,
                )
                    .reason,
            )

            val catchUp =
                measured {
                    store.withWriterLease {
                        VexLifeContinuitySourceLocator.catchUpCommittedTurn(
                            committedHead = appended.lastHead,
                            request = appended.first.first,
                            response = appended.first.second,
                            backend = lexicalBackend,
                        )
                    }
                }
            assertEquals(
                VexLifeContinuitySourceLocator.CatchUpState.UPDATED,
                catchUp.value.state,
            )
            assertFalse(
                catchUp.value.wholeHistoryEventEnumerationPerformed,
            )
            assertEquals(
                InitialCompletedTurns +
                    1,
                catchUp.value.completedTurnCount,
            )

            val caughtUpLookup =
                measured {
                    store.withWriterLease {
                        VexLifeContinuitySourceLocator.locate(
                            conversationOwner = this,
                            queryTerms =
                                listOf(
                                    "zephyr-catchup-only",
                                ),
                            speakerScope =
                                VexLifeContinuitySourceLocator.SpeakerScope.HUMAN,
                            backend = lexicalBackend,
                        )
                    }
                }
            assertEquals(
                appended.first.first.eventRef,
                assertIs<VexLifeContinuitySourceLocator.LocateOutcome.Found>(
                    caughtUpLookup.value,
                )
                    .candidates
                    .single()
                    .requestEvent
                    .eventRef,
            )

            val currentGeneration =
                assertIs<VexLifeContinuitySourceLocator.BackendLoad.Ready>(
                    lexicalBackend.load(),
                )
                    .generation
            val sourceLightPortableBytes =
                sourceLightPortableEncodingBytes(
                    currentGeneration,
                )

            val missingBackend =
                VexLifeContinuitySourceLocator.InMemoryBackend(
                    secret =
                        ByteArray(
                            32,
                        ) {
                            67
                        },
                    fingerprintBindingRef =
                        "fingerprint.test.lh6-missing",
                )
            val missingFallback =
                measured {
                    store.withWriterLease {
                        VexLifeContinuitySourceLocator.locate(
                            conversationOwner = this,
                            queryTerms =
                                listOf(
                                    LexicalAnchor,
                                ),
                            speakerScope =
                                VexLifeContinuitySourceLocator.SpeakerScope.HUMAN,
                            backend = missingBackend,
                        )
                    }
                }
            assertEquals(
                VexLifeContinuitySourceLocator.FallbackReason.ABSENT,
                assertIs<VexLifeContinuitySourceLocator.LocateOutcome.FallbackRequired>(
                    missingFallback.value,
                )
                    .reason,
            )
            val coldRebuild =
                measured {
                    store.withWriterLease {
                        VexLifeContinuitySourceLocator.rebuild(
                            conversationOwner = this,
                            backend = missingBackend,
                        )
                    }
                }
            val coldLookup =
                measured {
                    store.withWriterLease {
                        VexLifeContinuitySourceLocator.locate(
                            conversationOwner = this,
                            queryTerms =
                                listOf(
                                    LexicalAnchor,
                                ),
                            speakerScope =
                                VexLifeContinuitySourceLocator.SpeakerScope.HUMAN,
                            backend = missingBackend,
                        )
                    }
                }
            assertEquals(
                corpus.first.first.eventRef,
                assertIs<VexLifeContinuitySourceLocator.LocateOutcome.Found>(
                    coldLookup.value,
                )
                    .candidates
                    .single()
                    .requestEvent
                    .eventRef,
            )

            val corruptBackend =
                CorruptibleBackend()
            corruptBackend.corrupt =
                true
            val corruptFallback =
                measured {
                    store.withWriterLease {
                        VexLifeContinuitySourceLocator.locate(
                            conversationOwner = this,
                            queryTerms =
                                listOf(
                                    LexicalAnchor,
                                ),
                            speakerScope =
                                VexLifeContinuitySourceLocator.SpeakerScope.HUMAN,
                            backend = corruptBackend,
                        )
                    }
                }
            assertEquals(
                VexLifeContinuitySourceLocator.FallbackReason.CORRUPT,
                assertIs<VexLifeContinuitySourceLocator.LocateOutcome.FallbackRequired>(
                    corruptFallback.value,
                )
                    .reason,
            )
            val corruptRebuild =
                measured {
                    store.withWriterLease {
                        VexLifeContinuitySourceLocator.rebuild(
                            conversationOwner = this,
                            backend = corruptBackend,
                        )
                    }
                }
            val corruptRecovered =
                store.withWriterLease {
                    VexLifeContinuitySourceLocator.locate(
                        conversationOwner = this,
                        queryTerms =
                            listOf(
                                LexicalAnchor,
                            ),
                        speakerScope =
                            VexLifeContinuitySourceLocator.SpeakerScope.HUMAN,
                        backend = corruptBackend,
                    )
                }
            assertEquals(
                corpus.first.first.eventRef,
                assertIs<VexLifeContinuitySourceLocator.LocateOutcome.Found>(
                    corruptRecovered,
                )
                    .candidates
                    .single()
                    .requestEvent
                    .eventRef,
            )

            val semanticBackend =
                VexLifeVolatileDenseSemanticBackend(
                    encoder =
                        ScaleEncoder(),
                    secret =
                        ByteArray(
                            32,
                        ) {
                            71
                        },
                    fingerprintBindingRef =
                        "fingerprint.test.lh6-dense",
                )
            val semanticRebuild =
                measured {
                    store.withWriterLease {
                        VexLifeContinuitySourceLocator.rebuild(
                            conversationOwner = this,
                            backend = semanticBackend,
                        )
                    }
                }
            val semanticLookup =
                measured {
                    store.withWriterLease {
                        VexLifeContinuitySourceLocator.locateNeighborhood(
                            conversationOwner = this,
                            queries =
                                listOf(
                                    VexLifeContinuitySourceLocator.NeighborhoodQuery(
                                        queryRef =
                                            "lh6-semantic",
                                        queryTerms =
                                            listOf(
                                                SemanticQuery,
                                            ),
                                        speakerScope =
                                            VexLifeContinuitySourceLocator.SpeakerScope.HUMAN,
                                    ),
                                ),
                            backend = semanticBackend,
                        )
                    }
                }
            val semanticReady =
                assertIs<VexLifeContinuitySourceLocator.NeighborhoodOutcome.Ready>(
                    semanticLookup.value,
                )
            assertTrue(
                corpus.semanticTarget.first.turnRef in
                    semanticReady.semanticDirectTurnRefs,
                "lexically absent semantic query must nominate and exact-reopen the old target",
            )
            assertTrue(
                semanticReady.derivedSemanticLookupCount >
                    0,
            )
            assertTrue(
                semanticReady.exactEventFileReadCount >
                    0,
            )
            assertFalse(
                semanticReady.canonicalSourceMutationPerformed,
            )

            val finalHead =
                store.withWriterLease {
                    requireNotNull(
                        immediatePriorCompletedTurn(),
                    )
                        .head
                }
            assertEquals(
                appended.lastHead.conversationHeadSha256,
                finalHead.conversationHeadSha256,
                "derived-index work must not rewrite canonical Conversation",
            )

            val homeFinal =
                homeStats(
                    home,
                )
            assertEquals(
                homeAfterCorpus.fileCount +
                    filesAddedByOneTurn(
                        homeAfterCorpus,
                        homeFinal,
                    ),
                homeFinal.fileCount,
            )

            val completedTurns =
                InitialCompletedTurns +
                    1
            val semanticRawFloatBytes =
                completedTurns.toLong() *
                    2L *
                    DenseDimension.toLong() *
                    4L

            val report =
                buildJsonObject {
                    put(
                        "schemaVersion",
                        "vexlife-prod.lh6-five-year-scale-report/v1",
                    )
                    put(
                        "initialCompletedTurns",
                        InitialCompletedTurns,
                    )
                    put(
                        "finalCompletedTurns",
                        completedTurns,
                    )
                    put(
                        "timestampSpanStart",
                        "2021-01-01T00:00:00Z",
                    )
                    put(
                        "timestampSpanEnd",
                        "2026-01-01T00:00:00Z",
                    )
                    put(
                        "corpusFormationMillis",
                        corpusTimed.millis,
                    )
                    put(
                        "corpusAdmissionWholeHistoryScans",
                        corpus.admissionWholeHistoryScans,
                    )
                    put(
                        "canonicalHomeFiles",
                        homeFinal.fileCount,
                    )
                    put(
                        "canonicalHomeBytes",
                        homeFinal.bytes,
                    )
                    put(
                        "sourceLightPortableEncodingBytesEstimate",
                        sourceLightPortableBytes,
                    )
                    put(
                        "denseDimension",
                        DenseDimension,
                    )
                    put(
                        "denseRawFloatPayloadBytes",
                        semanticRawFloatBytes,
                    )
                    put(
                        "lexicalRebuildMillis",
                        lexicalRebuild.millis,
                    )
                    put(
                        "oldestLexicalLookupMillis",
                        oldestLexical.millis,
                    )
                    put(
                        "oldestLexicalExactEventReads",
                        oldestFound.exactEventFileReadCount,
                    )
                    put(
                        "oldestLexicalExactHeadReads",
                        oldestFound.exactHeadFileReadCount,
                    )
                    put(
                        "oldestLexicalWholeHeadTraversal",
                        oldestFound.wholeHistoryHeadTraversalPerformed,
                    )
                    put(
                        "oldestLexicalDerivedEntryScans",
                        oldestFound.derivedEntryScanCount,
                    )
                    put(
                        "oldestLexicalPostingLookups",
                        oldestFound.derivedPostingLookupCount,
                    )
                    put(
                        "appendMillis",
                        appendTimed.millis,
                    )
                    put(
                        "staleDetectionMillis",
                        stale.millis,
                    )
                    put(
                        "catchUpMillis",
                        catchUp.millis,
                    )
                    put(
                        "caughtUpLookupMillis",
                        caughtUpLookup.millis,
                    )
                    put(
                        "missingDetectionMillis",
                        missingFallback.millis,
                    )
                    put(
                        "coldRebuildMillis",
                        coldRebuild.millis,
                    )
                    put(
                        "coldOldestLookupMillis",
                        coldLookup.millis,
                    )
                    put(
                        "corruptDetectionMillis",
                        corruptFallback.millis,
                    )
                    put(
                        "corruptRebuildMillis",
                        corruptRebuild.millis,
                    )
                    put(
                        "semanticRebuildMillis",
                        semanticRebuild.millis,
                    )
                    put(
                        "semanticLookupMillis",
                        semanticLookup.millis,
                    )
                    put(
                        "semanticExactEventReads",
                        semanticReady.exactEventFileReadCount,
                    )
                    put(
                        "semanticExactHeadReads",
                        semanticReady.exactHeadFileReadCount,
                    )
                    put(
                        "semanticWholeHeadTraversal",
                        semanticReady.wholeHistoryHeadTraversalPerformed,
                    )
                    put(
                        "semanticDerivedLookupCount",
                        semanticReady.derivedSemanticLookupCount,
                    )
                    put(
                        "semanticReturnedTurnCount",
                        semanticReady.semanticDirectTurnRefs.size,
                    )
                    put(
                        "canonicalSourceMutationPerformed",
                        false,
                    )
                    put(
                        "persistentStoreSelected",
                        false,
                    )
                }

            val reportPath =
                Path.of(
                    System.getProperty(
                        ReportProperty,
                        "build/lh6/lh6-scale-report.json",
                    ),
                )
                    .toAbsolutePath()
            Files.createDirectories(
                requireNotNull(
                    reportPath.parent,
                ),
            )
            val reportText =
                VexContractJson.canonicalEncodeElement(
                    report,
                ) +
                    "\n"
            Files.writeString(
                reportPath,
                reportText,
                StandardCharsets.UTF_8,
            )
            println(
                "VEXLIFE_LH6_REPORT_PATH=" +
                    reportPath,
            )
            println(
                "VEXLIFE_LH6_REPORT=" +
                    reportText.trim(),
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    private fun formCorpus(
        home: Path,
        store: DirectConversationStore,
    ): Corpus {
        var first:
            Pair<DirectConversationStore.Event, DirectConversationStore.Event>? =
            null
        var semanticTarget:
            Pair<DirectConversationStore.Event, DirectConversationStore.Event>? =
            null
        var last:
            Pair<DirectConversationStore.Event, DirectConversationStore.Event>? =
            null
        var lastHead:
            DirectConversationStore.Head? =
            null
        var admissionWholeHistoryScans =
            0
        val start =
            Instant.parse(
                "2021-01-01T00:00:00Z",
            )

        store.withWriterLease {
            repeat(
                InitialCompletedTurns,
            ) {
                ordinal ->
                val at =
                    start.plusSeconds(
                        ordinal.toLong() *
                            43_200L,
                    )
                val requestMessageRef =
                    "message.lh6.human." +
                        "%05d".format(
                            ordinal,
                        )
                val admission =
                    currentForRequestAdmission(
                        requestMessageRef,
                    )
                if (
                    admission.wholeHistoryEventEnumerationPerformed
                ) {
                    admissionWholeHistoryScans +=
                        1
                }
                val pair =
                    appendPair(
                        owner = this,
                        home = home,
                        ordinal = ordinal,
                        at = at,
                        admission = admission,
                        human =
                            buildString {
                                append(
                                    "LH6 five-year turn ",
                                )
                                append(
                                    ordinal,
                                )
                                append(
                                    " day ",
                                )
                                append(
                                    ordinal /
                                        2,
                                )
                                append(
                                    " lexical-key-",
                                )
                                append(
                                    ordinal,
                                )
                                append(
                                    " semantic-slot-",
                                )
                                append(
                                    ordinal %
                                        DenseDimension,
                                )
                                append(
                                    ".",
                                )
                                if (
                                    ordinal ==
                                        0
                                ) {
                                    append(
                                        " ",
                                    )
                                    append(
                                        LexicalAnchor,
                                    )
                                    append(
                                        ".",
                                    )
                                }
                            },
                        vex =
                            "LH6 response " +
                                ordinal +
                                " acknowledged.",
                    )
                if (
                    ordinal ==
                        0
                ) {
                    first =
                        pair.first
                }
                if (
                    ordinal ==
                        SemanticTargetOrdinal
                ) {
                    semanticTarget =
                        pair.first
                }
                last =
                    pair.first
                lastHead =
                    pair.second
            }
        }

        return Corpus(
            first =
                requireNotNull(
                    first,
                ),
            semanticTarget =
                requireNotNull(
                    semanticTarget,
                ),
            last =
                requireNotNull(
                    last,
                ),
            lastHead =
                requireNotNull(
                    lastHead,
                ),
            admissionWholeHistoryScans =
                admissionWholeHistoryScans,
        )
    }

    private data class Appended(
        val first: Pair<DirectConversationStore.Event, DirectConversationStore.Event>,
        val lastHead: DirectConversationStore.Head,
    )

    private fun appendOne(
        home: Path,
        store: DirectConversationStore,
        ordinal: Int,
        at: Instant,
        human: String,
        vex: String,
    ): Appended =
        store.withWriterLease {
            val messageRef =
                "message.lh6.human." +
                    "%05d".format(
                        ordinal,
                    )
            val admission =
                currentForRequestAdmission(
                    messageRef,
                )
            assertFalse(
                admission.wholeHistoryEventEnumerationPerformed,
                "current attempt index should admit one append without lifetime enumeration",
            )
            val result =
                appendPair(
                    owner = this,
                    home = home,
                    ordinal = ordinal,
                    at = at,
                    admission = admission,
                    human = human,
                    vex = vex,
                )
            Appended(
                first = result.first,
                lastHead = result.second,
            )
        }

    private fun appendPair(
        owner: DirectConversationStore.Locked,
        home: Path,
        ordinal: Int,
        at: Instant,
        admission: DirectConversationStore.RequestAdmission,
        human: String,
        vex: String,
    ): Pair<
        Pair<DirectConversationStore.Event, DirectConversationStore.Event>,
        DirectConversationStore.Head,
    > {
        val suffix =
            "%05d".format(
                ordinal,
            )
        val request =
            owner.beginRequest(
                turnRef =
                    "turn.lh6." +
                        suffix,
                requestMessageRef =
                    "message.lh6.human." +
                        suffix,
                content = human,
                formedAt = at,
                admissionSnapshot = admission,
            )
        val response =
            owner.appendResponse(
                request = request,
                responseMessageRef =
                    "message.lh6.vex." +
                        suffix,
                content = vex,
                model = "default_model",
                endpointOrigin = "http://127.0.0.1:8765",
                runtimeObservationRef =
                    "observation.lh6." +
                        suffix,
                invocationEvidenceRef =
                    "evidence.lh6." +
                        suffix,
                formedAt =
                    at.plusSeconds(
                        1,
                    ),
            )
        val context =
            CanonicalLivedContextFixture.write(
                productHome = home,
                request = request,
                response = response,
                formedAt =
                    at.plusSeconds(
                        2,
                    ),
            )
        val head =
            owner.commitCompletedTurn(
                request = request,
                response = response,
                contextSha256 = context.sha256,
                formedAt =
                    at.plusSeconds(
                        3,
                    ),
            )
        return (request to response) to
            head
    }

    private class ScaleEncoder :
        VexLifeVolatileDenseSemanticBackend.Encoder {
        override val representationBindingRef: String =
            "semantic.test.lh6-five-year-1024"

        override val dimension: Int =
            DenseDimension

        override fun encodeSources(
            texts: List<String>,
        ): List<FloatArray> =
            texts.map {
                text ->
                vector(
                    sourceSlot(
                        text,
                    ),
                )
            }

        override fun encodeQuery(
            queryTerms: List<String>,
        ): FloatArray =
            vector(
                if (
                    SemanticQuery in
                        queryTerms
                ) {
                    SemanticTargetOrdinal %
                        DenseDimension
                } else {
                    queryTerms
                        .joinToString(
                            "\n",
                        )
                        .hashCode()
                        .and(
                            Int.MAX_VALUE,
                        ) %
                        DenseDimension
                },
            )

        private fun sourceSlot(
            text: String,
        ): Int {
            val marker =
                "semantic-slot-"
            val start =
                text.indexOf(
                    marker,
                )
            if (
                start >=
                    0
            ) {
                val digits =
                    text.substring(
                        start +
                            marker.length,
                    )
                        .takeWhile(
                            Char::isDigit,
                        )
                digits
                    .toIntOrNull()
                    ?.let {
                        return it %
                            DenseDimension
                    }
            }
            return text
                .hashCode()
                .and(
                    Int.MAX_VALUE,
                ) %
                DenseDimension
        }

        private fun vector(
            slot: Int,
        ): FloatArray =
            FloatArray(
                DenseDimension,
            )
                .also {
                    it[
                        slot
                    ] =
                        1f
                }
    }

    private class CorruptibleBackend :
        VexLifeContinuitySourceLocator.Backend {
        private val delegate =
            VexLifeContinuitySourceLocator.InMemoryBackend(
                secret =
                    ByteArray(
                        32,
                    ) {
                        73
                    },
                fingerprintBindingRef =
                    "fingerprint.test.lh6-corrupt",
            )

        var corrupt: Boolean =
            false

        override val fingerprintBindingRef: String
            get() =
                delegate.fingerprintBindingRef

        override fun fingerprint(
            value: String,
        ): String =
            delegate.fingerprint(
                value,
            )

        override fun load(): VexLifeContinuitySourceLocator.BackendLoad =
            if (
                corrupt
            ) {
                VexLifeContinuitySourceLocator.BackendLoad.Corrupt(
                    "synthetic LH6 corruption",
                )
            } else {
                delegate.load()
            }

        override fun replace(
            generation: VexLifeContinuitySourceLocator.Generation,
        ) {
            delegate.replace(
                generation,
            )
            corrupt =
                false
        }

        override fun clear() {
            delegate.clear()
            corrupt =
                false
        }
    }

    private fun sourceLightPortableEncodingBytes(
        generation: VexLifeContinuitySourceLocator.Generation,
    ): Long {
        val text =
            buildString {
                appendLine(
                    generation.schemaVersion,
                )
                appendLine(
                    generation.sourceConversationHeadSha256,
                )
                appendLine(
                    generation.sourceConversationSequence,
                )
                appendLine(
                    generation.fingerprintBindingRef,
                )
                appendLine(
                    generation.sourceLifecycleBindingRef,
                )
                appendLine(
                    generation.sourceEventCount,
                )
                appendLine(
                    generation.completedTurnCount,
                )
                appendLine(
                    generation.canonicalCompletedTurnCount,
                )
                generation.entries.forEach {
                    entry ->
                    append(
                        entry.turnRef,
                    )
                    append(
                        '|',
                    )
                    append(
                        entry.canonicalOrdinal,
                    )
                    append(
                        '|',
                    )
                    appendAddress(
                        entry.coordinates.request,
                    )
                    append(
                        '|',
                    )
                    appendAddress(
                        entry.coordinates.response,
                    )
                    append(
                        '|',
                    )
                    append(
                        entry.requestFingerprints
                            .sorted()
                            .joinToString(
                                ",",
                            ),
                    )
                    append(
                        '|',
                    )
                    append(
                        entry.responseFingerprints
                            .sorted()
                            .joinToString(
                                ",",
                            ),
                    )
                    append(
                        '|',
                    )
                    append(
                        entry.typedEdges
                            .map {
                                it.name
                            }
                            .sorted()
                            .joinToString(
                                ",",
                            ),
                    )
                    appendLine()
                }
            }
        return text
            .toByteArray(
                StandardCharsets.UTF_8,
            )
            .size
            .toLong()
    }

    private fun StringBuilder.appendAddress(
        address: DirectConversationStore.EventAddress,
    ) {
        append(
            address.sequence,
        )
        append(
            ':',
        )
        append(
            address.eventRef,
        )
        append(
            ':',
        )
        append(
            address.eventHash,
        )
    }

    private fun homeStats(
        home: Path,
    ): HomeStats {
        val files =
            Files.walk(
                home,
            )
                .use {
                    paths ->
                    paths
                        .filter(
                            Files::isRegularFile,
                        )
                        .toList()
                }
        return HomeStats(
            fileCount =
                files.size
                    .toLong(),
            bytes =
                files.sumOf {
                    Files.size(
                        it,
                    )
                },
        )
    }

    /**
     * The final corpus has one additional committed turn. The exact count is intentionally measured
     * rather than hard-coded because canonical Home may add bounded owner metadata around one turn.
     */
    private fun filesAddedByOneTurn(
        before: HomeStats,
        after: HomeStats,
    ): Long =
        after.fileCount -
            before.fileCount

    private inline fun <T> measured(
        block: () -> T,
    ): Timed<T> {
        val started =
            System.nanoTime()
        val value =
            block()
        return Timed(
            value = value,
            nanos =
                System.nanoTime() -
                    started,
        )
    }
}

// [VXG RealForever]
