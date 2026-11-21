'use strict';

const crypto = require('node:crypto');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const cp = require('node:child_process');

function sha256File(file) {
  return crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex');
}
function need(value,label) {
  if (!value) throw new Error(label + ' is required');
  return path.resolve(value);
}
function run(cwd,args) {
  const r=cp.spawnSync('/usr/bin/git',['-C',cwd,...args],{encoding:'utf8',shell:false});
  if (r.status !== 0) throw new Error('git ' + args.join(' ') + ': ' + (r.stderr || r.stdout));
  return r.stdout.trim();
}

const sdkRoot=need(process.argv[2],'sdkRoot');
const contextRoot=need(process.argv[3],'contextRoot');
const outputDirectory=need(process.argv[4],'outputDirectory');
const expectedCompanionHead=process.argv[5];
if (!/^[0-9a-f]{40}$/.test(expectedCompanionHead || '')) throw new Error('expectedCompanionHead must be exact');

const repoRoot=path.resolve(contextRoot,'..','..','..');
const observedHead=run(repoRoot,['rev-parse','HEAD']);
const observedTree=run(repoRoot,['rev-parse','HEAD^{tree}']);
if (observedHead !== expectedCompanionHead) throw new Error('companion checkout head mismatch');

const builder=require(path.join(sdkRoot,'lib/vexlocalbridge/package-builder.js'));
const attempt=JSON.parse(fs.readFileSync(path.join(contextRoot,'CURRENT-ATTEMPT.json'),'utf8'));
const binding=JSON.parse(fs.readFileSync(path.join(contextRoot,'experiment-binding.json'),'utf8'));
const authority=JSON.parse(fs.readFileSync(path.join(contextRoot,'authority/current.json'),'utf8'));

const temp=fs.mkdtempSync(path.join(os.tmpdir(),'memory-depth-companion-task-'));
try {
  const content=path.join(temp,'content');
  fs.mkdirSync(path.join(content,'authority'),{recursive:true});
  fs.mkdirSync(path.join(content,'inputs'),{recursive:true});
  fs.mkdirSync(path.join(content,'payload'),{recursive:true});

  const authorityPath=path.join(content,'authority/effect.json');
  const attemptPath=path.join(content,'inputs/CURRENT-ATTEMPT.json');
  const bindingPath=path.join(content,'inputs/experiment-binding.json');
  const payloadPath=path.join(content,attempt.task.payloadPackagePath);

  fs.copyFileSync(path.join(contextRoot,'authority/current.json'),authorityPath);
  fs.copyFileSync(path.join(contextRoot,'CURRENT-ATTEMPT.json'),attemptPath);
  fs.copyFileSync(path.join(contextRoot,'experiment-binding.json'),bindingPath);
  fs.copyFileSync(path.join(contextRoot,attempt.task.payloadSourcePath),payloadPath);

  const taskTemplate={
    schemaVersion:'vextreme.vexlocalbridge-task-manifest/v1',
    artifactClass:'EXECUTABLE_RELAY',
    taskRef:'task.vexlife-prod.memory-depth.provisional-recontact.companion',
    attemptRef:'attempt.vexlife-prod.memory-depth.provisional-recontact.companion.' + attempt.attemptRef.toLowerCase().replace(/[^a-z0-9._-]+/g,'-') + '.pr472',
    packageRef:'package.vexlife-prod.memory-depth.provisional-recontact.companion.' + attempt.attemptRef.toLowerCase().replace(/[^a-z0-9._-]+/g,'-') + '.pr472',
    requestRef:'request.vexlife-prod.memory-depth.provisional-recontact.companion.' + attempt.attemptRef.toLowerCase().replace(/[^a-z0-9._-]+/g,'-') + '.pr472',
    correlationRef:'correlation.vexlife-prod.memory-depth.provisional-recontact.companion.' + attempt.attemptRef.toLowerCase().replace(/[^a-z0-9._-]+/g,'-') + '.pr472',
    taskStorageKey:'vexlife-prod-memory-depth-provisional-recontact-companion',
    attemptStorageKey:attempt.task.attemptStorageKey,
    requestingRoleRef:'MemoryDepthCompanion[ChatGPT]',
    requestingInstanceRef:'instance.chatgpt.memory-depth-companion.20261121.' + attempt.attemptRef.toLowerCase().replace(/[^a-z0-9._-]+/g,'-'),
    parentOperationsRef:'github.pull.vexlife-prod.472',
    returnRouteRef:'github.pull.vexlife-prod.472',
    hostProfileRef:'macos-victor',
    repositoryExecutionProfileRefOrNull:'profile.repository.vextreme-sdk.macos-v1',
    repositoryRefOrNull:'VexGPT/VexLife-Prod',
    baseRefOrNull:binding.experiment.baseHead,
    headRefOrNull:observedHead,
    treeRefOrNull:observedTree,
    branchRefOrNull:'VXG-112126-chatgpt-memory-depth-provisional-recontact-companion-a001',
    claimRefOrNull:'work.vexlife-prod.memory-depth.provisional-recontact.companion.20261121',
    exactPathRefs:[
      'relay/memory-depth/provisional-recontact/CURRENT-ATTEMPT.json',
      'relay/memory-depth/provisional-recontact/experiment-binding.json',
      'relay/memory-depth/provisional-recontact/authority/current.json',
      'relay/memory-depth/provisional-recontact/' + attempt.task.payloadSourcePath
    ],
    authorityBinding:{
      authoritySchemaRef:authority.authoritySchemaRef,
      authorityEnvelopePath:'authority/effect.json',
      authorityEnvelopeSha256:sha256File(authorityPath),
      semanticAdmissionRef:authority.semanticAdmissionRef,
      transactionRef:authority.transactionRef,
      transactionVersion:authority.transactionVersion,
      allowedEffectRefs:authority.allowedEffectRefs,
      heldEffectRefs:authority.heldEffectRefs,
    },
    payload:{
      payloadClass:attempt.task.payloadClass,
      payloadRuntime:'node',
      payloadPath:attempt.task.payloadPackagePath,
      payloadSha256:sha256File(payloadPath),
      typedArgv:[],
      immutableInputs:[
        {path:'inputs/CURRENT-ATTEMPT.json',sha256:sha256File(attemptPath)},
        {path:'inputs/experiment-binding.json',sha256:sha256File(bindingPath)},
      ],
    },
    executionStateAtFormation:'TASK_PREPARED',
    singleUseNonce:attempt.task.singleUseNonce,
    stopConditions:[
      'stop.on.companion-head-drift',
      'stop.on.github-currentness-unavailable',
      'stop.on.experiment-source-drift',
      'stop.on.live-product-source-move',
      'stop.on.runtime-currentness-hold',
      'stop.on.unadmitted-host-effect'
    ],
    archiveDigestBinding:'FILENAME_SHA256',
  };

  const formed=builder.createCanonicalTaskZip({
    taskTemplate,
    contentRoot:content,
    outputDirectory,
    outputStem:attempt.task.outputStem,
    ttlMs:2*60*60*1000,
  });
  process.stdout.write(JSON.stringify({
    finalZip:formed.finalZip,
    digest:formed.digest,
    taskRef:formed.task.taskRef,
    attemptRef:formed.task.attemptRef,
    formedAt:formed.task.formedAt,
    expiresAt:formed.task.expiresAt,
    companionHead:observedHead,
    companionTree:observedTree,
    mode:attempt.mode,
    formationEvidence:formed.formationEvidence,
  })+'\n');
} finally {
  fs.rmSync(temp,{recursive:true,force:true});
}
