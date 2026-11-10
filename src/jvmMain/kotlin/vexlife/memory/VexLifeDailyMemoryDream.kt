package vexlife.memory

import java.nio.charset.StandardCharsets
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import vexlife.contract.VexContractJson
import vexlife.contract.VexDurableContractFile
import vexlife.filesystem.VexOwnedPath

/** Deterministic manual G03 memory-only Daily Dream over a validated Product Memory frontier. */
object VexLifeDailyMemoryDream {
    const val ContractRef = "contract.multivex.g03.daily-memory-only-dream/v1"
    private const val WriterSchema = "vexlife-prod.daily-memory-dream-writer/v1"

    data class Invocation(
        val instanceRef: String,
        val restInvocationAuthorityRef: String,
        val dayRef: String,
        val dayIndex: Long,
        val calendarDateRef: String,
        val timeZoneRef: String,
        val observedAt: Instant,
        val expectedConversationHeadSha256: String,
        val expectedScoreHeadSha256: String,
        val expectedDailyDreamHeadSha256: String?,
    )

    data class Result(
        val state: String,
        val priorDailyDreamHeadSha256: String?,
        val dailyDreamHeadSha256: String,
        val dailyStratumSha256: String,
        val sourceConversationHeadSha256: String,
        val sourceScoreHeadSha256: String,
        val sourceSemanticAuthorityHeadSha256: String,
        val activeStatementRefs: List<String>,
        val heldStatementRefs: List<String>,
        val openLoopRefs: List<String>,
        val lineageAwareGenerativeDreamRan: Boolean = false,
        val systemMemoryConsolidationRan: Boolean = true,
        val rhythmLearned: Boolean = false,
        val trainingRan: Boolean = false,
        val modelWeightsChanged: Boolean = false,
        val adapterChanged: Boolean = false,
        val synchronizationActivated: Boolean = false,
        val poweredDown: Boolean = false,
        val publicationPerformed: Boolean = false,
        val firstPersonAuthorityGranted: Boolean = false,
    )

    internal data class Faults(
        val failAfterStratumWrite: Boolean = false,
        val failBeforeHeadAdvance: Boolean = false,
    )

    @Serializable
    private data class WriterLeaseCore(
        val schemaVersion: String,
        val companionLineageRef: String,
        val threadRef: String,
        val instanceRef: String,
        val pid: Long,
        val lockToken: String,
        val formedAt: String,
    )

    @Serializable
    private data class WriterLease(
        val schemaVersion: String,
        val companionLineageRef: String,
        val threadRef: String,
        val instanceRef: String,
        val pid: Long,
        val lockToken: String,
        val formedAt: String,
        val leaseSha256: String,
    )

    private data class HeldWriter(
        val home: Path,
        val path: Path,
        val lease: WriterLease,
    ) : AutoCloseable {
        override fun close() {
            val current =
                requireNotNull(
                    VexDurableContractFile.read<WriterLease>(
                        ownedRoot = home,
                        path = path,
                    ),
                ) {
                    "Daily Dream writer lease disappeared before release"
                }
            require(current == lease && leaseSha256(current) == current.leaseSha256) {
                "Daily Dream writer lease changed before release"
            }
            require(Files.deleteIfExists(path)) {
                "Daily Dream writer lease could not be released"
            }
        }
    }

    private data class ReceiptSpec(
        val schemaVersion: String,
        val refField: String,
        val hashField: String,
        val prefix: String,
        val directory: String,
    )

    private val Orientation = ReceiptSpec("vextreme.daily-pre-rest-orientation/v1", "preRestOrientationRef", "orientationSha256", "pre-rest-orientation", "orientations")
    private val PreDream = ReceiptSpec("vextreme.daily-pre-dream-state/v1", "preDreamStateRef", "preDreamStateSha256", "pre-dream-state", "pre-dream")
    private val Closure = ReceiptSpec("vextreme.daily-day-closure/v1", "dayClosureRef", "dayClosureSha256", "day-closure", "closures")
    private val Consolidation = ReceiptSpec("vextreme.daily-memory-consolidation/v1", "memoryConsolidationRef", "memoryConsolidationSha256", "memory-consolidation", "consolidations")
    private val PostDream = ReceiptSpec("vextreme.daily-post-dream-state/v1", "postDreamStateRef", "postDreamStateSha256", "post-dream-state", "post-dream")
    private val Stratum = ReceiptSpec("vextreme.daily-stratum/v1", "dailyStratumRef", "dailyStratumSha256", "daily-stratum", "strata")
    private val Wake = ReceiptSpec("vextreme.daily-wake-receipt/v1", "wakeReceiptRef", "wakeReceiptSha256", "daily-wake", "wakes")
    private val Head = ReceiptSpec("vextreme.daily-memory-dream-head/v1", "dailyDreamHeadRef", "dailyDreamHeadSha256", "daily-dream-head", "heads")

