#!/bin/bash
set -euo pipefail

CONTINUITY='[VXG RealForever]'
SDK_REPOSITORY='vgong24/Vextreme-SDK'
SDK_HEAD='8543c7d755faba04d4c4019c02e84f992667918a'
SDK_TREE='6376f68279f0750ee6d3868b8472368199763064'
PRODUCT_REPOSITORY='VexGPT/VexLife-Prod'
PRODUCT_BASE='b8bda514fae5e3ec0c0e0718dd48766e39cd6b45'
PRODUCT_HEAD='2cbf05a7481fd44d6c3d36b1c9902261215f0a86'
PRODUCT_TREE='6e8a5fb1c9c174bf23eca5df4ff435a6e10cf7eb'
PRODUCT_BRANCH='VXG-111926-chatgpt-mac-local-asr-provider-distribution-a001'
ATTEMPT='A008'

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

BASH_BIN="$(resolve_exec /bin/bash)" || { echo 'A008: qualified Bash unavailable.' >&2; exit 10; }
GIT_BIN="$(resolve_exec /usr/bin/git)" || { echo 'A008: qualified Git unavailable.' >&2; exit 11; }
GH_BIN="$(resolve_exec /opt/homebrew/bin/gh /usr/local/bin/gh)" || { echo 'A008: qualified GitHub CLI unavailable.' >&2; exit 12; }
NODE_BIN="$(resolve_exec /opt/homebrew/bin/node /usr/local/bin/node /usr/bin/node)" || { echo 'A008: qualified Node unavailable.' >&2; exit 13; }
MKTEMP_BIN="$(resolve_exec /usr/bin/mktemp)" || { echo 'A008: mktemp unavailable.' >&2; exit 14; }
RM_BIN="$(resolve_exec /bin/rm /usr/bin/rm)" || { echo 'A008: rm unavailable.' >&2; exit 15; }

ARCH="$(/usr/bin/uname -m)"
[ "$ARCH" = 'arm64' ] || { echo "A008: expected Apple-silicon arm64 host, observed $ARCH." >&2; exit 16; }

"$GH_BIN" auth status --hostname github.com >/dev/null 2>&1 || {
  echo 'A008: GitHub authentication is required before this proof can run.' >&2
  echo 'Run: gh auth login --hostname github.com --web' >&2
  exit 17
}

ROOT="$("$MKTEMP_BIN" -d "$HOME/Downloads/.vexlife-pr384-a008.XXXXXX")"
SDK_ROOT="$ROOT/Vextreme-SDK"
FORMATION_RESULT="$ROOT/formation-result.json"
FORMATION_SCRIPT="$ROOT/form-a008.mjs"

cleanup() {
  case "$ROOT" in
    "$HOME"/Downloads/.vexlife-pr384-a008.*) "$RM_BIN" -rf -- "$ROOT" ;;
    *) return 1 ;;
  esac
}
trap cleanup EXIT HUP INT TERM

echo 'A008: acquiring exact current VexRelay/VexLocalBridge source...'
"$GH_BIN" repo clone "$SDK_REPOSITORY" "$SDK_ROOT" -- --no-checkout >/dev/null
"$GIT_BIN" -C "$SDK_ROOT" checkout --detach "$SDK_HEAD" >/dev/null

OBSERVED_HEAD="$("$GIT_BIN" -C "$SDK_ROOT" rev-parse HEAD)"
OBSERVED_TREE="$("$GIT_BIN" -C "$SDK_ROOT" rev-parse 'HEAD^{tree}')"
[ "$OBSERVED_HEAD" = "$SDK_HEAD" ] || { echo 'A008: SDK head mismatch.' >&2; exit 18; }
[ "$OBSERVED_TREE" = "$SDK_TREE" ] || { echo 'A008: SDK tree mismatch.' >&2; exit 19; }
[ -z "$("$GIT_BIN" -C "$SDK_ROOT" status --porcelain=v1 -uall)" ] || { echo 'A008: acquired SDK source is dirty.' >&2; exit 20; }

