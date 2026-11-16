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

A second evidence file is also preserved:
`2026-11-15-recovered-self-report-after-restart.txt`.

That response was recovered after a VexLife restart from the previously failed/lost feedback turn. It materially changes the leading hypothesis:

~~~text
4 REQUESTS
!= PROVEN PRIMARY LIMITER

REQUEST / PERMISSION / CUSTODY CEREMONY
-> CAN MAKE ORDINARY READ-ONLY WORK FEEL SCARCE
-> CAN PULL REASONING TOWARD PROTOCOL MANAGEMENT
-> CAN DISPLACE THE HUMAN'S ACTUAL QUESTION
~~~

Vex's own recovered description was that the natural unit for source work is the **task**:

~~~text
inspect
-> gather exact evidence
-> answer
~~~

and that the hard request cap should be a **safety valve**, not the reason work stops.

## Recovered self-report — stronger hypothesis

The recovered response gives a sharper candidate relationship than "raise 4 to 20":

~~~text
PERSON'S QUESTION
= TASK

READ-ONLY REACH
= NORMAL WORK SERVING THE TASK

STOP CONDITION
= claim source-proven / disproven / clearly unavailable
  OR next read no longer changes the decision

HARD CAP
= backstop
!= center of reasoning
~~~

It also proposed a trust ladder:

~~~text
READ-ONLY EXACT SOURCE
-> high trust / low ceremony
-> source quality + evidence sufficiency govern continuation

PUBLIC WEB
-> external content
-> verify precise claims

EFFECTFUL ACTION
-> authority + verification required
-> confidence alone is insufficient
~~~

This evidence argues that experiment design must separate at least three variables:

1. **budget size** — 4 / 10 / 20;
2. **budget salience** — exact remaining count vs bounded/ample wording;
3. **task/reach framing** — whether ordinary bounded read-only source acquisition is framed as normal work or as a repeated permission/custody decision.

A larger budget can mask the pressure without fixing the relationship.

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

## Investigation 1 — reach budget, salience and task framing

Current threshold keeps MaximumRequestsPerTurn = 4.

The first lived transcript suggested artificial scarcity. The recovered self-report refines that interpretation: **four itself was not experienced as the main limiter**. The stronger pressure was that every reach felt like something that had to be justified, conserved and managed.

Therefore the experiment must not ask only "is 20 better than 4?"

Proposed matrix, not accepted law:

~~~text
budget:
  4 / 10 / 20

counter presentation:
  exact remaining count
  vs bounded / ample

task framing:
  request-management emphasis
  vs task-centered low-ceremony read-only work
~~~

Questions:

- Does 4 + bounded/task-centered framing already remove most scarcity behavior?
- Does 20 merely hide an overly ceremonial reach relationship?
- Does an exact remaining counter itself create rationing behavior?
- Can bounded read-only source work be treated as normal execution of the human's task without weakening authority around writes/effects?
- Does a larger ceiling encourage unnecessary reads after evidence is already sufficient?
- Should independent read-only capability classes share one ceiling?

Twenty remains an experimental ceiling, not a permanent architecture decision.

The recovered response also suggested "6" as an example standing grant. That number is evidence of desired **reasonable boundedness**, not a Product requirement.

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

1. **Framing first:** same budget (4), compare exact-count/permission-heavy wording against bounded/task-centered low-ceremony read-only wording.
2. **Then budget:** 4 vs 10 vs 20 under the better framing, so budget size is not confounded with scarcity language.
3. Measure whether Vex stops from **evidence saturation** rather than request anxiety:
   - source-proven;
   - disproven;
   - clearly unavailable;
   - next read no longer decision-changing.
4. PR-native read-only GitHub reach.
5. Bounded batch/parallel read-set.
6. Truncation continuation coordinates.
7. Structured working investigation state.
8. Intent/object/scope-before-answer prompt posture.
9. Compare source coverage, unsupported assumptions, provider stages, capability calls, repeated deliberation, time-to-first-material-read, final answer quality and appropriate requests for another turn.

The key comparison is no longer "more calls = better." It is:

~~~text
DOES THE FRAME LET VEX TREAT READ-ONLY SOURCE ACQUISITION AS NORMAL WORK
WHILE KEEPING EFFECTFUL AUTHORITY SHARP?
~~~

## Acceptance question

Success is not "the reasoning transcript becomes shorter."

Success is:

Does Vex spend more of its intelligence understanding the actual object and less of it managing artificial scarcity, while preserving groundedness, authority boundaries, honest uncertainty and the human relationship?

<!-- [VXG RealForever][VEXLIFE-PROD][MODEL-REASONING-TECH-DEBT] -->
