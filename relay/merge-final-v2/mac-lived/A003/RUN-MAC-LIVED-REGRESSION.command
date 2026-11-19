#!/bin/bash
set -Eeuo pipefail

CANDIDATE_HEAD='1c96c04d553ab54c475c16aeedc317e57412663c'
CANDIDATE_TREE='eb48643e221b99b6dc75eda075107dff278aca93'
CANDIDATE_BRANCH='VXG-111826-chatgpt-merge-final-v2-a001'
EXPECTED_MAIN='152c323ee6c3d4a43243d0a9c9b6de2ce11f605b'
REPOSITORY='VexGPT/VexLife-Prod'
ATTEMPT='V2-MAC-LIVED-A003'
RETURN_STEM='RETURN-VEXLIFE-MERGE-FINAL-V2-MAC-LIVED-A003'
HEAD12="$(printf '%s' "$CANDIDATE_HEAD" | /usr/bin/cut -c1-12)"

TMPBASE="$(getconf DARWIN_USER_TEMP_DIR 2>/dev/null || printf '/tmp/')"
BOOTSTRAP="$TMPBASE/vexlife-final-mac-lived-$ATTEMPT-$$"
REPO_ROOT="$BOOTSTRAP/repo"
EVIDENCE="$BOOTSTRAP/evidence"
MOUNT="$BOOTSTRAP/dmg-mount"
DOWNLOADS="$HOME/Downloads"
ARTIFACT_DIR="$DOWNLOADS/VexLife-Final-338-Mac-$HEAD12"
INSTALLED_APP='/Applications/VexLife.app'
SUCCESS=0
CURRENT_STAGE='INIT'
FAILURE=''
MOUNTED=0
BASE_HOME=''
STARTED_AT="$(/bin/date -u '+%Y-%m-%dT%H:%M:%SZ')"

mkdir -p "$EVIDENCE" "$DOWNLOADS" "$ARTIFACT_DIR"
exec > >(tee -a "$EVIDENCE/run.log") 2>&1

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

NODE_BIN="$(resolve_exec /opt/homebrew/bin/node /usr/local/bin/node /usr/bin/node)" || {
  echo 'FINAL_MAC_LIVED: qualified Node unavailable.' >&2
  exit 10
}
GH_BIN="$(resolve_exec /opt/homebrew/bin/gh /usr/local/bin/gh)" || {
  echo 'FINAL_MAC_LIVED: qualified GitHub CLI unavailable.' >&2
  exit 11
}
GIT_BIN="$(resolve_exec /usr/bin/git)" || exit 12
CURL_BIN="$(resolve_exec /usr/bin/curl)" || exit 13
SHASUM_BIN="$(resolve_exec /usr/bin/shasum)" || exit 14
DITTO_BIN="$(resolve_exec /usr/bin/ditto)" || exit 15
HDIUTIL_BIN="$(resolve_exec /usr/bin/hdiutil)" || exit 16
OPEN_BIN="$(resolve_exec /usr/bin/open)" || exit 17
CODESIGN_BIN="$(resolve_exec /usr/bin/codesign)" || exit 18

set_stage() {
  CURRENT_STAGE="$1"
  printf '\nSTAGE=%s\n' "$CURRENT_STAGE"
  printf '%s\t%s\n' "$("$NODE_BIN" -e 'process.stdout.write(new Date().toISOString())')" "$CURRENT_STAGE" >> "$EVIDENCE/stages.tsv"
}

fail() {
  FAILURE="$*"
  printf 'SAFE_FAILURE=%s\n' "$FAILURE" >&2
  return 1
}

summary() {
  cat > "$EVIDENCE/summary.txt" <<EOF
continuity=[VXG RealForever]
attempt=$ATTEMPT
candidateHead=$CANDIDATE_HEAD
candidateTree=$CANDIDATE_TREE
expectedMain=$EXPECTED_MAIN
startedAt=$STARTED_AT
currentStage=$CURRENT_STAGE
success=$SUCCESS
failure=$FAILURE
EOF
}

