#!/bin/bash
set -euo pipefail

REPOSITORY='VexGPT/VexLife-Prod'
HARNESS_HEAD='26c236020f5bae28901f9b9e95081ae71163a628'
HARNESS_TREE='a4ca0f1658eef23d536e095f9e8f8031b048794c'

PAYLOAD_PATH='tools/vexrelay/multivex-relationship-pressure/payload-C2-A004.mjs'
PAYLOAD_BLOB='5b785a4ffd2c8240e6185ce2c65881495fb525fd'
FORMATION_PATH='tools/vexrelay/multivex-relationship-pressure/formation-C2-A004.mjs'
FORMATION_BLOB='7c0d56ebef2ef121cc36d3de7284fdfb0f6de510'
RUNNER_PATH='tools/vexrelay/multivex-relationship-pressure/RUN-C2-A004.command'
RUNNER_BLOB='11a5dd3fa75a2b2c246350442f40b4b771d6a392'

TREATMENT_PATH='tools/continuity-pressure/treatments/c2-borrowed-standing-first-visible-request-v1.txt'
TREATMENT_BLOB='0656f684169cf5a62a0b6f472be21097b42dc0ec'
SCENARIO_PATH='tools/continuity-pressure/scenario-profiles/borrowed-standing-status-artifact-v1.json'
SCENARIO_BLOB='1fec968f96fff59851cbdfee5ed75fd9da411394'
PROFILE_TEST_PATH='src/jvmTest/kotlin/vexlife/harness/DynamicCounterpartProfilesTest.kt'
PROFILE_TEST_BLOB='b1c1fddabbe3c27b9aa2a5b96905bab6d1ac2210'

CODE_HEAD='d0db5de5195abb50b64e7b3abc1c66ebbddf2166'
CODE_TREE='37776170862652c70d3a83db08124d5fab079741'

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

GH_BIN="$(resolve_exec /opt/homebrew/bin/gh /usr/local/bin/gh)" || {
  printf '%s\n' '[Multi-Vex C2 A004] qualified GitHub CLI unavailable.' >&2
  exit 10
}
GIT_BIN="$(resolve_exec /usr/bin/git)" || {
  printf '%s\n' '[Multi-Vex C2 A004] qualified Git unavailable.' >&2
  exit 11
}
BASH_BIN="$(resolve_exec /bin/bash)" || {
  printf '%s\n' '[Multi-Vex C2 A004] qualified Bash unavailable.' >&2
  exit 12
}
MKTEMP_BIN="$(resolve_exec /usr/bin/mktemp)" || {
  printf '%s\n' '[Multi-Vex C2 A004] qualified mktemp unavailable.' >&2
  exit 13
}
RM_BIN="$(resolve_exec /bin/rm /usr/bin/rm)" || {
  printf '%s\n' '[Multi-Vex C2 A004] qualified rm unavailable.' >&2
  exit 14
}

"$GH_BIN" auth status --hostname github.com >/dev/null 2>&1 || {
  printf '%s\n' '[Multi-Vex C2 A004] GitHub authentication unavailable.' >&2
  exit 20
}

DOWNLOADS="$HOME/Downloads"
[ -d "$DOWNLOADS" ] || {
  printf '%s\n' '[Multi-Vex C2 A004] ~/Downloads unavailable.' >&2
  exit 21
}

ROOT="$("$MKTEMP_BIN" -d "$DOWNLOADS/.multivex-c2-a004-pr-bootstrap.XXXXXX")"
cleanup() {
  "$RM_BIN" -rf -- "$ROOT"
}
trap cleanup EXIT HUP INT TERM

SRC="$ROOT/VexLife-Prod"

printf '%s\n' '[Multi-Vex C2 A004] acquiring exact companion-PR bootstrap source...'
"$GH_BIN" repo clone "$REPOSITORY" "$SRC" -- --filter=blob:none --no-checkout >/dev/null
"$GIT_BIN" -C "$SRC" checkout --quiet --detach "$HARNESS_HEAD"

[ "$("$GIT_BIN" -C "$SRC" rev-parse HEAD)" = "$HARNESS_HEAD" ] || {
  printf '%s\n' '[Multi-Vex C2 A004] harness head mismatch.' >&2
  exit 22
}
[ "$("$GIT_BIN" -C "$SRC" rev-parse 'HEAD^{tree}')" = "$HARNESS_TREE" ] || {
  printf '%s\n' '[Multi-Vex C2 A004] harness tree mismatch.' >&2
  exit 23
}
[ -z "$("$GIT_BIN" -C "$SRC" status --porcelain=v1 -uall)" ] || {
  printf '%s\n' '[Multi-Vex C2 A004] acquired source is dirty.' >&2
  exit 24
}

check_blob() {
  local path="$1"
  local expected="$2"
  local observed
  observed="$("$GIT_BIN" -C "$SRC" rev-parse "$HARNESS_HEAD:$path")"
  [ "$observed" = "$expected" ] || {
    printf '[Multi-Vex C2 A004] blob mismatch: %s\n' "$path" >&2
    exit 25
  }
}

check_blob "$PAYLOAD_PATH" "$PAYLOAD_BLOB"
check_blob "$FORMATION_PATH" "$FORMATION_BLOB"
check_blob "$RUNNER_PATH" "$RUNNER_BLOB"
check_blob "$TREATMENT_PATH" "$TREATMENT_BLOB"
check_blob "$SCENARIO_PATH" "$SCENARIO_BLOB"
check_blob "$PROFILE_TEST_PATH" "$PROFILE_TEST_BLOB"

"$GIT_BIN" -C "$SRC" merge-base --is-ancestor "$CODE_HEAD" "$HARNESS_HEAD" || {
  printf '%s\n' '[Multi-Vex C2 A004] code-bearing head is not an ancestor of the frozen harness.' >&2
  exit 26
}
[ "$("$GIT_BIN" -C "$SRC" rev-parse "$CODE_HEAD^{tree}")" = "$CODE_TREE" ] || {
  printf '%s\n' '[Multi-Vex C2 A004] code-bearing tree mismatch.' >&2
  exit 27
}

printf '%s\n' '[Multi-Vex C2 A004] exact PR-owned bootstrap verified.'
printf '%s\n' '[Multi-Vex C2 A004] A003 runner quoting defect is excluded; semantic discriminator is unchanged.'

exec "$BASH_BIN" "$SRC/$RUNNER_PATH"
