'use strict';
const fs = require('node:fs');
const path = require('node:path');
const { spawnSync } = require('node:child_process');

const SDK_ROOT = process.env.VEX_SDK_ROOT;
const PROOF_ROOT = process.env.VEX_PROOF_ROOT;
const DOWNLOADS = process.env.VEX_DOWNLOADS;
const PRODUCT_BASE = process.env.VEX_PRODUCT_BASE;
const PRODUCT_HEAD = process.env.VEX_PROOF_HEAD;
const PRODUCT_TREE = process.env.VEX_PROOF_TREE;
const PRODUCT_BRANCH = process.env.VEX_PROOF_BRANCH;
const RUNNER_BLOB = process.env.VEX_RUNNER_BLOB;
const PAYLOAD_BLOB = process.env.VEX_PAYLOAD_BLOB;
if (![SDK_ROOT, PROOF_ROOT, DOWNLOADS, PRODUCT_BASE, PRODUCT_HEAD, PRODUCT_TREE, PRODUCT_BRANCH, RUNNER_BLOB, PAYLOAD_BLOB].every(Boolean)) {
  throw new Error('FORMATION_ENVIRONMENT_MISSING');
}

const builder = require(path.join(SDK_ROOT, 'lib/vexlocalbridge/package-builder.js'));
const bootstrapSource = require(path.join(SDK_ROOT, 'lib/vexlocalbridge/mac-vexlife-prod-source-bootstrap-command.js'));
const { writeJson, hashFile, bindAuthoritySummaryFromEnvelope, createCanonicalTaskZip } = builder;
const { renderMacVexLifeProdSourceBootstrapCommand } = bootstrapSource;

const PAYLOAD_SOURCE = path.join(PROOF_ROOT, 'relay/evolve-context/lived-4b-proof/payload.cjs');
const contentRoot = fs.mkdtempSync(path.join(process.env.TMPDIR || '/tmp', 'evolve-context-a003-package-'));
const authorityPath = path.join(contentRoot, 'authority.json');
const payloadPath = path.join(contentRoot, 'payload.cjs');
const bindingPath = path.join(contentRoot, 'inputs', 'proof-binding.json');
fs.mkdirSync(path.dirname(bindingPath), { recursive: true });

const authority = {
  authoritySchemaRef: 'vextreme.effect-transaction/v1',
  semanticAdmissionRef: 'github.pr.VexGPT.VexLife-Prod.512',
  transactionRef: 'transaction.vexlife-prod.evolve-context.lived-4b.20261122.a003',
  transactionVersion: 1,
  allowedEffectRefs: [
    'effect.vexlocalbridge.vexlife-prod.product-lifecycle-proof/v1',
    'effect.vexlocalbridge.vexlife-prod.build-dependency-fetch/v1',
  ],
  heldEffectRefs: [
    'effect.vexlocalbridge.vexlife-prod.visual-environment-provisioning/v1',
    'effect.vexlocalbridge.vexlife-prod.visual-input-lived-proof/v1',
    'effect.vexlife-prod.model-acquisition',
    'effect.vexlife-prod.model-switch-for-proof',
    'effect.vexlife-prod.real-conversation-mutation',
    'effect.vexlife-prod.memory-mutation',
    'effect.vexlife-prod.home-recreation',
    'effect.vexlife-prod.home-deletion',
    'effect.github.merge',
    'effect.github.publication',
  ],
};
writeJson(authorityPath, authority);
writeJson(bindingPath, {
  schemaVersion: 'vexlife-prod.evolve-context-lived-4b-binding/v1',
  repository: 'VexGPT/VexLife-Prod',
  base: PRODUCT_BASE,
  head: PRODUCT_HEAD,
  tree: PRODUCT_TREE,
  branch: PRODUCT_BRANCH,
  proofPr: 512,
  runnerPath: 'tools/continuity-pressure/evolve-context-lived-4b-proof.mjs',
  runnerBlob: RUNNER_BLOB,
  payloadPath: 'relay/evolve-context/lived-4b-proof/payload.cjs',
  payloadBlob: PAYLOAD_BLOB,
});
fs.copyFileSync(PAYLOAD_SOURCE, payloadPath);
const syntax = spawnSync(process.execPath, ['--check', payloadPath], { encoding: 'utf8', shell: false });
if (syntax.status !== 0) throw new Error('PAYLOAD_SYNTAX_INVALID:' + (syntax.stderr || syntax.stdout));

