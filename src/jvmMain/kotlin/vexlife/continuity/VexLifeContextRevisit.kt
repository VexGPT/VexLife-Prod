package vexlife.continuity

import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import vexlife.conversation.DirectConversationStore
import vexlife.memory.VexLifeMemoryContract

/**
 * Context revisitation: one request from Vex, many bounded owner reads by the host, one honest
 * packet back.
 *
 * ```
 * queries[] (terms / seedRefs / window / corrections), scopes, order, budgets
 *   -> each owner is read once: canonical Conversation, current Memory pages, current working
 *      checkpoint (the caller reads them; this object never reads or writes the Home)
 *   -> locate per query, then one typed step outward (corrects / corrected by / pointed back from)
 *   -> one node per turn or page, however many routes reached it; every route is kept
 *   -> where the work stands, corrections, matches, Memory — then the budget
 *   -> what did not fit is named as a frontier with the exact way to ask for it
 *   -> what was searched, what could not be reached, and that not found is not "never happened"
 * ```
 *
 * Permanent distinctions, near the code because they are easy to blur:
 * - LOCATE != EXPAND: a term hit is a location; following a correction is a typed step.
 * - A match is relevance to this request, not meaning: nothing here is accepted, corrected, written
 *   or promoted, and the result is not a new working state.
 * - Turns already in the prompt are named, not repeated.
 * - Words are matched exactly (case- and space-insensitive). There is no meaning index yet, so a
 *   different wording needs another query in the same request, and the packet says so.
 */
object VexLifeContextRevisit {
    const val Header = "VEXLIFE_REVISIT/v1"
    const val MaximumQueries = 6
    const val MaximumTermsPerQuery = 6
    const val MaximumTermCharacters = 160
    const val MaximumSeedRefs = 12
    const val DefaultMaximumTurns = 6
    const val MaximumTurns = 12
    const val DefaultMaximumTokens = 6_000
    const val MaximumTokens = 8_000
    const val ExcerptCharacters = 2_400
    const val MaximumFrontierRefsListed = 24
    const val MaximumWindowHours = 24 * 365
    const val MaximumWindowTurns = 10_000

    enum class Scope {
        CONVERSATION,
        MEMORY,
        WORKING_CONTINUITY,
    }

    enum class Speaker {
        ANY,
        HUMAN,
        VEX,
    }

    enum class Order {
        CURRENT_FIRST,
        CHRONOLOGICAL,
    }

    enum class State {
        RETURNED,
        PARTIAL,
        NOT_FOUND,
    }

    /** Why a node arrived in the result. Direct routes locate; the last three are typed steps. */
    enum class Route {
        TERMS,
        SEED,
        WINDOW,
        CORRECTIONS,
        CORRECTS,
        CORRECTED_BY,
        POINTED_BACK_FROM,
    }

    /** Human-time and Conversation-density lenses joined as a union: whichever reaches further. */
    data class Window(val lastHours: Int?, val lastTurns: Int?)

    data class Query(
        val ref: String,
        val terms: List<String>,
        val seedRefs: List<String>,
        val speaker: Speaker,
        val window: Window?,
        val corrections: Boolean,
        val from: Int?,
    )

    data class Request(
        val queries: List<Query>,
        val scopes: Set<Scope>,
        val order: Order,
        val maxTurns: Int,
        val maxTokens: Int,
    )

    sealed interface Parsed {
        data class Admitted(val request: Request) : Parsed

        data class Refused(val reason: String) : Parsed
    }

    /** What the owners said when asked; a null value carries its reason in the matching gap. */
    data class Sources(
        val conversation: DirectConversationStore.Snapshot?,
        val conversationGap: String?,
        val memory: VexLifeMemoryContract.Context?,
        val memoryGap: String?,
        val working: WorkingDirectory?,
        val workingGap: String?,
        val promptResidentEventRefs: Set<String>,
        val now: Instant,
    )

    /** The current working checkpoint as references; its meaning stays with its owners. */
    data class WorkingDirectory(
        val checkpointRef: String,
        val disposition: String,
        val cursorEventRef: String,
        val openLoopRefs: List<String>,
        val currentStatementRefs: List<String>,
        val activeWorkNodeRefs: List<String>,
        val currentIntentReceiptRefs: List<String>,
    )

