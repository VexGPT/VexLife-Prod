// Vex's browser frame host: what it serves, what it removes, and what it refuses.
// [VXG RealForever]
import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import http from 'node:http';
import os from 'node:os';
import path from 'node:path';

import { createWorkspaceServer } from '../server.mjs';
import { createFixtureWeb } from '../tools/fixture-web.mjs';
import { createUpstream, isPublicAddress } from '../lib/upstream.mjs';
import { frameUrlFor, pageUrlOf, upstreamOriginOf } from '../public/frame-address.mjs';
import { getJson } from '../public/turn-stream.mjs';

const PAGE_HTML = `<!doctype html><html lang="en"><head><meta charset="utf-8">
<meta http-equiv="Content-Security-Policy" content="script-src https://vgong24.github.io">
<meta http-equiv="refresh" content="30; url=https://vexgpt.github.io/VexWork/">
<title>Terrain</title><script src="app.js"></script></head><body><a href="https://vexgpt.github.io/">VexWork</a></body></html>`;

const SITES = {
  'https://vgong24.github.io': {
    routes: {
      '/page.html': (req, res) => {
        res.writeHead(200, {
          'content-type': 'text/html; charset=utf-8',
          'x-frame-options': 'DENY',
          'content-security-policy': "frame-ancestors 'none'",
          'set-cookie': 'session=upstream; Path=/',
          'strict-transport-security': 'max-age=1',
          'cache-control': 'max-age=60'
        });
        res.end(PAGE_HTML);
      },
      '/app.js': (req, res) => {
        res.writeHead(200, { 'content-type': 'text/javascript' });
        res.end('window.app = 1;');
      },
      '/moved': (req, res) => {
        res.writeHead(301, { location: '/Vextreme/' });
        res.end();
      },
      '/elsewhere': (req, res) => {
        res.writeHead(302, { location: 'https://vexgpt.github.io/VexWork/' });
        res.end();
      },
      '/notes.txt': (req, res) => {
        res.writeHead(200, { 'content-type': 'text/plain; charset=utf-8' });
        res.end('plain notes');
      },
      '/sw.js': (req, res) => {
        res.writeHead(200, { 'content-type': 'text/javascript' });
        res.end('self.addEventListener("fetch", () => {});');
      }
    }
  },
  'https://intranet.example': { local: true }
};

async function withFrameHost(body) {
  const dataDir = fs.mkdtempSync(path.join(os.tmpdir(), 'vex-frame-test-'));
  const web = await createFixtureWeb(SITES);
  const workspace = createWorkspaceServer({ port: 0, framePort: 0, dataDir, endpoint: 'http://127.0.0.1:9', upstream: web.upstream });
  const listening = await workspace.listen();
  const frameHost = { token: listening.frameToken, port: listening.framePort };
  try {
    await body({ ...listening, frameHost, web, dataDir });
  } finally {
    await workspace.close();
    await web.close();
    fs.rmSync(dataDir, { recursive: true, force: true });
  }
}

function frameRequest(frameHost, pageUrl, { headers = {}, method = 'GET', raw = null } = {}) {
  const framed = raw ?? frameUrlFor(pageUrl, frameHost);
  const url = new URL(framed);
  return new Promise((resolve, reject) => {
    const req = http.request({
      host: '127.0.0.1',
      port: frameHost.port,
      method,
      path: `${url.pathname}${url.search}`,
      headers: { host: url.host, ...headers }
    }, (res) => {
      const chunks = [];
      res.on('data', (chunk) => chunks.push(chunk));
      res.on('end', () => resolve({ status: res.statusCode, headers: res.headers, body: Buffer.concat(chunks).toString('utf8') }));
    });
    req.on('error', reject);
    req.end();
  });
}

const asDocument = { 'sec-fetch-dest': 'iframe', accept: 'text/html', cookie: 'session=leaked' };

