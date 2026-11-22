#!/bin/bash
set -euo pipefail

HARNESS_HEAD="${1:-}"
HARNESS_TREE="${2:-}"
RUN_BLOB="${3:-}"
FORMATION_BLOB="${4:-}"
PAYLOAD_BLOB="${5:-}"
README_BLOB="${6:-}"
[ -n "$HARNESS_HEAD" ] && [ -n "$HARNESS_TREE" ] && [ -n "$RUN_BLOB" ] && [ -n "$FORMATION_BLOB" ] && [ -n "$PAYLOAD_BLOB" ] && [ -n "$README_BLOB" ] || { echo 'PR488 whole lived: frozen harness identities missing.' >&2; exit 9; }
HARNESS_REPOSITORY='VexGPT/VexLife-Prod'
HARNESS_DIR='tools/vexrelay/pr488-warmboot-sent-resource-whole-lived'
SDK_REPOSITORY='vgong24/Vextreme-SDK'
CARRIER_PR='1956'
CARRIER_HEAD='149455eeec4c7b7ca00e91022154a85faa3d608c'
CARRIER_TREE='53ea701f9bbf24a5cf37d43e4dbe3836187f4011'
CURRENT_CONTRACT_REF='source.vexrelay.current-contract/v1'
DOWNLOADS="$HOME/Downloads"
resolve_exec(){ for c in "$@"; do [ -x "$c" ] && [ -f "$c" ] && { printf '%s' "$c"; return 0; }; done; return 1; }
BASH_BIN="$(resolve_exec /bin/bash)" || exit 10
GIT_BIN="$(resolve_exec /usr/bin/git)" || exit 11
GH_BIN="$(resolve_exec /opt/homebrew/bin/gh /usr/local/bin/gh)" || exit 12
NODE_BIN="$(resolve_exec /opt/homebrew/bin/node /usr/local/bin/node /usr/bin/node)" || exit 13
SHASUM_BIN="$(resolve_exec /usr/bin/shasum)" || exit 14
MKTEMP_BIN="$(resolve_exec /usr/bin/mktemp)" || exit 15
RM_BIN="$(resolve_exec /bin/rm /usr/bin/rm)" || exit 16
TEE_BIN="$(resolve_exec /usr/bin/tee)" || exit 17
"$GH_BIN" auth status --hostname github.com >/dev/null 2>&1 || { echo 'GitHub authentication unavailable.' >&2; exit 20; }
WORK_ROOT="$("$MKTEMP_BIN" -d "$DOWNLOADS/.vexlife-pr488-whole-lived-a001.XXXXXX")"
case "$WORK_ROOT" in "$DOWNLOADS"/.vexlife-pr488-whole-lived-a001.*) ;; *) exit 21 ;; esac
cleanup(){ case "$WORK_ROOT" in "$DOWNLOADS"/.vexlife-pr488-whole-lived-a001.*) "$RM_BIN" -rf -- "$WORK_ROOT" ;; esac; }
trap cleanup EXIT HUP INT TERM
HARNESS_ROOT="$WORK_ROOT/VexLife-Prod-harness"
SDK_ROOT="$WORK_ROOT/Vextreme-SDK"
CONTRACT="$WORK_ROOT/current-contract.json"
PR_STATE="$WORK_ROOT/carrier-pr.json"
FORMATION_OUTPUT="$WORK_ROOT/formation-output.txt"
EXECUTION_OUTPUT="$WORK_ROOT/execution-output.txt"

