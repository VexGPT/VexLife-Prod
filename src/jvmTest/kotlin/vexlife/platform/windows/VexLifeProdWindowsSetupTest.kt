package vexlife.platform.windows

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertEquals
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.home.VexLifeProdHomeFiles
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticRuntimeRealization
import vexlife.model.heretic.runtime.HereticRealizationArtifactMaterialization
import vexlife.platform.VexLifePlatformRealization
import vexlife.service.VexLifeProdServiceContract
import vexlife.setup.VexLifeProdSetupModel

class VexLifeProdWindowsSetupTest {
    private val sourceSha = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"

    @Test
    fun absentHomeMapsToSeparateHomeFormationStep() {
        val parent =
            Files.createTempDirectory("vexlife-windows-setup-home-")
                .toAbsolutePath()
        val home = parent.resolve(".vexlife")
        val location = VexLifeProdHomeFiles.observeLocation(home)

        val observed =
            VexLifeProdWindowsSetup.observeWithHooks(
                desiredSourceSha = sourceSha,
                productHome = home,
                explicitModelHandle = null,
                hooks =
                    hooks(
                        location = location,
                        current = currentProjection(home, location),
                        materialization = absentMaterialization(home),
                    ),
            )

        assertIs<vexlife.entry.VexLifeProdEntryFamily.HomeFormationRequired>(
            observed.entryState,
        )
        assertEquals(
            HereticModelCatalog.Models.Qwen4B,
            observed.selectedModel,
        )
    }

    @Test
    fun existingHomeAndMissingWindowsDependenciesMapsToSetupRequired() {
        val parent =
            Files.createTempDirectory("vexlife-windows-setup-missing-")
                .toAbsolutePath()
        val home = parent.resolve(".vexlife")
        CanonicalVexLifeHomeFixture.form(home)
        val location = VexLifeProdHomeFiles.observeLocation(home)

        val observed =
            VexLifeProdWindowsSetup.observeWithHooks(
                desiredSourceSha = sourceSha,
                productHome = home,
                explicitModelHandle = HereticModelCatalog.Models.Qwen4B.handle,
                hooks =
                    hooks(
                        location = location,
                        current = currentProjection(home, location),
                        materialization = absentMaterialization(home),
                    ),
            )

        assertIs<vexlife.entry.VexLifeProdEntryFamily.SetupRequired>(
            observed.entryState,
        )
        assertEquals(
            VexLifeWindowsRuntimeMaterialization.FileState.ABSENT,
            observed.materialization?.modelState,
        )
    }

    @Test
    fun savedModelAndExactWindowsDependenciesMapsToStartRequired() {
        val parent =
            Files.createTempDirectory("vexlife-windows-setup-ready-")
                .toAbsolutePath()
        val home = parent.resolve(".vexlife")
        CanonicalVexLifeHomeFixture.form(home)
        val location = VexLifeProdHomeFiles.observeLocation(home)
        val model = HereticModelCatalog.Models.Qwen4B
        check(
            VexLifeProdSetupModel.persist(
                productHome = home,
                model = model,
            )
        )

        val observed =
            VexLifeProdWindowsSetup.observeWithHooks(
                desiredSourceSha = sourceSha,
                productHome = home,
                explicitModelHandle = null,
                hooks =
                    hooks(
                        location = location,
                        current =
                            currentProjection(
                                home = home,
                                location = location,
                                selectedModel = model,
                            ),
                        materialization = readyMaterialization(home),
                    ),
            )

        assertIs<vexlife.entry.VexLifeProdEntryFamily.StartRequired>(
            observed.entryState,
        )
        assertEquals(
            true,
            observed.materialization?.ready,
        )
    }

    @Test
    fun unsupportedSavedMacModelRequiresExplicitWindowsChoice() {
        val parent =
            Files.createTempDirectory("vexlife-windows-setup-unsupported-")
                .toAbsolutePath()
        val home = parent.resolve(".vexlife")
        CanonicalVexLifeHomeFixture.form(home)
        val location = VexLifeProdHomeFiles.observeLocation(home)
        val macOnlyModel = HereticModelCatalog.Models.Qwen27B
        check(
            VexLifeProdSetupModel.persist(
                productHome = home,
                model = macOnlyModel,
            )
        )

        val observed =
            VexLifeProdWindowsSetup.observeWithHooks(
                desiredSourceSha = sourceSha,
                productHome = home,
                explicitModelHandle = null,
                hooks =
                    hooks(
                        location = location,
                        current =
                            currentProjection(
                                home = home,
                                location = location,
                                selectedModel = macOnlyModel,
                            ),
                        materialization = absentMaterialization(home),
                    ),
            )

        val held =
            assertIs<vexlife.entry.VexLifeProdEntryFamily.HeldUnknown>(
                observed.entryState,
            )
        assertEquals(
            "SELECTED_MODEL_NOT_SUPPORTED_ON_WINDOWS",
            held.detail,
        )
        assertEquals(
            null,
            observed.selectedModel,
        )
        assertEquals(
            listOf(HereticModelCatalog.Models.Qwen4B),
            observed.supportedModels,
        )
    }

