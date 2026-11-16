package vexlife.site

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContains

/** The Mac trust handoff stays distinct from VexLife's own in-app first run. */
class VexLifeMacFirstDownloadTest {
    private val root: Path = Path.of("").toAbsolutePath()
    private val page = Files.readString(root.resolve("site/index.html"))
    private val script = Files.readString(root.resolve("site/assets/site.js"))
    private val css = Files.readString(root.resolve("site/assets/site.css"))

    @Test
    fun macApprovalActionSitsBesideTheMacDownload() {
        assertContains(page, "id=\"mac-download\"")
        assertContains(page, "id=\"mac-security\"")
        assertContains(page, "Allow VexLife on this Mac")
        assertContains(
            page,
            "x-apple.systempreferences:com.apple.settings.PrivacySecurity.extension",
        )
    }

    @Test
    fun trustFlowExplainsTheExactGatekeeperTransitionBeforeInAppSetup() {
        assertContains(script, "First time on this Mac")
        assertContains(script, "“VexLife” Not Opened")
        assertContains(script, "“VexLife” was blocked to protect your Mac.")
        assertContains(script, "Open Anyway")
        assertContains(script, "continue to VexLife First run")
        assertContains(page, "After macOS allows VexLife to open")
    }

    @Test
    fun trustVisualHighlightsOnlyTheMacSecurityPath() {
        assertContains(css, ".mac-trust-flow")
        assertContains(css, ".security-open-row")
        assertContains(css, "border: 2px solid #f0d88a")
        assertContains(script, "Apple instructions")
        assertContains(script, "https://support.apple.com/102445")
    }
}
