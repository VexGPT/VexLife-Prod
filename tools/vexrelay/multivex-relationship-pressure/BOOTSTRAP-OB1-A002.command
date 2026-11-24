#!/bin/bash
# [VXG RealForever]
set -euo pipefail

HARNESS_REPO='VexGPT/VexLife-Prod'
HARNESS_HEAD='a060afbe62976ea39ed89aa66a70c3e2d7a9f134'
HARNESS_TREE='73e445e179ee2c1fa7eb3fb4e8072688d73180cf'
README_BLOB='70017d34422d45a0df110914f030b8653025c120'
PAYLOAD_BLOB='8b1995089a884b81f09a2130be95c5d68958f687'
FORMATION_BLOB='d7d4fcad61625e973d5eef94103c766e78db44cb'
RUN_BLOB='17d7ed58abbe60c5c79508deffa1e9785924b1c7'

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

GH="$(find_exe /opt/homebrew/bin/gh /usr/local/bin/gh)" || { echo '[Multi-Vex owner-boundary OB1 A002] GitHub CLI unavailable.' >&2; exit 20; }
GIT=/usr/bin/git
WORK="$(/usr/bin/mktemp -d "$HOME/Downloads/.vexlife-multivex-relationship-pressure-ob1-a002-bootstrap.XXXXXX")"
cleanup() {
  case "$WORK" in
    "$HOME"/Downloads/.vexlife-multivex-relationship-pressure-ob1-a002-bootstrap.*)
      /bin/rm -rf -- "$WORK"
      ;;
  esac
}
trap cleanup EXIT HUP INT TERM

"$GH" auth status --hostname github.com >/dev/null 2>&1 || { echo '[Multi-Vex owner-boundary OB1 A002] GitHub authentication unavailable.' >&2; exit 21; }

REPO="$WORK/VexLife-Prod"
echo '[Multi-Vex owner-boundary OB1 A002] acquiring frozen OB1 explicit owner-boundary discriminator harness...'
"$GH" repo clone "$HARNESS_REPO" "$REPO" -- --filter=blob:none --no-checkout >/dev/null
"$GIT" -C "$REPO" checkout --quiet --detach "$HARNESS_HEAD"

[ "$("$GIT" -C "$REPO" rev-parse HEAD)" = "$HARNESS_HEAD" ] || { echo 'Harness head mismatch.' >&2; exit 22; }
[ "$("$GIT" -C "$REPO" rev-parse 'HEAD^{tree}')" = "$HARNESS_TREE" ] || { echo 'Harness tree mismatch.' >&2; exit 23; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/multivex-relationship-pressure/README.md")" = "$README_BLOB" ] || { echo 'README blob mismatch.' >&2; exit 24; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/multivex-relationship-pressure/payload-OB1-A002.mjs")" = "$PAYLOAD_BLOB" ] || { echo 'Payload blob mismatch.' >&2; exit 25; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/multivex-relationship-pressure/formation-OB1-A002.mjs")" = "$FORMATION_BLOB" ] || { echo 'Formation blob mismatch.' >&2; exit 26; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/multivex-relationship-pressure/RUN-OB1-A002.command")" = "$RUN_BLOB" ] || { echo 'Runner blob mismatch.' >&2; exit 27; }
[ -z "$("$GIT" -C "$REPO" status --porcelain=v1 -uall)" ] || { echo 'Harness checkout dirty.' >&2; exit 28; }
/bin/bash -n "$REPO/tools/vexrelay/multivex-relationship-pressure/RUN-OB1-A002.command" || { echo 'Runner syntax invalid.' >&2; exit 29; }
NODE="$(find_exe /opt/homebrew/bin/node /usr/local/bin/node /usr/bin/node)" || { echo 'Node unavailable for harness syntax check.' >&2; exit 30; }
"$NODE" --check "$REPO/tools/vexrelay/multivex-relationship-pressure/formation-OB1-A002.mjs" || { echo 'Formation syntax invalid.' >&2; exit 31; }
"$NODE" --check "$REPO/tools/vexrelay/multivex-relationship-pressure/payload-OB1-A002.mjs" || { echo 'Payload syntax invalid.' >&2; exit 32; }

echo '[Multi-Vex owner-boundary OB1 A002] frozen harness verified; entering OB1 explicit owner-boundary discriminator under exact Foundation v0.6.20 without changing Product cache settings.'
/bin/bash "$REPO/tools/vexrelay/multivex-relationship-pressure/RUN-OB1-A002.command"
