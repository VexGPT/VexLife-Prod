package vexlife.web

import vexlife.browser.VexLifeProdBrowserContract
import vexlife.service.VexLifeProdServiceContract
import vexlife.voiceinput.VexLifeVoiceInputFamily
import vexlife.workspace.VexLifeSourceTemplate
import vexlife.workspace.VexLifeWorkspaceApp

/**
 * Current VexLife-Prod sequencing beneath the canonical A003 presentation contract.
 *
 * Presentation contract:
 * - ordinary Enter sends;
 * - Shift+Enter inserts a newline;
 * - the human turn appears immediately as in-flight presentation state;
 * - durable Conversation remains authoritative and replaces the in-flight projection on commit;
 * - browser Context inclusion is explicit;
 * - current Send idempotency / Memory / working-continuity / runtime owners remain untouched.
 */
object VexLifeWebP1RuntimeArtifacts {
    fun generate(): VexWebArtifacts.Bundle {
        val semantic =
            VexWebArtifacts.generate(
                VexLifeWebP1.Surface,
            )

        return semantic.copy(
            html =
                VexLifeWebP1ExperienceMarkup.render(),
            css =
                VexLifeWebP1ExperienceStyle.render(),
            javascript =
                runtimeJavascript().trimIndent() +
                    "\n",
        )
    }

