package vexlife.companion

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Instant
import kotlinx.serialization.Serializable
import vexlife.contract.VexContractJson
import vexlife.contract.VexDurableContractFile
import vexlife.continuity.VexLifeWorkingContinuity
import vexlife.continuity.VexLifeWorkingContinuityOwner
import vexlife.conversation.DirectConversationStore
import vexlife.filesystem.VexOwnedPath
import vexlife.memory.VexLifeCurrentMemoryReader
import vexlife.memory.VexLifeMemoryContract

/**
 * Direct-turn composition of already-owned working-continuity semantics.
 *
 * Formation is effect-free and occurs while the exact prompt + Memory are still available.
 * Realization occurs only after the Conversation head is durable. A continuity-specific failure is
 * recorded as HELD and must not retroactively turn a completed Companion turn into a provider failure.
 */
object DirectWorkingContinuity {
    const val ReceiptSchema =
        "vexlife-prod.direct-working-continuity-post-commit/v1"

    @Serializable
    data class Effects(
        val postCommitReceiptPersisted: Boolean,
        val workingCheckpointPersisted: Boolean,
        val workingHeadMutated: Boolean,
        val successorContextClaimed: Boolean =
            false,
        val successorContextIssued: Boolean =
            false,
        val conversationMutated: Boolean =
            false,
        val semanticMemoryMutated: Boolean =
            false,
        val providerCalled: Boolean =
            false,
        val modelCalled: Boolean =
            false,
        val trainingRan: Boolean =
            false,
        val modelWeightsChanged: Boolean =
            false,
    )

    @Serializable
    @ConsistentCopyVisibility
    data class Receipt internal constructor(
        val schemaVersion: String,
        val truthClass: String,
        val turnRef: String,
        val promptReceiptRef: String,
        val planRefOrNull: String?,
        val planFingerprintOrNull: String?,
        val planStateOrNull: String?,
        val checkpointRefOrNull: String?,
        val checkpointFingerprintOrNull: String?,
        val ownerCommitReceiptRefOrNull: String?,
        val ownerCommitReceiptFingerprintOrNull: String?,
        val disposition: String,
        val failureClassOrNull: String?,
        val sourceRefs: List<String>,
        val formedAt: String,
        val effects: Effects,
        val receiptRef: String,
        val semanticFingerprint: String,
    )

    @ConsistentCopyVisibility
    data class CurrentSuccessor internal constructor(
        val context: VexLifeWorkingContinuityOwner.SuccessorContext,
    )

    @ConsistentCopyVisibility
    data class Prepared internal constructor(
        val pressureInput: VexLifeWorkingContinuity.PromptPressureInput,
        val workingSet: VexLifeWorkingContinuity.WorkingSet,
        internal val scoreCoordinates: VexLifeCurrentMemoryReader.CurrentScoreCoordinates?,
        val plan: VexLifeWorkingContinuity.Plan?,
        val formationFailureClassOrNull: String?,
    )

    @Serializable
    private data class ReceiptCore(
        val schemaVersion: String,
        val truthClass: String,
        val turnRef: String,
        val promptReceiptRef: String,
        val planRefOrNull: String?,
        val planFingerprintOrNull: String?,
        val planStateOrNull: String?,
        val checkpointRefOrNull: String?,
        val checkpointFingerprintOrNull: String?,
        val ownerCommitReceiptRefOrNull: String?,
        val ownerCommitReceiptFingerprintOrNull: String?,
        val disposition: String,
        val failureClassOrNull: String?,
        val sourceRefs: List<String>,
        val formedAt: String,
        val effects: Effects,
    )

