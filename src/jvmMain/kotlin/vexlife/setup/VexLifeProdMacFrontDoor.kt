package vexlife.setup

import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import kotlinx.serialization.Serializable
import vexlife.contract.VexContractJson
import vexlife.entry.VexLifeProdEntryFamily
import vexlife.home.VexLifeProdHomeFiles
import vexlife.local.VexLifeProdSetupProgress
import vexlife.model.heretic.HereticProductIdentity
import vexlife.model.heretic.runtime.HereticArtifactMaterialization
import vexlife.model.heretic.runtime.HereticMacHostProfile
import vexlife.model.heretic.runtime.HereticPythonEnvironment
import vexlife.model.heretic.runtime.HereticSetupStorage
import vexlife.platform.macos.MacUserLaunchdProcess
import vexlife.platform.python.ManagedPython
import vexlife.service.VexLifeProdCurrentState
import vexlife.service.VexLifeProdMacServiceLifetime
import vexlife.service.VexLifeProdServiceContract

/**
 * macOS observer/adapter for the portable Product-entry Family.
 *
 * Mac-specific Home-location / host / model-presence / launchd / receipt / PID truth is mapped into
 * common Product-entry State. The Family owns lawful Controls / Effects / DisplayState; this adapter
 * owns no duplicate human decision tree.
 *
 * Mapping order (each step is no-effect observation):
 *
 *   selected Home location   formable -> host? -> HOME_FORMATION_REQUIRED | HOST_HELD
 *                            held     -> HOME_HELD
 *                            canonical:
 *   Product-service ownership  unknown -> HOLD_UNKNOWN (never act on unproven ownership)
 *                              running owned -> CURRENT | UPDATE_AVAILABLE
 *                              stopped / absent / loaded-not-ready:
 *   host                       ineligible -> HOST_HELD
 *   model + runtime presence   invalid files -> MODEL_FILES_HELD
 *                              acquisition needed -> storage? -> SETUP_REQUIRED | STORAGE_HELD
 *                              all present -> START_REQUIRED | REPAIR_REQUIRED | UPDATE_REPAIR_REQUIRED
 */
object VexLifeProdMacFrontDoor {
    const val Schema =
        "vexlife-prod.macos-front-door/v3"

    private val GitSha =
        Regex(
            "[0-9a-f]{40}",
        )

    private val PlanStates =
        setOf(
            VexLifeProdEntryFamily.StateSetupRequired,
            VexLifeProdEntryFamily.StateStorageHeld,
        )

    private val OwnedServiceStates =
        setOf(
            VexLifeProdEntryFamily.StateCurrent,
            VexLifeProdEntryFamily.StateUpdateAvailable,
            VexLifeProdEntryFamily.StateRepairRequired,
            VexLifeProdEntryFamily.StateUpdateRepairRequired,
        )

    @Serializable
    data class SetupPlan(
        val modelRef: String,
        val modelProfileRef: String,
        val modelLabel: String,
        val modelArtifactTotalBytes: Long,
        val modelArtifactMissingBytes: Long,
        val modelMinimumMemoryBytes: Long,
        val currentTotalMemoryBytes: Long?,
        val currentFreeStorageBytes: Long?,
        val requiredFreeStorageBytes: Long,
        val storageShortfallObserved: Boolean,
        val acquisitionDisposition: String,
        val effectsPerformed: Boolean,
    ) {
        init {
            require(
                modelRef ==
                    HereticProductIdentity.ModelRef &&
                    modelProfileRef ==
                    HereticProductIdentity.ModelProfileRef &&
                    modelLabel.isNotBlank() &&
                    modelArtifactTotalBytes >
                    0 &&
                    modelArtifactMissingBytes in
                    0..modelArtifactTotalBytes &&
                    modelMinimumMemoryBytes >
                    0 &&
                    requiredFreeStorageBytes >=
                    modelArtifactMissingBytes &&
                    acquisitionDisposition ==
                    "MAY_ACQUIRE_MISSING_MODEL_AND_RUNTIME_PREREQUISITES" &&
                    !effectsPerformed,
            ) {
                "VexLife-Prod setup plan is invalid"
            }
        }
    }

