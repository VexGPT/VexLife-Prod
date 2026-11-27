package vexlife.continuity

import kotlinx.serialization.Serializable

/**
 * Bounded declarative Working Focus for one VexLife lineage/thread.
 *
 * Working Focus answers one question only: what work is Vex deliberately carrying now, in a form
 * that can survive ordinary turn boundaries without becoming Conversation, semantic Memory, raw
 * reasoning, prompt residency, Attention priority, or provider/cache state.
 *
 * The durable owner is responsible for currentness. This object only defines and validates the
 * bounded semantic shape that owner may custody.
 */
object VexLifeWorkingFocus {
    const val EffectOwnerRef = "owner.vexlife-prod.working-focus.001"
    const val MaximumTextCharacters = 2_048
    const val MaximumRelationships = 16
    const val MaximumOpenEdges = 16
    const val MaximumRefs = 64

    @Serializable
    enum class Standing {
        OBSERVED,
        HYPOTHESIS,
        UNRESOLVED,
    }

    /** One compact relationship kept on the working whiteboard. Source bodies stay with owners. */
    @Serializable
    data class Relationship(
        val relationshipRef: String,
        val ownerRef: String,
        val standing: Standing,
        val compactMeaning: String,
        val sourceRefs: List<String>,
    ) {
        init {
            requireRef(relationshipRef, "relationshipRef")
            requireRef(ownerRef, "ownerRef")
            requireCompactText(compactMeaning, "compactMeaning")
            requireRefs(sourceRefs, "sourceRefs", allowEmpty = false)
        }
    }

    /** One unresolved edge. Its meaning still belongs to the named owner/source coordinates. */
    @Serializable
    data class OpenEdge(
        val edgeRef: String,
        val ownerRef: String,
        val compactMeaning: String,
        val sourceRefs: List<String>,
    ) {
        init {
            requireRef(edgeRef, "edgeRef")
            requireRef(ownerRef, "ownerRef")
            requireCompactText(compactMeaning, "compactMeaning")
            requireRefs(sourceRefs, "sourceRefs", allowEmpty = false)
        }
    }

    /**
     * One bounded proposal for current Working Focus.
     *
     * [formationRef] is the source-addressed proposal identity used by the durable owner when a new
     * focus identity is formed. [priorWorkingFocusRefOrNull] means this proposal is an update of the
     * exact current active focus; null deliberately forms/replaces with a new focus identity.
     *
     * [sourceBindingRefs] are the exact owner-supplied currentness/version coordinates that must be
     * revalidated unchanged before a displaced focus may resume. Their internal meaning remains with
     * their rightful owners.
     */
    @Serializable
    data class Draft(
        val lineageRef: String,
        val threadRef: String,
        val formationRef: String,
        val priorWorkingFocusRefOrNull: String?,
        val subjectScopeRefs: List<String>,
        val objective: String,
        val currentQuestionOrNull: String?,
        val retainedRelationships: List<Relationship>,
        val openEdges: List<OpenEdge>,
        val nextProbeOrNull: String?,
        val sourceBindingRefs: List<String>,
        val sourceRefs: List<String>,
    ) {
        init {
            requireRef(lineageRef, "lineageRef")
            requireRef(threadRef, "threadRef")
            requireRef(formationRef, "formationRef")
            priorWorkingFocusRefOrNull?.let { requireRef(it, "priorWorkingFocusRefOrNull") }
            requireRefs(subjectScopeRefs, "subjectScopeRefs", allowEmpty = false)
            requireCompactText(objective, "objective")
            currentQuestionOrNull?.let { requireCompactText(it, "currentQuestionOrNull") }
            require(retainedRelationships.size <= MaximumRelationships) {
                "working focus retainedRelationships exceeds $MaximumRelationships"
            }
            require(
                retainedRelationships.map(Relationship::relationshipRef).distinct().size ==
                    retainedRelationships.size,
            ) {
                "working focus relationshipRef must be unique"
            }
            require(openEdges.size <= MaximumOpenEdges) {
                "working focus openEdges exceeds $MaximumOpenEdges"
            }
            require(openEdges.map(OpenEdge::edgeRef).distinct().size == openEdges.size) {
                "working focus edgeRef must be unique"
            }
            nextProbeOrNull?.let { requireCompactText(it, "nextProbeOrNull") }
            requireRefs(sourceBindingRefs, "sourceBindingRefs", allowEmpty = false)
            requireRefs(sourceRefs, "sourceRefs", allowEmpty = false)
        }
    }

    /** Deterministic representation only. Sorting does not imply priority or significance. */
    fun normalize(draft: Draft): Draft =
        draft.copy(
            subjectScopeRefs = draft.subjectScopeRefs.distinct().sorted(),
            retainedRelationships =
                draft.retainedRelationships
                    .map { it.copy(sourceRefs = it.sourceRefs.distinct().sorted()) }
                    .sortedBy(Relationship::relationshipRef),
            openEdges =
                draft.openEdges
                    .map { it.copy(sourceRefs = it.sourceRefs.distinct().sorted()) }
                    .sortedBy(OpenEdge::edgeRef),
            sourceBindingRefs = draft.sourceBindingRefs.distinct().sorted(),
            sourceRefs = draft.sourceRefs.distinct().sorted(),
        )

    private fun requireCompactText(value: String, label: String) {
        require(value.isNotBlank() && value.length <= MaximumTextCharacters) {
            "$label must be 1..$MaximumTextCharacters characters"
        }
        require(value.none { it == '\u0000' }) {
            "$label must not contain NUL"
        }
    }

    private fun requireRefs(values: List<String>, label: String, allowEmpty: Boolean) {
        require(allowEmpty || values.isNotEmpty()) {
            "$label must not be empty"
        }
        require(values.size <= MaximumRefs) {
            "$label exceeds $MaximumRefs refs"
        }
        require(values.distinct().size == values.size) {
            "$label must be unique"
        }
        values.forEachIndexed { index, value -> requireRef(value, "$label[$index]") }
    }

    private fun requireRef(value: String, label: String) {
        require(value.matches(Regex("[A-Za-z0-9][A-Za-z0-9._:/-]*"))) {
            "$label must be one portable stable ref"
        }
    }
}

// [VXG RealForever]
