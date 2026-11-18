#!/bin/bash
set -u
set -o pipefail

# [VXG RealForever]
# SPOKEN-LANGUAGE-A004 lived circuit:
# qualified Home-owned local ASR -> Product service -> Brave mic -> Composer.
#
# Exact repaired Product candidate. The proof carrier is isolated so proof changes
# cannot move the Product source under test.
SOURCE_SHA="de6a23b763bbf6a18489c5b84f04f59a8938e285"
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

ROOT="$(/usr/bin/mktemp -d "$HOME/Downloads/.vexlife-local-asr-product-spoken-language-a004.XXXXXX")" || exit 14
REPO="$ROOT/VexLife-Prod"
PROVIDER_SOURCE="$ROOT/whisper.cpp"
ART="$ROOT/artifacts"
EVIDENCE="$ROOT/evidence"
CMAKE_ROOT="$ROOT/cmake"
RETURN="$HOME/Downloads/RETURN-VEXLIFE-VOICEINPUT-SPOKEN-LANGUAGE-A004.zip"
/bin/mkdir -p "$ART" "$EVIDENCE" "$CMAKE_ROOT"

STATUS='FAIL'
DETAIL='carrier did not reach terminal disposition'
HOME_PATH=''
WORKSPACE_KEEP='false'

package_return() {
  {
    printf 'VexLife Local ASR Product Circuit SPOKEN-LANGUAGE-A004\n'
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
      "$HOME/Downloads"/.vexlife-local-asr-product-spoken-language-a004.*)
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

printf '=== VexLife local voice SPOKEN-LANGUAGE-A004 ===\n'
[ "$(/usr/bin/uname -s)" = 'Darwin' ] || fail 'SPOKEN-LANGUAGE-A004 currently qualifies macOS only' 20
[ "$(/usr/bin/uname -m)" = 'arm64' ] || fail 'SPOKEN-LANGUAGE-A004 currently qualifies Apple Silicon arm64 only' 21

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
set +e
[ "$BUILD_STATUS" -eq 0 ] || fail "Product proof/build failed ($BUILD_STATUS)" 34

[ -z "$(/usr/bin/git -C "$REPO" status --porcelain=v1 -uall)" ]   || fail 'repository-owned proof mutated the exact Product checkout' 35

IMAGE_ROOT="$REPO/build/vexlife-prod-service-image/images/$SOURCE_SHA"
SETUP="$IMAGE_ROOT/bin/vexlife-prod-setup"
[ -x "$SETUP" ] || fail 'candidate Product service image/setup launcher was not formed' 36
/bin/cp "$IMAGE_ROOT/service-image.json" "$EVIDENCE/service-image.json"

printf '\n[3/6] Materializing the exact qualified Whisper provider into Vex Home...\n'
WHISPER_COMMIT='d1be6fde11ac6e0407606b4e42fe72d34add8037'
WHISPER_TREE='6fe2755682242a5c01aba65a22b3c0305d747841'
MODEL_EN_URL='https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.en.bin'
MODEL_EN_SHA='a03779c86df3323075f5e796cb2ce5029f00ec8869eee3fdfb897afe36c6d002'
MODEL_EN_BYTES='147964211'
MODEL_MULTI_URL='https://huggingface.co/ggerganov/whisper.cpp/resolve/5359861c739e955e79d9a303bcbc70fb988958b1/ggml-base.bin'
MODEL_MULTI_SHA='60ed5bc3dd14eea856493d334349b405782ddcaf0028d4b5df4088345fba2efe'
MODEL_MULTI_BYTES='147951465'
MULTILINGUAL_MODEL_REF='model.local-asr.whisper.base'
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

PROVIDER_ROOT="$HOME_PATH/runtime/vexlife-prod/local-asr/whispercpp/v1.9.5"
MODEL_EN_DOWNLOAD="$ART/ggml-base.en.bin"
MODEL_MULTI_DOWNLOAD="$ART/ggml-base.bin"
MODEL_EN_REUSED='false'
MODEL_MULTI_REUSED='false'

if [ -f "$PROVIDER_ROOT/models/ggml-base.en.bin" ] &&
   [ "$(sha_of "$PROVIDER_ROOT/models/ggml-base.en.bin")" = "$MODEL_EN_SHA" ] &&
   [ "$(/usr/bin/stat -f '%z' "$PROVIDER_ROOT/models/ggml-base.en.bin")" = "$MODEL_EN_BYTES" ]; then
  /bin/cp "$PROVIDER_ROOT/models/ggml-base.en.bin" "$MODEL_EN_DOWNLOAD"
  MODEL_EN_REUSED='true'
else
  download "$MODEL_EN_URL" "$MODEL_EN_DOWNLOAD" || fail 'Whisper Base English download failed' 51
fi
[ "$(sha_of "$MODEL_EN_DOWNLOAD")" = "$MODEL_EN_SHA" ] || fail 'Whisper Base English digest mismatch' 52
[ "$(/usr/bin/stat -f '%z' "$MODEL_EN_DOWNLOAD")" = "$MODEL_EN_BYTES" ] || fail 'Whisper Base English size mismatch' 53

if [ -f "$PROVIDER_ROOT/models/ggml-base.bin" ] &&
   [ "$(sha_of "$PROVIDER_ROOT/models/ggml-base.bin")" = "$MODEL_MULTI_SHA" ] &&
   [ "$(/usr/bin/stat -f '%z' "$PROVIDER_ROOT/models/ggml-base.bin")" = "$MODEL_MULTI_BYTES" ]; then
  /bin/cp "$PROVIDER_ROOT/models/ggml-base.bin" "$MODEL_MULTI_DOWNLOAD"
  MODEL_MULTI_REUSED='true'
else
  download "$MODEL_MULTI_URL" "$MODEL_MULTI_DOWNLOAD" || fail 'Whisper Base Multilingual download failed' 531
fi
[ "$(sha_of "$MODEL_MULTI_DOWNLOAD")" = "$MODEL_MULTI_SHA" ] || fail 'Whisper Base Multilingual digest mismatch' 532
[ "$(/usr/bin/stat -f '%z' "$MODEL_MULTI_DOWNLOAD")" = "$MODEL_MULTI_BYTES" ] || fail 'Whisper Base Multilingual size mismatch' 533

STAGE="$ROOT/provider-stage"
/bin/mkdir -p "$STAGE/models"
# Preserve the complete upstream build/bin linkage shape, including versioned dylib symlinks.
/usr/bin/ditto "$PROVIDER_SOURCE/build/bin" "$STAGE/bin"
/bin/chmod +x "$STAGE/bin/whisper-cli"
/bin/cp "$MODEL_EN_DOWNLOAD" "$STAGE/models/ggml-base.en.bin"
/bin/cp "$MODEL_MULTI_DOWNLOAD" "$STAGE/models/ggml-base.bin"
printf '%s\n' "$WHISPER_COMMIT" > "$STAGE/provider-source.txt"
/bin/cp "$PROVIDER_SOURCE/LICENSE" "$STAGE/LICENSE.whisper.cpp"

# Replace only this exact versioned provider coordinate after all bytes are verified.
/bin/mkdir -p "$(/usr/bin/dirname "$PROVIDER_ROOT")"
/bin/rm -rf -- "$PROVIDER_ROOT"
/bin/mv "$STAGE" "$PROVIDER_ROOT"

[ -x "$PROVIDER_ROOT/bin/whisper-cli" ] || fail 'Home-owned whisper-cli is not executable' 54
[ "$(sha_of "$PROVIDER_ROOT/models/ggml-base.en.bin")" = "$MODEL_EN_SHA" ]   || fail 'Home-owned Whisper Base English digest mismatch' 55
[ "$(sha_of "$PROVIDER_ROOT/models/ggml-base.bin")" = "$MODEL_MULTI_SHA" ]   || fail 'Home-owned Whisper Base Multilingual digest mismatch' 551
[ "$(/bin/cat "$PROVIDER_ROOT/provider-source.txt")" = "$WHISPER_COMMIT" ]   || fail 'Home-owned provider source identity mismatch' 56

/bin/mv "$PROVIDER_SOURCE" "$ROOT/whisper.cpp-built-source-hidden"
DYLD_LIBRARY_PATH="$PROVIDER_ROOT/bin"   "$PROVIDER_ROOT/bin/whisper-cli" --version   > "$EVIDENCE/home-provider-version.txt" 2>&1   || fail 'Home-owned Whisper provider cannot start without its temporary build tree' 57

{
  printf 'providerRoot=%s\n' "$PROVIDER_ROOT"
  printf 'englishModelReusedFromQualifiedHome=%s\n' "$MODEL_EN_REUSED"
  printf 'multilingualModelReusedFromQualifiedHome=%s\n' "$MODEL_MULTI_REUSED"
  printf 'providerSourceCommit=%s\n' "$WHISPER_COMMIT"
  printf 'providerSourceTree=%s\n' "$WHISPER_TREE"
  printf 'englishModelSha256=%s\n' "$MODEL_EN_SHA"
  printf 'englishModelBytes=%s\n' "$MODEL_EN_BYTES"
  printf 'multilingualModelSha256=%s\n' "$MODEL_MULTI_SHA"
  printf 'multilingualModelBytes=%s\n' "$MODEL_MULTI_BYTES"
  /usr/bin/find "$PROVIDER_ROOT/bin" -maxdepth 1 -type f -print0 |
    while IFS= read -r -d '' file; do
      printf 'FILE %s  %s\n' "$(sha_of "$file")" "$(/usr/bin/basename "$file")"
    done
  /usr/bin/find "$PROVIDER_ROOT/bin" -maxdepth 1 -type l -print0 |
    while IFS= read -r -d '' link; do
      printf 'LINK %s -> %s\n' "$(/usr/bin/basename "$link")" "$(/usr/bin/readlink "$link")"
    done
  printf '%s\n' '--- whisper-cli otool ---'
  /usr/bin/otool -L "$PROVIDER_ROOT/bin/whisper-cli"
} > "$EVIDENCE/provider-artifacts.txt"

printf '\n[4/6] Re-observing Product entry and activating only its exact lawful Control...\n'
set +e
"$SETUP"   --inspect-front-door   --home "$HOME_PATH"   --expected-source-sha "$SOURCE_SHA"   > "$EVIDENCE/front-door-before.json"   2> "$EVIDENCE/front-door-before.stderr.txt"
INSPECT_STATUS=$?
set +e
[ "$INSPECT_STATUS" -eq 0 ] || fail "candidate Product front-door inspection failed ($INSPECT_STATUS)" 60

FRONT_DOOR_STATE="$(
  /usr/bin/python3 - "$EVIDENCE/front-door-before.json" <<'PY'
import json,sys
with open(sys.argv[1], encoding='utf-8') as f:
    value=json.load(f)
print(value.get("semanticStateRef",""))
PY
)"
STATE_PARSE_STATUS=$?
[ "$STATE_PARSE_STATUS" -eq 0 ] || fail "front-door state JSON parse failed ($STATE_PARSE_STATUS)" 601

