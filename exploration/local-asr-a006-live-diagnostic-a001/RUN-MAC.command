#!/bin/bash
set -u
set -o pipefail

# [VXG RealForever]
# Read-only live diagnostic for A006. It does not stop/restart services, mutate Vex Home,
# invoke Conversation, use the microphone, or capture transcript/audio content.

EXPECTED_SOURCE='0972e3a75f39157bc069eb6abd88bb49e967f88b'
BASE='http://127.0.0.1:18210'
RETURN="$HOME/Downloads/RETURN-VEXLIFE-LOCAL-ASR-A006-LIVE-DIAGNOSTIC-A001.zip"

ROOT="$(/usr/bin/mktemp -d "$HOME/Downloads/.vexlife-local-asr-a006-diagnostic.XXXXXX")" || exit 10
E="$ROOT/evidence"
/bin/mkdir -p "$E/endpoints" "$E/runtime" "$E/presentation" "$E/provider"

cleanup() {
  case "$ROOT" in
    "$HOME/Downloads"/.vexlife-local-asr-a006-diagnostic.*)
      /bin/rm -rf -- "$ROOT"
      ;;
  esac
}
trap cleanup EXIT HUP INT TERM

fetch_endpoint() {
  local path="$1"
  local name="$2"
  local headers="$E/endpoints/$name.headers.txt"
  local body="$E/endpoints/$name.body.json"
  local code

  code="$(
    /usr/bin/curl       -sS       --max-time 6       -D "$headers"       -o "$body"       -w '%{http_code}'       "$BASE$path"       2> "$E/endpoints/$name.curl.stderr.txt"       || true
  )"

  printf '%s\t%s\t%s\n' "$path" "${code:-000}" "$name" >> "$E/endpoints/index.tsv"
}

fetch_hash_only() {
  local path="$1"
  local name="$2"
  local tmp="$ROOT/$name.payload"
  local code

  code="$(
    /usr/bin/curl       -sS       --max-time 6       -D "$E/presentation/$name.headers.txt"       -o "$tmp"       -w '%{http_code}'       "$BASE$path"       2> "$E/presentation/$name.curl.stderr.txt"       || true
  )"

  {
    printf 'path=%s\n' "$path"
    printf 'httpStatus=%s\n' "${code:-000}"
    if [ -f "$tmp" ]; then
      printf 'bytes=%s\n' "$(/usr/bin/stat -f '%z' "$tmp" 2>/dev/null || printf 'UNKNOWN')"
      printf 'sha256=%s\n' "$(/usr/bin/shasum -a 256 "$tmp" | /usr/bin/awk '{print $1}')"
    fi
  } > "$E/presentation/$name.identity.txt"

  if [ "$name" = 'manifest' ] && [ -f "$tmp" ]; then
    /bin/cp "$tmp" "$E/presentation/vexweb-manifest.json"
  fi

  /bin/rm -f "$tmp"
}

printf '=== VexLife A006 live diagnostic ===\n'
printf 'This is read-only. Leave the A006 window/service as-is.\n\n'

{
  /usr/bin/sw_vers
  /usr/bin/uname -a
  printf 'uid=%s\n' "$(/usr/bin/id -u)"
  printf 'expectedSourceSha=%s\n' "$EXPECTED_SOURCE"
  printf 'formedAt=%s\n' "$(/bin/date -u '+%Y-%m-%dT%H:%M:%SZ')"
} > "$E/host.txt" 2>&1

printf '[1/5] Reading localhost projections...\n'
: > "$E/endpoints/index.tsv"
fetch_endpoint '/api/v1/source-currentness' 'source-currentness'
fetch_endpoint '/api/v1/current-state' 'current-state'
fetch_endpoint '/api/v1/settings' 'settings'
fetch_endpoint '/api/v1/strings/workspace' 'workspace-strings'
fetch_endpoint '/api/v1/local-asr' 'local-asr'
fetch_endpoint '/api/v1/models' 'models'
fetch_endpoint '/api/v1/diagnostics' 'diagnostics'

printf '[2/5] Identifying served presentation bytes...\n'
fetch_hash_only '/' 'index'
fetch_hash_only '/vexweb.js' 'javascript'
fetch_hash_only '/vexweb.css' 'css'
fetch_hash_only '/vexweb-manifest.json' 'manifest'

HOME_PATH=''
OBSERVED_SOURCE=''
SERVICE_PID=''
RUNTIME_RECEIPT=''

if [ -x /usr/bin/python3 ]; then
  read -r HOME_PATH OBSERVED_SOURCE SERVICE_PID RUNTIME_RECEIPT <<EOF
