#!/bin/bash
set -Eeuo pipefail

CANDIDATE_HEAD='8461ee40515f2379345ff50c0f9733b580268a7b'
CANDIDATE_TREE='622db1b7e9ecd878af7067974ab874dc7850d2cd'
CANDIDATE_BRANCH='VXG-111826-chatgpt-merge-final-v2-a001'
EXPECTED_MAIN='98a22ede8d37b3b8868eb42f4e7a9a1d612b97d0'
EXPECTED_DMG_SHA256='ce929e9ae7cabf6c13d08bc18f3f1c5afe0d6f57bcd4dadbaf648a3cfe4d1a61'
REPOSITORY='VexGPT/VexLife-Prod'
ATTEMPT='V2-MAC-LIVED-A001'
RETURN_STEM='RETURN-VEXLIFE-MERGE-FINAL-V2-MAC-LIVED-A001'
HEAD12="$(printf '%s' "$CANDIDATE_HEAD" | cut -c1-12)"

DOWNLOADS="$HOME/Downloads"
ARTIFACT_DIR="$DOWNLOADS/VexLife-Current-Main-Proof-$HEAD12"
DMG="$ARTIFACT_DIR/VexLife-macOS-AppleSilicon.dmg"
RECEIPT="$ARTIFACT_DIR/VexLife-macOS-AppleSilicon.receipt.json"

TMPBASE="$(getconf DARWIN_USER_TEMP_DIR 2>/dev/null || printf '/tmp/')"
BOOTSTRAP="$TMPBASE/vexlife-v2-mac-lived-$ATTEMPT-$$"
EVIDENCE="$BOOTSTRAP/evidence"
MOUNT="$BOOTSTRAP/dmg-mount"
RUN_APP="$MOUNT/VexLife.app"
SUCCESS=0
TURN_PROOF='NOT_REACHED'
REOPEN_PROOF='NOT_REACHED'
LANGUAGE_PROJECTION='NOT_REACHED'
MODEL_PROJECTION='NOT_REACHED'
STARTED_AT="$(date -u '+%Y-%m-%dT%H:%M:%SZ')"

mkdir -p "$EVIDENCE" "$MOUNT" "$DOWNLOADS"
exec > >(tee -a "$EVIDENCE/run.log") 2>&1

write_summary() {
  cat > "$EVIDENCE/summary.txt" <<EOF
attempt=$ATTEMPT
candidateHead=$CANDIDATE_HEAD
candidateTree=$CANDIDATE_TREE
expectedMain=$EXPECTED_MAIN
expectedDmgSha256=$EXPECTED_DMG_SHA256
startedAt=$STARTED_AT
turnProof=$TURN_PROOF
reopenProof=$REOPEN_PROOF
languageProjection=$LANGUAGE_PROJECTION
modelProjection=$MODEL_PROJECTION
success=$SUCCESS
EOF
}

finalize() {
  rc=$?
  trap - EXIT HUP INT TERM
  set +e
  if mount | grep -F " on $MOUNT " >/dev/null 2>&1; then
    /usr/bin/hdiutil detach "$MOUNT" >/dev/null 2>&1 || true
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
    if [ "$SUCCESS" = '1' ]; then echo 'MAC_LIVED_DISPOSITION=PASS'; else echo 'MAC_LIVED_DISPOSITION=SAFE_FAILURE'; fi
    echo "RETURN_ZIP=$final"
    echo "RETURN_SHA256=$digest"
    echo '============================================================'
  fi
  exit "$rc"
}
trap finalize EXIT
trap 'exit 130' HUP INT TERM

fail() { echo "FAIL:$*" >&2; return 1; }

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
  curl -fsS 'http://127.0.0.1:18210/api/v1/diagnostics/export' -o "$EVIDENCE/vexlife-debug.$suffix.zip" || true
  launchctl print "gui/$(id -u)/com.vexlife.prod.service" > "$EVIDENCE/launchctl.$suffix.txt" 2>&1 || true
}

json_raw() {
  file="$1"
  key="$2"
  /usr/bin/plutil -extract "$key" raw -o - "$file" 2>/dev/null || true
}

echo 'VexLife merge-final V2 Mac lived sanity'
echo "Candidate: $CANDIDATE_HEAD"
echo "Tree:      $CANDIDATE_TREE"

[ "$(uname -s)" = 'Darwin' ] || fail 'MACOS_REQUIRED'
case "$(uname -m)" in arm64|aarch64) ;; *) fail "APPLE_SILICON_REQUIRED:$(uname -m)" ;; esac
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
printf '%s\n' "$OBSERVED_DMG_SHA" > "$EVIDENCE/observed-dmg-sha256.txt"
[ "$OBSERVED_DMG_SHA" = "$EXPECTED_DMG_SHA256" ] || fail "A009_DMG_SHA_MISMATCH:$OBSERVED_DMG_SHA"

