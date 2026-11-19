#!/bin/bash
set -Eeuo pipefail

CANDIDATE_HEAD='ead6343c049a09bcc6aed3ae646624d8e739351c'
CANDIDATE_TREE='60cc787d33da5223fd4f07d82ae37515c9230006'
CANDIDATE_BRANCH='VXG-111926-chatgpt-webtoapp-product-quit-a001'
BASE_HEAD='cb47d5c92db709048ac668cfa3c0eadd34b02152'
BASE_BRANCH='VXG-111826-chatgpt-merge-final-v2-a001'
REPOSITORY='VexGPT/VexLife-Prod'
ATTEMPT='WEBTOAPP-PRODUCT-QUIT-MAC-A001'
RETURN_STEM='RETURN-VEXLIFE-WEBTOAPP-PRODUCT-QUIT-MAC-A001'
HEAD12="$(printf '%s' "$CANDIDATE_HEAD" | /usr/bin/cut -c1-12)"

TMPBASE="$(getconf DARWIN_USER_TEMP_DIR 2>/dev/null || printf '/tmp/')"
BOOTSTRAP="$TMPBASE/vexlife-webtoapp-quit-$ATTEMPT-$$"
REPO_ROOT="$BOOTSTRAP/repo"
EVIDENCE="$BOOTSTRAP/evidence"
MOUNT="$BOOTSTRAP/dmg-mount"
DOWNLOADS="$HOME/Downloads"
ARTIFACT_DIR="$DOWNLOADS/VexLife-WebToApp-Quit-Mac-$HEAD12"
INSTALLED_APP='/Applications/VexLife.app'
SERVICE_RECEIPT="$HOME/Library/Application Support/VexLife/ProductService/service-lifetime.json"
SUCCESS=0
CURRENT_STAGE='INIT'
FAILURE=''
MOUNTED=0
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

NODE_BIN="$(resolve_exec /opt/homebrew/bin/node /usr/local/bin/node /usr/bin/node)" || exit 10
GH_BIN="$(resolve_exec /opt/homebrew/bin/gh /usr/local/bin/gh)" || exit 11
GIT_BIN="$(resolve_exec /usr/bin/git)" || exit 12
CURL_BIN="$(resolve_exec /usr/bin/curl)" || exit 13
SHASUM_BIN="$(resolve_exec /usr/bin/shasum)" || exit 14
DITTO_BIN="$(resolve_exec /usr/bin/ditto)" || exit 15
HDIUTIL_BIN="$(resolve_exec /usr/bin/hdiutil)" || exit 16
OPEN_BIN="$(resolve_exec /usr/bin/open)" || exit 17
CODESIGN_BIN="$(resolve_exec /usr/bin/codesign)" || exit 18
LAUNCHCTL_BIN="$(resolve_exec /bin/launchctl)" || exit 19
PS_BIN="$(resolve_exec /bin/ps)" || exit 20

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
baseHead=$BASE_HEAD
startedAt=$STARTED_AT
currentStage=$CURRENT_STAGE
success=$SUCCESS
failure=$FAILURE
EOF
}

finalize() {
  rc=$?
  trap - EXIT HUP INT TERM
  set +e
  if [ "$MOUNTED" = '1' ]; then
    "$HDIUTIL_BIN" detach "$MOUNT" >/dev/null 2>&1 || true
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
      echo 'WEBTOAPP_PRODUCT_QUIT_MAC_DISPOSITION=PASS'
    else
      echo 'WEBTOAPP_PRODUCT_QUIT_MAC_DISPOSITION=SAFE_FAILURE'
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

human_pass() {
  prompt="$1"
  printf '\n%s\n' "$prompt"
  IFS= read -r answer
  case "$answer" in
    PASS|pass|Pass) return 0 ;;
    *) fail "HUMAN_HELD:$answer" ;;
  esac
}

process_alive() {
  pid="$1"
  [ -n "$("$PS_BIN" -p "$pid" -o pid= 2>/dev/null | tr -d '[:space:]')" ]
}

