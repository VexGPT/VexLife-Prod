package vexlife.memory

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vexlife.conversation.DirectConversationIdentity
import vexlife.contract.VexContractJson

class VexLifeMemoryContractTest {
    private val deviceRef =
        "device.vexlife.00000000-0000-0000-0000-000000000001"
    private val lineageRef =
        "companion-lineage.vexlife.00000000-0000-0000-0000-000000000002"
    private val homeRef =
        "vex-home.$deviceRef"

    private fun page(): VexLifeMemoryContract.Page =
        VexLifeMemoryContract.Page(
            pageRef = "page.living-journal.memory.001",
            statementRef = "statement.score.continuity.001",
            summary = "Victor prefers exact source-grounded continuation.",
            summaryHash = "a".repeat(
                64,
            ),
            memoryRelation = "CURRENT_CONTINUITY",
            recordedStatementState = "ACCEPTED",
            effectiveState = "CURRENT",
            consentState = "PERMITTED",
            semanticAcceptanceRef = "acceptance.score.001",
            semanticAcceptanceSha256 = "b".repeat(
                64,
            ),
            semanticAuthorityHeadSha256 = "c".repeat(
                64,
            ),
        )

    private fun currentMemory(): VexLifeMemoryContract.Context =
        VexLifeMemoryContract.Context(
            schemaVersion = VexLifeMemoryContract.PacketSchema,
            truthClass = VexLifeMemoryContract.PacketTruthClass,
            projectionState = "CURRENT",
            projectionCurrentness = "CURRENT",
            sourceOwnerRefs =
                listOf(
                    "github.issue.vextreme-sdk.225",
                    "github.issue.vextreme-sdk.416",
                ),
            currentDailyStratumRef = "daily-memory-stratum.vexlife.001",
            currentDailyStratumSha256 = "d".repeat(
                64,
            ),
            sourceConversationHeadSha256 = "e".repeat(
                64,
            ),
            sourceScoreHeadSha256 = "f".repeat(
                64,
            ),
            sourceSemanticAuthorityHeadSha256 = "1".repeat(
                64,
            ),
            pages =
                listOf(
                    page(),
                ),
            heldOrDeferredStatementRefs = emptyList(),
            boundedOutStatementRefs = emptyList(),
            tokenBoundedOutPageRefs = emptyList(),
            reasons = emptyList(),
            memoryTokenCap = VexLifeMemoryContract.MaximumMemoryTokens,
            systemMessageSha256 = "2".repeat(
                64,
            ),
            estimatedSystemMessageTokens = 120,
            memoryContextRef = "memory-context.vexlife.prompt.001",
            semanticFingerprint = "3".repeat(
                64,
            ),
            sourceRefs =
                listOf(
                    "acceptance.score.001",
                    "daily-memory-stratum.vexlife.001",
                    "github.issue.vextreme-sdk.225",
                    "github.issue.vextreme-sdk.416",
                    "page.living-journal.memory.001",
                    "statement.score.continuity.001",
                ),
            included = true,
        )

    @Test
    fun currentMemoryBindsConversationScoreDailyAndAcceptanceSources() {
        val current =
            VexLifeMemoryContract.validateContext(
                currentMemory(),
            )

        assertTrue(
            current.included,
        )
        assertEquals(
            "e".repeat(
                64,
            ),
            current.sourceConversationHeadSha256,
        )
        assertEquals(
            "f".repeat(
                64,
            ),
            current.sourceScoreHeadSha256,
        )
        assertTrue(
            current.sourceRefs.contains(
                page().semanticAcceptanceRef,
            ),
        )

        val encoded =
            VexContractJson.canonicalEncode(
                current,
            )
        assertFalse(
            encoded.contains(
                "rawConversationContent",
            ),
        )
        assertFalse(
            encoded.contains(
                "hiddenReasoning",
            ),
        )
    }

    @Test
    fun heldMemoryCannotClaimProviderContent() {
        val held =
            currentMemory()
                .copy(
                    projectionState = "HELD",
                    projectionCurrentness = "HELD",
                    currentDailyStratumRef = null,
                    currentDailyStratumSha256 = null,
                    sourceConversationHeadSha256 = null,
                    sourceScoreHeadSha256 = null,
                    sourceSemanticAuthorityHeadSha256 = null,
                    pages = emptyList(),
                    reasons =
                        listOf(
                            "NO_COMMITTED_DAILY_MEMORY",
                        ),
                    systemMessageSha256 = null,
                    estimatedSystemMessageTokens = 0,
                    sourceRefs =
                        listOf(
                            "github.issue.vextreme-sdk.225",
                            "github.issue.vextreme-sdk.416",
                        ),
                    included = false,
                )

        assertFalse(
            VexLifeMemoryContract.validateContext(
                held,
            )
                .included,
        )
    }

