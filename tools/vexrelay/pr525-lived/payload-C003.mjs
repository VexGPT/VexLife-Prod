import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import crypto from 'node:crypto';
import http from 'node:http';
import { spawnSync } from 'node:child_process';

const SOURCE_BASE='3f85f2ecdea8824ba45c2d7240c96ec03c56ae0d';
const A_HEAD='5a26a310878b191f39de521a8b5ae01a2d10b224';
const B_HEAD='3c45cee139b2532f058a891715539f74394dfcb1';
const TREE='c96fe037602801379231b7b5c7f3b4808057e4df';
const BRANCH='companion/pr525-lived-b001-source';
const BASE=A_HEAD;
const HEAD=B_HEAD;
const REPOSITORY='VexGPT/VexLife-Prod';
const FOUR_B_HANDLE='heretic-4b';
const FOUR_B_MODEL_REF='model.vexlife-prod.heretic.qwen3.8-4b-distill.mlx4.d2f8f4028f20';
const SERVICE_ORIGIN='http://127.0.0.1:18210';
const SOURCE_PATH='/api/v1/source-currentness';
const RUNTIME_RECEIPT_RELATIVE='runtime/vexlife-prod/heretic-persistent-runtime-owner-receipt.json';
const CHRONOLOGY_RELATIVE='runtime/vexlife-prod/logs/chronology';
const TASK_REF='task.vexlife-prod.pr525.composed-warm-boot-lived.c003';
const ATTEMPT_PREFIX='attempt.vexlife-prod.pr525.composed-warm-boot-lived.c003.';
const RETURN_ROUTE_REF='github.pull.vexlife-prod.525';
const PREPARATION_FACT='PROMPT_PREPARATION_OBSERVED';
const SEND_FACT='SEND_ACCEPTED';
const STAGE_FACT='PROVIDER_STAGE_OBSERVED';
const TURN_COMPLETED_FACT='TURN_COMPLETED';
const TURN_FAILED_FACT='TURN_FAILED';

const RESULT_DIR=process.env.VEXLOCALBRIDGE_RESULT_DIR;
const PAYLOAD_ROOT=process.env.VEXLOCALBRIDGE_PAYLOAD_ROOT || process.cwd();
const HOME=path.join(os.homedir(),'.vexlife');
const RESULT_FILE=RESULT_DIR && path.join(RESULT_DIR,'payload-result.json');
const WORK_ROOT=RESULT_DIR && path.join(RESULT_DIR,'lived-work');

const effects={
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
};

const beforeState={
  candidate:{sourceBase:SOURCE_BASE,aHead:A_HEAD,bHead:B_HEAD,tree:TREE,branch:BRANCH},
};
const afterState={};
const completedStepReceipts=[];
const candidateFindings=[];
const harnessFindings=[];
const environmentFindings=[];
const unknownFindings=[];

class LivedStop extends Error {
  constructor(category,code){
    super(code);
    this.category=category;
    this.code=code;
  }
}
const stop=(category,code)=>{ throw new LivedStop(category,code); };
const sleep=(ms)=>new Promise(resolve=>setTimeout(resolve,ms));

