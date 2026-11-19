#!/bin/bash
set -euo pipefail

CONTINUITY='[VXG RealForever]'
SDK_REPOSITORY='vgong24/Vextreme-SDK'
SDK_HEAD='8543c7d755faba04d4c4019c02e84f992667918a'
SDK_TREE='6376f68279f0750ee6d3868b8472368199763064'
PRODUCT_REPOSITORY='VexGPT/VexLife-Prod'
PRODUCT_BASE='b8bda514fae5e3ec0c0e0718dd48766e39cd6b45'
PRODUCT_HEAD='cb47d5c92db709048ac668cfa3c0eadd34b02152'
PRODUCT_TREE='c500a68873499ec754ea50a43d85f427debccb99'
PRODUCT_BRANCH='VXG-111926-chatgpt-mac-local-asr-provider-distribution-a001'
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

ROOT="$("$MKTEMP_BIN" -d "$HOME/Downloads/.vexlife-pr384-a007.XXXXXX")"
SDK_ROOT="$ROOT/Vextreme-SDK"
FORMATION_RESULT="$ROOT/formation-result.json"
FORMATION_SCRIPT="$ROOT/form-a007.mjs"

cleanup() {
  case "$ROOT" in
    "$HOME"/Downloads/.vexlife-pr384-a007.*) "$RM_BIN" -rf -- "$ROOT" ;;
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

const BASE = 'b8bda514fae5e3ec0c0e0718dd48766e39cd6b45';
const HEAD = 'cb47d5c92db709048ac668cfa3c0eadd34b02152';
const TREE = 'c500a68873499ec754ea50a43d85f427debccb99';
const BRANCH = 'VXG-111926-chatgpt-mac-local-asr-provider-distribution-a001';
const PROVIDER_ROOT =
  'build/vexlife-prod-service-image/images/' +
  HEAD +
  '/local-asr-provider/whispercpp/v1.9.5';
const PROVIDER_BIN = PROVIDER_ROOT + '/bin';
const PROVIDER_EXE = PROVIDER_BIN + '/whisper-server';
const SOURCE_ID = PROVIDER_ROOT + '/provider-source.txt';
const TREE_ID = PROVIDER_ROOT + '/provider-tree.txt';
const SERVICE_LAUNCHER =
  'build/vexlife-prod-service-image/images/' +
  HEAD +
  '/bin/vexlife-prod-service';

