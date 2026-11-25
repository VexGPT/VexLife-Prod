package vexlife.partner

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import vexlife.companion.CompanionProviderMessage
import vexlife.companion.ProviderCapabilityTools

/**
 * Pure provider wire-workbench contract for external Partner inference.
 *
 * This source owns no network transport, endpoint, credential, account, model selection,
 * Conversation, Memory, capability admission, or effect authority.
 *
 * Permanent:
 * WIRE CODEC != PROVIDER CONNECTION
 * WIRE CODEC != CREDENTIAL OWNER
 * WIRE TOOL CALL != VEX EFFECT AUTHORITY
 * SYNTHETIC CONFORMANCE != LIVE PROVIDER QUALIFICATION
 */
object ExternalProviderWire {
    enum class Provider {
        OPENAI_RESPONSES,
        ANTHROPIC_MESSAGES,
        KIMI_CHAT_COMPLETIONS,
    }

    enum class FailureMeaning {
        CREDENTIAL_REJECTED,
        ACCOUNT_OR_BILLING_HELD,
        PERMISSION_HELD,
        MODEL_NOT_ACCESSIBLE,
        RATE_LIMITED,
        PROVIDER_OVERLOADED,
        PROVIDER_UNAVAILABLE,
        REQUEST_INVALID,
        CONTEXT_LIMIT_EXCEEDED,
        STREAM_INTERRUPTED,
        RESPONSE_INVALID,
        UNKNOWN_PROVIDER_FAILURE,
    }

    enum class RetryDisposition {
        RETRYABLE,
        DO_NOT_RETRY_UNCHANGED,
        UNKNOWN,
    }

    data class Failure(
        val provider: Provider,
        val meaning: FailureMeaning,
        val retry: RetryDisposition,
        val httpStatus: Int?,
        val providerType: String?,
        val providerCode: String?,
        val message: String?,
    )

    data class PreparedRequest(
        val model: String,
        val messages: List<CompanionProviderMessage>,
        val maxOutputTokens: Int,
        val tools: List<ProviderCapabilityTools.Tool> = emptyList(),
    ) {
        init {
            require(model.isNotBlank()) {
                "external provider request requires one model identifier"
            }
            require(messages.isNotEmpty()) {
                "external provider request requires provider-ready messages"
            }
            require(maxOutputTokens > 0) {
                "external provider request output bound must be positive"
            }
        }
    }

    data class ToolCall(
        val providerCallRef: String,
        val name: String,
        val arguments: JsonObject,
    ) {
        init {
            require(providerCallRef.isNotBlank()) {
                "provider tool call requires one call identity"
            }
            require(name.isNotBlank()) {
                "provider tool call requires one function name"
            }
        }
    }

    sealed interface Event {
        data class TextDelta(
            val text: String,
        ) : Event

        data class ToolCallReady(
            val call: ToolCall,
        ) : Event

        data class Completed(
            val finishReason: String?,
        ) : Event

        data class Failed(
            val failure: Failure,
        ) : Event
    }

    data class SseEvent(
        val event: String?,
        val data: String,
    )

    /** Minimal SSE framing used by the provider workbench. */
    fun parseSse(
        text: String,
    ): List<SseEvent> {
        val events = mutableListOf<SseEvent>()
        var eventName: String? = null
        val data = mutableListOf<String>()

        fun flush() {
            if (
                eventName != null ||
                    data.isNotEmpty()
            ) {
                events +=
                    SseEvent(
                        event = eventName,
                        data = data.joinToString("\n"),
                    )
            }
            eventName = null
            data.clear()
        }

        text
            .lineSequence()
            .forEach { line ->
                when {
                    line.isEmpty() ->
                        flush()

                    line.startsWith(":") ->
                        Unit

                    line.startsWith("event:") ->
                        eventName =
                            line
                                .removePrefix("event:")
                                .trim()

                    line.startsWith("data:") ->
                        data +=
                            line
                                .removePrefix("data:")
                                .trimStart()

                    else ->
                        Unit
                }
            }
        flush()

        return events
    }

