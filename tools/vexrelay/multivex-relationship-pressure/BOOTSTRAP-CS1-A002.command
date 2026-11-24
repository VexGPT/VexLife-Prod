#!/bin/bash
# [VXG RealForever]
set -euo pipefail

HARNESS_REPO='VexGPT/VexLife-Prod'
HARNESS_HEAD='1f1fdd40624d78bb39cbb5766a06bbd343322c84'
HARNESS_TREE='78e3e76c788593651c1aa00f80b8a27e8195ec25'
README_BLOB='6efb63d725318cf74b1d230eb442682bf760ba93'
PAYLOAD_BLOB='72499e3a99cfd6caf825d0854555e1837dadd3c2'
FORMATION_BLOB='4964a7ab32cc10bfb10a4389c15b4dc9e00080f3'
RUN_BLOB='9cf077899f7f3508ae2d54aeda04b33a933d2c58'

find_exe() {
  local candidate
  for candidate in "$@"; do
    if [ -x "$candidate" ] && [ -f "$candidate" ]; then
      printf '%s\n' "$candidate"
      return 0
    fi
  done
  return 1
}

GH="$(find_exe /opt/homebrew/bin/gh /usr/local/bin/gh)" || { echo '[Multi-Vex current-state-owner-scope CS1 A002] GitHub CLI unavailable.' >&2; exit 20; }
GIT=/usr/bin/git
WORK="$(/usr/bin/mktemp -d "$HOME/Downloads/.vexlife-multivex-relationship-pressure-cs1-a002-bootstrap.XXXXXX")"
cleanup() {
  case "$WORK" in
    "$HOME"/Downloads/.vexlife-multivex-relationship-pressure-cs1-a002-bootstrap.*)
      /bin/rm -rf -- "$WORK"
      ;;
  esac
}
trap cleanup EXIT HUP INT TERM

"$GH" auth status --hostname github.com >/dev/null 2>&1 || { echo '[Multi-Vex current-state-owner-scope CS1 A002] GitHub authentication unavailable.' >&2; exit 21; }

REPO="$WORK/VexLife-Prod"
echo '[Multi-Vex current-state-owner-scope CS1 A002] acquiring frozen CS1 current-state owner-scope treatment harness...'
"$GH" repo clone "$HARNESS_REPO" "$REPO" -- --filter=blob:none --no-checkout >/dev/null
"$GIT" -C "$REPO" checkout --quiet --detach "$HARNESS_HEAD"

[ "$("$GIT" -C "$REPO" rev-parse HEAD)" = "$HARNESS_HEAD" ] || { echo 'Harness head mismatch.' >&2; exit 22; }
[ "$("$GIT" -C "$REPO" rev-parse 'HEAD^{tree}')" = "$HARNESS_TREE" ] || { echo 'Harness tree mismatch.' >&2; exit 23; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/multivex-relationship-pressure/README.md")" = "$README_BLOB" ] || { echo 'README blob mismatch.' >&2; exit 24; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/multivex-relationship-pressure/payload-CS1-A002.mjs")" = "$PAYLOAD_BLOB" ] || { echo 'Payload blob mismatch.' >&2; exit 25; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/multivex-relationship-pressure/formation-CS1-A002.mjs")" = "$FORMATION_BLOB" ] || { echo 'Formation blob mismatch.' >&2; exit 26; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/multivex-relationship-pressure/RUN-CS1-A002.command")" = "$RUN_BLOB" ] || { echo 'Runner blob mismatch.' >&2; exit 27; }
[ -z "$("$GIT" -C "$REPO" status --porcelain=v1 -uall)" ] || { echo 'Harness checkout dirty.' >&2; exit 28; }
/bin/bash -n "$REPO/tools/vexrelay/multivex-relationship-pressure/RUN-CS1-A002.command" || { echo 'Runner syntax invalid.' >&2; exit 29; }
NODE="$(find_exe /opt/homebrew/bin/node /usr/local/bin/node /usr/bin/node)" || { echo 'Node unavailable for harness syntax check.' >&2; exit 30; }
"$NODE" --check "$REPO/tools/vexrelay/multivex-relationship-pressure/formation-CS1-A002.mjs" || { echo 'Formation syntax invalid.' >&2; exit 31; }
"$NODE" --check "$REPO/tools/vexrelay/multivex-relationship-pressure/payload-CS1-A002.mjs" || { echo 'Payload syntax invalid.' >&2; exit 32; }

echo '[Multi-Vex current-state-owner-scope CS1 A002] frozen harness verified; entering CS1 current-state owner-scope treatment under exact Foundation v0.6.20 without changing Product cache settings.'
/bin/bash "$REPO/tools/vexrelay/multivex-relationship-pressure/RUN-CS1-A002.command"
