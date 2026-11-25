package vexlife.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vexlife.browser.VexLifeProdBrowserContract
import vexlife.conversation.VexLifeConversationTurnFamily
import vexlife.sources.VexLifeLocalSourcesFamily
import vexlife.workspace.VexLifeWorkspaceEffectsFamily

class VexLifeProdServiceContractTest {
    @Test
    fun ordinaryServiceTopologyIsOneFixedSourceContract() {
        assertEquals(
            "127.0.0.1",
            VexLifeProdServiceContract.Host,
        )
        assertEquals(
            18_210,
            VexLifeProdServiceContract.Port,
        )
        assertEquals(
            "http://127.0.0.1:18210",
            VexLifeProdServiceContract.Origin,
        )
        assertEquals(
            18_211,
            VexLifeProdBrowserContract.FrameHostPort,
        )
        assertEquals(
            "http://127.0.0.1:18211",
            VexLifeProdBrowserContract.FrameHostOrigin,
        )
        assertEquals(
            "/",
            VexLifeProdServiceContract.PresentationIndexPath,
        )
        assertEquals(
            "/vexweb.js",
            VexLifeProdServiceContract.PresentationJavascriptPath,
        )
        assertEquals(
            "/api/v1/source-currentness",
            VexLifeProdServiceContract.SourceCurrentnessPath,
        )
        assertEquals(
            "/api/v1/current-state",
            VexLifeProdServiceContract.CurrentStatePath,
        )
        assertEquals(
            "/api/v1/conversation",
            VexLifeProdServiceContract.ConversationPath,
        )
        assertEquals(
            "/api/v1/conversation/turn",
            VexLifeProdServiceContract.ConversationTurnPath,
        )
        assertEquals(
            "/api/v1/conversation/cancel",
            VexLifeProdServiceContract.ConversationCancelPath,
        )
        assertEquals(
            "/api/v1/attachments/draft",
            VexLifeProdServiceContract.AttachmentDraftPath,
        )
        assertEquals(
            "/api/v1/attachments/draft/stage",
            VexLifeProdServiceContract.AttachmentStagePath,
        )
        assertEquals(
            "/api/v1/attachments/draft/control",
            VexLifeProdServiceContract.AttachmentControlPath,
        )
        assertEquals(
            "/api/v1/local-sources",
            VexLifeProdServiceContract.LocalSourcesPath,
        )
        assertEquals(
            "/api/v1/local-sources/control",
            VexLifeProdServiceContract.LocalSourcesControlPath,
        )
        assertEquals(
            "/api/v1/workspace-effects",
            VexLifeProdServiceContract.WorkspaceEffectsPath,
        )
        assertEquals(
            "/api/v1/workspace-effects/control",
            VexLifeProdServiceContract.WorkspaceEffectsControlPath,
        )
        assertEquals(
            "/api/v1/context",
            VexLifeProdServiceContract.ContextPath,
        )
        assertEquals(
            "/api/v1/local-asr",
            VexLifeProdServiceContract.LocalAsrPath,
        )
        assertEquals(
            "/api/v1/local-asr/spoken-language",
            VexLifeProdServiceContract.LocalAsrSpokenLanguagePath,
        )
        assertEquals(
            "/api/v1/local-asr/composer-output",
            VexLifeProdServiceContract.LocalAsrComposerOutputPath,
        )
        assertEquals(
            "/api/v1/local-asr/model/control",
            VexLifeProdServiceContract.LocalAsrModelControlPath,
        )
        assertEquals(
            "/api/v1/local-asr/prepare",
            VexLifeProdServiceContract.LocalAsrPreparePath,
        )
        assertEquals(
            "/api/v1/local-asr/transcribe",
            VexLifeProdServiceContract.LocalAsrTranscribePath,
        )
        assertEquals(
            "/api/v1/foundational-field",
            VexLifeProdServiceContract.FoundationalFieldPath,
        )
        assertEquals(
            "/api/v1/memory/dream",
            VexLifeProdServiceContract.MemoryDreamPath,
        )
        assertEquals(
            "/api/v1/conversation/send",
            VexLifeProdServiceContract.ConversationSendPath,
        )
        assertEquals(
            "/api/v1/models/sources",
            VexLifeProdServiceContract.ModelsSourcesPath,
        )
        assertEquals(
            "/api/v1/models/sources/control",
            VexLifeProdServiceContract.ModelsSourcesControlPath,
        )
        assertEquals(
            "/api/v1/partner-connections",
            VexLifeProdServiceContract.PartnerConnectionsPath,
        )
        assertEquals(
            "/api/v1/partner-connections/control",
            VexLifeProdServiceContract.PartnerConnectionsControlPath,
        )
        val held =
            VexLifeProdServiceContract.PartnerConnectionAvailabilityProjection(
                schemaVersion =
                    VexLifeProdServiceContract.PartnerConnectionAvailabilitySchema,
                state =
                    VexLifeProdServiceContract.PartnerConnectionAvailabilityHeldFunding,
                connectEnabled = false,
                liveProviderTestingFunded = false,
                effectsPerformed = false,
            )
        assertFalse(
            held.connectEnabled,
        )
        assertEquals(
            "x-vexlife-partner-control",
            VexLifeProdServiceContract.PartnerConnectionControlHeader,
        )
        assertEquals(
            "x-vexlife-partner-provider-ref",
            VexLifeProdServiceContract.PartnerConnectionProviderRefHeader,
        )
        assertEquals(
            "x-vexlife-partner-workspace-id",
            VexLifeProdServiceContract.PartnerConnectionWorkspaceIdHeader,
        )
        assertEquals(
            VexLifeProdServiceContract.PartnerConnectionControl.Connect,
            VexLifeProdServiceContract.PartnerConnectionControl.forWire(
                "CONNECT",
            ),
        )
        assertNull(
            VexLifeProdServiceContract.PartnerConnectionControl.forWire(
                "connect",
            ),
        )
    }

