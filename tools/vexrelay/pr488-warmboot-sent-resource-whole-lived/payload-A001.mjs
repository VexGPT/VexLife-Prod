import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import crypto from 'node:crypto';
import http from 'node:http';
import { spawnSync } from 'node:child_process';

const BASE='0566b1eed876a28c2fcc6586743ae1f4af7eaecd';
const HEAD='a23b6a949d27e4b77530273a390955f0d127ad1a';
const TREE='a268826bf26eecb3a2136593b202cf651643d4e9';
const BRANCH='VXG-112126-chatgpt-sent-resource-continuity-current-main-a001';
const REPOSITORY='VexGPT/VexLife-Prod';
const FOUR_B_HANDLE='heretic-4b';
const FOUR_B_MODEL_REF='model.vexlife-prod.heretic.qwen3.8-4b-distill.mlx4.d2f8f4028f20';
const SERVICE_ORIGIN='http://127.0.0.1:18210';
const SOURCE_PATH='/api/v1/source-currentness';
const TURN_PATH='/api/v1/conversation/turn';
const CONVERSATION_PATH='/api/v1/conversation';
const ATTACHMENT_DRAFT_PATH='/api/v1/attachments/draft';
const RESOURCE_METADATA_PATH='/api/v1/conversation/resource/metadata';
const RESOURCE_CONTENT_PATH='/api/v1/conversation/resource/content';
const RUNTIME_RECEIPT_RELATIVE='runtime/vexlife-prod/heretic-persistent-runtime-owner-receipt.json';
const CHRONOLOGY_RELATIVE='runtime/vexlife-prod/logs/chronology';
const TASK_REF='task.vexlife-prod.pr488.warmboot-sent-resource.whole-lived.a001';
const ATTEMPT_PREFIX='attempt.vexlife-prod.pr488.warmboot-sent-resource.whole-lived.a001.';
const RETURN_ROUTE_REF='github.pull.vexlife-prod.488';
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
  sourceMutationPerformed:false, branchMutationPerformed:false, claimMutationPerformed:false,
  prMetadataMutationPerformed:false, reviewPerformed:false, approvalPerformed:false,
  readyTransitionPerformed:false, mergePerformed:false, mainMutationPerformed:false,
  checkpointMutationPerformed:false, forcePushPerformed:false, historyRewritePerformed:false,
  hostInstallationPerformed:false, modelRuntimePerformed:false, providerOrNetworkEffectPerformed:false,
  publicationPerformed:false, credentialSerializationPerformed:false,
  filesystemOutsideQualifiedRootsPerformed:false,
};
const beforeState={candidate:{base:BASE,head:HEAD,tree:TREE,branch:BRANCH}};
const afterState={};
const completedStepReceipts=[];
const candidateFindings=[];
const harnessFindings=[];
const environmentFindings=[];
const unknownFindings=[];

