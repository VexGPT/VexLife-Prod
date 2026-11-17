package vexlife.companion

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class DirectContinuityProjectionWorkbenchTest {
    private data class CanonicalSource(
        val owner: String,
        val ref: String,
    )

    private data class ContinuityProjectionEpoch(
        val epoch: Int,
        val ref: String,
        val sourceRefs: List<CanonicalSource>,
    )

    private data class TurnContinuityProjection(
        val turnRef: String,
        val state: String,
        val anchors: List<String>,
        val takeaway: String,
    )

    private data class TurnEnvelope(
        val turnRef: String,
        val requestRef: String,
        val responseRef: String,
        val provenanceRefs: List<String>,
        val continuity: TurnContinuityProjection,
    )

    private data class ExecutionTail(
        val currentStateRef: String,
        val activeStackRef: String,
        val currentRequestRef: String,
        val transientPayloadRefs: List<String>,
    )

    private data class Projection(
        val checkpoint: ContinuityProjectionEpoch,
        val spine: List<TurnEnvelope>,
        val tail: ExecutionTail,
    ) {
        fun stablePrefix(): String =
            buildString {
                append("checkpoint:")
                append(checkpoint.epoch)
                append(':')
                append(checkpoint.ref)
                append('\n')
                checkpoint.sourceRefs.forEach {
                    append("source:")
                    append(it.owner)
                    append(':')
                    append(it.ref)
                    append('\n')
                }
                spine.forEach { turn ->
                    append("turn:")
                    append(turn.turnRef)
                    append(':')
                    append(turn.requestRef)
                    append(':')
                    append(turn.responseRef)
                    append('\n')
                    turn.provenanceRefs.forEach {
                        append("prov:")
                        append(it)
                        append('\n')
                    }
                    append("key:")
                    append(turn.continuity.turnRef)
                    append(':')
                    append(turn.continuity.state)
                    append(':')
                    append(turn.continuity.anchors.joinToString(","))
                    append(':')
                    append(turn.continuity.takeaway)
                    append('\n')
                }
            }

        fun fullPromptShape(): String =
            stablePrefix() +
                buildString {
                    append("state:")
                    append(tail.currentStateRef)
                    append('\n')
                    append("stack:")
                    append(tail.activeStackRef)
                    append('\n')
                    append("request:")
                    append(tail.currentRequestRef)
                    append('\n')
                    tail.transientPayloadRefs.forEach {
                        append("transient:")
                        append(it)
                        append('\n')
                    }
                }
    }

    private val canonicalSources =
        listOf(
            CanonicalSource("conversation", "head-100"),
            CanonicalSource("memory", "memory-42"),
            CanonicalSource("capability-trace", "trace-17"),
        )

    private val checkpoint =
        ContinuityProjectionEpoch(
            epoch = 100,
            ref = "C100",
            sourceRefs = canonicalSources,
        )

    private val turn101 =
        TurnEnvelope(
            turnRef = "turn-101",
            requestRef = "req-101",
            responseRef = "resp-101",
            provenanceRefs = listOf("web-returned:e17"),
            continuity =
                TurnContinuityProjection(
                    turnRef = "turn-101",
                    state = "TRUNK",
                    anchors = listOf("checkpoint:C100"),
                    takeaway = "cross-model reach is one semantic circuit",
                ),
        )

    @Test
    fun volatileExecutionTailDoesNotRewriteStablePrefix() {
        val base =
            Projection(
                checkpoint = checkpoint,
                spine = listOf(turn101),
                tail =
                    ExecutionTail(
                        currentStateRef = "reach:10-bounded",
                        activeStackRef = "stack:root",
                        currentRequestRef = "req-102",
                        transientPayloadRefs = listOf("web-chunk:0-9000"),
                    ),
            )
        val laterStage =
            base.copy(
                tail =
                    base.tail.copy(
                        transientPayloadRefs =
                            listOf(
                                "web-chunk:0-9000",
                                "web-chunk:9000-18000",
                            ),
                    ),
            )

        assertEquals(base.stablePrefix(), laterStage.stablePrefix())
        assertNotEquals(base.fullPromptShape(), laterStage.fullPromptShape())
    }

    @Test
    fun completedTurnExtendsStablePrefixAppendOnly() {
        val previous =
            Projection(
                checkpoint = checkpoint,
                spine = listOf(turn101),
                tail =
                    ExecutionTail(
                        currentStateRef = "reach:10-bounded",
                        activeStackRef = "stack:root",
                        currentRequestRef = "req-102",
                        transientPayloadRefs = emptyList(),
                    ),
            )
        val turn102 =
            TurnEnvelope(
                turnRef = "turn-102",
                requestRef = "req-102",
                responseRef = "resp-102",
                provenanceRefs = listOf("web-returned:e18"),
                continuity =
                    TurnContinuityProjection(
                        turnRef = "turn-102",
                        state = "TANGENT_RESOLVED",
                        anchors = listOf("turn-101"),
                        takeaway = "KV cache is compute continuity, not semantic truth",
                    ),
            )
        val next =
            previous.copy(
                spine = previous.spine + turn102,
                tail =
                    previous.tail.copy(
                        currentRequestRef = "req-103",
                    ),
            )

        assertTrue(next.stablePrefix().startsWith(previous.stablePrefix()))
    }

    @Test
    fun semanticRebaseChangesCacheEpochButPreservesCanonicalLineage() {
        val before =
            Projection(
                checkpoint = checkpoint,
                spine = listOf(turn101),
                tail =
                    ExecutionTail(
                        currentStateRef = "reach:10-bounded",
                        activeStackRef = "stack:root",
                        currentRequestRef = "req-102",
                        transientPayloadRefs = emptyList(),
                    ),
            )
        val nextCheckpoint =
            ContinuityProjectionEpoch(
                epoch = 102,
                ref = "C102",
                sourceRefs =
                    canonicalSources +
                        CanonicalSource(
                            owner = "conversation",
                            ref = "head-102",
                        ),
            )
        val rebased =
            Projection(
                checkpoint = nextCheckpoint,
                spine = emptyList(),
                tail = before.tail,
            )

        assertNotEquals(before.stablePrefix(), rebased.stablePrefix())
        assertTrue(rebased.checkpoint.sourceRefs.containsAll(canonicalSources))
    }

    @Test
    fun incompleteAttemptEvidenceDoesNotEnterCanonicalSpineByDefault() {
        val failedAttemptRef = "failed-turn-103.incomplete-answer"
        val projection =
            Projection(
                checkpoint = checkpoint,
                spine = listOf(turn101),
                tail =
                    ExecutionTail(
                        currentStateRef = "reach:10-bounded",
                        activeStackRef = "stack:root",
                        currentRequestRef = "req-103",
                        transientPayloadRefs = emptyList(),
                    ),
            )

        assertFalse(projection.stablePrefix().contains(failedAttemptRef))
    }

    @Test
    fun mutablePrefixStatePreventsHistoricalConversationReuse() {
        val historicalConversation =
            """
            turn:101:req|resp
            turn:102:req|resp
            """.trimIndent() + "\n"

        val currentShape =
            "foundation|extent=17\n" +
                historicalConversation +
                "request:103"
        val nextCurrentShape =
            "foundation|extent=18\n" +
                historicalConversation +
                "turn:103:req|resp\n" +
                "request:104"
        val currentReuse =
            commonPrefixLength(
                currentShape,
                nextCurrentShape,
            )

        val stablePrefix =
            "foundation|checkpoint=C100\n" +
                historicalConversation
        val projectedShape =
            stablePrefix +
                "state:extent=18\n" +
                "request:103"
        val nextProjectedShape =
            stablePrefix +
                "turn:103:req|resp\n" +
                "state:extent=19\n" +
                "request:104"
        val projectedReuse =
            commonPrefixLength(
                projectedShape,
                nextProjectedShape,
            )

        assertTrue(currentReuse < stablePrefix.length)
        assertTrue(projectedReuse >= stablePrefix.length)
        assertTrue(projectedReuse > currentReuse)
    }

    private fun commonPrefixLength(
        first: String,
        second: String,
    ): Int {
        val limit = minOf(first.length, second.length)
        var index = 0
        while (
            index < limit &&
            first[index] == second[index]
        ) {
            index += 1
        }
        return index
    }

}
