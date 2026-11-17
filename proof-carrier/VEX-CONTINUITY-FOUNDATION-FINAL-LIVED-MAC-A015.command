#!/bin/bash
set -euo pipefail

# [VXG RealForever]
# PR #212 continuity foundation final Mac proof A015.
# Canonical route: current VexRelay native task-archive carrier.
# Source/merge authority remains PR #212.
#
# Proven carrier shape reused from successful #186/#180/#174:
# exact SDK source -> fetch source-readable specs -> SDK forms canonical package locally
# -> RUN-VEX-TASK.command -> canonical RETURN ZIP.
#
# No prebuilt Relay ZIP is committed or downloaded.

SDK_REPO='vgong24/Vextreme-SDK'
SDK_HEAD='8543c7d755faba04d4c4019c02e84f992667918a'
SDK_TREE='6376f68279f0750ee6d3868b8472368199763064'
CARRIER_REPO='VexGPT/VexLife-Prod'
CARRIER_REF='dc41add6296b672d75252c74790b04ac1b9f8775'
SPEC_ROOT='proof-carrier/continuity-foundation-final-a015'

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

GH="$(resolve_exec /opt/homebrew/bin/gh /usr/local/bin/gh)" || exit 12
GIT="$(resolve_exec /usr/bin/git)" || exit 11
NODE="$(resolve_exec /opt/homebrew/bin/node /usr/local/bin/node /usr/bin/node)" || exit 13

"$GH" auth status --hostname github.com >/dev/null 2>&1 || exit 21

BOOT_ROOT="$(/usr/bin/mktemp -d "$HOME/Downloads/.continuity-foundation-final-relay-a015.XXXXXX")"
cleanup() {
  case "$BOOT_ROOT" in
    "$HOME/Downloads"/.continuity-foundation-final-relay-a015.*) /bin/rm -rf -- "$BOOT_ROOT" ;;
  esac
}
trap cleanup EXIT HUP INT TERM

SDK_ROOT="$BOOT_ROOT/Vextreme-SDK"
SPEC_DIR="$BOOT_ROOT/spec"
/bin/mkdir -p "$SPEC_DIR"

printf '%s\n' 'Continuity foundation A015: binding exact VexRelay source...'
"$GH" repo clone "$SDK_REPO" "$SDK_ROOT" -- --no-checkout >/dev/null
"$GIT" -C "$SDK_ROOT" checkout --detach "$SDK_HEAD" >/dev/null
[ "$("$GIT" -C "$SDK_ROOT" rev-parse HEAD)" = "$SDK_HEAD" ] || exit 23
[ "$("$GIT" -C "$SDK_ROOT" rev-parse 'HEAD^{tree}')" = "$SDK_TREE" ] || exit 24
[ -z "$("$GIT" -C "$SDK_ROOT" status --porcelain=v1 -uall)" ] || exit 25

for name in seed stage authority outer-task; do
  "$GH" api --method GET \
    -H 'Accept: application/vnd.github.raw+json' \
    -f ref="$CARRIER_REF" \
    "repos/$CARRIER_REPO/contents/$SPEC_ROOT/$name.json" \
    > "$SPEC_DIR/$name.json"
done

"$NODE" --input-type=module - "$SDK_ROOT" "$SPEC_DIR" <<'NODEFORM'
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import { pathToFileURL } from 'node:url';

const [sdkRoot, specDir] = process.argv.slice(2);
const read = (name) => JSON.parse(fs.readFileSync(path.join(specDir, name + '.json'), 'utf8'));

const seed = read('seed');
const stage = read('stage');
const authorityEnvelope = read('authority');
const outerTaskTemplate = read('outer-task');

const now = new Date();
seed.execution.formedAt = now.toISOString();
seed.execution.expiresAtOrNull = new Date(now.getTime() + 4 * 60 * 60 * 1000).toISOString();

const formationUrl = pathToFileURL(
  path.join(sdkRoot, 'lib/vexlocalbridge/vexrelay-task-archive-carrier-formation.mjs'),
).href;
const { formVexRelayTaskArchiveCarrier } = await import(formationUrl);

const formed = await formVexRelayTaskArchiveCarrier({
  recipe: 'ExactHeadSourceOwnerProof',
  seed,
  stage,
  outerTaskTemplate,
  authorityEnvelope,
  outputDirectory: path.join(os.homedir(), 'Downloads'),
  outputStem: 'VEXLIFE-CONTINUITY-FOUNDATION-FINAL-A015-RELAY',
  ttlMs: 4 * 60 * 60 * 1000,
});

console.log('Canonical Relay package: ' + formed.outer.finalZip);
console.log('Canonical Relay package SHA-256: ' + formed.outer.digest);
console.log('Inner task: ' + formed.inner.manifest.taskRef);
console.log('Inner archive SHA-256: ' + formed.inner.sha256);

const runner = path.join(sdkRoot, 'lib/vexlocalbridge/templates/RUN-VEX-TASK.command');
const child = spawnSync('/bin/bash', [runner, formed.outer.finalZip], {
  stdio: 'inherit',
  env: process.env,
});
if (child.error) throw child.error;
process.exit(child.status ?? 2);
NODEFORM