wait_process_gone() {
  pid="$1"
  seconds="$2"
  deadline=$(( $(/bin/date +%s) + seconds ))
  while [ "$(/bin/date +%s)" -lt "$deadline" ]; do
    if ! process_alive "$pid"; then
      return 0
    fi
    /bin/sleep 1
  done
  return 1
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

capture_state() {
  suffix="$1"
  "$CURL_BIN" -fsS --max-time 15 'http://127.0.0.1:18210/api/v1/source-currentness' > "$EVIDENCE/source-currentness.$suffix.json"
  "$CURL_BIN" -fsS --max-time 15 'http://127.0.0.1:18210/api/v1/current-state' > "$EVIDENCE/current-state.$suffix.json"
  "$CURL_BIN" -fsS --max-time 15 'http://127.0.0.1:18210/api/v1/conversation' > "$EVIDENCE/conversation.$suffix.json"
}

extract_exact_lifetime() {
  home="$1"
  [ -f "$SERVICE_RECEIPT" ] || fail "SERVICE_LIFETIME_RECEIPT_MISSING:$SERVICE_RECEIPT"
  "$NODE_BIN" - "$SERVICE_RECEIPT" "$home" "$CANDIDATE_HEAD" "$EVIDENCE" <<'NODE'
const fs=require('node:fs');
const path=require('node:path');
const [receiptPath,home,head,evidence]=process.argv.slice(2);
const service=JSON.parse(fs.readFileSync(receiptPath,'utf8'));
if(service.sourceSha!==head) throw new Error('SERVICE_RECEIPT_SOURCE:'+service.sourceSha);
if(!service.launchd || service.launchd.label!=='com.vexlife.prod.service') throw new Error('SERVICE_LABEL_INVALID');
if(!/^gui\/\d+$/.test(service.launchd.domainTarget||'')) throw new Error('SERVICE_DOMAIN_INVALID:'+service.launchd.domainTarget);
if(!(Number(service.launchd.pid)>0)) throw new Error('SERVICE_PID_INVALID');
if(!String(service.launchd.programPath||'').endsWith('/bin/vexlife-prod-service')) throw new Error('SERVICE_PROGRAM_INVALID');
const args=service.launchd.programArguments||[];
if(!args.includes('--source-sha') || !args.includes(head) || !args.includes('--home') || !args.includes(home)) {
  throw new Error('SERVICE_ARGV_NOT_EXACT');
}
const source=JSON.parse(fs.readFileSync(path.join(evidence,'source-currentness.before-stop.json'),'utf8'));
if(source.sourceSha!==head || Number(source.processPid)!==Number(service.launchd.pid)) {
  throw new Error('SERVICE_ENDPOINT_OWNERSHIP_MISMATCH');
}
const runtimeCandidates=[
  path.join(home,'runtime/vexlife-prod/heretic-persistent-runtime-owner-receipt.json'),
  path.join(home,'runtime/heretic/persistent-runtime-owner-receipt.json')
];
let runtimePath=null, runtime=null;
for(const p of runtimeCandidates){
  if(fs.existsSync(p)){
    const x=JSON.parse(fs.readFileSync(p,'utf8'));
    const pid=Number(x?.runtime?.pid);
    if(pid>0){ runtimePath=p; runtime=x; break; }
  }
}
if(!runtime) throw new Error('RUNTIME_RECEIPT_MISSING');
fs.writeFileSync(path.join(evidence,'service-lifetime.before-stop.json'),JSON.stringify(service,null,2)+'\n');
fs.writeFileSync(path.join(evidence,'runtime-receipt.before-stop.json'),JSON.stringify(runtime,null,2)+'\n');
fs.writeFileSync(path.join(evidence,'service-pid.txt'),String(service.launchd.pid)+'\n');
fs.writeFileSync(path.join(evidence,'service-domain.txt'),service.launchd.domainTarget+'\n');
fs.writeFileSync(path.join(evidence,'service-label.txt'),service.launchd.label+'\n');
fs.writeFileSync(path.join(evidence,'runtime-pid.txt'),String(runtime.runtime.pid)+'\n');
fs.writeFileSync(path.join(evidence,'runtime-receipt-path.txt'),runtimePath+'\n');
NODE
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

"$GH_BIN" repo clone "$REPOSITORY" "$REPO_ROOT" -- --no-checkout --origin origin >/dev/null
"$GIT_BIN" -C "$REPO_ROOT" fetch --no-tags origin "$CANDIDATE_BRANCH" "$BASE_BRANCH" >/dev/null
BRANCH_HEAD="$("$GIT_BIN" -C "$REPO_ROOT" rev-parse "refs/remotes/origin/$CANDIDATE_BRANCH")"
BASE_REMOTE="$("$GIT_BIN" -C "$REPO_ROOT" rev-parse "refs/remotes/origin/$BASE_BRANCH")"
[ "$BRANCH_HEAD" = "$CANDIDATE_HEAD" ] || fail "CANDIDATE_MOVED:$BRANCH_HEAD"
[ "$BASE_REMOTE" = "$BASE_HEAD" ] || fail "BASE_338_MOVED:$BASE_REMOTE"
"$GIT_BIN" -C "$REPO_ROOT" checkout --detach "$CANDIDATE_HEAD" >/dev/null
HEAD="$("$GIT_BIN" -C "$REPO_ROOT" rev-parse HEAD)"
TREE="$("$GIT_BIN" -C "$REPO_ROOT" rev-parse 'HEAD^{tree}')"
[ "$HEAD" = "$CANDIDATE_HEAD" ] || fail "HEAD_MISMATCH:$HEAD"
[ "$TREE" = "$CANDIDATE_TREE" ] || fail "TREE_MISMATCH:$TREE"
[ -z "$("$GIT_BIN" -C "$REPO_ROOT" status --porcelain=v1 -uall)" ] || fail 'SOURCE_DIRTY'
"$GIT_BIN" -C "$REPO_ROOT" merge-base --is-ancestor "$BASE_HEAD" "$CANDIDATE_HEAD" || fail 'BASE_NOT_ANCESTOR'
printf '%s\n' "$HEAD" > "$EVIDENCE/source-head.txt"
printf '%s\n' "$TREE" > "$EVIDENCE/source-tree.txt"
printf '%s\n' "$BASE_REMOTE" > "$EVIDENCE/observed-base.txt"

set_stage JVM_PROOF
export GRADLE_OPTS='-Dorg.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m -Dorg.gradle.workers.max=1 -Dkotlin.compiler.execution.strategy=in-process'
export KOTLIN_DAEMON_JVMARGS='-Xmx4096m'
(
  cd "$REPO_ROOT"
  ./gradlew --no-daemon --max-workers=1 jvmTest
) > "$EVIDENCE/jvm-test.log" 2>&1 || {
  cat "$EVIDENCE/jvm-test.log"
  fail 'JVM_TEST_FAILED'
}
tail -n 120 "$EVIDENCE/jvm-test.log"

set_stage FORM_EXACT_DMG
(
  cd "$REPO_ROOT"
  ./gradlew --no-daemon --max-workers=1 -PvexlifeAppVersion=0.0.0 archiveVexLifeProdMacApplication
) > "$EVIDENCE/dmg-formation.log" 2>&1 || {
  cat "$EVIDENCE/dmg-formation.log"
  fail 'DMG_FORMATION_FAILED'
}
tail -n 160 "$EVIDENCE/dmg-formation.log"

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
"$SHASUM_BIN" -a 256 "$DMG" > "$EVIDENCE/dmg-sha256.txt"

set_stage INSTALL_ORDINARY_PRODUCT
mkdir -p "$MOUNT"
"$HDIUTIL_BIN" attach "$DMG" -readonly -nobrowse -mountpoint "$MOUNT" > "$EVIDENCE/dmg-attach.txt"
MOUNTED=1
"$OPEN_BIN" "$MOUNT"
cat <<'HUMAN'
============================================================
VEXLIFE WEBTOAPP PRODUCT-QUIT MAC PROOF

1. Drag VexLife.app from this exact DMG to Applications and replace the
   prior app if Finder asks.
2. Do not kill Java, Python, mlx, whisper, launchd, or any PID.
3. Do not clear quarantine with Terminal.
4. Return here after the copy finishes and press Enter.
============================================================
HUMAN
IFS= read -r _
[ -d "$INSTALLED_APP" ] || fail 'INSTALLED_APP_NOT_FOUND'
"$HDIUTIL_BIN" detach "$MOUNT" >/dev/null
MOUNTED=0

set_stage OPEN_EXACT_PRODUCT
"$OPEN_BIN" "$INSTALLED_APP"
cat <<'HUMAN'
============================================================
OPEN THE EXACT PRODUCT

Use the ordinary VexLife UI.
- If macOS blocks the unsigned app, use the normal Privacy & Security
  -> Open Anyway decision.
- If VexLife offers Start / Update & Restart for your existing Home,
  use the normal Product control.
- Do not switch or reacquire a model just for this proof.
============================================================
HUMAN
human_pass 'Type PASS when the workspace is open and usable, or HELD with a short reason:'
wait_exact_source 300 || fail 'EXACT_SOURCE_NOT_REACHABLE'
capture_state before-stop

BASE_HOME="$("$NODE_BIN" -e '
  const fs=require("node:fs");
  const x=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));
  if(!x.homePath) process.exit(2);
  process.stdout.write(x.homePath);
