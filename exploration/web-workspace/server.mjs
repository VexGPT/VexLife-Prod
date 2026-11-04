#!/usr/bin/env node
// VexLife web workspace exploration: local presentation server.
//
// Serves the hand-built workspace and projects the current canonical VexLife Companion:
//   ProbeRuntime    -> project current Companion availability
//   ShowPage        -> frame public pages on their own local origins
//   AcquireContext  -> form an exploration browser-context receipt (not yet prompt context)
//   RequestTurn     -> delegate one canonical Conversation turn to the current Companion
//
// Conversation, prompt/Memory composition, model/runtime ownership, and durable message truth remain
// upstream in VexLife. This server never calls the raw model endpoint and never writes a Vex Home.
// Two listeners are loopback-only: the workspace and its frame host.
//
// [VXG RealForever]
import crypto from 'node:crypto';
import fs from 'node:fs';
import http from 'node:http';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

import { acquirePageContext, formRenderedContext } from './lib/context-source.mjs';
import { createContextStore } from './lib/context-store.mjs';
import { createFrameHost } from './lib/frame-host.mjs';
import { createVexLifeCompanionClient, DEFAULT_VEXLIFE_COMPANION_ORIGIN } from './lib/vexlife-companion-client.mjs';
import { observeHostResources } from './lib/host-resources.mjs';
import { createUpstream } from './lib/upstream.mjs';
import { TOKEN_SHAPE } from './public/frame-address.mjs';
import { projectPromptContextMaterializationReceipt } from './public/prompt-context-receipt.mjs';

const HERE = path.dirname(fileURLToPath(import.meta.url));
const REPOSITORY_ROOT = path.resolve(HERE, '..', '..');
const PUBLIC_DIR = path.join(HERE, 'public');
const GENERATED_DIR = path.join(HERE, 'generated');
export const DEFAULT_PORT = 18210;
export const DEFAULT_FRAME_PORT = 18211;
export const DEFAULT_DATA_DIR = path.join(REPOSITORY_ROOT, 'build', 'vex-exploration', 'web-workspace');
const MAX_BODY_BYTES = 64 * 1024;
const MAX_RENDERED_BODY_BYTES = 1024 * 1024;
const MAX_TURN_TEXT = 20000;

const CONTENT_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.mjs': 'text/javascript; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon'
};

function contentSecurityPolicy(frameSource) {
  return [
    "default-src 'self'",
    "script-src 'self'",
    "style-src 'self'",
    "img-src 'self' data:",
    "connect-src 'self'",
    // Vex's browser frames the frame host; a page that leaves it may still show as itself.
    `frame-src ${frameSource} https:`,
    "base-uri 'none'",
    "form-action 'none'",
    "frame-ancestors 'none'"
  ].join('; ');
}

/** The frame host's name token: random once per data directory, so framed sites keep their storage. */
function frameToken(dataDir) {
  const file = path.join(dataDir, 'frame-host-token');
  try {
    const existing = fs.readFileSync(file, 'utf8').trim();
    if (TOKEN_SHAPE.test(existing)) return existing;
  } catch { /* formed below */ }
  const alphabet = 'abcdefghijklmnopqrstuvwxyz0123456789';
  const token = Array.from(crypto.randomBytes(12), (byte) => alphabet[byte % alphabet.length]).join('');
  fs.mkdirSync(dataDir, { recursive: true });
  fs.writeFileSync(file, `${token}\n`, { mode: 0o600 });
  return token;
}

class HttpError extends Error {
  constructor(status, code, message) {
    super(message);
    this.status = status;
    this.code = code;
  }
}

function sendJson(res, status, value) {
  const body = `${JSON.stringify(value)}\n`;
  res.writeHead(status, {
    'content-type': 'application/json; charset=utf-8',
    'cache-control': 'no-store',
    'x-content-type-options': 'nosniff'
  });
  res.end(body);
}

