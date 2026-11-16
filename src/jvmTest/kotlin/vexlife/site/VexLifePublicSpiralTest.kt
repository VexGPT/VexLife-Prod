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
        assertContains(page, "Vextreme@04ee63c4bf9d2ccbbb03e93514a8a67d242c49db/widgets/vex-fab.js")
        assertContains(page, "Vextreme@04ee63c4bf9d2ccbbb03e93514a8a67d242c49db/components/vex-spiral-features.js")
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
}

// [VXG RealForever]
