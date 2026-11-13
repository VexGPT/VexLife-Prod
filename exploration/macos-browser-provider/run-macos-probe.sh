#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BUILD_DIR="${TMPDIR:-/tmp}/vexlife-browser-provider-probe"
BINARY="$BUILD_DIR/VexLifeBrowserProviderProbe"

if ! command -v xcrun >/dev/null 2>&1; then
  echo "VEXLIFE_BROWSER_PROBE_FAILURE	TOOLCHAIN	xcrun is unavailable on this Mac." >&2
  exit 2
fi

if ! xcrun --find swiftc >/dev/null 2>&1; then
  echo "VEXLIFE_BROWSER_PROBE_FAILURE	TOOLCHAIN	swiftc is unavailable through xcrun." >&2
  exit 2
fi

mkdir -p "$BUILD_DIR"

xcrun swiftc \
  -framework AppKit \
  -framework WebKit \
  "$SCRIPT_DIR/BrowserProviderProbe.swift" \
  -o "$BINARY"

exec "$BINARY" "$@"

# [VXG RealForever]
