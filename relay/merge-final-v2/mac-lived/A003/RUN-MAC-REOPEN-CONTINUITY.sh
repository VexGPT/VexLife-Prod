#!/bin/bash
set -Eeuo pipefail

CANDIDATE_HEAD='8461ee40515f2379345ff50c0f9733b580268a7b'
CANDIDATE_TREE='622db1b7e9ecd878af7067974ab874dc7850d2cd'
CANDIDATE_BRANCH='VXG-111826-chatgpt-merge-final-v2-a001'
EXPECTED_MAIN='98a22ede8d37b3b8868eb42f4e7a9a1d612b97d0'
EXPECTED_DMG_SHA256='ce929e9ae7cabf6c13d08bc18f3f1c5afe0d6f57bcd4dadbaf648a3cfe4d1a61'
PRIOR_TURN_REF='turn.vexlife.browser-companion.198e8177-bba6-4fbb-abd7-c85a3e3abc79'
REPOSITORY='VexGPT/VexLife-Prod'
ATTEMPT='V2-MAC-LIVED-A003'
RETURN_STEM='RETURN-VEXLIFE-MERGE-FINAL-V2-MAC-LIVED-A003'
HEAD12="$(printf '%s' "$CANDIDATE_HEAD" | cut -c1-12)"

DOWNLOADS="$HOME/Downloads"
ARTIFACT_DIR="$DOWNLOADS/VexLife-Current-Main-Proof-$HEAD12"
DMG="$ARTIFACT_DIR/VexLife-macOS-AppleSilicon.dmg"
RECEIPT="$ARTIFACT_DIR/VexLife-macOS-AppleSilicon.receipt.json"
HOME_PATH="$HOME/.vexlife"
CHRON="$HOME_PATH/runtime/vexlife-prod/logs/chronology/chronology.jsonl"

TMPBASE="$(getconf DARWIN_USER_TEMP_DIR 2>/dev/null || printf '/tmp/')"
BOOTSTRAP="$TMPBASE/vexlife-v2-mac-lived-$ATTEMPT-$$"
EVIDENCE="$BOOTSTRAP/evidence"
MOUNT="$BOOTSTRAP/dmg-mount"
RUN_MOUNT=''
RUN_APP=''
SUCCESS=0
REOPEN_PROOF='NOT_REACHED'
DMG_MOUNT_MODE='NOT_REACHED'
STARTED_AT="$(date -u '+%Y-%m-%dT%H:%M:%SZ')"

mkdir -p "$EVIDENCE" "$MOUNT" "$DOWNLOADS"
exec > >(tee -a "$EVIDENCE/run.log") 2>&1

write_summary() {
  cat > "$EVIDENCE/summary.txt" <<EOF
attempt=$ATTEMPT
candidateHead=$CANDIDATE_HEAD
candidateTree=$CANDIDATE_TREE
expectedMain=$EXPECTED_MAIN
priorTurnRef=$PRIOR_TURN_REF
startedAt=$STARTED_AT
dmgMountMode=$DMG_MOUNT_MODE
reopenProof=$REOPEN_PROOF
success=$SUCCESS
EOF
}

best_effort_detach() {
  mount_point="$1"
  [ -n "$mount_point" ] || return 0
  if mount | grep -F " on $mount_point " >/dev/null 2>&1; then
    if /usr/bin/hdiutil detach "$mount_point" > "$EVIDENCE/dmg-detach.log" 2>&1; then
      printf '%s\n' 'DETACHED' > "$EVIDENCE/dmg-detach-state.txt"
    else
      printf '%s\n' 'DETACH_HELD_NON_SEMANTIC' > "$EVIDENCE/dmg-detach-state.txt"
    fi
  else
    printf '%s\n' 'ALREADY_NOT_MOUNTED' > "$EVIDENCE/dmg-detach-state.txt"
  fi
}

