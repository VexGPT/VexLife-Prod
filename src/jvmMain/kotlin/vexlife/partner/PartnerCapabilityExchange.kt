package vexlife.partner

import kotlinx.serialization.json.JsonObject
import vexlife.capability.VexLifeCapabilityAtlas
import vexlife.companion.ProviderCapabilityTools
import vexlife.contract.VexContractJson

/**
 * Keeps provider-native call identity beside one canonical Vex capability request.
 *
 * This object performs no capability, network, credential, Conversation, or Memory effect.
 *
 * The provider call reference is carriage currentness only:
 *
 * provider call X
 * -> canonical Vex request
 * -> rightful Bridge later performs/adjudicates that request
 * -> canonical Vex result
 * -> provider-native result still references X
 *
 * PROVIDER CALL ID != VEX CAPABILITY IDENTITY
 * PROVIDER CALL ID != EFFECT AUTHORITY
 */
object PartnerCapabilityExchange {
    data class Pending(
        val provider: ExternalProviderWire.Provider,
        val providerCall: ExternalProviderWire.ToolCall,
        val canonicalControl: String,
    ) {
        val providerCallRef: String
            get() = providerCall.providerCallRef
    }

    fun normalize(
        provider: ExternalProviderWire.Provider,
        call: ExternalProviderWire.ToolCall,
        offered: List<VexLifeCapabilityAtlas.Definition>,
    ): Pending {
        require(
            offered.isNotEmpty(),
        ) {
            "Partner capability normalization requires the exact capabilities offered this stage"
        }

        val canonicalControl =
            ProviderCapabilityTools.control(
                providerName = call.name,
                arguments =
                    VexContractJson.canonicalEncodeElement(
                        call.arguments,
                    ),
                offered = offered,
            )

        return Pending(
            provider = provider,
            providerCall = call,
            canonicalControl = canonicalControl,
        )
    }

    fun encodeResult(
        pending: Pending,
        semanticResult: String,
    ): JsonObject =
        when (
            pending.provider
        ) {
            ExternalProviderWire.Provider.OPENAI_RESPONSES ->
                OpenAiResponsesWire.encodeToolResult(
                    call = pending.providerCall,
                    semanticResult = semanticResult,
                )

            ExternalProviderWire.Provider.ANTHROPIC_MESSAGES ->
                AnthropicMessagesWire.encodeToolResult(
                    call = pending.providerCall,
                    semanticResult = semanticResult,
                )

            ExternalProviderWire.Provider.KIMI_CHAT_COMPLETIONS ->
                KimiChatWire.encodeToolResult(
                    call = pending.providerCall,
                    semanticResult = semanticResult,
                )
        }
}

// [VXG RealForever]
