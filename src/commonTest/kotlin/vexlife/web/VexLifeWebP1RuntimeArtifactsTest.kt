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
                "\"type\":\"CAPABILITY\",\"capabilityRef\":\"vision.selected-scope\"",
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
                "id=\"page-frame\" title=\"Opened page\" referrerpolicy=\"no-referrer\"",
            ),
        )
        assertTrue(
            bundle.html.contains(
                "id=\"context-frame\"",
            ),
        )
        assertTrue(
            bundle.html.contains(
                "sandbox=\"allow-scripts allow-same-origin\"",
            ),
        )

        assertTrue(
            bundle.html.contains(
                "placeholder=\"Type a web address\"",
            ),
        )
        assertTrue(
            bundle.html.contains(
                "Open a web page from the address field. Vex page context is separate and may not be available for every page.",
            ),
        )
        assertFalse(
            bundle.html.contains(
                "placeholder=\"Type a Vex-owned web address\"",
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
                VexLifeProdServiceContract.VisionControlPath,
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                VexLifeProdBrowserContract.InitialPageUrl,
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "const contextFrameUrlFor = (pageUrl) =>",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "ui.pageFrame.src = pageUrl",
            ),
        )
        // The generated script is trimIndent()-ed, so assert adjacency, not source indentation.
        assertTrue(
            Regex("""ui\.pageFrame\.src = pageUrl;\s*ui\.contextFrame\.removeAttribute\('src'\);""")
                .containsMatchIn(bundle.javascript),
            "ordinary navigation must not eagerly load the Vex context proxy",
        )
        assertTrue(
            bundle.javascript.contains(
                "const contextFrameUrl = contextFrameUrlFor(state.contextTargetPageUrl);",
            ) &&
                bundle.javascript.contains(
                    "state.contextLoading = true;",
                ) &&
                bundle.javascript.contains(
                    "ui.contextFrame.src = contextFrameUrl;",
                ),
            "the context provider should load only after explicit Include-page intent",
        )
        assertTrue(
            bundle.javascript.contains(
                "A separately rendered snapshot of this entered URL will be given to Vex on the next turn.",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "Separate context-provider rendering",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "event.source !== ui.contextFrame.contentWindow",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "state.contextTargetPageUrl",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "Current address unavailable · enter a web address",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "ui.query.value = ''",
            ),
            "an unobservable child navigation must not leave the prior URL displayed as current",
        )
        assertTrue(
            bundle.javascript.contains(
                "Only http and https web pages open here.",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "The embedded Browser does not navigate to the local Product service.",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "Vex page context is not available for this site yet.",
            ),
        )
        assertFalse(
            bundle.javascript.contains(
                "ui.pageFrame.src = frameUrlFor(pageUrl)",
            ),
        )
        assertFalse(
            bundle.javascript.contains(
                "This page is outside the admitted Vex-owned public boundary.",
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
            bundle.javascript.contains(
                "current.modelArtifactPresentBySizeBytes",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "Exact hashes are not rechecked in this awareness view.",
            ),
        )
        assertTrue(
            bundle.html.contains(
                "Kept in your archives",
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
                "const activateVision = async () =>",
            ),
        )
        // Memory is projected by the service's Memory Family; the renderer only shows its DisplayState.
        assertTrue(bundle.javascript.contains("const loadMemory = async () =>"))
        // Read aloud reads the Conversation projection (or the private one) and offers only on-device voices.
        assertTrue(bundle.javascript.contains(": (state.conversation?.messages ?? []))"))
        // A private message goes only to the private route.
        assertTrue(bundle.javascript.contains("await sendPrivate();"))
        assertTrue(bundle.javascript.contains("voice.localService &&"))
        assertTrue(bundle.javascript.contains("state.memory?.display"))
        assertFalse(bundle.html.contains(">Not connected</button>"), "unbuilt controls say what they are, not 'Not connected'")
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
                "state.vision = result.vision;",
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
        assertTrue(
            bundle.html.contains(
                "sandbox=\"allow-scripts allow-same-origin allow-forms allow-popups allow-popups-to-escape-sandbox allow-downloads allow-modals\"",
            ),
            "the human Browser frame should retain ordinary in-pane web interaction without top-navigation authority",
        )
        assertTrue(
            bundle.html.contains(
                "id=\"context-frame\"",
            ) &&
                bundle.html.contains(
                    "sandbox=\"allow-scripts allow-same-origin\"",
                ),
            "the Vex context provider must remain stricter than the human Browser frame",
        )
        assertFalse(
            bundle.html.contains(
                "allow-top-navigation",
            ),
        )
    }
}

// [VXG RealForever]