    fun commit(productHome: Path, invocation: Invocation): Result =
        commitWithFrontier(
            productHome = productHome,
            invocation = invocation,
            sourceProvider = { VexLifeCurrentMemoryReader.readDreamSourceFrontier(productHome) },
        )

    internal fun commitWithFrontier(
        productHome: Path,
        invocation: Invocation,
        faults: Faults = Faults(),
        sourceProvider: () -> VexLifeCurrentMemoryReader.DreamSourceFrontier,
    ): Result {
        validateInvocation(invocation)
        val home = VexOwnedPath.requireRoot(productHome, "Daily Memory Dream Vex Home")
        val initial = sourceProvider()
        requireExpectedSources(invocation, initial)
        val root = dreamRoot(home, initial)

        acquireWriter(home, root, initial, invocation).use {
            val frontier = sourceProvider()
            require(frontier == initial) {
                "DREAM_SOURCE_STALE: source frontier changed before Dream formation"
            }

            frontier.priorDaily?.let { prior ->
                if (prior.dayRef == invocation.dayRef) {
                    require(
                        prior.dayIndex == invocation.dayIndex &&
                            prior.calendarDateRef == invocation.calendarDateRef &&
                            prior.timeZoneRef == invocation.timeZoneRef &&
                            prior.observedAt == invocation.observedAt.toString() &&
                            prior.restInvocationAuthorityRef == invocation.restInvocationAuthorityRef &&
                            frontier.conversationHeadSha256 == invocation.expectedConversationHeadSha256 &&
                            frontier.scoreHeadSha256 == invocation.expectedScoreHeadSha256,
                    ) {
                        "DREAM_DAY_CONFLICT: committed dayRef has different identity"
                    }
                    return result("IDEMPOTENT_REPLAY", frontier, prior.dailyDreamHeadSha256, prior.dailyStratumSha256)
                }
            }

            require(
                invocation.expectedDailyDreamHeadSha256 ==
                    frontier.priorDaily?.dailyDreamHeadSha256,
            ) {
                "DREAM_HEAD_MISMATCH: expected Daily Dream head differs from current head"
            }
            require(invocation.dayIndex == (frontier.priorDaily?.dayIndex?.plus(1) ?: 0L)) {
                "DREAM_DAY_INVALID: dayIndex must advance exactly one committed day"
            }

            observeTail(home, root, frontier)?.let {
                requireTailMatchesInvocation(home, root, it, frontier, invocation)
            }

            val common = commonCore(frontier, invocation)
            val activeRefs = frontier.activeStatementBindings.refs("statementRef")
            val heldRefs = frontier.heldStatementBindings.refs("statementRef")
            val openLoopRefs = frontier.openLoopBindings.refs("openLoopRef")

            val orientation =
                form(
                    Orientation,
                    common.extend {
                        put("invocationMode", "MANUAL_ONE_SHOT")
                        put("restInvocationAuthorityRef", invocation.restInvocationAuthorityRef)
                        put("selectedMode", "MEMORY_ONLY_CONSOLIDATION")
                        put("exactG01ConversationHeadSha256", frontier.conversationHeadSha256)
                        put("exactG02ScoreHeadSha256", frontier.scoreHeadSha256)
                        putStrings("allowedChangeClasses", listOf("DAILY_MEMORY_FRONTIER", "DAILY_STRATUM", "WAKE_RECEIPT"))
                        putStrings("heldChangeClasses", listOf("MODEL_WEIGHTS", "RHYTHM", "TRAINING", "SYNC", "PUBLICATION", "POWER_CONTROL", "FIRST_PERSON_AUTHORITY"))
                        putStrings("openLoopRefs", openLoopRefs)
                        put("noticeState", "FORMED")
                        put("formedAt", invocation.observedAt.toString())
                    },
                )
            writeAddressed(home, root, Orientation, orientation)

            val preDream =
                form(
                    PreDream,
                    common.extend {
                        putHash("orientationSha256", orientation, Orientation)
                        put("sourceConversationHeadSha256", frontier.conversationHeadSha256)
                        put("sourceConversationEventHash", frontier.conversationResponseEventHash)
                        put("sourceScoreHeadSha256", frontier.scoreHeadSha256)
                        put("sourceScoreEventHash", frontier.scoreEventHash)
                        putStrings("currentStatementRefs", frontier.currentStatementRefs)
                        putStrings("openLoopRefs", openLoopRefs)
                        put("preDreamRuntimeRef", frontier.selectedRuntimeRef)
                        put("preDreamModelProfileRef", frontier.selectedModelProfileRef)
                        put("rawConversationContentIncluded", false)
                        put("formedAt", invocation.observedAt.toString())
                    },
                )
            writeAddressed(home, root, PreDream, preDream)

            val closure =
                form(
                    Closure,
                    common.extend {
                        putHash("preDreamStateSha256", preDream, PreDream)
                        put("sourceConversationHeadSha256", frontier.conversationHeadSha256)
                        put("sourceScoreHeadSha256", frontier.scoreHeadSha256)
                        putStrings("currentStatementRefs", frontier.currentStatementRefs)
                        putStrings("heldOrDeferredStatementRefs", heldRefs)
                        putStrings("openLoopRefs", openLoopRefs)
                        put("closureState", "FROZEN")
                        put("rawConversationContentIncluded", false)
                        put("formedAt", invocation.observedAt.toString())
                    },
                )
            writeAddressed(home, root, Closure, closure)

            val consolidation =
                form(
                    Consolidation,
                    common.extend {
                        putHash("dayClosureSha256", closure, Closure)
                        put("carriedCurrentScoreBindings", JsonArray(frontier.activeStatementBindings))
                        put("heldOrDeferredScoreBindings", JsonArray(frontier.heldStatementBindings))
                        put("openLoopCarryForwardBindings", JsonArray(frontier.openLoopBindings))
                        put("sourceConversationHeadSha256", frontier.conversationHeadSha256)
                        put("sourceScoreHeadSha256", frontier.scoreHeadSha256)
                        put("referenceLevelOnly", true)
                        put("rawConversationContentIncluded", false)
                        put("newSemanticAcceptanceCreated", false)
                        put("firstPersonAuthorityGranted", false)
                        put("lineageAwareGenerativeDreamRan", false)
                        put("systemMemoryConsolidationRan", true)
                        put("rhythmLearned", false)
                        put("trainingRan", false)
                        put("modelWeightsChanged", false)
                        put("adapterChanged", false)
                        put("synchronizationActivated", false)
                        put("publicationPerformed", false)
                        put("poweredDown", false)
                        put("formedAt", invocation.observedAt.toString())
                    },
                )
            writeAddressed(home, root, Consolidation, consolidation)

            val postDream =
                form(
                    PostDream,
                    common.extend {
                        putHash("memoryConsolidationSha256", consolidation, Consolidation)
                        put("sourceConversationHeadSha256", frontier.conversationHeadSha256)
                        put("sourceScoreHeadSha256", frontier.scoreHeadSha256)
                        putStrings("activeContinuityStatementRefs", activeRefs)
                        putStrings("heldOrDeferredStatementRefs", heldRefs)
                        putStrings("openLoopRefs", openLoopRefs)
                        put("selectedRuntimeRef", frontier.selectedRuntimeRef)
                        put("selectedModelProfileRef", frontier.selectedModelProfileRef)
                        put("preDreamRuntimeRef", frontier.selectedRuntimeRef)
                        put("preDreamModelProfileRef", frontier.selectedModelProfileRef)
                        put("memoryFrontierChanged", true)
                        put("rawConversationContentIncluded", false)
                        put("formedAt", invocation.observedAt.toString())
                    },
                )
            writeAddressed(home, root, PostDream, postDream)

            val sequence = frontier.priorDaily?.sequence?.plus(1) ?: 0L
            val stratum =
                form(
                    Stratum,
                    common.extend {
                        put("sequence", sequence)
                        putNullable("priorDailyStratumSha256", frontier.priorDaily?.dailyStratumSha256)
                        putHash("orientationSha256", orientation, Orientation)
                        putHash("preDreamStateSha256", preDream, PreDream)
                        putHash("dayClosureSha256", closure, Closure)
                        putHash("memoryConsolidationSha256", consolidation, Consolidation)
                        putHash("postDreamStateSha256", postDream, PostDream)
                        put("sourceConversationHeadSha256", frontier.conversationHeadSha256)
                        put("sourceScoreHeadSha256", frontier.scoreHeadSha256)
                        put("sourceModelProfileRef", frontier.selectedModelProfileRef)
                        put("sourceRuntimeRef", frontier.selectedRuntimeRef)
                        put("modelWeightsChanged", false)
                        put("trainingRan", false)
                        put("rhythmLearned", false)
                        put("synchronizationActivated", false)
                        put("formedAt", invocation.observedAt.toString())
                    },
                )
            writeAddressed(home, root, Stratum, stratum)

            if (faults.failAfterStratumWrite) {
                error("DREAM_HEAD_MISMATCH: simulated failure after durable Daily Stratum before head advance")
            }

            val wake =
                form(
                    Wake,
                    common.extend {
                        putHash("dailyStratumSha256", stratum, Stratum)
                        put("selectedMode", "MEMORY_ONLY_CONSOLIDATION")
                        put("lineageAwareGenerativeDreamRan", false)
                        put("systemMemoryConsolidationRan", true)
                        put("trainingRan", false)
                        put("modelWeightsChanged", false)
                        put("adapterChanged", false)
                        put("selectedRuntimeRef", frontier.selectedRuntimeRef)
                        put("selectedModelProfileRef", frontier.selectedModelProfileRef)
                        put("preDreamRuntimeRef", frontier.selectedRuntimeRef)
                        put("preDreamModelProfileRef", frontier.selectedModelProfileRef)
                        put("memoryFrontierChanged", true)
                        put("poweredDown", false)
                        put("synchronizationActivated", false)
                        put("publicationPerformed", false)
                        putStrings("openLoopRefs", openLoopRefs)
                        putStrings("heldOrDeferredRefs", heldRefs)
                        put("successorState", "FRESH_PROCESS_MAY_RESUME_FROM_COMMITTED_STRATUM")
                        put("uninterruptedSubjectiveAwarenessClaimed", false)
                        put("formedAt", invocation.observedAt.toString())
                    },
                )
            writeAddressed(home, root, Wake, wake)

            val head =
                form(
                    Head,
                    buildJsonObject {
                        put("homeRef", frontier.homeRef)
                        put("deviceRef", frontier.deviceRef)
                        put("companionLineageRef", frontier.companionLineageRef)
                        put("threadRef", frontier.threadRef)
                        put("sequence", sequence)
                        put("dayRef", invocation.dayRef)
                        put("dayIndex", invocation.dayIndex)
                        putHash("dailyStratumSha256", stratum, Stratum)
                        putHash("wakeReceiptSha256", wake, Wake)
                        put("sourceConversationHeadSha256", frontier.conversationHeadSha256)
                        put("sourceScoreHeadSha256", frontier.scoreHeadSha256)
                        put("sourceSemanticAuthorityHeadSha256", frontier.semanticAuthorityHeadSha256)
                        putNullable("priorDailyDreamHeadSha256", frontier.priorDaily?.dailyDreamHeadSha256)
                        put("contractRef", ContractRef)
                        put("formedAt", invocation.observedAt.toString())
                    },
                )
            writeAddressed(home, root, Head, head)

            require(sourceProvider() == frontier) {
                "DREAM_SOURCE_STALE: source frontier changed before atomic Daily Dream head advance"
            }
            if (faults.failBeforeHeadAdvance) {
                error("DREAM_HEAD_MISMATCH: simulated failure before Daily Dream head advance")
            }

            VexDurableContractFile.write(
                ownedRoot = home,
                path = root.resolve("head.json"),
                value = head,
            )

            return result("COMMITTED", frontier, head.value(Head.hashField), stratum.value(Stratum.hashField))
        }
    }

