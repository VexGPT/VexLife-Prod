package vexlife.service

import java.nio.file.Files
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.home.VexLifeProdHome
import vexlife.home.VexLifeProdHomeFiles
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticProductIdentity
import vexlife.model.heretic.runtime.FileHereticModelConfigurationStore
import vexlife.model.heretic.runtime.HereticArtifactMaterialization
import vexlife.model.heretic.runtime.HereticMacHostProfile

class VexLifeProdCurrentStateTest {
    @Test
    fun currentStateReadsExistingOwnersWithoutSetupEffects() {
        val home = Files.createTempDirectory("vexlife-prod-current-state-").toAbsolutePath()

        try {
            CanonicalVexLifeHomeFixture.form(
                home = home,
                deviceSuffix = "current-state",
                lineageSuffix = "current-state",
            )
            val observedHome = requireNotNull(VexLifeProdHomeFiles.observeExisting(home))
            val bound =
                HereticModelConfiguration.Value(
                    schemaVersion = HereticModelConfiguration.Schema,
                    state = HereticModelConfiguration.State.BoundProductModel,
                    homeRef = observedHome.manifest.homeRef,
                    companionLineageRef = observedHome.manifest.currentCompanionLineageRef,
                    modelRef = HereticProductIdentity.ModelRef,
                    modelProfileRef = HereticProductIdentity.ModelProfileRef,
                    artifactRef = HereticProductIdentity.ArtifactSetRef,
                    artifactContentSetSha256 = HereticProductIdentity.ArtifactContentSetSha256,
                    runtimeAdapterRef = HereticProductIdentity.RuntimeAdapterRef,
                    runtimeClass = HereticProductIdentity.RuntimeClass,
                    endpoint = HereticProductIdentity.Endpoint,
                    requestModel = HereticProductIdentity.RequestModel,
                    runtimeReceiptPath = HereticProductIdentity.StableRuntimeReceiptRelativePath,
                    automaticFallback = false,
                    automaticDownload = false,
                    automaticActivation = false,
                )
            val totalBytes = 15_153_242_506L

            val projection =
                VexLifeProdCurrentState.observeWithHooks(
                    VexLifeProdCurrentState.Hooks(
                        resolveProductHome = {
                            home
                        },
                        observeLocation = { productHome ->
                            VexLifeProdHomeFiles.observeLocation(productHome)
                        },
                        observeHost = {
                            HereticMacHostProfile.evaluate(
                                HereticMacHostProfile.Observation(
                                    platform = "darwin",
                                    architecture = "arm64",
                                    chipModel = "Apple M4 Pro",
                                    totalMemoryBytes = 36L * 1024L * 1024L * 1024L,
                                )
                            )
                        },
                        observeModel = { _, _ ->
                            FileHereticModelConfigurationStore.Snapshot.Heretic(bound)
                        },
                        observeArtifacts = { _, _ ->
                            HereticArtifactMaterialization.PresenceObservation(
                                state = "COMPLETE_BY_SIZE_NOT_HASHED",
                                modelPath = home.resolve("models/heretic/test").toString(),
                                providerRevision = "a".repeat(40),
                                contentSetSha256 = HereticProductIdentity.ArtifactContentSetSha256,
                                memberCount = 11,
                                totalBytes = totalBytes,
                                presentBySizeBytes = totalBytes,
                                missingBytes = 0,
                                invalidSizeBytes = 0,
                                effectsPerformed = false,
                            )
                        },
                        observeFreeStorageBytes = {
                            100L * 1024L * 1024L * 1024L
                        },
                        now = {
                            Instant.parse("2026-11-12T18:00:00Z")
                        },
                    )
                )

            assertEquals(observedHome.manifest.homeRef, projection.homeRef)
            assertEquals(observedHome.installation.deviceRef, projection.deviceRef)
            assertEquals(HereticProductIdentity.ModelRef, projection.activeModelRef)
            assertEquals("BOUND_HERETIC_PRODUCT_MODEL", projection.modelSelectionState)
            assertEquals("COMPLETE_BY_SIZE_NOT_HASHED", projection.modelArtifactState)
            assertEquals(totalBytes, projection.modelArtifactPresentBySizeBytes)
            assertEquals("CONFIGURATION_ONLY_NOT_REOBSERVED", projection.runtimeCurrentness)
            assertEquals(
                HereticProductIdentity.StableRuntimeReceiptRelativePath,
                projection.runtimeReceiptPath,
            )
            assertEquals(100L * 1024L * 1024L * 1024L, projection.freeStorageBytes)
            assertFalse(projection.effectsPerformed)
        } finally {
            home.toFile().deleteRecursively()
        }
    }

