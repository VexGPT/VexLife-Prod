// The one way this workspace reaches the public web.
//
// Both the frame host (pages the human sees) and context acquisition (text Vex receives) reach
// pages only through here. The address check happens at connect time, on the address actually
// dialed, so a name cannot be re-pointed at this machine or the local network between a check
// and the connection.
//
// [VXG RealForever]
import dns from 'node:dns';
import http from 'node:http';
import https from 'node:https';
import net from 'node:net';
import zlib from 'node:zlib';

const DEFAULT_TIMEOUT_MS = 20000;

/** Why a page could not be reached. Reasons match BrowserContextFamily.FrameRefusal. */
export class UpstreamRefusal extends Error {
  constructor(reason, message) {
    super(message);
    this.reason = reason;
  }
}

function ipv4Public(address) {
  const [a, b, c] = address.split('.').map(Number);
  return !(
    a === 0 || a === 10 || a === 127 || a >= 224 ||
    (a === 169 && b === 254) || (a === 172 && b >= 16 && b <= 31) || (a === 192 && b === 168) ||
    (a === 100 && b >= 64 && b <= 127) || (a === 192 && b === 0 && c === 0) || (a === 198 && b >= 18 && b <= 19)
  );
}

/** True when the address is on the public internet (not this machine, a private or special range). */
export function isPublicAddress(address) {
  const family = net.isIP(address);
  if (family === 4) return ipv4Public(address);
  if (family !== 6) return false;
  const lower = address.toLowerCase();
  const mapped = /^::ffff:(\d+\.\d+\.\d+\.\d+)$/u.exec(lower);
  if (mapped) return ipv4Public(mapped[1]);
  if (lower === '::' || lower === '::1') return false;
  const first = Number.parseInt(lower.split(':')[0] || '0', 16);
  // fc00::/7 unique local, fe80::/10 link local, ff00::/8 multicast, 2001:db8::/32 documentation.
  if ((first & 0xfe00) === 0xfc00 || (first & 0xffc0) === 0xfe80 || (first & 0xff00) === 0xff00) return false;
  if (lower.startsWith('2001:db8:') || lower.startsWith('64:ff9b:')) return false;
  return true;
}

export function createUpstream({ lookup = dns.lookup, admitAddress = isPublicAddress, timeoutMs = DEFAULT_TIMEOUT_MS } = {}) {
  // Kept-alive connections were admitted when they were dialed; a reused socket is the same address.
  const agents = {
    'http:': new http.Agent({ keepAlive: true, maxSockets: 8 }),
    'https:': new https.Agent({ keepAlive: true, maxSockets: 8 })
  };

  function guardedLookup(hostname, options, callback) {
    const opts = typeof options === 'function' ? {} : { ...options };
    const done = typeof options === 'function' ? options : callback;
    lookup(hostname, { ...opts, all: true }, (error, addresses) => {
      if (error) return done(error);
      const list = Array.isArray(addresses) ? addresses : [{ address: addresses, family: net.isIP(addresses) }];
      if (list.length === 0 || list.some((entry) => !admitAddress(entry.address))) {
        const refusal = new Error(`${hostname} resolves to a local address`);
        refusal.code = 'VEX_LOCAL_NETWORK';
        return done(refusal);
      }
      if (opts.all) return done(null, list);
      return done(null, list[0].address, list[0].family);
    });
  }

  /**
   * One request. Resolves with the raw upstream response (headers and body untouched); rejects
   * with UpstreamRefusal('LocalNetwork' | 'Unreachable').
   */
  function request(target, { method = 'GET', headers = {}, body = null, signal } = {}) {
    return new Promise((resolve, reject) => {
      let url;
      try {
        url = new URL(target);
      } catch {
        reject(new UpstreamRefusal('Unreachable', 'not a URL'));
        return;
      }
      if (url.protocol !== 'https:' && url.protocol !== 'http:') {
        reject(new UpstreamRefusal('Unreachable', 'not a web address'));
        return;
      }
      const literal = url.hostname.replace(/^\[|\]$/gu, '');
      if (net.isIP(literal) && !admitAddress(literal)) {
        reject(new UpstreamRefusal('LocalNetwork', `${literal} is a local address`));
        return;
      }
      const transport = url.protocol === 'https:' ? https : http;
      const req = transport.request(url, { method, headers, lookup: guardedLookup, signal, agent: agents[url.protocol] }, (res) => {
        req.setTimeout(0);
        resolve(res);
      });
      req.setTimeout(timeoutMs, () => req.destroy(new UpstreamRefusal('Unreachable', 'timed out')));
      req.on('error', (error) => {
        if (error instanceof UpstreamRefusal) reject(error);
        else if (error.code === 'VEX_LOCAL_NETWORK') reject(new UpstreamRefusal('LocalNetwork', error.message));
        else reject(new UpstreamRefusal('Unreachable', error.message));
      });
      if (body && body.length > 0) req.end(body);
      else req.end();
    });
  }

  return Object.freeze({
    request,
    close() {
      agents['http:'].destroy();
      agents['https:'].destroy();
    }
  });
}

/** The response body with its content-encoding removed. */
export function decodedBody(response) {
  const encoding = String(response.headers['content-encoding'] || '').trim().toLowerCase();
  if (encoding === 'gzip' || encoding === 'x-gzip') return response.pipe(zlib.createGunzip());
  if (encoding === 'deflate') return response.pipe(zlib.createInflate());
  if (encoding === 'br') return response.pipe(zlib.createBrotliDecompress());
  return response;
}

/** Read a stream into one Buffer; null when it is longer than maxBytes. */
export async function readBounded(stream, maxBytes) {
  const chunks = [];
  let total = 0;
  for await (const chunk of stream) {
    total += chunk.length;
    if (total > maxBytes) {
      stream.destroy?.();
      return null;
    }
    chunks.push(chunk);
  }
  return Buffer.concat(chunks);
}
