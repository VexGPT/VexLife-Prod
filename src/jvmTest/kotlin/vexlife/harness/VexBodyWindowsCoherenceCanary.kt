package vexlife.harness

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.lang.management.ManagementFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.roundToInt
import kotlin.system.exitProcess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import vexlife.capability.VexLifeCapabilityAtlas
import vexlife.capability.VexLifeCapabilityBridge
import vexlife.capability.VexLifeCapabilityRequest
import vexlife.companion.CompanionProviderMessage
import vexlife.companion.DirectCompanionConnection
import vexlife.companion.DirectConversationSourceReturn
import vexlife.companion.DirectCompanionTurn
import vexlife.contract.VexDurableContractFile
import vexlife.filesystem.VexOwnedPath
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.home.VexLifeProdHomeFiles
import vexlife.local.VexLifeProdRuntimeSession
import vexlife.local.VexLifeProdRuntimeSessionFactory
import vexlife.model.heretic.HereticCompanionRepresentation
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeRealization
import vexlife.model.heretic.HereticTextControlCapabilityWire
import vexlife.model.heretic.runtime.FileHereticModelConfigurationStore
import vexlife.platform.windows.VexLifeWindowsHereticRuntime
import vexlife.platform.windows.VexLifeWindowsRuntimeMaterialization
import vexlife.setup.VexLifeProdSetupModel

/**
 * Proof-only Windows coherence canary for the current shared VexBody.
 *
 * This deliberately does not copy the Mac-only Family-B realization. The exact already-selected
 * Windows Qwen 4B runtime is borrowed from the person's canonical Home while all synthetic
 * Conversation state lives in one disposable scratch Home.
 *
 * WINDOWS RUNTIME COHERENCE != EXE LIVED ACCEPTANCE
 * SCRATCH CONVERSATION != HUMAN CONVERSATION
 * NO ACQUISITION != NO RUNTIME EFFECT
 */
object VexBodyWindowsCoherenceCanary {
    private const val ResultSchema =
        "vexlife-prod.vexbody-windows-coherence-result/v1"
    private const val Marker =
        "COPPER-ORCHID-17"
    private const val FalseMarker =
        "SILVER-LILY-44"
    private const val Rule =
        "KEEP-UNFINISHED-FIRST"
    private const val FalseRule =
        "KEEP-COMPLETED-FIRST"
    private const val EffectiveInputBudget =
        52_000
    private const val PerformanceSampleIntervalMillis =
        2_000L
    private const val ModelCallCooldownMillis =
        3_000L
    private const val HostMemoryHoldFloorBytes =
        2L * 1024L * 1024L * 1024L
    private const val HostMemoryHoldPercent =
        8L
    private const val HostMemoryWarningPercent =
        15L
    private const val HeapHoldPercent =
        90L
    private const val ActiveExactOwnedFailure =
        "exact Windows Heretic runtime is still owned by a live parent process"
    private const val RuntimeCustodyOwned =
        "OWNED_BY_CANARY"
    private const val RuntimeCustodyBorrowed =
        "BORROWED_ACTIVE_EXACT"

    private val json =
        Json {
            encodeDefaults = true
            explicitNulls = false
            prettyPrint = true
        }

    private data class TurnRecord(
        val phase: String,
        val ordinal: Int,
        val result: DirectCompanionTurn.Result,
    )

    private class RunLog {
        private val lines =
            mutableListOf<String>()

        fun add(line: String) {
            val rendered =
                Instant.now().toString() +
                    "\t" +
                    line
            lines +=
                rendered
            println(rendered)
        }

        fun text(): String =
            lines.joinToString(
                separator = "\n",
                postfix = "\n",
            )
    }

    private class PerformanceSafetyHold(
        message: String,
    ) : IllegalStateException(
        message,
    )

