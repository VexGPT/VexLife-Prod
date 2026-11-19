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
import vexlife.model.VexLifeModelsFamily
import vexlife.model.heretic.HereticCompanionRepresentation
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticModelOfferings
import vexlife.model.heretic.HereticRuntimeConfiguration

/**
 * Read-only projection of the effective local-model execution configuration.
 *
 * This object owns no model, runtime or continuity setting. It reads each value from the source that
 * already owns it and adds the relationship information future humans/AI otherwise rediscover:
 * provenance and the boundary that must be crossed for the value to change.
 *
 * Model architecture is read from the exact pinned config.json member. Product policy descends from
 * existing Kotlin owners. mlx-lm defaults are projected only while the exact pinned dependency
 * version remains the one this source audited.
 */
object HereticExecutionTopology {
    const val Schema = "vexlife-prod.heretic-execution-topology/v1"
    const val FieldCatalogSchema = "vexlife-prod.heretic-execution-field-catalog/v1"

    enum class Provenance {
        MODEL_ARTIFACT,
        PRODUCT_SOURCE,
        PRODUCT_HOME_CONFIGURATION,
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

    /**
     * Human-facing grouping only. These names do not become model/runtime/configuration owners.
     */
    enum class FieldGroup {
        MODEL_ARCHITECTURE,
        RUNTIME,
        GENERATION,
        CONTINUITY,
    }

    /**
     * Whether a person can change this field through a currently earned Product path.
     *
     * HELD_UNTIL_QUALIFIED keeps a known/model-supported or Product-plausible field visible while
     * making the missing edit path explicit. It is not a Control and grants no mutation authority.
     */
    enum class UserMutability {
        USER_EDITABLE,
        READ_ONLY_MODEL_ARTIFACT,
        READ_ONLY_PRODUCT_POLICY,
        HELD_UNTIL_QUALIFIED,
    }

    /** The boundary a future/current user edit would cross. Null means no user edit is admitted. */
    enum class UserApplyBoundary {
        NEXT_SEND,
        RUNTIME_RESTART,
    }

    /**
     * One inspectable field in Vex Context.
     *
     * The effective value/provenance/current change boundary still come from the rightful source
     * owner represented by [EffectiveField]. This descriptor only adds the human relationship:
     * what the field means, whether a user edit is currently earned, and what consequence to expect.
     */
    data class FieldDescriptor(
        val fieldRef: String,
        val group: FieldGroup,
        val label: String,
        val explanation: String,
        val currentValue: String?,
        val inheritedDefaultValue: String?,
        val recommendedValue: String?,
        val minimumValue: String?,
        val maximumValue: String?,
        val unit: String?,
        val provenance: Provenance,
        val currentChangeBoundary: ChangeBoundary,
        val sourceRef: String,
        val ownerRef: String,
        val effectiveState: EffectiveState,
        val userMutability: UserMutability,
        val userApplyBoundary: UserApplyBoundary?,
        val controlRef: String?,
        val increaseConsequence: String?,
        val decreaseConsequence: String?,
        val relatedFieldRefs: List<String> = emptyList(),
    ) {
        init {
            require(fieldRef.matches(Regex("field\\.vexlife-prod\\.[a-z0-9.-]+"))) {
                "execution field ref is invalid"
            }
            require(label.isNotBlank() && explanation.isNotBlank() && sourceRef.isNotBlank() && ownerRef.isNotBlank()) {
                "execution field human/source relationship is incomplete"
            }
            require(
                (userMutability == UserMutability.USER_EDITABLE) ==
                    (controlRef != null && userApplyBoundary != null),
            ) {
                "editable execution field must name one current Control and apply boundary"
            }
            require(
                userMutability != UserMutability.HELD_UNTIL_QUALIFIED ||
                    (controlRef == null && userApplyBoundary != null),
            ) {
                "held execution field may name a future apply boundary but no current Control"
            }
            require(relatedFieldRefs.distinct().size == relatedFieldRefs.size) {
                "execution field related refs must be unique"
            }
        }
    }

    data class FieldCatalog(
        val schemaVersion: String,
        val modelRef: String,
        val modelProfileRef: String,
        val fields: List<FieldDescriptor>,
        val effectsPerformed: Boolean,
    ) {
        init {
            require(schemaVersion == FieldCatalogSchema && !effectsPerformed) {
                "execution field catalog identity/effect boundary is invalid"
            }
            require(fields.isNotEmpty() && fields.map { it.fieldRef }.distinct().size == fields.size) {
                "execution field catalog requires unique fields"
            }
        }
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
        val fieldCatalog: FieldCatalog,
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
            runtimeConfigurationValue =
                FileHereticRuntimeConfigurationStore(
                    home,
                ).read(),
            modelForRef = { modelRef ->
                HereticModelOfferings.entry(
                    home,
                    modelRef,
                )
            },
        )
    }

