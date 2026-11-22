import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { createRequire } from 'node:module';
import { pathToFileURL } from 'node:url';

const [sdkRootArg, outputRootArg] = process.argv.slice(2);
if (!sdkRootArg || !outputRootArg) throw new Error('usage: node formation-A002.mjs <sdk-root> <output-root>');
const sdkRoot = path.resolve(sdkRootArg);
const outputRoot = path.resolve(outputRootArg);
const require = createRequire(import.meta.url);
const { formVexRelayTaskArchiveCarrier } = await import(pathToFileURL(path.join(sdkRoot, 'lib/vexlocalbridge/vexrelay-task-archive-carrier-formation.mjs')).href);
const { createHostAdapter } = require(path.join(sdkRoot, 'lib/vexlocalbridge/mac-runner.js'));
const { expectedCapsuleRoot } = require(path.join(sdkRoot, 'lib/vexlocalbridge/cli.js'));
const native = require(path.join(sdkRoot, 'lib/vexlocalbridge/vexrelay-task-archive.js'));

const BASE = '0566b1eed876a28c2fcc6586743ae1f4af7eaecd';
const HEAD = 'a23b6a949d27e4b77530273a390955f0d127ad1a';
const TREE = 'a268826bf26eecb3a2136593b202cf651643d4e9';
const BRANCH = 'VXG-112126-chatgpt-sent-resource-continuity-current-main-a001';
const BASE_BRANCH = 'main';
const CHANGED = [
  'docs/lens/vexlife-prod.screen-lens.json',
  'docs/work/sent-resource-continuity/README.md',
  'localization/strings/manifest.json',
  'localization/strings/source/workspace.json',
  'src/commonMain/kotlin/vexlife/artifact/VexLifeLocalArtifact.kt',
  'src/commonMain/kotlin/vexlife/service/VexLifeProdServiceContract.kt',
  'src/commonMain/kotlin/vexlife/web/VexLifeWebP1ExperienceMarkup.kt',
  'src/commonMain/kotlin/vexlife/web/VexLifeWebP1ExperienceStyle.kt',
  'src/commonMain/kotlin/vexlife/web/VexLifeWebP1RuntimeArtifacts.kt',
  'src/commonTest/kotlin/vexlife/web/VexLifeWebP1RuntimeArtifactsTest.kt',
  'src/jvmMain/kotlin/vexlife/artifact/VexLifeHistoricalRequestResources.kt',
  'src/jvmMain/kotlin/vexlife/artifact/VexLifeLocalArtifactStore.kt',
  'src/jvmMain/kotlin/vexlife/artifact/VexLifeRequestArtifactManifest.kt',
  'src/jvmMain/kotlin/vexlife/capability/reach/VexLifeAttachmentReach.kt',
  'src/jvmMain/kotlin/vexlife/companion/DirectCompanionConnection.kt',
  'src/jvmMain/kotlin/vexlife/companion/DirectCompanionTurn.kt',
  'src/jvmMain/kotlin/vexlife/companion/VexLifeInitialPromptPreparation.kt',
  'src/jvmMain/kotlin/vexlife/local/VexLifeProdMacRuntimeHandoff.kt',
  'src/jvmMain/kotlin/vexlife/logging/VexLogger.kt',
  'src/jvmMain/kotlin/vexlife/service/VexLifeConversationServiceProjection.kt',
  'src/jvmMain/kotlin/vexlife/service/VexLifeProdLocalService.kt',
  'src/jvmMain/kotlin/vexlife/service/VexLifeProdMacServiceLifetime.kt',
  'src/jvmMain/kotlin/vexlife/service/VexLifeSentResourceReview.kt',
  'src/jvmMain/kotlin/vexlife/setup/VexLifeProdSetupProcess.kt',
  'src/jvmTest/kotlin/vexlife/artifact/VexLifeHistoricalRequestResourcesTest.kt',
  'src/jvmTest/kotlin/vexlife/artifact/VexLifeLocalArtifactStoreTest.kt',
  'src/jvmTest/kotlin/vexlife/artifact/VexLifeRequestArtifactManifestTest.kt',
  'src/jvmTest/kotlin/vexlife/companion/DirectCompanionAttachmentTurnTest.kt',
  'src/jvmTest/kotlin/vexlife/companion/DirectCompanionConnectionTest.kt',
  'src/jvmTest/kotlin/vexlife/companion/DirectCompanionTurnTest.kt',
  'src/jvmTest/kotlin/vexlife/companion/DirectPromptContextTest.kt',
  'src/jvmTest/kotlin/vexlife/companion/VexLifeInitialPromptPreparationTest.kt',
  'src/jvmTest/kotlin/vexlife/local/VexLifeProdMacRuntimeHandoffTest.kt',
  'src/jvmTest/kotlin/vexlife/service/VexLifeConversationServiceProjectionTest.kt',
  'src/jvmTest/kotlin/vexlife/service/VexLifeProdLocalServiceTest.kt',
  'src/jvmTest/kotlin/vexlife/service/VexLifeProdMacServiceLifetimeTest.kt',
  'src/jvmTest/kotlin/vexlife/service/VexLifeSentResourceReviewTest.kt',
  'src/jvmTest/kotlin/vexlife/setup/VexLifeProdSetupProcessTest.kt'
];
const SDK_HEAD = '6f8f6f50467f5df5a542713f7dc25b82db4ac8fa';
const SDK_TREE = '96795640b01f1969da980f52d5a8ec136a8c44aa';
const profilePath = path.join(sdkRoot, 'docs/private-continuity/vexlocalbridge/profiles/macos-victor-vextreme-sdk.json');
const profile = JSON.parse(fs.readFileSync(profilePath, 'utf8'));
const adapter = createHostAdapter(profile);
const suffix = crypto.randomBytes(10).toString('hex');
const now = new Date();
const expiry = new Date(now.getTime() + 60 * 60 * 1000);
const effects = ['CHECK_AUTH', 'READ_REPOSITORY', 'CHECK_STATE', 'QUERY_PROVIDER', 'RUN_LOCAL_COMMAND', 'FORM_RESULT'];

