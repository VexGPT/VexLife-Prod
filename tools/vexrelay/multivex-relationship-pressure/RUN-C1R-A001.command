#!/bin/bash
set -euo pipefail

SDK_REPOSITORY='vgong24/Vextreme-SDK'
SDK_HEAD='149455eeec4c7b7ca00e91022154a85faa3d608c'
SDK_TREE='53ea701f9bbf24a5cf37d43e4dbe3836187f4011'
PRODUCT_REPOSITORY='VexGPT/VexLife-Prod'
F20_BODY_BLOB='4af59951c177bddaec058253a967f448e8a5c947'
F20_REVISIONS_BLOB='a7a497900bacc5ded736ab12f58e0c6e876a39c8'

ROOT="$(cd "$(dirname "$0")" && pwd -P)"
DOWNLOADS="$HOME/Downloads"

resolve_exec() {
  local candidate
  for candidate in "$@"; do
    if [ -x "$candidate" ] && [ -f "$candidate" ]; then
      printf '%s' "$candidate"
      return 0
    fi
  done
  return 1
}

BASH_BIN="$(resolve_exec /bin/bash)" || { printf '%s\n' '[Multi-Vex C1R] Bash unavailable.' >&2; exit 10; }
GIT_BIN="$(resolve_exec /usr/bin/git)" || { printf '%s\n' '[Multi-Vex C1R] Git unavailable.' >&2; exit 11; }
GH_BIN="$(resolve_exec /opt/homebrew/bin/gh /usr/local/bin/gh)" || { printf '%s\n' '[Multi-Vex C1R] GitHub CLI unavailable.' >&2; exit 12; }
NODE_BIN="$(resolve_exec /opt/homebrew/bin/node /usr/local/bin/node /usr/bin/node)" || { printf '%s\n' '[Multi-Vex C1R] Node unavailable.' >&2; exit 13; }
SHASUM_BIN="$(resolve_exec /usr/bin/shasum)" || { printf '%s\n' '[Multi-Vex C1R] shasum unavailable.' >&2; exit 14; }
MKTEMP_BIN="$(resolve_exec /usr/bin/mktemp)" || { printf '%s\n' '[Multi-Vex C1R] mktemp unavailable.' >&2; exit 15; }
RM_BIN="$(resolve_exec /bin/rm /usr/bin/rm)" || { printf '%s\n' '[Multi-Vex C1R] rm unavailable.' >&2; exit 16; }
CAT_BIN="$(resolve_exec /bin/cat /usr/bin/cat)" || { printf '%s\n' '[Multi-Vex C1R] cat unavailable.' >&2; exit 17; }
TAIL_BIN="$(resolve_exec /usr/bin/tail)" || { printf '%s\n' '[Multi-Vex C1R] tail unavailable.' >&2; exit 18; }
FIND_BIN="$(resolve_exec /usr/bin/find)" || { printf '%s\n' '[Multi-Vex C1R] find unavailable.' >&2; exit 19; }
SORT_BIN="$(resolve_exec /usr/bin/sort)" || { printf '%s\n' '[Multi-Vex C1R] sort unavailable.' >&2; exit 20; }
BASE64_BIN="$(resolve_exec /usr/bin/base64)" || { printf '%s\n' '[Multi-Vex C1R] base64 unavailable.' >&2; exit 21; }

"$GH_BIN" auth status --hostname github.com >/dev/null 2>&1 || {
  printf '%s\n' '[Multi-Vex C1R] GitHub authentication unavailable.' >&2
  exit 22
}
[ -d "$DOWNLOADS" ] || { printf '%s\n' '[Multi-Vex C1R] ~/Downloads unavailable.' >&2; exit 23; }

WORK_ROOT="$("$MKTEMP_BIN" -d "$DOWNLOADS/.vexlife-multivex-relationship-pressure-c1r-a001.XXXXXX")"
WATCHER_PID=''
cleanup() {
  if [ -n "$WATCHER_PID" ]; then
    /bin/kill "$WATCHER_PID" >/dev/null 2>&1 || true
    wait "$WATCHER_PID" >/dev/null 2>&1 || true
  fi
  case "$WORK_ROOT" in
    "$DOWNLOADS"/.vexlife-multivex-relationship-pressure-c1r-a001.*)
      "$RM_BIN" -rf -- "$WORK_ROOT"
      ;;
  esac
}
trap cleanup EXIT HUP INT TERM

SDK_ROOT="$WORK_ROOT/Vextreme-SDK"
FORMATION_OUTPUT="$WORK_ROOT/formation-output.txt"
F20_BODY="$WORK_ROOT/foundation-v0.6.20-body.json"
F20_REVISIONS="$WORK_ROOT/foundation-v0.6.20-revisions.json"

