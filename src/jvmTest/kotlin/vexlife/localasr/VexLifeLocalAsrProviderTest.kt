package vexlife.localasr

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VexLifeLocalAsrProviderTest {
    @Test
    fun stdinWhisperInvocationRequestsTextOnStdout() {
        val command =
            whisperCppCliCommand(
                executablePath = Path.of("/vex/home/bin/whisper-cli"),
                modelPath = Path.of("/vex/home/models/ggml-base.en.bin"),
                languageCode = "en",
            )

        assertEquals("/vex/home/bin/whisper-cli", command.first())
        assertTrue(command.windowed(2).any { it == listOf("-f", "-") })
        assertTrue(command.contains("-np"))
        assertTrue(command.contains("-nt"))
        assertTrue(
            command.contains("-otxt"),
            "stdin whisper-cli must explicitly emit final transcript text to stdout",
        )
        assertTrue(command.windowed(2).any { it == listOf("-l", "en") })
    }

    @Test
    fun multilingualWhisperInvocationCarriesSelectedLanguage() {
        val command =
            whisperCppCliCommand(
                executablePath = Path.of("/vex/home/bin/whisper-cli"),
                modelPath = Path.of("/vex/home/models/ggml-base.bin"),
                languageCode = "ja",
            )

        assertTrue(command.windowed(2).any { it == listOf("-m", "/vex/home/models/ggml-base.bin") })
        assertTrue(command.windowed(2).any { it == listOf("-l", "ja") })
        assertTrue(command.windowed(2).any { it == listOf("-f", "-") })
        assertTrue(command.contains("-otxt"))
    }

    @Test
    fun multilingualWhisperInvocationCanRequestAutoDetection() {
        val command =
            whisperCppCliCommand(
                executablePath = Path.of("/vex/home/bin/whisper-cli"),
                modelPath = Path.of("/vex/home/models/ggml-base.bin"),
                languageCode = "auto",
            )

        assertTrue(command.windowed(2).any { it == listOf("-l", "auto") })
        assertTrue(command.contains("-otxt"))
    }
}

// [VXG RealForever]