printf '%s\n' '[1/6] Acquiring frozen PR488 whole-lived harness...'
"$GH_BIN" repo clone "$HARNESS_REPOSITORY" "$HARNESS_ROOT" -- --filter=blob:none --no-checkout >/dev/null
"$GIT_BIN" -C "$HARNESS_ROOT" checkout --quiet --detach "$HARNESS_HEAD"
[ "$("$GIT_BIN" -C "$HARNESS_ROOT" rev-parse HEAD)" = "$HARNESS_HEAD" ] || exit 22
[ "$("$GIT_BIN" -C "$HARNESS_ROOT" rev-parse 'HEAD^{tree}')" = "$HARNESS_TREE" ] || exit 23
[ "$("$GIT_BIN" -C "$HARNESS_ROOT" rev-parse "$HARNESS_HEAD:$HARNESS_DIR/RUN-LIVED-A001.command")" = "$RUN_BLOB" ] || exit 24
[ "$("$GIT_BIN" -C "$HARNESS_ROOT" rev-parse "$HARNESS_HEAD:$HARNESS_DIR/formation-A001.mjs")" = "$FORMATION_BLOB" ] || exit 25
[ "$("$GIT_BIN" -C "$HARNESS_ROOT" rev-parse "$HARNESS_HEAD:$HARNESS_DIR/payload-A001.mjs")" = "$PAYLOAD_BLOB" ] || exit 26
[ "$("$GIT_BIN" -C "$HARNESS_ROOT" rev-parse "$HARNESS_HEAD:$HARNESS_DIR/README.md")" = "$README_BLOB" ] || exit 27
[ -z "$("$GIT_BIN" -C "$HARNESS_ROOT" status --porcelain=v1 -uall)" ] || exit 28
ROOT="$HARNESS_ROOT/$HARNESS_DIR"

printf '%s\n' '[2/6] Verifying current Relay contract and qualified VexLife host carrier...'
"$GH_BIN" api --method GET -H 'Accept: application/vnd.github.raw+json' "repos/$SDK_REPOSITORY/contents/docs/private-continuity/vexrelay/current-contract.json?ref=main" > "$CONTRACT"
"$GH_BIN" api --method GET "repos/$SDK_REPOSITORY/pulls/$CARRIER_PR" > "$PR_STATE"
"$NODE_BIN" - "$CONTRACT" "$PR_STATE" "$CURRENT_CONTRACT_REF" "$CARRIER_HEAD" <<'NODE'
const fs=require('node:fs');const [cf,pf,ref,head]=process.argv.slice(2);const c=JSON.parse(fs.readFileSync(cf,'utf8'));const p=JSON.parse(fs.readFileSync(pf,'utf8'));
if(c.contractRef!==ref) throw new Error('CONTRACT_REF_MISMATCH');
const d=(c.acceptedAdapters||[]).find(x=>x.adapterRef==='adapter.vexlocalbridge.direct-host-effect');
if(!d||d.state!=='TERMINAL_ACCEPTED_CURRENT'||d.effectClass!=='HOST_SEMANTIC_MODEL_RUNTIME') throw new Error('DIRECT_HOST_EFFECT_NOT_CURRENT');
if(p.head?.sha!==head||p.state!=='open') throw new Error('QUALIFIED_CARRIER_CURRENTNESS_MISMATCH');
NODE

printf '%s\n' '[3/6] Acquiring exact qualified VexLife Mac carrier...'
"$GH_BIN" repo clone "$SDK_REPOSITORY" "$SDK_ROOT" -- --filter=blob:none --no-checkout >/dev/null
"$GIT_BIN" -C "$SDK_ROOT" checkout --quiet --detach "$CARRIER_HEAD"
[ "$("$GIT_BIN" -C "$SDK_ROOT" rev-parse HEAD)" = "$CARRIER_HEAD" ] || exit 29
[ "$("$GIT_BIN" -C "$SDK_ROOT" rev-parse 'HEAD^{tree}')" = "$CARRIER_TREE" ] || exit 30
[ -z "$("$GIT_BIN" -C "$SDK_ROOT" status --porcelain=v1 -uall)" ] || exit 31

printf '%s\n' '[4/6] Static-checking frozen whole-lived formation and payload...'
"$NODE_BIN" --check "$ROOT/formation-A001.mjs"
"$NODE_BIN" --check "$ROOT/payload-A001.mjs"

