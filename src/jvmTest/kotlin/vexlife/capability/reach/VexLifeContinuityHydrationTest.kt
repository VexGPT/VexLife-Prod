package vexlife.capability.reach

import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vexlife.companion.DirectPromptContext
import vexlife.continuity.VexLifeWorkingContinuity
import vexlife.conversation.CanonicalLivedContextFixture
import vexlife.conversation.DirectConversationStore
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.memory.VexLifeMemoryContextFixture

class VexLifeContinuityHydrationTest {
    private fun productHome(): Path {
        val parent =
            Files.createTempDirectory("vexlife-prod-continuity-hydration-").toAbsolutePath()
        val home = parent.resolve("VexLife-Prod")
        CanonicalVexLifeHomeFixture.form(home)
        return home
    }

    private fun DirectConversationStore.Locked.commit(
        home: Path,
        suffix: String,
        human: String,
        vex: String,
    ): Pair<DirectConversationStore.Event, DirectConversationStore.Event> {
        val request =
            beginRequest(
                turnRef = "turn.continuity-hydration.$suffix",
                requestMessageRef = "message.continuity-hydration.human.$suffix",
                content = human,
            )
        val response =
            appendResponse(
                request = request,
                responseMessageRef = "message.continuity-hydration.vex.$suffix",
                content = vex,
                model = "default_model",
                endpointOrigin = "http://127.0.0.1:8765",
                runtimeObservationRef = "observation.continuity-hydration.$suffix",
                invocationEvidenceRef = "evidence.continuity-hydration.$suffix",
            )
        val context =
            CanonicalLivedContextFixture.write(
                productHome = home,
                request = request,
                response = response,
                formedAt = Instant.parse("2026-11-16T08:00:00Z"),
            )
        commitCompletedTurn(
            request = request,
            response = response,
            contextSha256 = context.sha256,
        )
        return request to response
    }

    @Test
    fun oneHydrationFusesQueriesAcrossExactConversationMemoryAndWorkingCoordinates() {
        val home = productHome()
        val store = DirectConversationStore(home)

        try {
            store.withWriterLease {
                val ownership =
                    commit(
                        home = home,
                        suffix = "ownership",
                        human =
                            "Conversation and Memory must remain separate semantic owners even when Vex needs old context.",
                        vex =
                            "Hydration may borrow both owners without creating a new truth store.",
                    )
                commit(
                    home = home,
                    suffix = "other",
                    human = "A different turn mentions gardens and music.",
                    vex = "Keep that unrelated source separate.",
                )

                val memory = VexLifeMemoryContextFixture.current()
                val workingSet =
                    VexLifeWorkingContinuity.WorkingSet(
                        lineageRef = ownership.first.companionLineageRef,
                        threadRef = ownership.first.threadRef,
                        cursorEventRef = ownership.second.eventRef,
                        activeWorkNodeRefs = listOf("work.memory-depth"),
                        currentIntentReceiptRefs = emptyList(),
                        openLoopRefs = listOf("openloop.semantic-locator"),
                        currentStatementRefs = listOf("statement.score.continuity.001"),
                        currentContinuityRecordRefs = emptyList(),
                        currentDailyStratumRefOrNull = memory.currentDailyStratumRef,
                        sourceRefs = listOf(ownership.second.eventRef),
                    )

                val projection =
                    VexLifeContinuityHydration.hydrate(
                        productHome = home,
                        conversationOwner = this,
                        queryGroups =
                            listOf(
                                listOf("Conversation", "Memory"),
                                listOf("semantic owners", "separate"),
                                listOf("source-grounded"),
                            ),
                        memoryContext = memory,
                        workingSet = workingSet,
                        excludedConversationEventRefs = emptySet(),
                        excludedMemoryStatementRefs = emptySet(),
                        availableTokens = DirectPromptContext.InputTokenBudget,
                        estimateTokens = DirectPromptContext::estimatedMessageTokens,
                    )

                assertEquals(VexLifeContinuityHydration.Returned, projection.state)
                assertEquals(1, projection.selectedConversationTurnCount)
                assertEquals(1, projection.selectedMemoryPageCount)
                assertTrue(ownership.first.eventRef in projection.sourceRefs)
                assertTrue(ownership.second.eventRef in projection.sourceRefs)
                assertTrue("statement.score.continuity.001" in projection.sourceRefs)
                assertTrue("work.memory-depth" in projection.frontierRefs)
                assertTrue("openloop.semantic-locator" in projection.frontierRefs)
                assertTrue(projection.text.contains(ownership.first.content))
                assertTrue(projection.text.contains(ownership.second.content))
                assertTrue(
                    projection.text.contains(
                        "Victor prefers exact source-grounded continuation."
                    )
                )
                assertTrue(projection.text.contains("LEXICAL_PLUS_CURRENT_ANCHORS"))
                assertTrue(projection.text.contains("no semantic-similarity locator"))
                assertTrue(projection.duplicateCandidateCount > 0)
                assertFalse(projection.effectsPerformed)
                assertFalse(projection.conversationMutationPerformed)
                assertFalse(projection.memoryMutationPerformed)
                assertFalse(projection.semanticAcceptanceCreated)
            }
        } finally {
            home.parent.toFile().deleteRecursively()
        }
    }

