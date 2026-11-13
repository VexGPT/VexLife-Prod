package vexlife.web

import vexlife.workspace.VexLifeWorkspaceBlueprint

/**
 * First generated-product VexWeb surface for the intended VexLife workspace.
 *
 * This source manifests the existing Blueprint identity through accepted VexWeb relationships. It
 * does not own browser/provider behavior, Conversation runtime behavior, model state, or context
 * admission.
 */
object VexLifeWebP1 {
    object Refs {
        val WorkspaceFrame =
            VexWeb.StructuralRef.of(
                "vexweb.vexlife-workspace.p1.frame",
            )

        val HeaderBodySeam =
            VexWeb.SeamRef.Horizontal.of(
                "vexweb.vexlife-workspace.p1.seam.header-body",
            )

        val HeaderRegion =
            VexWeb.StructuralRef.of(
                "vexweb.vexlife-workspace.p1.region.header",
            )

        val BodyRegion =
            VexWeb.StructuralRef.of(
                "vexweb.vexlife-workspace.p1.region.body",
            )

        val BodyFrame =
            VexWeb.StructuralRef.of(
                "vexweb.vexlife-workspace.p1.body.frame",
            )

        val BrowserCompanionSeam =
            VexWeb.SeamRef.Vertical.of(
                "vexweb.vexlife-workspace.p1.body.seam.browser-companion",
            )

        val BrowserRegion =
            VexWeb.StructuralRef.of(
                "vexweb.vexlife-workspace.p1.body.region.browser",
            )

        val CompanionRegion =
            VexWeb.StructuralRef.of(
                "vexweb.vexlife-workspace.p1.body.region.companion",
            )

        val BrowserCompanionHandle =
            VexWeb.StructuralRef.of(
                "vexweb.vexlife-workspace.p1.body.seam.browser-companion.handle",
            )

        val CompanionHeader =
            VexWeb.StructuralRef.of(
                "vexweb.vexlife-workspace.p1.body.companion.header",
            )

        val CompanionContent =
            VexWeb.StructuralRef.of(
                "vexweb.vexlife-workspace.p1.body.companion.content",
            )

        val OverlayRoot =
            VexWeb.StructuralRef.of(
                "vexweb.vexlife-workspace.p1.overlay",
            )

        val Snackbar =
            VexWeb.StructuralRef.of(
                "vexweb.vexlife-workspace.p1.overlay.snackbar",
            )

        val ConversationSendCapability =
            VexWeb.CapabilityRef.of(
                "conversation.send",
            )

        /** Persistent selected scope: choose one window (Engage) / release it (Stop). No capture. */
        val VisionSelectedScopeCapability =
            VexWeb.CapabilityRef.of(
                "vision.selected-scope",
            )
    }

    private fun blueprintSurface(
        element: vexlife.blueprint.VexUiElementRef,
        placement: VexWeb.Placement =
            VexWeb.Placement.InFlow,
        depth: VexWeb.Depth =
            VexWeb.Depth.Content,
        presentation: VexWeb.Presentation =
            VexWeb.Presentation.Surface,
        children: List<VexWeb.Node> =
            emptyList(),
    ): VexWeb.Element =
        VexWebAuthoring.element(
            element = element,
            placement = placement,
            depth = depth,
            presentation = presentation,
            children = children,
        )

