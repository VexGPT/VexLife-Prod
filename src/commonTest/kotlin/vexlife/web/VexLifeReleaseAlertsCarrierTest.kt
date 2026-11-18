package vexlife.web

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vexlife.release.VexLifeReleaseInboxFamily
import vexlife.service.VexLifeProdServiceContract

/**
 * Transitional pre-Spiral manifestation proof.
 *
 * Product release-alert meaning stays below this current utility-row carrier. When the Product
 * Spiral is accepted, only the presentation locus changes; ReleaseInbox and service semantics stay.
 */
class VexLifeReleaseAlertsCarrierTest {
    private val bundle =
        VexLifeWebP1RuntimeArtifacts.generate()

    @Test
    fun currentUtilityRowManifestsTheAuthoredReleaseAlertsControlAndPanel() {
        assertTrue(
            bundle.html.contains(
                "data-vex-ref=\"${VexLifeWebP1.ReleaseAlerts.identity.ref}\"",
            ),
        )
        assertTrue(
            bundle.html.contains(
                "data-vex-ref=\"${VexLifeWebP1.ReleaseAlertsPanel.identity.ref}\"",
            ),
        )
        assertTrue(
            bundle.html.contains(
                "data-vex-ref=\"${VexLifeWebP1.ReleaseAlertsList.identity.ref}\"",
            ),
        )
        assertTrue(bundle.javascript.contains(VexLifeProdServiceContract.ReleaseAlertsPath))
        assertTrue(bundle.javascript.contains(VexLifeProdServiceContract.ReleaseAlertsControlPath))
        assertTrue(bundle.javascript.contains(VexLifeProdServiceContract.ReleaseAlertsStringsPath))
        assertTrue(bundle.javascript.contains(VexLifeReleaseInboxFamily.KeyQuickAction))
        assertTrue(bundle.javascript.contains("state.releaseAlerts?.display?.unreadCount"))
        assertTrue(bundle.javascript.contains("button.dataset.attention"))
        assertTrue(bundle.javascript.contains("entry.releaseIdentifier"))
        assertTrue(bundle.javascript.contains("entry.viewControlRef"))
        assertTrue(bundle.javascript.contains("entry.dismissControlRef"))
        assertTrue(bundle.javascript.contains("release: entry.release"))
        assertTrue(bundle.javascript.contains("result.pageToOpenOrNull"))
        assertTrue(bundle.css.contains(".quick-action[data-attention=\"true\"]::after"))
        assertTrue(bundle.css.contains("background: var(--danger)"))
    }

    @Test
    fun releaseObservationDoesNotBlockStartupOrBecomeHighFrequencyPolling() {
        val polling =
            bundle.javascript
                .substringAfter("setInterval(() => {")
                .substringBefore("}, 2500);")
        assertFalse(
            polling.contains("loadReleaseAlerts"),
            "release alerts must not join the high-frequency Conversation/Vision polling loop",
        )

        val startup =
            bundle.javascript
                .substringAfter("void Promise.all([")
                .substringBefore("])")
        assertFalse(
            startup.contains("loadReleaseAlerts"),
            "outside release observation must not hold Product startup rendering",
        )
        assertTrue(
            bundle.javascript.contains("void loadReleaseAlerts().catch(() => {});"),
            "release observation should begin after core presentation arrival",
        )
        assertTrue(
            bundle.javascript.contains("document.addEventListener('visibilitychange'"),
            "foreground return should create a freshness opportunity",
        )
    }

    @Test
    fun carrierDoesNotOwnReleaseWordsOrInventEngagementAnimation() {
        assertFalse(
            bundle.javascript.contains("VexLife updates"),
            "release-alert words must come from the Family's keyed DisplayState",
        )
        assertFalse(
            bundle.html.contains("VexLife updates"),
            "release-alert markup must not author Product words",
        )
        assertFalse(
            bundle.css.contains("pulse", ignoreCase = true),
            "release attention is positional, not animated engagement pressure",
        )
    }
}

// [VXG RealForever]
