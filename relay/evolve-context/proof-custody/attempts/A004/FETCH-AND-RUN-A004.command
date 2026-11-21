#!/bin/bash
set -euo pipefail

CUSTODY_COMMIT="ba2ea23e1e094e81668939f0e23181a93ead4aea"
RUNNER_SHA256="990b066919a7c1aa84830ffbd7a8978f822b9ee07e507fdb1fe2ac26c5b851ea"
RUNNER_NAME="VEX-EVOLVECONTEXT-STAGE-A004-PROOF-20261121.zip"
RUNNER_PATH="relay/evolve-context/proof-custody/attempts/A004/$RUNNER_NAME"
URL="https://raw.githubusercontent.com/VexGPT/VexLife-Prod/$CUSTODY_COMMIT/$RUNNER_PATH"
DOWNLOADS="$HOME/Downloads"
ZIP="$DOWNLOADS/$RUNNER_NAME"
TMP="$ZIP.part"
DIR="$DOWNLOADS/VEX-EVOLVECONTEXT-STAGE-A004-PROOF-20261121"

mkdir -p "$DOWNLOADS"
rm -f "$TMP"

command -v curl >/dev/null 2>&1
command -v shasum >/dev/null 2>&1
command -v unzip >/dev/null 2>&1

echo "Downloading exact A004 runner from proof custody..."
curl -fL --retry 3 --retry-delay 1 "$URL" -o "$TMP"
printf '%s  %s\n' "$RUNNER_SHA256" "$TMP" | shasum -a 256 -c -
mv "$TMP" "$ZIP"

rm -rf "$DIR"
unzip -oq "$ZIP" -d "$DOWNLOADS"

echo
echo "Running exact A004 diagnostic proof..."
/bin/bash "$DIR/RUN-VEX-EVOLVECONTEXT-STAGE-A004.command"
