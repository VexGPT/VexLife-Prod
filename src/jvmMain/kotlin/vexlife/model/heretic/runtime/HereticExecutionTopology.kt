package vexlife.model.heretic.runtime

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.security.MessageDigest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import vexlife.capability.VexLifeCapabilityBridge
import vexlife.companion.DirectPromptContext
import vexlife.memory.VexLifeMemoryContract
import vexlife.model.heretic.HereticCompanionRepresentation
import vexlife.model.heretic.HereticModelCatalog

/**
 * Read-only projection of the effective local-model execution configuration.
 *
 * This object deliberately owns no model, runtime or continuity setting. It reads each value from
 * the source that already owns it, then adds two pieces of relationship information future humans
 * and AI workers otherwise have to rediscover:
 *
 * - where the effective value came from;
 * - what boundary must be crossed for that value to change.
 *
 * Model architecture is read from the exact pinned config.json member. Product policy descends from
 * its existing Kotlin owners. mlx-lm defaults are projected only while the exact pinned dependency
 * version remains the one this source has audited.
 */
object HereticExecutionTopology {
    const val Schema = "vexlife-prod.heretic-execution-topology/v1"

    enum class Provenance {
        MODEL_ARTIFACT,
        PRODUCT_SOURCE,
        PROVIDER_RECOMMENDATION,
        PINNED_MLX_LM_DEFAULT,
    }

    enum class ChangeBoundary {
        MODEL_ARTIFACT_CHANGE,
        PRODUCT_SOURCE_CHANGE,
        RUNTIME_RESTART,
    }

    enum class EffectiveState {
        ACTIVE,
        DORMANT,
    }

    data class EffectiveField<T>(
        val value: T,
        val provenance: Provenance,
        val changeBoundary: ChangeBoundary,
        val sourceRef: String,
        val inheritedDefault: T? = null,
        val inheritedDefaultSourceRef: String? = null,
        val state: EffectiveState = EffectiveState.ACTIVE,
    )

    data class ModelArchitecture(
        val architectureClass: EffectiveField<String?>,
        val modelType: EffectiveField<String?>,
        val nativeContextTokens: EffectiveField<Int?>,
        val hiddenLayers: EffectiveField<Int?>,
        val fullAttentionInterval: EffectiveField<Int?>,
        val fullAttentionLayers: EffectiveField<Int?>,
        val linearAttentionLayers: EffectiveField<Int?>,
        val attentionHeads: EffectiveField<Int?>,
        val keyValueHeads: EffectiveField<Int?>,
        val headDimension: EffectiveField<Int?>,
        val cacheEnabled: EffectiveField<Boolean?>,
    )

    data class RuntimeConfiguration(
        val mlxLmVersion: EffectiveField<String>,
        val promptCacheEntries: EffectiveField<Int>,
        val promptCacheBytes: EffectiveField<Long?>,
        val prefillStepSize: EffectiveField<Int>,
        val kvBits: EffectiveField<Int?>,
        val kvGroupSize: EffectiveField<Int>,
        val quantizedKvStart: EffectiveField<Int>,
        val promptConcurrency: EffectiveField<Int>,
        val decodeConcurrency: EffectiveField<Int>,
    )

    data class GenerationConfiguration(
        val messageProtocol: EffectiveField<String>,
        val reasoningRepresentation: EffectiveField<String>,
        val capabilityRepresentation: EffectiveField<String>,
        val reachRepresentationStanding: EffectiveField<String>,
        val temperature: EffectiveField<Double>,
        val topP: EffectiveField<Double>,
        val topK: EffectiveField<Int>,
        val reasoningEffort: EffectiveField<String?>,
        val maximumOutputTokens: EffectiveField<Int>,
    )

    data class ContinuityConfiguration(
        /** Product ceiling for input material before output reservation. */
        val inputTokenBudget: EffectiveField<Int>,
        /** Room withheld from ordinary prompt material when capability reach is admitted. */
        val capabilityExchangeReserveTokens: EffectiveField<Int>,
        /** Ordinary prompt room left when the capability reserve is active. */
        val directTurnInputWithReachTokens: EffectiveField<Int>,
        val maximumMemoryTokens: EffectiveField<Int>,
        val maximumRequestsPerTurn: EffectiveField<Int>,
        val reservedOutputTokens: EffectiveField<Int>,
        val hardInputOutputTokenLimit: EffectiveField<Int>,
        /**
         * No dynamic host-pressure target exists yet. This is explicit absence, not a guessed
         * threshold. A later behavioral owner may replace this only after lived evidence earns it.
         */
        val adaptiveContextPolicyState: String,
    )

