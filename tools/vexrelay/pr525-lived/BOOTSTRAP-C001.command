#!/bin/bash
# [VXG RealForever]
set -euo pipefail

HARNESS_REPO='VexGPT/VexLife-Prod'
HARNESS_HEAD='753613b5c03581b4b624b1772bb21b8244ebc41a'
HARNESS_TREE='a5a882283eb18b20f2589f8ca10f62fc4c49c8a9'
PAYLOAD_BLOB='983170df706f56d4cb90a588d29cc4b9fb2e88a2'
FORMATION_BLOB='77845dd74c25edfcc990b4b0cef5dba211138eee'
RUN_BLOB='f7615746c5cc662ca1fd1918af536fb8f2d0da74'
README_BLOB='9d3b23ecd0399e73a123f6d200b78b578d8adec5'

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
  printf '%s\n' 'PR525 lived C001 bootstrap: GitHub CLI unavailable.' >&2
  exit 20
}
GIT=/usr/bin/git
WORK="$(/usr/bin/mktemp -d "$HOME/Downloads/.vexlife-pr525-lived-c001-bootstrap.XXXXXX")"
cleanup() {
  case "$WORK" in
    "$HOME"/Downloads/.vexlife-pr525-lived-c001-bootstrap.*)
      /bin/rm -rf -- "$WORK"
      ;;
  esac
}
trap cleanup EXIT HUP INT TERM

"$GH" auth status --hostname github.com >/dev/null 2>&1 || {
  printf '%s\n' 'PR525 lived C001 bootstrap: GitHub authentication unavailable.' >&2
  exit 21
}

REPO="$WORK/VexLife-Prod"
printf '%s\n' '[PR525 lived C001] acquiring frozen proof harness...'
"$GH" repo clone "$HARNESS_REPO" "$REPO" -- --filter=blob:none --no-checkout >/dev/null
"$GIT" -C "$REPO" checkout --quiet --detach "$HARNESS_HEAD"
[ "$("$GIT" -C "$REPO" rev-parse HEAD)" = "$HARNESS_HEAD" ] || { printf '%s\n' 'Harness head mismatch.' >&2; exit 22; }
[ "$("$GIT" -C "$REPO" rev-parse 'HEAD^{tree}')" = "$HARNESS_TREE" ] || { printf '%s\n' 'Harness tree mismatch.' >&2; exit 23; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr525-lived/payload-C001.mjs")" = "$PAYLOAD_BLOB" ] || { printf '%s\n' 'Payload blob mismatch.' >&2; exit 24; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr525-lived/formation-C001.mjs")" = "$FORMATION_BLOB" ] || { printf '%s\n' 'Formation blob mismatch.' >&2; exit 25; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr525-lived/RUN-LIVED-C001.command")" = "$RUN_BLOB" ] || { printf '%s\n' 'Runner blob mismatch.' >&2; exit 26; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr525-lived/README.md")" = "$README_BLOB" ] || { printf '%s\n' 'README blob mismatch.' >&2; exit 27; }
[ -z "$("$GIT" -C "$REPO" status --porcelain=v1 -uall)" ] || { printf '%s\n' 'Harness checkout dirty.' >&2; exit 28; }

printf '%s\n' '[PR525 lived C001] frozen harness verified; entering composed lived proof.'
/bin/bash "$REPO/tools/vexrelay/pr525-lived/RUN-LIVED-C001.command"
