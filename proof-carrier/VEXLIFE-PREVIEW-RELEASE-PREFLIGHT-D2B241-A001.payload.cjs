#!/usr/bin/env node
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const os = require('node:os');
const crypto = require('node:crypto');
const { spawnSync } = require('node:child_process');

const RESULT_SCHEMA='vextreme.vexlocalbridge-mac-payload-result/v1';

function sha256(value){return crypto.createHash('sha256').update(value).digest('hex');}
function effects(){return {
  sourceMutationPerformed:false, branchMutationPerformed:false, claimMutationPerformed:false,
  prMetadataMutationPerformed:false, reviewPerformed:false, approvalPerformed:false,
  readyTransitionPerformed:false, mergePerformed:false, mainMutationPerformed:false,
  checkpointMutationPerformed:false, forcePushPerformed:false, historyRewritePerformed:false,
  hostInstallationPerformed:false, modelRuntimePerformed:false, providerOrNetworkEffectPerformed:true,
  publicationPerformed:false, credentialSerializationPerformed:false,
  filesystemOutsideQualifiedRootsPerformed:false,
};}
function writeResult(value){
  const root=process.env.VEXLOCALBRIDGE_RESULT_DIR;
  if(!root) throw new Error('VEXLOCALBRIDGE_RESULT_DIR unavailable');
  fs.mkdirSync(root,{recursive:true});
  fs.writeFileSync(path.join(root,'payload-result.json'),JSON.stringify({
    schemaVersion:RESULT_SCHEMA,acceptedExitCodes:[0],...value
  },null,2)+'\n');
}
function run(command,args,options={}){
  const r=spawnSync(command,args,{
    cwd:options.cwd, env:options.env||process.env, encoding:'utf8',
    timeout:options.timeoutMs||690000, maxBuffer:128*1024*1024,
    stdio:['ignore','pipe','pipe'],
  });
  return {
    status:Number.isInteger(r.status)?r.status:127,
    signal:r.signal||null,
    stdout:String(r.stdout||''),
    stderr:String(r.stderr||(r.error?r.error.message:'')),
  };
}
function retain(root,name,command,result){
  const full=path.join(root,name);
  const content=[
    'command='+command.join(' '),
    'exitCode='+result.status,
    'signal='+(result.signal||''),
    '',
    '===== STDOUT =====',
    result.stdout,
    '',
    '===== STDERR =====',
    result.stderr,
    '',
  ].join('\n');
  fs.writeFileSync(full,content);
  return {
    sourcePath:full,
    targetPath:'retained/inner-vexrelay/'+name,
    expectedSha256:sha256(Buffer.from(content)),
  };
}
function failure(stage,result,evidenceRef){
  const tail=(result.stdout+'\n'+result.stderr).slice(-12000);
  return {
    failureFamily:'VEXLIFE_PROD_PREVIEW_RELEASE_PREFLIGHT',
    failureFingerprint:sha256(Buffer.from(stage+'\n'+tail)),
    stage,
    summary:stage+' failed exit='+result.status+(result.signal?' signal='+result.signal:'')+
      '; full output retained as '+evidenceRef,
    evidenceRef,
  };
}

