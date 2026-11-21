#!/bin/bash
set -euo pipefail

CUSTODY_COMMIT="860e1632349c3df3e759980d031468bc9af3c67f"
RUNNER_SHA256="8f475ad270acf2ab3b98e9682d4056319995bd93c39fe3d6e112c9a68cb2bb5f"
RUNNER_NAME="VEX-EVOLVECONTEXT-STAGE-A005-PROOF-20261121.zip"
RUNNER_PATH="relay/evolve-context/proof-custody/attempts/A005/$RUNNER_NAME"
URL="https://raw.githubusercontent.com/VexGPT/VexLife-Prod/$CUSTODY_COMMIT/$RUNNER_PATH"
DOWNLOADS="$HOME/Downloads"
ZIP="$DOWNLOADS/$RUNNER_NAME"
TMP="$ZIP.part"
DIR="$DOWNLOADS/VEX-EVOLVECONTEXT-STAGE-A005-PROOF-20261121"

mkdir -p "$DOWNLOADS"
rm -f "$TMP"

command -v curl >/dev/null 2>&1
command -v shasum >/dev/null 2>&1
command -v unzip >/dev/null 2>&1

echo "Downloading exact A005 runner from proof custody..."
curl -fL --retry 3 --retry-delay 1 "$URL" -o "$TMP"
printf '%s  %s\n' "$RUNNER_SHA256" "$TMP" | shasum -a 256 -c -
mv "$TMP" "$ZIP"

rm -rf "$DIR"
unzip -oq "$ZIP" -d "$DOWNLOADS"

echo
echo "Running exact A005 semantic-split proof..."
/bin/bash "$DIR/RUN-VEX-EVOLVECONTEXT-STAGE-A005.command"
