#!/bin/bash
set -euo pipefail

CONTINUITY='[VXG RealForever]'
PRODUCT_REPOSITORY='VexGPT/VexLife-Prod'
PRODUCT_HEAD='e00832bf3e28e2134da71d0191282ff47e2d7211'
PRODUCT_TREE='2fb64cb7bb7009c866b53fb7b3488c6bff396e59'
PRODUCT_BRANCH='VXG-111926-chatgpt-labs-native-tools-convergence-a001'
TARGET_MODEL_REF='model.vexlife-prod.heretic.qwen3.8-4b-distill.mlx4.d2f8f4028f20'
SCENARIO_REF='scenario.vexlife.continuity-pressure.depth-continuity.v1'
SCENARIO_SHA='65c81bfdcc0935d261204566bfc05468317c59e962eabceffb9f7e0fca359cc1'
RUNTIME_HOME="$HOME/.vexlife"

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

BASH_BIN="$(resolve_exec /bin/bash)" || exit 10
GIT_BIN="$(resolve_exec /usr/bin/git)" || exit 11
GH_BIN="$(resolve_exec /opt/homebrew/bin/gh /usr/local/bin/gh)" || exit 12
NODE_BIN="$(resolve_exec /opt/homebrew/bin/node /usr/local/bin/node /usr/bin/node)" || exit 13
ZIP_BIN="$(resolve_exec /usr/bin/zip)" || exit 14
SHASUM_BIN="$(resolve_exec /usr/bin/shasum)" || exit 15
MKTEMP_BIN="$(resolve_exec /usr/bin/mktemp)" || exit 16
RM_BIN="$(resolve_exec /bin/rm /usr/bin/rm)" || exit 17
FIND_BIN="$(resolve_exec /usr/bin/find)" || exit 18
CP_BIN="$(resolve_exec /bin/cp)" || exit 19
MKDIR_BIN="$(resolve_exec /bin/mkdir)" || exit 20

"$GH_BIN" auth status --hostname github.com >/dev/null 2>&1 || {
  echo '4B15: GitHub authentication is required.' >&2
  exit 21
}

ROOT="$("$MKTEMP_BIN" -d "$HOME/Downloads/.vexlife-pr393-4b15-a006.XXXXXX")"
REPO_ROOT="$ROOT/VexLife-Prod"
RESULT_DIR="$ROOT/result"
RETURN_DIR="$ROOT/return"
STDOUT_LOG="$RETURN_DIR/harness.stdout.log"
STDERR_LOG="$RETURN_DIR/harness.stderr.log"
PREFLIGHT="$RETURN_DIR/preflight.json"
SUMMARY="$RETURN_DIR/RESULT-SUMMARY.txt"
MANIFEST="$RETURN_DIR/manifest.json"

cleanup() {
  case "$ROOT" in
    "$HOME"/Downloads/.vexlife-pr393-4b15-a006.*) "$RM_BIN" -rf -- "$ROOT" ;;
    *) return 1 ;;
  esac
}
trap cleanup EXIT HUP INT TERM

"$MKDIR_BIN" -p "$RESULT_DIR" "$RETURN_DIR"

echo '4B15: acquiring exact integrated #393 source...'
"$GH_BIN" repo clone "$PRODUCT_REPOSITORY" "$REPO_ROOT" -- --no-checkout >/dev/null
"$GIT_BIN" -C "$REPO_ROOT" checkout --detach "$PRODUCT_HEAD" >/dev/null
OBSERVED_HEAD="$("$GIT_BIN" -C "$REPO_ROOT" rev-parse HEAD)"
OBSERVED_TREE="$("$GIT_BIN" -C "$REPO_ROOT" rev-parse 'HEAD^{tree}')"
[ "$OBSERVED_HEAD" = "$PRODUCT_HEAD" ] || { echo '4B15: source head mismatch.' >&2; exit 22; }
[ "$OBSERVED_TREE" = "$PRODUCT_TREE" ] || { echo '4B15: source tree mismatch.' >&2; exit 23; }
[ -z "$("$GIT_BIN" -C "$REPO_ROOT" status --porcelain=v1 -uall)" ] || { echo '4B15: source checkout is dirty.' >&2; exit 24; }

SCENARIO="$REPO_ROOT/tools/continuity-pressure/scenarios/depth-continuity-v1.json"
OBS_SCENARIO_SHA="$("$SHASUM_BIN" -a 256 "$SCENARIO" | /usr/bin/cut -d ' ' -f 1)"
[ "$OBS_SCENARIO_SHA" = "$SCENARIO_SHA" ] || { echo "4B15: scenario digest mismatch: $OBS_SCENARIO_SHA" >&2; exit 25; }