    fun recoverAbandonedWriter(
        productHome: Path,
        expectedCompanionLineageRef: String,
        expectedThreadRef: String,
        expectedInstanceRef: String,
    ): Boolean {
        val home = VexOwnedPath.requireRoot(productHome, "Daily Memory Dream Vex Home")
        val root =
            VexOwnedPath.resolveInside(
                home,
                "daily-memory-dream/" + expectedCompanionLineageRef + "/" + expectedThreadRef,
            )
        val lock = root.resolve("writer.lock")
        if (!Files.exists(lock, LinkOption.NOFOLLOW_LINKS)) return false
        val lease =
            requireNotNull(
                VexDurableContractFile.read<WriterLease>(
                    ownedRoot = home,
                    path = lock,
                ),
            ) {
                "Daily Dream writer lease is unreadable"
            }
        require(
            lease.schemaVersion == WriterSchema &&
                lease.companionLineageRef == expectedCompanionLineageRef &&
                lease.threadRef == expectedThreadRef &&
                lease.instanceRef == expectedInstanceRef &&
                leaseSha256(lease) == lease.leaseSha256,
        ) {
            "Daily Dream writer lease identity is invalid"
        }
        require(processState(lease.pid) == "ABSENT") {
            "Daily Dream writer recovery requires an absent owning process"
        }
        return VexDurableContractFile.delete(ownedRoot = home, path = lock)
    }