async function main(){
  const bindingPath=process.argv[2];
  if(!bindingPath) throw new Error('binding path required');
  const binding=JSON.parse(fs.readFileSync(path.resolve(bindingPath),'utf8'));
  const capsuleRoot=process.env.VEXLOCALBRIDGE_CAPSULE_ROOT;
  if(!capsuleRoot||!path.isAbsolute(capsuleRoot)) throw new Error('VEXLOCALBRIDGE_CAPSULE_ROOT unavailable');
  const retainedRoot=path.join(
    capsuleRoot,'bridge','retained-diagnostics',
    process.env.VEXLOCALBRIDGE_TASK_REF||'task',
    process.env.VEXLOCALBRIDGE_ATTEMPT_REF||'attempt'
  );
  fs.mkdirSync(retainedRoot,{recursive:true});
  const retainedEvidenceFiles=[];
  const completedStepReceipts=[];
  let work=null;

  try{
    if(process.platform!=='darwin'||process.arch!=='arm64'){
      throw new Error('Preview release preflight requires Apple-Silicon macOS');
    }
    const gh=['/opt/homebrew/bin/gh','/usr/local/bin/gh'].find(fs.existsSync);
    if(!gh) throw new Error('GitHub CLI unavailable');
    if(run(gh,['auth','status','--hostname','github.com'],{timeoutMs:30000}).status!==0){
      throw new Error('GitHub CLI authentication unavailable');
    }
    const javaResult=run('/usr/libexec/java_home',['-v','21'],{timeoutMs:10000});
    if(javaResult.status!==0) throw new Error('Java 21 home unavailable');
    const javaHome=javaResult.stdout.trim();
    const env={...process.env,JAVA_HOME:javaHome,PATH:javaHome+'/bin:'+process.env.PATH};

    work=fs.mkdtempSync(path.join(process.env.TMPDIR||os.tmpdir(),'vexlife-preview-release-preflight-'));
    const checkout=path.join(work,'VexLife-Prod');
    let r=run(gh,['repo','clone',binding.repository,checkout,'--','--no-checkout'],{env,timeoutMs:180000});
    if(r.status!==0) throw new Error('clone failed: '+r.stderr.slice(-4000));
    r=run('/usr/bin/git',['-C',checkout,'checkout','--detach',binding.head],{env,timeoutMs:60000});
    if(r.status!==0) throw new Error('checkout failed: '+r.stderr.slice(-4000));
    r=run('/usr/bin/git',['-C',checkout,'fetch','--quiet','origin','main'],{env,timeoutMs:60000});
    if(r.status!==0) throw new Error('origin/main fetch failed: '+r.stderr.slice(-4000));

    const head=run('/usr/bin/git',['-C',checkout,'rev-parse','HEAD'],{env}).stdout.trim();
    const tree=run('/usr/bin/git',['-C',checkout,'rev-parse','HEAD^{tree}'],{env}).stdout.trim();
    const remoteMain=run('/usr/bin/git',['-C',checkout,'rev-parse','refs/remotes/origin/main'],{env}).stdout.trim();
    const status=run('/usr/bin/git',['-C',checkout,'status','--porcelain=v1','-uall'],{env}).stdout.trim();
    if(head!==binding.head||tree!==binding.tree||remoteMain!==binding.head||status!==''){
      throw new Error('exact current-main binding changed before preflight');
    }
    completedStepReceipts.push({step:'exact-current-main',state:'PASS',head,tree,remoteMain});

    const args=['--no-daemon','jvmTest','checkVexLifeStrings','--stacktrace'];
    r=run('./gradlew',args,{cwd:checkout,env,timeoutMs:690000});
    const retained=retain(retainedRoot,'release-test-preflight.log',['./gradlew',...args],r);
    retainedEvidenceFiles.push(retained);
    completedStepReceipts.push({
      step:'release-test-preflight',
      state:r.status===0?'PASS':'SAFE_FAILURE',
      exitCode:r.status,signal:r.signal,evidenceRef:retained.targetPath
    });

    if(r.status!==0){
      writeResult({
        disposition:'SAFE_FAILURE',
        stopReasonOrNull:'PREVIEW_RELEASE_TEST_PREFLIGHT_FAILED',
        firstSubstantiveFailureOrNull:failure('RELEASE_TEST_PREFLIGHT',r,retained.targetPath),
        candidateFindings:['RELEASE_TEST_PREFLIGHT_REPRODUCED_FAILURE'],
        harnessFindings:[],environmentFindings:[],unknownFindings:[],
        effects:effects(),
        beforeState:{head:binding.head,tree:binding.tree,publicationPerformed:false},
        afterState:{head,tree,publicationPerformed:false},
        completedStepReceipts,retainedEvidenceFiles
      });
      return;
    }

    writeResult({
      disposition:'PASS',stopReasonOrNull:null,firstSubstantiveFailureOrNull:null,
      candidateFindings:[],harnessFindings:[],environmentFindings:[],unknownFindings:[],
      effects:effects(),
      beforeState:{head:binding.head,tree:binding.tree,publicationPerformed:false},
      afterState:{head,tree,releaseFirstCommand:'PASS',publicationPerformed:false},
      completedStepReceipts,retainedEvidenceFiles
    });
  }catch(error){
    writeResult({
      disposition:'SAFE_FAILURE',stopReasonOrNull:'PREVIEW_RELEASE_PREFLIGHT_STOPPED',
      firstSubstantiveFailureOrNull:{
        failureFamily:'VEXLIFE_PROD_PREVIEW_RELEASE_PREFLIGHT_HARNESS',
        failureFingerprint:sha256(Buffer.from(String(error?.message||error))),
        stage:'HARNESS',summary:String(error?.message||error).slice(0,4000),
        evidenceRef:'payload-result.json'
      },
      candidateFindings:[],harnessFindings:['PREVIEW_RELEASE_PREFLIGHT_STOPPED'],
      environmentFindings:[],unknownFindings:[],effects:effects(),
      beforeState:{head:binding.head,tree:binding.tree},afterState:{},
      completedStepReceipts,retainedEvidenceFiles
    });
  }finally{
    if(work){try{fs.rmSync(work,{recursive:true,force:true});}catch(_){}}
  }
}
main().catch(error=>{
  try{writeResult({
    disposition:'SAFE_FAILURE',stopReasonOrNull:'UNCAUGHT_PREVIEW_RELEASE_PREFLIGHT_FAILURE',
    firstSubstantiveFailureOrNull:{
      failureFamily:'VEXLIFE_PROD_PREVIEW_RELEASE_PREFLIGHT_UNCAUGHT',
      failureFingerprint:sha256(Buffer.from(String(error?.message||error))),
      stage:'UNCAUGHT',summary:String(error?.message||error).slice(0,4000),
      evidenceRef:'payload-result.json'
    },
    candidateFindings:[],harnessFindings:['UNCAUGHT_PREVIEW_RELEASE_PREFLIGHT_FAILURE'],
    environmentFindings:[],unknownFindings:[],effects:effects(),
    beforeState:{},afterState:{},completedStepReceipts:[],retainedEvidenceFiles:[]
  });}catch(_){}
  process.exitCode=0;
});

// [VXG RealForever]
