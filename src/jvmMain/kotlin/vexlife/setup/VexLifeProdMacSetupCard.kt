package vexlife.setup

import java.nio.file.Path
import kotlinx.serialization.Serializable
import vexlife.entry.VexLifeProdEntryFamily
import vexlife.entry.VexLifeProdEntryFamily.Companion.gib
import vexlife.home.VexLifeProdHome
import vexlife.home.VexLifeProdHomeFiles
import vexlife.model.heretic.runtime.HereticSetupStorage
import vexlife.service.VexLifeProdServiceContract

/**
 * Concrete native presentation meaning for one Setup Journey card.
 *
 * Family State -> DisplayState -> **this card** -> AppKit manifestation.
 *
 * The card composes the Family's DisplayState with the observed facts the person needs to decide
 * (where the Home is, whether this Mac and drive meet the requirements, what setup will download).
 * Every button carries either a Family-authored Control ref or one of three renderer-local
 * navigation refs. The native renderer only shows these strings and returns the chosen ref; it does
 * not author copy, decide which buttons exist, or map anything to an Effect.
 */
object VexLifeProdMacSetupCard {
    const val ActionControlPrefix =
        "CONTROL:"
    const val ActionChooseHome =
        "CHOOSE_HOME"
    const val ActionRecheck =
        "RECHECK"
    const val ActionClose =
        "CLOSE"

    @Serializable
    data class Button(
        val label: String,
        val actionRef: String,
    ) {
        init {
            require(
                label.isNotBlank() &&
                    (
                        actionRef.startsWith(
                            ActionControlPrefix,
                        ) ||
                            actionRef in
                            setOf(
                                ActionChooseHome,
                                ActionRecheck,
                                ActionClose,
                            )
                    ),
            ) {
                "Setup Journey button is invalid"
            }
        }
    }

    @Serializable
    data class Card(
        val title: String,
        val rail: String,
        val body: String,
        val buttons: List<Button>,
    ) {
        init {
            require(
                title.isNotBlank() &&
                    rail.isNotBlank() &&
                    body.isNotBlank() &&
                    buttons.size in
                    1..5 &&
                    buttons.last().actionRef ==
                    ActionClose &&
                    buttons.count {
                        it.actionRef ==
                            ActionClose
                    } == 1 &&
                    buttons.map {
                        it.label
                    }.toSet().size ==
                    buttons.size,
            ) {
                "Setup Journey card is invalid"
            }
        }
    }

    data class Facts(
        val displayState: VexLifeProdEntryFamily.EntryDisplayState,
        val semanticStateRef: String,
        val currentState: VexLifeProdServiceContract.CurrentStateProjection,
        val location: VexLifeProdHomeFiles.Location,
        val setupPlan: VexLifeProdMacFrontDoor.SetupPlan?,
        val formationPreview: HereticSetupStorage.Requirement?,
        val currentServiceSourceSha: String?,
        val requiredChipModel: String,
        val requiredMemoryBytes: Long,
        val protectedLocation: String?,
    )

    fun form(
        facts: Facts,
    ): Card {
        val display =
            facts.displayState
        val buttons =
            buildList {
                display.primaryControlRef?.let {
                    add(Button(requireNotNull(display.primaryLabel), ActionControlPrefix + it))
                }
                display.secondaryControlRef?.let {
                    add(Button(requireNotNull(display.secondaryLabel), ActionControlPrefix + it))
                }
                display.homeChoiceLabel?.let {
                    add(Button(it, ActionChooseHome))
                }
                display.recheckLabel?.let {
                    add(Button(it, ActionRecheck))
                }
                add(Button(display.cancelLabel, ActionClose))
            }

        return Card(
            title = display.title,
            rail = rail(display.journey),
            body =
                listOfNotNull(
                    display.detail,
                    foundSection(facts),
                    facts.setupPlan?.let(::planSection),
                    facts.formationPreview?.let(::upNextSection),
                    noticeSection(facts),
                ).joinToString(
                    "\n\n",
                ),
            buttons = buttons,
        )
    }

