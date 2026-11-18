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
import vexlife.model.heretic.HereticModelOfferings
import vexlife.model.heretic.HereticRuntimeConfiguration
import vexlife.model.heretic.HereticRuntimeRealization
import vexlife.model.heretic.HuggingFaceModelInspection
import vexlife.model.heretic.runtime.FileHereticModelConfigurationStore
import vexlife.model.heretic.runtime.FileHereticModelImportStore
import vexlife.model.heretic.runtime.FileHereticRuntimeConfigurationStore
import vexlife.model.heretic.runtime.FileHereticRuntimeReceiptStore
import vexlife.model.heretic.runtime.HereticPersistentRuntime
import vexlife.model.heretic.runtime.HereticArtifactDelivery
import vexlife.model.heretic.runtime.HereticArtifactMaterialization
import vexlife.text.VexLifeText

/**
 * Settings → Models: the offered models as this Home holds them, and the person's model Controls.
 *
 * Effects and their owners:
 * - Select: stage the Settings target in memory only; it does not change the Home or runtime.
 * - Runtime configuration: persist admitted launch configuration only.
 * - Use/Start: commit the staged target to the Home, then request convergence through [ModelSwitchRealization].
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
    private val runtimeRealizations: HereticRuntimeRealization.Resolver,
    private val modelSwitchRealization: ModelSwitchRealization? = null,
    private val acquirer:
        (
            model: HereticModelCatalog.Entry,
            onProgress: (Long) -> Unit,
        ) -> HereticArtifactMaterialization.Acquirer =
        { model, onProgress ->
            val delivered = mutableMapOf<String, Long>()
            HereticArtifactDelivery.Acquirer(
                model = model,
                onProgress = { member, progress ->
                    delivered[member.artifactRef] = progress.bytes
                    onProgress(delivered.values.sum())
                }
            )
        },
    private val inspectImport: (String) -> HuggingFaceModelInspection.Candidate =
        { sourceUrl ->
            HuggingFaceModelInspection.inspect(sourceUrl)
        },
    private val now: () -> Instant = { Instant.now() },
) : AutoCloseable {
    private val home: Path = productHome.toAbsolutePath().normalize()

    private val executor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "vexlife-model-download").apply { isDaemon = true }
    }

    fun interface ModelSwitchRealization {
        fun switch(
            realization: HereticRuntimeRealization.Value,
            observe: (SwitchPhase) -> Unit,
        )
    }

    sealed interface SwitchPhase {
        val stateRef: String
        val messageKey: String

        data object Requested : SwitchPhase {
            override val stateRef = "REQUESTED"
            override val messageKey = "workspace.status.model-switch-requested"
        }

        data object WaitingForCurrentTurn : SwitchPhase {
            override val stateRef = "WAITING_FOR_CURRENT_TURN"
            override val messageKey = "workspace.status.model-switch-waiting"
        }

        data object PreparingTarget : SwitchPhase {
            override val stateRef = "PREPARING_TARGET"
            override val messageKey = "workspace.status.model-switch-preparing"
        }

        data object SwitchingRuntime : SwitchPhase {
            override val stateRef = "SWITCHING_RUNTIME"
            override val messageKey = "workspace.status.model-switch-runtime"
        }
    }

    @Serializable
    data class SwitchProjection(
        val handle: String,
        val label: String,
        val phase: String,
        val message: VexLifeText,
        val startedAt: String,
    )

    private data class Download(val handle: String, val baseBytes: Long, val totalBytes: Long)

    private val download = AtomicReference<Download?>(null)
    /** Settings-stage target. Durable Home choice changes only when Start is accepted. */
    private val targetModelRef = AtomicReference<String?>(null)
    private val switching = AtomicReference<SwitchProjection?>(null)
    private val downloadedBytes = AtomicLong(0)
    private val lastOutcome = AtomicReference<Outcome?>(null)
    private val checkedImportCandidate =
        AtomicReference<HuggingFaceModelInspection.Candidate?>(null)
    private val importProjection = AtomicReference<ImportProjection?>(null)

    @Serializable
    data class RuntimeConfigurationProjection(
        val promptCacheEntries: Int,
        val mode: String,
        val recommendedPromptCacheEntries: Int,
        val recommendationStanding: String,
        val minimumPromptCacheEntries: Int,
        val maximumPromptCacheEntries: Int,
    )

    @Serializable
    data class Row(
        val handle: String,
        val label: String,
        val standing: HereticModelCatalog.Standing,
        val totalBytes: Long,
        val presentBytes: Long,
        val recommendedMemoryBytes: Long?,
        val fitsThisMac: Boolean?,
        val imported: Boolean,
        val providerRepository: String,
        val providerRevision: String,
        val providerLicense: String?,
        val chosen: Boolean,
        val targeted: Boolean,
        val serving: Boolean,
        val downloadedBytes: Long?,
        val runtimeConfiguration: RuntimeConfigurationProjection,
        val runtimeConfigurationPending: Boolean,
        val display: VexLifeModelsFamily.DisplayState,
    )

    sealed interface OutcomeState {
        val stateRef: String

        data object NotLawfulNow : OutcomeState {
            override val stateRef = "NOT_LAWFUL_NOW"
        }

        data object Switching : OutcomeState {
            override val stateRef = "SWITCHING"
        }

        data object Selected : OutcomeState {
            override val stateRef = "SELECTED"
        }

        data object Configured : OutcomeState {
            override val stateRef = "CONFIGURED"
        }

        data object Switched : OutcomeState {
            override val stateRef = "SWITCHED"
        }

        data object Failed : OutcomeState {
            override val stateRef = "FAILED"
        }

        data object Downloaded : OutcomeState {
            override val stateRef = "DOWNLOADED"
        }

        data object Removed : OutcomeState {
            override val stateRef = "REMOVED"
        }

        data object Forgotten : OutcomeState {
            override val stateRef = "FORGOTTEN"
        }
    }

    @Serializable
    data class ImportProjection(
        val state: String,
        val importRef: String? = null,
        val sourceUrl: String? = null,
        val label: String? = null,
        val providerRepository: String? = null,
        val providerRevision: String? = null,
        val providerLicense: String? = null,
        val totalBytes: Long? = null,
        val memberCount: Int? = null,
        val relationship: String? = null,
        val message: VexLifeText,
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
        val selectedHandle: String?,
        val servingHandle: String?,
        val totalMemoryBytes: Long?,
        val freeStorageBytes: Long?,
        val downloading: String?,
        /** One accepted Use is converging the serving runtime through the rightful runtime owner. */
        val switching: SwitchProjection?,
        /** Durable selected model differs from the current qualified serving Session. */
        val switchPending: Boolean,
        val lastOutcome: Outcome?,
        val modelImport: ImportProjection?,
        val effectsPerformed: Boolean,
    )

    fun projection(): Projection {
        val chosen = chosenModelRef()
        val target = targetModelRef.get() ?: chosen
        val serving = servingModelRef()
        val running = download.get()
        val currentSwitch = switching.get()
        val memory = totalMemoryBytes()
        val runtimeConfigurationStore =
            FileHereticRuntimeConfigurationStore(
                home,
            )
        val servingRuntime =
            runCatching {
                FileHereticRuntimeReceiptStore(home).read()
            }.getOrNull()
        val servingPromptCacheEntries =
            servingRuntime
                ?.runtime
                ?.args
                ?.let(
                    HereticPersistentRuntime::promptCacheEntryLimit,
                )
        val rows =
            HereticModelOfferings.entries(home).map { entry ->
                val runtimeResolution =
                    runtimeRealizations.resolve(
                        entry,
                    )
                val realizable =
                    runtimeResolution is HereticRuntimeRealization.Resolution.Supported
                val presence = HereticArtifactMaterialization.observePresence(home, entry)
                val runtimeConfiguration =
                    runtimeConfigurationStore.effective(
                        entry,
                    )
                val runtimeConfigurationPending =
                    serving == entry.modelRef &&
                        servingRuntime?.modelRef == entry.modelRef &&
                        servingPromptCacheEntries != runtimeConfiguration.promptCacheEntries
                val state =
                    state(
                        entry = entry,
                        presence = presence,
                        chosen = chosen,
                        targeted = target,
                        serving = serving,
                        running = running,
                        currentSwitch = currentSwitch,
                        realizable = realizable,
                        runtimeConfiguration = runtimeConfiguration,
                        runtimeConfigurationPending = runtimeConfigurationPending,
                    )
                Row(
                    handle = entry.handle,
                    label = entry.humanLabel,
                    standing = entry.standing,
                    totalBytes = entry.totalBytes,
                    presentBytes = presence.presentBySizeBytes,
                    recommendedMemoryBytes = entry.recommendedMemoryBytes,
                    fitsThisMac = HereticModelCatalog.fits(entry, memory),
                    imported = entry.origin == HereticModelCatalog.Origin.HOME_IMPORTED,
                    providerRepository = entry.providerRepository,
                    providerRevision = entry.providerRevision,
                    providerLicense = entry.providerLicense,
                    chosen = state.chosen,
                    targeted = state.targeted,
                    serving = state.serving,
                    downloadedBytes =
                        running
                            ?.takeIf { it.handle == entry.handle }
                            ?.let { it.baseBytes + downloadedBytes.get() },
                    runtimeConfiguration =
                        RuntimeConfigurationProjection(
                            promptCacheEntries = runtimeConfiguration.promptCacheEntries,
                            mode = runtimeConfiguration.mode,
                            recommendedPromptCacheEntries =
                                runtimeConfiguration.recommendedPromptCacheEntries,
                            recommendationStanding =
                                runtimeConfiguration.recommendationStanding,
                            minimumPromptCacheEntries =
                                runtimeConfiguration.minimumPromptCacheEntries,
                            maximumPromptCacheEntries =
                                runtimeConfiguration.maximumPromptCacheEntries,
                        ),
                    runtimeConfigurationPending = runtimeConfigurationPending,
                    display = VexLifeModelsFamily.displayStateFor(state),
                )
            }
        return Projection(
            schemaVersion = Schema,
            models = rows,
            selectedHandle = rows.singleOrNull { it.targeted }?.handle,
            servingHandle = rows.singleOrNull { it.serving }?.handle,
            totalMemoryBytes = memory,
            freeStorageBytes = runCatching { Files.getFileStore(home).usableSpace }.getOrNull(),
            downloading = running?.handle,
            switching = currentSwitch,
            switchPending = chosen != null && serving != null && chosen != serving,
            lastOutcome = lastOutcome.get(),
            modelImport = importProjection.get(),
            effectsPerformed = false,
        )
    }

    fun checkImport(sourceUrl: String): Projection {
        return try {
            val candidate = inspectImport(sourceUrl)
            checkedImportCandidate.set(candidate)
            importProjection.set(
                ImportProjection(
                    state = "READY",
                    importRef = candidate.importRef,
                    sourceUrl = candidate.sourceUrl,
                    label = candidate.humanLabel,
                    providerRepository = candidate.providerRepository,
                    providerRevision = candidate.providerRevision,
                    providerLicense = candidate.providerLicense,
                    totalBytes = candidate.totalBytes,
                    memberCount = candidate.memberCount,
                    relationship = candidate.relationship,
                    message = VexLifeText("workspace.status.model-import-ready"),
                ),
            )
            projection()
        } catch (error: Exception) {
            checkedImportCandidate.set(null)
            importProjection.set(
                ImportProjection(
                    state = "HELD",
                    sourceUrl = sourceUrl,
                    message =
                        VexLifeText.text(
                            "workspace.status.model-import-held",
                            "reason" to (error.message ?: error.javaClass.simpleName),
                        ),
                ),
            )
            projection()
        }
    }

    fun addCheckedImport(importRef: String): Projection {
        val candidate =
            requireNotNull(
                checkedImportCandidate.get(),
            ) {
                "no checked model import is ready"
            }
        require(candidate.importRef == importRef) {
            "checked model import changed before Add"
        }
        val result =
            FileHereticModelImportStore(home)
                .add(candidate.record)
        checkedImportCandidate.set(null)
        importProjection.set(
            ImportProjection(
                state = "ADDED",
                importRef = candidate.importRef,
                sourceUrl = candidate.sourceUrl,
                label = candidate.humanLabel,
                providerRepository = candidate.providerRepository,
                providerRevision = candidate.providerRevision,
                providerLicense = candidate.providerLicense,
                totalBytes = candidate.totalBytes,
                memberCount = candidate.memberCount,
                relationship = candidate.relationship,
                message =
                    VexLifeText.text(
                        "workspace.status.model-import-added",
                        "model" to candidate.humanLabel,
                    ),
            ),
        )
        return projection()
            .copy(effectsPerformed = result.writePerformed)
    }

    /** One Control, re-resolved against a fresh observation; the projection after it. */
    fun control(
        controlRef: String,
        handle: String,
        promptCacheEntries: Int? = null,
    ): Projection {
        val entry =
            requireNotNull(HereticModelOfferings.byHandle(home, handle)) {
                "model $handle is not offered by this Vex Home"
            }
        val control =
            requireNotNull(
                VexLifeModelsFamily.controlForRef(
                    controlRef = controlRef,
                    promptCacheEntries = promptCacheEntries,
                ),
            ) {
                "model control $controlRef is not known"
            }
        val presence =
            HereticArtifactMaterialization.observePresence(
                home,
                entry,
            )
        val chosen =
            chosenModelRef()
        val target =
            targetModelRef.get()
                ?: chosen
        val serving =
            servingModelRef()
        val effectiveRuntimeConfiguration =
            FileHereticRuntimeConfigurationStore(
                home,
            )
                .effective(
                    entry,
                )
        val servingRuntime =
            runCatching {
                FileHereticRuntimeReceiptStore(
                    home,
                )
                    .read()
            }
                .getOrNull()
        val runtimeConfigurationPending =
            serving == entry.modelRef &&
                servingRuntime != null &&
                servingRuntime.modelRef == entry.modelRef &&
                servingRuntime.runtime.args
                    .let(
                        HereticPersistentRuntime::promptCacheEntryLimit,
                    ) != effectiveRuntimeConfiguration.promptCacheEntries
        val state =
            state(
                entry = entry,
                presence = presence,
                chosen = chosen,
                targeted = target,
                serving = serving,
                running = download.get(),
                currentSwitch = switching.get(),
                runtimeConfiguration = effectiveRuntimeConfiguration,
                runtimeConfigurationPending = runtimeConfigurationPending,
            )
        val act = VexLifeModelsFamily.resolve(state, control)
        if (act == null) {
            remember(
                entry,
                controlRef,
                OutcomeState.NotLawfulNow,
                VexLifeText("workspace.status.model-control-not-lawful"),
            )
            return projection()
        }
        VexLogger.record(
            VexLogger.Fact.ModelControlAccepted,
            mapOf("model" to handle, "controlRef" to controlRef),
        )
        act.admittedState?.let { admittedState ->
            when (control) {
                VexLifeModelsFamily.Select -> {
                    require(admittedState.targeted) {
                        "Models admitted State is not one staged target selection"
                    }
                    targetModelRef.set(entry.modelRef)
                    remember(
                        entry,
                        controlRef,
                        OutcomeState.Selected,
                        VexLifeText.text(
                            "workspace.status.model-selected",
                            "model" to entry.humanLabel,
                        ),
                    )
                }

                VexLifeModelsFamily.Remove,
                VexLifeModelsFamily.ForgetImportedModel,
                -> {
                    require(!admittedState.targeted) {
                        "Models cleanup Control did not clear its staged target"
                    }
                    targetModelRef.compareAndSet(entry.modelRef, null)
                }

                else ->
                    error("unexpected Models admitted State for $controlRef")
            }
        }
        when (val effect = act.effect) {
            VexLifeModelsFamily.Effect.SwitchModel -> {
                if (!state.chosen) {
                    val manifest =
                        requireNotNull(
                            VexLifeProdHomeFiles.observeExisting(
                                home,
                            ),
                        ).manifest
                    FileHereticModelConfigurationStore(home)
                        .selectModel(
                            manifest,
                            entry,
                        )
                    VexLogger.record(
                        VexLogger.Fact.PersonModelChosen,
                        mapOf(
                            "model" to handle,
                            "via" to "settings-start",
                        ),
                    )
                }
                startSwitch(
                    entry = entry,
                    controlRef = controlRef,
                )
            }
            VexLifeModelsFamily.Effect.UseRecommendedRuntimeConfiguration -> {
                val configured =
                    FileHereticRuntimeConfigurationStore(home)
                        .useRecommended(
                            entry,
                        )
                remember(
                    entry,
                    controlRef,
                    OutcomeState.Configured,
                    VexLifeText.text(
                        "workspace.status.model-runtime-configured",
                        "entries" to configured.promptCacheEntries.toString(),
                    ),
                )
            }
            is VexLifeModelsFamily.Effect.SetPromptCacheEntries -> {
                val configured =
                    FileHereticRuntimeConfigurationStore(home)
                        .setPromptCacheEntries(
                            model = entry,
                            promptCacheEntries = effect.entries,
                        )
                remember(
                    entry,
                    controlRef,
                    OutcomeState.Configured,
                    VexLifeText.text(
                        "workspace.status.model-runtime-configured",
                        "entries" to configured.promptCacheEntries.toString(),
                    ),
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
                    OutcomeState.Removed,
                    VexLifeText(
                        key = "workspace.status.model-removed",
                        gib = mapOf("freed" to removal.freedBytes),
                    ),
                )
            }
            VexLifeModelsFamily.Effect.ForgetImportedModel -> {
                require(entry.origin == HereticModelCatalog.Origin.HOME_IMPORTED) {
                    "only one Home-imported model can be forgotten"
                }
                val forgotten =
                    VexLifeProdLocalSetupLease.acquire(home).use {
                        // Forget depends on this model's artifact bytes remaining absent. Hold the
                        // same Home setup lease used by artifact mutation across the final absence
                        // check, runtime-setting cleanup, and imported-history write so a concurrent
                        // setup/download cannot create orphaned bytes between those effects.
                        FileHereticRuntimeConfigurationStore(home)
                            .forgetModel(entry.modelRef)
                        FileHereticModelImportStore(home)
                            .forget(entry.handle)
                    }
                remember(
                    entry,
                    controlRef,
                    OutcomeState.Forgotten,
                    VexLifeText("workspace.status.model-forgotten"),
                )
                if (!forgotten.writePerformed) {
                    error("imported model disappeared before Forget completed")
                }
            }
            null -> Unit
        }
        return projection()
    }

    private fun startSwitch(
        entry: HereticModelCatalog.Entry,
        controlRef: String,
    ) {
        val resolved =
            when (
                val resolution =
                    runtimeRealizations.resolve(
                        entry,
                    )
            ) {
                is HereticRuntimeRealization.Resolution.Supported ->
                    resolution.realization

                is HereticRuntimeRealization.Resolution.Unsupported ->
                    error(
                        "selected model has no runtime realization on current target " +
                            HereticRuntimeRealization.targetRef(
                                resolution.target,
                            ),
                    )
            }
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
                phase = SwitchPhase.Requested.stateRef,
                message = switchMessage(SwitchPhase.Requested, entry),
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
            OutcomeState.Switching,
            initial.message,
        )
        executor.execute {
            try {
                realization.switch(
                    resolved,
                ) {
                    phase ->
                    switching.set(
                        SwitchProjection(
                            handle = entry.handle,
                            label = entry.humanLabel,
                            phase = phase.stateRef,
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
                targetModelRef.compareAndSet(
                    entry.modelRef,
                    null,
                )
                remember(
                    entry,
                    controlRef,
                    OutcomeState.Switched,
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
                    OutcomeState.Failed,
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
            phase.messageKey,
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
                        acquirer =
                            acquirer(
                                entry,
                            ) { bytes ->
                                downloadedBytes.set(bytes)
                            },
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
                    OutcomeState.Downloaded,
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
                    OutcomeState.Failed,
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
        lastOutcome.set(
            Outcome(
                handle = entry.handle,
                controlRef = controlRef,
                state = state.stateRef,
                message = message,
                at = now().toString(),
            ),
        )
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
        targeted: String?,
        serving: String?,
        running: Download?,
        currentSwitch: SwitchProjection?,
        realizable: Boolean,
        runtimeConfiguration: HereticRuntimeConfiguration.Effective,
        runtimeConfigurationPending: Boolean = false,
    ) =
        VexLifeModelsFamily.State(
            handle = entry.handle,
            artifactPresence =
                when (presence.state) {
                    HereticArtifactMaterialization.PresenceState.CompleteBySizeNotHashed ->
                        VexLifeModelsFamily.ArtifactPresence.Present
                    HereticArtifactMaterialization.PresenceState.PartialBySize ->
                        VexLifeModelsFamily.ArtifactPresence.Partial
                    HereticArtifactMaterialization.PresenceState.InvalidSize ->
                        VexLifeModelsFamily.ArtifactPresence.Damaged
                    HereticArtifactMaterialization.PresenceState.Absent ->
                        VexLifeModelsFamily.ArtifactPresence.Absent
                },
            downloading = running?.handle == entry.handle,
            chosen = chosen == entry.modelRef,
            targeted = targeted == entry.modelRef,
            serving = serving == entry.modelRef,
            switching = currentSwitch?.handle == entry.handle,
            imported = entry.origin == HereticModelCatalog.Origin.HOME_IMPORTED,
            realizable = realizable,
            runtimeConfiguration = runtimeConfiguration,
            runtimeConfigurationPending = runtimeConfigurationPending,
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
            outcome.state == OutcomeState.Failed.stateRef &&
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
