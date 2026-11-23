import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import crypto from 'node:crypto';
import { spawnSync } from 'node:child_process';

const BASE='1baf85526d44f392c0c5b99651241f39efa97c01';
const HEAD='d01ab5443097c338e01aa3860e28566f153fbeff';
const TREE='9cb434f1c60b6bf7b84ab97e7f88ab075ba19aa2';
const BRANCH='VXG-112226-dcp-b1-foundation-v0620-a001';
const REPOSITORY='VexGPT/VexLife-Prod';
const FOUR_B_MODEL_REF='model.vexlife-prod.heretic.qwen3.8-4b-distill.mlx4.d2f8f4028f20';
const TASK_REF='task.vexlife-prod.pr552.dcp-b1-foundation-v0620.a003';
const ATTEMPT_PREFIX='attempt.vexlife-prod.pr552.dcp-b1-foundation-v0620.a003.';
const RETURN_ROUTE_REF='github.pull.vexlife-prod.552';

const RESULT_DIR=process.env.VEXLOCALBRIDGE_RESULT_DIR;
const PAYLOAD_ROOT=process.env.VEXLOCALBRIDGE_PAYLOAD_ROOT || process.cwd();
const CAPSULE_ROOT=process.env.VEXLOCALBRIDGE_CAPSULE_ROOT;
const HOME=path.join(os.homedir(),'.vexlife');
const RESULT_FILE=RESULT_DIR && path.join(RESULT_DIR,'payload-result.json');
const WORK_ROOT=RESULT_DIR && path.join(RESULT_DIR,'dcp-b1-foundation-v0620-work');
const PROGRESS_FILE=CAPSULE_ROOT && path.join(CAPSULE_ROOT,'bridge','progress','dcp-b1.log');

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

class Stop extends Error{
  constructor(category,code){super(code);this.category=category;this.code=code;}
}
const stop=(category,code)=>{throw new Stop(category,code);};

