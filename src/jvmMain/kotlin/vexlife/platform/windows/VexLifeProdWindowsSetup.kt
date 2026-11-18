package vexlife.platform.windows

import java.net.InetSocketAddress
import java.net.ServerSocket
import java.nio.file.Files
import java.nio.file.Path
import java.time.OffsetDateTime
import vexlife.entry.VexLifeProdEntryFamily
import vexlife.home.VexLifeProdHome
import vexlife.home.VexLifeProdHomeFiles
import vexlife.local.VexLifeProdLocalSetupLease
import vexlife.local.VexLifeProdRuntimeSessionFactory
import vexlife.local.VexLifeProdSetupProgress
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticRuntimeRealization
import vexlife.service.VexLifeProdLocalService
import vexlife.service.VexLifeProdServiceContract
import vexlife.setup.VexLifeProdSetupLanguage
import vexlife.setup.VexLifeProdSetupModel

/**
 * Windows Product-entry realization for the lived release preview.
 *
 * The portable Entry Family still owns State / Control / Effect meaning. This owner supplies only
 * Windows host/Home/model/runtime observations and realizes the exact Home + runtime effects selected
 * by the Family. It deliberately does not install a Windows Service or invent a second Product
 * lifecycle owner: the preview EXE retains [RunningProduct] custody until the person closes it.
 */
internal object VexLifeProdWindowsSetup {
    private val GitSha = Regex("[0-9a-f]{40}")
    private const val OneGiB = 1024L * 1024L * 1024L

    data class Observation(
        val entryState: VexLifeProdEntryFamily.EntryState,
        val displayState: VexLifeProdEntryFamily.EntryDisplayState,
        val location: VexLifeProdHomeFiles.Location,
        val currentState: VexLifeProdServiceContract.CurrentStateProjection?,
        val selectedModel: HereticModelCatalog.Entry?,
        val supportedModels: List<HereticModelCatalog.Entry>,
        val materialization: VexLifeWindowsRuntimeMaterialization.Observation?,
        val freeStorageBytes: Long?,
    )

    class RunningProduct internal constructor(
        private val service: VexLifeProdLocalService,
    ) : AutoCloseable {
        val origin: String
            get() = service.origin

        override fun close() {
            service.close()
        }
    }

