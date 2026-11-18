#!/bin/bash
set -Eeuo pipefail

CANDIDATE_HEAD='6035038863229d3e6ad88ea8082b1139391a3585'
CANDIDATE_TREE='49710bd60abdc6d3c2245ae9af8fa1f4fa751a61'
CANDIDATE_BRANCH='VXG-111826-chatgpt-shared-mac-windows-reconcile-a001'
REPOSITORY='VexGPT/VexLife-Prod'
ATTEMPT='A002'
RETURN_STEM='RETURN-VEXLIFE-SHARED-MAC-RECONCILE-A002'
HEAD12="$(printf '%s' "$CANDIDATE_HEAD" | cut -c1-12)"

TMPBASE="$(getconf DARWIN_USER_TEMP_DIR 2>/dev/null || printf '/tmp/')"
BOOTSTRAP="$TMPBASE/vexlife-mac-reconcile-$ATTEMPT-$$"
REPO_ROOT="$BOOTSTRAP/repo"
EVIDENCE="$BOOTSTRAP/evidence"
DOWNLOADS="$HOME/Downloads"
ARTIFACT_DIR="$DOWNLOADS/VexLife-Mac-Reconcile-$HEAD12"
RUN_LOG="$EVIDENCE/run.log"
SUCCESS=0
TURN_PROOF='NOT_REACHED'
REOPEN_PROOF='NOT_REACHED'
STARTED_AT="$(date -u '+%Y-%m-%dT%H:%M:%SZ')"

mkdir -p "$EVIDENCE" "$DOWNLOADS" "$ARTIFACT_DIR"
exec > >(tee -a "$RUN_LOG") 2>&1

write_summary() {
  cat > "$EVIDENCE/summary.txt" <<EOF
attempt=$ATTEMPT
candidateHead=$CANDIDATE_HEAD
candidateTree=$CANDIDATE_TREE
candidateBranch=$CANDIDATE_BRANCH
startedAt=$STARTED_AT
turnProof=$TURN_PROOF
reopenProof=$REOPEN_PROOF
success=$SUCCESS
EOF
}

finalize() {
  rc=$?
  trap - EXIT HUP INT TERM
  set +e
  write_summary
  printf '%s\n' "$rc" > "$EVIDENCE/exit-code.txt"

  (
    cd "$EVIDENCE" || exit 0
    find . -type f ! -name 'SHA256SUMS.txt' -print \
      | LC_ALL=C sort \
      | while IFS= read -r file; do shasum -a 256 "$file"; done \
      > SHA256SUMS.txt 2>/dev/null
  )

  tmpzip="$DOWNLOADS/$RETURN_STEM.zip"
  rm -f "$tmpzip"
  (
    cd "$BOOTSTRAP" || exit 0
    /usr/bin/zip -qry "$tmpzip" evidence
  )

  if [ -f "$tmpzip" ]; then
    digest="$(shasum -a 256 "$tmpzip" | awk '{print $1}')"
    final="$DOWNLOADS/$RETURN_STEM--sha256-$digest.zip"
    mv -f "$tmpzip" "$final"
    echo
    echo "============================================================"
    if [ "$SUCCESS" = "1" ]; then
      echo "MAC_RECONCILE_DISPOSITION=PASS"
    else
      echo "MAC_RECONCILE_DISPOSITION=SAFE_FAILURE"
    fi
    echo "RETURN_ZIP=$final"
    echo "RETURN_SHA256=$digest"
    echo "============================================================"
  else
    echo "RETURN_ZIP_FORMATION_FAILED" >&2
  fi

  rm -rf "$BOOTSTRAP"
  exit "$rc"
}

trap finalize EXIT
trap 'exit 130' HUP INT TERM

fail() {
  echo "FAIL:$*" >&2
  return 1
}

