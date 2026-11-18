#!/usr/bin/env node
'use strict';

const fs=require('node:fs');
const os=require('node:os');
const path=require('node:path');

const [sdkRootArg,payloadPathArg]=process.argv.slice(2);
if(!sdkRootArg||!payloadPathArg){
  throw new Error('usage: VEXLIFE-PREVIEW-RELEASE-PREFLIGHT-D2B241-A001.form.cjs <sdk-root> <payload>');
}
const sdkRoot=path.resolve(sdkRootArg);
const payloadSource=path.resolve(payloadPathArg);
const {
  createCanonicalTaskZip,writeJson,hashFile,bindAuthoritySummaryFromEnvelope
}=require(path.join(sdkRoot,'lib/vexlocalbridge/package-builder.js'));

const binding={
  schemaVersion:'vexlife-prod.preview-release-preflight/v1',
  continuity:'[VXG RealForever]',
  repository:'VexGPT/VexLife-Prod',
  branch:'main',
  head:'d2b241b903e34e0e14b3c561ffcfa77ae367662a',
  tree:'d94a7f9c1917bbb1ca32fca87e4a0e5b9f62af46',
};

const authorityEnvelope={
  authoritySchemaRef:'vextreme.effect-transaction/v1',
  semanticAdmissionRef:'user.victor.vexlife-preview-release-preflight.20261118',
  transactionRef:'transaction.vexlife-prod.preview-release-preflight.d2b241.a001',
  transactionVersion:1,
  allowedEffectRefs:[
    'effect.vexlife-prod.preview.exact-main-read',
    'effect.vexlife-prod.preview.local-test-proof',
    'effect.vexlife-prod.preview.diagnostic-evidence-retention',
  ],
  heldEffectRefs:[
    'effect.vexlife-prod.source-mutation',
    'effect.vexlife-prod.branch-mutation',
    'effect.vexlife-prod.main-mutation',
    'effect.vexlife-prod.pr-metadata-mutation',
    'effect.vexlife-prod.review',
    'effect.vexlife-prod.approval',
    'effect.vexlife-prod.ready',
    'effect.vexlife-prod.merge',
    'effect.vexlife-prod.publication',
    'effect.vexlife-prod.host-installation',
    'effect.vexlife-prod.home-mutation',
    'effect.vexlife-prod.runtime-lifecycle',
  ],
};

const root=fs.mkdtempSync(path.join(os.tmpdir(),'vexlife-preview-release-preflight-d2b241-form-'));
try{
  const authorityPath=path.join(root,'authority/effect.json');
  const payloadPath=path.join(root,'payload/run.cjs');
  const bindingPath=path.join(root,'inputs/binding.json');
  fs.mkdirSync(path.dirname(authorityPath),{recursive:true});
  fs.mkdirSync(path.dirname(payloadPath),{recursive:true});
  fs.mkdirSync(path.dirname(bindingPath),{recursive:true});
  writeJson(authorityPath,authorityEnvelope);
  fs.copyFileSync(payloadSource,payloadPath);
  writeJson(bindingPath,binding);

  const task=bindAuthoritySummaryFromEnvelope({
    schemaVersion:'vextreme.vexlocalbridge-task-manifest/v1',
    artifactClass:'EXECUTABLE_RELAY',
    taskRef:'task.vexlife-prod.preview-release-preflight.d2b241.001',
    attemptRef:'attempt.vexlife-prod.preview-release-preflight.d2b241.a001',
    packageRef:'package.vexlife-prod.preview-release-preflight.d2b241.a001',
    requestRef:'request.vexlife-prod.preview-release-preflight.d2b241.a001',
    correlationRef:'correlation.vexlife-prod.preview-release-preflight.d2b241.a001',
    taskStorageKey:'vexlife-prod-preview-release-preflight-d2b241',
    attemptStorageKey:'attempt-a001',
    requestingRoleRef:'Operations[VexLife-Prod]',
    requestingInstanceRef:'instance.chatgpt.vexlife-prod.preview-release-preflight.d2b241.a001',
    parentOperationsRef:'work.vexlife-prod.release-alerts.preview-publication.20261118',
    returnRouteRef:'chat.vexlife-prod.preview-release-preflight.20261118',
    hostProfileRef:'macos-victor',
    repositoryExecutionProfileRefOrNull:'profile.repository.vextreme-sdk.macos-v1',
    repositoryRefOrNull:null,
    baseRefOrNull:null,
    headRefOrNull:null,
    treeRefOrNull:null,
    branchRefOrNull:null,
    claimRefOrNull:null,
    exactPathRefs:[],
    authorityBinding:{
      authoritySchemaRef:authorityEnvelope.authoritySchemaRef,
      authorityEnvelopePath:'authority/effect.json',
      authorityEnvelopeSha256:hashFile(authorityPath),
      semanticAdmissionRef:authorityEnvelope.semanticAdmissionRef,
      transactionRef:authorityEnvelope.transactionRef,
      transactionVersion:authorityEnvelope.transactionVersion,
      allowedEffectRefs:[...authorityEnvelope.allowedEffectRefs],
      heldEffectRefs:[...authorityEnvelope.heldEffectRefs],
    },
    payload:{
      payloadClass:'VEXLIFE_PROD_PREVIEW_RELEASE_PREFLIGHT',
      payloadRuntime:'node',
      payloadPath:'payload/run.cjs',
      payloadSha256:hashFile(payloadPath),
      typedArgv:['inputs/binding.json'],
      immutableInputs:[{path:'inputs/binding.json',sha256:hashFile(bindingPath)}],
    },
    executionStateAtFormation:'TASK_PREPARED',
    singleUseNonce:'nonce-vexlife-prod-preview-release-preflight-d2b241-a001',
    stopConditions:[
      'stop.on.exact-main-drift',
      'stop.on.release-test-preflight-failure',
      'stop.on.bound-expiry',
    ],
    archiveDigestBinding:'FILENAME_SHA256',
  },authorityEnvelope);

  const formed=createCanonicalTaskZip({
    taskTemplate:task,
    contentRoot:root,
    outputDirectory:path.join(os.homedir(),'Downloads'),
    outputStem:'VEXLIFE-PREVIEW-RELEASE-PREFLIGHT-D2B241-A001',
    ttlMs:4*60*60*1000,
  });
  process.stdout.write(JSON.stringify({
    state:'PASS',package:formed.finalZip,sha256:formed.digest,
    taskRef:formed.task.taskRef,attemptRef:formed.task.attemptRef,
    formedAt:formed.formationEvidence.formedAt,expiresAt:formed.formationEvidence.expiresAt,
  },null,2)+'\n');
}finally{
  fs.rmSync(root,{recursive:true,force:true});
}

// [VXG RealForever]
