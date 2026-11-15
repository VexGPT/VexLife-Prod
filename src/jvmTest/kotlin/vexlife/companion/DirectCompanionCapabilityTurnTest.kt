package vexlife.companion

import java.net.InetAddress
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import vexlife.capability.VexLifeCapabilityBridge
import vexlife.capability.VexLifeCapabilityRequest
import vexlife.capability.reach.VexLifeWebReach
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.local.VexLifeProdLocalSetup
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticProductIdentity
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeState

/**
 * The lived threshold, end to end inside one human turn:
 *
 * Vex notices a need -> emits one inspectable request -> the rightful owner performs it -> the
 * typed result returns in the next stage -> Vex answers from it -> evidence records what happened.
 */
class DirectCompanionCapabilityTurnTest {
    private fun productHome(): Path {
        val parent = Files.createTempDirectory("vexlife-prod-capability-turn-").toAbsolutePath()
        val home = parent.resolve("VexLife-Prod")
        CanonicalVexLifeHomeFixture.form(
            home = home,
            deviceSuffix = "reach-device",
            lineageSuffix = "reach-lineage",
        )
        return home
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
                    runtimeReceiptRef = "receipt.vexlife-prod.heretic.reach",
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

    private fun reply(content: String) =
        DirectCompanionConnection.HttpResult(
            status = 200,
            body =
                buildString {
                    append(
                        """{"model":"default_model","choices":[{"message":{"role":"assistant","content":"""
                    )
                    append(
                        Json.encodeToString(
                            kotlinx.serialization.json.JsonPrimitive.serializer(),
                            kotlinx.serialization.json.JsonPrimitive(content),
                        )
                    )
                    append("}}]}")
                },
        )

    /** The provider messages of one call, as role/content pairs. */
    private fun messages(call: DirectCompanionConnection.HttpCall): List<Pair<String, String>> =
        Json.parseToJsonElement(call.body).jsonObject.getValue("messages").jsonArray.map {
            val message = it as JsonObject
            message.getValue("role").jsonPrimitive.content to
                message.getValue("content").jsonPrimitive.content
        }

    private val publicResolver = VexLifeWebReach.Resolver {
        listOf(InetAddress.getByAddress(it, byteArrayOf(93, 184.toByte(), 215.toByte(), 14)))
    }

    private val page = VexLifeWebReach.Transport { request, _ ->
        assertEquals("https://example.com/kites", request.uri.toString())
        VexLifeWebReach.Response(
            status = 200,
            headers = mapOf("content-type" to listOf("text/html")),
            body =
                "<html><title>Kites</title><body><p>Fly the kite on a 30 m line.</p></body></html>"
                    .toByteArray(StandardCharsets.UTF_8),
            truncated = false,
        )
    }

    private val fetchControl =
        VexLifeCapabilityRequest.RequestHeader +
            "\n" +
            """{"capability":"web","action":"fetch","arguments":{"url":"https://example.com/kites"}}"""

    private fun withHome(block: (Path) -> Unit) {
        val home = productHome()
        try {
            block(home)
        } finally {
            home.parent.toFile().deleteRecursively()
        }
    }

    @Test
    fun vexReachesTheWebAndAnswersFromWhatCameBackInTheSameTurn() = withHome { home ->
        val calls = mutableListOf<List<Pair<String, String>>>()
        val progress = mutableListOf<DirectCompanionTurn.Progress>()
        val transport = DirectCompanionConnection.Transport { call ->
            calls += messages(call)
            when (calls.size) {
                1 -> reply(fetchControl)
                2 -> reply("The page says to fly it on a 30 m line.")
                else -> error("unexpected provider call")
            }
        }
        val result =
            DirectCompanionTurn.performWithHooks(
                setup = setup(home),
                intent =
                    DirectCompanionTurn.Intent(
                        idempotencyKey = "send.reach.web.001",
                        content = "What does example.com say about kites?",
                    ),
                hooks =
                    DirectCompanionTurn.Hooks(
                        transport = transport,
                        capabilityOwners =
                            VexLifeCapabilityBridge.Owners(
                                webTransport = page,
                                resolver = publicResolver,
                            ),
                        progressObserver = { progress += it },
                        now = { Instant.parse("2026-11-14T15:00:00Z") },
                    ),
            )

        // Stage one was told what it can reach and exactly how to ask.
        val firstSystem = calls[0].filter { it.first == "system" }.joinToString("\n") { it.second }
        assertTrue(firstSystem.contains("What I can reach this turn, and how to ask for it."))
        assertTrue(firstSystem.contains("Now: AVAILABLE — atlas, conversation, github, home"))
        assertFalse(firstSystem.contains("\"capabilityEntries\""))
        assertTrue(firstSystem.contains("- web: Read the public web"))
        assertTrue(firstSystem.contains(VexLifeCapabilityRequest.RequestHeader))

        // Stage two is stage one plus Vex's own request and the host's typed result.
        assertEquals(calls[0], calls[1].dropLast(2))
        assertEquals("assistant" to fetchControl, calls[1][calls[1].size - 2])
        val returned = calls[1].last()
        assertEquals("user", returned.first)
        assertTrue(returned.second.startsWith(VexLifeCapabilityRequest.ResultHeader))
        assertTrue(returned.second.contains("Fly the kite on a 30 m line."))
        assertTrue(returned.second.contains("\"state\":\"RETURNED\""))

        assertEquals("The page says to fly it on a 30 m line.", result.content)
        assertEquals(
            listOf(
                VexLifeCapabilityBridge.Use(
                    "web",
                    "fetch",
                    "https://example.com/kites",
                    VexLifeCapabilityBridge.State.RETURNED,
                )
            ),
            result.capabilityUses,
        )

        // The person saw the reach happen, and never saw the control text as an answer.
        assertTrue(
            progress.any {
                it.phase == DirectCompanionTurn.ProgressPhase.Reaching && it.reach?.handle == "web"
            }
        )
        assertTrue(
            progress.any {
                it.phase == DirectCompanionTurn.ProgressPhase.Reached &&
                    it.reach?.state == VexLifeCapabilityBridge.State.RETURNED
            }
        )
        assertFalse(
            progress
                .filter { it.phase == DirectCompanionTurn.ProgressPhase.Responding }
                .joinToString("") { it.delta.orEmpty() }
                .contains(VexLifeCapabilityRequest.RequestHeader)
        )

        // Evidence: the prompt kept room for the exchange; the trace records hashes, not content.
        val receipt = result.promptContextReceipt
        assertEquals(
            VexLifeCapabilityBridge.ExchangeAllowanceTokens,
            receipt.capabilityExchangeAllowanceTokens,
        )
        assertEquals(DirectPromptContext.DirectTurnInputTokenBudget, receipt.inputTokenBudget)
        assertTrue(
            "capability.vexlife.web.read" in
                assertNotNull(receipt.capabilityFrameSnapshot).availableCapabilityRefs
        )
        val record = assertNotNull(DirectTurnContextStore(home).read(result.turnRef))
        val trace = assertNotNull(record.capabilityTrace)
        assertEquals(1, trace.entries.size)
        assertEquals(
            VexLifeCapabilityRequest.sha256(returned.second),
            trace.entries.single().resultSha256,
        )
        assertFalse(trace.rawResultContentPersisted)
        val evidenceText =
            Files.walk(home.resolve("runtime/vexlife-prod/turn-evidence")).use { paths ->
                paths
                    .filter { Files.isRegularFile(it) }
                    .map { Files.readString(it) }
                    .toList()
                    .joinToString("\n")
            }
        assertTrue(evidenceText.contains("vexlife-prod.turn-runtime-evidence/v3"))
        assertFalse(evidenceText.contains("Fly the kite on a 30 m line."))

        // A repeated send of the same attempt returns the durable result and its reach, without
        // calling the provider or the web again.
        val again =
            DirectCompanionTurn.performWithHooks(
                setup = setup(home),
                intent =
                    DirectCompanionTurn.Intent(
                        idempotencyKey = "send.reach.web.001",
                        content = "What does example.com say about kites?",
                    ),
                hooks =
                    DirectCompanionTurn.Hooks(
                        transport = { error("no provider call for a completed duplicate") }
                    ),
            )
        assertTrue(again.recoveredCompletedTurn)
        assertEquals(result.capabilityUses, again.capabilityUses)
    }

    @Test
    fun aTurnWithoutRequestsAnswersOnceAndKeepsTheV2Evidence() = withHome { home ->
        var calls = 0
        val result =
            DirectCompanionTurn.performWithHooks(
                setup = setup(home),
                intent =
                    DirectCompanionTurn.Intent(
                        idempotencyKey = "send.reach.none.001",
                        content = "Hello",
                    ),
                hooks =
                    DirectCompanionTurn.Hooks(
                        transport = {
                            calls++
                            reply("Hi.")
                        }
                    ),
            )
        assertEquals(1, calls)
        assertTrue(result.capabilityUses.isEmpty())
        assertEquals(
            null,
            assertNotNull(DirectTurnContextStore(home).read(result.turnRef)).capabilityTrace,
        )
    }

    @Test
    fun aRequestTheHostCannotPerformStillReturnsAndVexStillAnswers() = withHome { home ->
        var calls = 0
        val loopbackControl =
            VexLifeCapabilityRequest.RequestHeader +
                "\n" +
                """{"capability":"web","action":"fetch","arguments":{"url":"http://127.0.0.1:18210/api/v1/state"}}"""
        val result =
            DirectCompanionTurn.performWithHooks(
                setup = setup(home),
                intent =
                    DirectCompanionTurn.Intent(
                        idempotencyKey = "send.reach.refused.001",
                        content = "Read my local service",
                    ),
                hooks =
                    DirectCompanionTurn.Hooks(
                        transport = { call ->
                            calls++
                            if (calls == 1) reply(loopbackControl)
                            else {
                                assertTrue(
                                    messages(call).last().second.contains("\"state\":\"REFUSED\"")
                                )
                                reply(
                                    "That address is on this computer, so I can't read it through the web."
                                )
                            }
                        },
                        capabilityOwners =
                            VexLifeCapabilityBridge.Owners(
                                webTransport = { _, _ -> error("never contacted") }
                            ),
                    ),
            )
        assertEquals(2, calls)
        assertEquals(VexLifeCapabilityBridge.State.REFUSED, result.capabilityUses.single().state)
    }

    @Test
    fun requestsAreBoundedPerTurnAndOneTooManyEndsTheTurnVisibly() = withHome { home ->
        var calls = 0
        val listControl =
            VexLifeCapabilityRequest.RequestHeader +
                "\n" +
                """{"capability":"atlas","action":"list"}"""
        val failure =
            assertFailsWith<DirectCompanionTurn.TurnException> {
                DirectCompanionTurn.performWithHooks(
                    setup = setup(home),
                    intent =
                        DirectCompanionTurn.Intent(
                            idempotencyKey = "send.reach.limit.001",
                            content = "Keep looking",
                        ),
                    hooks =
                        DirectCompanionTurn.Hooks(
                            transport = {
                                calls++
                                reply(listControl)
                            }
                        ),
                )
            }
        // Four admitted requests, one answered LIMIT_REACHED, then the sixth stage still asked.
        assertEquals(VexLifeCapabilityBridge.MaximumRequestsPerTurn + 2, calls)
        assertNotNull(failure.failureReceipt)
    }
}

// [VXG RealForever]