    fun observe(
        desiredSourceSha: String,
        productHome: Path,
        explicitModelHandle: String?,
    ): Observation {
        require(GitSha.matches(desiredSourceSha)) {
            "Windows setup requires one exact desired Product source SHA"
        }
        require(productHome.isAbsolute) {
            "Windows setup requires one absolute selected Vex Home"
        }

        val home = productHome.normalize()
        val location = VexLifeProdHomeFiles.observeLocation(home)
        val host = VexLifeWindowsHostObservation.current()

        if (host !is VexLifeWindowsHostObservation.Result.Resolved ||
            host.target != HereticRuntimeRealization.WindowsX64
        ) {
            val reason =
                when (host) {
                    is VexLifeWindowsHostObservation.Result.UnsupportedHost -> "PLATFORM_UNSUPPORTED"
                    is VexLifeWindowsHostObservation.Result.UnsupportedArchitecture ->
                        "ARCHITECTURE_UNSUPPORTED"
                    else -> "ARCHITECTURE_UNSUPPORTED"
                }
            val state =
                VexLifeProdEntryFamily.HostHeld(
                    desiredSourceSha = desiredSourceSha,
                    reasonRef = reason,
                    chipModel = System.getenv("PROCESSOR_IDENTIFIER"),
                    totalMemoryBytes = VexLifeProdWindowsCurrentState.totalMemoryBytes(),
                    requiredChipModel = "Windows x64",
                    requiredMemoryBytes =
                        HereticModelCatalog.Models.Qwen4B.recommendedMemoryBytes,
                    homePresent = location is VexLifeProdHomeFiles.Location.Existing,
                )
            return observation(
                state = state,
                location = location,
                currentState = null,
                selectedModel = null,
                supportedModels = emptyList(),
                materialization = null,
                freeStorageBytes = null,
            )
        }

        val current = VexLifeProdWindowsCurrentState.observe(home)
        if (Path.of(current.homePath).normalize() != home ||
            current.homeDisposition != location.disposition.wireValue
        ) {
            val state =
                VexLifeProdEntryFamily.HeldUnknown(
                    desiredSourceSha = desiredSourceSha,
                    detail = "the selected Vex Home changed while VexLife was looking",
                )
            return observation(
                state = state,
                location = location,
                currentState = current,
                selectedModel = null,
                supportedModels = emptyList(),
                materialization = null,
                freeStorageBytes = current.freeStorageBytes,
            )
        }

        val resolver =
            HereticRuntimeRealization.Resolver(
                HereticRuntimeRealization.TargetSource {
                    HereticRuntimeRealization.WindowsX64
                },
            )
        val supportedModels = resolver.supportedModels()
        val selectedModel =
            runCatching {
                    VexLifeProdSetupModel.resolve(
                        productHome = home,
                        explicit = explicitModelHandle,
                        totalMemoryBytes = current.totalMemoryBytes,
                        runtimeRealizations = resolver,
                    )
                }
                .getOrNull()

        when (location) {
            is VexLifeProdHomeFiles.Location.Formable -> {
                val state =
                    VexLifeProdEntryFamily.HomeFormationRequired(
                        desiredSourceSha = desiredSourceSha,
                    )
                return observation(
                    state = state,
                    location = location,
                    currentState = current,
                    selectedModel = selectedModel,
                    supportedModels = supportedModels,
                    materialization = null,
                    freeStorageBytes = current.freeStorageBytes,
                )
            }

            is VexLifeProdHomeFiles.Location.Held -> {
                val state =
                    VexLifeProdEntryFamily.HomeHeld(
                        desiredSourceSha = desiredSourceSha,
                        disposition = location.disposition,
                        detail = location.detail,
                    )
                return observation(
                    state = state,
                    location = location,
                    currentState = current,
                    selectedModel = selectedModel,
                    supportedModels = supportedModels,
                    materialization = null,
                    freeStorageBytes = current.freeStorageBytes,
                )
            }

            is VexLifeProdHomeFiles.Location.Existing -> Unit
        }

        val model =
            requireNotNull(selectedModel) {
                "Windows setup has no target-supported Product model"
            }
        val resolution = resolver.resolve(model)
        val realization =
            requireNotNull(
                (resolution as? HereticRuntimeRealization.Resolution.Supported)?.realization,
            ) {
                "selected Product model has no Windows runtime realization"
            }
        val materialization =
            VexLifeWindowsRuntimeMaterialization.observe(
                productHome = home,
                realization = realization,
            )

        if (!servicePortAvailable()) {
            val state =
                VexLifeProdEntryFamily.HeldUnknown(
                    desiredSourceSha = desiredSourceSha,
                    detail =
                        "the VexLife loopback service coordinate is already occupied and this preview " +
                            "does not own authority to replace that process",
                )
            return observation(
                state = state,
                location = location,
                currentState = current,
                selectedModel = model,
                supportedModels = supportedModels,
                materialization = materialization,
                freeStorageBytes = current.freeStorageBytes,
            )
        }

        if (materialization.modelState == VexLifeWindowsRuntimeMaterialization.FileState.INVALID) {
            val state =
                VexLifeProdEntryFamily.ModelFilesHeld(
                    desiredSourceSha = desiredSourceSha,
                    invalidBytes = realization.artifact.expectedPrimaryBytes ?: 1L,
                )
            return observation(
                state = state,
                location = location,
                currentState = current,
                selectedModel = model,
                supportedModels = supportedModels,
                materialization = materialization,
                freeStorageBytes = current.freeStorageBytes,
            )
        }

        if (
            materialization.runtimeArchiveState ==
                VexLifeWindowsRuntimeMaterialization.FileState.INVALID ||
                materialization.runtimeImageState ==
                VexLifeWindowsRuntimeMaterialization.FileState.INVALID ||
                materialization.runtimeExecutableState ==
                VexLifeWindowsRuntimeMaterialization.FileState.INVALID
        ) {
            val state =
                VexLifeProdEntryFamily.HeldUnknown(
                    desiredSourceSha = desiredSourceSha,
                    detail =
                        "the existing Windows local-model runtime files are not the exact declared " +
                            "realization; this preview preserves them rather than replacing them",
                )
            return observation(
                state = state,
                location = location,
                currentState = current,
                selectedModel = model,
                supportedModels = supportedModels,
                materialization = materialization,
                freeStorageBytes = current.freeStorageBytes,
            )
        }

        val savedModel = VexLifeProdSetupModel.saved(home)
        val needsAcquisition =
            savedModel != model ||
                materialization.modelState != VexLifeWindowsRuntimeMaterialization.FileState.VERIFIED ||
                materialization.runtimeArchiveState !=
                    VexLifeWindowsRuntimeMaterialization.FileState.VERIFIED ||
                materialization.runtimeImageState !=
                    VexLifeWindowsRuntimeMaterialization.FileState.VERIFIED ||
                materialization.runtimeExecutableState !=
                    VexLifeWindowsRuntimeMaterialization.FileState.VERIFIED

        if (needsAcquisition) {
            val missingBytes =
                (if (
                    materialization.modelState ==
                        VexLifeWindowsRuntimeMaterialization.FileState.ABSENT
                ) {
                    realization.artifact.expectedPrimaryBytes ?: 0L
                } else {
                    0L
                }) +
                    (if (
                        materialization.runtimeArchiveState ==
                            VexLifeWindowsRuntimeMaterialization.FileState.ABSENT
                    ) {
                        realization.runtimeBinary?.archiveBytes ?: 0L
                    } else {
                        0L
                    })
            val requiredFreeBytes = missingBytes + OneGiB
            val freeBytes = current.freeStorageBytes
            if (freeBytes != null && freeBytes < requiredFreeBytes) {
                val state =
                    VexLifeProdEntryFamily.StorageHeld(
                        desiredSourceSha = desiredSourceSha,
                        requiredFreeBytes = requiredFreeBytes,
                        freeBytes = freeBytes,
                    )
                return observation(
                    state = state,
                    location = location,
                    currentState = current,
                    selectedModel = model,
                    supportedModels = supportedModels,
                    materialization = materialization,
                    freeStorageBytes = freeBytes,
                )
            }

            val state =
                VexLifeProdEntryFamily.SetupRequired(
                    desiredSourceSha = desiredSourceSha,
                )
            return observation(
                state = state,
                location = location,
                currentState = current,
                selectedModel = model,
                supportedModels = supportedModels,
                materialization = materialization,
                freeStorageBytes = freeBytes,
            )
        }

        val state =
            VexLifeProdEntryFamily.StartRequired(
                desiredSourceSha = desiredSourceSha,
                priorServiceSourceSha = null,
            )
        return observation(
            state = state,
            location = location,
            currentState = current,
            selectedModel = model,
            supportedModels = supportedModels,
            materialization = materialization,
            freeStorageBytes = current.freeStorageBytes,
        )
    }

