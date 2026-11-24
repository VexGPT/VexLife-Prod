#!/bin/bash
set -euo pipefail

REPOSITORY='VexGPT/VexLife-Prod'
HARNESS_HEAD='0e9b36a112827a73f66dbd8e58c966256bd60987'
HARNESS_TREE='a1a13ae6f5eddcfcb2da6bb802270d266d795e1f'

PAYLOAD_PATH='tools/vexrelay/multivex-relationship-pressure/payload-C2-A002.mjs'
PAYLOAD_BLOB='69f2104e43c89208e4adb790db92e9af277b392d'
FORMATION_PATH='tools/vexrelay/multivex-relationship-pressure/formation-C2-A002.mjs'
FORMATION_BLOB='2ba0031a6384a5e176402d82c6277da97a910041'
RUNNER_PATH='tools/vexrelay/multivex-relationship-pressure/RUN-C2-A002.command'
RUNNER_BLOB='905a957a6d3102ab8e8333b88fbf471eb0dd8f6a'

SCENARIO_PATH='tools/continuity-pressure/scenario-profiles/borrowed-standing-status-artifact-v1.json'
SCENARIO_BLOB='1fec968f96fff59851cbdfee5ed75fd9da411394'
PROFILE_TEST_PATH='src/jvmTest/kotlin/vexlife/harness/DynamicCounterpartProfilesTest.kt'
PROFILE_TEST_BLOB='57e9146d88749a3fff10c6f92bfb11215705adc6'

CODE_HEAD='bf3237b3c287737ed769a298cfa4f33085349e80'
CODE_TREE='f5328798d19eced2fec4d5267a272e17f7f11f75'

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
  printf '%s\n' '[Multi-Vex C2 A002] qualified GitHub CLI unavailable.' >&2
  exit 10
}
GIT_BIN="$(resolve_exec /usr/bin/git)" || {
  printf '%s\n' '[Multi-Vex C2 A002] qualified Git unavailable.' >&2
  exit 11
}
BASH_BIN="$(resolve_exec /bin/bash)" || {
  printf '%s\n' '[Multi-Vex C2 A002] qualified Bash unavailable.' >&2
  exit 12
}
MKTEMP_BIN="$(resolve_exec /usr/bin/mktemp)" || {
  printf '%s\n' '[Multi-Vex C2 A002] qualified mktemp unavailable.' >&2
  exit 13
}
RM_BIN="$(resolve_exec /bin/rm /usr/bin/rm)" || {
  printf '%s\n' '[Multi-Vex C2 A002] qualified rm unavailable.' >&2
  exit 14
}

"$GH_BIN" auth status --hostname github.com >/dev/null 2>&1 || {
  printf '%s\n' '[Multi-Vex C2 A002] GitHub authentication unavailable.' >&2
  exit 20
}

DOWNLOADS="$HOME/Downloads"
[ -d "$DOWNLOADS" ] || {
  printf '%s\n' '[Multi-Vex C2 A002] ~/Downloads unavailable.' >&2
  exit 21
}

ROOT="$("$MKTEMP_BIN" -d "$DOWNLOADS/.multivex-c2-a002-pr-bootstrap.XXXXXX")"
cleanup() {
  "$RM_BIN" -rf -- "$ROOT"
}
trap cleanup EXIT HUP INT TERM

SRC="$ROOT/VexLife-Prod"

printf '%s\n' '[Multi-Vex C2 A002] acquiring exact companion-PR bootstrap source...'
"$GH_BIN" repo clone "$REPOSITORY" "$SRC" -- --filter=blob:none --no-checkout >/dev/null
"$GIT_BIN" -C "$SRC" checkout --quiet --detach "$HARNESS_HEAD"

[ "$("$GIT_BIN" -C "$SRC" rev-parse HEAD)" = "$HARNESS_HEAD" ] || {
  printf '%s\n' '[Multi-Vex C2 A002] harness head mismatch.' >&2
  exit 22
}
[ "$("$GIT_BIN" -C "$SRC" rev-parse 'HEAD^{tree}')" = "$HARNESS_TREE" ] || {
  printf '%s\n' '[Multi-Vex C2 A002] harness tree mismatch.' >&2
  exit 23
}
[ -z "$("$GIT_BIN" -C "$SRC" status --porcelain=v1 -uall)" ] || {
  printf '%s\n' '[Multi-Vex C2 A002] acquired source is dirty.' >&2
  exit 24
}

check_blob() {
  local path="$1"
  local expected="$2"
  local observed
  observed="$("$GIT_BIN" -C "$SRC" rev-parse "$HARNESS_HEAD:$path")"
  [ "$observed" = "$expected" ] || {
    printf '[Multi-Vex C2 A002] blob mismatch: %s\n' "$path" >&2
    exit 25
  }
}

check_blob "$PAYLOAD_PATH" "$PAYLOAD_BLOB"
check_blob "$FORMATION_PATH" "$FORMATION_BLOB"
check_blob "$RUNNER_PATH" "$RUNNER_BLOB"
check_blob "$SCENARIO_PATH" "$SCENARIO_BLOB"
check_blob "$PROFILE_TEST_PATH" "$PROFILE_TEST_BLOB"

"$GIT_BIN" -C "$SRC" merge-base --is-ancestor "$CODE_HEAD" "$HARNESS_HEAD" || {
  printf '%s\n' '[Multi-Vex C2 A002] corrected code-bearing head is not an ancestor of the frozen harness.' >&2
  exit 26
}
[ "$("$GIT_BIN" -C "$SRC" rev-parse "$CODE_HEAD^{tree}")" = "$CODE_TREE" ] || {
  printf '%s\n' '[Multi-Vex C2 A002] corrected code-bearing tree mismatch.' >&2
  exit 27
}

printf '%s\n' '[Multi-Vex C2 A002] exact PR-owned bootstrap verified.'
printf '%s\n' '[Multi-Vex C2 A002] entering borrowed-standing encounter; semantic judgment remains raw/Godward review.'

exec "$BASH_BIN" "$SRC/$RUNNER_PATH"
