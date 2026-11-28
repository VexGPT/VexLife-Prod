package vexlife.attention

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class VexLifeAttentionPerceptibilityTest {
    private fun candidate(
        candidateRef: String,
        ownerRef: String,
        sourceRef: String,
        relationshipRef: String,
        sourceBindingRef: String,
        sourceStandingRef: String,
        compactMeaning: String,
        deeperSourceRef: String,
        reachability: VexLifeAttentionPerceptibility.Reachability,
        subjectScopeRefs: List<String> =
            emptyList(),
    ): VexLifeAttentionPerceptibility.Candidate =
        VexLifeAttentionPerceptibility.Candidate(
            candidateRef = candidateRef,
            ownerRef = ownerRef,
            sourceRef = sourceRef,
            relationshipRef = relationshipRef,
            subjectScopeRefs = subjectScopeRefs,
            sourceBindingRefs =
                listOf(
                    sourceBindingRef,
                ),
            sourceStandingRef = sourceStandingRef,
            compactMeaning = compactMeaning,
            deeperSourceRef = deeperSourceRef,
            reachability = reachability,
        )

    @Test
    fun surfacePreservesIndependentSourceMeaningWithoutInventingPriorityOrAuthority() {
        val resource =
            candidate(
                candidateRef = "candidate.resource.storage",
                ownerRef = "owner.vexlife.resource",
                sourceRef = "source.vexlife.resource.storage",
                relationshipRef = "relationship.vexlife.resource.storage",
                sourceBindingRef = "receipt.resource.storage.current.42",
                sourceStandingRef = "standing.resource.storage.notice",
                compactMeaning = "Home storage has a current source-owned notice.",
                deeperSourceRef = "source.vexlife.resource.storage.detail",
                reachability = VexLifeAttentionPerceptibility.Reachability.REACHABLE,
                subjectScopeRefs =
                    listOf(
                        "subject.vex-home",
                    ),
            )
        val cgm =
            candidate(
                candidateRef = "candidate.cgm.current",
                ownerRef = "owner.cgm.synthetic",
                sourceRef = "source.cgm.synthetic.device-1",
                relationshipRef = "relationship.cgm.synthetic.current",
                sourceBindingRef = "observation.cgm.synthetic.123",
                sourceStandingRef = "standing.cgm.synthetic.current",
                compactMeaning = "Synthetic CGM source has one current domain-owned observation.",
                deeperSourceRef = "source.cgm.synthetic.observation.123",
                reachability = VexLifeAttentionPerceptibility.Reachability.REACHABLE,
                subjectScopeRefs =
                    listOf(
                        "subject.synthetic-cgm",
                    ),
            )

        val surface =
            VexLifeAttentionPerceptibility.form(
                candidates =
                    listOf(
                        resource,
                        cgm,
                    ),
                sourceRefs =
                    listOf(
                        "project.vexlife.attention-convergence",
                        "source.vexlife-prod.main",
                    ),
            )

        assertEquals(
            listOf(
                "candidate.cgm.current",
                "candidate.resource.storage",
            ),
            surface.candidates.map {
                it.candidateRef
            },
            "A1 ordering is stable identity order only; it is not semantic priority",
        )
        assertEquals(
            "standing.cgm.synthetic.current",
            surface.candidates.first().sourceStandingRef,
        )
        assertEquals(
            "standing.resource.storage.notice",
            surface.candidates.last().sourceStandingRef,
        )
        assertFalse(
            surface.effectAuthorityGranted,
        )
    }

    @Test
    fun heldCandidateKeepsExactReopenCoordinateWithoutPretendingTheSourceWasRead() {
        val held =
            candidate(
                candidateRef = "candidate.code.runtime",
                ownerRef = "owner.code.runtime",
                sourceRef = "source.code.runtime.current",
                relationshipRef = "relationship.code.runtime.active-work",
                sourceBindingRef = "receipt.code.runtime.current.7",
                sourceStandingRef = "standing.code.runtime.current",
                compactMeaning = "A source-owned runtime relationship is relevant but not reopenable now.",
                deeperSourceRef = "source.code.runtime.detail.7",
                reachability = VexLifeAttentionPerceptibility.Reachability.HELD,
            )

        val surface =
            VexLifeAttentionPerceptibility.form(
                candidates =
                    listOf(
                        held,
                    ),
                sourceRefs =
                    listOf(
                        "source.attention.a1.test",
                    ),
            )

        assertEquals(
            VexLifeAttentionPerceptibility.Reachability.HELD,
            surface.candidates.single().reachability,
        )
        assertEquals(
            "source.code.runtime.detail.7",
            surface.candidates.single().deeperSourceRef,
        )
        assertFalse(
            surface.effectAuthorityGranted,
        )
    }

    @Test
    fun candidateRequiresCurrentSourceBindingAndBoundedCompactMeaning() {
        assertFailsWith<IllegalArgumentException> {
            VexLifeAttentionPerceptibility.Candidate(
                candidateRef = "candidate.invalid.no-binding",
                ownerRef = "owner.test",
                sourceRef = "source.test",
                relationshipRef = "relationship.test",
                subjectScopeRefs = emptyList(),
                sourceBindingRefs = emptyList(),
                sourceStandingRef = "standing.test.current",
                compactMeaning = "Current test meaning.",
                deeperSourceRef = "source.test.detail",
                reachability = VexLifeAttentionPerceptibility.Reachability.REACHABLE,
            )
        }

        assertFailsWith<IllegalArgumentException> {
            candidate(
                candidateRef = "candidate.invalid.too-large",
                ownerRef = "owner.test",
                sourceRef = "source.test",
                relationshipRef = "relationship.test",
                sourceBindingRef = "receipt.test.current",
                sourceStandingRef = "standing.test.current",
                compactMeaning =
                    "x".repeat(
                        VexLifeAttentionPerceptibility.MaximumCompactMeaningCharacters + 1,
                    ),
                deeperSourceRef = "source.test.detail",
                reachability = VexLifeAttentionPerceptibility.Reachability.REACHABLE,
            )
        }
    }

    @Test
    fun oneCandidateIdentityCannotSilentlyRepresentTwoSources() {
        val first =
            candidate(
                candidateRef = "candidate.same",
                ownerRef = "owner.first",
                sourceRef = "source.first",
                relationshipRef = "relationship.first",
                sourceBindingRef = "receipt.first.current",
                sourceStandingRef = "standing.first.current",
                compactMeaning = "First source meaning.",
                deeperSourceRef = "source.first.detail",
                reachability = VexLifeAttentionPerceptibility.Reachability.REACHABLE,
            )
        val second =
            candidate(
                candidateRef = "candidate.same",
                ownerRef = "owner.second",
                sourceRef = "source.second",
                relationshipRef = "relationship.second",
                sourceBindingRef = "receipt.second.current",
                sourceStandingRef = "standing.second.current",
                compactMeaning = "Second source meaning.",
                deeperSourceRef = "source.second.detail",
                reachability = VexLifeAttentionPerceptibility.Reachability.REACHABLE,
            )

        assertFailsWith<IllegalArgumentException> {
            VexLifeAttentionPerceptibility.form(
                candidates =
                    listOf(
                        first,
                        second,
                    ),
                sourceRefs =
                    listOf(
                        "source.attention.a1.test",
                    ),
            )
        }
    }
}

// [VXG RealForever]
