#!/bin/bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd -P)"
CHANNEL="${1:-}"
VERSION="${2:-}"
REPOSITORY="${VEXLIFE_REPOSITORY:-VexGPT/VexLife-Prod}"

command -v gh >/dev/null || { echo "GitHub CLI (gh) is required." >&2; exit 1; }
gh auth status -h github.com >/dev/null 2>&1 || {
  echo "GitHub CLI is not authenticated for github.com." >&2
  exit 1
}

[ "$(uname -s)" = "Darwin" ] || {
  echo "VexLife Mac release publication must run on macOS." >&2
  exit 1
}
case "$(uname -m)" in
  arm64|aarch64) ;;
  *) echo "VexLife Mac release publication requires Apple silicon." >&2; exit 1 ;;
esac

STATUS="$(git -C "$ROOT" status --porcelain=v1 --untracked-files=all)"
[ -z "$STATUS" ] || {
  echo "VexLife release publication requires a clean source checkout:" >&2
  printf '%s\n' "$STATUS" >&2
  exit 1
}

git -C "$ROOT" fetch --quiet origin main
SOURCE_SHA="$(git -C "$ROOT" rev-parse HEAD)"
REMOTE_MAIN="$(git -C "$ROOT" rev-parse refs/remotes/origin/main)"
[ "$SOURCE_SHA" = "$REMOTE_MAIN" ] || {
  echo "Public releases are cut from exact current origin/main." >&2
  echo "HEAD=$SOURCE_SHA" >&2
  echo "origin/main=$REMOTE_MAIN" >&2
  exit 1
}

case "$CHANNEL" in
  preview)
    [ -z "$VERSION" ] || {
      echo "Preview does not take a semantic version; leave version empty." >&2
      exit 1
    }
    APP_VERSION="0.0.0"
    ;;
  stable)
    [[ "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || {
      echo "Stable version must look like 0.1.0 (got '$VERSION')." >&2
      exit 1
    }
    if gh release view "v$VERSION" --repo "$REPOSITORY" >/dev/null 2>&1; then
      echo "v$VERSION already exists; stable versions are never rebuilt or moved." >&2
      exit 1
    fi
    APP_VERSION="$VERSION"
    ;;
  *)
    echo "usage: bash tools/release/publish-mac-release.sh preview | stable <major.minor.patch>" >&2
    exit 1
    ;;
esac

cd "$ROOT"
./gradlew --no-daemon jvmTest checkVexLifeStrings
./gradlew --no-daemon -PvexlifeAppVersion="$APP_VERSION" archiveVexLifeProdMacApplication

SERVICE_DIR="$ROOT/build/vexlife-prod-service-image/archives/$SOURCE_SHA"
APP_DIR="$ROOT/build/vexlife-prod-mac-application/$SOURCE_SHA"

test -f "$APP_DIR/VexLife-macOS-AppleSilicon.dmg"
test -f "$APP_DIR/VexLife-macOS-AppleSilicon.dmg.sha256"
test -f "$APP_DIR/VexLife-macOS-AppleSilicon.receipt.json"
(cd "$APP_DIR" && shasum -a 256 -c "VexLife-macOS-AppleSilicon.dmg.sha256")
/usr/bin/hdiutil verify "$APP_DIR/VexLife-macOS-AppleSilicon.dmg" >/dev/null

if [ "$CHANNEL" = "preview" ]; then
  TAG="private-service-image-$SOURCE_SHA"
  NOTES="$(mktemp "${TMPDIR:-/tmp}/vexlife-preview-notes.XXXXXX")"
  trap 'rm -f -- "$NOTES"' EXIT HUP INT TERM
  {
    echo "Preview of $SOURCE_SHA for Mac (Apple silicon)."
    echo
    echo "Preferred download: VexLife-macOS-AppleSilicon.dmg."
    echo
    echo "**Trust:** this open-source build is ad-hoc signed but is not Apple Developer ID signed or notarized. macOS may require explicit approval before first launch and may separately ask to open the bundled Java runtime."
    echo
    echo "source-sha: $SOURCE_SHA"
    for file in "$APP_DIR"/*.dmg "$SERVICE_DIR"/*.command; do
      echo "sha256 $(basename "$file") $(shasum -a 256 "$file" | awk '{print $1}')"
    done
  } > "$NOTES"

  if gh release view "$TAG" --repo "$REPOSITORY" >/dev/null 2>&1; then
    gh release upload "$TAG" "$SERVICE_DIR"/* "$APP_DIR"/*.dmg "$APP_DIR"/*.sha256 "$APP_DIR"/*.receipt.json --clobber --repo "$REPOSITORY"
    gh release edit "$TAG" --title "Preview ${SOURCE_SHA:0:7}" --notes-file "$NOTES" --repo "$REPOSITORY"
  else
    gh release create "$TAG" "$SERVICE_DIR"/* "$APP_DIR"/*.dmg "$APP_DIR"/*.sha256 "$APP_DIR"/*.receipt.json       --repo "$REPOSITORY" --target "$SOURCE_SHA" --prerelease       --title "Preview ${SOURCE_SHA:0:7}" --notes-file "$NOTES"
  fi
else
  NOTES="$(mktemp "${TMPDIR:-/tmp}/vexlife-stable-notes.XXXXXX")"
  trap 'rm -f -- "$NOTES"' EXIT HUP INT TERM
  {
    echo "VexLife $VERSION for Mac (Apple silicon), cut from main at $SOURCE_SHA."
    echo
    echo "This stable version records a feature threshold that was lived and accepted."
    echo
    echo "**Trust:** this open-source build is ad-hoc signed but is not Apple Developer ID signed or notarized. macOS may require explicit approval before first launch and may separately ask to open the bundled Java runtime."
    echo
    echo "source-sha: $SOURCE_SHA"
    for file in "$APP_DIR"/*.dmg "$SERVICE_DIR"/*.command; do
      echo "sha256 $(basename "$file") $(shasum -a 256 "$file" | awk '{print $1}')"
    done
  } > "$NOTES"

  gh release create "v$VERSION"     "$APP_DIR"/*.dmg "$APP_DIR"/*.sha256 "$APP_DIR"/*.receipt.json     "$SERVICE_DIR"/*.command "$SERVICE_DIR"/*.sha256     --repo "$REPOSITORY" --target "$SOURCE_SHA" --title "VexLife $VERSION"     --notes-file "$NOTES" --latest
fi

# Release existence and the public download catalog are one human-facing publication outcome.
# Form/publish the Pages projection only after GitHub has accepted the exact release.
VEXLIFE_REPOSITORY="$REPOSITORY" bash "$ROOT/tools/site/publish-gh-pages.sh"

printf 'VEXLIFE_RELEASE_PUBLISHED\t%s\t%s\t%s\n' "$CHANNEL" "$SOURCE_SHA" "${VERSION:-preview}"
