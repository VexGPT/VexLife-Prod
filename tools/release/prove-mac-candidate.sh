#!/bin/bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd -P)"
APP_VERSION="${1:-0.0.0}"
DISTRIBUTION_CHANNEL="${2:-candidate}"

proof_fail() {
  printf 'VEXLIFE_MAC_CANDIDATE_PROOF_FAILED\t%s\n' "$1" >&2
  exit 1
}

require_nonempty() {
  local label="$1"
  local path="$2"
  [ -s "$path" ] || proof_fail "$label"
}

require_executable() {
  local label="$1"
  local path="$2"
  [ -x "$path" ] || proof_fail "$label"
}

require_absent() {
  local label="$1"
  local path="$2"
  [ ! -e "$path" ] || proof_fail "$label"
}

[ "$(uname -s)" = "Darwin" ] || {
  echo "VexLife Mac candidate proof must run on macOS." >&2
  exit 1
}
case "$(uname -m)" in
  arm64|aarch64) ;;
  *)
    echo "VexLife Mac candidate proof requires Apple silicon." >&2
    exit 1
    ;;
esac

[[ "$APP_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || {
  echo "usage: bash tools/release/prove-mac-candidate.sh [major.minor.patch] [candidate|preview|stable]" >&2
  echo "default candidate version is 0.0.0" >&2
  exit 1
}
case "$DISTRIBUTION_CHANNEL" in
  candidate|preview)
    [ "$APP_VERSION" = "0.0.0" ] || {
      echo "$DISTRIBUTION_CHANNEL proof must use app version 0.0.0; source SHA is its release identity." >&2
      exit 1
    }
    ;;
  stable)
    [ "$APP_VERSION" != "0.0.0" ] || {
      echo "stable proof requires one semantic app version." >&2
      exit 1
    }
    ;;
  *)
    echo "distribution channel must be candidate, preview, or stable." >&2
    exit 1
    ;;
esac

STATUS="$(git -C "$ROOT" status --porcelain=v1 --untracked-files=all)"
[ -z "$STATUS" ] || {
  echo "VexLife Mac candidate proof requires a clean source checkout:" >&2
  printf '%s\n' "$STATUS" >&2
  exit 1
}

SOURCE_SHA="$(git -C "$ROOT" rev-parse HEAD)"
[[ "$SOURCE_SHA" =~ ^[0-9a-f]{40}$ ]] || {
  echo "candidate source must be one exact 40-character Git commit" >&2
  exit 1
}

cd "$ROOT"

# Candidate proof owns the bounded build-memory profile too. This must not live in a chat-authored
# carrier: exact #490 host proof showed the otherwise-identical canonical route could OOM during
# Kotlin/JVM compilation when the caller did not happen to supply the old wrapper's heap flags.
GRADLE_MEMORY_ARGS=(
  "-Dorg.gradle.jvmargs=-Xmx4g"
  "-Dkotlin.daemon.jvm.options=-Xmx4g"
)

./gradlew --no-daemon "${GRADLE_MEMORY_ARGS[@]}" jvmTest checkVexLifeStrings
./gradlew --no-daemon "${GRADLE_MEMORY_ARGS[@]}" \
  -PvexlifeAppVersion="$APP_VERSION" \
  -PvexlifeDistributionChannel="$DISTRIBUTION_CHANNEL" \
  archiveVexLifeProdMacApplication

APP_DIR="$ROOT/build/vexlife-prod-mac-application/$SOURCE_SHA"
APP="$APP_DIR/VexLife.app"
SOURCE_SHORT="${SOURCE_SHA:0:7}"
case "$DISTRIBUTION_CHANNEL" in
  candidate)
    DMG_NAME="VexLife-Candidate-$SOURCE_SHORT-macOS-AppleSilicon.dmg"
    EXPECTED_VOLUME_NAME="VexLife Candidate $SOURCE_SHORT"
    ;;
  preview)
    DMG_NAME="VexLife-Preview-$SOURCE_SHORT-macOS-AppleSilicon.dmg"
    EXPECTED_VOLUME_NAME="VexLife Preview $SOURCE_SHORT"
    ;;
  stable)
    DMG_NAME="VexLife-$APP_VERSION-macOS-AppleSilicon.dmg"
    EXPECTED_VOLUME_NAME="VexLife $APP_VERSION"
    ;;
esac
DMG="$APP_DIR/$DMG_NAME"
DMG_SUM="$DMG.sha256"
RECEIPT="$APP_DIR/VexLife-macOS-AppleSilicon.receipt.json"

for path in "$APP/Contents/Info.plist" "$DMG" "$DMG_SUM" "$RECEIPT"; do
  [ -f "$path" ] && [ ! -L "$path" ] || {
    echo "required candidate artifact is absent or symlinked: $path" >&2
    exit 1
  }
done

(
  cd "$APP_DIR"
  shasum -a 256 -c "$DMG_NAME.sha256"
)

/usr/bin/hdiutil verify "$DMG" >/dev/null
/usr/bin/codesign --verify --strict "$APP"

EXPECTED_SOURCE="$(/usr/bin/plutil -extract VexLifeProdExpectedSourceSha raw -o - "$APP/Contents/Info.plist")"
[ "$EXPECTED_SOURCE" = "$SOURCE_SHA" ] || {
  echo "formed VexLife.app source does not match exact checkout" >&2
  echo "expected=$SOURCE_SHA" >&2
  echo "observed=$EXPECTED_SOURCE" >&2
  exit 1
}

