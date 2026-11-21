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

    @Test
    fun provisionalRecontactCarriesTheProposalButNotItsPrivateReasoningAndCommitsOnlyTheSecondAnswer() {
        val root = Files.createTempDirectory("vexlife-prod-provisional-recontact-")
        try {
            val scenarioFile = root.resolve("scenario.json")
            val resultDir = root.resolve("result")
            Files.writeString(
                scenarioFile,
                """
                {
                  "schemaVersion": "vexlife.continuity-pressure-scenario/v1",
                  "scenarioRef": "scenario.vexlife.continuity-pressure.test.provisional-recontact.v1",
                  "label": "Provisional re-contact seam",
                  "purpose": "Prove one first proposal can remain uncommitted while a second provider contact receives it without first-pass private reasoning.",
                  "syntheticOnly": true,
                  "sets": [
                    {
                      "setRef": "set.recontact",
                      "kind": "PROBE",
                      "turns": [
                        {
                          "turnRef": "recontact.01",
                          "input": "I am unsure. Stay with only what is actually here.",
                          "tags": [],
                          "expectedSignals": [],
                          "assertionRefs": []
                        }
                      ]
                    }
                  ]
                }
                """.trimIndent(),
            )

            val physicalBodies = mutableListOf<String>()
            val stub =
                object : DirectCompanionConnection.StreamingTransport {
                    override fun post(call: DirectCompanionConnection.HttpCall) =
                        error("the provisional re-contact seam is streaming-only")

                    override fun postStreaming(
                        call: DirectCompanionConnection.HttpCall,
                        onLine: (String) -> Unit,
                    ): DirectCompanionConnection.StreamingHttpResult {
                        physicalBodies += call.body
                        val callNumber = physicalBodies.size
                        val prompt = call.body.length / 4
                        val chunk = """{"model":"default_model","choices":[{"delta":%s}]}"""
                        onLine("data: " + chunk.format("""{"role":"assistant"}"""))
                        if (callNumber == 1) {
                            assertFalse(
                                call.body.contains("VEXLIFE_PROVISIONAL_RESPONSE_RECONTACT/v1")
                            )
                            onLine(
                                "data: " +
                                    chunk.format(
                                        """{"reasoning":"first-pass-private-reasoning"}"""
                                    )
                            )
                            onLine(
                                "data: " +
                                    chunk.format(
                                        """{"content":"First proposal."}"""
                                    )
                            )
                        } else {
                            assertEquals(2, callNumber)
                            assertTrue(call.body.contains("First proposal."))
                            assertTrue(
                                call.body.contains("VEXLIFE_PROVISIONAL_RESPONSE_RECONTACT/v1")
                            )
                            assertTrue(
                                call.body.contains(
                                    "I am unsure. Stay with only what is actually here."
                                )
                            )
                            assertFalse(call.body.contains("first-pass-private-reasoning"))
                            onLine(
                                "data: " +
                                    chunk.format(
                                        """{"reasoning":"second-pass-private-reasoning"}"""
                                    )
                            )
                            onLine(
                                "data: " +
                                    chunk.format(
                                        """{"content":"Second response."}"""
                                    )
                            )
                        }
                        onLine(
                            """data: {"model":"default_model","choices":[],"usage":{"prompt_tokens":$prompt,""" +
                                """"completion_tokens":3,"prompt_tokens_details":{"cached_tokens":${prompt / 2}}}}"""
                        )
                        onLine("data: [DONE]")
                        return DirectCompanionConnection.StreamingHttpResult(
                            200,
                            callNumber.toString().padStart(64, '0'),
                        )
                    }
                }

            val config =
                ContinuityPressureAdapter.Config(
                    stage = ContinuityPressureAdapter.Stage.quick,
                    scenarioRef =
                        "scenario.vexlife.continuity-pressure.test.provisional-recontact.v1",
                    scenarioFile = scenarioFile,
                    resultDir = resultDir,
                    strategy =
                        ContinuityPressureAdapter.Strategy.EFFECTIVE_SELECTION_BUDGET,
                    effectiveInputTokenBudget = 80_000,
                    sets = 1,
                    turnsPerSet = 1,
                    sourceSha = "1".repeat(40),
                    sourceClean = true,
                    runtimeHome = Path.of("/nonexistent/host/vex-home"),
                    experimentWindow = true,
                    responseCommitMode =
                        ContinuityPressureAdapter.ResponseCommitMode.PROVISIONAL_RECONTACT,
                )
            val lent =
                ContinuityPressureAdapter.LiveRuntime(
                    evidence = ContinuityPressureAdapter.FixtureEvidence,
                    observe = { ContinuityPressureAdapter.FixtureEvidence },
                    transport = stub,
                )

            assertEquals(0, ContinuityPressureAdapter.run(config) { lent })
            assertEquals(2, physicalBodies.size)

            val result =
                Json.parseToJsonElement(Files.readString(resultDir.resolve("result.json")))
                    .jsonObject
            assertEquals(
                "PROVISIONAL_RECONTACT",
                result.getValue("responseCommitMode").jsonPrimitive.content,
            )
            val turn = result.getValue("turns").jsonArray.single().jsonObject
            assertEquals("Second response.", turn.getValue("responseText").jsonPrimitive.content)

            val zipPath =
                Files.list(resultDir).use {
                    stream ->
                    stream.filter { it.toString().endsWith(".zip") }.findFirst().orElseThrow()
                }
            java.util.zip.ZipFile(zipPath.toFile()).use {
                zip ->
                val recontacts =
                    String(
                        zip.getInputStream(
                                zip.getEntry("implementation/provisional-recontacts.jsonl")
                            )
                            .readBytes()
                    )
                assertTrue(recontacts.contains("First proposal."))
                assertFalse(recontacts.contains("first-pass-private-reasoning"))
            }
        } finally {
            root.toFile().deleteRecursively()
        }
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
