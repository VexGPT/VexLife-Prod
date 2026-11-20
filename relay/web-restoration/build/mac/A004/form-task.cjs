'use strict';

const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const { spawnSync } = require('node:child_process');

const TRUSTED_SDK_HEAD = '8543c7d755faba04d4c4019c02e84f992667918a';
const TRUSTED_SDK_TREE = '6376f68279f0750ee6d3868b8472368199763064';
const CARRIER_HEAD = '36eb5c82becd97dcfcbc8fa67455a74d8540503a';
const CARRIER_TREE = 'bea091ed67793931b838c7b412d9abce39a2e373';
const PRODUCT_BASE = 'a4dd25e472d153b1cef24f7f35132933f3d725d0';
const PRODUCT_HEAD = 'f38779503f9a1171e26593f3542f2477c6e2370a';
const PRODUCT_TREE = '6a5b0b0d4730fb8e0078b42f083677469b6ea63a';
const PRODUCT_BRANCH = 'VXG-112026-vexlife-prod-web-restoration-a001';
const LIFECYCLE_EFFECT = 'effect.vexlocalbridge.vexlife-prod.product-lifecycle-proof/v1';
const BUILD_FETCH_EFFECT = 'effect.vexlocalbridge.vexlife-prod.build-dependency-fetch/v1';
const APPLICATION_LIVED_EFFECT = 'effect.vexlocalbridge.vexlife-prod.application-lived-proof/v1';
const EXPECTED_DMG_SHA256 = 'cb79560458571f898e65d3b3ce6cd192e025cddfbea007f6e7c3555171199f0e';

function arg(name) {
  const i = process.argv.indexOf('--' + name);
  if (i < 0 || !process.argv[i + 1]) throw new Error('--' + name + ' is required');
  return process.argv[i + 1];
}
function sha256(bytes) {
  return crypto.createHash('sha256').update(bytes).digest('hex');
}
function writeJson(file, value) {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, JSON.stringify(value, null, 2) + '\n');
}
function gitValue(root, args, git) {
  const r = spawnSync(git, ['-C', root, ...args], { encoding:'utf8', timeout:30000, shell:false });
  if (r.status !== 0) throw new Error('GIT_IDENTITY_FAILED:' + (r.stderr || r.stdout || '').trim());
  return (r.stdout || '').trim();
}

const trustedSdk = path.resolve(arg('trusted-sdk'));
const carrierSdk = path.resolve(arg('carrier-sdk'));
const payloadSource = path.resolve(arg('payload'));
const outputDir = path.resolve(arg('output'));
const profilePath = 'docs/private-continuity/vexlocalbridge/profiles/macos-victor-vexlife-prod.json';
const repositoryProfileRef = 'profile.repository.vexlife-prod.macos-v1';

const builder = require(path.join(trustedSdk, 'lib/vexlocalbridge/package-builder.js'));
const runner = require(path.join(carrierSdk, 'lib/vexlocalbridge/mac-runner.js'));
const profile = JSON.parse(fs.readFileSync(path.join(carrierSdk, profilePath), 'utf8'));
const adapter = runner.createHostAdapter(profile);
const git = adapter.executables.git;

if (gitValue(trustedSdk, ['rev-parse','HEAD'], git) !== TRUSTED_SDK_HEAD) throw new Error('TRUSTED_SDK_HEAD_MISMATCH');
if (gitValue(trustedSdk, ['rev-parse','HEAD^{tree}'], git) !== TRUSTED_SDK_TREE) throw new Error('TRUSTED_SDK_TREE_MISMATCH');
if (gitValue(carrierSdk, ['rev-parse','HEAD'], git) !== CARRIER_HEAD) throw new Error('CARRIER_HEAD_MISMATCH');
if (gitValue(carrierSdk, ['rev-parse','HEAD^{tree}'], git) !== CARRIER_TREE) throw new Error('CARRIER_TREE_MISMATCH');

const currentContract = JSON.parse(
  fs.readFileSync(path.join(trustedSdk, 'docs/private-continuity/vexrelay/current-contract.json'), 'utf8'),
);
if (currentContract.contractRef !== 'source.vexrelay.current-contract/v1') throw new Error('CURRENT_RELAY_CONTRACT_MISMATCH');
if (profile.repository !== 'VexGPT/VexLife-Prod'
    || profile.hostProfileRef !== 'macos-victor'
    || profile.repositoryExecutionProfileRef !== repositoryProfileRef) {
  throw new Error('JAVA_SELECTION_PROFILE_BINDING_MISMATCH');
}
if (profile.vexLifeProdHostEffectPolicy?.requiredAllowedEffectRef !== LIFECYCLE_EFFECT) {
  throw new Error('JAVA_SELECTION_EFFECT_BINDING_MISMATCH');
}