    private fun result(
        state: String,
        frontier: VexLifeCurrentMemoryReader.DreamSourceFrontier,
        headSha: String,
        stratumSha: String,
    ): Result =
        Result(
            state = state,
            priorDailyDreamHeadSha256 = frontier.priorDaily?.dailyDreamHeadSha256,
            dailyDreamHeadSha256 = headSha,
            dailyStratumSha256 = stratumSha,
            sourceConversationHeadSha256 = frontier.conversationHeadSha256,
            sourceScoreHeadSha256 = frontier.scoreHeadSha256,
            sourceSemanticAuthorityHeadSha256 = frontier.semanticAuthorityHeadSha256,
            activeStatementRefs = frontier.activeStatementBindings.refs("statementRef"),
            heldStatementRefs = frontier.heldStatementBindings.refs("statementRef"),
            openLoopRefs = frontier.openLoopBindings.refs("openLoopRef"),
        )

    private fun commonCore(
        frontier: VexLifeCurrentMemoryReader.DreamSourceFrontier,
        invocation: Invocation,
    ): JsonObject =
        buildJsonObject {
            put("homeRef", frontier.homeRef)
            put("deviceRef", frontier.deviceRef)
            put("companionLineageRef", frontier.companionLineageRef)
            put("threadRef", frontier.threadRef)
            put("dayRef", invocation.dayRef)
            put("dayIndex", invocation.dayIndex)
            put("calendarDateRef", invocation.calendarDateRef)
            put("timeZoneRef", invocation.timeZoneRef)
            put("observedAt", invocation.observedAt.toString())
            put("contractRef", ContractRef)
            put("privacyClass", "DEVICE_PRIVATE")
            put("sourceSemanticAuthorityHeadSha256", frontier.semanticAuthorityHeadSha256)
        }

