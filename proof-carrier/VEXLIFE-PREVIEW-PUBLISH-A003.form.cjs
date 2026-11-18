#!/usr/bin/env node
'use strict';

const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');

const [sdkRootArg, payloadPathArg, targetHeadArg, targetTreeArg] = process.argv.slice(2);
if (!sdkRootArg || !payloadPathArg || !targetHeadArg || !targetTreeArg) {
  throw new Error(
    'usage: VEXLIFE-PREVIEW-PUBLISH-A003.form.cjs <sdk-root> <payload-path> <target-head> <target-tree>',
  );
}
if (!/^[0-9a-f]{40}$/.test(targetHeadArg) || !/^[0-9a-f]{40}$/.test(targetTreeArg)) {
  throw new Error('Preview candidate head/tree must be exact lowercase Git object IDs');
}

const sdkRoot = path.resolve(sdkRootArg);
const payloadSource = path.resolve(payloadPathArg);
const targetHead = targetHeadArg;
const targetTree = targetTreeArg;
const short = targetHead.slice(0, 12);
const packageBuilder = require(path.join(sdkRoot, 'lib/vexlocalbridge/package-builder.js'));
const {
  createCanonicalTaskZip,
  writeJson,
  hashFile,
  bindAuthoritySummaryFromEnvelope,
} = packageBuilder;

const candidate = {
  schemaVersion: 'vexlife-prod.preview-publication-candidate/v1',
  continuity: '[VXG RealForever]',
  repository: 'VexGPT/VexLife-Prod',
  branch: 'main',
  channel: 'preview',
  head: targetHead,
  tree: targetTree,
};

const authorityEnvelope = {
  authoritySchemaRef: 'vextreme.effect-transaction/v1',
  semanticAdmissionRef: 'user.victor.vexlife-preview-publication.20261118',
  transactionRef: `transaction.vexlife-prod.preview-publication.${short}.a001`,
  transactionVersion: 1,
  allowedEffectRefs: [
    'effect.vexlife-prod.preview.exact-main-read',
    'effect.vexlife-prod.preview.local-test-and-mac-artifact-build',
    'effect.vexlife-prod.preview.github-release-publication',
    'effect.vexlife-prod.preview.gh-pages-publication',
    'effect.vexlife-prod.preview.provider-network-readback',
  ],
  heldEffectRefs: [
    'effect.vexlife-prod.source-mutation',
    'effect.vexlife-prod.main-mutation',
    'effect.vexlife-prod.pr-metadata-mutation',
    'effect.vexlife-prod.review',
    'effect.vexlife-prod.approval',
    'effect.vexlife-prod.ready',
    'effect.vexlife-prod.merge',
    'effect.vexlife-prod.stable-release-publication',
    'effect.vexlife-prod.checkpoint-mutation',
    'effect.vexlife-prod.training',
  ],
};

const authorityRelativePath = 'authority/effect.json';
const payloadRelativePath = 'payload/run.cjs';
const candidateRelativePath = 'inputs/candidate.json';

const contentRoot = fs.mkdtempSync(
  path.join(os.tmpdir(), `vexlife-preview-publication-${short}-form-`),
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
    taskRef: `task.vexlife-prod.preview-publication.${short}.001`,
    attemptRef: `attempt.vexlife-prod.preview-publication.${short}.a001`,
    packageRef: `package.vexlife-prod.preview-publication.${short}.a001`,
    requestRef: `request.vexlife-prod.preview-publication.${short}.a001`,
    correlationRef: `correlation.vexlife-prod.preview-publication.${short}.a001`,
    taskStorageKey: `vexlife-prod-preview-publication-${short}`,
    attemptStorageKey: 'attempt-a001',
    requestingRoleRef: 'Operations[VexLife-Prod]',
    requestingInstanceRef: `instance.chatgpt.vexlife-prod.preview-publication.${short}.a001`,
    parentOperationsRef: 'work.vexlife-prod.release-alerts.preview-publication.20261118',
    returnRouteRef: 'chat.vexlife-prod.release-alerts.preview-publication.20261118',
    hostProfileRef: 'macos-victor',
    repositoryExecutionProfileRefOrNull: 'profile.repository.vextreme-sdk.macos-v1',
    repositoryRefOrNull: null,
    baseRefOrNull: null,
    headRefOrNull: null,
    treeRefOrNull: null,
    branchRefOrNull: null,
    claimRefOrNull: null,
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
      payloadClass: 'VEXLIFE_PROD_PREVIEW_PUBLICATION',
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
    singleUseNonce: `nonce-vexlife-prod-preview-publication-${short}-a001`,
    stopConditions: [
      'stop.on.exact-main-drift',
      'stop.on.host-profile-mismatch',
      'stop.on.github-auth-unavailable',
      'stop.on.preview-publication-failure',
      'stop.on.release-readback-mismatch',
      'stop.on.pages-catalog-readback-mismatch',
      'stop.on.bound-expiry',
    ],
    archiveDigestBinding: 'FILENAME_SHA256',
  }, authorityEnvelope);

  const formed = createCanonicalTaskZip({
    taskTemplate,
    contentRoot,
    outputDirectory: path.join(os.homedir(), 'Downloads'),
    outputStem: `VEXLIFE-PREVIEW-PUBLISH-${short}-A003`,
    ttlMs: 4 * 60 * 60 * 1000,
  });

  process.stdout.write(JSON.stringify({
    state: 'PASS',
    candidateHead: targetHead,
    candidateTree: targetTree,
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
