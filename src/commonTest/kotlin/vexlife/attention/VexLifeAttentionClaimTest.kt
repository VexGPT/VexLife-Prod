package vexlife.attention

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class VexLifeAttentionClaimTest {
    private fun candidateFixture(
        ref: String = "candidate.resource.storage",
        ownerRef: String = "owner.resource",
        sourceRef: String = "source.resource.storage",
        sourceBindingRef: String = "receipt.resource.storage.current.7",
        subjectScopeRefs: List<String> = listOf("subject.vex-home"),
        reachability: VexLifeAttentionPerceptibility.Reachability =
            VexLifeAttentionPerceptibility.Reachability.REACHABLE,
    ) =
        VexLifeAttentionPerceptibility.Candidate(
            candidateRef = ref,
            ownerRef = ownerRef,
            sourceRef = sourceRef,
            relationshipRef = "relationship.resource.storage",
            subjectScopeRefs = subjectScopeRefs,
            sourceBindingRefs = listOf(sourceBindingRef),
            sourceStandingRef = "standing.resource.storage.current",
            compactMeaning = "Home storage has a current source-owned condition.",
            deeperSourceRef = "source.resource.storage.detail",
            reachability = reachability,
        )

    private fun requirement(
        candidate: VexLifeAttentionPerceptibility.Candidate = candidateFixture(),
        requirementRef: String = "requirement.resource.storage.present-attention.7",
        requirementBindingRef: String = "receipt.resource.storage.attention.7",
    ) =
        VexLifeAttentionClaim.SourceRequirement(
            requirementRef = requirementRef,
            ownerRef = candidate.ownerRef,
            sourceRef = candidate.sourceRef,
            candidateRef = candidate.candidateRef,
            candidateSourceBindingRefs = candidate.sourceBindingRefs,
            candidateSubjectScopeRefs = candidate.subjectScopeRefs,
            requirementBindingRefs = listOf(requirementBindingRef),
            requirementStandingRef = "standing.resource.storage.attention-required",
            acknowledgementRequirementRefOrNull =
                "requirement.resource.storage.acknowledgement",
        )

    @Test
    fun sourceOwnedRequirementFormsClaimWithoutInventingPriorityOrEffects() {
        val candidate = candidateFixture()

        val claim =
            VexLifeAttentionClaim.form(
                candidate = candidate,
                requirement = requirement(candidate),
            )

        assertEquals(candidate.candidateRef, claim.candidateRef)
        assertEquals(candidate.sourceBindingRefs, claim.candidateSourceBindingRefs)
        assertEquals(candidate.subjectScopeRefs, claim.subjectScopeRefs)
        assertEquals(
            "standing.resource.storage.attention-required",
            claim.requirementStandingRef,
        )

        val surface =
            VexLifeAttentionClaim.formSurface(
                listOf(claim),
            )

        assertFalse(surface.effectAuthorityGranted)
        assertFalse(surface.displacementPerformed)
    }

    @Test
    fun requirementFailsClosedWhenCandidateSourceOrSubjectBindingIsStale() {
        val candidate = candidateFixture()

        assertFailsWith<IllegalArgumentException> {
            VexLifeAttentionClaim.form(
                candidate = candidate,
                requirement =
                    requirement(candidate).copy(
                        candidateSourceBindingRefs =
                            listOf("receipt.resource.storage.current.old"),
                    ),
            )
        }

        assertFailsWith<IllegalArgumentException> {
            VexLifeAttentionClaim.form(
                candidate = candidate,
                requirement =
                    requirement(candidate).copy(
                        candidateSubjectScopeRefs =
                            listOf("subject.other-home"),
                    ),
            )
        }
    }

    @Test
    fun acknowledgementBindsExactCurrentClaimButDoesNotResolveSourceCondition() {
        val candidate = candidateFixture()
        val claim =
            VexLifeAttentionClaim.form(
                candidate = candidate,
                requirement = requirement(candidate),
            )

        val acknowledged =
            VexLifeAttentionClaim.acknowledge(
                claim = claim,
                observation =
                    VexLifeAttentionClaim.AcknowledgementObservation(
                        acknowledgementRef = "ack.resource.storage.7",
                        actorRef = "actor.human.current",
                        requirementRef = claim.requirementRef,
                        requirementBindingRefs = claim.requirementBindingRefs,
                        acknowledgementRequirementRefOrNull =
                            claim.acknowledgementRequirementRefOrNull,
                        candidateSourceBindingRefs = claim.candidateSourceBindingRefs,
                        subjectScopeRefs = claim.subjectScopeRefs,
                    ),
            )

        assertEquals(claim, acknowledged.claim)
        assertEquals("ack.resource.storage.7", acknowledged.acknowledgementRef)
        assertEquals("actor.human.current", acknowledged.actorRef)
        assertFalse(acknowledged.sourceResolutionObservedByAcknowledgement)
        assertFalse(acknowledged.effectAuthorityGranted)
    }

    @Test
    fun staleAcknowledgementCannotApplyToNewRequirementOrCandidateGeneration() {
        val candidate = candidateFixture()
        val claim =
            VexLifeAttentionClaim.form(
                candidate = candidate,
                requirement = requirement(candidate),
            )

        assertFailsWith<IllegalArgumentException> {
            VexLifeAttentionClaim.acknowledge(
                claim = claim,
                observation =
                    VexLifeAttentionClaim.AcknowledgementObservation(
                        acknowledgementRef = "ack.resource.storage.stale-requirement",
                        actorRef = "actor.human.current",
                        requirementRef = claim.requirementRef,
                        requirementBindingRefs =
                            listOf("receipt.resource.storage.attention.old"),
                        acknowledgementRequirementRefOrNull =
                            claim.acknowledgementRequirementRefOrNull,
                        candidateSourceBindingRefs = claim.candidateSourceBindingRefs,
                        subjectScopeRefs = claim.subjectScopeRefs,
                    ),
            )
        }

        assertFailsWith<IllegalArgumentException> {
            VexLifeAttentionClaim.acknowledge(
                claim = claim,
                observation =
                    VexLifeAttentionClaim.AcknowledgementObservation(
                        acknowledgementRef = "ack.resource.storage.stale-source",
                        actorRef = "actor.human.current",
                        requirementRef = claim.requirementRef,
                        requirementBindingRefs = claim.requirementBindingRefs,
                        acknowledgementRequirementRefOrNull =
                            claim.acknowledgementRequirementRefOrNull,
                        candidateSourceBindingRefs =
                            listOf("receipt.resource.storage.current.old"),
                        subjectScopeRefs = claim.subjectScopeRefs,
                    ),
            )
        }

        assertFailsWith<IllegalArgumentException> {
            VexLifeAttentionClaim.acknowledge(
                claim = claim,
                observation =
                    VexLifeAttentionClaim.AcknowledgementObservation(
                        acknowledgementRef = "ack.resource.storage.stale-ack-requirement",
                        actorRef = "actor.human.current",
                        requirementRef = claim.requirementRef,
                        requirementBindingRefs = claim.requirementBindingRefs,
                        acknowledgementRequirementRefOrNull =
                            "requirement.resource.storage.acknowledgement.v2",
                        candidateSourceBindingRefs = claim.candidateSourceBindingRefs,
                        subjectScopeRefs = claim.subjectScopeRefs,
                    ),
            )
        }
    }

    @Test
    fun heldCandidateMayStillCarrySourceRequirementWithoutPretendingPresentationOrRead() {
        val candidate =
            candidateFixture(
                reachability = VexLifeAttentionPerceptibility.Reachability.HELD,
            )

        val claim =
            VexLifeAttentionClaim.form(
                candidate = candidate,
                requirement = requirement(candidate),
            )

        assertEquals(
            VexLifeAttentionPerceptibility.Reachability.HELD,
            claim.reachability,
        )

        val surface =
            VexLifeAttentionClaim.formSurface(
                listOf(claim),
            )

        assertFalse(surface.displacementPerformed)
        assertFalse(surface.effectAuthorityGranted)
    }

    @Test
    fun claimSurfaceUsesIdentityOrderOnlyAndCannotBecomeHiddenPriorityQueue() {
        val firstCandidate =
            candidateFixture(
                ref = "candidate.zeta",
                ownerRef = "owner.zeta",
                sourceRef = "source.zeta",
                sourceBindingRef = "binding.zeta",
                subjectScopeRefs = listOf("subject.zeta"),
            )
        val secondCandidate =
            candidateFixture(
                ref = "candidate.alpha",
                ownerRef = "owner.alpha",
                sourceRef = "source.alpha",
                sourceBindingRef = "binding.alpha",
                subjectScopeRefs = listOf("subject.alpha"),
            )

        val zeta =
            VexLifeAttentionClaim.form(
                candidate = firstCandidate,
                requirement =
                    requirement(
                        candidate = firstCandidate,
                        requirementRef = "requirement.zeta",
                        requirementBindingRef = "binding.requirement.zeta",
                    ),
            )
        val alpha =
            VexLifeAttentionClaim.form(
                candidate = secondCandidate,
                requirement =
                    requirement(
                        candidate = secondCandidate,
                        requirementRef = "requirement.alpha",
                        requirementBindingRef = "binding.requirement.alpha",
                    ),
            )

        val surface =
            VexLifeAttentionClaim.formSurface(
                listOf(zeta, alpha),
            )

        assertEquals(
            listOf("requirement.alpha", "requirement.zeta"),
            surface.claims.map { it.requirementRef },
            "lexical identity order is not severity, urgency, priority, or presentation order",
        )
    }

    @Test
    fun duplicateRequirementIdentityCannotSilentlyRepresentTwoClaims() {
        val first = candidateFixture(ref = "candidate.first")
        val second =
            candidateFixture(
                ref = "candidate.second",
                ownerRef = "owner.second",
                sourceRef = "source.second",
                sourceBindingRef = "binding.second",
                subjectScopeRefs = listOf("subject.second"),
            )

        val firstClaim =
            VexLifeAttentionClaim.form(
                candidate = first,
                requirement =
                    requirement(
                        candidate = first,
                        requirementRef = "requirement.same",
                    ),
            )
        val secondClaim =
            VexLifeAttentionClaim.form(
                candidate = second,
                requirement =
                    requirement(
                        candidate = second,
                        requirementRef = "requirement.same",
                        requirementBindingRef = "binding.requirement.second",
                    ),
            )

        assertFailsWith<IllegalArgumentException> {
            VexLifeAttentionClaim.formSurface(
                listOf(firstClaim, secondClaim),
            )
        }
    }
}

// [VXG RealForever]