$(/usr/bin/python3 - "$E/endpoints/current-state.body.json" "$E/endpoints/source-currentness.body.json" <<'PY'
import json,sys

def read(path):
    try:
        with open(path, encoding="utf-8") as f:
            return json.load(f)
    except Exception:
        return {}

current = read(sys.argv[1])
source = read(sys.argv[2])

home = current.get("homePath") or ""
sha = source.get("sourceSha") or ""
pid = source.get("processPid") or ""
receipt = current.get("runtimeReceiptPath") or ""

print(str(home), str(sha), str(pid), str(receipt))
PY
)
EOF
fi

if [ -z "$HOME_PATH" ] && [ -f "$HOME/.vexlife/config/home.json" ]; then
  HOME_PATH="$HOME/.vexlife"
fi

{
  printf 'expectedSourceSha=%s\n' "$EXPECTED_SOURCE"
  printf 'observedSourceSha=%s\n' "$OBSERVED_SOURCE"
  printf 'sourceMatchesExpected=%s\n' "$([ "$OBSERVED_SOURCE" = "$EXPECTED_SOURCE" ] && printf 'true' || printf 'false')"
  printf 'homePath=%s\n' "$HOME_PATH"
  printf 'servicePid=%s\n' "$SERVICE_PID"
  printf 'runtimeReceiptPath=%s\n' "$RUNTIME_RECEIPT"
} > "$E/live-coordinate.txt"

printf '[3/5] Reading bounded Product runtime evidence...\n'
if [ -n "$HOME_PATH" ] && [ -d "$HOME_PATH" ]; then
  for relative in     'runtime/product-service/service-lifetime.json'     'runtime/product-service/launchd/com.vexlife.prod.service.plist'     'runtime/vexlife-prod/settings/person.json'     'runtime/vexlife-prod/settings/local-asr.json'
  do
    source="$HOME_PATH/$relative"
    target="$E/runtime/$(printf '%s' "$relative" | /usr/bin/tr '/' '_')"
    if [ -f "$source" ]; then
      /bin/cp "$source" "$target"
    fi
  done

  if [ -n "$RUNTIME_RECEIPT" ] && [ -x /usr/bin/python3 ]; then
    /usr/bin/python3 - "$HOME_PATH" "$RUNTIME_RECEIPT" "$E/runtime/heretic-current-runtime-receipt.json" <<'PY'
from pathlib import Path
import shutil,sys

home = Path(sys.argv[1]).resolve()
raw = Path(sys.argv[2])
path = raw if raw.is_absolute() else home / raw
try:
    path = path.resolve()
    if home == path or home in path.parents:
        if path.is_file():
            shutil.copyfile(path, sys.argv[3])
except Exception:
    pass
PY
  fi

  STDERR="$HOME_PATH/runtime/product-service/logs/service.err.log"
  STDOUT="$HOME_PATH/runtime/product-service/logs/service.out.log"
  CHRON="$HOME_PATH/runtime/vexlife-prod/logs/chronology/chronology.jsonl"

  if [ -f "$STDERR" ]; then
    /usr/bin/tail -n 300 "$STDERR" > "$E/runtime/service-stderr-tail.txt"
  fi
  if [ -f "$STDOUT" ]; then
    /usr/bin/tail -n 120 "$STDOUT" > "$E/runtime/service-stdout-tail.txt"
  fi
  if [ -f "$CHRON" ]; then
    /usr/bin/grep -E       '"fact":"(ENTRY_|SETUP_|SERVICE_|RUNTIME_|MODEL_|LOCAL_ASR_|VOICE_INPUT_|PRESENTATION_ARRIVED|DEBUG_|PROCESS_)'       "$CHRON" |
      /usr/bin/tail -n 500       > "$E/runtime/relevant-chronology.jsonl"       || true
  fi

  PROVIDER_ROOT="$HOME_PATH/runtime/vexlife-prod/local-asr/whispercpp/v1.9.5"
  if [ -d "$PROVIDER_ROOT" ]; then
    {
      printf 'providerRoot=%s\n' "$PROVIDER_ROOT"
      if [ -f "$PROVIDER_ROOT/provider-source.txt" ]; then
        printf 'providerSource=%s\n' "$(/bin/cat "$PROVIDER_ROOT/provider-source.txt" 2>/dev/null || true)"
      fi
      if [ -f "$PROVIDER_ROOT/models/ggml-base.en.bin" ]; then
        printf 'modelBytes=%s\n' "$(/usr/bin/stat -f '%z' "$PROVIDER_ROOT/models/ggml-base.en.bin" 2>/dev/null || true)"
        printf 'modelSha256=%s\n' "$(/usr/bin/shasum -a 256 "$PROVIDER_ROOT/models/ggml-base.en.bin" | /usr/bin/awk '{print $1}')"
      fi
      printf '%s\n' '--- bin inventory ---'
      /usr/bin/find "$PROVIDER_ROOT/bin" -maxdepth 1 \( -type f -o -type l \) -print 2>/dev/null |
        /usr/bin/sort
      printf '%s\n' '--- symlinks ---'
      /usr/bin/find "$PROVIDER_ROOT/bin" -maxdepth 1 -type l -print0 2>/dev/null |
        while IFS= read -r -d '' link; do
          printf '%s -> %s\n' "$(/usr/bin/basename "$link")" "$(/usr/bin/readlink "$link")"
        done
      if [ -x "$PROVIDER_ROOT/bin/whisper-cli" ]; then
        printf '%s\n' '--- whisper-cli dependencies ---'
        /usr/bin/otool -L "$PROVIDER_ROOT/bin/whisper-cli" 2>&1 || true
        printf '%s\n' '--- whisper-cli version ---'
        DYLD_LIBRARY_PATH="$PROVIDER_ROOT/bin"           "$PROVIDER_ROOT/bin/whisper-cli" --version 2>&1 || true
      fi
    } > "$E/provider/current-provider.txt"
  fi
