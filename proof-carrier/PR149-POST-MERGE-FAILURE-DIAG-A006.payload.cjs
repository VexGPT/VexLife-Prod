#!/usr/bin/env node
'use strict';

const fs=require('node:fs');
const path=require('node:path');
const crypto=require('node:crypto');

const RESULT_SCHEMA='vextreme.vexlocalbridge-mac-payload-result/v1';
const ORIGIN='http://127.0.0.1:18210';
const SOURCE='0aa1de1341cfd5e0c6135cbd30bfa8d19613d6d5';
const TURN='turn.vexlife.browser-companion.ce0f7c4d-1c0e-4113-a091-258dda79769e';

function sha256(v){return crypto.createHash('sha256').update(v).digest('hex');}
function effects(){return {
  sourceMutationPerformed:false,branchMutationPerformed:false,claimMutationPerformed:false,
  prMetadataMutationPerformed:false,reviewPerformed:false,approvalPerformed:false,
  readyTransitionPerformed:false,mergePerformed:false,mainMutationPerformed:false,
  checkpointMutationPerformed:false,forcePushPerformed:false,historyRewritePerformed:false,
  hostInstallationPerformed:false,modelRuntimePerformed:false,providerOrNetworkEffectPerformed:false,
  publicationPerformed:false,credentialSerializationPerformed:false,
  filesystemOutsideQualifiedRootsPerformed:false
};}
function writeResult(v){
  const root=process.env.VEXLOCALBRIDGE_RESULT_DIR;
  if(!root) throw new Error('VEXLOCALBRIDGE_RESULT_DIR unavailable');
  fs.mkdirSync(root,{recursive:true});
  fs.writeFileSync(path.join(root,'payload-result.json'),JSON.stringify({
    schemaVersion:RESULT_SCHEMA,acceptedExitCodes:[0],...v
  },null,2)+'\n');
}
async function getJson(route){
  const response=await fetch(ORIGIN+route,{headers:{accept:'application/json'},cache:'no-store'});
  if(!response.ok) throw new Error(route+' returned HTTP '+response.status);
  return response.json();
}
function chronologyFiles(home){
  const dir=path.join(home,'runtime/vexlife-prod/logs/chronology');
  if(!fs.existsSync(dir)) return [];
  return fs.readdirSync(dir)
    .filter(n=>n.startsWith('chronology')&&n.endsWith('.jsonl'))
    .map(n=>path.join(dir,n));
}
function matchingFailure(home){
  const matches=[];
  for(const file of chronologyFiles(home)){
    for(const line of fs.readFileSync(file,'utf8').split(/\n/)){
      if(!line.trim()) continue;
      try{
        const r=JSON.parse(line);
        if(
          r.schemaVersion==='vexlife-prod.chronology-record/v1' &&
          r.sourceSha===SOURCE &&
          r.fact==='TURN_FAILED' &&
          r.refs?.turnRef===TURN
        ){
          matches.push({
            at:r.at,
            sequence:r.sequence,
            owner:r.owner,
            fact:r.fact,
            refs:r.refs||{},
            detail:r.detail??null,
            chronologyFile:path.basename(file)
          });
        }
      }catch(_){}
    }
  }
  matches.sort((a,b)=>String(a.at).localeCompare(String(b.at))||a.sequence-b.sequence);
  return matches;
}
function failureReceipt(home){
  const file=path.join(home,'recovery/direct-companion',TURN+'.json');
  if(!fs.existsSync(file)) return null;
  const r=JSON.parse(fs.readFileSync(file,'utf8'));
  return {
    failureRef:r.failureRef??null,
    turnRef:r.turnRef??null,
    failureCode:r.failureCode??null,
    requestDurablyRecorded:r.requestDurablyRecorded??null,
    responseDurablyRecorded:r.responseDurablyRecorded??null,
    contextDurablyRecorded:r.contextDurablyRecorded??null,
    currentHeadCommitted:r.currentHeadCommitted??null,
    retrySameSendAttemptAllowed:r.retrySameSendAttemptAllowed??null,
    resumePossible:r.resumePossible??null,
    exactNextSafeRoute:r.exactNextSafeRoute??null,
    formedAt:r.formedAt??null
  };
}

async function main(){
  const current=await getJson('/api/v1/current-state');
  if(current.effectsPerformed!==false) throw new Error('current-state read performed effects');
  if(!current.homePath||!path.isAbsolute(current.homePath)) throw new Error('current-state lacks Home path');
  const failures=matchingFailure(current.homePath);
  if(failures.length!==1) throw new Error('expected exactly one matching TURN_FAILED chronology record, got '+failures.length);
  const detail=String(failures[0].detail||'');
  const classification=
    detail.includes('closed before [DONE]') ? 'STREAM_CLOSED_BEFORE_DONE' :
    detail.includes('response content is empty') ? 'DONE_WITH_EMPTY_CONTENT' :
    detail.includes('conflicting reasoning fields') ? 'CONFLICTING_REASONING_FIELDS' :
    detail.includes('invalid SSE JSON') ? 'INVALID_SSE_JSON' :
    detail.includes('non-SSE line') ? 'NON_SSE_LINE' :
    'OTHER_ENDPOINT_RESPONSE_INVALID';

  writeResult({
    disposition:'PASS',
    stopReasonOrNull:null,
    firstSubstantiveFailureOrNull:null,
    candidateFindings:[],
    harnessFindings:[],
    environmentFindings:[],
    unknownFindings: classification==='OTHER_ENDPOINT_RESPONSE_INVALID' ? ['UNCLASSIFIED_ENDPOINT_RESPONSE_INVALID'] : [],
    effects:effects(),
    beforeState:{
      sourceSha:SOURCE,
      turnRef:TURN,
      currentProductSourceSha:current.sourceSha??null,
      homeRef:current.homeRef??null
    },
    afterState:{
      classification,
      turnFailedChronology:failures[0],
      failureReceipt:failureReceipt(current.homePath),
      detailSha256:sha256(Buffer.from(detail))
    },
    completedStepReceipts:[
      {step:'current-state-read',state:'PASS'},
      {step:'turn-failed-detail-recovered',state:'PASS',classification}
    ]
  });
}
main().catch(error=>{
  try{writeResult({
    disposition:'SAFE_FAILURE',
    stopReasonOrNull:'READ_ONLY_FAILURE_DETAIL_RECOVERY_STOPPED',
    firstSubstantiveFailureOrNull:{
      failureFamily:'VEXLIFE_PROD_FAILURE_DETAIL_RECOVERY',
      failureFingerprint:sha256(Buffer.from(String(error?.message||error))),
      stage:'READ_ONLY_DIAGNOSTIC',
      summary:String(error?.message||error).slice(0,1800),
      evidenceRef:'after-state.json'
    },
    candidateFindings:[],harnessFindings:['READ_ONLY_FAILURE_DETAIL_RECOVERY_STOPPED'],
    environmentFindings:[],unknownFindings:[],effects:effects(),
    beforeState:{sourceSha:SOURCE,turnRef:TURN},
    afterState:{errorMessage:String(error?.message||error).slice(0,1800)},
    completedStepReceipts:[]
  });}catch(_){}
  process.exitCode=0;
});
