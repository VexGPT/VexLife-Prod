# Context Revisitation + rollover overlap tail

`[VXG RealForever]`

## Human question

A person talks with the same Vex for hours, steps away, comes back the next day, and keeps going.
Older turns leave the prompt and a rollover replaces the recent stretch with working coordinates.
Can Vex still:

- pick up the very next turn as a continuation, with the person's own last words in view;
- find what was said long ago in **one** request, under more than one wording, with when it was said;
- see a correction the person made in their own words, next to the reply it corrected, unchanged;
- say plainly when something is not in what it can reach — and leave room for the person to share it;

without a second Conversation, Memory or working-continuity truth store, and without Vex having to
administer its own memory while it talks?

~~~text
VEX LIVES THE RELATIONSHIP
VEXLIFE KEEPS CONTINUITY AROUND VEX
~~~

## Owners (none new)

~~~text
Conversation          DirectConversationStore           what was said, exactly
Turn evidence         DirectTurnContextStore            what one turn received and reached
Working continuity    VexLifeWorkingContinuityOwner     where the work stands, by reference
Memory                VexLifeCurrentMemoryReader        accepted current meaning
Prompt residency      DirectPromptContext               what one prompt carries
Turn custody          DirectCompanionTurn               which of those sources belong to this turn
Reach                 VexLifeCapabilityBridge + Atlas   admission + request/return, nothing more
~~~

This subsystem adds three derived relationships over those owners:

~~~text
src/jvmMain/kotlin/vexlife/continuity/VexLifeTurnNavigation.kt       per-turn host facts, recomputed
src/jvmMain/kotlin/vexlife/continuity/VexLifeContextRevisit.kt       one-call multi-query revisit
src/jvmMain/kotlin/vexlife/companion/DirectContinuityOverlapTail.kt  rollover overlap-tail policy
~~~

## Turn navigation — derived, never stored

