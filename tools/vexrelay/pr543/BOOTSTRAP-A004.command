#!/bin/bash
# [VXG RealForever]
set -euo pipefail
HARNESS_REPO='VexGPT/VexLife-Prod'
HARNESS_HEAD='c07d652022edb9504219174ecdf61e59e6cf47bc'
HARNESS_TREE='8360e0d605ca0ad35ee4aaea288f470bdbaa6da6'
README_BLOB='d9d9447a541194f482e4f1f4ab2104f25becfbfa'
FORMATION_BLOB='41c431bec11edf661af6d6263dedb65b61e6b42a'
RUN_BLOB='511a14300c7505e0696a002a11b6b7997e6349a6'
find_exe(){ for candidate in "$@"; do [ -x "$candidate" ] && { printf '%s\n' "$candidate"; return 0; }; done; return 1; }
GH="$(find_exe /opt/homebrew/bin/gh /usr/local/bin/gh)" || exit 20
GIT=/usr/bin/git
WORK="$(/usr/bin/mktemp -d "$HOME/Downloads/.vexlife-pr543-source-owner-a004-bootstrap.XXXXXX")"
cleanup(){ case "$WORK" in "$HOME"/Downloads/.vexlife-pr543-source-owner-a004-bootstrap.*) /bin/rm -rf -- "$WORK" ;; esac; }
trap cleanup EXIT HUP INT TERM
"$GH" auth status --hostname github.com >/dev/null 2>&1 || exit 21
REPO="$WORK/VexLife-Prod"
"$GH" repo clone "$HARNESS_REPO" "$REPO" -- --filter=blob:none --no-checkout >/dev/null
"$GIT" -C "$REPO" checkout --quiet --detach "$HARNESS_HEAD"
[ "$("$GIT" -C "$REPO" rev-parse HEAD)" = "$HARNESS_HEAD" ] || exit 22
[ "$("$GIT" -C "$REPO" rev-parse 'HEAD^{tree}')" = "$HARNESS_TREE" ] || exit 23
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr543/README.md")" = "$README_BLOB" ] || exit 24
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr543/formation-A004.mjs")" = "$FORMATION_BLOB" ] || exit 25
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr543/RUN-SOURCE-OWNER-A004.command")" = "$RUN_BLOB" ] || exit 26
/bin/bash "$REPO/tools/vexrelay/pr543/RUN-SOURCE-OWNER-A004.command"
