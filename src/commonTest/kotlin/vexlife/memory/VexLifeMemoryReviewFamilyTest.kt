package vexlife.memory

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import vexlife.consideration.VexLifeConsiderationContract

class VexLifeMemoryReviewFamilyTest {
    private val first =
        VexLifeMemoryReviewFamily.CandidateRef(
            "review.vexlife-prod.memory." +
                "1".repeat(
                    32,
                ),
        )
    private val second =
        VexLifeMemoryReviewFamily.CandidateRef(
            "review.vexlife-prod.memory." +
                "2".repeat(
                    32,
                ),
        )

    @Test
    fun exactPendingCandidateAdmitsKeepNotNowAndDecline() {
        val pending =
            VexLifeMemoryReviewFamily.Pending(
                candidateRef =
                    first,
                summary =
                    "Victor uses tight to mean compact and causally exact.",
                candidateClass =
                    VexLifeConsiderationContract.MemoryCandidateClass.CORRECTION,
            )

        assertEquals(
            VexLifeMemoryReviewFamily.Effect.Accept(
                first,
            ),
            assertNotNull(
                VexLifeMemoryReviewFamily.resolve(
                    pending,
                    VexLifeMemoryReviewFamily.Keep(
                        first,
                    ),
                ),
            )
                .effect,
        )
        assertEquals(
            VexLifeMemoryReviewFamily.Effect.Defer(
                first,
            ),
            assertNotNull(
                VexLifeMemoryReviewFamily.resolve(
                    pending,
                    VexLifeMemoryReviewFamily.NotNow(
                        first,
                    ),
                ),
            )
                .effect,
        )
        assertEquals(
            VexLifeMemoryReviewFamily.Effect.Dismiss(
                first,
            ),
            assertNotNull(
                VexLifeMemoryReviewFamily.resolve(
                    pending,
                    VexLifeMemoryReviewFamily.Decline(
                        first,
                    ),
                ),
            )
                .effect,
        )
    }

    @Test
    fun idleAndMismatchedCandidateAdmitNoReviewEffect() {
        assertNull(
            VexLifeMemoryReviewFamily.resolve(
                VexLifeMemoryReviewFamily.Idle,
                VexLifeMemoryReviewFamily.Keep(
                    first,
                ),
            ),
        )

        val pending =
            VexLifeMemoryReviewFamily.Pending(
                candidateRef =
                    first,
                summary =
                    "Victor uses tight to mean compact and causally exact.",
                candidateClass =
                    VexLifeConsiderationContract.MemoryCandidateClass.CORRECTION,
            )
        assertNull(
            VexLifeMemoryReviewFamily.resolve(
                pending,
                VexLifeMemoryReviewFamily.Keep(
                    second,
                ),
            ),
        )
    }

    @Test
    fun controlRefsRemainTypedThroughOneCandidateCoordinate() {
        assertEquals(
            VexLifeMemoryReviewFamily.Keep(
                first,
            ),
            VexLifeMemoryReviewFamily.controlForRef(
                VexLifeMemoryReviewFamily.ControlKeep,
                first,
            ),
        )
        assertEquals(
            VexLifeMemoryReviewFamily.NotNow(
                first,
            ),
            VexLifeMemoryReviewFamily.controlForRef(
                VexLifeMemoryReviewFamily.ControlNotNow,
                first,
            ),
        )
        assertEquals(
            VexLifeMemoryReviewFamily.Decline(
                first,
            ),
            VexLifeMemoryReviewFamily.controlForRef(
                VexLifeMemoryReviewFamily.ControlDecline,
                first,
            ),
        )
        assertNull(
            VexLifeMemoryReviewFamily.controlForRef(
                "UNKNOWN",
                first,
            ),
        )
    }

    @Test
    fun candidateIdentityRejectsUnownedOrMalformedRefs() {
        assertFailsWith<IllegalArgumentException> {
            VexLifeMemoryReviewFamily.CandidateRef(
                "review.memory.123",
            )
        }
    }
}

// [VXG RealForever]
