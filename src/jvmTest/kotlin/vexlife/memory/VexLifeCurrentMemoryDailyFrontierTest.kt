package vexlife.memory

import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import vexlife.consideration.VexLifeConsiderationContract
import vexlife.conversation.CanonicalLivedContextFixture
import vexlife.conversation.DirectConversationStore
import vexlife.home.CanonicalVexLifeHomeFixture

class VexLifeCurrentMemoryDailyFrontierTest {
    @Test
    fun laterKeepHoldsValidOlderDailyWhileCurrentAcceptedMeaningRemainsUsable() {
        withHome("vexlife-memory-daily-frontier-") { home ->
            val first =
                completedConversation(
                    home = home,
                    suffix = "tight",
                    requestContent = "When I say tight, I mean compact and causally exact.",
                    baseTime = Instant.parse("2026-11-18T10:00:00Z"),
                )
            keepCorrection(
                home = home,
                conversation = first,
                subjectRef = "subject.shared-language.tight",
                summary = TightSummary,
                scopeHint = "shared language: tight",
            )

            val firstFrontier = VexLifeCurrentMemoryReader.readDreamSourceFrontier(home)
            val daily =
                VexLifeDailyMemoryDream.commit(
                    productHome = home,
                    invocation =
                        VexLifeDailyMemoryDream.Invocation(
                            instanceRef = "instance.vexlife-prod.memory.daily-frontier.test",
                            restInvocationAuthorityRef = "authority.manual.memory.daily-frontier.test",
                            dayRef = "day.memory.daily-frontier.000",
                            dayIndex = 0,
                            calendarDateRef = "2026-11-18",
                            timeZoneRef = "America/Los_Angeles",
                            observedAt = Instant.parse("2026-11-18T10:01:00Z"),
                            expectedConversationHeadSha256 =
                                firstFrontier.conversationHeadSha256,
                            expectedScoreHeadSha256 = firstFrontier.scoreHeadSha256,
                            expectedDailyDreamHeadSha256 = null,
                        ),
                )

            val currentDaily = VexLifeCurrentMemoryReader.readCurrent(home)
            assertTrue(currentDaily.included)
            assertEquals(daily.sourceScoreHeadSha256, currentDaily.sourceScoreHeadSha256)
            assertEquals(
                daily.sourceSemanticAuthorityHeadSha256,
                currentDaily.sourceSemanticAuthorityHeadSha256,
            )

            val later =
                completedConversation(
                    home = home,
                    suffix = "causal",
                    requestContent = "When I say causal, preserve the actual cause and effect order.",
                    baseTime = Instant.parse("2026-11-18T11:00:00Z"),
                )
            keepCorrection(
                home = home,
                conversation = later,
                subjectRef = "subject.shared-language.causal",
                summary = CausalSummary,
                scopeHint = "shared language: causal",
            )

            val currentScore = VexLifeCurrentMemoryReader.readCurrentScoreCoordinates(home)
            assertNotEquals(daily.sourceScoreHeadSha256, currentScore.scoreHeadSha256)
            assertNotEquals(
                daily.sourceSemanticAuthorityHeadSha256,
                currentScore.semanticAuthorityHeadSha256,
            )

            val heldDaily = VexLifeCurrentMemoryReader.readCurrent(home)
            assertFalse(heldDaily.included)
            assertEquals("HELD", heldDaily.projectionCurrentness)
            assertEquals(listOf("DAILY_MEMORY_SOURCE_FRONTIER_STALE"), heldDaily.reasons)
            assertTrue(heldDaily.pages.isEmpty())

            val projection = VexLifeCurrentAcceptedSemanticProjection.read(home)
            assertEquals("CURRENT", projection.state)
            assertTrue(projection.statements.any { it.summary == TightSummary })
            assertTrue(projection.statements.any { it.summary == CausalSummary })

            val block = VexLifeCurrentAcceptedSemanticPromptBlock.form(projection)
            assertTrue(block.included)
            val content = assertNotNull(block.content)
            assertTrue(content.contains(TightSummary))
            assertTrue(content.contains(CausalSummary))
        }
    }

