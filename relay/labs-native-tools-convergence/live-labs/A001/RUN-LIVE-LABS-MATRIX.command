#!/bin/bash
set -euo pipefail

CONTINUITY='[VXG RealForever]'
PRODUCT_REPOSITORY='VexGPT/VexLife-Prod'
PRODUCT_HEAD='4a325b928668bca17d43f5f38eb2099334066a4b'
PRODUCT_TREE='8230f579226c96492d6d31144cba974aac8417d4'
PRODUCT_BRANCH='VXG-111926-chatgpt-labs-native-tools-convergence-a001'
TARGET_MODEL_REF='model.vexlife-prod.heretic.qwen3.8-4b-distill.mlx4.d2f8f4028f20'
ADAPTER_BLOB='f7a6a99a0a5ec852fcb85ad74b753481c9b9c85c'
ADAPTER_SHA256='b8c326e980574d71276b92b0bd7ce6e1daa4c2273dd67e62fd269179635896bf'
A012_RETURN_SHA256='18d09b29fc03f7afe8333d1e99cc6f9ba56d6dc68e69bf1602f81eaa55fa3c77'
A012_INNER_SHA256='30410c43fef1be77a5fed630b5ef2c568794f9ee05c435d1ce4de9c7ce02d5ef'
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

GIT_BIN="$(resolve_exec /usr/bin/git)" || exit 10
GH_BIN="$(resolve_exec /opt/homebrew/bin/gh /usr/local/bin/gh)" || exit 11
NODE_BIN="$(resolve_exec /opt/homebrew/bin/node /usr/local/bin/node /usr/bin/node)" || exit 12
ZIP_BIN="$(resolve_exec /usr/bin/zip)" || exit 13
SHASUM_BIN="$(resolve_exec /usr/bin/shasum)" || exit 14
MKTEMP_BIN="$(resolve_exec /usr/bin/mktemp)" || exit 15
RM_BIN="$(resolve_exec /bin/rm /usr/bin/rm)" || exit 16
CP_BIN="$(resolve_exec /bin/cp)" || exit 17
MKDIR_BIN="$(resolve_exec /bin/mkdir)" || exit 18
MV_BIN="$(resolve_exec /bin/mv /usr/bin/mv)" || exit 19

"$GH_BIN" auth status --hostname github.com >/dev/null 2>&1 || {
  echo 'PR393 Labs Matrix A001: GitHub authentication is required.' >&2
  exit 20
}

ROOT="$("$MKTEMP_BIN" -d "$HOME/Downloads/.vexlife-pr393-labs-matrix-a001.XXXXXX")"
REPO_ROOT="$ROOT/VexLife-Prod"
RESULT_DIR="$ROOT/result"
RETURN_DIR="$ROOT/return"
MATRIX_RESULT="$RESULT_DIR/matrix-result.json"
STDOUT_LOG="$RETURN_DIR/gradle.stdout.log"
STDERR_LOG="$RETURN_DIR/gradle.stderr.log"
PREFLIGHT="$RETURN_DIR/preflight.json"
SUMMARY="$RETURN_DIR/RESULT-SUMMARY.txt"
MANIFEST="$RETURN_DIR/manifest.json"

cleanup() {
  case "$ROOT" in
    "$HOME"/Downloads/.vexlife-pr393-labs-matrix-a001.*) "$RM_BIN" -rf -- "$ROOT" ;;
    *) return 1 ;;
  esac
}
trap cleanup EXIT HUP INT TERM
"$MKDIR_BIN" -p "$RESULT_DIR" "$RETURN_DIR"

echo 'PR393 Labs Matrix A001: acquiring exact Product candidate...'
"$GH_BIN" repo clone "$PRODUCT_REPOSITORY" "$REPO_ROOT" -- --no-checkout >/dev/null
"$GIT_BIN" -C "$REPO_ROOT" checkout --detach "$PRODUCT_HEAD" >/dev/null
OBSERVED_HEAD="$("$GIT_BIN" -C "$REPO_ROOT" rev-parse HEAD)"
OBSERVED_TREE="$("$GIT_BIN" -C "$REPO_ROOT" rev-parse 'HEAD^{tree}')"
[ "$OBSERVED_HEAD" = "$PRODUCT_HEAD" ] || { echo 'candidate head mismatch' >&2; exit 21; }
[ "$OBSERVED_TREE" = "$PRODUCT_TREE" ] || { echo 'candidate tree mismatch' >&2; exit 22; }
[ -z "$("$GIT_BIN" -C "$REPO_ROOT" status --porcelain=v1 -uall)" ] || { echo 'candidate checkout dirty before adapter' >&2; exit 23; }

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

