package vexlife.memory

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlinx.serialization.Serializable
import vexlife.contract.VexContractJson

/** Test-only formation of exact current VexLife Memory provider bytes. */
object VexLifeMemoryContextFixture {
    @Serializable
    private data class ProviderPacket(
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
        val pages: List<VexLifeMemoryContract.Page>,
        val heldOrDeferredStatementRefs: List<String>,
        val boundedOutStatementRefs: List<String>,
        val tokenBoundedOutPageRefs: List<String>,
        val reasons: List<String>,
        val memoryTokenCap: Int,
    )

    fun current(
        memoryTokenCap: Int =
            VexLifeMemoryContract.MaximumMemoryTokens,
    ): VexLifeMemoryContract.Context {
        require(
            memoryTokenCap in
                0..VexLifeMemoryContract.MaximumMemoryTokens,
        )

        val page =
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
        val basePacket =
            ProviderPacket(
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
                        page,
                    ),
                heldOrDeferredStatementRefs = emptyList(),
                boundedOutStatementRefs = emptyList(),
                tokenBoundedOutPageRefs = emptyList(),
                reasons = emptyList(),
                memoryTokenCap = memoryTokenCap,
            )
        val candidateSystemContent =
            listOf(
                "VEXLIFE_CURRENT_MEMORY_CONTEXT/v1",
                "These are current source-attributed Memory references, not proof of subjective recall or original wording by this model.",
                "Use them as bounded continuity context. If they conflict with current source or the human, preserve the conflict rather than inventing reconciliation.",
                VexContractJson.compactEncode(
                    basePacket,
                ),
            )
                .joinToString(
                    "\n",
                )
        val candidateEstimate =
            estimatedMessageTokens(
                candidateSystemContent,
            )
        val included =
            candidateEstimate <=
                memoryTokenCap
        val providerPacket =
            if (
                included
            ) {
                basePacket
            } else {
                basePacket.copy(
                    pages = emptyList(),
                    tokenBoundedOutPageRefs =
                        listOf(
                            page.pageRef,
                        ),
                )
            }
        val systemContent =
            if (
                included
            ) {
                candidateSystemContent
            } else {
                null
            }
        val systemMessageSha256 =
            systemContent
                ?.let(
                    ::sha256,
                )
        val estimatedSystemMessageTokens =
            if (
                included
            ) {
                candidateEstimate
            } else {
                0
            }
        val core =
            VexLifeMemoryContract.PacketCore(
                schemaVersion = providerPacket.schemaVersion,
                truthClass = providerPacket.truthClass,
                projectionState = providerPacket.projectionState,
                projectionCurrentness = providerPacket.projectionCurrentness,
                sourceOwnerRefs = providerPacket.sourceOwnerRefs,
                currentDailyStratumRef = providerPacket.currentDailyStratumRef,
                currentDailyStratumSha256 = providerPacket.currentDailyStratumSha256,
                sourceConversationHeadSha256 = providerPacket.sourceConversationHeadSha256,
                sourceScoreHeadSha256 = providerPacket.sourceScoreHeadSha256,
                sourceSemanticAuthorityHeadSha256 = providerPacket.sourceSemanticAuthorityHeadSha256,
                pages = providerPacket.pages,
                heldOrDeferredStatementRefs = providerPacket.heldOrDeferredStatementRefs,
                boundedOutStatementRefs = providerPacket.boundedOutStatementRefs,
                tokenBoundedOutPageRefs = providerPacket.tokenBoundedOutPageRefs,
                reasons = providerPacket.reasons,
                memoryTokenCap = providerPacket.memoryTokenCap,
                systemMessageSha256 = systemMessageSha256,
                estimatedSystemMessageTokens = estimatedSystemMessageTokens,
            )
        val fingerprint =
            sha256(
                VexContractJson.canonicalEncode(
                    core,
                ),
            )
        val sourceRefs =
            if (
                included
            ) {
                listOf(
                    "acceptance.score.001",
                    "daily-memory-stratum.vexlife.001",
                    "github.issue.vextreme-sdk.225",
                    "github.issue.vextreme-sdk.416",
                    "page.living-journal.memory.001",
                    "statement.score.continuity.001",
                )
            } else {
                listOf(
                    "daily-memory-stratum.vexlife.001",
                    "github.issue.vextreme-sdk.225",
                    "github.issue.vextreme-sdk.416",
                )
            }

        return VexLifeMemoryContract.Context(
            schemaVersion = core.schemaVersion,
            truthClass = core.truthClass,
            projectionState = core.projectionState,
            projectionCurrentness = core.projectionCurrentness,
            sourceOwnerRefs = core.sourceOwnerRefs,
            currentDailyStratumRef = core.currentDailyStratumRef,
            currentDailyStratumSha256 = core.currentDailyStratumSha256,
            sourceConversationHeadSha256 = core.sourceConversationHeadSha256,
            sourceScoreHeadSha256 = core.sourceScoreHeadSha256,
            sourceSemanticAuthorityHeadSha256 = core.sourceSemanticAuthorityHeadSha256,
            pages = core.pages,
            heldOrDeferredStatementRefs = core.heldOrDeferredStatementRefs,
            boundedOutStatementRefs = core.boundedOutStatementRefs,
            tokenBoundedOutPageRefs = core.tokenBoundedOutPageRefs,
            reasons = core.reasons,
            memoryTokenCap = core.memoryTokenCap,
            systemMessageSha256 = core.systemMessageSha256,
            estimatedSystemMessageTokens = core.estimatedSystemMessageTokens,
            memoryContextRef =
                "memory-context.vexlife.prompt." +
                    fingerprint.take(
                        32,
                    ),
            semanticFingerprint = fingerprint,
            sourceRefs = sourceRefs,
            included = included,
        )
            .let(
                VexLifeMemoryContract::validateContext,
            )
    }

    fun currentUnselectedForPrompt(): VexLifeMemoryContract.Context =
        current(
            memoryTokenCap = 0,
        )

    private fun estimatedMessageTokens(
        content: String,
    ): Int {
        val bytes =
            content.toByteArray(
                StandardCharsets.UTF_8,
            )
                .size

        return maxOf(
            1,
            (
                bytes +
                    3
            ) /
                4,
        ) +
            4
    }

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
                    it.toInt() and
                        0xff,
                )
            }
}

// [VXG RealForever]