    fun resolveSuccessorContext(
        productHome: Path,
        prior: DirectConversationStore.ImmediatePriorCompletedTurn?,
        currentRequest: DirectConversationStore.Event,
        issuedAt: Instant =
            Instant.now(),
    ): CurrentSuccessor? {
        if (
            prior ==
                null
        ) {
            return null
        }
        requireImmediatePriorBinding(
            prior = prior,
            currentRequest = currentRequest,
        )
        val owner =
            VexLifeWorkingContinuityOwner(
                productHome,
            )
        val current =
            owner.readCurrent(
                lineageRef =
                    currentRequest.companionLineageRef,
                threadRef =
                    currentRequest.threadRef,
            )
                ?: return null
        if (
            !rolloverEligible(
                current = current,
                prior = prior,
            )
        ) {
            return null
        }

        val binding =
            currentRequestBinding(
                currentRequest,
            )
        val claim =
            owner.claimSuccessorContext(
                lineageRef =
                    currentRequest.companionLineageRef,
                threadRef =
                    currentRequest.threadRef,
                priorConversationHeadSha256 =
                    prior.head.conversationHeadSha256,
                priorRequestEventRef =
                    prior.requestEvent.eventRef,
                currentRequestEventBinding =
                    binding,
                claimedAt = issuedAt,
            )
                .claim
        val issued =
            owner.issueSuccessorContext(
                lineageRef =
                    currentRequest.companionLineageRef,
                threadRef =
                    currentRequest.threadRef,
                currentRequestEventBinding =
                    binding,
                successorClaimRef =
                    claim.successorClaimRef,
                successorClaimFingerprint =
                    claim.semanticFingerprint,
                issuedAt = issuedAt,
            )
                .context
        val currentContext =
            requireNotNull(
                owner.readSuccessorContext(
                    lineageRef =
                        currentRequest.companionLineageRef,
                    threadRef =
                        currentRequest.threadRef,
                    currentRequestEventBinding =
                        binding,
                ),
            ) {
                "direct working continuity issued successor Context is not current"
            }
        require(
            currentContext ==
                issued,
        ) {
            "direct working continuity successor Context readback changed after issuance"
        }
        requireConversationBinding(
            context = currentContext,
            prior = prior,
            currentRequest = currentRequest,
        )
        return CurrentSuccessor(
            context = currentContext,
        )
    }

    fun requireSuccessorContextCurrent(
        productHome: Path,
        prior: DirectConversationStore.ImmediatePriorCompletedTurn?,
        currentRequest: DirectConversationStore.Event,
        expected: CurrentSuccessor?,
    ) {
        if (
            prior ==
                null
        ) {
            require(
                expected ==
                    null,
            ) {
                "first direct turn cannot carry working-checkpoint successor Context"
            }
            return
        }
        requireImmediatePriorBinding(
            prior = prior,
            currentRequest = currentRequest,
        )
        val owner =
            VexLifeWorkingContinuityOwner(
                productHome,
            )
        val current =
            owner.readCurrent(
                lineageRef =
                    currentRequest.companionLineageRef,
                threadRef =
                    currentRequest.threadRef,
            )
        val eligible =
            current !=
                null &&
                rolloverEligible(
                    current = current,
                    prior = prior,
                )

        if (
            expected ==
                null
        ) {
            require(
                !eligible,
            ) {
                "working rollover became required after prompt materialization"
            }
            return
        }

        require(
            eligible,
        ) {
            "working rollover successor Context is no longer eligible"
        }
        val binding =
            currentRequestBinding(
                currentRequest,
            )
        val observed =
            requireNotNull(
                owner.readSuccessorContext(
                    lineageRef =
                        currentRequest.companionLineageRef,
                    threadRef =
                        currentRequest.threadRef,
                    currentRequestEventBinding =
                        binding,
                ),
            ) {
                "working rollover successor Context is no longer current"
            }
        require(
            observed ==
                expected.context,
        ) {
            "working rollover successor Context changed before provider invocation"
        }
        requireConversationBinding(
            context = observed,
            prior = prior,
            currentRequest = currentRequest,
        )
    }

