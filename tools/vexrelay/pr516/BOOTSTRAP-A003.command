#!/bin/bash
# [VXG RealForever]
set -euo pipefail

HARNESS_REPO='VexGPT/VexLife-Prod'
HARNESS_HEAD='e06518087ed504eb3455cd60c8d0dca2cb1a38a4'
HARNESS_TREE='e067ab98e23c1305d4f05ea590923bd8b0570900'
README_BLOB='0343fc622e22cc7ded8aa99160232cc2a82aded6'
FORMATION_BLOB='9c01e97bb4b33d02fa2b3fae7e247ada6115e679'
RUN_BLOB='efc6458c454d867aa1a3fd1265e43a48dac10db9'
FORMZIP_BLOB='7399a319a44f66851f416b111ec47828bf0b4ea6'

find_exe(){ for candidate in "$@"; do [ -x "$candidate" ] && { printf '%s\n' "$candidate"; return 0; }; done; return 1; }
GH="$(find_exe /opt/homebrew/bin/gh /usr/local/bin/gh)" || { echo 'PR516 A003 bootstrap: GitHub CLI not found.' >&2; exit 20; }
GIT=/usr/bin/git
WORK="$(/usr/bin/mktemp -d "$HOME/Downloads/.vexlife-pr516-source-owner-a003-bootstrap.XXXXXX")"
cleanup(){ case "$WORK" in "$HOME"/Downloads/.vexlife-pr516-source-owner-a003-bootstrap.*) /bin/rm -rf -- "$WORK" ;; esac; }
trap cleanup EXIT HUP INT TERM

"$GH" auth status --hostname github.com >/dev/null 2>&1 || { echo 'PR516 A003 bootstrap: GitHub authentication unavailable.' >&2; exit 21; }
REPO="$WORK/VexLife-Prod"
printf '%s\n' '[PR516] acquiring preserved SourceOwner A003 harness...'
"$GH" repo clone "$HARNESS_REPO" "$REPO" -- --filter=blob:none --no-checkout >/dev/null
"$GIT" -C "$REPO" checkout --quiet --detach "$HARNESS_HEAD"
[ "$("$GIT" -C "$REPO" rev-parse HEAD)" = "$HARNESS_HEAD" ] || { echo 'Harness head mismatch.' >&2; exit 22; }
[ "$("$GIT" -C "$REPO" rev-parse 'HEAD^{tree}')" = "$HARNESS_TREE" ] || { echo 'Harness tree mismatch.' >&2; exit 23; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr516/README.md")" = "$README_BLOB" ] || { echo 'README blob mismatch.' >&2; exit 24; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr516/formation-A003.mjs")" = "$FORMATION_BLOB" ] || { echo 'Formation blob mismatch.' >&2; exit 25; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr516/RUN-SOURCE-OWNER-A003.command")" = "$RUN_BLOB" ] || { echo 'Runner blob mismatch.' >&2; exit 26; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr516/FORM-DOWNLOAD-ZIP-A003.py")" = "$FORMZIP_BLOB" ] || { echo 'Packet former blob mismatch.' >&2; exit 27; }
[ -z "$("$GIT" -C "$REPO" status --porcelain=v1 -uall)" ] || { echo 'Harness checkout dirty.' >&2; exit 28; }
printf '%s\n' '[PR516] preserved A003 harness verified; entering repository-only SourceOwner proof.'
/bin/bash "$REPO/tools/vexrelay/pr516/RUN-SOURCE-OWNER-A003.command"
