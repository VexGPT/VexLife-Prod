'use strict';

const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const cp = require('node:child_process');
const http = require('node:http');

const RESULT_VERSION = 'vextreme.vexlocalbridge-mac-payload-result/v1';
const EFFECT_FIELDS = [
  'sourceMutationPerformed','branchMutationPerformed','claimMutationPerformed','prMetadataMutationPerformed',
  'reviewPerformed','approvalPerformed','readyTransitionPerformed','mergePerformed','mainMutationPerformed',
  'checkpointMutationPerformed','forcePushPerformed','historyRewritePerformed','hostInstallationPerformed',
  'modelRuntimePerformed','providerOrNetworkEffectPerformed','publicationPerformed',
  'credentialSerializationPerformed','filesystemOutsideQualifiedRootsPerformed',
];

const PACKAGE_ROOT = path.resolve(__dirname, '..');
const ATTEMPT = JSON.parse(fs.readFileSync(path.join(PACKAGE_ROOT, 'inputs', 'CURRENT-ATTEMPT.json'), 'utf8'));
const BINDING = JSON.parse(fs.readFileSync(path.join(PACKAGE_ROOT, 'inputs', 'experiment-binding.json'), 'utf8'));
const RESULT_DIR = process.env.VEXLOCALBRIDGE_RESULT_DIR;
const CAPSULE = process.env.VEXLOCALBRIDGE_CAPSULE_ROOT;
if (!RESULT_DIR || !CAPSULE) throw new Error('VexLocalBridge execution environment is incomplete');

const BRIDGE = path.join(CAPSULE, 'bridge');
const STABLE = path.join(BRIDGE, 'retained-vexrelay', 'memory-depth-provisional-recontact-companion', 'a003-runtime-lineage');
fs.rmSync(STABLE, { recursive:true, force:true });
fs.mkdirSync(STABLE, { recursive:true });
fs.mkdirSync(RESULT_DIR, { recursive:true });

function sha256File(file) {
  return crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex');
}
function writeJson(name, value) {
  const file = path.join(STABLE, name);
  fs.writeFileSync(file, JSON.stringify(value, null, 2) + '\n');
  return file;
}
function readJsonIfExists(file) {
  if (!fs.existsSync(file)) return { state:'ABSENT', path:file, value:null };
  try { return { state:'PRESENT', path:file, value:JSON.parse(fs.readFileSync(file,'utf8')) }; }
  catch (error) { return { state:'INVALID_JSON', path:file, value:null, error:error.message }; }
}
function run(argv) {
  const r = cp.spawnSync(argv[0], argv.slice(1), { encoding:'utf8', shell:false, timeout:120000, maxBuffer:16*1024*1024 });
  return { argv, exitCode:r.status ?? -1, stdout:r.stdout || '', stderr:r.stderr || '', accepted:r.status === 0 };
}
function psForReceipt(observed) {
  const pid = observed?.value?.runtime?.pid;
  if (!Number.isInteger(pid)) return { state:'NO_PID' };
  const r = run(['/bin/ps','-p',String(pid),'-o','pid=,pgid=,command=']);
  return { state:r.exitCode === 0 && r.stdout.trim() ? 'LIVE' : 'NOT_LIVE', pid, receiptProcessGroupId:observed.value.runtime.processGroupId ?? null, ps:r };
}
function getJson(url, timeoutMs=3000) {
  return new Promise(resolve => {
    const req = http.get(url, { timeout:timeoutMs }, res => {
      let body='';
      res.setEncoding('utf8');
      res.on('data', d => body += d);
      res.on('end', () => {
        let json=null, parseError=null;
        try { json=JSON.parse(body); } catch (error) { parseError=error.message; }
        resolve({ state:'RESPONDED', url, statusCode:res.statusCode, body, json, parseError });
      });
    });
    req.on('timeout', () => { req.destroy(); resolve({ state:'TIMEOUT', url }); });
    req.on('error', error => resolve({ state:'UNAVAILABLE', url, error:error.message }));
  });
}
function effects() {
  const value=Object.fromEntries(EFFECT_FIELDS.map(k=>[k,false]));
  value.providerOrNetworkEffectPerformed=true;
  return value;
}
function retainedFiles(files) {
  return files.filter(Boolean).filter(fs.existsSync).map(sourcePath => ({
    sourcePath,
    targetPath:'retained/inner-vexrelay/memory-depth-provisional-recontact-companion/a003-runtime-lineage/' + path.basename(sourcePath),
    expectedSha256:sha256File(sourcePath),
  }));
}

