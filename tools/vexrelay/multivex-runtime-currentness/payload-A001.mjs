import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import crypto from 'node:crypto';
import { spawnSync } from 'node:child_process';

const BASE='f1855e9be6b9172ba6b43b597fa5d902dc409ed1';
const HEAD='f9cc4a05ab2be6a45127ad4badaab8c9110afbb4';
const TREE='220e7ddccca1b45ae56e576041ce2bc860dbf229';
const BRANCH='VXG-112226-evolve-context-dynamic-counterpart-dcp1-a001';
const REPOSITORY='VexGPT/VexLife-Prod';
const TASK_REF='task.vexlife-prod.multivex.runtime-currentness.a001';
const ATTEMPT_PREFIX='attempt.vexlife-prod.multivex.runtime-currentness.a001.';
const RETURN_ROUTE_REF='github.pull.vexlife-prod.543';
const PRIOR_SUCCESS_RECEIPT_REF='receipt.vexlife-prod.heretic-persistent-runtime.6bb92df935fd3446f35a60a3';
const PRIOR_SUCCESS_PID=31743;
const PRIOR_SUCCESS_CACHE=2;

const RESULT_DIR=process.env.VEXLOCALBRIDGE_RESULT_DIR;
const CAPSULE_ROOT=process.env.VEXLOCALBRIDGE_CAPSULE_ROOT;
const HOME=path.join(os.homedir(),'.vexlife');
const RESULT_FILE=RESULT_DIR && path.join(RESULT_DIR,'payload-result.json');
const PROGRESS_FILE=CAPSULE_ROOT && path.join(CAPSULE_ROOT,'bridge','progress','multivex-runtime-currentness.log');

const effects={
  sourceMutationPerformed:false,branchMutationPerformed:false,claimMutationPerformed:false,
  prMetadataMutationPerformed:false,reviewPerformed:false,approvalPerformed:false,
  readyTransitionPerformed:false,mergePerformed:false,mainMutationPerformed:false,
  checkpointMutationPerformed:false,forcePushPerformed:false,historyRewritePerformed:false,
  hostInstallationPerformed:false,modelRuntimePerformed:false,providerOrNetworkEffectPerformed:false,
  publicationPerformed:false,credentialSerializationPerformed:false,filesystemOutsideQualifiedRootsPerformed:false,
};
const beforeState={candidate:{base:BASE,head:HEAD,tree:TREE,branch:BRANCH}};
const afterState={};
const completedStepReceipts=[];
const candidateFindings=[],harnessFindings=[],environmentFindings=[],unknownFindings=[];