    data class Match(val queryRef: String, val route: Route, val detail: String)

    data class Result(
        val state: State,
        val text: String,
        /** Exact source returned with its words: event refs, then Memory statement refs. */
        val returnedSourceRefs: List<String>,
        /** Matched turns already present in this prompt, named without repeating them. */
        val residentSourceRefs: List<String>,
        val frontierRefs: List<String>,
        val scannedTurnCount: Int,
        val searchedMemoryPageCount: Int,
        /** Distinct turns and pages resolved: each once, however many routes reached it. */
        val resolvedNodeCount: Int,
        val matchRouteCount: Int,
        val effectsPerformed: Boolean = false,
    )

    fun parse(arguments: JsonObject): Parsed {
        val scopes =
            when (val value = arguments["scopes"]) {
                null -> Scope.entries.toSet()
                else ->
                    strings(value)
                        .map {
                            enumOrNull<Scope>(it)
                                ?: return Parsed.Refused(
                                    "unknown scope \"$it\"; scopes are " +
                                        Scope.entries.joinToString(", ")
                                )
                        }
                        .toSet()
                        .ifEmpty { Scope.entries.toSet() }
            }
        val order =
            arguments.string("order")?.let {
                enumOrNull<Order>(it)
                    ?: return Parsed.Refused("order is CURRENT_FIRST or CHRONOLOGICAL")
            } ?: Order.CURRENT_FIRST
        val maxTurns = arguments.int("maxTurns") ?: DefaultMaximumTurns
        val maxTokens = arguments.int("maxTokens") ?: DefaultMaximumTokens
        if (maxTurns !in 1..MaximumTurns) return Parsed.Refused("maxTurns is 1-$MaximumTurns")
        if (maxTokens !in 500..MaximumTokens)
            return Parsed.Refused("maxTokens is 500-$MaximumTokens")
        val sharedWindow =
            when (val parsed = window(arguments["window"])) {
                is WindowParse.Bad -> return Parsed.Refused(parsed.reason)
                is WindowParse.Good -> parsed.window
            }

        val rawQueries =
            when (val value = arguments["queries"]) {
                is JsonArray -> value.map { it as? JsonObject ?: return badQuery() }
                null ->
                    if (arguments.containsKey("terms") || arguments.containsKey("seedRefs"))
                        listOf(arguments)
                    else return badQuery()
                else -> return badQuery()
            }
        if (rawQueries.size !in 1..MaximumQueries) {
            return Parsed.Refused("give 1-$MaximumQueries queries")
        }
        val queries = rawQueries.mapIndexed { index, raw ->
            val terms =
                strings(raw["terms"])
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .distinctBy { normalize(it) }
            val seeds = strings(raw["seedRefs"]).map { it.trim() }.filter { it.isNotEmpty() }
            val speaker =
                raw.string("speaker")?.let {
                    enumOrNull<Speaker>(it) ?: return Parsed.Refused("speaker is ANY, HUMAN or VEX")
                } ?: Speaker.ANY
            val ownWindow =
                when (val parsed = window(raw["window"])) {
                    is WindowParse.Bad -> return Parsed.Refused(parsed.reason)
                    is WindowParse.Good -> parsed.window
                }
            val corrections = (raw["corrections"] as? JsonPrimitive)?.booleanOrNull == true
            val from = raw.int("from")
            if (
                terms.size > MaximumTermsPerQuery || terms.any { it.length > MaximumTermCharacters }
            ) {
                return Parsed.Refused(
                    "each query takes up to $MaximumTermsPerQuery terms of up to " +
                        "$MaximumTermCharacters characters"
                )
            }
            if (seeds.size > MaximumSeedRefs) {
                return Parsed.Refused("each query takes up to $MaximumSeedRefs seedRefs")
            }
            if (from != null && (from < 0 || seeds.isEmpty())) {
                return Parsed.Refused("\"from\" is a character offset and needs seedRefs")
            }
            val window = ownWindow ?: sharedWindow
            if (terms.isEmpty() && seeds.isEmpty() && window == null && !corrections) {
                return Parsed.Refused(
                    "a query needs terms, seedRefs, a window or \"corrections\": true"
                )
            }
            Query(
                ref = raw.string("ref")?.take(40) ?: "q${index + 1}",
                terms = terms,
                seedRefs = seeds,
                speaker = speaker,
                window = window,
                corrections = corrections,
                from = from,
            )
        }
        if (queries.map { it.ref }.distinct().size != queries.size) {
            return Parsed.Refused("query refs must be distinct")
        }
        return Parsed.Admitted(Request(queries, scopes, order, maxTurns, maxTokens))
    }

