#!/usr/bin/env node
'use strict';

const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');

const [sdkRootArg, payloadPathArg] = process.argv.slice(2);
if (!sdkRootArg || !payloadPathArg) {
  throw new Error('usage: PR148-LIVED-A010.form.mjs <sdk-root> <payload-path>');
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
  head: 'c07e77d23cadfb894c6bc9ad18570b85a45c9896',
  tree: '3880fedcf11a181a66752dfc1cb48d7c79d033cc',
  branch: 'VXG-111526-vexlife-clean-convergence-a001',
  sourceOwnerTaskRef: 'task.vexlife-prod.pr148.clean-convergence.source-owner.001',
  sourceOwnerAttemptRef: 'attempt.vexlife-prod.pr148.clean-convergence.source-owner.a008',
};

const authorityEnvelope = {
  authoritySchemaRef: 'vextreme.effect-transaction/v1',
  semanticAdmissionRef: 'github.pull.vexlife-prod.148',
  transactionRef: 'transaction.vexlife-prod.pr148.lived-circuit.a010',
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

const authorityRelativePath = 'authority/effect.json';
const payloadRelativePath = 'payload/run.cjs';
const candidateRelativePath = 'inputs/candidate.json';

const contentRoot = fs.mkdtempSync(path.join(os.tmpdir(), 'vexlife-prod-pr148-lived-a010-form-'));
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
    throw new Error('PR148_LIVED_A010_PAYLOAD_STAGING_MISSING');
  }
  if (!fs.existsSync(candidatePath) || !fs.statSync(candidatePath).isFile()) {
    throw new Error('PR148_LIVED_A010_CANDIDATE_STAGING_MISSING');
  }


  const taskTemplate = bindAuthoritySummaryFromEnvelope({
    schemaVersion: 'vextreme.vexlocalbridge-task-manifest/v1',
    artifactClass: 'EXECUTABLE_RELAY',
    taskRef: 'task.vexlife-prod.pr148.lived-circuit.001',
    attemptRef: 'attempt.vexlife-prod.pr148.lived-circuit.a010',
    packageRef: 'package.vexlife-prod.pr148.lived-circuit.a010',
    requestRef: 'request.vexlife-prod.pr148.lived-circuit.a010',
    correlationRef: 'correlation.vexlife-prod.pr148.lived-circuit.a010',
    taskStorageKey: 'vexlife-prod-pr148-lived-circuit',
    attemptStorageKey: 'attempt-a010',
    requestingRoleRef: 'Operations[VexLife-Prod]',
    requestingInstanceRef: 'instance.operations.vexlife-prod.pr148.lived-circuit.a010',
    parentOperationsRef: 'github.pull.vexlife-prod.148',
    returnRouteRef: 'github.pull.vexlife-prod.148',
    hostProfileRef: 'macos-victor',
    repositoryExecutionProfileRefOrNull: 'profile.repository.vextreme-sdk.macos-v1',
    repositoryRefOrNull: null,
    baseRefOrNull: null,
    headRefOrNull: null,
    treeRefOrNull: null,
    branchRefOrNull: null,
    claimRefOrNull: 'github.pull.vexlife-prod.148',
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
      payloadClass: 'VEXLIFE_PROD_FAILURE_RETRY_LIVED',
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
    singleUseNonce: 'nonce-vexlife-prod-pr148-lived-a010-20261115',
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
    outputStem: 'VEXLIFE-PROD-PR148-LIVED-A010',
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