class Stop extends Error{constructor(category,code){super(code);this.category=category;this.code=code;}}
const stop=(category,code)=>{throw new Stop(category,code);};
function stage(v){
  const line='[MULTI-VEX-RUNTIME-CURRENTNESS] '+v;
  process.stdout.write(line+'\n');
  if(PROGRESS_FILE){fs.mkdirSync(path.dirname(PROGRESS_FILE),{recursive:true});fs.appendFileSync(PROGRESS_FILE,line+'\n');}
}
function sha256Bytes(b){return crypto.createHash('sha256').update(b).digest('hex');}
function sha256File(p){return sha256Bytes(fs.readFileSync(p));}
function readOwnedJson(relative,label){
  const p=path.join(HOME,relative);
  if(!fs.existsSync(p)) return {path:relative,present:false};
  const st=fs.lstatSync(p);
  if(st.isSymbolicLink()) stop('environment',label+'_SYMLINK_REJECTED');
  if(!st.isFile()) stop('environment',label+'_NOT_FILE');
  const bytes=fs.readFileSync(p);
  let value;
  try{value=JSON.parse(bytes.toString('utf8'));}catch(e){stop('environment',label+'_JSON_INVALID');}
  return {path:relative,present:true,sha256:sha256Bytes(bytes),value};
}
function processAlive(pid){
  if(!Number.isInteger(pid)||pid<=0) return null;
  try{process.kill(pid,0);return true;}catch(e){if(e?.code==='ESRCH') return false;return null;}
}
function promptCache(args){
  if(!Array.isArray(args)) return null;
  const i=args.indexOf('--prompt-cache-size');
  if(i<0||i+1>=args.length) return null;
  const n=Number(args[i+1]);
  return Number.isInteger(n)?n:null;
}
function ps(pid){
  if(!Number.isInteger(pid)||pid<=0) return null;
  const r=spawnSync('/bin/ps',['-p',String(pid),'-o','pid=,ppid=,command='],{encoding:'utf8',timeout:5000});
  return {exitCode:r.status,stdout:String(r.stdout||'').trim(),stderr:String(r.stderr||'').trim()};
}
function health(){
  const r=spawnSync('/usr/bin/curl',['-sS','--max-time','3','-w','\n%{http_code}','http://127.0.0.1:8765/health'],{encoding:'utf8',timeout:5000});
  const out=String(r.stdout||'');
  const lines=out.split(/\n/);
  const code=lines.pop()||'';
  return {exitCode:r.status,httpCode:code,body:lines.join('\n').slice(0,4000),stderr:String(r.stderr||'').slice(0,4000)};
}
function addFinding(cat,code){
  if(cat==='candidate')candidateFindings.push(code);
  else if(cat==='harness')harnessFindings.push(code);
  else if(cat==='environment')environmentFindings.push(code);
  else unknownFindings.push(code);
}
function writeResult(disposition,stopReasonOrNull=null){
  fs.mkdirSync(RESULT_DIR,{recursive:true});
  afterState.progressTail=PROGRESS_FILE&&fs.existsSync(PROGRESS_FILE)?fs.readFileSync(PROGRESS_FILE,'utf8').slice(-16000):'';
  fs.writeFileSync(RESULT_FILE,JSON.stringify({
    schemaVersion:'vextreme.vexlocalbridge-mac-payload-result/v1',disposition,stopReasonOrNull,
    acceptedExitCodes:[0],candidateFindings:[...new Set(candidateFindings)],
    harnessFindings:[...new Set(harnessFindings)],environmentFindings:[...new Set(environmentFindings)],
    unknownFindings:[...new Set(unknownFindings)],effects,beforeState,afterState,completedStepReceipts,
  },null,2)+'\n');
}