FRONT_DOOR_CONTROL="$(
  /usr/bin/python3 - "$EVIDENCE/front-door-before.json" <<'PY'
import json,sys
with open(sys.argv[1], encoding='utf-8') as f:
    value=json.load(f)
print((value.get("displayState") or {}).get("primaryControlRef") or "")
PY
)"
CONTROL_PARSE_STATUS=$?
[ "$CONTROL_PARSE_STATUS" -eq 0 ] || fail "front-door Control JSON parse failed ($CONTROL_PARSE_STATUS)" 602
printf 'frontDoorState=%s\nfrontDoorControl=%s\n' "$FRONT_DOOR_STATE" "$FRONT_DOOR_CONTROL" > "$EVIDENCE/front-door-choice.txt"

case "$FRONT_DOOR_STATE" in
  CURRENT)
    # Already converged to this exact candidate source; no restart effect is needed.
    ;;
  UPDATE_AVAILABLE|REPAIR_REQUIRED|UPDATE_REPAIR_REQUIRED|START_REQUIRED)
    [ -n "$FRONT_DOOR_CONTROL" ] || fail "front door offered no Control for $FRONT_DOOR_STATE" 61
    set +e
    "$SETUP"       --activate-front-door-control "$FRONT_DOOR_CONTROL"       --home "$HOME_PATH"       --expected-source-sha "$SOURCE_SHA"       > "$EVIDENCE/front-door-activation.json"       2> "$EVIDENCE/front-door-activation.stderr.txt"
    ACTIVATE_STATUS=$?
    set +e
    [ "$ACTIVATE_STATUS" -eq 0 ] || fail "candidate Product front-door Control activation failed ($ACTIVATE_STATUS)" 62
    ACTIVATION_FAILURE="$(
      /usr/bin/python3 - "$EVIDENCE/front-door-activation.json" <<'PY'
