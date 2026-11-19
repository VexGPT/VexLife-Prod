import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { spawnSync } from 'node:child_process';

const RESULT_SCHEMA = 'vextreme.vexlocalbridge-mac-payload-result/v1';
const POLICY_ENV = 'VEXLOCALBRIDGE_VEXLIFE_PROD_HOST_EFFECT_POLICY_JSON';
const RESULT_DIR = process.env.VEXLOCALBRIDGE_RESULT_DIR;
const CAPSULE_ROOT = process.env.VEXLOCALBRIDGE_CAPSULE_ROOT;
const BUILD_FETCH_EFFECT = 'effect.vexlocalbridge.vexlife-prod.build-dependency-fetch/v1';
const APPLICATION_LIVED_EFFECT = 'effect.vexlocalbridge.vexlife-prod.application-lived-proof/v1';

const effects = {
  sourceMutationPerformed: false,
  branchMutationPerformed: false,
  claimMutationPerformed: false,
  prMetadataMutationPerformed: false,
  reviewPerformed: false,
  approvalPerformed: false,
  readyTransitionPerformed: false,
  mergePerformed: false,
  mainMutationPerformed: false,
  checkpointMutationPerformed: false,
  forcePushPerformed: false,
  historyRewritePerformed: false,
  hostInstallationPerformed: false,
  modelRuntimePerformed: false,
  providerOrNetworkEffectPerformed: false,
  publicationPerformed: false,
  credentialSerializationPerformed: false,
  filesystemOutsideQualifiedRootsPerformed: false,
};

const completedStepReceipts = [];
const candidateFindings = [];
const harnessFindings = [];
const environmentFindings = [];
const unknownFindings = [];
const beforeState = {};
const afterState = {};

function fail(code, message = code) {
  const error = new Error(message);
  error.code = code;
  throw error;
}
function readJson(file) {
  return JSON.parse(fs.readFileSync(file, 'utf8'));
}
function sha256File(file) {
  return crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex');
}
function truncate(value, limit = 12000) {
  const text = String(value || '');
  return text.length <= limit ? text : text.slice(text.length - limit);
}
function run(executable, args, {
  cwd = process.cwd(),
  timeoutMs = 10 * 60 * 1000,
  env = {},
  acceptedExitCodes = [0],
  label = path.basename(executable),
} = {}) {
  if (!path.isAbsolute(executable)) fail('EXECUTABLE_NOT_ABSOLUTE', executable);
  const receipt = spawnSync(executable, args, {
    cwd,
    env: { ...process.env, ...env },
    encoding: 'utf8',
    timeout: timeoutMs,
    maxBuffer: 48 * 1024 * 1024,
    shell: false,
  });
  const exitCode = Number.isInteger(receipt.status) ? receipt.status : 127;
  const out = truncate(receipt.stdout);
  const err = truncate(receipt.stderr || receipt.error?.message || '');
  console.log('COMMAND ' + label + ' exit=' + exitCode);
  if (out) console.log(out);
  if (err) console.error(err);
  if (!acceptedExitCodes.includes(exitCode)) {
    const error = new Error(label + ' failed with exit ' + exitCode + ': ' + (err || out));
    error.code = label.toUpperCase().replace(/[^A-Z0-9]+/g, '_') + '_FAILED';
    throw error;
  }
  return { exitCode, stdout: receipt.stdout || '', stderr: receipt.stderr || '' };
}
function plutilRaw(infoPlist, key) {
  return run('/usr/bin/plutil', ['-extract', key, 'raw', '-o', '-', infoPlist], {
    cwd: path.dirname(infoPlist),
    timeoutMs: 10000,
    label: 'plist-' + key,
  }).stdout.trim();
}
function writeResult(disposition, stopReasonOrNull, retainedEvidenceFiles = []) {
  if (!RESULT_DIR) throw new Error('VEXLOCALBRIDGE_RESULT_DIR_MISSING');
  fs.mkdirSync(RESULT_DIR, { recursive: true });
  const value = {
    schemaVersion: RESULT_SCHEMA,
    disposition,
    stopReasonOrNull,
    firstSubstantiveFailureOrNull: null,
    acceptedExitCodes: [0],
    candidateFindings,
    harnessFindings,
    environmentFindings,
    unknownFindings,
    effects,
    beforeState,
    afterState,
    completedStepReceipts,
    retainedEvidenceFiles,
  };
  fs.writeFileSync(
    path.join(RESULT_DIR, 'payload-result.json'),
    JSON.stringify(value, null, 2) + '\n',
  );
}

