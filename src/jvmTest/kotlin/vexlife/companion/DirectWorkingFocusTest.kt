package vexlife.companion

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vexlife.continuity.VexLifeWorkingFocus
import vexlife.continuity.VexLifeWorkingFocusOwner

class DirectWorkingFocusTest {
    private val lineageRef = "lineage.vexlife.direct-working-focus.test"
    private val threadRef = "thread.vexlife.direct-working-focus.test"

    private fun draft(
        formationRef: String = "receipt.direct-working-focus.001",
        priorRevisionRefOrNull: String? = null,
        objective: String = "Keep the current Self-Inspection relationship in working focus.",
    ) =
        VexLifeWorkingFocus.Draft(
            lineageRef = lineageRef,
            threadRef = threadRef,
            formationRef = formationRef,
            priorRevisionRefOrNull = priorRevisionRefOrNull,
            subjectScopeRefs = listOf("subject.victor", "project.self-inspection"),
            objective = objective,
            currentQuestionOrNull = "What exact relationship is current now?",
            retainedRelationships =
                listOf(
                    VexLifeWorkingFocus.Relationship(
                        relationshipRef = "relationship.alpha",
                        ownerRef = "owner.alpha",
                        standing = VexLifeWorkingFocus.Standing.OBSERVED,
                        compactMeaning = "Alpha is an observed current relationship.",
                        sourceRefs = listOf("source.alpha"),
                    ),
                    VexLifeWorkingFocus.Relationship(
                        relationshipRef = "relationship.beta",
                        ownerRef = "owner.beta",
                        standing = VexLifeWorkingFocus.Standing.HYPOTHESIS,
                        compactMeaning = "Beta is a hypothesis still being pressure-tested.",
                        sourceRefs = listOf("source.beta"),
                    ),
                ),
            openEdges =
                listOf(
                    VexLifeWorkingFocus.OpenEdge(
                        edgeRef = "edge.alpha",
                        ownerRef = "owner.edge.alpha",
                        compactMeaning = "Resolve the next currentness edge.",
                        sourceRefs = listOf("source.edge.alpha"),
                    ),
                    VexLifeWorkingFocus.OpenEdge(
                        edgeRef = "edge.beta",
                        ownerRef = "owner.edge.beta",
                        compactMeaning = "Reopen exact source if verification is needed.",
                        sourceRefs = listOf("source.edge.beta"),
                    ),
                ),
            nextProbeOrNull = "Project the smallest truthful current prompt representation.",
            sourceBindingRefs = listOf("binding.direct-working-focus.current"),
            sourceRefs = listOf("source.direct-working-focus.current"),
        )

    private fun charEstimator(value: String): Int = value.length

    @Test
    fun activeOwnerResolvesExactCurrentObservationAndRevalidates() {
        val home = Files.createTempDirectory("vexlife-direct-working-focus-active-").toAbsolutePath()
        try {
            val owner = VexLifeWorkingFocusOwner(home)
            val committed = owner.setCurrent(draft())

            val observed =
                DirectWorkingFocus.observeCurrent(
                    productHome = home,
                    lineageRef = lineageRef,
                    threadRef = threadRef,
                )

            assertEquals(DirectWorkingFocus.CurrentStanding.ACTIVE, observed.standing)
            assertEquals(committed.current.stateRef, observed.stateRefOrNull)
            assertEquals(committed.current.revisionRefOrNull, observed.revisionRefOrNull)
            assertEquals(
                committed.current.revisionOrNull?.workingFocusRef,
                observed.workingFocusRefOrNull,
            )

            DirectWorkingFocus.requireCurrent(
                productHome = home,
                lineageRef = lineageRef,
                threadRef = threadRef,
                expected = observed,
            )
        } finally {
            home.toFile().deleteRecursively()
        }
    }