import json,sys
with open(sys.argv[1], encoding='utf-8') as f:
    value=json.load(f)
failure=value.get("failure")
print("" if failure is None else json.dumps(failure, separators=(",",":")))
PY
    )"
    ACTIVATION_STATE="$(
      /usr/bin/python3 - "$EVIDENCE/front-door-activation.json" <<'PY'
import json,sys
with open(sys.argv[1], encoding='utf-8') as f:
    value=json.load(f)
print(value.get("semanticStateRef",""))
PY
    )"
    {
      printf 'activationState=%s\n' "$ACTIVATION_STATE"
      printf 'activationFailure=%s\n' "$ACTIVATION_FAILURE"
    } >> "$EVIDENCE/front-door-choice.txt"
    [ -z "$ACTIVATION_FAILURE" ] || fail "candidate Product front-door effect reported failure" 63
    [ "$ACTIVATION_STATE" = 'CURRENT' ] || fail "candidate Product front-door effect did not converge to CURRENT: $ACTIVATION_STATE" 64
    ;;
  *)
    fail "candidate Product front door is not restartable without acquisition: $FRONT_DOOR_STATE" 65
    ;;
esac

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
[ "$READY" = 'true' ] || fail 'candidate Product service never became source-current' 66
printf '%s\n' "$SOURCE_JSON" > "$EVIDENCE/source-currentness.json"

