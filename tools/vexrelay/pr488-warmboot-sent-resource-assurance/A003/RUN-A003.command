#!/bin/bash
set -euo pipefail

HARNESS_HEAD="${1:-}"
HARNESS_TREE="${2:-}"
RUN_BLOB="${3:-}"
FORMATION_BLOB="${4:-}"
CONTEXT_BLOB="${5:-}"
[ -n "$HARNESS_HEAD" ] && [ -n "$HARNESS_TREE" ] && [ -n "$RUN_BLOB" ] && [ -n "$FORMATION_BLOB" ] && [ -n "$CONTEXT_BLOB" ] || {
  echo 'PR488 Assurance A003: frozen harness identities missing.' >&2; exit 9;
}

HARNESS_REPOSITORY='VexGPT/VexLife-Prod'
HARNESS_DIR='tools/vexrelay/pr488-warmboot-sent-resource-assurance/A003'
SDK_REPOSITORY='vgong24/Vextreme-SDK'
SDK_HEAD='6f8f6f50467f5df5a542713f7dc25b82db4ac8fa'
SDK_TREE='96795640b01f1969da980f52d5a8ec136a8c44aa'
PRODUCT_HEAD='a23b6a949d27e4b77530273a390955f0d127ad1a'
PRODUCT_TREE='a268826bf26eecb3a2136593b202cf651643d4e9'
CURRENT_CONTRACT_REF='source.vexrelay.current-contract/v1'
DOWNLOADS="$HOME/Downloads"

find_exe(){ for candidate in "$@"; do [ -x "$candidate" ] && { printf '%s\n' "$candidate"; return 0; }; done; return 1; }
GH="$(find_exe /opt/homebrew/bin/gh /usr/local/bin/gh)" || { echo 'GitHub CLI not found.' >&2; exit 20; }
NODE="$(find_exe /opt/homebrew/bin/node /usr/local/bin/node /usr/bin/node)" || { echo 'Node not found.' >&2; exit 21; }
GIT=/usr/bin/git
SHASUM=/usr/bin/shasum
BASH_BIN=/bin/bash
ROOT_TMP="$(/usr/bin/mktemp -d "$DOWNLOADS/.pr488-assurance-a003.XXXXXX")"
cleanup(){ /bin/rm -rf -- "$ROOT_TMP"; }
trap cleanup EXIT HUP INT TERM

"$GH" auth status --hostname github.com >/dev/null 2>&1 || { echo 'GitHub authentication unavailable.' >&2; exit 22; }

HARNESS="$ROOT_TMP/VexLife-Prod-harness"
"$GH" repo clone "$HARNESS_REPOSITORY" "$HARNESS" -- --filter=blob:none --no-checkout >/dev/null
"$GIT" -C "$HARNESS" checkout --quiet --detach "$HARNESS_HEAD"
[ "$("$GIT" -C "$HARNESS" rev-parse HEAD)" = "$HARNESS_HEAD" ] || { echo 'Harness head mismatch.' >&2; exit 23; }
[ "$("$GIT" -C "$HARNESS" rev-parse 'HEAD^{tree}')" = "$HARNESS_TREE" ] || { echo 'Harness tree mismatch.' >&2; exit 24; }
[ "$("$GIT" -C "$HARNESS" rev-parse "$HARNESS_HEAD:$HARNESS_DIR/RUN-A003.command")" = "$RUN_BLOB" ] || { echo 'Runner blob mismatch.' >&2; exit 25; }
[ "$("$GIT" -C "$HARNESS" rev-parse "$HARNESS_HEAD:$HARNESS_DIR/formation-A003.mjs")" = "$FORMATION_BLOB" ] || { echo 'Formation blob mismatch.' >&2; exit 26; }
[ "$("$GIT" -C "$HARNESS" rev-parse "$HARNESS_HEAD:$HARNESS_DIR/relay-context.json")" = "$CONTEXT_BLOB" ] || { echo 'Context blob mismatch.' >&2; exit 27; }
[ -z "$("$GIT" -C "$HARNESS" status --porcelain=v1 -uall)" ] || { echo 'Harness checkout dirty.' >&2; exit 28; }
"$NODE" --check "$HARNESS/$HARNESS_DIR/formation-A003.mjs"