    @Test
    fun windowsArtifactPresenceUsesSharedCurrentStateVocabulary() {
        assertEquals(
            "ABSENT",
            VexLifeProdWindowsCurrentState.serviceArtifactState(
                HereticRealizationArtifactMaterialization.PresenceState.ABSENT,
            ),
        )
        assertEquals(
            "PARTIAL_BY_SIZE",
            VexLifeProdWindowsCurrentState.serviceArtifactState(
                HereticRealizationArtifactMaterialization.PresenceState.PARTIAL,
            ),
        )
        assertEquals(
            "COMPLETE_BY_SIZE_NOT_HASHED",
            VexLifeProdWindowsCurrentState.serviceArtifactState(
                HereticRealizationArtifactMaterialization.PresenceState.PRESENT,
            ),
        )
        assertEquals(
            "INVALID_SIZE",
            VexLifeProdWindowsCurrentState.serviceArtifactState(
                HereticRealizationArtifactMaterialization.PresenceState.DAMAGED,
            ),
        )
    }

    private fun hooks(
        location: VexLifeProdHomeFiles.Location,
        current: VexLifeProdServiceContract.CurrentStateProjection,
        materialization: VexLifeWindowsRuntimeMaterialization.Observation,
    ) =
        VexLifeProdWindowsSetup.Hooks(
            observeHost = {
                resolvedWindowsHost()
            },
            observeHomeLocation = {
                location
            },
            observeCurrentState = {
                current
            },
            observeMaterialization = {
                _,
                _,
                ->
                materialization
            },
            servicePortAvailable = {
                true
            },
        )

    private fun resolvedWindowsHost() =
        VexLifeWindowsHostObservation.Result.Resolved(
            raw =
                VexLifeWindowsHostObservation.Raw(
                    osName = "Windows 11",
                    jvmOsArch = "amd64",
                    processorArchitecture = "AMD64",
                    processorArchitectureW6432 = null,
                ),
            target = HereticRuntimeRealization.WindowsX64,
            nativeArchitecture = VexLifePlatformRealization.Architecture.X86_64,
            processArchitecture = VexLifePlatformRealization.Architecture.X86_64,
            architectureEvidence =
                VexLifeWindowsHostObservation.ArchitectureEvidence.PROCESSOR_ARCHITECTURE,
        )

    private fun currentProjection(
        home: Path,
        location: VexLifeProdHomeFiles.Location,
        selectedModel: HereticModelCatalog.Entry? = null,
    ): VexLifeProdServiceContract.CurrentStateProjection {
        val existing = location as? VexLifeProdHomeFiles.Location.Existing
        val selected = selectedModel != null
        return VexLifeProdServiceContract.CurrentStateProjection(
            schemaVersion = VexLifeProdServiceContract.CurrentStateProjectionSchema,
            observedAt = "2026-11-18T00:00:00Z",
            homePath = home.toString(),
            homeDisposition = location.disposition.wireValue,
            homeRef = existing?.home?.manifest?.homeRef,
            deviceRef = existing?.home?.installation?.deviceRef,
            companionLineageRef = existing?.home?.installation?.companionLineageRef,
            platform = "windows",
            architecture = "x86_64",
            chipModel = "test windows host",
            hostEligibilityState = "ELIGIBLE",
            hostEligibilityReason = null,
            totalMemoryBytes = 32L * 1024L * 1024L * 1024L,
            modelMinimumMemoryBytes =
                selectedModel?.recommendedMemoryBytes,
            freeStorageBytes = 100L * 1024L * 1024L * 1024L,
            modelSelectionState =
                if (selected) {
                    "HERETIC_ESTABLISHMENT_PENDING"
                } else {
                    "UNCONFIGURED"
                },
            activeModelRef = selectedModel?.modelRef,
            activeModelProfileRef = selectedModel?.identity?.modelProfileRef,
            runtimeState =
                if (selected) {
                    "ESTABLISHMENT_PENDING"
                } else {
                    "NOT_SELECTED"
                },
            runtimeCurrentness =
                if (selected) {
                    "CONFIGURATION_ONLY_NOT_REOBSERVED"
                } else {
                    "NOT_APPLICABLE"
                },
            runtimeReceiptPath =
                if (selected) {
                    HereticRuntimeRealization.WindowsQwen4B.runtimeReceiptRelativePath
                } else {
                    null
                },
            modelArtifactState =
                if (selected) {
                    "COMPLETE_BY_SIZE_NOT_HASHED"
                } else {
                    "NOT_SELECTED"
                },
            modelArtifactTotalBytes =
                selectedModel?.totalBytes,
            modelArtifactPresentBySizeBytes =
                selectedModel?.totalBytes,
            modelArtifactMissingBytes =
                if (selected) 0L else null,
            modelArtifactInvalidSizeBytes =
                if (selected) 0L else null,
            effectsPerformed = false,
        )
    }

    private fun absentMaterialization(
        home: Path,
    ) =
        materialization(
            home = home,
            state = VexLifeWindowsRuntimeMaterialization.FileState.ABSENT,
        )

    private fun readyMaterialization(
        home: Path,
    ) =
        materialization(
            home = home,
            state = VexLifeWindowsRuntimeMaterialization.FileState.VERIFIED,
        )

    private fun materialization(
        home: Path,
        state: VexLifeWindowsRuntimeMaterialization.FileState,
    ): VexLifeWindowsRuntimeMaterialization.Observation {
        val root = home.resolve("runtime/vexlife-prod/test")
        return VexLifeWindowsRuntimeMaterialization.Observation(
            realizationRef = HereticRuntimeRealization.WindowsQwen4B.realizationRef,
            modelPath = home.resolve("models/test/model.gguf").toString(),
            modelState = state,
            runtimeArchivePath = root.resolve("runtime.zip").toString(),
            runtimeArchiveState = state,
            runtimeImagePath = root.resolve("image").toString(),
            runtimeImageState = state,
            runtimeExecutablePath = root.resolve("image/llama-server.exe").toString(),
            runtimeExecutableState = state,
            effectsPerformed = false,
        )
    }
}

// [VXG RealForever]
