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
