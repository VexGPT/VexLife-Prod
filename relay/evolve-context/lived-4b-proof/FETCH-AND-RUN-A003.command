#!/bin/bash
set -euo pipefail

PRODUCT_REPOSITORY='VexGPT/VexLife-Prod'
PRODUCT_MAIN='e1c18be71f73c8ec51818a960c0d9b50b94efc63'
PRODUCT_MAIN_TREE='60b9d3498b23a53a97c1d555a30b678681224dca'
PROOF_PR='512'
PROOF_BRANCH='VXG-112226-vex-evolve-context-lived-4b-proof-a003'
RUNNER_PATH='tools/continuity-pressure/evolve-context-lived-4b-proof.mjs'
PAYLOAD_PATH='relay/evolve-context/lived-4b-proof/payload.cjs'
FORM_PATH='relay/evolve-context/lived-4b-proof/form-and-run.cjs'
BOOTSTRAP_PATH='relay/evolve-context/lived-4b-proof/FETCH-AND-RUN-A003.command'
SDK_MAIN='6f8f6f50467f5df5a542713f7dc25b82db4ac8fa'
SDK_TREE='96795640b01f1969da980f52d5a8ec136a8c44aa'
CARRIER_PR='1956'
CARRIER_HEAD='149455eeec4c7b7ca00e91022154a85faa3d608c'
CARRIER_TREE='53ea701f9bbf24a5cf37d43e4dbe3836187f4011'
DOWNLOADS="$HOME/Downloads"
EXPECTED_PROOF_HEAD="\${VEX_EXPECTED_PROOF_HEAD:-}"

resolve_exec() {
  local candidate
  for candidate in "$@"; do
    if [ -x "$candidate" ] && [ -f "$candidate" ]; then printf '%s' "$candidate"; return 0; fi
  done
  return 1
}

GH_BIN="$(resolve_exec /opt/homebrew/bin/gh /usr/local/bin/gh)" || { printf '%s\n' 'EvolveContext lived A003: qualified GitHub CLI unavailable.' >&2; exit 10; }
GIT_BIN="$(resolve_exec /usr/bin/git)" || { printf '%s\n' 'EvolveContext lived A003: qualified Git unavailable.' >&2; exit 11; }
NODE_BIN="$(resolve_exec /opt/homebrew/bin/node /usr/local/bin/node /usr/bin/node)" || { printf '%s\n' 'EvolveContext lived A003: qualified Node unavailable.' >&2; exit 12; }
MKTEMP_BIN="$(resolve_exec /usr/bin/mktemp)" || { printf '%s\n' 'EvolveContext lived A003: qualified mktemp unavailable.' >&2; exit 13; }
RM_BIN="$(resolve_exec /bin/rm /usr/bin/rm)" || { printf '%s\n' 'EvolveContext lived A003: qualified rm unavailable.' >&2; exit 14; }

[ -n "$EXPECTED_PROOF_HEAD" ] || { printf '%s\n' 'EvolveContext lived A003: exact proof head was not supplied by the digest-bound bootstrap command.' >&2; exit 15; }
[ -d "$DOWNLOADS" ] || { printf '%s\n' 'EvolveContext lived A003: Downloads directory unavailable.' >&2; exit 16; }
"$GH_BIN" auth status --hostname github.com >/dev/null 2>&1 || { printf '%s\n' 'EvolveContext lived A003: GitHub authentication unavailable.' >&2; exit 17; }

printf '%s\n' 'EvolveContext lived A003: re-grounding Product, proof companion, SDK, and carrier...'
OBS_PRODUCT_MAIN="$("$GH_BIN" api "repos/$PRODUCT_REPOSITORY/commits/main" --jq '.sha')"
[ "$OBS_PRODUCT_MAIN" = "$PRODUCT_MAIN" ] || { printf 'Product main moved. expected=%s observed=%s\n' "$PRODUCT_MAIN" "$OBS_PRODUCT_MAIN" >&2; exit 20; }
OBS_PRODUCT_TREE="$("$GH_BIN" api "repos/$PRODUCT_REPOSITORY/git/commits/$PRODUCT_MAIN" --jq '.tree.sha')"
[ "$OBS_PRODUCT_TREE" = "$PRODUCT_MAIN_TREE" ] || { printf 'Product main tree moved. expected=%s observed=%s\n' "$PRODUCT_MAIN_TREE" "$OBS_PRODUCT_TREE" >&2; exit 21; }

