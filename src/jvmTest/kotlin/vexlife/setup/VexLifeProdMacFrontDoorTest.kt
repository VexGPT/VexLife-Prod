package vexlife.setup

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import vexlife.browser.VexLifeProdBrowserContract
import vexlife.entry.VexLifeProdEntryFamily
import vexlife.platform.macos.MacUserLaunchdProcess
import vexlife.service.VexLifeProdMacServiceLifetime
import vexlife.service.VexLifeProdServiceContract

class VexLifeProdMacFrontDoorTest {
    private val desired =
        "a".repeat(
            40,
        )

    private val prior =
        "b".repeat(
            40,
        )

    private fun currentState(): VexLifeProdServiceContract.CurrentStateProjection =
        VexLifeProdServiceContract.CurrentStateProjection(
            schemaVersion = VexLifeProdServiceContract.CurrentStateProjectionSchema,
            observedAt = "2026-11-13T00:00:00Z",
            homePath = "/tmp/.vexlife",
            homeDisposition = "EXISTING_CANONICAL_HOME",
            homeRef = "home.vexlife.front-door-test",
            deviceRef = "device.vexlife.front-door-test",
            companionLineageRef = "companion.vexlife.front-door-test",
            platform = "macOS",
            architecture = "arm64",
            chipModel = "Apple M4 Pro",
            hostEligibilityState = "ELIGIBLE",
            hostEligibilityReason = null,
            totalMemoryBytes = 48L * 1024L * 1024L * 1024L,
            modelMinimumMemoryBytes = 16L * 1024L * 1024L * 1024L,
            freeStorageBytes = 3L * 1024L * 1024L * 1024L,
            modelSelectionState = "BOUND_HERETIC_PRODUCT_MODEL",
            activeModelRef = "model.heretic.front-door-test",
            activeModelProfileRef = "profile.heretic.front-door-test",
            runtimeState = "BOUND_PRODUCT_MODEL",
            runtimeCurrentness = "CONFIGURATION_ONLY_NOT_REOBSERVED",
            runtimeReceiptPath = "/tmp/.vexlife/runtime/heretic.json",
            modelArtifactState = "COMPLETE_BY_SIZE_NOT_HASHED",
            modelArtifactTotalBytes = 100L,
            modelArtifactPresentBySizeBytes = 100L,
            modelArtifactMissingBytes = 0L,
            modelArtifactInvalidSizeBytes = 0L,
            effectsPerformed = false,
        )

    private fun absentHomeCurrentState(): VexLifeProdServiceContract.CurrentStateProjection =
        VexLifeProdServiceContract.CurrentStateProjection(
            schemaVersion = VexLifeProdServiceContract.CurrentStateProjectionSchema,
            observedAt = "2026-11-13T00:00:00Z",
            homePath = "/tmp/.vexlife",
            homeDisposition = "ABSENT_CANONICAL_VEXLIFE_HOME",
            homeRef = null,
            deviceRef = null,
            companionLineageRef = null,
            platform = "darwin",
            architecture = "arm64",
            chipModel = "Apple M4 Pro",
            hostEligibilityState = "ELIGIBLE",
            hostEligibilityReason = null,
            totalMemoryBytes = 48L * 1024L * 1024L * 1024L,
            modelMinimumMemoryBytes = null,
            freeStorageBytes = 100L * 1024L * 1024L * 1024L,
            modelSelectionState = "UNCONFIGURED",
            activeModelRef = null,
            activeModelProfileRef = null,
            runtimeState = "NOT_SELECTED",
            runtimeCurrentness = "NOT_APPLICABLE",
            runtimeReceiptPath = null,
            modelArtifactState = "NOT_SELECTED",
            modelArtifactTotalBytes = null,
            modelArtifactPresentBySizeBytes = null,
            modelArtifactMissingBytes = null,
            modelArtifactInvalidSizeBytes = null,
            effectsPerformed = false,
        )

