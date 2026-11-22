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
  exit 17
}

ROOT="$("$MKTEMP_BIN" -d "$HOME/Downloads/.vexlife-pr506-swiftui-assurance-a007.XXXXXX")"
SDK_ROOT="$ROOT/Vextreme-SDK"
FORMATION_RESULT="$ROOT/formation-result.json"
FORMATION_SCRIPT="$ROOT/form-pr506-swiftui-assurance-a007.mjs"

cleanup() {
  case "$ROOT" in
    "$HOME"/Downloads/.vexlife-pr506-swiftui-assurance-a007.*) "$RM_BIN" -rf -- "$ROOT" ;;
    *) return 1 ;;
  esac
}
trap cleanup EXIT HUP INT TERM

echo 'A007: deriving exact immutable Product tree from pinned commit...'
PRODUCT_TREE="$("$GH_BIN" api "repos/$PRODUCT_REPOSITORY/git/commits/$PRODUCT_HEAD" --jq '.tree.sha')"
"$NODE_BIN" -e 'if(!/^[0-9a-f]{40}$/.test(process.argv[1])) process.exit(1)' "$PRODUCT_TREE" || {
  echo "A007: invalid Product tree returned for exact head: $PRODUCT_TREE" >&2
  exit 18
}

echo 'A007: acquiring exact current VexRelay/VexLocalBridge source...'
"$GH_BIN" repo clone "$SDK_REPOSITORY" "$SDK_ROOT" -- --no-checkout >/dev/null
"$GIT_BIN" -C "$SDK_ROOT" checkout --detach "$SDK_HEAD" >/dev/null

OBSERVED_HEAD="$("$GIT_BIN" -C "$SDK_ROOT" rev-parse HEAD)"
OBSERVED_TREE="$("$GIT_BIN" -C "$SDK_ROOT" rev-parse 'HEAD^{tree}')"
[ "$OBSERVED_HEAD" = "$SDK_HEAD" ] || { echo 'A007: SDK head mismatch.' >&2; exit 19; }
[ "$OBSERVED_TREE" = "$SDK_TREE" ] || { echo 'A007: SDK tree mismatch.' >&2; exit 20; }
[ -z "$("$GIT_BIN" -C "$SDK_ROOT" status --porcelain=v1 -uall)" ] || { echo 'A007: acquired SDK source is dirty.' >&2; exit 21; }

A007_SDK_ROOT="$SDK_ROOT" "$NODE_BIN" -e '
const fs=require("node:fs");
const path=require("node:path");
const root=process.env.A007_SDK_ROOT;
const contract=JSON.parse(fs.readFileSync(path.join(root,"docs/private-continuity/vexrelay/current-contract.json"),"utf8"));
if(contract.contractRef!=="source.vexrelay.current-contract/v1") process.exit(22);
const carrier=contract.acceptedAdapters.find(x=>x.adapterRef==="adapter.vexlocalbridge.native-vexrelay-task-archive/v1");
if(!carrier || carrier.state!=="TERMINAL_ACCEPTED_CURRENT") process.exit(23);
if(!contract.acceptedRecipeFamilies.includes("ExactHeadSourceOwnerProof")) process.exit(24);
'

cat > "$FORMATION_SCRIPT" <<'NODE_A007'
import fs from 'node:fs';
import path from 'node:path';
import { pathToFileURL } from 'node:url';

const SDK_ROOT = process.env.A007_SDK_ROOT;
const OUT = process.env.A007_FORMATION_RESULT;
const TREE = process.env.A007_PRODUCT_TREE;
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

if (!/^[0-9a-f]{40}$/.test(TREE)) throw new Error('A007_PRODUCT_TREE_INVALID');
if (TREE !== '9a412c7346c233102d863d9f4f45c7264aa4195e') throw new Error('A007_PRODUCT_TREE_MISMATCH');

const now = new Date();
const expires = new Date(now.getTime() + 60 * 60 * 1000);

