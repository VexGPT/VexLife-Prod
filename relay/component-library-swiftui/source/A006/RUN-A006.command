#!/bin/bash
set -euo pipefail

CONTINUITY='[VXG RealForever]'
SDK_REPOSITORY='vgong24/Vextreme-SDK'
SDK_HEAD='6f8f6f50467f5df5a542713f7dc25b82db4ac8fa'
SDK_TREE='96795640b01f1969da980f52d5a8ec136a8c44aa'
PRODUCT_REPOSITORY='VexGPT/VexLife-Prod'
PRODUCT_BASE='e1c18be71f73c8ec51818a960c0d9b50b94efc63'
PRODUCT_HEAD='8c1fbee1e9029f89f4452c86dfcc9e7f08e4834b'
PRODUCT_BRANCH='VXG-112126-component-swiftui-reference-a001'
PRODUCT_BASE_BRANCH='main'
ATTEMPT='A006'

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

BASH_BIN="$(resolve_exec /bin/bash)" || { echo 'A006: qualified Bash unavailable.' >&2; exit 10; }
GIT_BIN="$(resolve_exec /usr/bin/git)" || { echo 'A006: qualified Git unavailable.' >&2; exit 11; }
GH_BIN="$(resolve_exec /opt/homebrew/bin/gh /usr/local/bin/gh)" || { echo 'A006: qualified GitHub CLI unavailable.' >&2; exit 12; }
NODE_BIN="$(resolve_exec /opt/homebrew/bin/node /usr/local/bin/node /usr/bin/node)" || { echo 'A006: qualified Node unavailable.' >&2; exit 13; }
MKTEMP_BIN="$(resolve_exec /usr/bin/mktemp)" || { echo 'A006: mktemp unavailable.' >&2; exit 14; }
RM_BIN="$(resolve_exec /bin/rm /usr/bin/rm)" || { echo 'A006: rm unavailable.' >&2; exit 15; }

ARCH="$(/usr/bin/uname -m)"
[ "$ARCH" = 'arm64' ] || { echo "A006: expected Apple-silicon arm64 host, observed $ARCH." >&2; exit 16; }

"$GH_BIN" auth status --hostname github.com >/dev/null 2>&1 || {
  echo 'A006: GitHub authentication is required before this proof can run.' >&2
  exit 17
}

ROOT="$("$MKTEMP_BIN" -d "$HOME/Downloads/.vexlife-pr506-swiftui-source-a006.XXXXXX")"
SDK_ROOT="$ROOT/Vextreme-SDK"
FORMATION_RESULT="$ROOT/formation-result.json"
FORMATION_SCRIPT="$ROOT/form-pr506-swiftui-a006.mjs"

cleanup() {
  case "$ROOT" in
    "$HOME"/Downloads/.vexlife-pr506-swiftui-source-a006.*) "$RM_BIN" -rf -- "$ROOT" ;;
    *) return 1 ;;
  esac
}
trap cleanup EXIT HUP INT TERM

echo 'A006: deriving exact immutable Product tree from pinned commit...'
PRODUCT_TREE="$("$GH_BIN" api "repos/$PRODUCT_REPOSITORY/git/commits/$PRODUCT_HEAD" --jq '.tree.sha')"
"$NODE_BIN" -e 'if(!/^[0-9a-f]{40}$/.test(process.argv[1])) process.exit(1)' "$PRODUCT_TREE" || {
  echo "A006: invalid Product tree returned for exact head: $PRODUCT_TREE" >&2
  exit 18
}

echo 'A006: acquiring exact current VexRelay/VexLocalBridge source...'
"$GH_BIN" repo clone "$SDK_REPOSITORY" "$SDK_ROOT" -- --no-checkout >/dev/null
"$GIT_BIN" -C "$SDK_ROOT" checkout --detach "$SDK_HEAD" >/dev/null

