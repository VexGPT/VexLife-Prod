package vexlife.capability.reach

import java.nio.file.Path
import vexlife.companion.DirectConversationSourceReturn
import vexlife.continuity.VexLifeWorkingContinuity
import vexlife.conversation.DirectConversationStore
import vexlife.memory.VexLifeMemoryContract

/**
 * One bounded read-only hydration over continuity owners already present in a direct turn.
 *
 * It owns no Conversation, Memory, Working Continuity or semantic truth. One model-visible request
 * may fan into several owner reads, fuse duplicate candidates, exact-reopen Conversation source,
 * and return current Memory plus explicit unresolved Working Continuity coordinates.
 *
 * This first slice is lexical + exact-current-anchor only. Terms inside one query group are
 * conjunctive clues; query groups are alternate vantages. A lexical miss is not proof that no
 * relevant durable history exists; semantic candidate location remains a separate successor edge.
 */
object VexLifeContinuityHydration {
    const val ContextHeader = "VEXLIFE_CONTINUITY_HYDRATION/v1"
    const val LocatorMode = "LEXICAL_PLUS_CURRENT_ANCHORS"
    const val MaximumQueryGroups = 4
    const val MaximumTermsPerQuery = 6
    const val MaximumConversationTurns = 6
    const val MaximumMemoryPages = 12
    const val MaximumFrontierRefs = 24

    const val Returned = "RETURNED"
    const val NotFound = "NOT_FOUND"
    const val HeldTokenBudget = "HELD_TOKEN_BUDGET"

    data class Query(val ref: String, val terms: List<String>)

    data class ConversationMatch(
        val request: DirectConversationStore.Event,
        val response: DirectConversationStore.Event,
        val queryRefs: List<String>,
        val matchedTerms: List<String>,
    )

    data class MemoryMatch(
        val page: VexLifeMemoryContract.Page,
        val queryRefs: List<String>,
        val matchedTerms: List<String>,
        val workingAnchor: Boolean,
    )

    data class Frontier(val ref: String, val reason: String)

    data class Resolution(
        val queries: List<Query>,
        val conversation: List<ConversationMatch>,
        val memory: List<MemoryMatch>,
        val frontier: List<Frontier>,
        val scannedCompletedTurns: Int,
        val conversationOwnerReadCount: Int,
        val memoryOwnerReadCount: Int,
        val duplicateCandidatesSuppressed: Int,
        val memoryProjectionState: String,
        val memoryReasons: List<String>,
        val excludedConversationEventCount: Int,
        val excludedMemoryStatementCount: Int,
    )

    data class Projection(
        val state: String,
        val text: String,
        val sourceRefs: List<String>,
        val frontierRefs: List<String>,
        val selectedConversationTurnCount: Int,
        val selectedMemoryPageCount: Int,
        val discoveredConversationTurnCount: Int,
        val discoveredMemoryPageCount: Int,
        val duplicateCandidateCount: Int,
        val omittedCandidateCount: Int,
        val locatorMode: String = LocatorMode,
        val contentTruncated: Boolean = false,
        val effectsPerformed: Boolean = false,
        val conversationMutationPerformed: Boolean = false,
        val memoryMutationPerformed: Boolean = false,
        val semanticAcceptanceCreated: Boolean = false,
    )

