// VexLife web workspace exploration: renderer and Effect realization.
//
// The UI renders DisplayState and emits typed Controls. Meaning is decided by the Families in
// families.mjs (checked against the Kotlin Families); this file only:
//   - routes Controls to their owning Family,
//   - realizes the returned Effect against the local server,
//   - returns observed outcomes to the same Family,
//   - projects DisplayState + the Composer relationship into the DOM.
// Presentation preferences (theme, viewable thoughts, low memory alert, seam position) are local
// presentation, not Families.
//
// [VXG RealForever]
import { bindWorkspace } from './binding.mjs';
import { BrowserContext, CompanionRuntime, Composer, ConversationTurn } from './families.mjs';
import { frameUrlFor } from './frame-address.mjs';
import { renderProse } from './prose.mjs';
import { getJson, postJson, requestTurnStream } from './turn-stream.mjs';

const DEFAULT_PAGE = 'https://vgong24.github.io/Vextreme/pages/terrain-map.html';
const RECHECK_WHILE_UNAVAILABLE_MS = 15000;
const RESOURCE_POLL_MS = 20000;
const LOW_MEMORY_SHARE = 0.25;
const CAPTURE_TIMEOUT_MS = 2500;
const UNREPORTED_LOAD_GRACE_MS = 700;
const numberFormat = new Intl.NumberFormat();

const FAMILIES = { companion: CompanionRuntime, turn: ConversationTurn, browser: BrowserContext };

const model = {
  companion: CompanionRuntime.initial,
  turn: ConversationTurn.initial,
  browser: BrowserContext.initial,
  transcript: [],
  live: null,
  view: 'conversation',
  session: null,
  resources: null,
  lastProbeAt: null,
  allocation: null,
  collapsed: null,
  expandedThoughts: new Set(),
  lowMemoryNotified: false,
  preferences: { thoughtsVisible: true, lowMemoryAlert: true, theme: null }
};

let ui = null;
let vex = null;
let snackbarTimer = null;
let recheckTimer = null;

// Realization custody for Vex's browser (not semantic state; the Family owns meaning):
//   - which frame documents are current (a document from before the last ShowPage is stale),
//   - the one context acquisition in flight (a newer one supersedes and aborts it),
//   - capture requests waiting for the frame's answer.
const frameCustody = {
  frameOrigin: null,
  currentDoc: null,
  docAtLastLoad: null,
  staleDocs: new Set(),
  acquisition: null,
  captures: new Map()
};

// ------------------------------------------------------------------ local presentation preferences

const PREFERENCES_KEY = 'vexlife.workspace.exploration.preferences';

function loadPreferences() {
  try {
    const stored = JSON.parse(localStorage.getItem(PREFERENCES_KEY) || '{}');
    Object.assign(model.preferences, {
      thoughtsVisible: stored.thoughtsVisible !== false,
      lowMemoryAlert: stored.lowMemoryAlert !== false,
      theme: stored.theme === 'light' || stored.theme === 'dark' ? stored.theme : null
    });
  } catch { /* storage unavailable: defaults stand */ }
}

function savePreferences() {
  try { localStorage.setItem(PREFERENCES_KEY, JSON.stringify(model.preferences)); } catch { /* not persisted */ }
}

function applyTheme() {
  if (model.preferences.theme) document.documentElement.dataset.theme = model.preferences.theme;
  else delete document.documentElement.dataset.theme;
}

