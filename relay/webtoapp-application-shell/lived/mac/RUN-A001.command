#!/bin/bash
set -euo pipefail

CONTINUITY='[VXG RealForever]'
SDK_REPOSITORY='vgong24/Vextreme-SDK'
TRUSTED_SDK_HEAD='8543c7d755faba04d4c4019c02e84f992667918a'
TRUSTED_SDK_TREE='6376f68279f0750ee6d3868b8472368199763064'
CARRIER_HEAD='36eb5c82becd97dcfcbc8fa67455a74d8540503a'
CARRIER_TREE='bea091ed67793931b838c7b412d9abce39a2e373'
PRODUCT_REPOSITORY='VexGPT/VexLife-Prod'
PRODUCT_BASE='fdb9f77bbd9895d0ab6932f669091b46ea813e9b'
PRODUCT_HEAD='d13e8e4a774aea60362491420ffeee2c07f91d1c'
PRODUCT_TREE='98ba3219e49f722272c976171cb736ed19ae9ac9'

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd -P)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd -P)"
FORM_TASK="$SCRIPT_DIR/../form-task.cjs"
PAYLOAD="$SCRIPT_DIR/payload.mjs"
CONSUMER="$SCRIPT_DIR/../consume-return.cjs"
GATE="$SCRIPT_DIR/../GATE.json"

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

BASH_BIN="$(resolve_exec /bin/bash)" || { echo 'Mac lived proof: Bash unavailable.' >&2; exit 10; }
GIT_BIN="$(resolve_exec /usr/bin/git)" || { echo 'Mac lived proof: Git unavailable.' >&2; exit 11; }
GH_BIN="$(resolve_exec /opt/homebrew/bin/gh /usr/local/bin/gh)" || { echo 'Mac lived proof: GitHub CLI unavailable.' >&2; exit 12; }
NODE_BIN="$(resolve_exec /opt/homebrew/bin/node /usr/local/bin/node /usr/bin/node)" || { echo 'Mac lived proof: Node unavailable.' >&2; exit 13; }
MKTEMP_BIN="$(resolve_exec /usr/bin/mktemp)" || { echo 'Mac lived proof: mktemp unavailable.' >&2; exit 14; }
RM_BIN="$(resolve_exec /bin/rm /usr/bin/rm)" || { echo 'Mac lived proof: rm unavailable.' >&2; exit 15; }
SHASUM_BIN="$(resolve_exec /usr/bin/shasum)" || { echo 'Mac lived proof: shasum unavailable.' >&2; exit 16; }
CUT_BIN="$(resolve_exec /usr/bin/cut)" || { echo 'Mac lived proof: cut unavailable.' >&2; exit 17; }

ARCH="$(/usr/bin/uname -m)"
[ "$ARCH" = 'arm64' ] || { echo "Mac lived proof: expected Apple-silicon arm64, observed $ARCH." >&2; exit 18; }

"$GH_BIN" auth status --hostname github.com >/dev/null 2>&1 || {
  echo 'Mac lived proof: GitHub authentication unavailable.' >&2
  exit 19
}

for required in "$FORM_TASK" "$PAYLOAD" "$CONSUMER" "$GATE"; do
  [ -f "$required" ] || { echo "Mac lived proof: companion source missing: $required" >&2; exit 20; }
done

COMPANION_HEAD="$("$GIT_BIN" -C "$REPO_ROOT" rev-parse HEAD)"
"$GIT_BIN" -C "$REPO_ROOT" merge-base --is-ancestor "$PRODUCT_HEAD" "$COMPANION_HEAD" || {
  echo 'Mac lived proof: companion checkout is not descended from exact #396 head.' >&2
  exit 21
}

DOWNLOADS="$HOME/Downloads"
ROOT="$("$MKTEMP_BIN" -d "$DOWNLOADS/.vexlife-pr398-mac-a001.XXXXXX")"
TRUSTED_SDK_ROOT="$ROOT/trusted-sdk"
CARRIER_SDK_ROOT="$ROOT/carrier-sdk"
PRODUCT_ROOT="$ROOT/product"
FORMED_ROOT="$ROOT/formed"
PRODUCT_BUNDLE="$ROOT/vexlife-prod.bundle"
CONSUMED_RESULT="$DOWNLOADS/VEXLIFE-PR398-APPLICATION-LIVED-MAC-A001-CONSUMED.json"
PRESERVE_ROOT=0

