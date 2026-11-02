package vexlife.web

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import vexlife.workspace.VexLifeWorkspaceBlueprint

class VexLifeWebF0Test {
    private val coordinates =
        VexLifeWebF0
            .Surface
            .coordinates()

    @Test
    fun blueprintParentAndManifestationContainmentRemainDistinct() {
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
                .sourceParentRef,
        )
        assertEquals(
            VexLifeWorkspaceBlueprint.Conversation.ref,
            byRef
                .getValue(
                    VexLifeWorkspaceBlueprint
                        .Composer
                        .ref,
                )
                .sourceParentRef,
        )
        assertEquals(
            VexLifeWorkspaceBlueprint.Conversation.ref,
            byRef
                .getValue(
                    VexLifeWorkspaceBlueprint
                        .Send
                        .ref,
                )
                .sourceParentRef,
        )
        assertEquals(
            VexLifeWorkspaceBlueprint.Composer.ref,
            byRef
                .getValue(
                    VexLifeWorkspaceBlueprint
                        .Send
                        .ref,
                )
                .containerRef,
        )
    }

    @Test
    fun splitRolesAreExplicitRatherThanChildOrderConvention() {
        val split =
            assertIs<
                VexWeb.Split
            >(
                VexLifeWebF0
                    .Surface
                    .requireNode(
                        VexLifeWebF0
                            .Refs
                            .SplitContainer,
                    ),
            )

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

        assertEquals(
            VexWeb.Parameter.SplitAxis.Horizontal,
            split.axis,
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
        val splitHandle =
            VexLifeWebF0
                .Surface
                .requireNode(
                    VexLifeWebF0
                        .Refs
                        .SplitHandle,
                )

        val binding =
            assertIs<
                VexWeb.Binding.LocalPresentation
            >(
                splitHandle.binding,
            )

        assertEquals(
            VexLifeWebF0
                .Refs
                .SplitContainer,
            binding.target,
        )

        assertTrue(
            VexWeb.Interaction.Resize in
                splitHandle.interactions,
        )
        assertTrue(
            VexWeb.Interaction.CollapseLeading in
                splitHandle.interactions,
        )
        assertTrue(
            VexWeb.Interaction.CollapseTrailing in
                splitHandle.interactions,
        )
    }

    @Test
    fun unboundSendCarriesItsMissingCapabilityAndFeedbackTarget() {
        val send =
            VexLifeWebF0
                .Surface
                .requireNode(
                    VexLifeWorkspaceBlueprint.Send,
                )

        val binding =
            assertIs<
                VexWeb.Binding.Unbound
            >(
                send.binding,
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
    fun interactiveNodesCannotExistWithoutAnExplicitBinding() {
        assertFailsWith<IllegalArgumentException> {
            VexWeb.Structural(
                ref =
                    VexWeb.StructuralRef.of(
                        "vexweb.test.unbound-control",
                    ),
                placement =
                    VexWeb.Placement.InFlow,
                depth =
                    VexWeb.Depth.Content,
                presentation =
                    VexWeb.Presentation.PrimaryAction,
                interactions =
                    setOf(
                        VexWeb.Interaction.Activate,
                    ),
            )
        }
    }

    @Test
    fun f1AuthoringRequiresEverySplitRole() {
        assertFailsWith<IllegalArgumentException> {
            vexWebSurface(
                VexLifeWorkspaceBlueprint,
            ) {
                content {
                    split(
                        ref =
                            VexWeb.StructuralRef.of(
                                "vexweb.test.incomplete-split",
                            ),
                        axis =
                            VexWeb.Parameter.SplitAxis.Horizontal,
                        mode =
                            VexWeb.Parameter
                                .SplitMode
                                .Balanced(),
                    ) {
                        leading {
                            structural(
                                ref =
                                    VexWeb.StructuralRef.of(
                                        "vexweb.test.incomplete-split.leading",
                                    ),
                                placement =
                                    VexWeb.Placement.Leading,
                                depth =
                                    VexWeb.Depth.Content,
                                presentation =
                                    VexWeb.Presentation.Surface,
                            )
                        }
                    }
                }

                overlayRoot {
                    structural(
                        ref =
                            VexWeb.StructuralRef.of(
                                "vexweb.test.overlay",
                            ),
                        placement =
                            VexWeb.Placement.Overlay,
                        depth =
                            VexWeb.Depth.Transient,
                        presentation =
                            VexWeb.Presentation.Surface,
                    )
                }
            }
        }
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