printf '%s\n' '[Multi-Vex 1/5] Acquiring exact VexLocalBridge host carrier...'
"$GH_BIN" repo clone "$SDK_REPOSITORY" "$SDK_ROOT" -- --filter=blob:none --no-checkout >/dev/null
"$GIT_BIN" -C "$SDK_ROOT" checkout --quiet --detach "$SDK_HEAD"
[ "$("$GIT_BIN" -C "$SDK_ROOT" rev-parse HEAD)" = "$SDK_HEAD" ] || { printf '%s\n' '[Multi-Vex C1R] SDK head mismatch.' >&2; exit 24; }
[ "$("$GIT_BIN" -C "$SDK_ROOT" rev-parse 'HEAD^{tree}')" = "$SDK_TREE" ] || { printf '%s\n' '[Multi-Vex C1R] SDK tree mismatch.' >&2; exit 25; }
[ -z "$("$GIT_BIN" -C "$SDK_ROOT" status --porcelain=v1 -uall)" ] || { printf '%s\n' '[Multi-Vex C1R] SDK checkout dirty.' >&2; exit 26; }

"$NODE_BIN" - "$SDK_ROOT" <<'NODE'
const fs=require('node:fs');
const path=require('node:path');
const root=process.argv[2];
const profile=JSON.parse(fs.readFileSync(path.join(root,'docs/private-continuity/vexlocalbridge/profiles/macos-victor-vexlife-prod.json'),'utf8'));
const policy=require(path.join(root,'lib/vexlocalbridge/vexlife-prod-host-effect-policy.js'));
policy.validateProfileBinding(profile);
if(profile.profileRef!=='profile.vexlocalbridge.macos-victor-vexlife-prod.v1') process.exit(31);
if(profile.repository!=='VexGPT/VexLife-Prod') process.exit(32);
if(profile.repositoryExecutionProfileRef!=='profile.repository.vexlife-prod.macos-v1') process.exit(33);
if(policy.REQUIRED_EFFECT_REF!=='effect.vexlocalbridge.vexlife-prod.product-lifecycle-proof/v1') process.exit(34);
if(profile.vexLifeProdHostEffectPolicy.productDataPolicy.modelAcquisitionAllowed!==false) process.exit(35);
if(profile.vexLifeProdHostEffectPolicy.productDataPolicy.modelSwitchForProofAllowed!==false) process.exit(36);
NODE

printf '%s\n' '[Multi-Vex 2/5] Acquiring exact Foundation v0.6.20 treatment bytes from #549...'
"$GH_BIN" api "repos/$PRODUCT_REPOSITORY/git/blobs/$F20_BODY_BLOB" --jq .content | "$BASE64_BIN" -D > "$F20_BODY"
"$GH_BIN" api "repos/$PRODUCT_REPOSITORY/git/blobs/$F20_REVISIONS_BLOB" --jq .content | "$BASE64_BIN" -D > "$F20_REVISIONS"
[ "$("$GIT_BIN" hash-object "$F20_BODY")" = "$F20_BODY_BLOB" ] || { printf '%s\n' '[Multi-Vex C1R] Foundation v0.6.20 body blob mismatch.' >&2; exit 37; }
[ "$("$GIT_BIN" hash-object "$F20_REVISIONS")" = "$F20_REVISIONS_BLOB" ] || { printf '%s\n' '[Multi-Vex C1R] Foundation v0.6.20 revisions blob mismatch.' >&2; exit 38; }

printf '%s\n' '[Multi-Vex 3/5] Static-checking C1 affinity/material-access relationship-pressure carrier...'
"$NODE_BIN" --check "$ROOT/formation-C1R-A001.mjs"
"$NODE_BIN" --check "$ROOT/payload-C1R-A001.mjs"

printf '%s\n' '[Multi-Vex 4/5] Forming fresh digest-bound affinity/material-access pressure task...'
"$NODE_BIN" \
  "$ROOT/formation-C1R-A001.mjs" \
  "$SDK_ROOT" \
  "$DOWNLOADS" \
  "$ROOT/payload-C1R-A001.mjs" \
  "$F20_BODY" \
  "$F20_REVISIONS" \
  > "$FORMATION_OUTPUT"
"$CAT_BIN" "$FORMATION_OUTPUT"