    private fun runtimeJavascript(): String {
        val browserCompanionSeam =
            VexLifeWebP1.BodyFrame.seams
                .filterIsInstance<VexWeb.Seam.Vertical>()
                .single {
                    it.ref ==
                        VexLifeWebP1.Refs.BrowserCompanionSeam
                }
        val allocation =
            browserCompanionSeam.allocation as
                VexWeb.Parameter.Allocation.Ratio
        val authoredBrowserShare =
            allocation.beforeParts.toDouble() /
                allocation.totalParts.toDouble()
        fun jsList(
            values: List<String>,
        ): String =
            values.joinToString(
                separator = ",",
                prefix = "[",
                postfix = "]",
            ) {
                jsString(
                    it,
                )
            }

        val humanPageSchemes =
            jsList(
                VexLifeProdBrowserContract.HumanPageSchemes,
            )
        val rejectedHumanPageHosts =
            jsList(
                VexLifeProdBrowserContract.RejectedHumanPageHosts,
            )
        val admittedHosts =
            jsList(
                VexLifeProdBrowserContract.AdmittedHosts,
            )
        val workspaceApps = jsList(VexLifeWorkspaceApp.entries.map { it.key })
        val sourceTemplateScopes = jsList(VexLifeSourceTemplate.Members.map { it.scope })

        fun jsNullable(value: String?): String = value?.let(::jsString) ?: "null"

        fun voiceDisplay(state: VexLifeVoiceInputFamily.State): String {
            val display = VexLifeVoiceInputFamily.displayStateFor(state)
            return buildString {
                append("Object.freeze({statusKey:")
                append(jsString(display.status.key))
                append(",primaryControlRef:")
                append(jsNullable(display.primaryControlRef))
                append(",primaryLabelKey:")
                append(jsNullable(display.primaryLabel?.key))
                append(",active:")
                append(display.active)
                append(",attention:")
                append(jsString(display.attention.name))
                append("})")
            }
        }

        val voiceLanguage =
            VexLifeVoiceInputFamily.Language.entries.joinToString(
                separator = ",",
                prefix = "{",
                postfix = "}",
            ) {
                jsString(it.personLanguageCode) + ":" + jsString(it.recognitionLocale)
            }
        val voiceExampleLanguage = VexLifeVoiceInputFamily.Language.English
        val voiceStateDisplays =
            listOf(
                VexLifeVoiceInputFamily.StateReady to
                    voiceDisplay(VexLifeVoiceInputFamily.Ready(voiceExampleLanguage)),
                VexLifeVoiceInputFamily.StateCheckingLanguage to
                    voiceDisplay(VexLifeVoiceInputFamily.CheckingLanguage(voiceExampleLanguage)),
                VexLifeVoiceInputFamily.StateInstallingLanguage to
                    voiceDisplay(VexLifeVoiceInputFamily.InstallingLanguage(voiceExampleLanguage)),
                VexLifeVoiceInputFamily.StateRequestingMicrophone to
                    voiceDisplay(VexLifeVoiceInputFamily.RequestingMicrophone(voiceExampleLanguage)),
                VexLifeVoiceInputFamily.StateListening to
                    voiceDisplay(VexLifeVoiceInputFamily.Listening(voiceExampleLanguage)),
                VexLifeVoiceInputFamily.StateStopping to
                    voiceDisplay(VexLifeVoiceInputFamily.Stopping(voiceExampleLanguage)),
            ).joinToString(separator = ",", prefix = "{", postfix = "}") { (stateRef, display) ->
                jsString(stateRef) + ":" + display
            }
        val voiceHoldDisplays =
            VexLifeVoiceInputFamily.HoldReason.entries.joinToString(
                separator = ",",
                prefix = "{",
                postfix = "}",
            ) {
                jsString(it.reasonRef) + ":" +
                    voiceDisplay(VexLifeVoiceInputFamily.Held(it))
            }
        val voiceFailureDisplays =
            VexLifeVoiceInputFamily.FailureReason.entries.joinToString(
                separator = ",",
                prefix = "{",
                postfix = "}",
            ) {
                jsString(it.reasonRef) + ":" +
                    voiceDisplay(VexLifeVoiceInputFamily.Failed(voiceExampleLanguage, it))
            }

        return """
            // Generated VexLife-Prod A003-faithful presentation over current New-Beginnings owners.
            (() => {
              const root = document.getElementById('workspace');
              if (!root) return;

              const service = Object.freeze({
                sourceCurrentnessPath: ${jsString(VexLifeProdServiceContract.SourceCurrentnessPath)},
                currentStatePath: ${jsString(VexLifeProdServiceContract.CurrentStatePath)},
                conversationPath: ${jsString(VexLifeProdServiceContract.ConversationPath)},
                contextPath: ${jsString(VexLifeProdServiceContract.ContextPath)},
                memoryPath: ${jsString(VexLifeProdServiceContract.MemoryPath)},
                settingsPath: ${jsString(VexLifeProdServiceContract.SettingsPath)},
                privatePath: ${jsString(VexLifeProdServiceContract.PrivatePath)},
                privateControlPath: ${jsString(VexLifeProdServiceContract.PrivateControlPath)},
                privateSendPath: ${jsString(VexLifeProdServiceContract.PrivateSendPath)},
                privateNoteProposePath: ${jsString(VexLifeProdServiceContract.PrivateNoteProposePath)},
                privateNoteKeepPath: ${jsString(VexLifeProdServiceContract.PrivateNoteKeepPath)},
                privateNoteDeclinePath: ${jsString(VexLifeProdServiceContract.PrivateNoteDeclinePath)},
                visionPath: ${jsString(VexLifeProdServiceContract.VisionPath)},
                visionControlPath: ${jsString(VexLifeProdServiceContract.VisionControlPath)},
                diagnosticsPath: ${jsString(VexLifeProdServiceContract.DiagnosticsPath)},
                diagnosticsControlPath: ${jsString(VexLifeProdServiceContract.DiagnosticsControlPath)},
                modelsPath: ${jsString(VexLifeProdServiceContract.ModelsPath)},
                modelsControlPath: ${jsString(VexLifeProdServiceContract.ModelsControlPath)},
                chronologyPath: ${jsString(VexLifeProdServiceContract.ChronologyPath)},
                workspaceStringsPath: ${jsString(VexLifeProdServiceContract.WorkspaceStringsPath)},
                sendPath: ${jsString(VexLifeProdServiceContract.ConversationSendPath)},
                cancelPath: ${jsString(VexLifeProdServiceContract.ConversationCancelPath)}
              });
              const apps = Object.freeze({
                keys: $workspaceApps,
                sourceTemplate: ${jsString(VexLifeWorkspaceApp.SourceTemplate.key)},
                archives: ${jsString(VexLifeWorkspaceApp.Archives.key)},
                browser: ${jsString(VexLifeWorkspaceApp.Browser.key)},
                sourceTemplateScopes: $sourceTemplateScopes,
                storageKey: 'vexlife-prod.presentation.app'
              });
              const browser = Object.freeze({
                initialPageUrl: ${jsString(VexLifeProdBrowserContract.InitialPageUrl)},
                humanPageSchemes: $humanPageSchemes,
                rejectedHumanPageHosts: $rejectedHumanPageHosts,
                rejectedHumanPageHostSuffix: ${jsString(VexLifeProdBrowserContract.RejectedHumanPageHostSuffix)},
                admittedHosts: $admittedHosts,
                frameHostSuffix: ${jsString(VexLifeProdBrowserContract.FrameHostSuffix)},
                frameHostPort: ${VexLifeProdBrowserContract.FrameHostPort},
                contextCharacterLimit: ${VexLifeProdBrowserContract.ContextCharacterLimit},
                maxTitleCharacters: ${VexLifeProdBrowserContract.MaxTitleCharacters},
                captureRendered: ${jsString(VexLifeProdBrowserContract.CaptureRendered)},
                messageType: 'vexlife.browser-context/v1',
                authoredBrowserShare: $authoredBrowserShare
              });
              // Serialization boundary for the Kotlin VoiceInput Family. Browser code realizes
              // Effects but does not invent Product State, Control, reason refs or display keys.
              const voiceInput = Object.freeze({
                state: Object.freeze({
                  ready: ${jsString(VexLifeVoiceInputFamily.StateReady)},
                  checkingLanguage: ${jsString(VexLifeVoiceInputFamily.StateCheckingLanguage)},
                  installingLanguage: ${jsString(VexLifeVoiceInputFamily.StateInstallingLanguage)},
                  requestingMicrophone: ${jsString(VexLifeVoiceInputFamily.StateRequestingMicrophone)},
                  listening: ${jsString(VexLifeVoiceInputFamily.StateListening)},
                  stopping: ${jsString(VexLifeVoiceInputFamily.StateStopping)},
                  held: ${jsString(VexLifeVoiceInputFamily.StateHeld)},
                  failed: ${jsString(VexLifeVoiceInputFamily.StateFailed)}
                }),
                control: Object.freeze({
                  start: ${jsString(VexLifeVoiceInputFamily.ControlStart)},
                  stop: ${jsString(VexLifeVoiceInputFamily.ControlStop)}
                }),
                effect: Object.freeze({
                  beginLocalDictation: ${jsString(VexLifeVoiceInputFamily.EffectBeginLocalDictation)},
                  endLocalDictation: ${jsString(VexLifeVoiceInputFamily.EffectEndLocalDictation)}
                }),
                holdReason: Object.freeze({
                  localRecognitionUnavailable: ${jsString(VexLifeVoiceInputFamily.HoldReason.LocalRecognitionUnavailable.reasonRef)},
                  microphoneApiUnavailable: ${jsString(VexLifeVoiceInputFamily.HoldReason.MicrophoneApiUnavailable.reasonRef)},
                  audioFeedbackUnavailable: ${jsString(VexLifeVoiceInputFamily.HoldReason.AudioFeedbackUnavailable.reasonRef)}
                }),
                failureReason: Object.freeze({
                  microphonePermissionRequired: ${jsString(VexLifeVoiceInputFamily.FailureReason.MicrophonePermissionRequired.reasonRef)},
                  microphoneUnavailable: ${jsString(VexLifeVoiceInputFamily.FailureReason.MicrophoneUnavailable.reasonRef)},
                  languageUnavailable: ${jsString(VexLifeVoiceInputFamily.FailureReason.LanguageUnavailable.reasonRef)},
                  recognitionStopped: ${jsString(VexLifeVoiceInputFamily.FailureReason.RecognitionStopped.reasonRef)},
                  unknown: ${jsString(VexLifeVoiceInputFamily.FailureReason.Unknown.reasonRef)}
                }),
                attentionOverlay: ${jsString(VexLifeVoiceInputFamily.Attention.Overlay.name)},
                defaultLanguage: ${jsString(VexLifeVoiceInputFamily.DefaultLanguage.personLanguageCode)},
                languages: Object.freeze($voiceLanguage),
                displayByState: Object.freeze($voiceStateDisplays),
                holdDisplayByReason: Object.freeze($voiceHoldDisplays),
                failureDisplayByReason: Object.freeze($voiceFailureDisplays)
              });
              // Browser-provider vocabulary stays local to this adapter boundary.
              const localRecognitionAvailability = Object.freeze({
                available: 'available',
                unavailable: 'unavailable'
              });

              const byId = (id) => document.getElementById(id);
              const ui = Object.freeze({
                frame: byId('frame'),
                seam: byId('seam'),
                collapseBefore: byId('collapse-before'),
                collapseAfter: byId('collapse-after'),
                query: byId('query'),
                addressForm: byId('address-form'),
                addressError: byId('address-error'),
                reload: byId('reload'),
                appSelector: byId('app-selector'),
                addressForm: byId('address-form'),
                family: byId('source-template-family'),
                viewport: root.querySelector('.viewport'),
                archives: byId('archives'),
                archivesSummary: byId('archives-summary'),
                archivesTimeline: byId('archives-timeline'),
                quickActions: byId('quick-actions'),
                languagePanel: byId('language-panel'),
                languageList: byId('language-list'),
                speech: byId('speech'),
                speechSwitch: byId('speech-switch'),
                speechStatus: byId('speech-status'),
                speechVoice: byId('speech-voice'),
                speechThoughts: byId('speech-thoughts'),
                speechRate: byId('speech-rate'),
                speechRateValue: byId('speech-rate-value'),
                speechPlay: byId('speech-play'),
                speechStop: byId('speech-stop'),
                speechPrevious: byId('speech-previous'),
                speechNext: byId('speech-next'),
                speechChoose: byId('speech-choose'),
                pageFrame: byId('page-frame'),
                contextFrame: byId('context-frame'),
                viewportEmpty: byId('viewport-empty'),
                frameStatus: byId('frame-status'),
                companionTitle: byId('companion-title'),
                companionStatus: byId('companion-status'),
                companionStatusText: byId('companion-status-text'),
                companionStatusModel: byId('companion-status-model'),
                visionControl: byId('vision-control'),
                visionControlStatus: byId('vision-control-status'),
                viewSwitch: byId('view-switch'),
                conversation: byId('view-conversation'),
                settings: byId('view-settings'),
                timeline: byId('timeline'),
                jumpLatest: byId('jump-latest'),
                responseProgress: byId('response-progress'),
                contextShare: byId('context-share'),
                contextShareLabel: byId('context-share-label'),
                ticket: byId('ticket'),
                ticketTitle: byId('ticket-title'),
                ticketLine: byId('ticket-line'),
                ticketUrl: byId('ticket-url'),
                ticketHash: byId('ticket-hash'),
                ticketView: byId('ticket-view'),
                composerHold: byId('composer-hold'),
                composeForm: byId('compose-form'),
                draft: byId('draft'),
                voiceInput: byId('voice-input'),
                voiceInputOverlay: byId('voice-input-overlay'),
                voiceInputStatus: byId('voice-input-status'),
                voiceWaveform: byId('voice-waveform'),
                send: byId('send'),
                stop: byId('stop'),
                contextWindow: byId('context-window'),
                contextCore: byId('context-core'),
                contextUsed: byId('context-used'),
                contextRemaining: byId('context-remaining'),
                contextText: byId('context-text'),
                contextNote: byId('context-note'),
                memory: byId('memory'),
                memoryUsed: byId('memory-used'),
                memoryRemaining: byId('memory-remaining'),
                memoryText: byId('memory-text'),
                memoryNote: byId('memory-note'),
                vexMemoryLabel: byId('vex-memory-label'),
                modelsList: byId('models-list'),
                modelsNote: byId('models-note'),
                diagnosticsStatus: byId('diagnostics-status'),
                diagnosticsCapture: byId('diagnostics-capture'),
                diagnosticsRecords: byId('diagnostics-records'),
                diagnosticsEnhanced: byId('diagnostics-enhanced'),
                diagnosticsClear: byId('diagnostics-clear'),
                vexMemoryDetail: byId('vex-memory-detail'),
                resourceNote: byId('resource-note'),
                homeCurrentState: byId('home-current-state'),
                thoughtVisibility: byId('thought-visibility'),
                lowMemoryAlert: byId('low-memory-alert'),
                alertThreshold: byId('alert-threshold'),
                privateMode: byId('private-mode'),
                privateModeLabel: byId('private-mode-label'),
                privateBanner: byId('private-banner'),
                privateBannerText: byId('private-banner-text'),
                privateNotes: byId('private-notes'),
                privateProposal: byId('private-proposal'),
                privateProposalText: byId('private-proposal-text'),
                privateKeep: byId('private-keep'),
                privateDecline: byId('private-decline'),
                privatePropose: byId('private-propose'),
                inspector: byId('inspector'),
                inspectorTitle: byId('inspector-title'),
                inspectorFacts: byId('inspector-facts'),
                inspectorText: byId('inspector-text'),
                inspectorClose: byId('inspector-close'),
                snackbar: byId('snackbar')
              });

              const state = {
                conversation: null,
                context: null,
                currentness: null,
                currentState: null,
                vision: null,
                visionInFlight: false,
                privateMode: null,
                privateInFlight: false,
                settings: null,
                languageOpen: false,
                privatePending: null,
                app: (() => {
                  try {
                    const saved = localStorage.getItem(apps.storageKey);
                    if (saved && apps.keys.includes(saved)) return saved;
                  } catch {}
                  return apps.sourceTemplate;
                })(),
                memory: null,
                diagnostics: null,
                diagnosticsInFlight: false,
                // Settings → Models projection (VexLifeModels); refreshed while a download runs.
                models: null,
                modelsInFlight: false,
                workspaceStrings: null,
                browserContext: null,
                includePage: false,
                humanPageUrl: null,
                humanFrameLoadExpected: false,
                contextTargetPageUrl: null,
                contextLoading: false,
                view: 'conversation',
                allocation: null,
                collapsed: 'none',
                pending: null,
                sendInFlight: false,
                sendCancelInFlight: false,
                sendCancelRequested: false,
                streamPhase: null,
                streamedContent: '',
                streamedReasoning: '',
                thoughtsVisible: true,
                humanReasoningByResponse: new Map(),
                // Human-readable browser-session projection of facts already emitted by the Send
                // stream. This is deliberately not a turn/stage owner: it may open/freeze visual
                // containers, but it never feeds DirectCompanionTurn, capability execution, or the
                // raw streamedReasoning/streamedContent buffers consumed by Speech/Kokoro.
                liveProcessEntries: [],
                processEntriesByResponse: new Map(),
                // Session-only presentation of terminal failed Sends. Conversation/recovery remains
                // the semantic owner; this only preserves what the human already saw in this browser
                // so terminal failure does not erase the process before the next lawful Control.
                failedAttempts: [],
                liveProcessExpanded: true,
                liveProcessManuallyToggled: false,
                // What Vex reached during the live turn (bridge uses: handle/action/target/state), and
                // per completed response for this browser session. Reach, never the content.
                liveReaches: [],
                reachesByResponse: new Map(),
                expandedThoughts: new Set(),
                timelineFollowTail: true,
                timelineScrollTop: 0,
                reasoningViewports: new Map(),
                theme: null,
                quickActionsOpen: false
              };

              // VoiceInput execution lives at the browser boundary. Semantic meaning comes from
              // VexLifeVoiceInputFamily above; browser API strings are converted here and do not leak
              // into Product State or human-facing copy.
              const dictation = {
                stateRef: null,
                holdReasonRef: null,
                failureReasonRef: null,
                language: null,
                desiredActive: false,
                session: 0,
                recognition: null,
                stream: null,
                audioContext: null,
                analyser: null,
                animationFrame: null,
                draftPrefix: '',
                finalTranscript: '',
                interimTranscript: ''
              };

              const localSpeechRecognition = () => {
                const Recognition = window.SpeechRecognition;
                if (!Recognition) return null;
                if (!('processLocally' in Recognition.prototype)) return null;
                if (typeof Recognition.available !== 'function' || typeof Recognition.install !== 'function') return null;
                return Recognition;
              };

              const voiceInputLanguage = () => {
                const code = state.settings?.replyLanguage ?? voiceInput.defaultLanguage;
                const locale = voiceInput.languages[code];
                return locale ? { code, locale } : null;
              };

              const setVoiceInputState = (stateRef, options = {}) => {
                dictation.stateRef = stateRef;
                dictation.holdReasonRef = options.holdReasonRef ?? null;
                dictation.failureReasonRef = options.failureReasonRef ?? null;
                if (options.language !== undefined) dictation.language = options.language;
              };

              const observeVoiceInputCapability = () => {
                const language = voiceInputLanguage();
                if (!language || !localSpeechRecognition()) {
                  setVoiceInputState(voiceInput.state.held, {
                    holdReasonRef: voiceInput.holdReason.localRecognitionUnavailable,
                    language
                  });
                  return;
                }
                if (!navigator.mediaDevices?.getUserMedia) {
                  setVoiceInputState(voiceInput.state.held, {
                    holdReasonRef: voiceInput.holdReason.microphoneApiUnavailable,
                    language
                  });
                  return;
                }
                if (!(window.AudioContext || window.webkitAudioContext)) {
                  setVoiceInputState(voiceInput.state.held, {
                    holdReasonRef: voiceInput.holdReason.audioFeedbackUnavailable,
                    language
                  });
                  return;
                }
                setVoiceInputState(voiceInput.state.ready, { language });
              };

              const voiceInputDisplay = () => {
                if (dictation.stateRef === voiceInput.state.held) {
                  return voiceInput.holdDisplayByReason[dictation.holdReasonRef] ?? null;
                }
                if (dictation.stateRef === voiceInput.state.failed) {
                  return voiceInput.failureDisplayByReason[dictation.failureReasonRef] ??
                    voiceInput.failureDisplayByReason[voiceInput.failureReason.unknown];
                }
                return voiceInput.displayByState[dictation.stateRef] ?? null;
              };

              const renderVoiceInput = () => {
                if (!ui.voiceInput) return;
                if (
                  !dictation.desiredActive &&
                  (dictation.stateRef === null ||
                    dictation.stateRef === voiceInput.state.ready ||
                    dictation.stateRef === voiceInput.state.held)
                ) {
                  observeVoiceInputCapability();
                }
                const display = voiceInputDisplay();
                if (!display) return;
                ui.voiceInput.disabled =
                  !display.primaryControlRef || state.sendInFlight || state.privateInFlight;
                ui.voiceInput.dataset.state = dictation.stateRef ?? '';
                ui.voiceInput.dataset.holdReason = dictation.holdReasonRef ?? '';
                ui.voiceInput.dataset.failureReason = dictation.failureReasonRef ?? '';
                ui.voiceInput.setAttribute('aria-pressed', String(Boolean(display.active)));
                const overlay = display.attention === voiceInput.attentionOverlay;
                ui.voiceInput.setAttribute('aria-expanded', String(overlay));
                const humanLabel = display.primaryLabelKey
                  ? t(display.primaryLabelKey)
                  : t(display.statusKey);
                ui.voiceInput.setAttribute('aria-label', humanLabel);
                ui.voiceInput.title = humanLabel;
                if (ui.voiceInputOverlay) ui.voiceInputOverlay.hidden = !overlay;
                if (ui.voiceInputStatus) ui.voiceInputStatus.textContent = t(display.statusKey);
              };

              const stopVoiceWaveform = async () => {
                if (dictation.animationFrame !== null) {
                  cancelAnimationFrame(dictation.animationFrame);
                  dictation.animationFrame = null;
                }
                if (dictation.stream) {
                  for (const track of dictation.stream.getTracks()) track.stop();
                  dictation.stream = null;
                }
                const context = dictation.audioContext;
                dictation.audioContext = null;
                dictation.analyser = null;
                if (context && context.state !== 'closed') {
                  await context.close().catch(() => {});
                }
                if (ui.voiceWaveform) {
                  const context2d = ui.voiceWaveform.getContext('2d');
                  context2d?.clearRect(0, 0, ui.voiceWaveform.width, ui.voiceWaveform.height);
                }
              };

              const drawVoiceWaveform = () => {
                const analyser = dictation.analyser;
                const canvas = ui.voiceWaveform;
                if (!dictation.desiredActive || !analyser || !canvas) return;
                const context2d = canvas.getContext('2d');
                if (!context2d) return;
                const values = new Uint8Array(analyser.frequencyBinCount);
                analyser.getByteFrequencyData(values);
                context2d.clearRect(0, 0, canvas.width, canvas.height);
                context2d.fillStyle = getComputedStyle(ui.voiceInput).color;
                const bars = 14;
                const gap = 3;
                const width = (canvas.width - gap * (bars - 1)) / bars;
                for (let index = 0; index < bars; index += 1) {
                  const bucket = Math.floor((index / bars) * values.length);
                  const normalized = values[bucket] / 255;
                  const height = Math.max(3, normalized * (canvas.height - 8));
                  const x = index * (width + gap);
                  const y = (canvas.height - height) / 2;
                  context2d.fillRect(x, y, width, height);
                }
                dictation.animationFrame = requestAnimationFrame(drawVoiceWaveform);
              };

              const startVoiceWaveform = async (stream) => {
                const AudioContextClass = window.AudioContext || window.webkitAudioContext;
                const context = new AudioContextClass();
                await context.resume();
                const source = context.createMediaStreamSource(stream);
                const analyser = context.createAnalyser();
                analyser.fftSize = 128;
                analyser.smoothingTimeConstant = 0.78;
                source.connect(analyser);
                dictation.audioContext = context;
                dictation.analyser = analyser;
                drawVoiceWaveform();
              };

              const appendTranscriptSegment = (existing, next) => {
                const segment = String(next ?? '').trim();
                if (!segment) return existing;
                return existing ? existing + ' ' + segment : segment;
              };

              const projectDictationIntoDraft = () => {
                const spoken = [dictation.finalTranscript, dictation.interimTranscript]
                  .map((value) => String(value ?? '').trim())
                  .filter(Boolean)
                  .join(' ');
                const prefix = dictation.draftPrefix;
                ui.draft.value =
                  prefix +
                  (prefix && spoken && !/\\s$/u.test(prefix) ? ' ' : '') +
                  spoken;
                sizeDraft();
              };

              const voiceInputFailureReason = (error) => {
                const code = String(error ?? '');
                if (
                  code === 'not-allowed' ||
                  code === 'service-not-allowed' ||
                  code === 'NotAllowedError' ||
                  code === 'Permission denied'
                ) {
                  return voiceInput.failureReason.microphonePermissionRequired;
                }
                if (code === 'audio-capture' || code === 'NotFoundError') {
                  return voiceInput.failureReason.microphoneUnavailable;
                }
                if (code === 'language-not-supported') {
                  return voiceInput.failureReason.languageUnavailable;
                }
                if (code === 'AbortError' || code === 'InvalidStateError') {
                  return voiceInput.failureReason.recognitionStopped;
                }
                return voiceInput.failureReason.unknown;
              };

              const failVoiceInput = async (failureReasonRef) => {
                dictation.desiredActive = false;
                dictation.recognition = null;
                setVoiceInputState(voiceInput.state.failed, {
                  failureReasonRef,
                  language: dictation.language ?? voiceInputLanguage()
                });
                await stopVoiceWaveform();
                renderVoiceInput();
              };

              const stopVoiceInput = async (abort = false) => {
                const language = dictation.language ?? voiceInputLanguage();
                dictation.desiredActive = false;
                setVoiceInputState(voiceInput.state.stopping, { language });
                renderVoiceInput();
                const recognition = dictation.recognition;
                dictation.recognition = null;
                if (recognition) {
                  try {
                    if (abort) recognition.abort();
                    else recognition.stop();
                  } catch {}
                }
                await stopVoiceWaveform();
                observeVoiceInputCapability();
                renderVoiceInput();
              };

              const ensureLocalDictationLanguage = async (Recognition, language) => {
                setVoiceInputState(voiceInput.state.checkingLanguage, { language });
                renderVoiceInput();
                const options = {
                  langs: [language.locale],
                  processLocally: true,
                  quality: 'dictation'
                };
                const availability = await Recognition.available(options);
                if (availability === localRecognitionAvailability.available) return true;
                if (availability === localRecognitionAvailability.unavailable) return false;
                setVoiceInputState(voiceInput.state.installingLanguage, { language });
                renderVoiceInput();
                return Boolean(await Recognition.install(options));
              };

              const startVoiceInput = async () => {
                if (state.sendInFlight || state.privateInFlight) return;
                observeVoiceInputCapability();
                const display = voiceInputDisplay();
                if (display?.primaryControlRef !== voiceInput.control.start) {
                  renderVoiceInput();
                  return;
                }

                const Recognition = localSpeechRecognition();
                const language = dictation.language ?? voiceInputLanguage();
                if (!Recognition || !language) {
                  observeVoiceInputCapability();
                  renderVoiceInput();
                  return;
                }

                const session = dictation.session + 1;
                dictation.session = session;
                dictation.desiredActive = true;
                dictation.language = language;
                dictation.draftPrefix = ui.draft.value;
                dictation.finalTranscript = '';
                dictation.interimTranscript = '';

                try {
                  const languageReady = await ensureLocalDictationLanguage(Recognition, language);
                  if (!dictation.desiredActive || session !== dictation.session) return;
                  if (!languageReady) {
                    await failVoiceInput(voiceInput.failureReason.languageUnavailable);
                    return;
                  }

                  setVoiceInputState(voiceInput.state.requestingMicrophone, { language });
                  renderVoiceInput();
                  const stream = await navigator.mediaDevices.getUserMedia({
                    audio: {
                      echoCancellation: true,
                      noiseSuppression: true,
                      autoGainControl: true
                    },
                    video: false
                  });
                  if (!dictation.desiredActive || session !== dictation.session) {
                    for (const track of stream.getTracks()) track.stop();
                    return;
                  }
                  dictation.stream = stream;
                  await startVoiceWaveform(stream);

                  const recognition = new Recognition();
                  recognition.lang = language.locale;
                  recognition.continuous = true;
                  recognition.interimResults = true;
                  recognition.maxAlternatives = 1;
                  recognition.processLocally = true;
                  dictation.recognition = recognition;

                  recognition.addEventListener('start', () => {
                    if (session !== dictation.session || !dictation.desiredActive) return;
                    setVoiceInputState(voiceInput.state.listening, { language });
                    renderVoiceInput();
                  });

                  recognition.addEventListener('result', (event) => {
                    if (session !== dictation.session || !dictation.desiredActive) return;
                    let interim = '';
                    for (let index = event.resultIndex; index < event.results.length; index += 1) {
                      const result = event.results[index];
                      const text = String(result?.[0]?.transcript ?? '').trim();
                      if (!text) continue;
                      if (result.isFinal) {
                        dictation.finalTranscript = appendTranscriptSegment(dictation.finalTranscript, text);
                      } else {
                        interim = appendTranscriptSegment(interim, text);
                      }
                    }
                    dictation.interimTranscript = interim;
                    projectDictationIntoDraft();
                  });

                  recognition.addEventListener('error', (event) => {
                    if (session !== dictation.session || !dictation.desiredActive) return;
                    if (event.error === 'no-speech') return;
                    void failVoiceInput(voiceInputFailureReason(event.error));
                  });

                  recognition.addEventListener('end', () => {
                    if (session !== dictation.session || !dictation.desiredActive) return;
                    const track = dictation.stream?.getAudioTracks()?.[0];
                    if (!track || track.readyState !== 'live') {
                      void failVoiceInput(voiceInput.failureReason.microphoneUnavailable);
                      return;
                    }
                    setTimeout(() => {
                      if (session !== dictation.session || !dictation.desiredActive) return;
                      try {
                        recognition.start(track);
                      } catch (error) {
                        void failVoiceInput(voiceInputFailureReason(error?.name));
                      }
                    }, 120);
                  });

                  const track = stream.getAudioTracks()[0];
                  if (!track) {
                    await failVoiceInput(voiceInput.failureReason.microphoneUnavailable);
                    return;
                  }
                  recognition.start(track);
                } catch (error) {
                  if (session !== dictation.session) return;
                  const failure = error instanceof DOMException
                    ? error.name
                    : error instanceof Error
                      ? error.message
                      : error;
                  await failVoiceInput(voiceInputFailureReason(failure));
                }
              };

              const applyVoiceInputControl = async () => {
                const display = voiceInputDisplay();
                if (display?.primaryControlRef === voiceInput.control.start) {
                  await startVoiceInput();
                } else if (display?.primaryControlRef === voiceInput.control.stop) {
                  await stopVoiceInput(false);
                }
              };

              const readJson = async (response) => {
                const value = await response.json();
                if (!response.ok) {
                  const error = new Error(String(value?.message ?? ('HTTP ' + response.status)));
                  error.vexStatus = response.status;
                  error.vexResponse = value;
                  throw error;
                }
                return value;
              };

              const readEventStream = async (response, onEvent) => {
                if (!response.ok) return readJson(response);
                if (!response.body) throw new Error('Vex stream did not expose a readable response body.');
                const reader = response.body.getReader();
                const decoder = new TextDecoder();
                let buffer = '';

                const consume = (raw) => {
                  const data = raw
                    .split(/\r?\n/gu)
                    .filter((line) => line.startsWith('data:'))
                    .map((line) => line.slice(5).trimStart())
                    .join('\n');
                  if (!data || data === '[DONE]') return;
                  let event;
                  try { event = JSON.parse(data); }
                  catch { throw new Error('Vex stream emitted invalid JSON.'); }
                  onEvent(event);
                };

                for (;;) {
                  const next = await reader.read();
                  if (next.done) break;
                  buffer += decoder.decode(next.value, { stream: true });
                  for (;;) {
                    const match = buffer.match(/\r?\n\r?\n/u);
                    if (!match || match.index === undefined) break;
                    const raw = buffer.slice(0, match.index);
                    buffer = buffer.slice(match.index + match[0].length);
                    consume(raw);
                  }
                }
                buffer += decoder.decode();
                if (buffer.trim()) consume(buffer);
              };

              const notify = (message) => {
                if (!ui.snackbar) return;
                ui.snackbar.textContent = message;
                ui.snackbar.hidden = false;
                clearTimeout(notify.timer);
                notify.timer = setTimeout(() => { ui.snackbar.hidden = true; }, 4800);
              };

              const THOUGHT_VISIBILITY_KEY = 'vexlife-prod.presentation.thoughts-visible';

              const loadThoughtVisibility = () => {
                try {
                  const stored = localStorage.getItem(THOUGHT_VISIBILITY_KEY);
                  if (stored === 'false') state.thoughtsVisible = false;
                  else if (stored === 'true') state.thoughtsVisible = true;
                } catch {}
              };

              const saveThoughtVisibility = () => {
                try {
                  localStorage.setItem(THOUGHT_VISIBILITY_KEY, String(state.thoughtsVisible));
                } catch {}
              };

              const currentTheme = () =>
                document.documentElement.dataset.theme ||
                (matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');

              const applyTheme = () => {
                try {
                  state.theme = localStorage.getItem('vexlife-prod.presentation.theme');
                } catch {
                  state.theme = null;
                }
                if (state.theme === 'light' || state.theme === 'dark') {
                  document.documentElement.dataset.theme = state.theme;
                } else {
                  delete document.documentElement.dataset.theme;
                }
              };

              const toggleTheme = () => {
                state.theme = currentTheme() === 'dark' ? 'light' : 'dark';
                document.documentElement.dataset.theme = state.theme;
                try {
                  localStorage.setItem('vexlife-prod.presentation.theme', state.theme);
                } catch {}
              };

              // Speech: the Vextreme Spiral FAB's Read Aloud, ported and extended. When Speech is on, Vex
              // is read aloud as it streams, and its thoughts too if the person chooses. It reads the
              // Conversation/raw stream projections, never the DOM. The visual process projection is
              // a sibling observer of that same stream: freezing/reordering visual containers must
              // never become Speech input, drop speech deltas, or feed execution back upstream.
              // Kokoro voices run on this machine after a one-time model download; system voices are
              // limited to those that run on this machine. Either way the words are never sent to a
              // speech service.
              const SPEECH_SETTINGS = 'vexlife-prod.presentation.speech/v1';
              const KOKORO_MODULE_URL = 'https://cdn.jsdelivr.net/npm/kokoro-js@1.2.1/+esm';
              const KOKORO_MODEL_ID = 'onnx-community/Kokoro-82M-v1.0-ONNX';
              const KOKORO_VOICES = [
                { id: 'af_heart', name: 'Heart' },
                { id: 'af_bella', name: 'Bella' },
                { id: 'af_nicole', name: 'Nicole' },
                { id: 'bf_emma', name: 'Emma (UK)' },
                { id: 'am_michael', name: 'Michael' },
                { id: 'am_fenrir', name: 'Fenrir' },
                { id: 'am_puck', name: 'Puck' },
                { id: 'bm_george', name: 'George (UK)' }
              ];
              const speech = {
                systemSupported: 'speechSynthesis' in window && 'SpeechSynthesisUtterance' in window,
                open: false,
                choosing: false,
                queue: [],
                current: null,
                paused: false,
                generation: 0,
                stream: { reply: 0, thought: 0 },
                kokoro: { worker: null, state: 'idle', jobs: new Map(), nextId: 1, cache: new Map(), context: null, source: null },
                settings: { enabled: false, voice: 'kokoro:af_heart', readThoughts: false, rate: 1 }
              };

              const loadSpeechSettings = () => {
                try {
                  const saved = JSON.parse(localStorage.getItem(SPEECH_SETTINGS) || '{}');
                  speech.settings.enabled = saved.enabled === true;
                  speech.settings.readThoughts = saved.readThoughts === true;
                  if (typeof saved.voice === 'string' && saved.voice) speech.settings.voice = saved.voice;
                  const rate = Number(saved.rate);
                  if (Number.isFinite(rate)) speech.settings.rate = Math.min(1.6, Math.max(0.7, rate));
                } catch {}
              };

              const saveSpeechSettings = () => {
                try {
                  localStorage.setItem(SPEECH_SETTINGS, JSON.stringify(speech.settings));
                } catch {}
              };

              const speechLanguage = () => (document.documentElement.lang || 'en').slice(0, 2).toLowerCase();

              const localVoices = () => {
                if (!speech.systemSupported) return [];
                const language = speechLanguage();
                return window.speechSynthesis.getVoices().filter((voice) =>
                  voice.localService && String(voice.lang || '').toLowerCase().startsWith(language));
              };

              // Kokoro voices are English; other languages use this machine's own voices.
              const voiceChoices = () => {
                const natural = speechLanguage() === 'en'
                  ? KOKORO_VOICES.map((voice) => ({ value: 'kokoro:' + voice.id, label: voice.name, group: 'natural' }))
                  : [];
                const system = localVoices().map((voice) => ({ value: 'system:' + voice.voiceURI, label: voice.name, group: 'system' }));
                return natural.concat(system);
              };

              const chosenVoice = () => {
                const choices = voiceChoices();
                return choices.find((choice) => choice.value === speech.settings.voice) ?? choices[0] ?? null;
              };

              const speechText = (text) => String(text ?? '')
                .replace(/[*_#>~|]+/gu, ' ')
                .replace(/\[([^\]]+)\]\([^)]*\)/gu, (match, label) => label)
                .replace(/\s+/gu, ' ')
                .trim();

              const speechChunks = (text) => {
                const sentences = String(text).match(/[^.!?]+[.!?]*\s*/gu) || [];
                const chunks = [];
                let current = '';
                for (const sentence of sentences) {
                  if (current && (current + sentence).length > 300) {
                    chunks.push(current.trim());
                    current = '';
                  }
                  current += sentence;
                }
                if (current.trim()) chunks.push(current.trim());
                return chunks;
              };

              // Runs inside a module Worker so the page stays responsive while Kokoro loads and speaks.
              function kokoroWorkerMain(moduleUrl, modelId) {
                let engine = null;
                let preparing = null;
                let generation = 0;
                let active = false;
                const queue = [];
                const options = async () => {
                  try {
                    if (self.navigator && self.navigator.gpu) {
                      const adapter = await self.navigator.gpu.requestAdapter();
                      if (adapter) return { device: 'webgpu', dtype: 'fp32' };
                    }
                  } catch (error) {}
                  return { device: 'wasm', dtype: 'q8' };
                };
                const ready = () => {
                  if (engine) return Promise.resolve(engine);
                  if (preparing) return preparing;
                  self.postMessage({ type: 'preparing' });
                  preparing = import(moduleUrl).then(async (module) => {
                    const chosen = await options();
                    try {
                      engine = await module.KokoroTTS.from_pretrained(modelId, chosen);
                    } catch (error) {
                      if (chosen.device !== 'webgpu') throw error;
                      engine = await module.KokoroTTS.from_pretrained(modelId, { device: 'wasm', dtype: 'q8' });
                    }
                    self.postMessage({ type: 'ready' });
                    return engine;
                  }).catch((error) => {
                    preparing = null;
                    throw error;
                  });
                  return preparing;
                };
                const drain = async () => {
                  if (active) return;
                  active = true;
                  try {
                    while (queue.length) {
                      const job = queue.shift();
                      if (job.generation !== generation) continue;
                      try {
                        const tts = await ready();
                        if (job.generation !== generation) continue;
                        const raw = await tts.generate(job.text, { voice: job.voice, speed: 1 });
                        const samples = raw && raw.audio;
                        if (!samples || !samples.length) throw new Error('KOKORO_AUDIO_EMPTY');
                        self.postMessage({ type: 'audio', id: job.id, sampleRate: raw.sampling_rate, samples: samples.buffer }, [samples.buffer]);
                      } catch (error) {
                        self.postMessage({ type: 'error', id: job.id, message: String((error && error.message) || error) });
                      }
                    }
                  } finally {
                    active = false;
                  }
                };
                self.onmessage = (event) => {
                  const value = event.data || {};
                  if (value.type === 'cancel') {
                    generation = value.generation;
                    queue.length = 0;
                    return;
                  }
                  if (value.type === 'prepare') {
                    ready().catch((error) => self.postMessage({ type: 'error', id: null, message: String((error && error.message) || error) }));
                    return;
                  }
                  if (value.type === 'generate') {
                    generation = value.generation;
                    queue.push(value);
                    void drain();
                  }
                };
              }

              const kokoroWorker = () => {
                const kokoro = speech.kokoro;
                if (kokoro.worker) return kokoro.worker;
                const source = '(' + kokoroWorkerMain.toString() + ')(' + JSON.stringify(KOKORO_MODULE_URL) + ', ' + JSON.stringify(KOKORO_MODEL_ID) + ');';
                const url = URL.createObjectURL(new Blob([source], { type: 'text/javascript' }));
                kokoro.worker = new Worker(url, { type: 'module' });
                kokoro.state = 'loading';
                kokoro.worker.onmessage = (event) => {
                  const value = event.data || {};
                  if (value.type === 'preparing' || value.type === 'ready') {
                    kokoro.state = value.type === 'ready' ? 'ready' : 'loading';
                    renderSpeech();
                    return;
                  }
                  const job = kokoro.jobs.get(value.id);
                  if (value.type === 'audio' && job) {
                    kokoro.jobs.delete(value.id);
                    job.resolve({ samples: new Float32Array(value.samples), sampleRate: value.sampleRate });
                  } else if (value.type === 'error') {
                    if (job) {
                      kokoro.jobs.delete(value.id);
                      job.reject(new Error(value.message));
                    }
                    if (kokoro.state !== 'ready') kokoro.state = 'failed';
                    renderSpeech();
                  }
                };
                kokoro.worker.onerror = () => {
                  kokoro.state = 'failed';
                  for (const job of kokoro.jobs.values()) job.reject(new Error('The natural voice stopped.'));
                  kokoro.jobs.clear();
                  renderSpeech();
                };
                return kokoro.worker;
              };

              const kokoroAudio = (text, voiceId) => {
                const kokoro = speech.kokoro;
                const key = voiceId + '|' + text;
                if (kokoro.cache.has(key)) return kokoro.cache.get(key);
                const id = kokoro.nextId++;
                const promise = new Promise((resolve, reject) => kokoro.jobs.set(id, { resolve, reject }));
                kokoroWorker().postMessage({ type: 'generate', id, text, voice: voiceId, generation: speech.generation });
                kokoro.cache.set(key, promise);
                if (kokoro.cache.size > 24) kokoro.cache.delete(kokoro.cache.keys().next().value);
                promise.catch(() => kokoro.cache.delete(key));
                return promise;
              };

              const audioContext = () => {
                const kokoro = speech.kokoro;
                if (!kokoro.context) {
                  const Context = window.AudioContext || window.webkitAudioContext;
                  if (!Context) return null;
                  kokoro.context = new Context();
                }
                if (kokoro.context.state === 'suspended' && !speech.paused) kokoro.context.resume().catch(() => {});
                return kokoro.context;
              };

              const speakItem = (item, generation) => new Promise((resolve, reject) => {
                const voice = chosenVoice();
                if (!voice) {
                  reject(new Error('No voice on this Mac can read aloud.'));
                  return;
                }
                if (voice.value.startsWith('kokoro:')) {
                  const voiceId = voice.value.slice('kokoro:'.length);
                  const next = speech.queue[1];
                  kokoroAudio(item.text, voiceId).then(({ samples, sampleRate }) => {
                    if (generation !== speech.generation) {
                      resolve();
                      return;
                    }
                    const context = audioContext();
                    if (!context) {
                      reject(new Error('This browser cannot play the natural voice.'));
                      return;
                    }
                    const buffer = context.createBuffer(1, samples.length, sampleRate);
                    buffer.copyToChannel(samples, 0);
                    const source = context.createBufferSource();
                    source.buffer = buffer;
                    source.playbackRate.value = speech.settings.rate;
                    source.connect(context.destination);
                    speech.kokoro.source = source;
                    source.onended = () => {
                      if (speech.kokoro.source === source) speech.kokoro.source = null;
                      resolve();
                    };
                    source.start();
                    if (next) kokoroAudio(next.text, voiceId).catch(() => {});
                  }, reject);
                  return;
                }
                const system = localVoices().find((candidate) => candidate.voiceURI === voice.value.slice('system:'.length));
                const utterance = new SpeechSynthesisUtterance(item.text);
                if (system) {
                  utterance.voice = system;
                  utterance.lang = system.lang;
                }
                utterance.rate = speech.settings.rate;
                utterance.onend = () => resolve();
                utterance.onerror = (event) => (event.error === 'interrupted' || event.error === 'canceled'
                  ? resolve()
                  : reject(new Error(String(event.error))));
                window.speechSynthesis.speak(utterance);
              });

              const pumpSpeech = () => {
                if (speech.current || speech.paused || !speech.queue.length) {
                  renderSpeech();
                  return;
                }
                const generation = speech.generation;
                speech.current = speech.queue[0];
                renderSpeech();
                speakItem(speech.current, generation).then(() => {
                  if (generation !== speech.generation) return;
                  speech.queue.shift();
                  speech.current = null;
                  pumpSpeech();
                }, () => {
                  if (generation !== speech.generation) return;
                  speech.current = null;
                  const fallback = localVoices()[0];
                  if (chosenVoice()?.group === 'natural' && fallback) {
                    speech.settings.voice = 'system:' + fallback.voiceURI;
                    saveSpeechSettings();
                    renderSpeechVoices();
                    notify("The natural voice couldn't load, so this Mac's voice is reading instead.");
                    pumpSpeech();
                    return;
                  }
                  stopSpeech();
                  notify("Speech stopped because the voice couldn't continue.");
                });
              };

              const enqueueSpeech = (text, eventRef) => {
                for (const chunk of speechChunks(speechText(text))) speech.queue.push({ text: chunk, eventRef });
                pumpSpeech();
              };

              const stopSpeech = () => {
                speech.generation += 1;
                speech.queue = [];
                speech.current = null;
                speech.paused = false;
                speech.choosing = false;
                if (speech.systemSupported) window.speechSynthesis.cancel();
                const kokoro = speech.kokoro;
                if (kokoro.source) {
                  try {
                    kokoro.source.onended = null;
                    kokoro.source.stop();
                  } catch {}
                  kokoro.source = null;
                }
                if (kokoro.context?.state === 'suspended') kokoro.context.resume().catch(() => {});
                if (kokoro.worker) kokoro.worker.postMessage({ type: 'cancel', generation: speech.generation });
                for (const job of kokoro.jobs.values()) job.reject(new Error('cancelled'));
                kokoro.jobs.clear();
                kokoro.cache.clear();
                renderSpeech();
              };

              const pauseOrResumeSpeech = () => {
                if (!speech.current && !speech.queue.length) {
                  startReading(null);
                  return;
                }
                speech.paused = !speech.paused;
                if (speech.systemSupported) {
                  if (speech.paused) window.speechSynthesis.pause();
                  else window.speechSynthesis.resume();
                }
                const context = speech.kokoro.context;
                if (context) {
                  if (speech.paused) context.suspend().catch(() => {});
                  else context.resume().catch(() => {});
                }
                if (!speech.paused) pumpSpeech();
                renderSpeech();
              };

              // Live speech follows the stream sentence by sentence; the tail is spoken on completion.
              const speakFresh = (text, offset, final) => {
                const pending = String(text ?? '').slice(offset);
                if (!pending.trim()) return offset + pending.length;
                const match = final ? null : pending.match(/^[\s\S]*[.!?:;](?=\s)|^[\s\S]*\n/u);
                const end = final ? pending.length : (match ? match[0].length : 0);
                if (end <= 0) return offset;
                enqueueSpeech(pending.slice(0, end), 'live');
                return offset + end;
              };

              const followStreamSpeech = (final) => {
                if (!speech.settings.enabled) return;
                if (speech.settings.readThoughts) {
                  speech.stream.thought = speakFresh(state.streamedReasoning, speech.stream.thought, final || Boolean(state.streamedContent));
                }
                speech.stream.reply = speakFresh(state.streamedContent, speech.stream.reply, final);
              };

              const readablePlan = () => (isPrivate()
                ? (state.privateMode?.exchanges ?? []).flatMap((exchange, index) => [
                    { role: 'human', content: exchange.human, eventRef: 'private.' + (index * 2 + 1) },
                    { role: 'vex', content: exchange.vex, eventRef: 'private.' + (index * 2 + 2) }
                  ])
                : (state.conversation?.messages ?? []))
                .filter((message) => String(message.content ?? '').trim())
                .map((message) => ({ eventRef: String(message.eventRef ?? ''), vex: message.role !== 'human', text: message.content }));

              const startReading = (eventRef) => {
                const plan = readablePlan();
                stopSpeech();
                if (!plan.length) {
                  notify('There is nothing to read yet.');
                  return;
                }
                let index = eventRef ? plan.findIndex((message) => message.eventRef === eventRef) : -1;
                if (index < 0) {
                  const vex = plan.map((message, at) => (message.vex ? at : -1)).filter((at) => at >= 0);
                  index = vex.length ? vex[vex.length - 1] : plan.length - 1;
                }
                audioContext();
                for (const message of plan.slice(index)) enqueueSpeech(message.text, message.eventRef);
              };

              const stepReading = (direction) => {
                const plan = readablePlan();
                const at = plan.findIndex((message) => message.eventRef === speech.current?.eventRef);
                const target = plan[at + direction];
                if (at < 0 || !target) return;
                startReading(target.eventRef);
              };

              const markReading = () => {
                if (!ui.timeline) return;
                const current = speech.current?.eventRef;
                for (const item of ui.timeline.querySelectorAll('.message')) {
                  item.classList.toggle('reading', Boolean(current) && current !== 'live' && item.dataset.eventRef === current);
                }
                ui.timeline.dataset.choosing = String(speech.choosing);
              };

              const renderSpeechVoices = () => {
                if (!ui.speechVoice) return;
                const choices = voiceChoices();
                const groups = [
                  ['natural', 'Natural voices · downloaded once, made on this Mac'],
                  ['system', "This Mac's voices"]
                ].map(([group, label]) => {
                  const members = choices.filter((choice) => choice.group === group);
                  if (!members.length) return null;
                  const node = document.createElement('optgroup');
                  node.label = label;
                  for (const choice of members) {
                    const option = document.createElement('option');
                    option.value = choice.value;
                    option.textContent = choice.label;
                    node.append(option);
                  }
                  return node;
                }).filter(Boolean);
                ui.speechVoice.replaceChildren(...groups);
                const chosen = chosenVoice();
                if (chosen) ui.speechVoice.value = chosen.value;
                ui.speechVoice.disabled = !choices.length;
              };

              const renderSpeech = () => {
                if (!ui.speech) return;
                ui.speech.hidden = !speech.open;
                const playing = Boolean(speech.current) && !speech.paused;
                const busy = Boolean(speech.current) || speech.queue.length > 0;
                ui.speech.dataset.mode = playing ? 'playing' : busy ? 'paused' : 'idle';
                const action = ui.quickActions?.querySelector('.quick-action[data-action="speech"]');
                if (action) {
                  action.setAttribute('aria-expanded', String(speech.open));
                  action.setAttribute('aria-pressed', String(speech.settings.enabled));
                  action.dataset.active = String(speech.settings.enabled);
                }
                ui.speechSwitch.setAttribute('aria-checked', String(speech.settings.enabled));
                ui.speechThoughts.checked = speech.settings.readThoughts;
                ui.speechRate.value = String(speech.settings.rate);
                ui.speechRateValue.textContent = speech.settings.rate.toFixed(1) + '×';
                ui.speechPlay.setAttribute('aria-label', playing ? 'Pause' : 'Play');
                ui.speechPlay.title = playing ? 'Pause' : 'Play';
                ui.speechChoose.setAttribute('aria-pressed', String(speech.choosing));
                const usable = voiceChoices().length > 0;
                ui.speechSwitch.disabled = !usable;
                ui.speechPlay.disabled = !usable;
                ui.speechChoose.disabled = !usable;
                ui.speechStop.disabled = !busy;
                ui.speechPrevious.disabled = !busy || !speech.current || speech.current.eventRef === 'live';
                ui.speechNext.disabled = ui.speechPrevious.disabled;
                const natural = chosenVoice()?.group === 'natural';
                const kokoroState = speech.kokoro.state;
                ui.speechStatus.textContent = !usable
                  ? 'No voice on this Mac is available for speech.'
                  : speech.choosing
                    ? 'Choose a message to start from. Esc cancels.'
                    : natural && kokoroState === 'loading'
                      ? 'Getting the natural voice ready. The first time, it downloads once (about 90 MB); speech is made on this Mac.'
                      : natural && kokoroState === 'failed'
                        ? "The natural voice couldn't load; this Mac's voices still work."
                        : busy
                          ? (playing ? 'Reading Vex…' : 'Paused.')
                          : speech.settings.enabled
                            ? (speech.settings.readThoughts ? "Vex's thoughts and replies will be read aloud." : "Vex's replies will be read aloud.")
                            : 'Speech is off. You can still choose where to start reading.';
                markReading();
              };

              // Reply language is a Home setting the service tells Vex about on every message.
              const renderLanguage = () => {
                if (!ui.languagePanel) return;
                ui.languagePanel.hidden = !state.languageOpen;
                const current = state.settings?.replyLanguage ?? 'en';
                document.documentElement.lang = current;
                const action = ui.quickActions?.querySelector('.quick-action[data-action="language"]');
                if (action) {
                  action.setAttribute('aria-expanded', String(state.languageOpen));
                  const name = (state.settings?.languages ?? []).find((language) => language.code === current)?.name;
                  action.title = 'Vex replies in ' + (name ?? current);
                  action.setAttribute('aria-label', action.title);
                }
                const items = (state.settings?.languages ?? []).map((language) => {
                  const item = document.createElement('li');
                  const button = document.createElement('button');
                  button.type = 'button';
                  button.dataset.language = language.code;
                  button.setAttribute('role', 'option');
                  button.setAttribute('aria-selected', String(language.code === current));
                  button.textContent = language.name;
                  item.append(button);
                  return item;
                });
                ui.languageList.replaceChildren(...items);
              };

              const loadSettings = async () => {
                const response = await fetch(service.settingsPath, { headers: { accept: 'application/json' }, cache: 'no-store' });
                state.settings = await readJson(response);
                renderLanguage();
                renderSpeechVoices();
                renderSpeech();
                return state.settings;
              };

              const chooseLanguage = async (code) => {
                if (dictation.desiredActive) await stopVoiceInput(true);
                try {
                  const response = await fetch(service.settingsPath, {
                    method: 'POST',
                    headers: { 'content-type': 'application/json', accept: 'application/json' },
                    body: JSON.stringify({ replyLanguage: code })
                  });
                  state.settings = await readJson(response);
                  state.languageOpen = false;
                  observeVoiceInputCapability();
                  renderVoiceInput();
                  renderLanguage();
                  renderSpeechVoices();
                  renderSpeech();
                  renderQuickActions();
                  const name = state.settings.languages.find((language) => language.code === code)?.name ?? code;
                  notify('Vex will reply in ' + name + '.');
                } catch (error) {
                  notify(error instanceof Error ? error.message : 'The language could not be changed.');
                }
              };

              const quickActionSvg = (paths) =>
                '<svg viewBox="0 0 24 24" aria-hidden="true">' + paths + '</svg>';

              const configureSourceQuickAction = (id, action, label, icon) => {
                const button = byId(id);
                const item = button?.closest('li');
                if (!button || !item) return null;
                item.hidden = false;
                item.className = 'quick-action-item';
                button.hidden = false;
                button.type = 'button';
                button.className = 'quick-action';
                button.dataset.action = action;
                button.dataset.bound = 'true';
                button.setAttribute('aria-label', label);
                button.title = label;
                button.innerHTML = quickActionSvg(icon);
                return { item, button };
              };

              const createQuickActionButton = (id, action, label, glyph) => {
                const item = document.createElement('li');
                item.className = 'quick-action-item';
                const button = document.createElement('button');
                button.id = id;
                button.type = 'button';
                button.className = 'quick-action quick-action-glyph';
                button.dataset.action = action;
                button.dataset.bound = 'true';
                button.setAttribute('aria-label', label);
                button.title = label;
                button.textContent = glyph;
                item.append(button);
                return { item, button };
              };

              const initializeQuickActions = () => {
                if (!ui.quickActions || byId('quick-actions-trigger')) return;

                const sourceItems = [...ui.quickActions.children];
                const speechAction = configureSourceQuickAction(
                  'speech-source',
                  'speech',
                  'Speech',
                  '<path d="M4 9.5h3.2L11.5 6v12l-4.3-3.5H4z"/><path d="M15 9.2a4 4 0 0 1 0 5.6M17.6 6.8a7.3 7.3 0 0 1 0 10.4"/>'
                );
                const languageAction = configureSourceQuickAction(
                  'language-source',
                  'language',
                  'Language',
                  '<path d="M5 20.5V4M5 4.5h12.5l-2 3.5 2 3.5H5"/>'
                );
                const visionAction = configureSourceQuickAction(
                  'vision-quick-access-source',
                  'vision',
                  'Vision',
                  '<path d="M3 12s3.6-5.2 9-5.2 9 5.2 9 5.2-3.6 5.2-9 5.2S3 12 3 12Z"/><circle cx="12" cy="12" r="2.8"/>'
                );
                const themeAction = createQuickActionButton(
                  'theme-quick-action',
                  'theme',
                  'Toggle dark / light theme',
                  currentTheme() === 'dark' ? '🌙' : '☀️'
                );

                const supportItem = document.createElement('li');
                supportItem.className = 'quick-action-item';
                const support = document.createElement('a');
                support.id = 'support-quick-action';
                support.className = 'quick-action quick-action-glyph';
                support.dataset.action = 'support';
                support.dataset.bound = 'true';
                support.href = 'https://vgong24.github.io/Vextreme/pages/vex-support.html';
                support.target = '_blank';
                support.rel = 'noopener noreferrer';
                support.setAttribute('aria-label', 'Support Vextreme and VexLife');
                support.title = 'Support the creator';
                support.textContent = '🫴';
                supportItem.append(support);

                const triggerItem = document.createElement('li');
                triggerItem.className = 'quick-action-trigger-item';
                const trigger = document.createElement('button');
                trigger.id = 'quick-actions-trigger';
                trigger.type = 'button';
                trigger.className = 'quick-action quick-action-trigger quick-action-glyph';
                trigger.setAttribute('aria-label', 'Open quick actions');
                trigger.setAttribute('aria-expanded', 'false');
                trigger.textContent = '🌀';
                triggerItem.append(trigger);

                const visibleSourceItems = new Set(
                  [speechAction?.item, languageAction?.item, visionAction?.item].filter(Boolean)
                );
                const preservedSourceItems = sourceItems.filter((item) => !visibleSourceItems.has(item));
                ui.quickActions.replaceChildren(
                  ...preservedSourceItems,
                  ...(speechAction ? [speechAction.item] : []),
                  themeAction.item,
                  ...(languageAction ? [languageAction.item] : []),
                  ...(visionAction ? [visionAction.item] : []),
                  supportItem,
                  triggerItem
                );
                ui.quickActions.dataset.open = 'false';

                trigger.addEventListener('click', (event) => {
                  event.stopPropagation();
                  state.quickActionsOpen = !state.quickActionsOpen;
                  if (!state.quickActionsOpen) {
                    state.languageOpen = false;
                    speech.open = false;
                    speech.choosing = false;
                  }
                  renderQuickActions();
                });

                themeAction.button.addEventListener('click', (event) => {
                  event.stopPropagation();
                  toggleTheme();
                  renderQuickActions();
                });

                speechAction?.button.addEventListener('click', (event) => {
                  event.stopPropagation();
                  state.languageOpen = false;
                  speech.open = !speech.open;
                  if (!speech.open) speech.choosing = false;
                  renderSpeechVoices();
                  renderQuickActions();
                });

                languageAction?.button.addEventListener('click', (event) => {
                  event.stopPropagation();
                  state.languageOpen = !state.languageOpen;
                  if (state.languageOpen) speech.open = false;
                  renderQuickActions();
                });

                visionAction?.button.addEventListener('click', (event) => {
                  event.stopPropagation();
                  ui.visionControl?.click();
                });

                document.addEventListener('click', (event) => {
                  if (!state.quickActionsOpen) return;
                  // Authored LocalInteractions may activate an existing Product Control with a
                  // synthetic click. Only a real human click outside the Spiral dismisses it.
                  if (!event.isTrusted) return;
                  if (
                    ui.quickActions.contains(event.target) ||
                    ui.speech?.contains(event.target) ||
                    ui.languagePanel?.contains(event.target)
                  ) return;
                  state.quickActionsOpen = false;
                  state.languageOpen = false;
                  speech.open = false;
                  speech.choosing = false;
                  renderQuickActions();
                });
              };

              const renderQuickActions = () => {
                if (!ui.quickActions) return;

                const trigger = byId('quick-actions-trigger');
                if (trigger) {
                  trigger.setAttribute('aria-expanded', String(state.quickActionsOpen));
                  trigger.setAttribute('aria-label', state.quickActionsOpen ? 'Close quick actions' : 'Open quick actions');
                }
                ui.quickActions.dataset.open = String(state.quickActionsOpen);

                const theme = byId('theme-quick-action');
                if (theme) {
                  theme.textContent = currentTheme() === 'dark' ? '🌙' : '☀️';
                  theme.title = 'Toggle theme';
                  theme.setAttribute('aria-label', 'Toggle dark / light theme');
                }

                const speechButton = byId('speech-source');
                if (speechButton) {
                  speechButton.dataset.bound = 'true';
                  speechButton.setAttribute('aria-expanded', String(speech.open));
                  speechButton.setAttribute('aria-pressed', String(speech.settings.enabled));
                  speechButton.dataset.active = String(speech.settings.enabled);
                }

                const languageButton = byId('language-source');
                if (languageButton) {
                  languageButton.dataset.bound = 'true';
                  languageButton.setAttribute('aria-expanded', String(state.languageOpen));
                }

                const visionButton = byId('vision-quick-access-source');
                if (visionButton) {
                  const display = state.vision?.display;
                  const bound = Boolean(display?.primaryControlRef);
                  const label = String(display?.label ?? 'Vision · Held');
                  visionButton.dataset.bound = String(bound);
                  visionButton.dataset.indicator = String(display?.indicatorRef ?? 'None');
                  visionButton.disabled = !bound || state.visionInFlight;
                  visionButton.setAttribute('aria-label', label);
                  visionButton.title = state.visionInFlight
                    ? 'Vision · Working'
                    : label + (display?.primaryLabel ? ' — ' + display.primaryLabel : '');
                }

                renderSpeech();
                renderLanguage();
              };

              // Vision copy, state and the one lawful Control come from the Product (VexLifeVisionFamily);
              // this renderer only shows them and sends the Control back for fresh resolution.
              const renderMemory = () => {
                const display = state.memory?.display;
                ui.vexMemoryLabel.textContent = String(display?.label ?? 'Memory · Checking');
                ui.vexMemoryDetail.textContent = String(display?.detail ?? "VexLife hasn't read Vex's Memory yet.");
                root.dataset.vexMemoryState = String(state.memory?.state ?? '');
              };

              const renderVision = () => {
                if (!ui.visionControl) return;
                const display = state.vision?.display;
                const label = String(display?.label ?? 'Vision · Held');
                const stateNode = ui.visionControl.querySelector('.vision-state');
                if (stateNode) stateNode.textContent = state.visionInFlight ? 'Working' : label.replace(/^Vision · /, '');
                ui.visionControl.dataset.indicator = String(display?.indicatorRef ?? 'None');
                ui.visionControl.disabled = !display?.primaryControlRef || state.visionInFlight;
                ui.visionControl.setAttribute('aria-disabled', String(ui.visionControl.disabled));
                ui.visionControl.title = state.visionInFlight
                  ? 'Vision · Working'
                  : label + (display?.primaryLabel ? ' — ' + display.primaryLabel : '');
                if (ui.visionControlStatus) {
                  ui.visionControlStatus.textContent = String(display?.detail ?? 'Vision availability is checking.');
                }
                renderQuickActions();
              };

              const setCompanionStatus = (stateName, text, model = '') => {
                if (!ui.companionStatus) return;
                ui.companionStatus.dataset.state = stateName;
                ui.companionStatusText.textContent = text;
                ui.companionStatusModel.textContent = model;
              };

              const contextFrameUrlFor = (pageUrl) => {
                const page = new URL(pageUrl);
                if (page.protocol !== 'https:' || page.port || !browser.admittedHosts.includes(page.hostname)) return null;
                return 'http://' + page.hostname + browser.frameHostSuffix + ':' + browser.frameHostPort + page.pathname + page.search + page.hash;
              };

              const admitPageAddress = (value) => {
                let text = String(value ?? '').trim();
                if (!text) throw new Error('Type a web address.');
                if (!/^[a-z][a-z0-9+.-]*:/iu.test(text)) text = 'https://' + text;
                const page = new URL(text);
                if (!browser.humanPageSchemes.includes(page.protocol)) {
                  throw new Error('Only http and https web pages open here.');
                }
                if (page.username || page.password) {
                  throw new Error('Addresses with embedded credentials do not open here.');
                }
                const host = page.hostname.toLowerCase();
                if (
                  browser.rejectedHumanPageHosts.includes(host) ||
                  host.endsWith(browser.rejectedHumanPageHostSuffix)
                ) {
                  throw new Error('The embedded Browser does not navigate to the local Product service.');
                }
                return page.href;
              };

              const normalizeContext = (value) => {
                if (!value || value.type !== browser.messageType || typeof value.pageUrl !== 'string' || typeof value.text !== 'string') return null;
                try {
                  const page = new URL(value.pageUrl);
                  if (page.protocol !== 'https:' || page.port || !browser.admittedHosts.includes(page.hostname)) return null;
                } catch {
                  return null;
                }
                const text = value.text.slice(0, browser.contextCharacterLimit).replace(/\r\n?/gu, '\n').trim();
                if (!text) return null;
                const title = typeof value.title === 'string' && value.title.length > 0
                  ? value.title.slice(0, browser.maxTitleCharacters)
                  : null;
                return Object.freeze({ pageUrl: value.pageUrl, title, text, capture: browser.captureRendered });
              };

              const renderFrameStatus = (text = '', stateName = '') => {
                if (!ui.frameStatus) return;
                ui.frameStatus.textContent = text;
                ui.frameStatus.dataset.frame = stateName;
                ui.frameStatus.hidden = !text;
              };

              // Product-owned navigation is the only place today where the exact identity of the
              // visible page is *earned*: the Product chose the address, so it knows it. Any context
              // captured for the previous page is invalidated because it described another page.
              const navigate = (value) => {
                const pageUrl = admitPageAddress(value);
                const contextFrameUrl = contextFrameUrlFor(pageUrl);

                if (state.browserContext) reportFact('BROWSER_CONTEXT_INVALIDATED', { by: 'navigation' });
                reportFact('BROWSER_PAGE_IDENTITY_OBSERVED', { ...pageRefs(pageUrl), via: 'product-navigation', eligible: String(Boolean(contextFrameUrl)) });
                state.browserContext = null;
                state.includePage = false;
                state.humanPageUrl = pageUrl;
                state.humanFrameLoadExpected = true;
                state.contextTargetPageUrl = contextFrameUrl ? pageUrl : null;
                state.contextLoading = false;

                ui.query.value = pageUrl;
                ui.query.placeholder = 'Type a web address';
                ui.pageFrame.src = pageUrl;
                ui.contextFrame.removeAttribute('src');
                ui.viewportEmpty.hidden = true;
                for (const member of ui.family.querySelectorAll('.family-member')) {
                  if (pageUrl.startsWith(member.dataset.familyScope)) member.setAttribute('aria-current', 'page');
                  else member.removeAttribute('aria-current');
                }
                renderFrameStatus(
                  contextFrameUrl
                    ? 'Opening page directly · Vex page context is available on demand.'
                    : 'Opening page directly · Vex page context is not available for this site yet.',
                  'Loading'
                );
                renderComposer();
              };

              const SCROLL_END_EPSILON = 2;

              const isElementAtBottom = (element) =>
                element.scrollHeight - element.scrollTop - element.clientHeight <= SCROLL_END_EPSILON;

              const reasoningViewport = (key, initialFollowTail = false) => {
                let viewport = state.reasoningViewports.get(key);
                if (!viewport) {
                  viewport = { followTail: initialFollowTail, scrollTop: 0 };
                  state.reasoningViewports.set(key, viewport);
                }
                return viewport;
              };

              const bindReasoningViewport = (element, key, initialFollowTail = false) => {
                element.dataset.scrollKey = key;
                reasoningViewport(key, initialFollowTail);
                element.addEventListener('scroll', () => {
                  const viewport = reasoningViewport(key, initialFollowTail);
                  viewport.scrollTop = element.scrollTop;
                  viewport.followTail = isElementAtBottom(element);
                });
              };

              const applyReasoningViewport = (
                element,
                key,
                initialFollowTail = false,
                restoreReadingPosition = true
              ) => {
                const viewport = reasoningViewport(key, initialFollowTail);
                if (viewport.followTail) {
                  element.scrollTop = element.scrollHeight;
                } else if (restoreReadingPosition) {
                  const maxScrollTop = Math.max(0, element.scrollHeight - element.clientHeight);
                  element.scrollTop = Math.min(viewport.scrollTop, maxScrollTop);
                }
                viewport.scrollTop = element.scrollTop;
              };

              const restoreReasoningViewports = () => {
                ui.timeline.querySelectorAll('.reasoning[data-scroll-key]').forEach((element) => {
                  const key = String(element.dataset.scrollKey ?? '');
                  const liveTail = element.dataset.liveTail === 'true';
                  applyReasoningViewport(element, key, liveTail);
                });
              };

              const resetLiveProcessViewports = () => {
                for (const key of state.reasoningViewports.keys()) {
                  if (key === 'live' || key.startsWith('live.process.')) {
                    state.reasoningViewports.delete(key);
                  }
                }
              };

              const applyTimelineViewport = (restoreReadingPosition = true) => {
                if (state.timelineFollowTail) {
                  ui.timeline.scrollTop = ui.timeline.scrollHeight;
                } else if (restoreReadingPosition) {
                  const maxScrollTop = Math.max(0, ui.timeline.scrollHeight - ui.timeline.clientHeight);
                  ui.timeline.scrollTop = Math.min(state.timelineScrollTop, maxScrollTop);
                }
                state.timelineScrollTop = ui.timeline.scrollTop;
                ui.jumpLatest.hidden = state.timelineFollowTail;
              };

              const nextSequence = () => {
                const messages = state.conversation?.messages ?? [];
                return messages.reduce((max, message) => Math.max(max, Number(message.sequence) || 0), -1) + 1;
              };

              const thoughtMark = () => {
                const mark = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
                mark.setAttribute('class', 'thought-mark');
                mark.setAttribute('viewBox', '0 0 16 16');
                mark.innerHTML = '<path d="M8 1.8 14.2 8 8 14.2 1.8 8z"/><path d="M8 5 11 8 8 11 5 8z"/>';
                return mark;
              };

              // Reach in the person's words: the bridge names handle/action/target, the words are keyed.
              const reachText = (use) => {
                const target = String(use?.target ?? '')
                  .replace(/^home:/u, '')
                  .replace(/^search: /u, '')
                  .replace(/^memory: /u, '');
                const key =
                  use?.handle === 'web' && use?.action === 'search'
                    ? 'workspace.status.reach-web-search'
                    : use?.handle === 'web'
                      ? 'workspace.status.reach-web-fetch'
                      : use?.handle === 'github'
                        ? 'workspace.status.reach-github'
                        : use?.handle === 'home'
                          ? 'workspace.status.reach-home'
                          : use?.handle === 'memory'
                            ? 'workspace.status.reach-memory'
                            : 'workspace.status.reach-atlas';
                const words = t(key).replace('{target}', target);
                if (!use?.state || use.state === 'RETURNED') return words;
                const settled = String(use.state).toLowerCase().replace(/_/gu, ' ');
                return words + ' ' + t('workspace.label.reach-state').replace('{state}', settled);
              };

              const reachList = (uses) => {
                const list = document.createElement('ul');
                list.className = 'reaches live-reaches';
                for (const use of uses) {
                  const line = document.createElement('li');
                  line.textContent = reachText(use);
                  list.append(line);
                }
                return list;
              };

              const cloneReach = (use) => ({
                handle: String(use?.handle ?? ''),
                action: String(use?.action ?? ''),
                target: use?.target == null ? null : String(use.target),
                state: use?.state == null ? null : String(use.state)
              });

              const freezeActiveProcessThought = () => {
                const entry = state.liveProcessEntries[state.liveProcessEntries.length - 1];
                if (entry?.kind === 'thought' && entry.active) entry.active = false;
              };

              const appendProcessReasoning = (delta) => {
                let entry = state.liveProcessEntries[state.liveProcessEntries.length - 1];
                if (entry?.kind !== 'thought' || !entry.active) {
                  entry = { kind: 'thought', text: '', active: true };
                  state.liveProcessEntries.push(entry);
                }
                entry.text += String(delta ?? '');
              };

              const projectProcessReach = (use) => {
                freezeActiveProcessThought();
                const next = cloneReach(use);
                if (next.state) {
                  for (let index = state.liveProcessEntries.length - 1; index >= 0; index -= 1) {
                    const entry = state.liveProcessEntries[index];
                    if (
                      entry?.kind === 'reach' &&
                      !entry.use.state &&
                      entry.use.handle === next.handle &&
                      entry.use.action === next.action &&
                      entry.use.target === next.target
                    ) {
                      entry.use = next;
                      return;
                    }
                  }
                }
                state.liveProcessEntries.push({ kind: 'reach', use: next });
              };

              const freezeLiveProcess = () => {
                freezeActiveProcessThought();
              };

              const snapshotLiveProcess = () =>
                state.liveProcessEntries.map((entry) =>
                  entry.kind === 'thought'
                    ? { kind: 'thought', text: entry.text, active: false }
                    : { kind: 'reach', use: cloneReach(entry.use) });

              const clearLiveProcessState = () => {
                state.streamPhase = null;
                state.streamedContent = '';
                state.streamedReasoning = '';
                state.liveReaches = [];
                state.liveProcessEntries = [];
                state.liveProcessExpanded = true;
                state.liveProcessManuallyToggled = false;
                resetLiveProcessViewports();
              };

              const failureRouteLabel = (route) =>
                route === 'RESUME_LAST_VALID_HEAD_THEN_FORM_NEW_SEND_ATTEMPT'
                  ? 'Try again from the last completed turn with a new Send.'
                  : route === 'FORM_NEW_SEND_ATTEMPT_AND_RETRY'
                    ? 'Try again as a new Send.'
                    : route
                      ? 'Next: ' + String(route).replaceAll('_', ' ').toLowerCase() + '.'
                      : 'This Send stopped before Vex completed a reply.';

              const processEntryNode = (entry, viewportKey, live) => {
                if (entry.kind === 'thought') {
                  if (!state.thoughtsVisible || !String(entry.text ?? '').trim()) return null;
                  const box = document.createElement('div');
                  box.className = 'reasoning process-thought' + (live && entry.active ? ' live-reasoning' : '');
                  box.dataset.liveTail = String(Boolean(live && entry.active));
                  bindReasoningViewport(box, viewportKey, live && entry.active);
                  // The reasoning viewport remains the scroll owner. TextPresentation lives inside
                  // that mounted viewport so Markdown readability can evolve without replacing the
                  // scrollbar/pointer surface or changing the raw reasoning owned by the stream.
                  box.append(textPresentationBody(entry.text));
                  return box;
                }

                const use = entry.use;
                const line = document.createElement('div');
                line.className = 'process-action';
                line.dataset.state = use?.state ? String(use.state) : 'ACTIVE';
                const mark = document.createElement('span');
                mark.className = 'process-action-mark';
                mark.setAttribute('aria-hidden', 'true');
                mark.textContent = !use?.state ? '↳' : use.state === 'RETURNED' ? '✓' : '!';
                const text = document.createElement('span');
                text.className = 'process-action-text';
                text.textContent = reachText(use);
                line.append(mark, text);
                return line;
              };

              const processHistoryNode = (entries, keyPrefix, live) => {
                const history = document.createElement('div');
                history.className = 'process-history';
                entries.forEach((entry, index) => {
                  const node = processEntryNode(entry, keyPrefix + '.' + String(index), live);
                  if (node) history.append(node);
                });
                return history.childElementCount ? history : null;
              };

              const syncLiveProcessEntry = (node, entry, viewportKey) => {
                if (entry.kind === 'thought') {
                  const active = Boolean(entry.active);
                  node.classList.toggle('live-reasoning', active);
                  node.dataset.liveTail = String(active);
                  let body = node.querySelector('.text-presentation');
                  if (!body) {
                    body = textPresentationBody(entry.text);
                    node.append(body);
                  }
                  // Regression boundary: T004 established that human scrolling owns follow state.
                  // Keep this exact reasoning viewport mounted. Only its inner TextPresentation
                  // changes. If the human is following the tail, restore the tail after geometry
                  // changes; if the human scrolled away, leave scrollTop untouched.
                  if (active) {
                    scheduleLiveTextPresentation(
                      body,
                      entry.text,
                      () => {
                        applyReasoningViewport(node, viewportKey, true, false);
                        applyTimelineViewport(false);
                      }
                    );
                  } else {
                    presentText(body, entry.text);
                  }
                  return;
                }

                const use = entry.use;
                node.dataset.state = use?.state ? String(use.state) : 'ACTIVE';
                const mark = node.querySelector('.process-action-mark');
                if (mark) mark.textContent = !use?.state ? '↳' : use.state === 'RETURNED' ? '✓' : '!';
                const text = node.querySelector('.process-action-text');
                if (text) text.textContent = reachText(use);
              };

              const syncLiveProcessHistory = (history) => {
                const visible = new Set();
                state.liveProcessEntries.forEach((entry, index) => {
                  if (entry.kind === 'thought' && (!state.thoughtsVisible || !String(entry.text ?? '').trim())) {
                    return;
                  }
                  const key = String(index);
                  const viewportKey = 'live.process.' + key;
                  let node = history.querySelector('[data-process-entry="' + key + '"]');
                  if (!node) {
                    node = processEntryNode(entry, viewportKey, true);
                    if (!node) return;
                    node.dataset.processEntry = key;
                    history.append(node);
                  }
                  syncLiveProcessEntry(node, entry, viewportKey);
                  visible.add(key);
                });

                // Removal is only for a human visibility-control change. Normal streaming is
                // append/update-only, so older process containers retain stable DOM identity.
                history.querySelectorAll('[data-process-entry]').forEach((node) => {
                  if (!visible.has(String(node.dataset.processEntry ?? ''))) node.remove();
                });
              };

              const createLiveProcessPanel = () => {
                const panel = document.createElement('div');
                panel.className = 'live-process-panel';

                const toggle = document.createElement('button');
                toggle.type = 'button';
                toggle.className = 'thought-toggle live-process-toggle';
                toggle.dataset.liveProcess = 'true';
                toggle.hidden = true;
                toggle.append(thoughtMark(), document.createElement('span'));

                const history = document.createElement('div');
                history.className = 'process-history live-process-history';

                panel.append(toggle, history);
                return panel;
              };

              const syncLiveProcessPanel = (thought) => {
                let panel = thought.querySelector('.live-process-panel');
                if (!panel) {
                  panel = createLiveProcessPanel();
                  thought.append(panel);
                }

                const hasEntries = state.liveProcessEntries.length > 0;
                panel.hidden = !hasEntries;
                if (!hasEntries) return;

                const collapseWithAnswer = Boolean(state.streamedContent);
                const expanded = collapseWithAnswer ? state.liveProcessExpanded : true;
                const toggle = panel.querySelector('.live-process-toggle');
                if (toggle) {
                  toggle.hidden = !collapseWithAnswer;
                  toggle.setAttribute('aria-expanded', String(expanded));
                  const label = toggle.querySelector('span');
                  if (label) label.textContent = expanded ? 'hide process' : 'view process';
                }

                const history = panel.querySelector('.live-process-history');
                if (!history) return;

                // Collapse is visual only. Keep the process DOM mounted so opening it again does
                // not recreate scrollable children or cut sibling observers such as Speech/Kokoro.
                history.hidden = !expanded;
                if (expanded) syncLiveProcessHistory(history);
              };

              const reachedLine = (message) => {
                if (message.role !== 'vex') return null;
                const key = String(message.messageRef ?? '');
                if (state.processEntriesByResponse.has(key)) return null;
                const uses = state.reachesByResponse.get(key);
                if (!uses || !uses.length) return null;
                const line = document.createElement('p');
                line.className = 'receipt-line reached-line';
                line.textContent = t('workspace.label.reached') + ': ' + uses.map(reachText).join(' · ');
                return line;
              };

              const completedThoughtNode = (message) => {
                if (message.role !== 'vex') return null;
                // Durable Conversation responses use messageRef. Private responses intentionally do
                // not become Conversation messages, so their existing eventRef is enough as a
                // presentation-local key for the same ephemeral thought UI.
                const key = String(message.messageRef ?? message.eventRef ?? '');
                const entries = state.processEntriesByResponse.get(key) ?? [];
                const reasoning = state.thoughtsVisible ? state.humanReasoningByResponse.get(key) : null;
                const hasVisibleProcess =
                  entries.some((entry) => entry.kind === 'reach' || (state.thoughtsVisible && String(entry.text ?? '').trim()));
                if (!hasVisibleProcess && !reasoning) return null;

                const holder = document.createElement('div');
                holder.className = 'thought';
                const expanded = state.expandedThoughts.has(key);
                const toggle = document.createElement('button');
                toggle.type = 'button';
                toggle.className = 'thought-toggle';
                toggle.dataset.thoughtKey = key;
                toggle.setAttribute('aria-expanded', String(expanded));
                const label = document.createElement('span');
                label.textContent = expanded ? 'hide process' : 'view process';
                toggle.append(thoughtMark(), label);
                holder.append(toggle);

                if (expanded) {
                  const process = entries.length
                    ? processHistoryNode(entries, 'response.' + key + '.process', false)
                    : null;
                  if (process) {
                    holder.append(process);
                  } else if (reasoning) {
                    // Non-streaming/idempotent recovery may have only response-local reasoning.
                    // Keep that truthful fallback without inventing missing process boundaries.
                    const box = document.createElement('div');
                    box.className = 'reasoning';
                    bindReasoningViewport(box, 'response.' + key, false);
                    box.append(textPresentationBody(reasoning));
                    holder.append(box);
                  }
                }
                return holder;
              };

              // Current TextPresentation interpretation grammar: Markdown.
              // Pattern recognition produces detached DOM nodes using createElement/textContent only;
              // it never creates HTML markup strings. The same grammar can serve reply and reasoning
              // text because custody stays upstream and each host surface keeps its own scroll owner.
              const markdownElement = (tag, className, children) => {
                const node = document.createElement(tag);
                if (className) node.className = className;
                for (const child of children || []) node.append(child);
                return node;
              };

              const markdownSafeHref = (url) => /^(https?:\/\/|mailto:)/i.test(url) ? url : null;

              const markdownWordChar = (character) => Boolean(character) && /[\p{L}\p{N}]/u.test(character);

              const markdownInlinePatterns = [
                { kind: 'code', re: /``([^\n]+?)``/ },
                { kind: 'code', re: /`([^`\n]+)`/ },
                { kind: 'link', re: /\[([^\]\n]+)\]\(([^()\s]+(?:\([^()\s]*\))?[^()\s]*)\)/ },
                { kind: 'url', re: /https?:\/\/[^\s<>()\[\]{}"']+/ },
                { kind: 'strong', re: /\*\*(?=\S)([^\n]*?\S)\*\*/ },
                { kind: 'strong', re: /__(?=\S)([^\n]*?\S)__/, word: true },
                { kind: 'del', re: /~~(?=\S)([^\n]*?\S)~~/ },
                { kind: 'em', re: /\*(?=[^\s*])([^*\n]*?[^\s*])\*/ },
                { kind: 'em', re: /_(?=[^\s_])([^_\n]*?[^\s_])_/, word: true }
              ];

              const markdownInlineNodes = (text, depth) => {
                const nodes = [];
                let rest = String(text);
                let guard = 0;
                while (rest && guard < 10000) {
                  guard += 1;
                  let best = null;
                  for (const rule of markdownInlinePatterns) {
                    const match = rule.re.exec(rest);
                    if (!match) continue;
                    if (rule.word) {
                      // snake_case and __init__ stay text: an underscore delimiter needs a boundary.
                      const before = rest.charAt(match.index - 1);
                      const after = rest.charAt(match.index + match[0].length);
                      if (markdownWordChar(before) || markdownWordChar(after)) continue;
                    }
                    if (!best || match.index < best.match.index) best = { rule, match };
                  }
                  if (!best || (depth || 0) > 8) {
                    nodes.push(document.createTextNode(rest));
                    break;
                  }
                  const { rule, match } = best;
                  if (match.index > 0) nodes.push(document.createTextNode(rest.slice(0, match.index)));
                  if (rule.kind === 'code') {
                    const code = document.createElement('code');
                    code.textContent = match[1];
                    nodes.push(code);
                  } else if (rule.kind === 'link') {
                    const href = markdownSafeHref(match[2]);
                    if (href) {
                      const link = markdownElement('a', null, markdownInlineNodes(match[1], (depth || 0) + 1));
                      link.href = href;
                      link.target = '_blank';
                      link.rel = 'noopener noreferrer';
                      nodes.push(link);
                    } else {
                      nodes.push(document.createTextNode(match[0]));
                    }
                  } else if (rule.kind === 'url') {
                    let url = match[0];
                    while (/[.,;:!?)]/.test(url.charAt(url.length - 1))) url = url.slice(0, -1);
                    const link = document.createElement('a');
                    link.href = url;
                    link.target = '_blank';
                    link.rel = 'noopener noreferrer';
                    link.textContent = url;
                    nodes.push(link);
                    rest = rest.slice(match.index + url.length);
                    continue;
                  } else {
                    nodes.push(markdownElement(rule.kind, null, markdownInlineNodes(match[1], (depth || 0) + 1)));
                  }
                  rest = rest.slice(match.index + match[0].length);
                }
                return nodes;
              };

              const markdownSplitRow = (line) => {
                let text = line.trim();
                if (text.startsWith('|')) text = text.slice(1);
                if (text.endsWith('|') && !text.endsWith('\\|')) text = text.slice(0, -1);
                const cells = [];
                let current = '';
                for (let index = 0; index < text.length; index += 1) {
                  const character = text.charAt(index);
                  if (character === '\\' && text.charAt(index + 1) === '|') {
                    current += '|';
                    index += 1;
                  } else if (character === '|') {
                    cells.push(current.trim());
                    current = '';
                  } else {
                    current += character;
                  }
                }
                cells.push(current.trim());
                return cells;
              };

              const markdownFullMatch = (re, line) => {
                const match = line.match(re);
                return match && match[0].length === line.length ? match : null;
              };

              const markdownTableSeparator = (line) =>
                Boolean(line) && line.includes('-') && Boolean(markdownFullMatch(/\s*\|?(?:\s*:?-+:?\s*\|)*\s*:?-+:?\s*\|?\s*/, line));

              const markdownFence = /^\s{0,3}(`{3,}|~{3,})\s*([\w+#.-]*)/;
              const markdownHeading = /^\s{0,3}(#{1,6})\s+(.*)/;
              const markdownRule = /\s{0,3}([-*_])(?:\s*\1){2,}\s*/;
              const markdownItem = /^(\s*)([-*+]|\d{1,9}[.)])\s+(.*)/;
              const markdownQuote = /^\s{0,3}>\s?(.*)/;

              const markdownStartsBlock = (line, next) =>
                markdownFence.test(line) || markdownHeading.test(line) || Boolean(markdownFullMatch(markdownRule, line)) ||
                markdownQuote.test(line) || markdownItem.test(line) ||
                (line.includes('|') && markdownTableSeparator(next));

              const markdownBlockNodes = (source, depth) => {
                const lines = String(source).replace(/\r\n?/g, '\n').split('\n');
                const out = [];
                let index = 0;
                const blank = (line) => !line || !line.trim();
                if (depth > 6) return [markdownElement('p', null, [document.createTextNode(String(source))])];
                while (index < lines.length) {
                  const line = lines[index];
                  if (blank(line)) {
                    index += 1;
                    continue;
                  }
                  let match = line.match(markdownFence);
                  if (match) {
                    const marker = match[1];
                    const body = [];
                    index += 1;
                    while (index < lines.length && !lines[index].trim().startsWith(marker)) {
                      body.push(lines[index]);
                      index += 1;
                    }
                    index += 1;
                    const code = document.createElement('code');
                    if (match[2]) code.dataset.language = match[2];
                    code.textContent = body.join('\n');
                    out.push(markdownElement('pre', null, [code]));
                    continue;
                  }
                  match = line.match(markdownHeading);
                  if (match) {
                    const level = match[1].length;
                    const heading = markdownElement('p', 'prose-heading md-h' + level, markdownInlineNodes(match[2].replace(/\s+#+\s*(?![\s\S])/, ''), 0));
                    heading.setAttribute('role', 'heading');
                    heading.setAttribute('aria-level', String(Math.min(6, level + 2)));
                    out.push(heading);
                    index += 1;
                    continue;
                  }
                  if (markdownFullMatch(markdownRule, line)) {
                    out.push(document.createElement('hr'));
                    index += 1;
                    continue;
                  }
                  if (line.includes('|') && markdownTableSeparator(lines[index + 1])) {
                    const header = markdownSplitRow(line);
                    const aligns = markdownSplitRow(lines[index + 1]).map((cell) =>
                      cell.startsWith(':') && cell.endsWith(':') ? 'center' : cell.endsWith(':') ? 'right' : cell.startsWith(':') ? 'left' : ''
                    );
                    index += 2;
                    const rows = [];
                    while (index < lines.length && !blank(lines[index]) && lines[index].includes('|')) {
                      rows.push(markdownSplitRow(lines[index]));
                      index += 1;
                    }
                    const cell = (tag, text, column) => {
                      const node = markdownElement(tag, null, markdownInlineNodes(text, 0));
                      if (aligns[column]) node.style.textAlign = aligns[column];
                      return node;
                    };
                    const head = markdownElement('thead', null, [markdownElement('tr', null, header.map((text, column) => cell('th', text, column)))]);
                    const body = markdownElement('tbody', null, rows.map((row) =>
                      markdownElement('tr', null, header.map((_, column) => cell('td', row[column] ?? '', column)))
                    ));
                    out.push(markdownElement('table', 'md-table', [head, body]));
                    continue;
                  }
                  match = line.match(markdownQuote);
                  if (match) {
                    const quoted = [];
                    while (index < lines.length && (match = lines[index].match(markdownQuote))) {
                      quoted.push(match[1]);
                      index += 1;
                    }
                    out.push(markdownElement('blockquote', null, markdownBlockNodes(quoted.join('\n'), depth + 1)));
                    continue;
                  }
                  match = line.match(markdownItem);
                  if (match) {
                    const indent = match[1].length;
                    const ordered = /\d/.test(match[2]);
                    const list = document.createElement(ordered ? 'ol' : 'ul');
                    if (ordered) {
                      const start = parseInt(match[2], 10);
                      if (start !== 1) list.start = start;
                    }
                    const items = [];
                    while (index < lines.length) {
                      const current = lines[index];
                      const item = current.match(markdownItem);
                      if (item && item[1].length === indent && /\d/.test(item[2]) === ordered) {
                        items.push([item[3]]);
                        index += 1;
                      } else if (!blank(current) && items.length && current.match(/^\s*/)[0].length > indent) {
                        items[items.length - 1].push(current.slice(Math.min(current.match(/^\s*/)[0].length, indent + 2)));
                        index += 1;
                      } else if (blank(current) && items.length && index + 1 < lines.length) {
                        const following = lines[index + 1];
                        const nextItem = following.match(markdownItem);
                        const continues =
                          (nextItem && nextItem[1].length === indent && /\d/.test(nextItem[2]) === ordered) ||
                          (!blank(following) && following.match(/^\s*/)[0].length > indent);
                        if (!continues) break;
                        items[items.length - 1].push('');
                        index += 1;
                      } else {
                        break;
                      }
                    }
                    for (const itemLines of items) {
                      const content = markdownBlockNodes(itemLines.join('\n'), depth + 1);
                      const li = document.createElement('li');
                      // A one-paragraph item is written inline, as people expect from a short list.
                      if (content.length && content[0].tagName === 'P' && !content[0].classList.contains('prose-heading')) {
                        li.append(...content[0].childNodes);
                        li.append(...content.slice(1));
                      } else {
                        li.append(...content);
                      }
                      list.append(li);
                    }
                    out.push(list);
                    continue;
                  }
                  const paragraph = [];
                  while (index < lines.length && !blank(lines[index]) &&
                    (!paragraph.length || !markdownStartsBlock(lines[index], lines[index + 1]))) {
                    paragraph.push(lines[index]);
                    index += 1;
                  }
                  const node = document.createElement('p');
                  paragraph.forEach((text, position) => {
                    if (position > 0) node.append(document.createElement('br'));
                    node.append(...markdownInlineNodes(text.trim(), 0));
                  });
                  out.push(node);
                }
                return out;
              };

              // VexArchitecture TextPresentation boundary:
              // - exact text remains owned by the relationship that produced it: canonical/live
              //   response text stays with Conversation/Send, ephemeral reasoning stays session-only;
              // - TextPresentation only asks how that exact text should become human-readable;
              // - one interpretation grammar recognizes presentation patterns in the source text;
              // - current Web realization uses Markdown patterns to form visible text structure;
              // - applying that presentation keeps compatible mounted nodes and changes only the
              //   smallest incompatible visible subtree;
              // - each host surface keeps its own interaction owner: timeline scroll for replies,
              //   reasoningViewport for thinking/process text.
              //
              // TextPresentation names the reusable relationship, not a new truth owner.
              // Markdown is one interpretation grammar; DOM is one platform manifestation.
              // RESPONSE TEXT != REASONING TEXT, even when both use the same TextPresentation.
              //
              // EVOLUTION / REUSE NOTE FOR FUTURE IMPLEMENTERS:
              // Do not duplicate this Web DOM machinery into Android, iOS, another template system,
              // or a non-Markdown presentation just because those realizations do not exist yet.
              // Start from TextPresentation: exact owner-provided text -> recognized presentation
              // meaning -> platform-specific manifestation. Reuse that relationship and keep each
              // platform's concrete realization local. If two or more real consumers need the same
              // presentation structure, that repeated pressure may earn a shared typed
              // TextPresentation structure above the platform renderers. Until then, do not invent
              // a portable AST or make this Web implementation the cross-platform authority.
              //
              // This is not Relay/convergence: no second independently-owned truth is composed here.
              // DOM identity and presentedTextByBody are presentation-local realization/currentness only.
              const sameTextPresentationKind = (current, projected) =>
                current.nodeType === projected.nodeType &&
                (current.nodeType !== 1 || current.tagName === projected.tagName);

              const applyTextPresentationAttributes = (current, projected) => {
                const next = new Map(
                  Array.from(projected.attributes).map((attribute) => [attribute.name, attribute.value])
                );
                for (const attribute of Array.from(current.attributes)) {
                  if (!next.has(attribute.name)) current.removeAttribute(attribute.name);
                }
                for (const [name, value] of next) {
                  if (current.getAttribute(name) !== value) current.setAttribute(name, value);
                }
              };

              const applyTextPresentationNode = (parent, current, projected) => {
                if (!sameTextPresentationKind(current, projected)) {
                  parent.insertBefore(projected, current);
                  current.remove();
                  return projected;
                }
                if (current.nodeType === 3) {
                  if (current.nodeValue !== projected.nodeValue) current.nodeValue = projected.nodeValue;
                  return current;
                }
                applyTextPresentationAttributes(current, projected);
                applyTextPresentationChildren(current, Array.from(projected.childNodes));
                return current;
              };

              const applyTextPresentationChildren = (parent, projectedChildren) => {
                let index = 0;
                for (const projected of projectedChildren) {
                  const current = parent.childNodes[index] || null;
                  if (current) applyTextPresentationNode(parent, current, projected);
                  else parent.append(projected);
                  index += 1;
                }
                while (parent.childNodes.length > projectedChildren.length) {
                  parent.childNodes[parent.childNodes.length - 1].remove();
                }
              };

              const presentedTextByBody = new WeakMap();

              const presentText = (container, source) => {
                const text = String(source ?? '');
                if (presentedTextByBody.get(container) === text) return;
                try {
                  // Reconsider presentation from the exact current source while mutating only the
                  // nested DOM subtrees whose projected structure or content actually changed.
                  applyTextPresentationChildren(container, markdownBlockNodes(text, 0));
                  container.removeAttribute('data-markdown-fallback');
                } catch {
                  // Presentation failure must never erase answer text that already exists.
                  container.replaceChildren(document.createTextNode(text));
                  container.setAttribute('data-markdown-fallback', 'true');
                }
                presentedTextByBody.set(container, text);
              };

              const textPresentationBody = (source) => {
                const body = document.createElement('div');
                body.className = 'text-presentation';
                presentText(body, source);
                return body;
              };

              // Live TextPresentation is frame-batched across all mounted surfaces. The queue is
              // presentation-local only: it does not own source currentness or scroll intent.
              // Each queued surface supplies its own afterPresent continuation so the rightful
              // interaction owner can respond to changed geometry without being collapsed together.
              let liveTextPresentationFrame = 0;
              const liveTextPresentationQueue = new Map();
              const scheduleLiveTextPresentation = (body, source, afterPresent = null) => {
                liveTextPresentationQueue.set(body, { source: String(source ?? ''), afterPresent });
                if (liveTextPresentationFrame) return;
                liveTextPresentationFrame = requestAnimationFrame(() => {
                  liveTextPresentationFrame = 0;
                  const pending = Array.from(liveTextPresentationQueue.entries());
                  liveTextPresentationQueue.clear();
                  for (const [target, presentation] of pending) {
                    if (!target?.isConnected) continue;
                    presentText(target, presentation.source);
                    if (typeof presentation.afterPresent === 'function') presentation.afterPresent();
                  }
                });
              };
              const messageNode = (message) => {
                const item = document.createElement('li');
                const human = message.role === 'human';
                item.className = 'message ' + (human ? 'message-you' : 'message-vex');
                item.dataset.eventRef = String(message.eventRef ?? '');

                const index = document.createElement('p');
                index.className = 'message-index';
                index.textContent = '[' + String(Number(message.sequence) || 0).padStart(2, '0') + ']';

                const bubble = document.createElement('div');
                bubble.className = 'bubble';
                if (human) {
                  // What the person wrote is shown exactly as written.
                  const paragraph = document.createElement('p');
                  paragraph.textContent = String(message.content ?? '');
                  bubble.append(paragraph);
                } else {
                  bubble.append(textPresentationBody(message.content));
                }

                const speaker = document.createElement('p');
                speaker.className = 'speaker';
                speaker.textContent = human ? 'You' : 'Vex';

                const thought = completedThoughtNode(message);
                if (thought) item.append(index, thought, bubble, speaker);
                else item.append(index, bubble, speaker);
                const reached = reachedLine(message);
                if (reached) item.append(reached);
                return item;
              };

              const waitingNode = (sequence) => {
                const item = document.createElement('li');
                item.className = 'message message-vex waiting';
                item.dataset.hasContent = String(Boolean(state.streamedContent));

                const index = document.createElement('p');
                index.className = 'message-index';
                index.textContent = '[' + String(sequence).padStart(2, '0') + ']';

                const thought = document.createElement('div');
                thought.className = 'thought thinking-now';
                const who = document.createElement('span');
                who.className = 'speaker-inline';
                who.textContent = 'Vex';
                const mark = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
                mark.setAttribute('class', 'thought-mark');
                mark.setAttribute('viewBox', '0 0 16 16');
                mark.innerHTML = '<path d="M8 1.8 14.2 8 8 14.2 1.8 8z"/><path d="M8 5 11 8 8 11 5 8z"/>';
                const label = document.createElement('span');
                label.className = 'thinking-label';
                label.textContent =
                  state.streamPhase === 'CheckingRuntime'
                    ? 'checking Vex…'
                    : state.streamPhase === 'Reconnecting'
                      ? 'reconnecting Vex…'
                      : state.streamPhase === 'Generating' || state.streamPhase === 'Reasoning'
                        ? 'thinking…'
                        : state.streamPhase === 'Responding'
                          ? 'responding…'
                          : 'preparing…';
                thought.append(who, mark, label);
                // Live process DOM is mounted once for this waiting message. Streaming updates
                // mutate/append inside it; they do not replace the panel or existing containers.
                thought.append(createLiveProcessPanel());
                syncLiveProcessPanel(thought);

                const bubble = document.createElement('div');
                bubble.className = 'bubble';
                if (state.streamedContent) bubble.append(textPresentationBody(state.streamedContent));
                const speaker = document.createElement('p');
                speaker.className = 'speaker';
                speaker.textContent = 'Vex';

                item.append(index, thought, bubble, speaker);
                return item;
              };

              const failedAttemptNode = (attempt, sequence) => {
                const item = document.createElement('li');
                item.className = 'message message-vex failed-attempt';
                item.dataset.failedAttemptKey = String(attempt.idempotencyKey ?? '');

                const index = document.createElement('p');
                index.className = 'message-index';
                index.textContent = '[' + String(sequence).padStart(2, '0') + ']';

                const thought = document.createElement('div');
                thought.className = 'thought';
                const who = document.createElement('span');
                who.className = 'speaker-inline';
                who.textContent = 'Vex';
                const mark = thoughtMark();
                const label = document.createElement('span');
                label.className = 'thinking-label';
                label.textContent = 'stopped before finishing';
                thought.append(who, mark, label);

                const history =
                  processHistoryNode(
                    attempt.processEntries ?? [],
                    'failed.' + String(attempt.idempotencyKey ?? ''),
                    false
                  );
                if (history) thought.append(history);

                const bubble = document.createElement('div');
                bubble.className = 'bubble';
                const detail = document.createElement('p');
                detail.textContent =
                  attempt.cancelledByPerson
                    ? t('workspace.status.conversation-stopped')
                    : String(attempt.failureCode ?? 'TURN_FAILED') +
                      ' · ' +
                      failureRouteLabel(attempt.exactNextSafeRoute);
                bubble.append(detail);

                const retry = document.createElement('button');
                retry.type = 'button';
                retry.className = 'thought-toggle failed-attempt-retry';
                retry.dataset.failedAttemptKey = String(attempt.idempotencyKey ?? '');
                retry.textContent = 'Try Again';
                retry.disabled = state.sendInFlight;
                bubble.append(retry);

                const speaker = document.createElement('p');
                speaker.className = 'speaker';
                speaker.textContent = 'Vex';

                item.append(index, thought, bubble, speaker);
                return item;
              };

              const renderTimeline = () => {
                if (!ui.timeline) return;
                ui.timeline.dataset.private = String(isPrivate());
                if (isPrivate()) {
                  const exchanges = state.privateMode?.exchanges ?? [];
                  const nodes = exchanges.flatMap((exchange, index) => {
                    const humanKey = 'private.' + (index * 2 + 1);
                    const vexKey = 'private.' + (index * 2 + 2);
                    const reasoning = String(exchange.sealedHumanReasoning ?? '').trim();
                    if (reasoning) {
                      state.humanReasoningByResponse.set(vexKey, reasoning);
                    } else {
                      state.humanReasoningByResponse.delete(vexKey);
                      state.expandedThoughts.delete(vexKey);
                    }
                    return [
                      messageNode({ role: 'human', content: exchange.human, sequence: index * 2 + 1, eventRef: humanKey }),
                      messageNode({ role: 'vex', content: exchange.vex, sequence: index * 2 + 2, eventRef: vexKey })
                    ];
                  });
                  if (state.privatePending) {
                    nodes.push(messageNode({ role: 'human', content: state.privatePending, sequence: exchanges.length * 2 + 1, eventRef: 'private.pending' }));
                    nodes.push(waitingNode(exchanges.length * 2 + 2));
                  }
                  if (!nodes.length) {
                    const empty = document.createElement('li');
                    empty.className = 'timeline-empty';
                    const heading = document.createElement('p');
                    heading.className = 'timeline-empty-heading';
                    heading.textContent = 'Private conversation';
                    const note = document.createElement('p');
                    note.textContent = 'Nothing you say here is kept. Vex still knows what it knew.';
                    empty.append(heading, note);
                    nodes.push(empty);
                  }
                  ui.timeline.replaceChildren(...nodes);
                  applyTimelineViewport();
                  markReading();
                  return;
                }
                for (const key of Array.from(state.humanReasoningByResponse.keys())) {
                  if (!key.startsWith('private.')) continue;
                  state.humanReasoningByResponse.delete(key);
                  state.expandedThoughts.delete(key);
                  state.reasoningViewports.delete('response.' + key);
                }
                const messages = state.conversation?.messages ?? [];
                const nodes = messages.map(messageNode);
                let sequence = nextSequence();

                for (const attempt of state.failedAttempts) {
                  nodes.push(messageNode({
                    role: 'human',
                    content: attempt.content,
                    sequence,
                    eventRef: 'presentation.failed.' + attempt.idempotencyKey
                  }));
                  sequence += 1;
                  nodes.push(failedAttemptNode(attempt, sequence));
                  sequence += 1;
                }

                if (state.pending) {
                  nodes.push(messageNode({
                    role: 'human',
                    content: state.pending.content,
                    sequence,
                    eventRef: 'presentation.pending.' + state.pending.idempotencyKey
                  }));
                  nodes.push(waitingNode(sequence + 1));
                }

                if (!nodes.length) {
                  const empty = document.createElement('li');
                  empty.className = 'timeline-empty';
                  const heading = document.createElement('p');
                  heading.className = 'timeline-empty-heading';
                  heading.textContent = 'Converse with Vex';
                  const note = document.createElement('p');
                  note.textContent = 'Include the page to hand Vex its text. Enter sends; Shift+Enter starts a new line.';
                  empty.append(heading, note);
                  nodes.push(empty);
                }

                ui.timeline.replaceChildren(...nodes);
                restoreReasoningViewports();
                applyTimelineViewport();
                markReading();
              };

              const updateLiveTurn = () => {
                const item = ui.timeline.querySelector('.message.waiting');
                if (!item) {
                  renderTimeline();
                  return;
                }

                item.dataset.hasContent = String(Boolean(state.streamedContent));
                const label = item.querySelector('.thinking-label');
                if (label) {
                  label.textContent =
                    state.streamPhase === 'CheckingRuntime'
                      ? 'checking Vex…'
                      : state.streamPhase === 'Reconnecting'
                        ? 'reconnecting Vex…'
                        : state.streamPhase === 'Generating' || state.streamPhase === 'Reasoning'
                          ? 'thinking…'
                          : state.streamPhase === 'Responding'
                            ? 'responding…'
                            : 'preparing…';
                }

                const thought = item.querySelector('.thinking-now');
                // Preserve live DOM identity while Vex streams. This is a continuation of the
                // T004 scroll-ownership fix: state restoration cannot compensate for destroying
                // the element a human is actively scrolling/dragging.
                if (thought) syncLiveProcessPanel(thought);

                const bubble = item.querySelector('.bubble');
                let body = bubble?.querySelector('.text-presentation');
                if (state.streamedContent) {
                  if (!body && bubble) {
                    body = textPresentationBody('');
                    bubble.append(body);
                  }
                  if (body) scheduleLiveTextPresentation(body, state.streamedContent, () => applyTimelineViewport(false));
                } else if (body) {
                  body.remove();
                }

                applyTimelineViewport(false);
              };

              const renderTicket = () => {
                const context = state.browserContext;
                const visible = state.includePage && context;
                ui.ticket.hidden = !visible;
                if (!visible) return;
                ui.ticket.dataset.state = 'Ready';
                ui.ticketTitle.textContent = context.title || 'Included page';
                ui.ticketLine.textContent = 'A separately rendered snapshot of this entered URL will be given to Vex on the next turn.';
                ui.ticketUrl.textContent = context.pageUrl;
                ui.ticketHash.textContent = '';
                ui.ticketView.dataset.contextReady = 'true';
              };

              // Private mode: semantics and copy come from the service (VexLifePrivateModeFamily). The
              // private transcript exists only in the running service; this renderer only shows it.
              const isPrivate = () => state.privateMode?.state === 'PRIVATE';

              const renderPrivate = () => {
                const display = state.privateMode?.display;
                const privateNow = isPrivate();
                ui.privateMode.setAttribute('aria-pressed', String(privateNow));
                ui.privateMode.dataset.private = String(privateNow);
                ui.privateModeLabel.textContent = String(display?.label ?? 'Kept in your archives');
                ui.privateMode.title = String(display?.detail ?? '') + (display?.primaryLabel ? ' (' + display.primaryLabel + ')' : '');
                ui.privateMode.disabled = !display || state.privateInFlight;
                root.dataset.vexPrivate = String(privateNow);
                ui.privateBanner.hidden = !privateNow;
                if (!privateNow) return;
                ui.privateBannerText.textContent = String(display?.detail ?? '');
                const notes = (state.privateMode?.keptNotes ?? []).map((note) => {
                  const item = document.createElement('li');
                  item.textContent = 'Kept: ' + note;
                  return item;
                });
                ui.privateNotes.replaceChildren(...notes);
                const proposal = state.privateMode?.proposedNoteOrNull;
                ui.privateProposal.hidden = !proposal;
                ui.privateProposalText.textContent = proposal ? 'Vex would keep: “' + proposal + '”' : '';
                ui.privatePropose.hidden = Boolean(proposal);
                ui.privatePropose.disabled = state.privateInFlight || !(state.privateMode?.exchanges ?? []).length;
                ui.privateKeep.disabled = state.privateInFlight;
                ui.privateDecline.disabled = state.privateInFlight;
              };

              const loadPrivate = async () => {
                const response = await fetch(service.privatePath, { headers: { accept: 'application/json' }, cache: 'no-store' });
                const wasPrivate = isPrivate();
                state.privateMode = await readJson(response);
                renderPrivate();
                if (wasPrivate !== isPrivate()) renderTimeline();
                if (state.app === apps.archives) renderArchives();
                return state.privateMode;
              };

              const privatePost = async (path, body) => {
                state.privateInFlight = true;
                renderPrivate();
                try {
                  const response = await fetch(path, {
                    method: 'POST',
                    headers: { 'content-type': 'application/json', accept: 'application/json' },
                    body: JSON.stringify(body ?? {})
                  });
                  const value = await readJson(response);
                  state.privateMode = value?.private ?? value;
                } finally {
                  state.privateInFlight = false;
                }
                renderPrivate();
                renderTimeline();
                renderComposer();
                if (state.app === apps.archives) renderArchives();
              };

              const togglePrivate = async () => {
                const controlRef = state.privateMode?.display?.primaryControlRef;
                if (!controlRef || state.privateInFlight || state.sendInFlight) return;
                try {
                  await privatePost(service.privateControlPath, {
                    controlRef,
                    requestRef: 'request.vexlife-prod.private.' + crypto.randomUUID().toLowerCase()
                  });
                  notify(isPrivate()
                    ? 'Private mode is on. Nothing here is kept unless you keep a note.'
                    : 'Private mode is off. Vex will know a private conversation happened.');
                } catch (error) {
                  notify(error instanceof Error ? error.message : 'Private mode could not change.');
                  await loadPrivate().catch(() => {});
                }
              };

              const sendPrivate = async () => {
                const content = ui.draft.value.trim();
                if (!content || state.privateInFlight) return;
                state.privatePending = content;
                ui.draft.value = '';
                sizeDraft();
                renderTimeline();
                try {
                  if (speech.settings.enabled) audioContext();
                  await privatePost(service.privateSendPath, { content });
                  const exchanges = state.privateMode?.exchanges ?? [];
                  if (speech.settings.enabled && exchanges.length) {
                    enqueueSpeech(exchanges[exchanges.length - 1].vex, 'private.' + exchanges.length * 2);
                  }
                } catch (error) {
                  if (!ui.draft.value) ui.draft.value = content;
                  sizeDraft();
                  notify(error instanceof Error ? error.message : 'Vex could not reply. Nothing was kept.');
                } finally {
                  state.privatePending = null;
                  renderTimeline();
                  renderComposer();
                }
              };

              // The left pane shows one app at a time. Choosing an app is presentation state only.
              const inSourceTemplate = (pageUrl) =>
                Boolean(pageUrl) && apps.sourceTemplateScopes.some((scope) => String(pageUrl).startsWith(scope));

              const renderApp = () => {
                const app = state.app;
                ui.appSelector.value = app;
                ui.family.hidden = app !== apps.sourceTemplate;
                ui.archives.hidden = app !== apps.archives;
                ui.viewport.hidden = app === apps.archives;
                ui.addressForm.dataset.app = app;
                ui.query.disabled = app === apps.archives;
                if (app === apps.archives) ui.frameStatus.hidden = true;
                for (const member of ui.family.querySelectorAll('.family-member')) {
                  const current = String(state.humanPageUrl ?? '').startsWith(member.dataset.familyScope);
                  if (current) member.setAttribute('aria-current', 'page');
                  else member.removeAttribute('aria-current');
                }
                root.dataset.vexApp = app;
                if (app === apps.archives) renderArchives();
              };

              const archiveDayKey = (date) =>
                date.getFullYear() + '-' + String(date.getMonth() + 1).padStart(2, '0') + '-' + String(date.getDate()).padStart(2, '0');

              const renderArchives = () => {
                if (!ui.archivesTimeline) return;
                const messages = (state.conversation?.messages ?? []).filter((message) => !Number.isNaN(Date.parse(message.formedAt)));
                if (!messages.length) {
                  ui.archivesSummary.textContent = 'Everything you and Vex say is kept here, in your Vex Home, by day.';
                  const empty = document.createElement('li');
                  empty.className = 'archives-empty';
                  empty.textContent = 'Nothing here yet.';
                  ui.archivesTimeline.replaceChildren(empty);
                  return;
                }
                const days = new Map();
                const place = (at, entry) => {
                  const key = archiveDayKey(at);
                  if (!days.has(key)) days.set(key, { date: at, messages: [] });
                  days.get(key).messages.push(entry);
                };
                for (const message of messages) place(new Date(message.formedAt), message);
                for (const interval of state.privateMode?.intervals ?? []) {
                  if (!Number.isNaN(Date.parse(interval.startedAt))) {
                    place(new Date(interval.startedAt), { privateInterval: interval, formedAt: interval.startedAt });
                  }
                }
                for (const day of days.values()) day.messages.sort((a, b) => Date.parse(a.formedAt) - Date.parse(b.formedAt));
                const first = new Date(messages[0].formedAt);
                ui.archivesSummary.textContent =
                  messages.length + ' message' + (messages.length === 1 ? '' : 's') + ' over ' + days.size + ' day' + (days.size === 1 ? '' : 's') +
                  ' since ' + first.toLocaleDateString(undefined, { year: 'numeric', month: 'long', day: 'numeric' }) +
                  ' · kept in your Vex Home';
                const items = [...days.entries()].reverse().map(([key, day]) => {
                  const item = document.createElement('li');
                  item.className = 'archives-day';
                  item.dataset.day = key;
                  const heading = document.createElement('h3');
                  heading.textContent = day.date.toLocaleDateString(undefined, { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' });
                  const count = document.createElement('span');
                  const spoken = day.messages.filter((entry) => !entry.privateInterval).length;
                  count.textContent = spoken + ' message' + (spoken === 1 ? '' : 's');
                  heading.append(count);
                  const entries = document.createElement('ol');
                  entries.className = 'archives-entries';
                  for (const message of day.messages) {
                    const entry = document.createElement('li');
                    if (message.privateInterval) {
                      const interval = message.privateInterval;
                      const from = new Date(interval.startedAt);
                      const to = new Date(interval.endedAt);
                      entry.className = 'archives-entry archives-private';
                      const time = document.createElement('time');
                      time.dateTime = String(interval.startedAt);
                      time.textContent = from.toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' });
                      const who = document.createElement('span');
                      who.className = 'who';
                      who.textContent = 'Private';
                      const text = document.createElement('p');
                      text.textContent =
                        'Private conversation until ' + to.toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' }) +
                        ' · ' + interval.exchangeCount + ' exchange' + (interval.exchangeCount === 1 ? '' : 's') + ' · not kept' +
                        (interval.keptNotes.length ? '\nKept: ' + interval.keptNotes.join('\nKept: ') : '');
                      entry.append(time, who, text);
                      entries.append(entry);
                      continue;
                    }
                    entry.className = 'archives-entry';
                    entry.dataset.eventRef = String(message.eventRef ?? '');
                    const time = document.createElement('time');
                    time.dateTime = String(message.formedAt);
                    time.textContent = new Date(message.formedAt).toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' });
                    const who = document.createElement('span');
                    who.className = 'who';
                    who.textContent = message.role === 'human' ? 'You' : 'Vex';
                    const text = document.createElement('p');
                    text.textContent = String(message.content ?? '');
                    entry.append(time, who, text);
                    entries.append(entry);
                  }
                  item.append(heading, entries);
                  return item;
                });
                ui.archivesTimeline.replaceChildren(...items);
              };

              const chooseApp = (app) => {
                if (!apps.keys.includes(app)) return;
                state.app = app;
                try {
                  localStorage.setItem(apps.storageKey, app);
                } catch {}
                if (app === apps.archives) {
                  state.includePage = false;
                } else if (app === apps.sourceTemplate && !inSourceTemplate(state.humanPageUrl)) {
                  const home = ui.family.querySelector('.family-member')?.dataset.familyHome;
                  if (home) navigate(home);
                }
                renderApp();
                renderComposer();
              };

              const renderComposer = () => {
                const contextEligible = Boolean(state.contextTargetPageUrl);
                const contextReady = Boolean(state.browserContext);
                const archivesOpen = state.app === apps.archives;
                const privateNow = isPrivate();
                const modelSwitching = Boolean(state.models?.switching);
                const modelSwitchHeld =
                  Boolean(state.models?.switchPending) &&
                  state.models?.lastOutcome?.state === 'FAILED';
                const canInclude = !archivesOpen && !privateNow && (contextEligible || contextReady);
                ui.contextShare.disabled = !canInclude || state.contextLoading;
                ui.contextShare.dataset.state = state.contextLoading
                  ? 'Loading'
                  : contextReady
                    ? 'Ready'
                    : contextEligible
                      ? 'Available'
                      : 'Unavailable';
                ui.contextShare.setAttribute('aria-pressed', String(state.includePage && contextReady));
                ui.contextShareLabel.textContent = state.contextLoading
                  ? 'Reading page…'
                  : state.includePage && contextReady
                    ? 'Page included'
                    : 'Include this page';
                ui.composerHold.textContent = modelSwitching
                  ? resolveText(state.models.switching.message)
                  : modelSwitchHeld
                    ? resolveText(state.models.lastOutcome.message)
                    : state.contextLoading
                    ? 'Reading the current page separately for Vex…'
                    : privateNow
                    ? (state.privateInFlight ? 'Vex is replying privately…' : 'Private · nothing here is kept unless you keep a note.')
                  : archivesOpen
                    ? "Vex doesn't see your archives yet."
                  : !canInclude
                    ? 'Vex page context is not available for this site yet.'
                    : contextEligible && !contextReady
                      ? 'Page context is available on demand.'
                      : state.pending && !state.sendInFlight
                        ? 'The prior Send outcome is unresolved. Send again to reconcile that exact attempt.'
                        : state.failedAttempts.length
                          ? 'A prior Send stopped. Its process is preserved here; use Try Again to form a new Send.'
                          : '';
                ui.send.hidden = state.sendInFlight;
                ui.stop.hidden = !state.sendInFlight;
                ui.stop.disabled = !state.sendInFlight || state.sendCancelInFlight;
                const stopLabel = t('workspace.button.conversation-stop');
                ui.stop.textContent = stopLabel;
                ui.stop.setAttribute('aria-label', stopLabel);
                ui.stop.title = stopLabel;
                ui.send.disabled = modelSwitching || modelSwitchHeld || (privateNow
                  ? state.privateInFlight || !ui.draft.value.trim()
                  : state.sendInFlight || (!state.pending && !ui.draft.value.trim()));
                ui.responseProgress.hidden = true;
                renderTicket();
                renderVoiceInput();
              };

              const setMeter = (meter, parts, text, label, known = true) => {
                if (!meter) return;
                meter.dataset.known = String(known);
                meter.setAttribute('aria-label', label);
                parts.forEach(([node, share]) => { if (node) node.style.width = Math.max(0, Math.min(1, share)) * 100 + '%'; });
                const textNode = meter.querySelector('.meter-text');
                if (textNode) textNode.textContent = text;
              };

              const formatBytes = (value) => {
                if (value === null || value === undefined || value === '') return 'Unknown';
                const bytes = Number(value);
                if (!Number.isFinite(bytes) || bytes < 0) return 'Unknown';
                const units = ['B', 'KB', 'MB', 'GB', 'TB'];
                let scaled = bytes;
                let unit = 0;
                while (scaled >= 1024 && unit < units.length - 1) {
                  scaled /= 1024;
                  unit += 1;
                }
                const digits = unit >= 3 ? 1 : (unit === 0 ? 0 : 1);
                return scaled.toFixed(digits) + ' ' + units[unit];
              };

              const renderSettings = () => {
                const prompt = state.context?.promptContext ?? null;
                if (prompt) {
                  const hard = Number(prompt.hardTokenLimit) || 0;
                  const used = Number(prompt.providerMaterializedInputTokenEstimate) || 0;
                  const reserved = Number(prompt.reservedOutputTokens) || 0;
                  const coreShare = hard > 0 ? Math.min(1, used / hard) : 0;
                  const reservedShare = hard > 0 ? Math.min(1 - coreShare, reserved / hard) : 0;
                  setMeter(
                    ui.contextWindow,
                    [[ui.contextCore, coreShare], [ui.contextUsed, reservedShare]],
                    hard > 0 ? used.toLocaleString() + ' input · ' + reserved.toLocaleString() + ' reserved · ' + Math.max(0, hard - used - reserved).toLocaleString() + ' remaining' : 'Context measured',
                    'Prompt context from the current canonical turn',
                    hard > 0
                  );
                  const priorCount = Array.isArray(prompt.selectedConversationEventRefs) ? prompt.selectedConversationEventRefs.length : 0;
                  const memoryCount = Array.isArray(prompt.memorySourceRefs) ? prompt.memorySourceRefs.length : 0;
                  const gaps = Number(prompt.privateConversationsNotRemembered) || 0;
                  const field = prompt.foundationalField ?? null;
                  const fieldParts = field && Array.isArray(field.carriedModules) ? field.carriedModules.length : 0;
                  const fieldLeftOut = field && Array.isArray(field.omittedModuleRefs) ? field.omittedModuleRefs.length : 0;
                  const fieldNote = !field
                    ? ''
                    : field.state === 'HELD'
                      ? ' The foundational field was not carried (' + String(field.heldReason ?? 'UNKNOWN').replaceAll('_', ' ').toLowerCase() + ').'
                      : ' Foundational field ' + String(field.fieldVersion ?? '') + ' was carried: ' + fieldParts + ' part' + (fieldParts === 1 ? '' : 's') +
                        (fieldLeftOut ? ', ' + fieldLeftOut + ' left out for room' : '') +
                        ', about ' + (Number(field.estimatedSystemMessageTokens) || 0).toLocaleString() + ' tokens.';
                  const recordExchanges = prompt.priorCompletedExchangeCount ?? null;
                  const recordNote = recordExchanges === null
                    ? ''
                    : ' of ' + (Number(recordExchanges) * 2).toLocaleString() + ' on record';
                  ui.contextNote.textContent =
                    'Canonical turn receipt: ' + priorCount + ' prior Conversation event' + (priorCount === 1 ? '' : 's') +
                    ' selected' + recordNote + '. Whole-history enumeration: ' + (prompt.wholeHistoryEventEnumerationPerformed ? 'yes' : 'no') + '.' +
                    (prompt.replyLanguageName ? ' Vex was told to reply in ' + prompt.replyLanguageName + '.' : '') +
                    (gaps ? ' It was told about ' + gaps + ' private conversation' + (gaps === 1 ? '' : 's') + ' it does not remember.' : '') +
                    fieldNote;
                  ui.memoryNote.textContent = prompt.memoryIncluded
                    ? 'VexLife Memory contributed through ' + memoryCount + ' source' + (memoryCount === 1 ? '' : 's') + ' to the current prompt.'
                    : 'No VexLife Memory packet was included in the current prompt.';
                } else {
                  setMeter(ui.contextWindow, [[ui.contextCore, 0], [ui.contextUsed, 0]], 'No turn receipt yet', 'No current prompt receipt', false);
                  ui.contextNote.textContent = 'Send a turn to inspect the current New-Beginnings prompt sequencing.';
                  ui.memoryNote.textContent = 'Memory inclusion is reported per completed canonical turn.';
                }

                const current = state.currentState;
                if (current) {
                  const totalMemory = Number(current.totalMemoryBytes);
                  const minimumMemory = Number(current.modelMinimumMemoryBytes);
                  const memoryKnown = Number.isFinite(totalMemory) && totalMemory > 0;
                  const minimumKnown = Number.isFinite(minimumMemory) && minimumMemory >= 0;
                  const minimumShare = memoryKnown && minimumKnown
                    ? Math.max(0, Math.min(1, minimumMemory / totalMemory))
                    : 0;
                  setMeter(
                    ui.memory,
                    [[ui.memoryUsed, minimumShare], [ui.memoryRemaining, memoryKnown ? 1 - minimumShare : 0]],
                    memoryKnown
                      ? formatBytes(totalMemory) + ' system · ' + (minimumKnown ? formatBytes(minimumMemory) + ' current-model minimum' : 'model minimum unknown')
                      : 'System memory unknown',
                    'Fresh host memory observation and current model requirement',
                    memoryKnown
                  );

                  const freeStorage = formatBytes(current.freeStorageBytes);
                  const modelTotal = formatBytes(current.modelArtifactTotalBytes);
                  const modelPresent = formatBytes(current.modelArtifactPresentBySizeBytes);
                  const modelLabel = current.activeModelRef ?? 'No model selected';
                  const modelStorage =
                    current.modelArtifactTotalBytes === null || current.modelArtifactTotalBytes === undefined
                      ? 'No selected-model artifact requirement.'
                      : 'Model files present by declared size: ' + modelPresent + ' of ' + modelTotal +
                        ' (' + String(current.modelArtifactState ?? 'UNKNOWN').replaceAll('_', ' ').toLowerCase() + '). ' +
                        'Exact hashes are not rechecked in this awareness view.';
                  ui.resourceNote.textContent =
                    'Storage available: ' + freeStorage +
                    '. Model: ' + modelLabel + '. ' + modelStorage + ' ' +
                    'Runtime binding: ' + String(current.runtimeState ?? 'UNKNOWN').replaceAll('_', ' ').toLowerCase() +
                    '; currentness: ' + String(current.runtimeCurrentness ?? 'UNKNOWN').replaceAll('_', ' ').toLowerCase() + '.';
                  ui.homeCurrentState.textContent =
                    'Current Home: ' + String(current.homePath ?? 'Unknown') +
                    ' · ' + String(current.homeDisposition ?? 'UNKNOWN').replaceAll('_', ' ').toLowerCase() +
                    ' · ' + String(current.homeRef ?? 'Unknown') + '.';
                } else {
                  setMeter(ui.memory, [[ui.memoryUsed, 0], [ui.memoryRemaining, 0]], 'Not measured', 'Current machine state is unavailable', false);
                  ui.resourceNote.textContent = 'Current machine and model resource truth is unavailable.';
                  ui.homeCurrentState.textContent = 'Current Home has not been observed yet.';
                }

                ui.thoughtVisibility.setAttribute('aria-checked', String(state.thoughtsVisible));
                ui.lowMemoryAlert.setAttribute('aria-checked', 'false');
                ui.lowMemoryAlert.disabled = true;
                ui.alertThreshold.textContent = 'not connected';
              };

              const renderCompanionHead = () => {
                const settings = state.view === 'settings';
                ui.companionTitle.textContent = settings ? 'Settings' : 'Converse with Vex';
                ui.companionStatus.hidden = settings;
                ui.viewSwitch.textContent = settings ? 'Return' : 'Settings';
                ui.viewSwitch.setAttribute('aria-expanded', String(settings));
                ui.conversation.hidden = settings;
                ui.settings.hidden = !settings;
              };

              const render = () => {
                renderCompanionHead();
                renderVision();
                renderTimeline();
                renderComposer();
                renderSettings();
                renderMemory();
              };

              const loadSourceCurrentness = async () => {
                const response = await fetch(service.sourceCurrentnessPath, { headers: { accept: 'application/json' }, cache: 'no-store' });
                state.currentness = await readJson(response);
                root.dataset.vexServiceSourceSha = String(state.currentness.sourceSha ?? '');
                root.dataset.vexServicePid = String(state.currentness.processPid ?? '');
                setCompanionStatus('Ready', 'Ready', ui.companionStatusModel.textContent);
              };

              const loadCurrentState = async () => {
                const response = await fetch(service.currentStatePath, { headers: { accept: 'application/json' }, cache: 'no-store' });
                state.currentState = await readJson(response);
                root.dataset.vexCurrentStateObservedAt = String(state.currentState?.observedAt ?? '');
                renderSettings();
                return state.currentState;
              };

              // Keyed words of the workspace scope, in the person's language. Until they arrive an
              // element shows its key, never an English string written here (localization law).
              const t = (key) => {
                const words = state.workspaceStrings?.[key];
                return typeof words === 'string' ? words : key;
              };

              // Resolves a Family-authored VexLifeText {key, args, gib, parts} in the person's language.
              const resolveText = (text) => {
                if (!text || typeof text.key !== 'string') return '';
                const args = text.args || {};
                const gib = text.gib || {};
                const parts = text.parts || {};
                return t(text.key).replace(/\{([a-zA-Z0-9]+)\}/gu, (whole, name) => {
                  if (name in args) {
                    // Formatting is presentation: an ISO instant is shown in the person's locale.
                    const value = String(args[name]);
                    return /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}/u.test(value)
                      ? new Date(value).toLocaleString(document.documentElement.lang || undefined)
                      : value;
                  }
                  if (name in parts) return resolveText(parts[name]);
                  if (name in gib) return (Number(gib[name]) / 1073741824).toFixed(1);
                  return whole;
                });
              };

              const applyStaticText = () => {
                for (const element of root.querySelectorAll('[data-vex-text]')) {
                  element.textContent = t(element.getAttribute('data-vex-text'));
                }
              };

              const loadWorkspaceStrings = async () => {
                const response = await fetch(service.workspaceStringsPath, { headers: { accept: 'application/json' }, cache: 'no-store' });
                const body = await readJson(response);
                state.workspaceStrings = body?.strings ?? {};
                applyStaticText();
                renderVoiceInput();
                renderDiagnostics();
              };

              // The presentation reports only what it observed itself (what it showed, what it could
              // prove about the visible page). It never reports a Conversation or runtime outcome:
              // those facts belong to their owners. A failed report changes nothing.
              const reportFact = (fact, refs = {}) => {
                try {
                  void fetch(service.chronologyPath, {
                    method: 'POST',
                    headers: { 'content-type': 'application/json' },
                    body: JSON.stringify({ fact, refs })
                  }).catch(() => {});
                } catch {}
              };

              const pageRefs = (pageUrl) => {
                try {
                  const page = new URL(pageUrl);
                  return { host: page.hostname, path: page.pathname };
                } catch {
                  return {};
                }
              };

              const renderDiagnostics = () => {
                if (!ui.diagnosticsStatus) return;
                const projection = state.diagnostics;
                if (!projection) {
                  ui.diagnosticsStatus.textContent = t('workspace.status.diagnostics-unavailable');
                  ui.diagnosticsCapture.textContent = '';
                  ui.diagnosticsRecords.textContent = '';
                  ui.diagnosticsEnhanced.disabled = true;
                  ui.diagnosticsClear.disabled = true;
                  return;
                }
                const display = projection.display;
                ui.diagnosticsStatus.textContent = resolveText(display.status);
                ui.diagnosticsCapture.textContent = resolveText(display.capture);
                ui.diagnosticsRecords.textContent = projection.chronologyRecords > 0 && projection.oldestAt
                  ? t('workspace.label.diagnostics-records')
                      .replace('{count}', String(projection.chronologyRecords))
                      .replace('{since}', new Date(projection.oldestAt).toLocaleString(document.documentElement.lang || undefined))
                  : t('workspace.label.diagnostics-no-records');
                // Buttons carry the Family's Control refs; their words come from the Control's key.
                const enhanced = display.controls.find((control) => control.controlRef === 'START_ENHANCED' || control.controlRef === 'STOP_ENHANCED');
                const clear = display.controls.find((control) => control.controlRef === 'CLEAR_HISTORY');
                ui.diagnosticsEnhanced.hidden = !enhanced;
                if (enhanced) {
                  ui.diagnosticsEnhanced.dataset.controlRef = enhanced.controlRef;
                  ui.diagnosticsEnhanced.textContent = resolveText(enhanced.label);
                }
                ui.diagnosticsEnhanced.disabled = !enhanced || state.diagnosticsInFlight;
                ui.diagnosticsClear.hidden = !clear;
                if (clear) ui.diagnosticsClear.textContent = resolveText(clear.label);
                ui.diagnosticsClear.disabled = !clear || state.diagnosticsInFlight;
              };

              const loadDiagnostics = async () => {
                const response = await fetch(service.diagnosticsPath, { headers: { accept: 'application/json' }, cache: 'no-store' });
                state.diagnostics = await readJson(response);
                renderDiagnostics();
                return state.diagnostics;
              };

              const diagnosticsControl = async (controlRef) => {
                if (state.diagnosticsInFlight) return;
                state.diagnosticsInFlight = true;
                renderDiagnostics();
                try {
                  const response = await fetch(service.diagnosticsControlPath, {
                    method: 'POST',
                    headers: { 'content-type': 'application/json', accept: 'application/json' },
                    body: JSON.stringify({ controlRef })
                  });
                  state.diagnostics = await readJson(response);
                } catch (error) {
                  notify(error instanceof Error ? error.message : t('workspace.status.diagnostics-unavailable'));
                } finally {
                  state.diagnosticsInFlight = false;
                  renderDiagnostics();
                }
              };

              const gibText = (bytes) => (Number(bytes) / 1073741824).toFixed(1);

              const servingModelLabel = () => {
                const serving = (state.models?.models || []).find((model) => model.serving);
                return serving?.label ?? '';
              };

              // Settings → Models. Rows, states and lawful Controls come from VexLifeModelsFamily via
              // the service; this renderer only lays them out and returns the chosen Control ref.
              const renderModels = () => {
                if (!ui.modelsList) return;
                ui.modelsList.textContent = '';
                const projection = state.models;
                if (!projection) return;
                for (const model of projection.models || []) {
                  const row = document.createElement('div');
                  row.className = 'model-row';
                  row.dataset.chosen = String(Boolean(model.chosen));
                  const name = document.createElement('p');
                  name.className = 'model-name';
                  name.textContent = model.label;
                  const facts = document.createElement('p');
                  facts.className = 'model-facts';
                  const status = model.downloadedBytes != null
                    ? t('workspace.status.model-downloading')
                        .replace('{done}', gibText(model.downloadedBytes))
                        .replace('{total}', gibText(model.totalBytes))
                    : resolveText(model.display.status);
                  facts.textContent = status + ' · ' + t('workspace.label.model-facts')
                    .replace('{size}', gibText(model.totalBytes))
                    .replace('{memory}', gibText(model.recommendedMemoryBytes))
                    .replace('{fit}', t(model.fitsThisMac ? 'workspace.label.model-fits' : 'workspace.label.model-tight'));
                  const notes = document.createElement('p');
                  notes.className = 'model-notes';
                  notes.textContent = [
                    ...(model.display.notes || []).map(resolveText),
                    model.standing === 'CANDIDATE' ? t('workspace.label.model-candidate') : ''
                  ].filter(Boolean).join(' · ');
                  const controls = document.createElement('div');
                  controls.className = 'model-controls';
                  for (const control of model.display.controls || []) {
                    const button = document.createElement('button');
                    button.type = 'button';
                    button.className = 'outline-button';
                    button.textContent = resolveText(control.label);
                    button.disabled = state.modelsInFlight || Boolean(projection.switching);
                    button.addEventListener('click', () => { void modelControl(control.controlRef, model.handle); });
                    controls.append(button);
                  }
                  row.append(name, controls, facts, notes);
                  ui.modelsList.append(row);
                }
                const lines = [];
                if (projection.switching) {
                  lines.push(resolveText(projection.switching.message));
                } else if (projection.lastOutcome) {
                  lines.push(resolveText(projection.lastOutcome.message));
                }
                if (projection.switchPending && !projection.switching) {
                  lines.push(t('workspace.status.models-switch-pending'));
                }
                if (projection.freeStorageBytes != null) {
                  lines.push(t('workspace.label.model-free-space').replace('{free}', gibText(projection.freeStorageBytes)));
                }
                if (ui.modelsNote) ui.modelsNote.textContent = lines.join(' ');
                if (projection.switching) {
                  setCompanionStatus(
                    'Checking',
                    resolveText(projection.switching.message),
                    servingModelLabel() || ui.companionStatusModel.textContent
                  );
                } else if (ui.companionStatus?.dataset.state !== 'Unavailable') {
                  setCompanionStatus(
                    'Ready',
                    'Ready',
                    servingModelLabel() || ui.companionStatusModel.textContent
                  );
                }
                renderComposer();
              };

              const loadModels = async () => {
                const response = await fetch(service.modelsPath, { headers: { accept: 'application/json' }, cache: 'no-store' });
                state.models = await readJson(response);
                renderModels();
                // Follow only active model work; nothing polls when the relationship is stable.
                if (state.models?.downloading || state.models?.switching) {
                  window.setTimeout(() => { void loadModels().catch(() => {}); }, 1000);
                }
                return state.models;
              };

              const modelControl = async (controlRef, handle) => {
                if (state.modelsInFlight) return;
                if (controlRef === 'REMOVE_MODEL' && !window.confirm(t('workspace.button.model-remove') + '?')) return;
                state.modelsInFlight = true;
                renderModels();
                try {
                  const response = await fetch(service.modelsControlPath, {
                    method: 'POST',
                    headers: { 'content-type': 'application/json', accept: 'application/json' },
                    body: JSON.stringify({ controlRef, handle })
                  });
                  state.models = await readJson(response);
                  if (controlRef === 'USE_MODEL') {
                    const message = state.models?.switching?.message ?? state.models?.lastOutcome?.message;
                    if (message) notify(resolveText(message));
                  }
                } catch (error) {
                  notify(error instanceof Error ? error.message : 'Models are not available.');
                } finally {
                  state.modelsInFlight = false;
                  renderModels();
                  if (state.models?.downloading || state.models?.switching) {
                    window.setTimeout(() => { void loadModels().catch(() => {}); }, 1000);
                  }
                }
              };

              const loadMemory = async () => {
                const response = await fetch(service.memoryPath, { headers: { accept: 'application/json' }, cache: 'no-store' });
                state.memory = await readJson(response);
                renderMemory();
                return state.memory;
              };

              const loadVision = async () => {
                const response = await fetch(service.visionPath, { headers: { accept: 'application/json' }, cache: 'no-store' });
                state.vision = await readJson(response);
                root.dataset.vexVisionState = String(state.vision?.state ?? 'HELD');
                root.dataset.vexVisionRouteReachable = String(state.vision?.routeReachable === true);
                renderVision();
                return state.vision;
              };

              const activateVision = async () => {
                if (state.visionInFlight) return;
                const controlRef = state.vision?.display?.primaryControlRef;
                if (!controlRef) {
                  state.view = 'conversation';
                  renderCompanionHead();
                  notify(String(state.vision?.display?.label ?? 'Vision · Held') + ' — ' + String(state.vision?.display?.detail ?? ''));
                  return;
                }

                const requestRef = 'request.vexlife-prod.vision.presentation.' + crypto.randomUUID().toLowerCase();
                state.view = 'conversation';
                state.visionInFlight = true;
                renderCompanionHead();
                renderVision();

                try {
                  const response = await fetch(service.visionControlPath, {
                    method: 'POST',
                    headers: { accept: 'application/json', 'content-type': 'application/json' },
                    body: JSON.stringify({ controlRef, requestRef })
                  });
                  const result = await response.json();
                  if (String(result?.requestRef ?? '') !== requestRef || !result?.vision) {
                    throw new Error('Vision result does not match the exact request.');
                  }
                  state.vision = result.vision;
                  if (result.outcome === 'NOT_LAWFUL_NOW') {
                    notify('Vision changed before VexLife acted. ' + result.vision.display.detail);
                  } else if (result.outcome === 'RUNTIME_REFUSED') {
                    notify(result.vision.display.label + ' — that request did not complete.');
                  } else {
                    notify(result.vision.display.label);
                  }
                } catch (error) {
                  notify(error instanceof Error ? error.message : 'Vision did not complete that request.');
                } finally {
                  state.visionInFlight = false;
                  await loadVision().catch(() => {
                    renderVision();
                  });
                }
              };

              const conversationRenderIdentity = (conversation) => {
                if (!conversation) return 'NONE';
                const messages = conversation.messages ?? [];
                return [
                  String(conversation.currentConversationHeadSha256 ?? 'NONE'),
                  String(conversation.abandonedAttemptCount ?? 0),
                  ...messages.map((message) => String(message.eventSha256 ?? ''))
                ].join(':');
              };

              const loadConversation = async (renderNow = true) => {
                const response = await fetch(service.conversationPath, { headers: { accept: 'application/json' }, cache: 'no-store' });
                const priorIdentity = conversationRenderIdentity(state.conversation);
                const nextConversation = await readJson(response);
                const nextIdentity = conversationRenderIdentity(nextConversation);
                state.conversation = nextConversation;
                const messages = state.conversation?.messages ?? [];
                const latest = [...messages].reverse().find((message) => message.role === 'vex' && message.model);
                setCompanionStatus(
                  'Ready',
                  'Ready',
                  servingModelLabel() || latest?.model || ''
                );
                if (renderNow && priorIdentity !== nextIdentity) {
                  renderTimeline();
                  if (state.app === apps.archives) renderArchives();
                }
              };

              const loadContext = async () => {
                const response = await fetch(service.contextPath, { headers: { accept: 'application/json' }, cache: 'no-store' });
                state.context = await readJson(response);
                renderSettings();
              };

              const completeCanonicalSend = async (result, streamed, preservedProcess = null) => {
                if (streamed) followStreamSpeech(true);
                state.sendInFlight = false;
                state.sendCancelInFlight = false;
                state.sendCancelRequested = false;
                root.dataset.vexLastContextPath = String(result.contextPath ?? '');
                root.dataset.vexLastPromptReceiptRef = String(result.promptContext?.receiptRef ?? '');
                root.dataset.vexLastBrowserContextRef = String(result.promptContext?.browserContextRef ?? '');
                const responseKey = String(result.responseMessageRef ?? '');
                const completedProcess =
                  Array.isArray(preservedProcess)
                    ? preservedProcess
                    : state.liveProcessEntries.length
                      ? (freezeLiveProcess(), snapshotLiveProcess())
                      : [];
                if (responseKey && completedProcess.length) {
                  state.processEntriesByResponse.set(responseKey, completedProcess);
                  if (state.liveProcessManuallyToggled && state.liveProcessExpanded) {
                    state.expandedThoughts.add(responseKey);
                  }
                } else if (responseKey) {
                  state.processEntriesByResponse.delete(responseKey);
                }
                const reasoning = result.humanReasoning;
                if (
                  reasoning?.state === 'PRESENT_EPHEMERAL' &&
                  typeof reasoning.text === 'string' &&
                  reasoning.text.length > 0
                ) {
                  state.humanReasoningByResponse.set(responseKey, reasoning.text);
                } else if (result.responseMessageRef) {
                  state.humanReasoningByResponse.delete(responseKey);
                  if (!state.processEntriesByResponse.has(responseKey)) state.expandedThoughts.delete(responseKey);
                }
                if (Array.isArray(result.capabilityUses) && result.capabilityUses.length > 0) {
                  state.reachesByResponse.set(String(result.responseMessageRef ?? ''), result.capabilityUses);
                }
                state.liveReaches = [];

                // Canonical Conversation/Context own completion. Streaming presentation is only a
                // projection, so refresh from those durable owners before retiring the pending Send.
                try {
                  await Promise.all([loadConversation(false), loadContext()]);
                } catch (projectionError) {
                  notify(
                    'Turn completed, but the current projection could not be refreshed: ' +
                    (projectionError instanceof Error ? projectionError.message : 'refresh failed.')
                  );
                }

                state.pending = null;
                clearLiveProcessState();
                setCompanionStatus(
                  'Ready',
                  'Ready',
                  servingModelLabel() || result.model || ''
                );
                render();
              };

              const reconcileSendOutcome = async (pending) => {
                // Re-present the exact same idempotency key through the non-streaming contract.
                // The turn owner either returns an already-completed durable result, reports the
                // already-failed attempt, or performs the originally intended Send if the first
                // transport never reached it. It must never create a second model call for one
                // already-completed/failed attempt.
                const response = await fetch(service.sendPath, {
                  method: 'POST',
                  headers: { accept: 'application/json', 'content-type': 'application/json' },
                  body: JSON.stringify(pending)
                });
                return readJson(response);
              };

              const cancelSend = async () => {
                if (!state.sendInFlight || !state.pending || state.sendCancelInFlight) return;
                state.sendCancelInFlight = true;
                setCompanionStatus(
                  'Checking',
                  t('workspace.status.conversation-stopping'),
                  servingModelLabel() || ''
                );
                renderComposer();
                try {
                  const response = await fetch(service.cancelPath, {
                    method: 'POST',
                    headers: { accept: 'application/json', 'content-type': 'application/json' },
                    body: JSON.stringify({ idempotencyKey: state.pending.idempotencyKey })
                  });
                  const result = await readJson(response);
                  if (result.cancellationRequested) state.sendCancelRequested = true;
                  notify(
                    result.cancellationRequested
                      ? t('workspace.status.conversation-stopping')
                      : t('workspace.status.conversation-cancel-not-active')
                  );
                } catch (error) {
                  notify(
                    error instanceof Error
                      ? error.message
                      : t('workspace.status.conversation-cancel-unavailable')
                  );
                } finally {
                  state.sendCancelInFlight = false;
                  renderComposer();
                }
              };

              const send = async (newAttemptSeed = null) => {
                if (isPrivate()) {
                  await sendPrivate();
                  return;
                }
                if (state.sendInFlight) return;

                const retryingExactAttempt = state.pending !== null && newAttemptSeed === null;
                const content =
                  newAttemptSeed
                    ? String(newAttemptSeed.content ?? '').trim()
                    : retryingExactAttempt
                      ? state.pending.content
                      : ui.draft.value.trim();
                if (!content) return;

                const pending =
                  newAttemptSeed
                    ? Object.freeze({
                        idempotencyKey: 'send.' + crypto.randomUUID().toLowerCase(),
                        content,
                        browserContext: newAttemptSeed.browserContext ?? null
                      })
                    : state.pending ?? Object.freeze({
                        idempotencyKey: 'send.' + crypto.randomUUID().toLowerCase(),
                        content,
                        browserContext: state.includePage ? state.browserContext : null
                      });
                state.pending = pending;
                state.sendInFlight = true;
                state.sendCancelInFlight = false;
                state.sendCancelRequested = false;
                if (!retryingExactAttempt) {
                  // A genuinely new Send owns a new live process slot. Reconciliation of the same
                  // pending idempotency key must preserve the process already observed for it.
                  state.liveReaches = [];
                  state.liveProcessEntries = [];
                  state.liveProcessExpanded = true;
                  state.liveProcessManuallyToggled = false;
                  resetLiveProcessViewports();
                }
                state.streamPhase = 'CheckingRuntime';
                state.streamedContent = '';
                state.streamedReasoning = '';
                // Speech and the visual process projection intentionally share only these raw
                // stream accumulators/events; neither presentation consumer owns the other.
                speech.stream = { reply: 0, thought: 0 };
                if (speech.settings.enabled) audioContext();
                if (!retryingExactAttempt && newAttemptSeed === null) {
                  ui.draft.value = '';
                  sizeDraft();
                }
                setCompanionStatus(
                  'Checking',
                  retryingExactAttempt
                    ? 'Retrying exact Send'
                    : newAttemptSeed
                      ? 'Trying again'
                      : 'Checking Vex',
                  ''
                );
                render();

                try {
                  const response = await fetch(service.sendPath, {
                    method: 'POST',
                    headers: { accept: 'text/event-stream', 'content-type': 'application/json' },
                    body: JSON.stringify(pending)
                  });
                  let result = null;
                  await readEventStream(response, (event) => {
                    if (event?.type === 'PHASE') {
                      state.streamPhase = String(event.phase ?? '');
                      if (state.streamPhase === 'CheckingRuntime') {
                        setCompanionStatus('Checking', 'Checking Vex', '');
                      } else if (state.streamPhase === 'Reconnecting') {
                        setCompanionStatus('Checking', 'Reconnecting Vex', '');
                      } else if (state.streamPhase === 'Generating') {
                        setCompanionStatus('Checking', 'Thinking', '');
                      }
                      updateLiveTurn();
                      return;
                    }
                    if (event?.type === 'REASONING_DELTA' && typeof event.delta === 'string') {
                      state.streamPhase = 'Reasoning';
                      state.streamedReasoning += event.delta;
                      appendProcessReasoning(event.delta);
                      setCompanionStatus('Checking', 'Thinking', '');
                      updateLiveTurn();
                      // Kokoro consumes the raw aggregate above, never the visual containers.
                      followStreamSpeech(false);
                      return;
                    }
                    if (event?.type === 'REACH' && event.reach) {
                      // A request Vex made (state absent) or what came back (state present). The
                      // returned event settles the newest open request of the same handle/action.
                      const use = event.reach;
                      let settled = false;
                      if (use.state) {
                        for (let index = state.liveReaches.length - 1; index >= 0; index -= 1) {
                          const open = state.liveReaches[index];
                          if (!open.state && open.handle === use.handle && open.action === use.action) {
                            state.liveReaches[index] = use;
                            settled = true;
                            break;
                          }
                        }
                      }
                      if (!settled) state.liveReaches.push(use);
                      projectProcessReach(use);
                      setCompanionStatus('Checking', reachText(use), '');
                      updateLiveTurn();
                      return;
                    }
                    if (event?.type === 'CONTENT_DELTA' && typeof event.delta === 'string') {
                      state.streamPhase = 'Responding';
                      state.streamedContent += event.delta;
                      freezeActiveProcessThought();
                      if (!state.liveProcessManuallyToggled) state.liveProcessExpanded = false;
                      setCompanionStatus('Checking', 'Responding', '');
                      updateLiveTurn();
                      // Final-answer speech remains on the same raw stream as T004.
                      followStreamSpeech(false);
                      return;
                    }
                    if (event?.type === 'FAILED') {
                      const failure = event.error ?? {};
                      const error = new Error(String(failure.message ?? 'Vex turn failed.'));
                      error.vexStatus = 409;
                      error.vexResponse = failure;
                      throw error;
                    }
                    if (event?.type === 'COMPLETED') result = event.response ?? null;
                  });
                  if (!result) throw new Error('Vex stream ended without a completed turn.');
                  await completeCanonicalSend(result, true);
                } catch (error) {
                  // Preserve what the human already saw until the canonical turn owner returns
                  // terminal truth. Presentation transport is not authority to erase process state.
                  freezeLiveProcess();
                  const interruptedProcess = snapshotLiveProcess();

                  let terminalError = error;
                  let status = Number(error?.vexStatus ?? 0);
                  let retryAllowed = error?.vexResponse?.retrySameSendAttemptAllowed;
                  let retireAttempt = status === 400 || retryAllowed === false;

                  if (!retireAttempt) {
                    setCompanionStatus('Checking', 'Reconciling Send', '');
                    render();
                    try {
                      const reconciled = await reconcileSendOutcome(pending);
                      await completeCanonicalSend(reconciled, false, interruptedProcess);
                      return;
                    } catch (reconciliationError) {
                      terminalError = reconciliationError;
                      status = Number(reconciliationError?.vexStatus ?? 0);
                      retryAllowed = reconciliationError?.vexResponse?.retrySameSendAttemptAllowed;
                      retireAttempt = status === 400 || retryAllowed === false;
                    }
                  }

                  state.sendInFlight = false;
                  state.sendCancelInFlight = false;
                  const cancelledByPerson = state.sendCancelRequested;
                  state.sendCancelRequested = false;
                  if (retireAttempt) {
                    const failure = terminalError?.vexResponse ?? {};
                    state.failedAttempts.push({
                      idempotencyKey: pending.idempotencyKey,
                      content: pending.content,
                      browserContext: pending.browserContext ?? null,
                      failureCode: String(failure.code ?? terminalError?.name ?? 'TURN_FAILED'),
                      exactNextSafeRoute: failure.exactNextSafeRoute ?? null,
                      processEntries: interruptedProcess,
                      cancelledByPerson
                    });
                    if (cancelledByPerson && !ui.draft.value.trim()) {
                      ui.draft.value = pending.content;
                    }
                    state.pending = null;
                    clearLiveProcessState();
                    setCompanionStatus(
                      cancelledByPerson ? 'Ready' : 'Unavailable',
                      cancelledByPerson
                        ? t('workspace.status.conversation-stopped')
                        : 'Send failed',
                      servingModelLabel() || ''
                    );
                  } else {
                    // Outcome is still unresolved: keep the same idempotency key and the process the
                    // human watched. A later exact retry/reconciliation must not look like a new Send.
                    state.pending = pending;
                    state.streamPhase = 'Reconnecting';
                    setCompanionStatus('Unavailable', 'Send outcome unresolved', '');
                    reportFact(
                      'SEND_OUTCOME_UNRESOLVED_SHOWN',
                      { reason: terminalError instanceof Error ? terminalError.name : 'unknown' }
                    );
                  }

                  sizeDraft();
                  render();
                  notify(
                    cancelledByPerson
                      ? t('workspace.description.conversation-stopped')
                      : terminalError instanceof Error
                        ? terminalError.message
                        : 'Send failed.'
                  );
                }
              };

              const sizeDraft = () => {
                ui.draft.style.height = 'auto';
                ui.draft.style.height = Math.min(ui.draft.scrollHeight + 2, 220) + 'px';
                renderComposer();
              };

              // Browser + Companion keep one allocation/collapse relationship across responsive
              // manifestations. Wide screens place the Seam left/right; narrow screens manifest the
              // same relationship top/bottom. Responsive orientation is renderer-owned and does not
              // create a second spatial state machine.
              const NARROW_BROWSER_DEFAULT_SHARE = 0.34;
              const narrowWorkspace = () => window.matchMedia('(max-width: 860px)').matches;
              const currentSplitShare = (narrow = narrowWorkspace()) =>
                state.allocation ??
                (narrow ? NARROW_BROWSER_DEFAULT_SHARE : browser.authoredBrowserShare);

              const applySplit = () => {
                const narrow = narrowWorkspace();
                // Preserve the lived narrow composition until the human explicitly resizes. Once an
                // allocation is chosen, the same value carries across both renderer orientations.
                const share = currentSplitShare(narrow);
                const percent = Math.round(share * 100);
                const browserCollapsed = state.collapsed === 'before';
                const companionCollapsed = state.collapsed === 'after';
                ui.frame.dataset.collapsed = state.collapsed ?? 'none';
                ui.frame.style.setProperty('--before', (share * 100).toFixed(2) + '%');
                ui.seam.setAttribute('aria-orientation', narrow ? 'horizontal' : 'vertical');
                ui.seam.setAttribute('aria-valuenow', String(percent));
                ui.seam.setAttribute(
                  'aria-valuetext',
                  browserCollapsed
                    ? 'Browser collapsed'
                    : companionCollapsed
                      ? 'Companion collapsed'
                      : 'Browser ' + percent + ' percent ' + (narrow ? 'height' : 'width')
                );
                const browserAction = (browserCollapsed ? 'Show' : 'Hide') + ' browser';
                const companionAction = (companionCollapsed ? 'Show' : 'Hide') + ' companion';
                ui.collapseBefore.setAttribute('aria-label', browserAction);
                ui.collapseBefore.title = browserAction;
                ui.collapseAfter.setAttribute('aria-label', companionAction);
                ui.collapseAfter.title = companionAction;
              };

              const setSplitFromPointer = (clientX, clientY) => {
                const rect = ui.frame.getBoundingClientRect();
                const narrow = narrowWorkspace();
                const span = narrow ? rect.height : rect.width;
                if (span <= 0) return;
                const position = narrow ? (clientY - rect.top) : (clientX - rect.left);
                state.allocation = Math.min(0.85, Math.max(0.15, position / span));
                state.collapsed = 'none';
                applySplit();
              };

              const showInspector = (title, facts, text) => {
                ui.inspectorTitle.textContent = title;
                ui.inspectorFacts.replaceChildren(...facts.flatMap(([term, value]) => {
                  const dt = document.createElement('dt');
                  dt.textContent = term;
                  const dd = document.createElement('dd');
                  dd.textContent = value;
                  return [dt, dd];
                }));
                ui.inspectorText.textContent = text;
                ui.inspector.showModal();
              };

              const openCurrentContext = () => {
                if (!state.browserContext) return;
                showInspector(
                  'Captured page',
                  [['Page', state.browserContext.pageUrl], ['Title', state.browserContext.title || 'None'], ['Read from', 'Separate context-provider rendering']],
                  state.browserContext.text
                );
              };

              loadThoughtVisibility();
              applyTheme();
              initializeQuickActions();
              renderQuickActions();
              applySplit();
              render();

              ui.addressForm.addEventListener('submit', (event) => {
                event.preventDefault();
                try {
                  navigate(ui.query.value);
                  ui.query.blur();
                  ui.addressError.hidden = true;
                } catch (error) {
                  ui.addressError.textContent = error instanceof Error ? error.message : 'Page could not be opened.';
                  ui.addressError.hidden = false;
                }
              });
              ui.query.addEventListener('input', () => { ui.addressError.hidden = true; });
              ui.reload.addEventListener('click', () => {
                try { navigate(ui.query.value); } catch (error) { notify(error.message); }
              });
              ui.appSelector.addEventListener('change', () => chooseApp(ui.appSelector.value));
              ui.languageList.addEventListener('click', (event) => {
                const button = event.target.closest('button[data-language]');
                if (button) void chooseLanguage(button.dataset.language);
              });
              ui.privateMode.addEventListener('click', () => void togglePrivate());
              ui.privatePropose.addEventListener('click', () => {
                void privatePost(service.privateNoteProposePath, {}).catch((error) =>
                  notify(error instanceof Error ? error.message : 'Vex could not propose a note.'));
              });
              ui.privateKeep.addEventListener('click', () => {
                const text = state.privateMode?.proposedNoteOrNull;
                if (!text) return;
                void privatePost(service.privateNoteKeepPath, { text }).catch((error) =>
                  notify(error instanceof Error ? error.message : 'The note was not kept.'));
              });
              ui.privateDecline.addEventListener('click', () => {
                void privatePost(service.privateNoteDeclinePath, {}).catch(() => {});
              });
              ui.family.addEventListener('click', (event) => {
                const member = event.target.closest('.family-member');
                if (member?.dataset.familyHome) navigate(member.dataset.familyHome);
              });
              ui.archivesTimeline.addEventListener('click', (event) => {
                const text = event.target.closest('.archives-entry p');
                if (text) text.dataset.open = String(text.dataset.open !== 'true');
              });

              // Context is admitted only from the Product-owned context frame, only for the exact page
              // whose identity is currently proven (contextTargetPageUrl). A message for any other
              // page is stale or foreign; arrival alone never makes a page eligible.
              window.addEventListener('message', (event) => {
                if (event.source !== ui.contextFrame.contentWindow) return;
                const next = normalizeContext(event.data);
                if (!next || !state.contextTargetPageUrl || next.pageUrl !== state.contextTargetPageUrl) return;
                reportFact('BROWSER_CONTEXT_READY', { ...pageRefs(next.pageUrl), characters: String(next.text.length) });
                state.browserContext = next;
                state.contextLoading = false;
                state.includePage = true;
                renderFrameStatus('Vex page context is ready for the next turn.', 'ContextReady');
                renderComposer();
              });

              // Why an unexpected visible-frame load clears page identity: the visible page is
              // cross-origin, so this presentation cannot observe its new address. A load it did not
              // start means the person navigated inside the page; keeping the old address (and any
              // context captured for it) would let Vex be told about a page that is no longer shown.
              // VISIBLE PAGE LOOKS ELIGIBLE != PRODUCT HAS PROVEN CURRENT PAGE IDENTITY: identity is
              // re-earned only by a Product-owned navigate(...) today. Never infer it from appearance.
              ui.pageFrame.addEventListener('load', () => {
                if (state.humanFrameLoadExpected) {
                  state.humanFrameLoadExpected = false;
                  if (state.contextTargetPageUrl && !state.browserContext) {
                    renderFrameStatus('Page opened directly · Vex page context is available on demand.', 'Direct');
                  } else if (!state.contextTargetPageUrl) {
                    renderFrameStatus('Page opened directly · Vex page context is not available for this site yet.', 'Direct');
                  }
                } else {
                  reportFact('BROWSER_PAGE_UNLOCATED', {
                    previousHost: pageRefs(state.humanPageUrl).host ?? 'none',
                    contextWasReady: String(Boolean(state.browserContext)),
                    contextWasLoading: String(Boolean(state.contextLoading))
                  });
                  state.browserContext = null;
                  state.includePage = false;
                  state.humanPageUrl = null;
                  state.contextTargetPageUrl = null;
                  state.contextLoading = false;
                  ui.contextFrame.removeAttribute('src');
                  ui.query.value = '';
                  ui.query.placeholder = 'Current address unavailable · enter a web address';
                  renderFrameStatus(
                    'The page navigated inside the embedded web view. Its exact address is not observable from this web-hosted presentation, so the stale address and Vex page context were cleared.',
                    'Unlocated'
                  );
                }
                renderComposer();
              });

              ui.visionControl.addEventListener('click', () => {
                void activateVision();
              });

              loadSpeechSettings();
              if (speech.systemSupported) {
                window.speechSynthesis.addEventListener('voiceschanged', () => {
                  renderSpeechVoices();
                  renderSpeech();
                });
              }
              ui.speechSwitch.addEventListener('click', () => {
                speech.settings.enabled = !speech.settings.enabled;
                saveSpeechSettings();
                if (speech.settings.enabled) {
                  audioContext();
                  if (chosenVoice()?.group === 'natural') kokoroWorker().postMessage({ type: 'prepare' });
                } else {
                  stopSpeech();
                }
                renderSpeech();
              });
              ui.speechVoice.addEventListener('change', () => {
                speech.settings.voice = ui.speechVoice.value;
                saveSpeechSettings();
                if (chosenVoice()?.group === 'natural') {
                  audioContext();
                  kokoroWorker().postMessage({ type: 'prepare' });
                }
                renderSpeech();
              });
              ui.speechThoughts.addEventListener('change', () => {
                speech.settings.readThoughts = ui.speechThoughts.checked;
                saveSpeechSettings();
                renderSpeech();
              });
              ui.speechRate.addEventListener('input', () => {
                speech.settings.rate = Math.min(1.6, Math.max(0.7, Number(ui.speechRate.value) || 1));
                saveSpeechSettings();
                if (speech.kokoro.source) speech.kokoro.source.playbackRate.value = speech.settings.rate;
                renderSpeech();
              });
              ui.speechPlay.addEventListener('click', pauseOrResumeSpeech);
              ui.speechStop.addEventListener('click', stopSpeech);
              ui.speechPrevious.addEventListener('click', () => stepReading(-1));
              ui.speechNext.addEventListener('click', () => stepReading(1));
              ui.speechChoose.addEventListener('click', () => {
                speech.choosing = !speech.choosing;
                renderSpeech();
              });
              ui.timeline.addEventListener('click', (event) => {
                if (!speech.choosing) return;
                const item = event.target.closest('.message');
                if (!item?.dataset.eventRef) return;
                event.preventDefault();
                startReading(item.dataset.eventRef);
              });
              document.addEventListener('keydown', (event) => {
                if (event.key !== 'Escape') return;
                if (speech.choosing) {
                  speech.choosing = false;
                  renderSpeech();
                } else if (speech.open) {
                  speech.open = false;
                  renderQuickActions();
                } else if (state.languageOpen) {
                  state.languageOpen = false;
                  renderQuickActions();
                } else if (state.quickActionsOpen) {
                  state.quickActionsOpen = false;
                  renderQuickActions();
                }
              });

              ui.viewSwitch.addEventListener('click', () => {
                state.view = state.view === 'settings' ? 'conversation' : 'settings';
                renderCompanionHead();
                if (state.view === 'settings') {
                  renderSettings();
                  void loadCurrentState().catch(() => {});
                  void loadMemory().catch(() => {});
                }
              });

              ui.contextShare.addEventListener('click', () => {
                if (state.contextLoading) return;

                if (state.browserContext) {
                  state.includePage = !state.includePage;
                  renderComposer();
                  return;
                }

                if (!state.contextTargetPageUrl) return;
                const contextFrameUrl = contextFrameUrlFor(state.contextTargetPageUrl);
                if (!contextFrameUrl) return;

                state.contextLoading = true;
                state.includePage = false;
                reportFact('BROWSER_CONTEXT_REQUESTED', pageRefs(state.contextTargetPageUrl));
                ui.contextFrame.src = contextFrameUrl;
                renderFrameStatus('Reading the current page separately for Vex…', 'Loading');
                renderComposer();
              });

              ui.voiceInput?.addEventListener('click', () => {
                void applyVoiceInputControl();
              });

              ui.stop.addEventListener('click', () => {
                void cancelSend();
              });

              ui.composeForm.addEventListener('submit', (event) => {
                event.preventDefault();
                if (dictation.desiredActive) void stopVoiceInput(true);
                void send();
              });

              ui.draft.addEventListener('keydown', (event) => {
                if (event.key === 'Enter' && !event.shiftKey && !event.isComposing) {
                  event.preventDefault();
                  if (dictation.desiredActive) void stopVoiceInput(true);
                  void send();
                }
              });

              ui.draft.addEventListener('input', () => {
                if (dictation.desiredActive) void stopVoiceInput(true);
                sizeDraft();
              });
              document.addEventListener('visibilitychange', () => {
                if (document.hidden && dictation.desiredActive) void stopVoiceInput(true);
              });
              ui.timeline.addEventListener('scroll', () => {
                state.timelineScrollTop = ui.timeline.scrollTop;
                state.timelineFollowTail = isElementAtBottom(ui.timeline);
                ui.jumpLatest.hidden = state.timelineFollowTail;
              });
              ui.timeline.addEventListener('click', (event) => {
                const retryFailed = event.target.closest('.failed-attempt-retry');
                if (retryFailed) {
                  if (state.sendInFlight) return;
                  const key = String(retryFailed.dataset.failedAttemptKey ?? '');
                  const failed = state.failedAttempts.find((attempt) => attempt.idempotencyKey === key);
                  if (!failed) return;
                  void send({
                    content: failed.content,
                    browserContext: failed.browserContext
                  });
                  return;
                }

                const liveToggle = event.target.closest('.live-process-toggle');
                if (liveToggle) {
                  state.liveProcessExpanded = !state.liveProcessExpanded;
                  state.liveProcessManuallyToggled = true;
                  updateLiveTurn();
                  return;
                }
                const toggle = event.target.closest('.thought-toggle');
                if (!toggle) return;
                const key = String(toggle.dataset.thoughtKey ?? '');
                if (
                  !key ||
                  (!state.humanReasoningByResponse.has(key) && !state.processEntriesByResponse.has(key))
                ) return;
                if (state.expandedThoughts.has(key)) state.expandedThoughts.delete(key);
                else state.expandedThoughts.add(key);
                renderTimeline();
              });
              ui.jumpLatest.addEventListener('click', () => {
                state.timelineFollowTail = true;
                applyTimelineViewport();
              });

              ui.ticketView.addEventListener('click', openCurrentContext);
              ui.inspectorClose.addEventListener('click', () => ui.inspector.close());

              ui.thoughtVisibility.addEventListener('click', () => {
                state.thoughtsVisible = !state.thoughtsVisible;
                saveThoughtVisibility();
                renderSettings();
                renderTimeline();
              });

              ui.collapseBefore.addEventListener('click', () => {
                state.collapsed = state.collapsed === 'before' ? 'none' : 'before';
                applySplit();
              });
              ui.collapseAfter.addEventListener('click', () => {
                state.collapsed = state.collapsed === 'after' ? 'none' : 'after';
                applySplit();
              });

              ui.seam.addEventListener('pointerdown', (event) => {
                if (event.target.closest('button')) return;
                ui.seam.setPointerCapture(event.pointerId);
                ui.frame.classList.add('resizing');
              });
              ui.seam.addEventListener('pointermove', (event) => {
                if (!ui.seam.hasPointerCapture(event.pointerId)) return;
                setSplitFromPointer(event.clientX, event.clientY);
              });
              ui.seam.addEventListener('pointerup', (event) => {
                if (ui.seam.hasPointerCapture(event.pointerId)) ui.seam.releasePointerCapture(event.pointerId);
                ui.frame.classList.remove('resizing');
              });
              ui.seam.addEventListener('pointercancel', () => ui.frame.classList.remove('resizing'));
              ui.seam.addEventListener('dblclick', () => {
                state.allocation = null;
                state.collapsed = 'none';
                applySplit();
              });
              ui.seam.addEventListener('keydown', (event) => {
                const narrow = narrowWorkspace();
                const current = currentSplitShare(narrow);
                const decrease = narrow ? 'ArrowUp' : 'ArrowLeft';
                const increase = narrow ? 'ArrowDown' : 'ArrowRight';
                if (event.key === decrease || event.key === increase) {
                  event.preventDefault();
                  state.allocation = Math.min(0.85, Math.max(0.15, current + (event.key === decrease ? -0.05 : 0.05)));
                  state.collapsed = 'none';
                  applySplit();
                } else if (event.key === 'Home' || event.key === 'Enter') {
                  event.preventDefault();
                  state.allocation = null;
                  state.collapsed = 'none';
                  applySplit();
                }
              });
              window.addEventListener('resize', applySplit);

              void loadWorkspaceStrings().catch(() => applyStaticText());
              ui.diagnosticsEnhanced?.addEventListener('click', () => {
                const controlRef = ui.diagnosticsEnhanced.dataset.controlRef;
                if (controlRef) void diagnosticsControl(controlRef);
              });
              ui.diagnosticsClear?.addEventListener('click', () => {
                if (window.confirm(t('workspace.description.diagnostics-clear-confirm'))) void diagnosticsControl('CLEAR_HISTORY');
              });

              void Promise.all([loadSourceCurrentness(), loadCurrentState(), loadConversation(), loadContext(), loadVision()])
                .then(() => {
                  render();
                  navigate(browser.initialPageUrl);
                  renderApp();
                  void loadPrivate().catch(() => {});
                  void loadSettings().catch(() => {});
                  void loadMemory().catch(() => {});
                  void loadDiagnostics().catch(() => renderDiagnostics());
                  void loadModels().catch(() => {});
                  // Observed: the presentation rendered with current service data.
                  reportFact('PRESENTATION_ARRIVED', { presentation: 'workspace' });
                })
                .catch((error) => {
                  setCompanionStatus('Unavailable', 'Unavailable', '');
                  notify(error instanceof Error ? error.message : 'Local service is not ready.');
                });

              setInterval(() => {
                if (state.pending) return;
                void loadConversation().catch(() => {});
                if (!state.visionInFlight) void loadVision().catch(() => {});
                if (!state.privateInFlight) void loadPrivate().catch(() => {});
              }, 2500);
            })();
        """
    }

    private fun jsString(
        value: String,
    ): String =
        "\"" +
            value
                .replace(
                    "\\",
                    "\\\\",
                )
                .replace(
                    "\"",
                    "\\\"",
                )
                .replace(
                    "\n",
                    "\\n",
                ) +
            "\""
}

// [VXG RealForever]
