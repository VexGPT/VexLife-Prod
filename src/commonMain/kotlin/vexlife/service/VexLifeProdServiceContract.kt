package vexlife.service

import kotlinx.serialization.Serializable

/**
 * Shared browser/service contract for the local VexLife implementation.
 *
 * This source owns only stable route/topology identity and wire projections. It does not own
 * Conversation meaning, Memory, Heretic lifecycle, prompt selection, presentation state, or process
 * lifetime. Prompt projections preserve the exact source identities formed by their rightful owners.
 */
object VexLifeProdServiceContract {
    const val SourceCurrentnessSchema =
        "vexlife-prod.local-service-source-currentness/v2"

    const val ConversationProjectionSchema =
        "vexlife-prod.local-service-conversation-projection/v1"

    const val SendResponseSchema =
        "vexlife-prod.local-service-send-response/v2"

    const val ErrorSchema =
        "vexlife-prod.local-service-error/v1"

    const val ServiceRef =
        "service.vexlife-prod.local"

    const val ProductRef =
        "product.vexlife-prod"

    const val Host =
        "127.0.0.1"

    const val Port =
        18_210

    const val Origin =
        "http://127.0.0.1:18210"

    const val SourceCurrentnessPath =
        "/api/v1/source-currentness"

    const val ConversationPath =
        "/api/v1/conversation"

    const val ConversationSendPath =
        "/api/v1/conversation/send"

    @Serializable
    data class SourceCurrentness(
        val schemaVersion: String,
        val serviceRef: String,
        val productRef: String,
        val serviceState: String,
        val sourceSha: String,
        val processPid: Long,
        val runtimeCurrentness: String,
        val startupQualifiedRuntimeReceiptRef: String,
        val currentConversationHeadSha256: String?,
        val effectsPerformed: Boolean,
    )

    @Serializable
    data class ConversationMessage(
        val eventRef: String,
        val eventSha256: String,
        val messageRef: String,
        val turnRef: String,
        val role: String,
        val content: String,
        val sequence: Long,
        val formedAt: String,
        val model: String?,
    )

    @Serializable
    data class ConversationProjection(
        val schemaVersion: String,
        val projectRef: String,
        val threadRef: String,
        val channelRef: String,
        val currentConversationHeadSha256: String?,
        val messages: List<ConversationMessage>,
        val abandonedAttemptCount: Int,
        val effectsPerformed: Boolean,
    )

    @Serializable
    data class SendRequest(
        val idempotencyKey: String,
        val content: String,
    ) {
        init {
            requirePortableRef(
                idempotencyKey,
                "idempotencyKey",
            )
            require(
                content.isNotEmpty(),
            ) {
                "send content must be non-empty"
            }
        }
    }

    @Serializable
    data class PromptContextProjection(
        val receiptRef: String,
        val semanticFingerprint: String,
        val contextMode: String,
        val priorConversationHeadSha256: String?,
        val selectedConversationEventRefs: List<String>,
        val providerMessageSetSha256: String,
        val providerMaterializedInputTokenEstimate: Int,
        val inputTokenBudget: Int,
        val reservedOutputTokens: Int,
        val hardTokenLimit: Int,
        val wholeHistoryEventEnumerationPerformed: Boolean,
        val memoryContextRef: String? =
            null,
        val memoryContextFingerprint: String? =
            null,
        val memorySourceRefs: List<String> =
            emptyList(),
        val memoryIncluded: Boolean,
        val memorySelectionPerformed: Boolean,
        val trainingSelectionPerformed: Boolean,
        val effectsPerformed: Boolean,
    )

    @Serializable
    data class SendResponse(
        val schemaVersion: String,
        val state: String,
        val recoveredCompletedTurn: Boolean,
        val modelCallPerformed: Boolean,
        val actualHttpCallPerformedNow: Boolean,
        val turnRef: String,
        val requestMessageRef: String,
        val responseMessageRef: String,
        val requestEventRef: String,
        val responseEventRef: String,
        val content: String,
        val model: String,
        val currentConversationHeadSha256: String,
        val contextRef: String,
        val promptContext: PromptContextProjection,
    )

    @Serializable
    data class ErrorResponse(
        val schemaVersion: String,
        val code: String,
        val message: String,
        val retrySameSendAttemptAllowed: Boolean?,
        val exactNextSafeRoute: String?,
    )

    internal fun requirePortableRef(
        value: String,
        label: String,
    ) {
        require(
            value.matches(
                Regex(
                    "[a-z0-9](?:[a-z0-9._-]{0,220}[a-z0-9])?",
                ),
            ),
        ) {
            "$label must be one portable ref"
        }
    }
}

// [VXG RealForever]
