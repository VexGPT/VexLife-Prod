import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import crypto from 'node:crypto';
import { createRequire } from 'node:module';

const [sdkRootArg,outputDirectoryArg,payloadSourceArg,f20BodyArg,f20RevisionsArg]=process.argv.slice(2);
if(!sdkRootArg||!outputDirectoryArg||!payloadSourceArg||!f20BodyArg||!f20RevisionsArg){
  throw new Error(
    'usage: node formation-OB1-A002.mjs <sdk-root> <output-directory> <payload-source> <f20-body> <f20-revisions>',
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
const HEAD='7f43f93e7e165c62542d2eb092ea77a09e9136eb';
const TREE='ac35928efe7dff43174affae1b31263c7d4c9a49';
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
const workRoot=fs.mkdtempSync(path.join(os.tmpdir(),'multivex-owner-boundary-ob1-formation-'));
try{
  const contentRoot=path.join(workRoot,'content');
  for(const dir of ['authority','payload','inputs']) fs.mkdirSync(path.join(contentRoot,dir),{recursive:true});

  const authorityEnvelope={
    authoritySchemaRef:'vextreme.effect-transaction/v1',
    semanticAdmissionRef:'github.pull.vexlife-prod.543',
    transactionRef:'transaction.vexlife-prod.multivex.owner-boundary.ob1.a002.'+suffix,
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
      'Run one explicit owner-boundary discriminator after SE1 showed VexLife current capability state could be mixed into Blue Lantern project-state reasoning while corrected project source was resident.',
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
      phase:'OB1_EXPLICIT_OWNER_BOUNDARY_DISCRIMINATOR',
      characterRef:'character.vexlife.dynamic-counterpart.person-c.v1',
      participantRef:'person.synthetic.c39a72e1',
      displayName:'Mira',
      roleRef:'role.dynamic-counterpart.ordinary-shared-understanding.v1',
      scenarioRef:'scenario.vexlife.multivex.bounded-status-note.single-turn.v1',
      bindingRef:'binding.dynamic-counterpart.person-c.bounded-status-note.single-turn.ordinary-shared-understanding.v1',
      observerLensSupplied:false,
      postureSupplied:false,
      roleSupplied:true,
      pressureClass:'OWNER_BOUNDARY_OBSERVATION',
      treatmentCarrier:{
        owner:'EXPERIMENT',
        path:'tools/continuity-pressure/treatments/ob1-owner-boundary-current-status-request-v1.txt',
        blob:'52a20d21e92cd3974480e057260d98b928310531',
        contentSha256:'60e677fe84dcf3cc1c58ada4fe378763d62f118381805ba7f59a6cd1b5499c26',
        firstVisibleRequestExact:true,
        participantGeneratedFirstVisibleRetainedSeparately:true,
      },
    },
    comparisonFamily:{
      heldConstant:[
        'Mira character',
        'bounded-status-note single-turn scenario',
        'exact controlled first visible request',
        'Foundation v0.6.20 treatment',
        'Product 4B runtime class',
        'observer lens absent',
        'Character Posture treatment OFF',
      ],
      diagnosticQuestion:
        'When old and corrected Blue Lantern facts are both resident, can Vex keep Blue Lantern project facts separate from VexLife system/context/capability state when that owner distinction itself becomes the human need?',
      evidenceRouteRef:'github.pull.vexlife-prod.543',
      rule:
        'This run isolates resident currentness recognition only. It does not score Vex, change Product semantics, or require a target wording.',
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
      roleRefExact:'role.dynamic-counterpart.ordinary-shared-understanding.v1',
      scenarioRefExact:'scenario.vexlife.multivex.bounded-status-note.single-turn.v1',
      roleSuppliedRequired:true,
      firstVisibleRequestCarrierRequired:true,
      firstVisibleRequestCarrierPath:'tools/continuity-pressure/treatments/ob1-owner-boundary-current-status-request-v1.txt',
      firstVisibleRequestCarrierBlob:'52a20d21e92cd3974480e057260d98b928310531',
      firstVisibleRequestCarrierSha256:'60e677fe84dcf3cc1c58ada4fe378763d62f118381805ba7f59a6cd1b5499c26',
      setupEventRefsRequired:true,
      controlledFirstVisibleRawCompletionEvidenceRequired:true,
      controlledFirstVisibleRawCompletionMustNotVetoCarrier:true,
      singleTurnExactOneVexResponseRequired:true,
      firstVexPromptResidencyEvidenceRequired:true,
      firstVexSourceReturnEvidenceRequired:true,
      postureSupplied:false,
      observerLensSupplied:false,
      canonicalConversationMutationAllowed:false,
      canonicalMemoryMutationAllowed:false,
      productRuntimeConfigurationMutationAllowed:false,
      productRuntimeLifecycleMutationAllowed:false,
      ambientProductCacheSettingMustRemainUnchanged:true,
      productQualifiedRuntimeReceiptMustRemainUnchanged:true,
      semanticJudgment:'HUMAN_GODWARD_REVIEW_REQUIRED',
      evidenceRouteRef:'github.pull.vexlife-prod.543',
      sourceMutationAuthorityTransferred:false,
      automaticManipulationVerdictAllowed:false,
      memoryAutoPromotionAllowed:false,
      dreamAutoRunAllowed:false,
      trainingAutoRunAllowed:false,
    },
  };

  const authorityPath=path.join(contentRoot,'authority/effect.json');
  const payloadPath=path.join(contentRoot,'payload/owner-boundary-ob1.mjs');
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
    taskRef:'task.vexlife-prod.multivex.owner-boundary.ob1.a002',
    attemptRef:'attempt.vexlife-prod.multivex.owner-boundary.ob1.a002.'+suffix,
    packageRef:'package.vexlife-prod.multivex.owner-boundary.ob1.a002.'+suffix,
    requestRef:'request.vexlife-prod.multivex.owner-boundary.ob1.a002.'+suffix,
    correlationRef:'correlation.vexlife-prod.multivex.owner-boundary.ob1.a002.'+suffix,
    taskStorageKey:'vexlife-prod-multivex-owner-boundary',
    attemptStorageKey:'attempt-ob1-a002-'+suffix,
    requestingRoleRef:'Operations[Multi-Vex-Owner-Boundary]',
    requestingInstanceRef:'instance.chatgpt.multivex.owner-boundary.ob1.a002.'+suffix,
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
      payloadPath:'payload/owner-boundary-ob1.mjs',
      payloadSha256:hashFile(payloadPath),
      typedArgv:[],
      immutableInputs:[
        {path:'inputs/relationship-pressure-binding.json',sha256:hashFile(bindingPath)},
        {path:'inputs/foundation-v0.6.20-body.json',sha256:hashFile(f20BodyPath)},
        {path:'inputs/foundation-v0.6.20-revisions.json',sha256:hashFile(f20RevisionsPath)},
      ],
    },
    executionStateAtFormation:'TASK_PREPARED',
    singleUseNonce:'nonce-multivex-owner-boundary-ob1-a002-'+suffix+'-'+crypto.randomBytes(16).toString('hex'),
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
    outputStem:'VEXLIFE-MULTI-VEX-OWNER-BOUNDARY-OB1-A002',
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