    fun rail(
        journey: List<VexLifeProdEntryFamily.JourneyStep>,
    ): String =
        journey.joinToString(
            "   ",
        ) {
            step ->
            val mark =
                when (
                    step.statusRef
                ) {
                    VexLifeProdEntryFamily.Complete -> "✓"
                    VexLifeProdEntryFamily.JourneyCurrent -> "●"
                    VexLifeProdEntryFamily.JourneyHeld -> "⊘"
                    else -> "○"
                }
            "$mark ${step.label}"
        }

    private fun foundSection(
        facts: Facts,
    ): String {
        val current =
            facts.currentState
        val lines =
            mutableListOf<String>()

        lines +=
            "Vex Home: " +
            current.homePath +
            " — " +
            homeStatus(facts.location)

        val memory =
            current.totalMemoryBytes?.let { gib(it) + " GiB memory" } ?: "memory unknown"
        val meets =
            current.hostEligibilityState ==
                "ELIGIBLE"
        lines +=
            "This Mac: " +
            (current.chipModel ?: "unknown chip") +
            ", " +
            memory +
            " — " +
            if (meets) {
                "meets the requirements"
            } else {
                "needs ${facts.requiredChipModel} and ${gib(facts.requiredMemoryBytes)} GiB memory"
            }

        lines +=
            "Free space on that drive: " +
            (current.freeStorageBytes?.let { gib(it) + " GiB" } ?: "unknown")

        if (
            facts.location is VexLifeProdHomeFiles.Location.Existing
        ) {
            lines +=
                "Local model: " +
                modelStatus(current)
            serviceStatus(facts)?.let {
                lines += "VexLife service: $it"
            }
        }

        return "What VexLife found\n" +
            lines.joinToString("\n") { "  $it" }
    }

    private fun homeStatus(
        location: VexLifeProdHomeFiles.Location,
    ): String =
        when (
            location
        ) {
            is VexLifeProdHomeFiles.Location.Existing ->
                "your existing Vex Home"

            is VexLifeProdHomeFiles.Location.Held ->
                "preserved (" +
                    location.detail +
                    ")"

            is VexLifeProdHomeFiles.Location.Formable ->
                when {
                    location.disposition ==
                        VexLifeProdHome.LocationDisposition.EmptyDirectory ->
                        "an empty folder VexLife can set up as your Home"

                    location.missingAncestorCount >
                        0 ->
                        "new; VexLife will also create " +
                            location.missingAncestorCount +
                            (if (location.missingAncestorCount == 1) " folder" else " folders") +
                            " leading to it"

                    else ->
                        "new; nothing is there yet"
                }
        }

    private fun modelStatus(
        current: VexLifeProdServiceContract.CurrentStateProjection,
    ): String =
        when (
            current.modelArtifactState
        ) {
            "NOT_SELECTED" ->
                "not set up yet"

            "ABSENT" ->
                "not downloaded yet"

            "PARTIAL_BY_SIZE" ->
                gib(current.modelArtifactPresentBySizeBytes ?: 0L) +
                    " of " +
                    gib(current.modelArtifactTotalBytes ?: 0L) +
                    " GiB downloaded"

            "COMPLETE_BY_SIZE_NOT_HASHED" ->
                "on this Mac (" +
                    gib(current.modelArtifactTotalBytes ?: 0L) +
                    " GiB)"

            "INVALID_SIZE" ->
                gib(current.modelArtifactInvalidSizeBytes ?: 0L) +
                    " GiB of files do not match their expected size"

            else ->
                "unknown"
        }

    private fun serviceStatus(
        facts: Facts,
    ): String? =
        when (
            facts.semanticStateRef
        ) {
            VexLifeProdEntryFamily.StateCurrent ->
                "running this version"

            VexLifeProdEntryFamily.StateUpdateAvailable ->
                "running an earlier version (" +
                    facts.currentServiceSourceSha?.take(7) +
                    ")"

            VexLifeProdEntryFamily.StateRepairRequired,
            VexLifeProdEntryFamily.StateUpdateRepairRequired,
            ->
                "registered but not responding"

            VexLifeProdEntryFamily.StateStartRequired ->
                "not running"

            VexLifeProdEntryFamily.StateHeldUnknown ->
                "in use by something VexLife cannot verify"

            else ->
                null
        }

