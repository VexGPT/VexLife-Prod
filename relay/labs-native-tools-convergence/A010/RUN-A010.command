#!/bin/bash
set -euo pipefail

CONTINUITY='[VXG RealForever]'
SDK_REPOSITORY='vgong24/Vextreme-SDK'
SDK_HEAD='8543c7d755faba04d4c4019c02e84f992667918a'
SDK_TREE='6376f68279f0750ee6d3868b8472368199763064'
PRODUCT_REPOSITORY='VexGPT/VexLife-Prod'
PRODUCT_BASE='146b0d482517201b85990a9d7b0b9ebf04075fd9'
PRODUCT_HEAD='e00832bf3e28e2134da71d0191282ff47e2d7211'
PRODUCT_TREE='2fb64cb7bb7009c866b53fb7b3488c6bff396e59'
PRODUCT_BRANCH='VXG-111926-chatgpt-labs-native-tools-convergence-a001'
ATTEMPT='A010'

resolve_exec() {
  local candidate
  for candidate in "$@"; do
    if [ -x "$candidate" ] && [ -f "$candidate" ]; then
      printf '%s' "$candidate"
      return 0
    fi
  done
  return 1
}

BASH_BIN="$(resolve_exec /bin/bash)" || { echo 'A010: qualified Bash unavailable.' >&2; exit 10; }
GIT_BIN="$(resolve_exec /usr/bin/git)" || { echo 'A010: qualified Git unavailable.' >&2; exit 11; }
GH_BIN="$(resolve_exec /opt/homebrew/bin/gh /usr/local/bin/gh)" || { echo 'A010: qualified GitHub CLI unavailable.' >&2; exit 12; }
NODE_BIN="$(resolve_exec /opt/homebrew/bin/node /usr/local/bin/node /usr/bin/node)" || { echo 'A010: qualified Node unavailable.' >&2; exit 13; }
MKTEMP_BIN="$(resolve_exec /usr/bin/mktemp)" || { echo 'A010: mktemp unavailable.' >&2; exit 14; }
RM_BIN="$(resolve_exec /bin/rm /usr/bin/rm)" || { echo 'A010: rm unavailable.' >&2; exit 15; }

ARCH="$(/usr/bin/uname -m)"
[ "$ARCH" = 'arm64' ] || { echo "A010: expected Apple-silicon arm64 host, observed $ARCH." >&2; exit 16; }

"$GH_BIN" auth status --hostname github.com >/dev/null 2>&1 || {
  echo 'A010: GitHub authentication is required before this proof can run.' >&2
  echo 'Run: gh auth login --hostname github.com --web' >&2
  exit 17
}

ROOT="$("$MKTEMP_BIN" -d "$HOME/Downloads/.vexlife-pr393-convergence-a010.XXXXXX")"
SDK_ROOT="$ROOT/Vextreme-SDK"
FORMATION_RESULT="$ROOT/formation-result.json"
FORMATION_SCRIPT="$ROOT/form-pr393-a010.mjs"

cleanup() {
  case "$ROOT" in
    "$HOME"/Downloads/.vexlife-pr393-convergence-a010.*) "$RM_BIN" -rf -- "$ROOT" ;;
    *) return 1 ;;
  esac
}
trap cleanup EXIT HUP INT TERM

echo 'A010: acquiring exact current VexRelay/VexLocalBridge source...'
"$GH_BIN" repo clone "$SDK_REPOSITORY" "$SDK_ROOT" -- --no-checkout >/dev/null
"$GIT_BIN" -C "$SDK_ROOT" checkout --detach "$SDK_HEAD" >/dev/null

