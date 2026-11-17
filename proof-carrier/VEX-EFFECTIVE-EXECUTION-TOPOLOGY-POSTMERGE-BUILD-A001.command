#!/bin/bash
set -euo pipefail

# [VXG RealForever]
# Post-merge lived build for PR #228.
# Builds exact accepted VexLife-Prod main only. No Home/runtime/model mutation is performed here.
# Human install/run remains explicit after the DMG is formed.

REPO='VexGPT/VexLife-Prod'
SOURCE_SHA='d0cba51decfe9057ac9afc007fdeb37da213cc97'
SOURCE_TREE='ac1f6f387606908c71de8bdef0727436015d987c'
OUT="$HOME/Downloads/VEXLIFE-TOPOLOGY-POSTMERGE-D0CBA51D"

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

GH="$(resolve_exec /opt/homebrew/bin/gh /usr/local/bin/gh)" || {
  printf '%s\n' 'gh is unavailable.' >&2
  exit 12
}
GIT="$(resolve_exec /usr/bin/git)" || exit 11
J21="$(/usr/libexec/java_home -v 21 2>/dev/null)" || {
  printf '%s\n' 'JDK 21 is unavailable.' >&2
  exit 21
}
[ -x "$J21/bin/java" ] || exit 22

"$GH" auth status --hostname github.com >/dev/null 2>&1 || {
  printf '%s\n' 'GitHub CLI is not authenticated.' >&2
  exit 23
}

WORK="$(/usr/bin/mktemp -d "$HOME/Downloads/.vexlife-topology-postmerge-build.XXXXXX")"
cleanup() {
  case "$WORK" in
    "$HOME/Downloads"/.vexlife-topology-postmerge-build.*) /bin/rm -rf -- "$WORK" ;;
  esac
}
trap cleanup EXIT HUP INT TERM

printf '%s\n' 'VexLife topology post-merge build: acquiring exact accepted source...'
"$GH" repo clone "$REPO" "$WORK/repo" -- --no-checkout >/dev/null
"$GIT" -C "$WORK/repo" checkout --detach "$SOURCE_SHA" >/dev/null

ACTUAL_HEAD="$("$GIT" -C "$WORK/repo" rev-parse HEAD)"
ACTUAL_TREE="$("$GIT" -C "$WORK/repo" rev-parse 'HEAD^{tree}')"
[ "$ACTUAL_HEAD" = "$SOURCE_SHA" ] || {
  printf 'Head mismatch: %s\n' "$ACTUAL_HEAD" >&2
  exit 31
}
[ "$ACTUAL_TREE" = "$SOURCE_TREE" ] || {
  printf 'Tree mismatch: %s\n' "$ACTUAL_TREE" >&2
  exit 32
}
[ -z "$("$GIT" -C "$WORK/repo" status --porcelain=v1 -uall)" ] || exit 33

export JAVA_HOME="$J21"
export PATH="$JAVA_HOME/bin:$PATH"

printf '%s\n' 'Building exact accepted VexLife Mac application...'
(
  cd "$WORK/repo"
  ./gradlew --no-daemon -Dorg.gradle.java.home="$J21"     -PvexlifeAppVersion=0.0.0     archiveVexLifeProdMacApplication
)

APP_DIR="$WORK/repo/build/vexlife-prod-mac-application/$SOURCE_SHA"
DMG="$APP_DIR/VexLife-macOS-AppleSilicon.dmg"
SUM="$APP_DIR/VexLife-macOS-AppleSilicon.dmg.sha256"
RECEIPT="$APP_DIR/VexLife-macOS-AppleSilicon.receipt.json"

test -f "$DMG"
test -f "$SUM"
test -f "$RECEIPT"

(
  cd "$APP_DIR"
  /usr/bin/shasum -a 256 -c "VexLife-macOS-AppleSilicon.dmg.sha256"
)
/usr/bin/hdiutil verify "$DMG"

rm -rf "$OUT"
mkdir -p "$OUT"
cp -f "$DMG" "$OUT/"
cp -f "$SUM" "$OUT/"
cp -f "$RECEIPT" "$OUT/"

cat > "$OUT/START-HERE.txt" <<'EOF'
VexLife post-merge topology verification
=========================================

Exact accepted Product source:
d0cba51decfe9057ac9afc007fdeb37da213cc97
tree:
ac1f6f387606908c71de8bdef0727436015d987c

What changed in PR #228:
- Added a read-only effective execution topology.
- No runtime settings changed.
- No prompt/context budgets changed.
- No capability limits changed.
- No model architecture changed.
- No Home / Conversation / Memory behavior changed.

What you should observe:
1. Install/open VexLife exactly as before.
2. Existing Home should remain recognized.
3. Existing selected model/artifacts should remain recognized; no forced re-download.
4. Ordinary conversation should work as before.
5. Capability reach should work as before.
6. The per-human-turn capability request bound is still 4.
7. Product prompt-cache-size is still 1.
8. The number 10 is only mlx-lm 0.32.0's inherited prompt-cache default that Product overrides to 1.
9. There is no new visible "10 rounds" behavior from PR #228.

If any of those behavioral points differ, treat it as a regression rather than expected topology behavior.

After the check, export Debug & diagnostics from VexLife and return that ZIP if you want the lived
post-merge run verified against the previously working circuit.
EOF

/usr/bin/shasum -a 256 "$OUT/VexLife-macOS-AppleSilicon.dmg" > "$OUT/POSTMERGE-DMG.sha256"

printf '\n%s\n' 'Build complete.'
printf 'Folder: %s\n' "$OUT"
printf 'Source: %s\n' "$SOURCE_SHA"
printf 'Tree:   %s\n' "$SOURCE_TREE"
printf '\nOpen START-HERE.txt, then install/run the DMG exactly as you normally do.\n'

/usr/bin/open "$OUT" || true
