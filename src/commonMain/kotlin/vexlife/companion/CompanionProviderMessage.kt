package vexlife.companion

import kotlinx.serialization.Serializable

/**
 * Provider-ready local Companion message.
 *
 * Prompt/context owners decide which source truth is materialized into these messages. The
 * Connection only transports them.
 */
@Serializable
data class CompanionProviderMessage(
    val role: String,
    val content: String,
) {
    init {
        require(
            role in
                setOf(
                    "system",
                    "user",
                    "assistant",
                ),
        ) {
            "provider-ready message role is not admitted"
        }
        require(
            content.isNotEmpty(),
        ) {
            "provider-ready message content must be non-empty"
        }
    }
}

// [VXG RealForever]
