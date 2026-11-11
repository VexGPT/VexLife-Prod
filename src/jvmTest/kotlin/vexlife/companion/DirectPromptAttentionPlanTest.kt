package vexlife.companion

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DirectPromptAttentionPlanTest {
    @Test
    fun exactConversationIsSelectedBeforeCarryAndCarryCannotEvictIt() {
        val plan =
            DirectPromptAttentionPlan.form(
                DirectPromptAttentionPlan.Input(
                    inputTokenBudget = 100,
                    fixedCurrentTurnTokens = 20,
                    selectedExactConversationTokens = 70,
                    carryRequested = true,
                    carryEstimatedSystemMessageTokens = 20,
                    maximumMemoryTokens = 50,
                ),
            )

        assertEquals(
            80,
            plan.conversationSelectionBudget,
        )
        assertEquals(
            70,
            plan.selectedExactConversationTokens,
        )
        assertEquals(
            10,
            plan.carryTokenAllowance,
        )
        assertFalse(
            plan.carryIncluded,
        )
        assertEquals(
            0,
            plan.carrySelectedTokens,
        )
        assertEquals(
            10,
            plan.memoryTokenCap,
        )
        assertEquals(
            0,
            plan.unallocatedAfterMemoryCap,
        )
        assertFalse(
            plan.effectsPerformed,
        )
    }

    @Test
    fun wholeCarryMayUseOnlySlackLeftAfterExactConversation() {
        val plan =
            DirectPromptAttentionPlan.form(
                DirectPromptAttentionPlan.Input(
                    inputTokenBudget = 100,
                    fixedCurrentTurnTokens = 20,
                    selectedExactConversationTokens = 50,
                    carryRequested = true,
                    carryEstimatedSystemMessageTokens = 15,
                    maximumMemoryTokens = 100,
                ),
            )

        assertEquals(
            30,
            plan.carryTokenAllowance,
        )
        assertTrue(
            plan.carryIncluded,
        )
        assertEquals(
            15,
            plan.carrySelectedTokens,
        )
        assertEquals(
            15,
            plan.memoryTokenCap,
        )
    }

    @Test
    fun omittedCarryDoesNotReserveSpaceAndMemoryGetsTheRemainingCapacity() {
        val plan =
            DirectPromptAttentionPlan.form(
                DirectPromptAttentionPlan.Input(
                    inputTokenBudget = 120,
                    fixedCurrentTurnTokens = 20,
                    selectedExactConversationTokens = 60,
                    carryRequested = false,
                    carryEstimatedSystemMessageTokens = 0,
                    maximumMemoryTokens = 100,
                ),
            )

        assertFalse(
            plan.carryIncluded,
        )
        assertEquals(
            0,
            plan.carrySelectedTokens,
        )
        assertEquals(
            40,
            plan.memoryTokenCap,
        )
    }

    @Test
    fun memoryMaximumDoesNotCreateAuthorityToConsumeUnusedPromptSpace() {
        val plan =
            DirectPromptAttentionPlan.form(
                DirectPromptAttentionPlan.Input(
                    inputTokenBudget = 200,
                    fixedCurrentTurnTokens = 20,
                    selectedExactConversationTokens = 40,
                    carryRequested = true,
                    carryEstimatedSystemMessageTokens = 10,
                    maximumMemoryTokens = 50,
                ),
            )

        assertTrue(
            plan.carryIncluded,
        )
        assertEquals(
            50,
            plan.memoryTokenCap,
        )
        assertEquals(
            80,
            plan.unallocatedAfterMemoryCap,
        )
    }

    @Test
    fun invalidInputsCannotMakeCarryOrConversationOverdrawFiniteAttention() {
        assertFailsWith<IllegalArgumentException> {
            DirectPromptAttentionPlan.form(
                DirectPromptAttentionPlan.Input(
                    inputTokenBudget = 100,
                    fixedCurrentTurnTokens = 30,
                    selectedExactConversationTokens = 71,
                    carryRequested = false,
                    carryEstimatedSystemMessageTokens = 0,
                    maximumMemoryTokens = 0,
                ),
            )
        }

        assertFailsWith<IllegalArgumentException> {
            DirectPromptAttentionPlan.form(
                DirectPromptAttentionPlan.Input(
                    inputTokenBudget = 100,
                    fixedCurrentTurnTokens = 20,
                    selectedExactConversationTokens = 40,
                    carryRequested = false,
                    carryEstimatedSystemMessageTokens = 1,
                    maximumMemoryTokens = 20,
                ),
            )
        }

        assertFailsWith<IllegalArgumentException> {
            DirectPromptAttentionPlan.form(
                DirectPromptAttentionPlan.Input(
                    inputTokenBudget =
                        DirectPromptContext.InputTokenBudget +
                            1,
                    fixedCurrentTurnTokens = 0,
                    selectedExactConversationTokens = 0,
                    carryRequested = false,
                    carryEstimatedSystemMessageTokens = 0,
                    maximumMemoryTokens = 0,
                ),
            )
        }
    }
}

// [VXG RealForever]
