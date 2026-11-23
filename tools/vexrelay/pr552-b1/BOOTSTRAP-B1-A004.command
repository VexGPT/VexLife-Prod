#!/bin/bash
# [VXG RealForever]
set -euo pipefail

HARNESS_REPO='VexGPT/VexLife-Prod'
HARNESS_HEAD='4231ae6ac4660c635b31ecf70e44dee796903815'
HARNESS_TREE='af8596f954739dcc25f2120395200a80e31aebd9'
README_BLOB='2e47d6b4ed714a28a29a47eabdb748cf7bf87445'
PAYLOAD_BLOB='5b6568bb3cc25f852c000e4d09b09dcb25d81765'
FORMATION_BLOB='59c46cfc1c58ac856ee7930036ccee456d4dfcb4'
RUN_BLOB='2749bd4fd5aeca113d262ca45cbb1e91ed2fffff'

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
  printf '%s\n' 'PR552 DCP-B1 A004 bootstrap: GitHub CLI unavailable.' >&2
  exit 20
}
GIT=/usr/bin/git
WORK="$(/usr/bin/mktemp -d "$HOME/Downloads/.vexlife-pr552-dcp-b1-a004-bootstrap.XXXXXX")"
cleanup() {
  case "$WORK" in
    "$HOME"/Downloads/.vexlife-pr552-dcp-b1-a004-bootstrap.*)
      /bin/rm -rf -- "$WORK"
      ;;
  esac
}
trap cleanup EXIT HUP INT TERM

"$GH" auth status --hostname github.com >/dev/null 2>&1 || {
  printf '%s\n' 'PR552 DCP-B1 A004 bootstrap: GitHub authentication unavailable.' >&2
  exit 21
}

REPO="$WORK/VexLife-Prod"
printf '%s\n' '[PR552 B1 A004] acquiring frozen carried-kernel-current treatment comparison harness...'
"$GH" repo clone "$HARNESS_REPO" "$REPO" -- --filter=blob:none --no-checkout >/dev/null
"$GIT" -C "$REPO" checkout --quiet --detach "$HARNESS_HEAD"
[ "$("$GIT" -C "$REPO" rev-parse HEAD)" = "$HARNESS_HEAD" ] || { printf '%s\n' 'Harness head mismatch.' >&2; exit 22; }
[ "$("$GIT" -C "$REPO" rev-parse 'HEAD^{tree}')" = "$HARNESS_TREE" ] || { printf '%s\n' 'Harness tree mismatch.' >&2; exit 23; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr552-b1/README.md")" = "$README_BLOB" ] || { printf '%s\n' 'README blob mismatch.' >&2; exit 24; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr552-b1/payload-B1-A004.mjs")" = "$PAYLOAD_BLOB" ] || { printf '%s\n' 'Payload blob mismatch.' >&2; exit 25; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr552-b1/formation-B1-A004.mjs")" = "$FORMATION_BLOB" ] || { printf '%s\n' 'Formation blob mismatch.' >&2; exit 26; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr552-b1/RUN-B1-A004.command")" = "$RUN_BLOB" ] || { printf '%s\n' 'Runner blob mismatch.' >&2; exit 27; }
[ -z "$("$GIT" -C "$REPO" status --porcelain=v1 -uall)" ] || { printf '%s\n' 'Harness checkout dirty.' >&2; exit 28; }

printf '%s\n' '[PR552 B1 A004] frozen harness verified; entering carried-kernel-current Foundation-treatment comparison.'
/bin/bash "$REPO/tools/vexrelay/pr552-b1/RUN-B1-A004.command"
