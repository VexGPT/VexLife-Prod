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
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import vexlife.capability.VexLifeCapabilityRequest
import vexlife.contract.VexDurableContractFile
import vexlife.filesystem.VexOwnedPath
import vexlife.companion.DirectCompanionConnection
import vexlife.companion.DirectCompanionTurn
import vexlife.companion.DirectPromptContext
import vexlife.companion.DirectTurnContextStore
import vexlife.companion.DirectWorkingContinuity
import vexlife.continuity.VexLifeContextRevisit
import vexlife.conversation.DirectConversationStore
import vexlife.continuity.VexLifeWorkingContinuityOwner
import vexlife.continuity.VexLifeWorkingContinuity
import vexlife.person.VexLifePersonSettings
import vexlife.person.VexLifePersonSettingsStore
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.home.VexLifeProdHomeFiles
import vexlife.local.VexLifeProdLocalSetup
import vexlife.logging.VexLogger
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticProductIdentity
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeState
import vexlife.model.heretic.runtime.FileHereticModelConfigurationStore
import vexlife.model.heretic.runtime.FileHereticRuntimeReceiptStore
import vexlife.model.heretic.runtime.HereticPersistentRuntime
import vexlife.model.heretic.runtime.MacHereticRuntimePlatform
import vexlife.model.heretic.runtime.StableVexLifeHereticRuntimeCompatibility

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

    /** Digest of the shared canonical scenario at VexWork #310 head c604191b (unchanged at 0fc67824). */
    const val CanonicalScenarioSha256 =
        "65c81bfdcc0935d261204566bfc05468317c59e962eabceffb9f7e0fca359cc1"

    /** Digest of the shared current-state placement scenario at VexWork #310 head 0fc67824. */
    const val PlacementScenarioSha256 =
        "d9d86dc1195123dc7565c3173d909739e90dc017c3a027b9b2c2edd13c0a72f1"

    private val SharedScenarioDigests = setOf(CanonicalScenarioSha256, PlacementScenarioSha256)

    /**
     * Default experiment budget. This Product's pressure policy puts headroom thresholds at 1/2/3
     * output reserves of 16k, and 24k is kept for capability exchanges. At 52k the run starts at
     * CHECKPOINT_RECOMMENDED and crosses ROLLOVER inside the scenario's pressure set; any budget
     * below about 48k + 24k is at ROLLOVER from turn one.
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
        /**
         * Live stages only: the host's selected Vex Home, observed read-only through the reuse-only
         * setup route to borrow its qualified runtime. Turns still go to a scratch Home.
         */
        val runtimeHome: Path? = null,
        /** Live stages only: the operator affirms a dedicated window, not alongside Victor's own conversation. */
        val experimentWindow: Boolean = false,
        /** Multiplies each turn's synthetic payload; 1 leaves the scenario's own sizes. Recorded. */
        val payloadScale: Int = 1,
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
        val beforeTurnControls: List<Control> = emptyList(),
    )

    /** A host action before the turn; only a canonical person-setting write is admitted. */
    @Serializable data class Control(val controlClass: String, val key: String, val value: String)

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

    private val RouteInDetail = Regex("\\((PROMPT_RESIDENT|HYDRATED_THIS_TURN)\\)")

    // ---------------------------------------------------------------- entry

    fun run(config: Config): Int = run(config, ::hostRuntime)

    /** [bind] borrows the live runtime; the host route by default, replaceable only in tests. */
    internal fun run(config: Config, bind: (Path) -> LiveRuntime): Int {
        val scenarioBytes = Files.readAllBytes(config.scenarioFile)
        val scenarioSha = sha256(scenarioBytes)
        val scenario = json.decodeFromString<Scenario>(String(scenarioBytes, Charsets.UTF_8))
        require(scenario.scenarioRef == config.scenarioRef) {
            "scenario ref does not match --scenario-ref"
        }
        require(scenario.syntheticOnly) { "only synthetic scenarios may run" }
        Files.createDirectories(config.resultDir)
        require(config.payloadScale in 1..16) { "--payload-scale is 1-16" }
        return when (config.stage) {
            Stage.projection -> projection(config, scenario, scenarioBytes, scenarioSha, FixtureProvider())
            Stage.analyze -> analyze(config)
            Stage.quick,
            Stage.full -> live(config, scenario, scenarioBytes, scenarioSha, bind)
            Stage.pressure,
            Stage.probe ->
                held(
                    config,
                    scenario,
                    scenarioSha,
                    "pressure and probe are two halves of one semantic live run over one scratch Home; " +
                        "run --stage quick or --stage full, which carry that Home across both.",
                )
        }
    }

    // ---------------------------------------------------------------- providers

    /** What answers a run's provider calls, and what the run records about them. */
    internal interface Provider {
        val live: Boolean
        val transport: DirectCompanionConnection.Transport
        val evidence: HereticModelConfiguration.QualifiedRuntimeEvidence
        /** Re-observe the same runtime; a live run never recovers or replaces it. */
        fun observe(): HereticModelConfiguration.QualifiedRuntimeEvidence
        /** Every provider call body as sent, in order. */
        val calls: List<JsonObject>
        /** Observed per-call provider telemetry (live only). */
        val telemetry: List<JsonObject>
        fun begin(turn: ScenarioTurn) {}
    }

    internal val FixtureEvidence =
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
        )

    private class FixtureProvider : Provider {
        private val fixture = Fixture()
        override val live = false
        override val transport: DirectCompanionConnection.Transport = fixture
        override val evidence = FixtureEvidence
        override fun observe() = evidence
        override val calls: List<JsonObject>
            get() = fixture.calls
        override val telemetry: List<JsonObject> = emptyList()
        override fun begin(turn: ScenarioTurn) {
            fixture.turn = turn
        }
    }

    /** The host's selected model runtime, borrowed for a live run without lifecycle authority. */
    internal class LiveRuntime(
        val evidence: HereticModelConfiguration.QualifiedRuntimeEvidence,
        val observe: () -> HereticModelConfiguration.QualifiedRuntimeEvidence,
        val transport: DirectCompanionConnection.StreamingTransport,
    )

    /**
     * The ordinary reuse-only route the long-lived service itself takes: no acquisition, no runtime
     * start. On an already set-up Home it observes; currentness is then re-observed read-only for
     * every stage, with no recovery, so an experiment can never restart or replace the runtime.
     */
    private fun bindingFromReceipt(
        authorityRef: String,
        modelRef: String,
        modelPath: String,
        pythonPath: String,
        pythonEnvironmentRoot: String,
    ): HereticPersistentRuntime.Binding =
        HereticPersistentRuntime.Binding(
            schemaVersion = HereticPersistentRuntime.BindingSchema,
            authorityRef = authorityRef,
            modelPath = modelPath,
            pythonPath = pythonPath,
            pythonEnvironmentRoot = pythonEnvironmentRoot,
            implementationIdentity = HereticPersistentRuntime.currentImplementationIdentity(),
            model = HereticModelCatalog.requireEntry(modelRef),
        )

    /**
     * Observe the runtime already bound by the selected Home without entering LocalSetup.
     *
     * LocalSetup's reuse-only route is lifecycle-safe for the Product, but it may still repair an
     * abandoned Conversation writer or currentize the Product runtime binding. A synthetic live
     * experiment has no authority to perform either effect on the person's canonical Home.
     *
     * The harness therefore follows the Home's already-bound runtimeReceiptPath and asks only that
     * rightful runtime owner for exact currentness. Missing/pending/stale binding truth holds the
     * experiment instead of converging or recovering anything.
     */
    private fun observeBoundRuntimeReadOnly(runtimeHome: Path): HereticModelConfiguration.QualifiedRuntimeEvidence {
        val home =
            requireNotNull(VexLifeProdHomeFiles.observeExisting(runtimeHome)) {
                "selected canonical Vex Home is absent"
            }
        val configured =
            FileHereticModelConfigurationStore(runtimeHome).read(home.manifest)
                as? FileHereticModelConfigurationStore.Snapshot.Heretic
                ?: error("selected Vex Home has no bound Product runtime model configuration")
        require(HereticModelConfiguration.isBound(configured.value)) {
            "selected Vex Home runtime model configuration is not already bound"
        }

        val platform = MacHereticRuntimePlatform(productHome = runtimeHome)
        return when (configured.value.runtimeReceiptPath) {
            HereticProductIdentity.StableRuntimeReceiptRelativePath -> {
                val ownedRoot = VexOwnedPath.requireRoot(runtimeHome, "canonical VexLife Home")
                val receipt =
                    requireNotNull(
                        VexDurableContractFile.read<StableVexLifeHereticRuntimeCompatibility.Receipt>(
                            ownedRoot = ownedRoot,
                            path =
                                VexOwnedPath.resolveInside(
                                    ownedRoot,
                                    HereticProductIdentity.StableRuntimeReceiptRelativePath,
                                ),
                        )
                    ) {
                        "selected Vex Home stable runtime receipt is absent"
                    }
                require(receipt.modelRef == configured.value.modelRef) {
                    "stable runtime receipt does not serve the Home's bound model"
                }
                val binding =
                    bindingFromReceipt(
                        authorityRef = receipt.authorityRef,
                        modelRef = receipt.modelRef,
                        modelPath = receipt.modelPath,
                        pythonPath = receipt.pythonPath,
                        pythonEnvironmentRoot = receipt.pythonEnvironmentRoot,
                    )
                requireNotNull(
                    StableVexLifeHereticRuntimeCompatibility.observe(
                        productHome = runtimeHome,
                        binding = binding,
                        platform = platform,
                    )
                ) {
                    "selected stable VexLife runtime is not currently borrowable"
                }.evidence
            }

            HereticProductIdentity.ProductRuntimeReceiptRelativePath -> {
                val store = FileHereticRuntimeReceiptStore(runtimeHome)
                val receipt =
                    requireNotNull(store.read()) {
                        "selected Vex Home Product runtime receipt is absent"
                    }
                require(receipt.modelRef == configured.value.modelRef) {
                    "Product runtime receipt does not serve the Home's bound model"
                }
                HereticPersistentRuntime.observeCurrentQualified(
                    binding =
                        bindingFromReceipt(
                            authorityRef = receipt.authorityRef,
                            modelRef = receipt.modelRef,
                            modelPath = receipt.modelPath,
                            pythonPath = receipt.pythonPath,
                            pythonEnvironmentRoot = receipt.pythonEnvironmentRoot,
                        ),
                    store = store,
                    platform = platform,
                )
            }

            else -> error("selected Vex Home runtime receipt owner is not admitted for read-only borrowing")
        }
    }

    private fun hostRuntime(runtimeHome: Path): LiveRuntime {
        val evidence = observeBoundRuntimeReadOnly(runtimeHome)
        return LiveRuntime(
            evidence = evidence,
            observe = { observeBoundRuntimeReadOnly(runtimeHome) },
            transport = DirectCompanionConnection.SystemTransport,
        )
    }

    /**
     * The live provider: every call goes to the real runtime unchanged; this only watches the SSE
     * lines it already passes through, for the provider's own usage report and the timing of the
     * first token, the first answer token and the end.
     */
    internal class LiveProvider(private val runtime: LiveRuntime) :
        Provider, DirectCompanionConnection.StreamingTransport {
        override val live = true
        override val transport: DirectCompanionConnection.Transport = this
        override val evidence = runtime.evidence
        override fun observe() = runtime.observe()
        override val calls = mutableListOf<JsonObject>()
        override val telemetry = mutableListOf<JsonObject>()

        override fun post(call: DirectCompanionConnection.HttpCall) = runtime.transport.post(call)

        override fun postStreaming(
            call: DirectCompanionConnection.HttpCall,
            onLine: (String) -> Unit,
        ): DirectCompanionConnection.StreamingHttpResult {
            calls += Json.parseToJsonElement(call.body).jsonObject
            val started = System.nanoTime()
            var firstToken: Long? = null
            var firstAnswer: Long? = null
            var usage: JsonObject? = null
            val result =
                runtime.transport.postStreaming(call) { line ->
                    val data = line.removePrefix("data:").trim()
                    if (line.startsWith("data:") && data != "[DONE]") {
                        runCatching { Json.parseToJsonElement(data).jsonObject }
                            .getOrNull()
                            ?.let { event ->
                                (event["usage"] as? JsonObject)?.let { usage = it }
                                val delta =
                                    ((event["choices"] as? kotlinx.serialization.json.JsonArray)
                                            ?.firstOrNull() as? JsonObject)
                                        ?.get("delta") as? JsonObject
                                fun text(key: String) =
                                    (delta?.get(key) as? JsonPrimitive)?.contentOrNull.orEmpty()
                                val reasoning = text("reasoning") + text("reasoning_content")
                                val content = text("content")
                                val now = System.nanoTime()
                                if (firstToken == null && (reasoning.isNotEmpty() || content.isNotEmpty()))
                                    firstToken = now
                                if (firstAnswer == null && content.isNotEmpty()) firstAnswer = now
                            }
                    }
                    onLine(line)
                }
            val ended = System.nanoTime()
            fun ms(at: Long?) = at?.let { (it - started) / 1_000_000 }
            val prompt = (usage?.get("prompt_tokens") as? JsonPrimitive)?.contentOrNull?.toIntOrNull()
            val cached =
                ((usage?.get("prompt_tokens_details") as? JsonObject)?.get("cached_tokens") as? JsonPrimitive)
                    ?.contentOrNull
                    ?.toIntOrNull()
            telemetry +=
                buildJsonObject {
                    put("call", calls.size - 1)
                    put("httpStatus", result.status)
                    put("promptTokens", prompt)
                    put("cachedPromptTokens", cached)
                    put("prefillTokens", if (prompt != null && cached != null) prompt - cached else null)
                    put(
                        "completionTokens",
                        (usage?.get("completion_tokens") as? JsonPrimitive)?.contentOrNull?.toIntOrNull(),
                    )
                    put("usageReported", usage != null)
                    put("firstTokenMs", ms(firstToken))
                    put("firstAnswerMs", ms(firstAnswer))
                    put("totalMs", ms(ended))
                }
            return result
        }
    }

    /** A live stage: bound only with an explicit host Home and a dedicated experiment window. */
    internal fun live(
        config: Config,
        scenario: Scenario,
        scenarioBytes: ByteArray,
        scenarioSha: String,
        bind: (Path) -> LiveRuntime,
    ): Int {
        val runtimeHome = config.runtimeHome
        if (runtimeHome == null || !config.experimentWindow) {
            return held(
                config,
                scenario,
                scenarioSha,
                "a live stage needs --runtime-home <the host's selected Vex Home, observed read-only> " +
                    "and --experiment-window (a dedicated window, not alongside Victor's own conversation). " +
                    "Nothing was run and no Home was touched.",
            )
        }
        val runtime =
            try {
                bind(runtimeHome)
            } catch (error: Throwable) {
                return held(
                    config,
                    scenario,
                    scenarioSha,
                    "the host's qualified runtime could not be observed read-only: " +
                        (error.message ?: error.javaClass.simpleName) +
                        ". Nothing was run and no Home was touched.",
                )
            }
        return projection(config, scenario, scenarioBytes, scenarioSha, LiveProvider(runtime))
    }

    // ---------------------------------------------------------------- projection

    private class Fixture : DirectCompanionConnection.Transport {
        var turn: ScenarioTurn? = null
        val revisitPackets = mutableListOf<String>()

        /** Every provider call body as sent: messages plus template arguments, in order. */
        val calls = mutableListOf<JsonObject>()

        override fun post(
            call: DirectCompanionConnection.HttpCall
        ): DirectCompanionConnection.HttpResult {
            val request = Json.parseToJsonElement(call.body).jsonObject
            calls += request
            val messages = request.getValue("messages").jsonArray
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
                    current.tags.any { it in SettingTags } -> perceivedSetting(messages)
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

        /** What the provider input itself carried as the reply-language setting, and where. */
        private fun perceivedSetting(messages: kotlinx.serialization.json.JsonArray): String {
            val carried = personContextsIn(messages)
            return when (carried.size) {
                0 -> "Fixture: the provider input carried no reply-language setting."
                1 ->
                    carried.single().let { (index, language) ->
                        val role = messages[index].jsonObject.getValue("role").jsonPrimitive.content
                        "Fixture: the provider input carried replyLanguage=$language in message " +
                            "${index + 1} of ${messages.size} ($role)."
                    }
                else -> "Fixture: the provider input carried ${carried.size} reply-language settings."
            }
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
                """{"capability":"continuity","action":"revisit","arguments":{"queries":[${queries.joinToString(",")}]}}"""
        }
    }

    private val SettingTags = setOf("CURRENTNESS", "CONFIGURATION_SWAP", "CONFIGURATION_PERSISTENCE")

    private val PersonContextJson = Regex("\"replyLanguage\":\"([a-zA-Z-]+)\"")

    /**
     * (message index, replyLanguage) of every person-context block VexLife itself carried in a
     * provider call: in the system block or the current-state envelope. Text that merely looks like
     * one inside history or returned source is history, not what VexLife said is current.
     */
    private fun personContextsIn(messages: kotlinx.serialization.json.JsonArray): List<Pair<Int, String>> =
        messages.flatMapIndexed { index, message ->
            val role = message.jsonObject.getValue("role").jsonPrimitive.content
            val content = message.jsonObject.getValue("content").jsonPrimitive.content
            if (role != "system" && !content.startsWith(DirectPromptContext.CurrentStateHeader)) {
                return@flatMapIndexed emptyList()
            }
            content
                .split("VEXLIFE_PERSON_CONTEXT/v1")
                .drop(1)
                .mapNotNull { block -> PersonContextJson.find(block)?.groupValues?.get(1)?.let { index to it } }
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
        /** Every provider call of this turn, as sent (messages + template arguments). */
        val providerCalls: List<JsonObject> = emptyList(),
        /** The canonical PersonSettings reply language when this turn was formed. */
        val canonicalReplyLanguage: String? = null,
        val controls: List<Control> = emptyList(),
        val hostScans: List<VexLifeContextRevisit.HostScan> = emptyList(),
        /** Live only: observed provider telemetry for this turn's calls. */
        val telemetry: List<JsonObject> = emptyList(),
        /** The Product's own chronology records written during this turn. */
        val chronology: List<VexLogger.Record> = emptyList(),
        val wallNanos: Long = 0,
        /** The Working Continuity owner's pressure state for this turn's prompt. */
        val pressureState: String? = null,
        val planState: String? = null,
        val checkpointRef: String? = null,
        val cursorEventRef: String? = null,
    )

    private fun projection(
        config: Config,
        scenario: Scenario,
        scenarioBytes: ByteArray,
        scenarioSha: String,
        provider: Provider,
    ): Int = VexLogger.capturing { chronology -> run(config, scenario, scenarioBytes, scenarioSha, provider, chronology) }

    private fun run(
        config: Config,
        scenario: Scenario,
        scenarioBytes: ByteArray,
        scenarioSha: String,
        provider: Provider,
        chronology: List<VexLogger.Record>,
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
        var clock = start
        val hooks =
            DirectCompanionTurn.Hooks(
                transport = provider.transport,
                // A live run keeps real time; a projection steps a synthetic clock.
                now = { if (provider.live) Instant.now() else clock },
                currentRuntimeEvidence = { provider.observe() },
                reobserveSameRuntimeEvidence = { provider.observe() },
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
                    provider.begin(turn)
                    val callsBefore = provider.calls.size
                    val telemetryBefore = provider.telemetry.size
                    val recordsBefore = chronology.size
                    val startedNanos = System.nanoTime()
                    val settings = VexLifePersonSettingsStore(home)
                    turn.beforeTurnControls.forEach { control ->
                        require(
                            control.controlClass == "PERSON_SETTING_UPDATE" &&
                                control.key == "replyLanguage"
                        ) {
                            "unsupported control ${control.controlClass}/${control.key}"
                        }
                        VexLifePersonSettings.languageName(control.value)
                        settings.write(
                            VexLifePersonSettings.Value(VexLifePersonSettings.Schema, control.value)
                        )
                    }
                    // As the service composes it each turn: read fresh from the canonical owner.
                    val language = settings.read().replyLanguage
                    val sent =
                        turn.input + payload(scenario.payloadSeed, turn, ordinal, config.payloadScale)
                    val result =
                        DirectCompanionTurn.performWithHooks(
                            setup = setup(home, provider.evidence),
                            intent =
                                DirectCompanionTurn.Intent(
                                    idempotencyKey = "send.continuity-pressure.${turn.turnRef}",
                                    content = sent,
                                    personContext =
                                        DirectPromptContext.PersonContext(
                                            replyLanguage = language,
                                            replyLanguageName =
                                                VexLifePersonSettings.languageName(language),
                                            privateConversations = emptyList(),
                                        ),
                                ),
                            hooks = hooks,
                        )
                    val evidence = DirectTurnContextStore(home).read(result.turnRef)
                    val revisitEntries =
                        evidence?.capabilityTrace?.entries.orEmpty().filter {
                            it.handle == "continuity"
                        }
                    val revisitRefs = revisitEntries.flatMap { it.sourceRefs }
                    val wallNanos = System.nanoTime() - startedNanos
                    val turnCalls = provider.calls.drop(callsBefore)
                    val workingReceipt =
                        DirectWorkingContinuity.read(productHome = home, turnRef = result.turnRef)
                    val head = DirectConversationStore(home).current().head
                    val committed =
                        head?.let {
                            VexLifeWorkingContinuityOwner(home).readCurrent(it.companionLineageRef, it.threadRef)
                        }
                    val receipt = result.promptContextReceipt
                    val pressure =
                        VexLifeWorkingContinuity.projectPressure(
                                VexLifeWorkingContinuity.PromptPressureInput(
                                    promptReceiptRef = receipt.receiptRef,
                                    providerMaterializedInputTokenEstimate =
                                        receipt.providerMaterializedInputTokenEstimate,
                                    inputTokenBudget = receipt.inputTokenBudget,
                                    reservedOutputTokens = receipt.reservedOutputTokens,
                                    hardTokenLimit = receipt.hardTokenLimit,
                                    memoryBoundedOutStatementCount = 0,
                                    memoryTokenBoundedOutPageCount = 0,
                                    sourceRefs = listOf(receipt.receiptRef),
                                )
                            )
                            .state
                            .name
                    observed +=
                        Observed(
                            ordinal = ordinal,
                            setRef = set.setRef,
                            scenarioTurn = turn,
                            sentContent = sent,
                            result = result,
                            revisitSourceRefs = revisitRefs,
                            revisitPacket =
                                turnCalls
                                    .mapNotNull { call ->
                                        call.getValue("messages").jsonArray.lastOrNull()?.jsonObject
                                            ?.getValue("content")
                                            ?.jsonPrimitive
                                            ?.content
                                            ?.takeIf { it.startsWith(VexLifeCapabilityRequest.ResultHeader) }
                                    }
                                    .lastOrNull(),
                            workingDisposition =
                                DirectWorkingContinuity.read(
                                        productHome = home,
                                        turnRef = result.turnRef,
                                    )
                                    ?.disposition,
                            providerCalls = turnCalls,
                            telemetry = provider.telemetry.drop(telemetryBefore),
                            chronology = chronology.drop(recordsBefore).toList(),
                            wallNanos = wallNanos,
                            pressureState = pressure,
                            planState = workingReceipt?.planStateOrNull,
                            checkpointRef = workingReceipt?.checkpointRefOrNull,
                            cursorEventRef = committed?.checkpoint?.workingSet?.cursorEventRef,
                            canonicalReplyLanguage = language,
                            controls = turn.beforeTurnControls,
                            hostScans = revisitEntries.mapNotNull { it.hostScan },
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
                provider = provider,
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
            provider,
        )
    }

    private fun setup(home: Path, evidence: HereticModelConfiguration.QualifiedRuntimeEvidence) =
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
            qualifiedRuntimeEvidence = evidence,
            endpoint = HereticProductIdentity.Endpoint,
            requestModel = HereticProductIdentity.RequestModel,
        )

    /**
     * Deterministic synthetic filler, separated from the probe text; empty when the turn asks none.
     */
    internal fun payload(seed: Long, turn: ScenarioTurn, ordinal: Int, scale: Int = 1): String {
        if (turn.payloadTargetTokens <= 0) return ""
        val target = turn.payloadTargetTokens * scale
        var state = seed xor (ordinal.toLong() * 0x9E3779B97F4A7C15uL.toLong())
        val words = StringBuilder()
        while (
            DirectPromptContext.estimatedMessageTokens(words.toString()) < target
        ) {
            state = state xor (state shl 13)
            state = state xor (state ushr 7)
            state = state xor (state shl 17)
            words.append(PayloadWords[((state ushr 1) % PayloadWords.size).toInt()]).append(' ')
        }
        return "\n\n[SYNTHETIC PAYLOAD seed=$seed turn=${turn.turnRef} targetTokens=$target; " +
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

    // ---------------------------------------------------------------- placement evidence

    private fun DirectPromptContext.ContextComponent.mutable() =
        stabilityClass == DirectPromptContext.ComponentStability.MUTABLE_CURRENT ||
            stabilityClass == DirectPromptContext.ComponentStability.EPHEMERAL_CURRENT

    private fun historyComponent(receipt: DirectPromptContext.Receipt) =
        receipt.contextComponents.firstOrNull {
            it.stabilityClass == DirectPromptContext.ComponentStability.APPEND_ONLY_HISTORY
        }

    /** Mutable or ephemeral components placed ahead of this prompt's cacheable history; null without history. */
    private fun mutableBeforeHistory(receipt: DirectPromptContext.Receipt): List<String>? {
        val history = historyComponent(receipt) ?: return null
        return receipt.contextComponents
            .filter { it.mutable() && it.providerMessageIndex < history.providerMessageIndex }
            .map { it.componentRef }
    }

    private fun firstCallMessages(o: Observed) =
        o.providerCalls.firstOrNull()?.getValue("messages")?.jsonArray

    /**
     * Provider messages of a turn's first call that the next turn can reuse unchanged if its
     * placement allows: the system block and the exact history. The next request and reply follow
     * them as new history; everything after is this turn's current tail.
     */
    private fun cacheablePrefixLength(o: Observed): Int {
        val receipt = o.result.promptContextReceipt
        val system =
            if (receipt.contextComponents.any {
                    it.placementClass == DirectPromptContext.ComponentPlacement.SYSTEM_PREFIX
                }
            ) 1 else 0
        return system + receipt.selectedConversationEventRefs.size
    }

    /** Whether [current]'s first call begins with exactly the system block and history [prior] sent. */
    private fun prefixPreserved(prior: Observed, current: Observed): Boolean? {
        val before = firstCallMessages(prior) ?: return null
        val after = firstCallMessages(current) ?: return null
        val length = cacheablePrefixLength(prior)
        if (length == 0) return null
        return after.size >= length && (0 until length).all { before[it] == after[it] }
    }

    /** The label the lane scenario gives its pasted look-alike block. */
    private const val HistoricalMarker = "saffron-lantern-settings"

    private val CurrentSettingAssertions =
        mapOf(
            "assert.reply-language-zh-current" to "zh",
            "assert.reply-language-zh-persists" to "zh",
            "assert.reply-language-en-current" to "en",
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
            val priorObserved = observed.lastOrNull { it.ordinal < o.ordinal }
            val messages = firstCallMessages(o)
            val carried = messages?.let(::personContextsIn).orEmpty()
            val beforeHistory = mutableBeforeHistory(receipt)
            val changed =
                o.controls.isNotEmpty() &&
                    o.canonicalReplyLanguage != (priorObserved?.canonicalReplyLanguage ?: VexLifePersonSettings.DefaultLanguage)
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
                        put(
                            "contextComponents",
                            json.encodeToJsonElement(
                                receipt.contextComponents.map { c ->
                                    buildJsonObject {
                                        put("componentRef", c.componentRef)
                                        put("stabilityClass", c.stabilityClass.name)
                                        put("placementClass", c.placementClass.name)
                                        put("logicalOrder", c.logicalOrder)
                                        put("providerMessageIndex", c.providerMessageIndex)
                                        put("sha256", c.sha256)
                                        put("sourceRefs", json.encodeToJsonElement(c.sourceRefs))
                                    }
                                }
                            ),
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
                                put("providerCallCount", o.providerCalls.size)
                                put(
                                    "providerMessageOrder",
                                    json.encodeToJsonElement(
                                        messages.orEmpty().mapIndexed { index, message ->
                                            buildJsonObject {
                                                put("index", index)
                                                put("role", message.jsonObject.getValue("role"))
                                                put(
                                                    "sha256",
                                                    sha256(
                                                        message.jsonObject
                                                            .getValue("content")
                                                            .jsonPrimitive
                                                            .content
                                                            .toByteArray()
                                                    ),
                                                )
                                                put(
                                                    "componentRefs",
                                                    json.encodeToJsonElement(
                                                        receipt.contextComponents
                                                            .filter { it.providerMessageIndex == index }
                                                            .map { it.componentRef }
                                                    ),
                                                )
                                            }
                                        }
                                    ),
                                )
                                put(
                                    "mutableCurrentBeforeCacheableHistory",
                                    beforeHistory?.let { it.isNotEmpty() },
                                )
                                put(
                                    "mutableComponentsBeforeHistory",
                                    json.encodeToJsonElement(beforeHistory.orEmpty()),
                                )
                                put(
                                    "systemAndHistoryPrefixPreservedFromPriorTurn",
                                    priorObserved?.let { prefixPreserved(it, o) },
                                )
                                put("canonicalReplyLanguage", o.canonicalReplyLanguage)
                                put(
                                    "carriedReplyLanguages",
                                    json.encodeToJsonElement(carried.map { it.second }),
                                )
                                put("configurationChanged", changed)
                                put("pressureState", o.pressureState)
                                put("planState", o.planState)
                                put("checkpointRef", o.checkpointRef)
                                put("cursorEventRef", o.cursorEventRef)
                                put(
                                    "workingContinuityCommit",
                                    when {
                                        o.chronology.any { it.fact == VexLogger.Fact.WorkingContinuityCommitFailed.wire } -> "FAILED"
                                        o.chronology.any { it.fact == VexLogger.Fact.WorkingContinuityCommitted.wire } -> "COMMITTED"
                                        else -> "NOT_RECORDED"
                                    },
                                )
                                if (o.telemetry.isNotEmpty()) {
                                    put("providerTelemetry", json.encodeToJsonElement(o.telemetry))
                                }
                                put(
                                    "revisitHostScans",
                                    json.encodeToJsonElement(
                                        // Latency is host timing, kept out of the deterministic result.
                                        o.hostScans.map { scan ->
                                            buildJsonObject {
                                                put("ownerReadCount", scan.ownerReadCount)
                                                put("conversationEventsRead", scan.conversationEventsRead)
                                                put("turnsScanned", scan.turnsScanned)
                                                put("memoryPagesScanned", scan.memoryPagesScanned)
                                                put("nodesResolved", scan.nodesResolved)
                                                put("nodesReturned", scan.nodesReturned)
                                                put("returnedTokens", scan.returnedTokens)
                                                put("frontierSize", scan.frontierSize)
                                            }
                                        }
                                    ),
                                )
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
                ref in CurrentSettingAssertions -> {
                    val expected = CurrentSettingAssertions.getValue(ref)
                    val receipt = o.result.promptContextReceipt
                    val messages = firstCallMessages(o)
                    val carried = messages?.let(::personContextsIn).orEmpty()
                    val person =
                        receipt.contextComponents.firstOrNull {
                            it.componentRef == DirectPromptContext.ComponentRefs.PersonContext
                        }
                    val history = historyComponent(receipt)
                    val contentByEvent =
                        observed.flatMap {
                            listOf(
                                it.result.requestEventRef to it.sentContent,
                                it.result.responseEventRef to it.result.content,
                            )
                        }
                            .toMap()
                    val historyExact =
                        history != null &&
                            messages != null &&
                            receipt.selectedConversationEventRefs.withIndex().all { (i, ref) ->
                                messages[history.providerMessageIndex + i]
                                    .jsonObject
                                    .getValue("content")
                                    .jsonPrimitive
                                    .content == contentByEvent[ref]
                            }
                    val visible = carried.map { it.second } == listOf(expected) && o.canonicalReplyLanguage == expected
                    val current =
                        person != null &&
                            person.stabilityClass == DirectPromptContext.ComponentStability.MUTABLE_CURRENT &&
                            history != null &&
                            person.providerMessageIndex > history.providerMessageIndex
                    Triple(
                        if (visible && current && historyExact) "PASS" else "FAIL",
                        "projection: provider input carried replyLanguage=${carried.joinToString(",") { it.second }} " +
                            "(canonical ${o.canonicalReplyLanguage}); person context at message " +
                            "${person?.providerMessageIndex} ${person?.placementClass}, history from message " +
                            "${history?.providerMessageIndex}; latest visible=$visible, placed as current after history=$current, " +
                            "history replayed exact=$historyExact; not answer quality",
                        listOf(receipt.receiptRef),
                    )
                }
                ref == "assert.historical-current-state-not-authority" -> {
                    // Where VexLife's real current state sat, and where the authentic but old
                    // look-alike arrived, in the call Vex answered from (the last of the turn).
                    val last = o.providerCalls.lastOrNull()?.getValue("messages")?.jsonArray
                    val carried = last?.let(::personContextsIn).orEmpty()
                    // The look-alike block itself (its label plus a person-context block), wherever it
                    // sits other than VexLife's own envelope: not the probe or Vex's request naming it.
                    val historical =
                        last.orEmpty().withIndex().filter { (_, m) ->
                            val content = m.jsonObject.getValue("content").jsonPrimitive.content
                            content.contains(HistoricalMarker) &&
                                content.contains("VEXLIFE_PERSON_CONTEXT/v1") &&
                                !content.startsWith(DirectPromptContext.CurrentStateHeader)
                        }
                    fun where(index: Int): String {
                        val m = last!![index].jsonObject
                        val content = m.getValue("content").jsonPrimitive.content
                        return when {
                            content.startsWith(VexLifeCapabilityRequest.ResultHeader) -> "returned source"
                            else -> "history (${m.getValue("role").jsonPrimitive.content})"
                        }
                    }
                    val envelope = carried.singleOrNull()
                    Triple(
                        "UNKNOWN",
                        "projection: VexLife carried replyLanguage=${envelope?.second} (canonical " +
                            "${o.canonicalReplyLanguage}) at message ${envelope?.first?.plus(1)} of ${last?.size}; " +
                            "the authentic old look-alike naming another language appears at " +
                            historical.joinToString(", ") { "message ${it.index + 1} (${where(it.index)})" }.ifEmpty { "no message" } +
                            "; whether Vex treats the real envelope as current is a live-answer question" +
                            (if (o.telemetry.isNotEmpty()) "; live answer recorded in responseText" else ""),
                        listOf(o.result.promptContextReceipt.receiptRef),
                    )
                }
                ref == "assert.configuration-timeline" ->
                    Triple(
                        "UNKNOWN",
                        "projection: the host records the order (" +
                            observed
                                .filter { it.ordinal <= o.ordinal && it.controls.isNotEmpty() }
                                .joinToString(", ") { "ordinal ${it.ordinal}: ${it.canonicalReplyLanguage}" } +
                            "); Vex sees only the current setting plus its own earlier replies, so whether it states the timeline is a live-answer question",
                        emptyList(),
                    )
                ref == "assert.mutable-placement" -> {
                    val upTo = observed.filter { it.ordinal <= o.ordinal }
                    val offending =
                        upTo.mapNotNull { t ->
                            mutableBeforeHistory(t.result.promptContextReceipt)
                                ?.takeIf { it.isNotEmpty() }
                                ?.let { t.ordinal to it }
                        }
                    val applicable = upTo.count { mutableBeforeHistory(it.result.promptContextReceipt) != null }
                    when {
                        applicable == 0 ->
                            Triple("UNKNOWN", "projection: no turn carried history to place against", emptyList())
                        offending.isEmpty() ->
                            Triple(
                                "PASS",
                                "projection: receipt component placement — in all $applicable turns with history, every mutable/ephemeral component sits after it; Vex itself cannot see placement, so a live answer should say so",
                                emptyList(),
                            )
                        else ->
                            Triple(
                                "FAIL",
                                "projection: receipt component placement — ${offending.size} of $applicable turns put mutable/ephemeral components before history (first: ordinal ${offending.first().first}: ${offending.first().second.joinToString(", ") { it.substringAfterLast('.') }})",
                                emptyList(),
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
        // How each assertion's source reached the prompt, and whether it rode a replay turn: a pass
        // that depends on the post-rollover replay would not survive the epoch spine being fixed.
        val oscillationSet = oscillations.toSet()
        val assertionRoutes = turns.flatMap { t ->
            t.getValue("assertions").jsonArray.map { a ->
                val obj = a.jsonObject
                val detail = obj["detail"]?.jsonPrimitive?.content.orEmpty()
                buildJsonObject {
                    put("ordinal", ordinal(t))
                    put("assertionRef", obj.getValue("assertionRef").jsonPrimitive.content)
                    put("state", obj.getValue("state").jsonPrimitive.content)
                    put("route", RouteInDetail.find(detail)?.groupValues?.get(1))
                    put("onReplayOscillationTurn", ordinal(t) in oscillationSet)
                }
            }
        }
        val passesOnReplay = assertionRoutes.count {
            it.getValue("state").jsonPrimitive.content == "PASS" &&
                it.getValue("onReplayOscillationTurn").jsonPrimitive.content == "true"
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
            val beforeHistory =
                turns.mapNotNull { ext(it)["mutableCurrentBeforeCacheableHistory"]?.jsonPrimitive?.booleanOrNull }
            put("mutableCurrentBeforeCacheableHistory", if (beforeHistory.isEmpty()) null else beforeHistory.any { it })
            val changes = turns.filter { ext(it)["configurationChanged"]?.jsonPrimitive?.booleanOrNull == true }
            val preservedAtChange =
                changes.map { ext(it)["systemAndHistoryPrefixPreservedFromPriorTurn"]?.jsonPrimitive?.booleanOrNull }
            put(
                "stableHistoryPrefixPreservedAcrossConfigurationChange",
                if (preservedAtChange.isEmpty() || preservedAtChange.any { it == null }) null
                else preservedAtChange.all { it == true },
            )
            val carriedByTurn =
                turns.map { t ->
                    ext(t)["carriedReplyLanguages"]?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty() to
                        ext(t)["canonicalReplyLanguage"]?.jsonPrimitive?.contentOrNull
                }
            put(
                "latestConfigurationObserved",
                if (carriedByTurn.all { it.first.isEmpty() }) null
                else carriedByTurn.all { (carried, canonical) -> carried == listOfNotNull(canonical) },
            )
            put("firstConfigurationChangeTurn", changes.firstOrNull()?.let(::ordinal))
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
                    put("assertionRoutes", json.encodeToJsonElement(assertionRoutes))
                    put("passesDependingOnReplayOscillation", passesOnReplay)
                    put(
                        "pressureStateFirstTurns",
                        buildJsonObject {
                            turns
                                .mapNotNull { t ->
                                    ext(t)["pressureState"]?.jsonPrimitive?.contentOrNull?.let { it to ordinal(t) }
                                }
                                .groupBy({ it.first }, { it.second })
                                .forEach { (state, ordinals) -> put(state, ordinals.min()) }
                        },
                    )
                    put(
                        "thresholdTrace",
                        json.encodeToJsonElement(
                            turns.map { t ->
                                listOf(
                                        ordinal(t).toString(),
                                        ext(t)["pressureState"]?.jsonPrimitive?.contentOrNull ?: "-",
                                        prompt(t).getValue("contextMode").jsonPrimitive.content,
                                        ext(t)["workingContinuityDisposition"]?.jsonPrimitive?.contentOrNull ?: "-",
                                        selected(t).size.toString(),
                                    )
                                    .joinToString(" ")
                            }
                        ),
                    )
                    put(
                        "systemAndHistoryPrefixPreservedByTurn",
                        json.encodeToJsonElement(
                            turns.map { ext(it)["systemAndHistoryPrefixPreservedFromPriorTurn"] ?: JsonNull }
                        ),
                    )
                    put(
                        "mutableCurrentBeforeHistoryTurns",
                        json.encodeToJsonElement(
                            turns
                                .filter { ext(it)["mutableCurrentBeforeCacheableHistory"]?.jsonPrimitive?.booleanOrNull == true }
                                .map(::ordinal)
                        ),
                    )
                    val scans = turns.flatMap { ext(it)["revisitHostScans"]?.jsonArray.orEmpty() }.map { it.jsonObject }
                    fun total(key: String) = scans.sumOf { it.getValue(key).jsonPrimitive.content.toInt() }
                    put(
                        "revisitHostScan",
                        buildJsonObject {
                            put("modelVisibleRevisitCalls", scans.size)
                            put("ownerReadCount", total("ownerReadCount"))
                            put("conversationEventsRead", total("conversationEventsRead"))
                            put("turnsScanned", total("turnsScanned"))
                            put("nodesResolved", total("nodesResolved"))
                            put("nodesReturned", total("nodesReturned"))
                            put("returnedTokens", total("returnedTokens"))
                            put("maximumFrontierSize", scans.maxOfOrNull { it.getValue("frontierSize").jsonPrimitive.content.toInt() } ?: 0)
                            put("ownerReadLatency", "host timing; see host-scan-timing.json beside the ZIP")
                        },
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
            states.none { it == "PASS" } && states.any { it == "FAIL" } -> "FAIL"
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

    private fun held(config: Config, scenario: Scenario, scenarioSha: String, reason: String): Int {
        val report = buildJsonObject {
            put("state", "HELD")
            put("stage", config.stage.name)
            put("executionClass", "LIVE_MODEL")
            put("effectClass", "HOST_SEMANTIC_MODEL_RUNTIME")
            put("scenarioRef", scenario.scenarioRef)
            put("scenarioSha256", scenarioSha)
            put("reason", reason)
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
        provider: Provider,
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
            put("scenarioIsCanonicalShared", scenarioSha in SharedScenarioDigests)
            put("sourceSha", config.sourceSha)
            put("sourceTreeClean", config.sourceClean)
            put("payloadScale", config.payloadScale)
            put("liveRuntimeBorrowed", provider.live)
            put("experimentWindowAffirmed", config.experimentWindow)
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
        // Exactly what each provider call carried (synthetic only), so the provider representation
        // can be re-rendered with the pinned chat template outside this run.
        if (provider.live) {
            files["implementation/provider-telemetry.jsonl"] =
                jsonl(
                    observed.flatMap { o ->
                        o.telemetry.map { row ->
                            buildJsonObject {
                                put("ordinal", o.ordinal)
                                row.forEach { (key, value) -> put(key, value) }
                            }
                        }
                    }
                )
        }
        files["implementation/provider-calls.jsonl"] =
            jsonl(
                observed.flatMap { o ->
                    o.providerCalls.mapIndexed { index, call ->
                        buildJsonObject {
                            put("ordinal", o.ordinal)
                            put("call", index)
                            put("messages", call.getValue("messages"))
                            put("chat_template_kwargs", call["chat_template_kwargs"] ?: JsonNull)
                        }
                    }
                }
            )
        files["implementation/adapter-report.json"] =
            buildJsonObject {
                put("adapterRef", AdapterRef)
                put(
                    "provider",
                    if (provider.live) {
                        "LIVE_MODEL: exact already-bound host runtime observed read-only; provider calls forwarded unchanged; " +
                            "usage and timing are observations from the returned stream"
                    } else {
                        "DETERMINISTIC_FIXTURE: echoes 'Reply only: X'; on probe turns asks revisit once with the probe's own content words; " +
                            "on setting turns reports the reply-language setting the provider input itself carried, and where"
                    },
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
            put("executionClass", if (provider.live) "LIVE_MODEL" else "DETERMINISTIC_PROJECTION")
            put("status", status)
            put(
                "source",
                buildJsonObject {
                    put("repositoryRef", "VexGPT/VexLife-Prod")
                    put("sourceSha", config.sourceSha)
                    put("modelRef", if (provider.live) provider.evidence.modelRef else null)
                    put("runtimeRef", if (provider.live) provider.evidence.runtimeReceiptRef else null)
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
        // Transition timing is real host time from the Product's own chronology records.
        Files.writeString(
            config.resultDir.resolve("transition-timing.json"),
            json.encodeToJsonElement(observed.map(::transitionTiming)).toString() + "\n",
        )
        // Owner-read latency is real host time, so it stays beside the ZIP, not in its digest.
        Files.writeString(
            config.resultDir.resolve("host-scan-timing.json"),
            json.encodeToJsonElement(
                observed.flatMap { o ->
                    o.hostScans.map { scan ->
                        buildJsonObject {
                            put("ordinal", o.ordinal)
                            put("ownerReadCount", scan.ownerReadCount)
                            put("ownerReadNanos", scan.ownerReadNanos)
                            put("conversationEventsRead", scan.conversationEventsRead)
                            put("turnsScanned", scan.turnsScanned)
                        }
                    }
                }
            ).toString() + "\n",
        )
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

    private fun at(o: Observed, fact: VexLogger.Fact, last: Boolean = false): Instant? =
        o.chronology
            .filter { it.fact == fact.wire }
            .let { if (last) it.lastOrNull() else it.firstOrNull() }
            ?.let { Instant.parse(it.at) }

    private fun millisBetween(from: Instant?, to: Instant?): Double? =
        if (from == null || to == null) null else Duration.between(from, to).toNanos() / 1_000_000.0

    /** Real host time across this turn's continuity boundaries, from the Product's chronology. */
    private fun transitionTiming(o: Observed): JsonObject = buildJsonObject {
        put("ordinal", o.ordinal)
        put("contextMode", o.result.promptContextReceipt.contextMode)
        put("pressureState", o.pressureState)
        put("planState", o.planState)
        put("workingContinuityDisposition", o.workingDisposition)
        put("turnWallMs", o.wallNanos / 1_000_000.0)
        put(
            "sendAcceptedToProviderInvocationMs",
            millisBetween(at(o, VexLogger.Fact.SendAccepted), at(o, VexLogger.Fact.ProviderInvocationRequested)),
        )
        put(
            "answerDurableToCheckpointCommittedMs",
            millisBetween(at(o, VexLogger.Fact.TurnCompleted), at(o, VexLogger.Fact.WorkingContinuityCommitted)),
        )
        put("checkpointCommitFailed", o.chronology.any { it.fact == VexLogger.Fact.WorkingContinuityCommitFailed.wire })
        put("providerCalls", o.providerCalls.size)
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
            runtimeHome =
                map["runtime-home"]?.let { Path.of(it) }?.also {
                    require(it.isAbsolute) { "--runtime-home must be an absolute path" }
                },
            experimentWindow = map["experiment-window"] == "true",
            payloadScale = map["payload-scale"]?.toInt() ?: 1,
        )
    }
}

fun main(args: Array<String>) {
    val code = ContinuityPressureAdapter.run(ContinuityPressureAdapter.parse(args))
    kotlin.system.exitProcess(code)
}

// [VXG RealForever]
