// JavaScript realization of the Kotlin semantic Families in src/commonMain/kotlin/vexlife/.
//
// Kotlin is the semantic authority. This module exists because Kotlin is not yet compiled into the
// browser. Every function here is checked against generated/family-conformance.json, which the
// Kotlin Families produce (src/jvmTest/.../FamilyConformanceGoldenTest.kt). Change the Kotlin
// first, regenerate, then make this file pass test/families.conformance.test.mjs.
//
// Shapes are exactly the generated JSON shapes: plain objects with a `type` field.
// act(...) returns null when the Control is not accepted, else { admittedState, effect }.
//
// [VXG RealForever]

const freeze = Object.freeze;

const act = (admittedState, effect = null) => freeze({ admittedState, effect });
const typed = (type) => freeze({ type });

// ------------------------------------------------------------------ CompanionRuntimeFamily

const REVISION_SEGMENT = /^[0-9a-f]{12,64}$/;

export const CompanionRuntime = freeze({
  initial: typed('Unobserved'),

  act(state, control) {
    if (control.type !== 'Check') return null;
    if (state.type === 'Checking') return null;
    return act(typed('Checking'), typed('ProbeRuntime'));
  },

  interpret(_state, observation) {
    if (observation.type === 'RuntimeReady') {
      if (isBlank(observation.modelRef) || isBlank(observation.endpointRef)) {
        return freeze({ type: 'Unavailable', reason: 'ModelIdentityMissing' });
      }
      return freeze({ type: 'Ready', modelRef: observation.modelRef, endpointRef: observation.endpointRef });
    }
    if (observation.type === 'RuntimeUnavailable') {
      return freeze({ type: 'Unavailable', reason: observation.reason });
    }
    throw new Error(`unknown CompanionRuntime observation ${observation.type}`);
  },

  display(state) {
    switch (state.type) {
      case 'Unobserved': return typed('NotChecked');
      case 'Checking': return typed('Checking');
      case 'Ready': return freeze({ type: 'Ready', modelLabel: CompanionRuntime.modelLabel(state.modelRef) });
      case 'Unavailable': return freeze({ type: 'Unavailable', reason: state.reason });
      default: throw new Error(`unknown CompanionRuntime state ${state.type}`);
    }
  },

  modelLabel(modelRef) {
    const segments = modelRef.split(/[/\\]/).filter((segment) => !isBlank(segment));
    if (segments.length === 0) return modelRef;
    const last = segments[segments.length - 1];
    if (segments.length >= 2 && REVISION_SEGMENT.test(last)) return segments[segments.length - 2];
    return last;
  }
});

// ------------------------------------------------------------------ ConversationTurnFamily

function activeTurn(state) {
  switch (state.type) {
    case 'Requested':
    case 'Responding':
    case 'Stopping':
      return state.turn;
    default:
      return null;
  }
}

function lastSequence(state) {
  return state.type === 'Idle' ? state.lastSequence : state.turn;
}