    internal fun project(
        model: HereticModelCatalog.Entry,
        exactModelConfig: String,
        modelConfigSourceRef: String,
        runtimeConfigurationValue: HereticRuntimeConfiguration.Value =
            HereticRuntimeConfiguration.empty(),
        modelForRef: (String) -> HereticModelCatalog.Entry? = HereticModelCatalog::entry,
    ): Projection {
        val root =
            Json.parseToJsonElement(exactModelConfig) as? JsonObject
                ?: error("Heretic model config.json must be one JSON object")
        val text = root["text_config"] as? JsonObject ?: root
        val hiddenLayers = text.int("num_hidden_layers")
        val interval = text.int("full_attention_interval")
        val layerTypes = text.stringList("layer_types")
        val fullLayers =
            layerTypes?.let { types ->
                require(hiddenLayers == null || types.size == hiddenLayers) {
                    "Heretic model layer_types count does not match num_hidden_layers"
                }
                require(types.all { it == "linear_attention" || it == "full_attention" }) {
                    "Heretic model layer_types contains an unsupported attention kind"
                }
                if (interval != null) {
                    require(interval > 0) {
                        "Heretic model full_attention_interval must be positive"
                    }
                    require(
                        types.withIndex().all { (index, type) ->
                            (type == "full_attention") == ((index + 1) % interval == 0)
                        },
                    ) {
                        "Heretic model layer_types does not match full_attention_interval"
                    }
                }
                types.count { it == "full_attention" }
            }
        val linearLayers =
            layerTypes?.count { it == "linear_attention" }

        val architecture =
            ModelArchitecture(
                architectureClass =
                    modelField(
                        root.array("architectures")?.firstOrNull()?.jsonPrimitive?.contentOrNull,
                        modelConfigSourceRef,
                    ),
                modelType =
                    modelField(
                        text.string("model_type") ?: root.string("model_type"),
                        modelConfigSourceRef,
                    ),
                nativeContextTokens =
                    modelField(text.int("max_position_embeddings"), modelConfigSourceRef),
                hiddenLayers = modelField(hiddenLayers, modelConfigSourceRef),
                fullAttentionInterval = modelField(interval, modelConfigSourceRef),
                fullAttentionLayers = modelField(fullLayers, modelConfigSourceRef),
                linearAttentionLayers = modelField(linearLayers, modelConfigSourceRef),
                attentionHeads = modelField(text.int("num_attention_heads"), modelConfigSourceRef),
                keyValueHeads = modelField(text.int("num_key_value_heads"), modelConfigSourceRef),
                headDimension = modelField(text.int("head_dim"), modelConfigSourceRef),
                cacheEnabled = modelField(text.boolean("use_cache"), modelConfigSourceRef),
            )

        val runtime =
            runtime(
                model = model,
                runtimeConfigurationValue = runtimeConfigurationValue,
                modelForRef = modelForRef,
            )
        val generation =
            generation(
                model,
            )
        val continuity =
            continuity()

        return Projection(
            schemaVersion = Schema,
            modelRef = model.modelRef,
            modelProfileRef = model.identity.modelProfileRef,
            architecture = architecture,
            runtime = runtime,
            generation = generation,
            continuity = continuity,
            fieldCatalog =
                fieldCatalog(
                    model = model,
                    architecture = architecture,
                    runtime = runtime,
                    generation = generation,
                    continuity = continuity,
                ),
            effectsPerformed = false,
        )
    }

    private fun fieldCatalog(
        model: HereticModelCatalog.Entry,
        architecture: ModelArchitecture,
        runtime: RuntimeConfiguration,
        generation: GenerationConfiguration,
        continuity: ContinuityConfiguration,
    ): FieldCatalog {
        val promptCacheRecommendation =
            HereticRuntimeConfiguration.recommendation(
                model,
            )
        return FieldCatalog(
            schemaVersion = FieldCatalogSchema,
            modelRef = model.modelRef,
            modelProfileRef = model.identity.modelProfileRef,
            fields =
                architectureFields(
                    architecture,
                ) +
                    runtimeFields(
                        runtime = runtime,
                        promptCacheRecommendation = promptCacheRecommendation,
                    ) +
                    generationFields(
                        generation,
                    ) +
                    continuityFields(
                        continuity,
                    ),
            effectsPerformed = false,
        )
    }

