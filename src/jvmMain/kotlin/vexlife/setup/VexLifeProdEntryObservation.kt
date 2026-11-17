package vexlife.setup

import java.nio.file.Path
import vexlife.entry.VexLifeProdEntryFamily
import vexlife.home.VexLifeProdHomeFiles
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticRuntimeRealization
import vexlife.model.heretic.runtime.HereticSetupStorage
import vexlife.service.VexLifeProdServiceContract

/**
 * Platform-neutral composition of accepted Product-entry observations into one Entry Family State.
 *
 * Platform adapters own host/process/service observation mechanics. This owner neither observes nor
 * performs effects; it only composes already-observed facts so Mac and Windows do not duplicate the
 * Product decision tree.
 */
internal object VexLifeProdEntryObservation {
    sealed interface Service {
        data object Absent : Service

        data class RunningOwned(
            val sourceSha: String,
            val pid: Long,
        ) : Service

        data class OwnedNotReady(
            val sourceSha: String,
            val pid: Long?,
            val substrateAbsent: Boolean,
        ) : Service

        data class HeldUnknown(
            val detail: String,
        ) : Service
    }

    data class HostRequirement(
        val requiredHardware: String,
        val requiredMemoryBytes: Long,
    ) {
        init {
            require(
                requiredHardware.isNotBlank() &&
                    requiredMemoryBytes > 0,
            ) {
                "Product-entry host requirement must be exact"
            }
        }
    }

    data class Observed(
        val currentState: VexLifeProdServiceContract.CurrentStateProjection,
        val location: VexLifeProdHomeFiles.Location,
        val entryState: VexLifeProdEntryFamily.EntryState,
        val currentServiceSourceSha: String?,
        val currentServicePid: Long?,
        val serviceDetail: String?,
        val storage: HereticSetupStorage.Requirement?,
        val model: HereticModelCatalog.Entry,
    )