PROOF_STATE="$("$GH_BIN" api "repos/$PRODUCT_REPOSITORY/pulls/$PROOF_PR" --jq '.state')"
PROOF_HEAD="$("$GH_BIN" api "repos/$PRODUCT_REPOSITORY/pulls/$PROOF_PR" --jq '.head.sha')"
PROOF_BASE="$("$GH_BIN" api "repos/$PRODUCT_REPOSITORY/pulls/$PROOF_PR" --jq '.base.sha')"
PROOF_HEAD_BRANCH="$("$GH_BIN" api "repos/$PRODUCT_REPOSITORY/pulls/$PROOF_PR" --jq '.head.ref')"
[ "$PROOF_STATE" = 'open' ] || { printf 'Proof PR #%s is not open. state=%s\n' "$PROOF_PR" "$PROOF_STATE" >&2; exit 22; }
[ "$PROOF_HEAD" = "$EXPECTED_PROOF_HEAD" ] || { printf 'Proof PR head moved. expected=%s observed=%s\n' "$EXPECTED_PROOF_HEAD" "$PROOF_HEAD" >&2; exit 23; }
[ "$PROOF_BASE" = "$PRODUCT_MAIN" ] || { printf 'Proof PR base moved. expected=%s observed=%s\n' "$PRODUCT_MAIN" "$PROOF_BASE" >&2; exit 24; }
[ "$PROOF_HEAD_BRANCH" = "$PROOF_BRANCH" ] || { printf 'Proof branch moved. expected=%s observed=%s\n' "$PROOF_BRANCH" "$PROOF_HEAD_BRANCH" >&2; exit 25; }
PROOF_TREE="$("$GH_BIN" api "repos/$PRODUCT_REPOSITORY/git/commits/$PROOF_HEAD" --jq '.tree.sha')"
CHANGED_PATHS="$("$GH_BIN" api "repos/$PRODUCT_REPOSITORY/compare/$PRODUCT_MAIN...$PROOF_HEAD" --jq '.files[].filename' | LC_ALL=C /usr/bin/sort)"
EXPECTED_PATHS="$(printf '%s\n%s\n%s\n%s\n' "$BOOTSTRAP_PATH" "$FORM_PATH" "$PAYLOAD_PATH" "$RUNNER_PATH" | LC_ALL=C /usr/bin/sort)"
[ "$CHANGED_PATHS" = "$EXPECTED_PATHS" ] || { printf 'Proof path membrane mismatch.\nObserved:\n%s\nExpected:\n%s\n' "$CHANGED_PATHS" "$EXPECTED_PATHS" >&2; exit 26; }

OBS_SDK_MAIN="$("$GH_BIN" api 'repos/vgong24/Vextreme-SDK/commits/main' --jq '.sha')"
[ "$OBS_SDK_MAIN" = "$SDK_MAIN" ] || { printf 'SDK main moved. expected=%s observed=%s\n' "$SDK_MAIN" "$OBS_SDK_MAIN" >&2; exit 27; }
OBS_SDK_TREE="$("$GH_BIN" api "repos/vgong24/Vextreme-SDK/git/commits/$SDK_MAIN" --jq '.tree.sha')"
[ "$OBS_SDK_TREE" = "$SDK_TREE" ] || { printf 'SDK main tree moved. expected=%s observed=%s\n' "$SDK_TREE" "$OBS_SDK_TREE" >&2; exit 28; }
OBS_CARRIER_STATE="$("$GH_BIN" api "repos/vgong24/Vextreme-SDK/pulls/$CARRIER_PR" --jq '.state')"
OBS_CARRIER_HEAD="$("$GH_BIN" api "repos/vgong24/Vextreme-SDK/pulls/$CARRIER_PR" --jq '.head.sha')"
[ "$OBS_CARRIER_STATE" = 'open' ] || { printf 'Carrier PR #%s is not open. state=%s\n' "$CARRIER_PR" "$OBS_CARRIER_STATE" >&2; exit 29; }
[ "$OBS_CARRIER_HEAD" = "$CARRIER_HEAD" ] || { printf 'Carrier head moved. expected=%s observed=%s\n' "$CARRIER_HEAD" "$OBS_CARRIER_HEAD" >&2; exit 30; }

