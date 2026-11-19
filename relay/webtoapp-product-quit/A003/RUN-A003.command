#!/bin/bash
set -euo pipefail

CONTINUITY='[VXG RealForever]'
SDK_REPOSITORY='vgong24/Vextreme-SDK'
TRUSTED_SDK_HEAD='8543c7d755faba04d4c4019c02e84f992667918a'
TRUSTED_SDK_TREE='6376f68279f0750ee6d3868b8472368199763064'
CARRIER_HEAD='93ec7816282c55182ba2c14bcdbd1a31c5fc6fc6'
CARRIER_TREE='35832fb03d5e8fccb707a879fa87e92745e98902'
PRODUCT_REPOSITORY='VexGPT/VexLife-Prod'
PRODUCT_BASE='1c96c04d553ab54c475c16aeedc317e57412663c'
PRODUCT_HEAD='fdb9f77bbd9895d0ab6932f669091b46ea813e9b'
PRODUCT_TREE='e68cf89a991af80cd153b49b201187e75241ab29'
PRODUCT_BRANCH='VXG-111926-chatgpt-webtoapp-product-quit-a001'
ATTEMPT='A003'

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd -P)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd -P)"
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

BASH_BIN="$(resolve_exec /bin/bash)" || { echo 'A003: qualified Bash unavailable.' >&2; exit 10; }
GIT_BIN="$(resolve_exec /usr/bin/git)" || { echo 'A003: qualified Git unavailable.' >&2; exit 11; }
GH_BIN="$(resolve_exec /opt/homebrew/bin/gh /usr/local/bin/gh)" || { echo 'A003: qualified GitHub CLI unavailable.' >&2; exit 12; }
NODE_BIN="$(resolve_exec /opt/homebrew/bin/node /usr/local/bin/node /usr/bin/node)" || { echo 'A003: qualified Node unavailable.' >&2; exit 13; }
MKTEMP_BIN="$(resolve_exec /usr/bin/mktemp)" || { echo 'A003: mktemp unavailable.' >&2; exit 14; }
RM_BIN="$(resolve_exec /bin/rm /usr/bin/rm)" || { echo 'A003: rm unavailable.' >&2; exit 15; }
SHASUM_BIN="$(resolve_exec /usr/bin/shasum)" || { echo 'A003: shasum unavailable.' >&2; exit 16; }
CUT_BIN="$(resolve_exec /usr/bin/cut)" || { echo 'A003: cut unavailable.' >&2; exit 17; }

ARCH="$(/usr/bin/uname -m)"
[ "$ARCH" = 'arm64' ] || { echo "A003: expected Apple-silicon arm64 host, observed $ARCH." >&2; exit 18; }

"$GH_BIN" auth status --hostname github.com >/dev/null 2>&1 || {
  echo 'A003: GitHub authentication is required.' >&2
  exit 19
}

for required in "$FORM_TASK" "$PAYLOAD" "$CONSUMER"; do
  [ -f "$required" ] || { echo "A003: companion source missing: $required" >&2; exit 20; }
done

COMPANION_HEAD="$("$GIT_BIN" -C "$REPO_ROOT" rev-parse HEAD)"
"$GIT_BIN" -C "$REPO_ROOT" merge-base --is-ancestor "$PRODUCT_HEAD" "$COMPANION_HEAD" || {
  echo 'A003: companion checkout is not descended from exact Product #391 head.' >&2
  exit 21
}

ROOT="$("$MKTEMP_BIN" -d "$HOME/Downloads/.vexlife-pr391-a003.XXXXXX")"
TRUSTED_SDK_ROOT="$ROOT/trusted-sdk"
CARRIER_SDK_ROOT="$ROOT/carrier-sdk"
PRODUCT_ROOT="$ROOT/product"
FORMED_ROOT="$ROOT/formed"
PRODUCT_BUNDLE="$ROOT/vexlife-prod.bundle"
CONSUMED_RESULT="$HOME/Downloads/VEXLIFE-PR391-WEBTOAPP-PRODUCT-QUIT-A003-CONSUMED.json"

cleanup() {
  case "$ROOT" in
    "$HOME"/Downloads/.vexlife-pr391-a003.*) "$RM_BIN" -rf -- "$ROOT" ;;
    *) return 1 ;;
  esac
}
trap cleanup EXIT HUP INT TERM