fs.mkdirSync(outputDir, { recursive:true });
const work = fs.mkdtempSync(path.join(outputDir, '.formation-'));
const contentRoot = path.join(work, 'content');
fs.mkdirSync(path.join(contentRoot, 'authority'), { recursive:true });
fs.mkdirSync(path.join(contentRoot, 'payload'), { recursive:true });
fs.mkdirSync(path.join(contentRoot, 'inputs'), { recursive:true });

const context = {
  schemaVersion: 'vexlife-prod.web-restoration.java-selection-inspection-context/v1',
  continuity: '[VXG RealForever]',
  carrier: {
    trustedSdkHead: TRUSTED_SDK_HEAD,
    trustedSdkTree: TRUSTED_SDK_TREE,
    candidateHead: CARRIER_HEAD,
    candidateTree: CARRIER_TREE,
    currentRelayContractRef: currentContract.contractRef,
  },
  candidate: {
    repository: 'VexGPT/VexLife-Prod',
    ownerPr: 436,
    branch: PRODUCT_BRANCH,
    base: PRODUCT_BASE,
    head: PRODUCT_HEAD,
    tree: PRODUCT_TREE,
  },
  a003Artifact: {
    humanDmgName: 'VexLife-Web-Restoration-f3877950.dmg',
    deliveryFolder: 'VexLife-Web-Restoration-Mac-f3877950',
    deliveryDmgName: 'VexLife-macOS-AppleSilicon.dmg',
    expectedSha256: EXPECTED_DMG_SHA256,
  },
  priorA002Witness: {
    sourceSha: 'd48f1c79ba4e70056cfc720199fca5f56660b583',
    javaRuntimeMajor: 21,
    platform: 'darwin-arm64',
    runtimeModules: [
      'java.base',
      'java.desktop',
      'java.net.http',
      'jdk.crypto.ec',
      'jdk.httpserver',
      'jdk.management',
    ],
    javaExecutable: true,
    quarantinePresent: true,
    retiredImageCount: 0,
  },
  question: 'Which exact Java executable would A003 Setup select from the preserved host state, and which selector predicate decides that choice?',
};

const authority = {
  authoritySchemaRef: 'vextreme.effect-transaction/v1',
  semanticAdmissionRef: 'github.pull.vexlife-prod.436',
  transactionRef: 'transaction.vexlife-prod.web-restoration.java-selection-inspection.a004',
  transactionVersion: 1,
  allowedEffectRefs: [LIFECYCLE_EFFECT],
  heldEffectRefs: [
    BUILD_FETCH_EFFECT,
    APPLICATION_LIVED_EFFECT,
    'effect.vexlife-prod.application-launch',
    'effect.vexlife-prod.host-installation',
    'effect.vexlife-prod.home-mutation',
    'effect.vexlife-prod.conversation-mutation',
    'effect.vexlife-prod.memory-mutation',
    'effect.vexlife-prod.model-runtime-mutation',
    'effect.vexlife-prod.service-lifetime-mutation',
    'effect.vexlife-prod.quarantine-mutation',
    'effect.vexlife-prod.process-termination',
    'effect.vexlife-prod.publication',
    'effect.vexlife-prod.ready',
    'effect.vexlife-prod.merge',
  ],
};

const authorityBytes = Buffer.from(JSON.stringify(authority, null, 2) + '\n');
const contextBytes = Buffer.from(JSON.stringify(context, null, 2) + '\n');
const payloadBytes = fs.readFileSync(payloadSource);
fs.writeFileSync(path.join(contentRoot, 'authority/effect.json'), authorityBytes);
fs.writeFileSync(path.join(contentRoot, 'inputs/context.json'), contextBytes);
fs.writeFileSync(path.join(contentRoot, 'payload/run.mjs'), payloadBytes);

