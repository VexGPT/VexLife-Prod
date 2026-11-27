package vexlife.continuity

import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonObject
import vexlife.continuity.ContinuityTurnFixture.Line
import vexlife.continuity.VexLifeContextRevisit.Parsed
import vexlife.continuity.VexLifeContextRevisit.State
import vexlife.contract.VexContractJson
import vexlife.memory.VexLifeMemoryContextFixture

/**
 * The required continuity shapes for one-call revisit, proven over a real canonical Home.
 *
 * Each test names the human situation it protects. Owners are read once by the test the way the
 * bridge reads them, then the pure revisit runs over what they said.
 */
class VexLifeContextRevisitTest {
    private val now = Instant.parse("2026-11-16T20:00:00Z")

    private fun minutesAgo(minutes: Long) = now.minus(Duration.ofMinutes(minutes))

    private fun estimate(text: String) = (text.toByteArray(Charsets.UTF_8).size + 3) / 4 + 4

    private fun request(json: String): VexLifeContextRevisit.Request =
        assertIs<Parsed.Admitted>(
                VexLifeContextRevisit.parse(VexContractJson.decode<JsonObject>(json))
            )
            .request

    private fun revisit(
        fixture: ContinuityTurnFixture,
        json: String,
        resident: Set<String> = emptySet(),
        working: VexLifeContextRevisit.WorkingDirectory? = null,
    ): VexLifeContextRevisit.Result =
        VexLifeContextRevisit.revisit(
            request = request(json),
            sources =
                VexLifeContextRevisit.Sources(
                    conversation = fixture.store.current(),
                    conversationGap = null,
                    memory = null,
                    memoryGap = "no Memory in this fixture",
                    working = working,
                    workingGap = if (working == null) "no working checkpoint exists yet" else null,
                    promptResidentEventRefs = resident,
                    now = now,
                ),
            estimateTokens = ::estimate,
        )

    @Test
    fun aDifferentWordingInTheSameRequestFindsWhatTheFirstWordingMissed() {
        ContinuityTurnFixture().use { fixture ->
            val (ship, _) =
                fixture.commit(
                    Line(
                        "We will ship the beta on Friday.",
                        "Friday it is for the beta.",
                        minutesAgo(300),
                    ),
                    Line(
                        "Unrelated: how is the garden?",
                        "The tomatoes are doing well.",
                        minutesAgo(200),
                    ),
                )
            val result =
                revisit(
                    fixture,
                    """{"queries":[{"ref":"launch","terms":["launch date"]},{"ref":"ship","terms":["ship"]}]}""",
                )

            assertEquals(State.RETURNED, result.state)
            assertTrue(ship.first.eventRef in result.returnedSourceRefs)
            assertTrue(result.text.contains("We will ship the beta on Friday."))
            assertTrue(result.text.contains("Query \"launch\" found nothing in what was searched."))
            assertTrue(result.text.contains("Exact lexical words are matched"))
        }
    }

    @Test
    fun nothingFoundIsReportedAsBoundedUncertaintyAndInvitesThePersonToShare() {
        ContinuityTurnFixture().use { fixture ->
            fixture.commit(Line("Hello again.", "Hello. Good to see you.", minutesAgo(30)))
            val result = revisit(fixture, """{"queries":[{"terms":["passport renewal"]}]}""")

            assertEquals(State.NOT_FOUND, result.state)
            assertTrue(result.returnedSourceRefs.isEmpty())
            assertTrue(result.text.contains("searched 1 completed turns of this thread"))
            assertTrue(result.text.contains("Not found here is not the same as never happened"))
            assertTrue(result.text.contains("can share it"))
            assertTrue(
                result.text.contains("Not reachable now: Memory — no Memory in this fixture")
            )
        }
    }

    @Test
    fun anEarlierStatementAndItsLaterCorrectionAreBothReachableAndTheOriginalIsUnchanged() {
        ContinuityTurnFixture().use { fixture ->
            val (friday, correction) =
                fixture.commit(
                    Line(
                        "Let's plan the launch for Friday.",
                        "Launch noted for Friday.",
                        minutesAgo(90),
                    ),
                    Line(
                        "Actually, the launch moved to Monday.",
                        "Understood: the launch is Monday now.",
                        minutesAgo(60),
                    ),
                )
            val result = revisit(fixture, """{"queries":[{"terms":["launch"]}]}""")

            assertEquals(State.RETURNED, result.state)
            assertTrue(friday.second.eventRef in result.returnedSourceRefs)
            assertTrue(correction.first.eventRef in result.returnedSourceRefs)
            assertTrue(
                result.text.contains(
                    "Corrections (the person's own words; each earlier reply is unchanged)"
                )
            )
            assertTrue(result.text.contains("Launch noted for Friday."))
            assertTrue(
                result.text.contains(
                    "the person's next message corrects this reply (${correction.first.turnRef})"
                )
            )
            assertTrue(result.text.contains("it opens \"actually\""))
            // Correction is relationship, not erasure: the original reply's bytes are the store's.
            val stored =
                fixture.store.current().canonicalEvents.single {
                    it.eventRef == friday.second.eventRef
                }
            assertEquals(friday.second.contentHash, stored.contentHash)
            assertTrue(
                result.text.indexOf("Let's plan the launch for Friday.") <
                    result.text.indexOf("Actually, the launch moved to Monday."),
                "a correction chain reads in causal order",
            )
        }
    }

