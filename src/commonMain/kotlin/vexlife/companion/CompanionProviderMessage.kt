package vexlife.companion

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Provider-ready local Companion message.
 *
 * Prompt/context owners decide which source truth is materialized into these messages. The
 * Connection only transports them.
 *
 * For reasoning-field providers, a selected historical assistant response may carry the exact
 * model-emitted reasoning that belonged to that completed response. It stays separate from the
 * human-facing [content] so Conversation truth is not rewritten merely to preserve provider
 * continuation.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class CompanionProviderMessage(
    val role: String,
    val content: String,
    @SerialName("reasoning_content")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val reasoningContent: String? = null,
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
        require(
            reasoningContent ==
                null ||
                (
                    role ==
                        "assistant" &&
                        reasoningContent.isNotEmpty()
                    )
        ) {
            "provider-ready reasoning continuation belongs only to a non-empty assistant message"
        }
    }
}

// [VXG RealForever]