async function readJsonBody(req, maxBytes = MAX_BODY_BYTES) {
  const type = req.headers['content-type'] || '';
  if (!/^application\/json\b/iu.test(type)) throw new HttpError(415, 'JSON_REQUIRED', 'request body must be application/json');
  const chunks = [];
  let total = 0;
  for await (const chunk of req) {
    total += chunk.length;
    if (total > maxBytes) throw new HttpError(413, 'BODY_TOO_LARGE', 'request body is too large');
    chunks.push(chunk);
  }
  const text = Buffer.concat(chunks).toString('utf8');
  if (text.trim().length === 0) return {};
  try {
    return JSON.parse(text);
  } catch {
    throw new HttpError(400, 'JSON_INVALID', 'request body is not valid JSON');
  }
}

function serveStatic(res, pathname, csp) {
  const fromGenerated = pathname.startsWith('/generated/');
  const root = fromGenerated ? GENERATED_DIR : PUBLIC_DIR;
  const relative = fromGenerated ? pathname.slice('/generated/'.length) : pathname === '/' ? 'index.html' : pathname.slice(1);
  let decoded;
  try {
    decoded = decodeURIComponent(relative);
  } catch {
    return sendJson(res, 400, { error: 'PATH_INVALID' });
  }
  const filePath = path.resolve(root, decoded);
  if (!filePath.startsWith(`${root}${path.sep}`) || !fs.existsSync(filePath) || !fs.statSync(filePath).isFile()) {
    return sendJson(res, 404, { error: 'NOT_FOUND' });
  }
  const headers = {
    'content-type': CONTENT_TYPES[path.extname(filePath)] || 'application/octet-stream',
    'cache-control': 'no-store',
    'x-content-type-options': 'nosniff',
    'referrer-policy': 'no-referrer'
  };
  if (filePath.endsWith('.html')) headers['content-security-policy'] = csp;
  res.writeHead(200, headers);
  fs.createReadStream(filePath).pipe(res);
}

