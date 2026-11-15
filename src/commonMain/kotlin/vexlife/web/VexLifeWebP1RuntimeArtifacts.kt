package vexlife.web

import vexlife.browser.VexLifeProdBrowserContract
import vexlife.service.VexLifeProdServiceContract
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
                sendPath: ${jsString(VexLifeProdServiceContract.ConversationSendPath)}
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
                streamPhase: null,
                streamedContent: '',
                streamedReasoning: '',
                thoughtsVisible: true,
                humanReasoningByResponse: new Map(),
                // What Vex reached during the live turn (bridge uses: handle/action/target/state), and
                // per completed response for this browser session. Reach, never the content.
                liveReaches: [],
                reachesByResponse: new Map(),
                expandedThoughts: new Set(),
                timelineFollowTail: true,
                timelineScrollTop: 0,
                reasoningViewports: new Map(),
                theme: null
              };

              const QUICK_ACTIONS = [
                { key: 'journeys', label: 'Journeys', icon: '<path d="M4 15.5c0-2 1.6-3 3.4-3.4l3.4-.8 2.2-4.3c.4-.8 1.4-1 2-.3l.6.6c.5.5.5 1.3 0 1.8l-1 1.2 2.8.8c1.4.4 2.4 1.7 2.4 3.2v1.2H4z"/><path d="M8 12l1.4 2M11 11.4l1.3 2"/>' },
                { key: 'speech', label: 'Speech', bound: true, icon: '<path d="M4 9.5h3.2L11.5 6v12l-4.3-3.5H4z"/><path d="M15 9.2a4 4 0 0 1 0 5.6M17.6 6.8a7.3 7.3 0 0 1 0 10.4"/>' },
                { key: 'theme', label: 'Light or dark', bound: true, icon: '<circle cx="12" cy="12" r="3.8"/><path d="M12 2.8v2.4M12 18.8v2.4M2.8 12h2.4M18.8 12h2.4M5.5 5.5l1.7 1.7M16.8 16.8l1.7 1.7M5.5 18.5l1.7-1.7M16.8 7.2l1.7-1.7"/>' },
                { key: 'language', label: 'Language', bound: true, icon: '<path d="M5 20.5V4M5 4.5h12.5l-2 3.5 2 3.5H5"/>' },
                { key: 'evolution', label: 'Evolution', icon: '<path d="M12.2 12.1c.4-.6-.3-1.4-1-1.2-1.3.4-1.3 2.3-.1 3 1.8 1 4-.5 3.9-2.6-.1-3-3.6-4.3-6-2.8-3.1 2-2.6 6.9.7 8.3 3.9 1.6 8.3-1.2 8.2-5.6"/>' },
                { key: 'vision', label: 'Vision', icon: '<path d="M3 12s3.6-5.2 9-5.2 9 5.2 9 5.2-3.6 5.2-9 5.2S3 12 3 12Z"/><circle cx="12" cy="12" r="2.8"/>' }
              ];

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
              // Conversation projections, never the DOM. Kokoro voices run on this machine after a
              // one-time model download; system voices are limited to those that run on this machine.
              // Either way the words are never sent to a speech service.
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
                try {
                  const response = await fetch(service.settingsPath, {
                    method: 'POST',
                    headers: { 'content-type': 'application/json', accept: 'application/json' },
                    body: JSON.stringify({ replyLanguage: code })
                  });
                  state.settings = await readJson(response);
                  state.languageOpen = false;
                  renderLanguage();
                  renderSpeechVoices();
                  renderSpeech();
                  const name = state.settings.languages.find((language) => language.code === code)?.name ?? code;
                  notify('Vex will reply in ' + name + '.');
                } catch (error) {
                  notify(error instanceof Error ? error.message : 'The language could not be changed.');
                }
              };

              const renderQuickActions = () => {
                if (!ui.quickActions) return;
                const sourceOnly = [...ui.quickActions.children].filter((item) => item.hidden);
                const items = QUICK_ACTIONS.map((action) => {
                  const item = document.createElement('li');
                  const button = document.createElement('button');
                  const visionDisplay = state.vision?.display;
                  const visionBound = action.key === 'vision' && Boolean(visionDisplay?.primaryControlRef);
                  const bound = visionBound || Boolean(action.bound);
                  button.type = 'button';
                  button.className = 'quick-action';
                  button.dataset.action = action.key;
                  button.dataset.bound = String(bound);
                  if (action.key === 'vision') button.dataset.indicator = String(visionDisplay?.indicatorRef ?? 'None');
                  button.disabled = action.key === 'vision' && state.visionInFlight;
                  button.setAttribute('aria-label', action.key === 'vision'
                    ? String(visionDisplay?.label ?? 'Vision · Held')
                    : (bound ? action.label : action.label + ' (Held)'));
                  button.title = action.key === 'vision'
                    ? (state.visionInFlight
                        ? 'Vision · Working'
                        : String(visionDisplay?.label ?? 'Vision · Held') + (visionDisplay?.primaryLabel ? ' — ' + visionDisplay.primaryLabel : ''))
                    : (bound ? action.label : action.label + ': not connected yet');
                  button.innerHTML = '<svg viewBox="0 0 24 24" aria-hidden="true">' + action.icon + '</svg>';
                  item.append(button);
                  return item;
                });
                ui.quickActions.replaceChildren(...sourceOnly, ...items);
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
                  applyReasoningViewport(element, key, key === 'live');
                });
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

              const reachedLine = (message) => {
                if (message.role !== 'vex') return null;
                const uses = state.reachesByResponse.get(String(message.messageRef ?? ''));
                if (!uses || !uses.length) return null;
                const line = document.createElement('p');
                line.className = 'receipt-line reached-line';
                line.textContent = t('workspace.label.reached') + ': ' + uses.map(reachText).join(' · ');
                return line;
              };

              const completedThoughtNode = (message) => {
                if (!state.thoughtsVisible || message.role !== 'vex') return null;
                const key = String(message.messageRef ?? '');
                const reasoning = state.humanReasoningByResponse.get(key);
                if (!reasoning) return null;

                const holder = document.createElement('div');
                holder.className = 'thought';
                const expanded = state.expandedThoughts.has(key);
                const toggle = document.createElement('button');
                toggle.type = 'button';
                toggle.className = 'thought-toggle';
                toggle.dataset.thoughtKey = key;
                toggle.setAttribute('aria-expanded', String(expanded));
                const label = document.createElement('span');
                label.textContent = expanded ? 'hide thoughts' : 'view thoughts';
                toggle.append(thoughtMark(), label);
                holder.append(toggle);

                if (expanded) {
                  const box = document.createElement('div');
                  box.className = 'reasoning';
                  bindReasoningViewport(box, 'response.' + key, false);
                  box.textContent = reasoning;
                  holder.append(box);
                }
                return holder;
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
                const paragraph = document.createElement('p');
                paragraph.textContent = String(message.content ?? '');
                bubble.append(paragraph);

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
                if (state.liveReaches.length) thought.append(reachList(state.liveReaches));

                if (state.thoughtsVisible && state.streamedReasoning) {
                  const reasoning = document.createElement('div');
                  reasoning.className = 'reasoning live-reasoning';
                  bindReasoningViewport(reasoning, 'live', true);
                  reasoning.textContent = state.streamedReasoning;
                  thought.append(reasoning);
                }

                const bubble = document.createElement('div');
                bubble.className = 'bubble';
                if (state.streamedContent) {
                  const paragraph = document.createElement('p');
                  paragraph.textContent = state.streamedContent;
                  bubble.append(paragraph);
                }
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
                  const nodes = exchanges.flatMap((exchange, index) => [
                    messageNode({ role: 'human', content: exchange.human, sequence: index * 2 + 1, eventRef: 'private.' + (index * 2 + 1) }),
                    messageNode({ role: 'vex', content: exchange.vex, sequence: index * 2 + 2, eventRef: 'private.' + (index * 2 + 2) })
                  ]);
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
                const messages = state.conversation?.messages ?? [];
                const nodes = messages.map(messageNode);

                if (state.pending) {
                  const sequence = nextSequence();
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
                // Reach lines sit between the label and the live reasoning, in the order Vex asked.
                item.querySelector('.live-reaches')?.remove();
                if (state.liveReaches.length && thought) {
                  const list = reachList(state.liveReaches);
                  const label = thought.querySelector('.thinking-label');
                  if (label) label.after(list);
                  else thought.append(list);
                }
                let reasoning = item.querySelector('.live-reasoning');
                if (state.thoughtsVisible && state.streamedReasoning) {
                  if (!reasoning) {
                    reasoning = document.createElement('div');
                    reasoning.className = 'reasoning live-reasoning';
                    bindReasoningViewport(reasoning, 'live', true);
                    thought?.append(reasoning);
                  }
                  reasoning.textContent = state.streamedReasoning;
                  applyReasoningViewport(reasoning, 'live', true, false);
                } else if (reasoning) {
                  reasoning.remove();
                }

                const bubble = item.querySelector('.bubble');
                let paragraph = bubble?.querySelector('p');
                if (state.streamedContent) {
                  if (!paragraph && bubble) {
                    paragraph = document.createElement('p');
                    bubble.append(paragraph);
                  }
                  if (paragraph) paragraph.textContent = state.streamedContent;
                } else if (paragraph) {
                  paragraph.remove();
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
                ui.composerHold.textContent = state.contextLoading
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
                        ? 'The prior Send outcome is unresolved. Send again to retry that exact attempt.'
                        : '';
                ui.send.disabled = privateNow
                  ? state.privateInFlight || !ui.draft.value.trim()
                  : state.sendInFlight || (!state.pending && !ui.draft.value.trim());
                ui.responseProgress.hidden = true;
                renderTicket();
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
                    button.disabled = state.modelsInFlight;
                    button.addEventListener('click', () => { void modelControl(control.controlRef, model.handle); });
                    controls.append(button);
                  }
                  row.append(name, controls, facts, notes);
                  ui.modelsList.append(row);
                }
                const lines = [];
                if (projection.lastOutcome) lines.push(resolveText(projection.lastOutcome.message));
                if (projection.switchPending) lines.push(t('workspace.status.models-switch-pending'));
                if (projection.freeStorageBytes != null) {
                  lines.push(t('workspace.label.model-free-space').replace('{free}', gibText(projection.freeStorageBytes)));
                }
                if (ui.modelsNote) ui.modelsNote.textContent = lines.join(' ');
              };

              const loadModels = async () => {
                const response = await fetch(service.modelsPath, { headers: { accept: 'application/json' }, cache: 'no-store' });
                state.models = await readJson(response);
                renderModels();
                // A running download is followed until it settles; nothing polls when idle.
                if (state.models?.downloading) window.setTimeout(() => { void loadModels().catch(() => {}); }, 2000);
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
                } catch (error) {
                  notify(error instanceof Error ? error.message : 'Models are not available.');
                } finally {
                  state.modelsInFlight = false;
                  renderModels();
                  if (state.models?.downloading) window.setTimeout(() => { void loadModels().catch(() => {}); }, 2000);
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
                setCompanionStatus('Ready', 'Ready', latest?.model ?? '');
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

              const completeCanonicalSend = async (result, streamed) => {
                if (streamed) followStreamSpeech(true);
                state.sendInFlight = false;
                root.dataset.vexLastContextPath = String(result.contextPath ?? '');
                root.dataset.vexLastPromptReceiptRef = String(result.promptContext?.receiptRef ?? '');
                root.dataset.vexLastBrowserContextRef = String(result.promptContext?.browserContextRef ?? '');
                const reasoning = result.humanReasoning;
                if (
                  reasoning?.state === 'PRESENT_EPHEMERAL' &&
                  typeof reasoning.text === 'string' &&
                  reasoning.text.length > 0
                ) {
                  state.humanReasoningByResponse.set(String(result.responseMessageRef ?? ''), reasoning.text);
                } else if (result.responseMessageRef) {
                  state.humanReasoningByResponse.delete(String(result.responseMessageRef));
                  state.expandedThoughts.delete(String(result.responseMessageRef));
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
                state.streamPhase = null;
                state.streamedContent = '';
                state.streamedReasoning = '';
                setCompanionStatus('Ready', 'Ready', result.model ?? '');
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

              const send = async () => {
                if (isPrivate()) {
                  await sendPrivate();
                  return;
                }
                if (state.sendInFlight) return;

                const retrying = state.pending !== null;
                const content = retrying ? state.pending.content : ui.draft.value.trim();
                if (!content) return;

                const pending = state.pending ?? Object.freeze({
                  idempotencyKey: 'send.' + crypto.randomUUID().toLowerCase(),
                  content,
                  browserContext: state.includePage ? state.browserContext : null
                });
                state.pending = pending;
                state.sendInFlight = true;
                state.liveReaches = [];
                state.streamPhase = 'CheckingRuntime';
                state.streamedContent = '';
                state.streamedReasoning = '';
                speech.stream = { reply: 0, thought: 0 };
                if (speech.settings.enabled) audioContext();
                state.reasoningViewports.set('live', { followTail: true, scrollTop: 0 });
                if (!retrying) {
                  ui.draft.value = '';
                  sizeDraft();
                }
                setCompanionStatus('Checking', retrying ? 'Retrying exact Send' : 'Checking Vex', '');
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
                      setCompanionStatus('Checking', 'Thinking', '');
                      updateLiveTurn();
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
                      setCompanionStatus('Checking', reachText(use), '');
                      updateLiveTurn();
                      return;
                    }
                    if (event?.type === 'CONTENT_DELTA' && typeof event.delta === 'string') {
                      state.streamPhase = 'Responding';
                      state.streamedContent += event.delta;
                      setCompanionStatus('Checking', 'Responding', '');
                      updateLiveTurn();
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
                  state.streamPhase = null;
                  state.streamedContent = '';
                  state.streamedReasoning = '';
                  state.liveReaches = [];

                  let terminalError = error;
                  let status = Number(error?.vexStatus ?? 0);
                  let retryAllowed = error?.vexResponse?.retrySameSendAttemptAllowed;
                  let retireAttempt = status === 400 || retryAllowed === false;

                  if (!retireAttempt) {
                    setCompanionStatus('Checking', 'Reconciling Send', '');
                    render();
                    try {
                      const reconciled = await reconcileSendOutcome(pending);
                      await completeCanonicalSend(reconciled, false);
                      return;
                    } catch (reconciliationError) {
                      terminalError = reconciliationError;
                      status = Number(reconciliationError?.vexStatus ?? 0);
                      retryAllowed = reconciliationError?.vexResponse?.retrySameSendAttemptAllowed;
                      retireAttempt = status === 400 || retryAllowed === false;
                    }
                  }

                  state.sendInFlight = false;
                  if (retireAttempt) {
                    state.pending = null;
                    if (!ui.draft.value) ui.draft.value = pending.content;
                    setCompanionStatus('Unavailable', 'Send failed', '');
                  } else {
                    state.pending = pending;
                    setCompanionStatus('Unavailable', 'Send outcome unresolved', '');
                    reportFact(
                      'SEND_OUTCOME_UNRESOLVED_SHOWN',
                      { reason: terminalError instanceof Error ? terminalError.name : 'unknown' }
                    );
                  }

                  sizeDraft();
                  render();
                  notify(terminalError instanceof Error ? terminalError.message : 'Send failed.');
                }
              };

              const sizeDraft = () => {
                ui.draft.style.height = 'auto';
                ui.draft.style.height = Math.min(ui.draft.scrollHeight + 2, 220) + 'px';
                renderComposer();
              };

              const applySplit = () => {
                const share = state.allocation ?? browser.authoredBrowserShare;
                ui.frame.dataset.collapsed = state.collapsed ?? 'none';
                ui.frame.style.setProperty('--before', (share * 100).toFixed(2) + '%');
                ui.seam.setAttribute('aria-valuenow', String(Math.round(share * 100)));
                ui.seam.setAttribute('aria-valuetext', 'Browser ' + Math.round(share * 100) + ' percent');
              };

              const setSplitFromClientX = (clientX) => {
                const rect = ui.frame.getBoundingClientRect();
                if (rect.width <= 0) return;
                state.allocation = Math.min(0.85, Math.max(0.15, (clientX - rect.left) / rect.width));
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

              ui.quickActions.addEventListener('click', (event) => {
                const button = event.target.closest('.quick-action');
                if (!button) return;
                const action = QUICK_ACTIONS.find((candidate) => candidate.key === button.dataset.action);
                if (!action) return;
                if (action.key === 'theme') {
                  toggleTheme();
                  return;
                }
                if (action.key === 'language') {
                  state.languageOpen = !state.languageOpen;
                  if (state.languageOpen) speech.open = false;
                  renderLanguage();
                  renderSpeech();
                  return;
                }
                if (action.key === 'speech') {
                  state.languageOpen = false;
                  renderLanguage();
                  speech.open = !speech.open;
                  if (!speech.open) speech.choosing = false;
                  renderSpeechVoices();
                  renderSpeech();
                  return;
                }
                if (action.key === 'vision') {
                  void activateVision();
                  return;
                }
                notify(action.label + " isn't connected in this workspace yet.");
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
                  renderSpeech();
                } else if (state.languageOpen) {
                  state.languageOpen = false;
                  renderLanguage();
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

              ui.composeForm.addEventListener('submit', (event) => {
                event.preventDefault();
                void send();
              });

              ui.draft.addEventListener('keydown', (event) => {
                if (event.key === 'Enter' && !event.shiftKey && !event.isComposing) {
                  event.preventDefault();
                  void send();
                }
              });

              ui.draft.addEventListener('input', () => sizeDraft());
              ui.timeline.addEventListener('scroll', () => {
                state.timelineScrollTop = ui.timeline.scrollTop;
                state.timelineFollowTail = isElementAtBottom(ui.timeline);
                ui.jumpLatest.hidden = state.timelineFollowTail;
              });
              ui.timeline.addEventListener('click', (event) => {
                const toggle = event.target.closest('.thought-toggle');
                if (!toggle) return;
                const key = String(toggle.dataset.thoughtKey ?? '');
                if (!key || !state.humanReasoningByResponse.has(key)) return;
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
                setSplitFromClientX(event.clientX);
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
                const current = state.allocation ?? browser.authoredBrowserShare;
                if (event.key === 'ArrowLeft' || event.key === 'ArrowRight') {
                  event.preventDefault();
                  state.allocation = Math.min(0.85, Math.max(0.15, current + (event.key === 'ArrowLeft' ? -0.05 : 0.05)));
                  state.collapsed = 'none';
                  applySplit();
                } else if (event.key === 'Home' || event.key === 'Enter') {
                  event.preventDefault();
                  state.allocation = null;
                  state.collapsed = 'none';
                  applySplit();
                }
              });

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
