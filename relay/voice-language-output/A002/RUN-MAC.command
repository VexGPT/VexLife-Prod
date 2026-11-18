#!/bin/bash
set -u
set -o pipefail

# [VXG RealForever]
# VOICE-LANGUAGE-OUTPUT-A002 lived circuit:
# qualified Home-owned local ASR -> Product service -> Brave mic -> Composer.
#
# Exact repaired Product candidate. The proof carrier is isolated so proof changes
# cannot move the Product source under test.
SOURCE_SHA="b30d817295dd8cca23941c53b8a43d00572555e2"
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

ROOT="$(/usr/bin/mktemp -d "$HOME/Downloads/.vexlife-local-asr-product-voice-language-output-a002.XXXXXX")" || exit 14
REPO="$ROOT/VexLife-Prod"
PROVIDER_SOURCE="$ROOT/whisper.cpp"
ART="$ROOT/artifacts"
EVIDENCE="$ROOT/evidence"
CMAKE_ROOT="$ROOT/cmake"
RETURN="$HOME/Downloads/RETURN-VEXLIFE-VOICE-LANGUAGE-OUTPUT-A002.zip"
/bin/mkdir -p "$ART" "$EVIDENCE" "$CMAKE_ROOT"

STATUS='FAIL'
DETAIL='carrier did not reach terminal disposition'
HOME_PATH=''
WORKSPACE_KEEP='false'

