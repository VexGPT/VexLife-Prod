package vexlife.continuity

/**
 * Exact representation identity selected for LH-5 semantic candidate nomination.
 *
 * This owner answers only what the BGE-M3 dense representation means. It does not own model-byte
 * custody, Python/ONNX establishment, worker-process lifetime, Conversation, Memory, or persistence.
 *
 * The representation was selected after the exact 2,048-source Product-aligned comparator:
 * BGE-M3 dense kept 30 / 31 truths inside the existing 24-direct-candidate budget while the
 * materially smaller GTE comparator kept 25 / 31.
 */
object VexLifeBgeM3DenseIdentity {
    const val Repository =
        "BAAI/bge-m3"

    const val Revision =
        "6a3fd5fa10d7c4e4fabeace29e36b2bfa76d45d5"

    const val Dimension =
        1_024

    const val MaxTokens =
        512

    const val Pooling =
        "NORMALIZED_CLS"

    const val QueryInstruction =
        "NONE"

    const val QueryTermSeparator =
        " "

    const val ExecutionProvider =
        "CPUExecutionProvider"

    /**
     * Product semantic binding for the exact representation + query formation.
     *
     * Dense cosine ranking is appended by [VexLifeVolatileDenseSemanticBackend], because ranking
     * belongs to that backend realization rather than to model identity.
     */
    const val RepresentationBindingRef =
        "semantic.bge-m3.6a3fd5fa10d7c4e4fabeace29e36b2bfa76d45d5." +
            "d1024.normalized-cls.max512.query-terms-space-v1"

    fun queryText(
        queryTerms: List<String>,
    ): String {
        val normalized =
            queryTerms
                .map(
                    String::trim,
                )
                .filter(
                    String::isNotEmpty,
                )

        require(
            normalized.isNotEmpty(),
        ) {
            "BGE-M3 semantic query requires at least one non-empty query term"
        }

        return normalized.joinToString(
            QueryTermSeparator,
        )
    }
}

// [VXG RealForever]
