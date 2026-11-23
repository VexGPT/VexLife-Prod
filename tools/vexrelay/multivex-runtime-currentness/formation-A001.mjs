import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import crypto from 'node:crypto';
import { createRequire } from 'node:module';

const [sdkRootArg,outputDirectoryArg,payloadSourceArg]=process.argv.slice(2);
if(!sdkRootArg||!outputDirectoryArg||!payloadSourceArg){
  throw new Error('usage: node formation-A001.mjs <sdk-root> <output-directory> <payload-source>');
}
const sdkRoot=path.resolve(sdkRootArg);
const outputDirectory=path.resolve(outputDirectoryArg);
const payloadSource=path.resolve(payloadSourceArg);
const require=createRequire(import.meta.url);
const packageBuilder=require(path.join(sdkRoot,'lib/vexlocalbridge/package-builder.js'));
const policy=require(path.join(sdkRoot,'lib/vexlocalbridge/vexlife-prod-host-effect-policy.js'));
const {bindAuthoritySummaryFromEnvelope,createCanonicalTaskZip,hashFile,writeJson}=packageBuilder;

const BASE='f1855e9be6b9172ba6b43b597fa5d902dc409ed1';
const HEAD='f9cc4a05ab2be6a45127ad4badaab8c9110afbb4';
const TREE='220e7ddccca1b45ae56e576041ce2bc860dbf229';
const BRANCH='VXG-112226-evolve-context-dynamic-counterpart-dcp1-a001';
const SDK_HEAD='149455eeec4c7b7ca00e91022154a85faa3d608c';
const SDK_TREE='53ea701f9bbf24a5cf37d43e4dbe3836187f4011';

const profilePath=path.join(sdkRoot,'docs/private-continuity/vexlocalbridge/profiles/macos-victor-vexlife-prod.json');
const profile=JSON.parse(fs.readFileSync(profilePath,'utf8'));
policy.validateProfileBinding(profile);
if(!fs.existsSync(payloadSource)||!fs.statSync(payloadSource).isFile()){
  throw new Error('MULTI_VEX_RUNTIME_CURRENTNESS_PAYLOAD_MISSING');
}

