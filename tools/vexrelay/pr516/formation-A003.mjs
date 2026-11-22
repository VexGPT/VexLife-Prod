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

const BASE='e1c18be71f73c8ec51818a960c0d9b50b94efc63';
const HEAD='aafda15b8d4fd75ce992def4c18561e5a320dc20';
const TREE='101e6bcc70e3b654ed7a95960332711bb6700a71';
const BRANCH='VXG-112226-chatgpt-initial-boot-prefix-a001';
const CHANGED=['src/jvmMain/kotlin/vexlife/companion/DirectPromptContext.kt','src/jvmTest/kotlin/vexlife/companion/DirectPromptContextTest.kt'];
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
 taskRef:'task.vexlife-prod.pr516.initial-boot-prefix.source-owner.a003',
 attemptRef:`attempt.vexlife-prod.pr516.initial-boot-prefix.source-owner.a003.${suffix}`,
 requestRef:`request.vexlife-prod.pr516.initial-boot-prefix.source-owner.a003.${suffix}`,
 correlationRef:`correlation.vexlife-prod.pr516.initial-boot-prefix.source-owner.a003.${suffix}`,
 packageRef:`package.vexlife-prod.pr516.initial-boot-prefix.source-owner.a003.${suffix}`,
 original:{originalIntent:'Prove exact initial boot provider prefix before any provider warmup.',originalGoal:'Close Stage A with focused exact-prefix and full JVM regression evidence.',userProtectedIntent:'Preserve #499/#504/#510 lessons and do not widen Product/runtime authority.',sourceRefs:['github.issue.vexlife-prod.514','github.pull.vexlife-prod.516']},
 lane:{entryClass:'FOUNDATION_DELTA',scopeMode:'STAGE_BOUNDED',laneRef:'lane.vexlife-prod.warm-boot.initial-prefix',parentRootRef:'github.issue.vexlife-prod.514',workRef:'github.pull.vexlife-prod.516',claimRef:'claim.vexlife-prod.pr516.initial-boot-prefix.source-owner',returnRouteRef:'return.vexlife-prod.pr516.initial-boot-prefix.source-owner.a003',ownerRef:'owner.vexlife-prod.pr516.initial-boot-prefix',rootHubRef:'hub.vexlife-prod.warm-boot'},
 role:{activeRole:'CODER',roleRef:'role.coder.vexlife-prod.pr516',instanceRef:`instance.vexlife-prod.pr516.source-owner.a003.${suffix}`,occupancyRef:'occupancy.vexlife-prod.pr516.initial-boot-prefix',providerBindingRef:'provider.github.vexlife-prod.pr516',threadRef:'thread.vexlife-prod.pr516.initial-boot-prefix',authorityEnvelopeRef:'authority-envelope.vexlife-prod.pr516.source-owner'},
 capabilities:{platformGitHubRead:'UNAVAILABLE',platformGitHubWrite:'UNAVAILABLE'},
 execution:{hostProfile:'macos-victor',executionSurface:'MAC_ZIP_RUN_COMMAND',singleUseNonce:`nonce-pr516-a003-${suffix}-${crypto.randomBytes(16).toString('hex')}`,duplicateAttemptMarker:'marker.vexlife-prod.pr516.initial-boot-prefix.source-owner.a003',duplicateProtectionMode:'OUTER_RELAY_BOUND',formedAt:now.toISOString(),expiresAtOrNull:expiry.toISOString()},
 workspace:{mode:'RUN_ROOT',projectRef:'project.vexlife-prod.pr516.source-owner',projectPath:'workspace',createRoot:true,createProjectPath:true},
 repositories:[{repositoryRef:'primary',mode:'CLONE_IN_WORKSPACE',nameWithOwner:'VexGPT/VexLife-Prod',localPath:'vexlife-prod',remote:'origin',baseBranch:'main',acceptedBase:BASE,expectedHead:HEAD,checkoutRef:HEAD,branch:BRANCH,expectedRemoteUrl:null,cloneTransport:'GITHUB_GH',githubHostname:'github.com'}],
 authority:{authorityRef:'authority.vexlife-prod.pr516.source-owner',authorityMode:'EXACT_STAGE_EFFECTS',grantedEffects:effects,conditionalEffects:[],consumedEffects:[],remainingEffects:effects,heldEffects:['WRITE_SOURCE','CREATE_BRANCH','CREATE_COMMIT','PUSH_BRANCH','CREATE_PULL_REQUEST','UPDATE_COORDINATION_METADATA','APPROVE_EXACT_HEAD','MARK_READY','ORDINARY_MERGE'],repositoryMembranes:[{repositoryRef:'primary',allowedPaths:CHANGED}],allowedExecutables:['/bin/bash','/usr/bin/git','git','gh','node'],allowedEnvironment:[],remoteObservationOwner:'LocalRelay',allowMutationLoop:false,stopConditions:['STOP_ON_UNKNOWN','STOP_ON_PATH_ESCAPE','STOP_ON_AUTH_BLOCK','STOP_ON_TEST_FAILURE','STOP_ON_EXACT_HEAD_OR_TREE_DRIFT']},
 source:{sourceSetRef:'source-set.vexlife-prod.pr516.initial-boot-prefix',sourceVersionRef:`source-version.vexlife-prod.pr516.${HEAD}`,overlapEvidenceRef:'overlap.vexlife-prod.pr516.initial-boot-prefix.a003',activeConflictState:'NONE',candidateState:'EXACT',candidateHead:HEAD,candidateTree:TREE,expectedChangedPaths:CHANGED.map(p=>({repositoryRef:'primary',path:p})),intentionallyUnchangedAuthorizedPaths:[]},
 policy:{maxOuterReturnFiles:10,maxSelectedSourceEntries:10,maxSelectedSourceBytes:1000000,maxDiffBytes:5000000,requireRacePreflightBeforeMutation:false,requireLocalValidationBeforePublish:false,requireInputPackageManifest:false,signoffCommit:true,draftPullRequest:true}
};

