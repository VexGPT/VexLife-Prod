#!/bin/bash
set -euo pipefail

HARNESS_HEAD="${1:-}"
HARNESS_TREE="${2:-}"
RUN_BLOB="${3:-}"
FORMATION_BLOB="${4:-}"
README_BLOB="${5:-}"
[ -n "$HARNESS_HEAD" ] && [ -n "$HARNESS_TREE" ] && [ -n "$RUN_BLOB" ] && [ -n "$FORMATION_BLOB" ] && [ -n "$README_BLOB" ] || {
  echo 'PR598 SourceOwner A001: frozen harness identities missing.' >&2; exit 9;
}
HARNESS_REPOSITORY='VexGPT/VexLife-Prod'
HARNESS_DIR='tools/vexrelay/pr598-response-readiness-presentation-source-owner-a001'
SDK_REPOSITORY='vgong24/Vextreme-SDK'
SDK_HEAD='6f8f6f50467f5df5a542713f7dc25b82db4ac8fa'
SDK_TREE='96795640b01f1969da980f52d5a8ec136a8c44aa'
CURRENT_CONTRACT_REF='source.vexrelay.current-contract/v1'
DOWNLOADS="$HOME/Downloads"

find_exe(){ for candidate in "$@"; do [ -x "$candidate" ] && { printf '%s\n' "$candidate"; return 0; }; done; return 1; }
GH="$(find_exe /opt/homebrew/bin/gh /usr/local/bin/gh)" || { echo 'GitHub CLI not found.' >&2; exit 20; }
NODE="$(find_exe /opt/homebrew/bin/node /usr/local/bin/node /usr/bin/node)" || { echo 'Node not found.' >&2; exit 21; }
GIT=/usr/bin/git
SHASUM=/usr/bin/shasum
BOOT="$(/usr/bin/mktemp -d "$DOWNLOADS/.pr598-source-owner-a001-bootstrap.XXXXXX")"
TASK_ROOT="$(/usr/bin/mktemp -d "$DOWNLOADS/.pr598-source-owner-a001-task.XXXXXX")"
cleanup(){ /bin/rm -rf -- "$BOOT" "$TASK_ROOT"; }
trap cleanup EXIT HUP INT TERM

"$GH" auth status --hostname github.com >/dev/null 2>&1 || { echo 'GitHub authentication unavailable.' >&2; exit 22; }
HARNESS="$BOOT/VexLife-Prod-harness"
"$GH" repo clone "$HARNESS_REPOSITORY" "$HARNESS" -- --filter=blob:none --no-checkout >/dev/null
"$GIT" -C "$HARNESS" checkout --quiet --detach "$HARNESS_HEAD"
[ "$("$GIT" -C "$HARNESS" rev-parse HEAD)" = "$HARNESS_HEAD" ] || { echo 'Harness head mismatch.' >&2; exit 23; }
[ "$("$GIT" -C "$HARNESS" rev-parse 'HEAD^{tree}')" = "$HARNESS_TREE" ] || { echo 'Harness tree mismatch.' >&2; exit 24; }
[ "$("$GIT" -C "$HARNESS" rev-parse "$HARNESS_HEAD:$HARNESS_DIR/RUN-SOURCE-OWNER-A001.command")" = "$RUN_BLOB" ] || { echo 'Runner blob mismatch.' >&2; exit 25; }
[ "$("$GIT" -C "$HARNESS" rev-parse "$HARNESS_HEAD:$HARNESS_DIR/formation-A001.mjs")" = "$FORMATION_BLOB" ] || { echo 'Formation blob mismatch.' >&2; exit 26; }
[ "$("$GIT" -C "$HARNESS" rev-parse "$HARNESS_HEAD:$HARNESS_DIR/README.md")" = "$README_BLOB" ] || { echo 'README blob mismatch.' >&2; exit 27; }
[ -z "$("$GIT" -C "$HARNESS" status --porcelain=v1 -uall)" ] || { echo 'Harness checkout dirty.' >&2; exit 28; }
ROOT="$HARNESS/$HARNESS_DIR"
"$NODE" --check "$ROOT/formation-A001.mjs"

