#!/usr/bin/env node
// [VXG RealForever]
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import crypto from 'node:crypto';
import { createRequire } from 'node:module';
import { spawnSync } from 'node:child_process';

const [sdkRoot,payloadSource,outputDirectory]=process.argv.slice(2);
if(!sdkRoot||!payloadSource||!outputDirectory) throw new Error('USAGE: FORM-AND-RUN-A007-LIVED-RUNTIME-RELEASE.mjs <sdkRoot> <payloadSource> <outputDirectory>');
const contract=JSON.parse(fs.readFileSync(path.join(sdkRoot,'docs/private-continuity/vexrelay/current-contract.json'),'utf8'));
if(contract.contractRef!=='source.vexrelay.current-contract/v1') throw new Error('CURRENT_CONTRACT_REF_MISMATCH');
if(contract.currentRuntimeVersion!=='v0.2.0-RC1') throw new Error('CURRENT_RUNTIME_VERSION_MISMATCH');
const direct=contract.acceptedAdapters.find(x=>x.adapterRef==='adapter.vexlocalbridge.direct-host-effect');
if(!direct||direct.state!=='TERMINAL_ACCEPTED_CURRENT'||direct.effectClass!=='HOST_SEMANTIC_MODEL_RUNTIME') throw new Error('DIRECT_HOST_EFFECT_NOT_CURRENT');

const require=createRequire(import.meta.url);
const builder=require(path.join(sdkRoot,'lib/vexlocalbridge/package-builder.js'));
const suffix=new Date().toISOString().replace(/[-:.TZ]/g,'')+'-'+crypto.randomBytes(6).toString('hex');
const formationRoot=fs.mkdtempSync(path.join(outputDirectory,'.vexlife-a007-lived-runtime-release-formation-'));
try {
  const contentRoot=path.join(formationRoot,'content');
  fs.mkdirSync(path.join(contentRoot,'authority'),{recursive:true});
  fs.mkdirSync(path.join(contentRoot,'payload'),{recursive:true});
  const authority={
    authoritySchemaRef:'vextreme.effect-transaction/v1',
    semanticAdmissionRef:'authority.victor.vexlife-lifecycle-convergence.20261121',
    transactionRef:'transaction.vexlife-prod.a007.lived-runtime-release.'+suffix,
    transactionVersion:1,
    allowedEffectRefs:[
      'effect.vexlife-prod.a007.observe-current-service-runtime',
      'effect.vexlife-prod.a007.retire-exact-current-product-service',
      'effect.vexlife-prod.a007.verify-runtime-release'
    ],
    heldEffectRefs:[
      'effect.vexlife-prod.home-mutation',
      'effect.vexlife-prod.model-selection-mutation',
      'effect.vexlife-prod.model-artifact-mutation',
      'effect.vexlife-prod.repository-mutation',
      'effect.vexlife-prod.publication',
      'effect.vexlife-prod.ready',
      'effect.vexlife-prod.merge'
    ]
  };
  const authorityPath=path.join(contentRoot,'authority/effect.json');
  fs.writeFileSync(authorityPath,JSON.stringify(authority,null,2)+'\n');
  const payloadPath=path.join(contentRoot,'payload/a007-lived-runtime-release.mjs');
  fs.copyFileSync(payloadSource,payloadPath);
  const sha=file=>crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex');
  const authoritySha=sha(authorityPath);
  const payloadSha=sha(payloadPath);
  const taskTemplate={
    schemaVersion:'vextreme.vexlocalbridge-task-manifest/v1',
    artifactClass:'EXECUTABLE_RELAY',
    taskRef:'task.vexlife-prod.a007.lived-runtime-release',
    attemptRef:'attempt.vexlife-prod.a007.lived-runtime-release.'+suffix,
    packageRef:'package.vexlife-prod.a007.lived-runtime-release.'+suffix,
    requestRef:'request.vexlife-prod.a007.lived-runtime-release.'+suffix,
    correlationRef:'correlation.vexlife-prod.a007.lived-runtime-release.'+suffix,
    taskStorageKey:'vexlife-prod-a007-lived-runtime-release',
    attemptStorageKey:'attempt-'+suffix,
    requestingRoleRef:'Operations[VexLife-Lifecycle]',
    requestingInstanceRef:'instance.operations.vexlife-prod.a007.'+suffix,
    parentOperationsRef:'github.pull.vexlife-prod.391',
    returnRouteRef:'route.vexlife-prod.a007.lived-runtime-release',
    hostProfileRef:'macos-victor',
    repositoryExecutionProfileRefOrNull:'profile.repository.vexlife.macos-v1',
    repositoryRefOrNull:null,
    baseRefOrNull:null,
    headRefOrNull:null,
    treeRefOrNull:null,
    branchRefOrNull:null,
    claimRefOrNull:null,
    exactPathRefs:[],
    authorityBinding:{
      ...authority,
      authorityEnvelopePath:'authority/effect.json',
      authorityEnvelopeSha256:authoritySha
    },
    payload:{
      payloadClass:'VEXLIFE_A007_LIVED_RUNTIME_RELEASE',
      payloadRuntime:'node',
      payloadPath:'payload/a007-lived-runtime-release.mjs',
      payloadSha256:payloadSha,
      typedArgv:[],
      immutableInputs:[]
    },
    executionStateAtFormation:'TASK_PREPARED',
    singleUseNonce:'nonce-a007-lived-runtime-release-'+suffix,
    stopConditions:[
      'stop.on.a007.service.identity.mismatch',
      'stop.on.runtime.receipt.identity.mismatch',
      'stop.on.exact.service.retirement.failure',
      'stop.on.runtime.release.failure'
    ],
    archiveDigestBinding:'FILENAME_SHA256'
  };
  const formed=builder.createCanonicalTaskZip({
    taskTemplate,
    contentRoot,
    outputDirectory,
    outputStem:'VEXLIFE-A007-LIVED-RUNTIME-RELEASE',
    ttlMs:2*60*60*1000
  });
  console.log('VEXLIFE_A007_LIVED_TASK_ZIP='+formed.finalZip);
  console.log('VEXLIFE_A007_LIVED_TASK_SHA256='+formed.digest);
  const launcher=path.join(sdkRoot,'lib/vexlocalbridge/templates/RUN-VEXLIFE-TASK.command');
  const run=spawnSync('/bin/bash',[launcher,formed.finalZip],{stdio:'inherit',env:process.env});
  if(run.error) throw run.error;
  process.exit(run.status??1);
} finally {
  fs.rmSync(formationRoot,{recursive:true,force:true});
}
