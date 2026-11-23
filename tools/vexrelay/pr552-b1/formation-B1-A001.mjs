import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import crypto from 'node:crypto';
import { createRequire } from 'node:module';

const [sdkRootArg,outputDirectoryArg,payloadSourceArg]=process.argv.slice(2);
if(!sdkRootArg||!outputDirectoryArg||!payloadSourceArg){
  throw new Error('usage: node formation-B1-A001.mjs <sdk-root> <output-directory> <payload-source>');
}
const sdkRoot=path.resolve(sdkRootArg);
const outputDirectory=path.resolve(outputDirectoryArg);
const payloadSource=path.resolve(payloadSourceArg);
const require=createRequire(import.meta.url);
const packageBuilder=require(path.join(sdkRoot,'lib/vexlocalbridge/package-builder.js'));
const policy=require(path.join(sdkRoot,'lib/vexlocalbridge/vexlife-prod-host-effect-policy.js'));
const {bindAuthoritySummaryFromEnvelope,createCanonicalTaskZip,hashFile,writeJson}=packageBuilder;

const BASE='f1855e9be6b9172ba6b43b597fa5d902dc409ed1';
const HEAD='4db72d0ae800f73560c75aa8a33cb75cfec65d90';
const TREE='11b2cfa2566a86f2694da79402f69b982e17ecde';
const BRANCH='VXG-112226-dcp-b1-foundation-v0620-a001';
const SDK_HEAD='149455eeec4c7b7ca00e91022154a85faa3d608c';
const SDK_TREE='53ea701f9bbf24a5cf37d43e4dbe3836187f4011';
const PROFILE_PATH=path.join(sdkRoot,'docs/private-continuity/vexlocalbridge/profiles/macos-victor-vexlife-prod.json');
const profile=JSON.parse(fs.readFileSync(PROFILE_PATH,'utf8'));
policy.validateProfileBinding(profile);
if(!fs.existsSync(payloadSource)||!fs.statSync(payloadSource).isFile()){
  throw new Error('PR552_DCP_B1_PAYLOAD_SOURCE_MISSING');
}

