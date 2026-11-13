package vexlife.web

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vexlife.browser.VexLifeProdBrowserContract
import vexlife.service.VexLifeProdServiceContract
import vexlife.workspace.VexLifeWorkspaceBlueprint

class VexLifeWebP1RuntimeArtifactsTest {
    @Test
    fun productPresentationKeepsA003InteractionContractOverCurrentOwners() {
        val bundle =
            VexLifeWebP1RuntimeArtifacts.generate()

        assertTrue(
            bundle.semanticManifest.contains(
                "\"type\":\"CAPABILITY\",\"capabilityRef\":\"conversation.send\"",
            ),
        )
        assertTrue(
            bundle.semanticManifest.contains(
                "\"type\":\"CAPABILITY\",\"capabilityRef\":\"vision.look-now\"",
            ),
        )

        VexLifeWorkspaceBlueprint.elements.forEach { element ->
            assertTrue(
                bundle.html.contains(
                    "data-vex-ref=\"${element.ref}\"",
                ),
                "missing current Blueprint coordinate from product template: ${element.ref}",
            )
        }

        assertTrue(
            bundle.html.contains(
                "class=\"workspace\"",
            ),
        )
        assertTrue(
            bundle.html.contains(
                "class=\"frame\"",
            ),
        )
        assertTrue(
            bundle.html.contains(
                "id=\"collapse-before\"",
            ),
        )
        assertTrue(
            bundle.html.contains(
                "id=\"collapse-after\"",
            ),
        )
        assertTrue(
            bundle.html.contains(
                "enterkeyhint=\"send\"",
            ),
        )
        assertTrue(
            bundle.html.contains(
                "sandbox=\"allow-scripts allow-same-origin\"",
            ),
        )

        assertTrue(
            bundle.html.contains(
                "placeholder=\"Type a Vex-owned web address\"",
            ),
        )
        assertTrue(
            bundle.html.contains(
                "Open a Vex-owned public page from the address field. Pages open signed out.",
            ),
        )
        assertFalse(
            bundle.html.contains(
                "Open any public web page from the address field.",
            ),
        )

        assertTrue(
            bundle.css.contains(
                "grid-template-columns: minmax(0, var(--before)) 12px minmax(0, 1fr)",
            ),
        )
        assertTrue(
            bundle.css.contains(
                ".quick-actions { display: flex",
            ),
        )
        assertTrue(
            bundle.css.contains(
                ".message-vex .bubble",
            ),
        )
        assertTrue(
            bundle.css.contains(
                ".composer {",
            ),
        )
        assertTrue(
            bundle.css.contains(
                "grid-row: 2;",
            ),
            "the composer must remain outside the independently scrolling Conversation viewport",
        )
        assertTrue(
            bundle.css.contains(
                "padding: 8px 14px 14px",
            ),
        )
        assertTrue(
            bundle.css.contains(
                ".switch[aria-checked=\"true\"] span",
            ),
        )

        assertTrue(
            bundle.javascript.contains(
                VexLifeProdServiceContract.ConversationSendPath,
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                VexLifeProdServiceContract.ConversationPath,
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                VexLifeProdServiceContract.ContextPath,
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                VexLifeProdServiceContract.SourceCurrentnessPath,
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                VexLifeProdServiceContract.CurrentStatePath,
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                VexLifeProdServiceContract.VisionPath,
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                VexLifeProdServiceContract.VisionLookNowPath,
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                VexLifeProdBrowserContract.InitialPageUrl,
            ),
        )

        assertTrue(
            bundle.javascript.contains(
                "event.key === 'Enter' && !event.shiftKey && !event.isComposing",
            ),
        )
        assertFalse(
            bundle.javascript.contains(
                "event.metaKey || event.ctrlKey",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "state.pending = pending",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "presentation.pending.",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "browserContext: state.includePage ? state.browserContext : null",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "const retrying = state.pending !== null",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "const pending = state.pending ?? Object.freeze",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "retrySameSendAttemptAllowed",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "const retireAttempt = status === 400 || retryAllowed === false",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "Turn completed, but the current projection could not be refreshed",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "VexLife Memory contributed through",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "Whole-history enumeration: ",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "Journeys",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "Light or dark",
            ),
        )
        assertTrue(
            bundle.html.contains(
                "Current resources",
            ),
        )
        assertTrue(
            bundle.html.contains(
                "id=\"resource-note\"",
            ),
        )
        assertTrue(
            bundle.html.contains(
                "id=\"home-current-state\"",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "Storage available: ",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "current.runtimeCurrentness",
            ),
        )
        assertTrue(
            bundle.html.contains(
                "Start clean chat",
            ),
        )
        assertTrue(
            bundle.html.contains(
                "What Vex receives",
            ),
        )
        assertTrue(
            bundle.html.contains(
                "id=\"vision-control\"",
            ),
        )
        assertTrue(
            bundle.html.contains(
                ">Vision</span>",
            ),
        )
        assertTrue(
            bundle.html.contains(
                "class=\"vision-state\">Held</span>",
            ),
        )
        assertTrue(
            bundle.html.contains(
                "aria-disabled=\"true\" disabled",
            ),
        )
        assertTrue(
            bundle.html.contains(
                "Vision availability is checking",
            ),
        )
        assertTrue(
            bundle.html.contains(
                "Vision route availability is checking.",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "const loadVision = async () =>",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "const lookNow = async () =>",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "state.visionInFlight",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "request.vexlife-prod.vision.presentation.",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "Vision · Ready — one snapshot was captured. Visual interpretation remains separate.",
            ),
        )
        assertFalse(
            bundle.html.contains(
                "screen permission and capture are not connected yet.",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "Include the page to hand Vex its text. Enter sends; Shift+Enter starts a new line.",
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
        assertTrue(
            bundle.javascript.contains(
                "vision-control",
            ),
        )
        assertFalse(
            bundle.javascript.contains(
                "getDisplayMedia",
            ),
        )
        assertFalse(
            bundle.javascript.contains(
                "mediaDevices",
            ),
        )
        assertFalse(
            bundle.javascript.contains(
                "ScreenCaptureKit",
            ),
        )
        assertFalse(
            bundle.html.contains(
                "allow-forms",
            ),
        )
        assertFalse(
            bundle.html.contains(
                "allow-popups",
            ),
        )
    }
}

// [VXG RealForever]