    fun resolve(
        productHome: Path,
        conversationOwner: DirectConversationStore.Locked,
        queryGroups: List<List<String>>,
        memoryContext: VexLifeMemoryContract.Context,
        workingSet: VexLifeWorkingContinuity.WorkingSet?,
        excludedConversationEventRefs: Set<String> = emptySet(),
        excludedMemoryStatementRefs: Set<String> = emptySet(),
    ): Resolution {
        val queries = requireQueries(queryGroups)
        val memory = VexLifeMemoryContract.validateContext(memoryContext)
        val conversation = linkedMapOf<String, ConversationAccumulator>()
        val memoryMatches = linkedMapOf<String, MemoryAccumulator>()
        val pagesByStatement = memory.pages.associateBy(VexLifeMemoryContract.Page::statementRef)
        var scannedCompletedTurns = 0
        var rawCandidates = 0

        queries.forEach { query ->
            val discovery =
                DirectConversationSourceReturn.discover(
                    conversationOwner = conversationOwner,
                    request =
                        DirectConversationSourceReturn.Request(
                            schemaVersion = DirectConversationSourceReturn.RequestSchema,
                            queryTerms = query.terms,
                            speakerScope = DirectConversationSourceReturn.AnySpeaker,
                        ),
                    excludedEventRefs = excludedConversationEventRefs,
                )
            scannedCompletedTurns = maxOf(scannedCompletedTurns, discovery.scannedCompletedTurnCount)
            rawCandidates += discovery.matches.size
            discovery.matches
                .filter { match ->
                    query.terms.all { term ->
                        match.matchedTerms.any { matched ->
                            matched.equals(term, ignoreCase = true)
                        }
                    }
                }
                .forEach { match ->
                    val key = "${match.requestEvent.eventRef}|${match.responseEvent.eventRef}"
                    conversation
                        .getOrPut(key) {
                            ConversationAccumulator(match.requestEvent, match.responseEvent)
                        }
                        .add(query.ref, match.matchedTerms)
                }

            when (
                val recalled =
                    VexLifeMemoryRecall.search(
                        home = productHome,
                        terms = query.terms,
                        read = { memory },
                    )
            ) {
                is VexLifeMemoryRecall.Outcome.Found -> {
                    rawCandidates += recalled.matches.size
                    recalled.matches
                        .filter { match ->
                            query.terms.all { term ->
                                match.matchedTerms.any { matched ->
                                    matched.equals(term, ignoreCase = true)
                                }
                            }
                        }
                        .forEach { match ->
                            if (match.statementRef !in excludedMemoryStatementRefs) {
                                pagesByStatement[match.statementRef]?.let { page ->
                                    memoryMatches
                                        .getOrPut(match.statementRef) { MemoryAccumulator(page) }
                                        .add(query.ref, match.matchedTerms)
                                }
                            }
                        }
                }
                is VexLifeMemoryRecall.Outcome.Refused ->
                    error("invalid continuity Memory query: ${recalled.reason}")
                is VexLifeMemoryRecall.Outcome.Unavailable ->
                    error("continuity hydration lost current Memory: ${recalled.reason}")
            }
        }

        workingSet?.currentStatementRefs.orEmpty().forEach { statementRef ->
            if (statementRef !in excludedMemoryStatementRefs) {
                pagesByStatement[statementRef]?.let { page ->
                    memoryMatches
                        .getOrPut(statementRef) { MemoryAccumulator(page) }
                        .anchor()
                }
            }
        }

        val selectedConversation =
            conversation.values
                .map(ConversationAccumulator::project)
                .sortedWith(
                    compareByDescending<ConversationMatch> { it.queryRefs.size }
                        .thenByDescending { it.matchedTerms.size }
                        .thenByDescending { it.matchedTerms.sumOf(String::length) }
                        .thenByDescending { it.response.sequence },
                )
                .take(MaximumConversationTurns)
        val selectedMemory =
            memoryMatches.values
                .map(MemoryAccumulator::project)
                .sortedWith(
                    compareByDescending<MemoryMatch> { it.workingAnchor }
                        .thenByDescending { it.queryRefs.size }
                        .thenByDescending { it.matchedTerms.size }
                        .thenBy { it.page.statementRef },
                )
                .take(MaximumMemoryPages)
        val resolvedStatements = selectedMemory.map { it.page.statementRef }.toSet()
        val frontier = workingFrontier(workingSet, resolvedStatements)
        val uniqueCandidates = conversation.size + memoryMatches.size

        return Resolution(
            queries = queries,
            conversation = selectedConversation,
            memory = selectedMemory,
            frontier = frontier,
            scannedCompletedTurns = scannedCompletedTurns,
            conversationOwnerReadCount = queries.size,
            memoryOwnerReadCount = queries.size,
            duplicateCandidatesSuppressed = maxOf(0, rawCandidates - uniqueCandidates),
            memoryProjectionState = memory.projectionState,
            memoryReasons = memory.reasons,
            excludedConversationEventCount = excludedConversationEventRefs.size,
            excludedMemoryStatementCount = excludedMemoryStatementRefs.size,
        )
    }

