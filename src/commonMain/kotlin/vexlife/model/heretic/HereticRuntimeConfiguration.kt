package vexlife.model.heretic

import kotlinx.serialization.Serializable

/**
 * Model-specific local runtime configuration exposed through Settings -> Models.
 *
 * Model choice remains owned by the model configuration relationship; this owner answers only how an
 * already-selected Heretic model should be launched. Defaults preserve the current lived safety floor.
 * Recommendations are evidence labels, not automatic authority to change a running model.
 */
object HereticRuntimeConfiguration {
    const val Schema = "vexlife-prod.heretic-runtime-configuration/v1"

    const val MinimumPromptCacheEntries = 1
    const val MaximumPromptCacheEntries = 10
    const val SafetyFloorPromptCacheEntries = 1
    const val Qwen4BExperimentalPromptCacheEntries = 3

    const val ModeExplicit = "EXPLICIT"
    /** Read compatibility for predecessor candidate files. Current Product writers persist explicit values. */
    const val ModeLegacyRecommended = "RECOMMENDED"

    const val RecommendationLivedSafetyFloor = "LIVED_SAFETY_FLOOR"
    const val RecommendationExperimentalCandidate = "EXPERIMENTAL_CANDIDATE"
    const val RecommendationHeldExperiment = "HELD_EXPERIMENT"
    const val RecommendationSafeFloorUntuned = "SAFE_FLOOR_UNTUNED"

    @Serializable
    data class ModelSetting(
        val modelRef: String,
        val mode: String,
        val promptCacheEntries: Int? = null,
    )

    @Serializable
    data class Value(
        val schemaVersion: String = Schema,
        val models: List<ModelSetting> = emptyList(),
    )

    data class Recommendation(
        val promptCacheEntries: Int,
        val maximumPromptCacheEntries: Int,
        val standing: String,
        val reasonKey: String,
    )

    data class Effective(
        val promptCacheEntries: Int,
        val mode: String,
        val recommendedPromptCacheEntries: Int,
        val recommendationStanding: String,
        val recommendationReasonKey: String,
        val minimumPromptCacheEntries: Int,
        val maximumPromptCacheEntries: Int,
    )

    fun empty(): Value = Value()

    fun validate(
        value: Value,
        modelForRef: (String) -> HereticModelCatalog.Entry? = HereticModelCatalog::entry,
    ): Value {
        require(value.schemaVersion == Schema) { "Heretic runtime configuration schema is not admitted" }
        require(value.models.map { it.modelRef }.distinct().size == value.models.size) {
            "Heretic runtime configuration contains duplicate model settings"
        }
        require(value.models == value.models.sortedBy { it.modelRef }) {
            "Heretic runtime configuration model settings must be canonical"
        }
        value.models.forEach { setting ->
            val model =
                requireNotNull(
                    modelForRef(
                        setting.modelRef,
                    ),
                ) {
                    "Heretic runtime configuration references an unoffered model"
                }
            require(setting.mode == ModeExplicit || setting.mode == ModeLegacyRecommended) {
                "Heretic runtime configuration mode is not admitted"
            }
            when (setting.mode) {
                ModeExplicit ->
                    requirePromptCacheEntriesFor(
                        model = model,
                        value =
                            requireNotNull(
                                setting.promptCacheEntries,
                            ) {
                                "explicit Heretic runtime configuration requires prompt-cache entries"
                            },
                    )

                ModeLegacyRecommended ->
                    require(setting.promptCacheEntries == null) {
                        "recommended Heretic runtime configuration must derive its value"
                    }
            }
        }
        return value
    }

    fun recommendation(model: HereticModelCatalog.Entry): Recommendation =
        when (model.modelRef) {
            HereticModelCatalog.Models.Qwen27B.modelRef ->
                Recommendation(
                    promptCacheEntries = SafetyFloorPromptCacheEntries,
                    maximumPromptCacheEntries = SafetyFloorPromptCacheEntries,
                    standing = RecommendationLivedSafetyFloor,
                    reasonKey = "workspace.description.model-cache-recommendation-27b",
                )

            HereticModelCatalog.Models.Qwen4B.modelRef ->
                Recommendation(
                    // The lived cache-3 run started successfully but then remained in one provider
                    // generation for 7+ minutes while the person observed repetitive output.
                    // Preserve 1 as the recommendation until a focused successor earns >1.
                    promptCacheEntries = SafetyFloorPromptCacheEntries,
                    maximumPromptCacheEntries = Qwen4BExperimentalPromptCacheEntries,
                    standing = RecommendationHeldExperiment,
                    reasonKey = "workspace.description.model-cache-recommendation-4b",
                )

            else ->
                Recommendation(
                    promptCacheEntries = SafetyFloorPromptCacheEntries,
                    maximumPromptCacheEntries = SafetyFloorPromptCacheEntries,
                    standing = RecommendationSafeFloorUntuned,
                    reasonKey = "workspace.description.model-cache-recommendation-untuned",
                )
        }