(async()=>{
  const evidence=[];
  const ghCandidates=['/opt/homebrew/bin/gh','/usr/local/bin/gh'];
  const gh=ghCandidates.find(p=>fs.existsSync(p));
  if (!gh) throw Object.assign(new Error('GitHub CLI unavailable'), { code:'GITHUB_AUTH_UNAVAILABLE' });

  const auth=run([gh,'auth','status','--hostname','github.com']);
  evidence.push({ step:'github-auth', ...auth });
  if (!auth.accepted) throw Object.assign(new Error('GitHub authentication unavailable'), { code:'GITHUB_AUTH_UNAVAILABLE' });

  const main=run([gh,'api','repos/VexGPT/VexLife-Prod/branches/main','--jq','.commit.sha']);
  evidence.push({ step:'product-main', ...main });
  if (!main.accepted || !/^[0-9a-f]{40}$/.test(main.stdout.trim())) {
    throw Object.assign(new Error('Product main currentness unavailable'), { code:'PRODUCT_MAIN_CURRENTNESS_UNAVAILABLE' });
  }
  const observedMainHead=main.stdout.trim();

  const serviceReceipt=readJsonIfExists(ATTEMPT.serviceReceipt);
  const productRuntimeReceipt=readJsonIfExists(ATTEMPT.runtimeReceipts[0]);
  const stableRuntimeReceipt=readJsonIfExists(ATTEMPT.runtimeReceipts[1]);
  const modelBinding=readJsonIfExists(ATTEMPT.modelBinding);

  const serviceCurrentness=await getJson(ATTEMPT.serviceSourceCurrentness);
  const serviceCurrentState=await getJson('http://127.0.0.1:18210/api/v1/current-state');
  const runtimeHealth=await getJson(ATTEMPT.runtimeHealth);

  const productRuntimeProcess=psForReceipt(productRuntimeReceipt);
  const stableRuntimeProcess=psForReceipt(stableRuntimeReceipt);

  const serviceSourceSha =
    serviceCurrentness?.json?.sourceSha ||
    serviceReceipt?.value?.sourceSha ||
    null;
  const boundRuntimePath =
    modelBinding?.value?.runtimeReceiptPath ||
    null;
  const boundReceiptClass =
    boundRuntimePath === 'runtime/vexlife-prod/heretic-persistent-runtime-owner-receipt.json' ? 'PRODUCT' :
    boundRuntimePath === 'runtime/heretic/persistent-runtime-owner-receipt.json' ? 'STABLE' :
    boundRuntimePath ? 'OTHER' : 'UNBOUND_OR_UNKNOWN';

  const runtimeReceipt =
    boundReceiptClass === 'PRODUCT' ? productRuntimeReceipt :
    boundReceiptClass === 'STABLE' ? stableRuntimeReceipt :
    productRuntimeReceipt.state === 'PRESENT' ? productRuntimeReceipt : stableRuntimeReceipt;
  const runtimeProcess =
    runtimeReceipt === stableRuntimeReceipt ? stableRuntimeProcess : productRuntimeProcess;

  let classification='AMBIGUOUS';
  let nextRoute='HOLD_AND_REFINE_DIAGNOSIS';
  if (serviceSourceSha && serviceSourceSha !== BINDING.experiment.head && runtimeReceipt?.state === 'PRESENT' && runtimeProcess?.state === 'LIVE') {
    classification='HOST_LINEAGE_NEWER_OR_DIFFERENT_FROM_EXPERIMENT';
    nextRoute='CURRENTIZE_EXPERIMENT_SOURCE_DO_NOT_ROLL_RUNTIME_BACK';
  } else if (serviceSourceSha === BINDING.experiment.head && runtimeReceipt?.state === 'PRESENT' && runtimeProcess?.state !== 'LIVE') {
    classification='EXPERIMENT_SOURCE_WITH_NONLIVE_BOUND_RUNTIME';
    nextRoute='SEPARATE_RUNTIME_LIFECYCLE_DIAGNOSIS';
  } else if (!serviceSourceSha && runtimeReceipt?.state !== 'PRESENT') {
    classification='SERVICE_AND_RUNTIME_LINEAGE_ABSENT_OR_UNOBSERVED';
    nextRoute='SEPARATE_RUNTIME_LIFECYCLE_DIAGNOSIS';
  }

  const summary={
    schemaVersion:'vexlife-prod.memory-depth.runtime-lineage-diagnosis/v1',
    observedAt:new Date().toISOString(),
    companionPullRequest:472,
    experimentHead:BINDING.experiment.head,
    experimentTree:BINDING.experiment.tree,
    observedMainHead,
    companionObservedMainHead:ATTEMPT.observedProductMainHead,
    mainMovedSinceCompanionBinding:observedMainHead !== ATTEMPT.observedProductMainHead,
    serviceSourceSha,
    serviceSourceMatchesExperiment:serviceSourceSha === BINDING.experiment.head,
    serviceSourceMatchesObservedMain:serviceSourceSha === observedMainHead,
    boundRuntimePath,
    boundReceiptClass,
    runtimeReceiptState:runtimeReceipt?.state || 'ABSENT',
    runtimeImplementationIdentity:runtimeReceipt?.value?.implementationIdentity || null,
    runtimeModelRef:runtimeReceipt?.value?.modelRef || null,
    runtimeModelProfileRef:runtimeReceipt?.value?.modelProfileRef || null,
    runtimeProcess,
    serviceCurrentness,
    serviceCurrentState,
    runtimeHealth,
    classification,
    nextRoute,
    effectsPerformed:false,
  };

  const files=[
    writeJson('diagnosis-summary.json', summary),
    writeJson('service-receipt.json', serviceReceipt),
    writeJson('product-runtime-receipt.json', productRuntimeReceipt),
    writeJson('stable-runtime-receipt.json', stableRuntimeReceipt),
    writeJson('model-binding.json', modelBinding),
    writeJson('command-evidence.json', evidence),
    writeJson('current-attempt.json', ATTEMPT),
    writeJson('experiment-binding.json', BINDING),
  ];

  const payload={
    schemaVersion:RESULT_VERSION,
    disposition:'PASS',
    stopReasonOrNull:null,
    firstSubstantiveFailureOrNull:null,
    acceptedExitCodes:[0],
    candidateFindings:[],
    harnessFindings:[],
    environmentFindings:[],
    unknownFindings:[],
    effects:effects(),
    beforeState:{ experimentHead:BINDING.experiment.head, companionPullRequest:472 },
    afterState:summary,
    completedStepReceipts:evidence.map((row,i)=>({ step:'diagnostic-command-'+(i+1), argv:row.argv, cwd:process.cwd(), exitCode:row.exitCode, accepted:row.accepted })),
    retainedEvidenceFiles:retainedFiles(files),
  };
  fs.writeFileSync(path.join(RESULT_DIR,'payload-result.json'), JSON.stringify(payload,null,2)+'\n');
})().catch(error=>{
  const code=error?.code || 'RUNTIME_LINEAGE_DIAGNOSIS_FAILED';
  const failure=writeJson('failure.json',{ code, message:error.message, stack:error.stack || null });
  const payload={
    schemaVersion:RESULT_VERSION,
    disposition:'SAFE_FAILURE',
    stopReasonOrNull:code,
    firstSubstantiveFailureOrNull:null,
    acceptedExitCodes:[0],
    candidateFindings:[],
    harnessFindings:[code],
    environmentFindings:[],
    unknownFindings:[],
    effects:effects(),
    beforeState:{ experimentHead:BINDING.experiment.head, companionPullRequest:472 },
    afterState:{ error:code },
    completedStepReceipts:[],
    retainedEvidenceFiles:retainedFiles([failure]),
  };
  fs.writeFileSync(path.join(RESULT_DIR,'payload-result.json'), JSON.stringify(payload,null,2)+'\n');
});
