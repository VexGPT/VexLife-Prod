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
    const val ModeRecommended = "RECOMMENDED"

    const val RecommendationLivedSafetyFloor = "LIVED_SAFETY_FLOOR"
    const val RecommendationExperimentalCandidate = "EXPERIMENTAL_CANDIDATE"
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
        val standing: String,
        val reasonKey: String,
    )

    data class Effective(
        val promptCacheEntries: Int,
        val mode: String,
        val recommendedPromptCacheEntries: Int,
        val recommendationStanding: String,
        val recommendationReasonKey: String,
    )

    fun empty(): Value = Value()

    fun validate(value: Value): Value {
        require(value.schemaVersion == Schema) { "Heretic runtime configuration schema is not admitted" }
        require(value.models.map { it.modelRef }.distinct().size == value.models.size) {
            "Heretic runtime configuration contains duplicate model settings"
        }
        require(value.models == value.models.sortedBy { it.modelRef }) {
            "Heretic runtime configuration model settings must be canonical"
        }
        value.models.forEach { setting ->
            require(HereticModelCatalog.entry(setting.modelRef) != null) {
                "Heretic runtime configuration references an unoffered model"
            }
            require(setting.mode == ModeExplicit || setting.mode == ModeRecommended) {
                "Heretic runtime configuration mode is not admitted"
            }
            when (setting.mode) {
                ModeExplicit ->
                    requirePromptCacheEntries(
                        requireNotNull(setting.promptCacheEntries) {
                            "explicit Heretic runtime configuration requires prompt-cache entries"
                        }
                    )

                ModeRecommended ->
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
                    standing = RecommendationLivedSafetyFloor,
                    reasonKey = "workspace.description.model-cache-recommendation-27b",
                )

            HereticModelCatalog.Models.Qwen4B.modelRef ->
                Recommendation(
                    promptCacheEntries = Qwen4BExperimentalPromptCacheEntries,
                    standing = RecommendationExperimentalCandidate,
                    reasonKey = "workspace.description.model-cache-recommendation-4b",
                )

            else ->
                Recommendation(
                    promptCacheEntries = SafetyFloorPromptCacheEntries,
                    standing = RecommendationSafeFloorUntuned,
                    reasonKey = "workspace.description.model-cache-recommendation-untuned",
                )
        }

    fun effective(
        value: Value,
        model: HereticModelCatalog.Entry,
    ): Effective {
        val current = validate(value)
        val recommendation = recommendation(model)
        val setting = current.models.singleOrNull { it.modelRef == model.modelRef }
        return when (setting?.mode) {
            ModeRecommended ->
                Effective(
                    promptCacheEntries = recommendation.promptCacheEntries,
                    mode = ModeRecommended,
                    recommendedPromptCacheEntries = recommendation.promptCacheEntries,
                    recommendationStanding = recommendation.standing,
                    recommendationReasonKey = recommendation.reasonKey,
                )

            ModeExplicit ->
                Effective(
                    promptCacheEntries = requirePromptCacheEntries(requireNotNull(setting.promptCacheEntries)),
                    mode = ModeExplicit,
                    recommendedPromptCacheEntries = recommendation.promptCacheEntries,
                    recommendationStanding = recommendation.standing,
                    recommendationReasonKey = recommendation.reasonKey,
                )

            else ->
                Effective(
                    promptCacheEntries = SafetyFloorPromptCacheEntries,
                    mode = ModeExplicit,
                    recommendedPromptCacheEntries = recommendation.promptCacheEntries,
                    recommendationStanding = recommendation.standing,
                    recommendationReasonKey = recommendation.reasonKey,
                )
        }
    }

    fun withPromptCacheEntries(
        value: Value,
        model: HereticModelCatalog.Entry,
        promptCacheEntries: Int,
    ): Value =
        withSetting(
            value,
            ModelSetting(
                modelRef = model.modelRef,
                mode = ModeExplicit,
                promptCacheEntries = requirePromptCacheEntries(promptCacheEntries),
            ),
        )

    fun withRecommended(
        value: Value,
        model: HereticModelCatalog.Entry,
    ): Value =
        withSetting(
            value,
            ModelSetting(
                modelRef = model.modelRef,
                mode = ModeRecommended,
            ),
        )

    private fun withSetting(
        value: Value,
        setting: ModelSetting,
    ): Value =
        Value(
            models =
                (validate(value).models.filterNot { it.modelRef == setting.modelRef } + setting)
                    .sortedBy { it.modelRef },
        ).let(::validate)

    fun requirePromptCacheEntries(value: Int): Int {
        require(value in MinimumPromptCacheEntries..MaximumPromptCacheEntries) {
            "prompt-cache entries must be in $MinimumPromptCacheEntries..$MaximumPromptCacheEntries"
        }
        return value
    }
}

// [VXG RealForever]
