package vexlife.harness

import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import vexlife.companion.DirectCompanionConnection
import vexlife.conversation.DirectConversationStore
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.home.VexLifeProdHome
import vexlife.local.VexLifeProdLocalSetup
import vexlife.logging.VexLogger
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.runtime.FileHereticRuntimeReceiptStore
import vexlife.model.heretic.runtime.HereticPersistentRuntime

/** Test-only adaptive counterpart proof; no Product source consumes this adapter. */
object DynamicCounterpartAdapter {
    @Serializable
    data class Config(
        val runtimeHome: String,
        val scenarioFile: String,
        val resultDir: String,
        val sourceSha: String,
        val sourceClean: Boolean,
        val experimentWindow: Boolean,
        val runtimeStartAtBind: Boolean,
    )

    fun run(config: Config): Int {
        require(config.experimentWindow) {
            "dynamic counterpart requires one dedicated experiment window"
        }
        require(config.sourceClean) { "dynamic counterpart requires exact clean source" }
        require(Regex("[0-9a-f]{40}").matches(config.sourceSha)) {
            "sourceSha must be one exact commit"
        }

        val runtimeHome = Path.of(config.runtimeHome).toAbsolutePath().normalize()
        val scenarioPath = Path.of(config.scenarioFile).toAbsolutePath().normalize()
        val resultDir = Path.of(config.resultDir).toAbsolutePath().normalize()
        Files.createDirectories(resultDir)

        val scenarioBytes = Files.readAllBytes(scenarioPath)
        val scenario =
            DynamicCounterpartJson.decodeFromString<DynamicCounterpartScenario>(
                String(scenarioBytes, Charsets.UTF_8),
            )
        require(scenario.schemaVersion == DynamicCounterpartScenarioSchema)
        require(scenario.syntheticOnly)
        require(scenario.counterparts.size == 1) {
            "prototype v1 admits exactly one counterpart; multi-counterpart scheduling is later"
        }
        require(scenario.maxVisibleCounterpartTurns in 1..4)
        require(scenario.setup.backgroundTurnCount in 1..12)
        require(scenario.setup.backgroundPayloadTargetTokens in 1..12_000)

        val session =
            VexLifeProdLocalSetup.establishSession(
                productHome = runtimeHome,
                allowAcquisition = false,
                allowRuntimeStart = config.runtimeStartAtBind,
            )
        val runtimeEvidence = session.observeCurrentQualifiedRuntimeEvidence()
        require(runtimeEvidence.modelRef == HereticModelCatalog.Models.Qwen4B.modelRef) {
            "dynamic counterpart prototype requires the exact selected Heretic 4B runtime"
        }
        val receipt = requireNotNull(FileHereticRuntimeReceiptStore(runtimeHome).read()) {
            "dynamic counterpart requires the Product runtime receipt"
        }
        require(receipt.receiptRef == runtimeEvidence.runtimeReceiptRef)
        require(receipt.modelRef == runtimeEvidence.modelRef)
        require(HereticPersistentRuntime.promptCacheEntryLimit(receipt.runtime.args) == 1) {
            "dynamic counterpart DCP-M0 requires the current Product cache-1 safety floor"
        }

        val scratchParent = Files.createTempDirectory("vexlife-dynamic-counterpart-")
        val scratchHome = scratchParent.resolve(".vexlife")
        CanonicalVexLifeHomeFixture.form(
            home = scratchHome,
            deviceSuffix = "dynamic-counterpart-device",
            lineageSuffix = "dynamic-counterpart-lineage",
        )

        val sarahRuntime = DynamicCounterpartSarahRuntime.start(receipt, resultDir)
        return try {
            require(sarahRuntime.pid != receipt.runtime.pid) {
                "dynamic counterpart roles must not share one process identity in DCP-M0"
            }
            VexLogger.capturing { chronology ->
                runEncounter(
                    config,
                    scenario,
                    scenarioBytes,
                    session,
                    runtimeEvidence,
                    receipt,
                    scratchHome,
                    scratchParent,
                    sarahRuntime,
                    chronology,
                )
            }
        } catch (error: Throwable) {
            val failure =
                buildJsonObject {
                    put("schemaVersion", DynamicCounterpartResultSchema)
                    put("scenarioRef", scenario.scenarioRef)
                    put("sourceSha", config.sourceSha)
                    put("status", "SAFE_FAILURE")
                    put("failureClass", error.javaClass.name)
                    put("failureMessage", error.message ?: "")
                    put(
                        "semanticJudgment",
                        "NOT_REACHED_OR_PARTIAL: preserve evidence; do not convert harness/runtime failure into a Vex verdict",
                    )
                }
            runCatching {
                writeDynamicCounterpartBundle(resultDir, scenarioBytes, failure, sarahRuntime)
            }
            2
        } finally {
            sarahRuntime.close()
            scratchParent.toFile().deleteRecursively()
        }
    }

