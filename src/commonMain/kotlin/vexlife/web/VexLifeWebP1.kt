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

        val ConversationCancelCapability =
            VexWeb.CapabilityRef.of(
                "conversation.cancel",
            )

        val AttachmentDraftCapability =
            VexWeb.CapabilityRef.of(
                "attachment.draft",
            )

        val LocalSourcesCapability =
            VexWeb.CapabilityRef.of(
                "local-sources.manage",
            )

        val WorkspaceEffectsCapability =
            VexWeb.CapabilityRef.of(
                "workspace-effects.manage",
            )

        val WorkspaceChecksCapability =
            VexWeb.CapabilityRef.of(
                "workspace-checks.manage",
            )

        val WorkspaceRunsCapability =
            VexWeb.CapabilityRef.of(
                "workspace-runs.manage",
            )

        const val ColorModeToggleAction = "color-mode.toggle"
        const val SupportHref = "https://vgong24.github.io/Vextreme/pages/vex-support.html"

        /** Browser-session presentation choices; neither mutates durable Product state. */
        const val ContextProcessRetentionChangeAction = "context-process.retention.change"
        const val CompanionModeChangeAction = "companion.mode.change"

        const val SpeechEnabledToggleAction = "speech.enabled.toggle"
        const val SpeechVoiceChangeAction = "speech.voice.change"
        const val SpeechReadThoughtsChangeAction = "speech.read-thoughts.change"
        const val SpeechRateChangeAction = "speech.rate.change"
        const val SpeechPreviousAction = "speech.previous"
        const val SpeechPlayPauseAction = "speech.play-pause"
        const val SpeechStopAction = "speech.stop"
        const val SpeechNextAction = "speech.next"
        const val SpeechChooseStartAction = "speech.start-here.toggle"

        /** The person's reply language, a Home setting every prompt carries. */
        val ReplyLanguageCapability = VexWeb.CapabilityRef.of("person.reply-language")
        val ScreenLanguageCapability = VexWeb.CapabilityRef.of("person.screen-language")

        /** Context Profile Controls all dispatch through the existing typed local service route. */
        val ContextProfileUseCapability = VexWeb.CapabilityRef.of("context-profile.use")
        val ContextProfileSaveCapability = VexWeb.CapabilityRef.of("context-profile.save")
        val ContextProfileSaveAsCapability = VexWeb.CapabilityRef.of("context-profile.save-as")
        val ContextProfileRestoreCapability = VexWeb.CapabilityRef.of("context-profile.restore-product-default")
        val ContextProfileImportCapability = VexWeb.CapabilityRef.of("context-profile.import")

        /** Private mode: a conversation that isn't kept, apart from notes both agree to keep. */
        val ConversationPrivateCapability = VexWeb.CapabilityRef.of("conversation.private")

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

    val BrowserQueryError =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.BrowserQueryError,
        )

    val BrowserQuery =
        blueprintSurface(
            element = VexLifeWorkspaceBlueprint.BrowserQuery,
            children =
                listOf(
                    BrowserQueryError,
                ),
        )

    val BrowserReload =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.BrowserReload,
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

    /**
     * Speech FAB opens the Product-owned Speech presentation. The read-aloud controls inside retain
     * their current runtime behavior and are addressed separately below.
     */
    val Speech =
        VexWebAuthoring.element(
            element = VexLifeWorkspaceBlueprint.Speech,
            placement = VexWeb.Placement.InFlow,
            depth = VexWeb.Depth.Chrome,
            presentation = VexWeb.Presentation.Surface,
            interactions = setOf(VexWeb.Interaction.Activate),
            binding =
                VexWebAuthoring.localInteraction(
                    VexWeb.Identity.Blueprint(VexLifeWorkspaceBlueprint.SpeechPanel)
                ),
        )

    /** Opens the language panel; the two choices inside it each bind their own Home setting. */
    val Language =
        VexWebAuthoring.element(
            element = VexLifeWorkspaceBlueprint.Language,
            placement = VexWeb.Placement.InFlow,
            depth = VexWeb.Depth.Chrome,
            presentation = VexWeb.Presentation.Surface,
            interactions = setOf(VexWeb.Interaction.Activate),
            binding =
                VexWebAuthoring.localInteraction(
                    VexWeb.Identity.Blueprint(VexLifeWorkspaceBlueprint.LanguagePanel),
                ),
        )

    /** The Spiral's Theme action changes only Product presentation mode. */
    val Theme =
        VexWebAuthoring.element(
            element = VexLifeWorkspaceBlueprint.Theme,
            placement = VexWeb.Placement.InFlow,
            depth = VexWeb.Depth.Chrome,
            presentation = VexWeb.Presentation.Surface,
            interactions = setOf(VexWeb.Interaction.Activate),
            binding = VexWebAuthoring.presentationAction(Refs.ColorModeToggleAction),
        )

    /** The Spiral's Support action opens the Product-authored external support destination. */
    val Support =
        VexWebAuthoring.element(
            element = VexLifeWorkspaceBlueprint.Support,
            placement = VexWeb.Placement.InFlow,
            depth = VexWeb.Depth.Chrome,
            presentation = VexWeb.Presentation.Surface,
            interactions = setOf(VexWeb.Interaction.Activate),
            binding = VexWebAuthoring.externalNavigation(Refs.SupportHref),
        )

    /** The visible Spiral trigger opens/closes the Quick Actions collection itself. */
    val QuickActionsTrigger =
        VexWebAuthoring.element(
            element = VexLifeWorkspaceBlueprint.QuickActionsTrigger,
            placement = VexWeb.Placement.InFlow,
            depth = VexWeb.Depth.Chrome,
            presentation = VexWeb.Presentation.Surface,
            interactions = setOf(VexWeb.Interaction.Activate),
            binding =
                VexWebAuthoring.localInteraction(
                    VexWeb.Identity.Blueprint(VexLifeWorkspaceBlueprint.QuickActions),
                ),
        )

    /** The language Vex replies in (Speech reads in it, dictation hears it), kept in the Home. */
    val ReplyLanguageChoice =
        VexWebAuthoring.element(
            element = VexLifeWorkspaceBlueprint.ReplyLanguageChoice,
            placement = VexWeb.Placement.InFlow,
            depth = VexWeb.Depth.Chrome,
            presentation = VexWeb.Presentation.Surface,
            interactions = setOf(VexWeb.Interaction.Activate),
            binding = VexWebAuthoring.capability(Refs.ReplyLanguageCapability),
        )

    /** The language VexLife's own screens are written in, kept in the Home. */
    val ScreenLanguageChoice =
        VexWebAuthoring.element(
            element = VexLifeWorkspaceBlueprint.ScreenLanguageChoice,
            placement = VexWeb.Placement.InFlow,
            depth = VexWeb.Depth.Chrome,
            presentation = VexWeb.Presentation.Surface,
            interactions = setOf(VexWeb.Interaction.Activate),
            binding = VexWebAuthoring.capability(Refs.ScreenLanguageCapability),
        )

    val LanguagePanel =
        blueprintSurface(
            element = VexLifeWorkspaceBlueprint.LanguagePanel,
            placement = VexWeb.Placement.Overlay,
            depth = VexWeb.Depth.Transient,
            children = listOf(ReplyLanguageChoice, ScreenLanguageChoice),
        )

    private fun speechAction(
        element: vexlife.blueprint.VexUiElementRef,
        actionRef: String,
        interaction: VexWeb.Interaction,
    ): VexWeb.Element =
        VexWebAuthoring.element(
            element = element,
            placement = VexWeb.Placement.InFlow,
            depth = VexWeb.Depth.Transient,
            presentation = VexWeb.Presentation.Surface,
            interactions = setOf(interaction),
            binding = VexWebAuthoring.localAction(actionRef),
        )

    val SpeechEnabled =
        speechAction(
            VexLifeWorkspaceBlueprint.SpeechEnabled,
            Refs.SpeechEnabledToggleAction,
            VexWeb.Interaction.Activate,
        )

    val SpeechVoice =
        speechAction(
            VexLifeWorkspaceBlueprint.SpeechVoice,
            Refs.SpeechVoiceChangeAction,
            VexWeb.Interaction.Change,
        )

    val SpeechReadThoughts =
        speechAction(
            VexLifeWorkspaceBlueprint.SpeechReadThoughts,
            Refs.SpeechReadThoughtsChangeAction,
            VexWeb.Interaction.Change,
        )

    val SpeechRate =
        speechAction(
            VexLifeWorkspaceBlueprint.SpeechRate,
            Refs.SpeechRateChangeAction,
            VexWeb.Interaction.Change,
        )

    val SpeechPrevious =
        speechAction(
            VexLifeWorkspaceBlueprint.SpeechPrevious,
            Refs.SpeechPreviousAction,
            VexWeb.Interaction.Activate,
        )

    val SpeechPlayPause =
        speechAction(
            VexLifeWorkspaceBlueprint.SpeechPlayPause,
            Refs.SpeechPlayPauseAction,
            VexWeb.Interaction.Activate,
        )

    val SpeechStop =
        speechAction(
            VexLifeWorkspaceBlueprint.SpeechStop,
            Refs.SpeechStopAction,
            VexWeb.Interaction.Activate,
        )

    val SpeechNext =
        speechAction(
            VexLifeWorkspaceBlueprint.SpeechNext,
            Refs.SpeechNextAction,
            VexWeb.Interaction.Activate,
        )

    val SpeechChooseStart =
        speechAction(
            VexLifeWorkspaceBlueprint.SpeechChooseStart,
            Refs.SpeechChooseStartAction,
            VexWeb.Interaction.Activate,
        )

    val SpeechPanel =
        blueprintSurface(
            element = VexLifeWorkspaceBlueprint.SpeechPanel,
            placement = VexWeb.Placement.Overlay,
            depth = VexWeb.Depth.Transient,
            children =
                listOf(
                    SpeechEnabled,
                    SpeechVoice,
                    SpeechReadThoughts,
                    SpeechRate,
                    SpeechPrevious,
                    SpeechPlayPause,
                    SpeechStop,
                    SpeechNext,
                    SpeechChooseStart,
                ),
        )

    /** Release communication is Product meaning; this Control only opens its bounded projection. */
    val ReleaseAlerts =
        VexWebAuthoring.element(
            element = VexLifeWorkspaceBlueprint.ReleaseAlerts,
            placement = VexWeb.Placement.InFlow,
            depth = VexWeb.Depth.Chrome,
            presentation = VexWeb.Presentation.Surface,
            interactions = setOf(VexWeb.Interaction.Activate),
            binding =
                VexWebAuthoring.localInteraction(
                    VexWeb.Identity.Blueprint(VexLifeWorkspaceBlueprint.ReleaseAlertsPanel),
                ),
        )

    val ReleaseAlertsList =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.ReleaseAlertsList,
        )

    val ReleaseAlertsPanel =
        blueprintSurface(
            element = VexLifeWorkspaceBlueprint.ReleaseAlertsPanel,
            placement = VexWeb.Placement.Overlay,
            depth = VexWeb.Depth.Transient,
            children = listOf(ReleaseAlertsList),
        )

    val QuickActions =
        blueprintSurface(
            element = VexLifeWorkspaceBlueprint.QuickActions,
            children =
                listOf(
                    VexSummon,
                    VisionQuickAccess,
                    Speech,
                    Language,
                    Theme,
                    ReleaseAlerts,
                    Support,
                    QuickActionsTrigger,
                ),
        )

    val Utilities =
        blueprintSurface(
            element = VexLifeWorkspaceBlueprint.Utilities,
            children =
                listOf(
                    QuickActions,
                    ReleaseAlertsPanel,
                    LanguagePanel,
                    SpeechPanel,
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
                    BrowserReload,
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

    val SourceTemplateFamily = blueprintSurface(VexLifeWorkspaceBlueprint.SourceTemplateFamily)

    val ArchivesTimeline = blueprintSurface(VexLifeWorkspaceBlueprint.ArchivesTimeline)

    val Archives =
        blueprintSurface(
            element = VexLifeWorkspaceBlueprint.Archives,
            placement = VexWeb.Placement.FillAvailable,
            children = listOf(ArchivesTimeline),
        )

    val VexContextProfileBadge =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.VexContextProfileBadge,
        )

    val VexContextProfileSummary =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.VexContextProfileSummary,
        )

    val VexContextProfileName =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.VexContextProfileName,
        )

    val VexContextProfileFoundationMode =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.VexContextProfileFoundationMode,
        )

    val VexContextProfileFoundationText =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.VexContextProfileFoundationText,
        )

    private fun contextProfileControl(
        element: vexlife.blueprint.VexUiElementRef,
        capability: VexWeb.CapabilityRef,
    ) =
        VexWebAuthoring.element(
            element = element,
            placement = VexWeb.Placement.InFlow,
            depth = VexWeb.Depth.Content,
            presentation = VexWeb.Presentation.Surface,
            interactions = setOf(VexWeb.Interaction.Activate),
            binding = VexWebAuthoring.capability(capability),
        )

    val VexContextProfileUse =
        contextProfileControl(
            VexLifeWorkspaceBlueprint.VexContextProfileUse,
            Refs.ContextProfileUseCapability,
        )

    val VexContextProfileSave =
        contextProfileControl(
            VexLifeWorkspaceBlueprint.VexContextProfileSave,
            Refs.ContextProfileSaveCapability,
        )

    val VexContextProfileSaveAs =
        contextProfileControl(
            VexLifeWorkspaceBlueprint.VexContextProfileSaveAs,
            Refs.ContextProfileSaveAsCapability,
        )

    val VexContextProfileRestore =
        contextProfileControl(
            VexLifeWorkspaceBlueprint.VexContextProfileRestore,
            Refs.ContextProfileRestoreCapability,
        )

    val VexContextProfilePortability =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.VexContextProfilePortability,
        )

    val VexContextProfileImport =
        contextProfileControl(
            VexLifeWorkspaceBlueprint.VexContextProfileImport,
            Refs.ContextProfileImportCapability,
        )

    val VexContextProfileHome =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.VexContextProfileHome,
        )

    val VexContextProfile =
        blueprintSurface(
            element = VexLifeWorkspaceBlueprint.VexContextProfile,
            children =
                listOf(
                    VexContextProfileSummary,
                    VexContextProfileUse,
                    VexContextProfileName,
                    VexContextProfileFoundationMode,
                    VexContextProfileFoundationText,
                    VexContextProfileSave,
                    VexContextProfileSaveAs,
                    VexContextProfileRestore,
                    VexContextProfilePortability,
                    VexContextProfileImport,
                    VexContextProfileHome,
                ),
        )

    val VexContextChanges =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.VexContextChanges,
        )

    val VexContextLabsStatus =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.VexContextLabsStatus,
        )

    val VexContextLabsTest =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.VexContextLabsTest,
        )

    val VexContextLabsSaveSupported =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.VexContextLabsSaveSupported,
        )

    val VexContextLabsRestore =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.VexContextLabsRestore,
        )

    val VexContextLabsReload =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.VexContextLabsReload,
        )

    val VexContextLabsClear =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.VexContextLabsClear,
        )

    val VexContextLabsInputBudget =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.VexContextLabsInputBudget,
        )

    val VexContextLabsMemoryMode =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.VexContextLabsMemoryMode,
        )

    val VexContextLabsMemoryBudget =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.VexContextLabsMemoryBudget,
        )

    val VexContextLabsNote =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.VexContextLabsNote,
        )

    val VexContextLabsFields =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.VexContextLabsFields,
        )

    val VexContextLabsLastRequest =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.VexContextLabsLastRequest,
        )

    val VexContextLabs =
        blueprintSurface(
            element = VexLifeWorkspaceBlueprint.VexContextLabs,
            children =
                listOf(
                    VexContextLabsStatus,
                    VexContextLabsTest,
                    VexContextLabsSaveSupported,
                    VexContextLabsRestore,
                    VexContextLabsReload,
                    VexContextLabsClear,
                    VexContextLabsInputBudget,
                    VexContextLabsMemoryMode,
                    VexContextLabsMemoryBudget,
                    VexContextLabsNote,
                    VexContextLabsFields,
                    VexContextLabsLastRequest,
                ),
        )

    val VexContextInputFoundation =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.VexContextInputFoundation,
        )
    val VexContextInputMemory =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.VexContextInputMemory,
        )
    val VexContextInputContinuity =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.VexContextInputContinuity,
        )
    val VexContextInputConversation =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.VexContextInputConversation,
        )
    val VexContextInputBrowser =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.VexContextInputBrowser,
        )
    val VexContextInputRequest =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.VexContextInputRequest,
        )
    val VexContextInputComponents =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.VexContextInputComponents,
        )

    val VexContextInput =
        blueprintSurface(
            element = VexLifeWorkspaceBlueprint.VexContextInput,
            children =
                listOf(
                    VexContextInputFoundation,
                    VexContextInputMemory,
                    VexContextInputContinuity,
                    VexContextInputConversation,
                    VexContextInputBrowser,
                    VexContextInputRequest,
                    VexContextInputComponents,
                ),
        )

    val VexContextProcessRetention =
        VexWebAuthoring.element(
            element = VexLifeWorkspaceBlueprint.VexContextProcessRetention,
            placement = VexWeb.Placement.InFlow,
            depth = VexWeb.Depth.Content,
            presentation = VexWeb.Presentation.Surface,
            interactions =
                setOf(
                    VexWeb.Interaction.Change,
                ),
            binding =
                VexWebAuthoring.presentationAction(
                    Refs.ContextProcessRetentionChangeAction,
                ),
        )

    val VexContextProcess =
        blueprintSurface(
            element = VexLifeWorkspaceBlueprint.VexContextProcess,
            children =
                listOf(
                    VexContextProcessRetention,
                ),
        )

    val VexContextOutput =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.VexContextOutput,
        )

    val VexContextAfter =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.VexContextAfter,
        )

    val VexContext =
        blueprintSurface(
            element = VexLifeWorkspaceBlueprint.VexContext,
            placement = VexWeb.Placement.FillAvailable,
            children =
                listOf(
                    VexContextProfileBadge,
                    VexContextProfile,
                    VexContextChanges,
                    VexContextLabs,
                    VexContextInput,
                    VexContextProcess,
                    VexContextOutput,
                    VexContextAfter,
                ),
        )

    val Browser =
        blueprintSurface(
            element = VexLifeWorkspaceBlueprint.Browser,
            placement = VexWeb.Placement.FillAvailable,
            children =
                listOf(
                    BrowserFrameStatus,
                    SourceTemplateFamily,
                    BrowserViewport,
                    Archives,
                    VexContext,
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

    val ContextReceiptView =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.ContextReceiptView,
        )

    val ContextReceipt =
        blueprintSurface(
            element = VexLifeWorkspaceBlueprint.ContextReceipt,
            children =
                listOf(
                    ContextReceiptView,
                ),
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
        VexWebAuthoring.element(
            element = VexLifeWorkspaceBlueprint.Stop,
            placement = VexWeb.Placement.Trailing,
            depth = VexWeb.Depth.Content,
            presentation = VexWeb.Presentation.Surface,
            interactions = setOf(VexWeb.Interaction.Activate),
            binding = VexWebAuthoring.capability(Refs.ConversationCancelCapability),
        )

    val ContextInspectorClose =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.ContextInspectorClose,
        )

    val ContextInspector =
        blueprintSurface(
            element = VexLifeWorkspaceBlueprint.ContextInspector,
            placement = VexWeb.Placement.Overlay,
            depth = VexWeb.Depth.Modal,
            children =
                listOf(
                    ContextInspectorClose,
                ),
        )

    /**
     * VoiceInput drafts into the existing Composer. It owns no Send and no Vex perception channel.
     */
    val VoiceInput =
        VexWebAuthoring.element(
            element = VexLifeWorkspaceBlueprint.VoiceInput,
            placement = VexWeb.Placement.Trailing,
            depth = VexWeb.Depth.Content,
            presentation = VexWeb.Presentation.Surface,
            interactions =
                setOf(
                    VexWeb.Interaction.Activate,
                ),
            binding =
                VexWebAuthoring.localInteraction(
                    VexWeb.Identity.Blueprint(
                        VexLifeWorkspaceBlueprint.Composer,
                    ),
                ),
        )

    val Attach =
        VexWebAuthoring.element(
            element = VexLifeWorkspaceBlueprint.Attach,
            placement = VexWeb.Placement.Leading,
            depth = VexWeb.Depth.Content,
            presentation = VexWeb.Presentation.Surface,
            interactions =
                setOf(
                    VexWeb.Interaction.Activate,
                ),
            binding =
                VexWebAuthoring.capability(
                    Refs.AttachmentDraftCapability,
                ),
        )

    val ComposerAttachments =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.ComposerAttachments,
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
                    ComposerAttachments,
                    Attach,
                    VoiceInput,
                    Send,
                    Stop,
                ),
        )

    val PrivateNoteKeep =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.PrivateNoteKeep,
        )

    val PrivateNoteDecline =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.PrivateNoteDecline,
        )

    val PrivateNotePropose =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.PrivateNotePropose,
        )

    val PrivateBanner =
        blueprintSurface(
            element = VexLifeWorkspaceBlueprint.PrivateBanner,
            children =
                listOf(
                    PrivateNoteKeep,
                    PrivateNoteDecline,
                    PrivateNotePropose,
                ),
        )

    val Conversation =
        blueprintSurface(
            element = VexLifeWorkspaceBlueprint.Conversation,
            children =
                listOf(
                    PrivateBanner,
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

    val LocalSourcesList =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.LocalSourcesList,
        )

    val LocalSourcesAdd =
        VexWebAuthoring.element(
            element = VexLifeWorkspaceBlueprint.LocalSourcesAdd,
            placement = VexWeb.Placement.Trailing,
            depth = VexWeb.Depth.Content,
            presentation = VexWeb.Presentation.Surface,
            interactions =
                setOf(
                    VexWeb.Interaction.Activate,
                ),
            binding =
                VexWebAuthoring.capability(
                    Refs.LocalSourcesCapability,
                ),
        )

    val LocalSourceAllowChanges =
        VexWebAuthoring.element(
            element = VexLifeWorkspaceBlueprint.LocalSourceAllowChanges,
            placement = VexWeb.Placement.Trailing,
            depth = VexWeb.Depth.Content,
            presentation = VexWeb.Presentation.Surface,
            interactions =
                setOf(
                    VexWeb.Interaction.Activate,
                ),
            binding =
                VexWebAuthoring.capability(
                    Refs.WorkspaceEffectsCapability,
                ),
        )

    val LocalSourceStopChanges =
        VexWebAuthoring.element(
            element = VexLifeWorkspaceBlueprint.LocalSourceStopChanges,
            placement = VexWeb.Placement.Trailing,
            depth = VexWeb.Depth.Content,
            presentation = VexWeb.Presentation.Surface,
            interactions =
                setOf(
                    VexWeb.Interaction.Activate,
                ),
            binding =
                VexWebAuthoring.capability(
                    Refs.WorkspaceEffectsCapability,
                ),
        )


    val LocalSourceAllowChecks =
        VexWebAuthoring.element(
            element = VexLifeWorkspaceBlueprint.LocalSourceAllowChecks,
            placement = VexWeb.Placement.Trailing,
            depth = VexWeb.Depth.Content,
            presentation = VexWeb.Presentation.Surface,
            interactions =
                setOf(
                    VexWeb.Interaction.Activate,
                ),
            binding =
                VexWebAuthoring.capability(
                    Refs.WorkspaceChecksCapability,
                ),
        )

    val LocalSourceStopChecks =
        VexWebAuthoring.element(
            element = VexLifeWorkspaceBlueprint.LocalSourceStopChecks,
            placement = VexWeb.Placement.Trailing,
            depth = VexWeb.Depth.Content,
            presentation = VexWeb.Presentation.Surface,
            interactions =
                setOf(
                    VexWeb.Interaction.Activate,
                ),
            binding =
                VexWebAuthoring.capability(
                    Refs.WorkspaceChecksCapability,
                ),
        )

    val LocalSourceAllowTests =
        VexWebAuthoring.element(
            element = VexLifeWorkspaceBlueprint.LocalSourceAllowTests,
            placement = VexWeb.Placement.Trailing,
            depth = VexWeb.Depth.Content,
            presentation = VexWeb.Presentation.Surface,
            interactions =
                setOf(
                    VexWeb.Interaction.Activate,
                ),
            binding =
                VexWebAuthoring.capability(
                    Refs.WorkspaceRunsCapability,
                ),
        )

    val LocalSourceStopTests =
        VexWebAuthoring.element(
            element = VexLifeWorkspaceBlueprint.LocalSourceStopTests,
            placement = VexWeb.Placement.Trailing,
            depth = VexWeb.Depth.Content,
            presentation = VexWeb.Presentation.Surface,
            interactions =
                setOf(
                    VexWeb.Interaction.Activate,
                ),
            binding =
                VexWebAuthoring.capability(
                    Refs.WorkspaceRunsCapability,
                ),
        )

    val SettingsLocalSources =
        blueprintSurface(
            element = VexLifeWorkspaceBlueprint.SettingsLocalSources,
            children =
                listOf(
                    LocalSourcesList,
                    LocalSourcesAdd,
                    LocalSourceAllowChanges,
                    LocalSourceStopChanges,
                    LocalSourceAllowChecks,
                    LocalSourceStopChecks,
                    LocalSourceAllowTests,
                    LocalSourceStopTests,
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

    val ModelsState = blueprintSurface(VexLifeWorkspaceBlueprint.ModelsState)
    val ModelChoice = blueprintSurface(VexLifeWorkspaceBlueprint.ModelChoice)
    val ModelsList = blueprintSurface(VexLifeWorkspaceBlueprint.ModelsList)
    val ModelPromptCacheEntries = blueprintSurface(VexLifeWorkspaceBlueprint.ModelPromptCacheEntries)
    val ModelUseRecommended = blueprintSurface(VexLifeWorkspaceBlueprint.ModelUseRecommended)
    val ModelStart = blueprintSurface(VexLifeWorkspaceBlueprint.ModelStart)
    val ModelConfiguration =
        blueprintSurface(
            element = VexLifeWorkspaceBlueprint.ModelConfiguration,
            children =
                listOf(
                    ModelPromptCacheEntries,
                    ModelUseRecommended,
                    ModelStart,
                ),
        )

    val SettingsModels =
        blueprintSurface(
            element = VexLifeWorkspaceBlueprint.SettingsModels,
            children =
                listOf(
                    ModelsState,
                    ModelChoice,
                    ModelsList,
                    ModelConfiguration,
                ),
        )

    val LocalAsrSelection =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.LocalAsrSelection,
        )

    val LocalAsrModelsList =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.LocalAsrModelsList,
        )

    val LocalAsrSpokenLanguage =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.LocalAsrSpokenLanguage,
        )

    val LocalAsrComposerOutput =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.LocalAsrComposerOutput,
        )

    val LocalAsrStatus =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.LocalAsrStatus,
        )

    val LocalAsrStorageNote =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.LocalAsrStorageNote,
        )

    val SettingsLocalAsr =
        blueprintSurface(
            element = VexLifeWorkspaceBlueprint.SettingsLocalAsr,
            children =
                listOf(
                    LocalAsrSelection,
                    LocalAsrModelsList,
                    LocalAsrSpokenLanguage,
                    LocalAsrComposerOutput,
                    LocalAsrStatus,
                    LocalAsrStorageNote,
                ),
        )

    val DiagnosticsTrace = blueprintSurface(VexLifeWorkspaceBlueprint.DiagnosticsTrace)
    val DiagnosticsEnhancedCapture = blueprintSurface(VexLifeWorkspaceBlueprint.DiagnosticsEnhancedCapture)
    val DiagnosticsClearHistory = blueprintSurface(VexLifeWorkspaceBlueprint.DiagnosticsClearHistory)
    val DiagnosticsExport = blueprintSurface(VexLifeWorkspaceBlueprint.DiagnosticsExport)

    val SettingsDiagnostics =
        blueprintSurface(
            element = VexLifeWorkspaceBlueprint.SettingsDiagnostics,
            children =
                listOf(
                    DiagnosticsTrace,
                    DiagnosticsEnhancedCapture,
                    DiagnosticsClearHistory,
                    DiagnosticsExport,
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
                    SettingsLocalSources,
                    SettingsContinuity,
                    SettingsModels,
                    SettingsLocalAsr,
                    SettingsDiagnostics,
                ),
        )

    val CompanionStatus =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.CompanionStatus,
        )

    val CompanionMode =
        VexWebAuthoring.element(
            element = VexLifeWorkspaceBlueprint.CompanionMode,
            placement = VexWeb.Placement.InFlow,
            depth = VexWeb.Depth.Chrome,
            presentation = VexWeb.Presentation.Surface,
            interactions =
                setOf(
                    VexWeb.Interaction.Change,
                ),
            binding =
                VexWebAuthoring.presentationAction(
                    Refs.CompanionModeChangeAction,
                ),
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

    /** Go private / leave private mode, resolved by VexLifePrivateModeFamily in the service. */
    val PrivateMode =
        VexWebAuthoring.element(
            element = VexLifeWorkspaceBlueprint.PrivateMode,
            placement = VexWeb.Placement.InFlow,
            depth = VexWeb.Depth.Chrome,
            presentation = VexWeb.Presentation.Surface,
            interactions = setOf(VexWeb.Interaction.Activate),
            binding = VexWebAuthoring.capability(Refs.ConversationPrivateCapability),
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
                    CompanionMode,
                    PrivateMode,
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