    @Test
    fun sourceAlreadyMaterializedInThePromptIsNotReturnedAgain() {
        val home = productHome()
        val store = DirectConversationStore(home)

        try {
            store.withWriterLease {
                val older =
                    commit(
                        home = home,
                        suffix = "older",
                        human = "The earlier continuity phrase is alpha-orbit.",
                        vex = "The older alpha-orbit source remains durable.",
                    )
                val alreadyPresent =
                    commit(
                        home = home,
                        suffix = "present",
                        human = "The newest continuity phrase is alpha-orbit too.",
                        vex = "This alpha-orbit turn is already materialized in the prompt.",
                    )
                val memory = VexLifeMemoryContextFixture.current()

                val projection =
                    VexLifeContinuityHydration.hydrate(
                        productHome = home,
                        conversationOwner = this,
                        queryGroups = listOf(listOf("alpha-orbit")),
                        memoryContext = memory,
                        workingSet = null,
                        excludedConversationEventRefs =
                            setOf(alreadyPresent.first.eventRef, alreadyPresent.second.eventRef),
                        excludedMemoryStatementRefs = setOf("statement.score.continuity.001"),
                        availableTokens = DirectPromptContext.InputTokenBudget,
                        estimateTokens = DirectPromptContext::estimatedMessageTokens,
                    )

                assertEquals(VexLifeContinuityHydration.Returned, projection.state)
                assertTrue(older.first.eventRef in projection.sourceRefs)
                assertTrue(older.second.eventRef in projection.sourceRefs)
                assertFalse(alreadyPresent.first.eventRef in projection.sourceRefs)
                assertFalse(alreadyPresent.second.eventRef in projection.sourceRefs)
                assertFalse("statement.score.continuity.001" in projection.sourceRefs)
                assertFalse(projection.text.contains(alreadyPresent.first.content))
                assertTrue(projection.text.contains(older.first.content))
            }
        } finally {
            home.parent.toFile().deleteRecursively()
        }
    }

    @Test
    fun insufficientReturnBudgetNeverCutsAnExactHistoricalTurn() {
        val home = productHome()
        val store = DirectConversationStore(home)

        try {
            store.withWriterLease {
                val old =
                    commit(
                        home = home,
                        suffix = "budget",
                        human = "Exact whole source: " + "depth ".repeat(120),
                        vex = "Exact whole response: " + "surface ".repeat(120),
                    )
                val memory = VexLifeMemoryContextFixture.current()
                val resolution =
                    VexLifeContinuityHydration.resolve(
                        productHome = home,
                        conversationOwner = this,
                        queryGroups = listOf(listOf("Exact whole source")),
                        memoryContext = memory,
                        workingSet = null,
                    )
                val full =
                    VexLifeContinuityHydration.project(
                        resolution = resolution,
                        availableTokens = DirectPromptContext.InputTokenBudget,
                        estimateTokens = DirectPromptContext::estimatedMessageTokens,
                    )
                val constrained =
                    VexLifeContinuityHydration.project(
                        resolution = resolution,
                        availableTokens = 220,
                        estimateTokens = DirectPromptContext::estimatedMessageTokens,
                    )

                assertEquals(VexLifeContinuityHydration.Returned, full.state)
                assertTrue(full.text.contains(old.first.content))
                assertTrue(full.text.contains(old.second.content))
                assertFalse(constrained.text.contains(old.first.content))
                assertFalse(constrained.text.contains(old.second.content))
                assertTrue(
                    constrained.state == VexLifeContinuityHydration.HeldTokenBudget ||
                        old.first.eventRef in constrained.frontierRefs
                )
            }
        } finally {
            home.parent.toFile().deleteRecursively()
        }
    }
}

// [VXG RealForever]