package_return() {
  {
    printf 'VexLife Local ASR Product Circuit VOICE-LANGUAGE-OUTPUT-A002\n'
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
      "$HOME/Downloads"/.vexlife-local-asr-product-voice-language-output-a002.*)
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

printf '=== VexLife local voice VOICE-LANGUAGE-OUTPUT-A002 ===\n'
[ "$(/usr/bin/uname -s)" = 'Darwin' ] || fail 'VOICE-LANGUAGE-OUTPUT-A002 currently qualifies macOS only' 20
[ "$(/usr/bin/uname -m)" = 'arm64' ] || fail 'VOICE-LANGUAGE-OUTPUT-A002 currently qualifies Apple Silicon arm64 only' 21

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
# A001 successor proof reached compileTestKotlinJvm and stopped only because two new test-owner
# imports were missing. Product main source, string checks, web generation and service-image assembly
# had already compiled. A002 keeps the same bounded build policy and retries the corrected exact head.
{
  printf 'gradleJvmMaxHeap=2048m\n'
  printf 'gradleMaxWorkers=2\n'
  printf 'kotlinCompilerExecutionStrategy=in-process\n'
  printf 'parentVoiceProof=A004\n'
  printf 'parentVoiceReturnSha256=96cc5bb17d5a0cfd79518f397d27631e368f7162a79216e3b94c9e1119640cbb\n'
  printf 'parentVoiceDisposition=PASS persistent Whisper partial/final zh\n'
  printf 'priorSuccessorAttempt=A001\n'
  printf 'priorSuccessorReturnSha256=beb184d16078f690c3c965ed5a2d6b7c3382189d2d86a33fe79e50f5d36efce9\n'
  printf 'priorSuccessorFailure=compileTestKotlinJvm missing test imports only\n'
} > "$EVIDENCE/build-policy.txt"
set +e
(
  cd "$REPO" || exit 1
  ./gradlew \
    --no-daemon \
    --max-workers=2 \
    -Dorg.gradle.jvmargs=-Xmx2048m \
    -Pkotlin.compiler.execution.strategy=in-process \
    compileKotlinJs \
    compileTestKotlinJs \
    compileKotlinJvm \
    compileTestKotlinJvm \
    jvmTest \
    checkVexLifeStrings \
    generateVexLifeWebP1 \
    verifyVexLifeWebP1 \
    assembleVexLifeProdServiceImage
) > "$EVIDENCE/product-build.txt" 2>&1
BUILD_STATUS=$?
set +e
[ "$BUILD_STATUS" -eq 0 ] || fail "Product proof/build failed ($BUILD_STATUS)" 34

[ -z "$(/usr/bin/git -C "$REPO" status --porcelain=v1 -uall)" ]   || fail 'repository-owned proof mutated the exact Product checkout' 35

IMAGE_ROOT="$REPO/build/vexlife-prod-service-image/images/$SOURCE_SHA"
SETUP="$IMAGE_ROOT/bin/vexlife-prod-setup"
[ -x "$SETUP" ] || fail 'candidate Product service image/setup launcher was not formed' 36
/bin/cp "$IMAGE_ROOT/service-image.json" "$EVIDENCE/service-image.json"

printf '\n[3/6] Reusing the exact A004-qualified Whisper provider...\n'
WHISPER_COMMIT='d1be6fde11ac6e0407606b4e42fe72d34add8037'
MODEL_EN_SHA='a03779c86df3323075f5e796cb2ce5029f00ec8869eee3fdfb897afe36c6d002'
MODEL_EN_BYTES='147964211'
MODEL_MULTI_SHA='60ed5bc3dd14eea856493d334349b405782ddcaf0028d4b5df4088345fba2efe'
MODEL_MULTI_BYTES='147951465'
MULTILINGUAL_MODEL_REF='model.local-asr.whisper.base'
PROVIDER_ROOT="$HOME_PATH/runtime/vexlife-prod/local-asr/whispercpp/v1.9.5"

[ -f "$PROVIDER_ROOT/provider-source.txt" ] || fail 'A004-qualified Whisper provider source receipt is absent' 40
[ "$(/bin/cat "$PROVIDER_ROOT/provider-source.txt")" = "$WHISPER_COMMIT" ] || fail 'Whisper provider source identity is not the A004-qualified source' 41
[ -x "$PROVIDER_ROOT/bin/whisper-server" ] || fail 'A004-qualified persistent whisper-server is absent' 42
[ -x "$PROVIDER_ROOT/bin/whisper-cli" ] || fail 'A004-qualified whisper-cli is absent' 43
[ -f "$PROVIDER_ROOT/models/ggml-base.en.bin" ] || fail 'Whisper Base English artifact is absent' 44
[ -f "$PROVIDER_ROOT/models/ggml-base.bin" ] || fail 'Whisper Base Multilingual artifact is absent' 45
[ "$(sha_of "$PROVIDER_ROOT/models/ggml-base.en.bin")" = "$MODEL_EN_SHA" ] || fail 'Whisper Base English digest is no longer current' 46
[ "$(/usr/bin/stat -f '%z' "$PROVIDER_ROOT/models/ggml-base.en.bin")" = "$MODEL_EN_BYTES" ] || fail 'Whisper Base English size is no longer current' 47
[ "$(sha_of "$PROVIDER_ROOT/models/ggml-base.bin")" = "$MODEL_MULTI_SHA" ] || fail 'Whisper Base Multilingual digest is no longer current' 48
[ "$(/usr/bin/stat -f '%z' "$PROVIDER_ROOT/models/ggml-base.bin")" = "$MODEL_MULTI_BYTES" ] || fail 'Whisper Base Multilingual size is no longer current' 49

DYLD_LIBRARY_PATH="$PROVIDER_ROOT/bin" \
  "$PROVIDER_ROOT/bin/whisper-server" --help \
  > "$EVIDENCE/home-provider-server-help.txt" 2>&1 \
  || fail 'A004-qualified persistent Whisper server cannot start from Home' 50
/usr/bin/grep -q 'translate from source language to english' "$EVIDENCE/home-provider-server-help.txt" \
  || fail 'pinned Whisper translation contract is not present' 51
{
  printf 'providerRoot=%s\n' "$PROVIDER_ROOT"
  printf 'providerSourceCommit=%s\n' "$WHISPER_COMMIT"
  printf 'englishModelSha256=%s\n' "$MODEL_EN_SHA"
  printf 'multilingualModelSha256=%s\n' "$MODEL_MULTI_SHA"
  printf 'reuseEvidence=A004 PASS / exact provider bytes unchanged\n'
} > "$EVIDENCE/provider-reuse.txt"

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
  UPDATE_AVAILABLE|REPAIR_REQUIRED|UPDATE_REPAIR_REQUIRED|SERVICE_RECOVERY_REQUIRED|START_REQUIRED)
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
  local output_mode="$3"
  if [ ! -f "$CHRONOLOGY" ]; then printf '0'; return 0; fi
  /usr/bin/awk -v fact="\"fact\":\"$fact\"" -v sha="$SOURCE_SHA" -v pass="\"dictationPass\":\"$pass\"" -v model="\"modelRef\":\"$MULTILINGUAL_MODEL_REF\"" -v language="\"language\":\"zh\"" -v output="\"composerOutputMode\":\"$output_mode\"" '
    index($0, fact) && index($0, sha) && index($0, pass) && index($0, model) && index($0, language) && index($0, output) { count += 1 }
    END { print count + 0 }
  ' "$CHRONOLOGY"
}