const stage={repositoryRef:'primary',expectedBase:BASE,expectedHead:HEAD,expectedTree:TREE,branch:BRANCH,
 proofCommands:[
  {command:'/bin/bash',args:['./gradlew','jvmTest','--tests','vexlife.companion.DirectPromptContextTest.initialBootPrefixExactlyMatchesTheRealGenesisTurnLeadingSystemMessage'],proofClass:'TEST',candidateEvidence:true,predicateClass:'SOURCE_OWNER_BEHAVIOR',predicateRef:'predicate.vexlife-prod.pr516.initial-boot-prefix.exact-provider-message',timeoutMs:300000},
  {command:'/bin/bash',args:['./gradlew','jvmTest'],proofClass:'TEST',candidateEvidence:true,predicateClass:'SOURCE_OWNER_REPOSITORY',predicateRef:'predicate.vexlife-prod.pr516.jvm-regression',timeoutMs:1200000}
 ],
 maxFiles:2,stateRef:'state.vexlife-prod.pr516.source-ready',currentLifecycleState:'SOURCE_READY',
 currentGoal:'Prove exact Warm Boot Stage A source and JVM regression safety.',
 remainingGoals:['Run fresh Independent Assurance on the exact proved head.','Only after Stage A acceptance, form Stage B provider preparation and visible Prompt Preparation State.'],
 onPass:{lifecycleState:'ASSURANCE_PENDING',nextFunction:'F13.RUN_FRESH_ASSURANCE',nextOwner:'Operations',whyThisIsNext:'Exact-prefix SourceOwner proof is complete; Independent Assurance remains separate before provider warmup.'},
 resultFilename:'RETURN-VEXLIFE-PR516-INITIAL-BOOT-PREFIX-SOURCE-OWNER-A003.zip'
};

const authorityEnvelope={authoritySchemaRef:'vextreme.effect-transaction/v1',semanticAdmissionRef:'github.issue.vexlife-prod.514',transactionRef:'transaction.vexlife-prod.pr516.initial-boot-prefix.source-owner.a003',transactionVersion:1,allowedEffectRefs:['effect.vexrelay.execute'],heldEffectRefs:['effect.vexlife-prod.source-mutation','effect.vexlife-prod.approval','effect.vexlife-prod.ready','effect.vexlife-prod.merge','effect.vexlife-prod.product-lifecycle','effect.vexlife-prod.model-runtime']};

