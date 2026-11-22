import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import crypto from 'node:crypto';
import { createRequire } from 'node:module';

const [sdkRootArg,outputDirectoryArg,payloadSourceArg]=process.argv.slice(2);
if(!sdkRootArg||!outputDirectoryArg||!payloadSourceArg) throw new Error('usage: node formation-A002.mjs <sdk-root> <output-directory> <payload-source>');
const sdkRoot=path.resolve(sdkRootArg), outputDirectory=path.resolve(outputDirectoryArg), payloadSource=path.resolve(payloadSourceArg);
const require=createRequire(import.meta.url);
const packageBuilder=require(path.join(sdkRoot,'lib/vexlocalbridge/package-builder.js'));
const policy=require(path.join(sdkRoot,'lib/vexlocalbridge/vexlife-prod-host-effect-policy.js'));
const {bindAuthoritySummaryFromEnvelope,createCanonicalTaskZip,hashFile,writeJson}=packageBuilder;

const BASE='0566b1eed876a28c2fcc6586743ae1f4af7eaecd';
const HEAD='a23b6a949d27e4b77530273a390955f0d127ad1a';
const TREE='a268826bf26eecb3a2136593b202cf651643d4e9';
const BRANCH='VXG-112126-chatgpt-sent-resource-continuity-current-main-a001';
const SDK_HEAD='149455eeec4c7b7ca00e91022154a85faa3d608c';
const SDK_TREE='53ea701f9bbf24a5cf37d43e4dbe3836187f4011';
const CURRENT_CONTRACT_REF='source.vexrelay.current-contract/v1';
const PROFILE_PATH=path.join(sdkRoot,'docs/private-continuity/vexlocalbridge/profiles/macos-victor-vexlife-prod.json');
const profile=JSON.parse(fs.readFileSync(PROFILE_PATH,'utf8'));
policy.validateProfileBinding(profile);
if(!fs.existsSync(payloadSource)||!fs.statSync(payloadSource).isFile()) throw new Error('PR488_WHOLE_LIVED_PAYLOAD_SOURCE_MISSING');

