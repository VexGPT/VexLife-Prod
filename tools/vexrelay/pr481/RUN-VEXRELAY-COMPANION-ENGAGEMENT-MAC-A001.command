#!/bin/bash
# [VXG RealForever]
set -uo pipefail

SDK_HEAD="6f8f6f50467f5df5a542713f7dc25b82db4ac8fa"
SDK_TREE="96795640b01f1969da980f52d5a8ec136a8c44aa"
CONTRACT_BLOB="b53cbac01c2deeb45fad5c32ce98e0796c04126f"
SOURCE_BASE="ae4a511b49ad4720b5c51de55ccb834d43aef218"
SOURCE_HEAD="40c6781f0920823f4a19231b33f7d4323cacac08"
SOURCE_TREE="637a4021e12e1133b2b2b2dfef5b38ad1cd4bdc5"
SOURCE_BRANCH="VXG-112126-companion-engagement-bar-a001"
RETURN_STEM="RETURN-VEXLIFE-COMPANION-ENGAGEMENT-MAC-A001"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd -P)"
WORK="$(/usr/bin/mktemp -d "$HOME/Downloads/.vexlife-pr481-relay-a001.XXXXXX")"
LOG="$WORK/execution.log"
PACKED=0

/bin/mkdir -p "$WORK"
exec > >(/usr/bin/tee -a "$LOG") 2>&1

pack_return() {
  local status="$1"
  local outcome="$2"
  local stage="$WORK/human-return"
  /bin/rm -rf "$stage"
  /bin/mkdir -p "$stage/relay"

  /bin/cp "$LOG" "$stage/execution.log" 2>/dev/null || true
  [ -f "$WORK/relay-run.json" ] && /bin/cp "$WORK/relay-run.json" "$stage/relay/relay-run.json"

  if [ -d "$WORK/relay-out" ]; then
    local index=0
    while IFS= read -r -d '' z; do
      index=$((index + 1))
      /bin/cp "$z" "$stage/relay/$(printf '%02d-' "$index")$(basename "$z")"
    done < <(/usr/bin/find "$WORK/relay-out" -type f -name '*.zip' -print0 2>/dev/null)
  fi

  if [ -d "$WORK/lived-out" ]; then
    for name in VexLife-macOS-AppleSilicon.dmg VexLife-macOS-AppleSilicon.dmg.sha256 VexLife-macOS-AppleSilicon.receipt.json CANDIDATE.txt; do
      [ -f "$WORK/lived-out/$name" ] && /bin/cp "$WORK/lived-out/$name" "$stage/$name"
    done
  fi

  cat > "$stage/RESULT-SUMMARY.txt" <<EOF
[VXG RealForever]
schemaVersion=vexlife-prod.companion-engagement-human-return/v1
status=$status
outcome=$outcome
sourceBase=$SOURCE_BASE
sourceHead=$SOURCE_HEAD
sourceTree=$SOURCE_TREE
sourceBranch=$SOURCE_BRANCH
relayContract=source.vexrelay.current-contract/v1
sdkHead=$SDK_HEAD
localWorkspaceUsed=false
githubActionsProofUsed=false
dmgIncluded=$([ -f "$stage/VexLife-macOS-AppleSilicon.dmg" ] && echo true || echo false)
livedAcceptance=PENDING
EOF

  local tmp="$WORK/$RETURN_STEM.zip"
  /usr/bin/ditto -c -k --sequesterRsrc --keepParent "$stage" "$tmp"
  local digest
  digest="$(/usr/bin/shasum -a 256 "$tmp" | /usr/bin/awk '{print $1}')"
  local final="$HOME/Downloads/$RETURN_STEM--sha256-$digest.zip"
  /bin/mv "$tmp" "$final"
  PACKED=1
  echo
  echo "[VexLife] Canonical human RETURN:"
  echo "$final"
  echo "SHA-256: $digest"
  /usr/bin/open -R "$final" >/dev/null 2>&1 || true
}