run_logged() {
  label="$1"
  shift
  log="$EVIDENCE/$label.log"
  echo
  echo ">>> $label"
  set +e
  "$@" > "$log" 2>&1
  rc=$?
  set -e
  cat "$log"
  if [ "$rc" -ne 0 ]; then
    fail "$label:exit=$rc"
  fi
}

capture_service() {
  suffix="$1"
  curl -fsS 'http://127.0.0.1:18210/api/v1/source-currentness' \
    -o "$EVIDENCE/source-currentness.$suffix.json"
  curl -fsS 'http://127.0.0.1:18210/api/v1/current-state' \
    -o "$EVIDENCE/current-state.$suffix.json"
  curl -fsS 'http://127.0.0.1:18210/api/v1/diagnostics/export' \
    -o "$EVIDENCE/vexlife-debug.$suffix.zip" || true
  launchctl print "gui/$(id -u)/com.vexlife.prod.service" \
    > "$EVIDENCE/launchctl.$suffix.txt" 2>&1 || true

  observed_source="$(/usr/bin/plutil -extract sourceSha raw -o - "$EVIDENCE/source-currentness.$suffix.json" 2>/dev/null || true)"
  if [ -z "$observed_source" ]; then observed_source='NONE'; fi
  [ "$observed_source" = "$CANDIDATE_HEAD" ] \
    || fail "SOURCE_CURRENTNESS_MISMATCH:$suffix:$observed_source"

  observed_home="$(/usr/bin/plutil -extract homePath raw -o - "$EVIDENCE/current-state.$suffix.json" 2>/dev/null || true)"
  [ -n "$observed_home" ] || fail "CURRENT_STATE_HOME_MISSING:$suffix"
  printf '%s\n' "$observed_home" > "$EVIDENCE/home-path.$suffix.txt"
}

wait_for_service() {
  tries=0
  while [ "$tries" -lt 60 ]; do
    if curl -fsS 'http://127.0.0.1:18210/api/v1/source-currentness' >/dev/null 2>&1; then
      return 0
    fi
    tries=$((tries + 1))
    sleep 2
  done
  fail 'PRODUCT_SERVICE_NOT_REACHABLE_AFTER_LIVED_STEP'
}

echo "VexLife shared Mac/Windows reconciliation lived proof"
echo "Candidate: $CANDIDATE_HEAD"
echo "Tree:      $CANDIDATE_TREE"

[ "$(uname -s)" = "Darwin" ] || fail 'MACOS_REQUIRED'
case "$(uname -m)" in
  arm64|aarch64) ;;
  *) fail "APPLE_SILICON_REQUIRED:$(uname -m)" ;;
esac

command -v git >/dev/null 2>&1 || fail 'GIT_REQUIRED'
command -v gh >/dev/null 2>&1 || fail 'GITHUB_CLI_REQUIRED'
command -v curl >/dev/null 2>&1 || fail 'CURL_REQUIRED'
gh auth status -h github.com >/dev/null 2>&1 || fail 'GITHUB_LOGIN_REQUIRED'

echo "macOS=$(sw_vers -productVersion)"
echo "arch=$(uname -m)"
gh --version | head -1
git --version

gh repo clone "$REPOSITORY" "$REPO_ROOT" -- --no-checkout --origin origin
git -C "$REPO_ROOT" fetch --no-tags --depth=64 origin "$CANDIDATE_BRANCH"
OBSERVED_BRANCH_HEAD="$(git -C "$REPO_ROOT" rev-parse FETCH_HEAD)"
[ "$OBSERVED_BRANCH_HEAD" = "$CANDIDATE_HEAD" ] || fail "BRANCH_HEAD_MISMATCH:$OBSERVED_BRANCH_HEAD"

git -C "$REPO_ROOT" checkout --detach "$CANDIDATE_HEAD"

