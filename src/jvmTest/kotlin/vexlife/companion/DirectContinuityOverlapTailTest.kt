package vexlife.companion

import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vexlife.companion.DirectContinuityOverlapTail.ExpansionReason
import vexlife.companion.DirectContinuityOverlapTail.FloorState
import vexlife.continuity.ContinuityTurnFixture
import vexlife.continuity.ContinuityTurnFixture.Line
import vexlife.continuity.VexLifeTurnNavigation
import vexlife.continuity.VexLifeWorkingContinuity
import vexlife.continuity.VexLifeWorkingContinuityOwner

/**
 * Rebase continuity: after a rollover the next prompt keeps the person's and Vex's own last words,
 * at least three whole exchanges when they fit, and says so when they do not.
 */
class DirectContinuityOverlapTailTest {
    private val start = Instant.parse("2026-11-16T09:00:00Z")

    private fun at(minutes: Long) = start.plus(Duration.ofMinutes(minutes))

    private fun estimate(text: String) = DirectPromptContext.estimatedMessageTokens(text)

    private fun newestFirst(fixture: ContinuityTurnFixture) =
        VexLifeTurnNavigation.project(fixture.store.current().canonicalEvents).asReversed()

    @Test
    fun theFloorIsKeptAndTheTailGrowsWhileTheNewerTurnStillLeansOnTheOlderOne() {
        ContinuityTurnFixture().use { fixture ->
            fixture.commit(
                Line(
                    "Start: the memory depth plan.",
                    "Here is the plan in five parts. " + "detail ".repeat(400),
                    at(0),
                ),
                Line("Separate question about lunch.", "Soup sounds good.", at(120)),
                Line("Part three needs a budget.", "Budgets bound depth and tokens.", at(240)),
                Line("That budget, is it per owner?", "Per owner and global.", at(300)),
                Line("No, I meant per query.", "Per query as well.", at(301)),
                Line("And then the frontier?", "The frontier names what did not fit.", at(302)),
            )
            val selection =
                DirectContinuityOverlapTail.select(
                    newestFirst = newestFirst(fixture),
                    historyComplete = true,
                    admissibleTokens = 10_000,
                    expansionBudgetTokens = 10_000,
                    estimateTokens = ::estimate,
                )

            assertEquals(FloorState.MET, selection.evidence.floorState)
            // floor: frontier / per-query / per-owner; +1 the budget turn "that budget" points to,
            // +1 the lunch pair, small and from the same sitting; the plan turn is large and
            // nothing
            // leans on it, so the tail stops there.
            assertEquals(5, selection.evidence.selectedPairs)
            assertEquals(
                listOf(ExpansionReason.POINTS_BACK, ExpansionReason.SMALL_PAIR),
                selection.evidence.expansionReasons,
            )
            assertEquals("Separate question about lunch.", selection.events.first().content)
            assertEquals("The frontier names what did not fit.", selection.events.last().content)
        }
    }

    @Test
    fun quietLargeTurnsKeepExactlyTheFloor() {
        ContinuityTurnFixture().use { fixture ->
            val big = "word ".repeat(1_400).trim()
            fixture.commit(
                *(0L..5L)
                    .map { Line("Long thought $it. $big", "Long reply $it. $big", at(it * 90)) }
                    .toTypedArray()
            )
            val selection =
                DirectContinuityOverlapTail.select(
                    newestFirst = newestFirst(fixture),
                    historyComplete = true,
                    admissibleTokens = 40_000,
                    expansionBudgetTokens = 12_000,
                    estimateTokens = ::estimate,
                )

            assertEquals(FloorState.MET, selection.evidence.floorState)
            assertEquals(3, selection.evidence.selectedPairs)
            assertTrue(selection.evidence.expansionReasons.isEmpty())
        }
    }

