package vexlife.harness

import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import vexlife.capability.VexLifeCapabilityRequest
import vexlife.companion.DirectCompanionConnection
import vexlife.companion.DirectCompanionTurn
import vexlife.companion.DirectPromptContext
import vexlife.companion.DirectTurnContextStore
import vexlife.companion.DirectWorkingContinuity
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.local.VexLifeProdLocalSetup
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticProductIdentity
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeState

/**
 * Claude lane's adapter into the shared VexWork continuity-pressure harness (#310).
 *
 * It lives in test scope on purpose: it never ships in the Product. It drives the real
 * [DirectCompanionTurn] — Conversation, prompt residency + overlap tail, Working Continuity,
 * capability frame, revisit bridge and Turn Context evidence — against a freshly formed synthetic
 * scratch Home, and emits `vexlife.continuity-pressure-result/v1` plus the experiment ZIP.
 *
 * ```
 * projection  deterministic fixture provider; no model; real owners      -> result + ZIP
 * analyze     re-derives summary + assertions from a projection's turns  -> analysis.json
 * pressure / probe / quick / full  need the selected local model on the host
 *             -> HELD here: this revision does not bind a scratch-Home runtime
 * ```
 *
 * The fixture provider decides nothing semantic: it echoes "Reply only: X", and on probe turns it
 * asks revisit once with the probe's own content words. Projection assertions therefore judge
 * source availability to the prompt (resident or revisited), never answer quality.
 */
object ContinuityPressureAdapter {
    const val AdapterRef = "adapter.vexlife-prod.claude-c20.continuity-pressure/v1"
    const val ResultSchema = "vexlife.continuity-pressure-result/v1"
    const val ExtensionKey = "vexlife-prod.claude-c20/v1"
    const val CanonicalScenarioRef = "scenario.vexlife.continuity-pressure.depth-continuity.v1"

    /** Digest of the shared canonical scenario at VexWork #310 head c604191b. */
    const val CanonicalScenarioSha256 =
        "65c81bfdcc0935d261204566bfc05468317c59e962eabceffb9f7e0fca359cc1"

    /**
     * Default experiment budget. With this Product's pressure policy (headroom thresholds at 1/2/3
     * output reserves of 16k) and the 24k capability exchange allowance, 52k is the smallest round
     * budget whose pressure curve crosses CHECKPOINT and ROLLOVER inside the scenario's pressure
     * set instead of starting at ROLLOVER on turn one.
     */
    const val DefaultEffectiveInputTokenBudget = 52_000

    private val json = Json { encodeDefaults = true }
    private val start = Instant.parse("2026-11-16T12:00:00Z")

    enum class Stage {
        projection,
        pressure,
        probe,
        analyze,
        quick,
        full,
    }

    enum class Strategy {
        EFFECTIVE_SELECTION_BUDGET,
        PAYLOAD_PRESSURE,
        NATIVE_LIMIT,
    }

    data class Config(
        val stage: Stage,
        val scenarioRef: String,
        val scenarioFile: Path,
        val resultDir: Path,
        val strategy: Strategy,
        val effectiveInputTokenBudget: Int?,
        val sets: Int?,
        val turnsPerSet: Int?,
        val sourceSha: String,
        /**
         * False when tracked files differed from [sourceSha]: the run is then not exact-head
         * evidence.
         */
        val sourceClean: Boolean,
        val keepScratch: Boolean = false,
    )

    @Serializable
    data class Scenario(
        val schemaVersion: String,
        val scenarioRef: String,
        val label: String,
        val purpose: String,
        val syntheticOnly: Boolean,
        val payloadSeed: Long = 0,
        val sets: List<ScenarioSet>,
    )

    @Serializable
    data class ScenarioSet(val setRef: String, val kind: String, val turns: List<ScenarioTurn>)

    @Serializable
    data class ScenarioTurn(
        val turnRef: String,
        val input: String,
        val tags: List<String>,
        val payloadTargetTokens: Int = 0,
        val expectedSignals: List<String> = emptyList(),
        val assertionRefs: List<String> = emptyList(),
    )

    /**
     * Which formation turn each scenario assertion is about; this adapter's evaluation, recorded.
     */
    private val AssertionAnchors =
        mapOf(
            "assert.cobalt-location" to "formation.01",
            "assert.meridian-7" to "formation.02",
            "assert.tight-means-exact" to "formation.03",
            "assert.river-map" to "formation.04",
            "assert.correction-source" to "formation.03",
        )

    private val KnownRefTags = setOf("ANCHOR", "CORRECTION", "LESSON")

    // ---------------------------------------------------------------- entry

