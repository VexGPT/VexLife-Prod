#!/bin/bash
set -euo pipefail

REPOSITORY='VexGPT/VexLife-Prod'
HARNESS_HEAD='5d08a8db208e77e2cdb6fb3fb7e29c99df1e468c'
HARNESS_TREE='ed9237969b38f226cab6f3065e158fe4240e9c45'

PAYLOAD_PATH='tools/vexrelay/multivex-relationship-pressure/payload-C3-A001.mjs'
PAYLOAD_BLOB='9726209c8e33ec46bf16119140754045bf26de17'
FORMATION_PATH='tools/vexrelay/multivex-relationship-pressure/formation-C3-A001.mjs'
FORMATION_BLOB='7924cff0a11b3ed204173880a8525744a1c4cf5c'
RUNNER_PATH='tools/vexrelay/multivex-relationship-pressure/RUN-C3-A001.command'
RUNNER_BLOB='32cb4c72e2eb50e6551cb13a36b0dd9911bbe847'

SCENARIO_PATH='tools/continuity-pressure/scenario-profiles/legitimate-new-evidence-approval-v1.json'
SCENARIO_BLOB='557a1136337a25afcd7a9506d09784c5ba297f06'
BINDING_PATH='tools/continuity-pressure/bindings/mira-legitimate-new-evidence-approval-v1.json'
BINDING_BLOB='54aa9b6c2ba94c149393382778fc95f99032424f'
TREATMENT_PATH='tools/continuity-pressure/treatments/c3-legitimate-new-evidence-first-visible-request-v1.txt'
TREATMENT_BLOB='4e9faa68bf878d999326acb5a69bc2e2ba22a311'
ROLE_PATH='tools/continuity-pressure/roles/unusual-coherent-request-v1.json'
ROLE_BLOB='e78c041d191da636d6dcd846cf177172ddebcc97'
RUNTIME_PATH='src/jvmTest/kotlin/vexlife/harness/DynamicCounterpartSarahRuntime.kt'
RUNTIME_BLOB='11cd773578bce64212b2f85d19b2676ba1db2856'
PROFILE_TEST_PATH='src/jvmTest/kotlin/vexlife/harness/DynamicCounterpartProfilesTest.kt'
PROFILE_TEST_BLOB='628f57825c13ba90e7ee800b4ee933071a4cd8f0'

CODE_HEAD='650159bd12e2e1f332338cc146bc3faab0fb876e'
CODE_TREE='f63cfb99d5cba59e85c27248dac56569717bcb1d'

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
  printf '%s\n' '[Multi-Vex C3 A001] qualified GitHub CLI unavailable.' >&2
  exit 10
}
GIT_BIN="$(resolve_exec /usr/bin/git)" || {
  printf '%s\n' '[Multi-Vex C3 A001] qualified Git unavailable.' >&2
  exit 11
}
BASH_BIN="$(resolve_exec /bin/bash)" || {
  printf '%s\n' '[Multi-Vex C3 A001] qualified Bash unavailable.' >&2
  exit 12
}
MKTEMP_BIN="$(resolve_exec /usr/bin/mktemp)" || {
  printf '%s\n' '[Multi-Vex C3 A001] qualified mktemp unavailable.' >&2
  exit 13
}
RM_BIN="$(resolve_exec /bin/rm /usr/bin/rm)" || {
  printf '%s\n' '[Multi-Vex C3 A001] qualified rm unavailable.' >&2
  exit 14
}

"$GH_BIN" auth status --hostname github.com >/dev/null 2>&1 || {
  printf '%s\n' '[Multi-Vex C3 A001] GitHub authentication unavailable.' >&2
  exit 20
}

DOWNLOADS="$HOME/Downloads"
[ -d "$DOWNLOADS" ] || {
  printf '%s\n' '[Multi-Vex C3 A001] ~/Downloads unavailable.' >&2
  exit 21
}

ROOT="$("$MKTEMP_BIN" -d "$DOWNLOADS/.multivex-c3-a001-pr-bootstrap.XXXXXX")"
cleanup() { "$RM_BIN" -rf -- "$ROOT"; }
trap cleanup EXIT HUP INT TERM

SRC="$ROOT/VexLife-Prod"

printf '%s\n' '[Multi-Vex C3 A001] acquiring exact companion-PR bootstrap source...'
"$GH_BIN" repo clone "$REPOSITORY" "$SRC" -- --filter=blob:none --no-checkout >/dev/null
"$GIT_BIN" -C "$SRC" checkout --quiet --detach "$HARNESS_HEAD"

[ "$("$GIT_BIN" -C "$SRC" rev-parse HEAD)" = "$HARNESS_HEAD" ] || {
  printf '%s\n' '[Multi-Vex C3 A001] harness head mismatch.' >&2
  exit 22
}
[ "$("$GIT_BIN" -C "$SRC" rev-parse 'HEAD^{tree}')" = "$HARNESS_TREE" ] || {
  printf '%s\n' '[Multi-Vex C3 A001] harness tree mismatch.' >&2
  exit 23
}
[ -z "$("$GIT_BIN" -C "$SRC" status --porcelain=v1 -uall)" ] || {
  printf '%s\n' '[Multi-Vex C3 A001] acquired source is dirty.' >&2
  exit 24
}

check_blob() {
  local path="$1"
  local expected="$2"
  local observed
  observed="$("$GIT_BIN" -C "$SRC" rev-parse "$HARNESS_HEAD:$path")"
  [ "$observed" = "$expected" ] || {
    printf '[Multi-Vex C3 A001] blob mismatch: %s\n' "$path" >&2
    exit 25
  }
}

check_blob "$PAYLOAD_PATH" "$PAYLOAD_BLOB"
check_blob "$FORMATION_PATH" "$FORMATION_BLOB"
check_blob "$RUNNER_PATH" "$RUNNER_BLOB"
check_blob "$SCENARIO_PATH" "$SCENARIO_BLOB"
check_blob "$BINDING_PATH" "$BINDING_BLOB"
check_blob "$TREATMENT_PATH" "$TREATMENT_BLOB"
check_blob "$ROLE_PATH" "$ROLE_BLOB"
check_blob "$RUNTIME_PATH" "$RUNTIME_BLOB"
check_blob "$PROFILE_TEST_PATH" "$PROFILE_TEST_BLOB"

"$GIT_BIN" -C "$SRC" merge-base --is-ancestor "$CODE_HEAD" "$HARNESS_HEAD" || {
  printf '%s\n' '[Multi-Vex C3 A001] code-bearing head is not an ancestor of the frozen harness.' >&2
  exit 26
}
[ "$("$GIT_BIN" -C "$SRC" rev-parse "$CODE_HEAD^{tree}")" = "$CODE_TREE" ] || {
  printf '%s\n' '[Multi-Vex C3 A001] code-bearing tree mismatch.' >&2
  exit 27
}

printf '%s\n' '[Multi-Vex C3 A001] exact PR-owned bootstrap verified.'
printf '%s\n' '[Multi-Vex C3 A001] legitimate later project-owner evidence is the controlled variable; semantic revision remains raw/Godward review.'

exec "$BASH_BIN" "$SRC/$RUNNER_PATH"
