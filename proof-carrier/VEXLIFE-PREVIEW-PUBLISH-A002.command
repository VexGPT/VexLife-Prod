#!/bin/bash
set -euo pipefail

# [VXG RealForever]
# VexLife Preview publication A002.
# Direct VexLocalBridge lifecycle carrier; not a vex.relay.task/v2 RELEASE operation.

SDK_REPO='vgong24/Vextreme-SDK'
SDK_HEAD='8543c7d755faba04d4c4019c02e84f992667918a'
SDK_TREE='6376f68279f0750ee6d3868b8472368199763064'
CONTRACT_REF='source.vexrelay.current-contract/v1'
RUNTIME_VERSION='v0.2.0-RC1'

CARRIER_REPO='VexGPT/VexLife-Prod'
CARRIER_CONTENT_COMMIT='ded957253f9ea00bfb01404f880369408e90f5d6'
PAYLOAD_PATH='proof-carrier/VEXLIFE-PREVIEW-PUBLISH-A002.payload.cjs'
PAYLOAD_BLOB='19c5e5264b5d658c1fad712353b4ac806c478531'
FORM_PATH='proof-carrier/VEXLIFE-PREVIEW-PUBLISH-A002.form.cjs'
FORM_BLOB='9afd0ddb63fb52b0a0300649ff7fb4744d0ceb35'

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
  printf '%s
' 'VexLife Preview A002: GitHub CLI unavailable.' >&2
  exit 12
}
GIT="$(resolve_exec /usr/bin/git)" || {
  printf '%s
' 'VexLife Preview A002: Git unavailable.' >&2
  exit 11
}
NODE="$(resolve_exec /opt/homebrew/bin/node /usr/local/bin/node /usr/bin/node)" || {
  printf '%s
' 'VexLife Preview A002: Node unavailable.' >&2
  exit 13
}

"$GH" auth status --hostname github.com >/dev/null 2>&1 || {
  printf '%s
' 'VexLife Preview A002: GitHub CLI authentication unavailable.' >&2
  exit 21
}

BOOT_ROOT="$(/usr/bin/mktemp -d "$HOME/Downloads/.vexlife-preview-publish-a002.XXXXXX")"
cleanup() {
  case "$BOOT_ROOT" in
    "$HOME/Downloads"/.vexlife-preview-publish-a002.*) /bin/rm -rf -- "$BOOT_ROOT" ;;
  esac
}
trap cleanup EXIT HUP INT TERM

SDK_ROOT="$BOOT_ROOT/Vextreme-SDK"
PAYLOAD="$BOOT_ROOT/VEXLIFE-PREVIEW-PUBLISH-A002.payload.cjs"
FORM="$BOOT_ROOT/VEXLIFE-PREVIEW-PUBLISH-A002.form.cjs"

printf '%s
' 'VexLife Preview A002: acquiring exact current VexLocalBridge source...'
"$GH" repo clone "$SDK_REPO" "$SDK_ROOT" -- --no-checkout >/dev/null
"$GIT" -C "$SDK_ROOT" checkout --detach "$SDK_HEAD" >/dev/null

OBSERVED_HEAD="$("$GIT" -C "$SDK_ROOT" rev-parse HEAD)"
OBSERVED_TREE="$("$GIT" -C "$SDK_ROOT" rev-parse 'HEAD^{tree}')"
[ "$OBSERVED_HEAD" = "$SDK_HEAD" ] || {
  printf '%s
' 'VexLife Preview A002: SDK head mismatch.' >&2
  exit 23
}
[ "$OBSERVED_TREE" = "$SDK_TREE" ] || {
  printf '%s
' 'VexLife Preview A002: SDK tree mismatch.' >&2
  exit 24
}
[ -z "$("$GIT" -C "$SDK_ROOT" status --porcelain=v1 -uall)" ] || {
  printf '%s
' 'VexLife Preview A002: acquired SDK source is dirty.' >&2
  exit 25
}