    private fun descriptor(
        fieldRef: String,
        group: FieldGroup,
        label: String,
        explanation: String,
        field: EffectiveField<*>,
        ownerRef: String,
        userMutability: UserMutability,
        userApplyBoundary: UserApplyBoundary? = null,
        controlRef: String? = null,
        recommendedValue: Any? = null,
        minimumValue: Any? = null,
        maximumValue: Any? = null,
        unit: String? = null,
        increaseConsequence: String? = null,
        decreaseConsequence: String? = null,
        relatedFieldRefs: List<String> = emptyList(),
        ): FieldDescriptor =
        FieldDescriptor(
            fieldRef = fieldRef,
            group = group,
            label = label,
            explanation = explanation,
            currentValue = field.value?.toString(),
            inheritedDefaultValue = field.inheritedDefault?.toString(),
            recommendedValue = recommendedValue?.toString(),
            minimumValue = minimumValue?.toString(),
            maximumValue = maximumValue?.toString(),
            unit = unit,
            provenance = field.provenance,
            currentChangeBoundary = field.changeBoundary,
            sourceRef = field.sourceRef,
            ownerRef = ownerRef,
            effectiveState = field.state,
            userMutability = userMutability,
            userApplyBoundary = userApplyBoundary,
            controlRef = controlRef,
            increaseConsequence = increaseConsequence,
            decreaseConsequence = decreaseConsequence,
            relatedFieldRefs = relatedFieldRefs,
        )

    private fun architectureFields(
        architecture: ModelArchitecture,
    ): List<FieldDescriptor> {
        val architectureOwner =
            "model-artifact.config"
        return listOf(
                descriptor(
                "field.vexlife-prod.model.architecture-class",
                FieldGroup.MODEL_ARCHITECTURE,
                "Architecture class",
                "The model implementation class declared by the exact pinned model artifact.",
                architecture.architectureClass,
                architectureOwner,
                UserMutability.READ_ONLY_MODEL_ARTIFACT,
                ),
                descriptor(
                "field.vexlife-prod.model.type",
                FieldGroup.MODEL_ARCHITECTURE,
                "Model type",
                "The model type declared by the exact pinned model artifact.",
                architecture.modelType,
                architectureOwner,
                UserMutability.READ_ONLY_MODEL_ARTIFACT,
                ),
                descriptor(
                "field.vexlife-prod.model.native-context-tokens",
                FieldGroup.MODEL_ARCHITECTURE,
                "Native context ceiling",
                "The maximum position/context span declared by the selected model artifact. Product/user prompt budgets must stay within the qualified runtime relationship to this ceiling.",
                architecture.nativeContextTokens,
                architectureOwner,
                UserMutability.READ_ONLY_MODEL_ARTIFACT,
                unit = "tokens",
                relatedFieldRefs =
                listOf(
                "field.vexlife-prod.continuity.input-token-budget",
                "field.vexlife-prod.continuity.hard-input-output-token-limit",
                ),
                ),
                descriptor(
                "field.vexlife-prod.model.hidden-layers",
                FieldGroup.MODEL_ARCHITECTURE,
                "Hidden layers",
                "The number of model layers declared by the artifact.",
                architecture.hiddenLayers,
                architectureOwner,
                UserMutability.READ_ONLY_MODEL_ARTIFACT,
                unit = "layers",
                ),
                descriptor(
                "field.vexlife-prod.model.full-attention-interval",
                FieldGroup.MODEL_ARCHITECTURE,
                "Full-attention interval",
                "How frequently the model artifact declares a full-attention layer among its layer sequence.",
                architecture.fullAttentionInterval,
                architectureOwner,
                UserMutability.READ_ONLY_MODEL_ARTIFACT,
                unit = "layers",
                ),
                descriptor(
                "field.vexlife-prod.model.full-attention-layers",
                FieldGroup.MODEL_ARCHITECTURE,
                "Full-attention layers",
                "The count of full-attention layers derived from the exact model artifact.",
                architecture.fullAttentionLayers,
                architectureOwner,
                UserMutability.READ_ONLY_MODEL_ARTIFACT,
                unit = "layers",
                ),
                descriptor(
                "field.vexlife-prod.model.linear-attention-layers",
                FieldGroup.MODEL_ARCHITECTURE,
                "Linear-attention layers",
                "The count of linear-attention layers derived from the exact model artifact.",
                architecture.linearAttentionLayers,
                architectureOwner,
                UserMutability.READ_ONLY_MODEL_ARTIFACT,
                unit = "layers",
                ),
                descriptor(
                "field.vexlife-prod.model.attention-heads",
                FieldGroup.MODEL_ARCHITECTURE,
                "Attention heads",
                "The model artifact's attention-head count.",
                architecture.attentionHeads,
                architectureOwner,
                UserMutability.READ_ONLY_MODEL_ARTIFACT,
                unit = "heads",
                ),
                descriptor(
                "field.vexlife-prod.model.key-value-heads",
                FieldGroup.MODEL_ARCHITECTURE,
                "Key/value heads",
                "The model artifact's key/value-head count, which contributes to KV-cache shape.",
                architecture.keyValueHeads,
                architectureOwner,
                UserMutability.READ_ONLY_MODEL_ARTIFACT,
                unit = "heads",
                ),
                descriptor(
                "field.vexlife-prod.model.head-dimension",
                FieldGroup.MODEL_ARCHITECTURE,
                "Head dimension",
                "The dimension of each attention head declared by the model artifact.",
                architecture.headDimension,
                architectureOwner,
                UserMutability.READ_ONLY_MODEL_ARTIFACT,
                ),
                descriptor(
                "field.vexlife-prod.model.cache-enabled",
                FieldGroup.MODEL_ARCHITECTURE,
                "Model cache support",
                "Whether the selected model artifact declares cache use.",
                architecture.cacheEnabled,
                architectureOwner,
                UserMutability.READ_ONLY_MODEL_ARTIFACT,
                ),
        )
    }

