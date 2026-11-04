// A stand-in public web for tests and previews: named sites served from memory or a directory.
//
// It returns an `upstream` with the same contract as lib/upstream.mjs, and its requests really
// travel over HTTP through createUpstream, so the frame host and context acquisition run their
// production paths. Sites marked `local` behave like names that resolve onto the local network.
//
// FIXTURE_WEB != PUBLIC_WEB: nothing proven against this is proven against real sites.
//
// [VXG RealForever]
import fs from 'node:fs';
import http from 'node:http';
import path from 'node:path';

import { UpstreamRefusal, createUpstream } from '../lib/upstream.mjs';

const TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.mjs': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.webp': 'image/webp',
  '.txt': 'text/plain; charset=utf-8',
  '.md': 'text/plain; charset=utf-8',
  '.woff2': 'font/woff2'
};

function serveDirectory(root, mount, res, pathname) {
  if (!pathname.startsWith(mount)) return false;
  let relative = decodeURIComponent(pathname.slice(mount.length));
  if (relative === '' || relative.endsWith('/')) relative += 'index.html';
  const file = path.resolve(root, relative);
  if (!file.startsWith(`${path.resolve(root)}${path.sep}`) || !fs.existsSync(file) || !fs.statSync(file).isFile()) return false;
  res.writeHead(200, { 'content-type': TYPES[path.extname(file)] || 'application/octet-stream' });
  fs.createReadStream(file).pipe(res);
  return true;
}

/**
 * sites: { [origin]: { local?: true, routes?: { [path]: (req, res) => void }, directory?: { root, mount } } }
 */
export async function createFixtureWeb(sites) {
  const requests = [];
  const server = http.createServer((req, res) => {
    const origin = req.headers['x-fixture-origin'];
    const site = sites[origin];
    const url = new URL(req.url, 'http://fixture.invalid');
    requests.push({ origin, method: req.method, path: `${url.pathname}${url.search}`, headers: { ...req.headers } });
    const route = site?.routes?.[url.pathname];
    if (route) return route(req, res, url);
    if (site?.directory && serveDirectory(site.directory.root, site.directory.mount, res, url.pathname)) return undefined;
    res.writeHead(404, { 'content-type': 'text/html; charset=utf-8' });
    return res.end('<!doctype html><html><head><title>Not found</title></head><body><h1>Not found</h1></body></html>');
  });
  await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve));
  const port = server.address().port;
  const transport = createUpstream({
    lookup: (_hostname, _options, callback) => callback(null, [{ address: '127.0.0.1', family: 4 }]),
    admitAddress: () => true
  });

  const upstream = Object.freeze({
    request(target, { headers = {}, ...options } = {}) {
      const url = new URL(target);
      const site = sites[url.origin];
      if (!site) return Promise.reject(new UpstreamRefusal('Unreachable', `${url.origin} is not in the fixture web`));
      if (site.local) return Promise.reject(new UpstreamRefusal('LocalNetwork', `${url.hostname} resolves to a local address`));
      return transport.request(`http://fixture.test:${port}${url.pathname}${url.search}`, {
        ...options,
        headers: { ...headers, 'x-fixture-origin': url.origin }
      });
    },
    close() {
      transport.close();
    }
  });

  return Object.freeze({
    upstream,
    requests,
    close() {
      transport.close();
      return new Promise((resolve) => {
        server.close(() => resolve());
        server.closeAllConnections?.();
      });
    }
  });
}