export function createWorkspaceServer({
  port = DEFAULT_PORT,
  framePort = DEFAULT_FRAME_PORT,
  dataDir = DEFAULT_DATA_DIR,
  companionOrigin = DEFAULT_VEXLIFE_COMPANION_ORIGIN,
  fetchImpl = fetch,
  upstream = createUpstream(),
  now = () => new Date(),
  log = () => {}
} = {}) {
  const contextStore = createContextStore(dataDir);
  const companion = createVexLifeCompanionClient({ origin: companionOrigin, fetchImpl });
  const token = frameToken(contextStore.dataDir);
  let inflight = null;
  let boundPort = port;
  let boundFramePort = framePort;

  const allowedHosts = () => new Set([`127.0.0.1:${boundPort}`, `localhost:${boundPort}`]);
  const allowedOrigins = () => new Set([`http://127.0.0.1:${boundPort}`, `http://localhost:${boundPort}`]);
  const frameHost = createFrameHost({ token, port: framePort, parentOrigins: () => [...allowedOrigins()], upstream, log });
  const csp = () => contentSecurityPolicy(`http://*.${token}.localhost:${boundFramePort}`);

  async function handleTurn(req, res) {
    const body = await readJsonBody(req);
    const { turn, text, contextRef } = body;
    if (!Number.isInteger(turn) || turn < 1) throw new HttpError(422, 'TURN_ID_INVALID', 'turn must be a positive integer');
    if (typeof text !== 'string' || text.trim().length === 0 || text.length > MAX_TURN_TEXT) {
      throw new HttpError(422, 'TURN_TEXT_INVALID', 'turn text must be non-empty and bounded');
    }
    if (contextRef !== null && contextRef !== undefined) {
      throw new HttpError(409, 'BROWSER_CONTEXT_BRIDGE_HELD', 'Canonical Vex Conversation is connected, but nested-browser page text is not yet admitted into its prompt-context owner');
    }

    const userText = text.trim();
    const currentness = await companion.currentness();
    const idempotencySeed = JSON.stringify({
      canonicalHead: currentness.canonicalConversationHeadSha256 ?? null,
      turn,
      userText
    });
    const idempotencyKey = `browser-send.vexlife-prod.${crypto.createHash('sha256').update(idempotencySeed).digest('hex').slice(0, 32)}`;
    const requestedAt = now();
    const controller = new AbortController();
    inflight = { turn, controller };

    res.writeHead(200, {
      'content-type': 'text/event-stream; charset=utf-8',
      'cache-control': 'no-store',
      connection: 'keep-alive',
      'x-accel-buffering': 'no'
    });
    const emit = (event, value) => {
      if (res.writableEnded || res.destroyed) return;
      res.write(`event: ${event}\ndata: ${JSON.stringify(value)}\n\n`);
    };

    emit('receipt', {
      turn,
      prompt: {
        contextPolicy: 'CANONICAL_VEXLIFE_CONVERSATION',
        context: null,
        canonicalIdentity: { projectRef: 'project.self-development', threadRef: 'thread.self-development.open-conversation', channelRef: 'channel.self-development.companion' },
        priorConversationHeadSha256: currentness.canonicalConversationHeadSha256 ?? null
      }
    });

    let responseText = '';
    let reasoningText = '';
    let started = false;
    let terminal = null;
    try {
      terminal = await companion.streamTurn({
        content: userText,
        idempotencyKey,
        messageCreatedAt: requestedAt.toISOString(),
        originalLanguage: 'en',
        signal: controller.signal,
        onFrame(frame) {
          if (frame.frameType === 'REASONING') {
            const delta = frame.payload?.reasoningDelta;
            if (typeof delta === 'string' && delta) {
              reasoningText += delta;
              emit('reasoning', { turn, text: delta });
            }
            return;
          }
          if (frame.frameType === 'DELTA') {
            const delta = frame.payload?.contentDelta;
            if (typeof delta === 'string' && delta) {
              if (!started) {
                started = true;
                emit('observation', { type: 'ResponseStarted', turn });
              }
              responseText += delta;
              emit('delta', { turn, text: delta });
            }
          }
        }
      });
    } catch (error) {
      inflight = null;
      if (controller.signal.aborted) {
        emit('observation', { type: 'ResponseFailed', turn, reason: 'StreamInterrupted' });
        return res.end();
      }
      const reason = error.status === 503 ? 'CompanionUnavailable' : 'ModelRequestFailed';
      emit('observation', { type: 'ResponseFailed', turn, reason });
      return res.end();
    } finally {
      inflight = null;
    }

    if (terminal.frameType === 'FAILURE') {
      const failureCode = terminal.payload?.failureCode ?? 'COMPANION_TURN_FAILED';
      const reason = /UNAVAILABLE|UNBOUND|HOME|RUNTIME/u.test(failureCode) ? 'CompanionUnavailable' : 'ModelRequestFailed';
      emit('observation', { type: 'ResponseFailed', turn, reason });
      return res.end();
    }

    const final = terminal.payload ?? {};
    if (!started && typeof final.content === 'string' && final.content) {
      started = true;
      responseText = final.content;
      emit('observation', { type: 'ResponseStarted', turn });
      emit('delta', { turn, text: final.content });
    }
    emit('observation', { type: 'ResponseCompleted', turn });
    emit('record', {
      schemaVersion: 'vexlife-prod.canonical-companion-turn-projection/v1',
      sequence: turn,
      turnRef: `turn-${turn}`,
      requestedAt: requestedAt.toISOString(),
      completedAt: now().toISOString(),
      userText,
      prompt: {
        contextPolicy: 'CANONICAL_VEXLIFE_CONVERSATION',
        context: null,
        canonicalRequestMessageRef: final.requestMessageRef ?? null,
        canonicalResponseMessageRef: final.responseMessageRef ?? null,
        materialization: projectPromptContextMaterializationReceipt(final.promptContextMaterialization ?? null)
      },
      model: {
        source: 'CURRENT_VEXLIFE_COMPANION',
        modelNameOrBoundedTestProfileRef: final.modelNameOrBoundedTestProfileRef ?? null
      },
      response: {
        text: responseText || final.content || '',
        reasoningCharacters: reasoningText.length,
        reasoning: reasoningText || null,
        finishReason: null,
        usage: null
      },
      outcome: 'Completed',
      failure: null,
      canonical: {
        conversationHeadSha256: final.conversationHeadSha256 ?? null,
        actualHttpCall: final.actualHttpCall === true,
        loopbackOnly: final.loopbackOnly === true
      }
    });
    res.end();
  }

  async function route(req, res) {
    const url = new URL(req.url, `http://${req.headers.host || '127.0.0.1'}`);
    const { pathname } = url;

    if (!allowedHosts().has(req.headers.host || '')) throw new HttpError(421, 'HOST_NOT_ALLOWED', 'unexpected Host header');
    if (req.method === 'POST') {
      const origin = req.headers.origin;
      if (origin !== undefined && !allowedOrigins().has(origin)) throw new HttpError(403, 'ORIGIN_NOT_ALLOWED', 'cross-origin request refused');
    }

    if (req.method === 'GET' && pathname === '/api/session') {
      return sendJson(res, 200, {
        schemaVersion: 'vexlife-prod.exploration.session/v2',
        companionOrigin,
        lastSequence: 0,
        frameHost: { token, port: boundFramePort, signedOut: true },
        dataDir: contextStore.dataDir,
        canonicalConversation: true,
        turnCancellation: false,
        browserContextBridge: false,
        vexHomeWrites: false
      });
    }
    if (req.method === 'POST' && pathname === '/api/companion/probe') {
      await readJsonBody(req);
      const observation = await companion.availability();
      return sendJson(res, 200, observation);
    }
    if (req.method === 'GET' && pathname === '/api/companion/resources') {
      return sendJson(res, 200, await observeHostResources({ endpoint: null, modelRef: null }));
    }
    if (req.method === 'POST' && pathname === '/api/conversation/archive') {
      await readJsonBody(req);
      throw new HttpError(409, 'CANONICAL_CONVERSATION_RESET_NOT_ADMITTED', 'This workspace projects the canonical VexLife Conversation and does not own its reset/archive semantics');
    }
    if (req.method === 'POST' && pathname === '/api/context/acquire') {
      const { page } = await readJsonBody(req);
      if (typeof page !== 'string') throw new HttpError(422, 'PAGE_REQUIRED', 'page must be a string');
      const { observation, record } = await acquirePageContext({ page, upstream, now });
      if (record) contextStore.saveContext(record);
      return sendJson(res, 200, observation);
    }
    if (req.method === 'POST' && pathname === '/api/context/rendered') {
      const { page, title, text } = await readJsonBody(req, MAX_RENDERED_BODY_BYTES);
      if (typeof page !== 'string') throw new HttpError(422, 'PAGE_REQUIRED', 'page must be a string');
      const { observation, record } = formRenderedContext({ page, title, text, now });
      if (record) contextStore.saveContext(record);
      return sendJson(res, 200, observation);
    }
    const contextMatch = /^\/api\/context\/(context-[0-9a-f]{16})$/u.exec(pathname);
    if (req.method === 'GET' && contextMatch) {
      const record = contextStore.loadContext(contextMatch[1]);
      return record ? sendJson(res, 200, record) : sendJson(res, 404, { error: 'CONTEXT_UNKNOWN' });
    }
    if (req.method === 'GET' && pathname === '/api/conversation') {
      return sendJson(res, 200, await companion.conversation());
    }
    if (req.method === 'POST' && pathname === '/api/conversation/turns') {
      return handleTurn(req, res);
    }
    const cancelMatch = /^\/api\/conversation\/turns\/(\d+)\/cancel$/u.exec(pathname);
    if (req.method === 'POST' && cancelMatch) {
      await readJsonBody(req);
      const turn = Number(cancelMatch[1]);
      throw new HttpError(409, 'CANONICAL_TURN_CANCELLATION_NOT_EXPOSED', `Turn ${turn} is canonical VexLife work; this presentation has no admitted cancellation effect yet`);
    }
    if (req.method === 'GET' && !pathname.startsWith('/api/')) {
      return serveStatic(res, pathname, csp());
    }
    throw new HttpError(404, 'NOT_FOUND', `${req.method} ${pathname}`);
  }

  const server = http.createServer((req, res) => {
    route(req, res).catch((error) => {
      const status = error instanceof HttpError ? error.status : 500;
      if (status === 500) log(`error ${req.method} ${req.url}: ${error.stack || error}`);
      if (!res.headersSent) sendJson(res, status, { error: error.code || 'INTERNAL_ERROR', message: error.message });
      else if (!res.writableEnded) res.end();
    });
  });

  const frameServers = [http.createServer(frameHost.handle), http.createServer(frameHost.handle)];

  const listenOn = (target, listenPort, address) => new Promise((resolve, reject) => {
    target.once('error', reject);
    target.listen(listenPort, address, () => {
      target.off('error', reject);
      resolve(target.address().port);
    });
  });

  const closeServer = (target) => new Promise((resolve) => {
    if (!target.listening) return resolve();
    target.close(() => resolve());
    target.closeAllConnections?.();
  });

  return Object.freeze({
    contextStore,
    frameToken: token,
    listen() {
      return (async () => {
        boundPort = await listenOn(server, port, '127.0.0.1');
        boundFramePort = await listenOn(frameServers[0], framePort, '127.0.0.1');
        frameHost.setPort(boundFramePort);
        // Browsers may try ::1 first for *.localhost names; answer there too when IPv6 loopback exists.
        await listenOn(frameServers[1], boundFramePort, '::1').catch(() => {});
        return { origin: `http://127.0.0.1:${boundPort}`, port: boundPort, framePort: boundFramePort, frameToken: token };
      })();
    },
    close() {
      inflight?.controller.abort();
      upstream.close?.();
      return Promise.all([closeServer(server), ...frameServers.map(closeServer)]).then(() => undefined);
    }
  });
}

