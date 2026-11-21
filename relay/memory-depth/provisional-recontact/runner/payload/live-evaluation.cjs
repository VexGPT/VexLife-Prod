'use strict';

const fs = require('node:fs');
const path = require('node:path');
const os = require('node:os');
const crypto = require('node:crypto');
const cp = require('node:child_process');
const http = require('node:http');

const RESULT_VERSION = 'vextreme.vexlocalbridge-mac-payload-result/v1';
const EFFECT_FIELDS = [
  'sourceMutationPerformed','branchMutationPerformed','claimMutationPerformed','prMetadataMutationPerformed',
  'reviewPerformed','approvalPerformed','readyTransitionPerformed','mergePerformed','mainMutationPerformed',
  'checkpointMutationPerformed','forcePushPerformed','historyRewritePerformed','hostInstallationPerformed',
  'modelRuntimePerformed','providerOrNetworkEffectPerformed','publicationPerformed',
  'credentialSerializationPerformed','filesystemOutsideQualifiedRootsPerformed',
];

const PACKAGE_ROOT = path.resolve(__dirname, '..');
const ATTEMPT = JSON.parse(fs.readFileSync(path.join(PACKAGE_ROOT, 'inputs', 'CURRENT-ATTEMPT.json'), 'utf8'));
const BINDING = JSON.parse(fs.readFileSync(path.join(PACKAGE_ROOT, 'inputs', 'experiment-binding.json'), 'utf8'));
const RESULT_DIR = process.env.VEXLOCALBRIDGE_RESULT_DIR;
const CAPSULE = process.env.VEXLOCALBRIDGE_CAPSULE_ROOT;
const TMP = process.env.TMPDIR;
if (!RESULT_DIR || !CAPSULE || !TMP) throw new Error('VexLocalBridge execution environment is incomplete');

const ATTEMPT_SLUG = ATTEMPT.attemptRef.toLowerCase().replace(/[^a-z0-9._-]+/g, '-');
const STABLE = path.join(
  CAPSULE,
  'bridge',
  'retained-vexrelay',
  'memory-depth-provisional-recontact-companion',
  ATTEMPT_SLUG,
);
const PRODUCT = path.join(TMP, 'VexLife-Prod');
const GRADLE_HOME = path.join(TMP, 'gradle-home');
const ROUND = path.join(TMP, 'round');
fs.rmSync(STABLE, { recursive:true, force:true });
fs.mkdirSync(STABLE, { recursive:true });
fs.mkdirSync(RESULT_DIR, { recursive:true });
fs.mkdirSync(GRADLE_HOME, { recursive:true });
fs.mkdirSync(ROUND, { recursive:true });

const evidenceFiles = [];
const steps = [];
let modelRuntimePerformed = false;