OBSERVED_HEAD="$("$GIT_BIN" -C "$SDK_ROOT" rev-parse HEAD)"
OBSERVED_TREE="$("$GIT_BIN" -C "$SDK_ROOT" rev-parse 'HEAD^{tree}')"
[ "$OBSERVED_HEAD" = "$SDK_HEAD" ] || { echo 'A010: SDK head mismatch.' >&2; exit 18; }
[ "$OBSERVED_TREE" = "$SDK_TREE" ] || { echo 'A010: SDK tree mismatch.' >&2; exit 19; }
[ -z "$("$GIT_BIN" -C "$SDK_ROOT" status --porcelain=v1 -uall)" ] || { echo 'A010: acquired SDK source is dirty.' >&2; exit 20; }

A010_SDK_ROOT="$SDK_ROOT" "$NODE_BIN" -e '
const fs=require("node:fs");
const path=require("node:path");
const root=process.env.A010_SDK_ROOT;
const contract=JSON.parse(fs.readFileSync(path.join(root,"docs/private-continuity/vexrelay/current-contract.json"),"utf8"));
if(contract.contractRef!=="source.vexrelay.current-contract/v1") process.exit(21);
const carrier=contract.acceptedAdapters.find(x=>x.adapterRef==="adapter.vexlocalbridge.native-vexrelay-task-archive/v1");
if(!carrier || carrier.state!=="TERMINAL_ACCEPTED_CURRENT") process.exit(22);
if(!contract.acceptedRecipeFamilies.includes("ExactHeadSourceOwnerProof")) process.exit(23);
'

cat > "$FORMATION_SCRIPT" <<'NODE_A010'
import fs from 'node:fs';
import path from 'node:path';
import { pathToFileURL } from 'node:url';

const SDK_ROOT = process.env.A010_SDK_ROOT;
const OUT = process.env.A010_FORMATION_RESULT;
const DOWNLOADS = path.join(process.env.HOME, 'Downloads');

const { formVexRelayTaskArchiveCarrier } =
  await import(pathToFileURL(path.join(
    SDK_ROOT,
    'lib/vexlocalbridge/vexrelay-task-archive-carrier-formation.mjs',
  )).href);

const BASE = '146b0d482517201b85990a9d7b0b9ebf04075fd9';
const HEAD = 'e00832bf3e28e2134da71d0191282ff47e2d7211';
const TREE = '2fb64cb7bb7009c866b53fb7b3488c6bff396e59';
const BRANCH = 'VXG-111926-chatgpt-labs-native-tools-convergence-a001';

