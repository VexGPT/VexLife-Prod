package vexlife.browser

/**
 * Browser presentation + Vex-context boundaries for the generated VexLife product circuit.
 *
 * Human navigation is ordinary web navigation. The isolated frame host is a separate context-only
 * provider for the currently admitted Vex-owned pages; it must not redefine what the human browser
 * is allowed to display.
 */
object VexLifeProdBrowserContract {
    const val FrameHostPort =
        18_211

    const val FrameHostOrigin =
        "http://127.0.0.1:18211"

    const val FrameHostSuffix =
        ".vex.localhost"

    const val InitialPageUrl =
        "https://vgong24.github.io/Vextreme/pages/terrain-map.html?group=direct_contact&page=direct-contact"

    const val RenderedTextLimit =
        200_000

    const val ContextCharacterLimit =
        12_000

    const val MaxTitleCharacters =
        300

    const val MaxPageUrlCharacters =
        4_096

    const val CaptureRendered =
        "RENDERED"

    val HumanPageSchemes =
        listOf(
            "http:",
            "https:",
        )

    val RejectedHumanPageHosts =
        listOf(
            "localhost",
            "127.0.0.1",
            "0.0.0.0",
            "[::1]",
        )

    const val RejectedHumanPageHostSuffix =
        ".localhost"

    /** Hosts currently admitted for Vex rendered-page context projection. */
    val AdmittedHosts =
        listOf(
            "vgong24.github.io",
            "vexgpt.github.io",
        )

    private val HumanWebPage =
        Regex(
            """https?://(?:\[[0-9A-Fa-f:]+\]|[A-Za-z0-9](?:[A-Za-z0-9.-]*[A-Za-z0-9])?)(?::[0-9]{1,5})?(?:[/?#][^\s\\]*)?""",
            RegexOption.IGNORE_CASE,
        )

    private val ProductLocalPage =
        Regex(
            """https?://(?:localhost|(?:[A-Za-z0-9.-]+\.)localhost|127\.0\.0\.1|0\.0\.0\.0|\[::1\])(?::[0-9]{1,5})?(?:[/?#][^\s\\]*)?""",
            RegexOption.IGNORE_CASE,
        )

    private val AdmittedPage =
        Regex(
            """https://(?:vgong24|vexgpt)\.github\.io(?:[/?#][^\s\\]*)?""",
        )

    fun admitHumanPageUrl(
        value: String,
    ): String {
        require(
            value.isNotEmpty() &&
                value.length <=
                MaxPageUrlCharacters &&
                value ==
                value.trim() &&
                value.none {
                    it.code <
                        0x20
                } &&
                HumanWebPage.matches(
                    value,
                ) &&
                !ProductLocalPage.matches(
                    value,
                ),
        ) {
            "VexLife-Prod human browser page must be one ordinary non-local HTTP(S) URL"
        }

        return value
    }

    fun admitContextPageUrl(
        value: String,
    ): String {
        require(
            value.isNotEmpty() &&
                value.length <=
                MaxPageUrlCharacters &&
                value ==
                value.trim() &&
                value.none {
                    it.code <
                        0x20
                } &&
                AdmittedPage.matches(
                    value,
                ),
        ) {
            "VexLife-Prod browser context page must be one admitted Vex-owned GitHub Pages HTTPS URL"
        }

        return value
    }

    /** Compatibility name retained for the context/frame-host owner. */
    fun admitPageUrl(
        value: String,
    ): String =
        admitContextPageUrl(
            value,
        )

    fun frameUrlFor(
        pageUrl: String,
    ): String {
        val admitted =
            admitContextPageUrl(
                pageUrl,
            )
        val separator =
            admitted.indexOf(
                '/',
                startIndex = "https://".length,
            )
        val authority =
            if (
                separator <
                    0
            ) {
                admitted.removePrefix(
                    "https://",
                )
            } else {
                admitted
                    .substring(
                        "https://".length,
                        separator,
                    )
            }
        val suffix =
            if (
                separator <
                    0
            ) {
                "/"
            } else {
                admitted.substring(
                    separator,
                )
            }

        return "http://" +
            authority +
            FrameHostSuffix +
            ":" +
            FrameHostPort +
            suffix
    }

    fun admittedUpstreamHost(
        frameHost: String,
    ): String {
        val suffix =
            FrameHostSuffix
        require(
            frameHost.endsWith(
                suffix,
            ),
        ) {
            "VexLife-Prod frame host is outside the admitted local frame namespace"
        }
        val upstream =
            frameHost.removeSuffix(
                suffix,
            )
        require(
            upstream in
                AdmittedHosts,
        ) {
            "VexLife-Prod frame host does not map to an admitted upstream"
        }

        return upstream
    }

    fun admitRenderedText(
        value: String,
    ): String {
        require(
            value.isNotBlank() &&
                value.length <=
                ContextCharacterLimit
        ) {
            "VexLife-Prod browser context text must be non-empty and within the admitted bound"
        }

        return value
    }

    fun admitTitle(
        value: String?,
    ): String? {
        if (
            value ==
                null
        ) {
            return null
        }

        require(
            value.length <=
                MaxTitleCharacters &&
                value.none {
                    it.code <
                        0x20 &&
                        it !=
                            '\n' &&
                        it !=
                            '\t'
                },
        ) {
            "VexLife-Prod browser title exceeds the admitted bound"
        }

        return value
    }
}

// [VXG RealForever]
