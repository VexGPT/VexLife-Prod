#!/usr/bin/env node
// [VXG RealForever]
import fs from 'node:fs';
import path from 'node:path';
import http from 'node:http';
import crypto from 'node:crypto';
import { spawnSync } from 'node:child_process';

const EXPECTED_SOURCE='2056a349408b62eea4db1fe6202517f45b67b2a1';
const EXPECTED_LABEL='com.vexlife.prod.service';
const SERVICE_ORIGIN='http://127.0.0.1:18210';
const SERVICE_RECEIPT=path.join(process.env.HOME||'', 'Library/Application Support/VexLife/ProductService/service-lifetime.json');
const ALLOWED_RUNTIME_RECEIPTS=new Set([
  'runtime/heretic/persistent-runtime-owner-receipt.json',
  'runtime/vexlife-prod/heretic-persistent-runtime-owner-receipt.json',
]);
const RESULT_DIR=process.env.VEXLOCALBRIDGE_RESULT_DIR;
if(!RESULT_DIR) throw new Error('VEXLOCALBRIDGE_RESULT_DIR is required');

const effects=()=>({
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
});
const sha=v=>crypto.createHash('sha256').update(String(v)).digest('hex');
const failure=(family,stage,summary)=>({
  failureFamily:family,
  failureFingerprint:sha(family+'\n'+stage+'\n'+summary+'\npayload-result.json'),
  stage,summary,evidenceRef:'payload-result.json',
});
const writeResult=(value)=>{
  fs.mkdirSync(RESULT_DIR,{recursive:true});
  fs.writeFileSync(path.join(RESULT_DIR,'payload-result.json'),JSON.stringify(value,null,2)+'\n');
};
const baseResult=()=>({
  schemaVersion:'vextreme.vexlocalbridge-mac-payload-result/v1',
  disposition:'PASS',
  stopReasonOrNull:null,
  firstSubstantiveFailureOrNull:null,
  acceptedExitCodes:[0],
  candidateFindings:[],
  harnessFindings:[],
  environmentFindings:[],
  unknownFindings:[],
  effects:effects(),
  beforeState:{},
  afterState:{},
  completedStepReceipts:[],
});
const fail=(code,summary,{candidate=false,effectOccurred=false,beforeState={},afterState={},steps=[]}={})=>{
  const result=baseResult();
  result.disposition=effectOccurred?'SAFE_FAILURE':'BLOCKED';
  result.stopReasonOrNull=code;
  result.firstSubstantiveFailureOrNull=failure(candidate?'CANDIDATE':'LIVED_HOST',effectOccurred?'RETIRE_AND_VERIFY':'PREFLIGHT',summary);
  (candidate?result.candidateFindings:result.environmentFindings).push(code);
  result.effects.modelRuntimePerformed=effectOccurred;
  result.beforeState=beforeState;
  result.afterState=afterState;
  result.completedStepReceipts=steps;
  writeResult(result);
  process.exit(0);
};
const delay=ms=>new Promise(r=>setTimeout(r,ms));
const processAlive=pid=>{
  try { process.kill(pid,0); return true; }
  catch(error) { return error?.code==='EPERM'; }
};
const httpJson=(route,timeoutMs=5000)=>new Promise((resolve,reject)=>{
  const req=http.get(SERVICE_ORIGIN+route,{headers:{accept:'application/json'}},res=>{
    let body='';
    res.setEncoding('utf8');
    res.on('data',chunk=>body+=chunk);
    res.on('end',()=>{
      if(res.statusCode!==200) return reject(new Error('HTTP_'+res.statusCode+'_'+route));
      try { resolve(JSON.parse(body)); } catch(error) { reject(new Error('INVALID_JSON_'+route+':'+error.message)); }
    });
  });
  req.setTimeout(timeoutMs,()=>req.destroy(new Error('HTTP_TIMEOUT_'+route)));
  req.on('error',reject);
});
const readJsonNoSymlink=file=>{
  const stat=fs.lstatSync(file);
  if(stat.isSymbolicLink()||!stat.isFile()) throw new Error('UNSAFE_OR_NONFILE:'+file);
  return JSON.parse(fs.readFileSync(file,'utf8'));
};

