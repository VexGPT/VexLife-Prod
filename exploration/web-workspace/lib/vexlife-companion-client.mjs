// Bounded adapter from the VexLife-Prod exploration shell to the current VexLife Browser Companion.
//
// The upstream owns Conversation, prompt/context continuity, capability composition, model/runtime
// invocation, and durable message truth. This module contributes transport + projection mechanics only.
// It never calls the raw Heretic endpoint.
//
// [VXG RealForever]

export const DEFAULT_VEXLIFE_COMPANION_ORIGIN = 'http://127.0.0.1:18110';
export const DIRECT_COMPANION = Object.freeze({
  projectRef: 'project.self-development',
  threadRef: 'thread.self-development.open-conversation',
  channelRef: 'channel.self-development.companion'
});

const AVAILABILITY_PATH = '/api/v1/companion/availability';
const CURRENTNESS_PATH = '/api/v1/companion/conversation-currentness';
const CONVERSATION_PATH = '/api/v1/companion/conversation';
const STREAM_PATH = '/api/v1/companion/turn/stream';
const FRAME_SCHEMA = 'vexlife.browser-companion-turn-stream-frame/v1';

function exactLoopbackOrigin(value) {
  const parsed = new URL(value);
  if (parsed.protocol !== 'http:' || parsed.hostname !== '127.0.0.1' || !parsed.port || parsed.pathname !== '/' || parsed.search || parsed.hash) {
    throw new TypeError('VexLife Companion origin must be one numeric-loopback HTTP origin');
  }
  return parsed.origin;
}

async function readJson(response, label) {
  const value = await response.json().catch(() => null);
  if (!response.ok || !value || typeof value !== 'object' || Array.isArray(value)) {
    const error = new Error(`${label} is unavailable`);
    error.status = response.status;
    error.value = value;
    throw error;
  }
  return value;
}

function query(path) {
  const params = new URLSearchParams(DIRECT_COMPANION);
  return `${path}?${params.toString()}`;
}

function unavailableReason(availability) {
  if (availability?.availabilityState === 'STARTING') return 'RuntimeUnhealthy';
  if (availability?.reasonCode === 'COMPANION_NOT_BOUND') return 'RuntimeUnhealthy';
  if (availability?.reasonCode === 'HOME_UNAVAILABLE') return 'RuntimeUnhealthy';
  if (availability?.reasonCode === 'RUNTIME_STATE_UNKNOWN') return 'RuntimeUnhealthy';
  return 'RuntimeUnhealthy';
}

function pairConversation(messages) {
  const turns = [];
  let pending = null;
  for (const message of messages) {
    if (message?.speakerRole === 'VICTOR') {
      pending = message;
      continue;
    }
    if (message?.speakerRole !== 'COMPANION' || pending === null) continue;
    const sequence = turns.length + 1;
    turns.push(Object.freeze({
      schemaVersion: 'vexlife-prod.companion-projection.turn/v1',
      sequence,
      turnRef: `turn-${sequence}`,
      requestedAt: pending.createdAt,
      completedAt: message.createdAt,
      userText: pending.content,
      prompt: Object.freeze({
        contextPolicy: 'CANONICAL_VEXLIFE_CONVERSATION',
        context: null,
        canonicalRequestMessageRef: pending.messageRef,
        canonicalResponseMessageRef: message.messageRef
      }),
      model: Object.freeze({ source: 'CURRENT_VEXLIFE_COMPANION' }),
      response: Object.freeze({
        text: message.content,
        reasoningCharacters: 0,
        reasoning: null,
        finishReason: null,
        usage: null
      }),
      outcome: 'Completed',
      failure: null,
      canonical: Object.freeze({
        requestMessageRef: pending.messageRef,
        responseMessageRef: message.messageRef,
        responseEventSha256: message.eventSha256
      })
    }));
    pending = null;
  }
  return Object.freeze({ turns: Object.freeze(turns), pendingHumanMessage: pending });
}

