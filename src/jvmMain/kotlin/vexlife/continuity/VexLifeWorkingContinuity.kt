package vexlife.continuity

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlinx.serialization.Serializable
import vexlife.contract.VexContractJson

/**
 * Effect-free working-continuity pressure and checkpoint planning for the direct New Beginnings
 * Companion path.
 *
 * This owner answers two questions only:
 *
 * 1. How much bounded prompt headroom remains after one exact materialization?
 * 2. Does that current truth justify a reference-only working checkpoint or rollover-ready plan?
 *
 * It does not persist a checkpoint, issue a successor Context Lease, summarize Conversation,
 * promote semantic Memory, call the model, or mutate Home. Those later effects require their own
 * rightful owners.
 */
object VexLifeWorkingContinuity {
    const val PressureSchema =
        "vexlife-prod.resource-context-pressure/v1"

    const val CheckpointSchema =
        "vexlife-prod.working-continuity-checkpoint/v1"

    const val PlanSchema =
        "vexlife-prod.continuity-rollover-plan/v1"

    const val PolicyRef =
        "policy.vexlife-prod.resource-context-pressure.001"

    @Serializable
    data class Policy(
        val schemaVersion: String =
            "vexlife-prod.resource-context-pressure-policy/v1",
        val policyRef: String =
            PolicyRef,
        val watchEnterReserveMultiples: Int =
            3,
        val checkpointEnterReserveMultiples: Int =
            2,
        val rolloverEnterReserveMultiples: Int =
            1,
        val memoryBoundedOutFloor: PressureState =
            PressureState.CheckpointRecommended,
    ) {
        init {
            require(
                watchEnterReserveMultiples >
                    checkpointEnterReserveMultiples &&
                    checkpointEnterReserveMultiples >
                    rolloverEnterReserveMultiples &&
                    rolloverEnterReserveMultiples >
                    0,
            ) {
                "working continuity pressure thresholds must remain strictly ordered"
            }
        }
    }

    val CurrentPolicy =
        Policy()

    @Serializable
    enum class PressureState {
        Normal,
        Watch,
        CheckpointRecommended,
        RolloverRequired,
    }

    @Serializable
    enum class PlanState {
        NoCheckpointRequired,
        CheckpointReady,
        RolloverReady,
    }

    @Serializable
    data class PromptPressureInput(
        val promptReceiptRef: String,
        val providerMaterializedInputTokenEstimate: Int,
        val inputTokenBudget: Int,
        val reservedOutputTokens: Int,
        val hardTokenLimit: Int,
        val memoryBoundedOutStatementCount: Int,
        val memoryTokenBoundedOutPageCount: Int,
        val sourceRefs: List<String>,
    ) {
        init {
            requireRef(
                promptReceiptRef,
                "promptReceiptRef",
            )
            require(
                providerMaterializedInputTokenEstimate in
                    0..inputTokenBudget &&
                    inputTokenBudget >
                    0 &&
                    reservedOutputTokens >
                    0 &&
                    inputTokenBudget +
                    reservedOutputTokens <=
                    hardTokenLimit,
            ) {
                "working continuity pressure input token bounds are invalid"
            }
            require(
                memoryBoundedOutStatementCount >=
                    0 &&
                    memoryTokenBoundedOutPageCount >=
                    0,
            ) {
                "working continuity Memory bounded-out counts must be non-negative"
            }
            requireRefs(
                sourceRefs,
                "pressure sourceRefs",
            )
        }
    }

    @Serializable
    data class Pressure(
        val schemaVersion: String,
        val truthClass: String,
        val policyRef: String,
        val policyFingerprint: String,
        val state: PressureState,
        val reasonCodes: List<String>,
        val promptReceiptRef: String,
        val providerMaterializedInputTokenEstimate: Int,
        val inputTokenBudget: Int,
        val reservedOutputTokens: Int,
        val hardTokenLimit: Int,
        val inputHeadroomTokens: Int,
        val outputReserveMultiplesOfHeadroom: Int,
        val memoryBoundedOutStatementCount: Int,
        val memoryTokenBoundedOutPageCount: Int,
        val sourceRefs: List<String>,
        val checkpointEffectPerformed: Boolean,
        val rolloverEffectPerformed: Boolean,
        val pressureRef: String,
        val semanticFingerprint: String,
    )

