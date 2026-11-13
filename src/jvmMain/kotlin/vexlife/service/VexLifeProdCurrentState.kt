package vexlife.service

import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import vexlife.home.VexLifeProdHome
import vexlife.home.VexLifeProdHomeFiles
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.runtime.FileHereticModelConfigurationStore
import vexlife.model.heretic.runtime.HereticArtifactMaterialization
import vexlife.model.heretic.runtime.HereticMacHostProfile

/**
 * Read-only convergence of current host, Home, model-selection and artifact-presence truth for the
 * human product surface.
 *
 * This projector deliberately does not depend on [vexlife.local.VexLifeProdLocalSetup]. That setup
 * may select/bind a model or recover an abandoned Conversation writer. Human awareness must be
 * formable before those effects are considered.
 *
 * The ordinary projection performs no setup, repair, acquisition, runtime start/recovery, model
 * selection/binding, Home mutation or multi-GiB artifact hashing.
 */
object VexLifeProdCurrentState {
    internal data class Hooks(
        val resolveProductHome: () -> Path = {
            VexLifeProdHomeFiles.defaultMacRoot(Path.of(System.getProperty("user.home")))
        },
        val observeLocation: (productHome: Path) -> VexLifeProdHomeFiles.Location = { productHome ->
            VexLifeProdHomeFiles.observeLocation(productHome)
        },
        val observeHost: () -> HereticMacHostProfile.Result = {
            HereticMacHostProfile.observeCurrent()
        },
        val observeModel:
            (
                productHome: Path,
                manifest: VexLifeProdHome.Manifest,
            ) -> FileHereticModelConfigurationStore.Snapshot =
            { productHome, manifest ->
                FileHereticModelConfigurationStore(productHome).read(manifest)
            },
        val observeArtifacts:
            (productHome: Path) -> HereticArtifactMaterialization.PresenceObservation =
            { productHome ->
                HereticArtifactMaterialization.observePresence(productHome)
            },
        val observeFreeStorageBytes: (productHome: Path) -> Long? = { productHome ->
            try {
                // The volume that will hold the Home is the one containing its nearest existing
                // ancestor; an alternate Home may sit under folders formation has not created yet.
                var observedPath: Path? = productHome
                while (observedPath != null && !Files.exists(observedPath)) {
                    observedPath = observedPath.parent
                }
                Files.getFileStore(requireNotNull(observedPath)).usableSpace
            } catch (_: Exception) {
                null
            }
        },
        val now: () -> Instant = {
            Instant.now()
        },
    )

    fun observeCurrentMac(): VexLifeProdServiceContract.CurrentStateProjection =
        observeWithHooks(Hooks())

    fun observeCurrentMac(productHome: Path): VexLifeProdServiceContract.CurrentStateProjection {
        require(productHome.isAbsolute) {
            "selected Vex Home path must be absolute"
        }
        val selectedHome = productHome.normalize()
        return observeWithHooks(
            Hooks(
                resolveProductHome = {
                    selectedHome
                }
            )
        )
    }

