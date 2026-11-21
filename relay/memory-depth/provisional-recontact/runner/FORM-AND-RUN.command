#!/bin/bash
set -euo pipefail

CONTEXT_ROOT="$(cd "$(dirname "$0")/.." && pwd -P)"
REPO_ROOT="$(cd "$CONTEXT_ROOT/../../.." && pwd -P)"
COMPANION_PR="472"
COMPANION_REPO="VexGPT/VexLife-Prod"
COMPANION_BRANCH="VXG-112126-chatgpt-memory-depth-provisional-recontact-companion-a001"

SDK_HEAD="6f8f6f50467f5df5a542713f7dc25b82db4ac8fa"
SDK_TREE="96795640b01f1969da980f52d5a8ec136a8c44aa"
CONTRACT_BLOB="b53cbac01c2deeb45fad5c32ce98e0796c04126f"
SDK_REPOSITORY="vgong24/Vextreme-SDK"

resolve_exec() {
  local c
  for c in "$@"; do
    if [ -x "$c" ] && [ -f "$c" ]; then printf '%s' "$c"; return 0; fi
  done
  return 1
}

GH_BIN="$(resolve_exec /opt/homebrew/bin/gh /usr/local/bin/gh)" || { echo 'Memory Depth companion: GitHub CLI unavailable.' >&2; exit 10; }
NODE_BIN="$(resolve_exec /opt/homebrew/bin/node /usr/local/bin/node /usr/bin/node)" || { echo 'Memory Depth companion: Node unavailable.' >&2; exit 11; }
GIT_BIN=/usr/bin/git
SHASUM_BIN=/usr/bin/shasum
CUT_BIN=/usr/bin/cut
MKTEMP_BIN=/usr/bin/mktemp
RM_BIN=/bin/rm

"$GH_BIN" auth status --hostname github.com >/dev/null 2>&1 || { echo 'Memory Depth companion: GitHub authentication unavailable.' >&2; exit 12; }

REMOTE_HEAD="$("$GH_BIN" pr view "$COMPANION_PR" --repo "$COMPANION_REPO" --json headRefOid --jq .headRefOid)"
LOCAL_HEAD="$("$GIT_BIN" -C "$REPO_ROOT" rev-parse HEAD)"
[ "$LOCAL_HEAD" = "$REMOTE_HEAD" ] || { echo "Memory Depth companion: local head $LOCAL_HEAD is not PR #$COMPANION_PR head $REMOTE_HEAD." >&2; exit 13; }
[ -z "$("$GIT_BIN" -C "$REPO_ROOT" status --porcelain=v1 -uall)" ] || { echo 'Memory Depth companion: checkout is dirty.' >&2; exit 14; }

TMP="$("$MKTEMP_BIN" -d "$HOME/Downloads/.vexlife-memory-depth-companion-sdk.XXXXXX")"
cleanup() { case "$TMP" in "$HOME/Downloads"/.vexlife-memory-depth-companion-sdk.*) "$RM_BIN" -rf -- "$TMP" ;; *) return 1 ;; esac; }
trap cleanup EXIT HUP INT TERM
SDK="$TMP/Vextreme-SDK"

printf '%s\n' 'Memory Depth companion: dedicated experiment window active.'
printf '%s\n' 'Please do not start/restart VexLife or run another VexLife host lane until the RETURN is printed.'
printf '%s\n' 'If currentness changes anyway, the RETURN will be treated as shared-host interference evidence before any runtime-failure diagnosis.'
printf '%s\n' 'Memory Depth companion: acquiring exact accepted VexLocalBridge source...'
"$GH_BIN" repo clone "$SDK_REPOSITORY" "$SDK" -- --no-checkout >/dev/null
"$GIT_BIN" -C "$SDK" checkout --detach "$SDK_HEAD" >/dev/null
[ "$("$GIT_BIN" -C "$SDK" rev-parse HEAD)" = "$SDK_HEAD" ] || { echo 'Memory Depth companion: SDK head mismatch.' >&2; exit 15; }
[ "$("$GIT_BIN" -C "$SDK" rev-parse 'HEAD^{tree}')" = "$SDK_TREE" ] || { echo 'Memory Depth companion: SDK tree mismatch.' >&2; exit 16; }
[ -z "$("$GIT_BIN" -C "$SDK" status --porcelain=v1 -uall)" ] || { echo 'Memory Depth companion: SDK source is dirty.' >&2; exit 17; }
OBSERVED_CONTRACT_BLOB="$("$GIT_BIN" -C "$SDK" rev-parse "$SDK_HEAD:docs/private-continuity/vexrelay/current-contract.json")"
[ "$OBSERVED_CONTRACT_BLOB" = "$CONTRACT_BLOB" ] || { echo 'Memory Depth companion: Relay contract blob mismatch.' >&2; exit 18; }
"$NODE_BIN" -e 'const fs=require("fs");const x=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));if(x.contractRef!=="source.vexrelay.current-contract/v1")process.exit(19)' "$SDK/docs/private-continuity/vexrelay/current-contract.json"

CURRENT_JSON="$CONTEXT_ROOT/CURRENT-ATTEMPT.json"
DO_NOT_REPLAY="$("$NODE_BIN" -e 'const fs=require("fs");const x=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));process.stdout.write(x.doNotReplay===true?"true":"false")' "$CURRENT_JSON")"
if [ "$DO_NOT_REPLAY" = "true" ]; then
  printf '%s\n' 'Memory Depth companion: current attempt is consumed and held for semantic discernment; no replay formed.'
  exit 23
fi

printf '%s\n' 'Memory Depth companion: forming current attempt from CURRENT-ATTEMPT.json...'
FORMATION="$("$NODE_BIN" "$CONTEXT_ROOT/runner/form-task.cjs" "$SDK" "$CONTEXT_ROOT" "$HOME/Downloads" "$LOCAL_HEAD")"
TASK_ZIP="$(printf '%s' "$FORMATION" | "$NODE_BIN" -e 'let s="";process.stdin.on("data",d=>s+=d).on("end",()=>process.stdout.write(JSON.parse(s).finalZip))')"
TASK_SHA="$(printf '%s' "$FORMATION" | "$NODE_BIN" -e 'let s="";process.stdin.on("data",d=>s+=d).on("end",()=>process.stdout.write(JSON.parse(s).digest))')"
[ -f "$TASK_ZIP" ] || { echo 'Memory Depth companion: formed task ZIP missing.' >&2; exit 20; }
OBSERVED="$("$SHASUM_BIN" -a 256 "$TASK_ZIP" | "$CUT_BIN" -d ' ' -f 1)"
[ "$OBSERVED" = "$TASK_SHA" ] || { echo 'Memory Depth companion: formed task digest mismatch.' >&2; exit 21; }

printf 'Memory Depth companion: task %s\n' "$(basename "$TASK_ZIP")"
printf '%s\n' 'Memory Depth companion: running the current attempt under the effect bounds recorded in CURRENT-ATTEMPT.json.'
"$NODE_BIN" "$CONTEXT_ROOT/runner/run-task.cjs" "$SDK" "$TASK_ZIP" "$TASK_SHA" "$HOME/Downloads"

AFTER="$("$SHASUM_BIN" -a 256 "$TASK_ZIP" | "$CUT_BIN" -d ' ' -f 1)"
[ "$AFTER" = "$TASK_SHA" ] || { echo 'Memory Depth companion: task bytes changed during execution.' >&2; exit 22; }