' "$EVIDENCE/current-state.before-stop.json")"
printf '%s\n' "$BASE_HOME" > "$EVIDENCE/home-path.txt"

extract_exact_lifetime "$BASE_HOME"
SERVICE_PID="$(tr -d '[:space:]' < "$EVIDENCE/service-pid.txt")"
RUNTIME_PID="$(tr -d '[:space:]' < "$EVIDENCE/runtime-pid.txt")"
SERVICE_DOMAIN="$(tr -d '\r\n' < "$EVIDENCE/service-domain.txt")"
SERVICE_LABEL="$(tr -d '\r\n' < "$EVIDENCE/service-label.txt")"
SERVICE_TARGET="$SERVICE_DOMAIN/$SERVICE_LABEL"

process_alive "$SERVICE_PID" || fail "SERVICE_PID_NOT_LIVE:$SERVICE_PID"
process_alive "$RUNTIME_PID" || fail "RUNTIME_PID_NOT_LIVE:$RUNTIME_PID"
"$PS_BIN" -p "$SERVICE_PID" -o pid=,ppid=,pgid=,rss=,command= > "$EVIDENCE/service-process.before-stop.txt"
"$PS_BIN" -p "$RUNTIME_PID" -o pid=,ppid=,pgid=,rss=,command= > "$EVIDENCE/runtime-process.before-stop.txt"
"$LAUNCHCTL_BIN" print "$SERVICE_TARGET" > "$EVIDENCE/launchctl.before-stop.txt" 2>&1 || fail 'EXACT_SERVICE_COORDINATE_NOT_LOADED'