try{
  if(!RESULT_DIR||!CAPSULE_ROOT) stop('harness','VEXLOCALBRIDGE_PAYLOAD_ENV_MISSING');
  stage('00 admit read-only runtime-currentness observation');
  const taskRef=process.env.VEXLOCALBRIDGE_TASK_REF||null;
  const attemptRef=process.env.VEXLOCALBRIDGE_ATTEMPT_REF||null;
  const returnRouteRef=process.env.VEXLOCALBRIDGE_RETURN_ROUTE_REF||null;
  const policy=JSON.parse(process.env.VEXLOCALBRIDGE_VEXLIFE_PROD_HOST_EFFECT_POLICY_JSON||'null');
  if(taskRef!==TASK_REF||typeof attemptRef!=='string'||!attemptRef.startsWith(ATTEMPT_PREFIX)||returnRouteRef!==RETURN_ROUTE_REF){
    stop('harness','TASK_ENV_BINDING_MISMATCH');
  }
  if(policy?.repository!==REPOSITORY||policy?.candidate?.head!==HEAD||policy?.candidate?.tree!==TREE){
    stop('harness','PRODUCT_HOST_EFFECT_POLICY_BINDING_MISMATCH');
  }
  if(!fs.existsSync(HOME)||!fs.statSync(HOME).isDirectory()) stop('environment','DEFAULT_VEXLIFE_HOME_MISSING');

  stage('01 read Product-owned runtime coordinates');
  const modelBinding=readOwnedJson('runtime/vexlife-prod/model-binding.json','MODEL_BINDING');
  const productReceipt=readOwnedJson('runtime/vexlife-prod/heretic-persistent-runtime-owner-receipt.json','PRODUCT_RUNTIME_RECEIPT');
  const runtimeConfig=readOwnedJson('runtime/vexlife-prod/heretic-runtime-configuration.json','RUNTIME_CONFIGURATION');
  const stableReceipt=readOwnedJson('runtime/heretic/persistent-runtime-owner-receipt.json','STABLE_RUNTIME_RECEIPT');

  const receipt=productReceipt.present?productReceipt.value:null;
  const pid=receipt?.runtime?.pid??null;
  const args=receipt?.runtime?.args??null;
  const cache=promptCache(args);

  stage('02 observe receipt process and loopback health');
  effects.providerOrNetworkEffectPerformed=true;
  const runtimeObservation={
    priorSuccessful:{receiptRef:PRIOR_SUCCESS_RECEIPT_REF,pid:PRIOR_SUCCESS_PID,promptCacheEntries:PRIOR_SUCCESS_CACHE},
    current:{
      receiptPresent:productReceipt.present,
      receiptSha256:productReceipt.sha256??null,
      receiptRef:receipt?.receiptRef??null,
      formedAt:receipt?.formedAt??null,
      state:receipt?.state??null,
      modelRef:receipt?.modelRef??null,
      modelProfileRef:receipt?.modelProfileRef??null,
      authorityRef:receipt?.authorityRef??null,
      implementationIdentity:receipt?.implementationIdentity??null,
      pid,
      processGroupId:receipt?.runtime?.processGroupId??null,
      exactOwnedDetachedGroup:receipt?.runtime?.exactOwnedDetachedGroup??null,
      args,
      promptCacheEntries:cache,
      processAlive:processAlive(pid),
      processView:ps(pid),
      health:health(),
    },
    changedFromPriorSuccessful:{
      receiptRef:receipt?.receiptRef!==PRIOR_SUCCESS_RECEIPT_REF,
      pid:pid!==PRIOR_SUCCESS_PID,
      promptCacheEntries:cache!==PRIOR_SUCCESS_CACHE,
    },
    modelBinding,
    runtimeConfiguration:runtimeConfig,
    stableRuntimeReceipt:stableReceipt,
  };

  afterState.runtimeCurrentnessDiagnostic=runtimeObservation;

  stage('03 verify read-only coordinates did not change during observation');
  const modelBindingAfter=readOwnedJson('runtime/vexlife-prod/model-binding.json','MODEL_BINDING_AFTER');
  const productReceiptAfter=readOwnedJson('runtime/vexlife-prod/heretic-persistent-runtime-owner-receipt.json','PRODUCT_RUNTIME_RECEIPT_AFTER');
  const runtimeConfigAfter=readOwnedJson('runtime/vexlife-prod/heretic-runtime-configuration.json','RUNTIME_CONFIGURATION_AFTER');
  afterState.readOnlyStable={
    modelBinding:modelBinding.sha256===modelBindingAfter.sha256,
    productReceipt:productReceipt.sha256===productReceiptAfter.sha256,
    runtimeConfiguration:runtimeConfig.sha256===runtimeConfigAfter.sha256,
  };
  if(Object.values(afterState.readOnlyStable).some(v=>v===false)) candidateFindings.push('RUNTIME_COORDINATE_CHANGED_DURING_OBSERVATION');

  completedStepReceipts.push({step:'runtime-currentness-observation',state:candidateFindings.length===0?'PASS':'SAFE_FAILURE'});
  stage('04 runtime-currentness observation complete');
  writeResult(candidateFindings.length===0?'PASS':'SAFE_FAILURE',candidateFindings.length===0?null:'RUNTIME_COORDINATE_CHANGED_DURING_OBSERVATION');
}catch(error){
  if(error instanceof Stop){addFinding(error.category,error.code);stage('STOP '+error.code);writeResult('SAFE_FAILURE',error.code);}
  else{harnessFindings.push('RUNTIME_CURRENTNESS_UNCAUGHT_'+String(error?.name||'ERROR'));afterState.uncaughtMessage=String(error?.message||'');writeResult('SAFE_FAILURE','RUNTIME_CURRENTNESS_UNCAUGHT');}
}
