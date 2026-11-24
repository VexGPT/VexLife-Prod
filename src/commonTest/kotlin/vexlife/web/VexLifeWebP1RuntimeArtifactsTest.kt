package vexlife.web

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vexlife.browser.VexLifeProdBrowserContract
import vexlife.localasr.VexLifeLocalAsrModelsFamily
import vexlife.model.VexLifeModelsFamily
import vexlife.conversation.VexLifeConversationTurnFamily
import vexlife.service.VexLifeProdServiceContract
import vexlife.sources.VexLifeLocalSourcesFamily
import vexlife.workspace.VexLifeWorkspaceBlueprint

class VexLifeWebP1RuntimeArtifactsTest {
    @Test
    fun browserProductRequestsCarryExactPresentationSourceAndReloadWhenServiceMoves() {
        val sourceSha = "a".repeat(40)
        val javascript =
            VexLifeWebP1RuntimeArtifacts
                .generate(
                    presentationSourceSha = sourceSha,
                )
                .javascript

        assertTrue(javascript.contains("presentationSourceSha: \"$sourceSha\""))
        assertTrue(
            javascript.contains(
                "headers.set(service.presentationSourceHeader, service.presentationSourceSha)"
            )
        )
        assertTrue(javascript.contains("value?.code === service.stalePresentationCode"))
        assertTrue(javascript.contains("t('workspace.status.product-reloading')"))
        assertTrue(javascript.contains("window.setTimeout(() => window.location.reload(), 2500)"))
        assertTrue(javascript.contains("const productFetch = async"))
        assertTrue(javascript.contains("const effectFetch = productFetch"))
        assertFalse(
            javascript.contains(
                "fetch(service.",
            ),
            "every browser Product API observation must carry presentation-source currentness",
        )
        assertTrue(javascript.contains("productFetch(service.settingsPath"))
        assertTrue(javascript.contains("productFetch(service.localAsrPath"))
        assertTrue(javascript.contains("productFetch(service.conversationPath"))
        val postEffects =
            Regex("method: 'POST'")
                .findAll(
                    javascript,
                )
                .toList()
        assertTrue(
            postEffects.isNotEmpty(),
        )
        postEffects.forEach {
            match ->
            val prefix =
                javascript.substring(
                    maxOf(
                        0,
                        match.range.first -
                            320,
                    ),
                    match.range.first,
                )
            assertTrue(
                prefix.contains(
                    "effectFetch(",
                ),
                "every browser POST effect must carry presentation-source currentness",
            )
        }
        assertTrue(
            javascript.contains(
                "effectFetch(service.localAsrComposerOutputPath",
            ),
            "Composer-output mutation must use the same source-current effect boundary",
        )
        assertFalse(
            javascript.contains(
                "fetch(service.localAsrComposerOutputPath",
            ),
            "Composer-output mutation must not bypass presentation-source currentness",
        )
    }

    @Test
    fun ContextFailureDoesNotGateIndependentProductSurfaces() {
        val javascript =
            VexLifeWebP1RuntimeArtifacts
                .generate()
                .javascript
        val startupDependencies =
            javascript
                .substringAfter(
                    "void Promise.all([",
                )
                .substringBefore(
                    "])",
                )

        assertFalse(
            startupDependencies.contains(
                "loadContext()",
            ),
            "Context must not gate Models, diagnostics, Memory or other independent startup projections",
        )
        assertTrue(
            javascript.contains(
                "void loadContext().catch((error) =>",
            ),
            "Context should continue as an independent projection after the core presentation is usable",
        )
        assertTrue(
            javascript.contains(
                "t('workspace.status.context-unsupported-detail')",
            ),
            "an unsupported durable Context generation should explain the scoped hold instead of collapsing the Product",
        )
        assertTrue(
            javascript.contains(
                "contextHeldUnsupportedState",
            ),
        )
    }

