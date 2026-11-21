package vexlife.harness

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import vexlife.companion.DirectCompanionConnection
import vexlife.companion.DirectContinuityOverlapTail
import vexlife.companion.DirectConversationSourceReturn
import vexlife.continuity.DirectConversationContextProjection

/**
 * The harness adapter runs the shared canonical scenario through the real owners and returns the
 * shared result contract, without touching any Home but the scratch one it forms and deletes.
 */
class ContinuityPressureAdapterTest {
    private val scenario = Path.of("tools/continuity-pressure/scenarios/depth-continuity-v1.json")

    private fun config(stage: ContinuityPressureAdapter.Stage, dir: Path, sets: Int? = null) =
        ContinuityPressureAdapter.Config(
            stage = stage,
            scenarioRef = ContinuityPressureAdapter.CanonicalScenarioRef,
            scenarioFile = scenario.toAbsolutePath(),
            resultDir = dir,
            strategy = ContinuityPressureAdapter.Strategy.EFFECTIVE_SELECTION_BUDGET,
            effectiveInputTokenBudget = null,
            sets = sets,
            turnsPerSet = null,
            sourceSha = "0".repeat(40),
            sourceClean = false,
        )

    @Test
    fun theProductCopyOfTheScenarioIsTheSharedCanonicalOne() {
        val sha =
            java.security.MessageDigest.getInstance("SHA-256")
                .digest(Files.readAllBytes(scenario))
                .joinToString("") {
                    "%02x".format(it)
                }
        assertEquals(ContinuityPressureAdapter.CanonicalScenarioSha256, sha)
    }

