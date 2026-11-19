#!/bin/bash
set -euo pipefail

CONTINUITY='[VXG RealForever]'
SDK_REPOSITORY='vgong24/Vextreme-SDK'
SDK_HEAD='8543c7d755faba04d4c4019c02e84f992667918a'
SDK_TREE='6376f68279f0750ee6d3868b8472368199763064'
PRODUCT_REPOSITORY='VexGPT/VexLife-Prod'
PRODUCT_BASE='152c323ee6c3d4a43243d0a9c9b6de2ce11f605b'
PRODUCT_HEAD='68f5235c3a3615a5cfa8d704e3af1f6560c35602'
PRODUCT_TREE='4b8313a874a5ec7377af5890d8b3a66da1019c97'
PRODUCT_BRANCH='VXG-111926-chatgpt-memory-depth-context-synthesis-a001'
ATTEMPT='A002'

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

BASH_BIN="$(resolve_exec /bin/bash)" || { echo 'A002: qualified Bash unavailable.' >&2; exit 10; }
GIT_BIN="$(resolve_exec /usr/bin/git)" || { echo 'A002: qualified Git unavailable.' >&2; exit 11; }
GH_BIN="$(resolve_exec /opt/homebrew/bin/gh /usr/local/bin/gh)" || { echo 'A002: qualified GitHub CLI unavailable.' >&2; exit 12; }
NODE_BIN="$(resolve_exec /opt/homebrew/bin/node /usr/local/bin/node /usr/bin/node)" || { echo 'A002: qualified Node unavailable.' >&2; exit 13; }
MKTEMP_BIN="$(resolve_exec /usr/bin/mktemp)" || { echo 'A002: mktemp unavailable.' >&2; exit 14; }
RM_BIN="$(resolve_exec /bin/rm /usr/bin/rm)" || { echo 'A002: rm unavailable.' >&2; exit 15; }

ARCH="$(/usr/bin/uname -m)"
[ "$ARCH" = 'arm64' ] || { echo "A002: expected Apple-silicon arm64 host, observed $ARCH." >&2; exit 16; }

"$GH_BIN" auth status --hostname github.com >/dev/null 2>&1 || {
  echo 'A002: GitHub authentication is required before this proof can run.' >&2
  echo 'Run: gh auth login --hostname github.com --web' >&2
  exit 17
}

ROOT="$("$MKTEMP_BIN" -d "$HOME/Downloads/.vexlife-pr383-a002.XXXXXX")"
SDK_ROOT="$ROOT/Vextreme-SDK"
FORMATION_RESULT="$ROOT/formation-result.json"
FORMATION_SCRIPT="$ROOT/form-a002.mjs"

cleanup() {
  case "$ROOT" in
    "$HOME"/Downloads/.vexlife-pr383-a002.*) "$RM_BIN" -rf -- "$ROOT" ;;
    *) return 1 ;;
  esac
}
trap cleanup EXIT HUP INT TERM

echo 'A002: acquiring exact current VexRelay/VexLocalBridge source...'
"$GH_BIN" repo clone "$SDK_REPOSITORY" "$SDK_ROOT" -- --no-checkout >/dev/null
"$GIT_BIN" -C "$SDK_ROOT" checkout --detach "$SDK_HEAD" >/dev/null

OBSERVED_HEAD="$("$GIT_BIN" -C "$SDK_ROOT" rev-parse HEAD)"
OBSERVED_TREE="$("$GIT_BIN" -C "$SDK_ROOT" rev-parse 'HEAD^{tree}')"
[ "$OBSERVED_HEAD" = "$SDK_HEAD" ] || { echo 'A002: SDK head mismatch.' >&2; exit 18; }
[ "$OBSERVED_TREE" = "$SDK_TREE" ] || { echo 'A002: SDK tree mismatch.' >&2; exit 19; }
[ -z "$("$GIT_BIN" -C "$SDK_ROOT" status --porcelain=v1 -uall)" ] || { echo 'A002: acquired SDK source is dirty.' >&2; exit 20; }

A002_SDK_ROOT="$SDK_ROOT" "$NODE_BIN" -e '
const fs=require("node:fs");
const path=require("node:path");
const root=process.env.A002_SDK_ROOT;
const contract=JSON.parse(fs.readFileSync(path.join(root,"docs/private-continuity/vexrelay/current-contract.json"),"utf8"));
if(contract.contractRef!=="source.vexrelay.current-contract/v1") process.exit(21);
const carrier=contract.acceptedAdapters.find(x=>x.adapterRef==="adapter.vexlocalbridge.native-vexrelay-task-archive/v1");
if(!carrier || carrier.state!=="TERMINAL_ACCEPTED_CURRENT") process.exit(22);
if(!contract.acceptedRecipeFamilies.includes("ExactHeadSourceOwnerProof")) process.exit(23);
'

