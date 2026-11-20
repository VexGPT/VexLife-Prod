package vexlife.labs

import java.net.InetAddress
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import vexlife.capability.VexLifeCapabilityBridge
import vexlife.capability.reach.VexLifeWebReach
import vexlife.companion.DirectCompanionConnection
import vexlife.companion.DirectPromptContext
import vexlife.context.VexLifeContextProfile
import vexlife.conversation.CanonicalLivedContextFixture
import vexlife.conversation.DirectConversationStore
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.logging.VexLogger
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticProductIdentity
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeState
import vexlife.model.heretic.runtime.HereticExecutionTopology
import vexlife.service.VexLifeProdServiceContract

class VexLabsNativeTestTest {
    private val qwenLayerTypes =
        List(64) {
            index ->
            if (
                (index + 1) %
                4 ==
                0
            ) {
                "full_attention"
            } else {
                "linear_attention"
            }
        }

    private val qwenConfig =
        """
        {
          "architectures": ["Qwen3_5ForConditionalGeneration"],
          "model_type": "qwen3_5",
          "text_config": {
            "model_type": "qwen3_5_text",
            "max_position_embeddings": 262144,
            "num_hidden_layers": 64,
            "num_attention_heads": 24,
            "num_key_value_heads": 4,
            "head_dim": 256,
            "full_attention_interval": 4,
            "layer_types": [${qwenLayerTypes.joinToString(",") { "\"$it\"" }}],
            "use_cache": true
          }
        }
        """
            .trimIndent()

    private fun runtimeEvidence(
        model: HereticModelCatalog.Entry =
            HereticModelCatalog.Models.Qwen4B,
    ): HereticModelConfiguration.QualifiedRuntimeEvidence {
        val identity =
            model.identity
        return HereticModelConfiguration.QualifiedRuntimeEvidence(
            runtimeReceiptRef =
                "receipt.vexlife-prod.vex-labs." +
                    model.handle +
                    ".test",
            runtimeState =
                HereticRuntimeState.Qualified,
            modelRef =
                identity.modelRef,
            modelProfileRef =
                identity.modelProfileRef,
            artifactRef =
                identity.artifactSetRef,
            artifactContentSetSha256 =
                identity.artifactContentSetSha256,
            runtimeAdapterRef =
                identity.runtimeAdapterRef,
            runtimeClass =
                identity.runtimeClass,
            endpoint =
                identity.endpoint,
            requestModel =
                identity.requestModel,
            qualification =
                HereticRuntimeQualification.qualified(),
        )
    }

    private fun nativeToolReply(
        functionName: String,
        arguments: String,
        reasoning: String? = null,
    ) =
        DirectCompanionConnection.HttpResult(
            status = 200,
            body =
                buildString {
                    append(
                        """{"model":"default_model","choices":[{"finish_reason":"tool_calls","message":{"role":"assistant","content":null""",
                    )
                    reasoning?.let {
                        append(
                            ""","reasoning_content":""",
                        )
                        append(
                            Json.encodeToString(
                                kotlinx.serialization.json.JsonPrimitive.serializer(),
                                kotlinx.serialization.json.JsonPrimitive(it),
                            ),
                        )
                    }
                    append(
                        ""","tool_calls":[{"id":"call_lab","type":"function","function":{"name":""",
                    )
                    append(
                        Json.encodeToString(
                            kotlinx.serialization.json.JsonPrimitive.serializer(),
                            kotlinx.serialization.json.JsonPrimitive(functionName),
                        ),
                    )
                    append(
                        ""","arguments":""",
                    )
                    append(
                        Json.encodeToString(
                            kotlinx.serialization.json.JsonPrimitive.serializer(),
                            kotlinx.serialization.json.JsonPrimitive(arguments),
                        ),
                    )
                    append(
                        """}}]}}]}""",
                    )
                },
        )

    private val publicResolver =
        VexLifeWebReach.Resolver {
            listOf(
                InetAddress.getByAddress(
                    it,
                    byteArrayOf(
                        93,
                        184.toByte(),
                        215.toByte(),
                        14,
                    ),
                ),
            )
        }

