package vexlife.continuity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class VexLifeWorkingFocusTest {
    private fun draft(
        bindings: List<String> = listOf("binding.focus.current.2", "binding.focus.current.1"),
    ) =
        VexLifeWorkingFocus.Draft(
            lineageRef = "lineage.vex.current",
            threadRef = "thread.vex.current",
            formationRef = "receipt.vex.focus.proposal.1",
            priorWorkingFocusRefOrNull = null,
            subjectScopeRefs = listOf("subject.victor", "project.self-inspection"),
            objective = "Understand the current Working Focus relationship.",
            currentQuestionOrNull = "Which owner should carry current focus?",
            retainedRelationships =
                listOf(
                    VexLifeWorkingFocus.Relationship(
                        relationshipRef = "relationship.working-continuity.neighborhood",
                        ownerRef = "owner.vexlife-prod.working-continuity.001",
                        standing = VexLifeWorkingFocus.Standing.OBSERVED,
                        compactMeaning = "Working Continuity owns checkpoint and rollover custody.",
                        sourceRefs = listOf("source.working-continuity.owner"),
                    ),
                    VexLifeWorkingFocus.Relationship(
                        relationshipRef = "relationship.focus.owner.hypothesis",
                        ownerRef = VexLifeWorkingFocus.EffectOwnerRef,
                        standing = VexLifeWorkingFocus.Standing.HYPOTHESIS,
                        compactMeaning = "Current focus needs a lifetime independent of prompt pressure.",
                        sourceRefs = listOf("source.working-focus.a0"),
                    ),
                ),
            openEdges =
                listOf(
                    VexLifeWorkingFocus.OpenEdge(
                        edgeRef = "edge.attention.displacement",
                        ownerRef = "owner.vexlife-prod.attention",
                        compactMeaning = "Return exact displacement and resume contract to Attention.",
                        sourceRefs = listOf("source.attention.a3"),
                    ),
                ),
            nextProbeOrNull = "Pressure-test exact currentness and displacement custody.",
            sourceBindingRefs = bindings,
            sourceRefs = listOf("source.self-inspection.a0", "source.working-continuity.owner"),
        )

    @Test
    fun normalizationIsDeterministicRepresentationOnly() {
        val normalized = VexLifeWorkingFocus.normalize(draft())

        assertEquals(
            listOf("binding.focus.current.1", "binding.focus.current.2"),
            normalized.sourceBindingRefs,
        )
        assertEquals(
            listOf(
                "relationship.focus.owner.hypothesis",
                "relationship.working-continuity.neighborhood",
            ),
            normalized.retainedRelationships.map { it.relationshipRef },
        )
    }

    @Test
    fun sourceBindingsAreRequiredAndCannotDuplicate() {
        assertFailsWith<IllegalArgumentException> {
            draft(bindings = emptyList())
        }
        assertFailsWith<IllegalArgumentException> {
            draft(bindings = listOf("binding.same", "binding.same"))
        }
    }

    @Test
    fun boundedWorkingTextCannotBecomeAnUnboundedPayload() {
        assertFailsWith<IllegalArgumentException> {
            draft().copy(
                objective = "x".repeat(VexLifeWorkingFocus.MaximumTextCharacters + 1),
            )
        }
    }

    @Test
    fun retainedRelationshipIdentityCannotSilentlyRepresentTwoMeanings() {
        val relationship = draft().retainedRelationships.first()
        assertFailsWith<IllegalArgumentException> {
            draft().copy(
                retainedRelationships = listOf(relationship, relationship.copy(compactMeaning = "other")),
            )
        }
    }
}

// [VXG RealForever]
