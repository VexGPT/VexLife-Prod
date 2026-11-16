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
        assertContains(page, "Vextreme@0fd053e9a0aa12572bd9b31b3b4d49534da8c549/widgets/vex-fab.js")
        assertContains(page, "Vextreme@0fd053e9a0aa12572bd9b31b3b4d49534da8c549/components/vex-spiral-features.js")
        assertContains(page, "id: 'theme'")
        assertContains(page, "id: 'support'")
        assertContains(page, "id: 'terrain'")
        assertContains(page, "target: '#theme-toggle'")
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
    fun existingThemeControlRemainsTheBehaviorOwnerBehindTheSpiralProjection() {
        assertContains(page, "id=\"theme-toggle\"")
        assertContains(page, "id=\"theme-toggle\" aria-label=\"Switch between dark and light\" hidden")
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
