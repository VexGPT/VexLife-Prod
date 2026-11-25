#!/bin/bash
# [VXG RealForever]
set -euo pipefail

HARNESS_REPO='VexGPT/VexLife-Prod'
HARNESS_HEAD='cd557adc1587a0ed14fc19868201bcceb269347f'
HARNESS_TREE='dfea101dbca8b335ffb174d3575f303cb27e592f'
README_BLOB='f16d97fddf729978cdbbcb4fee450f7474626b3e'
FORMATION_BLOB='596f014a85c3ff57d8bd6e53afa764fe77196ac3'
RUN_BLOB='9a3298a38f5c88b3fbc959fdb7e5f66c2ddad9e9'

find_exe(){ for candidate in "$@"; do [ -x "$candidate" ] && { printf '%s\n' "$candidate"; return 0; }; done; return 1; }
GH="$(find_exe /opt/homebrew/bin/gh /usr/local/bin/gh)" || { echo 'PR537 A003 bootstrap: GitHub CLI not found.' >&2; exit 20; }
GIT=/usr/bin/git
WORK="$(/usr/bin/mktemp -d "$HOME/Downloads/.vexlife-pr537-source-owner-a003-bootstrap.XXXXXX")"
cleanup(){ case "$WORK" in "$HOME"/Downloads/.vexlife-pr537-source-owner-a003-bootstrap.*) /bin/rm -rf -- "$WORK" ;; esac; }
trap cleanup EXIT HUP INT TERM

"$GH" auth status --hostname github.com >/dev/null 2>&1 || {
  echo 'PR537 A003 bootstrap: GitHub authentication unavailable.' >&2
  exit 21
}
REPO="$WORK/VexLife-Prod"
printf '%s\n' '[PR537] acquiring frozen SourceOwner A003 harness...'
"$GH" repo clone "$HARNESS_REPO" "$REPO" -- --filter=blob:none --no-checkout >/dev/null
"$GIT" -C "$REPO" checkout --quiet --detach "$HARNESS_HEAD"
[ "$("$GIT" -C "$REPO" rev-parse HEAD)" = "$HARNESS_HEAD" ] || { echo 'Harness head mismatch.' >&2; exit 22; }
[ "$("$GIT" -C "$REPO" rev-parse 'HEAD^{tree}')" = "$HARNESS_TREE" ] || { echo 'Harness tree mismatch.' >&2; exit 23; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr537/README.md")" = "$README_BLOB" ] || { echo 'README blob mismatch.' >&2; exit 24; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr537/formation-A003.mjs")" = "$FORMATION_BLOB" ] || { echo 'Formation blob mismatch.' >&2; exit 25; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr537/RUN-SOURCE-OWNER-A003.command")" = "$RUN_BLOB" ] || { echo 'Runner blob mismatch.' >&2; exit 26; }
[ -z "$("$GIT" -C "$REPO" status --porcelain=v1 -uall)" ] || { echo 'Harness checkout dirty.' >&2; exit 27; }

printf '%s\n' '[PR537] frozen A003 harness verified; entering Participant Identity SourceOwner proof.'
/bin/bash "$REPO/tools/vexrelay/pr537/RUN-SOURCE-OWNER-A003.command"