    @Test
    fun localAsrDictationPassVocabularyIsExact() {
        assertEquals(
            "x-vexlife-dictation-pass",
            VexLifeProdServiceContract.LocalAsrDictationPassHeader,
        )
        assertEquals(
            VexLifeProdServiceContract.LocalAsrDictationPass.Partial,
            VexLifeProdServiceContract.LocalAsrDictationPass.forWire(
                "partial",
            ),
        )
        assertEquals(
            VexLifeProdServiceContract.LocalAsrDictationPass.Final,
            VexLifeProdServiceContract.LocalAsrDictationPass.forWire(
                "final",
            ),
        )
        assertNull(
            VexLifeProdServiceContract.LocalAsrDictationPass.forWire(
                "stream",
            ),
        )
    }

    @Test
    fun currentStateProjectionRequiresBoundedReadOnlyTruth() {
        val projection =
            VexLifeProdServiceContract.CurrentStateProjection(
                schemaVersion =
                    VexLifeProdServiceContract.CurrentStateProjectionSchema,
                observedAt = "2026-11-12T18:00:00Z",
                homePath = "/Users/victor/.vexlife",
                homeDisposition = "EXISTING_CURRENT",
                homeRef = "vex-home.device.vexlife.test",
                deviceRef = "device.vexlife.test",
                companionLineageRef = "companion-lineage.vexlife.test",
                platform = "darwin",
                architecture = "arm64",
                chipModel = "Apple M4 Pro",
                hostEligibilityState = "ELIGIBLE",
                hostEligibilityReason = null,
                totalMemoryBytes = 36L * 1024L * 1024L * 1024L,
                modelMinimumMemoryBytes = 12L * 1024L * 1024L * 1024L,
                freeStorageBytes = 100L * 1024L * 1024L * 1024L,
                modelSelectionState = "BOUND_HERETIC_PRODUCT_MODEL",
                activeModelRef = "model.heretic.test",
                activeModelProfileRef = "model-profile.heretic.test",
                runtimeState = "BOUND_PRODUCT_MODEL",
                runtimeCurrentness = "CONFIGURATION_ONLY_NOT_REOBSERVED",
                runtimeReceiptPath = "runtime/heretic/receipt.json",
                modelArtifactState = "COMPLETE_BY_SIZE_NOT_HASHED",
                modelArtifactTotalBytes = 15_000L,
                modelArtifactPresentBySizeBytes = 15_000L,
                modelArtifactMissingBytes = 0,
                modelArtifactInvalidSizeBytes = 0,
                effectsPerformed = false,
            )

        assertEquals(
            "COMPLETE_BY_SIZE_NOT_HASHED",
            projection.modelArtifactState,
        )
        assertEquals(
            "CONFIGURATION_ONLY_NOT_REOBSERVED",
            projection.runtimeCurrentness,
        )
        assertEquals(
            100L * 1024L * 1024L * 1024L,
            projection.freeStorageBytes,
        )
        assertFalse(
            projection.effectsPerformed,
        )
    }

