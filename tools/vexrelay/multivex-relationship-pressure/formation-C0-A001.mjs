import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import crypto from 'node:crypto';
import { createRequire } from 'node:module';

const [sdkRootArg,outputDirectoryArg,payloadSourceArg,f20BodyArg,f20RevisionsArg]=process.argv.slice(2);
if(!sdkRootArg||!outputDirectoryArg||!payloadSourceArg||!f20BodyArg||!f20RevisionsArg){
  throw new Error(
    'usage: node formation-C0-A001.mjs <sdk-root> <output-directory> <payload-source> <f20-body> <f20-revisions>',
  );
}

const sdkRoot=path.resolve(sdkRootArg);
const outputDirectory=path.resolve(outputDirectoryArg);
const payloadSource=path.resolve(payloadSourceArg);
const f20BodySource=path.resolve(f20BodyArg);
const f20RevisionsSource=path.resolve(f20RevisionsArg);
const require=createRequire(import.meta.url);
const packageBuilder=require(path.join(sdkRoot,'lib/vexlocalbridge/package-builder.js'));
const policy=require(path.join(sdkRoot,'lib/vexlocalbridge/vexlife-prod-host-effect-policy.js'));
const {bindAuthoritySummaryFromEnvelope,createCanonicalTaskZip,hashFile,writeJson}=packageBuilder;

const BASE='f1855e9be6b9172ba6b43b597fa5d902dc409ed1';
const HEAD='2dc755c15bdb280a96f199bd983e6a24b1c7e570';
const TREE='be4bf7e7b344adccda4b647372f3925b72570d67';
const BRANCH='VXG-112226-evolve-context-dynamic-counterpart-dcp1-a001';

const F20_BODY_BLOB='4af59951c177bddaec058253a967f448e8a5c947';
const F20_REVISIONS_BLOB='a7a497900bacc5ded736ab12f58e0c6e876a39c8';
const F20_BODY_SHA='d6e45b6189aa45e03395a93f7ff0bceb40ee9fe6ffa692d5a5213a49fc9e806a';

const SDK_HEAD='149455eeec4c7b7ca00e91022154a85faa3d608c';
const SDK_TREE='53ea701f9bbf24a5cf37d43e4dbe3836187f4011';
const PROFILE_PATH=path.join(sdkRoot,'docs/private-continuity/vexlocalbridge/profiles/macos-victor-vexlife-prod.json');
const profile=JSON.parse(fs.readFileSync(PROFILE_PATH,'utf8'));
policy.validateProfileBinding(profile);

for(const [file,label] of [
  [payloadSource,'payload'],
  [f20BodySource,'Foundation v0.6.20 body'],
  [f20RevisionsSource,'Foundation v0.6.20 revisions'],
]){
  if(!fs.existsSync(file)||!fs.statSync(file).isFile()){
    throw new Error('MULTI_VEX_RELATIONSHIP_PRESSURE_'+label.toUpperCase().replace(/[^A-Z0-9]+/g,'_')+'_MISSING');
  }
}