/usr/bin/curl -fsS --max-time 5   'http://127.0.0.1:18210/api/v1/local-asr'   > "$EVIDENCE/local-asr-before.json"   || fail 'Local ASR Settings projection is not reachable' 67
/usr/bin/python3 - "$EVIDENCE/local-asr-before.json" <<'PY' || fail 'Local ASR projection does not expose current speech models and Chinese spoken-language choice' 68
import json,sys
with open(sys.argv[1], encoding='utf-8') as f:
    value=json.load(f)
by_model={o.get("modelRef"):o for o in value.get("options",[])}
expected={
    "model.local-asr.whisper.base.en",
    "model.local-asr.whisper.base",
}
assert expected.issubset(by_model), by_model.keys()
for ref in expected:
    assert by_model[ref].get("currentState") == "READY", (ref, by_model[ref])
assert "zh" in by_model["model.local-asr.whisper.base"].get("languageCodes",[])
assert by_model["model.local-asr.whisper.base.en"].get("languageCodes") == ["en"]
choices=value.get("spokenLanguageChoices",[])
assert any(
    choice.get("mode") == "FIXED"
    and choice.get("languageCode") == "zh"
    and choice.get("supportedBySelectedModel") in (True, False)
    for choice in choices
), choices
PY

/usr/bin/curl -fsS --max-time 5   'http://127.0.0.1:18210/api/v1/settings'   > "$EVIDENCE/settings-before.json"   || fail 'Settings projection is not reachable' 681
/usr/bin/python3 - "$EVIDENCE/settings-before.json" <<'PY' || fail 'this proof requires Vex reply language to remain English' 682
import json,sys
with open(sys.argv[1], encoding='utf-8') as f:
    value=json.load(f)