finish() {
  local status=$?
  trap - EXIT
  if [ "$PACKED" -ne 1 ]; then
    pack_return "$status" "SAFE_FAILURE_OR_HOST_BOUNDARY" || true
  fi
  /bin/rm -rf "$WORK"
  exit "$status"
}
trap finish EXIT

GH=""
for c in /opt/homebrew/bin/gh /usr/local/bin/gh; do
  if [ -x "$c" ]; then GH="$c"; break; fi
done
[ -n "$GH" ] || { echo "VexRelay: GitHub CLI not found." >&2; exit 20; }

NODE=""
for c in /opt/homebrew/bin/node /usr/local/bin/node /usr/bin/node; do
  if [ -x "$c" ]; then NODE="$c"; break; fi
done
[ -n "$NODE" ] || { echo "VexRelay: qualified Node not found." >&2; exit 21; }

"$GH" auth status -h github.com >/dev/null 2>&1 || {
  echo "VexRelay: existing GitHub CLI authentication is required to read private Vextreme-SDK." >&2
  exit 22
}

SDK="$WORK/Vextreme-SDK"
echo "[VexRelay] acquiring exact current SDK $SDK_HEAD"
"$GH" repo clone vgong24/Vextreme-SDK "$SDK" -- --no-checkout
/usr/bin/git -C "$SDK" checkout --detach "$SDK_HEAD"
[ "$(/usr/bin/git -C "$SDK" rev-parse HEAD)" = "$SDK_HEAD" ] || { echo "SDK head mismatch" >&2; exit 23; }
[ "$(/usr/bin/git -C "$SDK" rev-parse 'HEAD^{tree}')" = "$SDK_TREE" ] || { echo "SDK tree mismatch" >&2; exit 24; }
[ "$(/usr/bin/git -C "$SDK" rev-parse "$SDK_HEAD:docs/private-continuity/vexrelay/current-contract.json")" = "$CONTRACT_BLOB" ] || {
  echo "SDK current-contract blob mismatch" >&2
  exit 25
}
[ -z "$(/usr/bin/git -C "$SDK" status --porcelain=v1 -uall)" ] || { echo "SDK checkout is not clean" >&2; exit 26; }

echo "[VexRelay] forming + running exact PR #481 SourceOwner proof"
/bin/mkdir -p "$WORK/relay-out"
set +e
"$NODE" "$SCRIPT_DIR/FORM-AND-RUN-A001.mjs" \
  "$SCRIPT_DIR/FORMATION-A001.json" \
  "$SDK" \
  "$WORK/relay-out" \
  "$WORK/relay-run.json"
RELAY_STATUS=$?
set -e
if [ "$RELAY_STATUS" -ne 0 ]; then
  echo "VexRelay proof did not PASS (exit=$RELAY_STATUS); DMG formation is held."
  exit "$RELAY_STATUS"
fi

echo "[VexLife] Relay PASS; forming one lived-test DMG from the same exact source"
PRODUCT="$WORK/product"
/usr/bin/git clone --no-checkout https://github.com/VexGPT/VexLife-Prod.git "$PRODUCT"
/usr/bin/git -C "$PRODUCT" checkout --detach "$SOURCE_HEAD"
[ "$(/usr/bin/git -C "$PRODUCT" rev-parse HEAD)" = "$SOURCE_HEAD" ] || { echo "Product head mismatch" >&2; exit 40; }
[ "$(/usr/bin/git -C "$PRODUCT" rev-parse 'HEAD^{tree}')" = "$SOURCE_TREE" ] || { echo "Product tree mismatch" >&2; exit 41; }
[ -z "$(/usr/bin/git -C "$PRODUCT" status --porcelain=v1 -uall)" ] || { echo "Product checkout is not clean" >&2; exit 42; }

export GRADLE_OPTS="-Dorg.gradle.jvmargs=-Xmx6g -Dkotlin.daemon.jvm.options=-Xmx4g"
(
  cd "$PRODUCT"
  /bin/bash ./gradlew --no-daemon -PvexlifeAppVersion=0.0.0 archiveVexLifeProdMacApplication
)

