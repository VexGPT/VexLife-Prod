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
    }

    @Test
    fun referenceDisplayStateProjectsPresentationAndCarriesOneParameterIdentity() {
        val family =
            ReferenceToggleFamily()
        val projection =
            family.projectionFor(
                ReferenceToggleFamily
                    .State
                    .Inactive,
            )
        val activation =
            ReferenceToggleFamily
                .Parameter
                .Activation(
                    active =
                        true,
                )
        val control =
            ReferenceToggleFamily
                .Control
                .Select(
                    activation =
                        activation,
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
                    control,
            )

        val effect =
            assertIs<
                ReferenceToggleFamily.Effect.Remember
            >(
                act?.effect,
            )

        assertSame(
            activation,
            effect.activation,
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