function stage(value){
  const line='[DCP-B1-CARRIER] '+value;
  process.stdout.write(line+'\n');
  if(PROGRESS_FILE){
    fs.mkdirSync(path.dirname(PROGRESS_FILE),{recursive:true});
    fs.appendFileSync(PROGRESS_FILE,line+'\n');
  }
}
function sha256Bytes(bytes){return crypto.createHash('sha256').update(bytes).digest('hex');}
function sha256File(file){return sha256Bytes(fs.readFileSync(file));}
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
  stage(label);
  const result=spawnSync(exe,args,{cwd,env,encoding:'utf8',timeout,maxBuffer:128*1024*1024,stdio:['ignore','pipe','pipe']});
  afterState.lastCommand={label,exitCode:result.status,signal:result.signal||null,stdoutTail:String(result.stdout||'').slice(-16000),stderrTail:String(result.stderr||'').slice(-16000)};
  if(result.error) stop('environment',label+'_SPAWN_FAILED');
  if(result.signal) stop('environment',label+'_SIGNALLED');
  if(result.status!==0) stop('candidate',label+'_FAILED');
  return result;
}
function runAllow(exe,args,{cwd=WORK_ROOT,env=process.env,timeout=120000,label='COMMAND'}={}){
  stage(label);
  const result=spawnSync(exe,args,{cwd,env,encoding:'utf8',timeout,maxBuffer:128*1024*1024,stdio:['ignore','pipe','pipe']});
  afterState.lastCommand={label,exitCode:result.status,signal:result.signal||null,stdoutTail:String(result.stdout||'').slice(-32000),stderrTail:String(result.stderr||'').slice(-32000)};
  if(result.error) stop('environment',label+'_SPAWN_FAILED');
  if(result.signal) stop('environment',label+'_SIGNALLED');
  return result;
}
function readJson(file,label){
  try{return JSON.parse(fs.readFileSync(file,'utf8'));}
  catch(error){
    afterState[label+'ReadError']=String(error?.message||error);
    stop('environment',label+'_UNREADABLE');
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
  afterState.progressTail=PROGRESS_FILE&&fs.existsSync(PROGRESS_FILE)?fs.readFileSync(PROGRESS_FILE,'utf8').slice(-24000):'';
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
  if(!RESULT_DIR||!PAYLOAD_ROOT||!CAPSULE_ROOT) stop('harness','VEXLOCALBRIDGE_PAYLOAD_ENV_MISSING');
  fs.mkdirSync(WORK_ROOT,{recursive:true});
  stage('00 admitted payload');

  const taskRef=process.env.VEXLOCALBRIDGE_TASK_REF||null;
  const attemptRef=process.env.VEXLOCALBRIDGE_ATTEMPT_REF||null;
  const returnRouteRef=process.env.VEXLOCALBRIDGE_RETURN_ROUTE_REF||null;
  const policy=JSON.parse(process.env.VEXLOCALBRIDGE_VEXLIFE_PROD_HOST_EFFECT_POLICY_JSON||'null');
  const allowed=JSON.parse(process.env.VEXLOCALBRIDGE_AUTHORIZED_EFFECT_REFS_JSON||'[]');
  const bindingPath=path.join(PAYLOAD_ROOT,'inputs','encounter-binding.json');
  if(!fs.existsSync(bindingPath)) stop('harness','DCP_B1_BINDING_MISSING');
  const binding=readJson(bindingPath,'DCP_B1_BINDING');

  if(taskRef!==TASK_REF||
     typeof attemptRef!=='string'||!attemptRef.startsWith(ATTEMPT_PREFIX)||
     returnRouteRef!==RETURN_ROUTE_REF){
    stop('harness','TASK_ENV_BINDING_MISMATCH');
  }
  if(binding?.candidate?.base!==BASE||
     binding?.candidate?.head!==HEAD||
     binding?.candidate?.tree!==TREE||
     binding?.candidate?.branch!==BRANCH){
    stop('harness','IMMUTABLE_DCP_B1_BINDING_MISMATCH');
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
     policy.productDataPolicy?.runtimeLifecycleMutationAllowed!==true){
    stop('harness','PRODUCT_HOST_EFFECT_POLICY_WIDENED_OR_INCOMPLETE');
  }

  stage('01 observe existing Home and exact Product 4B selection');
  if(!fs.existsSync(HOME)||!fs.statSync(HOME).isDirectory()) stop('environment','DEFAULT_VEXLIFE_HOME_MISSING');
  const productModelBindingFile=path.join(HOME,'runtime','vexlife-prod','model-binding.json');
  if(!fs.existsSync(productModelBindingFile)) stop('environment','PRODUCT_MODEL_BINDING_MISSING');
  const productBindingBefore=readJson(productModelBindingFile,'PRODUCT_MODEL_BINDING_BEFORE');
  beforeState.productModelBinding={
    sha256:sha256File(productModelBindingFile),
    schemaVersion:productBindingBefore?.schemaVersion??null,
    state:productBindingBefore?.state??null,
    modelRef:productBindingBefore?.modelRef??null,
  };
  if(productBindingBefore?.modelRef!==FOUR_B_MODEL_REF) stop('environment','FOUR_B_NOT_ALREADY_SELECTED_IN_PRODUCT_BINDING');
  beforeState.protected={
    conversations:aggregateTree(path.join(HOME,'conversations')),
    memory:aggregateTree(path.join(HOME,'memory')),
  };

  const gh=findExe(['/opt/homebrew/bin/gh','/usr/local/bin/gh'],'GITHUB_CLI');
  const git=findExe(['/usr/bin/git'],'GIT');
  const bash=findExe(['/bin/bash'],'BASH');
  run(gh,['auth','status','--hostname','github.com'],{label:'02 GitHub auth',timeout:30000});

  const repoRoot=path.join(WORK_ROOT,'VexLife-Prod');
  effects.providerOrNetworkEffectPerformed=true;
  run(gh,['repo','clone',REPOSITORY,repoRoot,'--','--filter=blob:none','--no-checkout'],{cwd:WORK_ROOT,label:'03 acquire exact Product source',timeout:300000});
  run(git,['checkout','--quiet','--detach',HEAD],{cwd:repoRoot,label:'04 checkout exact B1 head',timeout:60000});
  const observedHead=run(git,['rev-parse','HEAD'],{cwd:repoRoot,label:'05 verify source head'}).stdout.trim();
  const observedTree=run(git,['rev-parse','HEAD^{tree}'],{cwd:repoRoot,label:'06 verify source tree'}).stdout.trim();
  if(observedHead!==HEAD||observedTree!==TREE) stop('harness','SOURCE_IDENTITY_DRIFT');
  const ancestor=spawnSync(git,['merge-base','--is-ancestor',BASE,HEAD],{cwd:repoRoot});
  if(ancestor.status!==0) stop('harness','BASE_NOT_ANCESTOR_OF_HEAD');
  if(run(git,['status','--porcelain=v1','-uall'],{cwd:repoRoot,label:'07 verify clean source'}).stdout.trim()) stop('harness','SOURCE_CHECKOUT_DIRTY');

  const expectedBlobs={
    'docs/foundation/FOUNDATIONAL-FIELD.md':'115e8003001f71c19625c75070768f7a1e9d202f',
    'src/jvmMain/resources/foundational/vex-foundational-field.json':'4af59951c177bddaec058253a967f448e8a5c947',
    'src/jvmMain/resources/foundational/vex-foundational-field.revisions.json':'a7a497900bacc5ded736ab12f58e0c6e876a39c8',
    'src/jvmTest/kotlin/vexlife/foundational/VexLifeFoundationalFieldFixture.kt':'c4fdeacc7c7a3aafb6ea43adb873bdab01b1c5d3',
    'src/jvmTest/kotlin/vexlife/foundational/VexLifeFoundationalFieldTest.kt':'cf66d1cac4c5d41c4ceef056e59e3e0272031292',
  };
  for(const [filePath,expectedBlob] of Object.entries(expectedBlobs)){
    const observedBlob=run(git,['rev-parse','HEAD:'+filePath],{cwd:repoRoot,label:'07 verify B1 blob '+filePath,timeout:60000}).stdout.trim();
    if(observedBlob!==expectedBlob){
      afterState.blobMismatch={filePath,expectedBlob,observedBlob};
      stop('harness','B1_TREATMENT_BLOB_DRIFT');
    }
  }
  completedStepReceipts.push({
    step:'b1-treatment-byte-bindings',
    state:'PASS',
    treatmentSource:'github.pull.vexlife-prod.549',
    controlHead:'806054ccd4c97a01cfd9ac76e77f7668b78f0dc7',
    controlTree:'ae5795dc189cf7d4c92cf8108fcb857feea7df8e',
  });
  completedStepReceipts.push({step:'exact-source',state:'PASS',head:HEAD,tree:TREE});

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
    'kotlin.daemon.jvmargs=-Xmx4096m\n');
  const gradleEnv={...process.env,JAVA_HOME:jdk,GRADLE_USER_HOME:gradleUserHome};
  const gradlew=path.join(repoRoot,'gradlew');
  if(!fs.existsSync(gradlew)) stop('harness','GRADLE_WRAPPER_MISSING');

  stage('08 compile reusable profile encounter source');
  run(bash,[
    gradlew,'--no-daemon','--max-workers=1',
    '-Dorg.gradle.jvmargs=-Xmx4g',
    '-Dkotlin.daemon.jvm.options=-Xmx4g',
    'jvmTest',
    '--tests','vexlife.harness.DynamicCounterpartProfilesTest',
    '--tests','vexlife.harness.DynamicCounterpartContractTest',
  ],{cwd:repoRoot,env:gradleEnv,label:'08 focused B1 source gate',timeout:1200000});
  completedStepReceipts.push({step:'focused-b1-source-gate',state:'PASS'});

  stage('09 full JVM regression gate');
  run(bash,[
    gradlew,'--no-daemon','--max-workers=1',
    '-Dorg.gradle.jvmargs=-Xmx4g',
    '-Dkotlin.daemon.jvm.options=-Xmx4g',
    'jvmTest',
  ],{cwd:repoRoot,env:gradleEnv,label:'09 full JVM source gate',timeout:1800000});
  completedStepReceipts.push({step:'full-jvm-source-gate',state:'PASS'});

  if(run(git,['status','--porcelain=v1','-uall'],{cwd:repoRoot,label:'10 verify post-build source'}).stdout.trim()) stop('harness','SOURCE_CHECKOUT_DIRTY_AFTER_TESTS');

  effects.modelRuntimePerformed=true;
  const encounterResultRelative=path.join('build','dynamic-counterpart-profile-encounter-b1-live');
  const encounterResultDir=path.join(repoRoot,encounterResultRelative);
  const args=[
    '--runtime-home',HOME,
    '--character-file','tools/continuity-pressure/characters/sarah-v1.json',
    '--role-file','tools/continuity-pressure/roles/ordinary-shared-understanding-v1.json',
    '--scenario-file','tools/continuity-pressure/scenario-profiles/changed-plan-v1.json',
    '--binding-file','tools/continuity-pressure/bindings/sarah-changed-plan-v1.json',
    '--result-dir',encounterResultRelative,
    '--source-sha',HEAD,
    '--source-clean','true',
    '--experiment-window','true',
    '--runtime-start-at-bind','true',
  ].join(' ');

  stage('11 begin full reusable-profile Sarah/Vex encounter');
  const lived=runAllow(bash,[
    gradlew,'--no-daemon','--max-workers=1',
    '-Dorg.gradle.jvmargs=-Xmx4g',
    '-Dkotlin.daemon.jvm.options=-Xmx4g',
    'dynamicCounterpartProfileEncounter',
    '--args='+args,
  ],{cwd:repoRoot,env:gradleEnv,label:'11 DCP-B1 full encounter',timeout:3600000});

  afterState.encounterGradle={
    exitCode:lived.status,
    stdoutTail:String(lived.stdout||'').slice(-32000),
    stderrTail:String(lived.stderr||'').slice(-32000),
  };
  const encounterResultFile=path.join(encounterResultDir,'profile-encounter-result.json');
  if(!fs.existsSync(encounterResultFile)) stop('candidate','DCP_B1_RESULT_MISSING');
  const encounterResult=readJson(encounterResultFile,'DCP_B1_RESULT');
  afterState.encounterResult=encounterResult;

  const checkpointFile=path.join(encounterResultDir,'profile-encounter-checkpoint.json');
  afterState.encounterCheckpoint=fs.existsSync(checkpointFile)?readJson(checkpointFile,'DCP_B1_CHECKPOINT'):null;

  const productBindingAfter=readJson(productModelBindingFile,'PRODUCT_MODEL_BINDING_AFTER');
  afterState.productModelBinding={
    sha256:sha256File(productModelBindingFile),
    schemaVersion:productBindingAfter?.schemaVersion??null,
    state:productBindingAfter?.state??null,
    modelRef:productBindingAfter?.modelRef??null,
  };
  afterState.protected={
    conversations:aggregateTree(path.join(HOME,'conversations')),
    memory:aggregateTree(path.join(HOME,'memory')),
  };
  afterState.canonicalConversationUnchanged=afterState.protected.conversations===beforeState.protected.conversations;
  afterState.canonicalMemoryUnchanged=afterState.protected.memory===beforeState.protected.memory;
  afterState.selectedModelUnchanged=productBindingAfter?.modelRef===FOUR_B_MODEL_REF;

  if(!afterState.canonicalConversationUnchanged) candidateFindings.push('CANONICAL_CONVERSATION_CHANGED');
  if(!afterState.canonicalMemoryUnchanged) candidateFindings.push('CANONICAL_MEMORY_CHANGED');
  if(!afterState.selectedModelUnchanged) candidateFindings.push('MODEL_SELECTION_CHANGED');
  if(encounterResult?.status!=='PASS') candidateFindings.push('DCP_B1_ENCOUNTER_NOT_PASS');
  if(lived.status!==0) candidateFindings.push('DCP_B1_GRADLE_NONZERO');

  completedStepReceipts.push({
    step:'dcp-b1-full-encounter',
    state:candidateFindings.length===0?'PASS':'SAFE_FAILURE',
    encounterStatus:encounterResult?.status??null,
    failureStage:encounterResult?.failureStage??null,
    semanticJudgment:encounterResult?.semanticJudgment??null,
  });

  if(candidateFindings.length===0){
    stage('12 DCP-B1 mechanical PASS; semantic review remains human/Godward');
    writeResult('PASS',null);
  }else{
    stage('12 DCP-B1 SAFE_FAILURE');
    writeResult('SAFE_FAILURE','DCP_B1_ENCOUNTER_ACCEPTANCE_NOT_MET');
  }
}catch(error){
  if(error instanceof Stop){
    addFinding(error.category,error.code);
    stage('STOP '+error.code);
    writeResult('SAFE_FAILURE',error.code);
  }else{
    harnessFindings.push('DCP_B1_UNCAUGHT_'+String(error?.name||'ERROR'));
    afterState.uncaughtClass=String(error?.name||'Error');
    afterState.uncaughtMessage=String(error?.message||'');
    stage('STOP DCP_B1_UNCAUGHT');
    writeResult('SAFE_FAILURE','DCP_B1_UNCAUGHT');
  }
}
