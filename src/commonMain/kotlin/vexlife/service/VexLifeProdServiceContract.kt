package vexlife.service

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import vexlife.attachment.VexLifeAttachmentDraftFamily
import vexlife.artifact.VexLifeLocalArtifact
import vexlife.browser.VexLifeProdBrowserContract
import vexlife.continuity.VexLifePrivateModeFamily
import vexlife.context.VexLifeContextProfile
import vexlife.context.VexLifeContextProfileFamily
import vexlife.conversation.VexLifeConversationTurnFamily
import vexlife.home.VexLifeProdHome
import vexlife.memory.VexLifeMemoryFamily
import vexlife.release.VexLifeReleaseIdentity
import vexlife.release.VexLifeReleaseInboxFamily
import vexlife.sources.VexLifeLocalSourcesFamily
import vexlife.text.VexLifeText
import vexlife.workspace.VexLifeWorkspaceEffectsFamily
import vexlife.vision.VexLifeVisionFamily

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
        "vexlife-prod.local-service-current-state/v2"

    const val ConversationProjectionSchema =
        "vexlife-prod.local-service-conversation-projection/v2"

    const val ConversationResourceMetadataSchema =
        "vexlife-prod.local-service-conversation-resource-metadata/v1"

    const val ConversationResourceUnavailableTextKey =
        "workspace.status.sent-resource-local-copy-unavailable"

    const val ConversationResourceInvalidTextKey =
        "workspace.status.sent-resource-local-copy-invalid"

    const val ConversationResourceReviewFailedTextKey =
        "workspace.status.sent-resource-review-failed"

    const val ConversationTurnProjectionSchema =
        "vexlife-prod.local-service-conversation-turn-projection/v2"

    const val FollowUpReadinessUnavailable = "UNAVAILABLE"
    const val FollowUpReadinessLowerMemory = "LOWER_MEMORY_REALIZATION"
    const val FollowUpReadinessNotYetPrepared = "NOT_YET_PREPARED"
    const val FollowUpReadinessPreparing = "PREPARING"
    const val FollowUpReadinessSuccessorReset = "SUCCESSOR_RESET"
    const val FollowUpReadinessFasterEnabled = "FASTER_FOLLOWUPS_ENABLED"

    val FollowUpReadinessStates =
        setOf(
            FollowUpReadinessUnavailable,
            FollowUpReadinessLowerMemory,
            FollowUpReadinessNotYetPrepared,
            FollowUpReadinessPreparing,
            FollowUpReadinessSuccessorReset,
            FollowUpReadinessFasterEnabled,
        )

    const val AttachmentDraftProjectionSchema =
        "vexlife-prod.local-service-attachment-draft-projection/v1"

    const val AttachmentStageResponseSchema =
        "vexlife-prod.local-service-attachment-stage-response/v2"

    const val AttachmentControlResponseSchema =
        "vexlife-prod.local-service-attachment-control-response/v1"

    const val LocalSourcesProjectionSchema =
        "vexlife-prod.local-service-local-sources-projection/v1"

    const val LocalSourcesControlResponseSchema =
        "vexlife-prod.local-service-local-sources-control-response/v1"

    const val WorkspaceEffectsProjectionSchema =
        "vexlife-prod.local-service-workspace-effects-projection/v1"

    const val WorkspaceEffectsControlResponseSchema =
        "vexlife-prod.local-service-workspace-effects-control-response/v1"

    const val ContextProjectionSchema =
        "vexlife-prod.local-service-context-projection/v2"

    const val ContextStateEmpty =
        "EMPTY"

    const val ContextStateAvailable =
        "AVAILABLE"

    const val ContextStateHistoricalWithoutProductEvidence =
        "HISTORICAL_WITHOUT_PRODUCT_EVIDENCE"

    const val ContextStateHeldUnsupportedDurablePromptReceipt =
        "HELD_UNSUPPORTED_DURABLE_PROMPT_RECEIPT"

    const val ContextHeldReasonPromptReceiptSchemaUnsupported =
        "PROMPT_RECEIPT_SCHEMA_NOT_SUPPORTED_BY_THIS_BUILD"

    const val ContextProfilesProjectionSchema =
        "vexlife-prod.local-service-context-profiles-projection/v2"

    const val ContextProfileControlResultSchema =
        "vexlife-prod.local-service-context-profile-control-result/v2"

    const val ContextProfileHomeRevealResultSchema =
        "vexlife-prod.local-service-context-profile-home-reveal-result/v1"

    const val VexLabsProjectionSchema =
        "vexlife-prod.local-service-vex-labs-native-test-projection/v2"

    const val VexLabsControlResultSchema =
        "vexlife-prod.local-service-vex-labs-native-test-control-result/v2"

    const val VexLabsSendResultSchema =
        "vexlife-prod.local-service-vex-labs-native-test-send-result/v2"

    const val MemoryDreamProjectionSchema =
        "vexlife-prod.local-service-memory-dream-projection/v1"

    const val MemoryDreamRunResponseSchema =
        "vexlife-prod.local-service-memory-dream-run-response/v1"

    const val MemoryProjectionSchema = "vexlife-prod.local-service-memory-projection/v2"

    const val ReleaseAlertsProjectionSchema =
        "vexlife-prod.local-service-release-alerts-projection/v1"

    const val ReleaseAlertsControlResultSchema =
        "vexlife-prod.local-service-release-alerts-control-result/v1"

    const val PrivateProjectionSchema = "vexlife-prod.local-service-private-projection/v2"
    const val PrivateControlResultSchema = "vexlife-prod.local-service-private-control-result/v2"

    const val VisionProjectionSchema =
        "vexlife-prod.local-service-vision-projection/v3"

    const val VisionControlResultSchema =
        "vexlife-prod.local-service-vision-control-result/v2"

    const val VisionRuntimeResultSchema =
        "vextreme.macos-screen-system-picker-product-runtime-result/v1"

    const val SendResponseSchema =
        "vexlife-prod.local-service-send-response/v4"

    const val SendStreamEventSchema =
        "vexlife-prod.local-service-send-stream-event/v1"

    const val ConversationCancelResponseSchema =
        "vexlife-prod.local-service-conversation-cancel-response/v2"

    @Serializable
    enum class ConversationControlOutcome {
        REALIZED,
        NOT_LAWFUL_NOW,
    }

    const val ErrorSchema =
        "vexlife-prod.local-service-error/v1"

    const val ServiceRef =
        "service.vexlife-prod.local"

    const val ProductRef =
        "product.vexlife-prod"

    const val PresentationSourceHeader =
        "x-vexlife-presentation-source"

    const val StalePresentationCode =
        "STALE_PRESENTATION"

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

    const val ConversationResourceMetadataPath =
        "/api/v1/conversation/resource/metadata"

    const val ConversationResourceContentPath =
        "/api/v1/conversation/resource/content"

    const val ConversationResourceEventRefHeader =
        "x-vexlife-request-event-ref"

    const val ConversationResourceEventHashHeader =
        "x-vexlife-request-event-hash"

    const val ConversationResourceSequenceHeader =
        "x-vexlife-request-sequence"

    const val ConversationResourceRefHeader =
        "x-vexlife-request-resource-ref"

    const val ConversationTurnPath =
        "/api/v1/conversation/turn"

    const val ContextPath =
        "/api/v1/context"

    const val ContextProfilesPath =
        "/api/v1/context-profiles"

    const val ContextProfilesControlPath =
        "/api/v1/context-profiles/control"

    const val ContextProfileHomeRevealPath =
        "/api/v1/context-profiles/reveal-in-home"

    const val ContextProfileExportPath =
        "/api/v1/context-profiles/export"

    const val VexLabsPath =
        "/api/v1/vex-labs/native-test"

    const val VexLabsControlPath =
        "/api/v1/vex-labs/native-test/control"

    const val VexLabsSendPath =
        "/api/v1/vex-labs/native-test/send"

    const val MemoryPath = "/api/v1/memory"

    const val ReleaseAlertsPath = "/api/v1/release-alerts"
    const val ReleaseAlertsControlPath = "/api/v1/release-alerts/control"

    /**
     * The foundational field as Vex would be given it now: the exact kernel words, every part with
     * the depth that stays in source, its sources, what is unresolved, and the revision record.
     */
    const val FoundationalFieldPath = "/api/v1/foundational-field"
    const val FoundationalFieldSchema = "vexlife-prod.local-service-foundational-field/v1"

    const val SettingsPath = "/api/v1/settings"

    /** Settings + currentness for the person's selected local speech-to-text realization. */
    const val LocalAsrPath = "/api/v1/local-asr"

    /** Load the selected local recognizer before microphone capture enters Listening. */
    const val LocalAsrPreparePath = "/api/v1/local-asr/prepare"

    /** One bounded local-only transcription effect; request body is one mono PCM WAV. */
    const val LocalAsrTranscribePath = "/api/v1/local-asr/transcribe"

    /** The person's microphone-language preference; independent from Vex reply language. */
    const val LocalAsrSpokenLanguagePath = "/api/v1/local-asr/spoken-language"

    /** What recognized speech becomes in the editable Composer. */
    const val LocalAsrComposerOutputPath = "/api/v1/local-asr/composer-output"

    /** Download/use/remove one offered local speech-recognition model artifact. */
    const val LocalAsrModelControlPath = "/api/v1/local-asr/model/control"

    const val LocalAsrDictationPassHeader = "x-vexlife-dictation-pass"

    enum class LocalAsrDictationPass(val wire: String) {
        Partial("partial"),
        Final("final"),
        ;

        companion object {
            fun forWire(value: String): LocalAsrDictationPass? =
                entries.firstOrNull { it.wire == value }
        }
    }

    const val LocalAsrProjectionSchema =
        "vexlife-prod.local-asr-projection/v1"

    const val LocalAsrTranscriptionResponseSchema =
        "vexlife-prod.local-asr-transcription-response/v1"

    const val DiagnosticsPath = "/api/v1/diagnostics"

    /** Settings → Models: offered models as this Home holds them, and their Controls. */
    const val ModelsPath = "/api/v1/models"
    const val ModelsSourcesPath = "/api/v1/models/sources"
    const val ModelsSourcesControlPath = "/api/v1/models/sources/control"
    const val PartnerConnectionsPath = "/api/v1/partner-connections"
    const val PartnerConnectionsControlPath = "/api/v1/partner-connections/control"
    const val PartnerConnectionAvailabilitySchema =
        "vexlife-prod.partner-connection-availability/v1"
    const val PartnerConnectionAvailabilityHeldFunding =
        "HELD_FUNDING"
    const val PartnerConnectionAvailabilityReady =
        "READY_FOR_LIVE_TESTING"
    const val PartnerConnectionControlSchema =
        "vexlife-prod.partner-connection-control-response/v1"
    const val PartnerConnectionControlHeader =
        "x-vexlife-partner-control"
    const val PartnerConnectionProviderRefHeader =
        "x-vexlife-partner-provider-ref"
    const val PartnerConnectionWorkspaceIdHeader =
        "x-vexlife-partner-workspace-id"
    const val ModelsControlPath = "/api/v1/models/control"
    const val ModelsImportCheckPath = "/api/v1/models/import/check"
    const val ModelsImportAddPath = "/api/v1/models/import/add"
    const val DiagnosticsControlPath = "/api/v1/diagnostics/control"
    const val DiagnosticsExportPath = "/api/v1/diagnostics/export"
    const val ChronologyPath = "/api/v1/chronology"
    /** The workspace scope's words in the person's language. Routes take no query parameters. */
    const val WorkspaceStringsPath = "/api/v1/strings/workspace"
    const val ReleaseAlertsStringsPath = "/api/v1/strings/release-alerts"
    const val SettingsSchema = "vexlife-prod.local-service-settings/v2"

    const val PrivatePath = "/api/v1/private"
    const val PrivateControlPath = "/api/v1/private/control"
    const val PrivateSendPath = "/api/v1/private/send"
    const val PrivateNoteProposePath = "/api/v1/private/note/propose"
    const val PrivateNoteKeepPath = "/api/v1/private/note/keep"
    const val PrivateNoteDeclinePath = "/api/v1/private/note/decline"

    /** Longest private message or kept note the service accepts. */
    const val PrivateTextLimit = 20_000

    const val MemoryDreamPath =
        "/api/v1/memory/dream"

    const val VisionPath =
        "/api/v1/vision"

    const val VisionLookNowPath =
        "/api/v1/vision/look-now"

    const val VisionScopePath =
        "/api/v1/vision/scope"

    /** Family-resolved Vision Control (Engage / Stop) against freshly observed Vision State. */
    const val VisionControlPath =
        "/api/v1/vision/control"

    const val VisionScopeEngagePath =
        "/api/v1/vision/scope/engage"

    const val VisionScopeStopPath =
        "/api/v1/vision/scope/stop"

    const val VisionSelectedScopeProjectionSchema =
        "vextreme.macos-screen-selected-scope-session-projection/v1"

    const val VisionSelectedScopeRuntimeResponseSchema =
        "vextreme.macos-screen-selected-scope-product-runtime-response/v1"

    const val ConversationSendPath =
        "/api/v1/conversation/send"

    const val ConversationCancelPath =
        "/api/v1/conversation/cancel"

    const val AttachmentDraftPath =
        "/api/v1/attachments/draft"

    const val AttachmentStagePath =
        "/api/v1/attachments/draft/stage"

    const val AttachmentControlPath =
        "/api/v1/attachments/draft/control"

    const val LocalSourcesPath =
        "/api/v1/local-sources"

    const val LocalSourcesControlPath =
        "/api/v1/local-sources/control"

    const val WorkspaceEffectsPath =
        "/api/v1/workspace-effects"

    const val WorkspaceEffectsControlPath =
        "/api/v1/workspace-effects/control"

    const val AttachmentDraftRefHeader =
        "x-vexlife-attachment-draft-ref"

    const val AttachmentNameHeader =
        "x-vexlife-attachment-name"

    @Serializable
    data class PartnerConnectionAvailabilityProjection(
        val schemaVersion: String,
        val state: String,
        val connectEnabled: Boolean,
        val liveProviderTestingFunded: Boolean,
        val effectsPerformed: Boolean,
    ) {
        init {
            require(
                schemaVersion ==
                    PartnerConnectionAvailabilitySchema &&
                    state in
                    setOf(
                        PartnerConnectionAvailabilityHeldFunding,
                        PartnerConnectionAvailabilityReady,
                    ) &&
                    connectEnabled ==
                    liveProviderTestingFunded &&
                    !effectsPerformed,
            ) {
                "Partner connection availability projection is invalid"
            }
        }
    }

    enum class PartnerConnectionControl(
        val wire: String,
    ) {
        Connect("CONNECT"),
        Replace("REPLACE"),
        Remove("REMOVE"),
        Recover("RECOVER"),
        UpdateScope("UPDATE_SCOPE"),
        ;

        companion object {
            fun forWire(
                value: String,
            ): PartnerConnectionControl? =
                entries.firstOrNull {
                    it.wire ==
                        value
                }
        }
    }

    @Serializable
    data class PartnerConnectionControlResult(
        val providerRef: String,
        val state: String,
        val credentialRef: String?,
        val recoveryPerformed: Boolean,
    ) {
        init {
            require(
                providerRef.isNotBlank() &&
                    state.matches(
                        Regex(
                            "[A-Z][A-Z0-9_]*",
                        ),
                    ) &&
                    (
                        credentialRef ==
                            null ||
                            credentialRef.matches(
                                Regex(
                                    "credential\\.vexlife\\.partner\\.[a-z0-9][a-z0-9._-]{7,180}",
                                ),
                            )
                    ),
            ) {
                "Partner connection control result is invalid"
            }
        }
    }

    @Serializable
    data class PartnerConnectionControlResponse(
        val schemaVersion: String,
        val control: String,
        val results: List<PartnerConnectionControlResult>,
        val effectsPerformed: Boolean,
    ) {
        init {
            require(
                schemaVersion ==
                    PartnerConnectionControlSchema &&
                    PartnerConnectionControl.forWire(
                        control,
                    ) !=
                    null &&
                    effectsPerformed ==
                    results.isNotEmpty(),
            ) {
                "Partner connection control response is invalid"
            }
        }
    }

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
        val homeRef: String?,
        val deviceRef: String?,
        val companionLineageRef: String?,
        val platform: String,
        val architecture: String,
        val chipModel: String?,
        val hostEligibilityState: String,
        val hostEligibilityReason: String?,
        val totalMemoryBytes: Long?,
        val modelMinimumMemoryBytes: Long?,
        val freeStorageBytes: Long?,
        val modelSelectionState: String,
        val activeModelRef: String?,
        val activeModelProfileRef: String?,
        val runtimeState: String,
        val runtimeCurrentness: String,
        val runtimeReceiptPath: String?,
        val modelArtifactState: String,
        val modelArtifactTotalBytes: Long?,
        val modelArtifactPresentBySizeBytes: Long?,
        val modelArtifactMissingBytes: Long?,
        val modelArtifactInvalidSizeBytes: Long?,
        val effectsPerformed: Boolean,
    ) {
        init {
            val homeIdentityValid =
                if (
                    VexLifeProdHome.LocationDisposition
                        .forWireValue(
                            homeDisposition,
                        )
                        ?.identityPresent ==
                    false
                ) {
                    homeRef ==
                        null &&
                        deviceRef ==
                        null &&
                        companionLineageRef ==
                        null
                } else {
                    !homeRef.isNullOrBlank() &&
                        !deviceRef.isNullOrBlank() &&
                        !companionLineageRef.isNullOrBlank()
                }

            require(
                schemaVersion ==
                    CurrentStateProjectionSchema &&
                    observedAt.isNotBlank() &&
                    homePath.isNotBlank() &&
                    homeDisposition.isNotBlank() &&
                    homeIdentityValid &&
                    platform.isNotBlank() &&
                    architecture.isNotBlank() &&
                    hostEligibilityState in
                    setOf(
                        "ELIGIBLE",
                        "UNSUPPORTED",
                    ) &&
                    modelSelectionState in
                    setOf(
                        "UNCONFIGURED",
                        "HERETIC_ESTABLISHMENT_PENDING",
                        "BOUND_HERETIC_PRODUCT_MODEL",
                    ) &&
                    runtimeState in
                    setOf(
                        "NOT_SELECTED",
                        "ESTABLISHMENT_PENDING",
                        "BOUND_PRODUCT_MODEL",
                    ) &&
                    runtimeCurrentness in
                    setOf(
                        "NOT_APPLICABLE",
                        "CONFIGURATION_ONLY_NOT_REOBSERVED",
                    ) &&
                    modelArtifactState in
                    setOf(
                        "NOT_SELECTED",
                        "ABSENT",
                        "PARTIAL_BY_SIZE",
                        "COMPLETE_BY_SIZE_NOT_HASHED",
                        "INVALID_SIZE",
                    ) &&
                    !effectsPerformed,
            ) {
                "current-state projection is invalid"
            }

            when (
                modelSelectionState
            ) {
                "UNCONFIGURED" ->
                    require(
                        activeModelRef ==
                            null &&
                            activeModelProfileRef ==
                            null &&
                            modelMinimumMemoryBytes ==
                            null &&
                            runtimeState ==
                            "NOT_SELECTED" &&
                            runtimeCurrentness ==
                            "NOT_APPLICABLE" &&
                            runtimeReceiptPath ==
                            null &&
                            modelArtifactState ==
                            "NOT_SELECTED" &&
                            modelArtifactTotalBytes ==
                            null &&
                            modelArtifactPresentBySizeBytes ==
                            null &&
                            modelArtifactMissingBytes ==
                            null &&
                            modelArtifactInvalidSizeBytes ==
                            null,
                    ) {
                        "unconfigured current-state projection claims selected-model truth"
                    }

                "HERETIC_ESTABLISHMENT_PENDING",
                "BOUND_HERETIC_PRODUCT_MODEL",
                -> {
                    require(
                        !activeModelRef.isNullOrBlank() &&
                            !activeModelProfileRef.isNullOrBlank() &&
                            (
                                modelMinimumMemoryBytes ==
                                    null ||
                                    modelMinimumMemoryBytes >=
                                    0
                            ) &&
                            runtimeCurrentness ==
                            "CONFIGURATION_ONLY_NOT_REOBSERVED" &&
                            !runtimeReceiptPath.isNullOrBlank() &&
                            modelArtifactState !=
                            "NOT_SELECTED" &&
                            modelArtifactTotalBytes !=
                            null &&
                            modelArtifactPresentBySizeBytes !=
                            null &&
                            modelArtifactMissingBytes !=
                            null &&
                            modelArtifactInvalidSizeBytes !=
                            null &&
                            modelArtifactTotalBytes >=
                            0 &&
                            modelArtifactPresentBySizeBytes >=
                            0 &&
                            modelArtifactMissingBytes >=
                            0 &&
                            modelArtifactInvalidSizeBytes >=
                            0 &&
                            modelArtifactPresentBySizeBytes +
                            modelArtifactMissingBytes +
                            modelArtifactInvalidSizeBytes ==
                            modelArtifactTotalBytes,
                    ) {
                        "selected-model current-state projection is incomplete"
                    }

                    require(
                        (
                            modelSelectionState ==
                                "HERETIC_ESTABLISHMENT_PENDING" &&
                                runtimeState ==
                                "ESTABLISHMENT_PENDING"
                        ) ||
                            (
                                modelSelectionState ==
                                    "BOUND_HERETIC_PRODUCT_MODEL" &&
                                    runtimeState ==
                                    "BOUND_PRODUCT_MODEL"
                            ),
                    ) {
                        "model selection and runtime-binding projection disagree"
                    }
                }
            }
        }
    }

    @Serializable
    data class ConversationResource(
        val resourceRef: String,
        val displayName: String,
    ) {
        init {
            requirePortableRef(
                resourceRef,
                "resourceRef",
            )
            require(
                VexLifeLocalArtifact.admitDisplayName(
                    displayName,
                ) ==
                    displayName,
            ) {
                "Conversation resource display name is not admitted"
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
        val resources: List<ConversationResource> = emptyList(),
        val resourceHistoryState: ConversationResourceHistoryState =
            ConversationResourceHistoryState.AVAILABLE,
    ) {
        init {
            require(
                resources.size <=
                    VexLifeLocalArtifact.MaximumPerRequest &&
                    (
                        role ==
                            "human" ||
                            resources.isEmpty()
                    ) &&
                    (
                        resourceHistoryState ==
                            ConversationResourceHistoryState.AVAILABLE ||
                            resources.isEmpty()
                    ),
            ) {
                "Conversation message resource projection is invalid"
            }
        }
    }

    @Serializable
    enum class ConversationResourceHistoryState {
        AVAILABLE,
        UNAVAILABLE,
    }

    @Serializable
    enum class ConversationResourceRepresentation {
        PARSED_TEXT,
        STATIC_IMAGE,
        UNKNOWN,
    }

    @Serializable
    enum class ConversationResourceCustody {
        EXACT,
        MISSING,
        INVALID,
    }

    @Serializable
    data class ConversationResourceMetadata(
        val schemaVersion: String,
        val requestEventRef: String,
        val requestEventHash: String,
        val requestSequence: Long,
        val resourceRef: String,
        val displayName: String,
        val representation: ConversationResourceRepresentation,
        val originalCustody: ConversationResourceCustody,
        val projectionCustody: ConversationResourceCustody,
        val readableBytes: Long? = null,
        val lineCount: Int? = null,
        val mediaType: String? = null,
        val widthPixels: Int? = null,
        val heightPixels: Int? = null,
        val contentAvailable: Boolean,
        val status: VexLifeText? = null,
        val effectsPerformed: Boolean = false,
    ) {
        init {
            require(
                schemaVersion ==
                    ConversationResourceMetadataSchema &&
                    requestSequence >=
                    0 &&
                    !effectsPerformed,
            ) {
                "Conversation resource metadata identity is invalid"
            }
            requireSha256(
                requestEventHash,
                "requestEventHash",
            )
            require(
                requestEventRef.isNotBlank() &&
                    VexLifeLocalArtifact.requestResourceOrdinal(
                        requestEventHash = requestEventHash,
                        resourceRef = resourceRef,
                    ) !=
                    null &&
                    VexLifeLocalArtifact.admitDisplayName(
                        displayName,
                    ) ==
                    displayName,
            ) {
                "Conversation resource metadata request/resource binding is invalid"
            }

            val expectedStatusKey =
                when {
                    originalCustody ==
                        ConversationResourceCustody.INVALID ||
                        projectionCustody ==
                        ConversationResourceCustody.INVALID ->
                        ConversationResourceInvalidTextKey

                    !contentAvailable ->
                        ConversationResourceUnavailableTextKey

                    else ->
                        null
                }
            require(
                status?.key ==
                    expectedStatusKey
            ) {
                "Conversation resource status text does not match its custody observation"
            }

            when (
                representation
            ) {
                ConversationResourceRepresentation.PARSED_TEXT ->
                    require(
                        projectionCustody ==
                            ConversationResourceCustody.EXACT &&
                            readableBytes !=
                            null &&
                            readableBytes >
                            0 &&
                            lineCount !=
                            null &&
                            lineCount >
                            0 &&
                            mediaType ==
                            null &&
                            widthPixels ==
                            null &&
                            heightPixels ==
                            null &&
                            contentAvailable,
                    ) {
                        "Conversation text-resource metadata is incomplete"
                    }

                ConversationResourceRepresentation.STATIC_IMAGE ->
                    require(
                        projectionCustody ==
                            ConversationResourceCustody.EXACT &&
                            readableBytes ==
                            null &&
                            lineCount ==
                            null &&
                            !mediaType.isNullOrBlank() &&
                            widthPixels !=
                            null &&
                            widthPixels >
                            0 &&
                            heightPixels !=
                            null &&
                            heightPixels >
                            0 &&
                            contentAvailable ==
                            (
                                originalCustody ==
                                    ConversationResourceCustody.EXACT
                            ),
                    ) {
                        "Conversation image-resource metadata is incomplete"
                    }

                ConversationResourceRepresentation.UNKNOWN ->
                    require(
                        projectionCustody !=
                            ConversationResourceCustody.EXACT &&
                            readableBytes ==
                            null &&
                            lineCount ==
                            null &&
                            mediaType ==
                            null &&
                            widthPixels ==
                            null &&
                            heightPixels ==
                            null &&
                            !contentAvailable,
                    ) {
                        "unknown Conversation resource metadata must not invent review content"
                    }
            }
        }
    }

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
    data class ContextProfilesProjection(
        val schemaVersion: String,
        val active: VexLifeContextProfile.Effective,
        val saved: List<VexLifeContextProfile.Effective>,
        val display: VexLifeContextProfileFamily.DisplayState,
        val productFoundation: VexLifeContextProfile.ProductFoundationIdentity,
        val homeRelativePath: String,
        val effectsPerformed: Boolean,
    ) {
        init {
            require(schemaVersion == ContextProfilesProjectionSchema) {
                "Context Profile projection schema is not current"
            }
        }
    }

    @Serializable
    data class ContextProfileControlRequest(
        val controlRef: String,
        val profileRef: String? = null,
        val definition: VexLifeContextProfile.Definition? = null,
        val portable: VexLifeContextProfile.Portable? = null,
    )

    @Serializable
    data class ContextProfileControlResult(
        val schemaVersion: String,
        val controlRef: String,
        val outcome: String,
        val effectPerformed: Boolean,
        val profiles: ContextProfilesProjection,
    ) {
        init {
            require(schemaVersion == ContextProfileControlResultSchema) {
                "Context Profile control result schema is not current"
            }
        }
    }

    @Serializable
    data class ContextProfileHomeRevealResult(
        val schemaVersion: String,
        val outcome: String,
        val effectPerformed: Boolean,
        val homeRelativePath: String,
    ) {
        init {
            require(schemaVersion == ContextProfileHomeRevealResultSchema) {
                "Context Profile Home reveal result schema is not current"
            }
        }
    }

    @Serializable
    data class VexLabsField(
        val fieldRef: String,
        val group: String,
        val label: VexLifeText,
        val explanation: VexLifeText,
        val currentValue: String?,
        val inheritedDefaultValue: String?,
        val recommendedValue: String?,
        val minimumValue: String?,
        val maximumValue: String?,
        val unit: VexLifeText?,
        val provenance: String,
        val currentChangeBoundary: String,
        val ownerRef: String,
        val effectiveState: String,
        val userMutability: String,
        val userApplyBoundary: String?,
        val controlRef: String?,
        val increaseConsequence: VexLifeText?,
        val decreaseConsequence: VexLifeText?,
        val relatedFieldRefs: List<String>,
        val labEditability: String,
        val labEditDetail: VexLifeText,
        val labObservedMinimumValue: String? = null,
    ) {
        init {
            require(
                fieldRef.isNotBlank() &&
                    label.key.isNotBlank() &&
                    explanation.key.isNotBlank() &&
                    labEditDetail.key.isNotBlank() &&
                    ownerRef.isNotBlank() &&
                    labEditability in
                    setOf(
                        "EDITABLE_LAB_ONLY",
                        "EDITABLE_PRODUCT_CONTROL_RESTART",
                        "READ_ONLY",
                        "HELD",
                    ),
            ) {
                "Vex Labs field projection is invalid"
            }
        }
    }

    @Serializable
    data class VexLabsAdmission(
        val reason: String,
        val requestedInputTokenBudget: Int,
        val minimumInputTokenBudget: Int,
        val fixedContextTokens: Int,
        val immediatePriorTokens: Int,
        val capabilityExchangeAllowanceTokens: Int,
    ) {
        init {
            require(
                reason in
                    setOf(
                        "CURRENT_TURN_FIXED_CONTEXT",
                        "IMMEDIATE_PRIOR_REQUIRED",
                    ) &&
                    requestedInputTokenBudget > 0 &&
                    minimumInputTokenBudget > requestedInputTokenBudget &&
                    fixedContextTokens >= 0 &&
                    immediatePriorTokens >= 0 &&
                    capabilityExchangeAllowanceTokens >= 0,
            ) {
                "Vex Labs admission projection is invalid"
            }
        }
    }

    @Serializable
    data class VexLabsExchange(
        val human: String,
        val vex: String,
        val sealedHumanReasoning: String?,
        val promptInputTokenBudget: Int,
        val maximumMemoryTokens: Int,
        val memoryMode: String,
        val memoryIncluded: Boolean,
        val selectedConversationEventCount: Int,
        val providerRequestBodySha256: String,
    )

    @Serializable
    data class VexLabsDraftDefaults(
        val inputTokenBudget: Int,
        val maximumMemoryTokens: Int,
        val memoryMode: String,
        val conversationPreset: String,
    )

    @Serializable
    data class VexLabsProjection(
        val schemaVersion: String,
        val state: String,
        val modelRef: String,
        val modelProfileRef: String,
        val fieldCatalogSchemaVersion: String,
        val fields: List<VexLabsField>,
        val defaults: VexLabsDraftDefaults,
        val conversationPreset: String,
        val memoryMode: String,
        val seedConversationHeadSha256: String?,
        val currentProductConversationHeadSha256: String?,
        val exchanges: List<VexLabsExchange>,
        val scratchConversationMessageCount: Int,
        val lastAdmission: VexLabsAdmission? = null,
        val effectsPerformed: Boolean,
    ) {
        init {
            require(
                schemaVersion == VexLabsProjectionSchema &&
                    state in setOf("READY", "HELD") &&
                    fields.isNotEmpty() &&
                    conversationPreset in setOf("CURRENT_SNAPSHOT", "EMPTY") &&
                    memoryMode in setOf("CURRENT", "NONE") &&
                    !effectsPerformed,
            ) {
                "Vex Labs projection is invalid"
            }
        }
    }

    @Serializable
    data class VexLabsControlRequest(
        val controlRef: String,
    )

    @Serializable
    data class VexLabsControlResult(
        val schemaVersion: String,
        val controlRef: String,
        val outcome: String,
        val projection: VexLabsProjection,
        val labEffectPerformed: Boolean,
    ) {
        init {
            require(schemaVersion == VexLabsControlResultSchema) {
                "Vex Labs control result schema is not current"
            }
        }
    }

    @Serializable
    data class VexLabsSendRequest(
        val content: String,
        val inputTokenBudget: Int,
        val maximumMemoryTokens: Int,
        val memoryMode: String,
    ) {
        init {
            require(
                content.isNotBlank() &&
                    inputTokenBudget > 0 &&
                    maximumMemoryTokens >= 0 &&
                    memoryMode in setOf("CURRENT", "NONE"),
            ) {
                "Vex Labs native-test Send request is invalid"
            }
        }
    }

    @Serializable
    data class VexLabsSendResult(
        val schemaVersion: String,
        val state: String,
        val reply: String?,
        val sealedHumanReasoning: String?,
        val providerRequestBody: String?,
        val providerRequestBodySha256: String?,
        val promptInputTokenBudget: Int,
        val maximumMemoryTokens: Int,
        val memoryMode: String,
        val promptReservedOutputTokens: Int?,
        val promptHardTokenLimit: Int?,
        val memoryIncluded: Boolean,
        val memoryContextRef: String?,
        val selectedConversationEventCount: Int,
        val admission: VexLabsAdmission?,
        val projection: VexLabsProjection,
        val realConversationMutationPerformed: Boolean,
        val realMemoryMutationPerformed: Boolean,
        val labConversationMutationPerformed: Boolean,
    ) {
        init {
            require(
                schemaVersion == VexLabsSendResultSchema &&
                    state in
                    setOf(
                        "LAB_TURN_COMPLETED",
                        "HELD_CONTEXT_BUDGET",
                    ) &&
                    promptInputTokenBudget > 0 &&
                    maximumMemoryTokens >= 0 &&
                    memoryMode in setOf("CURRENT", "NONE") &&
                    !realConversationMutationPerformed &&
                    !realMemoryMutationPerformed &&
                    labConversationMutationPerformed,
            ) {
                "Vex Labs native-test Send result is invalid"
            }
            if (
                state ==
                    "LAB_TURN_COMPLETED"
            ) {
                require(
                    !reply.isNullOrBlank() &&
                        providerRequestBody != null &&
                        providerRequestBodySha256?.matches(
                            Regex("[0-9a-f]{64}"),
                        ) ==
                        true &&
                        promptReservedOutputTokens !=
                        null &&
                        promptReservedOutputTokens >
                        0 &&
                        promptHardTokenLimit !=
                        null &&
                        promptHardTokenLimit >
                        promptReservedOutputTokens &&
                        admission ==
                        null,
                ) {
                    "completed Vex Labs turn result is incomplete"
                }
            } else {
                require(
                    reply ==
                        null &&
                        providerRequestBody ==
                        null &&
                        providerRequestBodySha256 ==
                        null &&
                        promptReservedOutputTokens ==
                        null &&
                        promptHardTokenLimit ==
                        null &&
                        !memoryIncluded &&
                        memoryContextRef ==
                        null &&
                        selectedConversationEventCount ==
                        0 &&
                        admission !=
                        null,
                ) {
                    "held Vex Labs context-budget result is inconsistent"
                }
            }
        }
    }

    @Serializable
    data class SettingsLanguage(val code: String, val name: String)

    /**
     * The person's two language choices and the options for each. [screenLanguage] is the explicit
     * choice (null: the same as the reply language); [screenLanguageShown] is what the screens are
     * written in now; [screenLanguages] are the screen languages every scope is complete in.
     */
    @Serializable
    data class SettingsProjection(
        val schemaVersion: String,
        val replyLanguage: String,
        val languages: List<SettingsLanguage>,
        val screenLanguage: String?,
        val screenLanguageShown: String,
        val screenLanguages: List<SettingsLanguage>,
    )

    /**
     * One change to one language choice: exactly one of the two is named, and the other is left as
     * it is. [screenLanguage] may be [vexlife.person.VexLifePersonSettings.FollowReplyLanguage].
     */
    @Serializable
    data class SettingsUpdate(val replyLanguage: String? = null, val screenLanguage: String? = null) {
        init {
            require((replyLanguage == null) != (screenLanguage == null)) {
                "a settings update changes exactly one language choice"
            }
            replyLanguage?.let(vexlife.person.VexLifePersonSettings::replyLanguageName)
            screenLanguage
                ?.takeIf { it != vexlife.person.VexLifePersonSettings.FollowReplyLanguage }
                ?.let(vexlife.person.VexLifePersonSettings::screenLanguageName)
        }
    }

    @Serializable
    data class LocalAsrModelControl(
        val controlRef: String,
        val labelKey: String,
    )

    @Serializable
    data class LocalAsrOption(
        val providerRef: String,
        val modelRef: String,
        val providerLabel: String,
        val modelLabel: String,
        val languageCodes: List<String>,
        val modelBytes: Long,
        val standing: String,
        val selected: Boolean,
        val artifactState: String,
        val presentBytes: Long,
        val downloadedBytes: Long?,
        val controls: List<LocalAsrModelControl>,
        val currentState: String,
        val currentDetailRef: String,
    )

    @Serializable
    data class LocalAsrSpokenLanguageChoice(
        val mode: String,
        val languageCode: String?,
        val labelKey: String?,
        val languageLabel: String?,
        val selected: Boolean,
        val supportedBySelectedModel: Boolean,
    )

    @Serializable
    data class LocalAsrComposerOutputChoice(
        val mode: String,
        val labelKey: String,
        val selected: Boolean,
        val supportedBySelectedModel: Boolean,
    )

    @Serializable
    data class LocalAsrProjection(
        val schemaVersion: String,
        val selectedProviderRef: String,
        val selectedModelRef: String,
        val spokenLanguageMode: String,
        val spokenLanguageCode: String?,
        val resolvedSpokenLanguageCode: String,
        val spokenLanguageChoices: List<LocalAsrSpokenLanguageChoice>,
        val composerOutputMode: String,
        val composerOutputChoices: List<LocalAsrComposerOutputChoice>,
        val freeStorageBytes: Long?,
        val options: List<LocalAsrOption>,
    ) {
        init {
            require(
                schemaVersion ==
                    LocalAsrProjectionSchema &&
                    options.isNotEmpty() &&
                    options.count { it.selected } == 1 &&
                    options.any {
                        it.providerRef == selectedProviderRef &&
                            it.modelRef == selectedModelRef &&
                            it.selected
                    } &&
                    spokenLanguageChoices.isNotEmpty() &&
                    spokenLanguageChoices.count { it.selected } == 1 &&
                    composerOutputChoices.isNotEmpty() &&
                    composerOutputChoices.count { it.selected } == 1 &&
                    resolvedSpokenLanguageCode.isNotBlank() &&
                    composerOutputMode.isNotBlank(),
            ) {
                "local ASR projection is invalid"
            }
        }
    }

    @Serializable
    data class LocalAsrSelectionUpdate(
        val providerRef: String,
        val modelRef: String,
    )

    @Serializable
    data class LocalAsrSpokenLanguageUpdate(
        val mode: String,
        val languageCode: String? = null,
    )

    @Serializable
    data class LocalAsrComposerOutputUpdate(
        val mode: String,
    )

    @Serializable
    data class LocalAsrModelControlRequest(
        val controlRef: String,
        val modelRef: String,
    )

    @Serializable
    data class LocalAsrTranscriptionResponse(
        val schemaVersion: String,
        val providerRef: String,
        val modelRef: String,
        val transcript: String,
        val composerOutputMode: String,
        val inferenceMillis: Long,
        val effectsPerformed: Boolean,
    ) {
        init {
            require(
                schemaVersion ==
                    LocalAsrTranscriptionResponseSchema &&
                    providerRef.isNotBlank() &&
                    modelRef.isNotBlank() &&
                    transcript.isNotBlank() &&
                    composerOutputMode.isNotBlank() &&
                    inferenceMillis >=
                    0 &&
                    effectsPerformed,
            ) {
                "local ASR transcription response is invalid"
            }
        }
    }

    /** One private exchange, held only in the running service. */
    @Serializable
    data class PrivateExchange(
        val human: String,
        val vex: String,
        val sealedHumanReasoning: String?,
        val at: String,
    )

    /** What survives a private conversation: when it was, how long, and the agreed notes. */
    @Serializable
    data class PrivateInterval(
        val intervalRef: String,
        val startedAt: String,
        val endedAt: String,
        val exchangeCount: Int,
        val keptNotes: List<String>,
    )

    /** See [VexLifePrivateModeFamily]. Exchanges and pending notes exist only while private. */
    @Serializable
    data class PrivateProjection(
        val schemaVersion: String,
        val state: String,
        val display: VexLifePrivateModeFamily.DisplayState,
        val startedAtOrNull: String?,
        val exchanges: List<PrivateExchange>,
        val keptNotes: List<String>,
        val proposedNoteOrNull: String?,
        val intervals: List<PrivateInterval>,
        val effectsPerformed: Boolean,
    ) {
        init {
            require(
                schemaVersion == PrivateProjectionSchema &&
                    state in setOf(VexLifePrivateModeFamily.StateKept, VexLifePrivateModeFamily.StatePrivate) &&
                    (state == VexLifePrivateModeFamily.StatePrivate ||
                        (startedAtOrNull == null && exchanges.isEmpty() && keptNotes.isEmpty() && proposedNoteOrNull == null)) &&
                    !effectsPerformed
            ) {
                "Private projection is invalid"
            }
        }
    }

    @Serializable
    data class PrivateControlRequest(val controlRef: String, val requestRef: String) {
        init {
            require(VexLifePrivateModeFamily.controlForRef(controlRef) != null) { "unknown Private mode Control" }
            requirePortableRef(requestRef, "requestRef")
        }
    }

    @Serializable
    data class PrivateControlResult(
        val schemaVersion: String,
        val requestRef: String,
        val controlRef: String,
        val outcome: String,
        val private: PrivateProjection,
    ) {
        init {
            require(schemaVersion == PrivateControlResultSchema && outcome in setOf(PrivateRealized, PrivateNotLawfulNow)) {
                "Private control result is invalid"
            }
        }
    }

    const val PrivateRealized = "REALIZED"
    const val PrivateNotLawfulNow = "NOT_LAWFUL_NOW"

    @Serializable
    data class PrivateSendRequest(val content: String) {
        init {
            require(content.isNotBlank() && content.length <= PrivateTextLimit) { "private message must be 1..$PrivateTextLimit characters" }
        }
    }

    /** Keeping requires the exact words Vex proposed: the agreement is on those words. */
    @Serializable
    data class PrivateNoteKeepRequest(val text: String) {
        init {
            require(text.isNotBlank() && text.length <= PrivateTextLimit) { "kept note must be 1..$PrivateTextLimit characters" }
        }
    }

    /** What a person is told about Vex's long-term Memory; see [VexLifeMemoryFamily]. */
    @Serializable
    data class MemoryProjection(
        val schemaVersion: String,
        val state: String,
        val display: VexLifeMemoryFamily.DisplayState,
        /** Current Memory packet identity, so Vex Context can compare "now" with the last turn. */
        val memoryContextRef: String? = null,
        val memoryContextFingerprint: String? = null,
        val currentDailyStratumRef: String? = null,
        val rememberedCount: Int = 0,
        val effectsPerformed: Boolean,
    ) {
        init {
            require(
                schemaVersion == MemoryProjectionSchema &&
                    state in VexLifeMemoryFamily.StateRefs &&
                    !effectsPerformed
            ) {
                "Memory projection is invalid"
            }
        }
    }

    /** Release communication projection; publication and acknowledgement owners remain upstream. */
    @Serializable
    data class ReleaseAlertsProjection(
        val schemaVersion: String,
        val display: VexLifeReleaseInboxFamily.DisplayState,
        val effectsPerformed: Boolean,
    ) {
        init {
            require(
                schemaVersion == ReleaseAlertsProjectionSchema &&
                    display.unreadCount >= 0 &&
                    !effectsPerformed
            ) {
                "release-alerts projection is invalid"
            }
        }
    }

    @Serializable
    data class ReleaseAlertsControlRequest(
        val controlRef: String,
        val release: VexLifeReleaseIdentity.Coordinate,
    ) {
        init {
            require(
                controlRef in
                    setOf(
                        VexLifeReleaseInboxFamily.ControlView,
                        VexLifeReleaseInboxFamily.ControlDismiss,
                    ),
            ) {
                "unknown release-alert Control"
            }
        }
    }

    @Serializable
    data class ReleaseAlertsControlResult(
        val schemaVersion: String,
        val controlRef: String,
        val outcome: String,
        val releaseAlerts: ReleaseAlertsProjection,
        val pageToOpenOrNull: String?,
    ) {
        init {
            require(
                schemaVersion == ReleaseAlertsControlResultSchema &&
                    controlRef in
                    setOf(
                        VexLifeReleaseInboxFamily.ControlView,
                        VexLifeReleaseInboxFamily.ControlDismiss,
                    ) &&
                    outcome in
                    setOf(
                        ReleaseAlertsControlRealized,
                        ReleaseAlertsControlNotLawfulNow,
                    ) &&
                    (
                        pageToOpenOrNull ==
                            null ||
                            pageToOpenOrNull.startsWith(
                                "https://",
                            )
                    ) &&
                    (
                        outcome !=
                            ReleaseAlertsControlNotLawfulNow ||
                            pageToOpenOrNull ==
                            null
                    ) &&
                    (
                        controlRef !=
                            VexLifeReleaseInboxFamily.ControlDismiss ||
                            pageToOpenOrNull ==
                            null
                    ),
            ) {
                "release-alert Control result is invalid"
            }
        }
    }

    const val ReleaseAlertsControlRealized =
        "REALIZED"

    const val ReleaseAlertsControlNotLawfulNow =
        "NOT_LAWFUL_NOW"

    @Serializable
    data class VisionProjection(
        val schemaVersion: String,
        val state: String,
        val routeReachable: Boolean,
        val display: VexLifeVisionFamily.DisplayState,
        val effectsPerformed: Boolean,
    ) {
        init {
            require(
                schemaVersion == VisionProjectionSchema &&
                    state in VisionStateRefs &&
                    (routeReachable || state == VexLifeVisionFamily.StateHeld) &&
                    !effectsPerformed
            ) {
                "Vision projection is invalid"
            }
        }
    }

    @Serializable
    data class VisionControlRequest(val controlRef: String, val requestRef: String) {
        init {
            require(VexLifeVisionFamily.controlForRef(controlRef) != null) { "unknown Vision Control" }
            requirePortableRef(requestRef, "requestRef")
        }
    }

    /**
     * One Vision activation: fresh observation, Family resolution, at most one runtime command,
     * fresh observation. [vision] is always the State observed afterwards, never a predicted one.
     */
    @Serializable
    data class VisionControlResult(
        val schemaVersion: String,
        val requestRef: String,
        val controlRef: String,
        val outcome: String,
        val runtimeErrorCodeOrNull: String?,
        val vision: VisionProjection,
    ) {
        init {
            require(
                schemaVersion == VisionControlResultSchema &&
                    outcome in setOf(VisionControlRealized, VisionControlNotLawfulNow, VisionControlRuntimeRefused) &&
                    (outcome == VisionControlRuntimeRefused) == (runtimeErrorCodeOrNull != null)
            ) {
                "Vision control result is invalid"
            }
            requirePortableRef(requestRef, "requestRef")
        }
    }

    const val VisionControlRealized = "REALIZED"
    const val VisionControlNotLawfulNow = "NOT_LAWFUL_NOW"
    const val VisionControlRuntimeRefused = "RUNTIME_REFUSED"

    val VisionStateRefs =
        setOf(
            VexLifeVisionFamily.StateHeld,
            VexLifeVisionFamily.StateReady,
            VexLifeVisionFamily.StateSelecting,
            VexLifeVisionFamily.StateEngaged,
            VexLifeVisionFamily.StateLooking,
        )

    @Serializable
    data class VisionScopeCommandRequest(
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
    data class VisionSelectedScopeProjection(
        val schemaVersion: String,
        val scopeSessionRef: String,
        val state: String,
        val selectedScopePresent: Boolean,
        val scopeIndicatorRequired: Boolean,
        val lookingIndicatorRequired: Boolean,
        val lookRequestRefOrNull: String?,
        val safetyAuthorizationRefOrNull: String?,
        val safetyBindingRefOrNull: String?,
        val safetyReceiptFingerprintOrNull: String?,
        val safetyObservedAtOrNull: String?,
        val holdReasonOrNull: String?,
        val endReasonOrNull: String?,
        val sensorActive: Boolean,
        val screenCaptureActive: Boolean,
        val screenCaptureAuthorized: Boolean,
        val captureExecutionAuthorized: Boolean,
        val requestReplayProtectionEstablished: Boolean,
        val captureExecutionRequiresSingleUseEffectReservation: Boolean,
        val activeObservation: Boolean,
        val rawBytesIncluded: Boolean,
        val rawRetentionAllowed: Boolean,
        val continuousCaptureAllowed: Boolean,
        val backgroundCaptureAllowed: Boolean,
        val visualModelInputAuthorized: Boolean,
        val memoryAccepted: Boolean,
        val trainingEligible: Boolean,
        val publicationEligible: Boolean,
    ) {
        init {
            require(
                schemaVersion ==
                    VisionSelectedScopeProjectionSchema &&
                    state in
                    setOf(
                        "HELD",
                        "SELECTING",
                        "ENGAGED",
                        "LOOKING",
                        "ENDED",
                    ) &&
                    selectedScopePresent ==
                    (
                        state ==
                            "ENGAGED" ||
                            state ==
                            "LOOKING"
                    ) &&
                    scopeIndicatorRequired ==
                    selectedScopePresent &&
                    lookingIndicatorRequired ==
                    (
                        state ==
                            "LOOKING"
                    ) &&
                    !sensorActive &&
                    !screenCaptureActive &&
                    !screenCaptureAuthorized &&
                    !captureExecutionAuthorized &&
                    !requestReplayProtectionEstablished &&
                    captureExecutionRequiresSingleUseEffectReservation &&
                    !activeObservation &&
                    !rawBytesIncluded &&
                    !rawRetentionAllowed &&
                    !continuousCaptureAllowed &&
                    !backgroundCaptureAllowed &&
                    !visualModelInputAuthorized &&
                    !memoryAccepted &&
                    !trainingEligible &&
                    !publicationEligible,
            ) {
                "Vision selected-scope projection fixed boundary is invalid"
            }

            requirePortableRef(
                scopeSessionRef,
                "scopeSessionRef",
            )

            lookRequestRefOrNull?.let {
                requirePortableRef(
                    it,
                    "lookRequestRefOrNull",
                )
            }
            safetyAuthorizationRefOrNull?.let {
                requirePortableRef(
                    it,
                    "safetyAuthorizationRefOrNull",
                )
            }
            safetyBindingRefOrNull?.let {
                requirePortableRef(
                    it,
                    "safetyBindingRefOrNull",
                )
            }

            require(
                safetyReceiptFingerprintOrNull ==
                    null ||
                    safetyReceiptFingerprintOrNull.matches(
                        Regex(
                            "sha256:[0-9a-f]{64}",
                        ),
                    ),
            ) {
                "Vision selected-scope Safety fingerprint is invalid"
            }

            require(
                holdReasonOrNull in
                setOf(
                    null,
                    "SAFETY_NOT_CURRENT",
                ) &&
                    endReasonOrNull in
                    setOf(
                        null,
                        "HUMAN_DISENGAGED",
                        "SOURCE_LOST",
                        "HOST_OWNER_LOST",
                        "EXPLICIT_STOP",
                    ),
            ) {
                "Vision selected-scope terminal/hold reason is invalid"
            }

            require(
                (
                    state ==
                        "LOOKING" &&
                        lookRequestRefOrNull !=
                        null
                ) ||
                    (
                        state !=
                            "LOOKING" &&
                            lookRequestRefOrNull ==
                            null
                    ),
            ) {
                "Vision selected-scope LOOK identity disagrees with state"
            }
        }
    }

    @Serializable
    data class VisionSelectedScopeRuntimeResponse(
        val schemaVersion: String,
        val commandRef: String,
        val commandType: String,
        val ok: Boolean,
        val projectionOrNull: VisionSelectedScopeProjection?,
        val shutdownRequested: Boolean,
        val errorCodeOrNull: String?,
    ) {
        init {
            require(
                schemaVersion ==
                    VisionSelectedScopeRuntimeResponseSchema &&
                    commandType in
                    setOf(
                        "ENGAGE",
                        "PROJECT",
                        "RECONCILE",
                        "STOP",
                        "SHUTDOWN",
                    ),
            ) {
                "Vision selected-scope runtime response identity is invalid"
            }
            requirePortableRef(
                commandRef,
                "commandRef",
            )

            if (
                ok
            ) {
                require(
                    errorCodeOrNull ==
                        null,
                ) {
                    "successful Vision selected-scope runtime response contains an error"
                }
            } else {
                require(
                    projectionOrNull ==
                        null &&
                        errorCodeOrNull !=
                        null &&
                        errorCodeOrNull.matches(
                            Regex(
                                "[A-Z][A-Z0-9_]*",
                            ),
                        ),
                ) {
                    "failed Vision selected-scope runtime response is not bounded"
                }
            }

            if (
                commandType ==
                    "SHUTDOWN"
            ) {
                require(
                    shutdownRequested,
                ) {
                    "Vision selected-scope SHUTDOWN must expose terminal shutdown truth"
                }
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
    data class AttachmentDraftProjection(
        val schemaVersion: String,
        val state: String,
        val display: VexLifeAttachmentDraftFamily.DisplayState,
        val items: List<VexLifeAttachmentDraftFamily.Item>,
        val effectsPerformed: Boolean,
    ) {
        init {
            require(
                schemaVersion ==
                    AttachmentDraftProjectionSchema &&
                    state in
                    setOf(
                        VexLifeAttachmentDraftFamily.StateEmpty,
                        VexLifeAttachmentDraftFamily.StateDraft,
                    ) &&
                    !effectsPerformed,
            ) {
                "attachment draft projection is invalid"
            }
        }
    }

    enum class AttachmentStageOutcome {
        READY,
        HELD,
        CANCELLED,
    }

    @Serializable
    data class AttachmentStageResponse(
        val schemaVersion: String,
        val draftRef: String,
        val outcome: AttachmentStageOutcome,
        val message: VexLifeText? = null,
        val draft: AttachmentDraftProjection,
    ) {
        init {
            require(
                schemaVersion ==
                    AttachmentStageResponseSchema,
            ) {
                "attachment stage response schema is not current"
            }
            requirePortableRef(
                draftRef,
                "draftRef",
            )
        }
    }

    @Serializable
    data class AttachmentControlRequest(
        val controlRef: String,
        val draftRef: String?,
    ) {
        init {
            require(
                VexLifeAttachmentDraftFamily.controlForRef(
                    controlRef = controlRef,
                    draftRef = draftRef,
                ) !=
                    null,
            ) {
                "attachment draft Control is unknown"
            }
        }
    }

    enum class AttachmentControlOutcome {
        REALIZED,
        NOT_LAWFUL_NOW,
    }

    @Serializable
    data class AttachmentControlResponse(
        val schemaVersion: String,
        val controlRef: String,
        val draftRef: String?,
        val outcome: AttachmentControlOutcome,
        val draft: AttachmentDraftProjection,
        val effectsPerformed: Boolean,
    ) {
        init {
            require(
                schemaVersion ==
                    AttachmentControlResponseSchema &&
                    effectsPerformed ==
                    (
                        outcome ==
                            AttachmentControlOutcome.REALIZED
                    ),
            ) {
                "attachment draft Control response is invalid"
            }
        }
    }

    @Serializable
    data class LocalSourcesProjection(
        val schemaVersion: String,
        val state: String,
        val display: VexLifeLocalSourcesFamily.DisplayState,
        val grants: List<VexLifeLocalSourcesFamily.Grant>,
        val effectsPerformed: Boolean,
    ) {
        init {
            require(
                schemaVersion ==
                    LocalSourcesProjectionSchema &&
                    state in
                    setOf(
                        VexLifeLocalSourcesFamily.StateEmpty,
                        VexLifeLocalSourcesFamily.StateGranted,
                    ) &&
                    !effectsPerformed,
            ) {
                "Local Sources projection is invalid"
            }
        }
    }

    @Serializable
    data class LocalSourcesControlRequest(
        val controlRef: String,
        val sourceRef: String? = null,
    ) {
        init {
            require(
                VexLifeLocalSourcesFamily.controlForRef(
                    controlRef = controlRef,
                    sourceRef = sourceRef,
                ) !=
                    null,
            ) {
                "Local Sources Control is unknown"
            }
        }
    }

    enum class LocalSourcesControlOutcome {
        REALIZED,
        CANCELLED,
        UNAVAILABLE,
        REFUSED,
        NOT_LAWFUL_NOW,
    }

    @Serializable
    data class LocalSourcesControlResponse(
        val schemaVersion: String,
        val controlRef: String,
        val sourceRef: String?,
        val outcome: LocalSourcesControlOutcome,
        val reasonKey: String? = null,
        val message: String? = null,
        val sources: LocalSourcesProjection,
        val grantMutationPerformed: Boolean,
    ) {
        init {
            require(
                schemaVersion ==
                    LocalSourcesControlResponseSchema &&
                    grantMutationPerformed ==
                    (
                        outcome ==
                            LocalSourcesControlOutcome.REALIZED
                    ),
            ) {
                "Local Sources Control response is invalid"
            }
        }
    }

    @Serializable
    data class WorkspaceEffectsProjection(
        val schemaVersion: String,
        val state: String,
        val grants: List<VexLifeWorkspaceEffectsFamily.Grant>,
        val effectsPerformed: Boolean,
    ) {
        init {
            require(
                schemaVersion ==
                    WorkspaceEffectsProjectionSchema &&
                    state in
                    setOf(
                        VexLifeWorkspaceEffectsFamily.StateEmpty,
                        VexLifeWorkspaceEffectsFamily.StateGranted,
                    ) &&
                    !effectsPerformed,
            ) {
                "Workspace Effects projection is invalid"
            }
        }
    }

    @Serializable
    data class WorkspaceEffectsControlRequest(
        val controlRef: String,
        val sourceRef: String,
    ) {
        init {
            require(
                controlRef in
                    VexLifeWorkspaceEffectsFamily.ControlRefs &&
                    VexLifeLocalSourcesFamily.isSourceRef(
                        sourceRef,
                    ),
            ) {
                "Workspace Effects Control is unknown"
            }
        }
    }

    enum class WorkspaceEffectsControlOutcome {
        REALIZED,
        REFUSED,
        NOT_LAWFUL_NOW,
    }

    @Serializable
    data class WorkspaceEffectsControlResponse(
        val schemaVersion: String,
        val controlRef: String,
        val sourceRef: String,
        val outcome: WorkspaceEffectsControlOutcome,
        val reasonKey: String? = null,
        val effects: WorkspaceEffectsProjection,
        val grantMutationPerformed: Boolean,
    ) {
        init {
            require(
                schemaVersion ==
                    WorkspaceEffectsControlResponseSchema &&
                    controlRef in
                    VexLifeWorkspaceEffectsFamily.ControlRefs &&
                    VexLifeLocalSourcesFamily.isSourceRef(
                        sourceRef,
                    ) &&
                    grantMutationPerformed ==
                    (
                        outcome ==
                            WorkspaceEffectsControlOutcome.REALIZED
                    ),
            ) {
                "Workspace Effects Control response is invalid"
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
    data class ProviderMeasurementProjection(
        val stop: String,
        val finishReason: String?,
        val promptTokens: Int?,
        val cachedPromptTokens: Int?,
        val prefillTokens: Int?,
        val completionTokens: Int?,
        val firstTokenMillis: Long?,
        val firstAnswerMillis: Long?,
        val totalMillis: Long,
    ) {
        init {
            require(
                stop.isNotBlank() &&
                    listOfNotNull(
                        promptTokens,
                        cachedPromptTokens,
                        prefillTokens,
                        completionTokens,
                    )
                        .all {
                            it >= 0
                        } &&
                    listOfNotNull(
                        firstTokenMillis,
                        firstAnswerMillis,
                        totalMillis,
                    )
                        .all {
                            it >= 0L
                        } &&
                    prefillTokens ==
                    promptTokens
                        ?.let {
                            prompt ->
                            prompt - (cachedPromptTokens ?: 0)
                        } &&
                    (
                        promptTokens == null ||
                            cachedPromptTokens == null ||
                            cachedPromptTokens <= promptTokens
                        )
            ) {
                "Conversation provider measurement projection is invalid"
            }
        }
    }

    @Serializable
    data class FollowUpReadinessProjection(
        val state: String,
        val lastCompletedProviderMeasurement: ProviderMeasurementProjection? = null,
    ) {
        init {
            require(
                state in FollowUpReadinessStates,
            ) {
                "Conversation follow-up readiness projection is invalid"
            }
        }
    }

    @Serializable
    data class ConversationTurnProjection(
        val schemaVersion: String,
        val state: String,
        val display: VexLifeConversationTurnFamily.DisplayState,
        val effectsPerformed: Boolean,
        /**
         * The existing Prompt Preparation owner says the next canonical Send still belongs to this
         * service's one initial local-start window. Presentation may explain it; it may not infer or
         * control provider/cache state from this flag.
         */
        val initialPromptPreparationPendingForNextTurn: Boolean = false,
        /**
         * The currently active canonical turn began inside that same initial local-start window.
         * This keeps the explanation visible while the first reply is being formed, even after the
         * reusable-prefix state itself has been consumed.
         */
        val initialPromptPreparationForActiveTurn: Boolean = false,
        /**
         * Follow-up mode is Product/runtime truth. The nested measurement is observed completed-turn
         * evidence only; neither field certifies invisible provider cache residency.
         */
        val followUpReadiness: FollowUpReadinessProjection? = null,
    ) {
        init {
            require(
                schemaVersion == ConversationTurnProjectionSchema &&
                    state in VexLifeConversationTurnFamily.StateRefs &&
                    !effectsPerformed &&
                    (
                        !initialPromptPreparationForActiveTurn ||
                            state in
                            setOf(
                                VexLifeConversationTurnFamily.StateActive,
                                VexLifeConversationTurnFamily.StateCancelling,
                            )
                    )
            ) {
                "Conversation turn projection is invalid"
            }
        }
    }

    @Serializable
    data class ConversationCancelRequest(
        val attemptRef: String,
    ) {
        init {
            requirePortableRef(attemptRef, "attemptRef")
        }
    }

    @Serializable
    data class ConversationCancelResponse(
        val schemaVersion: String,
        val controlRef: String,
        val attemptRef: String,
        val outcome: ConversationControlOutcome,
        val turn: ConversationTurnProjection,
        val effectsPerformed: Boolean,
    ) {
        init {
            require(
                schemaVersion == ConversationCancelResponseSchema &&
                    controlRef == VexLifeConversationTurnFamily.ControlStop &&
                    effectsPerformed == (outcome == ConversationControlOutcome.REALIZED)
            ) {
                "Conversation cancel response is invalid"
            }
            requirePortableRef(attemptRef, "attemptRef")
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
    data class FoundationalFieldModuleProjection(
        val moduleRef: String,
        val origins: List<String>,
        val standing: String,
        val depthInSource: Boolean,
    )

    /**
     * What one prompt was given from the foundational field: which revision, which parts, how many
     * tokens, and what was left out and why. Evidence of supply, not of understanding.
     */
    @Serializable
    data class FoundationalFieldProjection(
        val state: String,
        val heldReason: String? = null,
        val fieldRef: String? = null,
        val fieldVersion: String? = null,
        val bodySha256: String? = null,
        val systemMessageSha256: String? = null,
        val estimatedSystemMessageTokens: Int,
        val tokenBudget: Int,
        val carriedModules: List<FoundationalFieldModuleProjection>,
        val omittedModuleRefs: List<String>,
        val omissionReasons: List<String>,
        val depthNotCarriedModuleRefs: List<String>,
        val sourceRefs: List<String>,
        val unresolvedRefs: List<String>,
        val effectAuthorityGranted: Boolean,
    )

    @Serializable
    data class FoundationalFieldRevisionProjection(
        val version: String,
        val bodySha256: String,
        val recordedOn: String,
        val recordedBy: String,
        val summary: String,
        val why: String,
    )

    /**
     * One part of the body as its owner holds it. [kernel] is what a prompt carries; [depth] is the
     * rest, which no prompt carries and which a person can read here by [moduleRef].
     */
    @Serializable
    data class FoundationalFieldPartProjection(
        val moduleRef: String,
        val title: String,
        val question: String,
        val origins: List<String>,
        val standing: String,
        val provenance: String,
        val sourceRefs: List<String>,
        val kernel: List<String>,
        val depth: List<String>,
    )

    @Serializable
    data class FoundationalFieldSourceProjection(
        val sourceRef: String,
        val repository: String?,
        val commit: String?,
        val path: String?,
        val blob: String?,
        val coverage: String,
    )

    @Serializable
    data class FoundationalFieldUnresolvedProjection(
        val unresolvedRef: String,
        val moduleRefs: List<String>,
        val summary: String,
        val note: String,
    )

    /**
     * A fresh no-effect read through the same owner the prompt uses. The lists are empty when the
     * body is unavailable; nothing is filled in from anywhere else.
     */
    @Serializable
    data class FoundationalFieldCurrentProjection(
        val schemaVersion: String,
        val foundationalField: FoundationalFieldProjection,
        val kernelText: String?,
        val unavailableDetail: String?,
        val purpose: String?,
        val parts: List<FoundationalFieldPartProjection>,
        val sources: List<FoundationalFieldSourceProjection>,
        val unresolved: List<FoundationalFieldUnresolvedProjection>,
        val revisions: List<FoundationalFieldRevisionProjection>,
        val effectsPerformed: Boolean,
    )

    @Serializable
    data class ContextComponentProjection(
        val componentRef: String,
        val stabilityClass: String,
        val placementClass: String,
        val logicalOrder: Int,
        val providerMessageIndex: Int,
        val sha256: String,
        val sourceRefs: List<String>,
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
        /** The reply language Vex was told, and how many private conversations it was told it doesn't remember. */
        val replyLanguage: String? = null,
        val replyLanguageName: String? = null,
        val privateConversationsNotRemembered: Int = 0,
        /** Absent when the turn's receipt predates the foundational field. */
        val foundationalField: FoundationalFieldProjection? = null,
        val contextProfileRef: String? = null,
        val contextProfileRevision: Int? = null,
        val contextProfileFingerprint: String? = null,
        val contextProfileFoundationMode: String? = null,
        val profileFoundationState: String? = null,
        val profileFoundationHeldReason: String? = null,
        val profileFoundationSystemMessageSha256: String? = null,
        val profileFoundationEstimatedSystemMessageTokens: Int = 0,
        val profileFoundationSourceCharacters: Int = 0,
        /**
         * Exact component order supplied to the provider. These are digests/refs and placement
         * evidence, not a second copy of the underlying Conversation, Memory or browser text.
         */
        val contextComponents: List<ContextComponentProjection> = emptyList(),
        /**
         * Completed exchanges the durable record held before this turn's request, as the prompt
         * stated it. Absent when the turn's receipt predates that statement.
         */
        val priorCompletedExchangeCount: Long? = null,
        val effectsPerformed: Boolean,
    )

    @Serializable
    data class ContextProjection(
        val schemaVersion: String,
        val state: String,
        val currentConversationHeadSha256: String?,
        val turnRef: String?,
        val contextPath: String?,
        val promptContext: PromptContextProjection?,
        /** Durable final provider-request hash; exact request text is never reconstructed here. */
        val finalProviderRequestBodySha256: String? = null,
        val heldReason: String? = null,
        val durablePromptReceiptSchema: String? = null,
        val effectsPerformed: Boolean,
    ) {
        init {
            require(
                schemaVersion ==
                    ContextProjectionSchema &&
                    state in
                    setOf(
                        ContextStateEmpty,
                        ContextStateAvailable,
                        ContextStateHistoricalWithoutProductEvidence,
                        ContextStateHeldUnsupportedDurablePromptReceipt,
                    ) &&
                    !effectsPerformed,
            ) {
                "Context projection identity/state is invalid"
            }

            when (
                state
            ) {
                ContextStateEmpty -> {
                    require(
                        currentConversationHeadSha256 ==
                            null &&
                            turnRef ==
                            null &&
                            contextPath ==
                            null &&
                            promptContext ==
                            null &&
                            heldReason ==
                            null &&
                            durablePromptReceiptSchema ==
                            null,
                    ) {
                        "empty Context projection must not claim turn evidence"
                    }
                }

                ContextStateAvailable -> {
                    require(
                        !currentConversationHeadSha256.isNullOrBlank() &&
                            !turnRef.isNullOrBlank() &&
                            !contextPath.isNullOrBlank() &&
                            promptContext !=
                            null &&
                            heldReason ==
                            null,
                    ) {
                        "available Context projection requires exact turn evidence"
                    }
                }

                ContextStateHistoricalWithoutProductEvidence -> {
                    require(
                        !currentConversationHeadSha256.isNullOrBlank() &&
                            !turnRef.isNullOrBlank() &&
                            !contextPath.isNullOrBlank() &&
                            promptContext ==
                            null &&
                            heldReason ==
                            null,
                    ) {
                        "historical Context projection must retain only canonical turn identity"
                    }
                }

                ContextStateHeldUnsupportedDurablePromptReceipt -> {
                    require(
                        !currentConversationHeadSha256.isNullOrBlank() &&
                            !turnRef.isNullOrBlank() &&
                            !contextPath.isNullOrBlank() &&
                            promptContext ==
                            null &&
                            heldReason ==
                            ContextHeldReasonPromptReceiptSchemaUnsupported &&
                            !durablePromptReceiptSchema.isNullOrBlank(),
                    ) {
                        "held Context projection must name the unsupported durable receipt schema"
                    }
                }
            }
        }
    }

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
    data class ProviderRequestProjection(
        val stageIndex: Int,
        val requestBodySha256: String,
        val requestBody: String,
        val persisted: Boolean = false,
    ) {
        init {
            require(
                stageIndex > 0 &&
                    requestBodySha256.matches(Regex("[0-9a-f]{64}")) &&
                    requestBody.isNotEmpty() &&
                    !persisted
            ) {
                "provider request projection must be one exact ephemeral request"
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
        /** Exact request bodies are local/browser-session evidence only and are never durable. */
        val providerRequests: List<ProviderRequestProjection> = emptyList(),
        /** What Vex reached during the turn; empty when he answered from context alone. */
        val capabilityUses: List<CapabilityUse> = emptyList(),
    )

    /** One capability request of a turn as the person may see it: the reach, never the content. */
    @Serializable
    data class CapabilityUse(
        val handle: String,
        val action: String,
        val target: String? = null,
        /** Present once the host returned (RETURNED, NOT_FOUND, REFUSED, FAILED, ...). */
        val state: String? = null,
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
        /** For REACH: the request Vex made (state null) or its returned state. */
        val reach: CapabilityUse? =
            null,
    ) {
        init {
            require(
                type in
                    setOf(
                        "PHASE",
                        "REASONING_DELTA",
                        "CONTENT_DELTA",
                        "REACH",
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
