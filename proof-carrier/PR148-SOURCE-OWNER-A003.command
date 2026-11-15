#!/bin/bash
set -euo pipefail

# [VXG RealForever]
# Proof-carrier only. This file is intentionally NOT part of PR #148's product branch.
# It acquires the exact current VexRelay/VexLocalBridge source, forms the canonical
# ExactHeadSourceOwnerProof package for the immutable PR #148 candidate below, executes it,
# and leaves Product/Home/model/runtime effects for the later lived-circuit stage.

CANDIDATE_REPO='VexGPT/VexLife-Prod'
CANDIDATE_BASE='035335a34d4220d6d3242c088a1a9667c29f135f'
CANDIDATE_HEAD='bec6021327ad333dd2db85abdf899e249a3d9c11'
CANDIDATE_TREE='bf56a066aa6b63d3cdc509a9e6641688951c4181'
CANDIDATE_BRANCH='VXG-111526-vexlife-clean-convergence-a003'

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
  printf '%s\n' 'PR148 SourceOwner: GitHub CLI unavailable.' >&2
  exit 12
}
GIT="$(resolve_exec /usr/bin/git)" || {
  printf '%s\n' 'PR148 SourceOwner: Git unavailable.' >&2
  exit 11
}
NODE="$(resolve_exec /opt/homebrew/bin/node /usr/local/bin/node /usr/bin/node)" || {
  printf '%s\n' 'PR148 SourceOwner: Node unavailable.' >&2
  exit 13
}

"$GH" auth status --hostname github.com >/dev/null 2>&1 || {
  printf '%s\n' 'PR148 SourceOwner: GitHub CLI authentication unavailable; no Relay task formed.' >&2
  exit 21
}

BOOT_ROOT="$(/usr/bin/mktemp -d "$HOME/Downloads/.vexlife-prod-pr148-source-owner-a003.XXXXXX")"
cleanup() {
  case "$BOOT_ROOT" in
    "$HOME/Downloads"/.vexlife-prod-pr148-source-owner-a003.*) /bin/rm -rf -- "$BOOT_ROOT" ;;
  esac
}
trap cleanup EXIT HUP INT TERM

SDK_ROOT="$BOOT_ROOT/Vextreme-SDK"

printf '%s\n' 'PR148 SourceOwner: acquiring exact VexRelay/VexLocalBridge source...'
"$GH" repo clone "$SDK_REPO" "$SDK_ROOT" -- --no-checkout >/dev/null
"$GIT" -C "$SDK_ROOT" checkout --detach "$SDK_HEAD" >/dev/null

OBSERVED_HEAD="$("$GIT" -C "$SDK_ROOT" rev-parse HEAD)"
OBSERVED_TREE="$("$GIT" -C "$SDK_ROOT" rev-parse 'HEAD^{tree}')"
[ "$OBSERVED_HEAD" = "$SDK_HEAD" ] || {
  printf '%s\n' 'PR148 SourceOwner: SDK head mismatch.' >&2
  exit 23
}
[ "$OBSERVED_TREE" = "$SDK_TREE" ] || {
  printf '%s\n' 'PR148 SourceOwner: SDK tree mismatch.' >&2
  exit 24
}
[ -z "$("$GIT" -C "$SDK_ROOT" status --porcelain=v1 -uall)" ] || {
  printf '%s\n' 'PR148 SourceOwner: acquired SDK source is dirty.' >&2
  exit 25
}

CONTRACT="$SDK_ROOT/docs/private-continuity/vexrelay/current-contract.json"
"$NODE" - "$CONTRACT" "$CONTRACT_REF" "$RUNTIME_VERSION" <<'NODECHECK'
const fs=require('node:fs');
const [file,ref,runtime]=process.argv.slice(2);
const value=JSON.parse(fs.readFileSync(file,'utf8'));
if(value.contractRef!==ref) {
  console.error(`PR148 SourceOwner: Relay contractRef mismatch: ${value.contractRef}`);
  process.exit(27);
}
if(value.currentRuntimeVersion!==runtime) {
  console.error(`PR148 SourceOwner: Relay runtime mismatch: ${value.currentRuntimeVersion}`);
  process.exit(28);
}
NODECHECK

printf '%s\n' 'PR148 SourceOwner: exact Relay source bound; forming canonical package...'

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
  path.join(
    sdkRoot,
    'lib/vexlocalbridge/vexrelay-task-archive-carrier-formation.mjs',
  ),
).href;
const { formVexRelayTaskArchiveCarrier } = await import(formationUrl);

