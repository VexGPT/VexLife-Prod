package vexlife.web

import vexlife.workspace.VexLifeWorkspaceBlueprint

/**
 * First relational VexWeb specimen, now authored through the F1 relational authoring surface.
 *
 * Frame / Seam / Region remain accepted manifestation meaning. [VexWebAuthoring] only removes
 * constructor ceremony; it emits the same accepted VexWeb objects and validators.
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

        val ConversationSendCapability =
            VexWeb.CapabilityRef.of(
                "conversation.send",
            )
    }

    val Snackbar =
        VexWebAuthoring.structural(
            ref = Refs.Snackbar,
            placement = VexWeb.Placement.Overlay,
            depth = VexWeb.Depth.Transient,
            presentation = VexWeb.Presentation.TransientFeedback,
        )

    val OverlayRoot =
        VexWebAuthoring.structural(
            ref = Refs.OverlayRoot,
            placement = VexWeb.Placement.Overlay,
            depth = VexWeb.Depth.Transient,
            presentation = VexWeb.Presentation.Surface,
            children =
                listOf(
                    Snackbar,
                ),
        )

    val Send =
        VexWebAuthoring.element(
            element = VexLifeWorkspaceBlueprint.Send,
            placement = VexWeb.Placement.Trailing,
            depth = VexWeb.Depth.Content,
            presentation = VexWeb.Presentation.PrimaryAction,
            interactions =
                setOf(
                    VexWeb.Interaction.Activate,
                ),
            binding =
                VexWebAuthoring.unbound(
                    capabilityRef = Refs.ConversationSendCapability,
                    feedbackTarget = Refs.Snackbar,
                ),
        )

    val Composer =
        VexWebAuthoring.element(
            element = VexLifeWorkspaceBlueprint.Composer,
            placement = VexWeb.Placement.InFlow,
            depth = VexWeb.Depth.Content,
            presentation = VexWeb.Presentation.Surface,
            children =
                listOf(
                    Send,
                ),
        )

    val Conversation =
        VexWebAuthoring.element(
            element = VexLifeWorkspaceBlueprint.Conversation,
            placement = VexWeb.Placement.FillAvailable,
            depth = VexWeb.Depth.Content,
            presentation = VexWeb.Presentation.Surface,
            children =
                listOf(
                    Composer,
                ),
        )

    val CompanionPane =
        VexWebAuthoring.element(
            element = VexLifeWorkspaceBlueprint.Companion,
            placement = VexWeb.Placement.FillAvailable,
            depth = VexWeb.Depth.Content,
            presentation = VexWeb.Presentation.Surface,
            children =
                listOf(
                    Conversation,
                ),
        )

    val BrowserViewport =
        VexWebAuthoring.element(
            element = VexLifeWorkspaceBlueprint.BrowserViewport,
            placement = VexWeb.Placement.FillAvailable,
            depth = VexWeb.Depth.Content,
            presentation = VexWeb.Presentation.Surface,
        )

    val BrowserPane =
        VexWebAuthoring.element(
            element = VexLifeWorkspaceBlueprint.Browser,
            placement = VexWeb.Placement.FillAvailable,
            depth = VexWeb.Depth.Content,
            presentation = VexWeb.Presentation.Surface,
            children =
                listOf(
                    BrowserViewport,
                ),
        )

    val SeamHandle =
        VexWebAuthoring.structural(
            ref = Refs.SeamHandle,
            placement = VexWeb.Placement.Overlay,
            depth = VexWeb.Depth.Chrome,
            presentation = VexWeb.Presentation.SeamHandle,
            interactions =
                setOf(
                    VexWeb.Interaction.Resize,
                    VexWeb.Interaction.CollapseBefore,
                    VexWeb.Interaction.CollapseAfter,
                ),
            binding =
                VexWebAuthoring.localSpatial(
                    Refs.BrowserCompanionSeam,
                ),
        )

    val WorkspaceFrame =
        VexWebAuthoring.frame(
            Refs.WorkspaceFrame,
        ) {
            verticalSeam(
                ref = Refs.BrowserCompanionSeam,
                betweenStart = VexWeb.XBoundary.FrameStart,
                betweenEnd = VexWeb.XBoundary.FrameEnd,
                allocation = VexWeb.Parameter.Allocation.Ratio.Equal,
                spanTop = VexWeb.YBoundary.FrameTop,
                spanBottom = VexWeb.YBoundary.FrameBottom,
            )

            region(
                ref = Refs.BrowserRegion,
                start = VexWeb.XBoundary.FrameStart,
                end =
                    VexWeb.XBoundary.Seam(
                        Refs.BrowserCompanionSeam,
                    ),
                top = VexWeb.YBoundary.FrameTop,
                bottom = VexWeb.YBoundary.FrameBottom,
                content = BrowserPane,
            )

            region(
                ref = Refs.CompanionRegion,
                start =
                    VexWeb.XBoundary.Seam(
                        Refs.BrowserCompanionSeam,
                    ),
                end = VexWeb.XBoundary.FrameEnd,
                top = VexWeb.YBoundary.FrameTop,
                bottom = VexWeb.YBoundary.FrameBottom,
                content = CompanionPane,
            )

            chrome(
                SeamHandle,
            )
        }

    val Surface =
        VexWebAuthoring.surface(
            VexLifeWorkspaceBlueprint.screenRef,
        ) {
            content(
                WorkspaceFrame,
            )
            overlay(
                OverlayRoot,
            )
        }
}

// [VXG RealForever]
