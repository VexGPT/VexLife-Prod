#!/bin/bash
set -euo pipefail

# [VXG RealForever]
# PR148 lived host proof runner.
# This is proof-carrier source only; it is not part of PR #148 Product ancestry.

SDK_REPO='vgong24/Vextreme-SDK'
SDK_HEAD='8543c7d755faba04d4c4019c02e84f992667918a'
SDK_TREE='6376f68279f0750ee6d3868b8472368199763064'
CONTRACT_REF='source.vexrelay.current-contract/v1'
RUNTIME_VERSION='v0.2.0-RC1'

CARRIER_REPO='VexGPT/VexLife-Prod'
CARRIER_CONTENT_COMMIT='8ed026e46f4d95e794c583e0050f77635bc79e8c'
PAYLOAD_PATH='proof-carrier/PR148-LIVED-A004.payload.cjs'
PAYLOAD_BLOB='76258e2adf2a655fa8bbb646312b2276909c8133'
FORM_PATH='proof-carrier/PR148-LIVED-A004.form.cjs'
FORM_BLOB='3941b38abc903f0bc0afa0c32680fd4270938e3f'

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
  printf '%s\n' 'PR148 LIVED A004: GitHub CLI unavailable.' >&2
  exit 12
}
GIT="$(resolve_exec /usr/bin/git)" || {
  printf '%s\n' 'PR148 LIVED A004: Git unavailable.' >&2
  exit 11
}
NODE="$(resolve_exec /opt/homebrew/bin/node /usr/local/bin/node /usr/bin/node)" || {
  printf '%s\n' 'PR148 LIVED A004: Node unavailable.' >&2
  exit 13
}

"$GH" auth status --hostname github.com >/dev/null 2>&1 || {
  printf '%s\n' 'PR148 LIVED A004: GitHub CLI authentication unavailable.' >&2
  exit 21
}

BOOT_ROOT="$(/usr/bin/mktemp -d "$HOME/Downloads/.vexlife-prod-pr148-lived-a004.XXXXXX")"
cleanup() {
  case "$BOOT_ROOT" in
    "$HOME/Downloads"/.vexlife-prod-pr148-lived-a004.*) /bin/rm -rf -- "$BOOT_ROOT" ;;
  esac
}
trap cleanup EXIT HUP INT TERM

SDK_ROOT="$BOOT_ROOT/Vextreme-SDK"
PAYLOAD="$BOOT_ROOT/PR148-LIVED-A004.payload.cjs"
FORM="$BOOT_ROOT/PR148-LIVED-A004.form.cjs"

printf '%s\n' 'PR148 LIVED A004: acquiring exact current VexLocalBridge source...'
"$GH" repo clone "$SDK_REPO" "$SDK_ROOT" -- --no-checkout >/dev/null
"$GIT" -C "$SDK_ROOT" checkout --detach "$SDK_HEAD" >/dev/null

OBSERVED_HEAD="$("$GIT" -C "$SDK_ROOT" rev-parse HEAD)"
OBSERVED_TREE="$("$GIT" -C "$SDK_ROOT" rev-parse 'HEAD^{tree}')"
[ "$OBSERVED_HEAD" = "$SDK_HEAD" ] || {
  printf '%s\n' 'PR148 LIVED A004: SDK head mismatch.' >&2
  exit 23
}
[ "$OBSERVED_TREE" = "$SDK_TREE" ] || {
  printf '%s\n' 'PR148 LIVED A004: SDK tree mismatch.' >&2
  exit 24
}
[ -z "$("$GIT" -C "$SDK_ROOT" status --porcelain=v1 -uall)" ] || {
  printf '%s\n' 'PR148 LIVED A004: acquired SDK source is dirty.' >&2
  exit 25
}