export const ConversationTurn = freeze({
  initial: freeze({ type: 'Idle', lastSequence: 0 }),

  /** Initial State from the conversation Store's observed last sequence. */
  idleAfter(sequence) {
    if (!Number.isInteger(sequence) || sequence < 0) throw new Error('lastSequence must be a non-negative integer');
    return freeze({ type: 'Idle', lastSequence: sequence });
  },

  activeTurn,

  turnRef(sequence) {
    return `turn-${sequence}`;
  },

  act(state, control) {
    if (control.type === 'Send') {
      const text = kotlinTrim(control.text);
      const trimmedRef = control.contextRef == null ? null : kotlinTrim(control.contextRef);
      const contextRef = trimmedRef && trimmedRef.length > 0 ? trimmedRef : null;
      if (text.length === 0) return null;
      if (activeTurn(state) !== null) return null;
      const turn = lastSequence(state) + 1;
      return act(
        freeze({ type: 'Requested', turn }),
        freeze({ type: 'RequestTurn', turn, text, contextRef })
      );
    }
    if (control.type === 'Stop') {
      if (state.type === 'Requested' || state.type === 'Responding') {
        return act(
          freeze({ type: 'Stopping', turn: state.turn }),
          freeze({ type: 'CancelTurn', turn: state.turn })
        );
      }
      return null;
    }
    throw new Error(`unknown ConversationTurn control ${control.type}`);
  },

  interpret(state, observation) {
    const active = activeTurn(state);
    if (active === null) return state;
    if (observation.turn !== active) return state;
    switch (observation.type) {
      case 'ResponseStarted':
        return state.type === 'Requested' ? freeze({ type: 'Responding', turn: active }) : state;
      case 'ResponseCompleted':
      case 'ResponseCancelled':
        return freeze({ type: 'Idle', lastSequence: active });
      case 'ResponseFailed':
        if (state.type === 'Stopping') return freeze({ type: 'Idle', lastSequence: active });
        return freeze({ type: 'Failed', turn: active, reason: observation.reason });
      default:
        throw new Error(`unknown ConversationTurn observation ${observation.type}`);
    }
  },

  display(state) {
    switch (state.type) {
      case 'Idle': return typed('Quiet');
      case 'Requested': return typed('WaitingForVex');
      case 'Responding': return typed('VexResponding');
      case 'Stopping': return typed('Stopping');
      case 'Failed': return freeze({ type: 'Failed', reason: state.reason });
      default: throw new Error(`unknown ConversationTurn state ${state.type}`);
    }
  }
});

// ------------------------------------------------------------------ BrowserContextFamily

const MAX_ADDRESS_LENGTH = 2048;
const MAX_HOST_LENGTH = 200;
const MAX_LABEL_LENGTH = 63;
const NON_WEB_SCHEME = /^[A-Za-z][A-Za-z0-9+.-]*:[^0-9]/u;
const HOST_SHAPE = /^[a-z0-9_-]+(\.[a-z0-9_-]+)*$/u;
const NUMERIC_LABEL = /^(0x[0-9a-f]*|[0-9]+)$/u;
const DECIMAL_OCTET = /^(0|[1-9][0-9]{0,2})$/u;
const LOCAL_SUFFIXES = freeze(['.localhost', '.local', '.internal', '.home.arpa']);

function hasRefusedCharacter(value) {
  for (const char of value) {
    const code = char.codePointAt(0);
    if (code < 0x20 || code === 0x7f || /\s/u.test(char)) return true;
  }
  return false;
}

const refused = (reason) => freeze({ type: 'Refused', reason });