    @Test
    fun failedAttemptTryAgainUsesCurrentBrowserContextSelection() {
        val javascript =
            VexLifeWebP1RuntimeArtifacts
                .generate()
                .javascript
        val retryBlock =
            javascript
                .substringAfter(
                    "const retryFailed = event.target.closest('.failed-attempt-retry');",
                )
                .substringBefore(
                    "const liveToggle = event.target.closest('.live-process-toggle');",
                )

        assertTrue(
            retryBlock.contains(
                "browserContext: state.includePage ? state.browserContext : null",
            ),
            "Try Again is a new Send and must re-read the current explicit Include-page choice",
        )
        assertFalse(
            retryBlock.contains(
                "failed.browserContext",
            ),
            "a failed attempt must not carry stale page bytes into a fresh Send after browser navigation/currentness changed",
        )
    }

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
                "id=\"page-frame\" data-vex-title=\"workspace.label.browser-viewport-opened-page\" title=\"workspace.label.browser-viewport-opened-page\" referrerpolicy=\"no-referrer\"",
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
                "data-vex-placeholder=\"workspace.placeholder.browser-query\"",
            ) &&
                bundle.html.contains(
                    "placeholder=\"workspace.placeholder.browser-query\"",
                ),
            "browser placeholder should be keyed until the selected-language catalog resolves it",
        )
        assertTrue(
            bundle.javascript.contains(
                "['data-vex-placeholder', 'placeholder']",
            ) &&
                bundle.javascript.contains(
                    "['data-vex-aria-label', 'aria-label']",
                ) &&
                bundle.javascript.contains(
                    "['data-vex-title', 'title']",
                ),
            "static text, placeholder, accessibility and title bindings must share one catalog resolver",
        )
        assertTrue(
            bundle.javascript.contains(
                "t('workspace.placeholder.browser-query-unlocated')",
            ),
            "runtime browser unlocation must not replace the keyed placeholder with English",
        )
        assertTrue(
            bundle.html.contains(
                "data-vex-text=\"workspace.description.browser-viewport-empty\"",
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
                "padding: var(--space-2) 14px 14px",
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
                "workspace.description.context-receipt-next-turn",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "workspace.label.context-inspector-context-provider",
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
                "workspace.placeholder.browser-query-unlocated",
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
                "workspace.status.browser-scheme-unsupported",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "workspace.status.browser-local-service-rejected",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "workspace.description.composer-context-unavailable",
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
                "workspace.status.context-refresh-failed",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "workspace.description.settings-memory-included",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "workspace.description.settings-context-whole-history",
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
        assertTrue(bundle.javascript.contains("const currentSwirlLanguage = () =>"))
        // The application-language owner is current: the Swirl projects the screen language.
        // Each language choice is a typed labelled field; the runtime fills the select inside it,
        // never the field itself (filling the label would replace its title and select).
        for (choice in listOf(VexLifeWebP1.ReplyLanguageChoice, VexLifeWebP1.ScreenLanguageChoice)) {
            assertTrue(
                bundle.javascript.contains("byRef(\"${choice.identity.ref}\")?.querySelector('select')") ||
                    bundle.javascript.contains("byRef('${choice.identity.ref}')?.querySelector('select')"),
                "${choice.identity.ref} is filled through its select",
            )
            assertTrue(
                Regex("""data-vex-ref="${Regex.escape(choice.identity.ref)}"[^>]*>\s*<span[^>]*>[^<]*</span>\s*<select """).containsMatchIn(bundle.html),
                "${choice.identity.ref} wraps exactly one select",
            )
        }
        assertTrue(bundle.javascript.contains("meaning: 'VexLife screen language'"))
        assertTrue(bundle.javascript.contains("state.settings?.screenLanguageShown ?? state.settings?.replyLanguage ?? 'en'"))
        assertTrue(bundle.javascript.contains("const LANGUAGE_FLAGS = Object.freeze({"))
        assertTrue(bundle.javascript.contains("en: '🇺🇸'"))
        assertTrue(bundle.javascript.contains("zh: '🇨🇳'"))
        assertTrue(bundle.javascript.contains("return value.split('-')[0].toLowerCase();"))
        assertTrue(bundle.javascript.contains("action.textContent = languageFlagFor(swirlLanguage.code);"))
        assertTrue(bundle.javascript.contains("t('workspace.label.reply-language') + ' ' + replyName + ' · ' +"))
        assertTrue(bundle.javascript.contains("byId('vision-quick-access-source')"))
        assertTrue(bundle.javascript.contains("byId('release-alerts-source')"))
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
        assertTrue(bundle.javascript.contains("configureSourceGlyphQuickAction"))
        assertTrue(bundle.javascript.contains("configureSourceSupportQuickAction"))
        assertTrue(bundle.javascript.contains("configureSourceQuickActionsTrigger"))
        assertFalse(bundle.javascript.contains("document.createElement('button');\n                button.id = 'theme-quick-action'"))
        assertFalse(bundle.javascript.contains("support.id = 'support-quick-action'"))
        assertTrue(bundle.html.contains("""data-vex-ref="${VexLifeWebP1.Theme.identity.ref}""""))
        assertTrue(bundle.html.contains("""data-vex-ref="${VexLifeWebP1.Support.identity.ref}""""))
        assertTrue(bundle.html.contains("""data-vex-ref="${VexLifeWebP1.QuickActionsTrigger.identity.ref}""""))
        assertTrue(bundle.html.contains("""data-vex-ref="${VexLifeWebP1.SpeechPanel.identity.ref}""""))
        assertTrue(bundle.html.contains("""data-vex-ref="${VexLifeWebP1.LanguagePanel.identity.ref}""""))
        assertTrue(bundle.html.contains("""data-vex-ref="${VexLifeWebP1.ReleaseAlertsPanel.identity.ref}""""))
        for (ref in listOf(
            VexLifeWebP1.SpeechEnabled.identity.ref,
            VexLifeWebP1.SpeechVoice.identity.ref,
            VexLifeWebP1.SpeechReadThoughts.identity.ref,
            VexLifeWebP1.SpeechRate.identity.ref,
            VexLifeWebP1.SpeechPrevious.identity.ref,
            VexLifeWebP1.SpeechPlayPause.identity.ref,
            VexLifeWebP1.SpeechStop.identity.ref,
            VexLifeWebP1.SpeechNext.identity.ref,
            VexLifeWebP1.SpeechChooseStart.identity.ref,
        )) {
            assertTrue(bundle.html.contains("""data-vex-ref="$ref""""))
        }
        assertTrue(bundle.javascript.contains("ui.speech.dataset.open = String(speech.open)"))
        assertTrue(bundle.javascript.contains("ui.languagePanel.dataset.open = String(state.languageOpen)"))
        assertTrue(bundle.javascript.contains("ui.releaseAlertsPanel.dataset.open = String(state.releaseAlertsOpen)"))
        val spiralMount =
            bundle.javascript
                .substringAfter("ui.quickActions.replaceChildren(")
                .substringBefore(");")
        val expectedSwirlOrder =
            listOf(
                "speechAction.item",
                "languageAction.item",
                "visionAction.item",
                "themeAction.item",
                "releaseAlertsAction.item",
                "supportAction.item",
                "triggerAction.item",
            )
        assertTrue(
            expectedSwirlOrder.zipWithNext().all { (before, after) ->
                spiralMount.indexOf(before) in 0 until spiralMount.indexOf(after)
            },
            "expanded Swirl order must stay Speech -> Language -> Vision -> Theme -> Alerts -> Support -> Swirl",
        )
        assertTrue(bundle.javascript.contains("state.quickActionsOpen"))
        assertTrue(bundle.javascript.contains("if (!state.quickActionsOpen) {"))
        assertTrue(bundle.javascript.contains("speech.choosing = false;"))
        assertTrue(
            bundle.javascript.contains(
                "unreadReleaseAlerts && !state.quickActionsOpen",
            ),
            "collapsed Spiral should own release attention",
        )
        assertTrue(
            bundle.javascript.contains(
                "unreadReleaseAlerts && state.quickActionsOpen",
            ),
            "expanded Spiral should move release attention to Alerts",
        )
        assertTrue(bundle.css.contains(".quick-actions[data-open=\"false\"] > .quick-action-item"))
        assertTrue(bundle.css.contains(".quick-action-trigger[aria-expanded=\"true\"]"))

        assertTrue(
            bundle.html.contains(
                "data-vex-text=\"workspace.heading.settings-resources\"",
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
                "workspace.description.resources-summary",
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
                "workspace.description.resources-model-files",
            ),
        )
        assertTrue(
            bundle.html.contains(
                "data-vex-title=\"workspace.description.private-kept\"",
            ) &&
                bundle.html.contains(
                    "data-vex-text=\"workspace.label.conversation-persistence\"",
                ) &&
                bundle.html.contains(
                    "data-vex-text=\"workspace.status.private-kept\"",
                ) &&
                !bundle.html.contains(
                    "class=\"private-dot\"",
                ),
            "Private mode must keep its keyed relationship/state identities without borrowing the runtime-status dot metaphor",
        )
        assertTrue(
            bundle.html.contains(
                "data-vex-text=\"workspace.heading.context-inspector\"",
            ),
        )
        assertTrue(
            bundle.html.contains(
                "id=\"vision-control\"",
            ),
        )
        assertTrue(
            bundle.html.contains(
                "data-vex-text=\"workspace.label.vision\"",
            ),
        )
        assertTrue(
            bundle.html.contains(
                "class=\"vision-state\" data-vex-text=\"workspace.status.vision-checking\"",
            ),
        )
        assertTrue(
            bundle.html.contains(
                "aria-disabled=\"true\" disabled",
            ),
        )
        assertTrue(
            bundle.html.contains(
                "data-vex-title=\"workspace.description.vision-checking\"",
            ) &&
                bundle.html.contains(
                    "data-vex-text=\"workspace.description.vision-checking\"",
                ),
            "Vision checking copy must stay keyed in both title and described status",
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
        assertTrue(bundle.javascript.contains(".filter((voice) => voice.localService)"))

        // VoiceInput is a local drafting path over one selected Local-ASR provider.
        assertTrue(bundle.html.contains("id=\"voice-input\""))
        assertTrue(bundle.html.contains("id=\"voice-waveform\""))
        assertTrue(bundle.html.contains("id=\"local-asr-models-list\""))
        assertTrue(bundle.html.contains("id=\"local-asr-spoken-language\""))
        assertTrue(bundle.html.contains("id=\"local-asr-composer-output\""))
        assertTrue(bundle.html.contains("id=\"local-asr-storage-note\""))
        assertTrue(bundle.javascript.contains(VexLifeProdServiceContract.LocalAsrPath))
        assertTrue(bundle.javascript.contains(VexLifeProdServiceContract.LocalAsrSpokenLanguagePath))
        assertTrue(bundle.javascript.contains(VexLifeProdServiceContract.LocalAsrComposerOutputPath))
        assertTrue(bundle.javascript.contains(VexLifeProdServiceContract.LocalAsrModelControlPath))
        assertTrue(bundle.javascript.contains(VexLifeProdServiceContract.LocalAsrPreparePath))
        assertTrue(bundle.javascript.contains(VexLifeProdServiceContract.LocalAsrTranscribePath))
        assertTrue(bundle.javascript.contains("state.localAsr?.resolvedSpokenLanguageCode"))
        assertTrue(bundle.javascript.contains("composerOutputChoices"))
        assertTrue(bundle.javascript.contains("const selectLocalAsrComposerOutput = async (mode) =>"))
        assertTrue(bundle.javascript.contains("if (language === 'zh-Hans')"))
        assertTrue(bundle.javascript.contains("if (language === 'zh-Hant')"))
        assertTrue(bundle.javascript.contains("const bestPreference = Math.min("))
        assertTrue(bundle.javascript.contains("entry.preference === bestPreference"))
        assertTrue(bundle.javascript.contains("code === 'auto'"))
        assertTrue(bundle.javascript.contains("localAsrAvailableForLanguage"))
        assertTrue(bundle.javascript.contains("const voiceInputSessionOwnsState = () =>"))
        assertTrue(bundle.javascript.contains("const observeVoiceInputCapability = (force = false) =>"))
        assertTrue(bundle.javascript.contains("if (!force && voiceInputSessionOwnsState()) return;"))
        assertTrue(bundle.javascript.contains("observeVoiceInputCapability(true);"))
        assertTrue(bundle.javascript.contains("const prepareLocalAsr = async () =>"))
        assertTrue(bundle.javascript.contains("const providerReady = prepareLocalAsr();"))
        assertTrue(bundle.javascript.contains("await providerReady;"))
        assertTrue(bundle.javascript.contains("const maybeProgressiveTranscription = () =>"))
        assertTrue(bundle.javascript.contains("dictation.progressiveTimer = window.setInterval"))
        assertTrue(bundle.javascript.contains("periodicWhileSpeaking"))
        assertTrue(bundle.javascript.contains("refineThinkingPause"))
        assertTrue(bundle.javascript.contains("silenceAfterResult >= 900"))
        assertTrue(bundle.javascript.contains("silenceAfterResult >= 2600"))
        assertTrue(bundle.javascript.contains("dictation.stableHypothesisPasses >= 2"))
        assertTrue(bundle.javascript.contains("dictation.interimTranscript = transcript"))
        assertTrue(bundle.javascript.contains("clearCapturedPcm(true)"))
        assertTrue(bundle.javascript.contains("dictation.speechSinceLastCommit"))
        assertTrue(bundle.javascript.contains("'x-vexlife-language': language.code"))
        assertTrue(
            bundle.javascript.contains(
                VexLifeProdServiceContract.LocalAsrDictationPassHeader,
            ),
        )
        assertTrue(bundle.javascript.contains("for (const control of option.controls ?? [])"))
        assertTrue(bundle.javascript.contains(VexLifeLocalAsrModelsFamily.ControlUse))
        assertTrue(bundle.javascript.contains(VexLifeLocalAsrModelsFamily.ControlRemove))
        assertFalse(
            bundle.javascript.contains(
                "const code = state.settings?.replyLanguage ?? voiceInput.defaultLanguage;",
            ),
            "microphone language must not stay permanently coupled to Vex reply language",
        )
        assertTrue(
            bundle.javascript.contains(
                "Local ASR currentness is independent from Conversation/Context/Release startup",
            ),
        )

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
                "workspace.description.conversation-empty",
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
        assertTrue(
            bundle.javascript.contains(
                "navigator.mediaDevices.getUserMedia",
            ),
            "VoiceInput may use the microphone API; Vision still must not use browser screen capture",
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
    fun modelSettingsKeepSelectionConfigurationAndActivationAsSeparateControls() {
        val bundle =
            VexLifeWebP1RuntimeArtifacts.generate()
        val javascript =
            bundle.javascript

        assertTrue(
            bundle.html.contains(
                "id=\"model-choice\"",
            ),
        )
        assertTrue(
            bundle.html.contains(
                "<select id=\"model-prompt-cache-entries\">",
            ),
            "three discrete performance levels should be presented as named choices, not a numeric slider",
        )
        assertTrue(
            bundle.html.contains(
                "id=\"model-start\"",
            ),
        )
        assertTrue(
            bundle.html.contains(
                "class=\"model-start-button\"",
            ),
            "runtime activation must have its own visually primary presentation",
        )
        assertTrue(
            bundle.html.contains(
                "id=\"model-choice-facts\"",
            ) &&
                bundle.html.contains(
                    "id=\"model-choice-notes\"",
                ),
            "the selected target should carry one compact summary instead of one card per model",
        )
        val modelsRenderer =
            javascript
                .substringAfter(
                    "const renderModels = () => {",
                )
                .substringBefore(
                    "const localAsrSelectionValue =",
                )
        assertFalse(
            modelsRenderer.contains(
                "row.className = 'model-row'",
            ),
            "the old large Models per-model card renderer must not compete with the dropdown",
        )
        assertTrue(
            javascript.contains(
                "row.className = 'model-storage-row'",
            ) &&
                javascript.contains(
                    "models.filter((model) => model.presentBytes > 0 || model.imported)",
                ) &&
                javascript.contains(
                    "modelControl(control.controlRef, model.handle)",
                ),
            "storage controls must remain bound to each model's projected lawfulness instead of only the dropdown target",
        )
        assertFalse(
            javascript.contains(
                "workspace.label.model-cache-level-1",
            ) ||
                javascript.contains(
                    "workspace.label.model-cache-level-2",
                ) ||
                javascript.contains(
                    "workspace.label.model-cache-level-3",
                ),
            "VexWeb must not own the human meaning of runtime-configuration values",
        )
        assertTrue(
            javascript.contains(
                "modelControl(\"${VexLifeModelsFamily.ControlSelect}\", handle)",
            ),
            "choosing a target must return the Select Control rather than start it",
        )
        assertTrue(
            javascript.contains(
                "choice.controlRef",
            ) &&
                javascript.contains(
                    "choice.promptCacheEntries",
                ),
            "the performance choice must return the Control projected by Models DisplayState",
        )
        assertTrue(
            javascript.contains(
                "modelControl(startControl.controlRef, selected.handle)",
            ),
            "explicit Start must return the activation Control projected by Models DisplayState",
        )
        assertFalse(
            javascript.contains(
                "modelChoice.onchange = () => { void modelControl(\"${VexLifeModelsFamily.ControlUse}\"",
            ),
            "target selection must never imply runtime activation",
        )
        assertFalse(
            javascript.contains("value === 1 ?") ||
                javascript.contains("value > 1") ||
                javascript.contains("selected.serving && selected.runtimeConfigurationPending"),
            "VexWeb must render Models DisplayState instead of reconstructing performance or activation meaning",
        )
        assertTrue(
            javascript.contains("display.runtimeConfigurationChoices") &&
                javascript.contains("[resolveText(choice.label), resolveText(choice.standing)]") &&
                javascript.contains(".filter(Boolean)") &&
                javascript.contains("resolveText(display.runtimeConfigurationSummary)") &&
                javascript.contains("resolveText(startControl.label)"),
            "runtime configuration and optional standing wording must arrive through the Models projection",
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
                "const ephemeralConversationSurface = ({",
            ) &&
                javascript.contains(
                    "const activeConversationSurface = () => {",
                ) &&
                javascript.contains(
                    "const nodes = surface.messages.map(messageNode);",
                ),
            "canonical, Private, and Lab conversations should reuse one conversation-surface renderer and messageNode/TextPresentation path",
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

    @Test
    fun vexLabsNativeTestExposesFieldLabAndKeepsScratchConversationSeparate() {
        val bundle =
            VexLifeWebP1RuntimeArtifacts.generate()
        val html =
            bundle.html
        val javascript =
            bundle.javascript

        assertTrue(
            html.contains(
                "id=\"vex-labs-fields\"",
            ),
        )
        assertTrue(
            html.contains(
                "data-vex-text=\"workspace.heading.vex-labs\"",
            ),
            "Lab heading should enter static presentation as a localization identity",
        )
        assertTrue(
            html.indexOf(
                "data-context-kind=\"vex-labs\"",
            ) <
                html.indexOf(
                    "data-context-kind=\"foundation\"",
                ),
            "the Labs sibling surface may appear above canonical input without becoming one of the components Vex received",
        )
        assertTrue(
            html.contains(
                "data-vex-text=\"workspace.label.companion-mode-conversation\"",
            ) &&
                html.contains(
                    "data-vex-text=\"workspace.label.companion-mode-labs\"",
                ),
            "Companion mode choices should remain keyed presentation labels",
        )
        assertTrue(
            javascript.contains(
                "vexLabsPath: \"${VexLifeProdServiceContract.VexLabsPath}\"",
            ),
        )
        assertTrue(
            javascript.contains(
                "state.companionMode = 'LAB'",
            ),
        )
        assertTrue(
            javascript.contains(
                "const activeConversationController = () => {",
            ) &&
                javascript.contains(
                    "const submitActiveConversation = () => {",
                ) &&
                javascript.contains(
                    "submitActiveConversation();",
                ),
            "the composer should reuse one conversation controller rather than branching its Send surface per screen",
        )
        assertTrue(
            javascript.contains(
                "workspace.description.vex-labs-scratch-boundary",
            ),
        )
        assertTrue(
            javascript.contains(
                "workspace.status.vex-labs-saved-supported",
            ),
            "saved Lab feedback should resolve through the workspace catalog",
        )
        assertTrue(
            javascript.contains(
                "result.realConversationMutationPerformed || result.realMemoryMutationPerformed",
            ),
            "the presentation should fail closed if the Lab ever reports mutation of real state",
        )
        assertTrue(
            javascript.contains(
                "const vexLabsDraftMatchesLastTest = () => {",
            ) &&
                javascript.contains(
                    "workspace.status.vex-labs-test-before-save",
                ),
            "Save supported settings must be gated on one completed Lab turn with the exact current draft",
        )
        assertTrue(
            javascript.contains(
                "const vexLabsAdmissionText = (admission) => {",
            ) &&
                javascript.contains(
                    "result.state === 'HELD_CONTEXT_BUDGET'",
                ) &&
                javascript.contains(
                    "ui.draft.value = content;",
                ) &&
                javascript.contains(
                    "workspace.label.vex-labs-field-observed-floor",
                ),
            "a context-budget hold must keep the person's message available and explain the observed Prompt admission floor instead of becoming a generic failed Conversation",
        )
    }

    @Test
    fun companionEngagementProjection() {
        val bundle = VexLifeWebP1RuntimeArtifacts.generate()
        assertTrue(bundle.html.contains("<option value=\"PRIVATE\" data-vex-text=\"workspace.status.private-active\">workspace.status.private-active</option>"))
        assertTrue(bundle.html.contains("id=\"private-mode\" hidden"))
        assertTrue(bundle.html.contains("id=\"vision-control\" hidden"))
        assertTrue(bundle.html.contains("id=\"vision-quick-access-source\""))
    }

    @Test
    fun companionEngagementControlRouting() {
        val bundle = VexLifeWebP1RuntimeArtifacts.generate()
        assertTrue(bundle.javascript.contains("requested === 'PRIVATE'") && bundle.javascript.contains("await togglePrivate();"))
        assertTrue(
            bundle.javascript.contains("display ? resolveText(display.label) : t('workspace.button.vision-quick-access')") &&
                bundle.javascript.contains("visionButton.disabled = !bound || state.visionInFlight;"),
        )
        assertFalse(bundle.javascript.contains("'Vision · Held'"))
    }

    @Test
    fun historicalSentResourcesStaySeparateFromHumanWordsAndReviewLazily() {
        val bundle =
            VexLifeWebP1RuntimeArtifacts.generate()
        val javascript =
            bundle.javascript
        val css =
            bundle.css

        assertTrue(
            javascript.contains(
                "conversationResourceMetadataPath: \"/api/v1/conversation/resource/metadata\"",
            ) &&
                javascript.contains(
                    "conversationResourceContentPath: \"/api/v1/conversation/resource/content\"",
                ),
            "historical review must use the bounded Product service routes",
        )
        assertTrue(
            javascript.contains(
                "headers.set(service.conversationResourceEventRefHeader, String(message.eventRef ?? ''));",
            ) &&
                javascript.contains(
                    "headers.set(service.conversationResourceEventHashHeader, String(message.eventSha256 ?? ''));",
                ) &&
                javascript.contains(
                    "headers.set(service.conversationResourceSequenceHeader, String(Number(message.sequence) || 0));",
                ) &&
                javascript.contains(
                    "headers.set(service.conversationResourceRefHeader, String(resource.resourceRef ?? ''));",
                ),
            "review must bind the exact canonical message and request-local resource identity",
        )
        assertTrue(
            javascript.contains(
                "paragraph.textContent = String(message.content ?? '');",
            ) &&
                javascript.contains(
                    "const resources = sentResourcesNode(message);",
                ) &&
                javascript.contains(
                    "if (resources) bubble.append(resources);",
                ),
            "resource history must remain a sibling projection beneath the exact human words",
        )
        assertTrue(
            javascript.contains(
                "Array.isArray(message?.resources)",
            ) &&
                javascript.contains(
                    "name.textContent = String(resource.displayName ?? '');",
                ) &&
                javascript.contains(
                    "detail.hidden = true;",
                ),
            "binding truth must be visible as the exact sent display name before any local-custody inspection runs",
        )
        assertTrue(
            javascript.contains(
                "service.conversationResourceMetadataPath",
            ) &&
                javascript.contains(
                    "service.conversationResourceContentPath",
                ) &&
                javascript.contains(
                    "{ method: 'GET', headers }",
                ),
            "metadata/content inspection must remain lazy behind Product fetch currentness",
        )
        assertTrue(
            javascript.contains(
                "if (metadata?.status) return resolveText(metadata.status);",
            ) &&
                javascript.contains(
                    VexLifeProdServiceContract.ConversationResourceReviewFailedTextKey,
                ),
            "custody/review copy must resolve through VexLifeText/string-key identity rather than renderer-authored English",
        )
        listOf(
            "Sent with this message",
            "Checking local copy",
            "local copy unavailable",
            "local copy needs attention",
            "Could not review this local copy",
            "Preview of ",
        ).forEach {
            forbidden ->
            assertFalse(
                javascript.contains(
                    forbidden,
                ),
                "sent-resource renderer must not hardcode human copy: $forbidden",
            )
        }
        assertTrue(
            javascript.contains(
                "URL.createObjectURL(blob)",
            ) &&
                javascript.contains(
                    "image.loading = 'lazy';",
                ) &&
                javascript.contains(
                    "const maximumPreviewCharacters = 6000;",
                ),
            "image and text review should remain bounded presentation work",
        )
        assertTrue(
            css.contains(
                ".sent-resources {",
            ) &&
                css.contains(
                    "display: flex; flex-wrap: wrap;",
                ) &&
                css.contains(
                    ".sent-resource {",
                ) &&
                css.contains(
                    "min-width: min(180px, 100%);",
                ) &&
                css.contains(
                    "@media (max-width: 520px)",
                ),
            "sent-resource cards must wrap without creating horizontal overflow",
        )
        assertTrue(
            css.contains(
                ".sent-resource-thumbnail",
            ) &&
                css.contains(
                    ".sent-resource-text-preview",
                ),
            "review presentation should support bounded image and readable-text previews",
        )
    }


    @Test
    fun conversationStopIsProjectedFromTheConversationTurnFamily() {
        val bundle = VexLifeWebP1RuntimeArtifacts.generate()
        assertTrue(bundle.html.contains("data-vex-ref=\"${VexLifeWebP1.Stop.identity.ref}\""))
        assertTrue(bundle.javascript.contains(VexLifeProdServiceContract.ConversationTurnPath))
        assertTrue(bundle.javascript.contains(VexLifeProdServiceContract.ConversationCancelPath))
        assertTrue(bundle.javascript.contains("body: JSON.stringify({ attemptRef })"))
        assertTrue(
            bundle.javascript.contains(
                "const turnDisplay = canonicalNow ? (state.conversationTurn?.display ?? null) : null"
            ),
            "only the canonical composer projects the ConversationTurn Family",
        )
        assertTrue(bundle.javascript.contains("ui.stop.hidden = !(turnActive || turnCancelling)"))
        assertTrue(bundle.javascript.contains("await loadConversationTurn();"))
        assertTrue(bundle.javascript.contains("workspace.button.conversation-send"))
        assertTrue(bundle.javascript.contains("workspace.button.conversation-stop"))
        assertFalse(
            bundle.javascript.contains("workspace.status.conversation-stopping"),
            "renderer must not re-author the Family's Cancelling status key",
        )
        VexLifeConversationTurnFamily.ControlRefs.forEach { controlRef ->
            assertTrue(
                bundle.javascript.contains(controlRef),
                "ConversationTurn Control ref must reach VexWeb: $controlRef",
            )
        }
    }
    @Test
    fun firstLocalStartStatusExplainsTheOneInitialLocalTurnWithoutOwningSend() {
        val bundle =
            VexLifeWebP1RuntimeArtifacts.generate(
                presentationSourceSha =
                    "a".repeat(
                        40,
                    ),
            )

        assertTrue(
            bundle.html.contains(
                "data-vex-ref=\"" +
                    VexLifeWorkspaceBlueprint.ResponseProgress.ref +
                    "\"",
            ),
            "first local start reuses the existing ResponseProgress coordinate",
        )
        assertTrue(
            bundle.html.contains(
                "id=\"response-progress\"",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "initialPromptPreparationPendingForNextTurn",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "initialPromptPreparationForActiveTurn",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "workspace.heading.first-local-start",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "workspace.description.first-local-start-ready-to-send",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "workspace.description.first-local-start-sent",
            ),
        )
        assertTrue(
            bundle.css.contains(
                ".response-progress[data-state=\"FirstLocalStart\"]",
            ),
        )
        assertTrue(
            bundle.css.contains(
                "var(--status-caution)",
            ),
            "first-local-start explanation uses the existing evidentiary caution token",
        )
        assertFalse(
            bundle.javascript.contains(
                "FirstLocalStartStatus",
            ),
            "first-local-start explanation must not create a second Blueprint/VexWeb coordinate",
        )
        assertFalse(
            bundle.javascript.contains(
                "READY_REUSABLE",
            ),
            "provider-cache state vocabulary must not leak into the browser presentation",
        )
    }

    @Test
    fun localArtifactDraftIsServiceProjectedAndNeverSerializedIntoCanonicalSend() {
        val sourceSha =
            "a".repeat(
                40,
            )
        val bundle =
            VexLifeWebP1RuntimeArtifacts.generate(
                presentationSourceSha = sourceSha,
            )
        val javascript =
            bundle.javascript

        assertTrue(
            bundle.html.contains(
                "data-vex-ref=\"" +
                    VexLifeWorkspaceBlueprint.Attach.ref +
                    "\"",
            ),
        )
        assertTrue(
            bundle.html.contains(
                "data-vex-ref=\"" +
                    VexLifeWorkspaceBlueprint.ComposerAttachments.ref +
                    "\"",
            ),
        )
        assertTrue(
            javascript.contains(
                "const loadAttachmentDraft = async",
            ),
        )
        assertTrue(
            javascript.contains(
                "new XMLHttpRequest()",
            ),
            "binary upload progress/cancellation must not be simulated through a finished fetch",
        )
        assertTrue(
            javascript.contains(
                "xhr.setRequestHeader(service.presentationSourceHeader, service.presentationSourceSha)",
            ),
            "binary staging is a Product effect and must carry presentation-source currentness",
        )
        assertTrue(
            javascript.contains(
                "effectFetch(service.attachmentControlPath",
            ),
            "Cancel/Remove must pass through the ordinary source-current effect boundary",
        )

        val sendBlock =
            javascript
                .substringAfter(
                    "const send = async",
                )
                .substringBefore(
                    "const sizeDraft =",
                )
        assertFalse(
            sendBlock.contains(
                "attachmentRefs",
            ),
            "browser Send must not author artifact/request-resource identity",
        )
        assertFalse(
            sendBlock.contains(
                "artifactRef",
            ),
            "browser Send must not author durable artifact identity",
        )
        assertFalse(
            sendBlock.contains(
                "projectionRef",
            ),
            "browser Send must not author derived-projection identity",
        )
        assertTrue(
            sendBlock.contains(
                "body: JSON.stringify(pending)",
            ),
        )
        assertTrue(
            javascript.contains(
                "state.attachmentDraft?.display?.requestResourcesReady === false",
            ),
            "Family projection must hold Send while a selected file is staging/held",
        )
        assertTrue(
            javascript.contains(
                "if (state.attachmentUploads.size > 0) return;",
            ),
        )
    }

    @Test
    fun localSourcesUseTheHostFolderPickerAndProjectFamilyControlsWithoutAPathTextbox() {
        val bundle =
            VexLifeWebP1RuntimeArtifacts.generate(
                presentationSourceSha =
                    "a".repeat(
                        40,
                    ),
            )
        val javascript =
            bundle.javascript

        assertTrue(
            bundle.html.contains(
                "data-vex-ref=\"" +
                    VexLifeWorkspaceBlueprint.SettingsLocalSources.ref +
                    "\"",
            ),
        )
        assertTrue(
            bundle.html.contains(
                "data-vex-ref=\"" +
                    VexLifeWorkspaceBlueprint.LocalSourcesAdd.ref +
                    "\"",
            ),
        )
        assertFalse(
            bundle.html.contains(
                "local-sources-path",
            ),
            "ordinary Local Sources Add must use the host folder picker, not ask the person for an absolute path",
        )
        assertTrue(
            javascript.contains(
                "const loadLocalSources = async",
            ),
        )
        assertTrue(
            javascript.contains(
                "effectFetch(service.localSourcesControlPath",
            ),
            "Local Sources mutation must cross the source-current Product effect boundary",
        )
        assertTrue(
            javascript.contains(
                "body: JSON.stringify({ controlRef, sourceRef })",
            ),
        )
        assertTrue(
            javascript.contains(
                "void applyLocalSourcesControl(controlRef);",
            ),
            "Add folder must send only the Family Control; the host picker supplies the selected path",
        )
        assertFalse(
            javascript.contains(
                "localSourcesPath.value",
            ),
        )
        assertTrue(
            javascript.contains(
                VexLifeLocalSourcesFamily.ControlAddFolder,
            ),
        )
        assertTrue(
            javascript.contains(
                VexLifeLocalSourcesFamily.ControlRemove,
            ),
        )
    }

}
// [VXG RealForever]
