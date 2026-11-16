package vexlife.site

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

/**
 * The public Pages Spiral reuses Vextreme's pinned presentation host while VexLife owns the
 * capabilities it chooses to register on this arrival/download surface.
 */
class VexLifePublicSpiralTest {
    private val root: Path = Path.of("").toAbsolutePath()
    private val page = Files.readString(root.resolve("site/index.html"))
    private val css = Files.readString(root.resolve("site/assets/site.css"))

    @Test
    fun publicHomePinsTheSharedSpiralAndRegistersOnlyTruthfulPageFeatures() {
        assertContains(page, "VEX_SPIRAL_FEATURES")
        assertContains(page, "Vextreme@bf4d7a34da2766d6b3d1b048ba9063ae1943128e/widgets/vex-fab.js")
        assertContains(page, "Vextreme@bf4d7a34da2766d6b3d1b048ba9063ae1943128e/components/vex-spiral-features.js")
        assertContains(page, "id: 'theme'")
        assertContains(page, "id: 'support'")
        assertContains(page, "id: 'terrain'")
        assertContains(page, "target: '#theme-toggle'")
        val themeEntry = page.substringAfter("id: 'theme'").substringBefore("},")
        assertFalse(themeEntry.contains("icon:"))
        assertContains(page, "https://vgong24.github.io/Vextreme/pages/vex-support.html")
        assertContains(page, "https://vgong24.github.io/Vextreme/pages/terrain-map.html")
    }

    @Test
    fun publicHomeDoesNotPretendToOwnInstalledProductCapabilities() {
        val manifest = page.substringAfter("window.VEX_SPIRAL_FEATURES = [").substringBefore("];")
        assertFalse(manifest.contains("vision", ignoreCase = true))
        assertFalse(manifest.contains("speech", ignoreCase = true))
        assertFalse(manifest.contains("language", ignoreCase = true))
        assertFalse(manifest.contains("summon", ignoreCase = true))
    }

    @Test
    fun existingThemeControlIsRecycledIntoTheSpiralWithoutAProxy() {
        assertContains(page, "id=\"theme-toggle\"")
        assertContains(page, "target: '#theme-toggle'")
        assertContains(script, "toggle.textContent = light ? '☀️' : '🌙'")
        assertContains(script, "event.stopPropagation()")
        assertContains(css, ".spiral-actions")
        assertContains(css, "#vex-spiral-trigger")
        assertFalse(css.contains("data-spiral-open"))
        assertFalse(script.contains("vex:spiral-change"))
    }
}

// [VXG RealForever]
