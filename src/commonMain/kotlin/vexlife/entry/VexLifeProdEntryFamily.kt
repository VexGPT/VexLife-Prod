package vexlife.entry

import kotlinx.serialization.Serializable
import vexlife.home.VexLifeProdHome
import vexlife.semantic.VexFamilyContract

/**
 * Portable Product-entry semantics.
 *
 * Platform adapters observe their own process/lifecycle truth and map it into [EntryState].
 * Renderers consume [EntryDisplayState] and emit [EntryControl]. Only this Family decides which
 * Effect a Control means under the current State.
 *
 * Platform process syntax, launchd/Windows service mechanics, Home, model acquisition and browser
 * opening remain outside this semantic owner.
 *
 * Held States ([HostHeld], [HomeHeld], [StorageHeld], [ModelFilesHeld], [HeldUnknown]) admit no
 * Effect. They exist so the human surface can explain *why* nothing is offered instead of offering
 * a Control whose realization would fail mid-effect (predecessor HOST_ELIGIBILITY_HELD /
 * HELD_NONCANONICAL_HOME parity).
 *
 * [EntryDisplayState.journey] is the Setup Journey rail: a projection of this State, never a second
 * step ledger. A rail position is re-derived from fresh observation every time.
 */
class VexLifeProdEntryFamily private constructor() :
    VexFamilyContract<
        VexLifeProdEntryFamily,
        VexLifeProdEntryFamily.EntryState,
        VexLifeProdEntryFamily.EntryControl,
        VexLifeProdEntryFamily.EntryEffect,
    >() {

    sealed interface EntryState : VexFamilyContract.State<VexLifeProdEntryFamily> {
        val desiredSourceSha: String
        val stateRef: String
    }

    data class Current(override val desiredSourceSha: String) : EntryState {
        override val stateRef: String = StateCurrent
    }

    data class UpdateAvailable(override val desiredSourceSha: String) : EntryState {
        override val stateRef: String = StateUpdateAvailable
    }

    data class RepairRequired(override val desiredSourceSha: String) : EntryState {
        override val stateRef: String = StateRepairRequired
    }

    data class UpdateRepairRequired(override val desiredSourceSha: String) : EntryState {
        override val stateRef: String = StateUpdateRepairRequired
    }

    /**
     * Home and every model/runtime prerequisite are present; the Product service is simply not
     * running (for example after a restart of the Mac). Starting needs no acquisition authority.
     *
     * [priorServiceSourceSha] is the exact source recorded by a stopped owned receipt, when one
     * exists, so the human can be told that starting also moves to a newer version.
     */
    data class StartRequired(
        override val desiredSourceSha: String,
        val priorServiceSourceSha: String?,
    ) : EntryState {
        override val stateRef: String = StateStartRequired
    }

    data class HomeFormationRequired(override val desiredSourceSha: String) : EntryState {
        override val stateRef: String = StateHomeFormationRequired
    }

    /** The selected coordinate exists but cannot safely become or be read as a Vex Home. */
    data class HomeHeld(
        override val desiredSourceSha: String,
        val disposition: VexLifeProdHome.LocationDisposition,
        val detail: String,
    ) : EntryState {
        override val stateRef: String = StateHomeHeld

        init {
            require(disposition.held && detail.isNotBlank()) {
                "held Home entry state requires one held disposition and bounded detail"
            }
        }
    }

    /**
     * The certified host profile does not admit this Mac for Home formation / model acquisition /
     * runtime start. [homePresent] lets the journey show an existing Home as preserved.
     */
    data class HostHeld(
        override val desiredSourceSha: String,
        val reasonRef: String,
        val chipModel: String?,
        val totalMemoryBytes: Long?,
        val requiredChipModel: String,
        val requiredMemoryBytes: Long,
        val homePresent: Boolean,
    ) : EntryState {
        override val stateRef: String = StateHostHeld

        init {
            require(
                reasonRef.isNotBlank() && requiredChipModel.isNotBlank() && requiredMemoryBytes > 0
            ) {
                "held host entry state requires one reason and the certified requirement it failed"
            }
        }
    }

    data class SetupRequired(override val desiredSourceSha: String) : EntryState {
        override val stateRef: String = StateSetupRequired
    }

    /**
     * Acquisition is needed but the selected Home's volume does not have the required free space.
     */
    data class StorageHeld(
        override val desiredSourceSha: String,
        val requiredFreeBytes: Long,
        val freeBytes: Long,
    ) : EntryState {
        override val stateRef: String = StateStorageHeld

        init {
            require(requiredFreeBytes > freeBytes && freeBytes >= 0) {
                "storage hold requires observed free space below the requirement"
            }
        }
    }

    /**
     * Existing model files fail exact size verification. The materialization owner preserves them
     * and requires an explicit repair owner before reacquisition; setup must not offer acquisition
     * that would stop on them.
     */
    data class ModelFilesHeld(override val desiredSourceSha: String, val invalidBytes: Long) :
        EntryState {
        override val stateRef: String = StateModelFilesHeld
    }

    data class HeldUnknown(override val desiredSourceSha: String, val detail: String) : EntryState {
        override val stateRef: String = StateHeldUnknown

        init {
            require(detail.isNotBlank()) {
                "held Product-entry state requires bounded detail"
            }
        }
    }

    sealed interface EntryControl : VexFamilyContract.Control<VexLifeProdEntryFamily> {
        val controlRef: String
    }

    data object OpenCurrent : EntryControl {
        override val controlRef: String = ControlOpenCurrent
    }

    data object ConvergeDesired : EntryControl {
        override val controlRef: String = ControlConvergeDesired
    }

    data object Start : EntryControl {
        override val controlRef: String = ControlStart
    }

    data object FormHome : EntryControl {
        override val controlRef: String = ControlFormHome
    }

    data object SetUp : EntryControl {
        override val controlRef: String = ControlSetUp
    }

    data object Cancel : EntryControl {
        override val controlRef: String = ControlCancel
    }

    sealed interface EntryEffect : VexFamilyContract.Effect<VexLifeProdEntryFamily> {
        val effectRef: String
    }

    data object OpenPresentation : EntryEffect {
        override val effectRef: String = EffectOpenPresentation
    }

    data object FormCanonicalHome : EntryEffect {
        override val effectRef: String = EffectFormCanonicalHome
    }

    /**
     * [allowAcquisition] authorizes downloading/creating missing model + runtime prerequisites.
     * [allowRuntimeStart] authorizes starting the exact local runtime from prerequisites that are
     * already present. Acquisition implies start; start never implies acquisition.
     */
    data class ConvergeProduct(
        val desiredSourceSha: String,
        val allowAcquisition: Boolean,
        val allowRuntimeStart: Boolean,
    ) : EntryEffect {
        override val effectRef: String = EffectConvergeProduct

        init {
            require(!allowAcquisition || allowRuntimeStart) {
                "acquisition authority without runtime-start authority is not lawful"
            }
        }
    }

    @Serializable
    data class JourneyStep(val stepRef: String, val label: String, val statusRef: String) {
        init {
            require(
                stepRef in JourneyStepRefs && label.isNotBlank() && statusRef in JourneyStatusRefs
            ) {
                "Setup Journey step is invalid"
            }
        }
    }

    /**
     * [homeChoiceLabel] offers renderer-local navigation back to the Home chooser. Choosing a
     * different coordinate is not a semantic Effect; it changes what will be observed next.
     * [recheckLabel] offers renderer-local re-observation for held States whose cause the person
     * can change outside VexLife (free space, folder contents, another running copy).
     */
    @Serializable
    data class EntryDisplayState(
        val title: String,
        val detail: String,
        val primaryControlRef: String?,
        val primaryLabel: String?,
        val secondaryControlRef: String?,
        val secondaryLabel: String?,
        val cancelLabel: String,
        val journey: List<JourneyStep>,
        val homeChoiceLabel: String?,
        val recheckLabel: String?,
    ) :
        VexFamilyContract.DisplayState<
            VexLifeProdEntryFamily,
            EntryState,
        > {
        init {
            require(
                title.isNotBlank() &&
                    detail.isNotBlank() &&
                    cancelLabel.isNotBlank() &&
                    ((primaryControlRef == null) == (primaryLabel == null)) &&
                    ((secondaryControlRef == null) == (secondaryLabel == null)) &&
                    journey.map {
                        it.stepRef
                    } == JourneyStepRefs &&
                    journey.count {
                        it.statusRef == JourneyCurrent || it.statusRef == JourneyHeld
                    } <= 1 &&
                    homeChoiceLabel?.isNotBlank() != false &&
                    recheckLabel?.isNotBlank() != false
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
        EntryEffect,
    >? =
        when (currentState) {
            is Current ->
                when (control) {
                    OpenCurrent -> stay(OpenPresentation)

                    Cancel -> stay()

                    else -> null
                }

            is UpdateAvailable ->
                when (control) {
                    OpenCurrent -> stay(OpenPresentation)

                    ConvergeDesired ->
                        stay(restartWithoutAcquisition(currentState.desiredSourceSha))

                    Cancel -> stay()

                    else -> null
                }

            is RepairRequired,
            is UpdateRepairRequired ->
                when (control) {
                    ConvergeDesired ->
                        stay(restartWithoutAcquisition(currentState.desiredSourceSha))

                    Cancel -> stay()

                    else -> null
                }

            is StartRequired ->
                when (control) {
                    Start -> stay(restartWithoutAcquisition(currentState.desiredSourceSha))

                    Cancel -> stay()

                    else -> null
                }

            is HomeFormationRequired ->
                when (control) {
                    FormHome -> stay(FormCanonicalHome)

                    Cancel -> stay()

                    else -> null
                }

            is SetupRequired ->
                when (control) {
                    SetUp ->
                        stay(
                            ConvergeProduct(
                                desiredSourceSha = currentState.desiredSourceSha,
                                allowAcquisition = true,
                                allowRuntimeStart = true,
                            )
                        )

                    Cancel -> stay()

                    else -> null
                }

            is HomeHeld,
            is HostHeld,
            is StorageHeld,
            is ModelFilesHeld,
            is HeldUnknown ->
                when (control) {
                    Cancel -> stay()

                    else -> null
                }
        }

    private fun restartWithoutAcquisition(desiredSourceSha: String): ConvergeProduct =
        ConvergeProduct(
            desiredSourceSha = desiredSourceSha,
            allowAcquisition = false,
            allowRuntimeStart = true,
        )

    override fun displayStateFor(state: EntryState): EntryDisplayState =
        when (state) {
            is Current ->
                display(
                    title = "VexLife is running.",
                    detail =
                        "Everything is current. Opening VexLife does not restart or replace anything.",
                    primary = ControlOpenCurrent to "Open VexLife",
                    journey = journey(Complete, Complete, Complete, Complete, JourneyCurrent),
                )

            is UpdateAvailable ->
                display(
                    title = "A newer version of VexLife is ready.",
                    detail =
                        "Your Home, conversations and memory stay as they are. Update & Restart stops only " +
                            "the exact VexLife service this Home owns and starts the newer one. Nothing will be downloaded.",
                    primary = ControlConvergeDesired to "Update & Restart",
                    secondary = ControlOpenCurrent to "Open Current",
                    journey = journey(Complete, Complete, Complete, JourneyCurrent, Upcoming),
                )

            is RepairRequired ->
                display(
                    title = "VexLife isn't responding.",
                    detail =
                        "VexLife's background service is registered but not answering. Repair & Restart " +
                            "re-checks the local model files, then starts the local model and VexLife again. " +
                            "Your Home, conversations and memory stay as they are. Nothing will be downloaded.",
                    primary = ControlConvergeDesired to "Repair & Restart",
                    journey = journey(Complete, Complete, Complete, JourneyCurrent, Upcoming),
                )

            is UpdateRepairRequired ->
                display(
                    title = "VexLife can update and restart.",
                    detail =
                        "An earlier VexLife service from this Home isn't answering. Update & Restart re-checks " +
                            "the local model files and starts this newer version. Nothing will be downloaded.",
                    primary = ControlConvergeDesired to "Update & Restart",
                    journey = journey(Complete, Complete, Complete, JourneyCurrent, Upcoming),
                )

            is StartRequired ->
                display(
                    title = "Start VexLife.",
                    detail =
                        "Your Vex Home and local model are already on this Mac. Starting checks those files, " +
                            "then starts the local model and VexLife. Nothing will be downloaded." +
                            if (
                                state.priorServiceSourceSha != null &&
                                    state.priorServiceSourceSha != state.desiredSourceSha
                            ) {
                                " This also moves VexLife to the newer version you just opened."
                            } else {
                                ""
                            },
                    primary = ControlStart to "Start VexLife",
                    journey = journey(Complete, Complete, Complete, JourneyCurrent, Upcoming),
                )

            is HomeFormationRequired ->
                display(
                    title = "Create your Vex Home.",
                    detail =
                        "VexLife keeps your identity, conversations and memory together in one folder on " +
                            "this Mac: your Vex Home. Creating it does not download or start anything; you'll " +
                            "review the local model as a separate step.",
                    primary = ControlFormHome to "Create Vex Home",
                    journey = journey(Complete, JourneyCurrent, Upcoming, Upcoming, Upcoming),
                    homeChoice = true,
                )

            is HomeHeld ->
                display(
                    title = "This folder is being preserved.",
                    detail =
                        homeHeldExplanation(state.disposition) +
                            " Nothing in it was changed, moved or deleted.",
                    journey = journey(Complete, JourneyHeld, Upcoming, Upcoming, Upcoming),
                    homeChoice = true,
                    recheck = true,
                    cancelLabel = "Close",
                )

            is HostHeld ->
                display(
                    title = "This Mac can't run VexLife's local model yet.",
                    detail =
                        hostHeldExplanation(state) +
                            if (state.homePresent) {
                                " Your existing Vex Home was left untouched."
                            } else {
                                " Nothing was created or downloaded."
                            },
                    journey =
                        journey(
                            JourneyHeld,
                            if (state.homePresent) Complete else Upcoming,
                            Upcoming,
                            Upcoming,
                            Upcoming,
                        ),
                    cancelLabel = "Close",
                )

            is SetupRequired ->
                display(
                    title = "Set up the local model.",
                    detail =
                        "Your Vex Home is ready. Next, VexLife downloads its verified local model and runtime " +
                            "into your Home and starts VexLife. Review the size and requirements before you continue. " +
                            "If the download is interrupted, finished parts are kept and it resumes next time.",
                    primary = ControlSetUp to "Set Up VexLife",
                    journey = journey(Complete, Complete, JourneyCurrent, Upcoming, Upcoming),
                )

            is StorageHeld ->
                display(
                    title = "Not enough free space for the local model.",
                    detail =
                        "VexLife needs about ${gib(state.requiredFreeBytes)} GiB free on the drive that holds " +
                            "your Vex Home before it can download the local model. That drive has " +
                            "${gib(state.freeBytes)} GiB free. Free up space and choose Check Again, or choose a " +
                            "location on another drive. Nothing was downloaded.",
                    journey = journey(Complete, Complete, JourneyHeld, Upcoming, Upcoming),
                    homeChoice = true,
                    recheck = true,
                    cancelLabel = "Close",
                )

            is ModelFilesHeld ->
                display(
                    title = "A local model file needs attention.",
                    detail =
                        "Some model files in your Vex Home are not the size VexLife expects " +
                            "(${gib(state.invalidBytes)} GiB affected). VexLife keeps them and will not delete or " +
                            "replace them on its own. If you moved or edited files inside your Vex Home, put them " +
                            "back and choose Check Again. Guided model-file repair is not part of this setup yet.",
                    journey = journey(Complete, Complete, JourneyHeld, Upcoming, Upcoming),
                    recheck = true,
                    cancelLabel = "Close",
                )

            is HeldUnknown ->
                display(
                    title = "VexLife will not replace the current process.",
                    detail =
                        "Something is already using VexLife's background service on this Mac, and VexLife " +
                            "cannot prove it belongs to the Home you chose. To keep your data safe it will not stop " +
                            "or replace it. If you already have a Vex Home somewhere else, choose that location. " +
                            "Otherwise quit the other VexLife and choose Check Again.",
                    journey = journey(Complete, Complete, Upcoming, JourneyHeld, Upcoming),
                    homeChoice = true,
                    recheck = true,
                    cancelLabel = "Close",
                )
        }

    private fun display(
        title: String,
        detail: String,
        journey: List<JourneyStep>,
        primary: Pair<String, String>? = null,
        secondary: Pair<String, String>? = null,
        homeChoice: Boolean = false,
        recheck: Boolean = false,
        cancelLabel: String = "Cancel",
    ): EntryDisplayState =
        EntryDisplayState(
            title = title,
            detail = detail,
            primaryControlRef = primary?.first,
            primaryLabel = primary?.second,
            secondaryControlRef = secondary?.first,
            secondaryLabel = secondary?.second,
            cancelLabel = cancelLabel,
            journey = journey,
            homeChoiceLabel = if (homeChoice) "Choose Another Location…" else null,
            recheckLabel = if (recheck) "Check Again" else null,
        )

    private fun journey(
        mac: String,
        home: String,
        model: String,
        start: String,
        ready: String,
    ): List<JourneyStep> =
        listOf(
            JourneyStep(JourneyStepMac, "This Mac", mac),
            JourneyStep(JourneyStepHome, "Vex Home", home),
            JourneyStep(JourneyStepModel, "Local model", model),
            JourneyStep(JourneyStepStart, "Start VexLife", start),
            JourneyStep(JourneyStepReady, "Ready", ready),
        )

    private fun homeHeldExplanation(disposition: VexLifeProdHome.LocationDisposition): String =
        when (disposition) {
            VexLifeProdHome.LocationDisposition.HeldNoncanonicalContent ->
                "The folder you chose already contains other files, so VexLife will not use it as a Vex Home. " +
                    "Choose an empty folder, or type a new folder name."

            VexLifeProdHome.LocationDisposition.HeldInvalidCanonicalRecords ->
                "This folder looks like a Vex Home, but its identity records did not check out. VexLife will not " +
                    "repair or overwrite it automatically. Keep it safe for recovery, or choose another location."

            VexLifeProdHome.LocationDisposition.HeldNotDirectory ->
                "The location you chose is a file (or a file is in the way), not a folder. Choose a folder location."

            VexLifeProdHome.LocationDisposition.HeldSymbolicLink ->
                "The location you chose is a link to somewhere else. Choose the real folder so your Home has one " +
                    "exact location."

            VexLifeProdHome.LocationDisposition.HeldLocationNotWritable ->
                "This Mac user cannot create folders there. Choose a location inside your own user folder, such " +
                    "as the suggested ~/.vexlife."

            VexLifeProdHome.LocationDisposition.HeldLocationUnreadable ->
                "VexLife could not read that location. If macOS asked for permission, you can allow it and choose " +
                    "Check Again, or choose another location."

            VexLifeProdHome.LocationDisposition.Absent,
            VexLifeProdHome.LocationDisposition.EmptyDirectory,
            VexLifeProdHome.LocationDisposition.Canonical ->
                error("HomeHeld admits only held dispositions")
        }

    private fun hostHeldExplanation(state: HostHeld): String {
        val requirement =
            "VexLife's local model currently runs on Macs with an ${state.requiredChipModel} chip and at least " +
                "${gib(state.requiredMemoryBytes)} GiB of memory."
        val observed =
            "This Mac: " +
                (state.chipModel ?: "unknown chip") +
                ", " +
                (state.totalMemoryBytes?.let { gib(it) + " GiB memory" } ?: "unknown memory") +
                "."
        val reason =
            when (state.reasonRef) {
                "PLATFORM_UNSUPPORTED" -> "VexLife setup currently runs on macOS only."

                "ARCHITECTURE_UNSUPPORTED" -> "This Mac does not use Apple silicon."

                "SYSTEM_MEMORY_UNKNOWN" -> "VexLife could not read how much memory this Mac has."

                "INSUFFICIENT_SYSTEM_MEMORY" ->
                    "This Mac has less memory than the local model needs."

                else -> "This Mac's chip is not yet certified for the local model."
            }
        return "$reason $requirement $observed"
    }

    companion object {
        const val StateCurrent = "CURRENT"
        const val StateUpdateAvailable = "UPDATE_AVAILABLE"
        const val StateRepairRequired = "REPAIR_REQUIRED"
        const val StateUpdateRepairRequired = "UPDATE_REPAIR_REQUIRED"
        const val StateStartRequired = "START_REQUIRED"
        const val StateHomeFormationRequired = "HOME_FORMATION_REQUIRED"
        const val StateHomeHeld = "HOME_HELD"
        const val StateHostHeld = "HOST_HELD"
        const val StateSetupRequired = "SETUP_REQUIRED"
        const val StateStorageHeld = "STORAGE_HELD"
        const val StateModelFilesHeld = "MODEL_FILES_HELD"
        const val StateHeldUnknown = "HOLD_UNKNOWN"

        val StateRefs =
            setOf(
                StateCurrent,
                StateUpdateAvailable,
                StateRepairRequired,
                StateUpdateRepairRequired,
                StateStartRequired,
                StateHomeFormationRequired,
                StateHomeHeld,
                StateHostHeld,
                StateSetupRequired,
                StateStorageHeld,
                StateModelFilesHeld,
                StateHeldUnknown,
            )

        const val ControlOpenCurrent = "OPEN_CURRENT"
        const val ControlConvergeDesired = "CONVERGE_DESIRED"
        const val ControlStart = "START"
        const val ControlFormHome = "FORM_HOME"
        const val ControlSetUp = "SET_UP"
        const val ControlCancel = "CANCEL"

        const val EffectOpenPresentation = "OPEN_PRESENTATION"
        const val EffectFormCanonicalHome = "FORM_CANONICAL_HOME"
        const val EffectConvergeProduct = "CONVERGE_PRODUCT"

        const val JourneyStepMac = "MAC"
        const val JourneyStepHome = "HOME"
        const val JourneyStepModel = "MODEL"
        const val JourneyStepStart = "START"
        const val JourneyStepReady = "READY"

        val JourneyStepRefs =
            listOf(
                JourneyStepMac,
                JourneyStepHome,
                JourneyStepModel,
                JourneyStepStart,
                JourneyStepReady,
            )

        const val Complete = "COMPLETE"
        const val JourneyCurrent = "CURRENT"
        const val Upcoming = "UPCOMING"
        const val JourneyHeld = "HELD"

        val JourneyStatusRefs = setOf(Complete, JourneyCurrent, Upcoming, JourneyHeld)

        val Semantics = VexLifeProdEntryFamily()

        fun controlForRef(controlRef: String): EntryControl? =
            when (controlRef) {
                ControlOpenCurrent -> OpenCurrent

                ControlConvergeDesired -> ConvergeDesired

                ControlStart -> Start

                ControlFormHome -> FormHome

                ControlSetUp -> SetUp

                ControlCancel -> Cancel

                else -> null
            }

        /** Binary GiB with one decimal, for human copy. */
        fun gib(bytes: Long): String {
            val gibibyte = 1024L * 1024L * 1024L
            val tenths = (bytes.coerceAtLeast(0L) * 10L + gibibyte / 2L) / gibibyte
            return "${tenths / 10L}.${tenths % 10L}"
        }
    }
}

// [VXG RealForever]