    @Test
    fun workingContinuityNamesWhatMattersMemorySaysWhatItIsAndWhatIsInViewIsNotRepeated() {
        ContinuityTurnFixture().use { fixture ->
            fixture.commit(Line("Lamp?", "Fixed.", minutesAgo(5)))
            val base = VexLifeMemoryContextFixture.current()
            val page = base.pages.single()
            val memory =
                base.copy(
                    pages =
                        listOf(
                            page,
                            page.copy(
                                pageRef = "page.living-journal.memory.002",
                                statementRef = "statement.score.continuity.002",
                                summary = "The parent objective is memory depth.",
                            ),
                            page.copy(
                                pageRef = "page.living-journal.memory.003",
                                statementRef = "statement.score.continuity.003",
                                summary = "Budgets bound memory depth.",
                            ),
                        )
                )
            val directory =
                VexLifeContextRevisit.WorkingDirectory(
                    checkpointRef = null,
                    disposition = null,
                    cursorEventRef = null,
                    openLoopRefs = emptyList(),
                    currentStatementRefs =
                        listOf("statement.score.continuity.001", "statement.score.gone.009"),
                    activeWorkNodeRefs = emptyList(),
                    currentIntentReceiptRefs = emptyList(),
                )
            val result =
                VexLifeContextRevisit.revisit(
                    request = request("""{"queries":[["memory depth"]]}"""),
                    sources =
                        VexLifeContextRevisit.Sources(
                            conversation = fixture.store.current(),
                            conversationGap = null,
                            memory = memory,
                            memoryGap = null,
                            working = directory,
                            workingGap = null,
                            promptResidentEventRefs = emptySet(),
                            promptResidentStatementRefs = setOf("statement.score.continuity.002"),
                            now = now,
                        ),
                    estimateTokens = ::estimate,
                )

            // Working Continuity says it matters now; current Memory says what it is.
            assertTrue("statement.score.continuity.001" in result.returnedSourceRefs)
            assertTrue(result.text.contains("Victor prefers exact source-grounded continuation."))
            assertTrue(result.text.contains("working[working_anchor]"))
            // A query hit not already in view comes back.
            assertTrue("statement.score.continuity.003" in result.returnedSourceRefs)
            // Memory already in the prompt is named, not repeated.
            assertFalse(result.text.contains("The parent objective is memory depth."))
            assertTrue(
                result.text.contains(
                    "Memory already in this prompt, not repeated: statement.score.continuity.002"
                )
            )
            // A statement Memory does not currently project stays an explicit, unresolved ref.
            assertTrue("statement.score.gone.009" in result.frontierRefs)
            assertFalse("statement.score.gone.009" in result.returnedSourceRefs)
            assertTrue(
                result.text.contains(
                    "Named current by Working Continuity but not in current Memory, so not resolved here: statement.score.gone.009"
                )
            )
        }
    }