function admitPageAddress(value) {
  const trimmed = kotlinTrim(String(value ?? ''));
  if (trimmed.length === 0) return refused('Empty');
  if (trimmed.length > MAX_ADDRESS_LENGTH) return refused('TooLong');
  if (hasRefusedCharacter(trimmed)) return refused('Malformed');

  const schemeEnd = trimmed.indexOf('://');
  if (schemeEnd < 0 && NON_WEB_SCHEME.test(trimmed)) return refused('UnsupportedScheme');
  const scheme = schemeEnd >= 0 ? trimmed.slice(0, schemeEnd).toLowerCase() : 'https';
  if (scheme !== 'http' && scheme !== 'https') return refused('UnsupportedScheme');

  const remainder = schemeEnd >= 0 ? trimmed.slice(schemeEnd + 3) : trimmed;
  const found = remainder.search(/[/?#]/u);
  const authorityEnd = found < 0 ? remainder.length : found;
  const authority = remainder.slice(0, authorityEnd);
  const rest = remainder.slice(authorityEnd);

  if (authority.includes('@')) return refused('CredentialsInAddress');
  if (authority.startsWith('[') || /[^\u0000-\u007f]/u.test(authority)) return refused('UnsupportedHost');

  const colon = authority.lastIndexOf(':');
  let host = (colon >= 0 ? authority.slice(0, colon) : authority).toLowerCase();
  if (host.endsWith('.')) host = host.slice(0, -1);
  const portText = colon >= 0 ? authority.slice(colon + 1) : '';

  if (host.length === 0) return refused('Malformed');
  if (host.length > MAX_HOST_LENGTH) return refused('TooLong');
  if (!HOST_SHAPE.test(host) || host.split('.').some((label) => label.length > MAX_LABEL_LENGTH)) return refused('Malformed');

  let port = null;
  if (portText.length > 0) {
    if (portText.length > 5 || !/^[0-9]+$/u.test(portText)) return refused('Malformed');
    port = Number(portText);
    if (port < 1 || port > 65535) return refused('Malformed');
  }

  const hostReason = hostRefusal(host);
  if (hostReason) return refused(hostReason);

  const defaultPort = scheme === 'https' ? 443 : 80;
  const portSuffix = port === null || port === defaultPort ? '' : `:${port}`;
  const path = rest.length === 0 || rest.startsWith('?') || rest.startsWith('#') ? `/${rest}` : rest;
  return freeze({ type: 'Admitted', page: `${scheme}://${host}${portSuffix}${path}` });
}

function hostRefusal(host) {
  if (host === 'localhost' || LOCAL_SUFFIXES.some((suffix) => host.endsWith(suffix))) return 'LocalNetwork';
  const labels = host.split('.');
  if (!NUMERIC_LABEL.test(labels[labels.length - 1])) return labels.length < 2 ? 'Malformed' : null;
  if (labels.length !== 4 || labels.some((label) => !DECIMAL_OCTET.test(label) || Number(label) > 255)) return 'Malformed';
  const [a, b, c] = labels.map(Number);
  const local = a === 0 || a === 10 || a === 127 || a >= 224 ||
    (a === 169 && b === 254) || (a === 172 && b >= 16 && b <= 31) || (a === 192 && b === 168) ||
    (a === 100 && b >= 64 && b <= 127) || (a === 192 && b === 0 && c === 0) || (a === 198 && b >= 18 && b <= 19);
  return local ? 'LocalNetwork' : null;
}

/** The frame no longer shows a reported page, so a receipt names something else. */
function frameDiverged(frame) {
  return frame.type === 'Unlocated' || frame.type === 'Refused';
}

export const BrowserContext = freeze({
  initial: typed('Unaddressed'),

  admitPageAddress,

  parsePageAddress(value) {
    const admission = admitPageAddress(value);
    return admission.type === 'Admitted' ? admission.page : null;
  },

  act(state, control) {
    switch (control.type) {
      case 'Open':
        return openPage(state, control.page);
      case 'Share':
        if (state.type !== 'Addressed') return null;
        if (state.context.type === 'NotShared' || state.context.type === 'Unavailable') {
          if (state.frame.type === 'Loading') return act(freeze({ ...state, context: typed('AwaitingPage') }));
          return act(
            freeze({ ...state, context: typed('Acquiring') }),
            freeze({ type: 'AcquireContext', page: state.page })
          );
        }
        return null;
      case 'StopSharing':
        if (state.type !== 'Addressed') return null;
        if (state.context.type === 'NotShared') return null;
        return act(freeze({ ...state, context: typed('NotShared') }));
      default:
        throw new Error(`unknown BrowserContext control ${control.type}`);
    }
  },

  /** Returns { admittedState, effect }: an observation may make one Effect necessary. */
  interpret(state, observation) {
    if (state.type !== 'Addressed') return act(state);
    switch (observation.type) {
      case 'Located':
        return settle(state, observation.page, freeze({ type: 'Showing', title: observation.title }));
      case 'FrameUnlocated':
        return settle(state, state.page, typed('Unlocated'));
      case 'FrameRefused':
        return settle(state, observation.page, freeze({ type: 'Refused', reason: observation.reason }));
      case 'ContextAcquired':
        if (observation.page === state.page && state.context.type === 'Acquiring') {
          return act(freeze({ ...state, context: freeze({ type: 'Shared', receipt: observation.receipt }) }));
        }
        return act(state);
      case 'ContextFailed':
        if (observation.page === state.page && state.context.type === 'Acquiring') {
          return act(freeze({ ...state, context: freeze({ type: 'Unavailable', reason: observation.reason }) }));
        }
        return act(state);
      default:
        throw new Error(`unknown BrowserContext observation ${observation.type}`);
    }
  },

  display(state) {
    if (state.type === 'Unaddressed') {
      return freeze({ pageUrl: null, frame: typed('NoPage'), context: typed('NotShared') });
    }
    let context;
    switch (state.context.type) {
      case 'NotShared': context = typed('NotShared'); break;
      case 'AwaitingPage':
      case 'Acquiring': context = typed('Preparing'); break;
      case 'Shared':
        context = freeze({ type: 'Shared', receipt: state.context.receipt, frameDiverged: frameDiverged(state.frame) });
        break;
      case 'Unavailable': context = freeze({ type: 'Unavailable', reason: state.context.reason }); break;
      default: throw new Error(`unknown context state ${state.context.type}`);
    }
    return freeze({ pageUrl: state.page, frame: state.frame, context });
  }
});

function openPage(state, page) {
  let context = typed('NotShared');
  if (state.type === 'Addressed') {
    switch (state.context.type) {
      case 'NotShared': break;
      case 'AwaitingPage':
      case 'Acquiring':
      case 'Shared':
        context = state.page === page ? state.context : typed('AwaitingPage');
        break;
      case 'Unavailable':
        context = typed('AwaitingPage');
        break;
      default:
        throw new Error(`unknown context state ${state.context.type}`);
    }
  }
  return act(
    freeze({ type: 'Addressed', page, frame: typed('Loading'), context }),
    freeze({ type: 'ShowPage', page })
  );
}

/** The frame settled on `page`. The page follows it; sharing asks for that page's receipt. */
function settle(state, page, frame) {
  const pageChanged = page !== state.page;
  const acquire = state.context.type === 'AwaitingPage' || (state.context.type !== 'NotShared' && pageChanged);
  return act(
    freeze({ type: 'Addressed', page, frame, context: acquire ? typed('Acquiring') : state.context }),
    acquire ? freeze({ type: 'AcquireContext', page }) : null
  );
}

// ------------------------------------------------------------------ ComposerConvergence

export const Composer = freeze({
  converge(companion, turn, browser) {
    const addressed = browser.type === 'Addressed' ? browser : null;
    const attachment = addressed && addressed.context.type === 'Shared'
      ? freeze({ type: 'Page', receipt: addressed.context.receipt, frameDiverged: frameDiverged(addressed.frame) })
      : typed('None');

    let hold = null;
    if (activeTurn(turn) !== null) hold = 'TurnInProgress';
    else if (companion.type === 'Unobserved') hold = 'CompanionNotChecked';
    else if (companion.type === 'Checking') hold = 'CompanionChecking';
    else if (companion.type === 'Unavailable') hold = 'CompanionUnavailable';
    else if (addressed && (addressed.context.type === 'AwaitingPage' || addressed.context.type === 'Acquiring')) hold = 'ContextPreparing';

    return freeze({
      sendAllowed: hold === null,
      stopAvailable: turn.type === 'Requested' || turn.type === 'Responding',
      hold,
      attachment,
      contextRef: attachment.type === 'Page' ? attachment.receipt.contextRef : null
    });
  }
});

// ------------------------------------------------------------------ shared

function isBlank(value) {
  return typeof value !== 'string' || kotlinTrim(value).length === 0;
}

// Kotlin's String.trim() removes leading/trailing chars for which Char.isWhitespace() is true.
function kotlinTrim(value) {
  return value.replace(/^[\s\u001c-\u001f]+|[\s\u001c-\u001f]+$/gu, '');
}