    fun run(config: Config): Int {
        val scenarioBytes = Files.readAllBytes(config.scenarioFile)
        val scenarioSha = sha256(scenarioBytes)
        val scenario = json.decodeFromString<Scenario>(String(scenarioBytes, Charsets.UTF_8))
        require(scenario.scenarioRef == config.scenarioRef) {
            "scenario ref does not match --scenario-ref"
        }
        require(scenario.syntheticOnly) { "only synthetic scenarios may run" }
        Files.createDirectories(config.resultDir)
        return when (config.stage) {
            Stage.projection -> projection(config, scenario, scenarioBytes, scenarioSha)
            Stage.analyze -> analyze(config)
            else -> held(config, scenario, scenarioSha)
        }
    }

    // ---------------------------------------------------------------- projection

    private class Fixture : DirectCompanionConnection.Transport {
        var turn: ScenarioTurn? = null
        val revisitPackets = mutableListOf<String>()

        override fun post(
            call: DirectCompanionConnection.HttpCall
        ): DirectCompanionConnection.HttpResult {
            val messages =
                Json.parseToJsonElement(call.body).jsonObject.getValue("messages").jsonArray
            val last = messages.last().jsonObject.getValue("content").jsonPrimitive.content
            val current = requireNotNull(turn)
            val content =
                when {
                    last.startsWith(VexLifeCapabilityRequest.ResultHeader) -> {
                        revisitPackets += last
                        val state =
                            Regex("\"revisit\":\"([A-Z_]+)\"").find(last)?.groupValues?.get(1)
                        "Fixture answer for ${current.turnRef}: revisit came back ${state ?: "without a revisit state"}."
                    }
                    current.tags.any { it in ProbeTags } -> revisitRequest(current)
                    else ->
                        Regex("Reply only: ([^.]+)\\.").find(current.input)?.groupValues?.get(1)
                            ?: "Fixture answer for ${current.turnRef}."
                }
            val body = buildJsonObject {
                put("model", "default_model")
                put(
                    "choices",
                    Json.parseToJsonElement(
                        """[{"message":{"role":"assistant","content":${JsonPrimitive(content)}}}]"""
                    ),
                )
            }
                .toString()
            return DirectCompanionConnection.HttpResult(status = 200, body = body)
        }

        private fun revisitRequest(turn: ScenarioTurn): String {
            val quoted =
                Regex("'([^']+)'").findAll(turn.input).map { it.groupValues[1].lowercase() }
            val words =
                Regex("[A-Za-z][A-Za-z/-]+")
                    .findAll(turn.input)
                    .map { it.value.lowercase().trim('-', '/') }
                    .filter { it.length >= 5 && it !in Stopwords }
            val terms = (quoted + words).distinct().take(6).toList()
            val queries =
                terms.mapIndexed { i, term -> """{"ref":"w${i + 1}","terms":["$term"]}""" } +
                    if (turn.tags.any { it.startsWith("LESSON") })
                        listOf("""{"ref":"corrections","corrections":true}""")
                    else emptyList()
            return VexLifeCapabilityRequest.RequestHeader +
                "\n" +
                """{"capability":"revisit","action":"gather","arguments":{"queries":[${queries.joinToString(",")}]}}"""
        }
    }

    private val ProbeTags =
        setOf(
            "EXACT_RECALL",
            "PARAPHRASED_RECALL",
            "TOOL_SALIENCE",
            "LESSON_TRANSFER",
            "LESSON_RECALL",
            "HONEST_ABSENCE",
            "CONTEXT_CUSTODY",
            "GROWTH_TRANSFER",
        )

    private val Stopwords =
        setOf(
            "without",
            "where",
            "which",
            "there",
            "their",
            "these",
            "those",
            "would",
            "could",
            "about",
            "after",
            "again",
            "being",
            "every",
            "other",
            "should",
            "while",
            "right",
            "intentionally",
            "needed",
            "actually",
            "available",
            "distinguish",
            "currently",
        )

    private data class Observed(
        val ordinal: Int,
        val setRef: String,
        val scenarioTurn: ScenarioTurn,
        /**
         * Exactly what was sent as the person's message: scenario input plus any synthetic payload.
         */
        val sentContent: String,
        val result: DirectCompanionTurn.Result,
        val revisitSourceRefs: List<String>,
        val revisitPacket: String?,
        val workingDisposition: String?,
    )

