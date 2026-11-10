package vexlife.memory

import java.nio.file.Files
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import vexlife.contract.VexContractJson

class VexLifeDailyMemoryDreamTest {
    private val observedAt = Instant.parse("2026-11-10T07:15:00Z")

    private fun binding(statementRef: String, accepted: Boolean): JsonObject =
        buildJsonObject {
            put("statementRef", statementRef)
            put("subjectRef", "subject.$statementRef")
            put("semanticSubjectFingerprint", (if (accepted) "1" else "2").repeat(64))
            put("memoryRelation", "CURRENT_LINEAGE_AUTOBIOGRAPHY")
            put("statementState", "HUMAN_CONFIRMED")
            put("summaryHash", (if (accepted) "3" else "4").repeat(64))
            put("acceptedForContinuity", accepted)
            put("consentState", if (accepted) "PERMITTED" else "DEFERRED")
            put("semanticAuthorityHeadSha256", "5".repeat(64))
            put("semanticAcceptanceRef", "acceptance.$statementRef")
            put("semanticAcceptanceSha256", (if (accepted) "6" else "7").repeat(64))
            put("semanticCandidateRef", "candidate.$statementRef")
            put("semanticCandidateSha256", (if (accepted) "8" else "9").repeat(64))
            put("classificationEvidenceBindings", JsonArray(emptyList()))
            put("consentDispositionRef", "consent.$statementRef")
            put("consentDispositionSha256", "a".repeat(64))
            put("sourceBindings", JsonArray(emptyList()))
            put("eventRef", "score-event.$statementRef")
            put("eventHash", "b".repeat(64))
            put("current", true)
        }

    private fun loop(): JsonObject =
        buildJsonObject {
            put("openLoopRef", "loop.g03.one")
            put("sourceStatementRefs", JsonArray(listOf(JsonPrimitive("statement.g03.active"))))
            put("sourceBindings", JsonArray(emptyList()))
            put("eventRef", "score-event.loop.g03.one")
            put("eventHash", "c".repeat(64))
            put("state", "OPEN")
        }

    private fun frontier(
        prior: VexLifeCurrentMemoryReader.DreamPriorDaily? = null,
    ): VexLifeCurrentMemoryReader.DreamSourceFrontier =
        VexLifeCurrentMemoryReader.DreamSourceFrontier(
            homeRef = "vex-home.device.vexlife.test",
            deviceRef = "device.vexlife.test",
            companionLineageRef = "companion-lineage.vexlife.test",
            threadRef = "thread.self-development.open-conversation",
            conversationHeadSha256 = "d".repeat(64),
            conversationResponseEventHash = "e".repeat(64),
            scoreHeadSha256 = "f".repeat(64),
            scoreEventHash = "0".repeat(64),
            semanticAuthorityHeadSha256 = "5".repeat(64),
            selectedRuntimeRef = "endpoint.vexlife.heretic.local",
            selectedModelProfileRef = "default_model",
            currentStatementRefs = listOf("statement.g03.active", "statement.g03.held"),
            activeStatementBindings = listOf(binding("statement.g03.active", true)),
            heldStatementBindings = listOf(binding("statement.g03.held", false)),
            openLoopBindings = listOf(loop()),
            priorDaily = prior,
        )

    private fun invocation(
        expectedDaily: String? = null,
    ): VexLifeDailyMemoryDream.Invocation =
        VexLifeDailyMemoryDream.Invocation(
            instanceRef = "instance.vexlife.g03.test",
            restInvocationAuthorityRef = "authority.manual.g03.test",
            dayRef = "day.g03.000",
            dayIndex = 0,
            calendarDateRef = "2026-11-10",
            timeZoneRef = "America/Los_Angeles",
            observedAt = observedAt,
            expectedConversationHeadSha256 = "d".repeat(64),
            expectedScoreHeadSha256 = "f".repeat(64),
            expectedDailyDreamHeadSha256 = expectedDaily,
        )

