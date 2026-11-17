package vexlife.capability

import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import vexlife.companion.DirectPromptContext
import vexlife.continuity.ContinuityTurnFixture
import vexlife.continuity.ContinuityTurnFixture.Line
import vexlife.continuity.VexLifeContextRevisit
import vexlife.conversation.DirectConversationStore

/**
 * Continuity revisit through the real request envelope: Vex writes one request, the bridge admits
 * it against the sources the turn bound, each owner is read at most once for the whole turn, and
 * what came back is traced by exact source and frontier ref without any Home write.
 */
class VexLifeCapabilityRevisitTest {
    private val now = Instant.parse("2026-11-16T20:00:00Z")

    private class Counting(private val home: Path) {
        var conversationReads = 0

        fun sources(resident: Set<String> = emptySet()) =
            VexLifeContextRevisit.TurnSources(
                readConversation = {
                    conversationReads += 1
                    DirectConversationStore(home).current()
                },
                memory = { null },
                readWorking = { null },
                promptResidentEventRefs = { resident },
                promptResidentStatementRefs = { emptySet() },
                now = { Instant.parse("2026-11-16T20:00:00Z") },
            )
    }

    private fun perform(
        home: Path,
        arguments: String,
        sources: VexLifeContextRevisit.TurnSources?,
        available: Set<String> = setOf(VexLifeReachDefinitions.RevisitRef),
        requestNumber: Int = 1,
    ): VexLifeCapabilityBridge.Exchange =
        VexLifeCapabilityBridge.perform(
            parsed =
                requireNotNull(
                    VexLifeCapabilityRequest.parseOrNull(
                        VexLifeCapabilityRequest.RequestHeader +
                            "\n" +
                            """{"capability":"continuity","action":"revisit","arguments":$arguments}"""
                    )
                ),
            admission =
                VexLifeCapabilityBridge.Admission(
                    productHome = home,
                    availableCapabilityRefs = available,
                    requestNumber = requestNumber,
                    tokensAvailable = 20_000,
                    turnSources = sources,
                ),
            owners = VexLifeCapabilityBridge.Owners(now = { now }),
            estimateTokens = DirectPromptContext::estimatedMessageTokens,
        )

