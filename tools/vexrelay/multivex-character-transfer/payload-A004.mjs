import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import crypto from 'node:crypto';
import { spawnSync } from 'node:child_process';

const BASE='f1855e9be6b9172ba6b43b597fa5d902dc409ed1';
const HEAD='ddb040dc6d6cb203889e8a72e70ccf1259bd503a';
const TREE='3ec627599f769b5319123fba8c93d07dfd0318c4';
const BRANCH='VXG-112226-evolve-context-dynamic-counterpart-dcp1-a001';
const REPOSITORY='VexGPT/VexLife-Prod';
const FOUR_B_MODEL_REF='model.vexlife-prod.heretic.qwen3.8-4b-distill.mlx4.d2f8f4028f20';
const TASK_REF='task.vexlife-prod.multivex.character-transfer.a004';
const ATTEMPT_PREFIX='attempt.vexlife-prod.multivex.character-transfer.a004.';
const RETURN_ROUTE_REF='github.pull.vexlife-prod.543';
const F20_BODY_BLOB='4af59951c177bddaec058253a967f448e8a5c947';
const F20_REVISIONS_BLOB='a7a497900bacc5ded736ab12f58e0c6e876a39c8';
const F20_BODY_SHA='d6e45b6189aa45e03395a93f7ff0bceb40ee9fe6ffa692d5a5213a49fc9e806a';

