// The JavaScript Families must reproduce every outcome the Kotlin Families recorded.
// Source of truth: generated/family-conformance.json (written by FamilyConformanceGoldenTest.kt).
// [VXG RealForever]
import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

import { BrowserContext, CompanionRuntime, Composer, ConversationTurn } from '../public/families.mjs';

const HERE = path.dirname(fileURLToPath(import.meta.url));
const table = JSON.parse(fs.readFileSync(path.join(HERE, '..', 'generated', 'family-conformance.json'), 'utf8'));

const families = { CompanionRuntime, ConversationTurn, BrowserContext };

function plain(value) {
  return JSON.parse(JSON.stringify(value));
}

function actual(testCase) {
  if (testCase.family === 'Composer') {
    return Composer.converge(testCase.companion, testCase.turn, testCase.browser);
  }
  const family = families[testCase.family];
  switch (testCase.kind) {
    case 'act': {
      const result = family.act(testCase.state, testCase.control);
      return {
        accepted: result !== null,
        admittedState: result?.admittedState ?? null,
        effect: result?.effect ?? null
      };
    }
    case 'interpret':
      return family.interpret(testCase.state, testCase.observation);
    case 'display':
      return family.display(testCase.state);
    case 'modelLabel':
      return CompanionRuntime.modelLabel(testCase.input);
    case 'parsePageAddress':
      return BrowserContext.parsePageAddress(testCase.input);
    case 'admitPageAddress':
      return BrowserContext.admitPageAddress(testCase.input);
    default:
      throw new Error(`unknown case kind ${testCase.kind}`);
  }
}

test('conformance table is complete and well formed', () => {
  assert.equal(table.schemaVersion, 'vexlife-prod.exploration.family-conformance/v1');
  assert.equal(table.cases.length, table.caseCount);
  assert.ok(table.caseCount > 800, 'the lattice should cover every Family');
});

test('every Kotlin outcome is reproduced by the JavaScript Families', () => {
  const failures = [];
  table.cases.forEach((testCase, index) => {
    try {
      assert.deepStrictEqual(plain(actual(testCase)), testCase.expected);
    } catch (error) {
      failures.push({ index, family: testCase.family, kind: testCase.kind, message: error.message.split('\n').slice(0, 12).join('\n') });
    }
  });
  assert.equal(failures.length, 0, `${failures.length} conformance case(s) differ:\n${JSON.stringify(failures.slice(0, 5), null, 2)}`);
});
