package vexlife.model.heretic.runtime

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import vexlife.model.heretic.HereticModelCatalog

class HereticExecutionTopologyTest {
    private val qwenLayerTypes =
        List(64) { index ->
            if ((index + 1) % 4 == 0) "full_attention" else "linear_attention"
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

    @Test
    fun topologyKeepsArchitectureRuntimeGenerationAndContinuitySourcesDistinct() {
        val topology =
            HereticExecutionTopology.project(
                model = HereticModelCatalog.Models.Qwen27B,
                exactModelConfig = qwenConfig,
                modelConfigSourceRef = "artifact.test.qwen27b.config",
            )

        assertEquals(HereticExecutionTopology.Schema, topology.schemaVersion)
        assertEquals(HereticModelCatalog.Models.Qwen27B.modelRef, topology.modelRef)
        assertFalse(topology.effectsPerformed)

        with(topology.architecture) {
            assertEquals("Qwen3_5ForConditionalGeneration", architectureClass.value)
            assertEquals("qwen3_5_text", modelType.value)
            assertEquals(262_144, nativeContextTokens.value)
            assertEquals(64, hiddenLayers.value)
            assertEquals(4, fullAttentionInterval.value)
            assertEquals(16, fullAttentionLayers.value)
            assertEquals(48, linearAttentionLayers.value)
            assertEquals(24, attentionHeads.value)
            assertEquals(4, keyValueHeads.value)
            assertEquals(256, headDimension.value)
            assertEquals(true, cacheEnabled.value)
            assertEquals(HereticExecutionTopology.Provenance.MODEL_ARTIFACT, fullAttentionInterval.provenance)
            assertEquals(
                HereticExecutionTopology.ChangeBoundary.MODEL_ARTIFACT_CHANGE,
                fullAttentionInterval.changeBoundary,
            )
            assertEquals("artifact.test.qwen27b.config", fullAttentionInterval.sourceRef)
        }

        with(topology.runtime) {
            assertEquals("0.32.0", mlxLmVersion.value)
            assertEquals(1, promptCacheEntries.value)
            assertEquals(10, promptCacheEntries.inheritedDefault)
            assertEquals(HereticExecutionTopology.Provenance.PRODUCT_SOURCE, promptCacheEntries.provenance)
            assertEquals(
                HereticExecutionTopology.ChangeBoundary.RUNTIME_RESTART,
                promptCacheEntries.changeBoundary,
            )
            assertEquals(2_048, prefillStepSize.value)
            assertEquals(
                HereticExecutionTopology.Provenance.PINNED_MLX_LM_DEFAULT,
                prefillStepSize.provenance,
            )
            assertEquals(2_048, prefillStepSize.inheritedDefault)
            assertNull(kvBits.value)
            assertNull(kvBits.inheritedDefault)
            assertEquals(HereticExecutionTopology.Provenance.PINNED_MLX_LM_DEFAULT, kvBits.provenance)
            assertEquals(64, kvGroupSize.value)
            assertEquals(HereticExecutionTopology.EffectiveState.DORMANT, kvGroupSize.state)
            assertEquals(5_000, quantizedKvStart.value)
            assertEquals(HereticExecutionTopology.EffectiveState.DORMANT, quantizedKvStart.state)
            assertEquals(8, promptConcurrency.value)
            assertEquals(32, decodeConcurrency.value)
        }

        with(topology.generation) {
            assertEquals("OPENAI_CHAT", messageProtocol.value)
            assertEquals("REASONING_FIELDS", reasoningRepresentation.value)
            assertEquals("VEXLIFE_CONTROL_TEXT", capabilityRepresentation.value)
            assertEquals("QUALIFIED", reachRepresentationStanding.value)
            assertEquals(1.0, temperature.value)
            assertEquals(0.95, topP.value)
            assertEquals(20, topK.value)
            assertNull(reasoningEffort.value)
            assertEquals(16_000, maximumOutputTokens.value)
        }

        with(topology.continuity) {
            assertEquals(192_000, inputTokenBudget.value)
            assertEquals(24_000, capabilityExchangeReserveTokens.value)
            assertEquals(168_000, directTurnInputWithReachTokens.value)
            assertEquals(100_000, maximumMemoryTokens.value)
            assertEquals(4, maximumRequestsPerTurn.value)
            assertEquals(16_000, reservedOutputTokens.value)
            assertEquals(208_000, hardInputOutputTokenLimit.value)
            assertEquals("NOT_CONFIGURED", adaptiveContextPolicyState)
        }
    }

    @Test
    fun topologyRejectsLayerTypesThatConflictWithTheArtifactInterval() {
        val conflicting =
            qwenConfig.replaceFirst("\"full_attention\"", "\"linear_attention\"")

        assertFailsWith<IllegalArgumentException> {
            HereticExecutionTopology.project(
                model = HereticModelCatalog.Models.Qwen27B,
                exactModelConfig = conflicting,
                modelConfigSourceRef = "artifact.test.qwen27b.config",
            )
        }
    }

    @Test
    fun observedTopologyRefusesConfigBytesThatAreNotThePinnedArtifactMember() {
        val home = Files.createTempDirectory("vexlife-execution-topology-").toAbsolutePath()
        try {
            val model = HereticModelCatalog.Models.Qwen27B
            val config =
                home.resolve(model.modelRootRelativePath)
                    .resolve(model.providerRevision)
                    .resolve("config.json")
            Files.createDirectories(config.parent)
            Files.writeString(config, qwenConfig)

            assertFailsWith<IllegalArgumentException> {
                HereticExecutionTopology.observe(productHome = home, model = model)
            }
        } finally {
            home.toFile().deleteRecursively()
        }
    }
}

// [VXG RealForever]
