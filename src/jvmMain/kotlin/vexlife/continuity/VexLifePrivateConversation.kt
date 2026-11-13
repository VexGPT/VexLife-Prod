package vexlife.continuity

import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.Serializable
import vexlife.companion.CompanionProviderMessage
import vexlife.companion.DirectPromptContext
import vexlife.contract.VexDurableContractFile
import vexlife.filesystem.VexOwnedPath

/**
 * The running service's one private conversation, plus the durable record of those that ended.
 *
 * The transcript lives only in this object's memory. Nothing of it is written while it is open;
 * when it ends, only an interval record is written to the Home (when it began and ended, how many
 * exchanges, and the notes the person kept from Vex's proposals). A service restart loses an open
 * private conversation entirely, which is the promise Private mode makes.
 *
 * Semantics (which Controls are lawful when) belong to [VexLifePrivateModeFamily]; this owner only
 * realizes the Effects and answers what is true now.
 */
class VexLifePrivateConversation(
    private val productHome: Path,
    private val now: () -> Instant = { Instant.now() },
) {
    /** One provider call; the default speaks to the qualified local runtime. */
    fun interface Completion {
        fun complete(messages: List<CompanionProviderMessage>): String
    }

    data class Exchange(val human: String, val vex: String, val at: Instant)

    data class Snapshot(
        val startedAt: Instant,
        val exchanges: List<Exchange>,
        val keptNotes: List<String>,
        val proposedNote: String?,
    )

    @Serializable
    data class IntervalRecord(
        val schemaVersion: String,
        val intervalRef: String,
        val startedAt: String,
        val endedAt: String,
        val exchangeCount: Int,
        val keptNotes: List<String>,
    ) {
        init {
            require(
                schemaVersion == IntervalSchema &&
                    exchangeCount >= 0 &&
                    keptNotes.all { it.isNotBlank() }
            ) {
                "private interval record is invalid"
            }
            Instant.parse(startedAt)
            Instant.parse(endedAt)
        }
    }

    private class Session(val startedAt: Instant) {
        val exchanges = mutableListOf<Exchange>()
        val keptNotes = mutableListOf<String>()
        var proposedNote: String? = null
        var calling = false
    }

    private var session: Session? = null

    @Synchronized
    fun state(): VexLifePrivateModeFamily.State =
        session?.let { VexLifePrivateModeFamily.Private(it.exchanges.size, it.keptNotes.size) }
            ?: VexLifePrivateModeFamily.Kept

    @Synchronized
    fun snapshot(): Snapshot? = session?.let {
        Snapshot(it.startedAt, it.exchanges.toList(), it.keptNotes.toList(), it.proposedNote)
    }

    @Synchronized
    fun begin() {
        check(session == null) { "a private conversation is already open" }
        session = Session(now())
    }

    /**
     * One private exchange. The prompt is this conversation so far plus the Memory Vex already had;
     * nothing is written anywhere. The model call runs outside the lock so the current state stays
     * readable while Vex replies.
     */
    fun send(content: String, memorySystemMessage: String?, completion: Completion): Exchange {
        val (open, messages) = beginCall { prompt(it, PrivateSystem, memorySystemMessage, content) }
        try {
            val reply =
                completion.complete(messages).trim().ifEmpty {
                    error("Vex returned an empty private reply")
                }
            return synchronized(this) {
                check(session === open) { "the private conversation ended while Vex was replying" }
                Exchange(content, reply, now()).also { open.exchanges += it }
            }
        } finally {
            endCall(open)
        }
    }

    /** Vex's proposal of the one thing worth keeping. Nothing is kept until [keepNote]. */
    fun proposeNote(completion: Completion): String {
        val (open, messages) =
            beginCall {
                check(it.exchanges.isNotEmpty()) {
                    "there is nothing in this private conversation to keep yet"
                }
                prompt(it, NoteSystem, null, NoteRequest)
            }
        try {
            val note =
                completion.complete(messages).trim().take(NoteCharacterLimit).ifEmpty {
                    error("Vex proposed an empty note")
                }
            return synchronized(this) {
                check(session === open) {
                    "the private conversation ended while Vex was writing the note"
                }
                open.proposedNote = note
                note
            }
        } finally {
            endCall(open)
        }
    }

    @Synchronized
    private fun beginCall(
        form: (Session) -> List<CompanionProviderMessage>
    ): Pair<Session, List<CompanionProviderMessage>> {
        val open = checkNotNull(session) { "no private conversation is open" }
        check(!open.calling) { "Vex is still replying in this private conversation" }
        val messages = form(open)
        open.calling = true
        return open to messages
    }

    @Synchronized
    private fun endCall(open: Session) {
        open.calling = false
    }

    /** Keeps exactly the words Vex proposed: the agreement is on those words, not others. */
    @Synchronized
    fun keepNote(text: String) {
        val open = checkNotNull(session) { "no private conversation is open" }
        require(open.proposedNote != null && text == open.proposedNote) {
            "only the note Vex just proposed can be kept"
        }
        open.keptNotes += text
        open.proposedNote = null
    }

    @Synchronized
    fun declineNote() {
        checkNotNull(session) { "no private conversation is open" }.proposedNote = null
    }

    /** Ends the private conversation: the transcript is dropped, the interval is recorded. */
    @Synchronized
    fun end(): IntervalRecord {
        val open = checkNotNull(session) { "no private conversation is open" }
        check(!open.calling) { "Vex is still replying in this private conversation" }
        val endedAt = now()
        val record =
            IntervalRecord(
                schemaVersion = IntervalSchema,
                intervalRef =
                    "private-interval.vexlife-prod.${open.startedAt.toEpochMilli()}.${UUID.randomUUID().toString().take(8)}",
                startedAt = open.startedAt.toString(),
                endedAt = endedAt.toString(),
                exchangeCount = open.exchanges.size,
                keptNotes = open.keptNotes.toList(),
            )
        VexDurableContractFile.write(
            ownedRoot = home(),
            path = intervalsDir().resolve("${record.intervalRef}.json"),
            value = record,
        )
        session = null
        return record
    }

    /** Ended private conversations, oldest first. */
    fun intervals(): List<IntervalRecord> {
        val dir = intervalsDir()
        if (!Files.isDirectory(dir, LinkOption.NOFOLLOW_LINKS)) return emptyList()
        return Files.list(dir).use { paths ->
            paths
                .filter { it.fileName.toString().endsWith(".json") }
                .toList()
                .mapNotNull {
                    VexDurableContractFile.read<IntervalRecord>(ownedRoot = home(), path = it)
                }
                .sortedBy { Instant.parse(it.startedAt) }
        }
    }

    private fun home(): Path = VexOwnedPath.requireRoot(productHome, "canonical VexLife Home")

    private fun intervalsDir(): Path = VexOwnedPath.resolveInside(home(), IntervalsRelativePath)

    private fun prompt(
        open: Session,
        system: String,
        memorySystemMessage: String?,
        request: String,
    ): List<CompanionProviderMessage> {
        val history = mutableListOf<CompanionProviderMessage>()
        var budget = HistoryTokenBudget
        for (exchange in open.exchanges.asReversed()) {
            val pair =
                listOf(
                    CompanionProviderMessage("user", exchange.human),
                    CompanionProviderMessage("assistant", exchange.vex),
                )
            val cost = pair.sumOf { DirectPromptContext.estimatedMessageTokens(it.content) }
            if (cost > budget) break
            budget -= cost
            history.addAll(0, pair)
        }
        val systemContent = listOfNotNull(system, memorySystemMessage).joinToString("\n\n")
        return listOf(CompanionProviderMessage("system", systemContent)) +
            history +
            CompanionProviderMessage("user", request)
    }

    companion object {
        const val IntervalSchema = "vexlife-prod.private-interval/v1"
        const val IntervalsRelativePath = "runtime/vexlife-prod/continuity/private-intervals"
        const val NoteCharacterLimit = 600
        private const val HistoryTokenBudget = 60_000

        private val PrivateSystem =
            listOf(
                    "VEXLIFE_PRIVATE_CONVERSATION/v1",
                    "This conversation is private. Nothing from it is kept after it ends unless you and the person both agree to keep a note.",
                    "Speak as you normally would. You still know what you knew before it began.",
                )
                .joinToString("\n")

        private val NoteSystem =
            listOf(
                    "VEXLIFE_PRIVATE_NOTE/v1",
                    "This private conversation will be forgotten when it ends. The person may keep one short note from it.",
                    "Write the one thing most worth keeping, in one or two plain sentences, from your own point of view, as the person would want it remembered.",
                    "Write only the note.",
                )
                .joinToString("\n")

        private const val NoteRequest = "What would you keep from our private conversation?"
    }
}

// [VXG RealForever]
