package vexlife.continuity

import java.security.SecureRandom
import java.util.UUID
import kotlin.math.sqrt

/**
 * Volatile dense-vector realization of [VexLifeContinuitySourceLocator.SemanticIndexedBackend].
 *
 * canonical Conversation bytes
 * -> injected dense representation
 * -> volatile normalized vectors
 * -> bounded ranked candidate indexes
 * -> existing Locator exact canonical reopen
 *
 * This owner is rebuildable candidate acceleration only. It does not own Conversation, Memory,
 * semantic acceptance, model selection, artifact/runtime establishment or persistence. Raw
 * Conversation bodies are offered only to [Encoder] while the Locator is already rebuilding or
 * catching up and are not retained by this backend.
 */
class VexLifeVolatileDenseSemanticBackend(
    private val encoder: Encoder,
    secret: ByteArray =
        SecureRandom().generateSeed(
            32,
        ),
    fingerprintBindingRef: String =
        "fingerprint.in-memory-dense." +
            UUID.randomUUID()
                .toString()
                .replace(
                    "-",
                    "",
                ),
) : VexLifeContinuitySourceLocator.SemanticIndexedBackend {
    companion object {
        const val DenseRankingBindingRef =
            "dense-cosine-v1"
    }

    /**
     * Representation owner supplied by construction.
     *
     * The binding ref must change whenever model, tokenizer, pooling/query formation or another
     * representation-defining relationship changes.
     */
    interface Encoder {
        val representationBindingRef: String

        val dimension: Int

        fun encodeSources(
            texts: List<String>,
        ): List<FloatArray>

        fun encodeQuery(
            queryTerms: List<String>,
        ): FloatArray
    }

    private data class DenseRow(
        val request: FloatArray,
        val response: FloatArray,
    )

    private val lexical =
        VexLifeContinuitySourceLocator.InMemoryBackend(
            secret = secret,
            fingerprintBindingRef = fingerprintBindingRef,
        )

    private var semanticGeneration:
        VexLifeContinuitySourceLocator.Generation? =
        null

    private var denseRows: List<DenseRow> =
        emptyList()

    internal data class Snapshot(
        val generation: VexLifeContinuitySourceLocator.Generation,
        /**
         * Request vector then response vector for each generation entry, in canonical entry order.
         * These are derived representation bytes only; no source text is retained here.
         */
        val vectors: List<FloatArray>,
    )

    override val fingerprintBindingRef: String
        get() =
            lexical.fingerprintBindingRef

    override val semanticRepresentationBindingRef: String =
        (
            requirePortableRef(
                encoder.representationBindingRef,
                "encoder.representationBindingRef",
            ) +
                "." +
                DenseRankingBindingRef
        )
            .also {
                requirePortableRef(
                    it,
                    "semanticRepresentationBindingRef",
                )
            }

    init {
        require(
            encoder.dimension in
                1..16_384,
        ) {
            "dense semantic encoder dimension must be between 1 and 16384"
        }
    }

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
    ): Unit =
        error(
            "dense semantic backend requires replaceWithSemanticSources",
        )

    override fun clear() {
        lexical.clear()
        semanticGeneration =
            null
        denseRows =
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
    ): Unit =
        error(
            "dense semantic backend requires replaceAfterAppendWithSemanticSource",
        )

    override fun replaceWithSemanticSources(
        generation: VexLifeContinuitySourceLocator.Generation,
        sourceContents: List<VexLifeContinuitySourceLocator.SemanticSourceContent>,
    ) {
        val ordered =
            requireCompleteSourceCoverage(
                generation =
                    generation,
                sourceContents =
                    sourceContents,
            )

        /*
         * Form and validate every vector before changing the lexical generation. Encoder failure
         * therefore cannot leave a current lexical generation paired with absent/stale semantic
         * state.
         */
        val vectors =
            normalizedVectors(
                encoder.encodeSources(
                    ordered.flatMap {
                        source ->
                        listOf(
                            source.requestContent,
                            source.responseContent,
                        )
                    },
                ),
                expectedCount =
                    ordered.size *
                        2,
                label =
                    "dense semantic source encoding",
            )

        val rows =
            vectors
                .chunked(
                    2,
                )
                .map {
                    pair ->
                    DenseRow(
                        request =
                            pair[0],
                        response =
                            pair[1],
                    )
                }

        lexical.replace(
            generation,
        )
        semanticGeneration =
            generation
        denseRows =
            rows
    }

    override fun replaceAfterAppendWithSemanticSource(
        priorGeneration: VexLifeContinuitySourceLocator.Generation,
        generation: VexLifeContinuitySourceLocator.Generation,
        appendedEntry: VexLifeContinuitySourceLocator.Entry,
        appendedSource: VexLifeContinuitySourceLocator.SemanticSourceContent,
    ) {
        require(
            semanticGeneration ==
                priorGeneration &&
                denseRows.size ==
                priorGeneration.entries.size,
        ) {
            "dense semantic append prior generation changed"
        }
        require(
            generation.entries.size ==
                priorGeneration.entries.size +
                    1 &&
                generation.entries.dropLast(
                    1,
                ) ==
                priorGeneration.entries &&
                generation.entries.last() ==
                appendedEntry &&
                appendedSource.entryIndex ==
                generation.entries.lastIndex
        ) {
            "dense semantic append does not extend the exact prior generation"
        }

        val vectors =
            normalizedVectors(
                encoder.encodeSources(
                    listOf(
                        appendedSource.requestContent,
                        appendedSource.responseContent,
                    ),
                ),
                expectedCount =
                    2,
                label =
                    "dense semantic append encoding",
            )

        val row =
            DenseRow(
                request =
                    vectors[0],
                response =
                    vectors[1],
            )

        /*
         * Representation work succeeds before the delegated lexical append. Only then are both
         * derived views advanced to the same exact generation.
         */
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
        denseRows =
            denseRows +
                row
    }

    internal fun snapshotOrNull(): Snapshot? =
        semanticGeneration
            ?.let {
                generation ->
                require(
                    denseRows.size ==
                        generation.entries.size,
                ) {
                    "dense semantic snapshot row count does not match current generation"
                }
                Snapshot(
                    generation = generation,
                    vectors =
                        denseRows.flatMap {
                            row ->
                            listOf(
                                row.request.copyOf(),
                                row.response.copyOf(),
                            )
                        },
                )
            }

    /**
     * Restore one already-validated derived snapshot without re-encoding canonical source content.
     *
     * The caller owns persistence/currentness of the snapshot. This backend still validates vector
     * count, dimension, finiteness and normalization before making the generation visible.
     */
    internal fun restore(
        snapshot: Snapshot,
    ) {
        require(
            snapshot.generation.fingerprintBindingRef ==
                fingerprintBindingRef
        ) {
            "dense semantic restore fingerprint binding changed"
        }
        val vectors =
            normalizedVectors(
                vectors = snapshot.vectors,
                expectedCount =
                    snapshot.generation.entries.size *
                        2,
                label = "dense semantic restore",
            )
        lexical.replace(
            snapshot.generation,
        )
        semanticGeneration =
            snapshot.generation
        denseRows =
            vectors
                .chunked(
                    2,
                )
                .map {
                    pair ->
                    DenseRow(
                        request = pair[0],
                        response = pair[1],
                    )
                }
    }

    override fun semanticCandidateEntryIndexes(
        queryTerms: List<String>,
        speakerScope: VexLifeContinuitySourceLocator.SpeakerScope,
    ): VexLifeContinuitySourceLocator.SemanticCandidateLookup {
        val generation =
            requireNotNull(
                semanticGeneration,
            ) {
                "dense semantic generation is absent"
            }
        require(
            denseRows.size ==
                generation.entries.size,
        ) {
            "dense semantic row count does not match current generation"
        }
        require(
            queryTerms.isNotEmpty() &&
                queryTerms.size <=
                VexLifeContinuitySourceLocator.MaximumQueryTerms &&
                queryTerms.all {
                    it.isNotBlank() &&
                        it.length <=
                        VexLifeContinuitySourceLocator.MaximumQueryTermCharacters
                },
        ) {
            "dense semantic query terms are outside Locator bounds"
        }

        val query =
            normalizedVectors(
                listOf(
                    encoder.encodeQuery(
                        queryTerms,
                    ),
                ),
                expectedCount =
                    1,
                label =
                    "dense semantic query encoding",
            )
                .single()

        val ranked =
            denseRows
                .indices
                .map {
                    index ->
                    val row =
                        denseRows[
                            index
                        ]
                    val score =
                        when (
                            speakerScope
                        ) {
                            VexLifeContinuitySourceLocator.SpeakerScope.HUMAN ->
                                dot(
                                    query,
                                    row.request,
                                )

                            VexLifeContinuitySourceLocator.SpeakerScope.VEX ->
                                dot(
                                    query,
                                    row.response,
                                )

                            VexLifeContinuitySourceLocator.SpeakerScope.ANY ->
                                maxOf(
                                    dot(
                                        query,
                                        row.request,
                                    ),
                                    dot(
                                        query,
                                        row.response,
                                    ),
                                )
                        }
                    index to
                        score
                }
                .sortedWith(
                    compareByDescending<Pair<Int, Double>> {
                        it.second
                    }
                        .thenBy {
                            it.first
                        },
                )
                .take(
                    VexLifeContinuitySourceLocator.MaximumNeighborhoodDirectTurns,
                )
                .map {
                    it.first
                }

        return VexLifeContinuitySourceLocator.SemanticCandidateLookup(
            entryIndexes =
                ranked,
            semanticLookupCount =
                1,
            representationBindingRef =
                semanticRepresentationBindingRef,
            sourceConversationHeadSha256 =
                generation.sourceConversationHeadSha256,
            sourceConversationSequence =
                generation.sourceConversationSequence,
            sourceLifecycleBindingRef =
                generation.sourceLifecycleBindingRef,
        )
    }

    private fun requireCompleteSourceCoverage(
        generation: VexLifeContinuitySourceLocator.Generation,
        sourceContents: List<VexLifeContinuitySourceLocator.SemanticSourceContent>,
    ): List<VexLifeContinuitySourceLocator.SemanticSourceContent> {
        val ordered =
            sourceContents
                .sortedBy {
                    it.entryIndex
                }
        require(
            ordered.size ==
                generation.entries.size &&
                ordered.map {
                    it.entryIndex
                } ==
                generation.entries.indices.toList()
        ) {
            "dense semantic rebuild requires exactly one source pair for every generation entry"
        }
        return ordered
    }

    private fun normalizedVectors(
        vectors: List<FloatArray>,
        expectedCount: Int,
        label: String,
    ): List<FloatArray> {
        require(
            vectors.size ==
                expectedCount,
        ) {
            "$label returned ${vectors.size} vectors; expected $expectedCount"
        }

        return vectors.mapIndexed {
            index,
            vector,
            ->
            normalize(
                vector =
                    vector,
                label =
                    "$label vector $index",
            )
        }
    }

    private fun normalize(
        vector: FloatArray,
        label: String,
    ): FloatArray {
        require(
            vector.size ==
                encoder.dimension,
        ) {
            "$label dimension ${vector.size} does not match encoder dimension ${encoder.dimension}"
        }

        var magnitudeSquared =
            0.0
        vector.forEach {
            value ->
            require(
                !value.isNaN() &&
                    !value.isInfinite(),
            ) {
                "$label contains a non-finite value"
            }
            magnitudeSquared +=
                value.toDouble() *
                    value.toDouble()
        }

        require(
            magnitudeSquared >
                0.0 &&
                !magnitudeSquared.isNaN() &&
                !magnitudeSquared.isInfinite(),
        ) {
            "$label must have one finite non-zero magnitude"
        }

        val magnitude =
            sqrt(
                magnitudeSquared,
            )

        return FloatArray(
            vector.size,
        ) {
            index ->
            (
                vector[index].toDouble() /
                    magnitude
            )
                .toFloat()
        }
    }

    private fun dot(
        left: FloatArray,
        right: FloatArray,
    ): Double {
        require(
            left.size ==
                right.size,
        ) {
            "dense semantic vectors have different dimensions"
        }

        var result =
            0.0
        left.indices.forEach {
            index ->
            result +=
                left[index].toDouble() *
                    right[index].toDouble()
        }
        return result
    }

    private fun requirePortableRef(
        value: String,
        field: String,
    ): String {
        require(
            value.matches(
                Regex(
                    "[a-z][a-z0-9.-]*",
                ),
            ),
        ) {
            "$field must be one portable ref"
        }
        return value
    }
}

// [VXG RealForever]
