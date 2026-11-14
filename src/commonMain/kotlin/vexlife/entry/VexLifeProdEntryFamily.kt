package vexlife.entry

import kotlinx.serialization.Serializable
import vexlife.home.VexLifeProdHome
import vexlife.semantic.VexFamilyContract
import vexlife.text.VexLifeText

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
 * [EntryDisplayState] names text by key ([VexLifeText]); the presentation resolves it in the
 * person's selected language.
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
    data class JourneyStep(val stepRef: String, val label: VexLifeText, val statusRef: String) {
        init {
            require(stepRef in JourneyStepRefs && statusRef in JourneyStatusRefs) {
                "Setup Journey step is invalid"
            }
        }
    }

    /**
     * Every human-readable field is a [VexLifeText] key, resolved by the concrete presentation in
     * the person's selected language (VexArchitecture localization law); this Family names meaning,
     * never words.
     *
     * [homeChoiceLabel] offers renderer-local navigation back to the Home chooser. Choosing a
     * different coordinate is not a semantic Effect; it changes what will be observed next.
     * [recheckLabel] offers renderer-local re-observation for held States whose cause the person
     * can change outside VexLife (free space, folder contents, another running copy).
     */
    @Serializable
    data class EntryDisplayState(
        val title: VexLifeText,
        val detail: VexLifeText,
        val primaryControlRef: String?,
        val primaryLabel: VexLifeText?,
        val secondaryControlRef: String?,
        val secondaryLabel: VexLifeText?,
        val cancelLabel: VexLifeText,
        val journey: List<JourneyStep>,
        val homeChoiceLabel: VexLifeText?,
        val recheckLabel: VexLifeText?,
    ) :
        VexFamilyContract.DisplayState<
            VexLifeProdEntryFamily,
            EntryState,
        > {
        init {
            require(
                ((primaryControlRef == null) == (primaryLabel == null)) &&
                    ((secondaryControlRef == null) == (secondaryLabel == null)) &&
                    journey.map {
                        it.stepRef
                    } == JourneyStepRefs &&
                    journey.count {
                        it.statusRef == JourneyCurrent || it.statusRef == JourneyHeld
                    } <= 1
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
                    title = text("setup.heading.current"),
                    detail = text("setup.description.current"),
                    primary = ControlOpenCurrent to text("setup.button.open-vexlife"),
                    journey = journey(Complete, Complete, Complete, Complete, JourneyCurrent),
                )

            is UpdateAvailable ->
                display(
                    title = text("setup.heading.update-available"),
                    detail = text("setup.description.update-available"),
                    primary = ControlConvergeDesired to text("setup.button.update-restart"),
                    secondary = ControlOpenCurrent to text("setup.button.open-current"),
                    journey = journey(Complete, Complete, Complete, JourneyCurrent, Upcoming),
                )

            is RepairRequired ->
                display(
                    title = text("setup.heading.repair-required"),
                    detail = text("setup.description.repair-required"),
                    primary = ControlConvergeDesired to text("setup.button.repair-restart"),
                    journey = journey(Complete, Complete, Complete, JourneyCurrent, Upcoming),
                )

            is UpdateRepairRequired ->
                display(
                    title = text("setup.heading.update-repair-required"),
                    detail = text("setup.description.update-repair-required"),
                    primary = ControlConvergeDesired to text("setup.button.update-restart"),
                    journey = journey(Complete, Complete, Complete, JourneyCurrent, Upcoming),
                )

            is StartRequired ->
                display(
                    title = text("setup.heading.start-required"),
                    detail =
                        if (
                            state.priorServiceSourceSha != null &&
                                state.priorServiceSourceSha != state.desiredSourceSha
                        ) {
                            text("setup.description.start-required-newer-version")
                        } else {
                            text("setup.description.start-required")
                        },
                    primary = ControlStart to text("setup.button.start-vexlife"),
                    journey = journey(Complete, Complete, Complete, JourneyCurrent, Upcoming),
                )

            is HomeFormationRequired ->
                display(
                    title = text("setup.heading.home-formation-required"),
                    detail = text("setup.description.home-formation-required"),
                    primary = ControlFormHome to text("setup.button.create-home"),
                    journey = journey(Complete, JourneyCurrent, Upcoming, Upcoming, Upcoming),
                    homeChoice = true,
                )

            is HomeHeld ->
                display(
                    title = text("setup.heading.home-held"),
                    detail =
                        VexLifeText(
                            key = "setup.description.home-held",
                            parts = mapOf("reason" to homeHeldExplanation(state.disposition)),
                        ),
                    journey = journey(Complete, JourneyHeld, Upcoming, Upcoming, Upcoming),
                    homeChoice = true,
                    recheck = true,
                    cancelLabel = text("setup.button.close"),
                )

            is HostHeld ->
                display(
                    title = text("setup.heading.host-held"),
                    detail = hostHeldExplanation(state),
                    journey =
                        journey(
                            JourneyHeld,
                            if (state.homePresent) Complete else Upcoming,
                            Upcoming,
                            Upcoming,
                            Upcoming,
                        ),
                    cancelLabel = text("setup.button.close"),
                )

            is SetupRequired ->
                display(
                    title = text("setup.heading.setup-required"),
                    detail = text("setup.description.setup-required"),
                    primary = ControlSetUp to text("setup.button.set-up"),
                    journey = journey(Complete, Complete, JourneyCurrent, Upcoming, Upcoming),
                )

            is StorageHeld ->
                display(
                    title = text("setup.heading.storage-held"),
                    detail =
                        VexLifeText(
                            key = "setup.description.storage-held",
                            gib =
                                mapOf(
                                    "required" to state.requiredFreeBytes,
                                    "free" to state.freeBytes,
                                ),
                        ),
                    journey = journey(Complete, Complete, JourneyHeld, Upcoming, Upcoming),
                    homeChoice = true,
                    recheck = true,
                    cancelLabel = text("setup.button.close"),
                )

            is ModelFilesHeld ->
                display(
                    title = text("setup.heading.model-files-held"),
                    detail =
                        VexLifeText(
                            key = "setup.description.model-files-held",
                            gib = mapOf("affected" to state.invalidBytes),
                        ),
                    journey = journey(Complete, Complete, JourneyHeld, Upcoming, Upcoming),
                    recheck = true,
                    cancelLabel = text("setup.button.close"),
                )

            is HeldUnknown ->
                display(
                    title = text("setup.heading.held-unknown"),
                    detail = text("setup.description.held-unknown"),
                    journey = journey(Complete, Complete, Upcoming, JourneyHeld, Upcoming),
                    homeChoice = true,
                    recheck = true,
                    cancelLabel = text("setup.button.close"),
                )
        }

    private fun text(key: String): VexLifeText = VexLifeText(key)

    private fun display(
        title: VexLifeText,
        detail: VexLifeText,
        journey: List<JourneyStep>,
        primary: Pair<String, VexLifeText>? = null,
        secondary: Pair<String, VexLifeText>? = null,
        homeChoice: Boolean = false,
        recheck: Boolean = false,
        cancelLabel: VexLifeText = text("setup.button.cancel"),
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
            homeChoiceLabel =
                if (homeChoice) text("setup.button.choose-another-location") else null,
            recheckLabel = if (recheck) text("setup.button.check-again") else null,
        )

    private fun journey(
        mac: String,
        home: String,
        model: String,
        start: String,
        ready: String,
    ): List<JourneyStep> =
        listOf(
            JourneyStep(JourneyStepMac, text("setup.label.step-mac"), mac),
            JourneyStep(JourneyStepHome, text("setup.label.step-home"), home),
            JourneyStep(JourneyStepModel, text("setup.label.step-model"), model),
            JourneyStep(JourneyStepStart, text("setup.label.step-start"), start),
            JourneyStep(JourneyStepReady, text("setup.label.step-ready"), ready),
        )

    private fun homeHeldExplanation(disposition: VexLifeProdHome.LocationDisposition): VexLifeText =
        when (disposition) {
            VexLifeProdHome.LocationDisposition.HeldNoncanonicalContent ->
                text("setup.description.home-held-other-files")

            VexLifeProdHome.LocationDisposition.HeldInvalidCanonicalRecords ->
                text("setup.description.home-held-invalid-records")

            VexLifeProdHome.LocationDisposition.HeldNotDirectory ->
                text("setup.description.home-held-not-folder")

            VexLifeProdHome.LocationDisposition.HeldSymbolicLink ->
                text("setup.description.home-held-link")

            VexLifeProdHome.LocationDisposition.HeldLocationNotWritable ->
                text("setup.description.home-held-not-writable")

            VexLifeProdHome.LocationDisposition.HeldLocationUnreadable ->
                text("setup.description.home-held-unreadable")

            VexLifeProdHome.LocationDisposition.Absent,
            VexLifeProdHome.LocationDisposition.EmptyDirectory,
            VexLifeProdHome.LocationDisposition.Canonical ->
                error("HomeHeld admits only held dispositions")
        }

    private fun hostHeldExplanation(state: HostHeld): VexLifeText =
        VexLifeText(
            key = "setup.description.host-held",
            parts =
                mapOf(
                    "reason" to
                        text(
                            when (state.reasonRef) {
                                "PLATFORM_UNSUPPORTED" -> "setup.description.host-reason-platform"
                                "ARCHITECTURE_UNSUPPORTED" ->
                                    "setup.description.host-reason-architecture"
                                "SYSTEM_MEMORY_UNKNOWN" ->
                                    "setup.description.host-reason-memory-unknown"
                                "INSUFFICIENT_SYSTEM_MEMORY" ->
                                    "setup.description.host-reason-memory-low"
                                else -> "setup.description.host-reason-chip"
                            }
                        ),
                    "requirement" to
                        VexLifeText(
                            key = "setup.description.host-requirement",
                            args = mapOf("chip" to state.requiredChipModel),
                            gib = mapOf("memory" to state.requiredMemoryBytes),
                        ),
                    "observed" to
                        VexLifeText(
                            key = "setup.description.host-observed",
                            args = state.chipModel?.let { mapOf("chip" to it) } ?: emptyMap(),
                            parts =
                                buildMap {
                                    if (state.chipModel == null)
                                        put("chip", text("setup.label.unknown-chip"))
                                    put("memory", memoryText(state.totalMemoryBytes))
                                },
                        ),
                    "outcome" to
                        text(
                            if (state.homePresent) {
                                "setup.description.host-home-untouched"
                            } else {
                                "setup.description.host-nothing-created"
                            }
                        ),
                ),
        )

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

        /** "{memory} GiB memory", or "unknown memory" when it could not be read. */
        fun memoryText(totalMemoryBytes: Long?): VexLifeText =
            totalMemoryBytes?.let {
                VexLifeText(key = "setup.label.memory-amount", gib = mapOf("memory" to it))
            } ?: VexLifeText("setup.label.unknown-memory")
    }
}

// [VXG RealForever]
