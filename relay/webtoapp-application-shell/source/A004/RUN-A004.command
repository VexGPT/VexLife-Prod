#!/bin/bash
set -euo pipefail

CONTINUITY='[VXG RealForever]'
SDK_REPOSITORY='vgong24/Vextreme-SDK'
SDK_HEAD='8543c7d755faba04d4c4019c02e84f992667918a'
SDK_TREE='6376f68279f0750ee6d3868b8472368199763064'
PRODUCT_REPOSITORY='VexGPT/VexLife-Prod'
PRODUCT_BASE='fdb9f77bbd9895d0ab6932f669091b46ea813e9b'
PRODUCT_HEAD='d13e8e4a774aea60362491420ffeee2c07f91d1c'
PRODUCT_TREE='98ba3219e49f722272c976171cb736ed19ae9ac9'
PRODUCT_BRANCH='VXG-111926-chatgpt-webtoapp-application-shell-a001'
ATTEMPT='A004'
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd -P)"
CONSUMER="$SCRIPT_DIR/consume-return.cjs"
CONSUMED_RESULT="$HOME/Downloads/VEXLIFE-PR396-WEBTOAPP-APPLICATION-SHELL-SOURCE-A004-CONSUMED.json"

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

BASH_BIN="$(resolve_exec /bin/bash)" || { echo 'A004: qualified Bash unavailable.' >&2; exit 10; }
GIT_BIN="$(resolve_exec /usr/bin/git)" || { echo 'A004: qualified Git unavailable.' >&2; exit 11; }
GH_BIN="$(resolve_exec /opt/homebrew/bin/gh /usr/local/bin/gh)" || { echo 'A004: qualified GitHub CLI unavailable.' >&2; exit 12; }
NODE_BIN="$(resolve_exec /opt/homebrew/bin/node /usr/local/bin/node /usr/bin/node)" || { echo 'A004: qualified Node unavailable.' >&2; exit 13; }
MKTEMP_BIN="$(resolve_exec /usr/bin/mktemp)" || { echo 'A004: mktemp unavailable.' >&2; exit 14; }
RM_BIN="$(resolve_exec /bin/rm /usr/bin/rm)" || { echo 'A004: rm unavailable.' >&2; exit 15; }

ARCH="$(/usr/bin/uname -m)"
[ "$ARCH" = 'arm64' ] || { echo "A004: expected Apple-silicon arm64 host, observed $ARCH." >&2; exit 16; }

"$GH_BIN" auth status --hostname github.com >/dev/null 2>&1 || {
  echo 'A004: GitHub authentication is required before this proof can run.' >&2
  echo 'Run: gh auth login --hostname github.com --web' >&2
  exit 17
}

ROOT="$("$MKTEMP_BIN" -d "$HOME/Downloads/.vexlife-pr396-a004.XXXXXX")"
SDK_ROOT="$ROOT/Vextreme-SDK"
FORMATION_RESULT="$ROOT/formation-result.json"
FORMATION_SCRIPT="$ROOT/form-a004.mjs"

cleanup() {
  case "$ROOT" in
    "$HOME"/Downloads/.vexlife-pr396-a004.*) "$RM_BIN" -rf -- "$ROOT" ;;
    *) return 1 ;;
  esac
}
trap cleanup EXIT HUP INT TERM

echo 'A004: acquiring exact current VexRelay/VexLocalBridge source...'
"$GH_BIN" repo clone "$SDK_REPOSITORY" "$SDK_ROOT" -- --no-checkout >/dev/null
"$GIT_BIN" -C "$SDK_ROOT" checkout --detach "$SDK_HEAD" >/dev/null