MODEL_BINDING="$RUNTIME_HOME/runtime/vexlife-prod/model-binding.json"
OBSERVED_MODEL=''
if [ -f "$MODEL_BINDING" ]; then
  OBSERVED_MODEL="$("$NODE_BIN" -e '
    const fs=require("node:fs");
    try {
      const v=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));
      process.stdout.write(typeof v.modelRef==="string"?v.modelRef:"");
    } catch {}
  ' "$MODEL_BINDING")"
fi

"$NODE_BIN" - "$PREFLIGHT" "$PRODUCT_HEAD" "$PRODUCT_TREE" "$RUNTIME_HOME" "$TARGET_MODEL_REF" "$OBSERVED_MODEL" "$OBS_SCENARIO_SHA" <<'NODE_PREFLIGHT'
const fs=require('node:fs');
const [out,head,tree,home,target,observed,scenarioSha]=process.argv.slice(2);
fs.writeFileSync(out, JSON.stringify({
  schemaVersion:'vexlife-prod.pr393-4b15-preflight/v1',
  continuity:'[VXG RealForever]',
  candidateHead:head,
  candidateTree:tree,
  runtimeHome:home,
  targetModelRef:target,
  observedModelRef:observed || null,
  scenarioRef:'scenario.vexlife.continuity-pressure.depth-continuity.v1',
  scenarioSha256:scenarioSha,
  effectiveInputTokenBudget:80000,
  payloadScale:7,
  sets:3,
  turnsPerSet:5,
  realHomeMutationAllowed:false,
  modelSwitchAllowed:false,
  runtimeStartAllowed:false,
  scratchConversationOnly:true,
  gradleJvmArgs:'-Xmx2048m -XX:MaxMetaspaceSize=768m -XX:ReservedCodeCacheSize=512m',
  expectedCapabilityRepresentation:'VEXLIFE_CONTROL_TEXT',
  assuranceState:'CLEAR_ON_EXACT_HEAD',
},null,2)+'\n');
NODE_PREFLIGHT

HARNESS_STATUS=0
ANALYZE_STATUS=null
RUN_CLASS='LIVE_4B_15TURN'
if [ "$OBSERVED_MODEL" != "$TARGET_MODEL_REF" ]; then
  RUN_CLASS='HELD_MODEL_NOT_4B'
  "$NODE_BIN" - "$RESULT_DIR/held.json" "$OBSERVED_MODEL" "$TARGET_MODEL_REF" <<'NODE_HELD'
const fs=require('node:fs');
const [out,observed,target]=process.argv.slice(2);
fs.writeFileSync(out, JSON.stringify({
  state:'HELD',
  stage:'full',
  executionClass:'LIVE_MODEL',
  reason:'canonical ~/.vexlife runtime binding is not the exact Heretic 4B Distill target',
  observedModelRef:observed || null,
  requiredModelRef:target,
  effectsPerformed:false,
},null,2)+'\n');
NODE_HELD
  HARNESS_STATUS=3
else
  echo '4B15: exact 4B binding observed; entering dedicated synthetic 15-turn run...'
  set +e
  (
    cd "$REPO_ROOT"
    export GRADLE_OPTS='-Dorg.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=768m -XX:ReservedCodeCacheSize=512m'
    "$NODE_BIN" tools/continuity-pressure/run.mjs       --stage full       --scenario-ref "$SCENARIO_REF"       --scenario-file tools/continuity-pressure/scenarios/depth-continuity-v1.json       --result-dir "$RESULT_DIR"       --runtime-home "$RUNTIME_HOME"       --experiment-window       --effective-input-token-budget 80000       --payload-scale 7       --sets 3       --turns-per-set 5
  ) >"$STDOUT_LOG" 2>"$STDERR_LOG"
  HARNESS_STATUS=$?
  set -e

  if [ -f "$RESULT_DIR/result.json" ]; then
    set +e
    (
      cd "$REPO_ROOT"
      export GRADLE_OPTS='-Dorg.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=768m -XX:ReservedCodeCacheSize=512m'
      "$NODE_BIN" tools/continuity-pressure/run.mjs         --stage analyze         --scenario-ref "$SCENARIO_REF"         --scenario-file tools/continuity-pressure/scenarios/depth-continuity-v1.json         --result-dir "$RESULT_DIR"
    ) >>"$STDOUT_LOG" 2>>"$STDERR_LOG"
    ANALYZE_STATUS=$?
    set -e
  fi
fi