OBSERVED_HEAD="$("$GIT_BIN" -C "$SDK_ROOT" rev-parse HEAD)"
OBSERVED_TREE="$("$GIT_BIN" -C "$SDK_ROOT" rev-parse 'HEAD^{tree}')"
[ "$OBSERVED_HEAD" = "$SDK_HEAD" ] || { echo 'A006: SDK head mismatch.' >&2; exit 19; }
[ "$OBSERVED_TREE" = "$SDK_TREE" ] || { echo 'A006: SDK tree mismatch.' >&2; exit 20; }
[ -z "$("$GIT_BIN" -C "$SDK_ROOT" status --porcelain=v1 -uall)" ] || { echo 'A006: acquired SDK source is dirty.' >&2; exit 21; }

A006_SDK_ROOT="$SDK_ROOT" "$NODE_BIN" -e '
const fs=require("node:fs");
const path=require("node:path");
const root=process.env.A006_SDK_ROOT;
const contract=JSON.parse(fs.readFileSync(path.join(root,"docs/private-continuity/vexrelay/current-contract.json"),"utf8"));
if(contract.contractRef!=="source.vexrelay.current-contract/v1") process.exit(22);
const carrier=contract.acceptedAdapters.find(x=>x.adapterRef==="adapter.vexlocalbridge.native-vexrelay-task-archive/v1");
if(!carrier || carrier.state!=="TERMINAL_ACCEPTED_CURRENT") process.exit(23);
if(!contract.acceptedRecipeFamilies.includes("ExactHeadSourceOwnerProof")) process.exit(24);
'

cat > "$FORMATION_SCRIPT" <<'NODE_A006'
import fs from 'node:fs';
import path from 'node:path';
import { pathToFileURL } from 'node:url';

const SDK_ROOT = process.env.A006_SDK_ROOT;
const OUT = process.env.A006_FORMATION_RESULT;
const TREE = process.env.A006_PRODUCT_TREE;
const DOWNLOADS = path.join(process.env.HOME, 'Downloads');

const { formVexRelayTaskArchiveCarrier } =
  await import(pathToFileURL(path.join(
    SDK_ROOT,
    'lib/vexlocalbridge/vexrelay-task-archive-carrier-formation.mjs',
  )).href);

const BASE = 'e1c18be71f73c8ec51818a960c0d9b50b94efc63';
const HEAD = '8c1fbee1e9029f89f4452c86dfcc9e7f08e4834b';
const BRANCH = 'VXG-112126-component-swiftui-reference-a001';
const BASE_BRANCH = 'main';

const CHANGED_PATHS = [
  "docs/foundation/INDEX.md",
  "docs/foundation/SWIFTUI-REFERENCE-ADAPTER.md",
  "reference/swiftui/build.gradle.kts",
  "reference/swiftui/src/main/kotlin/vexlife/reference/swiftui/VexLifeSwiftUiQuickActionsReference.kt",
  "reference/swiftui/src/main/kotlin/vexlife/reference/swiftui/VexLifeSwiftUiReferenceMaterializer.kt",
  "reference/swiftui/src/main/kotlin/vexlife/reference/swiftui/VexLifeSwiftUiStableRepeaterReference.kt",
  "reference/swiftui/src/main/kotlin/vexlife/reference/swiftui/VexLifeSwiftUiWorkspaceFrameReference.kt",
  "reference/swiftui/src/test/kotlin/vexlife/reference/swiftui/VexLifeSwiftUiQuickActionsReferenceTest.kt",
  "reference/swiftui/src/test/kotlin/vexlife/reference/swiftui/VexLifeSwiftUiStableRepeaterReferenceTest.kt",
  "reference/swiftui/src/test/kotlin/vexlife/reference/swiftui/VexLifeSwiftUiWorkspaceFrameReferenceTest.kt",
  "settings.gradle.kts",
];

if (!/^[0-9a-f]{40}$/.test(TREE)) throw new Error('A006_PRODUCT_TREE_INVALID');
if (TREE !== '9a412c7346c233102d863d9f4f45c7264aa4195e') throw new Error('A006_PRODUCT_TREE_MISMATCH');

