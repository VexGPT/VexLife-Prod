#!/bin/bash
set -u

REPO='VexGPT/VexLife-Prod'
PR='241'
CANDIDATE_HEAD='d408a84cc2ab6a79874bead461fc74d1bc2eaca9'
CANDIDATE_TREE='b058a82fd9e76ed0ef230eb27f2ed86aebe25a55'
CANDIDATE_BASE='d0cba51decfe9057ac9afc007fdeb37da213cc97'
PRECONDITION_TOOLING_HEAD='88eb2484abb0355a4b36bad6a46aa768d98ed0f4'
SCENARIO_REF='scenario.vexlife.continuity-pressure.depth-continuity.v1'
SCENARIO_PATH='tools/continuity-pressure/scenarios/depth-continuity-v1.json'
RUNTIME_HOME="${VEX_C20_RUNTIME_HOME:-$HOME/.vexlife}"
STAMP="$(/bin/date -u +%Y%m%dT%H%M%SZ)"
WORK_ROOT="$(/usr/bin/mktemp -d "$HOME/Downloads/.vexlife-pr241-exact-live-a001.XXXXXX")" || exit 2
RETURN_DIR="$WORK_ROOT/RETURN-VEXLIFE-C20-PR241-EXACT-LIVE-A001-$STAMP"
RETURN_ZIP="$HOME/Downloads/RETURN-VEXLIFE-C20-PR241-EXACT-LIVE-A001-$STAMP.zip"
SRC="$WORK_ROOT/VexLife-Prod"
RESULT_DIR="$WORK_ROOT/live-result"
mkdir -p "$RETURN_DIR"

STATE='FAILED'
STAGE='BOOTSTRAP'
DETAIL='not started'
FOCUSED_EXIT='NOT_RUN'
RUNTIME_PRECONDITION_EXIT='NOT_RUN'
LIVE_EXIT='NOT_RUN'
EXPERIMENT_STATUS='NOT_RUN'
OBSERVED_HEAD='UNOBSERVED'
OBSERVED_TREE='UNOBSERVED'

resolve_exec() {
  local x
  for x in "$@"; do
    if [ -n "$x" ] && [ -x "$x" ] && [ -f "$x" ]; then printf '%s' "$x"; return 0; fi
  done
  return 1
}

GIT_BIN="$(resolve_exec /usr/bin/git "$(command -v git 2>/dev/null || true)")" || true
GH_BIN="$(resolve_exec /opt/homebrew/bin/gh /usr/local/bin/gh "$(command -v gh 2>/dev/null || true)")" || true
NODE_BIN="$(resolve_exec /opt/homebrew/bin/node /usr/local/bin/node /usr/bin/node "$(command -v node 2>/dev/null || true)")" || true
SHASUM_BIN="$(resolve_exec /usr/bin/shasum "$(command -v shasum 2>/dev/null || true)")" || true
DITTO_BIN="$(resolve_exec /usr/bin/ditto)" || true
OPEN_BIN="$(resolve_exec /usr/bin/open)" || true

write_summary() {
  cat > "$RETURN_DIR/RETURN-SUMMARY.txt" <<SUMMARY
[VXG RealForever]
artifactClass=VEXLIFE_C20_PR241_EXACT_LIVE_RETURN
state=$STATE
stage=$STAGE
detail=$DETAIL
repository=$REPO
pr=$PR
candidateHead=$CANDIDATE_HEAD
candidateTree=$CANDIDATE_TREE
candidateBase=$CANDIDATE_BASE
observedHead=$OBSERVED_HEAD
observedTree=$OBSERVED_TREE
runtimeHome=$RUNTIME_HOME
focusedProofExit=$FOCUSED_EXIT
runtimePreconditionExit=$RUNTIME_PRECONDITION_EXIT
liveHarnessExit=$LIVE_EXIT
experimentStatus=$EXPERIMENT_STATUS
scenarioRef=$SCENARIO_REF
mode=quick
pressureStrategy=EFFECTIVE_SELECTION_BUDGET
effectiveInputTokenBudget=80000
payloadScale=16
sets=3
turnsPerSet=2
maximumLiveTurns=6
candidateMutationAllowed=false
canonicalConversationMutationAllowed=false
runtimeLifecycleEffectAllowed=true
runtimeStartAllowed=true
acquisitionAllowed=false
modelSwitchAllowed=false
purpose=exact-head rollover-successor-successor live qualification
SUMMARY
}

