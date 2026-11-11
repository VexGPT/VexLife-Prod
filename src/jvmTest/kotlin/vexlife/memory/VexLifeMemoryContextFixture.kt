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

    fun current(): VexLifeMemoryContract.Context {
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
        val providerPacket =
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
                memoryTokenCap = VexLifeMemoryContract.MaximumMemoryTokens,
            )
        val systemContent =
            listOf(
                "VEXLIFE_CURRENT_MEMORY_CONTEXT/v1",
                "These are current source-attributed Memory references, not proof of subjective recall or original wording by this model.",
                "Use them as bounded continuity context. If they conflict with current source or the human, preserve the conflict rather than inventing reconciliation.",
                VexContractJson.compactEncode(
                    providerPacket,
                ),
            )
                .joinToString(
                    "\n",
                )
        val systemMessageSha256 =
            sha256(
                systemContent,
            )
        val estimatedSystemMessageTokens =
            estimatedMessageTokens(
                systemContent,
            )
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
            listOf(
                "acceptance.score.001",
                "daily-memory-stratum.vexlife.001",
                "github.issue.vextreme-sdk.225",
                "github.issue.vextreme-sdk.416",
                "page.living-journal.memory.001",
                "statement.score.continuity.001",
            )

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
                "memory-context.vexlife.prompt.${fingerprint.take(32)}",
            semanticFingerprint = fingerprint,
            sourceRefs = sourceRefs,
            included = true,
        )
    }

    fun currentUnselectedForPrompt(): VexLifeMemoryContract.Context {
        val selected =
            current()
        val boundedPageRefs =
            selected.pages
                .map(
                    VexLifeMemoryContract.Page::pageRef,
                )
                .sorted()
        val core =
            VexLifeMemoryContract.PacketCore(
                schemaVersion = selected.schemaVersion,
                truthClass = selected.truthClass,
                projectionState = selected.projectionState,
                projectionCurrentness = selected.projectionCurrentness,
                sourceOwnerRefs = selected.sourceOwnerRefs,
                currentDailyStratumRef = selected.currentDailyStratumRef,
                currentDailyStratumSha256 = selected.currentDailyStratumSha256,
                sourceConversationHeadSha256 = selected.sourceConversationHeadSha256,
                sourceScoreHeadSha256 = selected.sourceScoreHeadSha256,
                sourceSemanticAuthorityHeadSha256 = selected.sourceSemanticAuthorityHeadSha256,
                pages = emptyList(),
                heldOrDeferredStatementRefs = selected.heldOrDeferredStatementRefs,
                boundedOutStatementRefs = selected.boundedOutStatementRefs,
                tokenBoundedOutPageRefs = boundedPageRefs,
                reasons = selected.reasons,
                memoryTokenCap = 0,
                systemMessageSha256 = null,
                estimatedSystemMessageTokens = 0,
            )
        val fingerprint =
            sha256(
                VexContractJson.canonicalEncode(
                    core,
                ),
            )
        val sourceRefs =
            buildList {
                addAll(
                    core.sourceOwnerRefs,
                )
                core.currentDailyStratumRef
                    ?.let(
                        ::add,
                    )
            }
                .distinct()
                .sorted()

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
            systemMessageSha256 = null,
            estimatedSystemMessageTokens = 0,
            memoryContextRef =
                "memory-context.vexlife.prompt." +
                    fingerprint.take(
                        32,
                    ),
            semanticFingerprint = fingerprint,
            sourceRefs = sourceRefs,
            included = false,
        )
            .let(
                VexLifeMemoryContract::validateContext,
            )
    }

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