    fun project(
        resolution: Resolution,
        availableTokens: Int,
        estimateTokens: (String) -> Int,
    ): Projection {
        require(availableTokens >= 0) { "continuity hydration token allowance must be nonnegative" }
        val conversation = mutableListOf<ConversationMatch>()
        val memory = mutableListOf<MemoryMatch>()
        val omitted = mutableListOf<Frontier>()

        fun rendered(): String = render(resolution, conversation, memory, omitted)
        if (estimateTokens(rendered()) > availableTokens) {
            return Projection(
                state = HeldTokenBudget,
                text = "$ContextHeader\nstate=$HeldTokenBudget\nNo historical source was returned; no owner was mutated.",
                sourceRefs = emptyList(),
                frontierRefs = resolution.frontier.map(Frontier::ref),
                selectedConversationTurnCount = 0,
                selectedMemoryPageCount = 0,
                discoveredConversationTurnCount = resolution.conversation.size,
                discoveredMemoryPageCount = resolution.memory.size,
                duplicateCandidateCount = resolution.duplicateCandidatesSuppressed,
                omittedCandidateCount = resolution.conversation.size + resolution.memory.size,
            )
        }

        val max = maxOf(resolution.conversation.size, resolution.memory.size)
        repeat(max) { index ->
            resolution.conversation.getOrNull(index)?.let { candidate ->
                conversation += candidate
                if (estimateTokens(rendered()) > availableTokens) {
                    conversation.removeLast()
                    omitted += Frontier(candidate.request.eventRef, "RETURN_BUDGET")
                }
            }
            resolution.memory.getOrNull(index)?.let { candidate ->
                memory += candidate
                if (estimateTokens(rendered()) > availableTokens) {
                    memory.removeLast()
                    omitted += Frontier(candidate.page.statementRef, "RETURN_BUDGET")
                }
            }
        }

        val text = rendered()
        require(estimateTokens(text) <= availableTokens) {
            "continuity hydration projection exceeded its admitted result budget"
        }
        val sourceRefs =
            buildList {
                    conversation.forEach {
                        add(it.request.eventRef)
                        add(it.response.eventRef)
                    }
                    memory.forEach {
                        add(it.page.pageRef)
                        add(it.page.statementRef)
                        add(it.page.semanticAcceptanceRef)
                    }
                }
                .distinct()
                .sorted()
        val frontierRefs =
            (resolution.frontier + omitted).map(Frontier::ref).distinct().sorted()
        val state = if (conversation.isEmpty() && memory.isEmpty()) NotFound else Returned
        return Projection(
            state = state,
            text = text,
            sourceRefs = sourceRefs,
            frontierRefs = frontierRefs,
            selectedConversationTurnCount = conversation.size,
            selectedMemoryPageCount = memory.size,
            discoveredConversationTurnCount = resolution.conversation.size,
            discoveredMemoryPageCount = resolution.memory.size,
            duplicateCandidateCount = resolution.duplicateCandidatesSuppressed,
            omittedCandidateCount = omitted.size,
        )
    }

    fun hydrate(
        productHome: Path,
        conversationOwner: DirectConversationStore.Locked,
        queryGroups: List<List<String>>,
        memoryContext: VexLifeMemoryContract.Context,
        workingSet: VexLifeWorkingContinuity.WorkingSet?,
        excludedConversationEventRefs: Set<String>,
        excludedMemoryStatementRefs: Set<String>,
        availableTokens: Int,
        estimateTokens: (String) -> Int,
    ): Projection =
        project(
            resolve(
                productHome,
                conversationOwner,
                queryGroups,
                memoryContext,
                workingSet,
                excludedConversationEventRefs,
                excludedMemoryStatementRefs,
            ),
            availableTokens,
            estimateTokens,
        )

    private fun render(
        resolution: Resolution,
        conversation: List<ConversationMatch>,
        memory: List<MemoryMatch>,
        omitted: List<Frontier>,
    ): String = buildString {
        appendLine(ContextHeader)
        appendLine("truthClass=BOUNDED_READ_ONLY_MULTI_OWNER_CONTINUITY_HYDRATION")
        appendLine("locatorMode=$LocatorMode")
        appendLine("queries=" + resolution.queries.joinToString(" | ") { "${it.ref}:${it.terms.joinToString(",")}" })
        appendLine("coverage=exact Conversation + current Memory + Working Continuity coordinates")
        appendLine(
            "scannedCompletedTurns=${resolution.scannedCompletedTurns}; " +
                "conversationOwnerReads=${resolution.conversationOwnerReadCount}; " +
                "memoryOwnerReads=${resolution.memoryOwnerReadCount}; " +
                "duplicateCandidatesSuppressed=${resolution.duplicateCandidatesSuppressed}"
        )
        appendLine("memoryProjectionState=${resolution.memoryProjectionState}")
        if (resolution.memoryReasons.isNotEmpty()) {
            appendLine("memoryReasons=" + resolution.memoryReasons.joinToString(" | "))
        }
        appendLine("alreadyPresentExcluded=conversationEvents:${resolution.excludedConversationEventCount},memoryStatements:${resolution.excludedMemoryStatementCount}")
        appendLine("boundary=no semantic-similarity locator; lexical/current-anchor miss != no relevant durable history")
        appendLine("CONVERSATION_EXACT_SOURCE")
        conversation.forEach {
            appendLine("[queries=${it.queryRefs.joinToString(",")}; matched=${it.matchedTerms.joinToString(",")}; request=${it.request.eventRef}; response=${it.response.eventRef}]")
            appendLine("HUMAN: ${it.request.content}")
            appendLine("VEX: ${it.response.content}")
        }
        if (conversation.isEmpty()) appendLine("(none selected)")
        appendLine("MEMORY_CURRENT_PROJECTION")
        memory.forEach {
            appendLine("[queries=${it.queryRefs.joinToString(",")}; statement=${it.page.statementRef}; page=${it.page.pageRef}; state=${it.page.effectiveState}; relation=${it.page.memoryRelation}; workingAnchor=${it.workingAnchor}]")
            appendLine(it.page.summary)
        }
        if (memory.isEmpty()) appendLine("(none selected)")
        appendLine("EXPLICIT_FRONTIER")
        val allFrontier = resolution.frontier + omitted
        allFrontier.forEach { appendLine("- ${it.ref} [${it.reason}]") }
        if (allFrontier.isEmpty()) appendLine("(none)")
        append("effectsPerformed=false; conversationMutationPerformed=false; memoryMutationPerformed=false; semanticAcceptanceCreated=false")
    }.trimEnd()

