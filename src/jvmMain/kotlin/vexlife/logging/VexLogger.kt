package vexlife.logging

import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong
import kotlinx.serialization.Serializable
import vexlife.contract.VexContractJson

/**
 * VexLife-Prod's one chronology of accepted or observed facts.
 *
 * Ported law (VexLife-Android `architecture/vexarchitecture/VEXLOGGER.md`):
 * ~~~text
 * rightful owner accepts / observes a fact
 * -> the observing boundary reports it here
 * -> VexLogger orders it
 * -> downstream projections (Home chronology file, stderr, debug export) persist / show it
 * ~~~
 *
 * The Logger owns no Family meaning, no Control or Effect meaning, no runtime policy, no execution
 * custody, no navigation or presentation truth. It is one root: no FamilyLogger / RuntimeLogger /
 * BrowserLogger peers. A fact is recorded when it *happened* (accepted or observed), never when it
 * is merely expected; "Ready", "page context available" or "service stopped" are logged only by the
 * owner that observed them.
 *
 * Failure isolation is absolute: [record] never throws and never changes the fact being recorded. A
 * sink that fails is dropped for that record and the Product continues exactly as it would have.
 */
object VexLogger {
    const val RecordSchema = "vexlife-prod.chronology-record/v1"

    /** Who observed or accepted the fact. The owner of the meaning, not of the log line. */
    enum class Owner(val wire: String) {
        Entry("entry"),
        Setup("setup"),
        Person("person"),
        ServiceLifetime("service-lifetime"),
        Service("service"),
        HereticRuntime("heretic-runtime"),
        Conversation("conversation"),
        Browser("browser"),
        Presentation("presentation"),
        Diagnostics("diagnostics"),
        Capability("capability"),
    }

    /**
     * Independently meaningful facts, one vocabulary. Add a fact only for a boundary whose outcome
     * cannot already be derived from the facts around it (VEXLOGGER.md §3).
     */
    enum class Fact(val owner: Owner) {
        // Setup Journey (setup process)
        EntryStateObserved(Owner.Entry),
        SetupControlAccepted(Owner.Setup),
        SetupControlNotLawfulNow(Owner.Setup),
        SetupEffectStarted(Owner.Setup),
        SetupEffectCompleted(Owner.Setup),
        SetupEffectFailed(Owner.Setup),
        SetupEffectCancelledByHuman(Owner.Setup),
        PersonLanguageSaved(Owner.Person),

        // Product service process and its lifetime owner
        ServiceProcessStarted(Owner.Service),
        ServiceListening(Owner.Service),
        ServiceStartFailed(Owner.Service),
        ServiceProcessTerminating(Owner.Service),
        ServiceRetiredVerified(Owner.ServiceLifetime),
        ServiceStartedCurrent(Owner.ServiceLifetime),
        ServiceReusedCurrent(Owner.ServiceLifetime),

        // Heretic runtime (Python/MLX)
        RuntimeCurrentnessLost(Owner.HereticRuntime),
        RuntimeRecoveryRequested(Owner.HereticRuntime),
        RuntimeRecoveryCompleted(Owner.HereticRuntime),
        RuntimeRecoveryFailed(Owner.HereticRuntime),
        RuntimeRetiredUnhealthy(Owner.HereticRuntime),
        RuntimeStartRequested(Owner.HereticRuntime),
        RuntimeQualified(Owner.HereticRuntime),
        RuntimeProcessNoLongerLive(Owner.HereticRuntime),
        RuntimeProcessLive(Owner.HereticRuntime),

        // Conversation
        SendAccepted(Owner.Conversation),
        ProviderInvocationRequested(Owner.Conversation),
        TurnCompleted(Owner.Conversation),
        TurnFailed(Owner.Conversation),
        SendAttemptTerminalizedAfterLoss(Owner.Conversation),

        // Capability bridge: a request Vex made, and what its owner returned.
        CapabilityRequestObserved(Owner.Capability),
        CapabilityResultReturned(Owner.Capability),

