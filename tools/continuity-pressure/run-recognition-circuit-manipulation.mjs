#!/usr/bin/env node
// Recognition Circuit manipulation-pressure live suite.
// Proof-only. Uses merged Foundation v0.6.19 as-is and maximizes experiment input residency.
// [VXG RealForever]
import { spawnSync } from 'node:child_process';
import crypto from 'node:crypto';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const repo = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..');
const scenarioFile = path.join(repo, 'tools', 'continuity-pressure', 'scenarios', 'lane', 'recognition-circuit-manipulation-extremes-v1.json');
const scenario = JSON.parse(fs.readFileSync(scenarioFile, 'utf8'));
const argv = process.argv.slice(2);
const args = {};
for (let i = 0; i < argv.length; i += 1) {
  const key = argv[i];
  if (!key.startsWith('--')) throw new Error('arguments are --key value: ' + key);
  const next = argv[i + 1];
  if (next !== undefined && !next.startsWith('--')) { args[key.slice(2)] = next; i += 1; }
  else args[key.slice(2)] = 'true';
}
const runtimeHome = path.resolve(args['runtime-home'] || path.join(os.homedir(), '.vexlife'));
const downloads = path.join(os.homedir(), 'Downloads');
const sourceSha = spawnSync('/usr/bin/git', ['rev-parse', 'HEAD'], { cwd: repo, encoding: 'utf8' }).stdout.trim();
if (!/^[0-9a-f]{40}$/.test(sourceSha)) throw new Error('exact source HEAD unavailable');
const dirty = spawnSync('/usr/bin/git', ['status', '--porcelain=v1', '-uall'], { cwd: repo, encoding: 'utf8' });
if (dirty.status !== 0 || dirty.stdout.trim() !== '') throw new Error('recognition-circuit proof checkout must be clean');
const root = fs.mkdtempSync(path.join(downloads, '.vexlife-recognition-circuit-manipulation.'));
const retained = path.join(root, 'retained');
const work = path.join(root, 'work');
fs.mkdirSync(retained, { recursive: true });
fs.mkdirSync(work, { recursive: true });

function run(name, command, commandArgs, options = {}) {
  const result = spawnSync(command, commandArgs, {
    cwd: options.cwd || repo,
    encoding: 'utf8',
    maxBuffer: 64 * 1024 * 1024,
    timeout: options.timeoutMs || 30 * 60 * 1000,
    env: { ...process.env, ...(options.env || {}) },
  });
  fs.writeFileSync(path.join(retained, name + '.stdout.log'), result.stdout || '');
  fs.writeFileSync(path.join(retained, name + '.stderr.log'), result.stderr || '');
  if (result.status !== 0) {
    const error = new Error(name + ' failed with exit ' + result.status);
    error.result = result;
    throw error;
  }
  return result;
}

const gradleCommon = ['./gradlew', '--no-daemon', '--max-workers=1', '-Pkotlin.compiler.execution.strategy=in-process', '-Dorg.gradle.jvmargs=-Xmx4g', '--console=plain'];
run('source-proof', gradleCommon[0], [...gradleCommon.slice(1), 'jvmTest']);

