package vexlife.web

import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The design foundation's laws (docs/foundation/DESIGN.md, aligned with the Vextreme Design
 * Foundation v2), held against the workspace style itself: tokens resolve, colors and shapes go
 * through tokens, both themes are authored, text is readable, and Vex's inside is drawn with the
 * relationship legend.
 */
class VexLifeWebP1DesignTokenTest {
    private val css = VexLifeWebP1ExperienceStyle.render()

    private val tokenBlocks =
        Regex(""":root(?:\[data-theme="dark"]|:not\(\[data-theme="light"]\))?\s*\{[^}]*}""")
            .findAll(css)
            .toList()

    private fun declarations(block: String): Map<String, String> =
        Regex("""(--[\w-]+)\s*:\s*([^;]+);""").findAll(block).associate {
            it.groupValues[1] to it.groupValues[2].trim()
        }

    private val light = declarations(tokenBlocks[0].value)
    private val darkMedia = declarations(tokenBlocks[1].value)
    private val darkExplicit = declarations(tokenBlocks[2].value)
    private val dark = light + darkExplicit

    private val outsideTokens: String =
        tokenBlocks.fold(css) { text, block -> text.replace(block.value, "") }

    @Test
    fun threeTokenBlocksAuthorLightAndOneDark() {
        assertEquals(3, tokenBlocks.size, "light :root, prefers-color-scheme dark, [data-theme=dark]")
        assertEquals(darkExplicit, darkMedia, "both dark blocks author the same dark values")
    }

    @Test
    fun everyTokenResolvesAndEveryDeclaredTokenIsUsed() {
        // Component-local properties (`.frame { --before: 50% }`) are declared where they live.
        val declared = Regex("""(--[\w-]+)\s*:""").findAll(css).map { it.groupValues[1] }.toSet()
        // A fallback (`var(--x, 12px)`) hides a missing token; it does not count as declared.
        val used = Regex("""var\((--[\w-]+)""").findAll(css).map { it.groupValues[1] }.toSet()
        assertEquals(emptySet(), used - declared, "used but never declared")
        // A scale is declared whole (the v2 ramp and spacing); every other token must be used.
        val scales = listOf("--gray-", "--space-")
        assertEquals(emptySet(), (light.keys - used).filterNot { name -> scales.any(name::startsWith) }.toSet(), "declared but never used")
    }

    @Test
    fun colorsShapesAndSizesGoThroughTokens() {
        val colorLiteral = Regex("""#[0-9a-fA-F]{3,8}\b|rgba?\(|hsla?\(|oklch\(""")
        assertEquals(
            emptyList(),
            colorLiteral.findAll(outsideTokens).map { it.value }.toList(),
            "color literals outside the token blocks",
        )
        // Vextreme v2: components reach for semantic tokens, never ramp stops.
        assertEquals(
            emptyList(),
            Regex("""var\(--(?:gray-\d|black|white)\)""").findAll(outsideTokens).map { it.value }.toList(),
            "ramp stops outside the token blocks",
        )
        val radius = Regex("""border-radius:\s*([^;]+);""").findAll(outsideTokens).map { it.groupValues[1].trim() }
        radius.forEach { value ->
            assertTrue(
                value == "50%" || value.split(' ').all { it.startsWith("var(--radius-") },
                "border-radius $value is not on the radius scale",
            )
        }
        val sizes = Regex("""font-size:\s*([^;]+);""").findAll(outsideTokens).map { it.groupValues[1].trim() }
        sizes.forEach { value ->
            assertTrue(
                value.startsWith("var(--step-") || value.endsWith("em") || value == "inherit",
                "font-size $value is not on the type scale",
            )
        }
    }

    @Test
    fun rawSpacingOnlyShrinks() {
        val raw =
            Regex("""(?:padding|margin|gap)(?:-[a-z]+)?:[^;]*?\b\d+(?:\.\d+)?px""").findAll(outsideTokens).count()
        // DESIGN.md "Not yet aligned": spacing still uses raw px. Lower this as it is migrated.
        assertTrue(raw <= RawSpacingCeiling, "raw px spacing grew to $raw (ceiling $RawSpacingCeiling)")
    }

