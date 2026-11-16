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
const HERETIC_PORT = 8765;

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
  const summary = String(error?.message || error || 'unknown diagnostic failure').slice(0, 1200);
  return {
    failureFamily: 'VEXLIFE_PROD_RUNTIME_MEMORY_SERVING_OWNER_DIAGNOSTIC',
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

async function fetchJson(url, timeoutMs = 3000) {
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

function readJsonIfPresent(file) {
  if (!fs.existsSync(file) || !fs.statSync(file).isFile()) return null;
  return JSON.parse(fs.readFileSync(file, 'utf8'));
}

function promptCacheSize(args) {
  if (!Array.isArray(args)) return null;
  const index = args.indexOf('--prompt-cache-size');
  if (index < 0 || index + 1 >= args.length) return null;
  const value = Number(args[index + 1]);
  return Number.isFinite(value) ? value : null;
}

function psSnapshot(pid, recordedArgs) {
  if (!Number.isInteger(pid) || pid <= 0) {
    return {
      pid: pid ?? null,
      alive: false,
      processGroupId: null,
      commandSha256: null,
      recordedArgsMatch: false,
      promptCacheSize: null,
    };
  }

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
      commandSha256: null,
      recordedArgsMatch: false,
      promptCacheSize: null,
      psError: String(result.error?.message || result.stderr || 'ps failed').slice(0, 400),
    };
  }

  const text = String(result.stdout || '').trim();
  const match = /^(\d+)\s+(\d+)\s+(.*)$/s.exec(text);
  if (!match) {
    return {
      pid,
      alive,
      processGroupId: null,
      commandSha256: sha256(Buffer.from(text)),
      recordedArgsMatch: false,
      promptCacheSize: null,
      psParseFailed: true,
    };
  }

  const command = match[3];
  const args = Array.isArray(recordedArgs) ? recordedArgs : [];
  const cache = /(?:^|\s)--prompt-cache-size(?:=|\s+)(\d+)(?:\s|$)/.exec(command);

  return {
    pid: Number(match[1]),
    alive,
    processGroupId: Number(match[2]),
    commandSha256: sha256(Buffer.from(command)),
    recordedArgsMatch: args.length > 0 && command.endsWith(' ' + args.join(' ')),
    promptCacheSize: cache ? Number(cache[1]) : null,
  };
}

function listenerPids(port) {
  const executable = ['/usr/sbin/lsof', '/usr/bin/lsof'].find((file) => fs.existsSync(file));
  if (!executable) return { state: 'UNKNOWN', pids: [], error: 'LSOF_UNAVAILABLE' };

  const result = spawnSync(
    executable,
    ['-nP', '-iTCP:' + String(port), '-sTCP:LISTEN', '-t'],
    { encoding: 'utf8', timeout: 5000, maxBuffer: 1024 * 1024 },
  );

  if (result.error) {
    return { state: 'UNKNOWN', pids: [], error: String(result.error.message || result.error) };
  }

  if (result.status !== 0 && String(result.stdout || '').trim() === '') {
    return { state: 'OBSERVED', pids: [], error: null };
  }

  const pids = [...new Set(
    String(result.stdout || '')
      .split(/\s+/)
      .map((value) => Number(value))
      .filter((value) => Number.isInteger(value) && value > 0),
  )].sort((a, b) => a - b);

  return { state: 'OBSERVED', pids, error: null };
}

function launchdSnapshot(receipt) {
  const substrate = receipt?.runtime?.processSubstrate;
  if (!substrate?.domainTarget || !substrate?.label) {
    return {
      state: 'NO_SUBSTRATE_RECEIPT',
      pid: null,
      coordinate: null,
    };
  }

  const coordinate = substrate.domainTarget + '/' + substrate.label;
  const result = spawnSync('/bin/launchctl', ['print', coordinate], {
    encoding: 'utf8',
    timeout: 5000,
    maxBuffer: 2 * 1024 * 1024,
  });

  if (result.error) {
    return {
      state: 'UNKNOWN',
      pid: null,
      coordinate,
      error: String(result.error.message || result.error).slice(0, 400),
    };
  }

  if (result.status !== 0) {
    return {
      state: 'ABSENT',
      pid: null,
      coordinate,
    };
  }

  const match = /(?:^|\n)\s*pid\s*=\s*(\d+)/.exec(String(result.stdout || ''));
  return {
    state: 'RUNNING',
    pid: match ? Number(match[1]) : null,
    coordinate,
  };
}

