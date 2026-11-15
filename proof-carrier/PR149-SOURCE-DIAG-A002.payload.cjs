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
function tail(v,n=6000){v=String(v||'');return v.length<=n?v:v.slice(-n);}
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
    timeout:opts.timeoutMs||600000,maxBuffer:64*1024*1024,stdio:['ignore','pipe','pipe']
  });
  if(r.error) return {status:null,error:String(r.error),stdout:String(r.stdout||''),stderr:String(r.stderr||'')};
  return {status:r.status,error:null,stdout:String(r.stdout||''),stderr:String(r.stderr||'')};
}
function listFiles(root,out=[]){
  if(!fs.existsSync(root)) return out;
  for(const name of fs.readdirSync(root)){
    const p=path.join(root,name);
    let st; try{st=fs.statSync(p);}catch(_){continue;}
    if(st.isDirectory()) listFiles(p,out); else out.push(p);
  }
  return out;
}
function jvmTestFailureSummary(repo){
  const root=path.join(repo,'build','test-results','jvmTest');
  const chunks=[];
  for(const file of listFiles(root).filter(p=>p.endsWith('.xml')).sort()){
    const text=fs.readFileSync(file,'utf8');
    if(!/<failure\b|<error\b/.test(text)) continue;
    const suite=(text.match(/<testsuite[^>]*name="([^"]+)"/)||[])[1]||path.basename(file);
    for(const m of text.matchAll(/<testcase[^>]*name="([^"]+)"[^>]*>[\s\S]*?(?:<failure|<error)([^>]*)>([\s\S]*?)(?:<\/failure>|<\/error>)[\s\S]*?<\/testcase>/g)){
      const attrs=m[2]||'';
      const msg=(attrs.match(/message="([^"]*)"/)||[])[1]||'';
      const body=(m[3]||'').replace(/&lt;/g,'<').replace(/&gt;/g,'>').replace(/&amp;/g,'&');
      chunks.push(suite+' :: '+m[1]+' :: '+msg+'\n'+tail(body,1200));
    }
  }
  return tail(chunks.join('\n---\n'),5000);
}
function failureRecord(task,stdout,stderr,testFailures){
  const terminal=[
    'task='+task,
    testFailures?('testFailures='+tail(testFailures,1200)):'',
    'stdoutTail='+tail(stdout,900),
    'stderrTail='+tail(stderr,900)
  ].filter(Boolean).join('\n');
  return {
    failureFamily:'VEXLIFE_PROD_PR149_SOURCE_DIAGNOSTIC',
    failureFingerprint:sha256(Buffer.from(terminal)),
    stage:'EXACT_SOURCE_DIAGNOSTIC',
    summary:terminal,
    evidenceRef:'after-state.json'
  };
}

