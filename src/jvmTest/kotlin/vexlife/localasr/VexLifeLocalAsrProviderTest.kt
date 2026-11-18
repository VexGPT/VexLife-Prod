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
            )

        assertEquals("/vex/home/bin/whisper-cli", command.first())
        assertTrue(command.windowed(2).any { it == listOf("-f", "-") })
        assertTrue(command.contains("-np"))
        assertTrue(command.contains("-nt"))
        assertTrue(
            command.contains("-otxt"),
            "stdin whisper-cli must explicitly emit final transcript text to stdout",
        )
    }
}

// [VXG RealForever]
