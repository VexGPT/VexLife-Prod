package vexlife.setup

import java.nio.file.Path
import kotlinx.serialization.Serializable
import vexlife.contract.VexContractJson
import vexlife.entry.VexLifeProdEntryFamily
import vexlife.model.heretic.HereticProductIdentity
import vexlife.model.heretic.runtime.HereticArtifactMaterialization
import vexlife.model.heretic.runtime.HereticMacHostProfile
import vexlife.service.VexLifeProdCurrentState
import vexlife.service.VexLifeProdMacServiceLifetime
import vexlife.service.VexLifeProdServiceContract

/**
 * macOS observer/adapter for the portable Product-entry Family.
 *
 * Mac-specific launchd/receipt/PID truth is mapped into common Product-entry State. The Family owns
 * lawful Controls / Effects / DisplayState; this adapter owns no duplicate human decision tree.
 */
object VexLifeProdMacFrontDoor {
    const val Schema =
        "vexlife-prod.macos-front-door/v2"

    private val GitSha =
        Regex(
            "[0-9a-f]{40}",
        )

    @Serializable
    data class SetupPlan(
        val modelRef: String,
        val modelProfileRef: String,
        val modelArtifactTotalBytes: Long,
        val modelMinimumMemoryBytes: Long,
        val currentTotalMemoryBytes: Long?,
        val currentFreeStorageBytes: Long?,
        val acquisitionDisposition: String,
        val effectsPerformed: Boolean,
    ) {
        init {
            require(
                modelRef ==
                    HereticProductIdentity.ModelRef &&
                    modelProfileRef ==
                    HereticProductIdentity.ModelProfileRef &&
                    modelArtifactTotalBytes >
                    0 &&
                    modelMinimumMemoryBytes >
                    0 &&
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
                    setOf(
                        VexLifeProdEntryFamily.StateCurrent,
                        VexLifeProdEntryFamily.StateUpdateAvailable,
                        VexLifeProdEntryFamily.StateRepairRequired,
                        VexLifeProdEntryFamily.StateUpdateRepairRequired,
                        VexLifeProdEntryFamily.StateHomeFormationRequired,
                        VexLifeProdEntryFamily.StateSetupRequired,
                        VexLifeProdEntryFamily.StateHeldUnknown,
                    ) &&
                    !effectsPerformed &&
                    !currentState.effectsPerformed &&
                    (
                        (
                            semanticStateRef ==
                                VexLifeProdEntryFamily.StateSetupRequired
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
                VexLifeProdEntryFamily.StateCurrent,
                VexLifeProdEntryFamily.StateUpdateAvailable,
                VexLifeProdEntryFamily.StateRepairRequired,
                VexLifeProdEntryFamily.StateUpdateRepairRequired,
                -> require(
                    !currentServiceSourceSha.isNullOrBlank() &&
                        currentServicePid !=
                        null &&
                        currentServicePid >
                        0,
                ) {
                    "Mac front-door owned-service state requires exact source/PID projection"
                }

                VexLifeProdEntryFamily.StateHomeFormationRequired,
                VexLifeProdEntryFamily.StateSetupRequired,
                -> require(
                        currentServiceSourceSha ==
                            null &&
                            currentServicePid ==
                            null &&
                            serviceDetail ==
                            null,
                    ) {
                        "setup-required projection must not invent existing-service truth"
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
            }
        }
    }

    internal data class Hooks(
        val observeCurrentState: () -> VexLifeProdServiceContract.CurrentStateProjection = {
            VexLifeProdCurrentState.observeCurrentMac()
        },
        val observeExistingService: (
            Path,
        ) -> VexLifeProdMacServiceLifetime.ExistingObservation = {
            productHome ->
            VexLifeProdMacServiceLifetime.observeExisting(
                productHome,
            )
        },
    )

    private data class Observed(
        val currentState: VexLifeProdServiceContract.CurrentStateProjection,
        val entryState: VexLifeProdEntryFamily.EntryState,
        val currentServiceSourceSha: String?,
        val currentServicePid: Long?,
        val serviceDetail: String?,
    )

    internal data class ResolvedControl(
        val semanticStateRef: String,
        val controlRef: String,
        val effect: VexLifeProdEntryFamily.EntryEffect?,
    )

    fun observe(
        desiredSourceSha: String,
    ): Projection =
        observeWithHooks(
            desiredSourceSha = desiredSourceSha,
            hooks = Hooks(),
        )

    internal fun observeWithHooks(
        desiredSourceSha: String,
        hooks: Hooks,
    ): Projection {
        val observed =
            observeSemantic(
                desiredSourceSha = desiredSourceSha,
                hooks = hooks,
            )
        val display =
            VexLifeProdEntryFamily.Semantics.displayStateFor(
                observed.entryState,
            )
        val setupPlan =
            if (
                observed.entryState is
                    VexLifeProdEntryFamily.SetupRequired
            ) {
                val artifactPlan =
                    HereticArtifactMaterialization.plan()
                SetupPlan(
                    modelRef =
                        HereticProductIdentity.ModelRef,
                    modelProfileRef =
                        HereticProductIdentity.ModelProfileRef,
                    modelArtifactTotalBytes =
                        artifactPlan.totalBytes,
                    modelMinimumMemoryBytes =
                        HereticMacHostProfile.MinimumMemoryBytes,
                    currentTotalMemoryBytes =
                        observed.currentState.totalMemoryBytes,
                    currentFreeStorageBytes =
                        observed.currentState.freeStorageBytes,
                    acquisitionDisposition =
                        "MAY_ACQUIRE_MISSING_MODEL_AND_RUNTIME_PREREQUISITES",
                    effectsPerformed = false,
                )
            } else {
                null
            }

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
            effectsPerformed = false,
        )
    }

    internal fun resolveTypedControl(
        desiredSourceSha: String,
        controlRef: String,
    ): ResolvedControl =
        resolveTypedControlWithHooks(
            desiredSourceSha = desiredSourceSha,
            controlRef = controlRef,
            hooks = Hooks(),
        )

    internal fun resolveTypedControlWithHooks(
        desiredSourceSha: String,
        controlRef: String,
        hooks: Hooks,
    ): ResolvedControl {
        val observed =
            observeSemantic(
                desiredSourceSha = desiredSourceSha,
                hooks = hooks,
            )
        val control =
            requireNotNull(
                VexLifeProdEntryFamily.controlForRef(
                    controlRef,
                ),
            ) {
                "Mac front door received an unknown Product-entry Control"
            }
        val act =
            requireNotNull(
                VexLifeProdEntryFamily.Semantics.resolve(
                    currentState = observed.entryState,
                    control = control,
                ),
            ) {
                "Product-entry Control is not lawful under the freshly reobserved State"
            }

        return ResolvedControl(
            semanticStateRef = observed.entryState.stateRef,
            controlRef = control.controlRef,
            effect = act.effect,
        )
    }

    private fun observeSemantic(
        desiredSourceSha: String,
        hooks: Hooks,
    ): Observed {
        require(
            GitSha.matches(
                desiredSourceSha,
            ),
        ) {
            "Mac front door requires one exact desired Product source SHA"
        }

        val current =
            hooks.observeCurrentState()
        require(
            !current.effectsPerformed,
        ) {
            "Mac front door requires no-effect current-state awareness"
        }

        if (
            current.homeDisposition ==
                "ABSENT_CANONICAL_VEXLIFE_HOME"
        ) {
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

            return Observed(
                currentState = current,
                entryState =
                    VexLifeProdEntryFamily.HomeFormationRequired(
                        desiredSourceSha,
                    ),
                currentServiceSourceSha = null,
                currentServicePid = null,
                serviceDetail = null,
            )
        }

        return when (
            val existing =
                hooks.observeExistingService(
                    Path.of(
                        current.homePath,
                    ),
                )
        ) {
            VexLifeProdMacServiceLifetime.ExistingObservation.Absent ->
                Observed(
                    currentState = current,
                    entryState =
                        VexLifeProdEntryFamily.SetupRequired(
                            desiredSourceSha,
                        ),
                    currentServiceSourceSha = null,
                    currentServicePid = null,
                    serviceDetail = null,
                )

            is VexLifeProdMacServiceLifetime.ExistingObservation.RunningOwned -> {
                val sourceSha =
                    existing.receipt.sourceSha
                Observed(
                    currentState = current,
                    entryState =
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
                    currentServiceSourceSha = sourceSha,
                    currentServicePid =
                        existing.receipt.launchd.pid,
                    serviceDetail = null,
                )
            }

            is VexLifeProdMacServiceLifetime.ExistingObservation.OwnedNotReady -> {
                val sourceSha =
                    existing.receipt.sourceSha
                Observed(
                    currentState = current,
                    entryState =
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
                    currentServiceSourceSha = sourceSha,
                    currentServicePid =
                        existing.receipt.launchd.pid,
                    serviceDetail =
                        existing.launchdObservation
                            .toString(),
                )
            }

            is VexLifeProdMacServiceLifetime.ExistingObservation.HeldUnknown ->
                Observed(
                    currentState = current,
                    entryState =
                        VexLifeProdEntryFamily.HeldUnknown(
                            desiredSourceSha = desiredSourceSha,
                            detail = existing.detail,
                        ),
                    currentServiceSourceSha = null,
                    currentServicePid = null,
                    serviceDetail = existing.detail,
                )
        }
    }
}

object VexLifeProdMacFrontDoorProcess {
    const val ActivationSchema =
        "vexlife-prod.macos-front-door-activation/v1"
    const val ManifestationRefreshEntry =
        "REFRESH_ENTRY"
    const val ManifestationOpenProduct =
        "OPEN_PRODUCT"
    const val ManifestationNoAction =
        "NO_ACTION"

    @Serializable
    data class ActivationProjection(
        val schemaVersion: String,
        val controlRef: String,
        val semanticStateRef: String,
        val manifestationRef: String,
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
                    ),
            ) {
                "VexLife-Prod Mac front-door activation projection is invalid"
            }
        }
    }

    internal data class ActivationHooks(
        val resolveControl: (
            desiredSourceSha: String,
            controlRef: String,
        ) -> VexLifeProdMacFrontDoor.ResolvedControl = {
            desiredSourceSha,
            controlRef,
            ->
            VexLifeProdMacFrontDoor.resolveTypedControl(
                desiredSourceSha = desiredSourceSha,
                controlRef = controlRef,
            )
        },
        val formHome: (
            desiredSourceSha: String,
        ) -> Unit = {
            desiredSourceSha ->
            VexLifeProdSetupProcess.realizeHomeFormation(
                desiredSourceSha,
            )
        },
        val convergeProduct: (
            desiredSourceSha: String,
            allowAcquisition: Boolean,
        ) -> Unit = {
            desiredSourceSha,
            allowAcquisition,
            ->
            VexLifeProdSetupProcess.realizeProductConvergence(
                desiredSourceSha = desiredSourceSha,
                allowAcquisition = allowAcquisition,
            )
        },
        val observeSemanticStateRef: (
            desiredSourceSha: String,
        ) -> String = {
            desiredSourceSha ->
            VexLifeProdMacFrontDoor.observe(
                desiredSourceSha,
            )
                .semanticStateRef
        },
    )

    fun inspect(
        desiredSourceSha: String,
    ) {
        println(
            VexContractJson.encode(
                VexLifeProdMacFrontDoor.observe(
                    desiredSourceSha,
                ),
            ),
        )
    }

    fun activate(
        controlRef: String,
        desiredSourceSha: String,
    ) {
        println(
            VexContractJson.encode(
                activateWithHooks(
                    controlRef = controlRef,
                    desiredSourceSha = desiredSourceSha,
                    hooks = ActivationHooks(),
                ),
            ),
        )
    }

    internal fun activateWithHooks(
        controlRef: String,
        desiredSourceSha: String,
        hooks: ActivationHooks,
    ): ActivationProjection {
        val resolved =
            hooks.resolveControl(
                desiredSourceSha,
                controlRef,
            )
        var semanticStateRef =
            resolved.semanticStateRef
        val manifestationRef =
            when (
                val effect =
                    resolved.effect
            ) {
                null ->
                    ManifestationNoAction

                VexLifeProdEntryFamily.OpenPresentation ->
                    ManifestationOpenProduct

                VexLifeProdEntryFamily.FormCanonicalHome -> {
                    hooks.formHome(
                        desiredSourceSha,
                    )
                    semanticStateRef =
                        hooks.observeSemanticStateRef(
                            desiredSourceSha,
                        )
                    require(
                        semanticStateRef ==
                            VexLifeProdEntryFamily.StateSetupRequired,
                    ) {
                        "fresh Home realization must re-observe Product entry as SETUP_REQUIRED"
                    }
                    ManifestationRefreshEntry
                }

                is VexLifeProdEntryFamily.ConvergeProduct -> {
                    require(
                        effect.desiredSourceSha ==
                            desiredSourceSha,
                    ) {
                        "Product-entry convergence Effect source differs from the activation source"
                    }
                    hooks.convergeProduct(
                        effect.desiredSourceSha,
                        effect.allowAcquisition,
                    )
                    semanticStateRef =
                        hooks.observeSemanticStateRef(
                            desiredSourceSha,
                        )
                    require(
                        semanticStateRef ==
                            VexLifeProdEntryFamily.StateCurrent,
                    ) {
                        "Product convergence must re-observe exact CURRENT Product entry before presentation opens"
                    }
                    ManifestationOpenProduct
                }
            }

        return ActivationProjection(
            schemaVersion = ActivationSchema,
            controlRef = resolved.controlRef,
            semanticStateRef = semanticStateRef,
            manifestationRef = manifestationRef,
        )
    }
}

// [VXG RealForever]
