#!/usr/bin/env node
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const os = require('node:os');
const crypto = require('node:crypto');
const { spawnSync } = require('node:child_process');

const RESULT_SCHEMA = 'vextreme.vexlocalbridge-mac-payload-result/v1';
const PRODUCT_ORIGIN = 'http://127.0.0.1:18210';
const HERETIC_ORIGIN = 'http://127.0.0.1:8765';
const POLL_MS = 1000;

function sha256(value) {
  return crypto.createHash('sha256').update(value).digest('hex');
}

function effects() {
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
    providerOrNetworkEffectPerformed: true,
    publicationPerformed: false,
    credentialSerializationPerformed: false,
    filesystemOutsideQualifiedRootsPerformed: false,
  };
}

function failureRecord(error, stage) {
  const summary = String(error?.message || error || 'unknown A004 failure').slice(0, 1600);
  return {
    failureFamily: 'VEXLIFE_PROD_RUNTIME_MEMORY_ONE_CACHE_CURRENTIZATION',
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

function resolveExec(candidates) {
  for (const candidate of candidates) {
    if (fs.existsSync(candidate)) return candidate;
  }
  throw new Error('required executable unavailable: ' + candidates.join(', '));
}

function run(command, args, options = {}) {
  const result = spawnSync(command, args, {
    cwd: options.cwd,
    env: options.env || process.env,
    encoding: 'utf8',
    timeout: options.timeoutMs || 300000,
    maxBuffer: options.maxBuffer || 64 * 1024 * 1024,
    stdio: options.stdio || ['ignore', 'pipe', 'pipe'],
  });
  if (result.error) throw result.error;
  if (result.status !== 0) {
    throw new Error(
      command + ' ' + args.join(' ') + ' failed exit=' + result.status +
      '\nstdout=' + String(result.stdout || '').slice(-12000) +
      '\nstderr=' + String(result.stderr || '').slice(-12000),
    );
  }
  return result;
}

function parseJsonOutput(result, label) {
  const lines = String(result.stdout || '')
    .split(/\n/)
    .map((line) => line.trim())
    .filter(Boolean);
  for (let i = lines.length - 1; i >= 0; i -= 1) {
    try { return JSON.parse(lines[i]); } catch (_) {}
  }
  throw new Error(label + ' emitted no parseable JSON: ' + String(result.stdout || '').slice(-4000));
}

async function fetchJson(url, timeoutMs = 5000) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const response = await fetch(url, {
      method: 'GET',
      headers: { accept: 'application/json' },
      signal: controller.signal,
    });
    const text = await response.text();
    let json = null;
    try { json = JSON.parse(text); } catch (_) {}
    return { ok: response.ok, status: response.status, json, error: null };
  } catch (error) {
    return { ok: false, status: null, json: null, error: String(error?.message || error) };
  } finally {
    clearTimeout(timer);
  }
}

async function waitForJson(url, predicate, deadlineMs) {
  let last = null;
  while (Date.now() < deadlineMs) {
    last = await fetchJson(url, 5000);
    if (last.ok && predicate(last.json)) return last;
    await new Promise((resolve) => setTimeout(resolve, POLL_MS));
  }
  throw new Error('timed out waiting for ' + url + '; last=' + JSON.stringify(last));
}

function readJson(file) {
  return JSON.parse(fs.readFileSync(file, 'utf8'));
}

function promptCacheSize(args) {
  if (!Array.isArray(args)) return null;
  const index = args.indexOf('--prompt-cache-size');
  if (index < 0 || index + 1 >= args.length) return null;
  const value = Number(args[index + 1]);
  return Number.isFinite(value) ? value : null;
}

function processSnapshot(pid, args) {
  let alive = true;
  try { process.kill(pid, 0); } catch (_) { alive = false; }

  const result = spawnSync('/bin/ps', ['-ww', '-p', String(pid), '-o', 'pid=,pgid=,command='], {
    encoding: 'utf8',
    timeout: 5000,
    maxBuffer: 4 * 1024 * 1024,
  });
  if (result.error || result.status !== 0) {
    return {
      pid,
      alive,
      processGroupId: null,
      recordedArgsMatch: false,
      promptCacheSize: null,
      commandSha256: null,
    };
  }

  const text = String(result.stdout || '').trim();
  const match = /^(\d+)\s+(\d+)\s+(.*)$/s.exec(text);
  if (!match) {
    return {
      pid,
      alive,
      processGroupId: null,
      recordedArgsMatch: false,
      promptCacheSize: null,
      commandSha256: sha256(Buffer.from(text)),
    };
  }

  const command = match[3];
  const cache = /(?:^|\s)--prompt-cache-size(?:=|\s+)(\d+)(?:\s|$)/.exec(command);
  return {
    pid: Number(match[1]),
    alive,
    processGroupId: Number(match[2]),
    recordedArgsMatch: command.endsWith(' ' + args.join(' ')),
    promptCacheSize: cache ? Number(cache[1]) : null,
    commandSha256: sha256(Buffer.from(command)),
  };
}

