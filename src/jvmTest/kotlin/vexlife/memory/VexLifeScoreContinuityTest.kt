package vexlife.memory

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import vexlife.contract.VexContractJson
import vexlife.contract.VexDurableContractFile
import vexlife.conversation.CanonicalLivedContextFixture
import vexlife.conversation.DirectConversationIdentity
import vexlife.conversation.DirectConversationStore
import vexlife.filesystem.VexOwnedPath
import vexlife.home.CanonicalVexLifeHomeFixture

class VexLifeScoreContinuityTest {
    private data class ConversationFixture(
        val head: DirectConversationStore.Head,
        val request: DirectConversationStore.Event,
        val response: DirectConversationStore.Event,
    )

    private data class AuthorityFixture(
        val acceptanceRef: String,
        val acceptanceSha256: String,
        val semanticAuthorityHeadSha256: String,
    )

    @Test
    fun candidateUsesExactCurrentConversationWithoutMintingSemanticAuthority() {
        withHome(
            "vexlife-score-candidate-",
        ) {
            home ->
            val conversation =
                completedConversation(
                    home,
                )
            val candidate =
                VexLifeScoreContinuity.formCandidate(
                    productHome = home,
                    input =
                        VexLifeScoreContinuity.CandidateInput(
                            semanticSubjectRef =
                                "subject.victor.preference.score-test",
                            proposedSummary =
                                "Victor prefers exact source ownership.",
                            proposedMemoryRelation =
                                "SHARED_RELATIONSHIP_HISTORY",
                            proposedStatementState =
                                "INFERRED",
                            proposerRef =
                                "owner.vexlife-prod.score-candidate.test",
                            proposerClass =
                                "LINEAGE",
                            formedAt =
                                Instant.parse(
                                    "2026-11-13T10:20:00Z",
                                ),
                        ),
                )

            assertEquals(
                conversation.head.conversationHeadSha256,
                candidate.sourceConversationHeadSha256,
            )
            assertEquals(
                listOf(
                    conversation.request.eventHash,
                    conversation.response.eventHash,
                ),
                candidate.sourceEventHashes,
            )
            assertFalse(
                candidate.candidate
                    .jsonObject
                    .toString()
                    .contains(
                        "Score source request",
                    ),
            )
            assertFalse(
                candidate.candidate
                    .jsonObject
                    .toString()
                    .contains(
                        "Score source response",
                    ),
            )
            assertFalse(
                Files.exists(
                    home.resolve(
                        "semantic-authority",
                    ),
                ),
            )
        }
    }

