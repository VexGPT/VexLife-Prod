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
                "--ground:#eef0ee",
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
                "Vex companion",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "Summon Vex",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "Converse with Vex",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "companionTitle.textContent = activeCompanionView === 'conversation' ? 'Converse with Vex' : 'Settings'",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "Include this page",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "Start clean chat",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "includeBrowserContext ? currentBrowserContext : null",
            ),
        )
        assertTrue(
            bundle.css.contains(
                "--paper:#efe5cd",
            ),
        )
        assertTrue(
            bundle.css.contains(
                "grid-template-columns:auto minmax(220px,520px) auto minmax(0,1fr)",
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
                "grid-template-columns:minmax(0,1fr) 40px",
            ),
        )
        assertFalse(
            bundle.css.contains(
                "max-height:34%;overflow:auto;border-top",
            ),
        )
        assertFalse(
            bundle.css.contains(
                "--vex-context-workspace-boundary:",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "authoredBrowserShare: 0.5",
            ),
        )
        assertFalse(
            bundle.css.contains(
                "--vex-browser-share:",
            ),
        )
        assertTrue(
            bundle.css.contains(
                "left:var(--vex-browser-share)",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "Vex companion ",
            ),
        )
        assertFalse(
            bundle.javascript.contains(
                "vexContextWorkspaceBoundary",
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