    @Test
    fun aResolvedTangentLeavesTheDirectoryButARevisitReopensItWhileTheParentStaysInView() {
        ContinuityTurnFixture().use { fixture ->
            val turns =
                fixture.commit(
                    Line(
                        "Our objective: design the memory depth hydration.",
                        "Agreed, that is the parent objective.",
                        minutesAgo(240),
                    ),
                    Line(
                        "Quick tangent: what is a Bloom filter?",
                        "A compact set-membership sketch; tangent resolved.",
                        minutesAgo(230),
                    ),
                    Line(
                        "Back to hydration: what about budgets?",
                        "Budgets bound depth, nodes and tokens.",
                        minutesAgo(10),
                    ),
                )
            val directory =
                VexLifeContextRevisit.WorkingDirectory(
                    checkpointRef = "checkpoint.vexlife-prod.working.test",
                    disposition = "ROLLOVER_COMMITTED",
                    cursorEventRef = turns.last().first.eventRef,
                    openLoopRefs = listOf("loop.memory-depth.hydration"),
                    currentStatementRefs = emptyList(),
                    activeWorkNodeRefs = listOf("work.memory-depth.parent-objective"),
                    currentIntentReceiptRefs = emptyList(),
                )
            val resident = setOf(turns.last().first.eventRef, turns.last().second.eventRef)
            val result =
                revisit(
                    fixture,
                    """{"queries":[{"terms":["Bloom filter"]},{"terms":["budgets"]}]}""",
                    resident = resident,
                    working = directory,
                )

            assertTrue(
                result.text.contains(
                    "Where the work stands — this turn's working position; last working checkpoint checkpoint.vexlife-prod.working.test (ROLLOVER_COMMITTED, formed at ${turns.last().first.turnRef})"
                )
            )
            assertTrue(result.text.contains("open loops: loop.memory-depth.hydration"))
            assertTrue(result.text.contains("A compact set-membership sketch; tangent resolved."))
            assertTrue(
                result.text.contains(
                    "Already in this prompt, not repeated: ${turns.last().first.turnRef}"
                )
            )
            assertFalse(result.text.contains("Budgets bound depth, nodes and tokens."))
            assertEquals(resident.sorted(), result.residentSourceRefs.sorted())
        }
    }

    @Test
    fun threeRoutesToTheSameTurnResolveItOnceAndKeepEveryRoute() {
        ContinuityTurnFixture().use { fixture ->
            val (yellow, _) =
                fixture.commit(
                    Line(
                        "The yellow Ferrari taxis in Chongqing were part of that story.",
                        "I remember the Chongqing yellow Ferrari reference.",
                        minutesAgo(500),
                    ),
                    Line("Different subject entirely.", "Noted.", minutesAgo(400)),
                )
            val result =
                revisit(
                    fixture,
                    """{"queries":[{"ref":"city","terms":["Chongqing"]},{"ref":"car","terms":["yellow Ferrari"]},""" +
                        """{"ref":"seed","seedRefs":["${yellow.first.turnRef}"]}]}""",
                )

            assertEquals(1, result.resolvedNodeCount)
            assertEquals(3, result.matchRouteCount)
            assertEquals(
                listOf(yellow.first.eventRef, yellow.second.eventRef),
                result.returnedSourceRefs,
            )
            assertEquals(
                1,
                Regex(Regex.escape("The yellow Ferrari taxis in Chongqing"))
                    .findAll(result.text)
                    .count(),
                "the exact source is returned once",
            )
            assertTrue(result.text.contains("matched: city[terms]; car[terms]; seed[seed]"))
        }
    }

    @Test
    fun whatDoesNotFitIsNamedAsAFrontierWithTheExactWayToAskForIt() {
        ContinuityTurnFixture().use { fixture ->
            fixture.commit(
                *(1..10)
                    .map {
                        Line(
                            "Budget note $it: keep the budget visible.",
                            "Budget point $it recorded.",
                            minutesAgo(100L - it),
                        )
                    }
                    .toTypedArray()
            )
            val result = revisit(fixture, """{"queries":[{"terms":["budget"]}],"maxTurns":3}""")

            assertEquals(State.PARTIAL, result.state)
            assertEquals(6, result.returnedSourceRefs.size)
            assertEquals(7, result.frontierRefs.size)
            assertTrue(result.text.contains("Not returned for room: 7 more"))
            val next = result.frontierRefs.first()
            assertTrue(result.text.contains("""{"queries":[{"seedRefs":["$next"]}]}"""))

            val followed = revisit(fixture, """{"queries":[{"seedRefs":["$next"]}]}""")
            assertEquals(State.RETURNED, followed.state)
            assertEquals(2, followed.returnedSourceRefs.size)
            assertTrue(followed.text.contains("· $next —"))
        }
    }

    @Test
    fun aLongTurnComesBackAsAnExactExcerptWithACursorThatReadsOn() {
        ContinuityTurnFixture().use { fixture ->
            val long = (1..400).joinToString(" ") { "segment-$it" }
            val (turn) = fixture.commit(Line("Please keep this long note.", long, minutesAgo(50)))
            val first = revisit(fixture, """{"queries":[{"seedRefs":["${turn.first.turnRef}"]}]}""")

            assertEquals(State.PARTIAL, first.state)
            val cursor = Regex(""""from":(\d+)""").find(first.text)!!.groupValues[1].toInt()
            assertTrue(first.text.contains("exact excerpt, characters 0-$cursor of ${long.length}"))

            val next =
                revisit(
                    fixture,
                    """{"queries":[{"seedRefs":["${turn.second.eventRef}"],"from":$cursor}]}""",
                )
            assertTrue(next.text.contains(long.substring(cursor, cursor + 200)))
        }
    }

