#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BUILD_DIR="${TMPDIR:-/tmp}/vexlife-browser-provider-probe"
BINARY="$BUILD_DIR/VexLifeBrowserProviderProbe"

if ! command -v xcrun >/dev/null 2>&1; then
  echo "VEXLIFE_BROWSER_PROBE_FAILURE\tTOOLCHAIN\txcrun is unavailable on this Mac." >&2
  exit 2
fi

if ! xcrun --find swiftc >/dev/null 2>&1; then
  echo "VEXLIFE_BROWSER_PROBE_FAILURE\tTOOLCHAIN\tswiftc is unavailable through xcrun." >&2
  exit 2
fi

mkdir -p "$BUILD_DIR"

xcrun swiftc \
  -parse-as-library \
  -framework AppKit \
  -framework WebKit \
  "$SCRIPT_DIR/BrowserProviderProbe.swift" \
  -o "$BINARY"

LOG="$BUILD_DIR/probe-$PPID-$$.log"
cleanup() { rm -f "$LOG"; }
trap cleanup EXIT HUP INT TERM

set +e
"$BINARY" "$@" 2>&1 | tee "$LOG"
probe_status=${PIPESTATUS[0]}
set -e

if [ "$probe_status" -ne 0 ]; then
  exit "$probe_status"
fi

# The default Relay proof is specifically the Vextreme regression check. A caller-supplied URL
# remains an exploratory launch and does not claim this Vextreme chrome predicate.
if [ "$#" -eq 0 ]; then
  chrome_line="$(grep -F $'VEXLIFE_BROWSER_PROBE_VEXTREME_CHROME\t' "$LOG" | tail -n 1 || true)"
  if [ -z "$chrome_line" ] || \
     [[ "$chrome_line" != *'"present":true'* ]] || \
     [[ "$chrome_line" != *'"text":"Vextreme"'* ]] || \
     [[ "$chrome_line" != *'"text":"VexLife"'* ]] || \
     [[ "$chrome_line" != *'"text":"VexWork"'* ]]; then
    printf '%s\n' $'VEXLIFE_BROWSER_PROBE_FAILURE\tVEXTREME_CHROME_REQUIRED\tCanonical Vextreme / VexLife / VexWork family chrome was not observed.' >&2
    exit 3
  fi
fi

exit 0

# [VXG RealForever]
