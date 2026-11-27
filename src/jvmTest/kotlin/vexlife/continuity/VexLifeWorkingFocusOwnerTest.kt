package vexlife.continuity

import java.nio.file.Files
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class VexLifeWorkingFocusOwnerTest {
    private val lineageRef = "lineage.vexlife.working-focus.test"
    private val threadRef = "thread.vexlife.working-focus.test"

    private fun draft(
        formationRef: String = "receipt.focus.proposal.001",
        priorRevisionRefOrNull: String? = null,
        objective: String = "Keep the Self-Inspection relationship on the working whiteboard.",
        binding: String = "binding.self-inspection.current.001",
    ) =
        VexLifeWorkingFocus.Draft(
            lineageRef = lineageRef,
            threadRef = threadRef,
            formationRef = formationRef,
            priorRevisionRefOrNull = priorRevisionRefOrNull,
            subjectScopeRefs = listOf("subject.victor", "project.self-inspection"),
            objective = objective,
            currentQuestionOrNull = "What is the smallest rightful Working Focus owner?",
            retainedRelationships =
                listOf(
                    VexLifeWorkingFocus.Relationship(
                        relationshipRef = "relationship.working-focus.owner",
                        ownerRef = VexLifeWorkingFocus.EffectOwnerRef,
                        standing = VexLifeWorkingFocus.Standing.OBSERVED,
                        compactMeaning = "Working Focus currentness is independent of prompt pressure.",
                        sourceRefs = listOf("source.self-inspection.a0"),
                    ),
                ),
            openEdges =
                listOf(
                    VexLifeWorkingFocus.OpenEdge(
                        edgeRef = "edge.attention.resume",
                        ownerRef = "owner.vexlife-prod.attention",
                        compactMeaning = "Attention still needs displacement and lawful resume.",
                        sourceRefs = listOf("source.attention.a3"),
                    ),
                ),
            nextProbeOrNull = "Prove displacement preserves exact focus without owning Attention.",
            sourceBindingRefs = listOf(binding),
            sourceRefs = listOf("source.self-inspection.a0"),
        )

    @Test
    fun currentFocusHasStableIdentityAcrossExactUpdateAndImmutableRevisionHistory() {
        val home = Files.createTempDirectory("vexlife-working-focus-update-").toAbsolutePath()
        try {
            val owner = VexLifeWorkingFocusOwner(home)
            val first =
                owner.setCurrent(
                    draft(),
                    Instant.parse("2026-11-27T20:00:00Z"),
                )
            val firstRevision = assertNotNull(first.current.revisionOrNull)
            assertEquals(VexLifeWorkingFocusOwner.FocusState.ACTIVE, first.current.state.state)

            val second =
                owner.setCurrent(
                    draft(
                        formationRef = "receipt.focus.proposal.002",
                        priorRevisionRefOrNull = assertNotNull(first.current.revisionRefOrNull),
                        objective = "Keep the refined Self-Inspection relationship current.",
                    ),
                    Instant.parse("2026-11-27T20:01:00Z"),
                )
            val secondRevision = assertNotNull(second.current.revisionOrNull)

            assertEquals(firstRevision.workingFocusRef, secondRevision.workingFocusRef)
            assertEquals(firstRevision.semanticFingerprint, secondRevision.priorRevisionFingerprintOrNull)
            assertNotEquals(firstRevision.semanticFingerprint, secondRevision.semanticFingerprint)
            assertEquals(
                second.current,
                owner.readCurrent(lineageRef, threadRef),
            )

            val revisionCount =
                Files.walk(home.resolve("working-focus")).use { paths ->
                    paths.filter { it.parent?.fileName?.toString() == "revisions" }.count()
                }
            assertEquals(2L, revisionCount)
        } finally {
            home.toFile().deleteRecursively()
        }
    }

    @Test
    fun staleUpdateCannotOverwriteANewerActiveRevision() {
        val home = Files.createTempDirectory("vexlife-working-focus-stale-update-").toAbsolutePath()
        try {
            val owner = VexLifeWorkingFocusOwner(home)
            val first = owner.setCurrent(draft())
            val firstRevisionRef = assertNotNull(first.current.revisionRefOrNull)

            val second =
                owner.setCurrent(
                    draft(
                        formationRef = "receipt.focus.proposal.current-update",
                        priorRevisionRefOrNull = firstRevisionRef,
                        objective = "Advance the exact current Working Focus revision.",
                    ),
                )

            val staleProposal =
                draft(
                    formationRef = "receipt.focus.proposal.stale-update",
                    priorRevisionRefOrNull = firstRevisionRef,
                    objective = "This proposal was formed against the superseded first revision.",
                )

            assertFailsWith<IllegalArgumentException> {
                owner.setCurrent(staleProposal)
            }
            assertEquals(second.current, owner.readCurrent(lineageRef, threadRef))
        } finally {
            home.toFile().deleteRecursively()
        }
    }

    @Test
    fun displacementPreservesExactRevisionAndRejectsNestedDisplacement() {
        val home = Files.createTempDirectory("vexlife-working-focus-displace-").toAbsolutePath()
        try {
            val owner = VexLifeWorkingFocusOwner(home)
            val active = owner.setCurrent(draft())
            val activeRevision = assertNotNull(active.current.revisionOrNull)

            val displaced =
                owner.displace(
                    lineageRef = lineageRef,
                    threadRef = threadRef,
                    causeOwnerRef = "owner.vexlife-prod.attention",
                    causeRef = "attention.claim.storage.current",
                    causeBindingRefs = listOf("binding.attention.claim.current"),
                    formedAt = Instant.parse("2026-11-27T20:02:00Z"),
                )

            assertEquals(VexLifeWorkingFocusOwner.FocusState.DISPLACED, displaced.current.state.state)
            assertEquals(activeRevision, displaced.current.revisionOrNull)

            assertFailsWith<IllegalArgumentException> {
                owner.displace(
                    lineageRef = lineageRef,
                    threadRef = threadRef,
                    causeOwnerRef = "owner.vexlife-prod.attention",
                    causeRef = "attention.claim.other",
                    causeBindingRefs = listOf("binding.attention.claim.other"),
                )
            }
            assertEquals(displaced.current, owner.readCurrent(lineageRef, threadRef))
        } finally {
            home.toFile().deleteRecursively()
        }
    }

    @Test
    fun exactDisplacementResumesOnlyAfterSavedSourceBindingsRevalidate() {
        val home = Files.createTempDirectory("vexlife-working-focus-resume-").toAbsolutePath()
        try {
            val owner = VexLifeWorkingFocusOwner(home)
            val active = owner.setCurrent(draft(binding = "binding.focus.source.current"))
            val revision = assertNotNull(active.current.revisionOrNull)
            val displaced =
                owner.displace(
                    lineageRef = lineageRef,
                    threadRef = threadRef,
                    causeOwnerRef = "owner.vexlife-prod.attention",
                    causeRef = "attention.claim.current",
                    causeBindingRefs = listOf("binding.attention.claim.current"),
                )

            val resumed =
                owner.resume(
                    lineageRef = lineageRef,
                    threadRef = threadRef,
                    displacedStateRef = displaced.current.stateRef,
                    releaseOwnerRef = "owner.resource.storage",
                    releaseRef = "retirement.resource.storage.current",
                    releaseBindingRefs = listOf("binding.retirement.current"),
                    revalidatedSourceBindingRefs = revision.sourceBindingRefs,
                    formedAt = Instant.parse("2026-11-27T20:03:00Z"),
                )

            assertEquals(VexLifeWorkingFocusOwner.FocusState.ACTIVE, resumed.current.state.state)
            assertEquals(VexLifeWorkingFocusOwner.Transition.RESUME, resumed.current.state.transition)
            assertEquals(revision, resumed.current.revisionOrNull)
            assertEquals(displaced.current.state.semanticFingerprint, resumed.current.state.priorStateFingerprintOrNull)
            assertEquals(revision.sourceBindingRefs, resumed.current.state.revalidatedSourceBindingRefs)
        } finally {
            home.toFile().deleteRecursively()
        }
    }

    @Test
    fun sourceBindingDriftFailsClosedWithoutChangingCurrentDisplacement() {
        val home = Files.createTempDirectory("vexlife-working-focus-stale-source-").toAbsolutePath()
        try {
            val owner = VexLifeWorkingFocusOwner(home)
            owner.setCurrent(draft(binding = "binding.focus.source.current"))
            val displaced =
                owner.displace(
                    lineageRef = lineageRef,
                    threadRef = threadRef,
                    causeOwnerRef = "owner.vexlife-prod.attention",
                    causeRef = "attention.claim.current",
                    causeBindingRefs = listOf("binding.attention.claim.current"),
                )

            assertFailsWith<IllegalArgumentException> {
                owner.resume(
                    lineageRef = lineageRef,
                    threadRef = threadRef,
                    displacedStateRef = displaced.current.stateRef,
                    releaseOwnerRef = "owner.resource.storage",
                    releaseRef = "retirement.resource.storage.current",
                    releaseBindingRefs = listOf("binding.retirement.current"),
                    revalidatedSourceBindingRefs = listOf("binding.focus.source.changed"),
                )
            }

            assertEquals(displaced.current, owner.readCurrent(lineageRef, threadRef))
        } finally {
            home.toFile().deleteRecursively()
        }
    }

    @Test
    fun replacingOrClearingDisplacedFocusMakesOldResumeStaleWithoutDeletingHistory() {
        val home = Files.createTempDirectory("vexlife-working-focus-stale-displacement-").toAbsolutePath()
        try {
            val owner = VexLifeWorkingFocusOwner(home)
            val first = owner.setCurrent(draft())
            val firstRevision = assertNotNull(first.current.revisionOrNull)
            val displaced =
                owner.displace(
                    lineageRef = lineageRef,
                    threadRef = threadRef,
                    causeOwnerRef = "owner.vexlife-prod.attention",
                    causeRef = "attention.claim.current",
                    causeBindingRefs = listOf("binding.attention.claim.current"),
                )

            val replacement =
                owner.setCurrent(
                    draft(
                        formationRef = "receipt.focus.proposal.replacement",
                        objective = "Carry a newly formed focus while the old one remains history.",
                    ),
                )
            assertEquals(VexLifeWorkingFocusOwner.FocusState.ACTIVE, replacement.current.state.state)
            assertNotEquals(firstRevision.workingFocusRef, replacement.current.revisionOrNull?.workingFocusRef)

            assertFailsWith<IllegalArgumentException> {
                owner.resume(
                    lineageRef = lineageRef,
                    threadRef = threadRef,
                    displacedStateRef = displaced.current.stateRef,
                    releaseOwnerRef = "owner.resource.storage",
                    releaseRef = "retirement.resource.storage.current",
                    releaseBindingRefs = listOf("binding.retirement.current"),
                    revalidatedSourceBindingRefs = firstRevision.sourceBindingRefs,
                )
            }

            val cleared =
                owner.clear(
                    lineageRef = lineageRef,
                    threadRef = threadRef,
                    causeOwnerRef = "owner.vexlife-prod.working-focus.client",
                    causeRef = "receipt.focus.clear.current",
                    causeBindingRefs = listOf("binding.focus.clear.current"),
                )
            assertEquals(VexLifeWorkingFocusOwner.FocusState.CLEARED, cleared.current.state.state)
            assertNull(cleared.current.revisionOrNull)

            val revisionCount =
                Files.walk(home.resolve("working-focus")).use { paths ->
                    paths.filter { it.parent?.fileName?.toString() == "revisions" }.count()
                }
            assertEquals(2L, revisionCount)
        } finally {
            home.toFile().deleteRecursively()
        }
    }

    @Test
    fun consumedFormationCannotResurrectDisplacedOrClearedFocus() {
        val home = Files.createTempDirectory("vexlife-working-focus-formation-replay-").toAbsolutePath()
        try {
            val owner = VexLifeWorkingFocusOwner(home)
            val originalDraft = draft()
            owner.setCurrent(originalDraft)
            val displaced =
                owner.displace(
                    lineageRef = lineageRef,
                    threadRef = threadRef,
                    causeOwnerRef = "owner.vexlife-prod.attention",
                    causeRef = "attention.claim.current",
                    causeBindingRefs = listOf("binding.attention.claim.current"),
                )

            assertFailsWith<IllegalArgumentException> {
                owner.setCurrent(originalDraft)
            }
            assertEquals(displaced.current, owner.readCurrent(lineageRef, threadRef))

            owner.clear(
                lineageRef = lineageRef,
                threadRef = threadRef,
                causeOwnerRef = "owner.vexlife-prod.working-focus.client",
                causeRef = "receipt.focus.clear.current",
                causeBindingRefs = listOf("binding.focus.clear.current"),
            )
            val cleared = assertNotNull(owner.readCurrent(lineageRef, threadRef))
            assertEquals(VexLifeWorkingFocusOwner.FocusState.CLEARED, cleared.state.state)

            assertFailsWith<IllegalArgumentException> {
                owner.setCurrent(originalDraft)
            }
            assertEquals(cleared, owner.readCurrent(lineageRef, threadRef))
        } finally {
            home.toFile().deleteRecursively()
        }
    }

    @Test
    fun consumedFormationCannotBeReboundToDifferentTruth() {
        val home = Files.createTempDirectory("vexlife-working-focus-formation-rebind-").toAbsolutePath()
        try {
            val owner = VexLifeWorkingFocusOwner(home)
            val originalDraft = draft()
            owner.setCurrent(originalDraft)
            owner.displace(
                lineageRef = lineageRef,
                threadRef = threadRef,
                causeOwnerRef = "owner.vexlife-prod.attention",
                causeRef = "attention.claim.current",
                causeBindingRefs = listOf("binding.attention.claim.current"),
            )

            assertFailsWith<IllegalArgumentException> {
                owner.setCurrent(
                    originalDraft.copy(
                        objective = "Attempt to reuse one consumed formation identity for different truth.",
                    ),
                )
            }
        } finally {
            home.toFile().deleteRecursively()
        }
    }

    @Test
    fun repeatedExactFormationOrResumeIsIdempotentNotASecondEffect() {
        val home = Files.createTempDirectory("vexlife-working-focus-idempotent-").toAbsolutePath()
        try {
            val owner = VexLifeWorkingFocusOwner(home)
            val first = owner.setCurrent(draft())
            val repeated = owner.setCurrent(draft())
            assertEquals("IDEMPOTENT_CURRENT", repeated.state)
            assertFalse(repeated.effectPerformedNow)
            assertEquals(first.current, repeated.current)

            val displaced =
                owner.displace(
                    lineageRef = lineageRef,
                    threadRef = threadRef,
                    causeOwnerRef = "owner.vexlife-prod.attention",
                    causeRef = "attention.claim.current",
                    causeBindingRefs = listOf("binding.attention.claim.current"),
                )
            val revision = assertNotNull(displaced.current.revisionOrNull)
            val resumed =
                owner.resume(
                    lineageRef = lineageRef,
                    threadRef = threadRef,
                    displacedStateRef = displaced.current.stateRef,
                    releaseOwnerRef = "owner.resource.storage",
                    releaseRef = "retirement.resource.storage.current",
                    releaseBindingRefs = listOf("binding.retirement.current"),
                    revalidatedSourceBindingRefs = revision.sourceBindingRefs,
                )
            val repeatedResume =
                owner.resume(
                    lineageRef = lineageRef,
                    threadRef = threadRef,
                    displacedStateRef = displaced.current.stateRef,
                    releaseOwnerRef = "owner.resource.storage",
                    releaseRef = "retirement.resource.storage.current",
                    releaseBindingRefs = listOf("binding.retirement.current"),
                    revalidatedSourceBindingRefs = revision.sourceBindingRefs,
                )
            assertEquals("IDEMPOTENT_RESUMED", repeatedResume.state)
            assertFalse(repeatedResume.effectPerformedNow)
            assertEquals(resumed.current, repeatedResume.current)
        } finally {
            home.toFile().deleteRecursively()
        }
    }
}

// [VXG RealForever]