    val Brand =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.Brand,
        )

    val BrowserQuery =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.BrowserQuery,
        )

    val AppSelector =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.AppSelector,
        )

    val VexSummon =
        VexWebAuthoring.element(
            element = VexLifeWorkspaceBlueprint.VexSummon,
            placement = VexWeb.Placement.InFlow,
            depth = VexWeb.Depth.Chrome,
            presentation = VexWeb.Presentation.Surface,
            interactions =
                setOf(
                    VexWeb.Interaction.Activate,
                ),
            binding =
                VexWebAuthoring.localInteraction(
                    VexWeb.Identity.Blueprint(
                        VexLifeWorkspaceBlueprint.Companion,
                    ),
                ),
        )

    val VisionQuickAccess =
        VexWebAuthoring.element(
            element = VexLifeWorkspaceBlueprint.VisionQuickAccess,
            placement = VexWeb.Placement.InFlow,
            depth = VexWeb.Depth.Chrome,
            presentation = VexWeb.Presentation.Surface,
            interactions =
                setOf(
                    VexWeb.Interaction.Activate,
                ),
            binding =
                VexWebAuthoring.localInteraction(
                    VexWeb.Identity.Blueprint(
                        VexLifeWorkspaceBlueprint.Vision,
                    ),
                ),
        )

    val QuickActions =
        blueprintSurface(
            element = VexLifeWorkspaceBlueprint.QuickActions,
            children =
                listOf(
                    VexSummon,
                    VisionQuickAccess,
                ),
        )

    val Utilities =
        blueprintSurface(
            element = VexLifeWorkspaceBlueprint.Utilities,
            children =
                listOf(
                    QuickActions,
                ),
        )

    val Header =
        blueprintSurface(
            element = VexLifeWorkspaceBlueprint.Header,
            placement = VexWeb.Placement.FillAvailable,
            depth = VexWeb.Depth.Chrome,
            children =
                listOf(
                    Brand,
                    BrowserQuery,
                    AppSelector,
                    Utilities,
                ),
        )

    val BrowserFrameStatus =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.BrowserFrameStatus,
        )

    val BrowserViewport =
        blueprintSurface(
            element = VexLifeWorkspaceBlueprint.BrowserViewport,
            placement = VexWeb.Placement.FillAvailable,
        )

    val Browser =
        blueprintSurface(
            element = VexLifeWorkspaceBlueprint.Browser,
            placement = VexWeb.Placement.FillAvailable,
            children =
                listOf(
                    BrowserFrameStatus,
                    BrowserViewport,
                ),
        )

    val ConversationTimeline =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.ConversationTimeline,
        )

    val ConversationJumpLatest =
        VexWebAuthoring.element(
            element = VexLifeWorkspaceBlueprint.ConversationJumpLatest,
            placement = VexWeb.Placement.InFlow,
            depth = VexWeb.Depth.Chrome,
            presentation = VexWeb.Presentation.Surface,
            interactions =
                setOf(
                    VexWeb.Interaction.Activate,
                ),
            binding =
                VexWebAuthoring.localInteraction(
                    ConversationTimeline.identity,
                ),
        )

    val ResponseProgress =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.ResponseProgress,
        )

    val ContextReceipt =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.ContextReceipt,
        )

    val ContextShare =
        VexWebAuthoring.element(
            element = VexLifeWorkspaceBlueprint.ContextShare,
            placement = VexWeb.Placement.InFlow,
            depth = VexWeb.Depth.Content,
            presentation = VexWeb.Presentation.Surface,
            interactions =
                setOf(
                    VexWeb.Interaction.Activate,
                ),
            binding =
                VexWebAuthoring.localInteraction(
                    ContextReceipt.identity,
                ),
        )

    val Stop =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.Stop,
        )

    val ContextInspector =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.ContextInspector,
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
                VexWebAuthoring.capability(
                    Refs.ConversationSendCapability,
                ),
        )

    val Composer =
        blueprintSurface(
            element = VexLifeWorkspaceBlueprint.Composer,
            children =
                listOf(
                    ContextReceipt,
                    ContextShare,
                    Send,
                    Stop,
                ),
        )

    val Conversation =
        blueprintSurface(
            element = VexLifeWorkspaceBlueprint.Conversation,
            children =
                listOf(
                    ConversationTimeline,
                    ConversationJumpLatest,
                    ResponseProgress,
                    Composer,
                    ContextInspector,
                ),
        )

    val ContextWindow =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.ContextWindow,
        )

    val SettingsContext =
        blueprintSurface(
            element = VexLifeWorkspaceBlueprint.SettingsContext,
            children =
                listOf(
                    ContextWindow,
                ),
        )

    val MemoryResource =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.MemoryResource,
        )

    val LowMemoryAlert =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.LowMemoryAlert,
        )

    val SettingsResources =
        blueprintSurface(
            element = VexLifeWorkspaceBlueprint.SettingsResources,
            children =
                listOf(
                    MemoryResource,
                    LowMemoryAlert,
                ),
        )

    val ThoughtVisibility =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.ThoughtVisibility,
        )

    val HomeDirectories =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.HomeDirectories,
        )

    val HomeAddDirectory =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.HomeAddDirectory,
        )

    val SettingsHome =
        blueprintSurface(
            element = VexLifeWorkspaceBlueprint.SettingsHome,
            children =
                listOf(
                    HomeDirectories,
                    HomeAddDirectory,
                ),
        )

    val ContinuityMemory = blueprintSurface(VexLifeWorkspaceBlueprint.ContinuityMemory)

    val ContinuityMode =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.ContinuityMode,
        )

    val DreamSync =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.DreamSync,
        )

    val SettingsContinuity =
        blueprintSurface(
            element = VexLifeWorkspaceBlueprint.SettingsContinuity,
            children =
                listOf(
                    ContinuityMemory,
                    ContinuityMode,
                    DreamSync,
                ),
        )

    val CleanChat =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.CleanChat,
        )

    val SettingsReset =
        blueprintSurface(
            element = VexLifeWorkspaceBlueprint.SettingsReset,
            children =
                listOf(
                    CleanChat,
                ),
        )

    val Settings =
        blueprintSurface(
            element = VexLifeWorkspaceBlueprint.Settings,
            children =
                listOf(
                    SettingsContext,
                    SettingsResources,
                    ThoughtVisibility,
                    SettingsHome,
                    SettingsContinuity,
                    SettingsReset,
                ),
        )

    val CompanionStatus =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.CompanionStatus,
        )

    val Vision =
        VexWebAuthoring.element(
            element = VexLifeWorkspaceBlueprint.Vision,
            placement = VexWeb.Placement.InFlow,
            depth = VexWeb.Depth.Chrome,
            presentation = VexWeb.Presentation.Surface,
            interactions =
                setOf(
                    VexWeb.Interaction.Activate,
                ),
            binding =
                VexWebAuthoring.capability(
                    Refs.VisionSelectedScopeCapability,
                ),
        )

    val CompanionViewSwitch =
        VexWebAuthoring.element(
            element = VexLifeWorkspaceBlueprint.CompanionViewSwitch,
            placement = VexWeb.Placement.Trailing,
            depth = VexWeb.Depth.Chrome,
            presentation = VexWeb.Presentation.Surface,
            interactions =
                setOf(
                    VexWeb.Interaction.Activate,
                ),
            binding =
                VexWebAuthoring.localInteraction(
                    Settings.identity,
                ),
        )

    val CompanionHeader =
        VexWebAuthoring.structural(
            ref = Refs.CompanionHeader,
            placement = VexWeb.Placement.InFlow,
            depth = VexWeb.Depth.Chrome,
            presentation = VexWeb.Presentation.Surface,
            children =
                listOf(
                    CompanionStatus,
                    Vision,
                    CompanionViewSwitch,
                ),
        )

    val CompanionContent =
        VexWebAuthoring.structural(
            ref = Refs.CompanionContent,
            placement = VexWeb.Placement.FillAvailable,
            depth = VexWeb.Depth.Content,
            presentation = VexWeb.Presentation.Surface,
            children =
                listOf(
                    Conversation,
                    Settings,
                ),
        )

    val Companion =
        blueprintSurface(
            element = VexLifeWorkspaceBlueprint.Companion,
            placement = VexWeb.Placement.FillAvailable,
            children =
                listOf(
                    CompanionHeader,
                    CompanionContent,
                ),
        )

    val BrowserCompanionHandle =
        VexWebAuthoring.structural(
            ref = Refs.BrowserCompanionHandle,
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

    val BodyFrame =
        VexWebAuthoring.frame(
            Refs.BodyFrame,
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
                content = Browser,
                depth = VexWeb.Depth.Content,
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
                content = Companion,
                depth = VexWeb.Depth.Content,
            )

            chrome(
                BrowserCompanionHandle,
            )
        }

    val WorkspaceFrame =
        VexWebAuthoring.frame(
            Refs.WorkspaceFrame,
        ) {
            horizontalSeam(
                ref = Refs.HeaderBodySeam,
                betweenTop = VexWeb.YBoundary.FrameTop,
                betweenBottom = VexWeb.YBoundary.FrameBottom,
                allocation =
                    VexWeb.Parameter.Allocation.Ratio.of(
                        beforeParts = 1,
                        afterParts = 11,
                    ),
                spanStart = VexWeb.XBoundary.FrameStart,
                spanEnd = VexWeb.XBoundary.FrameEnd,
            )

            region(
                ref = Refs.HeaderRegion,
                start = VexWeb.XBoundary.FrameStart,
                end = VexWeb.XBoundary.FrameEnd,
                top = VexWeb.YBoundary.FrameTop,
                bottom =
                    VexWeb.YBoundary.Seam(
                        Refs.HeaderBodySeam,
                    ),
                content = Header,
            )

            region(
                ref = Refs.BodyRegion,
                start = VexWeb.XBoundary.FrameStart,
                end = VexWeb.XBoundary.FrameEnd,
                top =
                    VexWeb.YBoundary.Seam(
                        Refs.HeaderBodySeam,
                    ),
                bottom = VexWeb.YBoundary.FrameBottom,
                content = BodyFrame,
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