const seed = {
  schemaVersion: 'vex.relay.task/v2',
  continuity: '[VXG RealForever]',
  taskRef: 'task.vexlife-prod.pr488.warmboot-sent-resource.source-owner.a002',
  attemptRef: 'attempt.vexlife-prod.pr488.warmboot-sent-resource.source-owner.a002.' + suffix,
  requestRef: 'request.vexlife-prod.pr488.warmboot-sent-resource.source-owner.a002.' + suffix,
  correlationRef: 'correlation.vexlife-prod.pr488.warmboot-sent-resource.source-owner.a002.' + suffix,
  packageRef: 'package.vexlife-prod.pr488.warmboot-sent-resource.source-owner.a002.' + suffix,
  original: {
    originalIntent: 'Converge sent-resource continuity with the current Warm Boot stack as one lived Conversation circuit.',
    originalGoal: 'Prove the restored exact Product source where attached image/file resources reach Vex, remain visibly reviewable on the human message after Send/reload, and first-turn provider-prefix preparation/runtime reuse stays correctly bounded to the same canonical Conversation attempt.',
    userProtectedIntent: 'The person should see what they actually sent, Vex should perceive that exact resource, startup/cache work should happen only at the right time, and later ordinary turns must not inherit a stale attachment or first-start state.',
    sourceRefs: ['github.pull.vexlife-prod.488', 'github.commit.vexlife-prod.a23b6a949d27e4b77530273a390955f0d127ad1a', 'github.pull.vexlife-prod.525', 'github.pull.vexlife-prod.533', 'github.pull.vexlife-prod.541', 'github.pull.vexlife-prod.542']
  },
  lane: {
    entryClass: 'SAME_LANE_SUCCESSOR', scopeMode: 'STAGE_BOUNDED',
    laneRef: 'lane.vexlife-prod.warmboot-sent-resource-convergence',
    parentRootRef: 'github.issue.vexlife-prod.514', workRef: 'github.pull.vexlife-prod.488',
    claimRef: 'claim.vexlife-prod.pr488.warmboot-sent-resource.source-owner.a002',
    returnRouteRef: 'return.vexlife-prod.pr488.warmboot-sent-resource.source-owner.a002',
    ownerRef: 'owner.vexlife-prod.pr488.warmboot-sent-resource', rootHubRef: 'hub.vexlife-prod.warm-boot'
  },
  role: {
    activeRole: 'CODER', roleRef: 'role.coder.vexlife-prod.pr488',
    instanceRef: 'instance.vexlife-prod.pr488.source-owner.a002.' + suffix,
    occupancyRef: 'occupancy.vexlife-prod.pr488.warmboot-sent-resource',
    providerBindingRef: 'provider.github.vexlife-prod.pr488',
    threadRef: 'thread.vexlife-prod.pr488.warmboot-sent-resource',
    authorityEnvelopeRef: 'authority-envelope.vexlife-prod.pr488.source-owner.a002'
  },
  capabilities: { platformGitHubRead: 'UNAVAILABLE', platformGitHubWrite: 'UNAVAILABLE' },
  execution: {
    hostProfile: 'macos-victor', executionSurface: 'MAC_ZIP_RUN_COMMAND',
    singleUseNonce: 'nonce-pr488-source-owner-a002-' + suffix + '-' + crypto.randomBytes(16).toString('hex'),
    duplicateAttemptMarker: 'marker.vexlife-prod.pr488.warmboot-sent-resource.source-owner.a002',
    duplicateProtectionMode: 'OUTER_RELAY_BOUND', formedAt: now.toISOString(), expiresAtOrNull: expiry.toISOString()
  },
  workspace: { mode: 'RUN_ROOT', projectRef: 'project.vexlife-prod.pr488.source-owner.a002', projectPath: 'workspace', createRoot: true, createProjectPath: true },
  repositories: [{
    repositoryRef: 'primary', mode: 'CLONE_IN_WORKSPACE', nameWithOwner: 'VexGPT/VexLife-Prod', localPath: 'vexlife-prod', remote: 'origin',
    baseBranch: BASE_BRANCH, acceptedBase: BASE, expectedHead: HEAD, checkoutRef: HEAD, branch: BRANCH,
    expectedRemoteUrl: null, cloneTransport: 'GITHUB_GH', githubHostname: 'github.com'
  }],
  authority: {
    authorityRef: 'authority.vexlife-prod.pr488.source-owner.a002', authorityMode: 'EXACT_STAGE_EFFECTS',
    grantedEffects: effects, conditionalEffects: [], consumedEffects: [], remainingEffects: effects,
    heldEffects: ['WRITE_SOURCE', 'CREATE_BRANCH', 'CREATE_COMMIT', 'PUSH_BRANCH', 'CREATE_PULL_REQUEST', 'UPDATE_COORDINATION_METADATA', 'APPROVE_EXACT_HEAD', 'MARK_READY', 'ORDINARY_MERGE'],
    repositoryMembranes: [{ repositoryRef: 'primary', allowedPaths: CHANGED }],
    allowedExecutables: ['/bin/bash', '/usr/bin/git', 'git', 'gh', 'node'], allowedEnvironment: [], remoteObservationOwner: 'LocalRelay', allowMutationLoop: false,
    stopConditions: ['STOP_ON_UNKNOWN', 'STOP_ON_PATH_ESCAPE', 'STOP_ON_AUTH_BLOCK', 'STOP_ON_TEST_FAILURE', 'STOP_ON_EXACT_HEAD_OR_TREE_DRIFT']
  },
  source: {
    sourceSetRef: 'source-set.vexlife-prod.pr488.warmboot-sent-resource', sourceVersionRef: 'source-version.vexlife-prod.pr488.' + HEAD,
    overlapEvidenceRef: 'overlap.vexlife-prod.pr488.warmboot-sent-resource.a002', activeConflictState: 'NONE', candidateState: 'EXACT',
    candidateHead: HEAD, candidateTree: TREE,
    expectedChangedPaths: CHANGED.map((p) => ({ repositoryRef: 'primary', path: p })), intentionallyUnchangedAuthorizedPaths: []
  },
  policy: {
    maxOuterReturnFiles: 10, maxSelectedSourceEntries: 10, maxSelectedSourceBytes: 5000000, maxDiffBytes: 10000000,
    requireRacePreflightBeforeMutation: false, requireLocalValidationBeforePublish: false, requireInputPackageManifest: false,
    signoffCommit: true, draftPullRequest: true
  }
};

