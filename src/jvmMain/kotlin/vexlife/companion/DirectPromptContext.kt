package vexlife.companion

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlinx.serialization.Serializable
import vexlife.contract.VexContractJson
import vexlife.conversation.DirectConversationStore

/**
 * Bounded direct Companion prompt materialization.
 *
 * CONTINUE selects exactly the immediate prior completed REQUEST/RESPONSE pair. First turn selects
 * no prior Conversation events. Memory is intentionally held until its rightful owner is ported.
 *
 * This owner performs no provider call and no durable mutation.
 */
object DirectPromptContext {
    const val ReceiptSchema =
        "vexlife-prod.direct-prompt-materialization-receipt/v1"

    const val InputTokenBudget =
        192_000

    const val ReservedOutputTokens =
        DirectCompanionConnection.OutputTokenLimit

    const val HardTokenLimit =
        208_000

    @Serializable
    private data class ReceiptCore(
        val schemaVersion: String,
        val contextMode: String,
        val priorConversationHeadSha256: String?,
        val selectedConversationEventRefs: List<String>,
        val selectedConversationEventSha256: List<String>,
        val currentRequestEventRef: String,
        val currentRequestEventSha256: String,
        val providerMessageSetSha256: String,
        val providerMaterializedInputTokenEstimate: Int,
        val inputTokenBudget: Int,
        val reservedOutputTokens: Int,
        val hardTokenLimit: Int,
        val wholeHistoryEventEnumerationPerformed: Boolean,
        val memoryIncluded: Boolean,
        val memorySelectionPerformed: Boolean,
        val trainingSelectionPerformed: Boolean,
        val effectsPerformed: Boolean,
    )

    @Serializable
    @ConsistentCopyVisibility
    data class Receipt internal constructor(
        val schemaVersion: String,
        val receiptRef: String,
        val semanticFingerprint: String,
        val contextMode: String,
        val priorConversationHeadSha256: String?,
        val selectedConversationEventRefs: List<String>,
        val selectedConversationEventSha256: List<String>,
        val currentRequestEventRef: String,
        val currentRequestEventSha256: String,
        val providerMessageSetSha256: String,
        val providerMaterializedInputTokenEstimate: Int,
        val inputTokenBudget: Int,
        val reservedOutputTokens: Int,
        val hardTokenLimit: Int,
        val wholeHistoryEventEnumerationPerformed: Boolean,
        val memoryIncluded: Boolean,
        val memorySelectionPerformed: Boolean,
        val trainingSelectionPerformed: Boolean,
        val effectsPerformed: Boolean,
    )

    @ConsistentCopyVisibility
    data class Materialization internal constructor(
        val messages: List<CompanionProviderMessage>,
        val receipt: Receipt,
    )