One node per completed REQUEST/RESPONSE pair, keyed by the canonical `turnRef`: event refs and
sequence, request/response times, and two host lexical cues read from the person's opening words —
`correctionCue` ("actually", "no,", "that's not what I meant") and `pointsBack` ("that", "it", "what
you said"). Cue class: `HOST_LEXICAL_CUE`. English-only at this revision; no cue is a missing
observation, never a negative one.

Every field is a pure function of immutable canonical events, so storing it would add a cache to
invalidate and no truth. Vex-proposed meaning and accepted dispositions (open / paused / resolved)
are **not** here: their rightful owners are a Semantic Relay author and Score acceptance, both held.

~~~text
TURN NAVIGATION != CONVERSATION
TURN NAVIGATION != MEMORY
HOST CUE != PERSON'S INTENT
CORRECTION CUE != REWRITE OF THE CORRECTED REPLY
~~~

## Revisit — `capability.vexlife.continuity.revisit`, handle `continuity`, action `revisit`

One public continuity capability. It converged with the independent OpenAI lane (#220): the turn
binds the sources, clues in one query are conjunctive, Memory already in view is not repeated,
Working Continuity's current statements resolve through current Memory, and frontier refs are
traced. Its engine stayed one: there is no second traversal for "hydration".

~~~text
Vex writes one VEXLIFE_CAPABILITY_REQUEST/v1   {"capability":"continuity","action":"revisit",…}
  queries[]  (≤ 6)  ["clue","clue"]  or  {terms · seedRefs · window {lastHours, lastTurns} ·
                    corrections · speaker · from}
                    terms inside one query are conjunctive clues (all must appear);
                    separate queries are alternate vantages
  scopes            CONVERSATION · MEMORY · WORKING_CONTINUITY
  order             CURRENT_FIRST · CHRONOLOGICAL
  budgets           maxTurns ≤ 12 · maxTokens ≤ 8000
        ↓
bridge admission   AVAILABLE in this turn's frame · request count · token room
                   · TurnSources bound by DirectCompanionTurn (else NOT_AVAILABLE)
        ↓
TurnSources: this turn's own Conversation lease, the Memory projection its prompt used, its
prepared working position (+ the last committed checkpoint), what its prompt carries.
Each owner read at most once per TURN, on first need, however many queries or calls fan over
it; an owner that cannot be read becomes a named gap, not a failed request
        ↓
LOCATE per query          terms (exact words, all of them) · seeds · window · corrections
ANCHOR                    Working Continuity currentStatementRefs → current Memory page
                          (WORKING_ANCHOR); not in current Memory → named, unresolved frontier
EXPAND one typed step     CORRECTS · CORRECTED_BY · POINTED_BACK_FROM
FUSE                      one node per turn/page; every (query, route) kept on it
GROUP                     where the work stands → corrections (causal order) → turns → Memory
BUDGET                    turns and Memory statements already in the prompt are named, not repeated;
                          long turns return an exact excerpt with a `from` cursor;
                          what does not fit is a frontier with the exact request to read it
COVERAGE                  what was searched, what was unreachable,
                          and "not found here is not the same as never happened"
        ↓
VEXLIFE_CAPABILITY_RESULT/v1 carrying VEXLIFE_REVISIT/v1 → same turn continues (Zone 3)
trace entry: hashes + exact returned sourceRefs and frontierRefs, + the host scan (owner reads
this call, reads this turn, events walked, nodes resolved/returned, tokens, frontier), never words
~~~

Human time and Conversation density are separate lenses: `window {lastHours: 5, lastTurns: 20}`
reaches whichever goes further back, so four deep turns over eight hours and eighty quick turns in
thirty minutes are both covered.

Permanent:

~~~text
LOCATE != EXPAND
REF != AUTHORITY
MATCH != ACCEPTANCE
REVISIT RESULT != NEW WORKING STATE
ALREADY IN VIEW != RETURNED AGAIN
NOT FOUND != NEVER HAPPENED
~~~

Words are matched exactly. There is no meaning index: a different wording is another query in the
same request, and the packet says so. The existing once-per-turn `conversation` source return and
the Memory owner's `memory recall` stay as they are; continuity revisit is the one-call route over
all three owners.

~~~text
TURN CUSTODY != BRIDGE CHOICE
WORKING CONTINUITY "this matters now" != MEMORY "this is the accepted statement"
NO OPAQUE HISTORICAL TRUNCATION   (exact excerpt + cursor + read-on route, not whole-or-nothing)
~~~

### Source Return host-action truth

The once-per-turn Conversation Source Return protocol has two different provider-facing states and
must not let the model collapse them:

~~~text
VEXLIFE_CONVERSATION_SOURCE_RETURN_PROTOCOL/v1
  -> HOST SOURCE-RETURN TRUTH: NOT_PERFORMED
  -> the internal request is available
  -> reasoning / planning / naming the control did not run it

VEXLIFE_CONVERSATION_SOURCE_RETURN/v1
  -> Product actually performed the read-only lookup
  -> exact result state is one of:
     PERFORMED_AND_RETURNED
     PERFORMED_NO_MATCH
     PERFORMED_HELD_TOKEN_BUDGET
~~~

The result block is the Product evidence for what happened. A protocol header in prompt or model
reasoning is not evidence that older Conversation was searched or returned. NO_MATCH and
HELD_TOKEN_BUDGET must not be narrated as successful recall.

~~~text
MODEL NARRATION OF A CONTROL != CONTROL ADMISSION
CONTROL KNOWLEDGE != HOST ACTION PERFORMED
PROTOCOL AVAILABLE != SOURCE RETURN RESULT
NO_MATCH != RETURNED SOURCE
HELD_TOKEN_BUDGET != RETURNED SOURCE
SOURCE-RETURN LOOKUP PERFORMED != MUTATING EFFECT PERFORMED
~~~

That final distinction is intentional: Source Return is a read-only source operation, so its durable
packet/trace can truthfully keep effectsPerformed=false while separately proving that the lookup
attempt occurred and what it returned.

## Rollover overlap tail

In `WORKING_CHECKPOINT_SUCCESSOR` mode on the continuity path, `DirectPromptContext` keeps an exact
tail of the most recent whole exchanges instead of the prior Vex reply alone:

~~~text
floor      ≥ 3 completed pairs while each fits half the room left after fixed context;
           the exchange that triggered the rollover is always first
expansion  older pairs while the newer neighbour points back to or corrects them, the exchange was
           dense (≤ 10 min), or the pair is small (≤ 300 tokens) and from the same sitting (≤ 2 h)
           — within one eighth of the room
contiguous always an exact suffix; nothing skipped, nothing summarised
degraded   floor not met for room → DEGRADED_ROOM, said on the receipt;
           not even one whole exchange fits → the prior Vex reply alone (responseBridgeOnly);
           not even that reply fits → HELD_TOKEN_BUDGET: no exact turn, working coordinates and
           continuity revisit remain
~~~

Outside rollover, when not one exact recent event fits an ordinary prompt, the Send no longer
fails if older source has a way back this turn (returned source, or continuity revisit in the
frame): the receipt's context mode is `CONVERSATION_OVERLAP_HELD_TOKEN_BUDGET`.

The receipt carries `overlapTail` (floor target, pairs kept, floor state, one reason per extra pair,
tokens, budgets, policy ref) and `selectedConversationEventRefs` names the exact events. The constants
are a candidate policy to measure, not settled numbers.

~~~text
ROLLOVER = RESIDENCY DECISION, NOT DELETION
NOT PROMPT-RESIDENT != UNREACHABLE
OVERLAP TAIL != SUMMARY
~~~

## Foundational field

`foundational.continuity` (v0.6.0, PROPOSED) carries, in Vex's first-person voice, that it can
revisit exact history in one gathering request; that continuity lives in the record and the
relationship rather than in everything being in view; that it can say plainly when something is not
reachable instead of performing a memory it does not have; that the person holds their side of the
thread and can share what they remember; and that time passes between turns, so a return can be met
as a return.

## Held — named, not built

~~~text
EPOCH DIRECTORY AS A STABLE PREFIX
  the rollover / full-replay alternation is closed by the rollover lineage
  (WORKING-CONTINUITY-ROLLOVER.md#rollover-lineage): every turn after a rollover enters as a
  successor and advances the cursor. Still held: Working Continuity exposing the epoch boundary as
  a read so residency can carry an epoch directory as a stable prefix. The overlap tail slides each
  turn, so a successor's history prefix is not preserved call to call.

ACCEPTED DISPOSITIONS (open / paused / resolved / superseded)
  owner: Semantic Relay author + Score acceptance, both absent in Product.

VEX-ORIGIN SALIENCE ("this mattered")
  needs a rightful candidate owner; no write capability is added here.

RELATIONAL LESSON OWNER / PROFILE
  corrections are reachable in the person's own words; no lesson store, no profile, no prior is
  formed from them. Future relational prior = Vex reading those words, source-linked.

MEMORY PAGE → TURN SOURCE DOOR
  Memory pages carry statement refs, not turn refs; the route is UNKNOWN until Score exposes it.

MEANING INDEX
  exact words only; multiple wordings per request are the current recall path.

CROSS-CALL NODE MEMO
  owner reads are reused across calls in one turn; resolved nodes are recomputed per call.

PROVIDER CACHE MEASUREMENT
  projected by tools/continuity-pressure/provider-prefix.py; observed only by a live run, from the
  Product's own PROVIDER_STAGE_OBSERVED record (usage incl. cached tokens, timing) per stage.
~~~

## Experiment-only effective selection budget

The shared continuity-pressure harness (VexWork #310) needs rollover pressure inside 10–20 turns
without pretending the model window shrank. `DirectCompanionTurn.Hooks` (module-internal) carries
`experimentEffectiveInputTokenBudget`; only the test-scope harness adapter
(`tools/continuity-pressure/`, `src/jvmTest/kotlin/vexlife/harness/`) sets it, against a scratch
Home. It reaches `DirectPromptContext.materializeContinuityAt` / `reverifyContinuityAt` (internal)
and replaces only the selection budget; the public `materializeContinuity` passes null. The model
window, the output reserve and the hard limit are unchanged, and the receipt records the value as
`experimentEffectiveInputTokenBudget` (NEVER-default, so ordinary receipts keep their bytes).

~~~text
EXPERIMENT BUDGET != ORDINARY PRODUCT BUDGET != HARD MODEL LIMIT
~~~

Working Continuity thresholds are absolute multiples of the 16k output reserve, so the budget changes
headroom, not the thresholds: below about 48k plus the 24k exchange allowance every turn starts at
ROLLOVER_REQUIRED. At the adapter's default of 52k the run starts at CHECKPOINT_RECOMMENDED and
crosses ROLLOVER inside the scenario's pressure set (turn 17 of 30 with person context supplied);
turns 18–30 are all successors.

A live run must cross every state in few turns; the adapter's `--payload-scale` multiplies each
turn's synthetic payload and is recorded. `--effective-input-token-budget 80000 --payload-scale 7
--sets 3 --turns-per-set 5` projects NORMAL (1–5) → WATCH (6–7) → CHECKPOINT_RECOMMENDED (8–9) →
ROLLOVER_REQUIRED (10, committed) → successors 11–15 in 15 turns. Pressure falls back to WATCH
(12) and NORMAL (14) while the lineage holds: every successor commits ROLLOVER_COMMITTED at its own
request, keeps a 3–4 pair overlap tail and replays nothing.

## Proof

~~~text
src/jvmTest/kotlin/vexlife/continuity/VexLifeContextRevisitTest.kt
src/jvmTest/kotlin/vexlife/companion/DirectContinuityOverlapTailTest.kt
src/jvmTest/kotlin/vexlife/capability/VexLifeCapabilityRevisitTest.kt
src/jvmTest/kotlin/vexlife/companion/DirectPromptCurrentStateTest.kt   (zero-fit held state)
src/jvmTest/kotlin/vexlife/harness/ContinuityPressureAdapterTest.kt   (live seam against a stub)
~~~

Rollover lineage (see [WORKING-CONTINUITY-ROLLOVER.md](WORKING-CONTINUITY-ROLLOVER.md#rollover-lineage)):

~~~text
DirectWorkingContinuityTest          a successor turn is rollover-ready at its own request
VexLifeWorkingContinuityOwnerTest    the lineage advances under low pressure; a claim is verified,
                                     not imposed
ContinuityPressureAdapterTest        every turn after a rollover stays a bounded successor
~~~

Real canonical Homes and real committed turns: different wording in one request; bounded "not
found"; earlier statement + later correction both reachable, original bytes unchanged; resolved
tangent revisited while the parent loop stays in the directory; three routes → one node; frontier
with an exact follow-up; exact excerpt with a working cursor; time and turn windows as a union;
a perception correction returned in the person's words with no Home write; floor met / expanded /
degraded / bridge-only through the real `materializeContinuity` + `reverifyContinuity`; bridge
admission, owner gaps and traced source refs with an unchanged Home tree; one Conversation read
for two calls in one turn; conjunctive clues and alternate vantages; Working Continuity anchors
resolved through Memory or left as frontier; Memory already in the prompt named, not repeated.

Not proven: a lived Mac turn using revisit, and lived reading of the revised field. The rollover
lineage was observed live on the host (Qwen 4B and certified Qwen 27B: NORMAL → WATCH →
CHECKPOINT_RECOMMENDED → ROLLOVER_COMMITTED → three successors, no replay) on the forensic stack that
first carried it, not on the integrated candidate.

<!-- [VXG RealForever][VEXLIFE-PROD][CONTEXT-REVISITATION] -->