OBSERVED_HEAD="$("$GIT_BIN" -C "$SDK_ROOT" rev-parse HEAD)"
OBSERVED_TREE="$("$GIT_BIN" -C "$SDK_ROOT" rev-parse 'HEAD^{tree}')"
[ "$OBSERVED_HEAD" = "$SDK_HEAD" ] || { echo 'A004: SDK head mismatch.' >&2; exit 18; }
[ "$OBSERVED_TREE" = "$SDK_TREE" ] || { echo 'A004: SDK tree mismatch.' >&2; exit 19; }
[ -z "$("$GIT_BIN" -C "$SDK_ROOT" status --porcelain=v1 -uall)" ] || { echo 'A004: acquired SDK source is dirty.' >&2; exit 20; }

A004_SDK_ROOT="$SDK_ROOT" "$NODE_BIN" -e '
const fs=require("node:fs");
const path=require("node:path");
const root=process.env.A004_SDK_ROOT;
const contract=JSON.parse(fs.readFileSync(path.join(root,"docs/private-continuity/vexrelay/current-contract.json"),"utf8"));
if(contract.contractRef!=="source.vexrelay.current-contract/v1") process.exit(21);
const carrier=contract.acceptedAdapters.find(x=>x.adapterRef==="adapter.vexlocalbridge.native-vexrelay-task-archive/v1");
if(!carrier || carrier.state!=="TERMINAL_ACCEPTED_CURRENT") process.exit(22);
if(!contract.acceptedRecipeFamilies.includes("ExactHeadSourceOwnerProof")) process.exit(23);
'

cat > "$FORMATION_SCRIPT" <<'NODE_A004'
import fs from 'node:fs';
import path from 'node:path';
import { pathToFileURL } from 'node:url';

const SDK_ROOT = process.env.A004_SDK_ROOT;
const OUT = process.env.A004_FORMATION_RESULT;
const DOWNLOADS = path.join(process.env.HOME, 'Downloads');

const { formVexRelayTaskArchiveCarrier } =
  await import(pathToFileURL(path.join(
    SDK_ROOT,
    'lib/vexlocalbridge/vexrelay-task-archive-carrier-formation.mjs',
  )).href);

const BASE = 'fdb9f77bbd9895d0ab6932f669091b46ea813e9b';
const HEAD = 'd13e8e4a774aea60362491420ffeee2c07f91d1c';
const TREE = '98ba3219e49f722272c976171cb736ed19ae9ac9';
const BRANCH = 'VXG-111926-chatgpt-webtoapp-application-shell-a001';

const CHANGED_PATHS = [
  'build.gradle.kts',
  'docs/release/RELEASES.md',
  'localization/strings/manifest.json',
  'localization/strings/source/setup.json',
  'localization/strings/source/windows-setup.json',
  'packaging/bootstrap/vexlife-prod-bootstrap.command.template',
  'packaging/macos-app/release-contract.json',
  'packaging/macos-webkit/VexLifeProductPresentation.swift',
  'packaging/private-service-image/setup/vexlife-prod-setup-window.applescript',
  'packaging/windows-webview2/VexLife.WebShell/Program.cs',
  'packaging/windows-webview2/VexLife.WebShell/VexLife.WebShell.csproj',
  'src/commonMain/kotlin/vexlife/entry/VexLifeProdEntryFamily.kt',
  'src/commonTest/kotlin/vexlife/entry/VexLifeProdEntryFamilyTest.kt',
  'src/jvmMain/kotlin/vexlife/platform/windows/VexLifeProdWindowsSetupWindow.kt',
  'src/jvmMain/kotlin/vexlife/platform/windows/VexLifeProdWindowsWebView2.kt',
  'src/jvmMain/kotlin/vexlife/service/VexLifeProdMacServiceLifetime.kt',
  'src/jvmMain/kotlin/vexlife/setup/VexLifeProdMacFrontDoor.kt',
  'src/jvmTest/kotlin/vexlife/platform/windows/VexLifeProdWindowsWebView2Test.kt',
  'src/jvmTest/kotlin/vexlife/service/VexLifeProdMacServiceLifetimeTest.kt',
  'src/jvmTest/kotlin/vexlife/setup/VexLifeProdMacFrontDoorTest.kt',
  'tools/windows-preview/FORM-WINDOWS-PREVIEW.ps1',
  'tools/windows-preview/README.md',
];

