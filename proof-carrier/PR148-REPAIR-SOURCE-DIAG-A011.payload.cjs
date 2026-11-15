#!/usr/bin/env node
'use strict';

const fs=require('node:fs');
const os=require('node:os');
const path=require('node:path');
const crypto=require('node:crypto');
const {spawnSync}=require('node:child_process');

const RESULT_SCHEMA='vextreme.vexlocalbridge-mac-payload-result/v1';

function sha256(v){return crypto.createHash('sha256').update(v).digest('hex');}
function effects(){return {
  sourceMutationPerformed:false,branchMutationPerformed:false,claimMutationPerformed:false,
  prMetadataMutationPerformed:false,reviewPerformed:false,approvalPerformed:false,
  readyTransitionPerformed:false,mergePerformed:false,mainMutationPerformed:false,
  checkpointMutationPerformed:false,forcePushPerformed:false,historyRewritePerformed:false,
  hostInstallationPerformed:false,modelRuntimePerformed:false,providerOrNetworkEffectPerformed:true,
  publicationPerformed:false,credentialSerializationPerformed:false,
  filesystemOutsideQualifiedRootsPerformed:false
};}
function writeResult(v){
  const root=process.env.VEXLOCALBRIDGE_RESULT_DIR;
  if(!root) throw new Error('VEXLOCALBRIDGE_RESULT_DIR unavailable');
  fs.mkdirSync(root,{recursive:true});
  fs.writeFileSync(path.join(root,'payload-result.json'),JSON.stringify({
    schemaVersion:RESULT_SCHEMA,acceptedExitCodes:[0],...v
  },null,2)+'\n');
}
function run(cmd,args,opts={}){
  const r=spawnSync(cmd,args,{
    cwd:opts.cwd,env:opts.env||process.env,encoding:'utf8',
    timeout:opts.timeoutMs||600000,maxBuffer:64*1024*1024,
    stdio:['ignore','pipe','pipe']
  });
  if(r.error) throw r.error;
  return r;
}
function tail(s,n=50000){
  const v=String(s||'');
  return v.length<=n?v:v.slice(v.length-n);
}
function failure(error,stage){
  const summary=String(error?.message||error).slice(0,1200);
  return {
    failureFamily:'VEXLIFE_PROD_REPAIR_SOURCE_DIAGNOSTIC',
    failureFingerprint:sha256(Buffer.from(stage+'\n'+summary)),
    stage,summary,evidenceRef:'payload-result.json'
  };
}

async function main(){
  const binding=JSON.parse(fs.readFileSync(path.resolve(process.argv[2]),'utf8'));
  const base={
    effects:effects(),candidateFindings:[],harnessFindings:[],environmentFindings:[],
    unknownFindings:[],beforeState:{candidateHead:binding.head,candidateTree:binding.tree},
    afterState:{},completedStepReceipts:[]
  };
  const work=fs.mkdtempSync(path.join(process.env.TMPDIR||os.tmpdir(),'vexlife-pr148-repair-a011-'));
  const repo=path.join(work,'candidate');
  try{
    const clone=run('/usr/bin/git',['clone','--no-checkout','https://github.com/'+binding.repository+'.git',repo],{timeoutMs:180000});
    if(clone.status!==0) throw new Error('clone failed: '+tail(clone.stderr,8000));
    let r=run('/usr/bin/git',['-C',repo,'checkout','--detach',binding.head],{timeoutMs:60000});
    if(r.status!==0) throw new Error('checkout failed: '+tail(r.stderr,8000));
    const head=run('/usr/bin/git',['-C',repo,'rev-parse','HEAD']).stdout.trim();
    const tree=run('/usr/bin/git',['-C',repo,'rev-parse','HEAD^{tree}']).stdout.trim();
    if(head!==binding.head||tree!==binding.tree) throw new Error('exact candidate mismatch');
    base.completedStepReceipts.push({step:'exact-candidate-clone',state:'PASS',head,tree});

    const javaProbe=run('/usr/libexec/java_home',['-v','21'],{timeoutMs:10000});
    const javaHome=javaProbe.status===0?javaProbe.stdout.trim():'';
    const javaVersion=javaHome
      ? run(path.join(javaHome,'bin/java'),['-version'],{timeoutMs:10000})
      : {status:1,stdout:'',stderr:'JDK 21 unavailable'};
    base.beforeState.javaHomeRequested21=javaHome||null;
    base.beforeState.javaVersionProbe=tail(javaVersion.stderr||javaVersion.stdout,4000);

    // Do not trust java_home's label alone. The Product's source-owned service-image task still
    // proves JDK21 independently; this diagnostic records what Gradle itself was launched with.
    const env=javaHome
      ? {...process.env,JAVA_HOME:javaHome,PATH:javaHome+'/bin:'+process.env.PATH}
      : process.env;

    const gradle=run('./gradlew',[
      '--no-daemon','--console=plain','--stacktrace',
      ...(javaHome?['-Dorg.gradle.java.home='+javaHome]:[]),
      'jvmTest','compileKotlinJs','compileTestKotlinJs',
      'checkVexLifeStrings','assembleVexLifeProdServiceImage'
    ],{cwd:repo,timeoutMs:600000,env});

    base.afterState.gradleExitCode=gradle.status;
    base.afterState.gradleStdoutTail=tail(gradle.stdout);
    base.afterState.gradleStderrTail=tail(gradle.stderr);

    if(gradle.status!==0){
      base.candidateFindings.push('EXACT_REPAIR_SOURCE_PREFLIGHT_FAILED');
      writeResult({
        disposition:'SAFE_FAILURE',
        stopReasonOrNull:'EXACT_REPAIR_SOURCE_PREFLIGHT_FAILED',
        firstSubstantiveFailureOrNull:failure(
          new Error('Gradle source preflight exit='+gradle.status),
          'EXACT_SOURCE_PREFLIGHT'
        ),
        ...base
      });
      return;
    }

    base.completedStepReceipts.push({
      step:'exact-source-preflight',state:'PASS',
      tasks:['jvmTest','compileKotlinJs','compileTestKotlinJs','checkVexLifeStrings','assembleVexLifeProdServiceImage']
    });
    writeResult({
      disposition:'PASS',stopReasonOrNull:null,firstSubstantiveFailureOrNull:null,...base
    });
  }catch(error){
    base.harnessFindings.push('SOURCE_DIAGNOSTIC_STOPPED');
    base.afterState.errorClass=error?.constructor?.name||'Error';
    base.afterState.errorMessage=String(error?.message||error).slice(0,3000);
    writeResult({
      disposition:'SAFE_FAILURE',stopReasonOrNull:'SOURCE_DIAGNOSTIC_STOPPED',
      firstSubstantiveFailureOrNull:failure(error,'SOURCE_DIAGNOSTIC'),...base
    });
  }finally{
    try{fs.rmSync(work,{recursive:true,force:true});}catch(_){}
  }
}
main().catch(error=>{
  try{writeResult({
    disposition:'SAFE_FAILURE',stopReasonOrNull:'UNCAUGHT_SOURCE_DIAGNOSTIC',
    firstSubstantiveFailureOrNull:failure(error,'UNCAUGHT'),
    candidateFindings:[],harnessFindings:['UNCAUGHT_SOURCE_DIAGNOSTIC'],
    environmentFindings:[],unknownFindings:[],effects:effects(),
    beforeState:{},afterState:{errorMessage:String(error?.message||error)},completedStepReceipts:[]
  });}catch(_){}
  process.exitCode=0;
});
