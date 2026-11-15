#!/usr/bin/env node
'use strict';

const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');

const [sdkRootArg, payloadPathArg] = process.argv.slice(2);
if (!sdkRootArg || !payloadPathArg) {
  throw new Error('usage: PR148-LIVED-A004.form.mjs <sdk-root> <payload-path>');
}

const sdkRoot = path.resolve(sdkRootArg);
const payloadSource = path.resolve(payloadPathArg);
const packageBuilder = require(path.join(sdkRoot, 'lib/vexlocalbridge/package-builder.js'));
const {
  createCanonicalTaskZip,
  writeJson,
  hashFile,
  bindAuthoritySummaryFromEnvelope,
} = packageBuilder;

const candidate = {
  schemaVersion: 'vexlife-prod.lived-circuit-candidate/v1',
  continuity: '[VXG RealForever]',
  repository: 'VexGPT/VexLife-Prod',
  pullRequest: 148,
  base: '035335a34d4220d6d3242c088a1a9667c29f135f',
  head: 'bec6021327ad333dd2db85abdf899e249a3d9c11',
  tree: 'bf56a066aa6b63d3cdc509a9e6641688951c4181',
  branch: 'VXG-111526-vexlife-clean-convergence-a001',
  sourceOwnerReturnSha256: 'afd03e75da8b25639b82417ac67b0158ca5e830735aa58d390cabb7ca402e546',
  sourceOwnerInnerReturnSha256: '6b0b3f82924d66a7280f20ab8d2b4583b2f66865a86ef5f26720099558e45fea',
};

const authorityEnvelope = {
  authoritySchemaRef: 'vextreme.effect-transaction/v1',
  semanticAdmissionRef: 'github.pull.vexlife-prod.148',
  transactionRef: 'transaction.vexlife-prod.pr148.lived-circuit.a004',
  transactionVersion: 1,
  allowedEffectRefs: [
    'effect.vexlife-prod.pr148.exact-source-read',
    'effect.vexlife-prod.pr148.private-image-build',
    'effect.vexlife-prod.pr148.native-setup',
    'effect.vexlife-prod.pr148.home-convergence-if-human-admitted',
    'effect.vexlife-prod.pr148.selected-model-convergence-if-human-admitted',
    'effect.vexlife-prod.pr148.model-runtime-inference',
    'effect.vexlife-prod.pr148.browser-lived-circuit',
    'effect.vexlife-prod.pr148.web-read-capability',
  ],
  heldEffectRefs: [
    'effect.vexlife-prod.source-mutation',
    'effect.vexlife-prod.branch-mutation',
    'effect.vexlife-prod.pr-metadata-mutation',
    'effect.vexlife-prod.ready',
    'effect.vexlife-prod.merge',
    'effect.vexlife-prod.publication',
    'effect.vexlife-prod.memory-dream',
    'effect.vexlife-prod.training',
  ],
};

const contentRoot = fs.mkdtempSync(path.join(os.tmpdir(), 'vexlife-prod-pr148-lived-a004-form-'));
try {
  const authorityPath = path.join(contentRoot, 'authority/effect.json');
  const payloadPath = path.join(contentRoot, 'payload/run.mjs');
  const candidatePath = path.join(contentRoot, 'inputs/candidate.json');

  fs.mkdirSync(path.dirname(authorityPath), { recursive: true });
  fs.mkdirSync(path.dirname(payloadPath), { recursive: true });
  fs.mkdirSync(path.dirname(candidatePath), { recursive: true });

  writeJson(authorityPath, authorityEnvelope);
  fs.copyFileSync(payloadSource, payloadPath);
  writeJson(candidatePath, candidate);

  const taskTemplate = bindAuthoritySummaryFromEnvelope({
    schemaVersion: 'vextreme.vexlocalbridge-task-manifest/v1',
    artifactClass: 'EXECUTABLE_RELAY',
    taskRef: 'task.vexlife-prod.pr148.lived-circuit.001',
    attemptRef: 'attempt.vexlife-prod.pr148.lived-circuit.a004',
    packageRef: 'package.vexlife-prod.pr148.lived-circuit.a004',
    requestRef: 'request.vexlife-prod.pr148.lived-circuit.a004',
    correlationRef: 'correlation.vexlife-prod.pr148.lived-circuit.a004',
    taskStorageKey: 'vexlife-prod-pr148-lived-circuit',
    attemptStorageKey: 'attempt-a004',
    requestingRoleRef: 'Operations[VexLife-Prod]',
    requestingInstanceRef: 'instance.operations.vexlife-prod.pr148.lived-circuit.a004',
    parentOperationsRef: 'github.pull.vexlife-prod.148',
    returnRouteRef: 'github.pull.vexlife-prod.148',
    hostProfileRef: 'macos-victor',
    repositoryExecutionProfileRefOrNull: 'profile.repository.vextreme-sdk.macos-v1',
    repositoryRefOrNull: candidate.repository,
    baseRefOrNull: candidate.base,
    headRefOrNull: candidate.head,
    treeRefOrNull: candidate.tree,
    branchRefOrNull: candidate.branch,
    claimRefOrNull: 'github.pull.vexlife-prod.148',
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
      payloadClass: 'VEXLIFE_PROD_HOST_SEMANTIC_LIVED_CIRCUIT',
      payloadRuntime: 'node',
      payloadPath: 'payload/run.mjs',
      payloadSha256: hashFile(payloadPath),
      typedArgv: ['inputs/candidate.json'],
      immutableInputs: [{
        path: 'inputs/candidate.json',
        sha256: hashFile(candidatePath),
      }],
    },
    executionStateAtFormation: 'TASK_PREPARED',
    singleUseNonce: 'nonce-vexlife-prod-pr148-lived-a004-20261115',
    stopConditions: [
      'stop.on.exact-source-drift',
      'stop.on.native-setup-failure',
      'stop.on.turn-failure',
      'stop.on.bound-expiry',
    ],
    archiveDigestBinding: 'FILENAME_SHA256',
  }, authorityEnvelope);

  const formed = createCanonicalTaskZip({
    taskTemplate,
    contentRoot,
    outputDirectory: path.join(os.homedir(), 'Downloads'),
    outputStem: 'VEXLIFE-PROD-PR148-LIVED-A004',
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
    finalByteQualification: formed.formationEvidence.finalByteQualification,
  }, null, 2) + '\n');
} finally {
  fs.rmSync(contentRoot, { recursive: true, force: true });
}

// [VXG RealForever]