SDK="$BOOT/Vextreme-SDK"
"$GH" repo clone "$SDK_REPOSITORY" "$SDK" -- --filter=blob:none --no-checkout >/dev/null
CURRENT="$("$GIT" -C "$SDK" rev-parse refs/remotes/origin/main)"
[ "$CURRENT" = "$SDK_HEAD" ] || { printf 'SDK main advanced: %s\n' "$CURRENT" >&2; exit 29; }
"$GIT" -C "$SDK" checkout --quiet --detach "$SDK_HEAD"
[ "$("$GIT" -C "$SDK" rev-parse 'HEAD^{tree}')" = "$SDK_TREE" ] || { echo 'SDK tree mismatch.' >&2; exit 30; }
CONTRACT="$SDK/docs/private-continuity/vexrelay/current-contract.json"
"$NODE" - "$CONTRACT" "$CURRENT_CONTRACT_REF" <<'NODE'
const fs=require('node:fs');
const [file,ref]=process.argv.slice(2);
const x=JSON.parse(fs.readFileSync(file,'utf8'));
if(x.contractRef!==ref||x.taskSchema!=='vex.relay.task/v2'||x.currentRuntimeVersion!=='v0.2.0-RC1') throw new Error('current Relay contract mismatch');
const c=(x.acceptedAdapters||[]).find(v=>v.adapterRef==='adapter.vexlocalbridge.native-vexrelay-task-archive/v1');
if(!c||c.state!=='TERMINAL_ACCEPTED_CURRENT') throw new Error('native repository carrier not current');
if(!(x.acceptedRecipeFamilies||[]).includes('ExactHeadSourceOwnerProof')) throw new Error('source-owner recipe not current');
NODE

"$NODE" "$ROOT/formation-A001.mjs" "$SDK" "$TASK_ROOT" > "$BOOT/formation.json"
PACKAGE="$("$NODE" -e 'const x=require(process.argv[1]);process.stdout.write(x.finalZip)' "$BOOT/formation.json")"
EXPECTED="$("$NODE" -e 'const x=require(process.argv[1]);process.stdout.write(x.digest)' "$BOOT/formation.json")"
CAPSULE="$("$NODE" -e 'const x=require(process.argv[1]);process.stdout.write(x.capsuleRoot)' "$BOOT/formation.json")"
PROFILE="$("$NODE" -e 'const x=require(process.argv[1]);process.stdout.write(x.profilePath)' "$BOOT/formation.json")"
[ "$("$SHASUM" -a 256 "$PACKAGE" | /usr/bin/cut -d ' ' -f 1)" = "$EXPECTED" ] || { echo 'Task digest mismatch.' >&2; exit 31; }

set +e
"$NODE" "$SDK/lib/vexlocalbridge/cli.js" run --package "$PACKAGE" --profile "$PROFILE" --capsule-root "$CAPSULE" > "$BOOT/run.json"
RUN_EXIT=$?
set -e
RESULT="$("$NODE" -e 'const fs=require("node:fs");const x=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));if(!x.resultZip)process.exit(2);process.stdout.write(x.resultZip)' "$BOOT/run.json")" || { /bin/cat "$BOOT/run.json" >&2 || true; exit 32; }
[ -f "$RESULT" ] || { echo "Canonical RETURN missing: $RESULT" >&2; exit 33; }
SHA="$("$SHASUM" -a 256 "$RESULT" | /usr/bin/cut -d ' ' -f 1)"
DEST="$DOWNLOADS/$(/usr/bin/basename "$RESULT")"
if [ -e "$DEST" ]; then
  [ "$("$SHASUM" -a 256 "$DEST" | /usr/bin/cut -d ' ' -f 1)" = "$SHA" ] || { echo 'Different RETURN bytes already exist.' >&2; exit 34; }
else
  /bin/cp -p "$RESULT" "$DEST"
fi
printf '\nPR598 Response Readiness projection SourceOwner A001 finished.\nRETURN: %s\nSHA-256: %s\nRunner exit: %s\n' "$DEST" "$SHA" "$RUN_EXIT"
/usr/bin/open -R "$DEST" >/dev/null 2>&1 || true
exit "$RUN_EXIT"
