package vexlife.partner

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import vexlife.companion.ProviderCapabilityTools
import vexlife.contract.VexContractJson
import vexlife.partner.ExternalProviderWire.Event
import vexlife.partner.ExternalProviderWire.Failure
import vexlife.partner.ExternalProviderWire.FailureMeaning
import vexlife.partner.ExternalProviderWire.PreparedRequest
import vexlife.partner.ExternalProviderWire.Provider
import vexlife.partner.ExternalProviderWire.RetryDisposition
import vexlife.partner.ExternalProviderWire.ToolCall

/** Pure Anthropic Messages representation. No HTTP or credential handling lives here. */
object AnthropicMessagesWire {
    private data class PendingToolCall(
        val providerCallRef: String,
        val name: String,
        val arguments: StringBuilder,
    )

    fun encodeRequest(
        request: PreparedRequest,
    ): String {
        val (system, turnMessages) =
            ExternalProviderWire.splitLeadingSystem(
                request.messages,
            )

        val body =
            JsonObject(
                linkedMapOf<String, JsonElement>().apply {
                    put(
                        "model",
                        JsonPrimitive(request.model),
                    )
                    put(
                        "max_tokens",
                        JsonPrimitive(request.maxOutputTokens),
                    )
                    system
                        ?.let {
                            put(
                                "system",
                                JsonPrimitive(it),
                            )
                        }
                    put(
                        "messages",
                        JsonArray(
                            turnMessages.map(
                                ExternalProviderWire::messageJson,
                            ),
                        ),
                    )
                    put(
                        "stream",
                        JsonPrimitive(true),
                    )
                    if (
                        request.tools.isNotEmpty()
                    ) {
                        put(
                            "tools",
                            JsonArray(
                                request.tools.map(
                                    ExternalProviderWire::anthropicTool,
                                ),
                            ),
                        )
                        put(
                            "tool_choice",
                            JsonObject(
                                linkedMapOf(
                                    "type" to JsonPrimitive("auto"),
                                    "disable_parallel_tool_use" to JsonPrimitive(true),
                                ),
                            ),
                        )
                    }
                },
            )

        return VexContractJson.canonicalEncodeElement(
            body,
        )
    }

    fun encodeToolResult(
        call: ToolCall,
        semanticResult: String,
    ): JsonObject =
        JsonObject(
            linkedMapOf(
                "type" to JsonPrimitive("tool_result"),
                "tool_use_id" to JsonPrimitive(call.providerCallRef),
                "content" to
                    JsonPrimitive(
                        ProviderCapabilityTools.providerResult(
                            semanticResult,
                        ),
                    ),
            ),
        )

