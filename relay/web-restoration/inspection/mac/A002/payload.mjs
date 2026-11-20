import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { spawnSync } from 'node:child_process';

const RESULT_DIR = process.env.VEXLOCALBRIDGE_RESULT_DIR;
const POLICY_ENV = 'VEXLOCALBRIDGE_VEXLIFE_PROD_HOST_EFFECT_POLICY_JSON';
const PRODUCT_HEAD = 'd48f1c79ba4e70056cfc720199fca5f56660b583';
const PRODUCT_TREE = 'd8dbdd7c94f9e747b68e5fb61c8b2ab73fb34218';
const CARRIER_HEAD = '36eb5c82becd97dcfcbc8fa67455a74d8540503a';

const candidateFindings = [];
const harnessFindings = [];
const environmentFindings = [];
const unknownFindings = [];
const completedStepReceipts = [];
const beforeState = {};
const afterState = {};
const effects = {
  sourceMutationPerformed:false, branchMutationPerformed:false, claimMutationPerformed:false,
  prMetadataMutationPerformed:false, reviewPerformed:false, approvalPerformed:false,
  readyTransitionPerformed:false, mergePerformed:false, mainMutationPerformed:false,
  checkpointMutationPerformed:false, forcePushPerformed:false, historyRewritePerformed:false,
  hostInstallationPerformed:false, modelRuntimePerformed:false, providerOrNetworkEffectPerformed:false,
  publicationPerformed:false, credentialSerializationPerformed:false,
  filesystemOutsideQualifiedRootsPerformed:false, productLifecycleMutationPerformed:false,
  quarantineMutationPerformed:false, javaExecutionPerformed:false,
};

