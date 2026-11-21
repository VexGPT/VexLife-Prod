#!/bin/bash
# [VXG RealForever]
set -euo pipefail

SOURCE_HEAD="fc3fee8d212bc106573767c7420ccf4ea6491cba"
SOURCE_TREE="4902b86e659da695854f9b59213d78d022909dc0"
PROOF_ATTEMPT="A002"
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

DEST="$HOME/Downloads/VEXLIFE-RESTART-TAKEOVER-LIFETIME-4B-LIVED-CANDIDATE-\${SOURCE_HEAD:0:12}"
/bin/mkdir -p "$DEST"
[ ! -L "$DEST" ] || { echo "Destination must not be a symlink" >&2; exit 45; }

for name in \
  VexLife-macOS-AppleSilicon.dmg \
  VexLife-macOS-AppleSilicon.dmg.sha256 \
  VexLife-macOS-AppleSilicon.receipt.json
do
  /bin/cp "$OUT/$name" "$DEST/$name"
done

COPIED_SHA="$(/usr/bin/shasum -a 256 "$DEST/VexLife-macOS-AppleSilicon.dmg" | /usr/bin/awk '{print $1}')"
[ "$COPIED_SHA" = "$DMG_SHA" ] || { echo "Copied DMG hash mismatch" >&2; exit 46; }

APP_PAYLOAD_SHA="$("$NODE" -e 'const fs=require("fs");const r=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));process.stdout.write(r.applicationPayloadSha256)' "$DEST/VexLife-macOS-AppleSilicon.receipt.json")"

cat > "$DEST/CANDIDATE.txt" <<EOF
[VXG RealForever]
sourceHead=$SOURCE_HEAD
sourceTree=$SOURCE_TREE
sourceProof=A002_LOCAL_RELAY_PASS
sourceProofAttempt=$PROOF_ATTEMPT
sourceProofRoute=$PROOF_ROUTE
livedCandidateDmgSha256=$DMG_SHA
livedCandidateAppPayloadSha256=$APP_PAYLOAD_SHA
receiptSchema=vexlife-prod.macos-application-archive-receipt/v1
distributionTrust=AD_HOC_SIGNATURE_NO_DEVELOPER_ID_NO_NOTARIZATION
livedAcceptance=PENDING
EOF

echo "Lived candidate formed and retained:"
echo "$DEST/VexLife-macOS-AppleSilicon.dmg"
echo "DMG SHA-256: $DMG_SHA"
echo "App payload SHA-256: $APP_PAYLOAD_SHA"
/usr/bin/open -R "$DEST/VexLife-macOS-AppleSilicon.dmg" >/dev/null 2>&1 || true