    data class Projection(
        val schemaVersion: String,
        val modelRef: String,
        val modelProfileRef: String,
        val architecture: ModelArchitecture,
        val runtime: RuntimeConfiguration,
        val generation: GenerationConfiguration,
        val continuity: ContinuityConfiguration,
        val effectsPerformed: Boolean,
    )

    /**
     * Observe topology for one materialized catalog model.
     *
     * Only config.json is hashed here. The artifact/materialization owner still owns whole-model
     * custody and qualification; this projection needs exact bytes only for the architecture member
     * whose values it exposes.
     */
    fun observe(
        productHome: Path,
        model: HereticModelCatalog.Entry = HereticModelCatalog.Default,
    ): Projection {
        require(productHome.isAbsolute) {
            "Heretic execution topology requires one absolute VexLife Home"
        }
        val plan = HereticArtifactMaterialization.plan(model)
        val configMember =
            plan.members.singleOrNull { it.logicalPath == "config.json" }
                ?: error("Heretic model has no pinned config.json member")
        val home = productHome.toAbsolutePath().normalize()
        val configPath =
            home
                .resolve(model.modelRootRelativePath)
                .resolve(model.providerRevision)
                .resolve(configMember.logicalPath)
                .normalize()
        require(configPath.startsWith(home)) {
            "Heretic model config escaped the VexLife Home"
        }
        require(Files.isRegularFile(configPath, LinkOption.NOFOLLOW_LINKS)) {
            "Heretic model config.json is not materialized"
        }
        val bytes = Files.readAllBytes(configPath)
        require(bytes.size.toLong() == configMember.byteLength) {
            "Heretic model config.json size does not match the pinned member"
        }
        require(sha256(bytes) == configMember.sha256) {
            "Heretic model config.json does not match the pinned member"
        }

        return project(
            model = model,
            exactModelConfig = String(bytes, StandardCharsets.UTF_8),
            modelConfigSourceRef = configMember.artifactRef,
        )
    }

    internal fun project(
        model: HereticModelCatalog.Entry,
        exactModelConfig: String,
        modelConfigSourceRef: String,
    ): Projection {
        val root =
            Json.parseToJsonElement(exactModelConfig) as? JsonObject
                ?: error("Heretic model config.json must be one JSON object")
        val text = root["text_config"] as? JsonObject ?: root
        val hiddenLayers = text.int("num_hidden_layers")
        val interval = text.int("full_attention_interval")
        val fullLayers =
            if (hiddenLayers != null && interval != null && interval > 0) {
                hiddenLayers / interval
            } else {
                null
            }
        val linearLayers =
            if (hiddenLayers != null && fullLayers != null) {
                hiddenLayers - fullLayers
            } else {
                null
            }

        val architecture =
            ModelArchitecture(
                architectureClass =
                    modelField(
                        root.array("architectures")
                            ?.firstOrNull()
                            ?.jsonPrimitive
                            ?.contentOrNull,
                        modelConfigSourceRef,
                    ),
                modelType =
                    modelField(
                        text.string("model_type") ?: root.string("model_type"),
                        modelConfigSourceRef,
                    ),
                nativeContextTokens =
                    modelField(
                        text.int("max_position_embeddings"),
                        modelConfigSourceRef,
                    ),
                hiddenLayers =
                    modelField(
                        hiddenLayers,
                        modelConfigSourceRef,
                    ),
                fullAttentionInterval =
                    modelField(
                        interval,
                        modelConfigSourceRef,
                    ),
                fullAttentionLayers =
                    modelField(
                        fullLayers,
                        modelConfigSourceRef,
                    ),
                linearAttentionLayers =
                    modelField(
                        linearLayers,
                        modelConfigSourceRef,
                    ),
                attentionHeads =
                    modelField(
                        text.int("num_attention_heads"),
                        modelConfigSourceRef,
                    ),
                keyValueHeads =
                    modelField(
                        text.int("num_key_value_heads"),
                        modelConfigSourceRef,
                    ),
                headDimension =
                    modelField(
                        text.int("head_dim"),
                        modelConfigSourceRef,
                    ),
                cacheEnabled =
                    modelField(
                        text.boolean("use_cache"),
                        modelConfigSourceRef,
                    ),
            )

        return Projection(
            schemaVersion = Schema,
            modelRef = model.modelRef,
            modelProfileRef = model.identity.modelProfileRef,
            architecture = architecture,
            runtime = runtime(model),
            generation = generation(model),
            continuity = continuity(),
            effectsPerformed = false,
        )
    }