MOUNT="$(/usr/bin/mktemp -d "${TMPDIR:-/tmp}/vexlife-mac-candidate-proof.XXXXXX")"
mounted=0
cleanup() {
  if [ "$mounted" -eq 1 ]; then
    /usr/bin/hdiutil detach "$MOUNT" >/dev/null 2>&1 || true
  fi
  /bin/rm -rf "$MOUNT"
}
trap cleanup EXIT HUP INT TERM

/usr/bin/hdiutil attach "$DMG" -readonly -nobrowse -mountpoint "$MOUNT" >/dev/null
mounted=1

MOUNTED_VOLUME_NAME="$(
  /usr/sbin/diskutil info -plist "$MOUNT" |
    /usr/bin/plutil -extract VolumeName raw -o - -
)"
[ "$MOUNTED_VOLUME_NAME" = "$EXPECTED_VOLUME_NAME" ] ||
  proof_fail "MOUNTED_DISTRIBUTION_VOLUME_IDENTITY"

MOUNTED_APP="$MOUNT/VexLife.app"
MOUNTED_INFO="$MOUNTED_APP/Contents/Info.plist"
[ -f "$MOUNTED_INFO" ] || {
  echo "verified DMG does not contain VexLife.app" >&2
  exit 1
}

printf 'VEXLIFE_MAC_CANDIDATE_PROOF_STAGE\tMOUNTED_APP_IDENTITY\n'
/usr/bin/plutil -extract CFBundleIconFile raw -o - "$MOUNTED_INFO" | /usr/bin/grep -Fx 'applet'
require_nonempty "MOUNTED_APPLET_ICON" "$MOUNTED_APP/Contents/Resources/applet.icns"

MOUNTED_SOURCE="$(/usr/bin/plutil -extract VexLifeProdExpectedSourceSha raw -o - "$MOUNTED_INFO")"
[ "$MOUNTED_SOURCE" = "$SOURCE_SHA" ] || {
  echo "mounted VexLife.app source does not match exact checkout" >&2
  exit 1
}

PROVIDER="$MOUNTED_APP/Contents/Resources/private-service-image/local-asr-provider/whispercpp/v1.9.5"
printf 'VEXLIFE_MAC_CANDIDATE_PROOF_STAGE\tMOUNTED_PROVIDER\n'
require_executable "MOUNTED_WHISPER_SERVER_EXECUTABLE" "$PROVIDER/bin/whisper-server"
/usr/bin/grep -Fx 'd1be6fde11ac6e0407606b4e42fe72d34add8037' "$PROVIDER/provider-source.txt"
/usr/bin/grep -Fx '6fe2755682242a5c01aba65a22b3c0305d747841' "$PROVIDER/provider-tree.txt"
/usr/bin/grep -F -- '-Dvexlife.privateServiceImageRoot="$ROOT"'   "$MOUNTED_APP/Contents/Resources/private-service-image/bin/vexlife-prod-service"
DYLD_LIBRARY_PATH="$PROVIDER/bin" "$PROVIDER/bin/whisper-server" --help >/dev/null || proof_fail "MOUNTED_WHISPER_SERVER_HELP"

printf 'VEXLIFE_MAC_CANDIDATE_PROOF_STAGE\tMOUNTED_ICON_OVERRIDE_ABSENCE\n'
require_absent "MOUNTED_ASSETS_CAR_ABSENT" "$MOUNTED_APP/Contents/Resources/Assets.car"
if /usr/bin/plutil -extract CFBundleIconName raw -o - "$MOUNTED_INFO" >/dev/null 2>&1; then
  echo "CFBundleIconName must be absent so the applet asset catalog cannot override applet.icns" >&2
  exit 1
fi

printf 'VEXLIFE_MAC_CANDIDATE_PROOF_STAGE\tMOUNTED_VOLUME_HYGIENE\n'
require_absent "MOUNTED_PRIVACY_WEBLOC_ABSENT" "$MOUNT/Open Privacy & Security.webloc"
require_absent "MOUNTED_FSEVENTSD_ABSENT" "$MOUNT/.fseventsd"
require_absent "MOUNTED_SPOTLIGHT_ABSENT" "$MOUNT/.Spotlight-V100"
require_absent "MOUNTED_TRASHES_ABSENT" "$MOUNT/.Trashes"
require_nonempty "MOUNTED_VOLUME_ICON" "$MOUNT/.VolumeIcon.icns"
/usr/bin/xattr -p com.apple.FinderInfo "$MOUNT" >/dev/null ||
  proof_fail "MOUNTED_VOLUME_FINDERINFO"

/usr/bin/hdiutil detach "$MOUNT" >/dev/null
mounted=0
/bin/rm -rf "$MOUNT"
trap - EXIT HUP INT TERM

/usr/bin/plutil -extract distributionTrust raw -o -   "$ROOT/packaging/macos-app/release-contract.json" |
  /usr/bin/grep -Fx 'AD_HOC_SIGNATURE_NO_DEVELOPER_ID_NO_NOTARIZATION'

DMG_SHA="$(/usr/bin/shasum -a 256 "$DMG" | /usr/bin/awk '{print $1}')"

printf 'VEXLIFE_MAC_DISTRIBUTION_IDENTITY\t%s\t%s\t%s\n' \
  "$DISTRIBUTION_CHANNEL" "$DMG_NAME" "$EXPECTED_VOLUME_NAME"
printf 'VEXLIFE_MAC_CANDIDATE_PROVED\t%s\t%s\t%s\t%s\n'   "$SOURCE_SHA"   "$APP_VERSION"   "$DMG"   "$DMG_SHA"
printf 'VEXLIFE_MAC_CANDIDATE_RECEIPT\t%s\n' "$RECEIPT"