    private fun receipt(
        sourceSha: String,
        pid: Long =
            4242L,
    ): VexLifeProdMacServiceLifetime.Receipt =
        VexLifeProdMacServiceLifetime.Receipt(
            schemaVersion = VexLifeProdMacServiceLifetime.ReceiptSchema,
            sourceSha = sourceSha,
            imagePayloadSha256 =
                "c".repeat(
                    64,
                ),
            imagePath =
                "/tmp/.vexlife/runtime/product-service/images/" +
                    sourceSha,
            serviceOrigin = VexLifeProdServiceContract.Origin,
            launchd =
                MacUserLaunchdProcess.Receipt(
                    schemaVersion = MacUserLaunchdProcess.Schema,
                    substrateRef = MacUserLaunchdProcess.SubstrateRef,
                    label = VexLifeProdMacServiceLifetime.Label,
                    domainTarget = "gui/501",
                    plistPath = "/tmp/.vexlife/runtime/product-service/launchd/com.vexlife.prod.service.plist",
                    programPath =
                        "/tmp/.vexlife/runtime/product-service/images/" +
                            sourceSha +
                            "/bin/vexlife-prod-service",
                    programArguments =
                        listOf(
                            "--source-sha",
                            sourceSha,
                        ),
                    workingDirectory =
                        "/tmp/.vexlife/runtime/product-service/images/" +
                            sourceSha,
                    stdoutPath = "/tmp/.vexlife/runtime/product-service/logs/service.out.log",
                    stderrPath = "/tmp/.vexlife/runtime/product-service/logs/service.err.log",
                    environment = emptyMap(),
                    keepAlive = false,
                    pid = pid,
                ),
        )

    private fun currentness(
        sourceSha: String,
        pid: Long =
            4242L,
    ): VexLifeProdServiceContract.SourceCurrentness =
        VexLifeProdServiceContract.SourceCurrentness(
            schemaVersion = VexLifeProdServiceContract.SourceCurrentnessSchema,
            serviceRef = VexLifeProdServiceContract.ServiceRef,
            productRef = VexLifeProdServiceContract.ProductRef,
            serviceState = "SOURCE_BOUND_LISTENING",
            sourceSha = sourceSha,
            processPid = pid,
            frameHostOrigin = VexLifeProdBrowserContract.FrameHostOrigin,
            runtimeCurrentness = "NOT_REOBSERVED_BY_SERVICE",
            startupQualifiedRuntimeReceiptRef = "receipt.vexlife-prod.front-door-test",
            currentConversationHeadSha256 = null,
            effectsPerformed = false,
        )

    private fun hooks(
        existing: VexLifeProdMacServiceLifetime.ExistingObservation,
    ): VexLifeProdMacFrontDoor.Hooks =
        VexLifeProdMacFrontDoor.Hooks(
            observeCurrentState = ::currentState,
            observeExistingService = {
                existing
            },
        )

    @Test
    fun absentHomeMapsToSetupBeforeServiceLifetimeObservation() {
        var serviceObserved =
            false
        val hooks =
            VexLifeProdMacFrontDoor.Hooks(
                observeCurrentState =
                    ::absentHomeCurrentState,
                observeExistingService = {
                    serviceObserved =
                        true
                    error(
                        "absent Home must not descend into service lifetime observation",
                    )
                },
            )

        val projection =
            VexLifeProdMacFrontDoor.observeWithHooks(
                desiredSourceSha = desired,
                hooks = hooks,
            )
        val effect =
            VexLifeProdMacFrontDoor.resolveControlWithHooks(
                desiredSourceSha = desired,
                controlRef = VexLifeProdEntryFamily.ControlSetUp,
                hooks = hooks,
            )

        assertEquals(
            VexLifeProdEntryFamily.StateSetupRequired,
            projection.semanticStateRef,
        )
        assertEquals(
            "Set Up VexLife",
            projection.displayState.primaryLabel,
        )
        assertEquals(
            VexLifeProdEntryFamily.EffectConvergeProduct,
            effect.effectRef,
        )
        assertEquals(
            true,
            effect.allowAcquisition,
        )
        assertFalse(
            serviceObserved,
        )
    }

    @Test
    fun exactRunningSourceMapsToCommonCurrentStateAndOpenDisplay() {
        val value =
            VexLifeProdMacFrontDoor.observeWithHooks(
                desiredSourceSha = desired,
                hooks =
                    hooks(
                        VexLifeProdMacServiceLifetime.ExistingObservation.RunningOwned(
                            receipt = receipt(desired),
                            sourceCurrentness = currentness(desired),
                        ),
                    ),
            )

        assertEquals(
            VexLifeProdEntryFamily.StateCurrent,
            value.semanticStateRef,
        )
        assertEquals(
            VexLifeProdEntryFamily.ControlOpenCurrent,
            value.displayState.primaryControlRef,
        )
        assertEquals(
            "Open VexLife",
            value.displayState.primaryLabel,
        )
        assertFalse(
            value.effectsPerformed,
        )
    }