    @Serializable
    data class Projection(
        val schemaVersion: String,
        val observedAt: String,
        val desiredSourceSha: String,
        val semanticStateRef: String,
        val currentServiceSourceSha: String?,
        val currentServicePid: Long?,
        val serviceDetail: String?,
        val displayState: VexLifeProdEntryFamily.EntryDisplayState,
        val setupPlan: SetupPlan?,
        val currentState: VexLifeProdServiceContract.CurrentStateProjection,
        val card: VexLifeProdMacSetupCard.Card,
        val effectsPerformed: Boolean,
    ) {
        init {
            require(
                schemaVersion ==
                    Schema &&
                    GitSha.matches(
                        desiredSourceSha,
                    ) &&
                    semanticStateRef in
                    VexLifeProdEntryFamily.StateRefs &&
                    !effectsPerformed &&
                    !currentState.effectsPerformed &&
                    (
                        (
                            semanticStateRef in
                                PlanStates
                        ) ==
                            (
                                setupPlan !=
                                    null
                            )
                    ),
            ) {
                "VexLife-Prod Mac front-door projection is invalid"
            }

            when (
                semanticStateRef
            ) {
                in OwnedServiceStates ->
                    require(
                        !currentServiceSourceSha.isNullOrBlank() &&
                            currentServicePid !=
                            null &&
                            currentServicePid >
                            0,
                    ) {
                        "Mac front-door owned-service state requires exact source/PID projection"
                    }

                VexLifeProdEntryFamily.StateStartRequired ->
                    require(
                        currentServicePid ==
                            null &&
                            serviceDetail ==
                            null,
                    ) {
                        "start-required projection must not claim a live owned service"
                    }

                VexLifeProdEntryFamily.StateHeldUnknown ->
                    require(
                        currentServiceSourceSha ==
                            null &&
                            currentServicePid ==
                            null &&
                            !serviceDetail.isNullOrBlank(),
                    ) {
                        "held front-door projection requires only bounded diagnostic detail"
                    }

                else ->
                    require(
                        currentServiceSourceSha ==
                            null &&
                            currentServicePid ==
                            null &&
                            serviceDetail ==
                            null,
                    ) {
                        "setup/held projection must not invent existing-service truth"
                    }
            }
        }
    }

    internal data class Hooks(
        val observeCurrentState: (
            Path,
        ) -> VexLifeProdServiceContract.CurrentStateProjection = {
            productHome ->
            VexLifeProdCurrentState.observeCurrentMac(productHome)
        },
        val observeHomeLocation: (
            Path,
        ) -> VexLifeProdHomeFiles.Location = {
            productHome ->
            VexLifeProdHomeFiles.observeLocation(
                productHome,
            )
        },
        val observeExistingService: (
            Path,
        ) -> VexLifeProdMacServiceLifetime.ExistingObservation = {
            productHome ->
            VexLifeProdMacServiceLifetime.observeExisting(
                productHome,
            )
        },
        /**
         * No-effect presence of the private managed Python and the Heretic environment directories.
         * Validity is still verified by their owners during convergence; absence alone means a
         * start without acquisition authority cannot succeed.
         */
        val observeRuntimeEnvironmentPresent: (
            Path,
        ) -> Boolean = {
            productHome ->
            runCatching {
                Files.isDirectory(
                    productHome.resolve(
                        ManagedPython.PythonInstallRelativePath,
                    ),
                    LinkOption.NOFOLLOW_LINKS,
                ) &&
                    Files.isDirectory(
                        HereticPythonEnvironment.environmentRoot(
                            productHome,
                        ),
                        LinkOption.NOFOLLOW_LINKS,
                    )
            }.getOrDefault(
                false,
            )
        },
        val userHome: () -> Path = {
            Path.of(
                System.getProperty(
                    "user.home",
                ),
            )
        },
    )

    internal data class Observed(
        val currentState: VexLifeProdServiceContract.CurrentStateProjection,
        val location: VexLifeProdHomeFiles.Location,
        val entryState: VexLifeProdEntryFamily.EntryState,
        val currentServiceSourceSha: String?,
        val currentServicePid: Long?,
        val serviceDetail: String?,
        val storage: HereticSetupStorage.Requirement?,
    )

