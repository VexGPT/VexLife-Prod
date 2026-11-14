package vexlife.setup

import vexlife.entry.VexLifeProdEntryFamily
import vexlife.home.VexLifeProdHome
import vexlife.local.VexLifeProdSetupProgress

/**
 * The semantic refs a `setup` string key may name as its `dataKey`, taken from their owners.
 *
 * A string key is the element's identity; its `dataKey` says which semantic ref the element
 * presents (`entry:control:CONVERGE_DESIRED`). This registry is not a second owner of those refs:
 * it only reads each owner's own vocabulary, so a `dataKey` that names a ref no owner has is caught
 * by a test instead of silently pointing nowhere.
 */
object VexLifeProdSetupElements {
    private val EntryControls =
        listOf(
                VexLifeProdEntryFamily.OpenCurrent,
                VexLifeProdEntryFamily.ConvergeDesired,
                VexLifeProdEntryFamily.Start,
                VexLifeProdEntryFamily.FormHome,
                VexLifeProdEntryFamily.SetUp,
                VexLifeProdEntryFamily.Cancel,
            )
            .map { it.controlRef }

    private val CardActions =
        listOf(
            VexLifeProdMacSetupCard.ActionChooseHome,
            VexLifeProdMacSetupCard.ActionChooseLanguage,
            VexLifeProdMacSetupCard.ActionRecheck,
            VexLifeProdMacSetupCard.ActionClose,
        )

    private val FailureRefs =
        listOf(
            VexLifeProdSetupFailure.StateChanged,
            VexLifeProdSetupFailure.SetupAlreadyRunning,
            VexLifeProdSetupFailure.StorageExhausted,
            VexLifeProdSetupFailure.NetworkUnavailable,
            VexLifeProdSetupFailure.PrerequisiteMissing,
            VexLifeProdSetupFailure.HomePreserved,
            VexLifeProdSetupFailure.StoppedSafely,
        )

    private val HostReasons =
        listOf(
            "PLATFORM_UNSUPPORTED",
            "ARCHITECTURE_UNSUPPORTED",
            "CHIP_MODEL_UNSUPPORTED",
            "SYSTEM_MEMORY_UNKNOWN",
            "INSUFFICIENT_SYSTEM_MEMORY",
        )

    private val ModelArtifactStates =
        listOf(
            "NOT_SELECTED",
            "ABSENT",
            "PARTIAL_BY_SIZE",
            "COMPLETE_BY_SIZE_NOT_HASHED",
            "INVALID_SIZE",
        )

    const val ArtifactBytesProgress = "setup:progress:ARTIFACT_BYTES"

    val DataKeys: Set<String> = buildSet {
        VexLifeProdEntryFamily.StateRefs.forEach { add("entry:state:$it") }
        EntryControls.forEach { add("entry:control:$it") }
        VexLifeProdEntryFamily.JourneyStepRefs.forEach { add("entry:step:$it") }
        CardActions.forEach { add("setup:action:$it") }
        FailureRefs.forEach { add("setup:failure:$it") }
        VexLifeProdSetupProgress.Phase.entries.forEach { add("setup:phase:${it.name}") }
        add(ArtifactBytesProgress)
        VexLifeProdHome.LocationDisposition.entries.forEach {
            add("home:disposition:${it.wireValue}")
        }
        HostReasons.forEach { add("host:reason:$it") }
        ModelArtifactStates.forEach { add("model:artifact-state:$it") }
    }

    /** The `dataKey` a card button's element key must carry for the action it returns. */
    fun dataKeyForAction(actionRef: String): String =
        if (actionRef.startsWith(VexLifeProdMacSetupCard.ActionControlPrefix)) {
            "entry:control:" + actionRef.removePrefix(VexLifeProdMacSetupCard.ActionControlPrefix)
        } else {
            "setup:action:$actionRef"
        }
}

// [VXG RealForever]