const suffix=crypto.randomBytes(10).toString('hex');
const workRoot=fs.mkdtempSync(path.join(os.tmpdir(),'pr552-dcp-b1-formation-'));
try{
  const contentRoot=path.join(workRoot,'content');
  for(const dir of ['authority','payload','inputs']) fs.mkdirSync(path.join(contentRoot,dir),{recursive:true});

  const authorityEnvelope={
    authoritySchemaRef:'vextreme.effect-transaction/v1',
    semanticAdmissionRef:'github.pull.vexlife-prod.552',
    transactionRef:'transaction.vexlife-prod.pr552.dcp-b1-foundation-v0620.a001.'+suffix,
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
      'effect.vexlife-prod.foundation-mutation',
      'effect.vexlife-prod.evolve-context-semantic-acceptance',
      'effect.vexlife-prod.dream',
      'effect.vexlife-prod.training',
      'effect.vexlife-prod.publication',
      'effect.vexlife-prod.ready',
      'effect.vexlife-prod.merge',
    ],
  };

  const binding={
    schemaVersion:'vexlife-prod.pr552-dcp-b1-foundation-v0620-binding/v1',
    continuity:'[VXG RealForever]',
    purpose:'Run the first full Sarah/Vex changed-plan encounter using the reusable character + role + scenario + binding architecture after S0 and S1 prerequisites passed.',
    candidate:{
      repository:'VexGPT/VexLife-Prod',
      base:BASE,
      head:HEAD,
      tree:TREE,
      branch:BRANCH,
    },
    profile:{
      characterRef:'character.vexlife.dynamic-counterpart.sarah.v1',
      roleRef:'role.dynamic-counterpart.ordinary-shared-understanding.v1',
      scenarioRef:'scenario.vexlife.evolve-context.changed-plan.v1',
      bindingRef:'binding.dynamic-counterpart.sarah.changed-plan.ordinary-shared-understanding.v1',
      observerLensSupplied:false,
    },
    prerequisites:{
      s0ReturnSha256:'097c0953d3ebf9c761486d3b475791d93e5db550944b8cf1f0001162f9e58a1e',
      s1ReturnSha256:'ebd4125e99a8643043170f5b3838df9f7b8adc4098912eaedc2156ade58363b7',
    },
    carrier:{
      sdkRepository:'vgong24/Vextreme-SDK',
      sdkHead:SDK_HEAD,
      sdkTree:SDK_TREE,
      profileRef:profile.profileRef,
      policyRef:profile.vexLifeProdHostEffectPolicy.policyRef,
    },
    acceptance:{
      exactSelectedModelRef:'model.vexlife-prod.heretic.qwen3.8-4b-distill.mlx4.d2f8f4028f20',
      modelMustAlreadyBeSelected:true,
      modelAcquisitionAllowed:false,
      modelSwitchAllowed:false,
      focusedSourceGateRequired:true,
      fullJvmSourceGateRequired:true,
      canonicalConversationMutationAllowed:false,
      canonicalMemoryMutationAllowed:false,
      observerLensSuppliedToEitherModel:false,
      semanticJudgment:'GODWARD_HUMAN_REVIEW_REQUIRED',
      memoryAutoPromotionAllowed:false,
      dreamAutoRunAllowed:false,
      trainingAutoRunAllowed:false,
    },
  };

  const authorityPath=path.join(contentRoot,'authority/effect.json');
  const payloadPath=path.join(contentRoot,'payload/encounter.mjs');
  const bindingPath=path.join(contentRoot,'inputs/encounter-binding.json');
  writeJson(authorityPath,authorityEnvelope);
  fs.copyFileSync(payloadSource,payloadPath);
  writeJson(bindingPath,binding);

  const taskTemplate={
    schemaVersion:'vextreme.vexlocalbridge-task-manifest/v1',
    artifactClass:'EXECUTABLE_RELAY',
    taskRef:'task.vexlife-prod.pr552.dcp-b1-foundation-v0620.a001',
    attemptRef:'attempt.vexlife-prod.pr552.dcp-b1-foundation-v0620.a001.'+suffix,
    packageRef:'package.vexlife-prod.pr552.dcp-b1-foundation-v0620.a001.'+suffix,
    requestRef:'request.vexlife-prod.pr552.dcp-b1-foundation-v0620.a001.'+suffix,
    correlationRef:'correlation.vexlife-prod.pr552.dcp-b1-foundation-v0620.a001.'+suffix,
    taskStorageKey:'vexlife-prod-pr552-dcp-b1-foundation-v0620',
    attemptStorageKey:'attempt-a001-'+suffix,
    requestingRoleRef:'Operations[PR552-DCP-B1]',
    requestingInstanceRef:'instance.chatgpt.pr552-dcp-b1-foundation-v0620.a001.'+suffix,
    parentOperationsRef:'github.pull.vexlife-prod.552',
    returnRouteRef:'github.pull.vexlife-prod.552',
    hostProfileRef:'macos-victor',
    repositoryExecutionProfileRefOrNull:'profile.repository.vexlife-prod.macos-v1',
    repositoryRefOrNull:'VexGPT/VexLife-Prod',
    baseRefOrNull:BASE,
    headRefOrNull:HEAD,
    treeRefOrNull:TREE,
    branchRefOrNull:BRANCH,
    claimRefOrNull:'github.pull.vexlife-prod.552',
    exactPathRefs:[],
    authorityBinding:{
      authoritySchemaRef:authorityEnvelope.authoritySchemaRef,
      authorityEnvelopePath:'authority/effect.json',
      authorityEnvelopeSha256:hashFile(authorityPath),
      semanticAdmissionRef:'stale',
      transactionRef:'stale',
      transactionVersion:1,
      allowedEffectRefs:['stale'],
      heldEffectRefs:[],
    },
    payload:{
      payloadClass:'NODE_SCRIPT',
      payloadRuntime:'node',
      payloadPath:'payload/encounter.mjs',
      payloadSha256:hashFile(payloadPath),
      typedArgv:[],
      immutableInputs:[{path:'inputs/encounter-binding.json',sha256:hashFile(bindingPath)}],
    },
    executionStateAtFormation:'TASK_PREPARED',
    singleUseNonce:'nonce-pr552-dcp-b1-foundation-v0620-a001-'+suffix+'-'+crypto.randomBytes(16).toString('hex'),
    stopConditions:[
      'STOP_ON_EXACT_SOURCE_DRIFT',
      'STOP_ON_PRODUCT_HOST_EFFECT_POLICY_DRIFT',
      'STOP_ON_SOURCE_GATE_FAILURE',
      'STOP_ON_CANONICAL_WRITER_NON_IDLE',
      'STOP_ON_MODEL_NOT_ALREADY_SELECTED_4B',
      'STOP_ON_MODEL_ACQUISITION_OR_SWITCH_REQUIREMENT',
      'STOP_ON_CANONICAL_CONVERSATION_OR_MEMORY_MUTATION',
      'STOP_ON_PROFILE_ENCOUNTER_MECHANICAL_HOLD_OR_FAILURE',
    ],
    archiveDigestBinding:'FILENAME_SHA256',
  };

  const formed=createCanonicalTaskZip({
    taskTemplate:bindAuthoritySummaryFromEnvelope(taskTemplate,authorityEnvelope),
    contentRoot,
    outputDirectory,
    outputStem:'VEXLIFE-PR552-DCP-B1-FOUNDATION-V0620-A001',
    ttlMs:3*60*60*1000,
  });

  const admitted=policy.admitTask({profile,task:formed.task,authorityEnvelope});
  if(admitted.candidate.base!==BASE||
     admitted.candidate.head!==HEAD||
     admitted.candidate.tree!==TREE||
     admitted.buildDependencyFetchAuthorized!==true||
     admitted.productDataPolicy.conversationMutationAllowed!==false||
     admitted.productDataPolicy.memoryMutationAllowed!==false||
     admitted.productDataPolicy.modelAcquisitionAllowed!==false||
     admitted.productDataPolicy.modelSwitchForProofAllowed!==false||
     admitted.productDataPolicy.runtimeLifecycleMutationAllowed!==true){
    throw new Error('PR552_DCP_B1_PRODUCT_HOST_EFFECT_ADMISSION_MISMATCH');
  }

  process.stdout.write(JSON.stringify({
    disposition:'PASS',
    packagePath:formed.finalZip,
    packageSha256:formed.digest,
    taskRef:formed.task.taskRef,
    attemptRef:formed.task.attemptRef,
    candidate:{base:BASE,head:HEAD,tree:TREE,branch:BRANCH},
    profile:binding.profile,
    prerequisites:binding.prerequisites,
    carrier:{sdkHead:SDK_HEAD,sdkTree:SDK_TREE,profileRef:profile.profileRef},
  })+'\n');
}finally{
  fs.rmSync(workRoot,{recursive:true,force:true});
}
