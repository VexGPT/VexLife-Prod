package vexlife.web

import vexlife.workspace.VexLifeWorkspaceBlueprint

/**
 * Accepted F0 meaning authored through the F1 compositional surface.
 *
 * The nested source should read in the same direction as the completed-view relationship graph while
 * every emitted node remains the accepted F0 vocabulary.
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

    val Surface =
        vexWebSurface(
            VexLifeWorkspaceBlueprint,
        ) {
            content {
                split(
                    ref =
                        Refs.SplitContainer,
                    mode =
                        VexWeb.Parameter
                            .SplitMode
                            .Balanced(
                                ratio =
                                    VexWeb.Parameter
                                        .SplitRatio
                                        .Equal,
                            ),
                ) {
                    leading {
                        element(
                            element =
                                VexLifeWorkspaceBlueprint.Browser,
                            placement =
                                VexWeb.Placement.Leading,
                            depth =
                                VexWeb.Depth.Content,
                            presentation =
                                VexWeb.Presentation.Surface,
                        ) {
                            element(
                                element =
                                    VexLifeWorkspaceBlueprint.BrowserViewport,
                                placement =
                                    VexWeb.Placement.FillAvailable,
                                depth =
                                    VexWeb.Depth.Content,
                                presentation =
                                    VexWeb.Presentation.Surface,
                            )
                        }
                    }

                    handle {
                        interactiveStructural(
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
                    }

                    trailing {
                        element(
                            element =
                                VexLifeWorkspaceBlueprint.Companion,
                            placement =
                                VexWeb.Placement.Trailing,
                            depth =
                                VexWeb.Depth.Content,
                            presentation =
                                VexWeb.Presentation.Surface,
                        ) {
                            element(
                                element =
                                    VexLifeWorkspaceBlueprint.Conversation,
                                placement =
                                    VexWeb.Placement.FillAvailable,
                                depth =
                                    VexWeb.Depth.Content,
                                presentation =
                                    VexWeb.Presentation.Surface,
                            ) {
                                element(
                                    element =
                                        VexLifeWorkspaceBlueprint.Composer,
                                    placement =
                                        VexWeb.Placement.InFlow,
                                    depth =
                                        VexWeb.Depth.Content,
                                    presentation =
                                        VexWeb.Presentation.Surface,
                                ) {
                                    interactiveElement(
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
                                }
                            }
                        }
                    }
                }
            }

            overlayRoot {
                structural(
                    ref =
                        Refs.OverlayRoot,
                    placement =
                        VexWeb.Placement.Overlay,
                    depth =
                        VexWeb.Depth.Transient,
                    presentation =
                        VexWeb.Presentation.Surface,
                ) {
                    structural(
                        ref =
                            Refs.Snackbar,
                        placement =
                            VexWeb.Placement.Overlay,
                        depth =
                            VexWeb.Depth.Transient,
                        presentation =
                            VexWeb.Presentation.TransientFeedback,
                    )
                }
            }
        }
}

// [VXG RealForever]