    fun prepare(
        productHome: Path,
        currentRequest: DirectConversationStore.Event,
        promptContext: DirectPromptContext.Receipt,
        memoryContext: VexLifeMemoryContract.Context,
        priorSuccessorContext: CurrentSuccessor? =
            null,
    ): Prepared {
        require(
            currentRequest.eventKind ==
                "REQUEST" &&
                promptContext.schemaVersion ==
                DirectPromptContext.ReceiptSchema &&
                promptContext.currentRequestEventRef ==
                currentRequest.eventRef &&
                promptContext.currentRequestEventSha256 ==
                currentRequest.eventSha256 &&
                !promptContext.trainingSelectionPerformed &&
                !promptContext.effectsPerformed,
        ) {
            "direct working continuity requires one exact effect-free prompt receipt/current request"
        }
        val validatedMemory =
            VexLifeMemoryContract.validateContext(
                memoryContext,
            )
        require(
            promptContext.memoryContextRef ==
                validatedMemory.memoryContextRef &&
                promptContext.memoryContextFingerprint ==
                validatedMemory.semanticFingerprint &&
                promptContext.memorySourceRefs ==
                validatedMemory.sourceRefs &&
                promptContext.memoryIncluded ==
                validatedMemory.included &&
                promptContext.memorySelectionPerformed ==
                validatedMemory.included,
        ) {
            "direct working continuity Memory does not match the exact prompt materialization"
        }
        if (
            priorSuccessorContext ==
                null
        ) {
            require(
                !promptContext.workingContinuityIncluded &&
                    promptContext.workingContinuityContextRef ==
                    null &&
                    promptContext.workingContinuityContextFingerprint ==
                    null &&
                    promptContext.workingContinuityCheckpointRef ==
                    null &&
                    promptContext.workingContinuitySuccessorClaimRef ==
                    null &&
                    promptContext.workingContinuitySourceRefs.isEmpty() &&
                    promptContext.workingContinuitySystemMessageSha256 ==
                    null &&
                    promptContext.workingContinuityEstimatedSystemMessageTokens ==
                    0,
            ) {
                "direct working continuity prompt receipt unexpectedly carries successor Context"
            }
        } else {
            require(
                priorSuccessorContext.context.contextMode ==
                    "WORKING_CHECKPOINT_SUCCESSOR" &&
                    priorSuccessorContext.context.lineageRef ==
                    currentRequest.companionLineageRef &&
                    priorSuccessorContext.context.threadRef ==
                    currentRequest.threadRef &&
                    promptContext.workingContinuityIncluded &&
                    promptContext.contextMode in
                    setOf(
                        "WORKING_CHECKPOINT_SUCCESSOR",
                        "SOURCE_RETURN_WORKING_CHECKPOINT_SUCCESSOR",
                    ) &&
                    promptContext.workingContinuityContextRef ==
                    priorSuccessorContext.context.successorContextRef &&
                    promptContext.workingContinuityContextFingerprint ==
                    priorSuccessorContext.context.semanticFingerprint &&
                    promptContext.workingContinuityCheckpointRef ==
                    priorSuccessorContext.context.checkpointRef &&
                    promptContext.workingContinuitySuccessorClaimRef ==
                    priorSuccessorContext.context.successorClaimRef &&
                    promptContext.workingContinuitySourceRefs ==
                    priorSuccessorContext.context.sourceRefs,
            ) {
                "direct working continuity prior successor Context does not match exact prompt materialization"
            }
        }

        val previousWorkingSet =
            priorSuccessorContext
                ?.context
                ?.workingSet
                ?.let(
                    VexLifeWorkingContinuity::validateWorkingSet,
                )
        var scoreCoordinates:
            VexLifeCurrentMemoryReader.CurrentScoreCoordinates? =
            null
        var scoreFailureClassOrNull: String? =
            null
        try {
            scoreCoordinates =
                VexLifeCurrentMemoryReader.readCurrentScoreCoordinates(
                    productHome = productHome,
                    threadRef = currentRequest.threadRef,
                )
        } catch (
            error: Exception,
        ) {
            scoreFailureClassOrNull =
                failureClass(
                    error,
                )
        }

        val workingSet =
            VexLifeWorkingContinuity.WorkingSet(
                lineageRef =
                    currentRequest.companionLineageRef,
                threadRef =
                    currentRequest.threadRef,
                cursorEventRef =
                    currentRequest.eventRef,
                activeWorkNodeRefs =
                    previousWorkingSet
                        ?.activeWorkNodeRefs
                        ?: emptyList(),
                currentIntentReceiptRefs =
                    previousWorkingSet
                        ?.currentIntentReceiptRefs
                        ?: emptyList(),
                openLoopRefs =
                    scoreCoordinates
                        ?.openLoopRefs
                        ?: emptyList(),
                currentStatementRefs =
                    scoreCoordinates
                        ?.currentStatementRefs
                        ?: emptyList(),
                currentContinuityRecordRefs =
                    previousWorkingSet
                        ?.currentContinuityRecordRefs
                        ?: emptyList(),
                currentDailyStratumRefOrNull =
                    if (
                        validatedMemory.included
                    ) {
                        validatedMemory.currentDailyStratumRef
                            ?: previousWorkingSet
                                ?.currentDailyStratumRefOrNull
                    } else {
                        previousWorkingSet
                            ?.currentDailyStratumRefOrNull
                    },
                sourceRefs =
                    (
                        previousWorkingSet
                            ?.sourceRefs
                            .orEmpty() +
                            priorSuccessorContext
                                ?.context
                                ?.sourceRefs
                                .orEmpty() +
                            validatedMemory.sourceRefs +
                            scoreCoordinates
                                ?.sourceRefs
                                .orEmpty() +
                            listOf(
                                promptContext.receiptRef,
                            )
                    )
                        .distinct()
                        .sorted(),
            )
                .let(
                    VexLifeWorkingContinuity::validateWorkingSet,
                )
        val pressureInput =
            VexLifeWorkingContinuity.PromptPressureInput(
                promptReceiptRef =
                    promptContext.receiptRef,
                providerMaterializedInputTokenEstimate =
                    promptContext.providerMaterializedInputTokenEstimate,
                inputTokenBudget =
                    promptContext.inputTokenBudget,
                reservedOutputTokens =
                    promptContext.reservedOutputTokens,
                hardTokenLimit =
                    promptContext.hardTokenLimit,
                memoryBoundedOutStatementCount =
                    validatedMemory.boundedOutStatementRefs.size,
                memoryTokenBoundedOutPageCount =
                    validatedMemory.tokenBoundedOutPageRefs.size,
                sourceRefs =
                    (
                        listOf(
                            promptContext.receiptRef,
                            validatedMemory.memoryContextRef,
                        ) +
                            priorSuccessorContext
                                ?.context
                                ?.let {
                                    listOf(
                                        it.successorContextRef,
                                    )
                                }
                                .orEmpty() +
                            scoreCoordinates
                                ?.sourceRefs
                                .orEmpty()
                    )
                        .distinct()
                        .sorted(),
            )

        val formation =
            if (
                scoreFailureClassOrNull !=
                    null
            ) {
                Prepared(
                    pressureInput = pressureInput,
                    workingSet = workingSet,
                    scoreCoordinates = null,
                    plan = null,
                    formationFailureClassOrNull =
                        scoreFailureClassOrNull,
                )
            } else {
                try {
                    val owner =
                        VexLifeWorkingContinuityOwner(
                            productHome,
                        )
                    val priorCheckpoint =
                        owner.readCurrent(
                            lineageRef =
                                currentRequest.companionLineageRef,
                            threadRef =
                                currentRequest.threadRef,
                        )
                            ?.checkpoint
                    Prepared(
                        pressureInput = pressureInput,
                        workingSet = workingSet,
                        scoreCoordinates =
                            requireNotNull(
                                scoreCoordinates,
                            ) {
                                "current Score coordinates disappeared after successful read"
                            },
                        plan =
                            VexLifeWorkingContinuity.formPlan(
                                pressureInput = pressureInput,
                                workingSet = workingSet,
                                priorCheckpoint = priorCheckpoint,
                            )
                                .let(
                                    VexLifeWorkingContinuity::validatePlan,
                                ),
                        formationFailureClassOrNull = null,
                    )
                } catch (
                    error: Exception,
                ) {
                    Prepared(
                        pressureInput = pressureInput,
                        workingSet = workingSet,
                        scoreCoordinates = scoreCoordinates,
                        plan = null,
                        formationFailureClassOrNull =
                            failureClass(
                                error,
                            ),
                    )
                }
            }

        return formation
    }

