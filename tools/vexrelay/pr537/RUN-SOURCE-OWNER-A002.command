#!/bin/bash
# [VXG RealForever]
set -euo pipefail

SDK_REPO='vgong24/Vextreme-SDK'
SDK_HEAD='6f8f6f50467f5df5a542713f7dc25b82db4ac8fa'
SDK_TREE='96795640b01f1969da980f52d5a8ec136a8c44aa'
CONTRACT_REF='source.vexrelay.current-contract/v1'
ROOT="$(cd "$(dirname "$0")" && pwd)"
BOOT="$(/usr/bin/mktemp -d "$HOME/Downloads/.pr537-source-owner-a002-bootstrap.XXXXXX")"
TASK_ROOT="$(/usr/bin/mktemp -d "$HOME/Downloads/.pr537-source-owner-a002-task.XXXXXX")"
cleanup(){ /bin/rm -rf -- "$BOOT" "$TASK_ROOT"; }
trap cleanup EXIT HUP INT TERM

find_exe(){ for candidate in "$@"; do [ -x "$candidate" ] && { printf '%s\n' "$candidate"; return 0; }; done; return 1; }
GH="$(find_exe /opt/homebrew/bin/gh /usr/local/bin/gh)" || { echo 'GitHub CLI not found.' >&2; exit 20; }
NODE="$(find_exe /opt/homebrew/bin/node /usr/local/bin/node /usr/bin/node)" || { echo 'Node not found.' >&2; exit 21; }
GIT=/usr/bin/git

"$GH" auth status --hostname github.com >/dev/null
SDK="$BOOT/sdk"
"$GH" repo clone "$SDK_REPO" "$SDK" -- --filter=blob:none --no-checkout >/dev/null
CURRENT="$("$GIT" -C "$SDK" rev-parse refs/remotes/origin/main)"
[ "$CURRENT" = "$SDK_HEAD" ] || { printf 'SDK main advanced: %s\n' "$CURRENT" >&2; exit 22; }
"$GIT" -C "$SDK" checkout --quiet --detach "$SDK_HEAD"
[ "$("$GIT" -C "$SDK" rev-parse 'HEAD^{tree}')" = "$SDK_TREE" ] || { echo 'SDK tree mismatch.' >&2; exit 23; }

CONTRACT="$SDK/docs/private-continuity/vexrelay/current-contract.json"
"$NODE" - "$CONTRACT" "$CONTRACT_REF" <<'NODE'
const fs=require('node:fs');
const [file,ref]=process.argv.slice(2);
const x=JSON.parse(fs.readFileSync(file,'utf8'));
if(x.contractRef!==ref||x.taskSchema!=='vex.relay.task/v2'||x.currentRuntimeVersion!=='v0.2.0-RC1'){
  throw new Error('current Relay contract mismatch');
}
const c=(x.acceptedAdapters||[]).find(v=>v.adapterRef==='adapter.vexlocalbridge.native-vexrelay-task-archive/v1');
if(!c||c.state!=='TERMINAL_ACCEPTED_CURRENT') throw new Error('native repository carrier not current');
if(!(x.acceptedRecipeFamilies||[]).includes('ExactHeadSourceOwnerProof')) throw new Error('source-owner recipe not current');
NODE

"$NODE" "$ROOT/formation-A002.mjs" "$SDK" "$TASK_ROOT" > "$BOOT/formation.json"
PACKAGE="$("$NODE" -e 'const x=require(process.argv[1]);process.stdout.write(x.finalZip)' "$BOOT/formation.json")"
EXPECTED="$("$NODE" -e 'const x=require(process.argv[1]);process.stdout.write(x.digest)' "$BOOT/formation.json")"
CAPSULE="$("$NODE" -e 'const x=require(process.argv[1]);process.stdout.write(x.capsuleRoot)' "$BOOT/formation.json")"
PROFILE="$("$NODE" -e 'const x=require(process.argv[1]);process.stdout.write(x.profilePath)' "$BOOT/formation.json")"
[ "$(/usr/bin/shasum -a 256 "$PACKAGE" | /usr/bin/cut -d ' ' -f 1)" = "$EXPECTED" ] || { echo 'Task digest mismatch.' >&2; exit 24; }

set +e
"$NODE" "$SDK/lib/vexlocalbridge/cli.js" run --package "$PACKAGE" --profile "$PROFILE" --capsule-root "$CAPSULE" > "$BOOT/run.json"
RUN_EXIT=$?
set -e

RESULT="$("$NODE" -e 'const fs=require("node:fs");const x=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));if(!x.resultZip)process.exit(2);process.stdout.write(x.resultZip)' "$BOOT/run.json")" || {
  /bin/cat "$BOOT/run.json" >&2 || true
  exit 25
}
[ -f "$RESULT" ] || { echo "Canonical RETURN missing: $RESULT" >&2; exit 26; }

SHA="$(/usr/bin/shasum -a 256 "$RESULT" | /usr/bin/cut -d ' ' -f 1)"
DEST="$HOME/Downloads/$(/usr/bin/basename "$RESULT")"
if [ -e "$DEST" ]; then
  [ "$(/usr/bin/shasum -a 256 "$DEST" | /usr/bin/cut -d ' ' -f 1)" = "$SHA" ] || {
    echo 'Different RETURN bytes already exist.' >&2
    exit 27
  }
else
  /bin/cp -p "$RESULT" "$DEST"
fi

printf '\nPR537 SourceOwner A002 finished.\nRETURN: %s\nSHA-256: %s\nRunner exit: %s\n' "$DEST" "$SHA" "$RUN_EXIT"
/usr/bin/open -R "$DEST" >/dev/null 2>&1 || true
