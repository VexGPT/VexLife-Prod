#!/usr/bin/env node
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const os = require('node:os');
const crypto = require('node:crypto');

const RESULT_SCHEMA = 'vextreme.vexlocalbridge-mac-payload-result/v1';

function sha256Bytes(value) {
  return crypto.createHash('sha256').update(value).digest('hex');
}

function sha256File(file) {
  return sha256Bytes(fs.readFileSync(file));
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

function writeResult(value) {
  const root = process.env.VEXLOCALBRIDGE_RESULT_DIR;
  if (!root) throw new Error('VEXLOCALBRIDGE_RESULT_DIR is unavailable');
  fs.mkdirSync(root, { recursive: true });
  fs.writeFileSync(
    path.join(root, 'payload-result.json'),
    JSON.stringify({ schemaVersion: RESULT_SCHEMA, acceptedExitCodes: [0], ...value }, null, 2) + '\n',
  );
}

function safeRealpath(file) {
  try { return fs.realpathSync(file); } catch (_) { return null; }
}

function within(child, parent) {
  const c = path.resolve(child);
  const p = path.resolve(parent);
  return c === p || c.startsWith(p + path.sep);
}

function recover() {
  if (process.platform !== 'darwin' || process.arch !== 'arm64') {
    throw new Error('PR281 orphan-inspection recovery requires Apple-Silicon macOS');
  }

  const downloads = path.join(os.homedir(), 'Downloads');
  const packagePrefix = 'VEXLIFE-PR281-ORPHAN-INSPECTION-A001--sha256-';
  const packageSuffix = '.zip';

  const packageFiles = fs.readdirSync(downloads)
    .filter((name) => name.startsWith(packagePrefix) && name.endsWith(packageSuffix))
    .map((name) => path.join(downloads, name));

  const candidates = [];
  for (const packagePath of packageFiles) {
    const name = path.basename(packagePath);
    const match = name.match(/--sha256-([a-f0-9]{64})\.zip$/);
    if (!match) continue;
    const expectedDigest = match[1];
    let observedDigest;
    try { observedDigest = sha256File(packagePath); } catch (_) { continue; }
    if (observedDigest !== expectedDigest) continue;

    const capsuleRoot = packagePath.slice(0, -4) + '-run-' + expectedDigest.slice(0, 16);
    const payloadResultPath = path.join(
      capsuleRoot,
      'bridge',
      'work',
      'vexlife-prod-pr281-orphan-inspection',
      'attempt-a002',
      'payload-result',
      'payload-result.json',
    );
    if (!fs.existsSync(payloadResultPath) || !fs.statSync(payloadResultPath).isFile()) continue;

    const payloadResult = JSON.parse(fs.readFileSync(payloadResultPath, 'utf8'));
    if (
      payloadResult.schemaVersion !== RESULT_SCHEMA ||
      payloadResult.disposition !== 'PASS' ||
      payloadResult.beforeState?.inspectionOnly !== true ||
      payloadResult.afterState?.inspectionOnly !== true
    ) {
      continue;
    }

    candidates.push({
      packagePath,
      expectedDigest,
      capsuleRoot,
      payloadResultPath,
      payloadResult,
    });
  }

  if (candidates.length !== 1) {
    throw new Error('expected exactly one recoverable prior orphan-inspection A001 capsule; found ' + candidates.length);
  }

  const prior = candidates[0];
  const priorBridgeRoot = path.join(prior.capsuleRoot, 'bridge');
  const priorBridgeReal = safeRealpath(priorBridgeRoot);
  if (!priorBridgeReal) throw new Error('prior bridge root is unavailable');

  const currentCapsuleRoot = process.env.VEXLOCALBRIDGE_CAPSULE_ROOT;
  if (!currentCapsuleRoot || !path.isAbsolute(currentCapsuleRoot)) {
    throw new Error('current VexLocalBridge capsule root is unavailable');
  }
  const currentRetainedRoot = path.join(
    currentCapsuleRoot,
    'bridge',
    'retained-diagnostics',
    process.env.VEXLOCALBRIDGE_TASK_REF || 'task',
    process.env.VEXLOCALBRIDGE_ATTEMPT_REF || 'attempt',
  );
  fs.mkdirSync(currentRetainedRoot, { recursive: true });

  const recoveredEvidence = [];
  const oldEvidence = Array.isArray(prior.payloadResult.retainedEvidenceFiles)
    ? prior.payloadResult.retainedEvidenceFiles
    : [];

  for (let index = 0; index < oldEvidence.length; index += 1) {
    const item = oldEvidence[index];
    if (
      !item ||
      typeof item.sourcePath !== 'string' ||
      !/^[a-f0-9]{64}$/.test(item.expectedSha256 || '')
    ) {
      throw new Error('prior retained-evidence binding is malformed at index ' + index);
    }

    const sourceReal = safeRealpath(item.sourcePath);
    if (!sourceReal || !within(sourceReal, priorBridgeReal)) {
      throw new Error('prior retained-evidence source escaped prior bridge root at index ' + index);
    }
    const observed = sha256File(sourceReal);
    if (observed !== item.expectedSha256) {
      throw new Error('prior retained-evidence hash mismatch at index ' + index);
    }

    const oldBase = path.basename(sourceReal).replace(/[^A-Za-z0-9._-]+/g, '_');
    const targetName = String(index + 1).padStart(2, '0') + '-' + oldBase;
    const copied = path.join(currentRetainedRoot, targetName);
    fs.copyFileSync(sourceReal, copied);
    const copiedSha = sha256File(copied);
    if (copiedSha !== observed) {
      throw new Error('recovered retained-evidence copy hash mismatch at index ' + index);
    }

    recoveredEvidence.push({
      sourcePath: copied,
      targetPath: 'retained/inner-vexrelay/orphan-inspection/' + targetName,
      expectedSha256: copiedSha,
    });
  }

  const priorPayloadBytes = fs.readFileSync(prior.payloadResultPath);
  const priorPayloadCopy = path.join(currentRetainedRoot, '00-prior-payload-result.json');
  fs.writeFileSync(priorPayloadCopy, priorPayloadBytes);
  recoveredEvidence.unshift({
    sourcePath: priorPayloadCopy,
    targetPath: 'retained/inner-vexrelay/orphan-inspection/00-prior-payload-result.json',
    expectedSha256: sha256Bytes(priorPayloadBytes),
  });

  const summary = prior.payloadResult.afterState?.summary || null;
  const priorFindings = Array.isArray(prior.payloadResult.candidateFindings)
    ? prior.payloadResult.candidateFindings
    : [];

  writeResult({
    disposition: 'PASS',
    stopReasonOrNull: null,
    firstSubstantiveFailureOrNull: null,
    candidateFindings: priorFindings,
    harnessFindings: ['RECOVERED_PRIOR_COMPLETED_INSPECTION_WITHOUT_REPLAY'],
    environmentFindings: [],
    unknownFindings: [],
    effects: defaultEffects(),
    beforeState: {
      recoveryOnly: true,
      priorPackageSha256: prior.expectedDigest,
      priorInspectionPayloadDisposition: prior.payloadResult.disposition,
    },
    afterState: {
      recoveryOnly: true,
      priorInspectionReplayed: false,
      recoveredEvidenceCount: recoveredEvidence.length,
      priorInspectionSummary: summary,
      priorInspectionAfterState: prior.payloadResult.afterState,
      priorInspectionCompletedStepReceipts: prior.payloadResult.completedStepReceipts || [],
    },
    completedStepReceipts: [
      { step: 'prior-package-digest', state: 'PASS', sha256: prior.expectedDigest },
      { step: 'prior-capsule-result-recovery', state: 'PASS' },
      { step: 'prior-retained-evidence-rehash', state: 'PASS', count: recoveredEvidence.length - 1 },
      { step: 'inspection-replay', state: 'NOT_PERFORMED' },
    ],
    retainedEvidenceFiles: recoveredEvidence,
  });
}

try {
  recover();
} catch (error) {
  writeResult({
    disposition: 'SAFE_FAILURE',
    stopReasonOrNull: 'PR281_ORPHAN_INSPECTION_RECOVERY_STOPPED',
    firstSubstantiveFailureOrNull: {
      failureFamily: 'VEXLIFE_PROD_PR281_ORPHAN_INSPECTION_RECOVERY',
      failureFingerprint: sha256Bytes(Buffer.from(String(error?.message || error))),
      stage: 'RECOVER_PRIOR_CAPSULE',
      summary: String(error?.message || error).slice(0, 4000),
      evidenceRef: 'payload-result.json',
    },
    candidateFindings: [],
    harnessFindings: ['ORPHAN_INSPECTION_RECOVERY_STOPPED'],
    environmentFindings: [],
    unknownFindings: [],
    effects: defaultEffects(),
    beforeState: { recoveryOnly: true },
    afterState: { recoveryOnly: true, priorInspectionReplayed: false },
    completedStepReceipts: [{ step: 'inspection-replay', state: 'NOT_PERFORMED' }],
    retainedEvidenceFiles: [],
  });
}

// [VXG RealForever]