ROOT="$("$MKTEMP_BIN" -d "$DOWNLOADS/.vex-evolve-context-lived-a003.XXXXXX")"
cleanup() { case "$ROOT" in "$DOWNLOADS"/.vex-evolve-context-lived-a003.*) "$RM_BIN" -rf -- "$ROOT" ;; *) return 1 ;; esac; }
trap cleanup EXIT HUP INT TERM

PROOF_ROOT="$ROOT/VexLife-Prod"
SDK_ROOT="$ROOT/Vextreme-SDK"
"$GH_BIN" repo clone "$PRODUCT_REPOSITORY" "$PROOF_ROOT" -- --no-checkout >/dev/null
"$GIT_BIN" -C "$PROOF_ROOT" checkout --detach "$PROOF_HEAD" >/dev/null
[ "$("$GIT_BIN" -C "$PROOF_ROOT" rev-parse HEAD)" = "$PROOF_HEAD" ] || { printf '%s\n' 'Proof checkout head mismatch.' >&2; exit 31; }
[ "$("$GIT_BIN" -C "$PROOF_ROOT" rev-parse 'HEAD^{tree}')" = "$PROOF_TREE" ] || { printf '%s\n' 'Proof checkout tree mismatch.' >&2; exit 32; }
[ -z "$("$GIT_BIN" -C "$PROOF_ROOT" status --porcelain=v1 -uall)" ] || { printf '%s\n' 'Proof checkout is dirty.' >&2; exit 33; }
RUNNER_BLOB="$("$GIT_BIN" -C "$PROOF_ROOT" rev-parse "$PROOF_HEAD:$RUNNER_PATH")"
PAYLOAD_BLOB="$("$GIT_BIN" -C "$PROOF_ROOT" rev-parse "$PROOF_HEAD:$PAYLOAD_PATH")"

"$GH_BIN" repo clone 'vgong24/Vextreme-SDK' "$SDK_ROOT" -- --no-checkout >/dev/null
"$GIT_BIN" -C "$SDK_ROOT" checkout --detach "$CARRIER_HEAD" >/dev/null
[ "$("$GIT_BIN" -C "$SDK_ROOT" rev-parse HEAD)" = "$CARRIER_HEAD" ] || { printf '%s\n' 'Carrier checkout head mismatch.' >&2; exit 34; }
[ "$("$GIT_BIN" -C "$SDK_ROOT" rev-parse 'HEAD^{tree}')" = "$CARRIER_TREE" ] || { printf '%s\n' 'Carrier checkout tree mismatch.' >&2; exit 35; }
[ -z "$("$GIT_BIN" -C "$SDK_ROOT" status --porcelain=v1 -uall)" ] || { printf '%s\n' 'Carrier checkout is dirty.' >&2; exit 36; }

printf '%s\n' 'EvolveContext lived A003: executing the source-controlled baseline → treatment → standing proof...'
VEX_SDK_ROOT="$SDK_ROOT" \
VEX_PROOF_ROOT="$PROOF_ROOT" \
VEX_DOWNLOADS="$DOWNLOADS" \
VEX_PRODUCT_BASE="$PRODUCT_MAIN" \
VEX_PROOF_HEAD="$PROOF_HEAD" \
VEX_PROOF_TREE="$PROOF_TREE" \
VEX_PROOF_BRANCH="$PROOF_BRANCH" \
VEX_RUNNER_BLOB="$RUNNER_BLOB" \
VEX_PAYLOAD_BLOB="$PAYLOAD_BLOB" \
"$NODE_BIN" "$PROOF_ROOT/$FORM_PATH"
