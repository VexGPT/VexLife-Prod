import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { createRequire } from 'node:module';
import { pathToFileURL } from 'node:url';

const [sdkRootArg, outputRootArg] = process.argv.slice(2);
if (!sdkRootArg || !outputRootArg) throw new Error('usage: node formation-A004.mjs <sdk-root> <output-root>');
const sdkRoot=path.resolve(sdkRootArg), outputRoot=path.resolve(outputRootArg);
const require=createRequire(import.meta.url);
const {formVexRelayTaskArchiveCarrier}=await import(pathToFileURL(path.join(sdkRoot,'lib/vexlocalbridge/vexrelay-task-archive-carrier-formation.mjs')).href);
const {createHostAdapter}=require(path.join(sdkRoot,'lib/vexlocalbridge/mac-runner.js'));
const {expectedCapsuleRoot}=require(path.join(sdkRoot,'lib/vexlocalbridge/cli.js'));
const native=require(path.join(sdkRoot,'lib/vexlocalbridge/vexrelay-task-archive.js'));

const BASE='a23b6a949d27e4b77530273a390955f0d127ad1a';
const HEAD='1edebbd556f78e43a8f08fb1be233ccb33fa414f';
const TREE='17bb6ac19a1c3565c8fe62afa385fc8ababbfc24';
const BRANCH='VXG-112326-chatgpt-conversation-prefix-cache-a001';
const BASE_BRANCH='VXG-112126-chatgpt-sent-resource-continuity-current-main-a001';
const CHANGED=[
  'docs/foundation/HERETIC-EXECUTION-TOPOLOGY.md',
  'src/jvmMain/kotlin/vexlife/companion/DirectCompanionConnection.kt',
  'src/jvmMain/kotlin/vexlife/companion/DirectCompanionTurn.kt',
  'src/jvmMain/kotlin/vexlife/companion/DirectPromptContext.kt',
  'src/jvmMain/kotlin/vexlife/model/heretic/runtime/HereticPersistentRuntime.kt',
  'src/jvmTest/kotlin/vexlife/companion/DirectCompanionConnectionTest.kt',
  'src/jvmTest/kotlin/vexlife/companion/DirectCompanionTurnTest.kt',
  'src/jvmTest/kotlin/vexlife/companion/DirectPromptContextTest.kt',
  'src/jvmTest/kotlin/vexlife/model/heretic/runtime/HereticPersistentRuntimeTest.kt',
  'src/jvmTest/kotlin/vexlife/model/heretic/runtime/MacHereticRuntimePlatformTest.kt'
]
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
 taskRef:'task.vexlife-prod.pr573.conversation-prefix-cache.source-owner.a004',
 attemptRef:`attempt.vexlife-prod.pr573.conversation-prefix-cache.source-owner.a004.${suffix}`,
 requestRef:`request.vexlife-prod.pr573.conversation-prefix-cache.source-owner.a004.${suffix}`,
 correlationRef:`correlation.vexlife-prod.pr573.conversation-prefix-cache.source-owner.a004.${suffix}`,
 packageRef:`package.vexlife-prod.pr573.conversation-prefix-cache.source-owner.a004.${suffix}`,
 original:{
  originalIntent:'Reuse the exact append-only Conversation prefix VexLife already owns so immediate local follow-ups stop repeatedly prefilling prior Conversation work while mutable current context remains current.',
  originalGoal:'Prove the exact ten-path #573 Conversation-prefix cache candidate: prompt-owned boundary projection, provider-only carriage, exact mlx-lm segmentation, historical runtime convergence, and source tests, without changing prompt bytes/order, cache recommendations/defaults, Conversation, Memory, Context Profile, or Vex Context ownership.',
  userProtectedIntent:'Vex should reactively carry forward computation whose semantic source remains current, while changed dependencies re-resolve through their rightful owners and provider-cache mechanics remain invisible to the person.',
  sourceRefs:['github.pull.vexlife-prod.573','github.issue.vexlife-prod.514','github.pull.vexlife-prod.488','github.pull.vexlife-prod.555']
 },
 lane:{entryClass:'SAME_LANE_SUCCESSOR',scopeMode:'STAGE_BOUNDED',laneRef:'lane.vexlife-prod.response-readiness.conversation-prefix-cache',parentRootRef:'github.issue.vexlife-prod.514',workRef:'github.pull.vexlife-prod.573',claimRef:'claim.vexlife-prod.pr573.conversation-prefix-cache.source-owner',returnRouteRef:'return.vexlife-prod.pr573.conversation-prefix-cache.source-owner.a004',ownerRef:'owner.vexlife-prod.pr573.conversation-prefix-cache',rootHubRef:'hub.vexlife-prod.warm-boot'},
 role:{activeRole:'CODER',roleRef:'role.coder.vexlife-prod.pr573',instanceRef:`instance.vexlife-prod.pr573.source-owner.a004.${suffix}`,occupancyRef:'occupancy.vexlife-prod.pr573.conversation-prefix-cache',providerBindingRef:'provider.github.vexlife-prod.pr573',threadRef:'thread.vexlife-prod.pr573.conversation-prefix-cache',authorityEnvelopeRef:'authority-envelope.vexlife-prod.pr573.source-owner'},
 capabilities:{platformGitHubRead:'UNAVAILABLE',platformGitHubWrite:'UNAVAILABLE'},
 execution:{hostProfile:'macos-victor',executionSurface:'MAC_ZIP_RUN_COMMAND',singleUseNonce:`nonce-pr573-a004-${suffix}-${crypto.randomBytes(16).toString('hex')}`,duplicateAttemptMarker:'marker.vexlife-prod.pr573.conversation-prefix-cache.source-owner.a004',duplicateProtectionMode:'OUTER_RELAY_BOUND',formedAt:now.toISOString(),expiresAtOrNull:expiry.toISOString()},
 workspace:{mode:'RUN_ROOT',projectRef:'project.vexlife-prod.pr573.source-owner',projectPath:'workspace',createRoot:true,createProjectPath:true},
 repositories:[{repositoryRef:'primary',mode:'CLONE_IN_WORKSPACE',nameWithOwner:'VexGPT/VexLife-Prod',localPath:'vexlife-prod',remote:'origin',baseBranch:BASE_BRANCH,acceptedBase:BASE,expectedHead:HEAD,checkoutRef:HEAD,branch:BRANCH,expectedRemoteUrl:null,cloneTransport:'GITHUB_GH',githubHostname:'github.com'}],
 authority:{authorityRef:'authority.vexlife-prod.pr573.source-owner',authorityMode:'EXACT_STAGE_EFFECTS',grantedEffects:effects,conditionalEffects:[],consumedEffects:[],remainingEffects:effects,heldEffects:['WRITE_SOURCE','CREATE_BRANCH','CREATE_COMMIT','PUSH_BRANCH','CREATE_PULL_REQUEST','UPDATE_COORDINATION_METADATA','APPROVE_EXACT_HEAD','MARK_READY','ORDINARY_MERGE'],repositoryMembranes:[{repositoryRef:'primary',allowedPaths:CHANGED}],allowedExecutables:['/bin/bash','/usr/bin/git','git','gh','node'],allowedEnvironment:[],remoteObservationOwner:'LocalRelay',allowMutationLoop:false,stopConditions:['STOP_ON_UNKNOWN','STOP_ON_PATH_ESCAPE','STOP_ON_AUTH_BLOCK','STOP_ON_TEST_FAILURE','STOP_ON_EXACT_HEAD_OR_TREE_DRIFT']},
 source:{sourceSetRef:'source-set.vexlife-prod.pr573.conversation-prefix-cache',sourceVersionRef:`source-version.vexlife-prod.pr573.${HEAD}`,overlapEvidenceRef:'overlap.vexlife-prod.pr573.conversation-prefix-cache.a004',activeConflictState:'NONE',candidateState:'EXACT',candidateHead:HEAD,candidateTree:TREE,expectedChangedPaths:CHANGED.map(p=>({repositoryRef:'primary',path:p})),intentionallyUnchangedAuthorizedPaths:[]},
 policy:{maxOuterReturnFiles:10,maxSelectedSourceEntries:10,maxSelectedSourceBytes:5000000,maxDiffBytes:10000000,requireRacePreflightBeforeMutation:false,requireLocalValidationBeforePublish:false,requireInputPackageManifest:false,signoffCommit:true,draftPullRequest:true}
};

