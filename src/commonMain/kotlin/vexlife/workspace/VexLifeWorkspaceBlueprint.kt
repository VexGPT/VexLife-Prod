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
    val ReadAloud = QuickActions.control("read-aloud")

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

    val Conversation =
        Companion.region(
            "conversation",
        )
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
    val SettingsReset =
        Settings.region(
            "reset",
        )
    val CleanChat =
        SettingsReset.control(
            "clean-chat",
        )
}

// [VXG RealForever]
