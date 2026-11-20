package vexlife.web

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vexlife.browser.VexLifeProdBrowserContract
import vexlife.localasr.VexLifeLocalAsrModelsFamily
import vexlife.model.VexLifeModelsFamily
import vexlife.service.VexLifeProdServiceContract
import vexlife.workspace.VexLifeWorkspaceBlueprint

class VexLifeWebP1RuntimeArtifactsTest {
    @Test
    fun browserEffectsCarryExactPresentationSourceAndReloadWhenServiceMoves() {
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
        assertTrue(javascript.contains("Reloading automatically"))
        assertTrue(javascript.contains("window.setTimeout(() => window.location.reload(), 2500)"))
        assertTrue(javascript.contains("const effectFetch = async"))
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
                "Conversation, Models, diagnostics, and other independent surfaces remain available.",
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
    fun contextProfileReachCadenceIsEditableWithoutResettingOtherProfileSettings() {
        val bundle =
            VexLifeWebP1RuntimeArtifacts.generate()

        assertTrue(
            bundle.html.contains(
                "id=\"context-profile-capability-request-block-size\"",
            ),
        )
        assertTrue(
            bundle.html.contains(
                "at most three blocks are admitted in one turn",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "schemaVersion: 'vexlife-prod.context-profile/v3'",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "inputTokenBudget: activeDefinition.inputTokenBudget ?? null",
            ),
            "editing capability cadence must not reset a separately saved active-context budget",
        )
        assertTrue(
            bundle.javascript.contains(
                "capabilityRequestBlockSize",
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
        assertTrue(bundle.javascript.contains("const currentSwirlLanguage = () =>"))
        assertTrue(bundle.javascript.contains("meaning: 'Vex reply language'"))
        assertTrue(bundle.javascript.contains("const LANGUAGE_FLAGS = Object.freeze({"))
        assertTrue(bundle.javascript.contains("en: '🇺🇸'"))
        assertTrue(bundle.javascript.contains("zh: '🇨🇳'"))
        assertTrue(bundle.javascript.contains("return value.split('-')[0].toLowerCase();"))
        assertTrue(bundle.javascript.contains("action.textContent = languageFlagFor(swirlLanguage.code);"))
        assertTrue(bundle.javascript.contains("action.title = swirlLanguage.meaning + ': ' + swirlLanguage.name;"))
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
                "supportItem",
                "triggerItem",
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
                javascript.contains("resolveText(display.runtimeConfigurationSummary)") &&
                javascript.contains("resolveText(startControl.label)"),
            "runtime configuration and activation wording must arrive through the Models projection",
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
        val thinkingIndex =
            html.indexOf(
                "id=\"vex-labs-thinking\"",
            )
        val reasoningDepthIndex =
            html.indexOf(
                "id=\"vex-labs-reasoning-effort\"",
            )
        val activeContextIndex =
            html.indexOf(
                "id=\"vex-labs-input-budget\"",
            )
        assertTrue(
            thinkingIndex >=
                0 &&
                reasoningDepthIndex >
                thinkingIndex &&
                activeContextIndex >
                reasoningDepthIndex,
            "VexContext quick configuration should put Thinking and reasoning depth ahead of deeper context-budget controls",
        )
        assertTrue(
            html.contains(
                "Vex Labs Native Test",
            ),
        )
        assertTrue(
            html.indexOf(
                "data-context-kind=\"vex-labs\"",
            ) <
                html.indexOf(
                    "data-context-kind=\"foundation\"",
                ),
            "the Lab field surface should appear above Foundation in What Vex received",
        )
        assertTrue(
            html.contains(
                "<option value=\"CONVERSATION\">Converse with Vex</option>",
            ) &&
                html.contains(
                    "<option value=\"LAB\">Vex Labs Test</option>",
                ),
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
                "Scratch Lab only. Your real Conversation and Memory are not written by Lab turns.",
            ),
        )
        assertTrue(
            javascript.contains(
                "Thinking, reasoning depth, and Memory prompt depth remain Lab-only until their own durable Product Controls are qualified.",
            ),
        )
        assertTrue(
            javascript.contains(
                "thinkingEnabled: Boolean(draft.thinkingEnabled)",
            ) &&
                javascript.contains(
                    "draft.thinkingEnabled && draft.reasoningEffort != null",
                ) &&
                javascript.contains(
                    "This pinned model/provider exposes no qualified reasoning-depth tier.",
                ),
            "the VexContext controls must alter the actual Lab request and remain model-aware",
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
                    "Test this exact Lab configuration before saving supported settings.",
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
                    "Last held turn observed floor",
                ),
            "a context-budget hold must keep the person's message available and explain the observed Prompt admission floor instead of becoming a generic failed Conversation",
        )
    }
}

// [VXG RealForever]
