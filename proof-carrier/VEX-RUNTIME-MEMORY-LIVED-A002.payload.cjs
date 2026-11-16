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
const TOTAL_BOUND_MS = 30 * 60 * 1000;
const RUNTIME_BOUND_MS = 60 * 1000;
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
    failureFamily: 'VEXLIFE_PROD_RUNTIME_MEMORY_ONE_CACHE_LIVED',
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
    const observed = records.slice(sendIndex);
    const providerInvocations =
      observed.filter((item) => item.fact === 'PROVIDER_INVOCATION_REQUESTED');
    const capabilityRequests =
      observed.filter((item) => item.fact === 'CAPABILITY_REQUEST_OBSERVED');
    const capabilityResults =
      observed.filter((item) => item.fact === 'CAPABILITY_RESULT_RETURNED');
    const runtimeRefs = [...new Set(
      providerInvocations.map((item) => item.refs?.runtimeReceiptRef).filter(Boolean),
    )];

    for (let i = sendIndex + 1; i < records.length; i += 1) {
      const record = records[i];
      if (
        (record.fact === 'TURN_COMPLETED' || record.fact === 'TURN_FAILED') &&
        record.refs?.turnRef === turnRef
      ) {
        return {
          state: record.fact,
          turnRef,
          terminalRefs: record.refs || {},
          providerInvocationCount: providerInvocations.length,
          capabilityRequestCount: capabilityRequests.length,
          capabilityResultCount: capabilityResults.length,
          runtimeReceiptRefs: runtimeRefs,
        };
      }
    }

    return {
      state: 'WAITING_FOR_FIRST_TURN',
      turnRef,
      terminalRefs: {},
      providerInvocationCount: providerInvocations.length,
      capabilityRequestCount: capabilityRequests.length,
      capabilityResultCount: capabilityResults.length,
      runtimeReceiptRefs: runtimeRefs,
    };
  }
  return {
    state: 'WAITING_FOR_FIRST_TURN',
    turnRef: null,
    terminalRefs: {},
    providerInvocationCount: 0,
    capabilityRequestCount: 0,
    capabilityResultCount: 0,
    runtimeReceiptRefs: [],
  };
}

function chronologySummary(records) {
  return records.slice(-100).map((record) => ({
    at: record.at,
    owner: record.owner,
    fact: record.fact,
    refs: record.refs || {},
  }));
}

function failureReceiptSummary(homePath, turnRef) {
  if (!turnRef) return null;
  const file = path.join(homePath, 'recovery/direct-companion', turnRef + '.json');
  if (!fs.existsSync(file) || !fs.statSync(file).isFile()) return null;
  try {
    const receipt = JSON.parse(fs.readFileSync(file, 'utf8'));
    return {
      failureRef: receipt.failureRef ?? null,
      turnRef: receipt.turnRef ?? null,
      failureCode: receipt.failureCode ?? null,
      requestDurablyRecorded: receipt.requestDurablyRecorded ?? null,
      responseDurablyRecorded: receipt.responseDurablyRecorded ?? null,
      contextDurablyRecorded: receipt.contextDurablyRecorded ?? null,
      currentHeadCommitted: receipt.currentHeadCommitted ?? null,
      retrySameSendAttemptAllowed: receipt.retrySameSendAttemptAllowed ?? null,
      resumePossible: receipt.resumePossible ?? null,
      exactNextSafeRoute: receipt.exactNextSafeRoute ?? null,
      formedAt: receipt.formedAt ?? null,
    };
  } catch (_) {
    return { unreadable: true };
  }
}

