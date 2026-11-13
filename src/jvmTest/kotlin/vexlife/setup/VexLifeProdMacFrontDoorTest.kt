package vexlife.setup

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import vexlife.browser.VexLifeProdBrowserContract
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

    private fun projection(
        existing: VexLifeProdMacServiceLifetime.ExistingObservation,
    ): VexLifeProdMacFrontDoor.Projection =
        VexLifeProdMacFrontDoor.observeWithHooks(
            desiredSourceSha = desired,
            hooks =
                VexLifeProdMacFrontDoor.Hooks(
                    observeCurrentState = ::currentState,
                    observeExistingService = {
                        existing
                    },
                ),
        )

    @Test
    fun exactRunningSourceOpensWithoutLifecycleEffect() {
        val value =
            projection(
                VexLifeProdMacServiceLifetime.ExistingObservation.RunningOwned(
                    receipt = receipt(desired),
                    sourceCurrentness = currentness(desired),
                ),
            )

        assertEquals(
            VexLifeProdMacFrontDoor.OpenCurrent,
            value.action,
        )
        assertEquals(
            desired,
            value.currentServiceSourceSha,
        )
        assertFalse(
            value.effectsPerformed,
        )
    }

    @Test
    fun exactOwnedPriorSourceOffersUpdateInsteadOfTreatingPortOccupancyAsFailure() {
        val value =
            projection(
                VexLifeProdMacServiceLifetime.ExistingObservation.RunningOwned(
                    receipt = receipt(prior),
                    sourceCurrentness = currentness(prior),
                ),
            )

        assertEquals(
            VexLifeProdMacFrontDoor.UpdateAvailable,
            value.action,
        )
        assertEquals(
            prior,
            value.currentServiceSourceSha,
        )
        assertEquals(
            4242L,
            value.currentServicePid,
        )
        assertFalse(
            value.effectsPerformed,
        )
    }

    @Test
    fun ownedUnhealthyServiceOffersRepairOrUpdateRepairBySourceIdentity() {
        val currentRepair =
            projection(
                VexLifeProdMacServiceLifetime.ExistingObservation.OwnedNotReady(
                    receipt = receipt(desired),
                    launchdObservation = MacUserLaunchdProcess.Observation.LoadedStopped,
                ),
            )
        val priorRepair =
            projection(
                VexLifeProdMacServiceLifetime.ExistingObservation.OwnedNotReady(
                    receipt = receipt(prior),
                    launchdObservation = MacUserLaunchdProcess.Observation.LoadedStopped,
                ),
            )

        assertEquals(
            VexLifeProdMacFrontDoor.RepairRequired,
            currentRepair.action,
        )
        assertEquals(
            VexLifeProdMacFrontDoor.UpdateRepairRequired,
            priorRepair.action,
        )
    }

    @Test
    fun absentAndUnknownRemainDistinctAndNoEffecting() {
        val absent =
            projection(
                VexLifeProdMacServiceLifetime.ExistingObservation.Absent,
            )
        val unknown =
            projection(
                VexLifeProdMacServiceLifetime.ExistingObservation.HeldUnknown(
                    "receipt/PID ownership is not exact",
                ),
            )

        assertEquals(
            VexLifeProdMacFrontDoor.SetupRequired,
            absent.action,
        )
        assertEquals(
            VexLifeProdMacFrontDoor.HoldUnknown,
            unknown.action,
        )
        assertEquals(
            "receipt/PID ownership is not exact",
            unknown.serviceDetail,
        )
        assertFalse(
            absent.effectsPerformed,
        )
        assertFalse(
            unknown.effectsPerformed,
        )
    }
}

// [VXG RealForever]