    @Serializable
    data class WorkingSet(
        val lineageRef: String,
        val threadRef: String,
        val cursorEventRef: String,
        val activeWorkNodeRefs: List<String>,
        val currentIntentReceiptRefs: List<String>,
        val openLoopRefs: List<String>,
        val currentStatementRefs: List<String>,
        val currentContinuityRecordRefs: List<String>,
        val currentDailyStratumRefOrNull: String?,
        val sourceRefs: List<String>,
    ) {
        init {
            requireRef(
                lineageRef,
                "lineageRef",
            )
            requireRef(
                threadRef,
                "threadRef",
            )
            requireRef(
                cursorEventRef,
                "cursorEventRef",
            )
            requireRefs(
                activeWorkNodeRefs,
                "activeWorkNodeRefs",
            )
            requireRefs(
                currentIntentReceiptRefs,
                "currentIntentReceiptRefs",
            )
            requireRefs(
                openLoopRefs,
                "openLoopRefs",
            )
            requireRefs(
                currentStatementRefs,
                "currentStatementRefs",
            )
            requireRefs(
                currentContinuityRecordRefs,
                "currentContinuityRecordRefs",
            )
            currentDailyStratumRefOrNull
                ?.let {
                    requireRef(
                        it,
                        "currentDailyStratumRefOrNull",
                    )
                }
            requireRefs(
                sourceRefs,
                "workingSet sourceRefs",
            )
        }
    }

    @Serializable
    data class CheckpointTruth(
        val referenceOnly: Boolean =
            true,
        val rawTranscriptIncluded: Boolean =
            false,
        val rawPrivatePayloadIncluded: Boolean =
            false,
        val modelSummaryIncluded: Boolean =
            false,
        val semanticAcceptanceCreated: Boolean =
            false,
        val memoryPromotionPerformed: Boolean =
            false,
        val sourceHistoryDeleted: Boolean =
            false,
        val persistencePerformed: Boolean =
            false,
        val successorContextIssued: Boolean =
            false,
    )

    @Serializable
    data class Checkpoint(
        val schemaVersion: String,
        val truthClass: String,
        val currentness: String,
        val lifecycle: String,
        val lineageRef: String,
        val threadRef: String,
        val pressureRef: String,
        val pressureFingerprint: String,
        val pressureState: PressureState,
        val promptReceiptRef: String,
        val workingSet: WorkingSet,
        val priorCheckpointRefOrNull: String?,
        val priorCheckpointFingerprintOrNull: String?,
        val sourceRefs: List<String>,
        val effectAuthorityGranted: Boolean,
        val semanticMemoryPromotionAuthorized: Boolean,
        val modelOutputAcceptedAsMemory: Boolean,
        val checkpointTruth: CheckpointTruth,
        val checkpointRef: String,
        val semanticFingerprint: String,
    )

    @Serializable
    data class PlanEffects(
        val checkpointPersisted: Boolean =
            false,
        val priorCheckpointSuperseded: Boolean =
            false,
        val successorContextIssued: Boolean =
            false,
        val memoryMutationPerformed: Boolean =
            false,
        val modelCalled: Boolean =
            false,
        val trainingRan: Boolean =
            false,
        val modelWeightsChanged: Boolean =
            false,
    )

    @Serializable
    data class Plan(
        val schemaVersion: String,
        val truthClass: String,
        val currentness: String,
        val state: PlanState,
        val reasonCodes: List<String>,
        val pressureRef: String,
        val pressureFingerprint: String,
        val lineageRef: String,
        val threadRef: String,
        val checkpoint: Checkpoint?,
        val nextSafeAction: String,
        val effectAuthorityGranted: Boolean,
        val effects: PlanEffects,
        val planRef: String,
        val semanticFingerprint: String,
    )

