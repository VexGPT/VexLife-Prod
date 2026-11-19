#!/bin/bash
set -euo pipefail

CONTINUITY='[VXG RealForever]'
SDK_REPOSITORY='vgong24/Vextreme-SDK'
SDK_HEAD='8543c7d755faba04d4c4019c02e84f992667918a'
SDK_TREE='6376f68279f0750ee6d3868b8472368199763064'
PRODUCT_REPOSITORY='VexGPT/VexLife-Prod'
PRODUCT_BASE='146b0d482517201b85990a9d7b0b9ebf04075fd9'
PRODUCT_HEAD='c646f8d10cd99ed4891f56ec2031269762cd38ae'
PRODUCT_TREE='2c402f16fe891a1fe66d5f0f25cc8f20ca135fc1'
PRODUCT_BRANCH='VXG-111926-chatgpt-labs-native-tools-convergence-a001'
ATTEMPT='A007'

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

BASH_BIN="$(resolve_exec /bin/bash)" || { echo 'A007: qualified Bash unavailable.' >&2; exit 10; }
GIT_BIN="$(resolve_exec /usr/bin/git)" || { echo 'A007: qualified Git unavailable.' >&2; exit 11; }
GH_BIN="$(resolve_exec /opt/homebrew/bin/gh /usr/local/bin/gh)" || { echo 'A007: qualified GitHub CLI unavailable.' >&2; exit 12; }
NODE_BIN="$(resolve_exec /opt/homebrew/bin/node /usr/local/bin/node /usr/bin/node)" || { echo 'A007: qualified Node unavailable.' >&2; exit 13; }
MKTEMP_BIN="$(resolve_exec /usr/bin/mktemp)" || { echo 'A007: mktemp unavailable.' >&2; exit 14; }
RM_BIN="$(resolve_exec /bin/rm /usr/bin/rm)" || { echo 'A007: rm unavailable.' >&2; exit 15; }

ARCH="$(/usr/bin/uname -m)"
[ "$ARCH" = 'arm64' ] || { echo "A007: expected Apple-silicon arm64 host, observed $ARCH." >&2; exit 16; }

"$GH_BIN" auth status --hostname github.com >/dev/null 2>&1 || {
  echo 'A007: GitHub authentication is required before this proof can run.' >&2
  echo 'Run: gh auth login --hostname github.com --web' >&2
  exit 17
}

ROOT="$("$MKTEMP_BIN" -d "$HOME/Downloads/.vexlife-pr393-convergence-a007.XXXXXX")"
SDK_ROOT="$ROOT/Vextreme-SDK"
FORMATION_RESULT="$ROOT/formation-result.json"
FORMATION_SCRIPT="$ROOT/form-pr393-a007.mjs"

cleanup() {
  case "$ROOT" in
    "$HOME"/Downloads/.vexlife-pr393-convergence-a007.*) "$RM_BIN" -rf -- "$ROOT" ;;
    *) return 1 ;;
  esac
}
trap cleanup EXIT HUP INT TERM

echo 'A007: acquiring exact current VexRelay/VexLocalBridge source...'
"$GH_BIN" repo clone "$SDK_REPOSITORY" "$SDK_ROOT" -- --no-checkout >/dev/null
"$GIT_BIN" -C "$SDK_ROOT" checkout --detach "$SDK_HEAD" >/dev/null

OBSERVED_HEAD="$("$GIT_BIN" -C "$SDK_ROOT" rev-parse HEAD)"
OBSERVED_TREE="$("$GIT_BIN" -C "$SDK_ROOT" rev-parse 'HEAD^{tree}')"
[ "$OBSERVED_HEAD" = "$SDK_HEAD" ] || { echo 'A007: SDK head mismatch.' >&2; exit 18; }
[ "$OBSERVED_TREE" = "$SDK_TREE" ] || { echo 'A007: SDK tree mismatch.' >&2; exit 19; }
[ -z "$("$GIT_BIN" -C "$SDK_ROOT" status --porcelain=v1 -uall)" ] || { echo 'A007: acquired SDK source is dirty.' >&2; exit 20; }

A007_SDK_ROOT="$SDK_ROOT" "$NODE_BIN" -e '
const fs=require("node:fs");
const path=require("node:path");
const root=process.env.A007_SDK_ROOT;
const contract=JSON.parse(fs.readFileSync(path.join(root,"docs/private-continuity/vexrelay/current-contract.json"),"utf8"));
if(contract.contractRef!=="source.vexrelay.current-contract/v1") process.exit(21);
const carrier=contract.acceptedAdapters.find(x=>x.adapterRef==="adapter.vexlocalbridge.native-vexrelay-task-archive/v1");
if(!carrier || carrier.state!=="TERMINAL_ACCEPTED_CURRENT") process.exit(22);
if(!contract.acceptedRecipeFamilies.includes("ExactHeadSourceOwnerProof")) process.exit(23);
'

