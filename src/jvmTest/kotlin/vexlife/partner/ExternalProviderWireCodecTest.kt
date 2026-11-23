package vexlife.partner

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import vexlife.companion.CompanionProviderMessage
import vexlife.companion.ProviderCapabilityTools
import vexlife.contract.VexContractJson
import vexlife.partner.ExternalProviderWire.Event
import vexlife.partner.ExternalProviderWire.FailureMeaning
import vexlife.partner.ExternalProviderWire.PreparedRequest
import vexlife.partner.ExternalProviderWire.RetryDisposition

class ExternalProviderWireCodecTest {
    private val root =
        "/vexlife/partner/provider-contract-fixtures/"

    private fun resource(
        relative: String,
    ): String =
        requireNotNull(
            ExternalProviderWireCodecTest::class.java.getResourceAsStream(
                root + relative,
            ),
        ) {
            "missing provider contract fixture: $relative"
        }
            .bufferedReader()
            .use {
                it.readText()
            }

    private fun jsonObject(
        text: String,
    ): JsonObject =
        VexContractJson.Strict
            .parseToJsonElement(
                text,
            )
            .jsonObject

    private fun readTool(): ProviderCapabilityTools.Tool =
        ProviderCapabilityTools.Tool(
            function =
                ProviderCapabilityTools.Function(
                    name = "memory_search",
                    description = "Search the currently admitted Vex Memory projection.",
                    parameters =
                        ProviderCapabilityTools.Parameters(
                            properties =
                                mapOf(
                                    "terms" to
                                        ProviderCapabilityTools.Property(
                                            type = "array",
                                            description = "Terms to search for.",
                                            items =
                                                ProviderCapabilityTools.Property(
                                                    type = "string",
                                                    description = "One search term.",
                                                ),
                                        ),
                                ),
                            required =
                                listOf(
                                    "terms",
                                ),
                        ),
                ),
        )

    private fun prepared(): PreparedRequest =
        PreparedRequest(
            model = "provider-model-ref-fixture",
            messages =
                listOf(
                    CompanionProviderMessage(
                        role = "system",
                        content = "You are Vex. Capability meaning and effects remain VexLife-owned.",
                    ),
                    CompanionProviderMessage(
                        role = "user",
                        content = "Use the smallest Vex-owned read capability if source is required.",
                    ),
                ),
            maxOutputTokens = 256,
            tools =
                listOf(
                    readTool(),
                ),
        )

    @Test
    fun requestEncodingPreservesOneSemanticToolAcrossThreeProviderGrammars() {
        val openAi =
            jsonObject(
                OpenAiResponsesWire.encodeRequest(
                    prepared(),
                ),
            )
        val anthropic =
            jsonObject(
                AnthropicMessagesWire.encodeRequest(
                    prepared(),
                ),
            )
        val kimi =
            jsonObject(
                KimiChatWire.encodeRequest(
                    prepared(),
                ),
            )

        assertEquals(
            "You are Vex. Capability meaning and effects remain VexLife-owned.",
            openAi
                .getValue("instructions")
                .jsonPrimitive
                .content,
        )
        assertEquals(
            "You are Vex. Capability meaning and effects remain VexLife-owned.",
            anthropic
                .getValue("system")
                .jsonPrimitive
                .content,
        )
        assertEquals(
            "system",
            kimi
                .getValue("messages")
                .jsonArray
                .first()
                .jsonObject
                .getValue("role")
                .jsonPrimitive
                .content,
        )

        assertEquals(
            "memory_search",
            openAi
                .getValue("tools")
                .jsonArray
                .single()
                .jsonObject
                .getValue("name")
                .jsonPrimitive
                .content,
        )
        assertTrue(
            openAi
                .getValue("tools")
                .jsonArray
                .single()
                .jsonObject
                .getValue("strict")
                .jsonPrimitive
                .boolean,
        )
        assertFalse(
            openAi
                .getValue("parallel_tool_calls")
                .jsonPrimitive
                .boolean,
        )

        assertEquals(
            "memory_search",
            anthropic
                .getValue("tools")
                .jsonArray
                .single()
                .jsonObject
                .getValue("name")
                .jsonPrimitive
                .content,
        )
        assertTrue(
            anthropic
                .getValue("tool_choice")
                .jsonObject
                .getValue("disable_parallel_tool_use")
                .jsonPrimitive
                .boolean,
        )

        assertEquals(
            "memory_search",
            kimi
                .getValue("tools")
                .jsonArray
                .single()
                .jsonObject
                .getValue("function")
                .jsonObject
                .getValue("name")
                .jsonPrimitive
                .content,
        )

        listOf(
            openAi,
            anthropic,
            kimi,
        ).forEach { request ->
            val encoded =
                request.toString()
            assertFalse(
                encoded.contains(
                    "Authorization",
                    ignoreCase = true,
                ),
            )
            assertFalse(
                encoded.contains(
                    "x-api-key",
                    ignoreCase = true,
                ),
            )
            assertFalse(
                encoded.contains(
                    "sk-",
                    ignoreCase = true,
                ),
            )
        }
    }

