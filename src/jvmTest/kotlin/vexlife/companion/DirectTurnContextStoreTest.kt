package vexlife.companion

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.security.MessageDigest
import java.time.Instant
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vexlife.contract.VexContractJson
import vexlife.conversation.CanonicalLivedContextFixture
import vexlife.conversation.DirectConversationStore
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticProductIdentity
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeState

class DirectTurnContextStoreTest {
    @Test
    fun unboundNewerPromptReceiptSidecarDoesNotCreateCompatibilityMeaning() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-direct-context-unbound-newer-receipt-",
            )
                .toAbsolutePath()
        val productHome =
            parent.resolve(
                "VexLife-Prod",
            )
        val turnRef =
            "turn.self-development.unbound-newer-prompt-receipt"

        try {
            home(
                productHome,
            )
            val evidenceDirectory =
                productHome.resolve(
                    "runtime/vexlife-prod/turn-evidence",
                )
            Files.createDirectories(
                evidenceDirectory,
            )
            Files.writeString(
                evidenceDirectory.resolve(
                    "$turnRef.json",
                ),
                """
                {
                  "schemaVersion": "${DirectTurnContextStore.RuntimeEvidenceSchema}",
                  "turnRef": "$turnRef",
                  "evidenceRef": "evidence.vexlife-prod.turn-runtime.${"e".repeat(32)}",
                  "promptContext": {
                    "schemaVersion": "vexlife-prod.direct-prompt-materialization-receipt/v9",
                    "contextProfileRef": "profile.vexlife.context.product-default"
                  }
                }
                """.trimIndent(),
            )

            assertNull(
                DirectTurnContextStore(
                    productHome,
                )
                    .inspectPromptReceiptCompatibility(
                        turnRef,
                    ),
                "a sidecar that is not referenced by canonical Context must not create Product compatibility meaning",
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun historicalV8PromptReceiptRemainsReadableAfterV9Currentization() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-direct-context-historical-v8-receipt-",
            )
                .toAbsolutePath()
        val productHome =
            parent.resolve(
                "VexLife-Prod",
            )
        val turnRef =
            "turn.self-development.historical-v8-prompt-receipt"
        val evidenceRef =
            "evidence.vexlife-prod.turn-runtime." +
                "d".repeat(
                    32,
                )

        try {
            home(
                productHome,
            )
            val conversation =
                DirectConversationStore(
                    productHome,
                )
            var contextRelativePath =
                ""

            conversation.withWriterLease {
                val request =
                    beginRequest(
                        turnRef =
                            turnRef,
                        requestMessageRef =
                            "message.self-development.human.historical-v8-prompt-receipt",
                        content =
                            "Preserve the immediately preceding durable prompt receipt generation.",
                    )
                val response =
                    appendResponse(
                        request =
                            request,
                        responseMessageRef =
                            "message.self-development.companion.historical-v8-prompt-receipt",
                        content =
                            "Historical prompt receipt compatibility.",
                        model =
                            "default_model",
                        endpointOrigin =
                            "http://127.0.0.1:8765",
                        runtimeObservationRef =
                            "observation.historical-v8-prompt-receipt",
                        invocationEvidenceRef =
                            "evidence.historical-v8-prompt-receipt",
                    )
                val formed =
                    CanonicalLivedContextFixture.write(
                        productHome =
                            productHome,
                        request =
                            request,
                        response =
                            response,
                        formedAt =
                            Instant.parse(
                                "2026-11-18T15:20:00Z",
                            ),
                    )
                contextRelativePath =
                    formed.relativePath

                val contextPath =
                    productHome.resolve(
                        formed.relativePath,
                    )
                val observed =
                    VexContractJson.Strict
                        .parseToJsonElement(
                            Files.readString(
                                contextPath,
                            ),
                        )
                        .jsonObject
                val core =
                    JsonObject(
                        observed
                            .filterKeys {
                                it !=
                                    "serializedContextSha256"
                            }
                            .toMutableMap()
                            .also {
                                it[
                                    "contextSourceRefs"
                                ] =
                                    JsonArray(
                                        listOf(
                                            evidenceRef,
                                            request.eventRef,
                                            response.eventRef,
                                        )
                                            .map(
                                                ::JsonPrimitive,
                                            ),
                                    )
                            },
                    )
                val contextSha256 =
                    sha256(
                        VexContractJson.canonicalEncodeElement(
                            core,
                        ),
                    )
                val rebound =
                    JsonObject(
                        core
                            .toMutableMap()
                            .also {
                                it[
                                    "serializedContextSha256"
                                ] =
                                    JsonPrimitive(
                                        contextSha256,
                                    )
                            },
                    )
                Files.writeString(
                    contextPath,
                    VexContractJson.canonicalEncodeElement(
                        rebound,
                    ),
                )
            }

            val evidenceDirectory =
                productHome.resolve(
                    "runtime/vexlife-prod/turn-evidence",
                )
            Files.createDirectories(
                evidenceDirectory,
            )
            Files.writeString(
                evidenceDirectory.resolve(
                    "$turnRef.json",
                ),
                """
                {
                  "schemaVersion": "${DirectTurnContextStore.RuntimeEvidenceSchema}",
                  "turnRef": "$turnRef",
                  "evidenceRef": "$evidenceRef",
                  "promptContext": {
                    "schemaVersion": "${DirectPromptContext.ReceiptSchemaV8}"
                  }
                }
                """.trimIndent(),
            )

            val compatibility =
                assertNotNull(
                    DirectTurnContextStore(
                        productHome,
                    )
                        .inspectPromptReceiptCompatibility(
                            turnRef,
                        ),
                )
            assertEquals(
                DirectTurnContextStore.RuntimeEvidenceSchema,
                compatibility.runtimeEvidenceSchema,
            )
            assertEquals(
                DirectPromptContext.ReceiptSchemaV8,
                compatibility.promptReceiptSchema,
            )
            assertTrue(
                compatibility.readableByCurrentSource,
                "advancing current receipt v8 -> v9 must not make the immediately preceding durable receipt unreadable",
            )
            assertTrue(
                contextRelativePath.isNotBlank(),
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

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
                        formedAt =
                            Instant.parse(
                                "2026-11-12T00:00:13Z",
                            ),
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
                        formedAt =
                            Instant.parse(
                                "2026-11-12T00:00:14Z",
                            ),
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
                val initialRuntime =
                    runtime()
                val initialInference =
                    LocalHereticInference(
                        runtime = initialRuntime,
                        model =
                            requireNotNull(
                                HereticModelCatalog.entry(
                                    initialRuntime.modelRef,
                                ),
                            ),
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
                        .invoke(
                            CompanionInferenceConnection.Request(
                                messages = initialPrompt.messages,
                                capabilityOffer = emptyList(),
                                conversationCacheBoundaryMessageCount = null,
                                progressObserver = null,
                                cancellation = null,
                            ),
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
                    initialInference.observation.observationRef,
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
    fun historicalV1RuntimeEvidenceRemainsReadableAfterV2SourceReturnSchema() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-direct-context-v1-runtime-",
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
                        turnRef =
                            "turn.self-development.context-v1-runtime",
                        requestMessageRef =
                            "message.self-development.human.context-v1-runtime",
                        content =
                            "Preserve historical runtime evidence compatibility.",
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
                                          "model": "default_model",
                                          "choices": [
                                            {
                                              "message": {
                                                "role": "assistant",
                                                "content": "Historical evidence response."
                                              }
                                            }
                                          ]
                                        }
                                        """.trimIndent(),
                                )
                            },
                    )
                val response =
                    appendResponse(
                        request = request,
                        responseMessageRef =
                            "message.self-development.companion.context-v1-runtime",
                        content = inference.content,
                        model = inference.model,
                        endpointOrigin = inference.invocationEvidence.endpointOrigin,
                        runtimeObservationRef =
                            inference.runtimeObservation.observationRef,
                        invocationEvidenceRef =
                            inference.invocationEvidence.invocationRef,
                    )
                val record =
                    context.record(
                        request = request,
                        response = response,
                        promptContext = prompt.receipt,
                        runtimeObservation = inference.runtimeObservation,
                        invocationEvidence = inference.invocationEvidence,
                    )

                val evidencePath =
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
                        )
                val evidenceObject =
                    VexContractJson.Strict
                        .parseToJsonElement(
                            Files.readString(
                                evidencePath,
                            ),
                        )
                        .jsonObject
                val legacyCore =
                    JsonObject(
                        evidenceObject
                            .filterKeys {
                                it !=
                                    "evidenceRef" &&
                                    it !=
                                    "evidenceSha256"
                            }
                            .toMutableMap()
                            .also {
                                it[
                                    "schemaVersion"
                                ] =
                                    JsonPrimitive(
                                        DirectTurnContextStore.RuntimeEvidenceSchemaV1,
                                    )
                            },
                    )
                val legacyEvidenceSha256 =
                    sha256(
                        VexContractJson.canonicalEncodeElement(
                            legacyCore,
                        ),
                    )
                val legacyEvidenceRef =
                    "evidence.vexlife-prod.turn-runtime." +
                        legacyEvidenceSha256.take(
                            32,
                        )
                val legacyEvidence =
                    JsonObject(
                        legacyCore
                            .toMutableMap()
                            .also {
                                it[
                                    "evidenceRef"
                                ] =
                                    JsonPrimitive(
                                        legacyEvidenceRef,
                                    )
                                it[
                                    "evidenceSha256"
                                ] =
                                    JsonPrimitive(
                                        legacyEvidenceSha256,
                                    )
                            },
                    )
                Files.writeString(
                    evidencePath,
                    VexContractJson.canonicalEncodeElement(
                        legacyEvidence,
                    ),
                )

                val contextPath =
                    productHome
                        .resolve(
                            "context",
                        )
                        .resolve(
                            record.companionLineageRef,
                        )
                        .resolve(
                            record.threadRef,
                        )
                        .resolve(
                            request.turnRef +
                                ".json",
                        )
                val contextObject =
                    VexContractJson.Strict
                        .parseToJsonElement(
                            Files.readString(
                                contextPath,
                            ),
                        )
                        .jsonObject
                val legacyContextCore =
                    JsonObject(
                        contextObject
                            .filterKeys {
                                it !=
                                    "serializedContextSha256"
                            }
                            .toMutableMap()
                            .also {
                                it[
                                    "contextSourceRefs"
                                ] =
                                    JsonArray(
                                        record.contextSourceRefs.map {
                                            sourceRef ->
                                            JsonPrimitive(
                                                if (
                                                    sourceRef ==
                                                        record.runtimeEvidenceRef
                                                ) {
                                                    legacyEvidenceRef
                                                } else {
                                                    sourceRef
                                                },
                                            )
                                        },
                                    )
                            },
                    )
                val legacyContextSha256 =
                    sha256(
                        VexContractJson.canonicalEncodeElement(
                            legacyContextCore,
                        ),
                    )
                val legacyContext =
                    JsonObject(
                        legacyContextCore
                            .toMutableMap()
                            .also {
                                it[
                                    "serializedContextSha256"
                                ] =
                                    JsonPrimitive(
                                        legacyContextSha256,
                                    )
                            },
                    )
                Files.writeString(
                    contextPath,
                    VexContractJson.canonicalEncodeElement(
                        legacyContext,
                    ),
                )

                val reread =
                    assertNotNull(
                        context.read(
                            request.turnRef,
                        ),
                    )
                assertEquals(
                    legacyEvidenceRef,
                    reread.runtimeEvidenceRef,
                )
                assertEquals(
                    prompt.receipt,
                    reread.promptContext,
                )
                assertEquals(
                    null,
                    reread.sourceReturnTrace,
                )
                assertEquals(
                    legacyContextSha256,
                    reread.contextSha256,
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
                        providerMeasurement = inference.measurement,
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
                val providerMeasurement =
                    assertNotNull(
                        reread.providerMeasurement,
                    )
                assertEquals(
                    inference.measurement.stop.wire,
                    providerMeasurement.stop,
                )
                assertEquals(
                    inference.measurement.promptTokens,
                    providerMeasurement.promptTokens,
                )
                assertEquals(
                    inference.measurement.cachedPromptTokens,
                    providerMeasurement.cachedPromptTokens,
                )
                assertEquals(
                    inference.measurement.prefillTokens,
                    providerMeasurement.prefillTokens,
                )
                assertEquals(
                    inference.measurement.firstTokenMillis,
                    providerMeasurement.firstTokenMillis,
                )
                assertEquals(
                    inference.measurement.totalMillis,
                    providerMeasurement.totalMillis,
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