    internal data class ResolvedControl(
        val semanticStateRef: String,
        val controlRef: String,
        val effect: VexLifeProdEntryFamily.EntryEffect?,
        val lawful: Boolean =
            true,
    )

    fun observe(
        desiredSourceSha: String,
        productHome: Path,
    ): Projection =
        observeWithHooks(
            desiredSourceSha = desiredSourceSha,
            productHome = productHome,
            hooks = Hooks(),
        )

    internal fun observeWithHooks(
        desiredSourceSha: String,
        productHome: Path,
        hooks: Hooks,
    ): Projection {
        val observed =
            observeSemantic(
                desiredSourceSha = desiredSourceSha,
                productHome = productHome,
                hooks = hooks,
            )
        val display =
            VexLifeProdEntryFamily.Semantics.displayStateFor(
                observed.entryState,
            )
        val setupPlan =
            if (
                observed.entryState.stateRef in
                PlanStates
            ) {
                val storage =
                    requireNotNull(
                        observed.storage,
                    ) {
                        "setup plan requires one storage requirement"
                    }
                SetupPlan(
                    modelRef =
                        HereticProductIdentity.ModelRef,
                    modelProfileRef =
                        HereticProductIdentity.ModelProfileRef,
                    modelLabel =
                        HereticProductIdentity.HumanModelLabel,
                    modelArtifactTotalBytes =
                        HereticArtifactMaterialization.plan().totalBytes,
                    modelArtifactMissingBytes =
                        storage.missingModelBytes,
                    modelMinimumMemoryBytes =
                        HereticMacHostProfile.MinimumMemoryBytes,
                    currentTotalMemoryBytes =
                        observed.currentState.totalMemoryBytes,
                    currentFreeStorageBytes =
                        observed.currentState.freeStorageBytes,
                    requiredFreeStorageBytes =
                        storage.requiredFreeBytes,
                    storageShortfallObserved =
                        storage.shortfallObserved,
                    acquisitionDisposition =
                        "MAY_ACQUIRE_MISSING_MODEL_AND_RUNTIME_PREREQUISITES",
                    effectsPerformed = false,
                )
            } else {
                null
            }

        val card =
            VexLifeProdMacSetupCard.form(
                VexLifeProdMacSetupCard.Facts(
                    displayState = display,
                    semanticStateRef = observed.entryState.stateRef,
                    currentState = observed.currentState,
                    location = observed.location,
                    setupPlan = setupPlan,
                    formationPreview =
                        if (
                            observed.entryState is
                                VexLifeProdEntryFamily.HomeFormationRequired
                        ) {
                            HereticSetupStorage.requirement(
                                missingModelBytes =
                                    HereticArtifactMaterialization.plan().totalBytes,
                                observedFreeBytes =
                                    observed.currentState.freeStorageBytes,
                            )
                        } else {
                            null
                        },
                    currentServiceSourceSha = observed.currentServiceSourceSha,
                    requiredChipModel = HereticMacHostProfile.RequiredChipModel,
                    requiredMemoryBytes = HereticMacHostProfile.MinimumMemoryBytes,
                    protectedLocation =
                        VexLifeProdMacSetupCard.protectedLocationName(
                            userHome = hooks.userHome(),
                            selectedHome = productHome.normalize(),
                        ),
                ),
            )

        return Projection(
            schemaVersion = Schema,
            observedAt = observed.currentState.observedAt,
            desiredSourceSha = desiredSourceSha,
            semanticStateRef = observed.entryState.stateRef,
            currentServiceSourceSha = observed.currentServiceSourceSha,
            currentServicePid = observed.currentServicePid,
            serviceDetail = observed.serviceDetail,
            displayState = display,
            setupPlan = setupPlan,
            currentState = observed.currentState,
            card = card,
            effectsPerformed = false,
        )
    }

    internal fun resolveTypedControl(
        desiredSourceSha: String,
        controlRef: String,
        productHome: Path,
    ): ResolvedControl =
        resolveTypedControlWithHooks(
            desiredSourceSha = desiredSourceSha,
            controlRef = controlRef,
            productHome = productHome,
            hooks = Hooks(),
        )

