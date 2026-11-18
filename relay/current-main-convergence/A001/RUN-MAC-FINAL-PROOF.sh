#!/bin/bash
set -Eeuo pipefail

CANDIDATE_HEAD='51858054cee336da66ba99d564aa172c2d30b266'
CANDIDATE_TREE='83eef175b3bfe5ad03517e84322a4758151c6e7b'
CANDIDATE_BRANCH='VXG-111826-chatgpt-windows-current-main-integration-a001'
EXPECTED_BASE='d2b241b903e34e0e14b3c561ffcfa77ae367662a'
REPOSITORY='VexGPT/VexLife-Prod'
ATTEMPT='A001'
RETURN_STEM='RETURN-VEXLIFE-CURRENT-MAIN-CONVERGENCE-A001'
HEAD12="$(printf '%s' "$CANDIDATE_HEAD" | cut -c1-12)"

TMPBASE="$(getconf DARWIN_USER_TEMP_DIR 2>/dev/null || printf '/tmp/')"
BOOTSTRAP="$TMPBASE/vexlife-current-main-proof-$ATTEMPT-$$"
REPO_ROOT="$BOOTSTRAP/repo"
EVIDENCE="$BOOTSTRAP/evidence"
SITE_OUT="$BOOTSTRAP/site"
MOUNT="$BOOTSTRAP/dmg-mount"
DOWNLOADS="$HOME/Downloads"
ARTIFACT_DIR="$DOWNLOADS/VexLife-Current-Main-Proof-$HEAD12"
SUCCESS=0
STARTED_AT="$(date -u '+%Y-%m-%dT%H:%M:%SZ')"

mkdir -p "$EVIDENCE" "$DOWNLOADS" "$ARTIFACT_DIR"
exec > >(tee -a "$EVIDENCE/run.log") 2>&1

summary() {
  cat > "$EVIDENCE/summary.txt" <<EOF
attempt=$ATTEMPT
candidateHead=$CANDIDATE_HEAD
candidateTree=$CANDIDATE_TREE
expectedBase=$EXPECTED_BASE
startedAt=$STARTED_AT
success=$SUCCESS
EOF
}

finalize() {
  rc=$?
  trap - EXIT HUP INT TERM
  set +e
  if mount | grep -F " on $MOUNT " >/dev/null 2>&1; then /usr/bin/hdiutil detach "$MOUNT" >/dev/null 2>&1 || true; fi
  summary
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
    if [ "$SUCCESS" = '1' ]; then echo 'CURRENT_MAIN_PROOF_DISPOSITION=PASS'; else echo 'CURRENT_MAIN_PROOF_DISPOSITION=SAFE_FAILURE'; fi
    echo "RETURN_ZIP=$final"
    echo "RETURN_SHA256=$digest"
    echo '============================================================'
  fi
  rm -rf "$BOOTSTRAP"
  exit "$rc"
}
trap finalize EXIT
trap 'exit 130' HUP INT TERM

fail() { echo "FAIL:$*" >&2; return 1; }

run_logged() {
  label="$1"; shift
  echo; echo ">>> $label"
  set +e
  "$@" > "$EVIDENCE/$label.log" 2>&1
  rc=$?
  set -e
  cat "$EVIDENCE/$label.log"
  [ "$rc" -eq 0 ] || fail "$label:exit=$rc"
}

echo 'VexLife current-main convergence final source/artifact proof'
echo "Candidate: $CANDIDATE_HEAD"
echo "Tree:      $CANDIDATE_TREE"

[ "$(uname -s)" = 'Darwin' ] || fail 'MACOS_REQUIRED'
case "$(uname -m)" in arm64|aarch64) ;; *) fail "APPLE_SILICON_REQUIRED:$(uname -m)" ;; esac
command -v git >/dev/null 2>&1 || fail 'GIT_REQUIRED'
command -v gh >/dev/null 2>&1 || fail 'GITHUB_CLI_REQUIRED'
gh auth status -h github.com >/dev/null 2>&1 || fail 'GITHUB_LOGIN_REQUIRED'

{
  echo "macOS=$(sw_vers -productVersion)"
  echo "arch=$(uname -m)"
  echo "hw.memsize=$(sysctl -n hw.memsize 2>/dev/null || echo UNKNOWN)"
  echo "hw.logicalcpu=$(sysctl -n hw.logicalcpu 2>/dev/null || echo UNKNOWN)"
  /usr/bin/vm_stat 2>/dev/null || true
} > "$EVIDENCE/host.txt"

export GRADLE_OPTS='-Dorg.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m -Dorg.gradle.workers.max=1 -Dkotlin.compiler.execution.strategy=in-process'
export KOTLIN_DAEMON_JVMARGS='-Xmx4096m'
printf 'GRADLE_OPTS=%s\nKOTLIN_DAEMON_JVMARGS=%s\n' "$GRADLE_OPTS" "$KOTLIN_DAEMON_JVMARGS" > "$EVIDENCE/build-memory-policy.txt"

