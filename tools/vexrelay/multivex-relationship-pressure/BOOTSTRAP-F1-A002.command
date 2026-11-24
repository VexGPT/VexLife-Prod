#!/bin/bash
set -euo pipefail

REPOSITORY='VexGPT/VexLife-Prod'
HARNESS_HEAD='f0b46a48baff55e3019480815fc07db2bc28556b'
HARNESS_TREE='278f22fb3f2796b91fa97c1bb4d898deb1446a31'

PAYLOAD_PATH='tools/vexrelay/multivex-relationship-pressure/payload-F1-A002.mjs'
PAYLOAD_BLOB='fd1e2df588bac21cc3cdc16f4b3ca351ccd44565'
FORMATION_PATH='tools/vexrelay/multivex-relationship-pressure/formation-F1-A002.mjs'
FORMATION_BLOB='7138c1088269f85e0ec457f4f77146efb63dcd4b'
RUNNER_PATH='tools/vexrelay/multivex-relationship-pressure/RUN-F1-A002.command'
RUNNER_BLOB='a80be86c8f0d7609da44181c34b88cf5b7ac8a25'

REQUEST_PATH='tools/continuity-pressure/treatments/f1-fresh-boot-capability-challenge-v1.txt'
REQUEST_BLOB='6caa7fe266743facd86a7cfa5f3aa361f185284d'
ADAPTER_PATH='src/jvmTest/kotlin/vexlife/harness/MultiVexFreshBootCapabilityAdapter.kt'
ADAPTER_BLOB='7229c2f78d35093c8b98865f25bd3be7bb24ab14'
ADAPTER_TEST_PATH='src/jvmTest/kotlin/vexlife/harness/MultiVexFreshBootCapabilityAdapterTest.kt'
ADAPTER_TEST_BLOB='9bab35e8a580a294c8fd831cd90c93b0ec9fca14'
BUILD_PATH='build.gradle.kts'
BUILD_BLOB='21f86bc55faba150c04abf03611e254ea6f80e9c'

CODE_HEAD='a3dc4fdf5c6243ebb0852c526f119b63e2c28cd9'
CODE_TREE='5017f30af284bff8f278944cf665233aa79d621b'

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
  printf '%s\n' '[Multi-Vex F1 A002] qualified GitHub CLI unavailable.' >&2
  exit 10
}
GIT_BIN="$(resolve_exec /usr/bin/git)" || {
  printf '%s\n' '[Multi-Vex F1 A002] qualified Git unavailable.' >&2
  exit 11
}
BASH_BIN="$(resolve_exec /bin/bash)" || {
  printf '%s\n' '[Multi-Vex F1 A002] qualified Bash unavailable.' >&2
  exit 12
}
MKTEMP_BIN="$(resolve_exec /usr/bin/mktemp)" || {
  printf '%s\n' '[Multi-Vex F1 A002] qualified mktemp unavailable.' >&2
  exit 13
}
RM_BIN="$(resolve_exec /bin/rm /usr/bin/rm)" || {
  printf '%s\n' '[Multi-Vex F1 A002] qualified rm unavailable.' >&2
  exit 14
}

"$GH_BIN" auth status --hostname github.com >/dev/null 2>&1 || {
  printf '%s\n' '[Multi-Vex F1 A002] GitHub authentication unavailable.' >&2
  exit 20
}

DOWNLOADS="$HOME/Downloads"
[ -d "$DOWNLOADS" ] || {
  printf '%s\n' '[Multi-Vex F1 A002] ~/Downloads unavailable.' >&2
  exit 21
}

ROOT="$("$MKTEMP_BIN" -d "$DOWNLOADS/.multivex-f1-a002-pr-bootstrap.XXXXXX")"
cleanup() { "$RM_BIN" -rf -- "$ROOT"; }
trap cleanup EXIT HUP INT TERM

SRC="$ROOT/VexLife-Prod"

printf '%s\n' '[Multi-Vex F1 A002] acquiring exact companion-PR bootstrap source...'
"$GH_BIN" repo clone "$REPOSITORY" "$SRC" -- --filter=blob:none --no-checkout >/dev/null
"$GIT_BIN" -C "$SRC" checkout --quiet --detach "$HARNESS_HEAD"

[ "$("$GIT_BIN" -C "$SRC" rev-parse HEAD)" = "$HARNESS_HEAD" ] || {
  printf '%s\n' '[Multi-Vex F1 A002] harness head mismatch.' >&2
  exit 22
}
[ "$("$GIT_BIN" -C "$SRC" rev-parse 'HEAD^{tree}')" = "$HARNESS_TREE" ] || {
  printf '%s\n' '[Multi-Vex F1 A002] harness tree mismatch.' >&2
  exit 23
}
[ -z "$("$GIT_BIN" -C "$SRC" status --porcelain=v1 -uall)" ] || {
  printf '%s\n' '[Multi-Vex F1 A002] acquired source is dirty.' >&2
  exit 24
}

check_blob() {
  local path="$1"
  local expected="$2"
  local observed
  observed="$("$GIT_BIN" -C "$SRC" rev-parse "$HARNESS_HEAD:$path")"
  [ "$observed" = "$expected" ] || {
    printf '[Multi-Vex F1 A002] blob mismatch: %s\n' "$path" >&2
    exit 25
  }
}

check_blob "$PAYLOAD_PATH" "$PAYLOAD_BLOB"
check_blob "$FORMATION_PATH" "$FORMATION_BLOB"
check_blob "$RUNNER_PATH" "$RUNNER_BLOB"
check_blob "$REQUEST_PATH" "$REQUEST_BLOB"
check_blob "$ADAPTER_PATH" "$ADAPTER_BLOB"
check_blob "$ADAPTER_TEST_PATH" "$ADAPTER_TEST_BLOB"
check_blob "$BUILD_PATH" "$BUILD_BLOB"

"$GIT_BIN" -C "$SRC" merge-base --is-ancestor "$CODE_HEAD" "$HARNESS_HEAD" || {
  printf '%s\n' '[Multi-Vex F1 A002] code-bearing head is not an ancestor of the frozen harness.' >&2
  exit 26
}
[ "$("$GIT_BIN" -C "$SRC" rev-parse "$CODE_HEAD^{tree}")" = "$CODE_TREE" ] || {
  printf '%s\n' '[Multi-Vex F1 A002] code-bearing tree mismatch.' >&2
  exit 27
}

printf '%s\n' '[Multi-Vex F1 A002] exact PR-owned bootstrap verified.'
printf '%s\n' '[Multi-Vex F1 A002] fresh scratch Conversation + native capability offer are proved mechanically; actual demonstration quality remains raw/Godward review.'

exec "$BASH_BIN" "$SRC/$RUNNER_PATH"