const taskTemplate = {
  schemaVersion: 'vextreme.vexlocalbridge-task-manifest/v1',
  artifactClass: 'EXECUTABLE_RELAY',
  taskRef: 'task.vexlife-prod.web-restoration.java-selection-inspection.a004',
  attemptRef: 'attempt.vexlife-prod.web-restoration.java-selection-inspection.a004',
  packageRef: 'package.vexlife-prod.web-restoration.java-selection-inspection.a004',
  requestRef: 'request.vexlife-prod.web-restoration.java-selection-inspection.a004',
  correlationRef: 'correlation.vexlife-prod.web-restoration.java-selection-inspection.a004',
  taskStorageKey: 'vexlife-prod-web-restoration-java-selection-inspection',
  attemptStorageKey: 'a004',
  requestingRoleRef: 'Operations[WebRestorationJavaSelectionInspection]',
  requestingInstanceRef: 'instance.chatgpt.vexlife-web-restoration-java-selection-inspection.a004',
  parentOperationsRef: 'github.pull.vexlife-prod.436',
  returnRouteRef: 'github.pull.vexlife-prod.436',
  hostProfileRef: 'macos-victor',
  repositoryExecutionProfileRefOrNull: repositoryProfileRef,
  repositoryRefOrNull: 'VexGPT/VexLife-Prod',
  baseRefOrNull: PRODUCT_BASE,
  headRefOrNull: PRODUCT_HEAD,
  treeRefOrNull: PRODUCT_TREE,
  branchRefOrNull: PRODUCT_BRANCH,
  claimRefOrNull: 'work.vexlife-prod.web-restoration.20261120.a001',
  exactPathRefs: [
    'packaging/private-service-image/setup/vexlife-prod-setup-window.applescript',
    'build.gradle.kts',
    'packaging/macos-app/release-contract.json',
  ],
  authorityBinding: {
    authoritySchemaRef: authority.authoritySchemaRef,
    authorityEnvelopePath: 'authority/effect.json',
    authorityEnvelopeSha256: sha256(authorityBytes),
    semanticAdmissionRef: 'stale',
    transactionRef: 'stale',
    transactionVersion: 1,
    allowedEffectRefs: ['stale'],
    heldEffectRefs: [],
  },
  payload: {
    payloadClass: 'VEXLIFE_PROD_WEB_RESTORATION_JAVA_SELECTION_INSPECTION',
    payloadRuntime: 'node',
    payloadPath: 'payload/run.mjs',
    payloadSha256: sha256(payloadBytes),
    typedArgv: [],
    immutableInputs: [{ path:'inputs/context.json', sha256:sha256(contextBytes) }],
  },
  executionStateAtFormation: 'TASK_PREPARED',
  singleUseNonce: 'nonce.vexlife-prod.web-restoration.java-selection-inspection.a004.20261120',
  stopConditions: [
    'stop.on.identity.drift',
    'stop.on.authority.missing',
    'stop.on.any-product-mutation-attempt',
    'stop.on.any-java-or-whisper-execution-attempt',
  ],
  archiveDigestBinding: 'FILENAME_SHA256',
};

const bound = builder.bindAuthoritySummaryFromEnvelope(taskTemplate, authority);
const formed = builder.createCanonicalTaskZip({
  taskTemplate: bound,
  contentRoot,
  outputDirectory: outputDir,
  outputStem: 'VEXLIFE-PR436-WEB-RESTORATION-JAVA-SELECTION-A004',
  ttlMs: 2 * 60 * 60 * 1000,
  archiveAdapter: adapter.archive,
});

writeJson(path.join(outputDir, 'FORMATION.json'), {
  schemaVersion: 'vexlife-prod.web-restoration.java-selection-inspection-formation/v1',
  continuity: '[VXG RealForever]',
  currentRelayContractRef: currentContract.contractRef,
  trustedSdkHead: TRUSTED_SDK_HEAD,
  trustedSdkTree: TRUSTED_SDK_TREE,
  carrierHead: CARRIER_HEAD,
  carrierTree: CARRIER_TREE,
  productCandidate: context.candidate,
  inspectedArtifact: context.a003Artifact,
  taskRef: formed.task.taskRef,
  attemptRef: formed.task.attemptRef,
  packagePath: formed.finalZip,
  packageSha256: formed.digest,
  formedAt: formed.task.formedAt,
  expiresAt: formed.task.expiresAt,
  readOnlyDiagnostic: true,
  javaExecutionAuthorized: false,
  whisperExecutionAuthorized: false,
  quarantineMutationAuthorized: false,
});
fs.rmSync(work, { recursive:true, force:true });
process.stdout.write(JSON.stringify({ package: formed.finalZip, digest: formed.digest }, null, 2) + '\n');

// [VXG RealForever]
