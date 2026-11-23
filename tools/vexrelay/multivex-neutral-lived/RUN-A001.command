#!/bin/bash
set -euo pipefail

SDK_REPOSITORY='vgong24/Vextreme-SDK'
SDK_HEAD='149455eeec4c7b7ca00e91022154a85faa3d608c'
SDK_TREE='53ea701f9bbf24a5cf37d43e4dbe3836187f4011'

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

BASH_BIN="$(resolve_exec /bin/bash)" || { printf '%s\n' '[Multi-Vex neutral] Bash unavailable.' >&2; exit 10; }
GIT_BIN="$(resolve_exec /usr/bin/git)" || { printf '%s\n' '[Multi-Vex neutral] Git unavailable.' >&2; exit 11; }
GH_BIN="$(resolve_exec /opt/homebrew/bin/gh /usr/local/bin/gh)" || { printf '%s\n' '[Multi-Vex neutral] GitHub CLI unavailable.' >&2; exit 12; }
NODE_BIN="$(resolve_exec /opt/homebrew/bin/node /usr/local/bin/node /usr/bin/node)" || { printf '%s\n' '[Multi-Vex neutral] Node unavailable.' >&2; exit 13; }
SHASUM_BIN="$(resolve_exec /usr/bin/shasum)" || { printf '%s\n' '[Multi-Vex neutral] shasum unavailable.' >&2; exit 14; }
MKTEMP_BIN="$(resolve_exec /usr/bin/mktemp)" || { printf '%s\n' '[Multi-Vex neutral] mktemp unavailable.' >&2; exit 15; }
RM_BIN="$(resolve_exec /bin/rm /usr/bin/rm)" || { printf '%s\n' '[Multi-Vex neutral] rm unavailable.' >&2; exit 16; }
CAT_BIN="$(resolve_exec /bin/cat /usr/bin/cat)" || { printf '%s\n' '[Multi-Vex neutral] cat unavailable.' >&2; exit 17; }
TAIL_BIN="$(resolve_exec /usr/bin/tail)" || { printf '%s\n' '[Multi-Vex neutral] tail unavailable.' >&2; exit 18; }
FIND_BIN="$(resolve_exec /usr/bin/find)" || { printf '%s\n' '[Multi-Vex neutral] find unavailable.' >&2; exit 19; }
SORT_BIN="$(resolve_exec /usr/bin/sort)" || { printf '%s\n' '[Multi-Vex neutral] sort unavailable.' >&2; exit 20; }

"$GH_BIN" auth status --hostname github.com >/dev/null 2>&1 || {
  printf '%s\n' '[Multi-Vex neutral] GitHub authentication unavailable.' >&2
  exit 21
}
[ -d "$DOWNLOADS" ] || { printf '%s\n' '[Multi-Vex neutral] ~/Downloads unavailable.' >&2; exit 22; }

WORK_ROOT="$("$MKTEMP_BIN" -d "$DOWNLOADS/.vexlife-multivex-neutral-lived-a001.XXXXXX")"
WATCHER_PID=''
cleanup() {
  if [ -n "$WATCHER_PID" ]; then
    /bin/kill "$WATCHER_PID" >/dev/null 2>&1 || true
    wait "$WATCHER_PID" >/dev/null 2>&1 || true
  fi
  case "$WORK_ROOT" in
    "$DOWNLOADS"/.vexlife-multivex-neutral-lived-a001.*)
      "$RM_BIN" -rf -- "$WORK_ROOT"
      ;;
  esac
}
trap cleanup EXIT HUP INT TERM

SDK_ROOT="$WORK_ROOT/Vextreme-SDK"
FORMATION_OUTPUT="$WORK_ROOT/formation-output.txt"

printf '%s\n' '[Multi-Vex neutral 1/4] Acquiring exact VexLocalBridge host carrier...'
"$GH_BIN" repo clone "$SDK_REPOSITORY" "$SDK_ROOT" -- --filter=blob:none --no-checkout >/dev/null
"$GIT_BIN" -C "$SDK_ROOT" checkout --quiet --detach "$SDK_HEAD"
[ "$("$GIT_BIN" -C "$SDK_ROOT" rev-parse HEAD)" = "$SDK_HEAD" ] || { printf '%s\n' '[Multi-Vex neutral] SDK head mismatch.' >&2; exit 23; }
[ "$("$GIT_BIN" -C "$SDK_ROOT" rev-parse 'HEAD^{tree}')" = "$SDK_TREE" ] || { printf '%s\n' '[Multi-Vex neutral] SDK tree mismatch.' >&2; exit 24; }
[ -z "$("$GIT_BIN" -C "$SDK_ROOT" status --porcelain=v1 -uall)" ] || { printf '%s\n' '[Multi-Vex neutral] SDK checkout dirty.' >&2; exit 25; }

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

printf '%s\n' '[Multi-Vex neutral 2/4] Static-checking simplified lived carrier...'
"$NODE_BIN" --check "$ROOT/formation-A001.mjs"
"$NODE_BIN" --check "$ROOT/payload-A001.mjs"

printf '%s\n' '[Multi-Vex neutral 3/4] Forming fresh digest-bound lived task...'
"$NODE_BIN"   "$ROOT/formation-A001.mjs"   "$SDK_ROOT"   "$DOWNLOADS"   "$ROOT/payload-A001.mjs"   > "$FORMATION_OUTPUT"
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
[ -f "$PACKAGE_PATH" ] && [ ! -L "$PACKAGE_PATH" ] || { printf '%s\n' '[Multi-Vex neutral] formed package missing.' >&2; exit 40; }
[ "$("$SHASUM_BIN" -a 256 "$PACKAGE_PATH" | /usr/bin/cut -d ' ' -f 1)" = "$PACKAGE_SHA" ] || { printf '%s\n' '[Multi-Vex neutral] formed package digest mismatch.' >&2; exit 41; }

CAPSULE_ROOT="$DOWNLOADS/VLB-${PACKAGE_SHA:0:16}"
PROGRESS_FILE="$CAPSULE_ROOT/bridge/progress/multivex-neutral-lived.log"
printf 'Multi-Vex neutral capsule: %s\n' "$CAPSULE_ROOT"
printf 'Multi-Vex neutral live stages: %s\n' "$PROGRESS_FILE"

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
[ -n "$BOOTSTRAP_COMMAND" ] || { printf '%s\n' '[Multi-Vex neutral] Product bootstrap was empty.' >&2; exit 42; }

printf '%s\n' '[Multi-Vex neutral 4/4] Running simplified adaptive Sarah ↔ Vex lived encounter.'
printf '%s\n' 'Role OFF. Full Character Posture OFF. Neutral baseline = scenario ground + participant truth/privacy boundaries + output contract.'
printf '%s\n' 'Observer lens remains absent; strict lived participant-state contract remains active.'
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

printf '\nMulti-Vex neutral VexLocalBridge exit=%s\n' "$RUN_STATUS"
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
