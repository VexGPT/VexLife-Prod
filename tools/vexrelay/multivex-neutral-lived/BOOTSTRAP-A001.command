#!/bin/bash
# [VXG RealForever]
set -euo pipefail

HARNESS_REPO='VexGPT/VexLife-Prod'
HARNESS_HEAD='b1f28c4c8aa15f4797d08ab1ca40b050b083594a'
HARNESS_TREE='c1fefb150941eac99a7ce7d87c11e4f3f3afdd38'
README_BLOB='45c4f39154ae2fc9b60143ae057b6f02e6887cd4'
PAYLOAD_BLOB='8d35b046d2f459d653e4e8792509e4aaf8cbeb94'
FORMATION_BLOB='d101a78d222d6bd9f7ae21e5a97be9e1414649b7'
RUN_BLOB='8b852e73984a7967539550b49745d92eb77c937b'

find_exe() {
  local candidate
  for candidate in "$@"; do
    if [ -x "$candidate" ] && [ -f "$candidate" ]; then
      printf '%s
' "$candidate"
      return 0
    fi
  done
  return 1
}

GH="$(find_exe /opt/homebrew/bin/gh /usr/local/bin/gh)" || {
  printf '%s
' '[Multi-Vex neutral A001] GitHub CLI unavailable.' >&2
  exit 20
}
GIT=/usr/bin/git
WORK="$(/usr/bin/mktemp -d "$HOME/Downloads/.vexlife-multivex-neutral-a001-bootstrap.XXXXXX")"
cleanup() {
  case "$WORK" in
    "$HOME"/Downloads/.vexlife-multivex-neutral-a001-bootstrap.*)
      /bin/rm -rf -- "$WORK"
      ;;
  esac
}
trap cleanup EXIT HUP INT TERM

"$GH" auth status --hostname github.com >/dev/null 2>&1 || {
  printf '%s
' '[Multi-Vex neutral A001] GitHub authentication unavailable.' >&2
  exit 21
}

REPO="$WORK/VexLife-Prod"
printf '%s
' '[Multi-Vex neutral A001] acquiring frozen simplified lived harness...'
"$GH" repo clone "$HARNESS_REPO" "$REPO" -- --filter=blob:none --no-checkout >/dev/null
"$GIT" -C "$REPO" checkout --quiet --detach "$HARNESS_HEAD"

[ "$("$GIT" -C "$REPO" rev-parse HEAD)" = "$HARNESS_HEAD" ] || { printf '%s
' 'Harness head mismatch.' >&2; exit 22; }
[ "$("$GIT" -C "$REPO" rev-parse 'HEAD^{tree}')" = "$HARNESS_TREE" ] || { printf '%s
' 'Harness tree mismatch.' >&2; exit 23; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/multivex-neutral-lived/README.md")" = "$README_BLOB" ] || { printf '%s
' 'README blob mismatch.' >&2; exit 24; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/multivex-neutral-lived/payload-A001.mjs")" = "$PAYLOAD_BLOB" ] || { printf '%s
' 'Payload blob mismatch.' >&2; exit 25; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/multivex-neutral-lived/formation-A001.mjs")" = "$FORMATION_BLOB" ] || { printf '%s
' 'Formation blob mismatch.' >&2; exit 26; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/multivex-neutral-lived/RUN-A001.command")" = "$RUN_BLOB" ] || { printf '%s
' 'Runner blob mismatch.' >&2; exit 27; }
[ -z "$("$GIT" -C "$REPO" status --porcelain=v1 -uall)" ] || { printf '%s
' 'Harness checkout dirty.' >&2; exit 28; }

printf '%s
' '[Multi-Vex neutral A001] frozen harness verified; entering simplified adaptive lived encounter.'
/bin/bash "$REPO/tools/vexrelay/multivex-neutral-lived/RUN-A001.command"
