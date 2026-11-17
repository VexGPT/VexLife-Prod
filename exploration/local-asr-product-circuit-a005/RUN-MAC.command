#!/bin/bash
set -u
set -o pipefail

# [VXG RealForever]
# A005 lived circuit:
# qualified Home-owned local ASR -> Product service -> Brave mic -> Composer.
#
# Required caller binding:
#   VEXLIFE_A005_SOURCE_SHA=<exact commit> /bin/bash RUN-MAC.command

SOURCE_SHA="${VEXLIFE_A005_SOURCE_SHA:-}"
[ -n "$SOURCE_SHA" ] || {
  echo "VEXLIFE_A005_SOURCE_SHA is required" >&2
  exit 10
}
case "$SOURCE_SHA" in
  *[!0-9a-f]*|'') echo "invalid source SHA" >&2; exit 11 ;;
esac
[ "${#SOURCE_SHA}" -eq 40 ] || {
  echo "source SHA must be one 40-character git commit" >&2
  exit 12
}

GH=''
for candidate in /opt/homebrew/bin/gh /usr/local/bin/gh; do
  if [ -x "$candidate" ]; then
    GH="$candidate"
    break
  fi
done
[ -n "$GH" ] || {
  echo "GitHub CLI is required" >&2
  exit 13
}

ROOT="$(/usr/bin/mktemp -d "$HOME/Downloads/.vexlife-local-asr-product-a005.XXXXXX")" || exit 14
REPO="$ROOT/VexLife-Prod"
PROVIDER_SOURCE="$ROOT/whisper.cpp"
ART="$ROOT/artifacts"
EVIDENCE="$ROOT/evidence"
CMAKE_ROOT="$ROOT/cmake"
RETURN="$HOME/Downloads/RETURN-VEXLIFE-LOCAL-ASR-PRODUCT-CIRCUIT-A005.zip"
/bin/mkdir -p "$ART" "$EVIDENCE" "$CMAKE_ROOT"

STATUS='FAIL'
DETAIL='carrier did not reach terminal disposition'
HOME_PATH=''
WORKSPACE_KEEP='false'

package_return() {
  {
    printf 'VexLife Local ASR Product Circuit A005\n'
    printf 'status=%s\n' "$STATUS"
    printf 'detail=%s\n' "$DETAIL"
    printf 'sourceSha=%s\n' "$SOURCE_SHA"
    printf 'home=%s\n' "$HOME_PATH"
    printf 'formedAt=%s\n' "$(/bin/date -u '+%Y-%m-%dT%H:%M:%SZ')"
  } > "$EVIDENCE/RESULT-SUMMARY.txt"

  /bin/rm -f "$RETURN"
  (
    cd "$EVIDENCE" || exit 1
    /usr/bin/zip -qry "$RETURN" .
  ) || return 1
  printf '\nRETURN: %s\n' "$RETURN"
  /usr/bin/shasum -a 256 "$RETURN"
}

cleanup() {
  if [ "$WORKSPACE_KEEP" != 'true' ]; then
    case "$ROOT" in
      "$HOME/Downloads"/.vexlife-local-asr-product-a005.*)
        /bin/rm -rf -- "$ROOT"
        ;;
    esac
  fi
}
trap cleanup EXIT HUP INT TERM

fail() {
  STATUS='FAIL'
  DETAIL="$1"
  printf '\nFAIL: %s\n' "$DETAIL" >&2
  package_return || true
  exit "${2:-1}"
}

download() {
  local url="$1"
  local out="$2"
  /usr/bin/curl -fL --retry 3 --retry-delay 2 --connect-timeout 20 "$url" -o "$out"
}

sha_of() {
  /usr/bin/shasum -a 256 "$1" | /usr/bin/awk '{print $1}'
}

printf '=== VexLife local voice A005 ===\n'
[ "$(/usr/bin/uname -s)" = 'Darwin' ] || fail 'A005 currently qualifies macOS only' 20
[ "$(/usr/bin/uname -m)" = 'arm64' ] || fail 'A005 currently qualifies Apple Silicon arm64 only' 21