async function main() {
  if (process.platform !== 'darwin') fail('MACOS_REQUIRED');
  if (!['arm64', 'aarch64'].includes(process.arch)) fail('APPLE_SILICON_REQUIRED');
  if (!RESULT_DIR || !CAPSULE_ROOT) fail('VEXLOCALBRIDGE_ENVIRONMENT_MISSING');

  const context = readJson(path.join(process.cwd(), 'inputs', 'context.json'));
  const policy = JSON.parse(process.env[POLICY_ENV] || 'null');
  if (!policy) fail('VEXLIFE_PROD_HOST_EFFECT_POLICY_MISSING');

  if (
    context?.candidate?.head !== '3fe20be5d4c5f80ff5f9e066862e971b325159e1' ||
    context?.candidate?.tree !== '0eb4db2c0493285f65bbfa3660a32dde2a51385c' ||
    context?.carrier?.candidateHead !== '36eb5c82becd97dcfcbc8fa67455a74d8540503a'
  ) {
    fail('IMMUTABLE_CONTEXT_MISMATCH');
  }
  if (
    policy.repository !== 'VexGPT/VexLife-Prod' ||
    policy.hostProfileRef !== 'macos-victor' ||
    policy.buildDependencyFetchEffectRef !== BUILD_FETCH_EFFECT ||
    policy.buildDependencyFetchAuthorized !== true ||
    policy.applicationLivedProofEffectRef !== APPLICATION_LIVED_EFFECT ||
    policy.applicationLivedProofAuthorized !== true ||
    policy.applicationProofPolicy?.buildTaskOwnedApplicationAllowed !== true
  ) {
    fail('MAC_BUILD_AUTHORITY_REQUIRED');
  }
  if (
    policy.applicationProofPolicy?.hostInstallationAllowed !== false ||
    policy.applicationProofPolicy?.systemAutomationAllowed !== false ||
    policy.applicationProofPolicy?.screenCaptureAllowed !== false ||
    policy.processPolicy?.broadProcessKillAllowed !== false ||
    policy.processPolicy?.arbitraryPidTerminationAllowed !== false ||
    policy.productDataPolicy?.conversationMutationAllowed !== false ||
    policy.productDataPolicy?.memoryMutationAllowed !== false ||
    policy.productDataPolicy?.homeRecreationAllowed !== false ||
    policy.productDataPolicy?.homeDeletionAllowed !== false ||
    policy.productDataPolicy?.modelAcquisitionAllowed !== false ||
    policy.productDataPolicy?.modelSwitchForProofAllowed !== false
  ) {
    fail('MAC_BUILD_POLICY_WIDENED');
  }

  beforeState.productLifecycleMutationAttempted = false;
  beforeState.applicationLaunchAttempted = false;
  beforeState.homeReadAttempted = false;
  beforeState.modelEffectAttempted = false;
  completedStepReceipts.push({ step: 'build-only-policy', state: 'PASS' });

  const bundlePath = path.join(process.cwd(), 'inputs', 'vexlife-prod.bundle');
  if (!fs.existsSync(bundlePath)) fail('IMMUTABLE_PRODUCT_BUNDLE_MISSING');

  const sourceRoot = path.join(CAPSULE_ROOT, 'mac-build-product-source');
  fs.rmSync(sourceRoot, { recursive: true, force: true });
  run('/usr/bin/git', ['clone', bundlePath, sourceRoot], {
    cwd: CAPSULE_ROOT,
    timeoutMs: 120000,
    label: 'clone-immutable-product-bundle',
  });
  run('/usr/bin/git', ['checkout', '--detach', context.candidate.head], {
    cwd: sourceRoot,
    timeoutMs: 30000,
    label: 'checkout-exact-product-head',
  });
  const observedHead = run('/usr/bin/git', ['rev-parse', 'HEAD'], {
    cwd: sourceRoot,
    timeoutMs: 10000,
    label: 'product-head',
  }).stdout.trim();
  const observedTree = run('/usr/bin/git', ['rev-parse', 'HEAD^{tree}'], {
    cwd: sourceRoot,
    timeoutMs: 10000,
    label: 'product-tree',
  }).stdout.trim();
  if (observedHead !== context.candidate.head) fail('PRODUCT_HEAD_MISMATCH');
  if (observedTree !== context.candidate.tree) fail('PRODUCT_TREE_MISMATCH');
  run('/usr/bin/git', ['merge-base', '--is-ancestor', context.candidate.base, context.candidate.head], {
    cwd: sourceRoot,
    timeoutMs: 10000,
    label: 'product-base-ancestor',
  });
  completedStepReceipts.push({ step: 'exact-product-source', state: 'PASS' });

  const gradleHome = path.join(CAPSULE_ROOT, 'mac-build-gradle-home');
  fs.mkdirSync(gradleHome, { recursive: true });
  const buildEnv = {
    GRADLE_USER_HOME: gradleHome,
    GRADLE_OPTS:
      '-Dorg.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m -Dorg.gradle.workers.max=1 -Dkotlin.compiler.execution.strategy=in-process',
    KOTLIN_DAEMON_JVMARGS: '-Xmx4096m',
    KONAN_DATA_DIR: path.join(CAPSULE_ROOT, 'mac-build-konan-data'),
  };

  effects.providerOrNetworkEffectPerformed = true;
  run('/bin/bash', [
    'gradlew',
    '--no-daemon',
    '--max-workers=1',
    '-PvexlifeAppVersion=0.0.0',
    'archiveVexLifeProdMacApplication',
  ], {
    cwd: sourceRoot,
    timeoutMs: 45 * 60 * 1000,
    env: buildEnv,
    label: 'mac-app-formation',
  });
  completedStepReceipts.push({ step: 'mac-app-formation', state: 'PASS' });

  const outputRoot = path.join(
    sourceRoot,
    'build',
    'vexlife-prod-mac-application',
    context.candidate.head,
  );
  const app = path.join(outputRoot, 'VexLife.app');
  const info = path.join(app, 'Contents', 'Info.plist');
  const dmg = path.join(outputRoot, 'VexLife-macOS-AppleSilicon.dmg');
  const receiptPath = path.join(outputRoot, 'VexLife-macOS-AppleSilicon.receipt.json');
  const shaPath = path.join(outputRoot, 'VexLife-macOS-AppleSilicon.dmg.sha256');

  for (const file of [info, dmg, receiptPath, shaPath]) {
    if (!fs.existsSync(file) || !fs.lstatSync(file).isFile()) {
      fail('MAC_BUILD_MEMBER_MISSING', file);
    }
  }

  const expectedSource = plutilRaw(info, 'VexLifeProdExpectedSourceSha');
  const presentationMode = plutilRaw(info, 'VexLifeProdPresentationMode');
  const executableName = plutilRaw(info, 'CFBundleExecutable');
  if (expectedSource !== context.candidate.head) fail('MAC_BUILD_SOURCE_BINDING_MISMATCH');
  if (presentationMode !== 'NATIVE_EMBEDDED') fail('MAC_BUILD_NOT_NATIVE_EMBEDDED');
  if (!executableName || executableName.includes('/') || executableName.includes('\\')) {
    fail('MAC_BUILD_EXECUTABLE_COORDINATE_INVALID');
  }
  const executable = path.join(app, 'Contents', 'MacOS', executableName);
  if (!fs.existsSync(executable) || !fs.lstatSync(executable).isFile()) {
    fail('MAC_BUILD_EXECUTABLE_MISSING');
  }
  fs.accessSync(executable, fs.constants.X_OK);

  run('/usr/bin/codesign', ['--verify', '--strict', app], {
    cwd: outputRoot,
    timeoutMs: 30000,
    label: 'verify-outer-app-code-signature',
  });
  const nested = path.join(app, 'Contents', 'Helpers', 'VexLifeProduct.app');
  run('/usr/bin/codesign', ['--verify', '--strict', nested], {
    cwd: outputRoot,
    timeoutMs: 30000,
    label: 'verify-nested-product-code-signature',
  });
  run('/usr/bin/hdiutil', ['verify', dmg], {
    cwd: outputRoot,
    timeoutMs: 120000,
    label: 'verify-dmg',
  });

  const receipt = readJson(receiptPath);
  const dmgSha256 = sha256File(dmg);
  if (
    receipt?.schemaVersion !== 'vexlife-prod.macos-application-archive-receipt/v1' ||
    receipt?.sourceSha !== context.candidate.head ||
    receipt?.diskImageName !== 'VexLife-macOS-AppleSilicon.dmg' ||
    receipt?.diskImageSha256 !== dmgSha256 ||
    receipt?.distributionTrust !== 'AD_HOC_SIGNATURE_NO_DEVELOPER_ID_NO_NOTARIZATION' ||
    receipt?.notarized !== false
  ) {
    fail('MAC_BUILD_RECEIPT_INVALID');
  }
  const sidecar = fs.readFileSync(shaPath, 'utf8').trim();
  if (sidecar !== dmgSha256 + '  VexLife-macOS-AppleSilicon.dmg') {
    fail('MAC_BUILD_SHA256_SIDECAR_MISMATCH');
  }

  afterState.build = {
    sourceSha: context.candidate.head,
    sourceTree: context.candidate.tree,
    applicationName: 'VexLife.app',
    presentationMode,
    executableName,
    diskImageName: receipt.diskImageName,
    diskImageSha256: dmgSha256,
    diskImageBytes: fs.statSync(dmg).size,
    distributionTrust: receipt.distributionTrust,
    notarized: receipt.notarized,
    applicationLaunchPerformed: false,
    productLifecycleMutationPerformed: false,
    homeMutationPerformed: false,
    modelEffectPerformed: false,
  };
  completedStepReceipts.push({ step: 'exact-mac-build-verified', state: 'PASS' });

  const retainedSourceRoot = path.join(
    CAPSULE_ROOT,
    'bridge',
    'retained-build',
    'vexlife-prod-webtoapp-mac-build-delivery',
    'a007',
  );
  const resultDirResolved = path.resolve(RESULT_DIR);
  const retainedResolved = path.resolve(retainedSourceRoot);
  if (
    retainedResolved === resultDirResolved ||
    retainedResolved.startsWith(resultDirResolved + path.sep)
  ) {
    fail('RETAINED_EVIDENCE_TRANSIENT_WORK_ROOT');
  }
  fs.rmSync(retainedSourceRoot, { recursive: true, force: true });
  fs.mkdirSync(retainedSourceRoot, { recursive: true });

  const retainedDmg = path.join(retainedSourceRoot, 'VexLife-macOS-AppleSilicon.dmg');
  const retainedReceipt = path.join(
    retainedSourceRoot,
    'VexLife-macOS-AppleSilicon.receipt.json',
  );
  const retainedSha = path.join(
    retainedSourceRoot,
    'VexLife-macOS-AppleSilicon.dmg.sha256',
  );

  for (const [source, target] of [
    [dmg, retainedDmg],
    [receiptPath, retainedReceipt],
    [shaPath, retainedSha],
  ]) {
    fs.copyFileSync(source, target);
    if (sha256File(source) !== sha256File(target)) {
      fail('MAC_BUILD_RETAINED_SOURCE_COPY_MISMATCH', path.basename(target));
    }
  }

  const retainedEvidenceFiles = [
    {
      sourcePath: retainedDmg,
      targetPath: 'retained/inner-vexrelay/VexLife-macOS-AppleSilicon.dmg',
      expectedSha256: sha256File(retainedDmg),
    },
    {
      sourcePath: retainedReceipt,
      targetPath: 'retained/inner-vexrelay/VexLife-macOS-AppleSilicon.receipt.json',
      expectedSha256: sha256File(retainedReceipt),
    },
    {
      sourcePath: retainedSha,
      targetPath: 'retained/inner-vexrelay/VexLife-macOS-AppleSilicon.dmg.sha256',
      expectedSha256: sha256File(retainedSha),
    },
  ];

  writeResult('PASS', null, retainedEvidenceFiles);
  console.log('VEXLIFE_PROD_MAC_BUILD_DELIVERY=PASS');
}

try {
  await main();
  process.exitCode = 0;
} catch (error) {
  const code = error?.code || 'MAC_BUILD_DELIVERY_FAILED';
  const value = String(error?.message || '');
  console.error(code + ': ' + (error?.stack || value));
  if (
    code.includes('GRADLE') ||
    code.includes('SWIFT') ||
    code.includes('DMG') ||
    code.includes('SIGNATURE') ||
    value.includes('Could not resolve') ||
    value.includes('timed out')
  ) {
    environmentFindings.push(code);
  } else {
    harnessFindings.push(code);
  }
  afterState.failure = { code, message: value };
  writeResult('SAFE_FAILURE', code, []);
  process.exitCode = 1;
}

// [VXG RealForever]
