// Vex's browser: location reporter, injected by the frame host as the first script of each page.
//
// It tells the workspace where the frame is, so the address field follows the page into depth, and
// keeps link-following inside Vex's browser. It reads nothing private: no cookies, no storage, no
// form values. Page text leaves only when the workspace asks for it to form a context receipt.
//
// Served with __VEX_TOKEN__, __VEX_PORT__ and __VEX_PARENT_ORIGINS__ filled in by the frame host.
// [VXG RealForever]
(function () {
  'use strict';
  if (window.__vexFrameReporter) return;
  window.__vexFrameReporter = true;

  var TOKEN = '__VEX_TOKEN__';
  var PORT = '__VEX_PORT__';
  var PARENT_ORIGINS = __VEX_PARENT_ORIGINS__;
  var SUFFIX = '.' + TOKEN + '.localhost';
  var TEXT_LIMIT = 200000;

  // Only the frame Vex's workspace holds reports; frames inside the page stay quiet.
  var reporting = window.parent !== window && window.parent === window.top;
  var doc = Math.random().toString(36).slice(2) + Date.now().toString(36);

  function upstreamOriginOf(hostname) {
    var name = String(hostname).toLowerCase();
    if (name.slice(-SUFFIX.length) !== SUFFIX) return null;
    var labels = name.slice(0, -SUFFIX.length).split('.');
    var marker = /^([sp])([0-9]{1,5})?$/.exec(labels.pop() || '');
    if (!marker || labels.length === 0) return null;
    var scheme = marker[1] === 's' ? 'https' : 'http';
    var port = marker[2] ? Number(marker[2]) : null;
    var defaultPort = scheme === 'https' ? 443 : 80;
    return scheme + '://' + labels.join('.') + (port && port !== defaultPort ? ':' + port : '');
  }

  function pageUrl(href) {
    var url = new URL(href, location.href);
    var origin = url.port === PORT ? upstreamOriginOf(url.hostname) : null;
    return origin ? origin + url.pathname + url.search + url.hash : url.href;
  }

  function frameUrl(href) {
    var url;
    try {
      url = new URL(href, document.baseURI);
    } catch (e) {
      return null;
    }
    if (url.protocol !== 'http:' && url.protocol !== 'https:') return null;
    if (url.port === PORT && upstreamOriginOf(url.hostname)) return url.href;
    if (!/^[a-z0-9_-]+(\.[a-z0-9_-]+)*$/.test(url.hostname)) return null;
    var marker = (url.protocol === 'https:' ? 's' : 'p') + url.port;
    return 'http://' + url.hostname + '.' + marker + SUFFIX + ':' + PORT + url.pathname + url.search + url.hash;
  }

  function post(message) {
    if (!reporting) return;
    message.vexFrame = 1;
    message.doc = doc;
    for (var i = 0; i < PARENT_ORIGINS.length; i += 1) {
      try {
        window.parent.postMessage(message, PARENT_ORIGINS[i]);
      } catch (e) { /* that origin is not the parent */ }
    }
  }

  var refusal = document.querySelector('meta[name="vex-frame-refusal"]');
  if (refusal) {
    var address = document.querySelector('meta[name="vex-frame-address"]');
    post({ kind: 'refused', href: address ? address.content : pageUrl(location.href), reason: refusal.content });
    return;
  }

  var lastHref = null;
  var lastTitle;
  function located(force) {
    var href = pageUrl(location.href);
    var title = document.title ? document.title.trim() || null : null;
    if (!force && href === lastHref && title === lastTitle) return;
    lastHref = href;
    lastTitle = title;
    post({ kind: 'located', href: href, title: title });
  }

  // Pages that write their depth into the address (pushState/replaceState) are followed.
  ['pushState', 'replaceState'].forEach(function (name) {
    var original = history[name];
    history[name] = function () {
      var result = original.apply(this, arguments);
      Promise.resolve().then(function () { located(false); });
      return result;
    };
  });
  window.addEventListener('popstate', function () { located(false); });
  window.addEventListener('hashchange', function () { located(false); });
  window.addEventListener('pageshow', function () { located(true); });
  document.addEventListener('DOMContentLoaded', function () {
    located(false);
    var head = document.head;
    if (head && window.MutationObserver) {
      new MutationObserver(function () { located(false); }).observe(head, { subtree: true, childList: true, characterData: true });
    }
  });
  located(true);

  // Links and forms that would leave Vex's browser (another site, a new tab, the top window) are
  // followed inside the frame instead. These listeners run after the page's own handlers.
  function leaves(url, target) {
    var t = (target || '').toLowerCase();
    if (t && t !== '_self') return true;
    return !(url.port === PORT && upstreamOriginOf(url.hostname));
  }

  window.addEventListener('click', function (event) {
    if (event.defaultPrevented || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
    var link = event.target && event.target.closest ? event.target.closest('a[href], area[href]') : null;
    if (!link || link.hasAttribute('download')) return;
    var url;
    try {
      url = new URL(link.href, document.baseURI);
    } catch (e) {
      return;
    }
    if (url.protocol !== 'http:' && url.protocol !== 'https:') return;
    if (!leaves(url, link.getAttribute('target'))) return;
    var destination = frameUrl(url.href);
    if (!destination) return;
    event.preventDefault();
    location.assign(destination);
  });

  window.addEventListener('submit', function (event) {
    if (event.defaultPrevented) return;
    var form = event.target;
    if (!form || form.tagName !== 'FORM') return;
    var url;
    try {
      url = new URL(form.action || location.href, document.baseURI);
    } catch (e) {
      return;
    }
    if (!leaves(url, form.getAttribute('target'))) return;
    var destination = frameUrl(url.href);
    if (!destination) return;
    event.preventDefault();
    if ((form.method || 'get').toLowerCase() === 'get') {
      var query = new URL(destination);
      query.search = new URLSearchParams(new FormData(form)).toString();
      location.assign(query.href);
    } else {
      form.action = destination;
      form.target = '_self';
      HTMLFormElement.prototype.submit.call(form);
    }
  });

  window.open = function (href) {
    var destination = href ? frameUrl(String(href)) : null;
    if (destination) location.assign(destination);
    return null;
  };

  // The workspace asks for the page as it is rendered, to form a context receipt.
  function readableText() {
    var body = document.body;
    if (!body) return '';
    var text = (body.innerText || body.textContent || '')
      .replace(/ /g, ' ')
      .replace(/[ \t]+\n/g, '\n')
      .replace(/\n{3,}/g, '\n\n')
      .trim();
    var description = document.querySelector('meta[name="description"]');
    var summary = description && description.content ? description.content.trim() : '';
    if (summary && text.indexOf(summary) === -1) text = 'Page description: ' + summary + (text ? '\n\n' + text : '');
    return text.slice(0, TEXT_LIMIT);
  }

  window.addEventListener('message', function (event) {
    if (event.source !== window.parent || PARENT_ORIGINS.indexOf(event.origin) === -1) return;
    var data = event.data;
    if (!data || data.vexFrameCommand !== 1 || data.op !== 'capture') return;
    var title = document.title ? document.title.trim() || null : null;
    post({ kind: 'captured', nonce: data.nonce, href: pageUrl(location.href), title: title, text: readableText() });
  });
})();
