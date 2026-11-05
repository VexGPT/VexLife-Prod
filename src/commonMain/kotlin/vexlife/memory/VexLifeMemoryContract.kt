package vexlife.memory

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import vexlife.continuity.VexLifeContinuity

/**
 * Exact portable contract for the current attributed Memory packet consumed by VexLife prompt
 * materialization.
 *
 * This is not raw Conversation replay and it is not subjective-memory self-certification. It binds
 * accepted Score statements through the committed daily stratum and Living Journal projection to one
 * bounded source-attributed provider context.
 */
object VexLifeMemoryContract {
    const val LivingJournalProjectionSchema =
        VexLifeContinuity.LivingJournalMemoryProjectionSchema

    const val PacketSchema =
        VexLifeContinuity.PromptMemoryPacketSchema

    const val SelectionSchema =
        VexLifeContinuity.PromptSelectionSchema

    const val PacketTruthClass =
        "CURRENT_ATTRIBUTED_MEMORY_CONTEXT"

    const val MaximumPages =
        100

    const val MaximumMemoryTokens =
        100_000

    const val ContextInputTokenBudget =
        192_000

    const val ReservedOutputTokens =
        16_000

    const val HardTokenLimit =
        208_000

    @Serializable
    data class Page(
        val pageRef: String,
        val statementRef: String,
        val summary: String,
        val summaryHash: String,
        val memoryRelation: String,
        val recordedStatementState: String,
        val effectiveState: String,
        val consentState: String,
        val semanticAcceptanceRef: String,
        val semanticAcceptanceSha256: String,
        val semanticAuthorityHeadSha256: String,
    )

    @Serializable
    data class PacketCore(
        val schemaVersion: String,
        val truthClass: String,
        val projectionState: String,
        val projectionCurrentness: String,
        val sourceOwnerRefs: List<String>,
        val currentDailyStratumRef: String?,
        val currentDailyStratumSha256: String?,
        val sourceConversationHeadSha256: String?,
        val sourceScoreHeadSha256: String?,
        val sourceSemanticAuthorityHeadSha256: String?,
        val pages: List<Page>,
        val heldOrDeferredStatementRefs: List<String>,
        val boundedOutStatementRefs: List<String>,
        val tokenBoundedOutPageRefs: List<String>,
        val reasons: List<String>,
        val memoryTokenCap: Int,
        val systemMessageSha256: String?,
        val estimatedSystemMessageTokens: Int,
    )

    @Serializable
    data class Context(
        val schemaVersion: String,
        val truthClass: String,
        val projectionState: String,
        val projectionCurrentness: String,
        val sourceOwnerRefs: List<String>,
        val currentDailyStratumRef: String?,
        val currentDailyStratumSha256: String?,
        val sourceConversationHeadSha256: String?,
        val sourceScoreHeadSha256: String?,
        val sourceSemanticAuthorityHeadSha256: String?,
        val pages: List<Page>,
        val heldOrDeferredStatementRefs: List<String>,
        val boundedOutStatementRefs: List<String>,
        val tokenBoundedOutPageRefs: List<String>,
        val reasons: List<String>,
        val memoryTokenCap: Int,
        val systemMessageSha256: String?,
        val estimatedSystemMessageTokens: Int,
        val memoryContextRef: String,
        val semanticFingerprint: String,
        val sourceRefs: List<String>,
        val included: Boolean,
    )

    @Serializable
    data class CurrentRequestBinding(
        val eventRef: String,
        val eventHash: String,
        val sequence: Long,
    )

    @Serializable
    data class SelectedEventBinding(
        val eventRef: String,
        val eventHash: String,
        val sequence: Long,
        val completedHeadSha256: String,
    )