    private fun projection(
        config: Config,
        scenario: Scenario,
        scenarioBytes: ByteArray,
        scenarioSha: String,
    ): Int {
        val budget =
            when (config.strategy) {
                Strategy.EFFECTIVE_SELECTION_BUDGET ->
                    config.effectiveInputTokenBudget ?: DefaultEffectiveInputTokenBudget
                else -> null
            }
        val parent = Files.createTempDirectory("vexlife-prod-continuity-pressure-")
        val home = parent.resolve(".vexlife")
        CanonicalVexLifeHomeFixture.form(
            home = home,
            deviceSuffix = "pressure-device",
            lineageSuffix = "pressure-lineage",
        )
        val fixture = Fixture()
        var clock = start
        val hooks =
            DirectCompanionTurn.Hooks(
                transport = fixture,
                now = { clock },
                experimentEffectiveInputTokenBudget = budget,
            )
        val experimentRef =
            "experiment.continuity-pressure.claude-c20." +
                scenarioSha.take(12) +
                "." +
                (budget ?: 0)
        val observed = mutableListOf<Observed>()
        var peakBytes = 0L
        val selectedSets = scenario.sets.take(config.sets ?: scenario.sets.size)
        var ordinal = 0
        try {
            for (set in selectedSets) {
                for (turn in set.turns.take(config.turnsPerSet ?: set.turns.size)) {
                    ordinal += 1
                    clock = clock.plus(Duration.ofMinutes(3))
                    fixture.turn = turn
                    val before = fixture.revisitPackets.size
                    val sent = turn.input + payload(scenario.payloadSeed, turn, ordinal)
                    val result =
                        DirectCompanionTurn.performWithHooks(
                            setup = setup(home),
                            intent =
                                DirectCompanionTurn.Intent(
                                    idempotencyKey = "send.continuity-pressure.${turn.turnRef}",
                                    content = sent,
                                ),
                            hooks = hooks,
                        )
                    val evidence = DirectTurnContextStore(home).read(result.turnRef)
                    val revisitRefs =
                        evidence
                            ?.capabilityTrace
                            ?.entries
                            .orEmpty()
                            .filter { it.handle == "revisit" }
                            .flatMap { it.sourceRefs }
                    observed +=
                        Observed(
                            ordinal = ordinal,
                            setRef = set.setRef,
                            scenarioTurn = turn,
                            sentContent = sent,
                            result = result,
                            revisitSourceRefs = revisitRefs,
                            revisitPacket = fixture.revisitPackets.drop(before).lastOrNull(),
                            workingDisposition =
                                DirectWorkingContinuity.read(
                                        productHome = home,
                                        turnRef = result.turnRef,
                                    )
                                    ?.disposition,
                        )
                    peakBytes = maxOf(peakBytes, directoryBytes(home))
                }
            }
        } catch (error: Exception) {
            System.err.println("projection stopped at ordinal $ordinal: ${error.message}")
            cleanup(parent, config.keepScratch)
            return writeResult(
                config,
                scenario,
                scenarioBytes,
                scenarioSha,
                experimentRef,
                budget,
                observed,
                "ERROR",
                peakBytes,
                deleted = !config.keepScratch,
                failure = error.message,
            )
        }
        val deleted = cleanup(parent, config.keepScratch)
        val turns = turnsJson(observed, budget)
        val status = statusOf(turns)
        return writeResult(
            config,
            scenario,
            scenarioBytes,
            scenarioSha,
            experimentRef,
            budget,
            observed,
            status,
            peakBytes,
            deleted,
            null,
        )
    }

    private fun setup(home: Path) =
        VexLifeProdLocalSetup.Result(
            state = "LOCAL_HERETIC_FOUNDATION_READY",
            productHome = home.toString(),
            homeDisposition = "EXISTING_HOME_PRESERVED",
            modelSelectionDisposition = "BOUND",
            managedPythonDisposition = "REUSED_VERIFIED",
            artifactDisposition = "REUSED_VERIFIED",
            environmentDisposition = "REUSED_VERIFIED",
            runtimeDisposition = "REUSED_EXACT_OWNED_MLX_RUNTIME",
            modelBindingDisposition = "BOUND_CURRENT",
            modelConfigurationState = HereticModelConfiguration.State.BoundProductModel,
            qualifiedRuntimeEvidence =
                HereticModelConfiguration.QualifiedRuntimeEvidence(
                    runtimeReceiptRef = "receipt.vexlife-prod.heretic.continuity-pressure-fixture",
                    runtimeState = HereticRuntimeState.Qualified,
                    modelRef = HereticProductIdentity.ModelRef,
                    modelProfileRef = HereticProductIdentity.ModelProfileRef,
                    artifactRef = HereticProductIdentity.ArtifactSetRef,
                    artifactContentSetSha256 = HereticProductIdentity.ArtifactContentSetSha256,
                    runtimeAdapterRef = HereticProductIdentity.RuntimeAdapterRef,
                    runtimeClass = HereticProductIdentity.RuntimeClass,
                    endpoint = HereticProductIdentity.Endpoint,
                    requestModel = HereticProductIdentity.RequestModel,
                    qualification = HereticRuntimeQualification.qualified(),
                ),
            endpoint = HereticProductIdentity.Endpoint,
            requestModel = HereticProductIdentity.RequestModel,
        )

