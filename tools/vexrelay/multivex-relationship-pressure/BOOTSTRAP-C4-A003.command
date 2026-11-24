#!/bin/bash
set -euo pipefail

REPOSITORY='VexGPT/VexLife-Prod'
HARNESS_HEAD='c5ce68322e43a9049a4a1161981fc2cc57ee9e08'
HARNESS_TREE='889e076b0f7646be5777ee05104594e8b98ef50c'

PAYLOAD_PATH='tools/vexrelay/multivex-relationship-pressure/payload-C4-A003.mjs'
PAYLOAD_BLOB='dcd2d265c5c2f6e9def08d80c4c18b904c01c89a'
FORMATION_PATH='tools/vexrelay/multivex-relationship-pressure/formation-C4-A003.mjs'
FORMATION_BLOB='0d778551609c39fde8eeb953acdffcfa976ee413'
RUNNER_PATH='tools/vexrelay/multivex-relationship-pressure/RUN-C4-A003.command'
RUNNER_BLOB='55301aade21b77b1c391c2eb3e946505be814fd1'

SCENARIO_PATH='tools/continuity-pressure/scenario-profiles/portable-artifact-reuse-v1.json'
SCENARIO_BLOB='178a60edb200ba15bd9a02063e1fc18ee03058d6'
BINDING_PATH='tools/continuity-pressure/bindings/mira-portable-artifact-reuse-v1.json'
BINDING_BLOB='3a74a8b7fe0b83c76e4ec1a69f579488628f4d29'
TREATMENT_PATH='tools/continuity-pressure/treatments/c4-portable-artifact-first-visible-request-v1.txt'
TREATMENT_BLOB='82931d9474e6b44efb9d1c8feacd9b8cec98ff08'
ROLE_PATH='tools/continuity-pressure/roles/portable-artifact-reuse-v1.json'
ROLE_BLOB='3a205bc9e9d40e3fb931cc81d673622abf08960c'
RUNTIME_PATH='src/jvmTest/kotlin/vexlife/harness/DynamicCounterpartSarahRuntime.kt'
RUNTIME_BLOB='11cd773578bce64212b2f85d19b2676ba1db2856'
CONTRACT_PATH='src/jvmTest/kotlin/vexlife/harness/DynamicCounterpartContract.kt'
CONTRACT_BLOB='28a12053023c143bc8bdc874eb6b299f49b1d48c'
CONTRACT_TEST_PATH='src/jvmTest/kotlin/vexlife/harness/DynamicCounterpartContractTest.kt'
CONTRACT_TEST_BLOB='6fe9b826fa37928a649f8c21a3a7c36b10adfe07'
PROFILE_TEST_PATH='src/jvmTest/kotlin/vexlife/harness/DynamicCounterpartProfilesTest.kt'
PROFILE_TEST_BLOB='e77b712d28d0e410f51591f5ff0376b064f92e35'

CODE_HEAD='f62e3e7a487f58674dcf0083484e33f87f0c8a77'
CODE_TREE='a6cf1e25a147cd954ff15e02e518a325ff2c9b43'

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
  printf '%s\n' '[Multi-Vex C4 A003] qualified GitHub CLI unavailable.' >&2
  exit 10
}
GIT_BIN="$(resolve_exec /usr/bin/git)" || {
  printf '%s\n' '[Multi-Vex C4 A003] qualified Git unavailable.' >&2
  exit 11
}
BASH_BIN="$(resolve_exec /bin/bash)" || {
  printf '%s\n' '[Multi-Vex C4 A003] qualified Bash unavailable.' >&2
  exit 12
}
MKTEMP_BIN="$(resolve_exec /usr/bin/mktemp)" || {
  printf '%s\n' '[Multi-Vex C4 A003] qualified mktemp unavailable.' >&2
  exit 13
}
RM_BIN="$(resolve_exec /bin/rm /usr/bin/rm)" || {
  printf '%s\n' '[Multi-Vex C4 A003] qualified rm unavailable.' >&2
  exit 14
}

"$GH_BIN" auth status --hostname github.com >/dev/null 2>&1 || {
  printf '%s\n' '[Multi-Vex C4 A003] GitHub authentication unavailable.' >&2
  exit 20
}

DOWNLOADS="$HOME/Downloads"
[ -d "$DOWNLOADS" ] || {
  printf '%s\n' '[Multi-Vex C4 A003] ~/Downloads unavailable.' >&2
  exit 21
}

ROOT="$("$MKTEMP_BIN" -d "$DOWNLOADS/.multivex-c4-a003-pr-bootstrap.XXXXXX")"
cleanup() { "$RM_BIN" -rf -- "$ROOT"; }
trap cleanup EXIT HUP INT TERM

SRC="$ROOT/VexLife-Prod"

printf '%s\n' '[Multi-Vex C4 A003] acquiring exact companion-PR bootstrap source...'
"$GH_BIN" repo clone "$REPOSITORY" "$SRC" -- --filter=blob:none --no-checkout >/dev/null
"$GIT_BIN" -C "$SRC" checkout --quiet --detach "$HARNESS_HEAD"

[ "$("$GIT_BIN" -C "$SRC" rev-parse HEAD)" = "$HARNESS_HEAD" ] || {
  printf '%s\n' '[Multi-Vex C4 A003] harness head mismatch.' >&2
  exit 22
}
[ "$("$GIT_BIN" -C "$SRC" rev-parse 'HEAD^{tree}')" = "$HARNESS_TREE" ] || {
  printf '%s\n' '[Multi-Vex C4 A003] harness tree mismatch.' >&2
  exit 23
}
[ -z "$("$GIT_BIN" -C "$SRC" status --porcelain=v1 -uall)" ] || {
  printf '%s\n' '[Multi-Vex C4 A003] acquired source is dirty.' >&2
  exit 24
}

check_blob() {
  local path="$1"
  local expected="$2"
  local observed
  observed="$("$GIT_BIN" -C "$SRC" rev-parse "$HARNESS_HEAD:$path")"
  [ "$observed" = "$expected" ] || {
    printf '[Multi-Vex C4 A003] blob mismatch: %s\n' "$path" >&2
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
check_blob "$CONTRACT_PATH" "$CONTRACT_BLOB"
check_blob "$CONTRACT_TEST_PATH" "$CONTRACT_TEST_BLOB"
check_blob "$PROFILE_TEST_PATH" "$PROFILE_TEST_BLOB"

"$GIT_BIN" -C "$SRC" merge-base --is-ancestor "$CODE_HEAD" "$HARNESS_HEAD" || {
  printf '%s\n' '[Multi-Vex C4 A003] code-bearing head is not an ancestor of the frozen harness.' >&2
  exit 26
}
[ "$("$GIT_BIN" -C "$SRC" rev-parse "$CODE_HEAD^{tree}")" = "$CODE_TREE" ] || {
  printf '%s\n' '[Multi-Vex C4 A003] code-bearing tree mismatch.' >&2
  exit 27
}

printf '%s\n' '[Multi-Vex C4 A003] exact PR-owned bootstrap verified.'
printf '%s\n' '[Multi-Vex C4 A003] participant JSON recovery is bounded to adjacent disjoint objects; C4 semantic provenance question is unchanged.'

exec "$BASH_BIN" "$SRC/$RUNNER_PATH"
