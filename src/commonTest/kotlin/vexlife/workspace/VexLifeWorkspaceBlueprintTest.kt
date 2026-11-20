package vexlife.workspace

import kotlin.test.Test
import kotlin.test.assertEquals

class VexLifeWorkspaceBlueprintTest {
    @Test
    fun workspaceCompositionKeepsStableUniquePerceptibleAddresses() {
        assertEquals(
            "screen.vexlife-workspace",
            VexLifeWorkspaceBlueprint.screenRef,
        )

        val refs =
            VexLifeWorkspaceBlueprint
                .elements
                .map {
                    it.ref
                }

        assertEquals(
            refs.size,
            refs.toSet().size,
        )
        assertEquals(
            99,
            refs.size,
        )

        val expectedParents =
            mapOf(
                VexLifeWorkspaceBlueprint.QuickActions.ref to VexLifeWorkspaceBlueprint.Utilities.ref,
                VexLifeWorkspaceBlueprint.Attach.ref to VexLifeWorkspaceBlueprint.Conversation.ref,
                VexLifeWorkspaceBlueprint.ComposerAttachments.ref to VexLifeWorkspaceBlueprint.Conversation.ref,
                VexLifeWorkspaceBlueprint.SettingsLocalSources.ref to VexLifeWorkspaceBlueprint.Settings.ref,
                VexLifeWorkspaceBlueprint.LocalSourcesList.ref to VexLifeWorkspaceBlueprint.SettingsLocalSources.ref,
                VexLifeWorkspaceBlueprint.LocalSourcesAdd.ref to VexLifeWorkspaceBlueprint.SettingsLocalSources.ref,
                VexLifeWorkspaceBlueprint.VisionQuickAccess.ref to VexLifeWorkspaceBlueprint.QuickActions.ref,
                VexLifeWorkspaceBlueprint.Speech.ref to VexLifeWorkspaceBlueprint.QuickActions.ref,
                VexLifeWorkspaceBlueprint.ReleaseAlerts.ref to VexLifeWorkspaceBlueprint.QuickActions.ref,
                VexLifeWorkspaceBlueprint.ReleaseAlertsPanel.ref to VexLifeWorkspaceBlueprint.Utilities.ref,
                VexLifeWorkspaceBlueprint.ReleaseAlertsList.ref to VexLifeWorkspaceBlueprint.ReleaseAlertsPanel.ref,
                VexLifeWorkspaceBlueprint.LanguagePanel.ref to VexLifeWorkspaceBlueprint.Utilities.ref,
                VexLifeWorkspaceBlueprint.ReplyLanguageChoice.ref to VexLifeWorkspaceBlueprint.LanguagePanel.ref,
                VexLifeWorkspaceBlueprint.ScreenLanguageChoice.ref to VexLifeWorkspaceBlueprint.LanguagePanel.ref,
                VexLifeWorkspaceBlueprint.SourceTemplateFamily.ref to VexLifeWorkspaceBlueprint.Browser.ref,
                VexLifeWorkspaceBlueprint.ArchivesTimeline.ref to VexLifeWorkspaceBlueprint.Archives.ref,
                VexLifeWorkspaceBlueprint.VexContext.ref to VexLifeWorkspaceBlueprint.Browser.ref,
                VexLifeWorkspaceBlueprint.VexContextProfile.ref to VexLifeWorkspaceBlueprint.VexContext.ref,
                VexLifeWorkspaceBlueprint.VexContextProfileUse.ref to VexLifeWorkspaceBlueprint.VexContextProfile.ref,
                VexLifeWorkspaceBlueprint.VexContextProfileSave.ref to VexLifeWorkspaceBlueprint.VexContextProfile.ref,
                VexLifeWorkspaceBlueprint.VexContextProfileSaveAs.ref to VexLifeWorkspaceBlueprint.VexContextProfile.ref,
                VexLifeWorkspaceBlueprint.VexContextProfileRestore.ref to VexLifeWorkspaceBlueprint.VexContextProfile.ref,
                VexLifeWorkspaceBlueprint.VexContextProfilePortability.ref to VexLifeWorkspaceBlueprint.VexContextProfile.ref,
                VexLifeWorkspaceBlueprint.VexContextProfileImport.ref to VexLifeWorkspaceBlueprint.VexContextProfile.ref,
                VexLifeWorkspaceBlueprint.VexContextProfileHome.ref to VexLifeWorkspaceBlueprint.VexContextProfile.ref,
                VexLifeWorkspaceBlueprint.VexContextChanges.ref to VexLifeWorkspaceBlueprint.VexContext.ref,
                VexLifeWorkspaceBlueprint.VexContextInput.ref to VexLifeWorkspaceBlueprint.VexContext.ref,
                VexLifeWorkspaceBlueprint.VexContextInputFoundation.ref to VexLifeWorkspaceBlueprint.VexContextInput.ref,
                VexLifeWorkspaceBlueprint.VexContextInputMemory.ref to VexLifeWorkspaceBlueprint.VexContextInput.ref,
                VexLifeWorkspaceBlueprint.VexContextInputContinuity.ref to VexLifeWorkspaceBlueprint.VexContextInput.ref,
                VexLifeWorkspaceBlueprint.VexContextInputConversation.ref to VexLifeWorkspaceBlueprint.VexContextInput.ref,
                VexLifeWorkspaceBlueprint.VexContextInputBrowser.ref to VexLifeWorkspaceBlueprint.VexContextInput.ref,
                VexLifeWorkspaceBlueprint.VexContextInputRequest.ref to VexLifeWorkspaceBlueprint.VexContextInput.ref,
                VexLifeWorkspaceBlueprint.VexContextInputComponents.ref to VexLifeWorkspaceBlueprint.VexContextInput.ref,
                VexLifeWorkspaceBlueprint.VexContextProcess.ref to VexLifeWorkspaceBlueprint.VexContext.ref,
                VexLifeWorkspaceBlueprint.VexContextOutput.ref to VexLifeWorkspaceBlueprint.VexContext.ref,
                VexLifeWorkspaceBlueprint.VexContextAfter.ref to VexLifeWorkspaceBlueprint.VexContext.ref,
                VexLifeWorkspaceBlueprint.BrowserFrameStatus.ref to VexLifeWorkspaceBlueprint.Browser.ref,
                VexLifeWorkspaceBlueprint.Vision.ref to VexLifeWorkspaceBlueprint.Companion.ref,
                VexLifeWorkspaceBlueprint.CompanionViewSwitch.ref to VexLifeWorkspaceBlueprint.Companion.ref,
                VexLifeWorkspaceBlueprint.ContextShare.ref to VexLifeWorkspaceBlueprint.Conversation.ref,
                VexLifeWorkspaceBlueprint.ContextReceipt.ref to VexLifeWorkspaceBlueprint.Conversation.ref,
                VexLifeWorkspaceBlueprint.VoiceInput.ref to VexLifeWorkspaceBlueprint.Conversation.ref,
                VexLifeWorkspaceBlueprint.ContextWindow.ref to VexLifeWorkspaceBlueprint.SettingsContext.ref,
                VexLifeWorkspaceBlueprint.MemoryResource.ref to VexLifeWorkspaceBlueprint.SettingsResources.ref,
                VexLifeWorkspaceBlueprint.PrivateMode.ref to VexLifeWorkspaceBlueprint.Companion.ref,
                VexLifeWorkspaceBlueprint.PrivateBanner.ref to VexLifeWorkspaceBlueprint.Conversation.ref,
                VexLifeWorkspaceBlueprint.ContinuityMemory.ref to VexLifeWorkspaceBlueprint.SettingsContinuity.ref,
                VexLifeWorkspaceBlueprint.SettingsModels.ref to VexLifeWorkspaceBlueprint.Settings.ref,
                VexLifeWorkspaceBlueprint.ModelsState.ref to VexLifeWorkspaceBlueprint.SettingsModels.ref,
                VexLifeWorkspaceBlueprint.ModelChoice.ref to VexLifeWorkspaceBlueprint.SettingsModels.ref,
                VexLifeWorkspaceBlueprint.ModelsList.ref to VexLifeWorkspaceBlueprint.SettingsModels.ref,
                VexLifeWorkspaceBlueprint.ModelConfiguration.ref to VexLifeWorkspaceBlueprint.SettingsModels.ref,
                VexLifeWorkspaceBlueprint.ModelPromptCacheEntries.ref to VexLifeWorkspaceBlueprint.ModelConfiguration.ref,
                VexLifeWorkspaceBlueprint.ModelUseRecommended.ref to VexLifeWorkspaceBlueprint.ModelConfiguration.ref,
                VexLifeWorkspaceBlueprint.ModelStart.ref to VexLifeWorkspaceBlueprint.ModelConfiguration.ref,
                VexLifeWorkspaceBlueprint.SettingsLocalAsr.ref to VexLifeWorkspaceBlueprint.Settings.ref,
                VexLifeWorkspaceBlueprint.LocalAsrSelection.ref to VexLifeWorkspaceBlueprint.SettingsLocalAsr.ref,
                VexLifeWorkspaceBlueprint.LocalAsrModelsList.ref to VexLifeWorkspaceBlueprint.SettingsLocalAsr.ref,
                VexLifeWorkspaceBlueprint.LocalAsrSpokenLanguage.ref to VexLifeWorkspaceBlueprint.SettingsLocalAsr.ref,
                VexLifeWorkspaceBlueprint.LocalAsrComposerOutput.ref to VexLifeWorkspaceBlueprint.SettingsLocalAsr.ref,
                VexLifeWorkspaceBlueprint.LocalAsrStatus.ref to VexLifeWorkspaceBlueprint.SettingsLocalAsr.ref,
                VexLifeWorkspaceBlueprint.LocalAsrStorageNote.ref to VexLifeWorkspaceBlueprint.SettingsLocalAsr.ref,
                VexLifeWorkspaceBlueprint.SettingsDiagnostics.ref to VexLifeWorkspaceBlueprint.Settings.ref,
                VexLifeWorkspaceBlueprint.DiagnosticsTrace.ref to VexLifeWorkspaceBlueprint.SettingsDiagnostics.ref,
                VexLifeWorkspaceBlueprint.DiagnosticsExport.ref to VexLifeWorkspaceBlueprint.SettingsDiagnostics.ref,
            )

        expectedParents.forEach { (ref, parent) ->
            assertEquals(
                parent,
                VexLifeWorkspaceBlueprint
                    .elements
                    .single {
                        it.ref ==
                            ref
                    }
                    .parentRef,
            )
        }
    }
}

// [VXG RealForever]