    @Test
    fun humanTimeAndTurnCountAreSeparateLensesJoinedAsAUnion() {
        ContinuityTurnFixture().use { fixture ->
            // Four deep turns over eight hours: sparse in turns, long in time.
            val sparse =
                fixture.commit(
                    Line("Deep turn one.", "One.", minutesAgo(480)),
                    Line("Deep turn two.", "Two.", minutesAgo(360)),
                    Line("Deep turn three.", "Three.", minutesAgo(240)),
                    Line("Deep turn four.", "Four.", minutesAgo(60)),
                )
            fun windowed(json: String) =
                revisit(fixture, """{"queries":[{"window":$json}],"maxTurns":12}""")
                    .returnedSourceRefs
                    .filter { ref -> sparse.any { it.first.eventRef == ref } }
                    .toSet()

            val byTime = windowed("""{"lastHours":5}""")
            assertEquals(setOf(sparse[2].first.eventRef, sparse[3].first.eventRef), byTime)

            val byTurns = windowed("""{"lastTurns":3}""")
            assertEquals(sparse.drop(1).map { it.first.eventRef }.toSet(), byTurns)

            // Union: whichever lens reaches further back wins.
            val union = windowed("""{"lastHours":5,"lastTurns":3}""")
            assertEquals(sparse.drop(1).map { it.first.eventRef }.toSet(), union)
            val wide = windowed("""{"lastHours":7,"lastTurns":1}""")
            assertEquals(sparse.drop(1).map { it.first.eventRef }.toSet(), wide)
        }
    }

    @Test
    fun aCorrectionOfVexsPerceptionComesBackInThePersonsWordsAndTheOriginalReplyStands() {
        ContinuityTurnFixture().use { fixture ->
            val (misread, correction) =
                fixture.commit(
                    Line(
                        "Why does the cache miss here?",
                        "You seem frustrated, so I will keep this short.",
                        minutesAgo(600),
                    ),
                    Line(
                        "I'm not frustrated. I want more exact causal detail.",
                        "Thank you for telling me. Here is the causal chain in detail.",
                        minutesAgo(598),
                    ),
                )
            fixture.commit(Line("Something else now.", "Sure.", minutesAgo(5)))
            val before = fixture.homeTree()

            val result = revisit(fixture, """{"queries":[{"ref":"lessons","corrections":true}]}""")

            assertEquals(fixture.homeTree(), before, "revisit writes nothing")
            assertTrue(result.text.contains("You seem frustrated, so I will keep this short."))
            assertTrue(result.text.contains("I'm not frustrated. I want more exact causal detail."))
            assertTrue(
                result.text.contains(
                    "opens like a correction (\"i'm not\") of the reply just before it"
                )
            )
            assertTrue(misread.second.eventRef in result.returnedSourceRefs)
            assertTrue(correction.first.eventRef in result.returnedSourceRefs)
            assertEquals("HOST_LEXICAL_CUE", VexLifeTurnNavigation.CueClass)
            assertFalse(result.effectsPerformed)
        }
    }