# Resolve the person's current Vex Home from the running Product when possible.
CURRENT_STATE="$(
  /usr/bin/curl -fsS --max-time 3     'http://127.0.0.1:18210/api/v1/current-state' 2>/dev/null || true
)"
if [ -n "$CURRENT_STATE" ] && [ -x /usr/bin/python3 ]; then
  HOME_PATH="$(
    printf '%s' "$CURRENT_STATE" |
      /usr/bin/python3 -c 'import json,sys; print(json.load(sys.stdin).get("homePath",""))' 2>/dev/null || true
  )"
fi
if [ -z "$HOME_PATH" ] && [ -f "$HOME/.vexlife/config/home.json" ]; then
  HOME_PATH="$HOME/.vexlife"
fi
[ -n "$HOME_PATH" ] || fail 'no established Vex Home could be resolved' 22
[ -f "$HOME_PATH/config/home.json" ] || fail 'resolved Vex Home is not canonical' 23
printf 'Resolved Vex Home: %s\n' "$HOME_PATH"

{
  /usr/bin/sw_vers
  /usr/bin/uname -a
  printf 'home=%s\n' "$HOME_PATH"
  "$GH" --version
} > "$EVIDENCE/host.txt" 2>&1

printf '\n[1/6] Fetching exact Product source...\n'
"$GH" repo clone VexGPT/VexLife-Prod "$REPO" -- --filter=blob:none --no-checkout   > "$EVIDENCE/product-clone.stdout.txt"   2> "$EVIDENCE/product-clone.stderr.txt"   || fail 'Product source clone failed' 30

/usr/bin/git -C "$REPO" checkout --detach "$SOURCE_SHA"   > "$EVIDENCE/product-checkout.stdout.txt"   2> "$EVIDENCE/product-checkout.stderr.txt"   || fail 'exact Product source checkout failed' 31

ACTUAL_SOURCE="$(/usr/bin/git -C "$REPO" rev-parse HEAD)"
SOURCE_TREE="$(/usr/bin/git -C "$REPO" rev-parse 'HEAD^{tree}')"
[ "$ACTUAL_SOURCE" = "$SOURCE_SHA" ] || fail 'Product checkout source identity mismatch' 32
[ -z "$(/usr/bin/git -C "$REPO" status --porcelain=v1 -uall)" ]   || fail 'Product checkout is not clean before proof' 33
{
  printf 'sourceSha=%s\n' "$ACTUAL_SOURCE"
  printf 'sourceTree=%s\n' "$SOURCE_TREE"
} > "$EVIDENCE/source.txt"

printf '\n[2/6] Running repository-owned source/build proof...\n'
set +e
(
  cd "$REPO" || exit 1
  ./gradlew --no-daemon     compileKotlinJs     compileTestKotlinJs     compileKotlinJvm     compileTestKotlinJvm     jvmTest     checkVexLifeStrings     generateVexLifeWebP1     verifyVexLifeWebP1     assembleVexLifeProdServiceImage
) > "$EVIDENCE/product-build.txt" 2>&1
BUILD_STATUS=$?
set -e
[ "$BUILD_STATUS" -eq 0 ] || fail "Product proof/build failed ($BUILD_STATUS)" 34

[ -z "$(/usr/bin/git -C "$REPO" status --porcelain=v1 -uall)" ]   || fail 'repository-owned proof mutated the exact Product checkout' 35

IMAGE_ROOT="$REPO/build/vexlife-prod-service-image/images/$SOURCE_SHA"
SETUP="$IMAGE_ROOT/bin/vexlife-prod-setup"
[ -x "$SETUP" ] || fail 'candidate Product service image/setup launcher was not formed' 36
/bin/cp "$IMAGE_ROOT/service-image.json" "$EVIDENCE/service-image.json"

printf '\n[3/6] Materializing the exact qualified Whisper provider into Vex Home...\n'
WHISPER_COMMIT='d1be6fde11ac6e0407606b4e42fe72d34add8037'
WHISPER_TREE='6fe2755682242a5c01aba65a22b3c0305d747841'
MODEL_URL='https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.en.bin'
MODEL_SHA='a03779c86df3323075f5e796cb2ce5029f00ec8869eee3fdfb897afe36c6d002'
MODEL_BYTES='147964211'
CMAKE_URL='https://github.com/Kitware/CMake/releases/download/v4.5.0/cmake-4.5.0-macos-universal.tar.gz'
CMAKE_SHA='a645d69fd21e521759cc0c64e7c60462d43faf0c7214ac05f75c80f7c5037dde'

