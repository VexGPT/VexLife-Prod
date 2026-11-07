package vexlife.workspace

import kotlin.test.Test
import kotlin.test.assertEquals

class VexLifeWorkspaceBlueprintTest {
    @Test
    fun initialEngagementHasStableUniqueNestedAddresses() {
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
            21,
            refs.size,
        )

        assertEquals(
            "element.vexlife-workspace.header.utilities.vex-summon",
            VexLifeWorkspaceBlueprint
                .VexSummon
                .ref,
        )
        assertEquals(
            VexLifeWorkspaceBlueprint.Utilities.ref,
            VexLifeWorkspaceBlueprint
                .VexSummon
                .parentRef,
        )

        assertEquals(
            "element.vexlife-workspace.companion.conversation.jump-latest",
            VexLifeWorkspaceBlueprint
                .ConversationJumpLatest
                .ref,
        )
        assertEquals(
            VexLifeWorkspaceBlueprint.Conversation.ref,
            VexLifeWorkspaceBlueprint
                .ConversationJumpLatest
                .parentRef,
        )

        assertEquals(
            "element.vexlife-workspace.browser.viewport",
            VexLifeWorkspaceBlueprint
                .BrowserViewport
                .ref,
        )
        assertEquals(
            VexLifeWorkspaceBlueprint.Browser.ref,
            VexLifeWorkspaceBlueprint
                .BrowserViewport
                .parentRef,
        )

        assertEquals(
            "element.vexlife-workspace.companion.conversation.send",
            VexLifeWorkspaceBlueprint
                .Send
                .ref,
        )
        assertEquals(
            VexLifeWorkspaceBlueprint.Conversation.ref,
            VexLifeWorkspaceBlueprint
                .Send
                .parentRef,
        )

        assertEquals(
            "element.vexlife-workspace.companion.settings.reset",
            VexLifeWorkspaceBlueprint
                .SettingsReset
                .ref,
        )
    }
}

// [VXG RealForever]
