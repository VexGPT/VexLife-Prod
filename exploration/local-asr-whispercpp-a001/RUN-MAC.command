#!/bin/bash
set -u
set -o pipefail

# [VXG RealForever]
# Disposable whisper.cpp quality/footprint comparator. No Product/Home mutation.

WHISPER_REPO='https://github.com/ggml-org/whisper.cpp.git'
WHISPER_COMMIT='d1be6fde11ac6e0407606b4e42fe72d34add8037'
WHISPER_TREE='6fe2755682242a5c01aba65a22b3c0305d747841'

WHISPER_MODEL_URL='https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.en.bin'
WHISPER_MODEL_SHA='a03779c86df3323075f5e796cb2ce5029f00ec8869eee3fdfb897afe36c6d002'
WHISPER_MODEL_BYTES='147964211'

FIXTURE_ARCHIVE_URL='https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-streaming-zipformer-en-20M-2023-02-17.tar.bz2'
FIXTURE_ARCHIVE_SHA='9c559283e8498d3fe95913c79ca1cb454bb26281ac2b102b41306c7d752765d9'
FIXTURE_DIR='sherpa-onnx-streaming-zipformer-en-20M-2023-02-17'
FIXTURE_WAV_SHA='6bc58a4efdf20daac252b6b1502632601a71efe0308f6757dc1eda34891a7e4f'

CMAKE_URL='https://github.com/Kitware/CMake/releases/download/v4.5.0/cmake-4.5.0-macos-universal.tar.gz'
CMAKE_SHA='a645d69fd21e521759cc0c64e7c60462d43faf0c7214ac05f75c80f7c5037dde'

RETURN="$HOME/Downloads/RETURN-VEXLIFE-LOCAL-ASR-WHISPERCPP-A001.zip"

ROOT="$(/usr/bin/mktemp -d "$HOME/Downloads/.vexlife-local-asr-whispercpp-a001.XXXXXX")" || exit 10
SRC="$ROOT/whisper.cpp"
ART="$ROOT/artifacts"
EVIDENCE="$ROOT/evidence"
CMAKE_ROOT="$ROOT/cmake"
/bin/mkdir -p "$ART" "$EVIDENCE" "$CMAKE_ROOT"

STATUS='FAIL'
DETAIL='probe did not reach terminal disposition'

cleanup() {
  case "$ROOT" in
    "$HOME/Downloads"/.vexlife-local-asr-whispercpp-a001.*) /bin/rm -rf -- "$ROOT" ;;
  esac
}
trap cleanup EXIT HUP INT TERM