    fun effective(
        value: Value,
        model: HereticModelCatalog.Entry,
        modelForRef: (String) -> HereticModelCatalog.Entry? = HereticModelCatalog::entry,
    ): Effective {
        val current = validate(value, modelForRef)
        val recommendation = recommendation(model)
        val setting = current.models.singleOrNull { it.modelRef == model.modelRef }
        return when (setting?.mode) {
            ModeLegacyRecommended ->
                Effective(
                    promptCacheEntries = recommendation.promptCacheEntries,
                    mode = ModeLegacyRecommended,
                    recommendedPromptCacheEntries = recommendation.promptCacheEntries,
                    recommendationStanding = recommendation.standing,
                    recommendationReasonKey = recommendation.reasonKey,
                    minimumPromptCacheEntries = MinimumPromptCacheEntries,
                    maximumPromptCacheEntries = recommendation.maximumPromptCacheEntries,
                )

            ModeExplicit ->
                Effective(
                    promptCacheEntries =
                        requirePromptCacheEntriesFor(
                            model = model,
                            value = requireNotNull(setting.promptCacheEntries),
                        ),
                    mode = ModeExplicit,
                    recommendedPromptCacheEntries = recommendation.promptCacheEntries,
                    recommendationStanding = recommendation.standing,
                    recommendationReasonKey = recommendation.reasonKey,
                    minimumPromptCacheEntries = MinimumPromptCacheEntries,
                    maximumPromptCacheEntries = recommendation.maximumPromptCacheEntries,
                )

            else ->
                Effective(
                    promptCacheEntries = SafetyFloorPromptCacheEntries,
                    mode = ModeExplicit,
                    recommendedPromptCacheEntries = recommendation.promptCacheEntries,
                    recommendationStanding = recommendation.standing,
                    recommendationReasonKey = recommendation.reasonKey,
                    minimumPromptCacheEntries = MinimumPromptCacheEntries,
                    maximumPromptCacheEntries = recommendation.maximumPromptCacheEntries,
                )
        }
    }

    fun withPromptCacheEntries(
        value: Value,
        model: HereticModelCatalog.Entry,
        promptCacheEntries: Int,
        modelForRef: (String) -> HereticModelCatalog.Entry? = HereticModelCatalog::entry,
    ): Value =
        withSetting(
            value,
            ModelSetting(
                modelRef = model.modelRef,
                mode = ModeExplicit,
                promptCacheEntries =
                    requirePromptCacheEntriesFor(
                        model = model,
                        value = promptCacheEntries,
                    ),
            ),
            modelForRef,
        )

    /**
     * Compatibility constructor for predecessor candidate records.
     *
     * Current Product writers use the recommendation's numeric value as one explicit Home setting;
     * they do not subscribe the Home to future recommendation changes.
     */
    internal fun withLegacyRecommended(
        value: Value,
        model: HereticModelCatalog.Entry,
        modelForRef: (String) -> HereticModelCatalog.Entry? = HereticModelCatalog::entry,
    ): Value =
        withSetting(
            value,
            ModelSetting(
                modelRef = model.modelRef,
                mode = ModeLegacyRecommended,
            ),
            modelForRef,
        )

    private fun withSetting(
        value: Value,
        setting: ModelSetting,
        modelForRef: (String) -> HereticModelCatalog.Entry?,
    ): Value =
        Value(
            models =
                (validate(value, modelForRef).models.filterNot { it.modelRef == setting.modelRef } +
                        setting)
                    .sortedBy { it.modelRef },
        ).let { validate(it, modelForRef) }

    fun requirePromptCacheEntriesFor(
        model: HereticModelCatalog.Entry,
        value: Int,
    ): Int {
        val admitted = requirePromptCacheEntries(value)
        val maximum =
            recommendation(
                model,
            )
                .maximumPromptCacheEntries
        require(admitted <= maximum) {
            "prompt-cache entries for ${model.handle} must be in $MinimumPromptCacheEntries..$maximum"
        }
        return admitted
    }

    /**
     * Low-level mlx-lm contract bound. Product-facing model settings are further narrowed by
     * [requirePromptCacheEntriesFor].
     */
    fun requirePromptCacheEntries(value: Int): Int {
        require(value in MinimumPromptCacheEntries..MaximumPromptCacheEntries) {
            "prompt-cache entries must be in $MinimumPromptCacheEntries..$MaximumPromptCacheEntries"
        }
        return value
    }
}

// [VXG RealForever]
