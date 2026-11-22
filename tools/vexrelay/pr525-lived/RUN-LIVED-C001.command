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

BASH_BIN="$(resolve_exec /bin/bash)" || { printf '%s\n' 'PR525 lived: Bash unavailable.' >&2; exit 10; }
GIT_BIN="$(resolve_exec /usr/bin/git)" || { printf '%s\n' 'PR525 lived: Git unavailable.' >&2; exit 11; }
GH_BIN="$(resolve_exec /opt/homebrew/bin/gh /usr/local/bin/gh)" || { printf '%s\n' 'PR525 lived: GitHub CLI unavailable.' >&2; exit 12; }
NODE_BIN="$(resolve_exec /opt/homebrew/bin/node /usr/local/bin/node /usr/bin/node)" || { printf '%s\n' 'PR525 lived: Node unavailable.' >&2; exit 13; }
SHASUM_BIN="$(resolve_exec /usr/bin/shasum)" || { printf '%s\n' 'PR525 lived: shasum unavailable.' >&2; exit 14; }
MKTEMP_BIN="$(resolve_exec /usr/bin/mktemp)" || { printf '%s\n' 'PR525 lived: mktemp unavailable.' >&2; exit 15; }
RM_BIN="$(resolve_exec /bin/rm /usr/bin/rm)" || { printf '%s\n' 'PR525 lived: rm unavailable.' >&2; exit 16; }
TEE_BIN="$(resolve_exec /usr/bin/tee)" || { printf '%s\n' 'PR525 lived: tee unavailable.' >&2; exit 17; }

"$GH_BIN" auth status --hostname github.com >/dev/null 2>&1 || {
  printf '%s\n' 'PR525 lived: GitHub authentication unavailable.' >&2
  exit 20
}
[ -d "$DOWNLOADS" ] || { printf '%s\n' 'PR525 lived: ~/Downloads unavailable.' >&2; exit 21; }

WORK_ROOT="$("$MKTEMP_BIN" -d "$DOWNLOADS/.vexlife-pr525-composed-warm-boot-lived-c001.XXXXXX")"
case "$WORK_ROOT" in
  "$DOWNLOADS"/.vexlife-pr525-composed-warm-boot-lived-c001.*) ;;
  *) printf '%s\n' 'PR525 lived: temporary root escaped Downloads.' >&2; exit 22 ;;
esac
cleanup() {
  case "$WORK_ROOT" in
    "$DOWNLOADS"/.vexlife-pr525-composed-warm-boot-lived-c001.*)
      "$RM_BIN" -rf -- "$WORK_ROOT"
      ;;
  esac
}
trap cleanup EXIT HUP INT TERM

SDK_ROOT="$WORK_ROOT/Vextreme-SDK"
FORMATION_OUTPUT="$WORK_ROOT/formation-output.txt"
EXECUTION_OUTPUT="$WORK_ROOT/execution-output.txt"

printf '%s\n' '[1/4] Acquiring exact accepted Product host carrier...'
"$GH_BIN" repo clone "$SDK_REPOSITORY" "$SDK_ROOT" -- --filter=blob:none --no-checkout >/dev/null
"$GIT_BIN" -C "$SDK_ROOT" checkout --quiet --detach "$SDK_HEAD"
[ "$("$GIT_BIN" -C "$SDK_ROOT" rev-parse HEAD)" = "$SDK_HEAD" ] || { printf '%s\n' 'SDK carrier head mismatch.' >&2; exit 23; }
[ "$("$GIT_BIN" -C "$SDK_ROOT" rev-parse 'HEAD^{tree}')" = "$SDK_TREE" ] || { printf '%s\n' 'SDK carrier tree mismatch.' >&2; exit 24; }
[ -z "$("$GIT_BIN" -C "$SDK_ROOT" status --porcelain=v1 -uall)" ] || { printf '%s\n' 'SDK carrier checkout dirty.' >&2; exit 25; }

"$NODE_BIN" - "$SDK_ROOT" <<'NODE'
const fs=require('node:fs'); const path=require('node:path'); const root=process.argv[2];
const profile=JSON.parse(fs.readFileSync(path.join(root,'docs/private-continuity/vexlocalbridge/profiles/macos-victor-vexlife-prod.json'),'utf8'));
const policy=require(path.join(root,'lib/vexlocalbridge/vexlife-prod-host-effect-policy.js'));
policy.validateProfileBinding(profile);
if(profile.profileRef!=='profile.vexlocalbridge.macos-victor-vexlife-prod.v1') process.exit(31);
if(profile.repository!=='VexGPT/VexLife-Prod') process.exit(32);
if(profile.repositoryExecutionProfileRef!=='profile.repository.vexlife-prod.macos-v1') process.exit(33);
if(policy.REQUIRED_EFFECT_REF!=='effect.vexlocalbridge.vexlife-prod.product-lifecycle-proof/v1') process.exit(34);
NODE

printf '%s\n' '[2/4] Static-checking frozen formation and payload...'
"$NODE_BIN" --check "$ROOT/formation-C001.mjs"
"$NODE_BIN" --check "$ROOT/payload-C001.mjs"

