#!/usr/bin/env node
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const os = require('node:os');
const crypto = require('node:crypto');
const { spawnSync } = require('node:child_process');

const RESULT_SCHEMA='vextreme.vexlocalbridge-mac-payload-result/v1';

function sha256Bytes(v){return crypto.createHash('sha256').update(v).digest('hex');}
function sha256File(p){return sha256Bytes(fs.readFileSync(p));}
function writeResult(v){
  const root=process.env.VEXLOCALBRIDGE_RESULT_DIR;
  if(!root) throw new Error('VEXLOCALBRIDGE_RESULT_DIR unavailable');
  fs.mkdirSync(root,{recursive:true});
  fs.writeFileSync(path.join(root,'payload-result.json'),JSON.stringify({schemaVersion:RESULT_SCHEMA,acceptedExitCodes:[0],...v},null,2)+'\n');
}
function effects(){
  return {
    sourceMutationPerformed:false,branchMutationPerformed:false,claimMutationPerformed:false,
    prMetadataMutationPerformed:false,reviewPerformed:false,approvalPerformed:false,
    readyTransitionPerformed:false,mergePerformed:false,mainMutationPerformed:false,
    checkpointMutationPerformed:false,forcePushPerformed:false,historyRewritePerformed:false,
    hostInstallationPerformed:false,modelRuntimePerformed:false,providerOrNetworkEffectPerformed:true,
    publicationPerformed:false,credentialSerializationPerformed:false,
    filesystemOutsideQualifiedRootsPerformed:false,
  };
}
function run(cmd,args,opt={}){
  const r=spawnSync(cmd,args,{
    cwd:opt.cwd,env:opt.env||process.env,encoding:'utf8',
    timeout:opt.timeoutMs||1200000,maxBuffer:128*1024*1024,stdio:['ignore','pipe','pipe']
  });
  return {status:Number.isInteger(r.status)?r.status:127,signal:r.signal||null,stdout:String(r.stdout||''),stderr:String(r.stderr||(r.error?r.error.message:''))};
}
function retain(root,name,content){
  const full=path.join(root,name);
  fs.mkdirSync(path.dirname(full),{recursive:true});
  if(Buffer.isBuffer(content)) fs.writeFileSync(full,content); else fs.writeFileSync(full,String(content));
  return {
    sourcePath:full,
    targetPath:'retained/inner-vexrelay/candidate-dmg/'+name,
    expectedSha256:sha256File(full),
  };
}
function failure(stage,result,evidenceRef){
  const tail=(result.stdout+'\n'+result.stderr).slice(-10000);
  return {
    failureFamily:'VEXLIFE_PROD_PR281_CANDIDATE_DMG',
    failureFingerprint:sha256Bytes(Buffer.from(stage+'\n'+tail)),
    stage,
    summary:stage+' failed exit='+result.status+(result.signal?' signal='+result.signal:'')+'; see '+evidenceRef,
    evidenceRef,
  };
}

