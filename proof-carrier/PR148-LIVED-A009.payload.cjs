#!/usr/bin/env node
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const os = require('node:os');
const crypto = require('node:crypto');
const { spawnSync } = require('node:child_process');

const RESULT_SCHEMA = 'vextreme.vexlocalbridge-mac-payload-result/v1';
const ORIGIN = 'http://127.0.0.1:18210';
const POLL_MS = 1200;
const TOTAL_BOUND_MS = 650000;
const SETUP_BOUND_MS = 300000;

function sha256(value) {
  return crypto.createHash('sha256').update(value).digest('hex');
}

function defaultEffects() {
  return {
    sourceMutationPerformed: false,
    branchMutationPerformed: false,
    claimMutationPerformed: false,
    prMetadataMutationPerformed: false,
    reviewPerformed: false,
    approvalPerformed: false,
    readyTransitionPerformed: false,
    mergePerformed: false,
    mainMutationPerformed: false,
    checkpointMutationPerformed: false,
    forcePushPerformed: false,
    historyRewritePerformed: false,
    hostInstallationPerformed: false,
    modelRuntimePerformed: false,
    providerOrNetworkEffectPerformed: false,
    publicationPerformed: false,
    credentialSerializationPerformed: false,
    filesystemOutsideQualifiedRootsPerformed: false,
  };
}

function failureRecord(error, stage) {
  const summary = String(error?.message || error || 'unknown lived failure').slice(0, 1200);
  return {
    failureFamily: 'VEXLIFE_PROD_REPAIRED_STARTUP_LIVED',
    failureFingerprint: sha256(Buffer.from(stage + '\n' + summary)),
    stage,
    summary,
    evidenceRef: 'payload-result.json',
  };
}

function writeResult(value) {
  const root = process.env.VEXLOCALBRIDGE_RESULT_DIR;
  if (!root) throw new Error('VEXLOCALBRIDGE_RESULT_DIR is unavailable');
  fs.mkdirSync(root, { recursive: true });
  fs.writeFileSync(
    path.join(root, 'payload-result.json'),
    JSON.stringify({ schemaVersion: RESULT_SCHEMA, acceptedExitCodes: [0], ...value }, null, 2) + '\n',
  );
}

function run(command, args, options = {}) {
  const result = spawnSync(command, args, {
    cwd: options.cwd,
    env: options.env || process.env,
    encoding: 'utf8',
    timeout: options.timeoutMs || 300000,
    maxBuffer: 32 * 1024 * 1024,
    stdio: options.stdio || ['ignore', 'pipe', 'pipe'],
  });
  if (result.error) throw result.error;
  if (result.status !== 0) {
    throw new Error(
      command + ' ' + args.join(' ') + ' failed exit=' + result.status +
      '\nstdout=' + String(result.stdout || '').slice(-8000) +
      '\nstderr=' + String(result.stderr || '').slice(-8000),
    );
  }
  return result;
}

async function getJson(route, timeoutMs = 5000) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const response = await fetch(ORIGIN + route, {
      method: 'GET',
      headers: { accept: 'application/json' },
      signal: controller.signal,
    });
    if (!response.ok) throw new Error(route + ' returned HTTP ' + response.status);
    return await response.json();
  } finally {
    clearTimeout(timer);
  }
}

function tryJson(route) {
  return getJson(route).catch(() => null);
}

async function waitForExactProduct(sourceSha, deadlineMs) {
  let last = null;
  while (Date.now() < deadlineMs) {
    last = await tryJson('/api/v1/source-currentness');
    if (
      last?.sourceSha === sourceSha &&
      last?.serviceState === 'SOURCE_BOUND_LISTENING' &&
      last?.effectsPerformed === false
    ) {
      return last;
    }
    await new Promise((resolve) => setTimeout(resolve, POLL_MS));
  }
  throw new Error(
    'exact Product source did not become CURRENT/listening; last=' + JSON.stringify(last),
  );
}