const taskTemplate = {
  schemaVersion: 'vextreme.vexlocalbridge-task-manifest/v1',
  artifactClass: 'EXECUTABLE_RELAY',
  taskRef: 'task.vexlife-prod.evolve-context.lived-4b',
  attemptRef: 'attempt.vexlife-prod.evolve-context.lived-4b.a003',
  packageRef: 'package.vexlife-prod.evolve-context.lived-4b.a003',
  requestRef: 'request.vexlife-prod.evolve-context.lived-4b.a003',
  correlationRef: 'correlation.vexlife-prod.evolve-context.lived-4b.a003',
  taskStorageKey: 'task-vexlife-prod-evolve-context-lived-4b',
  attemptStorageKey: 'attempt-a003',
  requestingRoleRef: 'Coder[Vex-EvolveContext]',
  requestingInstanceRef: 'instance.chatgpt.vex-evolve-context.lived-4b.20261122.a003',
  parentOperationsRef: 'github.pr.VexGPT.VexLife-Prod.512',
  returnRouteRef: 'github.pr.VexGPT.VexLife-Prod.512',
  hostProfileRef: 'macos-victor',
  repositoryExecutionProfileRefOrNull: 'profile.repository.vexlife-prod.macos-v1',
  repositoryRefOrNull: 'VexGPT/VexLife-Prod',
  baseRefOrNull: PRODUCT_BASE,
  headRefOrNull: PRODUCT_HEAD,
  treeRefOrNull: PRODUCT_TREE,
  branchRefOrNull: PRODUCT_BRANCH,
  claimRefOrNull: 'claim.vexlife-prod.evolve-context.lived-4b.post-merge.a003',
  exactPathRefs: [
    'src/jvmMain/resources/foundational/vex-foundational-field.json',
    'src/jvmMain/kotlin/vexlife/continuity/DirectConversationContextProjection.kt',
    'src/jvmTest/kotlin/vexlife/harness/ContinuityPressureAdapter.kt',
    'src/jvmTest/kotlin/vexlife/harness/ContinuityPressureAdapterTest.kt',
    'tools/continuity-pressure/scenarios/depth-continuity-v1.json',
    'tools/continuity-pressure/scenarios/lane/historical-current-state-v1.json',
    'tools/continuity-pressure/evolve-context-lived-4b-proof.mjs',
    'relay/evolve-context/lived-4b-proof/payload.cjs',
    'relay/evolve-context/lived-4b-proof/form-and-run.cjs',
    'relay/evolve-context/lived-4b-proof/FETCH-AND-RUN-A003.command',
  ],
  authorityBinding: {
    authoritySchemaRef: authority.authoritySchemaRef,
    authorityEnvelopePath: 'authority.json',
    authorityEnvelopeSha256: hashFile(authorityPath),
    semanticAdmissionRef: authority.semanticAdmissionRef,
    transactionRef: authority.transactionRef,
    transactionVersion: authority.transactionVersion,
    allowedEffectRefs: [...authority.allowedEffectRefs],
    heldEffectRefs: [...authority.heldEffectRefs],
  },
  payload: {
    payloadClass: 'VEXLIFE_PROD_EVOLVECONTEXT_LIVED_4B_PROOF',
    payloadRuntime: 'node',
    payloadPath: 'payload.cjs',
    payloadSha256: hashFile(payloadPath),
    typedArgv: [],
    immutableInputs: [{ path: 'inputs/proof-binding.json', sha256: hashFile(bindingPath) }],
  },
  executionStateAtFormation: 'TASK_PREPARED',
  singleUseNonce: 'nonce-vex-evolve-context-lived-4b-a003-20261122',
  stopConditions: [
    'STOP_ON_IDENTITY_DRIFT',
    'STOP_ON_REAL_HOME_WRITER_NOT_IDLE',
    'STOP_ON_SELECTED_MODEL_NOT_4B',
    'STOP_ON_EVIDENCE_ACQUISITION_FAILURE',
  ],
  archiveDigestBinding: 'FILENAME_SHA256',
};

const formed = createCanonicalTaskZip({
  taskTemplate: bindAuthoritySummaryFromEnvelope(taskTemplate, authority),
  contentRoot,
  outputDirectory: DOWNLOADS,
  outputStem: 'VEX-EVOLVECONTEXT-LIVED-4B-A003',
  ttlMs: 5_400_000,
});
console.log('FORMED_VEXLOCALBRIDGE_TASK=' + formed.finalZip);
console.log('VEXLOCALBRIDGE_TASK_SHA256=' + formed.digest);

const bootstrap = renderMacVexLifeProdSourceBootstrapCommand({
  packageSha256: formed.digest,
  sdkHead: '149455eeec4c7b7ca00e91022154a85faa3d608c',
  sdkTree: '53ea701f9bbf24a5cf37d43e4dbe3836187f4011',
});
const execution = spawnSync('/bin/bash', ['-c', bootstrap], { env: process.env, stdio: 'inherit', shell: false });

const capsuleRoot = path.join(DOWNLOADS, 'VLB-' + formed.digest.slice(0, 16));
function walk(root) {
  if (!fs.existsSync(root)) return [];
  const out = [];
  for (const entry of fs.readdirSync(root, { withFileTypes: true })) {
    const p = path.join(root, entry.name);
    if (entry.isDirectory()) out.push(...walk(p));
    else if (entry.isFile()) out.push(p);
  }
  return out;
}
const returns = walk(capsuleRoot).filter((file) => /^RETURN-.*--sha256-[a-f0-9]{64}\.zip$/.test(path.basename(file)));
if (returns.length !== 1) {
  console.error('VEXLOCALBRIDGE_RETURN_COUNT=' + returns.length);
  console.error('VEXLOCALBRIDGE_EXECUTION_STATUS=' + execution.status);
  console.error('VEXLOCALBRIDGE_CAPSULE=' + capsuleRoot);
  process.exit(40);
}
const sourceReturn = returns[0];
const digest = hashFile(sourceReturn);
const match = path.basename(sourceReturn).match(/--sha256-([a-f0-9]{64})\.zip$/);
if (!match || match[1] !== digest) throw new Error('CANONICAL_RETURN_DIGEST_BINDING_MISMATCH');
const target = path.join(DOWNLOADS, path.basename(sourceReturn));
if (path.resolve(sourceReturn) !== path.resolve(target)) {
  if (fs.existsSync(target) && hashFile(target) !== digest) throw new Error('DOWNLOADS_RETURN_NAME_COLLISION');
  if (!fs.existsSync(target)) fs.copyFileSync(sourceReturn, target);
}
if (hashFile(target) !== digest) throw new Error('COPIED_RETURN_DIGEST_MISMATCH');
console.log('VEXLOCALBRIDGE_EXECUTION_STATUS=' + execution.status);
console.log('CANONICAL_VEXLOCALBRIDGE_RETURN=' + target);
process.exit(0);