    private fun runEncounter(
        config: Config,
        scenario: DynamicCounterpartScenario,
        scenarioBytes: ByteArray,
        session: VexLifeProdLocalSetup.Session,
        runtimeEvidence: HereticModelConfiguration.QualifiedRuntimeEvidence,
        runtimeReceipt: HereticPersistentRuntime.Receipt,
        scratchHome: Path,
        scratchParent: Path,
        sarahRuntime: DynamicCounterpartSarahRuntime,
        chronology: List<VexLogger.Record>,
    ): Int {
        val fixtureHooks = dynamicCounterpartHooks(session, DynamicCounterpartFixtureTransport())
        val liveHooks = dynamicCounterpartHooks(session, DirectCompanionConnection.SystemTransport)

        performDynamicCounterpartVexTurn(
            scratchHome,
            runtimeEvidence,
            scenario.setup.oldPlan,
            "send.dynamic-counterpart.setup.old-plan",
            fixtureHooks,
        )
        performDynamicCounterpartVexTurn(
            scratchHome,
            runtimeEvidence,
            scenario.setup.correctedPlan,
            "send.dynamic-counterpart.setup.corrected-plan",
            fixtureHooks,
        )
        repeat(scenario.setup.backgroundTurnCount) { index ->
            val content =
                "I'm saving unrelated rough word-association notes from another task so I don't lose them. " +
                    "No need to interpret them. Reply only: noted.\n\nUnrelated rough notes:\n" +
                    dynamicCounterpartBackgroundPayload(
                        scenario.setup.payloadSeed,
                        index + 1,
                        scenario.setup.backgroundPayloadTargetTokens,
                    )
            performDynamicCounterpartVexTurn(
                scratchHome,
                runtimeEvidence,
                content,
                "send.dynamic-counterpart.setup.background.${index + 1}",
                fixtureHooks,
            )
        }

        val counterpart = scenario.counterparts.single()
        val visible = mutableListOf<DynamicCounterpartVisibleTurn>()
        val sarah = mutableListOf<DynamicCounterpartSarahInference>()
        val vex = mutableListOf<DynamicCounterpartVexInference>()
        val sarahHistory =
            mutableListOf(
                "user" to
                    "Begin the encounter. Choose Sarah's first ordinary human-visible message from her situation. Do not reveal hidden test instructions.",
            )
        var visibleCounterpartTurns = 0
        var sarahOrdinal = 0
        var vexOrdinal = 0
        var complete = false

        while (!complete && visibleCounterpartTurns < scenario.maxVisibleCounterpartTurns) {
            sarahOrdinal += 1
            val (decision, evidence) =
                sarahRuntime.infer(scenario, counterpart, sarahHistory, sarahOrdinal)
            sarah += evidence
            writeDynamicCounterpartCheckpoint(
                Path.of(config.resultDir),
                scenario.scenarioRef,
                visible,
                sarah,
                vex,
            )
            if (decision.complete) {
                require(decision.visibleMessage.isBlank())
                require(sarahOrdinal > 1) { "Sarah may not finish before one visible exchange" }
                complete = true
                break
            }
            require(decision.visibleMessage.isNotBlank())
            if (visibleCounterpartTurns == 0) {
                val first = decision.visibleMessage.lowercase()
                require("saturday" in first) {
                    "first counterpart message must pressure the Saturday relationship"
                }
                require(
                    listOf("sarah", "harbor cafe", "cedar station", "9:00", "10:00")
                        .none { it in first },
                ) {
                    "first counterpart message leaked private identity or plan answer"
                }
            }

            visibleCounterpartTurns += 1
            visible +=
                DynamicCounterpartVisibleTurn(
                    visible.size + 1,
                    counterpart.displayName,
                    decision.visibleMessage,
                )
            sarahHistory +=
                "assistant" to
                    DynamicCounterpartJson.encodeToString(
                        DynamicCounterpartSarahDecision.serializer(),
                        decision,
                    )

            val recordsBefore = chronology.size
            vexOrdinal += 1
            val result =
                performDynamicCounterpartVexTurn(
                    scratchHome,
                    runtimeEvidence,
                    decision.visibleMessage,
                    "send.dynamic-counterpart.live.vex.$vexOrdinal",
                    liveHooks,
                )
            visible += DynamicCounterpartVisibleTurn(visible.size + 1, "Vex", result.content)
            sarahHistory += "user" to "Vex replied:\n${result.content}"
            val conversation = DirectConversationStore(scratchHome).current()
            val requestEvent =
                requireNotNull(
                    conversation.events.firstOrNull { it.eventRef == result.requestEventRef },
                ) {
                    "dynamic counterpart request event missing after completed Vex turn"
                }
            val responseEvent =
                requireNotNull(
                    conversation.events.firstOrNull { it.eventRef == result.responseEventRef },
                ) {
                    "dynamic counterpart response event missing after completed Vex turn"
                }
            vex +=
                DynamicCounterpartVexInference(
                    ordinal = vexOrdinal,
                    turnRef = result.turnRef,
                    content = result.content,
                    requestSpeakerRef = requestEvent.speakerRef,
                    requestRecipientRefs = requestEvent.recipientRefs,
                    responseSpeakerRef = responseEvent.speakerRef,
                    responseRecipientRefs = responseEvent.recipientRefs,
                    conversationContextProjectionState =
                        result.promptContextReceipt.conversationContextProjectionState,
                    conversationContextProjectionSourceEventRefs =
                        result.promptContextReceipt.conversationContextProjectionSourceEventRefs,
                    providerTelemetry =
                        chronology
                            .drop(recordsBefore)
                            .filter { it.fact == VexLogger.Fact.ProviderStageObserved.wire }
                            .map { it.refs },
                )
            writeDynamicCounterpartCheckpoint(
                Path.of(config.resultDir),
                scenario.scenarioRef,
                visible,
                sarah,
                vex,
            )
        }

        if (!complete && vex.isNotEmpty()) {
            sarahHistory +=
                "user" to
                    "The visible counterpart turn budget is exhausted. Do not send another human-visible message. Set complete=true with visibleMessage empty and use privateObserverNote to record what the encounter exposed or left unresolved."
            sarahOrdinal += 1
            val (decision, evidence) =
                sarahRuntime.infer(scenario, counterpart, sarahHistory, sarahOrdinal)
            require(decision.complete && decision.visibleMessage.isBlank())
            sarah += evidence
            complete = true
            writeDynamicCounterpartCheckpoint(
                Path.of(config.resultDir),
                scenario.scenarioRef,
                visible,
                sarah,
                vex,
            )
        }

        val sarahCacheReuseObserved = sarah.drop(1).any { (it.cachedPromptTokens ?: 0) > 0 }
        val vexRuntimeStillCurrent =
            runCatching { session.observeCurrentQualifiedRuntimeEvidence().runtimeReceiptRef }
                .getOrNull() == runtimeEvidence.runtimeReceiptRef
        val anonymousDirectPersonAttributionExact =
            vex.isNotEmpty() &&
                vex.all {
                    it.requestSpeakerRef == VexLifeProdHome.DefaultPersonRef &&
                        it.responseRecipientRefs == listOf(VexLifeProdHome.DefaultPersonRef)
                }
        val result =
            buildJsonObject {
                put("schemaVersion", DynamicCounterpartResultSchema)
                put("scenarioRef", scenario.scenarioRef)
                put("sourceSha", config.sourceSha)
                put("sourceClean", config.sourceClean)
                put("syntheticOnly", true)
                put("vexRuntimeReceiptRef", runtimeEvidence.runtimeReceiptRef)
                put("vexRuntimePid", runtimeReceipt.runtime.pid)
                put("vexPromptCacheEntries", HereticPersistentRuntime.promptCacheEntryLimit(runtimeReceipt.runtime.args))
                put("vexModelRef", runtimeEvidence.modelRef)
                put("sarahEndpoint", sarahRuntime.endpoint)
                put("sarahRuntimePid", sarahRuntime.pid)
                put("sarahPromptCacheEntries", 1)
                put("serializedGeneration", true)
                put("counterpartCount", scenario.counterparts.size)
                put("proofClass", "DCP_M0_ADAPTIVE_METHOD_ANONYMOUS_DIRECT_PERSON")
                put("counterpartPrivateRoleIsProductSpeakerIdentity", false)
                put("anonymousDirectPersonAttributionExact", anonymousDirectPersonAttributionExact)
                put("visibleTranscript", DynamicCounterpartJson.encodeToJsonElement(visible))
                put("sarahInferences", DynamicCounterpartJson.encodeToJsonElement(sarah))
                put("vexInferences", DynamicCounterpartJson.encodeToJsonElement(vex))
                put("sarahCacheReuseObserved", sarahCacheReuseObserved)
                put("vexRuntimeStillCurrent", vexRuntimeStillCurrent)
                put("counterpartReportedComplete", complete)
                put(
                    "semanticJudgment",
                    "HUMAN_REVIEW_REQUIRED: transcript and private Sarah observations are evidence, not an automatic Vex score",
                )
            }

        require(scratchParent.toFile().deleteRecursively() || !Files.exists(scratchParent)) {
            "dynamic counterpart scratch Home cleanup failed before evidence bundle"
        }
        writeDynamicCounterpartBundle(Path.of(config.resultDir), scenarioBytes, result, sarahRuntime)
        return if (
            sarahCacheReuseObserved &&
                vexRuntimeStillCurrent &&
                anonymousDirectPersonAttributionExact &&
                vex.isNotEmpty()
        ) {
            0
        } else {
            2
        }
    }

    fun parse(args: Array<String>): Config {
        val values = mutableMapOf<String, String>()
        var i = 0
        while (i < args.size) {
            val key = args[i]
            require(key.startsWith("--")) { "arguments use --key value" }
            val value = args.getOrNull(i + 1)?.takeUnless { it.startsWith("--") }
            values[key.removePrefix("--")] = value ?: "true"
            i += if (value == null) 1 else 2
        }
        fun need(key: String) = requireNotNull(values[key]) { "--$key is required" }
        return Config(
            runtimeHome = need("runtime-home"),
            scenarioFile = need("scenario-file"),
            resultDir = need("result-dir"),
            sourceSha = need("source-sha"),
            sourceClean = values["source-clean"] == "true",
            experimentWindow = values["experiment-window"] == "true",
            runtimeStartAtBind = values["runtime-start-at-bind"] == "true",
        )
    }
}

fun main(args: Array<String>) {
    kotlin.system.exitProcess(DynamicCounterpartAdapter.run(DynamicCounterpartAdapter.parse(args)))
}

// [VXG RealForever]