    private fun form(spec: ReceiptSpec, core: JsonObject): JsonObject =
        VexLifeCurrentMemoryReader.formDailyReceipt(
            schemaVersion = spec.schemaVersion,
            refField = spec.refField,
            hashField = spec.hashField,
            prefix = spec.prefix,
            core = core,
        )

    private fun writeAddressed(home: Path, root: Path, spec: ReceiptSpec, value: JsonObject) {
        val sha = value.value(spec.hashField)
        val path = root.resolve(spec.directory).resolve("$sha.json")
        VexOwnedPath.ensureParent(home, path)
        try {
            Files.writeString(
                path,
                VexContractJson.canonicalEncodeElement(value) + "\n",
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE,
            )
            VexDurableContractFile.secureFile(path)
        } catch (_: FileAlreadyExistsException) {
            val existing =
                VexLifeCurrentMemoryReader.readDailyReceiptAt(
                    home = home,
                    path = path,
                    schemaVersion = spec.schemaVersion,
                    refField = spec.refField,
                    hashField = spec.hashField,
                    prefix = spec.prefix,
                    label = spec.prefix,
                )
            require(existing == value) {
                "DREAM_RECEIPT_CORRUPT: existing addressed receipt differs from exact retry"
            }
        }
    }

    private fun observeTail(
        home: Path,
        root: Path,
        frontier: VexLifeCurrentMemoryReader.DreamSourceFrontier,
    ): JsonObject? {
        val directory = root.resolve(Stratum.directory)
        if (!Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) return null
        val expectedSequence = frontier.priorDaily?.sequence?.plus(1) ?: 0L
        val expectedPrior = frontier.priorDaily?.dailyStratumSha256
        val candidates =
            Files.list(directory).use { stream ->
                stream
                    .filter { it.fileName.toString().endsWith(".json") }
                    .map { path ->
                        VexLifeCurrentMemoryReader.readDailyReceiptAt(
                            home = home,
                            path = path,
                            schemaVersion = Stratum.schemaVersion,
                            refField = Stratum.refField,
                            hashField = Stratum.hashField,
                            prefix = Stratum.prefix,
                            label = "Daily Stratum tail",
                        )
                    }
                    .filter { it.longValue("sequence") >= expectedSequence }
                    .toList()
            }
        require(
            candidates.all {
                it.longValue("sequence") == expectedSequence &&
                    it.nullableValue("priorDailyStratumSha256") == expectedPrior
            } &&
                candidates.size <= 1,
        ) {
            "DREAM_TAIL_ATTENTION: reordered or competing Daily Stratum tail exists"
        }
        return candidates.singleOrNull()
    }