const CHANGED_PATHS = [
  '.github/workflows/mac-app-proof.yml',
  'build.gradle.kts',
  'packaging/private-service-image/bin/vexlife-prod-service',
  'packaging/private-service-image/local-asr-provider-contract.json',
  'src/jvmMain/kotlin/vexlife/localasr/VexLifeLocalAsrMacArm64ProviderRealization.kt',
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
  attemptRef: 'attempt.vexlife-prod.pr384.mac-local-asr.source-owner.a007.20261119a007',
  requestRef: 'request.vexlife-prod.pr384.mac-local-asr.source-owner.a007.20261119a007',
  correlationRef: 'correlation.vexlife-prod.pr384.mac-local-asr.source-owner.a007.20261119a007',
  packageRef: 'package.vexlife-prod.pr384.mac-local-asr.source-owner.a007.20261119a007',
  original: {
    originalIntent:
      'Close the fresh-install Mac Local-ASR provider distribution gap without changing VoiceInput semantics or creating a fake provider-install control.',
    originalGoal:
      'Complete the remaining exact PR #384 Mac provider runtime-smoke SourceOwner predicate, then return to final #338 composition.',
    userProtectedIntent:
      'Keep Product source authority in PR #384, reuse still-current A004/A005 evidence, avoid Local Workspace, hold Product/Home/microphone/lifecycle effects, and return one canonical result.',
    sourceRefs: [
      'github.pr.vexlife-prod.384',
      'github.pr.vexlife-prod.386',
      'source.vexrelay.current-contract/v1',
      'return.pr384.a004.inner.b231bcd99a0333cbc8027523a5e73be1a153667793f41539ae1eef5b755371ce',
      'return.pr384.a005.inner.51d73098840a5b7b08fdb9daf3b683a5684edee596951c5a9017f5538a5137e5',
    ],
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
    roleRef: 'role.coder.vexlife-prod.pr384.a007',
    instanceRef: 'instance.vexlife-prod.pr384.a007',
    occupancyRef: 'occupancy.vexlife-prod.pr384.a007',
    providerBindingRef: 'provider.macos-victor.vexlife-prod.pr384.a007',
    threadRef: 'thread.local-vex-operations.pr384.a007',
    authorityEnvelopeRef: 'authority-envelope.vexlife-prod.pr384.a007',
  },
  capabilities: {
    platformGitHubRead: 'AVAILABLE',
    platformGitHubWrite: 'UNAVAILABLE',
  },
  execution: {
    hostProfile: 'macos-victor',
    executionSurface: 'MAC_ZIP_RUN_COMMAND',
    singleUseNonce: 'nonce.vexlife-prod.pr384.a007.20261119a007',
    duplicateAttemptMarker: 'duplicate.vexlife-prod.pr384.a007.20261119a007',
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
    authorityRef: 'authority.vexlife-prod.pr384.source-owner.a007',
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
      '/bin/test',
      '/usr/bin/grep',
      '/usr/bin/env',
    ],
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
    overlapEvidenceRef: 'github.pr.vexlife-prod.384.bounded-eight-path-scope',
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

const gradleArgs = [
  './gradlew',
  '-PvexlifeAppVersion=0.0.0',
  'archiveVexLifeProdMacApplication',
  '--stacktrace',
  '--no-daemon',
  '--max-workers=1',
  '-Dorg.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=768m -XX:ReservedCodeCacheSize=512m',
];

const stage = {
  repositoryRef: 'primary',
  expectedBase: BASE,
  expectedHead: HEAD,
  expectedTree: TREE,
  branch: BRANCH,
  proofCommands: [
    {
      command: '/bin/bash',
      args: gradleArgs,
      predicateClass: 'SOURCE_OWNER_BEHAVIOR',
      predicateRef: 'proof.pr384.mac-local-asr.materialize-provider',
      timeoutMs: 900000,
    },
    {
      command: '/bin/test',
      args: ['-x', PROVIDER_EXE],
      predicateClass: 'SOURCE_OWNER_REPOSITORY',
      predicateRef: 'proof.pr384.mac-local-asr.provider-executable',
    },
    {
      command: '/usr/bin/grep',
      args: ['-Fx', 'd1be6fde11ac6e0407606b4e42fe72d34add8037', SOURCE_ID],
      predicateClass: 'SOURCE_OWNER_REPOSITORY',
      predicateRef: 'proof.pr384.mac-local-asr.provider-source',
    },
    {
      command: '/usr/bin/grep',
      args: ['-Fx', '6fe2755682242a5c01aba65a22b3c0305d747841', TREE_ID],
      predicateClass: 'SOURCE_OWNER_REPOSITORY',
      predicateRef: 'proof.pr384.mac-local-asr.provider-tree',
    },
    {
      command: '/usr/bin/grep',
      args: ['-F', '--', '-Dvexlife.privateServiceImageRoot="$ROOT"', SERVICE_LAUNCHER],
      predicateClass: 'SOURCE_OWNER_REPOSITORY',
      predicateRef: 'proof.pr384.mac-local-asr.private-image-root-binding',
    },
    {
      command: '/usr/bin/env',
      args: [PROVIDER_EXE, '--help'],
      env: {
        DYLD_LIBRARY_PATH: PROVIDER_BIN,
      },
      predicateClass: 'SOURCE_OWNER_BEHAVIOR',
      predicateRef: 'proof.pr384.mac-local-asr.packaged-whisper-server-help',
      timeoutMs: 120000,
    },
  ],
  maxFiles: 8,
  stateRef: 'state.vexlife-prod.pr384.source-owner.a007',
  currentLifecycleState: 'SOURCE_READY',
  currentGoal:
    'Close the remaining PR #384 packaged whisper-server runtime smoke after A005 proved provider/package identity.',
  remainingGoals: [
    'Compose the accepted #372/#382/#384 work into the final #338 candidate.',
    'Run exact final-byte Mac and Windows regressions before lifecycle.',
  ],
  onPass: {
    lifecycleState: 'SOURCE_READY',
    nextFunction: 'F12.FORM_CONTINUATION',
    nextOwner: 'Operations',
    whyThisIsNext:
      'PR #384 SourceOwner package/runtime proof is complete; the release trajectory returns to final #338 composition.',
  },
  resultFilename:
    'RETURN-VEXLIFE-PR384-MAC-LOCAL-ASR-SOURCE-OWNER-A007.zip',
};

const authorityEnvelope = {
  authoritySchemaRef: 'vextreme.effect-transaction/v1',
  semanticAdmissionRef: 'github.pr.vexlife-prod.384',
  transactionRef: 'transaction.vexlife-prod.pr384.source-owner.a007',
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
  attemptRef: 'attempt.vexlife-prod.pr384.mac-local-asr.carrier.a007.20261119a007',
  packageRef: 'package.vexlife-prod.pr384.mac-local-asr.carrier.a007.20261119a007',
  requestRef: 'request.vexlife-prod.pr384.mac-local-asr.carrier.a007.20261119a007',
  correlationRef: 'correlation.vexlife-prod.pr384.mac-local-asr.carrier.a007.20261119a007',
  taskStorageKey: 'task-vexlife-prod-pr384-mac-local-asr-carrier',
  attemptStorageKey: 'attempt-a007',
  requestingRoleRef: 'Operations[PR384-A007]',
  requestingInstanceRef: 'instance.local-vex-operations.pr384.a007',
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
    transactionRef: 'transaction.vexlife-prod.pr384.source-owner.a007',
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
  singleUseNonce: 'nonce.vexlife-prod.pr384.carrier.a007.20261119a007',
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
    'VEXLIFE-PR384-MAC-LOCAL-ASR-SOURCE-OWNER-A007',
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
NODE_A007

echo 'A007: forming the exact current task/carrier...'
A007_SDK_ROOT="$SDK_ROOT" A007_FORMATION_RESULT="$FORMATION_RESULT" "$NODE_BIN" "$FORMATION_SCRIPT"

PACKAGE="$("$NODE_BIN" -e 'const fs=require("node:fs");const x=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));process.stdout.write(x.outerZip)' "$FORMATION_RESULT")"
EXPECTED="$("$NODE_BIN" -e 'const fs=require("node:fs");const x=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));process.stdout.write(x.outerSha256)' "$FORMATION_RESULT")"

[ -f "$PACKAGE" ] || { echo 'A007: formed package is missing.' >&2; exit 23; }
OBSERVED="$(/usr/bin/shasum -a 256 "$PACKAGE" | /usr/bin/cut -d ' ' -f 1)"
[ "$OBSERVED" = "$EXPECTED" ] || { echo 'A007: formed package digest mismatch.' >&2; exit 24; }

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
