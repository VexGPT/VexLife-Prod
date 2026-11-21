#!/bin/bash
# [VXG RealForever]
set -euo pipefail

HARNESS_HEAD="c98405c94ea00f668dfc5d9590ccbd1cbda66c68"
HARNESS_TREE="6d690e634aef0452647d47e7b66c85a86ed4d821"
FORMATION_BLOB="b7b53f0f767ab5c72a302c90af9f81316fe9e11a"
FORM_RUN_BLOB="f36a917df082fb063fc6483f708aa03ccaa8f5e6"
RUN_BLOB="b5a9798e25a7b6a7b53b49de775fa31a31051171"

WORK="$(/usr/bin/mktemp -d "$HOME/Downloads/.vexlife-pr481-harness-bootstrap.XXXXXX")"
trap '/bin/rm -rf "$WORK"' EXIT

REPO="$WORK/VexLife-Prod"
echo "[VexRelay] acquiring preserved PR #482 harness $HARNESS_HEAD"
/usr/bin/git clone --no-checkout https://github.com/VexGPT/VexLife-Prod.git "$REPO"
/usr/bin/git -C "$REPO" checkout --detach "$HARNESS_HEAD"

[ "$(/usr/bin/git -C "$REPO" rev-parse HEAD)" = "$HARNESS_HEAD" ] || { echo "Harness head mismatch" >&2; exit 20; }
[ "$(/usr/bin/git -C "$REPO" rev-parse 'HEAD^{tree}')" = "$HARNESS_TREE" ] || { echo "Harness tree mismatch" >&2; exit 21; }
[ "$(/usr/bin/git -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr481/FORMATION-A001.json")" = "$FORMATION_BLOB" ] || { echo "Formation blob mismatch" >&2; exit 22; }
[ "$(/usr/bin/git -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr481/FORM-AND-RUN-A001.mjs")" = "$FORM_RUN_BLOB" ] || { echo "Form/run blob mismatch" >&2; exit 23; }
[ "$(/usr/bin/git -C "$REPO" rev-parse "$HARNESS_HEAD:tools/vexrelay/pr481/RUN-VEXRELAY-COMPANION-ENGAGEMENT-MAC-A001.command")" = "$RUN_BLOB" ] || { echo "Runner blob mismatch" >&2; exit 24; }
[ -z "$(/usr/bin/git -C "$REPO" status --porcelain=v1 -uall)" ] || { echo "Harness checkout is not clean" >&2; exit 25; }

echo "[VexRelay] preserved harness verified; entering exact A001 runner"
/bin/bash "$REPO/tools/vexrelay/pr481/RUN-VEXRELAY-COMPANION-ENGAGEMENT-MAC-A001.command"
