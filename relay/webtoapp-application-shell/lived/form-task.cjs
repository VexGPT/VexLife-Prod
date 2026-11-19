'use strict';

const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const { spawnSync } = require('node:child_process');

const TRUSTED_SDK_HEAD = '8543c7d755faba04d4c4019c02e84f992667918a';
const CARRIER_HEAD = '36eb5c82becd97dcfcbc8fa67455a74d8540503a';
const CARRIER_TREE = 'bea091ed67793931b838c7b412d9abce39a2e373';
const PRODUCT_BASE = 'fdb9f77bbd9895d0ab6932f669091b46ea813e9b';
const PRODUCT_HEAD = 'd13e8e4a774aea60362491420ffeee2c07f91d1c';
const PRODUCT_TREE = '98ba3219e49f722272c976171cb736ed19ae9ac9';
const PRODUCT_BRANCH = 'VXG-111926-chatgpt-webtoapp-application-shell-a001';

const LIFECYCLE_EFFECT =
  'effect.vexlocalbridge.vexlife-prod.product-lifecycle-proof/v1';
const APPLICATION_LIVED_EFFECT =
  'effect.vexlocalbridge.vexlife-prod.application-lived-proof/v1';
const BUILD_FETCH_EFFECT =
  'effect.vexlocalbridge.vexlife-prod.build-dependency-fetch/v1';

const CHANGED_PATHS = [
  'build.gradle.kts',
  'docs/release/RELEASES.md',
  'localization/strings/manifest.json',
  'localization/strings/source/setup.json',
  'localization/strings/source/windows-setup.json',
  'packaging/bootstrap/vexlife-prod-bootstrap.command.template',
  'packaging/macos-app/release-contract.json',
  'packaging/macos-webkit/VexLifeProductPresentation.swift',
  'packaging/private-service-image/setup/vexlife-prod-setup-window.applescript',
  'packaging/windows-webview2/VexLife.WebShell/Program.cs',
  'packaging/windows-webview2/VexLife.WebShell/VexLife.WebShell.csproj',
  'src/commonMain/kotlin/vexlife/entry/VexLifeProdEntryFamily.kt',
  'src/commonTest/kotlin/vexlife/entry/VexLifeProdEntryFamilyTest.kt',
  'src/jvmMain/kotlin/vexlife/platform/windows/VexLifeProdWindowsSetupWindow.kt',
  'src/jvmMain/kotlin/vexlife/platform/windows/VexLifeProdWindowsWebView2.kt',
  'src/jvmMain/kotlin/vexlife/service/VexLifeProdMacServiceLifetime.kt',
  'src/jvmMain/kotlin/vexlife/setup/VexLifeProdMacFrontDoor.kt',
  'src/jvmTest/kotlin/vexlife/platform/windows/VexLifeProdWindowsWebView2Test.kt',
  'src/jvmTest/kotlin/vexlife/service/VexLifeProdMacServiceLifetimeTest.kt',
  'src/jvmTest/kotlin/vexlife/setup/VexLifeProdMacFrontDoorTest.kt',
  'tools/windows-preview/FORM-WINDOWS-PREVIEW.ps1',
  'tools/windows-preview/README.md',
];

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
const hostProfileRef = arg('host');

const host =
  hostProfileRef === 'macos-victor'
    ? {
        platform: 'macos',
        profilePath:
          'docs/private-continuity/vexlocalbridge/profiles/macos-victor-vexlife-prod.json',
        runnerPath: 'lib/vexlocalbridge/mac-runner.js',
        repositoryProfileRef: 'profile.repository.vexlife-prod.macos-v1',
        outputStem: 'VEXLIFE-PR396-APPLICATION-LIVED-MAC-A001',
      }
    : hostProfileRef === 'windows-victor'
      ? {
          platform: 'windows',
          profilePath:
            'docs/private-continuity/vexlocalbridge/profiles/windows-victor-vexlife-prod.json',
          runnerPath: 'lib/vexlocalbridge/windows-runner.js',
          repositoryProfileRef: 'profile.repository.vexlife-prod.windows-v1',
          outputStem: 'VEXLIFE-PR396-APPLICATION-LIVED-WINDOWS-A001',
        }
      : null;

