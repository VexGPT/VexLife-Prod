#!/usr/bin/env node
'use strict';

const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');

const [sdkRootArg, payloadPathArg] = process.argv.slice(2);
if (!sdkRootArg || !payloadPathArg) {
  throw new Error('usage: PR281-ORPHAN-INSPECTION-RECOVERY-A002.form.cjs <sdk-root> <payload>');
}

const sdkRoot = path.resolve(sdkRootArg);
const payloadSource = path.resolve(payloadPathArg);
const {
  createCanonicalTaskZip,
  writeJson,
  hashFile,
  bindAuthoritySummaryFromEnvelope,
} = require(path.join(sdkRoot, 'lib/vexlocalbridge/package-builder.js'));

const authorityEnvelope = {
  authoritySchemaRef: 'vextreme.effect-transaction/v1',
  semanticAdmissionRef: 'github.pull.vexlife-prod.281',
  transactionRef: 'transaction.vexlife-prod.pr281.orphan-inspection-recovery.a002',
  transactionVersion: 1,
  allowedEffectRefs: [
    'effect.vexlife-prod.pr281.orphan-inspection-evidence-recovery',
    'effect.vexlife-prod.pr281.inspection-evidence-retention',
  ],
  heldEffectRefs: [
    'effect.vexlife-prod.pr281.host-lifecycle-inspection',
    'effect.vexlife-prod.pr281.loopback-currentness-inspection',
    'effect.vexlife-prod.source-mutation',
    'effect.vexlife-prod.branch-mutation',
    'effect.vexlife-prod.pr-metadata-mutation',
    'effect.vexlife-prod.ready',
    'effect.vexlife-prod.merge',
    'effect.vexlife-prod.publication',
    'effect.vexlife-prod.host-installation',
    'effect.vexlife-prod.home-mutation',
    'effect.vexlife-prod.runtime-lifecycle',
    'effect.vexlife-prod.service-retirement',
    'effect.vexlife-prod.service-start',
  ],
};

const root = fs.mkdtempSync(path.join(os.tmpdir(), 'vexlife-pr281-orphan-inspection-recovery-a001-form-'));
try {
  const authorityPath = path.join(root, 'authority/effect.json');
  const payloadPath = path.join(root, 'payload/run.cjs');
  fs.mkdirSync(path.dirname(authorityPath), { recursive: true });
  fs.mkdirSync(path.dirname(payloadPath), { recursive: true });
  writeJson(authorityPath, authorityEnvelope);
  fs.copyFileSync(payloadSource, payloadPath);

  const task = bindAuthoritySummaryFromEnvelope({
    schemaVersion: 'vextreme.vexlocalbridge-task-manifest/v1',
    artifactClass: 'EXECUTABLE_RELAY',
    taskRef: 'task.vexlife-prod.pr281.orphan-inspection-recovery.002',
    attemptRef: 'attempt.vexlife-prod.pr281.orphan-inspection-recovery.a002',
    packageRef: 'package.vexlife-prod.pr281.orphan-inspection-recovery.a002',
    requestRef: 'request.vexlife-prod.pr281.orphan-inspection-recovery.a002',
    correlationRef: 'correlation.vexlife-prod.pr281.orphan-inspection-recovery.a002',
    taskStorageKey: 'vexlife-prod-pr281-orphan-inspection-recovery',
    attemptStorageKey: 'attempt-a002',
    requestingRoleRef: 'Operations[VexLife-Prod]',
    requestingInstanceRef: 'instance.chatgpt.vexlife-prod.pr281.orphan-inspection-recovery.a002',
    parentOperationsRef: 'github.pull.vexlife-prod.281',
    returnRouteRef: 'github.pull.vexlife-prod.281',
    hostProfileRef: 'macos-victor',
    repositoryExecutionProfileRefOrNull: 'profile.repository.vextreme-sdk.macos-v1',
    repositoryRefOrNull: null,
    baseRefOrNull: null,
    headRefOrNull: null,
    treeRefOrNull: null,
    branchRefOrNull: null,
    claimRefOrNull: 'github.pull.vexlife-prod.281',
    exactPathRefs: [],
    authorityBinding: {
      authoritySchemaRef: authorityEnvelope.authoritySchemaRef,
      authorityEnvelopePath: 'authority/effect.json',
      authorityEnvelopeSha256: hashFile(authorityPath),
      semanticAdmissionRef: authorityEnvelope.semanticAdmissionRef,
      transactionRef: authorityEnvelope.transactionRef,
      transactionVersion: authorityEnvelope.transactionVersion,
      allowedEffectRefs: [...authorityEnvelope.allowedEffectRefs],
      heldEffectRefs: [...authorityEnvelope.heldEffectRefs],
    },
    payload: {
      payloadClass: 'VEXLIFE_PROD_PR281_ORPHAN_INSPECTION_RECOVERY',
      payloadRuntime: 'node',
      payloadPath: 'payload/run.cjs',
      payloadSha256: hashFile(payloadPath),
      typedArgv: [],
      immutableInputs: [],
    },
    executionStateAtFormation: 'TASK_PREPARED',
    singleUseNonce: 'nonce-vexlife-prod-pr281-orphan-inspection-recovery-a002-20261118',
    stopConditions: [
      'stop.on.prior-capsule-absent-or-ambiguous',
      'stop.on.prior-evidence-hash-mismatch',
      'stop.on.bound-expiry',
    ],
    archiveDigestBinding: 'FILENAME_SHA256',
  }, authorityEnvelope);

  const formed = createCanonicalTaskZip({
    taskTemplate: task,
    contentRoot: root,
    outputDirectory: path.join(os.homedir(), 'Downloads'),
    outputStem: 'VEXLIFE-PR281-ORPHAN-INSPECTION-RECOVERY-A002',
    ttlMs: 4 * 60 * 60 * 1000,
  });

  process.stdout.write(JSON.stringify({
    state: 'PASS',
    package: formed.finalZip,
    sha256: formed.digest,
    taskRef: formed.task.taskRef,
    attemptRef: formed.task.attemptRef,
    formedAt: formed.formationEvidence.formedAt,
    expiresAt: formed.formationEvidence.expiresAt,
  }, null, 2) + '\n');
} finally {
  fs.rmSync(root, { recursive: true, force: true });
}

// [VXG RealForever]