    private fun runtime(model: HereticModelCatalog.Entry): RuntimeConfiguration {
        val version = model.identity.mlxLmVersion
        require(version == SupportedMlxLmVersion) {
            "Heretic execution topology has not audited mlx-lm defaults for $version"
        }
        val serverSource = "mlx-lm@$version:mlx_lm/server.py"
        val generateSource = "mlx-lm@$version:mlx_lm/generate.py"
        val runtimeSource =
            "src/jvmMain/kotlin/vexlife/model/heretic/runtime/HereticPersistentRuntime.kt"

        return RuntimeConfiguration(
            mlxLmVersion =
                EffectiveField(
                    value = version,
                    provenance = Provenance.PRODUCT_SOURCE,
                    changeBoundary = ChangeBoundary.PRODUCT_SOURCE_CHANGE,
                    sourceRef = "src/commonMain/kotlin/vexlife/model/heretic/HereticProductIdentity.kt",
                ),
            promptCacheEntries =
                EffectiveField(
                    value = HereticPersistentRuntime.PromptCacheEntryLimit,
                    provenance = Provenance.PRODUCT_SOURCE,
                    changeBoundary = ChangeBoundary.RUNTIME_RESTART,
                    sourceRef = runtimeSource,
                    inheritedDefault = MlxDefaults.PromptCacheEntries,
                    inheritedDefaultSourceRef = serverSource,
                ),
            promptCacheBytes =
                EffectiveField(
                    value = null,
                    provenance = Provenance.PINNED_MLX_LM_DEFAULT,
                    changeBoundary = ChangeBoundary.RUNTIME_RESTART,
                    sourceRef = serverSource,
                    inheritedDefault = null,
                    inheritedDefaultSourceRef = serverSource,
                ),
            prefillStepSize =
                pinnedMlxDefault(
                    value = MlxDefaults.PrefillStepSize,
                    sourceRef = serverSource,
                ),
            kvBits =
                pinnedMlxDefault<Int?>(
                    value = null,
                    sourceRef = serverSource,
                ),
            kvGroupSize =
                pinnedMlxDefault(
                    value = MlxDefaults.KvGroupSize,
                    sourceRef = serverSource,
                    state = EffectiveState.DORMANT,
                ),
            quantizedKvStart =
                pinnedMlxDefault(
                    value = MlxDefaults.QuantizedKvStart,
                    sourceRef = generateSource,
                    state = EffectiveState.DORMANT,
                ),
            promptConcurrency =
                pinnedMlxDefault(
                    value = MlxDefaults.PromptConcurrency,
                    sourceRef = serverSource,
                ),
            decodeConcurrency =
                pinnedMlxDefault(
                    value = MlxDefaults.DecodeConcurrency,
                    sourceRef = serverSource,
                ),
        )
    }

    private fun generation(model: HereticModelCatalog.Entry): GenerationConfiguration {
        val profile = model.companionProfile
        val sampling = profile.generation.sampling
        val samplingProvenance =
            when (sampling.basis) {
                HereticCompanionRepresentation.SamplingBasis.PINNED_MODEL_FILE ->
                    Provenance.MODEL_ARTIFACT

                HereticCompanionRepresentation.SamplingBasis.PROVIDER_RECOMMENDATION ->
                    Provenance.PROVIDER_RECOMMENDATION
            }

        fun <T> samplingField(value: T): EffectiveField<T> =
            EffectiveField(
                value = value,
                provenance = samplingProvenance,
                changeBoundary = ChangeBoundary.PRODUCT_SOURCE_CHANGE,
                sourceRef = sampling.sourceRef,
            )

        fun <T> profileField(value: T): EffectiveField<T> =
            EffectiveField(
                value = value,
                provenance = Provenance.PRODUCT_SOURCE,
                changeBoundary = ChangeBoundary.PRODUCT_SOURCE_CHANGE,
                sourceRef = profile.profileRef,
            )

        return GenerationConfiguration(
            messageProtocol = profileField(profile.messageProtocol.name),
            reasoningRepresentation = profileField(profile.reasoningRepresentation.name),
            capabilityRepresentation = profileField(profile.capabilityRepresentation.name),
            reachRepresentationStanding = profileField(profile.reachRepresentationStanding.name),
            temperature = samplingField(sampling.temperature),
            topP = samplingField(sampling.topP),
            topK = samplingField(sampling.topK),
            reasoningEffort = profileField(profile.generation.reasoningEffort?.wireValue),
            maximumOutputTokens = profileField(profile.generation.productMaxOutputTokens),
        )
    }