    private fun runtimeFields(
        runtime: RuntimeConfiguration,
        promptCacheRecommendation: HereticRuntimeConfiguration.Recommendation,
    ): List<FieldDescriptor> {
        val runtimeOwner =
            "HereticRuntimeConfiguration"
        return listOf(
                descriptor(
                "field.vexlife-prod.runtime.mlx-lm-version",
                FieldGroup.RUNTIME,
                "Runtime version",
                "The exact mlx-lm runtime version whose defaults this Product source has audited.",
                runtime.mlxLmVersion,
                runtimeOwner,
                UserMutability.READ_ONLY_PRODUCT_POLICY,
                ),
                descriptor(
                "field.vexlife-prod.runtime.prompt-cache-entries",
                FieldGroup.RUNTIME,
                "Keep recent conversation ready",
                "How many recent prompt-cache entries the current model runtime keeps ready. This is already an explicit per-model Home setting.",
                runtime.promptCacheEntries,
                runtimeOwner,
                UserMutability.USER_EDITABLE,
                userApplyBoundary = UserApplyBoundary.RUNTIME_RESTART,
                controlRef = VexLifeModelsFamily.ControlSetPromptCacheEntries,
                recommendedValue = promptCacheRecommendation.promptCacheEntries,
                minimumValue = HereticRuntimeConfiguration.MinimumPromptCacheEntries,
                maximumValue = promptCacheRecommendation.maximumPromptCacheEntries,
                unit = "entries",
                increaseConsequence = "May keep more recent prompt state ready, while using more memory. Values above the recommendation remain experimental unless current evidence says otherwise.",
                decreaseConsequence = "Uses less prompt-cache memory, while retaining fewer recent prompt states ready for reuse.",
                ),
                descriptor(
                "field.vexlife-prod.runtime.prompt-cache-bytes",
                FieldGroup.RUNTIME,
                "Prompt-cache byte cap",
                "A runtime byte cap for prompt cache when the selected provider exposes one. The pinned mlx-lm relationship currently leaves this unset.",
                runtime.promptCacheBytes,
                runtimeOwner,
                UserMutability.HELD_UNTIL_QUALIFIED,
                userApplyBoundary = UserApplyBoundary.RUNTIME_RESTART,
                unit = "bytes",
                ),
                descriptor(
                "field.vexlife-prod.runtime.prefill-step-size",
                FieldGroup.RUNTIME,
                "Prefill step size",
                "How many prompt tokens the pinned runtime processes per prefill step.",
                runtime.prefillStepSize,
                runtimeOwner,
                UserMutability.HELD_UNTIL_QUALIFIED,
                userApplyBoundary = UserApplyBoundary.RUNTIME_RESTART,
                unit = "tokens",
                increaseConsequence = "May change prefill throughput/memory behavior; VexLife has not qualified a user-edit path yet.",
                decreaseConsequence = "May reduce per-step work/memory while increasing the number of prefill steps; VexLife has not qualified a user-edit path yet.",
                ),
                descriptor(
                "field.vexlife-prod.runtime.kv-bits",
                FieldGroup.RUNTIME,
                "KV-cache quantization bits",
                "Optional KV-cache quantization bit depth exposed by the runtime. Null means the current audited runtime path does not enable it.",
                runtime.kvBits,
                runtimeOwner,
                UserMutability.HELD_UNTIL_QUALIFIED,
                userApplyBoundary = UserApplyBoundary.RUNTIME_RESTART,
                unit = "bits",
                ),
                descriptor(
                "field.vexlife-prod.runtime.kv-group-size",
                FieldGroup.RUNTIME,
                "KV quantization group size",
                "The runtime's KV quantization group size. It is dormant while KV quantization is not enabled.",
                runtime.kvGroupSize,
                runtimeOwner,
                UserMutability.HELD_UNTIL_QUALIFIED,
                userApplyBoundary = UserApplyBoundary.RUNTIME_RESTART,
                ),
                descriptor(
                "field.vexlife-prod.runtime.quantized-kv-start",
                FieldGroup.RUNTIME,
                "Quantized-KV start",
                "The runtime threshold after which KV quantization would begin. It is dormant while KV quantization is not enabled.",
                runtime.quantizedKvStart,
                runtimeOwner,
                UserMutability.HELD_UNTIL_QUALIFIED,
                userApplyBoundary = UserApplyBoundary.RUNTIME_RESTART,
                unit = "tokens",
                ),
                descriptor(
                "field.vexlife-prod.runtime.prompt-concurrency",
                FieldGroup.RUNTIME,
                "Prompt concurrency",
                "The pinned runtime's prompt-processing concurrency.",
                runtime.promptConcurrency,
                runtimeOwner,
                UserMutability.HELD_UNTIL_QUALIFIED,
                userApplyBoundary = UserApplyBoundary.RUNTIME_RESTART,
                ),
                descriptor(
                "field.vexlife-prod.runtime.decode-concurrency",
                FieldGroup.RUNTIME,
                "Decode concurrency",
                "The pinned runtime's decode concurrency.",
                runtime.decodeConcurrency,
                runtimeOwner,
                UserMutability.HELD_UNTIL_QUALIFIED,
                userApplyBoundary = UserApplyBoundary.RUNTIME_RESTART,
                ),
        )
    }