    private fun ContinuityTurnFixture.threeTurns() =
        commit(
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

    @Test
    fun oneRequestUsesTheTurnsSourcesAndTracesTheExactSourceItReturned() {
        ContinuityTurnFixture().use { fixture ->
            val turns = fixture.threeTurns()
            val before = fixture.homeTree()
            val counting = Counting(fixture.home)
            val exchange =
                perform(
                    fixture.home,
                    """{"queries":[{"ref":"why","terms":["separate"]},{"ref":"owner","terms":["one owner"]}]}""",
                    counting.sources(
                        resident = setOf(turns.last().first.eventRef, turns.last().second.eventRef)
                    ),
                )
            Files.writeString(
                Path.of("build/revisit-sample-packet.txt"),
                exchange.resultMessage.content,
            )

            assertEquals(fixture.homeTree(), before, "revisit writes nothing to the Home")
            assertEquals(VexLifeCapabilityBridge.State.RETURNED, exchange.entry.state)
            assertEquals("continuity", exchange.use.handle)
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
                    "Not reachable now: Memory — no Memory projection is bound to this turn"
                )
            )
            assertTrue(
                content.contains("Where the work stands").not(),
                "no position bound, none invented",
            )
            assertTrue(
                content.contains("working continuity — no working position is bound to this turn")
            )
            VexLifeCapabilityBridge.trace(listOf(exchange.entry))
        }
    }

    @Test
    fun aSecondRequestInTheSameTurnReadsNoOwnerAgain() {
        ContinuityTurnFixture().use { fixture ->
            fixture.threeTurns()
            val counting = Counting(fixture.home)
            val sources = counting.sources()
            val first =
                perform(
                    fixture.home,
                    """{"queries":[["separate"],["one owner"],["lamp"]]}""",
                    sources,
                )
            val second =
                perform(
                    fixture.home,
                    """{"queries":[["accepted","meaning"]]}""",
                    sources,
                    requestNumber = 2,
                )

            assertEquals(
                1,
                counting.conversationReads,
                "Conversation is read once for the whole turn",
            )
            val firstScan = assertNotNull(first.entry.hostScan)
            val secondScan = assertNotNull(second.entry.hostScan)
            assertEquals(
                2,
                firstScan.ownerReadCount,
                "Conversation and working position, once each",
            )
            assertEquals(0, secondScan.ownerReadCount)
            assertEquals(2, secondScan.ownerReadsThisTurn)
            assertEquals(6, firstScan.conversationEventsRead)
            assertEquals(VexLifeCapabilityBridge.State.RETURNED, second.entry.state)
        }
    }

    @Test
    fun cluesInOneQueryAreConjunctiveAndQueriesAreAlternateVantages() {
        ContinuityTurnFixture().use { fixture ->
            val turns = fixture.threeTurns()
            // Both clues must appear in one turn: "lamp" + "fixed" locates; "lamp" + "broken" does
            // not.
            val joint =
                perform(
                    fixture.home,
                    """{"queries":[["lamp","fixed"]]}""",
                    Counting(fixture.home).sources(),
                )
            assertEquals(
                setOf(turns[2].first.eventRef, turns[2].second.eventRef),
                joint.entry.sourceRefs.toSet(),
            )
            val absent =
                perform(
                    fixture.home,
                    """{"queries":[["lamp","broken"]]}""",
                    Counting(fixture.home).sources(),
                )
            assertEquals(VexLifeCapabilityBridge.State.NOT_FOUND, absent.entry.state)
            // Separate queries are alternate vantages: either one reaches its own turn.
            val vantages =
                perform(
                    fixture.home,
                    """{"queries":[["lamp","broken"],["one owner"]]}""",
                    Counting(fixture.home).sources(),
                )
            assertEquals(
                setOf(turns[0].first.eventRef, turns[0].second.eventRef),
                vantages.entry.sourceRefs.toSet(),
            )
        }
    }

    @Test
    fun revisitNeedsTheFrameAndATurnBinding() {
        ContinuityTurnFixture().use { fixture ->
            fixture.commit(Line("Hello.", "Hello.", now.minus(Duration.ofMinutes(1))))
            val notInFrame =
                perform(
                    fixture.home,
                    """{"queries":[{"terms":["hello"]}]}""",
                    Counting(fixture.home).sources(),
                    available = emptySet(),
                )
            assertEquals(VexLifeCapabilityBridge.State.NOT_AVAILABLE, notInFrame.entry.state)
            assertTrue(notInFrame.entry.sourceRefs.isEmpty())

            val unbound =
                perform(fixture.home, """{"queries":[{"terms":["hello"]}]}""", sources = null)
            assertEquals(VexLifeCapabilityBridge.State.NOT_AVAILABLE, unbound.entry.state)
            assertTrue(unbound.resultMessage.content.contains("not bound to this provider turn"))
        }
    }

    @Test
    fun aMalformedRevisitCostsOneRequestAndReturnsTheShapeToUse() {
        ContinuityTurnFixture().use { fixture ->
            val exchange =
                perform(
                    fixture.home,
                    """{"queries":[{"window":{}}]}""",
                    Counting(fixture.home).sources(),
                )

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
    fun theAtlasDescribesOneContinuityCapabilityAndTheReachFrameOffersIt() {
        val definition = requireNotNull(VexLifeCapabilityAtlas.resolve("continuity"))
        assertEquals(VexLifeReachDefinitions.RevisitRef, definition.capabilityRef)
        assertEquals("READ_ONLY_LOCAL", definition.effectClass)
        assertEquals(listOf("revisit"), definition.actions.map { it.action })
        assertTrue(
            VexLifeReachDefinitions.RevisitRef in VexLifeReachDefinitions.EnvelopeCapabilityRefs
        )
        val described = VexLifeCapabilityBridge.describeText(definition, null)
        assertTrue(described.contains("queries (object[], required)"))
        assertTrue(described.contains(VexLifeReachDefinitions.RevisitExampleArguments))
    }
}

// [VXG RealForever]
