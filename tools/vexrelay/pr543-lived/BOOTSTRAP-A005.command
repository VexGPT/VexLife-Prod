#!/bin/bash
# [VXG RealForever]
set -euo pipefail

HARNESS_REPO='VexGPT/VexLife-Prod'
HARNESS_HEAD='c03d024a398d8f549dad94ac6eba14ae94cf0f09'
HARNESS_TREE='c1664e9466f2b2151c5f98a9914b7513bdc6d536'
README_BLOB='f45dc4976903ea266b4122f4b8c5b46f69e8b394'
PAYLOAD_BLOB='6b6847816cef9416cdb6b5739f4303ec53c33240'
FORMATION_BLOB='d5dbf0addab2b8d7756a8a2ff6d9353d71223539'
RUN_BLOB='8a594b5d38fcf19e006250090a1b1b08f2c9731a'

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

GH="$(find_exe /opt/homebrew/bin/gh /usr/local/bin/gh)" || {
  printf '%s\n' 'PR543 DCP-1 lived A005 bootstrap: GitHub CLI unavailable.' >&2
  exit 20
}
GIT=/usr/bin/git
WORK="$(/usr/bin/mktemp -d "$HOME/Downloads/.vexlife-pr543-dcp1-lived-a005-bootstrap.XXXXXX")"
cleanup() {
  case "$WORK" in
    "$HOME"/Downloads/.vexlife-pr543-dcp1-lived-a005-bootstrap.*)
      /bin/rm -rf -- "$WORK"
      ;;
  esac
}
trap cleanup EXIT HUP INT TERM

"$GH" auth status --hostname github.com >/dev/null 2>&1 || {
  printf '%s\n' 'PR543 DCP-1 lived A005 bootstrap: GitHub authentication unavailable.' >&2
  exit 21
}

REPO="$WORK/VexLife-Prod"
printf '%s\n' '[PR543 lived A005] acquiring frozen complete DCP-1 harness...'
"$GH" repo clone "$HARNESS_REPO" "$REPO" -- --filter=blob:none --no-checkout >/dev/null
"$GIT" -C "$REPO" checkout --quiet --detach "$HARNESS_HEAD"
[ "$("$GIT" -C "$REPO" rev-parse HEAD)" = "$HARNESS_HEAD" ] || { printf '%s\n' 'Harness head mismatch.' >&2; exit 22; }
[ "$("$GIT" -C "$REPO" rev-parse 'HEAD^{tree}')" = "$HARNESS_TREE" ] || { printf '%s\n' 'Harness tree mismatch.' >&2; exit 23; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr543-lived/README.md")" = "$README_BLOB" ] || { printf '%s\n' 'README blob mismatch.' >&2; exit 24; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr543-lived/payload-A005.mjs")" = "$PAYLOAD_BLOB" ] || { printf '%s\n' 'Payload blob mismatch.' >&2; exit 25; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr543-lived/formation-A005.mjs")" = "$FORMATION_BLOB" ] || { printf '%s\n' 'Formation blob mismatch.' >&2; exit 26; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr543-lived/RUN-LIVED-A005.command")" = "$RUN_BLOB" ] || { printf '%s\n' 'Runner blob mismatch.' >&2; exit 27; }
[ -z "$("$GIT" -C "$REPO" status --porcelain=v1 -uall)" ] || { printf '%s\n' 'Harness checkout dirty.' >&2; exit 28; }

printf '%s\n' '[PR543 lived A005] frozen complete harness verified; entering DCP-1 source-gated lived proof.'
/bin/bash "$REPO/tools/vexrelay/pr543-lived/RUN-LIVED-A005.command"