cat > "$FORMATION_SCRIPT" <<'NODE_A002'
import fs from 'node:fs';
import path from 'node:path';
import { pathToFileURL } from 'node:url';

const SDK_ROOT = process.env.A002_SDK_ROOT;
const OUT = process.env.A002_FORMATION_RESULT;
const DOWNLOADS = path.join(process.env.HOME, 'Downloads');

const { formVexRelayTaskArchiveCarrier } =
  await import(pathToFileURL(path.join(
    SDK_ROOT,
    'lib/vexlocalbridge/vexrelay-task-archive-carrier-formation.mjs',
  )).href);

const BASE = '152c323ee6c3d4a43243d0a9c9b6de2ce11f605b';
const HEAD = '68f5235c3a3615a5cfa8d704e3af1f6560c35602';
const TREE = '4b8313a874a5ec7377af5890d8b3a66da1019c97';
const BRANCH = 'VXG-111926-chatgpt-memory-depth-context-synthesis-a001';

const CHANGED_PATHS = [
  'docs/foundation/CAPABILITY-ATLAS-SELF-FRAME.md',
  'docs/foundation/DIRECT-PERSONAL-COMPANION.md',
  'docs/foundation/FOUNDATIONAL-FIELD.md',
  'docs/work/memory-depth-context-synthesis/README.md',
  'src/commonMain/kotlin/vexlife/model/heretic/HereticCompanionRepresentation.kt',
  'src/commonMain/kotlin/vexlife/model/heretic/HereticModelImport.kt',
  'src/jvmMain/kotlin/vexlife/capability/VexLifeCapabilityAtlas.kt',
  'src/jvmMain/kotlin/vexlife/capability/VexLifeCapabilityBridge.kt',
  'src/jvmMain/kotlin/vexlife/capability/VexLifeReachDefinitions.kt',
  'src/jvmMain/kotlin/vexlife/capability/reach/VexLifeHomeReach.kt',
  'src/jvmMain/kotlin/vexlife/capability/reach/VexLifeMemoryRecall.kt',
  'src/jvmMain/kotlin/vexlife/companion/DirectCompanionConnection.kt',
  'src/jvmMain/kotlin/vexlife/companion/DirectCompanionTurn.kt',
  'src/jvmMain/kotlin/vexlife/companion/DirectPromptContext.kt',
  'src/jvmMain/kotlin/vexlife/companion/HarmonyCapabilityTools.kt',
  'src/jvmMain/kotlin/vexlife/companion/ProviderCapabilityTools.kt',
  'src/jvmMain/kotlin/vexlife/model/heretic/HuggingFaceModelInspection.kt',
  'src/jvmMain/kotlin/vexlife/model/heretic/runtime/HereticExecutionTopology.kt',
  'src/jvmMain/resources/foundational/vex-foundational-field.json',
  'src/jvmMain/resources/foundational/vex-foundational-field.revisions.json',
  'src/jvmTest/kotlin/vexlife/capability/VexLifeCapabilityAtlasTest.kt',
  'src/jvmTest/kotlin/vexlife/capability/VexLifeCapabilityReachTest.kt',
  'src/jvmTest/kotlin/vexlife/companion/DirectCompanionCapabilityTurnTest.kt',
  'src/jvmTest/kotlin/vexlife/companion/DirectCompanionTurnTest.kt',
  'src/jvmTest/kotlin/vexlife/companion/DirectPromptCurrentStateTest.kt',
  'src/jvmTest/kotlin/vexlife/companion/HarmonyCapabilityToolsTest.kt',
  'src/jvmTest/kotlin/vexlife/foundational/VexLifeFoundationalFieldTest.kt',
  'src/jvmTest/kotlin/vexlife/model/heretic/HuggingFaceModelInspectionTest.kt',
  'src/jvmTest/kotlin/vexlife/model/heretic/runtime/HereticExecutionTopologyTest.kt',
];

const now = new Date();
const expires = new Date(now.getTime() + 60 * 60 * 1000);

