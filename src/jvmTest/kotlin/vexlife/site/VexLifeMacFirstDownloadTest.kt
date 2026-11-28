package vexlife.site

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

/** The public Mac download surface keeps Gatekeeper guidance visible without owning the protected decision. */
class VexLifeMacFirstDownloadTest {
    private val root: Path = Path.of("").toAbsolutePath()
    private val page = Files.readString(root.resolve("site/index.html"))
    private val script = Files.readString(root.resolve("site/assets/site.js"))
    private val css = Files.readString(root.resolve("site/assets/site.css"))

    @Test
    fun privacyAndSecuritySitsInThePrimaryDownloadPath() {
        val download = page.indexOf("id=\"mac-download\"")
        val arrow = page.indexOf("id=\"mac-security-arrow\"")
        val security = page.indexOf("id=\"mac-security\"")
        val notes = page.indexOf("id=\"mac-notes\"")

        assertTrue(download >= 0)
        assertTrue(arrow > download)
        assertTrue(security > arrow)
        assertTrue(notes > security)
        assertContains(page, "Open Privacy &amp; Security")
        assertContains(
            page,
            "x-apple.systempreferences:com.apple.settings.PrivacySecurity.extension",
        )
    }

    @Test
    fun gatekeeperPathUsesTheLivedMacScreenshots() {
        listOf(
            "gatekeeper-not-opened.png",
            "gatekeeper-privacy-security.png",
            "gatekeeper-open-anyway-confirm.png",
            "gatekeeper-admin-auth.png",
        ).forEach { name ->
            assertTrue(Files.isRegularFile(root.resolve("site/walkthrough").resolve(name)))
            assertContains(script, name)
        }

        assertContains(script, "If macOS blocks the first launch, this is the path.")
        assertContains(script, "Open Anyway")
        assertContains(script, "VexLife does not bypass this decision.")
        assertContains(script, "https://support.apple.com/102445")
        assertContains(css, ".mac-trust-gallery")
        assertContains(css, ".mac-trust-shot")
    }

    @Test
    fun releaseAssetSizeUsesHumanUnits() {
        assertContains(script, "humanFileSize")
        assertContains(script, "' GB'")
        assertContains(script, "' MB'")
        assertContains(script, "fact('Size', humanFileSize(mac.size))")
    }
}
