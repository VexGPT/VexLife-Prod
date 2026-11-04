#!/bin/bash
set -euo pipefail

# VexLife-Prod macOS source-local launcher.
#
# Human/product front door: VexGPT/VexLife-Prod.
# Current canonical Conversation/model runtime owner: vgong24/VexLife on 127.0.0.1:18110.
# This launcher never substitutes a second Conversation or raw model path.
#
# [VXG RealForever]

REPOSITORY="VexGPT/VexLife-Prod"
SOURCE_REF="${VEXLIFE_PROD_SOURCE_REF:-main}"
COMPANION_ORIGIN="${VEXLIFE_COMPANION_ORIGIN:-http://127.0.0.1:18110}"
WORKSPACE_PORT="${VEXLIFE_PROD_PORT:-0}"
FRAME_PORT="${VEXLIFE_PROD_FRAME_PORT:-0}"
DATA_DIR="${VEXLIFE_PROD_DATA_DIR:-$HOME/Library/Application Support/VexLife-Prod/exploration/web-workspace}"
BACKEND_BOOTSTRAP_URL="https://raw.githubusercontent.com/vgong24/VexLife/main/setup-vexlife.command"

say() { printf '\n%s\n' "$1"; }
fail() { printf '\nVexLife Prod stopped: %s\n' "$1" >&2; exit 1; }

[ "$(uname -s)" = "Darwin" ] || fail "this source-local launcher is for macOS."
case "$SOURCE_REF" in
  ''|*[!A-Za-z0-9._-]*) fail "the requested VexLife-Prod source ref is not a supported GitHub ref." ;;
esac
case "$WORKSPACE_PORT" in ''|*[!0-9]*) fail "VEXLIFE_PROD_PORT must be numeric." ;; esac
case "$FRAME_PORT" in ''|*[!0-9]*) fail "VEXLIFE_PROD_FRAME_PORT must be numeric." ;; esac

for tool in /usr/bin/curl /usr/bin/tar /usr/bin/plutil /usr/bin/mktemp /usr/bin/open /bin/cat; do
  [ -x "$tool" ] || fail "a required macOS system tool is unavailable: $tool"
done
command -v node >/dev/null 2>&1 || fail "Node.js 20 or newer is required."
NODE_MAJOR="$(node -p 'Number(process.versions.node.split(".")[0])' 2>/dev/null || echo 0)"
[ "$NODE_MAJOR" -ge 20 ] || fail "Node.js 20 or newer is required."

TMP_ROOT="$(/usr/bin/mktemp -d "${TMPDIR:-/tmp}/vexlife-prod.XXXXXX")"
SERVER_PID=""
cleanup() {
  if [ -n "$SERVER_PID" ]; then
    kill "$SERVER_PID" 2>/dev/null || true
    wait "$SERVER_PID" 2>/dev/null || true
  fi
  /bin/rm -rf "$TMP_ROOT"
}
trap cleanup EXIT INT TERM

loopback_json() {
  /usr/bin/curl --noproxy '*' -fsS --max-time 3 "$1" -o "$2" 2>/dev/null
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

say "Getting current VexLife Prod..."
META="$TMP_ROOT/source.json"
ARCHIVE="$TMP_ROOT/vexlife-prod.tar.gz"
EXTRACT_ROOT="$TMP_ROOT/extracted"
/bin/mkdir -p "$EXTRACT_ROOT"

/usr/bin/curl --proto '=https' --tlsv1.2 -fsSL --retry 3 \
  "https://api.github.com/repos/$REPOSITORY/commits/$SOURCE_REF" -o "$META" \
  || fail "the VexLife-Prod source could not be resolved from GitHub."
SOURCE_SHA="$(/usr/bin/plutil -extract sha raw -o - "$META" 2>/dev/null || true)"
case "$SOURCE_SHA" in *[!0-9a-fA-F]*|'') fail "GitHub did not return one exact source identity." ;; esac
[ "${#SOURCE_SHA}" -eq 40 ] || fail "GitHub did not return one exact 40-character source identity."
SOURCE_SHA="$(printf '%s' "$SOURCE_SHA" | tr '[:upper:]' '[:lower:]')"

/usr/bin/curl --proto '=https' --tlsv1.2 -fsSL --retry 3 \
  "https://codeload.github.com/$REPOSITORY/tar.gz/$SOURCE_SHA" -o "$ARCHIVE" \
  || fail "the exact VexLife-Prod source could not be downloaded."

while IFS= read -r entry; do
  case "$entry" in /*|../*|*/../*|*/..) fail "the downloaded source archive contained an unsafe path." ;; esac
