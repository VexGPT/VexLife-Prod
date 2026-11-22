import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import crypto from 'node:crypto';
import { createRequire } from 'node:module';

const [sdkRootArg,outputDirectoryArg,payloadSourceArg]=process.argv.slice(2);
if(!sdkRootArg||!outputDirectoryArg||!payloadSourceArg){
  throw new Error('usage: node formation-A002.mjs <sdk-root> <output-directory> <payload-source>');
}
const sdkRoot=path.resolve(sdkRootArg);
const outputDirectory=path.resolve(outputDirectoryArg);
const payloadSource=path.resolve(payloadSourceArg);
const require=createRequire(import.meta.url);
const packageBuilder=require(path.join(sdkRoot,'lib/vexlocalbridge/package-builder.js'));
const policy=require(path.join(sdkRoot,'lib/vexlocalbridge/vexlife-prod-host-effect-policy.js'));
const {
  bindAuthoritySummaryFromEnvelope,
  createCanonicalTaskZip,
  hashFile,
  writeJson,
}=packageBuilder;

const BASE='0566b1eed876a28c2fcc6586743ae1f4af7eaecd';
const HEAD='3f85f2ecdea8824ba45c2d7240c96ec03c56ae0d';
const TREE='0ea9001c3d3a0ef0a87ff755a57be33c91ffdc1b';
const BRANCH='VXG-112226-chatgpt-initial-prompt-preparation-b001';
const SDK_HEAD='149455eeec4c7b7ca00e91022154a85faa3d608c';
const SDK_TREE='53ea701f9bbf24a5cf37d43e4dbe3836187f4011';
const PROFILE_PATH=path.join(sdkRoot,'docs/private-continuity/vexlocalbridge/profiles/macos-victor-vexlife-prod.json');
const profile=JSON.parse(fs.readFileSync(PROFILE_PATH,'utf8'));
policy.validateProfileBinding(profile);
if(!fs.existsSync(payloadSource)||!fs.statSync(payloadSource).isFile()) throw new Error('PR518_LIVED_PAYLOAD_SOURCE_MISSING');

const CHANGED=[
  'src/jvmMain/kotlin/vexlife/companion/DirectCompanionConnection.kt',
  'src/jvmMain/kotlin/vexlife/companion/DirectCompanionTurn.kt',
  'src/jvmMain/kotlin/vexlife/companion/VexLifeInitialPromptPreparation.kt',
  'src/jvmMain/kotlin/vexlife/logging/VexLogger.kt',
  'src/jvmMain/kotlin/vexlife/service/VexLifeProdLocalService.kt',
  'src/jvmTest/kotlin/vexlife/companion/DirectCompanionConnectionTest.kt',
  'src/jvmTest/kotlin/vexlife/companion/DirectCompanionTurnTest.kt',
  'src/jvmTest/kotlin/vexlife/companion/DirectPromptContextTest.kt',
  'src/jvmTest/kotlin/vexlife/companion/VexLifeInitialPromptPreparationTest.kt',
];
const suffix=crypto.randomBytes(10).toString('hex');
const workRoot=fs.mkdtempSync(path.join(os.tmpdir(),'pr518-warm-boot-lived-formation-'));

