package vexlife.companion

import java.nio.file.Files
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import vexlife.conversation.CanonicalLivedContextFixture
import vexlife.conversation.DirectConversationStore
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticProductIdentity
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeState

class DirectTurnContextStoreTest {
    private fun home(
        productHome: java.nio.file.Path,
    ) {
        CanonicalVexLifeHomeFixture.form(
            home = productHome,
            deviceSuffix = "context-device",
            lineageSuffix = "context-lineage",
        )
    }

    private fun runtime(): HereticModelConfiguration.QualifiedRuntimeEvidence =
        HereticModelConfiguration.QualifiedRuntimeEvidence(
            runtimeReceiptRef = "receipt.vexlife-prod.heretic.context",
            runtimeState = HereticRuntimeState.Qualified,
            modelRef = HereticProductIdentity.ModelRef,
            modelProfileRef = HereticProductIdentity.ModelProfileRef,
            artifactRef = HereticProductIdentity.ArtifactSetRef,
            artifactContentSetSha256 = HereticProductIdentity.ArtifactContentSetSha256,
            runtimeAdapterRef = HereticProductIdentity.RuntimeAdapterRef,
            runtimeClass = HereticProductIdentity.RuntimeClass,
            endpoint = HereticProductIdentity.Endpoint,
            requestModel = HereticProductIdentity.RequestModel,
            qualification = HereticRuntimeQualification.qualified(),
        )

