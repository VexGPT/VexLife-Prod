#!/bin/bash
set -euo pipefail

SDK_REPO='vgong24/Vextreme-SDK'
SDK_BRANCH='VXG-111926-chatgpt-vexlife-prod-host-effect-a001'
SDK_HEAD='149455eeec4c7b7ca00e91022154a85faa3d608c'
SDK_TREE='53ea701f9bbf24a5cf37d43e4dbe3836187f4011'
CONTRACT_REF='source.vexrelay.current-contract/v1'
ROOT="$(cd "$(dirname "$0")" && pwd)"
BOOT="$(/usr/bin/mktemp -d "$HOME/Downloads/.pr516-source-owner-a002-bootstrap.XXXXXX")"
TASK_ROOT="$(/usr/bin/mktemp -d "$HOME/Downloads/.pr516-source-owner-a002-task.XXXXXX")"

cleanup() {
  /bin/rm -rf -- "$BOOT" "$TASK_ROOT"
}
trap cleanup EXIT HUP INT TERM

find_exe() {
  for candidate in "$@"; do
    if [ -x "$candidate" ]; then printf '%s\n' "$candidate"; return 0; fi
  done
  return 1
}
GH="$(find_exe /opt/homebrew/bin/gh /usr/local/bin/gh)" || { printf '%s\n' 'GitHub CLI not found.' >&2; exit 20; }
NODE="$(find_exe /opt/homebrew/bin/node /usr/local/bin/node /usr/bin/node)" || { printf '%s\n' 'Node not found.' >&2; exit 21; }
GIT=/usr/bin/git

"$GH" auth status --hostname github.com >/dev/null
SDK="$BOOT/sdk"
"$GH" repo clone "$SDK_REPO" "$SDK" -- --filter=blob:none --no-checkout >/dev/null
"$GIT" -C "$SDK" fetch --quiet --no-tags origin main "$SDK_BRANCH"
CURRENT_SDK_MAIN="$("$GIT" -C "$SDK" rev-parse refs/remotes/origin/main)"
"$GIT" -C "$SDK" show "$CURRENT_SDK_MAIN:docs/private-continuity/vexrelay/current-contract.json" > "$BOOT/current-contract.json"
"$NODE" - "$BOOT/current-contract.json" "$CONTRACT_REF" "$SDK" "$CURRENT_SDK_MAIN" <<'NODE'
const fs=require('node:fs');
const cp=require('node:child_process');
const [file,expected,sdk,main]=process.argv.slice(2);
const value=JSON.parse(fs.readFileSync(file,'utf8'));
if(value.contractRef!==expected) throw new Error(`current Relay contract mismatch: ${value.contractRef}`);
if(value.taskSchema!=='vex.relay.task/v2') throw new Error(`unexpected task schema: ${value.taskSchema}`);
if(value.currentRuntimeVersion!=='v0.2.0-RC1') throw new Error(`unexpected current runtime: ${value.currentRuntimeVersion}`);
const carrier=(value.acceptedAdapters||[]).find(x=>x.adapterRef==='adapter.vexlocalbridge.native-vexrelay-task-archive/v1');
if(!carrier || carrier.state!=='TERMINAL_ACCEPTED_CURRENT') throw new Error('current native VexRelay task-archive carrier is not terminal accepted');
if(!(value.acceptedRecipeFamilies||[]).includes('ExactHeadSourceOwnerProof')) throw new Error('current contract does not admit ExactHeadSourceOwnerProof');
const min=value.currentAcceptedMain?.sha;
if(!min) throw new Error('current contract has no accepted-main binding');
const ancestry=cp.spawnSync('/usr/bin/git',['-C',sdk,'merge-base','--is-ancestor',min,main]);
if(ancestry.status!==0) throw new Error(`current SDK main ${main} does not satisfy minimum accepted ancestor ${min}`);
NODE