function chronologyFiles(homePath) {
  const dir = path.join(homePath, 'runtime/vexlife-prod/logs/chronology');
  if (!fs.existsSync(dir)) return [];
  return fs.readdirSync(dir)
    .filter((name) => name.startsWith('chronology') && name.endsWith('.jsonl'))
    .sort((a, b) => {
      if (a === 'chronology.jsonl') return 1;
      if (b === 'chronology.jsonl') return -1;
      return a.localeCompare(b);
    })
    .map((name) => path.join(dir, name));
}

function readChronology(homePath, candidateHead, startedEpochMs) {
  const records = [];
  for (const file of chronologyFiles(homePath)) {
    for (const line of fs.readFileSync(file, 'utf8').split(/\n/)) {
      if (!line.trim()) continue;
      try {
        const record = JSON.parse(line);
        const atMs = Date.parse(record.at);
        if (
          record.schemaVersion === 'vexlife-prod.chronology-record/v1' &&
          record.sourceSha === candidateHead &&
          Number.isFinite(atMs) &&
          atMs >= startedEpochMs
        ) {
          records.push(record);
        }
      } catch (_) {}
    }
  }
  records.sort((a, b) => Date.parse(a.at) - Date.parse(b.at) || a.sequence - b.sequence);
  return records;
}

function applyObservedEffects(records, effects) {
  const facts = new Set(records.map((record) => record.fact));
  if (
    facts.has('RUNTIME_START_REQUESTED') ||
    facts.has('RUNTIME_RECOVERY_REQUESTED') ||
    facts.has('RUNTIME_RECOVERY_COMPLETED') ||
    facts.has('RUNTIME_GENERATION_UNAVAILABLE') ||
    facts.has('PROVIDER_INVOCATION_REQUESTED')
  ) {
    effects.modelRuntimePerformed = true;
  }
}

function analyzeFirstTurn(records) {
  for (let sendIndex = 0; sendIndex < records.length; sendIndex += 1) {
    const send = records[sendIndex];
    if (send.fact !== 'SEND_ACCEPTED' || !send.refs?.turnRef) continue;
    const turnRef = send.refs.turnRef;
    for (let i = sendIndex + 1; i < records.length; i += 1) {
      const record = records[i];
      if (
        (record.fact === 'TURN_COMPLETED' || record.fact === 'TURN_FAILED') &&
        record.refs?.turnRef === turnRef
      ) {
        const segment = records.slice(sendIndex, i + 1);
        const providerInvocations =
          segment.filter((item) => item.fact === 'PROVIDER_INVOCATION_REQUESTED');
        const runtimeRefs = [...new Set(
          providerInvocations.map((item) => item.refs?.runtimeReceiptRef).filter(Boolean),
        )];
        return {
          state: record.fact,
          turnRef,
          terminalRefs: record.refs || {},
          providerInvocationCount: providerInvocations.length,
          runtimeReceiptRefs: runtimeRefs,
        };
      }
    }
  }
  return { state: 'WAITING_FOR_FIRST_TURN' };
}