    fun revisit(request: Request, sources: Sources, estimateTokens: (String) -> Int): Result {
        val conversationInScope = Scope.CONVERSATION in request.scopes
        val turns =
            if (conversationInScope && sources.conversation != null)
                VexLifeTurnNavigation.project(sources.conversation.canonicalEvents)
            else emptyList()
        val turnByRef = turns.associateBy { it.turnRef }
        val turnByEventRef =
            turns.flatMap { listOf(it.request.eventRef to it, it.response.eventRef to it) }.toMap()
        val pages =
            if (Scope.MEMORY in request.scopes) sources.memory?.pages.orEmpty() else emptyList()
        val pageByStatement = pages.associateBy { it.statementRef }
        val directory = if (Scope.WORKING_CONTINUITY in request.scopes) sources.working else null

        // One node per turn / page; every route that reached it is kept on that one node.
        val turnMatches = linkedMapOf<String, MutableList<Match>>()
        val pageMatches = linkedMapOf<String, MutableList<Match>>()
        val matchedTerms = mutableMapOf<String, MutableSet<String>>()
        val cursors = mutableMapOf<String, Int>()
        val unresolvedSeeds = mutableListOf<String>()
        val emptyQueries = mutableListOf<String>()

        fun hitTurn(turn: VexLifeTurnNavigation.Turn, match: Match) {
            turnMatches.getOrPut(turn.turnRef) { mutableListOf() } += match
        }

        for (query in request.queries) {
            val before = turnMatches.values.sumOf { it.size } + pageMatches.values.sumOf { it.size }
            val inWindow = windowPredicate(query.window, turns, sources.now)
            val normalizedTerms = query.terms.map(::normalize)

            if (query.terms.isNotEmpty()) {
                turns.filter(inWindow).forEach { turn ->
                    val searchable =
                        normalize(
                            when (query.speaker) {
                                Speaker.HUMAN -> turn.request.content
                                Speaker.VEX -> turn.response.content
                                Speaker.ANY -> turn.request.content + "\n" + turn.response.content
                            }
                        )
                    val hit =
                        query.terms.filterIndexed { i, _ ->
                            searchable.contains(normalizedTerms[i])
                        }
                    if (hit.isNotEmpty()) {
                        hitTurn(turn, Match(query.ref, Route.TERMS, hit.joinToString(", ")))
                        matchedTerms.getOrPut(turn.turnRef) { mutableSetOf() } +=
                            hit.map(::normalize)
                    }
                }
                pages.forEach { page ->
                    val summary = normalize(page.summary)
                    val hit =
                        query.terms.filterIndexed { i, _ -> summary.contains(normalizedTerms[i]) }
                    if (hit.isNotEmpty()) {
                        pageMatches.getOrPut(page.statementRef) { mutableListOf() } +=
                            Match(query.ref, Route.TERMS, hit.joinToString(", "))
                    }
                }
            }

            query.seedRefs.forEach { seed ->
                val turn = turnByRef[seed] ?: turnByEventRef[seed]
                when {
                    turn != null -> {
                        hitTurn(turn, Match(query.ref, Route.SEED, seed))
                        if (query.from != null && seed in turnByEventRef) cursors[seed] = query.from
                    }
                    seed in pageByStatement ->
                        pageMatches.getOrPut(seed) { mutableListOf() } +=
                            Match(query.ref, Route.SEED, seed)
                    else -> unresolvedSeeds += seed
                }
            }

            if (query.corrections) {
                turns
                    .filter(inWindow)
                    .filter { it.correctionCue != null }
                    .forEach { turn ->
                        hitTurn(turn, Match(query.ref, Route.CORRECTIONS, turn.correctionCue!!))
                    }
            }

            if (query.terms.isEmpty() && query.seedRefs.isEmpty() && !query.corrections) {
                turns.filter(inWindow).forEach {
                    hitTurn(it, Match(query.ref, Route.WINDOW, "window"))
                }
            }

            val after = turnMatches.values.sumOf { it.size } + pageMatches.values.sumOf { it.size }
            if (after == before) emptyQueries += query.ref
        }

        // One typed step outward from what was located. Expansion never locates on its own.
        val located = turnMatches.keys.mapNotNull { turnByRef[it] }
        located.forEach { turn ->
            val queryRef = turnMatches.getValue(turn.turnRef).first().queryRef
            turn.correctsOrdinal?.let { ordinal ->
                hitTurn(turns[ordinal], Match(queryRef, Route.CORRECTED_BY, turn.turnRef))
            }
            turns
                .getOrNull(turn.ordinal + 1)
                ?.takeIf { it.correctionCue != null }
                ?.let { hitTurn(it, Match(queryRef, Route.CORRECTS, turn.turnRef)) }
            if (turn.pointsBack && turn.ordinal > 0) {
                hitTurn(
                    turns[turn.ordinal - 1],
                    Match(queryRef, Route.POINTED_BACK_FROM, turn.turnRef),
                )
            }
        }

        val directRoutes = setOf(Route.TERMS, Route.SEED, Route.WINDOW, Route.CORRECTIONS)
        val nodes = turnMatches.keys.map { turnByRef.getValue(it) }
        fun branches(turn: VexLifeTurnNavigation.Turn) =
            turnMatches
                .getValue(turn.turnRef)
                .filter { it.route in directRoutes }
                .map { it.queryRef }
                .distinct()
                .size
        fun correctionLinked(turn: VexLifeTurnNavigation.Turn) =
            turnMatches.getValue(turn.turnRef).any {
                it.route == Route.CORRECTS ||
                    it.route == Route.CORRECTED_BY ||
                    it.route == Route.CORRECTIONS
            }
        val ranked =
            nodes.sortedWith(
                compareByDescending<VexLifeTurnNavigation.Turn> { branches(it) }
                    .thenByDescending { matchedTerms[it.turnRef]?.size ?: 0 }
                    .thenByDescending { it.request.sequence }
            )

        // Budget: resident turns are named, others get exact words until turns or tokens run out.
        val resident = mutableListOf<VexLifeTurnNavigation.Turn>()
        val chosen = mutableListOf<Rendered>()
        val frontier = mutableListOf<String>()
        var tokens = estimateTokens(Header) + 200
        var truncated = false
        val lastOrdinal = turns.lastOrNull()?.ordinal ?: 0
        for (turn in ranked) {
            if (
                turn.request.eventRef in sources.promptResidentEventRefs &&
                    turn.response.eventRef in sources.promptResidentEventRefs
            ) {
                resident += turn
                continue
            }
            if (chosen.size >= request.maxTurns) {
                frontier += turn.turnRef
                continue
            }
            val remainingCharacters = maxOf(0, (request.maxTokens - tokens) * 3)
            val excerpt = minOf(ExcerptCharacters, remainingCharacters / 2)
            if (excerpt < 200) {
                frontier += turn.turnRef
                continue
            }
            val rendered =
                renderTurn(
                    turn = turn,
                    matches = turnMatches.getValue(turn.turnRef),
                    terms = matchedTerms[turn.turnRef].orEmpty(),
                    excerptCharacters = excerpt,
                    cursors = cursors,
                    turnsAgo = lastOrdinal - turn.ordinal,
                    now = sources.now,
                )
            val cost = estimateTokens(rendered.text)
            if (tokens + cost > request.maxTokens && chosen.isNotEmpty()) {
                frontier += turn.turnRef
                continue
            }
            chosen += rendered
            tokens += cost
            truncated = truncated || rendered.cut
        }

        // Later related: an older returned turn that shares matched words with a newer node.
        val laterByTurn = chosen.associate { rendered ->
            val words = matchedTerms[rendered.turn.turnRef].orEmpty()
            rendered.turn.turnRef to
                nodes
                    .filter {
                        it.request.sequence > rendered.turn.request.sequence &&
                            words.isNotEmpty() &&
                            matchedTerms[it.turnRef].orEmpty().any(words::contains)
                    }
                    .sortedByDescending { it.request.sequence }
                    .map { it.turnRef }
                    .take(3)
        }

        val pageLines = mutableListOf<String>()
        val returnedPages = mutableListOf<String>()
        pageMatches.forEach { (statementRef, matches) ->
            val page = pageByStatement.getValue(statementRef)
            val line =
                "· ${page.statementRef} [${page.effectiveState}; ${page.memoryRelation}] " +
                    "matched: ${routesLine(matches)}\n  ${page.summary}"
            val cost = estimateTokens(line)
            if (tokens + cost > request.maxTokens) {
                frontier += statementRef
            } else {
                pageLines += line
                returnedPages += statementRef
                tokens += cost
            }
        }

        val (corrections, matched) = chosen.partition { correctionLinked(it.turn) }
        val orderedMatched =
            when (request.order) {
                Order.CURRENT_FIRST -> matched
                Order.CHRONOLOGICAL -> matched.sortedBy { it.turn.request.sequence }
            }
        val anyFound = nodes.isNotEmpty() || pageMatches.isNotEmpty()
        val state =
            when {
                !anyFound -> State.NOT_FOUND
                frontier.isNotEmpty() || truncated -> State.PARTIAL
                else -> State.RETURNED
            }

        val text = buildString {
            appendLine(Header)
            appendLine(
                "state: $state · " +
                    coverageLine(request, sources, turns.size, pages.size, conversationInScope)
            )
            gapsLine(request, sources)?.let(::appendLine)
            directory?.let { appendLine(directoryLine(it, turnByEventRef)) }
            if (resident.isNotEmpty()) {
                appendLine(
                    "Already in this prompt, not repeated: " +
                        resident.sortedBy { it.request.sequence }.joinToString(", ") { it.turnRef }
                )
            }
            if (corrections.isNotEmpty()) {
                appendLine("Corrections (the person's own words; each earlier reply is unchanged):")
                corrections.sortedBy { it.turn.request.sequence }.forEach { appendLine(it.text) }
            }
            if (orderedMatched.isNotEmpty()) {
                appendLine(
                    if (request.order == Order.CURRENT_FIRST) "Turns, strongest and newest first:"
                    else "Turns, in Conversation order:"
                )
                orderedMatched.forEach { rendered ->
                    appendLine(rendered.text)
                    laterByTurn[rendered.turn.turnRef]
                        ?.takeIf { it.isNotEmpty() }
                        ?.let {
                            appendLine("  later turns share these words: ${it.joinToString(", ")}")
                        }
                }
            }
            if (pageLines.isNotEmpty()) {
                appendLine(
                    "Memory pages (accepted meaning and its state, as the Memory owner projects it):"
                )
                pageLines.forEach(::appendLine)
            }
            if (frontier.isNotEmpty()) {
                val listed = frontier.take(MaximumFrontierRefsListed)
                appendLine(
                    "Not returned for room: ${frontier.size} more — ${listed.joinToString(", ")}" +
                        (if (frontier.size > listed.size) ", …" else "") +
                        ". To read them: {\"queries\":[{\"seedRefs\":[\"${listed.first()}\"]}]}"
                )
            }
            if (unresolvedSeeds.isNotEmpty()) {
                appendLine(
                    "Seed refs not found in what was searched: ${unresolvedSeeds.joinToString(", ")}"
                )
            }
            emptyQueries.forEach {
                appendLine("Query \"$it\" found nothing in what was searched.")
            }
            if (!anyFound || emptyQueries.isNotEmpty() || unresolvedSeeds.isNotEmpty()) {
                appendLine(
                    "Not found here is not the same as never happened. Words are matched exactly, so " +
                        "another wording can find what this one missed; and the person may remember " +
                        "what is not reachable here and can share it."
                )
            }
        }

        return Result(
            state = state,
            text = text.trimEnd(),
            returnedSourceRefs =
                chosen.flatMap { listOf(it.turn.request.eventRef, it.turn.response.eventRef) } +
                    returnedPages,
            residentSourceRefs =
                resident.flatMap { listOf(it.request.eventRef, it.response.eventRef) },
            frontierRefs = frontier,
            scannedTurnCount = turns.size,
            searchedMemoryPageCount = pages.size,
            resolvedNodeCount = turnMatches.size + pageMatches.size,
            matchRouteCount =
                turnMatches.values.sumOf { it.size } + pageMatches.values.sumOf { it.size },
        )
    }

