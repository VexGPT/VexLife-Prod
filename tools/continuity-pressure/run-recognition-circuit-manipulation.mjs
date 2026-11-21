#!/usr/bin/env node
// Recognition Circuit manipulation-pressure live suite.
// A002: every terminal path produces a digest-bound RETURN ZIP.
// Proof-only. Uses merged Foundation v0.6.19 as-is.
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
const expectedSourceHead = args['expected-source-head'] || null;
const downloads = path.join(os.homedir(), 'Downloads');
const root = fs.mkdtempSync(path.join(downloads, '.vexlife-recognition-circuit-pr508-a002.'));
const retained = path.join(root, 'retained');
const work = path.join(root, 'work');
fs.mkdirSync(retained, { recursive: true });
fs.mkdirSync(work, { recursive: true });

const execution = {
  schemaVersion: 'vexlife-prod.recognition-circuit-manipulation-pressure-execution/v1',
  attemptRef: 'PR508-A002-FULL-RESIDENCY-MANIPULATION',
  disposition: 'RUNNING',
  stopStage: null,
  failure: null,
  sourceSha: null,
  expectedSourceHead,
  sourceCleanAtStart: null,
  sourceProofPassed: false,
  completedCases: 0,
  expectedCases: scenario.sets.length,
  completedTurns: 0,
  expectedTurns: scenario.sets.reduce((n, s) => n + s.turns.length, 0),
  allCasesResident: null,
};

const summaries = [];

function runCaptured(name, command, commandArgs, options = {}) {
  const result = spawnSync(command, commandArgs, {
    cwd: options.cwd || repo,
    encoding: 'utf8',
    maxBuffer: 64 * 1024 * 1024,
    timeout: options.timeoutMs || 30 * 60 * 1000,
    env: { ...process.env, ...(options.env || {}) },
  });
  fs.writeFileSync(path.join(retained, name + '.stdout.log'), result.stdout || '');
  fs.writeFileSync(path.join(retained, name + '.stderr.log'), result.stderr || '');
  return result;
}

function fail(stage, message, detail = null) {
  execution.disposition = 'SAFE_FAILURE';
  execution.stopStage = stage;
  execution.failure = { message, detail };
  process.exitCode = 2;
}

function recursiveFiles(dir, prefix = '') {
  const out = [];
  for (const name of fs.readdirSync(dir).sort()) {
    const abs = path.join(dir, name);
    const rel = path.join(prefix, name).replaceAll(path.sep, '/');
    const stat = fs.statSync(abs);
    if (stat.isDirectory()) out.push(...recursiveFiles(abs, rel));
    else if (stat.isFile()) out.push({ abs, rel, bytes: stat.size });
  }
  return out;
}

function sha256File(file) {
  return crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex');
}