test('a public page has one frame-host address and back', () => {
  const frameHost = { token: 'abc123def456', port: 18211 };
  const cases = [
    ['https://vgong24.github.io/Vextreme/pages/terrain-map.html?group=12#top', 'http://vgong24.github.io.s.abc123def456.localhost:18211/Vextreme/pages/terrain-map.html?group=12#top'],
    ['http://example.com:8080/a', 'http://example.com.p8080.abc123def456.localhost:18211/a'],
    ['https://youtube.com/', 'http://youtube.com.s.abc123def456.localhost:18211/']
  ];
  for (const [page, framed] of cases) {
    assert.equal(frameUrlFor(page, frameHost), framed);
    assert.equal(pageUrlOf(framed, frameHost), page);
  }
  assert.equal(upstreamOriginOf('vgong24.github.io.s.zzzzzzzzzzzz.localhost', frameHost), null, 'another token is not this frame host');
  assert.equal(upstreamOriginOf('s.abc123def456.localhost', frameHost), null);
  assert.equal(upstreamOriginOf('example.com.s443.abc123def456.localhost', frameHost), 'https://example.com');
  assert.equal(frameUrlFor('https://[2001:db8::1]/', frameHost), null);
});

test('only public addresses are dialed, checked on the address actually resolved', async () => {
  assert.equal(isPublicAddress('8.8.8.8'), true);
  for (const local of ['127.0.0.1', '10.1.2.3', '192.168.0.4', '169.254.169.254', '100.64.0.1', '::1', 'fe80::1', 'fd00::5', '::ffff:127.0.0.1']) {
    assert.equal(isPublicAddress(local), false, local);
  }
  const upstream = createUpstream({ lookup: (_name, _options, callback) => callback(null, [{ address: '10.0.0.5', family: 4 }]) });
  try {
    await assert.rejects(upstream.request('https://rebinding.example/'), { reason: 'LocalNetwork' });
    await assert.rejects(upstream.request('http://127.0.0.1:18210/api/session'), { reason: 'LocalNetwork' });
  } finally {
    upstream.close();
  }
});

test('a framed page loses what forbids framing, gains the reporter first, and browses signed out', async () => {
  await withFrameHost(async ({ frameHost, web }) => {
    const page = await frameRequest(frameHost, 'https://vgong24.github.io/page.html', { headers: asDocument });
    assert.equal(page.status, 200);
    for (const header of ['x-frame-options', 'content-security-policy', 'set-cookie', 'strict-transport-security']) {
      assert.equal(page.headers[header], undefined, header);
    }
    assert.equal(page.headers['cache-control'], 'max-age=60');
    assert.match(page.body, /<head><script src="\/__vex-frame\/reporter\.js"><\/script><meta charset="utf-8">/u);
    assert.doesNotMatch(page.body, /Content-Security-Policy/iu, 'meta CSP removed');
    assert.match(page.body, new RegExp(`url=http://vexgpt\\.github\\.io\\.s\\.${frameHost.token}\\.localhost:${frameHost.port}/VexWork/`, 'u'), 'meta refresh stays in the frame');
    assert.match(page.body, /<a href="https:\/\/vexgpt\.github\.io\/">/u, 'links keep their real address; the reporter keeps them in the frame');
    const sent = web.requests.find((request) => request.path === '/page.html');
    assert.equal(sent.headers.cookie, undefined, 'no cookie reaches the site');

    const script = await frameRequest(frameHost, 'https://vgong24.github.io/app.js', { headers: { 'sec-fetch-dest': 'script' } });
    assert.equal(script.body, 'window.app = 1;', 'subresources pass through untouched');
  });
});

test('the reporter is served for this frame host and workspace only', async () => {
  await withFrameHost(async ({ frameHost, port }) => {
    const reporter = await frameRequest(frameHost, 'https://vgong24.github.io/__vex-frame/reporter.js');
    assert.equal(reporter.status, 200);
    assert.match(reporter.headers['content-type'], /javascript/u);
    assert.ok(reporter.body.includes(`var TOKEN = ${JSON.stringify(frameHost.token)};`));
    assert.ok(reporter.body.includes(`var PORT = ${JSON.stringify(String(frameHost.port))};`));
    assert.ok(reporter.body.includes(`var PARENT_ORIGINS = ["http://127.0.0.1:${port}","http://localhost:${port}"];`));
    assert.doesNotMatch(reporter.body, /'__VEX_(TOKEN|PORT)__'|__VEX_PARENT_ORIGINS__;/u);
  });
});