    @Test
    fun aRoomTooSmallForTheFloorIsReportedAsDegradedInsteadOfClaimed() {
        ContinuityTurnFixture().use { fixture ->
            val big = "word ".repeat(2_000).trim()
            fixture.commit(
                Line("Old. $big", "Old reply. $big", at(0)),
                Line("Middle. $big", "Middle reply. $big", at(1)),
                Line("Newest.", "Newest reply.", at(2)),
            )
            val selection =
                DirectContinuityOverlapTail.select(
                    newestFirst = newestFirst(fixture),
                    historyComplete = true,
                    admissibleTokens = 1_000,
                    expansionBudgetTokens = 500,
                    estimateTokens = ::estimate,
                )

            assertEquals(FloorState.DEGRADED_ROOM, selection.evidence.floorState)
            assertEquals(1, selection.evidence.selectedPairs)
            assertEquals(listOf("Newest.", "Newest reply."), selection.events.map { it.content })
        }
    }

    @Test
    fun aShortHistoryIsNotDegradation() {
        ContinuityTurnFixture().use { fixture ->
            fixture.commit(Line("One.", "Reply one.", at(0)), Line("Two.", "Reply two.", at(1)))
            val selection =
                DirectContinuityOverlapTail.select(
                    newestFirst = newestFirst(fixture),
                    historyComplete = true,
                    admissibleTokens = 10_000,
                    expansionBudgetTokens = 10_000,
                    estimateTokens = ::estimate,
                )

            assertEquals(FloorState.HISTORY_SHORTER_THAN_FLOOR, selection.evidence.floorState)
            assertEquals(2, selection.evidence.selectedPairs)
        }
    }

    /** Commit a rollover checkpoint whose cursor is the newest completed request. */
    private fun rollover(fixture: ContinuityTurnFixture, openLoop: String) {
        val newest = fixture.store.current().canonicalEvents.last { it.eventKind == "REQUEST" }
        val workingSet =
            VexLifeWorkingContinuity.WorkingSet(
                lineageRef = newest.companionLineageRef,
                threadRef = newest.threadRef,
                cursorEventRef = newest.eventRef,
                activeWorkNodeRefs = listOf("work-node.memory-depth.parent"),
                currentIntentReceiptRefs = emptyList(),
                openLoopRefs = listOf(openLoop),
                currentStatementRefs = emptyList(),
                currentContinuityRecordRefs = emptyList(),
                currentDailyStratumRefOrNull = null,
                sourceRefs = listOf("source.continuity.overlap-test"),
            )
        val pressure =
            VexLifeWorkingContinuity.PromptPressureInput(
                promptReceiptRef = "receipt.vexlife.prompt-context.overlap-test",
                providerMaterializedInputTokenEstimate = 176_000,
                inputTokenBudget = 192_000,
                reservedOutputTokens = 16_000,
                hardTokenLimit = 208_000,
                memoryBoundedOutStatementCount = 0,
                memoryTokenBoundedOutPageCount = 0,
                sourceRefs = listOf("source.continuity.overlap-pressure"),
            )
        VexLifeWorkingContinuityOwner(fixture.home)
            .commitPlan(
                plan = VexLifeWorkingContinuity.formPlan(pressure, workingSet),
                pressureInput = pressure,
                workingSet = workingSet,
                committedAt = at(1_000),
            )
    }