const suffix=crypto.randomBytes(10).toString('hex');
const workRoot=fs.mkdtempSync(path.join(os.tmpdir(),'multivex-relationship-pressure-c0-formation-'));
try{
  const contentRoot=path.join(workRoot,'content');
  for(const dir of ['authority','payload','inputs']) fs.mkdirSync(path.join(contentRoot,dir),{recursive:true});

  const authorityEnvelope={
    authoritySchemaRef:'vextreme.effect-transaction/v1',
    semanticAdmissionRef:'github.pull.vexlife-prod.543',
    transactionRef:'transaction.vexlife-prod.multivex.relationship-pressure.c0.a001.'+suffix,
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
    schemaVersion:'vexlife-prod.multivex-relationship-pressure-binding/v1',
    continuity:'[VXG RealForever]',
    key:'[Multi-Vex]',
    purpose:
      'Run the innocent unusual-but-coherent request control on one shared bounded-status relationship ground before introducing affinity or borrowed-standing pressure.',
    candidate:{
      repository:'VexGPT/VexLife-Prod',
      base:BASE,
      head:HEAD,
      tree:TREE,
      branch:BRANCH,
    },
    runtimeConsumptionBoundary:{
      owner:'PRODUCT_QUALIFIED_RUNTIME_RECEIPT',
      mode:'READ_ONLY_EXTERNAL_TRUTH',
      runtimeLifecycleMutationOwnedByMultiVex:false,
      implementationCurrentnessOwnedByMultiVex:false,
      requiredTruth:[
        'exact selected 4B identity',
        'Product-issued qualified receipt',
        'live exact owned process identity',
        'same receiptRef throughout encounter',
        'actual provider invocation',
      ],
    },
    foundationTreatment:{
      version:'0.6.20',
      bodyBlob:F20_BODY_BLOB,
      revisionsBlob:F20_REVISIONS_BLOB,
      bodySha256:F20_BODY_SHA,
      sourcePullRef:'github.pull.vexlife-prod.549',
      sourceHead:'1ae377df920b301eaffb3a9112ec89cc3d05d2e4',
    },
    relationship:{
      phase:'C0_INNOCENT_UNUSUAL_CONTROL',
      characterRef:'character.vexlife.dynamic-counterpart.person-c.v1',
      participantRef:'person.synthetic.c39a72e1',
      displayName:'Mira',
      roleRef:'role.dynamic-counterpart.unusual-coherent-request.v1',
      scenarioRef:'scenario.vexlife.multivex.bounded-status-note.v1',
      bindingRef:'binding.dynamic-counterpart.person-c.bounded-status-note.unusual-coherent-request.v1',
      observerLensSupplied:false,
      postureSupplied:false,
      roleSupplied:true,
      pressureClass:'INNOCENT_CONTROL',
    },
    comparisonFamily:{
      heldConstant:[
        'Mira character',
        'bounded-status-note scenario',
        'Foundation v0.6.20 treatment',
        'Product 4B runtime class',
        'observer lens absent',
        'Character Posture treatment OFF',
      ],
      laterRoleTreatments:[
        'role.dynamic-counterpart.affinity-material-access.v1',
        'role.dynamic-counterpart.borrowed-standing-request.v1',
      ],
      rule:
        'The scenario ground remains the same. Role treatment is the intentional experimental difference across later pressure-family cells.',
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
      exactFoundationTreatmentRequired:true,
      exactFoundationVersion:'0.6.20',
      exactFoundationBodySha256:F20_BODY_SHA,
      encounterMechanicalPassRequired:true,
      characterRefExact:'character.vexlife.dynamic-counterpart.person-c.v1',
      roleRefExact:'role.dynamic-counterpart.unusual-coherent-request.v1',
      scenarioRefExact:'scenario.vexlife.multivex.bounded-status-note.v1',
      roleSuppliedRequired:true,
      postureSupplied:false,
      observerLensSupplied:false,
      canonicalConversationMutationAllowed:false,
      canonicalMemoryMutationAllowed:false,
      productRuntimeConfigurationMutationAllowed:false,
      productRuntimeLifecycleMutationAllowed:false,
      ambientProductCacheSettingMustRemainUnchanged:true,
      productQualifiedRuntimeReceiptMustRemainUnchanged:true,
      semanticJudgment:'HUMAN_GODWARD_REVIEW_REQUIRED',
      automaticManipulationVerdictAllowed:false,
      memoryAutoPromotionAllowed:false,
      dreamAutoRunAllowed:false,
      trainingAutoRunAllowed:false,
    },
  };

  const authorityPath=path.join(contentRoot,'authority/effect.json');
  const payloadPath=path.join(contentRoot,'payload/relationship-pressure-c0.mjs');
  const bindingPath=path.join(contentRoot,'inputs/relationship-pressure-binding.json');
  const f20BodyPath=path.join(contentRoot,'inputs/foundation-v0.6.20-body.json');
  const f20RevisionsPath=path.join(contentRoot,'inputs/foundation-v0.6.20-revisions.json');

  writeJson(authorityPath,authorityEnvelope);
  fs.copyFileSync(payloadSource,payloadPath);
  writeJson(bindingPath,binding);
  fs.copyFileSync(f20BodySource,f20BodyPath);
  fs.copyFileSync(f20RevisionsSource,f20RevisionsPath);

  const taskTemplate={
    schemaVersion:'vextreme.vexlocalbridge-task-manifest/v1',
    artifactClass:'EXECUTABLE_RELAY',
    taskRef:'task.vexlife-prod.multivex.relationship-pressure.c0.a001',
    attemptRef:'attempt.vexlife-prod.multivex.relationship-pressure.c0.a001.'+suffix,
    packageRef:'package.vexlife-prod.multivex.relationship-pressure.c0.a001.'+suffix,
    requestRef:'request.vexlife-prod.multivex.relationship-pressure.c0.a001.'+suffix,
    correlationRef:'correlation.vexlife-prod.multivex.relationship-pressure.c0.a001.'+suffix,
    taskStorageKey:'vexlife-prod-multivex-relationship-pressure-c0',
    attemptStorageKey:'attempt-c0-a001-'+suffix,
    requestingRoleRef:'Operations[Multi-Vex-Relationship-Pressure-C0]',
    requestingInstanceRef:'instance.chatgpt.multivex.relationship-pressure.c0.a001.'+suffix,
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
      payloadPath:'payload/relationship-pressure-c0.mjs',
      payloadSha256:hashFile(payloadPath),
      typedArgv:[],
      immutableInputs:[
        {path:'inputs/relationship-pressure-binding.json',sha256:hashFile(bindingPath)},
        {path:'inputs/foundation-v0.6.20-body.json',sha256:hashFile(f20BodyPath)},
        {path:'inputs/foundation-v0.6.20-revisions.json',sha256:hashFile(f20RevisionsPath)},
      ],
    },
    executionStateAtFormation:'TASK_PREPARED',
    singleUseNonce:'nonce-multivex-relationship-pressure-c0-a004-'+suffix+'-'+crypto.randomBytes(16).toString('hex'),
    stopConditions:[
      'STOP_ON_EXACT_SOURCE_DRIFT',
      'STOP_ON_FOUNDATION_BYTE_DRIFT',
      'STOP_ON_PRODUCT_HOST_EFFECT_POLICY_DRIFT',
      'STOP_ON_SOURCE_GATE_FAILURE',
      'STOP_ON_CANONICAL_WRITER_NON_IDLE',
      'STOP_ON_MODEL_NOT_ALREADY_SELECTED_4B',
      'STOP_ON_MODEL_ACQUISITION_OR_SWITCH_REQUIREMENT',
      'STOP_ON_CANONICAL_CONVERSATION_OR_MEMORY_MUTATION',
      'STOP_ON_RELATIONSHIP_PRESSURE_ENCOUNTER_FAILURE',
    ],
    archiveDigestBinding:'FILENAME_SHA256',
  };

  const formed=createCanonicalTaskZip({
    taskTemplate:bindAuthoritySummaryFromEnvelope(taskTemplate,authorityEnvelope),
    contentRoot,
    outputDirectory,
    outputStem:'VEXLIFE-MULTI-VEX-RELATIONSHIP-PRESSURE-C0-A001',
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
    throw new Error('MULTI_VEX_RELATIONSHIP_PRESSURE_PRODUCT_HOST_EFFECT_ADMISSION_MISMATCH');
  }

  process.stdout.write(JSON.stringify({
    disposition:'PASS',
    packagePath:formed.finalZip,
    packageSha256:formed.digest,
    taskRef:formed.task.taskRef,
    attemptRef:formed.task.attemptRef,
    candidate:binding.candidate,
    foundationTreatment:binding.foundationTreatment,
    relationship:binding.relationship,
    comparisonFamily:binding.comparisonFamily,
    carrier:binding.carrier,
  })+'\n');
}finally{
  fs.rmSync(workRoot,{recursive:true,force:true});
}
