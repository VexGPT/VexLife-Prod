package vexlife.setup

import java.nio.file.Path
import kotlinx.serialization.Serializable
import vexlife.contract.VexContractJson
import vexlife.entry.VexLifeProdEntryFamily
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

    const val EffectSchema =
        "vexlife-prod.macos-front-door-effect/v1"

    private val GitSha =
        Regex(
            "[0-9a-f]{40}",
        )

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
                        VexLifeProdEntryFamily.StateSetupRequired,
                        VexLifeProdEntryFamily.StateHeldUnknown,
                    ) &&
                    !effectsPerformed &&
                    !currentState.effectsPerformed,
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

                VexLifeProdEntryFamily.StateSetupRequired ->
                    require(
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

    @Serializable
    data class EffectProjection(
        val schemaVersion: String,
        val observedAt: String,
        val desiredSourceSha: String,
        val semanticStateRef: String,
        val controlRef: String,
        val effectRef: String?,
        val allowAcquisition: Boolean?,
        val effectsPerformed: Boolean,
    ) {
        init {
            require(
                schemaVersion ==
                    EffectSchema &&
                    GitSha.matches(
                        desiredSourceSha,
                    ) &&
                    controlRef in
                    setOf(
                        VexLifeProdEntryFamily.ControlOpenCurrent,
                        VexLifeProdEntryFamily.ControlConvergeDesired,
                        VexLifeProdEntryFamily.ControlSetUp,
                        VexLifeProdEntryFamily.ControlCancel,
                    ) &&
                    !effectsPerformed,
            ) {
                "VexLife-Prod Mac front-door effect projection is invalid"
            }

            when (
                effectRef
            ) {
                null ->
                    require(
                        allowAcquisition ==
                            null,
                    ) {
                        "no-effect Product-entry resolution must not claim acquisition authority"
                    }

                VexLifeProdEntryFamily.EffectOpenPresentation ->
                    require(
                        allowAcquisition ==
                            null,
                    ) {
                        "open-presentation effect must not claim acquisition authority"
                    }

                VexLifeProdEntryFamily.EffectConvergeProduct ->
                    require(
                        allowAcquisition !=
                            null,
                    ) {
                        "converge-product effect requires explicit acquisition authority truth"
                    }

                else ->
                    error(
                        "unsupported Product-entry effect projection",
                    )
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

        return Projection(
            schemaVersion = Schema,
            observedAt = observed.currentState.observedAt,
            desiredSourceSha = desiredSourceSha,
            semanticStateRef = observed.entryState.stateRef,
            currentServiceSourceSha = observed.currentServiceSourceSha,
            currentServicePid = observed.currentServicePid,
            serviceDetail = observed.serviceDetail,
            displayState = display,
            currentState = observed.currentState,
            effectsPerformed = false,
        )
    }

    fun resolveControl(
        desiredSourceSha: String,
        controlRef: String,
    ): EffectProjection =
        resolveControlWithHooks(
            desiredSourceSha = desiredSourceSha,
            controlRef = controlRef,
            hooks = Hooks(),
        )

    internal fun resolveControlWithHooks(
        desiredSourceSha: String,
        controlRef: String,
        hooks: Hooks,
    ): EffectProjection {
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
        val effect =
            act.effect

        return EffectProjection(
            schemaVersion = EffectSchema,
            observedAt = observed.currentState.observedAt,
            desiredSourceSha = desiredSourceSha,
            semanticStateRef = observed.entryState.stateRef,
            controlRef = control.controlRef,
            effectRef =
                effect
                    ?.effectRef,
            allowAcquisition =
                (
                    effect as?
                        VexLifeProdEntryFamily.ConvergeProduct
                )
                    ?.allowAcquisition,
            effectsPerformed = false,
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
                    VexLifeProdEntryFamily.SetupRequired(
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

    fun resolve(
        controlRef: String,
        desiredSourceSha: String,
    ) {
        println(
            VexContractJson.encode(
                VexLifeProdMacFrontDoor.resolveControl(
                    desiredSourceSha = desiredSourceSha,
                    controlRef = controlRef,
                ),
            ),
        )
    }
}

// [VXG RealForever]
