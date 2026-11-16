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
    private val script = Files.readString(root.resolve("site/assets/site.js"))

    @Test
    fun publicHomePinsTheSharedSpiralAndRegistersOnlyTruthfulPageFeatures() {
        assertContains(page, "VEX_SPIRAL_FEATURES")
        assertContains(page, "Vextreme@da073582bc3147642b78640f8c72290396fc2489/widgets/vex-fab.js")
        assertContains(page, "Vextreme@da073582bc3147642b78640f8c72290396fc2489/components/vex-spiral-features.js")
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
    fun existingThemeControlRemainsTheBehaviorAndIconOwnerBehindTheSpiralProjection() {
        assertContains(page, "id=\"theme-toggle\"")
        assertContains(page, "id=\"theme-toggle\" aria-label=\"Switch between dark and light\" hidden")
        assertContains(script, "data-vex-spiral-icon")
        assertContains(script, "light ? '☀️' : '🌙'")
        assertContains(css, ".spiral-actions")
        assertContains(css, "#vex-spiral-trigger")
    }

    @Test
    fun openCloseAndReopenUseOneHeaderProjectionCircuit() {
        assertContains(script, "vex:spiral-change")
        assertContains(script, "header.dataset.spiralOpen")
        assertContains(script, "node.inert = true")
        assertContains(script, "node.inert = false")
        assertContains(css, "data-spiral-open=\"true\"")
        assertContains(css, "translateX(92px) scale(0.72)")
        assertContains(css, "translateX(64px) scale(0.80)")
    }

    @Test
    fun terrainIsVisuallyQuieterInsideTheSpiral() {
        assertContains(css, "[data-vex-spiral-feature-id=\"terrain\"]")
        assertContains(css, "font-size: 14px")
    }
}

// [VXG RealForever]