CMAKE_BIN=''
for candidate in /opt/homebrew/bin/cmake /usr/local/bin/cmake /Applications/CMake.app/Contents/bin/cmake; do
  if [ -x "$candidate" ]; then
    CMAKE_BIN="$candidate"
    break
  fi
done
if [ -z "$CMAKE_BIN" ]; then
  CMAKE_ARCHIVE="$ART/cmake-4.5.0-macos-universal.tar.gz"
  download "$CMAKE_URL" "$CMAKE_ARCHIVE" || fail 'temporary CMake download failed' 40
  [ "$(sha_of "$CMAKE_ARCHIVE")" = "$CMAKE_SHA" ] || fail 'temporary CMake digest mismatch' 41
  /usr/bin/tar -xzf "$CMAKE_ARCHIVE" -C "$CMAKE_ROOT" || fail 'temporary CMake extraction failed' 42
  CMAKE_BIN="$(
    /usr/bin/find "$CMAKE_ROOT" -type f -path '*/CMake.app/Contents/bin/cmake' -perm -111 -print 2>/dev/null |
      /usr/bin/head -n 1
  )"
fi
[ -n "$CMAKE_BIN" ] && [ -x "$CMAKE_BIN" ] || fail 'CMake is unavailable' 43

/usr/bin/git clone --no-checkout --filter=blob:none   https://github.com/ggml-org/whisper.cpp.git "$PROVIDER_SOURCE"   > "$EVIDENCE/whisper-clone.stdout.txt"   2> "$EVIDENCE/whisper-clone.stderr.txt"   || fail 'whisper.cpp clone failed' 44
/usr/bin/git -C "$PROVIDER_SOURCE" checkout --detach "$WHISPER_COMMIT"   > "$EVIDENCE/whisper-checkout.stdout.txt"   2> "$EVIDENCE/whisper-checkout.stderr.txt"   || fail 'whisper.cpp exact checkout failed' 45
[ "$(/usr/bin/git -C "$PROVIDER_SOURCE" rev-parse HEAD)" = "$WHISPER_COMMIT" ]   || fail 'whisper.cpp commit mismatch' 46
[ "$(/usr/bin/git -C "$PROVIDER_SOURCE" rev-parse 'HEAD^{tree}')" = "$WHISPER_TREE" ]   || fail 'whisper.cpp tree mismatch' 47

"$CMAKE_BIN"   -S "$PROVIDER_SOURCE"   -B "$PROVIDER_SOURCE/build"   -DCMAKE_BUILD_TYPE=Release   -DWHISPER_BUILD_TESTS=OFF   -DWHISPER_BUILD_SERVER=OFF   -DWHISPER_BUILD_EXAMPLES=ON   > "$EVIDENCE/whisper-cmake-configure.txt" 2>&1   || fail 'whisper.cpp configure failed' 48
CPU_COUNT="$(/usr/sbin/sysctl -n hw.ncpu 2>/dev/null || printf '4')"
"$CMAKE_BIN" --build "$PROVIDER_SOURCE/build" --config Release --target whisper-cli --parallel "$CPU_COUNT"   > "$EVIDENCE/whisper-build.txt" 2>&1   || fail 'whisper.cpp build failed' 49

WHISPER_BIN="$PROVIDER_SOURCE/build/bin/whisper-cli"
[ -x "$WHISPER_BIN" ] || fail 'whisper-cli was not formed' 50

MODEL_DOWNLOAD="$ART/ggml-base.en.bin"
download "$MODEL_URL" "$MODEL_DOWNLOAD" || fail 'Whisper Base English download failed' 51
[ "$(sha_of "$MODEL_DOWNLOAD")" = "$MODEL_SHA" ] || fail 'Whisper model digest mismatch' 52
[ "$(/usr/bin/stat -f '%z' "$MODEL_DOWNLOAD")" = "$MODEL_BYTES" ] || fail 'Whisper model size mismatch' 53