    fun formHome(
        productHome: Path,
    ): VexLifeProdHomeFiles.Result {
        require(productHome.isAbsolute) {
            "Windows Home formation requires one absolute selected Vex Home"
        }
        val home = productHome.normalize()
        return VexLifeProdLocalSetupLease.acquire(home).use {
            when (val location = VexLifeProdHomeFiles.observeLocation(home)) {
                is VexLifeProdHomeFiles.Location.Existing -> return@use location.home
                is VexLifeProdHomeFiles.Location.Held ->
                    throw IllegalArgumentException(
                        "selected Vex Home is preserved and cannot be formed: " +
                            location.disposition.wireValue +
                            " — " +
                            location.detail,
                    )
                is VexLifeProdHomeFiles.Location.Formable -> Unit
            }

            val host =
                requireNotNull(
                    VexLifeWindowsHostObservation.current()
                        as? VexLifeWindowsHostObservation.Result.Resolved,
                ) {
                    "Windows Home formation requires one resolved Windows host"
                }
            require(host.target == HereticRuntimeRealization.WindowsX64) {
                "Windows Home formation requires the Windows x64 target"
            }

            val plan =
                VexLifeProdHome.plan(
                    personRef = VexLifeProdHome.DefaultPersonRef,
                    familyRef = VexLifeProdHome.DefaultFamilyRef,
                    deviceName =
                        System.getenv("COMPUTERNAME")
                            ?.takeIf(String::isNotBlank)
                            ?: "Windows computer",
                    platform = "windows",
                    architecture = host.nativeArchitecture.name.lowercase(),
                    ids = VexLifeProdHomeFiles.newInstallationIds(),
                    createdAt = OffsetDateTime.now().toString(),
                )
            VexLifeProdHomeFiles.bootstrap(
                productHome = home,
                plan = plan,
            )
        }
    }

