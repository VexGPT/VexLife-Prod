#!/usr/bin/env node
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const os = require('node:os');
const crypto = require('node:crypto');
const { spawnSync } = require('node:child_process');

const RESULT_SCHEMA = 'vextreme.vexlocalbridge-mac-payload-result/v1';
const ORIGIN = 'http://127.0.0.1:18210';
const MAX_TOTAL_MS = 650000;
const POLL_MS = 1200;

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
  const summary = String(error?.message || error || 'unknown host proof failure').slice(0, 1000);
  return {
    failureFamily: 'VEXLIFE_PROD_LIVED_CIRCUIT',
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
    const error = new Error(
      command + ' ' + args.join(' ') + ' failed exit=' + result.status +
      '\nstdout=' + String(result.stdout || '').slice(-8000) +
      '\nstderr=' + String(result.stderr || '').slice(-8000),
    );
    error.exitCode = result.status;
    throw error;
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
    const text = fs.readFileSync(file, 'utf8');
    for (const line of text.split(/\n/)) {
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
      } catch (_) {
        // Chronology is diagnostics; one malformed historical line cannot become Product truth.
      }
    }
  }
  return records;
}

function applyObservedEffects(records, effects) {
  const facts = new Set(records.map((record) => record.fact));
  if (
    facts.has('RUNTIME_START_REQUESTED') ||
    facts.has('RUNTIME_RECOVERY_REQUESTED') ||
    facts.has('RUNTIME_RECOVERY_COMPLETED') ||
    facts.has('RUNTIME_MODEL_SWITCHED') ||
    facts.has('PROVIDER_INVOCATION_REQUESTED')
  ) {
    effects.modelRuntimePerformed = true;
  }
}

function segmentForTurn(records, sendIndex, turnRef) {
  let terminalIndex = -1;
  let terminal = null;
  for (let i = sendIndex + 1; i < records.length; i += 1) {
    const record = records[i];
    if (
      (record.fact === 'TURN_COMPLETED' || record.fact === 'TURN_FAILED') &&
      record.refs?.turnRef === turnRef
    ) {
      terminalIndex = i;
      terminal = record;
      break;
    }
  }
  return terminalIndex >= 0
    ? { records: records.slice(sendIndex, terminalIndex + 1), terminal, terminalIndex }
    : null;
}

function analyzeTurns(records) {
  const sends = records
    .map((record, index) => ({ record, index }))
    .filter(({ record }) => record.fact === 'SEND_ACCEPTED' && record.refs?.turnRef);

  const completed = [];
  for (const send of sends) {
    const turnRef = send.record.refs.turnRef;
    const segment = segmentForTurn(records, send.index, turnRef);
    if (!segment) continue;

    const providerInvocations = segment.records.filter((r) => r.fact === 'PROVIDER_INVOCATION_REQUESTED');
    const capabilityRequests = segment.records.filter((r) => r.fact === 'CAPABILITY_REQUEST_OBSERVED');
    const capabilityResults = segment.records.filter((r) => r.fact === 'CAPABILITY_RESULT_RETURNED');
    const matchedCapabilities = capabilityRequests
      .map((request) => ({
        request,
        result: capabilityResults.find((result) => result.refs?.callRef === request.refs?.callRef) || null,
      }));

    completed.push({
      turnRef,
      sendIndex: send.index,
      terminalIndex: segment.terminalIndex,
      terminalFact: segment.terminal.fact,
      terminalRefs: segment.terminal.refs || {},
      providerInvocations,
      matchedCapabilities,
    });
  }

  const first = completed.find((turn) => {
    if (turn.terminalFact !== 'TURN_COMPLETED') return false;
    return turn.matchedCapabilities.some(({ request, result }) =>
      request.refs?.handle === 'web' &&
      request.refs?.action === 'fetch' &&
      result?.refs?.state === 'RETURNED'
    );
  });

  if (!first) {
    const failed = completed.find((turn) => turn.terminalFact === 'TURN_FAILED');
    return {
      state: failed ? 'TURN_FAILED_BEFORE_WEB_PASS' : 'WAITING_FOR_WEB_TURN',
      failedTurn: failed || null,
      completed,
    };
  }

  const web = first.matchedCapabilities.find(({ request, result }) =>
    request.refs?.handle === 'web' &&
    request.refs?.action === 'fetch' &&
    result?.refs?.state === 'RETURNED'
  );

  const firstRuntimeRefs = [...new Set(
    first.providerInvocations
      .map((record) => record.refs?.runtimeReceiptRef)
      .filter(Boolean),
  )];

  const second = completed.find((turn) =>
    turn.sendIndex > first.terminalIndex &&
    turn.terminalFact === 'TURN_COMPLETED' &&
    turn.turnRef !== first.turnRef
  );

  if (!second) {
    const failedAfter = completed.find((turn) =>
      turn.sendIndex > first.terminalIndex &&
      turn.terminalFact === 'TURN_FAILED'
    );
    return {
      state: failedAfter ? 'SECOND_TURN_FAILED' : 'WAITING_FOR_SECOND_TURN',
      first,
      web,
      firstRuntimeRefs,
      failedTurn: failedAfter || null,
      completed,
    };
  }

  const secondRuntimeRefs = [...new Set(
    second.providerInvocations
      .map((record) => record.refs?.runtimeReceiptRef)
      .filter(Boolean),
  )];

  return {
    state: 'PASS',
    first,
    second,
    web,
    firstRuntimeRefs,
    secondRuntimeRefs,
    completed,
  };
}

