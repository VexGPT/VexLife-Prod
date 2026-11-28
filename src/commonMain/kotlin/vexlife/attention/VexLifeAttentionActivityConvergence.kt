package vexlife.attention

import kotlinx.serialization.Serializable

/**
 * Activity-scoped nomination over the already-owned A1 perceptibility surface.
 *
 * This owner does not decide source truth or discover connections. It validates one bounded,
 * externally-formed nomination against the exact current activity binding and the exact current
 * candidate bindings, then projects which source addresses are worth exact deeper inspection.
 *
 * The nomination may come from Vex/model cognition. That makes it candidate reasoning, not source
 * truth, not final semantic relevance, and not effect authority. This owner never performs the
 * source read.
 */
object VexLifeAttentionActivityConvergence {
    @Serializable
    data class Activity(
        val activityRef: String,
        val sourceBindingRefs: List<String>,
        val subjectScopeRefs: List<String> = emptyList(),
        val workingFocusRefOrNull: String? = null,
    ) {
        init {
            requireRef(activityRef, "activityRef")
            requireRefs(sourceBindingRefs, "sourceBindingRefs", allowEmpty = false)
            requireRefs(subjectScopeRefs, "subjectScopeRefs", allowEmpty = true)
            workingFocusRefOrNull?.let {
                requireRef(it, "workingFocusRefOrNull")
            }
        }
    }

    @Serializable
    data class SelectionBudget(
        val budgetRef: String,
        val maximumNominatedCandidates: Int,
    ) {
        init {
            requireRef(budgetRef, "budgetRef")
            require(maximumNominatedCandidates >= 0) {
                "attention activity nomination budget must be nonnegative"
            }
        }
    }

    /**
     * One A1 candidate as Vex/model cognition nominated it for exact deeper inspection.
     *
     * Exact source and subject bindings prevent an older nomination from silently applying after the
     * source or its owner/subject scope changes.
     */
    @Serializable
    data class CandidateNomination(
        val candidateRef: String,
        val candidateSourceBindingRefs: List<String>,
        val candidateSubjectScopeRefs: List<String> = emptyList(),
    ) {
        init {
            requireRef(candidateRef, "candidateRef")
            requireRefs(
                candidateSourceBindingRefs,
                "candidateSourceBindingRefs",
                allowEmpty = false,
            )
            requireRefs(
                candidateSubjectScopeRefs,
                "candidateSubjectScopeRefs",
                allowEmpty = true,
            )
        }
    }

    /**
     * Ephemeral cognitive nomination bound to exact current activity.
     *
     * No rationale/private reasoning is retained here. The only durable-safe shape is which exact
     * current candidates were nominated from which exact current activity/subject binding.
     */
    @Serializable
    data class Nomination(
        val nominationRef: String,
        val activityRef: String,
        val activitySourceBindingRefs: List<String>,
        val activitySubjectScopeRefs: List<String> = emptyList(),
        val candidates: List<CandidateNomination>,
    ) {
        init {
            requireRef(nominationRef, "nominationRef")
            requireRef(activityRef, "activityRef")
            requireRefs(
                activitySourceBindingRefs,
                "activitySourceBindingRefs",
                allowEmpty = false,
            )
            requireRefs(
                activitySubjectScopeRefs,
                "activitySubjectScopeRefs",
                allowEmpty = true,
            )
            require(
                candidates
                    .map(CandidateNomination::candidateRef)
                    .distinct()
                    .size ==
                    candidates.size,
            ) {
                "attention activity nomination candidateRef must be unique"
            }
        }
    }

    @Serializable
    data class NominatedCandidate(
        val candidateRef: String,
        val ownerRef: String,
        val sourceRef: String,
        val relationshipRef: String,
        val subjectScopeRefs: List<String>,
        val sourceBindingRefs: List<String>,
        val sourceStandingRef: String,
        val deeperSourceRef: String,
        val reachability: VexLifeAttentionPerceptibility.Reachability,
    )

