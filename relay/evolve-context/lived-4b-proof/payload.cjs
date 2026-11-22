'use strict';
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const { spawnSync } = require('node:child_process');

const RESULT_DIR = process.env.VEXLOCALBRIDGE_RESULT_DIR;
const CAPSULE_ROOT = process.env.VEXLOCALBRIDGE_CAPSULE_ROOT;
const TASK_REF = process.env.VEXLOCALBRIDGE_TASK_REF;
const ATTEMPT_REF = process.env.VEXLOCALBRIDGE_ATTEMPT_REF;
if (!RESULT_DIR || !CAPSULE_ROOT || !TASK_REF || !ATTEMPT_REF) throw new Error('VLB_ENVIRONMENT_MISSING');
fs.mkdirSync(RESULT_DIR, { recursive: true });

const effectNames = [
  'sourceMutationPerformed','branchMutationPerformed','claimMutationPerformed','prMetadataMutationPerformed',
  'reviewPerformed','approvalPerformed','readyTransitionPerformed','mergePerformed','mainMutationPerformed',
  'checkpointMutationPerformed','forcePushPerformed','historyRewritePerformed','hostInstallationPerformed',
  'modelRuntimePerformed','providerOrNetworkEffectPerformed','publicationPerformed',
  'credentialSerializationPerformed','filesystemOutsideQualifiedRootsPerformed',
];
function effects() { return Object.fromEntries(effectNames.map((name) => [name, false])); }
function sha256(file) { return crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex'); }
function writeJson(file, value) { fs.mkdirSync(path.dirname(file), { recursive: true }); fs.writeFileSync(file, JSON.stringify(value, null, 2) + '\n'); }
function failureRecord(summary) {
  return {
    failureFamily: 'LIVED_EVIDENCE_ACQUISITION',
    failureFingerprint: crypto.createHash('sha256').update(summary).digest('hex'),
    stage: 'EVIDENCE_ACQUISITION',
    summary,
    evidenceRef: 'lived-proof.json',
  };
}
function resolveExec(candidates, label) {
  const found = candidates.find((candidate) => {
    try { return fs.statSync(candidate).isFile() && (fs.statSync(candidate).mode & 0o111) !== 0; }
    catch { return false; }
  });
  if (!found) throw new Error(label + '_EXECUTABLE_UNAVAILABLE');
  return found;
}

const binding = JSON.parse(fs.readFileSync(path.join(process.cwd(), 'inputs', 'proof-binding.json'), 'utf8'));
const policy = JSON.parse(process.env.VEXLOCALBRIDGE_VEXLIFE_PROD_HOST_EFFECT_POLICY_JSON || 'null');
const retainedRoot = path.join(
  CAPSULE_ROOT, 'bridge', 'retained-vexrelay',
  'task-vexlife-prod-evolve-context-lived-4b', 'attempt-a003', 'evolve-context'
);
fs.mkdirSync(retainedRoot, { recursive: true });
const livedPath = path.join(retainedRoot, 'lived-proof.json');
const stdoutPath = path.join(retainedRoot, 'lived-proof.stdout.log');
const stderrPath = path.join(retainedRoot, 'lived-proof.stderr.log');
const carrierPath = path.join(retainedRoot, 'carrier-summary.json');
const workRoot = path.join(process.cwd(), 'payload-work');
const sourceRoot = path.join(workRoot, 'VexLife-Prod');
fs.mkdirSync(workRoot, { recursive: true });

let proof = null;
let child = null;
let firstFailure = null;
const receipts = [];
const observedEffects = effects();
observedEffects.providerOrNetworkEffectPerformed = true;

try {
  if (!policy || policy.repository !== 'VexGPT/VexLife-Prod' || policy.hostProfileRef !== 'macos-victor') {
    throw new Error('VEXLIFE_PROD_HOST_EFFECT_POLICY_NOT_ADMITTED');
  }
  if (policy.buildDependencyFetchAuthorized !== true
      || policy.productDataPolicy?.modelAcquisitionAllowed !== false
      || policy.productDataPolicy?.modelSwitchForProofAllowed !== false
      || policy.productDataPolicy?.conversationMutationAllowed !== false
      || policy.productDataPolicy?.memoryMutationAllowed !== false
      || policy.productDataPolicy?.runtimeLifecycleMutationAllowed !== true) {
    throw new Error('VEXLIFE_PROD_HOST_EFFECT_POLICY_BOUNDARY_MISMATCH');
  }
  receipts.push({ step: 'host-effect-policy', state: 'PASS' });

  const GH = resolveExec(['/opt/homebrew/bin/gh','/usr/local/bin/gh'], 'GH');
  const GIT = resolveExec(['/usr/bin/git'], 'GIT');
  const NODE = resolveExec(['/opt/homebrew/bin/node','/usr/local/bin/node','/usr/bin/node'], 'NODE');

  let r = spawnSync(GH, ['repo','clone',binding.repository,sourceRoot,'--','--no-checkout'], {
    cwd: workRoot, encoding: 'utf8', timeout: 300000, maxBuffer: 16 * 1024 * 1024, shell: false,
  });
  if (r.error || r.status !== 0) throw new Error('PROOF_SOURCE_CLONE_FAILED:' + (r.error?.message || r.status));
  r = spawnSync(GIT, ['-C',sourceRoot,'checkout','--detach',binding.head], {
    cwd: workRoot, encoding: 'utf8', timeout: 120000, maxBuffer: 16 * 1024 * 1024, shell: false,
  });
  if (r.error || r.status !== 0) throw new Error('PROOF_SOURCE_CHECKOUT_FAILED:' + (r.error?.message || r.status));
  const head = spawnSync(GIT, ['-C',sourceRoot,'rev-parse','HEAD'], {encoding:'utf8',shell:false}).stdout.trim();
  const tree = spawnSync(GIT, ['-C',sourceRoot,'rev-parse','HEAD^{tree}'], {encoding:'utf8',shell:false}).stdout.trim();
  const dirty = spawnSync(GIT, ['-C',sourceRoot,'status','--porcelain=v1','-uall'], {encoding:'utf8',shell:false}).stdout.trim();
  const runnerBlob = spawnSync(GIT, ['-C',sourceRoot,'rev-parse',binding.head + ':' + binding.runnerPath], {encoding:'utf8',shell:false}).stdout.trim();
  if (head !== binding.head || tree !== binding.tree || dirty !== '' || runnerBlob !== binding.runnerBlob) {
    throw new Error('PROOF_SOURCE_BINDING_MISMATCH');
  }
  receipts.push({ step: 'exact-proof-source', state: 'PASS', head, tree, runnerBlob });

  child = spawnSync(NODE, [path.join(sourceRoot, binding.runnerPath)], {
    cwd: sourceRoot,
    env: {
      ...process.env,
      VEX_EVOLVE_CONTEXT_EVIDENCE_JSON: livedPath,
      VEX_EVOLVE_CONTEXT_WORK_ROOT: path.join(workRoot, 'runs'),
    },
    encoding: 'utf8', timeout: 5400000, maxBuffer: 32 * 1024 * 1024, shell: false,
  });
  fs.writeFileSync(stdoutPath, child.stdout || '');
  fs.writeFileSync(stderrPath, child.stderr || '');
  if (fs.existsSync(livedPath)) proof = JSON.parse(fs.readFileSync(livedPath, 'utf8'));
  observedEffects.modelRuntimePerformed = proof?.modelRuntimeAttempted === true;

  const exitCode = Number.isInteger(child.status) ? child.status : null;
  if (child.error || exitCode !== 0 || proof?.mechanicalState !== 'PASS') {
    const summary = proof?.failure?.message || child?.error?.message || 'lived proof did not complete mechanically';
    firstFailure = failureRecord(summary);
    receipts.push({ step: 'lived-4b-proof', state: 'SAFE_FAILURE', exitCode, code: proof?.failure?.code || null });
  } else {
    receipts.push({ step: 'lived-4b-proof', state: 'PASS', exitCode: 0, semanticJudgment: proof.semanticJudgment });
  }
} catch (error) {
  firstFailure = firstFailure || failureRecord(String(error.message || error));
  if (!fs.existsSync(stdoutPath)) fs.writeFileSync(stdoutPath, child?.stdout || '');
  if (!fs.existsSync(stderrPath)) fs.writeFileSync(stderrPath, (child?.stderr || '') + '\n' + String(error.stack || error) + '\n');
  if (fs.existsSync(livedPath)) {
    try { proof = JSON.parse(fs.readFileSync(livedPath, 'utf8')); } catch {}
  }
  observedEffects.modelRuntimePerformed = proof?.modelRuntimeAttempted === true;
  receipts.push({ step: 'lived-4b-proof', state: 'SAFE_FAILURE', code: String(error.message || error).split(':')[0] });
}

const disposition = firstFailure ? 'SAFE_FAILURE' : 'PASS';
writeJson(carrierPath, {
  schemaVersion: 'vexlife-prod.evolve-context-lived-4b-carrier-summary/v1',
  taskRef: TASK_REF,
  attemptRef: ATTEMPT_REF,
  proofSource: binding,
  disposition,
  runnerExitCode: Number.isInteger(child?.status) ? child.status : null,
  modelRuntimeAttempted: proof?.modelRuntimeAttempted === true,
  mechanicalState: proof?.mechanicalState || null,
  semanticJudgment: proof?.semanticJudgment || null,
});
if (!fs.existsSync(livedPath)) {
  writeJson(livedPath, {
    schemaVersion: 'vexlife-prod.evolve-context-lived-4b-proof/v1',
    mechanicalState: 'SAFE_FAILURE',
    semanticJudgment: 'NOT_REACHED',
    modelRuntimeAttempted: false,
    failure: firstFailure,
  });
}

const retained = [
  [livedPath, 'lived-proof.json'],
  [stdoutPath, 'lived-proof.stdout.log'],
  [stderrPath, 'lived-proof.stderr.log'],
  [carrierPath, 'carrier-summary.json'],
].map(([sourcePath, name]) => ({
  sourcePath,
  targetPath: 'retained/inner-vexrelay/evolve-context/' + name,
  expectedSha256: sha256(sourcePath),
}));

const payloadResult = {
  schemaVersion: 'vextreme.vexlocalbridge-mac-payload-result/v1',
  disposition,
  stopReasonOrNull: firstFailure ? 'LIVED_EVIDENCE_ACQUISITION_FAILED' : null,
  firstSubstantiveFailureOrNull: firstFailure,
  acceptedExitCodes: [0],
  candidateFindings: [],
  harnessFindings: firstFailure ? ['LIVED_EVIDENCE_ACQUISITION_FAILED'] : [],
  environmentFindings: [],
  unknownFindings: [],
  effects: observedEffects,
  beforeState: {
    productMain: binding.base,
    proofHead: binding.head,
    proofTree: binding.tree,
    proofPr: binding.proofPr,
    foundationExpected: '0.6.19',
  },
  afterState: {
    mechanicalState: proof?.mechanicalState || null,
    semanticJudgment: proof?.semanticJudgment || null,
    modelRuntimeAttempted: proof?.modelRuntimeAttempted === true,
    baseline: proof?.baseline || null,
    treatment: proof?.treatment || null,
    standing: proof?.standing || null,
  },
  completedStepReceipts: receipts,
  retainedEvidenceFiles: retained,
};
writeJson(path.join(RESULT_DIR, 'payload-result.json'), payloadResult);
process.exitCode = 0;