const summaries = [];
for (let index = 0; index < scenario.sets.length; index += 1) {
  const set = scenario.sets[index];
  const slug = set.setRef.replace(/[^a-zA-Z0-9._-]+/g, '-');
  const single = {
    ...scenario,
    scenarioRef: scenario.scenarioRef + '.' + slug,
    label: scenario.label + ' / ' + set.setRef,
    sets: [set],
  };
  const oneFile = path.join(work, slug + '.json');
  fs.writeFileSync(oneFile, JSON.stringify(single, null, 2) + '\n');
  const resultDir = path.join(retained, 'cases', slug);
  fs.mkdirSync(resultDir, { recursive: true });
  const harnessArgs = [
    '--stage','full',
    '--scenario-ref',single.scenarioRef,
    '--scenario-file',oneFile,
    '--result-dir',resultDir,
    '--pressure-strategy','EFFECTIVE_SELECTION_BUDGET',
    '--effective-input-token-budget','192000',
    '--sets','1',
    '--turns-per-set',String(set.turns.length),
    '--source-sha',sourceSha,
    '--source-clean','true',
    '--runtime-home',runtimeHome,
    '--experiment-window',
    '--runtime-start-at-bind',
    '--payload-scale','1',
  ];
  run('case-' + slug, './gradlew', [...gradleCommon.slice(1), '--offline', '-q', 'continuityPressure', '--args=' + harnessArgs.join(' ')], { timeoutMs: 30 * 60 * 1000 });
  const resultPath = path.join(resultDir, 'result.json');
  if (!fs.existsSync(resultPath)) throw new Error('missing result.json for ' + set.setRef);
  const result = JSON.parse(fs.readFileSync(resultPath, 'utf8'));
  const turns = Array.isArray(result.turns) ? result.turns : [];
  if (turns.length !== set.turns.length) throw new Error(set.setRef + ' completed ' + turns.length + ' of ' + set.turns.length + ' turns');
  const residency = turns.map((turn, turnIndex) => {
    const prompt = turn.prompt || {};
    const prior = Number(prompt.priorCompletedExchangeCount ?? turnIndex);
    const selected = Array.isArray(prompt.selectedConversationEventRefs) ? prompt.selectedConversationEventRefs : [];
    const allPriorResident = selected.length === prior * 2;
    const noRollover = prompt.workingContinuityIncluded !== true && prompt.contextMode !== 'WORKING_CHECKPOINT_SUCCESSOR';
    const fullBudget = Number(prompt.effectiveInputTokenBudget) === 192000;
    return {
      turnRef: turn.scenarioTurnRef || set.turns[turnIndex].turnRef,
      priorCompletedExchangeCount: prior,
      selectedConversationEventCount: selected.length,
      contextMode: prompt.contextMode || null,
      workingContinuityIncluded: prompt.workingContinuityIncluded === true,
      effectiveInputTokenBudget: prompt.effectiveInputTokenBudget ?? null,
      providerMaterializedInputTokenEstimate: prompt.providerMaterializedInputTokenEstimate ?? null,
      allPriorResident,
      noRollover,
      fullBudget,
      valid: allPriorResident && noRollover && fullBudget,
    };
  });
  const validForFoundationJudgment = residency.every(x => x.valid);
  summaries.push({
    setRef: set.setRef,
    kind: set.kind,
    completedTurnCount: turns.length,
    validForFoundationJudgment,
    residency,
    responses: turns.map((turn, i) => ({
      turnRef: turn.scenarioTurnRef || set.turns[i].turnRef,
      responseText: turn.responseText || null,
    })),
  });
}

const foundation = JSON.parse(fs.readFileSync(path.join(repo, 'src', 'jvmMain', 'resources', 'foundational', 'vex-foundational-field.json'), 'utf8'));
const suite = {
  schemaVersion:'vexlife-prod.recognition-circuit-manipulation-pressure-result/v1',
  sourceSha,
  foundationVersion:foundation.version,
  experimentEffectiveInputTokenBudget:192000,
  hardTokenLimit:208000,
  retrievalPressureIntended:false,
  residencyInvariant:'every prior completed exchange must remain selected; no working-continuity rollover',
  sourceBasis:{
    recognitionCircuitRepository:'vgong24/Vextreme',
    recognitionCircuitCommit:'1a0f161b4175e83c223e57642d08501004cdb9fd',
    recognitionCircuitPath:'pages/recognition-circuit.html',
    relationship:'synthetic mechanics derived from pattern literacy; historical attribution is not copied into the synthetic cases',
  },
  caseCount:summaries.length,
  turnCount:summaries.reduce((n, x) => n + x.completedTurnCount, 0),
  allCasesResident:summaries.every(x => x.validForFoundationJudgment),
  semanticReviewRequired:true,
  cases:summaries,
};
fs.writeFileSync(path.join(retained, 'suite-summary.json'), JSON.stringify(suite, null, 2) + '\n');
fs.copyFileSync(scenarioFile, path.join(retained, 'scenario.json'));

const zipName = 'RETURN-vexlife-prod-recognition-circuit-manipulation-pressure-' + sourceSha.slice(0, 12) + '.zip';
const zipPath = path.join(downloads, zipName);
const zipped = spawnSync('/usr/bin/zip', ['-qry', zipPath, '.'], { cwd: retained, encoding: 'utf8' });
if (zipped.status !== 0) throw new Error('zip failed: ' + (zipped.stderr || 'unknown'));
const digest = crypto.createHash('sha256').update(fs.readFileSync(zipPath)).digest('hex');
console.log(JSON.stringify({
  state:suite.allCasesResident ? 'PASS_EXECUTION_REVIEW_SEMANTICS' : 'INVALID_RESIDENCY',
  sourceSha,
  foundationVersion:foundation.version,
  cases:suite.caseCount,
  turns:suite.turnCount,
  allCasesResident:suite.allCasesResident,
  zip:zipPath,
  sha256:digest,
}, null, 2));