    @Test
    fun theFirstTurnAfterARolloverKeepsTheLastWholeExchangesSoThatStillLands() {
        ContinuityTurnFixture().use { fixture ->
            val turns =
                fixture.commit(
                    Line(
                        "Long ago we discussed the garden layout.",
                        "Raised beds on the south side.",
                        at(0),
                    ),
                    Line("Now: the hydration design.", "One request, many owner reads.", at(600)),
                    Line("What about duplicates?", "One node, every route kept.", at(601)),
                    Line("And the frontier?", "Named with the exact way to ask.", at(602)),
                    Line(
                        "Write the overlap rule.",
                        "At least three whole exchanges when they fit.",
                        at(603),
                    ),
                )
            rollover(fixture, openLoop = "loop.memory-depth.overlap-rule")
            val headBefore = fixture.store.current().head

            fixture.store.withWriterLease {
                val prior = requireNotNull(immediatePriorCompletedTurn())
                val current =
                    beginRequest(
                        turnRef = "turn.continuity.006",
                        requestMessageRef = "message.continuity.human.006",
                        content = "Why three and not that one number you said earlier?",
                        formedAt = at(1_001),
                    )
                val successor =
                    requireNotNull(
                        DirectWorkingContinuity.resolveSuccessorContext(
                            productHome = fixture.home,
                            prior = prior,
                            currentRequest = current,
                            issuedAt = at(1_002),
                        )
                    )
                val materialized =
                    DirectPromptContext.materializeContinuity(
                        conversationOwner = this,
                        prior = prior,
                        currentRequest = current,
                        workingContinuityContext = successor,
                    )

                val tail = turns.drop(1)
                assertEquals("WORKING_CHECKPOINT_SUCCESSOR", materialized.receipt.contextMode)
                assertEquals(
                    tail.flatMap { listOf(it.first.eventRef, it.second.eventRef) },
                    materialized.receipt.selectedConversationEventRefs,
                )
                val evidence = requireNotNull(materialized.receipt.overlapTail)
                assertEquals(FloorState.MET, evidence.floorState)
                assertEquals(4, evidence.selectedPairs)
                assertFalse(evidence.responseBridgeOnly)
                assertEquals(
                    prior.responseEvent.eventRef,
                    materialized.receipt.successorPriorResponseEventRef,
                )

                val roles = materialized.messages.map { it.role }
                assertEquals(
                    listOf("system") + List(4) { listOf("user", "assistant") }.flatten() + "user",
                    roles,
                )
                assertTrue(
                    materialized.messages.first().content.contains("loop.memory-depth.overlap-rule")
                )
                assertEquals(
                    tail.flatMap { listOf(it.first.content, it.second.content) },
                    materialized.messages.drop(1).dropLast(1).map { it.content },
                )
                assertEquals(current.content, materialized.messages.last().content)
                assertFalse(
                    materialized.messages.any { it.content.contains("garden layout") },
                    "an exchange outside the tail stays in Conversation, reachable through revisit",
                )

                DirectPromptContext.reverifyContinuity(
                    materialization = materialized,
                    conversationOwner = this,
                    prior = prior,
                    currentRequest = current,
                    workingContinuityContext = successor,
                )
            }
            assertEquals(
                headBefore,
                fixture.store.current().head,
                "materialization writes no Conversation",
            )
        }
    }

    @Test
    fun whenNotEvenTheNewestExchangeFitsOnlyVexsPriorReplyIsKeptAndThatIsSaid() {
        ContinuityTurnFixture().use { fixture ->
            fixture.commit(
                Line("Earlier.", "Earlier reply.", at(0)),
                Line("Paste: " + "token ".repeat(80_000).trim(), "I read the whole paste.", at(1)),
            )
            rollover(fixture, openLoop = "loop.memory-depth.big-paste")

            fixture.store.withWriterLease {
                val prior = requireNotNull(immediatePriorCompletedTurn())
                val current =
                    beginRequest(
                        turnRef = "turn.continuity.003",
                        requestMessageRef = "message.continuity.human.003",
                        content = "What did that paste change?",
                        formedAt = at(2),
                    )
                val successor =
                    requireNotNull(
                        DirectWorkingContinuity.resolveSuccessorContext(
                            productHome = fixture.home,
                            prior = prior,
                            currentRequest = current,
                            issuedAt = at(3),
                        )
                    )
                val materialized =
                    DirectPromptContext.materializeContinuity(
                        conversationOwner = this,
                        prior = prior,
                        currentRequest = current,
                        workingContinuityContext = successor,
                    )

                val evidence = requireNotNull(materialized.receipt.overlapTail)
                assertEquals(FloorState.DEGRADED_ROOM, evidence.floorState)
                assertTrue(evidence.responseBridgeOnly)
                assertEquals(
                    listOf(prior.responseEvent.eventRef),
                    materialized.receipt.selectedConversationEventRefs,
                )
                assertEquals(
                    listOf("system", "assistant", "user"),
                    materialized.messages.map { it.role },
                )
                assertFalse(materialized.messages.any { it.content.startsWith("Paste: ") })
            }
        }
    }
}

// [VXG RealForever]