const CHANGED_PATHS = [
  'docs/foundation/CAPABILITY-ATLAS-SELF-FRAME.md',
  'docs/foundation/DIRECT-PERSONAL-COMPANION.md',
  'docs/foundation/FOUNDATIONAL-FIELD.md',
  'docs/foundation/VEX-LABS-NATIVE-TEST.md',
  'docs/work/memory-depth-context-synthesis/README.md',
  'src/commonMain/kotlin/vexlife/model/heretic/HereticCompanionRepresentation.kt',
  'src/commonMain/kotlin/vexlife/model/heretic/HereticModelImport.kt',
  'src/commonMain/kotlin/vexlife/service/VexLifeProdServiceContract.kt',
  'src/commonMain/kotlin/vexlife/web/VexLifeWebP1ExperienceMarkup.kt',
  'src/commonMain/kotlin/vexlife/web/VexLifeWebP1ExperienceStyle.kt',
  'src/commonMain/kotlin/vexlife/web/VexLifeWebP1RuntimeArtifacts.kt',
  'src/commonTest/kotlin/vexlife/model/heretic/HereticModelCatalogTest.kt',
  'src/commonTest/kotlin/vexlife/web/VexLifeWebP1RuntimeArtifactsTest.kt',
  'src/commonTest/kotlin/vexlife/web/VexLifeWebP1ThoughtVisibilityTest.kt',
  'src/jvmMain/kotlin/vexlife/capability/VexLifeCapabilityAtlas.kt',
  'src/jvmMain/kotlin/vexlife/capability/VexLifeCapabilityBridge.kt',
  'src/jvmMain/kotlin/vexlife/capability/VexLifeReachDefinitions.kt',
  'src/jvmMain/kotlin/vexlife/capability/reach/VexLifeHomeReach.kt',
  'src/jvmMain/kotlin/vexlife/capability/reach/VexLifeMemoryRecall.kt',
  'src/jvmMain/kotlin/vexlife/companion/DirectCompanionConnection.kt',
  'src/jvmMain/kotlin/vexlife/companion/DirectCompanionTurn.kt',
  'src/jvmMain/kotlin/vexlife/companion/DirectConversationSourceReturn.kt',
  'src/jvmMain/kotlin/vexlife/companion/DirectPromptContext.kt',
  'src/jvmMain/kotlin/vexlife/companion/HarmonyCapabilityTools.kt',
  'src/jvmMain/kotlin/vexlife/companion/ProviderCapabilityTools.kt',
  'src/jvmMain/kotlin/vexlife/diagnostics/VexLifeDiagnostics.kt',
  'src/jvmMain/kotlin/vexlife/labs/VexLabsNativeTest.kt',
  'src/jvmMain/kotlin/vexlife/logging/VexLogger.kt',
  'src/jvmMain/kotlin/vexlife/model/heretic/HuggingFaceModelInspection.kt',
  'src/jvmMain/kotlin/vexlife/model/heretic/runtime/HereticExecutionTopology.kt',
  'src/jvmMain/kotlin/vexlife/service/VexLifeProdLocalService.kt',
  'src/jvmMain/resources/foundational/vex-foundational-field.json',
  'src/jvmMain/resources/foundational/vex-foundational-field.revisions.json',
  'src/jvmTest/kotlin/vexlife/capability/VexLifeCapabilityAtlasTest.kt',
  'src/jvmTest/kotlin/vexlife/capability/VexLifeCapabilityReachTest.kt',
  'src/jvmTest/kotlin/vexlife/capability/VexLifeCapabilityRevisitTest.kt',
  'src/jvmTest/kotlin/vexlife/companion/DirectCompanionCapabilityTurnTest.kt',
  'src/jvmTest/kotlin/vexlife/companion/DirectCompanionConnectionTest.kt',
  'src/jvmTest/kotlin/vexlife/companion/DirectCompanionTurnFoundationalFieldTest.kt',
  'src/jvmTest/kotlin/vexlife/companion/DirectCompanionTurnTest.kt',
  'src/jvmTest/kotlin/vexlife/companion/DirectConversationSourceReturnTest.kt',
  'src/jvmTest/kotlin/vexlife/companion/DirectPromptCurrentStateTest.kt',
  'src/jvmTest/kotlin/vexlife/companion/HarmonyCapabilityToolsTest.kt',
  'src/jvmTest/kotlin/vexlife/foundational/VexLifeFoundationalFieldTest.kt',
  'src/jvmTest/kotlin/vexlife/harness/ContinuityPressureAdapter.kt',
  'src/jvmTest/kotlin/vexlife/labs/VexLabsNativeTestTest.kt',
  'src/jvmTest/kotlin/vexlife/model/heretic/HuggingFaceModelInspectionTest.kt',
  'src/jvmTest/kotlin/vexlife/model/heretic/runtime/HereticExecutionTopologyTest.kt',
];

const now = new Date();
const expires = new Date(now.getTime() + 60 * 60 * 1000);

