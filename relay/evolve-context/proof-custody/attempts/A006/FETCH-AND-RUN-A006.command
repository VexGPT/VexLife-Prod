#!/bin/bash
set -euo pipefail

CUSTODY_COMMIT="0c6a0563e192da10c52ffbb3287f3de20290b02b"
RUNNER_SHA256="50a6babeaea2276755c790f112cc5347a9445e9852046afaea359f4fcb7df10f"
RUNNER_NAME="VEX-EVOLVECONTEXT-STAGE-A006-PROOF-20261121.zip"
RUNNER_PATH="relay/evolve-context/proof-custody/attempts/A006/$RUNNER_NAME"
URL="https://raw.githubusercontent.com/VexGPT/VexLife-Prod/$CUSTODY_COMMIT/$RUNNER_PATH"
DOWNLOADS="$HOME/Downloads"
ZIP="$DOWNLOADS/$RUNNER_NAME"
TMP="$ZIP.part"
DIR="$DOWNLOADS/VEX-EVOLVECONTEXT-STAGE-A006-PROOF-20261121"

mkdir -p "$DOWNLOADS"
rm -f "$TMP"

command -v curl >/dev/null 2>&1
command -v shasum >/dev/null 2>&1
command -v unzip >/dev/null 2>&1

echo "Downloading exact A006 runner from proof custody..."
curl -fL --retry 3 --retry-delay 1 "$URL" -o "$TMP"
printf '%s  %s\n' "$RUNNER_SHA256" "$TMP" | shasum -a 256 -c -
mv "$TMP" "$ZIP"

rm -rf "$DIR"
unzip -oq "$ZIP" -d "$DOWNLOADS"

echo
echo "Running exact A006 merged-Foundation closure proof..."
/bin/bash "$DIR/RUN-VEX-EVOLVECONTEXT-STAGE-A006.command"
