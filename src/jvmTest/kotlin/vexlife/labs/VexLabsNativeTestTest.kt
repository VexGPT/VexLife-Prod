package vexlife.labs

import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vexlife.companion.DirectCompanionConnection
import vexlife.companion.DirectPromptContext
import vexlife.context.VexLifeContextProfile
import vexlife.conversation.CanonicalLivedContextFixture
import vexlife.conversation.DirectConversationStore
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.home.VexLifeProdHome
import vexlife.home.VexLifeProdHomeFiles
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

    private fun runtimeEvidence(): HereticModelConfiguration.QualifiedRuntimeEvidence =
        HereticModelConfiguration.QualifiedRuntimeEvidence(
            runtimeReceiptRef =
                "receipt.vexlife-prod.vex-labs.test",
            runtimeState =
                HereticRuntimeState.Qualified,
            modelRef =
                HereticProductIdentity.ModelRef,
            modelProfileRef =
                HereticProductIdentity.ModelProfileRef,
            artifactRef =
                HereticProductIdentity.ArtifactSetRef,
            artifactContentSetSha256 =
                HereticProductIdentity.ArtifactContentSetSha256,
            runtimeAdapterRef =
                HereticProductIdentity.RuntimeAdapterRef,
            runtimeClass =
                HereticProductIdentity.RuntimeClass,
            endpoint =
                HereticProductIdentity.Endpoint,
            requestModel =
                HereticProductIdentity.RequestModel,
            qualification =
                HereticRuntimeQualification.qualified(),
        )

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
                            "Historical regression PR488 context must stay outside a fresh Vex Labs conversation.",
                    )
                val response =
                    appendResponse(
                        request = request,
                        responseMessageRef =
                            "message.vex-labs.companion.seed",
                        content =
                            "Canonical Product history remains canonical Product history.",
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
                        38,
                        before.fields.size,
                    )
                    val profileBudget =
                        before.fields.single {
                            it.fieldRef ==
                                "field.vexlife-prod.context-profile.input-token-budget"
                        }
                    assertEquals(
                        "EDITABLE_LAB_ONLY",
                        profileBudget.labEditability,
                    )
                    assertEquals(
                        "workspace.label.execution-field-context-profile-input-token-budget",
                        profileBudget.label.key,
                    )
                    assertEquals(
                        "workspace.description.vex-labs-edit-context-profile-active",
                        profileBudget.labEditDetail.key,
                    )

                    val promptCache =
                        before.fields.single {
                            it.fieldRef ==
                                "field.vexlife-prod.runtime.prompt-cache-entries"
                        }
                    assertEquals(
                        "workspace.label.execution-field-runtime-prompt-cache-entries",
                        promptCache.label.key,
                    )
                    assertEquals(
                        "workspace.description.execution-field-runtime-prompt-cache-entries-increase",
                        promptCache.increaseConsequence?.key,
                    )
                    assertEquals(
                        0,
                        before.scratchConversationMessageCount,
                    )
                    assertEquals(
                        "EMPTY",
                        before.conversationPreset,
                    )
                    assertEquals(
                        "NONE",
                        before.memoryMode,
                    )
                    assertEquals(
                        null,
                        before.seedConversationHeadSha256,
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
                        2,
                        result.projection.scratchConversationMessageCount,
                    )
                    assertEquals(
                        1,
                        requestBodies.size,
                    )
                    assertFalse(
                        requestBodies.first().contains(
                            "Historical regression PR488 context must stay outside a fresh Vex Labs conversation.",
                        ),
                        "fresh Labs must not inherit canonical Conversation bytes by default",
                    )
                    assertTrue(
                        requestBodies.first().contains(
                            "priorCompletedExchanges=0",
                        ),
                        "fresh Labs must tell Vex that its disposable Conversation starts empty",
                    )
                    assertTrue(
                        requestBodies.first().contains(
                            "What would you say with a smaller active context?",
                        ),
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
                            ),
                        )
                    assertEquals(
                        "The second Lab turn reused the shared Conversation engine.",
                        second.reply,
                    )
                    assertEquals(
                        4,
                        second.projection.scratchConversationMessageCount,
                    )
                    assertEquals(
                        2,
                        requestBodies.size,
                    )
                    assertTrue(
                        requestBodies.last().contains(
                            "This answer exists only in Vex Labs.",
                        ),
                        "the second Lab request should carry the first Lab response through the shared Conversation component",
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
                        "CURRENT_SNAPSHOT",
                        reloaded.projection.conversationPreset,
                    )
                    assertEquals(
                        realHeadBefore,
                        reloaded.projection.seedConversationHeadSha256,
                    )

                    val restored =
                        labs.control(
                            VexLifeProdServiceContract.VexLabsControlRequest(
                                controlRef =
                                    VexLabsNativeTest.ControlRestoreDefaults,
                            ),
                        )
                    assertEquals(
                        "EMPTY",
                        restored.projection.conversationPreset,
                    )
                    assertEquals(
                        "NONE",
                        restored.projection.memoryMode,
                    )
                    assertEquals(
                        0,
                        restored.projection.scratchConversationMessageCount,
                    )
                    assertEquals(
                        null,
                        restored.projection.seedConversationHeadSha256,
                    )
                }
        } finally {
            parent.toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun canonicalHomeIdentityChangeReformsDisposableScratchWithoutImportingConversation() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-vex-labs-home-currentness-test-",
            )
        val home =
            parent.resolve(
                "VexLife-Prod",
            )
        val replacementHome =
            parent.resolve(
                "Replacement-VexLife-Prod",
            )
        CanonicalVexLifeHomeFixture.form(
            home,
        )
        CanonicalVexLifeHomeFixture.form(
            replacementHome,
        )
        val conversation =
            DirectConversationStore(
                home,
            )
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
            val originalHome =
                requireNotNull(
                    VexLifeProdHomeFiles.observeExisting(
                        home,
                    ),
                )
            val replacement =
                requireNotNull(
                    VexLifeProdHomeFiles.observeExisting(
                        replacementHome,
                    ),
                )
            assertTrue(
                originalHome.manifest.homeRef !=
                    replacement.manifest.homeRef,
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
                                        "content": "Disposable Lab reply."
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
                    assertEquals(
                        0,
                        labs.projection()
                            .scratchConversationMessageCount,
                    )
                    val completed =
                        labs.send(
                            VexLifeProdServiceContract.VexLabsSendRequest(
                                content =
                                    "Create one disposable scratch exchange.",
                                inputTokenBudget =
                                    64_000,
                                maximumMemoryTokens =
                                    0,
                                memoryMode =
                                    "NONE",
                            ),
                        )
                    assertEquals(
                        2,
                        completed.projection.scratchConversationMessageCount,
                    )
                    assertEquals(
                        1,
                        completed.projection.exchanges.size,
                    )

                    Files.copy(
                        replacementHome.resolve(
                            VexLifeProdHome.HomeManifestRelativePath,
                        ),
                        home.resolve(
                            VexLifeProdHome.HomeManifestRelativePath,
                        ),
                        StandardCopyOption.REPLACE_EXISTING,
                    )
                    Files.copy(
                        replacementHome.resolve(
                            "devices/${replacement.manifest.currentDeviceRef}.json",
                        ),
                        home.resolve(
                            "devices/${replacement.manifest.currentDeviceRef}.json",
                        ),
                        StandardCopyOption.REPLACE_EXISTING,
                    )
                    assertEquals(
                        replacement.manifest.homeRef,
                        requireNotNull(
                            VexLifeProdHomeFiles.observeExisting(
                                home,
                            ),
                        )
                            .manifest
                            .homeRef,
                    )

                    val realigned =
                        labs.projection()
                    assertEquals(
                        "EMPTY",
                        realigned.conversationPreset,
                    )
                    assertEquals(
                        0,
                        realigned.scratchConversationMessageCount,
                    )
                    assertEquals(
                        null,
                        realigned.seedConversationHeadSha256,
                    )
                    assertTrue(
                        realigned.exchanges.isEmpty(),
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