finalize() {
  rc=$?
  trap - EXIT HUP INT TERM
  set +e
  if [ "$SUCCESS" = '1' ]; then
    best_effort_detach "$RUN_MOUNT"
  fi
  write_summary
  printf '%s\n' "$rc" > "$EVIDENCE/exit-code.txt"
  (
    cd "$EVIDENCE" || exit 0
    find . -type f ! -name 'SHA256SUMS.txt' -print | LC_ALL=C sort | while IFS= read -r f; do shasum -a 256 "$f"; done > SHA256SUMS.txt 2>/dev/null
  )
  tmp="$DOWNLOADS/$RETURN_STEM.zip"
  rm -f "$tmp"
  (cd "$BOOTSTRAP" && /usr/bin/zip -qry "$tmp" evidence)
  if [ -f "$tmp" ]; then
    digest="$(shasum -a 256 "$tmp" | awk '{print $1}')"
    final="$DOWNLOADS/$RETURN_STEM--sha256-$digest.zip"
    mv -f "$tmp" "$final"
    echo
    echo '============================================================'
    if [ "$SUCCESS" = '1' ]; then echo 'MAC_REOPEN_DISPOSITION=PASS'; else echo 'MAC_REOPEN_DISPOSITION=SAFE_FAILURE'; fi
    echo "RETURN_ZIP=$final"
    echo "RETURN_SHA256=$digest"
    echo '============================================================'
  fi
  exit "$rc"
}
trap finalize EXIT
trap 'exit 130' HUP INT TERM

fail() { echo "FAIL:$*" >&2; return 1; }

json_raw() {
  file="$1"
  key="$2"
  /usr/bin/plutil -extract "$key" raw -o - "$file" 2>/dev/null || true
}

wait_for_service() {
  tries=0
  while [ "$tries" -lt 90 ]; do
    if curl -fsS 'http://127.0.0.1:18210/api/v1/source-currentness' >/dev/null 2>&1; then
      return 0
    fi
    tries=$((tries + 1))
    sleep 2
  done
  fail 'PRODUCT_SERVICE_NOT_REACHABLE'
}

capture() {
  suffix="$1"
  curl -fsS 'http://127.0.0.1:18210/api/v1/source-currentness' -o "$EVIDENCE/source-currentness.$suffix.json"
  curl -fsS 'http://127.0.0.1:18210/api/v1/current-state' -o "$EVIDENCE/current-state.$suffix.json"
  curl -fsS 'http://127.0.0.1:18210/api/v1/settings' -o "$EVIDENCE/settings.$suffix.json"
  curl -fsS 'http://127.0.0.1:18210/api/v1/local-asr' -o "$EVIDENCE/local-asr.$suffix.json"
  curl -fsS 'http://127.0.0.1:18210/api/v1/models' -o "$EVIDENCE/models.$suffix.json"
  launchctl print "gui/$(id -u)/com.vexlife.prod.service" > "$EVIDENCE/launchctl.$suffix.txt" 2>&1 || true
}

find_existing_exact_mount() {
  info="$EVIDENCE/hdiutil-info.plist"
  /usr/bin/hdiutil info -plist > "$info"
  i=0
  while [ "$i" -lt 32 ]; do
    image_path="$(/usr/libexec/PlistBuddy -c "Print :images:$i:image-path" "$info" 2>/dev/null || true)"
    if [ "$image_path" = "$DMG" ]; then
      j=0
      while [ "$j" -lt 32 ]; do
        mount_point="$(/usr/libexec/PlistBuddy -c "Print :images:$i:system-entities:$j:mount-point" "$info" 2>/dev/null || true)"
        if [ -n "$mount_point" ] && [ -d "$mount_point/VexLife.app" ]; then
          printf '%s\n' "$mount_point"
          return 0
        fi
        j=$((j + 1))
      done
    fi
    i=$((i + 1))
  done
  return 1
}

echo 'VexLife merge-final V2 Mac reopen continuity'
echo "Candidate: $CANDIDATE_HEAD"

[ "$(uname -s)" = 'Darwin' ] || fail 'MACOS_REQUIRED'
command -v gh >/dev/null 2>&1 || fail 'GITHUB_CLI_REQUIRED'
command -v curl >/dev/null 2>&1 || fail 'CURL_REQUIRED'
gh auth status -h github.com >/dev/null 2>&1 || fail 'GITHUB_LOGIN_REQUIRED'

OBSERVED_MAIN="$(gh api "repos/$REPOSITORY/commits/main" --jq '.sha')"
printf '%s\n' "$OBSERVED_MAIN" > "$EVIDENCE/observed-main.txt"
[ "$OBSERVED_MAIN" = "$EXPECTED_MAIN" ] || fail "ORIGIN_MAIN_MOVED:$OBSERVED_MAIN"