    @Test
    fun absentCanonicalHomeProjectsNoEffectSetupAwareness() {
        val parent =
            Files.createTempDirectory("vexlife-prod-current-state-home-absent-").toAbsolutePath()
        val home = parent.resolve(".vexlife")

        try {
            val projection =
                VexLifeProdCurrentState.observeWithHooks(
                    VexLifeProdCurrentState.Hooks(
                        resolveProductHome = {
                            home
                        },
                        observeLocation = { productHome ->
                            VexLifeProdHomeFiles.Location.Formable(
                                productHome = productHome.toString(),
                                disposition = VexLifeProdHome.LocationDisposition.Absent,
                                nearestExistingAncestor = productHome.parent.toString(),
                                missingAncestorCount = 0,
                            )
                        },
                        observeHost = {
                            HereticMacHostProfile.evaluate(
                                HereticMacHostProfile.Observation(
                                    platform = "darwin",
                                    architecture = "arm64",
                                    chipModel = "Apple M4 Pro",
                                    totalMemoryBytes = 36L * 1024L * 1024L * 1024L,
                                )
                            )
                        },
                        observeModel = { _, _ ->
                            error("absent Home awareness must not read Product model binding")
                        },
                        observeArtifacts = { _, _ ->
                            error("absent Home awareness must not inspect model artifacts")
                        },
                        observeFreeStorageBytes = {
                            200L * 1024L * 1024L * 1024L
                        },
                        now = {
                            Instant.parse("2026-11-12T18:00:00Z")
                        },
                    )
                )

            assertEquals("ABSENT_CANONICAL_VEXLIFE_HOME", projection.homeDisposition)
            assertEquals(home.toString(), projection.homePath)
            assertNull(projection.homeRef)
            assertNull(projection.deviceRef)
            assertNull(projection.companionLineageRef)
            assertEquals("UNCONFIGURED", projection.modelSelectionState)
            assertEquals("NOT_SELECTED", projection.modelArtifactState)
            assertEquals(200L * 1024L * 1024L * 1024L, projection.freeStorageBytes)
            assertFalse(projection.effectsPerformed)
            assertFalse(Files.exists(home))
        } finally {
            parent.toFile().deleteRecursively()
        }
    }

    @Test
    fun unconfiguredCurrentStateDoesNotInventModelOrArtifactTruth() {
        val home =
            Files.createTempDirectory("vexlife-prod-current-state-unconfigured-").toAbsolutePath()

        try {
            CanonicalVexLifeHomeFixture.form(
                home = home,
                deviceSuffix = "current-state-unconfigured",
                lineageSuffix = "current-state-unconfigured",
            )

            val projection =
                VexLifeProdCurrentState.observeWithHooks(
                    VexLifeProdCurrentState.Hooks(
                        resolveProductHome = {
                            home
                        },
                        observeLocation = { productHome ->
                            VexLifeProdHomeFiles.observeLocation(productHome)
                        },
                        observeHost = {
                            HereticMacHostProfile.evaluate(
                                HereticMacHostProfile.Observation(
                                    platform = "darwin",
                                    architecture = "arm64",
                                    chipModel = "Apple M4 Pro",
                                    totalMemoryBytes = 36L * 1024L * 1024L * 1024L,
                                )
                            )
                        },
                        observeModel = { _, _ ->
                            FileHereticModelConfigurationStore.Snapshot.Unconfigured(
                                VexLifeProdHome.initialProductModelConfiguration()
                            )
                        },
                        observeArtifacts = { _, _ ->
                            error(
                                "unconfigured awareness must not inspect selected-model artifacts"
                            )
                        },
                        observeFreeStorageBytes = {
                            50L * 1024L * 1024L * 1024L
                        },
                    )
                )

            assertEquals("UNCONFIGURED", projection.modelSelectionState)
            assertNull(projection.activeModelRef)
            assertNull(projection.modelArtifactTotalBytes)
            assertEquals("NOT_SELECTED", projection.modelArtifactState)
            assertEquals("NOT_APPLICABLE", projection.runtimeCurrentness)
            assertFalse(projection.effectsPerformed)
        } finally {
            home.toFile().deleteRecursively()
        }
    }
}

// [VXG RealForever]
