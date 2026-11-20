package vexlife.web

import vexlife.capability.VexLifeCapabilityAtlas
import vexlife.conversation.VexLifeConversationTurnFamily
import vexlife.continuity.VexLifePrivateModeFamily
import vexlife.context.VexLifeContextProfileFamily
import vexlife.diagnostics.VexLifeDiagnosticsFamily
import vexlife.localasr.VexLifeLocalAsrModelsFamily
import vexlife.localasr.VexLifeLocalAsrProvider
import vexlife.memory.VexLifeMemoryFamily
import vexlife.model.VexLifeModelsFamily
import vexlife.model.heretic.HereticModelCatalog
import vexlife.person.VexLifeLanguageFamily
import vexlife.release.VexLifeReleaseInboxFamily
import vexlife.sources.VexLifeLocalSourcesFamily
import vexlife.voiceinput.VexLifeVoiceInputFamily
import vexlife.vision.VexLifeVisionFamily

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
        VexLifeMemoryFamily.StateRefs.forEach { add("memory:state:$it") }
        VexLifeLocalSourcesFamily.ControlRefs.forEach { add("local-sources:control:$it") }
        VexLifePrivateModeFamily.StateRefs.forEach { add("private-mode:state:$it") }
        VexLifePrivateModeFamily.ControlRefs.forEach { add("private-mode:control:$it") }
        VexLifeContextProfileFamily.StateRefs.forEach { add("context-profile:state:$it") }
        VexLifeContextProfileFamily.ControlRefs.forEach { add("context-profile:control:$it") }
        VexLifeVisionFamily.StateRefs.forEach { add("vision:state:$it") }
        VexLifeVisionFamily.ControlRefs.forEach { add("vision:control:$it") }
        VexLifeVisionFamily.HoldReasonRefs.forEach { add("vision:reason:$it") }
        LocalAsrStates.forEach { add("local-asr:state:$it") }
        VexLifeLocalAsrModelsFamily.ControlRefs.forEach { add("local-asr:control:$it") }
        add(LocalAsrSelectionSaved)
        DiagnosticsStates.forEach { add("diagnostics:state:$it") }
        DiagnosticsControls.forEach { add("diagnostics:control:$it") }
        add(DiagnosticsExport)
        VexLifeLanguageFamily.ControlRefs.forEach { add("language:control:$it") }
        VexLifeConversationTurnFamily.StateRefs.forEach { add("conversation-turn:state:$it") }
        VexLifeConversationTurnFamily.ControlRefs.forEach { add("conversation-turn:control:$it") }
        VexLifeReleaseInboxFamily.StatusRefs.forEach { add("release-alerts:state:$it") }
        listOf(VexLifeReleaseInboxFamily.ControlView, VexLifeReleaseInboxFamily.ControlDismiss).forEach {
            add("release-alerts:control:$it")
        }
    }
}

// [VXG RealForever]
