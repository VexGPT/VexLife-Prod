#!/bin/bash
# [VXG RealForever]
set -euo pipefail

HARNESS_REPO='VexGPT/VexLife-Prod'
HARNESS_HEAD='497a4bf8127da8602facb522208d6cd7898b476c'
HARNESS_TREE='de2d38efc5336e1b8692dd4c4763850c8a56155f'
README_BLOB='e9df913ebae06a3b8b0fc2d2701a3f995e077aa3'
PAYLOAD_BLOB='efc1447b1f829c93a25e19fecbde0f6dfb7c943f'
FORMATION_BLOB='a1e1c2b8990ff741821dfea6032bb389498d5134'
RUN_BLOB='6bae7e5fa04d56e4870f64f212612d278f6329fd'

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
  printf '%s\n' 'PR543 DCP-S1 A003 bootstrap: GitHub CLI unavailable.' >&2
  exit 20
}
GIT=/usr/bin/git
WORK="$(/usr/bin/mktemp -d "$HOME/Downloads/.vexlife-pr543-dcp-s1-a003-bootstrap.XXXXXX")"
cleanup() {
  case "$WORK" in
    "$HOME"/Downloads/.vexlife-pr543-dcp-s1-a003-bootstrap.*)
      /bin/rm -rf -- "$WORK"
      ;;
  esac
}
trap cleanup EXIT HUP INT TERM

"$GH" auth status --hostname github.com >/dev/null 2>&1 || {
  printf '%s\n' 'PR543 DCP-S1 A003 bootstrap: GitHub authentication unavailable.' >&2
  exit 21
}

REPO="$WORK/VexLife-Prod"
printf '%s\n' '[PR543 S1 A003] acquiring frozen corrected reusable-profile harness...'
"$GH" repo clone "$HARNESS_REPO" "$REPO" -- --filter=blob:none --no-checkout >/dev/null
"$GIT" -C "$REPO" checkout --quiet --detach "$HARNESS_HEAD"
[ "$("$GIT" -C "$REPO" rev-parse HEAD)" = "$HARNESS_HEAD" ] || { printf '%s\n' 'Harness head mismatch.' >&2; exit 22; }
[ "$("$GIT" -C "$REPO" rev-parse 'HEAD^{tree}')" = "$HARNESS_TREE" ] || { printf '%s\n' 'Harness tree mismatch.' >&2; exit 23; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr543-profile-s1/README.md")" = "$README_BLOB" ] || { printf '%s\n' 'README blob mismatch.' >&2; exit 24; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr543-profile-s1/payload-S1-A003.mjs")" = "$PAYLOAD_BLOB" ] || { printf '%s\n' 'Payload blob mismatch.' >&2; exit 25; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr543-profile-s1/formation-S1-A003.mjs")" = "$FORMATION_BLOB" ] || { printf '%s\n' 'Formation blob mismatch.' >&2; exit 26; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr543-profile-s1/RUN-S1-A003.command")" = "$RUN_BLOB" ] || { printf '%s\n' 'Runner blob mismatch.' >&2; exit 27; }
[ -z "$("$GIT" -C "$REPO" status --porcelain=v1 -uall)" ] || { printf '%s\n' 'Harness checkout dirty.' >&2; exit 28; }

printf '%s\n' '[PR543 S1 A003] frozen harness verified; entering corrected reusable-profile proof.'
/bin/bash "$REPO/tools/vexrelay/pr543-profile-s1/RUN-S1-A003.command"