    fun materialize(
        prior: DirectConversationStore.ImmediatePriorCompletedTurn?,
        currentRequest: DirectConversationStore.Event,
    ): Materialization {
        require(
            currentRequest.eventKind ==
                "REQUEST",
        ) {
            "direct prompt context requires one durable current REQUEST"
        }

        val selected =
            if (
                prior ==
                    null
            ) {
                require(
                    currentRequest.sequence ==
                        0L &&
                        currentRequest.priorEventSha256 ==
                        null,
                ) {
                    "first-turn prompt context requires one genesis request"
                }
                emptyList()
            } else {
                require(
                    prior.exactEventFileReadCount ==
                        2 &&
                        !prior.wholeHistoryEventEnumerationPerformed &&
                        currentRequest.sequence ==
                        prior.head.sequence +
                        1 &&
                        currentRequest.priorEventSha256 ==
                        prior.responseEvent.eventSha256 &&
                        currentRequest.threadRef ==
                        prior.head.threadRef &&
                        currentRequest.channelRef ==
                        prior.head.channelRef,
                ) {
                    "direct prompt context current request does not immediately follow the exact prior completed turn"
                }
                listOf(
                    prior.requestEvent,
                    prior.responseEvent,
                )
            }

        val messages =
            buildList {
                for (
                    event in
                    selected
                ) {
                    add(
                        CompanionProviderMessage(
                            role =
                                when (
                                    event.eventKind
                                ) {
                                    "REQUEST" ->
                                        "user"

                                    "RESPONSE" ->
                                        "assistant"

                                    else ->
                                        error(
                                            "selected direct Conversation event has unsupported provider role",
                                        )
                                },
                            content = event.content,
                        ),
                    )
                }
                add(
                    CompanionProviderMessage(
                        role = "user",
                        content = currentRequest.content,
                    ),
                )
            }

        val estimate =
            messages.sumOf {
                estimatedMessageTokens(
                    it.content,
                )
            }

        require(
            estimate <=
                InputTokenBudget &&
                estimate +
                ReservedOutputTokens <=
                HardTokenLimit,
        ) {
            "direct prompt materialization exceeds the admitted input/output token budget"
        }

        val providerMessageSetSha256 =
            sha256(
                VexContractJson.encode(
                    messages,
                ),
            )
        val core =
            ReceiptCore(
                schemaVersion = ReceiptSchema,
                contextMode =
                    if (
                        prior ==
                            null
                    ) {
                        "FIRST_TURN"
                    } else {
                        "IMMEDIATE_PRIOR_COMPLETED_TURN"
                    },
                priorConversationHeadSha256 =
                    prior
                        ?.head
                        ?.conversationHeadSha256,
                selectedConversationEventRefs =
                    selected.map {
                        it.eventRef
                    },
                selectedConversationEventSha256 =
                    selected.map {
                        it.eventSha256
                    },
                currentRequestEventRef = currentRequest.eventRef,
                currentRequestEventSha256 = currentRequest.eventSha256,
                providerMessageSetSha256 = providerMessageSetSha256,
                providerMaterializedInputTokenEstimate = estimate,
                inputTokenBudget = InputTokenBudget,
                reservedOutputTokens = ReservedOutputTokens,
                hardTokenLimit = HardTokenLimit,
                wholeHistoryEventEnumerationPerformed = false,
                memoryIncluded = false,
                memorySelectionPerformed = false,
                trainingSelectionPerformed = false,
                effectsPerformed = false,
            )
        val fingerprint =
            sha256(
                VexContractJson.encode(
                    core,
                ),
            )
        val receipt =
            Receipt(
                schemaVersion = core.schemaVersion,
                receiptRef =
                    "receipt.vexlife-prod.direct-prompt." +
                        fingerprint.take(
                            32,
                        ),
                semanticFingerprint = fingerprint,
                contextMode = core.contextMode,
                priorConversationHeadSha256 = core.priorConversationHeadSha256,
                selectedConversationEventRefs = core.selectedConversationEventRefs,
                selectedConversationEventSha256 = core.selectedConversationEventSha256,
                currentRequestEventRef = core.currentRequestEventRef,
                currentRequestEventSha256 = core.currentRequestEventSha256,
                providerMessageSetSha256 = core.providerMessageSetSha256,
                providerMaterializedInputTokenEstimate = core.providerMaterializedInputTokenEstimate,
                inputTokenBudget = core.inputTokenBudget,
                reservedOutputTokens = core.reservedOutputTokens,
                hardTokenLimit = core.hardTokenLimit,
                wholeHistoryEventEnumerationPerformed = core.wholeHistoryEventEnumerationPerformed,
                memoryIncluded = core.memoryIncluded,
                memorySelectionPerformed = core.memorySelectionPerformed,
                trainingSelectionPerformed = core.trainingSelectionPerformed,
                effectsPerformed = core.effectsPerformed,
            )

        return Materialization(
            messages = messages,
            receipt = receipt,
        )
    }

    fun reverify(
        materialization: Materialization,
        prior: DirectConversationStore.ImmediatePriorCompletedTurn?,
        currentRequest: DirectConversationStore.Event,
    ) {
        val current =
            materialize(
                prior = prior,
                currentRequest = currentRequest,
            )

        require(
            current ==
                materialization,
        ) {
            "direct prompt materialization is no longer current at the provider boundary"
        }
    }

    fun estimatedMessageTokens(
        content: String,
    ): Int {
        val bytes =
            content
                .toByteArray(
                    StandardCharsets.UTF_8,
                )
                .size

        return maxOf(
            1,
            (
                bytes +
                    3
            ) /
                4,
        ) +
            4
    }

    private fun sha256(
        value: String,
    ): String =
        MessageDigest
            .getInstance(
                "SHA-256",
            )
            .digest(
                value.toByteArray(
                    StandardCharsets.UTF_8,
                ),
            )
            .joinToString(
                "",
            ) {
                "%02x".format(
                    it.toInt() and
                        0xff,
                )
            }
}

// [VXG RealForever]
