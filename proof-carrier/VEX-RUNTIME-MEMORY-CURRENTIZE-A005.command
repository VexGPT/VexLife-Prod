#!/bin/bash
set -euo pipefail

# [VXG RealForever]
# Exact one-cache runtime currentization for PR193/#188 A005; A004 ended before host preflight effects due a carrier-local JavaScript shadowing defect.
# Source/build proof first; then only the Product Entry/Setup Family's lawful no-acquisition convergence effect.

SDK_REPO='vgong24/Vextreme-SDK'
SDK_HEAD='8543c7d755faba04d4c4019c02e84f992667918a'
SDK_TREE='6376f68279f0750ee6d3868b8472368199763064'
CONTRACT_REF='source.vexrelay.current-contract/v1'
RUNTIME_VERSION='v0.2.0-RC1'

CARRIER_REPO='VexGPT/VexLife-Prod'
CARRIER_CONTENT_COMMIT='1c83a2aa42e498514866c8c8a1b92d58a45b8d67'
PAYLOAD_PATH='proof-carrier/VEX-RUNTIME-MEMORY-CURRENTIZE-A005.payload.cjs'
PAYLOAD_BLOB='f43fa308ff4dc19505c29b9d9dc8ef3a369f8f08'
FORM_PATH='proof-carrier/VEX-RUNTIME-MEMORY-CURRENTIZE-A005.form.cjs'
FORM_BLOB='53d945b731ead5523dd27221ae76f0b35be13f0e'

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
  printf '%s\n' 'RUNTIME MEMORY CURRENTIZE A005: GitHub CLI unavailable.' >&2
  exit 12
}
GIT="$(resolve_exec /usr/bin/git)" || {
  printf '%s\n' 'RUNTIME MEMORY CURRENTIZE A005: Git unavailable.' >&2
  exit 11
}
NODE="$(resolve_exec /opt/homebrew/bin/node /usr/local/bin/node /usr/bin/node)" || {
  printf '%s\n' 'RUNTIME MEMORY CURRENTIZE A005: Node unavailable.' >&2
  exit 13
}

"$GH" auth status --hostname github.com >/dev/null 2>&1 || {
  printf '%s\n' 'RUNTIME MEMORY CURRENTIZE A005: GitHub CLI authentication unavailable.' >&2
  exit 21
}

BOOT_ROOT="$(/usr/bin/mktemp -d "$HOME/Downloads/.vexlife-prod-runtime-memory-currentize-a005.XXXXXX")"
cleanup() {
  case "$BOOT_ROOT" in
    "$HOME/Downloads"/.vexlife-prod-runtime-memory-currentize-a005.*) /bin/rm -rf -- "$BOOT_ROOT" ;;
  esac
}
trap cleanup EXIT HUP INT TERM

SDK_ROOT="$BOOT_ROOT/Vextreme-SDK"
PAYLOAD="$BOOT_ROOT/VEX-RUNTIME-MEMORY-CURRENTIZE-A005.payload.cjs"
FORM="$BOOT_ROOT/VEX-RUNTIME-MEMORY-CURRENTIZE-A005.form.cjs"

printf '%s\n' 'RUNTIME MEMORY CURRENTIZE A005: acquiring exact current VexLocalBridge source...'
"$GH" repo clone "$SDK_REPO" "$SDK_ROOT" -- --no-checkout >/dev/null
"$GIT" -C "$SDK_ROOT" checkout --detach "$SDK_HEAD" >/dev/null

[ "$("$GIT" -C "$SDK_ROOT" rev-parse HEAD)" = "$SDK_HEAD" ] || {
  printf '%s\n' 'RUNTIME MEMORY CURRENTIZE A005: SDK head mismatch.' >&2
  exit 23
}
[ "$("$GIT" -C "$SDK_ROOT" rev-parse 'HEAD^{tree}')" = "$SDK_TREE" ] || {
  printf '%s\n' 'RUNTIME MEMORY CURRENTIZE A005: SDK tree mismatch.' >&2
  exit 24
}
[ -z "$("$GIT" -C "$SDK_ROOT" status --porcelain=v1 -uall)" ] || {
  printf '%s\n' 'RUNTIME MEMORY CURRENTIZE A005: acquired SDK source is dirty.' >&2
  exit 25
}

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
  local repo="$1"
  local commit="$2"
  local path="$3"
  local expected_blob="$4"
  local out="$5"

  "$GH" api "repos/$repo/contents/$path?ref=$commit" --jq .content \
    | "$NODE" -e 'let s="";process.stdin.setEncoding("utf8");process.stdin.on("data",d=>s+=d);process.stdin.on("end",()=>process.stdout.write(Buffer.from(s.replace(/\s/g,""),"base64")));' \
    > "$out"

  local observed
  observed="$("$GIT" hash-object "$out")"
  [ "$observed" = "$expected_blob" ] || {
    printf 'RUNTIME MEMORY CURRENTIZE A005: carrier byte mismatch: %s expected=%s observed=%s\n' "$path" "$expected_blob" "$observed" >&2
    exit 30
  }
}

fetch_exact_file "$CARRIER_REPO" "$CARRIER_CONTENT_COMMIT" "$PAYLOAD_PATH" "$PAYLOAD_BLOB" "$PAYLOAD"
fetch_exact_file "$CARRIER_REPO" "$CARRIER_CONTENT_COMMIT" "$FORM_PATH" "$FORM_BLOB" "$FORM"

"$NODE" --check "$PAYLOAD" >/dev/null
"$NODE" --check "$FORM" >/dev/null

printf '%s\n' 'RUNTIME MEMORY CURRENTIZE A005: source/build proof runs first; only then may the existing Product Front Door rotate the exact-owned runtime/service without model acquisition.'\nprintf '%s\n' 'No Conversation Send is performed by this carrier.'

FORMATION_JSON="$("$NODE" "$FORM" "$SDK_ROOT" "$PAYLOAD")"
printf '%s\n' "$FORMATION_JSON"

PACKAGE="$(printf '%s' "$FORMATION_JSON" | "$NODE" -e 'let s="";process.stdin.on("data",d=>s+=d);process.stdin.on("end",()=>process.stdout.write(JSON.parse(s).package));')"
EXPECTED_PACKAGE_SHA="$(printf '%s' "$FORMATION_JSON" | "$NODE" -e 'let s="";process.stdin.on("data",d=>s+=d);process.stdin.on("end",()=>process.stdout.write(JSON.parse(s).sha256));')"

[ -f "$PACKAGE" ] || {
  printf '%s\n' 'RUNTIME MEMORY CURRENTIZE A005: canonical diagnostic package is missing after formation.' >&2
  exit 31
}
[ "$(/usr/bin/shasum -a 256 "$PACKAGE" | /usr/bin/cut -d ' ' -f 1)" = "$EXPECTED_PACKAGE_SHA" ] || {
  printf '%s\n' 'RUNTIME MEMORY CURRENTIZE A005: canonical diagnostic package digest mismatch.' >&2
  exit 32
}

VEX_EXPECTED_PACKAGE_SHA256="$EXPECTED_PACKAGE_SHA" /bin/bash "$SDK_ROOT/lib/vexlocalbridge/templates/RUN-VEX-TASK.command" "$PACKAGE"
