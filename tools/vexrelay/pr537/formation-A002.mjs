import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { createRequire } from 'node:module';
import { pathToFileURL } from 'node:url';

const [sdkRootArg, outputRootArg] = process.argv.slice(2);
if (!sdkRootArg || !outputRootArg) throw new Error('usage: node formation-A002.mjs <sdk-root> <output-root>');
const sdkRoot=path.resolve(sdkRootArg), outputRoot=path.resolve(outputRootArg);
const require=createRequire(import.meta.url);
const {formVexRelayTaskArchiveCarrier}=await import(pathToFileURL(path.join(sdkRoot,'lib/vexlocalbridge/vexrelay-task-archive-carrier-formation.mjs')).href);
const {createHostAdapter}=require(path.join(sdkRoot,'lib/vexlocalbridge/mac-runner.js'));
const {expectedCapsuleRoot}=require(path.join(sdkRoot,'lib/vexlocalbridge/cli.js'));
const native=require(path.join(sdkRoot,'lib/vexlocalbridge/vexrelay-task-archive.js'));

const BASE='0566b1eed876a28c2fcc6586743ae1f4af7eaecd';
const HEAD='a46d00760c6a987fe1d0ed4cfd2da7fabcc8887b';
const TREE='5711b5e9b3abb5c80b7a1ed489ee41d99bbe724f';
const BRANCH='VXG-112226-evolve-context-participant-identity-a001';
const CHANGED=[
  'src/jvmMain/kotlin/vexlife/companion/DirectPromptContext.kt',
  'src/jvmTest/kotlin/vexlife/companion/DirectPromptParticipantIdentityTest.kt',
];
const SDK_HEAD='6f8f6f50467f5df5a542713f7dc25b82db4ac8fa';
const SDK_TREE='96795640b01f1969da980f52d5a8ec136a8c44aa';
const profilePath=path.join(sdkRoot,'docs/private-continuity/vexlocalbridge/profiles/macos-victor-vextreme-sdk.json');
const profile=JSON.parse(fs.readFileSync(profilePath,'utf8'));
const adapter=createHostAdapter(profile);
const suffix=crypto.randomBytes(10).toString('hex');
const now=new Date(), expiry=new Date(now.getTime()+60*60*1000);
const effects=['CHECK_AUTH','READ_REPOSITORY','CHECK_STATE','QUERY_PROVIDER','RUN_LOCAL_COMMAND','FORM_RESULT'];
const gradle=['./gradlew','--no-daemon','-Dorg.gradle.jvmargs=-Xmx4g','-Dkotlin.daemon.jvm.options=-Xmx4g','jvmTest'];