for name in result.json analysis.json held.json transition-timing.json host-scan-timing.json; do
  if [ -f "$RESULT_DIR/$name" ]; then
    "$CP_BIN" "$RESULT_DIR/$name" "$RETURN_DIR/$name"
  fi
done

HARNESS_ZIP="$("$FIND_BIN" "$RESULT_DIR" -maxdepth 1 -type f -name 'continuity-pressure-*.zip' -print | /usr/bin/head -n 1)"
if [ -n "$HARNESS_ZIP" ] && [ -f "$HARNESS_ZIP" ]; then
  "$CP_BIN" "$HARNESS_ZIP" "$RETURN_DIR/$(/usr/bin/basename "$HARNESS_ZIP")"
fi

"$NODE_BIN" - "$SUMMARY" "$RETURN_DIR" "$HARNESS_STATUS" "$ANALYZE_STATUS" "$RUN_CLASS" "$PRODUCT_HEAD" "$PRODUCT_TREE" "$TARGET_MODEL_REF" "$OBSERVED_MODEL" <<'NODE_SUMMARY'
const fs=require('node:fs'),path=require('node:path');
const [out,dir,harnessStatus,analyzeStatus,runClass,head,tree,target,observed]=process.argv.slice(2);
let state='NO_RESULT', turnCount=null, resultStatus=null, summary=null, failure=null;
if(fs.existsSync(path.join(dir,'result.json'))){
  const r=JSON.parse(fs.readFileSync(path.join(dir,'result.json'),'utf8'));
  state='RESULT';
  resultStatus=r.status;
  turnCount=Array.isArray(r.turns)?r.turns.length:null;
  summary=r.summary??null;
  failure=r.failure??null;
}else if(fs.existsSync(path.join(dir,'held.json'))){
  const h=JSON.parse(fs.readFileSync(path.join(dir,'held.json'),'utf8'));
  state='HELD'; failure=h.reason??null;
}
const lines=[
  '[VXG RealForever]',
  'PR393_4B15_RETURN/v1',
  'runClass='+runClass,
  'candidateHead='+head,
  'candidateTree='+tree,
  'targetModelRef='+target,
  'observedModelRef='+(observed||'NONE'),
  'harnessExit='+harnessStatus,
  'analyzeExit='+(analyzeStatus==='null'?'NONE':analyzeStatus),
  'state='+state,
  'resultStatus='+(resultStatus??'NONE'),
  'turnCount='+(turnCount??'NONE'),
  'failure='+(failure??'NONE'),
  'summary='+(summary?JSON.stringify(summary):'NONE'),
];
fs.writeFileSync(out,lines.join('\n')+'\n');
NODE_SUMMARY

"$NODE_BIN" - "$MANIFEST" "$RETURN_DIR" "$PRODUCT_HEAD" "$PRODUCT_TREE" "$HARNESS_STATUS" "$ANALYZE_STATUS" <<'NODE_MANIFEST'
const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto');
const [out,dir,head,tree,harnessStatus,analyzeStatus]=process.argv.slice(2);
const files={};
for(const name of fs.readdirSync(dir).sort()){
  if(name==='manifest.json') continue;
  const p=path.join(dir,name);
  if(!fs.statSync(p).isFile()) continue;
  const b=fs.readFileSync(p);
  files[name]={sha256:crypto.createHash('sha256').update(b).digest('hex'),bytes:b.length};
}
fs.writeFileSync(out,JSON.stringify({
  schemaVersion:'vexlife-prod.pr393-4b15-return/v1',
  continuity:'[VXG RealForever]',
  candidateHead:head,
  candidateTree:tree,
  harnessExit:Number(harnessStatus),
  analyzeExit:analyzeStatus==='null'?null:Number(analyzeStatus),
  files,
},null,2)+'\n');
NODE_MANIFEST

STAGING="$HOME/Downloads/RETURN-VEXLIFE-PR393-4B-15TURN-FOUNDATION-A006.zip"
"$RM_BIN" -f -- "$STAGING"
(
  cd "$RETURN_DIR"
  "$ZIP_BIN" -q -r "$STAGING" .
)
DIGEST="$("$SHASUM_BIN" -a 256 "$STAGING" | /usr/bin/cut -d ' ' -f 1)"
FINAL="$HOME/Downloads/RETURN-VEXLIFE-PR393-4B-15TURN-FOUNDATION-A006--sha256-$DIGEST.zip"
"$RM_BIN" -f -- "$FINAL"
mv "$STAGING" "$FINAL"

echo "4B15: RETURN $FINAL"
echo "4B15: sha256 $DIGEST"
echo '4B15: upload that exact RETURN ZIP.'
exit 0
