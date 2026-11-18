package vexlife.web

import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The design foundation's laws (docs/foundation/DESIGN.md), held against the workspace style
 * itself: tokens resolve, colors and shapes go through tokens, both themes are authored, text is
 * readable, and Vex's inside is drawn with the relationship legend.
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
        assertEquals(emptySet(), light.keys - used, "declared but never used")
    }

    @Test
    fun colorsShapesAndSizesGoThroughTokens() {
        val colorLiteral = Regex("""#[0-9a-fA-F]{3,8}\b|rgba?\(|hsla?\(|oklch\(""")
        assertEquals(
            emptyList(),
            colorLiteral.findAll(outsideTokens).map { it.value }.toList(),
            "color literals outside the token blocks",
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
        val inverse = setOf("--surface", "--danger-ink", "--paper-ink", "--paper-ink-2", "--meter-ink")
        for ((theme, tokens) in listOf("light" to light, "dark" to dark)) {
            for (text in textTokens - inverse) {
                for (ground in listOf("--surface", "--ground", "--raised")) {
                    val ratio = contrast(hex(tokens, text), hex(tokens, ground))
                    assertTrue(ratio >= 4.5, "$theme $text on $ground is ${"%.2f".format(ratio)}:1")
                }
            }
            val pairs =
                listOf(
                    "--surface" to "--ink",
                    "--danger-ink" to "--danger",
                    "--paper-ink" to "--paper",
                    "--paper-ink-2" to "--paper",
                    "--meter-ink" to "--core",
                    "--meter-ink" to "--used",
                    "--meter-ink" to "--remaining",
                )
            for ((text, ground) in pairs) {
                val ratio = contrast(hex(tokens, text), hex(tokens, ground))
                assertTrue(ratio >= 4.5, "$theme $text on $ground is ${"%.2f".format(ratio)}:1")
            }
        }
    }

    @Test
    fun vexsInsideIsDrawnWithTheRelationshipLegend() {
        val kinds = Regex("""\.context-section\[data-context-kind="([a-z]+)"]\s*\{([^}]*)}""").findAll(css).toList()
        assertTrue(kinds.size >= 10, "Vex Context section kinds")
        kinds.forEach { assertTrue("var(--relation-" in it.groupValues[2], "${it.groupValues[1]} rail") }
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

    private fun hex(tokens: Map<String, String>, name: String): String {
        var value = requireNotNull(tokens[name]) { "$name is not declared" }
        while (value.startsWith("var(")) value = requireNotNull(tokens[value.removePrefix("var(").removeSuffix(")")])
        require(Regex("#[0-9a-fA-F]{6}").matches(value)) { "$name is $value, not a #rrggbb color" }
        return value
    }

    private fun luminance(hex: String): Double {
        val channels =
            listOf(1, 3, 5).map { hex.substring(it, it + 2).toInt(16) / 255.0 }.map {
                if (it <= 0.03928) it / 12.92 else ((it + 0.055) / 1.055).pow(2.4)
            }
        return 0.2126 * channels[0] + 0.7152 * channels[1] + 0.0722 * channels[2]
    }

    private fun contrast(a: String, b: String): Double {
        val (high, low) = listOf(luminance(a), luminance(b)).sortedDescending()
        return (high + 0.05) / (low + 0.05)
    }

    private companion object {
        const val RawSpacingCeiling = 208
    }
}

// [VXG RealForever]
