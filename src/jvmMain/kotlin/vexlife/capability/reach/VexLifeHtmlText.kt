package vexlife.capability.reach

import java.net.URI

/**
 * Readable text from an HTML page, for Vex to read: title, block structure as line breaks, list
 * items, and links kept inline as markdown `[text](url)` so a page can lead to the next fetch.
 *
 * This is a reader, not a browser: no script runs, and pages that build their content in script
 * return little text. That limit is reported by the result's size, not hidden.
 */
object VexLifeHtmlText {
    data class Extracted(val title: String?, val text: String)

    private const val MaximumLinks = 120

    fun extract(html: String, base: URI?): Extracted {
        val title =
            Regex(
                    "<title[^>]*>(.*?)</title>",
                    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
                )
                .find(html)
                ?.groupValues
                ?.get(1)
                ?.let(::inline)
                ?.takeIf { it.isNotBlank() }
        var body = html
        body = body.replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), " ")
        for (tag in listOf("script", "style", "noscript", "svg", "template", "head", "iframe")) {
            body =
                body.replace(
                    Regex(
                        "<$tag\\b[^>]*>.*?</$tag\\s*>",
                        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
                    ),
                    " ",
                )
        }
        var links = 0
        body =
            body.replace(
                Regex(
                    "<a\\b[^>]*?href\\s*=\\s*(\"[^\"]*\"|'[^']*')[^>]*>(.*?)</a\\s*>",
                    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
                )
            ) {
                val label = inline(it.groupValues[2])
                val href = decodeEntities(it.groupValues[1].trim('"', '\''))
                val target = absolute(href, base)
                if (target == null || links >= MaximumLinks || label.isBlank()) {
                    " $label "
                } else {
                    links++
                    " [$label]($target) "
                }
            }
        body = body.replace(Regex("<li\\b[^>]*>", RegexOption.IGNORE_CASE), "\n- ")
        body =
            body.replace(Regex("<h([1-6])\\b[^>]*>", RegexOption.IGNORE_CASE)) {
                "\n\n" + "#".repeat(it.groupValues[1].toInt()) + " "
            }
        body = body.replace(Regex("<(br|hr)\\b[^>]*>", RegexOption.IGNORE_CASE), "\n")
        body =
            body.replace(
                Regex(
                    "</?(p|div|section|article|header|footer|main|nav|aside|tr|table|ul|ol|pre|blockquote|h[1-6]|dl|dt|dd|figure|figcaption)\\b[^>]*>",
                    RegexOption.IGNORE_CASE,
                ),
                "\n",
            )
        body = body.replace(Regex("<(td|th)\\b[^>]*>", RegexOption.IGNORE_CASE), " | ")
        body = body.replace(Regex("<[^>]+>"), " ")
        body = decodeEntities(body)
        val text =
            body
                .lines()
                .map { it.replace(Regex("[ \\t\\u00a0]+"), " ").trim() }
                .joinToString("\n")
                .replace(Regex("\n{3,}"), "\n\n")
                .trim()
        return Extracted(title = title, text = text)
    }

    /** Inline fragment to one line of plain text. */
    fun inline(fragment: String): String =
        decodeEntities(fragment.replace(Regex("<[^>]+>"), " ")).replace(Regex("\\s+"), " ").trim()

    fun decodeEntities(value: String): String =
        Regex("&(#x[0-9a-fA-F]+|#[0-9]+|[a-zA-Z]+);").replace(value) {
            val entity = it.groupValues[1]
            when {
                entity.startsWith("#x") || entity.startsWith("#X") ->
                    entity.drop(2).toIntOrNull(16)?.let(::codePoint) ?: it.value
                entity.startsWith("#") -> entity.drop(1).toIntOrNull()?.let(::codePoint) ?: it.value
                else -> Named[entity] ?: it.value
            }
        }

    private fun codePoint(value: Int): String =
        if (value in 1..0x10FFFF && value !in 0xD800..0xDFFF) String(Character.toChars(value))
        else ""

    private fun absolute(href: String, base: URI?): String? {
        if (
            href.isBlank() ||
                href.startsWith("#") ||
                href.startsWith("javascript:", ignoreCase = true) ||
                href.startsWith("mailto:", ignoreCase = true)
        ) {
            return null
        }
        val resolved =
            runCatching { if (base != null) base.resolve(href.replace(" ", "%20")) else URI(href) }
                .getOrNull() ?: return null
        val scheme = resolved.scheme?.lowercase()
        return resolved.toString().takeIf { scheme == "http" || scheme == "https" }
    }

    private val Named =
        mapOf(
            "amp" to "&",
            "lt" to "<",
            "gt" to ">",
            "quot" to "\"",
            "apos" to "'",
            "nbsp" to " ",
            "ndash" to "–",
            "mdash" to "—",
            "hellip" to "…",
            "lsquo" to "‘",
            "rsquo" to "’",
            "ldquo" to "“",
            "rdquo" to "”",
            "copy" to "©",
            "reg" to "®",
            "trade" to "™",
            "middot" to "·",
            "bull" to "•",
            "times" to "×",
            "rarr" to "→",
            "larr" to "←",
        )
}

// [VXG RealForever]
