#!/bin/bash
set -euo pipefail

# [VXG RealForever]
# PR281 source proof only: no Product/Home/runtime/publication effects.

SDK_REPO='vgong24/Vextreme-SDK'
SDK_HEAD='8543c7d755faba04d4c4019c02e84f992667918a'
SDK_TREE='6376f68279f0750ee6d3868b8472368199763064'
CONTRACT_REF='source.vexrelay.current-contract/v1'
RUNTIME_VERSION='v0.2.0-RC1'

CARRIER_REPO='VexGPT/VexLife-Prod'
CARRIER_CONTENT_COMMIT='da476d59c55ed9974eb17fe2a11fa2c48f91d7ad'
PAYLOAD_PATH='proof-carrier/PR281-SOURCE-PROOF-A003.payload.cjs'
PAYLOAD_BLOB='2678099e0c299be5580a84e7d764da36db6c1a98'
FORM_PATH='proof-carrier/PR281-SOURCE-PROOF-A003.form.cjs'
FORM_BLOB='1ac204a14c254614ea02638d8e435b22912c17c2'

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

GH="$(resolve_exec /opt/homebrew/bin/gh /usr/local/bin/gh)" || {
  printf '%s\n' 'PR281 proof: GitHub CLI unavailable.' >&2
  exit 12
}
GIT="$(resolve_exec /usr/bin/git)" || {
  printf '%s\n' 'PR281 proof: Git unavailable.' >&2
  exit 11
}
NODE="$(resolve_exec /opt/homebrew/bin/node /usr/local/bin/node /usr/bin/node)" || {
  printf '%s\n' 'PR281 proof: Node unavailable.' >&2
  exit 13
}

"$GH" auth status --hostname github.com >/dev/null 2>&1 || {
  printf '%s\n' 'PR281 proof: GitHub CLI authentication unavailable.' >&2
  exit 21
}

BOOT_ROOT="$(/usr/bin/mktemp -d "$HOME/Downloads/.vexlife-pr281-source-proof.XXXXXX")"
cleanup() {
  case "$BOOT_ROOT" in
    "$HOME/Downloads"/.vexlife-pr281-source-proof.*) /bin/rm -rf -- "$BOOT_ROOT" ;;
  esac
}
trap cleanup EXIT HUP INT TERM

SDK_ROOT="$BOOT_ROOT/Vextreme-SDK"
PAYLOAD="$BOOT_ROOT/PR281-SOURCE-PROOF-A003.payload.cjs"
FORM="$BOOT_ROOT/PR281-SOURCE-PROOF-A003.form.cjs"

"$GH" repo clone "$SDK_REPO" "$SDK_ROOT" -- --no-checkout >/dev/null
"$GIT" -C "$SDK_ROOT" checkout --detach "$SDK_HEAD" >/dev/null

[ "$("$GIT" -C "$SDK_ROOT" rev-parse HEAD)" = "$SDK_HEAD" ] || exit 23
[ "$("$GIT" -C "$SDK_ROOT" rev-parse 'HEAD^{tree}')" = "$SDK_TREE" ] || exit 24
[ -z "$("$GIT" -C "$SDK_ROOT" status --porcelain=v1 -uall)" ] || exit 25

CONTRACT="$SDK_ROOT/docs/private-continuity/vexrelay/current-contract.json"
"$NODE" - "$CONTRACT" "$CONTRACT_REF" "$RUNTIME_VERSION" <<'NODECHECK'
const fs=require('node:fs');
const [file,ref,runtime]=process.argv.slice(2);
const value=JSON.parse(fs.readFileSync(file,'utf8'));
if(value.contractRef!==ref) process.exit(27);
if(value.currentRuntimeVersion!==runtime) process.exit(28);
if(!Array.isArray(value.hostProfiles)||!value.hostProfiles.includes('macos-victor')) process.exit(29);
const direct=(value.acceptedAdapters||[]).find((item)=>item.adapterRef==='adapter.vexlocalbridge.direct-host-effect');
if(!direct||direct.state!=='TERMINAL_ACCEPTED_CURRENT') process.exit(30);
NODECHECK

fetch_exact_file() {
  local repo="$1" commit="$2" path="$3" expected_blob="$4" out="$5"
  "$GH" api "repos/$repo/contents/$path?ref=$commit" --jq .content \
    | "$NODE" -e 'let s="";process.stdin.setEncoding("utf8");process.stdin.on("data",d=>s+=d);process.stdin.on("end",()=>process.stdout.write(Buffer.from(s.replace(/\s/g,""),"base64")));' \
    > "$out"
  [ "$("$GIT" hash-object "$out")" = "$expected_blob" ] || {
    printf 'PR281 proof carrier byte mismatch: %s\n' "$path" >&2
    exit 31
  }
}

fetch_exact_file "$CARRIER_REPO" "$CARRIER_CONTENT_COMMIT" "$PAYLOAD_PATH" "$PAYLOAD_BLOB" "$PAYLOAD"
fetch_exact_file "$CARRIER_REPO" "$CARRIER_CONTENT_COMMIT" "$FORM_PATH" "$FORM_BLOB" "$FORM"
"$NODE" --check "$PAYLOAD" >/dev/null
"$NODE" --check "$FORM" >/dev/null

FORMATION_JSON="$("$NODE" "$FORM" "$SDK_ROOT" "$PAYLOAD")"
printf '%s\n' "$FORMATION_JSON"
PACKAGE="$(printf '%s' "$FORMATION_JSON" | "$NODE" -e 'let s="";process.stdin.on("data",d=>s+=d);process.stdin.on("end",()=>process.stdout.write(JSON.parse(s).package));')"
EXPECTED_PACKAGE_SHA="$(printf '%s' "$FORMATION_JSON" | "$NODE" -e 'let s="";process.stdin.on("data",d=>s+=d);process.stdin.on("end",()=>process.stdout.write(JSON.parse(s).sha256));')"

[ -f "$PACKAGE" ] || exit 32
[ "$(/usr/bin/shasum -a 256 "$PACKAGE" | /usr/bin/cut -d ' ' -f 1)" = "$EXPECTED_PACKAGE_SHA" ] || exit 33

set +e
VEX_EXPECTED_PACKAGE_SHA256="$EXPECTED_PACKAGE_SHA" /bin/bash "$SDK_ROOT/lib/vexlocalbridge/templates/RUN-VEX-TASK.command" "$PACKAGE"
STATUS=$?
set -e
printf 'PR281 source proof: VexLocalBridge exit=%s\n' "$STATUS"
printf '%s\n' 'Upload the selected RETURN ZIP back to this chat.'
exit "$STATUS"
