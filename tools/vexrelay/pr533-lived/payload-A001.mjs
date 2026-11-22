import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import crypto from 'node:crypto';
import http from 'node:http';
import { spawnSync } from 'node:child_process';

const BASE='5a26a310878b191f39de521a8b5ae01a2d10b224';
const HEAD='3f124ed97ee74df0dffb49e5b368fbc78be1d57f';
const TREE='67f842dcababff034eeb61c3d401cf45c1c4bd00';
const BRANCH='VXG-112226-chatgpt-first-local-start-ui-d001';
const REPOSITORY='VexGPT/VexLife-Prod';
const FOUR_B_HANDLE='heretic-4b';
const FOUR_B_MODEL_REF='model.vexlife-prod.heretic.qwen3.8-4b-distill.mlx4.d2f8f4028f20';
const SERVICE_ORIGIN='http://127.0.0.1:18210';
const SOURCE_PATH='/api/v1/source-currentness';
const TURN_PATH='/api/v1/conversation/turn';
const RUNTIME_RECEIPT_RELATIVE='runtime/vexlife-prod/heretic-persistent-runtime-owner-receipt.json';
const TASK_REF='task.vexlife-prod.pr533.first-local-start-ui.lived.a001';
const ATTEMPT_PREFIX='attempt.vexlife-prod.pr533.first-local-start-ui.lived.a001.';
const RETURN_ROUTE_REF='github.pull.vexlife-prod.533';

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

const beforeState={candidate:{base:BASE,head:HEAD,tree:TREE,branch:BRANCH}};
const afterState={};
const completedStepReceipts=[];
const candidateFindings=[];
const harnessFindings=[];
const environmentFindings=[];
const unknownFindings=[];

class LivedStop extends Error {
  constructor(category,code){ super(code); this.category=category; this.code=code; }
}
const stop=(category,code)=>{ throw new LivedStop(category,code); };
const sleep=(ms)=>new Promise(resolve=>setTimeout(resolve,ms));