    /**
     * A2 result is a no-effect relationship projection.
     *
     * [quietCandidateRefs] means "not nominated for this activity now", not deleted, stale, absent,
     * globally irrelevant, or outside another lawful subject relationship.
     */
    @Serializable
    data class Result(
        val activityRef: String,
        val activitySourceBindingRefs: List<String>,
        val activitySubjectScopeRefs: List<String>,
        val workingFocusRefOrNull: String?,
        val nominationRef: String,
        val budgetRef: String,
        val nominated: List<NominatedCandidate>,
        val quietCandidateRefs: List<String>,
        val sourceReadPerformed: Boolean = false,
        val effectAuthorityGranted: Boolean = false,
    ) {
        init {
            require(!sourceReadPerformed) {
                "attention activity convergence cannot perform source reads"
            }
            require(!effectAuthorityGranted) {
                "attention activity convergence cannot grant effect authority"
            }
        }
    }

    fun converge(
        activity: Activity,
        surface: VexLifeAttentionPerceptibility.Surface,
        nomination: Nomination,
        budget: SelectionBudget,
    ): Result {
        require(nomination.activityRef == activity.activityRef) {
            "attention activity nomination does not bind the current activityRef"
        }
        requireSameRefs(
            expected = activity.sourceBindingRefs,
            actual = nomination.activitySourceBindingRefs,
            label = "attention activity source binding",
        )
        requireSameRefs(
            expected = activity.subjectScopeRefs,
            actual = nomination.activitySubjectScopeRefs,
            label = "attention activity subject scope",
        )
        require(nomination.candidates.size <= budget.maximumNominatedCandidates) {
            "attention activity nomination exceeds the supplied selection budget"
        }

        val candidatesByRef =
            surface.candidates.associateBy {
                it.candidateRef
            }

        val nominated =
            nomination.candidates
                .map { selected ->
                    val candidate =
                        requireNotNull(
                            candidatesByRef[selected.candidateRef],
                        ) {
                            "attention activity nomination names a candidate absent from the current perceptibility surface: " +
                                selected.candidateRef
                        }
                    requireSameRefs(
                        expected = candidate.sourceBindingRefs,
                        actual = selected.candidateSourceBindingRefs,
                        label = "attention candidate source binding " + candidate.candidateRef,
                    )
                    requireSameRefs(
                        expected = candidate.subjectScopeRefs,
                        actual = selected.candidateSubjectScopeRefs,
                        label = "attention candidate subject scope " + candidate.candidateRef,
                    )
                    NominatedCandidate(
                        candidateRef = candidate.candidateRef,
                        ownerRef = candidate.ownerRef,
                        sourceRef = candidate.sourceRef,
                        relationshipRef = candidate.relationshipRef,
                        subjectScopeRefs = candidate.subjectScopeRefs,
                        sourceBindingRefs = candidate.sourceBindingRefs,
                        sourceStandingRef = candidate.sourceStandingRef,
                        deeperSourceRef = candidate.deeperSourceRef,
                        reachability = candidate.reachability,
                    )
                }
                .sortedBy {
                    it.candidateRef
                }

        val nominatedRefs =
            nominated
                .map(NominatedCandidate::candidateRef)
                .toSet()
        val quiet =
            surface.candidates
                .map(VexLifeAttentionPerceptibility.Candidate::candidateRef)
                .filterNot(nominatedRefs::contains)
                .sorted()

        return Result(
            activityRef = activity.activityRef,
            activitySourceBindingRefs = activity.sourceBindingRefs,
            activitySubjectScopeRefs = activity.subjectScopeRefs,
            workingFocusRefOrNull = activity.workingFocusRefOrNull,
            nominationRef = nomination.nominationRef,
            budgetRef = budget.budgetRef,
            nominated = nominated,
            quietCandidateRefs = quiet,
        )
    }

    private fun requireSameRefs(
        expected: List<String>,
        actual: List<String>,
        label: String,
    ) {
        require(expected.toSet() == actual.toSet()) {
            "$label is stale or mismatched"
        }
    }

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
        require(values.distinct().size == values.size) {
            "$label must be unique"
        }
        values.forEachIndexed { index, value ->
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