package_return() {
  {
    printf 'VexLife Local ASR whisper.cpp A001\n'
    printf 'status=%s\n' "$STATUS"
    printf 'detail=%s\n' "$DETAIL"
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

fail() {
  DETAIL="$1"
  STATUS='FAIL'
  printf 'FAIL: %s\n' "$DETAIL" >&2
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

printf '=== VexLife Local ASR whisper.cpp A001 ===\n'

[ "$(/usr/bin/uname -s)" = 'Darwin' ] || fail 'A001 currently qualifies macOS only' 11
[ "$(/usr/bin/uname -m)" = 'arm64' ] || fail 'A001 currently qualifies Apple Silicon arm64 only' 12
[ -x /usr/bin/git ] || fail 'git is unavailable' 13
/usr/bin/xcrun --find clang >/dev/null 2>&1 || fail 'Apple clang/Xcode command-line tools are unavailable' 14

{
  /usr/bin/sw_vers
  /usr/bin/uname -a
  /usr/bin/xcrun clang --version
} > "$EVIDENCE/host.txt" 2>&1

CMAKE_BIN=''
for candidate in /opt/homebrew/bin/cmake /usr/local/bin/cmake /Applications/CMake.app/Contents/bin/cmake; do
  if [ -x "$candidate" ]; then
    CMAKE_BIN="$candidate"
    break
  fi
done

if [ -z "$CMAKE_BIN" ]; then
  printf 'No installed CMake found; using pinned temporary CMake 4.5.0...\n'
  CMAKE_ARCHIVE="$ART/cmake-4.5.0-macos-universal.tar.gz"
  download "$CMAKE_URL" "$CMAKE_ARCHIVE" || fail 'temporary CMake download failed' 20
  ACTUAL_CMAKE_SHA="$(sha_of "$CMAKE_ARCHIVE")"
  [ "$ACTUAL_CMAKE_SHA" = "$CMAKE_SHA" ] || fail 'temporary CMake digest mismatch' 21
  /usr/bin/tar -xzf "$CMAKE_ARCHIVE" -C "$CMAKE_ROOT" || fail 'temporary CMake extraction failed' 22
  CMAKE_BIN="$CMAKE_ROOT/CMake.app/Contents/bin/cmake"
  [ -x "$CMAKE_BIN" ] || fail 'temporary CMake executable missing after extraction' 23
  printf 'cmakeSource=TEMPORARY_PINNED_4.5.0\n' > "$EVIDENCE/cmake-source.txt"
  printf 'cmakeArchiveSha256=%s\n' "$ACTUAL_CMAKE_SHA" >> "$EVIDENCE/cmake-source.txt"
else
  printf 'cmakeSource=ALREADY_INSTALLED\n' > "$EVIDENCE/cmake-source.txt"
  printf 'cmakePath=%s\n' "$CMAKE_BIN" >> "$EVIDENCE/cmake-source.txt"
fi
"$CMAKE_BIN" --version >> "$EVIDENCE/cmake-source.txt" 2>&1

printf 'Cloning pinned whisper.cpp release source...\n'
/usr/bin/git clone --no-checkout --filter=blob:none "$WHISPER_REPO" "$SRC"   > "$EVIDENCE/git-clone.stdout.txt"   2> "$EVIDENCE/git-clone.stderr.txt"   || fail 'whisper.cpp clone failed' 30

/usr/bin/git -C "$SRC" checkout --detach "$WHISPER_COMMIT"   > "$EVIDENCE/git-checkout.stdout.txt"   2> "$EVIDENCE/git-checkout.stderr.txt"   || fail 'whisper.cpp pinned checkout failed' 31

ACTUAL_COMMIT="$(/usr/bin/git -C "$SRC" rev-parse HEAD)"
ACTUAL_TREE="$(/usr/bin/git -C "$SRC" rev-parse 'HEAD^{tree}')"
[ "$ACTUAL_COMMIT" = "$WHISPER_COMMIT" ] || fail 'whisper.cpp commit mismatch' 32
[ "$ACTUAL_TREE" = "$WHISPER_TREE" ] || fail 'whisper.cpp tree mismatch' 33
[ -z "$(/usr/bin/git -C "$SRC" status --porcelain=v1 -uall)" ] || fail 'whisper.cpp checkout is not clean' 34

printf 'Building pinned whisper-cli...\n'
"$CMAKE_BIN"   -S "$SRC"   -B "$SRC/build"   -DCMAKE_BUILD_TYPE=Release   -DWHISPER_BUILD_TESTS=OFF   -DWHISPER_BUILD_SERVER=OFF   -DWHISPER_BUILD_EXAMPLES=ON   > "$EVIDENCE/cmake-configure.stdout.txt"   2> "$EVIDENCE/cmake-configure.stderr.txt"   || fail 'whisper.cpp CMake configure failed' 35

CPU_COUNT="$(/usr/sbin/sysctl -n hw.ncpu 2>/dev/null || printf '4')"
"$CMAKE_BIN"   --build "$SRC/build"   --config Release   --target whisper-cli   --parallel "$CPU_COUNT"   > "$EVIDENCE/cmake-build.stdout.txt"   2> "$EVIDENCE/cmake-build.stderr.txt"   || fail 'whisper-cli build failed' 36

CLI="$SRC/build/bin/whisper-cli"
[ -x "$CLI" ] || fail 'whisper-cli was not formed' 37
"$CLI" --version > "$EVIDENCE/whisper-version.txt" 2>&1 || fail 'whisper-cli version probe failed' 38

printf 'Downloading pinned Whisper Base English model...\n'
MODEL="$ART/ggml-base.en.bin"
download "$WHISPER_MODEL_URL" "$MODEL" || fail 'Whisper model download failed' 40
ACTUAL_MODEL_SHA="$(sha_of "$MODEL")"
ACTUAL_MODEL_BYTES="$(/usr/bin/stat -f '%z' "$MODEL")"
[ "$ACTUAL_MODEL_SHA" = "$WHISPER_MODEL_SHA" ] || fail 'Whisper model digest mismatch' 41
[ "$ACTUAL_MODEL_BYTES" = "$WHISPER_MODEL_BYTES" ] || fail 'Whisper model byte length mismatch' 42

printf 'Downloading exact shared deterministic fixture archive...\n'
FIXTURE_ARCHIVE="$ART/$FIXTURE_DIR.tar.bz2"
download "$FIXTURE_ARCHIVE_URL" "$FIXTURE_ARCHIVE" || fail 'fixture archive download failed' 43
ACTUAL_FIXTURE_ARCHIVE_SHA="$(sha_of "$FIXTURE_ARCHIVE")"
[ "$ACTUAL_FIXTURE_ARCHIVE_SHA" = "$FIXTURE_ARCHIVE_SHA" ] || fail 'fixture archive digest mismatch' 44

/usr/bin/tar -xjf "$FIXTURE_ARCHIVE" -C "$ART" "$FIXTURE_DIR/test_wavs/0.wav"   || fail 'fixture extraction failed' 45
WAV="$ART/$FIXTURE_DIR/test_wavs/0.wav"
[ -f "$WAV" ] || fail 'fixture WAV missing after extraction' 46
ACTUAL_WAV_SHA="$(sha_of "$WAV")"
[ "$ACTUAL_WAV_SHA" = "$FIXTURE_WAV_SHA" ] || fail 'fixture WAV digest mismatch' 47

{
  printf 'providerSourceCommit=%s\n' "$ACTUAL_COMMIT"
  printf 'providerSourceTree=%s\n' "$ACTUAL_TREE"
  printf 'providerRelease=v1.9.5\n'
  printf 'providerLicense=MIT\n'
  printf 'modelRef=ggml-base.en.bin\n'
  printf 'modelSha256=%s\n' "$ACTUAL_MODEL_SHA"
  printf 'modelBytes=%s\n' "$ACTUAL_MODEL_BYTES"
  printf 'fixtureSha256=%s\n' "$ACTUAL_WAV_SHA"
  printf 'fixtureAudioSeconds=6.625\n'
  printf 'productHomeMutation=false\n'
  printf 'hereticMutation=false\n'
  printf 'productSourceMutation=false\n'
  printf 'cloudTranscription=false\n'
} > "$EVIDENCE/manifest.txt"

cat > "$EVIDENCE/reference.txt" <<'REFERENCE'
AFTER EARLY NIGHTFALL THE YELLOW LAMPS WOULD LIGHT UP HERE AND THERE THE SQUALID QUARTER OF THE BROTHELS
REFERENCE

printf 'Running local whisper.cpp inference...\n'
OUT_BASE="$EVIDENCE/whisper-result"
set +e
/usr/bin/time -l   "$CLI"   -m "$MODEL"   -f "$WAV"   -l en   -nt   -otxt   -of "$OUT_BASE"   > "$EVIDENCE/whisper.stdout.txt"   2> "$EVIDENCE/whisper.stderr-and-time.txt"
CLI_STATUS=$?
set -e

[ "$CLI_STATUS" -eq 0 ] || fail "whisper-cli exited $CLI_STATUS" 50
[ -s "$OUT_BASE.txt" ] || fail 'whisper.cpp formed no text output file' 51

TRANSCRIPT="$(/usr/bin/tr '\n' ' ' < "$OUT_BASE.txt" | /usr/bin/sed -E 's/[[:space:]]+/ /g; s/^ //; s/ $//')"
[ -n "$TRANSCRIPT" ] || fail 'whisper.cpp transcript normalized to empty' 52
printf '%s\n' "$TRANSCRIPT" > "$EVIDENCE/final-transcript.txt"

STATUS='PASS'
DETAIL='pinned whisper.cpp v1.9.5 and ggml-base.en produced a local transcript from the exact shared deterministic fixture'
package_return || exit 60
