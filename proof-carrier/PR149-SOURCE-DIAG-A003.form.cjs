#!/usr/bin/env node
'use strict';

const fs=require('node:fs');
const os=require('node:os');
const path=require('node:path');

const [sdkRootArg,payloadPathArg]=process.argv.slice(2);
if(!sdkRootArg||!payloadPathArg) throw new Error('usage: PR149-SOURCE-DIAG-A003.form.cjs <sdk-root> <payload>');
const sdkRoot=path.resolve(sdkRootArg);
const payloadSource=path.resolve(payloadPathArg);
const {createCanonicalTaskZip,writeJson,hashFile,bindAuthoritySummaryFromEnvelope}=
  require(path.join(sdkRoot,'lib/vexlocalbridge/package-builder.js'));

const candidate={
  schemaVersion:'vexlife-prod.source-diagnostic-candidate/v1',
  continuity:'[VXG RealForever]',
  repository:'VexGPT/VexLife-Prod',
  pullRequest:149,
  base:'b4699fb85d85a1c5ec1b14db83cf59e4171d74d1',
  head:'1f0a163d3ec0ad75d8107c61a043dc9c7f4bcb45',
  tree:'7ace8f41998385c126d1f0507e572ec5201ce4f9',
  branch:'VXG-111526-pr148-conversation-closure-a001'
};
const authorityEnvelope={
  authoritySchemaRef:'vextreme.effect-transaction/v1',
  semanticAdmissionRef:'github.pull.vexlife-prod.149',
  transactionRef:'transaction.vexlife-prod.pr149.source-diagnostic.a003',
  transactionVersion:1,
  allowedEffectRefs:[
    'effect.vexlife-prod.pr149.source-read',
    'effect.vexlife-prod.pr149.source-build-test'
  ],
  heldEffectRefs:[
    'effect.vexlife-prod.source-mutation',
    'effect.vexlife-prod.home-mutation',
    'effect.vexlife-prod.runtime-start',
    'effect.vexlife-prod.runtime-retire',
    'effect.vexlife-prod.runtime-recovery',
    'effect.vexlife-prod.pr-metadata-mutation',
    'effect.vexlife-prod.ready',
    'effect.vexlife-prod.merge'
  ]
};
const root=fs.mkdtempSync(path.join(os.tmpdir(),'vexlife-pr149-source-diag-a003-form-'));
try{
  const authorityRelativePath='authority/effect.json';
  const payloadRelativePath='payload/run.cjs';
  const candidateRelativePath='inputs/candidate.json';
  const authorityPath=path.join(root,authorityRelativePath);
  const payloadPath=path.join(root,payloadRelativePath);
  const candidatePath=path.join(root,candidateRelativePath);
  fs.mkdirSync(path.dirname(authorityPath),{recursive:true});
  fs.mkdirSync(path.dirname(payloadPath),{recursive:true});
  fs.mkdirSync(path.dirname(candidatePath),{recursive:true});
  writeJson(authorityPath,authorityEnvelope);
  fs.copyFileSync(payloadSource,payloadPath);
  writeJson(candidatePath,candidate);

  const task=bindAuthoritySummaryFromEnvelope({
    schemaVersion:'vextreme.vexlocalbridge-task-manifest/v1',
    artifactClass:'EXECUTABLE_RELAY',
    taskRef:'task.vexlife-prod.pr149.source-diagnostic.002',
    attemptRef:'attempt.vexlife-prod.pr149.source-diagnostic.a003',
    packageRef:'package.vexlife-prod.pr149.source-diagnostic.a003',
    requestRef:'request.vexlife-prod.pr149.source-diagnostic.a003',
    correlationRef:'correlation.vexlife-prod.pr149.source-diagnostic.a003',
    taskStorageKey:'vexlife-prod-pr149-source-diagnostic',
    attemptStorageKey:'attempt-a003',
    requestingRoleRef:'Operations[VexLife-Prod]',
    requestingInstanceRef:'instance.operations.vexlife-prod.pr149.source-diagnostic.a003',
    parentOperationsRef:'github.pull.vexlife-prod.149',
    returnRouteRef:'github.pull.vexlife-prod.149',
    hostProfileRef:'macos-victor',
    repositoryExecutionProfileRefOrNull:'profile.repository.vextreme-sdk.macos-v1',
    repositoryRefOrNull:null,baseRefOrNull:null,headRefOrNull:null,treeRefOrNull:null,branchRefOrNull:null,
    claimRefOrNull:'github.pull.vexlife-prod.149',exactPathRefs:[],
    authorityBinding:{
      authoritySchemaRef:authorityEnvelope.authoritySchemaRef,
      authorityEnvelopePath:authorityRelativePath,
      authorityEnvelopeSha256:hashFile(authorityPath),
      semanticAdmissionRef:authorityEnvelope.semanticAdmissionRef,
      transactionRef:authorityEnvelope.transactionRef,
      transactionVersion:authorityEnvelope.transactionVersion,
      allowedEffectRefs:[...authorityEnvelope.allowedEffectRefs],
      heldEffectRefs:[...authorityEnvelope.heldEffectRefs]
    },
    payload:{
      payloadClass:'VEXLIFE_PROD_PR149_SOURCE_DIAGNOSTIC',
      payloadRuntime:'node',
      payloadPath:payloadRelativePath,
      payloadSha256:hashFile(payloadPath),
      typedArgv:[candidateRelativePath],
      immutableInputs:[{path:candidateRelativePath,sha256:hashFile(candidatePath)}]
    },
    executionStateAtFormation:'TASK_PREPARED',
    singleUseNonce:'nonce-vexlife-prod-pr149-source-diagnostic-a003-20261115',
    stopConditions:['stop.on.source-diagnostic-failure','stop.on.bound-expiry'],
    archiveDigestBinding:'FILENAME_SHA256'
  },authorityEnvelope);
  const formed=createCanonicalTaskZip({
    taskTemplate:task,contentRoot:root,
    outputDirectory:path.join(os.homedir(),'Downloads'),
    outputStem:'VEXLIFE-PROD-PR149-SOURCE-DIAG-A003',
    ttlMs:4*60*60*1000
  });
  process.stdout.write(JSON.stringify({
    state:'PASS',package:formed.finalZip,sha256:formed.digest,
    taskRef:formed.task.taskRef,attemptRef:formed.task.attemptRef
  },null,2)+'\n');
}finally{
  fs.rmSync(root,{recursive:true,force:true});
}