CONTRACT="$SDK_ROOT/docs/private-continuity/vexrelay/current-contract.json"
"$NODE" - "$CONTRACT" "$CONTRACT_REF" "$RUNTIME_VERSION" <<'NODECHECK'
const fs=require('node:fs');
const [file,ref,runtime]=process.argv.slice(2);
const value=JSON.parse(fs.readFileSync(file,'utf8'));
if(value.contractRef!==ref) {
  console.error('VexLife Preview A002: current contractRef mismatch: '+value.contractRef);
  process.exit(27);
}
if(value.currentRuntimeVersion!==runtime) {
  console.error('VexLife Preview A002: current runtime mismatch: '+value.currentRuntimeVersion);
  process.exit(28);
}
if(!Array.isArray(value.hostProfiles) || !value.hostProfiles.includes('macos-victor')) {
  console.error('VexLife Preview A002: macos-victor is not a current host profile.');
  process.exit(29);
}
const direct=(value.acceptedAdapters||[]).find((item)=>item.adapterRef==='adapter.vexlocalbridge.direct-host-effect');
if(!direct || direct.state!=='TERMINAL_ACCEPTED_CURRENT') {
  console.error('VexLife Preview A002: current direct-host VexLocalBridge adapter is unavailable.');
  process.exit(30);
}
NODECHECK

fetch_exact_file() {
  local repo="$1"
  local commit="$2"
  local path="$3"
  local expected_blob="$4"
  local out="$5"

  "$GH" api "repos/$repo/contents/$path?ref=$commit" --jq .content     | "$NODE" -e 'let s="";process.stdin.setEncoding("utf8");process.stdin.on("data",d=>s+=d);process.stdin.on("end",()=>process.stdout.write(Buffer.from(s.replace(/\\s/g,""),"base64")));'     > "$out"

  local observed
  observed="$("$GIT" hash-object "$out")"
  [ "$observed" = "$expected_blob" ] || {
    printf 'VexLife Preview A002: carrier member byte mismatch: %s expected=%s observed=%s
' "$path" "$expected_blob" "$observed" >&2
    exit 31
  }
}

fetch_exact_file "$CARRIER_REPO" "$CARRIER_CONTENT_COMMIT" "$PAYLOAD_PATH" "$PAYLOAD_BLOB" "$PAYLOAD"
fetch_exact_file "$CARRIER_REPO" "$CARRIER_CONTENT_COMMIT" "$FORM_PATH" "$FORM_BLOB" "$FORM"

"$NODE" --check "$PAYLOAD" >/dev/null
"$NODE" --check "$FORM" >/dev/null

printf '%s
' 'VexLife Preview A002: forming fresh digest-bound host package...'
FORMATION_JSON="$("$NODE" "$FORM" "$SDK_ROOT" "$PAYLOAD")"
printf '%s
' "$FORMATION_JSON"

PACKAGE="$(printf '%s' "$FORMATION_JSON" | "$NODE" -e 'let s="";process.stdin.on("data",d=>s+=d);process.stdin.on("end",()=>process.stdout.write(JSON.parse(s).package));')"
EXPECTED_PACKAGE_SHA="$(printf '%s' "$FORMATION_JSON" | "$NODE" -e 'let s="";process.stdin.on("data",d=>s+=d);process.stdin.on("end",()=>process.stdout.write(JSON.parse(s).sha256));')"

[ -f "$PACKAGE" ] || {
  printf '%s
' 'VexLife Preview A002: canonical host package is missing after formation.' >&2
  exit 32
}
OBSERVED_PACKAGE_SHA="$(/usr/bin/shasum -a 256 "$PACKAGE" | /usr/bin/cut -d ' ' -f 1)"
[ "$OBSERVED_PACKAGE_SHA" = "$EXPECTED_PACKAGE_SHA" ] || {
  printf '%s
' 'VexLife Preview A002: canonical host package digest mismatch.' >&2
  exit 33
}

printf 'VexLife Preview A002: executing %s
' "$PACKAGE"
set +e
VEX_EXPECTED_PACKAGE_SHA256="$EXPECTED_PACKAGE_SHA" /bin/bash "$SDK_ROOT/lib/vexlocalbridge/templates/RUN-VEX-TASK.command" "$PACKAGE"
STATUS=$?
set -e

printf 'VexLife Preview A002: VexLocalBridge exit=%s
' "$STATUS"
printf '%s
' 'If a RETURN ZIP was formed, Finder should select it. Upload that one ZIP back to this chat.'
exit "$STATUS"