test('redirects stay inside Vex’s browser', async () => {
  await withFrameHost(async ({ frameHost }) => {
    const moved = await frameRequest(frameHost, 'https://vgong24.github.io/moved', { headers: asDocument });
    assert.equal(moved.status, 301);
    assert.equal(moved.headers.location, frameUrlFor('https://vgong24.github.io/Vextreme/', frameHost));
    const elsewhere = await frameRequest(frameHost, 'https://vgong24.github.io/elsewhere', { headers: asDocument });
    assert.equal(elsewhere.headers.location, frameUrlFor('https://vexgpt.github.io/VexWork/', frameHost));
  });
});

test('local, unreachable and foreign names are refused with a page that reports why', async () => {
  await withFrameHost(async ({ frameHost }) => {
    const byForm = await frameRequest(frameHost, 'http://192.168.1.1/', { headers: asDocument });
    assert.equal(byForm.status, 403);
    assert.match(byForm.body, /<meta name="vex-frame-refusal" content="LocalNetwork">/u);
    assert.match(byForm.body, /<meta name="vex-frame-address" content="http:\/\/192\.168\.1\.1\/">/u);

    const byResolution = await frameRequest(frameHost, 'https://intranet.example/', { headers: asDocument });
    assert.equal(byResolution.status, 502);
    assert.match(byResolution.body, /content="LocalNetwork"/u);

    const unreachable = await frameRequest(frameHost, 'https://offline.example/', { headers: asDocument });
    assert.match(unreachable.body, /content="Unreachable"/u);

    const foreign = await frameRequest(frameHost, null, { raw: `http://vgong24.github.io.s.zzzzzzzzzzzz.localhost:${frameHost.port}/`, headers: asDocument });
    assert.equal(foreign.status, 421);
    const workspaceName = await frameRequest(frameHost, null, { raw: `http://127.0.0.1:${frameHost.port}/`, headers: asDocument });
    assert.equal(workspaceName.status, 421);
  });
});

test('a non-HTML document is wrapped so the frame can still report it; service workers are refused', async () => {
  await withFrameHost(async ({ frameHost }) => {
    const wrapper = await frameRequest(frameHost, 'https://vgong24.github.io/notes.txt', { headers: asDocument });
    assert.match(wrapper.body, /<script src="\/__vex-frame\/reporter\.js"><\/script>/u);
    assert.match(wrapper.body, /<iframe src="\/notes\.txt\?__vexframe_raw=1"/u);
    const raw = await frameRequest(frameHost, null, { raw: `${frameUrlFor('https://vgong24.github.io/notes.txt', frameHost)}?__vexframe_raw=1`, headers: { 'sec-fetch-dest': 'iframe' } });
    assert.equal(raw.body, 'plain notes');

    const worker = await frameRequest(frameHost, 'https://vgong24.github.io/sw.js', { headers: { 'service-worker': 'script', 'sec-fetch-dest': 'serviceworker' } });
    assert.equal(worker.status, 403);
  });
});

test('the workspace names its frame host and frames nothing else it serves', async () => {
  await withFrameHost(async ({ origin, frameHost }) => {
    const session = await getJson('/api/session', { origin });
    assert.deepEqual(session.frameHost, { ...frameHost, signedOut: true });
    const response = await fetch(`${origin}/`);
    const csp = response.headers.get('content-security-policy');
    assert.match(csp, new RegExp(`frame-src http://\\*\\.${frameHost.token}\\.localhost:${frameHost.port} https:`, 'u'));
    assert.match(csp, /frame-ancestors 'none'/u);
  });
});

test('the frame-host token persists with the data directory', async () => {
  const dataDir = fs.mkdtempSync(path.join(os.tmpdir(), 'vex-frame-token-'));
  try {
    const first = createWorkspaceServer({ port: 0, framePort: 0, dataDir });
    const second = createWorkspaceServer({ port: 0, framePort: 0, dataDir });
    assert.match(first.frameToken, /^[a-z0-9]{12}$/u);
    assert.equal(second.frameToken, first.frameToken);
  } finally {
    fs.rmSync(dataDir, { recursive: true, force: true });
  }
});