    fun decodeStream(
        text: String,
    ): List<Event> {
        val output = mutableListOf<Event>()
        val pending = linkedMapOf<Int, PendingToolCall>()
        var stopReason: String? = null
        var completed = false

        ExternalProviderWire
            .parseSse(
                text,
            )
            .forEach { sse ->
                val event =
                    runCatching {
                        VexContractJson.Strict
                            .parseToJsonElement(
                                sse.data,
                            )
                            .jsonObject
                    }
                        .getOrElse {
                            output +=
                                Event.Failed(
                                    invalidResponse(
                                        "Anthropic SSE data is not valid JSON",
                                    ),
                                )
                            return@forEach
                        }
                val type =
                    event[
                        "type"
                    ]
                        ?.jsonPrimitive
                        ?.contentOrNull

                when (type) {
                    "content_block_start" -> {
                        val index =
                            event[
                                "index"
                            ]
                                ?.jsonPrimitive
                                ?.contentOrNull
                                ?.toIntOrNull()
                        val block =
                            event[
                                "content_block"
                            ] as? JsonObject
                        if (
                            index !=
                                null &&
                                block
                                    ?.get(
                                        "type",
                                    )
                                    ?.jsonPrimitive
                                    ?.contentOrNull ==
                                "tool_use"
                        ) {
                            val id =
                                block[
                                    "id"
                                ]
                                    ?.jsonPrimitive
                                    ?.contentOrNull
                            val name =
                                block[
                                    "name"
                                ]
                                    ?.jsonPrimitive
                                    ?.contentOrNull
                            if (
                                !id.isNullOrBlank() &&
                                    !name.isNullOrBlank()
                            ) {
                                val initial =
                                    (
                                        block[
                                            "input"
                                        ] as? JsonObject
                                    )
                                        ?.takeIf {
                                            it.isNotEmpty()
                                        }
                                        ?.let(
                                            VexContractJson::canonicalEncodeElement,
                                        )
                                        .orEmpty()
                                pending[
                                    index
                                ] =
                                    PendingToolCall(
                                        providerCallRef = id,
                                        name = name,
                                        arguments =
                                            StringBuilder(
                                                initial,
                                            ),
                                    )
                            }
                        }
                    }

                    "content_block_delta" -> {
                        val delta =
                            event[
                                "delta"
                            ] as? JsonObject
                        when (
                            delta
                                ?.get(
                                    "type",
                                )
                                ?.jsonPrimitive
                                ?.contentOrNull
                        ) {
                            "text_delta" ->
                                delta[
                                    "text"
                                ]
                                    ?.jsonPrimitive
                                    ?.contentOrNull
                                    ?.takeIf {
                                        it.isNotEmpty()
                                    }
                                    ?.let {
                                        output +=
                                            Event.TextDelta(
                                                it,
                                            )
                                    }

                            "input_json_delta" -> {
                                val index =
                                    event[
                                        "index"
                                    ]
                                        ?.jsonPrimitive
                                        ?.contentOrNull
                                        ?.toIntOrNull()
                                val piece =
                                    delta[
                                        "partial_json"
                                    ]
                                        ?.jsonPrimitive
                                        ?.contentOrNull
                                if (
                                    index !=
                                        null &&
                                        piece !=
                                        null
                                ) {
                                    pending[
                                        index
                                    ]
                                        ?.arguments
                                        ?.append(
                                            piece,
                                        )
                                }
                            }
                        }
                    }

                    "content_block_stop" -> {
                        val index =
                            event[
                                "index"
                            ]
                                ?.jsonPrimitive
                                ?.contentOrNull
                                ?.toIntOrNull()
                        val state =
                            index
                                ?.let(
                                    pending::remove,
                                )
                        if (
                            state !=
                                null
                        ) {
                            output +=
                                toolEventOrFailure(
                                    state,
                                )
                        }
                    }

                    "message_delta" ->
                        stopReason =
                            (
                                event[
                                    "delta"
                                ] as? JsonObject
                            )
                                ?.get(
                                    "stop_reason",
                                )
                                ?.jsonPrimitive
                                ?.contentOrNull

                    "message_stop" -> {
                        completed = true
                        if (
                            output.none {
                                it is Event.Failed
                            }
                        ) {
                            output +=
                                Event.Completed(
                                    finishReason = stopReason,
                                )
                        }
                    }

                    "error" -> {
                        output +=
                            Event.Failed(
                                failureFromErrorObject(
                                    httpStatus = null,
                                    error =
                                        event[
                                            "error"
                                        ] as? JsonObject,
                                ),
                            )
                    }
                }
            }

        if (
            !completed &&
                output.none {
                    it is Event.Failed
                }
        ) {
            output +=
                Event.Failed(
                    Failure(
                        provider = Provider.ANTHROPIC_MESSAGES,
                        meaning = FailureMeaning.STREAM_INTERRUPTED,
                        retry = RetryDisposition.UNKNOWN,
                        httpStatus = null,
                        providerType = null,
                        providerCode = null,
                        message = "Anthropic stream ended before message_stop",
                    ),
                )
        }

        return output
    }

    fun decodeResponseBody(
        text: String,
    ): List<Event> {
        val root =
            runCatching {
                VexContractJson.Strict
                    .parseToJsonElement(
                        text,
                    )
                    .jsonObject
            }
                .getOrElse {
                    return listOf(
                        Event.Failed(
                            invalidResponse(
                                "Anthropic response body is not valid JSON",
                            ),
                        ),
                    )
                }

        if (
            root[
                "type"
            ]
                ?.jsonPrimitive
                ?.contentOrNull ==
                "error"
        ) {
            return listOf(
                Event.Failed(
                    failureFromErrorObject(
                        httpStatus = null,
                        error =
                            root[
                                "error"
                            ] as? JsonObject,
                    ),
                ),
            )
        }

        val output = mutableListOf<Event>()
        (
            root[
                "content"
            ] as? JsonArray
        )
            ?.forEach { element ->
                val block =
                    element as? JsonObject
                        ?: return@forEach
                when (
                    block[
                        "type"
                    ]
                        ?.jsonPrimitive
                        ?.contentOrNull
                ) {
                    "text" ->
                        block[
                            "text"
                        ]
                            ?.jsonPrimitive
                            ?.contentOrNull
                            ?.takeIf {
                                it.isNotEmpty()
                            }
                            ?.let {
                                output +=
                                    Event.TextDelta(
                                        it,
                                    )
                            }

                    "tool_use" -> {
                        val id =
                            block[
                                "id"
                            ]
                                ?.jsonPrimitive
                                ?.contentOrNull
                        val name =
                            block[
                                "name"
                            ]
                                ?.jsonPrimitive
                                ?.contentOrNull
                        val input =
                            block[
                                "input"
                            ] as? JsonObject
                        if (
                            !id.isNullOrBlank() &&
                                !name.isNullOrBlank() &&
                                input !=
                                null
                        ) {
                            output +=
                                Event.ToolCallReady(
                                    ToolCall(
                                        providerCallRef = id,
                                        name = name,
                                        arguments = input,
                                    ),
                                )
                        } else {
                            output +=
                                Event.Failed(
                                    invalidResponse(
                                        "Anthropic tool_use block is incomplete",
                                    ),
                                )
                        }
                    }
                }
            }

        if (
            output.none {
                it is Event.Failed
            }
        ) {
            output +=
                Event.Completed(
                    finishReason =
                        root[
                            "stop_reason"
                        ]
                            ?.jsonPrimitive
                            ?.contentOrNull,
                )
        }

        return output
    }