    @Test
    fun indexedConversationViewReturnsLexicalAndSeedNeighborhoodWithoutFullSnapshot() {
        ContinuityTurnFixture().use {
            fixture ->
            val turns =
                fixture.commit(
                    Line(
                        "Why does the cache miss here?",
                        "You seem frustrated, so I will keep this short.",
                        minutesAgo(90),
                    ),
                    Line(
                        "I'm not frustrated. I want exact causal detail.",
                        "Understood. Here is the exact chain.",
                        minutesAgo(85),
                    ),
                    Line(
                        "The sapphire seed is stored here.",
                        "Seed recorded.",
                        minutesAgo(80),
                    ),
                    Line(
                        "Unrelated trailing turn.",
                        "Trailing.",
                        minutesAgo(75),
                    ),
                )
            val backend =
                VexLifeContinuitySourceLocator.InMemoryBackend(
                    secret =
                        ByteArray(32) {
                            73
                        },
                    fingerprintBindingRef =
                        "fingerprint.test.revisit-neighborhood",
                )

            fixture.store.withWriterLease {
                val owner =
                    this
                VexLifeContinuitySourceLocator.rebuild(
                    conversationOwner =
                        owner,
                    backend =
                        backend,
                )
                var fullConversationReads =
                    0

                fun view(
                    revisitRequest: VexLifeContextRevisit.Request,
                    lifecycle: VexLifeContextRevisit.ConversationLifecycle?,
                ): VexLifeContextRevisit.ConversationView? {
                    if (
                        !VexLifeContextRevisit.supportsIndexedConversationView(
                            revisitRequest,
                        )
                    ) {
                        return null
                    }
                    val outcome =
                        VexLifeContinuitySourceLocator.locateNeighborhood(
                            conversationOwner =
                                owner,
                            queries =
                                revisitRequest.queries.map {
                                    query ->
                                    VexLifeContinuitySourceLocator.NeighborhoodQuery(
                                        queryTerms =
                                            query.terms,
                                        speakerScope =
                                            when (
                                                query.speaker
                                            ) {
                                                VexLifeContextRevisit.Speaker.ANY ->
                                                    VexLifeContinuitySourceLocator.SpeakerScope.ANY

                                                VexLifeContextRevisit.Speaker.HUMAN ->
                                                    VexLifeContinuitySourceLocator.SpeakerScope.HUMAN

                                                VexLifeContextRevisit.Speaker.VEX ->
                                                    VexLifeContinuitySourceLocator.SpeakerScope.VEX
                                            },
                                        seedRefs =
                                            query.seedRefs
                                                .toSet(),
                                        typedEdges =
                                            if (
                                                query.corrections
                                            ) {
                                                setOf(
                                                    VexLifeContinuitySourceLocator.TypedEdgeKind.CORRECTS_PREVIOUS,
                                                )
                                            } else {
                                                emptySet()
                                            },
                                    )
                                },
                            backend =
                                backend,
                            sourceLifecycleBindingRef =
                                lifecycle
                                    ?.bindingRef
                                    ?: VexLifeContinuitySourceLocator.UnboundSourceLifecycleBindingRef,
                        )
                    return when (
                        outcome
                    ) {
                        is VexLifeContinuitySourceLocator.NeighborhoodOutcome.Ready ->
                            VexLifeContextRevisit.ConversationView(
                                turns =
                                    outcome.turns,
                                totalCompletedTurnCount =
                                    outcome.totalCompletedTurnCount,
                                conversationEventsRead =
                                    outcome.exactEventFileReadCount,
                                exactHeadFileReadCount =
                                    outcome.exactHeadFileReadCount,
                                wholeHistoryHeadTraversalPerformed =
                                    outcome.wholeHistoryHeadTraversalPerformed,
                                derivedEntryScanCount =
                                    outcome.derivedEntryScanCount,
                                derivedPostingLookupCount =
                                    outcome.derivedPostingLookupCount,
                                derivedRefLookupCount =
                                    outcome.derivedRefLookupCount,
                                derivedTypedEdgeLookupCount =
                                    outcome.derivedTypedEdgeLookupCount,
                            )

                        is VexLifeContinuitySourceLocator.NeighborhoodOutcome.FallbackRequired ->
                            null
                    }
                }

                val turnSources =
                    VexLifeContextRevisit.TurnSources(
                        readConversation = {
                            fullConversationReads +=
                                1
                            error(
                                "indexed revisit must not request the full Conversation snapshot",
                            )
                        },
                        memory = {
                            null
                        },
                        readWorking = {
                            null
                        },
                        promptResidentEventRefs = {
                            emptySet()
                        },
                        promptResidentStatementRefs = {
                            emptySet()
                        },
                        now = {
                            now
                        },
                        readConversationLifecycle = {
                            VexLifeContextRevisit.ConversationLifecycle(
                                bindingRef =
                                    VexLifeContinuitySourceLocator.UnboundSourceLifecycleBindingRef,
                                blockedEventRefs =
                                    emptySet(),
                                historicalCurrentizedEventRefs =
                                    emptySet(),
                            )
                        },
                        readConversationView =
                            ::view,
                    )

                val lexicalRequest =
                    request(
                        """{"queries":[{"terms":["cache miss"]}],"scopes":["CONVERSATION"]}""",
                    )
                assertTrue(
                    VexLifeContextRevisit.supportsIndexedConversationView(
                        lexicalRequest,
                    ),
                )
                val lexicalSources =
                    turnSources.sources(
                        lexicalRequest,
                    )
                assertEquals(
                    0,
                    fullConversationReads,
                )
                val lexicalView =
                    assertIs<VexLifeContextRevisit.ConversationView>(
                        lexicalSources.conversationView,
                    )
                assertEquals(
                    listOf(
                        turns[0].first.turnRef,
                        turns[1].first.turnRef,
                    ),
                    lexicalView.turns.map(
                        VexLifeTurnNavigation.Turn::turnRef,
                    ),
                )
                assertEquals(
                    4,
                    lexicalView.conversationEventsRead,
                )
                assertEquals(
                    0,
                    lexicalView.derivedEntryScanCount,
                )

                val lexicalResult =
                    VexLifeContextRevisit.revisit(
                        request =
                            lexicalRequest,
                        sources =
                            lexicalSources,
                        estimateTokens =
                            ::estimate,
                    )
                assertTrue(
                    lexicalResult.text.contains(
                        "searched 2 exact indexed-neighborhood turns against 4 completed turns",
                    ),
                )
                assertTrue(
                    lexicalResult.text.contains(
                        "You seem frustrated, so I will keep this short.",
                    ),
                )
                assertTrue(
                    lexicalResult.text.contains(
                        "I'm not frustrated. I want exact causal detail.",
                    ),
                    "immediate correction neighbor must be available for typed one-step expansion",
                )
                assertTrue(
                    lexicalResult.text.contains(
                        "this message corrects the reply in " +
                            turns[0].first.turnRef,
                    ),
                    "the adjacent correction reached through typed expansion must retain its CORRECTS relationship",
                )
                assertEquals(
                    4,
                    lexicalResult.conversationEventsRead,
                )

                val seedRequest =
                    request(
                        """{"queries":[{"seedRefs":["${turns[2].second.eventRef}"]}],"scopes":["CONVERSATION"]}""",
                    )
                val seedSources =
                    turnSources.sources(
                        seedRequest,
                    )
                assertEquals(
                    0,
                    fullConversationReads,
                )
                val seedResult =
                    VexLifeContextRevisit.revisit(
                        request =
                            seedRequest,
                        sources =
                            seedSources,
                        estimateTokens =
                            ::estimate,
                    )
                assertTrue(
                    turns[2].first.eventRef in
                        seedResult.returnedSourceRefs,
                )
                assertTrue(
                    seedResult.text.contains(
                        "The sapphire seed is stored here.",
                    ),
                )

                val correctionRequest =
                    request(
                        """{"queries":[{"corrections":true}],"scopes":["CONVERSATION"]}""",
                    )
                assertTrue(
                    VexLifeContextRevisit.supportsIndexedConversationView(
                        correctionRequest,
                    ),
                )
                val correctionSources =
                    turnSources.sources(
                        correctionRequest,
                    )
                assertEquals(
                    0,
                    fullConversationReads,
                    "global correction typed-edge lookup must not force the full Conversation snapshot",
                )
                val correctionView =
                    assertIs<VexLifeContextRevisit.ConversationView>(
                        correctionSources.conversationView,
                    )
                assertTrue(
                    correctionView.derivedTypedEdgeLookupCount >
                        0,
                )
                val correctionResult =
                    VexLifeContextRevisit.revisit(
                        request =
                            correctionRequest,
                        sources =
                            correctionSources,
                        estimateTokens =
                            ::estimate,
                    )
                assertTrue(
                    correctionResult.text.contains(
                        "I'm not frustrated. I want exact causal detail.",
                    ),
                )
                assertTrue(
                    correctionResult.text.contains(
                        "opens like a correction",
                    ),
                    "global corrections:true directly locates the correction turn as CORRECTIONS",
                )
                assertTrue(
                    correctionResult.text.contains(
                        "the person's next message corrects this reply (" +
                            turns[1].first.turnRef +
                            ")",
                    ),
                    "one typed step from the correction turn must retain CORRECTED_BY on the exact prior turn",
                )
            }
        }
    }