OBSERVED_HEAD="$(gh api "repos/$REPOSITORY/branches/$CANDIDATE_BRANCH" --jq '.commit.sha')"
printf '%s\n' "$OBSERVED_HEAD" > "$EVIDENCE/observed-candidate-head.txt"
[ "$OBSERVED_HEAD" = "$CANDIDATE_HEAD" ] || fail "CANDIDATE_MOVED:$OBSERVED_HEAD"

[ -f "$DMG" ] || fail "A009_DMG_MISSING:$DMG"
[ -f "$RECEIPT" ] || fail "A009_RECEIPT_MISSING:$RECEIPT"
OBSERVED_DMG_SHA="$(shasum -a 256 "$DMG" | awk '{print $1}')"
[ "$OBSERVED_DMG_SHA" = "$EXPECTED_DMG_SHA256" ] || fail "A009_DMG_SHA_MISMATCH:$OBSERVED_DMG_SHA"
printf '%s\n' "$OBSERVED_DMG_SHA" > "$EVIDENCE/observed-dmg-sha256.txt"
RECEIPT_SOURCE="$(json_raw "$RECEIPT" sourceSha)"
[ "$RECEIPT_SOURCE" = "$CANDIDATE_HEAD" ] || fail "A009_RECEIPT_SOURCE_MISMATCH:$RECEIPT_SOURCE"

[ -f "$CHRON" ] || fail "CHRONOLOGY_MISSING:$CHRON"
grep -F "$PRIOR_TURN_REF" "$CHRON" > "$EVIDENCE/prior-turn.records.jsonl" 2>/dev/null || true
grep -F '"fact":"TURN_COMPLETED"' "$EVIDENCE/prior-turn.records.jsonl" >/dev/null || fail 'PRIOR_TURN_NOT_COMPLETED'
grep -F '"fact":"WORKING_CONTINUITY_COMMITTED"' "$EVIDENCE/prior-turn.records.jsonl" >/dev/null || fail 'PRIOR_TURN_WORKING_CONTINUITY_NOT_COMMITTED'

wait_for_service
capture 'before-reopen'

SOURCE="$(json_raw "$EVIDENCE/source-currentness.before-reopen.json" sourceSha)"
[ "$SOURCE" = "$CANDIDATE_HEAD" ] || fail "SOURCE_CURRENTNESS_MISMATCH:$SOURCE"
CURRENT_HOME="$(json_raw "$EVIDENCE/current-state.before-reopen.json" homePath)"
[ "$CURRENT_HOME" = "$HOME_PATH" ] || fail "HOME_CHANGED:$CURRENT_HOME"
SERVING="$(json_raw "$EVIDENCE/models.before-reopen.json" servingHandle)"
[ -n "$SERVING" ] && [ "$SERVING" != 'null' ] || fail 'SERVING_MODEL_NOT_PROJECTED'
PENDING="$(json_raw "$EVIDENCE/models.before-reopen.json" switchPending)"
[ "$PENDING" = 'false' ] || fail "MODEL_SWITCH_PENDING:$PENDING"
printf '%s\n' "$SERVING" > "$EVIDENCE/serving-model.before-reopen.txt"

grep -q '"code":"zh-Hans"' "$EVIDENCE/settings.before-reopen.json" || fail 'ZH_HANS_SETTING_NOT_PROJECTED'
grep -q '"code":"zh-Hant"' "$EVIDENCE/settings.before-reopen.json" || fail 'ZH_HANT_SETTING_NOT_PROJECTED'
grep -q '"composerOutputChoices"' "$EVIDENCE/local-asr.before-reopen.json" || fail 'COMPOSER_OUTPUT_NOT_PROJECTED'

if pgrep -x VexLife >/dev/null 2>&1; then
  echo
  echo 'Quit/close the currently open VexLife native app/window normally.'
  echo 'Leave the Vex Home and background service/runtime alone.'
  read -r -p 'Press Enter after the native app is closed: ' _
fi

RUN_MOUNT="$(find_existing_exact_mount || true)"
if [ -n "$RUN_MOUNT" ]; then
  DMG_MOUNT_MODE='REUSED_EXISTING_EXACT_MOUNT'
