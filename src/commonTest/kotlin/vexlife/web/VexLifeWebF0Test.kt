package vexlife.web

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import vexlife.workspace.VexLifeWorkspaceBlueprint

class VexLifeWebF0Test {
    private val coordinates =
        VexLifeWebF0
            .Surface
            .coordinates()

    @Test
    fun completedViewSliceKeepsExactBlueprintIdentity() {
        val byRef =
            coordinates.associateBy {
                it.ref
            }

        assertEquals(
            VexLifeWorkspaceBlueprint.Browser.ref,
            byRef
                .getValue(
                    VexLifeWorkspaceBlueprint
                        .BrowserViewport
                        .ref,
                )
                .parentRef,
        )
        assertEquals(
            VexLifeWorkspaceBlueprint.Conversation.ref,
            byRef
                .getValue(
                    VexLifeWorkspaceBlueprint
                        .Composer
                        .ref,
                )
                .parentRef,
        )
        assertEquals(
            VexLifeWorkspaceBlueprint.Composer.ref,
            byRef
                .getValue(
                    VexLifeWorkspaceBlueprint
                        .Send
                        .ref,
                )
                .parentRef,
        )
    }

    @Test
    fun splitRolesAreExplicitRatherThanChildOrderConvention() {
        val split =
            VexLifeWebF0.SplitContainer

        assertEquals(
            VexLifeWorkspaceBlueprint.Browser.ref,
            split.leading.identity.ref,
        )
        assertEquals(
            VexLifeWebF0.Refs.SplitHandle.value,
            split.handle.identity.ref,
        )
        assertEquals(
            VexLifeWorkspaceBlueprint.Companion.ref,
            split.trailing.identity.ref,
        )

        val mode =
            assertIs<
                VexWeb.Parameter.SplitMode.Balanced
            >(
                split.mode,
            )

        assertEquals(
            1,
            mode.ratio.leadingParts,
        )
        assertEquals(
            1,
            mode.ratio.trailingParts,
        )
    }

    @Test
    fun splitHandleBindsLocalPresentationToTheExactSplit() {
        val binding =
            assertIs<
                VexWeb.Binding.LocalPresentation
            >(
                VexLifeWebF0
                    .SplitHandle
                    .binding,
            )

        assertEquals(
            VexLifeWebF0
                .Refs
                .SplitContainer,
            binding.target,
        )

        assertTrue(
            VexWeb.Interaction.Resize in
                VexLifeWebF0.SplitHandle.interactions,
        )
        assertTrue(
            VexWeb.Interaction.CollapseLeading in
                VexLifeWebF0.SplitHandle.interactions,
        )
        assertTrue(
            VexWeb.Interaction.CollapseTrailing in
                VexLifeWebF0.SplitHandle.interactions,
        )
    }

    @Test
    fun unboundSendCarriesItsMissingCapabilityAndFeedbackTarget() {
        val binding =
            assertIs<
                VexWeb.Binding.Unbound
            >(
                VexLifeWebF0
                    .Send
                    .binding,
            )

        val fallback =
            assertIs<
                VexWeb.Fallback.NotYetImplemented
            >(
                binding.fallback,
            )

        assertEquals(
            "conversation.send",
            fallback.capabilityRef.value,
        )
        assertEquals(
            VexLifeWebF0.Refs.Snackbar,
            fallback.feedbackTarget,
        )
    }

    @Test
    fun authoredDepthIsRelativeRatherThanAPlatformZIndex() {
        assertTrue(
            VexWeb.Depth.Chrome.isAbove(
                VexWeb.Depth.Content,
            ),
        )
        assertTrue(
            VexWeb.Depth.Transient.isAbove(
                VexWeb.Depth.Chrome,
            ),
        )
        assertTrue(
            VexWeb.Depth.Modal.isAbove(
                VexWeb.Depth.Transient,
            ),
        )
    }

    @Test
    fun allFractalCoordinatesAreUniqueAndSourceAddressable() {
        val refs =
            coordinates.map {
                it.ref
            }

        assertEquals(
            refs.size,
            refs.toSet().size,
        )
        assertTrue(
            VexLifeWorkspaceBlueprint.Send.ref in
                refs,
        )
        assertTrue(
            VexLifeWebF0.Refs.Snackbar.value in
                refs,
        )
    }
}

// [VXG RealForever]