RECEIPT_SOURCE="$(json_raw "$RECEIPT" sourceSha)"
[ "$RECEIPT_SOURCE" = "$CANDIDATE_HEAD" ] || fail "A009_RECEIPT_SOURCE_MISMATCH:$RECEIPT_SOURCE"

BEFORE_SERVICE='ABSENT'
BEFORE_HOME=''
if curl -fsS 'http://127.0.0.1:18210/api/v1/source-currentness' -o "$EVIDENCE/source-currentness.before.json" 2>/dev/null; then
  BEFORE_SERVICE='PRESENT'
  curl -fsS 'http://127.0.0.1:18210/api/v1/current-state' -o "$EVIDENCE/current-state.before.json" || true
  BEFORE_HOME="$(json_raw "$EVIDENCE/current-state.before.json" homePath)"
fi
printf '%s\n' "$BEFORE_SERVICE" > "$EVIDENCE/before-service-state.txt"
printf '%s\n' "$BEFORE_HOME" > "$EVIDENCE/home-path.before.txt"

if [ -z "$BEFORE_HOME" ] && [ -d "$HOME/.vexlife" ]; then
  BEFORE_HOME="$HOME/.vexlife"
  printf '%s\n' "$BEFORE_HOME" > "$EVIDENCE/home-path.before.txt"
fi

if pgrep -x VexLife >/dev/null 2>&1; then
  echo
  echo 'A VexLife native app is already open.'
  echo 'Quit the VexLife app/window normally. Do not kill Java, Python, launchd, or model processes manually.'
  read -r -p 'Press Enter after the VexLife app itself is closed: ' _
fi

/usr/bin/hdiutil attach "$DMG" -readonly -nobrowse -mountpoint "$MOUNT" >/dev/null
[ -d "$RUN_APP" ] || fail 'DMG_APP_MISSING'
/usr/bin/codesign --verify --strict "$RUN_APP"

echo
echo 'Launching the exact A009 VexLife app from the proved DMG...'
/usr/bin/open "$RUN_APP"

cat <<'HUMAN'

====================== MAC LIVED CHECK ======================
Use the native VexLife entry normally.

If it offers Open / Update / Repair / Start, choose the truthful action needed
to reach the Product. Preserve your existing Vex Home.

If it unexpectedly asks you to download a new LLM/model, do NOT approve that
download for this sanity check.

When the browser Product is open and usable, return here.
============================================================
HUMAN

read -r -p 'Type PASS when VexLife is open, or HELD if it requested an unexpected new model download / showed an error: ' ENTRY
[ "$ENTRY" = 'PASS' ] || fail "HUMAN_ENTRY_HELD:$ENTRY"

wait_for_service
capture 'first'

OBSERVED_SOURCE="$(json_raw "$EVIDENCE/source-currentness.first.json" sourceSha)"
[ "$OBSERVED_SOURCE" = "$CANDIDATE_HEAD" ] || fail "SOURCE_CURRENTNESS_MISMATCH:$OBSERVED_SOURCE"

FIRST_HOME="$(json_raw "$EVIDENCE/current-state.first.json" homePath)"
[ -n "$FIRST_HOME" ] || fail 'CURRENT_STATE_HOME_MISSING'
printf '%s\n' "$FIRST_HOME" > "$EVIDENCE/home-path.first.txt"
if [ -n "$BEFORE_HOME" ]; then
  [ "$FIRST_HOME" = "$BEFORE_HOME" ] || fail "HOME_CHANGED:before=$BEFORE_HOME after=$FIRST_HOME"
fi

grep -q '"code":"zh-Hans"' "$EVIDENCE/settings.first.json" || fail 'ZH_HANS_SETTING_NOT_PROJECTED'
grep -q '"code":"zh-Hant"' "$EVIDENCE/settings.first.json" || fail 'ZH_HANT_SETTING_NOT_PROJECTED'
grep -q '"composerOutputChoices"' "$EVIDENCE/local-asr.first.json" || fail 'COMPOSER_OUTPUT_CHOICES_NOT_PROJECTED'
grep -q '"mode":"AS_SPOKEN"' "$EVIDENCE/local-asr.first.json" || fail 'AS_SPOKEN_NOT_PROJECTED'
grep -q '"mode":"TRANSLATE_TO_ENGLISH"' "$EVIDENCE/local-asr.first.json" || fail 'TRANSLATE_TO_ENGLISH_NOT_PROJECTED'
LANGUAGE_PROJECTION='CURRENT_LANGUAGE_AND_COMPOSER_OUTPUT_PRESENT'