CONTRACT="$SDK_ROOT/docs/private-continuity/vexrelay/current-contract.json"
"$NODE" - "$CONTRACT" "$CONTRACT_REF" "$RUNTIME_VERSION" <<'NODECHECK'
const fs=require('node:fs');
const [file,ref,runtime]=process.argv.slice(2);
const value=JSON.parse(fs.readFileSync(file,'utf8'));
if(value.contractRef!==ref) {
  console.error('PR148 LIVED A004: current Relay contractRef mismatch: '+value.contractRef);
  process.exit(27);
}
if(value.currentRuntimeVersion!==runtime) {
  console.error('PR148 LIVED A004: current Relay runtime mismatch: '+value.currentRuntimeVersion);
  process.exit(28);
}
const route=value.routeSelection?.effectClassRoutes?.HOST_SEMANTIC_MODEL_RUNTIME;
if(route!=='adapter.vexlocalbridge.direct-host-effect') {
  console.error('PR148 LIVED A004: current HOST_SEMANTIC_MODEL_RUNTIME route mismatch: '+route);
  process.exit(29);
}
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
    printf 'PR148 LIVED A004: carrier member byte mismatch: %s expected=%s observed=%s\n' "$path" "$expected_blob" "$observed" >&2
    exit 30
  }
}

fetch_exact_file "$CARRIER_REPO" "$CARRIER_CONTENT_COMMIT" "$PAYLOAD_PATH" "$PAYLOAD_BLOB" "$PAYLOAD"
fetch_exact_file "$CARRIER_REPO" "$CARRIER_CONTENT_COMMIT" "$FORM_PATH" "$FORM_BLOB" "$FORM"

"$NODE" --check "$PAYLOAD" >/dev/null
"$NODE" --check "$FORM" >/dev/null

printf '%s\n' ''
printf '%s\n' 'PR148 LIVED A004: SourceOwner already passed. This stage will open the real native Setup/Product.'
printf '%s\n' 'When VexLife opens in the browser:'
printf '%s\n' '  1. Make sure Viewable Thoughts is ON.'
printf '%s\n' '  2. Send: Use your web.fetch capability to fetch https://example.com/ and tell me the page title in one sentence. Do not answer from memory.'
printf '%s\n' '  3. While thoughts are still arriving, drag/scroll inside the live thought panel up and down.'
printf '%s\n' '  4. After the final answer, reopen View Process and confirm the same-session sequence is still inspectable.'
printf '%s\n' '  5. Then send: Reply with exactly: next loop is alive.'
printf '%s\n' ''
printf '%s\n' 'The machine proof will stop automatically after the web capability turn and the second Send both complete.'
printf '%s\n' ''

FORMATION_JSON="$("$NODE" "$FORM" "$SDK_ROOT" "$PAYLOAD")"
printf '%s\n' "$FORMATION_JSON"

PACKAGE="$(printf '%s' "$FORMATION_JSON" | "$NODE" -e 'let s="";process.stdin.on("data",d=>s+=d);process.stdin.on("end",()=>process.stdout.write(JSON.parse(s).package));')"
EXPECTED_PACKAGE_SHA="$(printf '%s' "$FORMATION_JSON" | "$NODE" -e 'let s="";process.stdin.on("data",d=>s+=d);process.stdin.on("end",()=>process.stdout.write(JSON.parse(s).sha256));')"

[ -f "$PACKAGE" ] || {
  printf '%s\n' 'PR148 LIVED A004: canonical direct-host package is missing after formation.' >&2
  exit 31
}
OBSERVED_PACKAGE_SHA="$(/usr/bin/shasum -a 256 "$PACKAGE" | /usr/bin/cut -d ' ' -f 1)"
[ "$OBSERVED_PACKAGE_SHA" = "$EXPECTED_PACKAGE_SHA" ] || {
  printf '%s\n' 'PR148 LIVED A004: canonical direct-host package digest mismatch.' >&2
  exit 32
}

printf 'PR148 LIVED A004: executing canonical direct-host package %s\n' "$PACKAGE"
set +e
VEX_EXPECTED_PACKAGE_SHA256="$EXPECTED_PACKAGE_SHA" /bin/bash "$SDK_ROOT/lib/vexlocalbridge/templates/RUN-VEX-TASK.command" "$PACKAGE"
STATUS=$?
set -e

printf 'PR148 LIVED A004: VexLocalBridge exit=%s\n' "$STATUS"
exit "$STATUS"