    @Test
    fun externallyOwnedDeferredAcceptanceCommitsScoreAndMakesDreamSourceCaughtUp() {
        withHome(
            "vexlife-score-accepted-",
        ) {
            home ->
            val conversation =
                completedConversation(
                    home,
                )
            val candidate =
                candidate(
                    home,
                )
            val authority =
                materializeDeferredAuthority(
                    home = home,
                    candidate = candidate.candidate,
                    formedAt =
                        "2026-11-13T10:21:00.000Z",
                )

            val result =
                VexLifeScoreContinuity.appendAcceptedStatement(
                    productHome = home,
                    invocation =
                        VexLifeScoreContinuity.AppendStatementInvocation(
                            instanceRef =
                                "instance.vexlife-prod.score.test",
                            statementRef =
                                "statement.vexlife-prod.score.test.001",
                            semanticAcceptanceRef =
                                authority.acceptanceRef,
                            semanticAcceptanceSha256 =
                                authority.acceptanceSha256,
                            expectedScoreHeadSha256 =
                                null,
                            formedAt =
                                Instant.parse(
                                    "2026-11-13T10:22:00Z",
                                ),
                        ),
                )

            assertEquals(
                "COMMITTED",
                result.state,
            )
            assertEquals(
                conversation.head.conversationHeadSha256,
                result.sourceConversationHeadSha256,
            )
            assertEquals(
                authority.semanticAuthorityHeadSha256,
                result.semanticAuthorityHeadSha256,
            )
            assertEquals(
                listOf(
                    "statement.vexlife-prod.score.test.001",
                ),
                result.currentStatementRefs,
            )

            val dream =
                VexLifeCurrentMemoryReader
                    .readDreamSourceFrontier(
                        home,
                    )
            assertEquals(
                conversation.head.conversationHeadSha256,
                dream.conversationHeadSha256,
            )
            assertEquals(
                conversation.head.conversationHeadSha256,
                dream.scoreSourceConversationHeadSha256,
            )
            assertEquals(
                result.scoreHeadSha256,
                dream.scoreHeadSha256,
            )
            assertEquals(
                authority.semanticAuthorityHeadSha256,
                dream.semanticAuthorityHeadSha256,
            )
            assertTrue(
                dream.activeStatementBindings
                    .isEmpty(),
            )
            assertEquals(
                1,
                dream.heldStatementBindings
                    .size,
            )
            assertEquals(
                "DISPUTED_OR_UNRESOLVED",
                dream.heldStatementBindings
                    .single()
                    .string(
                        "memoryRelation",
                    ),
            )
            assertEquals(
                "UNKNOWN",
                dream.heldStatementBindings
                    .single()
                    .string(
                        "statementState",
                    ),
            )
            assertEquals(
                semanticHashString(
                    "External owner preserved this as unresolved rather than accepting the Product proposal.",
                ),
                dream.heldStatementBindings
                    .single()
                    .string(
                        "summaryHash",
                    ),
            )

            val loop =
                VexLifeScoreContinuity.appendOpenLoop(
                    productHome = home,
                    invocation =
                        VexLifeScoreContinuity.AppendOpenLoopInvocation(
                            instanceRef =
                                "instance.vexlife-prod.score.test.loop",
                            openLoopRef =
                                "open-loop.vexlife-prod.score.test.001",
                            sourceStatementRefs =
                                listOf(
                                    "statement.vexlife-prod.score.test.001",
                                ),
                            expectedScoreHeadSha256 =
                                result.scoreHeadSha256,
                            expectedConversationHeadSha256 =
                                conversation.head.conversationHeadSha256,
                            formedAt =
                                Instant.parse(
                                    "2026-11-13T10:23:00Z",
                                ),
                        ),
                )
            assertEquals(
                listOf(
                    "open-loop.vexlife-prod.score.test.001",
                ),
                loop.openLoopRefs,
            )
            assertEquals(
                1,
                VexLifeCurrentMemoryReader
                    .readDreamSourceFrontier(
                        home,
                    )
                    .openLoopBindings
                    .size,
            )
        }
    }