capture_failure_context() {
  set +e

  # Keep the failure package useful without retaining microphone audio or transcript text.
  if [ -n "${BASE_HOME:-}" ] && [ -d "$BASE_HOME" ]; then
    LOG_ROOT="$BASE_HOME/runtime/product-service/logs"
    CHRON_ROOT="$BASE_HOME/runtime/vexlife-prod/logs/chronology"
    if [ -f "$LOG_ROOT/service.err.log" ]; then
      /usr/bin/tail -n 500 "$LOG_ROOT/service.err.log" > "$EVIDENCE/service.err.tail.log" 2>/dev/null || true
    fi
    if [ -f "$LOG_ROOT/service.out.log" ]; then
      /usr/bin/tail -n 200 "$LOG_ROOT/service.out.log" > "$EVIDENCE/service.out.tail.log" 2>/dev/null || true
    fi
    if [ -f "$CHRON_ROOT/chronology.jsonl" ]; then
      /usr/bin/tail -n 500 "$CHRON_ROOT/chronology.jsonl" > "$EVIDENCE/chronology.tail.jsonl" 2>/dev/null || true
    fi
  fi

  for pair in     "failure-source-currentness|http://127.0.0.1:18210/api/v1/source-currentness"     "failure-current-state|http://127.0.0.1:18210/api/v1/current-state"     "failure-local-asr|http://127.0.0.1:18210/api/v1/local-asr"; do
    name="${pair%%|*}"
    url="${pair#*|}"
    "$CURL_BIN" -sS --max-time 5 -H 'Accept: application/json' "$url" > "$EVIDENCE/$name.json" 2>"$EVIDENCE/$name.curl.err" || true
  done

  /usr/bin/pgrep -fl 'whisper-server' > "$EVIDENCE/whisper-server-processes.txt" 2>/dev/null || true
  /bin/ps -axo pid,ppid,etime,state,command 2>/dev/null |
    /usr/bin/grep -E '[w]hisper-server|VexLifeProdServiceProcess'       > "$EVIDENCE/relevant-processes.txt" || true
  set -e
}

finalize() {
  rc=$?
  trap - EXIT HUP INT TERM
  set +e
  if [ "$MOUNTED" = '1' ]; then
    "$HDIUTIL_BIN" detach "$MOUNT" >/dev/null 2>&1 || true
  fi
  if [ "$SUCCESS" != '1' ]; then
    capture_failure_context
  fi
  summary
  printf '%s\n' "$rc" > "$EVIDENCE/exit-code.txt"
  (
    cd "$EVIDENCE" || exit 0
    find . -type f ! -name 'SHA256SUMS.txt' -print | LC_ALL=C sort |
      while IFS= read -r f; do "$SHASUM_BIN" -a 256 "$f"; done > SHA256SUMS.txt 2>/dev/null
  )
  tmp="$DOWNLOADS/$RETURN_STEM.zip"
  /bin/rm -f -- "$tmp"
  "$DITTO_BIN" -c -k --sequesterRsrc --keepParent "$EVIDENCE" "$tmp" >/dev/null 2>&1
  if [ -f "$tmp" ]; then
    digest="$("$SHASUM_BIN" -a 256 "$tmp" | /usr/bin/cut -d ' ' -f 1)"
    final="$DOWNLOADS/$RETURN_STEM--sha256-$digest.zip"
    /bin/mv -f -- "$tmp" "$final"
    echo
    echo '============================================================'
    if [ "$SUCCESS" = '1' ]; then
      echo 'FINAL_MAC_LIVED_DISPOSITION=PASS'
    else
      echo 'FINAL_MAC_LIVED_DISPOSITION=SAFE_FAILURE'
    fi
    echo "RETURN_ZIP=$final"
    echo "RETURN_SHA256=$digest"
    echo "DMG_PROOF_DIR=$ARTIFACT_DIR"
    echo '============================================================'
    "$OPEN_BIN" -R "$final" >/dev/null 2>&1 || true
  fi
  /bin/rm -rf -- "$BOOTSTRAP"
  exit "$rc"
}
trap finalize EXIT
trap 'FAILURE="INTERRUPTED_BY_HUMAN"; exit 130' HUP INT TERM

capture_get() {
  label="$1"
  url="$2"
  "$CURL_BIN" -fsS --max-time 15 -H 'Accept: application/json' "$url" > "$EVIDENCE/$label.json"
}

