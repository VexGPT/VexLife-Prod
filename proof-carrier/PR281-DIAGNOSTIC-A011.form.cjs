#!/usr/bin/env node
'use strict';

const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');

const [sdkRootArg, payloadPathArg] = process.argv.slice(2);
if (!sdkRootArg || !payloadPathArg) throw new Error('usage: PR281-DIAGNOSTIC-A011.form.cjs <sdk-root> <payload>');

const sdkRoot = path.resolve(sdkRootArg);
const payloadSource = path.resolve(payloadPathArg);
const {
  createCanonicalTaskZip,
  writeJson,
  hashFile,
  bindAuthoritySummaryFromEnvelope,
} = require(path.join(sdkRoot, 'lib/vexlocalbridge/package-builder.js'));

const candidate = {
  schemaVersion: 'vexlife-prod.pr281-diagnostic-candidate/v1',
  continuity: '[VXG RealForever]',
  repository: 'VexGPT/VexLife-Prod',
  pullRequest: 281,
  branch: 'VXG-111826-chatgpt-service-home-lifetime-a001',
  base: 'feacef19e5fdf0f4341ac85c8669ba82675714c2',
  head: '53b6f00b75758375589428dbfcdf7f741a98d2d1',
  tree: '7a9de20ab8260d5c43181d10c2bd91594324a539',
};

const authorityEnvelope = {
  authoritySchemaRef: 'vextreme.effect-transaction/v1',
  semanticAdmissionRef: 'github.pull.vexlife-prod.281',
  transactionRef: 'transaction.vexlife-prod.pr281.diagnostic.a011',
  transactionVersion: 1,
  allowedEffectRefs: [
    'effect.vexlife-prod.pr281.exact-source-read',
    'effect.vexlife-prod.pr281.local-build-and-test-proof',
    'effect.vexlife-prod.pr281.diagnostic-evidence-retention',
  ],
  heldEffectRefs: [
    'effect.vexlife-prod.source-mutation',
    'effect.vexlife-prod.branch-mutation',
    'effect.vexlife-prod.pr-metadata-mutation',
    'effect.vexlife-prod.ready',
    'effect.vexlife-prod.merge',
    'effect.vexlife-prod.publication',
    'effect.vexlife-prod.host-installation',
    'effect.vexlife-prod.home-mutation',
    'effect.vexlife-prod.runtime-lifecycle',
  ],
};

const root = fs.mkdtempSync(path.join(os.tmpdir(), 'vexlife-pr281-diagnostic-a011-form-'));
try {
  const authorityPath = path.join(root, 'authority/effect.json');
  const payloadPath = path.join(root, 'payload/run.cjs');
  const candidatePath = path.join(root, 'inputs/candidate.json');
  fs.mkdirSync(path.dirname(authorityPath), { recursive: true });
  fs.mkdirSync(path.dirname(payloadPath), { recursive: true });
  fs.mkdirSync(path.dirname(candidatePath), { recursive: true });
  writeJson(authorityPath, authorityEnvelope);
  fs.copyFileSync(payloadSource, payloadPath);
  writeJson(candidatePath, candidate);

  const task = bindAuthoritySummaryFromEnvelope({
    schemaVersion: 'vextreme.vexlocalbridge-task-manifest/v1',
    artifactClass: 'EXECUTABLE_RELAY',
    taskRef: 'task.vexlife-prod.pr281.diagnostic.011',
    attemptRef: 'attempt.vexlife-prod.pr281.diagnostic.a011',
    packageRef: 'package.vexlife-prod.pr281.diagnostic.a011',
    requestRef: 'request.vexlife-prod.pr281.diagnostic.a011',
    correlationRef: 'correlation.vexlife-prod.pr281.diagnostic.a011',
    taskStorageKey: 'vexlife-prod-pr281-diagnostic',
    attemptStorageKey: 'attempt-a011',
    requestingRoleRef: 'Operations[VexLife-Prod]',
    requestingInstanceRef: 'instance.chatgpt.vexlife-prod.pr281.diagnostic.a011',
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
      payloadClass: 'VEXLIFE_PROD_PR281_DIAGNOSTIC',
      payloadRuntime: 'node',
      payloadPath: 'payload/run.cjs',
      payloadSha256: hashFile(payloadPath),
      typedArgv: ['inputs/candidate.json'],
      immutableInputs: [{ path: 'inputs/candidate.json', sha256: hashFile(candidatePath) }],
    },
    executionStateAtFormation: 'TASK_PREPARED',
    singleUseNonce: 'nonce-vexlife-prod-pr281-diagnostic-a011-20261118',
    stopConditions: ['stop.on.exact-source-drift','stop.on.diagnostic-stage-failure','stop.on.bound-expiry'],
    archiveDigestBinding: 'FILENAME_SHA256',
  }, authorityEnvelope);

  const formed = createCanonicalTaskZip({
    taskTemplate: task,
    contentRoot: root,
    outputDirectory: path.join(os.homedir(), 'Downloads'),
    outputStem: 'VEXLIFE-PR281-DIAGNOSTIC-A011',
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
