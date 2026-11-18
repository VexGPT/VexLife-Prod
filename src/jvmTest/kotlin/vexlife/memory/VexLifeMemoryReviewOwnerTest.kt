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
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.jsonPrimitive
import vexlife.consideration.VexLifeConsiderationContract
import vexlife.conversation.CanonicalLivedContextFixture
import vexlife.conversation.DirectConversationStore
import vexlife.home.CanonicalVexLifeHomeFixture

class VexLifeMemoryReviewOwnerTest {
    private data class ConversationFixture(
        val request: DirectConversationStore.Event,
        val response: DirectConversationStore.Event,
        val headSha256: String,
    ) {
        val sourceEventHashes: List<String>
            get() =
                listOf(
                    request.eventHash,
                    response.eventHash,
                )
    }

    @Test
    fun humanCorrectionBecomesPendingWithoutMemoryMutation() {
        withHome(
            "vexlife-memory-review-pending-",
        ) {
            home ->
            val conversation =
                completedConversation(
                    home = home,
                    suffix = "pending",
                    requestContent =
                        "When I say tight, I mean compact and causally exact.",
                )
            val pending =
                propose(
                    home = home,
                    conversation =
                        conversation,
                )

            val observed =
                VexLifeMemoryReviewOwner.observe(
                    home,
                )
            assertEquals(
                VexLifeMemoryReviewFamily.Pending(
                    candidateRef =
                        pending.candidateRef,
                    summary =
                        Summary,
                    candidateClass =
                        VexLifeConsiderationContract.MemoryCandidateClass.CORRECTION,
                ),
                observed,
            )
            assertNull(
                VexLifeCurrentMemoryReader
                    .readCurrentSemanticAuthorityHead(
                        home,
                    ),
            )
            assertNull(
                VexLifeCurrentMemoryReader
                    .readCurrentScoreCoordinates(
                        home,
                    )
                    .scoreHeadSha256,
            )
        }
    }

    @Test
    fun notNowAndDeclineResolveReviewWithoutAcceptanceOrScore() {
        for (
            controlRef in
            listOf(
                VexLifeMemoryReviewFamily.ControlNotNow,
                VexLifeMemoryReviewFamily.ControlDecline,
            )
        ) {
            withHome(
                "vexlife-memory-review-no-write-",
            ) {
                home ->
                val conversation =
                    completedConversation(
                        home = home,
                        suffix =
                            controlRef.lowercase(),
                        requestContent =
                            "When I say tight, I mean compact and causally exact.",
                    )
                val pending =
                    propose(
                        home = home,
                        conversation =
                            conversation,
                    )
                val control =
                    requireNotNull(
                        VexLifeMemoryReviewFamily.controlForRef(
                            controlRef =
                                controlRef,
                            candidateRef =
                                pending.candidateRef,
                        ),
                    )
                VexLifeMemoryReviewOwner.act(
                    productHome = home,
                    control = control,
                    humanSubjectRef =
                        "person.local-user",
                    decidedAt =
                        Instant.parse(
                            "2026-11-18T08:10:00Z",
                        ),
                )

                assertEquals(
                    VexLifeMemoryReviewFamily.Idle,
                    VexLifeMemoryReviewOwner.observe(
                        home,
                    ),
                )
                assertNull(
                    VexLifeCurrentMemoryReader
                        .readCurrentSemanticAuthorityHead(
                            home,
                        ),
                )
                assertNull(
                    VexLifeCurrentMemoryReader
                        .readCurrentScoreCoordinates(
                            home,
                        )
                        .scoreHeadSha256,
                )
            }
        }
    }

