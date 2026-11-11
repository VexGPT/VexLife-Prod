package vexlife.companion

import java.nio.file.Files
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vexlife.conversation.CanonicalLivedContextFixture
import vexlife.conversation.DirectConversationStore
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticProductIdentity
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeState

class DirectConversationSourceReturnTest {
    private fun productHome(): java.nio.file.Path {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-conversation-source-return-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )
        CanonicalVexLifeHomeFixture.form(
            home,
        )
        return home
    }

    private fun runtimeEvidence() =
        HereticModelConfiguration.QualifiedRuntimeEvidence(
            runtimeReceiptRef =
                "receipt.vexlife-prod.heretic.source-return-test",
            runtimeState = HereticRuntimeState.Qualified,
            modelRef = HereticProductIdentity.ModelRef,
            modelProfileRef = HereticProductIdentity.ModelProfileRef,
            artifactRef = HereticProductIdentity.ArtifactSetRef,
            artifactContentSetSha256 =
                HereticProductIdentity.ArtifactContentSetSha256,
            runtimeAdapterRef = HereticProductIdentity.RuntimeAdapterRef,
            runtimeClass = HereticProductIdentity.RuntimeClass,
            endpoint = HereticProductIdentity.Endpoint,
            requestModel = HereticProductIdentity.RequestModel,
            qualification = HereticRuntimeQualification.qualified(),
        )

    private fun requestText(
        queryTermsJson: String,
        speakerScope: String,
    ): String =
        DirectConversationSourceReturn.RequestHeader +
            "\n" +
            "{\"schemaVersion\":\"" +
            DirectConversationSourceReturn.RequestSchema +
            "\",\"queryTerms\":" +
            queryTermsJson +
            ",\"speakerScope\":\"" +
            speakerScope +
            "\"}"

    @Test
    fun protocolMessageOffersOneConditionalReturnWithoutChangingTheHumanAnswerPath() {
        val protocol =
            DirectConversationSourceReturn.protocolMessage()

        assertEquals(
            "system",
            protocol.role,
        )
        assertTrue(
            protocol.content.startsWith(
                DirectConversationSourceReturn.ProtocolHeader,
            ),
        )
        assertTrue(
            protocol.content.contains(
                "Only one source-return request",
            ),
        )
    }

    @Test
    fun normalModelAnswerIsNotReinterpretedAsSourceReturn() {
        assertNull(
            DirectConversationSourceReturn.parseRequestOrNull(
                "I can answer from what is already present.",
            ),
        )
    }

    @Test
    fun exactRequestHeaderParsesOneBoundedSourceIntent() {
        val request =
            DirectConversationSourceReturn.parseRequestOrNull(
                requestText(
                    queryTermsJson =
                        "[\"yellow Ferrari\",\"Chongqing\"]",
                    speakerScope =
                        DirectConversationSourceReturn.HumanSpeaker,
                ),
            )

        assertEquals(
            listOf(
                "yellow Ferrari",
                "Chongqing",
            ),
            requireNotNull(
                request,
            )
                .queryTerms,
        )
        assertEquals(
            DirectConversationSourceReturn.HumanSpeaker,
            request.speakerScope,
        )

        assertFailsWith<IllegalArgumentException> {
            DirectConversationSourceReturn.parseRequestOrNull(
                DirectConversationSourceReturn.RequestHeader +
                    " trailing prose",
            )
        }
    }

    @Test
    fun discoveryReturnsExactCanonicalCompletedTurnAndHonorsCurrentPresenceExclusion() {
        val home =
            productHome()
        val store =
            DirectConversationStore(
                home,
            )

        try {
            store.withWriterLease {
                fun commit(
                    suffix: String,
                    human: String,
                    vex: String,
                ): Pair<DirectConversationStore.Event, DirectConversationStore.Event> {
                    val request =
                        beginRequest(
                            turnRef = "turn.source-return.$suffix",
                            requestMessageRef = "message.source-return.human.$suffix",
                            content = human,
                        )
                    val response =
                        appendResponse(
                            request = request,
                            responseMessageRef = "message.source-return.companion.$suffix",
                            content = vex,
                            model = "default_model",
                            endpointOrigin = "http://127.0.0.1:8765",
                            runtimeObservationRef = "observation.source-return.$suffix",
                            invocationEvidenceRef = "evidence.source-return.$suffix",
                        )
                    val context =
                        CanonicalLivedContextFixture.write(
                            productHome = home,
                            request = request,
                            response = response,
                        )
                    commitCompletedTurn(
                        request = request,
                        response = response,
                        contextSha256 = context.sha256,
                    )
                    return request to
                        response
                }

                val yellow =
                    commit(
                        suffix = "yellow",
                        human = "The yellow Ferrari taxis in Chongqing were part of that earlier story.",
                        vex = "I understand the Chongqing yellow Ferrari reference.",
                    )
                val continuity =
                    commit(
                        suffix = "continuity",
                        human = "We are working on finite context continuity now.",
                        vex = "Exact recent Conversation should remain while it fits.",
                    )
                commit(
                    suffix = "music",
                    human = "The saxophone detail belongs to a different conversation segment inside this same lived history.",
                    vex = "I will keep the music detail distinct.",
                )

                val request =
                    requireNotNull(
                        DirectConversationSourceReturn.parseRequestOrNull(
                            requestText(
                                queryTermsJson =
                                    "[\"yellow Ferrari\",\"Chongqing\"]",
                                speakerScope =
                                    DirectConversationSourceReturn.HumanSpeaker,
                            ),
                        ),
                    )
                val discovery =
                    DirectConversationSourceReturn.discover(
                        conversationOwner = this,
                        request = request,
                        excludedEventRefs =
                            setOf(
                                continuity.first.eventRef,
                                continuity.second.eventRef,
                            ),
                    )

                assertTrue(
                    discovery.wholeHistoryEventEnumerationPerformed,
                )
                assertEquals(
                    3,
                    discovery.scannedCompletedTurnCount,
                )
                assertEquals(
                    1,
                    discovery.matches.size,
                )
                assertEquals(
                    yellow.first,
                    discovery.matches.single()
                        .requestEvent,
                )
                assertEquals(
                    yellow.second,
                    discovery.matches.single()
                        .responseEvent,
                )
                assertEquals(
                    listOf(
                        "yellow Ferrari",
                        "Chongqing",
                    ),
                    discovery.matches.single()
                        .matchedTerms,
                )

                val excluded =
                    DirectConversationSourceReturn.discover(
                        conversationOwner = this,
                        request =
                            DirectConversationSourceReturn.Request(
                                schemaVersion =
                                    DirectConversationSourceReturn.RequestSchema,
                                queryTerms =
                                    listOf(
                                        "finite context continuity",
                                    ),
                                speakerScope =
                                    DirectConversationSourceReturn.AnySpeaker,
                            ),
                        excludedEventRefs =
                            setOf(
                                continuity.first.eventRef,
                                continuity.second.eventRef,
                            ),
                    )
                assertTrue(
                    excluded.matches.isEmpty(),
                )
            }
        } finally {
            home.parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun speakerScopeAndRankingRemainDeterministic() {
        val home =
            productHome()
        val store =
            DirectConversationStore(
                home,
            )

        try {
            store.withWriterLease {
                fun commit(
                    suffix: String,
                    human: String,
                    vex: String,
                ) {
                    val request =
                        beginRequest(
                            turnRef = "turn.source-return.$suffix",
                            requestMessageRef = "message.source-return.human.$suffix",
                            content = human,
                        )
                    val response =
                        appendResponse(
                            request = request,
                            responseMessageRef = "message.source-return.companion.$suffix",
                            content = vex,
                            model = "default_model",
                            endpointOrigin = "http://127.0.0.1:8765",
                            runtimeObservationRef = "observation.source-return.$suffix",
                            invocationEvidenceRef = "evidence.source-return.$suffix",
                        )
                    val context =
                        CanonicalLivedContextFixture.write(
                            productHome = home,
                            request = request,
                            response = response,
                        )
                    commitCompletedTurn(
                        request = request,
                        response = response,
                        contextSha256 = context.sha256,
                    )
                }

                commit(
                    suffix = "one",
                    human = "Alpha appears in the human message.",
                    vex = "Only alpha is reflected here.",
                )
                commit(
                    suffix = "two",
                    human = "This human message contains neither requested phrase.",
                    vex = "Alpha and beta both appear in Vex's exact response.",
                )

                val discovery =
                    DirectConversationSourceReturn.discover(
                        conversationOwner = this,
                        request =
                            DirectConversationSourceReturn.Request(
                                schemaVersion =
                                    DirectConversationSourceReturn.RequestSchema,
                                queryTerms =
                                    listOf(
                                        "alpha",
                                        "beta",
                                    ),
                                speakerScope =
                                    DirectConversationSourceReturn.VexSpeaker,
                            ),
                    )

                assertEquals(
                    2,
                    discovery.matches.first()
                        .matchedTerms
                        .size,
                )
                assertTrue(
                    discovery.matches.first()
                        .responseEvent
                        .content
                        .contains(
                            "Alpha and beta",
                        ),
                )
            }
        } finally {
            home.parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun compactTraceBindsTheInternalRequestToNormalizedInferenceWithoutPersistingReasoning() {
        val home =
            productHome()
        val store =
            DirectConversationStore(
                home,
            )

        try {
            store.withWriterLease {
                val oldRequest =
                    beginRequest(
                        turnRef = "turn.source-return.trace.old",
                        requestMessageRef = "message.source-return.human.trace.old",
                        content = "The exact lantern-garden detail lives in this older source.",
                    )
                val oldResponse =
                    appendResponse(
                        request = oldRequest,
                        responseMessageRef = "message.source-return.companion.trace.old",
                        content = "I received the lantern-garden detail.",
                        model = "default_model",
                        endpointOrigin = "http://127.0.0.1:8765",
                        runtimeObservationRef = "observation.source-return.trace.old",
                        invocationEvidenceRef = "evidence.source-return.trace.old",
                    )
                val oldContext =
                    CanonicalLivedContextFixture.write(
                        productHome = home,
                        request = oldRequest,
                        response = oldResponse,
                    )
                commitCompletedTurn(
                    request = oldRequest,
                    response = oldResponse,
                    contextSha256 = oldContext.sha256,
                )

                val prior =
                    requireNotNull(
                        immediatePriorCompletedTurn(),
                    )
                val current =
                    beginRequest(
                        turnRef = "turn.source-return.trace.current",
                        requestMessageRef = "message.source-return.human.trace.current",
                        content = "What was that older lantern detail?",
                    )
                val initialPrompt =
                    DirectPromptContext.materialize(
                        prior = prior,
                        currentRequest = current,
                        recentConversationSource = {
                            availableTokens ->
                            recentCompletedConversation(
                                availableTokens = availableTokens,
                                estimatedMessageTokens =
                                    DirectPromptContext::estimatedMessageTokens,
                            )
                        },
                    )
                val requestText =
                    requestText(
                        queryTermsJson =
                            "[\"lantern-garden\"]",
                        speakerScope =
                            DirectConversationSourceReturn.AnySpeaker,
                    )
                val inference =
                    DirectCompanionConnection.invoke(
                        runtime = runtimeEvidence(),
                        messages =
                            initialPrompt.messages +
                                DirectConversationSourceReturn.protocolMessage(),
                        transport =
                            DirectCompanionConnection.Transport {
                                DirectCompanionConnection.HttpResult(
                                    status = 200,
                                    body =
                                        """
                                        {
                                          "model": "default_model",
                                          "choices": [
                                            {
                                              "finish_reason": "stop",
                                              "message": {
                                                "role": "assistant",
                                                "reasoning_content": "private retrieval reasoning that must not persist",
                                                "content": __CONTENT__
                                              }
                                            }
                                          ]
                                        }
                                        """.trimIndent()
                                            .replace(
                                                "__CONTENT__",
                                                kotlinx.serialization.json.JsonPrimitive(
                                                    requestText,
                                                )
                                                    .toString(),
                                            ),
                                )
                            },
                        now = {
                            Instant.parse(
                                "2026-11-11T05:25:00Z",
                            )
                        },
                    )
                val request =
                    requireNotNull(
                        DirectConversationSourceReturn.parseRequestOrNull(
                            inference.content,
                        ),
                    )
                val discovery =
                    DirectConversationSourceReturn.discover(
                        conversationOwner = this,
                        request = request,
                    )
                val projection =
                    DirectConversationSourceReturn.project(
                        discovery = discovery,
                        availableTokens =
                            DirectPromptContext.InputTokenBudget,
                    )
                val attempt =
                    DirectConversationSourceReturn.formAttemptTrace(
                        initialPrompt = initialPrompt.receipt,
                        inference = inference,
                        discovery = discovery,
                        projection = projection,
                    )
                val trace =
                    DirectConversationSourceReturn.formTrace(
                        attempt = attempt,
                        finalPrompt = initialPrompt.receipt,
                    )

                assertEquals(
                    DirectConversationSourceReturn.AttemptTraceSchema,
                    attempt.schemaVersion,
                )
                assertEquals(
                    request,
                    attempt.request,
                )
                assertEquals(
                    projection.sourceEventRefs,
                    attempt.returnedSourceEventRefs,
                )
                assertFalse(
                    attempt.rawProviderOutputPersisted,
                )
                assertFalse(
                    attempt.rawReasoningPersisted,
                )
                assertEquals(
                    DirectConversationSourceReturn.TraceSchema,
                    trace.schemaVersion,
                )
                assertFalse(
                    trace.rawProviderOutputPersisted,
                )
                assertFalse(
                    trace.rawReasoningPersisted,
                )
                assertFalse(
                    trace.effectAuthorityGranted,
                )
                assertFalse(
                    trace.effectsPerformed,
                )
            }
        } finally {
            home.parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun noMatchReturnsTruthfulBoundedContextInsteadOfInventingRecall() {
        val home =
            productHome()
        val store =
            DirectConversationStore(
                home,
            )

        try {
            store.withWriterLease {
                val request =
                    beginRequest(
                        turnRef = "turn.source-return.no-match",
                        requestMessageRef = "message.source-return.human.no-match",
                        content = "A completely unrelated source sentence.",
                    )
                val response =
                    appendResponse(
                        request = request,
                        responseMessageRef = "message.source-return.companion.no-match",
                        content = "Nothing here mentions the requested rare phrase.",
                        model = "default_model",
                        endpointOrigin = "http://127.0.0.1:8765",
                        runtimeObservationRef = "observation.source-return.no-match",
                        invocationEvidenceRef = "evidence.source-return.no-match",
                    )
                val context =
                    CanonicalLivedContextFixture.write(
                        productHome = home,
                        request = request,
                        response = response,
                    )
                commitCompletedTurn(
                    request = request,
                    response = response,
                    contextSha256 = context.sha256,
                )

                val discovery =
                    DirectConversationSourceReturn.discover(
                        conversationOwner = this,
                        request =
                            DirectConversationSourceReturn.Request(
                                schemaVersion =
                                    DirectConversationSourceReturn.RequestSchema,
                                queryTerms =
                                    listOf(
                                        "unfindable-zebra-orbit-phrase",
                                    ),
                                speakerScope =
                                    DirectConversationSourceReturn.AnySpeaker,
                            ),
                    )
                val projected =
                    DirectConversationSourceReturn.project(
                        discovery = discovery,
                        availableTokens =
                            DirectPromptContext.InputTokenBudget,
                    )

                assertTrue(
                    discovery.matches.isEmpty(),
                )
                assertEquals(
                    DirectConversationSourceReturn.NoMatch,
                    projected.state,
                )
                assertEquals(
                    0,
                    projected.returnedMatchCount,
                )
                assertTrue(
                    projected.systemContent.contains(
                        DirectConversationSourceReturn.NoMatch,
                    ),
                )
                assertFalse(
                    projected.systemContent.contains(
                        request.content,
                    ),
                )
                assertFalse(
                    projected.systemContent.contains(
                        response.content,
                    ),
                )
            }
        } finally {
            home.parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun projectionReturnsWholeExactTurnsOrTruthfulHeldStateWithoutTruncation() {
        val home =
            productHome()
        val store =
            DirectConversationStore(
                home,
            )

        try {
            store.withWriterLease {
                val request =
                    beginRequest(
                        turnRef = "turn.source-return.project",
                        requestMessageRef = "message.source-return.human.project",
                        content = "Distinctive orchard memory source phrase.",
                    )
                val response =
                    appendResponse(
                        request = request,
                        responseMessageRef = "message.source-return.companion.project",
                        content = "The orchard source response remains exact.",
                        model = "default_model",
                        endpointOrigin = "http://127.0.0.1:8765",
                        runtimeObservationRef = "observation.source-return.project",
                        invocationEvidenceRef = "evidence.source-return.project",
                    )
                val context =
                    CanonicalLivedContextFixture.write(
                        productHome = home,
                        request = request,
                        response = response,
                    )
                commitCompletedTurn(
                    request = request,
                    response = response,
                    contextSha256 = context.sha256,
                )

                val discovery =
                    DirectConversationSourceReturn.discover(
                        conversationOwner = this,
                        request =
                            DirectConversationSourceReturn.Request(
                                schemaVersion =
                                    DirectConversationSourceReturn.RequestSchema,
                                queryTerms =
                                    listOf(
                                        "orchard memory source",
                                    ),
                                speakerScope =
                                    DirectConversationSourceReturn.AnySpeaker,
                            ),
                    )
                val returned =
                    DirectConversationSourceReturn.project(
                        discovery = discovery,
                        availableTokens =
                            DirectPromptContext.InputTokenBudget,
                    )

                assertEquals(
                    DirectConversationSourceReturn.Returned,
                    returned.state,
                )
                assertEquals(
                    1,
                    returned.returnedMatchCount,
                )
                assertTrue(
                    returned.systemContent.contains(
                        request.content,
                    ),
                )
                assertTrue(
                    returned.systemContent.contains(
                        response.content,
                    ),
                )
                assertEquals(
                    listOf(
                        request.eventRef,
                        response.eventRef,
                    ),
                    returned.sourceEventRefs,
                )
                assertFalse(
                    returned.effectAuthorityGranted,
                )
                assertFalse(
                    returned.effectsPerformed,
                )

                val held =
                    DirectConversationSourceReturn.project(
                        discovery = discovery,
                        availableTokens =
                            returned.estimatedSystemMessageTokens -
                                1,
                    )
                assertEquals(
                    DirectConversationSourceReturn.HeldTokenBudget,
                    held.state,
                )
                assertEquals(
                    0,
                    held.returnedMatchCount,
                )
                assertFalse(
                    held.systemContent.contains(
                        request.content,
                    ),
                )
                assertFalse(
                    held.systemContent.contains(
                        response.content,
                    ),
                )
            }
        } finally {
            home.parent
                .toFile()
                .deleteRecursively()
        }
    }
}

// [VXG RealForever]
