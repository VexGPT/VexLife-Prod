#!/bin/bash
set -euo pipefail

CONTINUITY='[VXG RealForever]'
SDK_REPOSITORY='vgong24/Vextreme-SDK'
SDK_HEAD='8543c7d755faba04d4c4019c02e84f992667918a'
SDK_TREE='6376f68279f0750ee6d3868b8472368199763064'
PRODUCT_REPOSITORY='VexGPT/VexLife-Prod'
PRODUCT_BASE='4a325b928668bca17d43f5f38eb2099334066a4b'
PRODUCT_HEAD='53d94563d27185c2ece7e9afcf0e8a1747ece776'
PRODUCT_BRANCH='VXG-111926-chatgpt-reasoning-continuation-a001'
PRODUCT_BASE_BRANCH='VXG-111926-chatgpt-labs-native-tools-convergence-a001'
ATTEMPT='A014'

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

BASH_BIN="$(resolve_exec /bin/bash)" || { echo 'A014: qualified Bash unavailable.' >&2; exit 10; }
GIT_BIN="$(resolve_exec /usr/bin/git)" || { echo 'A014: qualified Git unavailable.' >&2; exit 11; }
GH_BIN="$(resolve_exec /opt/homebrew/bin/gh /usr/local/bin/gh)" || { echo 'A014: qualified GitHub CLI unavailable.' >&2; exit 12; }
NODE_BIN="$(resolve_exec /opt/homebrew/bin/node /usr/local/bin/node /usr/bin/node)" || { echo 'A014: qualified Node unavailable.' >&2; exit 13; }
MKTEMP_BIN="$(resolve_exec /usr/bin/mktemp)" || { echo 'A014: mktemp unavailable.' >&2; exit 14; }
RM_BIN="$(resolve_exec /bin/rm /usr/bin/rm)" || { echo 'A014: rm unavailable.' >&2; exit 15; }

ARCH="$(/usr/bin/uname -m)"
[ "$ARCH" = 'arm64' ] || { echo "A014: expected Apple-silicon arm64 host, observed $ARCH." >&2; exit 16; }

"$GH_BIN" auth status --hostname github.com >/dev/null 2>&1 || {
  echo 'A014: GitHub authentication is required before this proof can run.' >&2
  exit 17
}

ROOT="$("$MKTEMP_BIN" -d "$HOME/Downloads/.vexlife-pr403-reasoning-source-a014.XXXXXX")"
SDK_ROOT="$ROOT/Vextreme-SDK"
FORMATION_RESULT="$ROOT/formation-result.json"
FORMATION_SCRIPT="$ROOT/form-pr403-a014.mjs"

cleanup() {
  case "$ROOT" in
    "$HOME"/Downloads/.vexlife-pr403-reasoning-source-a014.*) "$RM_BIN" -rf -- "$ROOT" ;;
    *) return 1 ;;
  esac
}
trap cleanup EXIT HUP INT TERM

echo 'A014: deriving exact immutable Product tree from pinned commit...'
PRODUCT_TREE="$("$GH_BIN" api "repos/$PRODUCT_REPOSITORY/git/commits/$PRODUCT_HEAD" --jq '.tree.sha')"
"$NODE_BIN" -e 'if(!/^[0-9a-f]{40}$/.test(process.argv[1])) process.exit(1)' "$PRODUCT_TREE" || {
  echo "A014: invalid Product tree returned for exact head: $PRODUCT_TREE" >&2
  exit 18
}

echo 'A014: acquiring exact current VexRelay/VexLocalBridge source...'
"$GH_BIN" repo clone "$SDK_REPOSITORY" "$SDK_ROOT" -- --no-checkout >/dev/null
"$GIT_BIN" -C "$SDK_ROOT" checkout --detach "$SDK_HEAD" >/dev/null

OBSERVED_HEAD="$("$GIT_BIN" -C "$SDK_ROOT" rev-parse HEAD)"
OBSERVED_TREE="$("$GIT_BIN" -C "$SDK_ROOT" rev-parse 'HEAD^{tree}')"
[ "$OBSERVED_HEAD" = "$SDK_HEAD" ] || { echo 'A014: SDK head mismatch.' >&2; exit 19; }
[ "$OBSERVED_TREE" = "$SDK_TREE" ] || { echo 'A014: SDK tree mismatch.' >&2; exit 20; }
[ -z "$("$GIT_BIN" -C "$SDK_ROOT" status --porcelain=v1 -uall)" ] || { echo 'A014: acquired SDK source is dirty.' >&2; exit 21; }