wait_exact_source() {
  seconds="$1"
  deadline=$(( $(/bin/date +%s) + seconds ))
  while [ "$(/bin/date +%s)" -lt "$deadline" ]; do
    if "$CURL_BIN" -fsS --max-time 5 'http://127.0.0.1:18210/api/v1/source-currentness' > "$EVIDENCE/source-currentness.wait.json" 2>/dev/null; then
      if "$NODE_BIN" -e '
        const fs=require("node:fs");
        const x=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));
        process.exit(x.sourceSha===process.argv[2] ? 0 : 1);
      ' "$EVIDENCE/source-currentness.wait.json" "$CANDIDATE_HEAD"; then
        return 0
      fi
    fi
    /bin/sleep 2
  done
  return 1
}

capture_product_state() {
  suffix="$1"
  capture_get "source-currentness.$suffix" 'http://127.0.0.1:18210/api/v1/source-currentness'
  capture_get "current-state.$suffix" 'http://127.0.0.1:18210/api/v1/current-state'
  capture_get "models.$suffix" 'http://127.0.0.1:18210/api/v1/models'
  capture_get "settings.$suffix" 'http://127.0.0.1:18210/api/v1/settings'
  capture_get "local-asr.$suffix" 'http://127.0.0.1:18210/api/v1/local-asr'
  capture_get "conversation.$suffix" 'http://127.0.0.1:18210/api/v1/conversation'
}

validate_product_state() {
  suffix="$1"
  expected_home="$2"
  "$NODE_BIN" -e '
    const fs=require("node:fs");
    const suffix=process.argv[1], expectedHead=process.argv[2], expectedHome=process.argv[3];
    const read=(name)=>JSON.parse(fs.readFileSync("evidence/"+name+"."+suffix+".json","utf8"));
    const source=read("source-currentness");
    const current=read("current-state");
    const asr=read("local-asr");
    if(source.sourceSha!==expectedHead) throw new Error("SOURCE_SHA_MISMATCH:"+source.sourceSha);
    if(!current.homePath) throw new Error("HOME_PATH_MISSING");
    if(expectedHome!=="-" && current.homePath!==expectedHome) throw new Error("HOME_CHANGED:"+current.homePath);
    const selected=(asr.options||[]).find(x=>x.selected===true);
    if(!selected) throw new Error("LOCAL_ASR_SELECTED_OPTION_MISSING");
    if(selected.providerRef!=="provider.local-asr.whispercpp") throw new Error("LOCAL_ASR_PROVIDER:"+selected.providerRef);
    if(selected.currentState!=="READY") throw new Error("LOCAL_ASR_NOT_READY:"+selected.currentState+":"+selected.currentDetailRef);
    process.stdout.write(current.homePath);
  ' "$suffix" "$CANDIDATE_HEAD" "$expected_home"
}

validate_chronology_since() {
  home="$1"
  since="$2"
  mode="$3"
  output="$4"
  "$NODE_BIN" - "$home" "$since" "$mode" "$output" "$CANDIDATE_HEAD" <<'NODE'
const fs=require('node:fs');
const path=require('node:path');
const [home,since,mode,output,head]=process.argv.slice(2);
const dir=path.join(home,'runtime/vexlife-prod/logs/chronology');
if(!fs.existsSync(dir)) throw new Error('CHRONOLOGY_DIRECTORY_MISSING:'+dir);
const records=[];
for(const name of fs.readdirSync(dir).filter(x=>x.startsWith('chronology')&&x.endsWith('.jsonl')).sort()){
  for(const line of fs.readFileSync(path.join(dir,name),'utf8').split(/\r?\n/)){
    if(!line.trim()) continue;
    try{
      const x=JSON.parse(line);
      if(Date.parse(x.at)>=Date.parse(since) && x.sourceSha===head) records.push(x);
    }catch{}
  }
}
fs.writeFileSync(output, records.map(x=>JSON.stringify(x)).join('\n')+(records.length?'\n':''));
const facts=new Set(records.map(x=>x.fact));
const bad=['TURN_FAILED','VOICE_INPUT_FAILURE_OBSERVED','LOCAL_ASR_INFERENCE_FAILED'].filter(x=>facts.has(x));
if(bad.length) throw new Error('FAILURE_FACTS_OBSERVED:'+bad.join(','));
if(mode==='mic'){
  const required=[
    'VOICE_INPUT_MICROPHONE_REQUESTED',
    'VOICE_INPUT_MICROPHONE_GRANTED',
    'VOICE_INPUT_AUDIO_CAPTURE_STARTED',
    'LOCAL_ASR_INFERENCE_COMPLETED',
    'TURN_COMPLETED'
  ];
  const missing=required.filter(x=>!facts.has(x));
  if(missing.length) throw new Error('MIC_FACTS_MISSING:'+missing.join(','));
}else if(mode==='reopen-turn'){
  if(!facts.has('TURN_COMPLETED')) throw new Error('REOPEN_TURN_COMPLETED_MISSING');
}
fs.writeFileSync(output+'.facts.txt',[...facts].sort().join('\n')+'\n');
NODE
}