    @Test
    fun exactOwnedPriorSourceMapsToSharedUpdateStateAndNoAcquisitionEffect() {
        val observed =
            VexLifeProdMacServiceLifetime.ExistingObservation.RunningOwned(
                receipt = receipt(prior),
                sourceCurrentness = currentness(prior),
            )
        val projection =
            VexLifeProdMacFrontDoor.observeWithHooks(
                desiredSourceSha = desired,
                hooks = hooks(observed),
            )
        val effect =
            VexLifeProdMacFrontDoor.resolveControlWithHooks(
                desiredSourceSha = desired,
                controlRef = VexLifeProdEntryFamily.ControlConvergeDesired,
                hooks = hooks(observed),
            )

        assertEquals(
            VexLifeProdEntryFamily.StateUpdateAvailable,
            projection.semanticStateRef,
        )
        assertEquals(
            prior,
            projection.currentServiceSourceSha,
        )
        assertEquals(
            "Update & Restart",
            projection.displayState.primaryLabel,
        )
        assertEquals(
            VexLifeProdEntryFamily.EffectConvergeProduct,
            effect.effectRef,
        )
        assertEquals(
            false,
            effect.allowAcquisition,
        )
        assertFalse(
            effect.effectsPerformed,
        )
    }

    @Test
    fun ownedUnhealthyServiceMapsToRepairMeaningWithoutDuplicatingPlatformDecisionLogic() {
        val currentRepair =
            VexLifeProdMacServiceLifetime.ExistingObservation.OwnedNotReady(
                receipt = receipt(desired),
                launchdObservation = MacUserLaunchdProcess.Observation.LoadedStopped,
            )
        val priorRepair =
            VexLifeProdMacServiceLifetime.ExistingObservation.OwnedNotReady(
                receipt = receipt(prior),
                launchdObservation = MacUserLaunchdProcess.Observation.LoadedStopped,
            )

        assertEquals(
            VexLifeProdEntryFamily.StateRepairRequired,
            VexLifeProdMacFrontDoor
                .observeWithHooks(
                    desired,
                    hooks(currentRepair),
                )
                .semanticStateRef,
        )
        assertEquals(
            VexLifeProdEntryFamily.StateUpdateRepairRequired,
            VexLifeProdMacFrontDoor
                .observeWithHooks(
                    desired,
                    hooks(priorRepair),
                )
                .semanticStateRef,
        )
    }

    @Test
    fun absentSetupIsOnlyMacEntryResolutionThatCanGrantAcquisition() {
        val absent =
            VexLifeProdMacServiceLifetime.ExistingObservation.Absent
        val effect =
            VexLifeProdMacFrontDoor.resolveControlWithHooks(
                desiredSourceSha = desired,
                controlRef = VexLifeProdEntryFamily.ControlSetUp,
                hooks = hooks(absent),
            )

        assertEquals(
            VexLifeProdEntryFamily.StateSetupRequired,
            effect.semanticStateRef,
        )
        assertEquals(
            VexLifeProdEntryFamily.EffectConvergeProduct,
            effect.effectRef,
        )
        assertEquals(
            true,
            effect.allowAcquisition,
        )
    }

    @Test
    fun unknownOwnershipProjectsHeldAndRejectsConvergeControl() {
        val unknown =
            VexLifeProdMacServiceLifetime.ExistingObservation.HeldUnknown(
                "receipt/PID ownership is not exact",
            )
        val projection =
            VexLifeProdMacFrontDoor.observeWithHooks(
                desiredSourceSha = desired,
                hooks = hooks(unknown),
            )

        assertEquals(
            VexLifeProdEntryFamily.StateHeldUnknown,
            projection.semanticStateRef,
        )
        assertEquals(
            "receipt/PID ownership is not exact",
            projection.serviceDetail,
        )
        assertNull(
            projection.displayState.primaryControlRef,
        )
    }
}

// [VXG RealForever]
