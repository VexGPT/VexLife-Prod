#!/bin/bash
set -euo pipefail

# [VXG RealForever]
# Vex Memory Foundation lived Mac build A002.
# Proof-carrier only: the Product candidate remains immutable on PR #166.
# No Local Workspace. No GitHub Actions. Checkout-free VexRelay bootstrap.

CANDIDATE_REPO='VexGPT/VexLife-Prod'
CANDIDATE_BASE='0aa1de1341cfd5e0c6135cbd30bfa8d19613d6d5'
CANDIDATE_HEAD='d80b54d67df9ca84834f9d04469e290f6e05065c'
CANDIDATE_TREE='cf1ab06b4433d528754a0fc2dd1afb4cb9a0149c'
CANDIDATE_BRANCH='VXG-111526-vex-memory-lived-mac-build-a001'

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
  printf '%s\n' 'Vex Memory A002: GitHub CLI unavailable.' >&2
  exit 12
}
GIT="$(resolve_exec /usr/bin/git)" || {
  printf '%s\n' 'Vex Memory A002: Git unavailable.' >&2
  exit 11
}
NODE="$(resolve_exec /opt/homebrew/bin/node /usr/local/bin/node /usr/bin/node)" || {
  printf '%s\n' 'Vex Memory A002: Node unavailable.' >&2
  exit 13
}

"$GH" auth status --hostname github.com >/dev/null 2>&1 || {
  printf '%s\n' 'Vex Memory A002: GitHub CLI authentication unavailable; no Relay task formed.' >&2
  exit 21
}

BOOT_ROOT="$(/usr/bin/mktemp -d "$HOME/Downloads/.vex-memory-relay-a002.XXXXXX")"
cleanup() {
  case "$BOOT_ROOT" in
    "$HOME/Downloads"/.vex-memory-relay-a002.*) /bin/rm -rf -- "$BOOT_ROOT" ;;
  esac
}
trap cleanup EXIT HUP INT TERM

SDK_ROOT="$BOOT_ROOT/Vextreme-SDK"

printf '%s\n' 'Vex Memory A002: acquiring exact current VexRelay/VexLocalBridge source...'
"$GH" repo clone "$SDK_REPO" "$SDK_ROOT" -- --no-checkout >/dev/null
"$GIT" -C "$SDK_ROOT" checkout --detach "$SDK_HEAD" >/dev/null

OBSERVED_HEAD="$("$GIT" -C "$SDK_ROOT" rev-parse HEAD)"
OBSERVED_TREE="$("$GIT" -C "$SDK_ROOT" rev-parse 'HEAD^{tree}')"
[ "$OBSERVED_HEAD" = "$SDK_HEAD" ] || {
  printf '%s\n' 'Vex Memory A002: SDK head mismatch.' >&2
  exit 23
}
[ "$OBSERVED_TREE" = "$SDK_TREE" ] || {
  printf '%s\n' 'Vex Memory A002: SDK tree mismatch.' >&2
  exit 24
}
[ -z "$("$GIT" -C "$SDK_ROOT" status --porcelain=v1 -uall)" ] || {
  printf '%s\n' 'Vex Memory A002: acquired SDK source is dirty.' >&2
  exit 25
}

CONTRACT="$SDK_ROOT/docs/private-continuity/vexrelay/current-contract.json"
"$NODE" - "$CONTRACT" "$CONTRACT_REF" "$RUNTIME_VERSION" <<'NODECHECK'
const fs=require('node:fs');
const [file,ref,runtime]=process.argv.slice(2);
const value=JSON.parse(fs.readFileSync(file,'utf8'));
if(value.contractRef!==ref) {
  console.error('Vex Memory A002: Relay contractRef mismatch: '+value.contractRef);
  process.exit(27);
}
if(value.currentRuntimeVersion!==runtime) {
  console.error('Vex Memory A002: Relay runtime mismatch: '+value.currentRuntimeVersion);
  process.exit(28);
}
if(value.routeSelection?.effectClassRoutes?.REPOSITORY_OPERATIONS_HOST_CARRIER
   !== 'adapter.vexlocalbridge.native-vexrelay-task-archive/v1') {
  console.error('Vex Memory A002: current repository host-carrier route mismatch.');
  process.exit(29);
}
NODECHECK

printf '%s\n' 'Vex Memory A002: exact Relay source bound; forming canonical local proof/build package...'

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
  path.join(os.homedir(),'Downloads','VEXLIFE-MEMORY-LIVED-A001-'+candidateHead.slice(0,12));