    /**
     * Realize a prepared continuity plan only after the corresponding Conversation head is durable.
     *
     * This method intentionally does not throw continuity failures back through the completed turn.
     * Instead it forms a HELD receipt. The receipt itself is persisted best-effort; when persistence
     * is unavailable, the returned in-process receipt records that fact without claiming durability.
     */
    fun commitAfterCompletedTurn(
        productHome: Path,
        turnRef: String,
        prepared: Prepared,
        formedAt: Instant =
            Instant.now(),
    ): Receipt {
        requireRef(
            turnRef,
            "turnRef",
        )

        val plan =
            prepared.plan
        val realization =
            if (
                plan ==
                    null
            ) {
                Realization(
                    disposition = "HELD",
                    failureClassOrNull =
                        prepared.formationFailureClassOrNull
                            ?: "UNKNOWN_FORMATION_FAILURE",
                    checkpointRefOrNull = null,
                    checkpointFingerprintOrNull = null,
                    ownerCommitReceiptRefOrNull = null,
                    ownerCommitReceiptFingerprintOrNull = null,
                    workingCheckpointPersisted = false,
                    workingHeadMutated = false,
                    sourceRefs = prepared.workingSet.sourceRefs,
                )
            } else {
                when (
                    plan.state
                ) {
                    VexLifeWorkingContinuity.PlanState.NoCheckpointRequired ->
                        Realization(
                            disposition = "NO_CHECKPOINT_REQUIRED",
                            failureClassOrNull = null,
                            checkpointRefOrNull = null,
                            checkpointFingerprintOrNull = null,
                            ownerCommitReceiptRefOrNull = null,
                            ownerCommitReceiptFingerprintOrNull = null,
                            workingCheckpointPersisted = false,
                            workingHeadMutated = false,
                            sourceRefs =
                                (
                                    prepared.workingSet.sourceRefs +
                                        listOf(
                                            plan.planRef,
                                        )
                                )
                                    .distinct()
                                    .sorted(),
                        )

                    VexLifeWorkingContinuity.PlanState.CheckpointReady,
                    VexLifeWorkingContinuity.PlanState.RolloverReady,
                    ->
                        try {
                            val expectedScoreCoordinates =
                                requireNotNull(
                                    prepared.scoreCoordinates,
                                ) {
                                    "committable working continuity plan lacks current Score coordinates"
                                }
                            val observedScoreCoordinates =
                                VexLifeCurrentMemoryReader.readCurrentScoreCoordinates(
                                    productHome = productHome,
                                    threadRef = prepared.workingSet.threadRef,
                                )
                            require(
                                observedScoreCoordinates ==
                                    expectedScoreCoordinates,
                            ) {
                                "current Score coordinates changed before working-continuity commit"
                            }

                            val committed =
                                VexLifeWorkingContinuityOwner(
                                    productHome,
                                )
                                    .commitPlan(
                                        plan = plan,
                                        pressureInput =
                                            prepared.pressureInput,
                                        workingSet =
                                            prepared.workingSet,
                                        committedAt =
                                            formedAt,
                                    )
                            val current =
                                committed.current
                            Realization(
                                disposition =
                                    current.commitReceipt.disposition,
                                failureClassOrNull = null,
                                checkpointRefOrNull =
                                    current.checkpoint.checkpointRef,
                                checkpointFingerprintOrNull =
                                    current.checkpoint.semanticFingerprint,
                                ownerCommitReceiptRefOrNull =
                                    current.commitReceipt.commitReceiptRef,
                                ownerCommitReceiptFingerprintOrNull =
                                    current.commitReceipt.semanticFingerprint,
                                workingCheckpointPersisted =
                                    current.commitReceipt.effects.checkpointPersisted,
                                workingHeadMutated =
                                    committed.effectPerformedNow,
                                sourceRefs =
                                    (
                                        prepared.workingSet.sourceRefs +
                                            listOf(
                                                plan.planRef,
                                                current.checkpoint.checkpointRef,
                                                current.commitReceipt.commitReceiptRef,
                                            )
                                    )
                                        .distinct()
                                        .sorted(),
                            )
                        } catch (
                            error: Exception,
                        ) {
                            Realization(
                                disposition = "HELD",
                                failureClassOrNull =
                                    failureClass(
                                        error,
                                    ),
                                checkpointRefOrNull =
                                    plan.checkpoint
                                        ?.checkpointRef,
                                checkpointFingerprintOrNull =
                                    plan.checkpoint
                                        ?.semanticFingerprint,
                                ownerCommitReceiptRefOrNull = null,
                                ownerCommitReceiptFingerprintOrNull = null,
                                workingCheckpointPersisted = false,
                                workingHeadMutated = false,
                                sourceRefs =
                                    (
                                        prepared.workingSet.sourceRefs +
                                            listOf(
                                                plan.planRef,
                                            )
                                    )
                                        .distinct()
                                        .sorted(),
                            )
                        }
                }
            }

        val durable =
            formReceipt(
                turnRef = turnRef,
                prepared = prepared,
                realization = realization,
                formedAt = formedAt,
                persisted = true,
            )

        return try {
            persist(
                productHome = productHome,
                receipt = durable,
            )
            requireNotNull(
                read(
                    productHome = productHome,
                    turnRef = turnRef,
                ),
            ) {
                "direct working continuity post-commit receipt did not become readable"
            }
                .also {
                    require(
                        it ==
                            durable,
                    ) {
                        "direct working continuity post-commit receipt readback mismatch"
                    }
                }
        } catch (
            _: Exception,
        ) {
            formReceipt(
                turnRef = turnRef,
                prepared = prepared,
                realization =
                    realization.copy(
                        disposition =
                            if (
                                realization.disposition ==
                                    "HELD"
                            ) {
                                "HELD"
                            } else {
                                "HELD_POST_COMMIT_RECEIPT"
                            },
                        failureClassOrNull =
                            realization.failureClassOrNull
                                ?: "POST_COMMIT_RECEIPT_PERSISTENCE_FAILED",
                    ),
                formedAt = formedAt,
                persisted = false,
            )
        }
    }

