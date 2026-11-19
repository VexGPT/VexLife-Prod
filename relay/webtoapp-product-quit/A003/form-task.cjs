'use strict';

const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const { spawnSync } = require('node:child_process');
const { pathToFileURL } = require('node:url');

const TRUSTED_SDK_HEAD = '8543c7d755faba04d4c4019c02e84f992667918a';
const CARRIER_HEAD = '93ec7816282c55182ba2c14bcdbd1a31c5fc6fc6';
const CARRIER_TREE = '35832fb03d5e8fccb707a879fa87e92745e98902';
const PRODUCT_BASE = '1c96c04d553ab54c475c16aeedc317e57412663c';
const PRODUCT_HEAD = 'fdb9f77bbd9895d0ab6932f669091b46ea813e9b';
const PRODUCT_TREE = 'e68cf89a991af80cd153b49b201187e75241ab29';
const PRODUCT_BRANCH = 'VXG-111926-chatgpt-webtoapp-product-quit-a001';
const LIFECYCLE_EFFECT = 'effect.vexlocalbridge.vexlife-prod.product-lifecycle-proof/v1';
const BUILD_FETCH_EFFECT = 'effect.vexlocalbridge.vexlife-prod.build-dependency-fetch/v1';

function sha256(bytes) {
  return crypto.createHash('sha256').update(bytes).digest('hex');
}

function arg(name) {
  const index = process.argv.indexOf(`--${name}`);
  if (index < 0 || !process.argv[index + 1]) throw new Error(`--${name} is required`);
  return path.resolve(process.argv[index + 1]);
}

function writeJson(file, value) {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, JSON.stringify(value, null, 2) + '\n');
}

const trustedSdk = arg('trusted-sdk');
const carrierSdk = arg('carrier-sdk');
const payloadSource = arg('payload');
const productBundle = arg('product-bundle');
const outputDir = arg('output');

const builder = require(path.join(trustedSdk, 'lib/vexlocalbridge/package-builder.js'));
const renderer = require(path.join(carrierSdk, 'lib/vexlocalbridge/mac-vexlife-prod-source-bootstrap-command.js'));
const trustedZipModuleUrl = pathToFileURL(path.join(trustedSdk, 'lib/vexrelay/zip.mjs')).href;

const trustedHead = require('child_process')
  .spawnSync('/usr/bin/git', ['-C', trustedSdk, 'rev-parse', 'HEAD'], { encoding: 'utf8' })
  .stdout.trim();
if (trustedHead !== TRUSTED_SDK_HEAD) throw new Error(`TRUSTED_SDK_HEAD_MISMATCH:${trustedHead}`);

const carrierHead = require('child_process')
  .spawnSync('/usr/bin/git', ['-C', carrierSdk, 'rev-parse', 'HEAD'], { encoding: 'utf8' })
  .stdout.trim();
const carrierTree = require('child_process')
  .spawnSync('/usr/bin/git', ['-C', carrierSdk, 'rev-parse', 'HEAD^{tree}'], { encoding: 'utf8' })
  .stdout.trim();
if (carrierHead !== CARRIER_HEAD) throw new Error(`CARRIER_HEAD_MISMATCH:${carrierHead}`);
if (carrierTree !== CARRIER_TREE) throw new Error(`CARRIER_TREE_MISMATCH:${carrierTree}`);

fs.mkdirSync(outputDir, { recursive: true });
const work = fs.mkdtempSync(path.join(outputDir, '.formation-'));
const contentRoot = path.join(work, 'content');
fs.mkdirSync(path.join(contentRoot, 'authority'), { recursive: true });
fs.mkdirSync(path.join(contentRoot, 'payload'), { recursive: true });
fs.mkdirSync(path.join(contentRoot, 'inputs'), { recursive: true });