    private data class Rendered(
        val turn: VexLifeTurnNavigation.Turn,
        val text: String,
        val cut: Boolean,
    )

    private fun renderTurn(
        turn: VexLifeTurnNavigation.Turn,
        matches: List<Match>,
        terms: Set<String>,
        excerptCharacters: Int,
        cursors: Map<String, Int>,
        turnsAgo: Int,
        now: Instant,
    ): Rendered {
        var cut = false
        fun side(event: DirectConversationStore.Event): String {
            val content = event.content
            val start =
                cursors[event.eventRef]
                    ?: terms
                        .mapNotNull { term -> normalizedIndex(content, term) }
                        .minOrNull()
                        ?.let { maxOf(0, it - excerptCharacters / 4) }
                    ?: 0
            if (start == 0 && content.length <= excerptCharacters) return content
            val from = minOf(start, content.length)
            val to = minOf(content.length, from + excerptCharacters)
            if (from > 0 || to < content.length) cut = true
            return content.substring(from, to) +
                " [exact excerpt, characters $from-$to of ${content.length}" +
                (if (to < content.length)
                    "; to read on: {\"queries\":[{\"seedRefs\":[\"${event.eventRef}\"],\"from\":$to}]}"
                else "") +
                "]"
        }
        val relation =
            matches
                .mapNotNull { match ->
                    when (match.route) {
                        Route.CORRECTED_BY ->
                            "the person's next message corrects this reply (${match.detail})"
                        Route.CORRECTS ->
                            "this message corrects the reply in ${match.detail}" +
                                (turn.correctionCue?.let { " — it opens \"$it\"" } ?: "")
                        Route.CORRECTIONS ->
                            "opens like a correction (\"${match.detail}\") of the reply just before it"
                        Route.POINTED_BACK_FROM -> "${match.detail} points back to this turn"
                        else -> null
                    }
                }
                .distinct()
        val text = buildString {
            append(
                "· ${turn.turnRef} — ${minute(turn.requestedAt)} (${ago(turn.requestedAt, now, turnsAgo)})"
            )
            append(" — matched: ").append(routesLine(matches)).append('\n')
            append("  person: ").append(side(turn.request)).append('\n')
            append("  Vex: ").append(side(turn.response))
            relation.forEach { append("\n  ").append(it) }
        }
        return Rendered(turn, text, cut)
    }

