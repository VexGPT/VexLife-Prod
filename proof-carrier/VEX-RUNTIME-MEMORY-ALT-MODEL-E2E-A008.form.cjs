#!/usr/bin/env node
'use strict';

const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');

const [sdkRootArg, payloadPathArg] = process.argv.slice(2);
if (!sdkRootArg || !payloadPathArg) {
  throw new Error('usage: VEX-RUNTIME-MEMORY-END-TO-END-A008.form.mjs <sdk-root> <payload-path>');
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
  pullRequest: 200,
  base: '20c0ff9e674dd04d6cdbfb92bf4e1cbb9363f745',
  head: '76c672837d644affcc4e5b63149de47cbe51441d',
  tree: '121a7a40eb8e5961d3f500a94f302a60fcf25dc8',
  branch: 'VXG-111626-runtime-memory-composed-alt-model-a001',
  sourceOwnerTaskRef: null,
  sourceOwnerAttemptRef: null,
  explicitModelHandle: 'heretic-27b',
  expectedActiveModelRef: 'model.devex.heretic.qwen3.8-27b.mlx4.329658ee8f27',
  parentCompositionPullRequest: 197,
};

const authorityEnvelope = {
  authoritySchemaRef: 'vextreme.effect-transaction/v1',
  semanticAdmissionRef: 'github.pull.vexlife-prod.200',
  transactionRef: 'transaction.vexlife-prod.runtime-memory.alt-model-end-to-end.a008',
  transactionVersion: 1,
  allowedEffectRefs: [
    'effect.vexlife-prod.runtime-memory.exact-source-read',
    'effect.vexlife-prod.runtime-memory.private-image-build',
    'effect.vexlife-prod.runtime-memory.native-setup',
    'effect.vexlife-prod.runtime-memory.home-convergence-if-human-admitted',
    'effect.vexlife-prod.runtime-memory.explicit-human-model-choice-heretic-27b',
    'effect.vexlife-prod.runtime-memory.exact-owned-runtime-rotation',
    'effect.vexlife-prod.runtime-memory.model-runtime-inference',
    'effect.vexlife-prod.runtime-memory.browser-lived-circuit',
    'effect.vexlife-prod.runtime-memory.content-free-chronology',
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
    'effect.vexlife-prod.model-quality-reduction',
    'effect.vexlife-prod.kv-quantization',
    'effect.vexlife-prod.context-truncation',
  ],
};

const authorityRelativePath = 'authority/effect.json';
const payloadRelativePath = 'payload/run.cjs';
const candidateRelativePath = 'inputs/candidate.json';

const contentRoot = fs.mkdtempSync(path.join(os.tmpdir(), 'vexlife-prod-runtime-memory-end-to-end-a008-form-'));
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
    taskRef: 'task.vexlife-prod.runtime-memory.alt-model-end-to-end.001',
    attemptRef: 'attempt.vexlife-prod.runtime-memory.alt-model-end-to-end.a008',
    packageRef: 'package.vexlife-prod.runtime-memory.alt-model-end-to-end.a008',
    requestRef: 'request.vexlife-prod.runtime-memory.alt-model-end-to-end.a008',
    correlationRef: 'correlation.vexlife-prod.runtime-memory.alt-model-end-to-end.a008',
    taskStorageKey: 'vexlife-prod-runtime-memory-end-to-end',
    attemptStorageKey: 'attempt-a008',
    requestingRoleRef: 'Operations[VexLife-Prod]',
    requestingInstanceRef: 'instance.operations.vexlife-prod.runtime-memory.alt-model-end-to-end.a008',
    parentOperationsRef: 'github.pull.vexlife-prod.200',
    returnRouteRef: 'github.pull.vexlife-prod.200',
    hostProfileRef: 'macos-victor',
    repositoryExecutionProfileRefOrNull: 'profile.repository.vextreme-sdk.macos-v1',
    repositoryRefOrNull: null,
    baseRefOrNull: null,
    headRefOrNull: null,
    treeRefOrNull: null,
    branchRefOrNull: null,
    claimRefOrNull: 'github.pull.vexlife-prod.200',
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
      payloadClass: 'VEXLIFE_PROD_RUNTIME_MEMORY_ALT_MODEL_END_TO_END',
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
    singleUseNonce: 'nonce-vexlife-prod-runtime-memory-end-to-end-a008-20261116',
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
    outputStem: 'VEXLIFE-RUNTIME-MEMORY-ALT-MODEL-E2E-A008',
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