if (typeof formVexRelayTaskArchiveCarrier !== 'function') {
  throw new Error('CURRENT_VEXRELAY_FORMATION_EXPORT_UNAVAILABLE');
}

const now = new Date();
const expires = new Date(now.getTime() + 4 * 60 * 60 * 1000);
const proofShell = [
  'set -euo pipefail',
  'J21="$(/usr/libexec/java_home -v 21 2>/dev/null)" || { printf "%s\\n" "VexLife-Prod PR148 SourceOwner: JDK 21 is unavailable." >&2; exit 21; }',
  '[ -n "$J21" ] && [ -x "$J21/bin/java" ] || { printf "%s\\n" "VexLife-Prod PR148 SourceOwner: resolved JDK 21 is invalid." >&2; exit 22; }',
  'export JAVA_HOME="$J21"',
  'export PATH="$JAVA_HOME/bin:$PATH"',
  '"$JAVA_HOME/bin/java" -version',
  './gradlew --no-daemon -Dorg.gradle.java.home="$J21" --version',
  './gradlew --no-daemon -Dorg.gradle.java.home="$J21" jvmTest compileKotlinJs compileTestKotlinJs checkVexLifeStrings assembleVexLifeProdServiceImage',
  '[ -z "$(/usr/bin/git status --porcelain=v1 -uall)" ] || { printf "%s\\n" "VexLife-Prod PR148 SourceOwner: proof left the candidate worktree dirty." >&2; /usr/bin/git status --porcelain=v1 -uall >&2; exit 31; }',
].join('; ');

