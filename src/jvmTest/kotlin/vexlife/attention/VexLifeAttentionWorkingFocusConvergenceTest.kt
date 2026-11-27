package vexlife.attention

import java.nio.file.Files
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import vexlife.continuity.VexLifeWorkingFocus
import vexlife.continuity.VexLifeWorkingFocusOwner

class VexLifeAttentionWorkingFocusConvergenceTest {
    private val lineageRef = "lineage.vexlife.attention-working-focus.test"
    private val threadRef = "thread.vexlife.attention-working-focus.test"

    private fun workingFocusDraft(
        formationRef: String = "receipt.focus.attention-composition.001",
        priorRevisionRefOrNull: String? = null,
        sourceBindingRef: String = "binding.focus.source.current",
    ) =
        VexLifeWorkingFocus.Draft(
            lineageRef = lineageRef,
            threadRef = threadRef,
            formationRef = formationRef,
            priorRevisionRefOrNull = priorRevisionRefOrNull,
            subjectScopeRefs = listOf("subject.victor", "project.self-inspection"),
            objective = "Continue the current Self-Inspection trajectory.",
            currentQuestionOrNull = "What is the next truthful Working Focus edge?",
            retainedRelationships =
                listOf(
                    VexLifeWorkingFocus.Relationship(
                        relationshipRef = "relationship.self-inspection.current",
                        ownerRef = VexLifeWorkingFocus.EffectOwnerRef,
                        standing = VexLifeWorkingFocus.Standing.OBSERVED,
                        compactMeaning = "Working Focus is the current deliberately-carried work.",
                        sourceRefs = listOf("source.working-focus.current"),
                    ),
                ),
            openEdges =
                listOf(
                    VexLifeWorkingFocus.OpenEdge(
                        edgeRef = "edge.attention.return",
                        ownerRef = VexLifeAttentionWorkingFocusConvergence.EffectOwnerRef,
                        compactMeaning = "Return current work after the exact attention claim retires.",
                        sourceRefs = listOf("source.attention.current"),
                    ),
                ),
            nextProbeOrNull = "Compose exact Attention displacement and lawful return.",
            sourceBindingRefs = listOf(sourceBindingRef),
            sourceRefs = listOf("source.working-focus.current"),
        )

    private fun candidate() =
        VexLifeAttentionPerceptibility.Candidate(
            candidateRef = "candidate.resource.storage",
            ownerRef = "owner.resource.storage",
            sourceRef = "source.resource.storage",
            relationshipRef = "relationship.resource.storage",
            subjectScopeRefs = listOf("subject.vex-home"),
            sourceBindingRefs = listOf("binding.resource.storage.current"),
            sourceStandingRef = "standing.resource.storage.current",
            compactMeaning = "Storage source says one current condition deserves present attention.",
            deeperSourceRef = "source.resource.storage.detail",
            reachability = VexLifeAttentionPerceptibility.Reachability.REACHABLE,
        )

