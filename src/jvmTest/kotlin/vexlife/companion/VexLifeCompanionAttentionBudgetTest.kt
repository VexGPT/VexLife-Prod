package vexlife.companion

import kotlin.test.Test
import kotlin.test.assertEquals

class VexLifeCompanionAttentionBudgetTest {
    @Test
    fun directArtifactCeilingConsumesOnlyOneQuarterOfOrdinaryRuntimeInput() {
        assertEquals(
            42_000,
            VexLifeCompanionAttentionBudget.directArtifactTokenCeiling(
                runtimeContextWindowTokens = null,
                requestedGrossInputTokenBudget = 192_000,
                capabilityRequestsAdmitted = true,
            ),
        )
        assertEquals(
            10_000,
            VexLifeCompanionAttentionBudget.directArtifactTokenCeiling(
                runtimeContextWindowTokens = null,
                requestedGrossInputTokenBudget = 64_000,
                capabilityRequestsAdmitted = true,
            ),
        )
        assertEquals(
            7_000,
            VexLifeCompanionAttentionBudget.directArtifactTokenCeiling(
                runtimeContextWindowTokens = null,
                requestedGrossInputTokenBudget = 52_000,
                capabilityRequestsAdmitted = true,
            ),
        )
        assertEquals(
            2_304,
            VexLifeCompanionAttentionBudget.directArtifactTokenCeiling(
                runtimeContextWindowTokens = 16_384,
                requestedGrossInputTokenBudget = 192_000,
                capabilityRequestsAdmitted = true,
            ),
        )
    }

    @Test
    fun providerNativeTurnWithoutCapabilityExchangeMayUseOneQuarterOfGrossInput() {
        assertEquals(
            48_000,
            VexLifeCompanionAttentionBudget.directArtifactTokenCeiling(
                runtimeContextWindowTokens = null,
                requestedGrossInputTokenBudget = 192_000,
                capabilityRequestsAdmitted = false,
            ),
        )
    }
}

// [VXG RealForever]