async function main() {
  const bindingPath = process.argv[2];
  if (!bindingPath) throw new Error('candidate binding path is required');
  const binding = JSON.parse(fs.readFileSync(path.resolve(bindingPath), 'utf8'));

  const resultBase = {
    effects: defaultEffects(),
    candidateFindings: [],
    harnessFindings: [],
    environmentFindings: [],
    unknownFindings: [],
    beforeState: {},
    afterState: {},
    completedStepReceipts: [],
  };

  const startedAt = new Date();
  const totalDeadline = startedAt.getTime() + TOTAL_BOUND_MS;
  let stage = 'HOST_PREFLIGHT';
  let observedHomePath = null;
  let observedRecords = [];

  if (process.platform !== 'darwin' || process.arch !== 'arm64') {
    resultBase.environmentFindings.push('HOST_NOT_DARWIN_ARM64');
    writeResult({
      disposition: 'SAFE_FAILURE',
      stopReasonOrNull: 'HOST_NOT_DARWIN_ARM64',
      firstSubstantiveFailureOrNull:
        failureRecord(new Error('lived proof requires Apple-Silicon macOS'), 'HOST_PREFLIGHT'),
      ...resultBase,
    });
    return;
  }

  resultBase.beforeState.sourceCurrentness = await tryJson('/api/v1/source-currentness');
  resultBase.beforeState.startedAt = startedAt.toISOString();
  resultBase.beforeState.candidateHead = binding.head;
  resultBase.beforeState.candidateTree = binding.tree;

  const work = fs.mkdtempSync(path.join(process.env.TMPDIR || os.tmpdir(), 'vexlife-prod-pr148-repaired-'));
  const repo = path.join(work, 'candidate');
  const app = path.join(work, 'VexLife Prod Setup.app');

  try {
    stage = 'EXACT_SOURCE_AND_IMAGE';
    resultBase.effects.providerOrNetworkEffectPerformed = true;

    run('/usr/bin/git', ['clone', '--no-checkout', 'https://github.com/' + binding.repository + '.git', repo], {
      timeoutMs: 180000,
    });
    run('/usr/bin/git', ['-C', repo, 'checkout', '--detach', binding.head], { timeoutMs: 60000 });

    const head = run('/usr/bin/git', ['-C', repo, 'rev-parse', 'HEAD']).stdout.trim();
    const tree = run('/usr/bin/git', ['-C', repo, 'rev-parse', 'HEAD^{tree}']).stdout.trim();
    const status = run('/usr/bin/git', ['-C', repo, 'status', '--porcelain=v1', '-uall']).stdout.trim();
    if (head !== binding.head) throw new Error('candidate HEAD mismatch: ' + head);
    if (tree !== binding.tree) throw new Error('candidate TREE mismatch: ' + tree);
    if (status !== '') throw new Error('candidate clone dirty before image assembly');

    resultBase.completedStepReceipts.push({
      step: 'exact-candidate-clone',
      state: 'PASS',
      head,
      tree,
    });

    const javaHome = run('/usr/libexec/java_home', ['-v', '21'], { timeoutMs: 10000 }).stdout.trim();
    const gradleEnv = { ...process.env, JAVA_HOME: javaHome, PATH: javaHome + '/bin:' + process.env.PATH };
    run('./gradlew', ['--no-daemon', '-Dorg.gradle.java.home=' + javaHome, 'assembleVexLifeProdServiceImage'], {
      cwd: repo,
      timeoutMs: 360000,
      env: gradleEnv,
    });

    const imageRoot = path.join(repo, 'build/vexlife-prod-service-image/images', binding.head);
    const manifest = JSON.parse(fs.readFileSync(path.join(imageRoot, 'service-image.json'), 'utf8'));
    if (manifest.sourceSha !== binding.head) throw new Error('service image source mismatch');
    if (manifest.platform !== 'darwin-arm64') throw new Error('service image platform mismatch');
    if (manifest.javaRuntimeMajor !== 21) throw new Error('service image Java runtime is not 21');

    resultBase.completedStepReceipts.push({
      step: 'exact-private-service-image',
      state: 'PASS',
      sourceSha: manifest.sourceSha,
      payloadSha256: manifest.payloadSha256,
    });

    const setupWindow = path.join(imageRoot, 'setup/vexlife-prod-setup-window.applescript');
    const setupLauncher = path.join(imageRoot, 'bin/vexlife-prod-setup');

    run('/usr/bin/osacompile', ['-o', app, setupWindow], { timeoutMs: 60000 });
    const plist = path.join(app, 'Contents/Info.plist');
    run('/usr/bin/plutil', ['-replace', 'CFBundleIdentifier', '-string', 'com.vexlife.prod.setup', plist]);
    run('/usr/bin/plutil', ['-insert', 'LSMultipleInstancesProhibited', '-bool', 'YES', plist]);
    run('/usr/bin/plutil', ['-insert', 'VexLifeProdExpectedSourceSha', '-string', binding.head, plist]);
    run('/usr/bin/plutil', ['-insert', 'VexLifeProdSetupLauncher', '-string', setupLauncher, plist]);
    run('/usr/bin/codesign', ['--force', '--sign', '-', app], { timeoutMs: 60000 });
    run('/usr/bin/codesign', ['--verify', '--strict', app], { timeoutMs: 60000 });

    stage = 'NATIVE_SETUP';
    process.stderr.write('\n[VexLife PR148 repaired startup proof]\n');
    process.stderr.write('Complete the native Setup Journey normally.\n');
    process.stderr.write('When the browser opens, send exactly: Reply with exactly: startup circuit alive.\n');
    process.stderr.write('The proof will finish automatically after that fresh turn reaches TURN_COMPLETED.\n\n');

    // Launch the real native Setup presentation but do not make its GUI process lifetime own the
    // proof. Product currentness + chronology are the returned truths.
    run('/usr/bin/open', [app], { timeoutMs: 30000 });

    stage = 'PRODUCT_CURRENTNESS';
    const setupDeadline = Math.min(totalDeadline, Date.now() + SETUP_BOUND_MS);
    const exactSource = await waitForExactProduct(binding.head, setupDeadline);

    const currentState = await getJson('/api/v1/current-state');
    if (currentState.effectsPerformed !== false) throw new Error('current-state read performed effects');
    if (!currentState.homePath || !path.isAbsolute(currentState.homePath)) {
      throw new Error('current-state lacks absolute Home projection');
    }
    if (!currentState.activeModelRef) throw new Error('current-state lacks active model identity');

    observedHomePath = currentState.homePath;
    resultBase.effects.hostInstallationPerformed =
      resultBase.beforeState.sourceCurrentness?.sourceSha !== binding.head;

    resultBase.completedStepReceipts.push({
      step: 'exact-product-current',
      state: 'PASS',
      sourceSha: exactSource.sourceSha,
      serviceState: exactSource.serviceState,
      activeModelRef: currentState.activeModelRef,
      runtimeCurrentness: currentState.runtimeCurrentness,
    });

    stage = 'FIRST_HUMAN_TURN';
    let first = { state: 'WAITING_FOR_FIRST_TURN' };
    while (Date.now() < totalDeadline) {
      observedRecords = readChronology(observedHomePath, binding.head, startedAt.getTime());
      applyObservedEffects(observedRecords, resultBase.effects);
      first = analyzeFirstTurn(observedRecords);
      if (first.state === 'TURN_COMPLETED' || first.state === 'TURN_FAILED') break;
      await new Promise((resolve) => setTimeout(resolve, POLL_MS));
    }

    if (first.state !== 'TURN_COMPLETED') {
      const failureCode = first.terminalRefs?.failureCode || null;
      if (first.state === 'TURN_FAILED') {
        resultBase.candidateFindings.push('FIRST_TURN_FAILED' + (failureCode ? ':' + failureCode : ''));
      } else {
        resultBase.environmentFindings.push('FIRST_TURN_NOT_COMPLETED_WITHIN_BOUND');
      }
      resultBase.afterState = {
        stage,
        machineState: first.state,
        sourceSha: (await tryJson('/api/v1/source-currentness'))?.sourceSha || null,
        activeModelRef: currentState.activeModelRef,
        runtimeCurrentness: currentState.runtimeCurrentness,
        observedCandidateChronologyRecords: observedRecords.length,
      };
      writeResult({
        disposition: 'SAFE_FAILURE',
        stopReasonOrNull: first.state,
        firstSubstantiveFailureOrNull:
          failureRecord(new Error(first.state + (failureCode ? ':' + failureCode : '')), stage),
        ...resultBase,
      });
      return;
    }

    if (first.providerInvocationCount < 1) {
      throw new Error('completed first turn has no provider invocation fact');
    }

    const afterSource = await getJson('/api/v1/source-currentness');
    const afterState = await getJson('/api/v1/current-state');
    if (afterSource.sourceSha !== binding.head || afterSource.serviceState !== 'SOURCE_BOUND_LISTENING') {
      throw new Error('exact Product source not current after first completed turn');
    }

    applyObservedEffects(observedRecords, resultBase.effects);
    resultBase.effects.modelRuntimePerformed = true;
    resultBase.afterState = {
      stage: 'FIRST_HUMAN_TURN_COMPLETED',
      machineState: 'PASS',
      sourceSha: afterSource.sourceSha,
      serviceState: afterSource.serviceState,
      servicePid: afterSource.processPid,
      activeModelRef: afterState.activeModelRef,
      activeModelProfileRef: afterState.activeModelProfileRef,
      runtimeState: afterState.runtimeState,
      runtimeCurrentness: afterState.runtimeCurrentness,
      turnRef: first.turnRef,
      providerInvocationCount: first.providerInvocationCount,
      runtimeReceiptRefs: first.runtimeReceiptRefs,
      generationUnavailableObserved:
        observedRecords.some((record) => record.fact === 'RUNTIME_GENERATION_UNAVAILABLE'),
      runtimeRecoveryRequestedObserved:
        observedRecords.some((record) => record.fact === 'RUNTIME_RECOVERY_REQUESTED'),
      runtimeRecoveryCompletedObserved:
        observedRecords.some((record) => record.fact === 'RUNTIME_RECOVERY_COMPLETED'),
      rawReasoningCollectedByProof: false,
      conversationTextCollectedByProof: false,
    };

    resultBase.completedStepReceipts.push({
      step: 'first-human-turn',
      state: 'PASS',
      turnRef: first.turnRef,
      providerInvocationCount: first.providerInvocationCount,
      runtimeReceiptRefs: first.runtimeReceiptRefs,
    });

    writeResult({
      disposition: 'PASS',
      stopReasonOrNull: null,
      firstSubstantiveFailureOrNull: null,
      ...resultBase,
    });
  } catch (error) {
    resultBase.harnessFindings.push('REPAIRED_STARTUP_LIVED_STOPPED');
    if (observedHomePath) {
      try {
        observedRecords = readChronology(observedHomePath, binding.head, startedAt.getTime());
        applyObservedEffects(observedRecords, resultBase.effects);
      } catch (_) {
        resultBase.unknownFindings.push('CHRONOLOGY_UNAVAILABLE_AFTER_STOP');
      }
    }
    resultBase.afterState = {
      stage,
      machineState: 'SAFE_FAILURE',
      errorClass: error?.constructor?.name || 'Error',
      errorMessage: String(error?.message || error).slice(0, 1800),
      observedCandidateChronologyRecords: observedRecords.length,
    };
    writeResult({
      disposition: 'SAFE_FAILURE',
      stopReasonOrNull: 'REPAIRED_STARTUP_LIVED_STOPPED',
      firstSubstantiveFailureOrNull: failureRecord(error, stage),
      ...resultBase,
    });
  } finally {
    try { fs.rmSync(work, { recursive: true, force: true }); } catch (_) {}
  }
}

main().catch((error) => {
  try {
    writeResult({
      disposition: 'SAFE_FAILURE',
      stopReasonOrNull: 'UNCAUGHT_REPAIRED_STARTUP_FAILURE',
      firstSubstantiveFailureOrNull: failureRecord(error, 'UNCAUGHT'),
      candidateFindings: [],
      harnessFindings: ['UNCAUGHT_REPAIRED_STARTUP_FAILURE'],
      environmentFindings: [],
      unknownFindings: [],
      effects: defaultEffects(),
      beforeState: {},
      afterState: {
        errorClass: error?.constructor?.name || 'Error',
        errorMessage: String(error?.message || error).slice(0, 1800),
      },
      completedStepReceipts: [],
    });
  } catch (_) {}
  process.exitCode = 0;
});

// [VXG RealForever]