SERVING_HANDLE="$(json_raw "$EVIDENCE/models.first.json" servingHandle)"
[ -n "$SERVING_HANDLE" ] && [ "$SERVING_HANDLE" != 'null' ] || fail 'SERVING_MODEL_NOT_PROJECTED'
SWITCH_PENDING="$(json_raw "$EVIDENCE/models.first.json" switchPending)"
[ "$SWITCH_PENDING" = 'false' ] || fail "MODEL_SWITCH_PENDING:$SWITCH_PENDING"
printf '%s\n' "$SERVING_HANDLE" > "$EVIDENCE/serving-model.first.txt"
MODEL_PROJECTION='SERVING_MODEL_CURRENT_AND_NO_SWITCH_PENDING'

CHRON="$FIRST_HOME/runtime/vexlife-prod/logs/chronology/chronology.jsonl"
BASELINE_LINES=0
if [ -f "$CHRON" ]; then
  BASELINE_LINES="$(wc -l < "$CHRON" | tr -d ' ')"
fi
printf '%s\n' "$BASELINE_LINES" > "$EVIDENCE/chronology-baseline-lines.txt"

cat <<'TURN'

====================== REAL TURN CHECK ======================
Send ONE ordinary message to Vex in the current Product and wait for a complete
reply. No special prompt is required. Do not switch models during this check.

When the reply is complete, return here.
============================================================
TURN

read -r -p 'Type PASS after one real Conversation turn completed, or HELD if it failed: ' TURN_RESULT
[ "$TURN_RESULT" = 'PASS' ] || fail "HUMAN_TURN_HELD:$TURN_RESULT"

sleep 1
if [ -f "$CHRON" ]; then
  start_line=$((BASELINE_LINES + 1))
  tail -n "+$start_line" "$CHRON" > "$EVIDENCE/chronology-new.jsonl" 2>/dev/null || true
fi
if [ -f "$EVIDENCE/chronology-new.jsonl" ] && grep -q '"fact":"TURN_COMPLETED"' "$EVIDENCE/chronology-new.jsonl"; then
  TURN_PROOF='TURN_COMPLETED_OBSERVED'
else
  TURN_PROOF='HUMAN_COMPLETED_BUT_CHRONOLOGY_FACT_NOT_FOUND'
  fail 'TURN_COMPLETED_FACT_NOT_OBSERVED'
fi

capture 'after-turn'

echo
echo 'Quit/close the VexLife native app/window normally. Leave the Home and service/runtime processes alone.'
read -r -p 'Press Enter after the native VexLife app is closed and you are ready to reopen: ' _

/usr/bin/open "$RUN_APP"

cat <<'REOPEN'

======================= REOPEN CHECK =======================
Let the same exact VexLife app reopen the Product.
Do not change Home or model.
When the Product is usable again, return here.
============================================================
REOPEN

read -r -p 'Type PASS when VexLife is usable after reopen, or HELD if it failed: ' REOPEN_RESULT
[ "$REOPEN_RESULT" = 'PASS' ] || fail "HUMAN_REOPEN_HELD:$REOPEN_RESULT"

wait_for_service
capture 'reopen'

REOPEN_SOURCE="$(json_raw "$EVIDENCE/source-currentness.reopen.json" sourceSha)"
[ "$REOPEN_SOURCE" = "$CANDIDATE_HEAD" ] || fail "REOPEN_SOURCE_MISMATCH:$REOPEN_SOURCE"
SECOND_HOME="$(json_raw "$EVIDENCE/current-state.reopen.json" homePath)"
[ "$SECOND_HOME" = "$FIRST_HOME" ] || fail "REOPEN_HOME_MISMATCH:first=$FIRST_HOME reopen=$SECOND_HOME"

grep -q '"code":"zh-Hans"' "$EVIDENCE/settings.reopen.json" || fail 'REOPEN_ZH_HANS_NOT_PROJECTED'
grep -q '"composerOutputChoices"' "$EVIDENCE/local-asr.reopen.json" || fail 'REOPEN_COMPOSER_OUTPUT_NOT_PROJECTED'

REOPEN_SERVING="$(json_raw "$EVIDENCE/models.reopen.json" servingHandle)"
[ "$REOPEN_SERVING" = "$SERVING_HANDLE" ] || fail "REOPEN_SERVING_MODEL_CHANGED:first=$SERVING_HANDLE reopen=$REOPEN_SERVING"
REOPEN_PENDING="$(json_raw "$EVIDENCE/models.reopen.json" switchPending)"
[ "$REOPEN_PENDING" = 'false' ] || fail "REOPEN_MODEL_SWITCH_PENDING:$REOPEN_PENDING"

REOPEN_PROOF='EXACT_SOURCE_SAME_HOME_SAME_SERVING_MODEL'
cat > "$EVIDENCE/human-confirmation.txt" <<EOF
entryUsable=yes
oneRealConversationTurnCompleted=yes
reopenUsable=yes
sameHome=$FIRST_HOME
sameServingModel=$SERVING_HANDLE
candidateSource=$CANDIDATE_HEAD
languageComposerProjection=present
EOF

SUCCESS=1
echo
echo 'Mac lived sanity completed.'
echo 'RETURN ZIP will be formed now.'
