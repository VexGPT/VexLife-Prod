package vexlife.web

import vexlife.workspace.VexLifeWorkspaceBlueprint

/**
 * First relational VexWeb spatial specimen.
 *
 * The browser/companion geometry is authored through Frame edges, one named Seam, and two Regions.
 * No binary decomposition language is source authority.
 */
object VexLifeWebF0 {
    object Refs {
        val WorkspaceFrame =
            VexWeb.StructuralRef.of(
                "vexweb.vexlife-workspace.frame",
            )

        val BrowserCompanionSeam =
            VexWeb.SeamRef.Vertical.of(
                "vexweb.vexlife-workspace.seam.browser-companion",
            )

        val BrowserRegion =
            VexWeb.StructuralRef.of(
                "vexweb.vexlife-workspace.region.browser",
            )

        val CompanionRegion =
            VexWeb.StructuralRef.of(
                "vexweb.vexlife-workspace.region.companion",
            )

        val SeamHandle =
            VexWeb.StructuralRef.of(
                "vexweb.vexlife-workspace.seam.browser-companion.handle",
            )

        val OverlayRoot =
            VexWeb.StructuralRef.of(
                "vexweb.vexlife-workspace.overlay",
            )

        val Snackbar =
            VexWeb.StructuralRef.of(
                "vexweb.vexlife-workspace.overlay.snackbar",
            )
    }

    val Snackbar =
        VexWeb.Structural(
            ref =
                Refs.Snackbar,
            placement =
                VexWeb.Placement.Overlay,
            depth =
                VexWeb.Depth.Transient,
            presentation =
                VexWeb.Presentation.TransientFeedback,
        )

    val OverlayRoot =
        VexWeb.Structural(
            ref =
                Refs.OverlayRoot,
            placement =
                VexWeb.Placement.Overlay,
            depth =
                VexWeb.Depth.Transient,
            presentation =
                VexWeb.Presentation.Surface,
            children =
                listOf(
                    Snackbar,
                ),
        )

    val Send =
        VexWeb.Element(
            element =
                VexLifeWorkspaceBlueprint.Send,
            placement =
                VexWeb.Placement.Trailing,
            depth =
                VexWeb.Depth.Content,
            presentation =
                VexWeb.Presentation.PrimaryAction,
            interactions =
                setOf(
                    VexWeb.Interaction.Activate,
                ),
            binding =
                VexWeb.Binding.Unbound(
                    fallback =
                        VexWeb.Fallback.NotYetImplemented(
                            capabilityRef =
                                VexWeb.CapabilityRef.of(
                                    "conversation.send",
                                ),
                            feedbackTarget =
                                Refs.Snackbar,
                        ),
                ),
        )

    val Composer =
        VexWeb.Element(
            element =
                VexLifeWorkspaceBlueprint.Composer,
            placement =
                VexWeb.Placement.InFlow,
            depth =
                VexWeb.Depth.Content,
            presentation =
                VexWeb.Presentation.Surface,
            children =
                listOf(
                    Send,
                ),
        )

    val Conversation =
        VexWeb.Element(
            element =
                VexLifeWorkspaceBlueprint.Conversation,
            placement =
                VexWeb.Placement.FillAvailable,
            depth =
                VexWeb.Depth.Content,
            presentation =
                VexWeb.Presentation.Surface,
            children =
                listOf(
                    Composer,
                ),
        )

    val CompanionPane =
        VexWeb.Element(
            element =
                VexLifeWorkspaceBlueprint.Companion,
            placement =
                VexWeb.Placement.FillAvailable,
            depth =
                VexWeb.Depth.Content,
            presentation =
                VexWeb.Presentation.Surface,
            children =
                listOf(
                    Conversation,
                ),
        )

    val BrowserViewport =
        VexWeb.Element(
            element =
                VexLifeWorkspaceBlueprint.BrowserViewport,
            placement =
                VexWeb.Placement.FillAvailable,
            depth =
                VexWeb.Depth.Content,
            presentation =
                VexWeb.Presentation.Surface,
        )

    val BrowserPane =
        VexWeb.Element(
            element =
                VexLifeWorkspaceBlueprint.Browser,
            placement =
                VexWeb.Placement.FillAvailable,
            depth =
                VexWeb.Depth.Content,
            presentation =
                VexWeb.Presentation.Surface,
            children =
                listOf(
                    BrowserViewport,
                ),
        )

    val SeamHandle =
        VexWeb.Structural(
            ref =
                Refs.SeamHandle,
            placement =
                VexWeb.Placement.Overlay,
            depth =
                VexWeb.Depth.Chrome,
            presentation =
                VexWeb.Presentation.SeamHandle,
            interactions =
                setOf(
                    VexWeb.Interaction.Resize,
                    VexWeb.Interaction.CollapseBefore,
                    VexWeb.Interaction.CollapseAfter,
                ),
            binding =
                VexWeb.Binding.LocalSpatial(
                    target =
                        Refs.BrowserCompanionSeam,
                ),
        )

    val WorkspaceFrame =
        VexWeb.Frame(
            ref =
                Refs.WorkspaceFrame,
            seams =
                listOf(
                    VexWeb.Seam.Vertical(
                        ref =
                            Refs.BrowserCompanionSeam,
                        between =
                            VexWeb.XSpan(
                                start =
                                    VexWeb.XBoundary.FrameStart,
                                end =
                                    VexWeb.XBoundary.FrameEnd,
                            ),
                        allocation =
                            VexWeb.Parameter.Allocation.Ratio.Equal,
                        span =
                            VexWeb.YSpan(
                                top =
                                    VexWeb.YBoundary.FrameTop,
                                bottom =
                                    VexWeb.YBoundary.FrameBottom,
                            ),
                    ),
                ),
            regions =
                listOf(
                    VexWeb.Region(
                        ref =
                            Refs.BrowserRegion,
                        bounds =
                            VexWeb.Bounds(
                                start =
                                    VexWeb.XBoundary.FrameStart,
                                end =
                                    VexWeb.XBoundary.Seam(
                                        Refs.BrowserCompanionSeam,
                                    ),
                                top =
                                    VexWeb.YBoundary.FrameTop,
                                bottom =
                                    VexWeb.YBoundary.FrameBottom,
                            ),
                        content =
                            BrowserPane,
                    ),
                    VexWeb.Region(
                        ref =
                            Refs.CompanionRegion,
                        bounds =
                            VexWeb.Bounds(
                                start =
                                    VexWeb.XBoundary.Seam(
                                        Refs.BrowserCompanionSeam,
                                    ),
                                end =
                                    VexWeb.XBoundary.FrameEnd,
                                top =
                                    VexWeb.YBoundary.FrameTop,
                                bottom =
                                    VexWeb.YBoundary.FrameBottom,
                            ),
                        content =
                            CompanionPane,
                    ),
                ),
            chrome =
                listOf(
                    SeamHandle,
                ),
        )

    val Surface =
        VexWeb.Surface(
            screenRef =
                VexLifeWorkspaceBlueprint.screenRef,
            content =
                WorkspaceFrame,
            overlayRoot =
                OverlayRoot,
        )
}

// [VXG RealForever]