    /**
     * Deterministic synthetic filler, separated from the probe text; empty when the turn asks none.
     */
    internal fun payload(seed: Long, turn: ScenarioTurn, ordinal: Int): String {
        if (turn.payloadTargetTokens <= 0) return ""
        var state = seed xor (ordinal.toLong() * 0x9E3779B97F4A7C15uL.toLong())
        val words = StringBuilder()
        while (
            DirectPromptContext.estimatedMessageTokens(words.toString()) < turn.payloadTargetTokens
        ) {
            state = state xor (state shl 13)
            state = state xor (state ushr 7)
            state = state xor (state shl 17)
            words.append(PayloadWords[((state ushr 1) % PayloadWords.size).toInt()]).append(' ')
        }
        return "\n\n[SYNTHETIC PAYLOAD seed=$seed turn=${turn.turnRef} targetTokens=${turn.payloadTargetTokens}; " +
            "filler, not part of the experiment's meaning]\n" +
            words.toString().trimEnd() +
            "\n[END SYNTHETIC PAYLOAD]"
    }

    private val PayloadWords =
        listOf(
            "amber",
            "basalt",
            "cinder",
            "dune",
            "ember",
            "fjord",
            "glacier",
            "harbor",
            "isthmus",
            "juniper",
            "kelp",
            "lagoon",
            "mesa",
            "nimbus",
            "obsidian",
            "prairie",
            "quartz",
            "ravine",
            "sierra",
            "tundra",
            "umber",
            "valley",
            "willow",
            "xenon",
            "yarrow",
            "zephyr",
            "alder",
            "birch",
            "cobble",
            "delta",
            "estuary",
            "fern",
        )

    // ---------------------------------------------------------------- turns + residency

    private fun turnsJson(observed: List<Observed>, budget: Int?): List<JsonObject> {
        val known = observed.filter { o -> o.scenarioTurn.tags.any { it in KnownRefTags } }
        return observed.map { o ->
            val receipt = o.result.promptContextReceipt
            val resident = receipt.selectedConversationEventRefs.toSet()
            val hydrated = o.revisitSourceRefs.toSet()
            val residency =
                known
                    .filter { it.ordinal < o.ordinal }
                    .map { k ->
                        val refs = listOf(k.result.requestEventRef, k.result.responseEventRef)
                        val state =
                            when {
                                refs.all { it in resident } -> "PROMPT_RESIDENT"
                                refs.any { it in hydrated } -> "HYDRATED_THIS_TURN"
                                else -> "OMITTED_BUDGET"
                            }
                        buildJsonObject {
                            put("ref", "${k.scenarioTurn.turnRef}:${k.result.turnRef}")
                            put("state", state)
                            put("evidenceRefs", json.encodeToJsonElement(refs + receipt.receiptRef))
                        }
                    }
            val assertions =
                o.scenarioTurn.assertionRefs.map { ref ->
                    assertion(ref, o, observed, resident, hydrated)
                }
            val snapshot = receipt.capabilityFrameSnapshot
            buildJsonObject {
                put("ordinal", o.ordinal)
                put("setRef", o.setRef)
                put("scenarioTurnRef", o.scenarioTurn.turnRef)
                put("turnRef", o.result.turnRef)
                put("requestSha256", sha256(o.sentContent.toByteArray()))
                put("responseSha256", sha256(o.result.content.toByteArray()))
                put("responseText", o.result.content)
                put("conversationHeadSha256", o.result.currentConversationHeadSha256)
                put(
                    "prompt",
                    buildJsonObject {
                        put("contextMode", receipt.contextMode)
                        put(
                            "selectedConversationEventRefs",
                            json.encodeToJsonElement(receipt.selectedConversationEventRefs),
                        )
                        put("providerMessageSetSha256", receipt.providerMessageSetSha256)
                        put(
                            "providerMaterializedInputTokenEstimate",
                            receipt.providerMaterializedInputTokenEstimate,
                        )
                        put("ordinaryInputTokenBudget", DirectPromptContext.InputTokenBudget)
                        put(
                            "effectiveInputTokenBudget",
                            receipt.experimentEffectiveInputTokenBudget,
                        )
                        put("hardTokenLimit", receipt.hardTokenLimit)
                        put("priorCompletedExchangeCount", receipt.priorCompletedExchangeCount)
                        put("memoryIncluded", receipt.memoryIncluded)
                        put("memoryContextRef", receipt.memoryContextRef)
                        put("memoryCurrentness", null as String?)
                        put("workingContinuityIncluded", receipt.workingContinuityIncluded)
                        put("workingContinuityContextRef", receipt.workingContinuityContextRef)
                        put(
                            "workingContinuityCheckpointRef",
                            receipt.workingContinuityCheckpointRef,
                        )
                        put("capabilityFrameIncluded", receipt.capabilityFrameIncluded)
                        put(
                            "availableCapabilityRefs",
                            json.encodeToJsonElement(snapshot?.availableCapabilityRefs.orEmpty()),
                        )
                        put(
                            "heldCapabilityRefs",
                            json.encodeToJsonElement(snapshot?.heldCapabilityRefs.orEmpty()),
                        )
                        put(
                            "unknownCapabilityRefs",
                            json.encodeToJsonElement(snapshot?.unknownCapabilityRefs.orEmpty()),
                        )
                        put(
                            "omittedCapabilityRefs",
                            json.encodeToJsonElement(snapshot?.omittedCapabilityRefs.orEmpty()),
                        )
                    },
                )
                put(
                    "capabilityUses",
                    json.encodeToJsonElement(
                        o.result.capabilityUses.map {
                            buildJsonObject {
                                put("handle", it.handle)
                                put("action", it.action)
                                put("target", it.target)
                                put("state", it.state.name)
                            }
                        }
                    ),
                )
                put("residency", json.encodeToJsonElement(residency))
                put("assertions", json.encodeToJsonElement(assertions))
                put(
                    "implementationExtensions",
                    buildJsonObject {
                        put(
                            ExtensionKey,
                            buildJsonObject {
                                put(
                                    "overlapTail",
                                    receipt.overlapTail?.let { json.encodeToJsonElement(it) }
                                        ?: JsonNull,
                                )
                                put("workingContinuityDisposition", o.workingDisposition)
                                put(
                                    "revisitSourceRefs",
                                    json.encodeToJsonElement(o.revisitSourceRefs),
                                )
                                put(
                                    "revisitState",
                                    o.revisitPacket?.let {
                                        Regex("\"revisit\":\"([A-Z_]+)\"")
                                            .find(it)
                                            ?.groupValues
                                            ?.get(1)
                                    },
                                )
                                put(
                                    "revisitFrontierLine",
                                    o.revisitPacket?.lineSequence()?.firstOrNull {
                                        it.startsWith("Not returned for room")
                                    },
                                )
                                put(
                                    "inputTokenBudgetAfterExchangeAllowance",
                                    receipt.inputTokenBudget,
                                )
                                put(
                                    "capabilityExchangeAllowanceTokens",
                                    receipt.capabilityExchangeAllowanceTokens,
                                )
                                put("provider", "DETERMINISTIC_FIXTURE")
                            },
                        )
                    },
                )
            }
        }
    }