const RESULT_DIR=process.env.VEXLOCALBRIDGE_RESULT_DIR;
const PAYLOAD_ROOT=process.env.VEXLOCALBRIDGE_PAYLOAD_ROOT || process.cwd();
const CAPSULE_ROOT=process.env.VEXLOCALBRIDGE_CAPSULE_ROOT;
const HOME=path.join(os.homedir(),'.vexlife');
const RESULT_FILE=RESULT_DIR && path.join(RESULT_DIR,'payload-result.json');
const WORK_ROOT=RESULT_DIR && path.join(RESULT_DIR,'multivex-character-transfer-work');
const PROGRESS_FILE=CAPSULE_ROOT && path.join(CAPSULE_ROOT,'bridge','progress','multivex-character-transfer.log');

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
  const line='[MULTI-VEX-CHARACTER-TRANSFER] '+value;
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
  afterState.lastCommand={label,exitCode:result.status,signal:result.signal||null,stdoutTail:String(result.stdout||'').slice(-24000),stderrTail:String(result.stderr||'').slice(-24000)};
  if(result.error) stop('environment',label+'_SPAWN_FAILED');
  if(result.signal) stop('environment',label+'_SIGNALLED');
  if(result.status!==0) stop('candidate',label+'_FAILED');
  return result;
}
function runAllow(exe,args,{cwd=WORK_ROOT,env=process.env,timeout=120000,label='COMMAND'}={}){
  stage(label);
  const result=spawnSync(exe,args,{cwd,env,encoding:'utf8',timeout,maxBuffer:128*1024*1024,stdio:['ignore','pipe','pipe']});
  afterState.lastCommand={label,exitCode:result.status,signal:result.signal||null,stdoutTail:String(result.stdout||'').slice(-48000),stderrTail:String(result.stderr||'').slice(-48000)};
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
function processAlive(pid){
  if(!Number.isInteger(pid)||pid<=0) return null;
  try{
    process.kill(pid,0);
    return true;
  }catch(error){
    if(error?.code==='ESRCH') return false;
    return null;
  }
}
function writeResult(disposition,stopReasonOrNull=null){
  fs.mkdirSync(RESULT_DIR,{recursive:true});
  afterState.progressTail=PROGRESS_FILE&&fs.existsSync(PROGRESS_FILE)?fs.readFileSync(PROGRESS_FILE,'utf8').slice(-32000):'';
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
  stage('00 admitted B2 character-transfer payload');

  const taskRef=process.env.VEXLOCALBRIDGE_TASK_REF||null;
  const attemptRef=process.env.VEXLOCALBRIDGE_ATTEMPT_REF||null;
  const returnRouteRef=process.env.VEXLOCALBRIDGE_RETURN_ROUTE_REF||null;
  const policy=JSON.parse(process.env.VEXLOCALBRIDGE_VEXLIFE_PROD_HOST_EFFECT_POLICY_JSON||'null');
  const allowed=JSON.parse(process.env.VEXLOCALBRIDGE_AUTHORIZED_EFFECT_REFS_JSON||'[]');
  const bindingPath=path.join(PAYLOAD_ROOT,'inputs','character-transfer-binding.json');
  const f20BodyPath=path.join(PAYLOAD_ROOT,'inputs','foundation-v0.6.20-body.json');
  const f20RevisionsPath=path.join(PAYLOAD_ROOT,'inputs','foundation-v0.6.20-revisions.json');
  if(!fs.existsSync(bindingPath)||!fs.existsSync(f20BodyPath)||!fs.existsSync(f20RevisionsPath)){
    stop('harness','MULTI_VEX_CHARACTER_TRANSFER_INPUT_MISSING');
  }
  const binding=readJson(bindingPath,'MULTI_VEX_CHARACTER_TRANSFER_BINDING');

  if(taskRef!==TASK_REF||
     typeof attemptRef!=='string'||!attemptRef.startsWith(ATTEMPT_PREFIX)||
     returnRouteRef!==RETURN_ROUTE_REF){
    stop('harness','TASK_ENV_BINDING_MISMATCH');
  }
  if(binding?.candidate?.base!==BASE||
     binding?.candidate?.head!==HEAD||
     binding?.candidate?.tree!==TREE||
     binding?.candidate?.branch!==BRANCH){
    stop('harness','IMMUTABLE_CHARACTER_TRANSFER_BINDING_MISMATCH');
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
  const runtimeConfigurationFile=path.join(HOME,'runtime','vexlife-prod','heretic-runtime-configuration.json');
  const productRuntimeReceiptFile=path.join(HOME,'runtime','vexlife-prod','heretic-persistent-runtime-owner-receipt.json');
  if(!fs.existsSync(productModelBindingFile)) stop('environment','PRODUCT_MODEL_BINDING_MISSING');
  if(!fs.existsSync(productRuntimeReceiptFile)) stop('environment','PRODUCT_RUNTIME_RECEIPT_MISSING');
  const productBindingBefore=readJson(productModelBindingFile,'PRODUCT_MODEL_BINDING_BEFORE');
  beforeState.productModelBinding={
    sha256:sha256File(productModelBindingFile),
    schemaVersion:productBindingBefore?.schemaVersion??null,
    state:productBindingBefore?.state??null,
    modelRef:productBindingBefore?.modelRef??null,
  };
  if(productBindingBefore?.modelRef!==FOUR_B_MODEL_REF) stop('environment','FOUR_B_NOT_ALREADY_SELECTED_IN_PRODUCT_BINDING');
  const runtimeConfigurationBefore=fs.existsSync(runtimeConfigurationFile)
    ?readJson(runtimeConfigurationFile,'RUNTIME_CONFIGURATION_BEFORE')
    :null;
  beforeState.productRuntimeConfiguration={
    present:runtimeConfigurationBefore!==null,
    sha256:runtimeConfigurationBefore?sha256File(runtimeConfigurationFile):null,
    selectedModelPromptCacheEntries:
      runtimeConfigurationBefore?.models?.find?.(x=>x?.modelRef===FOUR_B_MODEL_REF)?.promptCacheEntries??null,
  };
  const productRuntimeReceiptBefore=readJson(productRuntimeReceiptFile,'PRODUCT_RUNTIME_RECEIPT_BEFORE');
  beforeState.productRuntimeReceipt={
    sha256:sha256File(productRuntimeReceiptFile),
    receiptRef:productRuntimeReceiptBefore?.receiptRef??null,
    modelRef:productRuntimeReceiptBefore?.modelRef??null,
    pid:productRuntimeReceiptBefore?.runtime?.pid??null,
    implementationClassSha256:productRuntimeReceiptBefore?.implementationIdentity?.classSha256??null,
    promptCacheEntries:(()=>{
      const args=productRuntimeReceiptBefore?.runtime?.args;
      if(!Array.isArray(args)) return null;
      const i=args.indexOf('--prompt-cache-size');
      return i>=0 ? Number(args[i+1]) : null;
    })(),
  };
  if(beforeState.productRuntimeReceipt.modelRef!==FOUR_B_MODEL_REF) stop('environment','PRODUCT_RUNTIME_RECEIPT_NOT_4B');
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
  run(gh,['repo','clone',REPOSITORY,repoRoot,'--','--filter=blob:none','--no-checkout'],{cwd:WORK_ROOT,label:'03 acquire exact Multi-Vex source',timeout:300000});
  run(git,['checkout','--quiet','--detach',HEAD],{cwd:repoRoot,label:'04 checkout exact B2 transfer head',timeout:60000});
  const observedHead=run(git,['rev-parse','HEAD'],{cwd:repoRoot,label:'05 verify source head'}).stdout.trim();
  const observedTree=run(git,['rev-parse','HEAD^{tree}'],{cwd:repoRoot,label:'06 verify source tree'}).stdout.trim();
  if(observedHead!==HEAD||observedTree!==TREE) stop('harness','SOURCE_IDENTITY_DRIFT');
  const ancestor=spawnSync(git,['merge-base','--is-ancestor',BASE,HEAD],{cwd:repoRoot});
  if(ancestor.status!==0) stop('harness','BASE_NOT_ANCESTOR_OF_HEAD');
  if(run(git,['status','--porcelain=v1','-uall'],{cwd:repoRoot,label:'07 verify clean source'}).stdout.trim()) stop('harness','SOURCE_CHECKOUT_DIRTY');
  completedStepReceipts.push({step:'exact-source',state:'PASS',head:HEAD,tree:TREE});

  const observedF20Body=run(git,['hash-object',f20BodyPath],{cwd:repoRoot,label:'07 verify F20 body input blob'}).stdout.trim();
  const observedF20Revisions=run(git,['hash-object',f20RevisionsPath],{cwd:repoRoot,label:'07 verify F20 revisions input blob'}).stdout.trim();
  if(observedF20Body!==F20_BODY_BLOB||observedF20Revisions!==F20_REVISIONS_BLOB){
    stop('harness','F20_FOUNDATION_INPUT_DRIFT');
  }
  completedStepReceipts.push({
    step:'foundation-treatment-byte-bindings',
    state:'PASS',
    version:'0.6.20',
    bodyBlob:F20_BODY_BLOB,
    revisionsBlob:F20_REVISIONS_BLOB,
    bodySha256:F20_BODY_SHA,
  });

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

  stage('08 focused B2 character-transfer source gate');
  run(bash,[
    gradlew,'--no-daemon','--max-workers=1',
    '-Dorg.gradle.jvmargs=-Xmx4g',
    '-Dkotlin.daemon.jvm.options=-Xmx4g',
    'jvmTest',
    '--tests','vexlife.harness.DynamicCounterpartProfilesTest',
    '--tests','vexlife.harness.DynamicCounterpartContractTest',
    '--tests','vexlife.foundational.VexLifeFoundationalFieldTest',
  ],{cwd:repoRoot,env:gradleEnv,label:'08 focused B2 transfer source gate',timeout:1200000});
  completedStepReceipts.push({step:'focused-character-transfer-source-gate',state:'PASS'});

  stage('09 full JVM regression gate');
  run(bash,[
    gradlew,'--no-daemon','--max-workers=1',
    '-Dorg.gradle.jvmargs=-Xmx4g',
    '-Dkotlin.daemon.jvm.options=-Xmx4g',
    'jvmTest',
  ],{cwd:repoRoot,env:gradleEnv,label:'09 full JVM source gate',timeout:1800000});
  completedStepReceipts.push({step:'full-jvm-source-gate',state:'PASS'});

  if(run(git,['status','--porcelain=v1','-uall'],{cwd:repoRoot,label:'10 verify post-build source'}).stdout.trim()){
    stop('harness','SOURCE_CHECKOUT_DIRTY_AFTER_TESTS');
  }

  effects.modelRuntimePerformed=true;
  const transferResultRelative=path.join('build','multivex-character-transfer');
  const transferResultDir=path.join(repoRoot,transferResultRelative);

  const commonArgs=[
    '--runtime-home',HOME,
    '--role-file','tools/continuity-pressure/roles/ordinary-shared-understanding-v1.json',
    '--scenario-file','tools/continuity-pressure/scenario-profiles/changed-plan-v1.json',
    '--foundation-body-file',f20BodyPath,
    '--foundation-revisions-file',f20RevisionsPath,
    '--source-sha',HEAD,
    '--source-clean','true',
    '--experiment-window','true',
    '--runtime-start-at-bind','false',
  ];

  function runEncounter(label,characterFile,bindingFile,resultLeaf){
    const relative=path.join(transferResultRelative,resultLeaf);
    const dir=path.join(repoRoot,relative);
    const args=[
      '--character-file',characterFile,
      '--binding-file',bindingFile,
      '--result-dir',relative,
      ...commonArgs,
    ].join(' ');
    const lived=runAllow(bash,[
      gradlew,'--no-daemon','--max-workers=1',
      '-Dorg.gradle.jvmargs=-Xmx4g',
      '-Dkotlin.daemon.jvm.options=-Xmx4g',
      'dynamicCounterpartProfileEncounter',
      '--args='+args,
    ],{cwd:repoRoot,env:gradleEnv,label,timeout:3600000});
    const resultFile=path.join(dir,'profile-encounter-result.json');
    const checkpointFile=path.join(dir,'profile-encounter-checkpoint.json');
    return {
      gradle:{
        exitCode:lived.status,
        stdoutTail:String(lived.stdout||'').slice(-48000),
        stderrTail:String(lived.stderr||'').slice(-48000),
      },
      result:fs.existsSync(resultFile)
        ?readJson(resultFile,label.replace(/[^A-Za-z0-9]+/g,'_')+'_RESULT')
        :null,
      checkpoint:fs.existsSync(checkpointFile)
        ?readJson(checkpointFile,label.replace(/[^A-Za-z0-9]+/g,'_')+'_CHECKPOINT')
        :null,
    };
  }

  stage('11 run Sarah control under exact Foundation v0.6.20');
  const sarah=runEncounter(
    '11 Sarah B2 control encounter',
    'tools/continuity-pressure/characters/sarah-v1.json',
    'tools/continuity-pressure/bindings/sarah-changed-plan-v1.json',
    'sarah',
  );
  afterState.sarahEncounter=sarah;

  stage('12 run Mira transfer under exact same Foundation v0.6.20');
  const mira=runEncounter(
    '12 Mira B2 transfer encounter',
    'tools/continuity-pressure/characters/mira-v1.json',
    'tools/continuity-pressure/bindings/mira-changed-plan-v1.json',
    'mira',
  );
  afterState.miraEncounter=mira;

  const productBindingAfter=readJson(productModelBindingFile,'PRODUCT_MODEL_BINDING_AFTER');
  afterState.productModelBinding={
    sha256:sha256File(productModelBindingFile),
    schemaVersion:productBindingAfter?.schemaVersion??null,
    state:productBindingAfter?.state??null,
    modelRef:productBindingAfter?.modelRef??null,
  };
  const runtimeConfigurationAfter=fs.existsSync(runtimeConfigurationFile)
    ?readJson(runtimeConfigurationFile,'RUNTIME_CONFIGURATION_AFTER')
    :null;
  afterState.productRuntimeConfiguration={
    present:runtimeConfigurationAfter!==null,
    sha256:runtimeConfigurationAfter?sha256File(runtimeConfigurationFile):null,
    selectedModelPromptCacheEntries:
      runtimeConfigurationAfter?.models?.find?.(x=>x?.modelRef===FOUR_B_MODEL_REF)?.promptCacheEntries??null,
  };
  const productRuntimeReceiptAfter=readJson(productRuntimeReceiptFile,'PRODUCT_RUNTIME_RECEIPT_AFTER');
  afterState.productRuntimeReceipt={
    sha256:sha256File(productRuntimeReceiptFile),
    receiptRef:productRuntimeReceiptAfter?.receiptRef??null,
    modelRef:productRuntimeReceiptAfter?.modelRef??null,
    pid:productRuntimeReceiptAfter?.runtime?.pid??null,
    implementationClassSha256:productRuntimeReceiptAfter?.implementationIdentity?.classSha256??null,
    promptCacheEntries:(()=>{
      const args=productRuntimeReceiptAfter?.runtime?.args;
      if(!Array.isArray(args)) return null;
      const i=args.indexOf('--prompt-cache-size');
      return i>=0 ? Number(args[i+1]) : null;
    })(),
  };
  afterState.productRuntimeConfigurationUnchanged=
    afterState.productRuntimeConfiguration.present===beforeState.productRuntimeConfiguration.present &&
    afterState.productRuntimeConfiguration.sha256===beforeState.productRuntimeConfiguration.sha256 &&
    afterState.productRuntimeConfiguration.selectedModelPromptCacheEntries===
      beforeState.productRuntimeConfiguration.selectedModelPromptCacheEntries;
  afterState.productRuntimeReceiptUnchanged=
    afterState.productRuntimeReceipt.sha256===beforeState.productRuntimeReceipt.sha256 &&
    afterState.productRuntimeReceipt.receiptRef===beforeState.productRuntimeReceipt.receiptRef &&
    afterState.productRuntimeReceipt.pid===beforeState.productRuntimeReceipt.pid;
  afterState.protected={
    conversations:aggregateTree(path.join(HOME,'conversations')),
    memory:aggregateTree(path.join(HOME,'memory')),
  };
  afterState.canonicalConversationUnchanged=afterState.protected.conversations===beforeState.protected.conversations;
  afterState.canonicalMemoryUnchanged=afterState.protected.memory===beforeState.protected.memory;
  afterState.selectedModelUnchanged=productBindingAfter?.modelRef===FOUR_B_MODEL_REF;
  afterState.counterpartProcessCleanup={
    sarahPid:sarah?.result?.counterpartRuntimePid??null,
    sarahAliveAfter:processAlive(sarah?.result?.counterpartRuntimePid??null),
    miraPid:mira?.result?.counterpartRuntimePid??null,
    miraAliveAfter:processAlive(mira?.result?.counterpartRuntimePid??null),
  };

  if(afterState.counterpartProcessCleanup.sarahAliveAfter===true){
    candidateFindings.push('SARAH_COUNTERPART_PROCESS_LEAK');
  }
  if(afterState.counterpartProcessCleanup.miraAliveAfter===true){
    candidateFindings.push('MIRA_COUNTERPART_PROCESS_LEAK');
  }

  if(!afterState.canonicalConversationUnchanged) candidateFindings.push('CANONICAL_CONVERSATION_CHANGED');
  if(!afterState.canonicalMemoryUnchanged) candidateFindings.push('CANONICAL_MEMORY_CHANGED');
  if(!afterState.selectedModelUnchanged) candidateFindings.push('MODEL_SELECTION_CHANGED');
  if(!afterState.productRuntimeConfigurationUnchanged) candidateFindings.push('PRODUCT_RUNTIME_CONFIGURATION_CHANGED');
  if(!afterState.productRuntimeReceiptUnchanged) candidateFindings.push('PRODUCT_RUNTIME_RECEIPT_CHANGED');

  const expectedRoleRef='role.dynamic-counterpart.ordinary-shared-understanding.v1';
  const expectedScenarioRef='scenario.vexlife.evolve-context.changed-plan.v1';
  function validateEncounter(label,value,expected){
    const result=value?.result;
    if(!result){
      candidateFindings.push(label+'_RESULT_MISSING');
      return;
    }
    if(value.gradle.exitCode!==0) candidateFindings.push(label+'_GRADLE_NONZERO');
    if(result.status!=='PASS'||result.mechanicalState!=='PASS') candidateFindings.push(label+'_NOT_PASS');
    if(result.characterRef!==expected.characterRef) candidateFindings.push(label+'_CHARACTER_REF_MISMATCH');
    if(result.participantRef!==expected.participantRef) candidateFindings.push(label+'_PARTICIPANT_REF_MISMATCH');
    if(result.participantName!==expected.displayName) candidateFindings.push(label+'_DISPLAY_NAME_MISMATCH');
    if(result.bindingRef!==expected.bindingRef) candidateFindings.push(label+'_BINDING_REF_MISMATCH');
    if(result.roleRef!==expectedRoleRef) candidateFindings.push(label+'_ROLE_REF_MISMATCH');
    if(result.scenarioRef!==expectedScenarioRef) candidateFindings.push(label+'_SCENARIO_REF_MISMATCH');
    if(result.foundationOverrideSupplied!==true) candidateFindings.push(label+'_FOUNDATION_OVERRIDE_MISSING');
    if(result.foundationVersion!=='0.6.20'||result.foundationBodySha256!==F20_BODY_SHA){
      candidateFindings.push(label+'_FOUNDATION_BINDING_MISMATCH');
    }
    if(result.observerLensSupplied!==false) candidateFindings.push(label+'_OBSERVER_LENS_PRESENT');
    if(result.participantAttributionExact!==true) candidateFindings.push(label+'_PARTICIPANT_ATTRIBUTION_NOT_EXACT');
    if(result.personContextProjectionExact!==true) candidateFindings.push(label+'_PERSON_CONTEXT_NOT_EXACT');
    if(result.visibleSourceExact!==true) candidateFindings.push(label+'_VISIBLE_SOURCE_NOT_EXACT');
    if(result.processIsolationExact!==true) candidateFindings.push(label+'_PROCESS_ISOLATION_NOT_EXACT');
    if(result.counterpartCacheReuseObserved!==true) candidateFindings.push(label+'_COUNTERPART_CACHE_REUSE_NOT_OBSERVED');
    if(result.vexRuntimeStillCurrent!==true) candidateFindings.push(label+'_VEX_RUNTIME_NOT_CURRENT');
    if(result.canonicalWriterStayedIdle!==true) candidateFindings.push(label+'_CANONICAL_WRITER_NOT_IDLE');
  }

  validateEncounter('SARAH',sarah,{
    characterRef:'character.vexlife.dynamic-counterpart.sarah.v1',
    participantRef:'person.synthetic.7c4e9b1a',
    displayName:'Sarah',
    bindingRef:'binding.dynamic-counterpart.sarah.changed-plan.ordinary-shared-understanding.v1',
  });
  validateEncounter('MIRA',mira,{
    characterRef:'character.vexlife.dynamic-counterpart.person-c.v1',
    participantRef:'person.synthetic.c39a72e1',
    displayName:'Mira',
    bindingRef:'binding.dynamic-counterpart.person-c.changed-plan.ordinary-shared-understanding.v1',
  });

  afterState.transferComparison={
    sameRoleRef:sarah?.result?.roleRef===mira?.result?.roleRef && sarah?.result?.roleRef===expectedRoleRef,
    sameScenarioRef:sarah?.result?.scenarioRef===mira?.result?.scenarioRef && sarah?.result?.scenarioRef===expectedScenarioRef,
    sameFoundation:
      sarah?.result?.foundationBodySha256===mira?.result?.foundationBodySha256 &&
      sarah?.result?.foundationBodySha256===F20_BODY_SHA,
    sameVexRuntimeReceipt:
      sarah?.result?.vexRuntimeReceiptRef===mira?.result?.vexRuntimeReceiptRef &&
      sarah?.result?.vexRuntimeReceiptRef===beforeState.productRuntimeReceipt.receiptRef,
    participantIdentityDiffers:
      sarah?.result?.participantRef!==mira?.result?.participantRef &&
      sarah?.result?.participantName!==mira?.result?.participantName,
    transcriptEqualityRequired:false,
  };
  if(!afterState.transferComparison.sameRoleRef) candidateFindings.push('TRANSFER_ROLE_TOPOLOGY_DRIFT');
  if(!afterState.transferComparison.sameScenarioRef) candidateFindings.push('TRANSFER_SCENARIO_TOPOLOGY_DRIFT');
  if(!afterState.transferComparison.sameFoundation) candidateFindings.push('TRANSFER_FOUNDATION_DRIFT');
  if(!afterState.transferComparison.sameVexRuntimeReceipt) candidateFindings.push('TRANSFER_RUNTIME_RECEIPT_DRIFT');
  if(!afterState.transferComparison.participantIdentityDiffers) candidateFindings.push('TRANSFER_IDENTITY_DID_NOT_DIFFER');

  completedStepReceipts.push({
    step:'multivex-character-transfer',
    state:candidateFindings.length===0?'PASS':'SAFE_FAILURE',
    sarahStatus:sarah?.result?.status??null,
    miraStatus:mira?.result?.status??null,
    sameRoleRef:afterState.transferComparison.sameRoleRef,
    sameScenarioRef:afterState.transferComparison.sameScenarioRef,
    sameFoundation:afterState.transferComparison.sameFoundation,
    sameVexRuntimeReceipt:afterState.transferComparison.sameVexRuntimeReceipt,
    participantIdentityDiffers:afterState.transferComparison.participantIdentityDiffers,
    productPromptCacheEntriesBefore:beforeState.productRuntimeConfiguration.selectedModelPromptCacheEntries,
    productPromptCacheEntriesAfter:afterState.productRuntimeConfiguration.selectedModelPromptCacheEntries,
    productRuntimeConfigurationUnchanged:afterState.productRuntimeConfigurationUnchanged,
    productRuntimeReceiptUnchanged:afterState.productRuntimeReceiptUnchanged,
    semanticJudgment:'HUMAN_GODWARD_REVIEW_REQUIRED: transfer proof establishes relationship portability, not identical wording or automatic trust.',
  });

  if(candidateFindings.length===0){
    stage('13 B2 Sarah-Mira character transfer mechanical PASS');
    writeResult('PASS',null);
  }else{
    stage('13 B2 Sarah-Mira character transfer SAFE_FAILURE');
    writeResult('SAFE_FAILURE','MULTI_VEX_CHARACTER_TRANSFER_ACCEPTANCE_NOT_MET');
  }
}catch(error){
  if(error instanceof Stop){
    addFinding(error.category,error.code);
    stage('STOP '+error.code);
    writeResult('SAFE_FAILURE',error.code);
  }else{
    harnessFindings.push('MULTI_VEX_CHARACTER_TRANSFER_UNCAUGHT_'+String(error?.name||'ERROR'));
    afterState.uncaughtClass=String(error?.name||'Error');
    afterState.uncaughtMessage=String(error?.message||'');
    stage('STOP MULTI_VEX_CHARACTER_TRANSFER_UNCAUGHT');
    writeResult('SAFE_FAILURE','MULTI_VEX_CHARACTER_TRANSFER_UNCAUGHT');
  }
}