    @Test
    fun openAiResponsesStreamNormalizesTextAndTerminalCompletion() {
        val events =
            OpenAiResponsesWire.decodeStream(
                resource(
                    "openai/responses-text.stream.txt",
                ),
            )

        assertEquals(
            listOf(
                "Hello",
                " from Vex",
            ),
            events
                .filterIsInstance<Event.TextDelta>()
                .map {
                    it.text
                },
        )
        assertEquals(
            "completed",
            events
                .filterIsInstance<Event.Completed>()
                .single()
                .finishReason,
        )
        assertTrue(
            events.none {
                it is Event.Failed
            },
        )
    }

    @Test
    fun openAiFunctionCallKeepsProviderCallIdentity() {
        val fixture =
            jsonObject(
                resource(
                    "openai/responses-tool.json",
                ),
            )
        val events =
            OpenAiResponsesWire.decodeResponseBody(
                fixture
                    .getValue("response")
                    .toString(),
            )
        val call =
            events
                .filterIsInstance<Event.ToolCallReady>()
                .single()
                .call

        assertEquals(
            "call_fixture",
            call.providerCallRef,
        )
        assertEquals(
            "memory_search",
            call.name,
        )
        assertEquals(
            "provider continuity",
            call.arguments
                .getValue("terms")
                .jsonArray
                .single()
                .jsonPrimitive
                .content,
        )
    }

    @Test
    fun anthropicStreamAssemblesToolInputAndKeepsToolUseId() {
        val events =
            AnthropicMessagesWire.decodeStream(
                resource(
                    "anthropic/messages-tool.stream.txt",
                ),
            )
        val call =
            events
                .filterIsInstance<Event.ToolCallReady>()
                .single()
                .call

        assertEquals(
            "toolu_fixture",
            call.providerCallRef,
        )
        assertEquals(
            "memory_search",
            call.name,
        )
        assertEquals(
            "provider continuity",
            call.arguments
                .getValue("terms")
                .jsonArray
                .single()
                .jsonPrimitive
                .content,
        )
        assertEquals(
            "tool_use",
            events
                .filterIsInstance<Event.Completed>()
                .single()
                .finishReason,
        )
    }

    @Test
    fun anthropicErrorAfterStreamStartDoesNotBecomeCompletion() {
        val events =
            AnthropicMessagesWire.decodeStream(
                resource(
                    "anthropic/messages-error.stream.txt",
                ),
            )
        val failure =
            events
                .filterIsInstance<Event.Failed>()
                .single()
                .failure

        assertEquals(
            FailureMeaning.PROVIDER_OVERLOADED,
            failure.meaning,
        )
        assertEquals(
            RetryDisposition.RETRYABLE,
            failure.retry,
        )
        assertTrue(
            events.none {
                it is Event.Completed
            },
        )
    }

    @Test
    fun kimiStreamAssemblesArgumentsAndKeepsToolCallId() {
        val events =
            KimiChatWire.decodeStream(
                resource(
                    "kimi/chat-tool.stream.txt",
                ),
            )
        val call =
            events
                .filterIsInstance<Event.ToolCallReady>()
                .single()
                .call

        assertEquals(
            "call_fixture",
            call.providerCallRef,
        )
        assertEquals(
            "memory_search",
            call.name,
        )
        assertEquals(
            "provider continuity",
            call.arguments
                .getValue("terms")
                .jsonArray
                .single()
                .jsonPrimitive
                .content,
        )
        assertEquals(
            "tool_calls",
            events
                .filterIsInstance<Event.Completed>()
                .single()
                .finishReason,
        )
    }

