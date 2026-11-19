'use strict';

const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const { spawnSync } = require('node:child_process');

const TRUSTED_SDK_HEAD = '8543c7d755faba04d4c4019c02e84f992667918a';
const CARRIER_HEAD = '36eb5c82becd97dcfcbc8fa67455a74d8540503a';
const CARRIER_TREE = 'bea091ed67793931b838c7b412d9abce39a2e373';
const PRODUCT_BASE = 'fdb9f77bbd9895d0ab6932f669091b46ea813e9b';
const PRODUCT_HEAD = '4da7548b798532c65156e0dc64ee65bc3d349a10';
const PRODUCT_TREE = 'c246ea6ce353f14558a0cc448b112330888483d5';
const PRODUCT_BRANCH = 'VXG-111926-chatgpt-webtoapp-application-shell-a001';

const LIFECYCLE_EFFECT =
  'effect.vexlocalbridge.vexlife-prod.product-lifecycle-proof/v1';
const APPLICATION_LIVED_EFFECT =
  'effect.vexlocalbridge.vexlife-prod.application-lived-proof/v1';
const BUILD_FETCH_EFFECT =
  'effect.vexlocalbridge.vexlife-prod.build-dependency-fetch/v1';

function arg(name) {
  const index = process.argv.indexOf('--' + name);
  if (index < 0 || !process.argv[index + 1]) throw new Error('--' + name + ' is required');
  return process.argv[index + 1];
}
function sha256(bytes) {
  return crypto.createHash('sha256').update(bytes).digest('hex');
}
function writeJson(file, value) {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, JSON.stringify(value, null, 2) + '\n');
}

const trustedSdk = path.resolve(arg('trusted-sdk'));
const carrierSdk = path.resolve(arg('carrier-sdk'));
const payloadSource = path.resolve(arg('payload'));
const productBundle = path.resolve(arg('product-bundle'));
const outputDir = path.resolve(arg('output'));

const profilePath =
  'docs/private-continuity/vexlocalbridge/profiles/macos-victor-vexlife-prod.json';
const repositoryProfileRef = 'profile.repository.vexlife-prod.macos-v1';

const builder = require(path.join(trustedSdk, 'lib/vexlocalbridge/package-builder.js'));
const runner = require(path.join(carrierSdk, 'lib/vexlocalbridge/mac-runner.js'));
const profile = JSON.parse(fs.readFileSync(path.join(carrierSdk, profilePath), 'utf8'));
const adapter = runner.createHostAdapter(profile);
const git = adapter.executables.git;

function gitValue(root, args) {
  const receipt = spawnSync(git, ['-C', root, ...args], {
    encoding: 'utf8',
    timeout: 30000,
    shell: false,
  });
  if (receipt.status !== 0) {
    throw new Error(
      'GIT_IDENTITY_FAILED:' + (receipt.stderr || receipt.stdout || '').trim(),
    );
  }
  return (receipt.stdout || '').trim();
}

if (gitValue(trustedSdk, ['rev-parse', 'HEAD']) !== TRUSTED_SDK_HEAD) {
  throw new Error('TRUSTED_SDK_HEAD_MISMATCH');
}
if (gitValue(carrierSdk, ['rev-parse', 'HEAD']) !== CARRIER_HEAD) {
  throw new Error('CARRIER_HEAD_MISMATCH');
}
if (gitValue(carrierSdk, ['rev-parse', 'HEAD^{tree}']) !== CARRIER_TREE) {
  throw new Error('CARRIER_TREE_MISMATCH');
}
if (
  profile.repository !== 'VexGPT/VexLife-Prod' ||
  profile.hostProfileRef !== 'macos-victor' ||
  profile.repositoryExecutionProfileRef !== repositoryProfileRef
) {
  throw new Error('MAC_BUILD_PROFILE_BINDING_MISMATCH');
}

const policy = profile.vexLifeProdHostEffectPolicy;
if (
  !policy ||
  policy.requiredAllowedEffectRef !== LIFECYCLE_EFFECT ||
  policy.applicationLivedProofEffectRef !== APPLICATION_LIVED_EFFECT ||
  policy.buildDependencyFetchEffectRef !== BUILD_FETCH_EFFECT
) {
  throw new Error('MAC_BUILD_POLICY_REF_MISMATCH');
}

