import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import crypto from 'node:crypto';
import { spawnSync } from 'node:child_process';

const BASE='f1855e9be6b9172ba6b43b597fa5d902dc409ed1';
const HEAD='617c9db4bae4d7f152b267c89288350ed7535b34';
const TREE='103ad831b46a99e9149d8038fc5e57f9dc2f640f';
const BRANCH='VXG-112226-evolve-context-dynamic-counterpart-dcp1-a001';
const REPOSITORY='VexGPT/VexLife-Prod';
const FOUR_B_MODEL_REF='model.vexlife-prod.heretic.qwen3.8-4b-distill.mlx4.d2f8f4028f20';
const TASK_REF='task.vexlife-prod.multivex.state-effect-scope.se1.a005';
const ATTEMPT_PREFIX='attempt.vexlife-prod.multivex.state-effect-scope.se1.a005.';
const RETURN_ROUTE_REF='github.pull.vexlife-prod.543';
const F20_BODY_BLOB='4af59951c177bddaec058253a967f448e8a5c947';
const F20_REVISIONS_BLOB='a7a497900bacc5ded736ab12f58e0c6e876a39c8';
const F20_BODY_SHA='d6e45b6189aa45e03395a93f7ff0bceb40ee9fe6ffa692d5a5213a49fc9e806a';

const RESULT_DIR=process.env.VEXLOCALBRIDGE_RESULT_DIR;
const PAYLOAD_ROOT=process.env.VEXLOCALBRIDGE_PAYLOAD_ROOT || process.cwd();
const CAPSULE_ROOT=process.env.VEXLOCALBRIDGE_CAPSULE_ROOT;
const HOME=path.join(os.homedir(),'.vexlife');
const RESULT_FILE=RESULT_DIR && path.join(RESULT_DIR,'payload-result.json');
const WORK_ROOT=RESULT_DIR && path.join(RESULT_DIR,'multivex-state-effect-scope-se1-work');
const PROGRESS_FILE=CAPSULE_ROOT && path.join(CAPSULE_ROOT,'bridge','progress','multivex-state-effect-scope-se1.log');

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
  const line='[MULTI-VEX-STATE-EFFECT-SCOPE-SE1] '+value;
  process.stdout.write(line+'\n');
  if(PROGRESS_FILE){
    fs.mkdirSync(path.dirname(PROGRESS_FILE),{recursive:true});
    fs.appendFileSync(PROGRESS_FILE,line+'\n');
  }
}
function sha256Bytes(bytes){return crypto.createHash('sha256').update(bytes).digest('hex');}

function canonicalizeJson(value){
  if(Array.isArray(value)) return value.map(canonicalizeJson);
  if(value && typeof value==='object'){
    const out={};
    for(const key of Object.keys(value).sort()) out[key]=canonicalizeJson(value[key]);
    return out;
  }
  return value;
}
function canonicalJsonSha256(value){
  return sha256Bytes(Buffer.from(JSON.stringify(canonicalizeJson(value)),'utf8'));
}

