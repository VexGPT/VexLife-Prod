package vexlife.diagnostics

import java.io.ByteArrayOutputStream
import java.lang.management.ManagementFactory
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.util.ArrayDeque
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import vexlife.contract.VexContractJson
import vexlife.logging.VexChronologyFile
import vexlife.logging.VexLogger
import vexlife.model.heretic.HereticProductIdentity
import vexlife.service.VexLifeProdMacServiceLifetime

/**
 * Realizes the Diagnostics Family's Effects inside one canonical Vex Home and forms the bounded
 * debug export. It owns only debug material: the chronology files and the enhanced-capture marker.
 *
 * It also runs one low-frequency observer of the Heretic runtime process, so the chronology shows
 * when that process stopped being live even if no turn was in progress. The observer reads the
 * runtime receipt's PID and asks the OS whether that process is alive; it never probes the
 * runtime's HTTP health (a busy runtime must not be disturbed by diagnostics) and never acts on
 * what it sees. Only transitions are recorded.
 */
class VexLifeDiagnostics(
    private val home: Path,
    private val sourceSha: String,
    private val clock: () -> Instant = Instant::now,
    private val observeIntervalSeconds: Long = 30,
) : AutoCloseable {
    @Serializable
    data class Projection(
        val schemaVersion: String,
        val state: String,
        val display: VexLifeDiagnosticsFamily.DisplayState,
        val chronologyRecords: Int,
        val chronologyBytes: Long,
        val oldestAt: String?,
        val newestAt: String?,
        val exportPath: String,
        val effectsPerformed: Boolean,
    )

    @Serializable
    data class EnhancedMarker(val schemaVersion: String, val startedAt: String, val until: String)

    @Serializable
    data class ProviderStreamTrace(
        val schemaVersion: String,
        val idempotencyKey: String,
        val startedAt: String,
        val completedAt: String?,
        val turnRef: String?,
        val outcome: String?,
        val failureCode: String?,
        val reasoning: String,
        val content: String,
        val reasoningTruncated: Boolean,
        val contentTruncated: Boolean,
        val runtimePid: Long?,
        val runtimeAlive: Boolean?,
        val serviceHeapUsedMb: Long?,
        val serviceHeapMaxMb: Long?,
        val hostTotalMemoryMb: Long?,
        val hostFreeMemoryMb: Long?,
        val homeVolumeFreeMb: Long?,
    )

    @Serializable
    data class RuntimeLogIndexEntry(
        val homeRelativePath: String,
        val exportPath: String,
        val bytes: Long,
        val modifiedAt: String?,
    )

    private data class MutableProviderStreamTrace(
        val idempotencyKey: String,
        val startedAt: String,
        var completedAt: String? = null,
        var turnRef: String? = null,
        var outcome: String? = null,
        var failureCode: String? = null,
        val reasoning: StringBuilder = StringBuilder(),
        val content: StringBuilder = StringBuilder(),
        var reasoningTruncated: Boolean = false,
        var contentTruncated: Boolean = false,
        var runtimePid: Long? = null,
        var runtimeAlive: Boolean? = null,
        var serviceHeapUsedMb: Long? = null,
        var serviceHeapMaxMb: Long? = null,
        var hostTotalMemoryMb: Long? = null,
        var hostFreeMemoryMb: Long? = null,
        var homeVolumeFreeMb: Long? = null,
    )

    private val streamTraceLock = Any()
    private val streamTraces = ArrayDeque<MutableProviderStreamTrace>()

    private val scheduler: ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "vexlife-prod-diagnostics").apply { isDaemon = true }
        }

    @Volatile private var lastRuntimeLive: Pair<Long, Boolean>? = null

    fun startObserving() {
        scheduler.scheduleWithFixedDelay(
            { runCatching { observeOnce() } },
            0,
            observeIntervalSeconds,
            TimeUnit.SECONDS,
        )
    }

    override fun close() {
        scheduler.shutdownNow()
    }

    fun state(): VexLifeDiagnosticsFamily.State {
        val marker = readMarker() ?: return VexLifeDiagnosticsFamily.Recording
        // An expired capture ends itself: higher-detail capture is short-lived by design.
        return if (Instant.parse(marker.until).isAfter(clock())) {
            VexLifeDiagnosticsFamily.RecordingEnhanced(marker.until)
        } else {
            runCatching { Files.deleteIfExists(markerPath()) }
            clearProviderStreamTraces()
            VexLifeDiagnosticsFamily.Recording
        }
    }

    fun projection(effectsPerformed: Boolean = false): Projection {
        val state = state()
        val files = VexChronologyFile.files(home)
        val tail = VexChronologyFile.tail(home, Int.MAX_VALUE)
        fun at(line: String?): String? =
            line?.let { runCatching { Json.parseToJsonElement(it).jsonObject["at"]?.jsonPrimitive?.content }.getOrNull() }
        return Projection(
            schemaVersion = ProjectionSchema,
            state = state.stateRef,
            display = VexLifeDiagnosticsFamily.displayStateFor(state),
            chronologyRecords = tail.size,
            chronologyBytes = files.sumOf { runCatching { Files.size(it) }.getOrDefault(0L) },
            oldestAt = at(tail.firstOrNull()),
            newestAt = at(tail.lastOrNull()),
            exportPath = ExportPath,
            effectsPerformed = effectsPerformed,
        )
    }

    /** A Control from the person, resolved against the State observed now. */
    fun control(controlRef: String): Projection {
        val control =
            requireNotNull(VexLifeDiagnosticsFamily.controlForRef(controlRef)) { "unknown diagnostics control" }
        val act =
            requireNotNull(VexLifeDiagnosticsFamily.resolve(state(), control)) {
                "that diagnostics control is not available now"
            }
        when (act.effect) {
            VexLifeDiagnosticsFamily.Effect.BeginEnhancedCapture -> {
                val now = clock()
                val until = now.plus(EnhancedCaptureDuration)
                clearProviderStreamTraces()
                writeMarker(EnhancedMarker(MarkerSchema, now.toString(), until.toString()))
                VexLogger.record(VexLogger.Fact.EnhancedCaptureStarted, mapOf("until" to until.toString()))
            }

            VexLifeDiagnosticsFamily.Effect.EndEnhancedCapture -> {
                Files.deleteIfExists(markerPath())
                clearProviderStreamTraces()
                VexLogger.record(VexLogger.Fact.EnhancedCaptureStopped)
            }

            VexLifeDiagnosticsFamily.Effect.ClearDebugHistory -> {
                val removed = VexChronologyFile.clear(home)
                Files.deleteIfExists(markerPath())
                clearProviderStreamTraces()
                // The first fact of the new history says the old one was cleared on purpose.
                VexLogger.record(VexLogger.Fact.DebugHistoryCleared, mapOf("filesRemoved" to removed.toString()))
            }

            null -> Unit
        }
        return projection(effectsPerformed = true)
    }

    fun beginProviderStreamTrace(idempotencyKey: String) {
        if (state() !is VexLifeDiagnosticsFamily.RecordingEnhanced) return
        synchronized(streamTraceLock) {
            streamTraces.removeIf { it.idempotencyKey == idempotencyKey }
            streamTraces.addLast(
                MutableProviderStreamTrace(
                    idempotencyKey = idempotencyKey,
                    startedAt = clock().toString(),
                ),
            )
            while (streamTraces.size > MaxProviderStreamTraces) {
                streamTraces.removeFirst()
            }
        }
    }

    fun observeProviderStreamTrace(
        idempotencyKey: String,
        reasoningDelta: String? = null,
        contentDelta: String? = null,
    ) {
        synchronized(streamTraceLock) {
            val trace = streamTraces.lastOrNull { it.idempotencyKey == idempotencyKey } ?: return
            reasoningDelta?.let {
                trace.reasoningTruncated =
                    appendTailBounded(
                        target = trace.reasoning,
                        delta = it,
                        limit = MaxReasoningTraceCharacters,
                    ) || trace.reasoningTruncated
            }
            contentDelta?.let {
                trace.contentTruncated =
                    appendTailBounded(
                        target = trace.content,
                        delta = it,
                        limit = MaxContentTraceCharacters,
                    ) || trace.contentTruncated
            }
        }
    }

    fun finishProviderStreamTrace(
        idempotencyKey: String,
        turnRef: String?,
        outcome: String,
        failureCode: String?,
    ) {
        val runtime = runCatching { runtimeProcess() }.getOrNull()
        val environment = runCatching { environment() }.getOrNull()
        synchronized(streamTraceLock) {
            val trace = streamTraces.lastOrNull { it.idempotencyKey == idempotencyKey } ?: return
            trace.completedAt = clock().toString()
            trace.turnRef = turnRef
            trace.outcome = outcome
            trace.failureCode = failureCode
            trace.runtimePid = runtime?.pid
            trace.runtimeAlive = runtime?.alive
            trace.serviceHeapUsedMb = environment?.serviceHeapUsedMb
            trace.serviceHeapMaxMb = environment?.serviceHeapMaxMb
            trace.hostTotalMemoryMb = environment?.hostTotalMemoryMb
            trace.hostFreeMemoryMb = environment?.hostFreeMemoryMb
            trace.homeVolumeFreeMb = environment?.homeVolumeFreeMb
        }
    }

    fun recordTurnFailureSnapshot(turnRef: String?, failureCode: String) {
        val runtime = runCatching { runtimeProcess() }.getOrNull()
        val environment = runCatching { environment() }.getOrNull()
        VexLogger.record(
            VexLogger.Fact.ProcessSnapshotObserved,
            buildMap {
                put("reason", "TURN_FAILED")
                turnRef?.let { put("turnRef", it) }
                put("failureCode", failureCode)
                put("runtimePid", runtime?.pid?.toString() ?: "none")
                put("runtimeAlive", runtime?.alive?.toString() ?: "unknown")
                put("serviceHeapUsedMb", environment?.serviceHeapUsedMb?.toString() ?: "unknown")
                put("serviceHeapMaxMb", environment?.serviceHeapMaxMb?.toString() ?: "unknown")
                put("hostTotalMemoryMb", environment?.hostTotalMemoryMb?.toString() ?: "unknown")
                put("hostFreeMemoryMb", environment?.hostFreeMemoryMb?.toString() ?: "unknown")
                put("homeVolumeFreeMb", environment?.homeVolumeFreeMb?.toString() ?: "unknown")
            },
        )
    }

    internal fun providerStreamTraces(): List<ProviderStreamTrace> =
        synchronized(streamTraceLock) {
            streamTraces.map {
                ProviderStreamTrace(
                    schemaVersion = ProviderStreamTraceSchema,
                    idempotencyKey = it.idempotencyKey,
                    startedAt = it.startedAt,
                    completedAt = it.completedAt,
                    turnRef = it.turnRef,
                    outcome = it.outcome,
                    failureCode = it.failureCode,
                    reasoning = it.reasoning.toString(),
                    content = it.content.toString(),
                    reasoningTruncated = it.reasoningTruncated,
                    contentTruncated = it.contentTruncated,
                    runtimePid = it.runtimePid,
                    runtimeAlive = it.runtimeAlive,
                    serviceHeapUsedMb = it.serviceHeapUsedMb,
                    serviceHeapMaxMb = it.serviceHeapMaxMb,
                    hostTotalMemoryMb = it.hostTotalMemoryMb,
                    hostFreeMemoryMb = it.hostFreeMemoryMb,
                    homeVolumeFreeMb = it.homeVolumeFreeMb,
                )
            }
        }

    private fun clearProviderStreamTraces() {
        synchronized(streamTraceLock) {
            streamTraces.clear()
        }
    }

    private fun appendTailBounded(
        target: StringBuilder,
        delta: String,
        limit: Int,
    ): Boolean {
        target.append(delta)
        if (target.length <= limit) return false
        target.delete(0, target.length - limit)
        return true
    }

    /**
     * One bounded ZIP an AI can read without terminal archaeology. Stable refs, states, PIDs,
     * receipts summaries, failure codes and bounded log tails. Conversation/prompts/page contents
     * remain excluded. When the person explicitly enables Enhanced Capture, the export may also
     * contain the bounded recent model reasoning/response stream ring described in its manifest.
     */
    fun export(): ByteArray {
        val entries = linkedMapOf<String, ByteArray>()
        fun put(name: String, text: String) {
            entries[name] = text.toByteArray(StandardCharsets.UTF_8)
        }

        val enhanced = state() is VexLifeDiagnosticsFamily.RecordingEnhanced
        val traces = if (enhanced) providerStreamTraces() else emptyList()
        val chronology = VexChronologyFile.tail(home, ExportChronologyRecords)
        put("chronology.jsonl", chronology.joinToString("\n", postfix = if (chronology.isEmpty()) "" else "\n"))
        if (traces.isNotEmpty()) {
            put("enhanced/provider-stream-traces.json", VexContractJson.encode(traces))
        }
        put("current/service-lifetime.json", summarizeJson(home.resolve(VexLifeProdMacServiceLifetime.ReceiptRelativePath)))
        listOf(
                HereticProductIdentity.ProductRuntimeReceiptRelativePath,
                HereticProductIdentity.StableRuntimeReceiptRelativePath,
            )
            .forEach { relative ->
                put("current/" + relative.substringAfterLast('/'), summarizeJson(home.resolve(relative)))
            }
        put("current/runtime-process.json", VexContractJson.encode(runtimeProcess()))
        put("environment/environment.json", VexContractJson.encode(environment()))
        tailFile(home.resolve(VexLifeProdMacServiceLifetime.StdoutRelativePath))?.let { put("logs/service.out.log", it) }
        tailFile(home.resolve(VexLifeProdMacServiceLifetime.StderrRelativePath))?.let { put("logs/service.err.log", it) }
        // Home-relative names: two runtime roots can each hold an mlx-lm-server.err.log, and the
        // current one must not be overwritten by an older one inside the export.
        val runtimeLogIndex =
            findRuntimeLogs().mapNotNull { path ->
                val relative = home.relativize(path).toString().replace('\\', '/')
                val exportPath = "logs/$relative"
                val tail = tailFile(path) ?: return@mapNotNull null
                put(exportPath, tail)
                RuntimeLogIndexEntry(
                    homeRelativePath = relative,
                    exportPath = exportPath,
                    bytes = runCatching { Files.size(path) }.getOrDefault(0L),
                    modifiedAt = runCatching { Files.getLastModifiedTime(path).toInstant().toString() }.getOrNull(),
                )
            }
        put("current/runtime-log-index.json", VexContractJson.encode(runtimeLogIndex))
        recoveryReceipts().forEach { (name, text) -> put("recovery/$name", text) }
        incompleteAnswerSummaries().forEach { (name, text) -> put("recovery/$name", text) }
        put("RESULT-SUMMARY.txt", summary(chronology, entries.keys))

        val manifest =
            JsonObject(
                mapOf(
                    "schemaVersion" to JsonPrimitive(ExportSchema),
                    "formedAt" to JsonPrimitive(clock().toString()),
                    "productSourceSha" to JsonPrimitive(sourceSha),
                    "humanContentIncluded" to JsonPrimitive(false),
                    "modelStreamContentIncluded" to JsonPrimitive(traces.isNotEmpty()),
                    "providerStreamTraceLimit" to JsonPrimitive(MaxProviderStreamTraces),
                    "runtimeLogPathsPreserved" to JsonPrimitive(true),
                    "policy" to
                        JsonPrimitive(
                            if (traces.isEmpty()) {
                                "stable refs, states, PIDs, receipt summaries, failure codes and bounded log tails; " +
                                    "no Conversation, prompts, model reasoning, page contents or tokens"
                            } else {
                                "Enhanced Capture explicitly enabled: includes a bounded local ring of recent model " +
                                    "reasoning/response stream text; excludes human prompts, Conversation, Memory, page " +
                                    "contents and arbitrary Home files"
                            }
                        ),
                    "files" to
                        JsonObject(
                            entries.mapValues { (_, bytes) ->
                                JsonObject(
                                    mapOf(
                                        "bytes" to JsonPrimitive(bytes.size),
                                        "sha256" to JsonPrimitive(sha256(bytes)),
                                    )
                                )
                            }
                        ),
                )
            )
        entries["manifest.json"] = (manifest.toString() + "\n").toByteArray(StandardCharsets.UTF_8)

        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            entries.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        VexLogger.record(
            VexLogger.Fact.DebugExportFormed,
            mapOf("files" to entries.size.toString(), "chronologyRecords" to chronology.size.toString()),
        )
        return out.toByteArray()
    }

    /** One observation pass: runtime liveness transitions, plus a snapshot while enhanced. */
    internal fun observeOnce() {
        val runtime = runtimeProcess()
        val current = runtime.pid?.let { it to runtime.alive }
        val previous = lastRuntimeLive
        if (current != null && current != previous) {
            VexLogger.record(
                if (runtime.alive) VexLogger.Fact.RuntimeProcessLive else VexLogger.Fact.RuntimeProcessNoLongerLive,
                mapOf("pid" to current.first.toString(), "receipt" to (runtime.receipt ?: "none")),
            )
        }
        lastRuntimeLive = current
        if (state() is VexLifeDiagnosticsFamily.RecordingEnhanced) {
            val environment = environment()
            VexLogger.record(
                VexLogger.Fact.ProcessSnapshotObserved,
                mapOf(
                    "runtimePid" to (runtime.pid?.toString() ?: "none"),
                    "runtimeAlive" to runtime.alive.toString(),
                    "serviceHeapUsedMb" to environment.serviceHeapUsedMb.toString(),
                    "serviceHeapMaxMb" to environment.serviceHeapMaxMb.toString(),
                    "hostFreeMemoryMb" to (environment.hostFreeMemoryMb?.toString() ?: "unknown"),
                    "homeVolumeFreeMb" to (environment.homeVolumeFreeMb?.toString() ?: "unknown"),
                ),
            )
        }
    }

    @Serializable
    data class RuntimeProcess(val receipt: String?, val pid: Long?, val alive: Boolean)

    /** The runtime receipt VexLife-Prod is using (its own, else the stable bridge's) and liveness. */
    internal fun runtimeProcess(): RuntimeProcess {
        listOf(
                HereticProductIdentity.ProductRuntimeReceiptRelativePath,
                HereticProductIdentity.StableRuntimeReceiptRelativePath,
            )
            .forEach { relative ->
                val file = home.resolve(relative)
                if (Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
                    val pid =
                        runCatching {
                                (Json.parseToJsonElement(Files.readString(file)).jsonObject["runtime"] as? JsonObject)
                                    ?.get("pid")
                                    ?.jsonPrimitive
                                    ?.longOrNull
                            }
                            .getOrNull()
                    if (pid != null) {
                        val alive = ProcessHandle.of(pid).map { it.isAlive }.orElse(false)
                        return RuntimeProcess(relative, pid, alive)
                    }
                }
            }
        return RuntimeProcess(null, null, false)
    }

    @Serializable
    data class Environment(
        val productSourceSha: String,
        val osName: String,
        val osVersion: String,
        val architecture: String,
        val processors: Int,
        val javaVersion: String,
        val servicePid: Long,
        val serviceHeapUsedMb: Long,
        val serviceHeapMaxMb: Long,
        val hostTotalMemoryMb: Long?,
        val hostFreeMemoryMb: Long?,
        val homeVolumeFreeMb: Long?,
        val observedAt: String,
    )

    internal fun environment(): Environment {
        val runtime = Runtime.getRuntime()
        val os = ManagementFactory.getOperatingSystemMXBean() as? com.sun.management.OperatingSystemMXBean
        val mb = 1024L * 1024L
        return Environment(
            productSourceSha = sourceSha,
            osName = System.getProperty("os.name"),
            osVersion = System.getProperty("os.version"),
            architecture = System.getProperty("os.arch"),
            processors = runtime.availableProcessors(),
            javaVersion = System.getProperty("java.version"),
            servicePid = ProcessHandle.current().pid(),
            serviceHeapUsedMb = (runtime.totalMemory() - runtime.freeMemory()) / mb,
            serviceHeapMaxMb = runtime.maxMemory() / mb,
            hostTotalMemoryMb = os?.totalMemorySize?.div(mb),
            hostFreeMemoryMb = os?.freeMemorySize?.div(mb),
            homeVolumeFreeMb = runCatching { Files.getFileStore(home).usableSpace / mb }.getOrNull(),
            observedAt = clock().toString(),
        )
    }

    private fun summary(chronology: List<String>, files: Collection<String>): String {
        val notable =
            chronology
                .mapNotNull { line -> runCatching { Json.parseToJsonElement(line).jsonObject }.getOrNull() }
                .filter { it["fact"]?.jsonPrimitive?.content in NotableFacts }
                .takeLast(25)
        return buildString {
            appendLine("VexLife debug export")
            appendLine("formed at: ${clock()}")
            appendLine("product source: $sourceSha")
            appendLine("chronology records: ${chronology.size}")
            appendLine("files: ${files.size + 1}")
            appendLine()
            appendLine("Most recent notable facts (oldest first):")
            if (notable.isEmpty()) appendLine("  none")
            notable.forEach { record ->
                val refs = (record["refs"] as? JsonObject)?.entries?.joinToString(" ") { "${it.key}=${it.value.jsonPrimitive.content}" }
                appendLine("  ${record["at"]?.jsonPrimitive?.content}  ${record["processRole"]?.jsonPrimitive?.content}  ${record["fact"]?.jsonPrimitive?.content}  ${refs ?: ""}")
            }
            appendLine()
            appendLine("No Conversation, prompts, model reasoning, page contents or tokens are included.")
        }
    }

    private fun summarizeJson(file: Path): String {
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) return "{\"present\":false}\n"
        return runCatching {
                val text = Files.readString(file)
                if (text.length > MaxReceiptChars) text.take(MaxReceiptChars) else text
            }
            .getOrElse { "{\"present\":true,\"readable\":false}\n" }
    }

    private fun tailFile(file: Path): String? {
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) return null
        return runCatching {
                val bytes = Files.readAllBytes(file)
                val start = (bytes.size - MaxLogTailBytes).coerceAtLeast(0)
                String(bytes, start, bytes.size - start, StandardCharsets.UTF_8)
            }
            .getOrNull()
    }

    private val IncompleteAnswerSuffix = ".incomplete-answer.json"

    private fun findRuntimeLogs(): List<Path> {
        val runtime = home.resolve("runtime")
        if (!Files.isDirectory(runtime, LinkOption.NOFOLLOW_LINKS)) return emptyList()
        return Files.walk(runtime, 4).use { stream ->
            stream
                .filter {
                    it.fileName.toString().startsWith("mlx-lm-server.") &&
                        it.fileName.toString().endsWith(".log")
                }
                .toList()
                .sortedBy { home.relativize(it).toString() }
        }
    }

    private fun recoveryReceipts(): List<Pair<String, String>> {
        val directory = home.resolve("recovery/direct-companion")
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) return emptyList()
        return Files.list(directory).use { stream ->
            stream
                .filter {
                    it.fileName.toString().endsWith(".json") &&
                        !it.fileName.toString().endsWith(IncompleteAnswerSuffix)
                }
                .toList()
                .sortedBy { runCatching { Files.getLastModifiedTime(it).toMillis() }.getOrDefault(0L) }
                .takeLast(MaxRecoveryReceipts)
                .map { it.fileName.toString() to summarizeJson(it) }
        }
    }

    /**
     * A kept incomplete answer is Vex's own words: the export carries only its shape (turn, failure,
     * length, digest, time), never the text, so the export's no-model-content policy holds.
     */
    private fun incompleteAnswerSummaries(): List<Pair<String, String>> {
        val directory = home.resolve("recovery/direct-companion")
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) return emptyList()
        return Files.list(directory).use { stream ->
            stream
                .filter { it.fileName.toString().endsWith(IncompleteAnswerSuffix) }
                .toList()
                .sortedBy { runCatching { Files.getLastModifiedTime(it).toMillis() }.getOrDefault(0L) }
                .takeLast(MaxRecoveryReceipts)
                .map { file ->
                    val summary =
                        runCatching {
                                val json = Json.parseToJsonElement(Files.readString(file)).jsonObject
                                JsonObject(
                                    json.filterKeys { it != "partialAnswer" } +
                                        ("partialAnswerIncluded" to JsonPrimitive(false))
                                )
                                    .toString() + "\n"
                            }
                            .getOrElse { "{\"present\":true,\"readable\":false}\n" }
                    file.fileName.toString() to summary
                }
        }
    }

    private fun markerPath(): Path = home.resolve(EnhancedMarkerRelativePath)

    private fun readMarker(): EnhancedMarker? =
        runCatching {
                val file = markerPath()
                if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) null
                else VexContractJson.decode<EnhancedMarker>(Files.readString(file))
            }
            .getOrNull()

    private fun writeMarker(marker: EnhancedMarker) {
        Files.createDirectories(markerPath().parent)
        Files.writeString(markerPath(), VexContractJson.encode(marker))
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }

    companion object {
        const val ProjectionSchema = "vexlife-prod.diagnostics/v1"
        const val ExportSchema = "vexlife-prod.debug-export/v1"
        const val MarkerSchema = "vexlife-prod.enhanced-capture/v1"
        const val ProviderStreamTraceSchema = "vexlife-prod.provider-stream-trace/v1"
        const val ExportPath = "/api/v1/diagnostics/export"
        const val EnhancedMarkerRelativePath = "runtime/vexlife-prod/logs/enhanced-capture.json"
        val EnhancedCaptureDuration: Duration = Duration.ofHours(2)
        const val MaxProviderStreamTraces = 30
        const val MaxReasoningTraceCharacters = 128 * 1024
        const val MaxContentTraceCharacters = 64 * 1024
        const val ExportChronologyRecords = 5_000
        const val MaxLogTailBytes = 64 * 1024
        const val MaxReceiptChars = 16 * 1024
        const val MaxRecoveryReceipts = 20

        val NotableFacts =
            setOf(
                "SETUP_EFFECT_FAILED",
                "SETUP_CONTROL_NOT_LAWFUL_NOW",
                "SERVICE_START_FAILED",
                "SERVICE_PROCESS_TERMINATING",
                "SERVICE_RETIRED_VERIFIED",
                "SERVICE_STARTED_CURRENT",
                "RUNTIME_CURRENTNESS_LOST",
                "RUNTIME_RECOVERY_REQUESTED",
                "RUNTIME_RECOVERY_FAILED",
                "RUNTIME_RETIRED_UNHEALTHY",
                "RUNTIME_PROCESS_NO_LONGER_LIVE",
                "RUNTIME_QUALIFIED",
                "TURN_FAILED",
                "SEND_ATTEMPT_TERMINALIZED_AFTER_LOSS",
                "BROWSER_PAGE_UNLOCATED",
                "BROWSER_CONTEXT_FAILED",
                "SEND_OUTCOME_UNRESOLVED_SHOWN",
                "DEBUG_HISTORY_CLEARED",
            )
    }
}

// [VXG RealForever]