    @Test
    fun fullProjectionCarriesEveryDeclarativeDetailWhenItFits() {
        val home = Files.createTempDirectory("vexlife-direct-working-focus-full-").toAbsolutePath()
        try {
            val owner = VexLifeWorkingFocusOwner(home)
            owner.setCurrent(draft())
            val observed =
                DirectWorkingFocus.observeCurrent(home, lineageRef, threadRef)

            val projection =
                DirectWorkingFocus.project(
                    observation = observed,
                    availableTokens = Int.MAX_VALUE,
                    estimateTokens = ::charEstimator,
                )

            assertEquals(DirectWorkingFocus.ProjectionState.INCLUDED_FULL, projection.state)
            assertTrue(projection.detailsIncluded)
            assertTrue(projection.estimatedTokens > projection.requiredCoreTokens)
            assertNotNull(projection.contentOrNull)
            assertNotNull(projection.contentSha256OrNull)
            assertTrue(projection.contentOrNull.contains("relationship.alpha"))
            assertTrue(projection.contentOrNull.contains("Alpha is an observed current relationship."))
            assertTrue(projection.contentOrNull.contains("edge.beta"))
            assertTrue(projection.contentOrNull.contains("source.direct-working-focus.current"))
            assertTrue(projection.contentOrNull.contains("source.alpha"))
            assertTrue(projection.contentOrNull.contains("source.edge.beta"))
            assertTrue(projection.heldRelationshipRefs.isEmpty())
            assertTrue(projection.heldOpenEdgeRefs.isEmpty())
            assertTrue("source.alpha" in projection.sourceRefs)
            assertTrue("source.edge.beta" in projection.sourceRefs)
            assertFalse(projection.effectsPerformed)

            val repeated =
                DirectWorkingFocus.project(
                    observation = observed,
                    availableTokens = Int.MAX_VALUE,
                    estimateTokens = ::charEstimator,
                )
            assertEquals(projection, repeated)
        } finally {
            home.toFile().deleteRecursively()
        }
    }

    @Test
    fun constrainedProjectionHoldsWholeDetailCollectionsInsteadOfLexicalPartialSelection() {
        val home = Files.createTempDirectory("vexlife-direct-working-focus-core-").toAbsolutePath()
        try {
            val owner = VexLifeWorkingFocusOwner(home)
            owner.setCurrent(draft())
            val observed =
                DirectWorkingFocus.observeCurrent(home, lineageRef, threadRef)
            val full =
                DirectWorkingFocus.project(
                    observation = observed,
                    availableTokens = Int.MAX_VALUE,
                    estimateTokens = ::charEstimator,
                )

            val core =
                DirectWorkingFocus.project(
                    observation = observed,
                    availableTokens = full.requiredCoreTokens,
                    estimateTokens = ::charEstimator,
                )

            assertEquals(DirectWorkingFocus.ProjectionState.INCLUDED_CORE_ONLY, core.state)
            assertFalse(core.detailsIncluded)
            assertEquals(
                listOf("relationship.alpha", "relationship.beta"),
                core.heldRelationshipRefs,
            )
            assertEquals(
                listOf("edge.alpha", "edge.beta"),
                core.heldOpenEdgeRefs,
            )
            assertNotNull(core.contentOrNull)
            assertTrue(core.contentOrNull.contains("relationship.alpha"))
            assertTrue(core.contentOrNull.contains("relationship.beta"))
            assertTrue(core.contentOrNull.contains("source.direct-working-focus.current"))
            assertFalse(core.contentOrNull.contains("source.alpha"))
            assertFalse(core.contentOrNull.contains("source.edge.beta"))
            assertFalse(core.contentOrNull.contains("Alpha is an observed current relationship."))
            assertFalse(core.contentOrNull.contains("Resolve the next currentness edge."))
            assertEquals(
                listOf(
                    assertNotNull(observed.stateRefOrNull),
                    assertNotNull(observed.revisionRefOrNull),
                    "source.direct-working-focus.current",
                ).sorted(),
                core.sourceRefs,
            )
        } finally {
            home.toFile().deleteRecursively()
        }
    }

