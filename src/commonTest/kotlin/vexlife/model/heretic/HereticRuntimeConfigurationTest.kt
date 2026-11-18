package vexlife.model.heretic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HereticRuntimeConfigurationTest {
    @Test
    fun defaultsPreserveTheExistingSafetyFloorWithoutAutoApplyingRecommendations() {
        val empty =
            HereticRuntimeConfiguration.empty()

        val twentySeven =
            HereticRuntimeConfiguration.effective(
                empty,
                HereticModelCatalog.Models.Qwen27B,
            )
        assertEquals(
            1,
            twentySeven.promptCacheEntries,
        )
        assertEquals(
            1,
            twentySeven.recommendedPromptCacheEntries,
        )
        assertEquals(
            1,
            twentySeven.maximumPromptCacheEntries,
        )
        assertEquals(
            HereticRuntimeConfiguration.RecommendationLivedSafetyFloor,
            twentySeven.recommendationStanding,
        )

        val four =
            HereticRuntimeConfiguration.effective(
                empty,
                HereticModelCatalog.Models.Qwen4B,
            )
        assertEquals(
            1,
            four.promptCacheEntries,
        )
        assertEquals(
            1,
            four.recommendedPromptCacheEntries,
        )
        assertEquals(
            3,
            four.maximumPromptCacheEntries,
        )
        assertEquals(
            HereticRuntimeConfiguration.RecommendationHeldExperiment,
            four.recommendationStanding,
        )
    }

    @Test
    fun legacyRecommendedCandidateConfigurationStillReadsTheCurrentSourceRecommendation() {
        val configured =
            HereticRuntimeConfiguration.withLegacyRecommended(
                value = HereticRuntimeConfiguration.empty(),
                model = HereticModelCatalog.Models.Qwen4B,
            )
        val effective =
            HereticRuntimeConfiguration.effective(
                configured,
                HereticModelCatalog.Models.Qwen4B,
            )

        assertEquals(
            HereticRuntimeConfiguration.ModeLegacyRecommended,
            effective.mode,
        )
        assertEquals(
            1,
            effective.promptCacheEntries,
        )
        assertEquals(
            3,
            effective.maximumPromptCacheEntries,
        )
        assertEquals(
            HereticRuntimeConfiguration.RecommendationHeldExperiment,
            effective.recommendationStanding,
        )
    }

    @Test
    fun explicitValuesCannotExceedTheModelSpecificEarnedRange() {
        val two =
            HereticRuntimeConfiguration.withPromptCacheEntries(
                value = HereticRuntimeConfiguration.empty(),
                model = HereticModelCatalog.Models.Qwen4B,
                promptCacheEntries = 2,
            )
        assertEquals(
            2,
            HereticRuntimeConfiguration
                .effective(
                    two,
                    HereticModelCatalog.Models.Qwen4B,
                )
                .promptCacheEntries,
        )

        assertFailsWith<IllegalArgumentException> {
            HereticRuntimeConfiguration.withPromptCacheEntries(
                value = HereticRuntimeConfiguration.empty(),
                model = HereticModelCatalog.Models.Qwen4B,
                promptCacheEntries = 4,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            HereticRuntimeConfiguration.withPromptCacheEntries(
                value = HereticRuntimeConfiguration.empty(),
                model = HereticModelCatalog.Models.Qwen27B,
                promptCacheEntries = 2,
            )
        }
    }
}

// [VXG RealForever]
