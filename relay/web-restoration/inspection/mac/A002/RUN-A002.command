#!/bin/bash
set -euo pipefail

SDK_REPOSITORY='vgong24/Vextreme-SDK'
TRUSTED_SDK_HEAD='8543c7d755faba04d4c4019c02e84f992667918a'
TRUSTED_SDK_TREE='6376f68279f0750ee6d3868b8472368199763064'
CARRIER_HEAD='36eb5c82becd97dcfcbc8fa67455a74d8540503a'
CARRIER_TREE='bea091ed67793931b838c7b412d9abce39a2e373'
PRODUCT_HEAD='d48f1c79ba4e70056cfc720199fca5f56660b583'

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd -P)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../../../.." && pwd -P)"
FORM_TASK="$SCRIPT_DIR/form-task.cjs"
PAYLOAD="$SCRIPT_DIR/payload.mjs"

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

BASH_BIN="$(resolve_exec /bin/bash)" || exit 10
GIT_BIN="$(resolve_exec /usr/bin/git)" || exit 11
GH_BIN="$(resolve_exec /opt/homebrew/bin/gh /usr/local/bin/gh)" || exit 12
NODE_BIN="$(resolve_exec /opt/homebrew/bin/node /usr/local/bin/node /usr/bin/node)" || exit 13
MKTEMP_BIN="$(resolve_exec /usr/bin/mktemp)" || exit 14
SHASUM_BIN="$(resolve_exec /usr/bin/shasum)" || exit 15
CUT_BIN="$(resolve_exec /usr/bin/cut)" || exit 16
CP_BIN="$(resolve_exec /bin/cp /usr/bin/cp)" || exit 17
RM_BIN="$(resolve_exec /bin/rm /usr/bin/rm)" || exit 18

[ "$(/usr/bin/uname -m)" = 'arm64' ] || { echo 'A002 requires Apple-silicon arm64.' >&2; exit 19; }
"$GH_BIN" auth status --hostname github.com >/dev/null 2>&1 || { echo 'GitHub authentication unavailable.' >&2; exit 20; }

COMPANION_HEAD="$("$GIT_BIN" -C "$REPO_ROOT" rev-parse HEAD)"
"$GIT_BIN" -C "$REPO_ROOT" merge-base --is-ancestor "$PRODUCT_HEAD" "$COMPANION_HEAD" || exit 21

DOWNLOADS="$HOME/Downloads"
ROOT="$("$MKTEMP_BIN" -d "$DOWNLOADS/.vexlife-pr436-java-inspection-a002.XXXXXX")"
TRUSTED_SDK_ROOT="$ROOT/trusted-sdk"
CARRIER_SDK_ROOT="$ROOT/carrier-sdk"
FORMED_ROOT="$ROOT/formed"
PRESERVE=0
cleanup() {
  if [ "$PRESERVE" -eq 0 ]; then
    case "$ROOT" in "$DOWNLOADS"/.vexlife-pr436-java-inspection-a002.*) "$RM_BIN" -rf -- "$ROOT" ;; esac
  else
    echo "A002 recovery root preserved: $ROOT" >&2
  fi
}
trap cleanup EXIT HUP INT TERM

echo 'A002: acquiring exact SDK sources...'
"$GH_BIN" repo clone "$SDK_REPOSITORY" "$TRUSTED_SDK_ROOT" -- --no-checkout >/dev/null
"$GIT_BIN" -C "$TRUSTED_SDK_ROOT" checkout --detach "$TRUSTED_SDK_HEAD" >/dev/null
"$GH_BIN" repo clone "$SDK_REPOSITORY" "$CARRIER_SDK_ROOT" -- --no-checkout >/dev/null
"$GIT_BIN" -C "$CARRIER_SDK_ROOT" checkout --detach "$CARRIER_HEAD" >/dev/null

[ "$("$GIT_BIN" -C "$TRUSTED_SDK_ROOT" rev-parse HEAD)" = "$TRUSTED_SDK_HEAD" ] || exit 22
[ "$("$GIT_BIN" -C "$TRUSTED_SDK_ROOT" rev-parse 'HEAD^{tree}')" = "$TRUSTED_SDK_TREE" ] || exit 23
[ "$("$GIT_BIN" -C "$CARRIER_SDK_ROOT" rev-parse HEAD)" = "$CARRIER_HEAD" ] || exit 24
[ "$("$GIT_BIN" -C "$CARRIER_SDK_ROOT" rev-parse 'HEAD^{tree}')" = "$CARRIER_TREE" ] || exit 25

mkdir -p "$FORMED_ROOT"
"$NODE_BIN" "$FORM_TASK" --trusted-sdk "$TRUSTED_SDK_ROOT" --carrier-sdk "$CARRIER_SDK_ROOT" --payload "$PAYLOAD" --output "$FORMED_ROOT"

FORMATION="$FORMED_ROOT/FORMATION.json"
PACKAGE="$("$NODE_BIN" -e 'const fs=require("node:fs");const x=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));process.stdout.write(x.packagePath)' "$FORMATION")"
EXPECTED="$("$NODE_BIN" -e 'const fs=require("node:fs");const x=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));process.stdout.write(x.packageSha256)' "$FORMATION")"
[ "$("$SHASUM_BIN" -a 256 "$PACKAGE" | "$CUT_BIN" -d ' ' -f 1)" = "$EXPECTED" ] || exit 26

PRESERVE=1
echo "A002: entering VexLocalBridge with package sha256 $EXPECTED"
set +e
VEX_EXPECTED_PACKAGE_SHA256="$EXPECTED" "$BASH_BIN" "$CARRIER_SDK_ROOT/lib/vexlocalbridge/templates/RUN-VEXLIFE-PROD-TASK.command" "$PACKAGE"
STATUS=$?
set -e

CAPSULE="$FORMED_ROOT/VLB-${EXPECTED:0:16}"
RESULTS="$CAPSULE/results"
[ -d "$RESULTS" ] || { echo "A002: no canonical result root; carrier exit=$STATUS" >&2; exit 27; }
COUNT="$(/usr/bin/find "$RESULTS" -type f -name 'RETURN-*.zip' | /usr/bin/wc -l | /usr/bin/tr -d ' ')"
[ "$COUNT" = '1' ] || { echo "A002: expected one RETURN, observed $COUNT" >&2; exit 28; }
RETURN_ZIP="$(/usr/bin/find "$RESULTS" -type f -name 'RETURN-*.zip' -print)"
RETURN_SHA="$("$SHASUM_BIN" -a 256 "$RETURN_ZIP" | "$CUT_BIN" -d ' ' -f 1)"
DELIVERY="$DOWNLOADS/RETURN-VEXLIFE-WEB-RESTORATION-JAVA-INSPECTION-A002.zip"

if [ -e "$DELIVERY" ]; then
  [ "$("$SHASUM_BIN" -a 256 "$DELIVERY" | "$CUT_BIN" -d ' ' -f 1)" = "$RETURN_SHA" ] || {
    echo 'A002: existing RETURN has different bytes.' >&2
    exit 29
  }
else
  "$CP_BIN" "$RETURN_ZIP" "$DELIVERY"
fi

echo ''
echo 'WEB_RESTORATION_JAVA_INSPECTION_A002=RETURN_READY'
echo "RETURN=$DELIVERY"
echo "RETURN_SHA256=$RETURN_SHA"
echo 'Upload that RETURN ZIP back to ChatGPT.'
PRESERVE=0
exit 0

# [VXG RealForever]