fs.mkdirSync(outputDir, { recursive: true });
const work = fs.mkdtempSync(path.join(outputDir, '.formation-'));
const contentRoot = path.join(work, 'content');
fs.mkdirSync(path.join(contentRoot, 'authority'), { recursive: true });
fs.mkdirSync(path.join(contentRoot, 'payload'), { recursive: true });
fs.mkdirSync(path.join(contentRoot, 'inputs'), { recursive: true });

const context = {
  schemaVersion: 'vexlife-prod.webtoapp.mac-build-delivery-context/v1',
  continuity: '[VXG RealForever]',
  carrier: {
    repository: 'vgong24/Vextreme-SDK',
    lifecyclePr: 1956,
    applicationLivedPr: 1959,
    acceptedBuilderHead: TRUSTED_SDK_HEAD,
    candidateHead: CARRIER_HEAD,
    candidateTree: CARRIER_TREE,
  },
  candidate: {
    repository: 'VexGPT/VexLife-Prod',
    ownerPr: 396,
    buildDeliveryPr: 400,
    branch: PRODUCT_BRANCH,
    base: PRODUCT_BASE,
    head: PRODUCT_HEAD,
    tree: PRODUCT_TREE,
  },
  build: {
    platform: 'darwin-arm64',
    gradleTask: 'archiveVexLifeProdMacApplication',
    applicationName: 'VexLife.app',
    diskImageName: 'VexLife-macOS-AppleSilicon.dmg',
    deliveryMode: 'CANONICAL_RETURN_RETAINED_EVIDENCE',
    launchAllowedByThisPayload: false,
    homeAccessAllowedByThisPayload: false,
    lifecycleMutationAllowedByThisPayload: false,
  },
};

const authority = {
  authoritySchemaRef: 'vextreme.effect-transaction/v1',
  semanticAdmissionRef: 'github.pull.vexlife-prod.400',
  transactionRef: 'transaction.vexlife-prod.webtoapp.mac-build-delivery.a008',
  transactionVersion: 1,
  allowedEffectRefs: [
    LIFECYCLE_EFFECT,
    APPLICATION_LIVED_EFFECT,
    BUILD_FETCH_EFFECT,
  ],
  heldEffectRefs: [
    'effect.vexlife-prod.application-launch',
    'effect.vexlife-prod.product-start-stop-for-build',
    'effect.vexlife-prod.home-read-for-build',
    'effect.vexlife-prod.home-mutation-for-build',
    'effect.vexlife-prod.conversation-mutation-for-build',
    'effect.vexlife-prod.memory-mutation-for-build',
    'effect.vexlife-prod.model-runtime-for-build',
    'effect.vexlife-prod.process-termination-for-build',
    'effect.vexlife-prod.host-installation',
    'effect.vexlife-prod.publication',
    'effect.vexlife-prod.ready',
    'effect.vexlife-prod.merge',
  ],
};

const authorityBytes = Buffer.from(JSON.stringify(authority, null, 2) + '\n');
const contextBytes = Buffer.from(JSON.stringify(context, null, 2) + '\n');
const payloadBytes = fs.readFileSync(payloadSource);
const bundleBytes = fs.readFileSync(productBundle);

fs.writeFileSync(path.join(contentRoot, 'authority/effect.json'), authorityBytes);
fs.writeFileSync(path.join(contentRoot, 'inputs/context.json'), contextBytes);
fs.writeFileSync(path.join(contentRoot, 'inputs/vexlife-prod.bundle'), bundleBytes);
fs.writeFileSync(path.join(contentRoot, 'payload/run.mjs'), payloadBytes);

