package vexlife.web

import kotlin.test.Test
import kotlin.test.assertFalse
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
                "action.key === 'vision'",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "void activateVision();",
            ),
        )
        // Vision copy and its one lawful Control come from VexLifeVisionFamily via the service; the
        // renderer only shows state.vision.display and never offers Look Now while LOOK is held.
        assertTrue(
            bundle.javascript.contains(
                "const controlRef = state.vision?.display?.primaryControlRef;",
            ),
        )
        assertFalse(
            bundle.javascript.contains(
                "Look Now",
            ),
        )
    }
}

// [VXG RealForever]