const now = new Date();
const expires = new Date(now.getTime() + 60 * 60 * 1000);

const seed = {
  schemaVersion: 'vex.relay.task/v2',
  continuity: '[VXG RealForever]',
  taskRef: 'task.vexlife-prod.pr396.webtoapp-application-shell.source-owner',
  attemptRef: 'attempt.vexlife-prod.pr396.webtoapp-application-shell.source-owner.a004.20261119a004',
  requestRef: 'request.vexlife-prod.pr396.webtoapp-application-shell.source-owner.a004.20261119a004',
  correlationRef: 'correlation.vexlife-prod.pr396.webtoapp-application-shell.source-owner.a004.20261119a004',
  packageRef: 'package.vexlife-prod.pr396.webtoapp-application-shell.source-owner.a004.20261119a004',
  original: {
    originalIntent:
      'Realize one VexLife desktop application meaning through Mac WKWebView and Windows WebView2 without making presentation a second Product, runtime, or BrowserContext owner.',
    originalGoal:
      'Prove the exact PR #396 application-shell source candidate: shared Quit meaning, Mac lifetime ownership, native presentation isolation, Windows WebView2 realization, localization, and packaging wiring.',
    userProtectedIntent:
      'Preserve VexArchitecture ownership and the explicit merge gate: Close is not Quit, Product meaning is shared across platforms, and nothing merges before built VexLife is lived-run on both Mac and Windows.',
    sourceRefs: [
      'github.pr.vexlife-prod.396',
      'github.pr.vexlife-prod.397',
      'github.pr.vexlife-prod.391',
      'github.pr.vexlife-prod.389',
      'github.pr.vexlife-prod.387',
      'source.vexrelay.current-contract/v1',
    ],
  },
  lane: {
    entryClass: 'SAME_LANE_SUCCESSOR',
    scopeMode: 'STAGE_BOUNDED',
    laneRef: 'lane.vexlife-prod.pr396.webtoapp-application-shell',
    parentRootRef: 'github.pr.vexlife-prod.396',
    workRef: 'work.vexlife-prod.webtoapp-application-shell.source-owner.20261119',
    claimRef: 'claim.vexlife-prod.pr396.webtoapp-application-shell-source-owner',
    returnRouteRef: 'github.pr.vexlife-prod.397',
    ownerRef: 'github.pr.vexlife-prod.396',
    rootHubRef: 'github.pr.vexlife-prod.396',
  },
  role: {
    activeRole: 'CODER',
    roleRef: 'role.coder.vexlife-prod.pr396.a004',
    instanceRef: 'instance.vexlife-prod.pr396.a004',
    occupancyRef: 'occupancy.vexlife-prod.pr396.a004',
    providerBindingRef: 'provider.macos-victor.vexlife-prod.pr396.a004',
    threadRef: 'thread.local-vex-operations.pr396.a004',
    authorityEnvelopeRef: 'authority-envelope.vexlife-prod.pr396.a004',
  },
  capabilities: {
    platformGitHubRead: 'AVAILABLE',
    platformGitHubWrite: 'UNAVAILABLE',
  },
  execution: {
    hostProfile: 'macos-victor',
    executionSurface: 'MAC_ZIP_RUN_COMMAND',
    singleUseNonce: 'nonce.vexlife-prod.pr396.a004.20261119a004',
    duplicateAttemptMarker: 'duplicate.vexlife-prod.pr396.a004.20261119a004',
    duplicateProtectionMode: 'OUTER_RELAY_BOUND',
    formedAt: now.toISOString(),
    expiresAtOrNull: expires.toISOString(),
  },
  workspace: {
    mode: 'RUN_ROOT',
    projectRef: 'project.vexlife-prod.pr396.source-owner',
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
    baseBranch: 'VXG-111926-chatgpt-webtoapp-product-quit-a001',
    acceptedBase: BASE,
    checkoutRef: HEAD,
    expectedHead: HEAD,
    branch: BRANCH,
  }],
  authority: {
    authorityRef: 'authority.vexlife-prod.pr396.source-owner.a004',
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
    sourceSetRef: 'source-set.vexlife-prod.pr396.webtoapp-application-shell',
    sourceVersionRef: 'git.' + HEAD,
    overlapEvidenceRef: 'github.pr.vexlife-prod.387',
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
      'checkVexLifeStrings',
      'jvmTest',
      'compileKotlinJs',
      'compileTestKotlinJs',
      '--stacktrace',
      '--no-daemon',
      '--max-workers=1',
      '-Dorg.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m',
    ],
    predicateClass: 'SOURCE_OWNER_BEHAVIOR',
    predicateRef: 'proof.pr396.webtoapp-application-shell.whole-source',
    timeoutMs: 1200000,
  }],
  maxFiles: 10,
  stateRef: 'state.vexlife-prod.pr396.source-owner.a004',
  currentLifecycleState: 'SOURCE_READY',
  currentGoal:
    'Prove the settled exact PR #396 application-shell source after static audit aligned shared Quit meaning, exact Mac lifetime custody, native embedded durable-app versus browser-recovery carriage, WebKit/WebView2 feature containment, TCC privacy metadata, and the complete 22-path application/release membrane.',
  remainingGoals: [
    'Run fresh semantic/architecture Assurance on the exact proved application-shell candidate.',
    'Form and run the built Mac VexLife application on the real Mac through a truthful application-lived host effect.',
    'Form and run the built Windows VexLife application on the real Windows machine through the matching application-lived host effect.',
    'Reconcile current UI/localization convergence before any final lifecycle/merge consideration.',
  ],
  onPass: {
    lifecycleState: 'ASSURANCE_PENDING',
    nextFunction: 'F13.RUN_FRESH_ASSURANCE',
    nextOwner: 'IndependentAssurance',
    whyThisIsNext:
      'Repository source behavior must be executable and exact before semantic Assurance and the two separate built-application lived gates.',
  },
  resultFilename:
    'RETURN-VEXLIFE-PR396-WEBTOAPP-APPLICATION-SHELL-SOURCE-A004.zip',
};

