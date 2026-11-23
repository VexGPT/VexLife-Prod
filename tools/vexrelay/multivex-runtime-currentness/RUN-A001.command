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

BASH_BIN="$(resolve_exec /bin/bash)" || { echo '[Multi-Vex runtime] Bash unavailable.' >&2; exit 10; }
GIT_BIN="$(resolve_exec /usr/bin/git)" || { echo '[Multi-Vex runtime] Git unavailable.' >&2; exit 11; }
GH_BIN="$(resolve_exec /opt/homebrew/bin/gh /usr/local/bin/gh)" || { echo '[Multi-Vex runtime] GitHub CLI unavailable.' >&2; exit 12; }
NODE_BIN="$(resolve_exec /opt/homebrew/bin/node /usr/local/bin/node /usr/bin/node)" || { echo '[Multi-Vex runtime] Node unavailable.' >&2; exit 13; }
SHASUM_BIN="$(resolve_exec /usr/bin/shasum)" || { echo '[Multi-Vex runtime] shasum unavailable.' >&2; exit 14; }
MKTEMP_BIN="$(resolve_exec /usr/bin/mktemp)" || { echo '[Multi-Vex runtime] mktemp unavailable.' >&2; exit 15; }
RM_BIN="$(resolve_exec /bin/rm /usr/bin/rm)" || { echo '[Multi-Vex runtime] rm unavailable.' >&2; exit 16; }
CAT_BIN="$(resolve_exec /bin/cat /usr/bin/cat)" || { echo '[Multi-Vex runtime] cat unavailable.' >&2; exit 17; }
TAIL_BIN="$(resolve_exec /usr/bin/tail)" || { echo '[Multi-Vex runtime] tail unavailable.' >&2; exit 18; }
FIND_BIN="$(resolve_exec /usr/bin/find)" || { echo '[Multi-Vex runtime] find unavailable.' >&2; exit 19; }
SORT_BIN="$(resolve_exec /usr/bin/sort)" || { echo '[Multi-Vex runtime] sort unavailable.' >&2; exit 20; }

"$GH_BIN" auth status --hostname github.com >/dev/null 2>&1 || {
  echo '[Multi-Vex runtime] GitHub authentication unavailable.' >&2
  exit 21
}
[ -d "$DOWNLOADS" ] || { echo '[Multi-Vex runtime] ~/Downloads unavailable.' >&2; exit 22; }

WORK_ROOT="$("$MKTEMP_BIN" -d "$DOWNLOADS/.vexlife-multivex-runtime-currentness-a001.XXXXXX")"
WATCHER_PID=''
cleanup() {
  if [ -n "$WATCHER_PID" ]; then
    /bin/kill "$WATCHER_PID" >/dev/null 2>&1 || true
    wait "$WATCHER_PID" >/dev/null 2>&1 || true
  fi
  case "$WORK_ROOT" in
    "$DOWNLOADS"/.vexlife-multivex-runtime-currentness-a001.*)
      "$RM_BIN" -rf -- "$WORK_ROOT"
      ;;
  esac
}
trap cleanup EXIT HUP INT TERM

SDK_ROOT="$WORK_ROOT/Vextreme-SDK"
FORMATION_OUTPUT="$WORK_ROOT/formation-output.txt"

echo '[Multi-Vex runtime 1/4] Acquiring exact VexLocalBridge carrier...'
"$GH_BIN" repo clone "$SDK_REPOSITORY" "$SDK_ROOT" -- --filter=blob:none --no-checkout >/dev/null
"$GIT_BIN" -C "$SDK_ROOT" checkout --quiet --detach "$SDK_HEAD"
[ "$("$GIT_BIN" -C "$SDK_ROOT" rev-parse HEAD)" = "$SDK_HEAD" ] || { echo 'SDK head mismatch.' >&2; exit 23; }
[ "$("$GIT_BIN" -C "$SDK_ROOT" rev-parse 'HEAD^{tree}')" = "$SDK_TREE" ] || { echo 'SDK tree mismatch.' >&2; exit 24; }
[ -z "$("$GIT_BIN" -C "$SDK_ROOT" status --porcelain=v1 -uall)" ] || { echo 'SDK checkout dirty.' >&2; exit 25; }

echo '[Multi-Vex runtime 2/4] Static-checking read-only diagnostic...'
"$NODE_BIN" --check "$ROOT/formation-A001.mjs"
"$NODE_BIN" --check "$ROOT/payload-A001.mjs"

echo '[Multi-Vex runtime 3/4] Forming digest-bound runtime-currentness task...'
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
[ -f "$PACKAGE_PATH" ] && [ ! -L "$PACKAGE_PATH" ] || { echo 'formed package missing.' >&2; exit 40; }
[ "$("$SHASUM_BIN" -a 256 "$PACKAGE_PATH" | /usr/bin/cut -d ' ' -f 1)" = "$PACKAGE_SHA" ] || { echo 'formed package digest mismatch.' >&2; exit 41; }

CAPSULE_ROOT="$DOWNLOADS/VLB-${PACKAGE_SHA:0:16}"
PROGRESS_FILE="$CAPSULE_ROOT/bridge/progress/multivex-runtime-currentness.log"
printf 'Multi-Vex runtime capsule: %s\n' "$CAPSULE_ROOT"

(
  while [ ! -f "$PROGRESS_FILE" ]; do /bin/sleep 0.25; done
  "$TAIL_BIN" -n +1 -F "$PROGRESS_FILE"
) &
WATCHER_PID="$!"

BOOTSTRAP_COMMAND="$("$NODE_BIN" - "$SDK_ROOT/lib/vexlocalbridge/mac-vexlife-prod-source-bootstrap-command.js" "$PACKAGE_SHA" "$SDK_HEAD" "$SDK_TREE" <<'NODE'
const [modulePath,packageSha256,sdkHead,sdkTree]=process.argv.slice(2);
const m=require(modulePath);
process.stdout.write(m.renderMacVexLifeProdSourceBootstrapCommand({packageSha256,sdkHead,sdkTree}));
NODE
)"
[ -n "$BOOTSTRAP_COMMAND" ] || { echo 'bootstrap was empty.' >&2; exit 42; }

echo '[Multi-Vex runtime 4/4] Observing Product runtime currentness only. No repair/restart.'
set +e
"$BASH_BIN" -c "$BOOTSTRAP_COMMAND"
RUN_STATUS="$?"
set -e

if [ -n "$WATCHER_PID" ]; then
  /bin/kill "$WATCHER_PID" >/dev/null 2>&1 || true
  wait "$WATCHER_PID" >/dev/null 2>&1 || true
  WATCHER_PID=''
fi

printf '\nMulti-Vex runtime-currentness VexLocalBridge exit=%s\n' "$RUN_STATUS"
printf 'Capsule retained: %s\n' "$CAPSULE_ROOT"
echo 'Canonical RETURN candidates:'
RETURN_COUNT=0
while IFS= read -r result; do
  [ -n "$result" ] || continue
  RETURN_COUNT=$((RETURN_COUNT+1))
  printf '  %s\n' "$result"
  printf '  SHA-256: %s\n' "$("$SHASUM_BIN" -a 256 "$result" | /usr/bin/cut -d ' ' -f 1)"
done < <("$FIND_BIN" "$CAPSULE_ROOT/results" -type f -name 'RETURN-*.zip' -print 2>/dev/null | "$SORT_BIN")

if [ "$RETURN_COUNT" -eq 0 ]; then
  echo 'NO_CANONICAL_RETURN_FOUND — preserve capsule; do not replay.' >&2
fi
exit "$RUN_STATUS"
