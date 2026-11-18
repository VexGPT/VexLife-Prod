#!/bin/bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd -P)"
REPOSITORY="${VEXLIFE_REPOSITORY:-VexGPT/VexLife-Prod}"
SOURCE_SHA="$(git -C "$ROOT" rev-parse HEAD)"
STATUS="$(git -C "$ROOT" status --porcelain=v1 --untracked-files=all)"

[ -z "$STATUS" ] || {
  echo "VexLife Pages publication requires a clean source checkout:" >&2
  printf '%s\n' "$STATUS" >&2
  exit 1
}

command -v gh >/dev/null || { echo "GitHub CLI (gh) is required." >&2; exit 1; }
gh auth status -h github.com >/dev/null 2>&1 || {
  echo "GitHub CLI is not authenticated for github.com." >&2
  exit 1
}
# Hosted release execution provides GH_TOKEN rather than an interactive credential helper. Let gh
# configure git for the authenticated account before cloning/pushing the separate gh-pages checkout.
gh auth setup-git >/dev/null

git -C "$ROOT" fetch --quiet origin main
REMOTE_MAIN="$(git -C "$ROOT" rev-parse refs/remotes/origin/main)"
[ "$SOURCE_SHA" = "$REMOTE_MAIN" ] || {
  echo "Pages publication requires the exact current origin/main." >&2
  echo "HEAD=$SOURCE_SHA" >&2
  echo "origin/main=$REMOTE_MAIN" >&2
  exit 1
}

TMP_ROOT="$(mktemp -d "${TMPDIR:-/tmp}/vexlife-pages-publish.XXXXXX")"
trap 'rm -rf -- "$TMP_ROOT"' EXIT HUP INT TERM

SITE_ROOT="$TMP_ROOT/site"
VEXLIFE_REPOSITORY="$REPOSITORY" VEXLIFE_SOURCE_SHA="$SOURCE_SHA"   bash "$ROOT/tools/site/form-pages-site.sh" "$SITE_ROOT"

ORIGIN_URL="$(git -C "$ROOT" remote get-url origin)"
PUBLISH_ROOT="$TMP_ROOT/publish"
git clone --quiet --no-checkout "$ORIGIN_URL" "$PUBLISH_ROOT"

if git -C "$PUBLISH_ROOT" show-ref --verify --quiet refs/remotes/origin/gh-pages; then
  git -C "$PUBLISH_ROOT" checkout --quiet -B gh-pages origin/gh-pages
else
  git -C "$PUBLISH_ROOT" checkout --quiet --orphan gh-pages
fi

git -C "$PUBLISH_ROOT" rm -rf . >/dev/null 2>&1 || true
find "$PUBLISH_ROOT" -mindepth 1 -maxdepth 1 ! -name .git -exec rm -rf -- {} +
cp -R "$SITE_ROOT/." "$PUBLISH_ROOT/"

git -C "$PUBLISH_ROOT" add -A
if git -C "$PUBLISH_ROOT" diff --cached --quiet; then
  printf 'VEXLIFE_PAGES_NO_CHANGE\t%s\n' "$SOURCE_SHA"
  exit 0
fi

AUTHOR_NAME="$(git -C "$ROOT" config user.name || true)"
AUTHOR_EMAIL="$(git -C "$ROOT" config user.email || true)"
AUTHOR_NAME="${AUTHOR_NAME:-VexGPT}"
AUTHOR_EMAIL="${AUTHOR_EMAIL:-victor.gong@vextreme24.com}"

git -C "$PUBLISH_ROOT"   -c user.name="$AUTHOR_NAME"   -c user.email="$AUTHOR_EMAIL"   commit --quiet -m "Publish VexLife site from $SOURCE_SHA"

PUBLISH_SHA="$(git -C "$PUBLISH_ROOT" rev-parse HEAD)"
git -C "$PUBLISH_ROOT" push --quiet origin HEAD:refs/heads/gh-pages

printf 'VEXLIFE_PAGES_PUBLISHED\t%s\t%s\n' "$SOURCE_SHA" "$PUBLISH_SHA"