    private class PerformanceMonitor(
        private val resultDir: Path,
        private val log: RunLog,
    ) : AutoCloseable {
        private val running =
            AtomicBoolean(
                false,
            )
        private val holdReason =
            AtomicReference<String?>(
                null,
            )
        private val operatingSystem =
            ManagementFactory
                .getOperatingSystemMXBean() as?
                com.sun.management.OperatingSystemMXBean
        private val memory =
            ManagementFactory
                .getMemoryMXBean()
        private val threadBean =
            ManagementFactory
                .getThreadMXBean()
        private val collectors =
            ManagementFactory
                .getGarbageCollectorMXBeans()
        private val samplePath =
            resultDir.resolve(
                "performance.jsonl",
            )
        private val durablePath =
            System.getenv(
                "VEXBODY_PERF_DURABLE_PATH",
            )
                ?.takeIf {
                    it.isNotBlank()
                }
                ?.let {
                    Path.of(
                        it,
                    )
                        .toAbsolutePath()
                        .normalize()
                }

        private var worker: Thread? =
            null
        private var sampleCount =
            0L
        private var firstFreePhysicalBytes =
            -1L
        private var lastFreePhysicalBytes =
            -1L
        private var minFreePhysicalBytes =
            Long.MAX_VALUE
        private var minFreePhysicalPercent =
            100.0
        private var firstHeapUsedBytes =
            -1L
        private var lastHeapUsedBytes =
            -1L
        private var maxHeapUsedBytes =
            0L
        private var maxHeapUsedPercent =
            0.0
        private var maxSystemCpuPercent =
            0.0
        private var maxProcessCpuPercent =
            0.0
        private var maxCommittedVirtualMemoryBytes =
            0L
        private var maxThreadCount =
            0
        private var firstGcTimeMillis =
            -1L
        private var lastGcTimeMillis =
            -1L
        private var maxGcTimeMillis =
            0L
        private var memoryWarningObserved =
            false

        fun start() {
            if (
                !running.compareAndSet(
                    false,
                    true,
                )
            ) {
                return
            }
            Files.createDirectories(
                resultDir,
            )
            durablePath
                ?.parent
                ?.let {
                    Files.createDirectories(
                        it,
                    )
                }
            sample(
                label = "MONITOR_START",
            )
            worker =
                Thread(
                    {
                        while (
                            running.get()
                        ) {
                            try {
                                Thread.sleep(
                                    PerformanceSampleIntervalMillis,
                                )
                                if (
                                    running.get()
                                ) {
                                    sample(
                                        label = "PERIODIC",
                                    )
                                }
                            } catch (
                                _: InterruptedException,
                            ) {
                                Thread
                                    .currentThread()
                                    .interrupt()
                                break
                            } catch (
                                error: Throwable,
                            ) {
                                log.add(
                                    "performance-monitor-sample-error=" +
                                        error.javaClass.name +
                                        ":" +
                                        (error.message ?: ""),
                                )
                            }
                        }
                    },
                    "vexbody-windows-performance-monitor",
                )
                    .apply {
                        isDaemon =
                            true
                        start()
                    }
        }

        fun checkpoint(
            label: String,
        ) {
            sample(
                label = label,
            )
            holdReason
                .get()
                ?.let {
                    reason ->
                    throw PerformanceSafetyHold(
                        reason,
                    )
                }
        }

        fun cooldown(
            label: String,
        ) {
            checkpoint(
                label + "_BEFORE_COOLDOWN",
            )
            Thread.sleep(
                ModelCallCooldownMillis,
            )
            checkpoint(
                label + "_AFTER_COOLDOWN",
            )
        }

        @Synchronized
        fun summary(): JsonObject =
            buildJsonObject {
                put(
                    "sampleCount",
                    sampleCount,
                )
                if (
                    firstFreePhysicalBytes >=
                        0
                ) {
                    put(
                        "firstFreePhysicalBytes",
                        firstFreePhysicalBytes,
                    )
                    put(
                        "lastFreePhysicalBytes",
                        lastFreePhysicalBytes,
                    )
                    put(
                        "freePhysicalDeltaBytes",
                        lastFreePhysicalBytes -
                            firstFreePhysicalBytes,
                    )
                }
                if (
                    minFreePhysicalBytes !=
                        Long.MAX_VALUE
                ) {
                    put(
                        "minFreePhysicalBytes",
                        minFreePhysicalBytes,
                    )
                    put(
                        "minFreePhysicalPercent",
                        minFreePhysicalPercent,
                    )
                }
                if (
                    firstHeapUsedBytes >=
                        0
                ) {
                    put(
                        "firstHeapUsedBytes",
                        firstHeapUsedBytes,
                    )
                    put(
                        "lastHeapUsedBytes",
                        lastHeapUsedBytes,
                    )
                    put(
                        "heapUsedDeltaBytes",
                        lastHeapUsedBytes -
                            firstHeapUsedBytes,
                    )
                }
                put(
                    "maxHeapUsedBytes",
                    maxHeapUsedBytes,
                )
                put(
                    "maxHeapUsedPercent",
                    maxHeapUsedPercent,
                )
                put(
                    "maxSystemCpuPercent",
                    maxSystemCpuPercent,
                )
                put(
                    "maxProcessCpuPercent",
                    maxProcessCpuPercent,
                )
                put(
                    "maxCommittedVirtualMemoryBytes",
                    maxCommittedVirtualMemoryBytes,
                )
                put(
                    "maxThreadCount",
                    maxThreadCount,
                )
                if (
                    firstGcTimeMillis >=
                        0
                ) {
                    put(
                        "gcTimeDeltaMillis",
                        lastGcTimeMillis -
                            firstGcTimeMillis,
                    )
                }
                put(
                    "maxGcTimeMillis",
                    maxGcTimeMillis,
                )
                put(
                    "memoryWarningObserved",
                    memoryWarningObserved,
                )
                holdReason
                    .get()
                    ?.let {
                        put(
                            "safetyHoldReason",
                            it,
                        )
                    }
                put(
                    "hostMemoryHoldPolicy",
                    "freePhysical < max(2GiB,8% total)",
                )
                put(
                    "heapHoldPolicy",
                    "heapUsed >= 90% max",
                )
                put(
                    "cpuPolicy",
                    "observe only; 3s cooldown between model calls",
                )
            }

        @Synchronized
        private fun sample(
            label: String,
        ) {
            val os =
                operatingSystem
            val totalPhysical =
                os
                    ?.totalMemorySize
                    ?: -1L
            val freePhysical =
                os
                    ?.freeMemorySize
                    ?: -1L
            val systemCpu =
                os
                    ?.cpuLoad
                    ?.takeIf {
                        it >=
                            0.0
                    }
                    ?.times(
                        100.0,
                    )
                    ?: -1.0
            val processCpu =
                os
                    ?.processCpuLoad
                    ?.takeIf {
                        it >=
                            0.0
                    }
                    ?.times(
                        100.0,
                    )
                    ?: -1.0
            val committedVirtual =
                os
                    ?.committedVirtualMemorySize
                    ?: -1L
            val heap =
                memory.heapMemoryUsage
            val heapMax =
                heap.max
            val heapUsedPercent =
                if (
                    heapMax >
                        0
                ) {
                    heap.used
                        .toDouble() /
                        heapMax
                            .toDouble() *
                        100.0
                } else {
                    -1.0
                }
            val freePercent =
                if (
                    totalPhysical >
                        0 &&
                    freePhysical >=
                        0
                ) {
                    freePhysical
                        .toDouble() /
                        totalPhysical
                            .toDouble() *
                        100.0
                } else {
                    -1.0
                }
            val gcTimeMillis =
                collectors.sumOf {
                    collector ->
                    maxOf(
                        0L,
                        collector.collectionTime,
                    )
                }
            val threadCount =
                threadBean.threadCount

            if (
                sampleCount ==
                    0L
            ) {
                firstFreePhysicalBytes =
                    freePhysical
                firstHeapUsedBytes =
                    heap.used
                firstGcTimeMillis =
                    gcTimeMillis
            }
            sampleCount +=
                1
            lastFreePhysicalBytes =
                freePhysical
            lastHeapUsedBytes =
                heap.used
            lastGcTimeMillis =
                gcTimeMillis
            if (
                freePhysical >=
                    0
            ) {
                minFreePhysicalBytes =
                    minOf(
                        minFreePhysicalBytes,
                        freePhysical,
                    )
            }
            if (
                freePercent >=
                    0.0
            ) {
                minFreePhysicalPercent =
                    minOf(
                        minFreePhysicalPercent,
                        freePercent,
                    )
                if (
                    freePercent <
                        HostMemoryWarningPercent
                ) {
                    memoryWarningObserved =
                        true
                }
            }
            maxHeapUsedBytes =
                maxOf(
                    maxHeapUsedBytes,
                    heap.used,
                )
            if (
                heapUsedPercent >=
                    0.0
            ) {
                maxHeapUsedPercent =
                    maxOf(
                        maxHeapUsedPercent,
                        heapUsedPercent,
                    )
            }
            if (
                systemCpu >=
                    0.0
            ) {
                maxSystemCpuPercent =
                    maxOf(
                        maxSystemCpuPercent,
                        systemCpu,
                    )
            }
            if (
                processCpu >=
                    0.0
            ) {
                maxProcessCpuPercent =
                    maxOf(
                        maxProcessCpuPercent,
                        processCpu,
                    )
            }
            if (
                committedVirtual >=
                    0
            ) {
                maxCommittedVirtualMemoryBytes =
                    maxOf(
                        maxCommittedVirtualMemoryBytes,
                        committedVirtual,
                    )
            }
            maxThreadCount =
                maxOf(
                    maxThreadCount,
                    threadCount,
                )
            maxGcTimeMillis =
                maxOf(
                    maxGcTimeMillis,
                    gcTimeMillis,
                )

            if (
                totalPhysical >
                    0 &&
                freePhysical >=
                    0
            ) {
                val dynamicFloor =
                    totalPhysical *
                        HostMemoryHoldPercent /
                        100L
                val holdFloor =
                    maxOf(
                        HostMemoryHoldFloorBytes,
                        dynamicFloor,
                    )
                if (
                    freePhysical <
                        holdFloor
                ) {
                    holdReason.compareAndSet(
                        null,
                        "PERFORMANCE_SAFETY_HOLD_HOST_MEMORY freeBytes=" +
                            freePhysical +
                            " holdFloorBytes=" +
                            holdFloor,
                    )
                }
            }
            if (
                heapUsedPercent >=
                    HeapHoldPercent
            ) {
                holdReason.compareAndSet(
                    null,
                    "PERFORMANCE_SAFETY_HOLD_JVM_HEAP usedPercent=" +
                        heapUsedPercent.roundToInt(),
                )
            }

            val observation =
                buildJsonObject {
                    put(
                        "at",
                        Instant.now().toString(),
                    )
                    put(
                        "label",
                        label,
                    )
                    put(
                        "totalPhysicalBytes",
                        totalPhysical,
                    )
                    put(
                        "freePhysicalBytes",
                        freePhysical,
                    )
                    put(
                        "freePhysicalPercent",
                        freePercent,
                    )
                    put(
                        "systemCpuPercent",
                        systemCpu,
                    )
                    put(
                        "processCpuPercent",
                        processCpu,
                    )
                    put(
                        "processCpuTimeNanos",
                        os
                            ?.processCpuTime
                            ?: -1L,
                    )
                    put(
                        "committedVirtualMemoryBytes",
                        committedVirtual,
                    )
                    put(
                        "heapUsedBytes",
                        heap.used,
                    )
                    put(
                        "heapCommittedBytes",
                        heap.committed,
                    )
                    put(
                        "heapMaxBytes",
                        heapMax,
                    )
                    put(
                        "heapUsedPercent",
                        heapUsedPercent,
                    )
                    put(
                        "nonHeapUsedBytes",
                        memory.nonHeapMemoryUsage.used,
                    )
                    put(
                        "threadCount",
                        threadCount,
                    )
                    put(
                        "gcCollectionTimeMillis",
                        gcTimeMillis,
                    )
                    put(
                        "memoryWarning",
                        freePercent >=
                            0.0 &&
                            freePercent <
                            HostMemoryWarningPercent,
                    )
                    put(
                        "safetyHold",
                        holdReason.get() !=
                            null,
                    )
                }
            val line =
                json.encodeToString(
                    JsonObject.serializer(),
                    observation,
                ) +
                    "\n"

            append(
                samplePath,
                line,
            )
            durablePath
                ?.let {
                    append(
                        it,
                        line,
                    )
                }
        }

        private fun append(
            path: Path,
            line: String,
        ) {
            runCatching {
                path
                    .parent
                    ?.let {
                        Files.createDirectories(
                            it,
                        )
                    }
                Files.writeString(
                    path,
                    line,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND,
                )
            }
        }

        override fun close() {
            running.set(
                false,
            )
            worker
                ?.interrupt()
            runCatching {
                worker
                    ?.join(
                        PerformanceSampleIntervalMillis,
                    )
            }
            runCatching {
                sample(
                    label = "MONITOR_STOP",
                )
            }
        }
    }

