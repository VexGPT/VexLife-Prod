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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import vexlife.browser.VexLifeProdBrowserContract
import vexlife.continuity.VexLifeWorkingContinuityOwner
import vexlife.conversation.CanonicalLivedContextFixture
import vexlife.conversation.DirectConversationStore
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.logging.VexLogger
import vexlife.local.VexLifeProdLocalSetup
import vexlife.memory.VexLifeMemoryContextFixture
import vexlife.memory.VexLifeMemoryContract
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticModelCatalog
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
        model: HereticModelCatalog.Entry = HereticModelCatalog.Models.Qwen27B,
    ): VexLifeProdLocalSetup.Result {
        val identity =
            model.identity
        return VexLifeProdLocalSetup.Result(
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
                    modelRef = identity.modelRef,
                    modelProfileRef = identity.modelProfileRef,
                    artifactRef = identity.artifactSetRef,
                    artifactContentSetSha256 = identity.artifactContentSetSha256,
                    runtimeAdapterRef = identity.runtimeAdapterRef,
                    runtimeClass = identity.runtimeClass,
                    endpoint = identity.endpoint,
                    requestModel = identity.requestModel,
                    qualification = HereticRuntimeQualification.qualified(),
                ),
            endpoint = identity.endpoint,
            requestModel = identity.requestModel,
        )
    }

    @Test
    fun held4BReachKeepsExactSourceReturnWithoutAdvertisingGenericCapabilityBridge() {
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
                        "No outside-source capability reach is admitted in this turn.",
                    ),
                )
                assertFalse(
                    call.body.contains(
                        "VEXLIFE_CAPABILITY_REQUEST/v1",
                    ),
                )
                assertTrue(
                    call.body.contains(
                        DirectConversationSourceReturn.ProtocolHeader,
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
                                "content": "4B text-control response"
                              }
                            }
                          ]
                        }
                        """.trimIndent(),
                )
            }

        try {
            val result =
                DirectCompanionTurn.performWithHooks(
                    setup =
                        setup(
                            home,
                            HereticModelCatalog.Models.Qwen4B,
                        ),
                    intent =
                        DirectCompanionTurn.Intent(
                            idempotencyKey =
                                "send.self-development.text-control-initial-boundary.001",
                            content =
                                "Use the current text-control representation.",
                        ),
                    hooks =
                        DirectCompanionTurn.Hooks(
                            transport = transport,
                            now = {
                                Instant.parse(
                                    "2026-11-19T16:00:00Z",
                                )
                            },
                        ),
                )

            assertEquals(
                1,
                calls,
            )
            assertEquals(
                "TURN_COMPLETED",
                result.state,
            )
            assertEquals(
                "4B text-control response",
                result.content,
            )
            assertEquals(
                0,
                result.promptContextReceipt.capabilityExchangeAllowanceTokens,
            )
            assertTrue(
                result.promptContextReceipt.sourceReturnProtocolEnabled,
            )
            val frame =
                requireNotNull(
                    result.promptContextReceipt.capabilityFrameSnapshot,
                )
            assertFalse(
                "capability.vexlife.continuity.revisit" in
                    frame.availableCapabilityRefs,
            )
            assertFalse(
                "capability.vexlife.web.read" in
                    frame.availableCapabilityRefs,
            )
        } finally {
            home.parent
                .toFile()
                .deleteRecursively()
        }
    }

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
                DirectPromptContext.DirectTurnInputTokenBudget - 8_000
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
                            assertFalse(
                                call.body.contains(
                                    "VEXLIFE_CONVERSATION_SOURCE_RETURN_PROTOCOL/v1",
                                ),
                            )
                            assertTrue(
                                call.body.contains(
                                    "\"name\":\"conversation_return\"",
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
                                "reasoning must reach the human before native tool selection decides the source-return route",
                            )
                            onLine(
                                """data: {"model":"default_model","choices":[{"delta":{"role":"assistant","tool_calls":[{"index":0,"id":"call_source","type":"function","function":{"name":"conversation_return","arguments":"{\"queryTerms\":[\"lantern-garden\"],\"speakerScope\":\"ANY\"}"}}]}}]}""",
                            )
                            onLine(
                                "",
                            )
                            onLine(
                                """data: {"model":"default_model","choices":[{"finish_reason":"tool_calls","delta":{}}]}""",
                            )
                            onLine(
                                "",
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
                            assertTrue(
                                call.body.contains(
                                    "\"name\":\"conversation_return\"",
                                ),
                                "native tool declarations stay stable; returned-source context and the turn guard own one-return-only truth",
                            )
                            val secondMessages =
                                Json
                                    .parseToJsonElement(
                                        call.body,
                                    )
                                    .jsonObject
                                    .getValue(
                                        "messages",
                                    )
                                    .jsonArray
                            val sourceCallIndex =
                                secondMessages.indexOfFirst {
                                    message ->
                                    val value =
                                        message.jsonObject
                                    value["role"]
                                        ?.jsonPrimitive
                                        ?.content ==
                                        "assistant" &&
                                        value["tool_calls"]
                                            ?.jsonArray
                                            ?.any {
                                                tool ->
                                                tool.jsonObject
                                                    .getValue(
                                                        "function",
                                                    )
                                                    .jsonObject
                                                    .getValue(
                                                        "name",
                                                    )
                                                    .jsonPrimitive
                                                    .content ==
                                                    "conversation_return"
                                            } ==
                                        true
                                }
                            val currentRequestIndex =
                                secondMessages.indexOfFirst {
                                    message ->
                                    val value =
                                        message.jsonObject
                                    value[
                                        "role"
                                    ]
                                        ?.jsonPrimitive
                                        ?.content ==
                                        "user" &&
                                        value[
                                            "content"
                                        ]
                                            ?.jsonPrimitive
                                            ?.content ==
                                        "What was the exact older lantern-garden detail?"
                                }
                            assertTrue(
                                currentRequestIndex >=
                                    0 &&
                                    sourceCallIndex >
                                    currentRequestIndex,
                                "native source-return continuation must follow the current human request",
                            )
                            assertTrue(
                                sourceCallIndex >=
                                    0,
                                "the final native stage must retain the assistant conversation_return tool call",
                            )
                            assertEquals(
                                "initial source-return reasoning visible live",
                                secondMessages[
                                    sourceCallIndex
                                ]
                                    .jsonObject[
                                    "reasoning_content"
                                ]
                                    ?.jsonPrimitive
                                    ?.content,
                                "the final provider stage must retain the reasoning that led Vex to request Conversation source",
                            )
                            val sourceResult =
                                assertNotNull(
                                    secondMessages
                                        .getOrNull(
                                            sourceCallIndex +
                                                1,
                                        ),
                                )
                                    .jsonObject
                            assertEquals(
                                "tool",
                                sourceResult
                                    .getValue(
                                        "role",
                                    )
                                    .jsonPrimitive
                                    .content,
                            )
                            assertTrue(
                                sourceResult
                                    .getValue(
                                        "content",
                                    )
                                    .jsonPrimitive
                                    .content
                                    .contains(
                                        DirectConversationSourceReturn.ContextHeader,
                                    ),
                            )
                            assertTrue(
                                sourceResult
                                    .getValue(
                                        "content",
                                    )
                                    .jsonPrimitive
                                    .content
                                    .contains(
                                        "lantern-garden exact older source",
                                    ),
                            )
                            assertFalse(
                                call.body.contains(
                                    DirectConversationSourceReturn.RequestHeader,
                                ),
                                "the canonical internal Source Return control must be re-projected rather than exposed as provider text",
                            )
                            assertTrue(
                                call.body.contains(
                                    "Do not emit another source-return request in this human turn.",
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
    fun turnPolicyHoldIsNotMisreportedAsRepeatedSourceReturn() {
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
            lateinit var failure: DirectCompanionTurn.TurnException
            VexLogger.capturing { records ->
                failure =
                    assertFailsWith<DirectCompanionTurn.TurnException> {
                        DirectCompanionTurn.performWithHooks(
                            setup =
                                setup(
                                    home,
                                ),
                            intent =
                                DirectCompanionTurn.Intent(
                                    idempotencyKey =
                                        "send.self-development.source-return-policy-held.001",
                                    content =
                                        "Find an older detail if needed.",
                                ),
                            hooks =
                                DirectCompanionTurn.Hooks(
                                    transport = transport,
                                    turnPolicy =
                                        DirectCompanionTurn.TurnPolicy(
                                            sourceReturnAllowed = false,
                                        ),
                                ),
                        )
                    }

                val invocation =
                    records.single {
                        it.fact ==
                            VexLogger.Fact.ProviderInvocationRequested.wire
                    }
                assertEquals(
                    "initial",
                    invocation.refs["stage"],
                )
                assertEquals(
                    "held_by_turn_policy",
                    invocation.refs["sourceReturnRequests"],
                )
            }

            assertEquals(
                1,
                calls,
            )
            assertEquals(
                "SOURCE_RETURN_REQUEST_HELD_BY_TURN_POLICY",
                failure.failureCode,
            )
            assertEquals(
                "SOURCE_RETURN_REQUEST_HELD_BY_TURN_POLICY",
                assertNotNull(
                    failure.failureReceipt,
                )
                    .failureCode,
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
                        "Vex capability context",
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
            DirectPromptContext.DirectTurnInputTokenBudget -
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
            // The producer exchange fills most of the prompt, so not even one whole exchange fits
            // the overlap tail: only Vex's prior reply is kept, and the receipt says so.
            assertEquals(
                listOf(first.responseEventRef),
                second.promptContextReceipt.selectedConversationEventRefs,
            )
            assertEquals(
                DirectContinuityOverlapTail.FloorState.DEGRADED_ROOM,
                second.promptContextReceipt.overlapTail?.floorState,
            )
            assertEquals(
                true,
                second.promptContextReceipt.overlapTail?.responseBridgeOnly,
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
            DirectPromptContext.DirectTurnInputTokenBudget -
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
            assertEquals(
                listOf(first.responseEventRef),
                recovered.promptContextReceipt.selectedConversationEventRefs,
            )
            assertEquals(
                true,
                recovered.promptContextReceipt.overlapTail?.responseBridgeOnly,
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
            // The recovered turn was issued its own request-scoped successor Context; its prompt
            // receipt records which one.
            val recoveredContextRef =
                assertNotNull(recovered.promptContextReceipt.workingContinuityContextRef)
            assertTrue(failedContext.successorContextRef != recoveredContextRef)
            assertEquals(
                failedRequest.eventRef,
                failedContext.currentRequestEventBinding.eventRef,
            )

            // The recovered successor continues the rollover lineage: its commit advances the head
            // to its own request, which supersedes the checkpoint its Context was issued from, so
            // that Context is evidence now, no longer current.
            val afterLineage =
                assertNotNull(
                    owner.readCurrent(
                        lineageRef = firstHead.companionLineageRef,
                        threadRef = firstHead.threadRef,
                    )
                )
            assertEquals("ROLLOVER_COMMITTED", afterLineage.commitReceipt.disposition)
            assertEquals(
                recovered.requestEventRef,
                afterLineage.checkpoint.workingSet.cursorEventRef,
            )
            assertNull(
                owner.readSuccessorContext(
                    lineageRef = firstHead.companionLineageRef,
                    threadRef = firstHead.threadRef,
                    currentRequestEventBinding = recoveredBinding,
                )
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
    fun responseBoundReasoningContinuesIntoTheNextProviderTurnWithoutRewritingConversation() {
        val home =
            productHome()
        val providerRequestBodies =
            mutableListOf<String>()
        val reasoning =
            "response-bound reasoning continuation"
        val transport =
            DirectCompanionConnection.Transport {
                call ->
                providerRequestBodies +=
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
        val firstIntent =
            DirectCompanionTurn.Intent(
                idempotencyKey =
                    "send.self-development.reasoning-continuation.001",
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
                    intent = firstIntent,
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

            val firstProviderRequest =
                Json
                    .parseToJsonElement(
                        providerRequestBodies.single(),
                    )
                    .jsonObject
            assertEquals(
                "true",
                firstProviderRequest[
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
                "true",
                firstProviderRequest[
                    "chat_template_kwargs"
                ]
                    ?.jsonObject
                    ?.get(
                        "preserve_thinking",
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

            val durableConversation =
                DirectConversationStore(
                    home,
                )
                    .current()
                    .canonicalEvents
            assertTrue(
                durableConversation.none {
                    it.content.contains(
                        reasoning,
                    )
                },
                "provider reasoning must not rewrite canonical Conversation content",
            )

            val turnEvidence =
                assertNotNull(
                    DirectTurnContextStore(
                        home,
                    )
                        .read(
                            first.turnRef,
                        ),
                )
            assertEquals(
                reasoning,
                turnEvidence.reasoningContinuation,
            )
            assertTrue(
                turnEvidence.rawReasoningPersisted,
            )

            val duplicate =
                DirectCompanionTurn.performWithHooks(
                    setup =
                        setup(
                            home,
                        ),
                    intent = firstIntent,
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
                providerRequestBodies.size,
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
                                "send.self-development.reasoning-continuation.002",
                            content =
                                "Continue from what you already worked through.",
                        ),
                    hooks =
                        DirectCompanionTurn.Hooks(
                            transport = transport,
                            now = {
                                Instant.parse(
                                    "2026-11-09T18:32:00Z",
                                )
                            },
                        ),
                )
            assertFalse(
                second.recoveredCompletedTurn,
            )
            assertEquals(
                2,
                providerRequestBodies.size,
            )

            val secondMessages =
                Json
                    .parseToJsonElement(
                        providerRequestBodies.last(),
                    )
                    .jsonObject[
                    "messages"
                ]
                    ?.jsonArray
                    ?: error(
                        "second provider request has no messages",
                    )
            val continuedAssistant =
                secondMessages
                    .map {
                        it.jsonObject
                    }
                    .firstOrNull {
                        it["role"]
                            ?.jsonPrimitive
                            ?.content ==
                            "assistant" &&
                            it["content"]
                                ?.jsonPrimitive
                                ?.content ==
                            "Reasoned response"
                    }
                    ?: error(
                        "second provider request does not carry the prior assistant response",
                    )
            assertEquals(
                reasoning,
                continuedAssistant[
                    "reasoning_content"
                ]
                    ?.jsonPrimitive
                    ?.content,
            )
            assertTrue(
                second.promptContextReceipt.selectedConversationReasoningEventRefs.contains(
                    first.responseEventRef,
                ),
            )
            assertTrue(
                second.promptContextReceipt.selectedConversationReasoningEstimatedTokens >
                    0,
            )
        } finally {
            home.parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun qwen4BCarriesPriorReasoningInProviderOnlyAssistantEnvelopeAcrossHumanTurns() {
        val home =
            productHome()
        val requestBodies =
            mutableListOf<String>()
        val reasoning =
            "I already compared the two constraints and the second one controls the answer."
        var callNumber =
            0
        val transport =
            DirectCompanionConnection.Transport {
                call ->
                requestBodies +=
                    call.body
                callNumber +=
                    1
                val body =
                    if (
                        callNumber ==
                            1
                    ) {
                        """
                        {
                          "model":"default_model",
                          "choices":[
                            {
                              "message":{
                                "role":"assistant",
                                "content":"The second constraint controls the answer.",
                                "reasoning_content":__REASONING__
                              }
                            }
                          ]
                        }
                        """.trimIndent()
                            .replace(
                                "__REASONING__",
                                kotlinx.serialization.json.JsonPrimitive(
                                    reasoning,
                                )
                                    .toString(),
                            )
                    } else {
                        """
                        {
                          "model":"default_model",
                          "choices":[
                            {
                              "message":{
                                "role":"assistant",
                                "content":"I continued from the prior reasoning."
                              }
                            }
                          ]
                        }
                        """.trimIndent()
                    }
                DirectCompanionConnection.HttpResult(
                    status = 200,
                    body = body,
                )
            }

        try {
            val first =
                DirectCompanionTurn.performWithHooks(
                    setup =
                        setup(
                            home,
                            HereticModelCatalog.Models.Qwen4B,
                        ),
                    intent =
                        DirectCompanionTurn.Intent(
                            idempotencyKey =
                                "send.self-development.reasoning-envelope.4b.001",
                            content =
                                "Compare these two constraints and tell me which controls.",
                        ),
                    hooks =
                        DirectCompanionTurn.Hooks(
                            transport = transport,
                            now = {
                                Instant.parse(
                                    "2026-11-19T18:40:00Z",
                                )
                            },
                        ),
                )
            assertEquals(
                reasoning,
                first.humanReasoning,
            )
            assertEquals(
                reasoning,
                assertNotNull(
                    DirectTurnContextStore(
                        home,
                    )
                        .read(
                            first.turnRef,
                        ),
                )
                    .reasoningContinuation,
            )

            val second =
                DirectCompanionTurn.performWithHooks(
                    setup =
                        setup(
                            home,
                            HereticModelCatalog.Models.Qwen4B,
                        ),
                    intent =
                        DirectCompanionTurn.Intent(
                            idempotencyKey =
                                "send.self-development.reasoning-envelope.4b.002",
                            content =
                                "Continue from that reasoning without recomputing it.",
                        ),
                    hooks =
                        DirectCompanionTurn.Hooks(
                            transport = transport,
                            now = {
                                Instant.parse(
                                    "2026-11-19T18:41:00Z",
                                )
                            },
                        ),
                )

            assertEquals(
                2,
                requestBodies.size,
            )
            val secondBody =
                Json
                    .parseToJsonElement(
                        requestBodies.last(),
                    )
                    .jsonObject
            assertFalse(
                secondBody[
                    "chat_template_kwargs"
                ]
                    ?.jsonObject
                    ?.containsKey(
                        "preserve_thinking",
                    )
                    ?: false,
            )
            val messages =
                secondBody
                    .getValue(
                        "messages",
                    )
                    .jsonArray
            val priorAssistant =
                messages
                    .map {
                        it.jsonObject
                    }
                    .firstOrNull {
                        it[
                            "role"
                        ]
                            ?.jsonPrimitive
                            ?.content ==
                            "assistant" &&
                            it[
                                "content"
                            ]
                                ?.jsonPrimitive
                                ?.content
                                ?.startsWith(
                                    DirectPromptContext.HistoricalReasoningEnvelopeHeader,
                                ) ==
                            true
                    }
                    ?: error(
                        "4B second turn has no provider-only historical reasoning envelope",
                    )
            val providerContent =
                requireNotNull(
                    priorAssistant[
                        "content"
                    ]
                        ?.jsonPrimitive
                        ?.content,
                )
            assertTrue(
                providerContent.contains(
                    reasoning,
                ),
            )
            assertTrue(
                providerContent.contains(
                    "The second constraint controls the answer.",
                ),
            )
            assertFalse(
                priorAssistant.containsKey(
                    "reasoning_content",
                ),
                "4B historical reasoning must not rely on a reasoning_content field its exact template strips before a new human query",
            )
            assertTrue(
                second.promptContextReceipt.selectedConversationReasoningEventRefs.contains(
                    first.responseEventRef,
                ),
            )
            val canonicalFirstResponse =
                DirectConversationStore(
                    home,
                )
                    .current()
                    .canonicalEvents
                    .first {
                        it.eventRef ==
                            first.responseEventRef
                    }
            assertEquals(
                "The second constraint controls the answer.",
                canonicalFirstResponse.content,
            )
            assertFalse(
                canonicalFirstResponse.content.contains(
                    reasoning,
                ),
            )
        } finally {
            home.parent
                .toFile()
                .deleteRecursively()
        }
    }

}

// [VXG RealForever]