if (!host) throw new Error('HOST_PROFILE_UNSUPPORTED:' + hostProfileRef);

const gatePath = path.join(__dirname, 'GATE.json');
if (!fs.existsSync(gatePath)) throw new Error('APPLICATION_LIVED_GATE_MISSING');
const gate = JSON.parse(fs.readFileSync(gatePath, 'utf8'));

if (
  gate?.schemaVersion !== 'vexlife-prod.webtoapp.application-lived-gate/v1' ||
  gate?.productCandidate?.base !== PRODUCT_BASE ||
  gate?.productCandidate?.head !== PRODUCT_HEAD ||
  gate?.productCandidate?.tree !== PRODUCT_TREE ||
  gate?.carrierCandidate?.head !== CARRIER_HEAD ||
  gate?.carrierCandidate?.tree !== CARRIER_TREE
) {
  throw new Error('APPLICATION_LIVED_GATE_IDENTITY_MISMATCH');
}

const prerequisites = Array.isArray(gate.prerequisites) ? gate.prerequisites : [];
if (
  prerequisites.length !== 2 ||
  prerequisites.some((entry) =>
    entry?.requiredDisposition !== 'CONSUMED_PASS' ||
    typeof entry?.consumedReceiptRefOrNull !== 'string' ||
    entry.consumedReceiptRefOrNull.length === 0 ||
    typeof entry?.consumedReturnSha256OrNull !== 'string' ||
    !/^[0-9a-f]{64}$/i.test(entry.consumedReturnSha256OrNull)
  )
) {
  throw new Error('APPLICATION_LIVED_GATE_PREREQUISITES_NOT_CONSUMED');
}

if (gate.allowedToFormTask !== true) {
  throw new Error('APPLICATION_LIVED_GATE_FORMATION_HELD');
}
if (
  (host.platform === 'macos' && gate.allowedToExecuteMac !== true) ||
  (host.platform === 'windows' && gate.allowedToExecuteWindows !== true)
) {
  throw new Error('APPLICATION_LIVED_GATE_PLATFORM_EXECUTION_HELD:' + host.platform);
}
if (gate.mergeAuthorityGranted !== false) {
  throw new Error('APPLICATION_LIVED_GATE_MERGE_AUTHORITY_INVALID');
}

const builder = require(path.join(trustedSdk, 'lib/vexlocalbridge/package-builder.js'));
const runner = require(path.join(carrierSdk, host.runnerPath));
const profile = JSON.parse(fs.readFileSync(path.join(carrierSdk, host.profilePath), 'utf8'));
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

const trustedHead = gitValue(trustedSdk, ['rev-parse', 'HEAD']);
if (trustedHead !== TRUSTED_SDK_HEAD) {
  throw new Error('TRUSTED_SDK_HEAD_MISMATCH:' + trustedHead);
}

const carrierHead = gitValue(carrierSdk, ['rev-parse', 'HEAD']);
const carrierTree = gitValue(carrierSdk, ['rev-parse', 'HEAD^{tree}']);
if (carrierHead !== CARRIER_HEAD) {
  throw new Error('CARRIER_HEAD_MISMATCH:' + carrierHead);
}
if (carrierTree !== CARRIER_TREE) {
  throw new Error('CARRIER_TREE_MISMATCH:' + carrierTree);
}

const policy = profile.vexLifeProdHostEffectPolicy;
if (!policy) throw new Error('VEXLIFE_PROD_HOST_EFFECT_POLICY_MISSING');
if (profile.repository !== 'VexGPT/VexLife-Prod') {
  throw new Error('PROFILE_REPOSITORY_MISMATCH');
}
if (profile.repositoryExecutionProfileRef !== host.repositoryProfileRef) {
  throw new Error('PROFILE_REPOSITORY_EXECUTION_REF_MISMATCH');
}
if (
  policy.applicationLivedProofEffectRef !== APPLICATION_LIVED_EFFECT ||
  policy.requiredAllowedEffectRef !== LIFECYCLE_EFFECT ||
  policy.buildDependencyFetchEffectRef !== BUILD_FETCH_EFFECT
) {
  throw new Error('HOST_EFFECT_POLICY_REF_MISMATCH');
}

