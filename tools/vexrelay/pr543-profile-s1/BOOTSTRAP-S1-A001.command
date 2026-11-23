#!/bin/bash
# [VXG RealForever]
set -euo pipefail

HARNESS_REPO='VexGPT/VexLife-Prod'
HARNESS_HEAD='f214fbebbb1d1ecab8924ce15db53a6e3f579b2b'
HARNESS_TREE='35858c3f536998000e0c71e8070f760eda5ff1ea'
README_BLOB='b523ce38b58a3d37f163aaef5120f98dd7d24a1a'
PAYLOAD_BLOB='799a8099c30ece3775850474228a11b6290f54bc'
FORMATION_BLOB='e5c3c94cff7e62d718adbc7d5292bc8688558b5f'
RUN_BLOB='f1f4af897daf87f562e71aacc086da6cc2a642b4'

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
  printf '%s\n' 'PR543 DCP-S1 bootstrap: GitHub CLI unavailable.' >&2
  exit 20
}
GIT=/usr/bin/git
WORK="$(/usr/bin/mktemp -d "$HOME/Downloads/.vexlife-pr543-dcp-s1-bootstrap.XXXXXX")"
cleanup() {
  case "$WORK" in
    "$HOME"/Downloads/.vexlife-pr543-dcp-s1-bootstrap.*)
      /bin/rm -rf -- "$WORK"
      ;;
  esac
}
trap cleanup EXIT HUP INT TERM

"$GH" auth status --hostname github.com >/dev/null 2>&1 || {
  printf '%s\n' 'PR543 DCP-S1 bootstrap: GitHub authentication unavailable.' >&2
  exit 21
}

REPO="$WORK/VexLife-Prod"
printf '%s\n' '[PR543 S1] acquiring frozen reusable-profile harness...'
"$GH" repo clone "$HARNESS_REPO" "$REPO" -- --filter=blob:none --no-checkout >/dev/null
"$GIT" -C "$REPO" checkout --quiet --detach "$HARNESS_HEAD"
[ "$("$GIT" -C "$REPO" rev-parse HEAD)" = "$HARNESS_HEAD" ] || { printf '%s\n' 'Harness head mismatch.' >&2; exit 22; }
[ "$("$GIT" -C "$REPO" rev-parse 'HEAD^{tree}')" = "$HARNESS_TREE" ] || { printf '%s\n' 'Harness tree mismatch.' >&2; exit 23; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr543-profile-s1/README.md")" = "$README_BLOB" ] || { printf '%s\n' 'README blob mismatch.' >&2; exit 24; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr543-profile-s1/payload-S1-A001.mjs")" = "$PAYLOAD_BLOB" ] || { printf '%s\n' 'Payload blob mismatch.' >&2; exit 25; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr543-profile-s1/formation-S1-A001.mjs")" = "$FORMATION_BLOB" ] || { printf '%s\n' 'Formation blob mismatch.' >&2; exit 26; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr543-profile-s1/RUN-S1-A001.command")" = "$RUN_BLOB" ] || { printf '%s\n' 'Runner blob mismatch.' >&2; exit 27; }
[ -z "$("$GIT" -C "$REPO" status --porcelain=v1 -uall)" ] || { printf '%s\n' 'Harness checkout dirty.' >&2; exit 28; }

printf '%s\n' '[PR543 S1] frozen harness verified; entering reusable-profile inhabitance proof.'
/bin/bash "$REPO/tools/vexrelay/pr543-profile-s1/RUN-S1-A001.command"
