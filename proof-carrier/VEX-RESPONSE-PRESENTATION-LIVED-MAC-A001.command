#!/bin/bash
set -euo pipefail

# [VXG RealForever]
# ResponsePresentation lived Mac build A001.
#
# TEMPORARY TEST CARRIER ONLY.
# Source/merge authority remains PR #168.
# Build-carriage coordination is PR #174.
# This file lives on a no-PR carrier branch so the PR queue gains only the one
# human-visible temporary build carrier.
#
# No Local Workspace. No hosted GitHub Actions. Checkout-free current VexRelay bootstrap.

CANDIDATE_REPO='VexGPT/VexLife-Prod'
CANDIDATE_BASE='8f052bf48920dec6516ea0e4cf379464f78bc80a'
CANDIDATE_HEAD='f7613418da7ec1472eb8d9bf00e8bc49236b3fba'
CANDIDATE_TREE='fdff1b9ee84d44a62b2ded708484be1481f30f35'
CANDIDATE_BRANCH='VXG-111526-response-presentation-lived-mac-build-a001'

SDK_REPO='vgong24/Vextreme-SDK'
SDK_HEAD='8543c7d755faba04d4c4019c02e84f992667918a'
SDK_TREE='6376f68279f0750ee6d3868b8472368199763064'
CONTRACT_REF='source.vexrelay.current-contract/v1'
RUNTIME_VERSION='v0.2.0-RC1'

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

GH="$(resolve_exec /opt/homebrew/bin/gh /usr/local/bin/gh)" || {
  printf '%s\n' 'ResponsePresentation A001: GitHub CLI unavailable.' >&2
  exit 12
}
GIT="$(resolve_exec /usr/bin/git)" || {
  printf '%s\n' 'ResponsePresentation A001: Git unavailable.' >&2
  exit 11
}
NODE="$(resolve_exec /opt/homebrew/bin/node /usr/local/bin/node /usr/bin/node)" || {
  printf '%s\n' 'ResponsePresentation A001: Node unavailable.' >&2
  exit 13
}

"$GH" auth status --hostname github.com >/dev/null 2>&1 || {
  printf '%s\n' 'ResponsePresentation A001: GitHub CLI authentication unavailable; no Relay task formed.' >&2
  exit 21
}

BOOT_ROOT="$(/usr/bin/mktemp -d "$HOME/Downloads/.response-presentation-relay-a001.XXXXXX")"
cleanup() {
  case "$BOOT_ROOT" in
    "$HOME/Downloads"/.response-presentation-relay-a001.*) /bin/rm -rf -- "$BOOT_ROOT" ;;
  esac
}
trap cleanup EXIT HUP INT TERM

SDK_ROOT="$BOOT_ROOT/Vextreme-SDK"

printf '%s\n' 'ResponsePresentation A001: acquiring exact current VexRelay/VexLocalBridge source...'
"$GH" repo clone "$SDK_REPO" "$SDK_ROOT" -- --no-checkout >/dev/null
"$GIT" -C "$SDK_ROOT" checkout --detach "$SDK_HEAD" >/dev/null

OBSERVED_HEAD="$("$GIT" -C "$SDK_ROOT" rev-parse HEAD)"
OBSERVED_TREE="$("$GIT" -C "$SDK_ROOT" rev-parse 'HEAD^{tree}')"
[ "$OBSERVED_HEAD" = "$SDK_HEAD" ] || {
  printf '%s\n' 'ResponsePresentation A001: SDK head mismatch.' >&2
  exit 23
}
[ "$OBSERVED_TREE" = "$SDK_TREE" ] || {
  printf '%s\n' 'ResponsePresentation A001: SDK tree mismatch.' >&2
  exit 24
}
[ -z "$("$GIT" -C "$SDK_ROOT" status --porcelain=v1 -uall)" ] || {
  printf '%s\n' 'ResponsePresentation A001: acquired SDK source is dirty.' >&2
  exit 25
}

