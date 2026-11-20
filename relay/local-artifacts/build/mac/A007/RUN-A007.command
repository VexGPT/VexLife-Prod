#!/bin/bash
set -euo pipefail

CONTINUITY='[VXG RealForever]'
SDK_REPOSITORY='vgong24/Vextreme-SDK'
TRUSTED_SDK_HEAD='8543c7d755faba04d4c4019c02e84f992667918a'
TRUSTED_SDK_TREE='6376f68279f0750ee6d3868b8472368199763064'
CARRIER_HEAD='36eb5c82becd97dcfcbc8fa67455a74d8540503a'
CARRIER_TREE='bea091ed67793931b838c7b412d9abce39a2e373'
PRODUCT_REPOSITORY='VexGPT/VexLife-Prod'
PRODUCT_BASE='3c8db58e2caee71997b6d8e1adeae89b5956eb56'
PRODUCT_HEAD='3165b2009acc84b8da38b45f9ec5bdc261387c2b'
PRODUCT_TREE='c86ad042177af0451fa0c35a0fdfec8bf0b745e6'
PRODUCT_BRANCH='VXG-111926-vexlife-local-artifacts-dmg-lived-proof-a001'

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd -P)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../../../.." && pwd -P)"
FORM_TASK="$SCRIPT_DIR/form-task.cjs"
PAYLOAD="$SCRIPT_DIR/payload.mjs"
CONSUMER="$SCRIPT_DIR/consume-return.cjs"

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

BASH_BIN="$(resolve_exec /bin/bash)" || { echo 'Local Artifacts build: Bash unavailable.' >&2; exit 10; }
GIT_BIN="$(resolve_exec /usr/bin/git)" || { echo 'Local Artifacts build: Git unavailable.' >&2; exit 11; }
GH_BIN="$(resolve_exec /opt/homebrew/bin/gh /usr/local/bin/gh)" || { echo 'Local Artifacts build: GitHub CLI unavailable.' >&2; exit 12; }
NODE_BIN="$(resolve_exec /opt/homebrew/bin/node /usr/local/bin/node /usr/bin/node)" || { echo 'Local Artifacts build: Node unavailable.' >&2; exit 13; }
MKTEMP_BIN="$(resolve_exec /usr/bin/mktemp)" || { echo 'Local Artifacts build: mktemp unavailable.' >&2; exit 14; }
RM_BIN="$(resolve_exec /bin/rm /usr/bin/rm)" || { echo 'Local Artifacts build: rm unavailable.' >&2; exit 15; }
SHASUM_BIN="$(resolve_exec /usr/bin/shasum)" || { echo 'Local Artifacts build: shasum unavailable.' >&2; exit 16; }
CUT_BIN="$(resolve_exec /usr/bin/cut)" || { echo 'Local Artifacts build: cut unavailable.' >&2; exit 17; }
CP_BIN="$(resolve_exec /bin/cp /usr/bin/cp)" || { echo 'Local Artifacts build: cp unavailable.' >&2; exit 18; }

ARCH="$(/usr/bin/uname -m)"
[ "$ARCH" = 'arm64' ] || { echo "Local Artifacts build: expected Apple-silicon arm64, observed $ARCH." >&2; exit 19; }

"$GH_BIN" auth status --hostname github.com >/dev/null 2>&1 || {
  echo 'Local Artifacts build: GitHub authentication unavailable.' >&2
  echo 'Run: gh auth login --hostname github.com --web' >&2
  exit 20
}

for required in "$FORM_TASK" "$PAYLOAD" "$CONSUMER"; do
  [ -f "$required" ] || { echo "Local Artifacts build: companion source missing: $required" >&2; exit 21; }
done

COMPANION_HEAD="$("$GIT_BIN" -C "$REPO_ROOT" rev-parse HEAD)"
"$GIT_BIN" -C "$REPO_ROOT" merge-base --is-ancestor "$PRODUCT_HEAD" "$COMPANION_HEAD" || {
  echo 'Local Artifacts build: companion checkout is not descended from exact #433 head.' >&2
  exit 22
}

