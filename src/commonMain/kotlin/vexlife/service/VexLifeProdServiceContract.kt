package vexlife.service

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import vexlife.browser.VexLifeProdBrowserContract

/**
 * Shared browser/service contract for the local VexLife implementation.
 *
 * This source owns only stable route/topology identity and wire projections. It does not own
 * Conversation meaning, Memory, Heretic lifecycle, prompt selection, presentation state, or process
 * lifetime. Prompt projections preserve the exact source identities formed by their rightful owners.
 */
object VexLifeProdServiceContract {
    const val SourceCurrentnessSchema =
        "vexlife-prod.local-service-source-currentness/v3"

    const val CurrentStateProjectionSchema =
        "vexlife-prod.local-service-current-state/v1"

    const val ConversationProjectionSchema =
        "vexlife-prod.local-service-conversation-projection/v1"

    const val ContextProjectionSchema =
        "vexlife-prod.local-service-context-projection/v1"

    const val MemoryDreamProjectionSchema =
        "vexlife-prod.local-service-memory-dream-projection/v1"

    const val MemoryDreamRunResponseSchema =
        "vexlife-prod.local-service-memory-dream-run-response/v1"

    const val VisionProjectionSchema =
        "vexlife-prod.local-service-vision-projection/v1"

    const val VisionRuntimeResultSchema =
        "vextreme.macos-screen-system-picker-product-runtime-result/v1"

    const val SendResponseSchema =
        "vexlife-prod.local-service-send-response/v4"

    const val SendStreamEventSchema =
        "vexlife-prod.local-service-send-stream-event/v1"

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

    const val PresentationIndexPath =
        "/"

    const val PresentationCssPath =
        "/vexweb.css"

    const val PresentationJavascriptPath =
        "/vexweb.js"

    const val PresentationManifestPath =
        "/vexweb-manifest.json"

    const val SourceCurrentnessPath =
        "/api/v1/source-currentness"

    const val CurrentStatePath =
        "/api/v1/current-state"

    const val ConversationPath =
        "/api/v1/conversation"

    const val ContextPath =
        "/api/v1/context"

    const val MemoryDreamPath =
        "/api/v1/memory/dream"

    const val VisionPath =
        "/api/v1/vision"

