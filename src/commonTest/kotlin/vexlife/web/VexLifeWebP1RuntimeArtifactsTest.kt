package vexlife.web

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vexlife.browser.VexLifeProdBrowserContract
import vexlife.service.VexLifeProdServiceContract
import vexlife.voiceinput.VexLifeVoiceInputFamily
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
                "\"type\":\"CAPABILITY\",\"capabilityRef\":\"conversation.cancel\"",
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
                "const send = async (newAttemptSeed = null) =>",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "const retryingExactAttempt = state.pending !== null && newAttemptSeed === null",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "idempotencyKey: 'send.' + crypto.randomUUID().toLowerCase()",
            ),
            "Try Again must form a new Send identity instead of replaying a failed key",
        )
        assertTrue(
            bundle.javascript.contains(
                "retrySameSendAttemptAllowed",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "let retireAttempt = status === 400 || retryAllowed === false",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "const interruptedProcess = snapshotLiveProcess()",
            ) &&
                bundle.javascript.contains(
                    "await completeCanonicalSend(reconciled, false, interruptedProcess)",
                ),
            "reconciliation must preserve the process the human already watched",
        )
        assertTrue(
            bundle.javascript.contains(
                "state.failedAttempts.push",
            ) &&
                bundle.javascript.contains(
                    "processEntries: interruptedProcess",
                ) &&
                bundle.javascript.contains(
                    "exactNextSafeRoute",
                ),
            "terminal failure must retain process history and the canonical continuation route",
        )
        assertTrue(
            bundle.javascript.contains(
                "className = 'thought-toggle failed-attempt-retry'",
            ) &&
                bundle.javascript.contains(
                    "void send({",
                ),
            "failed-attempt presentation must expose an explicit fresh-Send Try Again",
        )
        assertFalse(
            bundle.javascript.contains(
                "if (!ui.draft.value) ui.draft.value = pending.content",
            ),
            "terminal failure must not silently turn an already-attempted Send back into a draft",
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
        assertFalse(
            bundle.javascript.contains(
                "const QUICK_ACTIONS = [",
            ),
            "the renderer must not recreate a second quick-action registry",
        )
        assertFalse(
            bundle.javascript.contains(
                "Journeys",
            ),
            "held Journeys must not be presented as a connected quick action",
        )
        assertFalse(
            bundle.javascript.contains(
                "Evolution",
            ),
            "held Evolution must not be presented as a connected quick action",
        )
        assertTrue(
            bundle.javascript.contains(
                "configureSourceQuickAction",
            ),
            "typed source Controls should be mounted into the Product Spiral",
        )
        assertTrue(bundle.javascript.contains("byId('speech-source')"))
        assertTrue(bundle.javascript.contains("byId('language-source')"))
        assertTrue(bundle.javascript.contains("byId('vision-quick-access-source')"))
        assertTrue(
            bundle.javascript.contains(
                "ui.visionControl?.click();",
            ),
            "VisionQuickAccess should realize its authored LocalInteraction instead of bypassing Vision",
        )
        assertTrue(
            bundle.javascript.contains(
                "if (!event.isTrusted) return;",
            ),
            "synthetic LocalInteraction activation must not be mistaken for outside-click dismissal",
        )
        assertTrue(bundle.javascript.contains("'theme-quick-action'"))
        assertTrue(bundle.javascript.contains("'support-quick-action'"))
        assertTrue(bundle.javascript.contains("https://vgong24.github.io/Vextreme/pages/vex-support.html"))
        assertTrue(bundle.javascript.contains("'quick-actions-trigger'"))
        assertTrue(bundle.javascript.contains("state.quickActionsOpen"))
        assertTrue(bundle.javascript.contains("if (!state.quickActionsOpen) {"))
        assertTrue(bundle.javascript.contains("speech.choosing = false;"))
        assertTrue(bundle.css.contains(".quick-actions[data-open=\"false\"] > .quick-action-item"))
        assertTrue(bundle.css.contains(".quick-action-trigger[aria-expanded=\"true\"]"))
        assertTrue(bundle.html.contains("data-vex-ref=\"${VexLifeWebP1.VoiceInput.identity.ref}\""))
        assertTrue(bundle.html.contains("id=\"voice-waveform\""))
        assertTrue(bundle.html.contains("data-vex-ref=\"${VexLifeWebP1.Stop.identity.ref}\""))
        assertTrue(bundle.javascript.contains(VexLifeProdServiceContract.ConversationCancelPath))
        assertTrue(bundle.javascript.contains("body: JSON.stringify({ idempotencyKey: state.pending.idempotencyKey })"))
        assertTrue(bundle.javascript.contains("ui.send.hidden = state.sendInFlight"))
        assertTrue(bundle.javascript.contains("ui.stop.hidden = !state.sendInFlight"))
        assertTrue(bundle.javascript.contains("workspace.button.conversation-stop"))
        assertTrue(bundle.javascript.contains("workspace.status.conversation-stopping"))
        assertTrue(bundle.javascript.contains("workspace.status.conversation-stopped"))
        assertTrue(bundle.javascript.contains("state.sendCancelRequested = true"))
        assertTrue(bundle.javascript.contains("window.SpeechRecognition"))
        assertTrue(bundle.javascript.contains("recognition.processLocally = true"))
        assertTrue(bundle.javascript.contains("quality: 'dictation'"))
        assertTrue(bundle.javascript.contains("recognition.start(track)"))
        assertTrue(bundle.javascript.contains("navigator.mediaDevices.getUserMedia"))
        assertTrue(bundle.javascript.contains("projectDictationIntoDraft"))
        assertTrue(bundle.javascript.contains("ui.draft.value ="))
        assertTrue(bundle.javascript.contains("if (dictation.desiredActive) void stopVoiceInput(true)"))
        VexLifeVoiceInputFamily.StateRefs.forEach { stateRef ->
            assertTrue(bundle.javascript.contains(stateRef), "VoiceInput State ref must reach VexWeb: $stateRef")
        }
        VexLifeVoiceInputFamily.ControlRefs.forEach { controlRef ->
            assertTrue(bundle.javascript.contains(controlRef), "VoiceInput Control ref must reach VexWeb: $controlRef")
        }
        VexLifeVoiceInputFamily.Text.entries.forEach { text ->
            assertTrue(bundle.javascript.contains(text.key), "VoiceInput text key must reach VexWeb: ${text.key}")
        }
        assertFalse(bundle.javascript.contains("Dictation stays on this device and edits only this draft."))
        assertFalse(bundle.javascript.contains("Listening locally · speech stays on this device."))
        assertFalse(bundle.javascript.contains("Microphone permission is needed for local dictation."))
        assertFalse(
            bundle.html.contains("Start local dictation"),
            "VoiceInput markup must carry keyed identity, not renderer-authored English",
        )
        assertFalse(
            bundle.javascript.contains("window.webkitSpeechRecognition"),
            "VoiceInput must not fall back to the legacy/cloud-capable prefixed recognizer",
        )
        assertFalse(
            bundle.javascript.contains("processLocally = false"),
            "VoiceInput must not silently fall back to remote speech recognition",
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
        // Speech follows the stream and runs Kokoro off the main thread.
        assertTrue(bundle.javascript.contains("followStreamSpeech(true);"))
        assertTrue(bundle.javascript.contains("new Worker(url, { type: 'module' })"))
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

    @Test
    fun vexTextPresentationMapsRepliesAndThinkingWithoutTakingTruthOrScrollOwnership() {
        val bundle = VexLifeWebP1RuntimeArtifacts.generate()
        val javascript = bundle.javascript
        val presentation =
            javascript.substring(
                javascript.indexOf("const markdownElement = "),
                javascript.indexOf("const messageNode = "),
            )

        assertTrue(!presentation.contains("innerHTML"), "TextPresentation builds DOM nodes, never markup")
        assertTrue(presentation.contains("/^(https?:\\/\\/|mailto:)/i"), "only web and mail links are linked")
        assertTrue(
            javascript.contains("bubble.append(textPresentationBody(message.content));"),
            "completed Vex replies should use the shared TextPresentation relationship",
        )
        assertTrue(
            javascript.contains(
                "messageNode({ role: 'vex', content: exchange.vex, sequence: index * 2 + 2, eventRef: vexKey })",
            ),
            "Private Vex replies should keep using the same messageNode/TextPresentation path",
        )
        assertTrue(
            javascript.contains("box.append(textPresentationBody(entry.text));"),
            "human-visible process reasoning should use the same TextPresentation relationship",
        )
        assertTrue(
            javascript.contains("box.append(textPresentationBody(reasoning));"),
            "response-local reasoning fallback should use TextPresentation too",
        )
        assertTrue(
            javascript.contains("paragraph.textContent = String(message.content ?? '');"),
            "the person's message is shown exactly as written",
        )

        // The frame queue is presentation-local and can carry both live reply and live reasoning
        // surfaces without turning either one into the other's scroll owner.
        assertTrue(javascript.contains("const liveTextPresentationQueue = new Map();"))
        assertTrue(
            javascript.contains(
                "scheduleLiveTextPresentation(body, state.streamedContent, () => applyTimelineViewport(false));",
            ),
        )
        assertTrue(
            javascript.contains("applyReasoningViewport(node, viewportKey, true, false);") &&
                javascript.contains("applyTimelineViewport(false);"),
            "reasoning geometry should notify its own viewport and the independent timeline owner",
        )
        assertTrue(
            javascript.contains("applyTextPresentationChildren(container, markdownBlockNodes(text, 0));"),
        )
        assertTrue(
            javascript.contains("applyTextPresentationChildren(current, Array.from(projected.childNodes));"),
        )
        assertTrue(javascript.contains("parent.insertBefore(projected, current);"))
        assertFalse(
            javascript.contains("node.textContent = entry.text"),
            "live reasoning must keep the scrollable viewport mounted instead of replacing it with raw text",
        )
        assertFalse(
            javascript.contains("container.replaceChildren(...markdownBlockNodes(text, 0));"),
            "ordinary streaming must not replace the whole TextPresentation body",
        )
        assertTrue(
            javascript.contains("container.replaceChildren(document.createTextNode(text));"),
            "presentation failure must fail open to exact owner-provided text",
        )

        assertTrue(
            bundle.css.contains(".text-presentation { min-width: 0; white-space: normal; }"),
            "shared Web manifestation styling should be owner-neutral",
        )
        assertTrue(
            bundle.css.contains(".reasoning {") &&
                bundle.css.contains("max-height: 220px; overflow-y: auto;"),
            "the reasoning viewport must remain the independent scroll owner",
        )
    }
}

// [VXG RealForever]
