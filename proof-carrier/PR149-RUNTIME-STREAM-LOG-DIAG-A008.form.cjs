#!/usr/bin/env node
'use strict';

const fs=require('node:fs');
const os=require('node:os');
const path=require('node:path');

const [sdkRootArg,payloadPathArg]=process.argv.slice(2);
if(!sdkRootArg||!payloadPathArg) throw new Error('usage: PR149-RUNTIME-STREAM-LOG-DIAG-A008.form.cjs <sdk-root> <payload>');
const sdkRoot=path.resolve(sdkRootArg);
const payloadSource=path.resolve(payloadPathArg);
const {createCanonicalTaskZip,writeJson,hashFile,bindAuthoritySummaryFromEnvelope}=
  require(path.join(sdkRoot,'lib/vexlocalbridge/package-builder.js'));

const authorityEnvelope={
  authoritySchemaRef:'vextreme.effect-transaction/v1',
  semanticAdmissionRef:'github.pull.vexlife-prod.149',
  transactionRef:'transaction.vexlife-prod.pr149.runtime-stream-log-diagnostic.a008',
  transactionVersion:1,
  allowedEffectRefs:['effect.vexlife-prod.runtime-log.read'],
  heldEffectRefs:[
    'effect.vexlife-prod.source-mutation',
    'effect.vexlife-prod.home-mutation',
    'effect.vexlife-prod.runtime-start',
    'effect.vexlife-prod.runtime-retire',
    'effect.vexlife-prod.runtime-recovery',
    'effect.vexlife-prod.model-inference',
    'effect.vexlife-prod.browser-navigation',
    'effect.vexlife-prod.browser-refresh',
    'effect.vexlife-prod.pr-metadata-mutation',
    'effect.vexlife-prod.ready',
    'effect.vexlife-prod.merge'
  ]
};

const root=fs.mkdtempSync(path.join(os.tmpdir(),'vexlife-pr149-runtime-stream-log-a008-form-'));
try{
  const authorityRelativePath='authority/effect.json';
  const payloadRelativePath='payload/run.cjs';
  const authorityPath=path.join(root,authorityRelativePath);
  const payloadPath=path.join(root,payloadRelativePath);
  fs.mkdirSync(path.dirname(authorityPath),{recursive:true});
  fs.mkdirSync(path.dirname(payloadPath),{recursive:true});
  writeJson(authorityPath,authorityEnvelope);
  fs.copyFileSync(payloadSource,payloadPath);

  const task=bindAuthoritySummaryFromEnvelope({
    schemaVersion:'vextreme.vexlocalbridge-task-manifest/v1',
    artifactClass:'EXECUTABLE_RELAY',
    taskRef:'task.vexlife-prod.pr149.runtime-stream-log-diagnostic.008',
    attemptRef:'attempt.vexlife-prod.pr149.runtime-stream-log-diagnostic.a008',
    packageRef:'package.vexlife-prod.pr149.runtime-stream-log-diagnostic.a008',
    requestRef:'request.vexlife-prod.pr149.runtime-stream-log-diagnostic.a008',
    correlationRef:'correlation.vexlife-prod.pr149.runtime-stream-log-diagnostic.a008',
    taskStorageKey:'vexlife-prod-pr149-runtime-stream-log-diagnostic',
    attemptStorageKey:'attempt-a008',
    requestingRoleRef:'Operations[VexLife-Prod]',
    requestingInstanceRef:'instance.operations.vexlife-prod.pr149.runtime-stream-log-diagnostic.a008',
    parentOperationsRef:'github.pull.vexlife-prod.149',
    returnRouteRef:'github.pull.vexlife-prod.149',
    hostProfileRef:'macos-victor',
    repositoryExecutionProfileRefOrNull:'profile.repository.vextreme-sdk.macos-v1',
    repositoryRefOrNull:null,baseRefOrNull:null,headRefOrNull:null,treeRefOrNull:null,branchRefOrNull:null,
    claimRefOrNull:'github.pull.vexlife-prod.149',
    exactPathRefs:[],
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
      payloadClass:'VEXLIFE_PROD_RUNTIME_STREAM_LOG_DIAGNOSTIC',
      payloadRuntime:'node',
      payloadPath:payloadRelativePath,
      payloadSha256:hashFile(payloadPath),
      typedArgv:[],
      immutableInputs:[]
    },
    executionStateAtFormation:'TASK_PREPARED',
    singleUseNonce:'nonce-vexlife-prod-pr149-runtime-stream-log-diagnostic-a008-20261115',
    stopConditions:['stop.on.read-only-runtime-log-diagnostic-failure','stop.on.bound-expiry'],
    archiveDigestBinding:'FILENAME_SHA256'
  },authorityEnvelope);

  const formed=createCanonicalTaskZip({
    taskTemplate:task,
    contentRoot:root,
    outputDirectory:path.join(os.homedir(),'Downloads'),
    outputStem:'VEXLIFE-PROD-PR149-RUNTIME-STREAM-LOG-DIAG-A008',
    ttlMs:4*60*60*1000
  });
  process.stdout.write(JSON.stringify({
    state:'PASS',
    package:formed.finalZip,
    sha256:formed.digest,
    taskRef:formed.task.taskRef,
    attemptRef:formed.task.attemptRef
  },null,2)+'\n');
}finally{
  fs.rmSync(root,{recursive:true,force:true});
}