function sha256Bytes(bytes){
  return crypto.createHash('sha256').update(bytes).digest('hex');
}
function sha256File(file){
  return sha256Bytes(fs.readFileSync(file));
}
function aggregateTree(root){
  if(!fs.existsSync(root)) return sha256Bytes(Buffer.from('ABSENT','utf8'));
  const entries=[];
  const walk=(dir,relative)=>{
    const children=fs.readdirSync(dir,{withFileTypes:true}).sort((a,b)=>a.name.localeCompare(b.name));
    for(const child of children){
      const absolute=path.join(dir,child.name);
      const rel=relative?path.posix.join(relative,child.name):child.name;
      const stat=fs.lstatSync(absolute);
      if(stat.isSymbolicLink()) stop('environment','PROTECTED_TREE_SYMLINK_REJECTED');
      if(stat.isDirectory()) walk(absolute,rel);
      else if(stat.isFile()) entries.push(rel+'\0'+sha256File(absolute));
    }
  };
  walk(root,'');
  return sha256Bytes(Buffer.from(entries.join('\n'),'utf8'));
}
function findExe(candidates,label){
  for(const candidate of candidates){
    try{
      fs.accessSync(candidate,fs.constants.X_OK);
      if(fs.statSync(candidate).isFile()) return candidate;
    }catch{}
  }
  stop('environment',label+'_UNAVAILABLE');
}
function run(exe,args,{cwd=WORK_ROOT,env=process.env,timeout=120000,label='COMMAND'}={}){
  const result=spawnSync(exe,args,{
    cwd,
    env,
    encoding:'utf8',
    timeout,
    maxBuffer:64*1024*1024,
    stdio:['ignore','pipe','pipe'],
  });
  if(result.error) stop('environment',label+'_SPAWN_FAILED');
  if(result.signal) stop('environment',label+'_SIGNALLED');
  if(result.status!==0) {
    afterState.lastCommand={
      label,
      exitCode:result.status,
      stdoutTail:String(result.stdout||'').slice(-3000),
      stderrTail:String(result.stderr||'').slice(-3000),
    };
    stop('environment',label+'_FAILED');
  }
  return result;
}
function processAlive(pid){
  try{ process.kill(pid,0); return true; }catch{return false;}
}
async function getJson(url,timeoutMs=5000){
  return await new Promise((resolve,reject)=>{
    const request=http.get(url,{headers:{accept:'application/json'}},response=>{
      let body='';
      response.setEncoding('utf8');
      response.on('data',chunk=>{
        body+=chunk;
        if(body.length>4*1024*1024){
          request.destroy(new Error('HTTP_BODY_TOO_LARGE'));
        }
      });
      response.on('end',()=>{
        if(response.statusCode<200||response.statusCode>299){
          reject(new Error('HTTP_'+response.statusCode));
          return;
        }
        try{resolve(JSON.parse(body));}catch(error){reject(error);}
      });
    });
    request.setTimeout(timeoutMs,()=>request.destroy(new Error('HTTP_TIMEOUT')));
    request.on('error',reject);
  });
}
async function waitJson(url,predicate,timeoutMs,label){
  const deadline=Date.now()+timeoutMs;
  let last=null;
  while(Date.now()<deadline){
    try{
      const value=await getJson(url);
      last=value;
      if(predicate(value)) return value;
    }catch(error){
      last={error:error.message};
    }
    await sleep(500);
  }
  afterState[label+'LastObservation']=last;
  stop('environment',label+'_TIMEOUT');
}
function readRuntimeReceipt(){
  const file=path.join(HOME,RUNTIME_RECEIPT_RELATIVE);
  if(!fs.existsSync(file)) stop('environment','RUNTIME_RECEIPT_MISSING');
  let receipt;
  try{ receipt=JSON.parse(fs.readFileSync(file,'utf8')); }
  catch{ stop('environment','RUNTIME_RECEIPT_INVALID'); }
  if(typeof receipt.receiptRef!=='string'||!receipt.runtime||!Number.isInteger(receipt.runtime.pid)){
    stop('environment','RUNTIME_RECEIPT_SHAPE_INVALID');
  }
  return {
    receiptRef:receipt.receiptRef,
    pid:receipt.runtime.pid,
    processGroupId:receipt.runtime.processGroupId??null,
    modelRef:receipt.modelRef??null,
    modelProfileRef:receipt.modelProfileRef??null,
    promptCacheEntries:(()=>{
      const args=Array.isArray(receipt.runtime.args)?receipt.runtime.args:[];
      const i=args.indexOf('--prompt-cache-size');
      return i>=0&&i+1<args.length?Number(args[i+1]):null;
    })(),
  };
}
function chronologyRecords(servicePid){
  const dir=path.join(HOME,CHRONOLOGY_RELATIVE);
  if(!fs.existsSync(dir)) return [];
  const files=fs.readdirSync(dir,{withFileTypes:true})
    .filter(entry=>entry.isFile()&&entry.name.startsWith('chronology')&&entry.name.endsWith('.jsonl'))
    .map(entry=>path.join(dir,entry.name))
    .sort();
  const records=[];
  for(const file of files){
    const lines=fs.readFileSync(file,'utf8').split(/\r?\n/);
    for(const line of lines){
      if(!line.trim()) continue;
      try{
        const record=JSON.parse(line);
        if(record.sourceSha===HEAD&&record.processPid===servicePid&&Number.isInteger(record.sequence)){
          records.push(record);
        }
      }catch{}
    }
  }
  records.sort((a,b)=>a.sequence-b.sequence);
  return records;
}
async function waitForRecord(servicePid,predicate,afterSequence,timeoutMs,label){
  const deadline=Date.now()+timeoutMs;
  while(Date.now()<deadline){
    const records=chronologyRecords(servicePid).filter(record=>record.sequence>afterSequence);
    const found=records.find(predicate);
    if(found) return found;
    await sleep(500);
  }
  afterState[label+'ObservedFacts']=chronologyRecords(servicePid)
    .filter(record=>record.sequence>afterSequence)
    .slice(-30)
    .map(record=>({sequence:record.sequence,fact:record.fact,state:record.refs?.state??null,reason:record.refs?.reason??null}));
  stop('environment',label+'_TIMEOUT');
}
async function waitPreparationTerminal(servicePid,afterSequence,timeoutMs){
  const deadline=Date.now()+timeoutMs;
  const terminal=new Set(['READY_REUSABLE','HELD','FAILED','INVALIDATED']);
  while(Date.now()<deadline){
    const records=chronologyRecords(servicePid)
      .filter(record=>record.sequence>afterSequence&&record.fact===PREPARATION_FACT);
    const candidate=records.find(record=>terminal.has(record.refs?.state));
    if(candidate) return candidate;
    await sleep(300);
  }
  stop('candidate','PROMPT_PREPARATION_TERMINAL_NOT_OBSERVED');
}
function compactPreparation(record){
  return {
    sequence:record.sequence,
    state:record.refs?.state??null,
    reason:record.refs?.reason??null,
    failureClass:record.refs?.failureClass??null,
    runtimeReceiptRef:record.refs?.runtimeReceiptRef??null,
    modelRef:record.refs?.modelRef??null,
    prefixSha:record.refs?.prefixSha??null,
    providerTotalMs:record.refs?.providerTotalMs?Number(record.refs.providerTotalMs):null,
  };
}
function metricsFrom(record){
  const refs=record.refs||{};
  const num=(key)=>{
    if(refs[key]===undefined) return null;
    const value=Number(refs[key]);
    return Number.isFinite(value)?value:null;
  };
  return {
    sequence:record.sequence,
    runtimeReceiptRef:refs.runtimeReceiptRef??null,
    stop:refs.stop??null,
    promptTokens:num('promptTokens'),
    cachedPromptTokens:num('cachedPromptTokens'),
    prefillTokens:num('prefillTokens'),
    completionTokens:num('completionTokens'),
    firstTokenMs:num('firstTokenMs'),
    firstAnswerMs:num('firstAnswerMs'),
    totalMs:num('totalMs'),
  };
}
function addFinding(category,code){
  if(category==='candidate') candidateFindings.push(code);
  else if(category==='harness') harnessFindings.push(code);
  else if(category==='environment') environmentFindings.push(code);
  else unknownFindings.push(code);
}
function writeResult(disposition,stopReasonOrNull=null){
  fs.mkdirSync(RESULT_DIR,{recursive:true});
  const value={
    schemaVersion:'vextreme.vexlocalbridge-mac-payload-result/v1',
    disposition,
    stopReasonOrNull,
    acceptedExitCodes:[0],
    candidateFindings:[...new Set(candidateFindings)],
    harnessFindings:[...new Set(harnessFindings)],
    environmentFindings:[...new Set(environmentFindings)],
    unknownFindings:[...new Set(unknownFindings)],
    effects,
    beforeState,
    afterState,
    completedStepReceipts,
  };
  fs.writeFileSync(RESULT_FILE,JSON.stringify(value,null,2)+'\n');
}