printf '%s\n' '[3/4] Forming fresh digest-bound lived Product task...'
"$NODE_BIN" "$ROOT/formation-C001.mjs" "$SDK_ROOT" "$DOWNLOADS" "$ROOT/payload-C001.mjs" | "$TEE_BIN" "$FORMATION_OUTPUT"

FORMATION_JSON="$("$NODE_BIN" - "$FORMATION_OUTPUT" <<'NODE'
const fs=require('node:fs');
const lines=fs.readFileSync(process.argv[2],'utf8').split(/\r?\n/).map(s=>s.trim()).filter(Boolean);
for(let i=lines.length-1;i>=0;i--){
  try{
    const x=JSON.parse(lines[i]);
    if(x&&x.disposition==='PASS'&&x.packagePath&&x.packageSha256){
      process.stdout.write(JSON.stringify(x)); process.exit(0);
    }
  }catch{}
}
process.exit(35);
NODE
)"
PACKAGE_PATH="$("$NODE_BIN" -e 'process.stdout.write(JSON.parse(process.argv[1]).packagePath)' "$FORMATION_JSON")"
PACKAGE_SHA="$("$NODE_BIN" -e 'process.stdout.write(JSON.parse(process.argv[1]).packageSha256)' "$FORMATION_JSON")"
[ -f "$PACKAGE_PATH" ] && [ ! -L "$PACKAGE_PATH" ] || { printf '%s\n' 'PR525 lived: formed package missing.' >&2; exit 36; }
[ "$("$SHASUM_BIN" -a 256 "$PACKAGE_PATH" | /usr/bin/cut -d ' ' -f 1)" = "$PACKAGE_SHA" ] || { printf '%s\n' 'PR525 lived: formed package digest mismatch.' >&2; exit 37; }

BOOTSTRAP_COMMAND="$("$NODE_BIN" - "$SDK_ROOT/lib/vexlocalbridge/mac-vexlife-prod-source-bootstrap-command.js" "$PACKAGE_SHA" "$SDK_HEAD" "$SDK_TREE" <<'NODE'
const [modulePath,packageSha256,sdkHead,sdkTree]=process.argv.slice(2);
const m=require(modulePath);
process.stdout.write(m.renderMacVexLifeProdSourceBootstrapCommand({packageSha256,sdkHead,sdkTree}));
NODE
)"
[ -n "$BOOTSTRAP_COMMAND" ] || { printf '%s\n' 'PR525 lived: Product bootstrap was empty.' >&2; exit 38; }

printf '%s\n' '[4/4] Running composed runtime-continuity + Warm Boot lived proof.'
printf '%s\n' 'IMPORTANT: do not send anything while build/start/rotation is running.'
printf '%s\n' 'When VexLife opens automatically, send ONE short ordinary text message with no attachment.'
set +e
"$BASH_BIN" -c "$BOOTSTRAP_COMMAND" 2>&1 | "$TEE_BIN" "$EXECUTION_OUTPUT"
RUN_STATUS="${PIPESTATUS[0]}"
set -e

RETURN_JSON="$("$NODE_BIN" - "$EXECUTION_OUTPUT" <<'NODE'
const fs=require('node:fs'); const text=fs.readFileSync(process.argv[2],'utf8');
const paths=[...text.matchAll(/"resultZip"\s*:\s*"([^"]+)"/g)];
const shas=[...text.matchAll(/"resultZipSha256"\s*:\s*"([a-f0-9]{64})"/g)];
if(!paths.length||!shas.length) process.exit(0);
process.stdout.write(JSON.stringify({path:paths[paths.length-1][1],sha:shas[shas.length-1][1]}));
NODE
)"
if [ -n "$RETURN_JSON" ]; then
  RETURN_PATH="$("$NODE_BIN" -e 'process.stdout.write(JSON.parse(process.argv[1]).path)' "$RETURN_JSON")"
  RETURN_SHA="$("$NODE_BIN" -e 'process.stdout.write(JSON.parse(process.argv[1]).sha)' "$RETURN_JSON")"
  if [ -f "$RETURN_PATH" ]; then
    OBSERVED_RETURN_SHA="$("$SHASUM_BIN" -a 256 "$RETURN_PATH" | /usr/bin/cut -d ' ' -f 1)"
    if [ "$OBSERVED_RETURN_SHA" = "$RETURN_SHA" ]; then
      printf '\nPR525 lived canonical RETURN: %s\n' "$RETURN_PATH"
      printf 'SHA-256: %s\n' "$RETURN_SHA"
    else
      printf '%s\n' 'PR525 lived: RETURN path observed but digest mismatch; preserve terminal output and do not replay.' >&2
    fi
  fi
fi

if [ "$RUN_STATUS" -eq 0 ]; then
  printf '%s\n' 'PR525 lived task completed. Return the canonical RETURN ZIP revealed by Finder.'
else
  printf 'PR525 lived task stopped safely with exit %s. Return the canonical RETURN ZIP if one was formed; do not replay automatically.\n' "$RUN_STATUS"
fi
exit "$RUN_STATUS"
