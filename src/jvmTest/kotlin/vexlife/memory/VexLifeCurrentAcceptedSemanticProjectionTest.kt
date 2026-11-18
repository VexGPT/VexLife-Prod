package vexlife.memory

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
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

class VexLifeCurrentAcceptedSemanticProjectionTest {
    @Test
    fun noScoreProjectsEmptyWithoutInventingAcceptedContinuity() {
        withHome(
            "vexlife-current-semantic-empty-",
        ) {
            home ->
            val before =
                fileDigests(
                    home,
                )
            val projection =
                VexLifeCurrentAcceptedSemanticProjection
                    .read(
                        home,
                    )
            val after =
                fileDigests(
                    home,
                )

            assertEquals(
                "EMPTY",
                projection.state,
            )
            assertTrue(
                projection.statements.isEmpty(),
            )
            assertEquals(
                null,
                projection.scoreHeadSha256,
            )
            assertEquals(
                null,
                projection.semanticAuthorityHeadSha256,
            )
            assertFalse(
                projection.effectsPerformed,
            )
            assertEquals(
                before,
                after,
                "accepted-semantic projection must be read-only",
            )
        }
    }

    @Test
    fun keepIsCurrentImmediatelyEvenWithoutDailyMemory() {
        withHome(
            "vexlife-current-semantic-keep-",
        ) {
            home ->
            val first =
                completedConversation(
                    home = home,
                    suffix = "keep",
                    requestContent =
                        "When I say tight, I mean compact and causally exact.",
                    baseTime =
                        Instant.parse(
                            "2026-11-18T09:00:00Z",
                        ),
                )
            keepCorrection(
                home = home,
                conversation =
                    first,
            )

            val projection =
                VexLifeCurrentAcceptedSemanticProjection
                    .read(
                        home,
                    )
            assertEquals(
                "CURRENT",
                projection.state,
            )
            assertEquals(
                1,
                projection.statements.size,
            )
            val statement =
                projection.statements.single()
            assertEquals(
                Summary,
                statement.summary,
            )
            assertTrue(
                statement.consentState in
                    setOf(
                        "PERMITTED",
                        "NARROWED",
                    ),
            )
            assertEquals(
                first.headSha256,
                projection.observedConversationHeadSha256,
            )
            assertEquals(
                first.headSha256,
                projection.sourceScoreConversationHeadSha256,
            )
            assertNotNull(
                projection.scoreHeadSha256,
            )
            assertNotNull(
                projection.semanticAuthorityHeadSha256,
            )

            val dailyBacked =
                VexLifeCurrentMemoryReader
                    .readCurrent(
                        home,
                    )
            assertFalse(
                dailyBacked.included,
            )
            assertEquals(
                listOf(
                    "NO_COMMITTED_DAILY_MEMORY",
                ),
                dailyBacked.reasons,
            )
        }
    }

    @Test
    fun unrelatedConversationAdvanceDoesNotInvalidateAcceptedMeaning() {
        withHome(
            "vexlife-current-semantic-later-turn-",
        ) {
            home ->
            val first =
                completedConversation(
                    home = home,
                    suffix = "first",
                    requestContent =
                        "When I say tight, I mean compact and causally exact.",
                    baseTime =
                        Instant.parse(
                            "2026-11-18T09:10:00Z",
                        ),
                )
            keepCorrection(
                home = home,
                conversation =
                    first,
            )
            val before =
                VexLifeCurrentAcceptedSemanticProjection
                    .read(
                        home,
                    )

            val later =
                completedConversation(
                    home = home,
                    suffix = "later",
                    requestContent =
                        "Tell me something unrelated.",
                    baseTime =
                        Instant.parse(
                            "2026-11-18T09:20:00Z",
                        ),
                )
            val after =
                VexLifeCurrentAcceptedSemanticProjection
                    .read(
                        home,
                    )

            assertEquals(
                before.statements,
                after.statements,
            )
            assertEquals(
                before.scoreHeadSha256,
                after.scoreHeadSha256,
            )
            assertEquals(
                before.semanticAuthorityHeadSha256,
                after.semanticAuthorityHeadSha256,
            )
            assertEquals(
                first.headSha256,
                after.sourceScoreConversationHeadSha256,
            )
            assertEquals(
                later.headSha256,
                after.observedConversationHeadSha256,
            )
            assertNotEquals(
                after.sourceScoreConversationHeadSha256,
                after.observedConversationHeadSha256,
                "accepted semantic currentness must stay distinct from Score source frontier freshness",
            )
        }
    }