    fun projectPressure(
        input: PromptPressureInput,
    ): Pressure {
        val headroom =
            input.inputTokenBudget -
                input.providerMaterializedInputTokenEstimate
        val reserveMultiples =
            headroom /
                input.reservedOutputTokens
        var state =
            when {
                headroom <=
                    input.reservedOutputTokens *
                    CurrentPolicy.rolloverEnterReserveMultiples ->
                    PressureState.RolloverRequired

                headroom <=
                    input.reservedOutputTokens *
                    CurrentPolicy.checkpointEnterReserveMultiples ->
                    PressureState.CheckpointRecommended

                headroom <=
                    input.reservedOutputTokens *
                    CurrentPolicy.watchEnterReserveMultiples ->
                    PressureState.Watch

                else ->
                    PressureState.Normal
            }

        val reasons =
            mutableSetOf<String>()

        when (
            state
        ) {
            PressureState.Normal ->
                reasons +=
                    "PRESSURE_WITHIN_NORMAL_BOUNDS"

            PressureState.Watch ->
                reasons +=
                    "CONTEXT_HEADROOM_AT_OR_BELOW_THREE_OUTPUT_RESERVES"

            PressureState.CheckpointRecommended ->
                reasons +=
                    "CONTEXT_HEADROOM_AT_OR_BELOW_TWO_OUTPUT_RESERVES"

            PressureState.RolloverRequired ->
                reasons +=
                    "CONTEXT_HEADROOM_AT_OR_BELOW_ONE_OUTPUT_RESERVE"
        }

        if (
            input.memoryBoundedOutStatementCount >
                0 ||
            input.memoryTokenBoundedOutPageCount >
                0
        ) {
            reasons +=
                "MEMORY_BOUNDED_OUT"
            if (
                state.ordinal <
                CurrentPolicy.memoryBoundedOutFloor.ordinal
            ) {
                state =
                    CurrentPolicy.memoryBoundedOutFloor
            }
        }

        val core =
            PressureCore(
                schemaVersion = PressureSchema,
                truthClass =
                    "SOURCE_BOUND_RESOURCE_CONTEXT_PRESSURE",
                policyRef = PolicyRef,
                policyFingerprint =
                    semanticHash(
                        CurrentPolicy,
                    ),
                state = state,
                reasonCodes =
                    reasons
                        .toList()
                        .sorted(),
                promptReceiptRef = input.promptReceiptRef,
                providerMaterializedInputTokenEstimate =
                    input.providerMaterializedInputTokenEstimate,
                inputTokenBudget = input.inputTokenBudget,
                reservedOutputTokens = input.reservedOutputTokens,
                hardTokenLimit = input.hardTokenLimit,
                inputHeadroomTokens = headroom,
                outputReserveMultiplesOfHeadroom = reserveMultiples,
                memoryBoundedOutStatementCount =
                    input.memoryBoundedOutStatementCount,
                memoryTokenBoundedOutPageCount =
                    input.memoryTokenBoundedOutPageCount,
                sourceRefs =
                    input.sourceRefs
                        .distinct()
                        .sorted(),
                checkpointEffectPerformed = false,
                rolloverEffectPerformed = false,
            )
        val fingerprint =
            semanticHash(
                core,
            )

        return Pressure(
            schemaVersion = core.schemaVersion,
            truthClass = core.truthClass,
            policyRef = core.policyRef,
            policyFingerprint = core.policyFingerprint,
            state = core.state,
            reasonCodes = core.reasonCodes,
            promptReceiptRef = core.promptReceiptRef,
            providerMaterializedInputTokenEstimate =
                core.providerMaterializedInputTokenEstimate,
            inputTokenBudget = core.inputTokenBudget,
            reservedOutputTokens = core.reservedOutputTokens,
            hardTokenLimit = core.hardTokenLimit,
            inputHeadroomTokens = core.inputHeadroomTokens,
            outputReserveMultiplesOfHeadroom =
                core.outputReserveMultiplesOfHeadroom,
            memoryBoundedOutStatementCount =
                core.memoryBoundedOutStatementCount,
            memoryTokenBoundedOutPageCount =
                core.memoryTokenBoundedOutPageCount,
            sourceRefs = core.sourceRefs,
            checkpointEffectPerformed =
                core.checkpointEffectPerformed,
            rolloverEffectPerformed =
                core.rolloverEffectPerformed,
            pressureRef =
                "projection.vexlife-prod.resource-context-pressure." +
                    fingerprint.take(
                        32,
                    ),
            semanticFingerprint = fingerprint,
        )
    }