function receiptSummary(kind, receipt) {
  if (!receipt || typeof receipt !== 'object') {
    return {
      kind,
      present: false,
      receipt: null,
      process: null,
      launchd: null,
      exactLiveOwner: false,
    };
  }

  const recordedArgs = Array.isArray(receipt.runtime?.args) ? receipt.runtime.args : [];
  const pid = Number(receipt.runtime?.pid);
  const process = psSnapshot(pid, recordedArgs);
  const launchd = launchdSnapshot(receipt);

  const summary = {
    schemaVersion: receipt.schemaVersion ?? null,
    receiptRef: receipt.receiptRef ?? null,
    state: receipt.state ?? null,
    endpoint: receipt.endpoint ?? null,
    modelRef: receipt.modelRef ?? null,
    runtimePid: Number.isInteger(pid) ? pid : null,
    runtimeProcessGroupId: receipt.runtime?.processGroupId ?? null,
    recordedPromptCacheSize: promptCacheSize(recordedArgs),
    recordedArgsSha256: sha256(Buffer.from(JSON.stringify(recordedArgs))),
    sourceIdentityRepository: receipt.sourceIdentity?.repository ?? null,
    sourceIdentityModulePath: receipt.sourceIdentity?.modulePath ?? null,
    implementationRepository: receipt.implementationIdentity?.repository ?? null,
    implementationClassSha256: receipt.implementationIdentity?.classSha256 ?? null,
  };

  const exactLiveOwner =
    process.alive === true &&
    process.recordedArgsMatch === true &&
    Number.isInteger(summary.runtimePid) &&
    process.pid === summary.runtimePid &&
    process.processGroupId === Number(receipt.runtime?.processGroupId) &&
    receipt.runtime?.exactOwnedDetachedGroup === true &&
    launchd.state === 'RUNNING' &&
    launchd.pid === summary.runtimePid;

  return {
    kind,
    present: true,
    receipt: summary,
    process,
    launchd,
    exactLiveOwner,
  };
}

function allMlxProcesses() {
  const result = spawnSync('/bin/ps', ['-ww', '-axo', 'pid=,pgid=,command='], {
    encoding: 'utf8',
    timeout: 5000,
    maxBuffer: 16 * 1024 * 1024,
  });
  if (result.error || result.status !== 0) {
    return {
      state: 'UNKNOWN',
      processes: [],
      error: String(result.error?.message || result.stderr || 'ps failed').slice(0, 400),
    };
  }

  const processes = [];
  for (const line of String(result.stdout || '').split(/\n/)) {
    if (!/mlx_lm\.server/.test(line)) continue;
    const match = line.trim().match(/^(\d+)\s+(\d+)\s+(.*)$/);
    if (!match) continue;
    const command = match[3];
    const cache = /(?:^|\s)--prompt-cache-size(?:=|\s+)(\d+)(?:\s|$)/.exec(command);
    processes.push({
      pid: Number(match[1]),
      processGroupId: Number(match[2]),
      promptCacheSize: cache ? Number(cache[1]) : null,
      commandSha256: sha256(Buffer.from(command)),
    });
  }
  processes.sort((a, b) => a.pid - b.pid);
  return { state: 'OBSERVED', processes, error: null };
}

