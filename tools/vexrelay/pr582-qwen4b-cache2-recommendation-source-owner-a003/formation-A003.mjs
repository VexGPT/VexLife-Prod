import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { createRequire } from 'node:module';
import { pathToFileURL } from 'node:url';

const [sdkRootArg, outputRootArg] = process.argv.slice(2);
if (!sdkRootArg || !outputRootArg) throw new Error('usage: node formation-A003.mjs <sdk-root> <output-root>');
const sdkRoot=path.resolve(sdkRootArg), outputRoot=path.resolve(outputRootArg);
const require=createRequire(import.meta.url);
const {formVexRelayTaskArchiveCarrier}=await import(pathToFileURL(path.join(sdkRoot,'lib/vexlocalbridge/vexrelay-task-archive-carrier-formation.mjs')).href);
const {createHostAdapter}=require(path.join(sdkRoot,'lib/vexlocalbridge/mac-runner.js'));
const {expectedCapsuleRoot}=require(path.join(sdkRoot,'lib/vexlocalbridge/cli.js'));
const native=require(path.join(sdkRoot,'lib/vexlocalbridge/vexrelay-task-archive.js'));

const BASE='3a47bac8618c99214cfdb3d4396e626bc95b96ef';
const HEAD='84103059f8cd220680a87f3adad4a1b62b02eed7';
const TREE='10be850ea05fd0a2f71354a3f561137a90604755';
const BRANCH='VXG-112326-chatgpt-qwen4b-cache2-recommendation-a001';
const BASE_BRANCH='VXG-112326-chatgpt-conversation-prefix-cache-a001';
const CHANGED=[
  'src/commonMain/kotlin/vexlife/model/VexLifeModelsFamily.kt',
  'src/commonMain/kotlin/vexlife/model/heretic/HereticRuntimeConfiguration.kt',
  'src/commonMain/kotlin/vexlife/web/VexLifeWebP1RuntimeArtifacts.kt',
  'src/commonTest/kotlin/vexlife/model/VexLifeModelsFamilyTest.kt',
  'src/commonTest/kotlin/vexlife/model/heretic/HereticRuntimeConfigurationTest.kt',
  'src/commonTest/kotlin/vexlife/web/VexLifeWebP1RuntimeArtifactsTest.kt',
  'src/jvmTest/kotlin/vexlife/model/VexLifeModelsTest.kt',
  'src/jvmTest/kotlin/vexlife/model/heretic/runtime/FileHereticRuntimeConfigurationStoreTest.kt',
  'src/jvmTest/kotlin/vexlife/model/heretic/runtime/HereticExecutionTopologyTest.kt'
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
 taskRef:'task.vexlife-prod.pr582.qwen4b-cache2-recommendation.source-owner.a003',
 attemptRef:`attempt.vexlife-prod.pr582.qwen4b-cache2-recommendation.source-owner.a003.${suffix}`,
 requestRef:`request.vexlife-prod.pr582.qwen4b-cache2-recommendation.source-owner.a003.${suffix}`,
 correlationRef:`correlation.vexlife-prod.pr582.qwen4b-cache2-recommendation.source-owner.a003.${suffix}`,
 packageRef:`package.vexlife-prod.pr582.qwen4b-cache2-recommendation.source-owner.a003.${suffix}`,
 original:{
  originalIntent:'Carry the lived-proven immediate follow-up readiness into the existing Qwen 4B runtime recommendation without silently changing the Product safety default.',
  originalGoal:'Prove the exact nine-path #582 recommendation candidate: default cache 1 unchanged, recommended cache 2, maximum cache 3 unchanged, plus the truthful Models/Topology projection closure required when the recommendation sits above the minimum.',
  userProtectedIntent:'Vex should make faster follow-ups available without hidden cache/runtime state changes or forcing the person to manage provider mechanics.',
  sourceRefs:['github.pull.vexlife-prod.582','github.pull.vexlife-prod.573','github.issue.vexlife-prod.514','github.pull.vexlife-prod.550']
 },
 lane:{entryClass:'SAME_LANE_SUCCESSOR',scopeMode:'STAGE_BOUNDED',laneRef:'lane.vexlife-prod.response-readiness.qwen4b-cache2-recommendation',parentRootRef:'github.issue.vexlife-prod.514',workRef:'github.pull.vexlife-prod.582',claimRef:'claim.vexlife-prod.pr582.qwen4b-cache2-recommendation.source-owner',returnRouteRef:'return.vexlife-prod.pr582.qwen4b-cache2-recommendation.source-owner.a003',ownerRef:'owner.vexlife-prod.pr582.qwen4b-cache2-recommendation',rootHubRef:'hub.vexlife-prod.warm-boot'},
 role:{activeRole:'CODER',roleRef:'role.coder.vexlife-prod.pr582',instanceRef:`instance.vexlife-prod.pr582.source-owner.a003.${suffix}`,occupancyRef:'occupancy.vexlife-prod.pr582.qwen4b-cache2-recommendation',providerBindingRef:'provider.github.vexlife-prod.pr582',threadRef:'thread.vexlife-prod.pr582.qwen4b-cache2-recommendation',authorityEnvelopeRef:'authority-envelope.vexlife-prod.pr582.source-owner'},
 capabilities:{platformGitHubRead:'UNAVAILABLE',platformGitHubWrite:'UNAVAILABLE'},
 execution:{hostProfile:'macos-victor',executionSurface:'MAC_ZIP_RUN_COMMAND',singleUseNonce:`nonce-pr582-a003-${suffix}-${crypto.randomBytes(16).toString('hex')}`,duplicateAttemptMarker:'marker.vexlife-prod.pr582.qwen4b-cache2-recommendation.source-owner.a003',duplicateProtectionMode:'OUTER_RELAY_BOUND',formedAt:now.toISOString(),expiresAtOrNull:expiry.toISOString()},
 workspace:{mode:'RUN_ROOT',projectRef:'project.vexlife-prod.pr582.source-owner',projectPath:'workspace',createRoot:true,createProjectPath:true},
 repositories:[{repositoryRef:'primary',mode:'CLONE_IN_WORKSPACE',nameWithOwner:'VexGPT/VexLife-Prod',localPath:'vexlife-prod',remote:'origin',baseBranch:BASE_BRANCH,acceptedBase:BASE,expectedHead:HEAD,checkoutRef:HEAD,branch:BRANCH,expectedRemoteUrl:null,cloneTransport:'GITHUB_GH',githubHostname:'github.com'}],
 authority:{authorityRef:'authority.vexlife-prod.pr582.source-owner',authorityMode:'EXACT_STAGE_EFFECTS',grantedEffects:effects,conditionalEffects:[],consumedEffects:[],remainingEffects:effects,heldEffects:['WRITE_SOURCE','CREATE_BRANCH','CREATE_COMMIT','PUSH_BRANCH','CREATE_PULL_REQUEST','UPDATE_COORDINATION_METADATA','APPROVE_EXACT_HEAD','MARK_READY','ORDINARY_MERGE'],repositoryMembranes:[{repositoryRef:'primary',allowedPaths:CHANGED}],allowedExecutables:['/bin/bash','/usr/bin/git','git','gh','node'],allowedEnvironment:[],remoteObservationOwner:'LocalRelay',allowMutationLoop:false,stopConditions:['STOP_ON_UNKNOWN','STOP_ON_PATH_ESCAPE','STOP_ON_AUTH_BLOCK','STOP_ON_TEST_FAILURE','STOP_ON_EXACT_HEAD_OR_TREE_DRIFT']},
 source:{sourceSetRef:'source-set.vexlife-prod.pr582.qwen4b-cache2-recommendation',sourceVersionRef:`source-version.vexlife-prod.pr582.${HEAD}`,overlapEvidenceRef:'overlap.vexlife-prod.pr582.qwen4b-cache2-recommendation.a003',activeConflictState:'NONE',candidateState:'EXACT',candidateHead:HEAD,candidateTree:TREE,expectedChangedPaths:CHANGED.map(p=>({repositoryRef:'primary',path:p})),intentionallyUnchangedAuthorizedPaths:[]},
 policy:{maxOuterReturnFiles:10,maxSelectedSourceEntries:9,maxSelectedSourceBytes:5000000,maxDiffBytes:10000000,requireRacePreflightBeforeMutation:false,requireLocalValidationBeforePublish:false,requireInputPackageManifest:false,signoffCommit:true,draftPullRequest:true}
};