    fun formPlan(
        pressureInput: PromptPressureInput,
        workingSet: WorkingSet,
        priorCheckpoint: Checkpoint? =
            null,
    ): Plan {
        val pressure =
            projectPressure(
                pressureInput,
            )
        priorCheckpoint
            ?.let {
                require(
                    it.schemaVersion ==
                        CheckpointSchema &&
                        it.currentness ==
                        "CURRENT" &&
                        it.lifecycle ==
                        "CURRENT" &&
                        it.lineageRef ==
                        workingSet.lineageRef &&
                        it.threadRef ==
                        workingSet.threadRef,
                ) {
                    "prior working checkpoint is not current for this lineage/thread"
                }
            }

        val checkpoint =
            when (
                pressure.state
            ) {
                PressureState.Normal,
                PressureState.Watch,
                ->
                    null

                PressureState.CheckpointRecommended,
                PressureState.RolloverRequired,
                ->
                    formCheckpoint(
                        pressure = pressure,
                        workingSet = workingSet,
                        priorCheckpoint = priorCheckpoint,
                    )
            }

        val state =
            when (
                pressure.state
            ) {
                PressureState.Normal,
                PressureState.Watch,
                ->
                    PlanState.NoCheckpointRequired

                PressureState.CheckpointRecommended ->
                    PlanState.CheckpointReady

                PressureState.RolloverRequired ->
                    PlanState.RolloverReady
            }

        val nextSafeAction =
            when (
                state
            ) {
                PlanState.NoCheckpointRequired ->
                    "CONTINUE_WITH_CURRENT_CONTEXT_MODE"

                PlanState.CheckpointReady ->
                    "DELEGATE_REFERENCE_ONLY_CHECKPOINT_TO_WORKING_CONTINUITY_OWNER"

                PlanState.RolloverReady ->
                    "DELEGATE_CHECKPOINT_THEN_FORM_EXACT_SUCCESSOR_CONTEXT"
            }

        val core =
            PlanCore(
                schemaVersion = PlanSchema,
                truthClass =
                    "SOURCE_BOUND_EFFECT_FREE_CONTINUITY_ROLLOVER_PLAN",
                currentness = "CURRENT",
                state = state,
                reasonCodes = pressure.reasonCodes,
                pressureRef = pressure.pressureRef,
                pressureFingerprint = pressure.semanticFingerprint,
                lineageRef = workingSet.lineageRef,
                threadRef = workingSet.threadRef,
                checkpoint = checkpoint,
                nextSafeAction = nextSafeAction,
                effectAuthorityGranted = false,
                effects = PlanEffects(),
            )
        val fingerprint =
            semanticHash(
                core,
            )

        return Plan(
            schemaVersion = core.schemaVersion,
            truthClass = core.truthClass,
            currentness = core.currentness,
            state = core.state,
            reasonCodes = core.reasonCodes,
            pressureRef = core.pressureRef,
            pressureFingerprint = core.pressureFingerprint,
            lineageRef = core.lineageRef,
            threadRef = core.threadRef,
            checkpoint = core.checkpoint,
            nextSafeAction = core.nextSafeAction,
            effectAuthorityGranted = core.effectAuthorityGranted,
            effects = core.effects,
            planRef =
                "plan.vexlife-prod.continuity-rollover." +
                    fingerprint.take(
                        32,
                    ),
            semanticFingerprint = fingerprint,
        )
    }

    @Serializable
    private data class PressureCore(
        val schemaVersion: String,
        val truthClass: String,
        val policyRef: String,
        val policyFingerprint: String,
        val state: PressureState,
        val reasonCodes: List<String>,
        val promptReceiptRef: String,
        val providerMaterializedInputTokenEstimate: Int,
        val inputTokenBudget: Int,
        val reservedOutputTokens: Int,
        val hardTokenLimit: Int,
        val inputHeadroomTokens: Int,
        val outputReserveMultiplesOfHeadroom: Int,
        val memoryBoundedOutStatementCount: Int,
        val memoryTokenBoundedOutPageCount: Int,
        val sourceRefs: List<String>,
        val checkpointEffectPerformed: Boolean,
        val rolloverEffectPerformed: Boolean,
    )

    @Serializable
    private data class CheckpointCore(
        val schemaVersion: String,
        val truthClass: String,
        val currentness: String,
        val lifecycle: String,
        val lineageRef: String,
        val threadRef: String,
        val pressureRef: String,
        val pressureFingerprint: String,
        val pressureState: PressureState,
        val promptReceiptRef: String,
        val workingSet: WorkingSet,
        val priorCheckpointRefOrNull: String?,
        val priorCheckpointFingerprintOrNull: String?,
        val sourceRefs: List<String>,
        val effectAuthorityGranted: Boolean,
        val semanticMemoryPromotionAuthorized: Boolean,
        val modelOutputAcceptedAsMemory: Boolean,
        val checkpointTruth: CheckpointTruth,
    )

    @Serializable
    private data class PlanCore(
        val schemaVersion: String,
        val truthClass: String,
        val currentness: String,
        val state: PlanState,
        val reasonCodes: List<String>,
        val pressureRef: String,
        val pressureFingerprint: String,
        val lineageRef: String,
        val threadRef: String,
        val checkpoint: Checkpoint?,
        val nextSafeAction: String,
        val effectAuthorityGranted: Boolean,
        val effects: PlanEffects,
    )

