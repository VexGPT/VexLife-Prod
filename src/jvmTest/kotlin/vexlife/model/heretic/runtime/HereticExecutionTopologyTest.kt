package vexlife.model.heretic.runtime

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticRuntimeConfiguration

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
            assertEquals("PROVIDER_NATIVE_TOOLS", capabilityRepresentation.value)
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
            assertEquals(16_000, reservedOutputTokens.value)
            assertEquals(208_000, hardInputOutputTokenLimit.value)
            assertEquals("NOT_CONFIGURED", adaptiveContextPolicyState)
        }
    }

    @Test
    fun explicitHomePromptCacheConfigurationBecomesEffectiveTopologyTruth() {
        val model =
            HereticModelCatalog.Models.Qwen4B
        val explicit =
            HereticRuntimeConfiguration.withPromptCacheEntries(
                value = HereticRuntimeConfiguration.empty(),
                model = model,
                promptCacheEntries = 2,
            )
        val explicitTopology =
            HereticExecutionTopology.project(
                model = model,
                exactModelConfig = qwenConfig,
                modelConfigSourceRef = "artifact.test.qwen4b.config",
                runtimeConfigurationValue = explicit,
            )

        assertEquals(
            2,
            explicitTopology.runtime.promptCacheEntries.value,
        )
        assertEquals(
            HereticExecutionTopology.Provenance.PRODUCT_HOME_CONFIGURATION,
            explicitTopology.runtime.promptCacheEntries.provenance,
        )
        assertEquals(
            FileHereticRuntimeConfigurationStore.RelativePath,
            explicitTopology.runtime.promptCacheEntries.sourceRef,
        )

        val legacyRecommended =
            HereticRuntimeConfiguration.withLegacyRecommended(
                value = explicit,
                model = model,
            )
        val legacyRecommendedTopology =
            HereticExecutionTopology.project(
                model = model,
                exactModelConfig = qwenConfig,
                modelConfigSourceRef = "artifact.test.qwen4b.config",
                runtimeConfigurationValue = legacyRecommended,
            )
        assertEquals(
            2,
            legacyRecommendedTopology.runtime.promptCacheEntries.value,
        )
        assertEquals(
            HereticExecutionTopology.Provenance.PRODUCT_SOURCE,
            legacyRecommendedTopology.runtime.promptCacheEntries.provenance,
        )
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

    @Test
    fun fieldCatalogExposesEveryKnownExecutionFieldWithoutStealingItsOwner() {
        val topology =
            HereticExecutionTopology.project(
                model = HereticModelCatalog.Models.Qwen4B,
                exactModelConfig = qwenConfig,
                modelConfigSourceRef = "artifact.test.qwen4b.config",
            )
        val catalog =
            topology.fieldCatalog

        assertEquals(
            HereticExecutionTopology.FieldCatalogSchema,
            catalog.schemaVersion,
        )
        assertEquals(
            topology.modelRef,
            catalog.modelRef,
        )
        assertEquals(
            topology.modelProfileRef,
            catalog.modelProfileRef,
        )
        assertFalse(
            catalog.effectsPerformed,
        )
        assertEquals(
            36,
            catalog.fields.size,
        )
        assertEquals(
            catalog.fields.size,
            catalog.fields.map { it.fieldRef }.distinct().size,
        )
        assertTrue(
            catalog.fields.all {
                it.label.key.startsWith(
                    "workspace.label.execution-field-",
                ) &&
                    it.explanation.key.startsWith(
                        "workspace.description.execution-field-",
                    ) &&
                    it.sourceRef.isNotBlank() &&
                    it.ownerRef.isNotBlank()
            },
        )

        val promptCache =
            catalog.fields.single {
                it.fieldRef ==
                    "field.vexlife-prod.runtime.prompt-cache-entries"
            }
        assertEquals(
            "1",
            promptCache.currentValue,
        )
        assertEquals(
            "10",
            promptCache.inheritedDefaultValue,
        )
        assertEquals(
            "2",
            promptCache.recommendedValue,
        )
        assertEquals(
            "1",
            promptCache.minimumValue,
        )
        assertEquals(
            "3",
            promptCache.maximumValue,
        )
        assertEquals(
            HereticExecutionTopology.UserMutability.USER_EDITABLE,
            promptCache.userMutability,
        )
        assertEquals(
            HereticExecutionTopology.UserApplyBoundary.RUNTIME_RESTART,
            promptCache.userApplyBoundary,
        )
        assertEquals(
            vexlife.model.VexLifeModelsFamily.ControlSetPromptCacheEntries,
            promptCache.controlRef,
        )
        assertEquals(
            "workspace.label.execution-field-runtime-prompt-cache-entries",
            promptCache.label.key,
        )
        assertEquals(
            "workspace.description.execution-field-runtime-prompt-cache-entries",
            promptCache.explanation.key,
        )

        val activeContext =
            catalog.fields.single {
                it.fieldRef ==
                    "field.vexlife-prod.continuity.input-token-budget"
            }
        assertEquals(
            "192000",
            activeContext.currentValue,
        )
        assertEquals(
            HereticExecutionTopology.UserMutability.READ_ONLY_PRODUCT_POLICY,
            activeContext.userMutability,
        )
        assertNull(
            activeContext.userApplyBoundary,
        )
        assertNull(
            activeContext.controlRef,
        )
        assertEquals(
            "workspace.description.execution-field-continuity-input-token-budget-decrease",
            requireNotNull(
                activeContext.decreaseConsequence,
            ).key,
        )

        val nativeContext =
            catalog.fields.single {
                it.fieldRef ==
                    "field.vexlife-prod.model.native-context-tokens"
            }
        assertEquals(
            "262144",
            nativeContext.currentValue,
        )
        assertEquals(
            HereticExecutionTopology.UserMutability.READ_ONLY_MODEL_ARTIFACT,
            nativeContext.userMutability,
        )
        assertNull(
            nativeContext.userApplyBoundary,
        )

        val kvGroup =
            catalog.fields.single {
                it.fieldRef ==
                    "field.vexlife-prod.runtime.kv-group-size"
            }
        assertEquals(
            HereticExecutionTopology.EffectiveState.DORMANT,
            kvGroup.effectiveState,
        )
        assertEquals(
            HereticExecutionTopology.UserMutability.HELD_UNTIL_QUALIFIED,
            kvGroup.userMutability,
        )
    }

}

// [VXG RealForever]