    fun read(
        productHome: Path,
        turnRef: String,
    ): Receipt? {
        requireRef(
            turnRef,
            "turnRef",
        )
        val directory =
            VexOwnedPath.resolveInside(
                productHome,
                "runtime/vexlife-prod/turn-working-continuity",
            )
        if (
            VexOwnedPath.observeDirectory(
                productHome,
                directory,
            ) ==
            VexOwnedPath.DirectoryState.Missing
        ) {
            return null
        }
        val path =
            VexOwnedPath.resolveInside(
                directory,
                turnRef +
                    ".json",
            )
        val value =
            VexDurableContractFile.read<Receipt>(
                ownedRoot = productHome,
                path = path,
            )
                ?: return null
        validateReceipt(
            value,
        )
        return value
    }

    @Serializable
    private data class Realization(
        val disposition: String,
        val failureClassOrNull: String?,
        val checkpointRefOrNull: String?,
        val checkpointFingerprintOrNull: String?,
        val ownerCommitReceiptRefOrNull: String?,
        val ownerCommitReceiptFingerprintOrNull: String?,
        val workingCheckpointPersisted: Boolean,
        val workingHeadMutated: Boolean,
        val sourceRefs: List<String>,
    )

    private fun formReceipt(
        turnRef: String,
        prepared: Prepared,
        realization: Realization,
        formedAt: Instant,
        persisted: Boolean,
    ): Receipt {
        require(
            realization.disposition in
                setOf(
                    "NO_CHECKPOINT_REQUIRED",
                    "CHECKPOINT_COMMITTED",
                    "ROLLOVER_COMMITTED",
                    "HELD",
                    "HELD_POST_COMMIT_RECEIPT",
                ),
        ) {
            "direct working continuity disposition is not admitted"
        }
        val plan =
            prepared.plan
        val sourceRefs =
            (
                realization.sourceRefs +
                    prepared.pressureInput.sourceRefs +
                    listOf(
                        prepared.pressureInput.promptReceiptRef,
                    )
            )
                .distinct()
                .sorted()
        val core =
            ReceiptCore(
                schemaVersion = ReceiptSchema,
                truthClass =
                    "DIRECT_TURN_WORKING_CONTINUITY_POST_COMMIT",
                turnRef = turnRef,
                promptReceiptRef =
                    prepared.pressureInput.promptReceiptRef,
                planRefOrNull =
                    plan
                        ?.planRef,
                planFingerprintOrNull =
                    plan
                        ?.semanticFingerprint,
                planStateOrNull =
                    plan
                        ?.state
                        ?.name,
                checkpointRefOrNull =
                    realization.checkpointRefOrNull,
                checkpointFingerprintOrNull =
                    realization.checkpointFingerprintOrNull,
                ownerCommitReceiptRefOrNull =
                    realization.ownerCommitReceiptRefOrNull,
                ownerCommitReceiptFingerprintOrNull =
                    realization.ownerCommitReceiptFingerprintOrNull,
                disposition =
                    realization.disposition,
                failureClassOrNull =
                    realization.failureClassOrNull,
                sourceRefs = sourceRefs,
                formedAt = formedAt.toString(),
                effects =
                    Effects(
                        postCommitReceiptPersisted =
                            persisted,
                        workingCheckpointPersisted =
                            realization.workingCheckpointPersisted,
                        workingHeadMutated =
                            realization.workingHeadMutated,
                    ),
            )
        val fingerprint =
            semanticHash(
                core,
            )
        return Receipt(
            schemaVersion = core.schemaVersion,
            truthClass = core.truthClass,
            turnRef = core.turnRef,
            promptReceiptRef = core.promptReceiptRef,
            planRefOrNull = core.planRefOrNull,
            planFingerprintOrNull =
                core.planFingerprintOrNull,
            planStateOrNull = core.planStateOrNull,
            checkpointRefOrNull =
                core.checkpointRefOrNull,
            checkpointFingerprintOrNull =
                core.checkpointFingerprintOrNull,
            ownerCommitReceiptRefOrNull =
                core.ownerCommitReceiptRefOrNull,
            ownerCommitReceiptFingerprintOrNull =
                core.ownerCommitReceiptFingerprintOrNull,
            disposition = core.disposition,
            failureClassOrNull =
                core.failureClassOrNull,
            sourceRefs = core.sourceRefs,
            formedAt = core.formedAt,
            effects = core.effects,
            receiptRef =
                "receipt.vexlife-prod.direct-working-continuity." +
                    fingerprint.take(
                        32,
                    ),
            semanticFingerprint = fingerprint,
        )
            .also(
                ::validateReceipt,
            )
    }