BASE_AS_PARTIAL="$(count_asr_pass LOCAL_ASR_INFERENCE_COMPLETED partial AS_SPOKEN)"
BASE_AS_FINAL="$(count_asr_pass LOCAL_ASR_INFERENCE_COMPLETED final AS_SPOKEN)"
BASE_TR_PARTIAL="$(count_asr_pass LOCAL_ASR_INFERENCE_COMPLETED partial TRANSLATE_TO_ENGLISH)"
BASE_TR_FINAL="$(count_asr_pass LOCAL_ASR_INFERENCE_COMPLETED final TRANSLATE_TO_ENGLISH)"
BASE_FAIL_AS_PARTIAL="$(count_asr_pass LOCAL_ASR_INFERENCE_FAILED partial AS_SPOKEN)"
BASE_FAIL_AS_FINAL="$(count_asr_pass LOCAL_ASR_INFERENCE_FAILED final AS_SPOKEN)"
BASE_FAIL_TR_PARTIAL="$(count_asr_pass LOCAL_ASR_INFERENCE_FAILED partial TRANSLATE_TO_ENGLISH)"
BASE_FAIL_TR_FINAL="$(count_asr_pass LOCAL_ASR_INFERENCE_FAILED final TRANSLATE_TO_ENGLISH)"

printf '\n[5/6] Voice language/output successor is ready for lived proof.\n'
printf 'Opening Brave at http://127.0.0.1:18210/\n\n'
printf 'A. Open "Vex replies in". Confirm BOTH 简体中文 and 繁體中文 exist. Choose 简体中文.\n'
printf 'B. Settings -> Local voice recognition: Whisper Base Multilingual; Spoken language = Follow Vex reply language · 简体中文; Composer output = As spoken.\n'
printf 'C. Conversation: mic, speak Chinese and switch into English in the SAME dictation. Confirm both remain editable; Stop. Do not Send unless you want to.\n'
printf 'D. Clear/edit the draft. Set Composer output = Translate to English. Speak a natural Chinese sentence. Confirm progressive/final Composer text is English; Stop.\n'
printf 'E. Choose 繁體中文 under Vex replies in and leave Spoken language on Follow briefly. Open Speak Vex aloud; matching Traditional voices should be preferred when present.\n'
printf '   Switching back to 简体中文 should prefer matching Simplified-region voices when present. Voice choice is human witness.\n'
printf '\nThe terminal finishes after BOTH reply scripts, Follow->spoken zh, and partial+final in BOTH Composer modes are observed.\n'

if /usr/bin/open -Ra "Brave Browser" >/dev/null 2>&1; then
  /usr/bin/open -a "Brave Browser" 'http://127.0.0.1:18210/'
else
  /usr/bin/open 'http://127.0.0.1:18210/'
fi

SAW_HANS='false'; SAW_HANT='false'; SAW_HANS_FOLLOW_ZH='false'; SAW_HANT_FOLLOW_ZH='false'; LIVED='false'

for _ in $(/usr/bin/seq 1 900); do
  SETTINGS_NOW="$(/usr/bin/curl -fsS --max-time 2 'http://127.0.0.1:18210/api/v1/settings' 2>/dev/null || true)"
  ASR_NOW="$(/usr/bin/curl -fsS --max-time 2 'http://127.0.0.1:18210/api/v1/local-asr' 2>/dev/null || true)"
  REPLY_NOW="$(printf '%s' "$SETTINGS_NOW" | /usr/bin/python3 -c 'import json,sys; print(json.load(sys.stdin).get("replyLanguage",""))' 2>/dev/null || true)"
  ASR_REL="$(printf '%s' "$ASR_NOW" | /usr/bin/python3 -c 'import json,sys; x=json.load(sys.stdin); print("%s|%s|%s|%s" % (x.get("spokenLanguageMode",""),x.get("resolvedSpokenLanguageCode",""),x.get("selectedModelRef",""),x.get("composerOutputMode","")))' 2>/dev/null || true)"

  [ "$REPLY_NOW" = 'zh-Hans' ] && SAW_HANS='true'
  [ "$REPLY_NOW" = 'zh-Hant' ] && SAW_HANT='true'
  case "$REPLY_NOW|$ASR_REL" in
    'zh-Hans|FOLLOW_REPLY_LANGUAGE|zh|model.local-asr.whisper.base|'*) SAW_HANS_FOLLOW_ZH='true' ;;
    'zh-Hant|FOLLOW_REPLY_LANGUAGE|zh|model.local-asr.whisper.base|'*) SAW_HANT_FOLLOW_ZH='true' ;;
  esac

  NOW_FAIL_AS_PARTIAL="$(count_asr_pass LOCAL_ASR_INFERENCE_FAILED partial AS_SPOKEN)"
  NOW_FAIL_AS_FINAL="$(count_asr_pass LOCAL_ASR_INFERENCE_FAILED final AS_SPOKEN)"
  NOW_FAIL_TR_PARTIAL="$(count_asr_pass LOCAL_ASR_INFERENCE_FAILED partial TRANSLATE_TO_ENGLISH)"
  NOW_FAIL_TR_FINAL="$(count_asr_pass LOCAL_ASR_INFERENCE_FAILED final TRANSLATE_TO_ENGLISH)"
  if [ "${NOW_FAIL_AS_PARTIAL:-0}" -gt "${BASE_FAIL_AS_PARTIAL:-0}" ] ||
     [ "${NOW_FAIL_AS_FINAL:-0}" -gt "${BASE_FAIL_AS_FINAL:-0}" ] ||
     [ "${NOW_FAIL_TR_PARTIAL:-0}" -gt "${BASE_FAIL_TR_PARTIAL:-0}" ] ||
     [ "${NOW_FAIL_TR_FINAL:-0}" -gt "${BASE_FAIL_TR_FINAL:-0}" ]; then
    /usr/bin/grep -E 'VOICE_INPUT|LOCAL_ASR' "$CHRONOLOGY" | /usr/bin/tail -n 180 > "$EVIDENCE/voice-local-asr-chronology.jsonl" || true
    fail 'current-source As-spoken or Translate-to-English inference failed; bounded evidence captured' 70
  fi

  NOW_AS_PARTIAL="$(count_asr_pass LOCAL_ASR_INFERENCE_COMPLETED partial AS_SPOKEN)"
  NOW_AS_FINAL="$(count_asr_pass LOCAL_ASR_INFERENCE_COMPLETED final AS_SPOKEN)"
  NOW_TR_PARTIAL="$(count_asr_pass LOCAL_ASR_INFERENCE_COMPLETED partial TRANSLATE_TO_ENGLISH)"
  NOW_TR_FINAL="$(count_asr_pass LOCAL_ASR_INFERENCE_COMPLETED final TRANSLATE_TO_ENGLISH)"

  if [ "$SAW_HANS" = 'true' ] && [ "$SAW_HANT" = 'true' ] &&
     [ "$SAW_HANS_FOLLOW_ZH" = 'true' ] && [ "$SAW_HANT_FOLLOW_ZH" = 'true' ] &&
     [ "${NOW_AS_PARTIAL:-0}" -gt "${BASE_AS_PARTIAL:-0}" ] &&
     [ "${NOW_AS_FINAL:-0}" -gt "${BASE_AS_FINAL:-0}" ] &&
     [ "${NOW_TR_PARTIAL:-0}" -gt "${BASE_TR_PARTIAL:-0}" ] &&
     [ "${NOW_TR_FINAL:-0}" -gt "${BASE_TR_FINAL:-0}" ]; then
    LIVED='true'; break
  fi
  /bin/sleep 1