    private fun workingFrontier(
        workingSet: VexLifeWorkingContinuity.WorkingSet?,
        resolvedStatements: Set<String>,
    ): List<Frontier> =
        buildList {
                workingSet?.activeWorkNodeRefs.orEmpty().forEach { add(Frontier(it, "ACTIVE_WORK_NODE_NO_GENERIC_RESOLVER")) }
                workingSet?.currentIntentReceiptRefs.orEmpty().forEach { add(Frontier(it, "CURRENT_INTENT_RECEIPT_NO_GENERIC_RESOLVER")) }
                workingSet?.openLoopRefs.orEmpty().forEach { add(Frontier(it, "OPEN_LOOP_NO_GENERIC_RESOLVER")) }
                workingSet?.currentContinuityRecordRefs.orEmpty().forEach { add(Frontier(it, "CONTINUITY_RECORD_NO_GENERIC_RESOLVER")) }
                workingSet?.currentStatementRefs.orEmpty().filterNot(resolvedStatements::contains).forEach {
                    add(Frontier(it, "CURRENT_STATEMENT_NOT_IN_CURRENT_MEMORY_PROJECTION"))
                }
            }
            .distinctBy(Frontier::ref)
            .take(MaximumFrontierRefs)

    private fun requireQueries(groups: List<List<String>>): List<Query> {
        require(groups.size in 1..MaximumQueryGroups) {
            "continuity hydration requires 1-$MaximumQueryGroups query groups"
        }
        return groups.mapIndexed { index, terms ->
            val cleaned = terms.map(String::trim).filter(String::isNotEmpty).distinctBy { it.lowercase() }
            require(cleaned.size in 1..MaximumTermsPerQuery && cleaned.all { it.length <= 160 }) {
                "continuity hydration query ${index + 1} requires 1-$MaximumTermsPerQuery terms up to 160 characters"
            }
            Query("query.${index + 1}", cleaned)
        }
    }

    private data class ConversationAccumulator(
        val request: DirectConversationStore.Event,
        val response: DirectConversationStore.Event,
        val queryRefs: MutableSet<String> = linkedSetOf(),
        val matchedTerms: MutableSet<String> = linkedSetOf(),
    ) {
        fun add(queryRef: String, terms: List<String>) {
            queryRefs += queryRef
            matchedTerms += terms
        }
        fun project() = ConversationMatch(request, response, queryRefs.toList().sorted(), matchedTerms.toList().sorted())
    }

    private data class MemoryAccumulator(
        val page: VexLifeMemoryContract.Page,
        val queryRefs: MutableSet<String> = linkedSetOf(),
        val matchedTerms: MutableSet<String> = linkedSetOf(),
        var workingAnchor: Boolean = false,
    ) {
        fun add(queryRef: String, terms: List<String>) {
            queryRefs += queryRef
            matchedTerms += terms
        }
        fun anchor() {
            queryRefs += "working-current-statement"
            workingAnchor = true
        }
        fun project() = MemoryMatch(page, queryRefs.toList().sorted(), matchedTerms.toList().sorted(), workingAnchor)
    }
}

// [VXG RealForever]