gh repo clone "$REPOSITORY" "$REPO_ROOT" -- --no-checkout --origin origin
git -C "$REPO_ROOT" fetch --no-tags --depth=256 origin "$CANDIDATE_BRANCH" main
BRANCH_HEAD="$(git -C "$REPO_ROOT" rev-parse FETCH_HEAD)"
[ "$BRANCH_HEAD" = "$CANDIDATE_HEAD" ] || fail "BRANCH_HEAD_MISMATCH:$BRANCH_HEAD"
git -C "$REPO_ROOT" checkout --detach "$CANDIDATE_HEAD"
HEAD="$(git -C "$REPO_ROOT" rev-parse HEAD)"
TREE="$(git -C "$REPO_ROOT" rev-parse 'HEAD^{tree}')"
[ "$HEAD" = "$CANDIDATE_HEAD" ] || fail "HEAD_MISMATCH:$HEAD"
[ "$TREE" = "$CANDIDATE_TREE" ] || fail "TREE_MISMATCH:$TREE"
[ -z "$(git -C "$REPO_ROOT" status --porcelain=v1 --untracked-files=all)" ] || fail 'SOURCE_NOT_CLEAN'
git -C "$REPO_ROOT" merge-base --is-ancestor "$EXPECTED_BASE" "$CANDIDATE_HEAD" || fail 'EXPECTED_CURRENT_MAIN_BASE_NOT_ANCESTOR'
git -C "$REPO_ROOT" rev-parse refs/remotes/origin/main > "$EVIDENCE/observed-origin-main.txt"
printf '%s\n' "$HEAD" > "$EVIDENCE/source-head.txt"
printf '%s\n' "$TREE" > "$EVIDENCE/source-tree.txt"

cd "$REPO_ROOT"
{ ./gradlew --version || true; echo; /usr/bin/java -version 2>&1 || true; } > "$EVIDENCE/build-tools.txt"

run_logged '01-script-and-site-proof' /bin/bash -c 'set -euo pipefail; bash -n tools/release/publish-mac-release.sh; bash -n tools/site/form-pages-site.sh; bash -n tools/site/publish-gh-pages.sh; bash tools/site/form-pages-site.sh "'"$SITE_OUT"'"; test -f "'"$SITE_OUT"'/index.html"; test -f "'"$SITE_OUT"'/data/models.json"; test -f "'"$SITE_OUT"'/data/releases.json"; test -f "'"$SITE_OUT"'/.nojekyll"'
run_logged '02-string-source-proof' ./gradlew --no-daemon --max-workers=1 checkVexLifeStrings
run_logged '03-jvm-proof' ./gradlew --no-daemon --max-workers=1 jvmTest
run_logged '04-js-compile-proof' ./gradlew --no-daemon --max-workers=1 compileKotlinJs
run_logged '05-mac-artifact-formation' ./gradlew --no-daemon --max-workers=1 -PvexlifeAppVersion=0.0.0 archiveVexLifeProdMacApplication

APP_DIR="$REPO_ROOT/build/vexlife-prod-mac-application/$CANDIDATE_HEAD"
APP="$APP_DIR/VexLife.app"
DMG="$APP_DIR/VexLife-macOS-AppleSilicon.dmg"
DMG_SHA="$APP_DIR/VexLife-macOS-AppleSilicon.dmg.sha256"
RECEIPT="$APP_DIR/VexLife-macOS-AppleSilicon.receipt.json"
[ -d "$APP" ] || fail 'MAC_APP_MISSING'
[ -f "$DMG" ] || fail 'MAC_DMG_MISSING'
[ -f "$DMG_SHA" ] || fail 'MAC_DMG_SHA_MISSING'
[ -f "$RECEIPT" ] || fail 'MAC_RECEIPT_MISSING'

run_logged '06-codesign-verify' /usr/bin/codesign --verify --strict "$APP"
run_logged '07-dmg-sha-verify' /bin/bash -c 'cd "'"$APP_DIR"'" && shasum -a 256 -c VexLife-macOS-AppleSilicon.dmg.sha256'
run_logged '08-dmg-hdiutil-verify' /usr/bin/hdiutil verify "$DMG"

RECEIPT_SOURCE="$(/usr/bin/plutil -extract sourceSha raw -o - "$RECEIPT" 2>/dev/null || true)"
[ "$RECEIPT_SOURCE" = "$CANDIDATE_HEAD" ] || fail "MAC_RECEIPT_SOURCE_MISMATCH:${RECEIPT_SOURCE:-NONE}"

mkdir -p "$MOUNT"
/usr/bin/hdiutil attach "$DMG" -readonly -nobrowse -mountpoint "$MOUNT" >/dev/null
/usr/bin/plutil -extract CFBundleIconFile raw -o - "$MOUNT/VexLife.app/Contents/Info.plist" | grep -Fx 'applet' >/dev/null
test -s "$MOUNT/VexLife.app/Contents/Resources/applet.icns"
test ! -e "$MOUNT/VexLife.app/Contents/Resources/Assets.car"
if /usr/bin/plutil -extract CFBundleIconName raw -o - "$MOUNT/VexLife.app/Contents/Info.plist" >/dev/null 2>&1; then fail 'CFBUNDLE_ICON_NAME_MUST_BE_ABSENT'; fi
test ! -e "$MOUNT/Open Privacy & Security.webloc"
test -s "$MOUNT/.VolumeIcon.icns"
/usr/bin/xattr -p com.apple.FinderInfo "$MOUNT" >/dev/null
/usr/bin/hdiutil detach "$MOUNT" >/dev/null

cp -f "$DMG" "$ARTIFACT_DIR/"
cp -f "$DMG_SHA" "$ARTIFACT_DIR/"
cp -f "$RECEIPT" "$ARTIFACT_DIR/"
cp -f "$RECEIPT" "$EVIDENCE/VexLife-macOS-AppleSilicon.receipt.json"
shasum -a 256 "$DMG" > "$EVIDENCE/dmg-sha256.txt"
/usr/bin/vm_stat > "$EVIDENCE/host-memory-after-proof.txt" 2>/dev/null || true

SUCCESS=1
echo
echo 'Final current-main source/artifact proof completed.'
echo "DMG_PROOF_DIR=$ARTIFACT_DIR"
echo 'RETURN ZIP will be formed now.'
