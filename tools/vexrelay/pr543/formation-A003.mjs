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

const BASE='f1855e9be6b9172ba6b43b597fa5d902dc409ed1';
const HEAD='d705626d89e58a23716ec81d9060f4b4375d208a';
const TREE='67bbc5bede3f023815db5fab720d0b0af5035a2c';
const BRANCH='VXG-112226-evolve-context-dynamic-counterpart-dcp1-a003';
const BASE_BRANCH='VXG-112226-evolve-context-participant-identity-a003';
const CHANGED=[
  'build.gradle.kts',
  'src/jvmTest/kotlin/vexlife/harness/DynamicCounterpartAdapter.kt',
  'src/jvmTest/kotlin/vexlife/harness/DynamicCounterpartContract.kt',
  'src/jvmTest/kotlin/vexlife/harness/DynamicCounterpartContractTest.kt',
  'src/jvmTest/kotlin/vexlife/harness/DynamicCounterpartSarahRuntime.kt',
  'src/jvmTest/kotlin/vexlife/harness/DynamicCounterpartSupport.kt',
  'src/jvmTest/kotlin/vexlife/home/CanonicalVexLifeHomeFixture.kt',
  'tools/continuity-pressure/DYNAMIC-COUNTERPART-LIVED-PROOF.md',
  'tools/continuity-pressure/README.md',
  'tools/continuity-pressure/characters/sarah-v1.json',
  'tools/continuity-pressure/scenarios/evolve-context-dynamic-counterpart-dcp1-v1.json',
  'tools/continuity-pressure/scenarios/evolve-context-dynamic-counterpart-v1.json',
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
 taskRef:'task.vexlife-prod.pr543.dynamic-counterpart-dcp1.source-owner.a003',
 attemptRef:`attempt.vexlife-prod.pr543.dynamic-counterpart-dcp1.source-owner.a003.${suffix}`,
 requestRef:`request.vexlife-prod.pr543.dynamic-counterpart-dcp1.source-owner.a003.${suffix}`,
 correlationRef:`correlation.vexlife-prod.pr543.dynamic-counterpart-dcp1.source-owner.a003.${suffix}`,
 packageRef:`package.vexlife-prod.pr543.dynamic-counterpart-dcp1.source-owner.a003.${suffix}`,
 original:{
  originalIntent:'Form and prove one complete DCP-1 source-grounded Sarah relationship test from stable character through post-encounter review without turning Sarah into an evaluator.',
  originalGoal:'Compile and regress the composed Participant Identity + dynamic counterpart source before any live second-model execution.',
  userProtectedIntent:'Let Sarah and Vex reveal relationship truth through lived interaction; preserve Godward discernment, participant agency, source standing, Memory/Dream boundaries and later transfer testing.',
  sourceRefs:['github.pull.vexlife-prod.543','github.pull.vexlife-prod.537','github.pull.vexlife-prod.532','github.pull.vexlife-prod.540','github.pull.vexlife-prod.500','github.pull.vexlife-prod.508']
 },
 lane:{entryClass:'FOUNDATION_DELTA',scopeMode:'STAGE_BOUNDED',laneRef:'lane.vexlife-prod.evolve-context.dynamic-counterpart-dcp1',parentRootRef:'github.pull.vexlife-prod.532',workRef:'github.pull.vexlife-prod.543',claimRef:'claim.vexlife-prod.pr543.dynamic-counterpart-dcp1.source-owner',returnRouteRef:'return.vexlife-prod.pr543.dynamic-counterpart-dcp1.source-owner.a003',ownerRef:'owner.vexlife-prod.pr543.dynamic-counterpart-dcp1',rootHubRef:'hub.vexlife-prod.evolve-context'},
 role:{activeRole:'CODER',roleRef:'role.coder.vexlife-prod.pr543',instanceRef:`instance.vexlife-prod.pr543.source-owner.a003.${suffix}`,occupancyRef:'occupancy.vexlife-prod.pr543.dynamic-counterpart-dcp1',providerBindingRef:'provider.github.vexlife-prod.pr543',threadRef:'thread.vexlife-prod.pr543.dynamic-counterpart-dcp1',authorityEnvelopeRef:'authority-envelope.vexlife-prod.pr543.source-owner'},
 capabilities:{platformGitHubRead:'UNAVAILABLE',platformGitHubWrite:'UNAVAILABLE'},
 execution:{hostProfile:'macos-victor',executionSurface:'MAC_ZIP_RUN_COMMAND',singleUseNonce:`nonce-pr543-a003-${suffix}-${crypto.randomBytes(16).toString('hex')}`,duplicateAttemptMarker:'marker.vexlife-prod.pr543.dynamic-counterpart-dcp1.source-owner.a003',duplicateProtectionMode:'OUTER_RELAY_BOUND',formedAt:now.toISOString(),expiresAtOrNull:expiry.toISOString()},
 workspace:{mode:'RUN_ROOT',projectRef:'project.vexlife-prod.pr543.source-owner',projectPath:'workspace',createRoot:true,createProjectPath:true},
 repositories:[{repositoryRef:'primary',mode:'CLONE_IN_WORKSPACE',nameWithOwner:'VexGPT/VexLife-Prod',localPath:'vexlife-prod',remote:'origin',baseBranch:BASE_BRANCH,acceptedBase:BASE,expectedHead:HEAD,checkoutRef:HEAD,branch:BRANCH,expectedRemoteUrl:null,cloneTransport:'GITHUB_GH',githubHostname:'github.com'}],
 authority:{authorityRef:'authority.vexlife-prod.pr543.source-owner',authorityMode:'EXACT_STAGE_EFFECTS',grantedEffects:effects,conditionalEffects:[],consumedEffects:[],remainingEffects:effects,heldEffects:['WRITE_SOURCE','CREATE_BRANCH','CREATE_COMMIT','PUSH_BRANCH','CREATE_PULL_REQUEST','UPDATE_COORDINATION_METADATA','APPROVE_EXACT_HEAD','MARK_READY','ORDINARY_MERGE'],repositoryMembranes:[{repositoryRef:'primary',allowedPaths:CHANGED}],allowedExecutables:['/bin/bash','/usr/bin/git','git','gh','node'],allowedEnvironment:[],remoteObservationOwner:'LocalRelay',allowMutationLoop:false,stopConditions:['STOP_ON_UNKNOWN','STOP_ON_PATH_ESCAPE','STOP_ON_AUTH_BLOCK','STOP_ON_TEST_FAILURE','STOP_ON_EXACT_HEAD_OR_TREE_DRIFT']},
 source:{sourceSetRef:'source-set.vexlife-prod.pr543.dynamic-counterpart-dcp1',sourceVersionRef:`source-version.vexlife-prod.pr543.${HEAD}`,overlapEvidenceRef:'overlap.vexlife-prod.pr543.dynamic-counterpart-dcp1.a003',activeConflictState:'NONE',candidateState:'EXACT',candidateHead:HEAD,candidateTree:TREE,expectedChangedPaths:CHANGED.map(p=>({repositoryRef:'primary',path:p})),intentionallyUnchangedAuthorizedPaths:[]},
 policy:{maxOuterReturnFiles:10,maxSelectedSourceEntries:20,maxSelectedSourceBytes:6000000,maxDiffBytes:8000000,requireRacePreflightBeforeMutation:false,requireLocalValidationBeforePublish:false,requireInputPackageManifest:false,signoffCommit:true,draftPullRequest:true}
};