else
  set +e
  /usr/bin/hdiutil attach "$DMG" -readonly -nobrowse -mountpoint "$MOUNT" > "$EVIDENCE/dmg-attach.log" 2>&1
  ATTACH_RC=$?
  set -e
  if [ "$ATTACH_RC" -eq 0 ]; then
    RUN_MOUNT="$MOUNT"
    DMG_MOUNT_MODE='ATTACHED_FRESH'
  else
    RUN_MOUNT="$(find_existing_exact_mount || true)"
    [ -n "$RUN_MOUNT" ] || {
      cat "$EVIDENCE/dmg-attach.log" >&2 || true
      fail "EXACT_DMG_NOT_USABLE:attach=$ATTACH_RC"
    }
    DMG_MOUNT_MODE='REUSED_EXISTING_EXACT_MOUNT_AFTER_RESOURCE_BUSY'
  fi
fi

RUN_APP="$RUN_MOUNT/VexLife.app"
[ -d "$RUN_APP" ] || fail "MOUNTED_APP_MISSING:$RUN_APP"
/usr/bin/codesign --verify --strict "$RUN_APP"
printf '%s\n' "$RUN_MOUNT" > "$EVIDENCE/dmg-mount-path.txt"
printf '%s\n' "$DMG_MOUNT_MODE" > "$EVIDENCE/dmg-mount-mode.txt"

echo
echo 'Reopening the same exact A009 VexLife app...'
/usr/bin/open "$RUN_APP"

cat <<'HUMAN'

======================= REOPEN CHECK =======================
Let VexLife return to the Product normally.

Please confirm:
  - VexLife is usable;
  - the prior Conversation containing the completed A001 turn is visible in Archives;
  - you did not need to choose a different Home or model.

Then return here.
============================================================
HUMAN

read -r -p 'Type PASS when the prior Conversation is visible and Product is usable, or HELD if not: ' RESULT
[ "$RESULT" = 'PASS' ] || fail "HUMAN_REOPEN_HELD:$RESULT"

wait_for_service
capture 'reopen'

REOPEN_SOURCE="$(json_raw "$EVIDENCE/source-currentness.reopen.json" sourceSha)"
[ "$REOPEN_SOURCE" = "$CANDIDATE_HEAD" ] || fail "REOPEN_SOURCE_MISMATCH:$REOPEN_SOURCE"
REOPEN_HOME="$(json_raw "$EVIDENCE/current-state.reopen.json" homePath)"
[ "$REOPEN_HOME" = "$CURRENT_HOME" ] || fail "REOPEN_HOME_MISMATCH:$REOPEN_HOME"
REOPEN_SERVING="$(json_raw "$EVIDENCE/models.reopen.json" servingHandle)"
[ "$REOPEN_SERVING" = "$SERVING" ] || fail "REOPEN_SERVING_MODEL_CHANGED:first=$SERVING reopen=$REOPEN_SERVING"
REOPEN_PENDING="$(json_raw "$EVIDENCE/models.reopen.json" switchPending)"
[ "$REOPEN_PENDING" = 'false' ] || fail "REOPEN_MODEL_SWITCH_PENDING:$REOPEN_PENDING"

grep -q '"code":"zh-Hans"' "$EVIDENCE/settings.reopen.json" || fail 'REOPEN_ZH_HANS_NOT_PROJECTED'
grep -q '"code":"zh-Hant"' "$EVIDENCE/settings.reopen.json" || fail 'REOPEN_ZH_HANT_NOT_PROJECTED'
grep -q '"composerOutputChoices"' "$EVIDENCE/local-asr.reopen.json" || fail 'REOPEN_COMPOSER_OUTPUT_NOT_PROJECTED'

REOPEN_PROOF='EXACT_SOURCE_SAME_HOME_SAME_MODEL_ARCHIVE_VISIBLE'
cat > "$EVIDENCE/human-confirmation.txt" <<EOF
priorTurnRef=$PRIOR_TURN_REF
priorTurnCompleted=yes
priorWorkingContinuityCommitted=yes
priorConversationVisibleAfterReopen=yes
sameHome=$CURRENT_HOME
sameServingModel=$SERVING
candidateSource=$CANDIDATE_HEAD
EOF

echo
echo 'Final observations are captured.'
echo 'Quit/close the native VexLife app/window normally.'
read -r -p 'Press Enter after the native app is closed: ' _

SUCCESS=1
echo
echo 'Mac reopen continuity completed.'
