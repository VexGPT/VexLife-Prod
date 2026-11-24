#!/bin/bash
# [VXG RealForever]
set -euo pipefail

HARNESS_REPO='VexGPT/VexLife-Prod'
HARNESS_HEAD='fc69c8a20781fc22fd8a373046a99f66cf0f78c3'
HARNESS_TREE='e1c7afaba237c0b248234e7ab4ce912df0570e58'
README_BLOB='69dc929cd29ac5a2f6608c15bfadaf1da5a3fe3b'
PAYLOAD_BLOB='7695011043f93038edf6a638f2475a0888494b4e'
FORMATION_BLOB='3ec817991a7b7f656b58ea9fc0b5db2af46bf9e0'
RUN_BLOB='97d059bf48685427709d3d927819fec0d15c7bfb'

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

GH="$(find_exe /opt/homebrew/bin/gh /usr/local/bin/gh)" || { echo '[Multi-Vex conversation-chronology CH2 A001] GitHub CLI unavailable.' >&2; exit 20; }
GIT=/usr/bin/git
WORK="$(/usr/bin/mktemp -d "$HOME/Downloads/.vexlife-multivex-relationship-pressure-ch2-a001-bootstrap.XXXXXX")"
cleanup() {
  case "$WORK" in
    "$HOME"/Downloads/.vexlife-multivex-relationship-pressure-ch2-a001-bootstrap.*)
      /bin/rm -rf -- "$WORK"
      ;;
  esac
}
trap cleanup EXIT HUP INT TERM

"$GH" auth status --hostname github.com >/dev/null 2>&1 || { echo '[Multi-Vex conversation-chronology CH2 A001] GitHub authentication unavailable.' >&2; exit 21; }

REPO="$WORK/VexLife-Prod"
echo '[Multi-Vex conversation-chronology CH2 A001] acquiring frozen CH1 changed-plan transfer harness...'
"$GH" repo clone "$HARNESS_REPO" "$REPO" -- --filter=blob:none --no-checkout >/dev/null
"$GIT" -C "$REPO" checkout --quiet --detach "$HARNESS_HEAD"

[ "$("$GIT" -C "$REPO" rev-parse HEAD)" = "$HARNESS_HEAD" ] || { echo 'Harness head mismatch.' >&2; exit 22; }
[ "$("$GIT" -C "$REPO" rev-parse 'HEAD^{tree}')" = "$HARNESS_TREE" ] || { echo 'Harness tree mismatch.' >&2; exit 23; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/multivex-relationship-pressure/README.md")" = "$README_BLOB" ] || { echo 'README blob mismatch.' >&2; exit 24; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/multivex-relationship-pressure/payload-CH2-A001.mjs")" = "$PAYLOAD_BLOB" ] || { echo 'Payload blob mismatch.' >&2; exit 25; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/multivex-relationship-pressure/formation-CH2-A001.mjs")" = "$FORMATION_BLOB" ] || { echo 'Formation blob mismatch.' >&2; exit 26; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/multivex-relationship-pressure/RUN-CH2-A001.command")" = "$RUN_BLOB" ] || { echo 'Runner blob mismatch.' >&2; exit 27; }
[ -z "$("$GIT" -C "$REPO" status --porcelain=v1 -uall)" ] || { echo 'Harness checkout dirty.' >&2; exit 28; }
/bin/bash -n "$REPO/tools/vexrelay/multivex-relationship-pressure/RUN-CH2-A001.command" || { echo 'Runner syntax invalid.' >&2; exit 29; }
NODE="$(find_exe /opt/homebrew/bin/node /usr/local/bin/node /usr/bin/node)" || { echo 'Node unavailable for harness syntax check.' >&2; exit 30; }
"$NODE" --check "$REPO/tools/vexrelay/multivex-relationship-pressure/formation-CH2-A001.mjs" || { echo 'Formation syntax invalid.' >&2; exit 31; }
"$NODE" --check "$REPO/tools/vexrelay/multivex-relationship-pressure/payload-CH2-A001.mjs" || { echo 'Payload syntax invalid.' >&2; exit 32; }

echo '[Multi-Vex conversation-chronology CH2 A001] frozen harness verified; entering CH1 changed-plan transfer under exact Foundation v0.6.20 without changing Product cache settings.'
/bin/bash "$REPO/tools/vexrelay/multivex-relationship-pressure/RUN-CH2-A001.command"
