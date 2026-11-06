package vexlife.continuity

import java.nio.file.Files
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vexlife.memory.VexLifeMemoryContract

class VexLifeWorkingContinuityOwnerTest {
    private fun pressureInput(
        estimate: Int,
    ): VexLifeWorkingContinuity.PromptPressureInput =
        VexLifeWorkingContinuity.PromptPressureInput(
            promptReceiptRef =
                "receipt.vexlife.prompt-context.owner-test",
            providerMaterializedInputTokenEstimate =
                estimate,
            inputTokenBudget =
                192_000,
            reservedOutputTokens =
                16_000,
            hardTokenLimit =
                208_000,
            memoryBoundedOutStatementCount =
                0,
            memoryTokenBoundedOutPageCount =
                0,
            sourceRefs =
                listOf(
                    "source.vexlife.prompt-context.owner-test",
                ),
        )

    private fun workingSet(
        cursor: String =
            "event.vexlife.request.owner-test",
        statement: String =
            "statement.vexlife.owner-test.001",
    ): VexLifeWorkingContinuity.WorkingSet =
        VexLifeWorkingContinuity.WorkingSet(
            lineageRef =
                "companion-lineage.vexlife.owner-test",
            threadRef =
                "thread.self-development.open-conversation",
            cursorEventRef =
                cursor,
            activeWorkNodeRefs =
                listOf(
                    "work-node.vexlife.owner-test",
                ),
            currentIntentReceiptRefs =
                listOf(
                    "receipt.vexlife.intent.owner-test",
                ),
            openLoopRefs =
                listOf(
                    "loop.vexlife.owner-test",
                ),
            currentStatementRefs =
                listOf(
                    statement,
                ),
            currentContinuityRecordRefs =
                listOf(
                    "continuity.vexlife.owner-test",
                ),
            currentDailyStratumRefOrNull =
                "daily-stratum.vexlife.owner-test",
            sourceRefs =
                listOf(
                    "source.vexlife.working-set.owner-test",
                ),
        )

    private fun checkpointPlan(
        workingSet: VexLifeWorkingContinuity.WorkingSet =
            workingSet(),
        prior: VexLifeWorkingContinuity.Checkpoint? =
            null,
    ): Pair<
        VexLifeWorkingContinuity.PromptPressureInput,
        VexLifeWorkingContinuity.Plan
    > {
        val input =
            pressureInput(
                estimate = 160_000,
            )
        return input to
            VexLifeWorkingContinuity.formPlan(
                pressureInput = input,
                workingSet = workingSet,
                priorCheckpoint = prior,
            )
    }

    private fun rolloverPlan(
        workingSet: VexLifeWorkingContinuity.WorkingSet =
            workingSet(),
        prior: VexLifeWorkingContinuity.Checkpoint? =
            null,
    ): Pair<
        VexLifeWorkingContinuity.PromptPressureInput,
        VexLifeWorkingContinuity.Plan
    > {
        val input =
            pressureInput(
                estimate = 176_000,
            )
        return input to
            VexLifeWorkingContinuity.formPlan(
                pressureInput = input,
                workingSet = workingSet,
                priorCheckpoint = prior,
            )
    }