    private fun assertion(
        ref: String,
        o: Observed,
        observed: List<Observed>,
        resident: Set<String>,
        hydrated: Set<String>,
    ): JsonObject {
        val anchorRef = AssertionAnchors[ref]
        val (state, detail, evidence) =
            when {
                anchorRef != null -> {
                    val anchor = observed.firstOrNull { it.scenarioTurn.turnRef == anchorRef }
                    if (anchor == null) {
                        Triple(
                            "UNKNOWN",
                            "anchor $anchorRef was not run in this configuration",
                            emptyList(),
                        )
                    } else {
                        val refs =
                            listOf(anchor.result.requestEventRef, anchor.result.responseEventRef)
                        val how =
                            when {
                                refs.all { it in resident } -> "PROMPT_RESIDENT"
                                refs.any { it in hydrated } -> "HYDRATED_THIS_TURN"
                                else -> null
                            }
                        Triple(
                            if (how != null) "PASS" else "FAIL",
                            "projection: source availability of $anchorRef to this prompt (${how ?: "neither resident nor revisited"}); not answer quality",
                            refs,
                        )
                    }
                }
                ref == "assert.absence" -> {
                    val packet = o.revisitPacket
                    when {
                        packet == null ->
                            Triple(
                                "UNKNOWN",
                                "projection: no revisit packet on this probe",
                                emptyList(),
                            )
                        Regex("Query \"w\\d+\" found nothing").containsMatchIn(packet) ->
                            Triple(
                                "PASS",
                                "projection: revisit reported that distinctive probe terms found nothing, and named not-found as not never-happened",
                                o.revisitSourceRefs,
                            )
                        else ->
                            Triple(
                                "UNKNOWN",
                                "projection: every probe term matched something; invention can only be judged on a live answer",
                                o.revisitSourceRefs,
                            )
                    }
                }
                else ->
                    Triple(
                        "UNKNOWN",
                        "no evaluation for this assertion in this adapter revision",
                        emptyList(),
                    )
            }
        return buildJsonObject {
            put("assertionRef", ref)
            put("state", state)
            put("evidenceRefs", json.encodeToJsonElement(evidence))
            put("detail", detail)
        }
    }

    // ---------------------------------------------------------------- summary + analysis