assert value.get("replyLanguage") == "en", value.get("replyLanguage")
PY

# Content-free route status sweep. Successful bodies are discarded; only service error bodies
# from non-2xx responses are retained so a misleading REQUEST_JSON_INVALID can be tied to its route.
: > "$EVIDENCE/core-endpoint-status.txt"
probe_endpoint() {
  local name="$1"
  local path="$2"
  local body="$EVIDENCE/endpoint-$name.body"
  local status
  status="$(/usr/bin/curl -sS --max-time 5 -o "$body" -w '%{http_code}' "http://127.0.0.1:18210$path" || printf '000')"
  printf '%s %s\n' "$name" "$status" >> "$EVIDENCE/core-endpoint-status.txt"
  case "$status" in
    2??) /bin/rm -f "$body" ;;
    *)   /bin/mv "$body" "$EVIDENCE/endpoint-$name-error.json" 2>/dev/null || true ;;
  esac
}
probe_endpoint source-currentness '/api/v1/source-currentness'
probe_endpoint current-state '/api/v1/current-state'
probe_endpoint settings '/api/v1/settings'
probe_endpoint workspace-strings '/api/v1/strings/workspace'
probe_endpoint local-asr '/api/v1/local-asr'
probe_endpoint models '/api/v1/models'
probe_endpoint diagnostics '/api/v1/diagnostics'
probe_endpoint conversation-turn '/api/v1/conversation-turn'
probe_endpoint conversation '/api/v1/conversation'
probe_endpoint context '/api/v1/context'
probe_endpoint vision '/api/v1/vision'

CHRONOLOGY="$HOME_PATH/runtime/vexlife-prod/logs/chronology/chronology.jsonl"

count_asr_pass() {
  local fact="$1"
  local pass="$2"
  if [ ! -f "$CHRONOLOGY" ]; then
    printf '0'
    return 0
  fi
  /usr/bin/awk -v fact="\"fact\":\"$fact\"" -v sha="$SOURCE_SHA" -v pass="\"dictationPass\":\"$pass\"" -v model="\"modelRef\":\"$MULTILINGUAL_MODEL_REF\"" -v language="\"language\":\"zh\"" '
    index($0, fact) && index($0, sha) && index($0, pass) && index($0, model) && index($0, language) { count += 1 }
    END { print count + 0 }
  ' "$CHRONOLOGY"
  return 0
}

BASELINE_PARTIAL="$(count_asr_pass LOCAL_ASR_INFERENCE_COMPLETED partial)"
BASELINE_FINAL="$(count_asr_pass LOCAL_ASR_INFERENCE_COMPLETED final)"
BASELINE_FAILED_PARTIAL="$(count_asr_pass LOCAL_ASR_INFERENCE_FAILED partial)"
BASELINE_FAILED_FINAL="$(count_asr_pass LOCAL_ASR_INFERENCE_FAILED final)"

printf '\n[5/6] Product is ready for Chinese spoken-language coherence proof.\n'
printf 'Opening Brave at http://127.0.0.1:18210/\n'
printf '\n'
printf 'In VexLife:\n'
printf '  1. Open Settings -> Local voice recognition. Confirm Base English and Base Multilingual are listed with storage controls.\n'
printf '  2. Use "whisper.cpp · Whisper Base Multilingual" and confirm it is ready locally.\n'
printf '  3. Under Spoken language choose 中文. Keep Vex reply language English.\n'
printf '  4. Return to Conversation. Click the mic and speak a natural Chinese sentence WITHOUT pressing Stop.\n'
printf '  5. Confirm progressive Composer text appears IN CHINESE rather than English.\n'
printf '  6. Continue briefly, then click Stop. Confirm the final editable text remains Chinese.\n'
printf '  7. Do not press Send unless you personally want to.\n'
printf '\nThis terminal waits for BOTH partial and final multilingual inference explicitly stamped language=zh.\n'

