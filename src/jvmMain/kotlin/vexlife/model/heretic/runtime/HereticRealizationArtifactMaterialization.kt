package vexlife.model.heretic.runtime

import java.nio.file.Path
import vexlife.artifact.DirectHttpsArtifactDelivery
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticRuntimeRealization
import vexlife.platform.windows.VexLifeWindowsRuntimeMaterialization

/**
 * Exact model-artifact relationship for one runtime realization.
 *
 * This owner answers only which model bytes the resolved realization expects, whether those bytes are
 * present, and how the already-authorized model-only acquire/remove effect is delegated to the
 * rightful concrete materializer.
 *
 * It does not choose a model, choose a platform, start a runtime, acquire runtime binaries, or own
 * Product-service lifetime.
 */
object HereticRealizationArtifactMaterialization {
    enum class PresenceState {
        ABSENT,
        PARTIAL,
        PRESENT,
        DAMAGED,
    }

    data class Observation(
        val realizationRef: String,
        val modelRef: String,
        val modelPath: String,
        val state: PresenceState,
        val totalBytes: Long,
        val presentBytes: Long,
        val effectsPerformed: Boolean,
    ) {
        init {
            require(
                totalBytes >
                    0 &&
                    presentBytes in
                    0..totalBytes &&
                    !effectsPerformed,
            ) {
                "realization artifact observation is invalid"
            }
        }
    }

    data class Acquisition(
        val observation: Observation,
        val disposition: String,
    ) {
        init {
            require(
                observation.state ==
                    PresenceState.PRESENT,
            ) {
                "realization artifact acquisition requires present model bytes"
            }
        }
    }

    data class Removal(
        val realizationRef: String,
        val modelPath: String,
        val removedFiles: Int,
        val freedBytes: Long,
    )

    fun observe(
        productHome: Path,
        realization: HereticRuntimeRealization.Value,
        model: HereticModelCatalog.Entry =
            HereticModelCatalog.requireEntry(realization.modelRef),
    ): Observation =
        when (
            realization.target
        ) {
            HereticRuntimeRealization.MacArm64 -> {
                requireModelMatchesRealization(
                    model = model,
                    realization = realization,
                )
                val observed =
                    HereticArtifactMaterialization.observePresence(
                        productHome = productHome,
                        model = model,
                    )

                Observation(
                    realizationRef = realization.realizationRef,
                    modelRef = realization.modelRef,
                    modelPath = observed.modelPath,
                    state =
                        when (
                            observed.state
                        ) {
                            HereticArtifactMaterialization.PresenceState.Absent ->
                                PresenceState.ABSENT

                            HereticArtifactMaterialization.PresenceState.PartialBySize ->
                                PresenceState.PARTIAL

                            HereticArtifactMaterialization.PresenceState.CompleteBySizeNotHashed ->
                                PresenceState.PRESENT

                            HereticArtifactMaterialization.PresenceState.InvalidSize ->
                                PresenceState.DAMAGED
                        },
                    totalBytes = observed.totalBytes,
                    presentBytes = observed.presentBySizeBytes,
                    effectsPerformed = false,
                )
            }

            HereticRuntimeRealization.WindowsX64 -> {
                val observed =
                    VexLifeWindowsRuntimeMaterialization.observe(
                        productHome = productHome,
                        realization = realization,
                    )
                val totalBytes =
                    requireNotNull(
                        realization.artifact.expectedPrimaryBytes,
                    ) {
                        "Windows realization model artifact requires exact byte length"
                    }

                Observation(
                    realizationRef = realization.realizationRef,
                    modelRef = realization.modelRef,
                    modelPath = observed.modelPath,
                    state =
                        when (
                            observed.modelState
                        ) {
                            VexLifeWindowsRuntimeMaterialization.FileState.ABSENT ->
                                PresenceState.ABSENT

                            VexLifeWindowsRuntimeMaterialization.FileState.VERIFIED ->
                                PresenceState.PRESENT

                            VexLifeWindowsRuntimeMaterialization.FileState.INVALID ->
                                PresenceState.DAMAGED
                        },
                    totalBytes = totalBytes,
                    presentBytes =
                        if (
                            observed.modelState ==
                                VexLifeWindowsRuntimeMaterialization.FileState.VERIFIED
                        ) {
                            totalBytes
                        } else {
                            0L
                        },
                    effectsPerformed = false,
                )
            }

            else ->
                error(
                    "no model-artifact materialization is admitted for realization target " +
                        HereticRuntimeRealization.targetRef(
                            realization.target,
                        ),
                )
        }

