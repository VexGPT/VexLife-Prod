// Where a public page lives inside Vex's browser, and back.
//
// The frame host gives every public site its own local origin, so the site keeps its own paths,
// storage and same-origin rules while the workspace can hear where it is:
//
//   https://vgong24.github.io/Vextreme/pages/terrain-map.html?group=12
//   <-> http://vgong24.github.io.s.<token>.localhost:<port>/Vextreme/pages/terrain-map.html?group=12
//
// The label before the token carries the scheme and a non-default port: `s` = https, `p` = http,
// e.g. `s8443`. The token is per data directory, so other sites cannot guess frame-host names.
// Browsers resolve every *.localhost name to this machine.
//
// Used by the workspace page (to navigate the frame) and by the server (to answer it). The
// reporter injected into framed pages carries its own copy of `upstreamOriginOf`; the frame-host
// tests hold the two together.
//
// [VXG RealForever]

const HOST_SHAPE = /^[a-z0-9_-]+(\.[a-z0-9_-]+)*$/u;
const MARKER = /^([sp])([0-9]{1,5})?$/u;
export const TOKEN_SHAPE = /^[a-z0-9]{12}$/u;

/** The frame-host URL for an admitted public page address, or null if it cannot be carried. */
export function frameUrlFor(pageUrl, { token, port }) {
  let url;
  try {
    url = new URL(pageUrl);
  } catch {
    return null;
  }
  if (url.protocol !== 'https:' && url.protocol !== 'http:') return null;
  if (url.username || url.password) return null;
  const host = url.hostname;
  if (!HOST_SHAPE.test(host)) return null;
  const marker = `${url.protocol === 'https:' ? 's' : 'p'}${url.port}`;
  const frameHost = `${host}.${marker}.${token}.localhost`;
  if (frameHost.length > 253) return null;
  return `http://${frameHost}:${port}${url.pathname}${url.search}${url.hash}`;
}

/**
 * The upstream origin a frame-host name stands for ("https://vgong24.github.io"), or null when
 * the name is not one of this frame host's.
 */
export function upstreamOriginOf(hostname, { token }) {
  const suffix = `.${token}.localhost`;
  const name = String(hostname).toLowerCase();
  if (!name.endsWith(suffix)) return null;
  const labels = name.slice(0, -suffix.length).split('.');
  const marker = MARKER.exec(labels.pop() ?? '');
  if (!marker || labels.length === 0) return null;
  const host = labels.join('.');
  if (!HOST_SHAPE.test(host)) return null;
  const scheme = marker[1] === 's' ? 'https' : 'http';
  const port = marker[2] ? Number(marker[2]) : null;
  if (port !== null && (port < 1 || port > 65535)) return null;
  const defaultPort = scheme === 'https' ? 443 : 80;
  return `${scheme}://${host}${port && port !== defaultPort ? `:${port}` : ''}`;
}

/** The public page address a frame-host URL shows, or null when it is not a frame-host URL. */
export function pageUrlOf(frameUrl, { token, port }) {
  let url;
  try {
    url = new URL(frameUrl);
  } catch {
    return null;
  }
  if (url.protocol !== 'http:' || url.port !== String(port)) return null;
  const origin = upstreamOriginOf(url.hostname, { token });
  return origin ? `${origin}${url.pathname}${url.search}${url.hash}` : null;
}