    @Test
    fun semanticCandidateRouteReturnsExactSourceWithoutPretendingTheQueryWordsWereLiteralMatches() {
        ContinuityTurnFixture().use {
            fixture ->
            val committed =
                fixture.commit(
                    Line(
                        "The paper crane is named Meridian-7.",
                        "Recorded.",
                        minutesAgo(90),
                    ),
                    Line(
                        "Later unrelated turn.",
                        "Later.",
                        minutesAgo(80),
                    ),
                )
            val projected =
                VexLifeTurnNavigation.project(
                    fixture.store
                        .current()
                        .canonicalEvents,
                )
            val semanticRequest =
                request(
                    """{"queries":[{"ref":"semantic","terms":["bird-shaped","identifier"],"speaker":"HUMAN"}],"scopes":["CONVERSATION"]}""",
                )
            val sources =
                VexLifeContextRevisit.Sources(
                    conversation = null,
                    conversationGap = null,
                    memory = null,
                    memoryGap = "no Memory in this fixture",
                    working = null,
                    workingGap = "no working checkpoint exists yet",
                    promptResidentEventRefs = emptySet(),
                    now = now,
                    conversationLifecycle =
                        VexLifeContextRevisit.ConversationLifecycle(
                            bindingRef =
                                VexLifeContinuitySourceLocator.UnboundSourceLifecycleBindingRef,
                            blockedEventRefs =
                                emptySet(),
                            historicalCurrentizedEventRefs =
                                emptySet(),
                        ),
                    conversationView =
                        VexLifeContextRevisit.ConversationView(
                            turns =
                                projected,
                            totalCompletedTurnCount =
                                projected.size,
                            conversationEventsRead =
                                2,
                            exactHeadFileReadCount =
                                2,
                            wholeHistoryHeadTraversalPerformed =
                                false,
                            derivedEntryScanCount =
                                0,
                            derivedPostingLookupCount =
                                6,
                            derivedRefLookupCount =
                                0,
                            derivedTypedEdgeLookupCount =
                                0,
                            derivedSemanticLookupCount =
                                1,
                            semanticCandidateMatches =
                                listOf(
                                    VexLifeContextRevisit.SemanticCandidateMatch(
                                        queryRef =
                                            "semantic",
                                        turnRef =
                                            committed[0]
                                                .first
                                                .turnRef,
                                    ),
                                ),
                        ),
                )

            val result =
                VexLifeContextRevisit.revisit(
                    request =
                        semanticRequest,
                    sources =
                        sources,
                    estimateTokens =
                        ::estimate,
                )

            assertEquals(
                State.RETURNED,
                result.state,
            )
            assertTrue(
                committed[0].first.eventRef in
                    result.returnedSourceRefs,
            )
            assertTrue(
                committed[0].second.eventRef in
                    result.returnedSourceRefs,
            )
            assertTrue(
                result.text.contains(
                    "The paper crane is named Meridian-7.",
                ),
                "semantic nomination must return the exact canonical source words for Vex to inspect",
            )
            assertTrue(
                result.text.contains(
                    "semantic[semantic_candidate]",
                ),
                "semantic nomination must remain visibly distinct from literal TERMS matching",
            )
            assertFalse(
                result.text.contains(
                    "semantic[terms]",
                ),
                "semantic nomination must not rewrite resemblance into an exact lexical match",
            )
            assertTrue(
                result.text.contains(
                    "similarity is not acceptance",
                ),
            )
            assertFalse(
                result.text.contains(
                    "Query \"semantic\" found nothing",
                ),
                "a source-nominated semantic candidate must survive the ContextRevisit exact-word layer",
            )
        }
    }

