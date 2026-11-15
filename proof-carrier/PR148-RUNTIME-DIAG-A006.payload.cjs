#!/usr/bin/env node
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const os = require('node:os');
const crypto = require('node:crypto');
const { spawnSync } = require('node:child_process');

const RESULT_SCHEMA = 'vextreme.vexlocalbridge-mac-payload-result/v1';
const PRODUCT_ORIGIN = 'http://127.0.0.1:18210';
const DEFAULT_RUNTIME_ORIGIN = 'http://127.0.0.1:8765';

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
  const summary = String(error?.message || error || 'unknown diagnostic failure').slice(0, 1000);
  return {
    failureFamily: 'VEXLIFE_PROD_RUNTIME_DIAGNOSTIC',
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
    const response = await fetch(url, { headers: { accept: 'application/json' }, signal: controller.signal });
    const text = await response.text();
    let json = null;
    try { json = JSON.parse(text); } catch (_) {}
    return { ok: response.ok, status: response.status, text, json };
  } catch (error) {
    return { ok: false, status: null, text: '', json: null, error: String(error?.message || error) };
  } finally {
    clearTimeout(timer);
  }
}

function readJsonIfPresent(file) {
  if (!fs.existsSync(file) || !fs.statSync(file).isFile()) return null;
  return JSON.parse(fs.readFileSync(file, 'utf8'));
}

function sanitize(text, homePath) {
  if (typeof text !== 'string') return text;
  let value = text;
  if (homePath) value = value.split(homePath).join('$VEX_HOME');
  value = value.split(os.homedir()).join('$HOME');
  return value;
}

function boundedFailureExcerpt(file, homePath) {
  if (!fs.existsSync(file) || !fs.statSync(file).isFile()) {
    return { present: false, file: path.basename(file), bytes: 0, excerpt: null };
  }
  const stat = fs.statSync(file);
  const maxRead = 256 * 1024;
  const start = Math.max(0, stat.size - maxRead);
  const fd = fs.openSync(file, 'r');
  try {
    const buffer = Buffer.alloc(stat.size - start);
    fs.readSync(fd, buffer, 0, buffer.length, start);
    const tail = buffer.toString('utf8');
    const markers = [
      'mlx_lm.server generation thread died',
      'Traceback (most recent call last):',
      'Out of memory',
      'out of memory',
      'MemoryError',
      'Metal',
      'RuntimeError',
      'ValueError',
      'Exception',
      'Error:',
    ];
    let markerIndex = -1;
    for (const marker of markers) {
      const index = tail.lastIndexOf(marker);
      if (index > markerIndex) markerIndex = index;
    }
    const excerpt = markerIndex >= 0
      ? tail.slice(markerIndex, Math.min(tail.length, markerIndex + 20000))
      : tail.slice(Math.max(0, tail.length - 12000));
    return {
      present: true,
      file: path.basename(file),
      bytes: stat.size,
      excerptSha256: sha256(Buffer.from(excerpt)),
      excerpt: sanitize(excerpt, homePath),
    };
  } finally {
    fs.closeSync(fd);
  }
}

function processSnapshot(pid) {
  if (!Number.isInteger(pid) || pid <= 0) return { pid: pid ?? null, alive: false, ps: null };
  let alive = true;
  try { process.kill(pid, 0); } catch (_) { alive = false; }
  const ps = spawnSync('/bin/ps', ['-ww', '-p', String(pid), '-o', 'pid=,pgid=,command='], {
    encoding: 'utf8',
    timeout: 5000,
  });
  return {
    pid,
    alive,
    psExit: ps.status,
    ps: String(ps.stdout || '').trim(),
  };
}

