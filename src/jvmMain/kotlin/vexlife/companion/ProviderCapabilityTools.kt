package vexlife.companion

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import vexlife.capability.VexLifeCapabilityAtlas
import vexlife.capability.VexLifeCapabilityRequest
import vexlife.contract.VexContractJson

/**
 * Provider-independent compilation of VexLife Capability Atlas actions into native tool schemas.
 *
 * Capability meaning stays with [VexLifeCapabilityAtlas]. This object owns only the common
 * OpenAI-compatible function schema and the reversible representation between one provider-native
 * tool call/result and the canonical VexLife capability request/result consumed by the bridge.
 *
 * Provider dialects may still differ at the outer message/channel layer (for example Harmony), but
 * they consume this same semantic function name, argument schema and result body.
 */
@OptIn(ExperimentalSerializationApi::class)
object ProviderCapabilityTools {
    const val ProviderFunctionType = "function"
    const val ProviderObjectType = "object"
    const val ProviderResultSchema = "vexlife-prod.provider-capability-result/v1"

    @Serializable
    data class Tool(
        val type: String = ProviderFunctionType,
        val function: Function,
    )

    @Serializable
    data class Function(
        val name: String,
        val description: String,
        val parameters: Parameters,
    )

    @Serializable
    data class Parameters(
        val type: String = ProviderObjectType,
        val properties: Map<String, Property>,
        val required: List<String>,
    )

    /**
     * Small JSON-Schema subset currently required by the Capability Atlas.
     *
     * Arrays stay arrays rather than collapsing to strings. Nested object detail can be added by
     * the semantic Atlas without changing provider adapters.
     */
    @Serializable
    data class Property(
        val type: String,
        val description: String,
        @EncodeDefault(EncodeDefault.Mode.NEVER) val items: Property? = null,
    )

    @Serializable
    data class FunctionCall(
        val name: String,
        /** JSON text, as mlx-lm/OpenAI-compatible requests require. */
        val arguments: String,
    )

    @Serializable
    data class ToolCall(
        val id: String,
        val type: String = ProviderFunctionType,
        val function: FunctionCall,
    )

    /** Standard OpenAI-chat tool-call/result message shape used by Qwen templates. */
    @Serializable
    data class OpenAiMessage(
        val role: String,
        @EncodeDefault(EncodeDefault.Mode.NEVER) val content: String? = null,
        @EncodeDefault(EncodeDefault.Mode.NEVER) val tool_calls: List<ToolCall>? = null,
        @EncodeDefault(EncodeDefault.Mode.NEVER) val tool_call_id: String? = null,
        @EncodeDefault(EncodeDefault.Mode.NEVER) val name: String? = null,
    )

    private data class PendingCall(
        val id: String,
        val name: String,
    )

    fun functionName(handle: String, action: String): String = "${handle}_$action"

    /** One native function for every requestable action in canonical offered order. */
    fun tools(offered: List<VexLifeCapabilityAtlas.Definition>): List<Tool> =
        offered.flatMap { definition ->
            definition.actions.map { action ->
                Tool(
                    function =
                        Function(
                            name = functionName(definition.handle, action.action),
                            description = oneLine("${action.purpose} Returns: ${action.returns}"),
                            parameters =
                                Parameters(
                                    properties =
                                        action.arguments.associate { argument ->
                                            argument.name to property(argument.type, argument.meaning)
                                        },
                                    required =
                                        action.arguments
                                            .filter { it.required }
                                            .map { it.name },
                                ),
                        ),
                )
            }
        }

    /**
     * Convert one native provider call into the canonical request text already understood by
     * [vexlife.capability.VexLifeCapabilityBridge].
     */
    fun control(
        providerName: String,
        arguments: String,
        offered: List<VexLifeCapabilityAtlas.Definition>,
    ): String {
        val name = providerName.removePrefix("functions.")
        val known =
            offered.firstNotNullOfOrNull { definition ->
                definition.actions
                    .firstOrNull { functionName(definition.handle, it.action) == name }
                    ?.let { definition.handle to it.action }
            }
        val (handle, action) =
            known
                ?: name
                    .split('_', '.', limit = 2)
                    .let { it.first() to it.getOrElse(1) { "" } }
        val parsedArguments =
            runCatching {
                VexContractJson.Strict.parseToJsonElement(arguments.ifBlank { "{}" })
            }
                .getOrNull()
        val request =
            JsonObject(
                mapOf(
                    "capability" to JsonPrimitive(handle),
                    "action" to JsonPrimitive(action),
                    // Keep unreadable arguments visible so the bridge can return the exact form.
                    "arguments" to (parsedArguments ?: JsonPrimitive(arguments)),
                ),
            )
        return VexLifeCapabilityRequest.RequestHeader + "\n" + request
    }

