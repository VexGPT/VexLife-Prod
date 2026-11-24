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

const BASE='a23b6a949d27e4b77530273a390955f0d127ad1a';
const HEAD='392d9d3727cdf07fdc2e4047231d1485b612042c';
const TREE='4df41dcb55082bcaa2d0a865bcaeadedd8673de3';
const BRANCH='VXG-112226-chatgpt-settings-followup-readiness-a001';
const BASE_BRANCH='VXG-112126-chatgpt-sent-resource-continuity-current-main-a001';
const CHANGED=[
  'localization/strings/manifest.json',
  'localization/strings/source/workspace.json'
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
 taskRef:'task.vexlife-prod.pr550.followup-readiness.source-owner.a002',
 attemptRef:`attempt.vexlife-prod.pr550.followup-readiness.source-owner.a002.${suffix}`,
 requestRef:`request.vexlife-prod.pr550.followup-readiness.source-owner.a002.${suffix}`,
 correlationRef:`correlation.vexlife-prod.pr550.followup-readiness.source-owner.a002.${suffix}`,
 packageRef:`package.vexlife-prod.pr550.followup-readiness.source-owner.a002.${suffix}`,
 original:{
  originalIntent:'Let Settings name the human performance-versus-memory tradeoff while VexLife runtime owners keep managing the cache circuitry.',
  originalGoal:'Prove the exact currentized localization-only #550 candidate: all 11 workspace languages stay complete while 4B recommendation copy reflects the already-proved #582 relationship without owning or mutating runtime recommendation policy.',
  userProtectedIntent:'A person should understand what they are choosing in Settings without being forced to reason in cache-entry jargon, while the Product keeps resource mechanics behind its rightful runtime owners.',
  sourceRefs:['github.pull.vexlife-prod.550','github.issue.vexlife-prod.514','github.pull.vexlife-prod.488','github.pull.vexlife-prod.547']
 },
 lane:{entryClass:'SAME_LANE_SUCCESSOR',scopeMode:'STAGE_BOUNDED',laneRef:'lane.vexlife-prod.settings.followup-readiness',parentRootRef:'github.issue.vexlife-prod.514',workRef:'github.pull.vexlife-prod.550',claimRef:'claim.vexlife-prod.pr550.followup-readiness.source-owner',returnRouteRef:'return.vexlife-prod.pr550.followup-readiness.source-owner.a002',ownerRef:'owner.vexlife-prod.pr550.followup-readiness',rootHubRef:'hub.vexlife-prod.warm-boot'},
 role:{activeRole:'CODER',roleRef:'role.coder.vexlife-prod.pr550',instanceRef:`instance.vexlife-prod.pr550.source-owner.a002.${suffix}`,occupancyRef:'occupancy.vexlife-prod.pr550.followup-readiness',providerBindingRef:'provider.github.vexlife-prod.pr550',threadRef:'thread.vexlife-prod.pr550.followup-readiness',authorityEnvelopeRef:'authority-envelope.vexlife-prod.pr550.source-owner'},
 capabilities:{platformGitHubRead:'UNAVAILABLE',platformGitHubWrite:'UNAVAILABLE'},
 execution:{hostProfile:'macos-victor',executionSurface:'MAC_ZIP_RUN_COMMAND',singleUseNonce:`nonce-pr550-a002-${suffix}-${crypto.randomBytes(16).toString('hex')}`,duplicateAttemptMarker:'marker.vexlife-prod.pr550.followup-readiness.source-owner.a002',duplicateProtectionMode:'OUTER_RELAY_BOUND',formedAt:now.toISOString(),expiresAtOrNull:expiry.toISOString()},
 workspace:{mode:'RUN_ROOT',projectRef:'project.vexlife-prod.pr550.source-owner',projectPath:'workspace',createRoot:true,createProjectPath:true},
 repositories:[{repositoryRef:'primary',mode:'CLONE_IN_WORKSPACE',nameWithOwner:'VexGPT/VexLife-Prod',localPath:'vexlife-prod',remote:'origin',baseBranch:BASE_BRANCH,acceptedBase:BASE,expectedHead:HEAD,checkoutRef:HEAD,branch:BRANCH,expectedRemoteUrl:null,cloneTransport:'GITHUB_GH',githubHostname:'github.com'}],
 authority:{authorityRef:'authority.vexlife-prod.pr550.source-owner',authorityMode:'EXACT_STAGE_EFFECTS',grantedEffects:effects,conditionalEffects:[],consumedEffects:[],remainingEffects:effects,heldEffects:['WRITE_SOURCE','CREATE_BRANCH','CREATE_COMMIT','PUSH_BRANCH','CREATE_PULL_REQUEST','UPDATE_COORDINATION_METADATA','APPROVE_EXACT_HEAD','MARK_READY','ORDINARY_MERGE'],repositoryMembranes:[{repositoryRef:'primary',allowedPaths:CHANGED}],allowedExecutables:['/bin/bash','/usr/bin/git','git','gh','node'],allowedEnvironment:[],remoteObservationOwner:'LocalRelay',allowMutationLoop:false,stopConditions:['STOP_ON_UNKNOWN','STOP_ON_PATH_ESCAPE','STOP_ON_AUTH_BLOCK','STOP_ON_TEST_FAILURE','STOP_ON_EXACT_HEAD_OR_TREE_DRIFT']},
 source:{sourceSetRef:'source-set.vexlife-prod.pr550.followup-readiness',sourceVersionRef:`source-version.vexlife-prod.pr550.${HEAD}`,overlapEvidenceRef:'overlap.vexlife-prod.pr550.followup-readiness.a002',activeConflictState:'NONE',candidateState:'EXACT',candidateHead:HEAD,candidateTree:TREE,expectedChangedPaths:CHANGED.map(p=>({repositoryRef:'primary',path:p})),intentionallyUnchangedAuthorizedPaths:[]},
 policy:{maxOuterReturnFiles:10,maxSelectedSourceEntries:2,maxSelectedSourceBytes:5000000,maxDiffBytes:10000000,requireRacePreflightBeforeMutation:false,requireLocalValidationBeforePublish:false,requireInputPackageManifest:false,signoffCommit:true,draftPullRequest:true}
};

