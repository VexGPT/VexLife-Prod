package vexlife.model

import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlinx.serialization.SerialName
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
 * - Use: persist the Home's exact choice, then request runtime convergence through [ModelSwitchRealization].
 * - Download: HereticArtifactMaterialization.materializeCurrent with acquisition, in the background.
 * - Redownload: HereticArtifactMaterialization.remove, then the same download.
 * - Remove: HereticArtifactMaterialization.remove (frees space; the model stays offered).
 *
 * The Family owns what Use means. [ModelSwitchRealization] is an injected realization seam:
 * Product construction delegates the effect to LocalSetup/runtime ownership and updates serving truth only after
 * the target runtime is qualified. This class never kills/spawns a model process itself.
 *
 * Every artifact effect holds the Home's setup lease, so Setup and Settings never write model files at
 * once. One background model operation runs at a time. Lawfulness is VexLifeModelsFamily's,
 * re-resolved against fresh State when each Control arrives.
 */
class VexLifeModels(
    productHome: Path,
    private val servingModelRef: () -> String?,
    private val totalMemoryBytes: () -> Long?,
    private val modelSwitchRealization: ModelSwitchRealization? = null,
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

    fun interface ModelSwitchRealization {
        fun switch(
            entry: HereticModelCatalog.Entry,
            observe: (SwitchPhase) -> Unit,
        )
    }

    @Serializable
    enum class SwitchPhase {
        @SerialName("REQUESTED")
        REQUESTED,

        @SerialName("WAITING_FOR_CURRENT_TURN")
        WAITING_FOR_CURRENT_TURN,

        @SerialName("PREPARING_TARGET")
        PREPARING_TARGET,

        @SerialName("SWITCHING_RUNTIME")
        SWITCHING_RUNTIME,
    }

    @Serializable
    data class SwitchProjection(
        val handle: String,
        val label: String,
        val phase: SwitchPhase,
        val message: VexLifeText,
        val startedAt: String,
    )

    private data class Download(val handle: String, val baseBytes: Long, val totalBytes: Long)

    private val download = AtomicReference<Download?>(null)
    private val switching = AtomicReference<SwitchProjection?>(null)
    private val downloadedBytes = AtomicLong(0)
    private val lastOutcome = AtomicReference<Outcome?>(null)

    @Serializable
    data class Row(
        val handle: String,
        val label: String,
        val standing: HereticModelCatalog.Standing,
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
    enum class OutcomeState {
        @SerialName("NOT_LAWFUL_NOW")
        NOT_LAWFUL_NOW,

        @SerialName("SWITCHING")
        SWITCHING,

        @SerialName("SWITCHED")
        SWITCHED,

        @SerialName("FAILED")
        FAILED,

        @SerialName("DOWNLOADED")
        DOWNLOADED,

        @SerialName("REMOVED")
        REMOVED,
    }

    @Serializable
    data class Outcome(
        val handle: String,
        val controlRef: String,
        val state: OutcomeState,
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
        /** One accepted Use is converging the serving runtime through the rightful runtime owner. */
        val switching: SwitchProjection?,
        /** Durable selected model differs from the current qualified serving Session. */
        val switchPending: Boolean,
        val lastOutcome: Outcome?,
        val effectsPerformed: Boolean,
    )

    fun projection(): Projection {
        val chosen = chosenModelRef()
        val serving = servingModelRef()
        val running = download.get()
        val currentSwitch = switching.get()
        val memory = totalMemoryBytes()
        val rows =
            HereticModelCatalog.Entries.map { entry ->
                val presence = HereticArtifactMaterialization.observePresence(home, entry)
                val state =
                    state(
                        entry = entry,
                        presence = presence,
                        chosen = chosen,
                        serving = serving,
                        running = running,
                        currentSwitch = currentSwitch,
                    )
                Row(
                    handle = entry.handle,
                    label = entry.humanLabel,
                    standing = entry.standing,
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
            switching = currentSwitch,
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
        val state =
            state(
                entry = entry,
                presence = presence,
                chosen = chosenModelRef(),
                serving = servingModelRef(),
                running = download.get(),
                currentSwitch = switching.get(),
            )
        val act = VexLifeModelsFamily.resolve(state, control)
        if (act == null) {
            remember(
                entry,
                controlRef,
                OutcomeState.NOT_LAWFUL_NOW,
                VexLifeText("workspace.status.model-control-not-lawful"),
            )
            return projection()
        }
        VexLogger.record(
            VexLogger.Fact.ModelControlAccepted,
            mapOf("model" to handle, "controlRef" to controlRef),
        )
        when (act.effect) {
            VexLifeModelsFamily.Effect.SwitchModel -> {
                val manifest = requireNotNull(VexLifeProdHomeFiles.observeExisting(home)).manifest
                if (!state.chosen) {
                    FileHereticModelConfigurationStore(home).selectModel(manifest, entry)
                    VexLogger.record(
                        VexLogger.Fact.PersonModelChosen,
                        mapOf("model" to handle, "via" to "settings"),
                    )
                }
                startSwitch(
                    entry = entry,
                    controlRef = controlRef,
                )
            }
            VexLifeModelsFamily.Effect.AcquireArtifacts ->
                startDownload(entry, controlRef, replace = false)
            VexLifeModelsFamily.Effect.ReplaceArtifacts ->
                startDownload(entry, controlRef, replace = true)
            VexLifeModelsFamily.Effect.RemoveArtifacts -> {
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
                    OutcomeState.REMOVED,
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

    private fun startSwitch(
        entry: HereticModelCatalog.Entry,
        controlRef: String,
    ) {
        val realization =
            requireNotNull(
                modelSwitchRealization,
            ) {
                "runtime switch realization is unavailable"
            }
        val startedAt = now().toString()
        val initial =
            SwitchProjection(
                handle = entry.handle,
                label = entry.humanLabel,
                phase = SwitchPhase.REQUESTED,
                message = switchMessage(SwitchPhase.REQUESTED, entry),
                startedAt = startedAt,
            )
        check(
            switching.compareAndSet(
                null,
                initial,
            ),
        ) {
            "another model switch is already active"
        }
        remember(
            entry,
            controlRef,
            OutcomeState.SWITCHING,
            initial.message,
        )
        executor.execute {
            try {
                realization.switch(
                    entry,
                ) {
                    phase ->
                    switching.set(
                        SwitchProjection(
                            handle = entry.handle,
                            label = entry.humanLabel,
                            phase = phase,
                            message = switchMessage(phase, entry),
                            startedAt = startedAt,
                        ),
                    )
                }
                require(
                    servingModelRef() ==
                        entry.modelRef,
                ) {
                    "model switch completed without the target model becoming the serving Session"
                }
                remember(
                    entry,
                    controlRef,
                    OutcomeState.SWITCHED,
                    VexLifeText.text(
                        "workspace.status.model-switched",
                        "model" to entry.humanLabel,
                    ),
                )
            } catch (
                error: Exception,
            ) {
                remember(
                    entry,
                    controlRef,
                    OutcomeState.FAILED,
                    VexLifeText.text(
                        "workspace.status.model-switch-failed",
                        "model" to entry.humanLabel,
                        "reason" to (error.message ?: error.javaClass.simpleName),
                    ),
                )
            } finally {
                switching.set(
                    null,
                )
            }
        }
    }

    private fun switchMessage(
        phase: SwitchPhase,
        entry: HereticModelCatalog.Entry,
    ): VexLifeText =
        VexLifeText.text(
            when (phase) {
                SwitchPhase.REQUESTED ->
                    "workspace.status.model-switch-requested"

                SwitchPhase.WAITING_FOR_CURRENT_TURN ->
                    "workspace.status.model-switch-waiting"

                SwitchPhase.PREPARING_TARGET ->
                    "workspace.status.model-switch-preparing"

                SwitchPhase.SWITCHING_RUNTIME ->
                    "workspace.status.model-switch-runtime"
            },
            "model" to entry.humanLabel,
        )

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
                    OutcomeState.DOWNLOADED,
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
                    OutcomeState.FAILED,
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
        state: OutcomeState,
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
        currentSwitch: SwitchProjection?,
    ) =
        VexLifeModelsFamily.State(
            handle = entry.handle,
            artifactPresence =
                when (presence.state) {
                    HereticArtifactMaterialization.PresenceState.COMPLETE_BY_SIZE_NOT_HASHED ->
                        VexLifeModelsFamily.ArtifactPresence.PRESENT
                    HereticArtifactMaterialization.PresenceState.PARTIAL_BY_SIZE ->
                        VexLifeModelsFamily.ArtifactPresence.PARTIAL
                    HereticArtifactMaterialization.PresenceState.INVALID_SIZE ->
                        VexLifeModelsFamily.ArtifactPresence.DAMAGED
                    HereticArtifactMaterialization.PresenceState.ABSENT ->
                        VexLifeModelsFamily.ArtifactPresence.ABSENT
                },
            downloading = running?.handle == entry.handle,
            chosen = chosen == entry.modelRef,
            serving = serving == entry.modelRef,
            switching = currentSwitch?.handle == entry.handle,
            anyDownloadRunning = running != null,
            anySwitchRunning = currentSwitch != null,
        )

    /** Waits for a running download to finish (tests and orderly shutdown). */
    internal fun awaitIdle(timeoutMs: Long = 30_000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (
            (download.get() != null || switching.get() != null) &&
            System.currentTimeMillis() < deadline
        ) {
            Thread.sleep(20)
        }
    }

    fun switchInProgress(): Boolean =
        switching.get() !=
            null

    fun switchHeld(): Boolean {
        val outcome = lastOutcome.get()
        val chosen = chosenModelRef()
        val serving = servingModelRef()
        return switching.get() == null &&
            outcome?.controlRef == VexLifeModelsFamily.ControlUse &&
            outcome.state == OutcomeState.FAILED &&
            chosen != null &&
            serving != null &&
            chosen != serving
    }

    override fun close() {
        executor.shutdownNow()
    }

    companion object {
        const val Schema = "vexlife-prod.models/v1"
    }
}

// [VXG RealForever]