cat > "$FORMATION_SCRIPT" <<'NODE_A007'
import fs from 'node:fs';
import path from 'node:path';
import { pathToFileURL } from 'node:url';

const SDK_ROOT = process.env.A007_SDK_ROOT;
const OUT = process.env.A007_FORMATION_RESULT;
const DOWNLOADS = path.join(process.env.HOME, 'Downloads');

const { formVexRelayTaskArchiveCarrier } =
  await import(pathToFileURL(path.join(
    SDK_ROOT,
    'lib/vexlocalbridge/vexrelay-task-archive-carrier-formation.mjs',
  )).href);

const BASE = '146b0d482517201b85990a9d7b0b9ebf04075fd9';
const HEAD = 'c646f8d10cd99ed4891f56ec2031269762cd38ae';
const TREE = '2c402f16fe891a1fe66d5f0f25cc8f20ca135fc1';
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
  attemptRef: 'attempt.vexlife-prod.pr393.native-tools.source-owner.a007.20261119a007',
  requestRef: 'request.vexlife-prod.pr393.native-tools.source-owner.a007.20261119a007',
  correlationRef: 'correlation.vexlife-prod.pr393.native-tools.source-owner.a007.20261119a007',
  packageRef: 'package.vexlife-prod.pr393.native-tools.source-owner.a007.20261119a007',
  original: {
    originalIntent:
      'Compose the cleared Vex Labs experiment-admission path with the cleared provider-native tools/layered-context path on accepted Product main without creating a second Conversation, Memory, capability, or provider owner.',
    originalGoal:
      'Prove exact PR #393 integrated source behavior across the 46-path convergence candidate before fresh Assurance and lived continuity/Foundation pressure.',
    userProtectedIntent:
      'Preserve the existing ownership split: Lab TurnPolicy controls experiment admission; provider-native tools control representation; Capability Bridge, Conversation Source Return, Memory, Home, repository, web and Conversation owners retain effect/result truth.',
    sourceRefs: [
      'github.pr.vexlife-prod.393',
      'return.pr393.sourceowner-a006.outer.ea07ebf0cb313c8a0d2879308f5e09942551b3493f0eaa6ad4be4584426b02ab',
      'return.pr393.sourceowner-a006.inner.2447e69b6f79679cfe14dd99d3f666f2b97ca54787cfdd7ed299145ea1d4c865',
      'finding.pr393.sourceowner-a006.test-import.assertFalse',
      'return.pr393.sourceowner-a005.outer.e2e94df48f0853f7328454d025999d0d3d5ab52b7838f33639654420e9d20b0a',
      'return.pr393.sourceowner-a005.inner.61bc7befa920c3005ca91cfdce6ec3cf005298555f9fcb244a7a6bfe1d1d7b56',
      'assurance.pr393.8abb3cc8.HOLD.fallback-reason-truth',
      'return.pr393.a001.outer.bfd6a9e26707af150e1cc2cffca55cdb554cce691c85e4f9e3a28e300d9fc710',
      'return.pr393.a001.inner.b0e57de26d9c0b5550fbc4551d2fadfe5772630d2492e2ea92587f62902f9318',
      'github.pr.vexlife-prod.393.comment.7011244205',
      'github.pr.vexlife-prod.381',
      'github.pr.vexlife-prod.381.comment.7007978712',
      'github.pr.vexlife-prod.383',
      'github.pr.vexlife-prod.383.comment.7010327188',
      'github.pr.vexlife-prod.383.comment.7010273139',
      'github.pr.vexlife-prod.338',
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
    roleRef: 'role.coder.vexlife-prod.pr393.a007',
    instanceRef: 'instance.vexlife-prod.pr393.a007',
    occupancyRef: 'occupancy.vexlife-prod.pr393.a007',
    providerBindingRef: 'provider.macos-victor.vexlife-prod.pr393.a007',
    threadRef: 'thread.local-vex-operations.pr393.a007',
    authorityEnvelopeRef: 'authority-envelope.vexlife-prod.pr393.a007',
  },
  capabilities: {
    platformGitHubRead: 'AVAILABLE',
    platformGitHubWrite: 'UNAVAILABLE',
  },
  execution: {
    hostProfile: 'macos-victor',
    executionSurface: 'MAC_ZIP_RUN_COMMAND',
    singleUseNonce: 'nonce.vexlife-prod.pr393.a007.20261119a007',
    duplicateAttemptMarker: 'duplicate.vexlife-prod.pr393.a007.20261119a007',
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
    authorityRef: 'authority.vexlife-prod.pr393.convergence.source-owner.a007',
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
  stateRef: 'state.vexlife-prod.pr393.convergence.source-owner.a007',
  currentLifecycleState: 'SOURCE_READY',
  currentGoal:
    'Prove the exact #393 correction after lived 4B A007 showed finish_reason=tool_calls without a consumable structured tool call; Qwen 4B now uses the existing text-control representation and native OpenAI tool carriage fails closed when that provider state lacks a consumable call.',
  remainingGoals: [
    'Run fresh Independent Assurance over the exact Assurance-corrected #393 candidate.',
    'Run the real 4B / 15-turn continuity/Foundation pressure scenario again as lived A004 on the cleared corrected bytes.',
    'Run the already-recorded integrated Vex Labs configuration matrix after lived continuity pressure passes.',
    'Use those results for final lifecycle/merge judgment; do not infer READY from SourceOwner alone.',
  ],
  onPass: {
    lifecycleState: 'ASSURANCE_PENDING',
    nextFunction: 'F13.RUN_FRESH_ASSURANCE',
    nextOwner: 'IndependentAssurance',
    whyThisIsNext:
      'The Assurance-corrected integrated source candidate has executable SourceOwner evidence; semantic clearance must be freshly re-established on these exact bytes before lived 4B continuity/Foundation re-pressure.',
  },
  resultFilename:
    'RETURN-VEXLIFE-PR393-CONVERGENCE-SOURCE-OWNER-A007.zip',
};

const authorityEnvelope = {
  authoritySchemaRef: 'vextreme.effect-transaction/v1',
  semanticAdmissionRef: 'github.pr.vexlife-prod.383',
  transactionRef: 'transaction.vexlife-prod.pr393.convergence.source-owner.a007',
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
  attemptRef: 'attempt.vexlife-prod.pr393.native-tools.carrier.a007.20261119a007',
  packageRef: 'package.vexlife-prod.pr393.convergence.carrier.a007.20261119a007',
  requestRef: 'request.vexlife-prod.pr393.convergence.carrier.a007.20261119a007',
  correlationRef: 'correlation.vexlife-prod.pr393.convergence.carrier.a007.20261119a007',
  taskStorageKey: 'task-vexlife-prod-pr393-convergence-carrier',
  attemptStorageKey: 'attempt-a007',
  requestingRoleRef: 'Operations[PR393-A007]',
  requestingInstanceRef: 'instance.local-vex-operations.pr393.a007',
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
    transactionRef: 'transaction.vexlife-prod.pr393.convergence.source-owner.a007',
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
  singleUseNonce: 'nonce.vexlife-prod.pr393.convergence.carrier.a007.20261119a007',
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
    'VEXLIFE-PR393-CONVERGENCE-SOURCE-OWNER-A007',
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
NODE_A007

echo 'A007: forming the exact current task/carrier...'
A007_SDK_ROOT="$SDK_ROOT" A007_FORMATION_RESULT="$FORMATION_RESULT" "$NODE_BIN" "$FORMATION_SCRIPT"

PACKAGE="$("$NODE_BIN" -e 'const fs=require("node:fs");const x=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));process.stdout.write(x.outerZip)' "$FORMATION_RESULT")"
EXPECTED="$("$NODE_BIN" -e 'const fs=require("node:fs");const x=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));process.stdout.write(x.outerSha256)' "$FORMATION_RESULT")"

[ -f "$PACKAGE" ] || { echo 'A007: formed package is missing.' >&2; exit 24; }
OBSERVED="$(/usr/bin/shasum -a 256 "$PACKAGE" | /usr/bin/cut -d ' ' -f 1)"
[ "$OBSERVED" = "$EXPECTED" ] || { echo 'A007: formed package digest mismatch.' >&2; exit 25; }

echo "A007: formed exact package $PACKAGE"
echo "A007: package sha256 $EXPECTED"
echo 'A007: entering current native VexRelay/VexLocalBridge carrier...'

set +e
VEX_EXPECTED_PACKAGE_SHA256="$EXPECTED" "$BASH_BIN" "$SDK_ROOT/lib/vexlocalbridge/templates/RUN-VEX-TASK.command" "$PACKAGE"
STATUS=$?
set -e

echo "A007: carrier exit $STATUS"
echo 'A007: if a canonical RETURN was formed, upload the exact RETURN ZIP revealed by VexLocalBridge.'
exit "$STATUS"