    fun ensureModel(
        productHome: Path,
        realization: HereticRuntimeRealization.Value,
        allowAcquisition: Boolean,
        macAcquirer: HereticArtifactMaterialization.Acquirer,
        model: HereticModelCatalog.Entry =
            HereticModelCatalog.requireEntry(realization.modelRef),
        windowsTransport: DirectHttpsArtifactDelivery.Transport =
            DirectHttpsArtifactDelivery.JdkTransport,
        onProgress: (Long) -> Unit = {},
    ): Acquisition =
        when (
            realization.target
        ) {
            HereticRuntimeRealization.MacArm64 -> {
                requireModelMatchesRealization(
                    model = model,
                    realization = realization,
                )
                val result =
                    HereticArtifactMaterialization.materializeCurrent(
                        productHome = productHome,
                        allowAcquisition = allowAcquisition,
                        acquirer = macAcquirer,
                        model = model,
                    )
                Acquisition(
                    observation =
                        observe(
                            productHome = productHome,
                            realization = realization,
                            model = model,
                        ),
                    disposition =
                        if (
                            result.acquisitionPerformed
                        ) {
                            "MATERIALIZED_MAC_MODEL"
                        } else {
                            "REUSED_MAC_MODEL"
                        },
                )
            }

            HereticRuntimeRealization.WindowsX64 -> {
                val result =
                    VexLifeWindowsRuntimeMaterialization.ensureModelArtifact(
                        productHome = productHome,
                        allowAcquisition = allowAcquisition,
                        realization = realization,
                        transport = windowsTransport,
                        onProgress = {
                            progress ->
                            onProgress(
                                progress.bytes,
                            )
                        },
                    )
                Acquisition(
                    observation =
                        observe(
                            productHome = productHome,
                            realization = realization,
                        ),
                    disposition = result.disposition,
                )
            }

            else ->
                error(
                    "no model-artifact acquisition is admitted for realization target " +
                        HereticRuntimeRealization.targetRef(
                            realization.target,
                        ),
                )
        }

    private fun requireModelMatchesRealization(
        model: HereticModelCatalog.Entry,
        realization: HereticRuntimeRealization.Value,
    ) {
        require(
            model.modelRef == realization.modelRef &&
                model.identity.modelProfileRef == realization.modelProfileRef &&
                model.identity.artifactSetRef == realization.artifact.artifactRef &&
                model.identity.artifactContentSetSha256 == realization.artifact.contentSetSha256,
        ) {
            "model offering does not match the exact runtime realization"
        }
    }

    fun removeModel(
        productHome: Path,
        realization: HereticRuntimeRealization.Value,
        model: HereticModelCatalog.Entry =
            HereticModelCatalog.requireEntry(realization.modelRef),
    ): Removal =
        when (
            realization.target
        ) {
            HereticRuntimeRealization.MacArm64 -> {
                requireModelMatchesRealization(
                    model = model,
                    realization = realization,
                )
                val removed =
                    HereticArtifactMaterialization.remove(
                        productHome = productHome,
                        model = model,
                    )
                Removal(
                    realizationRef = realization.realizationRef,
                    modelPath = removed.modelPath,
                    removedFiles = removed.removedFiles,
                    freedBytes = removed.freedBytes,
                )
            }

            HereticRuntimeRealization.WindowsX64 -> {
                val removed =
                    VexLifeWindowsRuntimeMaterialization.removeModelArtifact(
                        productHome = productHome,
                        realization = realization,
                    )
                Removal(
                    realizationRef = realization.realizationRef,
                    modelPath = removed.modelPath,
                    removedFiles = removed.removedFiles,
                    freedBytes = removed.freedBytes,
                )
            }

            else ->
                error(
                    "no model-artifact removal is admitted for realization target " +
                        HereticRuntimeRealization.targetRef(
                            realization.target,
                        ),
                )
        }
}

// [VXG RealForever]