    /**
     * Currentness re-check before any effect (predecessor law: ACTION WAS OFFERED != ACTION IS STILL
     * LAWFUL NOW). A Control that the freshly re-observed State no longer admits is reported as
     * not lawful instead of being realized.
     */
    internal fun resolveTypedControlWithHooks(
        desiredSourceSha: String,
        controlRef: String,
        productHome: Path,
        hooks: Hooks,
    ): ResolvedControl {
        val observed =
            observeSemantic(
                desiredSourceSha = desiredSourceSha,
                productHome = productHome,
                hooks = hooks,
            )
        val control =
            VexLifeProdEntryFamily.controlForRef(
                controlRef,
            )
        val act =
            control?.let {
                VexLifeProdEntryFamily.Semantics.resolve(
                    currentState = observed.entryState,
                    control = it,
                )
            }

        return ResolvedControl(
            semanticStateRef = observed.entryState.stateRef,
            controlRef = control?.controlRef ?: controlRef,
            effect = act?.effect,
            lawful = act != null,
        )
    }

    internal fun observeSemantic(
        desiredSourceSha: String,
        productHome: Path,
        hooks: Hooks,
    ): Observed {
        require(
            GitSha.matches(
                desiredSourceSha,
            ),
        ) {
            "Mac front door requires one exact desired Product source SHA"
        }

        require(productHome.isAbsolute) {
            "selected Vex Home path must be absolute"
        }
        val selectedHome = productHome.normalize()
        val current = hooks.observeCurrentState(selectedHome)
        require(
            Path.of(current.homePath).normalize() == selectedHome
        ) {
            "Product-entry current-state projection changed the selected Vex Home coordinate"
        }
        require(
            !current.effectsPerformed,
        ) {
            "Mac front door requires no-effect current-state awareness"
        }

        val location =
            hooks.observeHomeLocation(
                selectedHome,
            )
        require(
            Path.of(location.productHome).normalize() == selectedHome
        ) {
            "Product-entry Home location changed the selected Vex Home coordinate"
        }

        fun observed(
            state: VexLifeProdEntryFamily.EntryState,
            sourceSha: String? = null,
            pid: Long? = null,
            detail: String? = null,
            storage: HereticSetupStorage.Requirement? = null,
        ) =
            Observed(
                currentState = current,
                location = location,
                entryState = state,
                currentServiceSourceSha = sourceSha,
                currentServicePid = pid,
                serviceDetail = detail,
                storage = storage,
            )

        if (
            current.homeDisposition !=
            location.disposition.wireValue
        ) {
            // Two no-effect reads of the same coordinate disagreed: something changed it meanwhile.
            val detail =
                "the selected Home changed while VexLife was looking (" +
                    current.homeDisposition +
                    " then " +
                    location.disposition.wireValue +
                    ")"
            return observed(
                VexLifeProdEntryFamily.HeldUnknown(
                    desiredSourceSha = desiredSourceSha,
                    detail = detail,
                ),
                detail = detail,
            )
        }

        val hostEligible =
            current.hostEligibilityState ==
                "ELIGIBLE"

        fun hostHeld(
            homePresent: Boolean,
        ) =
            VexLifeProdEntryFamily.HostHeld(
                desiredSourceSha = desiredSourceSha,
                reasonRef = current.hostEligibilityReason ?: "HOST_UNSUPPORTED",
                chipModel = current.chipModel,
                totalMemoryBytes = current.totalMemoryBytes,
                requiredChipModel = HereticMacHostProfile.RequiredChipModel,
                requiredMemoryBytes = HereticMacHostProfile.MinimumMemoryBytes,
                homePresent = homePresent,
            )

        when (
            location
        ) {
            is VexLifeProdHomeFiles.Location.Formable -> {
                require(
                    current.homeRef ==
                        null &&
                        current.deviceRef ==
                        null &&
                        current.companionLineageRef ==
                        null,
                ) {
                    "absent Home current-state projection cannot claim Home/device/Companion identity"
                }
                return observed(
                    if (
                        hostEligible
                    ) {
                        VexLifeProdEntryFamily.HomeFormationRequired(
                            desiredSourceSha,
                        )
                    } else {
                        hostHeld(
                            homePresent = false,
                        )
                    },
                )
            }

            is VexLifeProdHomeFiles.Location.Held ->
                return observed(
                    VexLifeProdEntryFamily.HomeHeld(
                        desiredSourceSha = desiredSourceSha,
                        dispositionRef = location.disposition.wireValue,
                        detail = location.detail,
                    ),
                )

            is VexLifeProdHomeFiles.Location.Existing ->
                Unit
        }

        val existing =
            hooks.observeExistingService(
                selectedHome,
            )

        when (
            existing
        ) {
            is VexLifeProdMacServiceLifetime.ExistingObservation.HeldUnknown ->
                return observed(
                    VexLifeProdEntryFamily.HeldUnknown(
                        desiredSourceSha = desiredSourceSha,
                        detail = existing.detail,
                    ),
                    detail = existing.detail,
                )

            is VexLifeProdMacServiceLifetime.ExistingObservation.RunningOwned -> {
                val sourceSha =
                    existing.receipt.sourceSha
                return observed(
                    if (
                        sourceSha ==
                        desiredSourceSha
                    ) {
                        VexLifeProdEntryFamily.Current(
                            desiredSourceSha,
                        )
                    } else {
                        VexLifeProdEntryFamily.UpdateAvailable(
                            desiredSourceSha,
                        )
                    },
                    sourceSha = sourceSha,
                    pid = existing.receipt.launchd.pid,
                )
            }

            else ->
                Unit
        }

        if (
            !hostEligible
        ) {
            return observed(
                hostHeld(
                    homePresent = true,
                ),
            )
        }

        if (
            current.modelArtifactState ==
            "INVALID_SIZE"
        ) {
            return observed(
                VexLifeProdEntryFamily.ModelFilesHeld(
                    desiredSourceSha = desiredSourceSha,
                    invalidBytes = current.modelArtifactInvalidSizeBytes ?: 0L,
                ),
            )
        }

        // A selected model whose runtime binding is still pending (setup interrupted after the
        // download finished) needs runtime start + qualification, not acquisition.
        val modelReady =
            current.modelSelectionState in
                setOf(
                    "HERETIC_ESTABLISHMENT_PENDING",
                    "BOUND_HERETIC_PRODUCT_MODEL",
                ) &&
                current.modelArtifactState ==
                "COMPLETE_BY_SIZE_NOT_HASHED"
        val runtimeEnvironmentPresent =
            modelReady &&
                hooks.observeRuntimeEnvironmentPresent(
                    selectedHome,
                )

        if (
            !runtimeEnvironmentPresent
        ) {
            val missingModelBytes =
                current.modelArtifactMissingBytes
                    ?: HereticArtifactMaterialization.plan().totalBytes
            val storage =
                HereticSetupStorage.requirement(
                    missingModelBytes = missingModelBytes,
                    observedFreeBytes = current.freeStorageBytes,
                )
            return observed(
                if (
                    storage.shortfallObserved
                ) {
                    VexLifeProdEntryFamily.StorageHeld(
                        desiredSourceSha = desiredSourceSha,
                        requiredFreeBytes = storage.requiredFreeBytes,
                        freeBytes = requireNotNull(current.freeStorageBytes),
                    )
                } else {
                    VexLifeProdEntryFamily.SetupRequired(
                        desiredSourceSha,
                    )
                },
                storage = storage,
            )
        }

        return when (
            existing
        ) {
            VexLifeProdMacServiceLifetime.ExistingObservation.Absent ->
                observed(
                    VexLifeProdEntryFamily.StartRequired(
                        desiredSourceSha = desiredSourceSha,
                        priorServiceSourceSha = null,
                    ),
                )

            is VexLifeProdMacServiceLifetime.ExistingObservation.OwnedNotReady -> {
                val sourceSha =
                    existing.receipt.sourceSha
                if (
                    existing.launchdObservation ==
                    MacUserLaunchdProcess.Observation.Absent
                ) {
                    // The exact owned receipt survives but launchd no longer carries the service
                    // (for example after the Mac restarted). That is stopped, not broken.
                    observed(
                        VexLifeProdEntryFamily.StartRequired(
                            desiredSourceSha = desiredSourceSha,
                            priorServiceSourceSha = sourceSha,
                        ),
                        sourceSha = sourceSha,
                    )
                } else {
                    observed(
                        if (
                            sourceSha ==
                            desiredSourceSha
                        ) {
                            VexLifeProdEntryFamily.RepairRequired(
                                desiredSourceSha,
                            )
                        } else {
                            VexLifeProdEntryFamily.UpdateRepairRequired(
                                desiredSourceSha,
                            )
                        },
                        sourceSha = sourceSha,
                        pid = existing.receipt.launchd.pid,
                    )
                }
            }

            is VexLifeProdMacServiceLifetime.ExistingObservation.RunningOwned,
            is VexLifeProdMacServiceLifetime.ExistingObservation.HeldUnknown,
            ->
                error(
                    "running/held service observation was already mapped",
                )
        }
    }
}

