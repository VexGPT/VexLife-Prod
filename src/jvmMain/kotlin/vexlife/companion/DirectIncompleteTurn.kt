package vexlife.companion

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Instant
import kotlinx.serialization.Serializable
import vexlife.contract.VexDurableContractFile
import vexlife.conversation.DirectConversationStore
import vexlife.filesystem.VexOwnedPath
import vexlife.logging.VexLogger

/**
 * What a human turn that did not finish left behind — kept and shown, never hidden.
 *
 * Cause and effect it owns: a turn can stop after Vex has already spoken part of its answer (the
 * provider stream closed before it finished, the runtime went away, a bound was reached). The
 * person's message is already durable — it stays outside the canonical chain as an abandoned
 * attempt — and the failure receipt says why the turn stopped. What was missing is the part of the
 * answer the person had already read: it lived only in the browser and vanished with the failure or
 * the next reload.
 *
 * This owner keeps exactly that visible answer text beside the failure receipt, and projects every
 * abandoned attempt (the message, what came through, why it stopped) so the timeline shows it in
 * place after any reload. Nothing here is deleted by Product code.
 *
 * What it is not: model input, Memory, or the canonical Conversation chain. An incomplete answer is
 * evidence of what was said, not a completed exchange, so the next turn's prompt is unchanged.
 * Reasoning stays session-scoped exactly as for completed turns (and in Diagnostics Enhanced
 * Capture when the person turned that on).
 */
object DirectIncompleteTurn {
    const val Schema = "vexlife-prod.direct-incomplete-answer/v1"
    const val MaximumAnswerCharacters = 64 * 1024
    private const val Directory = "recovery/direct-companion"

    @Serializable
    data class Record(
        val schemaVersion: String,
        val turnRef: String,
        val requestEventRef: String,
        val failureCode: String,
        /** The answer text the person was shown, from its first character. */
        val partialAnswer: String,
        /** Characters Vex streamed; more than [partialAnswer] holds when truncated. */
        val streamedCharacters: Int,
        val partialAnswerTruncated: Boolean,
        val partialAnswerSha256: String,
        val formedAt: String,
    )

    /** One abandoned human turn, as the person should see it. */
    data class Observed(
        val turnRef: String,
        val requestEventRef: String,
        val requestContent: String,
        val requestSequence: Long,
        val requestFormedAt: String,
        /** What came through of Vex's answer; null when nothing had been shown. */
        val partialAnswer: String?,
        val partialAnswerTruncated: Boolean,
        /** The whole answer was recorded but the turn never became current. */
        val answerRecordedNotCommitted: Boolean,
        val failureCode: String?,
        val exactNextSafeRoute: String?,
        val stoppedAt: String?,
    )

    /**
     * Accumulates exactly the answer text one turn showed the person, in order. Bounded from the
     * start: the person read from the beginning, so the beginning is what is kept.
     */
    class Visible {
        private val text = StringBuilder()
        private var streamed = 0

        @Synchronized
        fun append(delta: String?) {
            if (delta.isNullOrEmpty()) return
            streamed += delta.length
            val room = MaximumAnswerCharacters - text.length
            if (room > 0) text.append(delta, 0, minOf(room, delta.length))
        }

        @Synchronized fun text(): String = text.toString()

        @Synchronized fun streamedCharacters(): Int = streamed

        @Synchronized fun truncated(): Boolean = streamed > text.length
    }

    private fun recordPath(productHome: Path, turnRef: String): Path =
        VexOwnedPath.resolveInside(
            VexOwnedPath.resolveInside(productHome, Directory),
            "$turnRef.incomplete-answer.json",
        )

    /**
     * Keeps [visible] for the failed turn whose durable request is [request]. Writes nothing when
     * no answer text had been shown. Never throws: keeping evidence must not change the failure.
     */
    internal fun keep(
        productHome: Path,
        request: DirectConversationStore.Event,
        failureCode: String,
        visible: Visible,
        formedAt: Instant,
    ): Record? =
        try {
            val answer = visible.text()
            if (answer.isBlank()) {
                null
            } else {
                val path = recordPath(productHome, request.turnRef)
                VexDurableContractFile.read<Record>(productHome, path)
                    ?: Record(
                            schemaVersion = Schema,
                            turnRef = request.turnRef,
                            requestEventRef = request.eventRef,
                            failureCode = failureCode,
                            partialAnswer = answer,
                            streamedCharacters = visible.streamedCharacters(),
                            partialAnswerTruncated = visible.truncated(),
                            partialAnswerSha256 = sha256(answer),
                            formedAt = formedAt.toString(),
                        )
                        .also { record ->
                            VexDurableContractFile.write(productHome, path, record)
                            VexLogger.record(
                                VexLogger.Fact.TurnIncompleteAnswerKept,
                                mapOf(
                                    "turnRef" to record.turnRef,
                                    "failureCode" to failureCode,
                                    "characters" to record.streamedCharacters.toString(),
                                    "truncated" to record.partialAnswerTruncated.toString(),
                                ),
                            )
                        }
            }
        } catch (error: Exception) {
            VexLogger.record(
                VexLogger.Fact.TurnIncompleteAnswerNotKept,
                mapOf("turnRef" to request.turnRef, "failureCode" to failureCode),
                detail = error.javaClass.simpleName + ": " + (error.message ?: ""),
            )
            null
        }

    /** Every abandoned attempt in [snapshot], oldest first, with what it left. */
    fun observe(
        productHome: Path,
        snapshot: DirectConversationStore.Snapshot,
    ): List<Observed> =
        snapshot.abandonedAttempts
            .map { attempt ->
                val request = attempt.requestEvent
                val record = runCatching {
                    VexDurableContractFile.read<Record>(
                        productHome,
                        recordPath(productHome, request.turnRef),
                    )
                }
                    .getOrNull()
                    ?.takeIf {
                        it.schemaVersion == Schema &&
                            it.turnRef == request.turnRef &&
                            it.partialAnswerSha256 == sha256(it.partialAnswer)
                    }
                val failure = DirectCompanionTurn.failureReceiptOrNull(productHome, request.turnRef)
                val recordedResponse = attempt.responseEventOrNull
                Observed(
                    turnRef = request.turnRef,
                    requestEventRef = request.eventRef,
                    requestContent = request.content,
                    requestSequence = request.sequence,
                    requestFormedAt = request.formedAt,
                    partialAnswer = recordedResponse?.content ?: record?.partialAnswer,
                    partialAnswerTruncated =
                        recordedResponse == null && record?.partialAnswerTruncated == true,
                    answerRecordedNotCommitted = recordedResponse != null,
                    failureCode = failure?.failureCode ?: record?.failureCode,
                    exactNextSafeRoute = failure?.exactNextSafeRoute,
                    stoppedAt = failure?.formedAt ?: record?.formedAt,
                )
            }
            .sortedWith(
                compareBy(
                    {
                        runCatching { Instant.parse(it.requestFormedAt) }
                            .getOrDefault(Instant.EPOCH)
                    },
                    { it.requestSequence },
                )
            )

    private fun sha256(text: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}

// [VXG RealForever]