    private fun keepCorrection(
        home: Path,
        conversation: ConversationFixture,
        subjectRef: String,
        summary: String,
        scopeHint: String,
    ) {
        val pending =
            VexLifeMemoryReviewOwner.proposeHumanCorrection(
                productHome = home,
                invocation =
                    VexLifeMemoryReviewOwner.ProposeHumanCorrectionInvocation(
                        semanticSubjectRef = subjectRef,
                        salience = correctionSalience(summary = summary, scopeHint = scopeHint),
                        sourceEventHashes = conversation.sourceEventHashes,
                        formedAt = conversation.baseTime.plusSeconds(10),
                    ),
            )
        VexLifeMemoryReviewOwner.act(
            productHome = home,
            control = VexLifeMemoryReviewFamily.Keep(pending.candidateRef),
            humanSubjectRef = "person.local-user",
            decidedAt = conversation.baseTime.plusSeconds(20),
        )
    }

    private fun correctionSalience(
        summary: String,
        scopeHint: String,
    ): VexLifeConsiderationContract.Salience =
        VexLifeConsiderationContract.Salience(
            selfAttributedVantage =
                VexLifeConsiderationContract.SelfAttributedVantage(
                    feltImportance = VexLifeConsiderationContract.Level.HIGH,
                    feltUncertainty = VexLifeConsiderationContract.Level.LOW,
                    feltRelationshipShift =
                        VexLifeConsiderationContract.RelationshipShift.CORRECTION,
                    feltNeedToReturn = VexLifeConsiderationContract.ReturnNeed.YES,
                ),
            continuitySignals =
                VexLifeConsiderationContract.ContinuitySignals(
                    olderSharedContextWasNeeded = false,
                    currentExternalTruthWasNeeded = false,
                    humanCorrectionObserved = true,
                    explicitHumanMemoryIntent = false,
                    futureUsefulness = VexLifeConsiderationContract.Level.HIGH,
                    unresolvedOpenLoop = false,
                ),
            memoryCandidate =
                VexLifeConsiderationContract.MemoryCandidate(
                    disposition =
                        VexLifeConsiderationContract.MemoryCandidateDisposition.CANDIDATE,
                    candidateClass =
                        VexLifeConsiderationContract.MemoryCandidateClass.CORRECTION,
                    summary = summary,
                    scopeHint = scopeHint,
                ),
        )

    private data class ConversationFixture(
        val request: DirectConversationStore.Event,
        val response: DirectConversationStore.Event,
        val headSha256: String,
        val baseTime: Instant,
    ) {
        val sourceEventHashes: List<String>
            get() = listOf(request.eventHash, response.eventHash)
    }

    private fun completedConversation(
        home: Path,
        suffix: String,
        requestContent: String,
        baseTime: Instant,
    ): ConversationFixture {
        val store = DirectConversationStore(home)
        lateinit var fixture: ConversationFixture
        store.withWriterLease(
            instanceRef = "instance.vexlife-prod.memory.daily-frontier.$suffix"
        ) {
            val request =
                beginRequest(
                    turnRef = "turn.vexlife-prod.memory.daily-frontier.$suffix",
                    requestMessageRef =
                        "message.vexlife-prod.memory.daily-frontier.human.$suffix",
                    content = requestContent,
                    formedAt = baseTime,
                )
            val response =
                appendResponse(
                    request = request,
                    responseMessageRef =
                        "message.vexlife-prod.memory.daily-frontier.vex.$suffix",
                    content = "I understand.",
                    model = "default_model",
                    endpointOrigin = "http://127.0.0.1:8765",
                    runtimeObservationRef =
                        "observation.vexlife-prod.memory.daily-frontier.runtime.$suffix",
                    invocationEvidenceRef =
                        "evidence.vexlife-prod.memory.daily-frontier.invocation.$suffix",
                    formedAt = baseTime.plusSeconds(1),
                )
            val context =
                CanonicalLivedContextFixture.write(
                    productHome = home,
                    request = request,
                    response = response,
                    formedAt = baseTime.plusSeconds(2),
                )
            val head =
                commitCompletedTurn(
                    request = request,
                    response = response,
                    contextSha256 = context.sha256,
                    formedAt = baseTime.plusSeconds(3),
                )
            fixture =
                ConversationFixture(
                    request = request,
                    response = response,
                    headSha256 = head.conversationHeadSha256,
                    baseTime = baseTime,
                )
        }
        return fixture
    }

    private inline fun withHome(
        prefix: String,
        block: (Path) -> Unit,
    ) {
        val parent = Files.createTempDirectory(prefix).toAbsolutePath()
        val home = parent.resolve(".vexlife")
        try {
            CanonicalVexLifeHomeFixture.form(home)
            block(home)
        } finally {
            parent.toFile().deleteRecursively()
        }
    }

    private companion object {
        const val TightSummary = "Victor uses tight to mean compact and causally exact."
        const val CausalSummary = "Victor uses causal to mean preserving actual cause and effect order."
    }
}

// [VXG RealForever]