const taskTemplate = {
  schemaVersion: 'vextreme.vexlocalbridge-task-manifest/v1',
  artifactClass: 'EXECUTABLE_RELAY',
  taskRef: 'task.vexlife-prod.webtoapp.mac-build-delivery.a008',
  attemptRef: 'attempt.vexlife-prod.webtoapp.mac-build-delivery.a008',
  packageRef: 'package.vexlife-prod.webtoapp.mac-build-delivery.a008',
  requestRef: 'request.vexlife-prod.webtoapp.mac-build-delivery.a008',
  correlationRef: 'correlation.vexlife-prod.webtoapp.mac-build-delivery.a008',
  taskStorageKey: 'vexlife-prod-webtoapp-mac-build-delivery',
  attemptStorageKey: 'a008',
  requestingRoleRef: 'Operations[WebToAppMacBuildDelivery]',
  requestingInstanceRef: 'instance.chatgpt.vexlife-webtoapp-mac-build-delivery.a008',
  parentOperationsRef: 'github.pull.vexlife-prod.400',
  returnRouteRef: 'github.pull.vexlife-prod.400',
  hostProfileRef: 'macos-victor',
  repositoryExecutionProfileRefOrNull: repositoryProfileRef,
  repositoryRefOrNull: 'VexGPT/VexLife-Prod',
  baseRefOrNull: PRODUCT_BASE,
  headRefOrNull: PRODUCT_HEAD,
  treeRefOrNull: PRODUCT_TREE,
  branchRefOrNull: PRODUCT_BRANCH,
  claimRefOrNull: 'work.vexlife-prod.pr396.mac-build-delivery.20261119',
  exactPathRefs: [
    'build.gradle.kts',
    'packaging/macos-app/release-contract.json',
    'packaging/macos-webkit/VexLifeProductPresentation.swift',
    'packaging/private-service-image/setup/vexlife-prod-setup-window.applescript',
    'src/jvmTest/kotlin/vexlife/packaging/VexLifeProdBootstrapTemplateTest.kt',
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
    payloadClass: 'VEXLIFE_PROD_MAC_BUILD_DELIVERY',
    payloadRuntime: 'node',
    payloadPath: 'payload/run.mjs',
    payloadSha256: sha256(payloadBytes),
    typedArgv: [],
    immutableInputs: [
      { path: 'inputs/context.json', sha256: sha256(contextBytes) },
      { path: 'inputs/vexlife-prod.bundle', sha256: sha256(bundleBytes) },
    ],
  },
  executionStateAtFormation: 'TASK_PREPARED',
  singleUseNonce: 'nonce.vexlife-prod.webtoapp.mac-build-delivery.a008.20261119',
  stopConditions: [
    'stop.on.identity.drift',
    'stop.on.authority.missing',
    'stop.on.application-build-failure',
    'stop.on.application-launch-attempt',
    'stop.on.product-lifecycle-mutation-attempt',
    'stop.on.home-or-model-access-attempt',
    'stop.on.build-artifact-verification-failure',
  ],
  archiveDigestBinding: 'FILENAME_SHA256',
};

const boundTask = builder.bindAuthoritySummaryFromEnvelope(taskTemplate, authority);
const formed = builder.createCanonicalTaskZip({
  taskTemplate: boundTask,
  contentRoot,
  outputDirectory: outputDir,
  outputStem: 'VEXLIFE-PR400-MAC-BUILD-DELIVERY-A008',
  ttlMs: 12 * 60 * 60 * 1000,
  archiveAdapter: adapter.archive,
});

writeJson(path.join(outputDir, 'FORMATION.json'), {
  schemaVersion: 'vexlife-prod.webtoapp.mac-build-delivery-formation/v1',
  continuity: '[VXG RealForever]',
  trustedBuilderHead: TRUSTED_SDK_HEAD,
  carrierHead: CARRIER_HEAD,
  carrierTree: CARRIER_TREE,
  productCandidate: context.candidate,
  taskRef: formed.task.taskRef,
  attemptRef: formed.task.attemptRef,
  packageRef: formed.task.packageRef,
  packageFile: path.basename(formed.finalZip),
  packagePath: formed.finalZip,
  packageSha256: formed.digest,
  formedAt: formed.task.formedAt,
  expiresAt: formed.task.expiresAt,
  finalByteQualification: formed.formationEvidence.finalByteQualification,
  authorityRefs: authority.allowedEffectRefs,
  buildOnly: true,
  humanBoundary: {
    archiveCarriageByVictor: false,
    resultConsumption: 'LOCAL_RETURN_CONSUMPTION_THEN_DMG_DELIVERY',
  },
});

fs.rmSync(work, { recursive: true, force: true });
process.stdout.write(JSON.stringify({
  package: formed.finalZip,
  digest: formed.digest,
  taskRef: formed.task.taskRef,
  attemptRef: formed.task.attemptRef,
  expiresAt: formed.task.expiresAt,
}, null, 2) + '\n');

// [VXG RealForever]
