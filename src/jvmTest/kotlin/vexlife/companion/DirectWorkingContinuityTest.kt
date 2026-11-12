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
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.memory.VexLifeMemoryContextFixture

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