async function main(){
  const bindingPath=process.argv[2];
  if(!bindingPath) throw new Error('candidate binding path required');
  const binding=JSON.parse(fs.readFileSync(path.resolve(bindingPath),'utf8'));
  if(process.platform!=='darwin'||process.arch!=='arm64') throw new Error('candidate DMG build requires Apple-Silicon macOS');

  const capsuleRoot=process.env.VEXLOCALBRIDGE_CAPSULE_ROOT;
  if(!capsuleRoot||!path.isAbsolute(capsuleRoot)) throw new Error('VEXLOCALBRIDGE_CAPSULE_ROOT unavailable');
  const retainedRoot=path.join(capsuleRoot,'bridge','retained-diagnostics',process.env.VEXLOCALBRIDGE_TASK_REF||'task',process.env.VEXLOCALBRIDGE_ATTEMPT_REF||'attempt');
  fs.mkdirSync(retainedRoot,{recursive:true});
  const retainedEvidenceFiles=[];
  const completedStepReceipts=[];
  let work=null;

  try{
    const gh=['/opt/homebrew/bin/gh','/usr/local/bin/gh'].find(fs.existsSync);
    if(!gh) throw new Error('GitHub CLI unavailable');
    const auth=run(gh,['auth','status','--hostname','github.com'],{timeoutMs:30000});
    if(auth.status!==0) throw new Error('GitHub CLI authentication unavailable');

    const jh=run('/usr/libexec/java_home',['-v','21'],{timeoutMs:10000});
    if(jh.status!==0) throw new Error('Java 21 home unavailable');
    const javaHome=jh.stdout.trim();
    const env={...process.env,JAVA_HOME:javaHome,PATH:javaHome+'/bin:'+process.env.PATH};

    work=fs.mkdtempSync(path.join(process.env.TMPDIR||os.tmpdir(),'vexlife-pr281-candidate-dmg-'));
    const checkout=path.join(work,'candidate');
    let r=run(gh,['repo','clone',binding.repository,checkout,'--','--no-checkout'],{env,timeoutMs:180000});
    if(r.status!==0) throw new Error('candidate clone failed: '+r.stderr.slice(-3000));
    r=run('/usr/bin/git',['-C',checkout,'checkout','--detach',binding.head],{env,timeoutMs:60000});
    if(r.status!==0) throw new Error('candidate checkout failed: '+r.stderr.slice(-3000));

    const head=run('/usr/bin/git',['-C',checkout,'rev-parse','HEAD'],{env}).stdout.trim();
    const tree=run('/usr/bin/git',['-C',checkout,'rev-parse','HEAD^{tree}'],{env}).stdout.trim();
    const status=run('/usr/bin/git',['-C',checkout,'status','--porcelain=v1','-uall'],{env}).stdout.trim();
    if(head!==binding.head||tree!==binding.tree||status!=='') throw new Error('candidate source identity/currentness mismatch');
    completedStepReceipts.push({step:'exact-source',state:'PASS',head,tree});

    const buildArgs=['--no-daemon','-Dorg.gradle.java.home='+javaHome,'-PvexlifeAppVersion=0.0.0','archiveVexLifeProdMacApplication','--stacktrace'];
    r=run('./gradlew',buildArgs,{cwd:checkout,env,timeoutMs:1800000});
    const buildLog=retain(retainedRoot,'build.log',[
      'command=./gradlew '+buildArgs.join(' '),
      'exitCode='+r.status,
      'signal='+(r.signal||''),
      '',
      '===== STDOUT =====',r.stdout,'','===== STDERR =====',r.stderr,''
    ].join('\n'));
    retainedEvidenceFiles.push(buildLog);
    completedStepReceipts.push({step:'archive-mac-application',state:r.status===0?'PASS':'SAFE_FAILURE',exitCode:r.status,evidenceRef:buildLog.targetPath});
    if(r.status!==0){
      writeResult({
        disposition:'SAFE_FAILURE',stopReasonOrNull:'PR281_CANDIDATE_DMG_BUILD_FAILED',
        firstSubstantiveFailureOrNull:failure('ARCHIVE_MAC_APPLICATION',r,buildLog.targetPath),
        candidateFindings:['CANDIDATE_DMG_BUILD_FAILURE'],harnessFindings:[],environmentFindings:[],unknownFindings:[],
        effects:effects(),beforeState:{candidateHead:binding.head,candidateTree:binding.tree},
        afterState:{candidateHead:head,candidateTree:tree,failedStage:'ARCHIVE_MAC_APPLICATION'},
        completedStepReceipts,retainedEvidenceFiles
      });
      return;
    }

    const appDir=path.join(checkout,'build','vexlife-prod-mac-application',binding.head);
    const dmg=path.join(appDir,'VexLife-macOS-AppleSilicon.dmg');
    const shaFile=path.join(appDir,'VexLife-macOS-AppleSilicon.dmg.sha256');
    const receiptFile=path.join(appDir,'VexLife-macOS-AppleSilicon.receipt.json');
    for(const p of [dmg,shaFile,receiptFile]) if(!fs.existsSync(p)||!fs.statSync(p).isFile()) throw new Error('expected candidate artifact missing: '+p);

    const receipt=JSON.parse(fs.readFileSync(receiptFile,'utf8'));
    if(receipt.sourceSha!==binding.head) throw new Error('candidate DMG receipt sourceSha mismatch');

    const expectedLine=fs.readFileSync(shaFile,'utf8').trim();
    const expectedSha=expectedLine.split(/\s+/)[0];
    const observedSha=sha256File(dmg);
    if(expectedSha!==observedSha) throw new Error('candidate DMG sha256 sidecar mismatch');

    const verify=run('/usr/bin/hdiutil',['verify',dmg],{timeoutMs:120000});
    const verifyLog=retain(retainedRoot,'hdiutil-verify.log',verify.stdout+(verify.stderr?'\nSTDERR:\n'+verify.stderr:''));
    retainedEvidenceFiles.push(verifyLog);
    if(verify.status!==0) throw new Error('candidate DMG hdiutil verify failed');

    const renamed='VexLife-PR281-CANDIDATE-'+binding.head.slice(0,12)+'.dmg';
    retainedEvidenceFiles.push(retain(retainedRoot,renamed,fs.readFileSync(dmg)));
    retainedEvidenceFiles.push(retain(retainedRoot,renamed+'.sha256',observedSha+'  '+renamed+'\n'));
    retainedEvidenceFiles.push(retain(retainedRoot,'VexLife-PR281-CANDIDATE-receipt.json',fs.readFileSync(receiptFile)));

    completedStepReceipts.push({step:'candidate-dmg-receipt-source',state:'PASS',sourceSha:receipt.sourceSha});
    completedStepReceipts.push({step:'candidate-dmg-sha256',state:'PASS',sha256:observedSha});
    completedStepReceipts.push({step:'candidate-dmg-hdiutil-verify',state:'PASS'});

    writeResult({
      disposition:'PASS',stopReasonOrNull:null,firstSubstantiveFailureOrNull:null,
      candidateFindings:[],harnessFindings:[],environmentFindings:[],unknownFindings:[],
      effects:effects(),
      beforeState:{candidateHead:binding.head,candidateTree:binding.tree,publicationPerformed:false,hostInstallationPerformed:false},
      afterState:{
        candidateHead:head,candidateTree:tree,
        candidateDmgName:renamed,candidateDmgSha256:observedSha,
        receiptSourceSha:receipt.sourceSha,
        hdiutilVerify:'PASS',
        publicationPerformed:false,hostInstallationPerformed:false,
      },
      completedStepReceipts,retainedEvidenceFiles
    });
  } catch(error){
    writeResult({
      disposition:'SAFE_FAILURE',stopReasonOrNull:'PR281_CANDIDATE_DMG_STOPPED',
      firstSubstantiveFailureOrNull:{
        failureFamily:'VEXLIFE_PROD_PR281_CANDIDATE_DMG_HARNESS',
        failureFingerprint:sha256Bytes(Buffer.from(String(error?.message||error))),
        stage:'CANDIDATE_DMG_HARNESS',summary:String(error?.message||error).slice(0,4000),evidenceRef:'payload-result.json'
      },
      candidateFindings:[],harnessFindings:['CANDIDATE_DMG_HARNESS_STOPPED'],environmentFindings:[],unknownFindings:[],
      effects:effects(),beforeState:{candidateHead:binding.head,candidateTree:binding.tree},
      afterState:{errorClass:error?.constructor?.name||'Error'},completedStepReceipts,retainedEvidenceFiles
    });
  } finally {
    if(work){try{fs.rmSync(work,{recursive:true,force:true});}catch(_){}}
  }
}
main().catch((error)=>{try{writeResult({
  disposition:'SAFE_FAILURE',stopReasonOrNull:'UNCAUGHT_PR281_CANDIDATE_DMG_FAILURE',
  firstSubstantiveFailureOrNull:{failureFamily:'VEXLIFE_PROD_PR281_CANDIDATE_DMG_UNCAUGHT',failureFingerprint:sha256Bytes(Buffer.from(String(error?.message||error))),stage:'UNCAUGHT',summary:String(error?.message||error).slice(0,4000),evidenceRef:'payload-result.json'},
  candidateFindings:[],harnessFindings:['UNCAUGHT_PR281_CANDIDATE_DMG_FAILURE'],environmentFindings:[],unknownFindings:[],effects:effects(),beforeState:{},afterState:{},completedStepReceipts:[],retainedEvidenceFiles:[]
});}catch(_){} process.exitCode=0;});

// [VXG RealForever]