export function createVexLifeCompanionClient({
  origin = DEFAULT_VEXLIFE_COMPANION_ORIGIN,
  fetchImpl = fetch
} = {}) {
  const base = exactLoopbackOrigin(origin);
  if (typeof fetchImpl !== 'function') throw new TypeError('VexLife Companion client requires fetch');

  const fetchJson = async (path, label) => readJson(await fetchImpl(`${base}${path}`, { cache: 'no-store' }), label);

  async function availability() {
    const value = await fetchJson(AVAILABILITY_PATH, 'VexLife Companion availability');
    if (value.schemaVersion !== 'vexlife.companion-availability/v1' || value.truthClass !== 'SOURCE_BOUND_COMPANION_AVAILABILITY') {
      throw new Error('VexLife Companion availability contract is not current');
    }
    if (value.availabilityState === 'READY' && typeof value.modelRefOrNull === 'string' && value.modelRefOrNull) {
      return Object.freeze({
        type: 'RuntimeReady',
        modelRef: value.modelRefOrNull,
        endpointRef: value.runtimeAdapterRef || value.bindingRef,
        availability: value
      });
    }
    return Object.freeze({ type: 'RuntimeUnavailable', reason: unavailableReason(value), availability: value });
  }

  async function currentness() {
    const value = await fetchJson(query(CURRENTNESS_PATH), 'canonical Conversation currentness');
    if (value.schemaVersion !== 'vexlife.browser-companion-conversation-currentness/v1') {
      throw new Error('canonical Conversation currentness contract is not current');
    }
    return value;
  }

  async function conversation() {
    const value = await fetchJson(query(CONVERSATION_PATH), 'canonical Conversation projection');
    if (value.schemaVersion !== 'vexlife.browser-companion-conversation-projection/v1') {
      throw new Error('canonical Conversation projection contract is not current');
    }
    const paired = pairConversation(Array.isArray(value.messages) ? value.messages : []);
    return Object.freeze({
      schemaVersion: 'vexlife-prod.companion-projection/v1',
      canonicalConversation: true,
      canonicalIdentity: DIRECT_COMPANION,
      canonicalConversationHeadSha256: value.canonicalConversationHeadSha256 ?? null,
      turns: paired.turns,
      pendingHumanMessage: paired.pendingHumanMessage,
      lastSequence: paired.turns.length,
      sourceProjection: value
    });
  }

  async function streamTurn({
    content,
    idempotencyKey,
    messageCreatedAt,
    originalLanguage = 'en',
    screenRef = 'screen.vexlife-workspace',
    selectedNodeRef = 'element.vexlife-workspace.companion.conversation.composer',
    signal,
    onFrame = () => {}
  }) {
    const response = await fetchImpl(`${base}${STREAM_PATH}`, {
      method: 'POST',
      headers: { 'content-type': 'application/json', accept: 'application/x-ndjson' },
      cache: 'no-store',
      body: JSON.stringify({
        ...DIRECT_COMPANION,
        content,
        idempotencyKey,
        messageCreatedAt,
        originalLanguage,
        screenRef,
        selectedNodeRef
      }),
      signal
    });
    if (!response.ok || !response.body) {
      const value = await response.json().catch(() => null);
      const error = new Error(value?.message || `VexLife Companion turn answered ${response.status}`);
      error.status = response.status;
      error.value = value;
      throw error;
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';
    let expectedSequence = 1;
    let terminal = null;

    const consume = (line) => {
      if (!line.trim()) return;
      const frame = JSON.parse(line);
      if (frame?.schemaVersion !== FRAME_SCHEMA || frame.sequence !== expectedSequence) {
        throw new Error('VexLife Companion stream frame contract is invalid');
      }
      expectedSequence += 1;
      if (!['PROGRESS', 'REASONING', 'DELTA', 'FINAL', 'FAILURE'].includes(frame.frameType)) {
        throw new Error('VexLife Companion stream frame type is invalid');
      }
      if (frame.frameType === 'FINAL' || frame.frameType === 'FAILURE') terminal = frame;
      onFrame(frame);
    };

    for (;;) {
      const { done, value } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });
      let newline;
      while ((newline = buffer.indexOf('\n')) >= 0) {
        const line = buffer.slice(0, newline).replace(/\r$/u, '');
        buffer = buffer.slice(newline + 1);
        consume(line);
      }
    }
    if (buffer.trim()) consume(buffer.trim());
    if (!terminal) throw new Error('VexLife Companion stream ended without a terminal frame');
    return terminal;
  }

  return Object.freeze({ origin: base, availability, currentness, conversation, streamTurn });
}