    @Test
    fun currentAuthorityMembershipIsRequiredBeforeScoreMutation() {
        withHome(
            "vexlife-score-authority-held-",
        ) {
            home ->
            completedConversation(
                home,
            )
            val candidate =
                candidate(
                    home,
                )
            val authority =
                materializeDeferredAuthority(
                    home = home,
                    candidate = candidate.candidate,
                    formedAt =
                        "2026-11-13T10:24:00.000Z",
                )

            val root =
                authorityRoot(
                    home,
                    candidate.candidate,
                )
            Files.delete(
                root.resolve(
                    "head.json",
                ),
            )

            val failure =
                assertFailsWith<IllegalArgumentException> {
                    VexLifeScoreContinuity
                        .appendAcceptedStatement(
                            productHome = home,
                            invocation =
                                VexLifeScoreContinuity.AppendStatementInvocation(
                                    instanceRef =
                                        "instance.vexlife-prod.score.no-authority",
                                    statementRef =
                                        "statement.vexlife-prod.score.no-authority",
                                    semanticAcceptanceRef =
                                        authority.acceptanceRef,
                                    semanticAcceptanceSha256 =
                                        authority.acceptanceSha256,
                                    expectedScoreHeadSha256 =
                                        null,
                                    formedAt =
                                        Instant.parse(
                                            "2026-11-13T10:25:00Z",
                                        ),
                                ),
                        )
                }
            assertTrue(
                failure.message
                    .orEmpty()
                    .contains(
                        "semantic authority",
                        ignoreCase = true,
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
    fun durableUncommittedScoreTailBlocksFurtherMutation() {
        withHome(
            "vexlife-score-tail-",
        ) {
            home ->
            completedConversation(
                home,
            )
            val candidate =
                candidate(
                    home,
                )
            val authority =
                materializeDeferredAuthority(
                    home = home,
                    candidate = candidate.candidate,
                    formedAt =
                        "2026-11-13T10:26:00.000Z",
                )
            val invocation =
                VexLifeScoreContinuity.AppendStatementInvocation(
                    instanceRef =
                        "instance.vexlife-prod.score.tail",
                    statementRef =
                        "statement.vexlife-prod.score.tail.001",
                    semanticAcceptanceRef =
                        authority.acceptanceRef,
                    semanticAcceptanceSha256 =
                        authority.acceptanceSha256,
                    expectedScoreHeadSha256 =
                        null,
                    formedAt =
                        Instant.parse(
                            "2026-11-13T10:27:00Z",
                        ),
                )

            assertFailsWith<IllegalStateException> {
                VexLifeScoreContinuity
                    .appendAcceptedStatementWithFaults(
                        productHome = home,
                        invocation = invocation,
                        faults =
                            VexLifeScoreContinuity.Faults(
                                failAfterEventWrite =
                                    true,
                            ),
                    )
            }
            val coordinates =
                VexLifeCurrentMemoryReader
                    .readCurrentScoreCoordinates(
                        home,
                    )
            assertNull(
                coordinates.scoreHeadSha256,
            )

            val failure =
                assertFailsWith<IllegalArgumentException> {
                    VexLifeScoreContinuity
                        .appendAcceptedStatement(
                            productHome = home,
                            invocation =
                                invocation.copy(
                                    instanceRef =
                                        "instance.vexlife-prod.score.tail.retry",
                                    statementRef =
                                        "statement.vexlife-prod.score.tail.002",
                                    formedAt =
                                        Instant.parse(
                                            "2026-11-13T10:28:00Z",
                                        ),
                                ),
                        )
                }
            assertTrue(
                failure.message
                    .orEmpty()
                    .contains(
                        "SCORE_TAIL_ATTENTION",
                    ),
            )
        }
    }

    private fun candidate(
        home: Path,
    ): VexLifeScoreContinuity.CandidateResult =
        VexLifeScoreContinuity.formCandidate(
            productHome = home,
            input =
                VexLifeScoreContinuity.CandidateInput(
                    semanticSubjectRef =
                        "subject.victor.preference.score-test",
                    proposedSummary =
                        "Victor prefers exact source ownership.",
                    proposedMemoryRelation =
                        "SHARED_RELATIONSHIP_HISTORY",
                    proposedStatementState =
                        "INFERRED",
                    proposerRef =
                        "owner.vexlife-prod.score-candidate.test",
                    proposerClass =
                        "LINEAGE",
                    formedAt =
                        Instant.parse(
                            "2026-11-13T10:20:00Z",
                        ),
                ),
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
                "instance.vexlife-prod.score-conversation.test",
        ) {
            val request =
                beginRequest(
                    turnRef =
                        "turn.vexlife-prod.score.test",
                    requestMessageRef =
                        "message.vexlife-prod.score.human.test",
                    content =
                        "Score source request",
                    formedAt =
                        Instant.parse(
                            "2026-11-13T10:10:00Z",
                        ),
                )
            val response =
                appendResponse(
                    request = request,
                    responseMessageRef =
                        "message.vexlife-prod.score.vex.test",
                    content =
                        "Score source response",
                    model =
                        "default_model",
                    endpointOrigin =
                        "http://127.0.0.1:8765",
                    runtimeObservationRef =
                        "observation.vexlife-prod.score.runtime.test",
                    invocationEvidenceRef =
                        "evidence.vexlife-prod.score.invocation.test",
                    formedAt =
                        Instant.parse(
                            "2026-11-13T10:10:01Z",
                        ),
                )
            val context =
                CanonicalLivedContextFixture.write(
                    productHome = home,
                    request = request,
                    response = response,
                    formedAt =
                        Instant.parse(
                            "2026-11-13T10:10:02Z",
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
                            "2026-11-13T10:10:03Z",
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

    private fun materializeDeferredAuthority(
        home: Path,
        candidate: JsonObject,
        formedAt: String,
    ): AuthorityFixture {
        val sourceBinding =
            candidate
                .array(
                    "sourceBindings",
                )
                .first()
                .jsonObject
        val sourceEvidence =
            buildJsonObject {
                put(
                    "sourceRef",
                    sourceBinding.string(
                        "eventRef",
                    ),
                )
                put(
                    "sourceSha256",
                    sourceBinding.string(
                        "eventHash",
                    ),
                )
            }
        val evidence =
            contentAddressed(
                prefix =
                    "score-classification-evidence",
                refField =
                    "classificationEvidenceRef",
                hashField =
                    "classificationEvidenceSha256",
                core =
                    buildJsonObject {
                        put(
                            "schemaVersion",
                            "vextreme.score-classification-evidence/v1",
                        )
                        put(
                            "candidateRef",
                            candidate.string(
                                "candidateRef",
                            ),
                        )
                        put(
                            "candidateSha256",
                            candidate.string(
                                "candidateSha256",
                            ),
                        )
                        put(
                            "semanticSubjectRef",
                            candidate.string(
                                "semanticSubjectRef",
                            ),
                        )
                        put(
                            "semanticSubjectFingerprint",
                            candidate.string(
                                "semanticSubjectFingerprint",
                            ),
                        )
                        put(
                            "evidenceClass",
                            "UNKNOWN_HOLD",
                        )
                        put(
                            "assertedMemoryRelation",
                            "DISPUTED_OR_UNRESOLVED",
                        )
                        put(
                            "assertedStatementState",
                            "UNKNOWN",
                        )
                        put(
                            "assertedSummarySha256",
                            semanticHashString(
                                "External owner preserved this as unresolved rather than accepting the Product proposal.",
                            ),
                        )
                        put(
                            "transitionKind",
                            "NONE",
                        )
                        put(
                            "transitionTargetRef",
                            JsonNull,
                        )
                        put(
                            "transitionTargetAcceptanceSha256",
                            JsonNull,
                        )
                        put(
                            "issuerRef",
                            "owner.multivex.memory.score-semantic-classifier.001",
                        )
                        put(
                            "issuerClass",
                            "SOURCE_MANAGED_MEMORY_CLASSIFIER",
                        )
                        put(
                            "ownerProjectRef",
                            "project.multivex.memory",
                        )
                        put(
                            "ownerDispositionRef",
                            "github.issue.vextreme-sdk.225.comment.5217085830",
                        )
                        put(
                            "sourceEvidenceBindings",
                            JsonArray(
                                listOf(
                                    sourceEvidence,
                                ),
                            ),
                        )
                        put(
                            "purposeRef",
                            "purpose.score.live-semantic-acceptance",
                        )
                        put(
                            "formedAt",
                            formedAt,
                        )
                        put(
                            "privacyClass",
                            "DEVICE_PRIVATE",
                        )
                    },
            )
        val consent =
            contentAddressed(
                prefix =
                    "score-consent-disposition",
                refField =
                    "consentDispositionRef",
                hashField =
                    "consentDispositionSha256",
                core =
                    buildJsonObject {
                        put(
                            "schemaVersion",
                            "vextreme.score-consent-disposition/v1",
                        )
                        put(
                            "candidateRef",
                            candidate.string(
                                "candidateRef",
                            ),
                        )
                        put(
                            "candidateSha256",
                            candidate.string(
                                "candidateSha256",
                            ),
                        )
                        put(
                            "semanticSubjectRef",
                            candidate.string(
                                "semanticSubjectRef",
                            ),
                        )
                        put(
                            "semanticSubjectFingerprint",
                            candidate.string(
                                "semanticSubjectFingerprint",
                            ),
                        )
                        put(
                            "purposeRef",
                            "purpose.score.live-semantic-acceptance",
                        )
                        put(
                            "privacyClass",
                            "DEVICE_PRIVATE",
                        )
                        putStrings(
                            "implicatedSubjectRefs",
                            listOf(
                                candidate.string(
                                    "semanticSubjectRef",
                                ),
                            ),
                        )
                        put(
                            "requiredAuthorityBindings",
                            JsonArray(
                                emptyList(),
                            ),
                        )
                        put(
                            "observedAuthorityBindings",
                            JsonArray(
                                emptyList(),
                            ),
                        )
                        put(
                            "disposition",
                            "DEFERRED",
                        )
                        put(
                            "permittedUseRefs",
                            JsonArray(
                                emptyList(),
                            ),
                        )
                        put(
                            "prohibitedUseRefs",
                            JsonArray(
                                emptyList(),
                            ),
                        )
                        put(
                            "retentionBoundaryRef",
                            "boundary.score.retention.test",
                        )
                        put(
                            "redisclosureBoundaryRef",
                            "boundary.score.redisclosure.test",
                        )
                        put(
                            "firstPersonBoundaryRef",
                            "boundary.score.first-person.test",
                        )
                        put(
                            "formedAt",
                            formedAt,
                        )
                        put(
                            "expiresAt",
                            JsonNull,
                        )
                        put(
                            "issuerRef",
                            "owner.multivex.safety.score-consent-authority.001",
                        )
                        put(
                            "issuerClass",
                            "SOURCE_MANAGED_SAFETY_CONSENT",
                        )
                        put(
                            "ownerProjectRef",
                            "project.multivex.safety",
                        )
                        put(
                            "ownerDispositionRef",
                            "github.issue.vextreme-sdk.226.comment.5217090896",
                        )
                        put(
                            "sourceEvidenceBindings",
                            JsonArray(
                                listOf(
                                    sourceEvidence,
                                ),
                            ),
                        )
                    },
            )
        val evidenceBinding =
            buildJsonObject {
                put(
                    "classificationEvidenceRef",
                    evidence.string(
                        "classificationEvidenceRef",
                    ),
                )
                put(
                    "classificationEvidenceSha256",
                    evidence.string(
                        "classificationEvidenceSha256",
                    ),
                )
            }
        val acceptance =
            contentAddressed(
                prefix =
                    "score-semantic-acceptance",
                refField =
                    "acceptanceRef",
                hashField =
                    "acceptanceSha256",
                core =
                    buildJsonObject {
                        put(
                            "schemaVersion",
                            "vextreme.score-semantic-acceptance/v1",
                        )
                        put(
                            "contractRef",
                            "contract.multivex.score.live-semantic-acceptance/v1",
                        )
                        put(
                            "semanticContractDispositionRef",
                            "github.issue.vextreme-sdk.350.comment.5216924433",
                        )
                        put(
                            "semanticExecutableAddendumRef",
                            "github.issue.vextreme-sdk.350.comment.5217097749",
                        )
                        put(
                            "candidateRef",
                            candidate.string(
                                "candidateRef",
                            ),
                        )
                        put(
                            "candidateSha256",
                            candidate.string(
                                "candidateSha256",
                            ),
                        )
                        put(
                            "sourceLineageRef",
                            candidate.string(
                                "sourceLineageRef",
                            ),
                        )
                        put(
                            "sourceThreadRef",
                            candidate.string(
                                "sourceThreadRef",
                            ),
                        )
                        put(
                            "sourceConversationHeadSha256",
                            candidate.string(
                                "sourceConversationHeadSha256",
                            ),
                        )
                        putStrings(
                            "sourceBindingHashes",
                            candidate.objectList(
                                "sourceBindings",
                            )
                                .map {
                                    it.string(
                                        "eventHash",
                                    )
                                },
                        )
                        put(
                            "semanticSubjectRef",
                            candidate.string(
                                "semanticSubjectRef",
                            ),
                        )
                        put(
                            "semanticSubjectFingerprint",
                            candidate.string(
                                "semanticSubjectFingerprint",
                            ),
                        )
                        put(
                            "acceptedSummary",
                            "External owner preserved this as unresolved rather than accepting the Product proposal.",
                        )
                        put(
                            "acceptedSummarySha256",
                            semanticHashString(
                                "External owner preserved this as unresolved rather than accepting the Product proposal.",
                            ),
                        )
                        put(
                            "memoryRelation",
                            "DISPUTED_OR_UNRESOLVED",
                        )
                        put(
                            "statementState",
                            "UNKNOWN",
                        )
                        put(
                            "acceptedForContinuity",
                            false,
                        )
                        put(
                            "transitionKind",
                            "NONE",
                        )
                        put(
                            "transitionTargetRef",
                            JsonNull,
                        )
                        put(
                            "transitionTargetAcceptanceSha256",
                            JsonNull,
                        )
                        put(
                            "classificationEvidenceBindings",
                            JsonArray(
                                listOf(
                                    evidenceBinding,
                                ),
                            ),
                        )
                        put(
                            "consentDispositionRef",
                            consent.string(
                                "consentDispositionRef",
                            ),
                        )
                        put(
                            "consentDispositionSha256",
                            consent.string(
                                "consentDispositionSha256",
                            ),
                        )
                        put(
                            "issuerRef",
                            "owner.multivex.score-semantic-authority.materializer.001",
                        )
                        put(
                            "issuerClass",
                            "SOURCE_MANAGED_MEMORY_SAFETY_COMPOSITION",
                        )
                        put(
                            "ownerDispositionRef",
                            "github.issue.vextreme-sdk.350.comment.5217097749",
                        )
                        put(
                            "formedAt",
                            formedAt,
                        )
                        put(
                            "privacyClass",
                            "DEVICE_PRIVATE",
                        )
                    },
            )
        val binding =
            buildJsonObject {
                put(
                    "semanticSubjectFingerprint",
                    candidate.string(
                        "semanticSubjectFingerprint",
                    ),
                )
                put(
                    "acceptanceRef",
                    acceptance.string(
                        "acceptanceRef",
                    ),
                )
                put(
                    "acceptanceSha256",
                    acceptance.string(
                        "acceptanceSha256",
                    ),
                )
                put(
                    "candidateRef",
                    candidate.string(
                        "candidateRef",
                    ),
                )
                put(
                    "candidateSha256",
                    candidate.string(
                        "candidateSha256",
                    ),
                )
                put(
                    "classificationEvidenceBindings",
                    JsonArray(
                        listOf(
                            evidenceBinding,
                        ),
                    ),
                )
                put(
                    "consentDispositionRef",
                    consent.string(
                        "consentDispositionRef",
                    ),
                )
                put(
                    "consentDispositionSha256",
                    consent.string(
                        "consentDispositionSha256",
                    ),
                )
            }
        val head =
            contentAddressed(
                prefix =
                    "score-semantic-authority-head",
                refField =
                    "semanticAuthorityHeadRef",
                hashField =
                    "semanticAuthorityHeadSha256",
                core =
                    buildJsonObject {
                        put(
                            "schemaVersion",
                            "vextreme.score-semantic-authority-head/v1",
                        )
                        put(
                            "sourceLineageRef",
                            candidate.string(
                                "sourceLineageRef",
                            ),
                        )
                        put(
                            "sourceThreadRef",
                            candidate.string(
                                "sourceThreadRef",
                            ),
                        )
                        put(
                            "sequence",
                            0,
                        )
                        put(
                            "priorSemanticAuthorityHeadSha256",
                            JsonNull,
                        )
                        put(
                            "currentAcceptanceBindings",
                            JsonArray(
                                listOf(
                                    binding,
                                ),
                            ),
                        )
                        put(
                            "formedAt",
                            formedAt,
                        )
                        put(
                            "contractRef",
                            "contract.multivex.score.live-semantic-acceptance/v1",
                        )
                        put(
                            "semanticExecutableAddendumRef",
                            "github.issue.vextreme-sdk.350.comment.5217097749",
                        )
                        putStrings(
                            "ownerRefs",
                            listOf(
                                "project.multivex.memory",
                                "project.multivex.safety",
                            ),
                        )
                    },
            )

        val root =
            authorityRoot(
                home,
                candidate,
            )
        writeCanonical(
            home,
            root.resolve(
                "candidates/" +
                    candidate.string(
                        "candidateSha256",
                    ) +
                    ".json",
            ),
            candidate,
        )
        writeCanonical(
            home,
            root.resolve(
                "classification-evidence/" +
                    evidence.string(
                        "classificationEvidenceSha256",
                    ) +
                    ".json",
            ),
            evidence,
        )
        writeCanonical(
            home,
            root.resolve(
                "consents/" +
                    consent.string(
                        "consentDispositionSha256",
                    ) +
                    ".json",
            ),
            consent,
        )
        writeCanonical(
            home,
            root.resolve(
                "acceptances/" +
                    acceptance.string(
                        "acceptanceSha256",
                    ) +
                    ".json",
            ),
            acceptance,
        )
        writeCanonical(
            home,
            root.resolve(
                "heads/" +
                    head.string(
                        "semanticAuthorityHeadSha256",
                    ) +
                    ".json",
            ),
            head,
        )
        VexDurableContractFile.write(
            ownedRoot = home,
            path =
                root.resolve(
                    "head.json",
                ),
            value = head,
        )

        return AuthorityFixture(
            acceptanceRef =
                acceptance.string(
                    "acceptanceRef",
                ),
            acceptanceSha256 =
                acceptance.string(
                    "acceptanceSha256",
                ),
            semanticAuthorityHeadSha256 =
                head.string(
                    "semanticAuthorityHeadSha256",
                ),
        )
    }

    private fun authorityRoot(
        home: Path,
        candidate: JsonObject,
    ): Path =
        VexOwnedPath.resolveInside(
            home,
            "semantic-authority/score/" +
                candidate.string(
                    "sourceLineageRef",
                ) +
                "/" +
                candidate.string(
                    "sourceThreadRef",
                ),
        )

    private fun contentAddressed(
        prefix: String,
        refField: String,
        hashField: String,
        core: JsonObject,
    ): JsonObject {
        val ref =
            prefix +
                "." +
                semanticHash(
                    core,
                ).take(
                    32,
                )
        val withRef =
            core.extend {
                put(
                    refField,
                    ref,
                )
            }
        return withRef.extend {
            put(
                hashField,
                semanticHash(
                    withRef,
                ),
            )
        }
    }

    private fun writeCanonical(
        home: Path,
        path: Path,
        value: JsonObject,
    ) {
        VexOwnedPath.ensureParent(
            home,
            path,
        )
        Files.writeString(
            path,
            VexContractJson.canonicalEncodeElement(
                value,
            ) +
                "\n",
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE_NEW,
            StandardOpenOption.WRITE,
        )
        VexDurableContractFile.secureFile(
            path,
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

    private fun semanticHashString(
        value: String,
    ): String =
        sha256(
            VexContractJson.canonicalEncode(
                value,
            ),
        )

    private fun semanticHash(
        value: JsonObject,
    ): String =
        sha256(
            VexContractJson.canonicalEncodeElement(
                value,
            ),
        )

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

    private fun JsonObject.extend(
        block: JsonObjectBuilder.() -> Unit,
    ): JsonObject =
        buildJsonObject {
            this@extend.forEach {
                key,
                value,
                ->
                put(
                    key,
                    value,
                )
            }
            block()
        }

    private fun JsonObject.string(
        field: String,
    ): String =
        requireNotNull(
            this[
                field
            ],
        )
            .jsonPrimitive
            .content

    private fun JsonObject.array(
        field: String,
    ): JsonArray =
        requireNotNull(
            this[
                field
            ],
        )
            .jsonArray

    private fun JsonObject.objectList(
        field: String,
    ): List<JsonObject> =
        array(
            field,
        )
            .map {
                it.jsonObject
            }

    private fun JsonObjectBuilder.putStrings(
        key: String,
        values: List<String>,
    ) {
        put(
            key,
            JsonArray(
                values.map(
                    ::JsonPrimitive,
                ),
            ),
        )
    }
}

// [VXG RealForever]