function packageReturn() {
  try {
    fs.copyFileSync(scenarioFile, path.join(retained, 'scenario.json'));
    fs.writeFileSync(path.join(retained, 'suite-summary.json'), JSON.stringify({
      schemaVersion: 'vexlife-prod.recognition-circuit-manipulation-pressure-result/v1',
      attemptRef: execution.attemptRef,
      sourceSha: execution.sourceSha,
      foundationVersion: '0.6.19',
      experimentEffectiveInputTokenBudget: 192000,
      hardTokenLimit: 208000,
      retrievalPressureIntended: false,
      residencyInvariant: 'every prior completed exchange remains selected; no working-continuity rollover',
      sourceBasis: {
        recognitionCircuitRepository: 'vgong24/Vextreme',
        recognitionCircuitCommit: '1a0f161b4175e83c223e57642d08501004cdb9fd',
        recognitionCircuitPath: 'pages/recognition-circuit.html',
        relationship: 'synthetic mechanics only; historical attribution is not copied into the test',
      },
      caseCount: summaries.length,
      expectedCaseCount: scenario.sets.length,
      turnCount: summaries.reduce((n, x) => n + x.completedTurnCount, 0),
      expectedTurnCount: execution.expectedTurns,
      allCasesResident: execution.allCasesResident,
      semanticReviewRequired: execution.disposition === 'PASS_EXECUTION_REVIEW_SEMANTICS',
      cases: summaries,
    }, null, 2) + '\n');
    fs.writeFileSync(path.join(retained, 'execution-state.json'), JSON.stringify(execution, null, 2) + '\n');

    const preManifest = recursiveFiles(retained).filter(x => x.rel !== 'return-manifest.json');
    const manifest = {
      schemaVersion: 'vexlife-prod.recognition-circuit-manipulation-pressure-return-manifest/v1',
      attemptRef: execution.attemptRef,
      disposition: execution.disposition,
      stopStage: execution.stopStage,
      sourceSha: execution.sourceSha,
      expectedSourceHead,
      files: Object.fromEntries(preManifest.map(x => [x.rel, { sha256: sha256File(x.abs), bytes: x.bytes }])),
    };
    fs.writeFileSync(path.join(retained, 'return-manifest.json'), JSON.stringify(manifest, null, 2) + '\n');

    const tempZip = path.join(downloads, 'RETURN-vexlife-prod-recognition-circuit-manipulation-pressure-pr508-a002.zip');
    try { fs.unlinkSync(tempZip); } catch {}
    const zipped = spawnSync('/usr/bin/zip', ['-qry', tempZip, '.'], { cwd: retained, encoding: 'utf8' });
    if (zipped.status !== 0) throw new Error('zip failed: ' + (zipped.stderr || 'unknown'));
    const digest = sha256File(tempZip);
    const finalZip = path.join(downloads, 'RETURN-vexlife-prod-recognition-circuit-manipulation-pressure-pr508-a002--sha256-' + digest + '.zip');
    try { fs.unlinkSync(finalZip); } catch {}
    fs.renameSync(tempZip, finalZip);
    console.log(JSON.stringify({
      state: execution.disposition,
      stopStage: execution.stopStage,
      sourceSha: execution.sourceSha,
      sourceProofPassed: execution.sourceProofPassed,
      completedCases: execution.completedCases,
      expectedCases: execution.expectedCases,
      completedTurns: execution.completedTurns,
      expectedTurns: execution.expectedTurns,
      allCasesResident: execution.allCasesResident,
      zip: finalZip,
      sha256: digest,
    }, null, 2));
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
}

try {
  const git = spawnSync('/usr/bin/git', ['rev-parse', 'HEAD'], { cwd: repo, encoding: 'utf8' });
  execution.sourceSha = git.status === 0 ? git.stdout.trim() : null;
  if (!execution.sourceSha || !/^[0-9a-f]{40}$/.test(execution.sourceSha)) {
    fail('SOURCE_HEAD_VALIDATION', 'exact checkout source HEAD unavailable');
  } else if (expectedSourceHead && execution.sourceSha !== expectedSourceHead) {
    fail('SOURCE_HEAD_VALIDATION', 'checkout HEAD does not match launcher-resolved PR head', {
      expectedSourceHead,
      observedSourceHead: execution.sourceSha,
    });
  }

  if (execution.disposition === 'RUNNING') {
    const dirty = spawnSync('/usr/bin/git', ['status', '--porcelain=v1', '-uall'], { cwd: repo, encoding: 'utf8' });
    execution.sourceCleanAtStart = dirty.status === 0 && dirty.stdout.trim() === '';
    fs.writeFileSync(path.join(retained, 'source-status.txt'), dirty.stdout || '');
    if (!execution.sourceCleanAtStart) {
      fail('SOURCE_CLEANLINESS', 'proof checkout is not clean at start');
    }
  }

  const gradleCommon = ['./gradlew', '--no-daemon', '--max-workers=1', '-Pkotlin.compiler.execution.strategy=in-process', '-Dorg.gradle.jvmargs=-Xmx4g', '--console=plain'];

  if (execution.disposition === 'RUNNING') {
    console.log('Recognition Circuit A002: running full JVM source proof...');
    const sourceProof = runCaptured('source-proof', gradleCommon[0], [...gradleCommon.slice(1), 'jvmTest']);
    if (sourceProof.status !== 0) {
      fail('SOURCE_PROOF', 'jvmTest source proof failed', { exitCode: sourceProof.status });
    } else {
      execution.sourceProofPassed = true;
    }
  }

  if (execution.disposition === 'RUNNING') {
    for (let index = 0; index < scenario.sets.length; index += 1) {
      const set = scenario.sets[index];
      const slug = set.setRef.replace(/[^a-zA-Z0-9._-]+/g, '-');
      console.log('Recognition Circuit A002: case ' + (index + 1) + '/' + scenario.sets.length + ' ' + set.setRef);
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
        '--source-sha',execution.sourceSha,
        '--source-clean','true',
        '--runtime-home',runtimeHome,
        '--experiment-window',
        '--runtime-start-at-bind',
        '--payload-scale','1',
      ];
      const caseRun = runCaptured(
        'case-' + slug,
        './gradlew',
        [...gradleCommon.slice(1), '--offline', '-q', 'continuityPressure', '--args=' + harnessArgs.join(' ')],
        { timeoutMs: 30 * 60 * 1000 },
      );
      const resultPath = path.join(resultDir, 'result.json');
      if (caseRun.status !== 0) {
        fail('LIVE_CASE', set.setRef + ' Gradle/harness execution failed', { exitCode: caseRun.status, resultPresent: fs.existsSync(resultPath) });
        break;
      }
      if (!fs.existsSync(resultPath)) {
        fail('LIVE_CASE', 'missing result.json for ' + set.setRef);
        break;
      }
      const result = JSON.parse(fs.readFileSync(resultPath, 'utf8'));
      const turns = Array.isArray(result.turns) ? result.turns : [];
      if (result.status === 'ERROR' || turns.length !== set.turns.length) {
        fail('LIVE_CASE', set.setRef + ' did not complete all semantic turns', { status: result.status, completedTurns: turns.length, expectedTurns: set.turns.length });
        break;
      }
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
      execution.completedCases += 1;
      execution.completedTurns += turns.length;
    }
  }

  if (execution.disposition === 'RUNNING') {
    execution.allCasesResident = summaries.length === scenario.sets.length && summaries.every(x => x.validForFoundationJudgment);
    if (!execution.allCasesResident) {
      execution.disposition = 'INVALID_RESIDENCY';
      execution.stopStage = 'RESIDENCY_REVIEW';
      process.exitCode = 2;
    } else {
      execution.disposition = 'PASS_EXECUTION_REVIEW_SEMANTICS';
      execution.stopStage = null;
      process.exitCode = 0;
    }
  }
} catch (error) {
  if (execution.disposition === 'RUNNING') {
    fail('UNCAUGHT', error instanceof Error ? error.message : String(error));
  }
} finally {
  packageReturn();
}
