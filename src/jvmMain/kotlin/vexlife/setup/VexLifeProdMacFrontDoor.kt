package vexlife.setup

import java.nio.file.Path
import kotlinx.serialization.Serializable
import vexlife.contract.VexContractJson
import vexlife.service.VexLifeProdCurrentState
import vexlife.service.VexLifeProdMacServiceLifetime
import vexlife.service.VexLifeProdServiceContract

/**
 * Read-only Mac front-door projection.
 *
 * This owner decides no lifecycle effect. It only combines current machine/Home/model truth with
 * exact existing Product-service ownership so the human can choose before install/retire/restart.
 */
object VexLifeProdMacFrontDoor {
    const val Schema =
        "vexlife-prod.macos-front-door/v1"

    const val OpenCurrent =
        "OPEN_CURRENT"

    const val UpdateAvailable =
        "UPDATE_AVAILABLE"

    const val RepairRequired =
        "REPAIR_REQUIRED"

    const val UpdateRepairRequired =
        "UPDATE_REPAIR_REQUIRED"

    const val SetupRequired =
        "SETUP_REQUIRED"

    const val HoldUnknown =
        "HOLD_UNKNOWN"

    private val GitSha =
        Regex(
            "[0-9a-f]{40}",
        )

    @Serializable
    data class Projection(
        val schemaVersion: String,
        val observedAt: String,
        val desiredSourceSha: String,
        val action: String,
        val currentServiceSourceSha: String?,
        val currentServicePid: Long?,
        val serviceDetail: String?,
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
                    action in
                    setOf(
                        OpenCurrent,
                        UpdateAvailable,
                        RepairRequired,
                        UpdateRepairRequired,
                        SetupRequired,
                        HoldUnknown,
                    ) &&
                    !effectsPerformed &&
                    !currentState.effectsPerformed,
            ) {
                "VexLife-Prod Mac front-door projection is invalid"
            }

            when (
                action
            ) {
                OpenCurrent,
                UpdateAvailable,
                RepairRequired,
                UpdateRepairRequired,
                -> require(
                    !currentServiceSourceSha.isNullOrBlank() &&
                        currentServicePid !=
                        null &&
                        currentServicePid >
                        0,
                ) {
                    "Mac front-door owned-service action requires exact source/PID projection"
                }

                SetupRequired ->
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

                HoldUnknown ->
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

        val existing =
            hooks.observeExistingService(
                Path.of(
                    current.homePath,
                ),
            )

        return when (
            existing
        ) {
            VexLifeProdMacServiceLifetime.ExistingObservation.Absent ->
                Projection(
                    schemaVersion = Schema,
                    observedAt = current.observedAt,
                    desiredSourceSha = desiredSourceSha,
                    action = SetupRequired,
                    currentServiceSourceSha = null,
                    currentServicePid = null,
                    serviceDetail = null,
                    currentState = current,
                    effectsPerformed = false,
                )

            is VexLifeProdMacServiceLifetime.ExistingObservation.RunningOwned -> {
                val sameSource =
                    existing.receipt.sourceSha ==
                        desiredSourceSha
                Projection(
                    schemaVersion = Schema,
                    observedAt = current.observedAt,
                    desiredSourceSha = desiredSourceSha,
                    action =
                        if (
                            sameSource
                        ) {
                            OpenCurrent
                        } else {
                            UpdateAvailable
                        },
                    currentServiceSourceSha = existing.receipt.sourceSha,
                    currentServicePid = existing.receipt.launchd.pid,
                    serviceDetail = null,
                    currentState = current,
                    effectsPerformed = false,
                )
            }

            is VexLifeProdMacServiceLifetime.ExistingObservation.OwnedNotReady -> {
                val sameSource =
                    existing.receipt.sourceSha ==
                        desiredSourceSha
                Projection(
                    schemaVersion = Schema,
                    observedAt = current.observedAt,
                    desiredSourceSha = desiredSourceSha,
                    action =
                        if (
                            sameSource
                        ) {
                            RepairRequired
                        } else {
                            UpdateRepairRequired
                        },
                    currentServiceSourceSha = existing.receipt.sourceSha,
                    currentServicePid = existing.receipt.launchd.pid,
                    serviceDetail =
                        existing.launchdObservation
                            .toString(),
                    currentState = current,
                    effectsPerformed = false,
                )
            }

            is VexLifeProdMacServiceLifetime.ExistingObservation.HeldUnknown ->
                Projection(
                    schemaVersion = Schema,
                    observedAt = current.observedAt,
                    desiredSourceSha = desiredSourceSha,
                    action = HoldUnknown,
                    currentServiceSourceSha = null,
                    currentServicePid = null,
                    serviceDetail = existing.detail,
                    currentState = current,
                    effectsPerformed = false,
                )
        }
    }
}

object VexLifeProdMacFrontDoorProcess {
    fun run(
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
}

// [VXG RealForever]