const suffix=crypto.randomBytes(10).toString('hex');
const workRoot=fs.mkdtempSync(path.join(os.tmpdir(),'multivex-runtime-currentness-'));
try{
  const contentRoot=path.join(workRoot,'content');
  for(const dir of ['authority','payload','inputs']) fs.mkdirSync(path.join(contentRoot,dir),{recursive:true});

  const authorityEnvelope={
    authoritySchemaRef:'vextreme.effect-transaction/v1',
    semanticAdmissionRef:'github.pull.vexlife-prod.543',
    transactionRef:'transaction.vexlife-prod.multivex.runtime-currentness.a001.'+suffix,
    transactionVersion:1,
    allowedEffectRefs:[
      'effect.vexlocalbridge.vexlife-prod.product-lifecycle-proof/v1',
      'effect.vexlocalbridge.vexlife-prod.build-dependency-fetch/v1',
    ],
    heldEffectRefs:[
      'effect.vexlife-prod.source-mutation',
      'effect.vexlife-prod.host-installation',
      'effect.vexlife-prod.home-recreation',
      'effect.vexlife-prod.home-deletion',
      'effect.vexlife-prod.canonical-conversation-mutation',
      'effect.vexlife-prod.canonical-memory-mutation',
      'effect.vexlife-prod.model-acquisition',
      'effect.vexlife-prod.model-switch-for-proof',
      'effect.vexlife-prod.runtime-repair',
      'effect.vexlife-prod.runtime-restart',
      'effect.vexlife-prod.foundation-mutation',
      'effect.vexlife-prod.dream',
      'effect.vexlife-prod.training',
      'effect.vexlife-prod.publication',
      'effect.vexlife-prod.ready',
      'effect.vexlife-prod.merge',
    ],
  };

  const binding={
    schemaVersion:'vexlife-prod.multivex-runtime-currentness-binding/v1',
    continuity:'[VXG RealForever]',
    key:'[Multi-Vex]',
    purpose:'Read-only diagnosis after neutral-lived A001 stopped at PRODUCT_RUNTIME_SETUP because the existing Product runtime receipt did not match current binding.',
    candidate:{repository:'VexGPT/VexLife-Prod',base:BASE,head:HEAD,tree:TREE,branch:BRANCH},
    priorSuccessfulRuntime:{
      sourceReturnSha256:'c0e9147e9127c842eadcd5c4097461729d8a6ebc513b374272c4d9f3e73d38dd',
      receiptRef:'receipt.vexlife-prod.heretic-persistent-runtime.6bb92df935fd3446f35a60a3',
      pid:31743,
      promptCacheEntries:2,
    },
    failedNeutralReturnSha256:'e31d898ad9c39e671de6b1e719f043b4ba341610f8717ae74a94d5cf42307b18',
    carrier:{sdkHead:SDK_HEAD,sdkTree:SDK_TREE,profileRef:profile.profileRef,policyRef:profile.vexLifeProdHostEffectPolicy.policyRef},
    acceptance:{
      readOnly:true,
      repairAllowed:false,
      runtimeRestartAllowed:false,
      modelAcquisitionAllowed:false,
      modelSwitchAllowed:false,
      homeMutationAllowed:false,
    },
  };

  const authorityPath=path.join(contentRoot,'authority/effect.json');
  const payloadPath=path.join(contentRoot,'payload/runtime-currentness.mjs');
  const bindingPath=path.join(contentRoot,'inputs/runtime-currentness-binding.json');
  writeJson(authorityPath,authorityEnvelope);
  fs.copyFileSync(payloadSource,payloadPath);
  writeJson(bindingPath,binding);

  const taskTemplate={
    schemaVersion:'vextreme.vexlocalbridge-task-manifest/v1',
    artifactClass:'EXECUTABLE_RELAY',
    taskRef:'task.vexlife-prod.multivex.runtime-currentness.a001',
    attemptRef:'attempt.vexlife-prod.multivex.runtime-currentness.a001.'+suffix,
    packageRef:'package.vexlife-prod.multivex.runtime-currentness.a001.'+suffix,
    requestRef:'request.vexlife-prod.multivex.runtime-currentness.a001.'+suffix,
    correlationRef:'correlation.vexlife-prod.multivex.runtime-currentness.a001.'+suffix,
    taskStorageKey:'vexlife-prod-multivex-runtime-currentness',
    attemptStorageKey:'attempt-a001-'+suffix,
    requestingRoleRef:'Operations[Multi-Vex-Runtime-Currentness]',
    requestingInstanceRef:'instance.chatgpt.multivex.runtime-currentness.a001.'+suffix,
    parentOperationsRef:'github.pull.vexlife-prod.543',
    returnRouteRef:'github.pull.vexlife-prod.543',
    hostProfileRef:'macos-victor',
    repositoryExecutionProfileRefOrNull:'profile.repository.vexlife-prod.macos-v1',
    repositoryRefOrNull:'VexGPT/VexLife-Prod',
    baseRefOrNull:BASE,
    headRefOrNull:HEAD,
    treeRefOrNull:TREE,
    branchRefOrNull:BRANCH,
    claimRefOrNull:'github.pull.vexlife-prod.543',
    exactPathRefs:[],
    authorityBinding:{
      authoritySchemaRef:authorityEnvelope.authoritySchemaRef,
      authorityEnvelopePath:'authority/effect.json',
      authorityEnvelopeSha256:hashFile(authorityPath),
      semanticAdmissionRef:'stale',transactionRef:'stale',transactionVersion:1,
      allowedEffectRefs:['stale'],heldEffectRefs:[],
    },
    payload:{
      payloadClass:'NODE_SCRIPT',
      payloadRuntime:'node',
      payloadPath:'payload/runtime-currentness.mjs',
      payloadSha256:hashFile(payloadPath),
      typedArgv:[],
      immutableInputs:[{path:'inputs/runtime-currentness-binding.json',sha256:hashFile(bindingPath)}],
    },
    executionStateAtFormation:'TASK_PREPARED',
    singleUseNonce:'nonce-multivex-runtime-currentness-a001-'+suffix+'-'+crypto.randomBytes(16).toString('hex'),
    stopConditions:[
      'STOP_ON_TASK_BINDING_DRIFT',
      'STOP_ON_HOME_COORDINATE_INVALID',
      'STOP_ON_RUNTIME_COORDINATE_CHANGE_DURING_OBSERVATION',
    ],
    archiveDigestBinding:'FILENAME_SHA256',
  };

  const formed=createCanonicalTaskZip({
    taskTemplate:bindAuthoritySummaryFromEnvelope(taskTemplate,authorityEnvelope),
    contentRoot,outputDirectory,
    outputStem:'VEXLIFE-MULTI-VEX-RUNTIME-CURRENTNESS-A001',
    ttlMs:3*60*60*1000,
  });

  const admitted=policy.admitTask({profile,task:formed.task,authorityEnvelope});
  if(admitted.candidate.base!==BASE||admitted.candidate.head!==HEAD||admitted.candidate.tree!==TREE){
    throw new Error('MULTI_VEX_RUNTIME_CURRENTNESS_ADMISSION_MISMATCH');
  }

  process.stdout.write(JSON.stringify({
    disposition:'PASS',packagePath:formed.finalZip,packageSha256:formed.digest,
    taskRef:formed.task.taskRef,attemptRef:formed.task.attemptRef,
    candidate:binding.candidate,priorSuccessfulRuntime:binding.priorSuccessfulRuntime,
    failedNeutralReturnSha256:binding.failedNeutralReturnSha256,
  })+'\n');
}finally{
  fs.rmSync(workRoot,{recursive:true,force:true});
}
