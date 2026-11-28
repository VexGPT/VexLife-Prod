package vexlife.attention

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class VexLifeAttentionActivityConvergenceTest {
    private fun candidate(
        ref: String,
        binding: String,
        subjectScopes: List<String> = listOf("subject.shared"),
        reachability: VexLifeAttentionPerceptibility.Reachability =
            VexLifeAttentionPerceptibility.Reachability.REACHABLE,
    ) =
        VexLifeAttentionPerceptibility.Candidate(
            candidateRef = ref,
            ownerRef = "owner." + ref,
            sourceRef = "source." + ref,
            relationshipRef = "relationship." + ref,
            subjectScopeRefs = subjectScopes,
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
                        reachability = VexLifeAttentionPerceptibility.Reachability.HELD,
                    ),
                    candidate(
                        "candidate.counterpart",
                        "binding.counterpart",
                        subjectScopes = listOf("subject.counterpart.sarah"),
                    ),
                ),
            sourceRefs = listOf("source.surface"),
        )

    private fun nomination(
        ref: String,
        activityRef: String,
        activityBindings: List<String>,
        activityScopes: List<String> = emptyList(),
        candidates: List<VexLifeAttentionActivityConvergence.CandidateNomination>,
    ) =
        VexLifeAttentionActivityConvergence.Nomination(
            nominationRef = ref,
            activityRef = activityRef,
            activitySourceBindingRefs = activityBindings,
            activitySubjectScopeRefs = activityScopes,
            candidates = candidates,
        )

    @Test
    fun exactActivityNominationSelectsOnlyCandidatesWorthInspectingAndLeavesOthersQuiet() {
        val result =
            VexLifeAttentionActivityConvergence.converge(
                activity =
                    VexLifeAttentionActivityConvergence.Activity(
                        activityRef = "activity.request.1",
                        sourceBindingRefs = listOf("request.event.1", "thread.1"),
                        subjectScopeRefs = listOf("subject.shared"),
                    ),
                surface = surface,
                nomination =
                    nomination(
                        ref = "nomination.turn.1",
                        activityRef = "activity.request.1",
                        activityBindings = listOf("thread.1", "request.event.1"),
                        activityScopes = listOf("subject.shared"),
                        candidates =
                            listOf(
                                VexLifeAttentionActivityConvergence.CandidateNomination(
                                    candidateRef = "candidate.code",
                                    candidateSourceBindingRefs = listOf("binding.code"),
                                    candidateSubjectScopeRefs = listOf("subject.shared"),
                                ),
                            ),
                    ),
                budget =
                    VexLifeAttentionActivityConvergence.SelectionBudget(
                        budgetRef = "budget.turn.1",
                        maximumNominatedCandidates = 1,
                    ),
            )

        assertEquals(listOf("candidate.code"), result.nominated.map { it.candidateRef })
        assertEquals(
            listOf("candidate.counterpart", "candidate.resource"),
            result.quietCandidateRefs,
        )
        assertFalse(result.sourceReadPerformed)
        assertFalse(result.effectAuthorityGranted)
    }

    @Test
    fun heldCandidateMayBeNominatedForInspectionWithoutPretendingAReadOccurred() {
        val result =
            VexLifeAttentionActivityConvergence.converge(
                activity =
                    VexLifeAttentionActivityConvergence.Activity(
                        activityRef = "activity.request.2",
                        sourceBindingRefs = listOf("request.event.2"),
                    ),
                surface = surface,
                nomination =
                    nomination(
                        ref = "nomination.turn.2",
                        activityRef = "activity.request.2",
                        activityBindings = listOf("request.event.2"),
                        candidates =
                            listOf(
                                VexLifeAttentionActivityConvergence.CandidateNomination(
                                    candidateRef = "candidate.resource",
                                    candidateSourceBindingRefs = listOf("binding.resource"),
                                    candidateSubjectScopeRefs = listOf("subject.shared"),
                                ),
                            ),
                    ),
                budget =
                    VexLifeAttentionActivityConvergence.SelectionBudget(
                        budgetRef = "budget.turn.2",
                        maximumNominatedCandidates = 1,
                    ),
            )

        assertEquals(
            VexLifeAttentionPerceptibility.Reachability.HELD,
            result.nominated.single().reachability,
        )
        assertEquals("deeper.candidate.resource", result.nominated.single().deeperSourceRef)
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
                nomination =
                    nomination(
                        ref = "nomination.turn.3",
                        activityRef = "activity.request.3",
                        activityBindings = listOf("request.event.3"),
                        candidates = emptyList(),
                    ),
                budget =
                    VexLifeAttentionActivityConvergence.SelectionBudget(
                        budgetRef = "budget.turn.3",
                        maximumNominatedCandidates = 0,
                    ),
            )

        assertEquals("working-focus.current.3", result.workingFocusRefOrNull)
        assertEquals(emptyList(), result.nominated)
        assertEquals(
            listOf("candidate.code", "candidate.counterpart", "candidate.resource"),
            result.quietCandidateRefs,
        )
    }

    @Test
    fun staleActivityCandidateOrSubjectBindingsFailClosed() {
        val activity =
            VexLifeAttentionActivityConvergence.Activity(
                activityRef = "activity.request.4",
                sourceBindingRefs = listOf("request.event.4"),
                subjectScopeRefs = listOf("subject.shared"),
            )
        val budget =
            VexLifeAttentionActivityConvergence.SelectionBudget(
                budgetRef = "budget.turn.4",
                maximumNominatedCandidates = 1,
            )

        assertFailsWith<IllegalArgumentException> {
            VexLifeAttentionActivityConvergence.converge(
                activity = activity,
                surface = surface,
                nomination =
                    nomination(
                        ref = "nomination.stale.activity",
                        activityRef = "activity.request.4",
                        activityBindings = listOf("request.event.old"),
                        activityScopes = listOf("subject.shared"),
                        candidates = emptyList(),
                    ),
                budget = budget,
            )
        }

        assertFailsWith<IllegalArgumentException> {
            VexLifeAttentionActivityConvergence.converge(
                activity = activity,
                surface = surface,
                nomination =
                    nomination(
                        ref = "nomination.stale.activity-scope",
                        activityRef = "activity.request.4",
                        activityBindings = listOf("request.event.4"),
                        activityScopes = listOf("subject.other"),
                        candidates = emptyList(),
                    ),
                budget = budget,
            )
        }

        assertFailsWith<IllegalArgumentException> {
            VexLifeAttentionActivityConvergence.converge(
                activity = activity,
                surface = surface,
                nomination =
                    nomination(
                        ref = "nomination.stale.candidate",
                        activityRef = "activity.request.4",
                        activityBindings = listOf("request.event.4"),
                        activityScopes = listOf("subject.shared"),
                        candidates =
                            listOf(
                                VexLifeAttentionActivityConvergence.CandidateNomination(
                                    candidateRef = "candidate.code",
                                    candidateSourceBindingRefs = listOf("binding.code.old"),
                                    candidateSubjectScopeRefs = listOf("subject.shared"),
                                ),
                            ),
                    ),
                budget = budget,
            )
        }

        assertFailsWith<IllegalArgumentException> {
            VexLifeAttentionActivityConvergence.converge(
                activity = activity,
                surface = surface,
                nomination =
                    nomination(
                        ref = "nomination.stale.candidate-scope",
                        activityRef = "activity.request.4",
                        activityBindings = listOf("request.event.4"),
                        activityScopes = listOf("subject.shared"),
                        candidates =
                            listOf(
                                VexLifeAttentionActivityConvergence.CandidateNomination(
                                    candidateRef = "candidate.counterpart",
                                    candidateSourceBindingRefs = listOf("binding.counterpart"),
                                    candidateSubjectScopeRefs = listOf("subject.counterpart.changed"),
                                ),
                            ),
                    ),
                budget = budget,
            )
        }
    }

    @Test
    fun nominationCannotNameAbsentCandidateOrExceedCallerSuppliedBound() {
        val activity =
            VexLifeAttentionActivityConvergence.Activity(
                activityRef = "activity.request.5",
                sourceBindingRefs = listOf("request.event.5"),
            )

        assertFailsWith<IllegalArgumentException> {
            VexLifeAttentionActivityConvergence.converge(
                activity = activity,
                surface = surface,
                nomination =
                    nomination(
                        ref = "nomination.absent",
                        activityRef = "activity.request.5",
                        activityBindings = listOf("request.event.5"),
                        candidates =
                            listOf(
                                VexLifeAttentionActivityConvergence.CandidateNomination(
                                    candidateRef = "candidate.missing",
                                    candidateSourceBindingRefs = listOf("binding.missing"),
                                ),
                            ),
                    ),
                budget =
                    VexLifeAttentionActivityConvergence.SelectionBudget(
                        budgetRef = "budget.turn.5",
                        maximumNominatedCandidates = 1,
                    ),
            )
        }

        assertFailsWith<IllegalArgumentException> {
            VexLifeAttentionActivityConvergence.converge(
                activity = activity,
                surface = surface,
                nomination =
                    nomination(
                        ref = "nomination.too-many",
                        activityRef = "activity.request.5",
                        activityBindings = listOf("request.event.5"),
                        candidates =
                            listOf(
                                VexLifeAttentionActivityConvergence.CandidateNomination(
                                    candidateRef = "candidate.code",
                                    candidateSourceBindingRefs = listOf("binding.code"),
                                    candidateSubjectScopeRefs = listOf("subject.shared"),
                                ),
                                VexLifeAttentionActivityConvergence.CandidateNomination(
                                    candidateRef = "candidate.counterpart",
                                    candidateSourceBindingRefs = listOf("binding.counterpart"),
                                    candidateSubjectScopeRefs = listOf("subject.counterpart.sarah"),
                                ),
                            ),
                    ),
                budget =
                    VexLifeAttentionActivityConvergence.SelectionBudget(
                        budgetRef = "budget.turn.5",
                        maximumNominatedCandidates = 1,
                    ),
            )
        }
    }

    @Test
    fun nominationOrderCannotBecomeHiddenPriorityAndSubjectScopeIsPreserved() {
        val result =
            VexLifeAttentionActivityConvergence.converge(
                activity =
                    VexLifeAttentionActivityConvergence.Activity(
                        activityRef = "activity.request.6",
                        sourceBindingRefs = listOf("request.event.6"),
                        subjectScopeRefs = listOf("subject.shared"),
                    ),
                surface = surface,
                nomination =
                    nomination(
                        ref = "nomination.turn.6",
                        activityRef = "activity.request.6",
                        activityBindings = listOf("request.event.6"),
                        activityScopes = listOf("subject.shared"),
                        candidates =
                            listOf(
                                VexLifeAttentionActivityConvergence.CandidateNomination(
                                    candidateRef = "candidate.counterpart",
                                    candidateSourceBindingRefs = listOf("binding.counterpart"),
                                    candidateSubjectScopeRefs = listOf("subject.counterpart.sarah"),
                                ),
                                VexLifeAttentionActivityConvergence.CandidateNomination(
                                    candidateRef = "candidate.code",
                                    candidateSourceBindingRefs = listOf("binding.code"),
                                    candidateSubjectScopeRefs = listOf("subject.shared"),
                                ),
                            ),
                    ),
                budget =
                    VexLifeAttentionActivityConvergence.SelectionBudget(
                        budgetRef = "budget.turn.6",
                        maximumNominatedCandidates = 2,
                    ),
            )

        assertEquals(
            listOf("candidate.code", "candidate.counterpart"),
            result.nominated.map { it.candidateRef },
            "nomination list order is not a priority channel",
        )
        assertEquals(
            listOf("subject.counterpart.sarah"),
            result.nominated.last().subjectScopeRefs,
            "Attention preserves the candidate owner/subject scope rather than globalizing currentness",
        )
    }
}

// [VXG RealForever]