A014_SDK_ROOT="$SDK_ROOT" "$NODE_BIN" -e '
const fs=require("node:fs");
const path=require("node:path");
const root=process.env.A014_SDK_ROOT;
const contract=JSON.parse(fs.readFileSync(path.join(root,"docs/private-continuity/vexrelay/current-contract.json"),"utf8"));
if(contract.contractRef!=="source.vexrelay.current-contract/v1") process.exit(22);
const carrier=contract.acceptedAdapters.find(x=>x.adapterRef==="adapter.vexlocalbridge.native-vexrelay-task-archive/v1");
if(!carrier || carrier.state!=="TERMINAL_ACCEPTED_CURRENT") process.exit(23);
if(!contract.acceptedRecipeFamilies.includes("ExactHeadSourceOwnerProof")) process.exit(24);
'

cat > "$FORMATION_SCRIPT" <<'NODE_A014'
import fs from 'node:fs';
import path from 'node:path';
import { pathToFileURL } from 'node:url';

const SDK_ROOT = process.env.A014_SDK_ROOT;
const OUT = process.env.A014_FORMATION_RESULT;
const TREE = process.env.A014_PRODUCT_TREE;
const DOWNLOADS = path.join(process.env.HOME, 'Downloads');

const { formVexRelayTaskArchiveCarrier } =
  await import(pathToFileURL(path.join(
    SDK_ROOT,
    'lib/vexlocalbridge/vexrelay-task-archive-carrier-formation.mjs',
  )).href);

const BASE = '4a325b928668bca17d43f5f38eb2099334066a4b';
const HEAD = '53d94563d27185c2ece7e9afcf0e8a1747ece776';
const BRANCH = 'VXG-111926-chatgpt-reasoning-continuation-a001';
const BASE_BRANCH = 'VXG-111926-chatgpt-labs-native-tools-convergence-a001';