function renderedFoundationKernel(body){
  const parts=['VEXLIFE_FOUNDATIONAL_FIELD/v1',...(body?.preamble??[])];
  for(const module of body?.modules??[]){
    parts.push('['+module.moduleRef+'] '+module.title);
    parts.push(...(module.kernel??[]));
  }
  return parts.join('\n\n');
}
function estimatedFoundationKernelTokens(body){
  const bytes=Buffer.byteLength(renderedFoundationKernel(body),'utf8');
  return Math.max(1,Math.floor((bytes+3)/4))+4;
}
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
  stage('00 admitted SE1 state-effect scope crossover payload');

  const taskRef=process.env.VEXLOCALBRIDGE_TASK_REF||null;
  const attemptRef=process.env.VEXLOCALBRIDGE_ATTEMPT_REF||null;
  const returnRouteRef=process.env.VEXLOCALBRIDGE_RETURN_ROUTE_REF||null;
  const policy=JSON.parse(process.env.VEXLOCALBRIDGE_VEXLIFE_PROD_HOST_EFFECT_POLICY_JSON||'null');
  const allowed=JSON.parse(process.env.VEXLOCALBRIDGE_AUTHORIZED_EFFECT_REFS_JSON||'[]');
  const bindingPath=path.join(PAYLOAD_ROOT,'inputs','relationship-pressure-binding.json');
  const f20BodyPath=path.join(PAYLOAD_ROOT,'inputs','foundation-v0.6.20-body.json');
  const f20RevisionsPath=path.join(PAYLOAD_ROOT,'inputs','foundation-v0.6.20-revisions.json');
  if(!fs.existsSync(bindingPath)||!fs.existsSync(f20BodyPath)||!fs.existsSync(f20RevisionsPath)){
    stop('harness','MULTI_VEX_RELATIONSHIP_PRESSURE_INPUT_MISSING');
  }
  const binding=readJson(bindingPath,'MULTI_VEX_RELATIONSHIP_PRESSURE_BINDING');

  if(taskRef!==TASK_REF||
     typeof attemptRef!=='string'||!attemptRef.startsWith(ATTEMPT_PREFIX)||
     returnRouteRef!==RETURN_ROUTE_REF){
    stop('harness','TASK_ENV_BINDING_MISMATCH');
  }
  if(binding?.candidate?.base!==BASE||
     binding?.candidate?.head!==HEAD||
     binding?.candidate?.tree!==TREE||
     binding?.candidate?.branch!==BRANCH){
    stop('harness','IMMUTABLE_RELATIONSHIP_PRESSURE_BINDING_MISMATCH');
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
  run(git,['checkout','--quiet','--detach',HEAD],{cwd:repoRoot,label:'04 checkout exact Multi-Vex source',timeout:60000});
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

  stage('08 focused SE1 state-effect source gate');
  run(bash,[
    gradlew,'--no-daemon','--max-workers=1',
    '-Dorg.gradle.jvmargs=-Xmx4g',
    '-Dkotlin.daemon.jvm.options=-Xmx4g',
    'jvmTest',
    '--tests','vexlife.harness.DynamicCounterpartProfilesTest',
    '--tests','vexlife.harness.DynamicCounterpartContractTest',
    '--tests','vexlife.foundational.VexLifeFoundationalFieldTest',
  ],{cwd:repoRoot,env:gradleEnv,label:'08 focused SE1 state-effect source gate',timeout:1200000});
  completedStepReceipts.push({step:'focused-relationship-pressure-source-gate',state:'PASS'});

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

  const treatmentSpecPath=path.join(repoRoot,'tools/continuity-pressure/treatments/foundation-state-effect-scope-v1.json');
  const treatmentSpec=readJson(treatmentSpecPath,'STATE_EFFECT_TREATMENT_SPEC');
  if(treatmentSpec?.schemaVersion!=='vexlife.multivex.foundation-treatment-patch/v1'||
     treatmentSpec?.treatmentRef!=='treatment.multivex.foundation.state-effect-scope.v1'||
     treatmentSpec?.sourceVersion!=='0.6.20'||
     treatmentSpec?.sourceBodySha256!==F20_BODY_SHA||
     treatmentSpec?.targetVersion!=='0.6.21'||
     treatmentSpec?.moduleRef!=='foundational.architecture'||
     treatmentSpec?.standing!=='EXPERIMENT_ONLY_NOT_PRODUCT_AUTHORITY'||
     treatmentSpec?.automaticAcceptanceAllowed!==false){
    stop('harness','STATE_EFFECT_TREATMENT_SPEC_DRIFT');
  }

  const f20Body=readJson(f20BodyPath,'F20_BODY');
  const f20Revisions=readJson(f20RevisionsPath,'F20_REVISIONS');
  if(f20Body?.version!=='0.6.20') stop('harness','F20_BODY_VERSION_DRIFT');
  const observedCanonicalF20Sha=canonicalJsonSha256(f20Body);
  if(observedCanonicalF20Sha!==F20_BODY_SHA){
    stop('harness','FOUNDATION_CANONICAL_JSON_ENCODER_MISMATCH');
  }
  const architecture=f20Body?.modules?.find?.(x=>x?.moduleRef===treatmentSpec.moduleRef);
  if(!architecture||!Array.isArray(architecture.kernel)) stop('harness','STATE_EFFECT_ARCHITECTURE_MODULE_MISSING');
  const sentenceIndexes=architecture.kernel
    .map((value,index)=>value===treatmentSpec.expectedKernelSentence?index:-1)
    .filter(index=>index>=0);
  if(sentenceIndexes.length!==1) stop('harness','STATE_EFFECT_EXPECTED_SENTENCE_NOT_EXACT_ONCE');

  const f21Body=JSON.parse(JSON.stringify(f20Body));
  f21Body.version=treatmentSpec.targetVersion;
  const f21Architecture=f21Body.modules.find(x=>x.moduleRef===treatmentSpec.moduleRef);
  f21Architecture.kernel[sentenceIndexes[0]]=treatmentSpec.replacementKernelSentence;
  const f21BodyPath=path.join(WORK_ROOT,'foundation-v0.6.21-state-effect-body.json');
  fs.writeFileSync(f21BodyPath,JSON.stringify(f21Body,null,2)+'\n');
  const f21BodySha=canonicalJsonSha256(f21Body);

  const f20KernelTokens=estimatedFoundationKernelTokens(f20Body);
  const f21KernelTokens=estimatedFoundationKernelTokens(f21Body);
  if(f20KernelTokens!==3598){
    stop('harness','FOUNDATION_KERNEL_ESTIMATOR_DRIFT');
  }
  if(f21KernelTokens>3600){
    stop('harness','F21_FOUNDATION_KERNEL_OVERSIZED');
  }
  if(f21KernelTokens>f20KernelTokens){
    stop('harness','F21_FOUNDATION_KERNEL_GREW');
  }

  if(f20Revisions?.schemaVersion!=='vexlife-prod.foundational-field-revisions/v1'||
     !Array.isArray(f20Revisions?.revisions)||
     f20Revisions.revisions.at(-1)?.version!=='0.6.20'||
     f20Revisions.revisions.at(-1)?.bodySha256!==F20_BODY_SHA){
    stop('harness','F20_REVISIONS_DRIFT');
  }
  const f21Revisions=JSON.parse(JSON.stringify(f20Revisions));
  f21Revisions.revisions.push({
    version:treatmentSpec.targetVersion,
    bodySha256:f21BodySha,
    recordedOn:'2026-11-24',
    recordedBy:'Multi-Vex experimental treatment; not accepted Product source',
    summary:'Keep one state, readiness, or causal step from becoming proof that a later effect or outcome occurred.',
    why:'SC2 selected the current Blue Lantern ready-for-internal-review state but added the unsupported consequence that internal review was no longer needed. This experiment refines one existing Architecture sentence instead of adding a new module or semantic owner.',
  });
  const f21RevisionsPath=path.join(WORK_ROOT,'foundation-v0.6.21-state-effect-revisions.json');
  fs.writeFileSync(f21RevisionsPath,JSON.stringify(f21Revisions,null,2)+'\n');

  afterState.foundationTreatment={
    treatmentRef:treatmentSpec.treatmentRef,
    sourceVersion:'0.6.20',
    sourceBodySha256:F20_BODY_SHA,
    targetVersion:treatmentSpec.targetVersion,
    targetBodySha256:f21BodySha,
    f20KernelTokens,
    f21KernelTokens,
    expectedKernelSentence:treatmentSpec.expectedKernelSentence,
    replacementKernelSentence:treatmentSpec.replacementKernelSentence,
    standing:treatmentSpec.standing,
  };
  completedStepReceipts.push({
    step:'derive-experimental-state-effect-foundation',
    state:'PASS',
    treatmentRef:treatmentSpec.treatmentRef,
    sourceBodySha256:F20_BODY_SHA,
    targetBodySha256:f21BodySha,
    targetVersion:treatmentSpec.targetVersion,
    f20KernelTokens,
    f21KernelTokens,
    automaticAcceptanceAllowed:false,
  });

  function runCell(cellRef,foundationBodyPath,foundationRevisionsPath,resultLeaf){
    const encounterResultRelative=path.join('build','multivex-state-effect-scope-se1',resultLeaf);
    const encounterResultDir=path.join(repoRoot,encounterResultRelative);
    const args=[
      '--runtime-home',HOME,
      '--character-file','tools/continuity-pressure/characters/mira-v1.json',
      '--role-file','tools/continuity-pressure/roles/ordinary-shared-understanding-v1.json',
      '--scenario-file','tools/continuity-pressure/scenario-profiles/bounded-status-note-single-turn-v1.json',
      '--binding-file','tools/continuity-pressure/bindings/mira-bounded-status-single-turn-ordinary-v1.json',
      '--foundation-body-file',foundationBodyPath,
      '--foundation-revisions-file',foundationRevisionsPath,
      '--result-dir',encounterResultRelative,
      '--source-sha',HEAD,
      '--source-clean','true',
      '--experiment-window','true',
      '--runtime-start-at-bind','false',
      '--posture-supplied','false',
      '--role-supplied','true',
      '--first-visible-request-file','tools/continuity-pressure/treatments/sc-resident-current-status-request-v1.txt',
    ].join(' ');

    stage('11 run '+cellRef);
    const lived=runAllow(bash,[
      gradlew,'--no-daemon','--max-workers=1',
      '-Dorg.gradle.jvmargs=-Xmx4g',
      '-Dkotlin.daemon.jvm.options=-Xmx4g',
      'dynamicCounterpartProfileEncounter',
      '--args='+args,
    ],{cwd:repoRoot,env:gradleEnv,label:'11 '+cellRef,timeout:3600000});

    const encounterResultFile=path.join(encounterResultDir,'profile-encounter-result.json');
    if(!fs.existsSync(encounterResultFile)) stop('candidate','STATE_EFFECT_'+cellRef.toUpperCase().replaceAll(/[^A-Z0-9]+/g,'_')+'_RESULT_MISSING');
    const result=readJson(encounterResultFile,'STATE_EFFECT_'+cellRef.toUpperCase().replaceAll(/[^A-Z0-9]+/g,'_')+'_RESULT');
    const checkpointFile=path.join(encounterResultDir,'profile-encounter-checkpoint.json');
    const checkpoint=fs.existsSync(checkpointFile)
      ?readJson(checkpointFile,'STATE_EFFECT_'+cellRef.toUpperCase().replaceAll(/[^A-Z0-9]+/g,'_')+'_CHECKPOINT')
      :null;
    return {
      cellRef,
      lived:{exitCode:lived.status,stdoutTail:String(lived.stdout||'').slice(-32000),stderrTail:String(lived.stderr||'').slice(-32000)},
      result,
      checkpoint,
    };
  }

  const f20Cell=runCell('F20_BASELINE',f20BodyPath,f20RevisionsPath,'f20');
  const f21Cell=runCell('F21_STATE_EFFECT',f21BodyPath,f21RevisionsPath,'f21');
  const f20Result=f20Cell.result;
  const f21Result=f21Cell.result;
  afterState.encounterCells={
    f20:{
      exitCode:f20Cell.lived.exitCode,
      result:f20Result,
      checkpoint:f20Cell.checkpoint,
    },
    f21:{
      exitCode:f21Cell.lived.exitCode,
      result:f21Result,
      checkpoint:f21Cell.checkpoint,
    },
  };

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
    f20:{
      pid:f20Result?.counterpartRuntimePid??null,
      aliveAfter:processAlive(f20Result?.counterpartRuntimePid??null),
    },
    f21:{
      pid:f21Result?.counterpartRuntimePid??null,
      aliveAfter:processAlive(f21Result?.counterpartRuntimePid??null),
    },
  };

  if(afterState.counterpartProcessCleanup.f20.aliveAfter===true||
     afterState.counterpartProcessCleanup.f21.aliveAfter===true){
    candidateFindings.push('SE1_COUNTERPART_PROCESS_LEAK');
  }
  if(!afterState.canonicalConversationUnchanged) candidateFindings.push('CANONICAL_CONVERSATION_CHANGED');
  if(!afterState.canonicalMemoryUnchanged) candidateFindings.push('CANONICAL_MEMORY_CHANGED');
  if(!afterState.selectedModelUnchanged) candidateFindings.push('MODEL_SELECTION_CHANGED');
  if(!afterState.productRuntimeConfigurationUnchanged) candidateFindings.push('PRODUCT_RUNTIME_CONFIGURATION_CHANGED');
  if(!afterState.productRuntimeReceiptUnchanged) candidateFindings.push('PRODUCT_RUNTIME_RECEIPT_CHANGED');

  function validateCell(cellRef,result,livedStatus,expectedVersion,expectedSha){
    const prefix='SE1_'+cellRef+'_';
    if(result?.status!=='PASS'||result?.mechanicalState!=='PASS') candidateFindings.push(prefix+'ENCOUNTER_NOT_PASS');
    if(livedStatus!==0) candidateFindings.push(prefix+'GRADLE_NONZERO');
    if(result?.characterRef!=='character.vexlife.dynamic-counterpart.person-c.v1') candidateFindings.push(prefix+'CHARACTER_REF_MISMATCH');
    if(result?.participantRef!=='person.synthetic.c39a72e1') candidateFindings.push(prefix+'PARTICIPANT_REF_MISMATCH');
    if(result?.participantName!=='Mira') candidateFindings.push(prefix+'DISPLAY_NAME_MISMATCH');
    if(result?.bindingRef!=='binding.dynamic-counterpart.person-c.bounded-status-note.single-turn.ordinary-shared-understanding.v1') candidateFindings.push(prefix+'BINDING_REF_MISMATCH');
    if(result?.roleRef!=='role.dynamic-counterpart.ordinary-shared-understanding.v1') candidateFindings.push(prefix+'ROLE_REF_MISMATCH');
    if(result?.scenarioRef!=='scenario.vexlife.multivex.bounded-status-note.single-turn.v1') candidateFindings.push(prefix+'SCENARIO_REF_MISMATCH');
    if(result?.foundationOverrideSupplied!==true||
       result?.foundationVersion!==expectedVersion||
       result?.foundationBodySha256!==expectedSha){
      candidateFindings.push(prefix+'FOUNDATION_BINDING_MISMATCH');
    }
    if(result?.roleSupplied!==true) candidateFindings.push(prefix+'ROLE_TREATMENT_NOT_SUPPLIED');
    if(result?.postureSupplied!==false) candidateFindings.push(prefix+'POSTURE_TREATMENT_PRESENT');
    if(result?.observerLensSupplied!==false) candidateFindings.push(prefix+'OBSERVER_LENS_PRESENT');
    if(result?.participantAttributionExact!==true) candidateFindings.push(prefix+'PARTICIPANT_ATTRIBUTION_NOT_EXACT');
    if(result?.personContextProjectionExact!==true) candidateFindings.push(prefix+'PERSON_CONTEXT_NOT_EXACT');
    if(result?.visibleSourceExact!==true) candidateFindings.push(prefix+'VISIBLE_SOURCE_NOT_EXACT');
    if(result?.processIsolationExact!==true) candidateFindings.push(prefix+'PROCESS_ISOLATION_NOT_EXACT');
    if(result?.vexRuntimeStillCurrent!==true) candidateFindings.push(prefix+'VEX_RUNTIME_NOT_CURRENT');
    if(result?.canonicalWriterStayedIdle!==true) candidateFindings.push(prefix+'CANONICAL_WRITER_NOT_IDLE');
    if(result?.firstVisibleRequestCarrierSupplied!==true) candidateFindings.push(prefix+'FIRST_VISIBLE_CARRIER_NOT_SUPPLIED');
    if(result?.firstVisibleRequestCarrierApplied!==true) candidateFindings.push(prefix+'FIRST_VISIBLE_CARRIER_NOT_APPLIED');
    if(result?.firstVisibleRequestCarrierSha256!=='a8a8ac84eb1b8060d6f5acad8537994793b30538c5ed91f5a06a2433f2c1026a') candidateFindings.push(prefix+'FIRST_VISIBLE_CARRIER_SHA_MISMATCH');
    if(typeof result?.firstVisibleParticipantGeneratedMessage!=='string') candidateFindings.push(prefix+'RAW_FIRST_VISIBLE_MESSAGE_EVIDENCE_MISSING');
    if(typeof result?.firstVisibleParticipantGeneratedComplete!=='boolean') candidateFindings.push(prefix+'RAW_FIRST_VISIBLE_COMPLETE_EVIDENCE_MISSING');
    if(typeof result?.firstVisibleParticipantGeneratedCompletionReason!=='string') candidateFindings.push(prefix+'RAW_FIRST_VISIBLE_COMPLETION_REASON_EVIDENCE_MISSING');
    const participantInferences=Array.isArray(result?.participantInferences)?result.participantInferences:[];
    const firstParticipant=participantInferences[0]??null;
    if(!firstParticipant) candidateFindings.push(prefix+'FIRST_PARTICIPANT_INFERENCE_MISSING');
    if(firstParticipant?.visibleMessage!=="What's the current Blue Lantern status? Please answer in your own words.") candidateFindings.push(prefix+'EFFECTIVE_FIRST_VISIBLE_REQUEST_MISMATCH');
    if(firstParticipant?.complete!==false) candidateFindings.push(prefix+'CONTROLLED_FIRST_VISIBLE_NOT_OPEN');
    if(firstParticipant?.completionReason!=="") candidateFindings.push(prefix+'CONTROLLED_FIRST_VISIBLE_COMPLETION_REASON_NOT_CLEARED');
    const inferences=Array.isArray(result?.vexInferences)?result.vexInferences:[];
    const first=inferences[0]??null;
    if(!first) candidateFindings.push(prefix+'FIRST_VEX_INFERENCE_MISSING');
    if(inferences.length!==1) candidateFindings.push(prefix+'NOT_EXACTLY_ONE_VEX_INFERENCE');
    if(result?.encounterTermination!=='VISIBLE_TURN_BUDGET_EXHAUSTED') candidateFindings.push(prefix+'NOT_SINGLE_TURN_TERMINATION');
    if(first&&!Array.isArray(first.selectedConversationEventRefs)) candidateFindings.push(prefix+'SELECTED_CONVERSATION_EVIDENCE_MISSING');
    const oldResident=first?.selectedConversationEventRefs?.includes?.(result?.setupOldPlanRequestEventRef)??false;
    const correctedResident=first?.selectedConversationEventRefs?.includes?.(result?.setupCorrectedPlanRequestEventRef)??false;
    if(!oldResident) candidateFindings.push(prefix+'OLD_PLAN_NOT_RESIDENT');
    if(!correctedResident) candidateFindings.push(prefix+'CORRECTED_PLAN_NOT_RESIDENT');
    return {
      first,
      oldResident,
      correctedResident,
    };
  }

  const f20Evidence=validateCell('F20',f20Result,f20Cell.lived.exitCode,'0.6.20',F20_BODY_SHA);
  const f21Evidence=validateCell('F21',f21Result,f21Cell.lived.exitCode,treatmentSpec.targetVersion,f21BodySha);

  completedStepReceipts.push({
    step:'multivex-state-effect-scope-se1',
    state:candidateFindings.length===0?'PASS':'SAFE_FAILURE',
    treatmentRef:treatmentSpec.treatmentRef,
    treatmentStanding:treatmentSpec.standing,
    observedCanonicalF20Sha,
    f21CanonicalBodySha256:f21BodySha,
    f20KernelTokens,
    f21KernelTokens,
    f20:{
      foundationVersion:f20Result?.foundationVersion??null,
      foundationBodySha256:f20Result?.foundationBodySha256??null,
      firstVexContent:f20Evidence.first?.content??null,
      oldPlanResident:f20Evidence.oldResident,
      correctedPlanResident:f20Evidence.correctedResident,
      sourceReturnProtocolEnabled:f20Evidence.first?.sourceReturnProtocolEnabled??null,
      sourceReturnState:f20Evidence.first?.sourceReturnState??null,
      sourceReturnSourceEventRefs:f20Evidence.first?.sourceReturnSourceEventRefs??[],
      rawParticipantGeneratedMessage:f20Result?.firstVisibleParticipantGeneratedMessage??null,
      rawParticipantGeneratedComplete:f20Result?.firstVisibleParticipantGeneratedComplete??null,
      rawParticipantGeneratedCompletionReason:f20Result?.firstVisibleParticipantGeneratedCompletionReason??null,
    },
    f21:{
      foundationVersion:f21Result?.foundationVersion??null,
      foundationBodySha256:f21Result?.foundationBodySha256??null,
      firstVexContent:f21Evidence.first?.content??null,
      oldPlanResident:f21Evidence.oldResident,
      correctedPlanResident:f21Evidence.correctedResident,
      sourceReturnProtocolEnabled:f21Evidence.first?.sourceReturnProtocolEnabled??null,
      sourceReturnState:f21Evidence.first?.sourceReturnState??null,
      sourceReturnSourceEventRefs:f21Evidence.first?.sourceReturnSourceEventRefs??[],
      rawParticipantGeneratedMessage:f21Result?.firstVisibleParticipantGeneratedMessage??null,
      rawParticipantGeneratedComplete:f21Result?.firstVisibleParticipantGeneratedComplete??null,
      rawParticipantGeneratedCompletionReason:f21Result?.firstVisibleParticipantGeneratedCompletionReason??null,
    },
    productPromptCacheEntriesBefore:beforeState.productRuntimeConfiguration.selectedModelPromptCacheEntries,
    productPromptCacheEntriesAfter:afterState.productRuntimeConfiguration.selectedModelPromptCacheEntries,
    productRuntimeConfigurationUnchanged:afterState.productRuntimeConfigurationUnchanged,
    productRuntimeReceiptUnchanged:afterState.productRuntimeReceiptUnchanged,
    semanticJudgment:"HUMAN_GODWARD_REVIEW_REQUIRED: SE1 A005 compares one-response matched cells after repairing controlled-first-carrier ownership. Raw participant completion is retained as evidence but cannot veto the experiment-owned visible request. Mechanical PASS does not accept v0.6.21 or score Vex.",
  });

  if(candidateFindings.length===0){
    stage('12 SE1 state-effect crossover evidence captured; semantic/currentness review remains human/Godward');
    writeResult('PASS',null);
  }else{
    stage('12 SE1 state-effect crossover SAFE_FAILURE');
    writeResult('SAFE_FAILURE','MULTI_VEX_STATE_EFFECT_SCOPE_SE1_ACCEPTANCE_NOT_MET');
  }
}catch(error){
  if(error instanceof Stop){
    addFinding(error.category,error.code);
    stage('STOP '+error.code);
    writeResult('SAFE_FAILURE',error.code);
  }else{
    harnessFindings.push('MULTI_VEX_STATE_EFFECT_SCOPE_SE1_UNCAUGHT_'+String(error?.name||'ERROR'));
    afterState.uncaughtClass=String(error?.name||'Error');
    afterState.uncaughtMessage=String(error?.message||'');
    stage('STOP MULTI_VEX_STATE_EFFECT_SCOPE_SE1_UNCAUGHT');
    writeResult('SAFE_FAILURE','MULTI_VEX_STATE_EFFECT_SCOPE_SE1_UNCAUGHT');
  }
}