function sha256File(file) {
  return crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex');
}
function gitBlobSha(file) {
  const bytes = fs.readFileSync(file);
  const header = Buffer.from('blob ' + bytes.length + '\0');
  return crypto.createHash('sha1').update(header).update(bytes).digest('hex');
}
function stableFile(name, bytesOrText) {
  const file = path.join(STABLE, name);
  fs.mkdirSync(path.dirname(file), { recursive:true });
  fs.writeFileSync(file, bytesOrText);
  evidenceFiles.push(file);
  return file;
}
function stableJson(name, value) {
  return stableFile(name, JSON.stringify(value, null, 2) + '\n');
}
function effects() {
  const x = Object.fromEntries(EFFECT_FIELDS.map(k => [k,false]));
  x.modelRuntimePerformed = modelRuntimePerformed;
  x.providerOrNetworkEffectPerformed = true;
  return x;
}
function run(argv, { cwd=PRODUCT, timeoutMs=600000, env={} }={}) {
  const started = Date.now();
  const r = cp.spawnSync(argv[0], argv.slice(1), {
    cwd, encoding:'utf8', shell:false, timeout:timeoutMs, maxBuffer:64*1024*1024,
    env:{...process.env,...env},
  });
  const timedOut = Boolean(r.error && (r.error.code === 'ETIMEDOUT' || /timed out/i.test(r.error.message || '')));
  return {
    argv, cwd, exitCode:r.status ?? -1, signal:r.signal || null, timedOut,
    elapsedMs:Date.now()-started, stdout:r.stdout || '', stderr:r.stderr || '',
    error:r.error ? String(r.error.stack || r.error.message || r.error) : null,
    accepted:r.status === 0 && !timedOut,
  };
}
function commandStep(name, result) {
  const receipt={step:name,argv:result.argv,cwd:result.cwd,exitCode:result.exitCode,accepted:result.accepted,timedOut:result.timedOut,elapsedMs:result.elapsedMs};
  steps.push(receipt);
  stableJson('commands/'+name+'.json', result);
  return receipt;
}
function requireRun(name, argv, options) {
  const r=run(argv,options);
  commandStep(name,r);
  if(!r.accepted) {
    const error=new Error(name+' failed');
    error.code=name.toUpperCase().replace(/[^A-Z0-9]+/g,'_')+'_FAILED';
    error.run=r;
    throw error;
  }
  return r;
}
function git(...args) {
  return requireRun('git-'+steps.length, ['/usr/bin/git','-C',PRODUCT,...args], {cwd:TMP,timeoutMs:120000});
}
function getJson(url, timeoutMs=3000) {
  return new Promise(resolve => {
    const req=http.get(url,{timeout:timeoutMs},res=>{
      let body=''; res.setEncoding('utf8');
      res.on('data',d=>body+=d);
      res.on('end',()=>{
        let json=null,parseError=null;
        try{json=JSON.parse(body);}catch(e){parseError=e.message;}
        resolve({state:'RESPONDED',url,statusCode:res.statusCode,body,json,parseError});
      });
    });
    req.on('timeout',()=>{req.destroy();resolve({state:'TIMEOUT',url});});
    req.on('error',e=>resolve({state:'UNAVAILABLE',url,error:e.message}));
  });
}
function parseJsonDocument(text,label) {
  const trimmed=text.trim();
  try{return JSON.parse(trimmed);}catch{}
  const lines=trimmed.split(/\r?\n/).map(x=>x.trim()).filter(Boolean);
  for(let i=lines.length-1;i>=0;i--) {
    try{return JSON.parse(lines[i]);}catch{}
  }
  throw Object.assign(new Error(label+' did not emit JSON'),{code:'JSON_OUTPUT_INVALID'});
}
function retainedFiles() {
  return evidenceFiles.filter(fs.existsSync).map(sourcePath=>({
    sourcePath,
    targetPath:'retained/inner-vexrelay/memory-depth-provisional-recontact-companion/'+ATTEMPT_SLUG+'/'+path.relative(STABLE,sourcePath).split(path.sep).join('/'),
    expectedSha256:sha256File(sourcePath),
  }));
}
function copyIfExists(src,name) {
  if(!fs.existsSync(src)) return null;
  return stableFile(name,fs.readFileSync(src));
}
function cleanAttemptLocal() {
  fs.rmSync(PRODUCT,{recursive:true,force:true});
  fs.rmSync(GRADLE_HOME,{recursive:true,force:true});
  fs.rmSync(ROUND,{recursive:true,force:true});
}

