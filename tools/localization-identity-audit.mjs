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
const runtimeFile = path.join(root, 'src/commonMain/kotlin/vexlife/web/VexLifeWebP1RuntimeArtifacts.kt');
const topologyFile = path.join(root, 'src/jvmMain/kotlin/vexlife/model/heretic/runtime/HereticExecutionTopology.kt');
const labsFile = path.join(root, 'src/jvmMain/kotlin/vexlife/labs/VexLabsNativeTest.kt');
const serviceContractFile = path.join(root, 'src/commonMain/kotlin/vexlife/service/VexLifeProdServiceContract.kt');
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
const technicalLiteral = text =>
  /^[A-Z0-9][A-Z0-9_.:/-]*$/.test(text);
const human = text =>
  /[A-Za-z]{2}/.test(text) &&
  text !== 'Vex' &&
  text !== 'VexLife' &&
  !localizationKey.test(text) &&
  !technicalLiteral.test(text);

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

// Visible literal text nodes are audited even when the child has no data-vex-ref of its own.
// That is the path that catches anonymous nested presentation before somebody attaches its words to
// a convenient parent merely to satisfy localization.
const htmlStart = markup.indexOf('<!doctype html>');
const htmlEnd = markup.lastIndexOf('</html>');
const html = htmlStart >= 0 && htmlEnd >= htmlStart ? markup.slice(htmlStart, htmlEnd + 7) : markup;
const hardcodedTextNodes = [];
for (const match of html.matchAll(/>([^<>{}$]*[A-Za-z][^<>{}$]*)</g)) {
  const literal = match[1].replace(/\\s+/g, ' ').trim();
  if (!literal || !human(literal)) continue;
  const open = html.lastIndexOf('<', match.index);
  const tag = open >= 0 ? html.slice(open, match.index + 1) : '';
  if (/data-vex-text\\s*=/.test(tag)) continue;
  const evidence = {
    path: relative(markupFile),
    line: lineAt(markup, htmlStart + match.index),
    literal,
  };
  hardcodedTextNodes.push(evidence);
  finding('HIGH', 'MARKUP_HUMAN_TEXT_LITERAL', evidence);
}

// Dynamic presentation must obey the same law. Scan only concrete browser presentation sinks; protocol
// IDs, CSS classes, paths and source data are not localization findings merely because they are strings.
const runtime = fs.readFileSync(runtimeFile, 'utf8');
const runtimeHumanLiterals = [];
const runtimePatterns = [
  ['RUNTIME_TEXT_CONTENT_LITERAL', /\\.textContent\\s*=\\s*(['"])([^'"\\n]*[A-Za-z][^'"\\n]*)\\1/g],
  ['RUNTIME_TITLE_LITERAL', /\\.title\\s*=\\s*(['"])([^'"\\n]*[A-Za-z][^'"\\n]*)\\1/g],
  ['RUNTIME_PLACEHOLDER_LITERAL', /\\.placeholder\\s*=\\s*(['"])([^'"\\n]*[A-Za-z][^'"\\n]*)\\1/g],
  ['RUNTIME_ARIA_LITERAL', /setAttribute\\(\\s*['"]aria-label['"]\\s*,\\s*(['"])([^'"\\n]*[A-Za-z][^'"\\n]*)\\1/g],
  ['RUNTIME_NOTIFY_LITERAL', /notify\\(\\s*(['"])([^'"\\n]*[A-Za-z][^'"\\n]*)\\1/g],
  ['RUNTIME_CONFIRM_LITERAL', /window\\.confirm\\(\\s*(['"])([^'"\\n]*[A-Za-z][^'"\\n]*)\\1/g],
  ['RUNTIME_PROMPT_LITERAL', /window\\.prompt\\(\\s*(['"])([^'"\\n]*[A-Za-z][^'"\\n]*)\\1/g],
  ['RUNTIME_ERROR_LITERAL', /throw new Error\\(\\s*(['"])([^'"\\n]*[A-Za-z][^'"\\n]*)\\1/g],
  ['RUNTIME_HOLD_LITERAL', /holdText\\s*:\\s*(['"])([^'"\\n]*[A-Za-z][^'"\\n]*)\\1/g],
];
for (const [kind, pattern] of runtimePatterns) {
  for (const match of runtime.matchAll(pattern)) {
    const literal = match[2].trim();
    if (!human(literal)) continue;
    const evidence = {
      path: relative(runtimeFile),
      line: lineAt(runtime, match.index),
      literal,
    };
    runtimeHumanLiterals.push({ kind, ...evidence });
    finding('HIGH', kind, evidence);
  }
}
// contextLine/contextEvidenceDisclosure are presentation helpers whose human arguments may be multiline.
for (const match of runtime.matchAll(/(?:contextLine|contextEvidenceDisclosure)\\([\\s\\S]{0,900}?\\);/g)) {
  const call = match[0];
  for (const literalMatch of call.matchAll(/(['"])([^'"\\n]*[A-Za-z][^'"\\n]*)\\1/g)) {
    const literal = literalMatch[2].trim();
    if (!human(literal)) continue;
    // Technical refs/states are data, not copy.
    if (/^[a-z0-9._:/-]+$/i.test(literal) && !/\\s/.test(literal)) continue;
    const evidence = {
      path: relative(runtimeFile),
      line: lineAt(runtime, match.index + literalMatch.index),
      literal,
    };
    runtimeHumanLiterals.push({ kind: 'RUNTIME_CONTEXT_HELPER_LITERAL', ...evidence });
    finding('HIGH', 'RUNTIME_CONTEXT_HELPER_LITERAL', evidence);
  }
}

// Upstream human-relationship types must remain keyed before they enter the Lab wire.
const keyedHumanRelationshipSources = [
  [topologyFile, ['val label: VexLifeText', 'val explanation: VexLifeText', 'val increaseConsequence: VexLifeText?', 'val decreaseConsequence: VexLifeText?', 'val unit: VexLifeText?']],
  [serviceContractFile, ['data class VexLabsField', 'val label: VexLifeText', 'val explanation: VexLifeText', 'val labEditDetail: VexLifeText', 'val unit: VexLifeText?']],
  [labsFile, ['workspace.label.execution-field-context-profile-input-token-budget', 'workspace.description.vex-labs-edit-context-profile-active']],
];
for (const [file, required] of keyedHumanRelationshipSources) {
  const source = fs.readFileSync(file, 'utf8');
  for (const token of required) {
    if (!source.includes(token)) {
      finding('BLOCK', 'UPSTREAM_HUMAN_RELATIONSHIP_NOT_KEYED', {
        path: relative(file),
        missing: token,
      });
    }
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
    const tailFromLiteral = region.slice(match.index, match.index + match[0].length + 12);
    if (/^"[^"]+"\s+to\b/.test(tailFromLiteral)) continue;
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
    runtime: relative(runtimeFile),
    topology: relative(topologyFile),
    labs: relative(labsFile),
    serviceContract: relative(serviceContractFile),
  },
  summary: {
    lensElementCount: elements.length,
    localizationLensEdgeCount: keysByLensRef.size,
    addressableHumanElementWithoutLensKeyCount: addressableWithoutKey.length,
    familyRawDisplayCopyCount: familyDisplayCopy.length,
    markupHumanTextLiteralCount: hardcodedTextNodes.length,
    runtimeHumanLiteralCount: runtimeHumanLiterals.length,
    findingCounts: counts,
  },
  addressableWithoutLocalizationLensEdge: addressableWithoutKey,
  markupRelationships,
  familyDisplayCopy,
  markupHumanTextLiterals: hardcodedTextNodes,
  runtimeHumanLiterals,
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
