package vexlife.companion

import vexlife.capability.VexLifeCapabilityBridge
import vexlife.memory.VexLifeMemoryContract
import vexlife.model.heretic.HereticCompanionRepresentation

/**
 * Product attention budget for one exact serving Companion runtime.
 *
 * Product ceilings remain conservative Product policy. A qualified runtime may establish a lower
 * hard context-window bound; that lower reality wins. Unknown runtime capacity never means
 * unlimited: the existing Product hard ceiling remains the maximum.
 *
 * The output share scales down only when the runtime itself is smaller than the Product envelope.
 * Capability exchange room is then carved from the same effective input envelope, so tools,
 * attachments and Conversation context cannot each pretend they own the whole window.
 */
object VexLifeCompanionAttentionBudget {
    data class Effective(
        val hardTokenLimit: Int,
        val reservedOutputTokens: Int,
        /** Input room before any current-turn capability exchange allowance is reserved. */
        val grossInputTokenBudget: Int,
        /** Maximum capability exchange room for this runtime before a smaller human/profile cap. */
        val capabilityExchangeTokenCeiling: Int,
    )

    fun forRuntime(
        runtimeContextWindowTokens: Int?,
        productOutputTokenCeiling: Int =
            HereticCompanionRepresentation.ProductMaxOutputTokens,
    ): Effective {
        require(
            runtimeContextWindowTokens ==
                null ||
                runtimeContextWindowTokens >
                0,
        ) {
            "runtime context window must be absent or positive"
        }
        require(
            productOutputTokenCeiling >
                0,
        ) {
            "Product output-token ceiling must be positive"
        }

        val hardTokenLimit =
            minOf(
                VexLifeMemoryContract.HardTokenLimit,
                runtimeContextWindowTokens
                    ?: VexLifeMemoryContract.HardTokenLimit,
            )
        val reservedOutputTokens =
            minOf(
                productOutputTokenCeiling,
                maxOf(
                    1,
                    hardTokenLimit /
                        4,
                ),
            )
        val grossInputTokenBudget =
            minOf(
                VexLifeMemoryContract.ContextInputTokenBudget,
                hardTokenLimit -
                    reservedOutputTokens,
            )
        require(
            grossInputTokenBudget >
                0,
        ) {
            "runtime context window leaves no Product input room"
        }

        val capabilityExchangeTokenCeiling =
            minOf(
                VexLifeCapabilityBridge.ExchangeAllowanceTokens,
                maxOf(
                    1,
                    grossInputTokenBudget /
                        4,
                ),
            )
        require(
            capabilityExchangeTokenCeiling <
                grossInputTokenBudget,
        ) {
            "runtime context window leaves no ordinary prompt room beside capability exchange"
        }

        return Effective(
            hardTokenLimit = hardTokenLimit,
            reservedOutputTokens = reservedOutputTokens,
            grossInputTokenBudget = grossInputTokenBudget,
            capabilityExchangeTokenCeiling = capabilityExchangeTokenCeiling,
        )
    }

}

// [VXG RealForever]
