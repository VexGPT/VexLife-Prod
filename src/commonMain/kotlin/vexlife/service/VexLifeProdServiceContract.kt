package vexlife.service

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import vexlife.browser.VexLifeProdBrowserContract
import vexlife.continuity.VexLifePrivateModeFamily
import vexlife.context.VexLifeContextProfile
import vexlife.context.VexLifeContextProfileFamily
import vexlife.home.VexLifeProdHome
import vexlife.memory.VexLifeMemoryFamily
import vexlife.release.VexLifeReleaseIdentity
import vexlife.release.VexLifeReleaseInboxFamily
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
        "vexlife-prod.local-service-conversation-projection/v1"

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
        "vexlife-prod.local-service-context-profiles-projection/v1"

    const val ContextProfileControlResultSchema =
        "vexlife-prod.local-service-context-profile-control-result/v1"

    const val ContextProfileHomeRevealResultSchema =
        "vexlife-prod.local-service-context-profile-home-reveal-result/v1"

    const val VexLabsProjectionSchema =
        "vexlife-prod.local-service-vex-labs-native-test-projection/v1"

    const val VexLabsControlResultSchema =
        "vexlife-prod.local-service-vex-labs-native-test-control-result/v1"

    const val VexLabsSendResultSchema =
        "vexlife-prod.local-service-vex-labs-native-test-send-result/v1"

    const val MemoryDreamProjectionSchema =
        "vexlife-prod.local-service-memory-dream-projection/v1"

    const val MemoryDreamRunResponseSchema =
        "vexlife-prod.local-service-memory-dream-run-response/v1"

    const val MemoryProjectionSchema = "vexlife-prod.local-service-memory-projection/v1"

    const val ReleaseAlertsProjectionSchema =
        "vexlife-prod.local-service-release-alerts-projection/v1"

    const val ReleaseAlertsControlResultSchema =
        "vexlife-prod.local-service-release-alerts-control-result/v1"

    const val PrivateProjectionSchema = "vexlife-prod.local-service-private-projection/v1"
    const val PrivateControlResultSchema = "vexlife-prod.local-service-private-control-result/v1"

    const val VisionProjectionSchema =
        "vexlife-prod.local-service-vision-projection/v2"

    const val VisionControlResultSchema =
        "vexlife-prod.local-service-vision-control-result/v1"

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
    const val ModelsControlPath = "/api/v1/models/control"
    const val ModelsImportCheckPath = "/api/v1/models/import/check"
    const val ModelsImportAddPath = "/api/v1/models/import/add"
    const val DiagnosticsControlPath = "/api/v1/diagnostics/control"
    const val DiagnosticsExportPath = "/api/v1/diagnostics/export"
    const val ChronologyPath = "/api/v1/chronology"
    /** The workspace scope's words in the person's language. Routes take no query parameters. */
    const val WorkspaceStringsPath = "/api/v1/strings/workspace"
    const val ReleaseAlertsStringsPath = "/api/v1/strings/release-alerts"
    const val SettingsSchema = "vexlife-prod.local-service-settings/v1"

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
        val label: String,
        val explanation: String,
        val currentValue: String?,
        val inheritedDefaultValue: String?,
        val recommendedValue: String?,
        val minimumValue: String?,
        val maximumValue: String?,
        val unit: String?,
        val provenance: String,
        val currentChangeBoundary: String,
        val ownerRef: String,
        val effectiveState: String,
        val userMutability: String,
        val userApplyBoundary: String?,
        val controlRef: String?,
        val increaseConsequence: String?,
        val decreaseConsequence: String?,
        val relatedFieldRefs: List<String>,
        val labEditability: String,
        val labEditDetail: String,
    ) {
        init {
            require(
                fieldRef.isNotBlank() &&
                    label.isNotBlank() &&
                    explanation.isNotBlank() &&
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
    data class VexLabsExchange(
        val human: String,
        val vex: String,
        val sealedHumanReasoning: String?,
        val promptInputTokenBudget: Int,
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
        val reply: String,
        val sealedHumanReasoning: String?,
        val providerRequestBody: String,
        val providerRequestBodySha256: String,
        val promptInputTokenBudget: Int,
        val promptReservedOutputTokens: Int,
        val promptHardTokenLimit: Int,
        val memoryIncluded: Boolean,
        val memoryContextRef: String?,
        val selectedConversationEventCount: Int,
        val projection: VexLabsProjection,
        val realConversationMutationPerformed: Boolean,
        val realMemoryMutationPerformed: Boolean,
        val labConversationMutationPerformed: Boolean,
    ) {
        init {
            require(
                schemaVersion == VexLabsSendResultSchema &&
                    state == "LAB_TURN_COMPLETED" &&
                    reply.isNotBlank() &&
                    providerRequestBodySha256.matches(Regex("[0-9a-f]{64}")) &&
                    promptInputTokenBudget > 0 &&
                    promptReservedOutputTokens > 0 &&
                    promptHardTokenLimit > promptReservedOutputTokens &&
                    !realConversationMutationPerformed &&
                    !realMemoryMutationPerformed &&
                    labConversationMutationPerformed,
            ) {
                "Vex Labs native-test Send result is invalid"
            }
        }
    }

    @Serializable
    data class SettingsLanguage(val code: String, val name: String)

    /** The person's Home settings Vex is told about, and the choices for each. */
    @Serializable
    data class SettingsProjection(
        val schemaVersion: String,
        val replyLanguage: String,
        val languages: List<SettingsLanguage>,
    )

    @Serializable
    data class SettingsUpdate(val replyLanguage: String) {
        init {
            vexlife.person.VexLifePersonSettings.replyLanguageName(replyLanguage)
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