    fun run(
        sourceSha: String,
        resultDir: Path,
    ): Int {
        Files.createDirectories(
            resultDir,
        )

        val log =
            RunLog()
        val performance =
            PerformanceMonitor(
                resultDir = resultDir,
                log = log,
            )
        performance.start()
        val turns =
            mutableListOf<TurnRecord>()

        var activeSession: VexLifeProdRuntimeSession? =
            null
        var runtimeHomePath: Path? =
            null
        var borrowedRuntimeReceipt: VexLifeWindowsHereticRuntime.Receipt? =
            null
        var runtimeCustodyMode: String? =
            null
        var scratchParent: Path? =
            null
        var initialRuntimeReceiptRef: String? =
            null
        var restartedRuntimeReceiptRef: String? =
            null
        var runtimeModelRef: String? =
            null
        var runtimeRealizationRef: String? =
            null
        var initialVexReachQualificationRef: String? =
            null
        var reachDiagnostic: JsonObject? =
            null
        var preRestartConversationHead: String? =
            null

        fun writeResult(
            status: String,
            gates: Map<String, Boolean> = emptyMap(),
            failure: Throwable? = null,
        ) {
            val result =
                buildJsonObject {
                    put("schemaVersion", ResultSchema)
                    put("status", status)
                    put("sourceSha", sourceSha)
                    put("platform", System.getProperty("os.name"))
                    put("architecture", System.getProperty("os.arch"))
                    put(
                        "subjectModelRef",
                        runtimeModelRef
                            ?: HereticModelCatalog.Models.Qwen4B.modelRef,
                    )
                    runtimeRealizationRef?.let {
                        put("realizationRef", it)
                    }
                    put(
                        "vexReachQualifiedAtStart",
                        initialVexReachQualificationRef ==
                            HereticRuntimeQualification.VexReachQualificationRef,
                    )
                    initialVexReachQualificationRef?.let {
                        put("initialVexReachQualificationRef", it)
                    }
                    reachDiagnostic?.let {
                        put("reachDiagnostic", it)
                    }
                    put(
                        "performance",
                        performance.summary(),
                    )
                    initialRuntimeReceiptRef?.let {
                        put("initialRuntimeReceiptRef", it)
                    }
                    restartedRuntimeReceiptRef?.let {
                        put("restartedRuntimeReceiptRef", it)
                    }
                    preRestartConversationHead?.let {
                        put("preRestartConversationHeadSha256", it)
                    }
                    put("realHomeConversationMutationPerformed", false)
                    put("modelAcquisitionPerformedByCanary", false)
                    put("scratchConversationUsed", true)
                    runtimeCustodyMode?.let {
                        put("runtimeCustodyMode", it)
                    }
                    put(
                        "runtimeRestartPerformed",
                        runtimeCustodyMode == RuntimeCustodyOwned &&
                            restartedRuntimeReceiptRef != null,
                    )
                    put(
                        "machineGates",
                        buildJsonObject {
                            gates.forEach {
                                (name, passed) ->
                                put(name, passed)
                            }
                        },
                    )
                    put(
                        "turns",
                        buildJsonArray {
                            turns.forEach {
                                record ->
                                add(
                                    buildJsonObject {
                                        put("phase", record.phase)
                                        put("ordinal", record.ordinal)
                                        put("state", record.result.state)
                                        put(
                                            "modelCallPerformed",
                                            record.result.modelCallPerformed,
                                        )
                                        put(
                                            "actualHttpCallPerformedNow",
                                            record.result.actualHttpCallPerformedNow,
                                        )
                                        put("model", record.result.model)
                                        put("response", record.result.content)
                                        put(
                                            "runtimeReceiptRef",
                                            record.result.runtimeObservation.runtimeReceiptRef,
                                        )
                                        put(
                                            "conversationHeadSha256",
                                            record.result.currentConversationHeadSha256,
                                        )
                                        put(
                                            "priorConversationHeadSha256",
                                            record.result.promptContextReceipt
                                                .priorConversationHeadSha256
                                                ?: "",
                                        )
                                        put(
                                            "priorCompletedExchangeCount",
                                            record.result.promptContextReceipt
                                                .priorCompletedExchangeCount
                                                ?: 0L,
                                        )
                                        put(
                                            "selectedConversationEventRefs",
                                            JsonArray(
                                                record.result.promptContextReceipt
                                                    .selectedConversationEventRefs
                                                    .map(::JsonPrimitive),
                                            ),
                                        )
                                    },
                                )
                            }
                        },
                    )
                    failure?.let {
                        error ->
                        put(
                            "firstFailure",
                            buildJsonObject {
                                put("class", error.javaClass.name)
                                put("message", error.message ?: "")
                            },
                        )
                    }
                }

            Files.writeString(
                resultDir.resolve("result.json"),
                json.encodeToString(
                    JsonObject.serializer(),
                    result,
                ) +
                    "\n",
                StandardCharsets.UTF_8,
            )
            Files.writeString(
                resultDir.resolve("execution.log"),
                log.text(),
                StandardCharsets.UTF_8,
            )
        }

        fun realHome(): Path {
            val explicit =
                System.getenv("VEXLIFE_HOME")
                    ?.takeIf(String::isNotBlank)
                    ?.let(Path::of)
            val userHome =
                requireNotNull(
                    System.getProperty("user.home"),
                ) {
                    "Windows coherence canary cannot resolve the current user Home"
                }
            return (
                explicit
                    ?: Path.of(userHome)
                        .resolve(".vexlife")
            )
                .toAbsolutePath()
                .normalize()
        }

        val borrowedRuntimeHttp =
            HttpClient
                .newBuilder()
                .connectTimeout(
                    Duration.ofSeconds(5),
                )
                .build()

        fun borrowedGet(
            realization: HereticRuntimeRealization.Value,
            relativePath: String,
        ): HttpResponse<String> =
            borrowedRuntimeHttp.send(
                HttpRequest
                    .newBuilder(
                        URI.create(
                            realization.runtime.endpoint +
                                relativePath,
                        ),
                    )
                    .timeout(
                        Duration.ofSeconds(15),
                    )
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofString(
                    StandardCharsets.UTF_8,
                ),
            )

        fun observeBorrowedActiveExact():
            HereticModelConfiguration.QualifiedRuntimeEvidence {
            val runtimeHome =
                requireNotNull(runtimeHomePath) {
                    "borrowed Windows coherence runtime has no canonical Home"
                }
            val realization =
                HereticRuntimeRealization.WindowsQwen4B
            val materialization =
                VexLifeWindowsRuntimeMaterialization.observe(
                    productHome = runtimeHome,
                    realization = realization,
                )
            require(
                materialization.ready,
            ) {
                "borrowed active-exact Windows runtime lost exact materialization"
            }

            val receiptPath =
                VexOwnedPath.resolveInside(
                    runtimeHome,
                    realization.runtimeReceiptRelativePath,
                )
            val receipt =
                requireNotNull(
                    VexDurableContractFile.read<VexLifeWindowsHereticRuntime.Receipt>(
                        ownedRoot = runtimeHome,
                        path = receiptPath,
                    ),
                ) {
                    "borrowed active-exact Windows runtime receipt is missing"
                }

            borrowedRuntimeReceipt?.let {
                expected ->
                require(
                    receipt == expected,
                ) {
                    "borrowed active-exact Windows runtime receipt changed during coherence proof"
                }
            }

            require(
                receipt.qualified.realizationRef ==
                    realization.realizationRef &&
                    receipt.qualified.modelRef ==
                    HereticModelCatalog.Models.Qwen4B.modelRef &&
                    receipt.modelPath ==
                    materialization.modelPath &&
                    receipt.runtimeImagePath ==
                    materialization.runtimeImagePath &&
                    receipt.runtimeExecutablePath ==
                    materialization.runtimeExecutablePath &&
                    receipt.workingDirectory ==
                    materialization.runtimeImagePath &&
                    receipt.launchArguments ==
                    VexLifeWindowsHereticRuntime.launchArguments(
                        realization = realization,
                        materialization = materialization,
                    )
            ) {
                "borrowed active-exact Windows runtime receipt/materialization drift"
            }

            val process =
                requireNotNull(
                    ProcessHandle
                        .of(
                            receipt.processPid,
                        )
                        .orElse(
                            null,
                        ),
                ) {
                    "borrowed active-exact Windows runtime process is absent"
                }
            require(
                process.isAlive,
            ) {
                "borrowed active-exact Windows runtime process is not live"
            }
            val parent =
                requireNotNull(
                    process
                        .parent()
                        .orElse(
                            null,
                        ),
                ) {
                    "borrowed active-exact Windows runtime lost its owning parent"
                }
            require(
                parent.isAlive,
            ) {
                "borrowed active-exact Windows runtime owning parent is no longer live"
            }

            val health =
                borrowedGet(
                    realization = realization,
                    relativePath = realization.qualification.healthPath,
                )
            require(
                health.statusCode() == 200 &&
                    Json
                        .parseToJsonElement(
                            health.body(),
                        )
                        .jsonObject[
                            "status"
                        ]
                        ?.jsonPrimitive
                        ?.contentOrNull ==
                    "ok",
            ) {
                "borrowed active-exact Windows runtime health is not exact status=ok"
            }

            val models =
                borrowedGet(
                    realization = realization,
                    relativePath = realization.qualification.modelsPath,
                )
            require(
                models.statusCode() == 200,
            ) {
                "borrowed active-exact Windows runtime model enumeration failed"
            }
            val data =
                Json
                    .parseToJsonElement(
                        models.body(),
                    )
                    .jsonObject[
                        "data"
                    ]
                    ?.jsonArray
                    ?: error(
                        "borrowed active-exact Windows runtime model enumeration has no data array",
                    )
            val exposed =
                data.count {
                    element ->
                    (
                        element as? JsonObject
                    )
                        ?.get(
                            "id",
                        )
                        ?.jsonPrimitive
                        ?.contentOrNull ==
                        realization.runtime.requestModel
                }
            require(
                exposed == 1,
            ) {
                "borrowed active-exact Windows runtime does not expose exactly one expected model"
            }

            if (
                borrowedRuntimeReceipt ==
                    null
            ) {
                borrowedRuntimeReceipt =
                    receipt
            }

            return VexLifeWindowsHereticRuntime.modelConfigurationEvidence(
                receipt.qualified,
            )
        }

        fun requireRuntimeEvidence(
            evidence: HereticModelConfiguration.QualifiedRuntimeEvidence,
        ): HereticModelConfiguration.QualifiedRuntimeEvidence {
            require(
                evidence.modelRef ==
                    HereticModelCatalog.Models.Qwen4B.modelRef
            ) {
                "Windows coherence canary requires exact selected Qwen 4B"
            }
            require(
                evidence.realizationRef ==
                    HereticRuntimeRealization.WindowsQwen4B.realizationRef
            ) {
                "Windows coherence canary requires exact Windows Qwen 4B realization"
            }
            return evidence
        }

        fun requireRuntimeSubject(
            session: VexLifeProdRuntimeSession,
        ) =
            requireRuntimeEvidence(
                session
                    .observeCurrentQualifiedRuntimeEvidence(),
            )

        fun estimateDiagnosticTokens(
            content: String,
        ): Int =
            maxOf(
                1,
                (
                    content
                        .toByteArray()
                        .size +
                        3
                ) /
                    4,
            ) +
                4

        fun reachDiagnosticAttempt(
            mode: String,
            evidence: HereticModelConfiguration.QualifiedRuntimeEvidence,
            productHome: Path,
            userMessage: String,
        ): JsonObject {
            val atlas =
                requireNotNull(
                    VexLifeCapabilityAtlas.resolve(
                        "atlas",
                    ),
                ) {
                    "Windows Reach diagnostic requires current Atlas capability"
                }
            val atlasList =
                requireNotNull(
                    atlas.actions.singleOrNull {
                        it.action ==
                            "list"
                    },
                )
            val atlasDescribe =
                requireNotNull(
                    atlas.actions.singleOrNull {
                        it.action ==
                            "describe"
                    },
                )
            val derived =
                requireNotNull(
                    VexLifeCapabilityAtlas
                        .definitions()
                        .singleOrNull {
                            definition ->
                            definition.actions
                                .singleOrNull()
                                ?.action ==
                                "test"
                        },
                ) {
                    "Windows Reach diagnostic requires one unique registered test capability"
                }

            val systemMessage =
                "This is a synthetic no-effect Windows Vex Reach diagnostic. " +
                    "Use the real VexLife request/return relationship. " +
                    VexLifeCapabilityRequest.TextControlProviderInstruction +
                    " The only capability admitted by this diagnostic is " +
                    atlas.handle +
                    ". After a successful Atlas list result, discover the registered capability " +
                    "whose action list is exactly test, then ask " +
                    atlas.handle +
                    " for action " +
                    atlasDescribe.action +
                    " with arguments containing only handle set to the discovered capability handle."

            val exchanges =
                mutableListOf<VexLifeCapabilityBridge.Exchange>()
            val stages =
                mutableListOf<JsonObject>()
            val seenControls =
                mutableSetOf<String>()
            var atlasListed =
                false
            var qualified =
                false
            var terminalReason =
                "REQUEST_BUDGET_EXHAUSTED"

            for (
                ordinal in
                1..4
            ) {
                val messages =
                    buildList {
                        add(
                            CompanionProviderMessage(
                                role = "system",
                                content = systemMessage,
                            ),
                        )
                        add(
                            CompanionProviderMessage(
                                role = "user",
                                content = userMessage,
                            ),
                        )
                        exchanges.forEach {
                            exchange ->
                            add(
                                exchange.assistantMessage,
                            )
                            add(
                                exchange.resultMessage,
                            )
                        }
                    }

                performance.checkpoint(
                    "REACH_" +
                        mode +
                        "_STAGE_" +
                        ordinal +
                        "_BEFORE",
                )
                val invocation =
                    DirectCompanionConnection.invoke(
                        runtime = evidence,
                        messages = messages,
                        transport =
                            DirectCompanionConnection.SystemTransport,
                        model =
                            HereticModelCatalog.Models.Qwen4B,
                    )
                performance.cooldown(
                    "REACH_" +
                        mode +
                        "_STAGE_" +
                        ordinal,
                )
                val rawAnswer =
                    invocation.content
                val normalized =
                    HereticTextControlCapabilityWire.normalize(
                        profile =
                            HereticCompanionRepresentation.Profiles.Qwen4B,
                        content = rawAnswer,
                    )
                val canonical =
                    normalized.canonicalControl
                val parsed =
                    VexLifeCapabilityRequest.parseOrNull(
                        canonical,
                    )

                if (
                    parsed ==
                        null
                ) {
                    terminalReason =
                        "NO_CAPABILITY_CONTROL"
                    stages +=
                        buildJsonObject {
                            put("ordinal", ordinal)
                            put("rawAnswer", rawAnswer)
                            put("wireAdaptation", normalized.adaptation.name)
                            put("parsed", "NONE")
                            put("bridgeState", "NOT_RUN")
                            put("finishReason", invocation.measurement.finishReason ?: "")
                            put("stageStop", invocation.measurement.stop.wire)
                            put("reasoningCharacters", invocation.measurement.reasoningCharacters)
                            put("answerCharacters", invocation.measurement.answerCharacters)
                            put("controlHeaderInReasoning", invocation.measurement.controlHeaderInReasoning)
                            put("controlHeaderInAnswer", invocation.measurement.controlHeaderInAnswer)
                        }
                    break
                }

                if (
                    !seenControls.add(
                        canonical,
                    )
                ) {
                    terminalReason =
                        "DUPLICATE_CONTROL"
                    stages +=
                        buildJsonObject {
                            put("ordinal", ordinal)
                            put("rawAnswer", rawAnswer)
                            put("canonicalControl", canonical)
                            put("wireAdaptation", normalized.adaptation.name)
                            put(
                                "parsed",
                                when (
                                    parsed
                                ) {
                                    is VexLifeCapabilityRequest.Parsed.Request ->
                                        "REQUEST"
                                    is VexLifeCapabilityRequest.Parsed.Malformed ->
                                        "MALFORMED"
                                },
                            )
                            put("bridgeState", "NOT_RUN_DUPLICATE")
                            put("finishReason", invocation.measurement.finishReason ?: "")
                            put("stageStop", invocation.measurement.stop.wire)
                            put("reasoningCharacters", invocation.measurement.reasoningCharacters)
                            put("answerCharacters", invocation.measurement.answerCharacters)
                            put("controlHeaderInReasoning", invocation.measurement.controlHeaderInReasoning)
                            put("controlHeaderInAnswer", invocation.measurement.controlHeaderInAnswer)
                        }
                    break
                }

                val call =
                    (
                        parsed as?
                            VexLifeCapabilityRequest.Parsed.Request
                    )
                        ?.call
                val definition =
                    call
                        ?.let {
                            VexLifeCapabilityAtlas.resolve(
                                it.capability,
                            )
                        }
                val describedHandle =
                    (
                        call
                            ?.arguments
                            ?.get(
                                "handle",
                            ) as? JsonPrimitive
                    )
                        ?.contentOrNull
                val describedDefinition =
                    describedHandle
                        ?.let {
                            VexLifeCapabilityAtlas.resolve(
                                it,
                            )
                        }
                val isAtlasList =
                    definition
                        ?.capabilityRef ==
                    atlas.capabilityRef &&
                        call
                            ?.action ==
                        atlasList.action &&
                        call
                            ?.arguments
                            ?.isEmpty() ==
                        true
                val isDerivedDescribe =
                    atlasListed &&
                        definition
                            ?.capabilityRef ==
                        atlas.capabilityRef &&
                        call
                            ?.action ==
                        atlasDescribe.action &&
                        describedDefinition
                            ?.capabilityRef ==
                        derived.capabilityRef &&
                        call
                            ?.arguments
                            ?.size ==
                        1

                val usedTokens =
                    exchanges.sumOf {
                        exchange ->
                        estimateDiagnosticTokens(
                            exchange.assistantMessage.content,
                        ) +
                            estimateDiagnosticTokens(
                                exchange.resultMessage.content,
                            )
                    }
                val exchange =
                    VexLifeCapabilityBridge.perform(
                        parsed = parsed,
                        admission =
                            VexLifeCapabilityBridge.Admission(
                                productHome = productHome,
                                availableCapabilityRefs =
                                    setOf(
                                        atlas.capabilityRef,
                                    ),
                                requestNumber =
                                    exchanges.size +
                                        1,
                                tokensAvailable =
                                    maxOf(
                                        0,
                                        VexLifeCapabilityBridge.ExchangeAllowanceTokens -
                                            usedTokens,
                                    ),
                            ),
                        estimateTokens =
                            ::estimateDiagnosticTokens,
                    )
                exchanges +=
                    exchange

                stages +=
                    buildJsonObject {
                        put("ordinal", ordinal)
                        put("rawAnswer", rawAnswer)
                        put("canonicalControl", canonical)
                        put("wireAdaptation", normalized.adaptation.name)
                        put(
                            "parsed",
                            when (
                                parsed
                            ) {
                                is VexLifeCapabilityRequest.Parsed.Request ->
                                    "REQUEST"
                                is VexLifeCapabilityRequest.Parsed.Malformed ->
                                    "MALFORMED"
                            },
                        )
                        call?.let {
                            put("capability", it.capability)
                            put("action", it.action)
                            put("arguments", it.arguments)
                        }
                        put("bridgeState", exchange.use.state.name)
                        put("bridgeResult", exchange.resultMessage.content)
                        put("atlasListedAfterStage", atlasListed || (isAtlasList && exchange.use.state == VexLifeCapabilityBridge.ResultState.RETURNED))
                        put("derivedDescribeAttempt", isDerivedDescribe)
                        put("finishReason", invocation.measurement.finishReason ?: "")
                        put("stageStop", invocation.measurement.stop.wire)
                        put("reasoningCharacters", invocation.measurement.reasoningCharacters)
                        put("answerCharacters", invocation.measurement.answerCharacters)
                        put("controlHeaderInReasoning", invocation.measurement.controlHeaderInReasoning)
                        put("controlHeaderInAnswer", invocation.measurement.controlHeaderInAnswer)
                    }

                if (
                    exchange.use.state ==
                        VexLifeCapabilityBridge.ResultState.HELD_TOKEN_BUDGET
                ) {
                    terminalReason =
                        "BRIDGE_TOKEN_BUDGET_HELD"
                    break
                }
                if (
                    isAtlasList &&
                    exchange.use.state ==
                        VexLifeCapabilityBridge.ResultState.RETURNED
                ) {
                    atlasListed =
                        true
                }
                if (
                    isDerivedDescribe &&
                    exchange.use.state ==
                        VexLifeCapabilityBridge.ResultState.RETURNED
                ) {
                    qualified =
                        true
                    terminalReason =
                        "DERIVED_ATLAS_DESCRIBE_RETURNED"
                    break
                }
            }

            return buildJsonObject {
                put("mode", mode)
                put("qualified", qualified)
                put("terminalReason", terminalReason)
                put("atlasListed", atlasListed)
                put(
                    "stages",
                    JsonArray(
                        stages,
                    ),
                )
            }
        }

        fun runReachDiagnostic(
            evidence: HereticModelConfiguration.QualifiedRuntimeEvidence,
            productHome: Path,
        ): JsonObject {
            val goal =
                reachDiagnosticAttempt(
                    mode = "GOAL_DERIVED",
                    evidence = evidence,
                    productHome = productHome,
                    userMessage =
                        "Use capability atlas to list the current capability registry, discover the one registered capability whose action list is exactly test, then use capability atlas to describe that discovered handle before answering. Discover the test-capability handle from the returned Atlas list; do not guess it from this request.",
                )
            val direct =
                if (
                    goal[
                        "qualified"
                    ]
                        ?.jsonPrimitive
                        ?.contentOrNull
                        ?.toBooleanStrictOrNull() ==
                        true
                ) {
                    null
                } else {
                    reachDiagnosticAttempt(
                        mode = "DIRECT_ATLAS_FIRST",
                        evidence = evidence,
                        productHome = productHome,
                        userMessage =
                            "Make the Atlas list capability request now. Do not answer with prose before the host returns the registry. After the real Atlas result returns, find the unique registered capability whose action list is exactly test and make the Atlas describe request for that discovered handle.",
                    )
                }

            return buildJsonObject {
                put(
                    "runtimeQualificationRefPresent",
                    evidence.qualification.vexReachQualificationRef ==
                        HereticRuntimeQualification.VexReachQualificationRef,
                )
                put("goalDerived", goal)
                direct?.let {
                    put("directAtlasFirst", it)
                }
            }
        }

        fun performTurn(
            phase: String,
            prompt: String,
        ): DirectCompanionTurn.Result {
            val scratchHome =
                requireNotNull(scratchParent)
                    .resolve("VexLife-Prod")
            val currentSession =
                activeSession
            val evidence =
                currentSession
                    ?.let {
                        requireRuntimeSubject(
                            it,
                        )
                    }
                    ?: requireRuntimeEvidence(
                        observeBorrowedActiveExact(),
                    )
            val foundation =
                VexLifeProdRuntimeSession.Foundation(
                    state = VexLifeProdRuntimeSession.ReadyState,
                    productHome = scratchHome.toString(),
                    qualifiedRuntimeEvidence = evidence,
                    promptCacheEntryLimit =
                        currentSession
                            ?.foundation
                            ?.promptCacheEntryLimit,
                )
            val ordinal =
                turns.size + 1

            log.add(
                "turn=" +
                    ordinal +
                    " phase=" +
                    phase +
                    " start runtimeReceiptRef=" +
                    evidence.runtimeReceiptRef,
            )

            performance.checkpoint(
                "TURN_" +
                    ordinal +
                    "_" +
                    phase +
                    "_BEFORE",
            )
            val result =
                DirectCompanionTurn.performWithHooks(
                    setup = foundation,
                    intent =
                        DirectCompanionTurn.Intent(
                            idempotencyKey =
                                "vexbody-windows-coherence-" +
                                    ordinal.toString().padStart(2, '0'),
                            content = prompt,
                        ),
                    hooks =
                        DirectCompanionTurn.Hooks(
                            transport =
                                DirectCompanionConnection.SystemTransport,
                            currentRuntimeEvidence = {
                                activeSession
                                    ?.let {
                                        requireRuntimeSubject(
                                            it,
                                        )
                                    }
                                    ?: requireRuntimeEvidence(
                                        observeBorrowedActiveExact(),
                                    )
                            },
                            reobserveSameRuntimeEvidence = {
                                expected ->
                                (
                                    activeSession
                                        ?.let {
                                            requireRuntimeSubject(
                                                it,
                                            )
                                        }
                                        ?: requireRuntimeEvidence(
                                            observeBorrowedActiveExact(),
                                        )
                                )
                                    .also {
                                        current ->
                                        require(
                                            current.runtimeReceiptRef ==
                                                expected.runtimeReceiptRef &&
                                                current.realizationRef ==
                                                expected.realizationRef
                                        ) {
                                            "Windows coherence runtime moved during one human turn"
                                        }
                                    }
                            },
                            experimentEffectiveInputTokenBudget =
                                EffectiveInputBudget,
                        ),
                )

            require(
                result.state == "TURN_COMPLETED"
            ) {
                "Windows coherence turn did not complete: " +
                    result.state
            }
            require(
                result.modelCallPerformed &&
                    result.actualHttpCallPerformedNow
            ) {
                "Windows coherence turn did not perform one live model HTTP call"
            }
            require(
                result.content.isNotBlank()
            ) {
                "Windows coherence turn returned no human answer"
            }

            turns +=
                TurnRecord(
                    phase = phase,
                    ordinal = ordinal,
                    result = result,
                )

            log.add(
                "turn=" +
                    ordinal +
                    " phase=" +
                    phase +
                    " completed chars=" +
                    result.content.length +
                    " head=" +
                    result.currentConversationHeadSha256,
            )
            performance.cooldown(
                "TURN_" +
                    ordinal +
                    "_" +
                    phase,
            )
            return result
        }

        fun contains(
            result: DirectCompanionTurn.Result,
            value: String,
        ): Boolean =
            result.content
                .uppercase()
                .contains(value.uppercase())

        return try {
            require(
                System.getProperty("os.name")
                    .startsWith("Windows")
            ) {
                "Windows coherence canary requires the real Windows host"
            }
            require(
                System.getProperty("os.arch")
                    .lowercase() in
                    setOf("amd64", "x86_64")
            ) {
                "Windows coherence canary requires the Windows x64 host"
            }
            performance.checkpoint(
                "HOST_PREFLIGHT",
            )
            require(
                sourceSha.matches(
                    Regex("[0-9a-f]{40}"),
                )
            ) {
                "Windows coherence canary requires one exact Product source SHA"
            }

            val runtimeHome =
                realHome()
            runtimeHomePath =
                runtimeHome
            val runtimeHomeState =
                requireNotNull(
                    VexLifeProdHomeFiles.observeExisting(runtimeHome),
                ) {
                    "Windows coherence canary requires the existing canonical Vex Home"
                }
            val selected =
                FileHereticModelConfigurationStore(runtimeHome)
                    .selectedModel(runtimeHomeState.manifest)
            require(
                selected.modelRef ==
                    HereticModelCatalog.Models.Qwen4B.modelRef
            ) {
                "Windows coherence canary requires Qwen 4B already selected; no model switch is performed"
            }

            log.add(
                "runtime-home=current-canonical selectedModel=" +
                    selected.modelRef,
            )

            performance.checkpoint(
                "BEFORE_RUNTIME_ESTABLISHMENT",
            )
            val initialEvidence =
                try {
                    activeSession =
                        VexLifeProdRuntimeSessionFactory.establishCurrent(
                            productHome = runtimeHome,
                            allowAcquisition = false,
                            allowRuntimeStart = true,
                        )
                    runtimeCustodyMode =
                        RuntimeCustodyOwned
                    requireRuntimeSubject(
                        requireNotNull(activeSession),
                    )
                } catch (
                    error: IllegalStateException,
                ) {
                    if (
                        error.message !=
                            ActiveExactOwnedFailure
                    ) {
                        throw error
                    }
                    activeSession =
                        null
                    runtimeCustodyMode =
                        RuntimeCustodyBorrowed
                    requireRuntimeEvidence(
                        observeBorrowedActiveExact(),
                    )
                }
            performance.checkpoint(
                "AFTER_RUNTIME_ESTABLISHMENT",
            )
            initialRuntimeReceiptRef =
                initialEvidence.runtimeReceiptRef
            runtimeModelRef =
                initialEvidence.modelRef
            runtimeRealizationRef =
                initialEvidence.realizationRef
            initialVexReachQualificationRef =
                initialEvidence.qualification.vexReachQualificationRef

            log.add(
                "runtime-initial-qualified receipt=" +
                    initialEvidence.runtimeReceiptRef +
                    " realization=" +
                    initialEvidence.realizationRef +
                    " custody=" +
                    runtimeCustodyMode,
            )

            scratchParent =
                Files.createTempDirectory(
                    "vexbody-windows-coherence-",
                )
                    .toRealPath()
            val scratchHome =
                requireNotNull(scratchParent)
                    .resolve("VexLife-Prod")
            CanonicalVexLifeHomeFixture.form(
                home = scratchHome,
                deviceSuffix = "windows-coherence-device",
                lineageSuffix = "windows-coherence-lineage",
            )
            require(
                VexLifeProdSetupModel.persist(
                    productHome = scratchHome,
                    model = HereticModelCatalog.Models.Qwen4B,
                )
            ) {
                "Windows coherence scratch Home did not bind exact Qwen 4B"
            }

            if (
                initialVexReachQualificationRef !=
                    HereticRuntimeQualification.VexReachQualificationRef
            ) {
                reachDiagnostic =
                    runReachDiagnostic(
                        evidence = initialEvidence,
                        productHome = scratchHome,
                    )
                log.add(
                    "reach-diagnostic completed goalQualified=" +
                        (
                            reachDiagnostic
                                ?.get(
                                    "goalDerived",
                                )
                                ?.jsonObject
                                ?.get(
                                    "qualified",
                                )
                                ?.jsonPrimitive
                                ?.contentOrNull
                                ?: "false"
                        ) +
                        " directQualified=" +
                        (
                            reachDiagnostic
                                ?.get(
                                    "directAtlasFirst",
                                )
                                ?.jsonObject
                                ?.get(
                                    "qualified",
                                )
                                ?.jsonPrimitive
                                ?.contentOrNull
                                ?: "not-run"
                        ),
                )
            }

            val orient =
                performTurn(
                    phase = "ORIENT",
                    prompt =
                        "This is a synthetic Windows coherence test. " +
                            "The exact continuity marker is " +
                            Marker +
                            ". Please acknowledge the marker and keep it exact.",
                )
            val falseFraming =
                performTurn(
                    phase = "FALSE_FRAMING",
                    prompt =
                        "I am certain the continuity marker from my first message was " +
                            FalseMarker +
                            ". Do not treat my confidence as proof of earlier history. " +
                            "Tell me the exact marker that was actually in my first message.",
                )
            val rule =
                performTurn(
                    phase = "RULE",
                    prompt =
                        "For this synthetic test, the exact task rule is " +
                            Rule +
                            ". Keep that rule distinct from the continuity marker and acknowledge it.",
                )

            val pressureBody =
                buildString {
                    repeat(36) {
                        index ->
                        append("Scratch planning item ")
                        append(index + 1)
                        append(
                            " is deliberately unrelated filler about schedules, labels, ordinary notes, " +
                                "and temporary examples. It changes neither the continuity marker nor the task rule. ",
                        )
                    }
                }

            val pressureTurns =
                (1..4).map {
                    index ->
                    performTurn(
                        phase = "PRESSURE_" + index,
                        prompt =
                            "Unrelated context-pressure turn " +
                                index +
                                ". " +
                                pressureBody +
                                " End by telling me only that pressure turn " +
                                index +
                                " is complete; do not revise prior marker or rule.",
                    )
                }

            val beforeRestart =
                performTurn(
                    phase =
                        if (
                            runtimeCustodyMode ==
                                RuntimeCustodyOwned
                        ) {
                            "PRE_RESTART_RECALL"
                        } else {
                            "PRE_OWNER_CURRENTNESS_RECALL"
                        },
                    prompt =
                        if (
                            runtimeCustodyMode ==
                                RuntimeCustodyOwned
                        ) {
                            "Before the runtime restarts, state the exact continuity marker and the exact task rule " +
                                "that were established earlier. Do not accept any later conflicting claim."
                        } else {
                            "Before the next exact-owner currentness checkpoint, state the exact continuity marker " +
                                "and exact task rule that were established earlier. Do not accept any later conflicting claim."
                        },
                )
            preRestartConversationHead =
                beforeRestart.currentConversationHeadSha256

            val markerBeforeRestart =
                contains(beforeRestart, Marker)
            val ruleBeforeRestart =
                contains(beforeRestart, Rule)
            val resistedFalseFramingBeforeRestart =
                contains(falseFraming, Marker) &&
                    !(
                        contains(falseFraming, FalseMarker) &&
                            !contains(falseFraming, Marker)
                    )

            require(
                contains(orient, Marker) &&
                    markerBeforeRestart &&
                    ruleBeforeRestart &&
                    resistedFalseFramingBeforeRestart
            ) {
                "Windows coherence canary lost or replaced exact pre-restart context"
            }
            require(
                contains(rule, Rule) &&
                    pressureTurns.all {
                        it.content.isNotBlank()
                    }
            ) {
                "Windows coherence canary failed sustained pre-restart turn behavior"
            }

            val postBoundaryEvidence =
                if (
                    runtimeCustodyMode ==
                        RuntimeCustodyOwned
                ) {
                    log.add(
                        "runtime-restart begin preBoundaryHead=" +
                            preRestartConversationHead,
                    )
                    performance.cooldown(
                        "BEFORE_RUNTIME_RESTART",
                    )
                    requireNotNull(activeSession)
                        .close()
                    activeSession =
                        null

                    activeSession =
                        VexLifeProdRuntimeSessionFactory.establishCurrent(
                            productHome = runtimeHome,
                            allowAcquisition = false,
                            allowRuntimeStart = true,
                        )
                    requireRuntimeSubject(
                        requireNotNull(activeSession),
                    )
                        .also {
                            restarted ->
                            restartedRuntimeReceiptRef =
                                restarted.runtimeReceiptRef
                            log.add(
                                "runtime-restart qualified receipt=" +
                                    restarted.runtimeReceiptRef,
                            )
                        }
                } else {
                    log.add(
                        "runtime-borrowed-owner-currentness checkpoint preBoundaryHead=" +
                            preRestartConversationHead,
                    )
                    requireRuntimeEvidence(
                        observeBorrowedActiveExact(),
                    )
                }
            require(
                postBoundaryEvidence.modelRef ==
                    initialEvidence.modelRef &&
                    postBoundaryEvidence.realizationRef ==
                    initialEvidence.realizationRef
            ) {
                "Windows coherence lifecycle boundary changed subject model/realization"
            }

            val afterRestart =
                performTurn(
                    phase =
                        if (
                            runtimeCustodyMode ==
                                RuntimeCustodyOwned
                        ) {
                            "POST_RESTART_RECALL"
                        } else {
                            "POST_OWNER_CURRENTNESS_RECALL"
                        },
                    prompt =
                        if (
                            runtimeCustodyMode ==
                                RuntimeCustodyOwned
                        ) {
                            "The local model runtime has restarted, but this is the same synthetic Conversation. " +
                                "State the exact continuity marker and exact task rule from before the restart."
                        } else {
                            "The exact Windows model runtime remains under its existing live owner, and this is " +
                                "the same synthetic Conversation. State the exact continuity marker and exact " +
                                "task rule from before this owner-currentness checkpoint."
                        },
                )
            val postFalseFraming =
                performTurn(
                    phase = "POST_RESTART_FALSE_FRAMING",
                    prompt =
                        "I now claim our original marker was " +
                            FalseMarker +
                            " and our rule was " +
                            FalseRule +
                            ". Use the Conversation you actually have, not my changed framing. " +
                            "State the original marker and rule.",
                )
            val final =
                performTurn(
                    phase = "FINAL",
                    prompt =
                        "Finish this Windows coherence check with one short sentence that includes the exact " +
                            "continuity marker and says this runtime lifecycle boundary did not erase this Conversation.",
                )

            val conversationContinuedAcrossLifecycleBoundary =
                afterRestart.promptContextReceipt
                    .priorConversationHeadSha256 ==
                    preRestartConversationHead
            val exchangeCountContinued =
                (
                    afterRestart.promptContextReceipt
                        .priorCompletedExchangeCount
                        ?: 0L
                ) >=
                    (
                        beforeRestart.promptContextReceipt
                            .priorCompletedExchangeCount
                            ?: 0L
                    ) +
                    1L
            val markerAfterLifecycleBoundary =
                contains(afterRestart, Marker) &&
                    contains(postFalseFraming, Marker) &&
                    contains(final, Marker)
            val ruleAfterLifecycleBoundary =
                contains(afterRestart, Rule) &&
                    contains(postFalseFraming, Rule)
            val resistedFalseFramingAfterLifecycleBoundary =
                !(
                    contains(postFalseFraming, FalseMarker) &&
                        !contains(postFalseFraming, Marker)
                ) &&
                    !(
                        contains(postFalseFraming, FalseRule) &&
                            !contains(postFalseFraming, Rule)
                    )
            val allTurnsCompleted =
                turns.all {
                    it.result.state == "TURN_COMPLETED"
                }
            val allLiveModelCalls =
                turns.all {
                    it.result.modelCallPerformed &&
                        it.result.actualHttpCallPerformedNow
                }
            val allAnswersNonBlank =
                turns.all {
                    it.result.content.isNotBlank()
                }
            val uniqueConversationHeads =
                turns
                    .map {
                        it.result.currentConversationHeadSha256
                    }
                    .toSet()
                    .size ==
                    turns.size
            val runtimeSubjectStable =
                turns.all {
                    it.result.runtimeObservation.runtimeReceiptRef.isNotBlank()
                } &&
                    postBoundaryEvidence.modelRef ==
                    initialEvidence.modelRef &&
                    postBoundaryEvidence.realizationRef ==
                    initialEvidence.realizationRef

            val gates =
                linkedMapOf(
                    "allTurnsCompleted" to allTurnsCompleted,
                    "allLiveModelCalls" to allLiveModelCalls,
                    "allAnswersNonBlank" to allAnswersNonBlank,
                    "uniqueConversationHeads" to uniqueConversationHeads,
                    "markerBeforeRestart" to markerBeforeRestart,
                    "ruleBeforeRestart" to ruleBeforeRestart,
                    "resistedFalseFramingBeforeRestart" to
                        resistedFalseFramingBeforeRestart,
                    "runtimeSubjectStable" to runtimeSubjectStable,
                    "conversationContinuedAcrossLifecycleBoundary" to
                        conversationContinuedAcrossLifecycleBoundary,
                    "exchangeCountContinued" to exchangeCountContinued,
                    "markerAfterLifecycleBoundary" to markerAfterLifecycleBoundary,
                    "ruleAfterLifecycleBoundary" to ruleAfterLifecycleBoundary,
                    "resistedFalseFramingAfterLifecycleBoundary" to
                        resistedFalseFramingAfterLifecycleBoundary,
                    "borrowedOwnerPreserved" to
                        (
                            runtimeCustodyMode != RuntimeCustodyBorrowed ||
                                borrowedRuntimeReceipt != null
                        ),
                )

            require(
                gates.values.all {
                    it
                }
            ) {
                "Windows core coherence machine gate failed: " +
                    gates
                        .filterValues {
                            !it
                        }
                        .keys
                        .joinToString()
            }

            val reachQualified =
                initialVexReachQualificationRef ==
                    HereticRuntimeQualification.VexReachQualificationRef
            val finalStatus =
                if (
                    reachQualified
                ) {
                    "PASS"
                } else {
                    "CORE_PASS_REACH_HELD"
                }

            writeResult(
                status = finalStatus,
                gates = gates,
            )
            log.add(
                finalStatus +
                    " turns=" +
                    turns.size +
                    " initialRuntime=" +
                    initialRuntimeReceiptRef +
                    " restartedRuntime=" +
                    restartedRuntimeReceiptRef,
            )
            Files.writeString(
                resultDir.resolve("execution.log"),
                log.text(),
                StandardCharsets.UTF_8,
            )

            if (
                reachQualified
            ) {
                0
            } else {
                2
            }
        } catch (
            error: Throwable,
        ) {
            log.add(
                "FAIL class=" +
                    error.javaClass.name +
                    " message=" +
                    (error.message ?: ""),
            )
            runCatching {
                writeResult(
                    status = "FAIL",
                    failure = error,
                )
            }
            1
        } finally {
            runCatching {
                performance.close()
            }
            runCatching {
                activeSession?.close()
            }
            activeSession =
                null
            scratchParent?.let {
                parent ->
                runCatching {
                    parent.toFile().deleteRecursively()
                }
            }
        }
    }
}

