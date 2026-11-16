#!/bin/bash
set -euo pipefail

# [VXG RealForever]
# TextPresentation lived Mac build A003.
#
# TEMPORARY TEST CARRIER ONLY.
# Source/merge authority remains PR #168.
# Build-carriage coordination is PR #174.
# This file lives on a no-PR carrier branch so the PR queue gains only the one
# human-visible temporary build carrier.
#
# No Local Workspace. No hosted GitHub Actions. Checkout-free current VexRelay bootstrap.

CANDIDATE_REPO='VexGPT/VexLife-Prod'
CANDIDATE_BASE='8ad527581c6f08c882c00b03ebac9e7fada58d3a'
CANDIDATE_HEAD='830b3d4b8285dedb3fc1cd0756f1110dfd44f0b7'
CANDIDATE_TREE='5d11fd5448f372af08f3277a22aedba1ce9332e1'
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
  printf '%s\n' 'TextPresentation A003: GitHub CLI unavailable.' >&2
  exit 12
}
GIT="$(resolve_exec /usr/bin/git)" || {
  printf '%s\n' 'TextPresentation A003: Git unavailable.' >&2
  exit 11
}
NODE="$(resolve_exec /opt/homebrew/bin/node /usr/local/bin/node /usr/bin/node)" || {
  printf '%s\n' 'TextPresentation A003: Node unavailable.' >&2
  exit 13
}

"$GH" auth status --hostname github.com >/dev/null 2>&1 || {
  printf '%s\n' 'TextPresentation A003: GitHub CLI authentication unavailable; no Relay task formed.' >&2
  exit 21
}

BOOT_ROOT="$(/usr/bin/mktemp -d "$HOME/Downloads/.text-presentation-relay-a003.XXXXXX")"
cleanup() {
  case "$BOOT_ROOT" in
    "$HOME/Downloads"/.text-presentation-relay-a003.*) /bin/rm -rf -- "$BOOT_ROOT" ;;
  esac
}
trap cleanup EXIT HUP INT TERM

SDK_ROOT="$BOOT_ROOT/Vextreme-SDK"

printf '%s\n' 'TextPresentation A003: acquiring exact current VexRelay/VexLocalBridge source...'
"$GH" repo clone "$SDK_REPO" "$SDK_ROOT" -- --no-checkout >/dev/null
"$GIT" -C "$SDK_ROOT" checkout --detach "$SDK_HEAD" >/dev/null

OBSERVED_HEAD="$("$GIT" -C "$SDK_ROOT" rev-parse HEAD)"
OBSERVED_TREE="$("$GIT" -C "$SDK_ROOT" rev-parse 'HEAD^{tree}')"
[ "$OBSERVED_HEAD" = "$SDK_HEAD" ] || {
  printf '%s\n' 'TextPresentation A003: SDK head mismatch.' >&2
  exit 23
}
[ "$OBSERVED_TREE" = "$SDK_TREE" ] || {
  printf '%s\n' 'TextPresentation A003: SDK tree mismatch.' >&2
  exit 24
}
[ -z "$("$GIT" -C "$SDK_ROOT" status --porcelain=v1 -uall)" ] || {
  printf '%s\n' 'TextPresentation A003: acquired SDK source is dirty.' >&2
  exit 25
}

CONTRACT="$SDK_ROOT/docs/private-continuity/vexrelay/current-contract.json"
"$NODE" - "$CONTRACT" "$CONTRACT_REF" "$RUNTIME_VERSION" <<'NODECHECK'
const fs=require('node:fs');
const [file,ref,runtime]=process.argv.slice(2);
const value=JSON.parse(fs.readFileSync(file,'utf8'));
if(value.contractRef!==ref) {
  console.error('TextPresentation A003: Relay contractRef mismatch: '+value.contractRef);
  process.exit(27);
}
if(value.currentRuntimeVersion!==runtime) {
  console.error('TextPresentation A003: Relay runtime mismatch: '+value.currentRuntimeVersion);
  process.exit(28);
}
if(value.routeSelection?.effectClassRoutes?.REPOSITORY_OPERATIONS_HOST_CARRIER
   !== 'adapter.vexlocalbridge.native-vexrelay-task-archive/v1') {
  console.error('TextPresentation A003: current repository host-carrier route mismatch.');
  process.exit(29);
}
NODECHECK

printf '%s\n' 'TextPresentation A003: exact Relay source bound; forming canonical local proof/build package...'

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
  path.join(os.homedir(),'Downloads','VEXLIFE-TEXT-PRESENTATION-A003-'+candidateHead.slice(0,12));

const escapedArtifactDir = artifactDir.replaceAll('"','\\"');
const proofShell = [
  'set -euo pipefail',
  'J21="$(/usr/libexec/java_home -v 21 2>/dev/null)" || { printf "%s\\n" "TextPresentation A003: JDK 21 is unavailable." >&2; exit 21; }',
  '[ -n "$J21" ] && [ -x "$J21/bin/java" ] || { printf "%s\\n" "TextPresentation A003: resolved JDK 21 is invalid." >&2; exit 22; }',
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
  'printf "%s\\n" "TextPresentation A003 test artifact: '+escapedArtifactDir+'/VexLife-macOS-AppleSilicon.dmg"',
  '[ -z "$(/usr/bin/git status --porcelain=v1 -uall)" ] || { printf "%s\\n" "TextPresentation A003: proof left the candidate worktree dirty." >&2; /usr/bin/git status --porcelain=v1 -uall >&2; exit 31; }',
].join('; ');

const now = new Date();
const expires = new Date(now.getTime() + 4 * 60 * 60 * 1000);