    private fun planSection(
        plan: VexLifeProdMacFrontDoor.SetupPlan,
    ): String {
        val lines =
            mutableListOf<String>()
        lines +=
            if (
                plan.modelArtifactMissingBytes >
                0
            ) {
                "• Download about ${gib(plan.modelArtifactMissingBytes)} GiB of verified model files " +
                    "(${plan.modelLabel})"
            } else {
                "• Re-use the model files already in your Home (${plan.modelLabel})"
            }
        lines += "• Prepare a private Python + MLX runtime inside your Vex Home"
        lines += "• Start the local model and VexLife on this Mac only (127.0.0.1)"
        lines +=
            "Needs about ${gib(plan.requiredFreeStorageBytes)} GiB free" +
            (plan.currentFreeStorageBytes?.let { " · ${gib(it)} GiB available" } ?: " · free space unknown") +
            ". Needs an internet connection; downloads resume if interrupted."
        return "What setup will do\n" +
            lines.joinToString("\n") { "  $it" }
    }

    /**
     * Walk-through honesty: before the Home-only step, say what the *next* step will need, so a
     * person is not surprised by a 14 GiB download after committing a folder.
     */
    private fun upNextSection(
        preview: HereticSetupStorage.Requirement,
    ): String =
        "Up next (you'll confirm it separately)\n" +
            "  Local model: about ${gib(preview.missingModelBytes)} GiB download, about " +
            "${gib(preview.requiredFreeBytes)} GiB free space on this drive, and an internet connection."

    private fun noticeSection(
        facts: Facts,
    ): String? {
        val notices =
            mutableListOf<String>()

        if (
            facts.protectedLocation !=
                null &&
            facts.semanticStateRef in
            setOf(
                VexLifeProdEntryFamily.StateHomeFormationRequired,
                VexLifeProdEntryFamily.StateHomeHeld,
            )
        ) {
            notices +=
                "Heads-up: this location is inside ${facts.protectedLocation}, which macOS protects. " +
                "macOS may ask you to allow access, and VexLife's background service can be blocked " +
                "from reading it later. A location such as ~/.vexlife avoids this."
        }

        facts.formationPreview
            ?.takeIf { it.shortfallObserved }
            ?.let {
                notices +=
                    "Heads-up: the local model step will need about ${gib(it.requiredFreeBytes)} GiB free on " +
                    "this drive, and it has ${gib(it.observedFreeBytes ?: 0L)} GiB. You can still create the " +
                    "Home now and free up space before the next step."
            }

        return notices
            .takeIf {
                it.isNotEmpty()
            }
            ?.joinToString(
                "\n\n",
            )
    }

    /**
     * Names the macOS privacy-protected (TCC) area that contains [selectedHome], if any.
     *
     * Lived evidence: proof artifacts living in ~/Downloads triggered a Files & Folders prompt. A Vex
     * Home is read later by a launchd background service, so a protected coordinate is a real
     * capability risk that must be explained before Home formation, not discovered afterwards.
     */
    fun protectedLocationName(
        userHome: Path,
        selectedHome: Path,
    ): String? {
        val home =
            userHome.normalize()
        val selected =
            selectedHome.normalize()
        return when {
            selected.startsWith(home.resolve("Desktop")) -> "your Desktop folder"
            selected.startsWith(home.resolve("Documents")) -> "your Documents folder"
            selected.startsWith(home.resolve("Downloads")) -> "your Downloads folder"
            selected.startsWith(home.resolve("Library/Mobile Documents")) -> "iCloud Drive"
            selected.startsWith(home.resolve("Library/CloudStorage")) -> "a cloud-storage folder"
            selected.startsWith(Path.of("/Volumes")) -> "an external or network drive"
            else -> null
        }
    }
}

// [VXG RealForever]