    internal fun splitLeadingSystem(
        messages: List<CompanionProviderMessage>,
    ): Pair<String?, List<CompanionProviderMessage>> {
        val leading =
            messages.takeWhile {
                it.role == "system"
            }
        val remaining =
            messages.drop(
                leading.size,
            )
        require(
            remaining.none {
                it.role == "system"
            },
        ) {
            "external provider adapters require system messages to precede turn messages"
        }

        return leading
            .takeIf {
                it.isNotEmpty()
            }
            ?.joinToString(
                separator = "\n\n",
            ) {
                it.content
            } to
            remaining
    }

    internal fun messageJson(
        message: CompanionProviderMessage,
    ): JsonObject =
        JsonObject(
            linkedMapOf(
                "role" to JsonPrimitive(message.role),
                "content" to JsonPrimitive(message.content),
            ),
        )

    internal fun parametersJson(
        parameters: ProviderCapabilityTools.Parameters,
        additionalProperties: Boolean = false,
    ): JsonObject =
        JsonObject(
            linkedMapOf<String, JsonElement>().apply {
                put(
                    "type",
                    JsonPrimitive(parameters.type),
                )
                put(
                    "properties",
                    JsonObject(
                        parameters.properties.mapValues {
                            propertyJson(
                                it.value,
                            )
                        },
                    ),
                )
                put(
                    "required",
                    JsonArray(
                        parameters.required.map(
                            ::JsonPrimitive,
                        ),
                    ),
                )
                put(
                    "additionalProperties",
                    JsonPrimitive(additionalProperties),
                )
            },
        )

    private fun propertyJson(
        property: ProviderCapabilityTools.Property,
    ): JsonObject =
        JsonObject(
            linkedMapOf<String, JsonElement>().apply {
                put(
                    "type",
                    JsonPrimitive(property.type),
                )
                put(
                    "description",
                    JsonPrimitive(property.description),
                )
                property.items
                    ?.let {
                        put(
                            "items",
                            propertyJson(
                                it,
                            ),
                        )
                    }
                if (
                    property.properties.isNotEmpty()
                ) {
                    put(
                        "properties",
                        JsonObject(
                            property.properties.mapValues {
                                propertyJson(
                                    it.value,
                                )
                            },
                        ),
                    )
                }
                if (
                    property.required.isNotEmpty()
                ) {
                    put(
                        "required",
                        JsonArray(
                            property.required.map(
                                ::JsonPrimitive,
                            ),
                        ),
                    )
                }
                if (
                    property.enumValues.isNotEmpty()
                ) {
                    put(
                        "enum",
                        JsonArray(
                            property.enumValues.map(
                                ::JsonPrimitive,
                            ),
                        ),
                    )
                }
            },
        )

    internal fun openAiResponsesTool(
        tool: ProviderCapabilityTools.Tool,
    ): JsonObject =
        JsonObject(
            linkedMapOf(
                "type" to JsonPrimitive("function"),
                "name" to JsonPrimitive(tool.function.name),
                "description" to JsonPrimitive(tool.function.description),
                "parameters" to
                    parametersJson(
                        tool.function.parameters,
                    ),
                "strict" to JsonPrimitive(true),
            ),
        )

    internal fun anthropicTool(
        tool: ProviderCapabilityTools.Tool,
    ): JsonObject =
        JsonObject(
            linkedMapOf(
                "name" to JsonPrimitive(tool.function.name),
                "description" to JsonPrimitive(tool.function.description),
                "input_schema" to
                    parametersJson(
                        tool.function.parameters,
                    ),
            ),
        )

    internal fun openAiCompatibleTool(
        tool: ProviderCapabilityTools.Tool,
    ): JsonObject =
        JsonObject(
            linkedMapOf(
                "type" to JsonPrimitive("function"),
                "function" to
                    JsonObject(
                        linkedMapOf(
                            "name" to JsonPrimitive(tool.function.name),
                            "description" to JsonPrimitive(tool.function.description),
                            "parameters" to
                                parametersJson(
                                    tool.function.parameters,
                                ),
                        ),
                    ),
            ),
        )
}

// [VXG RealForever]