    private fun persist(
        productHome: Path,
        receipt: Receipt,
    ) {
        val path =
            pathFor(
                productHome = productHome,
                turnRef = receipt.turnRef,
            )
        val existing =
            VexDurableContractFile.read<Receipt>(
                ownedRoot = productHome,
                path = path,
            )
        if (
            existing ==
                null
        ) {
            VexDurableContractFile.write(
                ownedRoot = productHome,
                path = path,
                value = receipt,
            )
        } else {
            validateReceipt(
                existing,
            )
            require(
                existing ==
                    receipt,
            ) {
                "direct working continuity turn receipt already contains different truth"
            }
        }
    }

    private fun pathFor(
        productHome: Path,
        turnRef: String,
    ): Path {
        val directory =
            VexOwnedPath.resolveInside(
                productHome,
                "runtime/vexlife-prod/turn-working-continuity",
            )
        VexOwnedPath.ensureDirectory(
            productHome,
            directory,
        )
        return VexOwnedPath.resolveInside(
            directory,
            turnRef +
                ".json",
        )
    }

    private fun validateReceipt(
        value: Receipt,
    ) {
        require(
            value.schemaVersion ==
                ReceiptSchema &&
                value.truthClass ==
                "DIRECT_TURN_WORKING_CONTINUITY_POST_COMMIT" &&
                value.disposition in
                setOf(
                    "NO_CHECKPOINT_REQUIRED",
                    "CHECKPOINT_COMMITTED",
                    "ROLLOVER_COMMITTED",
                    "HELD",
                    "HELD_POST_COMMIT_RECEIPT",
                ) &&
                !value.effects.successorContextClaimed &&
                !value.effects.successorContextIssued &&
                !value.effects.conversationMutated &&
                !value.effects.semanticMemoryMutated &&
                !value.effects.providerCalled &&
                !value.effects.modelCalled &&
                !value.effects.trainingRan &&
                !value.effects.modelWeightsChanged,
        ) {
            "direct working continuity post-commit receipt identity/effects are invalid"
        }
        requireRef(
            value.turnRef,
            "working continuity turnRef",
        )
        requireRef(
            value.promptReceiptRef,
            "working continuity promptReceiptRef",
        )
        value.planRefOrNull
            ?.let {
                requireRef(
                    it,
                    "working continuity planRef",
                )
            }
        value.planFingerprintOrNull
            ?.let {
                requireSha256(
                    it,
                    "working continuity planFingerprint",
                )
            }
        value.checkpointRefOrNull
            ?.let {
                requireRef(
                    it,
                    "working continuity checkpointRef",
                )
            }
        value.checkpointFingerprintOrNull
            ?.let {
                requireSha256(
                    it,
                    "working continuity checkpointFingerprint",
                )
            }
        value.ownerCommitReceiptRefOrNull
            ?.let {
                requireRef(
                    it,
                    "working continuity ownerCommitReceiptRef",
                )
            }
        value.ownerCommitReceiptFingerprintOrNull
            ?.let {
                requireSha256(
                    it,
                    "working continuity ownerCommitReceiptFingerprint",
                )
            }
        value.sourceRefs.forEach {
            requireRef(
                it,
                "working continuity sourceRef",
            )
        }
        require(
            value.sourceRefs ==
                value.sourceRefs
                    .distinct()
                    .sorted(),
        ) {
            "direct working continuity sourceRefs must be canonical"
        }
        Instant.parse(
            value.formedAt,
        )

        val core =
            ReceiptCore(
                schemaVersion = value.schemaVersion,
                truthClass = value.truthClass,
                turnRef = value.turnRef,
                promptReceiptRef = value.promptReceiptRef,
                planRefOrNull = value.planRefOrNull,
                planFingerprintOrNull =
                    value.planFingerprintOrNull,
                planStateOrNull = value.planStateOrNull,
                checkpointRefOrNull =
                    value.checkpointRefOrNull,
                checkpointFingerprintOrNull =
                    value.checkpointFingerprintOrNull,
                ownerCommitReceiptRefOrNull =
                    value.ownerCommitReceiptRefOrNull,
                ownerCommitReceiptFingerprintOrNull =
                    value.ownerCommitReceiptFingerprintOrNull,
                disposition = value.disposition,
                failureClassOrNull =
                    value.failureClassOrNull,
                sourceRefs = value.sourceRefs,
                formedAt = value.formedAt,
                effects = value.effects,
            )
        val fingerprint =
            semanticHash(
                core,
            )
        require(
            value.semanticFingerprint ==
                fingerprint &&
                value.receiptRef ==
                "receipt.vexlife-prod.direct-working-continuity." +
                    fingerprint.take(
                        32,
                    ),
        ) {
            "direct working continuity post-commit receipt semantic identity is invalid"
        }

        when (
            value.disposition
        ) {
            "NO_CHECKPOINT_REQUIRED" ->
                require(
                    value.planStateOrNull ==
                        VexLifeWorkingContinuity.PlanState.NoCheckpointRequired.name &&
                        value.checkpointRefOrNull ==
                        null &&
                        value.ownerCommitReceiptRefOrNull ==
                        null &&
                        !value.effects.workingCheckpointPersisted &&
                        !value.effects.workingHeadMutated &&
                        value.failureClassOrNull ==
                        null,
                ) {
                    "no-checkpoint working continuity receipt shape is invalid"
                }

            "CHECKPOINT_COMMITTED",
            "ROLLOVER_COMMITTED",
            ->
                require(
                    value.planRefOrNull !=
                        null &&
                        value.planFingerprintOrNull !=
                        null &&
                        value.checkpointRefOrNull !=
                        null &&
                        value.checkpointFingerprintOrNull !=
                        null &&
                        value.ownerCommitReceiptRefOrNull !=
                        null &&
                        value.ownerCommitReceiptFingerprintOrNull !=
                        null &&
                        value.effects.workingCheckpointPersisted &&
                        value.failureClassOrNull ==
                        null,
                ) {
                    "committed working continuity receipt shape is invalid"
                }

            "HELD" ->
                require(
                    value.failureClassOrNull !=
                        null &&
                        !value.effects.workingCheckpointPersisted &&
                        !value.effects.workingHeadMutated,
                ) {
                    "held working continuity receipt shape is invalid"
                }

            "HELD_POST_COMMIT_RECEIPT" ->
                require(
                    value.failureClassOrNull !=
                        null &&
                        !value.effects.postCommitReceiptPersisted,
                ) {
                    "post-commit receipt persistence failure shape is invalid"
                }
        }
    }

