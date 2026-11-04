// Form the exact text a turn may carry for a page, with its identity (sha256, length, capture).
//
// HUMAN NESTED RENDER != VEX CONTEXT ACCESS. Two ways in, both named on the receipt:
//   Rendered -> the text the page showed in the human's frame, handed over by the frame reporter
//               when the human included the page (formRenderedContext);
//   Source   -> the page source fetched separately, when the frame could not supply what it showed
//               (acquirePageContext). Only public pages are fetched (see upstream.mjs), and every
//               redirect hop is admitted again.
// Neither path reads cookies, storage or form values.
//
// [VXG RealForever]
import crypto from 'node:crypto';

import { BrowserContext } from '../public/families.mjs';
import { decodedBody, readBounded } from './upstream.mjs';

export const CONTEXT_CHARACTER_LIMIT = 12000;
export const RENDERED_TEXT_LIMIT = 200000;
const MAX_SOURCE_BYTES = 2 * 1024 * 1024;
const MAX_REDIRECTS = 3;
const MAX_TITLE = 300;

const ENTITY = {
  amp: '&', lt: '<', gt: '>', quot: '"', apos: "'", nbsp: ' ', ensp: ' ', emsp: ' ', thinsp: ' ',
  mdash: '—', ndash: '–', hellip: '…', rsquo: '’', lsquo: '‘', rdquo: '”', ldquo: '“', laquo: '«', raquo: '»',
  lsaquo: '‹', rsaquo: '›', middot: '·', bull: '•', copy: '©', reg: '®', trade: '™', deg: '°', times: '×',
  rarr: '→', larr: '←', uarr: '↑', darr: '↓', harr: '↔'
};

function decodeEntities(text) {
  return text.replace(/&(#x[0-9a-f]+|#\d+|[a-z]+);/giu, (match, body) => {
    if (body[0] === '#') {
      const code = body[1] === 'x' || body[1] === 'X' ? Number.parseInt(body.slice(2), 16) : Number.parseInt(body.slice(1), 10);
      return Number.isFinite(code) && code > 0 && code <= 0x10ffff ? String.fromCodePoint(code) : match;
    }
    return ENTITY[body.toLowerCase()] ?? match;
  });
}

function collapse(text) {
  return text.replace(/[\t\f\v\r ]+/gu, ' ').trim();
}

/** Readable text of an HTML source document: what a reader of the source would see, not the live DOM. */
export function extractReadableText(html) {
  const titleMatch = /<title\b[^>]*>([\s\S]*?)<\/title\s*>/iu.exec(html);
  const title = titleMatch ? collapse(decodeEntities(titleMatch[1].replace(/<[^>]+>/gu, ' '))) || null : null;
  // Script-rendered pages often carry their only source-level summary in the meta description.
  const descriptionMatch = /<meta\b(?=[^>]*\bname\s*=\s*["']description["'])[^>]*\bcontent\s*=\s*(["'])([\s\S]*?)\1[^>]*>/iu.exec(html);
  const description = descriptionMatch ? collapse(decodeEntities(descriptionMatch[2])) : '';

  let body = html
    .replace(/<!--[\s\S]*?-->/gu, ' ')
    .replace(/<(script|style|noscript|template|svg|canvas|iframe|object)\b[^>]*>[\s\S]*?<\/\1\s*>/giu, ' ')
    .replace(/<head\b[^>]*>[\s\S]*?<\/head\s*>/iu, ' ')
    .replace(/<h[1-6]\b[^>]*>/giu, '\n\n')
    .replace(/<li\b[^>]*>/giu, '\n- ')
    .replace(/<(br|hr)\b[^>]*>/giu, '\n')
    .replace(/<\/(p|div|h[1-6]|tr|section|article|header|footer|main|nav|aside|ul|ol|table|pre|blockquote|figure|figcaption|dd|dt)\s*>/giu, '\n')
    .replace(/<td\b[^>]*>/giu, ' | ')
    .replace(/<[^>]+>/gu, ' ');

  body = decodeEntities(body);

  const lines = [];
  for (const raw of body.split('\n')) {
    const line = collapse(raw);
    if (line === '-' || line === '|') continue;
    if (line.length === 0) {
      if (lines.length > 0 && lines[lines.length - 1] !== '') lines.push('');
      continue;
    }
    lines.push(line);
  }
  while (lines.length > 0 && lines[lines.length - 1] === '') lines.pop();

  const text = lines.join('\n');
  return { title, text: description ? `Page description: ${description}${text ? `\n\n${text}` : ''}` : text };
}