    internal fun summary(turns: List<JsonObject>): JsonObject {
        fun prompt(t: JsonObject) = t.getValue("prompt").jsonObject
        fun ext(t: JsonObject) =
            t.getValue("implementationExtensions").jsonObject.getValue(ExtensionKey).jsonObject
        fun selected(t: JsonObject) =
            prompt(t).getValue("selectedConversationEventRefs").jsonArray.map {
                it.jsonPrimitive.content
            }
        fun ordinal(t: JsonObject) = t.getValue("ordinal").jsonPrimitive.content.toInt()
        val omission = turns.firstOrNull { t ->
            val prior =
                prompt(t)["priorCompletedExchangeCount"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            selected(t).size / 2 < prior
        }
        val rollover = turns.firstOrNull {
            ext(it)["workingContinuityDisposition"]?.jsonPrimitive?.content == "ROLLOVER_COMMITTED"
        }
        val hydration = turns.firstOrNull {
            ext(it).getValue("revisitSourceRefs").jsonArray.isNotEmpty()
        }
        val successorIndexes =
            turns.indices.filter {
                prompt(turns[it])
                    .getValue("contextMode")
                    .jsonPrimitive
                    .content
                    .contains("WORKING_CHECKPOINT_SUCCESSOR")
            }
        val oscillations = successorIndexes.mapNotNull { i ->
            val next = turns.getOrNull(i + 1) ?: return@mapNotNull null
            if (
                prompt(next)
                    .getValue("contextMode")
                    .jsonPrimitive
                    .content
                    .contains("WORKING_CHECKPOINT_SUCCESSOR")
            )
                return@mapNotNull null
            // The next ordinary turn reaches back past where the successor's exact tail began:
            // its newest-first selection holds events older than that tail's first event.
            val tailStart = selected(turns[i]).firstOrNull() ?: return@mapNotNull null
            if (selected(next).indexOf(tailStart) > 0) ordinal(next) else null
        }
        fun assertionState(ref: String) =
            turns
                .flatMap { it.getValue("assertions").jsonArray }
                .map { it.jsonObject }
                .firstOrNull { it.getValue("assertionRef").jsonPrimitive.content == ref }
                ?.getValue("state")
                ?.jsonPrimitive
                ?.content
        fun bool(state: String?) =
            when (state) {
                "PASS" -> true
                "FAIL" -> false
                else -> null
            }
        val capabilityCounts =
            turns
                .flatMap { it.getValue("capabilityUses").jsonArray }
                .groupingBy { it.jsonObject.getValue("handle").jsonPrimitive.content }
                .eachCount()
        return buildJsonObject {
            put("turnCount", turns.size)
            put("firstConversationOmissionTurn", omission?.let(::ordinal))
            put("firstWorkingRolloverTurn", rollover?.let(::ordinal))
            put("firstHydrationTurn", hydration?.let(::ordinal))
            put(
                "postRolloverReplayOscillation",
                if (successorIndexes.isEmpty()) null else oscillations.isNotEmpty(),
            )
            put("knownLessonReuse", bool(assertionState("assert.tight-means-exact")))
            put("honestAbsence", bool(assertionState("assert.absence")))
            put(
                ExtensionKey,
                buildJsonObject {
                    put(
                        "successorTurns",
                        json.encodeToJsonElement(successorIndexes.map { ordinal(turns[it]) }),
                    )
                    put("oscillationTurns", json.encodeToJsonElement(oscillations))
                    put("capabilityUseCounts", json.encodeToJsonElement(capabilityCounts))
                    put(
                        "selectedConversationCountByTurn",
                        json.encodeToJsonElement(turns.map { selected(it).size }),
                    )
                    put(
                        "assertionEvaluation",
                        "source availability to the prompt (resident or revisited); answer quality is a live-stage question",
                    )
                },
            )
        }
    }

    private fun statusOf(turns: List<JsonObject>): String {
        val states =
            turns
                .flatMap { it.getValue("assertions").jsonArray }
                .map { it.jsonObject.getValue("state").jsonPrimitive.content }
        return when {
            states.isEmpty() -> "PARTIAL"
            states.all { it == "PASS" } -> "PASS"
            states.none { it == "PASS" } -> "FAIL"
            else -> "PARTIAL"
        }
    }

    private fun analyze(config: Config): Int {
        val resultFile = config.resultDir.resolve("result.json")
        require(Files.exists(resultFile)) {
            "analyze needs a result.json from a projection in --result-dir"
        }
        val result = Json.parseToJsonElement(Files.readString(resultFile)).jsonObject
        val turns = result.getValue("turns").jsonArray.map { it.jsonObject }
        val derived = summary(turns)
        val recorded = result.getValue("summary")
        val analysis = buildJsonObject {
            put("schemaVersion", "vexlife-prod.claude-c20.continuity-pressure-analysis/v1")
            put("experimentRef", result.getValue("experimentRef"))
            put("summary", derived)
            put("matchesRecordedSummary", derived == recorded)
            put("status", statusOf(turns))
        }
        Files.writeString(config.resultDir.resolve("analysis.json"), analysis.toString() + "\n")
        println(analysis)
        return if (derived == recorded) 0 else 1
    }

    // ---------------------------------------------------------------- held live stages

    private fun held(config: Config, scenario: Scenario, scenarioSha: String): Int {
        val report = buildJsonObject {
            put("state", "HELD")
            put("stage", config.stage.name)
            put("executionClass", "LIVE_MODEL")
            put("effectClass", "HOST_SEMANTIC_MODEL_RUNTIME")
            put("scenarioRef", scenario.scenarioRef)
            put("scenarioSha256", scenarioSha)
            put(
                "reason",
                "This adapter revision does not bind a qualified local model runtime to a scratch Home. " +
                    "A live stage must form the scratch Home and borrow the current qualified runtime " +
                    "through VexLifeProdLocalSetup on the host, then use the same send loop with the " +
                    "system transport. Nothing was run and no Home was touched.",
            )
        }
        Files.writeString(config.resultDir.resolve("held.json"), report.toString() + "\n")
        System.err.println(report)
        return 3
    }

    // ---------------------------------------------------------------- result + ZIP

    private fun writeResult(
        config: Config,
        scenario: Scenario,
        scenarioBytes: ByteArray,
        scenarioSha: String,
        experimentRef: String,
        budget: Int?,
        observed: List<Observed>,
        status: String,
        peakBytes: Long,
        deleted: Boolean,
        failure: String?,
    ): Int {
        val turns = turnsJson(observed, budget)
        val summary = summary(turns)
        val files = linkedMapOf<String, ByteArray>()
        fun jsonl(rows: List<JsonElement>) =
            rows.joinToString("") { it.toString() + "\n" }.toByteArray()
        val runRef = "run." + experimentRef.removePrefix("experiment.")
        val config0 = buildJsonObject {
            put("stage", config.stage.name)
            put("pressureStrategy", config.strategy.name)
            put("effectiveInputTokenBudget", budget)
            put("sets", config.sets)
            put("turnsPerSet", config.turnsPerSet)
            put("scenarioSha256", scenarioSha)
            put("scenarioIsCanonicalShared", scenarioSha == CanonicalScenarioSha256)
            put("sourceSha", config.sourceSha)
            put("sourceTreeClean", config.sourceClean)
        }
        files["config.json"] = config0.toString().toByteArray()
        files["scenario.json"] = scenarioBytes
        files["turns.jsonl"] = jsonl(turns)
        files["context-residency.jsonl"] =
            jsonl(
                turns.flatMap { t ->
                    t.getValue("residency").jsonArray.map {
                        buildJsonObject {
                            put("ordinal", t.getValue("ordinal"))
                            put("row", it)
                        }
                    }
                }
            )
        files["capability-uses.jsonl"] =
            jsonl(
                turns.flatMap { t ->
                    t.getValue("capabilityUses").jsonArray.map {
                        buildJsonObject {
                            put("ordinal", t.getValue("ordinal"))
                            put("use", it)
                        }
                    }
                }
            )
        files["assertions.json"] =
            json
                .encodeToJsonElement(turns.flatMap { it.getValue("assertions").jsonArray })
                .toString()
                .toByteArray()
        files["summary.json"] = summary.toString().toByteArray()
        files["implementation/adapter-report.json"] =
            buildJsonObject {
                put("adapterRef", AdapterRef)
                put(
                    "provider",
                    "DETERMINISTIC_FIXTURE: echoes 'Reply only: X'; on probe turns asks revisit once with the probe's own content words",
                )
                put(
                    "owners",
                    json.encodeToJsonElement(
                        listOf(
                            "DirectCompanionTurn",
                            "DirectConversationStore",
                            "DirectPromptContext",
                            "DirectContinuityOverlapTail",
                            "VexLifeWorkingContinuityOwner",
                            "VexLifeCapabilityBridge",
                            "VexLifeContextRevisit",
                            "DirectTurnContextStore",
                        )
                    ),
                )
                put("assertionAnchors", json.encodeToJsonElement(AssertionAnchors))
                put(
                    "pressurePolicyNote",
                    "Working Continuity thresholds are absolute multiples of the 16k output reserve; the effective budget changes headroom, not the thresholds.",
                )
                put("failure", failure)
            }
                .toString()
                .toByteArray()
        val artifacts = buildJsonObject {
            files.forEach { (name, bytes) ->
                put(
                    name,
                    buildJsonObject {
                        put("sha256", sha256(bytes))
                        put("bytes", bytes.size)
                    },
                )
            }
        }
        val result = buildJsonObject {
            put("schemaVersion", ResultSchema)
            put("experimentRef", experimentRef)
            put("runRef", runRef)
            put("scenarioRef", scenario.scenarioRef)
            put("adapterRef", AdapterRef)
            put("executionClass", "DETERMINISTIC_PROJECTION")
            put("status", status)
            put(
                "source",
                buildJsonObject {
                    put("repositoryRef", "VexGPT/VexLife-Prod")
                    put("sourceSha", config.sourceSha)
                    put("modelRef", null as String?)
                    put("runtimeRef", null as String?)
                },
            )
            put(
                "pressure",
                buildJsonObject {
                    put("strategy", config.strategy.name)
                    put("ordinaryInputTokenBudget", DirectPromptContext.InputTokenBudget)
                    put("hardTokenLimit", DirectPromptContext.HardTokenLimit)
                    put("effectiveInputTokenBudget", budget)
                    put("payloadSeed", scenario.payloadSeed)
                },
            )
            put(
                "privacy",
                buildJsonObject {
                    put("syntheticOnly", true)
                    put("humanPrivateContentIncluded", false)
                    put("rawReasoningIncluded", false)
                },
            )
            put(
                "scratch",
                buildJsonObject {
                    put("isolationClass", "TEMP_DIRECTORY_SYNTHETIC_CANONICAL_HOME_FIXTURE")
                    put("deletedAfterBundle", deleted)
                    put("peakBytes", peakBytes)
                },
            )
            put("turns", json.encodeToJsonElement(turns))
            put("summary", summary)
            put("artifacts", artifacts)
        }
        files["result.json"] = result.toString().toByteArray()
        val manifest = buildJsonObject {
            put("schemaVersion", "vexlife-prod.claude-c20.continuity-pressure-zip/v1")
            put("experimentRef", experimentRef)
            put("files", buildJsonObject { files.forEach { (n, b) -> put(n, sha256(b)) } })
        }
        val zipped = zip(linkedMapOf("manifest.json" to manifest.toString().toByteArray()) + files)
        Files.write(config.resultDir.resolve("result.json"), files.getValue("result.json"))
        val zipPath = config.resultDir.resolve("continuity-pressure-$experimentRef.zip")
        Files.write(zipPath, zipped)
        println(
            buildJsonObject {
                put("state", status)
                put("experimentRef", experimentRef)
                put("zip", zipPath.toString())
                put("zipSha256", sha256(zipped))
                put("resultSha256", sha256(files.getValue("result.json")))
                put("summary", summary)
            }
        )
        return if (status == "ERROR") 2 else 0
    }

    private fun zip(entries: Map<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            entries.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name).apply { time = 0L })
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    private fun cleanup(parent: Path, keep: Boolean): Boolean {
        if (keep) return false
        parent.toFile().deleteRecursively()
        return !Files.exists(parent)
    }

