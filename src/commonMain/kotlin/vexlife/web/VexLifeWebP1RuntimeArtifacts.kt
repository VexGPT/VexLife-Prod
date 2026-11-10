package vexlife.web

import vexlife.browser.VexLifeProdBrowserContract
import vexlife.service.VexLifeProdServiceContract

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
        val admittedHosts =
            VexLifeProdBrowserContract.AdmittedHosts
                .joinToString(
                    separator = ",",
                    prefix = "[",
                    postfix = "]",
                ) {
                    jsString(
                        it,
                    )
                }

        return """
            // Generated VexLife-Prod A003-faithful presentation over current New-Beginnings owners.
            (() => {
              const root = document.getElementById('workspace');
              if (!root) return;

              const service = Object.freeze({
                sourceCurrentnessPath: ${jsString(VexLifeProdServiceContract.SourceCurrentnessPath)},
                conversationPath: ${jsString(VexLifeProdServiceContract.ConversationPath)},
                contextPath: ${jsString(VexLifeProdServiceContract.ContextPath)},
                sendPath: ${jsString(VexLifeProdServiceContract.ConversationSendPath)}
              });
              const browser = Object.freeze({
                initialPageUrl: ${jsString(VexLifeProdBrowserContract.InitialPageUrl)},
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
                quickActions: byId('quick-actions'),
                pageFrame: byId('page-frame'),
                viewportEmpty: byId('viewport-empty'),
                frameStatus: byId('frame-status'),
                companionTitle: byId('companion-title'),
                companionStatus: byId('companion-status'),
                companionStatusText: byId('companion-status-text'),
                companionStatusModel: byId('companion-status-model'),
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
                thoughtVisibility: byId('thought-visibility'),
                lowMemoryAlert: byId('low-memory-alert'),
                alertThreshold: byId('alert-threshold'),
                cleanChat: byId('clean-chat'),
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
                browserContext: null,
                includePage: false,
                view: 'conversation',
                allocation: null,
                collapsed: 'none',
                pending: null,
                sendInFlight: false,
                thoughtsVisible: true,
                humanReasoningByResponse: new Map(),
                expandedThoughts: new Set(),
                theme: null
              };

              const QUICK_ACTIONS = [
                { key: 'journeys', label: 'Journeys', icon: '<path d="M4 15.5c0-2 1.6-3 3.4-3.4l3.4-.8 2.2-4.3c.4-.8 1.4-1 2-.3l.6.6c.5.5.5 1.3 0 1.8l-1 1.2 2.8.8c1.4.4 2.4 1.7 2.4 3.2v1.2H4z"/><path d="M8 12l1.4 2M11 11.4l1.3 2"/>' },
                { key: 'sound', label: 'Sound', icon: '<path d="M4 9.5h3.2L11.5 6v12l-4.3-3.5H4z"/><path d="M15 9.2a4 4 0 0 1 0 5.6M17.6 6.8a7.3 7.3 0 0 1 0 10.4"/>' },
                { key: 'theme', label: 'Light or dark', bound: true, icon: '<circle cx="12" cy="12" r="3.8"/><path d="M12 2.8v2.4M12 18.8v2.4M2.8 12h2.4M18.8 12h2.4M5.5 5.5l1.7 1.7M16.8 16.8l1.7 1.7M5.5 18.5l1.7-1.7M16.8 7.2l1.7-1.7"/>' },
                { key: 'language', label: 'Language', icon: '<path d="M5 20.5V4M5 4.5h12.5l-2 3.5 2 3.5H5"/>' },
                { key: 'evolution', label: 'Evolution', icon: '<path d="M12.2 12.1c.4-.6-.3-1.4-1-1.2-1.3.4-1.3 2.3-.1 3 1.8 1 4-.5 3.9-2.6-.1-3-3.6-4.3-6-2.8-3.1 2-2.6 6.9.7 8.3 3.9 1.6 8.3-1.2 8.2-5.6"/>' }
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

              const renderQuickActions = () => {
                if (!ui.quickActions) return;
                const sourceOnly = [...ui.quickActions.children].filter((item) => item.hidden);
                const items = QUICK_ACTIONS.map((action) => {
                  const item = document.createElement('li');
                  const button = document.createElement('button');
                  button.type = 'button';
                  button.className = 'quick-action';
                  button.dataset.action = action.key;
                  button.dataset.bound = String(Boolean(action.bound));
                  button.setAttribute('aria-label', action.bound ? action.label : action.label + ' (not connected yet)');
                  button.title = action.bound ? action.label : action.label + ': not connected yet';
                  button.innerHTML = '<svg viewBox="0 0 24 24" aria-hidden="true">' + action.icon + '</svg>';
                  item.append(button);
                  return item;
                });
                ui.quickActions.replaceChildren(...sourceOnly, ...items);
              };

              const setCompanionStatus = (stateName, text, model = '') => {
                if (!ui.companionStatus) return;
                ui.companionStatus.dataset.state = stateName;
                ui.companionStatusText.textContent = text;
                ui.companionStatusModel.textContent = model;
              };

              const frameUrlFor = (pageUrl) => {
                const page = new URL(pageUrl);
                if (page.protocol !== 'https:' || page.port || !browser.admittedHosts.includes(page.hostname)) {
                  throw new Error('This browser opens admitted Vex-owned public pages only.');
                }
                return 'http://' + page.hostname + browser.frameHostSuffix + ':' + browser.frameHostPort + page.pathname + page.search + page.hash;
              };

              const admitPageAddress = (value) => {
                let text = String(value ?? '').trim();
                if (!text) throw new Error('Type a web address.');
                if (!/^[a-z][a-z0-9+.-]*:/iu.test(text)) text = 'https://' + text;
                const page = new URL(text);
                if (page.protocol !== 'https:' || page.port || !browser.admittedHosts.includes(page.hostname)) {
                  throw new Error('This page is outside the admitted Vex-owned public boundary.');
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
                state.browserContext = null;
                ui.query.value = pageUrl;
                ui.pageFrame.src = frameUrlFor(pageUrl);
                ui.viewportEmpty.hidden = true;
                renderFrameStatus('Loading page…', 'Loading');
                renderComposer();
              };

              const isNearBottom = () =>
                ui.timeline.scrollHeight - ui.timeline.scrollTop - ui.timeline.clientHeight < 72;

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
                label.textContent = 'responding…';
                thought.append(who, mark, label);

                const bubble = document.createElement('div');
                bubble.className = 'bubble';
                const speaker = document.createElement('p');
                speaker.className = 'speaker';
                speaker.textContent = 'Vex';

                item.append(index, thought, bubble, speaker);
                return item;
              };

              const renderTimeline = () => {
                if (!ui.timeline) return;
                const nearBottom = isNearBottom();
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
                if (nearBottom || state.pending) {
                  ui.timeline.scrollTop = ui.timeline.scrollHeight;
                  ui.jumpLatest.hidden = true;
                }
              };

              const renderTicket = () => {
                const context = state.browserContext;
                const visible = state.includePage && context;
                ui.ticket.hidden = !visible;
                if (!visible) return;
                ui.ticket.dataset.state = 'Ready';
                ui.ticketTitle.textContent = context.title || 'Included page';
                ui.ticketLine.textContent = 'This rendered page text will be given to Vex on the next turn.';
                ui.ticketUrl.textContent = context.pageUrl;
                ui.ticketHash.textContent = '';
                ui.ticketView.dataset.contextReady = 'true';
              };

              const renderComposer = () => {
                const canInclude = Boolean(state.browserContext);
                ui.contextShare.disabled = !canInclude;
                ui.contextShare.dataset.state = canInclude ? 'Ready' : 'Unavailable';
                ui.contextShare.setAttribute('aria-pressed', String(state.includePage && canInclude));
                ui.contextShareLabel.textContent = state.includePage && canInclude ? 'Page included' : 'Include this page';
                ui.composerHold.textContent = !canInclude
                  ? 'Page context becomes available when the opened page reports readable text.'
                  : state.pending && !state.sendInFlight
                    ? 'The prior Send outcome is unresolved. Send again to retry that exact attempt.'
                    : '';
                ui.send.disabled = state.sendInFlight || (!state.pending && !ui.draft.value.trim());
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

                setMeter(ui.memory, [[ui.memoryUsed, 0]], 'Not measured', 'Mac memory is not exposed by the current service', false);
                ui.thoughtVisibility.setAttribute('aria-checked', String(state.thoughtsVisible));
                ui.lowMemoryAlert.setAttribute('aria-checked', 'false');
                ui.lowMemoryAlert.disabled = true;
                ui.alertThreshold.textContent = 'not measured';
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
                renderTimeline();
                renderComposer();
                renderSettings();
              };

              const loadSourceCurrentness = async () => {
                const response = await fetch(service.sourceCurrentnessPath, { headers: { accept: 'application/json' }, cache: 'no-store' });
                state.currentness = await readJson(response);
                root.dataset.vexServiceSourceSha = String(state.currentness.sourceSha ?? '');
                root.dataset.vexServicePid = String(state.currentness.processPid ?? '');
                setCompanionStatus('Ready', 'Ready', ui.companionStatusModel.textContent);
              };

              const loadConversation = async () => {
                const response = await fetch(service.conversationPath, { headers: { accept: 'application/json' }, cache: 'no-store' });
                state.conversation = await readJson(response);
                const messages = state.conversation?.messages ?? [];
                const latest = [...messages].reverse().find((message) => message.role === 'vex' && message.model);
                setCompanionStatus('Ready', 'Ready', latest?.model ?? '');
                renderTimeline();
              };

              const loadContext = async () => {
                const response = await fetch(service.contextPath, { headers: { accept: 'application/json' }, cache: 'no-store' });
                state.context = await readJson(response);
                renderSettings();
              };

              const send = async () => {
                if (state.sendInFlight) return;

                const retrying = state.pending !== null;
                const content = retrying ? state.pending.content : ui.draft.value.trim();
                if (!content) return;

                const pending = state.pending ?? Object.freeze({
                  idempotencyKey: 'send.' + crypto.randomUUID().toLowerCase(),
                  content,
                  browserContext: state.includePage ? state.browserContext : null,
                  viewableThoughts: state.thoughtsVisible
                });
                state.pending = pending;
                state.sendInFlight = true;
                if (!retrying) {
                  ui.draft.value = '';
                  sizeDraft();
                }
                setCompanionStatus('Checking', retrying ? 'Retrying exact Send' : 'Responding', '');
                render();

                try {
                  const response = await fetch(service.sendPath, {
                    method: 'POST',
                    headers: { accept: 'application/json', 'content-type': 'application/json' },
                    body: JSON.stringify(pending)
                  });
                  const result = await readJson(response);
                  state.pending = null;
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
                  setCompanionStatus('Ready', 'Ready', result.model ?? '');
                  render();

                  try {
                    await Promise.all([loadConversation(), loadContext()]);
                  } catch (projectionError) {
                    notify(
                      'Turn completed, but the current projection could not be refreshed: ' +
                      (projectionError instanceof Error ? projectionError.message : 'refresh failed.')
                    );
                  }
                } catch (error) {
                  state.sendInFlight = false;
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
                  [['Page', state.browserContext.pageUrl], ['Title', state.browserContext.title || 'None'], ['Read from', 'Rendered page text']],
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
                  ui.addressError.textContent = error instanceof Error ? error.message : 'Page is not admitted.';
                  ui.addressError.hidden = false;
                }
              });
              ui.query.addEventListener('input', () => { ui.addressError.hidden = true; });
              ui.reload.addEventListener('click', () => {
                try { navigate(ui.query.value); } catch (error) { notify(error.message); }
              });
              ui.appSelector.addEventListener('change', () => { ui.appSelector.value = 'browser'; });

              window.addEventListener('message', (event) => {
                if (event.source !== ui.pageFrame.contentWindow) return;
                const next = normalizeContext(event.data);
                if (!next) return;
                state.browserContext = next;
                ui.query.value = next.pageUrl;
                renderFrameStatus();
                renderComposer();
              });

              ui.pageFrame.addEventListener('load', () => {
                state.browserContext = null;
                renderFrameStatus('Waiting for readable page context…', 'Loading');
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
                notify(action.label + " isn't connected in this workspace yet.");
              });

              ui.viewSwitch.addEventListener('click', () => {
                state.view = state.view === 'settings' ? 'conversation' : 'settings';
                renderCompanionHead();
                if (state.view === 'settings') renderSettings();
              });

              ui.contextShare.addEventListener('click', () => {
                if (!state.browserContext) return;
                state.includePage = !state.includePage;
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
                if (isNearBottom()) ui.jumpLatest.hidden = true;
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
                ui.timeline.scrollTop = ui.timeline.scrollHeight;
                ui.jumpLatest.hidden = true;
              });

              ui.ticketView.addEventListener('click', openCurrentContext);
              ui.inspectorClose.addEventListener('click', () => ui.inspector.close());

              ui.thoughtVisibility.addEventListener('click', () => {
                state.thoughtsVisible = !state.thoughtsVisible;
                saveThoughtVisibility();
                renderSettings();
                renderTimeline();
              });

              ui.cleanChat.addEventListener('click', () => {
                notify('Current New-Beginnings Conversation reset/archive semantics are not owned by this presentation yet.');
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

              void Promise.all([loadSourceCurrentness(), loadConversation(), loadContext()])
                .then(() => {
                  render();
                  navigate(browser.initialPageUrl);
                })
                .catch((error) => {
                  setCompanionStatus('Unavailable', 'Unavailable', '');
                  notify(error instanceof Error ? error.message : 'Local service is not ready.');
                });

              setInterval(() => {
                if (state.pending) return;
                void loadConversation().catch(() => {});
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