    private val labWebPage =
        VexLifeWebReach.Transport {
            request,
            _,
            ->
            assertEquals(
                "https://example.com/lab-evidence",
                request.uri.toString(),
            )
            VexLifeWebReach.Response(
                status = 200,
                headers =
                    mapOf(
                        "content-type" to
                            listOf(
                                "text/plain",
                            ),
                    ),
                body =
                    "LAB-WEB-EVIDENCE: source owner returned the current page."
                        .toByteArray(
                            StandardCharsets.UTF_8,
                        ),
                truncated = false,
            )
        }

    private fun activeProfile(): VexLifeContextProfile.Effective =
        VexLifeContextProfile.Effective(
            profileRef =
                VexLifeContextProfile.ProductDefaultRef,
            revision = 0,
            definition =
                VexLifeContextProfile.Definition(
                    name =
                        "VexLife Default",
                ),
            basedOnProductFoundation =
                VexLifeContextProfile.ProductFoundationIdentity(
                    version = null,
                    bodySha256 = null,
                ),
            fingerprint =
                "c".repeat(
                    64,
                ),
            formedAt = null,
            productDefault = true,
            productFoundationChangedSinceSave = false,
        )

    @Test
    fun qwen27ProjectionExposesThinkingAndLowMediumXHighBeforeDeeperFields() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-vex-labs-qwen27-options-",
            )
        val home =
            parent.resolve(
                "VexLife-Prod",
            )
        CanonicalVexLifeHomeFixture.form(
            home,
        )
        var providerRequestBody: String? =
            null
        val topology =
            HereticExecutionTopology.project(
                model =
                    HereticModelCatalog.Models.Qwen27B,
                exactModelConfig =
                    qwenConfig,
                modelConfigSourceRef =
                    "artifact.test.qwen27b.config",
            )

        try {
            VexLabsNativeTest(
                productHome = home,
                currentRuntimeEvidence = {
                    runtimeEvidence(
                        HereticModelCatalog.Models.Qwen27B,
                    )
                },
                activeContextProfile =
                    ::activeProfile,
                personContext = {
                    DirectPromptContext.PersonContext(
                        replyLanguage = "en",
                        replyLanguageName = "English",
                        privateConversations =
                            emptyList(),
                    )
                },
                currentProductConversationHead = {
                    null
                },
                transport =
                    DirectCompanionConnection.Transport {
                        call ->
                        providerRequestBody =
                            call.body
                        DirectCompanionConnection.HttpResult(
                            status = 200,
                            body =
                                """{"model":"default_model","choices":[{"message":{"role":"assistant","content":"Medium depth used."}}]}""",
                        )
                    },
                executionTopology = {
                    topology
                },
            )
                .use {
                    labs ->
                    val projection =
                        labs.projection()
                    assertTrue(
                        projection.reasoningOptions.thinkingToggleSupported,
                    )
                    assertEquals(
                        listOf(
                            "low",
                            "medium",
                            "xhigh",
                        ),
                        projection.reasoningOptions.reasoningEffortOptions,
                    )
                    assertEquals(
                        "xhigh",
                        projection.reasoningOptions.defaultReasoningEffort,
                    )
                    assertEquals(
                        listOf(
                            "field.vexlife-prod.generation.thinking",
                            "field.vexlife-prod.generation.reasoning-effort",
                            "field.vexlife-prod.context-profile.input-token-budget",
                        ),
                        projection.fields
                            .take(
                                3,
                            )
                            .map {
                                it.fieldRef
                            },
                    )
                    assertEquals(
                        "EDITABLE_LAB_ONLY",
                        projection.fields
                            .first {
                                it.fieldRef ==
                                    "field.vexlife-prod.generation.reasoning-effort"
                            }
                            .labEditability,
                    )

                    val medium =
                        labs.send(
                            VexLifeProdServiceContract.VexLabsSendRequest(
                                content =
                                    "Use medium reasoning depth.",
                                inputTokenBudget =
                                    64_000,
                                maximumMemoryTokens =
                                    0,
                                memoryMode =
                                    "NONE",
                                thinkingEnabled =
                                    true,
                                reasoningEffort =
                                    "medium",
                            ),
                        )
                    assertEquals(
                        "LAB_TURN_COMPLETED",
                        medium.state,
                    )
                    assertEquals(
                        "medium",
                        medium.reasoningEffort,
                    )
                    assertTrue(
                        requireNotNull(
                            providerRequestBody,
                        )
                            .contains(
                                """"reasoning_effort":"medium"""",
                            ),
                    )
                    assertTrue(
                        requireNotNull(
                            providerRequestBody,
                        )
                            .contains(
                                """"enable_thinking":true""",
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
    fun nativeLabUsesScratchConversationAndTemporaryContextWithoutMutatingRealConversation() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-vex-labs-test-",
            )
        val home =
            parent.resolve(
                "VexLife-Prod",
            )
        CanonicalVexLifeHomeFixture.form(
            home,
        )
        val conversation =
            DirectConversationStore(
                home,
            )

        try {
            conversation.withWriterLease {
                val request =
                    beginRequest(
                        turnRef =
                            "turn.vex-labs.seed",
                        requestMessageRef =
                            "message.vex-labs.human.seed",
                        content =
                            "Keep this exact seed conversation available to the Lab.",
                    )
                val response =
                    appendResponse(
                        request = request,
                        responseMessageRef =
                            "message.vex-labs.companion.seed",
                        content =
                            "The exact seed is present.",
                        model =
                            HereticProductIdentity.RequestModel,
                        endpointOrigin =
                            "http://127.0.0.1:8765",
                        runtimeObservationRef =
                            "observation.vex-labs.seed",
                        invocationEvidenceRef =
                            "evidence.vex-labs.seed",
                    )
                val context =
                    CanonicalLivedContextFixture.write(
                        productHome = home,
                        request = request,
                        response = response,
                        formedAt =
                            Instant.parse(
                                "2026-11-19T08:00:00Z",
                            ),
                    )
                commitCompletedTurn(
                    request = request,
                    response = response,
                    contextSha256 =
                        context.sha256,
                )
            }

            val realHeadBefore =
                conversation.current()
                    .head
                    ?.conversationHeadSha256
            val requestBodies =
                mutableListOf<String>()
            val topology =
                HereticExecutionTopology.project(
                    model =
                        HereticModelCatalog.Models.Qwen4B,
                    exactModelConfig =
                        qwenConfig,
                    modelConfigSourceRef =
                        "artifact.test.qwen4b.config",
                )
            VexLabsNativeTest(
                productHome = home,
                currentRuntimeEvidence =
                    ::runtimeEvidence,
                activeContextProfile =
                    ::activeProfile,
                personContext = {
                    DirectPromptContext.PersonContext(
                        replyLanguage = "en",
                        replyLanguageName = "English",
                        privateConversations =
                            emptyList(),
                    )
                },
                currentProductConversationHead = {
                    conversation.current()
                        .head
                        ?.conversationHeadSha256
                },
                transport =
                    DirectCompanionConnection.Transport {
                        call ->
                        requestBodies +=
                            call.body
                        val reply =
                            if (
                                requestBodies.size ==
                                    1
                            ) {
                                "This answer exists only in Vex Labs."
                            } else {
                                "The second Lab turn reused the shared Conversation engine."
                            }
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
                                        "content": "$reply",
                                        "reasoning": "I can use the scratch Conversation through DirectCompanionTurn."
                                      }
                                    }
                                  ]
                                }
                                """.trimIndent(),
                        )
                    },
                executionTopology = {
                    topology
                },
            )
                .use {
                    labs ->
                    val before =
                        labs.projection()
                    assertEquals(
                        39,
                        before.fields.size,
                    )
                    assertEquals(
                        listOf(
                            "field.vexlife-prod.generation.thinking",
                            "field.vexlife-prod.generation.reasoning-effort",
                            "field.vexlife-prod.context-profile.input-token-budget",
                        ),
                        before.fields
                            .take(
                                3,
                            )
                            .map {
                                it.fieldRef
                            },
                        "VexContext should put the controls people are most likely to change first",
                    )
                    assertEquals(
                        "EDITABLE_LAB_ONLY",
                        before.fields
                            .first {
                                it.fieldRef ==
                                    "field.vexlife-prod.generation.thinking"
                            }
                            .labEditability,
                    )
                    assertEquals(
                        "HELD",
                        before.fields
                            .first {
                                it.fieldRef ==
                                    "field.vexlife-prod.generation.reasoning-effort"
                            }
                            .labEditability,
                    )
                    assertTrue(
                        before.fields.any {
                            it.fieldRef ==
                                "field.vexlife-prod.context-profile.input-token-budget" &&
                                it.labEditability ==
                                "EDITABLE_LAB_ONLY"
                        },
                    )
                    assertTrue(
                        before.reasoningOptions.thinkingToggleSupported,
                    )
                    assertTrue(
                        before.reasoningOptions.reasoningEffortOptions.isEmpty(),
                    )
                    assertEquals(
                        null,
                        before.reasoningOptions.defaultReasoningEffort,
                    )
                    assertEquals(
                        2,
                        before.scratchConversationMessageCount,
                    )

                    val result =
                        labs.send(
                            VexLifeProdServiceContract.VexLabsSendRequest(
                                content =
                                    "What would you say with a smaller active context?",
                                inputTokenBudget =
                                    64_000,
                                maximumMemoryTokens =
                                    0,
                                memoryMode =
                                    "NONE",
                            ),
                        )

                    assertEquals(
                        "LAB_TURN_COMPLETED",
                        result.state,
                    )
                    assertEquals(
                        "This answer exists only in Vex Labs.",
                        result.reply,
                    )
                    assertFalse(
                        result.realConversationMutationPerformed,
                    )
                    assertFalse(
                        result.realMemoryMutationPerformed,
                    )
                    assertTrue(
                        result.labConversationMutationPerformed,
                    )
                    assertFalse(
                        result.memoryIncluded,
                    )
                    assertEquals(
                        4,
                        result.projection.scratchConversationMessageCount,
                    )
                    assertEquals(
                        1,
                        requestBodies.size,
                    )
                    assertTrue(
                        requestBodies.first().contains(
                            "Keep this exact seed conversation available to the Lab.",
                        ),
                    )
                    assertTrue(
                        requestBodies.first().contains(
                            "What would you say with a smaller active context?",
                        ),
                    )
                    assertTrue(
                        requestBodies.first().contains(
                            """"enable_thinking":true""",
                        ),
                    )
                    assertFalse(
                        requestBodies.first().contains(
                            """"reasoning_effort":""",
                        ),
                        "the pinned 4B template has no qualified reasoning-depth tier",
                    )

                    val second =
                        labs.send(
                            VexLifeProdServiceContract.VexLabsSendRequest(
                                content =
                                    "And what about the next Lab turn?",
                                inputTokenBudget =
                                    64_000,
                                maximumMemoryTokens =
                                    0,
                                memoryMode =
                                    "NONE",
                                thinkingEnabled =
                                    false,
                            ),
                        )
                    assertEquals(
                        "The second Lab turn reused the shared Conversation engine.",
                        second.reply,
                    )
                    assertEquals(
                        6,
                        second.projection.scratchConversationMessageCount,
                    )
                    assertEquals(
                        2,
                        requestBodies.size,
                    )
                    assertTrue(
                        requestBodies.last().contains(
                            """"enable_thinking":false""",
                        ),
                        "the VexContext Thinking control must change the next 4B Lab provider request",
                    )
                    assertTrue(
                        requestBodies.last().contains(
                            "This answer exists only in Vex Labs.",
                        ),
                        "the second Lab request should carry the first Lab response through the shared Conversation component",
                    )
                    assertTrue(
                        requestBodies.last().contains(
                            DirectPromptContext.HistoricalReasoningEnvelopeHeader,
                        ),
                        "the second 4B Lab request should carry the first Lab reasoning through the qualified provider-only assistant-history envelope",
                    )
                    assertTrue(
                        requestBodies.last().contains(
                            "I can use the scratch Conversation through DirectCompanionTurn.",
                        ),
                    )
                    assertFalse(
                        requestBodies.last().contains(
                            """"reasoning_content":"I can use the scratch Conversation through DirectCompanionTurn."""",
                        ),
                        "the exact 4B template strips historical reasoning_content before a later human query",
                    )
                    assertTrue(
                        requestBodies.last().contains(
                            "And what about the next Lab turn?",
                        ),
                    )

                    assertEquals(
                        realHeadBefore,
                        conversation.current()
                            .head
                            ?.conversationHeadSha256,
                    )

                    val cleared =
                        labs.control(
                            VexLifeProdServiceContract.VexLabsControlRequest(
                                controlRef =
                                    VexLabsNativeTest.ControlClearConversation,
                            ),
                        )
                    assertEquals(
                        0,
                        cleared.projection.scratchConversationMessageCount,
                    )
                    assertTrue(
                        cleared.projection.exchanges.isEmpty(),
                    )

                    val reloaded =
                        labs.control(
                            VexLifeProdServiceContract.VexLabsControlRequest(
                                controlRef =
                                    VexLabsNativeTest.ControlReloadCurrentConversation,
                            ),
                        )
                    assertEquals(
                        2,
                        reloaded.projection.scratchConversationMessageCount,
                    )
                    assertEquals(
                        realHeadBefore,
                        reloaded.projection.seedConversationHeadSha256,
                    )
                }
        } finally {
            parent.toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun lowContextBudgetIsHeldWithPromptOwnedMinimumInsteadOfCallingTheModel() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-vex-labs-admission-test-",
            )
        val home =
            parent.resolve(
                "VexLife-Prod",
            )
        CanonicalVexLifeHomeFixture.form(
            home,
        )
        val conversation =
            DirectConversationStore(
                home,
            )
        val realHeadBefore =
            conversation.current()
                .head
                ?.conversationHeadSha256
        var providerCalls =
            0
        val topology =
            HereticExecutionTopology.project(
                model =
                    HereticModelCatalog.Models.Qwen4B,
                exactModelConfig =
                    qwenConfig,
                modelConfigSourceRef =
                    "artifact.test.qwen4b.config",
            )

        try {
            VexLabsNativeTest(
                productHome = home,
                currentRuntimeEvidence =
                    ::runtimeEvidence,
                activeContextProfile =
                    ::activeProfile,
                personContext = {
                    DirectPromptContext.PersonContext(
                        replyLanguage = "en",
                        replyLanguageName = "English",
                        privateConversations =
                            emptyList(),
                    )
                },
                currentProductConversationHead = {
                    conversation.current()
                        .head
                        ?.conversationHeadSha256
                },
                transport =
                    DirectCompanionConnection.Transport {
                        providerCalls +=
                            1
                        error(
                            "provider must not be called when the Prompt owner holds admission",
                        )
                    },
                executionTopology = {
                    topology
                },
            )
                .use {
                    labs ->
                    lateinit var held: VexLifeProdServiceContract.VexLabsSendResult
                    VexLogger.capturing {
                        records ->
                        held =
                            labs.send(
                                VexLifeProdServiceContract.VexLabsSendRequest(
                                    content =
                                        "Can this tiny context fit one real Lab turn?",
                                    inputTokenBudget =
                                        1,
                                    maximumMemoryTokens =
                                        0,
                                    memoryMode =
                                        "NONE",
                                ),
                            )
                        val promptHeld =
                            records.single {
                                it.fact ==
                                    VexLogger.Fact.PromptAdmissionHeld.wire
                            }
                        assertEquals(
                            "prompt",
                            promptHeld.owner,
                        )
                        assertEquals(
                            "1",
                            promptHeld.refs["requestedInputTokens"],
                        )
                        assertTrue(
                            requireNotNull(
                                promptHeld.refs["minimumInputTokens"],
                            )
                                .toInt() >
                                1,
                        )
                        assertEquals(
                            null,
                            promptHeld.detail,
                        )
                    }

                    assertEquals(
                        0,
                        providerCalls,
                    )
                    assertEquals(
                        "HELD_CONTEXT_BUDGET",
                        held.state,
                    )
                    assertEquals(
                        null,
                        held.reply,
                    )
                    assertTrue(
                        requireNotNull(
                            held.admission,
                        )
                            .minimumInputTokenBudget >
                            held.promptInputTokenBudget,
                    )
                    assertEquals(
                        held.admission,
                        held.projection.lastAdmission,
                    )
                    assertEquals(
                        realHeadBefore,
                        conversation.current()
                            .head
                            ?.conversationHeadSha256,
                    )
                    assertTrue(
                        held.projection.fields.any {
                            it.fieldRef ==
                                "field.vexlife-prod.context-profile.input-token-budget" &&
                                it.labObservedMinimumValue ==
                                held.admission
                                    ?.minimumInputTokenBudget
                                    ?.toString()
                        },
                    )
                }
        } finally {
            parent.toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun lowContextDistinguishesMandatoryCurrentStateFromExactImmediatePriorFloor() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-vex-labs-prior-floor-test-",
            )
        val home =
            parent.resolve(
                "VexLife-Prod",
            )
        CanonicalVexLifeHomeFixture.form(
            home,
        )
        val conversation =
            DirectConversationStore(
                home,
            )

        try {
            conversation.withWriterLease {
                val request =
                    beginRequest(
                        turnRef =
                            "turn.vex-labs.large-seed",
                        requestMessageRef =
                            "message.vex-labs.human.large-seed",
                        content =
                            "x".repeat(
                                6_000,
                            ),
                    )
                val response =
                    appendResponse(
                        request = request,
                        responseMessageRef =
                            "message.vex-labs.companion.large-seed",
                        content =
                            "y".repeat(
                                6_000,
                            ),
                        model =
                            HereticProductIdentity.RequestModel,
                        endpointOrigin =
                            "http://127.0.0.1:8765",
                        runtimeObservationRef =
                            "observation.vex-labs.large-seed",
                        invocationEvidenceRef =
                            "evidence.vex-labs.large-seed",
                    )
                val context =
                    CanonicalLivedContextFixture.write(
                        productHome = home,
                        request = request,
                        response = response,
                        formedAt =
                            Instant.parse(
                                "2026-11-19T09:00:00Z",
                            ),
                    )
                commitCompletedTurn(
                    request = request,
                    response = response,
                    contextSha256 =
                        context.sha256,
                )
            }

            val topology =
                HereticExecutionTopology.project(
                    model =
                        HereticModelCatalog.Models.Qwen4B,
                    exactModelConfig =
                        qwenConfig,
                    modelConfigSourceRef =
                        "artifact.test.qwen4b.config",
                )
            var providerCalls =
                0

            VexLabsNativeTest(
                productHome = home,
                currentRuntimeEvidence =
                    ::runtimeEvidence,
                activeContextProfile =
                    ::activeProfile,
                personContext = {
                    DirectPromptContext.PersonContext(
                        replyLanguage = "en",
                        replyLanguageName = "English",
                        privateConversations =
                            emptyList(),
                    )
                },
                currentProductConversationHead = {
                    conversation.current()
                        .head
                        ?.conversationHeadSha256
                },
                transport =
                    DirectCompanionConnection.Transport {
                        providerCalls +=
                            1
                        error(
                            "provider must not be called while the exact prior turn is outside the Lab budget",
                        )
                    },
                executionTopology = {
                    topology
                },
            )
                .use {
                    labs ->
                    var requested =
                        1
                    var held =
                        labs.send(
                            VexLifeProdServiceContract.VexLabsSendRequest(
                                content =
                                    "Find the exact admission floor.",
                                inputTokenBudget =
                                    requested,
                                maximumMemoryTokens =
                                    0,
                                memoryMode =
                                    "NONE",
                            ),
                        )
                    assertEquals(
                        "CURRENT_TURN_FIXED_CONTEXT",
                        requireNotNull(
                            held.admission,
                        )
                            .reason,
                    )

                    var admissionSteps =
                        0
                    while (
                        held.admission
                            ?.reason !=
                            "IMMEDIATE_PRIOR_REQUIRED" &&
                        admissionSteps <
                            16
                    ) {
                        requested =
                            requireNotNull(
                                held.admission,
                            )
                                .minimumInputTokenBudget
                        held =
                            labs.send(
                                VexLifeProdServiceContract.VexLabsSendRequest(
                                    content =
                                        "Find the exact admission floor.",
                                    inputTokenBudget =
                                        requested,
                                    maximumMemoryTokens =
                                        0,
                                    memoryMode =
                                        "NONE",
                                ),
                            )
                        assertEquals(
                            "HELD_CONTEXT_BUDGET",
                            held.state,
                        )
                        admissionSteps +=
                            1
                    }

                    val priorAdmission =
                        requireNotNull(
                            held.admission,
                        )
                    assertEquals(
                        "IMMEDIATE_PRIOR_REQUIRED",
                        priorAdmission.reason,
                    )
                    assertTrue(
                        priorAdmission.immediatePriorTokens >
                            0,
                    )
                    assertTrue(
                        priorAdmission.minimumInputTokenBudget >
                            priorAdmission.fixedContextTokens,
                    )
                    assertEquals(
                        0,
                        providerCalls,
                    )
                }
        } finally {
            parent.toFile()
                .deleteRecursively()
        }
    }
}

// [VXG RealForever]
