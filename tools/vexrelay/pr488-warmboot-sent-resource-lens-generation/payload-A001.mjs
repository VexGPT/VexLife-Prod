import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import crypto from 'node:crypto';
import { spawnSync } from 'node:child_process';

const BASE='0566b1eed876a28c2fcc6586743ae1f4af7eaecd';
const HEAD='d588292abb4192b0b6d6dd752d0f4ad076cc51d5';
const TREE='124d98bae17ef128f61ac0570a44db729e1d29e0';
const BRANCH='VXG-112126-chatgpt-sent-resource-continuity-current-main-a001';
const REPOSITORY='VexGPT/VexLife-Prod';
const LENS_PATH='docs/lens/vexlife-prod.screen-lens.json';
const PREVIOUS_LENS_BLOB='375381414accfffed71f5bd4a014f6a841ebd118';
const TASK_REF='task.vexlife-prod.pr488.warmboot-sent-resource.lens-generation.a001';
const ATTEMPT_PREFIX='attempt.vexlife-prod.pr488.warmboot-sent-resource.lens-generation.a001.';
const RETURN_ROUTE_REF='github.pull.vexlife-prod.488';

const RESULT_DIR=process.env.VEXLOCALBRIDGE_RESULT_DIR;
const PAYLOAD_ROOT=process.env.VEXLOCALBRIDGE_PAYLOAD_ROOT || process.cwd();
const RESULT_FILE=RESULT_DIR && path.join(RESULT_DIR,'payload-result.json');
const WORK_ROOT=RESULT_DIR && path.join(RESULT_DIR,'lens-generation-work');

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
const beforeState={candidate:{base:BASE,head:HEAD,tree:TREE,branch:BRANCH},lens:{path:LENS_PATH,blob:PREVIOUS_LENS_BLOB}};
const afterState={};
const completedStepReceipts=[];
const candidateFindings=[];
const harnessFindings=[];
const environmentFindings=[];
const unknownFindings=[];

