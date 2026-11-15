#!/bin/bash
set -euo pipefail

# [VXG RealForever]
# PR149 runtime stream log diagnostic A008.
# Read-only: no model call, no runtime lifecycle, no Product/Home mutation.

SDK_REPO='vgong24/Vextreme-SDK'
SDK_HEAD='8543c7d755faba04d4c4019c02e84f992667918a'
SDK_TREE='6376f68279f0750ee6d3868b8472368199763064'
CONTRACT_REF='source.vexrelay.current-contract/v1'
RUNTIME_VERSION='v0.2.0-RC1'

CARRIER_REPO='VexGPT/VexLife-Prod'
CARRIER_CONTENT_COMMIT='79507e27fd33deb7f145eae6a058a18e2777e70b'
PAYLOAD_PATH='proof-carrier/PR149-RUNTIME-STREAM-LOG-DIAG-A008.payload.cjs'
PAYLOAD_BLOB='2a4508c7f37d7d0b856d45a3bf71172e4afa8bb4'
FORM_PATH='proof-carrier/PR149-RUNTIME-STREAM-LOG-DIAG-A008.form.cjs'
FORM_BLOB='10ad72e5350904bae7906b988c9d9a1f32bab571'

resolve_exec() {
  local c
  for c in "$@"; do
    if [ -x "$c" ] && [ -f "$c" ]; then printf '%s' "$c"; return 0; fi
  done
  return 1
}

GH="$(resolve_exec /opt/homebrew/bin/gh /usr/local/bin/gh)" || { echo 'A008: GitHub CLI unavailable.' >&2; exit 12; }
GIT="$(resolve_exec /usr/bin/git)" || { echo 'A008: Git unavailable.' >&2; exit 11; }
NODE="$(resolve_exec /opt/homebrew/bin/node /usr/local/bin/node /usr/bin/node)" || { echo 'A008: Node unavailable.' >&2; exit 13; }

"$GH" auth status --hostname github.com >/dev/null 2>&1 || { echo 'A008: GitHub CLI authentication unavailable.' >&2; exit 21; }

BOOT_ROOT="$(/usr/bin/mktemp -d "$HOME/Downloads/.vexlife-prod-runtime-stream-log-a008.XXXXXX")"
cleanup(){ case "$BOOT_ROOT" in "$HOME/Downloads"/.vexlife-prod-runtime-stream-log-a008.*) /bin/rm -rf -- "$BOOT_ROOT";; esac; }
trap cleanup EXIT HUP INT TERM

SDK_ROOT="$BOOT_ROOT/Vextreme-SDK"
PAYLOAD="$BOOT_ROOT/PR149-RUNTIME-STREAM-LOG-DIAG-A008.payload.cjs"
FORM="$BOOT_ROOT/PR149-RUNTIME-STREAM-LOG-DIAG-A008.form.cjs"

echo 'A008: acquiring exact current VexLocalBridge source...'
"$GH" repo clone "$SDK_REPO" "$SDK_ROOT" -- --no-checkout >/dev/null
"$GIT" -C "$SDK_ROOT" checkout --detach "$SDK_HEAD" >/dev/null
[ "$("$GIT" -C "$SDK_ROOT" rev-parse HEAD)" = "$SDK_HEAD" ] || exit 23
[ "$("$GIT" -C "$SDK_ROOT" rev-parse 'HEAD^{tree}')" = "$SDK_TREE" ] || exit 24

CONTRACT="$SDK_ROOT/docs/private-continuity/vexrelay/current-contract.json"
"$NODE" - "$CONTRACT" "$CONTRACT_REF" "$RUNTIME_VERSION" <<'NODECHECK'
const fs=require('node:fs');
const [file,ref,runtime]=process.argv.slice(2);
const value=JSON.parse(fs.readFileSync(file,'utf8'));
if(value.contractRef!==ref) process.exit(27);
if(value.currentRuntimeVersion!==runtime) process.exit(28);
const route=value.routeSelection?.effectClassRoutes?.HOST_SEMANTIC_MODEL_RUNTIME;
if(route!=='adapter.vexlocalbridge.direct-host-effect') process.exit(29);
NODECHECK

fetch_exact_file() {
  local repo="$1" commit="$2" path="$3" blob="$4" out="$5"
  "$GH" api "repos/$repo/contents/$path?ref=$commit" --jq .content     | "$NODE" -e 'let s="";process.stdin.setEncoding("utf8");process.stdin.on("data",d=>s+=d);process.stdin.on("end",()=>process.stdout.write(Buffer.from(s.replace(/\s/g,""),"base64")));'     > "$out"
  [ "$("$GIT" hash-object "$out")" = "$blob" ] || { echo "A008: carrier member byte mismatch: $path" >&2; exit 30; }
}

fetch_exact_file "$CARRIER_REPO" "$CARRIER_CONTENT_COMMIT" "$PAYLOAD_PATH" "$PAYLOAD_BLOB" "$PAYLOAD"
fetch_exact_file "$CARRIER_REPO" "$CARRIER_CONTENT_COMMIT" "$FORM_PATH" "$FORM_BLOB" "$FORM"
"$NODE" --check "$PAYLOAD" >/dev/null
"$NODE" --check "$FORM" >/dev/null

echo
echo 'A008: reading current MLX/service log tails only.'
echo 'No model call or runtime restart will be performed.'
echo

FORMATION_JSON="$("$NODE" "$FORM" "$SDK_ROOT" "$PAYLOAD")"
printf '%s\n' "$FORMATION_JSON"
PACKAGE="$(printf '%s' "$FORMATION_JSON" | "$NODE" -e 'let s="";process.stdin.on("data",d=>s+=d);process.stdin.on("end",()=>process.stdout.write(JSON.parse(s).package));')"
SHA="$(printf '%s' "$FORMATION_JSON" | "$NODE" -e 'let s="";process.stdin.on("data",d=>s+=d);process.stdin.on("end",()=>process.stdout.write(JSON.parse(s).sha256));')"
[ "$(/usr/bin/shasum -a 256 "$PACKAGE" | /usr/bin/cut -d ' ' -f 1)" = "$SHA" ] || exit 32

VEX_EXPECTED_PACKAGE_SHA256="$SHA" /bin/bash "$SDK_ROOT/lib/vexlocalbridge/templates/RUN-VEX-TASK.command" "$PACKAGE"
