package vexlife.harness

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MultiVexFreshBootCapabilityAdapterTest {
    @Test
    fun freshBootChallengeAsksForEvidenceWithoutChoosingVexsCapability() {
        val request =
            Files.readString(
                Path.of(
                    "tools/continuity-pressure/treatments/f1-fresh-boot-capability-challenge-v1.txt",
                ),
            )
                .trim()

        assertContains(request, "What can you actually do right now?")
        assertContains(request, "Show me one low-risk thing")
        assertContains(request, "use one read capability")
        assertContains(request, "Choose the demonstration yourself")
        assertContains(request, "what the returned evidence proves and what it does not")

        val lower = request.lowercase()
        assertFalse("use web" in lower)
        assertFalse("use github" in lower)
        assertFalse("use home" in lower)
        assertFalse("use memory" in lower)
        assertFalse("use conversation" in lower)
        assertFalse("you are better" in lower)
        assertFalse("prove you're better" in lower)
        assertFalse("reply exactly" in lower)
        assertFalse("correct answer" in lower)
    }

    @Test
    fun freshBootAdapterDoesNotRequireCapabilityUseForMechanicalPass() {
        val source =
            Files.readString(
                Path.of(
                    "src/jvmTest/kotlin/vexlife/harness/MultiVexFreshBootCapabilityAdapter.kt",
                ),
            )

        assertContains(
            source,
            "providerToolCount > 0",
        )
        assertContains(
            source,
            "capabilityUses",
        )
        assertContains(
            source,
            "capabilityTracePresent",
        )
        assertContains(
            source,
            "It does not decide whether Vex chose a meaningful demonstration",
        )
        assertFalse(
            "require(result.capabilityUses.isNotEmpty())" in source,
        )
        assertFalse(
            "require(trace != null)" in source,
        )
        assertTrue(
            "freshConversationEventCountBefore" in source &&
                "freshConversationEventCountAfter" in source,
        )
    }
}

// [VXG RealForever]