function runtimePolicySnapshot() {
  const result = spawnSync('/bin/ps', ['-ww', '-axo', 'pid=,rss=,command='], {
    encoding: 'utf8',
    timeout: 5000,
    maxBuffer: 8 * 1024 * 1024,
  });
  if (result.error || result.status !== 0) {
    return { state: 'UNKNOWN', reason: 'PS_UNAVAILABLE', processes: [] };
  }
  const processes = [];
  for (const line of String(result.stdout || '').split(/\n/)) {
    if (!/mlx_lm\.server/.test(line)) continue;
    const match = line.trim().match(/^(\d+)\s+(\d+)\s+(.*)$/);
    if (!match) continue;
    const cache = match[3].match(/--prompt-cache-size(?:=|\s+)(\d+)/);
    processes.push({
      pid: Number(match[1]),
      rssMiB: Math.round((Number(match[2]) / 1024) * 10) / 10,
      promptCacheSize: cache ? Number(cache[1]) : null,
      promptCacheSizeOne: cache ? Number(cache[1]) === 1 : false,
    });
  }
  return {
    state: 'OBSERVED',
    observedAt: new Date().toISOString(),
    processCount: processes.length,
    processes,
  };
}

async function waitForOneCacheRuntime(deadlineMs) {
  let last = null;
  while (Date.now() < deadlineMs) {
    last = runtimePolicySnapshot();
    if (
      last.state === 'OBSERVED' &&
      last.processCount === 1 &&
      last.processes[0].promptCacheSizeOne === true
    ) {
      return last;
    }
    await new Promise((resolve) => setTimeout(resolve, POLL_MS));
  }
  throw new Error(
    'exact one-cache mlx-lm runtime was not uniquely observed; last=' + JSON.stringify(last),
  );
}

