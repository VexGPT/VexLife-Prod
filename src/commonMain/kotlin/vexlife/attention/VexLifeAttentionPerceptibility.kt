package vexlife.attention

import kotlinx.serialization.Serializable

/**
 * Read-only source-addressed perceptibility for Attention Convergence.
 *
 * This owner answers one narrow question:
 *
 * ```text
 * Which already-owned current truths can be represented as small source-addressed candidates
 * without copying their source bodies into present cognition?
 * ```
 *
 * It does not discover connections, rank sources, decide domain significance, admit prompt bytes,
 * grant effects, acknowledge anything, or own source currentness. Callers supply candidates formed
 * from their rightful source owners. [form] only validates and projects those candidates into one
 * deterministic inspectable surface.
 *
 * A later Attention Convergence stage may relate these candidates to current human activity or to a
 * source-owned attention requirement. Those semantics are intentionally absent here.
 */
object VexLifeAttentionPerceptibility {
    const val SurfaceSchema =
        "vexlife-prod.attention-perceptibility-surface/v1"

    /**
     * A candidate carries a compact owner-authored meaning, never the source body itself.
     *
     * The exact operating envelope will be pressure-tested later. A1 keeps one candidate below a
     * small fixed source-projection ceiling so a single source cannot turn this relation into an
     * unbounded source dump.
     */
    const val MaximumCompactMeaningCharacters = 1_024

    /**
     * Whether the candidate's deeper source coordinate can currently be followed.
     *
     * This is only perceptibility/reopen standing. It does not reinterpret the domain's own
     * [Candidate.sourceStandingRef] and it grants no read/effect authority.
     */
    @Serializable
    enum class Reachability {
        REACHABLE,
        HELD,
        UNAVAILABLE,
        UNKNOWN,
    }

    /**
     * The smallest A1 unit that may later participate in present-cognition convergence.
     *
     * [sourceBindingRefs] carries the exact source-owned version/currentness/evidence coordinates
     * needed to keep this projection from floating free of its source. Their internal semantics stay
     * with that owner.
     *
     * [deeperSourceRef] is an address, not an executable request. A later consumer must still use the
     * rightful capability/source owner to reopen it.
     */
    @Serializable
    data class Candidate(
        val candidateRef: String,
        val ownerRef: String,
        val sourceRef: String,
        val relationshipRef: String,
        val subjectScopeRefs: List<String>,
        val sourceBindingRefs: List<String>,
        val sourceStandingRef: String,
        val compactMeaning: String,
        val deeperSourceRef: String,
        val reachability: Reachability,
    ) {
        init {
            requireRef(
                candidateRef,
                "candidateRef",
            )
            requireRef(
                ownerRef,
                "ownerRef",
            )
            requireRef(
                sourceRef,
                "sourceRef",
            )
            requireRef(
                relationshipRef,
                "relationshipRef",
            )
            requireRefs(
                subjectScopeRefs,
                "subjectScopeRefs",
                allowEmpty = true,
            )
            requireRefs(
                sourceBindingRefs,
                "sourceBindingRefs",
                allowEmpty = false,
            )
            requireRef(
                sourceStandingRef,
                "sourceStandingRef",
            )
            require(
                compactMeaning.isNotBlank() &&
                    compactMeaning.length <=
                    MaximumCompactMeaningCharacters,
            ) {
                "attention candidate compactMeaning must be 1..$MaximumCompactMeaningCharacters characters"
            }
            requireRef(
                deeperSourceRef,
                "deeperSourceRef",
            )
        }
    }

    /**
     * Exact A1 projection over candidates supplied by existing source/topology owners.
     *
     * Ordering is lexical by stable candidate identity only so equivalent inputs serialize
     * deterministically. The order is not priority, relevance, urgency, or source significance.
     */
    @Serializable
    data class Surface(
        val schemaVersion: String = SurfaceSchema,
        val candidates: List<Candidate>,
        val sourceRefs: List<String>,
        val effectAuthorityGranted: Boolean = false,
    ) {
        init {
            require(
                schemaVersion ==
                    SurfaceSchema,
            ) {
                "attention perceptibility surface schema is not supported"
            }
            require(
                candidates
                    .map(
                        Candidate::candidateRef,
                    )
                    .distinct()
                    .size ==
                    candidates.size,
            ) {
                "attention perceptibility candidateRef must be unique"
            }
            requireRefs(
                sourceRefs,
                "sourceRefs",
                allowEmpty = false,
            )
            require(
                !effectAuthorityGranted,
            ) {
                "attention perceptibility surface cannot grant effect authority"
            }
        }
    }

    fun form(
        candidates: List<Candidate>,
        sourceRefs: List<String>,
    ): Surface =
        Surface(
            candidates =
                candidates.sortedBy {
                    it.candidateRef
                },
            sourceRefs =
                sourceRefs
                    .distinct()
                    .sorted(),
        )

    private fun requireRefs(
        values: List<String>,
        label: String,
        allowEmpty: Boolean,
    ) {
        require(
            allowEmpty ||
                values.isNotEmpty(),
        ) {
            "$label must not be empty"
        }
        require(
            values.distinct().size ==
                values.size,
        ) {
            "$label must be unique"
        }
        values.forEachIndexed {
            index,
            value,
            ->
            requireRef(
                value,
                "$label[$index]",
            )
        }
    }

    private fun requireRef(
        value: String,
        label: String,
    ) {
        require(
            value.matches(
                Regex(
                    "[A-Za-z0-9][A-Za-z0-9._:/-]*",
                ),
            ),
        ) {
            "$label must be one portable stable ref"
        }
    }
}

// [VXG RealForever]