function receiptSummary(receipt) {
  if (!receipt || typeof receipt !== 'object') return null;
  return {
    schemaVersion: receipt.schemaVersion ?? null,
    receiptRef: receipt.receiptRef ?? null,
    state: receipt.state ?? null,
    formedAt: receipt.formedAt ?? null,
    modelRef: receipt.modelRef ?? null,
    modelProfileRef: receipt.modelProfileRef ?? null,
    artifactRef: receipt.artifactRef ?? null,
    artifactContentSetSha256: receipt.artifactContentSetSha256 ?? null,
    runtimeAdapterRef: receipt.runtimeAdapterRef ?? null,
    runtimeClass: receipt.runtimeClass ?? null,
    endpoint: receipt.endpoint ?? null,
    requestModel: receipt.requestModel ?? null,
    modelPath: receipt.modelPath ?? null,
    runtimePid: receipt.runtime?.pid ?? null,
    runtimeProcessGroupId: receipt.runtime?.processGroupId ?? null,
    runtimeArgs: Array.isArray(receipt.runtime?.args) ? receipt.runtime.args : [],
  };
}

function modelBindingSummary(binding) {
  if (!binding || typeof binding !== 'object') return null;
  return {
    schemaVersion: binding.schemaVersion ?? null,
    state: binding.state ?? null,
    modelRef: binding.modelRef ?? null,
    modelProfileRef: binding.modelProfileRef ?? null,
    runtimeReceiptRef: binding.runtimeReceiptRef ?? null,
    runtimeReceiptPath: binding.runtimeReceiptPath ?? null,
    artifactContentSetSha256: binding.artifactContentSetSha256 ?? null,
  };
}