    private fun generationFields(
        generation: GenerationConfiguration,
    ): List<FieldDescriptor> {
        val representationOwner =
            "HereticCompanionRepresentation"
        return listOf(
                descriptor(
                "field.vexlife-prod.generation.message-protocol",
                FieldGroup.GENERATION,
                "Message protocol",
                "How VexLife represents messages to this model/provider relationship.",
                generation.messageProtocol,
                representationOwner,
                UserMutability.READ_ONLY_PRODUCT_POLICY,
                ),
                descriptor(
                "field.vexlife-prod.generation.reasoning-representation",
                FieldGroup.GENERATION,
                "Reasoning representation",
                "How this provider relationship carries model reasoning.",
                generation.reasoningRepresentation,
                representationOwner,
                UserMutability.READ_ONLY_PRODUCT_POLICY,
                ),
                descriptor(
                "field.vexlife-prod.generation.capability-representation",
                FieldGroup.GENERATION,
                "Capability representation",
                "How Vex capability requests are represented to this model/provider relationship.",
                generation.capabilityRepresentation,
                representationOwner,
                UserMutability.READ_ONLY_PRODUCT_POLICY,
                ),
                descriptor(
                "field.vexlife-prod.generation.reach-standing",
                FieldGroup.GENERATION,
                "Vex reach standing",
                "Whether this exact model/provider representation has earned ordinary Product capability reach.",
                generation.reachRepresentationStanding,
                representationOwner,
                UserMutability.READ_ONLY_PRODUCT_POLICY,
                ),
                descriptor(
                "field.vexlife-prod.generation.temperature",
                FieldGroup.GENERATION,
                "Temperature",
                "The selected model/provider sampling temperature.",
                generation.temperature,
                representationOwner,
                UserMutability.HELD_UNTIL_QUALIFIED,
                userApplyBoundary = UserApplyBoundary.NEXT_SEND,
                increaseConsequence = "Usually increases sampling variability; VexLife has not qualified a user-edit path for this model relationship yet.",
                decreaseConsequence = "Usually reduces sampling variability; VexLife has not qualified a user-edit path for this model relationship yet.",
                ),
                descriptor(
                "field.vexlife-prod.generation.top-p",
                FieldGroup.GENERATION,
                "Top-p",
                "The selected model/provider nucleus-sampling probability mass.",
                generation.topP,
                representationOwner,
                UserMutability.HELD_UNTIL_QUALIFIED,
                userApplyBoundary = UserApplyBoundary.NEXT_SEND,
                ),
                descriptor(
                "field.vexlife-prod.generation.top-k",
                FieldGroup.GENERATION,
                "Top-k",
                "The selected model/provider top-k sampling bound.",
                generation.topK,
                representationOwner,
                UserMutability.HELD_UNTIL_QUALIFIED,
                userApplyBoundary = UserApplyBoundary.NEXT_SEND,
                ),
                descriptor(
                "field.vexlife-prod.generation.reasoning-effort",
                FieldGroup.GENERATION,
                "Reasoning effort",
                "Provider-native reasoning effort when this exact model/provider profile exposes one.",
                generation.reasoningEffort,
                representationOwner,
                UserMutability.HELD_UNTIL_QUALIFIED,
                userApplyBoundary = UserApplyBoundary.NEXT_SEND,
                ),
                descriptor(
                "field.vexlife-prod.generation.maximum-output-tokens",
                FieldGroup.GENERATION,
                "Maximum output tokens",
                "The Product-owned upper bound for one model response.",
                generation.maximumOutputTokens,
                representationOwner,
                UserMutability.HELD_UNTIL_QUALIFIED,
                userApplyBoundary = UserApplyBoundary.NEXT_SEND,
                unit = "tokens",
                increaseConsequence = "Allows a longer single response but reserves more context-window room and can increase generation work.",
                decreaseConsequence = "Bounds responses more tightly and can leave more room for input, while truncating long answers sooner.",
                relatedFieldRefs =
                listOf(
                "field.vexlife-prod.continuity.input-token-budget",
                "field.vexlife-prod.continuity.hard-input-output-token-limit",
                ),
                ),
        )
    }

