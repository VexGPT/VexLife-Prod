#!/bin/bash
set -u
set -o pipefail

# [VXG RealForever]
# Disposable local-ASR provider qualification. No Vex Home/Product source mutation.

JVM_URL='https://github.com/k2-fsa/sherpa-onnx/releases/download/v2.0.1/sherpa-onnx-jvm-2.0.1.jar'
JVM_SHA='8320e2457b6e38dd624fb619d3552e11ef78ffeb394622056126b16f2fd99d12'
NATIVE_URL='https://github.com/k2-fsa/sherpa-onnx/releases/download/v2.0.1/sherpa-onnx-native-lib-osx-aarch64-2.0.1.jar'
NATIVE_SHA='718a4ea4799341146ef9a0cc15daa70e6622ae4a16911952604035f396703499'
MODEL_URL='https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-streaming-zipformer-en-20M-2023-02-17.tar.bz2'
MODEL_SHA='9c559283e8498d3fe95913c79ca1cb454bb26281ac2b102b41306c7d752765d9'
MODEL_DIR='sherpa-onnx-streaming-zipformer-en-20M-2023-02-17'
RETURN="$HOME/Downloads/RETURN-VEXLIFE-LOCAL-ASR-SHERPA-A001.zip"

ROOT="$(/usr/bin/mktemp -d "$HOME/Downloads/.vexlife-local-asr-sherpa-a001.XXXXXX")" || exit 10
ART="$ROOT/artifacts"
SRC="$ROOT/source"
CLASSES="$ROOT/classes"
EVIDENCE="$ROOT/evidence"
/bin/mkdir -p "$ART" "$SRC" "$CLASSES" "$EVIDENCE"

STATUS='FAIL'
DETAIL='probe did not reach terminal disposition'

cleanup() {
  case "$ROOT" in
    "$HOME/Downloads"/.vexlife-local-asr-sherpa-a001.*) /bin/rm -rf -- "$ROOT" ;;
  esac
}
trap cleanup EXIT HUP INT TERM

