#!/bin/bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd -P)"
REPOSITORY="${VEXLIFE_REPOSITORY:-VexGPT/VexLife-Prod}"
BASELINE="${VEXLIFE_PREVIEW_BASELINE:-13606a471543ce9d9e4fecd5fb471971b581d6db}"

command -v gh >/dev/null || { echo "GitHub CLI (gh) is required." >&2; exit 1; }
gh auth status -h github.com >/dev/null 2>&1 || {
  echo "GitHub CLI is not authenticated for github.com." >&2
  exit 1
}

[ "$(uname -s)" = "Darwin" ] || {
  echo "VexLife Preview queue must run on macOS." >&2
  exit 1
}
case "$(uname -m)" in
  arm64|aarch64) ;;
  *) echo "VexLife Preview queue requires Apple silicon." >&2; exit 1 ;;
esac

STATUS="$(git -C "$ROOT" status --porcelain=v1 --untracked-files=all)"
[ -z "$STATUS" ] || {
  echo "VexLife Preview queue requires a clean current-main source checkout:" >&2
  printf '%s\n' "$STATUS" >&2
  exit 1
}

git -C "$ROOT" fetch --quiet origin main
SOURCE_HEAD="$(git -C "$ROOT" rev-parse HEAD)"
REMOTE_MAIN="$(git -C "$ROOT" rev-parse refs/remotes/origin/main)"
[ "$SOURCE_HEAD" = "$REMOTE_MAIN" ] || {
  echo "VexLife Preview queue must start from exact current origin/main." >&2
  echo "HEAD=$SOURCE_HEAD" >&2
  echo "origin/main=$REMOTE_MAIN" >&2
  exit 1
}

[[ "$BASELINE" =~ ^[0-9a-f]{40}$ ]] || {
  echo "VexLife Preview baseline must be one exact 40-character Git commit." >&2
  exit 1
}
git -C "$ROOT" merge-base --is-ancestor "$BASELINE" "$REMOTE_MAIN" || {
  echo "VexLife Preview baseline is not an ancestor of current main." >&2
  echo "baseline=$BASELINE" >&2
  echo "origin/main=$REMOTE_MAIN" >&2
  exit 1
}

TMP_ROOT="$(mktemp -d "${TMPDIR:-/tmp}/vexlife-preview-queue.XXXXXX")"
COMMITS="$TMP_ROOT/commits.txt"
cleanup() {
  git -C "$ROOT" worktree prune >/dev/null 2>&1 || true
  rm -rf -- "$TMP_ROOT"
}
trap cleanup EXIT HUP INT TERM

git -C "$ROOT" rev-list --first-parent --reverse "$BASELINE..$REMOTE_MAIN" > "$COMMITS"

attempted=0
published=0
already_published=0
failed=0
failed_shas=""

while IFS= read -r source_sha; do
  [ -n "$source_sha" ] || continue
  tag="private-service-image-$source_sha"

  if gh release view "$tag" --repo "$REPOSITORY" >/dev/null 2>&1; then
    printf 'VEXLIFE_PREVIEW_QUEUE_ALREADY_PUBLISHED\t%s\n' "$source_sha"
    already_published=$((already_published + 1))
    continue
  fi

  attempted=$((attempted + 1))
  worktree="$TMP_ROOT/worktree-$source_sha"
  printf 'VEXLIFE_PREVIEW_QUEUE_ATTEMPT\t%s\n' "$source_sha"

  git -C "$ROOT" worktree add --detach "$worktree" "$source_sha" >/dev/null
  set +e
  (
    cd "$worktree"
    VEXLIFE_REPOSITORY="$REPOSITORY" \
      bash tools/release/publish-mac-release.sh preview "" "$source_sha"
  )
  release_status=$?
  set -e

  git -C "$ROOT" worktree remove --force "$worktree" >/dev/null 2>&1 || true

  if [ "$release_status" -eq 0 ]; then
    published=$((published + 1))
    printf 'VEXLIFE_PREVIEW_QUEUE_PUBLISHED\t%s\n' "$source_sha"
  else
    failed=$((failed + 1))
    failed_shas="${failed_shas}${failed_shas:+,}$source_sha"
    printf 'VEXLIFE_PREVIEW_QUEUE_FAILED\t%s\t%s\n' "$source_sha" "$release_status" >&2
  fi
done < "$COMMITS"

# Successful per-SHA release calls deliberately defer the public catalog. Project it once from
# newest current main after the queue pass so an older historical cutoff never becomes site currentness.
VEXLIFE_REPOSITORY="$REPOSITORY" bash "$ROOT/tools/site/publish-gh-pages.sh"

printf 'VEXLIFE_PREVIEW_QUEUE_RESULT\tmain=%s\tattempted=%s\tpublished=%s\talready=%s\tfailed=%s\n' \
  "$REMOTE_MAIN" "$attempted" "$published" "$already_published" "$failed"

if [ "$failed" -ne 0 ]; then
  printf 'VEXLIFE_PREVIEW_QUEUE_HELD\t%s\n' "$failed_shas" >&2
  exit 1
fi