FORMATION_JSON="$("$NODE_BIN" - "$FORMATION_OUTPUT" <<'NODE'
const fs=require('node:fs');
const lines=fs.readFileSync(process.argv[2],'utf8').split(/\r?\n/).map(s=>s.trim()).filter(Boolean);
for(let i=lines.length-1;i>=0;i--){
  try{
    const x=JSON.parse(lines[i]);
    if(x&&x.disposition==='PASS'&&x.packagePath&&x.packageSha256){
      process.stdout.write(JSON.stringify(x));
      process.exit(0);
    }
  }catch{}
}
process.exit(39);
NODE
)"
PACKAGE_PATH="$("$NODE_BIN" -e 'process.stdout.write(JSON.parse(process.argv[1]).packagePath)' "$FORMATION_JSON")"
PACKAGE_SHA="$("$NODE_BIN" -e 'process.stdout.write(JSON.parse(process.argv[1]).packageSha256)' "$FORMATION_JSON")"
[ -f "$PACKAGE_PATH" ] && [ ! -L "$PACKAGE_PATH" ] || { printf '%s\n' '[Multi-Vex C1R] formed package missing.' >&2; exit 40; }
[ "$("$SHASUM_BIN" -a 256 "$PACKAGE_PATH" | /usr/bin/cut -d ' ' -f 1)" = "$PACKAGE_SHA" ] || { printf '%s\n' '[Multi-Vex C1R] formed package digest mismatch.' >&2; exit 41; }

CAPSULE_ROOT="$DOWNLOADS/VLB-${PACKAGE_SHA:0:16}"
PROGRESS_FILE="$CAPSULE_ROOT/bridge/progress/multivex-relationship-pressure-c0.log"
printf 'Multi-Vex C1R correction capsule: %s\n' "$CAPSULE_ROOT"
printf 'Multi-Vex C1R correction live stages: %s\n' "$PROGRESS_FILE"

(
  while [ ! -f "$PROGRESS_FILE" ]; do
    /bin/sleep 0.25
  done
  "$TAIL_BIN" -n +1 -F "$PROGRESS_FILE"
) &
WATCHER_PID="$!"

BOOTSTRAP_COMMAND="$("$NODE_BIN" - "$SDK_ROOT/lib/vexlocalbridge/mac-vexlife-prod-source-bootstrap-command.js" "$PACKAGE_SHA" "$SDK_HEAD" "$SDK_TREE" <<'NODE'
const [modulePath,packageSha256,sdkHead,sdkTree]=process.argv.slice(2);
const m=require(modulePath);
process.stdout.write(m.renderMacVexLifeProdSourceBootstrapCommand({packageSha256,sdkHead,sdkTree}));
NODE
)"
[ -n "$BOOTSTRAP_COMMAND" ] || { printf '%s\n' '[Multi-Vex C1R] Product bootstrap was empty.' >&2; exit 42; }

printf '%s\n' '[Multi-Vex 5/5] Running C1 affinity/material-access pressure under exact Foundation v0.6.20.'
printf '%s\n' 'One Mira encounter runs with affinity-material-access Role ON, Character Posture OFF, observer lens absent. Blue Lantern facts and the seven-word request remain unchanged from C0.'
printf '%s\n' 'VexLocalBridge stays attached to this terminal.'
printf '%s\n' 'If needed, type CANCEL then Enter. Do not use Ctrl+C or Activity Monitor kill.'
set +e
"$BASH_BIN" -c "$BOOTSTRAP_COMMAND"
RUN_STATUS="$?"
set -e

if [ -n "$WATCHER_PID" ]; then
  /bin/kill "$WATCHER_PID" >/dev/null 2>&1 || true
  wait "$WATCHER_PID" >/dev/null 2>&1 || true
  WATCHER_PID=''
fi

printf '\nMulti-Vex relationship-pressure C1 VexLocalBridge exit=%s\n' "$RUN_STATUS"
printf 'Capsule retained for Operations consumption: %s\n' "$CAPSULE_ROOT"
printf '%s\n' 'Canonical RETURN candidates:'
RETURN_COUNT=0
while IFS= read -r result; do
  [ -n "$result" ] || continue
  RETURN_COUNT=$((RETURN_COUNT+1))
  printf '  %s\n' "$result"
  printf '  SHA-256: %s\n' "$("$SHASUM_BIN" -a 256 "$result" | /usr/bin/cut -d ' ' -f 1)"
done < <("$FIND_BIN" "$CAPSULE_ROOT/results" -type f -name 'RETURN-*.zip' -print 2>/dev/null | "$SORT_BIN")

if [ "$RETURN_COUNT" -eq 0 ]; then
  printf '%s\n' 'NO_CANONICAL_RETURN_FOUND — preserve this capsule; do not replay.' >&2
fi
exit "$RUN_STATUS"