const stage={repositoryRef:'primary',expectedBase:BASE,expectedHead:HEAD,expectedTree:TREE,branch:BRANCH,
 proofCommands:[
  {command:'/bin/bash',args:['-c',"ISOLATED=\"$(/usr/bin/mktemp -d \"${TMPDIR:-/tmp}/pr582-a003-java-tmp.XXXXXX\")\"; cleanup(){ /bin/rm -rf -- \"$ISOLATED\"; }; trap cleanup EXIT HUP INT TERM; export TMPDIR=\"$ISOLATED\"; export JAVA_TOOL_OPTIONS=\"-Djava.io.tmpdir=$ISOLATED\"; /bin/bash -x tools/release/prove-mac-candidate.sh 0.0.0"],proofClass:'TEST',candidateEvidence:true,predicateClass:'SOURCE_OWNER_REPOSITORY',predicateRef:'predicate.vexlife-prod.pr582.mac-candidate-isolated-trace',timeoutMs:1800000}
 ],
 maxFiles:9,stateRef:'state.vexlife-prod.pr582.source-ready',currentLifecycleState:'SOURCE_READY',
 currentGoal:'Re-prove the same evidence-backed Qwen 4B recommendation/projection closure after A002 exposed only one stale absent-target Control-list expectation; no Product behavior change.',
 remainingGoals:['If A003 PASS, run fresh Independent Assurance on recommendation, lower-choice standing, and compatibility behavior.','If CLEAR, currentize #550 copy through its localization owner and then run integration review against the active overlap field.'],
 onPass:{lifecycleState:'ASSURANCE_PENDING',nextFunction:'F13.RUN_FRESH_ASSURANCE',nextOwner:'Operations',whyThisIsNext:'The exact recommendation source is proved; fresh semantic Assurance must judge whether lived evidence supports recommendation 2 without collapsing recommendation into automatic configuration.'},
 resultFilename:'RETURN-VEXLIFE-PR582-QWEN4B-CACHE2-RECOMMENDATION-SOURCE-OWNER-A003.zip'
};

