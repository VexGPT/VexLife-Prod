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
            bundle.html.contains(
                "id=\"vision-quick-access-source\" hidden",
            ),
            "the authored VisionQuickAccess control should remain the source coordinate",
        )
        assertTrue(
            bundle.javascript.contains(
                "configureSourceQuickAction(",
            ) &&
                bundle.javascript.contains(
                    "'vision-quick-access-source'",
                ),
            "the renderer should reuse the authored VisionQuickAccess control",
        )
        assertTrue(
            bundle.javascript.contains(
                "ui.visionControl?.click();",
            ),
            "VisionQuickAccess should realize its authored LocalInteraction through the existing Vision control",
        )
        assertFalse(
            bundle.javascript.contains(
                "action.key === 'vision'",
            ),
            "the removed parallel renderer dispatcher must not return",
        )
        assertFalse(
            bundle.javascript.contains(
                "const QUICK_ACTIONS = [",
            ),
            "the renderer must not recreate a second quick-action registry",
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