    @Test
    fun insufficientCoreRoomHoldsPromptResidencyWithoutMutatingDurableFocus() {
        val home = Files.createTempDirectory("vexlife-direct-working-focus-held-").toAbsolutePath()
        try {
            val owner = VexLifeWorkingFocusOwner(home)
            val committed = owner.setCurrent(draft())
            val observed =
                DirectWorkingFocus.observeCurrent(home, lineageRef, threadRef)
            val measured =
                DirectWorkingFocus.project(
                    observation = observed,
                    availableTokens = Int.MAX_VALUE,
                    estimateTokens = ::charEstimator,
                )

            val held =
                DirectWorkingFocus.project(
                    observation = observed,
                    availableTokens = measured.requiredCoreTokens - 1,
                    estimateTokens = ::charEstimator,
                )

            assertEquals(DirectWorkingFocus.ProjectionState.HELD_TOKEN_BUDGET, held.state)
            assertNull(held.contentOrNull)
            assertNull(held.contentSha256OrNull)
            assertEquals(0, held.estimatedTokens)
            assertEquals(measured.requiredCoreTokens, held.requiredCoreTokens)
            assertEquals(
                committed.current,
                owner.readCurrent(lineageRef, threadRef),
            )
        } finally {
            home.toFile().deleteRecursively()
        }
    }

    @Test
    fun nonActiveFocusNeverMasqueradesAsPromptResidentCurrentWork() {
        val home = Files.createTempDirectory("vexlife-direct-working-focus-standing-").toAbsolutePath()
        try {
            val owner = VexLifeWorkingFocusOwner(home)

            val absent =
                DirectWorkingFocus.observeCurrent(home, lineageRef, threadRef)
            assertEquals(DirectWorkingFocus.CurrentStanding.ABSENT, absent.standing)
            assertEquals(
                DirectWorkingFocus.ProjectionState.NOT_ACTIVE,
                DirectWorkingFocus.project(absent, Int.MAX_VALUE, ::charEstimator).state,
            )

            owner.setCurrent(draft())
            owner.displace(
                lineageRef = lineageRef,
                threadRef = threadRef,
                causeOwnerRef = "owner.vexlife-prod.attention-working-focus-convergence.001",
                causeRef = "requirement.current",
                causeBindingRefs = listOf("binding.attention.current"),
            )
            val displaced =
                DirectWorkingFocus.observeCurrent(home, lineageRef, threadRef)
            val displacedProjection =
                DirectWorkingFocus.project(displaced, Int.MAX_VALUE, ::charEstimator)
            assertEquals(DirectWorkingFocus.CurrentStanding.DISPLACED, displaced.standing)
            assertEquals(DirectWorkingFocus.ProjectionState.NOT_ACTIVE, displacedProjection.state)
            assertNull(displacedProjection.contentOrNull)

            owner.clear(
                lineageRef = lineageRef,
                threadRef = threadRef,
                causeOwnerRef = "owner.vexlife-prod.working-focus.client",
                causeRef = "receipt.focus.clear.current",
                causeBindingRefs = listOf("binding.focus.clear.current"),
            )
            val cleared =
                DirectWorkingFocus.observeCurrent(home, lineageRef, threadRef)
            assertEquals(DirectWorkingFocus.CurrentStanding.CLEARED, cleared.standing)
            assertEquals(
                DirectWorkingFocus.ProjectionState.NOT_ACTIVE,
                DirectWorkingFocus.project(cleared, Int.MAX_VALUE, ::charEstimator).state,
            )
        } finally {
            home.toFile().deleteRecursively()
        }
    }

    @Test
    fun olderObservationFailsCurrentnessAfterWorkingFocusAdvances() {
        val home = Files.createTempDirectory("vexlife-direct-working-focus-stale-").toAbsolutePath()
        try {
            val owner = VexLifeWorkingFocusOwner(home)
            val first = owner.setCurrent(draft())
            val observed =
                DirectWorkingFocus.observeCurrent(home, lineageRef, threadRef)

            owner.setCurrent(
                draft(
                    formationRef = "receipt.direct-working-focus.002",
                    priorRevisionRefOrNull =
                        assertNotNull(first.current.revisionRefOrNull),
                    objective = "Advance the Working Focus before provider residency.",
                ),
            )

            assertFailsWith<IllegalArgumentException> {
                DirectWorkingFocus.requireCurrent(
                    productHome = home,
                    lineageRef = lineageRef,
                    threadRef = threadRef,
                    expected = observed,
                )
            }
        } finally {
            home.toFile().deleteRecursively()
        }
    }
}

// [VXG RealForever]