fi

if [ -n "$SERVICE_PID" ]; then
  /bin/ps -p "$SERVICE_PID" -ww -o pid=,ppid=,pgid=,etime=,command=     > "$E/runtime/service-process.txt" 2>&1 || true
fi

/usr/bin/launchctl print "gui/$UID/com.vexlife.prod.service"   > "$E/runtime/launchd-service.txt" 2>&1 || true

printf '[4/5] Summarizing local endpoint state...\n'
if [ -x /usr/bin/python3 ]; then
  /usr/bin/python3 - "$E" <<'PY'
from pathlib import Path
import json,sys

root = Path(sys.argv[1])
endpoint_dir = root / "endpoints"

def read_json(name):
    path = endpoint_dir / f"{name}.body.json"
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except Exception:
        return None

rows = []
index = endpoint_dir / "index.tsv"
if index.exists():
    for line in index.read_text(encoding="utf-8").splitlines():
        parts = line.split("\t")
        if len(parts) == 3:
            rows.append(parts)

with (root / "endpoint-summary.txt").open("w", encoding="utf-8") as out:
    for path, status, name in rows:
        body = read_json(name)
        out.write(f"{path}\n")
        out.write(f"  HTTP {status}\n")
        if isinstance(body, dict):
            if "code" in body:
                out.write(f"  errorCode={body.get('code')}\n")
                out.write(f"  message={body.get('message')}\n")
            if name == "source-currentness":
                out.write(f"  sourceSha={body.get('sourceSha')}\n")
                out.write(f"  processPid={body.get('processPid')}\n")
            elif name == "local-asr":
                out.write(f"  selected={body.get('selectedProviderRef')} / {body.get('selectedModelRef')}\n")
                for option in body.get("options") or []:
                    if option.get("selected"):
                        out.write(
                            "  current="
                            + str(option.get("currentState"))
                            + " / "
                            + str(option.get("currentDetailRef"))
                            + "\n"
                        )
            elif name == "workspace-strings":
                strings = body.get("strings") or {}
                out.write(f"  stringCount={len(strings)}\n")
                for key in [
                    "workspace.heading.local-asr",
                    "workspace.status.local-asr-ready",
                    "workspace.status.local-asr-unavailable",
                    "workspace.heading.models",
                    "workspace.heading.diagnostics",
                ]:
                    out.write(f"  {key}={strings.get(key)!r}\n")
            elif name == "models":
                out.write(f"  modelCount={len(body.get('models') or [])}\n")
            elif name == "diagnostics":
                out.write(f"  diagnosticsState={body.get('state')}\n")
        else:
            raw = (endpoint_dir / f"{name}.body.json")
            if raw.exists():
                out.write("  bodyPrefix=" + raw.read_text(encoding="utf-8", errors="replace")[:500].replace("\n"," ") + "\n")
        out.write("\n")
PY
fi

printf '[5/5] Forming RETURN ZIP...\n'
{
  printf 'VexLife Local ASR A006 Live Diagnostic A001\n'
  printf 'status=COLLECTED\n'
  printf 'effectsPerformed=false\n'
  printf 'expectedSourceSha=%s\n' "$EXPECTED_SOURCE"
  printf 'observedSourceSha=%s\n' "$OBSERVED_SOURCE"
  printf 'sourceMatchesExpected=%s\n' "$([ "$OBSERVED_SOURCE" = "$EXPECTED_SOURCE" ] && printf 'true' || printf 'false')"
  printf 'home=%s\n' "$HOME_PATH"
  printf 'privacy=NO_CONVERSATION_BODY_NO_PROMPTS_NO_MIC_AUDIO_NO_TRANSCRIPT\n'
  printf 'formedAt=%s\n' "$(/bin/date -u '+%Y-%m-%dT%H:%M:%SZ')"
} > "$E/RESULT-SUMMARY.txt"

/bin/rm -f "$RETURN"
(
  cd "$E" || exit 1
  /usr/bin/zip -qry "$RETURN" .
) || exit 20

printf '\nRETURN: %s\n' "$RETURN"
/usr/bin/ls -lh "$RETURN"
/usr/bin/shasum -a 256 "$RETURN"