    fun classifyFailure(
        httpStatus: Int,
        body: String,
    ): Failure {
        val root =
            runCatching {
                VexContractJson.Strict
                    .parseToJsonElement(
                        body,
                    )
                    .jsonObject
            }
                .getOrNull()
        return failureFromErrorObject(
            httpStatus = httpStatus,
            error =
                root
                    ?.get(
                        "error",
                    ) as? JsonObject,
        )
    }

    private fun toolEventOrFailure(
        state: PendingToolCall,
    ): Event =
        runCatching {
            VexContractJson.Strict
                .parseToJsonElement(
                    state.arguments.toString().ifBlank {
                        "{}"
                    },
                )
                .jsonObject
        }
            .fold(
                onSuccess = {
                    Event.ToolCallReady(
                        ToolCall(
                            providerCallRef = state.providerCallRef,
                            name = state.name,
                            arguments = it,
                        ),
                    )
                },
                onFailure = {
                    Event.Failed(
                        invalidResponse(
                            "Anthropic streamed tool input is not one complete JSON object",
                        ),
                    )
                },
            )

    private fun failureFromErrorObject(
        httpStatus: Int?,
        error: JsonObject?,
    ): Failure {
        val type =
            error
                ?.get(
                    "type",
                )
                ?.jsonPrimitive
                ?.contentOrNull
        val message =
            error
                ?.get(
                    "message",
                )
                ?.jsonPrimitive
                ?.contentOrNull

        val meaning =
            when {
                httpStatus == 401 ||
                    type == "authentication_error" ->
                    FailureMeaning.CREDENTIAL_REJECTED

                httpStatus == 402 ||
                    type == "billing_error" ->
                    FailureMeaning.ACCOUNT_OR_BILLING_HELD

                httpStatus == 403 ||
                    type == "permission_error" ->
                    FailureMeaning.PERMISSION_HELD

                httpStatus == 429 ||
                    type == "rate_limit_error" ->
                    FailureMeaning.RATE_LIMITED

                httpStatus == 529 ||
                    type == "overloaded_error" ->
                    FailureMeaning.PROVIDER_OVERLOADED

                httpStatus != null &&
                    httpStatus >= 500 ||
                    type == "api_error" ->
                    FailureMeaning.PROVIDER_UNAVAILABLE

                httpStatus == 400 ||
                    type == "invalid_request_error" ||
                    type == "request_too_large" ->
                    FailureMeaning.REQUEST_INVALID

                else ->
                    FailureMeaning.UNKNOWN_PROVIDER_FAILURE
            }
        val retry =
            when (
                meaning
            ) {
                FailureMeaning.RATE_LIMITED,
                FailureMeaning.PROVIDER_OVERLOADED,
                FailureMeaning.PROVIDER_UNAVAILABLE,
                ->
                    RetryDisposition.RETRYABLE

                FailureMeaning.CREDENTIAL_REJECTED,
                FailureMeaning.ACCOUNT_OR_BILLING_HELD,
                FailureMeaning.PERMISSION_HELD,
                FailureMeaning.REQUEST_INVALID,
                FailureMeaning.CONTEXT_LIMIT_EXCEEDED,
                FailureMeaning.MODEL_NOT_ACCESSIBLE,
                ->
                    RetryDisposition.DO_NOT_RETRY_UNCHANGED

                else ->
                    RetryDisposition.UNKNOWN
            }

        return Failure(
            provider = Provider.ANTHROPIC_MESSAGES,
            meaning = meaning,
            retry = retry,
            httpStatus = httpStatus,
            providerType = type,
            providerCode = null,
            message = message,
        )
    }

    private fun invalidResponse(
        message: String,
    ): Failure =
        Failure(
            provider = Provider.ANTHROPIC_MESSAGES,
            meaning = FailureMeaning.RESPONSE_INVALID,
            retry = RetryDisposition.UNKNOWN,
            httpStatus = null,
            providerType = null,
            providerCode = null,
            message = message,
        )
}

// [VXG RealForever]