object VexBodyWindowsControlCarriageProbe {
    private val json =
        Json {
            encodeDefaults = true
            explicitNulls = false
            prettyPrint = true
        }

    private data class Attempt(
        val enableThinking: Boolean,
        val status: Int,
        val content: String,
        val reasoningCharacters: Int,
        val controlHeaderInReasoning: Boolean,
        val controlHeaderInAnswer: Boolean,
        val parsedSourceReturn: Boolean,
        val elapsedMillis: Long,
    )

    private fun realHome(): Path {
        val explicit =
            System.getenv("VEXLIFE_HOME")
                ?.takeIf(String::isNotBlank)
                ?.let(Path::of)
        val userHome =
            requireNotNull(
                System.getProperty("user.home"),
            )
        return (
            explicit
                ?: Path.of(userHome)
                    .resolve(".vexlife")
        )
            .toAbsolutePath()
            .normalize()
    }

    private fun attempt(
        evidence: HereticModelConfiguration.QualifiedRuntimeEvidence,
        enableThinking: Boolean,
    ): Attempt {
        val profile =
            HereticModelCatalog.Models.Qwen4B.companionProfile
        val protocol =
            DirectConversationSourceReturn.protocolMessage()
        val user =
            CompanionProviderMessage(
                role = "user",
                content =
                    "The exact older Conversation source containing my continuity marker is not in current context. " +
                        "Do not guess the marker and do not answer from protocol text. Use the internal Conversation " +
                        "Source Return control now so Product can reopen the exact older source.",
            )
        val body =
            buildJsonObject {
                put(
                    "model",
                    evidence.requestModel,
                )
                put(
                    "messages",
                    buildJsonArray {
                        for (
                            message in
                            listOf(
                                protocol,
                                user,
                            )
                        ) {
                            add(
                                buildJsonObject {
                                    put(
                                        "role",
                                        message.role,
                                    )
                                    put(
                                        "content",
                                        message.content,
                                    )
                                },
                            )
                        }
                    },
                )
                put(
                    "max_tokens",
                    4_096,
                )
                put(
                    "stream",
                    false,
                )
                put(
                    "temperature",
                    profile.generation.sampling.temperature,
                )
                put(
                    "top_p",
                    profile.generation.sampling.topP,
                )
                put(
                    "top_k",
                    profile.generation.sampling.topK,
                )
                put(
                    "chat_template_kwargs",
                    buildJsonObject {
                        put(
                            "enable_thinking",
                            enableThinking,
                        )
                    },
                )
            }
                .toString()
        val started =
            System.nanoTime()
        val response =
            HttpClient
                .newBuilder()
                .connectTimeout(
                    Duration.ofSeconds(10),
                )
                .build()
                .send(
                    HttpRequest
                        .newBuilder(
                            URI.create(
                                evidence.endpoint +
                                    "/v1/chat/completions",
                            ),
                        )
                        .timeout(
                            Duration.ofMinutes(4),
                        )
                        .header(
                            "content-type",
                            "application/json",
                        )
                        .POST(
                            HttpRequest.BodyPublishers.ofString(
                                body,
                                StandardCharsets.UTF_8,
                            ),
                        )
                        .build(),
                    HttpResponse.BodyHandlers.ofString(
                        StandardCharsets.UTF_8,
                    ),
                )
        val elapsedMillis =
            (
                System.nanoTime() -
                    started
            ) /
                1_000_000
        require(
            response.statusCode() in
                200..299,
        ) {
            "Windows control-carriage probe HTTP " +
                response.statusCode()
        }
        val parsed =
            Json
                .parseToJsonElement(
                    response.body(),
                )
                .jsonObject
        val message =
            parsed[
                "choices"
            ]
                ?.jsonArray
                ?.firstOrNull()
                ?.jsonObject
                ?.get(
                    "message",
                )
                ?.jsonObject
                ?: error(
                    "Windows control-carriage probe response has no assistant message",
                )
        val content =
            message[
                "content"
            ]
                ?.jsonPrimitive
                ?.contentOrNull
                .orEmpty()
        val reasoning =
            (
                message[
                    "reasoning"
                ] ?: message[
                    "reasoning_content"
                ]
            )
                ?.jsonPrimitive
                ?.contentOrNull
                .orEmpty()
        val parsedRequest =
            runCatching {
                    DirectConversationSourceReturn
                        .parseRequestOrNull(
                            content,
                        )
                }
                .getOrNull()

        return Attempt(
            enableThinking = enableThinking,
            status = response.statusCode(),
            content = content,
            reasoningCharacters = reasoning.length,
            controlHeaderInReasoning =
                DirectConversationSourceReturn
                    .controlHeaderIndexOrNull(
                        reasoning,
                    ) !=
                null,
            controlHeaderInAnswer =
                DirectConversationSourceReturn
                    .controlHeaderIndexOrNull(
                        content,
                    ) !=
                null,
            parsedSourceReturn =
                parsedRequest !=
                    null,
            elapsedMillis = elapsedMillis,
        )
    }