const seed = {
  schemaVersion: 'vex.relay.task/v2',
  continuity: '[VXG RealForever]',
  taskRef: 'task.vexlife-prod.pr148.clean-convergence.source-owner.001',
  attemptRef: 'attempt.vexlife-prod.pr148.clean-convergence.source-owner.a003',
  requestRef: 'request.vexlife-prod.pr148.clean-convergence.source-owner.a003',
  correlationRef: 'correlation.vexlife-prod.pr148.clean-convergence.source-owner.a003',
  packageRef: 'package.vexlife-prod.pr148.clean-convergence.source-owner.a003',
  original: {
    originalIntent: 'Stabilize VexLife-Prod without rewriting lived-good relationships or importing held experimental ancestry.',
    originalGoal: 'Prove the exact repaired PR #148 source/build/private-image candidate before any native Product runtime effect.',
    userProtectedIntent: 'Preserve multi-model selection, canonical turn/capability sequencing, live reasoning, human scroll ownership, session-scoped raw reasoning, and truthful next-loop evidence.',
    sourceRefs: [
      'github.pull.vexlife-prod.148',
      'source.vexrelay.current-contract/v1',
    ],
  },
  lane: {
    entryClass: 'SAME_LANE_ROLE_TRANSITION',
    scopeMode: 'STAGE_BOUNDED',
    laneRef: 'lane.vexlife-prod.clean-convergence',
    parentRootRef: 'root.vexlife-prod.new-beginnings',
    workRef: 'work.vexlife-prod.clean-convergence',
    claimRef: 'github.pull.vexlife-prod.148',
    returnRouteRef: 'github.pull.vexlife-prod.148',
    ownerRef: 'owner.vexlife-prod.clean-convergence',
    rootHubRef: 'hub.vexlife-prod.new-beginnings',
  },
  role: {
    activeRole: 'CODER',
    roleRef: 'role.coder.vexlife-prod.clean-convergence.source-owner',
    instanceRef: 'instance.vexrelay.vexlife-prod.pr148.source-owner.a003',
    occupancyRef: 'occupancy.vexrelay.vexlife-prod.pr148.source-owner.a003',
    providerBindingRef: 'provider.vexlocalbridge.macos-victor.pr148.source-owner.a003',
    threadRef: 'thread.vexlife-prod.pr148.source-owner',
    authorityEnvelopeRef: 'authority-envelope.vexlife-prod.pr148.source-owner.a003',
  },
  capabilities: {
    platformGitHubRead: 'AVAILABLE',
    platformGitHubWrite: 'UNAVAILABLE',
  },
  execution: {
    hostProfile: 'macos-victor',
    executionSurface: 'MAC_ZIP_RUN_COMMAND',
    singleUseNonce: 'nonce-vexlife-prod-pr148-source-owner-a003-20261115',
    duplicateAttemptMarker: 'marker.vexlife-prod.pr148.source-owner.a003',
    duplicateProtectionMode: 'OUTER_RELAY_BOUND',
    formedAt: now.toISOString(),
    expiresAtOrNull: expires.toISOString(),
  },
  workspace: {
    mode: 'RUN_ROOT',
    projectRef: 'project.vexlife-prod.clean-convergence',
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
    authorityRef: 'authority.vexlife-prod.pr148.source-owner.a003',
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
      allowedPaths: ['docs','src','localization','packaging'],
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
    sourceSetRef: 'source-set.vexlife-prod.pr148.clean-convergence',
    sourceVersionRef: candidateHead,
    overlapEvidenceRef: 'github.pull.vexlife-prod.148',
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
  proofCommands: [
    {
      command: '/bin/bash',
      args: ['-lc', proofShell],
      proofClass: 'TEST',
      candidateEvidence: true,
      predicateClass: 'SOURCE_OWNER_BEHAVIOR',
      predicateRef: 'predicate.vexlife-prod.pr148.source-build-and-image',
      timeoutMs: 600000,
    },

  ],
  maxFiles: 10,
  stateRef: 'state.vexlife-prod.pr148.source-owner',
  currentLifecycleState: 'SOURCE_READY',
  currentGoal: 'Prove the exact repaired PR #148 source/build/private-image candidate only.',
  remainingGoals: [
    'Run native Setup/Product lived-circuit proof on the exact proved candidate.',
    'Prove live reasoning and human scroll interaction in the real Product browser.',
    'Prove capability request/return/continuation and a fresh second Send.',
  ],
  onPass: {
    lifecycleState: 'SOURCE_READY',
    nextFunction: 'F08.EXECUTE_OR_HANDOFF',
    nextOwner: 'Operations',
    whyThisIsNext: 'Exact source/build proof is complete; native Setup/Product behavior remains a separate host-semantic proof.',
  },
  resultFilename: 'RETURN-VEXLIFE-PROD-PR148-SOURCE-OWNER-A003.zip',
};

const authorityEnvelope = {
  authoritySchemaRef: 'vextreme.effect-transaction/v1',
  semanticAdmissionRef: 'github.pull.vexlife-prod.148',
  transactionRef: 'transaction.vexlife-prod.pr148.source-owner.a003',
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
  taskRef: 'task.vexlocalbridge.vexlife-prod.pr148.source-owner.001',
  attemptRef: 'attempt.vexlocalbridge.vexlife-prod.pr148.source-owner.a003',
  packageRef: 'package.vexlocalbridge.vexlife-prod.pr148.source-owner.a003',
  requestRef: 'request.vexlocalbridge.vexlife-prod.pr148.source-owner.a003',
  correlationRef: 'correlation.vexlocalbridge.vexlife-prod.pr148.source-owner.a003',
  taskStorageKey: 'vexlife-prod-pr148-source-owner',
  attemptStorageKey: 'attempt-a003',
  requestingRoleRef: 'Operations[VexLife-Prod]',
  requestingInstanceRef: 'instance.operations.vexlife-prod.pr148.source-owner',
  parentOperationsRef: 'github.pull.vexlife-prod.148',
  returnRouteRef: 'github.pull.vexlife-prod.148',
  hostProfileRef: 'macos-victor',
  repositoryExecutionProfileRefOrNull: 'profile.repository.vextreme-sdk.macos-v1',
  repositoryRefOrNull: candidateRepo,
  baseRefOrNull: candidateBase,
  headRefOrNull: candidateHead,
  treeRefOrNull: candidateTree,
  branchRefOrNull: candidateBranch,
  claimRefOrNull: 'github.pull.vexlife-prod.148',
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
  singleUseNonce: 'nonce-vlb-vexlife-prod-pr148-source-owner-a003-20261115',
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
  outputDirectory: path.join(os.homedir(), 'Downloads'),
  outputStem: 'VEXLIFE-PROD-PR148-SOURCE-OWNER-A003-RELAY',
  ttlMs: 4 * 60 * 60 * 1000,
});

console.log(`Canonical Relay package: ${formed.outer.finalZip}`);
console.log(`Canonical Relay package SHA-256: ${formed.outer.digest}`);
console.log(`Inner task: ${formed.inner.manifest.taskRef}`);
console.log(`Inner archive SHA-256: ${formed.inner.sha256}`);

const runner = path.join(
  sdkRoot,
  'lib/vexlocalbridge/templates/RUN-VEX-TASK.command',
);
const child = spawnSync('/bin/bash', [runner, formed.outer.finalZip], {
  stdio: 'inherit',
  env: process.env,
});
if (child.error) throw child.error;
process.exit(child.status ?? 2);
NODEFORM