OBSERVED_HEAD="$(git -C "$REPO_ROOT" rev-parse HEAD)"
OBSERVED_TREE="$(git -C "$REPO_ROOT" rev-parse 'HEAD^{tree}')"
[ "$OBSERVED_HEAD" = "$CANDIDATE_HEAD" ] || fail "HEAD_MISMATCH:$OBSERVED_HEAD"
[ "$OBSERVED_TREE" = "$CANDIDATE_TREE" ] || fail "TREE_MISMATCH:$OBSERVED_TREE"
[ -z "$(git -C "$REPO_ROOT" status --porcelain=v1 --untracked-files=all)" ] || fail 'SOURCE_NOT_CLEAN'

printf '%s\n' "$OBSERVED_HEAD" > "$EVIDENCE/source-head.txt"
printf '%s\n' "$OBSERVED_TREE" > "$EVIDENCE/source-tree.txt"

cd "$REPO_ROOT"

run_logged '01-jvm-strings-proof' \
  ./gradlew --no-daemon --max-workers=1 jvmTest checkVexLifeStrings

run_logged '02-js-compile-proof' \
  ./gradlew --no-daemon --max-workers=1 compileKotlinJs

run_logged '03-mac-artifact-formation' \
  ./gradlew --no-daemon --max-workers=1 \
    -PvexlifeAppVersion=0.0.0 archiveVexLifeProdMacApplication

APP_DIR="$REPO_ROOT/build/vexlife-prod-mac-application/$CANDIDATE_HEAD"
APP="$APP_DIR/VexLife.app"
DMG="$APP_DIR/VexLife-macOS-AppleSilicon.dmg"
DMG_SHA="$APP_DIR/VexLife-macOS-AppleSilicon.dmg.sha256"
RECEIPT="$APP_DIR/VexLife-macOS-AppleSilicon.receipt.json"

[ -d "$APP" ] || fail 'MAC_APP_MISSING'
[ -f "$DMG" ] || fail 'MAC_DMG_MISSING'
[ -f "$DMG_SHA" ] || fail 'MAC_DMG_SHA_MISSING'
[ -f "$RECEIPT" ] || fail 'MAC_RECEIPT_MISSING'

run_logged '04-codesign-verify' /usr/bin/codesign --verify --strict "$APP"
run_logged '05-dmg-sha-verify' /bin/bash -c "cd \"$APP_DIR\" && shasum -a 256 -c \"$(basename "$DMG_SHA")\""
run_logged '06-dmg-hdiutil-verify' /usr/bin/hdiutil verify "$DMG"

RECEIPT_SOURCE="$(/usr/bin/plutil -extract sourceSha raw -o - "$RECEIPT" 2>/dev/null || true)"
if [ -z "$RECEIPT_SOURCE" ]; then RECEIPT_SOURCE='NONE'; fi
[ "$RECEIPT_SOURCE" = "$CANDIDATE_HEAD" ] || fail "MAC_RECEIPT_SOURCE_MISMATCH:$RECEIPT_SOURCE"

cp -f "$DMG" "$ARTIFACT_DIR/"
cp -f "$DMG_SHA" "$ARTIFACT_DIR/"
cp -f "$RECEIPT" "$ARTIFACT_DIR/"
shasum -a 256 "$DMG" > "$EVIDENCE/dmg-sha256.txt"
cp -f "$RECEIPT" "$EVIDENCE/VexLife-macOS-AppleSilicon.receipt.json"

DEFAULT_HOME="$HOME/.vexlife"
DEFAULT_CHRON="$DEFAULT_HOME/runtime/vexlife-prod/logs/chronology/chronology.jsonl"
BASELINE_LINES=0
if [ -f "$DEFAULT_CHRON" ]; then
  BASELINE_LINES="$(wc -l < "$DEFAULT_CHRON" | tr -d ' ')"
fi
printf '%s\n' "$BASELINE_LINES" > "$EVIDENCE/chronology-baseline-lines.txt"

if pgrep -x VexLife >/dev/null 2>&1; then
  echo
  echo "An existing VexLife app is open on this Mac."
  echo "Quit that VexLife app/window now. Do not manually kill Java, Python, or launchd processes."
  read -r -p "Press Enter after the VexLife app itself is closed: " _
