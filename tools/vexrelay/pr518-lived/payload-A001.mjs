import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import crypto from 'node:crypto';
import http from 'node:http';
import { spawnSync } from 'node:child_process';

const BASE='0566b1eed876a28c2fcc6586743ae1f4af7eaecd';
const HEAD='3f85f2ecdea8824ba45c2d7240c96ec03c56ae0d';
const TREE='0ea9001c3d3a0ef0a87ff755a57be33c91ffdc1b';
const BRANCH='VXG-112226-chatgpt-initial-prompt-preparation-b001';
const REPOSITORY='VexGPT/VexLife-Prod';
const FOUR_B_HANDLE='heretic-4b';
const SERVICE_ORIGIN='http://127.0.0.1:18210';
const SOURCE_PATH='/api/v1/source-currentness';
const MODELS_PATH='/api/v1/models';
const RUNTIME_RECEIPT_RELATIVE='runtime/vexlife-prod/heretic-persistent-runtime-owner-receipt.json';
const CHRONOLOGY_RELATIVE='runtime/vexlife-prod/logs/chronology';
const PREPARATION_FACT='PROMPT_PREPARATION_OBSERVED';
const SEND_FACT='SEND_ACCEPTED';
const STAGE_FACT='PROVIDER_STAGE_OBSERVED';
const TURN_COMPLETED_FACT='TURN_COMPLETED';
const TURN_FAILED_FACT='TURN_FAILED';

