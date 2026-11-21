'use strict';

const fs = require('node:fs');
const path = require('node:path');
const os = require('node:os');
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

const HOME = path.resolve(ATTEMPT.home);
const ATTEMPT_SLUG = ATTEMPT.attemptRef.toLowerCase().replace(/[^a-z0-9._-]+/g, '-');
const STABLE = path.join(CAPSULE, 'bridge', 'retained-vexrelay', 'memory-depth-provisional-recontact-companion', ATTEMPT_SLUG);
const SERVICE_LIFETIME_RECEIPT = path.join(os.homedir(), 'Library', 'Application Support', 'VexLife', 'ProductService', 'service-lifetime.json');
const RUNTIME_RECEIPT = path.join(HOME, 'runtime', 'vexlife-prod', 'heretic-persistent-runtime-owner-receipt.json');
const GIT_SHA = /^[0-9a-f]{40}$/;

fs.rmSync(STABLE, {recursive:true, force:true});
fs.mkdirSync(STABLE, {recursive:true});
fs.mkdirSync(RESULT_DIR, {recursive:true});

const evidenceFiles=[];
const steps=[];
let modelRuntimePerformed=false;

function sha256File(file) {
  return crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex');
}
function stableFile(name, bytesOrText) {
  const file=path.join(STABLE,name);
  fs.mkdirSync(path.dirname(file),{recursive:true});
  fs.writeFileSync(file,bytesOrText);
  evidenceFiles.push(file);
  return file;
}
function stableJson(name,value) {
  return stableFile(name,JSON.stringify(value,null,2)+'\n');
}
function effects() {
  const x=Object.fromEntries(EFFECT_FIELDS.map(k=>[k,false]));
  x.modelRuntimePerformed=modelRuntimePerformed;
  return x;
}
function run(argv,{timeoutMs=300000,env={}}={}) {
  const started=Date.now();
  const r=cp.spawnSync(argv[0],argv.slice(1),{
    encoding:'utf8',shell:false,timeout:timeoutMs,maxBuffer:64*1024*1024,
    env:{...process.env,...env},
  });
  const timedOut=Boolean(r.error && (r.error.code==='ETIMEDOUT' || /timed out/i.test(r.error.message||'')));
  return {argv,exitCode:r.status??-1,signal:r.signal||null,timedOut,elapsedMs:Date.now()-started,
    stdout:r.stdout||'',stderr:r.stderr||'',error:r.error?String(r.error.stack||r.error.message||r.error):null,
    accepted:r.status===0 && !timedOut};
}
function commandStep(name,result) {
  const receipt={step:name,argv:result.argv,exitCode:result.exitCode,accepted:result.accepted,timedOut:result.timedOut,elapsedMs:result.elapsedMs};
  steps.push(receipt); stableJson('commands/'+name+'.json',result); return receipt;
}
function requireRun(name,argv,options) {
  const r=run(argv,options); commandStep(name,r);
  if(!r.accepted){const e=new Error(name+' failed');e.code=name.toUpperCase().replace(/[^A-Z0-9]+/g,'_')+'_FAILED';e.run=r;throw e;}
  return r;
}
function parseJsonDocument(text,label) {
  const trimmed=text.trim();
  try{return JSON.parse(trimmed);}catch{}
  const lines=trimmed.split(/\r?\n/).map(x=>x.trim()).filter(Boolean);
  for(let i=lines.length-1;i>=0;i--){try{return JSON.parse(lines[i]);}catch{}}
  throw Object.assign(new Error(label+' did not emit JSON'),{code:'JSON_OUTPUT_INVALID'});
}
function getJson(url,timeoutMs=5000) {
  return new Promise(resolve=>{
    const req=http.get(url,{timeout:timeoutMs},res=>{
      let body='';res.setEncoding('utf8');res.on('data',d=>body+=d);
      res.on('end',()=>{let json=null,parseError=null;try{json=JSON.parse(body);}catch(e){parseError=e.message;}
        resolve({state:'RESPONDED',url,statusCode:res.statusCode,body,json,parseError});});
    });
    req.on('timeout',()=>{req.destroy();resolve({state:'TIMEOUT',url});});
    req.on('error',e=>resolve({state:'UNAVAILABLE',url,error:e.message}));
  });
}
function retainedFiles() {
  return evidenceFiles.filter(fs.existsSync).map(sourcePath=>({
    sourcePath,
    targetPath:'retained/inner-vexrelay/memory-depth-provisional-recontact-companion/'+ATTEMPT_SLUG+'/'+path.relative(STABLE,sourcePath).split(path.sep).join('/'),
    expectedSha256:sha256File(sourcePath),
  }));
}
function readJsonFile(file,label) {
  if(!fs.existsSync(file)) throw Object.assign(new Error(label+' is missing: '+file),{code:label.toUpperCase().replace(/[^A-Z0-9]+/g,'_')+'_MISSING'});
  return JSON.parse(fs.readFileSync(file,'utf8'));
}
function processAlive(pid) {
  try{process.kill(pid,0);return true;}catch{return false;}
}
function validateRuntimeReceipt(receipt) {
  if(!receipt || receipt.state!=='PERSISTENT_MLX_RUNTIME_QUALIFIED') throw Object.assign(new Error('Heretic runtime receipt is not QUALIFIED'),{code:'RUNTIME_NOT_QUALIFIED'});
  const pid=receipt.runtime && Number(receipt.runtime.pid);
  if(!Number.isInteger(pid) || pid<=0 || !processAlive(pid)) throw Object.assign(new Error('qualified Heretic runtime receipt PID is not live'),{code:'RUNTIME_RECEIPT_PROCESS_NOT_LIVE'});
  if(receipt.modelRef!==BINDING.model.modelRef || receipt.modelProfileRef!==BINDING.model.modelProfileRef || receipt.artifactContentSetSha256!==BINDING.model.artifactContentSetSha256) {
    throw Object.assign(new Error('qualified runtime model relationship differs from bound 4B'),{code:'RUNTIME_MODEL_RELATIONSHIP_CHANGED'});
  }
  return pid;
}

