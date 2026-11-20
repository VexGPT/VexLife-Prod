#!/bin/bash
set -euo pipefail

PRODUCT_REPOSITORY='VexGPT/VexLife-Prod'
A004_SOURCE_HEAD='d3304213c741f3b639d81e342cc90f95238eb665'
A004_SOURCE_TREE='e04ab88db638225fd59b65a4e04a7d40e66f799a'
A004_ROOT='relay/web-restoration/build/mac/A004'
README_BLOB='5e4dd1e535a81ae2ae782df218026c3fab2e03a3'
RUN_BLOB='106f3d17e0466f98d20429c6a97ccccd131f2eb7'
FORM_BLOB='e86690f699cbefb1ebaebc6ce136a9b4e2f39e72'
PAYLOAD_BLOB='8d39228520333ed4f901a160721c28ec84c4845c'

resolve_exec() {
  local candidate
  for candidate in "$@"; do
    if [ -x "$candidate" ] && [ -f "$candidate" ]; then
      printf '%s' "$candidate"
      return 0
    fi
  done
  return 1
}

BASH_BIN="$(resolve_exec /bin/bash)" || { echo 'A004 bootstrap: Bash unavailable.' >&2; exit 10; }
GIT_BIN="$(resolve_exec /usr/bin/git)" || { echo 'A004 bootstrap: Git unavailable.' >&2; exit 11; }
GH_BIN="$(resolve_exec /opt/homebrew/bin/gh /usr/local/bin/gh)" || { echo 'A004 bootstrap: GitHub CLI unavailable.' >&2; exit 12; }
MKTEMP_BIN="$(resolve_exec /usr/bin/mktemp)" || { echo 'A004 bootstrap: mktemp unavailable.' >&2; exit 13; }
RM_BIN="$(resolve_exec /bin/rm /usr/bin/rm)" || { echo 'A004 bootstrap: rm unavailable.' >&2; exit 14; }

[ "$(/usr/bin/uname -m)" = 'arm64' ] || { echo 'A004 bootstrap: Apple-silicon arm64 is required.' >&2; exit 15; }
"$GH_BIN" auth status --hostname github.com >/dev/null 2>&1 || {
  echo 'A004 bootstrap: GitHub authentication is required for the private source.' >&2
  echo 'Run: gh auth login --hostname github.com --web' >&2
  exit 16
}

DOWNLOADS="$HOME/Downloads"
ROOT="$("$MKTEMP_BIN" -d "$DOWNLOADS/.vexlife-pr436-java-selection-bootstrap.XXXXXX")"
CHECKOUT="$ROOT/vexlife-prod"
PRESERVE=0

cleanup() {
  if [ "$PRESERVE" -eq 0 ]; then
    case "$ROOT" in
      "$DOWNLOADS"/.vexlife-pr436-java-selection-bootstrap.*) "$RM_BIN" -rf -- "$ROOT" ;;
      *) return 1 ;;
    esac
  else
    echo "A004 bootstrap recovery root preserved: $ROOT" >&2
  fi
}
trap cleanup EXIT HUP INT TERM

printf '%s\n' 'A004 bootstrap: acquiring exact read-only diagnostic source...'
"$GH_BIN" repo clone "$PRODUCT_REPOSITORY" "$CHECKOUT" -- --no-checkout >/dev/null
"$GIT_BIN" -C "$CHECKOUT" checkout --detach "$A004_SOURCE_HEAD" >/dev/null

[ "$("$GIT_BIN" -C "$CHECKOUT" rev-parse HEAD)" = "$A004_SOURCE_HEAD" ] || { echo 'A004 bootstrap: source head mismatch.' >&2; exit 17; }
[ "$("$GIT_BIN" -C "$CHECKOUT" rev-parse 'HEAD^{tree}')" = "$A004_SOURCE_TREE" ] || { echo 'A004 bootstrap: source tree mismatch.' >&2; exit 18; }
[ "$("$GIT_BIN" -C "$CHECKOUT" rev-parse "HEAD:$A004_ROOT/README.md")" = "$README_BLOB" ] || { echo 'A004 bootstrap: README blob mismatch.' >&2; exit 19; }
[ "$("$GIT_BIN" -C "$CHECKOUT" rev-parse "HEAD:$A004_ROOT/RUN-A004.command")" = "$RUN_BLOB" ] || { echo 'A004 bootstrap: runner blob mismatch.' >&2; exit 20; }
[ "$("$GIT_BIN" -C "$CHECKOUT" rev-parse "HEAD:$A004_ROOT/form-task.cjs")" = "$FORM_BLOB" ] || { echo 'A004 bootstrap: task former blob mismatch.' >&2; exit 21; }
[ "$("$GIT_BIN" -C "$CHECKOUT" rev-parse "HEAD:$A004_ROOT/payload.mjs")" = "$PAYLOAD_BLOB" ] || { echo 'A004 bootstrap: payload blob mismatch.' >&2; exit 22; }

PRESERVE=1
set +e
"$BASH_BIN" "$CHECKOUT/$A004_ROOT/RUN-A004.command"
STATUS=$?
set -e
if [ "$STATUS" -ne 0 ]; then
  echo "A004 bootstrap: diagnostic carrier stopped with exit $STATUS." >&2
  exit "$STATUS"
fi

RETURN="$DOWNLOADS/RETURN-VEXLIFE-WEB-RESTORATION-JAVA-SELECTION-A004.zip"
[ -s "$RETURN" ] || { echo 'A004 bootstrap: expected canonical RETURN was not formed.' >&2; exit 23; }

PRESERVE=0
printf '\nA004 bootstrap: canonical RETURN ready:\n%s\n' "$RETURN"
exit 0

# [VXG RealForever]