object VexLifeProdMacFrontDoorProcess {
    const val ActivationSchema =
        "vexlife-prod.macos-front-door-activation/v2"
    const val ManifestationRefreshEntry =
        "REFRESH_ENTRY"
    const val ManifestationOpenProduct =
        "OPEN_PRODUCT"
    const val ManifestationNoAction =
        "NO_ACTION"
    const val StateUnobserved =
        "UNOBSERVED"

    @Serializable
    data class ActivationProjection(
        val schemaVersion: String,
        val controlRef: String,
        val semanticStateRef: String,
        val manifestationRef: String,
        val failure: VexLifeProdSetupFailure.Failure? =
            null,
        val productOrigin: String? =
            null,
        val notice: Notice? =
            null,
    ) {
        init {
            require(
                schemaVersion ==
                    ActivationSchema &&
                    controlRef.isNotBlank() &&
                    semanticStateRef.isNotBlank() &&
                    manifestationRef in
                    setOf(
                        ManifestationRefreshEntry,
                        ManifestationOpenProduct,
                        ManifestationNoAction,
                    ) &&
                    (
                        failure ==
                            null ||
                            manifestationRef ==
                            ManifestationRefreshEntry
                    ) &&
                    (
                        (
                            manifestationRef ==
                                ManifestationOpenProduct
                        ) ==
                            (
                                productOrigin !=
                                    null
                            )
                    ),
            ) {
                "VexLife-Prod Mac front-door activation projection is invalid"
            }
        }
    }

