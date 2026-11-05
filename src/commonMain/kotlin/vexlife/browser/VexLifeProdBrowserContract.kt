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

    const val FramePath =
        "/frame"

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
