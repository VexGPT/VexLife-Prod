#!/usr/bin/env node
'use strict';

const fs=require('node:fs');
const os=require('node:os');
const path=require('node:path');

const [sdkRootArg,payloadPathArg]=process.argv.slice(2);
if(!sdkRootArg||!payloadPathArg){
  throw new Error('usage: PR317-LIVED-RETENTION-INSPECTION-A001.form.cjs <sdk-root> <payload>');
}
const sdkRoot=path.resolve(sdkRootArg);
const payloadSource=path.resolve(payloadPathArg);
const {
  createCanonicalTaskZip,writeJson,hashFile,bindAuthoritySummaryFromEnvelope
}=require(path.join(sdkRoot,'lib/vexlocalbridge/package-builder.js'));

const binding={
  schemaVersion:'vexlife-prod.pr317-lived-retention-inspection/v1',
  continuity:'[VXG RealForever]',
  repository:'VexGPT/VexLife-Prod',
  currentSourceSha:'9a007983e2f664198f30663b25bb14eb083d7d46',
  expectedRetiredSourceShas:[
    '0b1acccfdccfe787a38eaccf202a33fcfb198bcf',
    '4a65fe6771dae8553974969b321472bcc2917bd5',
    '7345cc2e366e918a9298f0a03c96950c9bae4ac0',
    'c3576a962e7a93bedab7addc2ae20d3b441b881b',
  ],
};

const authorityEnvelope={
  authoritySchemaRef:'vextreme.effect-transaction/v1',
  semanticAdmissionRef:'github.pull.vexlife-prod.317',
  transactionRef:'transaction.vexlife-prod.pr317.lived-retention-inspection.a001',
  transactionVersion:1,
  allowedEffectRefs:[
    'effect.vexlife-prod.pr317.host-retention-inspection',
    'effect.vexlife-prod.pr317.loopback-currentness-inspection',
    'effect.vexlife-prod.pr317.inspection-evidence-retention',
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
    'effect.vexlife-prod.service-retirement',
    'effect.vexlife-prod.service-start',
    'effect.vexlife-prod.private-image-deletion',
  ],
};

const root=fs.mkdtempSync(path.join(os.tmpdir(),'vexlife-pr317-lived-retention-inspection-form-'));
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
    taskRef:'task.vexlife-prod.pr317.lived-retention-inspection.001',
    attemptRef:'attempt.vexlife-prod.pr317.lived-retention-inspection.a001',
    packageRef:'package.vexlife-prod.pr317.lived-retention-inspection.a001',
    requestRef:'request.vexlife-prod.pr317.lived-retention-inspection.a001',
    correlationRef:'correlation.vexlife-prod.pr317.lived-retention-inspection.a001',
    taskStorageKey:'vexlife-prod-pr317-lived-retention-inspection',
    attemptStorageKey:'attempt-a001',
    requestingRoleRef:'Operations[VexLife-Prod]',
    requestingInstanceRef:'instance.chatgpt.vexlife-prod.pr317.lived-retention-inspection.a001',
    parentOperationsRef:'github.pull.vexlife-prod.317',
    returnRouteRef:'github.pull.vexlife-prod.317',
    hostProfileRef:'macos-victor',
    repositoryExecutionProfileRefOrNull:'profile.repository.vextreme-sdk.macos-v1',
    repositoryRefOrNull:null,
    baseRefOrNull:null,
    headRefOrNull:null,
    treeRefOrNull:null,
    branchRefOrNull:null,
    claimRefOrNull:'github.pull.vexlife-prod.317',
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
      payloadClass:'VEXLIFE_PROD_PR317_LIVED_RETENTION_INSPECTION',
      payloadRuntime:'node',
      payloadPath:'payload/run.cjs',
      payloadSha256:hashFile(payloadPath),
      typedArgv:['inputs/binding.json'],
      immutableInputs:[{path:'inputs/binding.json',sha256:hashFile(bindingPath)}],
    },
    executionStateAtFormation:'TASK_PREPARED',
    singleUseNonce:'nonce-vexlife-prod-pr317-lived-retention-inspection-a001',
    stopConditions:[
      'stop.on.host-inspection-harness-failure',
      'stop.on.bound-expiry',
    ],
    archiveDigestBinding:'FILENAME_SHA256',
  },authorityEnvelope);

  const formed=createCanonicalTaskZip({
    taskTemplate:task,
    contentRoot:root,
    outputDirectory:path.join(os.homedir(),'Downloads'),
    outputStem:'VEXLIFE-PR317-LIVED-RETENTION-INSPECTION-A001',
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