const authorityEnvelope = {
  authoritySchemaRef: 'vextreme.effect-transaction/v1',
  semanticAdmissionRef: 'github.pr.vexlife-prod.396',
  transactionRef: 'transaction.vexlife-prod.pr396.source-owner.a004',
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
  taskRef: 'task.vexlife-prod.pr396.webtoapp-application-shell.carrier',
  attemptRef: 'attempt.vexlife-prod.pr396.webtoapp-application-shell.carrier.a004.20261119a004',
  packageRef: 'package.vexlife-prod.pr396.webtoapp-application-shell.carrier.a004.20261119a004',
  requestRef: 'request.vexlife-prod.pr396.webtoapp-application-shell.carrier.a004.20261119a004',
  correlationRef: 'correlation.vexlife-prod.pr396.webtoapp-application-shell.carrier.a004.20261119a004',
  taskStorageKey: 'task-vexlife-prod-pr396-webtoapp-application-shell-carrier',
  attemptStorageKey: 'attempt-a004',
  requestingRoleRef: 'Operations[PR396-A004]',
  requestingInstanceRef: 'instance.local-vex-operations.pr396.a004',
  parentOperationsRef: 'github.pr.vexlife-prod.396',
  returnRouteRef: 'github.pr.vexlife-prod.397',
  hostProfileRef: 'macos-victor',
  repositoryExecutionProfileRefOrNull:
    'profile.repository.vextreme-sdk.macos-v1',
  repositoryRefOrNull: 'VexGPT/VexLife-Prod',
  baseRefOrNull: BASE,
  headRefOrNull: HEAD,
  treeRefOrNull: TREE,
  branchRefOrNull: BRANCH,
  claimRefOrNull: 'claim.vexlife-prod.pr396.webtoapp-application-shell-source-owner',
  exactPathRefs: CHANGED_PATHS,
  authorityBinding: {
    authoritySchemaRef: 'vextreme.effect-transaction/v1',
    authorityEnvelopePath: 'authority/effect.json',
    authorityEnvelopeSha256: '0'.repeat(64),
    semanticAdmissionRef: 'github.pr.vexlife-prod.396',
    transactionRef: 'transaction.vexlife-prod.pr396.source-owner.a004',
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
  singleUseNonce: 'nonce.vexlife-prod.pr396.carrier.a004.20261119a004',
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
    'VEXLIFE-PR396-WEBTOAPP-APPLICATION-SHELL-SOURCE-OWNER-A004',
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
NODE_A004

echo 'A004: forming the exact current task/carrier...'
A004_SDK_ROOT="$SDK_ROOT" A004_FORMATION_RESULT="$FORMATION_RESULT" "$NODE_BIN" "$FORMATION_SCRIPT"

PACKAGE="$("$NODE_BIN" -e 'const fs=require("node:fs");const x=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));process.stdout.write(x.outerZip)' "$FORMATION_RESULT")"
EXPECTED="$("$NODE_BIN" -e 'const fs=require("node:fs");const x=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));process.stdout.write(x.outerSha256)' "$FORMATION_RESULT")"

[ -f "$PACKAGE" ] || { echo 'A004: formed package is missing.' >&2; exit 24; }
OBSERVED="$(/usr/bin/shasum -a 256 "$PACKAGE" | /usr/bin/cut -d ' ' -f 1)"
[ "$OBSERVED" = "$EXPECTED" ] || { echo 'A004: formed package digest mismatch.' >&2; exit 25; }

echo "A004: formed exact package $PACKAGE"
echo "A004: package sha256 $EXPECTED"
echo 'A004: entering current native VexRelay/VexLocalBridge carrier...'

set +e
VEX_EXPECTED_PACKAGE_SHA256="$EXPECTED" "$BASH_BIN" "$SDK_ROOT/lib/vexlocalbridge/templates/RUN-VEX-TASK.command" "$PACKAGE"
STATUS=$?
set -e

echo "A004: carrier exit $STATUS"

CAPSULE_ROOT="${PACKAGE%.zip}-run-${EXPECTED:0:16}"
RESULT_ROOT="$CAPSULE_ROOT/results"
RETURN_COUNT="$(/usr/bin/find "$RESULT_ROOT" -type f -name 'RETURN-*.zip' 2>/dev/null | /usr/bin/wc -l | /usr/bin/tr -d ' ')"
[ "$RETURN_COUNT" = '1' ] || {
  echo "A004: expected exactly one canonical outer RETURN, observed $RETURN_COUNT." >&2
  exit 30
}
RETURN_ZIP="$(/usr/bin/find "$RESULT_ROOT" -type f -name 'RETURN-*.zip' -print)"
"$RM_BIN" -f -- "$CONSUMED_RESULT"
"$NODE_BIN" "$CONSUMER" \
  --sdk-root "$SDK_ROOT" \
  --task-zip "$PACKAGE" \
  --return-zip "$RETURN_ZIP" \
  --output "$CONSUMED_RESULT"

SOURCE_PASS="$("$NODE_BIN" -e 'const fs=require("node:fs");const x=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));process.stdout.write(String(x.sourceOwnerPass))' "$CONSUMED_RESULT")"
echo "A004_CONSUMED_RESULT=$CONSUMED_RESULT"
cat "$CONSUMED_RESULT"

if [ "$SOURCE_PASS" = 'true' ]; then
  exit 0
fi
exit 2
