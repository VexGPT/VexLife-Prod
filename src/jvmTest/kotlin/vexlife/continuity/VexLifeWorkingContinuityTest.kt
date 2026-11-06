package vexlife.continuity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VexLifeWorkingContinuityTest {
    private fun input(
        estimate: Int,
        boundedStatements: Int =
            0,
        boundedPages: Int =
            0,
    ): VexLifeWorkingContinuity.PromptPressureInput =
        VexLifeWorkingContinuity.PromptPressureInput(
            promptReceiptRef =
                "receipt.vexlife.prompt-context.test",
            providerMaterializedInputTokenEstimate =
                estimate,
            inputTokenBudget =
                192_000,
            reservedOutputTokens =
                16_000,
            hardTokenLimit =
                208_000,
            memoryBoundedOutStatementCount =
                boundedStatements,
            memoryTokenBoundedOutPageCount =
                boundedPages,
            sourceRefs =
                listOf(
                    "source.vexlife.prompt-context.test",
                ),
        )

    private fun workingSet(
        statementRefs: List<String> =
            listOf(
                "statement.vexlife.test.001",
            ),
    ): VexLifeWorkingContinuity.WorkingSet =
        VexLifeWorkingContinuity.WorkingSet(
            lineageRef =
                "companion-lineage.vexlife.test",
            threadRef =
                "thread.self-development.open-conversation",
            cursorEventRef =
                "event.vexlife.request.test",
            activeWorkNodeRefs =
                listOf(
                    "work-node.vexlife.test",
                ),
            currentIntentReceiptRefs =
                listOf(
                    "receipt.vexlife.intent.test",
                ),
            openLoopRefs =
                listOf(
                    "loop.vexlife.test",
                ),
            currentStatementRefs =
                statementRefs,
            currentContinuityRecordRefs =
                listOf(
                    "continuity.vexlife.test",
                ),
            currentDailyStratumRefOrNull =
                "daily-stratum.vexlife.test",
            sourceRefs =
                listOf(
                    "source.vexlife.working-set.test",
                ),
        )

    @Test
    fun pressureThresholdsPreserveStableRolloverMeaning() {
        val reserve =
            16_000

        assertEquals(
            VexLifeWorkingContinuity.PressureState.Normal,
            VexLifeWorkingContinuity
                .projectPressure(
                    input(
                        estimate =
                            192_000 -
                                (
                                    reserve *
                                        3
                                ) -
                                1,
                    ),
                )
                .state,
        )
        assertEquals(
            VexLifeWorkingContinuity.PressureState.Watch,
            VexLifeWorkingContinuity
                .projectPressure(
                    input(
                        estimate =
                            192_000 -
                                (
                                    reserve *
                                        3
                                ),
                    ),
                )
                .state,
        )
        assertEquals(
            VexLifeWorkingContinuity.PressureState.CheckpointRecommended,
            VexLifeWorkingContinuity
                .projectPressure(
                    input(
                        estimate =
                            192_000 -
                                (
                                    reserve *
                                        2
                                ),
                    ),
                )
                .state,
        )
        assertEquals(
            VexLifeWorkingContinuity.PressureState.RolloverRequired,
            VexLifeWorkingContinuity
                .projectPressure(
                    input(
                        estimate =
                            192_000 -
                                reserve,
                    ),
                )
                .state,
        )
    }

    @Test
    fun memoryBoundedOutRaisesNormalPressureToCheckpointRecommended() {
        val pressure =
            VexLifeWorkingContinuity.projectPressure(
                input(
                    estimate = 1_000,
                    boundedStatements = 1,
                ),
            )

        assertEquals(
            VexLifeWorkingContinuity.PressureState.CheckpointRecommended,
            pressure.state,
        )
        assertTrue(
            "MEMORY_CONTINUITY_BOUNDED_OUT" in
                pressure.reasonCodes,
        )
        assertFalse(
            "PRESSURE_WITHIN_NORMAL_BOUNDS" in
                pressure.reasonCodes,
        )
        assertFalse(
            pressure.checkpointEffectPerformed,
        )
        assertFalse(
            pressure.rolloverEffectPerformed,
        )
    }

    @Test
    fun checkpointPlanIsReferenceOnlyAndDoesNotPromoteMemoryOrModelOutput() {
        val plan =
            VexLifeWorkingContinuity.formPlan(
                pressureInput =
                    input(
                        estimate =
                            192_000 -
                                32_000,
                    ),
                workingSet =
                    workingSet(),
            )

        assertEquals(
            VexLifeWorkingContinuity.PlanState.CheckpointReady,
            plan.state,
        )
        val checkpoint =
            assertNotNull(
                plan.checkpoint,
            )
        assertTrue(
            checkpoint.checkpointTruth.referenceOnly,
        )
        assertFalse(
            checkpoint.checkpointTruth.rawTranscriptIncluded,
        )
        assertFalse(
            checkpoint.checkpointTruth.modelSummaryIncluded,
        )
        assertFalse(
            checkpoint.semanticMemoryPromotionAuthorized,
        )
        assertFalse(
            checkpoint.modelOutputAcceptedAsMemory,
        )
        assertFalse(
            plan.effectAuthorityGranted,
        )
        assertEquals(
            VexLifeWorkingContinuity.PlanEffects(),
            plan.effects,
        )
    }

    @Test
    fun rolloverPlanNamesSuccessorContextAsNextEffectWithoutPerformingIt() {
        val plan =
            VexLifeWorkingContinuity.formPlan(
                pressureInput =
                    input(
                        estimate =
                            192_000 -
                                16_000,
                    ),
                workingSet =
                    workingSet(),
            )

        assertEquals(
            VexLifeWorkingContinuity.PlanState.RolloverReady,
            plan.state,
        )
        assertNotNull(
            plan.checkpoint,
        )
        assertEquals(
            "DELEGATE_CHECKPOINT_THEN_FORM_EXACT_SUCCESSOR_CONTEXT",
            plan.nextSafeAction,
        )
        assertFalse(
            plan.effects.checkpointPersisted,
        )
        assertFalse(
            plan.effects.successorContextIssued,
        )
    }

    @Test
    fun normalAndWatchPressureDoNotManufactureCheckpointTruth() {
        for (
            estimate in
            listOf(
                1_000,
                192_000 -
                    48_000,
            )
        ) {
            val plan =
                VexLifeWorkingContinuity.formPlan(
                    pressureInput =
                        input(
                            estimate = estimate,
                        ),
                    workingSet =
                        workingSet(),
                )

            assertEquals(
                VexLifeWorkingContinuity.PlanState.NoCheckpointRequired,
                plan.state,
            )
            assertNull(
                plan.checkpoint,
            )
        }
    }

    @Test
    fun checkpointIdentityIsOrderingStableButChangesWithWorkingTruth() {
        val first =
            VexLifeWorkingContinuity.formPlan(
                pressureInput =
                    input(
                        estimate =
                            160_000,
                    ),
                workingSet =
                    workingSet(
                        statementRefs =
                            listOf(
                                "statement.vexlife.test.002",
                                "statement.vexlife.test.001",
                            ),
                    ),
            )
        val second =
            VexLifeWorkingContinuity.formPlan(
                pressureInput =
                    input(
                        estimate =
                            160_000,
                    ),
                workingSet =
                    workingSet(
                        statementRefs =
                            listOf(
                                "statement.vexlife.test.001",
                                "statement.vexlife.test.002",
                            ),
                    ),
            )
        val changed =
            VexLifeWorkingContinuity.formPlan(
                pressureInput =
                    input(
                        estimate =
                            160_000,
                    ),
                workingSet =
                    workingSet(
                        statementRefs =
                            listOf(
                                "statement.vexlife.test.003",
                            ),
                    ),
            )

        assertEquals(
            first.checkpoint
                ?.semanticFingerprint,
            second.checkpoint
                ?.semanticFingerprint,
        )
        assertTrue(
            first.checkpoint
                ?.semanticFingerprint !=
                changed.checkpoint
                    ?.semanticFingerprint,
        )
    }

    @Test
    fun priorCheckpointMustBeCurrentAndBoundToTheSameLineageThread() {
        val first =
            VexLifeWorkingContinuity.formPlan(
                pressureInput =
                    input(
                        estimate =
                            160_000,
                    ),
                workingSet =
                    workingSet(),
            )
        val prior =
            assertNotNull(
                first.checkpoint,
            )
        val next =
            VexLifeWorkingContinuity.formPlan(
                pressureInput =
                    input(
                        estimate =
                            160_000,
                    ),
                workingSet =
                    workingSet(
                        statementRefs =
                            listOf(
                                "statement.vexlife.test.002",
                            ),
                    ),
                priorCheckpoint = prior,
            )

        assertEquals(
            prior.checkpointRef,
            next.checkpoint
                ?.priorCheckpointRefOrNull,
        )
        assertEquals(
            prior.semanticFingerprint,
            next.checkpoint
                ?.priorCheckpointFingerprintOrNull,
        )
    }
}

// [VXG RealForever]