    private fun rolloverEligible(
        current: VexLifeWorkingContinuityOwner.Current,
        prior: DirectConversationStore.ImmediatePriorCompletedTurn,
    ): Boolean =
        current.commitReceipt.disposition ==
            "ROLLOVER_COMMITTED" &&
            current.commitReceipt.successorContextRequired &&
            current.checkpoint.workingSet.cursorEventRef ==
            prior.requestEvent.eventRef

    private fun currentRequestBinding(
        currentRequest: DirectConversationStore.Event,
    ): VexLifeMemoryContract.CurrentRequestBinding =
        VexLifeMemoryContract.CurrentRequestBinding(
            eventRef =
                currentRequest.eventRef,
            eventHash =
                currentRequest.eventHash,
            sequence =
                currentRequest.sequence,
        )

    private fun requireImmediatePriorBinding(
        prior: DirectConversationStore.ImmediatePriorCompletedTurn,
        currentRequest: DirectConversationStore.Event,
    ) {
        require(
            prior.exactEventFileReadCount ==
                2 &&
                !prior.wholeHistoryEventEnumerationPerformed &&
                currentRequest.eventKind ==
                "REQUEST" &&
                currentRequest.sequence ==
                prior.head.sequence +
                1 &&
                currentRequest.priorEventSha256 ==
                prior.responseEvent.eventSha256 &&
                currentRequest.companionLineageRef ==
                prior.head.companionLineageRef &&
                currentRequest.threadRef ==
                prior.head.threadRef &&
                prior.requestEvent.channelRef ==
                prior.responseEvent.channelRef &&
                currentRequest.channelRef ==
                prior.responseEvent.channelRef,
        ) {
            "direct working continuity current request does not immediately follow the exact prior completed turn"
        }
    }

