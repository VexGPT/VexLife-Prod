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
