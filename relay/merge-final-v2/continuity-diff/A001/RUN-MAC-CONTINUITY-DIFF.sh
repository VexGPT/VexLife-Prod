#!/bin/bash
set -Eeuo pipefail

REPOSITORY='VexGPT/VexLife-Prod'
EXPECTED_MAIN='3120a7535f5c9095735c6880f6dd0119ec39a7a8'
CANDIDATE_HEAD='855fb3cbbbfeb7e92899649fa88bdcf7146d7919'
CANDIDATE_BRANCH='VXG-111826-chatgpt-merge-final-v2-a001'
ATTEMPT='V2-CONTINUITY-DIFF-A001'
RETURN_STEM='RETURN-VEXLIFE-MERGE-FINAL-V2-CONTINUITY-DIFF-A001'
TEST_FILTER='vexlife.harness.ContinuityPressureAdapterTest.projectionRunsTheWholeScenarioReportsWhatItSawAndAnalyzeAgrees'

TMPBASE="$(getconf DARWIN_USER_TEMP_DIR 2>/dev/null || printf '/tmp/')"
BOOTSTRAP="$TMPBASE/vexlife-continuity-diff-$ATTEMPT-$$"
REPO_ROOT="$BOOTSTRAP/repo"
EVIDENCE="$BOOTSTRAP/evidence"
DOWNLOADS="$HOME/Downloads"
STARTED_AT="$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
SUCCESS=0

mkdir -p "$EVIDENCE" "$DOWNLOADS"
exec > >(tee -a "$EVIDENCE/run.log") 2>&1

snapshot_scratch() {
  find "$TMPBASE" -maxdepth 1 -type d -name 'vexlife-prod-continuity-pressure-*' -print 2>/dev/null | LC_ALL=C sort || true
}

finalize() {
  rc=$?
  trap - EXIT HUP INT TERM
  set +e
  cat > "$EVIDENCE/summary.txt" <<EOF
attempt=$ATTEMPT
expectedMain=$EXPECTED_MAIN
candidateHead=$CANDIDATE_HEAD
startedAt=$STARTED_AT
success=$SUCCESS
EOF
  printf '%s\n' "$rc" > "$EVIDENCE/exit-code.txt"
  (
    cd "$EVIDENCE" || exit 0
    find . -type f ! -name 'SHA256SUMS.txt' -print | LC_ALL=C sort | while IFS= read -r f; do shasum -a 256 "$f"; done > SHA256SUMS.txt 2>/dev/null
  )
  tmp="$DOWNLOADS/$RETURN_STEM.zip"
  rm -f "$tmp"
  (cd "$BOOTSTRAP" && /usr/bin/zip -qry "$tmp" evidence)
  if [ -f "$tmp" ]; then
    digest="$(shasum -a 256 "$tmp" | awk '{print $1}')"
    final="$DOWNLOADS/$RETURN_STEM--sha256-$digest.zip"
    mv -f "$tmp" "$final"
    echo
    echo '============================================================'
    [ "$SUCCESS" = '1' ] && echo 'DIFF_PROBE_DISPOSITION=PASS' || echo 'DIFF_PROBE_DISPOSITION=SAFE_FAILURE'
    echo "RETURN_ZIP=$final"
    echo "RETURN_SHA256=$digest"
    echo '============================================================'
  fi
  rm -rf "$BOOTSTRAP"
  exit "$rc"
}
trap finalize EXIT
trap 'exit 130' HUP INT TERM

fail() { echo "FAIL:$*" >&2; return 1; }

[ "$(uname -s)" = 'Darwin' ] || fail 'MACOS_REQUIRED'
command -v git >/dev/null 2>&1 || fail 'GIT_REQUIRED'
command -v gh >/dev/null 2>&1 || fail 'GITHUB_CLI_REQUIRED'
gh auth status -h github.com >/dev/null 2>&1 || fail 'GITHUB_LOGIN_REQUIRED'

export GRADLE_OPTS='-Dorg.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m -Dorg.gradle.workers.max=1 -Dkotlin.compiler.execution.strategy=in-process'
export KOTLIN_DAEMON_JVMARGS='-Xmx4096m'

gh repo clone "$REPOSITORY" "$REPO_ROOT" -- --no-checkout --origin origin
git -C "$REPO_ROOT" fetch --no-tags --depth=256 origin "$CANDIDATE_BRANCH" main
OBSERVED_MAIN="$(git -C "$REPO_ROOT" rev-parse refs/remotes/origin/main)"
printf '%s\n' "$OBSERVED_MAIN" > "$EVIDENCE/observed-origin-main.txt"
[ "$OBSERVED_MAIN" = "$EXPECTED_MAIN" ] || fail "ORIGIN_MAIN_MOVED:$OBSERVED_MAIN"
BRANCH_HEAD="$(git -C "$REPO_ROOT" rev-parse refs/remotes/origin/$CANDIDATE_BRANCH)"
printf '%s\n' "$BRANCH_HEAD" > "$EVIDENCE/observed-candidate-head.txt"
[ "$BRANCH_HEAD" = "$CANDIDATE_HEAD" ] || fail "CANDIDATE_MOVED:$BRANCH_HEAD"

run_side() {
  label="$1"
  ref="$2"
  git -C "$REPO_ROOT" checkout --detach "$ref" >/dev/null
  git -C "$REPO_ROOT" clean -fdx >/dev/null
  (
    cd "$REPO_ROOT"
    for n in 1 2 3; do
      before="$EVIDENCE/$label-$n-scratch-before.txt"
      after="$EVIDENCE/$label-$n-scratch-after.txt"
      log="$EVIDENCE/$label-$n.log"
      snapshot_scratch > "$before"
      set +e
      ./gradlew --no-daemon --max-workers=1 jvmTest --tests "$TEST_FILTER" > "$log" 2>&1
      rc=$?
      set -e
      snapshot_scratch > "$after"
      printf '%s\n' "$rc" > "$EVIDENCE/$label-$n-exit.txt"
      echo "[$label run $n] exit=$rc"
      tail -n 80 "$log" || true
    done
  )
}

run_side main "$EXPECTED_MAIN"
run_side candidate "$CANDIDATE_HEAD"

main_codes="$(cat "$EVIDENCE"/main-*-exit.txt | tr '\n' ' ')"
candidate_codes="$(cat "$EVIDENCE"/candidate-*-exit.txt | tr '\n' ' ')"
printf 'main=%s\ncandidate=%s\n' "$main_codes" "$candidate_codes" > "$EVIDENCE/disposition.txt"

if grep -qv '^0$' "$EVIDENCE"/main-*-exit.txt; then
  echo 'CLASSIFICATION=CURRENT_MAIN_TEST_NOT_STABLE'
elif grep -qv '^0$' "$EVIDENCE"/candidate-*-exit.txt; then
  echo 'CLASSIFICATION=CANDIDATE_SPECIFIC_FAILURE'
else
  echo 'CLASSIFICATION=FOCUSED_TEST_STABLE_BOTH'
  SUCCESS=1
fi | tee "$EVIDENCE/classification.txt"

exit 0