const seed = {
  schemaVersion: 'vex.relay.task/v2',
  continuity: '[VXG RealForever]',
  taskRef: 'task.vexlife-prod.pr383.native-tools.source-owner',
  attemptRef: 'attempt.vexlife-prod.pr383.native-tools.source-owner.a002.20261119a002',
  requestRef: 'request.vexlife-prod.pr383.native-tools.source-owner.a002.20261119a002',
  correlationRef: 'correlation.vexlife-prod.pr383.native-tools.source-owner.a002.20261119a002',
  packageRef: 'package.vexlife-prod.pr383.native-tools.source-owner.a002.20261119a002',
  original: {
    originalIntent:
      'Complete the Memory Depth reach/context relationship without making provider representation, prompt residency, or retrieval a second source owner.',
    originalGoal:
      'Prove exact PR #383 native-tool, layered-source, Memory pagination, prompt-context, Foundation, and execution-topology source behavior on the immutable candidate.',
    userProtectedIntent:
      'Preserve VexArchitecture ownership: Capability Atlas semantic meaning, Bridge/current admission and effect truth, rightful Conversation/Memory/Home/web/repository owners, and provider-specific representation only at the provider boundary.',
    sourceRefs: [
      'github.pr.vexlife-prod.383',
      'github.pr.vexlife-prod.388',
      'source.vexrelay.current-contract/v1',
      'github.pr.vexlife-prod.383.comment.7008928686',
      'github.pr.vexlife-prod.381.comment.7008929477',
      'return.pr383.a001.outer.5a66836cfa7c0611f06264a71fa389ba40d8cea783f827779cb89bd65834d829',
      'return.pr383.a001.inner.0e472848144a2c9322b40edba607801bc7699cf142a9e379e3e98f4e79b95743',
    ],
  },
  lane: {
    entryClass: 'SAME_LANE_SUCCESSOR',
    scopeMode: 'STAGE_BOUNDED',
    laneRef: 'lane.vexlife-prod.pr383.memory-depth-native-tools',
    parentRootRef: 'github.pr.vexlife-prod.383',
    workRef: 'work.vexlife-prod.native-tools-layered-context.20261119',
    claimRef: 'claim.vexlife-prod.pr383.native-tools-source-owner',
    returnRouteRef: 'github.pr.vexlife-prod.388',
    ownerRef: 'github.pr.vexlife-prod.383',
    rootHubRef: 'github.pr.vexlife-prod.383',
  },
  role: {
    activeRole: 'CODER',
    roleRef: 'role.coder.vexlife-prod.pr383.a002',
    instanceRef: 'instance.vexlife-prod.pr383.a002',
    occupancyRef: 'occupancy.vexlife-prod.pr383.a002',
    providerBindingRef: 'provider.macos-victor.vexlife-prod.pr383.a002',
    threadRef: 'thread.local-vex-operations.pr383.a002',
    authorityEnvelopeRef: 'authority-envelope.vexlife-prod.pr383.a002',
  },
  capabilities: {
    platformGitHubRead: 'AVAILABLE',
    platformGitHubWrite: 'UNAVAILABLE',
  },
  execution: {
    hostProfile: 'macos-victor',
    executionSurface: 'MAC_ZIP_RUN_COMMAND',
    singleUseNonce: 'nonce.vexlife-prod.pr383.a002.20261119a002',
    duplicateAttemptMarker: 'duplicate.vexlife-prod.pr383.a002.20261119a002',
    duplicateProtectionMode: 'OUTER_RELAY_BOUND',
    formedAt: now.toISOString(),
    expiresAtOrNull: expires.toISOString(),
  },
  workspace: {
    mode: 'RUN_ROOT',
    projectRef: 'project.vexlife-prod.pr383.source-owner',
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
    authorityRef: 'authority.vexlife-prod.pr383.source-owner.a002',
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
    sourceSetRef: 'source-set.vexlife-prod.pr383.memory-depth-native-tools',
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
    predicateRef: 'proof.pr383.native-tools.whole-source',
    timeoutMs: 900000,
  }],
  maxFiles: 10,
  stateRef: 'state.vexlife-prod.pr383.source-owner.a002',
  currentLifecycleState: 'SOURCE_READY',
  currentGoal:
    'Re-prove the corrected exact PR #383 native-tools/layered-context candidate after A001 found and source-corrected the nullable speakerScope compiler defect.',
  remainingGoals: [
    'Run fresh Independent Assurance over the exact proved PR #383 candidate.',
    'Compose the cleared #383 infrastructure with the already-proved #381 Labs/turn-policy candidate under the recorded representation-versus-admission boundary.',
    'Currentize the integrated candidate against final #338 runtime/release composition and run same-byte Product proof.',
  ],
  onPass: {
    lifecycleState: 'ASSURANCE_PENDING',
    nextFunction: 'F13.RUN_FRESH_ASSURANCE',
    nextOwner: 'IndependentAssurance',
    whyThisIsNext:
      'The immutable PR #383 source candidate has executable SourceOwner evidence; semantic clearance remains a separate fresh Assurance effect before integration.',
  },
  resultFilename:
    'RETURN-VEXLIFE-PR383-NATIVE-TOOLS-SOURCE-OWNER-A002.zip',
};

