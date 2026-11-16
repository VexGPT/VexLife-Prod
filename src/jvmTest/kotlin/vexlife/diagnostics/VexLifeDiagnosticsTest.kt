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
            diagnostics.beginProviderStreamTrace("send.expiring")
            diagnostics.observeProviderStreamTrace(
                "send.expiring",
                reasoningDelta = "temporary thought",
            )
            assertEquals(1, diagnostics.providerStreamTraces().size)
            now = now.plusSeconds(3 * 3600)
            assertEquals(VexLifeDiagnosticsFamily.Recording, diagnostics.state(), "it ends by itself")
            assertTrue(
                diagnostics.providerStreamTraces().isEmpty(),
                "expired enhanced capture erases stream traces",
            )

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
    fun enhancedCaptureKeepsBoundedProviderStreamsWithTerminalMachineState() {
        withHome { home ->
            val diagnostics = VexLifeDiagnostics(home, sha)

            diagnostics.beginProviderStreamTrace("send.not-enabled")
            assertTrue(
                diagnostics.providerStreamTraces().isEmpty(),
                "normal diagnostics must not retain model stream text",
            )

            diagnostics.control(VexLifeDiagnosticsFamily.ControlStartEnhanced)
            repeat(35) { index ->
                val key = "send.$index"
                diagnostics.beginProviderStreamTrace(key)
                diagnostics.observeProviderStreamTrace(
                    idempotencyKey = key,
                    reasoningDelta = "reasoning-$index",
                    contentDelta = "content-$index",
                )
                diagnostics.finishProviderStreamTrace(
                    idempotencyKey = key,
                    turnRef = "turn.$index",
                    outcome = "TURN_FAILED",
                    failureCode = "CONNECTION_EndpointResponseInvalid",
                )
            }

            val traces = diagnostics.providerStreamTraces()
            assertEquals(30, traces.size)
            assertEquals("send.5", traces.first().idempotencyKey)
            assertEquals("send.34", traces.last().idempotencyKey)
            assertEquals("reasoning-34", traces.last().reasoning)
            assertEquals("content-34", traces.last().content)
            assertTrue(traces.last().serviceHeapMaxMb != null)
            assertTrue(traces.last().hostTotalMemoryMb != null)

            val names = mutableListOf<String>()
            val contents = StringBuilder()
            ZipInputStream(ByteArrayInputStream(diagnostics.export())).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    names += entry.name
                    contents.append(String(zip.readAllBytes()))
                }
            }
            assertTrue(names.contains("enhanced/provider-stream-traces.json"))
            assertTrue(contents.contains("\"modelStreamContentIncluded\":true"))
            assertTrue(contents.contains("reasoning-34"))
            assertTrue(contents.contains("content-34"))
            assertFalse(
                contents.contains("reasoning-0"),
                "oldest traces must be evicted from the bounded ring",
            )

            diagnostics.control(VexLifeDiagnosticsFamily.ControlStopEnhanced)
            assertTrue(
                diagnostics.providerStreamTraces().isEmpty(),
                "stopping enhanced capture erases raw stream traces",
            )
        }
    }

    @Test
    fun exportPreservesDistinctRuntimeLogPathsInsteadOfOverwritingSameBasename() {
        withHome { home ->
            val first = home.resolve("runtime/heretic/mlx-lm-server.err.log")
            val second = home.resolve("runtime/vexlife-prod/mlx-lm-server.err.log")
            Files.createDirectories(first.parent)
            Files.createDirectories(second.parent)
            Files.writeString(first, "old-runtime-log")
            Files.writeString(second, "current-runtime-log")

            val diagnostics = VexLifeDiagnostics(home, sha)
            val names = mutableListOf<String>()
            val contents = StringBuilder()
            ZipInputStream(ByteArrayInputStream(diagnostics.export())).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    names += entry.name
                    contents.append(String(zip.readAllBytes()))
                }
            }

            assertTrue(names.contains("logs/runtime/heretic/mlx-lm-server.err.log"), names.toString())
            assertTrue(
                names.contains("logs/runtime/vexlife-prod/mlx-lm-server.err.log"),
                names.toString(),
            )
            assertTrue(names.contains("current/runtime-log-index.json"))
            assertTrue(contents.contains("old-runtime-log"))
            assertTrue(contents.contains("current-runtime-log"))
            assertTrue(contents.contains("\"runtimeLogPathsPreserved\":true"))
        }
    }

    @Test
    fun turnFailureSnapshotIsRecordedAtFailureTimeWithoutHumanContent() {
        withHome { home ->
            val diagnostics = VexLifeDiagnostics(home, sha)
            diagnostics.recordTurnFailureSnapshot(
                turnRef = "turn.failure.001",
                failureCode = "CONNECTION_EndpointResponseInvalid",
            )

            val chronology = VexChronologyFile.tail(home, 20).joinToString("\n")
            assertTrue(chronology.contains("\"fact\":\"PROCESS_SNAPSHOT_OBSERVED\""))
            assertTrue(chronology.contains("\"reason\":\"TURN_FAILED\""))
            assertTrue(chronology.contains("\"turnRef\":\"turn.failure.001\""))
            assertTrue(
                chronology.contains(
                    "\"failureCode\":\"CONNECTION_EndpointResponseInvalid\""
                )
            )
            assertTrue(chronology.contains("\"serviceHeapMaxMb\":"))
            assertFalse(chronology.contains("human prompt"))
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