    fun persistSelections(
        productHome: Path,
        language: String,
        model: HereticModelCatalog.Entry,
    ) {
        require(productHome.isAbsolute) {
            "Windows setup selection persistence requires one absolute Vex Home"
        }
        val home = productHome.normalize()
        require(VexLifeProdSetupLanguage.persist(home, language)) {
            "Windows setup could not persist the selected language into the Vex Home"
        }
        require(VexLifeProdSetupModel.persist(home, model)) {
            "Windows setup could not persist the selected model into the Vex Home"
        }
    }

    fun startProduct(
        desiredSourceSha: String,
        productHome: Path,
        language: String,
        model: HereticModelCatalog.Entry,
        allowAcquisition: Boolean,
        progress: VexLifeProdSetupProgress,
    ): RunningProduct {
        require(GitSha.matches(desiredSourceSha)) {
            "Windows setup start requires one exact desired Product source SHA"
        }
        val home = productHome.normalize()
        persistSelections(
            productHome = home,
            language = language,
            model = model,
        )
        val session =
            VexLifeProdRuntimeSessionFactory.establishCurrent(
                productHome = home,
                allowAcquisition = allowAcquisition,
                allowRuntimeStart = true,
                progress = progress,
            )
        return try {
            progress.phase(VexLifeProdSetupProgress.Phase.StartingVexLife)
            val service =
                VexLifeProdLocalService.start(
                    sourceSha = desiredSourceSha,
                    session = session,
                    visionRuntimeExecutablePathOrNull = null,
                )
            progress.phase(VexLifeProdSetupProgress.Phase.ConfirmingVexLife)
            RunningProduct(service)
        } catch (error: Throwable) {
            runCatching {
                session.close()
            }
            throw error
        }
    }

    private fun observation(
        state: VexLifeProdEntryFamily.EntryState,
        location: VexLifeProdHomeFiles.Location,
        currentState: VexLifeProdServiceContract.CurrentStateProjection?,
        selectedModel: HereticModelCatalog.Entry?,
        supportedModels: List<HereticModelCatalog.Entry>,
        materialization: VexLifeWindowsRuntimeMaterialization.Observation?,
        freeStorageBytes: Long?,
    ): Observation =
        Observation(
            entryState = state,
            displayState = VexLifeProdEntryFamily.Semantics.displayStateFor(state),
            location = location,
            currentState = currentState,
            selectedModel = selectedModel,
            supportedModels = supportedModels,
            materialization = materialization,
            freeStorageBytes = freeStorageBytes,
        )

    private fun servicePortAvailable(): Boolean =
        runCatching {
                ServerSocket().use { socket ->
                    socket.reuseAddress = false
                    socket.bind(
                        InetSocketAddress(
                            VexLifeProdServiceContract.Host,
                            VexLifeProdServiceContract.Port,
                        ),
                    )
                }
                true
            }
            .getOrDefault(false)
}

// [VXG RealForever]