const outerTaskTemplate={
 schemaVersion:'vextreme.vexlocalbridge-task-manifest/v1',artifactClass:'EXECUTABLE_RELAY',
 taskRef:'task.vexlife-prod.pr516.initial-boot-prefix.source-owner.a003.carrier',
 attemptRef:`attempt.vexlife-prod.pr516.initial-boot-prefix.source-owner.a003.carrier.${suffix}`,
 packageRef:`package.vexlife-prod.pr516.initial-boot-prefix.source-owner.a003.carrier.${suffix}`,
 requestRef:`request.vexlife-prod.pr516.initial-boot-prefix.source-owner.a003.carrier.${suffix}`,
 correlationRef:`correlation.vexlife-prod.pr516.initial-boot-prefix.source-owner.a003.carrier.${suffix}`,
 taskStorageKey:'vexlife-prod-pr516-initial-boot-prefix-source-owner-a003',attemptStorageKey:`attempt-a003-${suffix}`,
 requestingRoleRef:'Operations[PR516-SOURCE-OWNER]',requestingInstanceRef:`instance.vexlife-prod.pr516.source-owner.carrier.a003.${suffix}`,
 parentOperationsRef:'github.issue.vexlife-prod.514',returnRouteRef:'github.pull.vexlife-prod.516',
 hostProfileRef:'macos-victor',repositoryExecutionProfileRefOrNull:'profile.repository.vextreme-sdk.macos-v1',
 repositoryRefOrNull:'vgong24/Vextreme-SDK',baseRefOrNull:SDK_HEAD,headRefOrNull:SDK_HEAD,treeRefOrNull:SDK_TREE,branchRefOrNull:'main',claimRefOrNull:null,exactPathRefs:[],
 authorityBinding:{authoritySchemaRef:authorityEnvelope.authoritySchemaRef,authorityEnvelopePath:'authority/effect.json',authorityEnvelopeSha256:'0'.repeat(64),semanticAdmissionRef:authorityEnvelope.semanticAdmissionRef,transactionRef:authorityEnvelope.transactionRef,transactionVersion:1,allowedEffectRefs:[...authorityEnvelope.allowedEffectRefs],heldEffectRefs:[...authorityEnvelope.heldEffectRefs]},
 payload:{payloadClass:native.VEXRELAY_ARCHIVE_PAYLOAD_CLASS,payloadRuntime:native.VEXRELAY_ARCHIVE_RUNTIME,payloadPath:'payload/inner-vexrelay.zip',payloadSha256:'0'.repeat(64),typedArgv:[],immutableInputs:[{path:'inputs/vexrelay-binding.json',sha256:'0'.repeat(64)}]},
 executionStateAtFormation:'TASK_PREPARED',singleUseNonce:`nonce-pr516-carrier-a003-${suffix}-${crypto.randomBytes(16).toString('hex')}`,
 stopConditions:['STOP_ON_INNER_TASK_IDENTITY_DRIFT','STOP_ON_EXACT_SOURCE_DRIFT','STOP_ON_PROOF_FAILURE','STOP_ON_CANONICAL_RETURN_CONSUMER_FAILURE'],archiveDigestBinding:'FILENAME_SHA256'
};

fs.mkdirSync(outputRoot,{recursive:true});
const formed=await formVexRelayTaskArchiveCarrier({recipe:'ExactHeadSourceOwnerProof',seed,stage,outerTaskTemplate,authorityEnvelope,outputDirectory:outputRoot,outputStem:'VEXLIFE-PR516-INITIAL-BOOT-PREFIX-SOURCE-OWNER-A003-TASK',ttlMs:60*60*1000,archiveAdapter:adapter.archive});
const capsuleRoot=expectedCapsuleRoot(formed.outer.finalZip,formed.outer.digest,profile.zipRelayCapsule);
process.stdout.write(JSON.stringify({finalZip:formed.outer.finalZip,digest:formed.outer.digest,capsuleRoot,profilePath,outerTaskRef:formed.outer.task.taskRef,innerTaskRef:formed.inner.manifest.taskRef,innerAttemptRef:formed.inner.manifest.attemptRef,innerArchiveSha256:formed.inner.sha256,candidate:{base:BASE,head:HEAD,tree:TREE,branch:BRANCH},formationEvidence:formed.outer.formationEvidence},null,2)+'\n');
