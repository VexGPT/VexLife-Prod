package vexlife.web

import kotlin.test.Test
import kotlin.test.assertTrue
import vexlife.workspace.VexLifeWorkspaceBlueprint

class VexLifeVisionQuickAccessTest {
    @Test
    fun visionQuickAccessIsPresentInTheGlobalUtilityRowWithoutFabricatingLookNow() {
        val bundle =
            VexLifeWebP1RuntimeArtifacts.generate()

        assertTrue(
            bundle.html.contains(
                "data-vex-ref=\"${VexLifeWorkspaceBlueprint.VisionQuickAccess.ref}\"",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "{ key: 'vision', label: 'Vision'",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "action.key === 'vision'",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "Vision · Held — Look Now isn't connected to the macOS picker yet.",
            ),
        )
    }
}

// [VXG RealForever]