    private fun requireTailMatchesInvocation(
        home: Path,
        root: Path,
        tail: JsonObject,
        frontier: VexLifeCurrentMemoryReader.DreamSourceFrontier,
        invocation: Invocation,
    ) {
        require(
            tail.value("dayRef") == invocation.dayRef &&
                tail.longValue("dayIndex") == invocation.dayIndex &&
                tail.value("calendarDateRef") == invocation.calendarDateRef &&
                tail.value("timeZoneRef") == invocation.timeZoneRef &&
                tail.value("observedAt") == invocation.observedAt.toString() &&
                tail.value("sourceConversationHeadSha256") == frontier.conversationHeadSha256 &&
                tail.value("sourceScoreHeadSha256") == frontier.scoreHeadSha256 &&
                tail.value("sourceSemanticAuthorityHeadSha256") == frontier.semanticAuthorityHeadSha256,
        ) {
            "DREAM_TAIL_ATTENTION: existing Daily Stratum tail differs from requested day/source"
        }
        val orientation =
            VexLifeCurrentMemoryReader.readDailyReceiptAt(
                home = home,
                path = root.resolve(Orientation.directory).resolve(tail.value("orientationSha256") + ".json"),
                schemaVersion = Orientation.schemaVersion,
                refField = Orientation.refField,
                hashField = Orientation.hashField,
                prefix = Orientation.prefix,
                label = "Daily Stratum tail orientation",
            )
        require(orientation.value("restInvocationAuthorityRef") == invocation.restInvocationAuthorityRef) {
            "DREAM_TAIL_ATTENTION: tail invocation authority differs from exact retry"
        }
    }

    private fun acquireWriter(
        home: Path,
        root: Path,
        frontier: VexLifeCurrentMemoryReader.DreamSourceFrontier,
        invocation: Invocation,
    ): HeldWriter {
        val lock = root.resolve("writer.lock")
        VexOwnedPath.ensureParent(home, lock)
        val core =
            WriterLeaseCore(
                schemaVersion = WriterSchema,
                companionLineageRef = frontier.companionLineageRef,
                threadRef = frontier.threadRef,
                instanceRef = invocation.instanceRef,
                pid = ProcessHandle.current().pid(),
                lockToken = UUID.randomUUID().toString().lowercase(),
                formedAt = Instant.now().toString(),
            )
        val lease =
            WriterLease(
                schemaVersion = core.schemaVersion,
                companionLineageRef = core.companionLineageRef,
                threadRef = core.threadRef,
                instanceRef = core.instanceRef,
                pid = core.pid,
                lockToken = core.lockToken,
                formedAt = core.formedAt,
                leaseSha256 = sha256(VexContractJson.canonicalEncode(core)),
            )
        try {
            Files.writeString(
                lock,
                VexContractJson.encode(lease) + "\n",
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE,
            )
            VexDurableContractFile.secureFile(lock)
        } catch (_: FileAlreadyExistsException) {
            val observed =
                requireNotNull(
                    VexDurableContractFile.read<WriterLease>(
                        ownedRoot = home,
                        path = lock,
                    ),
                ) {
                    "Daily Dream writer lease exists but is unreadable"
                }
            require(leaseSha256(observed) == observed.leaseSha256) {
                "Daily Dream writer lease is invalid"
            }
            if (processState(observed.pid) == "ABSENT") {
                error("DREAM_WRITER_RECOVERY_REQUIRED: prior Daily Dream writer is absent; explicit recovery is required")
            }
            error("DREAM_WRITER_CONFLICT: another Daily Dream writer is active or unverifiable")
        }
        return HeldWriter(home = home, path = lock, lease = lease)
    }

