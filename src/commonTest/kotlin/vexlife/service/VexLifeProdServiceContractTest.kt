package vexlife.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import vexlife.browser.VexLifeProdBrowserContract

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
            "/api/v1/context",
            VexLifeProdServiceContract.ContextPath,
        )
        assertEquals(
            "/api/v1/memory/dream",
            VexLifeProdServiceContract.MemoryDreamPath,
        )
        assertEquals(
            "/api/v1/conversation/send",
            VexLifeProdServiceContract.ConversationSendPath,
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
}

// [VXG RealForever]