    @Test
    fun sourceReturnTraceRoundTripsTwoProviderStagesWithoutPersistingPrivateReasoning() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-direct-context-source-return-",
            )
                .toAbsolutePath()
        val productHome =
            parent.resolve(
                "VexLife-Prod",
            )

        try {
            home(
                productHome,
            )
            val conversation =
                DirectConversationStore(
                    productHome,
                )
            val context =
                DirectTurnContextStore(
                    productHome,
                )

            conversation.withWriterLease {
                val oldRequest =
                    beginRequest(
                        turnRef =
                            "turn.self-development.context-source-return.old",
                        requestMessageRef =
                            "message.self-development.human.context-source-return.old",
                        content =
                            "lantern-garden exact older source " +
                                "O".repeat(
                                    40_000,
                                ),
                    )
                val oldResponse =
                    appendResponse(
                        request = oldRequest,
                        responseMessageRef =
                            "message.self-development.companion.context-source-return.old",
                        content =
                            "I received the lantern-garden detail.",
                        model = "default_model",
                        endpointOrigin = "http://127.0.0.1:8765",
                        runtimeObservationRef =
                            "observation.context-source-return.old",
                        invocationEvidenceRef =
                            "evidence.context-source-return.old",
                    )
                val oldContext =
                    CanonicalLivedContextFixture.write(
                        productHome = productHome,
                        request = oldRequest,
                        response = oldResponse,
                    )
                commitCompletedTurn(
                    request = oldRequest,
                    response = oldResponse,
                    contextSha256 = oldContext.sha256,
                )

                val newerRequestTokens =
                    187_000
                val newerRequest =
                    beginRequest(
                        turnRef =
                            "turn.self-development.context-source-return.newer",
                        requestMessageRef =
                            "message.self-development.human.context-source-return.newer",
                        content =
                            "N".repeat(
                                (
                                    newerRequestTokens -
                                        4
                                ) *
                                    4,
                            ),
                    )
                val newerResponse =
                    appendResponse(
                        request = newerRequest,
                        responseMessageRef =
                            "message.self-development.companion.context-source-return.newer",
                        content =
                            "Recent response that does not contain the older query term.",
                        model = "default_model",
                        endpointOrigin = "http://127.0.0.1:8765",
                        runtimeObservationRef =
                            "observation.context-source-return.newer",
                        invocationEvidenceRef =
                            "evidence.context-source-return.newer",
                    )
                val newerContext =
                    CanonicalLivedContextFixture.write(
                        productHome = productHome,
                        request = newerRequest,
                        response = newerResponse,
                    )
                commitCompletedTurn(
                    request = newerRequest,
                    response = newerResponse,
                    contextSha256 = newerContext.sha256,
                )

                val prior =
                    requireNotNull(
                        immediatePriorCompletedTurn(),
                    )
                val request =
                    beginRequest(
                        turnRef =
                            "turn.self-development.context-source-return.current",
                        requestMessageRef =
                            "message.self-development.human.context-source-return.current",
                        content =
                            "What was the exact older lantern-garden detail?",
                    )
                val initialPrompt =
                    DirectPromptContext.materializeContinuity(
                        conversationOwner = this,
                        prior = prior,
                        currentRequest = request,
                        sourceReturnProtocolEnabled = true,
                    )

                assertTrue(
                    initialPrompt.receipt.sourceReturnProtocolEnabled,
                )
                assertTrue(
                    initialPrompt.receipt.selectedConversationEventRefs.contains(
                        newerRequest.eventRef,
                    ),
                )
                assertFalse(
                    initialPrompt.receipt.selectedConversationEventRefs.contains(
                        oldRequest.eventRef,
                    ),
                )

                val sourceReturnRequest =
                    "VEXLIFE_CONVERSATION_SOURCE_RETURN_REQUEST/v1" +
                        "\n" +
                        """{"schemaVersion":"vexlife-prod.conversation-source-return-request/v1","queryTerms":["lantern-garden"],"speakerScope":"ANY"}"""
                val initialInference =
                    DirectCompanionConnection.invoke(
                        runtime = runtime(),
                        messages = initialPrompt.messages,
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
                                              "message": {
                                                "role": "assistant",
                                                "reasoning_content": "PRIVATE_SOURCE_RETURN_REASONING_MUST_NOT_PERSIST",
                                                "content": __CONTENT__
                                              }
                                            }
                                          ]
                                        }
                                        """.trimIndent()
                                            .replace(
                                                "__CONTENT__",
                                                kotlinx.serialization.json.JsonPrimitive(
                                                    sourceReturnRequest,
                                                )
                                                    .toString(),
                                            ),
                                )
                            },
                        now = {
                            Instant.parse(
                                "2026-11-11T05:55:00Z",
                            )
                        },
                    )
                val sourceRequest =
                    requireNotNull(
                        DirectConversationSourceReturn.parseRequestOrNull(
                            initialInference.content,
                        ),
                    )
                val discovery =
                    DirectConversationSourceReturn.discover(
                        conversationOwner = this,
                        request = sourceRequest,
                        excludedEventRefs =
                            initialPrompt.receipt.selectedConversationEventRefs.toSet(),
                    )
                val projection =
                    DirectConversationSourceReturn.project(
                        discovery = discovery,
                        availableTokens =
                            DirectPromptContext.InputTokenBudget,
                    )
                assertEquals(
                    DirectConversationSourceReturn.Returned,
                    projection.state,
                )
                assertEquals(
                    listOf(
                        oldRequest.eventRef,
                        oldResponse.eventRef,
                    ),
                    projection.sourceEventRefs,
                )

                val attempt =
                    DirectConversationSourceReturn.formAttemptTrace(
                        initialPrompt = initialPrompt.receipt,
                        inference = initialInference,
                        discovery = discovery,
                        projection = projection,
                    )
                val finalPrompt =
                    DirectPromptContext.materializeContinuity(
                        conversationOwner = this,
                        prior = prior,
                        currentRequest = request,
                        sourceReturnDiscovery = discovery,
                    )
                val trace =
                    DirectConversationSourceReturn.formTrace(
                        attempt = attempt,
                        finalPrompt = finalPrompt.receipt,
                    )
                val finalInference =
                    DirectCompanionConnection.invoke(
                        runtime = runtime(),
                        messages = finalPrompt.messages,
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
                                              "message": {
                                                "role": "assistant",
                                                "reasoning_content": "PRIVATE_FINAL_REASONING_MUST_NOT_PERSIST",
                                                "content": "The exact older lantern-garden source has been returned."
                                              }
                                            }
                                          ]
                                        }
                                        """.trimIndent(),
                                )
                            },
                        now = {
                            Instant.parse(
                                "2026-11-11T05:55:01Z",
                            )
                        },
                    )
                val response =
                    appendResponse(
                        request = request,
                        responseMessageRef =
                            "message.self-development.companion.context-source-return.current",
                        content = finalInference.content,
                        model = finalInference.model,
                        endpointOrigin = finalInference.invocationEvidence.endpointOrigin,
                        runtimeObservationRef =
                            finalInference.runtimeObservation.observationRef,
                        invocationEvidenceRef =
                            finalInference.invocationEvidence.invocationRef,
                    )
                val record =
                    context.record(
                        request = request,
                        response = response,
                        promptContext = finalPrompt.receipt,
                        runtimeObservation = finalInference.runtimeObservation,
                        invocationEvidence = finalInference.invocationEvidence,
                        sourceReturnTrace = trace,
                        formedAt =
                            Instant.parse(
                                "2026-11-11T05:55:02Z",
                            ),
                    )
                commitCompletedTurn(
                    request = request,
                    response = response,
                    contextSha256 = record.contextSha256,
                )

                val reread =
                    assertNotNull(
                        context.read(
                            request.turnRef,
                        ),
                    )
                val rereadTrace =
                    assertNotNull(
                        reread.sourceReturnTrace,
                    )
                assertEquals(
                    trace,
                    rereadTrace,
                )
                assertEquals(
                    initialPrompt.receipt.receiptRef,
                    rereadTrace.attempt.initialPromptReceiptRef,
                )
                assertEquals(
                    finalPrompt.receipt.receiptRef,
                    rereadTrace.finalPromptReceiptRef,
                )
                assertEquals(
                    initialInference.runtimeObservation.observationRef,
                    rereadTrace.attempt.runtimeObservationRef,
                )
                assertEquals(
                    finalInference.runtimeObservation.observationRef,
                    reread.runtimeObservation.observationRef,
                )
                assertFalse(
                    reread.rawProviderResponsePersisted,
                )
                assertFalse(
                    reread.rawReasoningPersisted,
                )
                assertFalse(
                    rereadTrace.rawProviderOutputPersisted,
                )
                assertFalse(
                    rereadTrace.rawReasoningPersisted,
                )

                val durableEvidence =
                    Files.readString(
                        productHome
                            .resolve(
                                "runtime",
                            )
                            .resolve(
                                "vexlife-prod",
                            )
                            .resolve(
                                "turn-evidence",
                            )
                            .resolve(
                                request.turnRef +
                                    ".json",
                            ),
                    )
                assertTrue(
                    durableEvidence.contains(
                        DirectConversationSourceReturn.TraceSchema,
                    ),
                )
                assertTrue(
                    durableEvidence.contains(
                        "lantern-garden",
                    ),
                )
                assertFalse(
                    durableEvidence.contains(
                        "PRIVATE_SOURCE_RETURN_REASONING_MUST_NOT_PERSIST",
                    ),
                )
                assertFalse(
                    durableEvidence.contains(
                        "PRIVATE_FINAL_REASONING_MUST_NOT_PERSIST",
                    ),
                )
            }
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun normalizedPromptAndInvocationEvidenceRoundTripWithoutRawProviderSecrets() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-direct-context-",
            )
                .toAbsolutePath()
        val productHome =
            parent.resolve(
                "VexLife-Prod",
            )

        try {
            home(
                productHome,
            )
            val conversation =
                DirectConversationStore(
                    productHome,
                )
            val context =
                DirectTurnContextStore(
                    productHome,
                )

            conversation.withWriterLease {
                val request =
                    beginRequest(
                        turnRef = "turn.self-development.context",
                        requestMessageRef = "message.self-development.human.context",
                        content = "Hello",
                    )
                val prompt =
                    DirectPromptContext.materialize(
                        prior = null,
                        currentRequest = request,
                    )
                val inference =
                    DirectCompanionConnection.invoke(
                        runtime = runtime(),
                        messages = prompt.messages,
                        transport =
                            DirectCompanionConnection.Transport {
                                DirectCompanionConnection.HttpResult(
                                    status = 200,
                                    body =
                                        """
                                        {
                                          "model": "/private/model/path",
                                          "choices": [
                                            {
                                              "message": {
                                                "role": "assistant",
                                                "content": "Hi",
                                                "reasoning": "do not persist me"
                                              }
                                            }
                                          ]
                                        }
                                        """.trimIndent(),
                                )
                            },
                        now = {
                            Instant.parse(
                                "2026-11-05T04:31:00Z",
                            )
                        },
                    )
                val response =
                    appendResponse(
                        request = request,
                        responseMessageRef = "message.self-development.companion.context",
                        content = inference.content,
                        model = inference.model,
                        endpointOrigin = inference.invocationEvidence.endpointOrigin,
                        runtimeObservationRef = inference.runtimeObservation.observationRef,
                        invocationEvidenceRef = inference.invocationEvidence.invocationRef,
                    )
                val record =
                    context.record(
                        request = request,
                        response = response,
                        promptContext = prompt.receipt,
                        runtimeObservation = inference.runtimeObservation,
                        invocationEvidence = inference.invocationEvidence,
                        formedAt = Instant.parse(
                            "2026-11-05T04:31:01Z",
                        ),
                    )

                val reread =
                    assertNotNull(
                        context.read(
                            request.turnRef,
                        ),
                    )
                assertEquals(
                    record,
                    reread,
                )
                assertFalse(
                    reread.rawProviderResponsePersisted,
                )
                assertFalse(
                    reread.rawReasoningPersisted,
                )
                assertFalse(
                    reread.rawReportedModelPersisted,
                )
                assertEquals(
                    "LOCAL_PATH_LIKE",
                    reread.runtimeObservation.reportedModelField.pathClass,
                )

                val serialized =
                    Files.readString(
                        productHome
                            .resolve(
                                "context",
                            )
                            .resolve(
                                "companion-lineage.vexlife.context-lineage",
                            )
                            .resolve(
                                "thread.self-development.open-conversation",
                            )
                            .resolve(
                                "turn.self-development.context.json",
                            ),
                    )
                assertFalse(
                    serialized.contains(
                        "/private/model/path",
                    ),
                )
                assertFalse(
                    serialized.contains(
                        "do not persist me",
                    ),
                )
                assertTrue(
                    serialized.contains(
                        prompt.receipt.receiptRef,
                    ),
                )
            }
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }
}

// [VXG RealForever]