    /**
     * Parse the OpenAI-compatible tool_calls shape emitted by mlx-lm into one canonical Vex request.
     *
     * VexLife currently performs one bridge request per provider stage. If a model emits several
     * parallel calls, the first remains actionable and the rest are deliberately not invented into
     * sequential effects.
     */
    fun controlFromToolCalls(
        toolCalls: JsonElement?,
        offered: List<VexLifeCapabilityAtlas.Definition>,
    ): String? {
        val first =
            (toolCalls as? JsonArray)
                ?.firstOrNull()
                ?.let { it as? JsonObject }
                ?: return null
        val function =
            first["function"] as? JsonObject
                ?: return null
        val name =
            function["name"]
                ?.jsonPrimitive
                ?.contentOrNull
                ?.takeIf(String::isNotBlank)
                ?: return null
        val arguments =
            function["arguments"]
                ?.jsonPrimitive
                ?.contentOrNull
                ?: "{}"
        return control(
            providerName = name,
            arguments = arguments,
            offered = offered,
        )
    }

    /**
     * Re-project canonical bridge exchanges into native OpenAI tool-call/tool-result messages.
     */
    fun messages(messages: List<CompanionProviderMessage>): List<OpenAiMessage> {
        val out = mutableListOf<OpenAiMessage>()
        var pending: PendingCall? = null

        for (message in messages) {
            val request =
                if (message.role == "assistant") {
                    VexLifeCapabilityRequest.parseOrNull(message.content)
                        as? VexLifeCapabilityRequest.Parsed.Request
                } else {
                    null
                }

            when {
                request != null -> {
                    val handle =
                        VexLifeCapabilityAtlas.resolve(request.call.capability)
                            ?.handle
                            ?: request.call.capability
                    val name = functionName(handle, request.call.action)
                    val id = "call_" + request.controlSha256.take(20)
                    out +=
                        OpenAiMessage(
                            role = "assistant",
                            tool_calls =
                                listOf(
                                    ToolCall(
                                        id = id,
                                        function =
                                            FunctionCall(
                                                name = name,
                                                arguments = request.call.arguments.toString(),
                                            ),
                                    ),
                                ),
                        )
                    pending = PendingCall(id = id, name = name)
                }

                pending != null &&
                    message.role == "user" &&
                    message.content.startsWith(VexLifeCapabilityRequest.ResultHeader) -> {
                    val call = requireNotNull(pending)
                    out +=
                        OpenAiMessage(
                            role = "tool",
                            content = providerResult(message.content),
                            tool_call_id = call.id,
                            name = call.name,
                        )
                    pending = null
                }

                else -> {
                    out += OpenAiMessage(role = message.role, content = message.content)
                    pending = null
                }
            }
        }

        return out
    }

    /**
     * Separate returned source from host guidance before handing it back through a native tool role.
     */
    fun providerResult(semanticResult: String): String {
        require(
            semanticResult.startsWith(VexLifeCapabilityRequest.ResultHeader + "\n"),
        ) {
            "native capability result requires one canonical VexLife result"
        }

        val afterHeader =
            semanticResult.substringAfter(VexLifeCapabilityRequest.ResultHeader + "\n")
        val noticeEnd = afterHeader.indexOf('\n')
        require(noticeEnd >= 0) {
            "native capability result lacks host notice"
        }
        val afterNotice = afterHeader.substring(noticeEnd + 1)
        val metaEnd = afterNotice.indexOf('\n')
        require(metaEnd >= 0) {
            "native capability result lacks metadata"
        }
        val metadata =
            VexContractJson.Strict
                .parseToJsonElement(afterNotice.substring(0, metaEnd)) as? JsonObject
                ?: error("native capability result metadata is not one JSON object")

        val afterMetadata = afterNotice.substring(metaEnd + 1)
        val contentMarkerEnd = afterMetadata.indexOf('\n')
        require(contentMarkerEnd >= 0) {
            "native capability result lacks content boundary"
        }
        val contentMarker = afterMetadata.substring(0, contentMarkerEnd)
        val contentTrust =
            when {
                contentMarker.startsWith("<<<CONTENT written by others") ->
                    "UNTRUSTED_EXTERNAL"

                contentMarker == "<<<CONTENT" ->
                    "HOST_RETURN"

                else ->
                    error("native capability result has an unknown content boundary")
            }

        val afterContentMarker = afterMetadata.substring(contentMarkerEnd + 1)
        val closing = "\nCONTENT>>>\n"
        val closingIndex = afterContentMarker.lastIndexOf(closing)
        require(closingIndex >= 0) {
            "native capability result lacks closing content boundary"
        }

        val content = afterContentMarker.substring(0, closingIndex)
        val hostGuidance = afterContentMarker.substring(closingIndex + closing.length)

        return JsonObject(
            mapOf(
                "schemaVersion" to JsonPrimitive(ProviderResultSchema),
                "metadata" to metadata,
                "contentTrust" to JsonPrimitive(contentTrust),
                "content" to JsonPrimitive(content),
                "hostGuidance" to JsonPrimitive(hostGuidance),
            ),
        )
            .toString()
    }

    private fun property(type: String, meaning: String): Property =
        when {
            type.endsWith("[]") ->
                Property(
                    type = "array",
                    description = oneLine(meaning),
                    items =
                        Property(
                            type = jsonType(type.removeSuffix("[]")),
                            description = "One array item.",
                        ),
                )

            else ->
                Property(
                    type = jsonType(type),
                    description = oneLine(meaning),
                )
        }

    private fun jsonType(type: String): String =
        when (type) {
            "string", "integer", "number", "boolean", "object", "array" -> type
            else -> "string"
        }

    private fun oneLine(text: String): String = text.replace(Regex("\\s+"), " ").trim()
}

// [VXG RealForever]