const CHANGED_PATHS = [
  "docs/foundation/CAPABILITY-ATLAS-SELF-FRAME.md",
  "docs/foundation/DIRECT-PERSONAL-COMPANION.md",
  "docs/foundation/FOUNDATIONAL-FIELD.md",
  "docs/foundation/INDEX.md",
  "docs/foundation/VEX-CONTEXT-PROFILES.md",
  "docs/foundation/VEX-LABS-NATIVE-TEST.md",
  "docs/foundation/WORKING-CONTINUITY-ROLLOVER.md",
  "src/commonMain/kotlin/vexlife/companion/CompanionProviderMessage.kt",
  "src/commonMain/kotlin/vexlife/context/VexLifeContextProfile.kt",
  "src/commonMain/kotlin/vexlife/memory/VexLifeMemoryContract.kt",
  "src/commonMain/kotlin/vexlife/model/heretic/HereticCompanionRepresentation.kt",
  "src/commonMain/kotlin/vexlife/service/VexLifeProdServiceContract.kt",
  "src/commonMain/kotlin/vexlife/web/VexLifeWebP1ExperienceMarkup.kt",
  "src/commonMain/kotlin/vexlife/web/VexLifeWebP1RuntimeArtifacts.kt",
  "src/commonTest/kotlin/vexlife/memory/VexLifeMemoryContractTest.kt",
  "src/commonTest/kotlin/vexlife/model/heretic/HereticModelCatalogTest.kt",
  "src/commonTest/kotlin/vexlife/web/VexLifeWebP1RuntimeArtifactsTest.kt",
  "src/jvmMain/kotlin/vexlife/capability/VexLifeCapabilityBridge.kt",
  "src/jvmMain/kotlin/vexlife/companion/DirectCompanionConnection.kt",
  "src/jvmMain/kotlin/vexlife/companion/DirectCompanionTurn.kt",
  "src/jvmMain/kotlin/vexlife/companion/DirectConversationSourceReturn.kt",
  "src/jvmMain/kotlin/vexlife/companion/DirectPromptContext.kt",
  "src/jvmMain/kotlin/vexlife/companion/DirectTurnContextStore.kt",
  "src/jvmMain/kotlin/vexlife/companion/ProviderCapabilityTools.kt",
  "src/jvmMain/kotlin/vexlife/context/VexLifeContextProfileStore.kt",
  "src/jvmMain/kotlin/vexlife/foundational/VexLifeFoundationalField.kt",
  "src/jvmMain/kotlin/vexlife/labs/VexLabsNativeTest.kt",
  "src/jvmMain/kotlin/vexlife/memory/VexLifeCurrentMemoryReader.kt",
  "src/jvmMain/kotlin/vexlife/model/heretic/runtime/HereticExecutionTopology.kt",
  "src/jvmMain/kotlin/vexlife/service/VexLifeProdLocalService.kt",
  "src/jvmMain/resources/foundational/vex-foundational-field.json",
  "src/jvmMain/resources/foundational/vex-foundational-field.revisions.json",
  "src/jvmTest/kotlin/vexlife/capability/VexLifeCapabilityReachTest.kt",
  "src/jvmTest/kotlin/vexlife/capability/VexLifeCapabilityRevisitTest.kt",
  "src/jvmTest/kotlin/vexlife/companion/DirectCompanionCapabilityTurnTest.kt",
  "src/jvmTest/kotlin/vexlife/companion/DirectCompanionConnectionTest.kt",
  "src/jvmTest/kotlin/vexlife/companion/DirectCompanionTurnFoundationalFieldTest.kt",
  "src/jvmTest/kotlin/vexlife/companion/DirectCompanionTurnTest.kt",
  "src/jvmTest/kotlin/vexlife/companion/DirectConversationSourceReturnTest.kt",
  "src/jvmTest/kotlin/vexlife/companion/DirectPromptContextTest.kt",
  "src/jvmTest/kotlin/vexlife/companion/DirectTurnContextStoreTest.kt",
  "src/jvmTest/kotlin/vexlife/context/VexLifeContextProfileStoreTest.kt",
  "src/jvmTest/kotlin/vexlife/foundational/VexLifeFoundationalFieldTest.kt",
  "src/jvmTest/kotlin/vexlife/labs/VexLabsNativeTestTest.kt",
  "src/jvmTest/kotlin/vexlife/memory/VexLifeMemoryContextFixture.kt",
  "src/jvmTest/kotlin/vexlife/model/heretic/HuggingFaceModelInspectionTest.kt",
  "src/jvmTest/kotlin/vexlife/model/heretic/runtime/HereticExecutionTopologyTest.kt",
  "src/jvmTest/kotlin/vexlife/service/VexLifeProdLocalServiceTest.kt",
];

if (!/^[0-9a-f]{40}$/.test(TREE)) throw new Error('A014_PRODUCT_TREE_INVALID');

const now = new Date();
const expires = new Date(now.getTime() + 60 * 60 * 1000);