human_pass() {
  prompt="$1"
  printf '\n%s\n' "$prompt"
  IFS= read -r answer
  case "$answer" in
    PASS|pass|Pass) return 0 ;;
    *) fail "HUMAN_HELD:$answer" ;;
  esac
}

set_stage HOST_AND_SOURCE
[ "$(/usr/bin/uname -s)" = 'Darwin' ] || fail 'MACOS_REQUIRED'
case "$(/usr/bin/uname -m)" in arm64|aarch64) ;; *) fail "APPLE_SILICON_REQUIRED:$("/usr/bin/uname" -m)" ;; esac
"$GH_BIN" auth status --hostname github.com >/dev/null 2>&1 || fail 'GITHUB_LOGIN_REQUIRED'
{
  echo "macOS=$(/usr/bin/sw_vers -productVersion)"
  echo "arch=$(/usr/bin/uname -m)"
  echo "memory=$(/usr/sbin/sysctl -n hw.memsize 2>/dev/null || echo UNKNOWN)"
} > "$EVIDENCE/host.txt"

echo 'Final Mac lived regression for exact #338 composed candidate'
echo "Candidate: $CANDIDATE_HEAD"
echo "Tree:      $CANDIDATE_TREE"

"$GH_BIN" repo clone "$REPOSITORY" "$REPO_ROOT" -- --no-checkout --origin origin >/dev/null
"$GIT_BIN" -C "$REPO_ROOT" fetch --no-tags origin "$CANDIDATE_BRANCH" main >/dev/null
BRANCH_HEAD="$("$GIT_BIN" -C "$REPO_ROOT" rev-parse "refs/remotes/origin/$CANDIDATE_BRANCH")"
MAIN_HEAD="$("$GIT_BIN" -C "$REPO_ROOT" rev-parse refs/remotes/origin/main)"
[ "$BRANCH_HEAD" = "$CANDIDATE_HEAD" ] || fail "BRANCH_HEAD_MISMATCH:$BRANCH_HEAD"
[ "$MAIN_HEAD" = "$EXPECTED_MAIN" ] || fail "MAIN_MOVED:$MAIN_HEAD"
"$GIT_BIN" -C "$REPO_ROOT" checkout --detach "$CANDIDATE_HEAD" >/dev/null
HEAD="$("$GIT_BIN" -C "$REPO_ROOT" rev-parse HEAD)"
TREE="$("$GIT_BIN" -C "$REPO_ROOT" rev-parse 'HEAD^{tree}')"
[ "$HEAD" = "$CANDIDATE_HEAD" ] || fail "HEAD_MISMATCH:$HEAD"
[ "$TREE" = "$CANDIDATE_TREE" ] || fail "TREE_MISMATCH:$TREE"
[ -z "$("$GIT_BIN" -C "$REPO_ROOT" status --porcelain=v1 -uall)" ] || fail 'SOURCE_DIRTY'
"$GIT_BIN" -C "$REPO_ROOT" merge-base --is-ancestor "$EXPECTED_MAIN" "$CANDIDATE_HEAD" || fail 'MAIN_NOT_ANCESTOR'
printf '%s\n' "$HEAD" > "$EVIDENCE/source-head.txt"
printf '%s\n' "$TREE" > "$EVIDENCE/source-tree.txt"
printf '%s\n' "$MAIN_HEAD" > "$EVIDENCE/observed-main.txt"