echo 'A003: acquiring exact accepted SDK builder source...'
"$GH_BIN" repo clone "$SDK_REPOSITORY" "$TRUSTED_SDK_ROOT" -- --no-checkout >/dev/null
"$GIT_BIN" -C "$TRUSTED_SDK_ROOT" checkout --detach "$TRUSTED_SDK_HEAD" >/dev/null

echo 'A003: acquiring exact #1956 host-effect carrier source...'
"$GH_BIN" repo clone "$SDK_REPOSITORY" "$CARRIER_SDK_ROOT" -- --no-checkout >/dev/null
"$GIT_BIN" -C "$CARRIER_SDK_ROOT" checkout --detach "$CARRIER_HEAD" >/dev/null

echo 'A003: acquiring exact #391 Product candidate...'
"$GH_BIN" repo clone "$PRODUCT_REPOSITORY" "$PRODUCT_ROOT" -- --no-checkout >/dev/null
"$GIT_BIN" -C "$PRODUCT_ROOT" checkout --detach "$PRODUCT_HEAD" >/dev/null

[ "$("$GIT_BIN" -C "$TRUSTED_SDK_ROOT" rev-parse HEAD)" = "$TRUSTED_SDK_HEAD" ] || { echo 'A003: trusted SDK head mismatch.' >&2; exit 22; }
[ "$("$GIT_BIN" -C "$TRUSTED_SDK_ROOT" rev-parse 'HEAD^{tree}')" = "$TRUSTED_SDK_TREE" ] || { echo 'A003: trusted SDK tree mismatch.' >&2; exit 23; }
[ "$("$GIT_BIN" -C "$CARRIER_SDK_ROOT" rev-parse HEAD)" = "$CARRIER_HEAD" ] || { echo 'A003: carrier head mismatch.' >&2; exit 24; }
[ "$("$GIT_BIN" -C "$CARRIER_SDK_ROOT" rev-parse 'HEAD^{tree}')" = "$CARRIER_TREE" ] || { echo 'A003: carrier tree mismatch.' >&2; exit 25; }
[ "$("$GIT_BIN" -C "$PRODUCT_ROOT" rev-parse HEAD)" = "$PRODUCT_HEAD" ] || { echo 'A003: Product head mismatch.' >&2; exit 26; }
[ "$("$GIT_BIN" -C "$PRODUCT_ROOT" rev-parse 'HEAD^{tree}')" = "$PRODUCT_TREE" ] || { echo 'A003: Product tree mismatch.' >&2; exit 27; }
"$GIT_BIN" -C "$PRODUCT_ROOT" merge-base --is-ancestor "$PRODUCT_BASE" "$PRODUCT_HEAD" || { echo 'A003: Product base is not ancestor.' >&2; exit 28; }

A003_SDK_ROOT="$CARRIER_SDK_ROOT" "$NODE_BIN" -e '
const fs=require("node:fs");
const path=require("node:path");
const root=process.env.A003_SDK_ROOT;
const contract=JSON.parse(fs.readFileSync(path.join(root,"docs/private-continuity/vexrelay/current-contract.json"),"utf8"));
if(contract.contractRef!=="source.vexrelay.current-contract/v1") process.exit(29);
const profile=JSON.parse(fs.readFileSync(path.join(root,"docs/private-continuity/vexlocalbridge/profiles/macos-victor-vexlife-prod.json"),"utf8"));
const policy=require(path.join(root,"lib/vexlocalbridge/vexlife-prod-host-effect-policy.js"));
policy.validateProfileBinding(profile);
if(profile.repository!=="VexGPT/VexLife-Prod") process.exit(30);
'

echo 'A003: forming immutable Product bundle...'
"$GIT_BIN" -C "$PRODUCT_ROOT" update-ref refs/heads/vexlife-prod-proof-candidate-a003 "$PRODUCT_HEAD"
"$GIT_BIN" -C "$PRODUCT_ROOT" bundle create "$PRODUCT_BUNDLE" refs/heads/vexlife-prod-proof-candidate-a003
"$GIT_BIN" -C "$PRODUCT_ROOT" bundle verify "$PRODUCT_BUNDLE" >/dev/null
[ -s "$PRODUCT_BUNDLE" ] || { echo 'A003: Product bundle was not formed.' >&2; exit 31; }

