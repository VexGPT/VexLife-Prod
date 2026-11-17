package vexlife.capability

import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vexlife.companion.DirectPromptContext
import vexlife.continuity.ContinuityTurnFixture
import vexlife.continuity.ContinuityTurnFixture.Line

/**
 * Revisit through the real request envelope: Vex writes one request, the bridge admits it, each
 * owner is read once, and what came back is traced by exact source ref without any Home write.
 */
class VexLifeCapabilityRevisitTest {
    private val now = Instant.parse("2026-11-16T20:00:00Z")

    private fun perform(
        home: Path,
        arguments: String,
        available: Set<String> = setOf(VexLifeReachDefinitions.RevisitRef),
        resident: Set<String> = emptySet(),
        owners: VexLifeCapabilityBridge.Owners =
            VexLifeCapabilityBridge.Owners(
                readMemory = { error("Memory has no committed source in this Home") },
                now = { now },
            ),
    ): VexLifeCapabilityBridge.Exchange =
        VexLifeCapabilityBridge.perform(
            parsed =
                requireNotNull(
                    VexLifeCapabilityRequest.parseOrNull(
                        VexLifeCapabilityRequest.RequestHeader +
                            "\n" +
                            """{"capability":"revisit","action":"gather","arguments":$arguments}"""
                    )
                ),
            admission =
                VexLifeCapabilityBridge.Admission(
                    productHome = home,
                    availableCapabilityRefs = available,
                    requestNumber = 1,
                    tokensAvailable = 20_000,
                    promptResidentEventRefs = resident,
                ),
            owners = owners,
            estimateTokens = DirectPromptContext::estimatedMessageTokens,
        )

    @Test
    fun oneRequestReadsEachOwnerOnceAndTracesTheExactSourceItReturned() {
        ContinuityTurnFixture().use { fixture ->
            val turns =
                fixture.commit(
                    Line(
                        "We decided Conversation stays separate from Memory.",
                        "Yes: one owner each.",
                        now.minus(Duration.ofHours(30)),
                    ),
                    Line(
                        "Why separate?",
                        "So exact words never become accepted meaning by accident.",
                        now.minus(Duration.ofHours(29)),
                    ),
                    Line(
                        "Different topic: the lamp.",
                        "The lamp is fixed.",
                        now.minus(Duration.ofMinutes(3)),
                    ),
                )
            val before = fixture.homeTree()
            val exchange =
                perform(
                    fixture.home,
                    """{"queries":[{"ref":"why","terms":["separate"]},{"ref":"owner","terms":["one owner"]}]}""",
                    resident = setOf(turns.last().first.eventRef, turns.last().second.eventRef),
                )
            Files.writeString(
                Path.of("build/revisit-sample-packet.txt"),
                exchange.resultMessage.content,
            )

            assertEquals(fixture.homeTree(), before, "revisit writes nothing to the Home")
            assertEquals(VexLifeCapabilityBridge.State.RETURNED, exchange.entry.state)
            assertEquals("revisit", exchange.use.handle)
            assertEquals("revisit: why, owner", exchange.entry.target)
            assertEquals(
                turns.take(2).flatMap { listOf(it.first.eventRef, it.second.eventRef) }.toSet(),
                exchange.entry.sourceRefs.toSet(),
            )
            assertFalse(exchange.entry.externalEffectsExecuted)
            assertFalse(exchange.entry.effectAuthorityGranted)
            val content = exchange.resultMessage.content
            assertTrue(content.startsWith(VexLifeCapabilityRequest.ResultHeader))
            assertTrue(content.contains("It is not a message from the person."))
            assertTrue(
                content.contains("So exact words never become accepted meaning by accident.")
            )
            assertTrue(content.contains("29 hours ago"))
            assertTrue(
                content.contains(
                    "Not reachable now: Memory — could not be read now: Memory has no committed source in this Home"
                )
            )
            assertTrue(
                content.contains("Where the work stands").not(),
                "no checkpoint exists, so none is invented",
            )
            assertTrue(content.contains("working continuity — no working checkpoint exists yet"))
            VexLifeCapabilityBridge.trace(listOf(exchange.entry))
        }
    }

    @Test
    fun revisitIsOnlyPerformedWhenThisTurnsFrameShowsItAvailable() {
        ContinuityTurnFixture().use { fixture ->
            fixture.commit(Line("Hello.", "Hello.", now.minus(Duration.ofMinutes(1))))
            val exchange =
                perform(
                    fixture.home,
                    """{"queries":[{"terms":["hello"]}]}""",
                    available = emptySet(),
                )

            assertEquals(VexLifeCapabilityBridge.State.NOT_AVAILABLE, exchange.entry.state)
            assertTrue(exchange.entry.sourceRefs.isEmpty())
        }
    }

    @Test
    fun aMalformedRevisitCostsOneRequestAndReturnsTheShapeToUse() {
        ContinuityTurnFixture().use { fixture ->
            val exchange = perform(fixture.home, """{"queries":[{"window":{}}]}""")

            assertEquals(VexLifeCapabilityBridge.State.INVALID_REQUEST, exchange.entry.state)
            assertTrue(
                exchange.resultMessage.content.contains("window needs lastHours, lastTurns or both")
            )
            assertTrue(
                exchange.resultMessage.content.contains(
                    VexLifeReachDefinitions.RevisitExampleArguments
                )
            )
        }
    }

    @Test
    fun theAtlasDescribesRevisitAndTheReachFrameOffersIt() {
        val definition = requireNotNull(VexLifeCapabilityAtlas.resolve("revisit"))
        assertEquals(VexLifeReachDefinitions.RevisitRef, definition.capabilityRef)
        assertEquals("READ_ONLY_LOCAL", definition.effectClass)
        assertEquals(listOf("gather"), definition.actions.map { it.action })
        assertTrue(
            VexLifeReachDefinitions.RevisitRef in VexLifeReachDefinitions.EnvelopeCapabilityRefs
        )
        val described = VexLifeCapabilityBridge.describeText(definition, null)
        assertTrue(described.contains("queries (object[], required)"))
        assertTrue(described.contains(VexLifeReachDefinitions.RevisitExampleArguments))
    }
}

// [VXG RealForever]
