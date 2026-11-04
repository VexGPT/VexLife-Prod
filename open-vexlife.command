#!/bin/bash
set -euo pipefail

# VexLife-Prod macOS front-door candidate.
#
# Reuses the accepted VexLife Mac setup/lifecycle for Home, runtime, repair,
# source rotation and canonical Companion readiness. VexLife-Prod owns only
# its separate presentation source/process relationship.
#
# [VXG RealForever]

PROD_REPOSITORY="VexGPT/VexLife-Prod"
VEXLIFE_REPOSITORY="vgong24/VexLife"
PROD_SOURCE_REF="${VEXLIFE_PROD_SOURCE_REF:-main}"
PROD_SOURCE_ROOT="${VEXLIFE_PROD_SOURCE_ROOT:-$HOME/Library/Application Support/VexLife-Prod/source}"
PROD_RUNTIME_ROOT="${VEXLIFE_PROD_RUNTIME_ROOT:-$HOME/Library/Application Support/VexLife-Prod}"
COMPANION_ORIGIN="${VEXLIFE_COMPANION_ORIGIN:-http://127.0.0.1:18110}"
BACKEND_SETUP_MODE="${VEXLIFE_PROD_VEXLIFE_SETUP_MODE:-window}"

say() { printf '\n%s\n' "$1"; }
fail() { printf '\nVexLife Prod stopped: %s\n' "$1" >&2; exit 1; }

[ "$(uname -s)" = "Darwin" ] || fail "this source-local front door is for macOS."
case "$PROD_SOURCE_REF" in
  ''|*[!A-Za-z0-9._-]*) fail "the requested VexLife-Prod source ref is not a supported GitHub ref." ;;
esac
case "$BACKEND_SETUP_MODE" in
  window|terminal) ;;
  *) fail "VEXLIFE_PROD_VEXLIFE_SETUP_MODE must be 'window' or 'terminal'." ;;
esac
for tool in /usr/bin/curl /usr/bin/tar /usr/bin/plutil /usr/bin/mktemp /usr/bin/open; do
  [ -x "$tool" ] || fail "a required macOS system tool is unavailable: $tool"
done

TMP_ROOT="$(/usr/bin/mktemp -d "${TMPDIR:-/tmp}/vexlife-prod-open.XXXXXX")"
trap '/bin/rm -rf "$TMP_ROOT"' EXIT

loopback_json() {
  /usr/bin/curl --noproxy '*' -fsS --max-time 4 "$1" -o "$2" 2>/dev/null
}

companion_ready() {
  local receipt="$TMP_ROOT/companion-availability.json"
  if ! loopback_json "$COMPANION_ORIGIN/api/v1/companion/availability" "$receipt"; then
    return 1
  fi
  local schema state
  schema="$(/usr/bin/plutil -extract schemaVersion raw -o - "$receipt" 2>/dev/null || true)"
  state="$(/usr/bin/plutil -extract availabilityState raw -o - "$receipt" 2>/dev/null || true)"
  [ "$schema" = "vexlife.companion-availability/v1" ] && [ "$state" = "READY" ]
}

# ---------------------------------------------------------------------------
# 1. Delegate Home/runtime/open/repair/currentness to the existing VexLife owner.
# ---------------------------------------------------------------------------

say "Opening the current VexLife setup / Open Vex owner..."
BACKEND_BOOTSTRAP="$TMP_ROOT/setup-vexlife.command"
/usr/bin/curl --proto '=https' --tlsv1.2 -fsSL --retry 3 \
  "https://raw.githubusercontent.com/$VEXLIFE_REPOSITORY/main/setup-vexlife.command" \
  -o "$BACKEND_BOOTSTRAP" \
  || fail "the canonical VexLife Mac front door could not be downloaded."

VEXLIFE_SETUP_MODE="$BACKEND_SETUP_MODE" /bin/bash "$BACKEND_BOOTSTRAP" \
  || fail "the canonical VexLife setup/lifecycle did not complete."

