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

const BASE='84103059f8cd220680a87f3adad4a1b62b02eed7';
const HEAD='70f1bcb0740f882bb7b39ec053d9948ed93b8d1d';
const TREE='306382cc98d3d8881c080f2eb761b7570515b570';
const BRANCH='VXG-112426-chatgpt-response-readiness-projection-a001';
const BASE_BRANCH='VXG-112326-chatgpt-qwen4b-cache2-recommendation-a001';
const CHANGED=[
  'src/commonMain/kotlin/vexlife/service/VexLifeProdServiceContract.kt',
  'src/commonTest/kotlin/vexlife/service/VexLifeProdServiceContractTest.kt',
  'src/jvmMain/kotlin/vexlife/companion/DirectCompanionTurn.kt',
  'src/jvmMain/kotlin/vexlife/companion/DirectTurnContextStore.kt',
  'src/jvmMain/kotlin/vexlife/local/VexLifeProdLocalSetup.kt',
  'src/jvmMain/kotlin/vexlife/local/VexLifeProdRuntimeSession.kt',
  'src/jvmMain/kotlin/vexlife/service/VexLifeProdLocalService.kt',
  'src/jvmTest/kotlin/vexlife/companion/DirectTurnContextStoreTest.kt'
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
 taskRef:'task.vexlife-prod.pr593.response-readiness-projection.source-owner.a001',
 attemptRef:`attempt.vexlife-prod.pr593.response-readiness-projection.source-owner.a001.${suffix}`,
 requestRef:`request.vexlife-prod.pr593.response-readiness-projection.source-owner.a001.${suffix}`,
 correlationRef:`correlation.vexlife-prod.pr593.response-readiness-projection.source-owner.a001.${suffix}`,
 packageRef:`package.vexlife-prod.pr593.response-readiness-projection.source-owner.a001.${suffix}`,
 original:{
  originalIntent:'Project the physically-proved Response Readiness relationship without making provider cache mechanics or guessed resource thresholds human-facing truth.',
  originalGoal:'Prove the eight-path #593 semantic projection candidate: effective runtime mode, durable final provider measurement, and source-owned Conversation projection compile and preserve historical turn-evidence compatibility.',
  userProtectedIntent:'Vex should own machine mechanics behind truthful human-facing follow-up readiness. This SourceOwner gate proves semantic source only; it must not add presentation copy, infer invisible cache residency, invent a RAM threshold, or absorb Multi-Vex.',
  sourceRefs:['github.pull.vexlife-prod.593','github.pull.vexlife-prod.582','github.pull.vexlife-prod.573','github.issue.vexlife-prod.514','github.pull.vexlife-prod.171','github.pull.vexlife-prod.172']
 },
 lane:{entryClass:'SAME_LANE_SUCCESSOR',scopeMode:'STAGE_BOUNDED',laneRef:'lane.vexlife-prod.response-readiness.response-readiness-projection-proof',parentRootRef:'github.issue.vexlife-prod.514',workRef:'github.pull.vexlife-prod.587',claimRef:'claim.vexlife-prod.pr593.response-readiness-projection.source-owner',returnRouteRef:'return.vexlife-prod.pr593.response-readiness-projection.source-owner.a001',ownerRef:'owner.vexlife-prod.pr593.response-readiness-projection-proof',rootHubRef:'hub.vexlife-prod.warm-boot'},
 role:{activeRole:'CODER',roleRef:'role.coder.vexlife-prod.pr593',instanceRef:`instance.vexlife-prod.pr593.response-readiness-projection.source-owner.a001.${suffix}`,occupancyRef:'occupancy.vexlife-prod.pr593.response-readiness-projection-proof',providerBindingRef:'provider.github.vexlife-prod.pr593',threadRef:'thread.vexlife-prod.pr593.response-readiness-projection-proof',authorityEnvelopeRef:'authority-envelope.vexlife-prod.pr593.response-readiness-projection.source-owner'},
 capabilities:{platformGitHubRead:'UNAVAILABLE',platformGitHubWrite:'UNAVAILABLE'},
 execution:{hostProfile:'macos-victor',executionSurface:'MAC_ZIP_RUN_COMMAND',singleUseNonce:`nonce-pr593-a001-${suffix}-${crypto.randomBytes(16).toString('hex')}`,duplicateAttemptMarker:'marker.vexlife-prod.pr593.response-readiness-projection.source-owner.a001',duplicateProtectionMode:'OUTER_RELAY_BOUND',formedAt:now.toISOString(),expiresAtOrNull:expiry.toISOString()},
 workspace:{mode:'RUN_ROOT',projectRef:'project.vexlife-prod.pr593.response-readiness-projection.source-owner',projectPath:'workspace',createRoot:true,createProjectPath:true},
 repositories:[{repositoryRef:'primary',mode:'CLONE_IN_WORKSPACE',nameWithOwner:'VexGPT/VexLife-Prod',localPath:'vexlife-prod',remote:'origin',baseBranch:BASE_BRANCH,acceptedBase:BASE,expectedHead:HEAD,checkoutRef:HEAD,branch:BRANCH,expectedRemoteUrl:null,cloneTransport:'GITHUB_GH',githubHostname:'github.com'}],
 authority:{authorityRef:'authority.vexlife-prod.pr593.response-readiness-projection.source-owner',authorityMode:'EXACT_STAGE_EFFECTS',grantedEffects:effects,conditionalEffects:[],consumedEffects:[],remainingEffects:effects,heldEffects:['WRITE_SOURCE','CREATE_BRANCH','CREATE_COMMIT','PUSH_BRANCH','CREATE_PULL_REQUEST','UPDATE_COORDINATION_METADATA','APPROVE_EXACT_HEAD','MARK_READY','ORDINARY_MERGE'],repositoryMembranes:[{repositoryRef:'primary',allowedPaths:CHANGED}],allowedExecutables:['/bin/bash','/usr/bin/git','git','gh','node'],allowedEnvironment:[],remoteObservationOwner:'LocalRelay',allowMutationLoop:false,stopConditions:['STOP_ON_UNKNOWN','STOP_ON_PATH_ESCAPE','STOP_ON_AUTH_BLOCK','STOP_ON_TEST_FAILURE','STOP_ON_EXACT_HEAD_OR_TREE_DRIFT']},
 source:{sourceSetRef:'source-set.vexlife-prod.pr593.response-readiness-projection-proof',sourceVersionRef:`source-version.vexlife-prod.pr593.${HEAD}`,overlapEvidenceRef:'overlap.vexlife-prod.pr593.response-readiness-projection-proof.a001',activeConflictState:'NONE',candidateState:'EXACT',candidateHead:HEAD,candidateTree:TREE,expectedChangedPaths:CHANGED.map(p=>({repositoryRef:'primary',path:p})),intentionallyUnchangedAuthorizedPaths:[]},
 policy:{maxOuterReturnFiles:10,maxSelectedSourceEntries:8,maxSelectedSourceBytes:5000000,maxDiffBytes:10000000,requireRacePreflightBeforeMutation:false,requireLocalValidationBeforePublish:false,requireInputPackageManifest:false,signoffCommit:true,draftPullRequest:true}
};

