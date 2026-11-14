package vexlife.service

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import vexlife.browser.VexLifeProdBrowserContract
import vexlife.continuity.VexLifePrivateModeFamily
import vexlife.home.VexLifeProdHome
import vexlife.memory.VexLifeMemoryFamily
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
        "vexlife-prod.local-service-context-projection/v1"

    const val MemoryDreamProjectionSchema =
        "vexlife-prod.local-service-memory-dream-projection/v1"

    const val MemoryDreamRunResponseSchema =
        "vexlife-prod.local-service-memory-dream-run-response/v1"

    const val MemoryProjectionSchema = "vexlife-prod.local-service-memory-projection/v1"

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

    const val MemoryPath = "/api/v1/memory"

    /**
     * The foundational field as Vex would be given it now: the exact kernel words, every part with
     * the depth that stays in source, its sources, what is unresolved, and the revision record.
     */
    const val FoundationalFieldPath = "/api/v1/foundational-field"
    const val FoundationalFieldSchema = "vexlife-prod.local-service-foundational-field/v1"

    const val SettingsPath = "/api/v1/settings"
    const val DiagnosticsPath = "/api/v1/diagnostics"
    const val DiagnosticsControlPath = "/api/v1/diagnostics/control"
    const val DiagnosticsExportPath = "/api/v1/diagnostics/export"
    const val ChronologyPath = "/api/v1/chronology"
    /** The workspace scope's words in the person's language. Routes take no query parameters. */
    const val WorkspaceStringsPath = "/api/v1/strings/workspace"
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
                            modelMinimumMemoryBytes !=
                            null &&
                            modelMinimumMemoryBytes >=
                            0 &&
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
            vexlife.person.VexLifePersonSettings.languageName(replyLanguage)
        }
    }

    /** One private exchange, held only in the running service. */
    @Serializable
    data class PrivateExchange(val human: String, val vex: String, val at: String)

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