"$NODE_BIN" - "$PREFLIGHT" "$PRODUCT_HEAD" "$PRODUCT_TREE" "$TARGET_MODEL_REF" "$OBSERVED_MODEL" "$ADAPTER_BLOB" "$ADAPTER_SHA256" "$A012_RETURN_SHA256" "$A012_INNER_SHA256" <<'NODE_PREFLIGHT'
const fs=require('node:fs');
const [out,head,tree,target,observed,blob,adapterSha,a012,a012inner]=process.argv.slice(2);
fs.writeFileSync(out,JSON.stringify({
  schemaVersion:'vexlife-prod.pr393-vex-labs-live-matrix-preflight/v1',
  continuity:'[VXG RealForever]',
  candidateHead:head,
  candidateTree:tree,
  targetModelRef:target,
  observedModelRef:observed||null,
  adapterGitBlob:blob,
  adapterSha256:adapterSha,
  sourceOwnerA012ReturnSha256:a012,
  sourceOwnerA012InnerSha256:a012inner,
  sourceOwnerState:'PASS',
  assuranceState:'CLEAR_ON_EXACT_HEAD',
  matrixCheckpointComment:7010554288,
  rows:['A_CURRENT_DEFAULT','B_NONE_ZERO','C_CURRENT_LOWER_NONZERO','D_BELOW_FLOOR_HELD','E_ABOVE_FLOOR_REAL_REPLY','F_SECOND_TURN_PRIOR_PRESENT'],
  realHomeMutationAllowed:false,
  modelSwitchAllowed:false,
  runtimeStartAllowed:false,
  acquisitionAllowed:false,
  scratchConversationOnly:true,
  rawProviderBodyPersisted:false
},null,2)+'\n');
NODE_PREFLIGHT

HARNESS_STATUS=0
RUN_CLASS='LIVE_LABS_MATRIX'
if [ "$OBSERVED_MODEL" != "$TARGET_MODEL_REF" ]; then
  RUN_CLASS='HELD_MODEL_NOT_4B'
  "$NODE_BIN" - "$MATRIX_RESULT" "$OBSERVED_MODEL" "$TARGET_MODEL_REF" <<'NODE_HELD'
const fs=require('node:fs');
const [out,observed,target]=process.argv.slice(2);
fs.writeFileSync(out,JSON.stringify({
  schemaVersion:'vexlife-prod.pr393-vex-labs-live-matrix/v1',
  continuity:'[VXG RealForever]',
  status:'HELD',
  reason:'canonical runtime binding is not the exact Heretic 4B target',
  observedModelRef:observed||null,
  requiredModelRef:target,
  effectsPerformed:false
},null,2)+'\n');
NODE_HELD
  HARNESS_STATUS=3
else
  ADAPTER_PATH="$REPO_ROOT/src/jvmTest/kotlin/vexlife/harness/VexLabsLiveMatrixAdapterTest.kt"
  "$GH_BIN" api -H "Accept: application/vnd.github.raw+json" "/repos/$PRODUCT_REPOSITORY/git/blobs/$ADAPTER_BLOB" > "$ADAPTER_PATH"
  OBSERVED_BLOB="$("$GIT_BIN" hash-object "$ADAPTER_PATH")"
  OBSERVED_ADAPTER_SHA="$("$SHASUM_BIN" -a 256 "$ADAPTER_PATH" | /usr/bin/cut -d ' ' -f 1)"
  [ "$OBSERVED_BLOB" = "$ADAPTER_BLOB" ] || { echo "adapter blob mismatch: $OBSERVED_BLOB" >&2; exit 24; }
  [ "$OBSERVED_ADAPTER_SHA" = "$ADAPTER_SHA256" ] || { echo "adapter sha256 mismatch: $OBSERVED_ADAPTER_SHA" >&2; exit 25; }

  echo 'PR393 Labs Matrix A001: exact 4B binding observed; running six-row real-runtime Lab matrix...'
  set +e
  (
    cd "$REPO_ROOT"
    export GRADLE_OPTS='-Dorg.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=768m -XX:ReservedCodeCacheSize=512m'
    export VEXLABS_RUNTIME_HOME="$RUNTIME_HOME"
    export VEXLABS_MATRIX_RESULT="$MATRIX_RESULT"
    ./gradlew jvmTest \
      --tests 'vexlife.harness.VexLabsLiveMatrixAdapterTest.integratedLiveConfigurationMatrix' \
      --stacktrace \
      --no-daemon \
      --max-workers=1 \
      '-Dorg.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=768m -XX:ReservedCodeCacheSize=512m'
  ) >"$STDOUT_LOG" 2>"$STDERR_LOG"
  HARNESS_STATUS=$?
  set -e
