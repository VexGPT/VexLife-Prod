#!/bin/bash
# [VXG RealForever]
set -euo pipefail

HARNESS_REPO='VexGPT/VexLife-Prod'
HARNESS_HEAD='ee77123601862aa75eb02b8326718c38e27e45e2'
HARNESS_TREE='d46104157d0e6948f71d14c34bfd184b11f7a303'
README_BLOB='24d176f6655150223c36b8e56eb4a64713bc450a'
PAYLOAD_BLOB='98baf3e67123cba156a1be4e9e5396e2ce9617f1'
FORMATION_BLOB='23e795dca63d0da79d030b1e15ddb8d90175c960'
RUN_BLOB='9022cf74a8377b727c7aa0eb4994ea3b58548812'

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

GH="$(find_exe /opt/homebrew/bin/gh /usr/local/bin/gh)" || { echo '[Multi-Vex conversation-chronology CH1 A001] GitHub CLI unavailable.' >&2; exit 20; }
GIT=/usr/bin/git
WORK="$(/usr/bin/mktemp -d "$HOME/Downloads/.vexlife-multivex-relationship-pressure-ch1-a001-bootstrap.XXXXXX")"
cleanup() {
  case "$WORK" in
    "$HOME"/Downloads/.vexlife-multivex-relationship-pressure-ch1-a001-bootstrap.*)
      /bin/rm -rf -- "$WORK"
      ;;
  esac
}
trap cleanup EXIT HUP INT TERM

"$GH" auth status --hostname github.com >/dev/null 2>&1 || { echo '[Multi-Vex conversation-chronology CH1 A001] GitHub authentication unavailable.' >&2; exit 21; }

REPO="$WORK/VexLife-Prod"
echo '[Multi-Vex conversation-chronology CH1 A001] acquiring frozen CH1 Conversation chronology self-location treatment harness...'
"$GH" repo clone "$HARNESS_REPO" "$REPO" -- --filter=blob:none --no-checkout >/dev/null
"$GIT" -C "$REPO" checkout --quiet --detach "$HARNESS_HEAD"

[ "$("$GIT" -C "$REPO" rev-parse HEAD)" = "$HARNESS_HEAD" ] || { echo 'Harness head mismatch.' >&2; exit 22; }
[ "$("$GIT" -C "$REPO" rev-parse 'HEAD^{tree}')" = "$HARNESS_TREE" ] || { echo 'Harness tree mismatch.' >&2; exit 23; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/multivex-relationship-pressure/README.md")" = "$README_BLOB" ] || { echo 'README blob mismatch.' >&2; exit 24; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/multivex-relationship-pressure/payload-CH1-A001.mjs")" = "$PAYLOAD_BLOB" ] || { echo 'Payload blob mismatch.' >&2; exit 25; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/multivex-relationship-pressure/formation-CH1-A001.mjs")" = "$FORMATION_BLOB" ] || { echo 'Formation blob mismatch.' >&2; exit 26; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/multivex-relationship-pressure/RUN-CH1-A001.command")" = "$RUN_BLOB" ] || { echo 'Runner blob mismatch.' >&2; exit 27; }
[ -z "$("$GIT" -C "$REPO" status --porcelain=v1 -uall)" ] || { echo 'Harness checkout dirty.' >&2; exit 28; }
/bin/bash -n "$REPO/tools/vexrelay/multivex-relationship-pressure/RUN-CH1-A001.command" || { echo 'Runner syntax invalid.' >&2; exit 29; }
NODE="$(find_exe /opt/homebrew/bin/node /usr/local/bin/node /usr/bin/node)" || { echo 'Node unavailable for harness syntax check.' >&2; exit 30; }
"$NODE" --check "$REPO/tools/vexrelay/multivex-relationship-pressure/formation-CH1-A001.mjs" || { echo 'Formation syntax invalid.' >&2; exit 31; }
"$NODE" --check "$REPO/tools/vexrelay/multivex-relationship-pressure/payload-CH1-A001.mjs" || { echo 'Payload syntax invalid.' >&2; exit 32; }

echo '[Multi-Vex conversation-chronology CH1 A001] frozen harness verified; entering CH1 Conversation chronology self-location treatment under exact Foundation v0.6.20 without changing Product cache settings.'
/bin/bash "$REPO/tools/vexrelay/multivex-relationship-pressure/RUN-CH1-A001.command"