    @Test
    fun projectionRunsTheWholeScenarioReportsWhatItSawAndAnalyzeAgrees() {
        val dir = Files.createTempDirectory("vexlife-prod-continuity-pressure-result-")
        try {
            val scratchBefore = scratchDirectories()
            assertEquals(
                0,
                ContinuityPressureAdapter.run(
                    config(ContinuityPressureAdapter.Stage.projection, dir)
                ),
            )
            assertEquals(
                scratchBefore,
                scratchDirectories(),
                "the scratch Home is deleted after the bundle",
            )

            val result =
                Json.parseToJsonElement(Files.readString(dir.resolve("result.json"))).jsonObject
            assertEquals(
                "vexlife.continuity-pressure-result/v1",
                result.getValue("schemaVersion").jsonPrimitive.content,
            )
            assertEquals(
                "DETERMINISTIC_PROJECTION",
                result.getValue("executionClass").jsonPrimitive.content,
            )
            assertEquals(
                "true",
                result
                    .getValue("scratch")
                    .jsonObject
                    .getValue("deletedAfterBundle")
                    .jsonPrimitive
                    .content,
            )
            val turns = result.getValue("turns").jsonArray.map { it.jsonObject }
            assertEquals(30, turns.size)
            assertEquals(
                52_000,
                turns
                    .first()
                    .getValue("prompt")
                    .jsonObject
                    .getValue("effectiveInputTokenBudget")
                    .jsonPrimitive
                    .content
                    .toInt(),
            )
            assertEquals(
                192_000,
                turns
                    .first()
                    .getValue("prompt")
                    .jsonObject
                    .getValue("ordinaryInputTokenBudget")
                    .jsonPrimitive
                    .content
                    .toInt(),
            )
            val summary = result.getValue("summary").jsonObject
            assertTrue(
                summary.getValue("firstWorkingRolloverTurn").jsonPrimitive.content.toInt() in
                    11..20,
                "rollover lands in the pressure set",
            )
            assertTrue(
                summary.getValue("firstHydrationTurn").jsonPrimitive.content.toInt() > 20,
                "revisit is first used by a probe",
            )
            assertEquals("true", summary.getValue("honestAbsence").jsonPrimitive.content)
            assertTrue(
                Files.list(dir).use { s -> s.anyMatch { it.fileName.toString().endsWith(".zip") } }
            )

            assertEquals(
                0,
                ContinuityPressureAdapter.run(config(ContinuityPressureAdapter.Stage.analyze, dir)),
            )
            val analysis =
                Json.parseToJsonElement(Files.readString(dir.resolve("analysis.json"))).jsonObject
            assertEquals("true", analysis.getValue("matchesRecordedSummary").jsonPrimitive.content)
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun conversationContextProjectionTreatmentPutsOlderExactSourceInTheFirstProviderCall() {
        val baselineDir =
            Files.createTempDirectory(
                "vexlife-prod-continuity-pressure-conversation-context-baseline-",
            )
        val treatmentDir =
            Files.createTempDirectory(
                "vexlife-prod-continuity-pressure-conversation-context-treatment-",
            )
        try {
            val baseline =
                config(
                    ContinuityPressureAdapter.Stage.projection,
                    baselineDir,
                )
            val treatment =
                baseline
                    .copy(
                        resultDir = treatmentDir,
                        conversationContextProjectionEnabled = true,
                    )

            val baselineExit =
                ContinuityPressureAdapter.run(
                    baseline,
                )
            assertEquals(
                0,
                baselineExit,
                "baseline continuity-pressure projection must complete",
            )
            val treatmentExit =
                ContinuityPressureAdapter.run(
                    treatment,
                )
            if (
                treatmentExit !=
                    0
            ) {
                val failedResult =
                    Json
                        .parseToJsonElement(
                            Files.readString(
                                treatmentDir.resolve(
                                    "result.json",
                                ),
                            ),
                        )
                        .jsonObject
                assertEquals(
                    0,
                    treatmentExit,
                    "treatment continuity-pressure projection failed: " +
                        failedResult["failure"],
                )
            }

            fun result(dir: Path) =
                Json
                    .parseToJsonElement(
                        Files.readString(
                            dir.resolve(
                                "result.json",
                            ),
                        ),
                    )
                    .jsonObject

            fun probe01(value: kotlinx.serialization.json.JsonObject) =
                value
                    .getValue(
                        "turns",
                    )
                    .jsonArray
                    .map {
                        it.jsonObject
                    }
                    .single {
                        it
                            .getValue(
                                "scenarioTurnRef",
                            )
                            .jsonPrimitive
                            .content ==
                            "probe.01"
                    }

            fun formation01Residency(
                turn: kotlinx.serialization.json.JsonObject,
            ): String =
                turn
                    .getValue(
                        "residency",
                    )
                    .jsonArray
                    .map {
                        it.jsonObject
                    }
                    .single {
                        it
                            .getValue(
                                "ref",
                            )
                            .jsonPrimitive
                            .content
                            .startsWith(
                                "formation.01:",
                            )
                    }
                    .getValue(
                        "state",
                    )
                    .jsonPrimitive
                    .content

            val baselineProbe =
                probe01(
                    result(
                        baselineDir,
                    ),
                )
            val treatmentProbe =
                probe01(
                    result(
                        treatmentDir,
                    ),
                )

            assertEquals(
                "HYDRATED_THIS_TURN",
                formation01Residency(
                    baselineProbe,
                ),
                "baseline reaches the old anchor only after Vex requests continuity revisit",
            )
            assertEquals(
                "CONVERSATION_CONTEXT_PROJECTION_THIS_TURN",
                formation01Residency(
                    treatmentProbe,
                ),
                "request-bound Conversation projection should place the exact old anchor before Vex asks",
            )
            val treatmentPrompt =
                treatmentProbe
                    .getValue(
                        "prompt",
                    )
                    .jsonObject
            assertEquals(
                "WORKING_CHECKPOINT_SUCCESSOR",
                treatmentPrompt
                    .getValue(
                        "contextMode",
                    )
                    .jsonPrimitive
                    .content,
                "Conversation context projection must not masquerade as provider Source Return context mode",
            )
            assertEquals(
                DirectConversationContextProjection.Returned,
                treatmentPrompt
                    .getValue(
                        "conversationContextProjectionState",
                    )
                    .jsonPrimitive
                    .content,
            )
            assertTrue(
                treatmentPrompt
                    .getValue(
                        "conversationContextProjectionSourceEventRefs",
                    )
                    .jsonArray
                    .isNotEmpty(),
            )
            assertTrue(
                treatmentPrompt
                    .getValue(
                        "sourceReturnSourceEventRefs",
                    )
                    .jsonArray
                    .isEmpty(),
                "Conversation context projection must leave provider Source Return evidence empty",
            )

            fun firstProbeProviderMessages(
                dir: Path,
                probe: kotlinx.serialization.json.JsonObject,
            ): kotlinx.serialization.json.JsonArray {
                val ordinal =
                    probe
                        .getValue(
                            "ordinal",
                        )
                        .jsonPrimitive
                        .content
                        .toInt()
                val zipPath =
                    Files
                        .list(
                            dir,
                        )
                        .use {
                            paths ->
                            paths
                                .filter {
                                    it
                                        .fileName
                                        .toString()
                                        .endsWith(
                                            ".zip",
                                        )
                                }
                                .findFirst()
                                .orElseThrow()
                        }
                return java.util.zip.ZipFile(
                    zipPath.toFile(),
                )
                    .use {
                        zip ->
                        String(
                            zip
                                .getInputStream(
                                    zip.getEntry(
                                        "implementation/provider-calls.jsonl",
                                    ),
                                )
                                .readBytes(),
                        )
                            .lineSequence()
                            .filter {
                                it.isNotBlank()
                            }
                            .map {
                                Json
                                    .parseToJsonElement(
                                        it,
                                    )
                                    .jsonObject
                            }
                            .single {
                                row ->
                                row
                                    .getValue(
                                        "ordinal",
                                    )
                                    .jsonPrimitive
                                    .content
                                    .toInt() ==
                                    ordinal &&
                                    row
                                        .getValue(
                                            "call",
                                        )
                                        .jsonPrimitive
                                        .content
                                        .toInt() ==
                                    0
                            }
                            .getValue(
                                "messages",
                            )
                            .jsonArray
                    }
            }

            val baselineFirst =
                firstProbeProviderMessages(
                    baselineDir,
                    baselineProbe,
                )
            val treatmentFirst =
                firstProbeProviderMessages(
                    treatmentDir,
                    treatmentProbe,
                )

            fun carriesAnchor(
                messages: kotlinx.serialization.json.JsonArray,
            ): Boolean =
                messages.any {
                    it
                        .jsonObject
                        .getValue(
                            "content",
                        )
                        .jsonPrimitive
                        .content
                        .contains(
                            "the cobalt lantern is stored beside the cedar gate",
                        )
                }

            assertFalse(
                carriesAnchor(
                    baselineFirst,
                ),
                "baseline first inference must preserve the known omitted-source pressure",
            )
            assertTrue(
                carriesAnchor(
                    treatmentFirst,
                ),
                "treatment first inference must already carry the exact canonical anchor",
            )
            assertTrue(
                treatmentFirst.any {
                    it
                        .jsonObject
                        .getValue(
                            "content",
                        )
                        .jsonPrimitive
                        .content
                        .startsWith(
                            DirectConversationContextProjection.ContextHeader,
                        )
                },
                "Conversation context projection must have its own provider-facing context identity",
            )
            assertFalse(
                treatmentFirst.any {
                    it
                        .jsonObject
                        .getValue(
                            "content",
                        )
                        .jsonPrimitive
                        .content
                        .startsWith(
                            DirectConversationSourceReturn.RequestHeader,
                        )
                },
                "request-bound projection must not fabricate a provider-authored source-return request",
            )
        } finally {
            baselineDir
                .toFile()
                .deleteRecursively()
            treatmentDir
                .toFile()
                .deleteRecursively()
        }
    }

    /**
     * Through the real turn loop (DirectCompanionTurn, the Working Continuity owners and
     * DirectPromptContext; only the provider is a fixture): once a rollover is committed, every
     * later turn stays a bounded WORKING_CHECKPOINT_SUCCESSOR even after pressure falls to Watch or
     * Normal. Each one advances the rollover lineage with its own request as the cursor, and no
     * pre-rollover history is replayed. Before the rollover-lineage fix the turn after the first
     * successor fell back to RECENT_COMPLETED_TURNS, replayed the old Conversation and rolled over
     * again at once.
     */
    @Test
    fun afterRolloverEveryLaterTurnStaysABoundedSuccessorAsPressureFalls() {
        val dir = Files.createTempDirectory("vexlife-prod-continuity-pressure-rollover-lineage-")
        try {
            val engineered =
                config(ContinuityPressureAdapter.Stage.projection, dir, sets = 3)
                    .copy(effectiveInputTokenBudget = 80_000, turnsPerSet = 5, payloadScale = 7)
            assertEquals(0, ContinuityPressureAdapter.run(engineered))
            val result =
                Json.parseToJsonElement(Files.readString(dir.resolve("result.json"))).jsonObject
            val turns = result.getValue("turns").jsonArray.map { it.jsonObject }
            fun ext(t: kotlinx.serialization.json.JsonObject) =
                t.getValue("implementationExtensions")
                    .jsonObject
                    .getValue("vexlife-prod.claude-c20/v1")
                    .jsonObject
            fun text(t: kotlinx.serialization.json.JsonObject, key: String) =
                ext(t)[key]?.jsonPrimitive?.content
            fun mode(t: kotlinx.serialization.json.JsonObject) =
                t.getValue("prompt").jsonObject.getValue("contextMode").jsonPrimitive.content
            fun selected(t: kotlinx.serialization.json.JsonObject) =
                t.getValue("prompt")
                    .jsonObject
                    .getValue("selectedConversationEventRefs")
                    .jsonArray
                    .map {
                        it.jsonPrimitive.content
                    }

            val rollover = turns.indexOfFirst {
                text(it, "workingContinuityDisposition") == "ROLLOVER_COMMITTED"
            }
            assertTrue(
                rollover in 0 until turns.size - 2,
                "a rollover is committed with turns after it",
            )
            val after = turns.drop(rollover + 1)
            val formation =
                turns
                    .filter { it.getValue("setRef").jsonPrimitive.content == "set.formation" }
                    .flatMap {
                        listOf(text(it, "requestEventRef")!!, text(it, "responseEventRef")!!)
                    }
                    .toSet()

            after.forEach { t ->
                val ordinal = t.getValue("ordinal").jsonPrimitive.content
                assertEquals(
                    "WORKING_CHECKPOINT_SUCCESSOR",
                    mode(t),
                    "turn $ordinal stays a successor",
                )
                assertEquals(
                    "ROLLOVER_COMMITTED",
                    text(t, "workingContinuityDisposition"),
                    "turn $ordinal",
                )
                assertEquals(
                    text(t, "requestEventRef"),
                    text(t, "cursorEventRef"),
                    "turn $ordinal advances the lineage to its own request",
                )
                assertTrue(
                    selected(t).size <= 2 * DirectContinuityOverlapTail.MaximumPairs,
                    "turn $ordinal is bounded",
                )
                assertTrue(
                    selected(t).none { it in formation },
                    "turn $ordinal replays no pre-rollover formation",
                )
            }
            assertTrue(
                after.any { text(it, "pressureState") in setOf("Watch", "Normal") },
                "the lineage holds after pressure itself has fallen",
            )
            assertEquals(
                "false",
                result
                    .getValue("summary")
                    .jsonObject
                    .getValue("postRolloverReplayOscillation")
                    .jsonPrimitive
                    .content,
            )
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun aLiveStageIsHeldHereAndTouchesNothing() {
        val dir = Files.createTempDirectory("vexlife-prod-continuity-pressure-held-")
        try {
            val scratchBefore = scratchDirectories()
            assertEquals(
                3,
                ContinuityPressureAdapter.run(config(ContinuityPressureAdapter.Stage.full, dir)),
            )
            assertEquals(scratchBefore, scratchDirectories())
            val held =
                Json.parseToJsonElement(Files.readString(dir.resolve("held.json"))).jsonObject
            assertEquals("HELD", held.getValue("state").jsonPrimitive.content)
            assertEquals(
                "HOST_SEMANTIC_MODEL_RUNTIME",
                held.getValue("effectClass").jsonPrimitive.content,
            )
            assertFalse(Files.exists(dir.resolve("result.json")))
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    /**
     * The live seam against a protocol stub, not a model: it proves a live run streams through the
     * runtime it is lent, keeps its scratch Home, records the provider's own usage report and
     * timing, and reads transition timing from the Product's chronology. Model behaviour is proved
     * only on the host.
     */
    @Test
    fun aLiveRunStreamsThroughTheLentRuntimeAndRecordsWhatTheProviderReported() {
        val dir = Files.createTempDirectory("vexlife-prod-continuity-pressure-live-seam-")
        try {
            var calls = 0
            val stub =
                object : DirectCompanionConnection.StreamingTransport {
                    override fun post(call: DirectCompanionConnection.HttpCall) =
                        error("a live run streams")

                    override fun postStreaming(
                        call: DirectCompanionConnection.HttpCall,
                        onLine: (String) -> Unit,
                    ): DirectCompanionConnection.StreamingHttpResult {
                        calls += 1
                        val prompt = call.body.length / 4
                        val chunk = """{"model":"default_model","choices":[{"delta":%s}]}"""
                        onLine("data: " + chunk.format("""{"role":"assistant"}"""))
                        onLine("data: " + chunk.format("""{"reasoning":"thinking"}"""))
                        onLine("data: " + chunk.format("""{"content":"Stub answer $calls."}"""))
                        onLine(
                            """data: {"model":"default_model","choices":[],"usage":{"prompt_tokens":$prompt,""" +
                                """"completion_tokens":3,"prompt_tokens_details":{"cached_tokens":${prompt / 2}}}}"""
                        )
                        onLine("data: [DONE]")
                        return DirectCompanionConnection.StreamingHttpResult(200, "0".repeat(64))
                    }
                }
            val placement =
                Path.of("tools/continuity-pressure/scenarios/current-state-placement-v1.json")
            val live =
                config(ContinuityPressureAdapter.Stage.quick, dir)
                    .copy(
                        scenarioRef =
                            "scenario.vexlife.continuity-pressure.current-state-placement.v1",
                        scenarioFile = placement.toAbsolutePath(),
                        runtimeHome = Path.of("/nonexistent/host/vex-home"),
                        experimentWindow = true,
                    )
            val lent =
                ContinuityPressureAdapter.LiveRuntime(
                    evidence = ContinuityPressureAdapter.FixtureEvidence,
                    observe = { ContinuityPressureAdapter.FixtureEvidence },
                    transport = stub,
                )
            assertEquals(0, ContinuityPressureAdapter.run(live) { lent })

            val result =
                Json.parseToJsonElement(Files.readString(dir.resolve("result.json"))).jsonObject
            assertEquals("LIVE_MODEL", result.getValue("executionClass").jsonPrimitive.content)
            assertEquals(
                ContinuityPressureAdapter.FixtureEvidence.runtimeReceiptRef,
                result.getValue("source").jsonObject.getValue("runtimeRef").jsonPrimitive.content,
            )
            assertEquals(8, calls)
            val zip =
                java.util.zip.ZipFile(
                    Files.list(dir)
                        .use { it.filter { p -> p.toString().endsWith(".zip") }.findFirst().get() }
                        .toFile()
                )
            val telemetry = zip.use { z ->
                String(
                        z.getInputStream(z.getEntry("implementation/provider-telemetry.jsonl"))
                            .readBytes()
                    )
                    .lines()
                    .filter { it.isNotBlank() }
                    .map { Json.parseToJsonElement(it).jsonObject }
            }
            assertEquals(8, telemetry.size)
            telemetry.forEach { row ->
                val prompt = row.getValue("promptTokens").jsonPrimitive.content.toInt()
                val cached = row.getValue("cachedPromptTokens").jsonPrimitive.content.toInt()
                assertEquals(
                    prompt - cached,
                    row.getValue("prefillTokens").jsonPrimitive.content.toInt(),
                )
                assertTrue(row.getValue("firstTokenMs").jsonPrimitive.content.toLong() >= 0)
                assertTrue(row.getValue("firstAnswerMs").jsonPrimitive.content.toLong() >= 0)
            }
            val timing =
                Json.parseToJsonElement(Files.readString(dir.resolve("transition-timing.json")))
                    .jsonArray
            assertEquals(8, timing.size)
            assertTrue(
                timing.all {
                    it.jsonObject["answerDurableToCheckpointCommittedMs"]?.jsonPrimitive?.content !=
                        "null"
                },
                "every turn's checkpoint commit is timed from the Product's chronology",
            )
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun aLiveStageWithoutAnExperimentWindowIsHeldBeforeAnyRuntimeIsTouched() {
        val dir = Files.createTempDirectory("vexlife-prod-continuity-pressure-window-")
        try {
            var bound = false
            val code =
                ContinuityPressureAdapter.run(
                    config(ContinuityPressureAdapter.Stage.quick, dir)
                        .copy(runtimeHome = Path.of("/nonexistent/host/vex-home"))
                ) {
                    bound = true
                    error("must not bind")
                }
            assertEquals(3, code)
            assertFalse(bound)
            val held =
                Json.parseToJsonElement(Files.readString(dir.resolve("held.json"))).jsonObject
            assertTrue(
                held.getValue("reason").jsonPrimitive.content.contains("--experiment-window")
            )
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun runtimeStartAtBindIsExplicitAndDefaultsHeld() {
        val dir = Files.createTempDirectory("vexlife-prod-continuity-pressure-runtime-bind-")
        try {
            assertFalse(
                config(
                    ContinuityPressureAdapter.Stage.quick,
                    dir,
                ).runtimeStartAtBind
            )

            val parsed =
                ContinuityPressureAdapter.parse(
                    arrayOf(
                        "--stage",
                        "quick",
                        "--scenario-ref",
                        ContinuityPressureAdapter.CanonicalScenarioRef,
                        "--scenario-file",
                        scenario.toAbsolutePath().toString(),
                        "--result-dir",
                        dir.toAbsolutePath().toString(),
                        "--source-sha",
                        "1".repeat(40),
                        "--runtime-home",
                        "/nonexistent/host/vex-home",
                        "--experiment-window",
                        "true",
                        "--runtime-start-at-bind",
                        "true",
                    )
                )
            assertTrue(parsed.runtimeStartAtBind)
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun syntheticPayloadIsDeterministicReachesItsTargetAndIsMarkedAsFiller() {
        val turn =
            ContinuityPressureAdapter.ScenarioTurn(
                "pressure.01",
                "Reply only: p1.",
                listOf("PRESSURE"),
                payloadTargetTokens = 1200,
            )
        val a = ContinuityPressureAdapter.payload(271828, turn, 11)
        assertEquals(a, ContinuityPressureAdapter.payload(271828, turn, 11))
        assertTrue(a != ContinuityPressureAdapter.payload(271828, turn, 12))
        assertTrue(
            a.contains("[SYNTHETIC PAYLOAD seed=271828 turn=pressure.01") &&
                a.endsWith("[END SYNTHETIC PAYLOAD]")
        )
        assertTrue(vexlife.companion.DirectPromptContext.estimatedMessageTokens(a) >= 1200)
        assertEquals(
            "",
            ContinuityPressureAdapter.payload(271828, turn.copy(payloadTargetTokens = 0), 11),
        )
    }

    private fun scratchDirectories(): Set<String> =
        Files.list(Path.of(System.getProperty("java.io.tmpdir"))).use { s ->
            s.map { it.fileName.toString() }
                .filter {
                    it.startsWith("vexlife-prod-continuity-pressure-") &&
                        !it.contains("result") &&
                        !it.contains("held")
                }
                .toList()
                .toSet()
        }
}

// [VXG RealForever]
