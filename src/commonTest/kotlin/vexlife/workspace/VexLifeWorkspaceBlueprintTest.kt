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
            39,
            refs.size,
        )

        val expectedParents =
            mapOf(
                VexLifeWorkspaceBlueprint.QuickActions.ref to VexLifeWorkspaceBlueprint.Utilities.ref,
                VexLifeWorkspaceBlueprint.BrowserFrameStatus.ref to VexLifeWorkspaceBlueprint.Browser.ref,
                VexLifeWorkspaceBlueprint.Vision.ref to VexLifeWorkspaceBlueprint.Companion.ref,
                VexLifeWorkspaceBlueprint.CompanionViewSwitch.ref to VexLifeWorkspaceBlueprint.Companion.ref,
                VexLifeWorkspaceBlueprint.ContextShare.ref to VexLifeWorkspaceBlueprint.Conversation.ref,
                VexLifeWorkspaceBlueprint.ContextReceipt.ref to VexLifeWorkspaceBlueprint.Conversation.ref,
                VexLifeWorkspaceBlueprint.ContextWindow.ref to VexLifeWorkspaceBlueprint.SettingsContext.ref,
                VexLifeWorkspaceBlueprint.MemoryResource.ref to VexLifeWorkspaceBlueprint.SettingsResources.ref,
                VexLifeWorkspaceBlueprint.CleanChat.ref to VexLifeWorkspaceBlueprint.SettingsReset.ref,
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
