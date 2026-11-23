#!/bin/bash
# [VXG RealForever]
set -euo pipefail

HARNESS_REPO='VexGPT/VexLife-Prod'
HARNESS_HEAD='2a67db5b10b9a318834bc2dc555e09aaa99ebf22'
HARNESS_TREE='c241f10b649f3cb4e5667d9de978c96ad2f8f37a'
README_BLOB='07118bdc66ef9a1aed7761048f741e9163f2f4b3'
PAYLOAD_BLOB='26d0d0f4872db1e9b171ec44cea612a8584f59c5'
FORMATION_BLOB='9e511c4715d51c9acde610a06d54de9303bf7f3e'
RUN_BLOB='e2055c6802ea70faf354eb79902584c72bf9cfd3'

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

GH="$(find_exe /opt/homebrew/bin/gh /usr/local/bin/gh)" || { echo '[Multi-Vex neutral A002] GitHub CLI unavailable.' >&2; exit 20; }
GIT=/usr/bin/git
WORK="$(/usr/bin/mktemp -d "$HOME/Downloads/.vexlife-multivex-neutral-a002-bootstrap.XXXXXX")"
cleanup() {
  case "$WORK" in
    "$HOME"/Downloads/.vexlife-multivex-neutral-a002-bootstrap.*)
      /bin/rm -rf -- "$WORK"
      ;;
  esac
}
trap cleanup EXIT HUP INT TERM

"$GH" auth status --hostname github.com >/dev/null 2>&1 || { echo '[Multi-Vex neutral A002] GitHub authentication unavailable.' >&2; exit 21; }

REPO="$WORK/VexLife-Prod"
echo '[Multi-Vex neutral A002] acquiring frozen composed lived harness...'
"$GH" repo clone "$HARNESS_REPO" "$REPO" -- --filter=blob:none --no-checkout >/dev/null
"$GIT" -C "$REPO" checkout --quiet --detach "$HARNESS_HEAD"

[ "$("$GIT" -C "$REPO" rev-parse HEAD)" = "$HARNESS_HEAD" ] || { echo 'Harness head mismatch.' >&2; exit 22; }
[ "$("$GIT" -C "$REPO" rev-parse 'HEAD^{tree}')" = "$HARNESS_TREE" ] || { echo 'Harness tree mismatch.' >&2; exit 23; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/multivex-neutral-lived/README.md")" = "$README_BLOB" ] || { echo 'README blob mismatch.' >&2; exit 24; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/multivex-neutral-lived/payload-A002.mjs")" = "$PAYLOAD_BLOB" ] || { echo 'Payload blob mismatch.' >&2; exit 25; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/multivex-neutral-lived/formation-A002.mjs")" = "$FORMATION_BLOB" ] || { echo 'Formation blob mismatch.' >&2; exit 26; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/multivex-neutral-lived/RUN-A002.command")" = "$RUN_BLOB" ] || { echo 'Runner blob mismatch.' >&2; exit 27; }
[ -z "$("$GIT" -C "$REPO" status --porcelain=v1 -uall)" ] || { echo 'Harness checkout dirty.' >&2; exit 28; }

echo '[Multi-Vex neutral A002] frozen harness verified; entering simplified adaptive lived encounter.'
/bin/bash "$REPO/tools/vexrelay/multivex-neutral-lived/RUN-A002.command"