set_stage FOCUSED_SOURCE_GATE
export GRADLE_OPTS='-Dorg.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m -Dorg.gradle.workers.max=1 -Dkotlin.compiler.execution.strategy=in-process'
export KOTLIN_DAEMON_JVMARGS='-Xmx4096m'
(
  cd "$REPO_ROOT"
  ./gradlew --no-daemon --max-workers=1 \
    compileKotlinJvm compileTestKotlinJvm \
    jvmTest \
    --tests vexlife.localasr.VexLifeLocalAsrProviderTest \
    --tests vexlife.service.VexLifeProdLocalServiceTest.localAsrSelectionProjectsStorageLawfulnessWithoutPretendingAbsentModelsAreUsable \
    --stacktrace
) > "$EVIDENCE/focused-source-gate.log" 2>&1 || {
  cat "$EVIDENCE/focused-source-gate.log"
  fail 'FOCUSED_SOURCE_GATE_FAILED'
}
cat "$EVIDENCE/focused-source-gate.log"

set_stage FORM_EXACT_DMG
export GRADLE_OPTS='-Dorg.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m -Dorg.gradle.workers.max=1 -Dkotlin.compiler.execution.strategy=in-process'
export KOTLIN_DAEMON_JVMARGS='-Xmx4096m'
(
  cd "$REPO_ROOT"
  ./gradlew --no-daemon --max-workers=1 -PvexlifeAppVersion=0.0.0 archiveVexLifeProdMacApplication
) > "$EVIDENCE/dmg-formation.log" 2>&1 || {
  cat "$EVIDENCE/dmg-formation.log"
  fail 'DMG_FORMATION_FAILED'
}
cat "$EVIDENCE/dmg-formation.log"

APP_DIR="$REPO_ROOT/build/vexlife-prod-mac-application/$CANDIDATE_HEAD"
APP="$APP_DIR/VexLife.app"
DMG="$APP_DIR/VexLife-macOS-AppleSilicon.dmg"
DMG_SHA="$APP_DIR/VexLife-macOS-AppleSilicon.dmg.sha256"
RECEIPT="$APP_DIR/VexLife-macOS-AppleSilicon.receipt.json"
[ -d "$APP" ] || fail 'MAC_APP_MISSING'
[ -f "$DMG" ] || fail 'MAC_DMG_MISSING'
[ -f "$DMG_SHA" ] || fail 'MAC_DMG_SHA_MISSING'
[ -f "$RECEIPT" ] || fail 'MAC_RECEIPT_MISSING'
"$CODESIGN_BIN" --verify --strict "$APP" > "$EVIDENCE/codesign.txt" 2>&1 || fail 'CODESIGN_VERIFY_FAILED'
(
  cd "$APP_DIR"
  "$SHASUM_BIN" -a 256 -c VexLife-macOS-AppleSilicon.dmg.sha256
) > "$EVIDENCE/dmg-sha-verify.txt" 2>&1 || fail 'DMG_SHA_VERIFY_FAILED'
"$HDIUTIL_BIN" verify "$DMG" > "$EVIDENCE/dmg-hdiutil-verify.txt" 2>&1 || fail 'DMG_HDIUTIL_VERIFY_FAILED'
RECEIPT_SOURCE="$(/usr/bin/plutil -extract sourceSha raw -o - "$RECEIPT" 2>/dev/null || true)"
[ "$RECEIPT_SOURCE" = "$CANDIDATE_HEAD" ] || fail "DMG_RECEIPT_SOURCE_MISMATCH:$RECEIPT_SOURCE"
cp -f "$DMG" "$ARTIFACT_DIR/"
cp -f "$DMG_SHA" "$ARTIFACT_DIR/"
cp -f "$RECEIPT" "$ARTIFACT_DIR/"
cp -f "$RECEIPT" "$EVIDENCE/"
"$SHASUM_BIN" -a 256 "$DMG" > "$EVIDENCE/dmg-sha256.txt"