companion_ready \
  || fail "the canonical VexLife lifecycle did not finish with Companion READY. Use its current Open/repair route before VexLife-Prod presentation startup."

command -v node >/dev/null 2>&1 || fail "Node.js 20 or newer is required after canonical VexLife setup."
NODE_MAJOR="$(node -p 'Number(process.versions.node.split(".")[0])' 2>/dev/null || echo 0)"
[ "$NODE_MAJOR" -ge 20 ] || fail "Node.js 20 or newer is required after canonical VexLife setup."

# ---------------------------------------------------------------------------
# 2. Materialize one exact VexLife-Prod source in a persistent private run root.
#    This follows the accepted VexLife exact-source bootstrap shape.
# ---------------------------------------------------------------------------

say "Getting current VexLife Prod presentation source..."
PROD_META="$TMP_ROOT/vexlife-prod-source.json"
PROD_ARCHIVE="$TMP_ROOT/vexlife-prod.tar.gz"
PROD_EXTRACT="$TMP_ROOT/vexlife-prod-extracted"
/bin/mkdir -p "$PROD_EXTRACT"

/usr/bin/curl --proto '=https' --tlsv1.2 -fsSL --retry 3 \
  "https://api.github.com/repos/$PROD_REPOSITORY/commits/$PROD_SOURCE_REF" -o "$PROD_META" \
  || fail "the VexLife-Prod source could not be resolved from GitHub."
PROD_SOURCE_SHA="$(/usr/bin/plutil -extract sha raw -o - "$PROD_META" 2>/dev/null || true)"
case "$PROD_SOURCE_SHA" in *[!0-9a-fA-F]*|'') fail "GitHub did not return one exact VexLife-Prod source identity." ;; esac
[ "${#PROD_SOURCE_SHA}" -eq 40 ] || fail "GitHub did not return one exact 40-character VexLife-Prod source identity."
PROD_SOURCE_SHA="$(printf '%s' "$PROD_SOURCE_SHA" | tr '[:upper:]' '[:lower:]')"

/usr/bin/curl --proto '=https' --tlsv1.2 -fsSL --retry 3 \
  "https://codeload.github.com/$PROD_REPOSITORY/tar.gz/$PROD_SOURCE_SHA" -o "$PROD_ARCHIVE" \
  || fail "the exact VexLife-Prod source could not be downloaded."
while IFS= read -r entry; do
  case "$entry" in /*|../*|*/../*|*/..) fail "the VexLife-Prod source archive contained an unsafe path." ;; esac
done < <(/usr/bin/tar -tzf "$PROD_ARCHIVE")
/usr/bin/tar -xzf "$PROD_ARCHIVE" -C "$PROD_EXTRACT"

