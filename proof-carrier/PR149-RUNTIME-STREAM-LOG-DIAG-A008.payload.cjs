#!/usr/bin/env node
'use strict';

const fs=require('node:fs');
const path=require('node:path');
const crypto=require('node:crypto');

const RESULT_SCHEMA='vextreme.vexlocalbridge-mac-payload-result/v1';
const ORIGIN='http://127.0.0.1:18210';

function effects(){return {
  sourceMutationPerformed:false,branchMutationPerformed:false,claimMutationPerformed:false,
  prMetadataMutationPerformed:false,reviewPerformed:false,approvalPerformed:false,
  readyTransitionPerformed:false,mergePerformed:false,mainMutationPerformed:false,
  checkpointMutationPerformed:false,forcePushPerformed:false,historyRewritePerformed:false,
  hostInstallationPerformed:false,modelRuntimePerformed:false,providerOrNetworkEffectPerformed:false,
  publicationPerformed:false,credentialSerializationPerformed:false,
  filesystemOutsideQualifiedRootsPerformed:false
};}
function sha256(v){return crypto.createHash('sha256').update(v).digest('hex');}
function writeResult(v){
  const root=process.env.VEXLOCALBRIDGE_RESULT_DIR;
  if(!root) throw new Error('VEXLOCALBRIDGE_RESULT_DIR unavailable');
  fs.mkdirSync(root,{recursive:true});
  fs.writeFileSync(path.join(root,'payload-result.json'),JSON.stringify({
    schemaVersion:RESULT_SCHEMA,acceptedExitCodes:[0],...v
  },null,2)+'\n');
}
async function getJson(route){
  const r=await fetch(ORIGIN+route,{headers:{accept:'application/json'},cache:'no-store'});
  if(!r.ok) throw new Error(route+' HTTP '+r.status);
  return r.json();
}
function tail(file,max=200000){
  if(!fs.existsSync(file)||!fs.statSync(file).isFile()) return null;
  const b=fs.readFileSync(file);
  const s=Math.max(0,b.length-max);
  return b.subarray(s).toString('utf8');
}
function runtimeLogs(home){
  const root=path.join(home,'runtime');
  const out=[];
  if(!fs.existsSync(root)) return out;
  const walk=(dir,depth)=>{
    if(depth>5)return;
    for(const n of fs.readdirSync(dir)){
      const p=path.join(dir,n);
      let st; try{st=fs.statSync(p);}catch(_){continue;}
      if(st.isDirectory()) walk(p,depth+1);
      else if(n.startsWith('mlx-lm-server.')&&n.endsWith('.log')) out.push(p);
    }
  };
  walk(root,0);
  return out.sort();
}
function classify(text){
  const lower=String(text||'').toLowerCase();
  const tags=[];
  const needles=[
    ['traceback','PYTHON_TRACEBACK'],
    ['memory','MEMORY'],
    ['out of memory','OUT_OF_MEMORY'],
    ['killed','KILLED'],
    ['broken pipe','BROKEN_PIPE'],
    ['connection reset','CONNECTION_RESET'],
    ['exception','EXCEPTION'],
    ['error','ERROR'],
    ['metal','METAL'],
    ['mlx','MLX'],
    ['cancel','CANCEL'],
    ['generator','GENERATOR'],
    ['client disconnected','CLIENT_DISCONNECTED']
  ];
  for(const [needle,tag] of needles) if(lower.includes(needle)) tags.push(tag);
  return [...new Set(tags)];
}
async function main(){
  const current=await getJson('/api/v1/current-state');
  if(current.effectsPerformed!==false) throw new Error('current-state read performed effects');
  const home=current.homePath;
  if(!home||!path.isAbsolute(home)) throw new Error('current-state lacks Home path');
  const logs=runtimeLogs(home).map(file=>{
    const text=tail(file);
    return {
      path:path.relative(home,file),
      bytes:fs.statSync(file).size,
      modifiedAt:fs.statSync(file).mtime.toISOString(),
      sha256:sha256(fs.readFileSync(file)),
      classification:classify(text),
      tail:text
    };
  });
  const serviceLogs=[
    'runtime/vexlife-prod/service.out.log',
    'runtime/vexlife-prod/service.err.log'
  ].map(rel=>{
    const file=path.join(home,rel);
    if(!fs.existsSync(file))return null;
    const text=tail(file,100000);
    return {path:rel,bytes:fs.statSync(file).size,modifiedAt:fs.statSync(file).mtime.toISOString(),classification:classify(text),tail:text};
  }).filter(Boolean);

  writeResult({
    disposition:'PASS',stopReasonOrNull:null,firstSubstantiveFailureOrNull:null,
    candidateFindings:[],harnessFindings:[],environmentFindings:[],unknownFindings:[],
    effects:effects(),
    beforeState:{sourceSha:current.sourceSha??null,activeModelRef:current.activeModelRef??null,homeRef:current.homeRef??null},
    afterState:{runtimeLogs:logs,serviceLogs,logCount:logs.length},
    completedStepReceipts:[{step:'current-state-read',state:'PASS'},{step:'runtime-log-tail-read',state:'PASS',count:logs.length}]
  });
}
main().catch(error=>{
  try{writeResult({
    disposition:'SAFE_FAILURE',stopReasonOrNull:'RUNTIME_LOG_DIAGNOSTIC_STOPPED',
    firstSubstantiveFailureOrNull:{failureFamily:'VEXLIFE_PROD_RUNTIME_LOG_DIAGNOSTIC',failureFingerprint:sha256(Buffer.from(String(error?.message||error))),stage:'READ_ONLY_DIAGNOSTIC',summary:String(error?.message||error).slice(0,1800),evidenceRef:'after-state.json'},
    candidateFindings:[],harnessFindings:['RUNTIME_LOG_DIAGNOSTIC_STOPPED'],environmentFindings:[],unknownFindings:[],effects:effects(),
    beforeState:{},afterState:{errorMessage:String(error?.message||error)},completedStepReceipts:[]
  });}catch(_){}
  process.exitCode=0;
});