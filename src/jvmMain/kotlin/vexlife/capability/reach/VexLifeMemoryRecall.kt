package vexlife.capability.reach

import java.nio.file.Path
import vexlife.memory.VexLifeCurrentMemoryReader
import vexlife.memory.VexLifeMemoryContract

/**
 * Read-only recall over current Memory pages, including pages the prompt had no room for.
 *
 * Owner of the pages stays [VexLifeCurrentMemoryReader]: this asks it for the widest admitted
 * projection (the Memory token maximum) and searches summaries there. Nothing is accepted,
 * corrected or written; consent and statement state come back exactly as the reader projects them.
 */
object VexLifeMemoryRecall {
    const val MaximumTerms = 6
    const val MaximumMatches = 24

    data class Match(
        val statementRef: String,
        val summary: String,
        val memoryRelation: String,
        val effectiveState: String,
        val matchedTerms: List<String>,
    )

    sealed interface Outcome {
        data class Found(
            val terms: List<String>,
            val matches: List<Match>,
            val searchedPageCount: Int,
            val pagesBeyondReach: Int,
            val projectionState: String,
        ) : Outcome

        data class Refused(val reason: String) : Outcome

        data class Unavailable(val reason: String) : Outcome
    }

    fun search(
        home: Path,
        terms: List<String>,
        read: (Path) -> VexLifeMemoryContract.Context = {
            VexLifeCurrentMemoryReader.readCurrent(it)
        },
    ): Outcome {
        val cleaned =
            terms.map { it.trim() }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }
        if (cleaned.isEmpty() || cleaned.size > MaximumTerms || cleaned.any { it.length > 160 }) {
            return Outcome.Refused("give 1-$MaximumTerms terms of up to 160 characters")
        }
        val context =
            try {
                read(home)
            } catch (error: Exception) {
                return Outcome.Unavailable(
                    "Memory could not be read now: " + (error.message ?: error.javaClass.simpleName)
                )
            }
        val lowered = cleaned.map { it.lowercase() }
        val matches =
            context.pages
                .mapNotNull { page ->
                    val summary = page.summary.lowercase()
                    val hit = cleaned.filterIndexed { index, _ -> summary.contains(lowered[index]) }
                    if (hit.isEmpty()) null
                    else
                        Match(
                            page.statementRef,
                            page.summary,
                            page.memoryRelation,
                            page.effectiveState,
                            hit,
                        )
                }
                .sortedByDescending { it.matchedTerms.size }
        return Outcome.Found(
            terms = cleaned,
            matches = matches.take(MaximumMatches),
            searchedPageCount = context.pages.size,
            pagesBeyondReach = context.tokenBoundedOutPageRefs.size,
            projectionState = context.projectionState,
        )
    }
}

// [VXG RealForever]
