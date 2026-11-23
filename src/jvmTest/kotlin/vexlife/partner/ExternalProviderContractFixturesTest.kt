package vexlife.partner

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ExternalProviderContractFixturesTest {
    private val json = Json {
        ignoreUnknownKeys = false
    }

    private val root = "/vexlife/partner/provider-contract-fixtures/"

    private fun resource(relative: String): String =
        requireNotNull(
            ExternalProviderContractFixturesTest::class.java.getResourceAsStream(
                root + relative,
            ),
        ) {
            "missing provider contract fixture: $relative"
        }
            .bufferedReader()
            .use {
                it.readText()
            }

    private fun jsonObject(relative: String): JsonObject =
        json
            .parseToJsonElement(
                resource(relative),
            )
            .jsonObject

    private fun sseData(relative: String): List<JsonObject> =
        resource(relative)
            .lineSequence()
            .filter {
                it.startsWith("data:")
            }
            .map {
                it.removePrefix("data:").trim()
            }
            .filter {
                it.isNotEmpty() &&
                    it != "[DONE]"
            }
            .map {
                json.parseToJsonElement(it).jsonObject
            }
            .toList()

    private fun errorCases(relative: String): JsonArray =
        jsonObject(relative)
            .getValue("cases")
            .jsonArray

    @Test
    fun manifestKeepsSyntheticContractProofSeparateFromLiveQualification() {
        val manifest =
            jsonObject(
                "manifest.json",
            )

        assertEquals(
            "vexlife.external-provider-contract-fixtures/v1",
            manifest
                .getValue("schemaVersion")
                .jsonPrimitive
                .content,
        )
        assertFalse(
            manifest
                .getValue("liveProviderQualificationEstablished")
                .jsonPrimitive
                .boolean,
        )

        val fixtures =
            manifest
                .getValue("fixtures")
                .jsonArray
        assertEquals(
            10,
            fixtures.size,
        )
        fixtures.forEach { fixture ->
            assertEquals(
                "OFFICIAL_DOC_CONFORMANT_SYNTHETIC",
                fixture
                    .jsonObject
                    .getValue("provenance")
                    .jsonPrimitive
                    .content,
            )
            assertTrue(
                fixture
                    .jsonObject
                    .getValue("officialRefs")
                    .jsonArray
                    .isNotEmpty(),
            )
        }
    }

    @Test
    fun everyDeclaredFixtureIsPresentAndMachineReadable() {
        val fixtures =
            jsonObject(
                "manifest.json",
            )
                .getValue("fixtures")
                .jsonArray

        fixtures.forEach { fixture ->
            val path =
                fixture
                    .jsonObject
                    .getValue("path")
                    .jsonPrimitive
                    .content
            if (
                path.endsWith(
                    ".json",
                )
            ) {
                jsonObject(
                    path,
                )
            } else {
                assertTrue(
                    sseData(
                        path,
                    ).isNotEmpty(),
                    "SSE fixture has no JSON data events: $path",
                )
            }
        }
    }

    @Test
    fun openAiResponsesStreamRequiresTerminalCompletion() {
        val events =
            sseData(
                "openai/responses-text.stream.txt",
            )
        val types =
            events.map {
                it
                    .getValue("type")
                    .jsonPrimitive
                    .content
            }

        assertEquals(
            "response.created",
            types.first(),
        )
        assertTrue(
            "response.output_text.delta" in types,
        )
        assertEquals(
            "response.completed",
            types.last(),
        )
    }

    @Test
    fun openAiFunctionCallAndResultStayBoundByCallId() {
        val fixture =
            jsonObject(
                "openai/responses-tool.json",
            )
        val call =
            fixture
                .getValue("response")
                .jsonObject
                .getValue("output")
                .jsonArray
                .single()
                .jsonObject
        val output =
            fixture
                .getValue("continuationInput")
                .jsonArray
                .single()
                .jsonObject

        assertEquals(
            "function_call",
            call
                .getValue("type")
                .jsonPrimitive
                .content,
        )
        assertEquals(
            "function_call_output",
            output
                .getValue("type")
                .jsonPrimitive
                .content,
        )
        assertEquals(
            call
                .getValue("call_id")
                .jsonPrimitive
                .content,
            output
                .getValue("call_id")
                .jsonPrimitive
                .content,
        )
        json.parseToJsonElement(
            call
                .getValue("arguments")
                .jsonPrimitive
                .content,
        )
    }

    @Test
    fun anthropicTextResponseIsContentBlockShaped() {
        val message =
            jsonObject(
                "anthropic/messages-text.json",
            )
        val content =
            message
                .getValue("content")
                .jsonArray

        assertTrue(
            content.isNotEmpty(),
        )
        assertEquals(
            "text",
            content
                .first()
                .jsonObject
                .getValue("type")
                .jsonPrimitive
                .content,
        )
        assertEquals(
            "end_turn",
            message
                .getValue("stop_reason")
                .jsonPrimitive
                .content,
        )
    }

    @Test
    fun anthropicToolStreamAssemblesPartialJsonBeforeToolUseCompletion() {
        val events =
            sseData(
                "anthropic/messages-tool.stream.txt",
            )
        val toolStart =
            events
                .first {
                    it["type"]
                        ?.jsonPrimitive
                        ?.contentOrNull ==
                        "content_block_start" &&
                        it["content_block"]
                            ?.jsonObject
                            ?.get("type")
                            ?.jsonPrimitive
                            ?.contentOrNull ==
                        "tool_use"
                }
        val partialArguments =
            events
                .filter {
                    it["type"]
                        ?.jsonPrimitive
                        ?.contentOrNull ==
                        "content_block_delta" &&
                        it["delta"]
                            ?.jsonObject
                            ?.get("type")
                            ?.jsonPrimitive
                            ?.contentOrNull ==
                        "input_json_delta"
                }
                .joinToString(
                    separator = "",
                ) {
                    it
                        .getValue("delta")
                        .jsonObject
                        .getValue("partial_json")
                        .jsonPrimitive
                        .content
                }
        val parsedArguments =
            json
                .parseToJsonElement(
                    partialArguments,
                )
                .jsonObject

        assertEquals(
            "toolu_fixture",
            toolStart
                .getValue("content_block")
                .jsonObject
                .getValue("id")
                .jsonPrimitive
                .content,
        )
        assertEquals(
            "provider continuity",
            parsedArguments
                .getValue("terms")
                .jsonArray
                .single()
                .jsonPrimitive
                .content,
        )
        assertEquals(
            "message_stop",
            events
                .last()
                .getValue("type")
                .jsonPrimitive
                .content,
        )
        assertTrue(
            events.any {
                it["delta"]
                    ?.jsonObject
                    ?.get("stop_reason")
                    ?.jsonPrimitive
                    ?.contentOrNull ==
                    "tool_use"
            },
        )
    }

    @Test
    fun anthropicStreamMayFailAfterItAlreadyStarted() {
        val events =
            sseData(
                "anthropic/messages-error.stream.txt",
            )

        assertEquals(
            "message_start",
            events
                .first()
                .getValue("type")
                .jsonPrimitive
                .content,
        )
        assertEquals(
            "error",
            events
                .last()
                .getValue("type")
                .jsonPrimitive
                .content,
        )
        assertEquals(
            "overloaded_error",
            events
                .last()
                .getValue("error")
                .jsonObject
                .getValue("type")
                .jsonPrimitive
                .content,
        )
        assertFalse(
            events.any {
                it["type"]
                    ?.jsonPrimitive
                    ?.contentOrNull ==
                    "message_stop"
            },
        )
    }

    @Test
    fun kimiToolStreamRequiresChunkAssemblyAndTerminatesAsToolCalls() {
        val events =
            sseData(
                "kimi/chat-tool.stream.txt",
            )
        val toolChunks =
            events.flatMap { event ->
                event
                    .getValue("choices")
                    .jsonArray
                    .flatMap { choice ->
                        choice
                            .jsonObject
                            .getValue("delta")
                            .jsonObject["tool_calls"]
                            ?.jsonArray
                            ?.map {
                                it.jsonObject
                            }
                            ?: emptyList()
                    }
            }

        val first =
            toolChunks.first()
        val arguments =
            toolChunks
                .mapNotNull {
                    it["function"]
                        ?.jsonObject
                        ?.get("arguments")
                        ?.jsonPrimitive
                        ?.contentOrNull
                }
                .joinToString(
                    separator = "",
                )
        val parsed =
            json
                .parseToJsonElement(
                    arguments,
                )
                .jsonObject

        assertEquals(
            "call_fixture",
            first
                .getValue("id")
                .jsonPrimitive
                .content,
        )
        assertEquals(
            "memory_search",
            first
                .getValue("function")
                .jsonObject
                .getValue("name")
                .jsonPrimitive
                .content,
        )
        assertEquals(
            "provider continuity",
            parsed
                .getValue("terms")
                .jsonArray
                .single()
                .jsonPrimitive
                .content,
        )
        assertEquals(
            "tool_calls",
            events
                .last()
                .getValue("choices")
                .jsonArray
                .single()
                .jsonObject
                .getValue("finish_reason")
                .jsonPrimitive
                .content,
        )
    }

    @Test
    fun providerFailuresRetainMachineSpecificClassificationEvidence() {
        val openAi =
            errorCases(
                "openai/errors.json",
            )
        val anthropic =
            errorCases(
                "anthropic/errors.json",
            )
        val kimi =
            errorCases(
                "kimi/errors.json",
            )

        listOf(
            openAi,
            anthropic,
            kimi,
        ).forEach { cases ->
            cases.forEach { item ->
                val case =
                    item.jsonObject
                assertTrue(
                    case
                        .getValue("httpStatus")
                        .jsonPrimitive
                        .int >
                        0,
                )
                assertNotNull(
                    case
                        .getValue("expectedVexMeaning")
                        .jsonPrimitive
                        .contentOrNull,
                )
                assertNotNull(
                    case
                        .getValue("retryExpected")
                        .jsonPrimitive,
                )
            }
        }

        val kimi429Meanings =
            kimi
                .map {
                    it.jsonObject
                }
                .filter {
                    it
                        .getValue("httpStatus")
                        .jsonPrimitive
                        .int ==
                        429
                }
                .associate {
                    it
                        .getValue("body")
                        .jsonObject
                        .getValue("error")
                        .jsonObject
                        .getValue("type")
                        .jsonPrimitive
                        .content to
                        it
                            .getValue("expectedVexMeaning")
                            .jsonPrimitive
                            .content
                }

        assertEquals(
            "PROVIDER_OVERLOADED",
            kimi429Meanings["engine_overloaded_error"],
        )
        assertEquals(
            "ACCOUNT_OR_BILLING_HELD",
            kimi429Meanings["exceeded_current_quota_error"],
        )
        assertEquals(
            "RATE_LIMITED",
            kimi429Meanings["rate_limit_reached_error"],
        )
    }

    @Test
    fun fixtureResourcesContainNoCredentialMaterial() {
        val fixtures =
            jsonObject(
                "manifest.json",
            )
                .getValue("fixtures")
                .jsonArray

        fixtures.forEach { fixture ->
            val path =
                fixture
                    .jsonObject
                    .getValue("path")
                    .jsonPrimitive
                    .content
            val body =
                resource(
                    path,
                )
            assertFalse(
                body.contains(
                    "Authorization:",
                    ignoreCase = true,
                ),
                "fixture contains an authorization header: $path",
            )
            assertFalse(
                body.contains(
                    "x-api-key",
                    ignoreCase = true,
                ),
                "fixture contains an API-key header: $path",
            )
            assertFalse(
                body.contains(
                    "sk-",
                    ignoreCase = true,
                ),
                "fixture contains secret-like key material: $path",
            )
        }
    }
}

// [VXG RealForever]
