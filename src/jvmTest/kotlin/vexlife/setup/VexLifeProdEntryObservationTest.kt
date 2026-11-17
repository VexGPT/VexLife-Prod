package vexlife.setup

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import vexlife.entry.VexLifeProdEntryFamily
import vexlife.home.VexLifeProdHome
import vexlife.home.VexLifeProdHomeFiles
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticRuntimeRealization
import vexlife.service.VexLifeProdServiceContract

class VexLifeProdEntryObservationTest {
    private val sourceSha =
        "a".repeat(
            40,
        )

    private fun homeFixture(): Pair<Path, VexLifeProdHomeFiles.Location.Existing> {
        val root =
            Files
                .createTempDirectory(
                    "vexlife-entry-observation-",
                )
                .toAbsolutePath()
                .resolve(
                    ".vexlife",
                )
        val plan =
            VexLifeProdHome.plan(
                personRef = VexLifeProdHome.DefaultPersonRef,
                familyRef = VexLifeProdHome.DefaultFamilyRef,
                deviceName = "Entry Observation Test",
                platform = "test",
                architecture = "test",
                ids =
                    VexLifeProdHome.InstallationIds(
                        deviceRef = "device.vexlife.entry-observation-test",
                        companionLineageRef =
                            "companion-lineage.vexlife.entry-observation-test",
                    ),
                createdAt = "2026-11-17T00:00:00Z",
            )
        val result =
            VexLifeProdHomeFiles.Result(
                state =
                    VexLifeProdHome.LocationDisposition.Canonical.wireValue,
                productHome =
                    root.toString(),
                manifest =
                    plan.manifest,
                installation =
                    plan.installation,
                effectsPerformed =
                    false,
            )
        return root to
            VexLifeProdHomeFiles.Location.Existing(
                result,
            )
    }

    private fun current(
        home: Path,
        model: HereticModelCatalog.Entry?,
        platform: String,
        architecture: String,
        artifactState: String =
            if (model == null) {
                "NOT_SELECTED"
            } else {
                "COMPLETE_BY_SIZE_NOT_HASHED"
            },
    ): VexLifeProdServiceContract.CurrentStateProjection =
        VexLifeProdServiceContract.CurrentStateProjection(
            schemaVersion =
                VexLifeProdServiceContract.CurrentStateProjectionSchema,
            observedAt =
                "2026-11-17T00:00:00Z",
            homePath =
                home.toString(),
            homeDisposition =
                VexLifeProdHome.LocationDisposition.Canonical.wireValue,
            homeRef =
                "home.vexlife.entry-observation-test",
            deviceRef =
                "device.vexlife.entry-observation-test",
            companionLineageRef =
                "companion-lineage.vexlife.entry-observation-test",
            platform =
                platform,
            architecture =
                architecture,
            chipModel =
                "test hardware",
            hostEligibilityState =
                "ELIGIBLE",
            hostEligibilityReason =
                null,
            totalMemoryBytes =
                64L * 1024L * 1024L * 1024L,
            modelMinimumMemoryBytes =
                model?.recommendedMemoryBytes,
            freeStorageBytes =
                100L * 1024L * 1024L * 1024L,
            modelSelectionState =
                if (model == null) {
                    "UNCONFIGURED"
                } else {
                    "BOUND_HERETIC_PRODUCT_MODEL"
                },
            activeModelRef =
                model?.modelRef,
            activeModelProfileRef =
                model?.identity?.modelProfileRef,
            runtimeState =
                if (model == null) {
                    "NOT_SELECTED"
                } else {
                    "BOUND_PRODUCT_MODEL"
                },
            runtimeCurrentness =
                "CONFIGURATION_ONLY_NOT_REOBSERVED",
            runtimeReceiptPath =
                model?.let {
                    "runtime/test-receipt.json"
                },
            modelArtifactState =
                artifactState,
            modelArtifactTotalBytes =
                model?.totalBytes,
            modelArtifactPresentBySizeBytes =
                model?.totalBytes,
            modelArtifactMissingBytes =
                if (model == null) {
                    null
                } else {
                    0L
                },
            modelArtifactInvalidSizeBytes =
                if (model == null) {
                    null
                } else {
                    0L
                },
            effectsPerformed =
                false,
        )

    @Test
    fun existingHomeWithMacOnlyModelHoldsOnWindowsInsteadOfFallingBack() {
        val (
            home,
            location,
        ) =
            homeFixture()
        val selected =
            HereticModelCatalog.Candidate9B

        val observed =
            VexLifeProdEntryObservation.observe(
                desiredSourceSha = sourceSha,
                productHome = home,
                current =
                    current(
                        home = home,
                        model = selected,
                        platform = "windows",
                        architecture = "x86_64",
                    ),
                location = location,
                service =
                    VexLifeProdEntryObservation.Service.Absent,
                runtimePrerequisitesPresent = false,
                target =
                    HereticRuntimeRealization.WindowsX64,
                hostRequirement =
                    VexLifeProdEntryObservation.HostRequirement(
                        requiredHardware = "Windows x64",
                        requiredMemoryBytes =
                            HereticModelCatalog.Candidate4B.recommendedMemoryBytes,
                    ),
            )

        val held =
            observed.entryState as
                VexLifeProdEntryFamily.ModelTargetHeld

        assertEquals(
            selected.modelRef,
            held.modelRef,
        )
        assertEquals(
            HereticRuntimeRealization.WindowsX64.targetRef,
            held.targetRef,
        )
        assertEquals(
            selected,
            observed.model,
        )
    }

    @Test
    fun runningOwnedServiceMapsToTheSameCurrentMeaningAcrossAdapters() {
        val (
            home,
            location,
        ) =
            homeFixture()
        val selected =
            HereticModelCatalog.Candidate4B

        val observed =
            VexLifeProdEntryObservation.observe(
                desiredSourceSha = sourceSha,
                productHome = home,
                current =
                    current(
                        home = home,
                        model = selected,
                        platform = "windows",
                        architecture = "x86_64",
                    ),
                location = location,
                service =
                    VexLifeProdEntryObservation.Service.RunningOwned(
                        sourceSha = sourceSha,
                        pid = 4242L,
                    ),
                runtimePrerequisitesPresent = true,
                target =
                    HereticRuntimeRealization.WindowsX64,
                hostRequirement =
                    VexLifeProdEntryObservation.HostRequirement(
                        requiredHardware = "Windows x64",
                        requiredMemoryBytes =
                            selected.recommendedMemoryBytes,
                    ),
            )

        assertEquals(
            VexLifeProdEntryFamily.StateCurrent,
            observed.entryState.stateRef,
        )
        assertEquals(
            sourceSha,
            observed.currentServiceSourceSha,
        )
        assertEquals(
            4242L,
            observed.currentServicePid,
        )
        assertFalse(
            observed.currentState.effectsPerformed,
        )
    }
}

// [VXG RealForever]