    @Test
    fun repeatedProjectionReadPerformsNoHomeMutation() {
        withHome(
            "vexlife-current-semantic-readonly-",
        ) {
            home ->
            val conversation =
                completedConversation(
                    home = home,
                    suffix = "readonly",
                    requestContent =
                        "When I say tight, I mean compact and causally exact.",
                    baseTime =
                        Instant.parse(
                            "2026-11-18T09:30:00Z",
                        ),
                )
            keepCorrection(
                home = home,
                conversation =
                    conversation,
            )
            val before =
                fileDigests(
                    home,
                )
            val first =
                VexLifeCurrentAcceptedSemanticProjection
                    .read(
                        home,
                    )
            val second =
                VexLifeCurrentAcceptedSemanticProjection
                    .read(
                        home,
                    )
            val after =
                fileDigests(
                    home,
                )

            assertEquals(
                first,
                second,
            )
            assertEquals(
                before,
                after,
                "projection read must not materialize or rewrite any Home object",
            )
        }
    }

    private fun keepCorrection(
        home: Path,
        conversation: ConversationFixture,
    ) {
        val pending =
            VexLifeMemoryReviewOwner
                .proposeHumanCorrection(
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
                                conversation.baseTime.plusSeconds(
                                    10,
                                ),
                        ),
                )
        VexLifeMemoryReviewOwner.act(
            productHome = home,
            control =
                VexLifeMemoryReviewFamily.Keep(
                    pending.candidateRef,
                ),
            humanSubjectRef =
                "person.local-user",
            decidedAt =
                conversation.baseTime.plusSeconds(
                    20,
                ),
        )
    }

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

    private data class ConversationFixture(
        val request: DirectConversationStore.Event,
        val response: DirectConversationStore.Event,
        val headSha256: String,
        val baseTime: Instant,
    ) {
        val sourceEventHashes: List<String>
            get() =
                listOf(
                    request.eventHash,
                    response.eventHash,
                )
    }

    private fun completedConversation(
        home: Path,
        suffix: String,
        requestContent: String,
        baseTime: Instant,
    ): ConversationFixture {
        val store =
            DirectConversationStore(
                home,
            )
        lateinit var fixture: ConversationFixture
        store.withWriterLease(
            instanceRef =
                "instance.vexlife-prod.current-semantic." +
                    suffix,
        ) {
            val request =
                beginRequest(
                    turnRef =
                        "turn.vexlife-prod.current-semantic." +
                            suffix,
                    requestMessageRef =
                        "message.vexlife-prod.current-semantic.human." +
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
                        "message.vexlife-prod.current-semantic.vex." +
                            suffix,
                    content =
                        "I understand.",
                    model =
                        "default_model",
                    endpointOrigin =
                        "http://127.0.0.1:8765",
                    runtimeObservationRef =
                        "observation.vexlife-prod.current-semantic.runtime." +
                            suffix,
                    invocationEvidenceRef =
                        "evidence.vexlife-prod.current-semantic.invocation." +
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
                    baseTime =
                        baseTime,
                )
        }
        return fixture
    }

    private fun fileDigests(
        home: Path,
    ): Map<String, String> =
        Files.walk(
            home,
        )
            .use {
                stream ->
                stream
                    .filter {
                        path ->
                        Files.isRegularFile(
                            path,
                        )
                    }
                    .toList()
                    .associate {
                        path ->
                        home.relativize(
                            path,
                        )
                            .toString() to
                            sha256(
                                Files.readAllBytes(
                                    path,
                                ),
                            )
                    }
            }
            .toSortedMap()

    private fun sha256(
        value: ByteArray,
    ): String =
        MessageDigest
            .getInstance(
                "SHA-256",
            )
            .digest(
                value,
            )
            .joinToString(
                "",
            ) {
                "%02x".format(
                    it.toInt() and
                        0xff,
                )
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

    private companion object {
        const val Summary =
            "Victor uses tight to mean compact and causally exact."
    }
}

// [VXG RealForever]