function analyzeTurns(records) {
  const summaries = [];
  const seen = new Set();
  for (let i = 0; i < records.length; i += 1) {
    const send = records[i];
    if (send.fact !== 'SEND_ACCEPTED' || !send.refs?.turnRef || seen.has(send.refs.turnRef)) continue;
    seen.add(send.refs.turnRef);
    const turnRef = send.refs.turnRef;
    let terminalIndex = -1;
    let nextSendIndex = records.length;
    for (let j = i + 1; j < records.length; j += 1) {
      if (records[j].fact === 'SEND_ACCEPTED') {
        nextSendIndex = j;
        break;
      }
      if (
        (records[j].fact === 'TURN_COMPLETED' || records[j].fact === 'TURN_FAILED') &&
        records[j].refs?.turnRef === turnRef
      ) {
        terminalIndex = j;
        break;
      }
    }
    const end = terminalIndex >= 0 ? terminalIndex + 1 : nextSendIndex;
    const window = records.slice(i, end);
    const providers = window.filter((item) => item.fact === 'PROVIDER_INVOCATION_REQUESTED');
    const terminal = terminalIndex >= 0 ? records[terminalIndex] : null;
    summaries.push({
      turnRef,
      sendAt: send.at,
      state: terminal?.fact || 'IN_FLIGHT',
      terminalAt: terminal?.at || null,
      failureCode: terminal?.refs?.failureCode || null,
      providerInvocationCount: providers.length,
      runtimeReceiptRefs: [...new Set(
        providers.map((item) => item.refs?.runtimeReceiptRef).filter(Boolean),
      )],
      capabilityRequestCount:
        window.filter((item) => item.fact === 'CAPABILITY_REQUEST_OBSERVED').length,
      capabilityResultCount:
        window.filter((item) => item.fact === 'CAPABILITY_RESULT_RETURNED').length,
      generationUnavailableObserved:
        window.some((item) => item.fact === 'RUNTIME_GENERATION_UNAVAILABLE'),
    });
  }
  return summaries;
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

  const work = fs.mkdtempSync(path.join(process.env.TMPDIR || os.tmpdir(), 'vexlife-prod-pr149-runtime-memory-one-cache-'));
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
    run(
      './gradlew',
      [
        '--no-daemon',
        '-Dorg.gradle.java.home=' + javaHome,
        'jvmTest',
        'compileKotlinJs',
        'compileTestKotlinJs',
        'checkVexLifeStrings',
        'assembleVexLifeProdServiceImage',
      ],
      {
        cwd: repo,
        timeoutMs: 600000,
        env: gradleEnv,
      },
    );

    resultBase.completedStepReceipts.push({
      step: 'exact-source-preflight',
      state: 'PASS',
      tasks: [
        'jvmTest',
        'compileKotlinJs',
        'compileTestKotlinJs',
        'checkVexLifeStrings',
        'assembleVexLifeProdServiceImage',
      ],
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
    process.stderr.write('\n[VexLife runtime-memory one-cache lived proof]\n');
    process.stderr.write('Complete the native Setup Journey normally if it appears.\n');
    process.stderr.write('When VexLife opens, stay in one normal Conversation and send three substantial messages.\n');
    process.stderr.write('Do not shorten them to help the test; after each answer, send the next one promptly.\n');
    process.stderr.write('If Vex naturally uses GitHub/web reach, let the turn continue normally.\n');
    process.stderr.write('The proof watches content-free chronology and returns after three completed Sends or the first failure.\n\n');

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

    stage = 'ONE_CACHE_RUNTIME';
    const oneCacheRuntime =
      await waitForOneCacheRuntime(
        Math.min(totalDeadline, Date.now() + RUNTIME_BOUND_MS),
      );
    resultBase.completedStepReceipts.push({
      step: 'one-cache-runtime-observed',
      state: 'PASS',
      runtime: oneCacheRuntime,
    });

    stage = 'THREE_HUMAN_TURNS';
    let turns = [];
    while (Date.now() < totalDeadline) {
      observedRecords = readChronology(observedHomePath, binding.head, startedAt.getTime());
      applyObservedEffects(observedRecords, resultBase.effects);
      turns = analyzeTurns(observedRecords);

      const failed = turns.find((turn) => turn.state === 'TURN_FAILED');
      if (failed) {
        resultBase.candidateFindings.push(
          'TURN_FAILED' + (failed.failureCode ? ':' + failed.failureCode : ''),
        );
        resultBase.afterState = {
          stage,
          machineState: 'SAFE_FAILURE',
          sourceCurrentness: await tryJson('/api/v1/source-currentness'),
          runtime: runtimePolicySnapshot(),
          turns,
          observedCandidateChronologyRecords: observedRecords.length,
          chronologySummary: chronologySummary(observedRecords),
          failureReceipt: failureReceiptSummary(observedHomePath, failed.turnRef),
          rawReasoningCollectedByProof: false,
          conversationTextCollectedByProof: false,
        };
        writeResult({
          disposition: 'SAFE_FAILURE',
          stopReasonOrNull: 'LIVED_TURN_FAILED',
          firstSubstantiveFailureOrNull:
            failureRecord(
              new Error(
                'TURN_FAILED' + (failed.failureCode ? ':' + failed.failureCode : ''),
              ),
              stage,
            ),
          ...resultBase,
        });
        return;
      }

      const completed = turns.filter((turn) => turn.state === 'TURN_COMPLETED');
      if (completed.length >= 3) {
        const selected = completed.slice(0, 3);
        if (selected.some((turn) => turn.providerInvocationCount < 1)) {
          throw new Error('a completed stress turn has no provider invocation fact');
        }

        const afterSource = await getJson('/api/v1/source-currentness');
        if (
          afterSource.sourceSha !== binding.head ||
          afterSource.serviceState !== 'SOURCE_BOUND_LISTENING'
        ) {
          throw new Error('exact Product source not current after three completed turns');
        }

        const finalRuntime = runtimePolicySnapshot();
        if (
          finalRuntime.state !== 'OBSERVED' ||
          finalRuntime.processCount !== 1 ||
          finalRuntime.processes[0].promptCacheSizeOne !== true
        ) {
          throw new Error(
            'one-cache runtime policy not current after three completed turns; observed=' +
            JSON.stringify(finalRuntime),
          );
        }

        resultBase.effects.modelRuntimePerformed = true;
        resultBase.afterState = {
          stage: 'THREE_HUMAN_TURNS_COMPLETED',
          machineState: 'PASS',
          sourceSha: afterSource.sourceSha,
          serviceState: afterSource.serviceState,
          runtime: finalRuntime,
          turns: selected,
          totalProviderInvocations:
            selected.reduce((sum, turn) => sum + turn.providerInvocationCount, 0),
          generationUnavailableObserved:
            observedRecords.some((record) => record.fact === 'RUNTIME_GENERATION_UNAVAILABLE'),
          runtimeRecoveryRequestedObserved:
            observedRecords.some((record) => record.fact === 'RUNTIME_RECOVERY_REQUESTED'),
          runtimeRecoveryCompletedObserved:
            observedRecords.some((record) => record.fact === 'RUNTIME_RECOVERY_COMPLETED'),
          chronologySummary: chronologySummary(observedRecords),
          rawReasoningCollectedByProof: false,
          conversationTextCollectedByProof: false,
        };
        resultBase.completedStepReceipts.push({
          step: 'three-human-turns',
          state: 'PASS',
          turnRefs: selected.map((turn) => turn.turnRef),
          providerInvocationCounts: selected.map((turn) => turn.providerInvocationCount),
        });
        writeResult({
          disposition: 'PASS',
          stopReasonOrNull: null,
          firstSubstantiveFailureOrNull: null,
          ...resultBase,
        });
        return;
      }

      await new Promise((resolve) => setTimeout(resolve, POLL_MS));
    }

    observedRecords = readChronology(observedHomePath, binding.head, startedAt.getTime());
    turns = analyzeTurns(observedRecords);
    applyObservedEffects(observedRecords, resultBase.effects);
    const inFlight = turns.find((turn) => turn.state === 'IN_FLIGHT');
    if (inFlight) {
      resultBase.unknownFindings.push('IN_FLIGHT_TURN_NOT_TERMINAL_WITHIN_BOUND');
    } else {
      resultBase.environmentFindings.push('THREE_HUMAN_SENDS_NOT_OBSERVED_WITHIN_BOUND');
    }
    resultBase.afterState = {
      stage,
      machineState: 'SAFE_FAILURE',
      turns,
      sourceCurrentness: await tryJson('/api/v1/source-currentness'),
      runtime: runtimePolicySnapshot(),
      chronologySummary: chronologySummary(observedRecords),
      failureReceipt:
        inFlight ? failureReceiptSummary(observedHomePath, inFlight.turnRef) : null,
      rawReasoningCollectedByProof: false,
      conversationTextCollectedByProof: false,
    };
    writeResult({
      disposition: 'SAFE_FAILURE',
      stopReasonOrNull:
        inFlight
          ? 'IN_FLIGHT_TURN_NOT_TERMINAL_WITHIN_BOUND'
          : 'THREE_HUMAN_SENDS_NOT_OBSERVED_WITHIN_BOUND',
      firstSubstantiveFailureOrNull:
        failureRecord(
          new Error(
            inFlight
              ? 'in-flight turn did not reach a terminal state within the bound'
              : 'three human Sends were not observed within the bound',
          ),
          stage,
        ),
      ...resultBase,
    });
  } catch (error) {
    if (
      stage === 'EXACT_SOURCE_AND_IMAGE' ||
      stage === 'NATIVE_SETUP' ||
      stage === 'PRODUCT_CURRENTNESS' ||
      stage === 'ONE_CACHE_RUNTIME' ||
      stage === 'THREE_HUMAN_TURNS'
    ) {
      resultBase.candidateFindings.push(
        'RUNTIME_MEMORY_CANDIDATE_STOPPED:' + stage,
      );
    } else {
      resultBase.harnessFindings.push(
        'RUNTIME_MEMORY_CARRIER_STOPPED:' + stage,
      );
    }
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
      stopReasonOrNull: 'RUNTIME_MEMORY_LIVED_STOPPED',
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
      stopReasonOrNull: 'UNCAUGHT_RUNTIME_MEMORY_FAILURE',
      firstSubstantiveFailureOrNull: failureRecord(error, 'UNCAUGHT'),
      candidateFindings: [],
      harnessFindings: ['UNCAUGHT_RUNTIME_MEMORY_FAILURE'],
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
