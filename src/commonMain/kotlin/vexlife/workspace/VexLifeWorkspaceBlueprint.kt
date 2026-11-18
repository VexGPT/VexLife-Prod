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

    val Companion =
        region(
            "companion",
        )
    val CompanionStatus =
        Companion.content(
            "companion-status",
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
