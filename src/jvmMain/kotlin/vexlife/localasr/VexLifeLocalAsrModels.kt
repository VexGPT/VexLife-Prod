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

    private data class Observed(
        val artifact: VexLifeLocalAsrArtifacts.Artifact,
        val presence: VexLifeLocalAsrArtifacts.Presence,
        val familyState: VexLifeLocalAsrModelsFamily.State,
    )

    private fun observe(
        modelRef: String,
    ): Observed {
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
            VexLifeLocalAsrProviderRealizationResolver.current()
        val providerObservation =
            providerRealization.observeProvider(
                productHome =
                    home,
            )
        return Observed(
            artifact =
                artifact,
            presence =
                presence,
            familyState =
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
                ),
        )
    }

    fun status(
        modelRef: String,
    ): Status {
        val observed =
            observe(
                modelRef,
            )
        return Status(
            artifactState =
                observed.presence.state,
            presentBytes =
                observed.presence.presentBytes,
            downloadedBytes =
                downloadedBytes
                    .get()
                    .takeIf {
                        observed.familyState.downloading
                    },
            controls =
                VexLifeLocalAsrModelsFamily.lawfulControls(
                    observed.familyState,
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
        val observed =
            observe(
                modelRef,
            )
        val control =
            requireNotNull(
                VexLifeLocalAsrModelsFamily.controlForRef(
                    controlRef,
                ),
            ) {
                "unknown local ASR model control " +
                    controlRef
            }
        val act =
            requireNotNull(
                VexLifeLocalAsrModelsFamily.resolve(
                    currentState =
                        observed.familyState,
                    control =
                        control,
                ),
            ) {
                "local ASR model control is not lawful now"
            }
        when (
            val effect =
                requireNotNull(
                    act.effect,
                ) {
                    "local ASR model control did not form an Effect"
                }
        ) {
            VexLifeLocalAsrModelsFamily.SelectSpeechModel -> {
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

            is VexLifeLocalAsrModelsFamily.AcquireSpeechModel ->
                startDownload(
                    artifact =
                        observed.artifact,
                    replace =
                        effect.replace,
                )

            VexLifeLocalAsrModelsFamily.AcquireSpeechProvider ->
                installProvider()

            VexLifeLocalAsrModelsFamily.RemoveSpeechModel -> {
                val removal =
                    VexLifeProdLocalSetupLease.acquire(
                        home,
                    )
                        .use {
                            VexLifeLocalAsrArtifacts.remove(
                                home,
                                observed.artifact,
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
            VexLifeLocalAsrProviderRealizationResolver.current()
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
                    "providerSource" to realization.providerSourceRef,
                ),
        )
        try {
            VexLifeProdLocalSetupLease.acquire(
                home,
            )
                .use {
                    realization.ensureProvider(
                        productHome =
                            home,
                        allowRepair =
                            true,
                    )
                }
            VexLogger.record(
                VexLogger.Fact.LocalAsrProviderDownloadCompleted,
                refs =
                    mapOf(
                        "realizationRef" to realization.realizationRef,
                        "providerSource" to realization.providerSourceRef,
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
                        "providerSource" to realization.providerSourceRef,
                        "failureType" to error.javaClass.simpleName,
                    ),
            )
            throw error
        }
    }

    private fun ensureProviderIfSupported() {
        val realization =
            VexLifeLocalAsrProviderRealizationResolver.current()
        if (
            !realization.acquisitionSupported
        ) {
            return
        }
        val before =
            realization.observeProvider(
                productHome =
                    home,
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
                    "providerSource" to realization.providerSourceRef,
                ),
        )
        try {
            realization.ensureProvider(
                productHome =
                    home,
            )
            VexLogger.record(
                VexLogger.Fact.LocalAsrProviderDownloadCompleted,
                refs =
                    mapOf(
                        "realizationRef" to realization.realizationRef,
                        "providerSource" to realization.providerSourceRef,
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
                        "providerSource" to realization.providerSourceRef,
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