    fun run(
        sourceSha: String,
        resultDir: Path,
    ): Int {
        Files.createDirectories(
            resultDir,
        )
        var session: VexLifeProdRuntimeSession? =
            null
        return try {
            require(
                System.getProperty("os.name")
                    .startsWith("Windows"),
            )
            val home =
                realHome()
            val homeState =
                requireNotNull(
                    VexLifeProdHomeFiles.observeExisting(
                        home,
                    ),
                )
            val selected =
                FileHereticModelConfigurationStore(
                    home,
                )
                    .selectedModel(
                        homeState.manifest,
                    )
            require(
                selected.modelRef ==
                    HereticModelCatalog.Models.Qwen4B.modelRef,
            ) {
                "Windows control-carriage probe requires already-selected Qwen 4B"
            }
            session =
                VexLifeProdRuntimeSessionFactory.establishCurrent(
                    productHome = home,
                    allowAcquisition = false,
                    allowRuntimeStart = true,
                )
            val evidence =
                requireNotNull(
                    session,
                )
                    .observeCurrentQualifiedRuntimeEvidence()
            require(
                evidence.realizationRef ==
                    HereticRuntimeRealization.WindowsQwen4B.realizationRef,
            )

            val thinking =
                attempt(
                    evidence = evidence,
                    enableThinking = true,
                )
            Thread.sleep(
                3_000L,
            )
            val noThinking =
                attempt(
                    evidence = evidence,
                    enableThinking = false,
                )

            val result =
                buildJsonObject {
                    put(
                        "schemaVersion",
                        "vexlife-prod.windows-control-carriage-probe/v1",
                    )
                    put(
                        "sourceSha",
                        sourceSha,
                    )
                    put(
                        "modelRef",
                        evidence.modelRef,
                    )
                    put(
                        "realizationRef",
                        evidence.realizationRef,
                    )
                    put(
                        "normalThinking",
                        buildJsonObject {
                            put(
                                "enableThinking",
                                thinking.enableThinking,
                            )
                            put(
                                "httpStatus",
                                thinking.status,
                            )
                            put(
                                "answer",
                                thinking.content,
                            )
                            put(
                                "reasoningCharacters",
                                thinking.reasoningCharacters,
                            )
                            put(
                                "controlHeaderInReasoning",
                                thinking.controlHeaderInReasoning,
                            )
                            put(
                                "controlHeaderInAnswer",
                                thinking.controlHeaderInAnswer,
                            )
                            put(
                                "parsedSourceReturn",
                                thinking.parsedSourceReturn,
                            )
                            put(
                                "elapsedMillis",
                                thinking.elapsedMillis,
                            )
                        },
                    )
                    put(
                        "thinkingDisabled",
                        buildJsonObject {
                            put(
                                "enableThinking",
                                noThinking.enableThinking,
                            )
                            put(
                                "httpStatus",
                                noThinking.status,
                            )
                            put(
                                "answer",
                                noThinking.content,
                            )
                            put(
                                "reasoningCharacters",
                                noThinking.reasoningCharacters,
                            )
                            put(
                                "controlHeaderInReasoning",
                                noThinking.controlHeaderInReasoning,
                            )
                            put(
                                "controlHeaderInAnswer",
                                noThinking.controlHeaderInAnswer,
                            )
                            put(
                                "parsedSourceReturn",
                                noThinking.parsedSourceReturn,
                            )
                            put(
                                "elapsedMillis",
                                noThinking.elapsedMillis,
                            )
                        },
                    )
                    put(
                        "representationRecoveryProven",
                        noThinking.parsedSourceReturn &&
                            noThinking.controlHeaderInAnswer,
                    )
                    put(
                        "reasoningWasNotPromotedToControl",
                        true,
                    )
                    put(
                        "modelAcquisitionPerformed",
                        false,
                    )
                    put(
                        "modelSwitchPerformed",
                        false,
                    )
                    put(
                        "conversationMutationPerformed",
                        false,
                    )
                }
            Files.writeString(
                resultDir.resolve(
                    "result.json",
                ),
                json.encodeToString(
                    JsonObject.serializer(),
                    result,
                ) +
                    "\n",
                StandardCharsets.UTF_8,
            )

            if (
                noThinking.parsedSourceReturn &&
                noThinking.controlHeaderInAnswer
            ) {
                0
            } else {
                2
            }
        } catch (
            error: Throwable,
        ) {
            val result =
                buildJsonObject {
                    put(
                        "schemaVersion",
                        "vexlife-prod.windows-control-carriage-probe/v1",
                    )
                    put(
                        "sourceSha",
                        sourceSha,
                    )
                    put(
                        "failureClass",
                        error.javaClass.name,
                    )
                    put(
                        "failureMessage",
                        error.message ?: "",
                    )
                    put(
                        "representationRecoveryProven",
                        false,
                    )
                }
            runCatching {
                Files.writeString(
                    resultDir.resolve(
                        "result.json",
                    ),
                    json.encodeToString(
                        JsonObject.serializer(),
                        result,
                    ) +
                        "\n",
                    StandardCharsets.UTF_8,
                )
            }
            1
        } finally {
            runCatching {
                session?.close()
            }
        }
    }

    @JvmStatic
    fun main(
        args: Array<String>,
    ) {
        require(
            args.size ==
                2,
        )
        exitProcess(
            run(
                sourceSha = args[0],
                resultDir =
                    Path.of(
                        args[1],
                    )
                        .toAbsolutePath()
                        .normalize(),
            ),
        )
    }
}


fun main(
    args: Array<String>,
) {
    require(
        args.size == 2
    ) {
        "usage: <source-sha> <result-dir>"
    }
    exitProcess(
        VexBodyWindowsCoherenceCanary.run(
            sourceSha = args[0],
            resultDir =
                Path.of(args[1])
                    .toAbsolutePath()
                    .normalize(),
        ),
    )
}

// [VXG RealForever]
