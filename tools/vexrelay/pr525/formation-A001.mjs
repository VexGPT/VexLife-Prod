import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { createRequire } from 'node:module';
import { pathToFileURL } from 'node:url';

const [sdkRootArg, outputRootArg] = process.argv.slice(2);
if (!sdkRootArg || !outputRootArg) throw new Error('usage: node formation-A001.mjs <sdk-root> <output-root>');
const sdkRoot=path.resolve(sdkRootArg), outputRoot=path.resolve(outputRootArg);
const require=createRequire(import.meta.url);
const {formVexRelayTaskArchiveCarrier}=await import(pathToFileURL(path.join(sdkRoot,'lib/vexlocalbridge/vexrelay-task-archive-carrier-formation.mjs')).href);
const {createHostAdapter}=require(path.join(sdkRoot,'lib/vexlocalbridge/mac-runner.js'));
const {expectedCapsuleRoot}=require(path.join(sdkRoot,'lib/vexlocalbridge/cli.js'));
const native=require(path.join(sdkRoot,'lib/vexlocalbridge/vexrelay-task-archive.js'));

const BASE='3f85f2ecdea8824ba45c2d7240c96ec03c56ae0d';
const HEAD='f8e57e5d6b998ad04ac04a20945497ea174a9f2c';
const TREE='4b5171c9c0b25432f692974ebdb04f6c76d17556';
const BRANCH='VXG-112226-chatgpt-pr518-runtime-continuity-composition-c001';
const BASE_BRANCH='VXG-112226-chatgpt-initial-prompt-preparation-b001';
const CHANGED=[
  'src/jvmMain/kotlin/vexlife/local/VexLifeProdMacRuntimeHandoff.kt',
  'src/jvmMain/kotlin/vexlife/service/VexLifeProdLocalService.kt',
  'src/jvmMain/kotlin/vexlife/service/VexLifeProdMacServiceLifetime.kt',
  'src/jvmMain/kotlin/vexlife/setup/VexLifeProdSetupProcess.kt',
  'src/jvmTest/kotlin/vexlife/local/VexLifeProdMacRuntimeHandoffTest.kt',
  'src/jvmTest/kotlin/vexlife/service/VexLifeProdLocalServiceTest.kt',
  'src/jvmTest/kotlin/vexlife/service/VexLifeProdMacServiceLifetimeTest.kt',
  'src/jvmTest/kotlin/vexlife/setup/VexLifeProdSetupProcessTest.kt',
];
const SDK_HEAD='6f8f6f50467f5df5a542713f7dc25b82db4ac8fa';
const SDK_TREE='96795640b01f1969da980f52d5a8ec136a8c44aa';
const profilePath=path.join(sdkRoot,'docs/private-continuity/vexlocalbridge/profiles/macos-victor-vextreme-sdk.json');
const profile=JSON.parse(fs.readFileSync(profilePath,'utf8'));
const adapter=createHostAdapter(profile);
const suffix=crypto.randomBytes(10).toString('hex');
const now=new Date(), expiry=new Date(now.getTime()+60*60*1000);
const effects=['CHECK_AUTH','READ_REPOSITORY','CHECK_STATE','QUERY_PROVIDER','RUN_LOCAL_COMMAND','FORM_RESULT'];

