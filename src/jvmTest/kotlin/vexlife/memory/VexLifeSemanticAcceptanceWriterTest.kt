package vexlife.memory

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vexlife.conversation.CanonicalLivedContextFixture
import vexlife.conversation.DirectConversationIdentity
import vexlife.conversation.DirectConversationStore
import vexlife.home.VexLifeProdHomeFiles
import vexlife.home.CanonicalVexLifeHomeFixture

class VexLifeSemanticAcceptanceWriterTest {
    private data class ConversationFixture(
        val head: DirectConversationStore.Head,
        val request: DirectConversationStore.Event,
        val response: DirectConversationStore.Event,
    )

    @Test
    fun explicitKeepMaterializesAuthorityThenExistingScoreOwnerCommitsStatement() {
        withHome(
            "vexlife-semantic-acceptance-keep-",
        ) {
            home ->
            val conversation =
                completedConversation(
                    home,
                )
            val result =
                keep(
                    home = home,
                    sourceHash =
                        conversation.response.eventHash,
                )

            val acceptance =
                VexLifeCurrentMemoryReader
                    .readCurrentScoreAcceptance(
                        productHome = home,
                        acceptanceRef =
                            result.acceptanceRef,
                        acceptanceSha256 =
                            result.acceptanceSha256,
                    )
            assertEquals(
                result.semanticAuthorityHeadSha256,
                acceptance.semanticAuthorityHeadSha256,
            )
            assertEquals(
                "PERMITTED",
                acceptance.consent[
                    "disposition"
                ]
                    ?.toString()
                    ?.trim(
                        '"',
                    ),
            )
            assertTrue(
                acceptance.acceptance[
                    "acceptedForContinuity"
                ]
                    ?.toString()
                    ?.toBooleanStrictOrNull()
                    ?: false,
            )

            val score =
                VexLifeScoreContinuity
                    .appendAcceptedStatement(
                        productHome = home,
                        invocation =
                            VexLifeScoreContinuity.AppendStatementInvocation(
                                instanceRef =
                                    "instance.vexlife-prod.semantic-acceptance.score.test",
                                statementRef =
                                    "statement.vexlife-prod.semantic-acceptance.test.001",
                                semanticAcceptanceRef =
                                    result.acceptanceRef,
                                semanticAcceptanceSha256 =
                                    result.acceptanceSha256,
                                expectedScoreHeadSha256 =
                                    null,
                                formedAt =
                                    Instant.parse(
                                        "2026-11-18T06:10:02Z",
                                    ),
                            ),
                    )

            assertEquals(
                "COMMITTED",
                score.state,
            )
            assertEquals(
                listOf(
                    "statement.vexlife-prod.semantic-acceptance.test.001",
                ),
                score.currentStatementRefs,
            )
        }
    }

    @Test
    fun staleExpectedSemanticAuthorityHeadFailsBeforeAnotherGenerationBecomesCurrent() {
        withHome(
            "vexlife-semantic-acceptance-stale-",
        ) {
            home ->
            val conversation =
                completedConversation(
                    home,
                )
            val first =
                keep(
                    home = home,
                    sourceHash =
                        conversation.response.eventHash,
                )

            val failure =
                assertFailsWith<IllegalArgumentException> {
                    VexLifeSemanticAcceptanceWriter.keep(
                        productHome = home,
                        invocation =
                            invocation(
                                sourceHash =
                                    conversation.response.eventHash,
                                subjectRef =
                                    "subject.victor.preference.semantic-acceptance-second",
                                expectedAuthorityHeadSha256 =
                                    null,
                                decisionRef =
                                    "decision.memory.keep.test.002",
                                decisionSha256 =
                                    sha256(
                                        "keep-002",
                                    ),
                                formedAt =
                                    Instant.parse(
                                        "2026-11-18T06:11:00Z",
                                    ),
                            ),
                    )
                }
            assertTrue(
                failure.message
                    .orEmpty()
                    .contains(
                        "SEMANTIC_AUTHORITY_STALE",
                    ),
            )
            assertEquals(
                first.semanticAuthorityHeadSha256,
                VexLifeCurrentMemoryReader
                    .readCurrentSemanticAuthorityHead(
                        home,
                    )
                    ?.get(
                        "semanticAuthorityHeadSha256",
                    )
                    ?.toString()
                    ?.trim(
                        '"',
                    ),
            )
        }
    }

