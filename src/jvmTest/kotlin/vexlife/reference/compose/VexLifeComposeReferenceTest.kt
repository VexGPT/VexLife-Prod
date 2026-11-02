package vexlife.reference.compose

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import vexlife.workspace.VexLifeWorkspaceBlueprint

class VexLifeComposeReferenceTest {
    @Test
    fun rendererConsumesCompilerVisibleBlueprintHandles() {
        assertEquals(
            "element.vexlife-workspace.browser.viewport",
            VexLifeWorkspaceBlueprint
                .BrowserViewport
                .ref,
        )
        assertSame(
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
    }

    @Test
    fun referenceDisplayStateProjectsPresentationWithoutExposingEffectMechanics() {
        val family =
            ReferenceToggleFamily()
        val projection =
            family.projectionFor(
                ReferenceToggleFamily
                    .State
                    .Inactive,
            )

        assertEquals(
            false,
            projection
                .displayState
                .checked,
        )

        val act =
            family.resolve(
                currentState =
                    projection.state,
                control =
                    ReferenceToggleFamily
                        .Control
                        .Select(
                            active =
                                true,
                        ),
            )

        assertIs<
            ReferenceToggleFamily.Effect.Remember
        >(
            act?.effect,
        )
        assertEquals(
            ReferenceToggleFamily
                .State
                .Active,
            act?.admittedState,
        )
    }
}

// [VXG RealForever]
