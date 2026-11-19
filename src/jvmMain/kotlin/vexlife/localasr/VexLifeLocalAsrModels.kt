package vexlife.localasr

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import vexlife.local.VexLifeProdLocalSetupLease
import vexlife.logging.VexLogger

/** Settings realization for downloadable local speech-recognition model files. */
internal class VexLifeLocalAsrModels(
    productHome: Path,
    private val selectionStore: VexLifeLocalAsrSelectionStore,
) : AutoCloseable {
    data class Status(
        val artifactState: String,
        val presentBytes: Long,
        val downloadedBytes: Long?,
        val controls: List<String>,
    )

    private data class Download(
        val modelRef: String,
        val replace: Boolean,
    )

    private val home =
        productHome
            .toAbsolutePath()
            .normalize()

    private val executor: ExecutorService =
        Executors.newSingleThreadExecutor {
            runnable ->
            Thread(
                runnable,
                "vexlife-local-asr-model-download",
            ).apply {
                isDaemon =
                    true
            }
        }

    private val download =
        AtomicReference<Download?>(
            null,
        )
    private val downloadedBytes =
        AtomicLong(
            0,
        )

    fun status(
        modelRef: String,
    ): Status {
        val artifact =
            VexLifeLocalAsrArtifacts.forModelRef(
                modelRef,
            )
        val presence =
            VexLifeLocalAsrArtifacts.observe(
                home,
                artifact,
            )
        val active =
            download.get()
        val selected =
            selectionStore.read()
                .modelRef ==
                modelRef
        val providerRealization =
            VexLifeLocalAsrProviderRealization.current()
        val providerObservation =
            VexLifeLocalAsrProviderMaterialization.observe(
                productHome =
                    home,
                realization =
                    providerRealization,
            )
        val state =
            VexLifeLocalAsrModelsFamily.State(
                artifactState =
                    presence.state,
                selected =
                    selected,
                downloading =
                    active?.modelRef ==
                    modelRef,
                anyDownloadRunning =
                    active !=
                    null,
                providerCurrent =
                    providerObservation.ready,
                providerAcquisitionSupported =
                    providerRealization.acquisitionSupported,
            )
        return Status(
            artifactState =
                presence.state,
            presentBytes =
                presence.presentBytes,
            downloadedBytes =
                downloadedBytes
                    .get()
                    .takeIf {
                        state.downloading
                    },
            controls =
                VexLifeLocalAsrModelsFamily.lawfulControls(
                    state,
                ),
        )
    }

    fun freeStorageBytes(): Long? =
        runCatching {
            Files.getFileStore(
                home,
            )
                .usableSpace
        }
            .getOrNull()

    fun control(
        controlRef: String,
        modelRef: String,
    ) {
        val artifact =
            VexLifeLocalAsrArtifacts.forModelRef(
                modelRef,
            )
        val status =
            status(
                modelRef,
            )
        require(
            controlRef in
                status.controls,
        ) {
            "local ASR model control is not lawful now"
        }

        when (controlRef) {
            VexLifeLocalAsrModelsFamily.ControlUse -> {
                val option =
                    requireNotNull(
                        VexLifeLocalAsrSelection.Options.firstOrNull {
                            it.modelRef ==
                                modelRef
                        },
                    )
                selectionStore.write(
                    VexLifeLocalAsrSelection.Value(
                        schemaVersion =
                            VexLifeLocalAsrSelection.Schema,
                        providerRef =
                            option.providerRef,
                        modelRef =
                            option.modelRef,
                    ),
                )
                VexLogger.record(
                    VexLogger.Fact.LocalAsrSelectionSaved,
                    refs =
                        mapOf(
                            "providerRef" to option.providerRef,
                            "modelRef" to option.modelRef,
                        ),
                )
            }

            VexLifeLocalAsrModelsFamily.ControlDownload ->
                startDownload(
                    artifact =
                        artifact,
                    replace =
                        false,
                )

            VexLifeLocalAsrModelsFamily.ControlRedownload ->
                startDownload(
                    artifact =
                        artifact,
                    replace =
                        true,
                )

            VexLifeLocalAsrModelsFamily.ControlInstallProvider -> {
                installProvider()
            }

            VexLifeLocalAsrModelsFamily.ControlRemove -> {
                val removal =
                    VexLifeProdLocalSetupLease.acquire(
                        home,
                    )
                        .use {
                            VexLifeLocalAsrArtifacts.remove(
                                home,
                                artifact,
                            )
                        }
                VexLogger.record(
                    VexLogger.Fact.LocalAsrModelFilesRemoved,
                    refs =
                        mapOf(
                            "modelRef" to modelRef,
                            "files" to removal.removedFiles.toString(),
                            "freedBytes" to removal.freedBytes.toString(),
                        ),
                )
            }

            else ->
                error(
                    "unknown local ASR model control " +
                        controlRef,
                )
        }
    }

    private fun startDownload(
        artifact: VexLifeLocalAsrArtifacts.Artifact,
        replace: Boolean,
    ) {
        if (
            !download.compareAndSet(
                null,
                Download(
                    modelRef =
                        artifact.modelRef,
                    replace =
                        replace,
                ),
            )
        ) {
            return
        }
        downloadedBytes.set(
            0,
        )
        VexLogger.record(
            VexLogger.Fact.LocalAsrModelDownloadStarted,
            refs =
                mapOf(
                    "modelRef" to artifact.modelRef,
                    "replace" to replace.toString(),
                ),
        )
        executor.execute {
            var modelComplete =
                false
            try {
                VexLifeProdLocalSetupLease.acquire(
                    home,
                )
                    .use {
                        if (
                            replace
                        ) {
                            VexLifeLocalAsrArtifacts.remove(
                                home,
                                artifact,
                            )
                        }
                        VexLifeLocalAsrArtifacts.acquire(
                            productHome =
                                home,
                            artifact =
                                artifact,
                            onProgress = {
                                bytes ->
                                downloadedBytes.set(
                                    bytes,
                                )
                            },
                        )
                        modelComplete =
                            true
                        VexLogger.record(
                            VexLogger.Fact.LocalAsrModelDownloadCompleted,
                            refs =
                                mapOf(
                                    "modelRef" to artifact.modelRef,
                                ),
                        )
                        ensureProviderIfSupported()
                    }
            } catch (
                error: Exception,
            ) {
                if (
                    !modelComplete
                ) {
                    VexLogger.record(
                        VexLogger.Fact.LocalAsrModelDownloadFailed,
                        refs =
                            mapOf(
                                "modelRef" to artifact.modelRef,
                                "failureType" to error.javaClass.simpleName,
                            ),
                    )
                }
            } finally {
                download.set(
                    null,
                )
                downloadedBytes.set(
                    0,
                )
            }
        }
    }

    private fun installProvider() {
        val realization =
            VexLifeLocalAsrProviderRealization.current()
        require(
            realization.acquisitionSupported,
        ) {
            "automatic local ASR provider acquisition is not available on this platform"
        }
        VexLogger.record(
            VexLogger.Fact.LocalAsrProviderDownloadStarted,
            refs =
                mapOf(
                    "realizationRef" to realization.realizationRef,
                    "providerSource" to VexLifeLocalAsrProviderRealization.ProviderSourceSha,
                ),
        )
        try {
            VexLifeProdLocalSetupLease.acquire(
                home,
            )
                .use {
                    VexLifeLocalAsrProviderMaterialization.ensure(
                        productHome =
                            home,
                        realization =
                            realization,
                    )
                }
            VexLogger.record(
                VexLogger.Fact.LocalAsrProviderDownloadCompleted,
                refs =
                    mapOf(
                        "realizationRef" to realization.realizationRef,
                        "providerSource" to VexLifeLocalAsrProviderRealization.ProviderSourceSha,
                    ),
            )
        } catch (
            error: Exception,
        ) {
            VexLogger.record(
                VexLogger.Fact.LocalAsrProviderDownloadFailed,
                refs =
                    mapOf(
                        "realizationRef" to realization.realizationRef,
                        "providerSource" to VexLifeLocalAsrProviderRealization.ProviderSourceSha,
                        "failureType" to error.javaClass.simpleName,
                    ),
            )
            throw error
        }
    }

    private fun ensureProviderIfSupported() {
        val realization =
            VexLifeLocalAsrProviderRealization.current()
        if (
            !realization.acquisitionSupported
        ) {
            return
        }
        val before =
            VexLifeLocalAsrProviderMaterialization.observe(
                productHome =
                    home,
                realization =
                    realization,
            )
        if (
            before.ready
        ) {
            return
        }
        VexLogger.record(
            VexLogger.Fact.LocalAsrProviderDownloadStarted,
            refs =
                mapOf(
                    "realizationRef" to realization.realizationRef,
                    "providerSource" to VexLifeLocalAsrProviderRealization.ProviderSourceSha,
                ),
        )
        try {
            VexLifeLocalAsrProviderMaterialization.ensure(
                productHome =
                    home,
                realization =
                    realization,
            )
            VexLogger.record(
                VexLogger.Fact.LocalAsrProviderDownloadCompleted,
                refs =
                    mapOf(
                        "realizationRef" to realization.realizationRef,
                        "providerSource" to VexLifeLocalAsrProviderRealization.ProviderSourceSha,
                    ),
            )
        } catch (
            error: Exception,
        ) {
            VexLogger.record(
                VexLogger.Fact.LocalAsrProviderDownloadFailed,
                refs =
                    mapOf(
                        "realizationRef" to realization.realizationRef,
                        "providerSource" to VexLifeLocalAsrProviderRealization.ProviderSourceSha,
                        "failureType" to error.javaClass.simpleName,
                    ),
            )
            throw error
        }
    }

    override fun close() {
        executor.shutdownNow()
    }
}

// [VXG RealForever]
