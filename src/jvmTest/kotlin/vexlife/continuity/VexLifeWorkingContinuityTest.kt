package vexlife.continuity

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vexlife.contract.VexContractJson

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
    fun tamperedPriorCheckpointFailsBeforeItCanParticipateInSupersession() {
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
        val tampered =
            prior.copy(
                semanticFingerprint =
                    "0".repeat(
                        64,
                    ),
            )

        assertFailsWith<IllegalArgumentException> {
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
                priorCheckpoint = tampered,
            )
        }
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

    @Test
    fun currentCycleCheckpointCarriesOnlyBoundedRefsHashesAndMovementState() {
        val awaiting =
            VexLifeWorkingContinuity.formCycleCheckpoint(
                lineageRef = "companion-lineage.vexlife.cycle",
                threadRef = "thread.self-development.open-conversation",
                turnRef = "turn.vexlife.cycle.001",
                requestEventRef = "event.vexlife.cycle.request.001",
                requestEventSha256 = "1".repeat(64),
                cycleIndex = 1,
                phase =
                    VexLifeWorkingContinuity.CyclePhase.AWAITING_INTEGRATION,
                workingFocusRefOrNull =
                    "focus.vexlife.current",
                workingFocusStateRefOrNull =
                    "state.vexlife.focus.current",
                workingFocusRevisionRefOrNull =
                    "revision.vexlife.focus.current",
                capabilityCallRef =
                    "call.vexlife.capability.cycle.001",
                capabilityResultSha256 =
                    "2".repeat(64),
                capabilityState = "RETURNED",
                capabilitySourceRefs =
                    listOf(
                        "source.vexlife.cycle.b",
                        "source.vexlife.cycle.a",
                    ),
                capabilityFrontierRefs =
                    listOf(
                        "frontier.vexlife.cycle.001",
                    ),
                externalEffectsExecuted = false,
                externalEffectMayHaveExecuted = false,
                nextMoveOrNull = null,
                nextControlSha256OrNull = null,
                formedAt =
                    Instant.parse(
                        "2026-11-28T17:00:00Z",
                    ),
            )

        assertEquals(
            VexLifeWorkingContinuity.CyclePhase.AWAITING_INTEGRATION,
            awaiting.phase,
        )
        assertEquals(
            listOf(
                "source.vexlife.cycle.a",
                "source.vexlife.cycle.b",
            ),
            awaiting.capabilitySourceRefs,
        )
        assertFalse(
            awaiting.rawProviderContentPersisted,
        )
        assertFalse(
            awaiting.rawPrivateReasoningPersisted,
        )
        assertFalse(
            awaiting.semanticMemoryMutationPerformed,
        )
        assertNull(
            awaiting.nextMoveOrNull,
        )

        val integrated =
            VexLifeWorkingContinuity.formCycleCheckpoint(
                lineageRef = awaiting.lineageRef,
                threadRef = awaiting.threadRef,
                turnRef = awaiting.turnRef,
                requestEventRef = awaiting.requestEventRef,
                requestEventSha256 = awaiting.requestEventSha256,
                cycleIndex = awaiting.cycleIndex,
                phase =
                    VexLifeWorkingContinuity.CyclePhase.INTEGRATED,
                workingFocusRefOrNull =
                    awaiting.workingFocusRefOrNull,
                workingFocusStateRefOrNull =
                    awaiting.workingFocusStateRefOrNull,
                workingFocusRevisionRefOrNull =
                    awaiting.workingFocusRevisionRefOrNull,
                capabilityCallRef =
                    awaiting.capabilityCallRef,
                capabilityResultSha256 =
                    awaiting.capabilityResultSha256,
                capabilityState =
                    awaiting.capabilityState,
                capabilitySourceRefs =
                    awaiting.capabilitySourceRefs,
                capabilityFrontierRefs =
                    awaiting.capabilityFrontierRefs,
                externalEffectsExecuted =
                    awaiting.externalEffectsExecuted,
                externalEffectMayHaveExecuted =
                    awaiting.externalEffectMayHaveExecuted,
                nextMoveOrNull =
                    VexLifeWorkingContinuity.CycleNextMove.REACH,
                nextControlSha256OrNull =
                    "3".repeat(64),
                formedAt =
                    Instant.parse(
                        "2026-11-28T17:00:01Z",
                    ),
            )

        assertEquals(
            VexLifeWorkingContinuity.CycleNextMove.REACH,
            integrated.nextMoveOrNull,
        )
        assertEquals(
            "3".repeat(64),
            integrated.nextControlSha256OrNull,
        )
        assertFalse(
            integrated.cycleCheckpointRef ==
                awaiting.cycleCheckpointRef,
        )

        val clearedFocus =
            VexLifeWorkingContinuity.formCycleCheckpoint(
                lineageRef = awaiting.lineageRef,
                threadRef = awaiting.threadRef,
                turnRef = awaiting.turnRef,
                requestEventRef = awaiting.requestEventRef,
                requestEventSha256 = awaiting.requestEventSha256,
                cycleIndex = 2,
                phase =
                    VexLifeWorkingContinuity.CyclePhase.AWAITING_INTEGRATION,
                workingFocusRefOrNull = null,
                workingFocusStateRefOrNull =
                    "state.vexlife.focus.cleared",
                workingFocusRevisionRefOrNull = null,
                capabilityCallRef =
                    "call.vexlife.capability.cycle.002",
                capabilityResultSha256 =
                    "4".repeat(64),
                capabilityState = "RETURNED",
                capabilitySourceRefs =
                    emptyList(),
                capabilityFrontierRefs =
                    emptyList(),
                externalEffectsExecuted = false,
                externalEffectMayHaveExecuted = false,
                nextMoveOrNull = null,
                nextControlSha256OrNull = null,
                formedAt =
                    Instant.parse(
                        "2026-11-28T17:00:02Z",
                    ),
            )
        assertEquals(
            "state.vexlife.focus.cleared",
            clearedFocus.workingFocusStateRefOrNull,
        )
        assertNull(
            clearedFocus.workingFocusRefOrNull,
        )
        assertNull(
            clearedFocus.workingFocusRevisionRefOrNull,
        )
    }


    @Test
    fun c1CycleBytesRemainReadableWhenC2MovementIdentityIsAbsent() {
        val c1Shape =
            VexLifeWorkingContinuity.formCycleCheckpoint(
                lineageRef =
                    "companion-lineage.vexlife.c1-compat",
                threadRef =
                    "thread.self-development.open-conversation",
                turnRef =
                    "turn.vexlife.c1-compat.001",
                requestEventRef =
                    "event.vexlife.c1-compat.request.001",
                requestEventSha256 =
                    "6".repeat(64),
                cycleIndex = 1,
                phase =
                    VexLifeWorkingContinuity.CyclePhase.AWAITING_INTEGRATION,
                workingFocusRefOrNull = null,
                workingFocusStateRefOrNull = null,
                workingFocusRevisionRefOrNull = null,
                capabilityCallRef =
                    "call.vexlife.c1-compat.001",
                capabilityResultSha256 =
                    "7".repeat(64),
                capabilityState =
                    "RETURNED",
                capabilitySourceRefs =
                    emptyList(),
                capabilityFrontierRefs =
                    emptyList(),
                externalEffectsExecuted = false,
                externalEffectMayHaveExecuted = false,
                nextMoveOrNull = null,
                nextControlSha256OrNull = null,
                formedAt =
                    Instant.parse(
                        "2026-11-28T18:10:00Z",
                    ),
            )

        val bytes =
            VexContractJson.canonicalEncode(
                c1Shape,
            )

        assertFalse(
            bytes.contains(
                "requestMessageRefOrNull",
            ),
        )
        assertFalse(
            bytes.contains(
                "capabilityHandleOrNull",
            ),
        )
        assertFalse(
            bytes.contains(
                "capabilityActionOrNull",
            ),
        )
        assertFalse(
            bytes.contains(
                "capabilityTargetOrNull",
            ),
        )

        assertEquals(
            c1Shape,
            VexContractJson.decode<VexLifeWorkingContinuity.CycleCheckpoint>(
                bytes,
            ),
        )
    }

}

// [VXG RealForever]