async function main() {
  const bindingPath = process.argv[2];
  if (!bindingPath) throw new Error('candidate binding path is required');
  const candidate = JSON.parse(fs.readFileSync(path.resolve(bindingPath), 'utf8'));

  const candidateFindings = [];
  const harnessFindings = [];
  const environmentFindings = [];
  const unknownFindings = [];
  const completedStepReceipts = [];

  const currentState = await fetchJson(PRODUCT_ORIGIN + '/api/v1/current-state');
  let homePath = currentState.ok && currentState.json?.homePath
    ? String(currentState.json.homePath)
    : path.join(os.homedir(), '.vexlife');
  let homeSource = currentState.ok && currentState.json?.homePath ? 'LOCAL_SERVICE_CURRENT_STATE' : 'SOURCE_DEFAULT_HOME';

  if (!path.isAbsolute(homePath)) {
    throw new Error('discovered Vex Home is not absolute');
  }
  homePath = path.normalize(homePath);

  const homeManifest = path.join(homePath, 'config/home.json');
  if (!fs.existsSync(homeManifest)) {
    writeResult({
      disposition: 'BLOCKED',
      stopReasonOrNull: 'VEX_HOME_NOT_DISCOVERABLE',
      firstSubstantiveFailureOrNull: failureRecord(new Error('no canonical Vex Home at discovered/default coordinate'), 'HOME_DISCOVERY'),
      candidateFindings,
      harnessFindings,
      environmentFindings,
      unknownFindings: ['VEX_HOME_NOT_DISCOVERABLE'],
      effects: effects(),
      beforeState: {
        candidateHead: candidate.head,
        attemptedHomePath: homePath,
        homeSource,
      },
      afterState: {
        currentStateProbe: {
          ok: currentState.ok,
          status: currentState.status,
          error: currentState.error || null,
        },
      },
      completedStepReceipts,
    });
    return;
  }

  completedStepReceipts.push({ step: 'home-discovery', state: 'PASS', homeSource });

  const productReceiptPath = path.join(homePath, 'runtime/vexlife-prod/heretic-persistent-runtime-owner-receipt.json');
  const stableReceiptPath = path.join(homePath, 'runtime/heretic/persistent-runtime-owner-receipt.json');
  const modelBindingPath = path.join(homePath, 'runtime/vexlife-prod/model-binding.json');
  const stderrPath = path.join(homePath, 'runtime/vexlife-prod/mlx-lm-server.err.log');
  const stdoutPath = path.join(homePath, 'runtime/vexlife-prod/mlx-lm-server.out.log');

  const productReceipt = readJsonIfPresent(productReceiptPath);
  const stableReceipt = readJsonIfPresent(stableReceiptPath);
  const receipt = productReceipt || stableReceipt;
  const receiptSource = productReceipt ? 'PRODUCT_RECEIPT' : stableReceipt ? 'STABLE_COMPATIBILITY_RECEIPT' : 'NONE';
  const modelBinding = readJsonIfPresent(modelBindingPath);

  if (!receipt) {
    unknownFindings.push('NO_RUNTIME_RECEIPT_PRESENT');
  }

  const receiptView = receiptSummary(receipt);
  const process = processSnapshot(receiptView?.runtimePid ?? null);
  const endpoint = receiptView?.endpoint || DEFAULT_RUNTIME_ORIGIN;
  const health = await fetchJson(endpoint.replace(/\/$/, '') + '/health', 5000);

  if (health.status === 503) candidateFindings.push('GENERATION_ENGINE_UNAVAILABLE_HTTP_503');
  if (health.json?.status === 'unavailable') candidateFindings.push('GENERATION_ENGINE_STATUS_UNAVAILABLE');
  if (process.alive && health.status === 503) candidateFindings.push('PROCESS_ALIVE_GENERATION_ENGINE_UNAVAILABLE');

  const stderrEvidence = boundedFailureExcerpt(stderrPath, homePath);
  const stdoutEvidence = boundedFailureExcerpt(stdoutPath, homePath);

  if (stderrEvidence.present) completedStepReceipts.push({ step: 'mlx-stderr-bounded-read', state: 'PASS' });
  else environmentFindings.push('MLX_STDERR_NOT_PRESENT');

  const afterState = {
    diagnosticState: 'OBSERVED',
    candidateHead: candidate.head,
    homePath,
    homeSource,
    currentStateProbe: {
      ok: currentState.ok,
      status: currentState.status,
      sourceSha: currentState.json?.sourceSha ?? null,
      activeModelRef: currentState.json?.activeModelRef ?? null,
      runtimeState: currentState.json?.runtimeState ?? null,
      runtimeCurrentness: currentState.json?.runtimeCurrentness ?? null,
      error: currentState.error || null,
    },
    runtimeReceiptSource: receiptSource,
    runtimeReceipt: receiptView,
    selectedModelBinding: modelBindingSummary(modelBinding),
    process,
    health: {
      endpoint,
      ok: health.ok,
      status: health.status,
      json: health.json,
      error: health.error || null,
    },
    mlxStderr: stderrEvidence,
    mlxStdout: stdoutEvidence,
    rawConversationTextCollected: false,
    rawReasoningCollected: false,
    runtimeMutationPerformed: false,
  };

  completedStepReceipts.push(
    { step: 'runtime-receipt-read', state: receipt ? 'PASS' : 'ABSENT' },
    { step: 'runtime-process-observation', state: process.alive ? 'LIVE' : 'NOT_LIVE' },
    { step: 'runtime-health-observation', state: String(health.status ?? health.error ?? 'UNAVAILABLE') },
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
      sourceOwnerReturnSha256: candidate.sourceOwnerReturnSha256,
    },
    afterState,
    completedStepReceipts,
  });
}

main().catch((error) => {
  writeResult({
    disposition: 'SAFE_FAILURE',
    stopReasonOrNull: 'RUNTIME_DIAGNOSTIC_FAILED',
    firstSubstantiveFailureOrNull: failureRecord(error, 'RUNTIME_DIAGNOSTIC'),
    candidateFindings: [],
    harnessFindings: ['RUNTIME_DIAGNOSTIC_FAILED'],
    environmentFindings: [],
    unknownFindings: [],
    effects: effects(),
    beforeState: {},
    afterState: { errorClass: error?.constructor?.name || 'Error', errorMessage: String(error?.message || error).slice(0,1600) },
    completedStepReceipts: [],
  });
  process.exitCode = 0;
});

// [VXG RealForever]