printf '%s\n' '[5/6] Forming fresh digest-bound whole-lived task...'
"$NODE_BIN" "$ROOT/formation-A001.mjs" "$SDK_ROOT" "$DOWNLOADS" "$ROOT/payload-A001.mjs" | "$TEE_BIN" "$FORMATION_OUTPUT"
FORMATION_JSON="$("$NODE_BIN" - "$FORMATION_OUTPUT" <<'NODE'
const fs=require('node:fs');const lines=fs.readFileSync(process.argv[2],'utf8').split(/\r?\n/).map(x=>x.trim()).filter(Boolean);for(let i=lines.length-1;i>=0;i--){try{const x=JSON.parse(lines[i]);if(x&&x.disposition==='PASS'&&x.packagePath&&x.packageSha256){process.stdout.write(JSON.stringify(x));process.exit(0);}}catch{}}process.exit(35);
NODE
)"
PACKAGE_PATH="$("$NODE_BIN" -e 'process.stdout.write(JSON.parse(process.argv[1]).packagePath)' "$FORMATION_JSON")"
PACKAGE_SHA="$("$NODE_BIN" -e 'process.stdout.write(JSON.parse(process.argv[1]).packageSha256)' "$FORMATION_JSON")"
[ -f "$PACKAGE_PATH" ] && [ ! -L "$PACKAGE_PATH" ] || exit 36
[ "$("$SHASUM_BIN" -a 256 "$PACKAGE_PATH" | /usr/bin/cut -d ' ' -f 1)" = "$PACKAGE_SHA" ] || exit 37
BOOTSTRAP_COMMAND="$("$NODE_BIN" - "$SDK_ROOT/lib/vexlocalbridge/mac-vexlife-prod-source-bootstrap-command.js" "$PACKAGE_SHA" "$CARRIER_HEAD" "$CARRIER_TREE" <<'NODE'
const [modulePath,packageSha256,sdkHead,sdkTree]=process.argv.slice(2);const m=require(modulePath);process.stdout.write(m.renderMacVexLifeProdSourceBootstrapCommand({packageSha256,sdkHead,sdkTree}));
NODE
)"
[ -n "$BOOTSTRAP_COMMAND" ] || exit 38

printf '%s\n' '[6/6] Entering the exact #488 whole lived acceptance.'
printf '%s\n' 'Do not Send until VexLife opens and the terminal prints the lived gate instructions.'
set +e
"$BASH_BIN" -c "$BOOTSTRAP_COMMAND" 2>&1 | "$TEE_BIN" "$EXECUTION_OUTPUT"
RUN_STATUS="${PIPESTATUS[0]}"
set -e
RETURN_JSON="$("$NODE_BIN" - "$EXECUTION_OUTPUT" <<'NODE'
const fs=require('node:fs');const t=fs.readFileSync(process.argv[2],'utf8');const p=[...t.matchAll(/"resultZip"\s*:\s*"([^"]+)"/g)];const s=[...t.matchAll(/"resultZipSha256"\s*:\s*"([a-f0-9]{64})"/g)];if(!p.length||!s.length)process.exit(0);process.stdout.write(JSON.stringify({path:p[p.length-1][1],sha:s[s.length-1][1]}));
NODE
)"
if [ -n "$RETURN_JSON" ]; then
  RETURN_PATH="$("$NODE_BIN" -e 'process.stdout.write(JSON.parse(process.argv[1]).path)' "$RETURN_JSON")"
  RETURN_SHA="$("$NODE_BIN" -e 'process.stdout.write(JSON.parse(process.argv[1]).sha)' "$RETURN_JSON")"
  if [ -f "$RETURN_PATH" ] && [ "$("$SHASUM_BIN" -a 256 "$RETURN_PATH" | /usr/bin/cut -d ' ' -f 1)" = "$RETURN_SHA" ]; then
    printf '\nPR488 whole-lived canonical RETURN: %s\nSHA-256: %s\n' "$RETURN_PATH" "$RETURN_SHA"
    /usr/bin/open -R "$RETURN_PATH" >/dev/null 2>&1 || true
  fi
fi
exit "$RUN_STATUS"