const authorityEnvelope={authoritySchemaRef:'vextreme.effect-transaction/v1',semanticAdmissionRef:'github.pull.vexlife-prod.582',transactionRef:'transaction.vexlife-prod.pr582.qwen4b-cache2-recommendation.source-owner.a003',transactionVersion:1,allowedEffectRefs:['effect.vexrelay.execute'],heldEffectRefs:['effect.vexlife-prod.source-mutation','effect.vexlife-prod.runtime-configuration','effect.vexlife-prod.model-runtime','effect.vexlife-prod.localization','effect.vexlife-prod.approval','effect.vexlife-prod.ready','effect.vexlife-prod.merge']};

const outerTaskTemplate={
 schemaVersion:'vextreme.vexlocalbridge-task-manifest/v1',artifactClass:'EXECUTABLE_RELAY',
 taskRef:'task.vexlife-prod.pr582.qwen4b-cache2-recommendation.source-owner.a003.carrier',
 attemptRef:`attempt.vexlife-prod.pr582.qwen4b-cache2-recommendation.source-owner.a003.carrier.${suffix}`,
 packageRef:`package.vexlife-prod.pr582.qwen4b-cache2-recommendation.source-owner.a003.carrier.${suffix}`,
 requestRef:`request.vexlife-prod.pr582.qwen4b-cache2-recommendation.source-owner.a003.carrier.${suffix}`,
 correlationRef:`correlation.vexlife-prod.pr582.qwen4b-cache2-recommendation.source-owner.a003.carrier.${suffix}`,
 taskStorageKey:'vexlife-prod-pr582-qwen4b-cache2-recommendation-source-owner-a003',attemptStorageKey:`attempt-a003-${suffix}`,
 requestingRoleRef:'Operations[PR582-SOURCE-OWNER]',requestingInstanceRef:`instance.vexlife-prod.pr582.source-owner.carrier.a003.${suffix}`,
 parentOperationsRef:'github.issue.vexlife-prod.514',returnRouteRef:'github.pull.vexlife-prod.582',
 hostProfileRef:'macos-victor',repositoryExecutionProfileRefOrNull:'profile.repository.vextreme-sdk.macos-v1',
 repositoryRefOrNull:'vgong24/Vextreme-SDK',baseRefOrNull:SDK_HEAD,headRefOrNull:SDK_HEAD,treeRefOrNull:SDK_TREE,branchRefOrNull:'main',claimRefOrNull:null,exactPathRefs:[],
 authorityBinding:{authoritySchemaRef:authorityEnvelope.authoritySchemaRef,authorityEnvelopePath:'authority/effect.json',authorityEnvelopeSha256:'0'.repeat(64),semanticAdmissionRef:authorityEnvelope.semanticAdmissionRef,transactionRef:authorityEnvelope.transactionRef,transactionVersion:1,allowedEffectRefs:[...authorityEnvelope.allowedEffectRefs],heldEffectRefs:[...authorityEnvelope.heldEffectRefs]},
 payload:{payloadClass:native.VEXRELAY_ARCHIVE_PAYLOAD_CLASS,payloadRuntime:native.VEXRELAY_ARCHIVE_RUNTIME,payloadPath:'payload/inner-vexrelay.zip',payloadSha256:'0'.repeat(64),typedArgv:[],immutableInputs:[{path:'inputs/vexrelay-binding.json',sha256:'0'.repeat(64)}]},
 executionStateAtFormation:'TASK_PREPARED',singleUseNonce:`nonce-pr582-carrier-a003-${suffix}-${crypto.randomBytes(16).toString('hex')}`,
 stopConditions:['STOP_ON_INNER_TASK_IDENTITY_DRIFT','STOP_ON_EXACT_SOURCE_DRIFT','STOP_ON_PROOF_FAILURE','STOP_ON_CANONICAL_RETURN_CONSUMER_FAILURE'],archiveDigestBinding:'FILENAME_SHA256'
};

fs.mkdirSync(outputRoot,{recursive:true});
const formed=await formVexRelayTaskArchiveCarrier({recipe:'ExactHeadSourceOwnerProof',seed,stage,outerTaskTemplate,authorityEnvelope,outputDirectory:outputRoot,outputStem:'VEXLIFE-PR582-QWEN4B-CACHE2-RECOMMENDATION-SOURCE-OWNER-A003-TASK',ttlMs:60*60*1000,archiveAdapter:adapter.archive});
const capsuleRoot=expectedCapsuleRoot(formed.outer.finalZip,formed.outer.digest,profile.zipRelayCapsule);
process.stdout.write(JSON.stringify({finalZip:formed.outer.finalZip,digest:formed.outer.digest,capsuleRoot,profilePath,outerTaskRef:formed.outer.task.taskRef,innerTaskRef:formed.inner.manifest.taskRef,innerAttemptRef:formed.inner.manifest.attemptRef,innerArchiveSha256:formed.inner.sha256,candidate:{base:BASE,head:HEAD,tree:TREE,branch:BRANCH},formationEvidence:formed.outer.formationEvidence},null,2)+'\n');
