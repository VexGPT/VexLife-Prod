import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import crypto from 'node:crypto';
import { spawnSync } from 'node:child_process';

const RESULT_DIR = process.env.VEXLOCALBRIDGE_RESULT_DIR;
const POLICY_ENV = 'VEXLOCALBRIDGE_VEXLIFE_PROD_HOST_EFFECT_POLICY_JSON';
const PRODUCT_HEAD = 'f38779503f9a1171e26593f3542f2477c6e2370a';
const PRODUCT_TREE = '6a5b0b0d4730fb8e0078b42f083677469b6ea63a';
const CARRIER_HEAD = '36eb5c82becd97dcfcbc8fa67455a74d8540503a';
const EXPECTED_DMG_SHA256 = 'cb79560458571f898e65d3b3ce6cd192e025cddfbea007f6e7c3555171199f0e';

const candidateFindings = [];
const harnessFindings = [];
const environmentFindings = [];
const unknownFindings = [];
const completedStepReceipts = [];
const beforeState = {};
const afterState = {};
const effects = {
  sourceMutationPerformed:false,
  branchMutationPerformed:false,
  claimMutationPerformed:false,
  prMetadataMutationPerformed:false,
  reviewPerformed:false,
  approvalPerformed:false,
  readyTransitionPerformed:false,
  mergePerformed:false,
  mainMutationPerformed:false,
  checkpointMutationPerformed:false,
  forcePushPerformed:false,
  historyRewritePerformed:false,
  hostInstallationPerformed:false,
  modelRuntimePerformed:false,
  providerOrNetworkEffectPerformed:false,
  publicationPerformed:false,
  credentialSerializationPerformed:false,
  filesystemOutsideQualifiedRootsPerformed:false,
  productLifecycleMutationPerformed:false,
  quarantineMutationPerformed:false,
  javaExecutionPerformed:false,
};

function fail(code, message = code) {
  const error = new Error(message);
  error.code = code;
  throw error;
}
function readJson(file) {
  return JSON.parse(fs.readFileSync(file, 'utf8'));
}
function readJsonMaybe(file) {
  try {
    if (!fs.existsSync(file) || !fs.lstatSync(file).isFile()) return null;
    return readJson(file);
  } catch (error) {
    return { __readError: String(error?.message || error) };
  }
}
function sha256File(file) {
  const hash = crypto.createHash('sha256');
  const fd = fs.openSync(file, 'r');
  const buffer = Buffer.alloc(1024 * 1024);
  try {
    let bytes = 0;
    while ((bytes = fs.readSync(fd, buffer, 0, buffer.length, null)) > 0) {
      hash.update(buffer.subarray(0, bytes));
    }
  } finally {
    fs.closeSync(fd);
  }
  return hash.digest('hex');
}
function runCommand(executable, args, { timeout = 30000 } = {}) {
  if (!fs.existsSync(executable)) {
    return { available:false, exitCode:null, stdout:'', stderr:'', error:'EXECUTABLE_MISSING' };
  }
  const r = spawnSync(executable, args, {
    encoding:'utf8',
    timeout,
    shell:false,
    maxBuffer:4 * 1024 * 1024,
  });
  return {
    available:true,
    exitCode:Number.isInteger(r.status) ? r.status : 127,
    stdout:String(r.stdout || '').trim(),
    stderr:String(r.stderr || r.error?.message || '').trim(),
    error:r.error?.message || null,
  };
}
function plutilRaw(plist, key) {
  const r = runCommand('/usr/bin/plutil', ['-extract', key, 'raw', '-o', '-', plist]);
  if (r.exitCode !== 0) return null;
  return r.stdout;
}
function inspectQuarantine(target) {
  const exists = fs.existsSync(target);
  const quarantine = exists ? runCommand('/usr/bin/xattr', ['-p','com.apple.quarantine',target]) : null;
  return {
    path:target,
    exists,
    quarantinePresent:quarantine ? quarantine.exitCode === 0 : null,
    quarantineValue:quarantine?.exitCode === 0 ? quarantine.stdout : null,
  };
}