PROVIDER_ROOT="$HOME_PATH/runtime/vexlife-prod/local-asr/whispercpp/v1.9.5"
STAGE="$ROOT/provider-stage"
/bin/mkdir -p "$STAGE/bin" "$STAGE/models"
/bin/cp "$WHISPER_BIN" "$STAGE/bin/whisper-cli"
/usr/bin/find "$PROVIDER_SOURCE/build/bin" -maxdepth 1 -type f -name '*.dylib' -exec /bin/cp {} "$STAGE/bin/" \;
/bin/chmod +x "$STAGE/bin/whisper-cli"
/bin/cp "$MODEL_DOWNLOAD" "$STAGE/models/ggml-base.en.bin"
printf '%s\n' "$WHISPER_COMMIT" > "$STAGE/provider-source.txt"
/bin/cp "$PROVIDER_SOURCE/LICENSE" "$STAGE/LICENSE.whisper.cpp"

# Replace only this exact versioned provider coordinate after all bytes are verified.
/bin/mkdir -p "$(/usr/bin/dirname "$PROVIDER_ROOT")"
/bin/rm -rf -- "$PROVIDER_ROOT"
/bin/mv "$STAGE" "$PROVIDER_ROOT"

[ -x "$PROVIDER_ROOT/bin/whisper-cli" ] || fail 'Home-owned whisper-cli is not executable' 54
[ "$(sha_of "$PROVIDER_ROOT/models/ggml-base.en.bin")" = "$MODEL_SHA" ]   || fail 'Home-owned Whisper model digest mismatch' 55
[ "$(/bin/cat "$PROVIDER_ROOT/provider-source.txt")" = "$WHISPER_COMMIT" ]   || fail 'Home-owned provider source identity mismatch' 56

DYLD_LIBRARY_PATH="$PROVIDER_ROOT/bin"   "$PROVIDER_ROOT/bin/whisper-cli" --version   > "$EVIDENCE/home-provider-version.txt" 2>&1   || fail 'Home-owned Whisper provider cannot start' 57

{
  printf 'providerRoot=%s\n' "$PROVIDER_ROOT"
  printf 'providerSourceCommit=%s\n' "$WHISPER_COMMIT"
  printf 'providerSourceTree=%s\n' "$WHISPER_TREE"
  printf 'modelSha256=%s\n' "$MODEL_SHA"
  printf 'modelBytes=%s\n' "$MODEL_BYTES"
  /usr/bin/find "$PROVIDER_ROOT/bin" -maxdepth 1 -type f -print0 |
    while IFS= read -r -d '' file; do
      printf '%s  %s\n' "$(sha_of "$file")" "$(/usr/bin/basename "$file")"
    done
} > "$EVIDENCE/provider-artifacts.txt"

printf '\n[4/6] Converging the candidate Product service through its own setup/lifetime owner...\n'
set +e
"$SETUP"   --home "$HOME_PATH"   --expected-source-sha "$SOURCE_SHA"   --reuse-existing   > "$EVIDENCE/product-setup.stdout.txt"   2> "$EVIDENCE/product-setup.stderr.txt"
SETUP_STATUS=$?
set -e
[ "$SETUP_STATUS" -eq 0 ] || fail "candidate Product setup/service convergence failed ($SETUP_STATUS)" 60

READY='false'
for _ in $(/usr/bin/seq 1 120); do
  SOURCE_JSON="$(
    /usr/bin/curl -fsS --max-time 2       'http://127.0.0.1:18210/api/v1/source-currentness' 2>/dev/null || true
  )"
  if printf '%s' "$SOURCE_JSON" | /usr/bin/grep -q "\"sourceSha\":\"$SOURCE_SHA\""; then
    READY='true'
    break
  fi
  /bin/sleep 1
done
[ "$READY" = 'true' ] || fail 'candidate Product service never became source-current' 61
printf '%s\n' "$SOURCE_JSON" > "$EVIDENCE/source-currentness.json"