DOWNLOADS="$HOME/Downloads"
ROOT="$("$MKTEMP_BIN" -d "$DOWNLOADS/.vexlife-pr429-local-artifacts-build-a007.XXXXXX")"
TRUSTED_SDK_ROOT="$ROOT/trusted-sdk"
CARRIER_SDK_ROOT="$ROOT/carrier-sdk"
PRODUCT_ROOT="$ROOT/product"
FORMED_ROOT="$ROOT/formed"
PRODUCT_BUNDLE="$ROOT/vexlife-prod.bundle"
PRESERVE_ROOT=0

cleanup() {
  if [ "$PRESERVE_ROOT" -eq 1 ]; then
    printf 'Local Artifacts build recovery root preserved: %s\n' "$ROOT" >&2
    return 0
  fi
  case "$ROOT" in
    "$DOWNLOADS"/.vexlife-pr429-local-artifacts-build-a007.*) "$RM_BIN" -rf -- "$ROOT" ;;
    *) return 1 ;;
  esac
}
trap cleanup EXIT HUP INT TERM

echo 'Local Artifacts build: acquiring exact accepted SDK builder source...'
"$GH_BIN" repo clone "$SDK_REPOSITORY" "$TRUSTED_SDK_ROOT" -- --no-checkout >/dev/null
"$GIT_BIN" -C "$TRUSTED_SDK_ROOT" checkout --detach "$TRUSTED_SDK_HEAD" >/dev/null

echo 'Local Artifacts build: acquiring exact #1959 VexLife-Prod host carrier...'
"$GH_BIN" repo clone "$SDK_REPOSITORY" "$CARRIER_SDK_ROOT" -- --no-checkout >/dev/null
"$GIT_BIN" -C "$CARRIER_SDK_ROOT" checkout --detach "$CARRIER_HEAD" >/dev/null

echo 'Local Artifacts build: acquiring exact #433 corrected Product candidate...'
"$GH_BIN" repo clone "$PRODUCT_REPOSITORY" "$PRODUCT_ROOT" -- --no-checkout >/dev/null
"$GIT_BIN" -C "$PRODUCT_ROOT" checkout --detach "$PRODUCT_HEAD" >/dev/null

[ "$("$GIT_BIN" -C "$TRUSTED_SDK_ROOT" rev-parse HEAD)" = "$TRUSTED_SDK_HEAD" ] || { echo 'Local Artifacts build: trusted SDK head mismatch.' >&2; exit 23; }
[ "$("$GIT_BIN" -C "$TRUSTED_SDK_ROOT" rev-parse 'HEAD^{tree}')" = "$TRUSTED_SDK_TREE" ] || { echo 'Local Artifacts build: trusted SDK tree mismatch.' >&2; exit 24; }
[ "$("$GIT_BIN" -C "$CARRIER_SDK_ROOT" rev-parse HEAD)" = "$CARRIER_HEAD" ] || { echo 'Local Artifacts build: carrier head mismatch.' >&2; exit 25; }
[ "$("$GIT_BIN" -C "$CARRIER_SDK_ROOT" rev-parse 'HEAD^{tree}')" = "$CARRIER_TREE" ] || { echo 'Local Artifacts build: carrier tree mismatch.' >&2; exit 26; }
[ "$("$GIT_BIN" -C "$PRODUCT_ROOT" rev-parse HEAD)" = "$PRODUCT_HEAD" ] || { echo 'Local Artifacts build: Product head mismatch.' >&2; exit 27; }
[ "$("$GIT_BIN" -C "$PRODUCT_ROOT" rev-parse 'HEAD^{tree}')" = "$PRODUCT_TREE" ] || { echo 'Local Artifacts build: Product tree mismatch.' >&2; exit 28; }
"$GIT_BIN" -C "$PRODUCT_ROOT" merge-base --is-ancestor "$PRODUCT_BASE" "$PRODUCT_HEAD" || { echo 'Local Artifacts build: Product base is not ancestor.' >&2; exit 29; }

