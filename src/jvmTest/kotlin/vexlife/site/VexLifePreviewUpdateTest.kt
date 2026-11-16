package vexlife.site

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContains

/** Release-surface invariants for preview comparison and branded Mac disk images. */
class VexLifePreviewUpdateTest {
    private val root: Path = Path.of("").toAbsolutePath()
    private val siteScript = Files.readString(root.resolve("site/assets/site.js"))
    private val releaseContract =
        Files.readString(root.resolve("packaging/macos-app/release-contract.json"))

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
}