const seed = {
  schemaVersion: 'vex.relay.task/v2',
  continuity: '[VXG RealForever]',
  taskRef: 'task.vexlife-prod.pr393.convergence.source-owner',
  attemptRef: 'attempt.vexlife-prod.pr393.native-tools.source-owner.a010.20261119a010',
  requestRef: 'request.vexlife-prod.pr393.native-tools.source-owner.a010.20261119a010',
  correlationRef: 'correlation.vexlife-prod.pr393.native-tools.source-owner.a010.20261119a010',
  packageRef: 'package.vexlife-prod.pr393.native-tools.source-owner.a010.20261119a010',
  original: {
    originalIntent:
      'Compose the cleared Vex Labs experiment-admission path with the cleared provider-native tools/layered-context path on accepted Product main without creating a second Conversation, Memory, capability, or provider owner.',
    originalGoal:
      'Prove the exact PR #393 source-return v1 provider-carriage repair and its 4B direct-turn regression across the 48-path convergence candidate before fresh Assurance and lived continuity/Foundation pressure.',
    userProtectedIntent:
      'Preserve the existing ownership split: Lab TurnPolicy controls experiment admission; provider-native tools control representation; Capability Bridge, Conversation Source Return, Memory, Home, repository, web and Conversation owners retain effect/result truth.',
    sourceRefs: [
      'github.pr.vexlife-prod.393',
      'github.pr.vexlife-prod.393.comment.7015342954',
      'return.pr393.4b-post-source-return-d002.outer.1513204ad6aa4ee004eabe2405c14fa81376e46037a98a48d825880ac204d08b',
      'git.vexlife-prod.e00832bf3e28e2134da71d0191282ff47e2d7211',
      'git.vexlife-prod.main.146b0d482517201b85990a9d7b0b9ebf04075fd9',
      'source.vexrelay.current-contract/v1',
    ],
  },
  lane: {
    entryClass: 'SAME_LANE_SUCCESSOR',
    scopeMode: 'STAGE_BOUNDED',
    laneRef: 'lane.vexlife-prod.pr393.labs-native-tools-convergence',
    parentRootRef: 'github.pr.vexlife-prod.383',
    workRef: 'work.vexlife-prod.labs-native-tools-convergence.20261119',
    claimRef: 'claim.vexlife-prod.pr393.convergence-source-owner',
    returnRouteRef: 'github.pr.vexlife-prod.393',
    ownerRef: 'github.pr.vexlife-prod.383',
    rootHubRef: 'github.pr.vexlife-prod.383',
  },
  role: {
    activeRole: 'CODER',
    roleRef: 'role.coder.vexlife-prod.pr393.a010',
    instanceRef: 'instance.vexlife-prod.pr393.a010',
    occupancyRef: 'occupancy.vexlife-prod.pr393.a010',
    providerBindingRef: 'provider.macos-victor.vexlife-prod.pr393.a010',
    threadRef: 'thread.local-vex-operations.pr393.a010',
    authorityEnvelopeRef: 'authority-envelope.vexlife-prod.pr393.a010',
  },
  capabilities: {
    platformGitHubRead: 'AVAILABLE',
    platformGitHubWrite: 'UNAVAILABLE',
  },
  execution: {
    hostProfile: 'macos-victor',
    executionSurface: 'MAC_ZIP_RUN_COMMAND',
    singleUseNonce: 'nonce.vexlife-prod.pr393.a010.20261119a010',
    duplicateAttemptMarker: 'duplicate.vexlife-prod.pr393.a010.20261119a010',
    duplicateProtectionMode: 'OUTER_RELAY_BOUND',
    formedAt: now.toISOString(),
    expiresAtOrNull: expires.toISOString(),
  },
  workspace: {
    mode: 'RUN_ROOT',
    projectRef: 'project.vexlife-prod.pr393.convergence-source-owner',
    projectPath: 'workspace',
    createRoot: true,
    createProjectPath: true,
  },
  repositories: [{
    repositoryRef: 'primary',
    mode: 'CLONE_IN_WORKSPACE',
    cloneTransport: 'GITHUB_GH',
    githubHostname: 'github.com',
    nameWithOwner: 'VexGPT/VexLife-Prod',
    localPath: 'repositories/primary',
    remote: 'origin',
    baseBranch: 'main',
    acceptedBase: BASE,
    checkoutRef: HEAD,
    expectedHead: HEAD,
    branch: BRANCH,
  }],
  authority: {
    authorityRef: 'authority.vexlife-prod.pr393.convergence.source-owner.a010',
    authorityMode: 'EXACT_STAGE_EFFECTS',
    grantedEffects: [
      'CHECK_AUTH',
      'READ_REPOSITORY',
      'CHECK_STATE',
      'RUN_LOCAL_COMMAND',
      'QUERY_PROVIDER',
      'FORM_RESULT',
    ],
    conditionalEffects: [],
    consumedEffects: [],
    remainingEffects: [
      'CHECK_AUTH',
      'READ_REPOSITORY',
      'CHECK_STATE',
      'RUN_LOCAL_COMMAND',
      'QUERY_PROVIDER',
      'FORM_RESULT',
    ],
    heldEffects: [
      'WRITE_SOURCE',
      'PUSH_BRANCH',
      'CREATE_PULL_REQUEST',
      'UPDATE_COORDINATION_METADATA',
      'APPROVE_EXACT_HEAD',
      'MARK_READY',
      'ORDINARY_MERGE',
      'RELEASE_PUBLICATION',
      'PRODUCT_LAUNCH',
      'REAL_HOME_MUTATION',
      'MODEL_RUNTIME_EFFECT',
    ],
    repositoryMembranes: [{
      repositoryRef: 'primary',
      allowedPaths: CHANGED_PATHS,
    }],
    allowedExecutables: [
      '/bin/bash',
    ],
    allowedEnvironment: [
      'HOME',
      'PATH',
      'JAVA_HOME',
    ],
    remoteObservationOwner: 'Operations',
    allowMutationLoop: false,
    stopConditions: [
      'STOP_ON_UNKNOWN',
      'STOP_ON_PATH_ESCAPE',
      'STOP_ON_AUTH_BLOCK',
      'STOP_ON_TEST_FAILURE',
    ],
  },
  source: {
    sourceSetRef: 'source-set.vexlife-prod.pr393.labs-native-tools-convergence',
    sourceVersionRef: 'git.' + HEAD,
    overlapEvidenceRef: 'github.pr.vexlife-prod.383.comment.7008928686',
    activeConflictState: 'CONFLICT',
    candidateState: 'EXACT',
    candidateHead: HEAD,
    candidateTree: TREE,
    expectedChangedPaths: CHANGED_PATHS.map((p) => ({
      repositoryRef: 'primary',
      path: p,
    })),
    intentionallyUnchangedAuthorizedPaths: [],
  },
  policy: {
    maxOuterReturnFiles: 10,
    maxSelectedSourceEntries: 10,
    maxSelectedSourceBytes: 5000000,
    maxDiffBytes: 10000000,
    requireRacePreflightBeforeMutation: false,
    requireLocalValidationBeforePublish: false,
    requireInputPackageManifest: false,
    signoffCommit: true,
    draftPullRequest: true,
  },
};