class LivedStop extends Error { constructor(category,code){ super(code); this.category=category; this.code=code; } }
const stop=(category,code)=>{ throw new LivedStop(category,code); };
const sleep=(ms)=>new Promise(resolve=>setTimeout(resolve,ms));
const sha256Bytes=(bytes)=>crypto.createHash('sha256').update(bytes).digest('hex');
const sha256File=(file)=>sha256Bytes(fs.readFileSync(file));
function aggregateTree(root){
  if(!fs.existsSync(root)) return sha256Bytes(Buffer.from('ABSENT','utf8'));
  const entries=[];
  const walk=(dir,relative)=>{
    for(const child of fs.readdirSync(dir,{withFileTypes:true}).sort((a,b)=>a.name.localeCompare(b.name))){
      const absolute=path.join(dir,child.name); const rel=relative?path.posix.join(relative,child.name):child.name;
      const stat=fs.lstatSync(absolute);
      if(stat.isSymbolicLink()) stop('environment','PROTECTED_TREE_SYMLINK_REJECTED');
      if(stat.isDirectory()) walk(absolute,rel); else if(stat.isFile()) entries.push(rel+'\0'+sha256File(absolute));
    }
  };
  walk(root,''); return sha256Bytes(Buffer.from(entries.join('\n'),'utf8'));
}
function findExe(candidates,label){ for(const c of candidates){ try{fs.accessSync(c,fs.constants.X_OK);if(fs.statSync(c).isFile())return c;}catch{} } stop('environment',label+'_UNAVAILABLE'); }
function run(exe,args,{cwd=WORK_ROOT,env=process.env,timeout=120000,label='COMMAND'}={}){
  const r=spawnSync(exe,args,{cwd,env,encoding:'utf8',timeout,maxBuffer:64*1024*1024,stdio:['ignore','pipe','pipe']});
  if(r.error) stop('environment',label+'_SPAWN_FAILED'); if(r.signal) stop('environment',label+'_SIGNALLED');
  if(r.status!==0){afterState.lastCommand={label,exitCode:r.status,stdoutTail:String(r.stdout||'').slice(-3000),stderrTail:String(r.stderr||'').slice(-3000)};stop('environment',label+'_FAILED');}
  return r;
}
function processAlive(pid){try{process.kill(pid,0);return true;}catch{return false;}}
async function requestBuffer(url,{headers={},timeoutMs=5000}={}){
  return await new Promise((resolve,reject)=>{
    const req=http.get(url,{headers},res=>{
      const chunks=[];let total=0;
      res.on('data',chunk=>{total+=chunk.length;if(total>16*1024*1024){req.destroy(new Error('HTTP_BODY_TOO_LARGE'));return;}chunks.push(chunk);});
      res.on('end',()=>{
        const body=Buffer.concat(chunks);
        if(res.statusCode<200||res.statusCode>299){reject(new Error('HTTP_'+res.statusCode+' '+body.toString('utf8').slice(0,1000)));return;}
        resolve({body,headers:res.headers,statusCode:res.statusCode});
      });
    });
    req.setTimeout(timeoutMs,()=>req.destroy(new Error('HTTP_TIMEOUT')));req.on('error',reject);
  });
}
async function getJson(url,timeoutMs=5000,headers={}){const r=await requestBuffer(url,{headers,timeoutMs});return JSON.parse(r.body.toString('utf8'));}
async function getText(url,timeoutMs=5000){const r=await requestBuffer(url,{timeoutMs});return r.body.toString('utf8');}
async function waitJson(url,predicate,timeoutMs,label){const end=Date.now()+timeoutMs;let last=null;while(Date.now()<end){try{const v=await getJson(url);last=v;if(predicate(v))return v;}catch(e){last={error:e.message};}await sleep(500);}afterState[label+'LastObservation']=last;stop('environment',label+'_TIMEOUT');}
function readRuntimeReceipt(){
  const file=path.join(HOME,RUNTIME_RECEIPT_RELATIVE);if(!fs.existsSync(file))stop('environment','RUNTIME_RECEIPT_MISSING');
  let r;try{r=JSON.parse(fs.readFileSync(file,'utf8'));}catch{stop('environment','RUNTIME_RECEIPT_INVALID');}
  if(typeof r.receiptRef!=='string'||!r.runtime||!Number.isInteger(r.runtime.pid))stop('environment','RUNTIME_RECEIPT_SHAPE_INVALID');
  const args=Array.isArray(r.runtime.args)?r.runtime.args:[];const i=args.indexOf('--prompt-cache-size');
  return {receiptRef:r.receiptRef,pid:r.runtime.pid,modelRef:r.modelRef??null,modelProfileRef:r.modelProfileRef??null,promptCacheEntries:i>=0&&i+1<args.length?Number(args[i+1]):null};
}
function chronologyRecords(servicePid){
  const dir=path.join(HOME,CHRONOLOGY_RELATIVE);if(!fs.existsSync(dir))return[];const records=[];
  for(const ent of fs.readdirSync(dir,{withFileTypes:true}).filter(e=>e.isFile()&&e.name.startsWith('chronology')&&e.name.endsWith('.jsonl')).sort((a,b)=>a.name.localeCompare(b.name))){
    for(const line of fs.readFileSync(path.join(dir,ent.name),'utf8').split(/\r?\n/)){if(!line.trim())continue;try{const r=JSON.parse(line);if(r.sourceSha===HEAD&&r.processPid===servicePid&&Number.isInteger(r.sequence))records.push(r);}catch{}}
  }
  return records.sort((a,b)=>a.sequence-b.sequence);
}
async function waitForRecord(servicePid,predicate,afterSequence,timeoutMs,label){const end=Date.now()+timeoutMs;while(Date.now()<end){const f=chronologyRecords(servicePid).filter(r=>r.sequence>afterSequence).find(predicate);if(f)return f;await sleep(500);}afterState[label+'Facts']=chronologyRecords(servicePid).filter(r=>r.sequence>afterSequence).slice(-30).map(r=>({sequence:r.sequence,fact:r.fact,state:r.refs?.state??null,turnRef:r.refs?.turnRef??null}));stop('environment',label+'_TIMEOUT');}
async function waitPreparationReady(servicePid,afterSequence,timeoutMs){const end=Date.now()+timeoutMs;const terminal=new Set(['READY_REUSABLE','HELD','FAILED','INVALIDATED']);while(Date.now()<end){const r=chronologyRecords(servicePid).filter(x=>x.sequence>afterSequence&&x.fact===PREPARATION_FACT).find(x=>terminal.has(x.refs?.state));if(r)return r;await sleep(300);}stop('candidate','PROMPT_PREPARATION_TERMINAL_NOT_OBSERVED');}
function metricsFrom(r){const refs=r.refs||{};const num=k=>refs[k]===undefined?null:(Number.isFinite(Number(refs[k]))?Number(refs[k]):null);return{sequence:r.sequence,runtimeReceiptRef:refs.runtimeReceiptRef??null,stop:refs.stop??null,promptTokens:num('promptTokens'),cachedPromptTokens:num('cachedPromptTokens'),prefillTokens:num('prefillTokens'),completionTokens:num('completionTokens'),firstTokenMs:num('firstTokenMs'),firstAnswerMs:num('firstAnswerMs'),totalMs:num('totalMs')};}
function resourceHeaders(message,resource){return{'x-vexlife-request-event-ref':String(message.eventRef??''),'x-vexlife-request-event-hash':String(message.eventSha256??''),'x-vexlife-request-sequence':String(Number(message.sequence)||0),'x-vexlife-request-resource-ref':String(resource.resourceRef??'')};}
function addFinding(category,code){if(category==='candidate')candidateFindings.push(code);else if(category==='harness')harnessFindings.push(code);else if(category==='environment')environmentFindings.push(code);else unknownFindings.push(code);}
function writeResult(disposition,stopReasonOrNull=null){fs.mkdirSync(RESULT_DIR,{recursive:true});fs.writeFileSync(RESULT_FILE,JSON.stringify({schemaVersion:'vextreme.vexlocalbridge-mac-payload-result/v1',disposition,stopReasonOrNull,acceptedExitCodes:[0],candidateFindings:[...new Set(candidateFindings)],harnessFindings:[...new Set(harnessFindings)],environmentFindings:[...new Set(environmentFindings)],unknownFindings:[...new Set(unknownFindings)],effects,beforeState,afterState,completedStepReceipts},null,2)+'\n');}
function compactMessage(m){return{eventRef:m.eventRef??null,eventSha256:m.eventSha256??null,sequence:m.sequence??null,turnRef:m.turnRef??null,role:m.role??null,contentSha256:sha256Bytes(Buffer.from(String(m.content??''),'utf8')),resourceCount:Array.isArray(m.resources)?m.resources.length:0,resourceRefs:Array.isArray(m.resources)?m.resources.map(x=>x.resourceRef):[]};}

