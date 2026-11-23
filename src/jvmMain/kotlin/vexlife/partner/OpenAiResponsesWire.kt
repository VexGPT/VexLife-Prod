package vexlife.partner

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import vexlife.contract.VexContractJson
import vexlife.partner.ExternalProviderWire.Event
import vexlife.partner.ExternalProviderWire.Failure
import vexlife.partner.ExternalProviderWire.FailureMeaning
import vexlife.partner.ExternalProviderWire.PreparedRequest
import vexlife.partner.ExternalProviderWire.Provider
import vexlife.partner.ExternalProviderWire.RetryDisposition
import vexlife.partner.ExternalProviderWire.ToolCall

/** Pure OpenAI Responses representation. No HTTP or credential handling lives here. */
object OpenAiResponsesWire {
    private data class PendingToolCall(
        val providerCallRef: String,
        var name: String,
        val arguments: StringBuilder,
    )

    fun encodeRequest(
        request: PreparedRequest,
    ): String {
        val (instructions, turnMessages) =
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
                    instructions
                        ?.let {
                            put(
                                "instructions",
                                JsonPrimitive(it),
                            )
                        }
                    put(
                        "input",
                        JsonArray(
                            turnMessages.map(
                                ExternalProviderWire::messageJson,
                            ),
                        ),
                    )
                    put(
                        "max_output_tokens",
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
                                    ExternalProviderWire::openAiResponsesTool,
                                ),
                            ),
                        )
                        put(
                            "tool_choice",
                            JsonPrimitive("auto"),
                        )
                        put(
                            "parallel_tool_calls",
                            JsonPrimitive(false),
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
        val pending = linkedMapOf<String, PendingToolCall>()

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
                                        "OpenAI SSE data is not valid JSON",
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
                    "response.output_text.delta" ->
                        event[
                            "delta"
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

                    "response.output_item.added" -> {
                        val item =
                            event[
                                "item"
                            ] as? JsonObject
                        if (
                            item
                                ?.get(
                                    "type",
                                )
                                ?.jsonPrimitive
                                ?.contentOrNull ==
                                "function_call"
                        ) {
                            val itemId =
                                item[
                                    "id"
                                ]
                                    ?.jsonPrimitive
                                    ?.contentOrNull
                            val callId =
                                item[
                                    "call_id"
                                ]
                                    ?.jsonPrimitive
                                    ?.contentOrNull
                            val name =
                                item[
                                    "name"
                                ]
                                    ?.jsonPrimitive
                                    ?.contentOrNull
                            if (
                                !itemId.isNullOrBlank() &&
                                    !callId.isNullOrBlank() &&
                                    !name.isNullOrBlank()
                            ) {
                                pending[
                                    itemId
                                ] =
                                    PendingToolCall(
                                        providerCallRef = callId,
                                        name = name,
                                        arguments =
                                            StringBuilder(
                                                item[
                                                    "arguments"
                                                ]
                                                    ?.jsonPrimitive
                                                    ?.contentOrNull
                                                    .orEmpty(),
                                            ),
                                    )
                            }
                        }
                    }

                    "response.function_call_arguments.delta" -> {
                        val itemId =
                            event[
                                "item_id"
                            ]
                                ?.jsonPrimitive
                                ?.contentOrNull
                        val delta =
                            event[
                                "delta"
                            ]
                                ?.jsonPrimitive
                                ?.contentOrNull
                        if (
                            itemId != null &&
                                delta != null
                        ) {
                            pending[
                                itemId
                            ]
                                ?.arguments
                                ?.append(
                                    delta,
                                )
                        }
                    }

                    "response.function_call_arguments.done" -> {
                        val itemId =
                            event[
                                "item_id"
                            ]
                                ?.jsonPrimitive
                                ?.contentOrNull
                        val state =
                            itemId
                                ?.let(
                                    pending::remove,
                                )
                        if (
                            state !=
                                null
                        ) {
                            val finalArguments =
                                event[
                                    "arguments"
                                ]
                                    ?.jsonPrimitive
                                    ?.contentOrNull
                                    ?: state.arguments.toString()
                            output +=
                                toolEventOrFailure(
                                    state.providerCallRef,
                                    state.name,
                                    finalArguments,
                                )
                        }
                    }

                    "response.completed" ->
                        output +=
                            Event.Completed(
                                finishReason = "completed",
                            )

                    "response.failed" -> {
                        val response =
                            event[
                                "response"
                            ] as? JsonObject
                        val error =
                            response
                                ?.get(
                                    "error",
                                ) as? JsonObject
                        output +=
                            Event.Failed(
                                failureFromErrorObject(
                                    httpStatus = null,
                                    error = error,
                                ),
                            )
                    }

                    "error" ->
                        output +=
                            Event.Failed(
                                failureFromErrorObject(
                                    httpStatus = null,
                                    error =
                                        event[
                                            "error"
                                        ] as? JsonObject
                                            ?: event,
                                ),
                            )
                }
            }

        if (
            pending.isNotEmpty() &&
                output.none {
                    it is Event.Failed
                }
        ) {
            output +=
                Event.Failed(
                    Failure(
                        provider = Provider.OPENAI_RESPONSES,
                        meaning = FailureMeaning.STREAM_INTERRUPTED,
                        retry = RetryDisposition.UNKNOWN,
                        httpStatus = null,
                        providerType = null,
                        providerCode = null,
                        message = "OpenAI stream ended with an incomplete function call",
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
                                "OpenAI response body is not valid JSON",
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
        val items =
            root[
                "output"
            ] as? JsonArray
                ?: JsonArray(
                    emptyList(),
                )

        items.forEach { element ->
            val item =
                element as? JsonObject
                    ?: return@forEach
            when (
                item[
                    "type"
                ]
                    ?.jsonPrimitive
                    ?.contentOrNull
            ) {
                "function_call" -> {
                    val callId =
                        item[
                            "call_id"
                        ]
                            ?.jsonPrimitive
                            ?.contentOrNull
                    val name =
                        item[
                            "name"
                        ]
                            ?.jsonPrimitive
                            ?.contentOrNull
                    val arguments =
                        item[
                            "arguments"
                        ]
                            ?.jsonPrimitive
                            ?.contentOrNull
                    if (
                        !callId.isNullOrBlank() &&
                            !name.isNullOrBlank() &&
                            arguments != null
                    ) {
                        output +=
                            toolEventOrFailure(
                                callId,
                                name,
                                arguments,
                            )
                    } else {
                        output +=
                            Event.Failed(
                                invalidResponse(
                                    "OpenAI function call is incomplete",
                                ),
                            )
                    }
                }

                "message" -> {
                    (
                        item[
                            "content"
                        ] as? JsonArray
                    )
                        ?.forEach { content ->
                            val part =
                                content as? JsonObject
                                    ?: return@forEach
                            if (
                                part[
                                    "type"
                                ]
                                    ?.jsonPrimitive
                                    ?.contentOrNull ==
                                    "output_text"
                            ) {
                                part[
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
                            }
                        }
                }
            }
        }

        output +=
            Event.Completed(
                finishReason =
                    root[
                        "status"
                    ]
                        ?.jsonPrimitive
                        ?.contentOrNull,
            )

        return output
    }

    fun classifyFailure(
        httpStatus: Int,
        body: String,
    ): Failure {
        val error =
            runCatching {
                VexContractJson.Strict
                    .parseToJsonElement(
                        body,
                    )
                    .jsonObject[
                        "error"
                    ] as? JsonObject
            }
                .getOrNull()

        return failureFromErrorObject(
            httpStatus = httpStatus,
            error = error,
        )
    }

    private fun toolEventOrFailure(
        callId: String,
        name: String,
        arguments: String,
    ): Event =
        runCatching {
            VexContractJson.Strict
                .parseToJsonElement(
                    arguments,
                )
                .jsonObject
        }
            .fold(
                onSuccess = {
                    Event.ToolCallReady(
                        ToolCall(
                            providerCallRef = callId,
                            name = name,
                            arguments = it,
                        ),
                    )
                },
                onFailure = {
                    Event.Failed(
                        invalidResponse(
                            "OpenAI function arguments are not one JSON object",
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

        val billingCodes =
            setOf(
                "credit_balance_exhausted",
                "project_spend_limit_exceeded",
                "organization_spend_limit_exceeded",
                "org_spend_limit_exceeded",
                "usage_limit_reached",
                "billing_hard_limit_reached",
                "insufficient_quota",
            )

        val meaning =
            when {
                httpStatus == 401 ||
                    code == "invalid_api_key" ->
                    FailureMeaning.CREDENTIAL_REJECTED

                code in
                    billingCodes ->
                    FailureMeaning.ACCOUNT_OR_BILLING_HELD

                httpStatus == 403 ->
                    FailureMeaning.PERMISSION_HELD

                code == "context_length_exceeded" ->
                    FailureMeaning.CONTEXT_LIMIT_EXCEEDED

                httpStatus == 429 ->
                    FailureMeaning.RATE_LIMITED

                httpStatus != null &&
                    httpStatus >= 500 ->
                    FailureMeaning.PROVIDER_UNAVAILABLE

                httpStatus == 400 ||
                    type == "invalid_request_error" ->
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
            provider = Provider.OPENAI_RESPONSES,
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
            provider = Provider.OPENAI_RESPONSES,
            meaning = FailureMeaning.RESPONSE_INVALID,
            retry = RetryDisposition.UNKNOWN,
            httpStatus = null,
            providerType = null,
            providerCode = null,
            message = message,
        )
}

// [VXG RealForever]