function currentTheme() {
  return model.preferences.theme ?? (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');
}

// ------------------------------------------------------------------ Control / Observation routing

function control(familyKey, value) {
  const result = FAMILIES[familyKey].act(model[familyKey], value);
  if (result === null) return false;
  if (result.admittedState) model[familyKey] = result.admittedState;
  render();
  if (result.effect) realize(familyKey, result.effect);
  return true;
}

function observe(familyKey, observation) {
  const interpreted = FAMILIES[familyKey].interpret(model[familyKey], observation);
  // BrowserContext answers with { admittedState, effect }: following a page may need its receipt.
  if (familyKey === 'browser') {
    model.browser = interpreted.admittedState;
    render();
    if (interpreted.effect) realize('browser', interpreted.effect);
    return;
  }
  model[familyKey] = interpreted;
  render();
}

async function realize(familyKey, effect) {
  switch (effect.type) {
    case 'ProbeRuntime': {
      let observation;
      try {
        observation = await postJson('/api/companion/probe');
      } catch {
        observation = { type: 'RuntimeUnavailable', reason: 'BridgeUnreachable' };
      }
      model.lastProbeAt = new Date();
      observe('companion', observation);
      scheduleRecheck();
      refreshResources();
      return;
    }
    case 'ShowPage':
      return showPage(effect.page);
    case 'AcquireContext':
      return acquireContext(effect.page);
    case 'RequestTurn':
      return realizeTurn(effect);
    case 'CancelTurn':
      postJson(`/api/conversation/turns/${effect.turn}/cancel`).catch(() => {});
      return;
    default:
      throw new Error(`no realization for ${familyKey} effect ${effect.type}`);
  }
}

// ------------------------------------------------------------------ Vex's browser realization

function showPage(page) {
  const framed = model.session?.frameHost ? frameUrlFor(page, model.session.frameHost) : null;
  if (frameCustody.currentDoc) frameCustody.staleDocs.add(frameCustody.currentDoc);
  frameCustody.currentDoc = null;
  frameCustody.docAtLastLoad = null;
  if (!framed) {
    // Without the frame host the page cannot be followed; it is shown as itself, unlocated.
    ui.pageFrame.src = page;
    return;
  }
  ui.pageFrame.src = framed;
}

function isFrameHostOrigin(origin) {
  const host = model.session?.frameHost;
  if (!host) return false;
  try {
    const url = new URL(origin);
    return url.protocol === 'http:' && url.port === String(host.port) && url.hostname.endsWith(`.${host.token}.localhost`);
  } catch {
    return false;
  }
}

/** Reports from the reporter inside the frame become BrowserContext observations. */
function onFrameMessage(event) {
  if (event.source !== ui.pageFrame.contentWindow || !isFrameHostOrigin(event.origin)) return;
  const data = event.data;
  if (!data || data.vexFrame !== 1 || typeof data.doc !== 'string') return;
  if (frameCustody.staleDocs.has(data.doc)) return;

  if (data.kind === 'captured') {
    const waiting = frameCustody.captures.get(data.nonce);
    if (waiting) {
      frameCustody.captures.delete(data.nonce);
      waiting(data);
    }
    return;
  }

  if (frameCustody.currentDoc !== data.doc) {
    if (frameCustody.currentDoc) frameCustody.staleDocs.add(frameCustody.currentDoc);
    frameCustody.currentDoc = data.doc;
  }
  frameCustody.frameOrigin = event.origin;

  const page = BrowserContext.parsePageAddress(typeof data.href === 'string' ? data.href : '');
  if (data.kind === 'located') {
    const title = typeof data.title === 'string' && data.title.trim() ? data.title.trim().slice(0, 300) : null;
    observe('browser', page ? { type: 'Located', page, title } : { type: 'FrameUnlocated' });
  } else if (data.kind === 'refused') {
    const reason = data.reason === 'LocalNetwork' ? 'LocalNetwork' : 'Unreachable';
    observe('browser', page ? { type: 'FrameRefused', page, reason } : { type: 'FrameUnlocated' });
  }
}

/**
 * Every load should come with a report from a new document. A load whose document never reported
 * means the frame shows something that cannot say where it is (it left the frame host).
 */
function onFrameLoad() {
  if (!ui.pageFrame.getAttribute('src') || model.browser.type !== 'Addressed') return;
  const settle = () => {
    if (frameCustody.currentDoc !== null && frameCustody.currentDoc !== frameCustody.docAtLastLoad) {
      frameCustody.docAtLastLoad = frameCustody.currentDoc;
      return true;
    }
    return false;
  };
  if (settle()) return;
  setTimeout(() => {
    if (!settle()) {
      frameCustody.frameOrigin = null;
      observe('browser', { type: 'FrameUnlocated' });
    }
  }, UNREPORTED_LOAD_GRACE_MS);
}

/** Ask the frame for the page text as it is rendered now. Resolves null when it cannot answer. */
function captureFromFrame() {
  const target = ui.pageFrame.contentWindow;
  if (!target || !frameCustody.frameOrigin) return Promise.resolve(null);
  const nonce = `${Date.now().toString(36)}-${Math.random().toString(36).slice(2)}`;
  return new Promise((resolve) => {
    const timer = setTimeout(() => {
      frameCustody.captures.delete(nonce);
      resolve(null);
    }, CAPTURE_TIMEOUT_MS);
    frameCustody.captures.set(nonce, (data) => {
      clearTimeout(timer);
      resolve(data);
    });
    target.postMessage({ vexFrameCommand: 1, op: 'capture', nonce }, frameCustody.frameOrigin);
  });
}

/**
 * AcquireContext: the text the frame shows when it is showing this page, else the page source.
 * One acquisition is current; a newer one aborts it, and its late answer is never observed.
 */
async function acquireContext(page) {
  frameCustody.acquisition?.controller.abort();
  const acquisition = { page, controller: new AbortController() };
  frameCustody.acquisition = acquisition;
  const { signal } = acquisition.controller;

  let observation = null;
  try {
    const browser = model.browser;
    const showingPage = browser.type === 'Addressed' && browser.page === page && browser.frame.type === 'Showing';
    const captured = showingPage ? await captureFromFrame() : null;
    if (signal.aborted) return;
    if (captured && captured.href === page && typeof captured.text === 'string' && captured.text.trim()) {
      observation = await postJson('/api/context/rendered', { page, title: captured.title, text: captured.text }, { signal });
    } else {
      observation = await postJson('/api/context/acquire', { page }, { signal });
    }
  } catch {
    if (signal.aborted) return;
    observation = { type: 'ContextFailed', page, reason: 'SourceUnreachable' };
  }
  if (frameCustody.acquisition !== acquisition) return;
  frameCustody.acquisition = null;
  observe('browser', observation);
}

async function realizeTurn(effect) {
  model.live = { turn: effect.turn, userText: effect.text, text: '', reasoning: '', prompt: null };
  render();
  let outcome;
  try {
    outcome = await requestTurnStream({
      effect,
      onEvent(event, data) {
        if (data.turn !== undefined && data.turn !== effect.turn) return;
        if (event === 'receipt') model.live.prompt = data.prompt;
        else if (event === 'delta') model.live.text += data.text;
        else if (event === 'reasoning') model.live.reasoning += data.text;
        else if (event === 'record') {
          model.transcript.push(data);
          model.live = null;
        } else if (event === 'observation') {
          observe('turn', data);
          return;
        }
        renderTimeline();
      }
    });
  } catch {
    outcome = { accepted: false, status: 0, detail: null };
  }

  if (!outcome.accepted) {
    model.live = null;
    observe('turn', { type: 'ResponseFailed', turn: effect.turn, reason: outcome.status === 0 ? 'CompanionUnavailable' : 'ModelRequestFailed' });
    if (outcome.detail?.error === 'TURN_SEQUENCE_STALE' || outcome.detail?.error === 'TURN_IN_FLIGHT') {
      notify('Another tab is using this conversation. Reload this tab to continue.');
    }
  } else if (ConversationTurn.activeTurn(model.turn) === effect.turn) {
    // The stream closed without a terminal observation (for example the local server stopped).
    model.live = null;
    observe('turn', { type: 'ResponseFailed', turn: effect.turn, reason: 'StreamInterrupted' });
  }
  refreshResources();
}

function scheduleRecheck() {
  clearTimeout(recheckTimer);
  if (model.companion.type !== 'Unavailable') return;
  recheckTimer = setTimeout(() => {
    if (document.visibilityState === 'visible' && model.companion.type === 'Unavailable') control('companion', { type: 'Check' });
    else scheduleRecheck();
  }, RECHECK_WHILE_UNAVAILABLE_MS);
}

async function refreshResources() {
  try {
    model.resources = await getJson('/api/companion/resources');
  } catch {
    return;
  }
  checkLowMemory();
  renderSettings();
}

function checkLowMemory() {
  const memory = model.resources?.memory;
  if (!memory?.totalBytes) return;
  const share = memory.availableBytes / memory.totalBytes;
  if (share >= LOW_MEMORY_SHARE) {
    model.lowMemoryNotified = false;
    return;
  }
  if (model.preferences.lowMemoryAlert && !model.lowMemoryNotified) {
    model.lowMemoryNotified = true;
    notify(`Memory is low: ${Math.round(share * 100)}% left on this Mac.`);
  }
}

// ------------------------------------------------------------------ Human language

const UNAVAILABLE_HOLD = {
  RuntimeNotListening: "Vex isn't running. Start VexLife, then check again.",
  RuntimeUnhealthy: "Vex's model is loading or unhealthy. Check again in a moment.",
  ModelIdentityMissing: "The model runtime answered but didn't name its model.",
  BridgeUnreachable: 'This workspace lost its local server. Restart it from Terminal.'
};

const HOLD_TEXT = {
  CompanionNotChecked: "Vex hasn't been checked yet.",
  CompanionChecking: 'Checking whether Vex is running.',
  TurnInProgress: '',
  ContextPreparing: 'Reading the page for Vex.'
};

const FAILURE_TEXT = {
  CompanionUnavailable: "Vex couldn't reply: the model runtime wasn't reachable.",
  ModelRequestFailed: "Vex couldn't reply: the model refused the request.",
  StreamInterrupted: "Vex's reply stopped before it finished."
};

const CONTEXT_UNAVAILABLE_TEXT = {
  SourceUnreachable: "Couldn't read this page. Include it again to retry.",
  SourceNotAdmitted: "This page can't be captured for Vex.",
  NoReadableText: 'This page has no readable text to give Vex.'
};

const QUICK_ACTIONS = [
  { key: 'journeys', label: 'Journeys', icon: '<path d="M4 15.5c0-2 1.6-3 3.4-3.4l3.4-.8 2.2-4.3c.4-.8 1.4-1 2-.3l.6.6c.5.5.5 1.3 0 1.8l-1 1.2 2.8.8c1.4.4 2.4 1.7 2.4 3.2v1.2H4z"/><path d="M8 12l1.4 2M11 11.4l1.3 2"/>' },
  { key: 'sound', label: 'Sound', icon: '<path d="M4 9.5h3.2L11.5 6v12l-4.3-3.5H4z"/><path d="M15 9.2a4 4 0 0 1 0 5.6M17.6 6.8a7.3 7.3 0 0 1 0 10.4"/>' },
  { key: 'theme', label: 'Light or dark', bound: true, icon: '<circle cx="12" cy="12" r="3.8"/><path d="M12 2.8v2.4M12 18.8v2.4M2.8 12h2.4M18.8 12h2.4M5.5 5.5l1.7 1.7M16.8 16.8l1.7 1.7M5.5 18.5l1.7-1.7M16.8 7.2l1.7-1.7"/>' },
  { key: 'language', label: 'Language', icon: '<path d="M5 20.5V4M5 4.5h12.5l-2 3.5 2 3.5H5"/>' },
  { key: 'evolution', label: 'Evolution', icon: '<path d="M12.2 12.1c.4-.6-.3-1.4-1-1.2-1.3.4-1.3 2.3-.1 3 1.8 1 4-.5 3.9-2.6-.1-3-3.6-4.3-6-2.8-3.1 2-2.6 6.9.7 8.3 3.9 1.6 8.3-1.2 8.2-5.6"/>' }
];

const shortUrl = (url) => url.replace(/^https:\/\//u, '');

const ADDRESS_REFUSAL_TEXT = {
  Empty: 'Type a web address.',
  TooLong: 'That address is too long to open here.',
  Malformed: "That doesn't look like a web address. Try something like example.com.",
  UnsupportedScheme: "Only web pages (http and https) open in Vex's browser.",
  CredentialsInAddress: "Addresses with a user name or password don't open here.",
  LocalNetwork: "Vex's browser opens public pages only, not this Mac or your local network.",
  UnsupportedHost: "This kind of address doesn't open here yet."
};
const characters = (count) => `${numberFormat.format(count)} character${count === 1 ? '' : 's'}`;
const tokens = (count) => `${numberFormat.format(Math.round(count))} tokens`;
const gigabytes = (bytes) => `${(bytes / 1024 ** 3).toFixed(1)} GB`;

// ------------------------------------------------------------------ Rendering

function render() {
  if (!ui) return;
  renderCompanionHead();
  renderBrowser();
  renderComposer();
  renderTimeline();
  renderSettings();
}

function renderCompanionHead() {
  const display = CompanionRuntime.display(model.companion);
  const status = vex.el('CompanionStatus');
  status.dataset.state = display.type;
  ui.statusText.textContent = {
    NotChecked: 'Not checked',
    Checking: 'Checking',
    Ready: 'Ready',
    Unavailable: 'Unavailable'
  }[display.type];
  ui.statusModel.textContent = display.type === 'Ready' ? display.modelLabel : '';
  status.title = display.type === 'Unavailable' ? UNAVAILABLE_HOLD[display.reason] : display.type === 'Ready' ? model.companion.modelRef : '';
  const check = vex.el('CompanionCheck');
  check.hidden = !(display.type === 'Unavailable' || display.type === 'NotChecked');
  check.disabled = model.companion.type === 'Checking';

  const settings = model.view === 'settings';
  ui.companionTitle.textContent = settings ? 'Settings' : 'Converse with Vex';
  status.hidden = settings;
  if (settings) check.hidden = true;
  const viewSwitch = vex.el('CompanionViewSwitch');
  viewSwitch.textContent = settings ? 'Return' : 'Settings';
  viewSwitch.setAttribute('aria-expanded', String(settings));
  vex.el('Conversation').hidden = settings;
  vex.el('Settings').hidden = !settings;
}

function renderBrowser() {
  const display = BrowserContext.display(model.browser);
  const query = vex.el('BrowserQuery');
  // The address follows the frame, except while the human is typing a new one.
  if (document.activeElement !== query) query.value = display.pageUrl ? shortUrl(display.pageUrl) : '';
  query.title = display.pageUrl ?? '';

  ui.viewportEmpty.hidden = display.frame.type !== 'NoPage';
  ui.reload.disabled = display.frame.type === 'NoPage';
  ui.pageFrame.title = display.frame.type === 'Showing' && display.frame.title ? display.frame.title : 'Opened page';
  document.title = display.frame.type === 'Showing' && display.frame.title ? `${display.frame.title} · VexLife` : 'VexLife';

  const status = vex.el('BrowserFrameStatus');
  const frameText = display.frame.type === 'Unlocated'
    ? "This page didn't report its address, so the address field still shows the last page Vex's browser could see."
    : '';
  status.textContent = frameText;
  status.hidden = frameText === '';
  status.dataset.frame = display.frame.type;
}

function renderComposer() {
  const composer = Composer.converge(model.companion, model.turn, model.browser);
  const browserDisplay = BrowserContext.display(model.browser);
  const turnDisplay = ConversationTurn.display(model.turn);

  const draft = ui.draft.value.trim();
  vex.el('Send').disabled = !composer.sendAllowed || draft.length === 0;
  const stopSupported = model.session?.turnCancellation === true;
  vex.el('Send').hidden = stopSupported && (composer.stopAvailable || turnDisplay.type === 'Stopping');
  vex.el('Stop').hidden = !stopSupported || !(composer.stopAvailable || turnDisplay.type === 'Stopping');
  vex.el('Stop').disabled = !composer.stopAvailable;

  let hold = '';
  if (composer.hold === 'CompanionUnavailable') hold = UNAVAILABLE_HOLD[model.companion.reason];
  else if (composer.hold) hold = HOLD_TEXT[composer.hold];
  ui.hold.textContent = hold;

  const progress = vex.el('ResponseProgress');
  const progressText = turnDisplay.type === 'Failed' ? FAILURE_TEXT[turnDisplay.reason] : turnDisplay.type === 'Stopping' ? 'Stopping' : '';
  progress.textContent = progressText;
  progress.hidden = progressText === '';
  progress.dataset.state = turnDisplay.type;

  const share = vex.el('ContextShare');
  const context = browserDisplay.context;
  const sharing = context.type === 'Shared' || context.type === 'Preparing';
  share.disabled = browserDisplay.frame.type === 'NoPage';
  share.setAttribute('aria-pressed', String(sharing));
  share.dataset.state = context.type;
  ui.shareLabel.textContent = context.type === 'Unavailable'
    ? CONTEXT_UNAVAILABLE_TEXT[context.reason]
    : sharing ? 'Page included' : 'Include this page';

  const ticket = vex.el('ContextReceipt');
  const wasHidden = ticket.hidden;
  ticket.hidden = !sharing;
  ticket.dataset.state = context.type;
  const contextBridgeConnected = model.session?.browserContextBridge === true;
  if (context.type === 'Preparing') {
    ui.ticketTitle.textContent = 'Reading the page';
    ui.ticketLine.textContent = contextBridgeConnected
      ? (browserDisplay.frame.type === 'Loading'
          ? 'Waiting for the page to open, then its admitted text can be supplied to Vex.'
          : 'The admitted page text will be available once it has been read.')
      : 'Forming a bounded page-text receipt. Canonical prompt handoff is not connected yet.';
    ui.ticketUrl.textContent = browserDisplay.pageUrl ? shortUrl(browserDisplay.pageUrl) : '';
    ui.ticketHash.textContent = '';
    ui.ticketDiverged.hidden = true;
    ui.ticketView.disabled = true;
  } else if (context.type === 'Shared') {
    const { receipt } = context;
    ui.ticketTitle.textContent = receipt.title || shortUrl(receipt.sourceUrl);
    const what = receipt.capture === 'Rendered' ? 'what this page shows' : "this page's source text";
    ui.ticketLine.textContent = contextBridgeConnected
      ? (receipt.truncated
          ? `Vex can receive the first ${characters(receipt.characters)} of ${what}.`
          : `Vex can receive ${characters(receipt.characters)} of ${what}.`)
      : (receipt.truncated
          ? `Captured the first ${characters(receipt.characters)} of ${what}; it is not yet admitted to Vex's prompt.`
          : `Captured ${characters(receipt.characters)} of ${what}; it is not yet admitted to Vex's prompt.`);
    ui.ticketUrl.textContent = shortUrl(receipt.sourceUrl);
    ui.ticketHash.textContent = `sha256 ${receipt.sha256.slice(0, 12)}`;
    ui.ticketDiverged.hidden = !context.frameDiverged;
    ui.ticketView.disabled = false;
    ui.ticketView.dataset.contextRef = receipt.contextRef;
  }
  if (wasHidden && !ticket.hidden) {
    ticket.classList.remove('arrive');
    void ticket.offsetWidth;
    ticket.classList.add('arrive');
  }

  vex.el('CleanChat').disabled = composer.hold === 'TurnInProgress';
}

const THOUGHT_MARK = '<svg class="thought-mark" viewBox="0 0 16 16" aria-hidden="true"><path d="M8 1.8 14.2 8 8 14.2 1.8 8z"/><path d="M8 5 11 8 8 11 5 8z"/></svg>';

function thoughtMark() {
  const holder = document.createElement('span');
  holder.innerHTML = THOUGHT_MARK;
  return holder.firstElementChild;
}

function messageNode({ who, index, text, prose = false }) {
  const item = document.createElement('li');
  item.className = `message message-${who}`;

  const bubble = document.createElement('div');
  bubble.className = 'bubble';
  if (prose) renderProse(bubble, text);
  else {
    text.split('\n').forEach((line, lineIndex) => {
      if (lineIndex > 0) bubble.append(document.createElement('br'));
      bubble.append(document.createTextNode(line));
    });
  }

  const marker = document.createElement('p');
  marker.className = 'message-index';
  marker.textContent = `[${String(index).padStart(2, '0')}]`;
  marker.setAttribute('aria-label', `Message ${index}`);

  const speaker = document.createElement('p');
  speaker.className = 'speaker';
  speaker.textContent = who === 'vex' ? 'Vex' : 'You';

  item.append(bubble, marker, speaker);
  return item;
}

function thoughtNode({ key, reasoning, thinking }) {
  if (!model.preferences.thoughtsVisible && !thinking) return null;
  const holder = document.createElement('div');
  holder.className = 'thought';

  if (thinking) {
    const row = document.createElement('p');
    row.className = 'thinking-now';
    const who = document.createElement('span');
    who.className = 'speaker-inline';
    who.textContent = 'Vex';
    const label = document.createElement('span');
    label.textContent = 'Thinking…';
    row.append(who, thoughtMark(), label);
    row.style.margin = '0';
    holder.append(row);
    if (model.preferences.thoughtsVisible && reasoning) {
      const box = document.createElement('div');
      box.className = 'reasoning';
      box.textContent = reasoning;
      holder.append(box);
      requestAnimationFrame(() => { box.scrollTop = box.scrollHeight; });
    }
    return holder;
  }

  if (!reasoning) return null;
  const expanded = model.expandedThoughts.has(key);
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
}

function receiptNode({ prompt, outcome, failure, usage }) {
  const facts = document.createElement('p');
  facts.className = 'receipt-line';
  if (outcome === 'Cancelled') facts.append('Stopped. ');
  if (outcome === 'Failed') facts.append(`${FAILURE_TEXT[failure?.reason] ?? "Vex couldn't reply."} `);
  if (prompt) {
    if (prompt.context) {
      const button = document.createElement('button');
      button.type = 'button';
      button.className = 'receipt-link';
      button.textContent = `Read ${prompt.context.title || shortUrl(prompt.context.sourceUrl)} (${characters(prompt.context.characters)})`;
      button.dataset.contextRef = prompt.context.contextRef;
      button.title = 'See the captured page receipt';
      facts.append(button);
    } else {
      facts.append('No page included');
    }
    if (prompt.materialization) {
      facts.append(' · ');
      const button = document.createElement('button');
      button.type = 'button';
      button.className = 'prompt-receipt-link';
      button.dataset.promptReceiptRef = prompt.materialization.receiptRef;
      button.textContent = `What Vex received (~${numberFormat.format(prompt.materialization.materializedInputTokenEstimate)} input tokens)`;
      button.title = 'Inspect the canonical prompt-materialization receipt for this turn';
      facts.append(button);
    }
  }
  if (usage?.prompt_tokens) facts.append(`, ${tokens(usage.prompt_tokens)} in, ${numberFormat.format(usage.completion_tokens ?? 0)} out`);
  return facts.childNodes.length > 0 ? facts : null;
}

function vexMessage({ index, key, text, reasoning, thinking, prompt, outcome, failure, usage }) {
  const node = messageNode({ who: 'vex', index, text: text || ' ', prose: true });
  const thought = thoughtNode({ key, reasoning, thinking });
  if (thought) node.prepend(thought);
  const receipt = thinking ? null : receiptNode({ prompt, outcome, failure, usage });
  if (receipt) node.append(receipt);
  if (!text && thinking) node.classList.add('waiting');
  return node;
}

function renderTimeline() {
  if (!ui) return;
  const timeline = vex.el('ConversationTimeline');
  const nodes = [];
  let index = 0;

  for (const record of model.transcript) {
    nodes.push(messageNode({ who: 'you', index: index++, text: record.userText }));
    nodes.push(vexMessage({
      index: index++,
      key: record.turnRef,
      text: record.response.text || (record.outcome === 'Completed' ? '' : '…'),
      reasoning: record.response.reasoning,
      thinking: false,
      prompt: record.prompt,
      outcome: record.outcome,
      failure: record.failure,
      usage: record.response.usage
    }));
  }

  if (model.live) {
    nodes.push(messageNode({ who: 'you', index: index++, text: model.live.userText }));
    const answering = model.live.text.length > 0;
    const live = vexMessage({
      index: index++,
      key: `turn-${model.live.turn}`,
      text: model.live.text,
      reasoning: model.live.reasoning || null,
      thinking: !answering,
      prompt: model.live.prompt
    });
    live.classList.add('live');
    nodes.push(live);
  }

  if (nodes.length === 0) {
    const empty = document.createElement('li');
    empty.className = 'timeline-empty';
    const heading = document.createElement('p');
    heading.className = 'timeline-empty-heading';
    heading.textContent = 'Talk with Vex about what you have open.';
    const hint = document.createElement('p');
    hint.textContent = model.session?.browserContextBridge === true
      ? 'Include the page to supply its admitted text to Vex. Enter sends; Shift+Enter starts a new line.'
      : 'You can capture a page receipt now; canonical prompt handoff is not connected yet. Enter sends without page context.';
    empty.append(heading, hint);
    nodes.push(empty);
  }

  const nearBottom = timeline.scrollHeight - timeline.scrollTop - timeline.clientHeight < 80;
  timeline.replaceChildren(...nodes);
  if (nearBottom || model.live) timeline.scrollTop = timeline.scrollHeight;
}

function setMeter({ meter, segments, text, label, known }) {
  meter.dataset.known = String(known);
  for (const [node, share] of segments) node.style.width = `${Math.max(0, Math.min(1, share)) * 100}%`;
  meter.querySelector('.meter-text').textContent = text;
  meter.setAttribute('aria-label', label);
}

function renderSettings() {
  if (!ui) return;
  const last = [...model.transcript].reverse().find((record) =>
    record.outcome === 'Completed' && record.prompt?.materialization);
  if (last) {
    const receipt = last.prompt.materialization;
    const input = receipt.materializedInputTokenEstimate;
    const reserved = receipt.reservedOutputTokens;
    const hard = receipt.hardTokenLimit;
    const remaining = Math.max(0, hard - input - reserved);
    setMeter({
      meter: vex.el('ContextWindow'),
      segments: [[ui.contextCore, hard ? input / hard : 0], [ui.contextUsed, hard ? reserved / hard : 0]],
      text: `~${tokens(input)} input + ${tokens(reserved)} reserved / ${tokens(hard)} limit`,
      label: `Prompt context: about ${tokens(input)} materialized input, ${tokens(reserved)} reserved output, ${tokens(remaining)} remaining at admission`,
      known: true
    });
    const priorCount = receipt.selectedConversationEventRefs.length;
    const memorySummary = receipt.memoryContextIncluded
      ? `Memory contributed about ${tokens(receipt.memoryEstimatedSystemMessageTokens)}.`
      : 'No Memory packet was included.';
    ui.contextNote.textContent = `Canonical receipt for the last turn: ${memorySummary} ${priorCount} prior Conversation event${priorCount === 1 ? '' : 's'} selected. Whole-history enumeration: no. Hidden reasoning included: no.`;
  } else {
    const windowTokens = model.resources?.contextWindow?.tokens ?? null;
    setMeter({
      meter: vex.el('ContextWindow'),
      segments: [[ui.contextCore, 0], [ui.contextUsed, 0]],
      text: windowTokens ? `${tokens(windowTokens)} model window known · no turn receipt yet` : 'No turn receipt yet',
      label: windowTokens ? `Model window ${tokens(windowTokens)}; no canonical prompt receipt retained in this presentation session` : 'No canonical prompt receipt retained in this presentation session',
      known: Boolean(windowTokens)
    });
    ui.contextNote.textContent = model.transcript.length
      ? 'Older/reloaded canonical Conversation turns do not carry their prompt receipts in the Conversation projection. Send a new turn here to inspect exactly what the canonical Companion admits for that turn.'
      : 'Send a turn to inspect the canonical prompt-materialization receipt that VexLife returns for that exact turn.';
  }

  const memory = model.resources?.memory;
  if (memory?.totalBytes) {
    const usedBytes = memory.totalBytes - memory.availableBytes;
    const share = memory.availableBytes / memory.totalBytes;
    setMeter({
      meter: vex.el('MemoryResource'),
      segments: [[ui.memoryUsed, usedBytes / memory.totalBytes]],
      text: `${gigabytes(usedBytes)} used of ${gigabytes(memory.totalBytes)}`,
      label: `Mac memory: ${gigabytes(usedBytes)} used of ${gigabytes(memory.totalBytes)}, ${Math.round(share * 100)}% left`,
      known: true
    });
    const runtime = model.resources.runtime?.residentBytes ? ` The model runtime holds ${gigabytes(model.resources.runtime.residentBytes)}.` : '';
    ui.memoryNote.textContent = `${Math.round(share * 100)}% left on this Mac.${runtime}`;
  } else {
    setMeter({ meter: vex.el('MemoryResource'), segments: [[ui.memoryUsed, 0]], text: 'Not measured', label: 'Mac memory not measured', known: false });
    ui.memoryNote.textContent = '';
  }

  vex.el('ThoughtVisibility').setAttribute('aria-checked', String(model.preferences.thoughtsVisible));
  vex.el('LowMemoryAlert').setAttribute('aria-checked', String(model.preferences.lowMemoryAlert));
  ui.alertThreshold.textContent = `${Math.round(LOW_MEMORY_SHARE * 100)}% left`;
}

function describeRenderer() {
  const source = vex.sendSourceBinding;
  const sendNote = source?.type === 'Unbound'
    ? `Accepted VexWeb source still marks Send as not yet implemented (${source.fallback.capabilityRef}); this exploration realizes it through the Conversation turn Family. `
    : '';
  ui.settingRenderer.textContent = `${sendNote}Hand-built while the VexWeb compiler is held. ${vex.coverage.bound} of ${vex.coverage.declared} Blueprint elements are bound by their Kotlin handles.`;
}

function renderQuickActions() {
  const list = vex.el('QuickActions');
  list.replaceChildren(...QUICK_ACTIONS.map((action) => {
    const item = document.createElement('li');
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'quick-action';
    button.dataset.action = action.key;
    button.dataset.bound = String(Boolean(action.bound));
    button.setAttribute('aria-label', action.bound ? action.label : `${action.label} (not connected yet)`);
    button.title = action.bound ? action.label : `${action.label}: not connected yet`;
    button.innerHTML = `<svg viewBox="0 0 24 24" aria-hidden="true">${action.icon}</svg>`;
    item.append(button);
    return item;
  }));
}

// ------------------------------------------------------------------ Snackbar, inspector, clean chat

function notify(text) {
  const bar = ui.snackbar;
  bar.textContent = text;
  bar.hidden = false;
  clearTimeout(snackbarTimer);
  snackbarTimer = setTimeout(() => { bar.hidden = true; }, 5200);
}

function showInspector({ title, facts, text }) {
  ui.inspectorTitle.textContent = title;
  ui.inspectorFacts.replaceChildren(...facts.flatMap(([term, value]) => {
    const dt = document.createElement('dt');
    dt.textContent = term;
    const dd = document.createElement('dd');
    dd.textContent = value;
    return [dt, dd];
  }));
  ui.inspectorText.textContent = text;
  vex.el('ContextInspector').showModal();
}

async function inspectContext(contextRef) {
  let record;
  try {
    record = await getJson(`/api/context/${contextRef}`);
  } catch {
    notify("That page text isn't available any more.");
    return;
  }
  showInspector({
    title: 'Captured page receipt',
    facts: [
      ['Page', record.sourceUrl],
      ['Title', record.title || 'None'],
      ['Read from', record.capture === 'Rendered' ? 'The page as it showed in your browser' : 'The page source (the page could not supply what it showed)'],
      ['Length', record.truncated ? `${characters(record.characters)} (first part of the page)` : characters(record.characters)],
      ['sha256', record.sha256],
      ['Read at', new Date(record.acquiredAt).toLocaleString()]
    ],
    text: record.text
  });
}

function inspectPromptReceipt(receiptRef) {
  const record = [...model.transcript].reverse().find((turn) => turn.prompt?.materialization?.receiptRef === receiptRef);
  if (!record) {
    notify('That prompt receipt is no longer retained in this presentation session.');
    return;
  }
  let description;
  try {
    description = describePromptContextMaterializationReceipt(record.prompt.materialization);
  } catch {
    notify("That prompt receipt isn't valid for inspection.");
    return;
  }
  showInspector({
    title: 'What Vex received for this turn',
    facts: description.facts,
    text: description.text
  });
}

function askCleanChat() {
  if (model.session?.canonicalConversation) {
    notify('This workspace projects the canonical Vex Conversation. Clean-chat/reset is not owned by this presentation yet.');
    return;
  }
  const count = model.transcript.length;
  ui.confirmText.textContent = count === 0
    ? 'This chat is already empty.'
    : `This clears the ${count} turn${count === 1 ? '' : 's'} in this workspace's conversation. They're kept in an archive file. VexLife memory isn't connected here, so nothing else is cleared.`;
  ui.confirmAccept.disabled = count === 0;
  ui.confirmDialog.showModal();
}

async function startCleanChat() {
  ui.confirmDialog.close();
  try {
    const result = await postJson('/api/conversation/archive');
    model.transcript = [];
    model.live = null;
    model.expandedThoughts.clear();
    model.turn = ConversationTurn.idleAfter(result.lastSequence);
    render();
    notify(`Started a clean chat. ${result.archivedTurns} turn${result.archivedTurns === 1 ? ' is' : 's are'} kept in ${result.archiveFile}.`);
  } catch (error) {
    notify(error.code === 'TURN_IN_FLIGHT' ? 'Wait for Vex to finish, then start a clean chat.' : "Couldn't start a clean chat.");
  }
}

// ------------------------------------------------------------------ Seam (accepted spatial source)

function applySplit() {
  const { beforeParts, afterParts } = vex.seam;
  const share = model.allocation ?? beforeParts / (beforeParts + afterParts);
  ui.frame.dataset.collapsed = model.collapsed ?? 'none';
  ui.frame.style.setProperty('--before', `${(share * 100).toFixed(2)}%`);
  ui.seam.setAttribute('aria-valuenow', String(Math.round(share * 100)));
  ui.seam.setAttribute('aria-valuetext', `Browser ${Math.round(share * 100)} percent`);
}

function wireSeam() {
  const { interactions } = vex.seam;
  const seam = ui.seam;
  ui.collapseBefore.hidden = !interactions.has('CollapseBefore');
  ui.collapseAfter.hidden = !interactions.has('CollapseAfter');

  ui.collapseBefore.addEventListener('click', () => {
    model.collapsed = model.collapsed === 'after' ? 'none' : 'before';
    applySplit();
  });
  ui.collapseAfter.addEventListener('click', () => {
    model.collapsed = model.collapsed === 'before' ? 'none' : 'after';
    applySplit();
  });

  if (!interactions.has('Resize')) return;

  const setFromClientX = (clientX) => {
    const rect = ui.frame.getBoundingClientRect();
    model.allocation = Math.min(0.85, Math.max(0.15, (clientX - rect.left) / rect.width));
    model.collapsed = 'none';
    applySplit();
  };

  seam.addEventListener('pointerdown', (event) => {
    if (event.target.closest('button')) return;
    seam.setPointerCapture(event.pointerId);
    ui.frame.classList.add('resizing');
    const move = (moveEvent) => setFromClientX(moveEvent.clientX);
    const up = () => {
      ui.frame.classList.remove('resizing');
      seam.removeEventListener('pointermove', move);
      seam.removeEventListener('pointerup', up);
      seam.removeEventListener('pointercancel', up);
    };
    seam.addEventListener('pointermove', move);
    seam.addEventListener('pointerup', up);
    seam.addEventListener('pointercancel', up);
  });

  seam.addEventListener('dblclick', () => {
    model.allocation = null;
    model.collapsed = 'none';
    applySplit();
  });

  seam.addEventListener('keydown', (event) => {
    const current = model.allocation ?? vex.seam.beforeParts / (vex.seam.beforeParts + vex.seam.afterParts);
    if (event.key === 'ArrowLeft' || event.key === 'ArrowRight') {
      event.preventDefault();
      model.allocation = Math.min(0.85, Math.max(0.15, current + (event.key === 'ArrowLeft' ? -0.05 : 0.05)));
      model.collapsed = 'none';
      applySplit();
    } else if (event.key === 'Home' || event.key === 'Enter') {
      event.preventDefault();
      model.allocation = null;
      model.collapsed = 'none';
      applySplit();
    }
  });
}

// ------------------------------------------------------------------ Human input -> Controls

function openPage(value) {
  const admission = BrowserContext.admitPageAddress(value);
  if (admission.type === 'Refused') {
    ui.addressError.textContent = ADDRESS_REFUSAL_TEXT[admission.reason];
    ui.addressError.hidden = false;
    return false;
  }
  ui.addressError.hidden = true;
  return control('browser', { type: 'Open', page: admission.page });
}

function send() {
  const composer = Composer.converge(model.companion, model.turn, model.browser);
  if (!composer.sendAllowed) return;
  if (composer.contextRef && model.session?.browserContextBridge === false) {
    notify('Vex is connected through the canonical Conversation, but page text is not connected to that prompt-context owner yet. Stop including the page to send this turn.');
    return;
  }
  if (control('turn', { type: 'Send', text: ui.draft.value, contextRef: composer.contextRef })) {
    ui.draft.value = '';
    sizeDraft();
    render();
  }
}

function sizeDraft() {
  const draft = ui.draft;
  draft.style.height = 'auto';
  draft.style.height = `${Math.min(draft.scrollHeight + 2, 220)}px`;
}

function wireInput() {
  ui.addressForm.addEventListener('submit', (event) => {