class Stop extends Error { constructor(category,code){super(code);this.category=category;this.code=code;} }
const stop=(category,code)=>{throw new Stop(category,code);};
function sha256Bytes(bytes){return crypto.createHash('sha256').update(bytes).digest('hex');}
function findExe(candidates,label){
  for(const candidate of candidates){try{fs.accessSync(candidate,fs.constants.X_OK);if(fs.statSync(candidate).isFile())return candidate;}catch{}}
  stop('environment',label+'_UNAVAILABLE');
}
function run(exe,args,{cwd=WORK_ROOT,env=process.env,timeout=120000,label='COMMAND'}={}){
  const r=spawnSync(exe,args,{cwd,env,encoding:'utf8',timeout,maxBuffer:64*1024*1024,stdio:['ignore','pipe','pipe']});
  if(r.error) stop('environment',label+'_SPAWN_FAILED');
  if(r.signal) stop('environment',label+'_SIGNALLED');
  if(r.status!==0){afterState.lastCommand={label,exitCode:r.status,stdoutTail:String(r.stdout||'').slice(-4000),stderrTail:String(r.stderr||'').slice(-4000)};stop('environment',label+'_FAILED');}
  return r;
}
function addFinding(category,code){
  if(category==='candidate')candidateFindings.push(code);else if(category==='harness')harnessFindings.push(code);else if(category==='environment')environmentFindings.push(code);else unknownFindings.push(code);
}
function writeResult(disposition,stopReasonOrNull=null){
  fs.mkdirSync(RESULT_DIR,{recursive:true});
  fs.writeFileSync(RESULT_FILE,JSON.stringify({
    schemaVersion:'vextreme.vexlocalbridge-mac-payload-result/v1',
    disposition,stopReasonOrNull,acceptedExitCodes:[0],
    candidateFindings:[...new Set(candidateFindings)],harnessFindings:[...new Set(harnessFindings)],environmentFindings:[...new Set(environmentFindings)],unknownFindings:[...new Set(unknownFindings)],
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
  const bindingPath=path.join(PAYLOAD_ROOT,'inputs','lens-generation-binding.json');
  if(!fs.existsSync(bindingPath)) stop('harness','LENS_GENERATION_BINDING_MISSING');
  const binding=JSON.parse(fs.readFileSync(bindingPath,'utf8'));
  if(taskRef!==TASK_REF||typeof attemptRef!=='string'||!attemptRef.startsWith(ATTEMPT_PREFIX)||returnRouteRef!==RETURN_ROUTE_REF) stop('harness','TASK_ENV_BINDING_MISMATCH');
  if(binding?.candidate?.base!==BASE||binding?.candidate?.head!==HEAD||binding?.candidate?.tree!==TREE||binding?.candidate?.branch!==BRANCH) stop('harness','IMMUTABLE_CANDIDATE_BINDING_MISMATCH');
  if(policy?.repository!==REPOSITORY||policy?.candidate?.head!==HEAD||policy?.candidate?.tree!==TREE||policy?.hostProfileRef!=='macos-victor'||policy?.repositoryExecutionProfileRef!=='profile.repository.vexlife-prod.macos-v1') stop('harness','PRODUCT_HOST_EFFECT_POLICY_BINDING_MISMATCH');
  for(const effectRef of ['effect.vexlocalbridge.vexlife-prod.product-lifecycle-proof/v1','effect.vexlocalbridge.vexlife-prod.build-dependency-fetch/v1']) if(!allowed.includes(effectRef)) stop('harness','REQUIRED_HOST_EFFECT_REF_MISSING');
  if(policy.buildDependencyFetchAuthorized!==true||policy.productDataPolicy?.conversationMutationAllowed!==false||policy.productDataPolicy?.memoryMutationAllowed!==false||policy.productDataPolicy?.modelAcquisitionAllowed!==false||policy.productDataPolicy?.modelSwitchForProofAllowed!==false) stop('harness','PRODUCT_HOST_EFFECT_POLICY_WIDENED_OR_INCOMPLETE');

  const gh=findExe(['/opt/homebrew/bin/gh','/usr/local/bin/gh'],'GITHUB_CLI');
  const git=findExe(['/usr/bin/git'],'GIT');
  const bash=findExe(['/bin/bash'],'BASH');
  run(gh,['auth','status','--hostname','github.com'],{label:'GITHUB_AUTH',timeout:30000});

  const repoRoot=path.join(WORK_ROOT,'VexLife-Prod');
  effects.providerOrNetworkEffectPerformed=true;
  run(gh,['repo','clone',REPOSITORY,repoRoot,'--','--filter=blob:none','--no-checkout'],{cwd:WORK_ROOT,label:'PRODUCT_CLONE',timeout:300000});
  run(git,['checkout','--quiet','--detach',HEAD],{cwd:repoRoot,label:'CANDIDATE_CHECKOUT',timeout:60000});
  const observedHead=run(git,['rev-parse','HEAD'],{cwd:repoRoot,label:'CANDIDATE_HEAD'}).stdout.trim();
  const observedTree=run(git,['rev-parse','HEAD^{tree}'],{cwd:repoRoot,label:'CANDIDATE_TREE'}).stdout.trim();
  const observedLensBlob=run(git,['rev-parse',`HEAD:${LENS_PATH}`],{cwd:repoRoot,label:'PREVIOUS_LENS_BLOB'}).stdout.trim();
  if(observedHead!==HEAD||observedTree!==TREE||observedLensBlob!==PREVIOUS_LENS_BLOB) stop('harness','CANDIDATE_SOURCE_IDENTITY_DRIFT');
  if(spawnSync(git,['merge-base','--is-ancestor',BASE,HEAD],{cwd:repoRoot}).status!==0) stop('harness','CANDIDATE_BASE_NOT_ANCESTOR');
  if(run(git,['status','--porcelain=v1','--untracked-files=all'],{cwd:repoRoot,label:'CLEAN_BEFORE'}).stdout.trim()!=='') stop('harness','CANDIDATE_CHECKOUT_DIRTY_BEFORE_GENERATION');

  const javaHomes=[];
  const addJavaHome=(value)=>{if(value&&fs.existsSync(path.join(value,'bin','java'))&&fs.existsSync(path.join(value,'bin','javac'))&&!javaHomes.includes(value))javaHomes.push(value);};
  addJavaHome(process.env.JAVA_HOME);
  for(const version of ['21','17']){const jh=spawnSync('/usr/libexec/java_home',['-v',version],{encoding:'utf8',timeout:10000});if(jh.status===0)addJavaHome(String(jh.stdout||'').trim());}
  addJavaHome('/Applications/Android Studio.app/Contents/jbr/Contents/Home');
  addJavaHome('/Applications/Android Studio Preview.app/Contents/jbr/Contents/Home');
  let jdk=null;
  for(const home of javaHomes){if(spawnSync(path.join(home,'bin','java'),['-version'],{encoding:'utf8',timeout:10000}).status===0){jdk=home;break;}}
  if(!jdk) stop('environment','GRADLE_CAPABLE_JAVA_UNAVAILABLE');

  const gradleUserHome=path.join(WORK_ROOT,'gradle-home');
  fs.mkdirSync(gradleUserHome,{recursive:true});
  fs.writeFileSync(path.join(gradleUserHome,'gradle.properties'),'org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m\n'+'kotlin.daemon.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m\n');
  const env={...process.env,JAVA_HOME:jdk,GRADLE_USER_HOME:gradleUserHome};
  const gradlew=path.join(repoRoot,'gradlew');
  if(!fs.existsSync(gradlew)) stop('harness','GRADLE_WRAPPER_MISSING');
  run(bash,[gradlew,'--no-daemon','--max-workers=1','-Dorg.gradle.jvmargs=-Xmx4g','-Dkotlin.daemon.jvm.options=-Xmx4g','generateVexLifeProdScreenLens'],{cwd:repoRoot,env,label:'GENERATE_SCREEN_LENS',timeout:1800000});

  const status=run(git,['status','--porcelain=v1','--untracked-files=all'],{cwd:repoRoot,label:'STATUS_AFTER_GENERATION'}).stdout.trim().split(/\r?\n/).filter(Boolean);
  if(status.length!==1||!status[0].endsWith(` ${LENS_PATH}`)) { afterState.observedStatus=status; stop('candidate','GENERATOR_CHANGED_UNEXPECTED_PATHS'); }
  const lensFile=path.join(repoRoot,LENS_PATH);
  if(!fs.existsSync(lensFile)||!fs.statSync(lensFile).isFile()) stop('candidate','GENERATED_SCREEN_LENS_MISSING');
  const bytes=fs.readFileSync(lensFile);
  let parsed;
  try{parsed=JSON.parse(bytes.toString('utf8'));}catch{stop('candidate','GENERATED_SCREEN_LENS_JSON_INVALID');}
  if(parsed?.schemaVersion!=='vexboard.screen-lens/v1'||parsed?.lensRef!=='lens.vexlife-prod'||parsed?.provenance?.generator!=='vexlife-prod-blueprint-export/1') stop('candidate','GENERATED_SCREEN_LENS_IDENTITY_INVALID');
  const blob=run(git,['hash-object',LENS_PATH],{cwd:repoRoot,label:'GENERATED_LENS_GIT_BLOB'}).stdout.trim();
  const sha256=sha256Bytes(bytes);
  afterState.generatedScreenLens={path:LENS_PATH,byteLength:bytes.length,sha256,gitBlobSha:blob,utf8Base64:bytes.toString('base64')};
  afterState.scratchGeneration={sourceHead:HEAD,sourceTree:TREE,previousLensBlob:PREVIOUS_LENS_BLOB,workingTreeStatus:status};
  completedStepReceipts.push({step:'product-owned-screen-lens-generation',state:'PASS',path:LENS_PATH,byteLength:bytes.length,sha256,gitBlobSha:blob});
  writeResult('PASS',null);
}catch(error){
  if(error instanceof Stop){addFinding(error.category,error.code);writeResult('SAFE_FAILURE',error.code);}
  else{harnessFindings.push('LENS_GENERATION_UNCAUGHT_'+String(error?.name||'ERROR'));afterState.uncaughtClass=String(error?.name||'Error');afterState.uncaughtMessage=String(error?.message||'');writeResult('SAFE_FAILURE','LENS_GENERATION_UNCAUGHT');}
}