    @Test
    fun emptyContextProjectionIsExplicitlyReadOnly() {
        val projection =
            VexLifeProdServiceContract.ContextProjection(
                schemaVersion =
                    VexLifeProdServiceContract.ContextProjectionSchema,
                state =
                    VexLifeProdServiceContract.ContextStateEmpty,
                currentConversationHeadSha256 =
                    null,
                turnRef =
                    null,
                contextPath =
                    null,
                promptContext =
                    null,
                effectsPerformed =
                    false,
            )

        assertEquals(
            VexLifeProdServiceContract.ContextProjectionSchema,
            projection.schemaVersion,
        )
        assertNull(
            projection.promptContext,
        )
        assertFalse(
            projection.effectsPerformed,
        )
    }

    @Test
    fun sendRequestKeepsBrowserContextOptionalAndBounded() {
        val request =
            VexLifeProdServiceContract.SendRequest(
                idempotencyKey = "send.vexlife.browser.test.001",
                content = "Hello Vex",
            )

        assertEquals(
            "send.vexlife.browser.test.001",
            request.idempotencyKey,
        )
        assertEquals(
            "Hello Vex",
            request.content,
        )
        assertNull(
            request.browserContext,
        )

        val withContext =
            VexLifeProdServiceContract.SendRequest(
                idempotencyKey = "send.vexlife.browser.test.002",
                content = "What is on this page?",
                browserContext =
                    VexLifeProdServiceContract.BrowserContextInput(
                        pageUrl = VexLifeProdBrowserContract.InitialPageUrl,
                        title = "Vextreme Terrain",
                        text = "Current direct-contact terrain text",
                        capture = VexLifeProdBrowserContract.CaptureRendered,
                    ),
            )
        assertEquals(
            VexLifeProdBrowserContract.InitialPageUrl,
            withContext.browserContext
                ?.pageUrl,
        )

        assertFailsWith<IllegalArgumentException> {
            VexLifeProdServiceContract.SendRequest(
                idempotencyKey = "../escape",
                content = "Hello",
            )
        }
        assertFailsWith<IllegalArgumentException> {
            VexLifeProdServiceContract.BrowserContextInput(
                pageUrl = "http://127.0.0.1/private",
                title = null,
                text = "private",
                capture = VexLifeProdBrowserContract.CaptureRendered,
            )
        }
    }
    @Test
    fun conversationTurnWireProjectsFamilyMeaningWithoutOwningIt() {
        val attempt =
            VexLifeConversationTurnFamily.Attempt(
                "send.vexlife.contract.001",
            )
        val state =
            VexLifeConversationTurnFamily.Active(
                attempt,
            )
        val projection =
            VexLifeProdServiceContract.ConversationTurnProjection(
                schemaVersion =
                    VexLifeProdServiceContract.ConversationTurnProjectionSchema,
                state = state.stateRef,
                display =
                    VexLifeConversationTurnFamily.displayStateFor(
                        state,
                    ),
                effectsPerformed = false,
            )

        assertEquals(
            VexLifeConversationTurnFamily.StateActive,
            projection.state,
        )
        assertEquals(
            VexLifeConversationTurnFamily.ControlStop,
            projection.display.primaryControlRef,
        )
        assertEquals(
            attempt.ref,
            projection.display.targetAttemptRef,
        )

        val measurement =
            VexLifeProdServiceContract.ProviderMeasurementProjection(
                stop = "COMPLETED",
                finishReason = "stop",
                promptTokens = 12_000,
                cachedPromptTokens = 8_000,
                prefillTokens = 4_000,
                completionTokens = 120,
                firstTokenMillis = 3_500,
                firstAnswerMillis = 3_900,
                totalMillis = 4_200,
            )
        val followUp =
            VexLifeProdServiceContract.FollowUpReadinessProjection(
                state =
                    VexLifeProdServiceContract.FollowUpReadinessFasterEnabled,
                lastCompletedProviderMeasurement = measurement,
            )
        assertEquals(
            8_000,
            followUp.lastCompletedProviderMeasurement
                ?.cachedPromptTokens,
        )
        assertEquals(
            VexLifeProdServiceContract.FollowUpReadinessFasterEnabled,
            followUp.state,
        )

        val response =
            VexLifeProdServiceContract.ConversationCancelResponse(
                schemaVersion =
                    VexLifeProdServiceContract.ConversationCancelResponseSchema,
                controlRef =
                    VexLifeConversationTurnFamily.ControlStop,
                attemptRef = attempt.ref,
                outcome =
                    VexLifeProdServiceContract.ConversationControlOutcome.REALIZED,
                turn =
                    VexLifeProdServiceContract.ConversationTurnProjection(
                        schemaVersion =
                            VexLifeProdServiceContract.ConversationTurnProjectionSchema,
                        state =
                            VexLifeConversationTurnFamily.StateCancelling,
                        display =
                            VexLifeConversationTurnFamily.displayStateFor(
                                VexLifeConversationTurnFamily.Cancelling(
                                    attempt,
                                ),
                            ),
                        effectsPerformed = false,
                    ),
                effectsPerformed = true,
            )
        assertEquals(
            VexLifeConversationTurnFamily.StateCancelling,
            response.turn.state,
        )
        assertEquals(
            VexLifeProdServiceContract.ConversationControlOutcome.REALIZED,
            response.outcome,
        )
    }

