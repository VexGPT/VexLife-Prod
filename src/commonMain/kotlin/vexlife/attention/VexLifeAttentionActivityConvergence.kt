package vexlife.attention

import kotlinx.serialization.Serializable

/**
 * Activity-scoped convergence over the already-owned A1 perceptibility surface.
 *
 * This owner does not decide source truth or discover connections. It validates one bounded,
 * externally-formed relevance judgment against the exact current activity binding and the exact
 * current candidate bindings, then projects which source addresses are nominated for possible
 * deeper pull.
 *
 * The judgment may come from Vex/model cognition. That makes it candidate reasoning, not source
 * truth and not effect authority. This owner never performs the source read.
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
        val maximumSelectedCandidates: Int,
    ) {
        init {
            requireRef(budgetRef, "budgetRef")
            require(maximumSelectedCandidates >= 0) {
                "attention activity selection budget must be nonnegative"
            }
        }
    }

    /**
     * One selected candidate as Vex/model cognition saw it.
     *
     * [candidateSourceBindingRefs] prevents a stale judgment over an older candidate generation from
     * silently applying to a newer source condition with the same candidate identity.
     */
    @Serializable
    data class CandidateJudgment(
        val candidateRef: String,
        val candidateSourceBindingRefs: List<String>,
    ) {
        init {
            requireRef(candidateRef, "candidateRef")
            requireRefs(
                candidateSourceBindingRefs,
                "candidateSourceBindingRefs",
                allowEmpty = false,
            )
        }
    }

    /**
     * Ephemeral cognitive judgment bound to exact current activity.
     *
     * No rationale/private reasoning is retained here. The only durable-safe shape is which exact
     * current candidates were nominated from which exact current activity binding.
     */
    @Serializable
    data class Judgment(
        val judgmentRef: String,
        val activityRef: String,
        val activitySourceBindingRefs: List<String>,
        val selected: List<CandidateJudgment>,
    ) {
        init {
            requireRef(judgmentRef, "judgmentRef")
            requireRef(activityRef, "activityRef")
            requireRefs(
                activitySourceBindingRefs,
                "activitySourceBindingRefs",
                allowEmpty = false,
            )
            require(
                selected
                    .map(CandidateJudgment::candidateRef)
                    .distinct()
                    .size ==
                    selected.size,
            ) {
                "attention activity judgment candidateRef must be unique"
            }
        }
    }

    @Serializable
    data class SelectedCandidate(
        val candidateRef: String,
        val ownerRef: String,
        val sourceRef: String,
        val relationshipRef: String,
        val sourceBindingRefs: List<String>,
        val sourceStandingRef: String,
        val deeperSourceRef: String,
        val reachability: VexLifeAttentionPerceptibility.Reachability,
    )

    /**
     * A2 result is a no-effect relationship projection.
     *
     * [quietCandidateRefs] means "not nominated for this activity now", not deleted, stale, absent,
     * or globally irrelevant.
     */
    @Serializable
    data class Result(
        val activityRef: String,
        val activitySourceBindingRefs: List<String>,
        val workingFocusRefOrNull: String?,
        val judgmentRef: String,
        val budgetRef: String,
        val selected: List<SelectedCandidate>,
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
        judgment: Judgment,
        budget: SelectionBudget,
    ): Result {
        require(judgment.activityRef == activity.activityRef) {
            "attention activity judgment does not bind the current activityRef"
        }
        requireSameRefs(
            expected = activity.sourceBindingRefs,
            actual = judgment.activitySourceBindingRefs,
            label = "attention activity source binding",
        )
        require(judgment.selected.size <= budget.maximumSelectedCandidates) {
            "attention activity judgment exceeds the supplied selection budget"
        }

        val candidatesByRef =
            surface.candidates.associateBy {
                it.candidateRef
            }

        val selected =
            judgment.selected
                .map { selected ->
                    val candidate =
                        requireNotNull(
                            candidatesByRef[selected.candidateRef],
                        ) {
                            "attention activity judgment names a candidate absent from the current perceptibility surface: " +
                                selected.candidateRef
                        }
                    requireSameRefs(
                        expected = candidate.sourceBindingRefs,
                        actual = selected.candidateSourceBindingRefs,
                        label = "attention candidate source binding " + candidate.candidateRef,
                    )
                    SelectedCandidate(
                        candidateRef = candidate.candidateRef,
                        ownerRef = candidate.ownerRef,
                        sourceRef = candidate.sourceRef,
                        relationshipRef = candidate.relationshipRef,
                        sourceBindingRefs = candidate.sourceBindingRefs,
                        sourceStandingRef = candidate.sourceStandingRef,
                        deeperSourceRef = candidate.deeperSourceRef,
                        reachability = candidate.reachability,
                    )
                }
                .sortedBy {
                    it.candidateRef
                }

        val selectedRefs =
            selected
                .map(SelectedCandidate::candidateRef)
                .toSet()
        val quiet =
            surface.candidates
                .map(VexLifeAttentionPerceptibility.Candidate::candidateRef)
                .filterNot(selectedRefs::contains)
                .sorted()

        return Result(
            activityRef = activity.activityRef,
            activitySourceBindingRefs = activity.sourceBindingRefs,
            workingFocusRefOrNull = activity.workingFocusRefOrNull,
            judgmentRef = judgment.judgmentRef,
            budgetRef = budget.budgetRef,
            selected = selected,
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
