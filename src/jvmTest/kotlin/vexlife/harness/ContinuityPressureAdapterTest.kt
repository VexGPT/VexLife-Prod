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
            val (telemetry, providerLabel) = zip.use { z ->
                val rows =
                    String(
                            z.getInputStream(z.getEntry("implementation/provider-telemetry.jsonl"))
                                .readBytes()
                        )
                        .lines()
                        .filter { it.isNotBlank() }
                        .map { Json.parseToJsonElement(it).jsonObject }
                val report =
                    Json.parseToJsonElement(
                            String(
                                z.getInputStream(z.getEntry("implementation/adapter-report.json"))
                                    .readBytes()
                            )
                        )
                        .jsonObject
                rows to report.getValue("provider").jsonPrimitive.content
            }
            assertEquals(8, telemetry.size)
            assertTrue(providerLabel.startsWith("LIVE_MODEL:"))
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
