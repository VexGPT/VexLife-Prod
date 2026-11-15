#!/usr/bin/env node
'use strict';

const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');

const [sdkRootArg, payloadPathArg] = process.argv.slice(2);
if (!sdkRootArg || !payloadPathArg) {
  throw new Error('usage: PR149-CLOSURE-A001.form.mjs <sdk-root> <payload-path>');
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
  pullRequest: 149,
  base: 'b4699fb85d85a1c5ec1b14db83cf59e4171d74d1',
  head: '22616f2fe57285665276d697812ff9005b52c6bc',
  tree: '0064000b0d1263520e7aa35817625c182cbb6d85',
  branch: 'VXG-111526-pr148-conversation-closure-a001',
  sourceOwnerTaskRef: null,
  sourceOwnerAttemptRef: null,
};

const authorityEnvelope = {
  authoritySchemaRef: 'vextreme.effect-transaction/v1',
  semanticAdmissionRef: 'github.pull.vexlife-prod.149',
  transactionRef: 'transaction.vexlife-prod.pr149.conversation-closure.a001',
  transactionVersion: 1,
  allowedEffectRefs: [
    'effect.vexlife-prod.pr149.exact-source-read',
    'effect.vexlife-prod.pr149.private-image-build',
    'effect.vexlife-prod.pr149.native-setup',
    'effect.vexlife-prod.pr149.home-convergence-if-human-admitted',
    'effect.vexlife-prod.pr149.selected-model-convergence-if-human-admitted',
    'effect.vexlife-prod.pr149.model-runtime-inference',
    'effect.vexlife-prod.pr149.browser-lived-circuit',
    'effect.vexlife-prod.pr149.web-read-capability',
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

const authorityRelativePath = 'authority/effect.json';
const payloadRelativePath = 'payload/run.cjs';
const candidateRelativePath = 'inputs/candidate.json';

const contentRoot = fs.mkdtempSync(path.join(os.tmpdir(), 'vexlife-prod-pr149-closure-a001-form-'));
try {
  const authorityPath = path.join(contentRoot, authorityRelativePath);
  const payloadPath = path.join(contentRoot, payloadRelativePath);
  const candidatePath = path.join(contentRoot, candidateRelativePath);

  fs.mkdirSync(path.dirname(authorityPath), { recursive: true });
  fs.mkdirSync(path.dirname(payloadPath), { recursive: true });
  fs.mkdirSync(path.dirname(candidatePath), { recursive: true });

  writeJson(authorityPath, authorityEnvelope);
  fs.copyFileSync(payloadSource, payloadPath);
  writeJson(candidatePath, candidate);

  if (!fs.existsSync(payloadPath) || !fs.statSync(payloadPath).isFile()) {
    throw new Error('PR149_CLOSURE_A001_PAYLOAD_STAGING_MISSING');
  }
  if (!fs.existsSync(candidatePath) || !fs.statSync(candidatePath).isFile()) {
    throw new Error('PR149_CLOSURE_A001_CANDIDATE_STAGING_MISSING');
  }


  const taskTemplate = bindAuthoritySummaryFromEnvelope({
    schemaVersion: 'vextreme.vexlocalbridge-task-manifest/v1',
    artifactClass: 'EXECUTABLE_RELAY',
    taskRef: 'task.vexlife-prod.pr149.conversation-closure.001',
    attemptRef: 'attempt.vexlife-prod.pr149.conversation-closure.a001',
    packageRef: 'package.vexlife-prod.pr149.conversation-closure.a001',
    requestRef: 'request.vexlife-prod.pr149.conversation-closure.a001',
    correlationRef: 'correlation.vexlife-prod.pr149.conversation-closure.a001',
    taskStorageKey: 'vexlife-prod-pr149-conversation-closure',
    attemptStorageKey: 'attempt-a001',
    requestingRoleRef: 'Operations[VexLife-Prod]',
    requestingInstanceRef: 'instance.operations.vexlife-prod.pr149.conversation-closure.a001',
    parentOperationsRef: 'github.pull.vexlife-prod.149',
    returnRouteRef: 'github.pull.vexlife-prod.149',
    hostProfileRef: 'macos-victor',
    repositoryExecutionProfileRefOrNull: 'profile.repository.vextreme-sdk.macos-v1',
    repositoryRefOrNull: null,
    baseRefOrNull: null,
    headRefOrNull: null,
    treeRefOrNull: null,
    branchRefOrNull: null,
    claimRefOrNull: 'github.pull.vexlife-prod.149',
    exactPathRefs: [],
    authorityBinding: {
      authoritySchemaRef: authorityEnvelope.authoritySchemaRef,
      authorityEnvelopePath: authorityRelativePath,
      authorityEnvelopeSha256: hashFile(authorityPath),
      semanticAdmissionRef: authorityEnvelope.semanticAdmissionRef,
      transactionRef: authorityEnvelope.transactionRef,
      transactionVersion: authorityEnvelope.transactionVersion,
      allowedEffectRefs: [...authorityEnvelope.allowedEffectRefs],
      heldEffectRefs: [...authorityEnvelope.heldEffectRefs],
    },
    payload: {
      payloadClass: 'VEXLIFE_PROD_CONVERSATION_CLOSURE_LIVED',
      payloadRuntime: 'node',
      payloadPath: payloadRelativePath,
      payloadSha256: hashFile(payloadPath),
      typedArgv: [candidateRelativePath],
      immutableInputs: [{
        path: candidateRelativePath,
        sha256: hashFile(candidatePath),
      }],
    },
    executionStateAtFormation: 'TASK_PREPARED',
    singleUseNonce: 'nonce-vexlife-prod-pr149-closure-a001-20261115',
    stopConditions: [
      'stop.on.exact-source-drift',
      'stop.on.native-setup-failure',
      'stop.on.first-turn-failure',
      'stop.on.bound-expiry',
    ],
    archiveDigestBinding: 'FILENAME_SHA256',
  }, authorityEnvelope);

  const formed = createCanonicalTaskZip({
    taskTemplate,
    contentRoot,
    outputDirectory: path.join(os.homedir(), 'Downloads'),
    outputStem: 'VEXLIFE-PROD-PR149-CLOSURE-A001',
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
