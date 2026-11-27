package vexlife.attention

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class VexLifeAttentionActivityConvergenceTest {
    private fun candidate(
        ref: String,
        binding: String,
        reachability: VexLifeAttentionPerceptibility.Reachability =
            VexLifeAttentionPerceptibility.Reachability.REACHABLE,
    ) =
        VexLifeAttentionPerceptibility.Candidate(
            candidateRef = ref,
            ownerRef = "owner." + ref,
            sourceRef = "source." + ref,
            relationshipRef = "relationship." + ref,
            subjectScopeRefs = listOf("subject.shared"),
            sourceBindingRefs = listOf(binding),
            sourceStandingRef = "standing." + ref,
            compactMeaning = "Current meaning for " + ref,
            deeperSourceRef = "deeper." + ref,
            reachability = reachability,
        )

    private val surface =
        VexLifeAttentionPerceptibility.form(
            candidates =
                listOf(
                    candidate("candidate.code", "binding.code"),
                    candidate(
                        "candidate.resource",
                        "binding.resource",
                        VexLifeAttentionPerceptibility.Reachability.HELD,
                    ),
                    candidate("candidate.unrelated", "binding.unrelated"),
                ),
            sourceRefs = listOf("source.surface"),
        )

    @Test
    fun exactActivityJudgmentNominatesOnlySelectedCandidatesAndLeavesOthersQuiet() {
        val result =
            VexLifeAttentionActivityConvergence.converge(
                activity =
                    VexLifeAttentionActivityConvergence.Activity(
                        activityRef = "activity.request.1",
                        sourceBindingRefs = listOf("request.event.1", "thread.1"),
                        subjectScopeRefs = listOf("subject.shared"),
                    ),
                surface = surface,
                judgment =
                    VexLifeAttentionActivityConvergence.Judgment(
                        judgmentRef = "judgment.turn.1",
                        activityRef = "activity.request.1",
                        activitySourceBindingRefs = listOf("thread.1", "request.event.1"),
                        selected =
                            listOf(
                                VexLifeAttentionActivityConvergence.CandidateJudgment(
                                    candidateRef = "candidate.code",
                                    candidateSourceBindingRefs = listOf("binding.code"),
                                ),
                            ),
                    ),
                budget =
                    VexLifeAttentionActivityConvergence.SelectionBudget(
                        budgetRef = "budget.turn.1",
                        maximumSelectedCandidates = 1,
                    ),
            )

        assertEquals(listOf("candidate.code"), result.selected.map { it.candidateRef })
        assertEquals(
            listOf("candidate.resource", "candidate.unrelated"),
            result.quietCandidateRefs,
        )
        assertFalse(result.sourceReadPerformed)
        assertFalse(result.effectAuthorityGranted)
    }

    @Test
    fun heldRelevantCandidateMayBeNominatedWithoutPretendingAReadOccurred() {
        val result =
            VexLifeAttentionActivityConvergence.converge(
                activity =
                    VexLifeAttentionActivityConvergence.Activity(
                        activityRef = "activity.request.2",
                        sourceBindingRefs = listOf("request.event.2"),
                        workingFocusRefOrNull = null,
                    ),
                surface = surface,
                judgment =
                    VexLifeAttentionActivityConvergence.Judgment(
                        judgmentRef = "judgment.turn.2",
                        activityRef = "activity.request.2",
                        activitySourceBindingRefs = listOf("request.event.2"),
                        selected =
                            listOf(
                                VexLifeAttentionActivityConvergence.CandidateJudgment(
                                    candidateRef = "candidate.resource",
                                    candidateSourceBindingRefs = listOf("binding.resource"),
                                ),
                            ),
                    ),
                budget =
                    VexLifeAttentionActivityConvergence.SelectionBudget(
                        budgetRef = "budget.turn.2",
                        maximumSelectedCandidates = 1,
                    ),
            )

        assertEquals(
            VexLifeAttentionPerceptibility.Reachability.HELD,
            result.selected.single().reachability,
        )
        assertEquals("deeper.candidate.resource", result.selected.single().deeperSourceRef)
        assertFalse(result.sourceReadPerformed)
    }

    @Test
    fun workingFocusIsOptionalAndWhenPresentIsOnlyPreservedAsAnOwnedReference() {
        val result =
            VexLifeAttentionActivityConvergence.converge(
                activity =
                    VexLifeAttentionActivityConvergence.Activity(
                        activityRef = "activity.request.3",
                        sourceBindingRefs = listOf("request.event.3"),
                        workingFocusRefOrNull = "working-focus.current.3",
                    ),
                surface = surface,
                judgment =
                    VexLifeAttentionActivityConvergence.Judgment(
                        judgmentRef = "judgment.turn.3",
                        activityRef = "activity.request.3",
                        activitySourceBindingRefs = listOf("request.event.3"),
                        selected = emptyList(),
                    ),
                budget =
                    VexLifeAttentionActivityConvergence.SelectionBudget(
                        budgetRef = "budget.turn.3",
                        maximumSelectedCandidates = 0,
                    ),
            )

        assertEquals("working-focus.current.3", result.workingFocusRefOrNull)
        assertEquals(emptyList(), result.selected)
        assertEquals(
            listOf("candidate.code", "candidate.resource", "candidate.unrelated"),
            result.quietCandidateRefs,
        )
    }

    @Test
    fun staleActivityOrCandidateBindingsFailClosed() {
        val activity =
            VexLifeAttentionActivityConvergence.Activity(
                activityRef = "activity.request.4",
                sourceBindingRefs = listOf("request.event.4"),
            )
        val budget =
            VexLifeAttentionActivityConvergence.SelectionBudget(
                budgetRef = "budget.turn.4",
                maximumSelectedCandidates = 1,
            )

        assertFailsWith<IllegalArgumentException> {
            VexLifeAttentionActivityConvergence.converge(
                activity = activity,
                surface = surface,
                judgment =
                    VexLifeAttentionActivityConvergence.Judgment(
                        judgmentRef = "judgment.stale.activity",
                        activityRef = "activity.request.4",
                        activitySourceBindingRefs = listOf("request.event.old"),
                        selected = emptyList(),
                    ),
                budget = budget,
            )
        }

        assertFailsWith<IllegalArgumentException> {
            VexLifeAttentionActivityConvergence.converge(
                activity = activity,
                surface = surface,
                judgment =
                    VexLifeAttentionActivityConvergence.Judgment(
                        judgmentRef = "judgment.stale.candidate",
                        activityRef = "activity.request.4",
                        activitySourceBindingRefs = listOf("request.event.4"),
                        selected =
                            listOf(
                                VexLifeAttentionActivityConvergence.CandidateJudgment(
                                    candidateRef = "candidate.code",
                                    candidateSourceBindingRefs = listOf("binding.code.old"),
                                ),
                            ),
                    ),
                budget = budget,
            )
        }
    }

    @Test
    fun judgmentCannotSelectAbsentCandidateOrExceedCallerSuppliedBound() {
        val activity =
            VexLifeAttentionActivityConvergence.Activity(
                activityRef = "activity.request.5",
                sourceBindingRefs = listOf("request.event.5"),
            )

        assertFailsWith<IllegalArgumentException> {
            VexLifeAttentionActivityConvergence.converge(
                activity = activity,
                surface = surface,
                judgment =
                    VexLifeAttentionActivityConvergence.Judgment(
                        judgmentRef = "judgment.absent",
                        activityRef = "activity.request.5",
                        activitySourceBindingRefs = listOf("request.event.5"),
                        selected =
                            listOf(
                                VexLifeAttentionActivityConvergence.CandidateJudgment(
                                    candidateRef = "candidate.missing",
                                    candidateSourceBindingRefs = listOf("binding.missing"),
                                ),
                            ),
                    ),
                budget =
                    VexLifeAttentionActivityConvergence.SelectionBudget(
                        budgetRef = "budget.turn.5",
                        maximumSelectedCandidates = 1,
                    ),
            )
        }

        assertFailsWith<IllegalArgumentException> {
            VexLifeAttentionActivityConvergence.converge(
                activity = activity,
                surface = surface,
                judgment =
                    VexLifeAttentionActivityConvergence.Judgment(
                        judgmentRef = "judgment.too-many",
                        activityRef = "activity.request.5",
                        activitySourceBindingRefs = listOf("request.event.5"),
                        selected =
                            listOf(
                                VexLifeAttentionActivityConvergence.CandidateJudgment(
                                    candidateRef = "candidate.code",
                                    candidateSourceBindingRefs = listOf("binding.code"),
                                ),
                                VexLifeAttentionActivityConvergence.CandidateJudgment(
                                    candidateRef = "candidate.unrelated",
                                    candidateSourceBindingRefs = listOf("binding.unrelated"),
                                ),
                            ),
                    ),
                budget =
                    VexLifeAttentionActivityConvergence.SelectionBudget(
                        budgetRef = "budget.turn.5",
                        maximumSelectedCandidates = 1,
                    ),
            )
        }
    }

    @Test
    fun selectionOrderCannotBecomeHiddenPriority() {
        val result =
            VexLifeAttentionActivityConvergence.converge(
                activity =
                    VexLifeAttentionActivityConvergence.Activity(
                        activityRef = "activity.request.6",
                        sourceBindingRefs = listOf("request.event.6"),
                    ),
                surface = surface,
                judgment =
                    VexLifeAttentionActivityConvergence.Judgment(
                        judgmentRef = "judgment.turn.6",
                        activityRef = "activity.request.6",
                        activitySourceBindingRefs = listOf("request.event.6"),
                        selected =
                            listOf(
                                VexLifeAttentionActivityConvergence.CandidateJudgment(
                                    candidateRef = "candidate.unrelated",
                                    candidateSourceBindingRefs = listOf("binding.unrelated"),
                                ),
                                VexLifeAttentionActivityConvergence.CandidateJudgment(
                                    candidateRef = "candidate.code",
                                    candidateSourceBindingRefs = listOf("binding.code"),
                                ),
                            ),
                    ),
                budget =
                    VexLifeAttentionActivityConvergence.SelectionBudget(
                        budgetRef = "budget.turn.6",
                        maximumSelectedCandidates = 2,
                    ),
            )

        assertEquals(
            listOf("candidate.code", "candidate.unrelated"),
            result.selected.map { it.candidateRef },
            "judgment list order is not a priority channel",
        )
    }
}

// [VXG RealForever]