const stage = {
  repositoryRef: 'primary',
  expectedBase: BASE,
  expectedHead: HEAD,
  expectedTree: TREE,
  branch: BRANCH,
  proofCommands: [{
    command: '/bin/bash',
    args: [
      './gradlew',
      'jvmTest',
      'compileKotlinJs',
      'compileTestKotlinJs',
      'checkVexLifeStrings',
      'generateVexLifeWebP1',
      '--stacktrace',
      '--no-daemon',
      '--max-workers=1',
      '-Dorg.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=768m -XX:ReservedCodeCacheSize=512m',
    ],
    predicateClass: 'SOURCE_OWNER_BEHAVIOR',
    predicateRef: 'proof.pr393.convergence.whole-source',
    timeoutMs: 900000,
  }],
  maxFiles: 10,
  stateRef: 'state.vexlife-prod.pr393.convergence.source-owner.a010',
  currentLifecycleState: 'SOURCE_READY',
  currentGoal:
    'Prove the exact #393 D002 source-return v1 provider-carriage repair: the 4B VEXLIFE_CONTROL_TEXT representation selected for initial prompt materialization remains identical through the initial provider-boundary reverify, and the direct-turn regression reaches the provider instead of failing ADMISSION_FAILED.',
  remainingGoals: [
    'Run fresh Independent Assurance over the exact Assurance-corrected #393 candidate.',
    'Run the real 4B / 15-turn continuity/Foundation pressure scenario again as lived A005 on the cleared repaired bytes.',
    'Run the already-recorded integrated Vex Labs configuration matrix after lived continuity pressure passes.',
    'Use those results for final lifecycle/merge judgment; do not infer READY from SourceOwner alone.',
  ],
  onPass: {
    lifecycleState: 'ASSURANCE_PENDING',
    nextFunction: 'F13.RUN_FRESH_ASSURANCE',
    nextOwner: 'IndependentAssurance',
    whyThisIsNext:
      'The Assurance-corrected integrated source candidate has executable SourceOwner evidence; semantic clearance must be freshly re-established on these exact bytes before lived 4B continuity/Foundation re-pressure on the repaired provider-boundary seam.',
  },
  resultFilename:
    'RETURN-VEXLIFE-PR393-CONVERGENCE-SOURCE-OWNER-A010.zip',
};

