package vexlife.site

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

/** Release-surface invariants for preview comparison and branded Mac disk images. */
class VexLifePreviewUpdateTest {
    private val root: Path = Path.of("").toAbsolutePath()
    private val siteScript = Files.readString(root.resolve("site/assets/site.js"))
    private val releaseContract =
        Files.readString(root.resolve("packaging/macos-app/release-contract.json"))
    private val buildSource = Files.readString(root.resolve("build.gradle.kts"))
    private val appIconArtwork =
        Files.readString(root.resolve("packaging/macos-app/vextreme-app-icon.svg"))

    @Test
    fun newestTwoPreviewsRemainSelectableDuringUpdateComparison() {
        assertContains(
            siteScript,
            "mains.slice(0, 2).forEach(function (release) { choices.push(release); });",
        )
    }

    @Test
    fun macReleaseContractBindsTheBrandedMountedVolumeIcon() {
        assertContains(releaseContract, "\"diskImageVolumeIconName\": \".VolumeIcon.icns\"")
        assertContains(releaseContract, "\"applicationIconName\": \"applet.icns\"")
    }

    @Test
    fun appleScriptAssetCatalogCannotOverrideTheSourceOwnedAppletIcon() {
        assertContains(buildSource, "resources.resolve(\n                \"Assets.car\",")
        assertContains(buildSource, "\"CFBundleIconName\"")
        assertContains(buildSource, "\"CFBundleIconFile\"")
        assertContains(buildSource, "\"applet\"")
    }

    @Test
    fun unsignedFirstLaunchOffersTheSupportedPrivacySecurityRoute() {
        assertContains(
            siteScript,
            "x-apple.systempreferences:com.apple.settings.PrivacySecurity.extension",
        )
        assertContains(siteScript, "https://support.apple.com/102445")
        assertFalse(buildSource.contains("Open Privacy & Security.webloc"))
    }

    @Test
    fun sealedDiskImageStripsMountTimeBookkeeping() {
        assertContains(buildSource, "\".fseventsd\"")
        assertContains(buildSource, "\".Spotlight-V100\"")
        assertContains(buildSource, "\".Trashes\"")
    }

    @Test
    fun appIconUsesAFullCanvasCompositionInsteadOfThumbnailingTheRawMark() {
        assertContains(appIconArtwork, "width=\"1024\" height=\"1024\"")
        assertContains(appIconArtwork, "translate(62 62) scale(7.5)")
        assertContains(buildSource, "macApplicationIconArtworkSource")
        assertContains(buildSource, "packaging/macos-app/vextreme-app-icon.svg")
    }
}