function listenerPids(port) {
  const lsof = ['/usr/sbin/lsof', '/usr/bin/lsof'].find((file) => fs.existsSync(file));
  if (!lsof) throw new Error('lsof is unavailable');
  const result = spawnSync(lsof, ['-nP', '-iTCP:' + String(port), '-sTCP:LISTEN', '-t'], {
    encoding: 'utf8',
    timeout: 5000,
    maxBuffer: 1024 * 1024,
  });
  if (result.error) throw result.error;
  if (result.status !== 0 && String(result.stdout || '').trim() === '') return [];
  return [...new Set(
    String(result.stdout || '')
      .split(/\s+/)
      .map((value) => Number(value))
      .filter((value) => Number.isInteger(value) && value > 0),
  )].sort((a, b) => a - b);
}

function launchdCurrent(receipt) {
  const substrate = receipt?.runtime?.processSubstrate;
  if (!substrate?.domainTarget || !substrate?.label) return { current: false, observedPid: null };
  const result = spawnSync('/bin/launchctl', ['print', substrate.domainTarget + '/' + substrate.label], {
    encoding: 'utf8',
    timeout: 5000,
    maxBuffer: 2 * 1024 * 1024,
  });
  if (result.error || result.status !== 0) return { current: false, observedPid: null };
  const match = /(?:^|\n)\s*pid\s*=\s*(\d+)/.exec(String(result.stdout || ''));
  const observedPid = match ? Number(match[1]) : null;
  return { current: observedPid === Number(receipt.runtime?.pid), observedPid };
}

function productRuntimeReceiptPath(homePath) {
  return path.join(homePath, 'runtime/vexlife-prod/heretic-persistent-runtime-owner-receipt.json');
}