const seed = {
  schemaVersion: 'vex.relay.task/v2',
  continuity: '[VXG RealForever]',
  taskRef: 'task.vexlife-prod.pr506.component-swiftui.assurance',
  attemptRef: 'attempt.vexlife-prod.pr506.component-swiftui.assurance.a007.20261121',
  requestRef: 'request.vexlife-prod.pr506.component-swiftui.assurance.a007.20261121',
  correlationRef: 'correlation.vexlife-prod.pr506.component-swiftui.assurance.a007.20261121',
  packageRef: 'package.vexlife-prod.pr506.component-swiftui.assurance.a007.20261121',
  original: {
    originalIntent:
      'Review the exact proved SwiftUI target-realization candidate independently from its SourceOwner proof and preserve the projectable Blueprint circuit without inheriting the authoring lane conclusion.',
    originalGoal:
      'Form fresh exact-head remote review evidence for VexLife-Prod #506 and determine whether the target adapter preserves ownership, portability, and reverse-projection boundaries.',
    userProtectedIntent:
      'Keep completed Product meaning, portable relationships, target-native realization, and VexBoard observation distinct so the ecosystem can learn without circular authority.',
    sourceRefs: [
      'github.pr.vexlife-prod.506',
      'github.pr.vexwork.402',
      'git.vexlife-prod.8c1fbee1e9029f89f4452c86dfcc9e7f08e4834b',
      'git.vexlife-prod.e1c18be71f73c8ec51818a960c0d9b50b94efc63',
      'source.vexrelay.current-contract/v1',
      'relay.return.pr506.source-owner.a006.e20aaba9f4c1b13c72a5d2d8b863f7e61695953cb2311093b7de0bdce10f90c5',
    ],
  },
  lane: {
    entryClass: 'SAME_LANE_SUCCESSOR',
    scopeMode: 'READ_ONLY',
    laneRef: 'lane.vexlife-prod.component-swiftui',
    parentRootRef: 'github.pr.vexlife-prod.506',
    workRef: 'work.vex.component-library-architecture.target-realizations',
    claimRef: 'claim.vexlife-prod.pr506.component-swiftui-assurance-readonly',
    returnRouteRef: 'github.pr.vexlife-prod.506',
    ownerRef: 'github.pr.vexlife-prod.506',
    rootHubRef: 'github.pr.vexwork.402',
  },
  role: {
    activeRole: 'INDEPENDENT_ASSURANCE',
    roleRef: 'role.independent-assurance.vexlife-prod.pr506.a007',
    instanceRef: 'instance.vexlife-prod.pr506.a007',
    occupancyRef: 'occupancy.vexlife-prod.pr506.a007',
    providerBindingRef: 'provider.macos-victor.vexlife-prod.pr506.a007',
    threadRef: 'thread.local-vex-operations.pr506.a007',
    authorityEnvelopeRef: 'authority-envelope.vexlife-prod.pr506.assurance.a007',
  },
  capabilities: {
    platformGitHubRead: 'UNAVAILABLE',
    platformGitHubWrite: 'UNAVAILABLE',
  },
  execution: {
    hostProfile: 'macos-victor',
    executionSurface: 'MAC_ZIP_RUN_COMMAND',
    singleUseNonce: 'nonce.vexlife-prod.pr506.a007.20261121',
    duplicateAttemptMarker: 'duplicate.vexlife-prod.pr506.a007.20261121',
    duplicateProtectionMode: 'OUTER_RELAY_BOUND',
    formedAt: now.toISOString(),
    expiresAtOrNull: expires.toISOString(),
  },
  workspace: {
    mode: 'RUN_ROOT',
    projectRef: 'project.vexlife-prod.pr506.component-swiftui-assurance',
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
    authorityRef: 'authority.vexlife-prod.pr506.component-swiftui.assurance.a007',
    authorityMode: 'EXACT_STAGE_EFFECTS',
    grantedEffects: [
      'CHECK_AUTH',
      'QUERY_PROVIDER',
      'FORM_RESULT',
    ],
    conditionalEffects: [],
    consumedEffects: [],
    remainingEffects: [
      'CHECK_AUTH',
      'QUERY_PROVIDER',
      'FORM_RESULT',
    ],
    heldEffects: [
      'READ_REPOSITORY',
      'RUN_LOCAL_COMMAND',
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
      'STOP_ON_EVIDENCE_REJECTED',
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
  branch: BRANCH,
  maxFiles: 10,
  stateRef: 'state.vexlife-prod.pr506.component-swiftui.assurance.a007',
  currentLifecycleState: 'ASSURANCE_PENDING',
  currentGoal:
    'Form fresh read-only exact-candidate review evidence for the re-proved SwiftUI target-realization boundary after the A005 foundation-index correction, without inheriting the SourceOwner semantic conclusion.',
  remainingGoals: [
    'Verify the A005 foundation-index correction is present and no malformed/stale native-proof status remains; then review whether Product meaning remains Product-owned and VexWeb pressure has not become universal portable authority.',
    'Review whether Kotlin Blueprint, target adapter, generated Swift, semantic manifest and Screen Lens remain distinct bounded projections.',
    'Return semantic CLEAR_ON_EXACT_HEAD or CORRECTION_REQUIRED outside the Relay recipe.',
    'If CLEAR, continue only the next A4 lifecycle/currentness and remaining Android proof obligations.',
  ],
  onPass: {
    lifecycleState: 'ASSURANCE_EVIDENCE_READY',
    nextFunction: 'F13.RUN_FRESH_ASSURANCE',
    nextOwner: 'Independent Assurance',
    whyThisIsNext:
      'Exact remote candidate evidence is available, but the IndependentAssurance recipe cannot self-issue semantic clearance.',
  },
  resultFilename:
    'RETURN-VEXLIFE-PR506-SWIFTUI-INDEPENDENT-ASSURANCE-A007.zip',
};

const authorityEnvelope = {
  authoritySchemaRef: 'vextreme.effect-transaction/v1',
  semanticAdmissionRef: 'github.pr.vexlife-prod.506',
  transactionRef: 'transaction.vexlife-prod.pr506.component-swiftui.assurance.a007',
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
  taskRef: 'task.vexlife-prod.pr506.component-swiftui-assurance.carrier',
  attemptRef: 'attempt.vexlife-prod.pr506.component-swiftui-assurance.carrier.a007.20261121',
  packageRef: 'package.vexlife-prod.pr506.component-swiftui-assurance.carrier.a007.20261121',
  requestRef: 'request.vexlife-prod.pr506.component-swiftui-assurance.carrier.a007.20261121',
  correlationRef: 'correlation.vexlife-prod.pr506.component-swiftui-assurance.carrier.a007.20261121',
  taskStorageKey: 'task-vexlife-prod-pr506-component-swiftui-carrier',
  attemptStorageKey: 'attempt-a007',
  requestingRoleRef: 'IndependentAssurance[PR506-A007]',
  requestingInstanceRef: 'instance.independent-assurance.pr506.a007',
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
  claimRefOrNull: 'claim.vexlife-prod.pr506.component-swiftui-assurance',
  exactPathRefs: CHANGED_PATHS,
  authorityBinding: {
    authoritySchemaRef: 'vextreme.effect-transaction/v1',
    authorityEnvelopePath: 'authority/effect.json',
    authorityEnvelopeSha256: '0'.repeat(64),
    semanticAdmissionRef: 'github.pr.vexlife-prod.506',
    transactionRef: 'transaction.vexlife-prod.pr506.component-swiftui.assurance.a007',
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
  singleUseNonce: 'nonce.vexlife-prod.pr506.component-swiftui-assurance.carrier.a007.20261121',
  stopConditions: [
    'stop.on.identity.drift',
    'stop.on.unadmitted.effect',
  ],
  archiveDigestBinding: 'FILENAME_SHA256',
};

const formed = await formVexRelayTaskArchiveCarrier({
  recipe: 'IndependentAssurance',
  seed,
  stage,
  outerTaskTemplate,
  authorityEnvelope,
  outputDirectory: DOWNLOADS,
  outputStem:
    'VEXLIFE-PR506-SWIFTUI-INDEPENDENT-ASSURANCE-A007',
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

echo 'A007: forming exact task/carrier...'
A007_SDK_ROOT="$SDK_ROOT" A007_FORMATION_RESULT="$FORMATION_RESULT" A007_PRODUCT_TREE="$PRODUCT_TREE" "$NODE_BIN" "$FORMATION_SCRIPT"

PACKAGE="$("$NODE_BIN" -e 'const fs=require("node:fs");const x=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));process.stdout.write(x.outerZip)' "$FORMATION_RESULT")"
EXPECTED="$("$NODE_BIN" -e 'const fs=require("node:fs");const x=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));process.stdout.write(x.outerSha256)' "$FORMATION_RESULT")"

[ -f "$PACKAGE" ] || { echo 'A007: formed package is missing.' >&2; exit 25; }
OBSERVED="$(/usr/bin/shasum -a 256 "$PACKAGE" | /usr/bin/cut -d ' ' -f 1)"
[ "$OBSERVED" = "$EXPECTED" ] || { echo 'A007: formed package digest mismatch.' >&2; exit 26; }

echo "A007: Product head $PRODUCT_HEAD"
echo "A007: Product tree $PRODUCT_TREE"
echo "A007: formed exact package $PACKAGE"
echo "A007: package sha256 $EXPECTED"
echo 'A007: entering current native VexRelay/VexLocalBridge Assurance carrier...'

set +e
VEX_EXPECTED_PACKAGE_SHA256="$EXPECTED" "$BASH_BIN" "$SDK_ROOT/lib/vexlocalbridge/templates/RUN-VEX-TASK.command" "$PACKAGE"
STATUS=$?
set -e

echo "A007: carrier exit $STATUS"
echo 'A007: if a canonical RETURN was formed, upload the exact RETURN ZIP revealed by VexLocalBridge.'
exit "$STATUS"