function parseArgs(argv) {
  const out = {};
  for (let i = 0; i < argv.length; i += 1) {
    const key = argv[i];
    if (!key.startsWith('--')) throw new Error(`unexpected argument: ${key}`);
    out[key.slice(2)] = argv[i + 1];
    i += 1;
  }
  return out;
}

function writeStartupReceipt(filePath, value) {
  if (!filePath) return;
  const resolved = path.resolve(filePath);
  fs.mkdirSync(path.dirname(resolved), { recursive: true });
  const temporary = `${resolved}.partial-${process.pid}-${crypto.randomBytes(4).toString('hex')}`;
  fs.writeFileSync(temporary, `${JSON.stringify(value, null, 2)}\n`, { mode: 0o600 });
  fs.renameSync(temporary, resolved);
}

const invokedDirectly = process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url);
if (invokedDirectly) {
  const args = parseArgs(process.argv.slice(2));
  const workspace = createWorkspaceServer({
    port: args.port ? Number(args.port) : DEFAULT_PORT,
    framePort: args['frame-port'] ? Number(args['frame-port']) : DEFAULT_FRAME_PORT,
    dataDir: args.data ? path.resolve(args.data) : DEFAULT_DATA_DIR,
    companionOrigin: args['companion-origin'] || process.env.VEXLIFE_COMPANION_ORIGIN || DEFAULT_VEXLIFE_COMPANION_ORIGIN,
    log: (line) => process.stdout.write(`[workspace] ${new Date().toISOString()} ${line}\n`)
  });
  workspace.listen().then(({ origin, port, framePort }) => {
    writeStartupReceipt(args['ready-file'], {
      schemaVersion: 'vexlife-prod.workspace-startup/v1',
      sourceSha: process.env.VEXLIFE_PROD_SOURCE_SHA || null,
      origin,
      port,
      framePort,
      dataDir: workspace.contextStore.dataDir,
      pid: process.pid,
      formedAt: new Date().toISOString()
    });
    process.stdout.write(`[workspace] VexLife web workspace (exploration) on ${origin}\n`);
    process.stdout.write(`[workspace] Vex's browser frame host on port ${framePort} (public pages, signed out)\n`);
    process.stdout.write(`[workspace] data: ${workspace.contextStore.dataDir}\n`);
  }).catch(async (error) => {
    await workspace.close().catch(() => {});
    process.stderr.write(`[workspace] could not start: ${error.stack || error.message}\n`);
    process.exitCode = 1;
  });
  const stop = () => workspace.close().then(() => process.exit(0));
  process.on('SIGINT', stop);
  process.on('SIGTERM', stop);
}