try{
  if(!RESULT_DIR||!PAYLOAD_ROOT) stop('harness','VEXLOCALBRIDGE_PAYLOAD_ENV_MISSING');
  fs.mkdirSync(WORK_ROOT,{recursive:true});

  const taskRef=process.env.VEXLOCALBRIDGE_TASK_REF||null;
  const attemptRef=process.env.VEXLOCALBRIDGE_ATTEMPT_REF||null;
  const returnRouteRef=process.env.VEXLOCALBRIDGE_RETURN_ROUTE_REF||null;
  const policy=JSON.parse(process.env.VEXLOCALBRIDGE_VEXLIFE_PROD_HOST_EFFECT_POLICY_JSON||'null');
  const allowed=JSON.parse(process.env.VEXLOCALBRIDGE_AUTHORIZED_EFFECT_REFS_JSON||'[]');
  const bindingPath=path.join(PAYLOAD_ROOT,'inputs','lived-binding.json');
  if(!fs.existsSync(bindingPath)) stop('harness','LIVED_BINDING_MISSING');
  const binding=JSON.parse(fs.readFileSync(bindingPath,'utf8'));

  if(taskRef!==TASK_REF||
     typeof attemptRef!=='string'||!attemptRef.startsWith(ATTEMPT_PREFIX)||
     returnRouteRef!==RETURN_ROUTE_REF){
    stop('harness','TASK_ENV_BINDING_MISMATCH');
  }
  if(binding?.candidate?.sourceBase!==SOURCE_BASE||
     binding?.candidate?.aHead!==A_HEAD||
     binding?.candidate?.bHead!==B_HEAD||
     binding?.candidate?.tree!==TREE||
     binding?.candidate?.branch!==BRANCH){
    stop('harness','IMMUTABLE_LIVED_BINDING_MISMATCH');
  }
  if(policy?.repository!==REPOSITORY||
     policy?.candidate?.head!==B_HEAD||
     policy?.candidate?.tree!==TREE||
     policy?.hostProfileRef!=='macos-victor'||
     policy?.repositoryExecutionProfileRef!=='profile.repository.vexlife-prod.macos-v1'){
    stop('harness','PRODUCT_HOST_EFFECT_POLICY_BINDING_MISMATCH');
  }
  for(const effectRef of [
    'effect.vexlocalbridge.vexlife-prod.product-lifecycle-proof/v1',
    'effect.vexlocalbridge.vexlife-prod.build-dependency-fetch/v1',
  ]){
    if(!allowed.includes(effectRef)) stop('harness','REQUIRED_HOST_EFFECT_REF_MISSING');
  }
  if(policy.buildDependencyFetchAuthorized!==true||
     policy.productDataPolicy?.conversationMutationAllowed!==false||
     policy.productDataPolicy?.memoryMutationAllowed!==false||
     policy.productDataPolicy?.modelAcquisitionAllowed!==false||
     policy.productDataPolicy?.modelSwitchForProofAllowed!==false||
     policy.productDataPolicy?.runtimeLifecycleMutationAllowed!==true||
     policy.productDataPolicy?.serviceLifetimeMutationAllowed!==true){
    stop('harness','PRODUCT_HOST_EFFECT_POLICY_WIDENED_OR_INCOMPLETE');
  }
  if(!fs.existsSync(HOME)||!fs.statSync(HOME).isDirectory()){
    stop('environment','DEFAULT_VEXLIFE_HOME_MISSING');
  }

  const gh=findExe(['/opt/homebrew/bin/gh','/usr/local/bin/gh'],'GITHUB_CLI');
  const git=findExe(['/usr/bin/git'],'GIT');
  const bash=findExe(['/bin/bash'],'BASH');
  const open=findExe(['/usr/bin/open'],'OPEN');
  run(gh,['auth','status','--hostname','github.com'],{label:'GITHUB_AUTH',timeout:30000});

  const repoRoot=path.join(WORK_ROOT,'VexLife-Prod');
  effects.providerOrNetworkEffectPerformed=true;
  run(gh,['repo','clone',REPOSITORY,repoRoot,'--','--filter=blob:none','--no-checkout'],{
    cwd:WORK_ROOT,label:'PRODUCT_CLONE',timeout:300000,
  });

  const observedTree=(ref,label)=>{
    run(git,['checkout','--quiet','--detach',ref],{cwd:repoRoot,label:label+'_CHECKOUT',timeout:60000});
    const head=run(git,['rev-parse','HEAD'],{cwd:repoRoot,label:label+'_HEAD'}).stdout.trim();
    const tree=run(git,['rev-parse','HEAD^{tree}'],{cwd:repoRoot,label:label+'_TREE'}).stdout.trim();
    if(head!==ref||tree!==TREE) stop('harness',label+'_SOURCE_IDENTITY_DRIFT');
    return {head,tree};
  };
  observedTree(A_HEAD,'A');
  const aAncestor=spawnSync(git,['merge-base','--is-ancestor',SOURCE_BASE,A_HEAD],{cwd:repoRoot});
  if(aAncestor.status!==0) stop('harness','SOURCE_BASE_NOT_ANCESTOR_OF_A');

  const javaHomes=[];
  const addJavaHome=(value)=>{
    if(value&&fs.existsSync(path.join(value,'bin','java'))&&fs.existsSync(path.join(value,'bin','javac'))&&!javaHomes.includes(value)){
      javaHomes.push(value);
    }
  };
  addJavaHome(process.env.JAVA_HOME);
  for(const version of ['21','17']){
    const jh=spawnSync('/usr/libexec/java_home',['-v',version],{encoding:'utf8',timeout:10000});
    if(jh.status===0) addJavaHome(String(jh.stdout||'').trim());
  }
  addJavaHome('/Applications/Android Studio.app/Contents/jbr/Contents/Home');
  addJavaHome('/Applications/Android Studio Preview.app/Contents/jbr/Contents/Home');
  let jdk=null;
  for(const home of javaHomes){
    const check=spawnSync(path.join(home,'bin','java'),['-version'],{encoding:'utf8',timeout:10000});
    if(check.status===0){jdk=home;break;}
  }
  if(!jdk) stop('environment','GRADLE_CAPABLE_JAVA_UNAVAILABLE');

  const gradleUserHome=path.join(WORK_ROOT,'gradle-home');
  fs.mkdirSync(gradleUserHome,{recursive:true});
  fs.writeFileSync(path.join(gradleUserHome,'gradle.properties'),
    'org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m\n'+
    'kotlin.daemon.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m\n');
  const gradleEnv={...process.env,JAVA_HOME:jdk,GRADLE_USER_HOME:gradleUserHome};
  const gradlew=path.join(repoRoot,'gradlew');
  if(!fs.existsSync(gradlew)) stop('harness','GRADLE_WRAPPER_MISSING');

  const buildAt=(ref,label)=>{
    observedTree(ref,label);
    run(bash,[
      gradlew,'--no-daemon','--max-workers=1',
      '-Dorg.gradle.jvmargs=-Xmx4g',
      '-Dkotlin.daemon.jvm.options=-Xmx4g',
      '-PvexlifeAppVersion=0.0.0',
      'archiveVexLifeProdMacApplication',
    ],{cwd:repoRoot,env:gradleEnv,label:label+'_MAC_APP_BUILD',timeout:2700000});
    const appRoot=path.join(repoRoot,'build','vexlife-prod-mac-application',ref);
    const app=path.join(appRoot,'VexLife.app');
    const receiptFile=path.join(appRoot,'VexLife-macOS-AppleSilicon.receipt.json');
    const setupLauncher=path.join(app,'Contents','Resources','private-service-image','bin','vexlife-prod-setup');
    for(const required of [app,receiptFile,setupLauncher]){
      if(!fs.existsSync(required)) stop('candidate',label+'_MAC_APP_ARTIFACT_MISSING');
    }
    const receipt=JSON.parse(fs.readFileSync(receiptFile,'utf8'));
    if(receipt.sourceSha!==ref) stop('candidate',label+'_MAC_APP_SOURCE_RECEIPT_MISMATCH');
    return {app,setupLauncher};
  };

  const aBuild=buildAt(A_HEAD,'A');
  const bBuild=buildAt(B_HEAD,'B');
  const bAncestor=spawnSync(git,['merge-base','--is-ancestor',A_HEAD,B_HEAD],{cwd:repoRoot});
  if(bAncestor.status!==0) stop('harness','A_NOT_ANCESTOR_OF_B');
  completedStepReceipts.push({step:'same-tree-a-b-builds',state:'PASS',aHead:A_HEAD,bHead:B_HEAD,tree:TREE});

  const parseJsonOutput=(result,label)=>{
    try{
      return JSON.parse(String(result.stdout||'').trim());
    }catch{
      afterState[label+'StdoutTail']=String(result.stdout||'').slice(-4000);
      afterState[label+'StderrTail']=String(result.stderr||'').slice(-4000);
      stop('harness',label+'_JSON_INVALID');
    }
  };
  const inspectFrontDoor=(launcher,source,label)=>
    parseJsonOutput(run(bash,[
      launcher,'--inspect-front-door',
      '--home',HOME,
      '--expected-source-sha',source,
    ],{cwd:repoRoot,label:label+'_FRONT_DOOR_INSPECT',timeout:120000}),label+'_FRONT_DOOR_INSPECT');
  const activateFrontDoor=(launcher,source,control,label)=>
    parseJsonOutput(run(bash,[
      launcher,'--activate-front-door-control',control,
      '--home',HOME,
      '--expected-source-sha',source,
    ],{cwd:repoRoot,label:label+'_FRONT_DOOR_'+control,timeout:900000}),label+'_FRONT_DOOR_'+control);

  // Before any runtime start, prove the Home already chose 4B. No model-selection effect is allowed.
  const modelProjection=parseJsonOutput(run(bash,[
    aBuild.setupLauncher,'--setup-model','--home',HOME,
  ],{cwd:repoRoot,label:'A_MODEL_SELECTION_OBSERVE',timeout:120000}),'A_MODEL_SELECTION_OBSERVE');
  afterState.modelSelectionBeforeStart={
    selected:modelProjection?.selected??null,
    chosen:modelProjection?.chosen??null,
    savedInHome:modelProjection?.savedInHome??null,
  };
  if(modelProjection?.selected!==FOUR_B_HANDLE||modelProjection?.savedInHome!==true){
    stop('environment','FOUR_B_NOT_ALREADY_SELECTED_IN_HOME');
  }

  // Recover/start A only through Product's lawful no-acquisition front-door controls.
  let aProjection=inspectFrontDoor(aBuild.setupLauncher,A_HEAD,'A');
  afterState.aFrontDoorInitial={
    semanticStateRef:aProjection?.semanticStateRef??null,
    currentServiceSourceSha:aProjection?.currentServiceSourceSha??null,
    currentServicePid:aProjection?.currentServicePid??null,
  };
  for(let i=0;i<4&&aProjection?.semanticStateRef!=='CURRENT';i++){
    const state=aProjection?.semanticStateRef;
    let control=null;
    if(state==='START_REQUIRED') control='START';
    else if(state==='UPDATE_AVAILABLE'||state==='REPAIR_REQUIRED'||state==='UPDATE_REPAIR_REQUIRED') control='CONVERGE_DESIRED';
    else if(state==='SERVICE_RECOVERY_REQUIRED') control='RECOVER_STALE_SERVICE';
    else stop('environment','A_FRONT_DOOR_STATE_'+String(state||'UNKNOWN'));
    effects.modelRuntimePerformed=true;
    const activated=activateFrontDoor(aBuild.setupLauncher,A_HEAD,control,'A');
    afterState['aActivation'+i]={
      controlRef:activated?.controlRef??null,
      semanticStateRef:activated?.semanticStateRef??null,
      manifestationRef:activated?.manifestationRef??null,
      failure:activated?.failure?.code??activated?.failure?.detail??null,
    };
    if(activated?.failure) stop('candidate','A_FRONT_DOOR_CONTROL_FAILED');
    aProjection=inspectFrontDoor(aBuild.setupLauncher,A_HEAD,'A');
  }
  if(aProjection?.semanticStateRef!=='CURRENT'||aProjection?.currentServiceSourceSha!==A_HEAD){
    stop('candidate','A_DID_NOT_CONVERGE_CURRENT');
  }

  const aSource=await waitJson(SERVICE_ORIGIN+SOURCE_PATH,value=>
    value?.sourceSha===A_HEAD&&Number.isInteger(value?.processPid)&&typeof value?.startupQualifiedRuntimeReceiptRef==='string',
    60000,'A_SOURCE_CURRENTNESS');
  const aRuntime=readRuntimeReceipt();
  if(aRuntime.modelRef!==FOUR_B_MODEL_REF){
    stop('environment','A_RUNTIME_NOT_EXACT_HERETIC_4B');
  }
  if(aRuntime.receiptRef!==aSource.startupQualifiedRuntimeReceiptRef||!processAlive(aRuntime.pid)){
    stop('candidate','A_RUNTIME_NOT_EXACT_LIVE');
  }
  afterState.aCurrent={
    sourceSha:aSource.sourceSha,
    servicePid:aSource.processPid,
    runtime:aRuntime,
  };
  completedStepReceipts.push({step:'a-current-selected-4b',state:'PASS'});

  const protectedBeforeRotation={
    conversations:aggregateTree(path.join(HOME,'conversations')),
    memory:aggregateTree(path.join(HOME,'memory')),
  };
  beforeState.protectedBeforeRotation=protectedBeforeRotation;

  // Same-tree B forces the real service source rotation while requiring runtime reuse.
  effects.modelRuntimePerformed=true;
  const bSetup=run(bash,[
    bBuild.setupLauncher,
    '--home',HOME,
    '--expected-source-sha',B_HEAD,
    '--reuse-existing',
  ],{cwd:repoRoot,label:'B_SETUP_REUSE_EXISTING',timeout:900000});
  const bSetupText=String(bSetup.stdout||'')+'\n'+String(bSetup.stderr||'');
  const readyLine=bSetupText.split(/\r?\n/).find(line=>line.startsWith('VEXLIFE_PROD_SETUP_READY\t'));
  if(!readyLine){
    afterState.bSetupTail=bSetupText.slice(-5000);
    stop('candidate','B_SETUP_READY_NOT_OBSERVED');
  }
  const readyParts=readyLine.split('\t');
  if(readyParts.length<7||readyParts[1]!==B_HEAD) stop('harness','B_SETUP_READY_IDENTITY_MISMATCH');
  const bRuntimeReceiptRef=readyParts[4];
  const bServicePid=Number(readyParts[5]);
  const bOrigin=readyParts[6];
  if(!Number.isInteger(bServicePid)||bOrigin!==SERVICE_ORIGIN) stop('harness','B_SETUP_READY_SHAPE_INVALID');

  const bSource=await waitJson(SERVICE_ORIGIN+SOURCE_PATH,value=>
    value?.sourceSha===B_HEAD&&
    value?.processPid===bServicePid&&
    value?.startupQualifiedRuntimeReceiptRef===bRuntimeReceiptRef,
    60000,'B_SOURCE_CURRENTNESS');
  const bRuntime=readRuntimeReceipt();
  if(bRuntime.modelRef!==FOUR_B_MODEL_REF){
    stop('environment','B_RUNTIME_NOT_EXACT_HERETIC_4B');
  }
  afterState.bCurrent={
    sourceSha:bSource.sourceSha,
    servicePid:bSource.processPid,
    runtime:bRuntime,
  };
  if(bRuntime.receiptRef!==aRuntime.receiptRef||bRuntime.pid!==aRuntime.pid||!processAlive(bRuntime.pid)){
    stop('candidate','A_TO_B_RUNTIME_HANDOFF_NOT_PRESERVED');
  }
  if(bRuntimeReceiptRef!==aRuntime.receiptRef){
    stop('candidate','B_STARTUP_RUNTIME_RECEIPT_CHANGED');
  }

  const protectedAfterRotation={
    conversations:aggregateTree(path.join(HOME,'conversations')),
    memory:aggregateTree(path.join(HOME,'memory')),
  };
  afterState.protectedAfterRotation=protectedAfterRotation;
  if(protectedAfterRotation.conversations!==protectedBeforeRotation.conversations){
    stop('candidate','SOURCE_ROTATION_MUTATED_CONVERSATION');
  }
  if(protectedAfterRotation.memory!==protectedBeforeRotation.memory){
    stop('candidate','SOURCE_ROTATION_MUTATED_MEMORY');
  }
  completedStepReceipts.push({step:'a-to-b-runtime-handoff',state:'PASS',sameReceipt:true,samePid:true});

  const preparationReady=await waitPreparationTerminal(bServicePid,0,90000);
  afterState.preparationReady=compactPreparation(preparationReady);
  if(preparationReady.refs?.state!=='READY_REUSABLE'){
    stop('candidate','B_PROMPT_PREPARATION_'+String(preparationReady.refs?.state||'UNKNOWN'));
  }
  if(preparationReady.refs?.runtimeReceiptRef!==bRuntime.receiptRef){
    stop('candidate','B_PREPARATION_RUNTIME_BINDING_MISMATCH');
  }
  completedStepReceipts.push({step:'b-prompt-preparation',state:'READY_REUSABLE'});

  // Human Send remains the one protected content effect; the harness never synthesizes it.
  run(open,[SERVICE_ORIGIN+'/'],{label:'OPEN_READY_B',timeout:30000});
  completedStepReceipts.push({step:'human-send-gate-opened',state:'READY_REUSABLE'});

  const send=await waitForRecord(bServicePid,
    record=>record.fact===SEND_FACT,
    preparationReady.sequence,
    15*60*1000,
    'HUMAN_SEND');
  const turnRef=send.refs?.turnRef;
  if(typeof turnRef!=='string'||!turnRef) stop('harness','SEND_ACCEPTED_TURN_REF_MISSING');
  afterState.humanSend={sequence:send.sequence,turnRef};

  const turnTerminal=await waitForRecord(bServicePid,
    record=>(record.fact===TURN_COMPLETED_FACT||record.fact===TURN_FAILED_FACT)&&record.refs?.turnRef===turnRef,
    send.sequence,
    20*60*1000,
    'TURN_TERMINAL');
  const postSendRecords=chronologyRecords(bServicePid).filter(record=>record.sequence>send.sequence&&record.sequence<=turnTerminal.sequence);
  const preparationAfter=postSendRecords.find(record=>
    record.fact===PREPARATION_FACT&&['CONSUMED','INVALIDATED','HELD','FAILED'].includes(record.refs?.state));
  const providerStage=postSendRecords.find(record=>record.fact===STAGE_FACT);
  afterState.preparationAfterSend=preparationAfter?compactPreparation(preparationAfter):null;
  afterState.turnTerminal={sequence:turnTerminal.sequence,fact:turnTerminal.fact,turnRef};
  if(!providerStage) stop('candidate','FIRST_PROVIDER_STAGE_NOT_OBSERVED');
  const metrics=metricsFrom(providerStage);
  afterState.firstProviderStage=metrics;

  const runtimeAfter=readRuntimeReceipt();
  afterState.runtimeAfter=runtimeAfter;
  const sourceAfter=await waitJson(SERVICE_ORIGIN+SOURCE_PATH,value=>value?.sourceSha===B_HEAD,30000,'SOURCE_AFTER_SEND');
  afterState.serviceAfterSend={
    sourceSha:sourceAfter.sourceSha,
    processPid:sourceAfter.processPid,
    runtimeReceiptRef:sourceAfter.startupQualifiedRuntimeReceiptRef,
    conversationHead:sourceAfter.currentConversationHeadSha256??null,
  };

  if(sourceAfter.processPid!==bServicePid) candidateFindings.push('B_SERVICE_PROCESS_CHANGED_DURING_SEND');
  if(runtimeAfter.receiptRef!==aRuntime.receiptRef||runtimeAfter.pid!==aRuntime.pid||!processAlive(runtimeAfter.pid)){
    candidateFindings.push('MLX_RUNTIME_CHANGED_AFTER_A_TO_B_HANDOFF');
  }
  if(metrics.runtimeReceiptRef!==aRuntime.receiptRef){
    candidateFindings.push('PROVIDER_STAGE_RUNTIME_BINDING_MISMATCH');
  }
  if(turnTerminal.fact!==TURN_COMPLETED_FACT){
    candidateFindings.push('HUMAN_SEND_DID_NOT_COMPLETE');
  }
  if(preparationAfter?.refs?.state!=='CONSUMED'){
    candidateFindings.push('PREPARATION_NOT_CONSUMED_BY_REAL_SEND');
  }
  if(metrics.promptTokens===null||metrics.cachedPromptTokens===null||metrics.prefillTokens===null){
    candidateFindings.push('PROVIDER_CACHE_METRICS_MISSING');
  }else{
    if(metrics.cachedPromptTokens<=0) candidateFindings.push('CACHE_REUSE_NOT_OBSERVED');
    if(metrics.prefillTokens>=metrics.promptTokens) candidateFindings.push('FULL_PROMPT_PREFILL_NOT_REDUCED');
  }

  completedStepReceipts.push({
    step:'composed-lived-cache-observation',
    state:candidateFindings.length===0?'PASS':'SAFE_FAILURE',
  });
  writeResult(candidateFindings.length===0?'PASS':'SAFE_FAILURE',
    candidateFindings.length===0?null:'COMPOSED_LIVED_CACHE_ACCEPTANCE_NOT_MET');
}catch(error){
  if(error instanceof LivedStop){
    addFinding(error.category,error.code);
    writeResult('SAFE_FAILURE',error.code);
  }else{
    harnessFindings.push('LIVED_PAYLOAD_UNCAUGHT_'+String(error?.name||'ERROR'));
    afterState.uncaughtClass=String(error?.name||'Error');
    afterState.uncaughtMessage=String(error?.message||'');
    writeResult('SAFE_FAILURE','LIVED_PAYLOAD_UNCAUGHT');
  }
}