echo 'Local Artifacts build: forming immutable Product bundle...'
"$GIT_BIN" -C "$PRODUCT_ROOT" update-ref refs/heads/vexlife-local-artifacts-proof-a007 "$PRODUCT_HEAD"
"$GIT_BIN" -C "$PRODUCT_ROOT" bundle create "$PRODUCT_BUNDLE" refs/heads/vexlife-local-artifacts-proof-a007
"$GIT_BIN" -C "$PRODUCT_ROOT" bundle verify "$PRODUCT_BUNDLE" >/dev/null
[ -s "$PRODUCT_BUNDLE" ] || { echo 'Local Artifacts build: Product bundle missing.' >&2; exit 30; }

mkdir -p "$FORMED_ROOT"
echo 'Local Artifacts build: forming source-managed host task...'
"$NODE_BIN" "$FORM_TASK" \
  --trusted-sdk "$TRUSTED_SDK_ROOT" \
  --carrier-sdk "$CARRIER_SDK_ROOT" \
  --payload "$PAYLOAD" \
  --product-bundle "$PRODUCT_BUNDLE" \
  --output "$FORMED_ROOT"

FORMATION="$FORMED_ROOT/FORMATION.json"
[ -f "$FORMATION" ] || { echo 'Local Artifacts build: formation receipt missing.' >&2; exit 31; }

PACKAGE="$("$NODE_BIN" -e 'const fs=require("node:fs");const x=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));process.stdout.write(x.packagePath)' "$FORMATION")"
EXPECTED="$("$NODE_BIN" -e 'const fs=require("node:fs");const x=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));process.stdout.write(x.packageSha256)' "$FORMATION")"
[ -f "$PACKAGE" ] || { echo 'Local Artifacts build: formed package missing.' >&2; exit 32; }
OBSERVED="$("$SHASUM_BIN" -a 256 "$PACKAGE" | "$CUT_BIN" -d ' ' -f 1)"
[ "$OBSERVED" = "$EXPECTED" ] || { echo 'Local Artifacts build: formed package digest mismatch.' >&2; exit 33; }

PRESERVE_ROOT=1
echo "Local Artifacts build: entering VexLocalBridge with package sha256 $EXPECTED"
set +e
VEX_EXPECTED_PACKAGE_SHA256="$EXPECTED" \
  "$BASH_BIN" "$CARRIER_SDK_ROOT/lib/vexlocalbridge/templates/RUN-VEXLIFE-PROD-TASK.command" "$PACKAGE"
CARRIER_STATUS=$?
set -e

CAPSULE_ROOT="$FORMED_ROOT/VLB-${EXPECTED:0:16}"
RESULT_ROOT="$CAPSULE_ROOT/results"
[ -d "$RESULT_ROOT" ] || {
  echo "Local Artifacts build: canonical result root missing; carrier exit=$CARRIER_STATUS" >&2
  exit 34
}

RETURN_COUNT="$(/usr/bin/find "$RESULT_ROOT" -type f -name 'RETURN-*.zip' | /usr/bin/wc -l | /usr/bin/tr -d ' ')"
[ "$RETURN_COUNT" = '1' ] || { echo "Local Artifacts build: expected one RETURN, observed $RETURN_COUNT." >&2; exit 35; }
RETURN_ZIP="$(/usr/bin/find "$RESULT_ROOT" -type f -name 'RETURN-*.zip' -print)"
RETURN_SHA="$("$SHASUM_BIN" -a 256 "$RETURN_ZIP" | "$CUT_BIN" -d ' ' -f 1)"