CONTRACT="$SDK_ROOT/docs/private-continuity/vexrelay/current-contract.json"
"$NODE" - "$CONTRACT" "$CONTRACT_REF" "$RUNTIME_VERSION" <<'NODECHECK'
const fs=require('node:fs');
const [file,ref,runtime]=process.argv.slice(2);
const value=JSON.parse(fs.readFileSync(file,'utf8'));
if(value.contractRef!==ref) {
  console.error('ResponsePresentation A001: Relay contractRef mismatch: '+value.contractRef);
  process.exit(27);
}
if(value.currentRuntimeVersion!==runtime) {
  console.error('ResponsePresentation A001: Relay runtime mismatch: '+value.currentRuntimeVersion);
  process.exit(28);
}
if(value.routeSelection?.effectClassRoutes?.REPOSITORY_OPERATIONS_HOST_CARRIER
   !== 'adapter.vexlocalbridge.native-vexrelay-task-archive/v1') {
  console.error('ResponsePresentation A001: current repository host-carrier route mismatch.');
  process.exit(29);
}
NODECHECK

printf '%s\n' 'ResponsePresentation A001: exact Relay source bound; forming canonical local proof/build package...'

"$NODE" --input-type=module - "$SDK_ROOT" "$CANDIDATE_REPO" "$CANDIDATE_BASE" "$CANDIDATE_HEAD" "$CANDIDATE_TREE" "$CANDIDATE_BRANCH" <<'NODEFORM'
import os from 'node:os';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import { pathToFileURL } from 'node:url';

const [
  sdkRoot,
  candidateRepo,
  candidateBase,
  candidateHead,
  candidateTree,
  candidateBranch,
] = process.argv.slice(2);

const formationUrl = pathToFileURL(
  path.join(sdkRoot,'lib/vexlocalbridge/vexrelay-task-archive-carrier-formation.mjs'),
).href;
const { formVexRelayTaskArchiveCarrier } = await import(formationUrl);
if (typeof formVexRelayTaskArchiveCarrier !== 'function') {
  throw new Error('CURRENT_VEXRELAY_FORMATION_EXPORT_UNAVAILABLE');
}

const artifactDir =
  path.join(os.homedir(),'Downloads','VEXLIFE-RESPONSE-PRESENTATION-A001-'+candidateHead.slice(0,12));

const escapedArtifactDir = artifactDir.replaceAll('"','\\"');
const proofShell = [
  'set -euo pipefail',
  'J21="$(/usr/libexec/java_home -v 21 2>/dev/null)" || { printf "%s\\n" "ResponsePresentation A001: JDK 21 is unavailable." >&2; exit 21; }',
  '[ -n "$J21" ] && [ -x "$J21/bin/java" ] || { printf "%s\\n" "ResponsePresentation A001: resolved JDK 21 is invalid." >&2; exit 22; }',
  'export JAVA_HOME="$J21"',
  'export PATH="$JAVA_HOME/bin:$PATH"',
  '"$JAVA_HOME/bin/java" -version',
  './gradlew --no-daemon -Dorg.gradle.java.home="$J21" --version',
  './gradlew --no-daemon -Dorg.gradle.java.home="$J21" jvmTest compileKotlinJs compileTestKotlinJs checkVexLifeStrings',
  './gradlew --no-daemon -Dorg.gradle.java.home="$J21" -PvexlifeAppVersion=0.0.0 archiveVexLifeProdMacApplication',
  'APP_DIR="build/vexlife-prod-mac-application/'+candidateHead+'"',
  'DMG="$APP_DIR/VexLife-macOS-AppleSilicon.dmg"',
  'SUM="$APP_DIR/VexLife-macOS-AppleSilicon.dmg.sha256"',
  'RECEIPT="$APP_DIR/VexLife-macOS-AppleSilicon.receipt.json"',
  'test -f "$DMG" && test -f "$SUM" && test -f "$RECEIPT"',
  '(cd "$APP_DIR" && /usr/bin/shasum -a 256 -c "VexLife-macOS-AppleSilicon.dmg.sha256")',
  '/usr/bin/hdiutil verify "$DMG"',
  '/bin/mkdir -p "'+escapedArtifactDir+'"',
  '/bin/cp -f "$DMG" "'+escapedArtifactDir+'/VexLife-macOS-AppleSilicon.dmg"',
  '/bin/cp -f "$SUM" "'+escapedArtifactDir+'/VexLife-macOS-AppleSilicon.dmg.sha256"',
  '/bin/cp -f "$RECEIPT" "'+escapedArtifactDir+'/VexLife-macOS-AppleSilicon.receipt.json"',
  '/usr/bin/shasum -a 256 "'+escapedArtifactDir+'/VexLife-macOS-AppleSilicon.dmg"',
  'printf "%s\\n" "ResponsePresentation A001 test artifact: '+escapedArtifactDir+'/VexLife-macOS-AppleSilicon.dmg"',
  '[ -z "$(/usr/bin/git status --porcelain=v1 -uall)" ] || { printf "%s\\n" "ResponsePresentation A001: proof left the candidate worktree dirty." >&2; /usr/bin/git status --porcelain=v1 -uall >&2; exit 31; }',
].join('; ');

