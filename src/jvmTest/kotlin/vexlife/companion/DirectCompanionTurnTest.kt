package vexlife.companion

import java.nio.file.Files
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import vexlife.browser.VexLifeProdBrowserContract
import vexlife.continuity.VexLifeWorkingContinuityOwner
import vexlife.conversation.CanonicalLivedContextFixture
import vexlife.conversation.DirectConversationStore
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.local.VexLifeProdLocalSetup
import vexlife.memory.VexLifeMemoryContextFixture
import vexlife.memory.VexLifeMemoryContract
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticProductIdentity
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeState

class DirectCompanionTurnTest {
    private fun productHome(): java.nio.file.Path {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-direct-turn-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )
        CanonicalVexLifeHomeFixture.form(
            home = home,
            deviceSuffix = "turn-device",
            lineageSuffix = "turn-lineage",
        )
        return home
    }

    private fun setup(
        home: java.nio.file.Path,
    ): VexLifeProdLocalSetup.Result =
        VexLifeProdLocalSetup.Result(
            state = "LOCAL_HERETIC_FOUNDATION_READY",
            productHome = home.toString(),
            homeDisposition = "EXISTING_HOME_PRESERVED",
            modelSelectionDisposition = "BOUND",
            managedPythonDisposition = "REUSED_VERIFIED",
            artifactDisposition = "REUSED_VERIFIED",
            environmentDisposition = "REUSED_VERIFIED",
            runtimeDisposition = "REUSED_EXACT_OWNED_MLX_RUNTIME",
            modelBindingDisposition = "BOUND_CURRENT",
            modelConfigurationState = HereticModelConfiguration.State.BoundProductModel,
            qualifiedRuntimeEvidence =
                HereticModelConfiguration.QualifiedRuntimeEvidence(
                    runtimeReceiptRef = "receipt.vexlife-prod.heretic.turn",
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
                ),
            endpoint = HereticProductIdentity.Endpoint,
            requestModel = HereticProductIdentity.RequestModel,
        )

    @Test
    fun conditionalSourceReturnUsesTwoProviderCallsButOnlyFinalInferenceBecomesHumanTurn() {
        val home =
            productHome()
        val conversation =
            DirectConversationStore(
                home,
            )

        conversation.withWriterLease {
            val oldRequest =
                beginRequest(
                    turnRef = "turn.source-return.live.old",
                    requestMessageRef = "message.source-return.live.human.old",
                    content =
                        "lantern-garden exact older source " +
                            "O".repeat(
                                40_000,
                            ),
                )
            val oldResponse =
                appendResponse(
                    request = oldRequest,
                    responseMessageRef = "message.source-return.live.companion.old",
                    content = "I received the lantern-garden detail.",
                    model = "default_model",
                    endpointOrigin = "http://127.0.0.1:8765",
                    runtimeObservationRef = "observation.source-return.live.old",
                    invocationEvidenceRef = "evidence.source-return.live.old",
                )
            val oldContext =
                CanonicalLivedContextFixture.write(
                    productHome = home,
                    request = oldRequest,
                    response = oldResponse,
                    formedAt =
                        Instant.parse(
                            "2026-11-12T00:00:00Z",
                        ),
                )
            commitCompletedTurn(
                request = oldRequest,
                response = oldResponse,
                contextSha256 = oldContext.sha256,
            )

            val newerRequestTokens =
                190_000
            val newerRequest =
                beginRequest(
                    turnRef = "turn.source-return.live.newer",
                    requestMessageRef = "message.source-return.live.human.newer",
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
                    responseMessageRef = "message.source-return.live.companion.newer",
                    content = "Recent response without the old query term.",
                    model = "default_model",
                    endpointOrigin = "http://127.0.0.1:8765",
                    runtimeObservationRef = "observation.source-return.live.newer",
                    invocationEvidenceRef = "evidence.source-return.live.newer",
                )
            val newerContext =
                CanonicalLivedContextFixture.write(
                    productHome = home,
                    request = newerRequest,
                    response = newerResponse,
                    formedAt =
                        Instant.parse(
                            "2026-11-12T00:00:01Z",
                        ),
                )
            commitCompletedTurn(
                request = newerRequest,
                response = newerResponse,
                contextSha256 = newerContext.sha256,
            )
        }

        var calls =
            0
        var runtimeObservations =
            0
        var runtimeReobservations =
            0
        val progress =
            mutableListOf<DirectCompanionTurn.Progress>()
        val sourceReturnRequest =
            "VEXLIFE_CONVERSATION_SOURCE_RETURN_REQUEST/v1" +
                "\n" +
                """{"schemaVersion":"vexlife-prod.conversation-source-return-request/v1","queryTerms":["lantern-garden"],"speakerScope":"ANY"}"""
        val wrappedSourceReturnRequest =
            "I need to reopen exact source before answering.\n" +
                sourceReturnRequest
        val transport =
            object :
                DirectCompanionConnection.StreamingTransport {
                override fun post(
                    call: DirectCompanionConnection.HttpCall,
                ): DirectCompanionConnection.HttpResult =
                    error(
                        "source-return live test must use streaming transport",
                    )

                override fun postStreaming(
                    call: DirectCompanionConnection.HttpCall,
                    onLine: (String) -> Unit,
                ): DirectCompanionConnection.StreamingHttpResult {
                    calls +=
                        1

                    fun emit(
                        field: String,
                        delta: String,
                        finish: Boolean =
                            false,
                    ) {
                        val finishPart =
                            if (
                                finish
                            ) {
                                """"finish_reason":"stop","""
                            } else {
                                ""
                            }
                        onLine(
                            (
                                """data: {"model":"default_model","choices":[{__FINISH__"delta":{"role":"assistant","__FIELD__":__DELTA__}}]}"""
                            )
                                .replace(
                                    "__FINISH__",
                                    finishPart,
                                )
                                .replace(
                                    "__FIELD__",
                                    field,
                                )
                                .replace(
                                    "__DELTA__",
                                    kotlinx.serialization.json.JsonPrimitive(
                                        delta,
                                    )
                                        .toString(),
                                ),
                        )
                        onLine(
                            "",
                        )
                    }

                    when (
                        calls
                    ) {
                        1 -> {
                            assertTrue(
                                call.body.contains(
                                    "VEXLIFE_CONVERSATION_SOURCE_RETURN_PROTOCOL/v1",
                                ),
                            )
                            assertTrue(
                                call.body.contains(
                                    "What was the exact older lantern-garden detail?",
                                ),
                            )
                            assertFalse(
                                call.body.contains(
                                    "lantern-garden exact older source",
                                ),
                            )

                            emit(
                                field = "reasoning_content",
                                delta =
                                    "initial source-return reasoning visible live",
                            )
                            assertTrue(
                                progress.any {
                                    it.phase ==
                                        DirectCompanionTurn.ProgressPhase.Reasoning &&
                                        it.delta ==
                                        "initial source-return reasoning visible live"
                                },
                                "reasoning must reach the human before answer/control content decides the source-return route",
                            )
                            emit(
                                field = "content",
                                delta =
                                    wrappedSourceReturnRequest.take(
                                        24,
                                    ),
                            )
                            emit(
                                field = "content",
                                delta =
                                    wrappedSourceReturnRequest.substring(
                                        24,
                                    ),
                                finish = true,
                            )
                        }

                        2 -> {
                            assertTrue(
                                call.body.contains(
                                    "VEXLIFE_CONVERSATION_SOURCE_RETURN/v1",
                                ),
                            )
                            assertTrue(
                                call.body.contains(
                                    "lantern-garden exact older source",
                                ),
                            )
                            assertTrue(
                                call.body.contains(
                                    "What was the exact older lantern-garden detail?",
                                ),
                            )
                            assertFalse(
                                call.body.contains(
                                    "VEXLIFE_CONVERSATION_SOURCE_RETURN_PROTOCOL/v1",
                                ),
                            )

                            emit(
                                field = "reasoning_content",
                                delta =
                                    "final visible source-grounded reasoning",
                            )
                            emit(
                                field = "content",
                                delta =
                                    "The exact lantern-garden detail is now grounded in returned source.",
                                finish = true,
                            )
                        }

                        else ->
                            error(
                                "unexpected source-return provider call",
                            )
                    }

                    onLine(
                        "data: [DONE]",
                    )
                    onLine(
                        "",
                    )
                    return DirectCompanionConnection.StreamingHttpResult(
                        status = 200,
                        responseBodySha256 =
                            if (
                                calls ==
                                    1
                            ) {
                                "a".repeat(
                                    64,
                                )
                            } else {
                                "b".repeat(
                                    64,
                                )
                            },
                    )
                }
            }

        try {
            val result =
                DirectCompanionTurn.performWithHooks(
                    setup =
                        setup(
                            home,
                        ),
                    intent =
                        DirectCompanionTurn.Intent(
                            idempotencyKey =
                                "send.self-development.source-return.live.001",
                            content =
                                "What was the exact older lantern-garden detail?",
                        ),
                    hooks =
                        DirectCompanionTurn.Hooks(
                            transport = transport,
                            currentRuntimeEvidence = {
                                currentSetup ->
                                runtimeObservations +=
                                    1
                                currentSetup.qualifiedRuntimeEvidence
                            },
                            reobserveSameRuntimeEvidence = {
                                expected ->
                                runtimeReobservations +=
                                    1
                                expected
                            },
                            progressObserver =
                                DirectCompanionTurn.ProgressObserver {
                                    progress +=
                                        it
                                },
                            now = {
                                Instant.parse(
                                    "2026-11-11T06:05:00Z",
                                )
                            },
                        ),
                )

            assertEquals(
                2,
                calls,
            )
            assertEquals(
                1,
                runtimeObservations,
            )
            assertEquals(
                1,
                runtimeReobservations,
            )
            assertEquals(
                "The exact lantern-garden detail is now grounded in returned source.",
                result.content,
            )
            assertEquals(
                "final visible source-grounded reasoning",
                result.humanReasoning,
            )
            assertEquals(
                "RETURNED",
                result.promptContextReceipt.sourceReturnState,
            )
            assertFalse(
                result.promptContextReceipt.sourceReturnProtocolEnabled,
            )

            val projectedProgress =
                progress
                    .mapNotNull(
                        DirectCompanionTurn.Progress::delta,
                    )
                    .joinToString(
                        "",
                    )
            assertTrue(
                projectedProgress.contains(
                    "final visible source-grounded reasoning",
                ),
            )
            assertTrue(
                projectedProgress.contains(
                    "The exact lantern-garden detail is now grounded in returned source.",
                ),
            )
            assertTrue(
                projectedProgress.contains(
                    "initial source-return reasoning visible live",
                ),
            )
            assertFalse(
                projectedProgress.contains(
                    "VEXLIFE_CONVERSATION_SOURCE_RETURN_REQUEST/v1",
                ),
            )
            assertFalse(
                projectedProgress.contains(
                    "I need to reopen exact source before answering.",
                ),
            )

            val snapshot =
                DirectConversationStore(
                    home,
                )
                    .current()
            val currentTurnEvents =
                snapshot.canonicalEvents.filter {
                    it.turnRef ==
                        result.turnRef
                }
            assertEquals(
                2,
                currentTurnEvents.size,
            )
            assertEquals(
                listOf(
                    "REQUEST",
                    "RESPONSE",
                ),
                currentTurnEvents.map {
                    it.eventKind
                },
            )
            assertFalse(
                currentTurnEvents.any {
                    it.content.contains(
                        "VEXLIFE_CONVERSATION_SOURCE_RETURN_REQUEST/v1",
                    )
                },
            )

            val context =
                assertNotNull(
                    DirectTurnContextStore(
                        home,
                    )
                        .read(
                            result.turnRef,
                        ),
                )
            val trace =
                assertNotNull(
                    context.sourceReturnTrace,
                )
            assertEquals(
                result.promptContextReceipt.receiptRef,
                trace.finalPromptReceiptRef,
            )
            assertEquals(
                "RETURNED",
                trace.attempt.sourceReturnState,
            )
            assertTrue(
                trace.attempt.returnedSourceEventRefs.isNotEmpty(),
            )
            assertFalse(
                trace.rawProviderOutputPersisted,
            )
            assertFalse(
                trace.rawReasoningPersisted,
            )
        } finally {
            home.parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun sourceReturnRefusesRuntimeReplacementBetweenProviderStages() {
        val home =
            productHome()
        var calls =
            0
        val sourceReturnRequest =
            "VEXLIFE_CONVERSATION_SOURCE_RETURN_REQUEST/v1" +
                "\n" +
                """{"schemaVersion":"vexlife-prod.conversation-source-return-request/v1","queryTerms":["missing-detail"],"speakerScope":"ANY"}"""
        val transport =
            DirectCompanionConnection.Transport {
                calls +=
                    1
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
            }

        try {
            val failure =
                assertFailsWith<DirectCompanionTurn.TurnException> {
                    DirectCompanionTurn.performWithHooks(
                        setup =
                            setup(
                                home,
                            ),
                        intent =
                            DirectCompanionTurn.Intent(
                                idempotencyKey =
                                    "send.self-development.source-return.runtime-drift.001",
                                content =
                                    "Find an older detail if needed.",
                            ),
                        hooks =
                            DirectCompanionTurn.Hooks(
                                transport = transport,
                                reobserveSameRuntimeEvidence = {
                                    expected ->
                                    expected.copy(
                                        runtimeReceiptRef =
                                            "receipt.vexlife-prod.heretic.changed-mid-turn",
                                    )
                                },
                            ),
                    )
                }

            assertEquals(
                1,
                calls,
            )
            assertEquals(
                "ADMISSION_FAILED",
                failure.failureCode,
            )
            assertTrue(
                failure.cause
                    ?.message
                    ?.contains(
                        "runtime changed during one direct Companion turn",
                    ) ==
                    true,
            )
            assertFalse(
                failure.failureReceipt
                    ?.currentHeadCommitted
                    ?: true,
            )
        } finally {
            home.parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun finalSourceReturnInferenceCannotRequestSourceAgainOrBecomeCanonicalResponse() {
        val home =
            productHome()
        var calls =
            0
        val sourceReturnRequest =
            "VEXLIFE_CONVERSATION_SOURCE_RETURN_REQUEST/v1" +
                "\n" +
                """{"schemaVersion":"vexlife-prod.conversation-source-return-request/v1","queryTerms":["missing-detail"],"speakerScope":"ANY"}"""
        val transport =
            DirectCompanionConnection.Transport {
                calls +=
                    1
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
            }

        try {
            val failure =
                assertFailsWith<DirectCompanionTurn.TurnException> {
                    DirectCompanionTurn.performWithHooks(
                        setup =
                            setup(
                                home,
                            ),
                        intent =
                            DirectCompanionTurn.Intent(
                                idempotencyKey =
                                    "send.self-development.source-return.recursion.001",
                                content =
                                    "Find an older detail if needed.",
                            ),
                        hooks =
                            DirectCompanionTurn.Hooks(
                                transport = transport,
                            ),
                    )
                }

            assertEquals(
                2,
                calls,
            )
            assertEquals(
                "SOURCE_RETURN_RECURSION_NOT_ADMITTED",
                failure.failureCode,
            )
            val receipt =
                assertNotNull(
                    failure.failureReceipt,
                )
            assertTrue(
                receipt.requestDurablyRecorded,
            )
            assertFalse(
                receipt.responseDurablyRecorded,
            )
            assertFalse(
                receipt.contextDurablyRecorded,
            )
            assertFalse(
                receipt.currentHeadCommitted,
            )

            val snapshot =
                DirectConversationStore(
                    home,
                )
                    .current()
            assertNull(
                snapshot.head,
            )
            assertTrue(
                snapshot.canonicalEvents.isEmpty(),
            )
            val abandoned =
                snapshot.abandonedAttempts
                    .single()
                    .requestEvent
            assertNull(
                DirectTurnContextStore(
                    home,
                )
                    .read(
                        abandoned.turnRef,
                    ),
            )
        } finally {
            home.parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun threeTurnsCarryRecentCanonicalConversationAndRemainCanonical() {
        val home =
            productHome()
        var calls =
            0
        val transport =
            DirectCompanionConnection.Transport {
                call ->
                calls +=
                    1
                assertTrue(
                    call.body.contains(
                        "VEXLIFE_SELF_CAPABILITY_FRAME/v1",
                    ),
                )
                val content =
                    when (
                        calls
                    ) {
                        1 ->
                            "First response"

                        2 -> {
                            assertTrue(
                                call.body.contains(
                                    "First request",
                                ),
                            )
                            assertTrue(
                                call.body.contains(
                                    "First response",
                                ),
                            )
                            assertTrue(
                                call.body.contains(
                                    "Second request",
                                ),
                            )
                            "Second response"
                        }

                        3 -> {
                            assertTrue(
                                call.body.contains(
                                    "First request",
                                ),
                            )
                            assertTrue(
                                call.body.contains(
                                    "First response",
                                ),
                            )
                            assertTrue(
                                call.body.contains(
                                    "Second request",
                                ),
                            )
                            assertTrue(
                                call.body.contains(
                                    "Second response",
                                ),
                            )
                            assertTrue(
                                call.body.contains(
                                    "Third request",
                                ),
                            )
                            "Third response"
                        }

                        else ->
                            error(
                                "unexpected provider call",
                            )
                    }

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
                                "content": "$content"
                              }
                            }
                          ]
                        }
                        """.trimIndent(),
                )
            }

        try {
            val first =
                DirectCompanionTurn.performWithHooks(
                    setup = setup(
                        home,
                    ),
                    intent =
                        DirectCompanionTurn.Intent(
                            idempotencyKey = "send.self-development.001",
                            content = "First request",
                        ),
                    hooks =
                        DirectCompanionTurn.Hooks(
                            transport = transport,
                            now = {
                                Instant.parse(
                                    "2026-11-05T05:01:00Z",
                                )
                            },
                        ),
                )
            val second =
                DirectCompanionTurn.performWithHooks(
                    setup = setup(
                        home,
                    ),
                    intent =
                        DirectCompanionTurn.Intent(
                            idempotencyKey = "send.self-development.002",
                            content = "Second request",
                        ),
                    hooks =
                        DirectCompanionTurn.Hooks(
                            transport = transport,
                            now = {
                                Instant.parse(
                                    "2026-11-05T05:02:00Z",
                                )
                            },
                        ),
                )
            val third =
                DirectCompanionTurn.performWithHooks(
                    setup = setup(
                        home,
                    ),
                    intent =
                        DirectCompanionTurn.Intent(
                            idempotencyKey = "send.self-development.003",
                            content = "Third request",
                        ),
                    hooks =
                        DirectCompanionTurn.Hooks(
                            transport = transport,
                            now = {
                                Instant.parse(
                                    "2026-11-05T05:03:00Z",
                                )
                            },
                        ),
                )

            assertEquals(
                3,
                calls,
            )
            assertTrue(
                first.promptContextReceipt.capabilityFrameIncluded,
            )
            assertNotNull(
                first.promptContextReceipt.capabilityFrameRef,
            )
            assertTrue(
                first.promptContextReceipt.capabilityFrameSourceRefs.contains(
                    "receipt.vexlife-prod.heretic.turn",
                ),
            )
            assertEquals(
                "First response",
                first.content,
            )
            assertEquals(
                "Second response",
                second.content,
            )
            assertEquals(
                "IMMEDIATE_PRIOR_COMPLETED_TURN",
                second.promptContextReceipt.contextMode,
            )
            assertEquals(
                2,
                second.promptContextReceipt.selectedConversationEventRefs.size,
            )
            assertEquals(
                "Third response",
                third.content,
            )
            assertEquals(
                "RECENT_COMPLETED_TURNS",
                third.promptContextReceipt.contextMode,
            )
            assertEquals(
                4,
                third.promptContextReceipt.selectedConversationEventRefs.size,
            )
            assertEquals(
                listOf(
                    0L,
                    1L,
                    2L,
                    3L,
                ),
                third.promptContextReceipt.selectedConversationEventSequences,
            )
            assertTrue(
                first.currentConversationHeadSha256 !=
                    second.currentConversationHeadSha256,
            )
            assertTrue(
                second.currentConversationHeadSha256 !=
                    third.currentConversationHeadSha256,
            )
        } finally {
            home.parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun successfulHighPressureTurnRollsIntoWorkingCheckpointSuccessorOnNextRealTurn() {
        val home =
            productHome()
        val memory =
            VexLifeMemoryContextFixture.current()
        val requestTokenTarget =
            DirectPromptContext.InputTokenBudget -
                DirectPromptContext.ReservedOutputTokens +
                100 -
                memory.estimatedSystemMessageTokens
        val requestContent =
            "x".repeat(
                (
                    requestTokenTarget -
                        4
                ) *
                    4,
            )
        var providerCalls =
            0
        val transport =
            DirectCompanionConnection.Transport {
                call ->
                providerCalls +=
                    1
                when (
                    providerCalls
                ) {
                    1 ->
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
                                        "content": "Rollover producer response"
                                      }
                                    }
                                  ]
                                }
                                """.trimIndent(),
                        )

                    else -> {
                        assertTrue(
                            call.body.contains(
                                "VEXLIFE_CURRENT_MEMORY_CONTEXT/v1",
                            ),
                        )
                        assertTrue(
                            call.body.contains(
                                "VEXLIFE_WORKING_CONTINUITY_CONTEXT/v1",
                            ),
                        )
                        assertTrue(
                            call.body.contains(
                                "Successor request after rollover",
                            ),
                        )
                        assertTrue(
                            call.body.contains(
                                "statement.score.continuity.001",
                            ),
                        )
                        assertTrue(
                            call.body.contains(
                                "Rollover producer response",
                            ),
                        )
                        assertFalse(
                            call.body.contains(
                                "x".repeat(
                                    1024,
                                ),
                            ),
                        )

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
                                        "content": "Successor response with prior Vex response bridge"
                                      }
                                    }
                                  ]
                                }
                                """.trimIndent(),
                        )
                    }
                }
            }

        try {
            val first =
                DirectCompanionTurn.performWithHooks(
                    setup =
                        setup(
                            home,
                        ),
                    intent =
                        DirectCompanionTurn.Intent(
                            idempotencyKey =
                                "send.self-development.rollover-producer",
                            content =
                                requestContent,
                        ),
                    hooks =
                        DirectCompanionTurn.Hooks(
                            transport = transport,
                            currentMemoryContext = {
                                memory
                            },
                            memoryContextAtCap = {
                                _,
                                memoryTokenCap,
                                ->
                                VexLifeMemoryContextFixture.current(
                                    memoryTokenCap,
                                )
                            },
                            now = {
                                Instant.parse(
                                    "2026-11-06T11:10:00Z",
                                )
                            },
                        ),
                )

            assertEquals(
                1,
                providerCalls,
            )
            assertEquals(
                "TURN_COMPLETED",
                first.state,
            )

            val continuity =
                assertNotNull(
                    DirectWorkingContinuity.read(
                        productHome = home,
                        turnRef = first.turnRef,
                    ),
                )
            assertEquals(
                "ROLLOVER_COMMITTED",
                continuity.disposition,
            )
            assertTrue(
                continuity.effects.postCommitReceiptPersisted,
            )
            assertTrue(
                continuity.effects.workingCheckpointPersisted,
            )
            assertTrue(
                continuity.effects.workingHeadMutated,
            )

            val conversationAfterFirst =
                DirectConversationStore(
                    home,
                )
                    .current()
            val firstHead =
                assertNotNull(
                    conversationAfterFirst.head,
                )
            val owner =
                VexLifeWorkingContinuityOwner(
                    home,
                )
            val ownerCurrent =
                assertNotNull(
                    owner.readCurrent(
                        lineageRef =
                            firstHead.companionLineageRef,
                        threadRef =
                            firstHead.threadRef,
                    ),
                )
            assertEquals(
                "ROLLOVER_COMMITTED",
                ownerCurrent.commitReceipt.disposition,
            )
            assertTrue(
                ownerCurrent.commitReceipt.successorContextRequired,
            )
            assertEquals(
                first.requestEventRef,
                ownerCurrent.checkpoint.workingSet.cursorEventRef,
            )
            assertTrue(
                ownerCurrent.checkpoint.workingSet.currentStatementRefs.isEmpty(),
            )

            val second =
                DirectCompanionTurn.performWithHooks(
                    setup =
                        setup(
                            home,
                        ),
                    intent =
                        DirectCompanionTurn.Intent(
                            idempotencyKey =
                                "send.self-development.rollover-successor",
                            content =
                                "Successor request after rollover",
                        ),
                    hooks =
                        DirectCompanionTurn.Hooks(
                            transport = transport,
                            currentMemoryContext = {
                                memory
                            },
                            memoryContextAtCap = {
                                _,
                                memoryTokenCap,
                                ->
                                VexLifeMemoryContextFixture.current(
                                    memoryTokenCap,
                                )
                            },
                            now = {
                                Instant.parse(
                                    "2026-11-06T11:11:00Z",
                                )
                            },
                        ),
                )

            assertEquals(
                2,
                providerCalls,
            )
            assertEquals(
                "Successor response with prior Vex response bridge",
                second.content,
            )
            assertEquals(
                "WORKING_CHECKPOINT_SUCCESSOR",
                second.promptContextReceipt.contextMode,
            )
            assertTrue(
                second.promptContextReceipt.selectedConversationEventRefs.isEmpty(),
            )
            assertTrue(
                second.promptContextReceipt.selectedConversationEventSha256.isEmpty(),
            )
            assertEquals(
                first.responseEventRef,
                second.promptContextReceipt.successorPriorResponseEventRef,
            )
            assertNotNull(
                second.promptContextReceipt.successorPriorResponseEventSha256,
            )
            assertNotNull(
                second.promptContextReceipt.successorPriorResponseEventSequence,
            )
            assertTrue(
                second.promptContextReceipt.workingContinuityIncluded,
            )
            assertNotNull(
                second.promptContextReceipt.workingContinuityContextRef,
            )
            assertNotNull(
                second.promptContextReceipt.workingContinuityContextFingerprint,
            )
            assertNotNull(
                second.promptContextReceipt.workingContinuityCheckpointRef,
            )
            assertNotNull(
                second.promptContextReceipt.workingContinuitySuccessorClaimRef,
            )

            val secondContext =
                assertNotNull(
                    DirectTurnContextStore(
                        home,
                    )
                        .read(
                            second.turnRef,
                        ),
                )
            assertTrue(
                secondContext.contextSourceRefs.contains(
                    requireNotNull(
                        second.promptContextReceipt.workingContinuityContextRef,
                    ),
                ),
            )
            assertTrue(
                secondContext.contextSourceRefs.contains(
                    requireNotNull(
                        second.promptContextReceipt.workingContinuityCheckpointRef,
                    ),
                ),
            )
            assertTrue(
                secondContext.contextSourceRefs.contains(
                    requireNotNull(
                        second.promptContextReceipt.workingContinuitySuccessorClaimRef,
                    ),
                ),
            )
            assertTrue(
                secondContext.contextSourceRefs.contains(
                    requireNotNull(
                        second.promptContextReceipt.capabilityFrameRef,
                    ),
                ),
            )
            assertTrue(
                secondContext.contextSourceRefs.contains(
                    first.responseEventRef,
                ),
            )
        } finally {
            home.parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun failedWorkingSuccessorDoesNotBlockFreshRequestScopedSuccessorAttempt() {
        val home =
            productHome()
        val memory =
            VexLifeMemoryContextFixture.current()
        val requestTokenTarget =
            DirectPromptContext.InputTokenBudget -
                DirectPromptContext.ReservedOutputTokens +
                100 -
                memory.estimatedSystemMessageTokens
        val highPressureRequest =
            "y".repeat(
                (
                    requestTokenTarget -
                        4
                ) *
                    4,
            )
        var calls =
            0
        val transport =
            DirectCompanionConnection.Transport {
                call ->
                calls +=
                    1
                when (
                    calls
                ) {
                    1 ->
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
                                        "content": "Rollover recovery baseline"
                                      }
                                    }
                                  ]
                                }
                                """.trimIndent(),
                        )

                    2 -> {
                        assertTrue(
                            call.body.contains(
                                "VEXLIFE_WORKING_CONTINUITY_CONTEXT/v1",
                            ),
                        )
                        assertTrue(
                            call.body.contains(
                                "Failed successor request",
                            ),
                        )
                        assertTrue(
                            call.body.contains(
                                "Rollover recovery baseline",
                            ),
                        )
                        assertFalse(
                            call.body.contains(
                                "y".repeat(
                                    1024,
                                ),
                            ),
                        )
                        DirectCompanionConnection.HttpResult(
                            status = 503,
                            body = "",
                        )
                    }

                    else -> {
                        assertTrue(
                            call.body.contains(
                                "VEXLIFE_WORKING_CONTINUITY_CONTEXT/v1",
                            ),
                        )
                        assertTrue(
                            call.body.contains(
                                "Fresh successor recovery request",
                            ),
                        )
                        assertFalse(
                            call.body.contains(
                                "Failed successor request",
                            ),
                        )
                        assertTrue(
                            call.body.contains(
                                "Rollover recovery baseline",
                            ),
                        )
                        assertFalse(
                            call.body.contains(
                                "y".repeat(
                                    1024,
                                ),
                            ),
                        )
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
                                        "content": "Fresh request-scoped successor recovered"
                                      }
                                    }
                                  ]
                                }
                                """.trimIndent(),
                        )
                    }
                }
            }

        try {
            val first =
                DirectCompanionTurn.performWithHooks(
                    setup =
                        setup(
                            home,
                        ),
                    intent =
                        DirectCompanionTurn.Intent(
                            idempotencyKey =
                                "send.self-development.rollover-recovery-base",
                            content =
                                highPressureRequest,
                        ),
                    hooks =
                        DirectCompanionTurn.Hooks(
                            transport = transport,
                            currentMemoryContext = {
                                memory
                            },
                            memoryContextAtCap = {
                                _,
                                memoryTokenCap,
                                ->
                                VexLifeMemoryContextFixture.current(
                                    memoryTokenCap,
                                )
                            },
                        ),
                )

            val firstHead =
                assertNotNull(
                    DirectConversationStore(
                        home,
                    )
                        .current()
                        .head,
                )
            val owner =
                VexLifeWorkingContinuityOwner(
                    home,
                )
            assertEquals(
                "ROLLOVER_COMMITTED",
                assertNotNull(
                    owner.readCurrent(
                        lineageRef =
                            firstHead.companionLineageRef,
                        threadRef =
                            firstHead.threadRef,
                    ),
                )
                    .commitReceipt
                    .disposition,
            )

            assertFailsWith<DirectCompanionTurn.TurnException> {
                DirectCompanionTurn.performWithHooks(
                    setup =
                        setup(
                            home,
                        ),
                    intent =
                        DirectCompanionTurn.Intent(
                            idempotencyKey =
                                "send.self-development.rollover-recovery-failed",
                            content =
                                "Failed successor request",
                        ),
                    hooks =
                        DirectCompanionTurn.Hooks(
                            transport = transport,
                            currentMemoryContext = {
                                memory
                            },
                            memoryContextAtCap = {
                                _,
                                memoryTokenCap,
                                ->
                                VexLifeMemoryContextFixture.current(
                                    memoryTokenCap,
                                )
                            },
                        ),
                )
            }

            val afterFailure =
                DirectConversationStore(
                    home,
                )
                    .current()
            assertEquals(
                first.currentConversationHeadSha256,
                afterFailure.head
                    ?.conversationHeadSha256,
            )
            val failedRequest =
                afterFailure.abandonedAttempts
                    .single()
                    .requestEvent
            val failedBinding =
                VexLifeMemoryContract.CurrentRequestBinding(
                    eventRef =
                        failedRequest.eventRef,
                    eventHash =
                        failedRequest.eventHash,
                    sequence =
                        failedRequest.sequence,
                )
            val failedContext =
                assertNotNull(
                    owner.readSuccessorContext(
                        lineageRef =
                            firstHead.companionLineageRef,
                        threadRef =
                            firstHead.threadRef,
                        currentRequestEventBinding =
                            failedBinding,
                    ),
                )

            val recovered =
                DirectCompanionTurn.performWithHooks(
                    setup =
                        setup(
                            home,
                        ),
                    intent =
                        DirectCompanionTurn.Intent(
                            idempotencyKey =
                                "send.self-development.rollover-recovery-fresh",
                            content =
                                "Fresh successor recovery request",
                        ),
                    hooks =
                        DirectCompanionTurn.Hooks(
                            transport = transport,
                            currentMemoryContext = {
                                memory
                            },
                            memoryContextAtCap = {
                                _,
                                memoryTokenCap,
                                ->
                                VexLifeMemoryContextFixture.current(
                                    memoryTokenCap,
                                )
                            },
                        ),
                )

            assertEquals(
                3,
                calls,
            )
            assertEquals(
                "Fresh request-scoped successor recovered",
                recovered.content,
            )
            assertEquals(
                "WORKING_CHECKPOINT_SUCCESSOR",
                recovered.promptContextReceipt.contextMode,
            )
            assertTrue(
                recovered.promptContextReceipt.selectedConversationEventRefs.isEmpty(),
            )
            assertEquals(
                first.responseEventRef,
                recovered.promptContextReceipt.successorPriorResponseEventRef,
            )
            assertNotNull(
                recovered.promptContextReceipt.successorPriorResponseEventSha256,
            )

            val afterRecovery =
                DirectConversationStore(
                    home,
                )
                    .current()
            val recoveredRequest =
                afterRecovery.canonicalEvents
                    .single {
                        it.eventRef ==
                            recovered.requestEventRef
                    }
            val recoveredBinding =
                VexLifeMemoryContract.CurrentRequestBinding(
                    eventRef =
                        recoveredRequest.eventRef,
                    eventHash =
                        recoveredRequest.eventHash,
                    sequence =
                        recoveredRequest.sequence,
                )
            val recoveredContext =
                assertNotNull(
                    owner.readSuccessorContext(
                        lineageRef =
                            firstHead.companionLineageRef,
                        threadRef =
                            firstHead.threadRef,
                        currentRequestEventBinding =
                            recoveredBinding,
                    ),
                )

            assertTrue(
                failedContext.successorContextRef !=
                    recoveredContext.successorContextRef,
            )
            assertEquals(
                failedRequest.eventRef,
                failedContext.currentRequestEventBinding.eventRef,
            )
            assertEquals(
                recovered.requestEventRef,
                recoveredContext.currentRequestEventBinding.eventRef,
            )
        } finally {
            home.parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun postCompletionContinuityExceptionCannotRetroactivelyFailCompletedTurn() {
        val home =
            productHome()
        var providerCalls =
            0

        try {
            val result =
                DirectCompanionTurn.performWithHooks(
                    setup =
                        setup(
                            home,
                        ),
                    intent =
                        DirectCompanionTurn.Intent(
                            idempotencyKey =
                                "send.self-development.continuity-post-commit-isolation",
                            content =
                                "Complete this turn even if post-completion continuity breaks.",
                        ),
                    hooks =
                        DirectCompanionTurn.Hooks(
                            transport =
                                DirectCompanionConnection.Transport {
                                    providerCalls +=
                                        1
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
                                                    "content": "Completed before continuity stewardship."
                                                  }
                                                }
                                              ]
                                            }
                                            """.trimIndent(),
                                    )
                                },
                            commitWorkingContinuity = {
                                _,
                                _,
                                _,
                                _,
                                ->
                                error(
                                    "synthetic post-completion continuity failure",
                                )
                            },
                        ),
                )

            assertEquals(
                1,
                providerCalls,
            )
            assertEquals(
                "TURN_COMPLETED",
                result.state,
            )
            assertEquals(
                "Completed before continuity stewardship.",
                result.content,
            )

            val current =
                DirectConversationStore(
                    home,
                )
                    .current()
            assertEquals(
                result.currentConversationHeadSha256,
                current.head
                    ?.conversationHeadSha256,
            )
            assertEquals(
                2,
                current.canonicalEvents.size,
            )
            assertTrue(
                current.abandonedAttempts.isEmpty(),
            )

            assertFalse(
                Files.exists(
                    home
                        .resolve(
                            "recovery/direct-companion",
                        )
                        .resolve(
                            result.turnRef +
                                ".json",
                        ),
                ),
            )
            assertNull(
                DirectWorkingContinuity.read(
                    productHome = home,
                    turnRef = result.turnRef,
                ),
            )
        } finally {
            home.parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun exactCompletedSendRetryReturnsDurableResultWithoutAnotherModelCall() {
        val home =
            productHome()
        var calls =
            0
        var runtimeObservations =
            0
        val transport =
            DirectCompanionConnection.Transport {
                calls +=
                    1
                DirectCompanionConnection.HttpResult(
                    status = 200,
                    body =
                        """
                        {
                          "choices": [
                            {
                              "message": {
                                "role": "assistant",
                                "content": "Canonical response"
                              }
                            }
                          ]
                        }
                        """.trimIndent(),
                )
            }
        val intent =
            DirectCompanionTurn.Intent(
                idempotencyKey = "send.self-development.retry",
                content = "One request",
            )

        try {
            val first =
                DirectCompanionTurn.performWithHooks(
                    setup = setup(
                        home,
                    ),
                    intent = intent,
                    hooks =
                        DirectCompanionTurn.Hooks(
                            transport = transport,
                            currentRuntimeEvidence = {
                                currentSetup ->
                                runtimeObservations +=
                                    1
                                currentSetup.qualifiedRuntimeEvidence
                            },
                        ),
                )
            val recovered =
                DirectCompanionTurn.performWithHooks(
                    setup = setup(
                        home,
                    ),
                    intent = intent,
                    hooks =
                        DirectCompanionTurn.Hooks(
                            transport = transport,
                            currentRuntimeEvidence = {
                                currentSetup ->
                                runtimeObservations +=
                                    1
                                currentSetup.qualifiedRuntimeEvidence
                            },
                        ),
                )

            assertEquals(
                1,
                calls,
            )
            assertEquals(
                1,
                runtimeObservations,
                "exact durable duplicate must not require current runtime observation",
            )
            assertFalse(
                first.recoveredCompletedTurn,
            )
            assertTrue(
                recovered.recoveredCompletedTurn,
            )
            assertFalse(
                recovered.modelCallPerformed,
            )
            assertFalse(
                recovered.actualHttpCallPerformedNow,
            )
            assertEquals(
                first.content,
                recovered.content,
            )
        } finally {
            home.parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun newSendRequiresFreshRuntimeCurrentnessBeforeProviderEffect() {
        val home =
            productHome()
        var providerCalls =
            0
        var runtimeObservations =
            0

        try {
            val failed =
                assertFailsWith<DirectCompanionTurn.TurnException> {
                    DirectCompanionTurn.performWithHooks(
                        setup = setup(
                            home,
                        ),
                        intent =
                            DirectCompanionTurn.Intent(
                                idempotencyKey = "send.self-development.runtime-held",
                                content = "Do not call the provider",
                            ),
                        hooks =
                            DirectCompanionTurn.Hooks(
                                transport =
                                    DirectCompanionConnection.Transport {
                                        providerCalls +=
                                            1
                                        error(
                                            "provider must not run without current runtime evidence",
                                        )
                                    },
                                currentRuntimeEvidence = {
                                    _ ->
                                    runtimeObservations +=
                                        1
                                    error(
                                        "runtime currentness held",
                                    )
                                },
                            ),
                    )
                }

            assertEquals(
                1,
                runtimeObservations,
            )
            assertEquals(
                0,
                providerCalls,
            )
            assertFalse(
                failed.retrySameSendAttemptAllowed,
            )

            val current =
                DirectConversationStore(
                    home,
                )
                    .current()
            assertEquals(
                null,
                current.head,
            )
            assertEquals(
                1,
                current.abandonedAttempts.size,
            )
        } finally {
            home.parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun providerFailurePreservesRequestSuppressesSameAttemptAndAllowsNewAttempt() {
        val home =
            productHome()
        var calls =
            0
        val failedIntent =
            DirectCompanionTurn.Intent(
                idempotencyKey = "send.self-development.failure",
                content = "Please answer",
            )

        try {
            val error =
                assertFailsWith<DirectCompanionTurn.TurnException> {
                    DirectCompanionTurn.performWithHooks(
                        setup = setup(
                            home,
                        ),
                        intent = failedIntent,
                        hooks =
                            DirectCompanionTurn.Hooks(
                                transport =
                                    DirectCompanionConnection.Transport {
                                        calls +=
                                            1
                                        DirectCompanionConnection.HttpResult(
                                            status = 503,
                                            body = "",
                                        )
                                    },
                            ),
                    )
                }

            val receipt =
                assertNotNull(
                    error.failureReceipt,
                )
            assertEquals(
                "CONNECTION_EndpointHttpError",
                receipt.failureCode,
            )
            assertTrue(
                receipt.requestDurablyRecorded,
            )
            assertFalse(
                receipt.responseDurablyRecorded,
            )
            assertFalse(
                receipt.currentHeadCommitted,
            )
            assertFalse(
                receipt.retrySameSendAttemptAllowed,
            )
            assertEquals(
                "FORM_NEW_SEND_ATTEMPT_AND_RETRY",
                receipt.exactNextSafeRoute,
            )

            val failedState =
                DirectConversationStore(
                    home,
                )
                    .current()
            assertNull(
                failedState.head,
            )
            assertEquals(
                1,
                failedState.abandonedAttempts.size,
            )

            val sameAttempt =
                assertFailsWith<DirectCompanionTurn.TurnException> {
                    DirectCompanionTurn.performWithHooks(
                        setup = setup(
                            home,
                        ),
                        intent = failedIntent,
                        hooks =
                            DirectCompanionTurn.Hooks(
                                transport =
                                    DirectCompanionConnection.Transport {
                                        calls +=
                                            1
                                        error(
                                            "same failed send must not reach the provider",
                                        )
                                    },
                            ),
                    )
                }

            assertEquals(
                "FAILED_SEND_ATTEMPT_REQUIRES_NEW_SEND_ATTEMPT",
                sameAttempt.failureCode,
            )
            assertFalse(
                sameAttempt.retrySameSendAttemptAllowed,
            )
            assertEquals(
                "FORM_NEW_SEND_ATTEMPT_AND_RETRY",
                sameAttempt.exactNextSafeRoute,
            )
            assertEquals(
                1,
                calls,
            )

            val recovered =
                DirectCompanionTurn.performWithHooks(
                    setup = setup(
                        home,
                    ),
                    intent =
                        DirectCompanionTurn.Intent(
                            idempotencyKey = "send.self-development.recovery-new",
                            content = "Please answer again",
                        ),
                    hooks =
                        DirectCompanionTurn.Hooks(
                            transport =
                                DirectCompanionConnection.Transport {
                                    calls +=
                                        1
                                    DirectCompanionConnection.HttpResult(
                                        status = 200,
                                        body =
                                            """
                                            {
                                              "choices": [
                                                {
                                                  "message": {
                                                    "role": "assistant",
                                                    "content": "Recovered on a new turn."
                                                  }
                                                }
                                              ]
                                            }
                                            """.trimIndent(),
                                    )
                                },
                        ),
                )

            assertEquals(
                "Recovered on a new turn.",
                recovered.content,
            )
            assertEquals(
                2,
                calls,
            )

            val current =
                DirectConversationStore(
                    home,
                )
                    .current()
            assertNotNull(
                current.head,
            )
            assertEquals(
                2,
                current.canonicalEvents.size,
            )
            assertEquals(
                1,
                current.abandonedAttempts.size,
            )
            assertEquals(
                "Please answer",
                current.abandonedAttempts
                    .single()
                    .requestEvent
                    .content,
            )
        } finally {
            home.parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun failedFollowUpResumesLastValidHeadOnlyWithANewSendAttempt() {
        val home =
            productHome()
        val setup =
            setup(
                home,
            )
        var calls =
            0
        val transport =
            DirectCompanionConnection.Transport {
                call ->
                calls +=
                    1

                when (
                    calls
                ) {
                    1 ->
                        DirectCompanionConnection.HttpResult(
                            status = 200,
                            body =
                                """
                                {
                                  "choices": [
                                    {
                                      "message": {
                                        "role": "assistant",
                                        "content": "Canonical first response"
                                      }
                                    }
                                  ]
                                }
                                """.trimIndent(),
                        )

                    2 ->
                        DirectCompanionConnection.HttpResult(
                            status = 503,
                            body = "",
                        )

                    else -> {
                        assertTrue(
                            call.body.contains(
                                "Canonical first request",
                            ),
                        )
                        assertTrue(
                            call.body.contains(
                                "Canonical first response",
                            ),
                        )
                        assertTrue(
                            call.body.contains(
                                "Fresh recovery request",
                            ),
                        )
                        assertFalse(
                            call.body.contains(
                                "Failed follow-up request",
                            ),
                        )

                        DirectCompanionConnection.HttpResult(
                            status = 200,
                            body =
                                """
                                {
                                  "choices": [
                                    {
                                      "message": {
                                        "role": "assistant",
                                        "content": "Recovery continued from the last valid head"
                                      }
                                    }
                                  ]
                                }
                                """.trimIndent(),
                        )
                    }
                }
            }

        try {
            val first =
                DirectCompanionTurn.performWithHooks(
                    setup = setup,
                    intent =
                        DirectCompanionTurn.Intent(
                            idempotencyKey = "send.self-development.canonical-first",
                            content = "Canonical first request",
                        ),
                    hooks =
                        DirectCompanionTurn.Hooks(
                            transport = transport,
                        ),
                )

            val failed =
                assertFailsWith<DirectCompanionTurn.TurnException> {
                    DirectCompanionTurn.performWithHooks(
                        setup = setup,
                        intent =
                            DirectCompanionTurn.Intent(
                                idempotencyKey = "send.self-development.failed-followup",
                                content = "Failed follow-up request",
                            ),
                        hooks =
                            DirectCompanionTurn.Hooks(
                                transport = transport,
                            ),
                    )
                }

            val failure =
                assertNotNull(
                    failed.failureReceipt,
                )
            assertEquals(
                first.currentConversationHeadSha256,
                failure.lastValidConversationHeadSha256OrNull,
            )
            assertTrue(
                failure.resumePossible,
            )
            assertEquals(
                "RESUME_LAST_VALID_HEAD_THEN_FORM_NEW_SEND_ATTEMPT",
                failure.exactNextSafeRoute,
            )

            val recovered =
                DirectCompanionTurn.performWithHooks(
                    setup = setup,
                    intent =
                        DirectCompanionTurn.Intent(
                            idempotencyKey = "send.self-development.fresh-recovery",
                            content = "Fresh recovery request",
                        ),
                    hooks =
                        DirectCompanionTurn.Hooks(
                            transport = transport,
                        ),
                )

            assertEquals(
                "Recovery continued from the last valid head",
                recovered.content,
            )
            assertEquals(
                "IMMEDIATE_PRIOR_COMPLETED_TURN",
                recovered.promptContextReceipt.contextMode,
            )
            assertEquals(
                3,
                calls,
            )

            val current =
                DirectConversationStore(
                    home,
                )
                    .current()
            assertEquals(
                4,
                current.canonicalEvents.size,
            )
            assertEquals(
                1,
                current.abandonedAttempts.size,
            )
            assertEquals(
                "Failed follow-up request",
                current.abandonedAttempts
                    .single()
                    .requestEvent
                    .content,
            )
        } finally {
            home.parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun completedDuplicateRequiresTheExactSameBrowserContext() {
        val home =
            productHome()
        var calls =
            0
        val transport =
            DirectCompanionConnection.Transport {
                calls +=
                    1
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
                                "content": "Browser-aware response"
                              }
                            }
                          ]
                        }
                        """.trimIndent(),
                )
            }
        val browserContext =
            DirectPromptContext.BrowserContext(
                pageUrl = VexLifeProdBrowserContract.InitialPageUrl,
                title = "Vextreme Terrain",
                text = "Current terrain text",
                capture = VexLifeProdBrowserContract.CaptureRendered,
            )
        val intent =
            DirectCompanionTurn.Intent(
                idempotencyKey = "send.self-development.browser-exact",
                content = "What is on this page?",
                browserContext = browserContext,
            )

        try {
            val first =
                DirectCompanionTurn.performWithHooks(
                    setup = setup(
                        home,
                    ),
                    intent = intent,
                    hooks =
                        DirectCompanionTurn.Hooks(
                            transport = transport,
                        ),
                )
            val duplicate =
                DirectCompanionTurn.performWithHooks(
                    setup = setup(
                        home,
                    ),
                    intent = intent,
                    hooks =
                        DirectCompanionTurn.Hooks(
                            transport = transport,
                        ),
                )

            assertEquals(
                1,
                calls,
            )
            assertFalse(
                first.recoveredCompletedTurn,
            )
            assertTrue(
                duplicate.recoveredCompletedTurn,
            )
            assertTrue(
                duplicate.promptContextReceipt.browserContextIncluded,
            )
            assertEquals(
                first.promptContextReceipt.browserContextSha256,
                duplicate.promptContextReceipt.browserContextSha256,
            )

            assertFailsWith<IllegalArgumentException> {
                DirectCompanionTurn.performWithHooks(
                    setup = setup(
                        home,
                    ),
                    intent =
                        intent.copy(
                            browserContext =
                                browserContext.copy(
                                    text = "Different terrain text",
                                ),
                        ),
                    hooks =
                        DirectCompanionTurn.Hooks(
                            transport = transport,
                        ),
                )
            }
            assertEquals(
                1,
                calls,
            )
        } finally {
            home.parent
                .toFile()
                .deleteRecursively()
        }
    }


    @Test
    fun explicitHumanReasoningIsEphemeralAndNeverRecoveredFromDurableDuplicate() {
        val home =
            productHome()
        var providerCalls =
            0
        var providerRequestBody: String? =
            null
        val reasoning =
            "ephemeral human-visible reasoning"
        val transport =
            DirectCompanionConnection.Transport {
                call ->
                providerCalls +=
                    1
                providerRequestBody =
                    call.body
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
                                "content": "Reasoned response",
                                "reasoning": "$reasoning"
                              }
                            }
                          ]
                        }
                        """.trimIndent(),
                )
            }
        val intent =
            DirectCompanionTurn.Intent(
                idempotencyKey =
                    "send.self-development.viewable-thoughts.001",
                content =
                    "Reason about this",
            )

        try {
            val first =
                DirectCompanionTurn.performWithHooks(
                    setup =
                        setup(
                            home,
                        ),
                    intent = intent,
                    hooks =
                        DirectCompanionTurn.Hooks(
                            transport = transport,
                            now = {
                                Instant.parse(
                                    "2026-11-09T18:30:00Z",
                                )
                            },
                        ),
                )

            val providerRequest =
                Json
                    .parseToJsonElement(
                        requireNotNull(
                            providerRequestBody,
                        ),
                    )
                    .jsonObject
            assertEquals(
                "true",
                providerRequest[
                    "chat_template_kwargs"
                ]
                    ?.jsonObject
                    ?.get(
                        "enable_thinking",
                    )
                    ?.jsonPrimitive
                    ?.content,
            )

            assertEquals(
                reasoning,
                first.humanReasoning,
            )
            assertFalse(
                first.recoveredCompletedTurn,
            )
            assertTrue(
                first.runtimeObservation.reasoningTrace.present,
            )
            assertFalse(
                first.runtimeObservation.reasoningTrace.rawPersisted,
            )

            Files.walk(
                home,
            )
                .use {
                    paths ->
                    paths
                        .filter(
                            Files::isRegularFile,
                        )
                        .forEach {
                            path ->
                            val bytes =
                                Files.readAllBytes(
                                    path,
                                )
                            val text =
                                bytes.toString(
                                    Charsets.UTF_8,
                                )
                            assertFalse(
                                text.contains(
                                    reasoning,
                                ),
                                "raw human reasoning must not persist at $path",
                            )
                        }
                }

            val duplicate =
                DirectCompanionTurn.performWithHooks(
                    setup =
                        setup(
                            home,
                        ),
                    intent = intent,
                    hooks =
                        DirectCompanionTurn.Hooks(
                            transport = transport,
                            now = {
                                Instant.parse(
                                    "2026-11-09T18:31:00Z",
                                )
                            },
                        ),
                )

            assertTrue(
                duplicate.recoveredCompletedTurn,
            )
            assertFalse(
                duplicate.actualHttpCallPerformedNow,
            )
            assertNull(
                duplicate.humanReasoning,
            )
            assertEquals(
                1,
                providerCalls,
            )
        } finally {
            home.parent
                .toFile()
                .deleteRecursively()
        }
    }

}

// [VXG RealForever]
