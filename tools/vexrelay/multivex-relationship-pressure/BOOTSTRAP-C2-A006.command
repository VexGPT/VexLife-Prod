#!/bin/bash
set -euo pipefail

REPOSITORY='VexGPT/VexLife-Prod'
HARNESS_HEAD='a5af842d9d280fd607e9c4706cf4884762a9396b'
HARNESS_TREE='acbb77504bcf5ab98ee5a742762a6315e1d7bb73'

PAYLOAD_PATH='tools/vexrelay/multivex-relationship-pressure/payload-C2-A006.mjs'
PAYLOAD_BLOB='2aa2bcbaf6e79b8a4487854851c94b94169bc93a'
FORMATION_PATH='tools/vexrelay/multivex-relationship-pressure/formation-C2-A006.mjs'
FORMATION_BLOB='e4da8785b53768c0268ab42680f8cf27533f6118'
RUNNER_PATH='tools/vexrelay/multivex-relationship-pressure/RUN-C2-A006.command'
RUNNER_BLOB='7db78ec2ec17363b172bd0fe2d7d86942dea6f84'

TREATMENT_PATH='tools/continuity-pressure/treatments/c2-borrowed-standing-first-visible-request-v1.txt'
TREATMENT_BLOB='0656f684169cf5a62a0b6f472be21097b42dc0ec'
SCENARIO_PATH='tools/continuity-pressure/scenario-profiles/borrowed-standing-status-artifact-v1.json'
SCENARIO_BLOB='ae5a89deb5fc030cd798b5b6cb4087191a4cdd09'
RUNTIME_PATH='src/jvmTest/kotlin/vexlife/harness/DynamicCounterpartSarahRuntime.kt'
RUNTIME_BLOB='11cd773578bce64212b2f85d19b2676ba1db2856'
PROFILE_TEST_PATH='src/jvmTest/kotlin/vexlife/harness/DynamicCounterpartProfilesTest.kt'
PROFILE_TEST_BLOB='99ea593f5dc017a7295161e6d38fdcfc6ce4496f'

CODE_HEAD='01300e4a5cc5c1f99287d0d75d979bbff171d665'
CODE_TREE='61d035f5a01847acfe4d0ca5fd118412df62fe0b'

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
  printf '%s\n' '[Multi-Vex C2 A006] qualified GitHub CLI unavailable.' >&2
  exit 10
}
GIT_BIN="$(resolve_exec /usr/bin/git)" || {
  printf '%s\n' '[Multi-Vex C2 A006] qualified Git unavailable.' >&2
  exit 11
}
BASH_BIN="$(resolve_exec /bin/bash)" || {
  printf '%s\n' '[Multi-Vex C2 A006] qualified Bash unavailable.' >&2
  exit 12
}
MKTEMP_BIN="$(resolve_exec /usr/bin/mktemp)" || {
  printf '%s\n' '[Multi-Vex C2 A006] qualified mktemp unavailable.' >&2
  exit 13
}
RM_BIN="$(resolve_exec /bin/rm /usr/bin/rm)" || {
  printf '%s\n' '[Multi-Vex C2 A006] qualified rm unavailable.' >&2
  exit 14
}

"$GH_BIN" auth status --hostname github.com >/dev/null 2>&1 || {
  printf '%s\n' '[Multi-Vex C2 A006] GitHub authentication unavailable.' >&2
  exit 20
}

DOWNLOADS="$HOME/Downloads"
[ -d "$DOWNLOADS" ] || {
  printf '%s\n' '[Multi-Vex C2 A006] ~/Downloads unavailable.' >&2
  exit 21
}

ROOT="$("$MKTEMP_BIN" -d "$DOWNLOADS/.multivex-c2-a006-pr-bootstrap.XXXXXX")"
cleanup() { "$RM_BIN" -rf -- "$ROOT"; }
trap cleanup EXIT HUP INT TERM

SRC="$ROOT/VexLife-Prod"

printf '%s\n' '[Multi-Vex C2 A006] acquiring exact companion-PR bootstrap source...'
"$GH_BIN" repo clone "$REPOSITORY" "$SRC" -- --filter=blob:none --no-checkout >/dev/null
"$GIT_BIN" -C "$SRC" checkout --quiet --detach "$HARNESS_HEAD"

[ "$("$GIT_BIN" -C "$SRC" rev-parse HEAD)" = "$HARNESS_HEAD" ] || {
  printf '%s\n' '[Multi-Vex C2 A006] harness head mismatch.' >&2
  exit 22
}
[ "$("$GIT_BIN" -C "$SRC" rev-parse 'HEAD^{tree}')" = "$HARNESS_TREE" ] || {
  printf '%s\n' '[Multi-Vex C2 A006] harness tree mismatch.' >&2
  exit 23
}
[ -z "$("$GIT_BIN" -C "$SRC" status --porcelain=v1 -uall)" ] || {
  printf '%s\n' '[Multi-Vex C2 A006] acquired source is dirty.' >&2
  exit 24
}

check_blob() {
  local path="$1"
  local expected="$2"
  local observed
  observed="$("$GIT_BIN" -C "$SRC" rev-parse "$HARNESS_HEAD:$path")"
  [ "$observed" = "$expected" ] || {
    printf '[Multi-Vex C2 A006] blob mismatch: %s\n' "$path" >&2
    exit 25
  }
}

check_blob "$PAYLOAD_PATH" "$PAYLOAD_BLOB"
check_blob "$FORMATION_PATH" "$FORMATION_BLOB"
check_blob "$RUNNER_PATH" "$RUNNER_BLOB"
check_blob "$TREATMENT_PATH" "$TREATMENT_BLOB"
check_blob "$SCENARIO_PATH" "$SCENARIO_BLOB"
check_blob "$RUNTIME_PATH" "$RUNTIME_BLOB"
check_blob "$PROFILE_TEST_PATH" "$PROFILE_TEST_BLOB"

"$GIT_BIN" -C "$SRC" merge-base --is-ancestor "$CODE_HEAD" "$HARNESS_HEAD" || {
  printf '%s\n' '[Multi-Vex C2 A006] code-bearing head is not an ancestor of the frozen harness.' >&2
  exit 26
}
[ "$("$GIT_BIN" -C "$SRC" rev-parse "$CODE_HEAD^{tree}")" = "$CODE_TREE" ] || {
  printf '%s\n' '[Multi-Vex C2 A006] code-bearing tree mismatch.' >&2
  exit 27
}

printf '%s\n' '[Multi-Vex C2 A006] exact PR-owned bootstrap verified.'
printf '%s\n' '[Multi-Vex C2 A006] eventless profiles use one fact-free participant turn trigger; C2 semantic discriminator is unchanged.'

exec "$BASH_BIN" "$SRC/$RUNNER_PATH"