    @Test
    fun keepUsesSemanticWriterThenScoreExactlyOnce() {
        withHome(
            "vexlife-memory-review-keep-",
        ) {
            home ->
            val conversation =
                completedConversation(
                    home = home,
                    suffix = "keep",
                    requestContent =
                        "When I say tight, I mean compact and causally exact.",
                )
            val pending =
                propose(
                    home = home,
                    conversation =
                        conversation,
                )
            val result =
                VexLifeMemoryReviewOwner.act(
                    productHome = home,
                    control =
                        VexLifeMemoryReviewFamily.Keep(
                            pending.candidateRef,
                        ),
                    humanSubjectRef =
                        "person.local-user",
                    decidedAt =
                        Instant.parse(
                            "2026-11-18T08:20:00Z",
                        ),
                ) as VexLifeMemoryReviewOwner.ActionResult.Kept

            assertFalse(
                result.semanticAcceptanceRecovered,
            )
            assertFalse(
                result.scoreStatementRecovered,
            )
            val binding =
                assertNotNull(
                    VexLifeCurrentMemoryReader
                        .readCurrentScoreStatementBinding(
                            productHome = home,
                            statementRef =
                                result.statementRef,
                        ),
                )
            assertEquals(
                result.semanticAcceptanceRef,
                binding.semanticAcceptanceRef,
            )
            assertEquals(
                result.semanticAcceptanceSha256,
                binding.semanticAcceptanceSha256,
            )
            val acceptance =
                VexLifeCurrentMemoryReader
                    .readCurrentScoreAcceptance(
                        productHome = home,
                        acceptanceRef =
                            result.semanticAcceptanceRef,
                        acceptanceSha256 =
                            result.semanticAcceptanceSha256,
                    )
            assertNotEquals(
                pending.proposalCandidateRef,
                acceptance.candidate[
                    "candidateRef"
                ]
                    ?.jsonPrimitive
                    ?.content,
                "Keep must re-form the semantic candidate at human-decision time",
            )
            assertEquals(
                pending.semanticSubjectFingerprint,
                acceptance.candidate[
                    "semanticSubjectFingerprint"
                ]
                    ?.jsonPrimitive
                    ?.content,
            )
            assertEquals(
                VexLifeMemoryReviewFamily.Idle,
                VexLifeMemoryReviewOwner.observe(
                    home,
                ),
            )
        }
    }

    @Test
    fun keepRejectsStaleConversationBeforeDecisionOrMemoryMutation() {
        withHome(
            "vexlife-memory-review-stale-",
        ) {
            home ->
            val first =
                completedConversation(
                    home = home,
                    suffix = "stale-first",
                    requestContent =
                        "When I say tight, I mean compact and causally exact.",
                )
            val pending =
                propose(
                    home = home,
                    conversation =
                        first,
                )
            completedConversation(
                home = home,
                suffix = "stale-second",
                requestContent =
                    "Tell me something unrelated.",
                baseTime =
                    Instant.parse(
                        "2026-11-18T08:01:00Z",
                    ),
            )

            val failure =
                assertFailsWith<IllegalArgumentException> {
                    VexLifeMemoryReviewOwner.act(
                        productHome = home,
                        control =
                            VexLifeMemoryReviewFamily.Keep(
                                pending.candidateRef,
                            ),
                        humanSubjectRef =
                            "person.local-user",
                        decidedAt =
                            Instant.parse(
                                "2026-11-18T08:30:00Z",
                            ),
                    )
                }
            assertTrue(
                failure.message
                    .orEmpty()
                    .contains(
                        "MEMORY_REVIEW_STALE",
                    ),
            )
            assertTrue(
                VexLifeMemoryReviewOwner.observe(
                    home,
                ) is VexLifeMemoryReviewFamily.Pending,
            )
            assertNull(
                VexLifeCurrentMemoryReader
                    .readCurrentSemanticAuthorityHead(
                        home,
                    ),
            )
        }
    }

