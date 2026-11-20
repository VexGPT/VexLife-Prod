#!/bin/bash
set -euo pipefail

CONTINUITY='[VXG RealForever]'
SDK_REPOSITORY='vgong24/Vextreme-SDK'
TRUSTED_SDK_HEAD='8543c7d755faba04d4c4019c02e84f992667918a'
TRUSTED_SDK_TREE='6376f68279f0750ee6d3868b8472368199763064'
CARRIER_HEAD='36eb5c82becd97dcfcbc8fa67455a74d8540503a'
CARRIER_TREE='bea091ed67793931b838c7b412d9abce39a2e373'
PRODUCT_HEAD='f38779503f9a1171e26593f3542f2477c6e2370a'
COMPANION_MIN_HEAD='534e8075d25550b4490ed7e982b8cc4924135bf3'

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

BASH_BIN="$(resolve_exec /bin/bash)" || { echo 'Java selection inspection: Bash unavailable.' >&2; exit 10; }
GIT_BIN="$(resolve_exec /usr/bin/git)" || { echo 'Java selection inspection: Git unavailable.' >&2; exit 11; }
GH_BIN="$(resolve_exec /opt/homebrew/bin/gh /usr/local/bin/gh)" || { echo 'Java selection inspection: GitHub CLI unavailable.' >&2; exit 12; }
NODE_BIN="$(resolve_exec /opt/homebrew/bin/node /usr/local/bin/node /usr/bin/node)" || { echo 'Java selection inspection: Node unavailable.' >&2; exit 13; }
MKTEMP_BIN="$(resolve_exec /usr/bin/mktemp)" || { echo 'Java selection inspection: mktemp unavailable.' >&2; exit 14; }
RM_BIN="$(resolve_exec /bin/rm /usr/bin/rm)" || { echo 'Java selection inspection: rm unavailable.' >&2; exit 15; }
SHASUM_BIN="$(resolve_exec /usr/bin/shasum)" || { echo 'Java selection inspection: shasum unavailable.' >&2; exit 16; }
CUT_BIN="$(resolve_exec /usr/bin/cut)" || { echo 'Java selection inspection: cut unavailable.' >&2; exit 17; }
CP_BIN="$(resolve_exec /bin/cp /usr/bin/cp)" || { echo 'Java selection inspection: cp unavailable.' >&2; exit 18; }

[ "$(/usr/bin/uname -m)" = 'arm64' ] || { echo 'Java selection inspection requires Apple-silicon arm64.' >&2; exit 19; }
"$GH_BIN" auth status --hostname github.com >/dev/null 2>&1 || {
  echo 'Java selection inspection: GitHub authentication unavailable.' >&2
  echo 'Run: gh auth login --hostname github.com --web' >&2
  exit 20
}
for required in "$FORM_TASK" "$PAYLOAD"; do
  [ -f "$required" ] || { echo "Java selection inspection: companion source missing: $required" >&2; exit 21; }
done

COMPANION_HEAD="$("$GIT_BIN" -C "$REPO_ROOT" rev-parse HEAD)"
"$GIT_BIN" -C "$REPO_ROOT" merge-base --is-ancestor "$COMPANION_MIN_HEAD" "$COMPANION_HEAD" || {
  echo 'Java selection inspection: companion checkout predates the A003 correction record.' >&2
  exit 22
}

DOWNLOADS="$HOME/Downloads"
ROOT="$("$MKTEMP_BIN" -d "$DOWNLOADS/.vexlife-pr436-java-selection-a004.XXXXXX")"
TRUSTED_SDK_ROOT="$ROOT/trusted-sdk"
CARRIER_SDK_ROOT="$ROOT/carrier-sdk"
FORMED_ROOT="$ROOT/formed"
PRESERVE=0

cleanup() {
  if [ "$PRESERVE" -eq 0 ]; then
    case "$ROOT" in
      "$DOWNLOADS"/.vexlife-pr436-java-selection-a004.*) "$RM_BIN" -rf -- "$ROOT" ;;
      *) return 1 ;;
    esac
  else
    echo "Java selection inspection recovery root preserved: $ROOT" >&2
  fi
}
trap cleanup EXIT HUP INT TERM

echo 'Java selection inspection: acquiring exact accepted SDK builder source...'
"$GH_BIN" repo clone "$SDK_REPOSITORY" "$TRUSTED_SDK_ROOT" -- --no-checkout >/dev/null
"$GIT_BIN" -C "$TRUSTED_SDK_ROOT" checkout --detach "$TRUSTED_SDK_HEAD" >/dev/null

echo 'Java selection inspection: acquiring exact VexLife-Prod host carrier...'
"$GH_BIN" repo clone "$SDK_REPOSITORY" "$CARRIER_SDK_ROOT" -- --no-checkout >/dev/null
"$GIT_BIN" -C "$CARRIER_SDK_ROOT" checkout --detach "$CARRIER_HEAD" >/dev/null