    @Test
    fun manualMemoryOnlyDreamCommitsReferenceFrontierAndHoldsLaterEffects() {
        val home = Files.createTempDirectory("vexlife-prod-g03-").toAbsolutePath()
        try {
            val source = frontier()
            val result =
                VexLifeDailyMemoryDream.commitWithFrontier(
                    productHome = home,
                    invocation = invocation(),
                    sourceProvider = { source },
                )
            assertEquals("COMMITTED", result.state)
            assertEquals(listOf("statement.g03.active"), result.activeStatementRefs)
            assertEquals(listOf("statement.g03.held"), result.heldStatementRefs)
            assertEquals(listOf("loop.g03.one"), result.openLoopRefs)
            assertFalse(result.lineageAwareGenerativeDreamRan)
            assertFalse(result.rhythmLearned)
            assertFalse(result.trainingRan)
            assertFalse(result.modelWeightsChanged)
            assertFalse(result.adapterChanged)
            assertFalse(result.synchronizationActivated)
            assertFalse(result.publicationPerformed)
            assertFalse(result.firstPersonAuthorityGranted)

            val root =
                home.resolve(
                    "daily-memory-dream/companion-lineage.vexlife.test/" +
                        "thread.self-development.open-conversation",
                )
            val head =
                VexContractJson.Strict
                    .parseToJsonElement(Files.readString(root.resolve("head.json")))
                    .jsonObject
            assertEquals(
                result.dailyDreamHeadSha256,
                head["dailyDreamHeadSha256"]?.jsonPrimitive?.content,
            )
            assertTrue(
                Files.exists(
                    root.resolve("strata/" + result.dailyStratumSha256 + ".json"),
                ),
            )

            val consolidationFiles = Files.list(root.resolve("consolidations")).use { it.toList() }
            assertEquals(1, consolidationFiles.size)
            val consolidation =
                VexContractJson.Strict
                    .parseToJsonElement(Files.readString(consolidationFiles.single()))
                    .jsonObject
            assertEquals(
                "false",
                consolidation["newSemanticAcceptanceCreated"]?.jsonPrimitive?.content,
            )
            assertEquals(
                "false",
                consolidation["rawConversationContentIncluded"]?.jsonPrimitive?.content,
            )
        } finally {
            home.toFile().deleteRecursively()
        }
    }

    @Test
    fun staleSourceExpectationFailsBeforeDailyMutation() {
        val home = Files.createTempDirectory("vexlife-prod-g03-stale-").toAbsolutePath()
        try {
            val bad = invocation().copy(expectedConversationHeadSha256 = "1".repeat(64))
            assertFailsWith<IllegalArgumentException> {
                VexLifeDailyMemoryDream.commitWithFrontier(
                    productHome = home,
                    invocation = bad,
                    sourceProvider = { frontier() },
                )
            }
            assertFalse(
                Files.exists(
                    home.resolve(
                        "daily-memory-dream/companion-lineage.vexlife.test/" +
                            "thread.self-development.open-conversation/head.json",
                    ),
                ),
            )
        } finally {
            home.toFile().deleteRecursively()
        }
    }

    @Test
    fun durableUncommittedTailCanBeRetriedExactly() {
        val home = Files.createTempDirectory("vexlife-prod-g03-tail-").toAbsolutePath()
        try {
            val source = frontier()
            assertFailsWith<IllegalStateException> {
                VexLifeDailyMemoryDream.commitWithFrontier(
                    productHome = home,
                    invocation = invocation(),
                    faults = VexLifeDailyMemoryDream.Faults(failAfterStratumWrite = true),
                    sourceProvider = { source },
                )
            }
            val completed =
                VexLifeDailyMemoryDream.commitWithFrontier(
                    productHome = home,
                    invocation = invocation(),
                    sourceProvider = { source },
                )
            assertEquals("COMMITTED", completed.state)
        } finally {
            home.toFile().deleteRecursively()
        }
    }
}

// [VXG RealForever]
