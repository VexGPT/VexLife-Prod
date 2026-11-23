import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import crypto from 'node:crypto';
import { createRequire } from 'node:module';

const [sdkRootArg,outputDirectoryArg,payloadSourceArg]=process.argv.slice(2);
if(!sdkRootArg||!outputDirectoryArg||!payloadSourceArg){
  throw new Error(
    'usage: node formation-A001.mjs <sdk-root> <output-directory> <payload-source>',
  );
}

const sdkRoot=path.resolve(sdkRootArg);
const outputDirectory=path.resolve(outputDirectoryArg);
const payloadSource=path.resolve(payloadSourceArg);
const require=createRequire(import.meta.url);
const packageBuilder=require(path.join(sdkRoot,'lib/vexlocalbridge/package-builder.js'));
const policy=require(path.join(sdkRoot,'lib/vexlocalbridge/vexlife-prod-host-effect-policy.js'));
const {bindAuthoritySummaryFromEnvelope,createCanonicalTaskZip,hashFile,writeJson}=packageBuilder;

const BASE='f1855e9be6b9172ba6b43b597fa5d902dc409ed1';
const HEAD='400823f457b936de8924d5bb8e3705872a728297';
const TREE='08717012dfd7654d479d305aee5ca69f6f8d43c4';
const BRANCH='VXG-112226-evolve-context-dynamic-counterpart-dcp1-a001';

const SDK_HEAD='149455eeec4c7b7ca00e91022154a85faa3d608c';
const SDK_TREE='53ea701f9bbf24a5cf37d43e4dbe3836187f4011';
const PROFILE_PATH=path.join(sdkRoot,'docs/private-continuity/vexlocalbridge/profiles/macos-victor-vexlife-prod.json');
const profile=JSON.parse(fs.readFileSync(PROFILE_PATH,'utf8'));
policy.validateProfileBinding(profile);

if(!fs.existsSync(payloadSource)||!fs.statSync(payloadSource).isFile()){
  throw new Error('MULTI_VEX_STEERING_PAYLOAD_MISSING');
}