"$GIT" -C "$SDK" checkout --quiet --detach "$SDK_HEAD"
[ "$("$GIT" -C "$SDK" rev-parse HEAD)" = "$SDK_HEAD" ] || { printf '%s\n' 'SDK exact head mismatch.' >&2; exit 22; }
[ "$("$GIT" -C "$SDK" rev-parse 'HEAD^{tree}')" = "$SDK_TREE" ] || { printf '%s\n' 'SDK exact tree mismatch.' >&2; exit 23; }
[ -z "$("$GIT" -C "$SDK" status --porcelain=v1 -uall)" ] || { printf '%s\n' 'SDK source dirty after exact checkout.' >&2; exit 24; }

for member in \
  lib/vexlocalbridge/vexrelay-task-archive-carrier-formation.mjs \
  lib/vexlocalbridge/vexrelay-task-archive.js \
  lib/vexlocalbridge/package-builder.js \
  lib/vexlocalbridge/cli.js \
  lib/vexrelay/recipe-compiler.mjs \
  docs/private-continuity/vexlocalbridge/profiles/macos-victor-vexlife-prod.json; do
  [ -f "$SDK/$member" ] || { printf 'Required SDK member missing: %s\n' "$member" >&2; exit 25; }
done

"$NODE" "$ROOT/formation-A002.mjs" "$SDK" "$TASK_ROOT" > "$BOOT/formation.json"
PACKAGE="$("$NODE" -e 'const x=require(process.argv[1]);process.stdout.write(x.finalZip)' "$BOOT/formation.json")"
EXPECTED="$("$NODE" -e 'const x=require(process.argv[1]);process.stdout.write(x.digest)' "$BOOT/formation.json")"
CAPSULE="$("$NODE" -e 'const x=require(process.argv[1]);process.stdout.write(x.capsuleRoot)' "$BOOT/formation.json")"
PROFILE="$("$NODE" -e 'const x=require(process.argv[1]);process.stdout.write(x.profilePath)' "$BOOT/formation.json")"
OBSERVED="$(/usr/bin/shasum -a 256 "$PACKAGE" | /usr/bin/cut -d ' ' -f 1)"
[ "$OBSERVED" = "$EXPECTED" ] || { printf '%s\n' 'Formed SourceOwner task digest mismatch before execution.' >&2; exit 26; }

set +e
"$NODE" "$SDK/lib/vexlocalbridge/cli.js" run \
  --package "$PACKAGE" \
  --profile "$PROFILE" \
  --capsule-root "$CAPSULE" > "$BOOT/run.json"
RUN_EXIT=$?
set -e

RESULT="$("$NODE" -e 'const fs=require("node:fs");const x=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));if(!x.resultZip)process.exit(2);process.stdout.write(x.resultZip)' "$BOOT/run.json")" || {
  printf '%s\n' "VexLocalBridge returned no canonical result path (exit $RUN_EXIT)." >&2
  /bin/cat "$BOOT/run.json" >&2 || true
  exit 27
}
[ -f "$RESULT" ] || { printf '%s\n' "Canonical RETURN missing: $RESULT" >&2; exit 28; }
RESULT_SHA="$(/usr/bin/shasum -a 256 "$RESULT" | /usr/bin/cut -d ' ' -f 1)"
DEST="$HOME/Downloads/$(/usr/bin/basename "$RESULT")"
if [ -e "$DEST" ]; then
  DEST_SHA="$(/usr/bin/shasum -a 256 "$DEST" | /usr/bin/cut -d ' ' -f 1)"
  [ "$DEST_SHA" = "$RESULT_SHA" ] || { printf '%s\n' "Different bytes already exist at $DEST" >&2; exit 29; }
else
  /bin/cp -p "$RESULT" "$DEST"
fi

printf '\n%s\n' 'PR516 Initial Boot Prefix SourceOwner A002 finished.'
printf 'RETURN: %s\n' "$DEST"
printf 'SHA-256: %s\n' "$RESULT_SHA"
printf 'Runner exit: %s (semantic disposition is inside RETURN)\n' "$RUN_EXIT"
/usr/bin/open -R "$DEST" >/dev/null 2>&1 || true