    @Serializable
    data class Selection(
        val schemaVersion: String,
        val contextMode: String,
        val homeRef: String,
        val deviceRef: String,
        val lineageRef: String,
        val threadRef: String,
        val priorConversationHeadSha256: String,
        val priorHeadSequence: Long,
        val priorInputTokenEstimate: Int,
        val currentRequestInputTokenEstimate: Int,
        val currentRequestEventBinding: CurrentRequestBinding,
        val memoryContextRef: String,
        val memoryContextFingerprint: String,
        val memorySourceRefs: List<String>,
        val memoryIncluded: Boolean,
        val workingCheckpointRefOrNull: String?,
        val workingCheckpointFingerprintOrNull: String?,
        val workingCommitReceiptRefOrNull: String?,
        val workingCommitReceiptFingerprintOrNull: String?,
        val successorClaimRefOrNull: String?,
        val successorClaimFingerprintOrNull: String?,
        val workingSetOrNull: JsonObject?,
        val workingSourceRefs: List<String>,
        val selectedEventBindings: List<SelectedEventBinding>,
        val wholeHistoryEventEnumerationPerformed: Boolean,
        val memorySelectionPerformed: Boolean,
        val trainingSelectionPerformed: Boolean,
    )

    fun validateContext(
        value: Context,
    ): Context {
        require(
            value.schemaVersion ==
                PacketSchema &&
                value.truthClass ==
                PacketTruthClass &&
                value.memoryTokenCap in
                0..MaximumMemoryTokens &&
                value.estimatedSystemMessageTokens in
                0..MaximumMemoryTokens,
        ) {
            "VexLife Memory context identity or token bounds are invalid"
        }
        requireRef(
            value.memoryContextRef,
            "memoryContextRef",
        )
        requireSha256(
            value.semanticFingerprint,
            "semanticFingerprint",
        )
        requireUniqueSortedRefs(
            value.sourceOwnerRefs,
            "sourceOwnerRefs",
        )
        requireUniqueSortedRefs(
            value.sourceRefs,
            "sourceRefs",
        )
        requireUniqueSortedRefs(
            value.heldOrDeferredStatementRefs,
            "heldOrDeferredStatementRefs",
        )
        requireUniqueSortedRefs(
            value.boundedOutStatementRefs,
            "boundedOutStatementRefs",
        )
        requireUniqueSortedRefs(
            value.tokenBoundedOutPageRefs,
            "tokenBoundedOutPageRefs",
        )

        if (
            value.included
        ) {
            require(
                value.projectionState ==
                    "CURRENT" &&
                    value.projectionCurrentness ==
                    "CURRENT" &&
                    value.pages.isNotEmpty() &&
                    value.currentDailyStratumRef !=
                    null &&
                    value.currentDailyStratumSha256 !=
                    null &&
                    value.sourceConversationHeadSha256 !=
                    null &&
                    value.sourceScoreHeadSha256 !=
                    null &&
                    value.sourceSemanticAuthorityHeadSha256 !=
                    null &&
                    value.systemMessageSha256 !=
                    null &&
                    value.estimatedSystemMessageTokens >
                    0,
            ) {
                "included VexLife Memory context is not current and source-complete"
            }
            requireRef(
                value.currentDailyStratumRef,
                "currentDailyStratumRef",
            )
            for (
                hash in
                listOf(
                    value.currentDailyStratumSha256,
                    value.sourceConversationHeadSha256,
                    value.sourceScoreHeadSha256,
                    value.sourceSemanticAuthorityHeadSha256,
                    value.systemMessageSha256,
                )
            ) {
                requireSha256(
                    requireNotNull(
                        hash,
                    ),
                    "included Memory source hash",
                )
            }
        } else {
            require(
                value.pages.isEmpty() &&
                    value.systemMessageSha256 ==
                    null &&
                    value.estimatedSystemMessageTokens ==
                    0,
            ) {
                "held/empty VexLife Memory context must not claim provider content"
            }
        }

        require(
            value.pages.size <=
                MaximumPages,
        ) {
            "VexLife Memory page bound exceeded"
        }
        value.pages.forEach(
            ::validatePage,
        )

        return value
    }

