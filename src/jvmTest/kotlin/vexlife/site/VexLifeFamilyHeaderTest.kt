package vexlife.site

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContains

/** The public VexLife shell keeps the same stable sibling-family coordinates as Vextreme/VexWork. */
class VexLifeFamilyHeaderTest {
    private val root: Path = Path.of("").toAbsolutePath()
    private val page: String = Files.readString(root.resolve("site/index.html"))
    private val css: String = Files.readString(root.resolve("site/assets/site.css"))
    private val mark: String = Files.readString(root.resolve("site/assets/vextreme-mark.svg"))

    @Test
    fun familyRailKeepsStableSiblingDestinationsAndVexLifeSelectedInTheMiddle() {
        assertContains(page, "https://vgong24.github.io/Vextreme/pages/terrain-map.html")
        assertContains(page, "https://vexgpt.github.io/VexWork/")
        assertContains(page, """<a class="family-current" href="./" aria-current="page">VexLife</a>""")

        val vextreme = page.indexOf(">Vextreme</a>")
        val vexLife = page.indexOf(">VexLife</a>")
        val vexWork = page.indexOf(">VexWork</a>")
        check(vextreme >= 0 && vextreme < vexLife && vexLife < vexWork) {
            "family order must remain Vextreme / VexLife / VexWork"
        }
    }

    @Test
    fun selectedFamilyLabelChangesEmphasisWithoutChangingSiblingSlots() {
        assertContains(css, "grid-template-columns: 5.8rem 0.65rem 5.3rem 0.65rem 5.3rem")
        assertContains(css, "grid-template-columns: 4.9rem 0.65rem 4.7rem 0.65rem 4.8rem")
        assertContains(css, ".family-current")
        assertContains(css, "font-size: 20px !important")
        assertContains(css, "min-height: 76px")
    }

    @Test
    fun familyMarkReusesTheVextremeSourceShape() {
        assertContains(css, "mask: url(\"vextreme-mark.svg\")")
        assertContains(mark, "M 51.89 109.32 A 50 50")
        assertContains(
            mark,
            "vgong24/Vextreme@469f65c84236b220be3b3c6d1a207821da8eda2e",
        )
    }
}