    @Test
    fun documentedFailureFixturesNormalizeWithoutCollapsingSameHttpStatus() {
        assertFailureFixture(
            provider = "openai",
            classify = OpenAiResponsesWire::classifyFailure,
        )
        assertFailureFixture(
            provider = "anthropic",
            classify = AnthropicMessagesWire::classifyFailure,
        )
        assertFailureFixture(
            provider = "kimi",
            classify = KimiChatWire::classifyFailure,
        )
    }

    @Test
    fun kimi429RetainsOverloadQuotaAndRateAsDifferentMeanings() {
        val cases =
            jsonObject(
                resource(
                    "kimi/errors.json",
                ),
            )
                .getValue("cases")
                .jsonArray
                .map {
                    it.jsonObject
                }

        val meanings =
            cases
                .filter {
                    it
                        .getValue("httpStatus")
                        .jsonPrimitive
                        .content
                        .toInt() ==
                        429
                }
                .associate { case ->
                    val type =
                        case
                            .getValue("body")
                            .jsonObject
                            .getValue("error")
                            .jsonObject
                            .getValue("type")
                            .jsonPrimitive
                            .content
                    val failure =
                        KimiChatWire.classifyFailure(
                            httpStatus = 429,
                            body =
                                case
                                    .getValue("body")
                                    .toString(),
                        )
                    type to failure.meaning
                }

        assertEquals(
            FailureMeaning.PROVIDER_OVERLOADED,
            meanings[
                "engine_overloaded_error"
            ],
        )
        assertEquals(
            FailureMeaning.ACCOUNT_OR_BILLING_HELD,
            meanings[
                "exceeded_current_quota_error"
            ],
        )
        assertEquals(
            FailureMeaning.RATE_LIMITED,
            meanings[
                "rate_limit_reached_error"
            ],
        )
    }

    @Test
    fun malformedProviderArgumentsFailAsResponseInvalidInsteadOfInventingAToolCall() {
        val stream =
            """
            event: content_block_start
            data: {"type":"content_block_start","index":0,"content_block":{"type":"tool_use","id":"toolu_bad","name":"memory_search","input":{}}}

            event: content_block_delta
            data: {"type":"content_block_delta","index":0,"delta":{"type":"input_json_delta","partial_json":"{\"terms\":["}}

            event: content_block_stop
            data: {"type":"content_block_stop","index":0}

            event: message_stop
            data: {"type":"message_stop"}

            """.trimIndent()

        val events =
            AnthropicMessagesWire.decodeStream(
                stream,
            )
        assertEquals(
            FailureMeaning.RESPONSE_INVALID,
            events
                .filterIsInstance<Event.Failed>()
                .single()
                .failure
                .meaning,
        )
        assertTrue(
            events.none {
                it is Event.ToolCallReady
            },
        )
    }

    private fun assertFailureFixture(
        provider: String,
        classify: (Int, String) -> ExternalProviderWire.Failure,
    ) {
        val cases =
            jsonObject(
                resource(
                    "$provider/errors.json",
                ),
            )
                .getValue("cases")
                .jsonArray

        cases.forEach { element ->
            val case =
                element.jsonObject
            val status =
                case
                    .getValue("httpStatus")
                    .jsonPrimitive
                    .content
                    .toInt()
            val expected =
                FailureMeaning.valueOf(
                    case
                        .getValue("expectedVexMeaning")
                        .jsonPrimitive
                        .content,
                )
            val retryExpected =
                case
                    .getValue("retryExpected")
                    .jsonPrimitive
                    .boolean
            val observed =
                classify(
                    status,
                    case
                        .getValue("body")
                        .toString(),
                )

            assertEquals(
                expected,
                observed.meaning,
                case
                    .getValue("ref")
                    .jsonPrimitive
                    .content,
            )
            assertEquals(
                if (retryExpected) {
                    RetryDisposition.RETRYABLE
                } else {
                    RetryDisposition.DO_NOT_RETRY_UNCHANGED
                },
                observed.retry,
                case
                    .getValue("ref")
                    .jsonPrimitive
                    .content,
            )
        }
    }
}

// [VXG RealForever]