let before={};
try {
  const source=await httpJson('/api/v1/source-currentness');
  const current=await httpJson('/api/v1/current-state');
  if(source.sourceSha!==EXPECTED_SOURCE) {
    fail('A007_SERVICE_NOT_CURRENT','Expected A007 Product service is not current; open the retained A007 candidate and use its normal Update & Restart control first.',{beforeState:{observedSourceSha:source.sourceSha||null}});
  }
  if(!Number.isInteger(source.processPid)||source.processPid<=0||!processAlive(source.processPid)) {
    fail('A007_SERVICE_PID_NOT_LIVE','A007 source-currentness did not bind one live Product service PID.',{beforeState:{sourceSha:source.sourceSha||null,servicePid:source.processPid||null}});
  }

  const serviceReceipt=readJsonNoSymlink(SERVICE_RECEIPT);
  if(serviceReceipt.sourceSha!==EXPECTED_SOURCE) throw new Error('SERVICE_RECEIPT_SOURCE_MISMATCH');
  if(serviceReceipt.serviceOrigin!==SERVICE_ORIGIN) throw new Error('SERVICE_RECEIPT_ORIGIN_MISMATCH');
  if(serviceReceipt.launchd?.label!==EXPECTED_LABEL) throw new Error('SERVICE_RECEIPT_LABEL_MISMATCH');
  if(serviceReceipt.launchd?.pid!==source.processPid) throw new Error('SERVICE_RECEIPT_PID_MISMATCH');
  const expectedDomain='gui/'+process.getuid();
  if(serviceReceipt.launchd?.domainTarget!==expectedDomain) throw new Error('SERVICE_RECEIPT_DOMAIN_MISMATCH');
  if(serviceReceipt.homeBinding?.homePath && serviceReceipt.homeBinding.homePath!==current.homePath) throw new Error('SERVICE_HOME_BINDING_MISMATCH');

  const runtimeRelative=current.runtimeReceiptPath;
  if(!ALLOWED_RUNTIME_RECEIPTS.has(runtimeRelative)) throw new Error('RUNTIME_RECEIPT_PATH_NOT_ADMITTED');
  const homeRoot=path.resolve(current.homePath);
  const runtimePath=path.resolve(homeRoot,runtimeRelative);
  if(runtimePath===homeRoot||!runtimePath.startsWith(homeRoot+path.sep)) throw new Error('RUNTIME_RECEIPT_PATH_ESCAPE');
  const runtimeReceipt=readJsonNoSymlink(runtimePath);
  const runtimePid=runtimeReceipt?.runtime?.pid;
  if(!Number.isInteger(runtimePid)||runtimePid<=0) throw new Error('RUNTIME_RECEIPT_PID_INVALID');
  if(!processAlive(runtimePid)) throw new Error('RUNTIME_PID_NOT_LIVE_BEFORE_RETIREMENT');
  if(runtimePid===source.processPid) throw new Error('SERVICE_RUNTIME_PID_COLLAPSE');

  before={
    sourceSha:source.sourceSha,
    servicePid:source.processPid,
    serviceReceiptSchema:serviceReceipt.schemaVersion||null,
    serviceLabel:serviceReceipt.launchd.label,
    serviceDomain:serviceReceipt.launchd.domainTarget,
    homeRef:serviceReceipt.homeBinding?.homeRef||null,
    runtimeReceipt:runtimeRelative,
    runtimePid,
    runtimeAlive:true,
  };

  const target=serviceReceipt.launchd.domainTarget+'/'+serviceReceipt.launchd.label;
  const bootout=spawnSync('/bin/launchctl',['bootout',target],{encoding:'utf8',timeout:15000});
  const steps=[
    {step:'preflight-exact-a007-service-and-runtime',state:'PASS',sourceSha:EXPECTED_SOURCE,servicePid:source.processPid,runtimePid},
    {step:'launchctl-bootout-exact-owned-service',state:bootout.status===0?'DISPATCHED':'FAILED',target,exitCode:bootout.status},
  ];
  if(bootout.error||bootout.status!==0) {
    fail('EXACT_SERVICE_BOOTOUT_FAILED','Exact receipt-bound launchctl bootout failed: '+String(bootout.error?.message||bootout.stderr||bootout.stdout||bootout.status),{effectOccurred:true,beforeState:before,afterState:{serviceAlive:processAlive(source.processPid),runtimeAlive:processAlive(runtimePid)},steps});
  }

  const deadline=Date.now()+60000;
  let serviceAlive=true;
  let runtimeAlive=true;
  while(Date.now()<deadline) {
    serviceAlive=processAlive(source.processPid);
    runtimeAlive=processAlive(runtimePid);
    if(!serviceAlive&&!runtimeAlive) break;
    await delay(250);
  }
  const print=spawnSync('/bin/launchctl',['print',target],{encoding:'utf8',timeout:5000});
  const launchdLoaded=print.status===0;
  const after={servicePid:source.processPid,serviceAlive,runtimePid,runtimeAlive,launchdLoaded};
  steps.push({step:'verify-service-process-retired',state:serviceAlive?'FAIL':'PASS',pid:source.processPid});
  steps.push({step:'verify-runtime-session-released',state:runtimeAlive?'FAIL':'PASS',pid:runtimePid});
  steps.push({step:'verify-launchd-job-unloaded',state:launchdLoaded?'FAIL':'PASS',target});

  if(serviceAlive) fail('A007_SERVICE_SURVIVED_EXACT_BOOTOUT','The exact A007 Product service PID remained live after receipt-bound bootout.',{candidate:true,effectOccurred:true,beforeState:before,afterState:after,steps});
  if(runtimeAlive) fail('A007_RUNTIME_SURVIVED_SERVICE_TERMINATION','The exact pre-termination MLX runtime PID remained live after A007 Product service termination.',{candidate:true,effectOccurred:true,beforeState:before,afterState:after,steps});
  if(launchdLoaded) fail('A007_LAUNCHD_JOB_STILL_LOADED','The exact Product service launchd job remained loaded after bootout.',{effectOccurred:true,beforeState:before,afterState:after,steps});

  const result=baseResult();
  result.effects.modelRuntimePerformed=true;
  result.beforeState=before;
  result.afterState=after;
  result.completedStepReceipts=steps;
  result.completedStepReceipts.push({step:'lived-a007-service-to-runtime-release',state:'PASS'});
  writeResult(result);
} catch(error) {
  fail('A007_LIVED_RUNTIME_RELEASE_PREFLIGHT_FAILED',String(error?.message||error),{beforeState:before});
}
