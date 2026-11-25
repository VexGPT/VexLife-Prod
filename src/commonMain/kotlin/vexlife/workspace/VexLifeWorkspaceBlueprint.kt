package vexlife.workspace

import vexlife.blueprint.VexScreenBlueprint

/**
 * Canonical VexLife-Prod nested workspace composition identity.
 *
 * Read this object as a product composition map. Blueprint identity owns stable addressability,
 * while VexWeb owns manifestation relationships and runtime/service owners remain separate.
 */
object VexLifeWorkspaceBlueprint :
    VexScreenBlueprint(
        "vexlife-workspace",
    ) {
    val Header =
        region(
            "header",
        )
    val Brand =
        Header.content(
            "brand",
        )
    val BrowserQuery =
        Header.textField(
            "browser-query",
        )
    val BrowserQueryError =
        BrowserQuery.content(
            "error",
        )
    val BrowserReload =
        Header.control(
            "browser-reload",
        )
    val AppSelector =
        Header.control(
            "app-selector",
        )
    val Utilities =
        Header.region(
            "utilities",
        )
    val QuickActions =
        Utilities.collection(
            "quick-actions",
        )
    val VexSummon =
        QuickActions.control(
            "vex-summon",
        )
    val VisionQuickAccess =
        QuickActions.control(
            "vision",
        )
    val Speech = QuickActions.control("speech")
    val Language = QuickActions.control("language")
    val Theme = QuickActions.control("theme")
    val Support = QuickActions.control("support")
    val QuickActionsTrigger = QuickActions.control("trigger")

    /** The two language choices, side by side: what Vex replies in, and what VexLife reads in. */
    val LanguagePanel = Utilities.region("language-panel")
    val ReplyLanguageChoice = LanguagePanel.control("reply-language")
    val ScreenLanguageChoice = LanguagePanel.control("screen-language")

    /** Product-owned read-aloud presentation and its authored local controls. */
    val SpeechPanel = Utilities.region("speech-panel")
    val SpeechEnabled = SpeechPanel.control("enabled")
    val SpeechVoice = SpeechPanel.control("voice")
    val SpeechReadThoughts = SpeechPanel.control("read-thoughts")
    val SpeechRate = SpeechPanel.control("rate")
    val SpeechPrevious = SpeechPanel.control("previous")
    val SpeechPlayPause = SpeechPanel.control("play-pause")
    val SpeechStop = SpeechPanel.control("stop")
    val SpeechNext = SpeechPanel.control("next")
    val SpeechChooseStart = SpeechPanel.control("choose-start")

    val ReleaseAlerts = QuickActions.control("release-alerts")
    val ReleaseAlertsPanel = Utilities.region("release-alerts-panel")
    val ReleaseAlertsList = ReleaseAlertsPanel.collection("release-alerts-list")

    val Browser =
        region(
            "browser",
        )
    val BrowserFrameStatus =
        Browser.content(
            "frame-status",
        )
    val BrowserViewport =
        Browser.region(
            "viewport",
        )

    /** Source template: Victor's live family pages, one control per member. */
    val SourceTemplateFamily = Browser.collection("source-template-family")

    /** Archives: the person's own records, starting with their dated timeline. */
    val Archives = Browser.region("archives")
    val ArchivesTimeline = Archives.collection("timeline")

    /** What Vex received and the person's chosen relationship for constructing the next turn. */
    val VexContext = Browser.region("vex-context")

    /** The active profile badge lives in the Vex Context header, outside the profile card. */
    val VexContextProfileBadge = VexContext.content("profile-badge")

    /** The Context Profile Family Controls, kept distinct from their authoring parameters. */
    val VexContextProfile = VexContext.region("profile")
    val VexContextProfileSummary = VexContextProfile.content("summary")
    val VexContextProfileName = VexContextProfile.textField("name")
    val VexContextProfileFoundationMode = VexContextProfile.control("foundation-mode")
    val VexContextProfileFoundationText = VexContextProfile.textField("foundation-text")
    val VexContextProfileUse = VexContextProfile.control("use")
    val VexContextProfileSave = VexContextProfile.control("save")
    val VexContextProfileSaveAs = VexContextProfile.control("save-as")
    val VexContextProfileRestore = VexContextProfile.control("restore-default")
    /** Export is portability/readback, not the Family's IMPORT Control. */
    val VexContextProfilePortability = VexContextProfile.control("portability")
    val VexContextProfileImport = VexContextProfile.control("import")
    val VexContextProfileHome = VexContextProfile.control("view-in-home")

    val VexContextChanges = VexContext.collection("changes")

    /**
     * Scratch configuration laboratory. This is a sibling experiment surface, not one of the
     * components that shaped the canonical turn shown under What Vex received.
     */
    val VexContextLabs = VexContext.region("labs")
    val VexContextLabsStatus = VexContextLabs.content("status")
    val VexContextLabsTest = VexContextLabs.control("test")
    val VexContextLabsSaveSupported = VexContextLabs.control("save-supported")
    val VexContextLabsRestore = VexContextLabs.control("restore-defaults")
    val VexContextLabsReload = VexContextLabs.control("reload-current-conversation")
    val VexContextLabsClear = VexContextLabs.control("clear")
    val VexContextLabsInputBudget = VexContextLabs.textField("active-context")
    val VexContextLabsMemoryMode = VexContextLabs.control("memory-mode")
    val VexContextLabsMemoryBudget = VexContextLabs.textField("memory-prompt-depth")
    val VexContextLabsNote = VexContextLabs.content("note")
    val VexContextLabsFields = VexContextLabs.collection("fields")
    val VexContextLabsLastRequest = VexContextLabs.content("last-request")

    /**
     * What Vex received is one presentation group over several independently-owned truths.
     * These children are stable read-only component coordinates; none is promoted to a Control.
     */
    val VexContextInput = VexContext.collection("input")
    val VexContextInputFoundation = VexContextInput.region("foundation")
    val VexContextInputMemory = VexContextInput.region("memory")
    val VexContextInputContinuity = VexContextInput.region("continuity")
    val VexContextInputConversation = VexContextInput.region("conversation")
    val VexContextInputBrowser = VexContextInput.region("browser")
    val VexContextInputRequest = VexContextInput.region("request")
    val VexContextInputComponents = VexContextInput.region("components")

    val VexContextProcess = VexContext.collection("process")
    val VexContextProcessRetention = VexContextProcess.control("retention")
    val VexContextOutput = VexContext.collection("output")
    val VexContextAfter = VexContext.collection("after")

    val Companion =
        region(
            "companion",
        )
    val CompanionStatus =
        Companion.content(
            "companion-status",
        )
    val CompanionMode =
        Companion.control(
            "mode",
        )
    val Vision =
        Companion.control(
            "vision",
        )
    val CompanionViewSwitch =
        Companion.control(
            "view-switch",
        )

    /** Private mode: a conversation that isn't kept. Replaces the earlier "clean chat". */
    val PrivateMode = Companion.control("private-mode")

    val Conversation =
        Companion.region(
            "conversation",
        )
    val PrivateBanner = Conversation.content("private-banner")
    val PrivateNoteKeep = PrivateBanner.control("keep-note")
    val PrivateNoteDecline = PrivateBanner.control("decline-note")
    val PrivateNotePropose = PrivateBanner.control("propose-note")
    val ConversationTimeline =
        Conversation.collection(
            "timeline",
        )
    val ConversationJumpLatest =
        Conversation.control(
            "jump-latest",
        )
    val ResponseProgress =
        Conversation.content(
            "response-progress",
        )
    val Composer =
        Conversation.textField(
            "composer",
        )
    val ContextReceipt =
        Conversation.content(
            "context-receipt",
        )
    val ContextReceiptView =
        ContextReceipt.control(
            "view-text",
        )
    val ContextShare =
        Conversation.control(
            "context-share",
        )
    /**
     * Human voice input is a local drafting affordance. It edits the same text draft and does not
     * create a second Conversation payload type or open Vex's Perception microphone channel.
     */
    val VoiceInput =
        Conversation.control(
            "voice-input",
        )
    /**
     * Local Artifacts staged for this unsent Composer draft.
     *
     * Add Files chooses local material; ComposerAttachments projects the Attachment Draft Family.
     * Neither coordinate owns durable artifact bytes or canonical Conversation resource binding.
     */
    val Attach =
        Conversation.control(
            "attach",
        )
    val ComposerAttachments =
        Conversation.content(
            "composer-attachments",
        )
    val Send =
        Conversation.control(
            "send",
        )
    val Stop =
        Conversation.control(
            "stop",
        )
    val ContextInspector =
        Conversation.content(
            "context-inspector",
        )
    val ContextInspectorClose =
        ContextInspector.control(
            "close",
        )

    val Settings =
        Companion.region(
            "settings",
        )
    val SettingsContext =
        Settings.region(
            "context",
        )
    val ContextWindow =
        SettingsContext.content(
            "context-window",
        )
    val SettingsResources =
        Settings.region(
            "resources",
        )
    val MemoryResource =
        SettingsResources.content(
            "memory",
        )
    val LowMemoryAlert =
        SettingsResources.control(
            "low-memory-alert",
        )
    val ThoughtVisibility =
        Settings.control(
            "thought-visibility",
        )
    val SettingsHome =
        Settings.region(
            "home",
        )
    val HomeDirectories =
        SettingsHome.control(
            "directories",
        )
    val HomeAddDirectory =
        SettingsHome.control(
            "add-directory",
        )

    /** Persistent person-granted folders Vex may read across turns. */
    val SettingsLocalSources =
        Settings.region(
            "local-sources",
        )
    val LocalSourcesList =
        SettingsLocalSources.content(
            "list",
        )
    val LocalSourcesAdd =
        SettingsLocalSources.control(
            "add",
        )
    /** Separate stronger authority for one existing Local Source; never implied by Add Folder. */
    val LocalSourceAllowChanges =
        SettingsLocalSources.control(
            "allow-changes",
        )
    val LocalSourceStopChanges =
        SettingsLocalSources.control(
            "stop-changes",
        )
    /** Process/check authority remains separate from both read and write authority. */
    val LocalSourceAllowChecks =
        SettingsLocalSources.control(
            "allow-checks",
        )
    val LocalSourceStopChecks =
        SettingsLocalSources.control(
            "stop-checks",
        )

    val SettingsContinuity =
        Settings.region(
            "continuity",
        )
    val ContinuityMemory = SettingsContinuity.content("memory")
    val ContinuityMode =
        SettingsContinuity.control(
            "continuity-mode",
        )
    val DreamSync =
        SettingsContinuity.control(
            "dream-sync",
        )
    /** Settings -> Models: current serving state, target selection, runtime configuration and files. */
    val SettingsModels = Settings.region("models")
    val ModelsState = SettingsModels.content("state")
    val ModelChoice = SettingsModels.control("choice")
    val ModelsList = SettingsModels.content("list")
    val ModelConfiguration = SettingsModels.region("configuration")
    val ModelPromptCacheEntries = ModelConfiguration.control("prompt-cache-entries")
    val ModelUseRecommended = ModelConfiguration.control("use-recommended")
    val ModelStart = ModelConfiguration.control("start")

    /** Local microphone speech recognition: installed model, spoken language and currentness. */
    val SettingsLocalAsr = Settings.region("local-asr")
    val LocalAsrSelection = SettingsLocalAsr.control("selection")
    val LocalAsrModelsList = SettingsLocalAsr.content("models-list")
    val LocalAsrSpokenLanguage = SettingsLocalAsr.control("spoken-language")
    val LocalAsrComposerOutput = SettingsLocalAsr.control("composer-output")
    val LocalAsrStatus = SettingsLocalAsr.content("status")
    val LocalAsrStorageNote = SettingsLocalAsr.content("storage-note")

    val SettingsDiagnostics = Settings.region("diagnostics")
    val DiagnosticsTrace = SettingsDiagnostics.content("trace")
    val DiagnosticsEnhancedCapture = SettingsDiagnostics.control("enhanced-capture")
    val DiagnosticsClearHistory = SettingsDiagnostics.control("clear-history")
    val DiagnosticsExport = SettingsDiagnostics.control("export")
}

// [VXG RealForever]
