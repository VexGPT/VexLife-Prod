package vexlife.companion

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import vexlife.capability.VexLifeCapabilityAtlas
import vexlife.capability.VexLifeCapabilityRequest

/**
 * Harmony-specific outer message/channel adapter for provider-native VexLife tools.
 *
 * Tool meaning and JSON schema are compiled once by [ProviderCapabilityTools]. This owner only
 * translates the canonical VexLife capability exchange into the message/channel form gpt-oss's
 * Harmony template expects.
 */
@OptIn(ExperimentalSerializationApi::class)
object HarmonyCapabilityTools {
    const val ProviderResultSchema = ProviderCapabilityTools.ProviderResultSchema

    /** One provider message as the Harmony template reads it: tool calls and tool results. */
    @Serializable
    data class Message(
        val role: String,
        val content: String,
        @EncodeDefault(EncodeDefault.Mode.NEVER) val tool_calls: List<ToolCall>? = null,
    )

    @Serializable
    data class ToolCall(
        val type: String = ProviderCapabilityTools.ProviderFunctionType,
        val function: Call,
    )

    /** `arguments` is JSON text, as OpenAI-compatible servers (mlx-lm included) require. */
    @Serializable
    data class Call(
        val name: String,
        val arguments: String,
    )

    fun functionName(handle: String, action: String): String =
        ProviderCapabilityTools.functionName(handle, action)

    fun tools(
        offered: List<VexLifeCapabilityAtlas.Definition>,
    ): List<ProviderCapabilityTools.Tool> =
        ProviderCapabilityTools.tools(offered)

    fun control(
        recipient: String,
        arguments: String,
        offered: List<VexLifeCapabilityAtlas.Definition>,
    ): String =
        ProviderCapabilityTools.control(
            providerName = recipient.removePrefix(FunctionsNamespace),
            arguments = arguments,
            offered = offered,
        )

    /**
     * Re-project the canonical bridge exchange into Harmony's assistant tool call + tool result.
     */
    fun messages(messages: List<CompanionProviderMessage>): List<Message> {
        val out = mutableListOf<Message>()
        var awaitingResult = false
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
                    out +=
                        Message(
                            role = "assistant",
                            content = "",
                            tool_calls =
                                listOf(
                                    ToolCall(
                                        function =
                                            Call(
                                                name =
                                                    functionName(
                                                        handle,
                                                        request.call.action,
                                                    ),
                                                arguments = request.call.arguments.toString(),
                                            ),
                                    ),
                                ),
                        )
                    awaitingResult = true
                }

                awaitingResult &&
                    message.role == "user" &&
                    message.content.startsWith(VexLifeCapabilityRequest.ResultHeader) -> {
                    out +=
                        Message(
                            role = "tool",
                            content = providerResult(message.content),
                        )
                    awaitingResult = false
                }

                else -> {
                    out += Message(role = message.role, content = message.content)
                    awaitingResult = false
                }
            }
        }
        return out
    }

    internal fun providerResult(semanticResult: String): String =
        ProviderCapabilityTools.providerResult(semanticResult)

    const val FunctionsNamespace = "functions."
}

// [VXG RealForever]