set_stage EXACT_SERVICE_TERMINATION
cat <<HUMAN
============================================================
EXACT PRODUCT SERVICE TERMINATION

The harness has now proven:
- Product source = $CANDIDATE_HEAD
- exact service PID = $SERVICE_PID
- exact runtime PID = $RUNTIME_PID
- exact launchd coordinate = $SERVICE_TARGET

It will now boot out ONLY that exact receipt-owned VexLife service coordinate.
It will NOT signal/kill the runtime PID directly.

Press Enter to perform that exact lifecycle pressure step.
============================================================
HUMAN
IFS= read -r _

set +e
"$LAUNCHCTL_BIN" bootout "$SERVICE_TARGET" > "$EVIDENCE/launchctl-bootout.txt" 2>&1
BOOTOUT_STATUS=$?
set -e
printf '%s\n' "$BOOTOUT_STATUS" > "$EVIDENCE/launchctl-bootout-status.txt"
[ "$BOOTOUT_STATUS" = '0' ] || fail "EXACT_SERVICE_BOOTOUT_FAILED:$BOOTOUT_STATUS"

wait_process_gone "$SERVICE_PID" 120 || fail "SERVICE_PROCESS_REMAINED_LIVE:$SERVICE_PID"
wait_process_gone "$RUNTIME_PID" 120 || fail "RUNTIME_PROCESS_LEAKED_AFTER_SERVICE_TERMINATION:$RUNTIME_PID"

if "$CURL_BIN" -fsS --max-time 3 'http://127.0.0.1:18210/api/v1/source-currentness' > "$EVIDENCE/source-currentness.after-stop.json" 2>/dev/null; then
  fail 'PRODUCT_ENDPOINT_STILL_RESPONDED_AFTER_EXACT_SERVICE_TERMINATION'
fi

{
  echo "servicePid=$SERVICE_PID"
  echo "serviceGone=yes"
  echo "runtimePid=$RUNTIME_PID"
  echo "runtimeGone=yes"
  echo "runtimeWasNotDirectlyKilled=yes"
  echo "serviceTarget=$SERVICE_TARGET"
} > "$EVIDENCE/termination-proof.txt"

set_stage REOPEN_SAME_PRODUCT
"$OPEN_BIN" "$INSTALLED_APP"
cat <<'HUMAN'
============================================================
REOPEN AFTER EXACT TERMINATION

Use the ordinary VexLife Start/repair control if the app offers it.
Do not recreate Home and do not reacquire/switch models just for proof.

Return here when the workspace is usable again.
============================================================
HUMAN
human_pass 'Type PASS when reopened with the same Home/Conversation, or HELD with a short reason:'
wait_exact_source 300 || fail 'REOPEN_SOURCE_NOT_REACHABLE'
capture_state reopened

REOPEN_HOME="$("$NODE_BIN" -e '
  const fs=require("node:fs");
  const x=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));
  if(!x.homePath) process.exit(2);
  process.stdout.write(x.homePath);
' "$EVIDENCE/current-state.reopened.json")"
[ "$REOPEN_HOME" = "$BASE_HOME" ] || fail "HOME_CHANGED_AFTER_REOPEN:$REOPEN_HOME"
process_alive "$SERVICE_PID" && fail "OLD_SERVICE_PID_REVIVED:$SERVICE_PID"
process_alive "$RUNTIME_PID" && fail "OLD_RUNTIME_PID_REVIVED:$RUNTIME_PID"

cat > "$EVIDENCE/human-confirmation.txt" <<EOF
exactCandidateInstalled=yes
workspaceUsableBeforeTermination=yes
exactOwnedServiceTerminationPerformed=yes
runtimeWasNotDirectlyKilled=yes
servicePidGone=yes
runtimePidGone=yes
sameHomeAfterReopen=yes
priorConversationVisibleAfterReopen=yes
candidateHead=$CANDIDATE_HEAD
homePath=$BASE_HOME
EOF

set_stage COMPLETE
SUCCESS=1
echo
echo 'VexLife WebToApp Product Quit Mac A001 completed.'