const seed={
 schemaVersion:'vex.relay.task/v2',continuity:'[VXG RealForever]',
 taskRef:'task.vexlife-prod.pr537.participant-identity.source-owner.a002',
 attemptRef:`attempt.vexlife-prod.pr537.participant-identity.source-owner.a002.${suffix}`,
 requestRef:`request.vexlife-prod.pr537.participant-identity.source-owner.a002.${suffix}`,
 correlationRef:`correlation.vexlife-prod.pr537.participant-identity.source-owner.a002.${suffix}`,
 packageRef:`package.vexlife-prod.pr537.participant-identity.source-owner.a002.${suffix}`,
 original:{
  originalIntent:'Make the current Conversation participant legible to Vex without inventing a human-readable name.',
  originalGoal:'Prove exact speakerRef-derived participant identity, explicit UNKNOWN name standing, and one source-backed synthetic name claim without widening durable receipt schema.',
  userProtectedIntent:'Preserve truthful person/relationship identity for ordinary Vex experience and dynamic counterpart testing; do not guess identity or collapse multi-person relationships.',
  sourceRefs:['github.pull.vexlife-prod.537','github.pull.vexlife-prod.532','github.pull.vexlife-prod.500','github.pull.vexlife-prod.508']
 },
 lane:{entryClass:'FOUNDATION_DELTA',scopeMode:'STAGE_BOUNDED',laneRef:'lane.vexlife-prod.evolve-context.participant-identity',parentRootRef:'github.pull.vexlife-prod.500',workRef:'github.pull.vexlife-prod.537',claimRef:'claim.vexlife-prod.pr537.participant-identity.source-owner',returnRouteRef:'return.vexlife-prod.pr537.participant-identity.source-owner.a002',ownerRef:'owner.vexlife-prod.pr537.participant-identity',rootHubRef:'hub.vexlife-prod.evolve-context'},
 role:{activeRole:'CODER',roleRef:'role.coder.vexlife-prod.pr537',instanceRef:`instance.vexlife-prod.pr537.source-owner.a002.${suffix}`,occupancyRef:'occupancy.vexlife-prod.pr537.participant-identity',providerBindingRef:'provider.github.vexlife-prod.pr537',threadRef:'thread.vexlife-prod.pr537.participant-identity',authorityEnvelopeRef:'authority-envelope.vexlife-prod.pr537.source-owner'},
 capabilities:{platformGitHubRead:'UNAVAILABLE',platformGitHubWrite:'UNAVAILABLE'},
 execution:{hostProfile:'macos-victor',executionSurface:'MAC_ZIP_RUN_COMMAND',singleUseNonce:`nonce-pr537-a002-${suffix}-${crypto.randomBytes(16).toString('hex')}`,duplicateAttemptMarker:'marker.vexlife-prod.pr537.participant-identity.source-owner.a002',duplicateProtectionMode:'OUTER_RELAY_BOUND',formedAt:now.toISOString(),expiresAtOrNull:expiry.toISOString()},
 workspace:{mode:'RUN_ROOT',projectRef:'project.vexlife-prod.pr537.source-owner',projectPath:'workspace',createRoot:true,createProjectPath:true},
 repositories:[{repositoryRef:'primary',mode:'CLONE_IN_WORKSPACE',nameWithOwner:'VexGPT/VexLife-Prod',localPath:'vexlife-prod',remote:'origin',baseBranch:'main',acceptedBase:BASE,expectedHead:HEAD,checkoutRef:HEAD,branch:BRANCH,expectedRemoteUrl:null,cloneTransport:'GITHUB_GH',githubHostname:'github.com'}],
 authority:{authorityRef:'authority.vexlife-prod.pr537.source-owner',authorityMode:'EXACT_STAGE_EFFECTS',grantedEffects:effects,conditionalEffects:[],consumedEffects:[],remainingEffects:effects,heldEffects:['WRITE_SOURCE','CREATE_BRANCH','CREATE_COMMIT','PUSH_BRANCH','CREATE_PULL_REQUEST','UPDATE_COORDINATION_METADATA','APPROVE_EXACT_HEAD','MARK_READY','ORDINARY_MERGE'],repositoryMembranes:[{repositoryRef:'primary',allowedPaths:CHANGED}],allowedExecutables:['/bin/bash','/usr/bin/git','git','gh','node'],allowedEnvironment:[],remoteObservationOwner:'LocalRelay',allowMutationLoop:false,stopConditions:['STOP_ON_UNKNOWN','STOP_ON_PATH_ESCAPE','STOP_ON_AUTH_BLOCK','STOP_ON_TEST_FAILURE','STOP_ON_EXACT_HEAD_OR_TREE_DRIFT']},
 source:{sourceSetRef:'source-set.vexlife-prod.pr537.participant-identity',sourceVersionRef:`source-version.vexlife-prod.pr537.${HEAD}`,overlapEvidenceRef:'overlap.vexlife-prod.pr537.participant-identity.a002',activeConflictState:'NONE',candidateState:'EXACT',candidateHead:HEAD,candidateTree:TREE,expectedChangedPaths:CHANGED.map(p=>({repositoryRef:'primary',path:p})),intentionallyUnchangedAuthorizedPaths:[]},
 policy:{maxOuterReturnFiles:10,maxSelectedSourceEntries:10,maxSelectedSourceBytes:2000000,maxDiffBytes:4000000,requireRacePreflightBeforeMutation:false,requireLocalValidationBeforePublish:false,requireInputPackageManifest:false,signoffCommit:true,draftPullRequest:true}
};

const focused=[
  ...gradle,
  '--tests','vexlife.companion.DirectPromptParticipantIdentityTest',
];

const stage={repositoryRef:'primary',expectedBase:BASE,expectedHead:HEAD,expectedTree:TREE,branch:BRANCH,
 proofCommands:[
  {command:'/bin/bash',args:focused,proofClass:'TEST',candidateEvidence:true,predicateClass:'SOURCE_OWNER_BEHAVIOR',predicateRef:'predicate.vexlife-prod.pr537.participant-identity.focused',timeoutMs:600000},
  {command:'/bin/bash',args:gradle,proofClass:'TEST',candidateEvidence:true,predicateClass:'SOURCE_OWNER_REPOSITORY',predicateRef:'predicate.vexlife-prod.pr537.jvm-regression',timeoutMs:900000}
 ],
 maxFiles:2,stateRef:'state.vexlife-prod.pr537.source-ready',currentLifecycleState:'SOURCE_READY',
 currentGoal:'Prove current participant identity projection keeps unknown names unknown and binds any supplied name to explicit source standing.',
 remainingGoals:['Run fresh Independent Assurance on the exact proved head.','Only then let dynamic counterpart #532 consume this identity relationship in its scratch proof.'],
 onPass:{lifecycleState:'ASSURANCE_PENDING',nextFunction:'F13.RUN_FRESH_ASSURANCE',nextOwner:'Operations',whyThisIsNext:'Participant-identity source is proved; semantic Assurance remains separate before dynamic-counterpart consumption or merge.'},
 resultFilename:'RETURN-VEXLIFE-PR537-PARTICIPANT-IDENTITY-SOURCE-OWNER-A002.zip'
};

