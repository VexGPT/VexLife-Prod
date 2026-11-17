#!/bin/bash
set -u
set -o pipefail

# [VXG RealForever]
# Disposable direct JVM -> whisper.cpp JNA seam probe. No Product/Home mutation.

WHISPER_REPO='https://github.com/ggml-org/whisper.cpp.git'
WHISPER_COMMIT='d1be6fde11ac6e0407606b4e42fe72d34add8037'
WHISPER_TREE='6fe2755682242a5c01aba65a22b3c0305d747841'

MODEL_URL='https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.en.bin'
MODEL_SHA='a03779c86df3323075f5e796cb2ce5029f00ec8869eee3fdfb897afe36c6d002'
MODEL_BYTES='147964211'

FIXTURE_ARCHIVE_URL='https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-streaming-zipformer-en-20M-2023-02-17.tar.bz2'
FIXTURE_ARCHIVE_SHA='9c559283e8498d3fe95913c79ca1cb454bb26281ac2b102b41306c7d752765d9'
FIXTURE_DIR='sherpa-onnx-streaming-zipformer-en-20M-2023-02-17'
FIXTURE_WAV_SHA='6bc58a4efdf20daac252b6b1502632601a71efe0308f6757dc1eda34891a7e4f'

CMAKE_URL='https://github.com/Kitware/CMake/releases/download/v4.5.0/cmake-4.5.0-macos-universal.tar.gz'
CMAKE_SHA='a645d69fd21e521759cc0c64e7c60462d43faf0c7214ac05f75c80f7c5037dde'

RETURN="$HOME/Downloads/RETURN-VEXLIFE-LOCAL-ASR-WHISPERCPP-JVM-A003.zip"

ROOT="$(/usr/bin/mktemp -d "$HOME/Downloads/.vexlife-local-asr-whispercpp-jvm-a003.XXXXXX")" || exit 10
SRC="$ROOT/whisper.cpp"
ART="$ROOT/artifacts"
PROBE="$ROOT/probe"
EVIDENCE="$ROOT/evidence"
CMAKE_ROOT="$ROOT/cmake"
GRADLE_USER_HOME="$ROOT/gradle-home"
/bin/mkdir -p "$ART" "$PROBE/src/main/java" "$EVIDENCE" "$CMAKE_ROOT" "$GRADLE_USER_HOME"
export GRADLE_USER_HOME

STATUS='FAIL'
DETAIL='probe did not reach terminal disposition'

cleanup() {
  case "$ROOT" in
    "$HOME/Downloads"/.vexlife-local-asr-whispercpp-jvm-a003.*) /bin/rm -rf -- "$ROOT" ;;
  esac
}
trap cleanup EXIT HUP INT TERM