const now = new Date();
const expires = new Date(now.getTime() + 4 * 60 * 60 * 1000);

const seed = {
  schemaVersion: 'vex.relay.task/v2',
  continuity: '[VXG RealForever]',
  taskRef: 'task.vexlife-prod.response-presentation-lived-mac.001',
  attemptRef: 'attempt.vexlife-prod.response-presentation-lived-mac.a001',
  requestRef: 'request.vexlife-prod.response-presentation-lived-mac.a001',
  correlationRef: 'correlation.vexlife-prod.response-presentation-lived-mac.a001',
  packageRef: 'package.vexlife-prod.response-presentation-lived-mac.a001',
  original: {
    originalIntent:
      'Make Vex streamed responses human-readable as ResponsePresentation while preserving visible structure and human scroll ownership as Markdown patterns take shape.',
    originalGoal:
      'Prove the exact temporary Mac build candidate and form one unsigned DMG for lived ResponsePresentation testing.',
    userProtectedIntent:
      'Use a temporary GitHub PR mapped to #168 and current VexRelay local execution instead of handing the human a task ZIP; close the temporary carrier once #168 is integrated.',
    sourceRefs: [
      'github.pull.vexlife-prod.168',
      'github.pull.vexlife-prod.174',
      'github.pull.vexlife-prod.161',
      'source.vexrelay.current-contract/v1',
    ],
  },
  lane: {
    entryClass: 'SAME_LANE_ROLE_TRANSITION',
    scopeMode: 'STAGE_BOUNDED',
    laneRef: 'lane.vexlife-prod.response-presentation',
    parentRootRef: 'root.vexlife-prod.new-beginnings',
    workRef: 'work.vexlife-prod.response-presentation',
    claimRef: 'github.pull.vexlife-prod.174',
    returnRouteRef: 'github.pull.vexlife-prod.174',
    ownerRef: 'owner.vexlife-prod.response-presentation',
    rootHubRef: 'hub.vexlife-prod.new-beginnings',
  },
  role: {
    activeRole: 'CODER',
    roleRef: 'role.coder.vexlife-prod.response-presentation-lived-mac',
    instanceRef: 'instance.vexrelay.vexlife-prod.response-presentation-lived-mac.a001',
    occupancyRef: 'occupancy.vexrelay.vexlife-prod.response-presentation-lived-mac.a001',
    providerBindingRef: 'provider.vexlocalbridge.macos-victor.response-presentation-lived-mac.a001',
    threadRef: 'thread.vexlife-prod.response-presentation',
    authorityEnvelopeRef: 'authority-envelope.vexlife-prod.response-presentation-lived-mac.a001',
  },
  capabilities: {
    platformGitHubRead: 'AVAILABLE',
    platformGitHubWrite: 'UNAVAILABLE',
  },
  execution: {
    hostProfile: 'macos-victor',
    executionSurface: 'MAC_ZIP_RUN_COMMAND',
    singleUseNonce: 'nonce-vexlife-prod-response-presentation-lived-mac-a001-20261116',
    duplicateAttemptMarker: 'marker.vexlife-prod.response-presentation-lived-mac.a001',
    duplicateProtectionMode: 'OUTER_RELAY_BOUND',
    formedAt: now.toISOString(),
    expiresAtOrNull: expires.toISOString(),
  },
  workspace: {
    mode: 'RUN_ROOT',
    projectRef: 'project.vexlife-prod.response-presentation',
    projectPath: 'workspace',
    createRoot: false,
    createProjectPath: true,
  },
  repositories: [{
    repositoryRef: 'primary',
    mode: 'CLONE_IN_WORKSPACE',
    cloneTransport: 'GITHUB_GH',
    githubHostname: 'github.com',
    nameWithOwner: candidateRepo,
    localPath: 'repositories/primary',
    remote: 'origin',
    baseBranch: 'main',
    acceptedBase: candidateBase,
    checkoutRef: candidateHead,
    expectedHead: candidateHead,
    branch: candidateBranch,
    expectedRemoteUrl: null,
  }],
  authority: {
    authorityRef: 'authority.vexlife-prod.response-presentation-lived-mac.a001',
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
      'CREATE_COMMIT',
      'PUSH_BRANCH',
      'UPDATE_COORDINATION_METADATA',
      'APPROVE_EXACT_HEAD',
      'MARK_READY',
      'ORDINARY_MERGE',
      'POST_MERGE_VERIFY',
      'MODEL_RUNTIME',
      'HOME_MUTATION',
    ],
    repositoryMembranes: [{
      repositoryRef: 'primary',
      allowedPaths: [
        '.github',
        'build.gradle.kts',
        'docs',
        'localization',
        'packaging',
        'site',
        'src',
        'tools',
      ],
    }],
    allowedExecutables: ['/bin/bash','git','gh','node'],
    allowedEnvironment: [],
    remoteObservationOwner: 'Operations',
    allowMutationLoop: false,
    stopConditions: [
      'STOP_ON_UNKNOWN',
      'STOP_ON_AUTH_BLOCK',
      'STOP_ON_BASE_OR_BRANCH_DRIFT',
      'STOP_ON_TEST_FAILURE',
    ],
  },
  source: {
    sourceSetRef: 'source-set.vexlife-prod.response-presentation-lived-mac',
    sourceVersionRef: candidateHead,
    overlapEvidenceRef: 'github.pull.vexlife-prod.174',
    activeConflictState: 'NONE',
    candidateState: 'EXACT',
    candidateHead,
    candidateTree,
    expectedChangedPaths: [],
    intentionallyUnchangedAuthorizedPaths: [],
  },
  policy: {
    maxOuterReturnFiles: 10,
    maxSelectedSourceEntries: 10,
    maxSelectedSourceBytes: 500000,
    maxDiffBytes: 5000000,
    requireRacePreflightBeforeMutation: false,
    requireLocalValidationBeforePublish: false,
    requireInputPackageManifest: true,
    signoffCommit: true,
    draftPullRequest: true,
  },
};

