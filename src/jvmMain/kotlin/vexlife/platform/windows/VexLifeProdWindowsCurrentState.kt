package vexlife.platform.windows

import com.sun.management.OperatingSystemMXBean
import java.lang.management.ManagementFactory
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import vexlife.home.VexLifeProdHomeFiles
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticRuntimeRealization
import vexlife.model.heretic.runtime.FileHereticModelConfigurationStore
import vexlife.model.heretic.runtime.HereticRealizationArtifactMaterialization
import vexlife.service.VexLifeProdServiceContract

/**
 * No-effect Windows current-state projection for the shared Product service.
 *
 * This is the Windows peer of the existing Mac projector. It consumes the exact Home binding and
 * realization-aware artifact owner; it does not start/recover a runtime or mutate Product state.
 */
internal object VexLifeProdWindowsCurrentState {
    fun observe(
        productHome: Path,
    ): VexLifeProdServiceContract.CurrentStateProjection {
        require(
            productHome.isAbsolute,
        ) {
            "Windows current state requires one absolute selected Vex Home"
        }
        val selectedHome =
            productHome
                .normalize()
        val host =
            requireResolvedHost()
        val location =
            VexLifeProdHomeFiles.observeLocation(
                selectedHome,
            )
        require(
            Path.of(
                location.productHome,
            )
                .normalize() ==
                selectedHome,
        ) {
            "Windows current-state Home observer changed the selected coordinate"
        }

        if (
            location !is
                VexLifeProdHomeFiles.Location.Existing
        ) {
            return VexLifeProdServiceContract.CurrentStateProjection(
                schemaVersion =
                    VexLifeProdServiceContract.CurrentStateProjectionSchema,
                observedAt =
                    Instant
                        .now()
                        .toString(),
                homePath =
                    selectedHome.toString(),
                homeDisposition =
                    location.disposition.wireValue,
                homeRef = null,
                deviceRef = null,
                companionLineageRef = null,
                platform = "windows",
                architecture =
                    host.nativeArchitecture.name
                        .lowercase(),
                chipModel =
                    System.getenv(
                        "PROCESSOR_IDENTIFIER",
                    ),
                hostEligibilityState = "ELIGIBLE",
                hostEligibilityReason = null,
                totalMemoryBytes =
                    totalMemoryBytes(),
                modelMinimumMemoryBytes = null,
                freeStorageBytes =
                    freeStorageBytes(
                        selectedHome,
                    ),
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

        val home =
            location.home
        val snapshot =
            FileHereticModelConfigurationStore(
                selectedHome,
            )
                .read(
                    home.manifest,
                )

        val modelProjection =
            when (
                snapshot
            ) {
                is FileHereticModelConfigurationStore.Snapshot.Unconfigured ->
                    ModelProjection.unconfigured()

                is FileHereticModelConfigurationStore.Snapshot.Heretic -> {
                    val value =
                        snapshot.value
                    val model =
                        HereticModelConfiguration.selected(
                            value,
                        )
                    val realization =
                        HereticRuntimeRealization.resolve(
                            modelRef = model.modelRef,
                            target = host.target,
                        )
                    val artifact =
                        realization
                            ?.let {
                                HereticRealizationArtifactMaterialization.observe(
                                    productHome = selectedHome,
                                    realization = it,
                                )
                            }

                    ModelProjection(
                        modelSelectionState =
                            value.state.wireValue,
                        activeModelRef =
                            value.modelRef,
                        activeModelProfileRef =
                            value.modelProfileRef,
                        modelMinimumMemoryBytes =
                            model.recommendedMemoryBytes,
                        runtimeState =
                            when (
                                value.state
                            ) {
                                HereticModelConfiguration.State.EstablishmentPending ->
                                    "ESTABLISHMENT_PENDING"

                                HereticModelConfiguration.State.BoundProductModel ->
                                    "BOUND_PRODUCT_MODEL"
                            },
                        runtimeCurrentness =
                            "CONFIGURATION_ONLY_NOT_REOBSERVED",
                        runtimeReceiptPath =
                            value.runtimeReceiptPath,
                        artifactState =
                            artifact
                                ?.state
                                ?.let(::serviceArtifactState)
                                ?: "ABSENT",
                        artifactTotalBytes =
                            artifact?.totalBytes ?: model.totalBytes,
                        artifactPresentBySizeBytes =
                            artifact?.presentBytes ?: 0L,
                        artifactMissingBytes =
                            artifact?.let {
                                if (
                                    it.state ==
                                        HereticRealizationArtifactMaterialization.PresenceState.ABSENT ||
                                    it.state ==
                                        HereticRealizationArtifactMaterialization.PresenceState.PARTIAL
                                ) {
                                    it.totalBytes - it.presentBytes
                                } else {
                                    0L
                                }
                            } ?: model.totalBytes,
                        artifactInvalidSizeBytes =
                            artifact?.let {
                                if (
                                    it.state ==
                                        HereticRealizationArtifactMaterialization.PresenceState.DAMAGED
                                ) {
                                    it.totalBytes
                                } else {
                                    0L
                                }
                            } ?: 0L,
                    )
                }
            }

        return VexLifeProdServiceContract.CurrentStateProjection(
            schemaVersion =
                VexLifeProdServiceContract.CurrentStateProjectionSchema,
            observedAt =
                Instant
                    .now()
                    .toString(),
            homePath =
                home.productHome,
            homeDisposition =
                home.state,
            homeRef =
                home.manifest.homeRef,
            deviceRef =
                home.installation.deviceRef,
            companionLineageRef =
                home.installation.companionLineageRef,
            platform = "windows",
            architecture =
                host.nativeArchitecture.name
                    .lowercase(),
            chipModel =
                System.getenv(
                    "PROCESSOR_IDENTIFIER",
                ),
            hostEligibilityState = "ELIGIBLE",
            hostEligibilityReason = null,
            totalMemoryBytes =
                totalMemoryBytes(),
            modelMinimumMemoryBytes =
                modelProjection.modelMinimumMemoryBytes,
            freeStorageBytes =
                freeStorageBytes(
                    selectedHome,
                ),
            modelSelectionState =
                modelProjection.modelSelectionState,
            activeModelRef =
                modelProjection.activeModelRef,
            activeModelProfileRef =
                modelProjection.activeModelProfileRef,
            runtimeState =
                modelProjection.runtimeState,
            runtimeCurrentness =
                modelProjection.runtimeCurrentness,
            runtimeReceiptPath =
                modelProjection.runtimeReceiptPath,
            modelArtifactState =
                modelProjection.artifactState,
            modelArtifactTotalBytes =
                modelProjection.artifactTotalBytes,
            modelArtifactPresentBySizeBytes =
                modelProjection.artifactPresentBySizeBytes,
            modelArtifactMissingBytes =
                modelProjection.artifactMissingBytes,
            modelArtifactInvalidSizeBytes =
                modelProjection.artifactInvalidSizeBytes,
            effectsPerformed = false,
        )
    }

    internal fun serviceArtifactState(
        state: HereticRealizationArtifactMaterialization.PresenceState,
    ): String =
        when (state) {
            HereticRealizationArtifactMaterialization.PresenceState.ABSENT -> "ABSENT"
            HereticRealizationArtifactMaterialization.PresenceState.PARTIAL -> "PARTIAL_BY_SIZE"
            HereticRealizationArtifactMaterialization.PresenceState.PRESENT ->
                "COMPLETE_BY_SIZE_NOT_HASHED"
            HereticRealizationArtifactMaterialization.PresenceState.DAMAGED -> "INVALID_SIZE"
        }

    fun totalMemoryBytes(): Long? =
        runCatching {
                (
                    ManagementFactory
                        .getOperatingSystemMXBean() as?
                        OperatingSystemMXBean
                )
                    ?.totalMemorySize
                    ?.takeIf {
                        it >=
                            0
                    }
            }
            .getOrNull()

    private fun requireResolvedHost():
        VexLifeWindowsHostObservation.Result.Resolved =
        when (
            val observed =
                VexLifeWindowsHostObservation.current()
        ) {
            is VexLifeWindowsHostObservation.Result.Resolved -> {
                require(
                    observed.target ==
                        HereticRuntimeRealization.WindowsX64,
                ) {
                    "Windows Product service currently requires Windows x64"
                }
                observed
            }

            is VexLifeWindowsHostObservation.Result.UnsupportedHost ->
                error(
                    "Windows Product current state requires a Windows host",
                )

            is VexLifeWindowsHostObservation.Result.UnsupportedArchitecture ->
                error(
                    "Windows Product current state requires one admitted architecture",
                )
        }

    private fun freeStorageBytes(
        productHome: Path,
    ): Long? =
        runCatching {
                Files
                    .getFileStore(
                        productHome,
                    )
                    .usableSpace
            }
            .getOrNull()

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
    ) {
        companion object {
            fun unconfigured() =
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
        }
    }
}

// [VXG RealForever]
