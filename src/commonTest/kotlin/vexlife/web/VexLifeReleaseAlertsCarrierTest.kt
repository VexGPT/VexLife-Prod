package vexlife.web

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vexlife.release.VexLifeReleaseInboxFamily
import vexlife.service.VexLifeProdServiceContract

/**
 * Current Product-Spiral manifestation proof.
 *
 * ReleaseInbox remains the semantic owner. The Spiral owns only presentation geometry and the
 * positional attention projection:
 *
 * collapsed Spiral -> unread attention on the Spiral host
 * expanded Spiral  -> unread attention on the authored Alerts child
 */
class VexLifeReleaseAlertsCarrierTest {
    private val bundle =
        VexLifeWebP1RuntimeArtifacts.generate()

    @Test
    fun spiralReusesTheAuthoredReleaseAlertsControlAndPanel() {
        assertTrue(
            bundle.html.contains(
                "data-vex-ref=\"${VexLifeWebP1.ReleaseAlerts.identity.ref}\"",
            ),
        )
        assertTrue(
            bundle.html.contains(
                "id=\"release-alerts-source\" hidden",
            ),
            "the authored release-alert control should remain the source coordinate",
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
        assertTrue(bundle.javascript.contains("byId('release-alerts-source')"))
        assertTrue(bundle.javascript.contains("state.releaseAlerts?.display?.unreadCount"))
        assertTrue(bundle.javascript.contains("unreadReleaseAlerts && !state.quickActionsOpen"))
        assertTrue(bundle.javascript.contains("unreadReleaseAlerts && state.quickActionsOpen"))
        assertTrue(bundle.javascript.contains("trigger.dataset.attention"))
        assertTrue(bundle.javascript.contains("releaseAlertsButton.dataset.attention"))
        assertTrue(bundle.javascript.contains("entry.releaseIdentifier"))
        assertTrue(bundle.javascript.contains("entry.viewControlRef"))
        assertTrue(bundle.javascript.contains("entry.dismissControlRef"))
        assertTrue(bundle.javascript.contains("release: entry.release"))
        assertTrue(bundle.javascript.contains("result.pageToOpenOrNull"))
        assertTrue(bundle.css.contains(".quick-action[data-attention=\"true\"]::after"))
        assertTrue(bundle.css.contains("background: var(--critical-bg)"))
        assertFalse(
            bundle.javascript.contains(
                "const QUICK_ACTIONS = [",
            ),
            "Alerts must not restore the old parallel quick-action registry",
        )
    }

    @Test
    fun releaseObservationDoesNotBlockStartupOrBecomeHighFrequencyPolling() {
        // The ConversationTurn projection is re-observed every tick (Stop needs it while a Send is
        // pending); the Conversation read itself still waits for the pending Send.
        val pollingStart =
            bundle.javascript.indexOf(
                "if (!state.pending) void loadConversation().catch(() => {});",
            )
        assertTrue(
            pollingStart >= 0,
            "the high-frequency Conversation/Vision polling loop must remain present",
        )
        val polling =
            bundle.javascript
                .substring(
                    pollingStart,
                )
                .substringBefore(
                    "}, 2500);",
                )
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
    fun spiralDoesNotOwnReleaseWordsOrInventEngagementAnimation() {
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
