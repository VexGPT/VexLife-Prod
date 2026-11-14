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
            46,
            refs.size,
        )

        val expectedParents =
            mapOf(
                VexLifeWorkspaceBlueprint.QuickActions.ref to VexLifeWorkspaceBlueprint.Utilities.ref,
                VexLifeWorkspaceBlueprint.VisionQuickAccess.ref to VexLifeWorkspaceBlueprint.QuickActions.ref,
                VexLifeWorkspaceBlueprint.Speech.ref to VexLifeWorkspaceBlueprint.QuickActions.ref,
                VexLifeWorkspaceBlueprint.SourceTemplateFamily.ref to VexLifeWorkspaceBlueprint.Browser.ref,
                VexLifeWorkspaceBlueprint.ArchivesTimeline.ref to VexLifeWorkspaceBlueprint.Archives.ref,
                VexLifeWorkspaceBlueprint.BrowserFrameStatus.ref to VexLifeWorkspaceBlueprint.Browser.ref,
                VexLifeWorkspaceBlueprint.Vision.ref to VexLifeWorkspaceBlueprint.Companion.ref,
                VexLifeWorkspaceBlueprint.CompanionViewSwitch.ref to VexLifeWorkspaceBlueprint.Companion.ref,
                VexLifeWorkspaceBlueprint.ContextShare.ref to VexLifeWorkspaceBlueprint.Conversation.ref,
                VexLifeWorkspaceBlueprint.ContextReceipt.ref to VexLifeWorkspaceBlueprint.Conversation.ref,
                VexLifeWorkspaceBlueprint.ContextWindow.ref to VexLifeWorkspaceBlueprint.SettingsContext.ref,
                VexLifeWorkspaceBlueprint.MemoryResource.ref to VexLifeWorkspaceBlueprint.SettingsResources.ref,
                VexLifeWorkspaceBlueprint.PrivateMode.ref to VexLifeWorkspaceBlueprint.Companion.ref,
                VexLifeWorkspaceBlueprint.PrivateBanner.ref to VexLifeWorkspaceBlueprint.Conversation.ref,
                VexLifeWorkspaceBlueprint.ContinuityMemory.ref to VexLifeWorkspaceBlueprint.SettingsContinuity.ref,
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
