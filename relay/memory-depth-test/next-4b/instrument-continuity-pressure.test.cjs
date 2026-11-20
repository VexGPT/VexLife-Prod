'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');

const instrument = require('./instrument-continuity-pressure.cjs');

const repoRoot = path.resolve(__dirname, '..', '..', '..');
const sourcePath = path.join(
  repoRoot,
  'src',
  'jvmTest',
  'kotlin',
  'vexlife',
  'harness',
  'ContinuityPressureAdapter.kt',
);

test('exact #403 adapter source is the admitted instrumentation input', () => {
  const source = fs.readFileSync(sourcePath);
  assert.equal(instrument.gitBlobSha(source), instrument.EXPECTED_SOURCE_GIT_BLOB);
});

test('instrumentation adds one atomic completed-turn checkpoint without changing source bytes', () => {
  const before = fs.readFileSync(sourcePath, 'utf8');
  const output = instrument.instrumentSource(before);
  const after = fs.readFileSync(sourcePath, 'utf8');

  assert.equal(after, before);
  assert.notEqual(output, before);
  assert.equal(
    output.split(instrument.PROGRESS_SCHEMA).length - 1,
    1,
    'progress schema should be introduced once',
  );
  assert.equal(
    output.split('writeProgressCheckpoint(').length - 1,
    2,
    'one function definition + one completed-turn call expected',
  );
  assert.match(output, /ATOMIC_MOVE/);
  assert.match(output, /AFTER_EACH_COMPLETED_TURN|already-completed observed turns/);
  assert.match(output, /completedTurnCount/);
  assert.match(output, /lastCompletedOrdinal/);
  assert.match(output, /provider\.evidence\.runtimeReceiptRef/);
  assert.match(output, /put\("turns", json\.encodeToJsonElement\(turns\)\)/);
  assert.match(output, /put\("summary", summary\(turns\)\)/);
});

test('instrumentation refuses source drift and double application', () => {
  const source = fs.readFileSync(sourcePath, 'utf8');
  assert.throws(
    () => instrument.instrumentSource(source.replace('object ContinuityPressureAdapter', 'object DriftedAdapter')),
    /SOURCE_BLOB_MISMATCH/,
  );
  const once = instrument.instrumentSource(source);
  assert.throws(
    () => instrument.instrumentSource(once),
    /SOURCE_BLOB_MISMATCH|ALREADY_INSTRUMENTED/,
  );
});
