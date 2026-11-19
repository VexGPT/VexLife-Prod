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
import vexlife.model.heretic.HereticModelCatalog
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
                (message["content"]?.jsonPrimitive?.content ?: "")
        }

    private fun nativeToolReply(
        functionName: String,
        arguments: String,
    ) =
        DirectCompanionConnection.HttpResult(
            status = 200,
            body =
                """{"model":"default_model","choices":[{"finish_reason":"tool_calls","message":{"role":"assistant","content":null,"tool_calls":[{"id":"call_test","type":"function","function":{"name":""" +
                    Json.encodeToString(
                        kotlinx.serialization.json.JsonPrimitive.serializer(),
                        kotlinx.serialization.json.JsonPrimitive(functionName),
                    ) +
                    ""","arguments":""" +
                    Json.encodeToString(
                        kotlinx.serialization.json.JsonPrimitive.serializer(),
                        kotlinx.serialization.json.JsonPrimitive(arguments),
                    ) +
                    """}}]}}]}""",
        )

    private val publicResolver = VexLifeWebReach.Resolver {
        listOf(InetAddress.getByAddress(it, byteArrayOf(93, 184.toByte(), 215.toByte(), 14)))
    }

    private val page = VexLifeWebReach.Transport { request, _ ->
        assertEquals("https://example.com/kites", request.uri.toString())
        VexLifeWebReach.Response(
            status = 200,
            headers = mapOf("content-type" to listOf("text/html")),
            body =
                "<html><head><title>Kites</title></head><body><p>Fly the kite on a 30 m line.</p></body></html>"
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
        val requestBodies = mutableListOf<JsonObject>()
        val progress = mutableListOf<DirectCompanionTurn.Progress>()
        val transport = DirectCompanionConnection.Transport { call ->
            requestBodies += Json.parseToJsonElement(call.body).jsonObject
            calls += messages(call)
            when (calls.size) {
                1 ->
                    nativeToolReply(
                        functionName = "web_fetch",
                        arguments = """{"url":"https://example.com/kites"}""",
                    )
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
                            VexLifeCapabilityBridge.OwnerBindings(
                                webTransport = page,
                                resolver = publicResolver,
                            ),
                        progressObserver = { progress += it },
                        now = { Instant.parse("2026-11-14T15:00:00Z") },
                    ),
            )

        // Stable semantic guidance stays in the prompt; exact actions arrive through native tools.
        val firstSystem = calls[0].filter { it.first == "system" }.joinToString("\n") { it.second }
        assertTrue(firstSystem.contains("Provider-native tools declare the read routes"))
        assertFalse(firstSystem.contains("Now: AVAILABLE"))
        assertFalse(firstSystem.contains(VexLifeCapabilityRequest.RequestHeader))
        val firstCurrent =
            calls[0]
                .single {
                    it.first == "user" &&
                        it.second.startsWith(DirectPromptContext.CurrentStateHeader)
                }
                .second
        assertTrue(
            firstCurrent.contains(
                "Now: AVAILABLE — atlas, continuity, conversation, github, home",
            ),
        )
        val firstToolNames =
            requestBodies[0]
                .getValue("tools")
                .jsonArray
                .map {
                    it.jsonObject
                        .getValue("function")
                        .jsonObject
                        .getValue("name")
                        .jsonPrimitive
                        .content
                }
        assertTrue("web_fetch" in firstToolNames)
        assertTrue("memory_search" in firstToolNames)

        // Stage two reuses the base messages, then carries a native assistant tool call + tool result.
        assertEquals(calls[0], calls[1].dropLast(2))
        val stageTwoMessages = requestBodies[1].getValue("messages").jsonArray
        val nativeCall = stageTwoMessages[stageTwoMessages.size - 2].jsonObject
        assertEquals("assistant", nativeCall.getValue("role").jsonPrimitive.content)
        assertEquals(
            "web_fetch",
            nativeCall
                .getValue("tool_calls")
                .jsonArray
                .single()
                .jsonObject
                .getValue("function")
                .jsonObject
                .getValue("name")
                .jsonPrimitive
                .content,
        )
        val returned = calls[1].last()
        assertEquals("tool", returned.first)
        assertTrue(returned.second.contains("Fly the kite on a 30 m line."))
        assertTrue(returned.second.contains("\"state\":\"RETURNED\""))

        assertEquals("The page says to fly it on a 30 m line.", result.content)
        assertEquals(
            listOf(
                VexLifeCapabilityBridge.Use(
                    "web",
                    "fetch",
                    "https://example.com/kites",
                    VexLifeCapabilityBridge.ResultState.RETURNED,
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
                    it.reach?.state == VexLifeCapabilityBridge.ResultState.RETURNED
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
            VexLifeCapabilityBridge.ResultState.RETURNED,
            trace.entries.single().state,
        )
        assertTrue(trace.entries.single().resultSha256.matches(Regex("[0-9a-f]{64}")))
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
    fun aLongWebReadReturnsAContinuationOffsetAndTheNextRequestReachesLaterSource() =
        withHome {
            home ->
            val tail = "FINAL-PARAGRAPH-SENTINEL"
            val readable =
                "A".repeat(
                    50_000,
                ) +
                    tail
            val longPage =
                VexLifeWebReach.Transport {
                    request,
                    _,
                    ->
                    assertEquals(
                        "https://example.com/long",
                        request.uri.toString(),
                    )
                    VexLifeWebReach.Response(
                        status = 200,
                        headers =
                            mapOf(
                                "content-type" to
                                    listOf(
                                        "text/plain",
                                    ),
                            ),
                        body =
                            readable.toByteArray(
                                StandardCharsets.UTF_8,
                            ),
                        truncated = false,
                    )
                }

            fun fetch(
                requestNumber: Int,
                offset: Int? =
                    null,
            ): VexLifeCapabilityBridge.Exchange {
                val arguments =
                    buildString {
                        append(
                            """{"url":"https://example.com/long"""",
                        )
                        offset?.let {
                            append(
                                ""","offset":$it""",
                            )
                        }
                        append(
                            "}",
                        )
                    }
                val control =
                    VexLifeCapabilityRequest.RequestHeader +
                        "\n" +
                        """{"capability":"web","action":"fetch","arguments":$arguments}"""
                val parsed =
                    assertNotNull(
                        VexLifeCapabilityRequest.parseOrNull(
                            control,
                        ),
                    )
                return VexLifeCapabilityBridge.perform(
                    parsed = parsed,
                    admission =
                        VexLifeCapabilityBridge.Admission(
                            productHome = home,
                            availableCapabilityRefs =
                                setOf(
                                    "capability.vexlife.web.read",
                                ),
                            requestNumber = requestNumber,
                            tokensAvailable =
                                VexLifeCapabilityBridge.ExchangeAllowanceTokens,
                        ),
                    ownerBindings =
                        VexLifeCapabilityBridge.OwnerBindings(
                            webTransport = longPage,
                            resolver = publicResolver,
                        ),
                    estimateTokens = DirectPromptContext::estimatedMessageTokens,
                )
            }

            fun metadata(
                exchange: VexLifeCapabilityBridge.Exchange,
            ): JsonObject =
                Json
                    .parseToJsonElement(
                        exchange.resultMessage.content
                            .lineSequence()
                            .drop(
                                2,
                            )
                            .first(),
                    )
                    .jsonObject

            val first =
                fetch(
                    requestNumber = 1,
                )
            val firstMeta =
                metadata(
                    first,
                )
            assertEquals(
                "0",
                firstMeta
                    .getValue(
                        "returnedStart",
                    )
                    .jsonPrimitive
                    .content,
            )
            val nextOffset =
                firstMeta
                    .getValue(
                        "nextOffset",
                    )
                    .jsonPrimitive
                    .content
                    .toInt()
            assertTrue(
                nextOffset in 1 until readable.length,
            )
            assertEquals(
                readable.length.toString(),
                firstMeta
                    .getValue(
                        "sourceCharacters",
                    )
                    .jsonPrimitive
                    .content,
            )
            assertEquals(
                "true",
                firstMeta
                    .getValue(
                        "truncated",
                    )
                    .jsonPrimitive
                    .content,
            )
            assertFalse(
                first.resultMessage.content.contains(
                    tail,
                ),
            )

            val second =
                fetch(
                    requestNumber = 2,
                    offset = nextOffset,
                )
            val secondMeta =
                metadata(
                    second,
                )
            assertEquals(
                nextOffset.toString(),
                secondMeta
                    .getValue(
                        "returnedStart",
                    )
                    .jsonPrimitive
                    .content,
            )
            assertEquals(
                readable.length.toString(),
                secondMeta
                    .getValue(
                        "returnedEnd",
                    )
                    .jsonPrimitive
                    .content,
            )
            assertTrue(
                second.resultMessage.content.contains(
                    tail,
                ),
            )
            assertFalse(
                "nextOffset" in
                    secondMeta,
            )
        }

    @Test
    fun narratedLateRequestIsCorrectedInsideTheSameTurnInsteadOfEscapingAsAResult() = withHome { home ->
        val calls = mutableListOf<List<Pair<String, String>>>()
        val narrated =
            buildString {
                append("I can demonstrate that. First I will explain what I am about to do. ")
                append("x".repeat(700))
                append("\n")
                append(fetchControl)
                append("\nResult: the page says something I have not actually retrieved.")
            }
        val transport =
            DirectCompanionConnection.Transport { call ->
                calls += messages(call)
                when (calls.size) {
                    1 -> reply(narrated)
                    2 -> {
                        val returned = calls.last().last()
                        assertEquals("user", returned.first)
                        assertTrue(returned.second.startsWith(VexLifeCapabilityRequest.ResultHeader))
                        assertTrue(returned.second.contains("\"state\":\"INVALID_REQUEST\""))
                        assertTrue(returned.second.contains("whole reply for this provider stage"))
                        reply("I had not actually reached the page. The request needs to be emitted by itself.")
                    }
                    else -> error("unexpected provider call")
                }
            }

        val result =
            DirectCompanionTurn.performWithHooks(
                setup = setup(home),
                intent =
                    DirectCompanionTurn.Intent(
                        idempotencyKey = "send.reach.late-control.001",
                        content = "Demonstrate a web lookup.",
                    ),
                hooks =
                    DirectCompanionTurn.Hooks(
                        transport = transport,
                        capabilityOwners =
                            VexLifeCapabilityBridge.OwnerBindings(
                                webTransport = { _, _ -> error("late malformed request must not contact web") },
                                resolver = publicResolver,
                            ),
                    ),
            )

        assertEquals(2, calls.size)
        assertEquals(
            "I had not actually reached the page. The request needs to be emitted by itself.",
            result.content,
        )
        assertEquals(VexLifeCapabilityBridge.ResultState.INVALID_REQUEST, result.capabilityUses.single().state)
        assertFalse(result.content.contains("page says something"))
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
                            VexLifeCapabilityBridge.OwnerBindings(
                                webTransport = { _, _ -> error("never contacted") }
                            ),
                    ),
            )
        assertEquals(2, calls)
        assertEquals(VexLifeCapabilityBridge.ResultState.REFUSED, result.capabilityUses.single().state)
    }

    @Test
    fun exhaustedReachMayCloseAsAHumanContinuationInsteadOfFailingTheTurn() = withHome { home ->
        var calls = 0
        val listControl =
            VexLifeCapabilityRequest.RequestHeader +
                "\n" +
                """{"capability":"atlas","action":"list"}"""
        val continuation =
            "I understand the current shape, but I still need outside truth to finish confidently. " +
                "Send another message and I can continue from here with a fresh reach budget."

        val result =
            DirectCompanionTurn.performWithHooks(
                setup = setup(home),
                intent =
                    DirectCompanionTurn.Intent(
                        idempotencyKey = "send.reach.limit-human-continuation.001",
                        content = "Keep looking until you can answer confidently.",
                    ),
                hooks =
                    DirectCompanionTurn.Hooks(
                        transport = { call ->
                            calls++
                            if (calls <= VexLifeCapabilityBridge.MaximumRequestsPerTurn + 1) {
                                reply(listControl)
                            } else {
                                val returned = messages(call).last()
                                assertEquals("user", returned.first)
                                assertTrue(returned.second.contains("\"state\":\"LIMIT_REACHED\""))
                                assertTrue(returned.second.contains("Do not send another capability request"))
                                assertTrue(returned.second.contains("share what you currently understand"))
                                assertTrue(returned.second.contains("continue in a fresh turn"))
                                reply(continuation)
                            }
                        }
                    ),
            )

        assertEquals(VexLifeCapabilityBridge.MaximumRequestsPerTurn + 2, calls)
        assertEquals("TURN_COMPLETED", result.state)
        assertEquals(continuation, result.content)
        assertEquals(
            VexLifeCapabilityBridge.MaximumRequestsPerTurn + 1,
            result.capabilityUses.size,
        )
        assertEquals(
            VexLifeCapabilityBridge.ResultState.LIMIT_REACHED,
            result.capabilityUses.last().state,
        )
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

    /** The same turn with the gpt-oss model: setup bound to it, runtime proved for it. */
    private fun gptOssSetup(home: Path): VexLifeProdLocalSetup.Result {
        val entry = HereticModelCatalog.Models.GptOss20B
        val runtime =
            HereticModelConfiguration.QualifiedRuntimeEvidence(
                runtimeReceiptRef = "receipt.vexlife-prod.heretic.reach.gpt-oss",
                runtimeState = HereticRuntimeState.Qualified,
                modelRef = entry.modelRef,
                modelProfileRef = entry.identity.modelProfileRef,
                artifactRef = entry.identity.artifactSetRef,
                artifactContentSetSha256 = entry.identity.artifactContentSetSha256,
                runtimeAdapterRef = entry.identity.runtimeAdapterRef,
                runtimeClass = entry.identity.runtimeClass,
                endpoint = entry.identity.endpoint,
                requestModel = entry.identity.requestModel,
                qualification = HereticRuntimeQualification.qualified(),
            )
        return setup(home)
            .copy(
                qualifiedRuntimeEvidence = runtime,
                endpoint = runtime.endpoint,
                requestModel = runtime.requestModel,
            )
    }

    /** A harmony reply as the pinned mlx-lm server streams it: inline markers, small chunks. */
    private fun harmonyStream(replies: (Int, JsonObject) -> String) =
        object : DirectCompanionConnection.StreamingTransport {
            var calls = 0

            override fun post(call: DirectCompanionConnection.HttpCall) = error("streams")

            override fun postStreaming(
                call: DirectCompanionConnection.HttpCall,
                onLine: (String) -> Unit,
            ): DirectCompanionConnection.StreamingHttpResult {
                calls++
                val text = replies(calls, Json.parseToJsonElement(call.body).jsonObject)
                for (chunk in text.chunked(6)) {
                    onLine(
                        "data: " +
                            """{"model":"default_model","choices":[{"delta":{"role":"assistant","content":""" +
                            kotlinx.serialization.json.JsonPrimitive(chunk) +
                            "}}]}"
                    )
                }
                onLine("""data: {"model":"default_model","choices":[{"delta":{},"finish_reason":"stop"}]}""")
                onLine("data: [DONE]")
                return DirectCompanionConnection.StreamingHttpResult(200, "d".repeat(64))
            }
        }

    @Test
    fun gptOssUsesNativeHarmonyToolAndAnswersFromReturnedSource() = withHome { home ->
        val requests = mutableListOf<JsonObject>()
        val transport =
            harmonyStream { call, body ->
                requests += body
                when (call) {
                    1 ->
                        "<|channel|>analysis<|message|>They ask what the page says. I'll fetch it.<|end|>" +
                            "<|start|>assistant<|channel|>commentary to=functions.web_fetch <|constrain|>json" +
                            """<|message|>{"url":"https://example.com/kites"}"""

                    2 ->
                        "<|channel|>analysis<|message|>The returned page gives the line length.<|end|>" +
                            "<|start|>assistant<|channel|>final<|message|>The page says to fly the kite on a 30 m line."

                    else -> error("unexpected provider stage")
                }
            }

        val result =
            DirectCompanionTurn.performWithHooks(
                setup = gptOssSetup(home),
                intent =
                    DirectCompanionTurn.Intent(
                        idempotencyKey = "send.reach.gpt-oss.native.001",
                        content = "What does example.com say about kites?",
                    ),
                hooks =
                    DirectCompanionTurn.Hooks(
                        transport = transport,
                        capabilityOwners =
                            VexLifeCapabilityBridge.OwnerBindings(
                                webTransport = page,
                                resolver = publicResolver,
                            ),
                        now = { Instant.parse("2026-11-16T12:00:00Z") },
                    ),
            )

        assertEquals("The page says to fly the kite on a 30 m line.", result.content)
        assertEquals(2, requests.size)
        val toolNames =
            requests.first()
                .getValue("tools")
                .jsonArray
                .map {
                    it.jsonObject
                        .getValue("function")
                        .jsonObject
                        .getValue("name")
                        .jsonPrimitive
                        .content
                }
        assertTrue("web_fetch" in toolNames)
        assertFalse("builtin_tools" in requests.first().getValue("chat_template_kwargs").jsonObject)
        val secondMessages = requests[1].getValue("messages").jsonArray
        assertTrue(secondMessages.any { it.jsonObject["role"]?.jsonPrimitive?.content == "tool" })
        assertTrue(
            secondMessages.any {
                it.jsonObject["content"]
                    ?.jsonPrimitive
                    ?.content
                    ?.contains("Fly the kite on a 30 m line.") ==
                    true
            },
        )
        assertEquals(
            listOf(
                VexLifeCapabilityBridge.Use(
                    "web",
                    "fetch",
                    "https://example.com/kites",
                    VexLifeCapabilityBridge.ResultState.RETURNED,
                ),
            ),
            result.capabilityUses,
        )
    }
}

// [VXG RealForever]