const seed = {
  schemaVersion: 'vex.relay.task/v2',
  continuity: '[VXG RealForever]',
  taskRef: 'task.vexlife-prod.pr403.reasoning-continuation.source-owner',
  attemptRef: 'attempt.vexlife-prod.pr403.reasoning-continuation.source-owner.a014.20261120',
  requestRef: 'request.vexlife-prod.pr403.reasoning-continuation.source-owner.a014.20261120',
  correlationRef: 'correlation.vexlife-prod.pr403.reasoning-continuation.source-owner.a014.20261120',
  packageRef: 'package.vexlife-prod.pr403.reasoning-continuation.source-owner.a014.20261120',
  original: {
    originalIntent:
      'Preserve a reasoning-field model completed turn as answer plus response-bound reasoning continuation so later selected provider history and same-turn tool/source stages can reason forward instead of reconstructing work already performed.',
    originalGoal:
      'Prove the exact PR #403 source candidate compiles and its source regressions enforce response-bound reasoning continuation without rewriting canonical Conversation or Memory.',
    userProtectedIntent:
      'Reasoning continuity is an effectiveness requirement: Vex should not forget its own completed reasoning whenever the next provider stage or next selected turn begins.',
    sourceRefs: [
      'github.pr.vexlife-prod.403',
      'git.vexlife-prod.53d94563d27185c2ece7e9afcf0e8a1747ece776',
      'git.vexlife-prod.4a325b928668bca17d43f5f38eb2099334066a4b',
      'source.vexrelay.current-contract/v1',
    ],
  },
  lane: {
    entryClass: 'SAME_LANE_SUCCESSOR',
    scopeMode: 'STAGE_BOUNDED',
    laneRef: 'lane.vexlife-prod.reasoning-continuation',
    parentRootRef: 'github.pr.vexlife-prod.393',
    workRef: 'work.vexlife-prod.reasoning-continuation.20261120',
    claimRef: 'claim.vexlife-prod.pr403.reasoning-continuation-source-owner',
    returnRouteRef: 'github.pr.vexlife-prod.403',
    ownerRef: 'github.pr.vexlife-prod.403',
    rootHubRef: 'github.pr.vexlife-prod.393',
  },
  role: {
    activeRole: 'CODER',
    roleRef: 'role.coder.vexlife-prod.pr403.a014',
    instanceRef: 'instance.vexlife-prod.pr403.a014',
    occupancyRef: 'occupancy.vexlife-prod.pr403.a014',
    providerBindingRef: 'provider.macos-victor.vexlife-prod.pr403.a014',
    threadRef: 'thread.local-vex-operations.pr403.a014',
    authorityEnvelopeRef: 'authority-envelope.vexlife-prod.pr403.a014',
  },
  capabilities: {
    platformGitHubRead: 'AVAILABLE',
    platformGitHubWrite: 'UNAVAILABLE',
  },
  execution: {
    hostProfile: 'macos-victor',
    executionSurface: 'MAC_ZIP_RUN_COMMAND',
    singleUseNonce: 'nonce.vexlife-prod.pr403.a014.20261120',
    duplicateAttemptMarker: 'duplicate.vexlife-prod.pr403.a014.20261120',
    duplicateProtectionMode: 'OUTER_RELAY_BOUND',
    formedAt: now.toISOString(),
    expiresAtOrNull: expires.toISOString(),
  },
  workspace: {
    mode: 'RUN_ROOT',
    projectRef: 'project.vexlife-prod.pr403.reasoning-continuation-source-owner',
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
    authorityRef: 'authority.vexlife-prod.pr403.reasoning-continuation.source-owner.a014',
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
    sourceSetRef: 'source-set.vexlife-prod.pr403.reasoning-continuation',
    sourceVersionRef: 'git.' + HEAD,
    overlapEvidenceRef: 'github.pr.vexlife-prod.403',
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
    predicateRef: 'proof.pr403.reasoning-continuation.whole-source',
    timeoutMs: 900000,
  }],
  maxFiles: 10,
  stateRef: 'state.vexlife-prod.pr403.reasoning-continuation.source-owner.a014',
  currentLifecycleState: 'SOURCE_READY',
  currentGoal:
    'Prove the current PR #403 candidate after A014 exposed one upstream Memory provider-representation ownership defect through 18 clustered prompt/rollover failures. VexLifeMemoryContract now owns the exact Daily/Living-Journal provider packet and wording once; Memory reader, Prompt Context verification, and exact-byte fixtures reuse that representation. Preserve reasoning continuation, configurable 10-read working blocks x up to three, Foundation v0.6.3 adaptive judgment, prompt topology/provenance, and rollover distinct from Memory formation.',
  remainingGoals: [
    'Run fresh Independent Assurance on the unchanged exact candidate after SourceOwner PASS.',
    'Resolve the capability-enabled disposable test boundary so lived probes exercise the real perceive -> reach -> return -> interpret -> reach-again loop without mutating canonical Home.',
    'Run an isolated lived reasoning-continuation OFF/ON comparison on the same qualified model/runtime and scratch Home, preserving exact prompt/source/provenance evidence.',
    'Compare semantic recognition, source/tool choice, owner-return integration, hallucination/absence behavior, promptTokens, cachedPromptTokens, prefillTokens and latency before considering any adaptive runtime reasoning-effort policy.',
    'Use the result to decide Product convergence; do not infer merge, automatic reasoning-depth control, or release from this source proof.',
  ],
  onPass: {
    lifecycleState: 'ASSURANCE_PENDING',
    nextFunction: 'F13.RUN_FRESH_ASSURANCE',
    nextOwner: 'IndependentAssurance',
    whyThisIsNext:
      'Executable source and the exact 48-path membrane are proven after removing the duplicated Memory provider-byte definition exposed by A014; prompt topology, Foundation v0.6.3, reasoning continuation and multi-block reach then require fresh semantic review before any lived model comparison.',
  },
  resultFilename:
    'RETURN-VEXLIFE-PR403-REASONING-CONTINUATION-SOURCE-OWNER-A014.zip',
};