async function main() {
  const candidatePath = process.argv[2];
  if (!candidatePath) throw new Error('candidate binding path is required');
  const candidate = JSON.parse(fs.readFileSync(path.resolve(candidatePath), 'utf8'));

  const candidateFindings = [];
  const harnessFindings = [];
  const environmentFindings = [];
  const unknownFindings = [];
  const completedStepReceipts = [];

  const productState = await fetchJson(PRODUCT_ORIGIN + '/api/v1/current-state');
  let homePath =
    productState.ok && typeof productState.json?.homePath === 'string'
      ? productState.json.homePath
      : path.join(os.homedir(), '.vexlife');
  const homeSource =
    productState.ok && typeof productState.json?.homePath === 'string'
      ? 'PRODUCT_CURRENT_STATE'
      : 'DEFAULT_CANONICAL_HOME';

  if (!path.isAbsolute(homePath)) throw new Error('Vex Home coordinate is not absolute');
  homePath = path.normalize(homePath);

  const homeIdentityPath = path.join(homePath, 'config/home.json');
  if (!fs.existsSync(homeIdentityPath) || !fs.statSync(homeIdentityPath).isFile()) {
    writeResult({
      disposition: 'BLOCKED',
      stopReasonOrNull: 'CANONICAL_VEX_HOME_NOT_FOUND',
      firstSubstantiveFailureOrNull:
        failureRecord(new Error('canonical Vex Home is not present at the observed/default coordinate'), 'HOME_DISCOVERY'),
      candidateFindings,
      harnessFindings,
      environmentFindings,
      unknownFindings: ['CANONICAL_VEX_HOME_NOT_FOUND'],
      effects: effects(),
      beforeState: {
        candidateHead: candidate.head,
        candidateTree: candidate.tree,
        homeSource,
      },
      afterState: {
        productCurrentStateProbe: {
          ok: productState.ok,
          status: productState.status,
          error: productState.error,
        },
      },
      completedStepReceipts,
    });
    return;
  }

  completedStepReceipts.push({ step: 'canonical-home-discovery', state: 'PASS', homeSource });

  const stableReceiptPath =
    path.join(homePath, 'runtime/heretic/persistent-runtime-owner-receipt.json');
  const productReceiptPath =
    path.join(homePath, 'runtime/vexlife-prod/heretic-persistent-runtime-owner-receipt.json');

  const stableReceipt = readJsonIfPresent(stableReceiptPath);
  const productReceipt = readJsonIfPresent(productReceiptPath);

  const stable = receiptSummary('STABLE_VEXLIFE', stableReceipt);
  const product = receiptSummary('VEXLIFE_PROD', productReceipt);
  const listener = listenerPids(HERETIC_PORT);
  const mlxProcesses = allMlxProcesses();
  const health = await fetchJson(HERETIC_ORIGIN + '/health', 5000);

  const stableOwnsListener =
    stable.exactLiveOwner &&
    listener.state === 'OBSERVED' &&
    listener.pids.length === 1 &&
    listener.pids[0] === stable.receipt.runtimePid;

  const productOwnsListener =
    product.exactLiveOwner &&
    listener.state === 'OBSERVED' &&
    listener.pids.length === 1 &&
    listener.pids[0] === product.receipt.runtimePid;

  let servingOwner;
  if (stableOwnsListener && !productOwnsListener) {
    servingOwner = 'STABLE_VEXLIFE';
  } else if (productOwnsListener && !stableOwnsListener) {
    servingOwner = 'VEXLIFE_PROD';
  } else if (listener.state === 'OBSERVED' && listener.pids.length === 0) {
    servingOwner = 'NO_LISTENER';
  } else {
    servingOwner = 'UNKNOWN_OR_CONFLICT';
  }

  const servingPid =
    servingOwner === 'STABLE_VEXLIFE'
      ? stable.receipt.runtimePid
      : servingOwner === 'VEXLIFE_PROD'
        ? product.receipt.runtimePid
        : null;

  const servingProcess =
    servingPid === null
      ? null
      : mlxProcesses.processes.find((item) => item.pid === servingPid) ?? null;

  const oneCacheActive =
    servingOwner === 'VEXLIFE_PROD' &&
    servingProcess?.promptCacheSize === 1 &&
    product.receipt?.recordedPromptCacheSize === 1;

  if (servingOwner === 'STABLE_VEXLIFE') {
    candidateFindings.push('SERVING_RUNTIME_IS_STABLE_VEXLIFE_NOT_PRODUCT_188');
  } else if (servingOwner === 'VEXLIFE_PROD' && oneCacheActive) {
    completedStepReceipts.push({ step: 'product-one-cache-serving-runtime', state: 'PASS' });
  } else if (servingOwner === 'VEXLIFE_PROD') {
    candidateFindings.push('PRODUCT_RUNTIME_SERVING_WITHOUT_EXACT_ONE_CACHE_POLICY');
  } else if (servingOwner === 'UNKNOWN_OR_CONFLICT') {
    unknownFindings.push('HERETIC_SERVING_OWNER_UNKNOWN_OR_CONFLICTING');
  } else if (servingOwner === 'NO_LISTENER') {
    environmentFindings.push('NO_HERETIC_LISTENER_ON_8765');
  }

  completedStepReceipts.push(
    { step: 'stable-runtime-receipt-observation', state: stable.present ? 'PRESENT' : 'ABSENT' },
    { step: 'product-runtime-receipt-observation', state: product.present ? 'PRESENT' : 'ABSENT' },
    { step: 'port-8765-listener-observation', state: listener.state },
    { step: 'mlx-process-census', state: mlxProcesses.state },
    { step: 'loopback-health-observation', state: String(health.status ?? health.error ?? 'UNAVAILABLE') },
  );

  writeResult({
    disposition: 'PASS',
    stopReasonOrNull: null,
    firstSubstantiveFailureOrNull: null,
    candidateFindings,
    harnessFindings,
    environmentFindings,
    unknownFindings,
    effects: effects(),
    beforeState: {
      candidateHead: candidate.head,
      candidateTree: candidate.tree,
      sourcePullRequest: candidate.sourcePullRequest,
      temporaryCarrierPullRequest: candidate.pullRequest,
    },
    afterState: {
      diagnosticState: 'OBSERVED',
      homeSource,
      servingOwner,
      servingPid,
      oneCacheActive,
      stable,
      product,
      listener,
      mlxProcesses,
      health: {
        ok: health.ok,
        status: health.status,
        json: health.json,
        error: health.error,
      },
      productCurrentStateProbe: {
        ok: productState.ok,
        status: productState.status,
        sourceSha: productState.json?.sourceSha ?? null,
        activeModelRef: productState.json?.activeModelRef ?? null,
        runtimeState: productState.json?.runtimeState ?? null,
        runtimeCurrentness: productState.json?.runtimeCurrentness ?? null,
        error: productState.error,
      },
      rawConversationTextCollected: false,
      rawReasoningCollected: false,
      runtimeMutationPerformed: false,
      sourceMutationPerformed: false,
      homeMutationPerformed: false,
    },
    completedStepReceipts,
  });
}

main().catch((error) => {
  writeResult({
    disposition: 'SAFE_FAILURE',
    stopReasonOrNull: 'SERVING_OWNER_DIAGNOSTIC_FAILED',
    firstSubstantiveFailureOrNull: failureRecord(error, 'SERVING_OWNER_DIAGNOSTIC'),
    candidateFindings: [],
    harnessFindings: ['SERVING_OWNER_DIAGNOSTIC_FAILED'],
    environmentFindings: [],
    unknownFindings: [],
    effects: effects(),
    beforeState: {},
    afterState: {
      errorClass: error?.constructor?.name || 'Error',
      errorMessage: String(error?.message || error).slice(0, 1600),
      rawConversationTextCollected: false,
      rawReasoningCollected: false,
      runtimeMutationPerformed: false,
    },
    completedStepReceipts: [],
  });
  process.exitCode = 0;
});

// [VXG RealForever]
