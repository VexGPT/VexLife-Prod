#!/bin/bash
set -euo pipefail

REPOSITORY='VexGPT/VexLife-Prod'
HARNESS_HEAD='518c5baed3c0de231feca84ebfeb0ad30ef4fe4c'
HARNESS_TREE='782b1f14f48b392327c0bd0fd47e97e612dac323'

PAYLOAD_PATH='tools/vexrelay/multivex-relationship-pressure/payload-C2-A005.mjs'
PAYLOAD_BLOB='145049f68aa4a8a264b029a60b5208b1bad8f217'
FORMATION_PATH='tools/vexrelay/multivex-relationship-pressure/formation-C2-A005.mjs'
FORMATION_BLOB='8f53dc0b362b5f693316549b8b91c78edea8b4a0'
RUNNER_PATH='tools/vexrelay/multivex-relationship-pressure/RUN-C2-A005.command'
RUNNER_BLOB='dba5e9ea85f35c1a51141c09b40a6a1285bd5466'

TREATMENT_PATH='tools/continuity-pressure/treatments/c2-borrowed-standing-first-visible-request-v1.txt'
TREATMENT_BLOB='0656f684169cf5a62a0b6f472be21097b42dc0ec'
SCENARIO_PATH='tools/continuity-pressure/scenario-profiles/borrowed-standing-status-artifact-v1.json'
SCENARIO_BLOB='ae5a89deb5fc030cd798b5b6cb4087191a4cdd09'
PROFILE_TEST_PATH='src/jvmTest/kotlin/vexlife/harness/DynamicCounterpartProfilesTest.kt'
PROFILE_TEST_BLOB='57d037d94e9b1db39c933e0e7599b8060862e6c9'

CODE_HEAD='5321be956dfc2dfe594b2df9628654eb522b0dc2'
CODE_TREE='1d8f4b93c7a41062bc0c7f392472c48c89ab0218'

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
  printf '%s\n' '[Multi-Vex C2 A005] qualified GitHub CLI unavailable.' >&2
  exit 10
}
GIT_BIN="$(resolve_exec /usr/bin/git)" || {
  printf '%s\n' '[Multi-Vex C2 A005] qualified Git unavailable.' >&2
  exit 11
}
BASH_BIN="$(resolve_exec /bin/bash)" || {
  printf '%s\n' '[Multi-Vex C2 A005] qualified Bash unavailable.' >&2
  exit 12
}
MKTEMP_BIN="$(resolve_exec /usr/bin/mktemp)" || {
  printf '%s\n' '[Multi-Vex C2 A005] qualified mktemp unavailable.' >&2
  exit 13
}
RM_BIN="$(resolve_exec /bin/rm /usr/bin/rm)" || {
  printf '%s\n' '[Multi-Vex C2 A005] qualified rm unavailable.' >&2
  exit 14
}

"$GH_BIN" auth status --hostname github.com >/dev/null 2>&1 || {
  printf '%s\n' '[Multi-Vex C2 A005] GitHub authentication unavailable.' >&2
  exit 20
}

DOWNLOADS="$HOME/Downloads"
[ -d "$DOWNLOADS" ] || {
  printf '%s\n' '[Multi-Vex C2 A005] ~/Downloads unavailable.' >&2
  exit 21
}

ROOT="$("$MKTEMP_BIN" -d "$DOWNLOADS/.multivex-c2-a005-pr-bootstrap.XXXXXX")"
cleanup() { "$RM_BIN" -rf -- "$ROOT"; }
trap cleanup EXIT HUP INT TERM

SRC="$ROOT/VexLife-Prod"

printf '%s\n' '[Multi-Vex C2 A005] acquiring exact companion-PR bootstrap source...'
"$GH_BIN" repo clone "$REPOSITORY" "$SRC" -- --filter=blob:none --no-checkout >/dev/null
"$GIT_BIN" -C "$SRC" checkout --quiet --detach "$HARNESS_HEAD"

[ "$("$GIT_BIN" -C "$SRC" rev-parse HEAD)" = "$HARNESS_HEAD" ] || {
  printf '%s\n' '[Multi-Vex C2 A005] harness head mismatch.' >&2
  exit 22
}
[ "$("$GIT_BIN" -C "$SRC" rev-parse 'HEAD^{tree}')" = "$HARNESS_TREE" ] || {
  printf '%s\n' '[Multi-Vex C2 A005] harness tree mismatch.' >&2
  exit 23
}
[ -z "$("$GIT_BIN" -C "$SRC" status --porcelain=v1 -uall)" ] || {
  printf '%s\n' '[Multi-Vex C2 A005] acquired source is dirty.' >&2
  exit 24
}

check_blob() {
  local path="$1"
  local expected="$2"
  local observed
  observed="$("$GIT_BIN" -C "$SRC" rev-parse "$HARNESS_HEAD:$path")"
  [ "$observed" = "$expected" ] || {
    printf '[Multi-Vex C2 A005] blob mismatch: %s\n' "$path" >&2
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
  printf '%s\n' '[Multi-Vex C2 A005] code-bearing head is not an ancestor of the frozen harness.' >&2
  exit 26
}
[ "$("$GIT_BIN" -C "$SRC" rev-parse "$CODE_HEAD^{tree}")" = "$CODE_TREE" ] || {
  printf '%s\n' '[Multi-Vex C2 A005] code-bearing tree mismatch.' >&2
  exit 27
}

printf '%s\n' '[Multi-Vex C2 A005] exact PR-owned bootstrap verified.'
printf '%s\n' '[Multi-Vex C2 A005] reviewer-weight ground has no private fact/event refs; borrowed-standing discriminator otherwise unchanged.'

exec "$BASH_BIN" "$SRC/$RUNNER_PATH"