fi

echo
echo "Launching the exact reconciled VexLife.app..."
/usr/bin/open "$APP"

cat <<'HUMAN'

==================== LIVED MAC CHECK ====================
Use VexLife normally through the native setup window.

Follow whatever the Product truthfully offers (Open / Update / Repair / Start).
When the browser Product is current:
  1. complete ONE real Conversation turn and receive Vex's reply;
  2. do not delete or replace your Vex Home;
  3. return to this Terminal.

No conversation text will be copied into the RETURN bundle.
========================================================
HUMAN

read -r -p "Press Enter after one real Conversation turn has completed: " _
wait_for_service
capture_service 'first'

FIRST_HOME="$(cat "$EVIDENCE/home-path.first.txt")"
CHRON="$FIRST_HOME/runtime/vexlife-prod/logs/chronology/chronology.jsonl"
if [ -f "$CHRON" ]; then
  if [ "$FIRST_HOME" = "$DEFAULT_HOME" ]; then
    start_line=$((BASELINE_LINES + 1))
    tail -n "+$start_line" "$CHRON" > "$EVIDENCE/chronology-new.jsonl" 2>/dev/null || true
  else
    tail -n 200 "$CHRON" > "$EVIDENCE/chronology-new.jsonl" 2>/dev/null || true
  fi
fi

if [ -f "$EVIDENCE/chronology-new.jsonl" ] \
  && grep -q '"fact":"TURN_COMPLETED"' "$EVIDENCE/chronology-new.jsonl"; then
  TURN_PROOF='TURN_COMPLETED_OBSERVED'
else
  TURN_PROOF='TURN_COMPLETED_NOT_OBSERVED'
  fail 'NEW_TURN_COMPLETED_FACT_NOT_OBSERVED'
fi

echo
echo "First turn is source-bound and recorded."
echo "Now quit/close the VexLife native app/window if it is still open."
echo "Leave the Vex Home untouched."
read -r -p "Press Enter when the native VexLife app is closed and ready to reopen: " _

echo "Reopening the same exact built VexLife.app..."
/usr/bin/open "$APP"

cat <<'HUMAN2'

==================== REOPEN CHECK =======================
Confirm the native entry returns against the same Vex Home.
Let it open the Product again.
In VexLife, confirm the prior Conversation is still present in Archives.
Then return to this Terminal.
========================================================
HUMAN2

read -r -p "Press Enter after same-Home / prior-Conversation continuity is visible: " _
wait_for_service
capture_service 'reopen'

SECOND_HOME="$(cat "$EVIDENCE/home-path.reopen.txt")"
[ "$SECOND_HOME" = "$FIRST_HOME" ] || fail "HOME_CONTINUITY_MISMATCH:first=$FIRST_HOME reopen=$SECOND_HOME"

REOPEN_PROOF='SAME_HOME_AND_EXACT_SOURCE_OBSERVED'

if [ -f "$CHRON" ]; then
  tail -n 200 "$CHRON" > "$EVIDENCE/chronology-final-tail.jsonl" 2>/dev/null || true
fi

cat > "$EVIDENCE/lived-human-confirmation.txt" <<EOF
oneRealConversationTurnCompleted=yes
priorConversationVisibleAfterReopen=yes
sameHomePath=$FIRST_HOME
candidateSource=$CANDIDATE_HEAD
EOF

SUCCESS=1
TURN_PROOF='TURN_COMPLETED_OBSERVED'
REOPEN_PROOF='SAME_HOME_EXACT_SOURCE_AND_HUMAN_ARCHIVE_CONTINUITY'
echo
echo "Mac lived reconciliation predicates completed."
echo "The exact DMG proof is retained at:"
echo "  $ARTIFACT_DIR"
echo "RETURN ZIP will be formed now."