        // Browser and presentation, reported by the web presentation that observed them
        BrowserPageIdentityObserved(Owner.Browser),
        BrowserPageUnlocated(Owner.Browser),
        BrowserContextRequested(Owner.Browser),
        BrowserContextReady(Owner.Browser),
        BrowserContextFailed(Owner.Browser),
        BrowserContextInvalidated(Owner.Browser),
        PresentationArrived(Owner.Presentation),
        SendOutcomeUnresolvedShown(Owner.Presentation),

        // Diagnostics (the Logger's own downstream projections)
        DebugHistoryCleared(Owner.Diagnostics),
        DebugExportFormed(Owner.Diagnostics),
        EnhancedCaptureStarted(Owner.Diagnostics),
        EnhancedCaptureStopped(Owner.Diagnostics),
        ProcessSnapshotObserved(Owner.Diagnostics);

        /** `SEND_ACCEPTED` — the wire name. */
        val wire: String = name.replace(Regex("([a-z])([A-Z])"), "$1_$2").uppercase()

        companion object {
            fun fromWire(value: String): Fact? = entries.firstOrNull { it.wire == value }
        }
    }

    /**
     * One recorded fact. [refs] carry stable coordinates (state/control refs, source SHA, PID,
     * turn ref, failure code), never human content. [sequence] orders records written by one
     * process; it is storage order, not identity. [at] is the primary chronology.
     */
    @Serializable
    data class Record(
        val schemaVersion: String,
        val at: String,
        val sequence: Long,
        val processRole: String,
        val processPid: Long,
        val sourceSha: String?,
        val owner: String,
        val fact: String,
        val refs: Map<String, String>,
        val detail: String?,
    )

    fun interface Sink {
        fun write(record: Record)
    }

    private const val DetailLimit = 1_500
    private const val RefLimit = 240
    private val RefKey = Regex("[a-z][a-zA-Z0-9]{0,40}")

    private val sequence = AtomicLong(0)

    @Volatile private var processRole: String = "unbound"

    @Volatile private var sourceSha: String? = null

    @Volatile private var sinks: List<Sink> = listOf(StandardErrorSink)

    @Volatile private var clock: () -> Instant = Instant::now

    /**
     * Names this process and, once the Vex Home is known to be canonical, adds its chronology file.
     * Before a Home exists facts go to standard error only; nothing is written anywhere else.
     */
    fun bind(processRole: String, sourceSha: String?, chronologyHome: Path?) {
        this.processRole = processRole
        this.sourceSha = sourceSha
        sinks =
            listOfNotNull(StandardErrorSink, chronologyHome?.let { VexChronologyFile.sinkFor(it) })
    }

    /** Adds the Home chronology once the Home has been formed or found during this process. */
    fun bindHome(chronologyHome: Path) {
        if (sinks.none { it is VexChronologyFile.HomeSink }) {
            sinks = sinks + VexChronologyFile.sinkFor(chronologyHome)
        }
    }

    fun record(fact: Fact, refs: Map<String, String> = emptyMap(), detail: String? = null) {
        try {
            val record =
                Record(
                    schemaVersion = RecordSchema,
                    at = clock().toString(),
                    sequence = sequence.incrementAndGet(),
                    processRole = processRole,
                    processPid = ProcessHandle.current().pid(),
                    sourceSha = sourceSha,
                    owner = fact.owner.wire,
                    fact = fact.wire,
                    refs =
                        refs
                            .filterKeys { RefKey.matches(it) }
                            .mapValues { (_, value) -> value.take(RefLimit) }
                            .toSortedMap(),
                    detail = detail?.take(DetailLimit),
                )
            sinks.forEach { sink -> runCatching { sink.write(record) } }
        } catch (_: Throwable) {
            // Traceability follows truth; it never controls it.
        }
    }

    /** Test seam: observe records in memory instead of the process sinks. */
    internal fun <T> capturing(block: (List<Record>) -> T): T {
        val captured = java.util.Collections.synchronizedList(mutableListOf<Record>())
        val previous = sinks
        sinks = listOf(Sink { captured += it })
        return try {
            block(captured)
        } finally {
            sinks = previous
        }
    }