fs.mkdirSync(outputDir, { recursive: true });
const work = fs.mkdtempSync(path.join(outputDir, '.formation-'));
const contentRoot = path.join(work, 'content');
fs.mkdirSync(path.join(contentRoot, 'authority'), { recursive: true });
fs.mkdirSync(path.join(contentRoot, 'payload'), { recursive: true });
fs.mkdirSync(path.join(contentRoot, 'inputs'), { recursive: true });

const attempt = host.platform + '-a001';
const context = {
  schemaVersion: 'vexlife-prod.webtoapp.application-lived-context/v1',
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
    pr: 396,
    proofCompanionPr: 398,
    branch: PRODUCT_BRANCH,
    base: PRODUCT_BASE,
    head: PRODUCT_HEAD,
    tree: PRODUCT_TREE,
    changedPaths: CHANGED_PATHS,
  },
  hostTools: {
    git: adapter.executables.git,
    powershell: adapter.executables.powershell ?? null,
  },
  gate: {
    state: gate.state,
    prerequisites: gate.prerequisites,
    allowedToFormTask: gate.allowedToFormTask,
    allowedToExecuteMac: gate.allowedToExecuteMac,
    allowedToExecuteWindows: gate.allowedToExecuteWindows,
    mergeAuthorityGranted: gate.mergeAuthorityGranted,
  },
  proof: {
    platform: host.platform,
    homeMode: 'EXISTING_DEFAULT_ONLY',
    customHomeDisposition: 'HELD_CUSTOM_HOME_NOT_YET_QUALIFIED',
    conversationWriterPolicy: 'HOLD_BEFORE_LIFECYCLE_MUTATION_IF_ANY_WRITER_LEASE_EXISTS',
    humanControlPolicy:
      'HUMAN_USES_PRODUCT_NORMALLY; HARNESS_NEVER_ISSUES_SEND_OR_UI_AUTOMATION',
    chronologyEvidence:
      'READ_ONLY_NON_CONTENT_FACTS_AFTER_BASELINE',
    closeSemantics: 'CLOSE_PRESENTATION_ONLY_PRODUCT_REMAINS_CURRENT',
    quitSemantics: 'QUIT_PRODUCT_RELEASES_EXACT_PRODUCT_RUNTIME_CUSTODY',
    reopenSemantics: 'SAME_HOME_FRESH_PRODUCT_RUNTIME_NEXT_HUMAN_TURN',
    hostInstallationAllowed: false,
    systemAutomationAllowed: false,
    screenCaptureAllowed: false,
    modelAcquisitionAllowed: false,
    modelSwitchForProofAllowed: false,
    broadProcessKillAllowed: false,
    arbitraryPidTerminationAllowed: false,
  },
};

