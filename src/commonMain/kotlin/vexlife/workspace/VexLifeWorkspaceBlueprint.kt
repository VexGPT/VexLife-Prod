package vexlife.workspace

import vexlife.blueprint.VexScreenBlueprint

/**
 * Initial engagement structure only.
 *
 * This declares stable static UI relationships visible in the supplied design. It intentionally
 * does not claim that Conversation, Settings, browser context, Memory, model or provider mechanics
 * are already connected, and it does not assign a Family merely because a UI region exists.
 */
object VexLifeWorkspaceBlueprint :
    VexScreenBlueprint(
        "vexlife-workspace",
    ) {
    override val content =
        layout {
            region(
                "header",
            ) {
                content(
                    "brand",
                )
                textField(
                    "browser-query",
                )
                control(
                    "app-selector",
                )
                region(
                    "utilities",
                )
            }

            region(
                "browser",
            ) {
                region(
                    "viewport",
                )
            }

            region(
                "companion",
            ) {
                region(
                    "conversation",
                ) {
                    collection(
                        "timeline",
                    )
                    content(
                        "response-progress",
                    )
                    textField(
                        "composer",
                    )
                    control(
                        "send",
                    )
                }

                region(
                    "settings",
                ) {
                    region(
                        "context",
                    )
                    region(
                        "resources",
                    )
                    region(
                        "home",
                    )
                    region(
                        "continuity",
                    )
                    region(
                        "reset",
                    )
                }
            }
        }
}

// [VXG RealForever]
