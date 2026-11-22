#!/bin/bash
# [VXG RealForever]
set -euo pipefail

HARNESS_REPO='VexGPT/VexLife-Prod'
HARNESS_HEAD='f7fd4a2d2b4ddf174d3134086cc6910b647b4ae6'
HARNESS_TREE='ebcfb9e4a2ec118dd4a65e70bd20fb78b17f57a8'
README_BLOB='20fd0ebd99a8f7b65869d4baf44649bd0dddb798'
FORMATION_BLOB='3b8bfac8d202c00a381403a11552d825bddf4713'
RUN_BLOB='4876aa5940b41f1691f0f673b9e04d8f0c330da9'
FORMZIP_BLOB='64621f788bc7e1ee67b2f220f4bc55d2f4e3124b'

find_exe(){ for candidate in "$@"; do [ -x "$candidate" ] && { printf '%s\n' "$candidate"; return 0; }; done; return 1; }
GH="$(find_exe /opt/homebrew/bin/gh /usr/local/bin/gh)" || { echo 'PR525 A001 bootstrap: GitHub CLI not found.' >&2; exit 20; }
GIT=/usr/bin/git
WORK="$(/usr/bin/mktemp -d "$HOME/Downloads/.vexlife-pr525-source-owner-a001-bootstrap.XXXXXX")"
cleanup(){ case "$WORK" in "$HOME"/Downloads/.vexlife-pr525-source-owner-a001-bootstrap.*) /bin/rm -rf -- "$WORK" ;; esac; }
trap cleanup EXIT HUP INT TERM

"$GH" auth status --hostname github.com >/dev/null 2>&1 || { echo 'PR525 A001 bootstrap: GitHub authentication unavailable.' >&2; exit 21; }
REPO="$WORK/VexLife-Prod"
printf '%s\n' '[PR525] acquiring frozen SourceOwner A001 harness...'
"$GH" repo clone "$HARNESS_REPO" "$REPO" -- --filter=blob:none --no-checkout >/dev/null
"$GIT" -C "$REPO" checkout --quiet --detach "$HARNESS_HEAD"
[ "$("$GIT" -C "$REPO" rev-parse HEAD)" = "$HARNESS_HEAD" ] || { echo 'Harness head mismatch.' >&2; exit 22; }
[ "$("$GIT" -C "$REPO" rev-parse 'HEAD^{tree}')" = "$HARNESS_TREE" ] || { echo 'Harness tree mismatch.' >&2; exit 23; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr525/README.md")" = "$README_BLOB" ] || { echo 'README blob mismatch.' >&2; exit 24; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr525/formation-A001.mjs")" = "$FORMATION_BLOB" ] || { echo 'Formation blob mismatch.' >&2; exit 25; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr525/RUN-SOURCE-OWNER-A001.command")" = "$RUN_BLOB" ] || { echo 'Runner blob mismatch.' >&2; exit 26; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr525/FORM-DOWNLOAD-ZIP.py")" = "$FORMZIP_BLOB" ] || { echo 'Packet former blob mismatch.' >&2; exit 27; }
[ -z "$("$GIT" -C "$REPO" status --porcelain=v1 -uall)" ] || { echo 'Harness checkout dirty.' >&2; exit 28; }

printf '%s\n' '[PR525] frozen A001 harness verified; entering SourceOwner proof.'
/bin/bash "$REPO/tools/vexrelay/pr525/RUN-SOURCE-OWNER-A001.command"