async function waitForExactProduct(candidateHead, deadlineMs) {
  let last = null;
  while (Date.now() < deadlineMs) {
    last = await tryJson('/api/v1/source-currentness');
    if (
      last?.sourceSha === candidateHead &&
      last?.serviceState === 'SOURCE_BOUND_LISTENING' &&
      last?.effectsPerformed === false
    ) {
      return last;
    }
    await new Promise((resolve) => setTimeout(resolve, POLL_MS));
  }
  throw new Error(
    'exact Product source did not become current before the bounded host deadline; last=' +
    JSON.stringify(last),
  );
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
  const deadlineMs = startedAt.getTime() + MAX_TOTAL_MS;
  let stage = 'HOST_PREFLIGHT';
  let observedHomePath = null;
  let observedRecords = [];

  if (process.platform !== 'darwin' || process.arch !== 'arm64') {
    resultBase.environmentFindings.push('HOST_NOT_DARWIN_ARM64');
    writeResult({
      disposition: 'SAFE_FAILURE',
      stopReasonOrNull: 'HOST_NOT_DARWIN_ARM64',
      firstSubstantiveFailureOrNull: failureRecord(new Error('lived proof requires Apple-Silicon macOS'), 'HOST'),
      ...resultBase,
    });
    return;
  }

  resultBase.beforeState.sourceCurrentness = await tryJson('/api/v1/source-currentness');
  resultBase.beforeState.startedAt = startedAt.toISOString();
  resultBase.beforeState.candidateHead = binding.head;
  resultBase.beforeState.candidateTree = binding.tree;

  const tempBase = process.env.TMPDIR || os.tmpdir();
  const work = fs.mkdtempSync(path.join(tempBase, 'vexlife-prod-pr148-lived-'));
  const repo = path.join(work, 'candidate');
  const app = path.join(work, 'VexLife Prod Setup.app');

  try {
    resultBase.effects.providerOrNetworkEffectPerformed = true;
    stage = 'EXACT_SOURCE_AND_IMAGE';

    run('/usr/bin/git', ['clone', '--no-checkout', 'https://github.com/' + binding.repository + '.git', repo], {
      timeoutMs: 180000,
    });
    run('/usr/bin/git', ['-C', repo, 'checkout', '--detach', binding.head], { timeoutMs: 60000 });

    const head = run('/usr/bin/git', ['-C', repo, 'rev-parse', 'HEAD']).stdout.trim();
    const tree = run('/usr/bin/git', ['-C', repo, 'rev-parse', 'HEAD^{tree}']).stdout.trim();
    const status = run('/usr/bin/git', ['-C', repo, 'status', '--porcelain=v1', '-uall']).stdout.trim();
    if (head !== binding.head) throw new Error('candidate HEAD mismatch: ' + head);
    if (tree !== binding.tree) throw new Error('candidate TREE mismatch: ' + tree);
    if (status !== '') throw new Error('candidate clone is not clean before image assembly');

    resultBase.completedStepReceipts.push({
      step: 'exact-candidate-clone',
      state: 'PASS',
      head,
      tree,
    });

    run('./gradlew', ['--no-daemon', 'assembleVexLifeProdServiceImage'], {
      cwd: repo,
      timeoutMs: 360000,
      env: { ...process.env, JAVA_TOOL_OPTIONS: '' },
    });

    const imageRoot = path.join(repo, 'build/vexlife-prod-service-image/images', binding.head);
    const manifestPath = path.join(imageRoot, 'service-image.json');
    const manifest = JSON.parse(fs.readFileSync(manifestPath, 'utf8'));
    if (manifest.sourceSha !== binding.head) throw new Error('service image source mismatch');
    if (manifest.platform !== 'darwin-arm64') throw new Error('service image platform mismatch');
    if (manifest.javaRuntimeMajor !== 21) throw new Error('service image Java runtime is not 21');

    resultBase.completedStepReceipts.push({
      step: 'exact-private-service-image',
      state: 'PASS',
      sourceSha: manifest.sourceSha,
      payloadSha256: manifest.payloadSha256,
      javaRuntimeMajor: manifest.javaRuntimeMajor,
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

    resultBase.completedStepReceipts.push({
      step: 'native-setup-app-source-binding',
      state: 'PASS',
      sourceSha: binding.head,
    });

    stage = 'NATIVE_SETUP';
    process.stderr.write('\n[VexLife PR148 lived proof]\n');
    process.stderr.write('Complete the native Setup window that just opened. When VexLife opens in the browser:\n');
    process.stderr.write('1) ensure Viewable Thoughts is enabled;\n');
    process.stderr.write('2) send the exact web-fetch prompt supplied by ChatGPT and scroll/drag the live thought while it grows;\n');
    process.stderr.write('3) after the answer, reopen View Process in the same session;\n');
    process.stderr.write('4) send the exact second-loop prompt supplied by ChatGPT.\n');
    process.stderr.write('The proof will finish automatically after both turns are observed.\n\n');

    run('/usr/bin/open', ['-W', app], { timeoutMs: Math.max(60000, deadlineMs - Date.now()) });

    stage = 'PRODUCT_CURRENTNESS';
    const exactSource = await waitForExactProduct(binding.head, deadlineMs);
    const currentState = await getJson('/api/v1/current-state');
    if (currentState.effectsPerformed !== false) throw new Error('current-state read unexpectedly performed effects');
    if (!currentState.homePath || !path.isAbsolute(currentState.homePath)) throw new Error('current-state lacks an absolute Home projection');
    if (!currentState.activeModelRef) throw new Error('current-state lacks active model identity');

    const homePath = currentState.homePath;
    observedHomePath = homePath;
    resultBase.effects.hostInstallationPerformed =
      resultBase.beforeState.sourceCurrentness?.sourceSha !== binding.head;
    stage = 'LIVED_TURNS';
    resultBase.completedStepReceipts.push({
      step: 'exact-product-current',
      state: 'PASS',
      sourceSha: exactSource.sourceSha,
      serviceState: exactSource.serviceState,
      activeModelRef: currentState.activeModelRef,
      runtimeCurrentness: currentState.runtimeCurrentness,
    });

    let analysis = null;
    let records = [];
    while (Date.now() < deadlineMs) {
      records = readChronology(homePath, binding.head, startedAt.getTime());
      observedRecords = records;
      applyObservedEffects(records, resultBase.effects);
      analysis = analyzeTurns(records);

      if (analysis.state === 'PASS') break;
      if (analysis.state === 'TURN_FAILED_BEFORE_WEB_PASS' || analysis.state === 'SECOND_TURN_FAILED') break;

      await new Promise((resolve) => setTimeout(resolve, POLL_MS));
    }

    if (!analysis || analysis.state !== 'PASS') {
      const failed = analysis?.failedTurn;
      const failureCode = failed?.terminalRefs?.failureCode || null;
      const state = analysis?.state || 'LIVED_SEQUENCE_TIMEOUT';

      if (state === 'WAITING_FOR_WEB_TURN') {
        resultBase.candidateFindings.push('FIRST_TURN_DID_NOT_PROVE_WEB_CAPABILITY_RETURN');
      } else if (state === 'WAITING_FOR_SECOND_TURN') {
        resultBase.candidateFindings.push('SECOND_SEND_DID_NOT_REACH_TURN_COMPLETED');
      } else if (state === 'TURN_FAILED_BEFORE_WEB_PASS' || state === 'SECOND_TURN_FAILED') {
        resultBase.candidateFindings.push(state + (failureCode ? ':' + failureCode : ''));
      } else {
        resultBase.environmentFindings.push('LIVED_SEQUENCE_NOT_COMPLETED_WITHIN_BOUND');
      }

      const sourceAfterFailure = await tryJson('/api/v1/source-currentness');
      applyObservedEffects(records, resultBase.effects);
      resultBase.afterState = {
        stage,
        machineState: state,
        sourceSha: sourceAfterFailure?.sourceSha || null,
        serviceState: sourceAfterFailure?.serviceState || null,
        activeModelRef: currentState.activeModelRef,
        runtimeCurrentness: currentState.runtimeCurrentness,
        observedCandidateChronologyRecords: records.length,
        humanScrollVerdict: 'NOT_CAPTURED_BY_MACHINE_RETURN',
      };

      writeResult({
        disposition: 'SAFE_FAILURE',
        stopReasonOrNull: state,
        firstSubstantiveFailureOrNull: failureRecord(new Error(state + (failureCode ? ':' + failureCode : '')), 'LIVED_CIRCUIT'),
        ...resultBase,
      });
      return;
    }

    if (analysis.first.providerInvocations.length < 2) {
      throw new Error('web capability turn completed without the expected provider continuation');
    }
    if (analysis.firstRuntimeRefs.length !== 1) {
      throw new Error(
        'web capability turn did not preserve exactly one runtime receipt across provider stages: ' +
        JSON.stringify(analysis.firstRuntimeRefs),
      );
    }
    if (Number(analysis.first.terminalRefs.capabilityRequests || '0') < 1) {
      throw new Error('completed web turn did not record capabilityRequests>=1');
    }

    const afterSource = await getJson('/api/v1/source-currentness');
    const afterState = await getJson('/api/v1/current-state');
    if (afterSource.sourceSha !== binding.head || afterSource.serviceState !== 'SOURCE_BOUND_LISTENING') {
      throw new Error('exact Product source was not current after the second completed Send');
    }

    applyObservedEffects(records, resultBase.effects);
    resultBase.effects.modelRuntimePerformed = true;
    resultBase.effects.providerOrNetworkEffectPerformed = true;

    resultBase.afterState = {
      machineState: 'PASS',
      stage: 'LIVED_TURNS_COMPLETED',
      sourceSha: afterSource.sourceSha,
      serviceState: afterSource.serviceState,
      servicePid: afterSource.processPid,
      activeModelRef: afterState.activeModelRef,
      activeModelProfileRef: afterState.activeModelProfileRef,
      runtimeState: afterState.runtimeState,
      runtimeCurrentness: afterState.runtimeCurrentness,
      firstTurnRef: analysis.first.turnRef,
      firstCapabilityCallRef: analysis.web.request.refs.callRef,
      firstCapabilityState: analysis.web.result.refs.state,
      firstProviderInvocationCount: analysis.first.providerInvocations.length,
      firstRuntimeReceiptRef: analysis.firstRuntimeRefs[0],
      secondTurnRef: analysis.second.turnRef,
      secondProviderInvocationCount: analysis.second.providerInvocations.length,
      secondRuntimeReceiptRefs: analysis.secondRuntimeRefs,
      observedCandidateChronologyRecords: records.length,
      rawReasoningPersistedByProof: false,
      conversationTextCollectedByProof: false,
      humanScrollVerdict: 'NOT_CAPTURED_BY_MACHINE_RETURN',
    };

    resultBase.completedStepReceipts.push(
      {
        step: 'first-turn-web-capability',
        state: 'PASS',
        turnRef: analysis.first.turnRef,
        callRef: analysis.web.request.refs.callRef,
        capabilityState: analysis.web.result.refs.state,
        providerInvocationCount: analysis.first.providerInvocations.length,
        runtimeReceiptRef: analysis.firstRuntimeRefs[0],
      },
      {
        step: 'second-send-next-loop',
        state: 'PASS',
        turnRef: analysis.second.turnRef,
        providerInvocationCount: analysis.second.providerInvocations.length,
      },
      {
        step: 'post-turn-source-currentness',
        state: 'PASS',
        sourceSha: afterSource.sourceSha,
        serviceState: afterSource.serviceState,
      },
    );

    writeResult({
      disposition: 'PASS',
      stopReasonOrNull: null,
      firstSubstantiveFailureOrNull: null,
      ...resultBase,
    });
  } catch (error) {
    resultBase.harnessFindings.push('DIRECT_HOST_LIVED_CIRCUIT_STOPPED');
    if (observedHomePath) {
      try {
        observedRecords = readChronology(observedHomePath, binding.head, startedAt.getTime());
        applyObservedEffects(observedRecords, resultBase.effects);
      } catch (_) {
        resultBase.unknownFindings.push('CHRONOLOGY_UNAVAILABLE_AFTER_HOST_STOP');
      }
    } else if (stage === 'NATIVE_SETUP' || stage === 'PRODUCT_CURRENTNESS' || stage === 'LIVED_TURNS') {
      resultBase.unknownFindings.push('HOST_EFFECT_STATE_UNCONFIRMED_AFTER_NATIVE_SETUP_STOP');
    }
    resultBase.afterState = {
      stage,
      machineState: 'HARNESS_OR_HOST_FAILURE',
      errorClass: error?.constructor?.name || 'Error',
      errorMessage: String(error?.message || error).slice(0, 1600),
      humanScrollVerdict: 'NOT_CAPTURED_BY_MACHINE_RETURN',
    };
    writeResult({
      disposition: 'SAFE_FAILURE',
      stopReasonOrNull: 'DIRECT_HOST_LIVED_CIRCUIT_STOPPED',
      firstSubstantiveFailureOrNull: failureRecord(error, stage),
      ...resultBase,
    });
  } finally {
    try { fs.rmSync(work, { recursive: true, force: true }); } catch (_) {}
  }
}

main().catch((error) => {
  const effects = defaultEffects();
  try {
    writeResult({
      disposition: 'SAFE_FAILURE',
      stopReasonOrNull: 'UNCAUGHT_DIRECT_HOST_FAILURE',
      firstSubstantiveFailureOrNull: failureRecord(error, 'UNCAUGHT'),
      candidateFindings: [],
      harnessFindings: ['UNCAUGHT_DIRECT_HOST_FAILURE'],
      environmentFindings: [],
      unknownFindings: [],
      effects,
      beforeState: {},
      afterState: {
        errorClass: error?.constructor?.name || 'Error',
        errorMessage: String(error?.message || error).slice(0, 1600),
        humanScrollVerdict: 'NOT_CAPTURED_BY_MACHINE_RETURN',
      },
      completedStepReceipts: [],
    });
  } catch (_) {}
  process.exitCode = 0;
});

// [VXG RealForever]