    private fun routesLine(matches: List<Match>): String =
        matches
            .groupBy { it.queryRef }
            .entries
            .joinToString("; ") { (query, routes) ->
                query +
                    "[" +
                    routes.map { it.route.name.lowercase() }.distinct().joinToString("+") +
                    "]"
            }

    private fun coverageLine(
        request: Request,
        sources: Sources,
        turnCount: Int,
        pageCount: Int,
        conversationInScope: Boolean,
    ): String =
        listOfNotNull(
                if (conversationInScope && sources.conversation != null)
                    "searched $turnCount completed turns of this thread (exact words; no meaning index yet)"
                else null,
                if (Scope.MEMORY in request.scopes && sources.memory != null)
                    "$pageCount Memory pages (projection ${sources.memory.projectionState})"
                else null,
                if (Scope.WORKING_CONTINUITY in request.scopes && sources.working != null)
                    "the current working checkpoint"
                else null,
            )
            .ifEmpty { listOf("nothing could be searched") }
            .joinToString(", ")

    private fun gapsLine(request: Request, sources: Sources): String? =
        listOfNotNull(
                sources.conversationGap
                    ?.takeIf {
                        Scope.CONVERSATION in request.scopes && sources.conversation == null
                    }
                    ?.let { "Conversation — $it" },
                sources.memoryGap
                    ?.takeIf { Scope.MEMORY in request.scopes && sources.memory == null }
                    ?.let { "Memory — $it" },
                sources.workingGap
                    ?.takeIf {
                        Scope.WORKING_CONTINUITY in request.scopes && sources.working == null
                    }
                    ?.let { "working continuity — $it" },
            )
            .takeIf { it.isNotEmpty() }
            ?.joinToString("; ", prefix = "Not reachable now: ")

