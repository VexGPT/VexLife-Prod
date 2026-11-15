package vexlife.model

import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlinx.serialization.Serializable
import vexlife.home.VexLifeProdHomeFiles
import vexlife.local.VexLifeProdLocalSetupLease
import vexlife.logging.VexLogger
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.runtime.FileHereticModelConfigurationStore
import vexlife.model.heretic.runtime.HereticArtifactDelivery
import vexlife.model.heretic.runtime.HereticArtifactMaterialization
import vexlife.text.VexLifeText

/**
 * Settings → Models: the offered models as this Home holds them, and the person's model Controls.
 *
 * Effects and their owners:
 * - Use: FileHereticModelConfigurationStore.selectModel (the Home's choice; next start switches)
 * - Download: HereticArtifactMaterialization.materializeCurrent with acquisition, in the background
 * - Redownload: HereticArtifactMaterialization.remove, then the same download
 * - Remove: HereticArtifactMaterialization.remove (frees space; the model stays offered)
 *
 * Every file effect holds the Home's setup lease, so Setup and Settings never write model files at
 * once. One download runs at a time. Lawfulness is VexLifeModelsFamily's, re-resolved against a
 * fresh observation when each Control arrives; a Control that is not lawful now does nothing.
 *
 * This owner holds no runtime-start authority: a chosen model starts at the next Setup Start.
 */
