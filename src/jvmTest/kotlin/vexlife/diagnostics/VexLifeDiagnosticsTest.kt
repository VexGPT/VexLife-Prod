package vexlife.diagnostics

import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.zip.ZipInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.logging.VexChronologyFile
import vexlife.logging.VexLogger
import vexlife.model.heretic.HereticProductIdentity

class VexLifeDiagnosticsTest {
    private val sha = "a".repeat(40)

    private fun withHome(block: (Path) -> Unit) {
        val parent = Files.createTempDirectory("vexlife-diagnostics-").toAbsolutePath()
        try {
            val home = parent.resolve(".vexlife")
            CanonicalVexLifeHomeFixture.form(home)
            VexLogger.withSinksForTest(listOf(VexChronologyFile.sinkFor(home))) { block(home) }
        } finally {
            parent.toFile().deleteRecursively()
        }
    }

    @Test
    fun theFamilyAdmitsOnlyTheControlsThatFitTheCurrentState() {
        val family = VexLifeDiagnosticsFamily
        val enhanced = VexLifeDiagnosticsFamily.RecordingEnhanced("2026-11-14T05:00:00Z")
        assertEquals(
            VexLifeDiagnosticsFamily.Effect.BeginEnhancedCapture,
            family.resolve(VexLifeDiagnosticsFamily.Recording, VexLifeDiagnosticsFamily.StartEnhanced)?.effect,
        )
        assertNull(family.resolve(enhanced, VexLifeDiagnosticsFamily.StartEnhanced))
        assertNull(family.resolve(VexLifeDiagnosticsFamily.Recording, VexLifeDiagnosticsFamily.StopEnhanced))
        assertEquals(
            VexLifeDiagnosticsFamily.Effect.ClearDebugHistory,
            family.resolve(enhanced, VexLifeDiagnosticsFamily.ClearHistory)?.effect,
        )
        // Language-free: every word is a key.
        val display = family.displayStateFor(enhanced)
        assertEquals("workspace.status.diagnostics-enhanced-on", display.capture.key)
        assertEquals(listOf("STOP_ENHANCED", "CLEAR_HISTORY"), display.controls.map { it.controlRef })
    }

    @Test
    fun enhancedCaptureIsShortLivedAndClearingRemovesOnlyDebugHistory() {
        withHome { home ->
            var now = Instant.parse("2026-11-14T03:00:00Z")
            val diagnostics = VexLifeDiagnostics(home, sha, clock = { now })
            assertEquals(VexLifeDiagnosticsFamily.StateRecording, diagnostics.projection().state)

            diagnostics.control(VexLifeDiagnosticsFamily.ControlStartEnhanced)
            assertIs<VexLifeDiagnosticsFamily.RecordingEnhanced>(diagnostics.state())
            now = now.plusSeconds(3 * 3600)
            assertEquals(VexLifeDiagnosticsFamily.Recording, diagnostics.state(), "it ends by itself")

            val failure = home.resolve("recovery/direct-companion/turn.json")
            Files.createDirectories(failure.parent)
            Files.writeString(failure, """{"failureCode":"TURN_FAILED"}""")
            val projection = diagnostics.control(VexLifeDiagnosticsFamily.ControlClearHistory)
            assertEquals(1, projection.chronologyRecords, "the new history starts with the clearing itself")
            assertTrue(Files.exists(failure), "recovery evidence is not debug history")
        }
    }

    @Test
    fun theExportIsBoundedAndCarriesNoConversation() {
        withHome { home ->
            val diagnostics = VexLifeDiagnostics(home, sha)
            val conversationSecret = "a sentence only the person wrote"
            Files.createDirectories(home.resolve("conversations"))
            Files.writeString(home.resolve("conversations/thread.jsonl"), conversationSecret)
            VexLogger.record(VexLogger.Fact.TurnFailed, mapOf("turnRef" to "turn.x", "failureCode" to "CONNECTION_TIMEOUT"))

            val names = mutableListOf<String>()
            val contents = StringBuilder()
            ZipInputStream(ByteArrayInputStream(diagnostics.export())).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    names += entry.name
                    contents.append(String(zip.readAllBytes()))
                }
            }
            assertTrue(
                names.containsAll(
                    listOf(
                        "chronology.jsonl",
                        "RESULT-SUMMARY.txt",
                        "manifest.json",
                        "environment/environment.json",
                        "current/runtime-process.json",
                    )
                ),
                names.toString(),
            )
            assertTrue(contents.contains("TURN_FAILED"))
            assertTrue(contents.contains("\"humanContentIncluded\":false"))
            assertFalse(contents.contains(conversationSecret), "Conversation never enters the export")
        }
    }

    @Test
    fun theRuntimeObserverRecordsOnlyLivenessTransitions() {
        withHome { home ->
            val receipt = home.resolve(HereticProductIdentity.ProductRuntimeReceiptRelativePath)
            Files.createDirectories(receipt.parent)
            Files.writeString(receipt, """{"runtime":{"pid":${ProcessHandle.current().pid()}}}""")
            val diagnostics = VexLifeDiagnostics(home, sha)
            diagnostics.observeOnce()
            diagnostics.observeOnce()
            Files.writeString(receipt, """{"runtime":{"pid":999999999}}""")
            diagnostics.observeOnce()
            val facts = VexChronologyFile.tail(home, 50).map { it.substringAfter("\"fact\":\"").substringBefore('"') }
            assertEquals(listOf("RUNTIME_PROCESS_LIVE", "RUNTIME_PROCESS_NO_LONGER_LIVE"), facts)
        }
    }
}

// [VXG RealForever]
