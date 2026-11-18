#!/usr/bin/env node
'use strict';

const fs=require('node:fs');
const path=require('node:path');
const os=require('node:os');
const crypto=require('node:crypto');
const {spawnSync}=require('node:child_process');

const RESULT_SCHEMA='vextreme.vexlocalbridge-mac-payload-result/v1';
function sha256(v){return crypto.createHash('sha256').update(v).digest('hex');}
function effects(){return {
  sourceMutationPerformed:false,branchMutationPerformed:false,claimMutationPerformed:false,
  prMetadataMutationPerformed:false,reviewPerformed:false,approvalPerformed:false,
  readyTransitionPerformed:false,mergePerformed:false,mainMutationPerformed:false,
  checkpointMutationPerformed:false,forcePushPerformed:false,historyRewritePerformed:false,
  hostInstallationPerformed:false,modelRuntimePerformed:false,providerOrNetworkEffectPerformed:false,
  publicationPerformed:false,credentialSerializationPerformed:false,
  filesystemOutsideQualifiedRootsPerformed:false,
};}
function writeResult(v){
  const root=process.env.VEXLOCALBRIDGE_RESULT_DIR;
  if(!root) throw new Error('VEXLOCALBRIDGE_RESULT_DIR unavailable');
  fs.mkdirSync(root,{recursive:true});
  fs.writeFileSync(path.join(root,'payload-result.json'),JSON.stringify({
    schemaVersion:RESULT_SCHEMA,acceptedExitCodes:[0],...v
  },null,2)+'\n');
}
function run(cmd,args,timeoutMs=10000){
  const r=spawnSync(cmd,args,{encoding:'utf8',timeout:timeoutMs,maxBuffer:16*1024*1024,stdio:['ignore','pipe','pipe']});
  return {status:Number.isInteger(r.status)?r.status:127,stdout:String(r.stdout||''),stderr:String(r.stderr||(r.error?r.error.message:''))};
}
function safeJson(s){try{return JSON.parse(s);}catch(_){return null;}}
function strip(v){
  let x=String(v||'').trim();
  if(/^\d+\s*=\s*/.test(x)) x=x.replace(/^\d+\s*=\s*/,'');
  if(x.startsWith('"')&&x.endsWith('"')) x=x.slice(1,-1);
  return x;
}
function parseLaunchctl(stdout){
  const lines=String(stdout||'').split(/\r?\n/);
  const pid=String(stdout||'').match(/^\s*pid\s*=\s*(\d+)\s*$/m);
  const program=String(stdout||'').match(/^\s*program\s*=\s*(.+)\s*$/m);
  const start=lines.findIndex(l=>l.trim()==='arguments = {');
  const args=[];
  if(start>=0){
    for(let i=start+1;i<lines.length;i++){
      const l=lines[i].trim();
      if(l==='}') break;
      if(l) args.push(strip(l));
    }
  }
  return {pid:pid?Number(pid[1]):null,programPath:program?strip(program[1]):null,args};
}
function parseArgs(launch){
  const args=[...launch.args];
  if(launch.programPath && args[0]===launch.programPath) args.shift();
  let sourceSha=null,homePath=null;
  for(let i=0;i<args.length-1;i++){
    if(args[i]==='--source-sha') sourceSha=args[i+1];
    if(args[i]==='--home') homePath=args[i+1];
  }
  return {args,sourceSha,homePath};
}
function curlJson(url){
  const r=run('/usr/bin/curl',['--silent','--show-error','--max-time','4',url],6000);
  return {exitCode:r.status,body:r.stdout,json:safeJson(r.stdout),stderr:r.stderr};
}
function treeBytes(root){
  let total=0;
  const stack=[root];
  while(stack.length){
    const p=stack.pop();
    let st;
    try{st=fs.lstatSync(p);}catch(_){continue;}
    if(st.isSymbolicLink()) continue;
    if(st.isFile()){total+=st.size;continue;}
    if(st.isDirectory()){
      for(const name of fs.readdirSync(p)) stack.push(path.join(p,name));
    }
  }
  return total;
}
function readFileObs(p){
  try{
    const st=fs.lstatSync(p);
    if(st.isSymbolicLink()) return {path:p,kind:'SYMLINK'};
    if(!st.isFile()) return {path:p,kind:st.isDirectory()?'DIRECTORY':'OTHER'};
    const b=fs.readFileSync(p);
    return {path:p,kind:'FILE',bytes:b.length,sha256:sha256(b),json:safeJson(b.toString('utf8'))};
  }catch(e){
    if(e&&e.code==='ENOENT') return {path:p,kind:'ABSENT'};
    return {path:p,kind:'UNREADABLE',error:String(e?.message||e)};
  }
}
function retain(root,name,value){
  const full=path.join(root,name);
  fs.mkdirSync(path.dirname(full),{recursive:true});
  const text=typeof value==='string'?value:JSON.stringify(value,null,2)+'\n';
  fs.writeFileSync(full,text);
  return {sourcePath:full,targetPath:'retained/inner-vexrelay/retention-proof/'+name,expectedSha256:sha256(Buffer.from(text))};
}