async function main(){
  const binding=JSON.parse(fs.readFileSync(path.resolve(process.argv[2]),'utf8'));
  const base={
    effects:effects(),candidateFindings:[],harnessFindings:[],environmentFindings:[],
    unknownFindings:[],beforeState:{candidateHead:binding.head,candidateTree:binding.tree},
    afterState:{},completedStepReceipts:[]
  };
  const work=fs.mkdtempSync(path.join(process.env.TMPDIR||os.tmpdir(),'vexlife-pr149-source-diag-a002-'));
  const repo=path.join(work,'candidate');
  try{
    let r=run('/usr/bin/git',['clone','--no-checkout','https://github.com/'+binding.repository+'.git',repo],{timeoutMs:180000});
    if(r.status!==0) throw new Error('clone failed: '+tail(r.stderr,2000));
    r=run('/usr/bin/git',['-C',repo,'checkout','--detach',binding.head],{timeoutMs:60000});
    if(r.status!==0) throw new Error('checkout failed: '+tail(r.stderr,2000));
    const head=run('/usr/bin/git',['-C',repo,'rev-parse','HEAD']).stdout.trim();
    const tree=run('/usr/bin/git',['-C',repo,'rev-parse','HEAD^{tree}']).stdout.trim();
    const dirty=run('/usr/bin/git',['-C',repo,'status','--porcelain=v1','-uall']).stdout.trim();
    if(head!==binding.head||tree!==binding.tree||dirty!=='') throw new Error('exact candidate mismatch or dirty clone');
    base.completedStepReceipts.push({step:'exact-candidate-clone',state:'PASS',head,tree});

    const javaProbe=run('/usr/libexec/java_home',['-v','21'],{timeoutMs:10000});
    const javaHome=javaProbe.status===0?javaProbe.stdout.trim():'';
    const javaVersion=javaHome?run(path.join(javaHome,'bin','java'),['-version'],{timeoutMs:10000}):{status:null,stdout:'',stderr:'java_home -v 21 did not resolve'};
    base.beforeState.javaHomeRequested21=javaHome||null;
    base.beforeState.javaVersionObserved=tail(javaVersion.stderr||javaVersion.stdout,2000);
    base.beforeState.javaHomeProbeStderr=tail(javaProbe.stderr,2000);
    const env=javaHome?{...process.env,JAVA_HOME:javaHome,PATH:javaHome+'/bin:'+process.env.PATH}:process.env;

    const tasks=[
      'compileKotlinJvm',
      'compileTestKotlinJvm',
      'jvmTest',
      'compileKotlinJs',
      'compileTestKotlinJs',
      'checkVexLifeStrings',
      'assembleVexLifeProdServiceImage'
    ];
    const taskReceipts=[];
    for(const task of tasks){
      const g=run('./gradlew',[
        '--no-daemon','--console=plain','--stacktrace',
        ...(javaHome?['-Dorg.gradle.java.home='+javaHome]:[]),
        task
      ],{cwd:repo,timeoutMs:600000,env});
      const receipt={
        task,status:g.status,error:g.error,
        stdoutTail:tail(g.stdout,6000),
        stderrTail:tail(g.stderr,6000)
      };
      if(task==='jvmTest'&&g.status!==0) receipt.testFailureSummary=jvmTestFailureSummary(repo);
      taskReceipts.push(receipt);
      if(g.status!==0){
        base.afterState={
          machineState:'SAFE_FAILURE',
          failedTask:task,
          gradleExitCode:g.status,
          javaHomeRequested21:javaHome||null,
          javaVersionObserved:base.beforeState.javaVersionObserved,
          stdoutTail:receipt.stdoutTail,
          stderrTail:receipt.stderrTail,
          testFailureSummary:receipt.testFailureSummary||''
        };
        base.completedStepReceipts.push(...taskReceipts.filter(x=>x.status===0).map(x=>({step:'gradle-'+x.task,state:'PASS'})));
        base.candidateFindings.push('EXACT_SOURCE_TASK_FAILED:'+task);
        writeResult({
          disposition:'SAFE_FAILURE',
          stopReasonOrNull:'EXACT_SOURCE_TASK_FAILED:'+task,
          firstSubstantiveFailureOrNull:failureRecord(task,g.stdout,g.stderr,receipt.testFailureSummary),
          ...base
        });
        return;
      }
    }

    base.completedStepReceipts.push(...taskReceipts.map(x=>({step:'gradle-'+x.task,state:'PASS'})));
    base.afterState={
      machineState:'PASS',
      javaHomeRequested21:javaHome||null,
      javaVersionObserved:base.beforeState.javaVersionObserved,
      tasks:tasks
    };
    writeResult({disposition:'PASS',stopReasonOrNull:null,firstSubstantiveFailureOrNull:null,...base});
  }catch(error){
    base.harnessFindings.push('SOURCE_DIAGNOSTIC_STOPPED');
    base.afterState={machineState:'SAFE_FAILURE',errorClass:error?.constructor?.name||'Error',errorMessage:String(error?.message||error)};
    writeResult({
      disposition:'SAFE_FAILURE',stopReasonOrNull:'SOURCE_DIAGNOSTIC_STOPPED',
      firstSubstantiveFailureOrNull:{
        failureFamily:'VEXLIFE_PROD_PR149_SOURCE_DIAGNOSTIC',
        failureFingerprint:sha256(Buffer.from(String(error?.message||error))),
        stage:'SOURCE_DIAGNOSTIC',summary:tail(error?.message||error,1800),evidenceRef:'after-state.json'
      },...base
    });
  }finally{
    try{fs.rmSync(work,{recursive:true,force:true});}catch(_){}
  }
}
main().catch(error=>{
  try{writeResult({
    disposition:'SAFE_FAILURE',stopReasonOrNull:'UNCAUGHT_SOURCE_DIAGNOSTIC',
    firstSubstantiveFailureOrNull:null,candidateFindings:[],harnessFindings:['UNCAUGHT_SOURCE_DIAGNOSTIC'],
    environmentFindings:[],unknownFindings:[],effects:effects(),beforeState:{},
    afterState:{errorMessage:String(error?.message||error)},completedStepReceipts:[]
  });}catch(_){}
  process.exitCode=0;
});