    private fun formCheckpoint(
        pressure: Pressure,
        workingSet: WorkingSet,
        priorCheckpoint: Checkpoint?,
    ): Checkpoint {
        val sourceRefs =
            (
                pressure.sourceRefs +
                    workingSet.sourceRefs +
                    listOf(
                        pressure.pressureRef,
                        pressure.promptReceiptRef,
                    )
            )
                .distinct()
                .sorted()

        val core =
            CheckpointCore(
                schemaVersion = CheckpointSchema,
                truthClass =
                    "SOURCE_BOUND_NON_SEMANTIC_WORKING_CONTINUITY_CHECKPOINT",
                currentness = "CURRENT",
                lifecycle = "CURRENT",
                lineageRef = workingSet.lineageRef,
                threadRef = workingSet.threadRef,
                pressureRef = pressure.pressureRef,
                pressureFingerprint = pressure.semanticFingerprint,
                pressureState = pressure.state,
                promptReceiptRef = pressure.promptReceiptRef,
                workingSet =
                    canonicalWorkingSet(
                        workingSet,
                    ),
                priorCheckpointRefOrNull =
                    priorCheckpoint
                        ?.checkpointRef,
                priorCheckpointFingerprintOrNull =
                    priorCheckpoint
                        ?.semanticFingerprint,
                sourceRefs = sourceRefs,
                effectAuthorityGranted = false,
                semanticMemoryPromotionAuthorized = false,
                modelOutputAcceptedAsMemory = false,
                checkpointTruth = CheckpointTruth(),
            )
        val fingerprint =
            semanticHash(
                core,
            )

        return Checkpoint(
            schemaVersion = core.schemaVersion,
            truthClass = core.truthClass,
            currentness = core.currentness,
            lifecycle = core.lifecycle,
            lineageRef = core.lineageRef,
            threadRef = core.threadRef,
            pressureRef = core.pressureRef,
            pressureFingerprint = core.pressureFingerprint,
            pressureState = core.pressureState,
            promptReceiptRef = core.promptReceiptRef,
            workingSet = core.workingSet,
            priorCheckpointRefOrNull =
                core.priorCheckpointRefOrNull,
            priorCheckpointFingerprintOrNull =
                core.priorCheckpointFingerprintOrNull,
            sourceRefs = core.sourceRefs,
            effectAuthorityGranted = core.effectAuthorityGranted,
            semanticMemoryPromotionAuthorized =
                core.semanticMemoryPromotionAuthorized,
            modelOutputAcceptedAsMemory =
                core.modelOutputAcceptedAsMemory,
            checkpointTruth = core.checkpointTruth,
            checkpointRef =
                "checkpoint.vexlife-prod.working-continuity." +
                    fingerprint.take(
                        32,
                    ),
            semanticFingerprint = fingerprint,
        )
    }

    private fun canonicalWorkingSet(
        workingSet: WorkingSet,
    ): WorkingSet =
        workingSet.copy(
            activeWorkNodeRefs =
                workingSet.activeWorkNodeRefs
                    .distinct()
                    .sorted(),
            currentIntentReceiptRefs =
                workingSet.currentIntentReceiptRefs
                    .distinct()
                    .sorted(),
            openLoopRefs =
                workingSet.openLoopRefs
                    .distinct()
                    .sorted(),
            currentStatementRefs =
                workingSet.currentStatementRefs
                    .distinct()
                    .sorted(),
            currentContinuityRecordRefs =
                workingSet.currentContinuityRecordRefs
                    .distinct()
                    .sorted(),
            sourceRefs =
                workingSet.sourceRefs
                    .distinct()
                    .sorted(),
        )

    private fun requireRefs(
        values: List<String>,
        label: String,
    ) {
        values.forEach {
            requireRef(
                it,
                label,
            )
        }
        require(
            values.distinct().size ==
                values.size,
        ) {
            "$label must be unique"
        }
    }

    private fun requireRef(
        value: String,
        label: String,
    ) {
        require(
            value.matches(
                Regex(
                    "[A-Za-z0-9][A-Za-z0-9._:/-]*",
                ),
            ),
        ) {
            "$label must be one stable ref"
        }
    }

    private inline fun <reified T> semanticHash(
        value: T,
    ): String {
        val bytes =
            VexContractJson
                .canonicalEncode(
                    value,
                )
                .toByteArray(
                    StandardCharsets.UTF_8,
                )

        return MessageDigest
            .getInstance(
                "SHA-256",
            )
            .digest(
                bytes,
            )
            .joinToString(
                "",
            ) {
                "%02x".format(
                    it,
                )
            }
    }
}

// [VXG RealForever]