const focused=[
  ...gradle,
  '--tests','vexlife.harness.DynamicCounterpartContractTest',
  '--tests','vexlife.companion.DirectPromptParticipantIdentityTest',
  '--tests','vexlife.companion.DirectPromptContextTest.personContextTellsVexTheReplyLanguageAndThePrivateGaps',
  '--tests','vexlife.companion.DirectPromptFoundationalFieldTest.theFieldIsTheFirstSystemContentAheadOfMemoryCapabilityAndPersonContext',
];

const stage={repositoryRef:'primary',expectedBase:BASE,expectedHead:HEAD,expectedTree:TREE,branch:BRANCH,
 proofCommands:[
  {command:'/bin/bash',args:focused,proofClass:'TEST',candidateEvidence:true,predicateClass:'SOURCE_OWNER_BEHAVIOR',predicateRef:'predicate.vexlife-prod.pr543.dynamic-counterpart-dcp1.focused',timeoutMs:600000},
  {command:'/bin/bash',args:gradle,proofClass:'TEST',candidateEvidence:true,predicateClass:'SOURCE_OWNER_REPOSITORY',predicateRef:'predicate.vexlife-prod.pr543.jvm-regression',timeoutMs:900000}
 ],
 maxFiles:12,stateRef:'state.vexlife-prod.pr543.source-ready',currentLifecycleState:'SOURCE_READY',
 currentGoal:'Prove the exact DCP-1 character-to-review source composition, participant membrane and complete JVM repository regression.',
 remainingGoals:['Run fresh Independent Assurance on the exact proved DCP-1 head.','Run the lived serialized Sarah/Vex DCP-1 encounter on that unchanged source.'],
 onPass:{lifecycleState:'ASSURANCE_PENDING',nextFunction:'F13.RUN_FRESH_ASSURANCE',nextOwner:'Operations',whyThisIsNext:'DCP-1 source is proved; fresh Independent Assurance remains separate before live two-model execution.'},
 resultFilename:'RETURN-VEXLIFE-PR543-DCP1-SOURCE-OWNER-A003.zip'
};