    private fun claim(
        requirementRef: String = "requirement.resource.storage.attention.001",
        requirementBindingRef: String = "binding.resource.storage.attention.001",
    ): VexLifeAttentionClaim.Claim {
        val candidate = candidate()
        return VexLifeAttentionClaim.form(
            candidate = candidate,
            requirement =
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
                ),
        )
    }

    private fun retirementObservation(
        claim: VexLifeAttentionClaim.Claim,
        retirementRef: String = "retirement.resource.storage.attention.001",
        retirementBindingRef: String = "binding.resource.storage.retirement.001",
    ) =
        VexLifeAttentionClaim.SourceRetirementObservation(
            retirementRef = retirementRef,
            ownerRef = claim.ownerRef,
            sourceRef = claim.sourceRef,
            requirementRef = claim.requirementRef,
            priorRequirementBindingRefs = claim.requirementBindingRefs,
            candidateRef = claim.candidateRef,
            priorCandidateSourceBindingRefs = claim.candidateSourceBindingRefs,
            priorSubjectScopeRefs = claim.subjectScopeRefs,
            retirementBindingRefs = listOf(retirementBindingRef),
            retirementStandingRef = "standing.resource.storage.attention-retired",
        )

    @Test
    fun exactClaimDisplacesActiveWorkingFocusWithoutChangingItsRevision() {
        val home = Files.createTempDirectory("vexlife-attention-focus-displace-").toAbsolutePath()
        try {
            val owner = VexLifeWorkingFocusOwner(home)
            val active =
                owner.setCurrent(
                    workingFocusDraft(),
                    Instant.parse("2026-11-27T21:00:00Z"),
                )
            val revision = assertNotNull(active.current.revisionOrNull)

            val displaced =
                VexLifeAttentionWorkingFocusConvergence.displaceForClaim(
                    owner = owner,
                    lineageRef = lineageRef,
                    threadRef = threadRef,
                    claim = claim(),
                    formedAt = Instant.parse("2026-11-27T21:01:00Z"),
                )

            assertEquals(
                VexLifeAttentionWorkingFocusConvergence.DisplacementStanding.DISPLACED,
                displaced.standing,
            )
            assertTrue(displaced.displacementPerformedNow)

            val current = assertNotNull(owner.readCurrent(lineageRef, threadRef))
            assertEquals(VexLifeWorkingFocusOwner.FocusState.DISPLACED, current.state.state)
            assertEquals(revision, current.revisionOrNull)
            assertEquals(
                VexLifeAttentionWorkingFocusConvergence.EffectOwnerRef,
                current.state.causeOwnerRef,
            )
            assertEquals("requirement.resource.storage.attention.001", current.state.causeRef)
            assertEquals(listOf(displaced.claimBindingRef), current.state.causeBindingRefs)
        } finally {
            home.toFile().deleteRecursively()
        }
    }

    @Test
    fun repeatedExactClaimIsIdempotentAndDoesNotCreateAnotherDisplacement() {
        val home = Files.createTempDirectory("vexlife-attention-focus-idempotent-").toAbsolutePath()
        try {
            val owner = VexLifeWorkingFocusOwner(home)
            owner.setCurrent(workingFocusDraft())
            val claim = claim()

            val first =
                VexLifeAttentionWorkingFocusConvergence.displaceForClaim(
                    owner,
                    lineageRef,
                    threadRef,
                    claim,
                )
            val repeated =
                VexLifeAttentionWorkingFocusConvergence.displaceForClaim(
                    owner,
                    lineageRef,
                    threadRef,
                    claim,
                )

            assertTrue(first.displacementPerformedNow)
            assertFalse(repeated.displacementPerformedNow)
            assertEquals(first.claimBindingRef, repeated.claimBindingRef)
            assertEquals(first.workingFocusStateRefOrNull, repeated.workingFocusStateRefOrNull)
        } finally {
            home.toFile().deleteRecursively()
        }
    }

    @Test
    fun secondIndependentClaimCannotSilentlyNestAnotherDisplacement() {
        val home = Files.createTempDirectory("vexlife-attention-focus-no-nested-").toAbsolutePath()
        try {
            val owner = VexLifeWorkingFocusOwner(home)
            owner.setCurrent(workingFocusDraft())
            VexLifeAttentionWorkingFocusConvergence.displaceForClaim(
                owner,
                lineageRef,
                threadRef,
                claim(),
            )
            val before = assertNotNull(owner.readCurrent(lineageRef, threadRef))

            assertFailsWith<IllegalArgumentException> {
                VexLifeAttentionWorkingFocusConvergence.displaceForClaim(
                    owner,
                    lineageRef,
                    threadRef,
                    claim(
                        requirementRef = "requirement.resource.storage.attention.002",
                        requirementBindingRef = "binding.resource.storage.attention.002",
                    ),
                )
            }

            assertEquals(before, owner.readCurrent(lineageRef, threadRef))
        } finally {
            home.toFile().deleteRecursively()
        }
    }

    @Test
    fun acknowledgementAloneCannotReturnDisplacedWorkingFocus() {
        val home = Files.createTempDirectory("vexlife-attention-focus-ack-").toAbsolutePath()
        try {
            val owner = VexLifeWorkingFocusOwner(home)
            owner.setCurrent(workingFocusDraft())
            val claim = claim()
            val displaced =
                VexLifeAttentionWorkingFocusConvergence.displaceForClaim(
                    owner,
                    lineageRef,
                    threadRef,
                    claim,
                )

            val acknowledged =
                VexLifeAttentionClaim.acknowledge(
                    claim = claim,
                    observation =
                        VexLifeAttentionClaim.AcknowledgementObservation(
                            acknowledgementRef = "ack.resource.storage.attention.001",
                            actorRef = "actor.human.current",
                            requirementRef = claim.requirementRef,
                            requirementBindingRefs = claim.requirementBindingRefs,
                            acknowledgementRequirementRefOrNull =
                                claim.acknowledgementRequirementRefOrNull,
                            candidateSourceBindingRefs = claim.candidateSourceBindingRefs,
                            subjectScopeRefs = claim.subjectScopeRefs,
                        ),
                )

            assertFalse(acknowledged.sourceResolutionObservedByAcknowledgement)
            assertEquals(
                displaced.workingFocusStateRefOrNull,
                owner.readCurrent(lineageRef, threadRef)?.stateRef,
            )
            assertEquals(
                VexLifeWorkingFocusOwner.FocusState.DISPLACED,
                owner.readCurrent(lineageRef, threadRef)?.state?.state,
            )
        } finally {
            home.toFile().deleteRecursively()
        }
    }

    @Test
    fun exactSourceRetirementCanReturnAfterRestartWithoutOldClaimObject() {
        val home = Files.createTempDirectory("vexlife-attention-focus-resume-").toAbsolutePath()
        try {
            val owner = VexLifeWorkingFocusOwner(home)
            val active = owner.setCurrent(workingFocusDraft())
            val revision = assertNotNull(active.current.revisionOrNull)
            val claim = claim()
            val displaced =
                VexLifeAttentionWorkingFocusConvergence.displaceForClaim(
                    owner,
                    lineageRef,
                    threadRef,
                    claim,
                )

            val retirement = retirementObservation(claim)
            val retired = VexLifeAttentionClaim.retire(claim, retirement)
            assertTrue(retired.requirementRetiredBySource)

            // Re-open the same durable Home through a fresh owner instance. Return depends on the
            // persisted displacement + source retirement coordinates, not the old Claim object.
            val restartedOwner = VexLifeWorkingFocusOwner(home)
            val resumed =
                VexLifeAttentionWorkingFocusConvergence.resumeAfterSourceRetirement(
                    owner = restartedOwner,
                    lineageRef = lineageRef,
                    threadRef = threadRef,
                    displacedStateRef = assertNotNull(displaced.workingFocusStateRefOrNull),
                    retirementObservation = retirement,
                    revalidatedWorkingFocusSourceBindingRefs = revision.sourceBindingRefs,
                    formedAt = Instant.parse("2026-11-27T21:02:00Z"),
                )

            assertTrue(resumed.resumePerformedNow)
            val current = assertNotNull(restartedOwner.readCurrent(lineageRef, threadRef))
            assertEquals(VexLifeWorkingFocusOwner.FocusState.ACTIVE, current.state.state)
            assertEquals(VexLifeWorkingFocusOwner.Transition.RESUME, current.state.transition)
            assertEquals(revision, current.revisionOrNull)
            assertEquals(resumed.workingFocusStateRef, current.stateRef)
            assertEquals(resumed.workingFocusRevisionRef, current.revisionRefOrNull)
        } finally {
            home.toFile().deleteRecursively()
        }
    }

    @Test
    fun foreignRetirementCannotReturnAnotherClaimsDisplacement() {
        val home = Files.createTempDirectory("vexlife-attention-focus-foreign-retire-").toAbsolutePath()
        try {
            val owner = VexLifeWorkingFocusOwner(home)
            val active = owner.setCurrent(workingFocusDraft())
            val originalClaim = claim()
            val displaced =
                VexLifeAttentionWorkingFocusConvergence.displaceForClaim(
                    owner,
                    lineageRef,
                    threadRef,
                    originalClaim,
                )
            val before = assertNotNull(owner.readCurrent(lineageRef, threadRef))
            val otherClaim =
                claim(
                    requirementRef = "requirement.resource.storage.attention.other",
                    requirementBindingRef = "binding.resource.storage.attention.other",
                )

            assertFailsWith<IllegalArgumentException> {
                VexLifeAttentionWorkingFocusConvergence.resumeAfterSourceRetirement(
                    owner = owner,
                    lineageRef = lineageRef,
                    threadRef = threadRef,
                    displacedStateRef = assertNotNull(displaced.workingFocusStateRefOrNull),
                    retirementObservation = retirementObservation(otherClaim),
                    revalidatedWorkingFocusSourceBindingRefs =
                        assertNotNull(active.current.revisionOrNull).sourceBindingRefs,
                )
            }

            assertEquals(before, owner.readCurrent(lineageRef, threadRef))
        } finally {
            home.toFile().deleteRecursively()
        }
    }

    @Test
    fun workingFocusSourceDriftBlocksReturnAndPreservesDisplacement() {
        val home = Files.createTempDirectory("vexlife-attention-focus-source-drift-").toAbsolutePath()
        try {
            val owner = VexLifeWorkingFocusOwner(home)
            owner.setCurrent(workingFocusDraft())
            val claim = claim()
            val displaced =
                VexLifeAttentionWorkingFocusConvergence.displaceForClaim(
                    owner,
                    lineageRef,
                    threadRef,
                    claim,
                )
            val before = assertNotNull(owner.readCurrent(lineageRef, threadRef))

            assertFailsWith<IllegalArgumentException> {
                VexLifeAttentionWorkingFocusConvergence.resumeAfterSourceRetirement(
                    owner = owner,
                    lineageRef = lineageRef,
                    threadRef = threadRef,
                    displacedStateRef = assertNotNull(displaced.workingFocusStateRefOrNull),
                    retirementObservation = retirementObservation(claim),
                    revalidatedWorkingFocusSourceBindingRefs =
                        listOf("binding.focus.source.changed"),
                )
            }

            assertEquals(before, owner.readCurrent(lineageRef, threadRef))
        } finally {
            home.toFile().deleteRecursively()
        }
    }

    @Test
    fun attentionClaimDoesNotNeedToDisplaceWhenNoActiveWorkingFocusExists() {
        val home = Files.createTempDirectory("vexlife-attention-focus-none-").toAbsolutePath()
        try {
            val owner = VexLifeWorkingFocusOwner(home)

            val result =
                VexLifeAttentionWorkingFocusConvergence.displaceForClaim(
                    owner,
                    lineageRef,
                    threadRef,
                    claim(),
                )

            assertEquals(
                VexLifeAttentionWorkingFocusConvergence.DisplacementStanding.NO_ACTIVE_WORKING_FOCUS,
                result.standing,
            )
            assertFalse(result.displacementPerformedNow)
            assertEquals(null, result.workingFocusStateRefOrNull)
            assertEquals(null, result.workingFocusRevisionRefOrNull)
        } finally {
            home.toFile().deleteRecursively()
        }
    }
}

// [VXG RealForever]