A008_SDK_ROOT="$SDK_ROOT" "$NODE_BIN" -e '
const fs=require("node:fs");
const path=require("node:path");
const root=process.env.A008_SDK_ROOT;
const contract=JSON.parse(fs.readFileSync(path.join(root,"docs/private-continuity/vexrelay/current-contract.json"),"utf8"));
if(contract.contractRef!=="source.vexrelay.current-contract/v1") process.exit(21);
const carrier=contract.acceptedAdapters.find(x=>x.adapterRef==="adapter.vexlocalbridge.native-vexrelay-task-archive/v1");
if(!carrier || carrier.state!=="TERMINAL_ACCEPTED_CURRENT") process.exit(22);
'

cat > "$FORMATION_SCRIPT" <<'NODE_A008'
import fs from 'node:fs';
import path from 'node:path';
import { pathToFileURL } from 'node:url';

const SDK_ROOT = process.env.A008_SDK_ROOT;
const OUT = process.env.A008_FORMATION_RESULT;
const DOWNLOADS = path.join(process.env.HOME, 'Downloads');

const { formVexRelayTaskArchiveCarrier } =
  await import(pathToFileURL(path.join(
    SDK_ROOT,
    'lib/vexlocalbridge/vexrelay-task-archive-carrier-formation.mjs',
  )).href);

const BASE = 'b8bda514fae5e3ec0c0e0718dd48766e39cd6b45';
const HEAD = '2cbf05a7481fd44d6c3d36b1c9902261215f0a86';
const TREE = '6e8a5fb1c9c174bf23eca5df4ff435a6e10cf7eb';
const BRANCH = 'VXG-111926-chatgpt-mac-local-asr-provider-distribution-a001';
const CHANGED_PATHS = [
  '.github/workflows/mac-app-proof.yml',
  'build.gradle.kts',
  'packaging/private-service-image/bin/vexlife-prod-service',
  'packaging/private-service-image/local-asr-provider-contract.json',
  'src/jvmMain/kotlin/vexlife/localasr/VexLifeLocalAsrMacArm64ProviderRealization.kt',
  'src/jvmMain/kotlin/vexlife/localasr/VexLifeLocalAsrProvider.kt',
  'src/jvmMain/kotlin/vexlife/packaging/VexLifeProdPrivateServiceImage.kt',
  'src/jvmTest/kotlin/vexlife/localasr/VexLifeLocalAsrProviderTest.kt',
  'src/jvmTest/kotlin/vexlife/packaging/VexLifeProdPrivateServiceImageTest.kt',
];

const now = new Date();
const expires = new Date(now.getTime() + 60 * 60 * 1000);