(async()=>{
  stableJson('current-attempt.json',ATTEMPT);
  stableJson('experiment-binding.json',BINDING);

  const lifetime=readJsonFile(SERVICE_LIFETIME_RECEIPT,'service lifetime receipt');
  stableJson('before/service-lifetime-receipt.json',lifetime);
  const sourceSha=lifetime.sourceSha;
  if(!GIT_SHA.test(sourceSha||'')) throw Object.assign(new Error('service lifetime receipt source SHA is invalid'),{code:'SERVICE_SOURCE_SHA_INVALID'});

  const expectedImage=path.resolve(HOME,'runtime','product-service','images',sourceSha);
  const imagePath=path.resolve(String(lifetime.imagePath||''));
  if(imagePath!==expectedImage) throw Object.assign(new Error('service lifetime image path is not the exact installed Home coordinate'),{code:'SERVICE_IMAGE_COORDINATE_MISMATCH'});
  const setupLauncher=path.join(imagePath,'bin','vexlife-prod-setup');
  if(!fs.existsSync(setupLauncher)) throw Object.assign(new Error('installed Product setup launcher is missing'),{code:'SETUP_LAUNCHER_MISSING'});
  fs.accessSync(setupLauncher,fs.constants.X_OK);
  stableJson('recovery-binding.json',{home:HOME,serviceLifetimeReceipt:SERVICE_LIFETIME_RECEIPT,sourceSha,imagePath,setupLauncher,
    requestedControl:'START',allowAcquisition:false,allowRuntimeStart:true});

  const beforeRuntime=fs.existsSync(RUNTIME_RECEIPT)?readJsonFile(RUNTIME_RECEIPT,'runtime receipt'):null;
  stableJson('before/runtime-receipt.json',beforeRuntime);

  const inspectBeforeRun=requireRun('inspect-before',[setupLauncher,'--inspect-front-door','--home',HOME,'--expected-source-sha',sourceSha],{timeoutMs:180000});
  const inspectBefore=parseJsonDocument(inspectBeforeRun.stdout,'inspect-before');
  stableJson('before/front-door.json',inspectBefore);

  let action='NO_ACTION_ALREADY_CURRENT';
  let activation=null;
  if(inspectBefore.semanticStateRef==='START_REQUIRED') {
    const primary=inspectBefore.displayState && inspectBefore.displayState.primaryControlRef;
    if(primary!=='START') throw Object.assign(new Error('START_REQUIRED did not offer exact START control'),{code:'START_CONTROL_NOT_OFFERED'});
    modelRuntimePerformed=true;
    const activateRun=requireRun('activate-start',[setupLauncher,'--activate-front-door-control','START','--home',HOME,'--expected-source-sha',sourceSha],{timeoutMs:900000});
    activation=parseJsonDocument(activateRun.stdout,'activate-start');
    stableJson('activation.json',activation);
    if(activation.failure) throw Object.assign(new Error('Product START control returned failure: '+JSON.stringify(activation.failure)),{code:'PRODUCT_START_FAILED'});
    if(activation.semanticStateRef!=='CURRENT') throw Object.assign(new Error('Product START did not reobserve CURRENT: '+activation.semanticStateRef),{code:'PRODUCT_START_NOT_CURRENT'});
    action='START_FROM_EXISTING_PREREQUISITES';
  } else if(inspectBefore.semanticStateRef!=='CURRENT') {
    throw Object.assign(new Error('Product front door does not admit bounded START recovery from state '+inspectBefore.semanticStateRef),{code:'PRODUCT_START_NOT_LAWFUL'});
  }

  const inspectAfterRun=requireRun('inspect-after',[setupLauncher,'--inspect-front-door','--home',HOME,'--expected-source-sha',sourceSha],{timeoutMs:180000});
  const inspectAfter=parseJsonDocument(inspectAfterRun.stdout,'inspect-after');
  stableJson('after/front-door.json',inspectAfter);
  if(inspectAfter.semanticStateRef!=='CURRENT') throw Object.assign(new Error('Product did not remain CURRENT after recovery'),{code:'PRODUCT_NOT_CURRENT_AFTER_RECOVERY'});

  const service=await getJson('http://127.0.0.1:18210/api/v1/source-currentness');
  stableJson('after/service-currentness.json',service);
  if(service.statusCode!==200 || !service.json || service.json.sourceSha!==sourceSha) throw Object.assign(new Error('Product service currentness did not return exact recovered source'),{code:'SERVICE_CURRENTNESS_NOT_RESTORED'});

  const runtime=readJsonFile(RUNTIME_RECEIPT,'runtime receipt');
  stableJson('after/runtime-receipt.json',runtime);
  const runtimePid=validateRuntimeReceipt(runtime);

  const outcome={
    schemaVersion:'vexlife-prod.memory-depth.runtime-recovery-outcome/v1',
    complete:true,
    action,
    acquisitionAuthorized:false,
    runtimeStartAuthorized:true,
    serviceSourceSha:sourceSha,
    servicePid:service.json.pid||null,
    runtimeReceiptRef:runtime.receiptRef||null,
    runtimePid,
    modelRef:runtime.modelRef,
    modelProfileRef:runtime.modelProfileRef,
    artifactContentSetSha256:runtime.artifactContentSetSha256,
    semanticModelEvaluationPerformed:false,
    nextAttemptRef:ATTEMPT.heldSuccessor && ATTEMPT.heldSuccessor.attemptRef || null,
  };
  stableJson('outcome.json',outcome);

  const payloadResult={
    schemaVersion:RESULT_VERSION,disposition:'PASS',stopReasonOrNull:null,firstSubstantiveFailureOrNull:null,acceptedExitCodes:[0],
    candidateFindings:[],harnessFindings:[],environmentFindings:[],unknownFindings:[],effects:effects(),
    beforeState:{serviceSourceSha:sourceSha,frontDoor:inspectBefore,runtimeReceiptPresent:beforeRuntime!==null},
    afterState:outcome,completedStepReceipts:steps,retainedEvidenceFiles:retainedFiles(),
  };
  fs.writeFileSync(path.join(RESULT_DIR,'payload-result.json'),JSON.stringify(payloadResult,null,2)+'\n');
})().catch(error=>{
  const code=error?.code||'RUNTIME_RECOVERY_FAILED';
  stableJson('failure.json',{code,message:error.message,stack:error.stack||null});
  const payloadResult={
    schemaVersion:RESULT_VERSION,disposition:'SAFE_FAILURE',stopReasonOrNull:code,firstSubstantiveFailureOrNull:null,acceptedExitCodes:[0],
    candidateFindings:[],harnessFindings:[],environmentFindings:[code],unknownFindings:[],effects:effects(),
    beforeState:{home:HOME},afterState:{error:code,message:error.message},completedStepReceipts:steps,retainedEvidenceFiles:retainedFiles(),
  };
  fs.writeFileSync(path.join(RESULT_DIR,'payload-result.json'),JSON.stringify(payloadResult,null,2)+'\n');
});