const authorityEnvelope = {
  authoritySchemaRef: 'vextreme.effect-transaction/v1',
  semanticAdmissionRef: 'github.pr.vexlife-prod.383',
  transactionRef: 'transaction.vexlife-prod.pr393.convergence.source-owner.a010',
  transactionVersion: 1,
  allowedEffectRefs: ['effect.vexrelay.execute'],
  heldEffectRefs: [
    'effect.source.mutation',
    'effect.publication',
    'effect.product.launch',
    'effect.home.mutation',
    'effect.model-runtime',
    'effect.lifecycle',
    'effect.release.publication',
  ],
};

const outerTaskTemplate = {
  schemaVersion: 'vextreme.vexlocalbridge-task-manifest/v1',
  artifactClass: 'EXECUTABLE_RELAY',
  taskRef: 'task.vexlife-prod.pr393.convergence.carrier',
  attemptRef: 'attempt.vexlife-prod.pr393.native-tools.carrier.a010.20261119a010',
  packageRef: 'package.vexlife-prod.pr393.convergence.carrier.a010.20261119a010',
  requestRef: 'request.vexlife-prod.pr393.convergence.carrier.a010.20261119a010',
  correlationRef: 'correlation.vexlife-prod.pr393.convergence.carrier.a010.20261119a010',
  taskStorageKey: 'task-vexlife-prod-pr393-convergence-carrier',
  attemptStorageKey: 'attempt-a010',
  requestingRoleRef: 'Operations[PR393-A010]',
  requestingInstanceRef: 'instance.local-vex-operations.pr393.a010',
  parentOperationsRef: 'github.pr.vexlife-prod.383',
  returnRouteRef: 'github.pr.vexlife-prod.393',
  hostProfileRef: 'macos-victor',
  repositoryExecutionProfileRefOrNull:
    'profile.repository.vextreme-sdk.macos-v1',
  repositoryRefOrNull: 'VexGPT/VexLife-Prod',
  baseRefOrNull: BASE,
  headRefOrNull: HEAD,
  treeRefOrNull: TREE,
  branchRefOrNull: BRANCH,
  claimRefOrNull: 'claim.vexlife-prod.pr393.convergence-source-owner',
  exactPathRefs: CHANGED_PATHS,
  authorityBinding: {
    authoritySchemaRef: 'vextreme.effect-transaction/v1',
    authorityEnvelopePath: 'authority/effect.json',
    authorityEnvelopeSha256: '0'.repeat(64),
    semanticAdmissionRef: 'github.pr.vexlife-prod.383',
    transactionRef: 'transaction.vexlife-prod.pr393.convergence.source-owner.a010',
    transactionVersion: 1,
    allowedEffectRefs: ['effect.vexrelay.execute'],
    heldEffectRefs: [
      'effect.source.mutation',
      'effect.publication',
      'effect.product.launch',
      'effect.home.mutation',
      'effect.model-runtime',
      'effect.lifecycle',
      'effect.release.publication',
    ],
  },
  payload: {
    payloadClass: 'VEXRELAY_TASK_ARCHIVE',
    payloadRuntime: 'NATIVE_VEXRELAY',
    payloadPath: 'payload/inner-vexrelay.zip',
    payloadSha256: '0'.repeat(64),
    typedArgv: [],
    immutableInputs: [{
      path: 'inputs/vexrelay-binding.json',
      sha256: '0'.repeat(64),
    }],
  },
  executionStateAtFormation: 'TASK_PREPARED',
  singleUseNonce: 'nonce.vexlife-prod.pr393.convergence.carrier.a010.20261119a010',
  stopConditions: [
    'stop.on.identity.drift',
    'stop.on.unadmitted.effect',
  ],
  archiveDigestBinding: 'FILENAME_SHA256',
};