if /usr/bin/open -Ra "Brave Browser" >/dev/null 2>&1; then
  /usr/bin/open -a "Brave Browser" 'http://127.0.0.1:18210/'
else
  /usr/bin/open 'http://127.0.0.1:18210/'
fi

LIVED='false'
for _ in $(/usr/bin/seq 1 600); do
  NOW_FAILED_PARTIAL="$(count_asr_pass LOCAL_ASR_INFERENCE_FAILED partial)"
  NOW_FAILED_FINAL="$(count_asr_pass LOCAL_ASR_INFERENCE_FAILED final)"
  if [ "${NOW_FAILED_PARTIAL:-0}" -gt "${BASELINE_FAILED_PARTIAL:-0}" ] ||
     [ "${NOW_FAILED_FINAL:-0}" -gt "${BASELINE_FAILED_FINAL:-0}" ]; then
    /usr/bin/grep -E 'VOICE_INPUT|LOCAL_ASR' "$CHRONOLOGY" |
      /usr/bin/tail -n 160 > "$EVIDENCE/voice-local-asr-chronology.jsonl" || true
    SERVICE_ERR="$HOME_PATH/runtime/product-service/logs/service.err.log"
    if [ -f "$SERVICE_ERR" ]; then
      /usr/bin/tail -n 180 "$SERVICE_ERR" > "$EVIDENCE/service-stderr-tail.txt" || true
    fi
    fail 'current-source progressive or final ASR inference failed; bounded evidence captured' 70
  fi

  NOW_PARTIAL="$(count_asr_pass LOCAL_ASR_INFERENCE_COMPLETED partial)"
  NOW_FINAL="$(count_asr_pass LOCAL_ASR_INFERENCE_COMPLETED final)"
  if [ "${NOW_PARTIAL:-0}" -gt "${BASELINE_PARTIAL:-0}" ] &&
     [ "${NOW_FINAL:-0}" -gt "${BASELINE_FINAL:-0}" ]; then
    LIVED='true'
    break
  fi
  /bin/sleep 1
done
[ "$LIVED" = 'true' ] || fail 'proof did not observe both partial and final multilingual ASR passes with language=zh' 71

printf '\n[6/6] Capturing content-free lived evidence...\n'
/usr/bin/curl -fsS --max-time 5   'http://127.0.0.1:18210/api/v1/local-asr'   > "$EVIDENCE/local-asr-after.json" || true
/usr/bin/curl -fsS --max-time 5   'http://127.0.0.1:18210/api/v1/current-state'   > "$EVIDENCE/current-state.json" || true
/usr/bin/python3 - "$EVIDENCE/local-asr-after.json" <<'PY' || fail 'final Local ASR projection is not fixed Chinese on Base Multilingual' 72
import json,sys
with open(sys.argv[1], encoding='utf-8') as f:
    value=json.load(f)
assert value.get("selectedModelRef") == "model.local-asr.whisper.base", value.get("selectedModelRef")
assert value.get("spokenLanguageMode") == "FIXED", value.get("spokenLanguageMode")
assert value.get("spokenLanguageCode") == "zh", value.get("spokenLanguageCode")
assert value.get("resolvedSpokenLanguageCode") == "zh", value.get("resolvedSpokenLanguageCode")
PY

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
DETAIL='exact source/build passed; Base Multilingual + FIXED zh remained independent from English Vex reply language; lived chronology observed partial and final inference explicitly carrying model.local-asr.whisper.base and language=zh; Chinese Composer script remains human-visible witness'
package_return || exit 80
