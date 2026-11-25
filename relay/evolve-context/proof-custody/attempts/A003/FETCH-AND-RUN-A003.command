#!/bin/bash
set -euo pipefail

CUSTODY_COMMIT="f1c85857947c0f1f50c6522ce4de21621ecc811e"
RUNNER_SHA256="4d7c8264d5a95706fc20d84774670ed440c7e046b130f6b3da8245e5faf57319"
RUNNER_NAME="VEX-EVOLVECONTEXT-STAGE-A003-PROOF-20261121.zip"
RUNNER_PATH="relay/evolve-context/proof-custody/attempts/A003/$RUNNER_NAME"
URL="https://raw.githubusercontent.com/VexGPT/VexLife-Prod/$CUSTODY_COMMIT/$RUNNER_PATH"
DOWNLOADS="$HOME/Downloads"
ZIP="$DOWNLOADS/$RUNNER_NAME"
TMP="$ZIP.part"
DIR="$DOWNLOADS/VEX-EVOLVECONTEXT-STAGE-A003-PROOF-20261121"

mkdir -p "$DOWNLOADS"
rm -f "$TMP"

command -v curl >/dev/null 2>&1
command -v shasum >/dev/null 2>&1
command -v unzip >/dev/null 2>&1

echo "Downloading exact A003 runner from proof custody..."
curl -fL --retry 3 --retry-delay 1 "$URL" -o "$TMP"
printf '%s  %s\n' "$RUNNER_SHA256" "$TMP" | shasum -a 256 -c -
mv "$TMP" "$ZIP"

rm -rf "$DIR"
unzip -oq "$ZIP" -d "$DOWNLOADS"

echo
echo "Running exact A003 proof..."
/bin/bash "$DIR/RUN-VEX-EVOLVECONTEXT-STAGE-A003.command"