const seed={
 schemaVersion:'vex.relay.task/v2',continuity:'[VXG RealForever]',
 taskRef:'task.vexlife-prod.pr525.runtime-continuity-composition.source-owner.a001',
 attemptRef:`attempt.vexlife-prod.pr525.runtime-continuity-composition.source-owner.a001.${suffix}`,
 requestRef:`request.vexlife-prod.pr525.runtime-continuity-composition.source-owner.a001.${suffix}`,
 correlationRef:`correlation.vexlife-prod.pr525.runtime-continuity-composition.source-owner.a001.${suffix}`,
 packageRef:`package.vexlife-prod.pr525.runtime-continuity-composition.source-owner.a001.${suffix}`,
 original:{
  originalIntent:'Compose the already-proven Mac startup-order and exact runtime-handoff relationships beneath Warm Boot Stage B so lived prefix preparation can execute on the same MLX runtime.',
  originalGoal:'Prove the exact eight-path #507 -> #510 composition on top of #518 without replaying historical branch trees or unrelated changes.',
  userProtectedIntent:'Keep VexLife seamless and performant while preserving canonical Conversation/Memory ownership and making Victor perform only one bounded host action.',
  sourceRefs:['github.issue.vexlife-prod.514','github.pull.vexlife-prod.507','github.pull.vexlife-prod.510','github.pull.vexlife-prod.518','github.pull.vexlife-prod.520','github.pull.vexlife-prod.525']
 },
 lane:{entryClass:'FOUNDATION_DELTA',scopeMode:'STAGE_BOUNDED',laneRef:'lane.vexlife-prod.warm-boot.runtime-continuity-composition',parentRootRef:'github.issue.vexlife-prod.514',workRef:'github.pull.vexlife-prod.525',claimRef:'claim.vexlife-prod.pr525.runtime-continuity-composition.source-owner',returnRouteRef:'return.vexlife-prod.pr525.runtime-continuity-composition.source-owner.a001',ownerRef:'owner.vexlife-prod.pr525.runtime-continuity-composition',rootHubRef:'hub.vexlife-prod.warm-boot'},
 role:{activeRole:'CODER',roleRef:'role.coder.vexlife-prod.pr525',instanceRef:`instance.vexlife-prod.pr525.source-owner.a001.${suffix}`,occupancyRef:'occupancy.vexlife-prod.pr525.runtime-continuity-composition',providerBindingRef:'provider.github.vexlife-prod.pr525',threadRef:'thread.vexlife-prod.pr525.runtime-continuity-composition',authorityEnvelopeRef:'authority-envelope.vexlife-prod.pr525.source-owner'},
 capabilities:{platformGitHubRead:'UNAVAILABLE',platformGitHubWrite:'UNAVAILABLE'},
 execution:{hostProfile:'macos-victor',executionSurface:'MAC_ZIP_RUN_COMMAND',singleUseNonce:`nonce-pr525-a001-${suffix}-${crypto.randomBytes(16).toString('hex')}`,duplicateAttemptMarker:'marker.vexlife-prod.pr525.runtime-continuity-composition.source-owner.a001',duplicateProtectionMode:'OUTER_RELAY_BOUND',formedAt:now.toISOString(),expiresAtOrNull:expiry.toISOString()},
 workspace:{mode:'RUN_ROOT',projectRef:'project.vexlife-prod.pr525.source-owner',projectPath:'workspace',createRoot:true,createProjectPath:true},
 repositories:[{repositoryRef:'primary',mode:'CLONE_IN_WORKSPACE',nameWithOwner:'VexGPT/VexLife-Prod',localPath:'vexlife-prod',remote:'origin',baseBranch:BASE_BRANCH,acceptedBase:BASE,expectedHead:HEAD,checkoutRef:HEAD,branch:BRANCH,expectedRemoteUrl:null,cloneTransport:'GITHUB_GH',githubHostname:'github.com'}],
 authority:{authorityRef:'authority.vexlife-prod.pr525.source-owner',authorityMode:'EXACT_STAGE_EFFECTS',grantedEffects:effects,conditionalEffects:[],consumedEffects:[],remainingEffects:effects,heldEffects:['WRITE_SOURCE','CREATE_BRANCH','CREATE_COMMIT','PUSH_BRANCH','CREATE_PULL_REQUEST','UPDATE_COORDINATION_METADATA','APPROVE_EXACT_HEAD','MARK_READY','ORDINARY_MERGE'],repositoryMembranes:[{repositoryRef:'primary',allowedPaths:CHANGED}],allowedExecutables:['/bin/bash','/usr/bin/git','git','gh','node'],allowedEnvironment:[],remoteObservationOwner:'LocalRelay',allowMutationLoop:false,stopConditions:['STOP_ON_UNKNOWN','STOP_ON_PATH_ESCAPE','STOP_ON_AUTH_BLOCK','STOP_ON_TEST_FAILURE','STOP_ON_EXACT_HEAD_OR_TREE_DRIFT']},
 source:{sourceSetRef:'source-set.vexlife-prod.pr525.runtime-continuity-composition',sourceVersionRef:`source-version.vexlife-prod.pr525.${HEAD}`,overlapEvidenceRef:'overlap.vexlife-prod.pr525.runtime-continuity-composition.a001',activeConflictState:'NONE',candidateState:'EXACT',candidateHead:HEAD,candidateTree:TREE,expectedChangedPaths:CHANGED.map(p=>({repositoryRef:'primary',path:p})),intentionallyUnchangedAuthorizedPaths:[]},
 policy:{maxOuterReturnFiles:10,maxSelectedSourceEntries:10,maxSelectedSourceBytes:5000000,maxDiffBytes:10000000,requireRacePreflightBeforeMutation:false,requireLocalValidationBeforePublish:false,requireInputPackageManifest:false,signoffCommit:true,draftPullRequest:true}
};