done < <(/usr/bin/tar -tzf "$ARCHIVE")
/usr/bin/tar -xzf "$ARCHIVE" -C "$EXTRACT_ROOT"

shopt -s nullglob dotglob
entries=("$EXTRACT_ROOT"/*)
shopt -u nullglob dotglob
[ "${#entries[@]}" -eq 1 ] && [ -d "${entries[0]}" ] || fail "the downloaded VexLife-Prod source did not contain one source root."
SOURCE_ROOT="${entries[0]}"
SERVER="$SOURCE_ROOT/exploration/web-workspace/server.mjs"
[ -f "$SERVER" ] || fail "the current VexLife-Prod source is missing its lived workspace server."

say "VexLife Prod source: $SOURCE_SHA"

if ! companion_ready; then
  say "The canonical VexLife Companion is not READY yet. Opening its current source-local setup first..."
  BACKEND_BOOTSTRAP="$TMP_ROOT/setup-vexlife.command"
  /usr/bin/curl --proto '=https' --tlsv1.2 -fsSL --retry 3 "$BACKEND_BOOTSTRAP_URL" -o "$BACKEND_BOOTSTRAP" \
    || fail "the canonical VexLife backend setup could not be downloaded."
  /bin/bash "$BACKEND_BOOTSTRAP"

  READY=0
  ATTEMPT=0
  while [ "$ATTEMPT" -lt 30 ]; do
    if companion_ready; then READY=1; break; fi
    ATTEMPT=$((ATTEMPT + 1))
    sleep 2
  done
  [ "$READY" -eq 1 ] || fail "the canonical VexLife Companion did not become READY at $COMPANION_ORIGIN. VexLife-Prod will not substitute another Conversation/model path."
fi

say "Canonical VexLife Companion is READY. Starting current VexLife Prod..."
READY_FILE="$TMP_ROOT/workspace-ready.json"
SERVER_LOG="$TMP_ROOT/workspace.log"
/bin/mkdir -p "$DATA_DIR"

VEXLIFE_PROD_SOURCE_SHA="$SOURCE_SHA" node "$SERVER" \
  --port "$WORKSPACE_PORT" \
  --frame-port "$FRAME_PORT" \
  --data "$DATA_DIR" \
  --companion-origin "$COMPANION_ORIGIN" \
  --ready-file "$READY_FILE" \
  >"$SERVER_LOG" 2>&1 &
SERVER_PID=$!

READY=0
WORKSPACE_ORIGIN=""
ATTEMPT=0
while [ "$ATTEMPT" -lt 80 ]; do
  if [ -f "$READY_FILE" ]; then
    STARTUP_SCHEMA="$(/usr/bin/plutil -extract schemaVersion raw -o - "$READY_FILE" 2>/dev/null || true)"
    STARTUP_SOURCE="$(/usr/bin/plutil -extract sourceSha raw -o - "$READY_FILE" 2>/dev/null || true)"
    WORKSPACE_ORIGIN="$(/usr/bin/plutil -extract origin raw -o - "$READY_FILE" 2>/dev/null || true)"
    if [ "$STARTUP_SCHEMA" = "vexlife-prod.workspace-startup/v1" ] \
      && [ "$STARTUP_SOURCE" = "$SOURCE_SHA" ] \
      && [ -n "$WORKSPACE_ORIGIN" ] \
      && /usr/bin/curl --noproxy '*' -fsS --max-time 2 "$WORKSPACE_ORIGIN/api/session" >/dev/null 2>&1; then
      READY=1
      break
    fi
  fi
  if ! kill -0 "$SERVER_PID" 2>/dev/null; then break; fi
  ATTEMPT=$((ATTEMPT + 1))
  sleep 0.25
done

if [ "$READY" -ne 1 ]; then
  if [ -s "$SERVER_LOG" ]; then
    say "VexLife Prod startup log:"
    /bin/cat "$SERVER_LOG" >&2
  fi
  fail "the exact VexLife-Prod source did not reach a source-bound ready state. The startup log above is the first failure; rerunning the same command fetches current main and starts on fresh loopback ports by default."
fi

say "Opening VexLife Prod at $WORKSPACE_ORIGIN"
/usr/bin/open "$WORKSPACE_ORIGIN"
say "VexLife Prod is running from exact source $SOURCE_SHA."
say "Local presentation data: $DATA_DIR"
say "Keep this Terminal window open; press Ctrl+C to stop this exact presentation process."
wait "$SERVER_PID"