package_return() {
  {
    printf 'VexLife Local ASR whisper.cpp JVM A003\n'
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

printf '=== VexLife Local ASR whisper.cpp JVM A003 ===\n'

[ "$(/usr/bin/uname -s)" = 'Darwin' ] || fail 'A003 currently qualifies macOS only' 11
[ "$(/usr/bin/uname -m)" = 'arm64' ] || fail 'A003 currently qualifies Apple Silicon arm64 only' 12
[ -x /usr/bin/git ] || fail 'git is unavailable' 13
/usr/bin/xcrun --find clang >/dev/null 2>&1 || fail 'Apple clang/Xcode command-line tools are unavailable' 14

JAVA_HOME="$(/usr/libexec/java_home 2>/dev/null)" || fail 'no macOS Java Home is available' 15
[ -x "$JAVA_HOME/bin/java" ] || fail 'resolved java executable missing' 16
[ -x "$JAVA_HOME/bin/javac" ] || fail 'resolved javac executable missing' 17
export JAVA_HOME

{
  /usr/bin/sw_vers
  /usr/bin/uname -a
  /usr/bin/xcrun clang --version
  "$JAVA_HOME/bin/java" -version
  "$JAVA_HOME/bin/javac" -version
  printf 'JAVA_HOME=%s\n' "$JAVA_HOME"
} > "$EVIDENCE/host.txt" 2>&1

CMAKE_BIN=''
for candidate in /opt/homebrew/bin/cmake /usr/local/bin/cmake /Applications/CMake.app/Contents/bin/cmake; do
  if [ -x "$candidate" ]; then
    CMAKE_BIN="$candidate"
    break
  fi
done

if [ -z "$CMAKE_BIN" ]; then
  CMAKE_ARCHIVE="$ART/cmake-4.5.0-macos-universal.tar.gz"
  download "$CMAKE_URL" "$CMAKE_ARCHIVE" || fail 'temporary CMake download failed' 20
  ACTUAL_CMAKE_SHA="$(sha_of "$CMAKE_ARCHIVE")"
  [ "$ACTUAL_CMAKE_SHA" = "$CMAKE_SHA" ] || fail 'temporary CMake digest mismatch' 21
  /usr/bin/tar -xzf "$CMAKE_ARCHIVE" -C "$CMAKE_ROOT" || fail 'temporary CMake extraction failed' 22
  CMAKE_BIN="$(
    /usr/bin/find "$CMAKE_ROOT" -type f -path '*/CMake.app/Contents/bin/cmake' -perm -111 -print 2>/dev/null |
      /usr/bin/head -n 1
  )"
  [ -n "$CMAKE_BIN" ] && [ -x "$CMAKE_BIN" ]     || fail 'temporary CMake executable missing after deterministic extracted-path discovery' 23
  {
    printf 'cmakeSource=TEMPORARY_PINNED_4.5.0\n'
    printf 'cmakeArchiveSha256=%s\n' "$ACTUAL_CMAKE_SHA"
  } > "$EVIDENCE/cmake-source.txt"
else
  {
    printf 'cmakeSource=ALREADY_INSTALLED\n'
    printf 'cmakePath=%s\n' "$CMAKE_BIN"
  } > "$EVIDENCE/cmake-source.txt"
fi
"$CMAKE_BIN" --version >> "$EVIDENCE/cmake-source.txt" 2>&1

printf 'Cloning pinned whisper.cpp source...\n'
/usr/bin/git clone --no-checkout --filter=blob:none "$WHISPER_REPO" "$SRC"   > "$EVIDENCE/git-clone.stdout.txt"   2> "$EVIDENCE/git-clone.stderr.txt"   || fail 'whisper.cpp clone failed' 30

/usr/bin/git -C "$SRC" checkout --detach "$WHISPER_COMMIT"   > "$EVIDENCE/git-checkout.stdout.txt"   2> "$EVIDENCE/git-checkout.stderr.txt"   || fail 'whisper.cpp pinned checkout failed' 31

ACTUAL_COMMIT="$(/usr/bin/git -C "$SRC" rev-parse HEAD)"
ACTUAL_TREE="$(/usr/bin/git -C "$SRC" rev-parse 'HEAD^{tree}')"
[ "$ACTUAL_COMMIT" = "$WHISPER_COMMIT" ] || fail 'whisper.cpp commit mismatch' 32
[ "$ACTUAL_TREE" = "$WHISPER_TREE" ] || fail 'whisper.cpp tree mismatch' 33

printf 'Building pinned shared native whisper library...\n'
"$CMAKE_BIN"   -S "$SRC"   -B "$SRC/build"   -DCMAKE_BUILD_TYPE=Release   -DWHISPER_BUILD_TESTS=OFF   -DWHISPER_BUILD_SERVER=OFF   -DWHISPER_BUILD_EXAMPLES=OFF   > "$EVIDENCE/cmake-configure.stdout.txt"   2> "$EVIDENCE/cmake-configure.stderr.txt"   || fail 'whisper.cpp CMake configure failed' 34

CPU_COUNT="$(/usr/sbin/sysctl -n hw.ncpu 2>/dev/null || printf '4')"
"$CMAKE_BIN"   --build "$SRC/build"   --config Release   --target whisper   --parallel "$CPU_COUNT"   > "$EVIDENCE/cmake-build.stdout.txt"   2> "$EVIDENCE/cmake-build.stderr.txt"   || fail 'whisper shared library build failed' 35

NATIVE_DIR="$SRC/build/bin"
WHISPER_DYLIB="$NATIVE_DIR/libwhisper.dylib"
[ -f "$WHISPER_DYLIB" ] || fail 'libwhisper.dylib was not formed' 36
/usr/bin/otool -L "$WHISPER_DYLIB" > "$EVIDENCE/libwhisper-otool.txt" 2>&1   || fail 'otool could not inspect libwhisper.dylib' 37
/usr/bin/shasum -a 256 "$NATIVE_DIR"/*.dylib > "$EVIDENCE/native-dylib-sha256.txt"

printf 'Downloading pinned model and shared fixture...\n'
MODEL="$ART/ggml-base.en.bin"
download "$MODEL_URL" "$MODEL" || fail 'Whisper model download failed' 40
ACTUAL_MODEL_SHA="$(sha_of "$MODEL")"
ACTUAL_MODEL_BYTES="$(/usr/bin/stat -f '%z' "$MODEL")"
[ "$ACTUAL_MODEL_SHA" = "$MODEL_SHA" ] || fail 'Whisper model digest mismatch' 41
[ "$ACTUAL_MODEL_BYTES" = "$MODEL_BYTES" ] || fail 'Whisper model byte length mismatch' 42

FIXTURE_ARCHIVE="$ART/$FIXTURE_DIR.tar.bz2"
download "$FIXTURE_ARCHIVE_URL" "$FIXTURE_ARCHIVE" || fail 'fixture archive download failed' 43
[ "$(sha_of "$FIXTURE_ARCHIVE")" = "$FIXTURE_ARCHIVE_SHA" ] || fail 'fixture archive digest mismatch' 44
/usr/bin/tar -xjf "$FIXTURE_ARCHIVE" -C "$ART" "$FIXTURE_DIR/test_wavs/0.wav"   || fail 'fixture extraction failed' 45
WAV="$ART/$FIXTURE_DIR/test_wavs/0.wav"
[ "$(sha_of "$WAV")" = "$FIXTURE_WAV_SHA" ] || fail 'fixture WAV digest mismatch' 46

cat > "$PROBE/settings.gradle" <<'GRADLESETTINGS'
rootProject.name = 'vexlife-whisper-jvm-probe'
GRADLESETTINGS

cat > "$PROBE/build.gradle" <<'GRADLEBUILD'
plugins {
    id 'application'
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'net.java.dev.jna:jna:5.13.0'
}

sourceSets {
    main {
        java {
            srcDirs = [
                System.getenv('WHISPER_JAVA_SRC'),
                'src/main/java'
            ]
        }
    }
}

application {
    mainClass = 'VexLifeWhisperJvmProbe'
}

tasks.withType(JavaExec).configureEach {
    systemProperty 'jna.library.path', System.getenv('WHISPER_NATIVE_DIR')
    environment 'DYLD_LIBRARY_PATH', System.getenv('WHISPER_NATIVE_DIR')
}
GRADLEBUILD

cat > "$PROBE/src/main/java/VexLifeWhisperJvmProbe.java" <<'JAVA'
import io.github.ggerganov.whispercpp.WhisperCpp;
import io.github.ggerganov.whispercpp.params.WhisperFullParams;
import io.github.ggerganov.whispercpp.params.WhisperSamplingStrategy;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public final class VexLifeWhisperJvmProbe {
    private static float[] readMono16Pcm(Path wav) throws Exception {
        try (AudioInputStream input = AudioSystem.getAudioInputStream(wav.toFile())) {
            AudioFormat format = input.getFormat();
            if (format.getSampleRate() != 16000.0f
                    || format.getSampleSizeInBits() != 16
                    || format.getChannels() != 1
                    || format.isBigEndian()) {
                throw new IllegalArgumentException("fixture must be 16 kHz mono signed little-endian PCM");
            }

            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) >= 0) {
                if (count > 0) {
                    bytes.write(buffer, 0, count);
                }
            }

            byte[] pcm = bytes.toByteArray();
            if ((pcm.length & 1) != 0) {
                throw new IllegalArgumentException("PCM byte count must be even");
            }

            float[] samples = new float[pcm.length / 2];
            for (int i = 0; i < samples.length; i++) {
                int low = pcm[i * 2] & 0xff;
                int high = pcm[i * 2 + 1];
                short value = (short) ((high << 8) | low);
                samples[i] = value / 32768.0f;
            }
            return samples;
        }
    }

    private static long millisSince(long startNanos) {
        return Math.round((System.nanoTime() - startNanos) / 1_000_000.0);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            throw new IllegalArgumentException(
                    "usage: VexLifeWhisperJvmProbe <native-dir> <model> <wav> <result>");
        }

        Path nativeDir = Path.of(args[0]).toAbsolutePath().normalize();
        Path model = Path.of(args[1]).toAbsolutePath().normalize();
        Path wav = Path.of(args[2]).toAbsolutePath().normalize();
        Path result = Path.of(args[3]).toAbsolutePath().normalize();

        System.setProperty("jna.library.path", nativeDir.toString());

        float[] samples = readMono16Pcm(wav);

        long wholeStart = System.nanoTime();
        long initStart = System.nanoTime();

        String transcript;
        String systemInfo;
        long initMs;
        long inferenceMs;

        try (WhisperCpp whisper = new WhisperCpp()) {
            whisper.initContext(model.toString());
            initMs = millisSince(initStart);
            systemInfo = whisper.getSystemInfo();

            WhisperFullParams.ByValue params =
                    whisper.getFullDefaultParams(
                            WhisperSamplingStrategy.WHISPER_SAMPLING_BEAM_SEARCH);
            params.n_threads = 4;
            params.language = "en";
            params.transcribeMode();
            params.printProgress(false);
            params.printRealtime(false);
            params.printTimestamps(false);
            params.write();

            long inferenceStart = System.nanoTime();
            transcript = whisper.fullTranscribe(params, samples);
            inferenceMs = millisSince(inferenceStart);
        }

        long wholeMs = millisSince(wholeStart);
        double audioSeconds = samples.length / 16000.0;
        double realTimeFactor =
                audioSeconds > 0
                        ? (inferenceMs / 1000.0) / audioSeconds
                        : -1.0;

        try (PrintWriter out =
                new PrintWriter(result.toFile(), StandardCharsets.UTF_8)) {
            out.println("schema=vexlife-local-asr-whispercpp-jvm-probe/v1");
            out.println("providerRef=provider.local-asr.whispercpp");
            out.println("bindingRef=whispercpp-java-jna");
            out.println("bindingSource=whisper.cpp-v1.9.5-pinned-source");
            out.println("sampleCount=" + samples.length);
            out.printf("audioSeconds=%.3f%n", audioSeconds);
            out.println("initMs=" + initMs);
            out.println("inferenceMs=" + inferenceMs);
            out.println("wholeMs=" + wholeMs);
            out.printf("realTimeFactor=%.4f%n", realTimeFactor);
            out.println("finalTranscript=" + transcript.replaceAll("\\s+", " ").trim());
            out.println("systemInfo=" + systemInfo.replaceAll("\\s+", " ").trim());
        }

        System.out.printf(
                "RESULT initMs=%d inferenceMs=%d wholeMs=%d rtf=%.4f transcript=%s%n",
                initMs,
                inferenceMs,
                wholeMs,
                realTimeFactor,
                transcript.replaceAll("\\s+", " ").trim());
    }
}
JAVA

export WHISPER_JAVA_SRC="$SRC/bindings/java/src/main/java"
export WHISPER_NATIVE_DIR="$NATIVE_DIR"

printf 'Compiling and running pinned whisper.cpp Java/JNA binding...\n'
set +e
(
  cd "$SRC/bindings/java" || exit 70
  /usr/bin/time -l     ./gradlew       --no-daemon       -p "$PROBE"       run       --args="$NATIVE_DIR $MODEL $WAV $EVIDENCE/provider-result.txt"
) > "$EVIDENCE/gradle.stdout.txt" 2> "$EVIDENCE/gradle.stderr-and-time.txt"
GRADLE_STATUS=$?
set -e

[ "$GRADLE_STATUS" -eq 0 ] || fail "JVM/JNA probe exited $GRADLE_STATUS" 50
[ -s "$EVIDENCE/provider-result.txt" ] || fail 'JVM/JNA provider result was not formed' 51
/usr/bin/grep -q '^finalTranscript=.' "$EVIDENCE/provider-result.txt"   || fail 'JVM/JNA provider formed no final transcript' 52

JNA_JAR="$(
  /usr/bin/find "$GRADLE_USER_HOME/caches" -type f -name 'jna-5.13.0.jar' -print 2>/dev/null |
    /usr/bin/head -n 1
)"
if [ -n "$JNA_JAR" ] && [ -f "$JNA_JAR" ]; then
  {
    printf 'jnaPath=%s\n' "$JNA_JAR"
    printf 'jnaSha256=%s\n' "$(sha_of "$JNA_JAR")"
    printf 'jnaBytes=%s\n' "$(/usr/bin/stat -f '%z' "$JNA_JAR")"
  } > "$EVIDENCE/jna-artifact.txt"
else
  printf 'jnaArtifact=NOT_LOCATED_AFTER_SUCCESSFUL_GRADLE_RUN\n' > "$EVIDENCE/jna-artifact.txt"
fi

{
  printf 'providerSourceCommit=%s\n' "$ACTUAL_COMMIT"
  printf 'providerSourceTree=%s\n' "$ACTUAL_TREE"
  printf 'providerRelease=v1.9.5\n'
  printf 'providerLicense=MIT\n'
  printf 'bindingSource=whisper.cpp/bindings/java@same-pinned-commit\n'
  printf 'bindingDependency=net.java.dev.jna:jna:5.13.0\n'
  printf 'modelRef=ggml-base.en.bin\n'
  printf 'modelSha256=%s\n' "$ACTUAL_MODEL_SHA"
  printf 'modelBytes=%s\n' "$ACTUAL_MODEL_BYTES"
  printf 'fixtureSha256=%s\n' "$FIXTURE_WAV_SHA"
  printf 'fixtureAudioSeconds=6.625\n'
  printf 'productHomeMutation=false\n'
  printf 'hereticMutation=false\n'
  printf 'productSourceMutation=false\n'
  printf 'cloudTranscription=false\n'
} > "$EVIDENCE/manifest.txt"

cat > "$EVIDENCE/reference.txt" <<'REFERENCE'
AFTER EARLY NIGHTFALL THE YELLOW LAMPS WOULD LIGHT UP HERE AND THERE THE SQUALID QUARTER OF THE BROTHELS
REFERENCE

STATUS='PASS'
DETAIL='pinned whisper.cpp Java/JNA binding called the locally-built native library directly from JVM and produced a transcript from the shared float-PCM fixture'
package_return || exit 60
