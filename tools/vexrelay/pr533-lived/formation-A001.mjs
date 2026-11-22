import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import crypto from 'node:crypto';
import { createRequire } from 'node:module';

const [sdkRootArg,outputDirectoryArg,payloadSourceArg]=process.argv.slice(2);
if(!sdkRootArg||!outputDirectoryArg||!payloadSourceArg){
  throw new Error('usage: node formation-A001.mjs <sdk-root> <output-directory> <payload-source>');
}
const sdkRoot=path.resolve(sdkRootArg);
const outputDirectory=path.resolve(outputDirectoryArg);
const payloadSource=path.resolve(payloadSourceArg);
const require=createRequire(import.meta.url);
const packageBuilder=require(path.join(sdkRoot,'lib/vexlocalbridge/package-builder.js'));
const policy=require(path.join(sdkRoot,'lib/vexlocalbridge/vexlife-prod-host-effect-policy.js'));
const {bindAuthoritySummaryFromEnvelope,createCanonicalTaskZip,hashFile,writeJson}=packageBuilder;

const BASE='5a26a310878b191f39de521a8b5ae01a2d10b224';
const HEAD='3f124ed97ee74df0dffb49e5b368fbc78be1d57f';
const TREE='67f842dcababff034eeb61c3d401cf45c1c4bd00';
const BRANCH='VXG-112226-chatgpt-first-local-start-ui-d001';
const SDK_HEAD='149455eeec4c7b7ca00e91022154a85faa3d608c';
const SDK_TREE='53ea701f9bbf24a5cf37d43e4dbe3836187f4011';
const CURRENT_CONTRACT_REF='source.vexrelay.current-contract/v1';
const PROFILE_PATH=path.join(sdkRoot,'docs/private-continuity/vexlocalbridge/profiles/macos-victor-vexlife-prod.json');
const profile=JSON.parse(fs.readFileSync(PROFILE_PATH,'utf8'));
policy.validateProfileBinding(profile);
if(!fs.existsSync(payloadSource)||!fs.statSync(payloadSource).isFile()) throw new Error('PR533_LIVED_PAYLOAD_SOURCE_MISSING');

