#!/bin/bash
# [VXG RealForever]
set -euo pipefail

HARNESS_REPO='VexGPT/VexLife-Prod'
HARNESS_BRANCH='companion/pr516-initial-boot-prefix-source-owner-a002'
HARNESS_HEAD='74f3b9040e3f5ad21b30bcb001963947c9bf419e'
HARNESS_TREE='b17cd359b593e8b7f353766e6b6b595ce49710e7'
README_BLOB='8a02054828052de1dccfe8d1cc345a78da8b280f'
FORMATION_BLOB='975bb7fd2335a41b0836bc731ec0d966c12875df'
RUN_BLOB='a24f45dfc2bd3f357589f5492787b8a508089a77'
FORMZIP_BLOB='8af1e47caa9fd4335d796eb3588e318324a838e3'

find_exe() {
  for candidate in "$@"; do
    if [ -x "$candidate" ]; then printf '%s\n' "$candidate"; return 0; fi
  done
  return 1
}
GH="$(find_exe /opt/homebrew/bin/gh /usr/local/bin/gh)" || { printf '%s\n' 'PR516 A002 bootstrap: GitHub CLI not found.' >&2; exit 20; }
GIT=/usr/bin/git
WORK="$(/usr/bin/mktemp -d "$HOME/Downloads/.vexlife-pr516-source-owner-a002-bootstrap.XXXXXX")"
cleanup() {
  case "$WORK" in
    "$HOME"/Downloads/.vexlife-pr516-source-owner-a002-bootstrap.*) /bin/rm -rf -- "$WORK" ;;
  esac
}
trap cleanup EXIT HUP INT TERM

"$GH" auth status --hostname github.com >/dev/null 2>&1 || {
  printf '%s\n' 'PR516 A002 bootstrap: GitHub authentication unavailable.' >&2
  exit 21
}

REPO="$WORK/VexLife-Prod"
printf '%s\n' '[PR516] acquiring preserved SourceOwner A002 harness...'
"$GH" repo clone "$HARNESS_REPO" "$REPO" -- --filter=blob:none --no-checkout >/dev/null
"$GIT" -C "$REPO" fetch --quiet --no-tags origin "$HARNESS_BRANCH"
"$GIT" -C "$REPO" checkout --quiet --detach "$HARNESS_HEAD"

[ "$("$GIT" -C "$REPO" rev-parse HEAD)" = "$HARNESS_HEAD" ] || { printf '%s\n' 'PR516 A002 bootstrap: harness head mismatch.' >&2; exit 22; }
[ "$("$GIT" -C "$REPO" rev-parse 'HEAD^{tree}')" = "$HARNESS_TREE" ] || { printf '%s\n' 'PR516 A002 bootstrap: harness tree mismatch.' >&2; exit 23; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr516/README.md")" = "$README_BLOB" ] || { printf '%s\n' 'PR516 A002 bootstrap: README blob mismatch.' >&2; exit 24; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr516/formation-A002.mjs")" = "$FORMATION_BLOB" ] || { printf '%s\n' 'PR516 A002 bootstrap: formation blob mismatch.' >&2; exit 25; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr516/RUN-SOURCE-OWNER-A002.command")" = "$RUN_BLOB" ] || { printf '%s\n' 'PR516 A002 bootstrap: runner blob mismatch.' >&2; exit 26; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr516/FORM-DOWNLOAD-ZIP.py")" = "$FORMZIP_BLOB" ] || { printf '%s\n' 'PR516 A002 bootstrap: packet former blob mismatch.' >&2; exit 27; }
[ -z "$("$GIT" -C "$REPO" status --porcelain=v1 -uall)" ] || { printf '%s\n' 'PR516 A002 bootstrap: harness checkout is dirty.' >&2; exit 28; }

printf '%s\n' '[PR516] preserved harness verified; entering SourceOwner A002.'
/bin/bash "$REPO/tools/vexrelay/pr516/RUN-SOURCE-OWNER-A002.command"