function inspectTrust(target) {
  const exists = fs.existsSync(target);
  let executable = false;
  if (exists) {
    try { fs.accessSync(target, fs.constants.X_OK); executable = true; } catch {}
  }
  const quarantine = exists
    ? runCommand('/usr/bin/xattr', ['-p','com.apple.quarantine',target])
    : null;
  const codesign = exists
    ? runCommand('/usr/bin/codesign', ['-dv','--verbose=4',target])
    : null;
  const spctl = exists
    ? runCommand('/usr/sbin/spctl', ['--assess','--type','execute','--verbose=4',target])
    : null;
  return {
    path:target,
    exists,
    executable,
    quarantinePresent:quarantine ? quarantine.exitCode === 0 : null,
    quarantineValue:quarantine?.exitCode === 0 ? quarantine.stdout : null,
    codesignExitCode:codesign?.exitCode ?? null,
    codesign:[codesign?.stdout,codesign?.stderr].filter(Boolean).join('\n') || null,
    spctlExitCode:spctl?.exitCode ?? null,
    spctl:[spctl?.stdout,spctl?.stderr].filter(Boolean).join('\n') || null,
  };
}
function normalizedAbsolute(value) {
  if (typeof value !== 'string' || value.length === 0 || !path.isAbsolute(value)) return null;
  return path.normalize(value);
}
function isFullHex40(value) {
  return typeof value === 'string' && /^[0-9a-f]{40}$/.test(value);
}
function arrayOfStrings(value) {
  return Array.isArray(value) && value.every((item) => typeof item === 'string');
}
function firstMissingCurrentModule(priorModules, currentModules) {
  const prior = new Set(priorModules);
  return currentModules.find((moduleName) => !prior.has(moduleName)) || null;
}
function evaluateReceipt({ receiptPath, currentManifest, legacyHome = null, kind }) {
  const result = {
    kind,
    receiptPath,
    present:false,
    accepted:false,
    rejection:null,
    sourceSha:null,
    schemaVersion:null,
    homePath:null,
    imagePath:null,
    priorManifest:null,
    selectedJavaPath:null,
  };
  const receipt = readJsonMaybe(receiptPath);
  if (receipt === null) {
    result.rejection = 'RECEIPT_MISSING';
    return result;
  }
  result.present = true;
  if (receipt.__readError) {
    result.rejection = 'RECEIPT_UNREADABLE';
    result.readError = receipt.__readError;
    return result;
  }
  result.schemaVersion = receipt.schemaVersion ?? null;
  result.sourceSha = receipt.sourceSha ?? null;
  if (!isFullHex40(receipt.sourceSha)) {
    result.rejection = 'RECEIPT_SOURCE_INVALID';
    return result;
  }

  let receiptHome = null;
  if (receipt.schemaVersion === 'vexlife-prod.macos-service-lifetime/v2') {
    receiptHome = receipt.homeBinding?.homePath ?? null;
    if (typeof receiptHome !== 'string' || receiptHome.length === 0) {
      result.rejection = 'RECEIPT_HOME_BINDING_MISSING';
      return result;
    }
  } else if (receipt.schemaVersion === 'vexlife-prod.macos-service-lifetime/v1') {
    if (legacyHome === null) {
      result.rejection = 'V1_RECEIPT_NOT_ADMITTED_AT_MACHINE_COORDINATE';
      return result;
    }
    receiptHome = legacyHome;
  } else {
    result.rejection = 'RECEIPT_SCHEMA_UNSUPPORTED';
    return result;
  }

  const home = normalizedAbsolute(receiptHome);
  const receiptImage = normalizedAbsolute(receipt.imagePath);
  result.homePath = home;
  result.imagePath = receiptImage;
  if (!home || !receiptImage) {
    result.rejection = 'RECEIPT_PATH_INVALID';
    return result;
  }
  const expectedImage = normalizedAbsolute(
    path.join(home, 'runtime', 'product-service', 'images', receipt.sourceSha),
  );
  result.expectedImagePath = expectedImage;
  if (!expectedImage || receiptImage !== expectedImage) {
    result.rejection = 'RECEIPT_IMAGE_COORDINATE_MISMATCH';
    return result;
  }

  const priorManifestPath = path.join(receiptImage, 'service-image.json');
  const priorManifest = readJsonMaybe(priorManifestPath);
  if (priorManifest === null || priorManifest?.__readError) {
    result.rejection = priorManifest?.__readError ? 'PRIOR_MANIFEST_UNREADABLE' : 'PRIOR_MANIFEST_MISSING';
    if (priorManifest?.__readError) result.priorManifestReadError = priorManifest.__readError;
    return result;
  }
  result.priorManifest = {
    path:priorManifestPath,
    sourceSha:priorManifest.sourceSha ?? null,
    payloadSha256:priorManifest.payloadSha256 ?? null,
    platform:priorManifest.platform ?? null,
    javaRuntimeMajor:priorManifest.javaRuntimeMajor ?? null,
    runtimeModules:Array.isArray(priorManifest.runtimeModules) ? priorManifest.runtimeModules.map(String) : null,
  };
  if (priorManifest.sourceSha !== receipt.sourceSha) {
    result.rejection = 'PRIOR_MANIFEST_SOURCE_MISMATCH';
    return result;
  }
  if (String(priorManifest.payloadSha256 || '') !== String(receipt.imagePayloadSha256 || '')) {
    result.rejection = 'PRIOR_MANIFEST_PAYLOAD_MISMATCH';
    return result;
  }
  if (priorManifest.platform !== 'darwin-arm64') {
    result.rejection = 'PRIOR_MANIFEST_PLATFORM_MISMATCH';
    return result;
  }
  if (String(priorManifest.javaRuntimeMajor ?? '') !== String(currentManifest.javaRuntimeMajor ?? '')) {
    result.rejection = 'JAVA_RUNTIME_MAJOR_MISMATCH';
    return result;
  }
  if (!arrayOfStrings(priorManifest.runtimeModules) || !arrayOfStrings(currentManifest.runtimeModules)) {
    result.rejection = 'RUNTIME_MODULE_SET_MISSING';
    return result;
  }
  const missingModule = firstMissingCurrentModule(priorManifest.runtimeModules, currentManifest.runtimeModules);
  result.missingCurrentModule = missingModule;
  if (missingModule !== null) {
    result.rejection = 'CURRENT_RUNTIME_MODULE_NOT_SUBSET';
    return result;
  }

  const javaPath = path.join(receiptImage, 'runtime', 'bin', 'java');
  let executable = false;
  try { fs.accessSync(javaPath, fs.constants.X_OK); executable = true; } catch {}
  result.javaExecutable = executable;
  result.selectedJavaPath = javaPath;
  if (!executable) {
    result.rejection = 'PRIOR_JAVA_NOT_EXECUTABLE';
    return result;
  }

  result.accepted = true;
  result.rejection = null;
  return result;
}
function writeResult(disposition, stopReasonOrNull, firstFailure = null) {
  if (!RESULT_DIR) throw new Error('VEXLOCALBRIDGE_RESULT_DIR_MISSING');
  fs.mkdirSync(RESULT_DIR, { recursive:true });
  fs.writeFileSync(path.join(RESULT_DIR,'payload-result.json'), JSON.stringify({
    schemaVersion:'vextreme.vexlocalbridge-mac-payload-result/v1',
    disposition,
    stopReasonOrNull,
    firstSubstantiveFailureOrNull:firstFailure,
    acceptedExitCodes:[0],
    candidateFindings,
    harnessFindings,
    environmentFindings,
    unknownFindings,
    effects,
    beforeState,
    afterState,
    completedStepReceipts,
    retainedEvidenceFiles:[],
  }, null, 2) + '\n');
}