DELIVERY_DIR="$DOWNLOADS/VexLife-Local-Artifacts-Mac-3165b200"
DELIVERY="$DELIVERY_DIR/BUILD-DELIVERY.json"
DMG="$DELIVERY_DIR/VexLife-macOS-AppleSilicon.dmg"
HUMAN_DMG="$DOWNLOADS/VexLife-Local-Artifacts-3165b200.dmg"
DELIVERED_RETURN="$DELIVERY_DIR/RETURN-VEXLIFE-LOCAL-ARTIFACTS-MAC-BUILD-A007.zip"

/bin/mkdir -p "$DELIVERY_DIR"
if [ -e "$DELIVERED_RETURN" ]; then
  EXISTING_RETURN_SHA="$("$SHASUM_BIN" -a 256 "$DELIVERED_RETURN" | "$CUT_BIN" -d ' ' -f 1)"
  [ "$EXISTING_RETURN_SHA" = "$RETURN_SHA" ] || {
    echo 'Local Artifacts build: existing delivered RETURN has different bytes.' >&2
    exit 36
  }
else
  "$CP_BIN" "$RETURN_ZIP" "$DELIVERED_RETURN"
fi
DELIVERED_RETURN_SHA="$("$SHASUM_BIN" -a 256 "$DELIVERED_RETURN" | "$CUT_BIN" -d ' ' -f 1)"
[ "$DELIVERED_RETURN_SHA" = "$RETURN_SHA" ] || {
  echo 'Local Artifacts build: delivered RETURN digest mismatch.' >&2
  exit 37
}

echo 'Local Artifacts build: validating canonical RETURN and delivering DMG when PASS...'
set +e
"$NODE_BIN" "$CONSUMER" \
  --sdk-root "$CARRIER_SDK_ROOT" \
  --task-zip "$PACKAGE" \
  --return-zip "$RETURN_ZIP" \
  --output-root "$DOWNLOADS"
CONSUMER_STATUS=$?
set -e

if [ "$CONSUMER_STATUS" -ne 0 ]; then
  echo ''
  echo 'LOCAL_ARTIFACTS_MAC_BUILD=SAFE_FAILURE_OR_RETURN_HELD'
  echo "LOCAL_ARTIFACTS_RETURN=$DELIVERED_RETURN"
  echo "LOCAL_ARTIFACTS_RETURN_SHA256=$DELIVERED_RETURN_SHA"
  echo 'Upload that RETURN ZIP back to ChatGPT; the recovery root is also preserved.' >&2
  exit 2
fi

[ -s "$DELIVERY" ] || { echo 'Local Artifacts build: delivery receipt missing.' >&2; exit 38; }
[ -s "$DMG" ] || { echo 'Local Artifacts build: DMG missing after successful consumption.' >&2; exit 39; }
[ -s "$HUMAN_DMG" ] || { echo 'Local Artifacts build: human-visible DMG missing after successful consumption.' >&2; exit 40; }

DMG_SHA="$("$SHASUM_BIN" -a 256 "$DMG" | "$CUT_BIN" -d ' ' -f 1)"
HUMAN_DMG_SHA="$("$SHASUM_BIN" -a 256 "$HUMAN_DMG" | "$CUT_BIN" -d ' ' -f 1)"
[ "$HUMAN_DMG_SHA" = "$DMG_SHA" ] || { echo 'Local Artifacts build: human-visible DMG digest mismatch.' >&2; exit 41; }

echo ''
echo 'LOCAL_ARTIFACTS_MAC_BUILD=PASS'
echo "LOCAL_ARTIFACTS_DMG=$HUMAN_DMG"
echo "LOCAL_ARTIFACTS_DMG_SHA256=$DMG_SHA"
echo "LOCAL_ARTIFACTS_RETURN=$DELIVERED_RETURN"
echo "LOCAL_ARTIFACTS_RETURN_SHA256=$DELIVERED_RETURN_SHA"
echo "LOCAL_ARTIFACTS_BUILD_RECEIPT=$DELIVERY"
echo ''
echo 'Open the DMG for manual Local Artifacts testing, and upload the RETURN ZIP back to ChatGPT.'

PRESERVE_ROOT=0
exit 0

# [VXG RealForever]