    internal fun observeWithHooks(hooks: Hooks): VexLifeProdServiceContract.CurrentStateProjection {
        val productHome = hooks.resolveProductHome().toAbsolutePath().normalize()
        val host = hooks.observeHost()
        val location = hooks.observeLocation(productHome)
        require(Path.of(location.productHome) == productHome) {
            "current-state Home location observation changed the selected coordinate"
        }

        if (location !is VexLifeProdHomeFiles.Location.Existing) {
            return VexLifeProdServiceContract.CurrentStateProjection(
                schemaVersion = VexLifeProdServiceContract.CurrentStateProjectionSchema,
                observedAt = hooks.now().toString(),
                homePath = productHome.toString(),
                homeDisposition = location.disposition.wireValue,
                homeRef = null,
                deviceRef = null,
                companionLineageRef = null,
                platform = host.observation.platform,
                architecture = host.observation.architecture,
                chipModel = host.observation.chipModel,
                hostEligibilityState = host.state.name.uppercase(),
                hostEligibilityReason = host.reason,
                totalMemoryBytes = host.observation.totalMemoryBytes,
                modelMinimumMemoryBytes = null,
                freeStorageBytes = hooks.observeFreeStorageBytes(productHome),
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
        }

        val home = location.home

        require(!home.effectsPerformed && Path.of(home.productHome) == productHome) {
            "current-state Home observation is not one no-effect exact-path projection"
        }

        val model = hooks.observeModel(productHome, home.manifest)

        val modelProjection =
            when (model) {
                is FileHereticModelConfigurationStore.Snapshot.Unconfigured ->
                    ModelProjection(
                        modelSelectionState = "UNCONFIGURED",
                        activeModelRef = null,
                        activeModelProfileRef = null,
                        modelMinimumMemoryBytes = null,
                        runtimeState = "NOT_SELECTED",
                        runtimeCurrentness = "NOT_APPLICABLE",
                        runtimeReceiptPath = null,
                        artifactState = "NOT_SELECTED",
                        artifactTotalBytes = null,
                        artifactPresentBySizeBytes = null,
                        artifactMissingBytes = null,
                        artifactInvalidSizeBytes = null,
                    )

                is FileHereticModelConfigurationStore.Snapshot.Heretic -> {
                    val current = model.value
                    val artifacts = hooks.observeArtifacts(productHome)
                    require(!artifacts.effectsPerformed) {
                        "current-state artifact presence observation performed an effect"
                    }

                    ModelProjection(
                        modelSelectionState = current.state.wireValue,
                        activeModelRef = current.modelRef,
                        activeModelProfileRef = current.modelProfileRef,
                        modelMinimumMemoryBytes = HereticMacHostProfile.MinimumMemoryBytes,
                        runtimeState =
                            when (current.state) {
                                HereticModelConfiguration.State.EstablishmentPending ->
                                    "ESTABLISHMENT_PENDING"

                                HereticModelConfiguration.State.BoundProductModel ->
                                    "BOUND_PRODUCT_MODEL"
                            },
                        runtimeCurrentness = "CONFIGURATION_ONLY_NOT_REOBSERVED",
                        runtimeReceiptPath = current.runtimeReceiptPath,
                        artifactState = artifacts.state,
                        artifactTotalBytes = artifacts.totalBytes,
                        artifactPresentBySizeBytes = artifacts.presentBySizeBytes,
                        artifactMissingBytes = artifacts.missingBytes,
                        artifactInvalidSizeBytes = artifacts.invalidSizeBytes,
                    )
                }
            }

        return VexLifeProdServiceContract.CurrentStateProjection(
            schemaVersion = VexLifeProdServiceContract.CurrentStateProjectionSchema,
            observedAt = hooks.now().toString(),
            homePath = productHome.toString(),
            homeDisposition = home.state,
            homeRef = home.manifest.homeRef,
            deviceRef = home.installation.deviceRef,
            companionLineageRef = home.installation.companionLineageRef,
            platform = host.observation.platform,
            architecture = host.observation.architecture,
            chipModel = host.observation.chipModel,
            hostEligibilityState = host.state.name.uppercase(),
            hostEligibilityReason = host.reason,
            totalMemoryBytes = host.observation.totalMemoryBytes,
            modelMinimumMemoryBytes = modelProjection.modelMinimumMemoryBytes,
            freeStorageBytes = hooks.observeFreeStorageBytes(productHome),
            modelSelectionState = modelProjection.modelSelectionState,
            activeModelRef = modelProjection.activeModelRef,
            activeModelProfileRef = modelProjection.activeModelProfileRef,
            runtimeState = modelProjection.runtimeState,
            runtimeCurrentness = modelProjection.runtimeCurrentness,
            runtimeReceiptPath = modelProjection.runtimeReceiptPath,
            modelArtifactState = modelProjection.artifactState,
            modelArtifactTotalBytes = modelProjection.artifactTotalBytes,
            modelArtifactPresentBySizeBytes = modelProjection.artifactPresentBySizeBytes,
            modelArtifactMissingBytes = modelProjection.artifactMissingBytes,
            modelArtifactInvalidSizeBytes = modelProjection.artifactInvalidSizeBytes,
            effectsPerformed = false,
        )
    }

    private data class ModelProjection(
        val modelSelectionState: String,
        val activeModelRef: String?,
        val activeModelProfileRef: String?,
        val modelMinimumMemoryBytes: Long?,
        val runtimeState: String,
        val runtimeCurrentness: String,
        val runtimeReceiptPath: String?,
        val artifactState: String,
        val artifactTotalBytes: Long?,
        val artifactPresentBySizeBytes: Long?,
        val artifactMissingBytes: Long?,
        val artifactInvalidSizeBytes: Long?,
    )
}

// [VXG RealForever]