const suffix=crypto.randomBytes(10).toString('hex');
const workRoot=fs.mkdtempSync(path.join(os.tmpdir(),'multivex-steering-formation-'));
try{
  const contentRoot=path.join(workRoot,'content');
  for(const dir of ['authority','payload','inputs']) fs.mkdirSync(path.join(contentRoot,dir),{recursive:true});

  const authorityEnvelope={
    authoritySchemaRef:'vextreme.effect-transaction/v1',
    semanticAdmissionRef:'github.pull.vexlife-prod.543',
    transactionRef:'transaction.vexlife-prod.multivex.counterpart-steering-ablation.a001.'+suffix,
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
    schemaVersion:'vexlife-prod.multivex-counterpart-steering-ablation-binding/v1',
    continuity:'[VXG RealForever]',
    key:'[Multi-Vex]',
    purpose:
      'Test whether Character posture and Role profile materially steer the synthetic participant first move beyond constant scenario ground, intention, uncertainty and ordinary human need.',
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
      vexTurnUsed:false,
    },
    design:{
      factors:[
        'CHARACTER_POSTURE_OFF_ON',
        'INTERACTION_ROLE_OFF_ON',
      ],
      constantGround:[
        'participant identity/name standing',
        'relationship history',
        'known scenario facts',
        'scenario current intentions',
        'scenario uncertainties',
        'private facts',
        'ordinary human need',
        'first private scenario event',
        'structured output contract',
        'no required Vex answer',
      ],
      conditions:[
        'GROUND_ONLY',
        'GROUND_PLUS_POSTURE',
        'GROUND_PLUS_ROLE',
        'GROUND_PLUS_POSTURE_PLUS_ROLE',
      ],
      replicatesPerCondition:4,
      sampleCount:16,
      schedule:'COUNTERBALANCED_FOUR_ROUNDS',
      currentFullProfileControl:'GROUND_PLUS_POSTURE_PLUS_ROLE materializes exact current full-profile system bytes',
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
      sampleCount:16,
      semanticJudgment:'HUMAN_GODWARD_REVIEW_REQUIRED',
      automaticSteeringVerdict:false,
    },
  };

  const authorityPath=path.join(contentRoot,'authority/effect.json');
  const payloadPath=path.join(contentRoot,'payload/steering.mjs');
  const bindingPath=path.join(contentRoot,'inputs/steering-binding.json');
  writeJson(authorityPath,authorityEnvelope);
  fs.copyFileSync(payloadSource,payloadPath);
  writeJson(bindingPath,binding);

  const taskTemplate={
    schemaVersion:'vextreme.vexlocalbridge-task-manifest/v1',
    artifactClass:'EXECUTABLE_RELAY',
    taskRef:'task.vexlife-prod.multivex.counterpart-steering-ablation.a001',
    attemptRef:'attempt.vexlife-prod.multivex.counterpart-steering-ablation.a001.'+suffix,
    packageRef:'package.vexlife-prod.multivex.counterpart-steering-ablation.a001.'+suffix,
    requestRef:'request.vexlife-prod.multivex.counterpart-steering-ablation.a001.'+suffix,
    correlationRef:'correlation.vexlife-prod.multivex.counterpart-steering-ablation.a001.'+suffix,
    taskStorageKey:'vexlife-prod-multivex-counterpart-steering-ablation',
    attemptStorageKey:'attempt-a001-'+suffix,
    requestingRoleRef:'Operations[Multi-Vex-Steering-Ablation]',
    requestingInstanceRef:'instance.chatgpt.multivex.counterpart-steering-ablation.a001.'+suffix,
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
      semanticAdmissionRef:'stale',
      transactionRef:'stale',
      transactionVersion:1,
      allowedEffectRefs:['stale'],
      heldEffectRefs:[],
    },
    payload:{
      payloadClass:'NODE_SCRIPT',
      payloadRuntime:'node',
      payloadPath:'payload/steering.mjs',
      payloadSha256:hashFile(payloadPath),
      typedArgv:[],
      immutableInputs:[
        {path:'inputs/steering-binding.json',sha256:hashFile(bindingPath)},
      ],
    },
    executionStateAtFormation:'TASK_PREPARED',
    singleUseNonce:'nonce-multivex-counterpart-steering-a001-'+suffix+'-'+crypto.randomBytes(16).toString('hex'),
    stopConditions:[
      'STOP_ON_EXACT_SOURCE_DRIFT',
      'STOP_ON_PRODUCT_HOST_EFFECT_POLICY_DRIFT',
      'STOP_ON_SOURCE_GATE_FAILURE',
      'STOP_ON_MODEL_NOT_ALREADY_SELECTED_4B',
      'STOP_ON_MODEL_ACQUISITION_OR_SWITCH_REQUIREMENT',
      'STOP_ON_CANONICAL_CONVERSATION_OR_MEMORY_MUTATION',
      'STOP_ON_COUNTERPART_PROCESS_LEAK',
      'STOP_ON_STEERING_SAMPLE_FAILURE',
    ],
    archiveDigestBinding:'FILENAME_SHA256',
  };

  const formed=createCanonicalTaskZip({
    taskTemplate:bindAuthoritySummaryFromEnvelope(taskTemplate,authorityEnvelope),
    contentRoot,
    outputDirectory,
    outputStem:'VEXLIFE-MULTI-VEX-COUNTERPART-STEERING-ABLATION-A001',
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
    throw new Error('MULTI_VEX_STEERING_PRODUCT_HOST_EFFECT_ADMISSION_MISMATCH');
  }

  process.stdout.write(JSON.stringify({
    disposition:'PASS',
    packagePath:formed.finalZip,
    packageSha256:formed.digest,
    taskRef:formed.task.taskRef,
    attemptRef:formed.task.attemptRef,
    candidate:binding.candidate,
    design:binding.design,
    carrier:binding.carrier,
  })+'\n');
}finally{
  fs.rmSync(workRoot,{recursive:true,force:true});
}
