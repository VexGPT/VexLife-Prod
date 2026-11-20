#!/usr/bin/env node
/**
 * Read-only VexLife localization/identity relationship audit.
 *
 * This does not invent a second UI registry. It reads the Product-owned Screen Lens,
 * workspace string catalog and VexWeb/markup source, then reports missing relationship edges.
 * Run from the VexLife-Prod repository root:
 *   node tools/localization-identity-audit.mjs
 */
import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const lensFile = path.join(root, 'docs/lens/vexlife-prod.screen-lens.json');
const stringsFiles = [
  path.join(root, 'localization/strings/source/workspace.json'),
  path.join(root, 'localization/strings/source/release-alerts.json'),
];
const vexWebFile = path.join(root, 'src/commonMain/kotlin/vexlife/web/VexLifeWebP1.kt');
const markupFile = path.join(root, 'src/commonMain/kotlin/vexlife/web/VexLifeWebP1ExperienceMarkup.kt');
const familyFiles = [
  'src/commonMain/kotlin/vexlife/vision/VexLifeVisionFamily.kt',
  'src/commonMain/kotlin/vexlife/memory/VexLifeMemoryFamily.kt',
  'src/commonMain/kotlin/vexlife/continuity/VexLifePrivateModeFamily.kt',
  'src/commonMain/kotlin/vexlife/context/VexLifeContextProfileFamily.kt',
].map(file => path.join(root, file));

const readJson = file => JSON.parse(fs.readFileSync(file, 'utf8'));
const relative = file => path.relative(root, file).split(path.sep).join('/');
const lineAt = (text, index) => text.slice(0, index).split('\n').length;
const localizationKey =
  /^[a-z0-9]+(?:-[a-z0-9]+)*(?:\.[a-z0-9]+(?:-[a-z0-9]+)*)*\.(?:button|label|heading|description|nav|legend|status|placeholder)\.[a-z0-9]+(?:-[a-z0-9]+)*$/;
const human = text =>
  /[A-Za-z]{2}/.test(text) &&
  text !== 'Vex' &&
  text !== 'VexLife' &&
  !localizationKey.test(text);

const lens = readJson(lensFile);
const catalogs = stringsFiles.map(readJson);
const catalog = Object.assign(
  {},
  ...catalogs.map(source =>
    Object.fromEntries(
      Object.entries(source).filter(([key]) => key !== '_meta')
    )
  )
);
const elements = (lens.screens ?? []).flatMap(screen => screen.elements ?? []);
const byRef = new Map(elements.map(element => [element.ref, element]));
const screenRefs = new Set((lens.screens ?? []).map(screen => screen.screenRef));
const findings = [];

function finding(severity, kind, fields = {}) {
  findings.push({ severity, kind, ...fields });
}

function parentChain(ref) {
  const chain = [];
  const seen = new Set();
  let current = ref;
  while (current) {
    if (seen.has(current)) {
      finding('BLOCK', 'LENS_PARENT_CYCLE', { elementRef: ref, parentChain: [...chain, current] });
      break;
    }
    seen.add(current);
    chain.push(current);
    const element = byRef.get(current);
    if (!element) {
      if (!screenRefs.has(current)) {
        finding('BLOCK', 'LENS_PARENT_MISSING', { elementRef: ref, missingParentRef: current, parentChain: chain });
      }
      break;
    }
    current = element.parentRef ?? null;
  }
  return chain;
}

const chains = new Map(elements.map(element => [element.ref, parentChain(element.ref)]));
const keysByLensRef = new Map();
for (const [key, entry] of Object.entries(catalog)) {
  if (key === '_meta' || !entry?.lens) continue;
  if (!keysByLensRef.has(entry.lens)) keysByLensRef.set(entry.lens, []);
  keysByLensRef.get(entry.lens).push(key);
  if (!byRef.has(entry.lens)) {
    finding('BLOCK', 'CATALOG_LENS_REF_MISSING', { key, elementRef: entry.lens });
  }
}

// VexWeb property -> exact Blueprint symbol. The source object remains the owner of this relation.
const vexWeb = fs.readFileSync(vexWebFile, 'utf8');
const propertyStarts = [...vexWeb.matchAll(/^ {4}val\s+([A-Za-z][A-Za-z0-9_]*)\s*=/gm)];
const blueprintByVexWebProperty = new Map();
for (let index = 0; index < propertyStarts.length; index++) {
  const match = propertyStarts[index];
  const end = propertyStarts[index + 1]?.index ?? vexWeb.length;
  const body = vexWeb.slice(match.index, end);
  const symbol = /VexLifeWorkspaceBlueprint\.([A-Za-z][A-Za-z0-9_]*)/.exec(body)?.[1];
  if (symbol) blueprintByVexWebProperty.set(match[1], symbol);
}

const refByBlueprintSymbol = new Map();
for (const element of elements) {
  const symbol = /^VexLifeWorkspaceBlueprint\.([A-Za-z][A-Za-z0-9_]*)$/.exec(String(element.symbol ?? ''))?.[1];
  if (symbol) refByBlueprintSymbol.set(symbol, element.ref);
}

