#!/bin/bash
# [VXG RealForever]
set -euo pipefail

SDK_HEAD="6f8f6f50467f5df5a542713f7dc25b82db4ac8fa"
SDK_TREE="96795640b01f1969da980f52d5a8ec136a8c44aa"
CONTRACT_BLOB="b53cbac01c2deeb45fad5c32ce98e0796c04126f"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd -P)"
WORK="$(/usr/bin/mktemp -d "$HOME/Downloads/.vexlife-a007-lived-runtime-release.XXXXXX")"
trap '/bin/rm -rf "$WORK"' EXIT

GH=""
for c in /opt/homebrew/bin/gh /usr/local/bin/gh; do
  if [ -x "$c" ]; then GH="$c"; break; fi
done
[ -n "$GH" ] || { echo "VexLife lived proof: GitHub CLI not found." >&2; exit 20; }

NODE=""
for c in /opt/homebrew/bin/node /usr/local/bin/node /usr/bin/node; do
  if [ -x "$c" ]; then NODE="$c"; break; fi
done
[ -n "$NODE" ] || { echo "VexLife lived proof: qualified Node not found." >&2; exit 21; }

"$GH" auth status -h github.com >/dev/null 2>&1 || {
  echo "VexLife lived proof: existing GitHub CLI authentication is required to read private Vextreme-SDK." >&2
  exit 22
}

SDK="$WORK/Vextreme-SDK"
echo "[VexLife] acquiring exact accepted VexLocalBridge source"
"$GH" repo clone vgong24/Vextreme-SDK "$SDK" -- --no-checkout
/usr/bin/git -C "$SDK" checkout --detach "$SDK_HEAD"
[ "$(/usr/bin/git -C "$SDK" rev-parse HEAD)" = "$SDK_HEAD" ] || { echo "SDK head mismatch" >&2; exit 23; }
[ "$(/usr/bin/git -C "$SDK" rev-parse 'HEAD^{tree}')" = "$SDK_TREE" ] || { echo "SDK tree mismatch" >&2; exit 24; }
[ "$(/usr/bin/git -C "$SDK" rev-parse "$SDK_HEAD:docs/private-continuity/vexrelay/current-contract.json")" = "$CONTRACT_BLOB" ] || {
  echo "SDK current-contract blob mismatch" >&2; exit 25;
}
[ -z "$(/usr/bin/git -C "$SDK" status --porcelain=v1 -uall)" ] || { echo "SDK checkout is not clean" >&2; exit 26; }

echo "[VexLife] forming and running exact A007 lived runtime-release task"
"$NODE" \
  "$SCRIPT_DIR/FORM-AND-RUN-A007-LIVED-RUNTIME-RELEASE.mjs" \
  "$SDK" \
  "$SCRIPT_DIR/A007-LIVED-RUNTIME-RELEASE-PAYLOAD.mjs" \
  "$HOME/Downloads"