const stage={repositoryRef:'primary',expectedBase:BASE,expectedHead:HEAD,expectedTree:TREE,branch:BRANCH,
 proofCommands:[
  {command:'/bin/bash',args:['-c',"ISOLATED=\"$(/usr/bin/mktemp -d \"${TMPDIR:-/tmp}/pr593-a001-java-tmp.XXXXXX\")\"; cleanup(){ /bin/rm -rf -- \"$ISOLATED\"; }; trap cleanup EXIT HUP INT TERM; export TMPDIR=\"$ISOLATED\"; export JAVA_TOOL_OPTIONS=\"-Djava.io.tmpdir=$ISOLATED\"; /bin/bash -x tools/release/prove-mac-candidate.sh 0.0.0"],proofClass:'TEST',candidateEvidence:true,predicateClass:'SOURCE_OWNER_REPOSITORY',predicateRef:'predicate.vexlife-prod.pr593.response-readiness-projection-harness.mac-candidate',timeoutMs:1800000}
 ],
 maxFiles:8,stateRef:'state.vexlife-prod.pr593.response-readiness-projection.source-ready',currentLifecycleState:'SOURCE_READY',
 currentGoal:'Prove #593 source compiles/tests and preserves separation between effective runtime mode, Working Continuity reset, and observed completed-turn provider measurement while historical runtime-evidence v1/v2/v3 stays readable.',
 remainingGoals:['If A001 PASS, run fresh Independent Assurance on #593 semantic ownership and historical evidence compatibility.','Only after SourceOwner + Assurance, form the presentation successor that composes #550 wording into existing ResponseProgress and regenerates Screen Lens once.'],
 onPass:{lifecycleState:'ASSURANCE_PENDING',nextFunction:'F13.RUN_FRESH_ASSURANCE',nextOwner:'Operations',whyThisIsNext:'The semantic projection source is repository-proved; fresh Assurance must verify that it projects existing runtime, continuity and turn-evidence owners without creating a second readiness truth or certifying invisible provider state.'},
 resultFilename:'RETURN-VEXLIFE-PR593-RESPONSE-READINESS-PROJECTION-SOURCE-OWNER-A001.zip'
};