cleanup() {
  if [ "$PRESERVE_ROOT" -eq 1 ]; then
    printf 'Mac lived proof recovery root preserved: %s\n' "$ROOT" >&2
    return 0
  fi
  case "$ROOT" in
    "$DOWNLOADS"/.vexlife-pr398-mac-a001.*) "$RM_BIN" -rf -- "$ROOT" ;;
    *) return 1 ;;
  esac
}
trap cleanup EXIT HUP INT TERM

echo 'Mac lived proof: acquiring exact accepted SDK builder source...'
"$GH_BIN" repo clone "$SDK_REPOSITORY" "$TRUSTED_SDK_ROOT" -- --no-checkout >/dev/null
"$GIT_BIN" -C "$TRUSTED_SDK_ROOT" checkout --detach "$TRUSTED_SDK_HEAD" >/dev/null

echo 'Mac lived proof: acquiring exact application-lived carrier source...'
"$GH_BIN" repo clone "$SDK_REPOSITORY" "$CARRIER_SDK_ROOT" -- --no-checkout >/dev/null
"$GIT_BIN" -C "$CARRIER_SDK_ROOT" checkout --detach "$CARRIER_HEAD" >/dev/null

echo 'Mac lived proof: acquiring exact #396 Product candidate...'
"$GH_BIN" repo clone "$PRODUCT_REPOSITORY" "$PRODUCT_ROOT" -- --no-checkout >/dev/null
"$GIT_BIN" -C "$PRODUCT_ROOT" checkout --detach "$PRODUCT_HEAD" >/dev/null

[ "$("$GIT_BIN" -C "$TRUSTED_SDK_ROOT" rev-parse HEAD)" = "$TRUSTED_SDK_HEAD" ] || { echo 'Mac lived proof: trusted SDK head mismatch.' >&2; exit 22; }
[ "$("$GIT_BIN" -C "$TRUSTED_SDK_ROOT" rev-parse 'HEAD^{tree}')" = "$TRUSTED_SDK_TREE" ] || { echo 'Mac lived proof: trusted SDK tree mismatch.' >&2; exit 23; }
[ "$("$GIT_BIN" -C "$CARRIER_SDK_ROOT" rev-parse HEAD)" = "$CARRIER_HEAD" ] || { echo 'Mac lived proof: carrier head mismatch.' >&2; exit 24; }
[ "$("$GIT_BIN" -C "$CARRIER_SDK_ROOT" rev-parse 'HEAD^{tree}')" = "$CARRIER_TREE" ] || { echo 'Mac lived proof: carrier tree mismatch.' >&2; exit 25; }
[ "$("$GIT_BIN" -C "$PRODUCT_ROOT" rev-parse HEAD)" = "$PRODUCT_HEAD" ] || { echo 'Mac lived proof: Product head mismatch.' >&2; exit 26; }
[ "$("$GIT_BIN" -C "$PRODUCT_ROOT" rev-parse 'HEAD^{tree}')" = "$PRODUCT_TREE" ] || { echo 'Mac lived proof: Product tree mismatch.' >&2; exit 27; }
"$GIT_BIN" -C "$PRODUCT_ROOT" merge-base --is-ancestor "$PRODUCT_BASE" "$PRODUCT_HEAD" || { echo 'Mac lived proof: Product base is not ancestor.' >&2; exit 28; }

echo 'Mac lived proof: forming immutable Product bundle...'
"$GIT_BIN" -C "$PRODUCT_ROOT" update-ref refs/heads/vexlife-pr398-lived-mac-a001 "$PRODUCT_HEAD"
"$GIT_BIN" -C "$PRODUCT_ROOT" bundle create "$PRODUCT_BUNDLE" refs/heads/vexlife-pr398-lived-mac-a001
"$GIT_BIN" -C "$PRODUCT_ROOT" bundle verify "$PRODUCT_BUNDLE" >/dev/null
[ -s "$PRODUCT_BUNDLE" ] || { echo 'Mac lived proof: Product bundle missing.' >&2; exit 29; }

