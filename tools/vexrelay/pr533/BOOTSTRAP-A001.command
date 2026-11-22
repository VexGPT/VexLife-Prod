#!/bin/bash
# [VXG RealForever]
set -euo pipefail

HARNESS_REPO='VexGPT/VexLife-Prod'
HARNESS_HEAD='0bc1b3691ab4df4672e5ad9f2ee43b84b94645b4'
HARNESS_TREE='0e9622f51c8c94e28bbb5ef2a6766e8c207ce575'
README_BLOB='08786e9cc8838d30f4999071cb63fccc38bb5c34'
FORMATION_BLOB='6acd5975739201f9cddc056bb0d376b1c665b8f4'
RUN_BLOB='ee4b8c56b9505454acf064ec411c13f2355b05ea'
FORMZIP_BLOB='0f2853b52e7a120420338d886adaa57d037bce8a'

find_exe(){ for candidate in "$@"; do [ -x "$candidate" ] && { printf '%s\n' "$candidate"; return 0; }; done; return 1; }
GH="$(find_exe /opt/homebrew/bin/gh /usr/local/bin/gh)" || { echo 'PR533 A001 bootstrap: GitHub CLI not found.' >&2; exit 20; }
GIT=/usr/bin/git
WORK="$(/usr/bin/mktemp -d "$HOME/Downloads/.vexlife-pr533-source-owner-a001-bootstrap.XXXXXX")"
cleanup(){ case "$WORK" in "$HOME"/Downloads/.vexlife-pr533-source-owner-a001-bootstrap.*) /bin/rm -rf -- "$WORK" ;; esac; }
trap cleanup EXIT HUP INT TERM

"$GH" auth status --hostname github.com >/dev/null 2>&1 || { echo 'PR533 A001 bootstrap: GitHub authentication unavailable.' >&2; exit 21; }
REPO="$WORK/VexLife-Prod"
printf '%s\n' '[PR533] acquiring frozen SourceOwner A001 harness...'
"$GH" repo clone "$HARNESS_REPO" "$REPO" -- --filter=blob:none --no-checkout >/dev/null
"$GIT" -C "$REPO" checkout --quiet --detach "$HARNESS_HEAD"
[ "$("$GIT" -C "$REPO" rev-parse HEAD)" = "$HARNESS_HEAD" ] || { echo 'Harness head mismatch.' >&2; exit 22; }
[ "$("$GIT" -C "$REPO" rev-parse 'HEAD^{tree}')" = "$HARNESS_TREE" ] || { echo 'Harness tree mismatch.' >&2; exit 23; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr533/README.md")" = "$README_BLOB" ] || { echo 'README blob mismatch.' >&2; exit 24; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr533/formation-A001.mjs")" = "$FORMATION_BLOB" ] || { echo 'Formation blob mismatch.' >&2; exit 25; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr533/RUN-SOURCE-OWNER-A001.command")" = "$RUN_BLOB" ] || { echo 'Runner blob mismatch.' >&2; exit 26; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr533/FORM-DOWNLOAD-ZIP.py")" = "$FORMZIP_BLOB" ] || { echo 'Packet former blob mismatch.' >&2; exit 27; }
[ -z "$("$GIT" -C "$REPO" status --porcelain=v1 -uall)" ] || { echo 'Harness checkout dirty.' >&2; exit 28; }

printf '%s\n' '[PR533] frozen A001 harness verified; entering SourceOwner proof.'
/bin/bash "$REPO/tools/vexrelay/pr533/RUN-SOURCE-OWNER-A001.command"
