package vexlife.memory

import java.nio.file.Path

/**
 * Volatile service-lifetime acceleration for current Memory provider selection.
 *
 * Canonical Conversation / Score / semantic-authority / Daily files remain the only durable truth.
 * This object retains one already-validated current Memory generation and may reuse it only while
 * exact current owner head receipts prove that the semantic owners are unchanged.
 *
 * A direct completed Conversation descendant does not by itself change accepted semantic meaning.
 * The accepted-semantic projection is therefore re-observed at the new Conversation head while a
 * Daily projection whose source Conversation head is now historical becomes explicitly HELD.
 *
 * Missing cache state, owner-head drift, or non-contiguous Conversation movement falls back to the
 * canonical readers. Cache loss never loses Memory.
 */
internal class VexLifeCurrentMemoryHotPath(
    productHome: Path,
) {
    data class Diagnostics(
        val canonicalRebuildCount: Int,
        val directConversationAdvanceCount: Int,
        val exactReuseCount: Int,
    )

    private data class Generation(
        val frontier: VexLifeCurrentMemoryReader.CurrentMemoryHotPathFrontier,
        val maximumDailyContext: VexLifeMemoryContract.Context,
        val currentAcceptedSemantic: VexLifeCurrentAcceptedSemanticProjection.Projection,
    )

    private val productHome =
        productHome
            .toAbsolutePath()
            .normalize()

    private val lock =
        Any()

    private var generation: Generation? =
        null

    private var canonicalRebuildCount =
        0

    private var directConversationAdvanceCount =
        0

    private var exactReuseCount =
        0

    fun readContext(
        memoryTokenCap: Int =
            VexLifeMemoryContract.MaximumMemoryTokens,
    ): VexLifeMemoryContract.Context =
        synchronized(
            lock,
        ) {
            val current =
                currentGeneration()
            dailyContext(
                generation = current,
                memoryTokenCap = memoryTokenCap,
            )
        }

    fun readSelection(
        memoryTokenCap: Int =
            VexLifeMemoryContract.MaximumMemoryTokens,
    ): VexLifeCurrentMemoryProviderSelection.Selection =
        synchronized(
            lock,
        ) {
            val current =
                currentGeneration()
            VexLifeCurrentMemoryProviderSelection.form(
                daily =
                    dailyContext(
                        generation = current,
                        memoryTokenCap = memoryTokenCap,
                    ),
                currentAcceptedSemantic =
                    current.currentAcceptedSemantic,
                memoryTokenCap =
                    memoryTokenCap,
            )
        }

    internal fun diagnostics(): Diagnostics =
        synchronized(
            lock,
        ) {
            Diagnostics(
                canonicalRebuildCount =
                    canonicalRebuildCount,
                directConversationAdvanceCount =
                    directConversationAdvanceCount,
                exactReuseCount =
                    exactReuseCount,
            )
        }

    private fun currentGeneration(): Generation {
        val observed =
            VexLifeCurrentMemoryReader
                .readCurrentMemoryHotPathFrontier(
                    productHome,
                )
        val existing =
            generation
                ?: return rebuild(
                    observed,
                )

        requireSameHome(
            existing.frontier,
            observed,
        )
        if (
            existing.frontier ==
            observed
        ) {
            exactReuseCount +=
                1
            return existing
        }
        if (
            sameSemanticOwners(
                existing.frontier,
                observed,
            ) &&
            directConversationDescendant(
                prior = existing.frontier,
                current = observed,
            )
        ) {
            directConversationAdvanceCount +=
                1
            val advanced =
                existing.copy(
                    frontier =
                        observed,
                    maximumDailyContext =
                        dailyContextAfterConversationAdvance(
                            source =
                                existing.maximumDailyContext,
                            observed =
                                observed,
                        ),
                    currentAcceptedSemantic =
                        VexLifeCurrentAcceptedSemanticProjection
                            .reobserve(
                                projection =
                                    existing.currentAcceptedSemantic,
                                observedConversationHeadSha256 =
                                    observed.conversationHeadSha256,
                            ),
                )
            generation =
                advanced
            return advanced
        }

        return rebuild(
            observed,
        )
    }

    private fun rebuild(
        observedBefore: VexLifeCurrentMemoryReader.CurrentMemoryHotPathFrontier,
    ): Generation {
        val daily =
            VexLifeCurrentMemoryReader.readCurrent(
                productHome =
                    productHome,
                memoryTokenCap =
                    VexLifeMemoryContract.MaximumMemoryTokens,
            )
        val semantic =
            VexLifeCurrentAcceptedSemanticProjection.read(
                productHome,
            )
        val observedAfter =
            VexLifeCurrentMemoryReader
                .readCurrentMemoryHotPathFrontier(
                    productHome,
                )
        require(
            observedAfter ==
                observedBefore
        ) {
            "Memory hot path owner frontier changed during canonical rebuild"
        }
        require(
            semantic.observedConversationHeadSha256 ==
                observedAfter.conversationHeadSha256 &&
                semantic.scoreHeadSha256 ==
                observedAfter.scoreHeadSha256
        ) {
            "Memory hot path semantic projection does not bind the exact observed frontier"
        }
        if (
            semantic.scoreHeadSha256 !=
            null
        ) {
            require(
                semantic.semanticAuthorityHeadSha256 ==
                    observedAfter.semanticAuthorityHeadSha256
            ) {
                "Memory hot path semantic projection authority differs from the observed frontier"
            }
        }
        if (
            daily.included
        ) {
            require(
                daily.sourceConversationHeadSha256 ==
                    observedAfter.conversationHeadSha256 &&
                    daily.sourceScoreHeadSha256 ==
                    observedAfter.scoreHeadSha256 &&
                    daily.sourceSemanticAuthorityHeadSha256 ==
                    observedAfter.semanticAuthorityHeadSha256 &&
                    observedAfter.dailyDreamHeadSha256 !=
                    null
            ) {
                "Memory hot path Daily projection does not bind the exact observed frontier"
            }
        }

        val rebuilt =
            Generation(
                frontier =
                    observedAfter,
                maximumDailyContext =
                    daily,
                currentAcceptedSemantic =
                    semantic,
            )
        generation =
            rebuilt
        canonicalRebuildCount +=
            1
        return rebuilt
    }

    private fun dailyContext(
        generation: Generation,
        memoryTokenCap: Int,
    ): VexLifeMemoryContract.Context =
        VexLifeCurrentMemoryReader.rebindCurrentContext(
            source =
                generation.maximumDailyContext,
            memoryTokenCap =
                memoryTokenCap,
        )

    private fun dailyContextAfterConversationAdvance(
        source: VexLifeMemoryContract.Context,
        observed: VexLifeCurrentMemoryReader.CurrentMemoryHotPathFrontier,
    ): VexLifeMemoryContract.Context {
        if (
            observed.dailyDreamHeadSha256 ==
            null
        ) {
            return source
        }
        return if (
            observed.dailySourceConversationHeadSha256 ==
            observed.conversationHeadSha256
        ) {
            source
        } else {
            VexLifeCurrentMemoryReader.rebindCurrentContext(
                source =
                    source,
                memoryTokenCap =
                    VexLifeMemoryContract.MaximumMemoryTokens,
                heldReasonOverride =
                    "DAILY_MEMORY_SOURCE_FRONTIER_STALE",
            )
        }
    }

    private fun requireSameHome(
        prior: VexLifeCurrentMemoryReader.CurrentMemoryHotPathFrontier,
        current: VexLifeCurrentMemoryReader.CurrentMemoryHotPathFrontier,
    ) {
        require(
            prior.homeRef ==
                current.homeRef &&
                prior.deviceRef ==
                    current.deviceRef &&
                prior.companionLineageRef ==
                    current.companionLineageRef &&
                prior.threadRef ==
                    current.threadRef
        ) {
            "Memory hot path cannot cross canonical Home or Conversation identity"
        }
    }

    private fun sameSemanticOwners(
        prior: VexLifeCurrentMemoryReader.CurrentMemoryHotPathFrontier,
        current: VexLifeCurrentMemoryReader.CurrentMemoryHotPathFrontier,
    ): Boolean =
        prior.scoreHeadSha256 ==
            current.scoreHeadSha256 &&
            prior.scoreSourceConversationHeadSha256 ==
                current.scoreSourceConversationHeadSha256 &&
            prior.semanticAuthorityHeadSha256 ==
                current.semanticAuthorityHeadSha256 &&
            prior.dailyDreamHeadSha256 ==
                current.dailyDreamHeadSha256 &&
            prior.dailySourceConversationHeadSha256 ==
                current.dailySourceConversationHeadSha256 &&
            prior.dailySourceScoreHeadSha256 ==
                current.dailySourceScoreHeadSha256 &&
            prior.dailySourceSemanticAuthorityHeadSha256 ==
                current.dailySourceSemanticAuthorityHeadSha256

    private fun directConversationDescendant(
        prior: VexLifeCurrentMemoryReader.CurrentMemoryHotPathFrontier,
        current: VexLifeCurrentMemoryReader.CurrentMemoryHotPathFrontier,
    ): Boolean {
        val priorHead =
            prior.conversationHeadSha256
                ?: return false
        val priorSequence =
            prior.conversationSequence
                ?: return false
        val currentHead =
            current.conversationHeadSha256
                ?: return false
        val currentPrior =
            current.priorConversationHeadSha256
                ?: return false
        val currentSequence =
            current.conversationSequence
                ?: return false
        return currentHead !=
            priorHead &&
            currentPrior ==
                priorHead &&
            currentSequence ==
                priorSequence +
                2
    }
}

// [VXG RealForever]