    @Test
    fun crashBeforeCurrentHeadAdvanceLeavesDurableChildrenNoncurrent() {
        withHome(
            "vexlife-semantic-acceptance-crash-",
        ) {
            home ->
            val conversation =
                completedConversation(
                    home,
                )
            assertFailsWith<IllegalStateException> {
                VexLifeSemanticAcceptanceWriter.keep(
                    productHome = home,
                    invocation =
                        invocation(
                            sourceHash =
                                conversation.response.eventHash,
                        ),
                    faults =
                        VexLifeSemanticAcceptanceWriter.Faults(
                            failBeforeHeadAdvance =
                                true,
                        ),
                )
            }

            assertNull(
                VexLifeCurrentMemoryReader
                    .readCurrentSemanticAuthorityHead(
                        home,
                    ),
            )
            val currentHome =
                requireNotNull(
                    VexLifeProdHomeFiles.observeExisting(
                        home,
                    ),
                )
            val root =
                home.resolve(
                    "semantic-authority/score/" +
                        currentHome.manifest.currentCompanionLineageRef +
                        "/" +
                        DirectConversationIdentity.ThreadRef,
                )
            assertTrue(
                Files.exists(
                    root.resolve(
                        "acceptances",
                    ),
                ),
            )
            assertFalse(
                Files.exists(
                    root.resolve(
                        "head.json",
                    ),
                ),
            )
        }
    }

    @Test
    fun sameSubjectRequiresExplicitTransitionAndExactCurrentScorePredecessor() {
        withHome(
            "vexlife-semantic-acceptance-transition-",
        ) {
            home ->
            val conversation =
                completedConversation(
                    home,
                )
            val first =
                keep(
                    home = home,
                    sourceHash =
                        conversation.response.eventHash,
                )
            VexLifeScoreContinuity
                .appendAcceptedStatement(
                    productHome = home,
                    invocation =
                        VexLifeScoreContinuity.AppendStatementInvocation(
                            instanceRef =
                                "instance.vexlife-prod.semantic-acceptance.transition-score.test",
                            statementRef =
                                "statement.vexlife-prod.semantic-acceptance.original",
                            semanticAcceptanceRef =
                                first.acceptanceRef,
                            semanticAcceptanceSha256 =
                                first.acceptanceSha256,
                            expectedScoreHeadSha256 =
                                null,
                            formedAt =
                                Instant.parse(
                                    "2026-11-18T06:12:01Z",
                                ),
                        ),
                )

            assertFailsWith<IllegalArgumentException> {
                VexLifeSemanticAcceptanceWriter.keep(
                    productHome = home,
                    invocation =
                        invocation(
                            sourceHash =
                                conversation.response.eventHash,
                            expectedAuthorityHeadSha256 =
                                first.semanticAuthorityHeadSha256,
                            decisionRef =
                                "decision.memory.keep.test.transition-none",
                            decisionSha256 =
                                sha256(
                                    "transition-none",
                                ),
                            formedAt =
                                Instant.parse(
                                    "2026-11-18T06:12:02Z",
                                ),
                        ),
                )
            }

            val corrected =
                VexLifeSemanticAcceptanceWriter.keep(
                    productHome = home,
                    invocation =
                        invocation(
                            sourceHash =
                                conversation.response.eventHash,
                            expectedAuthorityHeadSha256 =
                                first.semanticAuthorityHeadSha256,
                            decisionRef =
                                "decision.memory.keep.test.transition-correct",
                            decisionSha256 =
                                sha256(
                                    "transition-correct",
                                ),
                            formedAt =
                                Instant.parse(
                                    "2026-11-18T06:12:03Z",
                                ),
                            classification =
                                VexLifeSemanticAcceptanceWriter.Classification.Corrects(
                                    acceptedSummary =
                                        "Victor uses tight to mean compact and causally exact.",
                                    memoryRelation =
                                        VexLifeSemanticAcceptanceWriter.MemoryRelation.SHARED_RELATIONSHIP_HISTORY,
                                    targetStatementRef =
                                        "statement.vexlife-prod.semantic-acceptance.original",
                                    targetAcceptanceSha256 =
                                        first.acceptanceSha256,
                                ),
                        ),
                )

            val coordinates =
                VexLifeCurrentMemoryReader
                    .readCurrentScoreCoordinates(
                        home,
                    )
            val appended =
                VexLifeScoreContinuity
                    .appendAcceptedStatement(
                        productHome = home,
                        invocation =
                            VexLifeScoreContinuity.AppendStatementInvocation(
                                instanceRef =
                                    "instance.vexlife-prod.semantic-acceptance.corrected-score.test",
                                statementRef =
                                    "statement.vexlife-prod.semantic-acceptance.corrected",
                                semanticAcceptanceRef =
                                    corrected.acceptanceRef,
                                semanticAcceptanceSha256 =
                                    corrected.acceptanceSha256,
                                expectedScoreHeadSha256 =
                                    coordinates.scoreHeadSha256,
                                formedAt =
                                    Instant.parse(
                                        "2026-11-18T06:12:04Z",
                                    ),
                            ),
                    )

            assertEquals(
                listOf(
                    "statement.vexlife-prod.semantic-acceptance.corrected",
                ),
                appended.currentStatementRefs,
            )
        }
    }

    private fun keep(
        home: Path,
        sourceHash: String,
    ): VexLifeSemanticAcceptanceWriter.Result =
        VexLifeSemanticAcceptanceWriter.keep(
            productHome = home,
            invocation =
                invocation(
                    sourceHash =
                        sourceHash,
                ),
        )