    @Test
    fun retryAfterAcceptanceRecoversExactDecisionAndAppendsScoreOnce() {
        withHome(
            "vexlife-memory-review-recover-acceptance-",
        ) {
            home ->
            val conversation =
                completedConversation(
                    home = home,
                    suffix = "recover-acceptance",
                    requestContent =
                        "When I say tight, I mean compact and causally exact.",
                )
            val pending =
                propose(
                    home = home,
                    conversation =
                        conversation,
                )
            val decidedAt =
                Instant.parse(
                    "2026-11-18T08:40:00Z",
                )
            assertFailsWith<IllegalStateException> {
                VexLifeMemoryReviewOwner.act(
                    productHome = home,
                    control =
                        VexLifeMemoryReviewFamily.Keep(
                            pending.candidateRef,
                        ),
                    humanSubjectRef =
                        "person.local-user",
                    decidedAt =
                        decidedAt,
                    faults =
                        VexLifeMemoryReviewOwner.Faults(
                            failAfterSemanticAcceptance =
                                true,
                        ),
                )
            }
            assertTrue(
                VexLifeMemoryReviewOwner.observe(
                    home,
                ) is VexLifeMemoryReviewFamily.Pending,
            )

            val recovered =
                VexLifeMemoryReviewOwner.act(
                    productHome = home,
                    control =
                        VexLifeMemoryReviewFamily.Keep(
                            pending.candidateRef,
                        ),
                    humanSubjectRef =
                        "person.local-user",
                    decidedAt =
                        decidedAt,
                ) as VexLifeMemoryReviewOwner.ActionResult.Kept

            assertTrue(
                recovered.semanticAcceptanceRecovered,
            )
            assertFalse(
                recovered.scoreStatementRecovered,
            )
            assertNotNull(
                VexLifeCurrentMemoryReader
                    .readCurrentScoreStatementBinding(
                        productHome = home,
                        statementRef =
                            recovered.statementRef,
                    ),
            )
        }
    }

    @Test
    fun retryAfterScoreRecoversBothOwnersWithoutDuplicateStatement() {
        withHome(
            "vexlife-memory-review-recover-score-",
        ) {
            home ->
            val conversation =
                completedConversation(
                    home = home,
                    suffix = "recover-score",
                    requestContent =
                        "When I say tight, I mean compact and causally exact.",
                )
            val pending =
                propose(
                    home = home,
                    conversation =
                        conversation,
                )
            val decidedAt =
                Instant.parse(
                    "2026-11-18T08:50:00Z",
                )
            assertFailsWith<IllegalStateException> {
                VexLifeMemoryReviewOwner.act(
                    productHome = home,
                    control =
                        VexLifeMemoryReviewFamily.Keep(
                            pending.candidateRef,
                        ),
                    humanSubjectRef =
                        "person.local-user",
                    decidedAt =
                        decidedAt,
                    faults =
                        VexLifeMemoryReviewOwner.Faults(
                            failAfterScore =
                                true,
                        ),
                )
            }
            val coordinatesBeforeRetry =
                VexLifeCurrentMemoryReader
                    .readCurrentScoreCoordinates(
                        home,
                    )

            val recovered =
                VexLifeMemoryReviewOwner.act(
                    productHome = home,
                    control =
                        VexLifeMemoryReviewFamily.Keep(
                            pending.candidateRef,
                        ),
                    humanSubjectRef =
                        "person.local-user",
                    decidedAt =
                        decidedAt,
                ) as VexLifeMemoryReviewOwner.ActionResult.Kept

            assertTrue(
                recovered.semanticAcceptanceRecovered,
            )
            assertTrue(
                recovered.scoreStatementRecovered,
            )
            assertEquals(
                coordinatesBeforeRetry.scoreHeadSha256,
                recovered.scoreHeadSha256,
            )
            assertEquals(
                VexLifeMemoryReviewFamily.Idle,
                VexLifeMemoryReviewOwner.observe(
                    home,
                ),
            )
        }
    }