class VexLifeModels(
    productHome: Path,
    private val servingModelRef: () -> String?,
    private val totalMemoryBytes: () -> Long?,
    private val acquirer: (onProgress: (Long) -> Unit) -> HereticArtifactMaterialization.Acquirer =
        { onProgress ->
            val delivered = mutableMapOf<String, Long>()
            HereticArtifactDelivery.Acquirer(
                onProgress = { member, progress ->
                    delivered[member.artifactRef] = progress.bytes
                    onProgress(delivered.values.sum())
                }
            )
        },
    private val now: () -> Instant = { Instant.now() },
) : AutoCloseable {
    private val home: Path = productHome.toAbsolutePath().normalize()

    private val executor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "vexlife-model-download").apply { isDaemon = true }
    }

    private data class Download(val handle: String, val baseBytes: Long, val totalBytes: Long)

    private val download = AtomicReference<Download?>(null)
    private val downloadedBytes = AtomicLong(0)
    private val lastOutcome = AtomicReference<Outcome?>(null)

    @Serializable
    data class Row(
        val handle: String,
        val label: String,
        val standing: String,
        val totalBytes: Long,
        val presentBytes: Long,
        val recommendedMemoryBytes: Long,
        val fitsThisMac: Boolean,
        val chosen: Boolean,
        val serving: Boolean,
        val downloadedBytes: Long?,
        val display: VexLifeModelsFamily.DisplayState,
    )

    @Serializable
    data class Outcome(
        val handle: String,
        val controlRef: String,
        val state: String,
        val message: VexLifeText,
        val at: String,
    )

    @Serializable
    data class Projection(
        val schemaVersion: String,
        val models: List<Row>,
        val totalMemoryBytes: Long?,
        val freeStorageBytes: Long?,
        val downloading: String?,
        /** The Home chose a model the running runtime does not serve: Setup's Start switches it. */
        val switchPending: Boolean,
        val lastOutcome: Outcome?,
        val effectsPerformed: Boolean,
    )

    fun projection(): Projection {
        val chosen = chosenModelRef()
        val serving = servingModelRef()
        val running = download.get()
        val memory = totalMemoryBytes()
        val rows =
            HereticModelCatalog.Entries.map { entry ->
                val presence = HereticArtifactMaterialization.observePresence(home, entry)
                val state = state(entry, presence, chosen, serving, running)
                Row(
                    handle = entry.handle,
                    label = entry.humanLabel,
                    standing = entry.standing.name,
                    totalBytes = entry.totalBytes,
                    presentBytes = presence.presentBySizeBytes,
                    recommendedMemoryBytes = entry.recommendedMemoryBytes,
                    fitsThisMac = HereticModelCatalog.fits(entry, memory),
                    chosen = state.chosen,
                    serving = state.serving,
                    downloadedBytes =
                        running
                            ?.takeIf { it.handle == entry.handle }
                            ?.let { it.baseBytes + downloadedBytes.get() },
                    display = VexLifeModelsFamily.displayStateFor(state),
                )
            }
        return Projection(
            schemaVersion = Schema,
            models = rows,
            totalMemoryBytes = memory,
            freeStorageBytes = runCatching { Files.getFileStore(home).usableSpace }.getOrNull(),
            downloading = running?.handle,
            switchPending = chosen != null && serving != null && chosen != serving,
            lastOutcome = lastOutcome.get(),
            effectsPerformed = false,
        )
    }

    /** One Control, re-resolved against a fresh observation; the projection after it. */
    fun control(controlRef: String, handle: String): Projection {
        val entry =
            requireNotNull(HereticModelCatalog.byHandle(handle)) { "model $handle is not offered" }
        val control =
            requireNotNull(VexLifeModelsFamily.controlForRef(controlRef)) {
                "model control $controlRef is not known"
            }
        val presence = HereticArtifactMaterialization.observePresence(home, entry)
        val state = state(entry, presence, chosenModelRef(), servingModelRef(), download.get())
        val act = VexLifeModelsFamily.resolve(state, control)
        if (act == null) {
            remember(
                entry,
                controlRef,
                "NOT_LAWFUL_NOW",
                VexLifeText("workspace.status.model-control-not-lawful"),
            )
            return projection()
        }
        VexLogger.record(
            VexLogger.Fact.ModelControlAccepted,
            mapOf("model" to handle, "controlRef" to controlRef),
        )
        when (act.effect) {
            VexLifeModelsFamily.Effect.ChooseModel -> {
                val manifest = requireNotNull(VexLifeProdHomeFiles.observeExisting(home)).manifest
                FileHereticModelConfigurationStore(home).selectModel(manifest, entry)
                VexLogger.record(
                    VexLogger.Fact.PersonModelChosen,
                    mapOf("model" to handle, "via" to "settings"),
                )
                remember(
                    entry,
                    controlRef,
                    "CHOSEN",
                    VexLifeText("workspace.status.model-chosen-restart"),
                )
            }
            VexLifeModelsFamily.Effect.AcquireModel ->
                startDownload(entry, controlRef, replace = false)
            VexLifeModelsFamily.Effect.ReplaceModel ->
                startDownload(entry, controlRef, replace = true)
            VexLifeModelsFamily.Effect.RemoveModel -> {
                val removal =
                    VexLifeProdLocalSetupLease.acquire(home).use {
                        HereticArtifactMaterialization.remove(home, entry)
                    }
                VexLogger.record(
                    VexLogger.Fact.ModelFilesRemoved,
                    mapOf(
                        "model" to handle,
                        "files" to removal.removedFiles.toString(),
                        "freedBytes" to removal.freedBytes.toString(),
                    ),
                )
                remember(
                    entry,
                    controlRef,
                    "REMOVED",
                    VexLifeText(
                        key = "workspace.status.model-removed",
                        gib = mapOf("freed" to removal.freedBytes),
                    ),
                )
            }
            null -> Unit
        }
        return projection()
    }

    private fun startDownload(
        entry: HereticModelCatalog.Entry,
        controlRef: String,
        replace: Boolean,
    ) {
        val base =
            if (replace) 0L
            else HereticArtifactMaterialization.observePresence(home, entry).presentBySizeBytes
        if (!download.compareAndSet(null, Download(entry.handle, base, entry.totalBytes))) return
        downloadedBytes.set(0)
        VexLogger.record(
            VexLogger.Fact.ModelDownloadStarted,
            mapOf("model" to entry.handle, "replace" to replace.toString()),
        )
        executor.execute {
            try {
                VexLifeProdLocalSetupLease.acquire(home).use {
                    if (replace) HereticArtifactMaterialization.remove(home, entry)
                    HereticArtifactMaterialization.materializeCurrent(
                        productHome = home,
                        allowAcquisition = true,
                        acquirer = acquirer { bytes -> downloadedBytes.set(bytes) },
                        model = entry,
                    )
                }
                VexLogger.record(
                    VexLogger.Fact.ModelDownloadCompleted,
                    mapOf("model" to entry.handle),
                )
                remember(
                    entry,
                    controlRef,
                    "DOWNLOADED",
                    VexLifeText("workspace.status.model-downloaded"),
                )
            } catch (error: Exception) {
                VexLogger.record(
                    VexLogger.Fact.ModelDownloadFailed,
                    mapOf("model" to entry.handle),
                    detail = error.javaClass.simpleName + ": " + (error.message ?: ""),
                )
                remember(
                    entry,
                    controlRef,
                    "FAILED",
                    VexLifeText.text(
                        "workspace.status.model-download-failed",
                        "reason" to (error.message ?: error.javaClass.simpleName),
                    ),
                )
            } finally {
                download.set(null)
            }
        }
    }

    private fun remember(
        entry: HereticModelCatalog.Entry,
        controlRef: String,
        state: String,
        message: VexLifeText,
    ) {
        lastOutcome.set(Outcome(entry.handle, controlRef, state, message, now().toString()))
    }

    private fun chosenModelRef(): String? = runCatching {
        val manifest =
            VexLifeProdHomeFiles.observeExisting(home)?.manifest ?: return@runCatching null
        when (val snapshot = FileHereticModelConfigurationStore(home).read(manifest)) {
            is FileHereticModelConfigurationStore.Snapshot.Heretic -> snapshot.value.modelRef
            is FileHereticModelConfigurationStore.Snapshot.Unconfigured -> null
        }
    }
        .getOrNull()

    private fun state(
        entry: HereticModelCatalog.Entry,
        presence: HereticArtifactMaterialization.PresenceObservation,
        chosen: String?,
        serving: String?,
        running: Download?,
    ) =
        VexLifeModelsFamily.State(
            handle = entry.handle,
            files =
                when (presence.state) {
                    "COMPLETE_BY_SIZE_NOT_HASHED" -> VexLifeModelsFamily.Files.PRESENT
                    "PARTIAL_BY_SIZE" -> VexLifeModelsFamily.Files.PARTIAL
                    "INVALID_SIZE" -> VexLifeModelsFamily.Files.DAMAGED
                    else -> VexLifeModelsFamily.Files.ABSENT
                },
            downloading = running?.handle == entry.handle,
            chosen = chosen == entry.modelRef,
            serving = serving == entry.modelRef,
            anyDownloadRunning = running != null,
        )

    /** Waits for a running download to finish (tests and orderly shutdown). */
    internal fun awaitIdle(timeoutMs: Long = 30_000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (download.get() != null && System.currentTimeMillis() < deadline) Thread.sleep(20)
    }

    override fun close() {
        executor.shutdownNow()
    }

    companion object {
        const val Schema = "vexlife-prod.models/v1"
    }
}

// [VXG RealForever]