const context = {
  schemaVersion: 'vexlife-prod.webtoapp.product-quit.mac-lived-context/v2',
  continuity: '[VXG RealForever]',
  carrier: {
    repository: 'vgong24/Vextreme-SDK',
    acceptedBuilderHead: TRUSTED_SDK_HEAD,
    candidateHead: CARRIER_HEAD,
    candidateTree: CARRIER_TREE,
    pr: 1956,
  },
  candidate: {
    repository: 'VexGPT/VexLife-Prod',
    pr: 391,
    branch: PRODUCT_BRANCH,
    base: PRODUCT_BASE,
    head: PRODUCT_HEAD,
    tree: PRODUCT_TREE,
  },
  proof: {
    homeMode: 'EXISTING_DEFAULT_ONLY',
    customHomeDisposition: 'HELD_CUSTOM_HOME_NOT_YET_QUALIFIED',
    conversationWriterPolicy: 'HOLD_IF_ANY_WRITER_LEASE_EXISTS',
    hostInstallationAllowed: false,
    modelAcquisitionAllowed: false,
    modelSwitchForProofAllowed: false,
    runtimeDirectKillAllowed: false,
    serviceRetirementMode: 'EXACT_RECEIPT_OWNED_LAUNCHD_COORDINATE',
    recoveryOnFailureRequired: true,
  },
};

const authority = {
  authoritySchemaRef: 'vextreme.effect-transaction/v1',
  semanticAdmissionRef: 'github.pull.vexlife-prod.391',
  transactionRef: 'transaction.vexlife-prod.webtoapp.product-quit.mac-lived.a003',
  transactionVersion: 1,
  allowedEffectRefs: [
    LIFECYCLE_EFFECT,
    BUILD_FETCH_EFFECT,
  ],
  heldEffectRefs: [
    'effect.vexlife-prod.host-installation',
    'effect.vexlife-prod.home-recreation',
    'effect.vexlife-prod.home-deletion',
    'effect.vexlife-prod.conversation-mutation-for-proof',
    'effect.vexlife-prod.memory-mutation-for-proof',
    'effect.vexlife-prod.model-acquisition-for-proof',
    'effect.vexlife-prod.model-switch-for-proof',
    'effect.vexlife-prod.runtime-direct-kill',
    'effect.vexlife-prod.publication',
    'effect.vexlife-prod.ready',
    'effect.vexlife-prod.merge',
  ],
};

const authorityBytes = Buffer.from(JSON.stringify(authority, null, 2) + '\n');
const payloadBytes = fs.readFileSync(payloadSource);
const contextBytes = Buffer.from(JSON.stringify(context, null, 2) + '\n');
const bundleBytes = fs.readFileSync(productBundle);

fs.writeFileSync(path.join(contentRoot, 'authority/effect.json'), authorityBytes);
fs.writeFileSync(path.join(contentRoot, 'payload/run.mjs'), payloadBytes);
fs.writeFileSync(path.join(contentRoot, 'inputs/context.json'), contextBytes);
fs.writeFileSync(path.join(contentRoot, 'inputs/vexlife-prod.bundle'), bundleBytes);