const proofShell = [
  'set -euo pipefail',
  'J21="$(/usr/libexec/java_home -v 21 2>/dev/null)" || { printf "%s\\n" "Vex Memory A002: JDK 21 is unavailable." >&2; exit 21; }',
  '[ -n "$J21" ] && [ -x "$J21/bin/java" ] || { printf "%s\\n" "Vex Memory A002: resolved JDK 21 is invalid." >&2; exit 22; }',
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
  '/bin/mkdir -p "'+artifactDir.replaceAll('"','\\"')+'"',
  '/bin/cp -f "$DMG" "'+artifactDir.replaceAll('"','\\"')+'/VexLife-macOS-AppleSilicon.dmg"',
  '/bin/cp -f "$SUM" "'+artifactDir.replaceAll('"','\\"')+'/VexLife-macOS-AppleSilicon.dmg.sha256"',
  '/bin/cp -f "$RECEIPT" "'+artifactDir.replaceAll('"','\\"')+'/VexLife-macOS-AppleSilicon.receipt.json"',
  '/usr/bin/shasum -a 256 "'+artifactDir.replaceAll('"','\\"')+'/VexLife-macOS-AppleSilicon.dmg"',
  'printf "%s\\n" "Vex Memory A002 test artifact: '+artifactDir.replaceAll('"','\\"')+'/VexLife-macOS-AppleSilicon.dmg"',
  '[ -z "$(/usr/bin/git status --porcelain=v1 -uall)" ] || { printf "%s\\n" "Vex Memory A002: proof left the candidate worktree dirty." >&2; /usr/bin/git status --porcelain=v1 -uall >&2; exit 31; }',
].join('; ');

const now = new Date();
const expires = new Date(now.getTime() + 4 * 60 * 60 * 1000);

const seed = {
  schemaVersion: 'vex.relay.task/v2',
  continuity: '[VXG RealForever]',
  taskRef: 'task.vexlife-prod.vex-memory-lived-mac.001',
  attemptRef: 'attempt.vexlife-prod.vex-memory-lived-mac.a002',
  requestRef: 'request.vexlife-prod.vex-memory-lived-mac.a002',
  correlationRef: 'correlation.vexlife-prod.vex-memory-lived-mac.a002',
  packageRef: 'package.vexlife-prod.vex-memory-lived-mac.a002',
  original: {
    originalIntent:
      'Continue Vex Memory Foundation refinement without losing stopped-turn evidence and without hiding the provider/runtime evidence needed to explain failures.',
    originalGoal:
      'Prove and form an exact local Mac test build containing conversation-loss protection, reach-pacing controls, and improved diagnostics.',
    userProtectedIntent:
      'Use Vex Relay locally without Local Workspace or GitHub Actions; keep Product source in GitHub and return one canonical evidence ZIP.',
    sourceRefs: [
      'github.pull.vexlife-prod.166',
      'github.pull.vexlife-prod.165',
      'github.pull.vexlife-prod.163',
      'github.pull.vexlife-prod.157',
      'github.pull.vexlife-prod.152',
      'source.vexrelay.current-contract/v1',
    ],
  },
  lane: {
    entryClass: 'SAME_LANE_ROLE_TRANSITION',
    scopeMode: 'STAGE_BOUNDED',
    laneRef: 'lane.vexlife-prod.vex-memory-foundation',
    parentRootRef: 'root.vexlife-prod.new-beginnings',
    workRef: 'work.vexlife-prod.vex-memory-foundation',
    claimRef: 'github.pull.vexlife-prod.166',
    returnRouteRef: 'github.pull.vexlife-prod.166',
    ownerRef: 'owner.vexlife-prod.vex-memory-foundation',
    rootHubRef: 'hub.vexlife-prod.new-beginnings',
  },
  role: {
    activeRole: 'CODER',
    roleRef: 'role.coder.vexlife-prod.vex-memory-lived-mac',
    instanceRef: 'instance.vexrelay.vexlife-prod.vex-memory-lived-mac.a002',
    occupancyRef: 'occupancy.vexrelay.vexlife-prod.vex-memory-lived-mac.a002',
    providerBindingRef: 'provider.vexlocalbridge.macos-victor.vex-memory-lived-mac.a002',
    threadRef: 'thread.vexlife-prod.vex-memory-foundation',
    authorityEnvelopeRef: 'authority-envelope.vexlife-prod.vex-memory-lived-mac.a002',
  },
  capabilities: {
    platformGitHubRead: 'AVAILABLE',
    platformGitHubWrite: 'UNAVAILABLE',
  },
  execution: {
    hostProfile: 'macos-victor',
    executionSurface: 'MAC_ZIP_RUN_COMMAND',
    singleUseNonce: 'nonce-vexlife-prod-vex-memory-lived-mac-a002-20261116',
    duplicateAttemptMarker: 'marker.vexlife-prod.vex-memory-lived-mac.a002',
    duplicateProtectionMode: 'OUTER_RELAY_BOUND',
    formedAt: now.toISOString(),
    expiresAtOrNull: expires.toISOString(),
  },
  workspace: {
    mode: 'RUN_ROOT',
    projectRef: 'project.vexlife-prod.vex-memory-foundation',
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
    authorityRef: 'authority.vexlife-prod.vex-memory-lived-mac.a002',
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
        'README.md',
        'build.gradle.kts',
        'docs',
        'localization',
        'packaging',
        'site',
        'src',
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
    sourceSetRef: 'source-set.vexlife-prod.vex-memory-lived-mac',
    sourceVersionRef: candidateHead,
    overlapEvidenceRef: 'github.pull.vexlife-prod.166',
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
    predicateRef: 'predicate.vexlife-prod.vex-memory-lived-mac.source-build',
    timeoutMs: 1200000,
  }],
  maxFiles: 10,
  stateRef: 'state.vexlife-prod.vex-memory-lived-mac',
  currentLifecycleState: 'SOURCE_READY',
  currentGoal:
    'Prove the exact integrated source and form one local unsigned Mac DMG for lived Vex Memory Foundation testing.',
  remainingGoals: [
    'Install/open the exact local DMG and continue lived Vex Memory Foundation refinement.',
    'Enable Enhanced Capture before the next failure-reproduction attempt.',
    'Return the next debug ZIP if the provider stream stops before [DONE].',
  ],
  onPass: {
    lifecycleState: 'SOURCE_READY',
    nextFunction: 'F08.EXECUTE_OR_HANDOFF',
    nextOwner: 'Victor',
    whyThisIsNext:
      'Exact source/build/DMG formation is complete; installing/opening the app and conducting the human conversation remain host/human lived effects.',
  },
  resultFilename: 'RETURN-VEXLIFE-MEMORY-LIVED-MAC-A002.zip',
};