const stage = {
  repositoryRef: 'primary', expectedBase: BASE, expectedHead: HEAD, expectedTree: TREE, branch: BRANCH,
  proofCommands: [{
    command: '/bin/bash', args: ['tools/release/prove-mac-candidate.sh', '0.0.0'], proofClass: 'TEST', candidateEvidence: true,
    predicateClass: 'SOURCE_OWNER_REPOSITORY', predicateRef: 'predicate.vexlife-prod.pr488.warmboot-sent-resource.mac-candidate.a002', timeoutMs: 1800000
  }],
  maxFiles: 10, stateRef: 'state.vexlife-prod.pr488.warmboot-sent-resource.source-ready.a002', currentLifecycleState: 'SOURCE_READY',
  currentGoal: 'Prove the restored exact sent-resource + Warm Boot candidate through the repository-owned Mac candidate proof, including generated Screen Lens currentness, localization, attachment/resource persistence, Conversation projection, prompt preparation, runtime handoff/lifetime and regressions.',
  remainingGoals: [
    'Run fresh Independent Assurance on the exact proved head.',
    'Run one wholesale lived scenario covering first-start presentation, image perception, durable sent-resource review/reload, and a clean second ordinary Send.'
  ],
  onPass: {
    lifecycleState: 'ASSURANCE_PENDING', nextFunction: 'F13.RUN_FRESH_ASSURANCE', nextOwner: 'Operations',
    whyThisIsNext: 'The restored exact source is proved; fresh Independent Assurance must judge the same head before the lived whole-circuit acceptance.'
  },
  resultFilename: 'RETURN-VEXLIFE-PR488-WARMBOOT-SENT-RESOURCE-SOURCE-OWNER-A002.zip'
};