const formed = await formVexRelayTaskArchiveCarrier({
  recipe: 'ExactHeadSourceOwnerProof',
  seed,
  stage,
  outerTaskTemplate,
  authorityEnvelope,
  outputDirectory: DOWNLOADS,
  outputStem:
    'VEXLIFE-PR393-CONVERGENCE-SOURCE-OWNER-A010',
  ttlMs: 60 * 60 * 1000,
});

fs.writeFileSync(
  OUT,
  JSON.stringify({
    outerZip: formed.outer.finalZip,
    outerSha256: formed.outer.digest,
    innerSha256: formed.inner.sha256,
    taskRef: formed.inner.manifest.taskRef,
    attemptRef: formed.inner.manifest.attemptRef,
    candidateHead: formed.inner.manifest.source.candidateHead,
    candidateTree: formed.inner.manifest.source.candidateTree,
    activeConflictState: formed.inner.manifest.source.activeConflictState,
    qualification: formed.outer.formationEvidence,
  }, null, 2) + '\n',
);
NODE_A010

echo 'A010: forming the exact current task/carrier...'
A010_SDK_ROOT="$SDK_ROOT" A010_FORMATION_RESULT="$FORMATION_RESULT" "$NODE_BIN" "$FORMATION_SCRIPT"

PACKAGE="$("$NODE_BIN" -e 'const fs=require("node:fs");const x=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));process.stdout.write(x.outerZip)' "$FORMATION_RESULT")"
EXPECTED="$("$NODE_BIN" -e 'const fs=require("node:fs");const x=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));process.stdout.write(x.outerSha256)' "$FORMATION_RESULT")"

[ -f "$PACKAGE" ] || { echo 'A010: formed package is missing.' >&2; exit 24; }
OBSERVED="$(/usr/bin/shasum -a 256 "$PACKAGE" | /usr/bin/cut -d ' ' -f 1)"
[ "$OBSERVED" = "$EXPECTED" ] || { echo 'A010: formed package digest mismatch.' >&2; exit 25; }

echo "A010: formed exact package $PACKAGE"
echo "A010: package sha256 $EXPECTED"
echo 'A010: entering current native VexRelay/VexLocalBridge carrier...'

set +e
VEX_EXPECTED_PACKAGE_SHA256="$EXPECTED" "$BASH_BIN" "$SDK_ROOT/lib/vexlocalbridge/templates/RUN-VEX-TASK.command" "$PACKAGE"
STATUS=$?
set -e

echo "A010: carrier exit $STATUS"
echo 'A010: if a canonical RETURN was formed, upload the exact RETURN ZIP revealed by VexLocalBridge.'
exit "$STATUS"