set_stage INSTALL_ORDINARY_PRODUCT
mkdir -p "$MOUNT"
"$HDIUTIL_BIN" attach "$DMG" -readonly -nobrowse -mountpoint "$MOUNT" > "$EVIDENCE/dmg-attach.txt"
MOUNTED=1
"$OPEN_BIN" "$MOUNT"

cat <<'HUMAN'
============================================================
FINAL VEXLIFE MAC INSTALL
This Finder window is the exact final #338 DMG.

1. If an older VexLife window is open, quit the app normally first.
   Do NOT kill the VexLife service or model processes manually.
2. Drag VexLife.app to Applications and replace the prior app if Finder asks.
3. Do not clear quarantine or bypass macOS security with a shell command.
4. Return here after the copy finishes.
============================================================
HUMAN
IFS= read -r _

[ -d "$INSTALLED_APP" ] || fail 'INSTALLED_APP_NOT_FOUND'
"$HDIUTIL_BIN" detach "$MOUNT" >/dev/null
MOUNTED=0

set_stage FIRST_PRODUCT_OPEN
"$OPEN_BIN" "$INSTALLED_APP"
cat <<'HUMAN'
============================================================
FINAL VEXLIFE MAC OPEN
Use the ordinary VexLife UI.

- If macOS blocks the unsigned app, use the normal System Settings
  Privacy & Security -> Open Anyway decision.
- If VexLife offers its normal Start / Update & Restart path for the
  existing Home and existing model, use it.
- Do NOT download/reinstall/switch reasoning or speech model bytes just
  to make this regression pass. If it unexpectedly asks for acquisition,
  return here and type HELD.
- When the VexLife workspace is open and usable, return here.
============================================================
HUMAN
human_pass 'Type PASS when the final candidate workspace is open and usable, or HELD with a short reason:'

wait_exact_source 300 || fail 'FINAL_SOURCE_NOT_REACHABLE'
capture_product_state first
cd "$BOOTSTRAP"
BASE_HOME="$(validate_product_state first -)" || fail 'FIRST_PRODUCT_STATE_INVALID'
printf '%s\n' "$BASE_HOME" > "$EVIDENCE/home-path.txt"
CHRONOLOGY_DIR="$BASE_HOME/runtime/vexlife-prod/logs/chronology"
[ -d "$CHRONOLOGY_DIR" ] || fail "CHRONOLOGY_DIRECTORY_MISSING:$CHRONOLOGY_DIR"

set_stage MACHINE_PROVIDER_PREPARE
PREPARE_STARTED="$("$NODE_BIN" -e 'process.stdout.write(new Date().toISOString())')"
printf '%s\n' "$PREPARE_STARTED" > "$EVIDENCE/provider-prepare-started.txt"
set +e
PREPARE_META="$("$CURL_BIN" -sS --max-time 70 \
  -X POST \
  -o "$EVIDENCE/provider-prepare.body" \
  -w '%{http_code}\t%{time_total}' \
  'http://127.0.0.1:18210/api/v1/local-asr/prepare' \
  2>"$EVIDENCE/provider-prepare.curl.err")"
PREPARE_RC=$?
set -e
printf '%s\n' "$PREPARE_META" > "$EVIDENCE/provider-prepare.http.txt"
printf '%s\n' "$PREPARE_RC" > "$EVIDENCE/provider-prepare.curl-exit.txt"
[ "$PREPARE_RC" -eq 0 ] || fail "LOCAL_ASR_PREPARE_CURL_FAILED:$PREPARE_RC"
PREPARE_CODE="$(printf '%s' "$PREPARE_META" | /usr/bin/cut -f 1)"
[ "$PREPARE_CODE" = '204' ] || fail "LOCAL_ASR_PREPARE_HTTP_$PREPARE_CODE"
capture_product_state prepared
validate_product_state prepared "$BASE_HOME" >/dev/null || fail 'PREPARED_PRODUCT_STATE_INVALID'
printf 'MACHINE_LOCAL_ASR_PREPARE=PASS\n' > "$EVIDENCE/provider-prepare-pass.txt"