const now = new Date();
const expires = new Date(now.getTime() + 60 * 60 * 1000);

const seed = {
  schemaVersion: 'vex.relay.task/v2',
  continuity: '[VXG RealForever]',
  taskRef: 'task.vexlife-prod.pr506.component-swiftui.source-owner',
  attemptRef: 'attempt.vexlife-prod.pr506.component-swiftui.source-owner.a006.20261121',
  requestRef: 'request.vexlife-prod.pr506.component-swiftui.source-owner.a006.20261121',
  correlationRef: 'correlation.vexlife-prod.pr506.component-swiftui.source-owner.a006.20261121',
  packageRef: 'package.vexlife-prod.pr506.component-swiftui.source-owner.a006.20261121',
  original: {
    originalIntent:
      'Use the completed Product relationship source to prove a second native target realization without making Compose, browser syntax, SwiftUI, generated target source, or VexBoard the semantic owner.',
    originalGoal:
      'Prove exact VexLife-Prod #506 Kotlin/JVM/JS source, deterministic SwiftUI generation, exact proof materialization, and Apple SwiftUI SDK type correctness on the current candidate.',
    userProtectedIntent:
      'Preserve the larger projectable Blueprint circuit: completed Products teach stable interfaceable relationships; target adapters realize them natively; target Screen Lens projects the resulting Product back into VexBoard as evidence rather than canonical writeback.',
    sourceRefs: [
      'github.pr.vexlife-prod.506',
      'github.pr.vexwork.402',
      'git.vexlife-prod.8c1fbee1e9029f89f4452c86dfcc9e7f08e4834b',
      'git.vexlife-prod.e1c18be71f73c8ec51818a960c0d9b50b94efc63',
      'source.vexrelay.current-contract/v1',
    ],
  },
  lane: {
    entryClass: 'SAME_LANE_SUCCESSOR',
    scopeMode: 'STAGE_BOUNDED',
    laneRef: 'lane.vexlife-prod.component-swiftui',
    parentRootRef: 'github.pr.vexlife-prod.506',
    workRef: 'work.vex.component-library-architecture.target-realizations',
    claimRef: 'claim.vexlife-prod.pr506.component-swiftui-source-owner',
    returnRouteRef: 'github.pr.vexlife-prod.506',
    ownerRef: 'github.pr.vexlife-prod.506',
    rootHubRef: 'github.pr.vexwork.402',
  },
  role: {
    activeRole: 'CODER',
    roleRef: 'role.coder.vexlife-prod.pr506.a006',
    instanceRef: 'instance.vexlife-prod.pr506.a006',
    occupancyRef: 'occupancy.vexlife-prod.pr506.a006',
    providerBindingRef: 'provider.macos-victor.vexlife-prod.pr506.a006',
    threadRef: 'thread.local-vex-operations.pr506.a006',
    authorityEnvelopeRef: 'authority-envelope.vexlife-prod.pr506.a006',
  },
  capabilities: {
    platformGitHubRead: 'AVAILABLE',
    platformGitHubWrite: 'UNAVAILABLE',
  },
  execution: {
    hostProfile: 'macos-victor',
    executionSurface: 'MAC_ZIP_RUN_COMMAND',
    singleUseNonce: 'nonce.vexlife-prod.pr506.a006.20261121',
    duplicateAttemptMarker: 'duplicate.vexlife-prod.pr506.a006.20261121',
    duplicateProtectionMode: 'OUTER_RELAY_BOUND',
    formedAt: now.toISOString(),
    expiresAtOrNull: expires.toISOString(),
  },
  workspace: {
    mode: 'RUN_ROOT',
    projectRef: 'project.vexlife-prod.pr506.component-swiftui-source-owner',
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
    baseBranch: BASE_BRANCH,
    acceptedBase: BASE,
    checkoutRef: HEAD,
    expectedHead: HEAD,
    branch: BRANCH,
  }],
  authority: {
    authorityRef: 'authority.vexlife-prod.pr506.component-swiftui.source-owner.a006',
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
    sourceSetRef: 'source-set.vexlife-prod.pr506.component-swiftui',
    sourceVersionRef: 'git.' + HEAD,
    overlapEvidenceRef: 'github.pr.vexlife-prod.506',
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
        './gradlew',
        'jvmTest',
        'compileKotlinJs',
        'compileTestKotlinJs',
        ':reference:swiftui:compileKotlin',
        ':reference:swiftui:test',
        ':reference:swiftui:materializeSwiftUiReference',
        '--stacktrace',
        '--no-daemon',
        '--max-workers=1',
        '-Dorg.gradle.jvmargs=-Xmx3072m -XX:MaxMetaspaceSize=1024m -XX:ReservedCodeCacheSize=512m',
      ],
      predicateClass: 'SOURCE_OWNER_BEHAVIOR',
      predicateRef: 'proof.pr506.component-swiftui.repository-kotlin',
      timeoutMs: 1200000,
    },
    {
      command: '/bin/bash',
      args: [
        '-c',
        [
          'set -euo pipefail',
          'OUT=reference/swiftui/build/generated/swiftui-reference',
          'for f in VexQuickActionsReference.swift VexStableConversationTimelineReference.swift VexWorkspaceFrameReference.swift; do [ -f "$OUT/$f" ]; done',
          'SDK_PATH=$(/usr/bin/xcrun --sdk iphonesimulator --show-sdk-path)',
          'SDK_VERSION=$(/usr/bin/xcrun --sdk iphonesimulator --show-sdk-version)',
          '/usr/bin/xcrun --sdk iphonesimulator swiftc -typecheck -sdk "$SDK_PATH" -target "arm64-apple-ios${SDK_VERSION}-simulator" "$OUT/VexQuickActionsReference.swift" "$OUT/VexStableConversationTimelineReference.swift" "$OUT/VexWorkspaceFrameReference.swift"',
        ].join('; '),
      ],
      predicateClass: 'SOURCE_OWNER_BEHAVIOR',
      predicateRef: 'proof.pr506.component-swiftui.apple-swiftui-sdk',
      timeoutMs: 300000,
    },
  ],
  maxFiles: 10,
  stateRef: 'state.vexlife-prod.pr506.component-swiftui.source-owner.a006',
  currentLifecycleState: 'SOURCE_READY',
  currentGoal:
    'Prove the exact current SwiftUI target-realization candidate end to end at the source-owner boundary: repository Kotlin/JVM/JS proof, deterministic reference generation/materialization, and type correctness of the exact generated Swift bytes against the current qualified Apple SwiftUI simulator SDK.',
  remainingGoals: [
    'Consume the exact SourceOwner return into the VexWork A4 realization proof map without changing A3 portable meaning.',
    'Run fresh Independent Assurance over the exact proved target-realization boundary.',
    'Only after accepted proof/assurance, reconcile A4 completion and the dependent Gallery/construction/self-projection work.',
    'Keep lived iOS presentation proof separate unless a later claim requires lived arrival.',
  ],
  onPass: {
    lifecycleState: 'ASSURANCE_PENDING',
    nextFunction: 'F13.RUN_FRESH_ASSURANCE',
    nextOwner: 'IndependentAssurance',
    whyThisIsNext:
      'The exact target source and Apple type surface are executable; semantic review must confirm the adapter preserves ownership and projection boundaries before any lifecycle or merge decision.',
  },
  resultFilename:
    'RETURN-VEXLIFE-PR506-SWIFTUI-SOURCE-OWNER-A006.zip',
};

