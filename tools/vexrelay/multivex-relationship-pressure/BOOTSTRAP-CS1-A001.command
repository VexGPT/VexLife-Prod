#!/bin/bash
# [VXG RealForever]
set -euo pipefail

HARNESS_REPO='VexGPT/VexLife-Prod'
HARNESS_HEAD='adcf01351950a3dde412a4da27ce2c5c4ffd634c'
HARNESS_TREE='134f96a0b38a045c24429eb42e51aab352a16821'
README_BLOB='e81ab2078dbae5c2662e56f9f8e9b25c13930511'
PAYLOAD_BLOB='aa9080ae70ec47a5f4b9e894a0e25c64686ac8e3'
FORMATION_BLOB='46d7972b497e686a6d2f03044d7d32226fb16a87'
RUN_BLOB='139695a9428d35347dffc2ed807167d1e66996cf'

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

GH="$(find_exe /opt/homebrew/bin/gh /usr/local/bin/gh)" || { echo '[Multi-Vex owner-boundary CS1 A001] GitHub CLI unavailable.' >&2; exit 20; }
GIT=/usr/bin/git
WORK="$(/usr/bin/mktemp -d "$HOME/Downloads/.vexlife-multivex-relationship-pressure-cs1-a001-bootstrap.XXXXXX")"
cleanup() {
  case "$WORK" in
    "$HOME"/Downloads/.vexlife-multivex-relationship-pressure-cs1-a001-bootstrap.*)
      /bin/rm -rf -- "$WORK"
      ;;
  esac
}
trap cleanup EXIT HUP INT TERM

"$GH" auth status --hostname github.com >/dev/null 2>&1 || { echo '[Multi-Vex owner-boundary CS1 A001] GitHub authentication unavailable.' >&2; exit 21; }

REPO="$WORK/VexLife-Prod"
echo '[Multi-Vex owner-boundary CS1 A001] acquiring frozen OB1 explicit owner-boundary discriminator harness...'
"$GH" repo clone "$HARNESS_REPO" "$REPO" -- --filter=blob:none --no-checkout >/dev/null
"$GIT" -C "$REPO" checkout --quiet --detach "$HARNESS_HEAD"

[ "$("$GIT" -C "$REPO" rev-parse HEAD)" = "$HARNESS_HEAD" ] || { echo 'Harness head mismatch.' >&2; exit 22; }
[ "$("$GIT" -C "$REPO" rev-parse 'HEAD^{tree}')" = "$HARNESS_TREE" ] || { echo 'Harness tree mismatch.' >&2; exit 23; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/multivex-relationship-pressure/README.md")" = "$README_BLOB" ] || { echo 'README blob mismatch.' >&2; exit 24; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/multivex-relationship-pressure/payload-CS1-A001.mjs")" = "$PAYLOAD_BLOB" ] || { echo 'Payload blob mismatch.' >&2; exit 25; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/multivex-relationship-pressure/formation-CS1-A001.mjs")" = "$FORMATION_BLOB" ] || { echo 'Formation blob mismatch.' >&2; exit 26; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/multivex-relationship-pressure/RUN-CS1-A001.command")" = "$RUN_BLOB" ] || { echo 'Runner blob mismatch.' >&2; exit 27; }
[ -z "$("$GIT" -C "$REPO" status --porcelain=v1 -uall)" ] || { echo 'Harness checkout dirty.' >&2; exit 28; }
/bin/bash -n "$REPO/tools/vexrelay/multivex-relationship-pressure/RUN-CS1-A001.command" || { echo 'Runner syntax invalid.' >&2; exit 29; }
NODE="$(find_exe /opt/homebrew/bin/node /usr/local/bin/node /usr/bin/node)" || { echo 'Node unavailable for harness syntax check.' >&2; exit 30; }
"$NODE" --check "$REPO/tools/vexrelay/multivex-relationship-pressure/formation-CS1-A001.mjs" || { echo 'Formation syntax invalid.' >&2; exit 31; }
"$NODE" --check "$REPO/tools/vexrelay/multivex-relationship-pressure/payload-CS1-A001.mjs" || { echo 'Payload syntax invalid.' >&2; exit 32; }

echo '[Multi-Vex owner-boundary CS1 A001] frozen harness verified; entering OB1 explicit owner-boundary discriminator under exact Foundation v0.6.20 without changing Product cache settings.'
/bin/bash "$REPO/tools/vexrelay/multivex-relationship-pressure/RUN-CS1-A001.command"