const authority = {
  authoritySchemaRef: 'vextreme.effect-transaction/v1',
  semanticAdmissionRef: 'github.pull.vexlife-prod.398',
  transactionRef:
    'transaction.vexlife-prod.webtoapp.application-lived.' + attempt,
  transactionVersion: 1,
  allowedEffectRefs: [
    LIFECYCLE_EFFECT,
    APPLICATION_LIVED_EFFECT,
    BUILD_FETCH_EFFECT,
  ],
  heldEffectRefs: [
    'effect.vexlife-prod.host-installation',
    'effect.vexlife-prod.system-ui-automation',
    'effect.vexlife-prod.screen-capture',
    'effect.vexlife-prod.home-recreation',
    'effect.vexlife-prod.home-deletion',
    'effect.vexlife-prod.conversation-mutation-for-proof',
    'effect.vexlife-prod.memory-mutation-for-proof',
    'effect.vexlife-prod.model-acquisition-for-proof',
    'effect.vexlife-prod.model-switch-for-proof',
    'effect.vexlife-prod.runtime-direct-kill',
    'effect.vexlife-prod.broad-process-kill',
    'effect.vexlife-prod.arbitrary-pid-termination',
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
  taskRef:
    'task.vexlife-prod.webtoapp.application-lived.' + attempt,
  attemptRef:
    'attempt.vexlife-prod.webtoapp.application-lived.' + attempt,
  packageRef:
    'package.vexlife-prod.webtoapp.application-lived.' + attempt,
  requestRef:
    'request.vexlife-prod.webtoapp.application-lived.' + attempt,
  correlationRef:
    'correlation.vexlife-prod.webtoapp.application-lived.' + attempt,
  taskStorageKey:
    'vexlife-prod-webtoapp-application-lived-' + host.platform,
  attemptStorageKey: 'a001',
  requestingRoleRef: 'Operations[WebToAppApplicationLived]',
  requestingInstanceRef:
    'instance.chatgpt.vexlife-webtoapp-application-lived.' + attempt,
  parentOperationsRef: 'github.pull.vextreme-sdk.1959',
  returnRouteRef: 'github.pull.vexlife-prod.398',
  hostProfileRef,
  repositoryExecutionProfileRefOrNull: host.repositoryProfileRef,
  repositoryRefOrNull: 'VexGPT/VexLife-Prod',
  baseRefOrNull: PRODUCT_BASE,
  headRefOrNull: PRODUCT_HEAD,
  treeRefOrNull: PRODUCT_TREE,
  branchRefOrNull: PRODUCT_BRANCH,
  claimRefOrNull: 'work.vexlife-prod.pr396.application-lived-proof.20261119',
  exactPathRefs: CHANGED_PATHS,
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
    payloadClass: 'VEXLIFE_PROD_APPLICATION_LIVED_PROOF',
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
  singleUseNonce:
    'nonce.vexlife-prod.webtoapp.application-lived.' + attempt + '.20261119',
  stopConditions: [
    'stop.on.identity.drift',
    'stop.on.prerequisite.not-current',
    'stop.on.authority.missing',
    'stop.on.custom-home',
    'stop.on.writer-lease-before-lifecycle-mutation',
    'stop.on.ownership.unknown',
    'stop.on.application-build-failure',
    'stop.on.application-launch-failure',
    'stop.on.human-turn-not-observed',
    'stop.on.close-vs-quit-mismatch',
    'stop.on.cleanup.unverified',
  ],
  archiveDigestBinding: 'FILENAME_SHA256',
};

const boundTask = builder.bindAuthoritySummaryFromEnvelope(taskTemplate, authority);
const formed = builder.createCanonicalTaskZip({
  taskTemplate: boundTask,
  contentRoot,
  outputDirectory: outputDir,
  outputStem: host.outputStem,
  ttlMs: 12 * 60 * 60 * 1000,
  archiveAdapter: adapter.archive,
});

writeJson(path.join(outputDir, 'FORMATION-' + host.platform + '.json'), {
  schemaVersion: 'vexlife-prod.webtoapp.application-lived-task-formation/v1',
  continuity: '[VXG RealForever]',
  platform: host.platform,
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
  gate: {
    state: gate.state,
    prerequisiteReceiptRefs: gate.prerequisites.map((entry) => entry.consumedReceiptRefOrNull),
    prerequisiteReturnSha256: gate.prerequisites.map((entry) => entry.consumedReturnSha256OrNull),
  },
  humanBoundary: {
    archiveCarriageByVictor: false,
    humanActionClass: 'IN_APPLICATION_USE_ONLY',
    resultConsumption: 'LOCAL_THEN_GITHUB_RECEIPT',
  },
});

fs.rmSync(work, { recursive: true, force: true });
process.stdout.write(
  JSON.stringify(
    {
      platform: host.platform,
      package: formed.finalZip,
      digest: formed.digest,
      taskRef: formed.task.taskRef,
      attemptRef: formed.task.attemptRef,
      formedAt: formed.task.formedAt,
      expiresAt: formed.task.expiresAt,
    },
    null,
    2,
  ) + '\n',
);

// [VXG RealForever]