shopt -s nullglob dotglob
entries=("$PROD_EXTRACT"/*)
shopt -u nullglob dotglob
[ "${#entries[@]}" -eq 1 ] && [ -d "${entries[0]}" ] \
  || fail "the downloaded VexLife-Prod source did not contain one source root."
DOWNLOADED_ROOT="${entries[0]}"
[ -f "$DOWNLOADED_ROOT/exploration/web-workspace/server.mjs" ] \
  || fail "the exact VexLife-Prod source is missing its presentation server."
[ -f "$DOWNLOADED_ROOT/exploration/web-workspace/tools/macos-presentation.mjs" ] \
  || fail "the exact VexLife-Prod source is missing its presentation lifecycle adapter."

/bin/mkdir -p "$PROD_SOURCE_ROOT"
RUN_PARENT="$(/usr/bin/mktemp -d "$PROD_SOURCE_ROOT/$PROD_SOURCE_SHA.run.XXXXXX")" \
  || fail "a fresh exact VexLife-Prod source location could not be reserved safely."
TARGET="$RUN_PARENT/source"
/bin/mv "$DOWNLOADED_ROOT" "$TARGET" \
  || fail "the fresh exact VexLife-Prod source could not be materialized safely."

# ---------------------------------------------------------------------------
# 3. Consume the current VexLife launchd substrate instead of reimplementing it.
#    The module is immutable because it is fetched from one exact VexLife SHA.
# ---------------------------------------------------------------------------

VEX_META="$TMP_ROOT/vexlife-source.json"
/usr/bin/curl --proto '=https' --tlsv1.2 -fsSL --retry 3 \
  "https://api.github.com/repos/$VEXLIFE_REPOSITORY/commits/main" -o "$VEX_META" \
  || fail "current VexLife source identity could not be resolved."
VEX_SOURCE_SHA="$(/usr/bin/plutil -extract sha raw -o - "$VEX_META" 2>/dev/null || true)"
case "$VEX_SOURCE_SHA" in *[!0-9a-fA-F]*|'') fail "GitHub did not return one exact VexLife source identity." ;; esac
[ "${#VEX_SOURCE_SHA}" -eq 40 ] || fail "GitHub did not return one exact 40-character VexLife source identity."
VEX_SOURCE_SHA="$(printf '%s' "$VEX_SOURCE_SHA" | tr '[:upper:]' '[:lower:]')"

SUBSTRATE_MODULE="$TMP_ROOT/macos-user-launchd-process.mjs"
/usr/bin/curl --proto '=https' --tlsv1.2 -fsSL --retry 3 \
  "https://raw.githubusercontent.com/$VEXLIFE_REPOSITORY/$VEX_SOURCE_SHA/src/core/macos-user-launchd-process.mjs" \
  -o "$SUBSTRATE_MODULE" \
  || fail "the exact current VexLife macOS launchd substrate could not be downloaded."

# ---------------------------------------------------------------------------
# 4. Let the bounded Prod presentation owner reuse/rotate only its exact process.
# ---------------------------------------------------------------------------

say "Starting current VexLife Prod presentation..."
RESULT="$TMP_ROOT/presentation-result.json"
if ! node "$TARGET/exploration/web-workspace/tools/macos-presentation.mjs" \
  --operation start \
  --source-root "$TARGET" \
  --source-sha "$PROD_SOURCE_SHA" \
  --runtime-root "$PROD_RUNTIME_ROOT" \
  --companion-origin "$COMPANION_ORIGIN" \
  --substrate-module "$SUBSTRATE_MODULE" >"$RESULT"; then
  for log in presentation.out.log presentation.err.log; do
    if [ -f "$PROD_RUNTIME_ROOT/runtime/$log" ]; then
      say "VexLife Prod $log:"
      /bin/cat "$PROD_RUNTIME_ROOT/runtime/$log" >&2 || true
    fi
  done
  fail "the VexLife-Prod presentation lifecycle did not reach current exact-source readiness."
fi

STATE="$(/usr/bin/plutil -extract state raw -o - "$RESULT" 2>/dev/null || true)"
ORIGIN="$(/usr/bin/plutil -extract origin raw -o - "$RESULT" 2>/dev/null || true)"
case "$STATE" in
  STARTED_CURRENT|REUSED_CURRENT) ;;
  *) fail "the VexLife-Prod presentation lifecycle returned an unexpected state: ${STATE:-UNKNOWN}" ;;
esac
case "$ORIGIN" in
  http://127.0.0.1:*) ;;
  *) fail "the VexLife-Prod presentation lifecycle did not return one numeric-loopback origin." ;;
esac

say "Opening VexLife Prod at $ORIGIN"
/usr/bin/open "$ORIGIN" >/dev/null 2>&1 \
  || fail "the current VexLife-Prod presentation could not be opened."

say "VexLife Prod is ready."
say "Prod source: $PROD_SOURCE_SHA"
say "VexLife lifecycle source used for the launchd substrate: $VEX_SOURCE_SHA"
say "Presentation ownership receipt: $PROD_RUNTIME_ROOT/recovery/presentation-process.json"

# [VXG RealForever]