    @Test
    fun semanticCandidateRankOrdersOtherwiseEqualCandidatesBeforeRecency() {
        ContinuityTurnFixture().use {
            fixture ->
            val committed =
                fixture.commit(
                    Line(
                        "The paper crane is named Meridian-7.",
                        "Recorded true source.",
                        minutesAgo(120),
                    ),
                    Line(
                        "The paper airplane is named Meridian-7.",
                        "Recorded similar wrong object.",
                        minutesAgo(90),
                    ),
                    Line(
                        "The bird feeder is beside the cedar gate.",
                        "Recorded similar wrong object.",
                        minutesAgo(60),
                    ),
                )
            val projected =
                VexLifeTurnNavigation.project(
                    fixture.store
                        .current()
                        .canonicalEvents,
                )
            val semanticRequest =
                request(
                    """{"queries":[{"ref":"semantic","terms":["bird-shaped","identifier"],"speaker":"HUMAN"}],"scopes":["CONVERSATION"]}""",
                )
            val rankedMatches =
                listOf(
                    committed[0],
                    committed[2],
                    committed[1],
                )
                    .map {
                        turn ->
                        VexLifeContextRevisit.SemanticCandidateMatch(
                            queryRef =
                                "semantic",
                            turnRef =
                                turn.first.turnRef,
                        )
                    }
            val sources =
                VexLifeContextRevisit.Sources(
                    conversation = null,
                    conversationGap = null,
                    memory = null,
                    memoryGap = "no Memory in this fixture",
                    working = null,
                    workingGap = "no working checkpoint exists yet",
                    promptResidentEventRefs = emptySet(),
                    now = now,
                    conversationLifecycle =
                        VexLifeContextRevisit.ConversationLifecycle(
                            bindingRef =
                                VexLifeContinuitySourceLocator.UnboundSourceLifecycleBindingRef,
                            blockedEventRefs =
                                emptySet(),
                            historicalCurrentizedEventRefs =
                                emptySet(),
                        ),
                    conversationView =
                        VexLifeContextRevisit.ConversationView(
                            turns =
                                projected,
                            totalCompletedTurnCount =
                                projected.size,
                            conversationEventsRead =
                                6,
                            exactHeadFileReadCount =
                                2,
                            wholeHistoryHeadTraversalPerformed =
                                false,
                            derivedEntryScanCount =
                                0,
                            derivedPostingLookupCount =
                                6,
                            derivedRefLookupCount =
                                0,
                            derivedTypedEdgeLookupCount =
                                0,
                            derivedSemanticLookupCount =
                                1,
                            semanticCandidateMatches =
                                rankedMatches,
                        ),
                )

            val result =
                VexLifeContextRevisit.revisit(
                    request =
                        semanticRequest,
                    sources =
                        sources,
                    estimateTokens =
                        ::estimate,
                )

            assertEquals(
                State.RETURNED,
                result.state,
            )
            val trueIndex =
                result.text.indexOf(
                    "The paper crane is named Meridian-7.",
                )
            val newerRankTwoIndex =
                result.text.indexOf(
                    "The bird feeder is beside the cedar gate.",
                )
            val newerRankThreeIndex =
                result.text.indexOf(
                    "The paper airplane is named Meridian-7.",
                )
            assertTrue(
                trueIndex >=
                    0 &&
                    newerRankTwoIndex >
                        trueIndex &&
                    newerRankThreeIndex >
                        newerRankTwoIndex,
                "semantic nomination rank must break recency ties without becoming source truth",
            )
            assertTrue(
                result.text.contains(
                    "semantic[semantic_candidate]",
                ),
            )
            assertFalse(
                result.text.contains(
                    "semantic[terms]",
                ),
            )
        }
    }

