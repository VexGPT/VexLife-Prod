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

        val AttachmentStageCapability = VexWeb.CapabilityRef.of("attachment.stage")
        val LocalSourcesAddCapability = VexWeb.CapabilityRef.of("local-sources.add")

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

    /**
     * Speech: Vex read aloud, ported from the Vextreme Spiral FAB's Read Aloud orb. Presentation over
     * the Conversation timeline; it reads projections and decides nothing. Natural (Kokoro) voices run
     * on this machine after a one-time download; system voices are limited to on-device ones.
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
                    VexWeb.Identity.Blueprint(VexLifeWorkspaceBlueprint.ConversationTimeline)
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
                    ReleaseAlerts,
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

    val SourceTemplateFamily = blueprintSurface(VexLifeWorkspaceBlueprint.SourceTemplateFamily)

    val ArchivesTimeline = blueprintSurface(VexLifeWorkspaceBlueprint.ArchivesTimeline)

    val Archives =
        blueprintSurface(
            element = VexLifeWorkspaceBlueprint.Archives,
            placement = VexWeb.Placement.FillAvailable,
            children = listOf(ArchivesTimeline),
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
                    VexContextProfileUse,
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

    val VexContextProcess =
        blueprintSurface(
            VexLifeWorkspaceBlueprint.VexContextProcess,
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
                    VexContextProfile,
                    VexContextChanges,
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

    val Attach =
        VexWebAuthoring.element(
            element = VexLifeWorkspaceBlueprint.Attach,
            placement = VexWeb.Placement.Leading,
            depth = VexWeb.Depth.Content,
            presentation = VexWeb.Presentation.Surface,
            interactions = setOf(VexWeb.Interaction.Activate),
            binding = VexWebAuthoring.capability(Refs.AttachmentStageCapability),
        )

    val ComposerAttachments = blueprintSurface(VexLifeWorkspaceBlueprint.ComposerAttachments)

    val Stop =
        VexWebAuthoring.element(
            element = VexLifeWorkspaceBlueprint.Stop,
            placement = VexWeb.Placement.Trailing,
            depth = VexWeb.Depth.Content,
            presentation = VexWeb.Presentation.Surface,
            interactions = setOf(VexWeb.Interaction.Activate),
            binding = VexWebAuthoring.capability(Refs.ConversationCancelCapability),
        )

    val ContextInspector =
        blueprintSurface(
            element = VexLifeWorkspaceBlueprint.ContextInspector,
            placement = VexWeb.Placement.Overlay,
            depth = VexWeb.Depth.Modal,
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

    val PrivateBanner = blueprintSurface(VexLifeWorkspaceBlueprint.PrivateBanner)

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

    val LocalSourcesList = blueprintSurface(VexLifeWorkspaceBlueprint.LocalSourcesList)
    val LocalSourcesPath = blueprintSurface(VexLifeWorkspaceBlueprint.LocalSourcesPath)

    val LocalSourcesAdd =
        VexWebAuthoring.element(
            element = VexLifeWorkspaceBlueprint.LocalSourcesAdd,
            placement = VexWeb.Placement.Trailing,
            depth = VexWeb.Depth.Content,
            presentation = VexWeb.Presentation.Surface,
            interactions = setOf(VexWeb.Interaction.Activate),
            binding = VexWebAuthoring.capability(Refs.LocalSourcesAddCapability),
        )

    val SettingsLocalSources =
        blueprintSurface(
            element = VexLifeWorkspaceBlueprint.SettingsLocalSources,
            children = listOf(LocalSourcesList, LocalSourcesPath, LocalSourcesAdd),
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
