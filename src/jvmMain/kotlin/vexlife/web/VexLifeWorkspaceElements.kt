package vexlife.web

import vexlife.capability.VexLifeCapabilityAtlas
import vexlife.diagnostics.VexLifeDiagnosticsFamily
import vexlife.localasr.VexLifeLocalAsrProvider
import vexlife.model.VexLifeModelsFamily
import vexlife.model.heretic.HereticModelCatalog
import vexlife.person.VexLifeLanguageFamily
import vexlife.release.VexLifeReleaseInboxFamily
import vexlife.voiceinput.VexLifeVoiceInputFamily

/**
 * Every semantic ref a `workspace` or `release-alerts` key may present through its `dataKey`,
 * named by the owner of that ref (docs/foundation/LOCALIZATION.md). This is the workspace's
 * counterpart of [vexlife.setup.VexLifeProdSetupElements.DataKeys]: the catalog's dataKeys are
 * checked against main source, so renaming an owner's ref fails here instead of drifting.
 */
object VexLifeWorkspaceElements {
    private val ModelControls =
        listOf(
            VexLifeModelsFamily.ControlSelect,
            VexLifeModelsFamily.ControlUse,
            VexLifeModelsFamily.ControlUseRecommendedRuntimeConfiguration,
            VexLifeModelsFamily.ControlSetPromptCacheEntries,
            VexLifeModelsFamily.ControlDownload,
            VexLifeModelsFamily.ControlRedownload,
            VexLifeModelsFamily.ControlRemove,
            VexLifeModelsFamily.ControlForgetImportedModel,
        )

    private val ModelFiles =
        listOf(
            VexLifeModelsFamily.ArtifactPresence.Absent,
            VexLifeModelsFamily.ArtifactPresence.Partial,
            VexLifeModelsFamily.ArtifactPresence.Present,
            VexLifeModelsFamily.ArtifactPresence.Damaged,
        )

    private val LocalAsrStates =
        listOf(
            VexLifeLocalAsrProvider.StateReady,
            VexLifeLocalAsrProvider.DetailProviderSourceNotCurrent,
            VexLifeLocalAsrProvider.DetailProviderExecutableUnavailable,
            VexLifeLocalAsrProvider.DetailModelUnavailable,
            VexLifeLocalAsrProvider.DetailModelDigestNotCurrent,
        )

    private val DiagnosticsStates =
        listOf(VexLifeDiagnosticsFamily.StateRecording, VexLifeDiagnosticsFamily.StateRecordingEnhanced)

    private val DiagnosticsControls =
        listOf(
            VexLifeDiagnosticsFamily.ControlStartEnhanced,
            VexLifeDiagnosticsFamily.ControlStopEnhanced,
            VexLifeDiagnosticsFamily.ControlClearHistory,
        )

    /** The debug export is a service route, not a Family Control; its dataKey names that route. */
    const val DiagnosticsExport = "diagnostics:export:DEBUG_ZIP"

    /** The person's saved local-dictation selection, shown as a confirmation. */
    const val LocalAsrSelectionSaved = "local-asr:selection:SAVED"

    val DataKeys: Set<String> = buildSet {
        VexLifeCapabilityAtlas.definitions().forEach { definition ->
            definition.actions.forEach { add("capability:${definition.handle}:${it.action}") }
        }
        add("capability:trace:USES")
        ModelControls.forEach { add("model:control:$it") }
        ModelFiles.forEach { add("model:files:${it.stateRef}") }
        HereticModelCatalog.Standing.entries.forEach { add("model:standing:${it.name}") }
        VexLifeVoiceInputFamily.StateRefs.forEach { add("voice-input:state:$it") }
        VexLifeVoiceInputFamily.ControlRefs.forEach { add("voice-input:control:$it") }
        VexLifeVoiceInputFamily.HoldReasonRefs.forEach { add("voice-input:reason:$it") }
        VexLifeVoiceInputFamily.FailureReasonRefs.forEach { add("voice-input:failure:$it") }
        LocalAsrStates.forEach { add("local-asr:state:$it") }
        add(LocalAsrSelectionSaved)
        DiagnosticsStates.forEach { add("diagnostics:state:$it") }
        DiagnosticsControls.forEach { add("diagnostics:control:$it") }
        add(DiagnosticsExport)
        VexLifeLanguageFamily.ControlRefs.forEach { add("language:control:$it") }
        VexLifeReleaseInboxFamily.StatusRefs.forEach { add("release-alerts:state:$it") }
        listOf(VexLifeReleaseInboxFamily.ControlView, VexLifeReleaseInboxFamily.ControlDismiss).forEach {
            add("release-alerts:control:$it")
        }
    }
}

// [VXG RealForever]