    private fun invocation(
        sourceHash: String,
        subjectRef: String =
            "subject.victor.preference.semantic-acceptance",
        expectedAuthorityHeadSha256: String? =
            null,
        decisionRef: String =
            "decision.memory.keep.test.001",
        decisionSha256: String =
            sha256(
                "keep-001",
            ),
        formedAt: Instant =
            Instant.parse(
                "2026-11-18T06:10:01Z",
            ),
        classification: VexLifeSemanticAcceptanceWriter.Classification =
            VexLifeSemanticAcceptanceWriter.Classification.Initial(
                acceptedSummary =
                    "Victor uses tight to mean compact and causally exact.",
                memoryRelation =
                    VexLifeSemanticAcceptanceWriter.MemoryRelation.SHARED_RELATIONSHIP_HISTORY,
                statementState =
                    VexLifeSemanticAcceptanceWriter.InitialStatementState.HUMAN_CONFIRMED,
            ),
    ): VexLifeSemanticAcceptanceWriter.KeepInvocation =
        VexLifeSemanticAcceptanceWriter.KeepInvocation(
            instanceRef =
                "instance.vexlife-prod.semantic-acceptance.writer.test",
            proposal =
                VexLifeSemanticAcceptanceWriter.Proposal(
                    semanticSubjectRef =
                        subjectRef,
                    proposedSummary =
                        "Victor uses tight to mean compact and causally exact.",
                    memoryRelation =
                        VexLifeSemanticAcceptanceWriter.MemoryRelation.SHARED_RELATIONSHIP_HISTORY,
                    proposedStatementState =
                        VexLifeSemanticAcceptanceWriter.InitialStatementState.HUMAN_CONFIRMED,
                    proposerRef =
                        "owner.vexlife-prod.memory-consideration.test",
                    proposerClass =
                        VexLifeSemanticAcceptanceWriter.ProposerClass.LINEAGE,
                    sourceEventHashes =
                        listOf(
                            sourceHash,
                        ),
                ),
            classification =
                classification,
            humanDecision =
                VexLifeSemanticAcceptanceWriter.HumanDecisionSource(
                    humanSubjectRef =
                        "person.victor",
                    decisionRef =
                        decisionRef,
                    decisionSha256 =
                        decisionSha256,
                    decidedAt =
                        formedAt.minusMillis(
                            1,
                        ),
                ),
            expectedSemanticAuthorityHeadSha256 =
                expectedAuthorityHeadSha256,
            formedAt =
                formedAt,
        )

    private fun completedConversation(
        home: Path,
    ): ConversationFixture {
        val store =
            DirectConversationStore(
                home,
            )
        lateinit var result: ConversationFixture
        store.withWriterLease(
            instanceRef =
                "instance.vexlife-prod.semantic-acceptance.conversation.test",
        ) {
            val request =
                beginRequest(
                    turnRef =
                        "turn.vexlife-prod.semantic-acceptance.test",
                    requestMessageRef =
                        "message.vexlife-prod.semantic-acceptance.human.test",
                    content =
                        "When I say tight, I mean compact and causally exact.",
                    formedAt =
                        Instant.parse(
                            "2026-11-18T06:09:00Z",
                        ),
                )
            val response =
                appendResponse(
                    request = request,
                    responseMessageRef =
                        "message.vexlife-prod.semantic-acceptance.vex.test",
                    content =
                        "I understand the correction.",
                    model =
                        "default_model",
                    endpointOrigin =
                        "http://127.0.0.1:8765",
                    runtimeObservationRef =
                        "observation.vexlife-prod.semantic-acceptance.runtime.test",
                    invocationEvidenceRef =
                        "evidence.vexlife-prod.semantic-acceptance.invocation.test",
                    formedAt =
                        Instant.parse(
                            "2026-11-18T06:09:01Z",
                        ),
                )
            val context =
                CanonicalLivedContextFixture.write(
                    productHome = home,
                    request = request,
                    response = response,
                    formedAt =
                        Instant.parse(
                            "2026-11-18T06:09:02Z",
                        ),
                )
            val head =
                commitCompletedTurn(
                    request = request,
                    response = response,
                    contextSha256 =
                        context.sha256,
                    formedAt =
                        Instant.parse(
                            "2026-11-18T06:09:03Z",
                        ),
                )
            result =
                ConversationFixture(
                    head = head,
                    request = request,
                    response = response,
                )
        }
        return result
    }

    private inline fun withHome(
        prefix: String,
        block: (Path) -> Unit,
    ) {
        val parent =
            Files.createTempDirectory(
                prefix,
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                ".vexlife",
            )
        try {
            CanonicalVexLifeHomeFixture.form(
                home,
            )
            block(
                home,
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    private fun sha256(
        value: String,
    ): String =
        MessageDigest
            .getInstance(
                "SHA-256",
            )
            .digest(
                value.toByteArray(
                    StandardCharsets.UTF_8,
                ),
            )
            .joinToString(
                "",
            ) {
                "%02x".format(
                    it.toInt() and
                        0xff,
                )
            }
}

// [VXG RealForever]