    private fun continuityFields(
        continuity: ContinuityConfiguration,
    ): List<FieldDescriptor> {
        val promptOwner =
            "DirectPromptContext"
        val memoryOwner =
            "VexLifeMemoryContract"
        val capabilityOwner =
            "VexLifeCapabilityBridge"
        return listOf(
                descriptor(
                "field.vexlife-prod.continuity.input-token-budget",
                FieldGroup.CONTINUITY,
                "Product input ceiling",
                "The Product ceiling for prompt input material before output reservation. A Context Profile may now request a lower per-turn ceiling; it does not rewrite this Product limit.",
                continuity.inputTokenBudget,
                promptOwner,
                UserMutability.READ_ONLY_PRODUCT_POLICY,
                unit = "tokens",
                increaseConsequence = "A higher Product ceiling could permit more prompt residency when the selected model/runtime also supports it.",
                decreaseConsequence = "A lower Product ceiling would constrain every profile and make working-continuity rollover pressure arrive sooner.",
                relatedFieldRefs =
                listOf(
                "field.vexlife-prod.model.native-context-tokens",
                "field.vexlife-prod.continuity.direct-turn-input-with-reach-tokens",
                "field.vexlife-prod.continuity.maximum-memory-tokens",
                "field.vexlife-prod.continuity.reserved-output-tokens",
                ),
                ),
                descriptor(
                "field.vexlife-prod.continuity.capability-exchange-reserve-tokens",
                FieldGroup.CONTINUITY,
                "Capability exchange reserve",
                "Prompt room VexLife keeps free so request/result capability exchanges can occur in the same human turn.",
                continuity.capabilityExchangeReserveTokens,
                capabilityOwner,
                UserMutability.READ_ONLY_PRODUCT_POLICY,
                unit = "tokens",
                relatedFieldRefs =
                listOf(
                "field.vexlife-prod.continuity.input-token-budget",
                "field.vexlife-prod.continuity.direct-turn-input-with-reach-tokens",
                ),
                ),
                descriptor(
                "field.vexlife-prod.continuity.direct-turn-input-with-reach-tokens",
                FieldGroup.CONTINUITY,
                "Context room with reach",
                "The ordinary prompt room left after the capability-exchange reserve. This is a derived consequence, not an independent user setting.",
                continuity.directTurnInputWithReachTokens,
                promptOwner,
                UserMutability.READ_ONLY_PRODUCT_POLICY,
                unit = "tokens",
                relatedFieldRefs =
                listOf(
                "field.vexlife-prod.continuity.input-token-budget",
                "field.vexlife-prod.continuity.capability-exchange-reserve-tokens",
                ),
                ),
                descriptor(
                "field.vexlife-prod.continuity.maximum-memory-tokens",
                FieldGroup.CONTINUITY,
                "Memory prompt budget",
                "The maximum accepted Memory material that may enter one prompt. Reducing prompt residency is not deletion or semantic forgetting.",
                continuity.maximumMemoryTokens,
                memoryOwner,
                UserMutability.HELD_UNTIL_QUALIFIED,
                userApplyBoundary = UserApplyBoundary.NEXT_SEND,
                unit = "tokens",
                increaseConsequence = "Allows more accepted Memory to remain prompt-resident when room exists, while consuming more of the active context budget.",
                decreaseConsequence = "Keeps less accepted Memory resident per turn; older accepted source remains durable and can be revisited through rightful retrieval.",
                relatedFieldRefs =
                listOf(
                "field.vexlife-prod.continuity.input-token-budget",
                ),
                ),
                descriptor(
                "field.vexlife-prod.continuity.maximum-requests-per-turn",
                FieldGroup.CONTINUITY,
                "Maximum capability requests per turn",
                "The Product bound on how many capability requests one human turn may admit.",
                continuity.maximumRequestsPerTurn,
                capabilityOwner,
                UserMutability.READ_ONLY_PRODUCT_POLICY,
                unit = "requests",
                ),
                descriptor(
                "field.vexlife-prod.continuity.reserved-output-tokens",
                FieldGroup.CONTINUITY,
                "Reserved output room",
                "Context-window room reserved for the model response.",
                continuity.reservedOutputTokens,
                promptOwner,
                UserMutability.READ_ONLY_PRODUCT_POLICY,
                unit = "tokens",
                relatedFieldRefs =
                listOf(
                "field.vexlife-prod.continuity.input-token-budget",
                "field.vexlife-prod.continuity.hard-input-output-token-limit",
                ),
                ),
                descriptor(
                "field.vexlife-prod.continuity.hard-input-output-token-limit",
                FieldGroup.CONTINUITY,
                "Hard input + output limit",
                "The Product's current absolute input/output token boundary for this direct path.",
                continuity.hardInputOutputTokenLimit,
                promptOwner,
                UserMutability.READ_ONLY_PRODUCT_POLICY,
                unit = "tokens",
                relatedFieldRefs =
                listOf(
                "field.vexlife-prod.model.native-context-tokens",
                "field.vexlife-prod.continuity.input-token-budget",
                "field.vexlife-prod.continuity.reserved-output-tokens",
                ),
                ),
                descriptor(
                "field.vexlife-prod.continuity.adaptive-context-policy",
                FieldGroup.CONTINUITY,
                "Adaptive context policy",
                "Whether VexLife currently has a qualified automatic host-pressure policy for changing active context depth. NOT_CONFIGURED means no such policy exists; the Product must not invent one.",
                EffectiveField(
                value = continuity.adaptiveContextPolicyState,
                provenance = Provenance.PRODUCT_SOURCE,
                changeBoundary = ChangeBoundary.PRODUCT_SOURCE_CHANGE,
                sourceRef =
                "src/jvmMain/kotlin/vexlife/model/heretic/runtime/HereticExecutionTopology.kt",
                ),
                promptOwner,
                UserMutability.READ_ONLY_PRODUCT_POLICY,
                ),
        )
    }