    @Test
    fun memoryDreamRunRequestRequiresExplicitBoundSourceFrontier() {
        val request =
            VexLifeProdServiceContract.MemoryDreamRunRequest(
                restInvocationAuthorityRef = "authority.manual.g03.test",
                dayRef = "day.g03.000",
                dayIndex = 0,
                calendarDateRef = "2026-11-10",
                timeZoneRef = "America/Los_Angeles",
                observedAt = "2026-11-10T07:45:00Z",
                expectedConversationHeadSha256 = "a".repeat(64),
                expectedScoreHeadSha256 = "b".repeat(64),
                expectedDailyDreamHeadSha256 = null,
            )

        assertEquals("day.g03.000", request.dayRef)
        assertNull(request.expectedDailyDreamHeadSha256)

        assertFailsWith<IllegalArgumentException> {
            request.copy(expectedConversationHeadSha256 = "stale")
        }
        assertFailsWith<IllegalArgumentException> {
            request.copy(dayIndex = -1)
        }
    }
    @Test
    fun persistentVisionScopeContractKeepsProviderAndCaptureTruthOut() {
        assertEquals(
            "/api/v1/vision/scope",
            VexLifeProdServiceContract.VisionScopePath,
        )
        assertEquals(
            "/api/v1/vision/scope/engage",
            VexLifeProdServiceContract.VisionScopeEngagePath,
        )
        assertEquals(
            "/api/v1/vision/scope/stop",
            VexLifeProdServiceContract.VisionScopeStopPath,
        )

        val projection =
            VexLifeProdServiceContract.VisionSelectedScopeProjection(
                schemaVersion =
                    VexLifeProdServiceContract.VisionSelectedScopeProjectionSchema,
                scopeSessionRef = "session.macos-screen-selected-scope.product.test.001",
                state = "ENGAGED",
                selectedScopePresent = true,
                scopeIndicatorRequired = true,
                lookingIndicatorRequired = false,
                lookRequestRefOrNull = null,
                safetyAuthorizationRefOrNull = null,
                safetyBindingRefOrNull = null,
                safetyReceiptFingerprintOrNull = null,
                safetyObservedAtOrNull = null,
                holdReasonOrNull = null,
                endReasonOrNull = null,
                sensorActive = false,
                screenCaptureActive = false,
                screenCaptureAuthorized = false,
                captureExecutionAuthorized = false,
                requestReplayProtectionEstablished = false,
                captureExecutionRequiresSingleUseEffectReservation = true,
                activeObservation = false,
                rawBytesIncluded = false,
                rawRetentionAllowed = false,
                continuousCaptureAllowed = false,
                backgroundCaptureAllowed = false,
                visualModelInputAuthorized = false,
                memoryAccepted = false,
                trainingEligible = false,
                publicationEligible = false,
            )

        assertEquals(
            "ENGAGED",
            projection.state,
        )
        assertFalse(
            projection.screenCaptureActive,
        )
        assertFalse(
            projection.visualModelInputAuthorized,
        )

        assertFailsWith<IllegalArgumentException> {
            projection.copy(
                screenCaptureActive = true,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            projection.copy(
                state = "LOOKING",
                lookingIndicatorRequired = true,
                lookRequestRefOrNull = null,
            )
        }
    }

    @Test
    fun persistentVisionRuntimeResponseIsExactCommandBoundTruth() {
        val request =
            VexLifeProdServiceContract.VisionScopeCommandRequest(
                requestRef = "request.vexlife-prod.vision.scope.001",
            )
        assertEquals(
            "request.vexlife-prod.vision.scope.001",
            request.requestRef,
        )

        val response =
            VexLifeProdServiceContract.VisionSelectedScopeRuntimeResponse(
                schemaVersion =
                    VexLifeProdServiceContract.VisionSelectedScopeRuntimeResponseSchema,
                commandRef = request.requestRef,
                commandType = "ENGAGE",
                ok = true,
                projectionOrNull = null,
                shutdownRequested = false,
                errorCodeOrNull = null,
            )
        assertEquals(
            "ENGAGE",
            response.commandType,
        )

        assertFailsWith<IllegalArgumentException> {
            response.copy(
                ok = false,
                errorCodeOrNull = null,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            response.copy(
                commandType = "CAPTURE",
            )
        }
    }

    @Test
    fun memoryDreamProjectionExposesScoreConversationCurrentnessWithoutEffect() {
        val projection =
            VexLifeProdServiceContract.MemoryDreamProjection(
                schemaVersion = VexLifeProdServiceContract.MemoryDreamProjectionSchema,
                state = "SCORE_NOT_CAUGHT_UP",
                sourceConversationHeadSha256 = "a".repeat(64),
                sourceScoreHeadSha256 = "b".repeat(64),
                scoreSourceConversationHeadSha256 = "c".repeat(64),
                sourceSemanticAuthorityHeadSha256 = "d".repeat(64),
                currentDailyDreamHeadSha256 = null,
                currentDailyStratumSha256 = null,
                currentDayRef = null,
                currentDayIndex = null,
                nextDayIndex = 0,
                activeStatementCount = 0,
                heldStatementCount = 0,
                openLoopCount = 0,
                effectsPerformed = false,
            )
        assertEquals("c".repeat(64), projection.scoreSourceConversationHeadSha256)
        assertFalse(projection.effectsPerformed)
    }

    @Test
    fun workspaceEffectsContractKeepsStrongerAuthoritySeparateFromLocalSourceReadGrant() {
        val sourceRef =
            VexLifeLocalSourcesFamily.sourceRefForSha256(
                "a".repeat(
                    64,
                ),
            )
        val grant =
            VexLifeWorkspaceEffectsFamily.Grant(
                grantRef =
                    VexLifeWorkspaceEffectsFamily.GrantRefPrefix +
                        "b".repeat(
                            64,
                        ),
                sourceRef =
                    sourceRef,
                sourceGrantedAt =
                    "2026-11-25T03:00:00Z",
                grantedAt =
                    "2026-11-25T03:01:00Z",
            )
        val projection =
            VexLifeProdServiceContract.WorkspaceEffectsProjection(
                schemaVersion =
                    VexLifeProdServiceContract.WorkspaceEffectsProjectionSchema,
                state =
                    VexLifeWorkspaceEffectsFamily.StateGranted,
                grants =
                    listOf(
                        grant,
                    ),
                effectsPerformed =
                    false,
            )
        val request =
            VexLifeProdServiceContract.WorkspaceEffectsControlRequest(
                controlRef =
                    VexLifeWorkspaceEffectsFamily.ControlRevoke,
                sourceRef =
                    sourceRef,
            )
        val response =
            VexLifeProdServiceContract.WorkspaceEffectsControlResponse(
                schemaVersion =
                    VexLifeProdServiceContract.WorkspaceEffectsControlResponseSchema,
                controlRef =
                    request.controlRef,
                sourceRef =
                    request.sourceRef,
                outcome =
                    VexLifeProdServiceContract.WorkspaceEffectsControlOutcome.REALIZED,
                effects =
                    VexLifeProdServiceContract.WorkspaceEffectsProjection(
                        schemaVersion =
                            VexLifeProdServiceContract.WorkspaceEffectsProjectionSchema,
                        state =
                            VexLifeWorkspaceEffectsFamily.StateEmpty,
                        grants =
                            emptyList(),
                        effectsPerformed =
                            false,
                    ),
                grantMutationPerformed =
                    true,
            )

        assertEquals(
            grant.sourceRef,
            projection.grants.single().sourceRef,
        )
        assertEquals(
            VexLifeWorkspaceEffectsFamily.ControlRevoke,
            response.controlRef,
        )
        assertTrue(
            response.grantMutationPerformed,
        )
        assertFalse(
            response.effects.effectsPerformed,
        )

        assertFailsWith<IllegalArgumentException> {
            response.copy(
                grantMutationPerformed =
                    false,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            VexLifeProdServiceContract.WorkspaceEffectsControlRequest(
                controlRef =
                    VexLifeLocalSourcesFamily.ControlRemove,
                sourceRef =
                    sourceRef,
            )
        }
    }

}

// [VXG RealForever]