// Concrete Product markup -> VexWeb property -> Blueprint -> Lens -> localization key.
const markup = fs.readFileSync(markupFile, 'utf8');
const tagPattern = /<(?:button|a|label|input|select|textarea|p|span|h[1-6]|section|div|ul|ol|nav|form|iframe)\b[^>]*>/gi;
const refPattern = /data-vex-ref\s*=\s*["']\$\{VexLifeWebP1\.([A-Za-z][A-Za-z0-9_]*)\.identity\.ref\}["']/;
const attrPattern = /(aria-label|title|placeholder)\s*=\s*["']([^"'$\n]*[A-Za-z][^"'$\n]*)["']/g;
const bindingToAttribute = Object.freeze({
  'data-vex-text': null,
  'data-vex-placeholder': 'placeholder',
  'data-vex-aria-label': 'aria-label',
  'data-vex-title': 'title',
});
const markupRelationships = [];
for (const match of markup.matchAll(tagPattern)) {
  const tag = match[0];
  const property = refPattern.exec(tag)?.[1];
  if (!property) continue;
  const blueprintSymbol = blueprintByVexWebProperty.get(property) ?? null;
  const elementRef = blueprintSymbol ? (refByBlueprintSymbol.get(blueprintSymbol) ?? null) : null;
  const bindings =
    Object.entries(bindingToAttribute)
      .map(([binding, attribute]) => {
        const key =
          new RegExp(binding + "\\s*=\\s*[\\\"']([^\\\"']+)[\\\"']")
            .exec(tag)?.[1]
            ?.trim() ?? null;
        return key ? { binding, attribute, key } : null;
      })
      .filter(Boolean);
  const boundAttributes = new Set(bindings.map(binding => binding.attribute).filter(Boolean));
  const attrs =
    [...tag.matchAll(attrPattern)]
      .map(value => ({ name: value[1], value: value[2].trim() }))
      .filter(value => human(value.value) && !boundAttributes.has(value.name));
  const keys = elementRef ? [...(keysByLensRef.get(elementRef) ?? [])].sort() : [];
  const relationship = {
    path: relative(markupFile),
    line: lineAt(markup, match.index),
    vexWebProperty: property,
    blueprintSymbol,
    elementRef,
    parentChain: elementRef ? chains.get(elementRef) : [],
    localizationKeys: keys,
    localizationBindings: bindings,
    hardcodedAttributes: attrs,
  };
  markupRelationships.push(relationship);

  if (blueprintSymbol && !elementRef) {
    finding('HIGH', 'MARKUP_BLUEPRINT_NOT_IN_LENS', relationship);
  }
  for (const binding of bindings) {
    const entry = catalog[binding.key];
    if (!entry) {
      finding('BLOCK', 'MARKUP_LOCALIZATION_KEY_MISSING', { ...relationship, binding });
      continue;
    }
    if (elementRef && entry.lens && entry.lens !== elementRef) {
      finding('HIGH', 'MARKUP_LOCALIZATION_LENS_MISMATCH', {
        ...relationship,
        binding,
        catalogLens: entry.lens,
      });
    }
    if (elementRef && !entry.lens) {
      finding('HIGH', 'MARKUP_LOCALIZATION_KEY_MISSING_LENS_EDGE', {
        ...relationship,
        binding,
      });
    }
  }
  if (attrs.length && elementRef && !keys.length) {
    finding('HIGH', 'HUMAN_COPY_WITHOUT_LOCALIZATION_LENS_EDGE', relationship);
  }
  if (attrs.length && !blueprintSymbol) {
    finding('REVIEW', 'HUMAN_COPY_ON_NON_BLUEPRINT_VEXWEB_REF', relationship);
  }
}

// Family-authored display words must stay keyed until concrete presentation resolves them.
const familyDisplayCopy = [];
for (const file of familyFiles) {
  if (!fs.existsSync(file)) continue;
  const source = fs.readFileSync(file, 'utf8');
  const start = source.indexOf('override fun displayStateFor');
  if (start < 0) continue;
  const tail = source.slice(start);
  const boundary = tail.search(/\n {4}(?:const val|fun controlForRef|fun stateFor)\b/);
  const region = boundary < 0 ? tail : tail.slice(0, boundary);
  for (const match of region.matchAll(/"((?:\\.|[^"\\])*)"/g)) {
    const literal = match[1].replace(/\\"/g, '"').trim();
    if (!human(literal)) continue;
    const evidence = { path: relative(file), line: lineAt(source, start + match.index), literal };
    familyDisplayCopy.push(evidence);
    finding('HIGH', 'RAW_FAMILY_DISPLAY_COPY', evidence);
  }
}

// Absence of a localization Lens edge is advisory until visible Product copy is proven.
const addressableWithoutKey = elements
  .filter(element => String(element.ref ?? '').startsWith('element.'))
  .filter(element => ['BUTTON', 'FIELD', 'TEXT'].includes(element.kind))
  .filter(element => !keysByLensRef.has(element.ref))
  .map(element => ({
    elementRef: element.ref,
    kind: element.kind,
    symbol: element.symbol ?? null,
    parentChain: chains.get(element.ref) ?? [],
  }));

const counts = findings.reduce((result, item) => {
  result[item.severity] = (result[item.severity] ?? 0) + 1;
  return result;
}, {});
const report = {
  schemaVersion: 'vexlife-prod.localization-identity-audit/v1',
  source: {
    lens: relative(lensFile),
    stringSources: stringsFiles.map(relative),
    vexWeb: relative(vexWebFile),
    markup: relative(markupFile),
  },
  summary: {
    lensElementCount: elements.length,
    localizationLensEdgeCount: keysByLensRef.size,
    addressableHumanElementWithoutLensKeyCount: addressableWithoutKey.length,
    familyRawDisplayCopyCount: familyDisplayCopy.length,
    findingCounts: counts,
  },
  addressableWithoutLocalizationLensEdge: addressableWithoutKey,
  markupRelationships,
  familyDisplayCopy,
  findings,
};

const out = process.argv[2] ? path.resolve(process.argv[2]) : null;
const json = JSON.stringify(report, null, 2) + '\n';
if (out) {
  fs.mkdirSync(path.dirname(out), { recursive: true });
  fs.writeFileSync(out, json);
} else {
  process.stdout.write(json);
}

if (findings.some(item => item.severity === 'BLOCK')) process.exitCode = 2;