const authorityEnvelope = {
  authoritySchemaRef: 'vextreme.effect-transaction/v1',
  semanticAdmissionRef: 'github.pull.vexlife-prod.166',
  transactionRef: 'transaction.vexlife-prod.vex-memory-lived-mac.a002',
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
  taskRef: 'task.vexlocalbridge.vexlife-prod.vex-memory-lived-mac.001',
  attemptRef: 'attempt.vexlocalbridge.vexlife-prod.vex-memory-lived-mac.a002',
  packageRef: 'package.vexlocalbridge.vexlife-prod.vex-memory-lived-mac.a002',
  requestRef: 'request.vexlocalbridge.vexlife-prod.vex-memory-lived-mac.a002',
  correlationRef: 'correlation.vexlocalbridge.vexlife-prod.vex-memory-lived-mac.a002',
  taskStorageKey: 'vexlife-prod-vex-memory-lived-mac',
  attemptStorageKey: 'attempt-a002',
  requestingRoleRef: 'Operations[VexLife-Prod]',
  requestingInstanceRef: 'instance.operations.vexlife-prod.vex-memory-lived-mac',
  parentOperationsRef: 'github.pull.vexlife-prod.166',
  returnRouteRef: 'github.pull.vexlife-prod.166',
  hostProfileRef: 'macos-victor',
  repositoryExecutionProfileRefOrNull: 'profile.repository.vextreme-sdk.macos-v1',
  repositoryRefOrNull: candidateRepo,
  baseRefOrNull: candidateBase,
  headRefOrNull: candidateHead,
  treeRefOrNull: candidateTree,
  branchRefOrNull: candidateBranch,
  claimRefOrNull: 'github.pull.vexlife-prod.166',
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
  singleUseNonce: 'nonce-vlb-vexlife-prod-vex-memory-lived-mac-a002-20261116',
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
  outputStem: 'VEXLIFE-MEMORY-LIVED-MAC-A002-RELAY',
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
