#!/usr/bin/env node
'use strict';

const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');

const [sdkRootArg, payloadPathArg] = process.argv.slice(2);
if (!sdkRootArg || !payloadPathArg) {
  throw new Error('usage: VEX-RUNTIME-MEMORY-CURRENTIZE-A004.form.cjs <sdk-root> <payload-path>');
}

const sdkRoot = path.resolve(sdkRootArg);
const payloadSource = path.resolve(payloadPathArg);
const {
  createCanonicalTaskZip,
  writeJson,
  hashFile,
  bindAuthoritySummaryFromEnvelope,
} = require(path.join(sdkRoot, 'lib/vexlocalbridge/package-builder.js'));

const candidate = {
  schemaVersion: 'vexlife-prod.runtime-memory-currentization-candidate/v1',
  continuity: '[VXG RealForever]',
  repository: 'VexGPT/VexLife-Prod',
  pullRequest: 193,
  sourcePullRequest: 188,
  base: '655e7c78cefba06a5e87afad285bb25a08632f05',
  head: '238608c6a3659f81a6b44496f8f1dd9231737efc',
  tree: '4c2703d645041839a26ae9406cdd8a3150ec2242',
  branch: 'VXG-111626-runtime-memory-currentized-test-a001',
  prerequisiteReturnSha256:
    '3a6cb533441183edbe50198c5915cf654b6c0de39a1a3bf049b4587b16c909f5',
  priorHarnessFailure: 'A004_HOST_PREFLIGHT_PROCESS_SHADOW_NO_HOST_EFFECT',
  prerequisiteTruth: {
    servingOwner: 'VEXLIFE_PROD',
    oneCacheActive: false,
    stableExactLiveOwner: false,
    productExactLiveOwner: true,
  },
};

const authorityEnvelope = {
  authoritySchemaRef: 'vextreme.effect-transaction/v1',
  semanticAdmissionRef: 'github.pull.vexlife-prod.193',
  transactionRef: 'transaction.vexlife-prod.runtime-memory.currentize-one-cache.a005',
  transactionVersion: 1,
  allowedEffectRefs: [
    'effect.vexlife-prod.runtime-memory.exact-source-read',
    'effect.vexlife-prod.runtime-memory.source-build-proof',
    'effect.vexlife-prod.runtime-memory.mac-package-proof',
    'effect.vexlife-prod.runtime-memory.front-door-inspection',
    'effect.vexlife-prod.runtime-memory.product-convergence-without-acquisition',
    'effect.vexlife-prod.runtime-memory.exact-owned-runtime-rotation',
    'effect.vexlife-prod.runtime-memory.private-image-install',
    'effect.vexlife-prod.runtime-memory.product-service-rotation',
    'effect.vexlife-prod.runtime-memory.home-runtime-binding-convergence',
    'effect.vexlife-prod.runtime-memory.currentness-read',
  ],
  heldEffectRefs: [
    'effect.vexlife-prod.model-acquisition',
    'effect.vexlife-prod.model-selection-change',
    'effect.vexlife-prod.conversation-send',
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

const contentRoot = fs.mkdtempSync(
  path.join(os.tmpdir(), 'vexlife-prod-runtime-memory-currentize-a005-form-'),
);

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

  const taskTemplate = bindAuthoritySummaryFromEnvelope({
    schemaVersion: 'vextreme.vexlocalbridge-task-manifest/v1',
    artifactClass: 'EXECUTABLE_RELAY',
    taskRef: 'task.vexlife-prod.runtime-memory.currentize-one-cache.001',
    attemptRef: 'attempt.vexlife-prod.runtime-memory.currentize-one-cache.a005',
    packageRef: 'package.vexlife-prod.runtime-memory.currentize-one-cache.a005',
    requestRef: 'request.vexlife-prod.runtime-memory.currentize-one-cache.a005',
    correlationRef: 'correlation.vexlife-prod.runtime-memory.currentize-one-cache.a005',
    taskStorageKey: 'vexlife-prod-runtime-memory-currentize-one-cache',
    attemptStorageKey: 'attempt-a005',
    requestingRoleRef: 'Operations[VexLife-Prod]',
    requestingInstanceRef:
      'instance.operations.vexlife-prod.runtime-memory.currentize-one-cache.a005',
    parentOperationsRef: 'github.pull.vexlife-prod.193',
    returnRouteRef: 'github.pull.vexlife-prod.193',
    hostProfileRef: 'macos-victor',
    repositoryExecutionProfileRefOrNull: 'profile.repository.vextreme-sdk.macos-v1',
    repositoryRefOrNull: null,
    baseRefOrNull: null,
    headRefOrNull: null,
    treeRefOrNull: null,
    branchRefOrNull: null,
    claimRefOrNull: 'github.pull.vexlife-prod.193',
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
      payloadClass: 'VEXLIFE_PROD_RUNTIME_MEMORY_CURRENTIZE_ONE_CACHE',
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
    singleUseNonce: 'nonce-vexlife-prod-runtime-memory-currentize-one-cache-a005-20261116',
    stopConditions: [
      'stop.on.exact-source-drift',
      'stop.on.source-build-failure',
      'stop.on.front-door-held-state',
      'stop.on.acquisition-required',
      'stop.on.runtime-currentization-failure',
      'stop.on.service-currentness-failure',
      'stop.on.bound-expiry',
    ],
    archiveDigestBinding: 'FILENAME_SHA256',
  }, authorityEnvelope);

  const formed = createCanonicalTaskZip({
    taskTemplate,
    contentRoot,
    outputDirectory: path.join(os.homedir(), 'Downloads'),
    outputStem: 'VEXLIFE-RUNTIME-MEMORY-CURRENTIZE-A005',
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