OBSERVED_PRODUCT_TREE="$("$GH" api "repos/$HARNESS_REPOSITORY/git/commits/$PRODUCT_HEAD" --jq '.tree.sha')"
[ "$OBSERVED_PRODUCT_TREE" = "$PRODUCT_TREE" ] || { echo 'Product tree drift.' >&2; exit 29; }

SDK="$ROOT_TMP/Vextreme-SDK"
"$GH" repo clone "$SDK_REPOSITORY" "$SDK" -- --filter=blob:none --no-checkout >/dev/null
CURRENT="$("$GIT" -C "$SDK" rev-parse refs/remotes/origin/main)"
[ "$CURRENT" = "$SDK_HEAD" ] || { printf 'SDK main advanced: %s\n' "$CURRENT" >&2; exit 30; }
"$GIT" -C "$SDK" checkout --quiet --detach "$SDK_HEAD"
[ "$("$GIT" -C "$SDK" rev-parse 'HEAD^{tree}')" = "$SDK_TREE" ] || { echo 'SDK tree mismatch.' >&2; exit 31; }

CONTRACT="$SDK/docs/private-continuity/vexrelay/current-contract.json"
"$NODE" - "$CONTRACT" "$CURRENT_CONTRACT_REF" <<'NODE'
const fs=require('node:fs');
const [file,ref]=process.argv.slice(2);
const x=JSON.parse(fs.readFileSync(file,'utf8'));
if(x.contractRef!==ref||x.taskSchema!=='vex.relay.task/v2'||x.currentRuntimeVersion!=='v0.2.0-RC1') throw new Error('current Relay contract mismatch');
const c=(x.acceptedAdapters||[]).find(v=>v.adapterRef==='adapter.vexlocalbridge.native-vexrelay-task-archive/v1');
if(!c||c.state!=='TERMINAL_ACCEPTED_CURRENT') throw new Error('native repository carrier not current');
if(!(x.acceptedRecipeFamilies||[]).includes('IndependentAssurance')) throw new Error('IndependentAssurance recipe not current');
NODE

FORMATION_OUT="$ROOT_TMP/formation.json"
"$NODE" "$HARNESS/$HARNESS_DIR/formation-A003.mjs" "$SDK" "$DOWNLOADS" > "$FORMATION_OUT"
PACKAGE="$("$NODE" -e 'const x=require(process.argv[1]);process.stdout.write(x.outerZip)' "$FORMATION_OUT")"
EXPECTED="$("$NODE" -e 'const x=require(process.argv[1]);process.stdout.write(x.outerSha256)' "$FORMATION_OUT")"
[ -f "$PACKAGE" ] || { echo 'formed Assurance package missing.' >&2; exit 32; }
[ "$("$SHASUM" -a 256 "$PACKAGE" | /usr/bin/cut -d ' ' -f 1)" = "$EXPECTED" ] || { echo 'Assurance package digest mismatch.' >&2; exit 33; }

printf 'PR488 Assurance A003: entering read-only IndependentAssurance carrier.\n'
set +e
VEX_EXPECTED_PACKAGE_SHA256="$EXPECTED" "$BASH_BIN" "$SDK/lib/vexlocalbridge/templates/RUN-VEX-TASK.command" "$PACKAGE"
STATUS=$?
set -e
printf 'PR488 Assurance A003 carrier exit: %s\n' "$STATUS"
printf 'If a canonical RETURN was formed, upload the exact RETURN ZIP revealed by VexLocalBridge.\n'
exit "$STATUS"
