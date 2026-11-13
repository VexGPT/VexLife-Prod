package vexlife.entry

import kotlinx.serialization.Serializable
import vexlife.semantic.VexFamilyContract

/**
 * Portable Product-entry semantics.
 *
 * Platform adapters observe their own process/lifecycle truth and map it into [EntryState]. Renderers
 * consume [EntryDisplayState] and emit [EntryControl]. Only this Family decides which Effect a
 * Control means under the current State.
 *
 * Platform process syntax, launchd/Windows service mechanics, Home, model acquisition and browser
 * opening remain outside this semantic owner.
 */
class VexLifeProdEntryFamily private constructor() :
    VexFamilyContract<
        VexLifeProdEntryFamily,
        VexLifeProdEntryFamily.EntryState,
        VexLifeProdEntryFamily.EntryControl,
        VexLifeProdEntryFamily.EntryEffect
    >() {

    sealed interface EntryState :
        VexFamilyContract.State<VexLifeProdEntryFamily> {
        val desiredSourceSha: String
        val stateRef: String
    }

    data class Current(
        override val desiredSourceSha: String,
    ) : EntryState {
        override val stateRef: String =
            StateCurrent
    }

    data class UpdateAvailable(
        override val desiredSourceSha: String,
    ) : EntryState {
        override val stateRef: String =
            StateUpdateAvailable
    }

    data class RepairRequired(
        override val desiredSourceSha: String,
    ) : EntryState {
        override val stateRef: String =
            StateRepairRequired
    }

    data class UpdateRepairRequired(
        override val desiredSourceSha: String,
    ) : EntryState {
        override val stateRef: String =
            StateUpdateRepairRequired
    }

    data class HomeFormationRequired(
        override val desiredSourceSha: String,
    ) : EntryState {
        override val stateRef: String =
            StateHomeFormationRequired
    }

    data class SetupRequired(
        override val desiredSourceSha: String,
    ) : EntryState {
        override val stateRef: String =
            StateSetupRequired
    }

    data class HeldUnknown(
        override val desiredSourceSha: String,
        val detail: String,
    ) : EntryState {
        override val stateRef: String =
            StateHeldUnknown

        init {
            require(
                detail.isNotBlank(),
            ) {
                "held Product-entry state requires bounded detail"
            }
        }
    }

    sealed interface EntryControl :
        VexFamilyContract.Control<VexLifeProdEntryFamily> {
        val controlRef: String
    }

    data object OpenCurrent : EntryControl {
        override val controlRef: String =
            ControlOpenCurrent
    }

    data object ConvergeDesired : EntryControl {
        override val controlRef: String =
            ControlConvergeDesired
    }

    data object FormHome : EntryControl {
        override val controlRef: String =
            ControlFormHome
    }

    data object SetUp : EntryControl {
        override val controlRef: String =
            ControlSetUp
    }

    data object Cancel : EntryControl {
        override val controlRef: String =
            ControlCancel
    }

    sealed interface EntryEffect :
        VexFamilyContract.Effect<VexLifeProdEntryFamily> {
        val effectRef: String
    }

    data object OpenPresentation : EntryEffect {
        override val effectRef: String =
            EffectOpenPresentation
    }

    data object FormCanonicalHome : EntryEffect {
        override val effectRef: String =
            EffectFormCanonicalHome
    }

    data class ConvergeProduct(
        val desiredSourceSha: String,
        val allowAcquisition: Boolean,
    ) : EntryEffect {
        override val effectRef: String =
            EffectConvergeProduct
    }

    @Serializable
    data class EntryDisplayState(
        val title: String,
        val detail: String,
        val primaryControlRef: String?,
        val primaryLabel: String?,
        val secondaryControlRef: String?,
        val secondaryLabel: String?,
        val cancelLabel: String,
    ) :
        VexFamilyContract.DisplayState<
            VexLifeProdEntryFamily,
            EntryState
        > {
        init {
            require(
                title.isNotBlank() &&
                    detail.isNotBlank() &&
                    cancelLabel.isNotBlank() &&
                    (
                        (
                            primaryControlRef ==
                                null
                        ) ==
                            (
                                primaryLabel ==
                                    null
                            )
                    ) &&
                    (
                        (
                            secondaryControlRef ==
                                null
                        ) ==
                            (
                                secondaryLabel ==
                                    null
                            )
                    ),
            ) {
                "Product-entry DisplayState is invalid"
            }
        }
    }

    override fun act(
        currentState: EntryState,
        control: EntryControl,
    ): Act<
        VexLifeProdEntryFamily,
        EntryState,
        EntryEffect
    >? =
        when (
            currentState
        ) {
            is Current ->
                when (
                    control
                ) {
                    OpenCurrent ->
                        stay(
                            OpenPresentation,
                        )

                    Cancel ->
                        stay()

                    else ->
                        null
                }

            is UpdateAvailable ->
                when (
                    control
                ) {
                    OpenCurrent ->
                        stay(
                            OpenPresentation,
                        )

                    ConvergeDesired ->
                        stay(
                            ConvergeProduct(
                                desiredSourceSha =
                                    currentState.desiredSourceSha,
                                allowAcquisition = false,
                            ),
                        )

                    Cancel ->
                        stay()

                    else ->
                        null
                }

            is RepairRequired,
            is UpdateRepairRequired,
            ->
                when (
                    control
                ) {
                    ConvergeDesired ->
                        stay(
                            ConvergeProduct(
                                desiredSourceSha =
                                    currentState.desiredSourceSha,
                                allowAcquisition = false,
                            ),
                        )

                    Cancel ->
                        stay()

                    else ->
                        null
                }

            is HomeFormationRequired ->
                when (
                    control
                ) {
                    FormHome ->
                        stay(
                            FormCanonicalHome,
                        )

                    Cancel ->
                        stay()

                    else ->
                        null
                }

            is SetupRequired ->
                when (
                    control
                ) {
                    SetUp ->
                        stay(
                            ConvergeProduct(
                                desiredSourceSha =
                                    currentState.desiredSourceSha,
                                allowAcquisition = true,
                            ),
                        )

                    Cancel ->
                        stay()

                    else ->
                        null
                }

            is HeldUnknown ->
                when (
                    control
                ) {
                    Cancel ->
                        stay()

                    else ->
                        null
                }
        }

    override fun displayStateFor(
        state: EntryState,
    ): EntryDisplayState =
        when (
            state
        ) {
            is Current ->
                EntryDisplayState(
                    title = "VexLife is current.",
                    detail =
                        "The exact desired Product service is already current. Opening it performs no service replacement.",
                    primaryControlRef = ControlOpenCurrent,
                    primaryLabel = "Open VexLife",
                    secondaryControlRef = null,
                    secondaryLabel = null,
                    cancelLabel = "Cancel",
                )

            is UpdateAvailable ->
                EntryDisplayState(
                    title = "A newer VexLife Product source is available.",
                    detail =
                        "Your Home and continuity remain preserved. Update & Restart may retire only the exact owned prior Product service.",
                    primaryControlRef = ControlConvergeDesired,
                    primaryLabel = "Update & Restart",
                    secondaryControlRef = ControlOpenCurrent,
                    secondaryLabel = "Open Current",
                    cancelLabel = "Cancel",
                )

            is RepairRequired ->
                EntryDisplayState(
                    title = "VexLife needs repair.",
                    detail =
                        "The exact owned Product service is not current for reuse. Repair & Restart preserves Home and continuity while reconverging the service.",
                    primaryControlRef = ControlConvergeDesired,
                    primaryLabel = "Repair & Restart",
                    secondaryControlRef = null,
                    secondaryLabel = null,
                    cancelLabel = "Cancel",
                )

            is UpdateRepairRequired ->
                EntryDisplayState(
                    title = "VexLife can update and repair the Product service.",
                    detail =
                        "An exact owned prior Product service is not current for reuse. Update & Restart converges to the desired source without granting acquisition authority.",
                    primaryControlRef = ControlConvergeDesired,
                    primaryLabel = "Update & Restart",
                    secondaryControlRef = null,
                    secondaryLabel = null,
                    cancelLabel = "Cancel",
                )

            is HomeFormationRequired ->
                EntryDisplayState(
                    title = "Create your Vex Home.",
                    detail =
                        "No canonical VexLife Home exists yet. This creates only your local Vex Home, device identity, and Companion lineage. It does not download or start a model.",
                    primaryControlRef = ControlFormHome,
                    primaryLabel = "Create Vex Home",
                    secondaryControlRef = null,
                    secondaryLabel = null,
                    cancelLabel = "Cancel",
                )

            is SetupRequired ->
                EntryDisplayState(
                    title = "Set up VexLife.",
                    detail =
                        "Your Vex Home exists, but no owned Product service is established. Continue only after reviewing the model/runtime requirements shown here.",
                    primaryControlRef = ControlSetUp,
                    primaryLabel = "Set Up VexLife",
                    secondaryControlRef = null,
                    secondaryLabel = null,
                    cancelLabel = "Cancel",
                )

            is HeldUnknown ->
                EntryDisplayState(
                    title = "VexLife will not replace the current process.",
                    detail =
                        "Current Product-service ownership is not exact. Preserve the process and inspect ownership before any retirement or replacement.",
                    primaryControlRef = null,
                    primaryLabel = null,
                    secondaryControlRef = null,
                    secondaryLabel = null,
                    cancelLabel = "Close",
                )
        }

    companion object {
        const val StateCurrent =
            "CURRENT"
        const val StateUpdateAvailable =
            "UPDATE_AVAILABLE"
        const val StateRepairRequired =
            "REPAIR_REQUIRED"
        const val StateUpdateRepairRequired =
            "UPDATE_REPAIR_REQUIRED"
        const val StateHomeFormationRequired =
            "HOME_FORMATION_REQUIRED"
        const val StateSetupRequired =
            "SETUP_REQUIRED"
        const val StateHeldUnknown =
            "HOLD_UNKNOWN"

        const val ControlOpenCurrent =
            "OPEN_CURRENT"
        const val ControlConvergeDesired =
            "CONVERGE_DESIRED"
        const val ControlFormHome =
            "FORM_HOME"
        const val ControlSetUp =
            "SET_UP"
        const val ControlCancel =
            "CANCEL"

        const val EffectOpenPresentation =
            "OPEN_PRESENTATION"
        const val EffectFormCanonicalHome =
            "FORM_CANONICAL_HOME"
        const val EffectConvergeProduct =
            "CONVERGE_PRODUCT"

        val Semantics =
            VexLifeProdEntryFamily()

        fun controlForRef(
            controlRef: String,
        ): EntryControl? =
            when (
                controlRef
            ) {
                ControlOpenCurrent ->
                    OpenCurrent

                ControlConvergeDesired ->
                    ConvergeDesired

                ControlFormHome ->
                    FormHome

                ControlSetUp ->
                    SetUp

                ControlCancel ->
                    Cancel

                else ->
                    null
            }
    }
}

// [VXG RealForever]
