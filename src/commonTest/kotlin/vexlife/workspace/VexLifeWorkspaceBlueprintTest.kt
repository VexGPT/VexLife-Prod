package vexlife.workspace

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

        assertTrue(
            "element.vexlife-workspace.browser.viewport" in
                refs,
        )
        assertTrue(
            "element.vexlife-workspace.companion.conversation.send" in
                refs,
        )
        assertTrue(
            "element.vexlife-workspace.companion.settings.reset" in
                refs,
        )
    }
}

// [VXG RealForever]