const authorityEnvelope = {
  authoritySchemaRef: 'vextreme.effect-transaction/v1',
  semanticAdmissionRef: 'github.pr.vexlife-prod.383',
  transactionRef: 'transaction.vexlife-prod.pr383.source-owner.a002',
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
  taskRef: 'task.vexlife-prod.pr383.native-tools.carrier',
  attemptRef: 'attempt.vexlife-prod.pr383.native-tools.carrier.a002.20261119a002',
  packageRef: 'package.vexlife-prod.pr383.native-tools.carrier.a002.20261119a002',
  requestRef: 'request.vexlife-prod.pr383.native-tools.carrier.a002.20261119a002',
  correlationRef: 'correlation.vexlife-prod.pr383.native-tools.carrier.a002.20261119a002',
  taskStorageKey: 'task-vexlife-prod-pr383-native-tools-carrier',
  attemptStorageKey: 'attempt-a002',
  requestingRoleRef: 'Operations[PR383-A002]',
  requestingInstanceRef: 'instance.local-vex-operations.pr383.a002',
  parentOperationsRef: 'github.pr.vexlife-prod.383',
  returnRouteRef: 'github.pr.vexlife-prod.388',
  hostProfileRef: 'macos-victor',
  repositoryExecutionProfileRefOrNull:
    'profile.repository.vextreme-sdk.macos-v1',
  repositoryRefOrNull: 'VexGPT/VexLife-Prod',
  baseRefOrNull: BASE,
  headRefOrNull: HEAD,
  treeRefOrNull: TREE,
  branchRefOrNull: BRANCH,
  claimRefOrNull: 'claim.vexlife-prod.pr383.native-tools-source-owner',
  exactPathRefs: CHANGED_PATHS,
  authorityBinding: {
    authoritySchemaRef: 'vextreme.effect-transaction/v1',
    authorityEnvelopePath: 'authority/effect.json',
    authorityEnvelopeSha256: '0'.repeat(64),
    semanticAdmissionRef: 'github.pr.vexlife-prod.383',
    transactionRef: 'transaction.vexlife-prod.pr383.source-owner.a002',
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
  singleUseNonce: 'nonce.vexlife-prod.pr383.carrier.a002.20261119a002',
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
    'VEXLIFE-PR383-NATIVE-TOOLS-SOURCE-OWNER-A002',
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
NODE_A002

echo 'A002: forming the exact current task/carrier...'
A002_SDK_ROOT="$SDK_ROOT" A002_FORMATION_RESULT="$FORMATION_RESULT" "$NODE_BIN" "$FORMATION_SCRIPT"

PACKAGE="$("$NODE_BIN" -e 'const fs=require("node:fs");const x=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));process.stdout.write(x.outerZip)' "$FORMATION_RESULT")"
EXPECTED="$("$NODE_BIN" -e 'const fs=require("node:fs");const x=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));process.stdout.write(x.outerSha256)' "$FORMATION_RESULT")"

[ -f "$PACKAGE" ] || { echo 'A002: formed package is missing.' >&2; exit 24; }
OBSERVED="$(/usr/bin/shasum -a 256 "$PACKAGE" | /usr/bin/cut -d ' ' -f 1)"
[ "$OBSERVED" = "$EXPECTED" ] || { echo 'A002: formed package digest mismatch.' >&2; exit 25; }

echo "A002: formed exact package $PACKAGE"
echo "A002: package sha256 $EXPECTED"
echo 'A002: entering current native VexRelay/VexLocalBridge carrier...'

set +e
VEX_EXPECTED_PACKAGE_SHA256="$EXPECTED" "$BASH_BIN" "$SDK_ROOT/lib/vexlocalbridge/templates/RUN-VEX-TASK.command" "$PACKAGE"
STATUS=$?
set -e

echo "A002: carrier exit $STATUS"
echo 'A002: if a canonical RETURN was formed, upload the exact RETURN ZIP revealed by VexLocalBridge.'
exit "$STATUS"