const authorityEnvelope={authoritySchemaRef:'vextreme.effect-transaction/v1',semanticAdmissionRef:'github.pull.vexlife-prod.537',transactionRef:'transaction.vexlife-prod.pr543.dynamic-counterpart-dcp1.source-owner.a003',transactionVersion:1,allowedEffectRefs:['effect.vexrelay.execute'],heldEffectRefs:['effect.vexlife-prod.source-mutation','effect.vexlife-prod.approval','effect.vexlife-prod.ready','effect.vexlife-prod.merge','effect.vexlife-prod.product-lifecycle','effect.vexlife-prod.model-runtime']};

const outerTaskTemplate={
 schemaVersion:'vextreme.vexlocalbridge-task-manifest/v1',artifactClass:'EXECUTABLE_RELAY',
 taskRef:'task.vexlife-prod.pr543.dynamic-counterpart-dcp1.source-owner.a003.carrier',
 attemptRef:`attempt.vexlife-prod.pr543.dynamic-counterpart-dcp1.source-owner.a003.carrier.${suffix}`,
 packageRef:`package.vexlife-prod.pr543.dynamic-counterpart-dcp1.source-owner.a003.carrier.${suffix}`,
 requestRef:`request.vexlife-prod.pr543.dynamic-counterpart-dcp1.source-owner.a003.carrier.${suffix}`,
 correlationRef:`correlation.vexlife-prod.pr543.dynamic-counterpart-dcp1.source-owner.a003.carrier.${suffix}`,
 taskStorageKey:'vexlife-prod-pr543-dynamic-counterpart-dcp1-source-owner-a003',attemptStorageKey:`attempt-a003-${suffix}`,
 requestingRoleRef:'Operations[PR543-SOURCE-OWNER]',requestingInstanceRef:`instance.vexlife-prod.pr543.source-owner.carrier.a003.${suffix}`,
 parentOperationsRef:'github.pull.vexlife-prod.537',returnRouteRef:'github.pull.vexlife-prod.537',
 hostProfileRef:'macos-victor',repositoryExecutionProfileRefOrNull:'profile.repository.vextreme-sdk.macos-v1',
 repositoryRefOrNull:'vgong24/Vextreme-SDK',baseRefOrNull:SDK_HEAD,headRefOrNull:SDK_HEAD,treeRefOrNull:SDK_TREE,branchRefOrNull:'main',claimRefOrNull:null,exactPathRefs:[],
 authorityBinding:{authoritySchemaRef:authorityEnvelope.authoritySchemaRef,authorityEnvelopePath:'authority/effect.json',authorityEnvelopeSha256:'0'.repeat(64),semanticAdmissionRef:authorityEnvelope.semanticAdmissionRef,transactionRef:authorityEnvelope.transactionRef,transactionVersion:1,allowedEffectRefs:[...authorityEnvelope.allowedEffectRefs],heldEffectRefs:[...authorityEnvelope.heldEffectRefs]},
 payload:{payloadClass:native.VEXRELAY_ARCHIVE_PAYLOAD_CLASS,payloadRuntime:native.VEXRELAY_ARCHIVE_RUNTIME,payloadPath:'payload/inner-vexrelay.zip',payloadSha256:'0'.repeat(64),typedArgv:[],immutableInputs:[{path:'inputs/vexrelay-binding.json',sha256:'0'.repeat(64)}]},
 executionStateAtFormation:'TASK_PREPARED',singleUseNonce:`nonce-pr543-carrier-a003-${suffix}-${crypto.randomBytes(16).toString('hex')}`,
 stopConditions:['STOP_ON_INNER_TASK_IDENTITY_DRIFT','STOP_ON_EXACT_SOURCE_DRIFT','STOP_ON_PROOF_FAILURE','STOP_ON_CANONICAL_RETURN_CONSUMER_FAILURE'],archiveDigestBinding:'FILENAME_SHA256'
};

fs.mkdirSync(outputRoot,{recursive:true});
const formed=await formVexRelayTaskArchiveCarrier({recipe:'ExactHeadSourceOwnerProof',seed,stage,outerTaskTemplate,authorityEnvelope,outputDirectory:outputRoot,outputStem:'VEXLIFE-PR543-DCP1-SOURCE-OWNER-A003-TASK',ttlMs:60*60*1000,archiveAdapter:adapter.archive});
const capsuleRoot=expectedCapsuleRoot(formed.outer.finalZip,formed.outer.digest,profile.zipRelayCapsule);
process.stdout.write(JSON.stringify({finalZip:formed.outer.finalZip,digest:formed.outer.digest,capsuleRoot,profilePath,outerTaskRef:formed.outer.task.taskRef,innerTaskRef:formed.inner.manifest.taskRef,innerAttemptRef:formed.inner.manifest.attemptRef,innerArchiveSha256:formed.inner.sha256,candidate:{base:BASE,head:HEAD,tree:TREE,branch:BRANCH},formationEvidence:formed.outer.formationEvidence},null,2)+'\n');