const stage={repositoryRef:'primary',expectedBase:BASE,expectedHead:HEAD,expectedTree:TREE,branch:BRANCH,
 proofCommands:[
  {command:'/bin/bash',args:['-c',"ISOLATED=\"$(/usr/bin/mktemp -d \"${TMPDIR:-/tmp}/pr573-a004-java-tmp.XXXXXX\")\"; cleanup(){ /bin/rm -rf -- \"$ISOLATED\"; }; trap cleanup EXIT HUP INT TERM; export TMPDIR=\"$ISOLATED\"; export JAVA_TOOL_OPTIONS=\"-Djava.io.tmpdir=$ISOLATED\"; /bin/bash -x tools/release/prove-mac-candidate.sh 0.0.0"],proofClass:'TEST',candidateEvidence:true,predicateClass:'SOURCE_OWNER_REPOSITORY',predicateRef:'predicate.vexlife-prod.pr573.mac-candidate-isolated-trace',timeoutMs:1800000}
 ],
 maxFiles:10,stateRef:'state.vexlife-prod.pr573.source-ready',currentLifecycleState:'SOURCE_READY',
 currentGoal:'Re-prove the same immutable corrected Conversation-prefix candidate with an isolated JVM temp root so global continuity-pressure scratch names cannot interfere, while retaining shell trace for the unchanged Mac packaging tail; no Product source or lifecycle change.',
 remainingGoals:['If A004 PASS, run fresh Independent Assurance on the unchanged corrected head.','Then run a fresh lived 4B cache=2 immediate-follow-up proof; A001 lived negative evidence remains terminal.'],
 onPass:{lifecycleState:'ASSURANCE_PENDING',nextFunction:'F13.RUN_FRESH_ASSURANCE',nextOwner:'Operations',whyThisIsNext:'The exact Conversation-prefix source is proved; fresh Independent Assurance must judge the same immutable head before any lived runtime effect is requested.'},
 resultFilename:'RETURN-VEXLIFE-PR573-CONVERSATION-PREFIX-CACHE-SOURCE-OWNER-A004.zip'
};

