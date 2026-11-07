package vexlife.web

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vexlife.browser.VexLifeProdBrowserContract
import vexlife.service.VexLifeProdServiceContract
import vexlife.workspace.VexLifeWorkspaceBlueprint

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
                VexLifeProdBrowserContract.InitialPageUrl,
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "allow-scripts allow-same-origin",
            ),
        )
        assertTrue(
            bundle.css.contains(
                "--vex-surface-canvas:#0f1012",
            ),
        )
        assertTrue(
            bundle.css.contains(
                "data-vex-generated-part=\"conversation-message\"",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                VexLifeWebP1.ConversationJumpLatest.identity.ref,
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "isNearBottom",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "aria-orientation",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "setPointerCapture",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "Vex conversation workspace",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "Summon Vex",
            ),
        )
        assertTrue(
            bundle.semanticManifest.contains(
                "\"ref\":\"${VexLifeWorkspaceBlueprint.VexSummon.ref}\"",
            ),
        )
        assertTrue(
            bundle.css.contains(
                VexLifeWebP1.Refs.CompanionHeader.value,
            ),
        )
        assertTrue(
            bundle.css.contains(
                "grid-template-columns:minmax(0,1.6fr) minmax(210px,.7fr)",
            ),
        )
        assertFalse(
            bundle.css.contains(
                "max-height:34%;overflow:auto;border-top",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "authoredInitialShare: 0.5",
            ),
        )
        assertFalse(
            bundle.css.contains(
                "--vex-browser-share:",
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