try{
  if(!RESULT_DIR||!PAYLOAD_ROOT)stop('harness','VEXLOCALBRIDGE_PAYLOAD_ENV_MISSING');fs.mkdirSync(WORK_ROOT,{recursive:true});
  const taskRef=process.env.VEXLOCALBRIDGE_TASK_REF||null,attemptRef=process.env.VEXLOCALBRIDGE_ATTEMPT_REF||null,returnRouteRef=process.env.VEXLOCALBRIDGE_RETURN_ROUTE_REF||null;
  const policy=JSON.parse(process.env.VEXLOCALBRIDGE_VEXLIFE_PROD_HOST_EFFECT_POLICY_JSON||'null');const allowed=JSON.parse(process.env.VEXLOCALBRIDGE_AUTHORIZED_EFFECT_REFS_JSON||'[]');
  const bindingPath=path.join(PAYLOAD_ROOT,'inputs','lived-binding.json');if(!fs.existsSync(bindingPath))stop('harness','LIVED_BINDING_MISSING');const binding=JSON.parse(fs.readFileSync(bindingPath,'utf8'));
  if(taskRef!==TASK_REF||typeof attemptRef!=='string'||!attemptRef.startsWith(ATTEMPT_PREFIX)||returnRouteRef!==RETURN_ROUTE_REF)stop('harness','TASK_ENV_BINDING_MISMATCH');
  if(binding?.candidate?.base!==BASE||binding?.candidate?.head!==HEAD||binding?.candidate?.tree!==TREE||binding?.candidate?.branch!==BRANCH)stop('harness','IMMUTABLE_LIVED_BINDING_MISMATCH');
  if(policy?.repository!==REPOSITORY||policy?.candidate?.head!==HEAD||policy?.candidate?.tree!==TREE||policy?.hostProfileRef!=='macos-victor'||policy?.repositoryExecutionProfileRef!=='profile.repository.vexlife-prod.macos-v1')stop('harness','PRODUCT_HOST_EFFECT_POLICY_BINDING_MISMATCH');
  for(const e of ['effect.vexlocalbridge.vexlife-prod.product-lifecycle-proof/v1','effect.vexlocalbridge.vexlife-prod.build-dependency-fetch/v1'])if(!allowed.includes(e))stop('harness','REQUIRED_HOST_EFFECT_REF_MISSING');
  if(policy.buildDependencyFetchAuthorized!==true||policy.productDataPolicy?.conversationMutationAllowed!==false||policy.productDataPolicy?.memoryMutationAllowed!==false||policy.productDataPolicy?.modelAcquisitionAllowed!==false||policy.productDataPolicy?.modelSwitchForProofAllowed!==false||policy.productDataPolicy?.runtimeLifecycleMutationAllowed!==true||policy.productDataPolicy?.serviceLifetimeMutationAllowed!==true)stop('harness','PRODUCT_HOST_EFFECT_POLICY_WIDENED_OR_INCOMPLETE');
  if(!fs.existsSync(HOME)||!fs.statSync(HOME).isDirectory())stop('environment','DEFAULT_VEXLIFE_HOME_MISSING');

  const gh=findExe(['/opt/homebrew/bin/gh','/usr/local/bin/gh'],'GITHUB_CLI'),git=findExe(['/usr/bin/git'],'GIT'),bash=findExe(['/bin/bash'],'BASH'),open=findExe(['/usr/bin/open'],'OPEN');
  run(gh,['auth','status','--hostname','github.com'],{label:'GITHUB_AUTH',timeout:30000});
  const initialTurn=await getJson(SERVICE_ORIGIN+TURN_PATH,2500).catch(()=>null);beforeState.initialTurnProjection=initialTurn;if(initialTurn&&['ACTIVE','CANCELLING'].includes(String(initialTurn.state||'')))stop('environment','CONVERSATION_TURN_ACTIVE_BEFORE_LIVED');
  const baselineConversation=await getJson(SERVICE_ORIGIN+CONVERSATION_PATH,2500).catch(()=>null);beforeState.baselineConversationHead=baselineConversation?.currentConversationHeadSha256??null;beforeState.baselineMessageCount=Array.isArray(baselineConversation?.messages)?baselineConversation.messages.length:null;
  const protectedBefore={conversations:aggregateTree(path.join(HOME,'conversations')),memory:aggregateTree(path.join(HOME,'memory'))};beforeState.protectedBeforeConvergence=protectedBefore;

  const repoRoot=path.join(WORK_ROOT,'VexLife-Prod');effects.providerOrNetworkEffectPerformed=true;run(gh,['repo','clone',REPOSITORY,repoRoot,'--','--filter=blob:none','--no-checkout'],{cwd:WORK_ROOT,label:'PRODUCT_CLONE',timeout:300000});
  run(git,['checkout','--quiet','--detach',HEAD],{cwd:repoRoot,label:'CANDIDATE_CHECKOUT',timeout:60000});
  const oh=run(git,['rev-parse','HEAD'],{cwd:repoRoot,label:'CANDIDATE_HEAD'}).stdout.trim(),ot=run(git,['rev-parse','HEAD^{tree}'],{cwd:repoRoot,label:'CANDIDATE_TREE'}).stdout.trim();if(oh!==HEAD||ot!==TREE)stop('harness','CANDIDATE_SOURCE_IDENTITY_DRIFT');
  if(spawnSync(git,['merge-base','--is-ancestor',BASE,HEAD],{cwd:repoRoot}).status!==0)stop('harness','CANDIDATE_BASE_NOT_ANCESTOR');

  const javaHomes=[];const addJ=v=>{if(v&&fs.existsSync(path.join(v,'bin','java'))&&fs.existsSync(path.join(v,'bin','javac'))&&!javaHomes.includes(v))javaHomes.push(v)};addJ(process.env.JAVA_HOME);for(const v of ['21','17']){const j=spawnSync('/usr/libexec/java_home',['-v',v],{encoding:'utf8',timeout:10000});if(j.status===0)addJ(String(j.stdout||'').trim())}addJ('/Applications/Android Studio.app/Contents/jbr/Contents/Home');
  let jdk=null;for(const h of javaHomes){if(spawnSync(path.join(h,'bin','java'),['-version'],{encoding:'utf8',timeout:10000}).status===0){jdk=h;break;}}if(!jdk)stop('environment','GRADLE_CAPABLE_JAVA_UNAVAILABLE');
  const gradleHome=path.join(WORK_ROOT,'gradle-home');fs.mkdirSync(gradleHome,{recursive:true});fs.writeFileSync(path.join(gradleHome,'gradle.properties'),'org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m\nkotlin.daemon.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m\n');const gradleEnv={...process.env,JAVA_HOME:jdk,GRADLE_USER_HOME:gradleHome};const gradlew=path.join(repoRoot,'gradlew');if(!fs.existsSync(gradlew))stop('harness','GRADLE_WRAPPER_MISSING');
  run(bash,[gradlew,'--no-daemon','--max-workers=1','-Dorg.gradle.jvmargs=-Xmx4g','-Dkotlin.daemon.jvm.options=-Xmx4g','-PvexlifeAppVersion=0.0.0','archiveVexLifeProdMacApplication'],{cwd:repoRoot,env:gradleEnv,label:'MAC_APP_BUILD',timeout:2700000});
  const appRoot=path.join(repoRoot,'build','vexlife-prod-mac-application',HEAD),app=path.join(appRoot,'VexLife.app'),receiptFile=path.join(appRoot,'VexLife-macOS-AppleSilicon.receipt.json'),setupLauncher=path.join(app,'Contents','Resources','private-service-image','bin','vexlife-prod-setup');for(const f of [app,receiptFile,setupLauncher])if(!fs.existsSync(f))stop('candidate','MAC_APP_ARTIFACT_MISSING');if(JSON.parse(fs.readFileSync(receiptFile,'utf8')).sourceSha!==HEAD)stop('candidate','MAC_APP_SOURCE_RECEIPT_MISMATCH');completedStepReceipts.push({step:'candidate-mac-app-build',state:'PASS'});

  const parseJsonOutput=(r,label)=>{try{return JSON.parse(String(r.stdout||'').trim())}catch{afterState[label+'Tail']=(String(r.stdout||'')+'\n'+String(r.stderr||'')).slice(-4000);stop('harness',label+'_JSON_INVALID')}};
  const modelProjection=parseJsonOutput(run(bash,[setupLauncher,'--setup-model','--home',HOME],{cwd:repoRoot,label:'MODEL_SELECTION_OBSERVE',timeout:120000}),'MODEL_SELECTION_OBSERVE');afterState.modelSelection={selected:modelProjection?.selected??null,savedInHome:modelProjection?.savedInHome??null};if(modelProjection?.selected!==FOUR_B_HANDLE||modelProjection?.savedInHome!==true)stop('environment','FOUR_B_NOT_ALREADY_SELECTED_IN_HOME');
  const inspect=()=>parseJsonOutput(run(bash,[setupLauncher,'--inspect-front-door','--home',HOME,'--expected-source-sha',HEAD],{cwd:repoRoot,label:'FRONT_DOOR_INSPECT',timeout:120000}),'FRONT_DOOR_INSPECT');
  const activate=c=>parseJsonOutput(run(bash,[setupLauncher,'--activate-front-door-control',c,'--home',HOME,'--expected-source-sha',HEAD],{cwd:repoRoot,label:'FRONT_DOOR_'+c,timeout:900000}),'FRONT_DOOR_'+c);
  let projection=inspect();for(let i=0;i<4&&projection?.semanticStateRef!=='CURRENT';i++){const s=projection?.semanticStateRef;let control=null;if(s==='START_REQUIRED')control='START';else if(['UPDATE_AVAILABLE','REPAIR_REQUIRED','UPDATE_REPAIR_REQUIRED'].includes(s))control='CONVERGE_DESIRED';else if(s==='SERVICE_RECOVERY_REQUIRED')control='RECOVER_STALE_SERVICE';else stop('environment','FRONT_DOOR_STATE_'+String(s||'UNKNOWN'));effects.modelRuntimePerformed=true;const a=activate(control);if(a?.failure)stop('candidate','FRONT_DOOR_CONTROL_FAILED');projection=inspect();}
  if(projection?.semanticStateRef!=='CURRENT'||projection?.currentServiceSourceSha!==HEAD)stop('candidate','CANDIDATE_DID_NOT_CONVERGE_CURRENT');
  const source=await waitJson(SERVICE_ORIGIN+SOURCE_PATH,v=>v?.sourceSha===HEAD&&Number.isInteger(v?.processPid)&&typeof v?.startupQualifiedRuntimeReceiptRef==='string',60000,'SOURCE_CURRENTNESS');const runtime=readRuntimeReceipt();if(runtime.modelRef!==FOUR_B_MODEL_REF)stop('environment','RUNTIME_NOT_EXACT_HERETIC_4B');if(runtime.receiptRef!==source.startupQualifiedRuntimeReceiptRef||!processAlive(runtime.pid))stop('candidate','RUNTIME_NOT_EXACT_LIVE');afterState.current={sourceSha:source.sourceSha,servicePid:source.processPid,runtime};
  const protectedAfter={conversations:aggregateTree(path.join(HOME,'conversations')),memory:aggregateTree(path.join(HOME,'memory'))};afterState.protectedAfterConvergence=protectedAfter;if(protectedAfter.conversations!==protectedBefore.conversations)stop('candidate','CONVERGENCE_MUTATED_CONVERSATION');if(protectedAfter.memory!==protectedBefore.memory)stop('candidate','CONVERGENCE_MUTATED_MEMORY');

  const prep=await waitPreparationReady(source.processPid,0,90000);afterState.preparationReady={sequence:prep.sequence,state:prep.refs?.state??null,runtimeReceiptRef:prep.refs?.runtimeReceiptRef??null,prefixSha:prep.refs?.prefixSha??null,providerTotalMs:prep.refs?.providerTotalMs?Number(prep.refs.providerTotalMs):null};if(prep.refs?.state!=='READY_REUSABLE')stop('candidate','PROMPT_PREPARATION_'+String(prep.refs?.state||'UNKNOWN'));if(prep.refs?.runtimeReceiptRef!==runtime.receiptRef)stop('candidate','PREPARATION_RUNTIME_BINDING_MISMATCH');
  const turnReady=await waitJson(SERVICE_ORIGIN+TURN_PATH,v=>v?.state==='READY'&&v?.initialPromptPreparationPendingForNextTurn===true&&v?.initialPromptPreparationForActiveTurn===false,30000,'FIRST_LOCAL_START_READY');afterState.firstLocalBeforeSend={state:turnReady.state,pending:turnReady.initialPromptPreparationPendingForNextTurn,active:turnReady.initialPromptPreparationForActiveTurn};
  const [html,css,js]=await Promise.all([getText(SERVICE_ORIGIN+'/',10000),getText(SERVICE_ORIGIN+'/vexweb.css',10000),getText(SERVICE_ORIGIN+'/vexweb.js',10000)]);const presentation={firstStart:html.includes('response-progress-title')&&css.includes('.response-progress[data-state="FirstLocalStart"]')&&js.includes('initialPromptPreparationPendingForNextTurn'),sentResources:js.includes('const sentResourceNode = (message, resource) =>')&&js.includes('conversationResourceMetadataPath')&&js.includes('conversationResourceContentPath')};afterState.presentationChecks=presentation;if(!presentation.firstStart||!presentation.sentResources)stop('candidate','LIVE_PRESENTATION_BYTES_MISSING_COMPOSED_CIRCUIT');

  const conversationAtGate=await getJson(SERVICE_ORIGIN+CONVERSATION_PATH,10000);const gateMessages=Array.isArray(conversationAtGate?.messages)?conversationAtGate.messages:[];const gateMaxSeq=gateMessages.reduce((m,x)=>Math.max(m,Number(x.sequence)||0),0);beforeState.gateConversationHead=conversationAtGate?.currentConversationHeadSha256??null;beforeState.gateMaxSequence=gateMaxSeq;
  const draftAtGate=await getJson(SERVICE_ORIGIN+ATTACHMENT_DRAFT_PATH,10000);afterState.draftAtGate={state:draftAtGate?.state??null,itemCount:Array.isArray(draftAtGate?.items)?draftAtGate.items.length:null};
  run(open,[SERVICE_ORIGIN+'/'],{label:'OPEN_LIVED_CANDIDATE',timeout:30000});
  process.stdout.write('\n[VexLife lived gate]\n1) Attach exactly ONE PNG or JPEG.\n2) Ask Vex a question whose answer depends on what is visibly in the image, then Send.\n3) After Vex answers, reload the VexLife page and confirm the sent image card is still beside your exact message.\n4) Then Send ONE ordinary text message with NO attachment.\nThe harness will observe both turns; it will not Send for you.\n\n');

  const send1=await waitForRecord(source.processPid,r=>r.fact===SEND_FACT,prep.sequence,15*60*1000,'FIRST_HUMAN_SEND');const turn1=send1.refs?.turnRef;if(typeof turn1!=='string'||!turn1)stop('harness','FIRST_SEND_TURN_REF_MISSING');
  const terminal1=await waitForRecord(source.processPid,r=>(r.fact===TURN_COMPLETED_FACT||r.fact===TURN_FAILED_FACT)&&r.refs?.turnRef===turn1,send1.sequence,20*60*1000,'FIRST_TURN_TERMINAL');if(terminal1.fact!==TURN_COMPLETED_FACT)candidateFindings.push('FIRST_IMAGE_SEND_DID_NOT_COMPLETE');
  const post1=chronologyRecords(source.processPid).filter(r=>r.sequence>send1.sequence&&r.sequence<=terminal1.sequence);const prepAfter1=post1.find(r=>r.fact===PREPARATION_FACT&&['CONSUMED','INVALIDATED','HELD','FAILED'].includes(r.refs?.state));const stage1=post1.find(r=>r.fact===STAGE_FACT);if(!stage1)candidateFindings.push('FIRST_PROVIDER_STAGE_NOT_OBSERVED');const metrics1=stage1?metricsFrom(stage1):null;afterState.firstProviderStage=metrics1;afterState.preparationAfterFirstSend=prepAfter1?{state:prepAfter1.refs?.state??null,reason:prepAfter1.refs?.reason??null}:null;if(prepAfter1?.refs?.state!=='CONSUMED')candidateFindings.push('PREPARATION_NOT_CONSUMED_BY_FIRST_SEND');if(metrics1){if(metrics1.runtimeReceiptRef!==runtime.receiptRef)candidateFindings.push('FIRST_PROVIDER_RUNTIME_BINDING_MISMATCH');if(metrics1.cachedPromptTokens===null||metrics1.cachedPromptTokens<=0)candidateFindings.push('FIRST_SEND_CACHE_REUSE_NOT_OBSERVED');if(metrics1.promptTokens===null||metrics1.prefillTokens===null||metrics1.prefillTokens>=metrics1.promptTokens)candidateFindings.push('FIRST_SEND_PREFILL_NOT_REDUCED');}

  const conv1=await waitJson(SERVICE_ORIGIN+CONVERSATION_PATH,v=>Array.isArray(v?.messages)&&v.messages.some(m=>m.role==='human'&&m.turnRef===turn1)&&v.messages.some(m=>m.role==='vex'&&m.turnRef===turn1),30000,'FIRST_CONVERSATION_PROJECTION');const human1=conv1.messages.find(m=>m.role==='human'&&m.turnRef===turn1);const vex1=conv1.messages.find(m=>m.role==='vex'&&m.turnRef===turn1);if(!human1||!vex1)stop('candidate','FIRST_TURN_NOT_PROJECTED');afterState.firstHumanMessage=compactMessage(human1);afterState.firstVexMessage={...compactMessage(vex1),contentCharacters:String(vex1.content??'').length};if(!Array.isArray(human1.resources)||human1.resources.length!==1)candidateFindings.push('FIRST_SEND_DID_NOT_PROJECT_EXACTLY_ONE_RESOURCE');
  let resourceEvidence=null;if(Array.isArray(human1.resources)&&human1.resources.length===1){const resource=human1.resources[0];const headers=resourceHeaders(human1,resource);const meta=await getJson(SERVICE_ORIGIN+RESOURCE_METADATA_PATH,10000,headers);const content=await requestBuffer(SERVICE_ORIGIN+RESOURCE_CONTENT_PATH,{headers,timeoutMs:10000});const contentType=String(content.headers['content-type']||'').split(';')[0].trim().toLowerCase();resourceEvidence={resourceRef:resource.resourceRef,displayNameSha256:sha256Bytes(Buffer.from(String(resource.displayName??''),'utf8')),representation:meta?.representation??null,contentAvailable:meta?.contentAvailable??null,originalCustody:meta?.originalCustody??null,projectionCustody:meta?.projectionCustody??null,mediaType:meta?.mediaType??null,widthPixels:meta?.widthPixels??null,heightPixels:meta?.heightPixels??null,contentType,contentBytes:content.body.length,contentSha256:sha256Bytes(content.body)};if(meta?.representation!=='STATIC_IMAGE')candidateFindings.push('FIRST_RESOURCE_NOT_STATIC_IMAGE');if(meta?.contentAvailable!==true||!['image/png','image/jpeg'].includes(contentType)||content.body.length<=0)candidateFindings.push('FIRST_IMAGE_NOT_REVIEWABLE_EXACT_CONTENT');}
  afterState.firstResource=resourceEvidence;

  run(open,[SERVICE_ORIGIN+'/'],{label:'REOPEN_AFTER_FIRST_SEND',timeout:30000});
  const turnAfter1=await waitJson(SERVICE_ORIGIN+TURN_PATH,v=>v?.state==='READY'&&v?.initialPromptPreparationPendingForNextTurn===false&&v?.initialPromptPreparationForActiveTurn===false,30000,'FIRST_LOCAL_START_RETIRED');afterState.firstLocalAfterFirstSend={state:turnAfter1.state,pending:turnAfter1.initialPromptPreparationPendingForNextTurn,active:turnAfter1.initialPromptPreparationForActiveTurn};
  const convReload=await getJson(SERVICE_ORIGIN+CONVERSATION_PATH,10000);const humanReload=(convReload.messages||[]).find(m=>m.role==='human'&&m.turnRef===turn1);if(!humanReload||!Array.isArray(humanReload.resources)||humanReload.resources.length!==1||humanReload.resources[0].resourceRef!==human1.resources?.[0]?.resourceRef)candidateFindings.push('FIRST_RESOURCE_NOT_PRESERVED_AFTER_PRESENTATION_REENTRY');afterState.firstResourceAfterReentry=humanReload?compactMessage(humanReload):null;
  const draftAfter1=await getJson(SERVICE_ORIGIN+ATTACHMENT_DRAFT_PATH,10000);afterState.draftAfterFirstSend={state:draftAfter1?.state??null,itemCount:Array.isArray(draftAfter1?.items)?draftAfter1.items.length:null};if(Array.isArray(draftAfter1?.items)&&draftAfter1.items.length!==0)candidateFindings.push('ATTACHMENT_DRAFT_NOT_EMPTY_AFTER_SUCCESSFUL_FIRST_SEND');

  const send2=await waitForRecord(source.processPid,r=>r.fact===SEND_FACT,terminal1.sequence,15*60*1000,'SECOND_HUMAN_SEND');const turn2=send2.refs?.turnRef;if(typeof turn2!=='string'||!turn2||turn2===turn1)stop('harness','SECOND_SEND_TURN_REF_INVALID');const terminal2=await waitForRecord(source.processPid,r=>(r.fact===TURN_COMPLETED_FACT||r.fact===TURN_FAILED_FACT)&&r.refs?.turnRef===turn2,send2.sequence,20*60*1000,'SECOND_TURN_TERMINAL');if(terminal2.fact!==TURN_COMPLETED_FACT)candidateFindings.push('SECOND_TEXT_SEND_DID_NOT_COMPLETE');
  const post2=chronologyRecords(source.processPid).filter(r=>r.sequence>send2.sequence&&r.sequence<=terminal2.sequence);const stage2=post2.find(r=>r.fact===STAGE_FACT);afterState.secondProviderStage=stage2?metricsFrom(stage2):null;
  const conv2=await waitJson(SERVICE_ORIGIN+CONVERSATION_PATH,v=>Array.isArray(v?.messages)&&v.messages.some(m=>m.role==='human'&&m.turnRef===turn2)&&v.messages.some(m=>m.role==='vex'&&m.turnRef===turn2),30000,'SECOND_CONVERSATION_PROJECTION');const human2=conv2.messages.find(m=>m.role==='human'&&m.turnRef===turn2);const vex2=conv2.messages.find(m=>m.role==='vex'&&m.turnRef===turn2);afterState.secondHumanMessage=human2?compactMessage(human2):null;afterState.secondVexMessage=vex2?{...compactMessage(vex2),contentCharacters:String(vex2.content??'').length}:null;if(!human2||!Array.isArray(human2.resources)||human2.resources.length!==0)candidateFindings.push('SECOND_SEND_CARRIED_STALE_RESOURCE');
  const firstStill=(conv2.messages||[]).find(m=>m.role==='human'&&m.turnRef===turn1);if(!firstStill||!Array.isArray(firstStill.resources)||firstStill.resources.length!==1||firstStill.resources[0].resourceRef!==human1.resources?.[0]?.resourceRef)candidateFindings.push('FIRST_RESOURCE_LOST_AFTER_SECOND_SEND');
  const turnAfter2=await waitJson(SERVICE_ORIGIN+TURN_PATH,v=>v?.state==='READY'&&v?.initialPromptPreparationPendingForNextTurn===false&&v?.initialPromptPreparationForActiveTurn===false,30000,'SECOND_TURN_READY');afterState.turnAfterSecondSend={state:turnAfter2.state,pending:turnAfter2.initialPromptPreparationPendingForNextTurn,active:turnAfter2.initialPromptPreparationForActiveTurn};
  const draftAfter2=await getJson(SERVICE_ORIGIN+ATTACHMENT_DRAFT_PATH,10000);afterState.draftAfterSecondSend={state:draftAfter2?.state??null,itemCount:Array.isArray(draftAfter2?.items)?draftAfter2.items.length:null};if(Array.isArray(draftAfter2?.items)&&draftAfter2.items.length!==0)candidateFindings.push('ATTACHMENT_DRAFT_NOT_EMPTY_AFTER_SECOND_SEND');

  const runtimeAfter=readRuntimeReceipt();afterState.runtimeAfter=runtimeAfter;if(runtimeAfter.receiptRef!==runtime.receiptRef||runtimeAfter.pid!==runtime.pid||!processAlive(runtimeAfter.pid))candidateFindings.push('RUNTIME_CHANGED_DURING_LIVED_CIRCUIT');const sourceAfter=await getJson(SERVICE_ORIGIN+SOURCE_PATH,10000);afterState.sourceAfter={sourceSha:sourceAfter.sourceSha??null,processPid:sourceAfter.processPid??null,runtimeReceiptRef:sourceAfter.startupQualifiedRuntimeReceiptRef??null,conversationHead:sourceAfter.currentConversationHeadSha256??null};if(sourceAfter.sourceSha!==HEAD||sourceAfter.processPid!==source.processPid||sourceAfter.startupQualifiedRuntimeReceiptRef!==runtime.receiptRef)candidateFindings.push('SERVICE_CURRENTNESS_CHANGED_DURING_LIVED_CIRCUIT');
  const memoryAfter=aggregateTree(path.join(HOME,'memory'));afterState.memoryAggregateAfter=memoryAfter;if(memoryAfter!==protectedAfter.memory)candidateFindings.push('LIVED_CIRCUIT_MUTATED_MEMORY');

  afterState.humanJudgment={groundedImageAnswer:'PENDING_VICTOR',sentResourceVisualAcceptance:'PENDING_VICTOR'};
  completedStepReceipts.push({step:'whole-lived-machine-circuit',state:candidateFindings.length===0?'PASS':'SAFE_FAILURE'});
  writeResult(candidateFindings.length===0?'PASS':'SAFE_FAILURE',candidateFindings.length===0?null:'WHOLE_LIVED_MACHINE_ACCEPTANCE_NOT_MET');
}catch(error){if(error instanceof LivedStop){addFinding(error.category,error.code);writeResult('SAFE_FAILURE',error.code);}else{harnessFindings.push('LIVED_PAYLOAD_UNCAUGHT_'+String(error?.name||'ERROR'));afterState.uncaughtClass=String(error?.name||'Error');afterState.uncaughtMessage=String(error?.message||'');writeResult('SAFE_FAILURE','LIVED_PAYLOAD_UNCAUGHT');}}
