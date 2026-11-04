// Vex's browser: the frame host.
//
// Shows public pages to the human inside the workspace, each site on its own local origin
// (see public/frame-address.mjs), so the workspace can follow where the frame is:
//
//   - adds the location reporter as the first script of each page the frame opens,
//   - removes what forbids showing a page inside another (X-Frame-Options, CSP frame-ancestors),
//   - keeps redirects inside Vex's browser,
//   - browses signed out: cookies are neither sent nor kept,
//   - refuses this machine and the local network (checked when connecting, see upstream.mjs).
//
// It never serves the workspace itself and holds no workspace capability: frame-host origins are
// not the workspace origin, so pages here cannot call the workspace API.
//
// [VXG RealForever]
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

import { BrowserContext } from '../public/families.mjs';
import { frameUrlFor, upstreamOriginOf } from '../public/frame-address.mjs';
import { UpstreamRefusal, decodedBody, readBounded } from './upstream.mjs';

const HERE = path.dirname(fileURLToPath(import.meta.url));
const REPORTER_TEMPLATE = fs.readFileSync(path.join(HERE, 'frame-reporter.js'), 'utf8');
export const REPORTER_PATH = '/__vex-frame/reporter.js';
export const RAW_PARAMETER = '__vexframe_raw';
const MAX_HTML_BYTES = 16 * 1024 * 1024;
const MAX_REQUEST_BODY_BYTES = 8 * 1024 * 1024;

const FORWARDED_REQUEST_HEADERS = [
  'accept', 'accept-language', 'cache-control', 'content-type', 'if-match', 'if-modified-since',
  'if-none-match', 'if-range', 'pragma', 'range', 'user-agent', 'x-requested-with'
];

// Hop-by-hop, framing, cookie, and origin-scoped policy headers do not cross the frame host.
const DROPPED_RESPONSE_HEADERS = new Set([
  'connection', 'keep-alive', 'proxy-connection', 'transfer-encoding', 'te', 'trailer', 'upgrade',
  'set-cookie', 'set-cookie2', 'x-frame-options', 'content-security-policy', 'content-security-policy-report-only',
  'strict-transport-security', 'public-key-pins', 'expect-ct', 'alt-svc', 'report-to', 'nel', 'clear-site-data',
  'cross-origin-opener-policy', 'cross-origin-embedder-policy', 'cross-origin-resource-policy',
  'permissions-policy', 'feature-policy', 'service-worker-allowed', 'origin-agent-cluster'
]);

const DOCUMENT_DESTINATIONS = new Set(['document', 'iframe', 'frame']);
const WRAPPED_TYPES = /^(text\/plain|text\/markdown|text\/csv|application\/json|application\/pdf|image\/)/iu;