try{
  const contentRoot=path.join(workRoot,'content');
  for(const dir of ['authority','payload','inputs']) fs.mkdirSync(path.join(contentRoot,dir),{recursive:true});

  const authorityEnvelope={
    authoritySchemaRef:'vextreme.effect-transaction/v1',
    semanticAdmissionRef:'github.issue.vexlife-prod.514',
    transactionRef:`transaction.vexlife-prod.pr518.warm-boot-lived.a002.${suffix}`,
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
      'effect.vexlife-prod.conversation-harness-mutation',
      'effect.vexlife-prod.memory-harness-mutation',
      'effect.vexlife-prod.model-acquisition',
      'effect.vexlife-prod.model-switch-for-proof',
      'effect.vexlife-prod.presentation-readiness',
      'effect.vexlife-prod.publication',
      'effect.vexlife-prod.ready',
      'effect.vexlife-prod.merge',
    ],
  };
  const binding={
    schemaVersion:'vexlife-prod.pr518-warm-boot-lived-binding/v1',
    continuity:'[VXG RealForever]',
    candidate:{repository:'VexGPT/VexLife-Prod',base:BASE,head:HEAD,tree:TREE,branch:BRANCH,changedPaths:CHANGED},
    carrier:{sdkRepository:'vgong24/Vextreme-SDK',sdkHead:SDK_HEAD,sdkTree:SDK_TREE,profileRef:profile.profileRef,policyRef:profile.vexLifeProdHostEffectPolicy.policyRef},
    acceptance:{
      requiredModelHandle:'heretic-4b',
      preparationState:'READY_REUSABLE',
      cachedPromptTokens:'>0',
      prefillTokens:'<promptTokens',
      sameRuntimeReceiptAndPid:true,
      humanSendMustBeOrdinaryProductSend:true,
      harnessConversationMutationAllowed:false,
      harnessMemoryMutationAllowed:false,
    },
  };

  const authorityPath=path.join(contentRoot,'authority/effect.json');
  const payloadPath=path.join(contentRoot,'payload/lived.mjs');
  const bindingPath=path.join(contentRoot,'inputs/lived-binding.json');
  writeJson(authorityPath,authorityEnvelope);
  fs.copyFileSync(payloadSource,payloadPath);
  writeJson(bindingPath,binding);

  const taskTemplate={
    schemaVersion:'vextreme.vexlocalbridge-task-manifest/v1',
    artifactClass:'EXECUTABLE_RELAY',
    taskRef:'task.vexlife-prod.pr518.warm-boot-lived.a002',
    attemptRef:`attempt.vexlife-prod.pr518.warm-boot-lived.a002.${suffix}`,
    packageRef:`package.vexlife-prod.pr518.warm-boot-lived.a002.${suffix}`,
    requestRef:`request.vexlife-prod.pr518.warm-boot-lived.a002.${suffix}`,
    correlationRef:`correlation.vexlife-prod.pr518.warm-boot-lived.a002.${suffix}`,
    taskStorageKey:'vexlife-prod-pr518-warm-boot-lived',
    attemptStorageKey:`attempt-a002-${suffix}`,
    requestingRoleRef:'Operations[PR518-LIVED]',
    requestingInstanceRef:`instance.chatgpt.pr518-warm-boot-lived.a002.${suffix}`,
    parentOperationsRef:'github.issue.vexlife-prod.514',
    returnRouteRef:'github.pull.vexlife-prod.518',
    hostProfileRef:'macos-victor',
    repositoryExecutionProfileRefOrNull:'profile.repository.vexlife-prod.macos-v1',
    repositoryRefOrNull:'VexGPT/VexLife-Prod',
    baseRefOrNull:BASE,
    headRefOrNull:HEAD,
    treeRefOrNull:TREE,
    branchRefOrNull:BRANCH,
    claimRefOrNull:'github.pull.vexlife-prod.518',
    exactPathRefs:CHANGED,
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
      payloadPath:'payload/lived.mjs',
      payloadSha256:hashFile(payloadPath),
      typedArgv:[],
      immutableInputs:[{path:'inputs/lived-binding.json',sha256:hashFile(bindingPath)}],
    },
    executionStateAtFormation:'TASK_PREPARED',
    singleUseNonce:`nonce-pr518-warm-boot-lived-a002-${suffix}-${crypto.randomBytes(16).toString('hex')}`,
    stopConditions:[
      'STOP_ON_EXACT_CANDIDATE_DRIFT',
      'STOP_ON_PRODUCT_HOST_EFFECT_POLICY_DRIFT',
      'STOP_ON_CUSTOM_HOME',
      'STOP_ON_ACTIVE_CONVERSATION_WRITER',
      'STOP_ON_MODEL_ACQUISITION_OR_SWITCH_REQUIREMENT',
      'STOP_ON_PREPARATION_NON_READY',
      'STOP_ON_CACHE_REUSE_NOT_OBSERVED',
    ],
    archiveDigestBinding:'FILENAME_SHA256',
  };

  const formed=createCanonicalTaskZip({
    taskTemplate:bindAuthoritySummaryFromEnvelope(taskTemplate,authorityEnvelope),
    contentRoot,
    outputDirectory,
    outputStem:'VEXLIFE-PR518-WARM-BOOT-LIVED-A002',
    ttlMs:2*60*60*1000,
  });
  const admitted=policy.admitTask({
    profile,
    task:formed.task,
    authorityEnvelope,
  });
  if(admitted.candidate.head!==HEAD||admitted.candidate.tree!==TREE||
     admitted.buildDependencyFetchAuthorized!==true||
     admitted.productDataPolicy.conversationMutationAllowed!==false||
     admitted.productDataPolicy.memoryMutationAllowed!==false||
     admitted.productDataPolicy.modelAcquisitionAllowed!==false||
     admitted.productDataPolicy.modelSwitchForProofAllowed!==false){
    throw new Error('PR518_LIVED_PRODUCT_HOST_EFFECT_ADMISSION_MISMATCH');
  }

  process.stdout.write(JSON.stringify({
    disposition:'PASS',
    packagePath:formed.finalZip,
    packageSha256:formed.digest,
    taskRef:formed.task.taskRef,
    attemptRef:formed.task.attemptRef,
    candidate:{base:BASE,head:HEAD,tree:TREE,branch:BRANCH},
    carrier:{sdkHead:SDK_HEAD,sdkTree:SDK_TREE,profileRef:profile.profileRef},
  })+'\n');
}finally{
  fs.rmSync(workRoot,{recursive:true,force:true});
}