mkdir -p "$FORMED_ROOT"
echo 'Mac lived proof: checking source-owned gate and forming task...'
"$NODE_BIN" "$FORM_TASK" \
  --trusted-sdk "$TRUSTED_SDK_ROOT" \
  --carrier-sdk "$CARRIER_SDK_ROOT" \
  --payload "$PAYLOAD" \
  --product-bundle "$PRODUCT_BUNDLE" \
  --output "$FORMED_ROOT" \
  --host macos-victor

FORMATION="$FORMED_ROOT/FORMATION-macos.json"
[ -f "$FORMATION" ] || { echo 'Mac lived proof: formation receipt missing.' >&2; exit 30; }

PACKAGE="$("$NODE_BIN" -e 'const fs=require("node:fs");const x=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));process.stdout.write(x.packagePath)' "$FORMATION")"
EXPECTED="$("$NODE_BIN" -e 'const fs=require("node:fs");const x=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));process.stdout.write(x.packageSha256)' "$FORMATION")"
[ -f "$PACKAGE" ] || { echo 'Mac lived proof: formed package missing.' >&2; exit 31; }
OBSERVED="$("$SHASUM_BIN" -a 256 "$PACKAGE" | "$CUT_BIN" -d ' ' -f 1)"
[ "$OBSERVED" = "$EXPECTED" ] || { echo 'Mac lived proof: formed package digest mismatch.' >&2; exit 32; }

PRESERVE_ROOT=1
echo "Mac lived proof: entering VexLocalBridge with package sha256 $EXPECTED"
set +e
VEX_EXPECTED_PACKAGE_SHA256="$EXPECTED" \
  "$BASH_BIN" "$CARRIER_SDK_ROOT/lib/vexlocalbridge/templates/RUN-VEXLIFE-PROD-TASK.command" "$PACKAGE"
CARRIER_STATUS=$?
set -e

CAPSULE_ROOT="$FORMED_ROOT/VLB-${EXPECTED:0:16}"
RESULT_ROOT="$CAPSULE_ROOT/results"
[ -d "$RESULT_ROOT" ] || {
  echo "Mac lived proof: canonical result root missing; carrier exit=$CARRIER_STATUS" >&2
  exit 33
}

RETURN_COUNT="$(/usr/bin/find "$RESULT_ROOT" -type f -name 'RETURN-*.zip' | /usr/bin/wc -l | /usr/bin/tr -d ' ')"
[ "$RETURN_COUNT" = '1' ] || { echo "Mac lived proof: expected one RETURN, observed $RETURN_COUNT." >&2; exit 34; }
RETURN_ZIP="$(/usr/bin/find "$RESULT_ROOT" -type f -name 'RETURN-*.zip' -print)"

"$RM_BIN" -f -- "$CONSUMED_RESULT"
"$NODE_BIN" "$CONSUMER" \
  --sdk-root "$CARRIER_SDK_ROOT" \
  --host macos-victor \
  --task-zip "$PACKAGE" \
  --return-zip "$RETURN_ZIP" \
  --output "$CONSUMED_RESULT"

[ -s "$CONSUMED_RESULT" ] || { echo 'Mac lived proof: consumed result missing.' >&2; exit 35; }
APP_PASS="$("$NODE_BIN" -e 'const fs=require("node:fs");const x=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));process.stdout.write(String(x.applicationLivedPass))' "$CONSUMED_RESULT")"
LIFECYCLE_PASS="$("$NODE_BIN" -e 'const fs=require("node:fs");const x=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));process.stdout.write(String(x.productQuitLifecyclePass))' "$CONSUMED_RESULT")"

echo "MAC_APPLICATION_LIVED_PASS=$APP_PASS"
echo "MAC_PRODUCT_QUIT_LIFECYCLE_PASS=$LIFECYCLE_PASS"
echo "MAC_CONSUMED_RESULT=$CONSUMED_RESULT"
cat "$CONSUMED_RESULT"

if [ "$APP_PASS" = 'true' ] && [ "$LIFECYCLE_PASS" = 'true' ]; then
  PRESERVE_ROOT=0
  exit 0
fi
exit 2

# [VXG RealForever]