function escapeHtml(text) {
  return String(text).replace(/[&<>"']/gu, (char) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[char]);
}

const REFUSAL_COPY = {
  LocalNetwork: {
    heading: 'This address is on your own network',
    detail: "Vex's browser opens public web pages only, so it doesn't reach this Mac or devices on your network."
  },
  Unreachable: {
    heading: "This page couldn't be reached",
    detail: "The site didn't answer. Check the address, or try again in a moment."
  }
};

function refusalDocument(pageUrl, reason) {
  const copy = REFUSAL_COPY[reason] ?? REFUSAL_COPY.Unreachable;
  return `<!doctype html>
<html lang="en"><head><meta charset="utf-8">
<meta name="vex-frame-refusal" content="${escapeHtml(reason)}">
<meta name="vex-frame-address" content="${escapeHtml(pageUrl)}">
<script src="${REPORTER_PATH}"></script>
<title>${escapeHtml(copy.heading)}</title>
<style>
  :root { color-scheme: light dark; font: 15px/1.5 system-ui, -apple-system, sans-serif; }
  body { margin: 0; min-height: 100vh; display: grid; place-items: center; background: Canvas; color: CanvasText; }
  main { max-width: 34rem; padding: 2rem; }
  h1 { font-size: 1.25rem; margin: 0 0 .5rem; }
  p { margin: 0 0 .75rem; opacity: .8; }
  .address { font: 13px/1.4 ui-monospace, SFMono-Regular, monospace; word-break: break-all; opacity: .65; }
</style></head>
<body><main><h1>${escapeHtml(copy.heading)}</h1><p>${escapeHtml(copy.detail)}</p><p class="address">${escapeHtml(pageUrl)}</p></main></body></html>
`;
}

/** A page for a non-HTML document, so the frame can still report where it is. */
function wrapperDocument(pageUrl, rawPath) {
  const name = decodeURIComponent(new URL(pageUrl).pathname.split('/').pop() || pageUrl);
  return `<!doctype html>
<html lang="en"><head><meta charset="utf-8">
<script src="${REPORTER_PATH}"></script>
<title>${escapeHtml(name)}</title>
<style>html, body { margin: 0; height: 100%; } iframe { border: 0; width: 100%; height: 100%; display: block; }</style>
</head><body><iframe src="${escapeHtml(rawPath)}" title="${escapeHtml(name)}"></iframe></body></html>
`;
}

/** Insert the reporter before any page script; drop meta-level CSP; keep meta refreshes in the frame. */
export function injectReporter(html, { pageUrl, token, port }) {
  let text = html.toString('latin1');
  text = text.replace(/<meta\b[^>]*http-equiv\s*=\s*["']?content-security-policy[^>]*>/giu, '');
  text = text.replace(/(<meta\b[^>]*http-equiv\s*=\s*["']?refresh["']?[^>]*content\s*=\s*["']\s*\d+\s*[;,]\s*url\s*=\s*)([^"'>]+)/giu, (match, prefix, target) => {
    try {
      const framed = frameUrlFor(new URL(target.trim(), pageUrl).href, { token, port });
      return framed ? `${prefix}${framed}` : match;
    } catch {
      return match;
    }
  });
  const tag = `<script src="${REPORTER_PATH}"></script>`;
  const head = /<head\b[^>]*>/iu.exec(text);
  if (head) return Buffer.from(`${text.slice(0, head.index + head[0].length)}${tag}${text.slice(head.index + head[0].length)}`, 'latin1');
  const root = /<html\b[^>]*>/iu.exec(text);
  if (root) return Buffer.from(`${text.slice(0, root.index + root[0].length)}${tag}${text.slice(root.index + root[0].length)}`, 'latin1');
  const doctype = /^\s*<!doctype[^>]*>/iu.exec(text);
  if (doctype) return Buffer.from(`${doctype[0]}${tag}${text.slice(doctype[0].length)}`, 'latin1');
  return Buffer.from(`${tag}${text}`, 'latin1');
}

export function createFrameHost({ token, port: initialPort, parentOrigins, upstream, log = () => {} }) {
  let port = initialPort;

  function reporterSource() {
    return REPORTER_TEMPLATE
      .replace("'__VEX_TOKEN__'", JSON.stringify(token))
      .replace("'__VEX_PORT__'", JSON.stringify(String(port)))
      .replace('__VEX_PARENT_ORIGINS__;', `${JSON.stringify(parentOrigins())};`);
  }

  function plain(res, status, text) {
    res.writeHead(status, { 'content-type': 'text/plain; charset=utf-8', 'cache-control': 'no-store', 'x-content-type-options': 'nosniff' });
    res.end(`${text}\n`);
  }

  function html(res, status, body) {
    const buffer = Buffer.from(body, 'utf8');
    res.writeHead(status, {
      'content-type': 'text/html; charset=utf-8',
      'content-length': buffer.length,
      'cache-control': 'no-store',
      'x-content-type-options': 'nosniff'
    });
    res.end(buffer);
  }

  function mapUrlToFrame(value, base) {
    try {
      return frameUrlFor(new URL(value, base).href, { token, port }) ?? value;
    } catch {
      return value;
    }
  }

  function requestHeaders(req, origin) {
    const headers = {};
    for (const name of FORWARDED_REQUEST_HEADERS) {
      if (req.headers[name] !== undefined) headers[name] = req.headers[name];
    }
    headers['accept-encoding'] = 'gzip, deflate, br';
    // The page believes it is on its own site; so does the site.
    for (const name of ['referer', 'origin']) {
      const value = req.headers[name];
      if (!value) continue;
      try {
        const url = new URL(value);
        const upstreamOrigin = url.port === String(port) ? upstreamOriginOf(url.hostname, { token }) : null;
        if (upstreamOrigin) headers[name] = name === 'origin' ? upstreamOrigin : `${upstreamOrigin}${url.pathname}${url.search}`;
      } catch { /* dropped */ }
    }
    return headers;
  }

  function responseHeaders(upstreamResponse, target) {
    const headers = {};
    for (const [name, value] of Object.entries(upstreamResponse.headers)) {
      if (DROPPED_RESPONSE_HEADERS.has(name)) continue;
      headers[name] = value;
    }
    if (headers.location) headers.location = mapUrlToFrame(headers.location, target);
    const allowOrigin = headers['access-control-allow-origin'];
    if (allowOrigin && allowOrigin !== '*' && allowOrigin !== 'null') headers['access-control-allow-origin'] = mapUrlToFrame(allowOrigin, target).replace(/\/$/u, '');
    headers['referrer-policy'] = 'no-referrer-when-downgrade';
    return headers;
  }

  async function readRequestBody(req) {
    if (req.method === 'GET' || req.method === 'HEAD') return null;
    const body = await readBounded(req, MAX_REQUEST_BODY_BYTES);
    if (body === null) throw new UpstreamRefusal('Unreachable', 'request body too large');
    return body;
  }

  async function handle(req, res) {
    const hostHeader = String(req.headers.host || '');
    const separator = hostHeader.lastIndexOf(':');
    const hostname = separator > 0 ? hostHeader.slice(0, separator) : hostHeader;
    const origin = hostHeader.slice(separator + 1) === String(port) ? upstreamOriginOf(hostname, { token }) : null;
    if (!origin) return plain(res, 421, 'This is not a page in Vex’s browser.');

    const requested = new URL(req.url, 'http://frame.invalid');
    if (requested.pathname === REPORTER_PATH) {
      res.writeHead(200, { 'content-type': 'text/javascript; charset=utf-8', 'cache-control': 'no-cache', 'x-content-type-options': 'nosniff' });
      return res.end(reporterSource());
    }
    // Service workers would outlive the page and intercept the frame host; pages run without them.
    if (req.headers['service-worker'] === 'script') return plain(res, 403, 'Service workers are not available in Vex’s browser.');
    if (!['GET', 'HEAD', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS'].includes(req.method)) return plain(res, 405, 'Method not available.');

    const raw = requested.searchParams.has(RAW_PARAMETER);
    if (raw) requested.searchParams.delete(RAW_PARAMETER);
    const target = `${origin}${requested.pathname}${requested.search}`;
    const destination = req.headers['sec-fetch-dest'];
    const isDocument = !raw && (destination ? DOCUMENT_DESTINATIONS.has(destination) : /text\/html/iu.test(String(req.headers.accept || '')));

    // The same admission the address field applies; the connect-time check covers what names resolve to.
    const admission = BrowserContext.admitPageAddress(target);
    if (admission.type === 'Refused') {
      const reason = admission.reason === 'LocalNetwork' ? 'LocalNetwork' : 'Unreachable';
      return isDocument ? html(res, 403, refusalDocument(target, reason)) : plain(res, 403, 'Refused.');
    }

    let upstreamResponse;
    try {
      upstreamResponse = await upstream.request(target, { method: req.method, headers: requestHeaders(req, origin), body: await readRequestBody(req) });
    } catch (error) {
      const reason = error instanceof UpstreamRefusal ? error.reason : 'Unreachable';
      log(`frame ${req.method} ${target} refused: ${reason}`);
      return isDocument ? html(res, 502, refusalDocument(target, reason)) : plain(res, 502, 'Unreachable.');
    }

    const status = upstreamResponse.statusCode ?? 502;
    const headers = responseHeaders(upstreamResponse, target);
    const contentType = String(upstreamResponse.headers['content-type'] || '');
    const hasBody = req.method !== 'HEAD' && status !== 204 && status !== 304 && !(status >= 300 && status < 400);

    if (isDocument && hasBody && status < 400 && WRAPPED_TYPES.test(contentType) && !/html/iu.test(contentType)) {
      upstreamResponse.resume();
      const rawUrl = new URL(requested.href);
      rawUrl.searchParams.set(RAW_PARAMETER, '1');
      return html(res, 200, wrapperDocument(target, `${rawUrl.pathname}${rawUrl.search}`));
    }

    if (isDocument && hasBody && /text\/html|application\/xhtml\+xml/iu.test(contentType)) {
      let body;
      try {
        body = await readBounded(decodedBody(upstreamResponse), MAX_HTML_BYTES);
      } catch {
        return html(res, 502, refusalDocument(target, 'Unreachable'));
      }
      if (body === null) return html(res, 502, refusalDocument(target, 'Unreachable'));
      const injected = injectReporter(body, { pageUrl: target, token, port });
      delete headers['content-encoding'];
      delete headers['content-length'];
      delete headers.etag;
      headers['content-length'] = injected.length;
      res.writeHead(status, headers);
      return res.end(injected);
    }

    res.writeHead(status, headers);
    if (req.method === 'HEAD') {
      upstreamResponse.resume();
      return res.end();
    }
    upstreamResponse.pipe(res);
    upstreamResponse.on('error', () => res.destroy());
  }

  return Object.freeze({
    setPort(value) {
      port = value;
    },
    handle(req, res) {
      handle(req, res).catch((error) => {
        log(`frame error ${req.method} ${req.url}: ${error.stack || error}`);
        if (!res.headersSent) plain(res, 502, 'Unreachable.');
        else res.destroy();
      });
    }
  });
}