const stage={repositoryRef:'primary',expectedBase:BASE,expectedHead:HEAD,expectedTree:TREE,branch:BRANCH,
 proofCommands:[
  {command:'/bin/bash',args:['-c',"ISOLATED=\"$(/usr/bin/mktemp -d \"${TMPDIR:-/tmp}/pr550-a002-java-tmp.XXXXXX\")\"; cleanup(){ /bin/rm -rf -- \"$ISOLATED\"; }; trap cleanup EXIT HUP INT TERM; export TMPDIR=\"$ISOLATED\"; export JAVA_TOOL_OPTIONS=\"-Djava.io.tmpdir=$ISOLATED\"; /bin/bash -x tools/release/prove-mac-candidate.sh 0.0.0"],proofClass:'TEST',candidateEvidence:true,predicateClass:'SOURCE_OWNER_REPOSITORY',predicateRef:'predicate.vexlife-prod.pr550.mac-candidate-isolated-trace',timeoutMs:1800000}
 ],
 maxFiles:2,stateRef:'state.vexlife-prod.pr550.source-ready',currentLifecycleState:'SOURCE_READY',
 currentGoal:'Prove the currentized Follow-up readiness copy: recommendation text matches #582 evidence, all localization identities/languages remain current, and Product runtime/configuration behavior remains unchanged.',
 remainingGoals:['If A002 PASS, run fresh Independent Assurance on the exact currentized localization head.','Then compose proved #550 copy with proved #582 recommendation source during integration; neither branch independently steals the other owner.'],
 onPass:{lifecycleState:'ASSURANCE_PENDING',nextFunction:'F13.RUN_FRESH_ASSURANCE',nextOwner:'Operations',whyThisIsNext:'The exact currentized Settings copy is proved; fresh Independent Assurance must judge copy/source alignment before integration with #582.'},
 resultFilename:'RETURN-VEXLIFE-PR550-FOLLOWUP-READINESS-SOURCE-OWNER-A002.zip'
};

const authorityEnvelope={authoritySchemaRef:'vextreme.effect-transaction/v1',semanticAdmissionRef:'github.pull.vexlife-prod.550',transactionRef:'transaction.vexlife-prod.pr550.followup-readiness.source-owner.a002',transactionVersion:1,allowedEffectRefs:['effect.vexrelay.execute'],heldEffectRefs:['effect.vexlife-prod.source-mutation','effect.vexlife-prod.approval','effect.vexlife-prod.ready','effect.vexlife-prod.merge','effect.vexlife-prod.product-lifecycle','effect.vexlife-prod.model-runtime']};