    const val VisionLookNowPath =
        "/api/v1/vision/look-now"

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
        val frameHostOrigin: String,
        val runtimeCurrentness: String,
        val startupQualifiedRuntimeReceiptRef: String,
        val currentConversationHeadSha256: String?,
        val effectsPerformed: Boolean,
    )

    @Serializable
    data class CurrentStateProjection(
        val schemaVersion: String,
        val observedAt: String,
        val homePath: String,
        val homeDisposition: String,
        val platform: String,
        val architecture: String,
        val chipModel: String?,
        val hostEligibilityState: String,
        val hostEligibilityReason: String?,
        val totalMemoryBytes: Long?,
        val modelMinimumMemoryBytes: Long?,
        val freeStorageBytes: Long?,
        val activeModelRef: String,
        val activeModelProfileRef: String,
        val runtimeState: String,
        val runtimeCurrentness: String,
        val runtimeReceiptRef: String,
        val modelArtifactState: String,
        val modelArtifactTotalBytes: Long,
        val modelArtifactVerifiedBytes: Long,
        val modelArtifactMissingBytes: Long,
        val modelArtifactInvalidBytes: Long,
        val effectsPerformed: Boolean,
    ) {
        init {
            require(
                schemaVersion ==
                    CurrentStateProjectionSchema &&
                    homePath.isNotBlank() &&
                    platform.isNotBlank() &&
                    architecture.isNotBlank() &&
                    hostEligibilityState in
                    setOf(
                        "ELIGIBLE",
                        "UNSUPPORTED",
                    ) &&
                    runtimeState.isNotBlank() &&
                    runtimeCurrentness ==
                    "STARTUP_QUALIFIED_NOT_REOBSERVED" &&
                    modelArtifactState in
                    setOf(
                        "VERIFIED_AT_SETUP",
                        "VERIFIED_REUSABLE",
                        "INCOMPLETE",
                        "INVALID",
                        "ABSENT",
                    ) &&
                    modelArtifactTotalBytes >=
                    0 &&
                    modelArtifactVerifiedBytes >=
                    0 &&
                    modelArtifactMissingBytes >=
                    0 &&
                    modelArtifactInvalidBytes >=
                    0 &&
                    modelArtifactVerifiedBytes +
                    modelArtifactMissingBytes +
                    modelArtifactInvalidBytes ==
                    modelArtifactTotalBytes &&
                    !effectsPerformed,
            ) {
                "current-state projection is invalid"
            }
        }
    }

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
    data class MemoryDreamProjection(
        val schemaVersion: String,
        val state: String,
        val sourceConversationHeadSha256: String,
        val sourceScoreHeadSha256: String,
        val scoreSourceConversationHeadSha256: String,
        val sourceSemanticAuthorityHeadSha256: String,
        val currentDailyDreamHeadSha256: String?,
        val currentDailyStratumSha256: String?,
        val currentDayRef: String?,
        val currentDayIndex: Long?,
        val nextDayIndex: Long,
        val activeStatementCount: Int,
        val heldStatementCount: Int,
        val openLoopCount: Int,
        val effectsPerformed: Boolean,
    )

    @Serializable
    data class MemoryDreamRunRequest(
        val restInvocationAuthorityRef: String,
        val dayRef: String,
        val dayIndex: Long,
        val calendarDateRef: String,
        val timeZoneRef: String,
        val observedAt: String,
        val expectedConversationHeadSha256: String,
        val expectedScoreHeadSha256: String,
        val expectedDailyDreamHeadSha256: String?,
    ) {
        init {
            requirePortableRef(restInvocationAuthorityRef, "restInvocationAuthorityRef")
            requirePortableRef(dayRef, "dayRef")
            require(
                dayIndex >= 0 &&
                    calendarDateRef.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) &&
                    timeZoneRef.isNotBlank() &&
                    observedAt.isNotBlank(),
            ) {
                "memory Dream day/time identity is invalid"
            }
            requireSha256(expectedConversationHeadSha256, "expectedConversationHeadSha256")
            requireSha256(expectedScoreHeadSha256, "expectedScoreHeadSha256")
            expectedDailyDreamHeadSha256?.let { requireSha256(it, "expectedDailyDreamHeadSha256") }
        }
    }

    @Serializable
    data class MemoryDreamRunResponse(
        val schemaVersion: String,
        val state: String,
        val before: MemoryDreamProjection,
        val after: MemoryDreamProjection,
        val dailyDreamHeadSha256: String,
        val dailyStratumSha256: String,
        val memoryFrontierMutationPerformed: Boolean,
        val systemMemoryConsolidationPerformedNow: Boolean,
        val modelCallPerformed: Boolean,
        val lineageAwareGenerativeDreamRan: Boolean,
        val rhythmLearned: Boolean,
        val trainingRan: Boolean,
        val modelWeightsChanged: Boolean,
        val adapterChanged: Boolean,
        val synchronizationActivated: Boolean,
        val poweredDown: Boolean,
        val publicationPerformed: Boolean,
        val firstPersonAuthorityGranted: Boolean,
        val effectsPerformed: Boolean,
    )
    @Serializable
    data class VisionProjection(
        val schemaVersion: String,
        val state: String,
        val routeReachable: Boolean,
        val effectsPerformed: Boolean,
    ) {
        init {
            require(
                schemaVersion ==
                    VisionProjectionSchema &&
                    state in
                    setOf(
                        "HELD",
                        "READY",
                    ) &&
                    routeReachable ==
                    (
                        state ==
                            "READY"
                    ) &&
                    !effectsPerformed,
            ) {
                "Vision projection is invalid"
            }
        }
    }

    @Serializable
    data class VisionLookNowRequest(
        val requestRef: String,
    ) {
        init {
            requirePortableRef(
                requestRef,
                "requestRef",
            )
        }
    }

    @Serializable
    data class VisionRuntimeResult(
        val schemaVersion: String,
        val state: String,
        val requestRef: String,
        val sessionRefOrNull: String?,
        val connectionIdOrNull: String?,
        val selectionMode: String,
        val osOwnsSelectionUi: Boolean,
        val helperLaunched: Boolean,
        val projectionOrNull: JsonElement?,
        val activeSharingWitnessOrNull: JsonElement?,
        val activeVisualObservationOrNull: JsonElement?,
        val rawBytesIncluded: Boolean,
        val rawRetentionAllowed: Boolean,
        val visualModelInputAuthorized: Boolean,
        val memoryAccepted: Boolean,
        val trainingEligible: Boolean,
        val publicationEligible: Boolean,
        val continuousCaptureAllowed: Boolean,
        val backgroundCaptureAllowed: Boolean,
        val persistentHostPermissionEstablished: Boolean,
    ) {
        init {
            require(
                schemaVersion ==
                    VisionRuntimeResultSchema &&
                    selectionMode ==
                    "SINGLE_WINDOW" &&
                    osOwnsSelectionUi &&
                    !rawBytesIncluded &&
                    !rawRetentionAllowed &&
                    !visualModelInputAuthorized &&
                    !memoryAccepted &&
                    !trainingEligible &&
                    !publicationEligible &&
                    !continuousCaptureAllowed &&
                    !backgroundCaptureAllowed &&
                    !persistentHostPermissionEstablished,
            ) {
                "Vision runtime result fixed boundary is invalid"
            }
            requirePortableRef(
                requestRef,
                "requestRef",
            )

            when (
                state
            ) {
                "HELD_SAFETY_NOT_CURRENT" ->
                    require(
                        sessionRefOrNull ==
                            null &&
                            connectionIdOrNull ==
                            null &&
                            !helperLaunched &&
                            projectionOrNull ==
                            null &&
                            activeSharingWitnessOrNull ==
                            null &&
                            activeVisualObservationOrNull ==
                            null,
                    ) {
                        "held Vision result contains realization/capture truth"
                    }

                "USER_CANCELLED",
                "ONE_SHOT_CAPTURE_FAILED",
                "ONE_SHOT_CAPTURED",
                -> {
                    val sessionRef =
                        requireNotNull(
                            sessionRefOrNull,
                        ) {
                            "Vision runtime result requires sessionRef"
                        }
                    val connectionId =
                        requireNotNull(
                            connectionIdOrNull,
                        ) {
                            "Vision runtime result requires connectionId"
                        }
                    requirePortableRef(
                        sessionRef,
                        "sessionRef",
                    )
                    requirePortableRef(
                        connectionId,
                        "connectionId",
                    )
                    require(
                        sessionRef !=
                            connectionId &&
                            requestRef !=
                            sessionRef &&
                            requestRef !=
                            connectionId &&
                            helperLaunched &&
                            projectionOrNull !=
                            null,
                    ) {
                        "Vision runtime attempt binding is invalid"
                    }

                    if (
                        state ==
                            "ONE_SHOT_CAPTURED"
                    ) {
                        require(
                            activeSharingWitnessOrNull !=
                                null &&
                                activeVisualObservationOrNull !=
                                null,
                        ) {
                            "captured Vision result requires canonical witness and observation"
                        }
                    } else {
                        require(
                            activeSharingWitnessOrNull ==
                                null &&
                                activeVisualObservationOrNull ==
                                null,
                        ) {
                            "non-captured Vision result cannot claim witness/observation"
                        }
                    }
                }

                else ->
                    error(
                        "Vision runtime result state is unsupported",
                    )
            }
        }
    }

    @Serializable
    data class BrowserContextInput(
        val pageUrl: String,
        val title: String?,
        val text: String,
        val capture: String,
    ) {
        init {
            VexLifeProdBrowserContract.admitPageUrl(
                pageUrl,
            )
            VexLifeProdBrowserContract.admitTitle(
                title,
            )
            VexLifeProdBrowserContract.admitRenderedText(
                text,
            )
            require(
                capture ==
                    VexLifeProdBrowserContract.CaptureRendered,
            ) {
                "VexLife-Prod browser context capture is not admitted"
            }
        }
    }

    @Serializable
    data class SendRequest(
        val idempotencyKey: String,
        val content: String,
        val browserContext: BrowserContextInput? =
            null,
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
    data class CapabilityFrameProjection(
        val frameRef: String,
        val semanticFingerprint: String,
        val sourceRefs: List<String>,
        val systemMessageSha256: String,
        val estimatedSystemMessageTokens: Int,
        val availableCapabilityRefs: List<String>,
        val heldCapabilityRefs: List<String>,
        val unavailableCapabilityRefs: List<String>,
        val unknownCapabilityRefs: List<String>,
        val omittedCapabilityRefs: List<String>,
        val omissionReasons: List<String>,
        val tokenBudget: Int,
        val usedTokens: Int,
        val truncated: Boolean,
        val effectAuthorityGranted: Boolean,
    )

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
        val browserContextIncluded: Boolean,
        val browserContextRef: String?,
        val browserPageUrl: String?,
        val browserTitle: String?,
        val browserContextSha256: String?,
        val browserContextCharacters: Int?,
        val browserContextCapture: String?,
        val workingContinuityIncluded: Boolean =
            false,
        val workingContinuityContextRef: String? =
            null,
        val workingContinuityContextFingerprint: String? =
            null,
        val capabilityFrameIncluded: Boolean =
            false,
        val capabilityFrame: CapabilityFrameProjection? =
            null,
        val effectsPerformed: Boolean,
    )

    @Serializable
    data class ContextProjection(
        val schemaVersion: String,
        val currentConversationHeadSha256: String?,
        val turnRef: String?,
        val contextPath: String?,
        val promptContext: PromptContextProjection?,
        val effectsPerformed: Boolean,
    )

    @Serializable
    data class HumanReasoningProjection(
        val state: String,
        val text: String?,
        val contentSha256: String?,
        val characterCount: Int,
        val persisted: Boolean,
    ) {
        init {
            require(
                state in
                    setOf(
                        "ABSENT",
                        "PRESENT_EPHEMERAL",
                        "UNAVAILABLE_AFTER_IDEMPOTENT_RECOVERY",
                    ),
            ) {
                "human reasoning projection state is invalid"
            }
            require(
                !persisted,
            ) {
                "human reasoning projection must remain ephemeral"
            }

            when (
                state
            ) {
                "PRESENT_EPHEMERAL" -> {
                    require(
                        !text.isNullOrEmpty() &&
                            contentSha256
                                ?.matches(
                                    Regex(
                                        "[0-9a-f]{64}",
                                    ),
                                ) ==
                            true &&
                            characterCount >
                            0,
                    ) {
                        "present human reasoning requires exact ephemeral text/hash/count"
                    }
                }

                "UNAVAILABLE_AFTER_IDEMPOTENT_RECOVERY" -> {
                    require(
                        text ==
                            null &&
                            characterCount >=
                            0 &&
                            (
                                contentSha256 ==
                                    null ||
                                    contentSha256.matches(
                                        Regex(
                                            "[0-9a-f]{64}",
                                        ),
                                    )
                            ),
                    ) {
                        "recovered human reasoning may retain only durable hash/count evidence"
                    }
                }

                else -> {
                    require(
                        text ==
                            null &&
                            contentSha256 ==
                            null &&
                            characterCount ==
                            0,
                    ) {
                        "non-present human reasoning must carry no raw or fingerprint material"
                    }
                }
            }
        }
    }

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
        val contextPath: String,
        val promptContext: PromptContextProjection,
        val humanReasoning: HumanReasoningProjection,
    )

    @Serializable
    data class SendStreamEvent(
        val schemaVersion: String =
            SendStreamEventSchema,
        val type: String,
        val phase: String? =
            null,
        val delta: String? =
            null,
        val accumulatedCharacters: Int? =
            null,
        val response: SendResponse? =
            null,
        val error: ErrorResponse? =
            null,
    ) {
        init {
            require(
                type in
                    setOf(
                        "PHASE",
                        "REASONING_DELTA",
                        "CONTENT_DELTA",
                        "COMPLETED",
                        "FAILED",
                    ),
            ) {
                "send stream event type is invalid"
            }
        }
    }

    @Serializable
    data class ErrorResponse(
        val schemaVersion: String,
        val code: String,
        val message: String,
        val retrySameSendAttemptAllowed: Boolean?,
        val exactNextSafeRoute: String?,
    )

    private fun requireSha256(
        value: String,
        label: String,
    ) {
        require(value.matches(Regex("[0-9a-f]{64}"))) {
            "$label must be one lowercase SHA-256"
        }
    }
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
