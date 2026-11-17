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