    private fun runtime(
        model: HereticModelCatalog.Entry,
        runtimeConfigurationValue: HereticRuntimeConfiguration.Value,
        modelForRef: (String) -> HereticModelCatalog.Entry?,
    ): RuntimeConfiguration {
        val version = model.identity.mlxLmVersion
        require(version == SupportedMlxLmVersion) {
            "Heretic execution topology has not audited mlx-lm defaults for $version"
        }

        val serverSource = "mlx-lm@$version:mlx_lm/server.py"
        val generateSource = "mlx-lm@$version:mlx_lm/generate.py"
        val runtimeConfigurationSource =
            "src/commonMain/kotlin/vexlife/model/heretic/HereticRuntimeConfiguration.kt"
        val validatedRuntimeConfiguration =
            HereticRuntimeConfiguration.validate(
                value = runtimeConfigurationValue,
                modelForRef = modelForRef,
            )
        val storedSetting =
            validatedRuntimeConfiguration.models.singleOrNull {
                it.modelRef == model.modelRef
            }
        val effectiveRuntimeConfiguration =
            HereticRuntimeConfiguration.effective(
                value = validatedRuntimeConfiguration,
                model = model,
                modelForRef = modelForRef,
            )
        val explicitHomeConfiguration =
            storedSetting?.mode ==
                HereticRuntimeConfiguration.ModeExplicit

        return RuntimeConfiguration(
            mlxLmVersion =
                EffectiveField(
                    value = version,
                    provenance = Provenance.PRODUCT_SOURCE,
                    changeBoundary = ChangeBoundary.PRODUCT_SOURCE_CHANGE,
                    sourceRef =
                        "src/commonMain/kotlin/vexlife/model/heretic/HereticProductIdentity.kt",
                ),
            promptCacheEntries =
                EffectiveField(
                    value = effectiveRuntimeConfiguration.promptCacheEntries,
                    provenance =
                        if (explicitHomeConfiguration) {
                            Provenance.PRODUCT_HOME_CONFIGURATION
                        } else {
                            Provenance.PRODUCT_SOURCE
                        },
                    changeBoundary = ChangeBoundary.RUNTIME_RESTART,
                    sourceRef =
                        if (explicitHomeConfiguration) {
                            FileHereticRuntimeConfigurationStore.RelativePath
                        } else {
                            runtimeConfigurationSource
                        },
                    inheritedDefault = MlxDefaults.PromptCacheEntries,
                    inheritedDefaultSourceRef = serverSource,
                ),
            promptCacheBytes =
                EffectiveField<Long?>(
                    value = null,
                    provenance = Provenance.PINNED_MLX_LM_DEFAULT,
                    changeBoundary = ChangeBoundary.RUNTIME_RESTART,
                    sourceRef = serverSource,
                    inheritedDefault = null,
                    inheritedDefaultSourceRef = serverSource,
                ),
            prefillStepSize = pinnedMlxDefault(MlxDefaults.PrefillStepSize, serverSource),
            kvBits = pinnedMlxDefault<Int?>(null, serverSource),
            kvGroupSize =
                pinnedMlxDefault(
                    MlxDefaults.KvGroupSize,
                    serverSource,
                    EffectiveState.DORMANT,
                ),
            quantizedKvStart =
                pinnedMlxDefault(
                    MlxDefaults.QuantizedKvStart,
                    generateSource,
                    EffectiveState.DORMANT,
                ),
            promptConcurrency = pinnedMlxDefault(MlxDefaults.PromptConcurrency, serverSource),
            decodeConcurrency = pinnedMlxDefault(MlxDefaults.DecodeConcurrency, serverSource),
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

                HereticCompanionRepresentation.SamplingBasis.PRODUCT_DEFAULT ->
                    Provenance.PRODUCT_SOURCE
            }

        fun <T> samplingField(value: T) =
            EffectiveField(
                value = value,
                provenance = samplingProvenance,
                changeBoundary = ChangeBoundary.PRODUCT_SOURCE_CHANGE,
                sourceRef = sampling.sourceRef,
            )

        fun <T> profileField(value: T) =
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
        fun field(value: Int, sourceRef: String) =
            EffectiveField(
                value = value,
                provenance = Provenance.PRODUCT_SOURCE,
                changeBoundary = ChangeBoundary.PRODUCT_SOURCE_CHANGE,
                sourceRef = sourceRef,
            )

        val promptSource = "src/jvmMain/kotlin/vexlife/companion/DirectPromptContext.kt"
        val memorySource = "src/commonMain/kotlin/vexlife/memory/VexLifeMemoryContract.kt"
        val capabilitySource =
            "src/jvmMain/kotlin/vexlife/capability/VexLifeCapabilityBridge.kt"

        return ContinuityConfiguration(
            inputTokenBudget = field(DirectPromptContext.InputTokenBudget, promptSource),
            capabilityExchangeReserveTokens =
                field(VexLifeCapabilityBridge.ExchangeAllowanceTokens, capabilitySource),
            directTurnInputWithReachTokens =
                field(DirectPromptContext.DirectTurnInputTokenBudget, promptSource),
            maximumMemoryTokens =
                field(VexLifeMemoryContract.MaximumMemoryTokens, memorySource),
            maximumRequestsPerTurn =
                field(VexLifeCapabilityBridge.MaximumRequestsPerTurn, capabilitySource),
            reservedOutputTokens = field(DirectPromptContext.ReservedOutputTokens, promptSource),
            hardInputOutputTokenLimit = field(DirectPromptContext.HardTokenLimit, promptSource),
            adaptiveContextPolicyState = "NOT_CONFIGURED",
        )
    }

    private fun <T> modelField(value: T, sourceRef: String) =
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
    ) =
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

    private fun JsonObject.array(name: String): JsonArray? = this[name] as? JsonArray

    private fun JsonObject.stringList(name: String): List<String>? =
        array(name)?.map { element ->
            (element as? JsonPrimitive)?.contentOrNull
                ?: error("Heretic model $name must contain only strings")
        }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
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