const stage = {
  repositoryRef: 'primary',
  expectedBase: candidateBase,
  expectedHead: candidateHead,
  expectedTree: candidateTree,
  branch: candidateBranch,
  proofCommands: [{
    command: '/bin/bash',
    args: ['-lc', proofShell],
    proofClass: 'TEST',
    candidateEvidence: true,
    predicateClass: 'SOURCE_OWNER_BEHAVIOR',
    predicateRef: 'predicate.vexlife-prod.response-presentation-lived-mac.source-build',
    timeoutMs: 1200000,
  }],
  maxFiles: 10,
  stateRef: 'state.vexlife-prod.response-presentation-lived-mac',
  currentLifecycleState: 'SOURCE_READY',
  currentGoal:
    'Prove exact ResponsePresentation source over current main packaging and form one local unsigned Mac DMG for lived streaming-presentation testing.',
  remainingGoals: [
    'Install/open the exact local DMG on the Mac.',
    'Stream a long quote/table/list/code response and observe presentation stability.',
    'Scroll away from the tail while the response continues and confirm human scroll ownership.',
    'Return the canonical Relay RETURN and lived observation to PR #174 / source PR #168.',
  ],
  onPass: {
    lifecycleState: 'SOURCE_READY',
    nextFunction: 'F08.EXECUTE_OR_HANDOFF',
    nextOwner: 'Victor',
    whyThisIsNext:
      'Exact source/build/DMG formation is complete; installing/opening the app and judging the lived ResponsePresentation behavior remain host/human effects.',
  },
  resultFilename: 'RETURN-VEXLIFE-RESPONSE-PRESENTATION-LIVED-MAC-A001.zip',
};

