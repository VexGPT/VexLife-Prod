package vexlife.companion

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import vexlife.capability.VexLifeCapabilityAtlas
import vexlife.capability.VexLifeCapabilityRequest
import vexlife.contract.VexContractJson

/**
 * VexLife capability request/return, in the form the gpt-oss harmony template gives tools.
 *
 * Why it exists: gpt-oss was trained to reach through tools the harmony template declares in its
 * `functions` namespace and to call them as `to=functions.<name>` messages. Told about VexLife's
 * reach only as developer text ("reply with VEXLIFE_CAPABILITY_REQUEST/v1"), with no tool declared,
 * it reasons "we don't have remote fetch capabilities" and answers from nothing.
 *
 * Relationship:
 * ```
 * the capabilities available this turn (Capability Atlas, as the turn's frame reports them)
 *   -> one harmony function per action: <handle>_<action>, typed arguments  [request]
 *   -> gpt-oss calls one natively: to=functions.web_fetch {"url": ...}     [response]
 *   -> the same VEXLIFE_CAPABILITY_REQUEST/v1 control a text-reply model writes
 *   -> VexLifeCapabilityBridge (unchanged: admission, pacing, owners, receipts, source return)
 *   -> the exchange, in the next stage, as a harmony tool call + tool result
 *   -> Vex's final answer from what came back
 * ```
 *
 * Owns representation only. It admits nothing, performs nothing, and never offers gpt-oss's builtin
 * browser or python: those would reach past the bridge.
 */
@OptIn(ExperimentalSerializationApi::class)
object HarmonyCapabilityTools {
    @Serializable data class Tool(val type: String = "function", val function: Function)

    @Serializable
    data class Function(val name: String, val description: String, val parameters: Parameters)

    @Serializable
    data class Parameters(
        val type: String = "object",
        val properties: Map<String, Property>,
        val required: List<String>,
    )

    @Serializable data class Property(val type: String, val description: String)

    /** One provider message as the harmony template reads it: tool calls and tool results. */
    @Serializable
    data class Message(
        val role: String,
        val content: String,
        @EncodeDefault(EncodeDefault.Mode.NEVER) val tool_calls: List<ToolCall>? = null,
    )

    @Serializable data class ToolCall(val type: String = "function", val function: Call)

    /** `arguments` is JSON text, as OpenAI-compatible servers (mlx-lm included) require. */
    @Serializable data class Call(val name: String, val arguments: String)

    fun functionName(handle: String, action: String): String = "${handle}_$action"

    /** One function per requestable action of each offered capability, in the offered order. */
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
                                        action.arguments.associate {
                                            it.name to
                                                Property(jsonType(it.type), oneLine(it.meaning))
                                        },
                                    required =
                                        action.arguments.filter { it.required }.map { it.name },
                                ),
                        )
                )
            }
        }

    /**
     * The control text for one native call, exactly what the bridge reads from a text-reply model.
     * A name that is not one of the offered functions still becomes a request, so the bridge
     * answers it the way it answers any unknown capability or action instead of the call
     * disappearing.
     */
    fun control(
        recipient: String,
        arguments: String,
        offered: List<VexLifeCapabilityAtlas.Definition>,
    ): String {
        val name = recipient.removePrefix(FunctionsNamespace)
        val known = offered.firstNotNullOfOrNull { definition ->
            definition.actions
                .firstOrNull { functionName(definition.handle, it.action) == name }
                ?.let { definition.handle to it.action }
        }
        val (handle, action) =
            known ?: name.split('_', '.', limit = 2).let { it.first() to it.getOrElse(1) { "" } }
        val parsedArguments = runCatching {
            VexContractJson.Strict.parseToJsonElement(arguments.ifBlank { "{}" })
        }.getOrNull()
        val request =
            JsonObject(
                mapOf(
                    "capability" to JsonPrimitive(handle),
                    "action" to JsonPrimitive(action),
                    // Unreadable arguments stay visible as text; the bridge answers with the form.
                    "arguments" to (parsedArguments ?: JsonPrimitive(arguments)),
                )
            )
        return VexLifeCapabilityRequest.RequestHeader + "\n" + request
    }

    /**
     * The turn's provider messages for the harmony template: a stage's request (Vex's control, as
     * the bridge keeps it) becomes Vex's tool call, and the host's result that follows it becomes
     * the tool's message. Everything else is unchanged.
     */
    fun messages(messages: List<CompanionProviderMessage>): List<Message> {
        val out = mutableListOf<Message>()
        var awaitingResult = false
        for (message in messages) {
            val request =
                if (message.role == "assistant") {
                    VexLifeCapabilityRequest.parseOrNull(message.content)
                        as? VexLifeCapabilityRequest.Parsed.Request
                } else null
            when {
                request != null -> {
                    val handle =
                        VexLifeCapabilityAtlas.resolve(request.call.capability)?.handle
                            ?: request.call.capability
                    out +=
                        Message(
                            role = "assistant",
                            content = "",
                            tool_calls =
                                listOf(
                                    ToolCall(
                                        function =
                                            Call(
                                                name = functionName(handle, request.call.action),
                                                arguments = request.call.arguments.toString(),
                                            )
                                    )
                                ),
                        )
                    awaitingResult = true
                    continue
                }
                awaitingResult &&
                    message.role == "user" &&
                    message.content.startsWith(VexLifeCapabilityRequest.ResultHeader) ->
                    out += Message(role = "tool", content = message.content)
                else -> out += Message(role = message.role, content = message.content)
            }
            awaitingResult = false
        }
        return out
    }

    const val FunctionsNamespace = "functions."

    private fun jsonType(type: String): String =
        type.takeIf { it in setOf("string", "integer", "number", "boolean") } ?: "string"

    /** The template writes each description as one `//` comment line. */
    private fun oneLine(text: String): String = text.replace(Regex("\\s+"), " ").trim()
}

// [VXG RealForever]
