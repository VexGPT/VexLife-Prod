#!/bin/bash
# [VXG RealForever]
set -euo pipefail

SOURCE_HEAD="314f057db77da4a6a299a91c6940da423598e582"
SOURCE_TREE="1ad966364e55dc36b6b7e311b9a163194479fdaa"
PROOF_ATTEMPT="A009"
PROOF_ROUTE="VEXRELAY_EXACT_HEAD_SOURCE_OWNER"

WORK="$(/usr/bin/mktemp -d "$HOME/Downloads/.vexlife-lived-candidate.XXXXXX")"
trap '/bin/rm -rf "$WORK"' EXIT

NODE=""
for c in /opt/homebrew/bin/node /usr/local/bin/node /usr/bin/node; do
  if [ -x "$c" ]; then NODE="$c"; break; fi
done
[ -n "$NODE" ] || { echo "Qualified Node executable not found." >&2; exit 20; }

echo "[VexLife] acquiring exact proved Product source"
/usr/bin/git clone --no-checkout https://github.com/VexGPT/VexLife-Prod.git "$WORK/repo"
/usr/bin/git -C "$WORK/repo" checkout --detach "$SOURCE_HEAD"

[ "$(/usr/bin/git -C "$WORK/repo" rev-parse HEAD)" = "$SOURCE_HEAD" ] || { echo "Source head mismatch" >&2; exit 40; }
[ "$(/usr/bin/git -C "$WORK/repo" rev-parse 'HEAD^{tree}')" = "$SOURCE_TREE" ] || { echo "Source tree mismatch" >&2; exit 41; }
[ -z "$(/usr/bin/git -C "$WORK/repo" status --porcelain=v1 -uall)" ] || { echo "Source checkout is not clean" >&2; exit 42; }

export GRADLE_OPTS="-Dorg.gradle.jvmargs=-Xmx4g -Dkotlin.daemon.jvm.options=-Xmx4g"
echo "[VexLife] forming lived candidate from exact proved source"
(
  cd "$WORK/repo"
  /bin/bash ./gradlew --no-daemon -PvexlifeAppVersion=0.0.0 archiveVexLifeProdMacApplication
)

OUT="$WORK/repo/build/vexlife-prod-mac-application/$SOURCE_HEAD"
APP="$OUT/VexLife.app"
DMG="$OUT/VexLife-macOS-AppleSilicon.dmg"
SUM="$OUT/VexLife-macOS-AppleSilicon.dmg.sha256"
RECEIPT="$OUT/VexLife-macOS-AppleSilicon.receipt.json"

for f in "$APP/Contents/Info.plist" "$DMG" "$SUM" "$RECEIPT"; do
  [ -e "$f" ] && [ ! -L "$f" ] || { echo "Required formed artifact missing or symlinked: $f" >&2; exit 43; }
done

(
  cd "$OUT"
  /usr/bin/shasum -a 256 -c "VexLife-macOS-AppleSilicon.dmg.sha256"
)
/usr/bin/hdiutil verify "$DMG" >/dev/null
/usr/bin/codesign --verify --strict "$APP"

DMG_SHA="$(/usr/bin/shasum -a 256 "$DMG" | /usr/bin/awk '{print $1}')"
EXPECTED_SOURCE="$(/usr/bin/plutil -extract VexLifeProdExpectedSourceSha raw -o - "$APP/Contents/Info.plist")"
[ "$EXPECTED_SOURCE" = "$SOURCE_HEAD" ] || {
  echo "VexLife.app expected source mismatch: $EXPECTED_SOURCE" >&2
  exit 44
}

"$NODE" - "$RECEIPT" "$SOURCE_HEAD" "$DMG_SHA" <<'NODE'
const fs=require('fs');
const args=process.argv.slice(2);
const receiptPath=args[0], sourceHead=args[1], dmgSha=args[2];
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

DEST="$HOME/Downloads/VEXLIFE-LIFETIME-COMPANION-A009-${SOURCE_HEAD:0:12}"
DMG_NAME="VexLife-LIFETIME-COMPANION-A009-${SOURCE_HEAD:0:8}.dmg"
SUM_NAME="$DMG_NAME.sha256"
RECEIPT_NAME="VexLife-LIFETIME-COMPANION-A009-${SOURCE_HEAD:0:8}.receipt.json"
/bin/mkdir -p "$DEST"
[ ! -L "$DEST" ] || { echo "Destination must not be a symlink" >&2; exit 45; }

/bin/cp "$DMG" "$DEST/$DMG_NAME"
/bin/cp "$RECEIPT" "$DEST/$RECEIPT_NAME"
printf '%s  %s\n' "$DMG_SHA" "$DMG_NAME" > "$DEST/$SUM_NAME"

COPIED_SHA="$(/usr/bin/shasum -a 256 "$DEST/$DMG_NAME" | /usr/bin/awk '{print $1}')"
[ "$COPIED_SHA" = "$DMG_SHA" ] || { echo "Copied DMG hash mismatch" >&2; exit 46; }

APP_PAYLOAD_SHA="$("$NODE" -e 'const fs=require("fs");const r=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));process.stdout.write(r.applicationPayloadSha256)' "$DEST/$RECEIPT_NAME")"

cat > "$DEST/CANDIDATE.txt" <<EOF
[VXG RealForever]
sourceHead=$SOURCE_HEAD
sourceTree=$SOURCE_TREE
sourceProof=A009_LOCAL_RELAY_PASS
sourceProofAttempt=$PROOF_ATTEMPT
sourceProofRoute=$PROOF_ROUTE
livedCandidateDmgSha256=$DMG_SHA
livedCandidateAppPayloadSha256=$APP_PAYLOAD_SHA
receiptSchema=vexlife-prod.macos-application-archive-receipt/v1
distributionTrust=AD_HOC_SIGNATURE_NO_DEVELOPER_ID_NO_NOTARIZATION
livedAcceptance=PENDING
EOF

echo "Lived candidate formed and retained:"
echo "$DEST/$DMG_NAME"
echo "DMG SHA-256: $DMG_SHA"
echo "App payload SHA-256: $APP_PAYLOAD_SHA"
/usr/bin/open -R "$DEST/$DMG_NAME" >/dev/null 2>&1 || true