    fun validateSelection(
        value: Selection,
    ): Selection {
        require(
            value.schemaVersion ==
                SelectionSchema &&
                value.contextMode in
                setOf(
                    "IMMEDIATE_PRIOR_COMPLETED_TURN",
                    "WORKING_CHECKPOINT_SUCCESSOR",
                ) &&
                value.priorHeadSequence >=
                1 &&
                value.currentRequestInputTokenEstimate >
                0 &&
                !value.wholeHistoryEventEnumerationPerformed &&
                !value.trainingSelectionPerformed &&
                value.memorySelectionPerformed ==
                value.memoryIncluded,
        ) {
            "VexLife prompt selection semantics are invalid"
        }
        requireIdentity(
            homeRef = value.homeRef,
            deviceRef = value.deviceRef,
            lineageRef = value.lineageRef,
        )
        requireRef(
            value.threadRef,
            "threadRef",
        )
        requireRef(
            value.currentRequestEventBinding.eventRef,
            "currentRequestEventBinding.eventRef",
        )
        requireSha256(
            value.currentRequestEventBinding.eventHash,
            "currentRequestEventBinding.eventHash",
        )
        requireSha256(
            value.priorConversationHeadSha256,
            "priorConversationHeadSha256",
        )
        requireRef(
            value.memoryContextRef,
            "memoryContextRef",
        )
        requireSha256(
            value.memoryContextFingerprint,
            "memoryContextFingerprint",
        )
        requireUniqueSortedRefs(
            value.memorySourceRefs,
            "memorySourceRefs",
        )
        requireUniqueSortedRefs(
            value.workingSourceRefs,
            "workingSourceRefs",
        )
        value.selectedEventBindings.forEach {
            binding ->
            requireRef(
                binding.eventRef,
                "selectedEventBindings.eventRef",
            )
            requireSha256(
                binding.eventHash,
                "selectedEventBindings.eventHash",
            )
            requireSha256(
                binding.completedHeadSha256,
                "selectedEventBindings.completedHeadSha256",
            )
        }

        return value
    }

    private fun validatePage(
        value: Page,
    ) {
        requireRefs(
            value.pageRef,
            value.statementRef,
            value.semanticAcceptanceRef,
        )
        require(
            value.summary.isNotEmpty() &&
                value.memoryRelation.isNotEmpty() &&
                value.recordedStatementState.isNotEmpty() &&
                value.effectiveState.isNotEmpty() &&
                value.consentState in
                setOf(
                    "PERMITTED",
                    "NARROWED",
                ),
        ) {
            "VexLife Memory page meaning is incomplete or not consent-admitted"
        }
        requireSha256(
            value.summaryHash,
            "summaryHash",
        )
        requireSha256(
            value.semanticAcceptanceSha256,
            "semanticAcceptanceSha256",
        )
        requireSha256(
            value.semanticAuthorityHeadSha256,
            "semanticAuthorityHeadSha256",
        )
    }

    private fun requireIdentity(
        homeRef: String,
        deviceRef: String,
        lineageRef: String,
    ) {
        require(
            deviceRef.startsWith(
                "device.vexlife.",
            ) &&
                lineageRef.startsWith(
                    "companion-lineage.vexlife.",
                ) &&
                homeRef ==
                "vex-home.$deviceRef",
        ) {
            "VexLife Memory selection is not bound to canonical Home/device/lineage"
        }
    }

    private fun requireUniqueSortedRefs(
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
                values.size &&
                values.sorted() ==
                values,
        ) {
            "$label must be unique and sorted"
        }
    }

    private fun requireRefs(
        vararg values: String,
    ) {
        values.forEach {
            requireRef(
                it,
                "ref",
            )
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
            "$label must be one portable non-empty ref"
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
}

// [VXG RealForever]