let mountRoot = null;
let mounted = false;
let disposition = 'PASS';
let stopReason = null;
let firstFailure = null;

try {
  if (process.platform !== 'darwin') fail('MACOS_REQUIRED');
  if (!['arm64','aarch64'].includes(process.arch)) fail('APPLE_SILICON_REQUIRED');

  const context = readJson(path.join(process.cwd(),'inputs','context.json'));
  const policy = JSON.parse(process.env[POLICY_ENV] || 'null');
  if (!policy) fail('POLICY_MISSING');
  if (context?.candidate?.head !== PRODUCT_HEAD
      || context?.candidate?.tree !== PRODUCT_TREE
      || context?.carrier?.candidateHead !== CARRIER_HEAD
      || context?.a003Artifact?.expectedSha256 !== EXPECTED_DMG_SHA256) {
    fail('IMMUTABLE_CONTEXT_MISMATCH');
  }
  if (policy.repository !== 'VexGPT/VexLife-Prod' || policy.hostProfileRef !== 'macos-victor') {
    fail('POLICY_BINDING_INVALID');
  }
  if (policy.applicationLivedProofAuthorized !== false || policy.buildDependencyFetchAuthorized !== false) {
    fail('READ_ONLY_EFFECT_SCOPE_WIDENED');
  }

  const userHome = os.homedir();
  const defaultHome = path.join(userHome,'.vexlife');
  const appSupport = path.join(userHome,'Library','Application Support','VexLife');
  const machineReceiptPath = path.join(appSupport,'ProductService','service-lifetime.json');
  const legacyReceiptPath = path.join(defaultHome,'runtime','product-service','service-lifetime.json');
  const primaryDmgPath = path.join(userHome,'Downloads',context.a003Artifact.humanDmgName);
  const alternateDmgPath = path.join(
    userHome,
    'Downloads',
    context.a003Artifact.deliveryFolder,
    context.a003Artifact.deliveryDmgName,
  );

  beforeState.paths = {
    defaultHome,
    appSupport,
    machineReceiptPath,
    legacyReceiptPath,
    primaryDmgPath,
    alternateDmgPath,
  };
  beforeState.readOnlyGuards = {
    javaExecutionAuthorized:false,
    whisperExecutionAuthorized:false,
    quarantineMutationAuthorized:false,
    productLifecycleMutationAuthorized:false,
    applicationLaunchAuthorized:false,
  };

  const machineReceipt = readJsonMaybe(machineReceiptPath);
  const legacyReceipt = readJsonMaybe(legacyReceiptPath);
  afterState.receipts = {
    machine:{path:machineReceiptPath,present:machineReceipt !== null,value:machineReceipt},
    legacy:{path:legacyReceiptPath,present:legacyReceipt !== null,value:legacyReceipt},
  };
  completedStepReceipts.push({step:'service-lifetime-receipts',state:'PASS'});

  const activeReceipt =
    machineReceipt && !machineReceipt.__readError
      ? machineReceipt
      : legacyReceipt && !legacyReceipt.__readError
        ? legacyReceipt
        : null;
  const activeBoundHome = activeReceipt?.homeBinding?.homePath
    ? normalizedAbsolute(String(activeReceipt.homeBinding.homePath))
    : defaultHome;
  afterState.activeLifetime = {
    sourceSha:activeReceipt?.sourceSha ?? null,
    imagePath:activeReceipt?.imagePath ?? null,
    imagePayloadSha256:activeReceipt?.imagePayloadSha256 ?? null,
    schemaVersion:activeReceipt?.schemaVersion ?? null,
    boundHome:activeBoundHome,
    defaultHome,
    matchesA002SourceWitness:activeReceipt?.sourceSha === context.priorA002Witness.sourceSha,
  };

  if (activeBoundHome && activeBoundHome !== defaultHome) {
    fail('HELD_CUSTOM_HOME_NOT_QUALIFIED_FOR_A004');
  }

  const dmgCandidates = [primaryDmgPath, alternateDmgPath];
  let dmgPath = null;
  for (const candidate of dmgCandidates) {
    if (!fs.existsSync(candidate)) continue;
    const stat = fs.lstatSync(candidate);
    if (!stat.isFile() || stat.isSymbolicLink()) continue;
    const observed = sha256File(candidate);
    if (observed === EXPECTED_DMG_SHA256) {
      dmgPath = candidate;
      break;
    }
  }
  if (!dmgPath) fail('A003_EXACT_DMG_NOT_FOUND');

  const dmgTrust = inspectQuarantine(dmgPath);
  afterState.a003Dmg = {
    path:dmgPath,
    sha256:sha256File(dmgPath),
    expectedSha256:EXPECTED_DMG_SHA256,
    exactDigest:true,
    quarantinePresent:dmgTrust.quarantinePresent,
    quarantineValue:dmgTrust.quarantineValue,
  };
  completedStepReceipts.push({step:'exact-a003-dmg-identity',state:'PASS'});

  mountRoot = fs.mkdtempSync(path.join(os.tmpdir(),'vexlife-a004-dmg-'));
  const attach = runCommand(
    '/usr/bin/hdiutil',
    ['attach','-readonly','-nobrowse','-noautoopen','-mountpoint',mountRoot,dmgPath],
    { timeout:120000 },
  );
  afterState.dmgAttach = {
    exitCode:attach.exitCode,
    stdout:attach.stdout,
    stderr:attach.stderr,
    mountRoot,
    readOnly:true,
  };
  if (attach.exitCode !== 0) fail('A003_DMG_READONLY_ATTACH_FAILED');
  mounted = true;
  completedStepReceipts.push({step:'a003-dmg-readonly-attach',state:'PASS'});

  const appRoot = path.join(mountRoot,'VexLife.app');
  const infoPlist = path.join(appRoot,'Contents','Info.plist');
  if (!fs.existsSync(infoPlist)) fail('A003_DMG_VEXLIFE_APP_MISSING');

  const expectedSource = plutilRaw(infoPlist,'VexLifeProdExpectedSourceSha');
  const launcherRelative = plutilRaw(infoPlist,'VexLifeProdEmbeddedSetupLauncherRelativePath');
  if (expectedSource !== PRODUCT_HEAD) fail('A003_INFO_PLIST_SOURCE_MISMATCH');
  if (!launcherRelative || path.isAbsolute(launcherRelative) || launcherRelative.includes('\0')) {
    fail('A003_SETUP_LAUNCHER_RELATIVE_PATH_INVALID');
  }

  const resourcesRoot = path.join(appRoot,'Contents','Resources');
  const setupLauncher = path.resolve(resourcesRoot, launcherRelative);
  if (setupLauncher !== resourcesRoot && !setupLauncher.startsWith(resourcesRoot + path.sep)) {
    fail('A003_SETUP_LAUNCHER_ESCAPES_RESOURCES');
  }
  if (!fs.existsSync(setupLauncher) || !fs.lstatSync(setupLauncher).isFile()) {
    fail('A003_SETUP_LAUNCHER_MISSING');
  }
  const currentImageRoot = path.dirname(path.dirname(setupLauncher));
  const currentManifestPath = path.join(currentImageRoot,'service-image.json');
  const currentManifest = readJsonMaybe(currentManifestPath);
  if (!currentManifest || currentManifest.__readError) fail('A003_CURRENT_SERVICE_IMAGE_MANIFEST_UNREADABLE');
  if (currentManifest.sourceSha !== PRODUCT_HEAD) fail('A003_CURRENT_MANIFEST_SOURCE_MISMATCH');
  if (currentManifest.platform !== 'darwin-arm64') fail('A003_CURRENT_MANIFEST_PLATFORM_MISMATCH');
  if (String(currentManifest.javaRuntimeMajor ?? '') !== '21') fail('A003_CURRENT_MANIFEST_JAVA_MAJOR_MISMATCH');
  if (!arrayOfStrings(currentManifest.runtimeModules)) fail('A003_CURRENT_MANIFEST_RUNTIME_MODULES_MISSING');

  let setupLauncherExecutable = false;
  try { fs.accessSync(setupLauncher, fs.constants.X_OK); setupLauncherExecutable = true; } catch {}
  const launcherPreview = fs.readFileSync(setupLauncher,'utf8').slice(0,8192);
  afterState.currentA003Image = {
    infoPlist,
    expectedSource,
    launcherRelative,
    setupLauncher,
    setupLauncherExecutable,
    currentImageRoot,
    currentManifestPath,
    manifest:{
      schemaVersion:currentManifest.schemaVersion ?? null,
      sourceSha:currentManifest.sourceSha,
      payloadSha256:currentManifest.payloadSha256 ?? null,
      platform:currentManifest.platform,
      javaRuntimeMajor:currentManifest.javaRuntimeMajor,
      runtimeModules:currentManifest.runtimeModules,
    },
    launcherReferencesBundledRuntimeJava:
      /runtime\/bin\/java/.test(launcherPreview) || /runtime["']?,?\s*["']bin["']?,?\s*["']java/.test(launcherPreview),
  };
  completedStepReceipts.push({step:'a003-current-service-image-manifest',state:'PASS'});

  const machine = evaluateReceipt({
    receiptPath:machineReceiptPath,
    currentManifest,
    legacyHome:null,
    kind:'MACHINE_V2_FIRST',
  });
  const legacy = machine.accepted
    ? {kind:'LEGACY_NOT_EVALUATED_AFTER_MACHINE_ACCEPT',accepted:false,rejection:'NOT_EVALUATED'}
    : evaluateReceipt({
        receiptPath:legacyReceiptPath,
        currentManifest,
        legacyHome:defaultHome,
        kind:'LEGACY_FALLBACK',
      });

  let selectorDisposition;
  let selectedJavaPath;
  let selectedJavaSourceSha;
  let selectedLaunchExecutable;
  if (machine.accepted) {
    selectorDisposition = 'REUSE_MACHINE_RECEIPT_JAVA';
    selectedJavaPath = machine.selectedJavaPath;
    selectedJavaSourceSha = machine.sourceSha;
    selectedLaunchExecutable = machine.selectedJavaPath;
  } else if (legacy.accepted) {
    selectorDisposition = 'REUSE_LEGACY_RECEIPT_JAVA';
    selectedJavaPath = legacy.selectedJavaPath;
    selectedJavaSourceSha = legacy.sourceSha;
    selectedLaunchExecutable = legacy.selectedJavaPath;
  } else {
    selectorDisposition = 'FALLBACK_TO_A003_BUNDLED_SETUP_LAUNCHER';
    selectedJavaPath = path.join(currentImageRoot,'runtime','bin','java');
    selectedJavaSourceSha = PRODUCT_HEAD;
    selectedLaunchExecutable = setupLauncher;
  }

  const selectedJavaTrust = inspectTrust(selectedJavaPath);
  afterState.selector = {
    selectorDisposition,
    selectedLaunchExecutable,
    selectedJavaPath,
    selectedJavaSourceSha,
    machineReceiptEvaluation:machine,
    legacyReceiptEvaluation:legacy,
    currentRuntimeModules:currentManifest.runtimeModules,
    a002RuntimeModules:context.priorA002Witness.runtimeModules,
    currentModulesEqualA002Witness:
      JSON.stringify([...currentManifest.runtimeModules].sort())
      === JSON.stringify([...context.priorA002Witness.runtimeModules].sort()),
    selectedJavaTrust,
  };

  if (selectorDisposition === 'REUSE_MACHINE_RECEIPT_JAVA') {
    candidateFindings.push('A003_SELECTOR_REUSES_MACHINE_RECEIPT_JAVA');
  } else if (selectorDisposition === 'REUSE_LEGACY_RECEIPT_JAVA') {
    candidateFindings.push('A003_SELECTOR_REUSES_LEGACY_RECEIPT_JAVA');
  } else {
    candidateFindings.push('A003_SELECTOR_FALLS_BACK_TO_BUNDLED_JAVA');
  }
  if (selectedJavaTrust.spctlExitCode !== null && selectedJavaTrust.spctlExitCode !== 0) {
    environmentFindings.push('SELECTED_JAVA_SPCTL_ASSESSMENT_REJECTED');
  }
  if (selectedJavaTrust.quarantinePresent === true) {
    environmentFindings.push('SELECTED_JAVA_QUARANTINE_PRESENT');
  }

  completedStepReceipts.push({step:'selector-predicates-recomputed',state:'PASS'});
  completedStepReceipts.push({step:'selected-java-trust-observation',state:'PASS'});
} catch (error) {
  disposition = 'SAFE_FAILURE';
  stopReason = error?.code || 'JAVA_SELECTION_INSPECTION_FAILED';
  firstFailure = {
    failureFamily:'HARNESS_OR_ENVIRONMENT',
    stage:'A004_READ_ONLY_JAVA_SELECTION_INSPECTION',
    summary:String(error?.message || error),
    evidenceRef:'payload-result.json',
  };
  harnessFindings.push(stopReason);
  afterState.failure = {
    code:stopReason,
    message:String(error?.message || error),
  };
} finally {
  if (mounted && mountRoot) {
    let detach = runCommand('/usr/bin/hdiutil',['detach',mountRoot],{timeout:30000});
    if (detach.exitCode !== 0) {
      const forced = runCommand('/usr/bin/hdiutil',['detach','-force',mountRoot],{timeout:30000});
      afterState.dmgDetach = {
        ordinary:{exitCode:detach.exitCode,stdout:detach.stdout,stderr:detach.stderr},
        forced:{exitCode:forced.exitCode,stdout:forced.stdout,stderr:forced.stderr},
      };
      if (forced.exitCode !== 0) {
        disposition = 'SAFE_FAILURE';
        stopReason = 'A003_DMG_DETACH_FAILED';
        harnessFindings.push(stopReason);
        firstFailure = firstFailure || {
          failureFamily:'HARNESS_OR_ENVIRONMENT',
          stage:'A004_DMG_CLEANUP',
          summary:'Read-only A003 DMG mount could not be detached cleanly.',
          evidenceRef:'payload-result.json',
        };
      }
    } else {
      afterState.dmgDetach = { ordinary:{exitCode:0,stdout:detach.stdout,stderr:detach.stderr} };
    }
  }
  if (mountRoot) {
    try { fs.rmSync(mountRoot,{recursive:true,force:true}); } catch {}
  }
}

writeResult(disposition, stopReason, firstFailure);
process.exitCode = disposition === 'PASS' ? 0 : 1;

// [VXG RealForever]