    private fun propose(
        home: Path,
        conversation: ConversationFixture,
    ): VexLifeMemoryReviewOwner.PendingReview =
        VexLifeMemoryReviewOwner.proposeHumanCorrection(
            productHome = home,
            invocation =
                VexLifeMemoryReviewOwner.ProposeHumanCorrectionInvocation(
                    semanticSubjectRef =
                        "subject.shared-language.tight",
                    salience =
                        correctionSalience(),
                    sourceEventHashes =
                        conversation.sourceEventHashes,
                    formedAt =
                        Instant.parse(
                            "2026-11-18T08:00:00Z",
                        ),
                ),
        )

    private fun correctionSalience(): VexLifeConsiderationContract.Salience =
        VexLifeConsiderationContract.Salience(
            selfAttributedVantage =
                VexLifeConsiderationContract.SelfAttributedVantage(
                    feltImportance =
                        VexLifeConsiderationContract.Level.HIGH,
                    feltUncertainty =
                        VexLifeConsiderationContract.Level.LOW,
                    feltRelationshipShift =
                        VexLifeConsiderationContract.RelationshipShift.CORRECTION,
                    feltNeedToReturn =
                        VexLifeConsiderationContract.ReturnNeed.YES,
                ),
            continuitySignals =
                VexLifeConsiderationContract.ContinuitySignals(
                    olderSharedContextWasNeeded =
                        false,
                    currentExternalTruthWasNeeded =
                        false,
                    humanCorrectionObserved =
                        true,
                    explicitHumanMemoryIntent =
                        false,
                    futureUsefulness =
                        VexLifeConsiderationContract.Level.HIGH,
                    unresolvedOpenLoop =
                        false,
                ),
            memoryCandidate =
                VexLifeConsiderationContract.MemoryCandidate(
                    disposition =
                        VexLifeConsiderationContract.MemoryCandidateDisposition.CANDIDATE,
                    candidateClass =
                        VexLifeConsiderationContract.MemoryCandidateClass.CORRECTION,
                    summary =
                        Summary,
                    scopeHint =
                        "shared language: tight",
                ),
        )

    private fun completedConversation(
        home: Path,
        suffix: String,
        requestContent: String,
        baseTime: Instant =
            Instant.parse(
                "2026-11-18T07:59:00Z",
            ),
    ): ConversationFixture {
        val store =
            DirectConversationStore(
                home,
            )
        lateinit var fixture: ConversationFixture
        store.withWriterLease(
            instanceRef =
                "instance.vexlife-prod.memory-review.conversation." +
                    suffix,
        ) {
            val request =
                beginRequest(
                    turnRef =
                        "turn.vexlife-prod.memory-review." +
                            suffix,
                    requestMessageRef =
                        "message.vexlife-prod.memory-review.human." +
                            suffix,
                    content =
                        requestContent,
                    formedAt =
                        baseTime,
                )
            val response =
                appendResponse(
                    request = request,
                    responseMessageRef =
                        "message.vexlife-prod.memory-review.vex." +
                            suffix,
                    content =
                        "I understand.",
                    model =
                        "default_model",
                    endpointOrigin =
                        "http://127.0.0.1:8765",
                    runtimeObservationRef =
                        "observation.vexlife-prod.memory-review.runtime." +
                            suffix,
                    invocationEvidenceRef =
                        "evidence.vexlife-prod.memory-review.invocation." +
                            suffix,
                    formedAt =
                        baseTime.plusSeconds(
                            1,
                        ),
                )
            val context =
                CanonicalLivedContextFixture.write(
                    productHome = home,
                    request = request,
                    response = response,
                    formedAt =
                        baseTime.plusSeconds(
                            2,
                        ),
                )
            val head =
                commitCompletedTurn(
                    request = request,
                    response = response,
                    contextSha256 =
                        context.sha256,
                    formedAt =
                        baseTime.plusSeconds(
                            3,
                        ),
                )
            fixture =
                ConversationFixture(
                    request = request,
                    response = response,
                    headSha256 =
                        head.conversationHeadSha256,
                )
        }
        return fixture
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

    private companion object {
        const val Summary =
            "Victor uses tight to mean compact and causally exact."
    }
}

// [VXG RealForever]