    @Test
    fun exactCheckpointPlanCommitsOneCurrentHeadAndIsIdempotent() {
        val home =
            Files.createTempDirectory(
                "vexlife-working-owner-checkpoint-",
            )
                .toAbsolutePath()

        try {
            val owner =
                VexLifeWorkingContinuityOwner(
                    home,
                )
            val working =
                workingSet()
            val (
                input,
                plan,
            ) =
                checkpointPlan(
                    working,
                )

            val first =
                owner.commitPlan(
                    plan = plan,
                    pressureInput = input,
                    workingSet = working,
                    committedAt =
                        Instant.parse(
                            "2026-11-06T10:00:00Z",
                        ),
                )

            assertEquals(
                "CHECKPOINT_COMMITTED",
                first.state,
            )
            assertTrue(
                first.effectPerformedNow,
            )
            assertEquals(
                plan.checkpoint,
                first.current.checkpoint,
            )
            assertTrue(
                first.current.commitReceipt.effects.homeMutated,
            )
            assertTrue(
                first.current.commitReceipt.effects.checkpointPersisted,
            )
            assertFalse(
                first.current.commitReceipt.successorContextRequired,
            )

            val second =
                owner.commitPlan(
                    plan = plan,
                    pressureInput = input,
                    workingSet = working,
                    committedAt =
                        Instant.parse(
                            "2026-11-06T10:01:00Z",
                        ),
                )

            assertEquals(
                "IDEMPOTENT_CURRENT",
                second.state,
            )
            assertFalse(
                second.effectPerformedNow,
            )
            assertEquals(
                first.current,
                second.current,
            )
            assertNull(
                owner.readSuccessorClaim(
                    lineageRef = working.lineageRef,
                    threadRef = working.threadRef,
                    currentRequestEventBinding =
                        VexLifeMemoryContract.CurrentRequestBinding(
                            eventRef =
                                "event.vexlife.request.owner-test.none",
                            eventHash =
                                "9".repeat(
                                    64,
                                ),
                            sequence =
                                2L,
                        ),
                ),
            )
        } finally {
            home
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun tamperedPlanCannotMasqueradeAsIdempotentCurrent() {
        val home =
            Files.createTempDirectory(
                "vexlife-working-owner-idempotent-tamper-",
            )
                .toAbsolutePath()

        try {
            val owner =
                VexLifeWorkingContinuityOwner(
                    home,
                )
            val working =
                workingSet()
            val (
                input,
                plan,
            ) =
                checkpointPlan(
                    working,
                )
            val committed =
                owner.commitPlan(
                    plan = plan,
                    pressureInput = input,
                    workingSet = working,
                    committedAt =
                        Instant.parse(
                            "2026-11-06T10:05:00Z",
                        ),
                )
            val tampered =
                plan.copy(
                    semanticFingerprint =
                        "0".repeat(
                            64,
                        ),
                )

            assertFailsWith<IllegalArgumentException> {
                owner.commitPlan(
                    plan = tampered,
                    pressureInput = input,
                    workingSet = working,
                    committedAt =
                        Instant.parse(
                            "2026-11-06T10:06:00Z",
                        ),
                )
            }

            assertEquals(
                committed.current,
                owner.readCurrent(
                    lineageRef = working.lineageRef,
                    threadRef = working.threadRef,
                ),
            )
        } finally {
            home
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun staleOrTamperedPlanFailsBeforeDurableOwnerNamespaceIsCreated() {
        val home =
            Files.createTempDirectory(
                "vexlife-working-owner-tamper-",
            )
                .toAbsolutePath()

        try {
            val owner =
                VexLifeWorkingContinuityOwner(
                    home,
                )
            val working =
                workingSet()
            val (
                input,
                plan,
            ) =
                checkpointPlan(
                    working,
                )
            val tampered =
                plan.copy(
                    semanticFingerprint =
                        "0".repeat(
                            64,
                        ),
                )

            assertFailsWith<IllegalArgumentException> {
                owner.commitPlan(
                    plan = tampered,
                    pressureInput = input,
                    workingSet = working,
                )
            }

            assertFalse(
                Files.exists(
                    home.resolve(
                        "working-continuity",
                    ),
                ),
            )
        } finally {
            home
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun successorCheckpointMovesCurrentHeadWhilePriorImmutableEvidenceRemains() {
        val home =
            Files.createTempDirectory(
                "vexlife-working-owner-supersession-",
            )
                .toAbsolutePath()

        try {
            val owner =
                VexLifeWorkingContinuityOwner(
                    home,
                )
            val firstWorking =
                workingSet()
            val (
                firstInput,
                firstPlan,
            ) =
                checkpointPlan(
                    firstWorking,
                )
            val first =
                owner.commitPlan(
                    plan = firstPlan,
                    pressureInput = firstInput,
                    workingSet = firstWorking,
                    committedAt =
                        Instant.parse(
                            "2026-11-06T10:10:00Z",
                        ),
                )

            val secondWorking =
                workingSet(
                    cursor =
                        "event.vexlife.request.owner-test.next",
                    statement =
                        "statement.vexlife.owner-test.002",
                )
            val (
                secondInput,
                secondPlan,
            ) =
                checkpointPlan(
                    workingSet = secondWorking,
                    prior = first.current.checkpoint,
                )
            val second =
                owner.commitPlan(
                    plan = secondPlan,
                    pressureInput = secondInput,
                    workingSet = secondWorking,
                    committedAt =
                        Instant.parse(
                            "2026-11-06T10:11:00Z",
                        ),
                )

            assertEquals(
                "CHECKPOINT_COMMITTED",
                second.state,
            )
            assertTrue(
                second.current.commitReceipt.effects.priorCheckpointSuperseded,
            )
            assertEquals(
                first.current.checkpoint.checkpointRef,
                second.current.commitReceipt.priorCheckpointRefOrNull,
            )
            assertEquals(
                second.current,
                owner.readCurrent(
                    lineageRef = secondWorking.lineageRef,
                    threadRef = secondWorking.threadRef,
                ),
            )

            val checkpointFiles =
                Files.walk(
                    home.resolve(
                        "working-continuity",
                    ),
                )
                    .use {
                        paths ->
                        paths
                            .filter {
                                path ->
                                path.parent
                                    ?.fileName
                                    ?.toString() ==
                                    "checkpoints" &&
                                    path.fileName
                                        .toString()
                                        .endsWith(
                                            ".json",
                                        )
                            }
                            .count()
                    }

            assertEquals(
                2L,
                checkpointFiles,
            )
        } finally {
            home
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun checkpointOnlyCommitCannotClaimSuccessorContext() {
        val home =
            Files.createTempDirectory(
                "vexlife-working-owner-no-successor-",
            )
                .toAbsolutePath()

        try {
            val owner =
                VexLifeWorkingContinuityOwner(
                    home,
                )
            val working =
                workingSet()
            val (
                input,
                plan,
            ) =
                checkpointPlan(
                    working,
                )
            owner.commitPlan(
                plan = plan,
                pressureInput = input,
                workingSet = working,
            )

            assertFailsWith<IllegalArgumentException> {
                owner.claimSuccessorContext(
                    lineageRef = working.lineageRef,
                    threadRef = working.threadRef,
                    priorConversationHeadSha256 =
                        "a".repeat(
                            64,
                        ),
                    priorRequestEventRef =
                        working.cursorEventRef,
                    currentRequestEventBinding =
                        VexLifeMemoryContract.CurrentRequestBinding(
                            eventRef =
                                "event.vexlife.request.owner-test.current",
                            eventHash =
                                "b".repeat(
                                    64,
                                ),
                            sequence =
                                2L,
                        ),
                )
            }
        } finally {
            home
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun rolloverCommitAdmitsOneExactIdempotentSuccessorClaimWithoutIssuingContext() {
        val home =
            Files.createTempDirectory(
                "vexlife-working-owner-rollover-",
            )
                .toAbsolutePath()

        try {
            val owner =
                VexLifeWorkingContinuityOwner(
                    home,
                )
            val working =
                workingSet()
            val (
                input,
                plan,
            ) =
                rolloverPlan(
                    working,
                )
            val committed =
                owner.commitPlan(
                    plan = plan,
                    pressureInput = input,
                    workingSet = working,
                    committedAt =
                        Instant.parse(
                            "2026-11-06T10:20:00Z",
                        ),
                )

            assertEquals(
                "ROLLOVER_COMMITTED",
                committed.state,
            )
            assertTrue(
                committed.current.commitReceipt.successorContextRequired,
            )

            val binding =
                VexLifeMemoryContract.CurrentRequestBinding(
                    eventRef =
                        "event.vexlife.request.owner-test.successor",
                    eventHash =
                        "b".repeat(
                            64,
                        ),
                    sequence =
                        2L,
                )
            val first =
                owner.claimSuccessorContext(
                    lineageRef = working.lineageRef,
                    threadRef = working.threadRef,
                    priorConversationHeadSha256 =
                        "a".repeat(
                            64,
                        ),
                    priorRequestEventRef =
                        working.cursorEventRef,
                    currentRequestEventBinding =
                        binding,
                    claimedAt =
                        Instant.parse(
                            "2026-11-06T10:21:00Z",
                        ),
                )

            assertEquals(
                "CLAIMED",
                first.state,
            )
            assertTrue(
                first.effectPerformedNow,
            )
            assertTrue(
                first.claim.effects.successorContextClaimed,
            )
            assertFalse(
                first.claim.effects.successorContextIssued,
            )
            assertFalse(
                first.claim.effects.memoryPromotionPerformed,
            )
            assertFalse(
                first.claim.effects.providerCalled,
            )
            assertFalse(
                first.claim.effects.modelCalled,
            )

            val second =
                owner.claimSuccessorContext(
                    lineageRef = working.lineageRef,
                    threadRef = working.threadRef,
                    priorConversationHeadSha256 =
                        "a".repeat(
                            64,
                        ),
                    priorRequestEventRef =
                        working.cursorEventRef,
                    currentRequestEventBinding =
                        binding,
                    claimedAt =
                        Instant.parse(
                            "2026-11-06T10:22:00Z",
                        ),
                )

            assertEquals(
                "IDEMPOTENT_CURRENT",
                second.state,
            )
            assertFalse(
                second.effectPerformedNow,
            )
            assertEquals(
                first.claim,
                second.claim,
            )
            assertEquals(
                first.claim,
                assertNotNull(
                    owner.readSuccessorClaim(
                        lineageRef = working.lineageRef,
                        threadRef = working.threadRef,
                        currentRequestEventBinding =
                            binding,
                    ),
                ),
            )
        } finally {
            home
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun exactCurrentRolloverClaimIssuesOneIdempotentSuccessorContext() {
        val home =
            Files.createTempDirectory(
                "vexlife-working-owner-context-",
            )
                .toAbsolutePath()

        try {
            val owner =
                VexLifeWorkingContinuityOwner(
                    home,
                )
            val working =
                workingSet()
            val (
                input,
                plan,
            ) =
                rolloverPlan(
                    working,
                )
            val committed =
                owner.commitPlan(
                    plan = plan,
                    pressureInput = input,
                    workingSet = working,
                    committedAt =
                        Instant.parse(
                            "2026-11-06T10:30:00Z",
                        ),
                )
            val claim =
                owner.claimSuccessorContext(
                    lineageRef = working.lineageRef,
                    threadRef = working.threadRef,
                    priorConversationHeadSha256 =
                        "a".repeat(
                            64,
                        ),
                    priorRequestEventRef =
                        working.cursorEventRef,
                    currentRequestEventBinding =
                        VexLifeMemoryContract.CurrentRequestBinding(
                            eventRef =
                                "event.vexlife.request.owner-test.context",
                            eventHash =
                                "b".repeat(
                                    64,
                                ),
                            sequence =
                                2L,
                        ),
                    claimedAt =
                        Instant.parse(
                            "2026-11-06T10:31:00Z",
                        ),
                )
                    .claim

            val first =
                owner.issueSuccessorContext(
                    lineageRef = working.lineageRef,
                    threadRef = working.threadRef,
                    currentRequestEventBinding =
                        claim.currentRequestEventBinding,
                    successorClaimRef =
                        claim.successorClaimRef,
                    successorClaimFingerprint =
                        claim.semanticFingerprint,
                    issuedAt =
                        Instant.parse(
                            "2026-11-06T10:32:00Z",
                        ),
                )

            assertEquals(
                "ISSUED",
                first.state,
            )
            assertTrue(
                first.effectPerformedNow,
            )
            assertEquals(
                "WORKING_CHECKPOINT_SUCCESSOR",
                first.context.contextMode,
            )
            assertEquals(
                committed.current.checkpoint.checkpointRef,
                first.context.checkpointRef,
            )
            assertEquals(
                committed.current.commitReceipt.commitReceiptRef,
                first.context.commitReceiptRef,
            )
            assertEquals(
                claim.successorClaimRef,
                first.context.successorClaimRef,
            )
            assertEquals(
                claim.currentRequestEventBinding,
                first.context.currentRequestEventBinding,
            )
            assertEquals(
                committed.current.checkpoint.workingSet,
                first.context.workingSet,
            )
            assertTrue(
                first.context.sourceRefs.contains(
                    committed.current.checkpoint.checkpointRef,
                ),
            )
            assertTrue(
                first.context.sourceRefs.contains(
                    committed.current.commitReceipt.commitReceiptRef,
                ),
            )
            assertTrue(
                first.context.sourceRefs.contains(
                    claim.successorClaimRef,
                ),
            )
            assertTrue(
                first.context.effects.successorContextIssued,
            )
            assertFalse(
                first.context.effects.successorClaimMutated,
            )
            assertFalse(
                first.context.effects.memoryPromotionPerformed,
            )
            assertFalse(
                first.context.effects.providerCalled,
            )
            assertFalse(
                first.context.effects.modelCalled,
            )

            val second =
                owner.issueSuccessorContext(
                    lineageRef = working.lineageRef,
                    threadRef = working.threadRef,
                    currentRequestEventBinding =
                        claim.currentRequestEventBinding,
                    successorClaimRef =
                        claim.successorClaimRef,
                    successorClaimFingerprint =
                        claim.semanticFingerprint,
                    issuedAt =
                        Instant.parse(
                            "2026-11-06T10:33:00Z",
                        ),
                )

            assertEquals(
                "IDEMPOTENT_CURRENT",
                second.state,
            )
            assertFalse(
                second.effectPerformedNow,
            )
            assertEquals(
                first.context,
                second.context,
            )
            assertEquals(
                first.context,
                assertNotNull(
                    owner.readSuccessorContext(
                        lineageRef = working.lineageRef,
                        threadRef = working.threadRef,
                        currentRequestEventBinding =
                            claim.currentRequestEventBinding,
                    ),
                ),
            )
        } finally {
            home
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun wrongClaimIdentityCannotIssueSuccessorContext() {
        val home =
            Files.createTempDirectory(
                "vexlife-working-owner-context-claim-mismatch-",
            )
                .toAbsolutePath()

        try {
            val owner =
                VexLifeWorkingContinuityOwner(
                    home,
                )
            val working =
                workingSet()
            val (
                input,
                plan,
            ) =
                rolloverPlan(
                    working,
                )
            owner.commitPlan(
                plan = plan,
                pressureInput = input,
                workingSet = working,
            )
            val claim =
                owner.claimSuccessorContext(
                    lineageRef = working.lineageRef,
                    threadRef = working.threadRef,
                    priorConversationHeadSha256 =
                        "a".repeat(
                            64,
                        ),
                    priorRequestEventRef =
                        working.cursorEventRef,
                    currentRequestEventBinding =
                        VexLifeMemoryContract.CurrentRequestBinding(
                            eventRef =
                                "event.vexlife.request.owner-test.context-mismatch",
                            eventHash =
                                "b".repeat(
                                    64,
                                ),
                            sequence =
                                2L,
                        ),
                )
                    .claim

            assertFailsWith<IllegalArgumentException> {
                owner.issueSuccessorContext(
                    lineageRef = working.lineageRef,
                    threadRef = working.threadRef,
                    currentRequestEventBinding =
                        claim.currentRequestEventBinding,
                    successorClaimRef =
                        claim.successorClaimRef,
                    successorClaimFingerprint =
                        "0".repeat(
                            64,
                        ),
                )
            }

            assertNull(
                owner.readSuccessorContext(
                    lineageRef = working.lineageRef,
                    threadRef = working.threadRef,
                    currentRequestEventBinding =
                        claim.currentRequestEventBinding,
                ),
            )
        } finally {
            home
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun supersedingCurrentCheckpointMakesPriorIssuedSuccessorContextNonCurrent() {
        val home =
            Files.createTempDirectory(
                "vexlife-working-owner-context-superseded-",
            )
                .toAbsolutePath()

        try {
            val owner =
                VexLifeWorkingContinuityOwner(
                    home,
                )
            val firstWorking =
                workingSet()
            val (
                firstInput,
                firstPlan,
            ) =
                rolloverPlan(
                    firstWorking,
                )
            val firstCommitted =
                owner.commitPlan(
                    plan = firstPlan,
                    pressureInput = firstInput,
                    workingSet = firstWorking,
                )
            val claim =
                owner.claimSuccessorContext(
                    lineageRef = firstWorking.lineageRef,
                    threadRef = firstWorking.threadRef,
                    priorConversationHeadSha256 =
                        "a".repeat(
                            64,
                        ),
                    priorRequestEventRef =
                        firstWorking.cursorEventRef,
                    currentRequestEventBinding =
                        VexLifeMemoryContract.CurrentRequestBinding(
                            eventRef =
                                "event.vexlife.request.owner-test.context-superseded",
                            eventHash =
                                "b".repeat(
                                    64,
                                ),
                            sequence =
                                2L,
                        ),
                )
                    .claim
            val issued =
                owner.issueSuccessorContext(
                    lineageRef = firstWorking.lineageRef,
                    threadRef = firstWorking.threadRef,
                    currentRequestEventBinding =
                        claim.currentRequestEventBinding,
                    successorClaimRef =
                        claim.successorClaimRef,
                    successorClaimFingerprint =
                        claim.semanticFingerprint,
                )

            assertNotNull(
                owner.readSuccessorContext(
                    lineageRef = firstWorking.lineageRef,
                    threadRef = firstWorking.threadRef,
                    currentRequestEventBinding =
                        claim.currentRequestEventBinding,
                ),
            )

            val secondWorking =
                workingSet(
                    cursor =
                        "event.vexlife.request.owner-test.context-next",
                    statement =
                        "statement.vexlife.owner-test.context-next",
                )
            val (
                secondInput,
                secondPlan,
            ) =
                checkpointPlan(
                    workingSet = secondWorking,
                    prior =
                        firstCommitted.current.checkpoint,
                )
            owner.commitPlan(
                plan = secondPlan,
                pressureInput = secondInput,
                workingSet = secondWorking,
            )

            assertNull(
                owner.readSuccessorContext(
                    lineageRef = firstWorking.lineageRef,
                    threadRef = firstWorking.threadRef,
                    currentRequestEventBinding =
                        claim.currentRequestEventBinding,
                ),
            )

            val contextFiles =
                Files.walk(
                    home.resolve(
                        "working-continuity",
                    ),
                )
                    .use {
                        paths ->
                        paths
                            .filter {
                                path ->
                                path
                                    .toString()
                                    .contains(
                                        "successor-contexts",
                                    ) &&
                                    path.fileName
                                        .toString()
                                        .endsWith(
                                            ".json",
                                        )
                            }
                            .count()
                    }
            assertEquals(
                1L,
                contextFiles,
            )
            assertTrue(
                issued.context.effects.successorContextIssued,
            )
        } finally {
            home
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun differentRequestsCanUseTheSameRolloverCheckpointWithoutStealingEachOther() {
        val home =
            Files.createTempDirectory(
                "vexlife-working-owner-request-scoped-successors-",
            )
                .toAbsolutePath()

        try {
            val owner =
                VexLifeWorkingContinuityOwner(
                    home,
                )
            val working =
                workingSet()
            val (
                input,
                plan,
            ) =
                rolloverPlan(
                    working,
                )
            owner.commitPlan(
                plan = plan,
                pressureInput = input,
                workingSet = working,
            )

            val bindingA =
                VexLifeMemoryContract.CurrentRequestBinding(
                    eventRef =
                        "event.vexlife.request.owner-test.successor-a",
                    eventHash =
                        "b".repeat(
                            64,
                        ),
                    sequence =
                        2L,
                )
            val bindingB =
                VexLifeMemoryContract.CurrentRequestBinding(
                    eventRef =
                        "event.vexlife.request.owner-test.successor-b",
                    eventHash =
                        "c".repeat(
                            64,
                        ),
                    sequence =
                        2L,
                )

            val claimA =
                owner.claimSuccessorContext(
                    lineageRef = working.lineageRef,
                    threadRef = working.threadRef,
                    priorConversationHeadSha256 =
                        "a".repeat(
                            64,
                        ),
                    priorRequestEventRef =
                        working.cursorEventRef,
                    currentRequestEventBinding =
                        bindingA,
                )
                    .claim
            val contextA =
                owner.issueSuccessorContext(
                    lineageRef = working.lineageRef,
                    threadRef = working.threadRef,
                    currentRequestEventBinding =
                        bindingA,
                    successorClaimRef =
                        claimA.successorClaimRef,
                    successorClaimFingerprint =
                        claimA.semanticFingerprint,
                )
                    .context

            val claimB =
                owner.claimSuccessorContext(
                    lineageRef = working.lineageRef,
                    threadRef = working.threadRef,
                    priorConversationHeadSha256 =
                        "a".repeat(
                            64,
                        ),
                    priorRequestEventRef =
                        working.cursorEventRef,
                    currentRequestEventBinding =
                        bindingB,
                )
                    .claim
            val contextB =
                owner.issueSuccessorContext(
                    lineageRef = working.lineageRef,
                    threadRef = working.threadRef,
                    currentRequestEventBinding =
                        bindingB,
                    successorClaimRef =
                        claimB.successorClaimRef,
                    successorClaimFingerprint =
                        claimB.semanticFingerprint,
                )
                    .context

            assertTrue(
                claimA.successorClaimRef !=
                    claimB.successorClaimRef,
            )
            assertTrue(
                contextA.successorContextRef !=
                    contextB.successorContextRef,
            )
            assertEquals(
                claimA,
                owner.readSuccessorClaim(
                    lineageRef = working.lineageRef,
                    threadRef = working.threadRef,
                    currentRequestEventBinding =
                        bindingA,
                ),
            )
            assertEquals(
                claimB,
                owner.readSuccessorClaim(
                    lineageRef = working.lineageRef,
                    threadRef = working.threadRef,
                    currentRequestEventBinding =
                        bindingB,
                ),
            )
            assertEquals(
                contextA,
                owner.readSuccessorContext(
                    lineageRef = working.lineageRef,
                    threadRef = working.threadRef,
                    currentRequestEventBinding =
                        bindingA,
                ),
            )
            assertEquals(
                contextB,
                owner.readSuccessorContext(
                    lineageRef = working.lineageRef,
                    threadRef = working.threadRef,
                    currentRequestEventBinding =
                        bindingB,
                ),
            )
        } finally {
            home
                .toFile()
                .deleteRecursively()
        }
    }
}

// [VXG RealForever]
