#!/usr/bin/env node
// Claude lane's command for the shared VexWork continuity-pressure harness (VexWork #310).
//
//   node tools/continuity-pressure/run.mjs --stage <projection|analyze|pressure|probe|quick|full>
//     --scenario-ref <ref> --scenario-file <path> --result-dir <dir>
//     [--pressure-strategy EFFECTIVE_SELECTION_BUDGET|PAYLOAD_PRESSURE|NATIVE_LIMIT]
//     [--effective-input-token-budget N] [--sets N] [--turns-per-set N]
//
// It checks the scenario against the shared scenario digests, binds the exact source head, and
// runs the test-scope Kotlin adapter through Gradle. Live stages return HELD (exit 3) in this
// revision. Nothing here touches a person's Home: the adapter forms and deletes a scratch Home.
//
// [VXG RealForever]
import { spawnSync } from 'node:child_process';
import crypto from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

// The shared scenarios this adapter binds, by ref, at their VexWork #310 digests (0fc67824).
const SHARED_SCENARIOS = {
  'scenario.vexlife.continuity-pressure.depth-continuity.v1':
    '65c81bfdcc0935d261204566bfc05468317c59e962eabceffb9f7e0fca359cc1',
  'scenario.vexlife.continuity-pressure.current-state-placement.v1':
    'd9d86dc1195123dc7565c3173d909739e90dc017c3a027b9b2c2edd13c0a72f1',
};
const STAGES = new Set(['projection', 'analyze', 'pressure', 'probe', 'quick', 'full']);

const repo = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..');
const argv = process.argv.slice(2);
const args = {};
for (let i = 0; i < argv.length; i += 1) {
  const key = argv[i];
  if (!key.startsWith('--')) throw new Error('arguments are --key value: ' + key);
  const next = argv[i + 1];
  if (next !== undefined && !next.startsWith('--')) { args[key.slice(2)] = next; i += 1; }
  else args[key.slice(2)] = 'true';
}
for (const key of ['stage', 'scenario-ref', 'scenario-file', 'result-dir']) {
  if (!args[key]) throw new Error('--' + key + ' is required');
}
if (!STAGES.has(args.stage)) throw new Error('unknown stage: ' + args.stage);
const expectedSha = SHARED_SCENARIOS[args['scenario-ref']];
if (!expectedSha) throw new Error('only the shared scenarios are bound: ' + Object.keys(SHARED_SCENARIOS).join(', '));

const scenarioFile = path.resolve(args['scenario-file']);
const scenarioSha = crypto.createHash('sha256').update(fs.readFileSync(scenarioFile)).digest('hex');
if (scenarioSha !== expectedSha) {
  throw new Error('scenario digest ' + scenarioSha + ' is not the shared ' + expectedSha);
}

const git = spawnSync('git', ['rev-parse', 'HEAD'], { cwd: repo, encoding: 'utf8' });
const sourceSha = git.status === 0 ? git.stdout.trim() : 'UNKNOWN';
const dirty = spawnSync('git', ['status', '--porcelain', '--untracked-files=no'], { cwd: repo, encoding: 'utf8' });
const sourceClean = dirty.status === 0 && dirty.stdout.trim() === '';

const forwarded = ['stage', 'scenario-ref', 'pressure-strategy', 'effective-input-token-budget', 'sets', 'turns-per-set']
  .filter((key) => args[key] !== undefined)
  .flatMap((key) => ['--' + key, args[key]]);
forwarded.push(
  '--scenario-file', scenarioFile,
  '--result-dir', path.resolve(args['result-dir']),
  '--source-sha', sourceSha,
  '--source-clean', String(sourceClean),
);

const gradle = spawnSync('./gradlew', ['-q', '--console=plain', 'continuityPressure', '--args=' + forwarded.join(' ')], {
  cwd: repo,
  stdio: 'inherit',
});
// Gradle wraps a non-zero adapter exit as its own failure; restore the adapter's meaning from
// what it wrote: 3 = live stage HELD on this host, 2 = projection ERROR result.
const resultDir = path.resolve(args['result-dir']);
let code = gradle.status ?? 1;
if (code !== 0 && fs.existsSync(path.join(resultDir, 'held.json'))) code = 3;
else if (code !== 0 && fs.existsSync(path.join(resultDir, 'result.json'))) {
  const status = JSON.parse(fs.readFileSync(path.join(resultDir, 'result.json'), 'utf8')).status;
  if (status === 'ERROR') code = 2;
}
process.exitCode = code;