const suffix=crypto.randomBytes(10).toString('hex');
const workRoot=fs.mkdtempSync(path.join(os.tmpdir(),'pr488-whole-lived-formation-'));
try{
  const contentRoot=path.join(workRoot,'content');for(const d of ['authority','payload','inputs'])fs.mkdirSync(path.join(contentRoot,d),{recursive:true});
  const authorityEnvelope={
    authoritySchemaRef:'vextreme.effect-transaction/v1',semanticAdmissionRef:'github.pull.vexlife-prod.488',
    transactionRef:`transaction.vexlife-prod.pr488.warmboot-sent-resource.whole-lived.a002.${suffix}`,transactionVersion:1,
    allowedEffectRefs:['effect.vexlocalbridge.vexlife-prod.product-lifecycle-proof/v1','effect.vexlocalbridge.vexlife-prod.build-dependency-fetch/v1'],
    heldEffectRefs:['effect.vexlife-prod.source-mutation','effect.vexlife-prod.host-installation','effect.vexlife-prod.home-recreation','effect.vexlife-prod.home-deletion','effect.vexlife-prod.conversation-harness-mutation','effect.vexlife-prod.memory-harness-mutation','effect.vexlife-prod.model-acquisition','effect.vexlife-prod.model-switch-for-proof','effect.vexlife-prod.harness-send','effect.vexlife-prod.approval','effect.vexlife-prod.ready','effect.vexlife-prod.merge','effect.vexlife-prod.publication']
  };
  const binding={
    schemaVersion:'vexlife-prod.pr488-whole-lived-binding/v1',continuity:'[VXG RealForever]',
    candidate:{repository:'VexGPT/VexLife-Prod',base:BASE,head:HEAD,tree:TREE,branch:BRANCH},
    carrier:{sdkRepository:'vgong24/Vextreme-SDK',qualifiedSourcePr:'github.pull.vextreme-sdk.1956',sdkHead:SDK_HEAD,sdkTree:SDK_TREE,currentContractRef:CURRENT_CONTRACT_REF,profileRef:profile.profileRef,policyRef:profile.vexLifeProdHostEffectPolicy.policyRef},
    acceptance:{requiredModelHandle:'heretic-4b',modelSelectionMustAlreadyBeSaved:true,modelAcquisitionAllowed:false,modelSwitchAllowed:false,harnessSendAllowed:false,firstPreparationState:'READY_REUSABLE',firstSendExactlyOneStaticImage:true,firstResourceReviewableAfterPresentationReentry:true,firstPreparationConsumed:true,firstCachedPromptTokens:'>0',firstPrefillTokens:'<promptTokens',secondSendResourceCount:0,firstLocalStartAfterFirstSend:false,sameRuntimeReceiptAndPid:true,humanGroundednessJudgment:'PENDING_VICTOR',humanSentResourceVisualAcceptance:'PENDING_VICTOR'}
  };
  const authorityPath=path.join(contentRoot,'authority/effect.json'),payloadPath=path.join(contentRoot,'payload/lived.mjs'),bindingPath=path.join(contentRoot,'inputs/lived-binding.json');
  writeJson(authorityPath,authorityEnvelope);fs.copyFileSync(payloadSource,payloadPath);writeJson(bindingPath,binding);
  const taskTemplate={
    schemaVersion:'vextreme.vexlocalbridge-task-manifest/v1',artifactClass:'EXECUTABLE_RELAY',
    taskRef:'task.vexlife-prod.pr488.warmboot-sent-resource.whole-lived.a002',
    attemptRef:`attempt.vexlife-prod.pr488.warmboot-sent-resource.whole-lived.a002.${suffix}`,
    packageRef:`package.vexlife-prod.pr488.warmboot-sent-resource.whole-lived.a002.${suffix}`,
    requestRef:`request.vexlife-prod.pr488.warmboot-sent-resource.whole-lived.a002.${suffix}`,
    correlationRef:`correlation.vexlife-prod.pr488.warmboot-sent-resource.whole-lived.a002.${suffix}`,
    taskStorageKey:'vexlife-prod-pr488-whole-lived',attemptStorageKey:`attempt-a002-${suffix}`,
    requestingRoleRef:'Operations[PR488-WHOLE-LIVED]',requestingInstanceRef:`instance.chatgpt.pr488-whole-lived.a002.${suffix}`,
    parentOperationsRef:'github.pull.vexlife-prod.488',returnRouteRef:'github.pull.vexlife-prod.488',hostProfileRef:'macos-victor',repositoryExecutionProfileRefOrNull:'profile.repository.vexlife-prod.macos-v1',
    repositoryRefOrNull:'VexGPT/VexLife-Prod',baseRefOrNull:BASE,headRefOrNull:HEAD,treeRefOrNull:TREE,branchRefOrNull:BRANCH,claimRefOrNull:'github.pull.vexlife-prod.488',exactPathRefs:[],
    authorityBinding:{authoritySchemaRef:authorityEnvelope.authoritySchemaRef,authorityEnvelopePath:'authority/effect.json',authorityEnvelopeSha256:hashFile(authorityPath),semanticAdmissionRef:'stale',transactionRef:'stale',transactionVersion:1,allowedEffectRefs:['stale'],heldEffectRefs:[]},
    payload:{payloadClass:'NODE_SCRIPT',payloadRuntime:'node',payloadPath:'payload/lived.mjs',payloadSha256:hashFile(payloadPath),typedArgv:[],immutableInputs:[{path:'inputs/lived-binding.json',sha256:hashFile(bindingPath)}]},
    executionStateAtFormation:'TASK_PREPARED',singleUseNonce:`nonce-pr488-whole-lived-a002-${suffix}-${crypto.randomBytes(16).toString('hex')}`,
    stopConditions:['STOP_ON_EXACT_SOURCE_DRIFT','STOP_ON_PRODUCT_HOST_EFFECT_POLICY_DRIFT','STOP_ON_ACTIVE_CONVERSATION_TURN','STOP_ON_CUSTOM_HOME','STOP_ON_MODEL_NOT_ALREADY_SELECTED_4B','STOP_ON_MODEL_ACQUISITION_OR_SWITCH_REQUIREMENT','STOP_ON_PREPARATION_NON_READY','STOP_ON_FIRST_SEND_NOT_EXACT_IMAGE_RESOURCE','STOP_ON_FIRST_RESOURCE_NOT_REVIEWABLE','STOP_ON_SECOND_SEND_STALE_RESOURCE','STOP_ON_RUNTIME_CONTINUITY_LOSS'],archiveDigestBinding:'FILENAME_SHA256'
  };
  const formed=createCanonicalTaskZip({taskTemplate:bindAuthoritySummaryFromEnvelope(taskTemplate,authorityEnvelope),contentRoot,outputDirectory,outputStem:'VEXLIFE-PR488-WARMBOOT-SENT-RESOURCE-WHOLE-LIVED-A002',ttlMs:3*60*60*1000});
  const admitted=policy.admitTask({profile,task:formed.task,authorityEnvelope});
  if(admitted.candidate.base!==BASE||admitted.candidate.head!==HEAD||admitted.candidate.tree!==TREE||admitted.buildDependencyFetchAuthorized!==true||admitted.productDataPolicy.conversationMutationAllowed!==false||admitted.productDataPolicy.memoryMutationAllowed!==false||admitted.productDataPolicy.modelAcquisitionAllowed!==false||admitted.productDataPolicy.modelSwitchForProofAllowed!==false||admitted.productDataPolicy.runtimeLifecycleMutationAllowed!==true||admitted.productDataPolicy.serviceLifetimeMutationAllowed!==true) throw new Error('PR488_WHOLE_LIVED_PRODUCT_HOST_EFFECT_ADMISSION_MISMATCH');
  process.stdout.write(JSON.stringify({disposition:'PASS',packagePath:formed.finalZip,packageSha256:formed.digest,taskRef:formed.task.taskRef,attemptRef:formed.task.attemptRef,candidate:{base:BASE,head:HEAD,tree:TREE,branch:BRANCH},carrier:{sdkHead:SDK_HEAD,sdkTree:SDK_TREE,qualifiedSourcePr:'github.pull.vextreme-sdk.1956',currentContractRef:CURRENT_CONTRACT_REF,profileRef:profile.profileRef}})+'\n');
}finally{fs.rmSync(workRoot,{recursive:true,force:true});}
