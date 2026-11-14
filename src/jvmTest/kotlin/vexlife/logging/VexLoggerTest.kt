package vexlife.logging

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vexlife.contract.VexContractJson
import vexlife.home.CanonicalVexLifeHomeFixture

class VexLoggerTest {
    private fun withHome(block: (Path) -> Unit) {
        val parent = Files.createTempDirectory("vexlife-logger-").toAbsolutePath()
        try {
            val home = parent.resolve(".vexlife")
            CanonicalVexLifeHomeFixture.form(home)
            block(home)
        } finally {
            parent.toFile().deleteRecursively()
        }
    }

    @Test
    fun factsHaveOneOwnerAndStableWireNames() {
        assertEquals("SEND_ACCEPTED", VexLogger.Fact.SendAccepted.wire)
        assertEquals("RUNTIME_RETIRED_UNHEALTHY", VexLogger.Fact.RuntimeRetiredUnhealthy.wire)
        assertEquals(VexLogger.Fact.entries.size, VexLogger.Fact.entries.map { it.wire }.toSet().size)
        VexLogger.Fact.entries.forEach { assertEquals(it, VexLogger.Fact.fromWire(it.wire)) }
    }

    @Test
    fun aFailingSinkNeverChangesTheFactOrThrows() {
        val kept = mutableListOf<VexLogger.Record>()
        VexLogger.withSinksForTest(
            listOf(VexLogger.Sink { error("disk full") }, VexLogger.Sink { kept += it })
        ) {
            VexLogger.record(VexLogger.Fact.TurnFailed, mapOf("turnRef" to "turn.a", "Bad Key!" to "x"))
        }
        assertEquals(1, kept.size, "the healthy sink still receives the fact")
        assertEquals(mapOf("turnRef" to "turn.a"), kept.single().refs, "only stable ref keys are kept")
    }

    @Test
    fun theHomeChronologyAppendsBoundedLinesAndClearingRemovesOnlyIt() {
        withHome { home ->
            val sink = VexChronologyFile.sinkFor(home)
            VexLogger.withSinksForTest(listOf(sink)) {
                repeat(3) { VexLogger.record(VexLogger.Fact.EntryStateObserved, mapOf("stateRef" to "S$it")) }
            }
            val lines = VexChronologyFile.tail(home, 10)
            assertEquals(3, lines.size)
            val first = VexContractJson.decode<VexLogger.Record>(lines.first())
            assertEquals("ENTRY_STATE_OBSERVED", first.fact)
            assertEquals("entry", first.owner)

            val conversation = home.resolve("config/home.json")
            val before = Files.readString(conversation)
            assertEquals(1, VexChronologyFile.clear(home))
            assertTrue(VexChronologyFile.tail(home, 10).isEmpty())
            assertEquals(before, Files.readString(conversation), "clearing touches debug history only")
        }
    }

    @Test
    fun noChronologyIsWrittenWhereThereIsNoVexHome() {
        val parent = Files.createTempDirectory("vexlife-logger-nohome-").toAbsolutePath()
        try {
            val notAHome = parent.resolve("absent")
            VexLogger.withSinksForTest(listOf(VexChronologyFile.sinkFor(notAHome))) {
                VexLogger.record(VexLogger.Fact.EntryStateObserved)
            }
            assertFalse(Files.exists(notAHome), "the Logger never creates a Home")
        } finally {
            parent.toFile().deleteRecursively()
        }
    }
}

// [VXG RealForever]
