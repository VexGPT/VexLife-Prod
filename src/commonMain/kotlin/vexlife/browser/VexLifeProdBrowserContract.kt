package vexlife.browser

/**
 * Bounded browser contract for the first generated VexLife product circuit.
 *
 * The initial product pressure is Vex-owned GitHub Pages. General public-web proxying is not earned
 * by this source. The frame host and prompt-context owner both consume this exact admission boundary.
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

    private val AdmittedPage =
        Regex(
            """https://(?:vgong24|vexgpt)\.github\.io(?:[/?#][^\s\\]*)?""",
        )

    fun admitPageUrl(
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
            "VexLife-Prod browser page must be one admitted Vex-owned GitHub Pages HTTPS URL"
        }

        return value
    }

    fun frameUrlFor(
        pageUrl: String,
    ): String {
        val admitted =
            admitPageUrl(
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
            upstream ==
                "vgong24.github.io" ||
                upstream ==
                    "vexgpt.github.io",
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