const suffix=crypto.randomBytes(10).toString('hex');
const workRoot=fs.mkdtempSync(path.join(os.tmpdir(),'pr533-first-local-start-lived-formation-'));
try{
  const contentRoot=path.join(workRoot,'content');
  for(const dir of ['authority','payload','inputs']) fs.mkdirSync(path.join(contentRoot,dir),{recursive:true});

  const authorityEnvelope={
    authoritySchemaRef:'vextreme.effect-transaction/v1',
    semanticAdmissionRef:'github.issue.vexlife-prod.514',
    transactionRef:`transaction.vexlife-prod.pr533.first-local-start-ui.lived.a001.${suffix}`,
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
      'effect.vexlife-prod.harness-send',
      'effect.vexlife-prod.approval',
      'effect.vexlife-prod.ready',
      'effect.vexlife-prod.merge',
      'effect.vexlife-prod.publication',
    ],
  };

  const binding={
    schemaVersion:'vexlife-prod.pr533-first-local-start-lived-binding/v1',
    continuity:'[VXG RealForever]',
    candidate:{repository:'VexGPT/VexLife-Prod',base:BASE,head:HEAD,tree:TREE,branch:BRANCH},
    carrier:{
      sdkRepository:'vgong24/Vextreme-SDK',
      qualifiedSourcePr:'github.pull.vextreme-sdk.1956',
      sdkHead:SDK_HEAD,
      sdkTree:SDK_TREE,
      currentContractRef:CURRENT_CONTRACT_REF,
      profileRef:profile.profileRef,
      policyRef:profile.vexLifeProdHostEffectPolicy.policyRef,
    },
    acceptance:{
      requiredModelHandle:'heretic-4b',
      modelSelectionMustAlreadyBeSaved:true,
      modelAcquisitionAllowed:false,
      modelSwitchAllowed:false,
      harnessSendAllowed:false,
      sourceMustBecomeCurrent:true,
      firstLocalStartProjection:{
        state:'READY',
        initialPromptPreparationPendingForNextTurn:true,
        initialPromptPreparationForActiveTurn:false,
      },
      presentationCoordinate:'element.vexlife-workspace.companion.conversation.response-progress',
      humanVisualAcceptance:'PENDING_HUMAN',
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
    taskRef:'task.vexlife-prod.pr533.first-local-start-ui.lived.a001',
    attemptRef:`attempt.vexlife-prod.pr533.first-local-start-ui.lived.a001.${suffix}`,
    packageRef:`package.vexlife-prod.pr533.first-local-start-ui.lived.a001.${suffix}`,
    requestRef:`request.vexlife-prod.pr533.first-local-start-ui.lived.a001.${suffix}`,
    correlationRef:`correlation.vexlife-prod.pr533.first-local-start-ui.lived.a001.${suffix}`,
    taskStorageKey:'vexlife-prod-pr533-first-local-start-ui-lived',
    attemptStorageKey:`attempt-a001-${suffix}`,
    requestingRoleRef:'Operations[PR533-LIVED-VISUAL]',
    requestingInstanceRef:`instance.chatgpt.pr533-first-local-start-ui-lived.a001.${suffix}`,
    parentOperationsRef:'github.issue.vexlife-prod.514',
    returnRouteRef:'github.pull.vexlife-prod.533',
    hostProfileRef:'macos-victor',
    repositoryExecutionProfileRefOrNull:'profile.repository.vexlife-prod.macos-v1',
    repositoryRefOrNull:'VexGPT/VexLife-Prod',
    baseRefOrNull:BASE,
    headRefOrNull:HEAD,
    treeRefOrNull:TREE,
    branchRefOrNull:BRANCH,
    claimRefOrNull:'github.pull.vexlife-prod.533',
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
      payloadPath:'payload/lived.mjs',
      payloadSha256:hashFile(payloadPath),
      typedArgv:[],
      immutableInputs:[{path:'inputs/lived-binding.json',sha256:hashFile(bindingPath)}],
    },
    executionStateAtFormation:'TASK_PREPARED',
    singleUseNonce:`nonce-pr533-first-local-start-lived-a001-${suffix}-${crypto.randomBytes(16).toString('hex')}`,
    stopConditions:[
      'STOP_ON_EXACT_SOURCE_DRIFT',
      'STOP_ON_PRODUCT_HOST_EFFECT_POLICY_DRIFT',
      'STOP_ON_ACTIVE_CONVERSATION_TURN',
      'STOP_ON_CUSTOM_HOME',
      'STOP_ON_MODEL_NOT_ALREADY_SELECTED_4B',
      'STOP_ON_MODEL_ACQUISITION_OR_SWITCH_REQUIREMENT',
      'STOP_ON_FIRST_LOCAL_START_PROJECTION_MISSING',
      'STOP_ON_LIVE_PRESENTATION_BYTES_DRIFT',
    ],
    archiveDigestBinding:'FILENAME_SHA256',
  };

  const formed=createCanonicalTaskZip({
    taskTemplate:bindAuthoritySummaryFromEnvelope(taskTemplate,authorityEnvelope),
    contentRoot,
    outputDirectory,
    outputStem:'VEXLIFE-PR533-FIRST-LOCAL-START-UI-LIVED-A001',
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
     admitted.productDataPolicy.runtimeLifecycleMutationAllowed!==true||
     admitted.productDataPolicy.serviceLifetimeMutationAllowed!==true){
    throw new Error('PR533_LIVED_PRODUCT_HOST_EFFECT_ADMISSION_MISMATCH');
  }

  process.stdout.write(JSON.stringify({
    disposition:'PASS',
    packagePath:formed.finalZip,
    packageSha256:formed.digest,
    taskRef:formed.task.taskRef,
    attemptRef:formed.task.attemptRef,
    candidate:{base:BASE,head:HEAD,tree:TREE,branch:BRANCH},
    carrier:{sdkHead:SDK_HEAD,sdkTree:SDK_TREE,qualifiedSourcePr:'github.pull.vextreme-sdk.1956',currentContractRef:CURRENT_CONTRACT_REF,profileRef:profile.profileRef},
  })+'\n');
}finally{
  fs.rmSync(workRoot,{recursive:true,force:true});
}