fi

if [ -f "$MATRIX_RESULT" ]; then
  "$CP_BIN" "$MATRIX_RESULT" "$RETURN_DIR/matrix-result.json"
fi

"$NODE_BIN" - "$SUMMARY" "$RETURN_DIR" "$HARNESS_STATUS" "$RUN_CLASS" "$PRODUCT_HEAD" "$PRODUCT_TREE" "$TARGET_MODEL_REF" "$OBSERVED_MODEL" <<'NODE_SUMMARY'
const fs=require('node:fs'),path=require('node:path');
const [out,dir,harnessStatus,runClass,head,tree,target,observed]=process.argv.slice(2);
let status='NO_RESULT', rows=0, providerCalls=null, failure=null;
const p=path.join(dir,'matrix-result.json');
if(fs.existsSync(p)){
  const r=JSON.parse(fs.readFileSync(p,'utf8'));
  status=r.status||'UNKNOWN';
  rows=Array.isArray(r.rows)?r.rows.length:0;
  providerCalls=r.providerCallCount??null;
  failure=r.failure??r.reason??null;
}
fs.writeFileSync(out,[
  '[VXG RealForever]',
  'PR393_VEX_LABS_LIVE_MATRIX_A001_RETURN/v1',
  'runClass='+runClass,
  'candidateHead='+head,
  'candidateTree='+tree,
  'targetModelRef='+target,
  'observedModelRef='+(observed||'NONE'),
  'harnessExit='+harnessStatus,
  'matrixStatus='+status,
  'rowCount='+rows,
  'providerCallCount='+(providerCalls??'NONE'),
  'failure='+(failure??'NONE')
].join('\n')+'\n');
NODE_SUMMARY

"$NODE_BIN" - "$MANIFEST" "$RETURN_DIR" "$PRODUCT_HEAD" "$PRODUCT_TREE" "$HARNESS_STATUS" <<'NODE_MANIFEST'
const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto');
const [out,dir,head,tree,harnessStatus]=process.argv.slice(2);
const files={};
for(const name of fs.readdirSync(dir).sort()){
  if(name==='manifest.json') continue;
  const p=path.join(dir,name);
  if(!fs.statSync(p).isFile()) continue;
  const b=fs.readFileSync(p);
  files[name]={sha256:crypto.createHash('sha256').update(b).digest('hex'),bytes:b.length};
}
fs.writeFileSync(out,JSON.stringify({
  schemaVersion:'vexlife-prod.pr393-vex-labs-live-matrix-return/v1',
  continuity:'[VXG RealForever]',
  candidateHead:head,
  candidateTree:tree,
  harnessExit:Number(harnessStatus),
  files
},null,2)+'\n');
NODE_MANIFEST

STAGING="$HOME/Downloads/RETURN-VEXLIFE-PR393-LABS-MATRIX-A001.zip"
"$RM_BIN" -f -- "$STAGING"
(
  cd "$RETURN_DIR"
  "$ZIP_BIN" -q -r "$STAGING" .
)
DIGEST="$("$SHASUM_BIN" -a 256 "$STAGING" | /usr/bin/cut -d ' ' -f 1)"
FINAL="$HOME/Downloads/RETURN-VEXLIFE-PR393-LABS-MATRIX-A001--sha256-$DIGEST.zip"
"$RM_BIN" -f -- "$FINAL"
"$MV_BIN" "$STAGING" "$FINAL"

echo "PR393 Labs Matrix A001: RETURN $FINAL"
echo "PR393 Labs Matrix A001: sha256 $DIGEST"
echo 'PR393 Labs Matrix A001: upload that exact RETURN ZIP.'
exit 0