const RESULT_DIR=process.env.VEXLOCALBRIDGE_RESULT_DIR;
const PAYLOAD_ROOT=process.env.VEXLOCALBRIDGE_PAYLOAD_ROOT;
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
  candidate:{base:BASE,head:HEAD,tree:TREE,branch:BRANCH},
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

  const task=JSON.parse(process.env.VEXLOCALBRIDGE_TASK_JSON||'null');
  const policy=JSON.parse(process.env.VEXLOCALBRIDGE_VEXLIFE_PROD_HOST_EFFECT_POLICY_JSON||'null');
  const allowed=JSON.parse(process.env.VEXLOCALBRIDGE_ALLOWED_EFFECT_REFS_JSON||'[]');
  const bindingPath=path.join(PAYLOAD_ROOT,'inputs','lived-binding.json');
  if(!fs.existsSync(bindingPath)) stop('harness','LIVED_BINDING_MISSING');
  const binding=JSON.parse(fs.readFileSync(bindingPath,'utf8'));

  if(task?.repositoryRefOrNull!==REPOSITORY||
     task?.baseRefOrNull!==BASE||
     task?.headRefOrNull!==HEAD||
     task?.treeRefOrNull!==TREE||
     task?.branchRefOrNull!==BRANCH){
    stop('harness','TASK_CANDIDATE_BINDING_MISMATCH');
  }
  if(binding?.candidate?.base!==BASE||binding?.candidate?.head!==HEAD||
     binding?.candidate?.tree!==TREE||binding?.candidate?.branch!==BRANCH){
    stop('harness','IMMUTABLE_LIVED_BINDING_MISMATCH');
  }
  if(policy?.repository!==REPOSITORY||
     policy?.candidate?.head!==HEAD||
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

  beforeState.protectedAggregate={
    conversations:aggregateTree(path.join(HOME,'conversations')),
    memory:aggregateTree(path.join(HOME,'memory')),
  };
  completedStepReceipts.push({step:'baseline-protected-data',state:'PASS'});

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
  run(git,['checkout','--quiet','--detach',HEAD],{cwd:repoRoot,label:'PRODUCT_CHECKOUT',timeout:60000});
  const observedHead=run(git,['rev-parse','HEAD'],{cwd:repoRoot,label:'PRODUCT_HEAD'}).stdout.trim();
  const observedTree=run(git,['rev-parse','HEAD^{tree}'],{cwd:repoRoot,label:'PRODUCT_TREE'}).stdout.trim();
  if(observedHead!==HEAD||observedTree!==TREE) stop('harness','PRODUCT_SOURCE_IDENTITY_DRIFT');
  const ancestor=spawnSync(git,['merge-base','--is-ancestor',BASE,HEAD],{cwd:repoRoot});
  if(ancestor.status!==0) stop('harness','PRODUCT_BASE_NOT_ANCESTOR');
  completedStepReceipts.push({step:'exact-candidate-source',state:'PASS'});

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
  run(bash,[
    gradlew,'--no-daemon','--max-workers=1',
    '-Dorg.gradle.jvmargs=-Xmx4g',
    '-Dkotlin.daemon.jvm.options=-Xmx4g',
    '-PvexlifeAppVersion=0.0.0',
    'archiveVexLifeProdMacApplication',
  ],{cwd:repoRoot,env:gradleEnv,label:'MAC_APP_BUILD',timeout:2700000});
  completedStepReceipts.push({step:'exact-candidate-app-build',state:'PASS'});

  const appRoot=path.join(repoRoot,'build','vexlife-prod-mac-application',HEAD);
  const app=path.join(appRoot,'VexLife.app');
  const receiptFile=path.join(appRoot,'VexLife-macOS-AppleSilicon.receipt.json');
  const setupLauncher=path.join(app,'Contents','Resources','private-service-image','bin','vexlife-prod-setup');
  for(const required of [app,receiptFile,setupLauncher]){
    if(!fs.existsSync(required)) stop('candidate','MAC_APP_ARTIFACT_MISSING');
  }
  const appReceipt=JSON.parse(fs.readFileSync(receiptFile,'utf8'));
  if(appReceipt.sourceSha!==HEAD) stop('candidate','MAC_APP_SOURCE_RECEIPT_MISMATCH');

  effects.modelRuntimePerformed=true;
  const setup=run(bash,[
    setupLauncher,
    '--home',HOME,
    '--expected-source-sha',HEAD,
    '--reuse-existing',
  ],{cwd:repoRoot,label:'CANDIDATE_SETUP_REUSE_EXISTING',timeout:600000});
  const setupText=String(setup.stdout||'')+'\n'+String(setup.stderr||'');
  const readyLine=setupText.split(/\r?\n/).find(line=>line.startsWith('VEXLIFE_PROD_SETUP_READY\t'));
  if(!readyLine){
    afterState.setupTail=setupText.slice(-4000);
    stop('environment','CANDIDATE_SETUP_READY_NOT_OBSERVED');
  }
  const readyParts=readyLine.split('\t');
  if(readyParts.length<7||readyParts[1]!==HEAD) stop('harness','CANDIDATE_SETUP_READY_IDENTITY_MISMATCH');
  const setupRuntimeReceiptRef=readyParts[4];
  const setupServicePid=Number(readyParts[5]);
  const setupOrigin=readyParts[6];
  if(!Number.isInteger(setupServicePid)||setupOrigin!==SERVICE_ORIGIN) stop('harness','CANDIDATE_SETUP_READY_SHAPE_INVALID');

  const sourceCurrent=await waitJson(SERVICE_ORIGIN+SOURCE_PATH,value=>
    value?.sourceSha===HEAD&&
    value?.processPid===setupServicePid&&
    value?.startupQualifiedRuntimeReceiptRef===setupRuntimeReceiptRef,
    60000,'SOURCE_CURRENTNESS');
  const models=await waitJson(SERVICE_ORIGIN+MODELS_PATH,value=>
    typeof value?.servingHandle==='string',
    30000,'MODELS_CURRENTNESS');
  afterState.modelRelationship={
    selectedHandle:models.selectedHandle??null,
    servingHandle:models.servingHandle??null,
    switchPending:models.switchPending??null,
  };
  if(models.selectedHandle!==FOUR_B_HANDLE||models.servingHandle!==FOUR_B_HANDLE||models.switchPending===true){
    stop('environment','LIVED_QWEN4B_NOT_SELECTED_AND_SERVING');
  }
  const row=Array.isArray(models.models)?models.models.find(item=>item.handle===FOUR_B_HANDLE):null;
  afterState.modelRelationship.promptCacheEntries=row?.runtimeConfiguration?.promptCacheEntries??null;

  const runtimeBefore=readRuntimeReceipt();
  if(runtimeBefore.receiptRef!==setupRuntimeReceiptRef||!processAlive(runtimeBefore.pid)){
    stop('environment','EXACT_RUNTIME_NOT_LIVE_BEFORE_PREPARATION');
  }
  afterState.runtimeBefore=runtimeBefore;
  afterState.serviceBeforeSend={
    sourceSha:sourceCurrent.sourceSha,
    processPid:sourceCurrent.processPid,
    runtimeReceiptRef:sourceCurrent.startupQualifiedRuntimeReceiptRef,
    conversationHead:sourceCurrent.currentConversationHeadSha256??null,
  };
  completedStepReceipts.push({step:'exact-candidate-service-runtime',state:'PASS'});

  const preparationReady=await waitPreparationTerminal(setupServicePid,0,90000);
  afterState.preparationReady=compactPreparation(preparationReady);
  if(preparationReady.refs?.state!=='READY_REUSABLE'){
    stop('candidate','PROMPT_PREPARATION_'+String(preparationReady.refs?.state||'UNKNOWN'));
  }
  if(preparationReady.refs?.runtimeReceiptRef!==setupRuntimeReceiptRef){
    stop('candidate','PREPARATION_RUNTIME_BINDING_MISMATCH');
  }

  const protectedAtReady={
    conversations:aggregateTree(path.join(HOME,'conversations')),
    memory:aggregateTree(path.join(HOME,'memory')),
  };
  afterState.protectedAggregateAtReady=protectedAtReady;
  if(protectedAtReady.conversations!==beforeState.protectedAggregate.conversations){
    stop('candidate','PREPARATION_MUTATED_CONVERSATION_BEFORE_SEND');
  }
  if(protectedAtReady.memory!==beforeState.protectedAggregate.memory){
    stop('candidate','PREPARATION_MUTATED_MEMORY_BEFORE_SEND');
  }
  completedStepReceipts.push({step:'pre-send-protected-data-unchanged',state:'PASS'});

  // The browser opening is the one human-visible gate: only now should Victor send one ordinary message.
  run(open,[SERVICE_ORIGIN+'/'],{label:'OPEN_READY_CANDIDATE',timeout:30000});
  completedStepReceipts.push({step:'human-send-gate-opened',state:'READY_REUSABLE'});

  const send=await waitForRecord(setupServicePid,
    record=>record.fact===SEND_FACT,
    preparationReady.sequence,
    15*60*1000,
    'HUMAN_SEND');
  const turnRef=send.refs?.turnRef;
  if(typeof turnRef!=='string'||!turnRef) stop('harness','SEND_ACCEPTED_TURN_REF_MISSING');
  afterState.humanSend={sequence:send.sequence,turnRef};

  // Observe the same turn to terminality. The harness never writes Conversation itself.
  const turnTerminal=await waitForRecord(setupServicePid,
    record=>(record.fact===TURN_COMPLETED_FACT||record.fact===TURN_FAILED_FACT)&&record.refs?.turnRef===turnRef,
    send.sequence,
    20*60*1000,
    'TURN_TERMINAL');
  const postSendRecords=chronologyRecords(setupServicePid).filter(record=>record.sequence>send.sequence&&record.sequence<=turnTerminal.sequence);
  const preparationAfter=postSendRecords.find(record=>
    record.fact===PREPARATION_FACT&&['CONSUMED','INVALIDATED','HELD','FAILED'].includes(record.refs?.state));
  const providerStage=postSendRecords.find(record=>record.fact===STAGE_FACT);
  afterState.preparationAfterSend=preparationAfter?compactPreparation(preparationAfter):null;
  afterState.turnTerminal={sequence:turnTerminal.sequence,fact:turnTerminal.fact,turnRef};
  if(!providerStage) stop('candidate','FIRST_PROVIDER_STAGE_NOT_OBSERVED');
  const metrics=metricsFrom(providerStage);
  afterState.firstProviderStage=metrics;

  const sourceAfter=await waitJson(SERVICE_ORIGIN+SOURCE_PATH,value=>value?.sourceSha===HEAD,30000,'SOURCE_AFTER_SEND');
  const runtimeAfter=readRuntimeReceipt();
  afterState.runtimeAfter=runtimeAfter;
  afterState.serviceAfterSend={
    sourceSha:sourceAfter.sourceSha,
    processPid:sourceAfter.processPid,
    runtimeReceiptRef:sourceAfter.startupQualifiedRuntimeReceiptRef,
    conversationHead:sourceAfter.currentConversationHeadSha256??null,
  };

  if(sourceAfter.processPid!==setupServicePid) candidateFindings.push('SERVICE_PROCESS_CHANGED_DURING_LIVED_PROOF');
  if(runtimeAfter.receiptRef!==runtimeBefore.receiptRef||runtimeAfter.pid!==runtimeBefore.pid||!processAlive(runtimeAfter.pid)){
    candidateFindings.push('MLX_RUNTIME_CHANGED_DURING_LIVED_PROOF');
  }
  if(metrics.runtimeReceiptRef!==runtimeBefore.receiptRef){
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
    step:'lived-cache-observation',
    state:candidateFindings.length===0?'PASS':'SAFE_FAILURE',
  });

  writeResult(candidateFindings.length===0?'PASS':'SAFE_FAILURE',
    candidateFindings.length===0?null:'LIVED_CACHE_ACCEPTANCE_NOT_MET');
}catch(error){
  if(error instanceof LivedStop){
    addFinding(error.category,error.code);
    writeResult('SAFE_FAILURE',error.code);
  }else{
    harnessFindings.push('LIVED_PAYLOAD_UNCAUGHT_'+String(error?.name||'ERROR'));
    afterState.uncaughtClass=String(error?.name||'Error');
    writeResult('SAFE_FAILURE','LIVED_PAYLOAD_UNCAUGHT');
  }
}
