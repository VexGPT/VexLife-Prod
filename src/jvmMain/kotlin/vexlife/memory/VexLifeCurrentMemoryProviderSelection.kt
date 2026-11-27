package vexlife.memory

import java.nio.file.Path

/**
 * Memory-owned selection of the provider components that may enter one prompt Memory budget.
 *
 * Daily / Living Journal is selected first. Current accepted semantic continuity then excludes only
 * the exact statement refs that Daily actually supplied and receives only the remaining admitted
 * Memory token room.
 *
 * This owner does not choose prompt placement, provider role, or receipt shape. Those remain with
 * the current prompt/context owner.
 */
internal object VexLifeCurrentMemoryProviderSelection {
    data class Selection(
        val memoryTokenCap: Int,
        val dailyContext: VexLifeMemoryContract.Context,
        val currentAcceptedSemanticBlock: VexLifeCurrentAcceptedSemanticPromptBlock.Block,
        val remainingCurrentSemanticTokenCap: Int,
        val estimatedSelectedTokens: Int,
        val effectsPerformed: Boolean = false,
    )

    fun read(
        productHome: Path,
        memoryTokenCap: Int = VexLifeMemoryContract.MaximumMemoryTokens,
    ): Selection =
        form(
            daily =
                VexLifeCurrentMemoryReader.readCurrent(
                    productHome = productHome,
                    memoryTokenCap = memoryTokenCap,
                ),
            currentAcceptedSemantic =
                VexLifeCurrentAcceptedSemanticProjection.read(
                    productHome,
                ),
            memoryTokenCap =
                memoryTokenCap,
        )

    internal fun form(
        daily: VexLifeMemoryContract.Context,
        currentAcceptedSemantic: VexLifeCurrentAcceptedSemanticProjection.Projection,
        memoryTokenCap: Int,
    ): Selection {
        require(memoryTokenCap in 0..VexLifeMemoryContract.MaximumMemoryTokens) {
            "current Memory provider selection token cap is outside the admitted range"
        }
        VexLifeMemoryContract.validateContext(
            daily,
        )
        require(daily.memoryTokenCap == memoryTokenCap) {
            "Daily Memory selection does not bind the admitted Memory token cap"
        }
        require(daily.estimatedSystemMessageTokens in 0..memoryTokenCap) {
            "Daily Memory selection exceeds the admitted Memory token cap"
        }

        val dailyStatementRefs =
            daily.pages
                .map(VexLifeMemoryContract.Page::statementRef)
                .also {
                    require(it.distinct().size == it.size) {
                        "Daily Memory selection contains duplicate statement refs"
                    }
                }
                .toSet()
        val remainingCurrentSemanticTokenCap = memoryTokenCap - daily.estimatedSystemMessageTokens
        val currentAcceptedSemanticBlock =
            VexLifeCurrentAcceptedSemanticPromptBlock.form(
                projection =
                    currentAcceptedSemantic,
                excludedStatementRefs =
                    dailyStatementRefs,
                memoryTokenCap =
                    remainingCurrentSemanticTokenCap,
            )
        require(
            dailyStatementRefs.intersect(
                currentAcceptedSemanticBlock.statementRefs.toSet(),
            )
                .isEmpty()
        ) {
            "Daily and current accepted semantic provider selections overlap by statement ref"
        }

        val estimatedSelectedTokens =
            daily.estimatedSystemMessageTokens +
                currentAcceptedSemanticBlock.estimatedContentTokens
        require(estimatedSelectedTokens <= memoryTokenCap) {
            "selected Memory provider components exceed the admitted Memory token cap"
        }
        require(!currentAcceptedSemanticBlock.effectsPerformed) {
            "current accepted semantic provider selection performed an effect"
        }

        return Selection(
            memoryTokenCap =
                memoryTokenCap,
            dailyContext =
                daily,
            currentAcceptedSemanticBlock =
                currentAcceptedSemanticBlock,
            remainingCurrentSemanticTokenCap =
                remainingCurrentSemanticTokenCap,
            estimatedSelectedTokens =
                estimatedSelectedTokens,
            effectsPerformed = false,
        )
    }
}

// [VXG RealForever]