const outerTaskTemplate={
 schemaVersion:'vextreme.vexlocalbridge-task-manifest/v1',artifactClass:'EXECUTABLE_RELAY',
 taskRef:'task.vexlife-prod.pr550.followup-readiness.source-owner.a002.carrier',
 attemptRef:`attempt.vexlife-prod.pr550.followup-readiness.source-owner.a002.carrier.${suffix}`,
 packageRef:`package.vexlife-prod.pr550.followup-readiness.source-owner.a002.carrier.${suffix}`,
 requestRef:`request.vexlife-prod.pr550.followup-readiness.source-owner.a002.carrier.${suffix}`,
 correlationRef:`correlation.vexlife-prod.pr550.followup-readiness.source-owner.a002.carrier.${suffix}`,
 taskStorageKey:'vexlife-prod-pr550-followup-readiness-source-owner-a002',attemptStorageKey:`attempt-a002-${suffix}`,
 requestingRoleRef:'Operations[PR550-SOURCE-OWNER]',requestingInstanceRef:`instance.vexlife-prod.pr550.source-owner.carrier.a002.${suffix}`,
 parentOperationsRef:'github.issue.vexlife-prod.514',returnRouteRef:'github.pull.vexlife-prod.550',
 hostProfileRef:'macos-victor',repositoryExecutionProfileRefOrNull:'profile.repository.vextreme-sdk.macos-v1',
 repositoryRefOrNull:'vgong24/Vextreme-SDK',baseRefOrNull:SDK_HEAD,headRefOrNull:SDK_HEAD,treeRefOrNull:SDK_TREE,branchRefOrNull:'main',claimRefOrNull:null,exactPathRefs:[],
 authorityBinding:{authoritySchemaRef:authorityEnvelope.authoritySchemaRef,authorityEnvelopePath:'authority/effect.json',authorityEnvelopeSha256:'0'.repeat(64),semanticAdmissionRef:authorityEnvelope.semanticAdmissionRef,transactionRef:authorityEnvelope.transactionRef,transactionVersion:1,allowedEffectRefs:[...authorityEnvelope.allowedEffectRefs],heldEffectRefs:[...authorityEnvelope.heldEffectRefs]},
 payload:{payloadClass:native.VEXRELAY_ARCHIVE_PAYLOAD_CLASS,payloadRuntime:native.VEXRELAY_ARCHIVE_RUNTIME,payloadPath:'payload/inner-vexrelay.zip',payloadSha256:'0'.repeat(64),typedArgv:[],immutableInputs:[{path:'inputs/vexrelay-binding.json',sha256:'0'.repeat(64)}]},
 executionStateAtFormation:'TASK_PREPARED',singleUseNonce:`nonce-pr550-carrier-a002-${suffix}-${crypto.randomBytes(16).toString('hex')}`,
 stopConditions:['STOP_ON_INNER_TASK_IDENTITY_DRIFT','STOP_ON_EXACT_SOURCE_DRIFT','STOP_ON_PROOF_FAILURE','STOP_ON_CANONICAL_RETURN_CONSUMER_FAILURE'],archiveDigestBinding:'FILENAME_SHA256'
};

fs.mkdirSync(outputRoot,{recursive:true});
const formed=await formVexRelayTaskArchiveCarrier({recipe:'ExactHeadSourceOwnerProof',seed,stage,outerTaskTemplate,authorityEnvelope,outputDirectory:outputRoot,outputStem:'VEXLIFE-PR550-FOLLOWUP-READINESS-SOURCE-OWNER-A002-TASK',ttlMs:60*60*1000,archiveAdapter:adapter.archive});
const capsuleRoot=expectedCapsuleRoot(formed.outer.finalZip,formed.outer.digest,profile.zipRelayCapsule);
process.stdout.write(JSON.stringify({finalZip:formed.outer.finalZip,digest:formed.outer.digest,capsuleRoot,profilePath,outerTaskRef:formed.outer.task.taskRef,innerTaskRef:formed.inner.manifest.taskRef,innerAttemptRef:formed.inner.manifest.attemptRef,innerArchiveSha256:formed.inner.sha256,candidate:{base:BASE,head:HEAD,tree:TREE,branch:BRANCH},formationEvidence:formed.outer.formationEvidence},null,2)+'\n');