const seed = {
  schemaVersion: 'vex.relay.task/v2',
  continuity: '[VXG RealForever]',
  taskRef: 'task.vexlife-prod.text-presentation-lived-mac.001',
  attemptRef: 'attempt.vexlife-prod.text-presentation-lived-mac.a003',
  requestRef: 'request.vexlife-prod.text-presentation-lived-mac.a003',
  correlationRef: 'correlation.vexlife-prod.text-presentation-lived-mac.a003',
  packageRef: 'package.vexlife-prod.text-presentation-lived-mac.a003',
  original: {
    originalIntent:
      'Make Vex reply and human-visible reasoning text share one TextPresentation relationship while preserving their distinct custody and independent human scroll ownership as Markdown patterns take shape.',
    originalGoal:
      'Prove the exact refreshed Mac candidate and form one unsigned DMG for lived TextPresentation testing across both replies and thinking.',
    userProtectedIntent:
      'Use temporary PR #174 mapped to #168 and current VexRelay local execution instead of an input ZIP; A002 safely exposed one stale proof assertion, now corrected without changing runtime behavior.',
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
    laneRef: 'lane.vexlife-prod.text-presentation',
    parentRootRef: 'root.vexlife-prod.new-beginnings',
    workRef: 'work.vexlife-prod.text-presentation',
    claimRef: 'github.pull.vexlife-prod.174',
    returnRouteRef: 'github.pull.vexlife-prod.174',
    ownerRef: 'owner.vexlife-prod.text-presentation',
    rootHubRef: 'hub.vexlife-prod.new-beginnings',
  },
  role: {
    activeRole: 'CODER',
    roleRef: 'role.coder.vexlife-prod.text-presentation-lived-mac',
    instanceRef: 'instance.vexrelay.vexlife-prod.text-presentation-lived-mac.a003',
    occupancyRef: 'occupancy.vexrelay.vexlife-prod.text-presentation-lived-mac.a003',
    providerBindingRef: 'provider.vexlocalbridge.macos-victor.text-presentation-lived-mac.a003',
    threadRef: 'thread.vexlife-prod.text-presentation',
    authorityEnvelopeRef: 'authority-envelope.vexlife-prod.text-presentation-lived-mac.a003',
  },
  capabilities: {
    platformGitHubRead: 'AVAILABLE',
    platformGitHubWrite: 'UNAVAILABLE',
  },
  execution: {
    hostProfile: 'macos-victor',
    executionSurface: 'MAC_ZIP_RUN_COMMAND',
    singleUseNonce: 'nonce-vexlife-prod-text-presentation-lived-mac-a003-20261116',
    duplicateAttemptMarker: 'marker.vexlife-prod.text-presentation-lived-mac.a003',
    duplicateProtectionMode: 'OUTER_RELAY_BOUND',
    formedAt: now.toISOString(),
    expiresAtOrNull: expires.toISOString(),
  },
  workspace: {
    mode: 'RUN_ROOT',
    projectRef: 'project.vexlife-prod.text-presentation',
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
    authorityRef: 'authority.vexlife-prod.text-presentation-lived-mac.a003',
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
    sourceSetRef: 'source-set.vexlife-prod.text-presentation-lived-mac',
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
    predicateRef: 'predicate.vexlife-prod.text-presentation-lived-mac.source-build',
    timeoutMs: 1200000,
  }],
  maxFiles: 10,
  stateRef: 'state.vexlife-prod.text-presentation-lived-mac',
  currentLifecycleState: 'SOURCE_READY',
  currentGoal:
    'Prove exact TextPresentation source over current main packaging and form one local unsigned Mac DMG for lived reply + thinking presentation testing.',
  remainingGoals: [
    'Install/open the exact local DMG on the Mac.',
    'Stream Markdown-rich reasoning and a long quote/table/list/code final reply and observe presentation stability in both.',
    'Scroll away inside the thinking viewport and the Conversation timeline while streaming continues; confirm each human scroll owner remains independent.',
    'Return the canonical Relay RETURN and lived observation to PR #174 / source PR #168.',
  ],
  onPass: {
    lifecycleState: 'SOURCE_READY',
    nextFunction: 'F08.EXECUTE_OR_HANDOFF',
    nextOwner: 'Victor',
    whyThisIsNext:
      'Exact source/build/DMG formation is complete; installing/opening the app and judging the lived TextPresentation behavior remain host/human effects.',
  },
  resultFilename: 'RETURN-VEXLIFE-TEXT-PRESENTATION-LIVED-MAC-A003.zip',
};

const authorityEnvelope = {
  authoritySchemaRef: 'vextreme.effect-transaction/v1',
  semanticAdmissionRef: 'github.pull.vexlife-prod.174',
  transactionRef: 'transaction.vexlife-prod.text-presentation-lived-mac.a003',
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
  taskRef: 'task.vexlocalbridge.vexlife-prod.text-presentation-lived-mac.001',
  attemptRef: 'attempt.vexlocalbridge.vexlife-prod.text-presentation-lived-mac.a003',
  packageRef: 'package.vexlocalbridge.vexlife-prod.text-presentation-lived-mac.a003',
  requestRef: 'request.vexlocalbridge.vexlife-prod.text-presentation-lived-mac.a003',
  correlationRef: 'correlation.vexlocalbridge.vexlife-prod.text-presentation-lived-mac.a003',
  taskStorageKey: 'vexlife-prod-text-presentation-lived-mac',
  attemptStorageKey: 'attempt-a003',
  requestingRoleRef: 'Operations[VexLife-Prod]',
  requestingInstanceRef: 'instance.operations.vexlife-prod.text-presentation-lived-mac',
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
  singleUseNonce: 'nonce-vlb-vexlife-prod-text-presentation-lived-mac-a003-20261116',
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
  outputStem: 'VEXLIFE-TEXT-PRESENTATION-LIVED-MAC-A003-RELAY',
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