set_stage MICROPHONE_AND_FIRST_TURN
MIC_START="$("$NODE_BIN" -e 'process.stdout.write(new Date().toISOString())')"
printf '%s\n' "$MIC_START" > "$EVIDENCE/mic-start.txt"
cat <<'HUMAN'
============================================================
FINAL MAC VOICEINPUT CHECK
In the VexLife workspace:

The machine preflight has already required the exact bundled provider to become ready.

1. Start Local Voice Recognition.
2. Allow microphone access if macOS/browser asks.
3. Wait until the control reaches Listening / the waveform is active.
   Do not press Stop while it still says it is preparing/requesting.
4. Speak one short sentence.
5. Stop dictation and confirm the recognized words are editable in the
   Composer and have NOT been sent automatically.
6. Click Send yourself and wait for Vex's full reply.
7. Do not switch models.
============================================================
HUMAN
human_pass 'Type PASS after the dictated message has been sent by you and Vex has fully replied, or HELD with a short reason:'

capture_product_state after-mic-turn
validate_product_state after-mic-turn "$BASE_HOME" >/dev/null || fail 'POST_MIC_PRODUCT_STATE_INVALID'
validate_chronology_since "$BASE_HOME" "$MIC_START" mic "$EVIDENCE/chronology-mic-turn.jsonl" ||
  fail 'MIC_OR_FIRST_TURN_CHRONOLOGY_NOT_PROVEN'

set_stage QUIT_AND_REOPEN
cat <<'HUMAN'
============================================================
QUIT / REOPEN CHECK
Quit the native VexLife app normally now (VexLife -> Quit or Cmd-Q).
Do not kill launchd, Java, Python, whisper-server, or model processes.
Return here after the native app has closed.
============================================================
HUMAN
IFS= read -r _

"$OPEN_BIN" "$INSTALLED_APP"
wait_exact_source 300 || fail 'REOPEN_SOURCE_NOT_REACHABLE'
capture_product_state reopen
validate_product_state reopen "$BASE_HOME" >/dev/null || fail 'REOPEN_PRODUCT_STATE_INVALID'

cat <<'HUMAN'
============================================================
REOPEN HUMAN CHECK
Confirm in the reopened VexLife workspace:
- the Product is usable;
- the prior Conversation is still visible in Archives/history;
- the same Home/model are in use;
- no new download/reconfiguration was required.
============================================================
HUMAN
human_pass 'Type PASS when confirmed, or HELD with a short reason:'

set_stage REOPEN_SECOND_TURN
SECOND_START="$("$NODE_BIN" -e 'process.stdout.write(new Date().toISOString())')"
printf '%s\n' "$SECOND_START" > "$EVIDENCE/reopen-turn-start.txt"
cat <<'HUMAN'
============================================================
SECOND REAL TURN
Send one ordinary typed message to Vex and wait for the full reply.
Do not use the microphone for this second turn and do not switch models.
============================================================
HUMAN
human_pass 'Type PASS after the second Vex reply is complete, or HELD with a short reason:'

capture_product_state final
validate_product_state final "$BASE_HOME" >/dev/null || fail 'FINAL_PRODUCT_STATE_INVALID'
validate_chronology_since "$BASE_HOME" "$SECOND_START" reopen-turn "$EVIDENCE/chronology-reopen-turn.jsonl" ||
  fail 'REOPEN_TURN_CHRONOLOGY_NOT_PROVEN'

cat > "$EVIDENCE/human-confirmation.txt" <<EOF
exactFinalCandidateInstalled=yes
focusedSourceGatePassed=yes
machineLocalAsrPreparePassed=yes
workspaceUsable=yes
microphoneDictationReachedEditableComposer=yes
dictationDidNotAutoSend=yes
humanPerformedSend=yes
firstVexReplyCompleted=yes
normalQuitPerformed=yes
reopenUsable=yes
priorConversationVisibleAfterReopen=yes
sameHomePreserved=yes
secondTypedTurnCompleted=yes
candidateHead=$CANDIDATE_HEAD
homePath=$BASE_HOME
EOF

set_stage COMPLETE
SUCCESS=1
echo
echo 'Final composed #338 Mac lived regression completed.'