async function main() {
  const candidatePath = process.argv[2];
  if (!candidatePath) throw new Error('candidate input is required');
  const candidate = readJson(path.resolve(candidatePath));

  const result = {
    candidateFindings: [],
    harnessFindings: [],
    environmentFindings: [],
    unknownFindings: [],
    effects: effects(),
    beforeState: {
      candidateHead: candidate.head,
      candidateTree: candidate.tree,
      sourcePullRequest: candidate.sourcePullRequest,
      temporaryCarrierPullRequest: candidate.pullRequest,
    },
    afterState: {},
    completedStepReceipts: [],
  };

  let stage = 'HOST_PREFLIGHT';
  const work = fs.mkdtempSync(path.join(process.env.TMPDIR || os.tmpdir(), 'vexlife-runtime-memory-a005-'));
  const repo = path.join(work, 'candidate');

  try {
    if (process.platform !== 'darwin' || process.arch !== 'arm64') {
      throw new Error('A004 requires Apple-silicon macOS');
    }

    const gh = resolveExec(['/opt/homebrew/bin/gh', '/usr/local/bin/gh']);
    const git = resolveExec(['/usr/bin/git']);
    run(gh, ['auth', 'status', '--hostname', 'github.com'], { timeoutMs: 30000 });

    stage = 'EXACT_SOURCE';
    run(gh, ['repo', 'clone', candidate.repository, repo, '--', '--no-checkout'], {
      timeoutMs: 180000,
    });
    run(git, ['-C', repo, 'checkout', '--detach', candidate.head], { timeoutMs: 60000 });

    const head = run(git, ['-C', repo, 'rev-parse', 'HEAD']).stdout.trim();
    const tree = run(git, ['-C', repo, 'rev-parse', 'HEAD^{tree}']).stdout.trim();
    const status = run(git, ['-C', repo, 'status', '--porcelain=v1', '-uall']).stdout.trim();
    if (head !== candidate.head) throw new Error('candidate HEAD mismatch: ' + head);
    if (tree !== candidate.tree) throw new Error('candidate TREE mismatch: ' + tree);
    if (status !== '') throw new Error('candidate clone is dirty before proof');

    result.completedStepReceipts.push({
      step: 'exact-candidate-source',
      state: 'PASS',
      head,
      tree,
    });

    stage = 'SOURCE_BUILD_PROOF';
    const javaHome = run('/usr/libexec/java_home', ['-v', '21'], { timeoutMs: 10000 }).stdout.trim();
    if (!javaHome || !fs.existsSync(path.join(javaHome, 'bin/java'))) {
      throw new Error('JDK 21 could not be resolved');
    }
    const gradleEnv = {
      ...process.env,
      JAVA_HOME: javaHome,
      PATH: javaHome + '/bin:' + process.env.PATH,
    };

    run(
      './gradlew',
      [
        '--no-daemon',
        '-Dorg.gradle.java.home=' + javaHome,
        'jvmTest',
        'compileKotlinJs',
        'compileTestKotlinJs',
        'checkVexLifeStrings',
      ],
      { cwd: repo, env: gradleEnv, timeoutMs: 1200000 },
    );

    run(
      './gradlew',
      [
        '--no-daemon',
        '-Dorg.gradle.java.home=' + javaHome,
        '-PvexlifeAppVersion=0.0.0',
        'archiveVexLifeProdMacApplication',
      ],
      { cwd: repo, env: gradleEnv, timeoutMs: 1200000 },
    );

    const imageRoot = path.join(repo, 'build/vexlife-prod-service-image/images', candidate.head);
    const setupLauncher = path.join(imageRoot, 'bin/vexlife-prod-setup');
    const imageManifestPath = path.join(imageRoot, 'service-image.json');
    if (!fs.existsSync(setupLauncher) || !fs.existsSync(imageManifestPath)) {
      throw new Error('exact private service image was not formed');
    }
    const imageManifest = readJson(imageManifestPath);
    if (imageManifest.sourceSha !== candidate.head) {
      throw new Error('private image source SHA mismatch');
    }

    const appRoot = path.join(repo, 'build/vexlife-prod-mac-application', candidate.head);
    const dmg = path.join(appRoot, 'VexLife-macOS-AppleSilicon.dmg');
    const dmgSum = path.join(appRoot, 'VexLife-macOS-AppleSilicon.dmg.sha256');
    if (!fs.existsSync(dmg) || !fs.existsSync(dmgSum)) {
      throw new Error('Mac DMG proof artifact was not formed');
    }
    run('/usr/bin/shasum', ['-a', '256', '-c', path.basename(dmgSum)], {
      cwd: appRoot,
      timeoutMs: 30000,
    });
    run('/usr/bin/hdiutil', ['verify', dmg], { timeoutMs: 120000 });

    const postBuildStatus = run(git, ['-C', repo, 'status', '--porcelain=v1', '-uall']).stdout.trim();
    if (postBuildStatus !== '') throw new Error('candidate worktree became dirty during source/build proof');

    result.completedStepReceipts.push({
      step: 'source-build-mac-package-proof',
      state: 'PASS',
      privateImagePayloadSha256: imageManifest.payloadSha256,
      dmgSha256: run('/usr/bin/shasum', ['-a', '256', dmg]).stdout.trim().split(/\s+/)[0],
    });

    stage = 'DISCOVER_CURRENT_HOME_AND_RUNTIME';
    const currentStateBefore = await fetchJson(PRODUCT_ORIGIN + '/api/v1/current-state', 5000);
    const homePath =
      currentStateBefore.ok && typeof currentStateBefore.json?.homePath === 'string'
        ? path.normalize(currentStateBefore.json.homePath)
        : path.join(os.homedir(), '.vexlife');
    if (!path.isAbsolute(homePath)) throw new Error('observed Vex Home is not absolute');

    const priorReceiptPath = productRuntimeReceiptPath(homePath);
    const priorReceipt = fs.existsSync(priorReceiptPath) ? readJson(priorReceiptPath) : null;
    const priorPid = Number(priorReceipt?.runtime?.pid ?? 0);
    const priorArgs = Array.isArray(priorReceipt?.runtime?.args) ? priorReceipt.runtime.args : [];
    const priorCacheSize = promptCacheSize(priorArgs);
    const beforeSource = await fetchJson(PRODUCT_ORIGIN + '/api/v1/source-currentness', 5000);

    result.beforeState.homeSource =
      currentStateBefore.ok ? 'PRODUCT_CURRENT_STATE' : 'DEFAULT_CANONICAL_HOME';
    result.beforeState.currentServiceSourceSha = beforeSource.json?.sourceSha ?? null;
    result.beforeState.activeModelRef = currentStateBefore.json?.activeModelRef ?? null;
    result.beforeState.productRuntimePid = Number.isInteger(priorPid) && priorPid > 0 ? priorPid : null;
    result.beforeState.productRuntimePromptCacheSize = priorCacheSize;
    result.beforeState.productImplementationClassSha256 =
      priorReceipt?.implementationIdentity?.classSha256 ?? null;

    stage = 'FRONT_DOOR_INSPECTION';
    const inspectionRun = run(
      setupLauncher,
      [
        '--inspect-front-door',
        '--home', homePath,
        '--expected-source-sha', candidate.head,
        '--language', 'en',
      ],
      { timeoutMs: 120000 },
    );
    const inspection = parseJsonOutput(inspectionRun, 'front-door inspection');
    const state = inspection.semanticStateRef;

    let controlRef = null;
    if (state === 'UPDATE_AVAILABLE' || state === 'REPAIR_REQUIRED' || state === 'UPDATE_REPAIR_REQUIRED') {
      controlRef = 'CONVERGE_DESIRED';
    } else if (state === 'START_REQUIRED') {
      controlRef = 'START';
    } else if (state === 'CURRENT') {
      controlRef = null;
    } else if (state === 'SETUP_REQUIRED') {
      throw new Error('candidate unexpectedly requires acquisition; A004 does not authorize acquisition');
    } else {
      throw new Error('front-door state does not admit A004 runtime currentization: ' + state);
    }

    result.completedStepReceipts.push({
      step: 'front-door-inspection',
      state: 'PASS',
      semanticStateRef: state,
      selectedControlRef: controlRef,
    });

    stage = 'FRONT_DOOR_CONVERGENCE';
    if (controlRef !== null) {
      const activationRun = run(
        setupLauncher,
        [
          '--activate-front-door-control', controlRef,
          '--home', homePath,
          '--expected-source-sha', candidate.head,
          '--language', 'en',
        ],
        { timeoutMs: 900000, maxBuffer: 64 * 1024 * 1024 },
      );
      const activation = parseJsonOutput(activationRun, 'front-door activation');
      if (activation.failure != null) {
        throw new Error('front-door convergence reported failure: ' + JSON.stringify(activation.failure));
      }
      if (activation.semanticStateRef !== 'CURRENT') {
        throw new Error('front-door convergence did not reobserve CURRENT: ' + activation.semanticStateRef);
      }
      result.completedStepReceipts.push({
        step: 'front-door-convergence',
        state: 'PASS',
        controlRef,
        reobservedState: activation.semanticStateRef,
      });
      result.effects.hostInstallationPerformed = true;
      result.effects.modelRuntimePerformed = true;
    } else {
      result.completedStepReceipts.push({
        step: 'front-door-convergence',
        state: 'NO_EFFECT_ALREADY_CURRENT',
      });
    }

    stage = 'EXACT_CURRENTNESS';
    const sourceCurrentness = await waitForJson(
      PRODUCT_ORIGIN + '/api/v1/source-currentness',
      (value) =>
        value?.sourceSha === candidate.head &&
        value?.serviceState === 'SOURCE_BOUND_LISTENING' &&
        value?.effectsPerformed === false,
      Date.now() + 180000,
    );
    const currentStateAfter = await waitForJson(
      PRODUCT_ORIGIN + '/api/v1/current-state',
      (value) =>
        value?.runtimeState === 'BOUND_PRODUCT_MODEL' &&
        value?.effectsPerformed === false,
      Date.now() + 120000,
    );

    const afterReceipt = readJson(priorReceiptPath);
    const afterArgs = Array.isArray(afterReceipt.runtime?.args) ? afterReceipt.runtime.args : [];
    const afterCacheSize = promptCacheSize(afterArgs);
    const afterPid = Number(afterReceipt.runtime?.pid);
    if (!Number.isInteger(afterPid) || afterPid <= 0) {
      throw new Error('current Product runtime receipt has no valid PID');
    }
    if (afterReceipt.state !== 'PERSISTENT_MLX_RUNTIME_QUALIFIED') {
      throw new Error('current Product runtime receipt is not QUALIFIED');
    }
    if (afterCacheSize !== 1) {
      throw new Error('current Product runtime receipt does not contain --prompt-cache-size 1');
    }

    const runtimeProcess = processSnapshot(afterPid, afterArgs);
    if (
      !runtimeProcess.alive ||
      runtimeProcess.pid !== afterPid ||
      runtimeProcess.processGroupId !== Number(afterReceipt.runtime?.processGroupId) ||
      !runtimeProcess.recordedArgsMatch ||
      runtimeProcess.promptCacheSize !== 1
    ) {
      throw new Error('live Product mlx-lm process does not match exact one-cache receipt');
    }

    const launchd = launchdCurrent(afterReceipt);
    if (!launchd.current || launchd.observedPid !== afterPid) {
      throw new Error('Product runtime launchd coordinate is not current to exact PID');
    }

    const listeners = listenerPids(8765);
    if (listeners.length !== 1 || listeners[0] !== afterPid) {
      throw new Error('127.0.0.1:8765 is not uniquely owned by the exact Product runtime');
    }

    const health = await fetchJson(HERETIC_ORIGIN + '/health', 5000);
    if (!health.ok || health.status !== 200 || health.json?.status !== 'ok') {
      throw new Error('exact one-cache Product runtime is not healthy');
    }

    if (
      result.beforeState.activeModelRef != null &&
      currentStateAfter.json?.activeModelRef !== result.beforeState.activeModelRef
    ) {
      throw new Error(
        'A004 changed the selected/serving model: before=' +
        result.beforeState.activeModelRef +
        ' after=' + currentStateAfter.json?.activeModelRef,
      );
    }

    if (
      priorReceipt &&
      priorCacheSize !== 1 &&
      priorPid === afterPid
    ) {
      throw new Error('old no-cache-policy Product PID was not rotated');
    }

    if (
      priorReceipt?.implementationIdentity?.classSha256 &&
      afterReceipt.implementationIdentity?.classSha256 ===
        priorReceipt.implementationIdentity.classSha256
    ) {
      throw new Error('runtime implementation identity did not advance across candidate currentization');
    }

    result.completedStepReceipts.push({
      step: 'exact-one-cache-runtime-currentness',
      state: 'PASS',
      pid: afterPid,
      promptCacheSize: 1,
      health: 'ok',
    });

    result.afterState = {
      machineState: 'PASS',
      sourceSha: sourceCurrentness.json.sourceSha,
      serviceState: sourceCurrentness.json.serviceState,
      servicePid: sourceCurrentness.json.processPid ?? null,
      activeModelRef: currentStateAfter.json.activeModelRef ?? null,
      runtimeReceiptRef: afterReceipt.receiptRef,
      runtimePid: afterPid,
      runtimeProcessGroupId: afterReceipt.runtime.processGroupId,
      runtimePromptCacheSize: afterCacheSize,
      runtimeImplementationClassSha256: afterReceipt.implementationIdentity?.classSha256 ?? null,
      process: runtimeProcess,
      launchd,
      listenerPids: listeners,
      health: {
        status: health.status,
        json: health.json,
      },
      priorRuntimePid: Number.isInteger(priorPid) && priorPid > 0 ? priorPid : null,
      priorRuntimePromptCacheSize: priorCacheSize,
      priorImplementationClassSha256:
        priorReceipt?.implementationIdentity?.classSha256 ?? null,
      modelChanged: false,
      conversationSendPerformed: false,
      rawConversationTextCollected: false,
      rawReasoningCollected: false,
    };

    writeResult({
      disposition: 'PASS',
      stopReasonOrNull: null,
      firstSubstantiveFailureOrNull: null,
      ...result,
    });
  } catch (error) {
    result.candidateFindings.push('A004_STOPPED:' + stage);
    result.afterState = {
      machineState: 'SAFE_FAILURE',
      stage,
      errorClass: error?.constructor?.name || 'Error',
      errorMessage: String(error?.message || error).slice(0, 2400),
      conversationSendPerformed: false,
      rawConversationTextCollected: false,
      rawReasoningCollected: false,
    };
    writeResult({
      disposition: 'SAFE_FAILURE',
      stopReasonOrNull: 'RUNTIME_MEMORY_ONE_CACHE_CURRENTIZATION_STOPPED',
      firstSubstantiveFailureOrNull: failureRecord(error, stage),
      ...result,
    });
  } finally {
    try { fs.rmSync(work, { recursive: true, force: true }); } catch (_) {}
  }
}

main().catch((error) => {
  try {
    writeResult({
      disposition: 'SAFE_FAILURE',
      stopReasonOrNull: 'UNCAUGHT_A004_FAILURE',
      firstSubstantiveFailureOrNull: failureRecord(error, 'UNCAUGHT'),
      candidateFindings: [],
      harnessFindings: ['UNCAUGHT_A004_FAILURE'],
      environmentFindings: [],
      unknownFindings: [],
      effects: effects(),
      beforeState: {},
      afterState: {
        errorClass: error?.constructor?.name || 'Error',
        errorMessage: String(error?.message || error).slice(0, 1600),
        conversationSendPerformed: false,
      },
      completedStepReceipts: [],
    });
  } catch (_) {}
  process.exitCode = 0;
});

// [VXG RealForever]