const authorityEnvelope = {
  authoritySchemaRef: 'vextreme.effect-transaction/v1',
  semanticAdmissionRef: 'github.pull.vexlife-prod.174',
  transactionRef: 'transaction.vexlife-prod.response-presentation-lived-mac.a001',
  transactionVersion: 1,
  allowedEffectRefs: ['effect.vexrelay.execute'],
  heldEffectRefs: [
    'effect.vexlife-prod.source-mutation',
    'effect.vexlife-prod.product-runtime',
    'effect.vexlife-prod.home-mutation',
    'effect.vexlife-prod.merge',
  ],
};

const outerTaskTemplate = {
  schemaVersion: 'vextreme.vexlocalbridge-task-manifest/v1',
  artifactClass: 'EXECUTABLE_RELAY',
  taskRef: 'task.vexlocalbridge.vexlife-prod.response-presentation-lived-mac.001',
  attemptRef: 'attempt.vexlocalbridge.vexlife-prod.response-presentation-lived-mac.a001',
  packageRef: 'package.vexlocalbridge.vexlife-prod.response-presentation-lived-mac.a001',
  requestRef: 'request.vexlocalbridge.vexlife-prod.response-presentation-lived-mac.a001',
  correlationRef: 'correlation.vexlocalbridge.vexlife-prod.response-presentation-lived-mac.a001',
  taskStorageKey: 'vexlife-prod-response-presentation-lived-mac',
  attemptStorageKey: 'attempt-a001',
  requestingRoleRef: 'Operations[VexLife-Prod]',
  requestingInstanceRef: 'instance.operations.vexlife-prod.response-presentation-lived-mac',
  parentOperationsRef: 'github.pull.vexlife-prod.174',
  returnRouteRef: 'github.pull.vexlife-prod.174',
  hostProfileRef: 'macos-victor',
  repositoryExecutionProfileRefOrNull: 'profile.repository.vextreme-sdk.macos-v1',
  repositoryRefOrNull: candidateRepo,
  baseRefOrNull: candidateBase,
  headRefOrNull: candidateHead,
  treeRefOrNull: candidateTree,
  branchRefOrNull: candidateBranch,
  claimRefOrNull: 'github.pull.vexlife-prod.174',
  exactPathRefs: [],
  authorityBinding: {
    authoritySchemaRef: authorityEnvelope.authoritySchemaRef,
    authorityEnvelopePath: 'authority/effect.json',
    authorityEnvelopeSha256: '0'.repeat(64),
    semanticAdmissionRef: authorityEnvelope.semanticAdmissionRef,
    transactionRef: authorityEnvelope.transactionRef,
    transactionVersion: authorityEnvelope.transactionVersion,
    allowedEffectRefs: [...authorityEnvelope.allowedEffectRefs],
    heldEffectRefs: [...authorityEnvelope.heldEffectRefs],
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
  singleUseNonce: 'nonce-vlb-vexlife-prod-response-presentation-lived-mac-a001-20261116',
  stopConditions: [
    'stop.on.identity.drift',
    'stop.on.first.substantive.failure',
  ],
  archiveDigestBinding: 'FILENAME_SHA256',
};

const formed = await formVexRelayTaskArchiveCarrier({
  recipe: 'ExactHeadSourceOwnerProof',
  seed,
  stage,
  outerTaskTemplate,
  authorityEnvelope,
  outputDirectory: path.join(os.homedir(),'Downloads'),
  outputStem: 'VEXLIFE-RESPONSE-PRESENTATION-LIVED-MAC-A001-RELAY',
  ttlMs: 4 * 60 * 60 * 1000,
});

console.log('Canonical Relay package: '+formed.outer.finalZip);
console.log('Canonical Relay package SHA-256: '+formed.outer.digest);
console.log('Inner task: '+formed.inner.manifest.taskRef);
console.log('Inner archive SHA-256: '+formed.inner.sha256);

const runner = path.join(sdkRoot,'lib/vexlocalbridge/templates/RUN-VEX-TASK.command');
const child = spawnSync('/bin/bash',[runner,formed.outer.finalZip],{
  stdio:'inherit',
  env:process.env,
});
if(child.error) throw child.error;
process.exit(child.status ?? 2);
NODEFORM