    private fun requireConversationBinding(
        context: VexLifeWorkingContinuityOwner.SuccessorContext,
        prior: DirectConversationStore.ImmediatePriorCompletedTurn,
        currentRequest: DirectConversationStore.Event,
    ) {
        require(
            context.contextMode ==
                "WORKING_CHECKPOINT_SUCCESSOR" &&
                context.lineageRef ==
                currentRequest.companionLineageRef &&
                context.threadRef ==
                currentRequest.threadRef &&
                context.priorConversationHeadSha256 ==
                prior.head.conversationHeadSha256 &&
                context.priorRequestEventRef ==
                prior.requestEvent.eventRef &&
                context.currentRequestEventBinding ==
                currentRequestBinding(
                    currentRequest,
                ) &&
                context.workingSet.cursorEventRef ==
                prior.requestEvent.eventRef,
        ) {
            "working rollover successor Context does not bind exact Conversation currentness"
        }
    }

    private fun failureClass(
        error: Throwable,
    ): String =
        error.javaClass.simpleName
            .ifBlank {
                "Throwable"
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

    private fun requireSha256(
        value: String,
        label: String,
    ) {
        require(
            value.matches(
                Regex(
                    "[0-9a-f]{64}",
                ),
            ),
        ) {
            "$label must be one lowercase SHA-256"
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
                    it.toInt() and
                        0xff,
                )
            }
    }
}

// [VXG RealForever]