    private fun continuity(): ContinuityConfiguration {
        fun field(
            value: Int,
            sourceRef: String,
        ): EffectiveField<Int> =
            EffectiveField(
                value = value,
                provenance = Provenance.PRODUCT_SOURCE,
                changeBoundary = ChangeBoundary.PRODUCT_SOURCE_CHANGE,
                sourceRef = sourceRef,
            )

        val promptSource = "src/jvmMain/kotlin/vexlife/companion/DirectPromptContext.kt"
        val memorySource = "src/commonMain/kotlin/vexlife/memory/VexLifeMemoryContract.kt"
        val capabilitySource = "src/jvmMain/kotlin/vexlife/capability/VexLifeCapabilityBridge.kt"

        return ContinuityConfiguration(
            inputTokenBudget =
                field(
                    DirectPromptContext.InputTokenBudget,
                    promptSource,
                ),
            capabilityExchangeReserveTokens =
                field(
                    VexLifeCapabilityBridge.ExchangeAllowanceTokens,
                    capabilitySource,
                ),
            directTurnInputWithReachTokens =
                field(
                    DirectPromptContext.DirectTurnInputTokenBudget,
                    promptSource,
                ),
            maximumMemoryTokens =
                field(
                    VexLifeMemoryContract.MaximumMemoryTokens,
                    memorySource,
                ),
            maximumRequestsPerTurn =
                field(
                    VexLifeCapabilityBridge.MaximumRequestsPerTurn,
                    capabilitySource,
                ),
            reservedOutputTokens =
                field(
                    DirectPromptContext.ReservedOutputTokens,
                    promptSource,
                ),
            hardInputOutputTokenLimit =
                field(
                    DirectPromptContext.HardTokenLimit,
                    promptSource,
                ),
            adaptiveContextPolicyState = "NOT_CONFIGURED",
        )
    }

    private fun <T> modelField(
        value: T,
        sourceRef: String,
    ): EffectiveField<T> =
        EffectiveField(
            value = value,
            provenance = Provenance.MODEL_ARTIFACT,
            changeBoundary = ChangeBoundary.MODEL_ARTIFACT_CHANGE,
            sourceRef = sourceRef,
        )

    private fun <T> pinnedMlxDefault(
        value: T,
        sourceRef: String,
        state: EffectiveState = EffectiveState.ACTIVE,
    ): EffectiveField<T> =
        EffectiveField(
            value = value,
            provenance = Provenance.PINNED_MLX_LM_DEFAULT,
            changeBoundary = ChangeBoundary.RUNTIME_RESTART,
            sourceRef = sourceRef,
            inheritedDefault = value,
            inheritedDefaultSourceRef = sourceRef,
            state = state,
        )

    private fun JsonObject.int(name: String): Int? =
        (this[name] as? JsonPrimitive)?.intOrNull

    private fun JsonObject.string(name: String): String? =
        (this[name] as? JsonPrimitive)?.contentOrNull

    private fun JsonObject.boolean(name: String): Boolean? =
        (this[name] as? JsonPrimitive)?.booleanOrNull

    private fun JsonObject.array(name: String): JsonArray? =
        this[name] as? JsonArray

    private fun sha256(bytes: ByteArray): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private const val SupportedMlxLmVersion = "0.32.0"

    private object MlxDefaults {
        const val PromptCacheEntries = 10
        const val PrefillStepSize = 2_048
        const val PromptConcurrency = 8
        const val DecodeConcurrency = 32
        const val KvGroupSize = 64
        const val QuantizedKvStart = 5_000
    }
}

// [VXG RealForever]