package_return() {
  write_summary
  {
    echo "formedAt=$(/bin/date -u +%Y-%m-%dT%H:%M:%SZ)"
    echo "uname=$(/usr/bin/uname -srm 2>/dev/null || true)"
    echo "swVers=$(/usr/bin/sw_vers -productVersion 2>/dev/null || true)"
    [ -n "$GIT_BIN" ] && echo "git=$($GIT_BIN --version 2>/dev/null || true)"
    [ -n "$GH_BIN" ] && echo "gh=$($GH_BIN --version 2>/dev/null | /usr/bin/head -n 1 || true)"
    [ -n "$NODE_BIN" ] && echo "node=$($NODE_BIN --version 2>/dev/null || true)"
    echo "java=$(/usr/bin/java -version 2>&1 | /usr/bin/head -n 1 || true)"
  } > "$RETURN_DIR/HOST-TOOLING.txt"

  if [ -d "$RESULT_DIR" ]; then
    mkdir -p "$RETURN_DIR/experiment"
    /usr/bin/ditto "$RESULT_DIR" "$RETURN_DIR/experiment" 2>/dev/null || /bin/cp -R "$RESULT_DIR/." "$RETURN_DIR/experiment/" 2>/dev/null || true
  fi

  if [ -d "$SRC/.git" ] && [ -n "$GIT_BIN" ]; then
    {
      echo "status:"
      "$GIT_BIN" -C "$SRC" status --porcelain=v1 -uall 2>/dev/null || true
      echo "head=$($GIT_BIN -C "$SRC" rev-parse HEAD 2>/dev/null || true)"
      echo "tree=$($GIT_BIN -C "$SRC" rev-parse 'HEAD^{tree}' 2>/dev/null || true)"
      echo "remote=$($GIT_BIN -C "$SRC" remote get-url origin 2>/dev/null || true)"
    } > "$RETURN_DIR/SOURCE-READBACK.txt"
  fi

  if [ -z "$DITTO_BIN" ]; then
    echo 'ditto unavailable; RETURN directory retained but ZIP could not be formed' >&2
    printf 'RETURN DIRECTORY: %s\n' "$RETURN_DIR"
    return 1
  fi

  /bin/rm -f "$RETURN_ZIP"
  "$DITTO_BIN" -c -k --norsrc --keepParent "$RETURN_DIR" "$RETURN_ZIP" || return 1
  if [ -n "$SHASUM_BIN" ]; then
    "$SHASUM_BIN" -a 256 "$RETURN_ZIP" > "$RETURN_ZIP.sha256"
  fi
  printf '\nRETURN ZIP: %s\n' "$RETURN_ZIP"
  if [ -n "$SHASUM_BIN" ]; then
    printf 'SHA-256: %s\n' "$(/usr/bin/cut -d ' ' -f 1 "$RETURN_ZIP.sha256")"
  fi
  [ -n "$OPEN_BIN" ] && "$OPEN_BIN" -R "$RETURN_ZIP" >/dev/null 2>&1 || true
  return 0
}

finish_and_exit() {
  package_return || true
  /bin/rm -rf "$WORK_ROOT" 2>/dev/null || true
  exit "$1"
}

fail_now() {
  STATE='FAILED'
  STAGE="$1"
  DETAIL="$2"
  finish_and_exit 1
}

[ -n "$GIT_BIN" ] || fail_now 'BOOTSTRAP' 'qualified git executable unavailable'
[ -n "$GH_BIN" ] || fail_now 'BOOTSTRAP' 'GitHub CLI unavailable'
[ -n "$NODE_BIN" ] || fail_now 'BOOTSTRAP' 'Node unavailable'
[ -n "$SHASUM_BIN" ] || fail_now 'BOOTSTRAP' 'shasum unavailable'

STAGE='GITHUB_AUTH'
"$GH_BIN" auth status --hostname github.com > "$RETURN_DIR/gh-auth.txt" 2>&1 ||
  fail_now "$STAGE" 'GitHub CLI is not authenticated for github.com'

STAGE='SOURCE_ACQUISITION'
"$GH_BIN" repo clone "$REPO" "$SRC" -- --no-checkout > "$RETURN_DIR/clone.log" 2>&1 ||
  fail_now "$STAGE" 'repository clone failed'

export GRADLE_OPTS="${GRADLE_OPTS:-} -Dorg.gradle.jvmargs=-Xmx4g -Dorg.gradle.workers.max=1 -Dorg.gradle.daemon=false -Dorg.gradle.offline=true"

STAGE='RUNTIME_HOME_PREFLIGHT'
if [ ! -d "$RUNTIME_HOME" ] || [ ! -f "$RUNTIME_HOME/config/home.json" ]; then
  STATE='HELD'
  DETAIL="canonical Vex Home unavailable at $RUNTIME_HOME; no runtime start/model call attempted"
  finish_and_exit 0
fi

STAGE='RUNTIME_PRECONDITION_TOOLING'
"$GIT_BIN" -C "$SRC" checkout --detach "$PRECONDITION_TOOLING_HEAD" >> "$RETURN_DIR/clone.log" 2>&1 ||
  fail_now "$STAGE" 'host-proof precondition tooling checkout failed'
[ -z "$($GIT_BIN -C "$SRC" status --porcelain=v1 -uall)" ] ||
  fail_now "$STAGE" 'host-proof tooling source is not clean'