const authorityEnvelope = {
  authoritySchemaRef: 'vextreme.effect-transaction/v1', semanticAdmissionRef: 'github.pull.vexlife-prod.488',
  transactionRef: 'transaction.vexlife-prod.pr488.warmboot-sent-resource.source-owner.a002', transactionVersion: 1,
  allowedEffectRefs: ['effect.vexrelay.execute'],
  heldEffectRefs: ['effect.vexlife-prod.source-mutation', 'effect.vexlife-prod.approval', 'effect.vexlife-prod.ready', 'effect.vexlife-prod.merge', 'effect.vexlife-prod.product-lifecycle', 'effect.vexlife-prod.model-runtime']
};

const outerTaskTemplate = {
  schemaVersion: 'vextreme.vexlocalbridge-task-manifest/v1', artifactClass: 'EXECUTABLE_RELAY',
  taskRef: 'task.vexlife-prod.pr488.warmboot-sent-resource.source-owner.a002.carrier',
  attemptRef: 'attempt.vexlife-prod.pr488.warmboot-sent-resource.source-owner.a002.carrier.' + suffix,
  packageRef: 'package.vexlife-prod.pr488.warmboot-sent-resource.source-owner.a002.carrier.' + suffix,
  requestRef: 'request.vexlife-prod.pr488.warmboot-sent-resource.source-owner.a002.carrier.' + suffix,
  correlationRef: 'correlation.vexlife-prod.pr488.warmboot-sent-resource.source-owner.a002.carrier.' + suffix,
  taskStorageKey: 'vexlife-prod-pr488-warmboot-sent-resource-source-owner-a002', attemptStorageKey: 'attempt-a002-' + suffix,
  requestingRoleRef: 'Operations[PR488-WARMBOOT-SENT-RESOURCE-SOURCE-OWNER-A002]',
  requestingInstanceRef: 'instance.vexlife-prod.pr488.source-owner.carrier.a002.' + suffix,
  parentOperationsRef: 'github.pull.vexlife-prod.488', returnRouteRef: 'github.pull.vexlife-prod.488',
  hostProfileRef: 'macos-victor', repositoryExecutionProfileRefOrNull: 'profile.repository.vextreme-sdk.macos-v1',
  repositoryRefOrNull: 'vgong24/Vextreme-SDK', baseRefOrNull: SDK_HEAD, headRefOrNull: SDK_HEAD, treeRefOrNull: SDK_TREE, branchRefOrNull: 'main', claimRefOrNull: null, exactPathRefs: [],
  authorityBinding: {
    authoritySchemaRef: authorityEnvelope.authoritySchemaRef, authorityEnvelopePath: 'authority/effect.json', authorityEnvelopeSha256: '0'.repeat(64),
    semanticAdmissionRef: authorityEnvelope.semanticAdmissionRef, transactionRef: authorityEnvelope.transactionRef, transactionVersion: 1,
    allowedEffectRefs: [...authorityEnvelope.allowedEffectRefs], heldEffectRefs: [...authorityEnvelope.heldEffectRefs]
  },
  payload: {
    payloadClass: native.VEXRELAY_ARCHIVE_PAYLOAD_CLASS, payloadRuntime: native.VEXRELAY_ARCHIVE_RUNTIME, payloadPath: 'payload/inner-vexrelay.zip', payloadSha256: '0'.repeat(64), typedArgv: [],
    immutableInputs: [{ path: 'inputs/vexrelay-binding.json', sha256: '0'.repeat(64) }]
  },
  executionStateAtFormation: 'TASK_PREPARED',
  singleUseNonce: 'nonce-pr488-carrier-a002-' + suffix + '-' + crypto.randomBytes(16).toString('hex'),
  stopConditions: ['STOP_ON_INNER_TASK_IDENTITY_DRIFT', 'STOP_ON_EXACT_SOURCE_DRIFT', 'STOP_ON_PROOF_FAILURE', 'STOP_ON_CANONICAL_RETURN_CONSUMER_FAILURE'],
  archiveDigestBinding: 'FILENAME_SHA256'
};

fs.mkdirSync(outputRoot, { recursive: true });
const formed = await formVexRelayTaskArchiveCarrier({
  recipe: 'ExactHeadSourceOwnerProof', seed, stage, outerTaskTemplate, authorityEnvelope,
  outputDirectory: outputRoot, outputStem: 'VEXLIFE-PR488-WARMBOOT-SENT-RESOURCE-SOURCE-OWNER-A002-TASK', ttlMs: 60 * 60 * 1000,
  archiveAdapter: adapter.archive
});
const capsuleRoot = expectedCapsuleRoot(formed.outer.finalZip, formed.outer.digest, profile.zipRelayCapsule);
process.stdout.write(JSON.stringify({
  finalZip: formed.outer.finalZip, digest: formed.outer.digest, capsuleRoot, profilePath,
  outerTaskRef: formed.outer.task.taskRef, innerTaskRef: formed.inner.manifest.taskRef, innerAttemptRef: formed.inner.manifest.attemptRef,
  innerArchiveSha256: formed.inner.sha256, candidate: { base: BASE, head: HEAD, tree: TREE, branch: BRANCH }, formationEvidence: formed.outer.formationEvidence
}, null, 2) + '\n');
