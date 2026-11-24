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

const BASE='9cc89abd429b6759e944a821b45c4bb476ae496d';
const HEAD='c8d13a59c7250ea77c0461044495e6147569d427';
const TREE='324fc5db486e01ac7a7fbe471fefe5cafa62ee8d';
const BRANCH='VXG-112426-chatgpt-response-readiness-presentation-a001';
const BASE_BRANCH='VXG-112426-chatgpt-response-readiness-projection-a001';
const CHANGED=[
  'docs/lens/vexlife-prod.screen-lens.json',
  'localization/strings/manifest.json',
  'localization/strings/source/workspace.json',
  'src/commonMain/kotlin/vexlife/web/VexLifeWebP1ExperienceStyle.kt',
  'src/commonMain/kotlin/vexlife/web/VexLifeWebP1RuntimeArtifacts.kt',
  'src/commonTest/kotlin/vexlife/web/VexLifeWebP1RuntimeArtifactsTest.kt'
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
 taskRef:'task.vexlife-prod.pr598.response-readiness-presentation.source-owner.a001',
 attemptRef:`attempt.vexlife-prod.pr598.response-readiness-presentation.source-owner.a001.${suffix}`,
 requestRef:`request.vexlife-prod.pr598.response-readiness-presentation.source-owner.a001.${suffix}`,
 correlationRef:`correlation.vexlife-prod.pr598.response-readiness-presentation.source-owner.a001.${suffix}`,
 packageRef:`package.vexlife-prod.pr598.response-readiness-presentation.source-owner.a001.${suffix}`,
 original:{
  originalIntent:'Complete the person-facing Response Readiness projection through the existing ResponseProgress without exposing provider cache mechanics or weakening localization currentness.',
  originalGoal:'Prove the six-path #598 presentation candidate: #550 wording, #513 repaint semantics, neutral ResponseProgress projection, complete localization, and the committed Product Screen Lens are current on the exact source head.',
  userProtectedIntent:'The person should see ordinary follow-up readiness meaning, not cache counters or a guessed memory threshold. The existing first-local-start presentation must remain authoritative when it applies.',
  sourceRefs:['github.pull.vexlife-prod.598','github.pull.vexlife-prod.593','github.pull.vexlife-prod.550','github.pull.vexlife-prod.513','github.issue.vexlife-prod.514']
 },
 lane:{entryClass:'SAME_LANE_SUCCESSOR',scopeMode:'STAGE_BOUNDED',laneRef:'lane.vexlife-prod.response-readiness.presentation-proof',parentRootRef:'github.issue.vexlife-prod.514',workRef:'github.pull.vexlife-prod.598',claimRef:'claim.vexlife-prod.pr598.response-readiness-presentation.source-owner',returnRouteRef:'github.pull.vexlife-prod.598',ownerRef:'owner.vexlife-prod.pr598.response-readiness-presentation-proof',rootHubRef:'hub.vexlife-prod.warm-boot'},
 role:{activeRole:'CODER',roleRef:'role.coder.vexlife-prod.pr598',instanceRef:`instance.vexlife-prod.pr598.response-readiness-presentation.source-owner.a001.${suffix}`,occupancyRef:'occupancy.vexlife-prod.pr598.response-readiness-presentation-proof',providerBindingRef:'provider.github.vexlife-prod.pr598',threadRef:'thread.vexlife-prod.pr598.response-readiness-presentation-proof',authorityEnvelopeRef:'authority-envelope.vexlife-prod.pr598.response-readiness-presentation.source-owner'}`,occupancyRef:'occupancy.vexlife-prod.pr598.response-readiness-presentation-proof',providerBindingRef:'provider.github.vexlife-prod.pr598',threadRef:'thread.vexlife-prod.pr598.response-readiness-presentation-proof',authorityEnvelopeRef:'authority-envelope.vexlife-prod.pr598.response-readiness-presentation.source-owner'},
 capabilities:{platformGitHubRead:'UNAVAILABLE',platformGitHubWrite:'UNAVAILABLE'},
 execution:{hostProfile:'macos-victor',executionSurface:'MAC_ZIP_RUN_COMMAND',singleUseNonce:`nonce-pr598-a001-${suffix}-${crypto.randomBytes(16).toString('hex')}`,duplicateAttemptMarker:'marker.vexlife-prod.pr598.response-readiness-presentation.source-owner.a001',duplicateProtectionMode:'OUTER_RELAY_BOUND',formedAt:now.toISOString(),expiresAtOrNull:expiry.toISOString()},
 workspace:{mode:'RUN_ROOT',projectRef:'project.vexlife-prod.pr598.response-readiness-presentation.source-owner',projectPath:'workspace',createRoot:true,createProjectPath:true},
 repositories:[{repositoryRef:'primary',mode:'CLONE_IN_WORKSPACE',nameWithOwner:'VexGPT/VexLife-Prod',localPath:'vexlife-prod',remote:'origin',baseBranch:BASE_BRANCH,acceptedBase:BASE,expectedHead:HEAD,checkoutRef:HEAD,branch:BRANCH,expectedRemoteUrl:null,cloneTransport:'GITHUB_GH',githubHostname:'github.com'}],
 authority:{authorityRef:'authority.vexlife-prod.pr598.response-readiness-presentation.source-owner',authorityMode:'EXACT_STAGE_EFFECTS',grantedEffects:effects,conditionalEffects:[],consumedEffects:[],remainingEffects:effects,heldEffects:['WRITE_SOURCE','CREATE_BRANCH','CREATE_COMMIT','PUSH_BRANCH','CREATE_PULL_REQUEST','UPDATE_COORDINATION_METADATA','APPROVE_EXACT_HEAD','MARK_READY','ORDINARY_MERGE'],repositoryMembranes:[{repositoryRef:'primary',allowedPaths:CHANGED}],allowedExecutables:['/bin/bash','/usr/bin/git','git','gh','node'],allowedEnvironment:[],remoteObservationOwner:'LocalRelay',allowMutationLoop:false,stopConditions:['STOP_ON_UNKNOWN','STOP_ON_PATH_ESCAPE','STOP_ON_AUTH_BLOCK','STOP_ON_TEST_FAILURE','STOP_ON_EXACT_HEAD_OR_TREE_DRIFT']},
 source:{sourceSetRef:'source-set.vexlife-prod.pr598.response-readiness-presentation-proof',sourceVersionRef:`source-version.vexlife-prod.pr598.${HEAD}`,overlapEvidenceRef:'overlap.vexlife-prod.pr598.response-readiness-presentation-proof.a001',activeConflictState:'NONE',candidateState:'EXACT',candidateHead:HEAD,candidateTree:TREE,expectedChangedPaths:CHANGED.map(p=>({repositoryRef:'primary',path:p})),intentionallyUnchangedAuthorizedPaths:[]},
 policy:{maxOuterReturnFiles:10,maxSelectedSourceEntries:6,maxSelectedSourceBytes:5000000,maxDiffBytes:10000000,requireRacePreflightBeforeMutation:false,requireLocalValidationBeforePublish:false,requireInputPackageManifest:false,signoffCommit:true,draftPullRequest:true}
};

const stage={repositoryRef:'primary',expectedBase:BASE,expectedHead:HEAD,expectedTree:TREE,branch:BRANCH,
 proofCommands:[
  {command:'/bin/bash',args:['-c',"ISOLATED=\"$(/usr/bin/mktemp -d \"${TMPDIR:-/tmp}/pr598-a001-java-tmp.XXXXXX\")\"; cleanup(){ /bin/rm -rf -- \"$ISOLATED\"; }; trap cleanup EXIT HUP INT TERM; export TMPDIR=\"$ISOLATED\"; export JAVA_TOOL_OPTIONS=\"-Djava.io.tmpdir=$ISOLATED\"; ./gradlew --no-daemon --max-workers=1 -Dorg.gradle.jvmargs=-Xmx4g -Dkotlin.daemon.jvm.options=-Xmx4g jvmTest checkVexLifeStrings"],proofClass:'TEST',candidateEvidence:true,predicateClass:'SOURCE_OWNER_REPOSITORY',predicateRef:'predicate.vexlife-prod.pr598.response-readiness-presentation.source-and-lens',timeoutMs:1800000}
 ],
 maxFiles:6,stateRef:'state.vexlife-prod.pr598.response-readiness-presentation.source-ready',currentLifecycleState:'SOURCE_READY',
 currentGoal:'Prove #598 source, localization and Product-generated Screen Lens currentness while preserving #513 repaint behavior and keeping provider measurement data out of browser presentation.',
 remainingGoals:['If A001 PASS, run fresh Independent Assurance on the exact #598 presentation head.','Then converge the separately-routed Mac DMG hygiene dependency before READY/merge.'],
 onPass:{lifecycleState:'ASSURANCE_PENDING',nextFunction:'F13.RUN_FRESH_ASSURANCE',nextOwner:'Operations',whyThisIsNext:'The presentation source, localization and committed Screen Lens are repository-proved; fresh Assurance must verify the human meaning and owner composition before lifecycle convergence.'},
 resultFilename:'RETURN-VEXLIFE-PR598-RESPONSE-READINESS-PRESENTATION-SOURCE-OWNER-A001.zip'
};

const authorityEnvelope={authoritySchemaRef:'vextreme.effect-transaction/v1',semanticAdmissionRef:'github.pull.vexlife-prod.598',transactionRef:'transaction.vexlife-prod.pr598.response-readiness-presentation.source-owner.a001',transactionVersion:1,allowedEffectRefs:['effect.vexrelay.execute'],heldEffectRefs:['effect.vexlife-prod.source-mutation','effect.vexlife-prod.approval','effect.vexlife-prod.ready','effect.vexlife-prod.merge','effect.vexlife-prod.product-lifecycle','effect.vexlife-prod.model-runtime']};

const outerTaskTemplate={
 schemaVersion:'vextreme.vexlocalbridge-task-manifest/v1',artifactClass:'EXECUTABLE_RELAY',
 taskRef:'task.vexlife-prod.pr598.response-readiness-presentation.source-owner.a001.carrier',
 attemptRef:`attempt.vexlife-prod.pr598.response-readiness-presentation.source-owner.a001.carrier.${suffix}`,
 packageRef:`package.vexlife-prod.pr598.response-readiness-presentation.source-owner.a001.carrier.${suffix}`,
 requestRef:`request.vexlife-prod.pr598.response-readiness-presentation.source-owner.a001.carrier.${suffix}`,
 correlationRef:`correlation.vexlife-prod.pr598.response-readiness-presentation.source-owner.a001.carrier.${suffix}`,
 taskStorageKey:'vexlife-prod-pr598-response-readiness-presentation-source-owner-a001',attemptStorageKey:`attempt-a001-${suffix}`,
 requestingRoleRef:'Operations[PR598-SOURCE-OWNER]',requestingInstanceRef:`instance.vexlife-prod.pr598.source-owner.carrier.a001.${suffix}`,
 parentOperationsRef:'github.issue.vexlife-prod.514',returnRouteRef:'github.pull.vexlife-prod.593',
 hostProfileRef:'macos-victor',repositoryExecutionProfileRefOrNull:'profile.repository.vextreme-sdk.macos-v1',
 repositoryRefOrNull:'vgong24/Vextreme-SDK',baseRefOrNull:SDK_HEAD,headRefOrNull:SDK_HEAD,treeRefOrNull:SDK_TREE,branchRefOrNull:'main',claimRefOrNull:null,exactPathRefs:[],
 authorityBinding:{authoritySchemaRef:authorityEnvelope.authoritySchemaRef,authorityEnvelopePath:'authority/effect.json',authorityEnvelopeSha256:'0'.repeat(64),semanticAdmissionRef:authorityEnvelope.semanticAdmissionRef,transactionRef:authorityEnvelope.transactionRef,transactionVersion:1,allowedEffectRefs:[...authorityEnvelope.allowedEffectRefs],heldEffectRefs:[...authorityEnvelope.heldEffectRefs]},
 payload:{payloadClass:native.VEXRELAY_ARCHIVE_PAYLOAD_CLASS,payloadRuntime:native.VEXRELAY_ARCHIVE_RUNTIME,payloadPath:'payload/inner-vexrelay.zip',payloadSha256:'0'.repeat(64),typedArgv:[],immutableInputs:[{path:'inputs/vexrelay-binding.json',sha256:'0'.repeat(64)}]},
 executionStateAtFormation:'TASK_PREPARED',singleUseNonce:`nonce-pr598-carrier-a001-${suffix}-${crypto.randomBytes(16).toString('hex')}`,
 stopConditions:['STOP_ON_INNER_TASK_IDENTITY_DRIFT','STOP_ON_EXACT_SOURCE_DRIFT','STOP_ON_PROOF_FAILURE','STOP_ON_CANONICAL_RETURN_CONSUMER_FAILURE'],archiveDigestBinding:'FILENAME_SHA256'
};

fs.mkdirSync(outputRoot,{recursive:true});
const formed=await formVexRelayTaskArchiveCarrier({recipe:'ExactHeadSourceOwnerProof',seed,stage,outerTaskTemplate,authorityEnvelope,outputDirectory:outputRoot,outputStem:'VEXLIFE-PR598-RESPONSE-READINESS-PRESENTATION-SOURCE-OWNER-A001-TASK',ttlMs:60*60*1000,archiveAdapter:adapter.archive});
const capsuleRoot=expectedCapsuleRoot(formed.outer.finalZip,formed.outer.digest,profile.zipRelayCapsule);
process.stdout.write(JSON.stringify({finalZip:formed.outer.finalZip,digest:formed.outer.digest,capsuleRoot,profilePath,outerTaskRef:formed.outer.task.taskRef,innerTaskRef:formed.inner.manifest.taskRef,innerAttemptRef:formed.inner.manifest.attemptRef,innerArchiveSha256:formed.inner.sha256,candidate:{base:BASE,head:HEAD,tree:TREE,branch:BRANCH},formationEvidence:formed.outer.formationEvidence},null,2)+'\n');