STAGE='RUNTIME_PRECONDITION'
set +e
(
  cd "$SRC" &&
  ./gradlew --offline --no-daemon --max-workers=1 -Dorg.gradle.jvmargs=-Xmx4g \
    continuityHostRuntimePrecondition --args="--runtime-home $RUNTIME_HOME"
) > "$RETURN_DIR/runtime-precondition.log" 2>&1
RUNTIME_PRECONDITION_EXIT=$?
set -e 2>/dev/null || true
if [ "$RUNTIME_PRECONDITION_EXIT" -ne 0 ]; then
  STATE='HELD'
  DETAIL="Product runtime precondition could not realize the already-selected certified runtime without acquisition; exit $RUNTIME_PRECONDITION_EXIT"
  finish_and_exit 0
fi

STAGE='EXACT_CANDIDATE_CHECKOUT'
"$GIT_BIN" -C "$SRC" checkout --detach "$CANDIDATE_HEAD" >> "$RETURN_DIR/clone.log" 2>&1 ||
  fail_now "$STAGE" 'exact PR241 candidate checkout failed'
OBSERVED_HEAD="$($GIT_BIN -C "$SRC" rev-parse HEAD 2>/dev/null || true)"
OBSERVED_TREE="$($GIT_BIN -C "$SRC" rev-parse 'HEAD^{tree}' 2>/dev/null || true)"
[ "$OBSERVED_HEAD" = "$CANDIDATE_HEAD" ] || fail_now "$STAGE" "head mismatch: $OBSERVED_HEAD"
[ "$OBSERVED_TREE" = "$CANDIDATE_TREE" ] || fail_now "$STAGE" "tree mismatch: $OBSERVED_TREE"
[ -z "$($GIT_BIN -C "$SRC" status --porcelain=v1 -uall)" ] ||
  fail_now "$STAGE" 'acquired candidate source is not clean'

STAGE='FOCUSED_EXACT_HEAD_PROOF'
set +e
(
  cd "$SRC" &&
  ./gradlew --offline --no-daemon --max-workers=1 -Dorg.gradle.jvmargs=-Xmx4g \
    jvmTest \
      --tests 'vexlife.companion.DirectWorkingContinuityTest.sourceReturnSuccessorPromptPreservesTheSameWorkingContinuityCustody' \
      --tests 'vexlife.continuity.VexLifeWorkingContinuityOwnerTest.activeRolloverLineageAdvancesCurrentCursorEvenWhenPressureReturnsToNormal' \
      --tests 'vexlife.continuity.VexLifeWorkingContinuityOwnerTest.aTurnThatDidNotEnterAsSuccessorStillCommitsAfterARollover' \
      --tests 'vexlife.harness.ContinuityPressureAdapterTest.afterRolloverEveryLaterTurnStaysABoundedSuccessorAsPressureFalls'
) > "$RETURN_DIR/focused-proof.log" 2>&1
FOCUSED_EXIT=$?
set -e 2>/dev/null || true
if [ "$FOCUSED_EXIT" -ne 0 ]; then
  fail_now "$STAGE" "focused exact-head proof failed with exit $FOCUSED_EXIT"
fi

STAGE='LIVE_EXACT_HEAD_EXPERIMENT'
mkdir -p "$RESULT_DIR"
set +e
(
  cd "$SRC" &&
  "$NODE_BIN" tools/continuity-pressure/run.mjs \
    --stage quick \
    --scenario-ref "$SCENARIO_REF" \
    --scenario-file "$SCENARIO_PATH" \
    --result-dir "$RESULT_DIR" \
    --runtime-home "$RUNTIME_HOME" \
    --experiment-window \
    --pressure-strategy EFFECTIVE_SELECTION_BUDGET \
    --effective-input-token-budget 80000 \
    --payload-scale 16 \
    --sets 3 \
    --turns-per-set 2
) > "$RETURN_DIR/live-run.log" 2>&1
LIVE_EXIT=$?
set -e 2>/dev/null || true

if [ -f "$RESULT_DIR/result.json" ]; then
  EXPERIMENT_STATUS="$($NODE_BIN -e 'const fs=require("fs");const p=process.argv[1];try{const x=JSON.parse(fs.readFileSync(p,"utf8"));process.stdout.write(String(x.status||"UNKNOWN"));}catch{process.stdout.write("UNREADABLE");}' "$RESULT_DIR/result.json")"
elif [ -f "$RESULT_DIR/held.json" ]; then
  EXPERIMENT_STATUS='HELD'
else
  EXPERIMENT_STATUS='NO_RESULT'
fi

if [ "$LIVE_EXIT" -eq 0 ]; then
  STATE="$EXPERIMENT_STATUS"
  STAGE='COMPLETE'
  DETAIL='exact PR241 live harness returned normally; inspect experiment evidence'
elif [ "$LIVE_EXIT" -eq 3 ]; then
  STATE='HELD'
  DETAIL='exact PR241 live harness held at runtime admission; no lifecycle repair/start performed'
else
  STATE='FAILED'
  DETAIL="exact PR241 live harness exited $LIVE_EXIT; inspect experiment evidence"
fi

finish_and_exit 0
