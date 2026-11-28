package vexlife.continuity

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
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
    /** Plan reason: the turn continues an active rollover lineage, so its cursor must advance. */
    const val RolloverLineageActiveReason = "WORKING_ROLLOVER_LINEAGE_ACTIVE"

    const val PressureSchema =
        "vexlife-prod.resource-context-pressure/v1"

    const val CheckpointSchema =
        "vexlife-prod.working-continuity-checkpoint/v1"

    const val PlanSchema =
        "vexlife-prod.continuity-rollover-plan/v1"

    const val CycleCheckpointSchemaV1 =
        "vexlife-prod.working-continuity-cycle-checkpoint/v1"

    const val CycleCheckpointSchema =
        "vexlife-prod.working-continuity-cycle-checkpoint/v2"

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

    @Serializable
    enum class CyclePhase {
        AWAITING_INTEGRATION,
        INTEGRATED,
    }

    @Serializable
    enum class CycleNextMove {
        REACH,
        SOURCE_RETURN,
        FINAL_ANSWER,
    }

    /**
     * One bounded CURRENT in-flight work cycle.
     *
     * This is not a cycle history. The durable owner keeps only the latest current cycle so a
     * human interruption or recoverable turn failure can preserve the unfinished edge without
     * persisting raw reasoning/provider content or duplicating Conversation/capability evidence.
     */
    @OptIn(ExperimentalSerializationApi::class)
    @Serializable
    data class CycleCheckpoint(
        val schemaVersion: String,
        val truthClass: String,
        val lineageRef: String,
        val threadRef: String,
        val turnRef: String,
        val requestEventRef: String,
        val requestEventSha256: String,
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        val requestMessageRefOrNull: String? = null,
        val cycleIndex: Int,
        val phase: CyclePhase,
        val workingFocusRefOrNull: String?,
        val workingFocusStateRefOrNull: String?,
        val workingFocusRevisionRefOrNull: String?,
        val capabilityCallRef: String,
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        val capabilityHandleOrNull: String? = null,
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        val capabilityActionOrNull: String? = null,
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        val capabilityTargetOrNull: String? = null,
        val capabilityResultSha256: String,
        val capabilityState: String,
        val capabilitySourceRefs: List<String>,
        val capabilityFrontierRefs: List<String>,
        val externalEffectsExecuted: Boolean,
        val externalEffectMayHaveExecuted: Boolean,
        val nextMoveOrNull: CycleNextMove?,
        val nextControlSha256OrNull: String?,
        val formedAt: String,
        val rawProviderContentPersisted: Boolean,
        val rawPrivateReasoningPersisted: Boolean,
        val semanticMemoryMutationPerformed: Boolean,
        val cycleCheckpointRef: String,
        val semanticFingerprint: String,
    )

    fun formCycleCheckpoint(
        lineageRef: String,
        threadRef: String,
        turnRef: String,
        requestEventRef: String,
        requestEventSha256: String,
        requestMessageRefOrNull: String? =
            null,
        cycleIndex: Int,
        phase: CyclePhase,
        workingFocusRefOrNull: String?,
        workingFocusStateRefOrNull: String?,
        workingFocusRevisionRefOrNull: String?,
        capabilityCallRef: String,
        capabilityHandleOrNull: String? =
            null,
        capabilityActionOrNull: String? =
            null,
        capabilityTargetOrNull: String? =
            null,
        capabilityResultSha256: String,
        capabilityState: String,
        capabilitySourceRefs: List<String>,
        capabilityFrontierRefs: List<String>,
        externalEffectsExecuted: Boolean,
        externalEffectMayHaveExecuted: Boolean,
        nextMoveOrNull: CycleNextMove?,
        nextControlSha256OrNull: String?,
        formedAt: Instant,
    ): CycleCheckpoint {
        val core =
            CycleCheckpointCore(
                schemaVersion =
                    if (
                        requestMessageRefOrNull !=
                            null ||
                        capabilityHandleOrNull !=
                            null ||
                        capabilityActionOrNull !=
                            null ||
                        capabilityTargetOrNull !=
                            null
                    ) {
                        CycleCheckpointSchema
                    } else {
                        CycleCheckpointSchemaV1
                    },
                truthClass = "CURRENT_REFERENCE_ONLY_WORKING_CONTINUITY_CYCLE",
                lineageRef = lineageRef,
                threadRef = threadRef,
                turnRef = turnRef,
                requestEventRef = requestEventRef,
                requestEventSha256 = requestEventSha256,
                requestMessageRefOrNull = requestMessageRefOrNull,
                cycleIndex = cycleIndex,
                phase = phase,
                workingFocusRefOrNull = workingFocusRefOrNull,
                workingFocusStateRefOrNull = workingFocusStateRefOrNull,
                workingFocusRevisionRefOrNull = workingFocusRevisionRefOrNull,
                capabilityCallRef = capabilityCallRef,
                capabilityHandleOrNull = capabilityHandleOrNull,
                capabilityActionOrNull = capabilityActionOrNull,
                capabilityTargetOrNull = capabilityTargetOrNull,
                capabilityResultSha256 = capabilityResultSha256,
                capabilityState = capabilityState,
                capabilitySourceRefs = capabilitySourceRefs.distinct().sorted(),
                capabilityFrontierRefs = capabilityFrontierRefs.distinct().sorted(),
                externalEffectsExecuted = externalEffectsExecuted,
                externalEffectMayHaveExecuted = externalEffectMayHaveExecuted,
                nextMoveOrNull = nextMoveOrNull,
                nextControlSha256OrNull = nextControlSha256OrNull,
                formedAt = formedAt.toString(),
                rawProviderContentPersisted = false,
                rawPrivateReasoningPersisted = false,
                semanticMemoryMutationPerformed = false,
            )
        val fingerprint = semanticHash(core)
        return CycleCheckpoint(
            schemaVersion = core.schemaVersion,
            truthClass = core.truthClass,
            lineageRef = core.lineageRef,
            threadRef = core.threadRef,
            turnRef = core.turnRef,
            requestEventRef = core.requestEventRef,
            requestEventSha256 = core.requestEventSha256,
            requestMessageRefOrNull = core.requestMessageRefOrNull,
            cycleIndex = core.cycleIndex,
            phase = core.phase,
            workingFocusRefOrNull = core.workingFocusRefOrNull,
            workingFocusStateRefOrNull = core.workingFocusStateRefOrNull,
            workingFocusRevisionRefOrNull = core.workingFocusRevisionRefOrNull,
            capabilityCallRef = core.capabilityCallRef,
            capabilityHandleOrNull = core.capabilityHandleOrNull,
            capabilityActionOrNull = core.capabilityActionOrNull,
            capabilityTargetOrNull = core.capabilityTargetOrNull,
            capabilityResultSha256 = core.capabilityResultSha256,
            capabilityState = core.capabilityState,
            capabilitySourceRefs = core.capabilitySourceRefs,
            capabilityFrontierRefs = core.capabilityFrontierRefs,
            externalEffectsExecuted = core.externalEffectsExecuted,
            externalEffectMayHaveExecuted = core.externalEffectMayHaveExecuted,
            nextMoveOrNull = core.nextMoveOrNull,
            nextControlSha256OrNull = core.nextControlSha256OrNull,
            formedAt = core.formedAt,
            rawProviderContentPersisted = core.rawProviderContentPersisted,
            rawPrivateReasoningPersisted = core.rawPrivateReasoningPersisted,
            semanticMemoryMutationPerformed = core.semanticMemoryMutationPerformed,
            cycleCheckpointRef =
                "checkpoint.vexlife-prod.working-continuity-cycle." +
                    fingerprint.take(32),
            semanticFingerprint = fingerprint,
        ).also(::validateCycleCheckpoint)
    }

    fun validateCycleCheckpoint(
        value: CycleCheckpoint,
    ): CycleCheckpoint {
        require(
            value.schemaVersion in
                setOf(
                    CycleCheckpointSchemaV1,
                    CycleCheckpointSchema,
                ) &&
                value.truthClass == "CURRENT_REFERENCE_ONLY_WORKING_CONTINUITY_CYCLE" &&
                value.cycleIndex > 0 &&
                value.capabilityState.isNotBlank() &&
                !value.rawProviderContentPersisted &&
                !value.rawPrivateReasoningPersisted &&
                !value.semanticMemoryMutationPerformed &&
                !(value.externalEffectsExecuted && value.externalEffectMayHaveExecuted)
        ) {
            "working continuity cycle checkpoint identity/effect truth is invalid"
        }
        requireRef(value.lineageRef, "cycle lineageRef")
        requireRef(value.threadRef, "cycle threadRef")
        requireRef(value.turnRef, "cycle turnRef")
        requireRef(value.requestEventRef, "cycle requestEventRef")
        requireSha256Value(value.requestEventSha256, "cycle requestEventSha256")
        value.requestMessageRefOrNull?.let {
            requireRef(
                it,
                "cycle requestMessageRef",
            )
        }
        requireRef(value.capabilityCallRef, "cycle capabilityCallRef")
        when (
            value.schemaVersion
        ) {
            CycleCheckpointSchemaV1 ->
                require(
                    value.requestMessageRefOrNull ==
                        null &&
                        value.capabilityHandleOrNull ==
                        null &&
                        value.capabilityActionOrNull ==
                        null &&
                        value.capabilityTargetOrNull ==
                        null
                ) {
                    "historical working continuity cycle v1 cannot contain C2 recovery identity"
                }

            CycleCheckpointSchema ->
                require(
                    value.requestMessageRefOrNull !=
                        null &&
                        value.capabilityHandleOrNull !=
                        null &&
                        value.capabilityActionOrNull !=
                        null
                ) {
                    "working continuity cycle v2 requires fresh-Send recovery identity"
                }
        }
        require(
            (value.capabilityHandleOrNull == null) ==
                (value.capabilityActionOrNull == null) &&
                (
                    value.capabilityTargetOrNull == null ||
                        value.capabilityHandleOrNull != null
                    )
        ) {
            "working continuity cycle capability meaning fields are inconsistent"
        }
        value.capabilityHandleOrNull?.let {
            requireRef(
                it,
                "cycle capabilityHandle",
            )
        }
        value.capabilityActionOrNull?.let {
            requireRef(
                it,
                "cycle capabilityAction",
            )
        }
        requireSha256Value(value.capabilityResultSha256, "cycle capabilityResultSha256")
        value.workingFocusRefOrNull?.let { requireRef(it, "cycle workingFocusRef") }
        value.workingFocusStateRefOrNull?.let { requireRef(it, "cycle workingFocusStateRef") }
        value.workingFocusRevisionRefOrNull?.let { requireRef(it, "cycle workingFocusRevisionRef") }
        require(
            (value.workingFocusRefOrNull == null) ==
                (value.workingFocusRevisionRefOrNull == null) &&
                (
                    value.workingFocusStateRefOrNull !=
                        null ||
                        (
                            value.workingFocusRefOrNull ==
                                null &&
                                value.workingFocusRevisionRefOrNull ==
                                null
                            )
                )
        ) {
            "working continuity cycle focus refs are inconsistent"
        }
        requireRefs(value.capabilitySourceRefs, "cycle capabilitySourceRefs")
        requireRefs(value.capabilityFrontierRefs, "cycle capabilityFrontierRefs")
        require(
            value.capabilitySourceRefs == value.capabilitySourceRefs.distinct().sorted() &&
                value.capabilityFrontierRefs == value.capabilityFrontierRefs.distinct().sorted()
        ) {
            "working continuity cycle capability refs must be canonical"
        }
        when (value.phase) {
            CyclePhase.AWAITING_INTEGRATION ->
                require(
                    value.nextMoveOrNull == null &&
                        value.nextControlSha256OrNull == null
                ) {
                    "awaiting-integration cycle cannot claim a next movement"
                }

            CyclePhase.INTEGRATED -> {
                require(value.nextMoveOrNull != null) {
                    "integrated cycle must name the next movement"
                }
                when (value.nextMoveOrNull) {
                    CycleNextMove.REACH,
                    CycleNextMove.SOURCE_RETURN,
                    ->
                        require(value.nextControlSha256OrNull != null) {
                            "control-producing integrated cycle requires exact next control SHA-256"
                        }

                    CycleNextMove.FINAL_ANSWER ->
                        require(value.nextControlSha256OrNull == null) {
                            "final-answer cycle does not persist answer bytes/hash as working continuity"
                        }

                    null -> error("integrated cycle next move disappeared")
                }
            }
        }
        value.nextControlSha256OrNull?.let {
            requireSha256Value(it, "cycle nextControlSha256")
        }
        Instant.parse(value.formedAt)
        requireRef(value.cycleCheckpointRef, "cycle checkpointRef")

        val core =
            CycleCheckpointCore(
                schemaVersion = value.schemaVersion,
                truthClass = value.truthClass,
                lineageRef = value.lineageRef,
                threadRef = value.threadRef,
                turnRef = value.turnRef,
                requestEventRef = value.requestEventRef,
                requestEventSha256 = value.requestEventSha256,
                requestMessageRefOrNull = value.requestMessageRefOrNull,
                cycleIndex = value.cycleIndex,
                phase = value.phase,
                workingFocusRefOrNull = value.workingFocusRefOrNull,
                workingFocusStateRefOrNull = value.workingFocusStateRefOrNull,
                workingFocusRevisionRefOrNull = value.workingFocusRevisionRefOrNull,
                capabilityCallRef = value.capabilityCallRef,
                capabilityHandleOrNull = value.capabilityHandleOrNull,
                capabilityActionOrNull = value.capabilityActionOrNull,
                capabilityTargetOrNull = value.capabilityTargetOrNull,
                capabilityResultSha256 = value.capabilityResultSha256,
                capabilityState = value.capabilityState,
                capabilitySourceRefs = value.capabilitySourceRefs,
                capabilityFrontierRefs = value.capabilityFrontierRefs,
                externalEffectsExecuted = value.externalEffectsExecuted,
                externalEffectMayHaveExecuted = value.externalEffectMayHaveExecuted,
                nextMoveOrNull = value.nextMoveOrNull,
                nextControlSha256OrNull = value.nextControlSha256OrNull,
                formedAt = value.formedAt,
                rawProviderContentPersisted = value.rawProviderContentPersisted,
                rawPrivateReasoningPersisted = value.rawPrivateReasoningPersisted,
                semanticMemoryMutationPerformed = value.semanticMemoryMutationPerformed,
            )
        val fingerprint = semanticHash(core)
        require(
            value.semanticFingerprint == fingerprint &&
                value.cycleCheckpointRef ==
                "checkpoint.vexlife-prod.working-continuity-cycle." +
                    fingerprint.take(32)
        ) {
            "working continuity cycle checkpoint semantic identity is invalid"
        }
        return value
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Serializable
    private data class CycleCheckpointCore(
        val schemaVersion: String,
        val truthClass: String,
        val lineageRef: String,
        val threadRef: String,
        val turnRef: String,
        val requestEventRef: String,
        val requestEventSha256: String,
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        val requestMessageRefOrNull: String? = null,
        val cycleIndex: Int,
        val phase: CyclePhase,
        val workingFocusRefOrNull: String?,
        val workingFocusStateRefOrNull: String?,
        val workingFocusRevisionRefOrNull: String?,
        val capabilityCallRef: String,
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        val capabilityHandleOrNull: String? = null,
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        val capabilityActionOrNull: String? = null,
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        val capabilityTargetOrNull: String? = null,
        val capabilityResultSha256: String,
        val capabilityState: String,
        val capabilitySourceRefs: List<String>,
        val capabilityFrontierRefs: List<String>,
        val externalEffectsExecuted: Boolean,
        val externalEffectMayHaveExecuted: Boolean,
        val nextMoveOrNull: CycleNextMove?,
        val nextControlSha256OrNull: String?,
        val formedAt: String,
        val rawProviderContentPersisted: Boolean,
        val rawPrivateReasoningPersisted: Boolean,
        val semanticMemoryMutationPerformed: Boolean,
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
            PressureState.Normal -> {
                // Added only after Memory pressure has had a chance to raise the final state.
            }

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
            if (
                state.ordinal <
                CurrentPolicy.memoryBoundedOutFloor.ordinal
            ) {
                state =
                    CurrentPolicy.memoryBoundedOutFloor
            }
            reasons +=
                "MEMORY_CONTINUITY_BOUNDED_OUT"
        }

        if (
            state ==
                PressureState.Normal &&
            reasons.isEmpty()
        ) {
            reasons +=
                "PRESSURE_WITHIN_NORMAL_BOUNDS"
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
        rolloverLineageActive: Boolean =
            false,
    ): Plan {
        val pressure =
            projectPressure(
                pressureInput,
            )
        priorCheckpoint
            ?.let {
                checkpoint ->
                validateCheckpoint(
                    checkpoint,
                )
                require(
                    checkpoint.lineageRef ==
                        workingSet.lineageRef &&
                        checkpoint.threadRef ==
                        workingSet.threadRef,
                ) {
                    "prior working checkpoint belongs to another lineage/thread"
                }
            }

        val checkpoint =
            if (
                rolloverLineageActive
            ) {
                formCheckpoint(
                    pressure = pressure,
                    workingSet = workingSet,
                    priorCheckpoint = priorCheckpoint,
                )
            } else {
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
            }

        val state =
            if (
                rolloverLineageActive
            ) {
                PlanState.RolloverReady
            } else {
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
            }

        val reasonCodes =
            (
                pressure.reasonCodes +
                    if (rolloverLineageActive) {
                        listOf(
                            RolloverLineageActiveReason,
                        )
                    } else {
                        emptyList()
                    }
            )
                .distinct()
                .sorted()

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
                reasonCodes = reasonCodes,
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

    fun validateWorkingSet(
        workingSet: WorkingSet,
    ): WorkingSet {
        require(
            workingSet ==
                canonicalWorkingSet(
                    workingSet,
                ),
        ) {
            "working continuity working set is not canonical"
        }

        return workingSet
    }

    fun validatePlan(
        plan: Plan,
    ): Plan {
        require(
            plan.schemaVersion ==
                PlanSchema &&
                plan.truthClass ==
                "SOURCE_BOUND_EFFECT_FREE_CONTINUITY_ROLLOVER_PLAN" &&
                plan.currentness ==
                "CURRENT" &&
                !plan.effectAuthorityGranted &&
                plan.effects ==
                PlanEffects() &&
                plan.reasonCodes ==
                plan.reasonCodes
                    .distinct()
                    .sorted(),
        ) {
            "working continuity plan identity/effects are invalid"
        }

        val expectedNextSafeAction =
            when (
                plan.state
            ) {
                PlanState.NoCheckpointRequired ->
                    "CONTINUE_WITH_CURRENT_CONTEXT_MODE"

                PlanState.CheckpointReady ->
                    "DELEGATE_REFERENCE_ONLY_CHECKPOINT_TO_WORKING_CONTINUITY_OWNER"

                PlanState.RolloverReady ->
                    "DELEGATE_CHECKPOINT_THEN_FORM_EXACT_SUCCESSOR_CONTEXT"
            }

        require(
            plan.nextSafeAction ==
                expectedNextSafeAction,
        ) {
            "working continuity plan next-safe-action is invalid"
        }

        when (
            plan.state
        ) {
            PlanState.NoCheckpointRequired ->
                require(
                    plan.checkpoint ==
                        null,
                ) {
                    "non-checkpoint working continuity plan must not carry a checkpoint"
                }

            PlanState.CheckpointReady,
            PlanState.RolloverReady,
            -> {
                val checkpoint =
                    validateCheckpoint(
                        requireNotNull(
                            plan.checkpoint,
                        ) {
                            "committable working continuity plan requires one checkpoint"
                        },
                    )
                require(
                    checkpoint.lineageRef ==
                        plan.lineageRef &&
                        checkpoint.threadRef ==
                        plan.threadRef &&
                        checkpoint.pressureRef ==
                        plan.pressureRef &&
                        checkpoint.pressureFingerprint ==
                        plan.pressureFingerprint,
                ) {
                    "working continuity plan/checkpoint binding is invalid"
                }
            }
        }

        val core =
            PlanCore(
                schemaVersion = plan.schemaVersion,
                truthClass = plan.truthClass,
                currentness = plan.currentness,
                state = plan.state,
                reasonCodes = plan.reasonCodes,
                pressureRef = plan.pressureRef,
                pressureFingerprint = plan.pressureFingerprint,
                lineageRef = plan.lineageRef,
                threadRef = plan.threadRef,
                checkpoint = plan.checkpoint,
                nextSafeAction = plan.nextSafeAction,
                effectAuthorityGranted =
                    plan.effectAuthorityGranted,
                effects = plan.effects,
            )
        val fingerprint =
            semanticHash(
                core,
            )

        require(
            plan.semanticFingerprint ==
                fingerprint &&
                plan.planRef ==
                "plan.vexlife-prod.continuity-rollover." +
                    fingerprint.take(
                        32,
                    ),
        ) {
            "working continuity plan semantic identity is invalid"
        }

        return plan
    }

    fun validateCheckpoint(
        checkpoint: Checkpoint,
    ): Checkpoint {
        require(
            checkpoint.schemaVersion ==
                CheckpointSchema &&
                checkpoint.truthClass ==
                "SOURCE_BOUND_NON_SEMANTIC_WORKING_CONTINUITY_CHECKPOINT" &&
                checkpoint.currentness ==
                "CURRENT" &&
                checkpoint.lifecycle ==
                "CURRENT" &&
                !checkpoint.effectAuthorityGranted &&
                !checkpoint.semanticMemoryPromotionAuthorized &&
                !checkpoint.modelOutputAcceptedAsMemory &&
                checkpoint.checkpointTruth ==
                CheckpointTruth(),
        ) {
            "working checkpoint is not current/reference-only"
        }

        validateWorkingSet(
            checkpoint.workingSet,
        )

        requireRefs(
            checkpoint.sourceRefs,
            "prior checkpoint sourceRefs",
        )
        require(
            checkpoint.sourceRefs ==
                checkpoint.sourceRefs
                    .distinct()
                    .sorted(),
        ) {
            "prior checkpoint sourceRefs must be canonical"
        }

        val core =
            CheckpointCore(
                schemaVersion = checkpoint.schemaVersion,
                truthClass = checkpoint.truthClass,
                currentness = checkpoint.currentness,
                lifecycle = checkpoint.lifecycle,
                lineageRef = checkpoint.lineageRef,
                threadRef = checkpoint.threadRef,
                pressureRef = checkpoint.pressureRef,
                pressureFingerprint = checkpoint.pressureFingerprint,
                pressureState = checkpoint.pressureState,
                promptReceiptRef = checkpoint.promptReceiptRef,
                workingSet = checkpoint.workingSet,
                priorCheckpointRefOrNull =
                    checkpoint.priorCheckpointRefOrNull,
                priorCheckpointFingerprintOrNull =
                    checkpoint.priorCheckpointFingerprintOrNull,
                sourceRefs = checkpoint.sourceRefs,
                effectAuthorityGranted =
                    checkpoint.effectAuthorityGranted,
                semanticMemoryPromotionAuthorized =
                    checkpoint.semanticMemoryPromotionAuthorized,
                modelOutputAcceptedAsMemory =
                    checkpoint.modelOutputAcceptedAsMemory,
                checkpointTruth = checkpoint.checkpointTruth,
            )
        val fingerprint =
            semanticHash(
                core,
            )

        require(
            checkpoint.semanticFingerprint ==
                fingerprint &&
                checkpoint.checkpointRef ==
                "checkpoint.vexlife-prod.working-continuity." +
                    fingerprint.take(
                        32,
                    ),
        ) {
            "working checkpoint semantic identity is invalid"
        }

        return checkpoint
    }

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

    private fun requireSha256Value(
        value: String,
        label: String,
    ) {
        require(
            value.matches(
                Regex("[0-9a-f]{64}"),
            ),
        ) {
            "$label must be one lowercase SHA-256"
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