    private fun directoryBytes(path: Path): Long =
        Files.walk(path).use { s ->
            s.filter { Files.isRegularFile(it) }.mapToLong { Files.size(it) }.sum()
        }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    // ---------------------------------------------------------------- CLI

    fun parse(args: Array<String>): Config {
        val map = mutableMapOf<String, String>()
        var i = 0
        while (i < args.size) {
            val key = args[i]
            require(key.startsWith("--")) { "arguments are --key value: $key" }
            val value = args.getOrNull(i + 1)?.takeUnless { it.startsWith("--") }
            map[key.removePrefix("--")] = value ?: "true"
            i += if (value == null) 1 else 2
        }
        fun need(k: String) = requireNotNull(map[k]) { "--$k is required" }
        return Config(
            stage = Stage.valueOf(need("stage")),
            scenarioRef = need("scenario-ref"),
            scenarioFile = Path.of(need("scenario-file")).toAbsolutePath(),
            resultDir = Path.of(need("result-dir")).toAbsolutePath(),
            strategy = Strategy.valueOf(map["pressure-strategy"] ?: "EFFECTIVE_SELECTION_BUDGET"),
            effectiveInputTokenBudget = map["effective-input-token-budget"]?.toInt(),
            sets = map["sets"]?.toInt(),
            turnsPerSet = map["turns-per-set"]?.toInt(),
            sourceSha =
                requireNotNull(map["source-sha"]?.takeIf { it.matches(Regex("[0-9a-f]{40}")) }) {
                    "--source-sha must be an exact commit"
                },
            sourceClean = map["source-clean"] == "true",
            keepScratch = map["keep-scratch"] == "true",
        )
    }
}

fun main(args: Array<String>) {
    val code = ContinuityPressureAdapter.run(ContinuityPressureAdapter.parse(args))
    kotlin.system.exitProcess(code)
}

// [VXG RealForever]
