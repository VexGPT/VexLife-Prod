package vexlife.web

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vexlife.workspace.VexLifeWorkspaceBlueprint

/** The Product Brand coordinate carries the canonical Vextreme family mark; the name stays VexLife. */
class VexLifeFamilyMarkTest {
    private val bundle = VexLifeWebP1RuntimeArtifacts.generate()

    @Test
    fun brandCoordinateCarriesTheCanonicalFamilyMarkAndKeepsTheVexLifeName() {
        val brand =
            bundle.html.substringAfter("data-vex-ref=\"${VexLifeWorkspaceBlueprint.Brand.ref}\"")
                .substringBefore("</a>")

        assertTrue(brand.contains("<svg viewBox=\"0 0 120 120\"><path d=\"${VexLifeWebP1ExperienceMarkup.FamilyMarkPath}\"/></svg>"))
        assertTrue(brand.contains("<span class=\"brand-name\">VexLife</span>"))
        assertFalse(bundle.html.contains("<circle cx=\"16\" cy=\"16\" r=\"10.5\"/>"), "hand-drawn mark must be gone")
    }

    @Test
    fun familyMarkFollowsThemeInkInsteadOfAFixedDarkTile() {
        val css = bundle.css
        assertTrue(css.contains(".brand-mark svg { width: 100%; height: 100%; fill: none; stroke: currentColor; stroke-width: 5;"))
        assertFalse(css.contains("background: #1b1e22;"))
    }
}

// [VXG RealForever]