const seed = {
  schemaVersion: 'vex.relay.task/v2',
  continuity: '[VXG RealForever]',
  taskRef: 'task.vexlife-prod.pr384.mac-local-asr.source-owner',
  attemptRef: 'attempt.vexlife-prod.pr384.mac-local-asr.source-owner.a008.20261119a008',
  requestRef: 'request.vexlife-prod.pr384.mac-local-asr.source-owner.a008.20261119a008',
  correlationRef: 'correlation.vexlife-prod.pr384.mac-local-asr.source-owner.a008.20261119a008',
  packageRef: 'package.vexlife-prod.pr384.mac-local-asr.source-owner.a008.20261119a008',
  original: {
    originalIntent:
      'Restore Mac VoiceInput by making the shared Local-ASR provider honor the selected realization without changing Windows provider behavior.',
    originalGoal:
      'Prove the PR #384 interface-dispatch correction that routes provider currentness through the selected realization, then return to final #338 composition.',
    userProtectedIntent:
      'Preserve the proven Windows realization unchanged, keep Product source authority in PR #384, avoid Local Workspace and lived effects during source proof, and return one canonical result.',
    sourceRefs: [
      'github.pr.vexlife-prod.384',
      'github.pr.vexlife-prod.386',
      'github.pr.vexlife-prod.390',
      'source.vexrelay.current-contract/v1',
      'return.pr384.a007.inner.1cd6475793e73704ff98a028f5d81104246f5615d728ba206c73e15ea411742d',
      'commit.vexlife-prod.df141f2f841ca9ad666893b140a71df5655df679',
    ]
  },
  lane: {
    entryClass: 'SAME_LANE_SUCCESSOR',
    scopeMode: 'STAGE_BOUNDED',
    laneRef: 'lane.vexlife-prod.pr384.mac-local-asr-distribution',
    parentRootRef: 'github.pr.vexlife-prod.338',
    workRef: 'work.vexlife-prod.pr384.mac-local-asr-distribution',
    claimRef: 'claim.vexlife-prod.pr384.mac-local-asr-source-owner',
    returnRouteRef: 'github.pr.vexlife-prod.386',
    ownerRef: 'github.pr.vexlife-prod.384',
    rootHubRef: 'github.pr.vexlife-prod.338',
  },
  role: {
    activeRole: 'CODER',
    roleRef: 'role.coder.vexlife-prod.pr384.a008',
    instanceRef: 'instance.vexlife-prod.pr384.a008',
    occupancyRef: 'occupancy.vexlife-prod.pr384.a008',
    providerBindingRef: 'provider.macos-victor.vexlife-prod.pr384.a008',
    threadRef: 'thread.local-vex-operations.pr384.a008',
    authorityEnvelopeRef: 'authority-envelope.vexlife-prod.pr384.a008',
  },
  capabilities: {
    platformGitHubRead: 'AVAILABLE',
    platformGitHubWrite: 'UNAVAILABLE',
  },
  execution: {
    hostProfile: 'macos-victor',
    executionSurface: 'MAC_ZIP_RUN_COMMAND',
    singleUseNonce: 'nonce.vexlife-prod.pr384.a008.20261119a008',
    duplicateAttemptMarker: 'duplicate.vexlife-prod.pr384.a008.20261119a008',
    duplicateProtectionMode: 'OUTER_RELAY_BOUND',
    formedAt: now.toISOString(),
    expiresAtOrNull: expires.toISOString(),
  },
  workspace: {
    mode: 'RUN_ROOT',
    projectRef: 'project.vexlife-prod.pr384.source-owner',
    projectPath: 'workspace',
    createRoot: true,
    createProjectPath: true,
  },
  repositories: [{
    repositoryRef: 'primary',
    mode: 'CLONE_IN_WORKSPACE',
    cloneTransport: 'GIT',
    cloneUrl: 'https://github.com/VexGPT/VexLife-Prod.git',
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
    authorityRef: 'authority.vexlife-prod.pr384.source-owner.a008',
    authorityMode: 'EXACT_STAGE_EFFECTS',
    grantedEffects: [
      'READ_REPOSITORY',
      'CHECK_STATE',
      'RUN_LOCAL_COMMAND',
      'QUERY_PROVIDER',
      'FORM_RESULT',
    ],
    conditionalEffects: [],
    consumedEffects: [],
    remainingEffects: [
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
      'APPROVE_EXACT_HEAD',
      'MARK_READY',
      'ORDINARY_MERGE',
      'RELEASE_PUBLICATION',
      'REAL_HOME_MUTATION',
      'MICROPHONE',
    ],
    repositoryMembranes: [{
      repositoryRef: 'primary',
      allowedPaths: CHANGED_PATHS,
    }],
    allowedExecutables: [
      '/bin/bash',
    ]
    allowedEnvironment: [
      'HOME',
      'PATH',
      'DYLD_LIBRARY_PATH',
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
    sourceSetRef: 'source-set.vexlife-prod.pr384.mac-local-asr',
    sourceVersionRef: 'git.' + HEAD,
    overlapEvidenceRef: 'github.pr.vexlife-prod.384.bounded-nine-path-scope',
    activeConflictState: 'NONE',
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
  proofCommands: [
    {
      command: '/bin/bash',
      args: [
        '-lc',
        "set -euo pipefail; FILE='src/jvmMain/kotlin/vexlife/localasr/VexLifeLocalAsrProvider.kt'; ! /usr/bin/grep -F 'VexLifeLocalAsrProviderMaterialization.observe(' \"$FILE\"; /usr/bin/grep -F 'realization.observeProvider(' \"$FILE\" >/dev/null"
      ],
      predicateClass: 'SOURCE_OWNER_REPOSITORY',
      predicateRef: 'proof.pr384.mac-local-asr.realization-dispatch-structure',
      timeoutMs: 30000,
    },
    {
      command: '/bin/bash',
      args: [
        '-lc',
        "./gradlew --no-daemon --max-workers=1 -Dorg.gradle.jvmargs='-Xmx2048m -XX:MaxMetaspaceSize=768m -XX:ReservedCodeCacheSize=512m' compileKotlinJvm compileTestKotlinJvm && ./gradlew --no-daemon --max-workers=1 -Dorg.gradle.jvmargs='-Xmx2048m -XX:MaxMetaspaceSize=768m -XX:ReservedCodeCacheSize=512m' jvmTest --tests vexlife.localasr.VexLifeLocalAsrProviderTest --tests vexlife.service.VexLifeProdLocalServiceTest.localAsrSelectionProjectsStorageLawfulnessWithoutPretendingAbsentModelsAreUsable --stacktrace"
      ],
      predicateClass: 'SOURCE_OWNER_BEHAVIOR',
      predicateRef: 'proof.pr384.mac-local-asr.realization-dispatch-focused-jvm',
      timeoutMs: 900000,
    },
  ],
  maxFiles: 9,
  stateRef: 'state.vexlife-prod.pr384.source-owner.a008',
  currentLifecycleState: 'SOURCE_READY',
  currentGoal:
    'Prove that shared Local-ASR current/prepare/transcribe dispatches provider placement through the selected realization, preserving Windows while restoring Mac bundled-provider currentness.',
  remainingGoals: [
    'Fast-forward final #338 to the corrected #384 head without rewriting source history.',
    'Resume the final Mac lived mic / Conversation / reopen regression on those exact bytes.',
    'Preserve the already-earned Windows checkpoint unless the corrected shared seam changes Windows evidence.',
  ],
  onPass: {
    lifecycleState: 'SOURCE_READY',
    nextFunction: 'F12.FORM_CONTINUATION',
    nextOwner: 'Operations',
    whyThisIsNext:
      'The interface-dispatch correction is source-proven; currentize #338 to this exact head and resume the held final Mac lived regression.',
  },
  resultFilename:
    'RETURN-VEXLIFE-PR384-MAC-LOCAL-ASR-INTERFACE-DISPATCH-A008.zip',
};

const authorityEnvelope = {
  authoritySchemaRef: 'vextreme.effect-transaction/v1',
  semanticAdmissionRef: 'github.pr.vexlife-prod.384',
  transactionRef: 'transaction.vexlife-prod.pr384.source-owner.a008',
  transactionVersion: 1,
  allowedEffectRefs: ['effect.vexrelay.execute'],
  heldEffectRefs: [
    'effect.product.launch',
    'effect.home.mutation',
    'effect.microphone',
    'effect.lifecycle',
    'effect.release.publication',
  ],
};

const outerTaskTemplate = {
  schemaVersion: 'vextreme.vexlocalbridge-task-manifest/v1',
  artifactClass: 'EXECUTABLE_RELAY',
  taskRef: 'task.vexlife-prod.pr384.mac-local-asr.carrier',
  attemptRef: 'attempt.vexlife-prod.pr384.mac-local-asr.carrier.a008.20261119a008',
  packageRef: 'package.vexlife-prod.pr384.mac-local-asr.carrier.a008.20261119a008',
  requestRef: 'request.vexlife-prod.pr384.mac-local-asr.carrier.a008.20261119a008',
  correlationRef: 'correlation.vexlife-prod.pr384.mac-local-asr.carrier.a008.20261119a008',
  taskStorageKey: 'task-vexlife-prod-pr384-mac-local-asr-carrier',
  attemptStorageKey: 'attempt-a008',
  requestingRoleRef: 'Operations[PR384-A008]',
  requestingInstanceRef: 'instance.local-vex-operations.pr384.a008',
  parentOperationsRef: 'github.pr.vexlife-prod.338',
  returnRouteRef: 'github.pr.vexlife-prod.386',
  hostProfileRef: 'macos-victor',
  repositoryExecutionProfileRefOrNull:
    'profile.repository.vextreme-sdk.macos-v1',
  repositoryRefOrNull: 'VexGPT/VexLife-Prod',
  baseRefOrNull: BASE,
  headRefOrNull: HEAD,
  treeRefOrNull: TREE,
  branchRefOrNull: BRANCH,
  claimRefOrNull: 'claim.vexlife-prod.pr384.mac-local-asr-source-owner',
  exactPathRefs: CHANGED_PATHS,
  authorityBinding: {
    authoritySchemaRef: 'vextreme.effect-transaction/v1',
    authorityEnvelopePath: 'authority/effect.json',
    authorityEnvelopeSha256: '0'.repeat(64),
    semanticAdmissionRef: 'github.pr.vexlife-prod.384',
    transactionRef: 'transaction.vexlife-prod.pr384.source-owner.a008',
    transactionVersion: 1,
    allowedEffectRefs: ['effect.vexrelay.execute'],
    heldEffectRefs: [
      'effect.product.launch',
      'effect.home.mutation',
      'effect.microphone',
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
  singleUseNonce: 'nonce.vexlife-prod.pr384.carrier.a008.20261119a008',
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
    'VEXLIFE-PR384-MAC-LOCAL-ASR-INTERFACE-DISPATCH-A008',
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
    qualification: formed.outer.formationEvidence,
  }, null, 2) + '\n',
);
NODE_A008

echo 'A008: forming the exact current task/carrier...'
A008_SDK_ROOT="$SDK_ROOT" A008_FORMATION_RESULT="$FORMATION_RESULT" "$NODE_BIN" "$FORMATION_SCRIPT"

PACKAGE="$("$NODE_BIN" -e 'const fs=require("node:fs");const x=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));process.stdout.write(x.outerZip)' "$FORMATION_RESULT")"
EXPECTED="$("$NODE_BIN" -e 'const fs=require("node:fs");const x=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));process.stdout.write(x.outerSha256)' "$FORMATION_RESULT")"

[ -f "$PACKAGE" ] || { echo 'A008: formed package is missing.' >&2; exit 23; }
OBSERVED="$(/usr/bin/shasum -a 256 "$PACKAGE" | /usr/bin/cut -d ' ' -f 1)"
[ "$OBSERVED" = "$EXPECTED" ] || { echo 'A008: formed package digest mismatch.' >&2; exit 24; }

echo "A008: formed exact package $PACKAGE"
echo "A008: package sha256 $EXPECTED"
echo 'A008: entering current native VexRelay/VexLocalBridge carrier...'

set +e
VEX_EXPECTED_PACKAGE_SHA256="$EXPECTED" "$BASH_BIN" "$SDK_ROOT/lib/vexlocalbridge/templates/RUN-VEX-TASK.command" "$PACKAGE"
STATUS=$?
set -e

echo "A008: carrier exit $STATUS"
echo 'A008: if a canonical RETURN was formed, upload the exact RETURN ZIP revealed by VexLocalBridge.'
exit "$STATUS"
