#!/bin/bash
# [VXG RealForever]
set -euo pipefail

HARNESS_REPO='VexGPT/VexLife-Prod'
HARNESS_HEAD='b83c5c42cf5482215b8da1f66555a69f49e28493'
HARNESS_TREE='8b6411bc98cdcb4b25380837f24c4a4deb7bac1e'
README_BLOB='d2ad55cdee06efd74c67e41fdf97f3e3173850e6'
FORMATION_BLOB='982adab2b922aa44c4c77f61e4f3d2c262e85e46'
RUN_BLOB='1a62501f08edde507ecc0509ad8095723d3b0059'
FORMZIP_BLOB='ae9d6069d8d6c18138acd8cc6bad03aa23a8f4ad'

find_exe(){ for candidate in "$@"; do [ -x "$candidate" ] && { printf '%s\n' "$candidate"; return 0; }; done; return 1; }
GH="$(find_exe /opt/homebrew/bin/gh /usr/local/bin/gh)" || { echo 'PR533 A003 bootstrap: GitHub CLI not found.' >&2; exit 20; }
GIT=/usr/bin/git
WORK="$(/usr/bin/mktemp -d "$HOME/Downloads/.vexlife-pr533-source-owner-a003-bootstrap.XXXXXX")"
cleanup(){ case "$WORK" in "$HOME"/Downloads/.vexlife-pr533-source-owner-a003-bootstrap.*) /bin/rm -rf -- "$WORK" ;; esac; }
trap cleanup EXIT HUP INT TERM

"$GH" auth status --hostname github.com >/dev/null 2>&1 || { echo 'PR533 A003 bootstrap: GitHub authentication unavailable.' >&2; exit 21; }
REPO="$WORK/VexLife-Prod"
printf '%s\n' '[PR533] acquiring frozen SourceOwner A003 harness...'
"$GH" repo clone "$HARNESS_REPO" "$REPO" -- --filter=blob:none --no-checkout >/dev/null
"$GIT" -C "$REPO" checkout --quiet --detach "$HARNESS_HEAD"
[ "$("$GIT" -C "$REPO" rev-parse HEAD)" = "$HARNESS_HEAD" ] || { echo 'Harness head mismatch.' >&2; exit 22; }
[ "$("$GIT" -C "$REPO" rev-parse 'HEAD^{tree}')" = "$HARNESS_TREE" ] || { echo 'Harness tree mismatch.' >&2; exit 23; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr533/README.md")" = "$README_BLOB" ] || { echo 'README blob mismatch.' >&2; exit 24; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr533/formation-A003.mjs")" = "$FORMATION_BLOB" ] || { echo 'Formation blob mismatch.' >&2; exit 25; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr533/RUN-SOURCE-OWNER-A003.command")" = "$RUN_BLOB" ] || { echo 'Runner blob mismatch.' >&2; exit 26; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr533/FORM-DOWNLOAD-ZIP.py")" = "$FORMZIP_BLOB" ] || { echo 'Packet former blob mismatch.' >&2; exit 27; }
[ -z "$("$GIT" -C "$REPO" status --porcelain=v1 -uall)" ] || { echo 'Harness checkout dirty.' >&2; exit 28; }

printf '%s\n' '[PR533] frozen A003 harness verified; entering SourceOwner proof.'
/bin/bash "$REPO/tools/vexrelay/pr533/RUN-SOURCE-OWNER-A003.command"
