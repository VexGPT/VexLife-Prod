package vexlife.workspace

import vexlife.blueprint.VexScreenBlueprint

/**
 * Initial engagement structure only.
 *
 * Directly consumed elements are compiler-visible properties. This lets renderers inherit stable
 * identity from the Blueprint rather than retyping string paths.
 *
 * The structure intentionally does not claim that Conversation, Settings, browser context, Memory,
 * model or provider mechanics are already connected, and it does not assign a Family merely because
 * a UI region exists.
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

    val Browser =
        region(
            "browser",
        )
    val BrowserViewport =
        Browser.region(
            "viewport",
        )

    val Companion =
        region(
            "companion",
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
    val Send =
        Conversation.control(
            "send",
        )

    val Settings =
        Companion.region(
            "settings",
        )
    val SettingsContext =
        Settings.region(
            "context",
        )
    val SettingsResources =
        Settings.region(
            "resources",
        )
    val SettingsHome =
        Settings.region(
            "home",
        )
    val SettingsContinuity =
        Settings.region(
            "continuity",
        )
    val SettingsReset =
        Settings.region(
            "reset",
        )
}

// [VXG RealForever]