function readJson(file) { return JSON.parse(fs.readFileSync(file, 'utf8')); }
function readJsonMaybe(file) {
  try {
    if (!fs.existsSync(file) || !fs.lstatSync(file).isFile()) return null;
    return readJson(file);
  } catch (error) {
    return { __readError: String(error?.message || error) };
  }
}
function readOnlyCommand(executable, args) {
  if (!fs.existsSync(executable)) return { available:false, exitCode:null, stdout:'', stderr:'' };
  const r = spawnSync(executable, args, { encoding:'utf8', timeout:15000, shell:false, maxBuffer:4*1024*1024 });
  return {
    available:true,
    exitCode:Number.isInteger(r.status) ? r.status : 127,
    stdout:String(r.stdout || '').trim(),
    stderr:String(r.stderr || r.error?.message || '').trim(),
  };
}
function inspectJava(javaPath) {
  let executable = false;
  const exists = fs.existsSync(javaPath);
  if (exists) {
    try { fs.accessSync(javaPath, fs.constants.X_OK); executable = true; } catch {}
  }
  const quarantine = exists ? readOnlyCommand('/usr/bin/xattr', ['-p','com.apple.quarantine',javaPath]) : null;
  const codesign = exists ? readOnlyCommand('/usr/bin/codesign', ['-dv','--verbose=4',javaPath]) : null;
  return {
    path:javaPath,
    exists,
    executable,
    quarantinePresent:quarantine ? quarantine.exitCode === 0 : null,
    quarantineValue:quarantine?.exitCode === 0 ? quarantine.stdout : null,
    codesignExitCode:codesign?.exitCode ?? null,
    codesign:[codesign?.stdout,codesign?.stderr].filter(Boolean).join('\n') || null,
  };
}
function writeResult(disposition, stopReasonOrNull) {
  fs.mkdirSync(RESULT_DIR, { recursive:true });
  fs.writeFileSync(path.join(RESULT_DIR,'payload-result.json'), JSON.stringify({
    schemaVersion:'vextreme.vexlocalbridge-mac-payload-result/v1',
    disposition,
    stopReasonOrNull,
    firstSubstantiveFailureOrNull:null,
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

try {
  if (process.platform !== 'darwin') throw Object.assign(new Error('MACOS_REQUIRED'), {code:'MACOS_REQUIRED'});
  if (!['arm64','aarch64'].includes(process.arch)) throw Object.assign(new Error('APPLE_SILICON_REQUIRED'), {code:'APPLE_SILICON_REQUIRED'});

  const context = readJson(path.join(process.cwd(),'inputs','context.json'));
  const policy = JSON.parse(process.env[POLICY_ENV] || 'null');
  if (!policy) throw Object.assign(new Error('POLICY_MISSING'), {code:'POLICY_MISSING'});
  if (context?.candidate?.head !== PRODUCT_HEAD || context?.candidate?.tree !== PRODUCT_TREE || context?.carrier?.candidateHead !== CARRIER_HEAD) {
    throw Object.assign(new Error('IMMUTABLE_CONTEXT_MISMATCH'), {code:'IMMUTABLE_CONTEXT_MISMATCH'});
  }
  if (policy.repository !== 'VexGPT/VexLife-Prod' || policy.hostProfileRef !== 'macos-victor') {
    throw Object.assign(new Error('POLICY_BINDING_INVALID'), {code:'POLICY_BINDING_INVALID'});
  }
  if (policy.applicationLivedProofAuthorized !== false || policy.buildDependencyFetchAuthorized !== false) {
    throw Object.assign(new Error('READ_ONLY_EFFECT_SCOPE_WIDENED'), {code:'READ_ONLY_EFFECT_SCOPE_WIDENED'});
  }

  const userHome = os.homedir();
  const defaultHome = path.join(userHome,'.vexlife');
  const appSupport = path.join(userHome,'Library','Application Support','VexLife');
  const machineReceiptPath = path.join(appSupport,'ProductService','service-lifetime.json');
  const legacyReceiptPath = path.join(defaultHome,'runtime','product-service','service-lifetime.json');
  beforeState.paths = { defaultHome, appSupport, machineReceiptPath, legacyReceiptPath };

  const machine = readJsonMaybe(machineReceiptPath);
  const legacy = readJsonMaybe(legacyReceiptPath);
  afterState.receipts = {
    machine:{path:machineReceiptPath,present:machine!==null,value:machine},
    legacy:{path:legacyReceiptPath,present:legacy!==null,value:legacy},
  };
  completedStepReceipts.push({step:'service-lifetime-receipts',state:'PASS'});

  let active = machine && !machine.__readError ? machine : (legacy && !legacy.__readError ? legacy : null);
  let boundHome = defaultHome;
  if (active?.homeBinding?.homePath) boundHome = path.resolve(String(active.homeBinding.homePath));
  afterState.activeLifetime = {
    sourceSha:active?.sourceSha ?? null,
    imagePath:active?.imagePath ?? null,
    imagePayloadSha256:active?.imagePayloadSha256 ?? null,
    schemaVersion:active?.schemaVersion ?? null,
    boundHome,
    defaultHome,
  };

  if (boundHome !== defaultHome) {
    candidateFindings.push('HELD_CUSTOM_HOME_NOT_QUALIFIED_FOR_A002');
    completedStepReceipts.push({step:'default-home-boundary',state:'HELD_CUSTOM_HOME'});
    writeResult('PASS',null);
    process.exitCode=0;
  } else {
    completedStepReceipts.push({step:'default-home-boundary',state:'PASS'});

    const imageBase = path.join(defaultHome,'runtime','product-service','images');
    const images = [];
    if (fs.existsSync(imageBase) && fs.lstatSync(imageBase).isDirectory()) {
      for (const name of fs.readdirSync(imageBase).sort()) {
        if (!/^[0-9a-f]{40}$/.test(name)) continue;
        const imageRoot = path.join(imageBase,name);
        if (!fs.lstatSync(imageRoot).isDirectory()) continue;
        const manifest = readJsonMaybe(path.join(imageRoot,'service-image.json'));
        const runtimeModules = Array.isArray(manifest?.runtimeModules) ? manifest.runtimeModules.map(String).sort() : [];
        const receiptMatches =
          Boolean(active) &&
          String(active.sourceSha || '') === name &&
          path.resolve(String(active.imagePath || '')) === imageRoot;
        const payloadMatches =
          receiptMatches &&
          typeof active.imagePayloadSha256 === 'string' &&
          manifest?.payloadSha256 === active.imagePayloadSha256;
        images.push({
          sourceSha:name,
          imageRoot,
          schemaVersion:manifest?.schemaVersion ?? null,
          platform:manifest?.platform ?? null,
          javaRuntimeMajor:manifest?.javaRuntimeMajor ?? null,
          runtimeModules,
          hasJavaDesktop:runtimeModules.includes('java.desktop'),
          payloadSha256:manifest?.payloadSha256 ?? null,
          receiptMatches,
          payloadMatches,
          java:inspectJava(path.join(imageRoot,'runtime','bin','java')),
        });
      }
    }
    afterState.installedImages = { imageBase, count:images.length, images };

    const activeImage = images.find(x => x.receiptMatches) || null;
    afterState.a001ReuseSelectorEvidence = {
      currentCandidateSource:PRODUCT_HEAD,
      currentCandidateKnownRequiresJavaDesktop:context.knownCompatibilityWitness.currentCandidateRequiresJavaDesktop === true,
      activeImageFound:activeImage !== null,
      activeSourceSha:activeImage?.sourceSha ?? null,
      activePlatform:activeImage?.platform ?? null,
      activeJavaRuntimeMajor:activeImage?.javaRuntimeMajor ?? null,
      activeRuntimeModules:activeImage?.runtimeModules ?? [],
      activeHasJavaDesktop:activeImage?.hasJavaDesktop ?? null,
      activeReceiptPayloadMatches:activeImage?.payloadMatches ?? null,
      activeJavaExecutable:activeImage?.java?.executable ?? null,
      activeJavaQuarantinePresent:activeImage?.java?.quarantinePresent ?? null,
      activeJavaQuarantineValue:activeImage?.java?.quarantineValue ?? null,
      activeJavaCodesign:activeImage?.java?.codesign ?? null,
      retiredImageCount:Math.max(0,images.length-(activeImage?1:0)),
    };

    if (!activeImage) candidateFindings.push('A001_NO_ACTIVE_INSTALLED_IMAGE_MATCHED_LIFETIME_RECEIPT');
    if (activeImage && !activeImage.payloadMatches) candidateFindings.push('A001_ACTIVE_IMAGE_PAYLOAD_RECEIPT_MISMATCH');
    if (activeImage && activeImage.platform !== 'darwin-arm64') candidateFindings.push('A001_ACTIVE_IMAGE_PLATFORM_MISMATCH');
    if (activeImage && Number(activeImage.javaRuntimeMajor) !== 21) candidateFindings.push('A001_ACTIVE_IMAGE_JAVA_MAJOR_MISMATCH');
    if (activeImage && !activeImage.java.executable) candidateFindings.push('A001_ACTIVE_IMAGE_JAVA_NOT_EXECUTABLE');
    if (activeImage && context.knownCompatibilityWitness.currentCandidateRequiresJavaDesktop && !activeImage.hasJavaDesktop) {
      candidateFindings.push('A001_ACTIVE_IMAGE_MISSING_JAVA_DESKTOP');
    }
    if (images.length <= 1) candidateFindings.push('RETIRED_COMPATIBLE_IMAGES_NOT_AVAILABLE_FOR_REUSE');

    completedStepReceipts.push({step:'installed-runtime-observation',state:'PASS'});
    completedStepReceipts.push({step:'java-signature-quarantine-observation',state:'PASS'});
    writeResult('PASS',null);
    process.exitCode=0;
  }
} catch (error) {
  const code = error?.code || 'JAVA_INSPECTION_FAILED';
  harnessFindings.push(code);
  afterState.failure = {code,message:String(error?.message || error)};
  writeResult('SAFE_FAILURE',code);
  process.exitCode=1;
}

// [VXG RealForever]
