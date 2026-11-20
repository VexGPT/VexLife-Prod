'use strict';

const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const { spawnSync } = require('node:child_process');

const TRUSTED_SDK_HEAD = '8543c7d755faba04d4c4019c02e84f992667918a';
const CARRIER_HEAD = '36eb5c82becd97dcfcbc8fa67455a74d8540503a';
const CARRIER_TREE = 'bea091ed67793931b838c7b412d9abce39a2e373';
const PRODUCT_BASE = 'a4dd25e472d153b1cef24f7f35132933f3d725d0';
const PRODUCT_HEAD = 'd48f1c79ba4e70056cfc720199fca5f56660b583';
const PRODUCT_TREE = 'd8dbdd7c94f9e747b68e5fb61c8b2ab73fb34218';
const PRODUCT_BRANCH = 'VXG-112026-vexlife-prod-web-restoration-a001';

const LIFECYCLE_EFFECT = 'effect.vexlocalbridge.vexlife-prod.product-lifecycle-proof/v1';
const APPLICATION_LIVED_EFFECT = 'effect.vexlocalbridge.vexlife-prod.application-lived-proof/v1';
const BUILD_FETCH_EFFECT = 'effect.vexlocalbridge.vexlife-prod.build-dependency-fetch/v1';

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

const profilePath = 'docs/private-continuity/vexlocalbridge/profiles/macos-victor-vexlife-prod.json';
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
    throw new Error('GIT_IDENTITY_FAILED:' + (receipt.stderr || receipt.stdout || '').trim());
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
  throw new Error('WEB_RESTORATION_MAC_PROFILE_BINDING_MISMATCH');
}

const policy = profile.vexLifeProdHostEffectPolicy;
if (
  !policy ||
  policy.requiredAllowedEffectRef !== LIFECYCLE_EFFECT ||
  policy.applicationLivedProofEffectRef !== APPLICATION_LIVED_EFFECT ||
  policy.buildDependencyFetchEffectRef !== BUILD_FETCH_EFFECT
) {
  throw new Error('WEB_RESTORATION_MAC_POLICY_REF_MISMATCH');
}

fs.mkdirSync(outputDir, { recursive: true });
const work = fs.mkdtempSync(path.join(outputDir, '.formation-'));
const contentRoot = path.join(work, 'content');
fs.mkdirSync(path.join(contentRoot, 'authority'), { recursive: true });
fs.mkdirSync(path.join(contentRoot, 'payload'), { recursive: true });
fs.mkdirSync(path.join(contentRoot, 'inputs'), { recursive: true });

const context = {
  schemaVersion: 'vexlife-prod.web-restoration.mac-build-delivery-context/v1',
  continuity: '[VXG RealForever]',
  carrier: {
    repository: 'vgong24/Vextreme-SDK',
    applicationLivedPr: 1959,
    acceptedBuilderHead: TRUSTED_SDK_HEAD,
    candidateHead: CARRIER_HEAD,
    candidateTree: CARRIER_TREE,
  },
  candidate: {
    repository: 'VexGPT/VexLife-Prod',
    ownerPr: 436,
    buildDeliveryPr: 436,
    branch: PRODUCT_BRANCH,
    base: PRODUCT_BASE,
    head: PRODUCT_HEAD,
    tree: PRODUCT_TREE,
  },
  proof: {
    jvmAndStrings: './gradlew --no-daemon jvmTest checkVexLifeStrings',
    lensGenerator: 'generateVexLifeProdScreenLens',
    lensPath: 'docs/lens/vexlife-prod.screen-lens.json',
    buildTask: 'archiveVexLifeProdMacApplication',
    applicationName: 'VexLife.app',
    diskImageName: 'VexLife-macOS-AppleSilicon.dmg',
    launchAllowedByThisPayload: false,
    homeAccessAllowedByThisPayload: false,
    lifecycleMutationAllowedByThisPayload: false,
  },
};

const authority = {
  authoritySchemaRef: 'vextreme.effect-transaction/v1',
  semanticAdmissionRef: 'github.pull.vexlife-prod.436',
  transactionRef: 'transaction.vexlife-prod.web-restoration.mac-build-delivery.a001',
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
  taskRef: 'task.vexlife-prod.web-restoration.mac-build-delivery.a001',
  attemptRef: 'attempt.vexlife-prod.web-restoration.mac-build-delivery.a001',
  packageRef: 'package.vexlife-prod.web-restoration.mac-build-delivery.a001',
  requestRef: 'request.vexlife-prod.web-restoration.mac-build-delivery.a001',
  correlationRef: 'correlation.vexlife-prod.web-restoration.mac-build-delivery.a001',
  taskStorageKey: 'vexlife-prod-web-restoration-mac-build-delivery',
  attemptStorageKey: 'a001',
  requestingRoleRef: 'Operations[WebRestorationMacBuildDelivery]',
  requestingInstanceRef: 'instance.chatgpt.vexlife-web-restoration-mac-build-delivery.a001',
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
    'build.gradle.kts',
    'packaging/macos-app/release-contract.json',
    'packaging/private-service-image/setup/vexlife-prod-setup-window.applescript',
    'docs/lens/vexlife-prod.screen-lens.json',
    'src/commonMain/kotlin/vexlife/web/VexLifeWebP1RuntimeArtifacts.kt',
    'src/commonTest/kotlin/vexlife/web/VexLifeWebP1RuntimeArtifactsTest.kt',
    'src/jvmMain/kotlin/vexlife/service/VexLifeProdLocalService.kt',
    'src/jvmTest/kotlin/vexlife/localasr/VexLifeLocalAsrMacBundledRuntimeRegressionTest.kt',
    'src/jvmTest/kotlin/vexlife/service/VexLifeProdLocalServiceTest.kt',
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
    payloadClass: 'VEXLIFE_PROD_WEB_RESTORATION_MAC_BUILD_DELIVERY',
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
  singleUseNonce: 'nonce.vexlife-prod.web-restoration.mac-build-delivery.a001.20261120',
  stopConditions: [
    'stop.on.identity.drift',
    'stop.on.authority.missing',
    'stop.on.jvm-or-strings-failure',
    'stop.on.screen-lens-drift',
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
  outputStem: 'VEXLIFE-PR436-WEB-RESTORATION-MAC-BUILD-A001',
  ttlMs: 12 * 60 * 60 * 1000,
  archiveAdapter: adapter.archive,
});

writeJson(path.join(outputDir, 'FORMATION.json'), {
  schemaVersion: 'vexlife-prod.web-restoration.mac-build-delivery-formation/v1',
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
    resultConsumption: 'LOCAL_RETURN_CONSUMPTION_THEN_DMG_AND_RETURN_DELIVERY',
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
