package vexlife.labs

import java.nio.file.Files
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
            var requestBody =
                ""
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
                        requestBody =
                            call.body
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
                                        "content": "This answer exists only in Vex Labs.",
                                        "reasoning": "I can use the seeded scratch Conversation."
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
                    assertTrue(
                        before.fields.any {
                            it.fieldRef ==
                                "field.vexlife-prod.context-profile.input-token-budget" &&
                                it.labEditability ==
                                "EDITABLE_LAB_ONLY"
                        },
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
                    assertTrue(
                        requestBody.contains(
                            "Keep this exact seed conversation available to the Lab.",
                        ),
                    )
                    assertTrue(
                        requestBody.contains(
                            "What would you say with a smaller active context?",
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
}

// [VXG RealForever]