    private fun directoryLine(
        directory: WorkingDirectory,
        turnByEventRef: Map<String, VexLifeTurnNavigation.Turn>,
    ): String = buildString {
        append(
            "Where the work stands — working checkpoint ${directory.checkpointRef} (${directory.disposition}"
        )
        turnByEventRef[directory.cursorEventRef]?.let { append(", formed at ${it.turnRef}") }
        append("); references only, their meaning lives with their owners:")
        listOf(
                "open loops" to directory.openLoopRefs,
                "current statements" to directory.currentStatementRefs,
                "active work" to directory.activeWorkNodeRefs,
                "current intents" to directory.currentIntentReceiptRefs,
            )
            .filter { it.second.isNotEmpty() }
            .ifEmpty { listOf("refs" to listOf("none recorded")) }
            .forEach { (label, refs) -> append("\n  $label: ").append(refs.joinToString(", ")) }
    }

    private fun windowPredicate(
        window: Window?,
        turns: List<VexLifeTurnNavigation.Turn>,
        now: Instant,
    ): (VexLifeTurnNavigation.Turn) -> Boolean {
        if (window == null) return { true }
        val byTime = window.lastHours?.let { now.minus(Duration.ofHours(it.toLong())) }
        val byTurns = window.lastTurns?.let { maxOf(0, turns.size - it) }
        return { turn ->
            (byTime != null && !turn.respondedAt.isBefore(byTime)) ||
                (byTurns != null && turn.ordinal >= byTurns)
        }
    }