const authorityEnvelope={authoritySchemaRef:'vextreme.effect-transaction/v1',semanticAdmissionRef:'github.pull.vexlife-prod.587',transactionRef:'transaction.vexlife-prod.pr593.response-readiness-projection.source-owner.a001',transactionVersion:1,allowedEffectRefs:['effect.vexrelay.execute'],heldEffectRefs:['effect.vexlife-prod.source-mutation','effect.vexlife-prod.approval','effect.vexlife-prod.ready','effect.vexlife-prod.merge','effect.vexlife-prod.product-lifecycle','effect.vexlife-prod.model-runtime']};

const outerTaskTemplate={
 schemaVersion:'vextreme.vexlocalbridge-task-manifest/v1',artifactClass:'EXECUTABLE_RELAY',
 taskRef:'task.vexlife-prod.pr593.response-readiness-projection.source-owner.a001.carrier',
 attemptRef:`attempt.vexlife-prod.pr593.response-readiness-projection.source-owner.a001.carrier.${suffix}`,
 packageRef:`package.vexlife-prod.pr593.response-readiness-projection.source-owner.a001.carrier.${suffix}`,
 requestRef:`request.vexlife-prod.pr593.response-readiness-projection.source-owner.a001.carrier.${suffix}`,
 correlationRef:`correlation.vexlife-prod.pr593.response-readiness-projection.source-owner.a001.carrier.${suffix}`,
 taskStorageKey:'vexlife-prod-pr593-response-readiness-projection-source-owner-a001',attemptStorageKey:`attempt-a001-${suffix}`,
 requestingRoleRef:'Operations[PR593-SOURCE-OWNER]',requestingInstanceRef:`instance.vexlife-prod.pr593.source-owner.carrier.a001.${suffix}`,
 parentOperationsRef:'github.issue.vexlife-prod.514',returnRouteRef:'github.pull.vexlife-prod.587',
 hostProfileRef:'macos-victor',repositoryExecutionProfileRefOrNull:'profile.repository.vextreme-sdk.macos-v1',
 repositoryRefOrNull:'vgong24/Vextreme-SDK',baseRefOrNull:SDK_HEAD,headRefOrNull:SDK_HEAD,treeRefOrNull:SDK_TREE,branchRefOrNull:'main',claimRefOrNull:null,exactPathRefs:[],
 authorityBinding:{authoritySchemaRef:authorityEnvelope.authoritySchemaRef,authorityEnvelopePath:'authority/effect.json',authorityEnvelopeSha256:'0'.repeat(64),semanticAdmissionRef:authorityEnvelope.semanticAdmissionRef,transactionRef:authorityEnvelope.transactionRef,transactionVersion:1,allowedEffectRefs:[...authorityEnvelope.allowedEffectRefs],heldEffectRefs:[...authorityEnvelope.heldEffectRefs]},
 payload:{payloadClass:native.VEXRELAY_ARCHIVE_PAYLOAD_CLASS,payloadRuntime:native.VEXRELAY_ARCHIVE_RUNTIME,payloadPath:'payload/inner-vexrelay.zip',payloadSha256:'0'.repeat(64),typedArgv:[],immutableInputs:[{path:'inputs/vexrelay-binding.json',sha256:'0'.repeat(64)}]},
 executionStateAtFormation:'TASK_PREPARED',singleUseNonce:`nonce-pr593-carrier-a001-${suffix}-${crypto.randomBytes(16).toString('hex')}`,
 stopConditions:['STOP_ON_INNER_TASK_IDENTITY_DRIFT','STOP_ON_EXACT_SOURCE_DRIFT','STOP_ON_PROOF_FAILURE','STOP_ON_CANONICAL_RETURN_CONSUMER_FAILURE'],archiveDigestBinding:'FILENAME_SHA256'
};

fs.mkdirSync(outputRoot,{recursive:true});
const formed=await formVexRelayTaskArchiveCarrier({recipe:'ExactHeadSourceOwnerProof',seed,stage,outerTaskTemplate,authorityEnvelope,outputDirectory:outputRoot,outputStem:'VEXLIFE-PR593-RESPONSE-READINESS-PROJECTION-SOURCE-OWNER-A001-TASK',ttlMs:60*60*1000,archiveAdapter:adapter.archive});
const capsuleRoot=expectedCapsuleRoot(formed.outer.finalZip,formed.outer.digest,profile.zipRelayCapsule);
process.stdout.write(JSON.stringify({finalZip:formed.outer.finalZip,digest:formed.outer.digest,capsuleRoot,profilePath,outerTaskRef:formed.outer.task.taskRef,innerTaskRef:formed.inner.manifest.taskRef,innerAttemptRef:formed.inner.manifest.attemptRef,innerArchiveSha256:formed.inner.sha256,candidate:{base:BASE,head:HEAD,tree:TREE,branch:BRANCH},formationEvidence:formed.outer.formationEvidence},null,2)+'\n');
