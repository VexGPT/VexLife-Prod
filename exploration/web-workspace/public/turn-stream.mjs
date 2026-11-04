// Client for the workspace's turn stream (server-sent events over a POST response).
// Shared by the browser workspace and tools/circuit-proof.mjs so both realize RequestTurn the same way.
// [VXG RealForever]

/**
 * POST one RequestTurn effect and deliver each event as onEvent(name, data).
 * Resolves when the stream ends. Rejects only if the request could not be made at all.
 */
export async function requestTurnStream({ origin = '', effect, onEvent, signal, fetchImpl = fetch }) {
  const response = await fetchImpl(`${origin}/api/conversation/turns`, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ turn: effect.turn, text: effect.text, contextRef: effect.contextRef }),
    signal
  });

  if (!response.ok || !response.body) {
    let detail = null;
    try { detail = await response.json(); } catch { /* not JSON */ }
    return { accepted: false, status: response.status, detail };
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';
  let eventName = 'message';
  let dataLines = [];

  const dispatch = () => {
    if (dataLines.length === 0) return;
    const data = JSON.parse(dataLines.join('\n'));
    onEvent(eventName, data);
    eventName = 'message';
    dataLines = [];
  };

  for (;;) {
    const { done, value } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });
    let newline;
    while ((newline = buffer.indexOf('\n')) >= 0) {
      const line = buffer.slice(0, newline).replace(/\r$/u, '');
      buffer = buffer.slice(newline + 1);
      if (line === '') dispatch();
      else if (line.startsWith('event:')) eventName = line.slice(6).trim();
      else if (line.startsWith('data:')) dataLines.push(line.slice(5).trimStart());
    }
  }
  dispatch();
  return { accepted: true, status: response.status, detail: null };
}

export async function postJson(path, body = {}, { origin = '', fetchImpl = fetch, signal } = {}) {
  const response = await fetchImpl(`${origin}${path}`, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(body),
    signal
  });
  const value = await response.json();
  if (!response.ok) {
    const error = new Error(value?.message || `${path} answered ${response.status}`);
    error.status = response.status;
    error.code = value?.error;
    throw error;
  }
  return value;
}

export async function getJson(path, { origin = '', fetchImpl = fetch } = {}) {
  const response = await fetchImpl(`${origin}${path}`);
  const value = await response.json();
  if (!response.ok) {
    const error = new Error(value?.message || `${path} answered ${response.status}`);
    error.status = response.status;
    throw error;
  }
  return value;
}
