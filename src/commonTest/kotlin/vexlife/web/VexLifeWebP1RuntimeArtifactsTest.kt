package vexlife.web

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vexlife.browser.VexLifeProdBrowserContract
import vexlife.service.VexLifeProdServiceContract

class VexLifeWebP1RuntimeArtifactsTest {
    @Test
    fun connectedBundleProjectsContractsWithoutOwningBackendMeaning() {
        val bundle =
            VexLifeWebP1RuntimeArtifacts.generate()

        assertTrue(
            bundle.semanticManifest.contains(
                "\"type\":\"CAPABILITY\",\"capabilityRef\":\"conversation.send\"",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                VexLifeProdServiceContract.ConversationSendPath,
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                VexLifeProdServiceContract.SourceCurrentnessPath,
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                VexLifeProdServiceContract.ContextPath,
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                VexLifeWebP1.SettingsContext.identity.ref,
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "Capabilities Vex received",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "OMITTED",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                VexLifeProdBrowserContract.InitialPageUrl,
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "allow-scripts allow-same-origin",
            ),
        )
        assertFalse(
            bundle.javascript.contains(
                "allow-forms",
            ),
        )
        assertFalse(
            bundle.javascript.contains(
                "allow-popups",
            ),
        )
        assertFalse(
            bundle.javascript.contains(
                "127.0.0.1:8765",
            ),
        )
        assertFalse(
            bundle.javascript.contains(
                "/v1/chat/completions",
            ),
        )
    }
}

// [VXG RealForever]