const authorityEnvelope = {
  authoritySchemaRef: 'vextreme.effect-transaction/v1',
  semanticAdmissionRef: 'github.pr.vexlife-prod.403',
  transactionRef: 'transaction.vexlife-prod.pr403.reasoning-continuation.source-owner.a014',
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
  taskRef: 'task.vexlife-prod.pr403.reasoning-continuation.carrier',
  attemptRef: 'attempt.vexlife-prod.pr403.reasoning-continuation.carrier.a014.20261120',
  packageRef: 'package.vexlife-prod.pr403.reasoning-continuation.carrier.a014.20261120',
  requestRef: 'request.vexlife-prod.pr403.reasoning-continuation.carrier.a014.20261120',
  correlationRef: 'correlation.vexlife-prod.pr403.reasoning-continuation.carrier.a014.20261120',
  taskStorageKey: 'task-vexlife-prod-pr403-reasoning-continuation-carrier',
  attemptStorageKey: 'attempt-a014',
  requestingRoleRef: 'Operations[PR403-A014]',
  requestingInstanceRef: 'instance.local-vex-operations.pr403.a014',
  parentOperationsRef: 'github.pr.vexlife-prod.403',
  returnRouteRef: 'github.pr.vexlife-prod.403',
  hostProfileRef: 'macos-victor',
  repositoryExecutionProfileRefOrNull:
    'profile.repository.vextreme-sdk.macos-v1',
  repositoryRefOrNull: 'VexGPT/VexLife-Prod',
  baseRefOrNull: BASE,
  headRefOrNull: HEAD,
  treeRefOrNull: TREE,
  branchRefOrNull: BRANCH,
  claimRefOrNull: 'claim.vexlife-prod.pr403.reasoning-continuation-source-owner',
  exactPathRefs: CHANGED_PATHS,
  authorityBinding: {
    authoritySchemaRef: 'vextreme.effect-transaction/v1',
    authorityEnvelopePath: 'authority/effect.json',
    authorityEnvelopeSha256: '0'.repeat(64),
    semanticAdmissionRef: 'github.pr.vexlife-prod.403',
    transactionRef: 'transaction.vexlife-prod.pr403.reasoning-continuation.source-owner.a014',
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
  singleUseNonce: 'nonce.vexlife-prod.pr403.reasoning-continuation.carrier.a014.20261120',
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
    'VEXLIFE-PR403-REASONING-CONTINUATION-SOURCE-OWNER-A014',
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
NODE_A014

echo 'A014: forming exact task/carrier...'
A014_SDK_ROOT="$SDK_ROOT" A014_FORMATION_RESULT="$FORMATION_RESULT" A014_PRODUCT_TREE="$PRODUCT_TREE" "$NODE_BIN" "$FORMATION_SCRIPT"

PACKAGE="$("$NODE_BIN" -e 'const fs=require("node:fs");const x=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));process.stdout.write(x.outerZip)' "$FORMATION_RESULT")"
EXPECTED="$("$NODE_BIN" -e 'const fs=require("node:fs");const x=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));process.stdout.write(x.outerSha256)' "$FORMATION_RESULT")"

[ -f "$PACKAGE" ] || { echo 'A014: formed package is missing.' >&2; exit 25; }
OBSERVED="$(/usr/bin/shasum -a 256 "$PACKAGE" | /usr/bin/cut -d ' ' -f 1)"
[ "$OBSERVED" = "$EXPECTED" ] || { echo 'A014: formed package digest mismatch.' >&2; exit 26; }

echo "A014: Product head $PRODUCT_HEAD"
echo "A014: Product tree $PRODUCT_TREE"
echo "A014: formed exact package $PACKAGE"
echo "A014: package sha256 $EXPECTED"
echo 'A014: entering current native VexRelay/VexLocalBridge carrier...'

set +e
VEX_EXPECTED_PACKAGE_SHA256="$EXPECTED" "$BASH_BIN" "$SDK_ROOT/lib/vexlocalbridge/templates/RUN-VEX-TASK.command" "$PACKAGE"
STATUS=$?
set -e

echo "A014: carrier exit $STATUS"
echo 'A014: if a canonical RETURN was formed, upload the exact RETURN ZIP revealed by VexLocalBridge.'
exit "$STATUS"
