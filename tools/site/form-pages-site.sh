#!/bin/bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd -P)"
OUT="${1:-$ROOT/build/vexlife-pages-site}"
REPOSITORY="${VEXLIFE_REPOSITORY:-VexGPT/VexLife-Prod}"
SOURCE_SHA="${VEXLIFE_SOURCE_SHA:-$(git -C "$ROOT" rev-parse HEAD)}"

case "$SOURCE_SHA" in
  *[!0-9a-f]*|'') echo "VexLife site source SHA is invalid: $SOURCE_SHA" >&2; exit 1 ;;
esac
[ "${#SOURCE_SHA}" -eq 40 ] || {
  echo "VexLife site source must be one exact Git commit." >&2
  exit 1
}

command -v python3 >/dev/null || { echo "python3 is required." >&2; exit 1; }
command -v gh >/dev/null || { echo "GitHub CLI (gh) is required to read Releases." >&2; exit 1; }

rm -rf -- "$OUT"
mkdir -p "$OUT/data"
cp -R "$ROOT/site/." "$OUT/"

"$ROOT/gradlew" --no-daemon -q generateVexLifeSiteData   -PvexlifeSiteDataDir="$OUT/data"   -PvexlifeSourceSha="$SOURCE_SHA"

python3 "$ROOT/.github/release/releases-snapshot.py"   --repository "$REPOSITORY"   --main-sha "$SOURCE_SHA"   --out "$OUT/data/releases.json"

# Branch-published Pages should serve the already-formed static bytes exactly as they are.
touch "$OUT/.nojekyll"

printf 'VEXLIFE_PAGES_SITE\t%s\t%s\n' "$SOURCE_SHA" "$OUT"