async function main(){
  const bindingPath=process.argv[2];
  if(!bindingPath) throw new Error('binding path required');
  const binding=JSON.parse(fs.readFileSync(path.resolve(bindingPath),'utf8'));
  const capsuleRoot=process.env.VEXLOCALBRIDGE_CAPSULE_ROOT;
  if(!capsuleRoot||!path.isAbsolute(capsuleRoot)) throw new Error('VEXLOCALBRIDGE_CAPSULE_ROOT unavailable');
  const retainedRoot=path.join(capsuleRoot,'bridge','retained-diagnostics',process.env.VEXLOCALBRIDGE_TASK_REF||'task',process.env.VEXLOCALBRIDGE_ATTEMPT_REF||'attempt');
  fs.mkdirSync(retainedRoot,{recursive:true});
  const retainedEvidenceFiles=[];
  const completedStepReceipts=[];

  try{
    if(process.platform!=='darwin'||process.arch!=='arm64') throw new Error('retention proof requires Apple-Silicon macOS');
    const uid=run('/usr/bin/id',['-u']);
    if(uid.status!==0) throw new Error('cannot resolve uid');
    const target='gui/'+uid.stdout.trim()+'/com.vexlife.prod.service';
    const lr=run('/bin/launchctl',['print',target]);
    retainedEvidenceFiles.push(retain(retainedRoot,'launchctl.txt',lr.stdout+(lr.stderr?'\nSTDERR:\n'+lr.stderr:'')));
    if(lr.status!==0) throw new Error('Product launchd job is not present');
    const launch=parseLaunchctl(lr.stdout);
    const identity=parseArgs(launch);
    if(!launch.pid||!identity.sourceSha||!identity.homePath) throw new Error('live Product launch shape is incomplete');

    const currentness=curlJson('http://127.0.0.1:18210/api/v1/source-currentness');
    retainedEvidenceFiles.push(retain(retainedRoot,'source-currentness.json',currentness.json||{body:currentness.body,stderr:currentness.stderr}));
    if(currentness.exitCode!==0||!currentness.json) throw new Error('typed Product source-currentness endpoint is unavailable');

    const currentMatches =
      identity.sourceSha===binding.currentSourceSha &&
      currentness.json.sourceSha===binding.currentSourceSha &&
      Number(currentness.json.processPid)===Number(launch.pid) &&
      currentness.json.serviceState==='SOURCE_BOUND_LISTENING' &&
      currentness.json.effectsPerformed===false;

    const home=path.resolve(identity.homePath);
    const imageBase=path.join(home,'runtime/product-service/images');
    const imageEntries=[];
    if(fs.existsSync(imageBase)&&fs.statSync(imageBase).isDirectory()){
      for(const name of fs.readdirSync(imageBase).sort()){
        const entry=path.join(imageBase,name);
        const st=fs.lstatSync(entry);
        const manifest=readFileObs(path.join(entry,'service-image.json'));
        imageEntries.push({
          name,
          kind:st.isSymbolicLink()?'SYMLINK':st.isDirectory()?'DIRECTORY':st.isFile()?'FILE':'OTHER',
          bytes:st.isDirectory()&&!st.isSymbolicLink()?treeBytes(entry):(st.isFile()?st.size:0),
          manifestKind:manifest.kind,
          manifestSourceSha:manifest.json?.sourceSha||null,
          manifestPayloadSha256:manifest.json?.payloadSha256||null,
          javaPresent:fs.existsSync(path.join(entry,'runtime/bin/java')),
        });
      }
    }

    const machineReceiptPath=path.join(os.homedir(),'Library/Application Support/VexLife/ProductService/service-lifetime.json');
    const machineReceipt=readFileObs(machineReceiptPath);
    retainedEvidenceFiles.push(retain(retainedRoot,'machine-lifetime-receipt.json',machineReceipt));
    retainedEvidenceFiles.push(retain(retainedRoot,'image-inventory.json',{imageBase,imageEntries}));

    const currentEntry=imageEntries.find(x=>x.name===binding.currentSourceSha)||null;
    const priorEntry=imageEntries.find(x=>x.name===binding.priorSourceSha)||null;
    const currentReceiptMatches =
      machineReceipt.kind==='FILE' &&
      machineReceipt.json?.schemaVersion==='vexlife-prod.macos-service-lifetime/v2' &&
      machineReceipt.json?.sourceSha===binding.currentSourceSha &&
      Number(machineReceipt.json?.launchd?.pid)===Number(launch.pid);

    const shaLikeRemaining=imageEntries.filter(x=>/^[0-9a-f]{40}$/.test(x.name)&&x.name!==binding.currentSourceSha);

    const summary={
      currentSourceSha:binding.currentSourceSha,
      priorSourceSha:binding.priorSourceSha,
      liveLaunchSourceSha:identity.sourceSha,
      livePid:launch.pid,
      selectedHome:home,
      currentnessMatches:currentMatches,
      machineLifetimeReceiptMatches:currentReceiptMatches,
      currentImagePresent:Boolean(currentEntry),
      currentImageBytes:currentEntry?.bytes||null,
      currentImageJavaPresent:Boolean(currentEntry?.javaPresent),
      priorImagePresent:Boolean(priorEntry),
      priorImageBytes:priorEntry?.bytes||null,
      remainingShaLikeImageEntries:shaLikeRemaining,
      imageEntryCount:imageEntries.length,
      retentionAcceptance:
        currentMatches &&
        currentReceiptMatches &&
        Boolean(currentEntry) &&
        Boolean(currentEntry?.javaPresent) &&
        !priorEntry,
    };
    retainedEvidenceFiles.push(retain(retainedRoot,'retention-summary.json',summary));

    completedStepReceipts.push({step:'live-current-service',state:currentMatches?'PASS':'SAFE_FAILURE'});
    completedStepReceipts.push({step:'machine-lifetime-receipt',state:currentReceiptMatches?'PASS':'SAFE_FAILURE'});
    completedStepReceipts.push({step:'current-image-present',state:currentEntry?'PASS':'SAFE_FAILURE'});
    completedStepReceipts.push({step:'prior-image-reclaimed',state:priorEntry?'SAFE_FAILURE':'PASS'});
    completedStepReceipts.push({step:'retention-acceptance',state:summary.retentionAcceptance?'PASS':'SAFE_FAILURE'});

    writeResult({
      disposition:summary.retentionAcceptance?'PASS':'SAFE_FAILURE',
      stopReasonOrNull:summary.retentionAcceptance?null:'PR307_LIVED_RETENTION_ACCEPTANCE_NOT_MET',
      firstSubstantiveFailureOrNull:summary.retentionAcceptance?null:{
        failureFamily:'VEXLIFE_PROD_PR307_LIVED_RETENTION',
        failureFingerprint:sha256(Buffer.from(JSON.stringify(summary))),
        stage:'POST_UPDATE_RETENTION_OBSERVATION',
        summary:'post-update retention predicates did not all hold; see retained summary',
        evidenceRef:'retained/inner-vexrelay/retention-proof/retention-summary.json'
      },
      candidateFindings:summary.retentionAcceptance?[]:['LIVED_RETENTION_ACCEPTANCE_NOT_MET'],
      harnessFindings:[],environmentFindings:[],unknownFindings:[],
      effects:effects(),
      beforeState:{inspectionOnly:true,expectedCurrentSourceSha:binding.currentSourceSha,expectedPriorSourceSha:binding.priorSourceSha},
      afterState:{inspectionOnly:true,summary},
      completedStepReceipts,retainedEvidenceFiles
    });
  }catch(error){
    writeResult({
      disposition:'SAFE_FAILURE',stopReasonOrNull:'PR307_LIVED_RETENTION_INSPECTION_STOPPED',
      firstSubstantiveFailureOrNull:{
        failureFamily:'VEXLIFE_PROD_PR307_LIVED_RETENTION_HARNESS',
        failureFingerprint:sha256(Buffer.from(String(error?.message||error))),
        stage:'HOST_INSPECTION',summary:String(error?.message||error).slice(0,4000),evidenceRef:'payload-result.json'
      },
      candidateFindings:[],harnessFindings:['RETENTION_INSPECTION_STOPPED'],environmentFindings:[],unknownFindings:[],
      effects:effects(),beforeState:{inspectionOnly:true},afterState:{inspectionOnly:true},
      completedStepReceipts,retainedEvidenceFiles
    });
  }
}
main().catch(error=>{
  try{writeResult({
    disposition:'SAFE_FAILURE',stopReasonOrNull:'UNCAUGHT_PR307_LIVED_RETENTION_INSPECTION_FAILURE',
    firstSubstantiveFailureOrNull:{
      failureFamily:'VEXLIFE_PROD_PR307_LIVED_RETENTION_UNCAUGHT',
      failureFingerprint:sha256(Buffer.from(String(error?.message||error))),
      stage:'UNCAUGHT',summary:String(error?.message||error).slice(0,4000),evidenceRef:'payload-result.json'
    },
    candidateFindings:[],harnessFindings:['UNCAUGHT_RETENTION_INSPECTION_FAILURE'],environmentFindings:[],unknownFindings:[],
    effects:effects(),beforeState:{},afterState:{},completedStepReceipts:[],retainedEvidenceFiles:[]
  });}catch(_){}
  process.exitCode=0;
});

// [VXG RealForever]