    fun observe(
        desiredSourceSha: String,
        productHome: Path,
        current: VexLifeProdServiceContract.CurrentStateProjection,
        location: VexLifeProdHomeFiles.Location,
        service: Service,
        runtimePrerequisitesPresent: Boolean,
        target: HereticRuntimeRealization.Target,
        hostRequirement: HostRequirement,
        explicitModelHandle: String? = null,
    ): Observed {
        require(
            productHome.isAbsolute,
        ) {
            "selected Vex Home path must be absolute"
        }
        val selectedHome =
            productHome.normalize()
        require(
            Path.of(
                    current.homePath,
                )
                .normalize() ==
                selectedHome,
        ) {
            "Product-entry current-state projection changed the selected Vex Home coordinate"
        }
        require(
            Path.of(
                    location.productHome,
                )
                .normalize() ==
                selectedHome,
        ) {
            "Product-entry Home location changed the selected Vex Home coordinate"
        }
        require(
            !current.effectsPerformed,
        ) {
            "Product-entry observation requires no-effect current-state awareness"
        }

        val explicitModel =
            explicitModelHandle?.let {
                requireNotNull(
                    HereticModelCatalog.byHandle(
                        it,
                    ),
                ) {
                    "model $it is not offered"
                }
            }
        val observedModel =
            current.activeModelRef?.let(
                HereticModelCatalog::requireEntry,
            )
        val selectedCandidate =
            explicitModel
                ?: observedModel

        fun holdUnsupported(
            model: HereticModelCatalog.Entry,
        ): Observed =
            observed(
                current = current,
                location = location,
                model = model,
                state =
                    VexLifeProdEntryFamily.ModelTargetHeld(
                        desiredSourceSha = desiredSourceSha,
                        modelRef = model.modelRef,
                        targetRef = target.targetRef,
                    ),
            )

        if (
            selectedCandidate != null &&
            !HereticRuntimeRealization.supports(
                modelRef = selectedCandidate.modelRef,
                target = target,
            )
        ) {
            return holdUnsupported(
                selectedCandidate,
            )
        }

        val model =
            VexLifeProdSetupModel.resolveObserved(
                explicit = explicitModelHandle,
                observedModelRef = current.activeModelRef,
                target = target,
                totalMemoryBytes = current.totalMemoryBytes,
            )

        fun state(
            value: VexLifeProdEntryFamily.EntryState,
            sourceSha: String? = null,
            pid: Long? = null,
            detail: String? = null,
            storage: HereticSetupStorage.Requirement? = null,
        ): Observed =
            observed(
                current = current,
                location = location,
                model = model,
                state = value,
                sourceSha = sourceSha,
                pid = pid,
                detail = detail,
                storage = storage,
            )

        if (
            current.homeDisposition !=
            location.disposition.wireValue
        ) {
            val detail =
                "the selected Home changed while VexLife was looking (" +
                    current.homeDisposition +
                    " then " +
                    location.disposition.wireValue +
                    ")"
            return state(
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
                reasonRef =
                    current.hostEligibilityReason
                        ?: "HOST_UNSUPPORTED",
                observedHardware = current.chipModel,
                totalMemoryBytes = current.totalMemoryBytes,
                requiredHardware = hostRequirement.requiredHardware,
                requiredMemoryBytes = hostRequirement.requiredMemoryBytes,
                homePresent = homePresent,
            )

        when (location) {
            is VexLifeProdHomeFiles.Location.Formable -> {
                require(
                    current.homeRef == null &&
                        current.deviceRef == null &&
                        current.companionLineageRef == null,
                ) {
                    "absent Home current-state projection cannot claim Home/device/Companion identity"
                }
                return state(
                    if (hostEligible) {
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
                return state(
                    VexLifeProdEntryFamily.HomeHeld(
                        desiredSourceSha = desiredSourceSha,
                        disposition = location.disposition,
                        detail = location.detail,
                    ),
                )

            is VexLifeProdHomeFiles.Location.Existing ->
                Unit
        }

        when (service) {
            is Service.HeldUnknown ->
                return state(
                    VexLifeProdEntryFamily.HeldUnknown(
                        desiredSourceSha = desiredSourceSha,
                        detail = service.detail,
                    ),
                    detail = service.detail,
                )

            is Service.RunningOwned ->
                if (
                    current.modelSelectionState !=
                    "HERETIC_ESTABLISHMENT_PENDING"
                ) {
                    return state(
                        if (
                            service.sourceSha ==
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
                        sourceSha = service.sourceSha,
                        pid = service.pid,
                    )
                }

            Service.Absent,
            is Service.OwnedNotReady ->
                Unit
        }

        if (!hostEligible) {
            return state(
                hostHeld(
                    homePresent = true,
                ),
            )
        }

        if (
            current.modelArtifactState ==
            "INVALID_SIZE"
        ) {
            return state(
                VexLifeProdEntryFamily.ModelFilesHeld(
                    desiredSourceSha = desiredSourceSha,
                    invalidBytes =
                        current.modelArtifactInvalidSizeBytes
                            ?: 0L,
                ),
            )
        }

        val modelReady =
            current.modelSelectionState in
                setOf(
                    "HERETIC_ESTABLISHMENT_PENDING",
                    "BOUND_HERETIC_PRODUCT_MODEL",
                ) &&
                current.modelArtifactState ==
                "COMPLETE_BY_SIZE_NOT_HASHED"

        if (
            !modelReady ||
            !runtimePrerequisitesPresent
        ) {
            val missingModelBytes =
                current.modelArtifactMissingBytes
                    ?: model.totalBytes
            val storage =
                HereticSetupStorage.requirement(
                    missingModelBytes = missingModelBytes,
                    observedFreeBytes = current.freeStorageBytes,
                )

            return state(
                if (
                    storage.shortfallObserved
                ) {
                    VexLifeProdEntryFamily.StorageHeld(
                        desiredSourceSha = desiredSourceSha,
                        requiredFreeBytes = storage.requiredFreeBytes,
                        freeBytes =
                            requireNotNull(
                                current.freeStorageBytes,
                            ),
                    )
                } else {
                    VexLifeProdEntryFamily.SetupRequired(
                        desiredSourceSha,
                    )
                },
                storage = storage,
            )
        }

        if (
            service is
            Service.RunningOwned
        ) {
            return state(
                if (
                    service.sourceSha ==
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
                sourceSha = service.sourceSha,
                pid = service.pid,
            )
        }

        return when (service) {
            Service.Absent ->
                state(
                    VexLifeProdEntryFamily.StartRequired(
                        desiredSourceSha = desiredSourceSha,
                        priorServiceSourceSha = null,
                    ),
                )

            is Service.OwnedNotReady ->
                if (
                    service.substrateAbsent
                ) {
                    state(
                        VexLifeProdEntryFamily.StartRequired(
                            desiredSourceSha = desiredSourceSha,
                            priorServiceSourceSha = service.sourceSha,
                        ),
                        sourceSha = service.sourceSha,
                    )
                } else {
                    state(
                        if (
                            service.sourceSha ==
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
                        sourceSha = service.sourceSha,
                        pid = service.pid,
                    )
                }

            is Service.RunningOwned,
            is Service.HeldUnknown ->
                error(
                    "running/held service observation was already mapped",
                )
        }
    }

    private fun observed(
        current: VexLifeProdServiceContract.CurrentStateProjection,
        location: VexLifeProdHomeFiles.Location,
        model: HereticModelCatalog.Entry,
        state: VexLifeProdEntryFamily.EntryState,
        sourceSha: String? = null,
        pid: Long? = null,
        detail: String? = null,
        storage: HereticSetupStorage.Requirement? = null,
    ): Observed =
        Observed(
            currentState = current,
            location = location,
            entryState = state,
            currentServiceSourceSha = sourceSha,
            currentServicePid = pid,
            serviceDetail = detail,
            storage = storage,
            model = model,
        )
}

// [VXG RealForever]
