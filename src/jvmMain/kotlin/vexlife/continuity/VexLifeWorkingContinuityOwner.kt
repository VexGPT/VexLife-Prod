package vexlife.continuity

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Instant
import kotlinx.serialization.Serializable
import vexlife.contract.VexContractJson
import vexlife.contract.VexDurableContractFile
import vexlife.filesystem.VexExclusiveFileLease
import vexlife.filesystem.VexOwnedPath
import vexlife.memory.VexLifeMemoryContract

/**
 * Durable effect owner for the accepted effect-free working-continuity plan.
 *
 * Currentness commits only through head.json after immutable checkpoint + receipt evidence is
 * durable. This owner does not form provider prompt content, issue a Context Lease, mutate semantic
 * Memory, call the provider/model, or decide that rollover is required.
 */
class VexLifeWorkingContinuityOwner(
    productHome: Path,
) {
    companion object {
        const val CommitReceiptSchema =
            "vexlife-prod.working-continuity-commit-receipt/v1"
        const val HeadSchema =
            "vexlife-prod.working-continuity-head/v1"
        const val SuccessorClaimSchema =
            "vexlife-prod.working-continuity-successor-claim/v1"
        const val SuccessorContextSchema =
            "vexlife-prod.working-continuity-successor-context/v1"
        const val EffectOwnerRef =
            "owner.vexlife-prod.working-continuity.001"
    }

    @Serializable
    data class CommitEffects(
        val homeMutated: Boolean,
        val checkpointPersisted: Boolean,
        val priorCheckpointSuperseded: Boolean,
        val successorContextClaimed: Boolean = false,
        val successorContextIssued: Boolean = false,
        val memoryMutated: Boolean = false,
        val modelCalled: Boolean = false,
        val trainingRan: Boolean = false,
        val modelWeightsChanged: Boolean = false,
    )

    @Serializable
    data class CommitReceipt(
        val schemaVersion: String,
        val truthClass: String,
        val effectOwnerRef: String,
        val lineageRef: String,
        val threadRef: String,
        val checkpointRef: String,
        val checkpointFingerprint: String,
        val priorCheckpointRefOrNull: String?,
        val priorCheckpointFingerprintOrNull: String?,
        val disposition: String,
        val successorContextRequired: Boolean,
        val planRef: String,
        val planFingerprint: String,
        val committedAt: String,
        val effects: CommitEffects,
        val commitReceiptRef: String,
        val semanticFingerprint: String,
    )

    @Serializable
    data class Head(
        val schemaVersion: String,
        val truthClass: String,
        val effectOwnerRef: String,
        val lineageRef: String,
        val threadRef: String,
        val checkpointRef: String,
        val checkpointFingerprint: String,
        val commitReceiptRef: String,
        val commitReceiptFingerprint: String,
        val formedAt: String,
        val headRef: String,
        val semanticFingerprint: String,
    )

    @Serializable
    data class SuccessorClaimEffects(
        val homeMutated: Boolean,
        val checkpointMutated: Boolean = false,
        val currentHeadMutated: Boolean = false,
        val successorContextClaimed: Boolean,
        val successorContextIssued: Boolean = false,
        val semanticAcceptanceCreated: Boolean = false,
        val memoryPromotionPerformed: Boolean = false,
        val providerCalled: Boolean = false,
        val modelCalled: Boolean = false,
        val trainingRan: Boolean = false,
        val modelWeightsChanged: Boolean = false,
    )

    @Serializable
    data class SuccessorClaim(
        val schemaVersion: String,
        val truthClass: String,
        val effectOwnerRef: String,
        val lineageRef: String,
        val threadRef: String,
        val checkpointRef: String,
        val checkpointFingerprint: String,
        val commitReceiptRef: String,
        val commitReceiptFingerprint: String,
        val priorConversationHeadSha256: String,
        val priorRequestEventRef: String,
        val currentRequestEventBinding: VexLifeMemoryContract.CurrentRequestBinding,
        val claimedAt: String,
        val effects: SuccessorClaimEffects,
        val successorClaimRef: String,
        val semanticFingerprint: String,
    )

    @Serializable
    data class SuccessorContextEffects(
        val homeMutated: Boolean,
        val checkpointMutated: Boolean = false,
        val currentHeadMutated: Boolean = false,
        val successorClaimMutated: Boolean = false,
        val successorContextIssued: Boolean,
        val semanticAcceptanceCreated: Boolean = false,
        val memoryPromotionPerformed: Boolean = false,
        val providerCalled: Boolean = false,
        val modelCalled: Boolean = false,
        val trainingRan: Boolean = false,
        val modelWeightsChanged: Boolean = false,
    )

    @Serializable
    data class SuccessorContext(
        val schemaVersion: String,
        val truthClass: String,
        val effectOwnerRef: String,
        val contextMode: String,
        val lineageRef: String,
        val threadRef: String,
        val checkpointRef: String,
        val checkpointFingerprint: String,
        val commitReceiptRef: String,
        val commitReceiptFingerprint: String,
        val successorClaimRef: String,
        val successorClaimFingerprint: String,
        val priorConversationHeadSha256: String,
        val priorRequestEventRef: String,
        val currentRequestEventBinding: VexLifeMemoryContract.CurrentRequestBinding,
        val workingSet: VexLifeWorkingContinuity.WorkingSet,
        val sourceRefs: List<String>,
        val issuedAt: String,
        val effects: SuccessorContextEffects,
        val successorContextRef: String,
        val semanticFingerprint: String,
    )

    data class Current(
        val checkpoint: VexLifeWorkingContinuity.Checkpoint,
        val commitReceipt: CommitReceipt,
        val head: Head,
    )

    data class CommitResult(
        val state: String,
        val effectPerformedNow: Boolean,
        val current: Current,
    )

    data class ClaimResult(
        val state: String,
        val effectPerformedNow: Boolean,
        val claim: SuccessorClaim,
    )

    data class IssueResult(
        val state: String,
        val effectPerformedNow: Boolean,
        val context: SuccessorContext,
    )

    private data class Paths(
        val root: Path,
        val checkpoints: Path,
        val receipts: Path,
        val successorClaims: Path,
        val successorContexts: Path,
        val head: Path,
        val lock: Path,
    )

    @Serializable
    private data class SuccessorContextCore(
        val schemaVersion: String,
        val truthClass: String,
        val effectOwnerRef: String,
        val contextMode: String,
        val lineageRef: String,
        val threadRef: String,
        val checkpointRef: String,
        val checkpointFingerprint: String,
        val commitReceiptRef: String,
        val commitReceiptFingerprint: String,
        val successorClaimRef: String,
        val successorClaimFingerprint: String,
        val priorConversationHeadSha256: String,
        val priorRequestEventRef: String,
        val currentRequestEventBinding: VexLifeMemoryContract.CurrentRequestBinding,
        val workingSet: VexLifeWorkingContinuity.WorkingSet,
        val sourceRefs: List<String>,
        val issuedAt: String,
        val effects: SuccessorContextEffects,
    )

    @Serializable
    private data class CommitReceiptCore(
        val schemaVersion: String,
        val truthClass: String,
        val effectOwnerRef: String,
        val lineageRef: String,
        val threadRef: String,
        val checkpointRef: String,
        val checkpointFingerprint: String,
        val priorCheckpointRefOrNull: String?,
        val priorCheckpointFingerprintOrNull: String?,
        val disposition: String,
        val successorContextRequired: Boolean,
        val planRef: String,
        val planFingerprint: String,
        val committedAt: String,
        val effects: CommitEffects,
    )

    @Serializable
    private data class HeadCore(
        val schemaVersion: String,
        val truthClass: String,
        val effectOwnerRef: String,
        val lineageRef: String,
        val threadRef: String,
        val checkpointRef: String,
        val checkpointFingerprint: String,
        val commitReceiptRef: String,
        val commitReceiptFingerprint: String,
        val formedAt: String,
    )

    @Serializable
    private data class SuccessorClaimCore(
        val schemaVersion: String,
        val truthClass: String,
        val effectOwnerRef: String,
        val lineageRef: String,
        val threadRef: String,
        val checkpointRef: String,
        val checkpointFingerprint: String,
        val commitReceiptRef: String,
        val commitReceiptFingerprint: String,
        val priorConversationHeadSha256: String,
        val priorRequestEventRef: String,
        val currentRequestEventBinding: VexLifeMemoryContract.CurrentRequestBinding,
        val claimedAt: String,
        val effects: SuccessorClaimEffects,
    )

    private val home =
        VexOwnedPath.requireRoot(
            productHome,
            "canonical VexLife Home",
        )

    fun readCurrent(
        lineageRef: String,
        threadRef: String,
    ): Current? {
        requireRef(lineageRef, "lineageRef")
        requireRef(threadRef, "threadRef")
        val paths = paths(lineageRef, threadRef)
        if (
            VexOwnedPath.observeDirectory(home, paths.root) ==
            VexOwnedPath.DirectoryState.Missing
        ) {
            return null
        }

        val head =
            VexDurableContractFile.read<Head>(
                ownedRoot = home,
                path = paths.head,
            )
                ?: return null
        validateHead(head, lineageRef, threadRef)

        val checkpoint =
            requireNotNull(
                VexDurableContractFile.read<VexLifeWorkingContinuity.Checkpoint>(
                    ownedRoot = home,
                    path = paths.checkpoints.resolve(head.checkpointFingerprint + ".json"),
                ),
            ) {
                "working continuity current head checkpoint is missing"
            }
                .let(VexLifeWorkingContinuity::validateCheckpoint)

        val receipt =
            requireNotNull(
                VexDurableContractFile.read<CommitReceipt>(
                    ownedRoot = home,
                    path = paths.receipts.resolve(head.commitReceiptFingerprint + ".json"),
                ),
            ) {
                "working continuity current commit receipt is missing"
            }
                .also(::validateCommitReceipt)

        require(
            checkpoint.checkpointRef == head.checkpointRef &&
            checkpoint.semanticFingerprint == head.checkpointFingerprint &&
            receipt.commitReceiptRef == head.commitReceiptRef &&
            receipt.semanticFingerprint == head.commitReceiptFingerprint &&
            receipt.checkpointRef == checkpoint.checkpointRef &&
            receipt.checkpointFingerprint == checkpoint.semanticFingerprint &&
            receipt.priorCheckpointRefOrNull == checkpoint.priorCheckpointRefOrNull &&
            receipt.priorCheckpointFingerprintOrNull == checkpoint.priorCheckpointFingerprintOrNull,
        ) {
            "working continuity current head bindings are invalid"
        }

        return Current(
            checkpoint = checkpoint,
            commitReceipt = receipt,
            head = head,
        )
    }

    fun commitPlan(
        plan: VexLifeWorkingContinuity.Plan,
        pressureInput: VexLifeWorkingContinuity.PromptPressureInput,
        workingSet: VexLifeWorkingContinuity.WorkingSet,
        committedAt: Instant = Instant.now(),
    ): CommitResult {
        require(
            plan.state in setOf(
                VexLifeWorkingContinuity.PlanState.CheckpointReady,
                VexLifeWorkingContinuity.PlanState.RolloverReady,
            ) &&
            plan.checkpoint != null &&
            !plan.effectAuthorityGranted &&
            plan.effects == VexLifeWorkingContinuity.PlanEffects() &&
            plan.lineageRef == workingSet.lineageRef &&
            plan.threadRef == workingSet.threadRef,
        ) {
            "working continuity owner requires one exact effect-free committable plan"
        }

        VexLifeWorkingContinuity.validatePlan(
            plan,
        )

        val before = readCurrent(plan.lineageRef, plan.threadRef)

        if (
            sameCurrentPlan(
                current = before,
                plan = plan,
            )
        ) {
            return CommitResult(
                state = "IDEMPOTENT_CURRENT",
                effectPerformedNow = false,
                current = requireNotNull(
                    before,
                ),
            )
        }

        requireExactPlan(plan, pressureInput, workingSet, before)

        val paths = paths(plan.lineageRef, plan.threadRef)
        VexOwnedPath.ensureDirectory(home, paths.root)

        return VexExclusiveFileLease.acquire(paths.lock).use {
            val rebound = readCurrent(plan.lineageRef, plan.threadRef)

            if (
                sameCurrentPlan(
                    current = rebound,
                    plan = plan,
                )
            ) {
                return@use CommitResult(
                    state = "IDEMPOTENT_CURRENT",
                    effectPerformedNow = false,
                    current = requireNotNull(
                        rebound,
                    ),
                )
            }

            requireExactPlan(plan, pressureInput, workingSet, rebound)

            val checkpoint =
                VexLifeWorkingContinuity.validateCheckpoint(
                    requireNotNull(plan.checkpoint),
                )
            val receipt =
                formCommitReceipt(
                    plan = plan,
                    prior = rebound,
                    committedAt = committedAt,
                )
            val head =
                formHead(
                    checkpoint = checkpoint,
                    receipt = receipt,
                    formedAt = committedAt,
                )

            writeImmutableOrRequireExact(
                path = paths.checkpoints.resolve(checkpoint.semanticFingerprint + ".json"),
                value = checkpoint,
            )
            writeImmutableOrRequireExact(
                path = paths.receipts.resolve(receipt.semanticFingerprint + ".json"),
                value = receipt,
            )

            // The head is the commit point. Earlier immutable evidence is not current by itself.
            VexDurableContractFile.write(
                ownedRoot = home,
                path = paths.head,
                value = head,
            )

            val current =
                requireNotNull(readCurrent(plan.lineageRef, plan.threadRef)) {
                    "working continuity commit did not become current"
                }
            require(
                current.head == head &&
                current.checkpoint == checkpoint &&
                current.commitReceipt == receipt,
            ) {
                "working continuity commit readback mismatch"
            }

            CommitResult(
                state =
                    if (plan.state == VexLifeWorkingContinuity.PlanState.RolloverReady) {
                        "ROLLOVER_COMMITTED"
                    } else {
                        "CHECKPOINT_COMMITTED"
                    },
                effectPerformedNow = true,
                current = current,
            )
        }
    }

    fun claimSuccessorContext(
        lineageRef: String,
        threadRef: String,
        priorConversationHeadSha256: String,
        priorRequestEventRef: String,
        currentRequestEventBinding: VexLifeMemoryContract.CurrentRequestBinding,
        claimedAt: Instant = Instant.now(),
    ): ClaimResult {
        requireSha256(priorConversationHeadSha256, "priorConversationHeadSha256")
        requireRef(priorRequestEventRef, "priorRequestEventRef")
        requireCurrentRequestBinding(currentRequestEventBinding)

        val current =
            requireNotNull(readCurrent(lineageRef, threadRef)) {
                "working continuity successor claim requires one current checkpoint"
            }
        require(
            current.commitReceipt.disposition == "ROLLOVER_COMMITTED" &&
            current.commitReceipt.successorContextRequired &&
            current.checkpoint.workingSet.cursorEventRef == priorRequestEventRef &&
            currentRequestEventBinding.eventRef != priorRequestEventRef,
        ) {
            "working continuity current checkpoint does not admit a successor-context claim"
        }

        val paths = paths(lineageRef, threadRef)
        val claimPath =
            successorClaimPath(
                paths = paths,
                checkpointFingerprint =
                    current.checkpoint.semanticFingerprint,
                currentRequestEventHash =
                    currentRequestEventBinding.eventHash,
            )
        val existing =
            VexDurableContractFile.read<SuccessorClaim>(
                ownedRoot = home,
                path = claimPath,
            )
        if (existing != null) {
            validateSuccessorClaim(existing)
            require(
                sameSuccessorRequest(
                    existing,
                    current,
                    priorConversationHeadSha256,
                    priorRequestEventRef,
                    currentRequestEventBinding,
                ),
            ) {
                "working continuity request-scoped successor claim address contains different truth"
            }
            return ClaimResult(
                state = "IDEMPOTENT_CURRENT",
                effectPerformedNow = false,
                claim = existing,
            )
        }

        return VexExclusiveFileLease.acquire(paths.lock).use {
            val rebound =
                requireNotNull(readCurrent(lineageRef, threadRef)) {
                    "working continuity current checkpoint disappeared before successor claim"
                }
            require(rebound == current) {
                "working continuity current checkpoint changed before successor claim"
            }

            val reboundPath =
                successorClaimPath(
                    paths = paths,
                    checkpointFingerprint =
                        rebound.checkpoint.semanticFingerprint,
                    currentRequestEventHash =
                        currentRequestEventBinding.eventHash,
                )
            val reboundExisting =
                VexDurableContractFile.read<SuccessorClaim>(
                    ownedRoot = home,
                    path = reboundPath,
                )
            if (reboundExisting != null) {
                validateSuccessorClaim(reboundExisting)
                require(
                    sameSuccessorRequest(
                        reboundExisting,
                        rebound,
                        priorConversationHeadSha256,
                        priorRequestEventRef,
                        currentRequestEventBinding,
                    ),
                ) {
                    "working continuity request-scoped successor claim address contains different truth"
                }
                return@use ClaimResult(
                    state = "IDEMPOTENT_CURRENT",
                    effectPerformedNow = false,
                    claim = reboundExisting,
                )
            }

            val claim =
                formSuccessorClaim(
                    current = rebound,
                    priorConversationHeadSha256 = priorConversationHeadSha256,
                    priorRequestEventRef = priorRequestEventRef,
                    currentRequestEventBinding = currentRequestEventBinding,
                    claimedAt = claimedAt,
                )
            writeImmutableOrRequireExact(
                path = reboundPath,
                value = claim,
            )
            val readback =
                requireNotNull(
                    VexDurableContractFile.read<SuccessorClaim>(
                        ownedRoot = home,
                        path = reboundPath,
                    ),
                ) {
                    "working continuity successor claim did not persist"
                }
                    .also(::validateSuccessorClaim)
            require(readback == claim) {
                "working continuity successor claim readback mismatch"
            }

            ClaimResult(
                state = "CLAIMED",
                effectPerformedNow = true,
                claim = readback,
            )
        }
    }

    fun readSuccessorClaim(
        lineageRef: String,
        threadRef: String,
        currentRequestEventBinding: VexLifeMemoryContract.CurrentRequestBinding,
    ): SuccessorClaim? {
        requireCurrentRequestBinding(
            currentRequestEventBinding,
        )
        val current = readCurrent(lineageRef, threadRef) ?: return null
        val paths = paths(lineageRef, threadRef)
        return VexDurableContractFile.read<SuccessorClaim>(
            ownedRoot = home,
            path =
                successorClaimPath(
                    paths = paths,
                    checkpointFingerprint =
                        current.checkpoint.semanticFingerprint,
                    currentRequestEventHash =
                        currentRequestEventBinding.eventHash,
                ),
        )
            ?.also {
                validateSuccessorClaim(it)
                require(
                    it.checkpointRef == current.checkpoint.checkpointRef &&
                    it.checkpointFingerprint == current.checkpoint.semanticFingerprint &&
                    it.commitReceiptRef == current.commitReceipt.commitReceiptRef &&
                    it.commitReceiptFingerprint == current.commitReceipt.semanticFingerprint &&
                    it.currentRequestEventBinding == currentRequestEventBinding,
                ) {
                    "working continuity successor claim is not current for the exact request"
                }
            }
    }

    fun issueSuccessorContext(
        lineageRef: String,
        threadRef: String,
        currentRequestEventBinding: VexLifeMemoryContract.CurrentRequestBinding,
        successorClaimRef: String,
        successorClaimFingerprint: String,
        issuedAt: Instant = Instant.now(),
    ): IssueResult {
        requireCurrentRequestBinding(
            currentRequestEventBinding,
        )
        requireRef(
            successorClaimRef,
            "successorClaimRef",
        )
        requireSha256(
            successorClaimFingerprint,
            "successorClaimFingerprint",
        )

        val current =
            requireNotNull(
                readCurrent(
                    lineageRef,
                    threadRef,
                ),
            ) {
                "working continuity successor Context requires one current checkpoint"
            }
        val claim =
            requireNotNull(
                readSuccessorClaim(
                    lineageRef = lineageRef,
                    threadRef = threadRef,
                    currentRequestEventBinding =
                        currentRequestEventBinding,
                ),
            ) {
                "working continuity successor Context requires one exact current request claim"
            }
        require(
            claim.successorClaimRef ==
                successorClaimRef &&
                claim.semanticFingerprint ==
                successorClaimFingerprint,
        ) {
            "working continuity successor Context does not bind the exact current claim"
        }

        val paths =
            paths(
                lineageRef,
                threadRef,
            )
        val contextPath =
            successorContextPath(
                paths = paths,
                checkpointFingerprint =
                    current.checkpoint.semanticFingerprint,
                currentRequestEventHash =
                    currentRequestEventBinding.eventHash,
            )
        val existing =
            VexDurableContractFile.read<SuccessorContext>(
                ownedRoot = home,
                path = contextPath,
            )
        if (
            existing !=
                null
        ) {
            validateSuccessorContext(
                existing,
            )
            require(
                successorContextMatches(
                    context = existing,
                    current = current,
                    claim = claim,
                ) &&
                    existing.currentRequestEventBinding ==
                    currentRequestEventBinding,
            ) {
                "working continuity request-scoped successor Context address contains different truth"
            }
            return IssueResult(
                state = "IDEMPOTENT_CURRENT",
                effectPerformedNow = false,
                context = existing,
            )
        }

        return VexExclusiveFileLease
            .acquire(
                paths.lock,
            )
            .use {
                val rebound =
                    requireNotNull(
                        readCurrent(
                            lineageRef,
                            threadRef,
                        ),
                    ) {
                        "working continuity current checkpoint disappeared before successor Context issuance"
                    }
                require(
                    rebound ==
                        current,
                ) {
                    "working continuity current checkpoint changed before successor Context issuance"
                }
                val reboundClaim =
                    requireNotNull(
                        readSuccessorClaim(
                            lineageRef = lineageRef,
                            threadRef = threadRef,
                            currentRequestEventBinding =
                                currentRequestEventBinding,
                        ),
                    ) {
                        "working continuity exact request successor claim disappeared before Context issuance"
                    }
                require(
                    reboundClaim ==
                        claim,
                ) {
                    "working continuity successor claim changed before Context issuance"
                }

                val reboundPath =
                    successorContextPath(
                        paths = paths,
                        checkpointFingerprint =
                            rebound.checkpoint.semanticFingerprint,
                        currentRequestEventHash =
                            currentRequestEventBinding.eventHash,
                    )
                val reboundExisting =
                    VexDurableContractFile.read<SuccessorContext>(
                        ownedRoot = home,
                        path = reboundPath,
                    )
                if (
                    reboundExisting !=
                        null
                ) {
                    validateSuccessorContext(
                        reboundExisting,
                    )
                    require(
                        successorContextMatches(
                            context = reboundExisting,
                            current = rebound,
                            claim = reboundClaim,
                        ) &&
                            reboundExisting.currentRequestEventBinding ==
                            currentRequestEventBinding,
                    ) {
                        "working continuity request-scoped successor Context address contains different truth"
                    }
                    return@use IssueResult(
                        state = "IDEMPOTENT_CURRENT",
                        effectPerformedNow = false,
                        context = reboundExisting,
                    )
                }

                val context =
                    formSuccessorContext(
                        current = rebound,
                        claim = reboundClaim,
                        issuedAt = issuedAt,
                    )
                writeImmutableOrRequireExact(
                    path = reboundPath,
                    value = context,
                )

                val readback =
                    requireNotNull(
                        VexDurableContractFile.read<SuccessorContext>(
                            ownedRoot = home,
                            path = reboundPath,
                        ),
                    ) {
                        "working continuity successor Context did not persist"
                    }
                        .also(
                            ::validateSuccessorContext,
                        )
                require(
                    readback ==
                        context,
                ) {
                    "working continuity successor Context readback mismatch"
                }

                IssueResult(
                    state = "ISSUED",
                    effectPerformedNow = true,
                    context = readback,
                )
            }
    }

    fun readSuccessorContext(
        lineageRef: String,
        threadRef: String,
        currentRequestEventBinding: VexLifeMemoryContract.CurrentRequestBinding,
    ): SuccessorContext? {
        requireCurrentRequestBinding(
            currentRequestEventBinding,
        )
        val current =
            readCurrent(
                lineageRef,
                threadRef,
            )
                ?: return null
        val claim =
            readSuccessorClaim(
                lineageRef = lineageRef,
                threadRef = threadRef,
                currentRequestEventBinding =
                    currentRequestEventBinding,
            )
                ?: return null
        val paths =
            paths(
                lineageRef,
                threadRef,
            )
        return VexDurableContractFile.read<SuccessorContext>(
            ownedRoot = home,
            path =
                successorContextPath(
                    paths = paths,
                    checkpointFingerprint =
                        current.checkpoint.semanticFingerprint,
                    currentRequestEventHash =
                        currentRequestEventBinding.eventHash,
                ),
        )
            ?.also {
                validateSuccessorContext(
                    it,
                )
                require(
                    successorContextMatches(
                        context = it,
                        current = current,
                        claim = claim,
                    ) &&
                        it.currentRequestEventBinding ==
                        currentRequestEventBinding,
                ) {
                    "working continuity successor Context is not current for the exact request"
                }
            }
    }

    private fun paths(
        lineageRef: String,
        threadRef: String,
    ): Paths {
        requireRef(lineageRef, "lineageRef")
        requireRef(threadRef, "threadRef")
        val key = sha256(lineageRef + "|" + threadRef).take(32)
        val root =
            VexOwnedPath.resolveInside(
                home,
                "working-continuity/" + key,
            )
        return Paths(
            root = root,
            checkpoints = root.resolve("checkpoints"),
            receipts = root.resolve("receipts"),
            successorClaims = root.resolve("successor-context-claims"),
            successorContexts = root.resolve("successor-contexts"),
            head = root.resolve("head.json"),
            lock = root.resolve("owner.lock"),
        )
    }

    private fun successorClaimPath(
        paths: Paths,
        checkpointFingerprint: String,
        currentRequestEventHash: String,
    ): Path {
        requireSha256(
            checkpointFingerprint,
            "successor claim checkpointFingerprint",
        )
        requireSha256(
            currentRequestEventHash,
            "successor claim currentRequestEventHash",
        )
        return paths.successorClaims
            .resolve(
                checkpointFingerprint,
            )
            .resolve(
                currentRequestEventHash +
                    ".json",
            )
    }

    private fun successorContextPath(
        paths: Paths,
        checkpointFingerprint: String,
        currentRequestEventHash: String,
    ): Path {
        requireSha256(
            checkpointFingerprint,
            "successor Context checkpointFingerprint",
        )
        requireSha256(
            currentRequestEventHash,
            "successor Context currentRequestEventHash",
        )
        return paths.successorContexts
            .resolve(
                checkpointFingerprint,
            )
            .resolve(
                currentRequestEventHash +
                    ".json",
            )
    }

    private fun sameCurrentPlan(
        current: Current?,
        plan: VexLifeWorkingContinuity.Plan,
    ): Boolean {
        if (
            current ==
                null ||
            plan.checkpoint ==
                null
        ) {
            return false
        }

        return current.checkpoint.checkpointRef ==
            plan.checkpoint.checkpointRef &&
            current.checkpoint.semanticFingerprint ==
            plan.checkpoint.semanticFingerprint &&
            current.commitReceipt.planRef ==
            plan.planRef &&
            current.commitReceipt.planFingerprint ==
            plan.semanticFingerprint
    }

    private fun requireExactPlan(
        plan: VexLifeWorkingContinuity.Plan,
        pressureInput: VexLifeWorkingContinuity.PromptPressureInput,
        workingSet: VexLifeWorkingContinuity.WorkingSet,
        current: Current?,
    ) {
        val derived =
            VexLifeWorkingContinuity.formPlan(
                pressureInput = pressureInput,
                workingSet = workingSet,
                priorCheckpoint = current?.checkpoint,
            )
        require(derived == plan) {
            "working continuity owner source truth does not re-derive the exact plan"
        }
    }

    private fun formCommitReceipt(
        plan: VexLifeWorkingContinuity.Plan,
        prior: Current?,
        committedAt: Instant,
    ): CommitReceipt {
        val checkpoint = requireNotNull(plan.checkpoint)
        val disposition =
            if (plan.state == VexLifeWorkingContinuity.PlanState.RolloverReady) {
                "ROLLOVER_COMMITTED"
            } else {
                "CHECKPOINT_COMMITTED"
            }
        val core =
            CommitReceiptCore(
                schemaVersion = CommitReceiptSchema,
                truthClass = "NON_SEMANTIC_WORKING_CONTINUITY_COMMIT",
                effectOwnerRef = EffectOwnerRef,
                lineageRef = checkpoint.lineageRef,
                threadRef = checkpoint.threadRef,
                checkpointRef = checkpoint.checkpointRef,
                checkpointFingerprint = checkpoint.semanticFingerprint,
                priorCheckpointRefOrNull = prior?.checkpoint?.checkpointRef,
                priorCheckpointFingerprintOrNull = prior?.checkpoint?.semanticFingerprint,
                disposition = disposition,
                successorContextRequired = disposition == "ROLLOVER_COMMITTED",
                planRef = plan.planRef,
                planFingerprint = plan.semanticFingerprint,
                committedAt = committedAt.toString(),
                effects =
                    CommitEffects(
                        homeMutated = true,
                        checkpointPersisted = true,
                        priorCheckpointSuperseded = prior != null,
                    ),
            )
        val fingerprint = semanticHash(core)
        return CommitReceipt(
            schemaVersion = core.schemaVersion,
            truthClass = core.truthClass,
            effectOwnerRef = core.effectOwnerRef,
            lineageRef = core.lineageRef,
            threadRef = core.threadRef,
            checkpointRef = core.checkpointRef,
            checkpointFingerprint = core.checkpointFingerprint,
            priorCheckpointRefOrNull = core.priorCheckpointRefOrNull,
            priorCheckpointFingerprintOrNull = core.priorCheckpointFingerprintOrNull,
            disposition = core.disposition,
            successorContextRequired = core.successorContextRequired,
            planRef = core.planRef,
            planFingerprint = core.planFingerprint,
            committedAt = core.committedAt,
            effects = core.effects,
            commitReceiptRef =
                "receipt.vexlife-prod.working-continuity." + fingerprint.take(32),
            semanticFingerprint = fingerprint,
        )
    }

    private fun formHead(
        checkpoint: VexLifeWorkingContinuity.Checkpoint,
        receipt: CommitReceipt,
        formedAt: Instant,
    ): Head {
        val core =
            HeadCore(
                schemaVersion = HeadSchema,
                truthClass = "CURRENT_WORKING_CONTINUITY_HEAD",
                effectOwnerRef = EffectOwnerRef,
                lineageRef = checkpoint.lineageRef,
                threadRef = checkpoint.threadRef,
                checkpointRef = checkpoint.checkpointRef,
                checkpointFingerprint = checkpoint.semanticFingerprint,
                commitReceiptRef = receipt.commitReceiptRef,
                commitReceiptFingerprint = receipt.semanticFingerprint,
                formedAt = formedAt.toString(),
            )
        val fingerprint = semanticHash(core)
        return Head(
            schemaVersion = core.schemaVersion,
            truthClass = core.truthClass,
            effectOwnerRef = core.effectOwnerRef,
            lineageRef = core.lineageRef,
            threadRef = core.threadRef,
            checkpointRef = core.checkpointRef,
            checkpointFingerprint = core.checkpointFingerprint,
            commitReceiptRef = core.commitReceiptRef,
            commitReceiptFingerprint = core.commitReceiptFingerprint,
            formedAt = core.formedAt,
            headRef =
                "head.vexlife-prod.working-continuity." + fingerprint.take(32),
            semanticFingerprint = fingerprint,
        )
    }

    private fun formSuccessorContext(
        current: Current,
        claim: SuccessorClaim,
        issuedAt: Instant,
    ): SuccessorContext {
        val sourceRefs =
            (
                current.checkpoint.sourceRefs +
                    listOf(
                        current.checkpoint.checkpointRef,
                        current.commitReceipt.commitReceiptRef,
                        claim.successorClaimRef,
                    )
            )
                .distinct()
                .sorted()
        val core =
            SuccessorContextCore(
                schemaVersion = SuccessorContextSchema,
                truthClass =
                    "NON_SEMANTIC_WORKING_CONTINUITY_SUCCESSOR_CONTEXT",
                effectOwnerRef = EffectOwnerRef,
                contextMode =
                    "WORKING_CHECKPOINT_SUCCESSOR",
                lineageRef = current.checkpoint.lineageRef,
                threadRef = current.checkpoint.threadRef,
                checkpointRef = current.checkpoint.checkpointRef,
                checkpointFingerprint =
                    current.checkpoint.semanticFingerprint,
                commitReceiptRef =
                    current.commitReceipt.commitReceiptRef,
                commitReceiptFingerprint =
                    current.commitReceipt.semanticFingerprint,
                successorClaimRef =
                    claim.successorClaimRef,
                successorClaimFingerprint =
                    claim.semanticFingerprint,
                priorConversationHeadSha256 =
                    claim.priorConversationHeadSha256,
                priorRequestEventRef =
                    claim.priorRequestEventRef,
                currentRequestEventBinding =
                    claim.currentRequestEventBinding,
                workingSet =
                    current.checkpoint.workingSet,
                sourceRefs = sourceRefs,
                issuedAt = issuedAt.toString(),
                effects =
                    SuccessorContextEffects(
                        homeMutated = true,
                        successorContextIssued = true,
                    ),
            )
        val fingerprint =
            semanticHash(
                core,
            )
        return SuccessorContext(
            schemaVersion = core.schemaVersion,
            truthClass = core.truthClass,
            effectOwnerRef = core.effectOwnerRef,
            contextMode = core.contextMode,
            lineageRef = core.lineageRef,
            threadRef = core.threadRef,
            checkpointRef = core.checkpointRef,
            checkpointFingerprint = core.checkpointFingerprint,
            commitReceiptRef = core.commitReceiptRef,
            commitReceiptFingerprint =
                core.commitReceiptFingerprint,
            successorClaimRef = core.successorClaimRef,
            successorClaimFingerprint =
                core.successorClaimFingerprint,
            priorConversationHeadSha256 =
                core.priorConversationHeadSha256,
            priorRequestEventRef =
                core.priorRequestEventRef,
            currentRequestEventBinding =
                core.currentRequestEventBinding,
            workingSet = core.workingSet,
            sourceRefs = core.sourceRefs,
            issuedAt = core.issuedAt,
            effects = core.effects,
            successorContextRef =
                "context.vexlife-prod.working-continuity-successor." +
                    fingerprint.take(
                        32,
                    ),
            semanticFingerprint = fingerprint,
        )
    }

    private fun formSuccessorClaim(
        current: Current,
        priorConversationHeadSha256: String,
        priorRequestEventRef: String,
        currentRequestEventBinding: VexLifeMemoryContract.CurrentRequestBinding,
        claimedAt: Instant,
    ): SuccessorClaim {
        val core =
            SuccessorClaimCore(
                schemaVersion = SuccessorClaimSchema,
                truthClass = "NON_SEMANTIC_WORKING_CONTINUITY_SUCCESSOR_CLAIM",
                effectOwnerRef = EffectOwnerRef,
                lineageRef = current.checkpoint.lineageRef,
                threadRef = current.checkpoint.threadRef,
                checkpointRef = current.checkpoint.checkpointRef,
                checkpointFingerprint = current.checkpoint.semanticFingerprint,
                commitReceiptRef = current.commitReceipt.commitReceiptRef,
                commitReceiptFingerprint = current.commitReceipt.semanticFingerprint,
                priorConversationHeadSha256 = priorConversationHeadSha256,
                priorRequestEventRef = priorRequestEventRef,
                currentRequestEventBinding = currentRequestEventBinding,
                claimedAt = claimedAt.toString(),
                effects =
                    SuccessorClaimEffects(
                        homeMutated = true,
                        successorContextClaimed = true,
                    ),
            )
        val fingerprint = semanticHash(core)
        return SuccessorClaim(
            schemaVersion = core.schemaVersion,
            truthClass = core.truthClass,
            effectOwnerRef = core.effectOwnerRef,
            lineageRef = core.lineageRef,
            threadRef = core.threadRef,
            checkpointRef = core.checkpointRef,
            checkpointFingerprint = core.checkpointFingerprint,
            commitReceiptRef = core.commitReceiptRef,
            commitReceiptFingerprint = core.commitReceiptFingerprint,
            priorConversationHeadSha256 = core.priorConversationHeadSha256,
            priorRequestEventRef = core.priorRequestEventRef,
            currentRequestEventBinding = core.currentRequestEventBinding,
            claimedAt = core.claimedAt,
            effects = core.effects,
            successorClaimRef =
                "claim.vexlife-prod.working-continuity-successor." +
                    fingerprint.take(32),
            semanticFingerprint = fingerprint,
        )
    }

    private fun validateHead(
        value: Head,
        lineageRef: String,
        threadRef: String,
    ) {
        require(
            value.schemaVersion == HeadSchema &&
            value.truthClass == "CURRENT_WORKING_CONTINUITY_HEAD" &&
            value.effectOwnerRef == EffectOwnerRef &&
            value.lineageRef == lineageRef &&
            value.threadRef == threadRef,
        ) {
            "working continuity head identity is invalid"
        }
        requireRef(value.checkpointRef, "head checkpointRef")
        requireSha256(value.checkpointFingerprint, "head checkpointFingerprint")
        requireRef(value.commitReceiptRef, "head commitReceiptRef")
        requireSha256(value.commitReceiptFingerprint, "head commitReceiptFingerprint")
        val core =
            HeadCore(
                schemaVersion = value.schemaVersion,
                truthClass = value.truthClass,
                effectOwnerRef = value.effectOwnerRef,
                lineageRef = value.lineageRef,
                threadRef = value.threadRef,
                checkpointRef = value.checkpointRef,
                checkpointFingerprint = value.checkpointFingerprint,
                commitReceiptRef = value.commitReceiptRef,
                commitReceiptFingerprint = value.commitReceiptFingerprint,
                formedAt = value.formedAt,
            )
        val fingerprint = semanticHash(core)
        require(
            value.semanticFingerprint == fingerprint &&
            value.headRef ==
                "head.vexlife-prod.working-continuity." + fingerprint.take(32),
        ) {
            "working continuity head semantic identity is invalid"
        }
    }

    private fun validateCommitReceipt(
        value: CommitReceipt,
    ) {
        require(
            value.schemaVersion == CommitReceiptSchema &&
            value.truthClass == "NON_SEMANTIC_WORKING_CONTINUITY_COMMIT" &&
            value.effectOwnerRef == EffectOwnerRef &&
            value.disposition in setOf("CHECKPOINT_COMMITTED", "ROLLOVER_COMMITTED") &&
            value.successorContextRequired == (value.disposition == "ROLLOVER_COMMITTED") &&
            value.effects.priorCheckpointSuperseded ==
                (value.priorCheckpointRefOrNull != null) &&
            ((value.priorCheckpointRefOrNull == null) ==
                (value.priorCheckpointFingerprintOrNull == null)) &&
            value.effects.homeMutated &&
            value.effects.checkpointPersisted &&
            !value.effects.successorContextClaimed &&
            !value.effects.successorContextIssued &&
            !value.effects.memoryMutated &&
            !value.effects.modelCalled &&
            !value.effects.trainingRan &&
            !value.effects.modelWeightsChanged,
        ) {
            "working continuity commit receipt identity/effects are invalid"
        }
        requireRef(value.lineageRef, "receipt lineageRef")
        requireRef(value.threadRef, "receipt threadRef")
        requireRef(value.checkpointRef, "receipt checkpointRef")
        requireSha256(value.checkpointFingerprint, "receipt checkpointFingerprint")
        value.priorCheckpointRefOrNull?.let {
            requireRef(it, "receipt priorCheckpointRefOrNull")
        }
        value.priorCheckpointFingerprintOrNull?.let {
            requireSha256(it, "receipt priorCheckpointFingerprintOrNull")
        }
        requireRef(value.planRef, "receipt planRef")
        requireSha256(value.planFingerprint, "receipt planFingerprint")
        val core =
            CommitReceiptCore(
                schemaVersion = value.schemaVersion,
                truthClass = value.truthClass,
                effectOwnerRef = value.effectOwnerRef,
                lineageRef = value.lineageRef,
                threadRef = value.threadRef,
                checkpointRef = value.checkpointRef,
                checkpointFingerprint = value.checkpointFingerprint,
                priorCheckpointRefOrNull = value.priorCheckpointRefOrNull,
                priorCheckpointFingerprintOrNull = value.priorCheckpointFingerprintOrNull,
                disposition = value.disposition,
                successorContextRequired = value.successorContextRequired,
                planRef = value.planRef,
                planFingerprint = value.planFingerprint,
                committedAt = value.committedAt,
                effects = value.effects,
            )
        val fingerprint = semanticHash(core)
        require(
            value.semanticFingerprint == fingerprint &&
            value.commitReceiptRef ==
                "receipt.vexlife-prod.working-continuity." + fingerprint.take(32),
        ) {
            "working continuity commit receipt semantic identity is invalid"
        }
    }

    private fun validateSuccessorContext(
        value: SuccessorContext,
    ) {
        require(
            value.schemaVersion ==
                SuccessorContextSchema &&
                value.truthClass ==
                "NON_SEMANTIC_WORKING_CONTINUITY_SUCCESSOR_CONTEXT" &&
                value.effectOwnerRef ==
                EffectOwnerRef &&
                value.contextMode ==
                "WORKING_CHECKPOINT_SUCCESSOR" &&
                value.effects.homeMutated &&
                !value.effects.checkpointMutated &&
                !value.effects.currentHeadMutated &&
                !value.effects.successorClaimMutated &&
                value.effects.successorContextIssued &&
                !value.effects.semanticAcceptanceCreated &&
                !value.effects.memoryPromotionPerformed &&
                !value.effects.providerCalled &&
                !value.effects.modelCalled &&
                !value.effects.trainingRan &&
                !value.effects.modelWeightsChanged,
        ) {
            "working continuity successor Context identity/effects are invalid"
        }
        requireRef(
            value.lineageRef,
            "successor Context lineageRef",
        )
        requireRef(
            value.threadRef,
            "successor Context threadRef",
        )
        requireRef(
            value.checkpointRef,
            "successor Context checkpointRef",
        )
        requireSha256(
            value.checkpointFingerprint,
            "successor Context checkpointFingerprint",
        )
        requireRef(
            value.commitReceiptRef,
            "successor Context commitReceiptRef",
        )
        requireSha256(
            value.commitReceiptFingerprint,
            "successor Context commitReceiptFingerprint",
        )
        requireRef(
            value.successorClaimRef,
            "successor Context successorClaimRef",
        )
        requireSha256(
            value.successorClaimFingerprint,
            "successor Context successorClaimFingerprint",
        )
        requireSha256(
            value.priorConversationHeadSha256,
            "successor Context priorConversationHeadSha256",
        )
        requireRef(
            value.priorRequestEventRef,
            "successor Context priorRequestEventRef",
        )
        requireCurrentRequestBinding(
            value.currentRequestEventBinding,
        )
        VexLifeWorkingContinuity.validateWorkingSet(
            value.workingSet,
        )
        value.sourceRefs.forEach {
            requireRef(
                it,
                "successor Context sourceRef",
            )
        }
        Instant.parse(
            value.issuedAt,
        )
        require(
            value.workingSet.lineageRef ==
                value.lineageRef &&
                value.workingSet.threadRef ==
                value.threadRef &&
                value.workingSet.cursorEventRef ==
                value.priorRequestEventRef &&
                value.sourceRefs ==
                value.sourceRefs
                    .distinct()
                    .sorted(),
        ) {
            "working continuity successor Context working/source bindings are invalid"
        }

        val core =
            SuccessorContextCore(
                schemaVersion = value.schemaVersion,
                truthClass = value.truthClass,
                effectOwnerRef = value.effectOwnerRef,
                contextMode = value.contextMode,
                lineageRef = value.lineageRef,
                threadRef = value.threadRef,
                checkpointRef = value.checkpointRef,
                checkpointFingerprint =
                    value.checkpointFingerprint,
                commitReceiptRef =
                    value.commitReceiptRef,
                commitReceiptFingerprint =
                    value.commitReceiptFingerprint,
                successorClaimRef =
                    value.successorClaimRef,
                successorClaimFingerprint =
                    value.successorClaimFingerprint,
                priorConversationHeadSha256 =
                    value.priorConversationHeadSha256,
                priorRequestEventRef =
                    value.priorRequestEventRef,
                currentRequestEventBinding =
                    value.currentRequestEventBinding,
                workingSet = value.workingSet,
                sourceRefs = value.sourceRefs,
                issuedAt = value.issuedAt,
                effects = value.effects,
            )
        val fingerprint =
            semanticHash(
                core,
            )
        require(
            value.semanticFingerprint ==
                fingerprint &&
                value.successorContextRef ==
                "context.vexlife-prod.working-continuity-successor." +
                    fingerprint.take(
                        32,
                    ),
        ) {
            "working continuity successor Context semantic identity is invalid"
        }
    }

    private fun validateSuccessorClaim(
        value: SuccessorClaim,
    ) {
        require(
            value.schemaVersion == SuccessorClaimSchema &&
            value.truthClass == "NON_SEMANTIC_WORKING_CONTINUITY_SUCCESSOR_CLAIM" &&
            value.effectOwnerRef == EffectOwnerRef &&
            value.effects.homeMutated &&
            !value.effects.checkpointMutated &&
            !value.effects.currentHeadMutated &&
            value.effects.successorContextClaimed &&
            !value.effects.successorContextIssued &&
            !value.effects.semanticAcceptanceCreated &&
            !value.effects.memoryPromotionPerformed &&
            !value.effects.providerCalled &&
            !value.effects.modelCalled &&
            !value.effects.trainingRan &&
            !value.effects.modelWeightsChanged,
        ) {
            "working continuity successor claim identity/effects are invalid"
        }
        requireRef(value.lineageRef, "successor claim lineageRef")
        requireRef(value.threadRef, "successor claim threadRef")
        requireRef(value.checkpointRef, "successor claim checkpointRef")
        requireSha256(value.checkpointFingerprint, "successor claim checkpointFingerprint")
        requireRef(value.commitReceiptRef, "successor claim commitReceiptRef")
        requireSha256(value.commitReceiptFingerprint, "successor claim commitReceiptFingerprint")
        requireSha256(value.priorConversationHeadSha256, "successor claim priorConversationHeadSha256")
        requireRef(value.priorRequestEventRef, "successor claim priorRequestEventRef")
        requireCurrentRequestBinding(value.currentRequestEventBinding)
        val core =
            SuccessorClaimCore(
                schemaVersion = value.schemaVersion,
                truthClass = value.truthClass,
                effectOwnerRef = value.effectOwnerRef,
                lineageRef = value.lineageRef,
                threadRef = value.threadRef,
                checkpointRef = value.checkpointRef,
                checkpointFingerprint = value.checkpointFingerprint,
                commitReceiptRef = value.commitReceiptRef,
                commitReceiptFingerprint = value.commitReceiptFingerprint,
                priorConversationHeadSha256 = value.priorConversationHeadSha256,
                priorRequestEventRef = value.priorRequestEventRef,
                currentRequestEventBinding = value.currentRequestEventBinding,
                claimedAt = value.claimedAt,
                effects = value.effects,
            )
        val fingerprint = semanticHash(core)
        require(
            value.semanticFingerprint == fingerprint &&
            value.successorClaimRef ==
                "claim.vexlife-prod.working-continuity-successor." +
                    fingerprint.take(32),
        ) {
            "working continuity successor claim semantic identity is invalid"
        }
    }

    private fun successorContextMatches(
        context: SuccessorContext,
        current: Current,
        claim: SuccessorClaim,
    ): Boolean =
        context.lineageRef ==
            current.checkpoint.lineageRef &&
            context.threadRef ==
            current.checkpoint.threadRef &&
            context.checkpointRef ==
            current.checkpoint.checkpointRef &&
            context.checkpointFingerprint ==
            current.checkpoint.semanticFingerprint &&
            context.commitReceiptRef ==
            current.commitReceipt.commitReceiptRef &&
            context.commitReceiptFingerprint ==
            current.commitReceipt.semanticFingerprint &&
            context.successorClaimRef ==
            claim.successorClaimRef &&
            context.successorClaimFingerprint ==
            claim.semanticFingerprint &&
            context.priorConversationHeadSha256 ==
            claim.priorConversationHeadSha256 &&
            context.priorRequestEventRef ==
            claim.priorRequestEventRef &&
            context.currentRequestEventBinding ==
            claim.currentRequestEventBinding &&
            context.workingSet ==
            current.checkpoint.workingSet

    private fun sameSuccessorRequest(
        existing: SuccessorClaim,
        current: Current,
        priorConversationHeadSha256: String,
        priorRequestEventRef: String,
        currentRequestEventBinding: VexLifeMemoryContract.CurrentRequestBinding,
    ): Boolean =
        existing.lineageRef == current.checkpoint.lineageRef &&
        existing.threadRef == current.checkpoint.threadRef &&
        existing.checkpointRef == current.checkpoint.checkpointRef &&
        existing.checkpointFingerprint == current.checkpoint.semanticFingerprint &&
        existing.commitReceiptRef == current.commitReceipt.commitReceiptRef &&
        existing.commitReceiptFingerprint == current.commitReceipt.semanticFingerprint &&
        existing.priorConversationHeadSha256 == priorConversationHeadSha256 &&
        existing.priorRequestEventRef == priorRequestEventRef &&
        existing.currentRequestEventBinding == currentRequestEventBinding

    private inline fun <reified T> writeImmutableOrRequireExact(
        path: Path,
        value: T,
    ) {
        val existing =
            VexDurableContractFile.read<T>(
                ownedRoot = home,
                path = path,
            )
        if (existing == null) {
            VexDurableContractFile.write(
                ownedRoot = home,
                path = path,
                value = value,
            )
        } else {
            require(existing == value) {
                "working continuity immutable address already contains different truth"
            }
        }
    }

    private fun requireCurrentRequestBinding(
        value: VexLifeMemoryContract.CurrentRequestBinding,
    ) {
        requireRef(value.eventRef, "currentRequestEventBinding.eventRef")
        requireSha256(value.eventHash, "currentRequestEventBinding.eventHash")
        require(value.sequence > 0) {
            "successor current request sequence must follow an existing completed turn"
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
            label + " must be one stable ref"
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
            label + " must be one lowercase SHA-256"
        }
    }

    private inline fun <reified T> semanticHash(
        value: T,
    ): String =
        sha256(
            VexContractJson.canonicalEncode(
                value,
            ),
        )

    private fun sha256(
        value: String,
    ): String =
        MessageDigest
            .getInstance(
                "SHA-256",
            )
            .digest(
                value.toByteArray(
                    StandardCharsets.UTF_8,
                ),
            )
            .joinToString(
                "",
            ) {
                "%02x".format(
                    it,
                )
            }
}

// [VXG RealForever]
