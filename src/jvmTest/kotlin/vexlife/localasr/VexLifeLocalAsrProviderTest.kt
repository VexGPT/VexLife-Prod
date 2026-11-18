package vexlife.localasr

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VexLifeLocalAsrProviderTest {
    @Test
    fun persistentWhisperServerBindsExactModelToLoopback() {
        val command =
            whisperCppServerCommand(
                executablePath =
                    Path.of(
                        "/vex/home/bin/whisper-server",
                    ),
                modelPath =
                    Path.of(
                        "/vex/home/models/ggml-base.bin",
                    ),
                port =
                    18427,
            )

        assertEquals(
            "/vex/home/bin/whisper-server",
            command.first(),
        )
        assertTrue(
            command.windowed(
                2,
            )
                .any {
                    it ==
                        listOf(
                            "-m",
                            "/vex/home/models/ggml-base.bin",
                        )
                },
        )
        assertTrue(
            command.windowed(
                2,
            )
                .any {
                    it ==
                        listOf(
                            "--host",
                            "127.0.0.1",
                        )
                },
        )
        assertTrue(
            command.windowed(
                2,
            )
                .any {
                    it ==
                        listOf(
                            "--port",
                            "18427",
                        )
                },
        )
        assertTrue(
            command.contains(
                "-nt",
            ),
        )
    }

    @Test
    fun serverInferenceMultipartCarriesAudioLanguageAndJsonContract() {
        val audio =
            "RIFF-test-WAVE"
                .toByteArray(
                    StandardCharsets.US_ASCII,
                )
        val request =
            whisperCppServerMultipart(
                wavBytes =
                    audio,
                languageCode =
                    "zh",
                boundary =
                    "vexlife-test-boundary",
            )

        assertEquals(
            "multipart/form-data; boundary=vexlife-test-boundary",
            request.contentType,
        )
        val body =
            request.body.toString(
                StandardCharsets.ISO_8859_1,
            )
        assertTrue(
            body.contains(
                "name=\"file\"; filename=\"audio.wav\"",
            ),
        )
        assertTrue(
            body.contains(
                "Content-Type: audio/wav",
            ),
        )
        assertTrue(
            body.contains(
                "RIFF-test-WAVE",
            ),
        )
        assertTrue(
            body.contains(
                "name=\"language\"\r\n\r\nzh",
            ),
        )
        assertTrue(
            body.contains(
                "name=\"response_format\"\r\n\r\njson",
            ),
        )
        assertTrue(
            body.contains(
                "name=\"no_timestamps\"\r\n\r\ntrue",
            ),
        )
    }

    @Test
    fun serverInferenceMultipartCanRequestAutoDetection() {
        val request =
            whisperCppServerMultipart(
                wavBytes =
                    byteArrayOf(
                        1,
                    ),
                languageCode =
                    "auto",
                boundary =
                    "vexlife-test-auto",
            )

        val body =
            request.body.toString(
                StandardCharsets.ISO_8859_1,
            )
        assertTrue(
            body.contains(
                "name=\"language\"\r\n\r\nauto",
            ),
        )
    }
}

// [VXG RealForever]