(async()=>{
  stableJson('current-attempt.json',ATTEMPT);
  stableJson('experiment-binding.json',BINDING);

  const gh=['/opt/homebrew/bin/gh','/usr/local/bin/gh'].find(fs.existsSync);
  if(!gh) throw Object.assign(new Error('GitHub CLI unavailable'),{code:'GITHUB_AUTH_UNAVAILABLE'});
  requireRun('github-auth',[gh,'auth','status','--hostname','github.com'],{cwd:TMP,timeoutMs:120000});

  const clone=requireRun('clone-experiment',[gh,'repo','clone','VexGPT/VexLife-Prod',PRODUCT,'--','--no-checkout'],{cwd:TMP,timeoutMs:300000});
  requireRun('checkout-experiment',['/usr/bin/git','-C',PRODUCT,'checkout','--detach',ATTEMPT.experimentHead],{cwd:TMP,timeoutMs:120000});

  const head=requireRun('observe-head',['/usr/bin/git','-C',PRODUCT,'rev-parse','HEAD'],{cwd:TMP,timeoutMs:120000}).stdout.trim();
  const tree=requireRun('observe-tree',['/usr/bin/git','-C',PRODUCT,'rev-parse','HEAD^{tree}'],{cwd:TMP,timeoutMs:120000}).stdout.trim();
  if(head!==ATTEMPT.experimentHead) throw Object.assign(new Error('experiment head mismatch'),{code:'EXPERIMENT_HEAD_MISMATCH'});
  if(tree!==ATTEMPT.experimentTree) throw Object.assign(new Error('experiment tree mismatch'),{code:'EXPERIMENT_TREE_MISMATCH'});

  const changed=requireRun('observe-changed-paths',['/usr/bin/git','-C',PRODUCT,'diff','--name-only',ATTEMPT.experimentBaseHead+'..'+ATTEMPT.experimentHead],{cwd:TMP,timeoutMs:120000})
    .stdout.trim().split(/\r?\n/).filter(Boolean).sort();
  const expected=[...BINDING.experiment.changedPaths].sort();
  if(JSON.stringify(changed)!==JSON.stringify(expected)) {
    stableJson('changed-paths.json',{expected,observed:changed});
    throw Object.assign(new Error('changed-path membrane mismatch'),{code:'CHANGED_PATH_MEMBRANE_MISMATCH'});
  }

  const blobEvidence={};
  for(const [rel,expectedBlob] of Object.entries(BINDING.experiment.expectedBlobs)) {
    const file=path.join(PRODUCT,rel);
    if(!fs.existsSync(file)) throw Object.assign(new Error('expected source missing: '+rel),{code:'EXPECTED_SOURCE_MISSING'});
    const observed=gitBlobSha(file);
    blobEvidence[rel]={expected:expectedBlob,observed,pass:observed===expectedBlob};
    if(observed!==expectedBlob) throw Object.assign(new Error('source blob mismatch: '+rel),{code:'SOURCE_BLOB_MISMATCH'});
  }
  stableJson('source-binding-evidence.json',{head,tree,changedPaths:changed,blobs:blobEvidence});

  const service=await getJson('http://127.0.0.1:18210/api/v1/source-currentness');
  stableJson('preflight-service-currentness.json',service);
  if(service.statusCode!==200 || !service.json) throw Object.assign(new Error('Product service currentness unavailable'),{code:'PRODUCT_SERVICE_CURRENTNESS_UNAVAILABLE'});
  if(service.json.sourceSha!==ATTEMPT.expectedServiceSourceSha) {
    throw Object.assign(new Error('Product service moved from the experiment base; currentize before behavior'),{code:'LIVE_PRODUCT_SOURCE_MOVED'});
  }

  const gradleCommon=[
    './gradlew','--no-daemon','--max-workers=1',
    '-Pkotlin.compiler.execution.strategy=in-process',
    '-Dorg.gradle.jvmargs=-Xmx4g','--console=plain'
  ];
  const gradleEnv={GRADLE_USER_HOME:GRADLE_HOME};

  const proof=requireRun('source-proof',[
    ...gradleCommon,
    'jvmTest',
    '--tests','vexlife.foundational.VexLifeFoundationalFieldTest',
    '--tests','vexlife.harness.ContinuityPressureAdapterTest'
  ],{cwd:PRODUCT,timeoutMs:ATTEMPT.sourceProofTimeoutMs,env:gradleEnv});
  stableFile('source-proof.stdout.log',proof.stdout);
  stableFile('source-proof.stderr.log',proof.stderr);

  const offline=[...gradleCommon,'--offline'];
  const start=requireRun('starting-current-state',[
    ...offline,'-q','observeVexLifeProdCurrentState','--args=--home '+ATTEMPT.home
  ],{cwd:PRODUCT,timeoutMs:180000,env:gradleEnv});
  const startState=parseJsonDocument(start.stdout,'starting-current-state');
  stableJson('starting-current-state.json',startState);

  const args=[
    '--stage','full',
    '--scenario-ref',ATTEMPT.scenarioRef,
    '--scenario-file',path.join(PRODUCT,ATTEMPT.scenarioPath),
    '--result-dir',ROUND,
    '--pressure-strategy','EFFECTIVE_SELECTION_BUDGET',
    '--effective-input-token-budget',String(ATTEMPT.effectiveInputTokenBudget),
    '--sets',String(ATTEMPT.sets),
    '--turns-per-set',String(ATTEMPT.turnsPerSet),
    '--source-sha',ATTEMPT.experimentHead,
    '--source-clean','true',
    '--runtime-home',ATTEMPT.home,
    '--experiment-window',
    '--payload-scale',String(ATTEMPT.payloadScale),
    '--response-commit-mode',ATTEMPT.responseCommitMode
  ];
  modelRuntimePerformed=true;
  const behavior=run([
    ...offline,'-q','continuityPressure','--args='+args.join(' ')
  ],{cwd:PRODUCT,timeoutMs:ATTEMPT.semanticWindowMs,env:gradleEnv});
  commandStep('behavior-process',behavior);
  stableFile('behavior.stdout.log',behavior.stdout);
  stableFile('behavior.stderr.log',behavior.stderr);

  const heldPath=path.join(ROUND,'held.json');
  const resultPath=path.join(ROUND,'result.json');
  const progressPath=path.join(ROUND,'progress-checkpoint.json');
  copyIfExists(heldPath,'held.json');
  copyIfExists(progressPath,'progress-checkpoint.json');

  if(fs.existsSync(heldPath) && !fs.existsSync(resultPath)) {
    modelRuntimePerformed=false;
    const held=JSON.parse(fs.readFileSync(heldPath,'utf8'));
    const e=new Error(held.reason || 'live experiment held');
    e.code='LIVE_EXPERIMENT_HELD';
    e.held=held;
    throw e;
  }
  if(!fs.existsSync(resultPath)) {
    const e=new Error('live experiment produced no result.json');
    e.code='LIVE_RESULT_MISSING';
    throw e;
  }

  const result=JSON.parse(fs.readFileSync(resultPath,'utf8'));
  stableJson('result.json',result);
  copyIfExists(path.join(ROUND,'transition-timing.json'),'transition-timing.json');
  copyIfExists(path.join(ROUND,'host-scan-timing.json'),'host-scan-timing.json');

  const zips=fs.readdirSync(ROUND).filter(x=>x.endsWith('.zip'));
  if(zips.length!==1) throw Object.assign(new Error('expected exactly one continuity-pressure result ZIP'),{code:'ROUND_ZIP_IDENTITY_AMBIGUOUS'});
  const roundZip=path.join(ROUND,zips[0]);
  stableFile('continuity-pressure-result.zip',fs.readFileSync(roundZip));

  const provisional=run(['/usr/bin/unzip','-p',roundZip,'implementation/provisional-recontacts.jsonl'],{cwd:ROUND,timeoutMs:120000});
  if(!provisional.accepted) throw Object.assign(new Error('provisional re-contact evidence missing from round ZIP'),{code:'PROVISIONAL_RECONTACT_EVIDENCE_MISSING'});
  stableFile('provisional-recontacts.jsonl',provisional.stdout);
  const provisionalRows=provisional.stdout.split(/\r?\n/).filter(Boolean);
  if(provisionalRows.length!==ATTEMPT.expectedTurns) {
    throw Object.assign(new Error('provisional re-contact count does not equal expected turn count'),{code:'PROVISIONAL_RECONTACT_COUNT_MISMATCH'});
  }

  const turns=Array.isArray(result.turns)?result.turns:[];
  const completedTurnCount=turns.length;
  if(result.responseCommitMode!==ATTEMPT.responseCommitMode) throw Object.assign(new Error('result responseCommitMode mismatch'),{code:'RESPONSE_COMMIT_MODE_MISMATCH'});
  if(completedTurnCount!==ATTEMPT.expectedTurns) throw Object.assign(new Error('behavior did not complete all expected turns'),{code:'BEHAVIOR_TURN_COUNT_INCOMPLETE'});
  if(behavior.timedOut) throw Object.assign(new Error('behavior semantic window exhausted'),{code:'BEHAVIOR_TIMEOUT'});
  if(behavior.exitCode!==0) throw Object.assign(new Error('behavior process returned non-zero'),{code:'BEHAVIOR_PROCESS_FAILED'});

  const end=requireRun('ending-current-state',[
    ...offline,'-q','observeVexLifeProdCurrentState','--args=--home '+ATTEMPT.home
  ],{cwd:PRODUCT,timeoutMs:180000,env:gradleEnv});
  const endState=parseJsonDocument(end.stdout,'ending-current-state');
  stableJson('ending-current-state.json',endState);

  const sameModel =
    startState.activeModelRef===endState.activeModelRef &&
    startState.activeModelProfileRef===endState.activeModelProfileRef &&
    startState.runtimeReceiptPath===endState.runtimeReceiptPath;
  if(!sameModel) throw Object.assign(new Error('starting/ending model relationship changed'),{code:'MODEL_RELATIONSHIP_CHANGED'});

  const outcome={
    schemaVersion:'vexlife-prod.memory-depth.provisional-recontact-live-outcome/v1',
    complete:true,
    experimentPullRequest:ATTEMPT.experimentPullRequest,
    experimentHead:ATTEMPT.experimentHead,
    foundationVersion:ATTEMPT.foundationVersion,
    foundationBodySha256:ATTEMPT.foundationBodySha256,
    scenarioRef:ATTEMPT.scenarioRef,
    scenarioSha256:ATTEMPT.scenarioSha256,
    responseCommitMode:ATTEMPT.responseCommitMode,
    completedTurnCount,
    expectedTurnCount:ATTEMPT.expectedTurns,
    provisionalRecontactCount:provisionalRows.length,
    behaviorStatus:result.status || null,
    behaviorExitCode:behavior.exitCode,
    behaviorElapsedMs:behavior.elapsedMs,
    modelRelationshipPreserved:sameModel,
    modelRef:startState.activeModelRef,
    modelProfileRef:startState.activeModelProfileRef,
    serviceSourceSha:service.json.sourceSha,
  };
  stableJson('outcome.json',outcome);

  cleanAttemptLocal();
  const payloadResult={
    schemaVersion:RESULT_VERSION,
    disposition:'PASS',
    stopReasonOrNull:null,
    firstSubstantiveFailureOrNull:null,
    acceptedExitCodes:[0],
    candidateFindings:[],
    harnessFindings:[],
    environmentFindings:[],
    unknownFindings:[],
    effects:effects(),
    beforeState:{experimentHead:ATTEMPT.experimentHead,serviceSourceSha:service.json.sourceSha,startingCurrentState:startState},
    afterState:outcome,
    completedStepReceipts:steps,
    retainedEvidenceFiles:retainedFiles(),
  };
  fs.writeFileSync(path.join(RESULT_DIR,'payload-result.json'),JSON.stringify(payloadResult,null,2)+'\n');
})().catch(error=>{
  const code=error?.code || 'LIVE_EVALUATION_FAILED';
  stableJson('failure.json',{code,message:error.message,stack:error.stack || null,held:error.held || null});
  try{copyIfExists(path.join(ROUND,'result.json'),'partial-result.json');}catch{}
  try{copyIfExists(path.join(ROUND,'progress-checkpoint.json'),'partial-progress-checkpoint.json');}catch{}
  cleanAttemptLocal();
  const payloadResult={
    schemaVersion:RESULT_VERSION,
    disposition:'SAFE_FAILURE',
    stopReasonOrNull:code,
    firstSubstantiveFailureOrNull:null,
    acceptedExitCodes:[0],
    candidateFindings:[],
    harnessFindings:[code],
    environmentFindings:[],
    unknownFindings:[],
    effects:effects(),
    beforeState:{experimentHead:ATTEMPT.experimentHead},
    afterState:{error:code,message:error.message},
    completedStepReceipts:steps,
    retainedEvidenceFiles:retainedFiles(),
  };
  fs.writeFileSync(path.join(RESULT_DIR,'payload-result.json'),JSON.stringify(payloadResult,null,2)+'\n');
});