    internal fun withSinksForTest(replacement: List<Sink>, block: () -> Unit) {
        val previous = sinks
        sinks = replacement
        try {
            block()
        } finally {
            sinks = previous
        }
    }

    /** Live developer projection: one line per fact on standard error, prefixed for grepping. */
    object StandardErrorSink : Sink {
        const val Prefix = "[VexLogger] "

        override fun write(record: Record) {
            System.err.println(Prefix + VexContractJson.compactEncode(record))
        }
    }
}

/**
 * The durable, bounded chronology inside a canonical Vex Home:
 * `runtime/vexlife-prod/logs/chronology/chronology.jsonl` plus at most [MaxRotated] rotated files
 * of at most [MaxFileBytes] each. Several processes (setup, service) append to the same file, so
 * every append holds an exclusive file lock; a record is one JSON line.
 *
 * This is diagnostics-owned history. Clearing it never touches Conversation, Memory, Home identity
 * or any ownership/currentness receipt.
 */
object VexChronologyFile {
    const val RelativeDirectory = "runtime/vexlife-prod/logs/chronology"
    const val CurrentName = "chronology.jsonl"
    const val MaxFileBytes = 2L * 1024L * 1024L
    const val MaxRotated = 7

    class HomeSink internal constructor(private val home: Path) : VexLogger.Sink {
        override fun write(record: VexLogger.Record) {
            append(home, VexContractJson.compactEncode(record))
        }
    }

    fun sinkFor(home: Path): VexLogger.Sink = HomeSink(home.toAbsolutePath().normalize())

    fun directory(home: Path): Path = home.resolve(RelativeDirectory)

    /** Every chronology file, oldest first. */
    fun files(home: Path): List<Path> {
        val directory = directory(home)
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) return emptyList()
        return Files.list(directory).use { stream ->
            stream
                .filter { it.fileName.toString().startsWith("chronology") && it.fileName.toString().endsWith(".jsonl") }
                .toList()
                .sortedWith(compareBy({ it.fileName.toString() == CurrentName }, { it.fileName.toString() }))
        }
    }

    /** The newest [limit] records across files, oldest first. Unparseable lines are skipped. */
    fun tail(home: Path, limit: Int): List<String> {
        val lines = ArrayDeque<String>()
        files(home).forEach { file ->
            Files.readAllLines(file, StandardCharsets.UTF_8).forEach { line ->
                if (line.isNotBlank()) {
                    lines.addLast(line)
                    if (lines.size > limit) lines.removeFirst()
                }
            }
        }
        return lines.toList()
    }

    /** Removes the chronology files only. Returns how many were removed. */
    fun clear(home: Path): Int {
        val removed = files(home).count { runCatching { Files.deleteIfExists(it) }.getOrDefault(false) }
        return removed
    }

    private fun append(home: Path, line: String) {
        // Only an existing Home receives a chronology; the Logger never creates a Home.
        if (!Files.isDirectory(home.resolve("config"), LinkOption.NOFOLLOW_LINKS)) return
        val directory = directory(home)
        Files.createDirectories(directory)
        val current = directory.resolve(CurrentName)
        FileChannel.open(
                current,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND,
            )
            .use { channel ->
                channel.lock().use {
                    channel.write(java.nio.ByteBuffer.wrap((line + "\n").toByteArray(StandardCharsets.UTF_8)))
                }
                if (channel.size() > MaxFileBytes) rotate(home, current)
            }
    }

    private fun rotate(home: Path, current: Path) {
        val rotated = directory(home).resolve("chronology.${System.currentTimeMillis()}.jsonl")
        runCatching { Files.move(current, rotated) }
        files(home)
            .filter { it.fileName.toString() != CurrentName }
            .dropLast(MaxRotated)
            .forEach { runCatching { Files.deleteIfExists(it) } }
    }
}

// [VXG RealForever]