const authorityEnvelope={authoritySchemaRef:'vextreme.effect-transaction/v1',semanticAdmissionRef:'github.pull.vexlife-prod.537',transactionRef:'transaction.vexlife-prod.pr537.participant-identity.source-owner.a002',transactionVersion:1,allowedEffectRefs:['effect.vexrelay.execute'],heldEffectRefs:['effect.vexlife-prod.source-mutation','effect.vexlife-prod.approval','effect.vexlife-prod.ready','effect.vexlife-prod.merge','effect.vexlife-prod.product-lifecycle','effect.vexlife-prod.model-runtime']};

const outerTaskTemplate={
 schemaVersion:'vextreme.vexlocalbridge-task-manifest/v1',artifactClass:'EXECUTABLE_RELAY',
 taskRef:'task.vexlife-prod.pr537.participant-identity.source-owner.a002.carrier',
 attemptRef:`attempt.vexlife-prod.pr537.participant-identity.source-owner.a002.carrier.${suffix}`,
 packageRef:`package.vexlife-prod.pr537.participant-identity.source-owner.a002.carrier.${suffix}`,
 requestRef:`request.vexlife-prod.pr537.participant-identity.source-owner.a002.carrier.${suffix}`,
 correlationRef:`correlation.vexlife-prod.pr537.participant-identity.source-owner.a002.carrier.${suffix}`,
 taskStorageKey:'vexlife-prod-pr537-participant-identity-source-owner-a002',attemptStorageKey:`attempt-a002-${suffix}`,
 requestingRoleRef:'Operations[PR537-SOURCE-OWNER]',requestingInstanceRef:`instance.vexlife-prod.pr537.source-owner.carrier.a002.${suffix}`,
 parentOperationsRef:'github.pull.vexlife-prod.537',returnRouteRef:'github.pull.vexlife-prod.537',
 hostProfileRef:'macos-victor',repositoryExecutionProfileRefOrNull:'profile.repository.vextreme-sdk.macos-v1',
 repositoryRefOrNull:'vgong24/Vextreme-SDK',baseRefOrNull:SDK_HEAD,headRefOrNull:SDK_HEAD,treeRefOrNull:SDK_TREE,branchRefOrNull:'main',claimRefOrNull:null,exactPathRefs:[],
 authorityBinding:{authoritySchemaRef:authorityEnvelope.authoritySchemaRef,authorityEnvelopePath:'authority/effect.json',authorityEnvelopeSha256:'0'.repeat(64),semanticAdmissionRef:authorityEnvelope.semanticAdmissionRef,transactionRef:authorityEnvelope.transactionRef,transactionVersion:1,allowedEffectRefs:[...authorityEnvelope.allowedEffectRefs],heldEffectRefs:[...authorityEnvelope.heldEffectRefs]},
 payload:{payloadClass:native.VEXRELAY_ARCHIVE_PAYLOAD_CLASS,payloadRuntime:native.VEXRELAY_ARCHIVE_RUNTIME,payloadPath:'payload/inner-vexrelay.zip',payloadSha256:'0'.repeat(64),typedArgv:[],immutableInputs:[{path:'inputs/vexrelay-binding.json',sha256:'0'.repeat(64)}]},
 executionStateAtFormation:'TASK_PREPARED',singleUseNonce:`nonce-pr537-carrier-a002-${suffix}-${crypto.randomBytes(16).toString('hex')}`,
 stopConditions:['STOP_ON_INNER_TASK_IDENTITY_DRIFT','STOP_ON_EXACT_SOURCE_DRIFT','STOP_ON_PROOF_FAILURE','STOP_ON_CANONICAL_RETURN_CONSUMER_FAILURE'],archiveDigestBinding:'FILENAME_SHA256'
};

fs.mkdirSync(outputRoot,{recursive:true});
const formed=await formVexRelayTaskArchiveCarrier({recipe:'ExactHeadSourceOwnerProof',seed,stage,outerTaskTemplate,authorityEnvelope,outputDirectory:outputRoot,outputStem:'VEXLIFE-PR537-PARTICIPANT-IDENTITY-SOURCE-OWNER-A002-TASK',ttlMs:60*60*1000,archiveAdapter:adapter.archive});
const capsuleRoot=expectedCapsuleRoot(formed.outer.finalZip,formed.outer.digest,profile.zipRelayCapsule);
process.stdout.write(JSON.stringify({finalZip:formed.outer.finalZip,digest:formed.outer.digest,capsuleRoot,profilePath,outerTaskRef:formed.outer.task.taskRef,innerTaskRef:formed.inner.manifest.taskRef,innerAttemptRef:formed.inner.manifest.attemptRef,innerArchiveSha256:formed.inner.sha256,candidate:{base:BASE,head:HEAD,tree:TREE,branch:BRANCH},formationEvidence:formed.outer.formationEvidence},null,2)+'\n');