/usr/bin/curl -fsS --max-time 5   'http://127.0.0.1:18210/api/v1/local-asr'   > "$EVIDENCE/local-asr-before.json"   || fail 'Local ASR Settings projection is not reachable' 62
/usr/bin/grep -q '"currentState":"READY"' "$EVIDENCE/local-asr-before.json"   || fail 'selected Local ASR provider is not READY in the candidate service' 63
/usr/bin/grep -q '"provider.local-asr.whispercpp"' "$EVIDENCE/local-asr-before.json"   || fail 'candidate Settings projection does not expose selected whisper.cpp provider' 64

CHRONOLOGY="$HOME_PATH/runtime/vexlife-prod/logs/chronology/chronology.jsonl"
BASELINE=0
if [ -f "$CHRONOLOGY" ]; then
  BASELINE="$(
    /usr/bin/grep -c '"fact":"LOCAL_ASR_INFERENCE_COMPLETED"' "$CHRONOLOGY" 2>/dev/null || true
  )"
fi

printf '\n[5/6] Product is ready for the lived mic test.\n'
printf 'Opening Brave at http://127.0.0.1:18210/\n'
printf '\n'
printf 'In VexLife:\n'
printf '  1. Open Settings and confirm "Local voice recognition" shows whisper.cpp · Whisper Base English and Ready locally.\n'
printf '  2. Return to Conversation. Click the mic, speak one sentence, then click Stop.\n'
printf '  3. Confirm the words appear in the existing Composer; do NOT press Send unless you want to.\n'
printf '\nThis terminal will package the RETURN automatically after local inference completes.\n'

if /usr/bin/open -Ra "Brave Browser" >/dev/null 2>&1; then
  /usr/bin/open -a "Brave Browser" 'http://127.0.0.1:18210/'
else
  /usr/bin/open 'http://127.0.0.1:18210/'
fi

LIVED='false'
for _ in $(/usr/bin/seq 1 600); do
  if [ -f "$CHRONOLOGY" ]; then
    NOW="$(
      /usr/bin/grep -c '"fact":"LOCAL_ASR_INFERENCE_COMPLETED"' "$CHRONOLOGY" 2>/dev/null || true
    )"
    if [ "${NOW:-0}" -gt "${BASELINE:-0}" ]; then
      if /usr/bin/grep '"fact":"LOCAL_ASR_INFERENCE_COMPLETED"' "$CHRONOLOGY" |
          /usr/bin/tail -n 1 |
          /usr/bin/grep -q "$SOURCE_SHA"; then
        LIVED='true'
        break
      fi
    fi
  fi
  /bin/sleep 1
done
[ "$LIVED" = 'true' ] || fail 'no current-source local ASR inference completed within the lived-test window' 70

printf '\n[6/6] Capturing content-free lived evidence...\n'
/usr/bin/curl -fsS --max-time 5   'http://127.0.0.1:18210/api/v1/local-asr'   > "$EVIDENCE/local-asr-after.json" || true
/usr/bin/curl -fsS --max-time 5   'http://127.0.0.1:18210/api/v1/current-state'   > "$EVIDENCE/current-state.json" || true

if [ -f "$CHRONOLOGY" ]; then
  /usr/bin/grep -E 'VOICE_INPUT|LOCAL_ASR' "$CHRONOLOGY" |
    /usr/bin/tail -n 120     > "$EVIDENCE/voice-local-asr-chronology.jsonl" || true
fi

SERVICE_ERR="$HOME_PATH/runtime/product-service/logs/service.err.log"
if [ -f "$SERVICE_ERR" ]; then
  /usr/bin/tail -n 180 "$SERVICE_ERR" > "$EVIDENCE/service-stderr-tail.txt" || true
fi

if [ -f "$REPO/build/generated/vexweb/vexlife-workspace/index.html" ]; then
  /usr/bin/shasum -a 256     "$REPO/build/generated/vexweb/vexlife-workspace/index.html"     "$REPO/build/generated/vexweb/vexlife-workspace/vexweb.js"     > "$EVIDENCE/generated-vexweb-sha256.txt"
fi

STATUS='PASS'
DETAIL='exact A005 Product service reached READY local ASR and one lived Brave microphone capture completed local inference; Composer arrival remains human-visible witness'
package_return || exit 80