    @Test
    fun promptSelectionCarriesMemoryAndExactConversationBindingsTogether() {
        val memory =
            VexLifeMemoryContract.validateContext(
                currentMemory(),
            )
        val selection =
            VexLifeMemoryContract.validateSelection(
                VexLifeMemoryContract.Selection(
                    schemaVersion = VexLifeMemoryContract.SelectionSchema,
                    contextMode = "IMMEDIATE_PRIOR_COMPLETED_TURN",
                    homeRef = homeRef,
                    deviceRef = deviceRef,
                    lineageRef = lineageRef,
                    threadRef = DirectConversationIdentity.ThreadRef,
                    priorConversationHeadSha256 = "4".repeat(
                        64,
                    ),
                    priorHeadSequence = 1,
                    priorInputTokenEstimate = 200,
                    currentRequestInputTokenEstimate = 20,
                    currentRequestEventBinding =
                        VexLifeMemoryContract.CurrentRequestBinding(
                            eventRef = "event.vexlife.request.current",
                            eventHash = "5".repeat(
                                64,
                            ),
                            sequence = 2,
                        ),
                    memoryContextRef = memory.memoryContextRef,
                    memoryContextFingerprint = memory.semanticFingerprint,
                    memorySourceRefs = memory.sourceRefs,
                    memoryIncluded = true,
                    workingCheckpointRefOrNull = null,
                    workingCheckpointFingerprintOrNull = null,
                    workingCommitReceiptRefOrNull = null,
                    workingCommitReceiptFingerprintOrNull = null,
                    successorClaimRefOrNull = null,
                    successorClaimFingerprintOrNull = null,
                    workingSetOrNull = null,
                    workingSourceRefs = emptyList(),
                    selectedEventBindings =
                        listOf(
                            VexLifeMemoryContract.SelectedEventBinding(
                                eventRef = "event.vexlife.request.prior",
                                eventHash = "6".repeat(
                                    64,
                                ),
                                sequence = 0,
                                completedHeadSha256 = "4".repeat(
                                    64,
                                ),
                            ),
                            VexLifeMemoryContract.SelectedEventBinding(
                                eventRef = "event.vexlife.response.prior",
                                eventHash = "7".repeat(
                                    64,
                                ),
                                sequence = 1,
                                completedHeadSha256 = "4".repeat(
                                    64,
                                ),
                            ),
                        ),
                    wholeHistoryEventEnumerationPerformed = false,
                    memorySelectionPerformed = true,
                    trainingSelectionPerformed = false,
                ),
            )

        assertTrue(
            selection.memoryIncluded,
        )
        assertTrue(
            selection.memorySelectionPerformed,
        )
        assertEquals(
            2,
            selection.selectedEventBindings.size,
        )
        assertFalse(
            selection.wholeHistoryEventEnumerationPerformed,
        )
    }

    @Test
    fun productNamespacedLineageCannotOwnMemorySelection() {
        assertFailsWith<IllegalArgumentException> {
            val memory =
                currentMemory()
            VexLifeMemoryContract.validateSelection(
                VexLifeMemoryContract.Selection(
                    schemaVersion = VexLifeMemoryContract.SelectionSchema,
                    contextMode = "IMMEDIATE_PRIOR_COMPLETED_TURN",
                    homeRef = "vex-home.prod.001",
                    deviceRef = "device.vexlife-prod.001",
                    lineageRef = "companion-lineage.vexlife-prod.001",
                    threadRef = DirectConversationIdentity.ThreadRef,
                    priorConversationHeadSha256 = "8".repeat(
                        64,
                    ),
                    priorHeadSequence = 1,
                    priorInputTokenEstimate = 10,
                    currentRequestInputTokenEstimate = 10,
                    currentRequestEventBinding =
                        VexLifeMemoryContract.CurrentRequestBinding(
                            eventRef = "event.vexlife.request.current",
                            eventHash = "9".repeat(
                                64,
                            ),
                            sequence = 2,
                        ),
                    memoryContextRef = memory.memoryContextRef,
                    memoryContextFingerprint = memory.semanticFingerprint,
                    memorySourceRefs = memory.sourceRefs,
                    memoryIncluded = true,
                    workingCheckpointRefOrNull = null,
                    workingCheckpointFingerprintOrNull = null,
                    workingCommitReceiptRefOrNull = null,
                    workingCommitReceiptFingerprintOrNull = null,
                    successorClaimRefOrNull = null,
                    successorClaimFingerprintOrNull = null,
                    workingSetOrNull = null,
                    workingSourceRefs = emptyList(),
                    selectedEventBindings = emptyList(),
                    wholeHistoryEventEnumerationPerformed = false,
                    memorySelectionPerformed = true,
                    trainingSelectionPerformed = false,
                ),
            )
        }
    }
}

// [VXG RealForever]
