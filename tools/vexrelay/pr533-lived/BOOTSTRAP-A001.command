#!/bin/bash
# [VXG RealForever]
set -euo pipefail

HARNESS_REPO='VexGPT/VexLife-Prod'
HARNESS_HEAD='51ea188fc8777749ea2b27889a3407ec9274eb98'
HARNESS_TREE='6a39b47cea2bd1149aef319581b3cd9930877521'
PAYLOAD_BLOB='7935b27ff6aa887d2450d95d85754ba7fc84bf4e'
FORMATION_BLOB='2461b47634013028ef0fc37c3a9ace005e871333'
RUN_BLOB='63b7cdcb95cb1a04ec65b143b66953963dec9686'
README_BLOB='f36a99be4f7243bb6856ab5d935dc005d9a403b2'

find_exe(){
  for candidate in "$@"; do
    if [ -x "$candidate" ] && [ -f "$candidate" ]; then printf '%s\n' "$candidate"; return 0; fi
  done
  return 1
}
GH="$(find_exe /opt/homebrew/bin/gh /usr/local/bin/gh)" || { echo 'PR533 lived bootstrap: GitHub CLI not found.' >&2; exit 20; }
GIT=/usr/bin/git
WORK="$(/usr/bin/mktemp -d "$HOME/Downloads/.vexlife-pr533-lived-visual-a001-bootstrap.XXXXXX")"
cleanup(){ case "$WORK" in "$HOME"/Downloads/.vexlife-pr533-lived-visual-a001-bootstrap.*) /bin/rm -rf -- "$WORK" ;; esac; }
trap cleanup EXIT HUP INT TERM

"$GH" auth status --hostname github.com >/dev/null 2>&1 || { echo 'PR533 lived bootstrap: GitHub authentication unavailable.' >&2; exit 21; }
REPO="$WORK/VexLife-Prod"
printf '%s\n' '[PR533 lived] acquiring frozen visual-preview harness...'
"$GH" repo clone "$HARNESS_REPO" "$REPO" -- --filter=blob:none --no-checkout >/dev/null
"$GIT" -C "$REPO" checkout --quiet --detach "$HARNESS_HEAD"
[ "$("$GIT" -C "$REPO" rev-parse HEAD)" = "$HARNESS_HEAD" ] || { echo 'Harness head mismatch.' >&2; exit 22; }
[ "$("$GIT" -C "$REPO" rev-parse 'HEAD^{tree}')" = "$HARNESS_TREE" ] || { echo 'Harness tree mismatch.' >&2; exit 23; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr533-lived/payload-A001.mjs")" = "$PAYLOAD_BLOB" ] || { echo 'Payload blob mismatch.' >&2; exit 24; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr533-lived/formation-A001.mjs")" = "$FORMATION_BLOB" ] || { echo 'Formation blob mismatch.' >&2; exit 25; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr533-lived/RUN-LIVED-A001.command")" = "$RUN_BLOB" ] || { echo 'Runner blob mismatch.' >&2; exit 26; }
[ "$("$GIT" -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr533-lived/README.md")" = "$README_BLOB" ] || { echo 'README blob mismatch.' >&2; exit 27; }
[ -z "$("$GIT" -C "$REPO" status --porcelain=v1 -uall)" ] || { echo 'Harness checkout dirty.' >&2; exit 28; }

printf '%s\n' '[PR533 lived] frozen harness verified; entering visual preview.'
/bin/bash "$REPO/tools/vexrelay/pr533-lived/RUN-LIVED-A001.command"