package_return() {
  {
    printf 'VexLife Local ASR sherpa A001\n'
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

verify_sha() {
  local file="$1"
  local expected="$2"
  local actual
  actual="$(/usr/bin/shasum -a 256 "$file" | /usr/bin/awk '{print $1}')"
  printf '%s  %s\n' "$actual" "$(/usr/bin/basename "$file")" >> "$EVIDENCE/download-sha256.txt"
  [ "$actual" = "$expected" ]
}

printf '=== VexLife Local ASR sherpa A001 ===\n'

[ "$(/usr/bin/uname -s)" = 'Darwin' ] || fail 'A001 currently qualifies macOS only' 11
[ "$(/usr/bin/uname -m)" = 'arm64' ] || fail 'A001 currently qualifies Apple Silicon arm64 only' 12

JAVA_HOME="$(/usr/libexec/java_home -v 21 2>/dev/null)" || fail 'JDK 21 is unavailable' 13
[ -x "$JAVA_HOME/bin/java" ] || fail 'resolved JDK 21 java is not executable' 14
[ -x "$JAVA_HOME/bin/javac" ] || fail 'resolved JDK 21 javac is not executable' 15

{
  /usr/bin/sw_vers
  /usr/bin/uname -a
  "$JAVA_HOME/bin/java" -version
} > "$EVIDENCE/host.txt" 2>&1

JVM_JAR="$ART/sherpa-onnx-jvm-2.0.1.jar"
NATIVE_JAR="$ART/sherpa-onnx-native-lib-osx-aarch64-2.0.1.jar"
MODEL_ARCHIVE="$ART/$MODEL_DIR.tar.bz2"

printf 'Downloading released JVM API...\n'
download "$JVM_URL" "$JVM_JAR" || fail 'JVM API download failed' 21
verify_sha "$JVM_JAR" "$JVM_SHA" || fail 'JVM API digest mismatch' 22

printf 'Downloading released macOS ARM64 native runtime...\n'
download "$NATIVE_URL" "$NATIVE_JAR" || fail 'native runtime download failed' 23
verify_sha "$NATIVE_JAR" "$NATIVE_SHA" || fail 'native runtime digest mismatch' 24

printf 'Downloading small English streaming model...\n'
download "$MODEL_URL" "$MODEL_ARCHIVE" || fail 'model archive download failed' 25
verify_sha "$MODEL_ARCHIVE" "$MODEL_SHA" || fail 'model archive digest mismatch' 26

printf 'Extracting model...\n'
/usr/bin/tar -xjf "$MODEL_ARCHIVE" -C "$ART" || fail 'model extraction failed' 27
MODEL_ROOT="$ART/$MODEL_DIR"
[ -d "$MODEL_ROOT" ] || fail 'model root missing after extraction' 28

for required in \
  encoder-epoch-99-avg-1.onnx \
  decoder-epoch-99-avg-1.onnx \
  joiner-epoch-99-avg-1.onnx \
  tokens.txt \
  test_wavs/0.wav
do
  [ -f "$MODEL_ROOT/$required" ] || fail "model member missing: $required" 29
done

cat > "$SRC/SherpaStreamingProbe.java" <<'JAVA'
import com.k2fsa.sherpa.onnx.OnlineModelConfig;
import com.k2fsa.sherpa.onnx.OnlineRecognizer;
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig;
import com.k2fsa.sherpa.onnx.OnlineStream;
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig;
import com.k2fsa.sherpa.onnx.WaveReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class SherpaStreamingProbe {
    private static long millisSince(long startNanos) {
        return Math.round((System.nanoTime() - startNanos) / 1_000_000.0);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException(
                    "usage: SherpaStreamingProbe <model-root> <result-path>");
        }

        Path root = Path.of(args[0]).toAbsolutePath().normalize();
        Path resultPath = Path.of(args[1]).toAbsolutePath().normalize();

        String encoder = root.resolve("encoder-epoch-99-avg-1.onnx").toString();
        String decoder = root.resolve("decoder-epoch-99-avg-1.onnx").toString();
        String joiner = root.resolve("joiner-epoch-99-avg-1.onnx").toString();
        String tokens = root.resolve("tokens.txt").toString();
        String waveFilename = root.resolve("test_wavs/0.wav").toString();

        OnlineTransducerModelConfig transducer =
                OnlineTransducerModelConfig.builder()
                        .setEncoder(encoder)
                        .setDecoder(decoder)
                        .setJoiner(joiner)
                        .build();

        OnlineModelConfig modelConfig =
                OnlineModelConfig.builder()
                        .setTransducer(transducer)
                        .setTokens(tokens)
                        .setNumThreads(1)
                        .setDebug(false)
                        .build();

        OnlineRecognizerConfig config =
                OnlineRecognizerConfig.builder()
                        .setOnlineModelConfig(modelConfig)
                        .setDecodingMethod("greedy_search")
                        .build();

        long wholeStart = System.nanoTime();
        long initStart = System.nanoTime();
        OnlineRecognizer recognizer = new OnlineRecognizer(config);
        OnlineStream stream = recognizer.createStream();
        long initMs = millisSince(initStart);

        WaveReader reader = new WaveReader(waveFilename);
        float[] samples = reader.getSamples();
        int sampleRate = reader.getSampleRate();
        double audioSeconds = samples.length / (double) sampleRate;
        int chunkSamples = Math.max(1, sampleRate / 10);

        long inferenceStart = System.nanoTime();
        long firstPartialMs = -1L;
        int partialCount = 0;
        int endpointCount = 0;
        String lastPartial = "";
        List<String> finalSegments = new ArrayList<>();

        for (int start = 0; start < samples.length; start += chunkSamples) {
            int end = Math.min(samples.length, start + chunkSamples);
            float[] chunk = Arrays.copyOfRange(samples, start, end);
            stream.acceptWaveform(chunk, sampleRate);

            while (recognizer.isReady(stream)) {
                recognizer.decode(stream);
            }

            String text = recognizer.getResult(stream).getText().trim();
            if (!text.isEmpty() && !text.equals(lastPartial)) {
                if (firstPartialMs < 0) {
                    firstPartialMs = millisSince(inferenceStart);
                }
                partialCount += 1;
                lastPartial = text;
                System.out.printf(
                        "PARTIAL ms=%d text=%s%n",
                        millisSince(inferenceStart),
                        text);
            }

            if (recognizer.isEndpoint(stream)) {
                if (!text.isEmpty()) {
                    finalSegments.add(text);
                    System.out.printf(
                            "ENDPOINT ms=%d text=%s%n",
                            millisSince(inferenceStart),
                            text);
                }
                endpointCount += 1;
                lastPartial = "";
                recognizer.reset(stream);
            }
        }

        float[] tail = new float[(int) Math.max(1, sampleRate * 0.8)];
        stream.acceptWaveform(tail, sampleRate);
        stream.inputFinished();
        while (recognizer.isReady(stream)) {
            recognizer.decode(stream);
        }

        String tailText = recognizer.getResult(stream).getText().trim();
        if (!tailText.isEmpty()) {
            if (finalSegments.isEmpty()
                    || !tailText.equals(finalSegments.get(finalSegments.size() - 1))) {
                finalSegments.add(tailText);
            }
        }

        long inferenceMs = millisSince(inferenceStart);
        long wholeMs = millisSince(wholeStart);
        String transcript =
                String.join(" ", finalSegments)
                        .replaceAll("\\s+", " ")
                        .trim();
        double realTimeFactor =
                audioSeconds > 0
                        ? (inferenceMs / 1000.0) / audioSeconds
                        : -1.0;

        stream.release();
        recognizer.release();

        try (PrintWriter out =
                new PrintWriter(
                        resultPath.toFile(),
                        StandardCharsets.UTF_8)) {
            out.println("schema=vexlife-local-asr-sherpa-probe/v1");
            out.println("providerRef=provider.local-asr.sherpa-onnx");
            out.println("providerVersion=2.0.1");
            out.println(
                    "modelRef=sherpa-onnx-streaming-zipformer-en-20M-2023-02-17");
            out.println("sampleRate=" + sampleRate);
            out.println("sampleCount=" + samples.length);
            out.printf("audioSeconds=%.3f%n", audioSeconds);
            out.println("initMs=" + initMs);
            out.println("firstPartialMs=" + firstPartialMs);
            out.println("inferenceMs=" + inferenceMs);
            out.println("wholeMs=" + wholeMs);
            out.printf("realTimeFactor=%.4f%n", realTimeFactor);
            out.println("partialCount=" + partialCount);
            out.println("endpointCount=" + endpointCount);
            out.println("finalTranscript=" + transcript);
        }

        System.out.printf(
                "RESULT initMs=%d firstPartialMs=%d inferenceMs=%d rtf=%.4f transcript=%s%n",
                initMs,
                firstPartialMs,
                inferenceMs,
                realTimeFactor,
                transcript);
    }
}
JAVA

printf 'Compiling JVM probe...\n'
"$JAVA_HOME/bin/javac" \
  -cp "$JVM_JAR:$NATIVE_JAR" \
  -d "$CLASSES" \
  "$SRC/SherpaStreamingProbe.java" \
  > "$EVIDENCE/javac.stdout.txt" \
  2> "$EVIDENCE/javac.stderr.txt" \
  || fail 'JVM probe compilation failed' 31

{
  printf 'jvmJar=%s\n' "$JVM_SHA"
  printf 'nativeJar=%s\n' "$NATIVE_SHA"
  printf 'modelArchive=%s\n' "$MODEL_SHA"
  for file in \
    "$MODEL_ROOT/encoder-epoch-99-avg-1.onnx" \
    "$MODEL_ROOT/decoder-epoch-99-avg-1.onnx" \
    "$MODEL_ROOT/joiner-epoch-99-avg-1.onnx" \
    "$MODEL_ROOT/tokens.txt" \
    "$MODEL_ROOT/test_wavs/0.wav"
  do
    /usr/bin/shasum -a 256 "$file"
  done
} > "$EVIDENCE/provider-artifacts.txt"

printf 'Running local streaming inference...\n'
set +e
/usr/bin/time -l \
  "$JAVA_HOME/bin/java" \
  -cp "$JVM_JAR:$NATIVE_JAR:$CLASSES" \
  SherpaStreamingProbe \
  "$MODEL_ROOT" \
  "$EVIDENCE/provider-result.txt" \
  > "$EVIDENCE/java.stdout.txt" \
  2> "$EVIDENCE/java.stderr-and-time.txt"
JAVA_STATUS=$?
set -e

[ "$JAVA_STATUS" -eq 0 ] || fail "sherpa JVM probe exited $JAVA_STATUS" 32
[ -s "$EVIDENCE/provider-result.txt" ] || fail 'provider result was not formed' 33
/usr/bin/grep -q '^finalTranscript=.' "$EVIDENCE/provider-result.txt" \
  || fail 'provider formed no final transcript' 34

{
  printf 'providerSourceCommit=aa273e8029af4e0aabac7d7455a177c5b493aa34\n'
  printf 'providerRelease=v2.0.1\n'
  printf 'providerLicense=Apache-2.0\n'
  printf 'modelLineage=desh2608/icefall-asr-librispeech-pruned-transducer-stateless7-streaming-small\n'
  printf 'modelLineageLicense=Apache-2.0\n'
  printf 'productHomeMutation=false\n'
  printf 'hereticMutation=false\n'
  printf 'productSourceMutation=false\n'
  printf 'cloudTranscription=false\n'
} > "$EVIDENCE/manifest.txt"

STATUS='PASS'
DETAIL='released sherpa JVM/native runtime produced a local streaming transcript from the pinned deterministic model fixture'
package_return || exit 40