const authorityEnvelope={authoritySchemaRef:'vextreme.effect-transaction/v1',semanticAdmissionRef:'github.pull.vexlife-prod.573',transactionRef:'transaction.vexlife-prod.pr573.conversation-prefix-cache.source-owner.a004',transactionVersion:1,allowedEffectRefs:['effect.vexrelay.execute'],heldEffectRefs:['effect.vexlife-prod.source-mutation','effect.vexlife-prod.approval','effect.vexlife-prod.ready','effect.vexlife-prod.merge','effect.vexlife-prod.product-lifecycle','effect.vexlife-prod.model-runtime']};

const outerTaskTemplate={
 schemaVersion:'vextreme.vexlocalbridge-task-manifest/v1',artifactClass:'EXECUTABLE_RELAY',
 taskRef:'task.vexlife-prod.pr573.conversation-prefix-cache.source-owner.a004.carrier',
 attemptRef:`attempt.vexlife-prod.pr573.conversation-prefix-cache.source-owner.a004.carrier.${suffix}`,
 packageRef:`package.vexlife-prod.pr573.conversation-prefix-cache.source-owner.a004.carrier.${suffix}`,
 requestRef:`request.vexlife-prod.pr573.conversation-prefix-cache.source-owner.a004.carrier.${suffix}`,
 correlationRef:`correlation.vexlife-prod.pr573.conversation-prefix-cache.source-owner.a004.carrier.${suffix}`,
 taskStorageKey:'vexlife-prod-pr573-conversation-prefix-cache-source-owner-a004',attemptStorageKey:`attempt-a004-${suffix}`,
 requestingRoleRef:'Operations[PR573-SOURCE-OWNER]',requestingInstanceRef:`instance.vexlife-prod.pr573.source-owner.carrier.a004.${suffix}`,
 parentOperationsRef:'github.issue.vexlife-prod.514',returnRouteRef:'github.pull.vexlife-prod.573',
 hostProfileRef:'macos-victor',repositoryExecutionProfileRefOrNull:'profile.repository.vextreme-sdk.macos-v1',
 repositoryRefOrNull:'vgong24/Vextreme-SDK',baseRefOrNull:SDK_HEAD,headRefOrNull:SDK_HEAD,treeRefOrNull:SDK_TREE,branchRefOrNull:'main',claimRefOrNull:null,exactPathRefs:[],
 authorityBinding:{authoritySchemaRef:authorityEnvelope.authoritySchemaRef,authorityEnvelopePath:'authority/effect.json',authorityEnvelopeSha256:'0'.repeat(64),semanticAdmissionRef:authorityEnvelope.semanticAdmissionRef,transactionRef:authorityEnvelope.transactionRef,transactionVersion:1,allowedEffectRefs:[...authorityEnvelope.allowedEffectRefs],heldEffectRefs:[...authorityEnvelope.heldEffectRefs]},
 payload:{payloadClass:native.VEXRELAY_ARCHIVE_PAYLOAD_CLASS,payloadRuntime:native.VEXRELAY_ARCHIVE_RUNTIME,payloadPath:'payload/inner-vexrelay.zip',payloadSha256:'0'.repeat(64),typedArgv:[],immutableInputs:[{path:'inputs/vexrelay-binding.json',sha256:'0'.repeat(64)}]},
 executionStateAtFormation:'TASK_PREPARED',singleUseNonce:`nonce-pr573-carrier-a004-${suffix}-${crypto.randomBytes(16).toString('hex')}`,
 stopConditions:['STOP_ON_INNER_TASK_IDENTITY_DRIFT','STOP_ON_EXACT_SOURCE_DRIFT','STOP_ON_PROOF_FAILURE','STOP_ON_CANONICAL_RETURN_CONSUMER_FAILURE'],archiveDigestBinding:'FILENAME_SHA256'
};

fs.mkdirSync(outputRoot,{recursive:true});
const formed=await formVexRelayTaskArchiveCarrier({recipe:'ExactHeadSourceOwnerProof',seed,stage,outerTaskTemplate,authorityEnvelope,outputDirectory:outputRoot,outputStem:'VEXLIFE-PR573-CONVERSATION-PREFIX-CACHE-SOURCE-OWNER-A004-TASK',ttlMs:60*60*1000,archiveAdapter:adapter.archive});
const capsuleRoot=expectedCapsuleRoot(formed.outer.finalZip,formed.outer.digest,profile.zipRelayCapsule);
process.stdout.write(JSON.stringify({finalZip:formed.outer.finalZip,digest:formed.outer.digest,capsuleRoot,profilePath,outerTaskRef:formed.outer.task.taskRef,innerTaskRef:formed.inner.manifest.taskRef,innerAttemptRef:formed.inner.manifest.attemptRef,innerArchiveSha256:formed.inner.sha256,candidate:{base:BASE,head:HEAD,tree:TREE,branch:BRANCH},formationEvidence:formed.outer.formationEvidence},null,2)+'\n');