    /** Completion copy shown once after an Effect reached a freshly observed CURRENT State. */
    @Serializable
    data class Notice(
        val title: String,
        val detail: String,
    )

    internal val ReadyNotice =
        Notice(
            title = "VexLife is ready.",
            detail =
                "VexLife is running on this Mac and opened in your browser at " +
                    VexLifeProdServiceContract.Origin +
                    ". You can close this window. To start VexLife again later (for example after " +
                    "restarting your Mac), run the same setup command: it checks first and offers only " +
                    "what is needed.",
        )

    internal data class ActivationHooks(
        val progress: VexLifeProdSetupProgress =
            VexLifeProdSetupProgress.standardError(),
        val resolveControl: (
            desiredSourceSha: String,
            controlRef: String,
            productHome: Path,
        ) -> VexLifeProdMacFrontDoor.ResolvedControl = {
            desiredSourceSha,
            controlRef,
            productHome,
            ->
            VexLifeProdMacFrontDoor.resolveTypedControl(
                desiredSourceSha = desiredSourceSha,
                controlRef = controlRef,
                productHome = productHome,
            )
        },
        val formHome: (
            desiredSourceSha: String,
            productHome: Path,
        ) -> Unit = {
            desiredSourceSha,
            productHome,
            ->
            VexLifeProdSetupProcess.realizeHomeFormation(
                desiredSourceSha = desiredSourceSha,
                productHome = productHome,
                progress = progress,
            )
        },
        val convergeProduct: (
            desiredSourceSha: String,
            allowAcquisition: Boolean,
            allowRuntimeStart: Boolean,
            productHome: Path,
        ) -> Unit = {
            desiredSourceSha,
            allowAcquisition,
            allowRuntimeStart,
            productHome,
            ->
            VexLifeProdSetupProcess.realizeProductConvergence(
                desiredSourceSha = desiredSourceSha,
                allowAcquisition = allowAcquisition,
                allowRuntimeStart = allowRuntimeStart,
                productHome = productHome,
                progress = progress,
            )
        },
        val observeSemanticStateRef: (
            desiredSourceSha: String,
            productHome: Path,
        ) -> String = {
            desiredSourceSha,
            productHome,
            ->
            VexLifeProdMacFrontDoor.observe(
                desiredSourceSha = desiredSourceSha,
                productHome = productHome,
            ).semanticStateRef
        },
    )