    private sealed interface WindowParse {
        data class Good(val window: Window?) : WindowParse

        data class Bad(val reason: String) : WindowParse
    }

    private fun window(value: kotlinx.serialization.json.JsonElement?): WindowParse {
        if (value == null) return WindowParse.Good(null)
        val obj =
            value as? JsonObject
                ?: return WindowParse.Bad("window is {\"lastHours\":5,\"lastTurns\":20}")
        val hours = obj.int("lastHours")
        val count = obj.int("lastTurns")
        if (hours == null && count == null)
            return WindowParse.Bad("window needs lastHours, lastTurns or both")
        if (hours != null && hours !in 1..MaximumWindowHours)
            return WindowParse.Bad("lastHours is 1-$MaximumWindowHours")
        if (count != null && count !in 1..MaximumWindowTurns)
            return WindowParse.Bad("lastTurns is 1-$MaximumWindowTurns")
        return WindowParse.Good(Window(hours, count))
    }

    private fun badQuery(): Parsed =
        Parsed.Refused(
            "queries is a list such as [{\"terms\":[\"launch date\"]},{\"terms\":[\"ship day\"]}]"
        )

    private fun normalize(value: String): String =
        value.lowercase().replace('’', '\'').replace(Regex("\\s+"), " ").trim()

    private fun normalizedIndex(content: String, normalizedTerm: String): Int? =
        Regex(Regex.escape(normalizedTerm).replace(" ", "\\E\\s+\\Q"), RegexOption.IGNORE_CASE)
            .find(content.replace('’', '\''))
            ?.range
            ?.first

    private fun minute(instant: Instant): String = MinuteFormat.format(instant)

    private val MinuteFormat: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneOffset.UTC)

    private fun ago(at: Instant, now: Instant, turnsAgo: Int): String {
        val elapsed = Duration.between(at, now)
        val time =
            when {
                elapsed.isNegative || elapsed.toMinutes() < 2 -> "just now"
                elapsed.toHours() < 2 -> "${elapsed.toMinutes()} minutes ago"
                elapsed.toDays() < 2 -> "${elapsed.toHours()} hours ago"
                else -> "${elapsed.toDays()} days ago"
            }
        val count =
            when (turnsAgo) {
                0 -> "the latest turn"
                1 -> "1 turn back"
                else -> "$turnsAgo turns back"
            }
        return "$time, $count"
    }

    private fun strings(value: kotlinx.serialization.json.JsonElement?): List<String> =
        when (value) {
            is JsonArray ->
                value.mapNotNull {
                    (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.contentOrNull
                }
            is JsonPrimitive -> listOfNotNull(value.takeIf { it.isString }?.contentOrNull)
            else -> emptyList()
        }

    private fun JsonObject.string(name: String): String? =
        (this[name] as? JsonPrimitive)
            ?.takeIf { it.isString }
            ?.contentOrNull
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    private fun JsonObject.int(name: String): Int? = (this[name] as? JsonPrimitive)?.intOrNull

    private inline fun <reified E : Enum<E>> enumOrNull(value: String): E? =
        enumValues<E>().firstOrNull { it.name == value.trim().uppercase() }
}

// [VXG RealForever]