    @Test
    fun textIsReadableInBothThemes() {
        val textTokens =
            Regex("""(?<![-\w])color:\s*var\((--[\w-]+)\)""").findAll(outsideTokens).map { it.groupValues[1] }.toSet()
        val inverse = setOf("--ticket-text", "--ticket-text-secondary", "--critical-text", "--accent-text-on", "--text-inverse")
        for ((theme, tokens) in listOf("light" to light, "dark" to dark)) {
            for (text in textTokens - inverse) {
                for (ground in listOf("--bg-surface", "--bg-canvas", "--bg-surface-raised")) {
                    val ratio = contrast(color(tokens, text), color(tokens, ground))
                    assertTrue(ratio >= 4.5, "$theme $text on $ground is ${"%.2f".format(ratio)}:1")
                }
            }
            val pairs =
                listOf(
                    "--ticket-text" to "--ticket-bg",
                    "--ticket-text-secondary" to "--ticket-bg",
                    "--critical-text" to "--critical-bg",
                    "--accent-text-on" to "--accent-bg",
                    "--text-primary" to "--bg-overlay",
                )
            for ((text, ground) in pairs) {
                val ratio = contrast(color(tokens, text), color(tokens, ground))
                assertTrue(ratio >= 4.5, "$theme $text on $ground is ${"%.2f".format(ratio)}:1")
            }
        }
    }

    @Test
    fun vexsInsideIsDrawnWithTheRelationshipLegend() {
        val kinds = Regex("""\.context-section\[data-context-kind="([a-z]+)"]\s*\{([^}]*)}""").findAll(css).toList()
        assertTrue(kinds.size >= 10, "Vex Context section kinds")
        kinds.forEach { assertTrue("var(--relation-" in it.groupValues[2], "${it.groupValues[1]} rail") }
        assertTrue(
            Regex("""\.context-section\[data-context-kind]\s*\{[^}]*border-left-color:\s*var\(--relation\)""").containsMatchIn(css),
            "one rule draws every relationship the same way",
        )
        val reasoning = Regex("""\n\.reasoning\s*\{([^}]*)}""").find(css)!!.groupValues[1]
        assertTrue("var(--relation-process)" in reasoning, "thoughts share the process rail with Vex Context")
        assertTrue(
            Regex("""\.thought-toggle \.thought-mark\s*\{[^}]*var\(--relation-process\)""").containsMatchIn(css),
            "the thought toggle's mark names the process relationship",
        )
        val model = Regex("""\.model-state-card, \.model-configuration, \.model-import-card\s*\{([^}]*)}""").find(css)!!.groupValues[1]
        assertTrue("var(--relation-runtime)" in model, "model cards carry the runtime rail")
        val legend = light.keys.filter { it.startsWith("--relation-") }.toSet()
        assertEquals(legend - setOf("--relation-exact"), darkExplicit.keys.filter { it.startsWith("--relation-") }.toSet(), "dark re-authors every colored relationship")
    }

    /** Linear sRGB of a token: `#rrggbb` or `oklch(L% C H)`, resolved through var() chains. */
    private fun color(tokens: Map<String, String>, name: String): DoubleArray {
        var value = requireNotNull(tokens[name]) { "$name is not declared" }
        while (value.startsWith("var(")) value = requireNotNull(tokens[value.removePrefix("var(").removeSuffix(")")]) { "$value" }
        Regex("#([0-9a-fA-F]{6})").matchEntire(value)?.let { hex ->
            return DoubleArray(3) { i ->
                val c = hex.groupValues[1].substring(i * 2, i * 2 + 2).toInt(16) / 255.0
                if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
            }
        }
        val oklch = requireNotNull(Regex("""oklch\(\s*([\d.]+)%\s+([\d.]+)\s+([\d.]+)""").find(value)) { "$name is $value" }
        val l = oklch.groupValues[1].toDouble() / 100
        val c = oklch.groupValues[2].toDouble()
        val h = Math.toRadians(oklch.groupValues[3].toDouble())
        val a = c * kotlin.math.cos(h)
        val b = c * kotlin.math.sin(h)
        val l1 = (l + 0.3963377774 * a + 0.2158037573 * b).pow(3)
        val m1 = (l - 0.1055613458 * a - 0.0638541728 * b).pow(3)
        val s1 = (l - 0.0894841775 * a - 1.2914855480 * b).pow(3)
        return doubleArrayOf(
            4.0767416621 * l1 - 3.3077115913 * m1 + 0.2309699292 * s1,
            -1.2684380046 * l1 + 2.6097574011 * m1 - 0.3413193965 * s1,
            -0.0041960863 * l1 - 0.7034186147 * m1 + 1.7076147010 * s1,
        ).map { it.coerceIn(0.0, 1.0) }.toDoubleArray()
    }

    private fun luminance(rgb: DoubleArray): Double = 0.2126 * rgb[0] + 0.7152 * rgb[1] + 0.0722 * rgb[2]

    private fun contrast(a: DoubleArray, b: DoubleArray): Double {
        val (high, low) = listOf(luminance(a), luminance(b)).sortedDescending()
        return (high + 0.05) / (low + 0.05)
    }

    private companion object {
        const val RawSpacingCeiling = 147
    }
}

// [VXG RealForever]