const authorityEnvelope = {
  authoritySchemaRef: 'vextreme.effect-transaction/v1',
  semanticAdmissionRef: 'github.pr.vexlife-prod.506',
  transactionRef: 'transaction.vexlife-prod.pr506.component-swiftui.source-owner.a006',
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
  taskRef: 'task.vexlife-prod.pr506.component-swiftui.carrier',
  attemptRef: 'attempt.vexlife-prod.pr506.component-swiftui.carrier.a006.20261121',
  packageRef: 'package.vexlife-prod.pr506.component-swiftui.carrier.a006.20261121',
  requestRef: 'request.vexlife-prod.pr506.component-swiftui.carrier.a006.20261121',
  correlationRef: 'correlation.vexlife-prod.pr506.component-swiftui.carrier.a006.20261121',
  taskStorageKey: 'task-vexlife-prod-pr506-component-swiftui-carrier',
  attemptStorageKey: 'attempt-a006',
  requestingRoleRef: 'Operations[PR506-A006]',
  requestingInstanceRef: 'instance.local-vex-operations.pr506.a006',
  parentOperationsRef: 'github.pr.vexlife-prod.506',
  returnRouteRef: 'github.pr.vexlife-prod.506',
  hostProfileRef: 'macos-victor',
  repositoryExecutionProfileRefOrNull:
    'profile.repository.vextreme-sdk.macos-v1',
  repositoryRefOrNull: 'VexGPT/VexLife-Prod',
  baseRefOrNull: BASE,
  headRefOrNull: HEAD,
  treeRefOrNull: TREE,
  branchRefOrNull: BRANCH,
  claimRefOrNull: 'claim.vexlife-prod.pr506.component-swiftui-source-owner',
  exactPathRefs: CHANGED_PATHS,
  authorityBinding: {
    authoritySchemaRef: 'vextreme.effect-transaction/v1',
    authorityEnvelopePath: 'authority/effect.json',
    authorityEnvelopeSha256: '0'.repeat(64),
    semanticAdmissionRef: 'github.pr.vexlife-prod.506',
    transactionRef: 'transaction.vexlife-prod.pr506.component-swiftui.source-owner.a006',
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
  singleUseNonce: 'nonce.vexlife-prod.pr506.component-swiftui.carrier.a006.20261121',
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
    'VEXLIFE-PR506-SWIFTUI-TARGET-REALIZATION-SOURCE-OWNER-A006',
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
NODE_A006

echo 'A006: forming exact task/carrier...'
A006_SDK_ROOT="$SDK_ROOT" A006_FORMATION_RESULT="$FORMATION_RESULT" A006_PRODUCT_TREE="$PRODUCT_TREE" "$NODE_BIN" "$FORMATION_SCRIPT"

PACKAGE="$("$NODE_BIN" -e 'const fs=require("node:fs");const x=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));process.stdout.write(x.outerZip)' "$FORMATION_RESULT")"
EXPECTED="$("$NODE_BIN" -e 'const fs=require("node:fs");const x=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));process.stdout.write(x.outerSha256)' "$FORMATION_RESULT")"

[ -f "$PACKAGE" ] || { echo 'A006: formed package is missing.' >&2; exit 25; }
OBSERVED="$(/usr/bin/shasum -a 256 "$PACKAGE" | /usr/bin/cut -d ' ' -f 1)"
[ "$OBSERVED" = "$EXPECTED" ] || { echo 'A006: formed package digest mismatch.' >&2; exit 26; }

echo "A006: Product head $PRODUCT_HEAD"
echo "A006: Product tree $PRODUCT_TREE"
echo "A006: formed exact package $PACKAGE"
echo "A006: package sha256 $EXPECTED"
echo 'A006: entering current native VexRelay/VexLocalBridge carrier...'

set +e
VEX_EXPECTED_PACKAGE_SHA256="$EXPECTED" "$BASH_BIN" "$SDK_ROOT/lib/vexlocalbridge/templates/RUN-VEX-TASK.command" "$PACKAGE"
STATUS=$?
set -e

echo "A006: carrier exit $STATUS"
echo 'A006: if a canonical RETURN was formed, upload the exact RETURN ZIP revealed by VexLocalBridge.'
exit "$STATUS"