function sha256Bytes(bytes){ return crypto.createHash('sha256').update(bytes).digest('hex'); }
function sha256File(file){ return sha256Bytes(fs.readFileSync(file)); }
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
  const result=spawnSync(exe,args,{cwd,env,encoding:'utf8',timeout,maxBuffer:64*1024*1024,stdio:['ignore','pipe','pipe']});
  if(result.error) stop('environment',label+'_SPAWN_FAILED');
  if(result.signal) stop('environment',label+'_SIGNALLED');
  if(result.status!==0){
    afterState.lastCommand={label,exitCode:result.status,stdoutTail:String(result.stdout||'').slice(-4000),stderrTail:String(result.stderr||'').slice(-4000)};
    stop('environment',label+'_FAILED');
  }
  return result;
}
function processAlive(pid){ try{ process.kill(pid,0); return true; }catch{return false;} }
async function getText(url,timeoutMs=5000){
  return await new Promise((resolve,reject)=>{
    const request=http.get(url,{headers:{accept:'*/*'}},response=>{
      let body='';
      response.setEncoding('utf8');
      response.on('data',chunk=>{
        body+=chunk;
        if(body.length>8*1024*1024) request.destroy(new Error('HTTP_BODY_TOO_LARGE'));
      });
      response.on('end',()=>{
        if(response.statusCode<200||response.statusCode>299){ reject(new Error('HTTP_'+response.statusCode)); return; }
        resolve(body);
      });
    });
    request.setTimeout(timeoutMs,()=>request.destroy(new Error('HTTP_TIMEOUT')));
    request.on('error',reject);
  });
}
async function getJson(url,timeoutMs=5000){
  const text=await getText(url,timeoutMs);
  return JSON.parse(text);
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
async function observeTurnOrNull(){
  try{ return await getJson(SERVICE_ORIGIN+TURN_PATH,2500); }catch{return null;}
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
    modelRef:receipt.modelRef??null,
    modelProfileRef:receipt.modelProfileRef??null,
  };
}
function parseJsonOutput(result,label){
  try{ return JSON.parse(String(result.stdout||'').trim()); }
  catch{
    afterState[label+'StdoutTail']=String(result.stdout||'').slice(-4000);
    afterState[label+'StderrTail']=String(result.stderr||'').slice(-4000);
    stop('harness',label+'_JSON_INVALID');
  }
}
function addFinding(category,code){
  if(category==='candidate') candidateFindings.push(code);
  else if(category==='harness') harnessFindings.push(code);
  else if(category==='environment') environmentFindings.push(code);
  else unknownFindings.push(code);
}
function writeResult(disposition,stopReasonOrNull=null){
  fs.mkdirSync(RESULT_DIR,{recursive:true});
  fs.writeFileSync(RESULT_FILE,JSON.stringify({
    schemaVersion:'vextreme.vexlocalbridge-mac-payload-result/v1',
    disposition,
    stopReasonOrNull,
    acceptedExitCodes:[0],
    candidateFindings:[...new Set(candidateFindings)],
    harnessFindings:[...new Set(harnessFindings)],
    environmentFindings:[...new Set(environmentFindings)],
    unknownFindings:[...new Set(unknownFindings)],
    effects,beforeState,afterState,completedStepReceipts,
  },null,2)+'\n');
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

  if(taskRef!==TASK_REF||typeof attemptRef!=='string'||!attemptRef.startsWith(ATTEMPT_PREFIX)||returnRouteRef!==RETURN_ROUTE_REF){
    stop('harness','TASK_ENV_BINDING_MISMATCH');
  }
  if(binding?.candidate?.base!==BASE||binding?.candidate?.head!==HEAD||binding?.candidate?.tree!==TREE||binding?.candidate?.branch!==BRANCH){
    stop('harness','IMMUTABLE_LIVED_BINDING_MISMATCH');
  }
  if(policy?.repository!==REPOSITORY||policy?.candidate?.head!==HEAD||policy?.candidate?.tree!==TREE||
     policy?.hostProfileRef!=='macos-victor'||policy?.repositoryExecutionProfileRef!=='profile.repository.vexlife-prod.macos-v1'){
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
  if(!fs.existsSync(HOME)||!fs.statSync(HOME).isDirectory()) stop('environment','DEFAULT_VEXLIFE_HOME_MISSING');

  const gh=findExe(['/opt/homebrew/bin/gh','/usr/local/bin/gh'],'GITHUB_CLI');
  const git=findExe(['/usr/bin/git'],'GIT');
  const bash=findExe(['/bin/bash'],'BASH');
  const open=findExe(['/usr/bin/open'],'OPEN');
  run(gh,['auth','status','--hostname','github.com'],{label:'GITHUB_AUTH',timeout:30000});

  const initialTurn=await observeTurnOrNull();
  beforeState.initialTurnProjection=initialTurn;
  if(initialTurn&&['ACTIVE','CANCELLING'].includes(String(initialTurn.state||''))){
    stop('environment','CONVERSATION_TURN_ACTIVE_BEFORE_PREVIEW');
  }

  const protectedBefore={
    conversations:aggregateTree(path.join(HOME,'conversations')),
    memory:aggregateTree(path.join(HOME,'memory')),
  };
  beforeState.protected=protectedBefore;

  const repoRoot=path.join(WORK_ROOT,'VexLife-Prod');
  effects.providerOrNetworkEffectPerformed=true;
  run(gh,['repo','clone',REPOSITORY,repoRoot,'--','--filter=blob:none','--no-checkout'],{
    cwd:WORK_ROOT,label:'PRODUCT_CLONE',timeout:300000,
  });
  run(git,['checkout','--quiet','--detach',HEAD],{cwd:repoRoot,label:'CANDIDATE_CHECKOUT',timeout:60000});
  const observedHead=run(git,['rev-parse','HEAD'],{cwd:repoRoot,label:'CANDIDATE_HEAD'}).stdout.trim();
  const observedTree=run(git,['rev-parse','HEAD^{tree}'],{cwd:repoRoot,label:'CANDIDATE_TREE'}).stdout.trim();
  if(observedHead!==HEAD||observedTree!==TREE) stop('harness','CANDIDATE_SOURCE_IDENTITY_DRIFT');
  const ancestor=spawnSync(git,['merge-base','--is-ancestor',BASE,HEAD],{cwd:repoRoot});
  if(ancestor.status!==0) stop('harness','CANDIDATE_BASE_NOT_ANCESTOR');

  const javaHomes=[];
  const addJavaHome=(value)=>{
    if(value&&fs.existsSync(path.join(value,'bin','java'))&&fs.existsSync(path.join(value,'bin','javac'))&&!javaHomes.includes(value)) javaHomes.push(value);
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

  const appRoot=path.join(repoRoot,'build','vexlife-prod-mac-application',HEAD);
  const app=path.join(appRoot,'VexLife.app');
  const receiptFile=path.join(appRoot,'VexLife-macOS-AppleSilicon.receipt.json');
  const setupLauncher=path.join(app,'Contents','Resources','private-service-image','bin','vexlife-prod-setup');
  for(const required of [app,receiptFile,setupLauncher]){
    if(!fs.existsSync(required)) stop('candidate','MAC_APP_ARTIFACT_MISSING');
  }
  const buildReceipt=JSON.parse(fs.readFileSync(receiptFile,'utf8'));
  if(buildReceipt.sourceSha!==HEAD) stop('candidate','MAC_APP_SOURCE_RECEIPT_MISMATCH');
  completedStepReceipts.push({step:'candidate-mac-app-build',state:'PASS',head:HEAD,tree:TREE});

  const turnAfterBuild=await observeTurnOrNull();
  afterState.turnAfterBuild=turnAfterBuild;
  if(turnAfterBuild&&['ACTIVE','CANCELLING'].includes(String(turnAfterBuild.state||''))){
    stop('environment','CONVERSATION_TURN_BECAME_ACTIVE_BEFORE_CONVERGENCE');
  }

  const modelProjection=parseJsonOutput(run(bash,[
    setupLauncher,'--setup-model','--home',HOME,
  ],{cwd:repoRoot,label:'MODEL_SELECTION_OBSERVE',timeout:120000}),'MODEL_SELECTION_OBSERVE');
  afterState.modelSelection={
    selected:modelProjection?.selected??null,
    chosen:modelProjection?.chosen??null,
    savedInHome:modelProjection?.savedInHome??null,
  };
  if(modelProjection?.selected!==FOUR_B_HANDLE||modelProjection?.savedInHome!==true){
    stop('environment','FOUR_B_NOT_ALREADY_SELECTED_IN_HOME');
  }

  const inspectFrontDoor=()=>
    parseJsonOutput(run(bash,[
      setupLauncher,'--inspect-front-door','--home',HOME,'--expected-source-sha',HEAD,
    ],{cwd:repoRoot,label:'FRONT_DOOR_INSPECT',timeout:120000}),'FRONT_DOOR_INSPECT');
  const activateFrontDoor=(control)=>
    parseJsonOutput(run(bash,[
      setupLauncher,'--activate-front-door-control',control,'--home',HOME,'--expected-source-sha',HEAD,
    ],{cwd:repoRoot,label:'FRONT_DOOR_'+control,timeout:900000}),'FRONT_DOOR_'+control);

  let projection=inspectFrontDoor();
  afterState.frontDoorInitial={
    semanticStateRef:projection?.semanticStateRef??null,
    currentServiceSourceSha:projection?.currentServiceSourceSha??null,
    currentServicePid:projection?.currentServicePid??null,
  };
  for(let i=0;i<4&&projection?.semanticStateRef!=='CURRENT';i++){
    const turnNow=await observeTurnOrNull();
    if(turnNow&&['ACTIVE','CANCELLING'].includes(String(turnNow.state||''))){
      stop('environment','CONVERSATION_TURN_ACTIVE_DURING_CONVERGENCE');
    }
    const state=projection?.semanticStateRef;
    let control=null;
    if(state==='START_REQUIRED') control='START';
    else if(state==='UPDATE_AVAILABLE'||state==='REPAIR_REQUIRED'||state==='UPDATE_REPAIR_REQUIRED') control='CONVERGE_DESIRED';
    else if(state==='SERVICE_RECOVERY_REQUIRED') control='RECOVER_STALE_SERVICE';
    else stop('environment','FRONT_DOOR_STATE_'+String(state||'UNKNOWN'));
    effects.modelRuntimePerformed=true;
    const activated=activateFrontDoor(control);
    afterState['activation'+i]={
      controlRef:activated?.controlRef??null,
      semanticStateRef:activated?.semanticStateRef??null,
      manifestationRef:activated?.manifestationRef??null,
      failure:activated?.failure?.code??activated?.failure?.detail??null,
    };
    if(activated?.failure) stop('candidate','FRONT_DOOR_CONTROL_FAILED');
    projection=inspectFrontDoor();
  }
  if(projection?.semanticStateRef!=='CURRENT'||projection?.currentServiceSourceSha!==HEAD){
    stop('candidate','CANDIDATE_DID_NOT_CONVERGE_CURRENT');
  }

  const source=await waitJson(SERVICE_ORIGIN+SOURCE_PATH,value=>
    value?.sourceSha===HEAD&&Number.isInteger(value?.processPid)&&typeof value?.startupQualifiedRuntimeReceiptRef==='string',
    60000,'SOURCE_CURRENTNESS');
  const runtime=readRuntimeReceipt();
  if(runtime.modelRef!==FOUR_B_MODEL_REF) stop('environment','RUNTIME_NOT_EXACT_HERETIC_4B');
  if(runtime.receiptRef!==source.startupQualifiedRuntimeReceiptRef||!processAlive(runtime.pid)){
    stop('candidate','RUNTIME_NOT_EXACT_LIVE');
  }
  afterState.current={sourceSha:source.sourceSha,servicePid:source.processPid,runtime};
  completedStepReceipts.push({step:'candidate-source-runtime-current',state:'PASS'});

  const protectedAfter={
    conversations:aggregateTree(path.join(HOME,'conversations')),
    memory:aggregateTree(path.join(HOME,'memory')),
  };
  afterState.protected=protectedAfter;
  if(protectedAfter.conversations!==protectedBefore.conversations) stop('candidate','PREVIEW_CONVERGENCE_MUTATED_CONVERSATION');
  if(protectedAfter.memory!==protectedBefore.memory) stop('candidate','PREVIEW_CONVERGENCE_MUTATED_MEMORY');

  const turnProjection=await waitJson(SERVICE_ORIGIN+TURN_PATH,value=>
    value?.state==='READY'&&
    value?.initialPromptPreparationPendingForNextTurn===true&&
    value?.initialPromptPreparationForActiveTurn===false,
    90000,'FIRST_LOCAL_START_PROJECTION');
  afterState.firstLocalStartProjection={
    state:turnProjection.state,
    pendingForNextTurn:turnProjection.initialPromptPreparationPendingForNextTurn,
    forActiveTurn:turnProjection.initialPromptPreparationForActiveTurn,
    effectsPerformed:turnProjection.effectsPerformed,
  };

  const [html,css,javascript]=await Promise.all([
    getText(SERVICE_ORIGIN+'/',10000),
    getText(SERVICE_ORIGIN+'/vexweb.css',10000),
    getText(SERVICE_ORIGIN+'/vexweb.js',10000),
  ]);
  const presentationChecks={
    htmlResponseProgress:
      html.includes('data-vex-ref="element.vexlife-workspace.companion.conversation.response-progress"')&&
      html.includes('id="response-progress-title"')&&
      html.includes('id="response-progress-detail"'),
    cssFirstLocalStart:
      css.includes('.response-progress[data-state="FirstLocalStart"]')&&
      css.includes('var(--status-caution)'),
    jsProjection:
      javascript.includes('initialPromptPreparationPendingForNextTurn')&&
      javascript.includes('initialPromptPreparationForActiveTurn')&&
      javascript.includes('workspace.heading.first-local-start')&&
      javascript.includes('workspace.description.first-local-start-ready-to-send')&&
      javascript.includes('workspace.description.first-local-start-sent'),
    noInventedIdentity:
      !javascript.includes('FirstLocalStartStatus')&&
      !html.includes('first-local-start-status'),
    noProviderCacheVocabulary:
      !javascript.includes('READY_REUSABLE')&&
      !javascript.includes('cachedPromptTokens'),
  };
  afterState.presentation={
    checks:presentationChecks,
    htmlSha256:sha256Bytes(Buffer.from(html,'utf8')),
    cssSha256:sha256Bytes(Buffer.from(css,'utf8')),
    javascriptSha256:sha256Bytes(Buffer.from(javascript,'utf8')),
  };
  if(Object.values(presentationChecks).some(value=>value!==true)){
    stop('candidate','LIVE_PRESENTATION_BYTES_NOT_ARCHITECTURE_CURRENT');
  }

  run(open,[SERVICE_ORIGIN+'/'],{label:'OPEN_CANDIDATE_VEXLIFE',timeout:30000});
  afterState.browserOpened=true;
  completedStepReceipts.push({step:'lived-first-local-start-preview',state:'PASS',humanVisualAcceptance:'PENDING_HUMAN'});
  writeResult('PASS',null);
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