export function sha256Hex(text) {
  return crypto.createHash('sha256').update(text, 'utf8').digest('hex');
}

function failed(page, reason) {
  return { observation: { type: 'ContextFailed', page, reason }, record: null };
}

function formed({ page, sourceUrl, title, text: fullText, capture, now }) {
  const truncated = fullText.length > CONTEXT_CHARACTER_LIMIT;
  const text = truncated ? fullText.slice(0, CONTEXT_CHARACTER_LIMIT) : fullText;
  const sha256 = sha256Hex(text);
  const receipt = {
    contextRef: `context-${sha256.slice(0, 16)}`,
    sourceUrl,
    title,
    characters: text.length,
    sha256,
    truncated,
    capture
  };
  return {
    observation: { type: 'ContextAcquired', page, receipt },
    record: { schemaVersion: 'vexlife-prod.exploration.context-record/v2', ...receipt, page, acquiredAt: now().toISOString(), text }
  };
}

function normalizedTitle(title) {
  if (typeof title !== 'string') return null;
  const collapsed = collapse(title);
  return collapsed.length === 0 ? null : collapsed.slice(0, MAX_TITLE);
}

/**
 * Form a receipt from the text the page showed in the human's frame.
 * The frame reporter supplied it; the page address must still be an admitted public page.
 */
export function formRenderedContext({ page, title, text, now = () => new Date() }) {
  if (BrowserContext.parsePageAddress(page) !== page) return failed(page, 'SourceNotAdmitted');
  if (typeof text !== 'string') return failed(page, 'NoReadableText');
  const cleaned = text
    .slice(0, RENDERED_TEXT_LIMIT)
    .replace(/\r\n?/gu, '\n')
    .split('\n')
    .map((line) => collapse(line))
    .join('\n')
    .replace(/\n{3,}/gu, '\n\n')
    .trim();
  if (cleaned.length === 0) return failed(page, 'NoReadableText');
  return formed({ page, sourceUrl: page, title: normalizedTitle(title), text: cleaned, capture: 'Rendered', now });
}

/**
 * Fetch the page source and form its context receipt.
 * Returns { observation, record } where observation is a BrowserContext observation and record holds
 * the full supplied text (kept by the workspace so the human can inspect exactly what Vex receives).
 */
export async function acquirePageContext({ page, upstream, now = () => new Date() }) {
  if (BrowserContext.parsePageAddress(page) !== page) return failed(page, 'SourceNotAdmitted');

  let url = page.replace(/#.*$/u, '');
  let response;
  try {
    for (let hop = 0; ; hop += 1) {
      response = await upstream.request(url, {
        headers: {
          accept: 'text/html,text/plain;q=0.9,*/*;q=0.1',
          'accept-encoding': 'gzip, deflate, br',
          'user-agent': 'VexLife-Prod web workspace exploration (context acquisition)'
        }
      });
      const status = response.statusCode ?? 0;
      if (status >= 300 && status < 400 && response.headers.location) {
        response.resume();
        if (hop >= MAX_REDIRECTS) return failed(page, 'SourceUnreachable');
        const next = BrowserContext.parsePageAddress(new URL(response.headers.location, url).href);
        if (!next) return failed(page, 'SourceNotAdmitted');
        url = next;
        continue;
      }
      break;
    }
  } catch (error) {
    return failed(page, error?.reason === 'LocalNetwork' ? 'SourceNotAdmitted' : 'SourceUnreachable');
  }

  const status = response.statusCode ?? 0;
  if (status < 200 || status >= 300) {
    response.resume();
    return failed(page, 'SourceUnreachable');
  }

  const contentType = String(response.headers['content-type'] || '');
  const readable = /html|xml/iu.test(contentType) || /^text\//iu.test(contentType) || /json/iu.test(contentType) || contentType === '';
  if (!readable) {
    response.resume();
    return failed(page, 'NoReadableText');
  }

  let source;
  try {
    const buffer = await readBounded(decodedBody(response), MAX_SOURCE_BYTES);
    source = (buffer ?? Buffer.alloc(0)).toString('utf8');
  } catch {
    return failed(page, 'SourceUnreachable');
  }

  const extracted = /html/iu.test(contentType) || /<html|<body|<p\b/iu.test(source)
    ? extractReadableText(source)
    : { title: null, text: source.trim() };

  if (extracted.text.length === 0) return failed(page, 'NoReadableText');
  return formed({ page, sourceUrl: url, title: extracted.title, text: extracted.text, capture: 'Source', now });
}
