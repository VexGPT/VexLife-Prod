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
            141,
            refs.size,
        )

        val expectedParents =
            mapOf(
                VexLifeWorkspaceBlueprint.QuickActions.ref to VexLifeWorkspaceBlueprint.Utilities.ref,
                VexLifeWorkspaceBlueprint.BrowserQueryError.ref to VexLifeWorkspaceBlueprint.BrowserQuery.ref,
                VexLifeWorkspaceBlueprint.BrowserReload.ref to VexLifeWorkspaceBlueprint.Header.ref,
                VexLifeWorkspaceBlueprint.Attach.ref to VexLifeWorkspaceBlueprint.Conversation.ref,
                VexLifeWorkspaceBlueprint.ComposerAttachments.ref to VexLifeWorkspaceBlueprint.Conversation.ref,
                VexLifeWorkspaceBlueprint.SettingsLocalSources.ref to VexLifeWorkspaceBlueprint.Settings.ref,
                VexLifeWorkspaceBlueprint.LocalSourcesList.ref to VexLifeWorkspaceBlueprint.SettingsLocalSources.ref,
                VexLifeWorkspaceBlueprint.LocalSourcesAdd.ref to VexLifeWorkspaceBlueprint.SettingsLocalSources.ref,
                VexLifeWorkspaceBlueprint.LocalSourceAllowChanges.ref to VexLifeWorkspaceBlueprint.SettingsLocalSources.ref,
                VexLifeWorkspaceBlueprint.LocalSourceStopChanges.ref to VexLifeWorkspaceBlueprint.SettingsLocalSources.ref,
                VexLifeWorkspaceBlueprint.VisionQuickAccess.ref to VexLifeWorkspaceBlueprint.QuickActions.ref,
                VexLifeWorkspaceBlueprint.Speech.ref to VexLifeWorkspaceBlueprint.QuickActions.ref,
                VexLifeWorkspaceBlueprint.Theme.ref to VexLifeWorkspaceBlueprint.QuickActions.ref,
                VexLifeWorkspaceBlueprint.Support.ref to VexLifeWorkspaceBlueprint.QuickActions.ref,
                VexLifeWorkspaceBlueprint.QuickActionsTrigger.ref to VexLifeWorkspaceBlueprint.QuickActions.ref,
                VexLifeWorkspaceBlueprint.ReleaseAlerts.ref to VexLifeWorkspaceBlueprint.QuickActions.ref,
                VexLifeWorkspaceBlueprint.ReleaseAlertsPanel.ref to VexLifeWorkspaceBlueprint.Utilities.ref,
                VexLifeWorkspaceBlueprint.ReleaseAlertsList.ref to VexLifeWorkspaceBlueprint.ReleaseAlertsPanel.ref,
                VexLifeWorkspaceBlueprint.LanguagePanel.ref to VexLifeWorkspaceBlueprint.Utilities.ref,
                VexLifeWorkspaceBlueprint.ReplyLanguageChoice.ref to VexLifeWorkspaceBlueprint.LanguagePanel.ref,
                VexLifeWorkspaceBlueprint.ScreenLanguageChoice.ref to VexLifeWorkspaceBlueprint.LanguagePanel.ref,
                VexLifeWorkspaceBlueprint.SpeechPanel.ref to VexLifeWorkspaceBlueprint.Utilities.ref,
                VexLifeWorkspaceBlueprint.SpeechEnabled.ref to VexLifeWorkspaceBlueprint.SpeechPanel.ref,
                VexLifeWorkspaceBlueprint.SpeechVoice.ref to VexLifeWorkspaceBlueprint.SpeechPanel.ref,
                VexLifeWorkspaceBlueprint.SpeechReadThoughts.ref to VexLifeWorkspaceBlueprint.SpeechPanel.ref,
                VexLifeWorkspaceBlueprint.SpeechRate.ref to VexLifeWorkspaceBlueprint.SpeechPanel.ref,
                VexLifeWorkspaceBlueprint.SpeechPrevious.ref to VexLifeWorkspaceBlueprint.SpeechPanel.ref,
                VexLifeWorkspaceBlueprint.SpeechPlayPause.ref to VexLifeWorkspaceBlueprint.SpeechPanel.ref,
                VexLifeWorkspaceBlueprint.SpeechStop.ref to VexLifeWorkspaceBlueprint.SpeechPanel.ref,
                VexLifeWorkspaceBlueprint.SpeechNext.ref to VexLifeWorkspaceBlueprint.SpeechPanel.ref,
                VexLifeWorkspaceBlueprint.SpeechChooseStart.ref to VexLifeWorkspaceBlueprint.SpeechPanel.ref,
                VexLifeWorkspaceBlueprint.SourceTemplateFamily.ref to VexLifeWorkspaceBlueprint.Browser.ref,
                VexLifeWorkspaceBlueprint.ArchivesTimeline.ref to VexLifeWorkspaceBlueprint.Archives.ref,
                VexLifeWorkspaceBlueprint.VexContext.ref to VexLifeWorkspaceBlueprint.Browser.ref,
                VexLifeWorkspaceBlueprint.VexContextProfile.ref to VexLifeWorkspaceBlueprint.VexContext.ref,
                VexLifeWorkspaceBlueprint.VexContextProfileBadge.ref to VexLifeWorkspaceBlueprint.VexContext.ref,
                VexLifeWorkspaceBlueprint.VexContextProfileSummary.ref to VexLifeWorkspaceBlueprint.VexContextProfile.ref,
                VexLifeWorkspaceBlueprint.VexContextProfileName.ref to VexLifeWorkspaceBlueprint.VexContextProfile.ref,
                VexLifeWorkspaceBlueprint.VexContextProfileFoundationMode.ref to VexLifeWorkspaceBlueprint.VexContextProfile.ref,
                VexLifeWorkspaceBlueprint.VexContextProfileFoundationText.ref to VexLifeWorkspaceBlueprint.VexContextProfile.ref,
                VexLifeWorkspaceBlueprint.VexContextProfileUse.ref to VexLifeWorkspaceBlueprint.VexContextProfile.ref,
                VexLifeWorkspaceBlueprint.VexContextProfileSave.ref to VexLifeWorkspaceBlueprint.VexContextProfile.ref,
                VexLifeWorkspaceBlueprint.VexContextProfileSaveAs.ref to VexLifeWorkspaceBlueprint.VexContextProfile.ref,
                VexLifeWorkspaceBlueprint.VexContextProfileRestore.ref to VexLifeWorkspaceBlueprint.VexContextProfile.ref,
                VexLifeWorkspaceBlueprint.VexContextProfilePortability.ref to VexLifeWorkspaceBlueprint.VexContextProfile.ref,
                VexLifeWorkspaceBlueprint.VexContextProfileImport.ref to VexLifeWorkspaceBlueprint.VexContextProfile.ref,
                VexLifeWorkspaceBlueprint.VexContextProfileHome.ref to VexLifeWorkspaceBlueprint.VexContextProfile.ref,
                VexLifeWorkspaceBlueprint.VexContextChanges.ref to VexLifeWorkspaceBlueprint.VexContext.ref,
                VexLifeWorkspaceBlueprint.VexContextLabs.ref to VexLifeWorkspaceBlueprint.VexContext.ref,
                VexLifeWorkspaceBlueprint.VexContextLabsStatus.ref to VexLifeWorkspaceBlueprint.VexContextLabs.ref,
                VexLifeWorkspaceBlueprint.VexContextLabsTest.ref to VexLifeWorkspaceBlueprint.VexContextLabs.ref,
                VexLifeWorkspaceBlueprint.VexContextLabsSaveSupported.ref to VexLifeWorkspaceBlueprint.VexContextLabs.ref,
                VexLifeWorkspaceBlueprint.VexContextLabsRestore.ref to VexLifeWorkspaceBlueprint.VexContextLabs.ref,
                VexLifeWorkspaceBlueprint.VexContextLabsReload.ref to VexLifeWorkspaceBlueprint.VexContextLabs.ref,
                VexLifeWorkspaceBlueprint.VexContextLabsClear.ref to VexLifeWorkspaceBlueprint.VexContextLabs.ref,
                VexLifeWorkspaceBlueprint.VexContextLabsInputBudget.ref to VexLifeWorkspaceBlueprint.VexContextLabs.ref,
                VexLifeWorkspaceBlueprint.VexContextLabsMemoryMode.ref to VexLifeWorkspaceBlueprint.VexContextLabs.ref,
                VexLifeWorkspaceBlueprint.VexContextLabsMemoryBudget.ref to VexLifeWorkspaceBlueprint.VexContextLabs.ref,
                VexLifeWorkspaceBlueprint.VexContextLabsNote.ref to VexLifeWorkspaceBlueprint.VexContextLabs.ref,
                VexLifeWorkspaceBlueprint.VexContextLabsFields.ref to VexLifeWorkspaceBlueprint.VexContextLabs.ref,
                VexLifeWorkspaceBlueprint.VexContextLabsLastRequest.ref to VexLifeWorkspaceBlueprint.VexContextLabs.ref,
                VexLifeWorkspaceBlueprint.VexContextInput.ref to VexLifeWorkspaceBlueprint.VexContext.ref,
                VexLifeWorkspaceBlueprint.VexContextInputFoundation.ref to VexLifeWorkspaceBlueprint.VexContextInput.ref,
                VexLifeWorkspaceBlueprint.VexContextInputMemory.ref to VexLifeWorkspaceBlueprint.VexContextInput.ref,
                VexLifeWorkspaceBlueprint.VexContextInputContinuity.ref to VexLifeWorkspaceBlueprint.VexContextInput.ref,
                VexLifeWorkspaceBlueprint.VexContextInputConversation.ref to VexLifeWorkspaceBlueprint.VexContextInput.ref,
                VexLifeWorkspaceBlueprint.VexContextInputBrowser.ref to VexLifeWorkspaceBlueprint.VexContextInput.ref,
                VexLifeWorkspaceBlueprint.VexContextInputRequest.ref to VexLifeWorkspaceBlueprint.VexContextInput.ref,
                VexLifeWorkspaceBlueprint.VexContextInputComponents.ref to VexLifeWorkspaceBlueprint.VexContextInput.ref,
                VexLifeWorkspaceBlueprint.VexContextProcess.ref to VexLifeWorkspaceBlueprint.VexContext.ref,
                VexLifeWorkspaceBlueprint.VexContextProcessRetention.ref to VexLifeWorkspaceBlueprint.VexContextProcess.ref,
                VexLifeWorkspaceBlueprint.VexContextOutput.ref to VexLifeWorkspaceBlueprint.VexContext.ref,
                VexLifeWorkspaceBlueprint.VexContextAfter.ref to VexLifeWorkspaceBlueprint.VexContext.ref,
                VexLifeWorkspaceBlueprint.BrowserFrameStatus.ref to VexLifeWorkspaceBlueprint.Browser.ref,
                VexLifeWorkspaceBlueprint.CompanionMode.ref to VexLifeWorkspaceBlueprint.Companion.ref,
                VexLifeWorkspaceBlueprint.Vision.ref to VexLifeWorkspaceBlueprint.Companion.ref,
                VexLifeWorkspaceBlueprint.CompanionViewSwitch.ref to VexLifeWorkspaceBlueprint.Companion.ref,
                VexLifeWorkspaceBlueprint.ContextShare.ref to VexLifeWorkspaceBlueprint.Conversation.ref,
                VexLifeWorkspaceBlueprint.ContextReceipt.ref to VexLifeWorkspaceBlueprint.Conversation.ref,
                VexLifeWorkspaceBlueprint.ContextReceiptView.ref to VexLifeWorkspaceBlueprint.ContextReceipt.ref,
                VexLifeWorkspaceBlueprint.VoiceInput.ref to VexLifeWorkspaceBlueprint.Conversation.ref,
                VexLifeWorkspaceBlueprint.ContextWindow.ref to VexLifeWorkspaceBlueprint.SettingsContext.ref,
                VexLifeWorkspaceBlueprint.MemoryResource.ref to VexLifeWorkspaceBlueprint.SettingsResources.ref,
                VexLifeWorkspaceBlueprint.PrivateMode.ref to VexLifeWorkspaceBlueprint.Companion.ref,
                VexLifeWorkspaceBlueprint.PrivateBanner.ref to VexLifeWorkspaceBlueprint.Conversation.ref,
                VexLifeWorkspaceBlueprint.PrivateNoteKeep.ref to VexLifeWorkspaceBlueprint.PrivateBanner.ref,
                VexLifeWorkspaceBlueprint.PrivateNoteDecline.ref to VexLifeWorkspaceBlueprint.PrivateBanner.ref,
                VexLifeWorkspaceBlueprint.PrivateNotePropose.ref to VexLifeWorkspaceBlueprint.PrivateBanner.ref,
                VexLifeWorkspaceBlueprint.ContextInspectorClose.ref to VexLifeWorkspaceBlueprint.ContextInspector.ref,
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
