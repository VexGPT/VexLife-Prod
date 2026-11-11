package vexlife.companion

import vexlife.memory.VexLifeMemoryContract

/**
 * Effect-free finite-attention ordering for one direct Companion prompt.
 *
 * The planner owns no content selection and performs no read, provider call, persistence or Memory
 * effect. It receives token facts already formed by rightful owners and preserves one relationship:
 *
 * fixed current-turn/context material
 * -> exact recent Conversation
 * -> whole present working carry when it fits remaining slack
 * -> caller-bounded Memory capacity from what remains
 */
object DirectPromptAttentionPlan {
    data class Input(
        val inputTokenBudget: Int =
            DirectPromptContext.InputTokenBudget,
        val fixedCurrentTurnTokens: Int,
        val selectedExactConversationTokens: Int,
        val carryRequested: Boolean,
        val carryEstimatedSystemMessageTokens: Int,
        val maximumMemoryTokens: Int =
            VexLifeMemoryContract.MaximumMemoryTokens,
    )

    data class Plan(
        val inputTokenBudget: Int,
        val fixedCurrentTurnTokens: Int,
        val conversationSelectionBudget: Int,
        val selectedExactConversationTokens: Int,
        val carryRequested: Boolean,
        val carryTokenAllowance: Int,
        val carryEstimatedSystemMessageTokens: Int,
        val carryIncluded: Boolean,
        val carrySelectedTokens: Int,
        val memoryTokenCap: Int,
        val unallocatedAfterMemoryCap: Int,
        val effectsPerformed: Boolean,
    )

    fun form(
        input: Input,
    ): Plan {
        require(
            input.inputTokenBudget >=
                0 &&
                input.inputTokenBudget <=
                DirectPromptContext.InputTokenBudget,
        ) {
            "prompt attention input budget is outside the admitted Product input budget"
        }
        require(
            input.fixedCurrentTurnTokens >=
                0 &&
                input.fixedCurrentTurnTokens <=
                input.inputTokenBudget,
        ) {
            "prompt attention fixed current-turn tokens exceed the input budget"
        }
        require(
            input.selectedExactConversationTokens >=
                0 &&
                input.carryEstimatedSystemMessageTokens >=
                0 &&
                input.maximumMemoryTokens >=
                0 &&
                input.maximumMemoryTokens <=
                VexLifeMemoryContract.MaximumMemoryTokens,
        ) {
            "prompt attention token facts must remain non-negative and inside current Memory bounds"
        }
        require(
            input.carryRequested ||
                input.carryEstimatedSystemMessageTokens ==
                0,
        ) {
            "prompt attention cannot reserve carry tokens when no carry is requested"
        }

        val conversationSelectionBudget =
            input.inputTokenBudget -
                input.fixedCurrentTurnTokens
        require(
            input.selectedExactConversationTokens <=
                conversationSelectionBudget,
        ) {
            "selected exact Conversation exceeds the budget made available before carry or Memory"
        }

        val carryAllowance =
            conversationSelectionBudget -
                input.selectedExactConversationTokens
        val carryIncluded =
            input.carryRequested &&
                input.carryEstimatedSystemMessageTokens <=
                carryAllowance
        val carrySelectedTokens =
            if (
                carryIncluded
            ) {
                input.carryEstimatedSystemMessageTokens
            } else {
                0
            }
        val afterCarry =
            carryAllowance -
                carrySelectedTokens
        val memoryTokenCap =
            minOf(
                input.maximumMemoryTokens,
                afterCarry,
            )
        val unallocated =
            afterCarry -
                memoryTokenCap

        return Plan(
            inputTokenBudget = input.inputTokenBudget,
            fixedCurrentTurnTokens = input.fixedCurrentTurnTokens,
            conversationSelectionBudget = conversationSelectionBudget,
            selectedExactConversationTokens = input.selectedExactConversationTokens,
            carryRequested = input.carryRequested,
            carryTokenAllowance = carryAllowance,
            carryEstimatedSystemMessageTokens = input.carryEstimatedSystemMessageTokens,
            carryIncluded = carryIncluded,
            carrySelectedTokens = carrySelectedTokens,
            memoryTokenCap = memoryTokenCap,
            unallocatedAfterMemoryCap = unallocated,
            effectsPerformed = false,
        )
            .also(
                ::validate,
            )
    }

    fun validate(
        value: Plan,
    ): Plan {
        val expectedConversationBudget =
            value.inputTokenBudget -
                value.fixedCurrentTurnTokens
        val expectedCarryAllowance =
            expectedConversationBudget -
                value.selectedExactConversationTokens
        val expectedCarryIncluded =
            value.carryRequested &&
                value.carryEstimatedSystemMessageTokens <=
                expectedCarryAllowance
        val expectedCarrySelected =
            if (
                expectedCarryIncluded
            ) {
                value.carryEstimatedSystemMessageTokens
            } else {
                0
            }
        val afterCarry =
            expectedCarryAllowance -
                expectedCarrySelected

        require(
            value.inputTokenBudget >=
                0 &&
                value.inputTokenBudget <=
                DirectPromptContext.InputTokenBudget &&
                value.fixedCurrentTurnTokens >=
                0 &&
                value.fixedCurrentTurnTokens <=
                value.inputTokenBudget &&
                value.conversationSelectionBudget ==
                expectedConversationBudget &&
                value.selectedExactConversationTokens >=
                0 &&
                value.selectedExactConversationTokens <=
                expectedConversationBudget &&
                value.carryTokenAllowance ==
                expectedCarryAllowance &&
                value.carryEstimatedSystemMessageTokens >=
                0 &&
                value.carryIncluded ==
                expectedCarryIncluded &&
                value.carrySelectedTokens ==
                expectedCarrySelected &&
                value.memoryTokenCap >=
                0 &&
                value.memoryTokenCap <=
                VexLifeMemoryContract.MaximumMemoryTokens &&
                value.memoryTokenCap <=
                afterCarry &&
                value.unallocatedAfterMemoryCap ==
                afterCarry -
                value.memoryTokenCap &&
                value.unallocatedAfterMemoryCap >=
                0 &&
                !value.effectsPerformed,
        ) {
            "prompt attention plan is internally inconsistent"
        }
        return value
    }

}

// [VXG RealForever]