    @Test
    fun windowRequestFallsBackToFullConversationSnapshot() {
        ContinuityTurnFixture().use {
            fixture ->
            fixture.commit(
                Line(
                    "Earlier source.",
                    "Earlier.",
                    minutesAgo(120),
                ),
                Line(
                    "Actually, this is corrected.",
                    "Correction noted.",
                    minutesAgo(60),
                ),
            )
            fixture.store.withWriterLease {
                val owner =
                    this
                var fullConversationReads =
                    0
                var indexedViewAttempts =
                    0
                val turnSources =
                    VexLifeContextRevisit.TurnSources(
                        readConversation = {
                            fullConversationReads +=
                                1
                            owner.current()
                        },
                        memory = {
                            null
                        },
                        readWorking = {
                            null
                        },
                        promptResidentEventRefs = {
                            emptySet()
                        },
                        promptResidentStatementRefs = {
                            emptySet()
                        },
                        now = {
                            now
                        },
                        readConversationLifecycle = {
                            VexLifeContextRevisit.ConversationLifecycle(
                                bindingRef =
                                    VexLifeContinuitySourceLocator.UnboundSourceLifecycleBindingRef,
                                blockedEventRefs =
                                    emptySet(),
                                historicalCurrentizedEventRefs =
                                    emptySet(),
                            )
                        },
                        readConversationView = {
                            revisitRequest,
                            _,
                            ->
                            indexedViewAttempts +=
                                1
                            if (
                                VexLifeContextRevisit.supportsIndexedConversationView(
                                    revisitRequest,
                                )
                            ) {
                                error(
                                    "this test expects only unsupported broad requests",
                                )
                            }
                            null
                        },
                    )

                val windowRequest =
                    request(
                        """{"queries":[{"window":{"lastTurns":2}}],"scopes":["CONVERSATION"]}""",
                    )
                assertFalse(
                    VexLifeContextRevisit.supportsIndexedConversationView(
                        windowRequest,
                    ),
                )
                val windowSources =
                    turnSources.sources(
                        windowRequest,
                    )
                assertEquals(
                    1,
                    indexedViewAttempts,
                )
                assertEquals(
                    1,
                    fullConversationReads,
                )
                assertTrue(
                    windowSources.conversationView ==
                        null,
                )
                val windowResult =
                    VexLifeContextRevisit.revisit(
                        request =
                            windowRequest,
                        sources =
                            windowSources,
                        estimateTokens =
                            ::estimate,
                    )
                assertTrue(
                    windowResult.returnedSourceRefs.isNotEmpty(),
                )

            }
        }
    }

    @Test
    fun malformedRequestsAreRefusedWithTheShapeToUseInstead() {
        listOf(
                """{}""" to "queries is a list",
                """{"queries":[{}]}""" to "a query needs terms",
                """{"queries":[{"terms":["a"]}],"maxTurns":99}""" to "maxTurns is 1-12",
                """{"queries":[{"terms":["a"],"window":{}}]}""" to "window needs",
                """{"queries":[{"terms":["a"],"from":3}]}""" to "needs seedRefs",
                """{"queries":[{"terms":["a"]}],"scopes":["DREAMS"]}""" to "unknown scope",
            )
            .forEach { (json, reason) ->
                val refused =
                    assertIs<Parsed.Refused>(
                        VexLifeContextRevisit.parse(VexContractJson.decode<JsonObject>(json))
                    )
                assertTrue(refused.reason.contains(reason), "$json -> ${refused.reason}")
            }
    }
}

// [VXG RealForever]