    private fun requireExpectedSources(
        invocation: Invocation,
        frontier: VexLifeCurrentMemoryReader.DreamSourceFrontier,
    ) {
        require(invocation.expectedConversationHeadSha256 == frontier.conversationHeadSha256) {
            "DREAM_SOURCE_STALE: expected Conversation head is stale"
        }
        require(invocation.expectedScoreHeadSha256 == frontier.scoreHeadSha256) {
            "DREAM_SOURCE_STALE: expected Score head is stale"
        }
    }

    private fun validateInvocation(invocation: Invocation) {
        requirePortableRef(invocation.instanceRef, "instanceRef")
        requirePortableRef(invocation.restInvocationAuthorityRef, "restInvocationAuthorityRef")
        requirePortableRef(invocation.dayRef, "dayRef")
        require(invocation.dayIndex >= 0) {
            "DREAM_DAY_INVALID: dayIndex must be nonnegative"
        }
        LocalDate.parse(invocation.calendarDateRef)
        ZoneId.of(invocation.timeZoneRef)
        requireSha256(invocation.expectedConversationHeadSha256, "expectedConversationHeadSha256")
        requireSha256(invocation.expectedScoreHeadSha256, "expectedScoreHeadSha256")
        invocation.expectedDailyDreamHeadSha256?.let {
            requireSha256(it, "expectedDailyDreamHeadSha256")
        }
    }

    private fun dreamRoot(home: Path, frontier: VexLifeCurrentMemoryReader.DreamSourceFrontier): Path =
        VexOwnedPath.resolveInside(
            home,
            "daily-memory-dream/" + frontier.companionLineageRef + "/" + frontier.threadRef,
        )

    private fun leaseSha256(lease: WriterLease): String =
        sha256(
            VexContractJson.canonicalEncode(
                WriterLeaseCore(
                    schemaVersion = lease.schemaVersion,
                    companionLineageRef = lease.companionLineageRef,
                    threadRef = lease.threadRef,
                    instanceRef = lease.instanceRef,
                    pid = lease.pid,
                    lockToken = lease.lockToken,
                    formedAt = lease.formedAt,
                ),
            ),
        )

    private fun processState(pid: Long): String =
        try {
            ProcessHandle.of(pid).map { if (it.isAlive) "ACTIVE" else "ABSENT" }.orElse("ABSENT")
        } catch (_: Exception) {
            "UNVERIFIABLE"
        }

    private fun JsonObject.extend(block: JsonObjectBuilder.() -> Unit): JsonObject =
        buildJsonObject {
            this@extend.forEach { (key, value) -> put(key, value) }
            block()
        }

    private fun JsonObjectBuilder.putStrings(key: String, values: List<String>) {
        put(key, JsonArray(values.map(::JsonPrimitive)))
    }

    private fun JsonObjectBuilder.putNullable(key: String, value: String?) {
        put(key, value?.let(::JsonPrimitive) ?: JsonNull)
    }

    private fun JsonObjectBuilder.putHash(key: String, value: JsonObject, spec: ReceiptSpec) {
        put(key, value.value(spec.hashField))
    }

    private fun List<JsonObject>.refs(field: String): List<String> = map { it.value(field) }

    private fun JsonObject.value(field: String): String = requireNotNull(this[field]).jsonPrimitive.content

    private fun JsonObject.nullableValue(field: String): String? {
        val element = this[field] ?: return null
        return if (element === JsonNull) null else element.jsonPrimitive.content
    }

    private fun JsonObject.longValue(field: String): Long = value(field).toLong()

    private fun requirePortableRef(value: String, label: String) {
        require(value.matches(Regex("[A-Za-z0-9][A-Za-z0-9._:/-]*"))) {
            "$label must be one portable ref"
        }
    }

    private fun requireSha256(value: String, label: String) {
        require(value.matches(Regex("[0-9a-f]{64}"))) {
            "$label must be one lowercase SHA-256"
        }
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}

// [VXG RealForever]
