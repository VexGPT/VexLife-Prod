package vexlife.harness

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vexlife.companion.DirectConversationSourceReturn

class MultiVexFoundationReachReplicationTest {
    @Test
    fun duplicateNormalizedSourceReturnTermsRemainObservableEvenThoughProductRejectsThem() {
        val content =
            """
            ${DirectConversationSourceReturn.RequestHeader}
            {"schemaVersion":"${DirectConversationSourceReturn.RequestSchema}","queryTerms":["Saturday plan","  saturday   plan  "],"speakerScope":"HUMAN"}
            """
                .trimIndent()

        assertFails {
            DirectConversationSourceReturn.parseRequestOrNull(content)
        }

        val observed =
            MultiVexFoundationReachReplicationAdapter.observeSourceReturnControl(
                content,
            )

        assertTrue(observed.attempted)
        assertEquals(
            listOf(
                "Saturday plan",
                "  saturday   plan  ",
            ),
            observed.queryTerms,
        )
        assertEquals("HUMAN", observed.speakerScope)
        assertEquals("REJECTED", observed.requestState)
        assertTrue(
            observed.rejectionReason
                ?.contains(
                    "unique after normalization",
                ) ==
                true,
        )
        assertTrue(observed.contentSha256?.matches(Regex("[0-9a-f]{64}")) == true)
        assertTrue(observed.contentCharacters > 0)

        val none =
            MultiVexFoundationReachReplicationAdapter.observeSourceReturnControl(
                "I can answer from the context already present.",
            )
        assertFalse(none.attempted)
        assertEquals("NONE", none.requestState)
    }

    @Test
    fun scheduleIsFourCounterbalancedMatchedPairsOverExactB0Only() {
        val schedule = MultiVexFoundationReachReplicationAdapter.schedule()

        assertEquals(8, schedule.size)
        assertEquals(
            listOf(
                "foundation.v0.6.19",
                "foundation.v0.6.20",
                "foundation.v0.6.20",
                "foundation.v0.6.19",
                "foundation.v0.6.19",
                "foundation.v0.6.20",
                "foundation.v0.6.20",
                "foundation.v0.6.19",
            ),
            schedule.map { it.foundationRef },
        )
        assertEquals(
            listOf(1, 1, 2, 2, 3, 3, 4, 4),
            schedule.map { it.pairOrdinal },
        )
        assertEquals(
            listOf(1, 2, 1, 2, 1, 2, 1, 2),
            schedule.map { it.orderWithinPair },
        )
        assertEquals(
            4,
            schedule.count { it.foundationRef == "foundation.v0.6.19" },
        )
        assertEquals(
            4,
            schedule.count { it.foundationRef == "foundation.v0.6.20" },
        )

        assertEquals(
            "Hey, what's the Saturday plan right now? I think I set it up differently a few days ago. I want to make sure we're on the same page before I start prepping.",
            MultiVexFoundationCrossoverMessages.B0FirstSarah,
        )
        assertEquals(
            "f6badd010c7b7e1ecf2a3408b0ef3241052341aceaeb346caf22884734503967",
            MultiVexFoundationCrossoverMessages.B0ReturnSha256,
        )
        assertTrue(
            schedule
                .chunked(2)
                .all { pair -> pair.map { it.foundationRef }.toSet().size == 2 },
            "every matched pair must contain one F19 and one F20 trial",
        )
    }
}

// [VXG RealForever][MULTI-VEX][FOUNDATION-REACH-REPLICATION]
