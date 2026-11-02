package vexlife.web

import vexlife.workspace.VexLifeWorkspaceBlueprint

/**
 * First VexWeb pressure specimen from the accepted completed-view blueprint.
 *
 * It is intentionally inert. No browser provider, Conversation backend, model, Memory, Settings
 * backend or DOM-context channel exists here.
 */
object VexLifeWebF0 {
    object Refs {
        val SplitContainer =
            VexWeb.StructuralRef.of(
                "vexweb.vexlife-workspace.split",
            )

        val SplitHandle =
            VexWeb.StructuralRef.of(
                "vexweb.vexlife-workspace.split.handle",
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
                VexWeb.Placement.Trailing,
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
                VexWeb.Placement.Leading,
            depth =
                VexWeb.Depth.Content,
            presentation =
                VexWeb.Presentation.Surface,
            children =
                listOf(
                    BrowserViewport,
                ),
        )

    val SplitHandle =
        VexWeb.Structural(
            ref =
                Refs.SplitHandle,
            placement =
                VexWeb.Placement.InFlow,
            depth =
                VexWeb.Depth.Chrome,
            presentation =
                VexWeb.Presentation.SplitHandle,
            interactions =
                setOf(
                    VexWeb.Interaction.Resize,
                    VexWeb.Interaction.CollapseLeading,
                    VexWeb.Interaction.CollapseTrailing,
                ),
            binding =
                VexWeb.Binding.LocalPresentation(
                    target =
                        Refs.SplitContainer,
                ),
        )

    val SplitContainer =
        VexWeb.Split(
            ref =
                Refs.SplitContainer,
            mode =
                VexWeb.Parameter.SplitMode.Balanced(
                    ratio =
                        VexWeb.Parameter
                            .SplitRatio
                            .Equal,
                ),
            leading =
                BrowserPane,
            handle =
                SplitHandle,
            trailing =
                CompanionPane,
        )

    val Surface =
        VexWeb.Surface(
            screenRef =
                VexLifeWorkspaceBlueprint.screenRef,
            content =
                SplitContainer,
            overlayRoot =
                OverlayRoot,
        )
}

// [VXG RealForever]
