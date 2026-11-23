package vexlife.partner

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import vexlife.contract.VexContractJson
import vexlife.partner.ExternalProviderWire.Event
import vexlife.partner.ExternalProviderWire.Failure
import vexlife.partner.ExternalProviderWire.FailureMeaning
import vexlife.partner.ExternalProviderWire.PreparedRequest
import vexlife.partner.ExternalProviderWire.Provider
import vexlife.partner.ExternalProviderWire.RetryDisposition
import vexlife.partner.ExternalProviderWire.ToolCall

/** Pure Kimi Open Platform Chat Completions representation. No HTTP or credential handling lives here. */
object KimiChatWire {
    private data class PendingToolCall(
        var providerCallRef: String? = null,
        var name: String? = null,
        val arguments: StringBuilder = StringBuilder(),
    )

    fun encodeRequest(
        request: PreparedRequest,
    ): String {
        val body =
            JsonObject(
                linkedMapOf<String, JsonElement>().apply {
                    put(
                        "model",
                        JsonPrimitive(request.model),
                    )
                    put(
                        "messages",
                        JsonArray(
                            request.messages.map(
                                ExternalProviderWire::messageJson,
                            ),
                        ),
                    )
                    put(
                        "max_completion_tokens",
                        JsonPrimitive(request.maxOutputTokens),
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
                                    ExternalProviderWire::openAiCompatibleTool,
                                ),
                            ),
                        )
                        put(
                            "tool_choice",
                            JsonPrimitive("auto"),
                        )
                    }
                },
            )

        return VexContractJson.canonicalEncodeElement(
            body,
        )
    }

    fun decodeStream(
        text: String,
    ): List<Event> {
        val output = mutableListOf<Event>()
        val pending = linkedMapOf<Int, PendingToolCall>()
        var terminalObserved = false

        ExternalProviderWire
            .parseSse(
                text,
            )
            .forEach { sse ->
                if (
                    sse.data ==
                        "[DONE]"
                ) {
                    return@forEach
                }

                val chunk =
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
                                        "Kimi SSE data is not valid JSON",
                                    ),
                                )
                            return@forEach
                        }

                (
                    chunk[
                        "choices"
                    ] as? JsonArray
                )
                    ?.forEach { choiceElement ->
                        val choice =
                            choiceElement as? JsonObject
                                ?: return@forEach
                        val delta =
                            choice[
                                "delta"
                            ] as? JsonObject

                        delta
                            ?.get(
                                "content",
                            )
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

                        (
                            delta
                                ?.get(
                                    "tool_calls",
                                ) as? JsonArray
                        )
                            ?.forEach { toolElement ->
                                val tool =
                                    toolElement as? JsonObject
                                        ?: return@forEach
                                val index =
                                    tool[
                                        "index"
                                    ]
                                        ?.jsonPrimitive
                                        ?.contentOrNull
                                        ?.toIntOrNull()
                                    ?: 0
                                val state =
                                    pending.getOrPut(
                                        index,
                                    ) {
                                        PendingToolCall()
                                    }
                                tool[
                                    "id"
                                ]
                                    ?.jsonPrimitive
                                    ?.contentOrNull
                                    ?.takeIf {
                                        it.isNotBlank()
                                    }
                                    ?.let {
                                        state.providerCallRef =
                                            state.providerCallRef
                                                ?: it
                                    }
                                val function =
                                    tool[
                                        "function"
                                    ] as? JsonObject
                                function
                                    ?.get(
                                        "name",
                                    )
                                    ?.jsonPrimitive
                                    ?.contentOrNull
                                    ?.takeIf {
                                        it.isNotBlank()
                                    }
                                    ?.let {
                                        state.name =
                                            state.name
                                                ?: it
                                    }
                                function
                                    ?.get(
                                        "arguments",
                                    )
                                    ?.jsonPrimitive
                                    ?.contentOrNull
                                    ?.let(
                                        state.arguments::append,
                                    )
                            }

                        val finishReason =
                            choice[
                                "finish_reason"
                            ]
                                ?.jsonPrimitive
                                ?.contentOrNull
                        if (
                            finishReason !=
                                null
                        ) {
                            terminalObserved = true
                            if (
                                finishReason ==
                                    "tool_calls"
                            ) {
                                pending
                                    .toSortedMap()
                                    .values
                                    .forEach { state ->
                                        output +=
                                            toolEventOrFailure(
                                                state,
                                            )
                                    }
                                pending.clear()
                            }
                            if (
                                output.none {
                                    it is Event.Failed
                                }
                            ) {
                                output +=
                                    Event.Completed(
                                        finishReason = finishReason,
                                    )
                            }
                        }
                    }
            }

        if (
            !terminalObserved &&
                output.none {
                    it is Event.Failed
                }
        ) {
            output +=
                Event.Failed(
                    Failure(
                        provider = Provider.KIMI_CHAT_COMPLETIONS,
                        meaning = FailureMeaning.STREAM_INTERRUPTED,
                        retry = RetryDisposition.UNKNOWN,
                        httpStatus = null,
                        providerType = null,
                        providerCode = null,
                        message = "Kimi stream ended before a terminal finish_reason",
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
                                "Kimi response body is not valid JSON",
                            ),
                        ),
                    )
                }

        root[
            "error"
        ]
            ?.let {
                return listOf(
                    Event.Failed(
                        failureFromErrorObject(
                            httpStatus = null,
                            error = it as? JsonObject,
                        ),
                    ),
                )
            }

        val output = mutableListOf<Event>()
        (
            root[
                "choices"
            ] as? JsonArray
        )
            ?.forEach { choiceElement ->
                val choice =
                    choiceElement as? JsonObject
                        ?: return@forEach
                val message =
                    choice[
                        "message"
                    ] as? JsonObject

                message
                    ?.get(
                        "content",
                    )
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

                (
                    message
                        ?.get(
                            "tool_calls",
                        ) as? JsonArray
                )
                    ?.forEach { toolElement ->
                        val tool =
                            toolElement as? JsonObject
                                ?: return@forEach
                        val id =
                            tool[
                                "id"
                            ]
                                ?.jsonPrimitive
                                ?.contentOrNull
                        val function =
                            tool[
                                "function"
                            ] as? JsonObject
                        val name =
                            function
                                ?.get(
                                    "name",
                                )
                                ?.jsonPrimitive
                                ?.contentOrNull
                        val arguments =
                            function
                                ?.get(
                                    "arguments",
                                )
                                ?.jsonPrimitive
                                ?.contentOrNull

                        if (
                            !id.isNullOrBlank() &&
                                !name.isNullOrBlank() &&
                                arguments !=
                                null
                        ) {
                            output +=
                                toolEventOrFailure(
                                    PendingToolCall(
                                        providerCallRef = id,
                                        name = name,
                                        arguments =
                                            StringBuilder(
                                                arguments,
                                            ),
                                    ),
                                )
                        } else {
                            output +=
                                Event.Failed(
                                    invalidResponse(
                                        "Kimi tool call is incomplete",
                                    ),
                                )
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
                                choice[
                                    "finish_reason"
                                ]
                                    ?.jsonPrimitive
                                    ?.contentOrNull,
                        )
                }
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
    ): Event {
        val id =
            state.providerCallRef
        val name =
            state.name

        if (
            id.isNullOrBlank() ||
                name.isNullOrBlank()
        ) {
            return Event.Failed(
                invalidResponse(
                    "Kimi streamed tool call lacks stable id or function name",
                ),
            )
        }

        return runCatching {
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
                            providerCallRef = id,
                            name = name,
                            arguments = it,
                        ),
                    )
                },
                onFailure = {
                    Event.Failed(
                        invalidResponse(
                            "Kimi streamed tool arguments are not one complete JSON object",
                        ),
                    )
                },
            )
    }

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
        val code =
            error
                ?.get(
                    "code",
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
                    type == "invalid_authentication_error" ||
                    type == "authentication_error" ->
                    FailureMeaning.CREDENTIAL_REJECTED

                httpStatus == 403 ||
                    type == "permission_denied_error" ->
                    FailureMeaning.PERMISSION_HELD

                (
                    httpStatus == 404 ||
                        type == "resource_not_found_error"
                ) &&
                    message
                        ?.contains(
                            "model",
                            ignoreCase = true,
                        ) ==
                    true ->
                    FailureMeaning.MODEL_NOT_ACCESSIBLE

                type == "engine_overloaded_error" ->
                    FailureMeaning.PROVIDER_OVERLOADED

                type == "exceeded_current_quota_error" ->
                    FailureMeaning.ACCOUNT_OR_BILLING_HELD

                type == "rate_limit_reached_error" ->
                    FailureMeaning.RATE_LIMITED

                httpStatus == 429 ->
                    FailureMeaning.UNKNOWN_PROVIDER_FAILURE

                httpStatus != null &&
                    httpStatus >= 500 ||
                    type == "server_error" ||
                    type == "server_unavailable" ->
                    FailureMeaning.PROVIDER_UNAVAILABLE

                httpStatus == 400 ||
                    type == "invalid_request_error" ||
                    type == "content_filter" ->
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
                FailureMeaning.MODEL_NOT_ACCESSIBLE,
                FailureMeaning.REQUEST_INVALID,
                FailureMeaning.CONTEXT_LIMIT_EXCEEDED,
                ->
                    RetryDisposition.DO_NOT_RETRY_UNCHANGED

                else ->
                    RetryDisposition.UNKNOWN
            }

        return Failure(
            provider = Provider.KIMI_CHAT_COMPLETIONS,
            meaning = meaning,
            retry = retry,
            httpStatus = httpStatus,
            providerType = type,
            providerCode = code,
            message = message,
        )
    }

    private fun invalidResponse(
        message: String,
    ): Failure =
        Failure(
            provider = Provider.KIMI_CHAT_COMPLETIONS,
            meaning = FailureMeaning.RESPONSE_INVALID,
            retry = RetryDisposition.UNKNOWN,
            httpStatus = null,
            providerType = null,
            providerCode = null,
            message = message,
        )
}

// [VXG RealForever]