    const val InspectionFailureSchema =
        "vexlife-prod.macos-front-door-inspection-failure/v1"

    /**
     * Observation itself could not be formed (an unexpected outside condition, not a held State).
     * The window still receives one card so the person sees a bounded explanation and can retry or
     * choose another Home instead of reading a stack trace. No Control is offered.
     */
    @Serializable
    data class InspectionFailure(
        val schemaVersion: String,
        val failure: VexLifeProdSetupFailure.Failure,
        val card: VexLifeProdMacSetupCard.Card,
        val effectsPerformed: Boolean,
    ) {
        init {
            require(
                schemaVersion ==
                    InspectionFailureSchema &&
                    card.buttons.none {
                        it.actionRef.startsWith(
                            VexLifeProdMacSetupCard.ActionControlPrefix,
                        )
                    } &&
                    !effectsPerformed,
            ) {
                "front-door inspection failure projection is invalid"
            }
        }
    }

    internal fun inspectionFailure(
        productHome: Path,
        error: Throwable,
    ): InspectionFailure {
        val failure =
            VexLifeProdSetupFailure.classify(
                error,
            )
        return InspectionFailure(
            schemaVersion = InspectionFailureSchema,
            failure = failure,
            card =
                VexLifeProdMacSetupCard.Card(
                    title = "VexLife couldn't check this Home.",
                    rail = "○ This Mac   ○ Vex Home   ○ Local model   ○ Start VexLife   ○ Ready",
                    body =
                        "VexLife tried to look at " +
                            productHome +
                            " and this Mac before offering anything, and the check itself did not finish. " +
                            "Nothing was changed.\n\nTechnical detail\n  " +
                            (failure.technicalDetail ?: failure.detail),
                    buttons =
                        listOf(
                            VexLifeProdMacSetupCard.Button("Check Again", VexLifeProdMacSetupCard.ActionRecheck),
                            VexLifeProdMacSetupCard.Button("Choose Another Location…", VexLifeProdMacSetupCard.ActionChooseHome),
                            VexLifeProdMacSetupCard.Button("Close", VexLifeProdMacSetupCard.ActionClose),
                        ),
                ),
            effectsPerformed = false,
        )
    }

    fun inspect(
        desiredSourceSha: String,
        productHome: Path,
    ) {
        val encoded =
            try {
                VexContractJson.encode(
                    VexLifeProdMacFrontDoor.observe(
                        desiredSourceSha = desiredSourceSha,
                        productHome = productHome,
                    ),
                )
            } catch (
                error: Exception,
            ) {
                VexContractJson.encode(
                    inspectionFailure(
                        productHome = productHome,
                        error = error,
                    ),
                )
            }
        utf8StandardOutput.println(
            encoded,
        )
        utf8StandardOutput.flush()
    }

    fun activate(
        controlRef: String,
        desiredSourceSha: String,
        productHome: Path,
    ) {
        utf8StandardOutput.println(
            VexContractJson.encode(
                activateWithHooks(
                    controlRef = controlRef,
                    desiredSourceSha = desiredSourceSha,
                    productHome = productHome,
                    hooks = ActivationHooks(),
                ),
            ),
        )
        utf8StandardOutput.flush()
    }

    /**
     * Card copy contains non-ASCII punctuation; a process launched by AppKit may have no locale, so
     * the JSON channel is written as UTF-8 explicitly (the window decodes it as UTF-8).
     */
    private val utf8StandardOutput: java.io.PrintStream by lazy {
        java.io.PrintStream(
            java.io.FileOutputStream(
                java.io.FileDescriptor.out,
            ),
            true,
            Charsets.UTF_8,
        )
    }