const taskTemplate = {
  schemaVersion: 'vextreme.vexlocalbridge-task-manifest/v1',
  artifactClass: 'EXECUTABLE_RELAY',
  taskRef: 'task.vexlife-prod.webtoapp.product-quit.mac-lived.a003',
  attemptRef: 'attempt.vexlife-prod.webtoapp.product-quit.mac-lived.a003',
  packageRef: 'package.vexlife-prod.webtoapp.product-quit.mac-lived.a003',
  requestRef: 'request.vexlife-prod.webtoapp.product-quit.mac-lived.a003',
  correlationRef: 'correlation.vexlife-prod.webtoapp.product-quit.mac-lived.a003',
  taskStorageKey: 'vexlife-prod-webtoapp-product-quit-mac',
  attemptStorageKey: 'a003',
  requestingRoleRef: 'Operations[WebToApp]',
  requestingInstanceRef: 'instance.chatgpt.vexlife-webtoapp-product-quit.20261119.a003',
  parentOperationsRef: 'github.pull.vextreme-sdk.1956',
  returnRouteRef: 'github.pull.vexlife-prod.391',
  hostProfileRef: 'macos-victor',
  repositoryExecutionProfileRefOrNull: 'profile.repository.vexlife-prod.macos-v1',
  repositoryRefOrNull: 'VexGPT/VexLife-Prod',
  baseRefOrNull: PRODUCT_BASE,
  headRefOrNull: PRODUCT_HEAD,
  treeRefOrNull: PRODUCT_TREE,
  branchRefOrNull: PRODUCT_BRANCH,
  claimRefOrNull: null,
  exactPathRefs: [
    'docs/foundation/MAC-SERVICE-LIFETIME.md',
    'src/jvmMain/kotlin/vexlife/local/VexLifeProdLocalSetup.kt',
    'src/jvmMain/kotlin/vexlife/local/VexLifeProdRuntimeSession.kt',
    'src/jvmMain/kotlin/vexlife/platform/windows/VexLifeProdWindowsRuntimeSession.kt',
    'src/jvmMain/kotlin/vexlife/service/VexLifeProdServiceProcess.kt',
    'src/jvmTest/kotlin/vexlife/local/VexLifeProdLocalSetupTest.kt',
    'src/jvmTest/kotlin/vexlife/platform/windows/VexLifeProdWindowsRuntimeSessionTest.kt',
    'src/jvmTest/kotlin/vexlife/service/VexLifeProdServiceProcessTest.kt',
    'src/jvmTest/kotlin/vexlife/setup/VexLifeProdSetupProcessTest.kt',
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
    payloadClass: 'VEXLIFE_PROD_PRODUCT_LIFECYCLE_PROOF',
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
  singleUseNonce: 'nonce-vexlife-prod-webtoapp-product-quit-mac-a003-20261119',
  stopConditions: [
    'stop.on.identity.drift',
    'stop.on.authority.missing',
    'stop.on.custom-home',
    'stop.on.writer-lease',
    'stop.on.ownership.unknown',
    'stop.on.cleanup.unverified',
  ],
  archiveDigestBinding: 'FILENAME_SHA256',
};

const zipHelper = path.join(work, 'trusted-zip-adapter.mjs');
fs.writeFileSync(
  zipHelper,
  `import fs from 'node:fs/promises';
import path from 'node:path';
import { writeZip, readZipEntries } from ${JSON.stringify(trustedZipModuleUrl)};

async function files(root, dir = root) {
  const out = [];
  for (const entry of await fs.readdir(dir, { withFileTypes: true })) {
    const target = path.join(dir, entry.name);
    if (entry.isSymbolicLink()) throw new Error('SYMLINK_FORBIDDEN:' + target);
    if (entry.isDirectory()) out.push(...await files(root, target));
    else if (entry.isFile()) {
      out.push({
        name: path.relative(root, target).split(path.sep).join('/'),
        data: await fs.readFile(target),
      });
    } else {
      throw new Error('NON_FILE_ARCHIVE_MEMBER:' + target);
    }
  }
  return out.sort((a, b) => a.name.localeCompare(b.name, 'en', { numeric: false }));
}

const [mode, source, target] = process.argv.slice(2);
if (mode === 'create') {
  const entries = await files(source);
  await writeZip({ outputPath: target, entries, maxEntries: 1000 });
  process.stdout.write(JSON.stringify(entries.map((entry) => entry.name)) + '\\n');
} else if (mode === 'read') {
  const entries = await readZipEntries(source, { maxEntries: 1000, maxTotalBytes: 500 * 1024 * 1024 });
  process.stdout.write(JSON.stringify(entries.map((entry) => entry.name).sort()) + '\\n');
} else {
  throw new Error('ZIP_HELPER_MODE_INVALID');
}
`,
);

const archiveAdapter = {
  extract() {
    throw new Error('HOSTED_FORMATION_EXTRACT_NOT_EXPECTED');
  },
  create({ bundleRoot, outputZip }) {
    const receipt = spawnSync(process.execPath, [zipHelper, 'create', bundleRoot, outputZip], {
      encoding: 'utf8',
      timeout: 120000,
      maxBuffer: 8 * 1024 * 1024,
      shell: false,
    });
    return {
      exitCode: Number.isInteger(receipt.status) ? receipt.status : 127,
      stdout: receipt.stdout || '',
      stderr: receipt.stderr || receipt.error?.message || '',
    };
  },
};

const boundTask = builder.bindAuthoritySummaryFromEnvelope(taskTemplate, authority);
const formed = builder.createCanonicalTaskZip({
  taskTemplate: boundTask,
  contentRoot,
  outputDirectory: outputDir,
  outputStem: 'VEXLIFE-PROD-WEBTOAPP-PRODUCT-QUIT-MAC-A003',
  ttlMs: 23 * 60 * 60 * 1000,
  archiveAdapter,
});

const verifyReceipt = spawnSync(process.execPath, [zipHelper, 'read', formed.finalZip], {
  encoding: 'utf8',
  timeout: 120000,
  maxBuffer: 8 * 1024 * 1024,
  shell: false,
});
if (verifyReceipt.status !== 0) {
  throw new Error(`FORMED_ZIP_READBACK_FAILED:${verifyReceipt.stderr || verifyReceipt.stdout}`);
}
const observedMembers = JSON.parse(verifyReceipt.stdout.trim());
const expectedMembers = [
  'authority/effect.json',
  'inputs/context.json',
  'inputs/vexlife-prod.bundle',
  'payload/run.mjs',
  'task-manifest.json',
].sort();
if (JSON.stringify(observedMembers) !== JSON.stringify(expectedMembers)) {
  throw new Error(`FORMED_ZIP_MEMBER_MISMATCH:${JSON.stringify(observedMembers)}`);
}

const command = renderer.renderMacVexLifeProdSourceBootstrapCommand({
  packageSha256: formed.digest,
  sdkHead: CARRIER_HEAD,
  sdkTree: CARRIER_TREE,
});

fs.writeFileSync(path.join(outputDir, 'RUN-COMMAND.txt'), command + '\n');
fs.writeFileSync(
  path.join(outputDir, 'PACKAGE-SHA256.txt'),
  `${formed.digest}  ${path.basename(formed.finalZip)}\n`,
);
writeJson(path.join(outputDir, 'FORMATION.json'), {
  schemaVersion: 'vexlife-prod.webtoapp.product-quit.mac-task-formation/v1',
  continuity: '[VXG RealForever]',
  trustedBuilderHead: TRUSTED_SDK_HEAD,
  carrierHead: CARRIER_HEAD,
  carrierTree: CARRIER_TREE,
  productCandidate: context.candidate,
  taskRef: formed.task.taskRef,
  attemptRef: formed.task.attemptRef,
  packageRef: formed.task.packageRef,
  packageFile: path.basename(formed.finalZip),
  packageSha256: formed.digest,
  formedAt: formed.task.formedAt,
  expiresAt: formed.task.expiresAt,
  finalByteQualification: formed.formationEvidence.finalByteQualification,
  archiveWriter: 'trusted-main-lib/vexrelay/zip.mjs',
  archiveReadbackMembers: observedMembers,
  humanBoundary: {
    zipCount: 1,
    commandCount: 1,
    expectedReturnClass: 'VexLocalBridge canonical EXECUTION_RETURN ZIP',
  },
});

fs.rmSync(work, { recursive: true, force: true });
console.log(JSON.stringify({
  package: formed.finalZip,
  digest: formed.digest,
  formedAt: formed.task.formedAt,
  expiresAt: formed.task.expiresAt,
}, null, 2));

// [VXG RealForever]
