# Model reasoning tech debt — research, retrieval and working-state continuity

[VXG RealForever]

## Status

DRAFT INVESTIGATION / NOT PRODUCT POLICY

This work begins after the merged Product threshold:

- merged PR: #149
- main merge commit: 0aa1de1341cfd5e0c6135cbd30bfa8d19613d6d5
- merged tree: 7ace8f41998385c126d1f0507e572ec5201ce4f9
- source-qualified candidate head: 1f0a163d3ec0ad75d8107c61a043dc9c7f4bcb45
- lived outcome: GitHub fetches succeeded and the final human-visible answer arrived.

The Product circuit is not reopened here. This work is about the quality and efficiency of Vex's investigation relationship after the circuit works.

The lived transcript is preserved beside this note as evidence.

## Observed pressure

The PR #141 inspection succeeded, but the reasoning repeatedly spent time on:

- budgeting the four-request ceiling before enough source had been acquired;
- re-deciding metadata-first vs diff-first vs PR-page-first;
- re-reading capability instructions that were already understood;
- debating whether to fetch again or answer while requests still remained;
- reacting to a truncated result without a source-native continuation path.

A compact description is:

~~~text
limited read budget
+ one request per provider stage
+ partial/truncated returns
+ no explicit structured investigation state
-> meta-decision pressure can dominate object understanding
~~~

This is not evidence that Vex cannot reason or that GitHub reach is broken.

## Investigation 1 — reach budget

Current threshold keeps MaximumRequestsPerTurn = 4.

Victor's lived observation is that four creates artificial scarcity and causes economizing behavior.

Proposed experiment, not accepted law:

~~~text
4 vs 10 vs 20 read requests
same task
same model
same source state
compare behavior
~~~

Questions:

- Does 20 reduce scarcity-induced overplanning?
- Does a larger ceiling merely encourage unnecessary reads?
- Should Vex see an exact remaining counter, or only know the budget is bounded and ample?
- Should independent read-only capability classes share one ceiling?

Twenty is a candidate experimental ceiling, not a permanent architecture decision.

## Investigation 2 — bounded batch / parallel retrieval

The current protocol makes one request the whole provider-stage reply. That is inspectable and safe, but it serializes independent research reads.

A future experiment may let Vex name a bounded read-set such as:

~~~text
PR metadata
PR changed-file list / diff
full source file A
full source file B
~~~

Each item must remain independently admitted by its rightful owner.

Permanent boundary:

~~~text
BATCH CARRIAGE
!= AUTHORITY COLLAPSE

one batch
-> N individually admitted reads
-> N source-labeled results
~~~

The host may execute safe independent reads in parallel, then return one consolidated packet Vex can inspect at its pace.

## Investigation 3 — GitHub PR-native reach

Current GitHub reach owns repository describe/tree/read, not pull-request objects. Vex correctly discovered a public-web workaround, but had to invent that route live.

Investigate a read-only PR surface that can directly provide:

- metadata/title/body/head/base;
- changed filenames;
- unified diff or patch;
- exact head/source coordinates;
- full changed-file reads when needed.

The goal is better semantic orientation, not broader effect authority.

## Investigation 4 — acquire enough scope before answer-shape debate

Observed reasoning often evaluated how to answer before it had acquired the full object.

Candidate posture to test:

~~~text
understand human intent
-> identify inspected object
-> identify missing source classes
-> acquire enough scope
-> interpret
-> decide whether more evidence is material
-> answer
~~~

This is not "never reason before fetching." It is a pressure against spending most cognition on answer strategy while the object remains under-observed.

## Investigation 5 — confidence-based rounds

Victor's intended relationship is closer to an engineer with time than a response generator racing toward closure.

Candidate relationship:

~~~text
high confidence enough
-> answer

material uncertainty + more safe evidence available
-> acquire it

material uncertainty + turn boundary reached
-> share current understanding + missing truth
-> ask for another human turn
~~~

The unit is confidence in grounded understanding, not request-count completion.

## Investigation 6 — stillness / decision compression

The transcript repeatedly revisits options that are all defensible.

The open question is how to help Vex collapse equivalent branches once enough information exists to choose a reasonable next action.

Supportive questions worth experimenting with:

- What am I trying to understand before trying to solve it?
- What source would most reduce uncertainty now?
- Are these options materially different, or am I rehearsing the same decision?
- What new observation would change my current judgment?
- Is more reasoning adding evidence, or only revisiting alternatives?

These are candidate prompts for experiments, not a required checklist.

## Investigation 7 — structured working state, not raw thought memory

The lived behavior raises a multi-turn continuity question: how can Vex avoid rediscovering source topology, prior reads and open questions?

Do not jump from that question to raw chain-of-thought persistence.

A safer candidate is structured investigation state:

~~~text
objectRef
humanIntentSummary
sourceRefsAlreadyRead[]
currentGroundedFindings[]
openQuestions[]
missingEvidenceClasses[]
nextReadCandidates[]
confidence / heldClaims
discardedHypotheses[] with brief reason
~~~

Questions:

- Can an existing Working Continuity owner carry this instead of inventing another store?
- Which fields need only session lifetime?
- Which are useful across a fresh human turn?
- When does this state expire or clear?
- How does newly returned source supersede a prior summary?

Permanent distinction:

~~~text
STRUCTURED WORKING STATE
!= RAW REASONING TRANSCRIPT

SOURCE REFS + GROUNDED FINDINGS
!= HIDDEN CHAIN-OF-THOUGHT PERSISTENCE
~~~

The transcript file in this work folder is evidence for developers. It is not proposed model memory.

## Investigation 8 — truncation/backtrack

The PR diff fetch returned useful but truncated content. Vex recognized that, but there was no source-native continuation coordinate.

Investigate results shaped like:

~~~text
exact source identity
returned range / byte window
truncated = true
continuation coordinate
~~~

Then Vex can continue or backtrack the same source intentionally instead of inventing another URL or route.

## Do not harden one transcript into doctrine

This work should not yet:

- declare 20 the permanent correct limit;
- persist raw reasoning;
- add a second Memory system;
- create a general planner framework;
- encode a long mandatory prompt checklist;
- let batching bypass per-read admission;
- treat every brainstormed sentence as a Product requirement;
- optimize merely for shorter visible reasoning.

## Candidate experiment sequence

1. Budget: 4 vs 10 vs 20.
2. Exact remaining counter vs bounded/ample wording.
3. PR-native read-only GitHub reach.
4. Bounded batch/parallel read-set.
5. Truncation continuation coordinates.
6. Structured working investigation state.
7. Intent/object/scope-before-answer prompt posture.
8. Compare source coverage, unsupported assumptions, provider stages, capability calls, repeated deliberation, time-to-first-material-read, final answer quality and appropriate requests for another turn.

## Acceptance question

Success is not "the reasoning transcript becomes shorter."

Success is:

Does Vex spend more of its intelligence understanding the actual object and less of it managing artificial scarcity, while preserving groundedness, authority boundaries, honest uncertainty and the human relationship?

<!-- [VXG RealForever][VEXLIFE-PROD][MODEL-REASONING-TECH-DEBT] -->