[ "$("$GIT_BIN" -C "$TRUSTED_SDK_ROOT" rev-parse HEAD)" = "$TRUSTED_SDK_HEAD" ] || exit 23
[ "$("$GIT_BIN" -C "$TRUSTED_SDK_ROOT" rev-parse 'HEAD^{tree}')" = "$TRUSTED_SDK_TREE" ] || exit 24
[ "$("$GIT_BIN" -C "$CARRIER_SDK_ROOT" rev-parse HEAD)" = "$CARRIER_HEAD" ] || exit 25
[ "$("$GIT_BIN" -C "$CARRIER_SDK_ROOT" rev-parse 'HEAD^{tree}')" = "$CARRIER_TREE" ] || exit 26

"$NODE_BIN" -e '
  const fs=require("node:fs");
  const p=process.argv[1];
  const x=JSON.parse(fs.readFileSync(p,"utf8"));
  if(x.contractRef!=="source.vexrelay.current-contract/v1") process.exit(27);
' "$TRUSTED_SDK_ROOT/docs/private-continuity/vexrelay/current-contract.json"

mkdir -p "$FORMED_ROOT"
echo 'Java selection inspection: forming current source-managed read-only task...'
"$NODE_BIN" "$FORM_TASK" \
  --trusted-sdk "$TRUSTED_SDK_ROOT" \
  --carrier-sdk "$CARRIER_SDK_ROOT" \
  --payload "$PAYLOAD" \
  --output "$FORMED_ROOT"

FORMATION="$FORMED_ROOT/FORMATION.json"
[ -f "$FORMATION" ] || { echo 'Java selection inspection: formation receipt missing.' >&2; exit 28; }

PACKAGE="$("$NODE_BIN" -e 'const fs=require("node:fs");const x=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));process.stdout.write(x.packagePath)' "$FORMATION")"
EXPECTED="$("$NODE_BIN" -e 'const fs=require("node:fs");const x=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));process.stdout.write(x.packageSha256)' "$FORMATION")"
[ -f "$PACKAGE" ] || { echo 'Java selection inspection: formed package missing.' >&2; exit 29; }
[ "$("$SHASUM_BIN" -a 256 "$PACKAGE" | "$CUT_BIN" -d ' ' -f 1)" = "$EXPECTED" ] || {
  echo 'Java selection inspection: formed package digest mismatch.' >&2
  exit 30
}

PRESERVE=1
echo "Java selection inspection: entering VexLocalBridge with package sha256 $EXPECTED"
set +e
VEX_EXPECTED_PACKAGE_SHA256="$EXPECTED" \
  "$BASH_BIN" "$CARRIER_SDK_ROOT/lib/vexlocalbridge/templates/RUN-VEXLIFE-PROD-TASK.command" "$PACKAGE"
CARRIER_STATUS=$?
set -e

CAPSULE_ROOT="$FORMED_ROOT/VLB-${EXPECTED:0:16}"
RESULT_ROOT="$CAPSULE_ROOT/results"
[ -d "$RESULT_ROOT" ] || {
  echo "Java selection inspection: canonical result root missing; carrier exit=$CARRIER_STATUS" >&2
  exit 31
}
RETURN_COUNT="$(/usr/bin/find "$RESULT_ROOT" -type f -name 'RETURN-*.zip' | /usr/bin/wc -l | /usr/bin/tr -d ' ')"
[ "$RETURN_COUNT" = '1' ] || {
  echo "Java selection inspection: expected one RETURN, observed $RETURN_COUNT." >&2
  exit 32
}
RETURN_ZIP="$(/usr/bin/find "$RESULT_ROOT" -type f -name 'RETURN-*.zip' -print)"
RETURN_SHA="$("$SHASUM_BIN" -a 256 "$RETURN_ZIP" | "$CUT_BIN" -d ' ' -f 1)"
DELIVERY="$DOWNLOADS/RETURN-VEXLIFE-WEB-RESTORATION-JAVA-SELECTION-A004.zip"

if [ -e "$DELIVERY" ]; then
  [ "$("$SHASUM_BIN" -a 256 "$DELIVERY" | "$CUT_BIN" -d ' ' -f 1)" = "$RETURN_SHA" ] || {
    echo 'Java selection inspection: existing RETURN has different bytes.' >&2
    exit 33
  }
else
  "$CP_BIN" "$RETURN_ZIP" "$DELIVERY"
fi
[ "$("$SHASUM_BIN" -a 256 "$DELIVERY" | "$CUT_BIN" -d ' ' -f 1)" = "$RETURN_SHA" ] || exit 34

echo ''
echo 'JAVA_SELECTION_A004=RETURN_READY'
echo "RETURN=$DELIVERY"
echo "RETURN_SHA256=$RETURN_SHA"
echo 'This task did not execute Java, Whisper, VexLife, or mutate quarantine.'
echo 'Upload that one RETURN ZIP back to ChatGPT.'

PRESERVE=0
exit 0

# [VXG RealForever]