    /**
     * One activation = fresh re-observation + at most one typed Effect + fresh observation.
     *
     * An Effect that throws is still followed by observation: the window always receives the State
     * reality now has, plus a bounded human explanation of what did not happen. Method failure is
     * never presented as a predicted successor State.
     */
    internal fun activateWithHooks(
        controlRef: String,
        desiredSourceSha: String,
        productHome: Path,
        hooks: ActivationHooks,
    ): ActivationProjection {
        require(productHome.isAbsolute) {
            "selected Vex Home path must be absolute"
        }
        val selectedHome = productHome.normalize()

        fun reobserve(): String =
            runCatching {
                hooks.observeSemanticStateRef(
                    desiredSourceSha,
                    selectedHome,
                )
            }.getOrDefault(
                StateUnobserved,
            )

        fun failed(
            resolvedControlRef: String,
            failure: VexLifeProdSetupFailure.Failure,
        ) =
            ActivationProjection(
                schemaVersion = ActivationSchema,
                controlRef = resolvedControlRef,
                semanticStateRef = reobserve(),
                manifestationRef = ManifestationRefreshEntry,
                failure = failure,
            )

        val resolved =
            try {
                hooks.resolveControl(
                    desiredSourceSha,
                    controlRef,
                    selectedHome,
                )
            } catch (
                error: Exception,
            ) {
                return failed(
                    controlRef,
                    VexLifeProdSetupFailure.classify(
                        error,
                    ),
                )
            }

        if (
            !resolved.lawful
        ) {
            return ActivationProjection(
                schemaVersion = ActivationSchema,
                controlRef = resolved.controlRef,
                semanticStateRef = resolved.semanticStateRef,
                manifestationRef = ManifestationRefreshEntry,
                failure = VexLifeProdSetupFailure.stateChanged(),
            )
        }

        return when (
            val effect =
                resolved.effect
        ) {
            null ->
                ActivationProjection(
                    schemaVersion = ActivationSchema,
                    controlRef = resolved.controlRef,
                    semanticStateRef = resolved.semanticStateRef,
                    manifestationRef = ManifestationNoAction,
                )

            VexLifeProdEntryFamily.OpenPresentation ->
                ActivationProjection(
                    schemaVersion = ActivationSchema,
                    controlRef = resolved.controlRef,
                    semanticStateRef = resolved.semanticStateRef,
                    manifestationRef = ManifestationOpenProduct,
                    productOrigin = VexLifeProdServiceContract.Origin,
                )

            VexLifeProdEntryFamily.FormCanonicalHome -> {
                try {
                    hooks.formHome(
                        desiredSourceSha,
                        selectedHome,
                    )
                } catch (
                    error: Exception,
                ) {
                    return failed(
                        resolved.controlRef,
                        VexLifeProdSetupFailure.classify(
                            error,
                        ),
                    )
                }
                ActivationProjection(
                    schemaVersion = ActivationSchema,
                    controlRef = resolved.controlRef,
                    semanticStateRef = reobserve(),
                    manifestationRef = ManifestationRefreshEntry,
                )
            }

            is VexLifeProdEntryFamily.ConvergeProduct -> {
                require(
                    effect.desiredSourceSha ==
                        desiredSourceSha,
                ) {
                    "Product-entry convergence Effect source differs from the activation source"
                }
                try {
                    hooks.convergeProduct(
                        effect.desiredSourceSha,
                        effect.allowAcquisition,
                        effect.allowRuntimeStart,
                        selectedHome,
                    )
                } catch (
                    error: Exception,
                ) {
                    return failed(
                        resolved.controlRef,
                        VexLifeProdSetupFailure.classify(
                            error,
                        ),
                    )
                }
                hooks.progress.phase(
                    VexLifeProdSetupProgress.Phase.ConfirmingVexLife,
                )
                val reobserved =
                    reobserve()
                if (
                    reobserved ==
                    VexLifeProdEntryFamily.StateCurrent
                ) {
                    ActivationProjection(
                        schemaVersion = ActivationSchema,
                        controlRef = resolved.controlRef,
                        semanticStateRef = reobserved,
                        manifestationRef = ManifestationOpenProduct,
                        productOrigin = VexLifeProdServiceContract.Origin,
                        notice = ReadyNotice,
                    )
                } else {
                    ActivationProjection(
                        schemaVersion = ActivationSchema,
                        controlRef = resolved.controlRef,
                        semanticStateRef = reobserved,
                        manifestationRef = ManifestationRefreshEntry,
                    )
                }
            }
        }
    }
}

// [VXG RealForever]