mkdir -p "$FORMED_ROOT"
echo 'A003: forming exact VexLocalBridge task from accepted source...'
"$NODE_BIN" "$FORM_TASK"   --trusted-sdk "$TRUSTED_SDK_ROOT"   --carrier-sdk "$CARRIER_SDK_ROOT"   --payload "$PAYLOAD"   --product-bundle "$PRODUCT_BUNDLE"   --output "$FORMED_ROOT"   > "$ROOT/formation-output.json"

FORMATION="$FORMED_ROOT/FORMATION.json"
[ -f "$FORMATION" ] || { echo 'A003: formation receipt missing.' >&2; exit 32; }

PACKAGE_FILE="$("$NODE_BIN" -e 'const fs=require("node:fs");const x=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));process.stdout.write(x.packageFile)' "$FORMATION")"
EXPECTED="$("$NODE_BIN" -e 'const fs=require("node:fs");const x=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));process.stdout.write(x.packageSha256)' "$FORMATION")"
PACKAGE="$FORMED_ROOT/$PACKAGE_FILE"

[ -f "$PACKAGE" ] || { echo 'A003: formed package missing.' >&2; exit 33; }
OBSERVED="$("$SHASUM_BIN" -a 256 "$PACKAGE" | "$CUT_BIN" -d ' ' -f 1)"
[ "$OBSERVED" = "$EXPECTED" ] || { echo 'A003: formed package digest mismatch.' >&2; exit 34; }

echo "A003: entering VexLocalBridge with package sha256 $EXPECTED"
set +e
VEX_EXPECTED_PACKAGE_SHA256="$EXPECTED"   "$BASH_BIN" "$CARRIER_SDK_ROOT/lib/vexlocalbridge/templates/RUN-VEXLIFE-PROD-TASK.command" "$PACKAGE"   > "$ROOT/vexlocalbridge.stdout.log"   2> "$ROOT/vexlocalbridge.stderr.log"
CARRIER_STATUS=$?
set -e

CAPSULE_ROOT="$FORMED_ROOT/VLB-${EXPECTED:0:16}"
RESULT_ROOT="$CAPSULE_ROOT/results"
[ -d "$RESULT_ROOT" ] || {
  cat "$ROOT/vexlocalbridge.stderr.log" >&2 || true
  echo "A003: canonical result root missing; carrier exit=$CARRIER_STATUS" >&2
  exit 35
}

RETURN_COUNT="$(/usr/bin/find "$RESULT_ROOT" -type f -name 'RETURN-*.zip' | /usr/bin/wc -l | /usr/bin/tr -d ' ')"
[ "$RETURN_COUNT" = '1' ] || {
  echo "A003: expected exactly one canonical RETURN, observed $RETURN_COUNT." >&2
  exit 36
}
RETURN_ZIP="$(/usr/bin/find "$RESULT_ROOT" -type f -name 'RETURN-*.zip' -print)"
RETURN_SHA="$("$SHASUM_BIN" -a 256 "$RETURN_ZIP" | "$CUT_BIN" -d ' ' -f 1)"

"$RM_BIN" -f -- "$CONSUMED_RESULT"
echo 'A003: validating and consuming canonical RETURN locally...'
"$NODE_BIN" "$CONSUMER"   --sdk-root "$CARRIER_SDK_ROOT"   --task-zip "$PACKAGE"   --return-zip "$RETURN_ZIP"   --output "$CONSUMED_RESULT"   > "$ROOT/consumed.stdout.json"

[ -s "$CONSUMED_RESULT" ] || { echo 'A003: consumed result missing.' >&2; exit 37; }

DISPOSITION="$("$NODE_BIN" -e 'const fs=require("node:fs");const x=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));process.stdout.write(x.relayDisposition)' "$CONSUMED_RESULT")"
STOP="$("$NODE_BIN" -e 'const fs=require("node:fs");const x=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));process.stdout.write(x.stopReasonOrNull ?? "")' "$CONSUMED_RESULT")"

echo "A003_CONSUMED_RESULT=$CONSUMED_RESULT"
echo "A003_CANONICAL_RETURN_SHA256=$RETURN_SHA"
echo "A003_RELAY_DISPOSITION=$DISPOSITION"
echo "A003_STOP_REASON=${STOP:-NONE}"
cat "$CONSUMED_RESULT"

if [ "$DISPOSITION" = 'PASS' ]; then
  exit 0
fi
exit 2

# [VXG RealForever]