done
[ "$LIVED" = 'true' ] || fail 'proof did not observe both Chinese reply scripts, Follow->zh, and both Composer output modes' 71

printf '\n[6/6] Capturing content-free successor evidence...\n'
/usr/bin/curl -fsS --max-time 5 'http://127.0.0.1:18210/api/v1/settings' > "$EVIDENCE/settings-after.json" || true
/usr/bin/curl -fsS --max-time 5 'http://127.0.0.1:18210/api/v1/local-asr' > "$EVIDENCE/local-asr-after.json" || true
/usr/bin/curl -fsS --max-time 5 'http://127.0.0.1:18210/api/v1/source-currentness' > "$EVIDENCE/source-currentness-after.json" || true
{
  printf 'sawReplyZhHans=%s\n' "$SAW_HANS"
  printf 'sawReplyZhHant=%s\n' "$SAW_HANT"
  printf 'sawZhHansFollowResolvedZh=%s\n' "$SAW_HANS_FOLLOW_ZH"
  printf 'sawZhHantFollowResolvedZh=%s\n' "$SAW_HANT_FOLLOW_ZH"
  printf 'asSpokenPartialDelta=%s\n' "$((NOW_AS_PARTIAL-BASE_AS_PARTIAL))"
  printf 'asSpokenFinalDelta=%s\n' "$((NOW_AS_FINAL-BASE_AS_FINAL))"
  printf 'translateEnglishPartialDelta=%s\n' "$((NOW_TR_PARTIAL-BASE_TR_PARTIAL))"
  printf 'translateEnglishFinalDelta=%s\n' "$((NOW_TR_FINAL-BASE_TR_FINAL))"
} > "$EVIDENCE/lived-predicates.txt"
if [ -f "$CHRONOLOGY" ]; then /usr/bin/grep -E 'VOICE_INPUT|LOCAL_ASR' "$CHRONOLOGY" | /usr/bin/tail -n 180 > "$EVIDENCE/voice-local-asr-chronology.jsonl" || true; fi
SERVICE_ERR="$HOME_PATH/runtime/product-service/logs/service.err.log"
if [ -f "$SERVICE_ERR" ]; then /usr/bin/tail -n 180 "$SERVICE_ERR" > "$EVIDENCE/service-stderr-tail.txt" || true; fi

STATUS='PASS'
DETAIL='source/build passed; both Chinese reply scripts observed; Follow resolved both to spoken zh; Base Multilingual completed partial+final As-spoken and Translate-to-English inference; Composer/script/voice output remains human-visible witness; Send remains separate'
package_return || exit 80
