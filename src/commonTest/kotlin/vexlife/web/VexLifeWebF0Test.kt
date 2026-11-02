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
            VexLifeWorkspaceBlueprint.Conversation.ref,
            byRef
                .getValue(
                    VexLifeWorkspaceBlueprint.Send.ref,
                )
                .sourceParentRef,
        )
        assertEquals(
            VexLifeWorkspaceBlueprint.Composer.ref,
            byRef
                .getValue(
                    VexLifeWorkspaceBlueprint.Send.ref,
                )
                .containerRef,
        )
    }

    @Test
    fun browserAndCompanionAreRegionsBoundToTheSameNamedSeam() {
        val byRef =
            VexLifeWebF0
                .Surface
                .spatialRegions()
                .associateBy {
                    it.ref
                }

        assertEquals(
            VexWeb.Bounds(
                start =
                    VexWeb.XBoundary.FrameStart,
                end =
                    VexWeb.XBoundary.Seam(
                        VexLifeWebF0.Refs.BrowserCompanionSeam,
                    ),
                top =
                    VexWeb.YBoundary.FrameTop,
                bottom =
                    VexWeb.YBoundary.FrameBottom,
            ),
            byRef
                .getValue(
                    VexLifeWebF0.Refs.BrowserRegion.value,
                )
                .bounds,
        )
        assertEquals(
            VexWeb.Bounds(
                start =
                    VexWeb.XBoundary.Seam(
                        VexLifeWebF0.Refs.BrowserCompanionSeam,
                    ),
                end =
                    VexWeb.XBoundary.FrameEnd,
                top =
                    VexWeb.YBoundary.FrameTop,
                bottom =
                    VexWeb.YBoundary.FrameBottom,
            ),
            byRef
                .getValue(
                    VexLifeWebF0.Refs.CompanionRegion.value,
                )
                .bounds,
        )
    }

    @Test
    fun seamHandleBindsToTheExactSpatialBoundary() {
        val binding =
            assertIs<
                VexWeb.Binding.LocalSpatial
            >(
                VexLifeWebF0
                    .SeamHandle
                    .binding,
            )

        assertEquals(
            VexLifeWebF0
                .Refs
                .BrowserCompanionSeam,
            binding.target,
        )
        assertTrue(
            VexWeb.Interaction.Resize in
                VexLifeWebF0.SeamHandle.interactions,
        )
        assertTrue(
            VexWeb.Interaction.CollapseBefore in
                VexLifeWebF0.SeamHandle.interactions,
        )
        assertTrue(
            VexWeb.Interaction.CollapseAfter in
                VexLifeWebF0.SeamHandle.interactions,
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
    fun spatialBindingCannotTargetAnImaginarySeam() {
        val unknown =
            VexWeb.SeamRef.Vertical.of(
                "vexweb.test.seam.unknown",
            )

        assertFailsWith<IllegalArgumentException> {
            VexWeb.Surface(
                screenRef =
                    VexLifeWorkspaceBlueprint.screenRef,
                content =
                    VexWeb.Frame(
                        ref =
                            VexWeb.StructuralRef.of(
                                "vexweb.test.frame",
                            ),
                        seams =
                            emptyList(),
                        regions =
                            listOf(
                                VexWeb.Region(
                                    ref =
                                        VexWeb.StructuralRef.of(
                                            "vexweb.test.frame.region",
                                        ),
                                    bounds =
                                        VexWeb.Bounds(
                                            start =
                                                VexWeb.XBoundary.FrameStart,
                                            end =
                                                VexWeb.XBoundary.FrameEnd,
                                            top =
                                                VexWeb.YBoundary.FrameTop,
                                            bottom =
                                                VexWeb.YBoundary.FrameBottom,
                                        ),
                                    content =
                                        VexWeb.Structural(
                                            ref =
                                                VexWeb.StructuralRef.of(
                                                    "vexweb.test.frame.content",
                                                ),
                                            placement =
                                                VexWeb.Placement.FillAvailable,
                                            depth =
                                                VexWeb.Depth.Content,
                                            presentation =
                                                VexWeb.Presentation.Surface,
                                        ),
                                ),
                            ),
                        chrome =
                            listOf(
                                VexWeb.Structural(
                                    ref =
                                        VexWeb.StructuralRef.of(
                                            "vexweb.test.frame.handle",
                                        ),
                                    placement =
                                        VexWeb.Placement.Overlay,
                                    depth =
                                        VexWeb.Depth.Chrome,
                                    presentation =
                                        VexWeb.Presentation.SeamHandle,
                                    interactions =
                                        setOf(
                                            VexWeb.Interaction.Resize,
                                        ),
                                    binding =
                                        VexWeb.Binding.LocalSpatial(
                                            target =
                                                unknown,
                                        ),
                                ),
                            ),
                    ),
                overlayRoot =
                    VexWeb.Structural(
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
                    ),
            ).coordinates()
        }
    }

    @Test
    fun allNodeAndSeamIdentitiesRemainSourceAddressable() {
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
            VexLifeWebF0.Refs.BrowserRegion.value in
                refs,
        )
        assertTrue(
            VexLifeWebF0.Refs.Snackbar.value in
                refs,
        )
    }
}

// [VXG RealForever]
