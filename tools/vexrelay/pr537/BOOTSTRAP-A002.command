#!/bin/bash
# [VXG RealForever]
set -euo pipefail

HARNESS_REPO='VexGPT/VexLife-Prod'
HARNESS_HEAD='4abba8e0dbb934543e025f2c2a0490cee8910f28'
HARNESS_TREE='a375b3b4b9410499b3fc02a346ca9a670d47bc9b'
README_BLOB='d95a238e2c79ccb7b7c1b380abf9c74874489e43'
FORMATION_BLOB='65568871e4ea024a13a044bc553ea10c7db81b7f'
RUN_BLOB='715561193daacd35d955e002969ace743501a0f3'

find_exe(){ for candidate in "$@"; do [ -x "$candidate" ] && { printf '%s\n' "$candidate"; return 0; }; done; return 1; }
GH="$(find_exe /opt/homebrew/bin/gh /usr/local/bin/gh)" || { echo 'PR537 A002 bootstrap: GitHub CLI not found.' >&2; exit 20; }
GIT=/usr/bin/git
WORK="$(/usr/bin/mktemp -d "$HOME/Downloads/.vexlife-pr537-source-owner-a002-bootstrap.XXXXXX")"
cleanup(){ case "$WORK" in "$HOME"/Downloads/.vexlife-pr537-source-owner-a002-bootstrap.*) /bin/rm -rf -- "$WORK" ;; esac; }
trap cleanup EXIT HUP INT TERM

"$GH" auth status --hostname github.com >/dev/null 2>&1 || {
  echo 'PR537 A002 bootstrap: GitHub authentication unavailable.' >&2
  exit 21
}
REPO="$WORK/VexLife-Prod"
printf '%s\n' '[PR537] acquiring frozen SourceOwner A002 harness...'
"$GH" repo clone "$HARNESS_REPO" "$REPO" -- --filter=blob:none --no-checkout >/dev/null
"$GIT" -C "$REPO" checkout --quiet --detach "$HARNESS_HEAD"
[ "$("$GIT" -C "$REPO" rev-parse HEAD)" = "$HARNESS_HEAD" ] || { echo 'Harness head mismatch.' >&2; exit 22; }
[ "$("$GIT" -C "$REPO" rev-parse 'HEAD^{tree}')" = "$HARNESS_TREE" ] || { echo 'Harness tree mismatch.' >&2; exit 23; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr537/README.md")" = "$README_BLOB" ] || { echo 'README blob mismatch.' >&2; exit 24; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr537/formation-A002.mjs")" = "$FORMATION_BLOB" ] || { echo 'Formation blob mismatch.' >&2; exit 25; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr537/RUN-SOURCE-OWNER-A002.command")" = "$RUN_BLOB" ] || { echo 'Runner blob mismatch.' >&2; exit 26; }
[ -z "$("$GIT" -C "$REPO" status --porcelain=v1 -uall)" ] || { echo 'Harness checkout dirty.' >&2; exit 27; }

printf '%s\n' '[PR537] frozen A002 harness verified; entering Participant Identity SourceOwner proof.'
/bin/bash "$REPO/tools/vexrelay/pr537/RUN-SOURCE-OWNER-A002.command"