OUT="$PRODUCT/build/vexlife-prod-mac-application/$SOURCE_HEAD"
APP="$OUT/VexLife.app"
DMG="$OUT/VexLife-macOS-AppleSilicon.dmg"
SUM="$OUT/VexLife-macOS-AppleSilicon.dmg.sha256"
RECEIPT="$OUT/VexLife-macOS-AppleSilicon.receipt.json"
for f in "$APP/Contents/Info.plist" "$DMG" "$SUM" "$RECEIPT"; do
  [ -e "$f" ] && [ ! -L "$f" ] || { echo "Required formed artifact missing or symlinked: $f" >&2; exit 43; }
done
(cd "$OUT" && /usr/bin/shasum -a 256 -c "VexLife-macOS-AppleSilicon.dmg.sha256")
/usr/bin/hdiutil verify "$DMG" >/dev/null
/usr/bin/codesign --verify --strict "$APP"

DMG_SHA="$(/usr/bin/shasum -a 256 "$DMG" | /usr/bin/awk '{print $1}')"
EXPECTED_SOURCE="$(/usr/bin/plutil -extract VexLifeProdExpectedSourceSha raw -o - "$APP/Contents/Info.plist")"
[ "$EXPECTED_SOURCE" = "$SOURCE_HEAD" ] || { echo "VexLife.app expected source mismatch: $EXPECTED_SOURCE" >&2; exit 44; }

"$NODE" - "$RECEIPT" "$SOURCE_HEAD" "$DMG_SHA" <<'NODE'
const fs=require('fs');
const [receiptPath, sourceHead, dmgSha]=process.argv.slice(2);
const r=JSON.parse(fs.readFileSync(receiptPath,'utf8'));
const expected={
  schemaVersion:'vexlife-prod.macos-application-archive-receipt/v1',
  sourceSha:sourceHead,
  platform:'darwin-arm64',
  bundleIdentifier:'com.vexlife.prod',
  applicationVersion:'0.0.0',
  applicationName:'VexLife.app',
  diskImageName:'VexLife-macOS-AppleSilicon.dmg',
  diskImageSha256:dmgSha,
  distributionTrust:'AD_HOC_SIGNATURE_NO_DEVELOPER_ID_NO_NOTARIZATION',
  notarized:false,
};
for(const [k,v] of Object.entries(expected)){
  if(r[k]!==v) throw new Error('RECEIPT_MISMATCH:'+k+':'+String(r[k])+':'+String(v));
}
if(typeof r.applicationPayloadSha256!=='string'||!/^[0-9a-f]{64}$/.test(r.applicationPayloadSha256)){
  throw new Error('RECEIPT_APPLICATION_PAYLOAD_SHA256_INVALID');
}
NODE

/bin/mkdir -p "$WORK/lived-out"
/bin/cp "$DMG" "$SUM" "$RECEIPT" "$WORK/lived-out/"
APP_PAYLOAD_SHA="$("$NODE" -e 'const fs=require("fs");const r=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));process.stdout.write(r.applicationPayloadSha256)' "$RECEIPT")"
cat > "$WORK/lived-out/CANDIDATE.txt" <<EOF
[VXG RealForever]
sourceBase=$SOURCE_BASE
sourceHead=$SOURCE_HEAD
sourceTree=$SOURCE_TREE
sourceBranch=$SOURCE_BRANCH
sourceProof=A001_LOCAL_VEXRELAY_PASS
sourceProofRoute=VEXRELAY_EXACT_HEAD_SOURCE_OWNER
livedCandidateDmgSha256=$DMG_SHA
livedCandidateAppPayloadSha256=$APP_PAYLOAD_SHA
receiptSchema=vexlife-prod.macos-application-archive-receipt/v1
distributionTrust=AD_HOC_SIGNATURE_NO_DEVELOPER_ID_NO_NOTARIZATION
livedAcceptance=PENDING
EOF

pack_return 0 "PASS__DMG_READY_FOR_LIVED_TEST"
echo "[VexLife] Local Relay + DMG proof PASS."
