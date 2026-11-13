package vexlife.companion

import java.nio.file.Files
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vexlife.conversation.CanonicalLivedContextFixture
import vexlife.conversation.DirectConversationStore
import vexlife.continuity.VexLifeWorkingContinuity
import vexlife.continuity.VexLifeWorkingContinuityOwner
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.memory.VexLifeMemoryContextFixture
import vexlife.memory.VexLifeMemoryContract

class DirectWorkingContinuityTest {
    private fun productHome(): java.nio.file.Path {
        val parent =
            Files.createTempDirectory(
                "vexlife-direct-working-continuity-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )
        CanonicalVexLifeHomeFixture.form(
            home = home,
            deviceSuffix = "working-device",
            lineageSuffix = "working-lineage",
        )
        return home
    }

    @Test
    fun absentReceiptReadIsEffectFree() {
        val home =
            productHome()

        try {
            assertNull(
                DirectWorkingContinuity.read(
                    productHome = home,
                    turnRef =
                        "turn.vexlife.working.absent",
                ),
            )
            assertFalse(
                Files.exists(
                    home.resolve(
                        "runtime/vexlife-prod/turn-working-continuity",
                    ),
                ),
            )
        } finally {
            home.parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun producerRejectsMemoryThatDoesNotMatchThePromptReceiptSelection() {
        val home =
            productHome()

        try {
            val store =
                DirectConversationStore(
                    home,
                )
            val memory =
                VexLifeMemoryContextFixture.current()

            store.withWriterLease {
                val request =
                    beginRequest(
                        turnRef =
                            "turn.vexlife.working.memory-mismatch",
                        requestMessageRef =
                            "message.vexlife.working.memory-mismatch",
                        content =
                            "Memory binding request",
                    )
                val prompt =
                    DirectPromptContext.materialize(
                        prior = null,
                        currentRequest = request,
                        memoryContext = memory,
                    )
                val otherMemory =
                    memory.copy(
                        memoryContextRef =
                            "memory-context.vexlife.prompt.other",
                    )

                assertFailsWith<IllegalArgumentException> {
                    DirectWorkingContinuity.prepare(
                        productHome = home,
                        currentRequest = request,
                        promptContext = prompt.receipt,
                        memoryContext = otherMemory,
                    )
                }
            }
        } finally {
            home.parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun sourceReturnSuccessorPromptPreservesTheSameWorkingContinuityCustody() {
        val home =
            productHome()

        try {
            val store =
                DirectConversationStore(
                    home,
                )
            val memory =
                VexLifeMemoryContextFixture.current()

            store.withWriterLease {
                val request =
                    beginRequest(
                        turnRef =
                            "turn.vexlife.working.source-return-successor",
                        requestMessageRef =
                            "message.vexlife.working.source-return-successor",
                        content =
                            "Source-return successor continuity request",
                    )
                val basePrompt =
                    DirectPromptContext.materialize(
                        prior = null,
                        currentRequest = request,
                        memoryContext = memory,
                    )
                val workingSet =
                    VexLifeWorkingContinuity.WorkingSet(
                        lineageRef =
                            request.companionLineageRef,
                        threadRef =
                            request.threadRef,
                        cursorEventRef =
                            request.eventRef,
                        activeWorkNodeRefs =
                            emptyList(),
                        currentIntentReceiptRefs =
                            emptyList(),
                        openLoopRefs =
                            listOf(
                                "open-loop.score.stale",
                            ),
                        currentStatementRefs =
                            listOf(
                                "statement.score.stale",
                            ),
                        currentContinuityRecordRefs =
                            emptyList(),
                        currentDailyStratumRefOrNull =
                            null,
                        sourceRefs =
                            emptyList(),
                    )
                val context =
                    VexLifeWorkingContinuityOwner.SuccessorContext(
                        schemaVersion =
                            VexLifeWorkingContinuityOwner.SuccessorContextSchema,
                        truthClass =
                            "NON_SEMANTIC_WORKING_CONTINUITY_SUCCESSOR_CONTEXT",
                        effectOwnerRef =
                            VexLifeWorkingContinuityOwner.EffectOwnerRef,
                        contextMode =
                            "WORKING_CHECKPOINT_SUCCESSOR",
                        lineageRef =
                            request.companionLineageRef,
                        threadRef =
                            request.threadRef,
                        checkpointRef =
                            "checkpoint.vexlife.test.source-return-successor",
                        checkpointFingerprint =
                            "a".repeat(
                                64,
                            ),
                        commitReceiptRef =
                            "receipt.vexlife.test.source-return-successor",
                        commitReceiptFingerprint =
                            "b".repeat(
                                64,
                            ),
                        successorClaimRef =
                            "claim.vexlife.test.source-return-successor",
                        successorClaimFingerprint =
                            "c".repeat(
                                64,
                            ),
                        priorConversationHeadSha256 =
                            "d".repeat(
                                64,
                            ),
                        priorRequestEventRef =
                            "event.vexlife.test.prior-request",
                        currentRequestEventBinding =
                            VexLifeMemoryContract.CurrentRequestBinding(
                                eventRef =
                                    request.eventRef,
                                eventHash =
                                    request.eventHash,
                                sequence =
                                    request.sequence,
                            ),
                        workingSet = workingSet,
                        sourceRefs =
                            listOf(
                                "source.vexlife.test.source-return-successor",
                            ),
                        issuedAt =
                            "2026-11-12T00:00:00Z",
                        effects =
                            VexLifeWorkingContinuityOwner.SuccessorContextEffects(
                                homeMutated = true,
                                successorContextIssued = true,
                            ),
                        successorContextRef =
                            "successor-context.vexlife.test.source-return-successor",
                        semanticFingerprint =
                            "e".repeat(
                                64,
                            ),
                    )
                val sourceReturnPrompt =
                    basePrompt.receipt.copy(
                        contextMode =
                            "SOURCE_RETURN_WORKING_CHECKPOINT_SUCCESSOR",
                        workingContinuityIncluded =
                            true,
                        workingContinuityContextRef =
                            context.successorContextRef,
                        workingContinuityContextFingerprint =
                            context.semanticFingerprint,
                        workingContinuityCheckpointRef =
                            context.checkpointRef,
                        workingContinuitySuccessorClaimRef =
                            context.successorClaimRef,
                        workingContinuitySourceRefs =
                            context.sourceRefs,
                        workingContinuitySystemMessageSha256 =
                            "f".repeat(
                                64,
                            ),
                        workingContinuityEstimatedSystemMessageTokens =
                            1,
                        sourceReturnState =
                            DirectConversationSourceReturn.NoMatch,
                        sourceReturnSystemMessageSha256 =
                            "1".repeat(
                                64,
                            ),
                        sourceReturnEstimatedSystemMessageTokens =
                            1,
                    )

                val prepared =
                    DirectWorkingContinuity.prepare(
                        productHome = home,
                        currentRequest = request,
                        promptContext = sourceReturnPrompt,
                        memoryContext = memory,
                        priorSuccessorContext =
                            DirectWorkingContinuity.CurrentSuccessor(
                                context,
                            ),
                    )

                assertEquals(
                    sourceReturnPrompt.receiptRef,
                    prepared.pressureInput.promptReceiptRef,
                )
                assertEquals(
                    request.eventRef,
                    prepared.workingSet.cursorEventRef,
                )
                assertTrue(
                    prepared.workingSet.openLoopRefs.isEmpty(),
                )
                assertTrue(
                    prepared.workingSet.currentStatementRefs.isEmpty(),
                )
            }
        } finally {
            home.parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun boundedRecentSelectionMayReachGenesisWithoutInvalidatingWorkingContinuity() {
        val home =
            productHome()

        try {
            val store =
                DirectConversationStore(
                    home,
                )
            val memory =
                VexLifeMemoryContextFixture.current()

            store.withWriterLease {
                val firstRequest =
                    beginRequest(
                        turnRef =
                            "turn.vexlife.working.genesis.001",
                        requestMessageRef =
                            "message.vexlife.working.genesis.human.001",
                        content =
                            "First short request.",
                    )
                val firstResponse =
                    appendResponse(
                        request = firstRequest,
                        responseMessageRef =
                            "message.vexlife.working.genesis.companion.001",
                        content =
                            "First short response.",
                        model = "default_model",
                        endpointOrigin = "http://127.0.0.1:8765",
                        runtimeObservationRef =
                            "observation.vexlife.working.genesis.001",
                        invocationEvidenceRef =
                            "evidence.vexlife.working.genesis.001",
                    )
                val firstContext =
                    CanonicalLivedContextFixture.write(
                        productHome = home,
                        request = firstRequest,
                        response = firstResponse,
                        formedAt =
                            Instant.parse(
                                "2026-11-12T00:00:15Z",
                            ),
                    )
                commitCompletedTurn(
                    request = firstRequest,
                    response = firstResponse,
                    contextSha256 = firstContext.sha256,
                )

                val prior =
                    requireNotNull(
                        immediatePriorCompletedTurn(),
                    )
                val secondRequest =
                    beginRequest(
                        turnRef =
                            "turn.vexlife.working.genesis.002",
                        requestMessageRef =
                            "message.vexlife.working.genesis.human.002",
                        content =
                            "Second short request.",
                    )
                val prompt =
                    DirectPromptContext.materialize(
                        prior = prior,
                        currentRequest = secondRequest,
                        recentConversationSource = {
                            availableTokens ->
                            recentCompletedConversation(
                                availableTokens = availableTokens,
                                estimatedMessageTokens =
                                    DirectPromptContext::estimatedMessageTokens,
                            )
                        },
                        memoryContext = memory,
                    )

                assertTrue(
                    prompt.receipt.wholeHistoryEventEnumerationPerformed,
                )

                val prepared =
                    DirectWorkingContinuity.prepare(
                        productHome = home,
                        currentRequest = secondRequest,
                        promptContext = prompt.receipt,
                        memoryContext = memory,
                    )

                assertEquals(
                    prompt.receipt.receiptRef,
                    prepared.pressureInput.promptReceiptRef,
                )
                assertTrue(
                    prepared.workingSet.currentStatementRefs.isEmpty(),
                )
                assertTrue(
                    prepared.workingSet.openLoopRefs.isEmpty(),
                )
            }
        } finally {
            home.parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun orphanedScoreMaterialHoldsWorkingContinuityWithoutRejectingPreparation() {
        val home =
            productHome()

        try {
            val store =
                DirectConversationStore(
                    home,
                )
            val memory =
                VexLifeMemoryContextFixture.current()

            store.withWriterLease {
                val request =
                    beginRequest(
                        turnRef =
                            "turn.vexlife.working.score-held",
                        requestMessageRef =
                            "message.vexlife.working.score-held",
                        content =
                            "Score continuity remains stewardship.",
                    )
                val prompt =
                    DirectPromptContext.materialize(
                        prior = null,
                        currentRequest = request,
                        memoryContext = memory,
                    )
                val eventsRoot =
                    home.resolve(
                        "score/" +
                            request.companionLineageRef +
                            "/" +
                            request.threadRef +
                            "/events",
                    )
                Files.createDirectories(
                    eventsRoot,
                )
                Files.writeString(
                    eventsRoot.resolve(
                        "orphan.json",
                    ),
                    "{}\n",
                )

                val prepared =
                    DirectWorkingContinuity.prepare(
                        productHome = home,
                        currentRequest = request,
                        promptContext = prompt.receipt,
                        memoryContext = memory,
                    )

                assertNull(
                    prepared.plan,
                )
                assertNull(
                    prepared.scoreCoordinates,
                )
                assertEquals(
                    "IllegalArgumentException",
                    prepared.formationFailureClassOrNull,
                )
                assertTrue(
                    prepared.workingSet.currentStatementRefs.isEmpty(),
                )
                assertTrue(
                    prepared.workingSet.openLoopRefs.isEmpty(),
                )

                val receipt =
                    DirectWorkingContinuity.commitAfterCompletedTurn(
                        productHome = home,
                        turnRef = request.turnRef,
                        prepared = prepared,
                        formedAt =
                            Instant.parse(
                                "2026-11-13T09:20:00Z",
                            ),
                    )

                assertEquals(
                    "HELD",
                    receipt.disposition,
                )
                assertFalse(
                    receipt.effects.workingCheckpointPersisted,
                )
                assertFalse(
                    receipt.effects.workingHeadMutated,
                )
            }
        } finally {
            home.parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun smallPromptFormsAndPersistsNoCheckpointRequiredPostCommitTruth() {
        val home =
            productHome()

        try {
            val store =
                DirectConversationStore(
                    home,
                )
            val memory =
                VexLifeMemoryContextFixture.current()

            store.withWriterLease {
                val request =
                    beginRequest(
                        turnRef =
                            "turn.vexlife.working.small",
                        requestMessageRef =
                            "message.vexlife.working.small",
                        content =
                            "Small continuity request",
                        formedAt =
                            Instant.parse(
                                "2026-11-06T11:00:00Z",
                            ),
                    )
                val prompt =
                    DirectPromptContext.materialize(
                        prior = null,
                        currentRequest = request,
                        memoryContext = memory,
                    )
                val prepared =
                    DirectWorkingContinuity.prepare(
                        productHome = home,
                        currentRequest = request,
                        promptContext = prompt.receipt,
                        memoryContext = memory,
                    )
                val receipt =
                    DirectWorkingContinuity.commitAfterCompletedTurn(
                        productHome = home,
                        turnRef = request.turnRef,
                        prepared = prepared,
                        formedAt =
                            Instant.parse(
                                "2026-11-06T11:00:01Z",
                            ),
                    )

                assertEquals(
                    "NO_CHECKPOINT_REQUIRED",
                    receipt.disposition,
                )
                assertEquals(
                    "NoCheckpointRequired",
                    receipt.planStateOrNull,
                )
                assertTrue(
                    receipt.effects.postCommitReceiptPersisted,
                )
                assertFalse(
                    receipt.effects.workingCheckpointPersisted,
                )
                assertFalse(
                    receipt.effects.workingHeadMutated,
                )
                assertEquals(
                    receipt,
                    DirectWorkingContinuity.read(
                        productHome = home,
                        turnRef = request.turnRef,
                    ),
                )
            }
        } finally {
            home.parent
                .toFile()
                .deleteRecursively()
        }
    }
}

// [VXG RealForever]