const stage={repositoryRef:'primary',expectedBase:BASE,expectedHead:HEAD,expectedTree:TREE,branch:BRANCH,
 proofCommands:[
  {command:'/bin/bash',args:['tools/release/prove-mac-candidate.sh','0.0.0'],proofClass:'TEST',candidateEvidence:true,predicateClass:'SOURCE_OWNER_REPOSITORY',predicateRef:'predicate.vexlife-prod.pr525.mac-candidate',timeoutMs:1800000}
 ],
 maxFiles:8,stateRef:'state.vexlife-prod.pr525.source-ready',currentLifecycleState:'SOURCE_READY',
 currentGoal:'Prove the exact #507 -> #510 runtime-continuity composition on top of Warm Boot Stage B.',
 remainingGoals:['Run fresh Independent Assurance on the exact proved #525 head.','Resume lived same-runtime Warm Boot cache proof using the accepted composed source.'],
 onPass:{lifecycleState:'ASSURANCE_PENDING',nextFunction:'F13.RUN_FRESH_ASSURANCE',nextOwner:'Operations',whyThisIsNext:'The composed source is proved; fresh Independent Assurance must separately judge the exact integration before lived provider execution.'},
 resultFilename:'RETURN-VEXLIFE-PR525-RUNTIME-CONTINUITY-COMPOSITION-SOURCE-OWNER-A001.zip'
};

const authorityEnvelope={authoritySchemaRef:'vextreme.effect-transaction/v1',semanticAdmissionRef:'github.issue.vexlife-prod.514',transactionRef:'transaction.vexlife-prod.pr525.runtime-continuity-composition.source-owner.a001',transactionVersion:1,allowedEffectRefs:['effect.vexrelay.execute'],heldEffectRefs:['effect.vexlife-prod.source-mutation','effect.vexlife-prod.approval','effect.vexlife-prod.ready','effect.vexlife-prod.merge','effect.vexlife-prod.product-lifecycle','effect.vexlife-prod.model-runtime']};

