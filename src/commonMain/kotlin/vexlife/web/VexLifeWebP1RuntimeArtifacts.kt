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
                privatePath: ${jsString(VexLifeProdServiceContract.PrivatePath)},
                privateControlPath: ${jsString(VexLifeProdServiceContract.PrivateControlPath)},
                privateSendPath: ${jsString(VexLifeProdServiceContract.PrivateSendPath)},
                privateNoteProposePath: ${jsString(VexLifeProdServiceContract.PrivateNoteProposePath)},
                privateNoteKeepPath: ${jsString(VexLifeProdServiceContract.PrivateNoteKeepPath)},
                privateNoteDeclinePath: ${jsString(VexLifeProdServiceContract.PrivateNoteDeclinePath)},
                visionPath: ${jsString(VexLifeProdServiceContract.VisionPath)},
                visionControlPath: ${jsString(VexLifeProdServiceContract.VisionControlPath)},
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
                readAloud: byId('read-aloud'),
                readAloudStatus: byId('read-aloud-status'),
                readAloudPlay: byId('read-aloud-play'),
                readAloudStop: byId('read-aloud-stop'),
                readAloudPrevious: byId('read-aloud-previous'),
                readAloudNext: byId('read-aloud-next'),
                readAloudChoose: byId('read-aloud-choose'),
                readAloudRate: byId('read-aloud-rate'),
                readAloudRateValue: byId('read-aloud-rate-value'),
                readAloudVoice: byId('read-aloud-voice'),
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
                privatePending: null,
                app: (() => {
                  try {
                    const saved = localStorage.getItem(apps.storageKey);
                    if (saved && apps.keys.includes(saved)) return saved;
                  } catch {}
                  return apps.sourceTemplate;
                })(),
                memory: null,
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
                expandedThoughts: new Set(),
                timelineFollowTail: true,
                timelineScrollTop: 0,
                reasoningViewports: new Map(),
                theme: null
              };

              const QUICK_ACTIONS = [
                { key: 'journeys', label: 'Journeys', icon: '<path d="M4 15.5c0-2 1.6-3 3.4-3.4l3.4-.8 2.2-4.3c.4-.8 1.4-1 2-.3l.6.6c.5.5.5 1.3 0 1.8l-1 1.2 2.8.8c1.4.4 2.4 1.7 2.4 3.2v1.2H4z"/><path d="M8 12l1.4 2M11 11.4l1.3 2"/>' },
                { key: 'read-aloud', label: 'Read aloud', bound: true, icon: '<path d="M4 9.5h3.2L11.5 6v12l-4.3-3.5H4z"/><path d="M15 9.2a4 4 0 0 1 0 5.6M17.6 6.8a7.3 7.3 0 0 1 0 10.4"/>' },
                { key: 'theme', label: 'Light or dark', bound: true, icon: '<circle cx="12" cy="12" r="3.8"/><path d="M12 2.8v2.4M12 18.8v2.4M2.8 12h2.4M18.8 12h2.4M5.5 5.5l1.7 1.7M16.8 16.8l1.7 1.7M5.5 18.5l1.7-1.7M16.8 7.2l1.7-1.7"/>' },
                { key: 'language', label: 'Language', icon: '<path d="M5 20.5V4M5 4.5h12.5l-2 3.5 2 3.5H5"/>' },
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

              // Read aloud, ported from the Vextreme Spiral FAB's Read Aloud orb. It reads the canonical
              // Conversation projection message by message and continues forward. Only voices that
              // run on this machine are offered, so Conversation text is never sent to a speech service.
              const READ_ALOUD_SETTINGS = 'vexlife-prod.presentation.read-aloud/v1';
              const readAloud = {
                supported: 'speechSynthesis' in window && 'SpeechSynthesisUtterance' in window,
                open: false,
                mode: 'idle',
                choosing: false,
                plan: [],
                index: 0,
                generation: 0,
                settings: { rate: 1, voiceURI: '' }
              };

              const loadReadAloudSettings = () => {
                try {
                  const saved = JSON.parse(localStorage.getItem(READ_ALOUD_SETTINGS) || '{}');
                  const rate = Number(saved.rate);
                  if (Number.isFinite(rate)) readAloud.settings.rate = Math.min(1.6, Math.max(0.7, rate));
                  if (typeof saved.voiceURI === 'string') readAloud.settings.voiceURI = saved.voiceURI;
                } catch {}
              };

              const saveReadAloudSettings = () => {
                try {
                  localStorage.setItem(READ_ALOUD_SETTINGS, JSON.stringify(readAloud.settings));
                } catch {}
              };

              const speechChunks = (text) => {
                const sentences = String(text).replace(/\s+/gu, ' ').trim().match(/[^.!?]+[.!?]*\s*/gu) || [];
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

              const readablePlan = () => (isPrivate()
                ? (state.privateMode?.exchanges ?? []).flatMap((exchange, index) => [
                    { role: 'human', content: exchange.human, eventRef: 'private.' + (index * 2 + 1) },
                    { role: 'vex', content: exchange.vex, eventRef: 'private.' + (index * 2 + 2) }
                  ])
                : (state.conversation?.messages ?? []))
                .filter((message) => String(message.content ?? '').trim())
                .map((message) => ({
                  eventRef: String(message.eventRef ?? ''),
                  speaker: message.role === 'human' ? 'You' : 'Vex',
                  chunks: speechChunks(message.content)
                }));

              const localVoices = () => {
                if (!readAloud.supported) return [];
                const language = (document.documentElement.lang || 'en').slice(0, 2).toLowerCase();
                return window.speechSynthesis.getVoices().filter((voice) =>
                  voice.localService && String(voice.lang || '').toLowerCase().startsWith(language));
              };

              const chosenVoice = () => {
                const voices = localVoices();
                return voices.find((voice) => voice.voiceURI === readAloud.settings.voiceURI)
                  ?? voices.find((voice) => voice.default)
                  ?? voices[0]
                  ?? null;
              };

              const renderReadAloudVoices = () => {
                if (!ui.readAloudVoice) return;
                const chosen = chosenVoice();
                const options = localVoices().map((voice) => {
                  const option = document.createElement('option');
                  option.value = voice.voiceURI;
                  option.textContent = voice.name;
                  return option;
                });
                ui.readAloudVoice.replaceChildren(...options);
                if (chosen) ui.readAloudVoice.value = chosen.voiceURI;
                ui.readAloudVoice.disabled = !options.length;
              };

              const markReading = () => {
                if (!ui.timeline) return;
                const current = readAloud.mode === 'idle' ? null : readAloud.plan[readAloud.index]?.eventRef;
                for (const item of ui.timeline.querySelectorAll('.message')) {
                  item.classList.toggle('reading', Boolean(current) && item.dataset.eventRef === current);
                }
                ui.timeline.dataset.choosing = String(readAloud.choosing);
              };

              const renderReadAloud = () => {
                if (!ui.readAloud) return;
                ui.readAloud.hidden = !readAloud.open;
                ui.readAloud.dataset.mode = readAloud.mode;
                const action = ui.quickActions?.querySelector('.quick-action[data-action="read-aloud"]');
                if (action) {
                  action.setAttribute('aria-expanded', String(readAloud.open));
                  action.dataset.active = String(readAloud.mode !== 'idle');
                }
                const playing = readAloud.mode === 'playing';
                ui.readAloudPlay.setAttribute('aria-label', playing ? 'Pause' : 'Play');
                ui.readAloudPlay.title = playing ? 'Pause' : 'Play';
                ui.readAloudChoose.setAttribute('aria-pressed', String(readAloud.choosing));
                ui.readAloudRate.value = String(readAloud.settings.rate);
                ui.readAloudRateValue.textContent = readAloud.settings.rate.toFixed(1) + '×';
                const usable = readAloud.supported && localVoices().length > 0;
                ui.readAloudPlay.disabled = !usable;
                ui.readAloudChoose.disabled = !usable;
                ui.readAloudStop.disabled = readAloud.mode === 'idle';
                ui.readAloudPrevious.disabled = readAloud.mode === 'idle' || readAloud.index <= 0;
                ui.readAloudNext.disabled = readAloud.mode === 'idle' || readAloud.index >= readAloud.plan.length - 1;
                const current = readAloud.plan[readAloud.index];
                ui.readAloudStatus.textContent = !readAloud.supported
                  ? "This browser can't read aloud."
                  : !usable
                    ? 'No voice on this Mac is available for reading aloud.'
                    : readAloud.choosing
                      ? 'Choose a message to start from. Esc cancels.'
                      : readAloud.mode === 'idle' || !current
                        ? "Reads Vex's latest reply, or choose where to start."
                        : (playing ? 'Reading ' : 'Paused · ') + current.speaker +
                          ' · message ' + (readAloud.index + 1) + ' of ' + readAloud.plan.length;
                markReading();
              };

              const stopReading = () => {
                readAloud.generation += 1;
                if (readAloud.supported) window.speechSynthesis.cancel();
                readAloud.mode = 'idle';
                renderReadAloud();
              };

              const speakFrom = (index, chunk) => {
                const generation = ++readAloud.generation;
                window.speechSynthesis.cancel();
                const message = readAloud.plan[index];
                if (!message) {
                  stopReading();
                  return;
                }
                readAloud.index = index;
                if (chunk >= message.chunks.length) {
                  speakFrom(index + 1, 0);
                  return;
                }
                const utterance = new SpeechSynthesisUtterance(message.chunks[chunk]);
                utterance.rate = readAloud.settings.rate;
                const voice = chosenVoice();
                if (voice) {
                  utterance.voice = voice;
                  utterance.lang = voice.lang;
                }
                utterance.onend = () => {
                  if (generation === readAloud.generation && readAloud.mode === 'playing') speakFrom(index, chunk + 1);
                };
                utterance.onerror = (event) => {
                  if (generation !== readAloud.generation || event.error === 'interrupted' || event.error === 'canceled') return;
                  stopReading();
                  notify("Read aloud stopped because the voice couldn't continue.");
                };
                readAloud.mode = 'playing';
                renderReadAloud();
                window.speechSynthesis.speak(utterance);
              };

              const startReading = (eventRef) => {
                readAloud.plan = readablePlan();
                readAloud.choosing = false;
                if (!readAloud.plan.length) {
                  renderReadAloud();
                  notify('There is nothing to read yet.');
                  return;
                }
                let index = eventRef ? readAloud.plan.findIndex((message) => message.eventRef === eventRef) : -1;
                if (index < 0) {
                  const vexIndexes = readAloud.plan.map((message, at) => (message.speaker === 'Vex' ? at : -1)).filter((at) => at >= 0);
                  index = vexIndexes.length ? vexIndexes[vexIndexes.length - 1] : readAloud.plan.length - 1;
                }
                speakFrom(index, 0);
              };

              const toggleReading = () => {
                if (readAloud.mode === 'playing') {
                  window.speechSynthesis.pause();
                  readAloud.mode = 'paused';
                } else if (readAloud.mode === 'paused') {
                  window.speechSynthesis.resume();
                  readAloud.mode = 'playing';
                } else {
                  startReading(null);
                  return;
                }
                renderReadAloud();
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
                renderReadAloud();
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

              const navigate = (value) => {
                const pageUrl = admitPageAddress(value);
                const contextFrameUrl = contextFrameUrlFor(pageUrl);

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
                  await privatePost(service.privateSendPath, { content });
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
                  ui.contextNote.textContent =
                    'Canonical turn receipt: ' + priorCount + ' prior Conversation event' + (priorCount === 1 ? '' : 's') +
                    ' selected. Whole-history enumeration: ' + (prompt.wholeHistoryEventEnumerationPerformed ? 'yes' : 'no') + '.';
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
                state.streamPhase = 'CheckingRuntime';
                state.streamedContent = '';
                state.streamedReasoning = '';
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
                      return;
                    }
                    if (event?.type === 'CONTENT_DELTA' && typeof event.delta === 'string') {
                      state.streamPhase = 'Responding';
                      state.streamedContent += event.delta;
                      setCompanionStatus('Checking', 'Responding', '');
                      updateLiveTurn();
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

                  // Preserve the fully streamed answer on screen until the canonical projection has
                  // arrived. Live presentation remains non-authoritative; this only prevents a
                  // visible vanish/reappear gap at the completion boundary.
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
                } catch (error) {
                  state.sendInFlight = false;
                  state.streamPhase = null;
                  state.streamedContent = '';
                  state.streamedReasoning = '';
                  const status = Number(error?.vexStatus ?? 0);
                  const retryAllowed = error?.vexResponse?.retrySameSendAttemptAllowed;
                  const retireAttempt = status === 400 || retryAllowed === false;

                  if (retireAttempt) {
                    state.pending = null;
                    if (!ui.draft.value) ui.draft.value = pending.content;
                    setCompanionStatus('Unavailable', 'Send failed', '');
                  } else {
                    state.pending = pending;
                    setCompanionStatus('Unavailable', 'Send outcome unresolved', '');
                  }

                  sizeDraft();
                  render();
                  notify(error instanceof Error ? error.message : 'Send failed.');
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

              window.addEventListener('message', (event) => {
                if (event.source !== ui.contextFrame.contentWindow) return;
                const next = normalizeContext(event.data);
                if (!next || !state.contextTargetPageUrl || next.pageUrl !== state.contextTargetPageUrl) return;
                state.browserContext = next;
                state.contextLoading = false;
                state.includePage = true;
                renderFrameStatus('Vex page context is ready for the next turn.', 'ContextReady');
                renderComposer();
              });

              ui.pageFrame.addEventListener('load', () => {
                if (state.humanFrameLoadExpected) {
                  state.humanFrameLoadExpected = false;
                  if (state.contextTargetPageUrl && !state.browserContext) {
                    renderFrameStatus('Page opened directly · Vex page context is available on demand.', 'Direct');
                  } else if (!state.contextTargetPageUrl) {
                    renderFrameStatus('Page opened directly · Vex page context is not available for this site yet.', 'Direct');
                  }
                } else {
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
                if (action.key === 'read-aloud') {
                  readAloud.open = !readAloud.open;
                  if (!readAloud.open) readAloud.choosing = false;
                  renderReadAloudVoices();
                  renderReadAloud();
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

              loadReadAloudSettings();
              if (readAloud.supported) {
                window.speechSynthesis.addEventListener('voiceschanged', () => {
                  renderReadAloudVoices();
                  renderReadAloud();
                });
              }
              ui.readAloudPlay.addEventListener('click', toggleReading);
              ui.readAloudStop.addEventListener('click', stopReading);
              ui.readAloudPrevious.addEventListener('click', () => speakFrom(Math.max(0, readAloud.index - 1), 0));
              ui.readAloudNext.addEventListener('click', () => speakFrom(readAloud.index + 1, 0));
              ui.readAloudChoose.addEventListener('click', () => {
                readAloud.choosing = !readAloud.choosing;
                renderReadAloud();
              });
              ui.readAloudRate.addEventListener('input', () => {
                readAloud.settings.rate = Math.min(1.6, Math.max(0.7, Number(ui.readAloudRate.value) || 1));
                saveReadAloudSettings();
                renderReadAloud();
              });
              ui.readAloudVoice.addEventListener('change', () => {
                readAloud.settings.voiceURI = ui.readAloudVoice.value;
                saveReadAloudSettings();
              });
              ui.timeline.addEventListener('click', (event) => {
                if (!readAloud.choosing) return;
                const item = event.target.closest('.message');
                if (!item?.dataset.eventRef) return;
                event.preventDefault();
                startReading(item.dataset.eventRef);
              });
              document.addEventListener('keydown', (event) => {
                if (event.key !== 'Escape') return;
                if (readAloud.choosing) {
                  readAloud.choosing = false;
                  renderReadAloud();
                } else if (readAloud.open) {
                  readAloud.open = false;
                  renderReadAloud();
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

              void Promise.all([loadSourceCurrentness(), loadCurrentState(), loadConversation(), loadContext(), loadVision()])
                .then(() => {
                  render();
                  navigate(browser.initialPageUrl);
                  renderApp();
                  void loadPrivate().catch(() => {});
                  void loadMemory().catch(() => {});
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