const outerTaskTemplate={
 schemaVersion:'vextreme.vexlocalbridge-task-manifest/v1',artifactClass:'EXECUTABLE_RELAY',
 taskRef:'task.vexlife-prod.pr525.runtime-continuity-composition.source-owner.a001.carrier',
 attemptRef:`attempt.vexlife-prod.pr525.runtime-continuity-composition.source-owner.a001.carrier.${suffix}`,
 packageRef:`package.vexlife-prod.pr525.runtime-continuity-composition.source-owner.a001.carrier.${suffix}`,
 requestRef:`request.vexlife-prod.pr525.runtime-continuity-composition.source-owner.a001.carrier.${suffix}`,
 correlationRef:`correlation.vexlife-prod.pr525.runtime-continuity-composition.source-owner.a001.carrier.${suffix}`,
 taskStorageKey:'vexlife-prod-pr525-runtime-continuity-composition-source-owner-a001',attemptStorageKey:`attempt-a001-${suffix}`,
 requestingRoleRef:'Operations[PR525-SOURCE-OWNER]',requestingInstanceRef:`instance.vexlife-prod.pr525.source-owner.carrier.a001.${suffix}`,
 parentOperationsRef:'github.issue.vexlife-prod.514',returnRouteRef:'github.pull.vexlife-prod.525',
 hostProfileRef:'macos-victor',repositoryExecutionProfileRefOrNull:'profile.repository.vextreme-sdk.macos-v1',
 repositoryRefOrNull:'vgong24/Vextreme-SDK',baseRefOrNull:SDK_HEAD,headRefOrNull:SDK_HEAD,treeRefOrNull:SDK_TREE,branchRefOrNull:'main',claimRefOrNull:null,exactPathRefs:[],
 authorityBinding:{authoritySchemaRef:authorityEnvelope.authoritySchemaRef,authorityEnvelopePath:'authority/effect.json',authorityEnvelopeSha256:'0'.repeat(64),semanticAdmissionRef:authorityEnvelope.semanticAdmissionRef,transactionRef:authorityEnvelope.transactionRef,transactionVersion:1,allowedEffectRefs:[...authorityEnvelope.allowedEffectRefs],heldEffectRefs:[...authorityEnvelope.heldEffectRefs]},
 payload:{payloadClass:native.VEXRELAY_ARCHIVE_PAYLOAD_CLASS,payloadRuntime:native.VEXRELAY_ARCHIVE_RUNTIME,payloadPath:'payload/inner-vexrelay.zip',payloadSha256:'0'.repeat(64),typedArgv:[],immutableInputs:[{path:'inputs/vexrelay-binding.json',sha256:'0'.repeat(64)}]},
 executionStateAtFormation:'TASK_PREPARED',singleUseNonce:`nonce-pr525-carrier-a001-${suffix}-${crypto.randomBytes(16).toString('hex')}`,
 stopConditions:['STOP_ON_INNER_TASK_IDENTITY_DRIFT','STOP_ON_EXACT_SOURCE_DRIFT','STOP_ON_PROOF_FAILURE','STOP_ON_CANONICAL_RETURN_CONSUMER_FAILURE'],archiveDigestBinding:'FILENAME_SHA256'
};

fs.mkdirSync(outputRoot,{recursive:true});
const formed=await formVexRelayTaskArchiveCarrier({recipe:'ExactHeadSourceOwnerProof',seed,stage,outerTaskTemplate,authorityEnvelope,outputDirectory:outputRoot,outputStem:'VEXLIFE-PR525-RUNTIME-CONTINUITY-COMPOSITION-SOURCE-OWNER-A001-TASK',ttlMs:60*60*1000,archiveAdapter:adapter.archive});
const capsuleRoot=expectedCapsuleRoot(formed.outer.finalZip,formed.outer.digest,profile.zipRelayCapsule);
process.stdout.write(JSON.stringify({finalZip:formed.outer.finalZip,digest:formed.outer.digest,capsuleRoot,profilePath,outerTaskRef:formed.outer.task.taskRef,innerTaskRef:formed.inner.manifest.taskRef,innerAttemptRef:formed.inner.manifest.attemptRef,innerArchiveSha256:formed.inner.sha256,candidate:{base:BASE,head:HEAD,tree:TREE,branch:BRANCH},formationEvidence:formed.outer.formationEvidence},null,2)+'\n');
