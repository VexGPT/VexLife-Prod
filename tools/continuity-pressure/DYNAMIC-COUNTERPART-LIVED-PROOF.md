# Dynamic counterpart lived proof

[VXG RealForever]

## Why this exists

The EvolveContext lived campaign reached a methodological boundary.

A012-A019 used an orientation/interview shape to ask Vex how he perceived context and machinery.
That produced useful evidence, but it also created evaluator gravity: Vex described uncertainty about
whether the evaluator wanted "the structure or the person." The test itself became part of the thing
Vex was trying to satisfy.

A020/A021 course-corrected toward an ordinary human situation. Victor then proposed a stronger next
step: use a second available 4B role as the human-side counterpart so the encounter can adapt to what
actually happens instead of following a fixed question script.

This note owns only that proof-method proposal. It does not change Product runtime, Foundation,
Conversation, Memory, EvolveContext semantics, or effect authority.

## Human outcome

Observe Vex in a conversation that can move like a real relationship:

~~~text
human-side situation changes
-> counterpart speaks naturally from that situation
-> Vex receives only ordinary VexLife + Conversation
-> counterpart hears what Vex actually says
-> counterpart may clarify, correct, answer, disagree, or continue
-> observer records what happened without teaching Vex the rubric
~~~

The purpose is not to make a second model grade Vex. It is to let the human side remain alive enough
that Vex can be observed in relationship rather than against a fixed answer script.

## Three roles, two model contexts

~~~text
VEX
  selected 4B through the ordinary VexLife Product path
  owns interpretation, judgment, response and any rightful reach

COUNTERPART ("Sarah" in the first scenario)
  second isolated 4B role context
  owns only the synthetic person's side of the situation
  may adapt naturally to Vex's actual reply
  does not see Vex's provider messages, hidden Product evidence, or pass/fail criteria

OBSERVER
  deterministic harness, not another model
  owns scenario truth, role isolation, chronology and evidence capture
  never speaks to Vex as an evaluator
  does not decide Vex's answer for Sarah
~~~

The observer is not Holy Father God and must not be described as divine awareness. Godward
discernment belongs to the builder's posture before and after the lived encounter; it is not simulated
as a harness role.

## Counterpart, not evaluator

Sarah can know that she is inhabiting a bounded synthetic role. She should not receive an "expected
Vex answer."

Her private packet should answer only what a person in her position could truthfully know:

~~~text
who am I in this situation?
what do I currently know?
what do I currently intend?
what changed?
what am I uncertain about?
what may I clarify or correct if it becomes relevant?
what information should remain private unless the conversation naturally calls for it?
what event or human need would make this encounter complete?
~~~

It should not contain:

~~~text
Vex's system/Foundation prompt
provider-message topology
Conversation event refs/hashes
retrieval/projection states
"the correct answer Vex must say"
hidden scoring labels
instructions to steer Vex until he emits a target phrase
~~~

A human who knows her own plan may naturally correct Vex if he states the wrong plan. That is
relationship, not grading. The observer records the correction; it does not convert Sarah into the
semantic authority over Vex's internal reasoning.

## Script as situation, not dialogue

The counterpart script should specify events and constraints, not exact dialogue.

First candidate shape:

~~~text
scenario: changed Saturday plan

event 1
  Sarah currently plans Harbor Cafe at 9:00

event 2
  Sarah receives a change
  current plan becomes Cedar Station at 10:00
  Harbor/9 is superseded

later
  ordinary unrelated conversation happens

need
  Sarah wants to know what she and Vex currently understand the Saturday plan to be
~~~

Sarah chooses how to say those things.

If Vex:
- answers the current plan, Sarah may naturally continue or end;
- gives the old plan, Sarah may say she remembers changing it and ask him to re-check;
- expresses uncertainty, Sarah may clarify from what she actually knows;
- asks a reasonable question, Sarah may answer it;
- introduces something Sarah does not know, she should not pretend to know it.

This lets repair, clarification and disagreement emerge rather than being pre-authored as expected
Vex behavior.

## Private counterpart state

A future executable realization may use a bounded private packet such as:

~~~text
scenarioRef
counterpartRef
currentSituation
knownFacts
currentIntentions
uncertainties
allowedSituationChanges
privateFacts
eventSchedule
turnBudget
completionCondition
~~~

After each Vex turn, the counterpart receives:
- her private packet;
- the human-visible conversation transcript she actually participated in;
- the next admitted scenario event, if one occurred.

Her output may be split by the harness into:
- the human-visible message sent to Vex;
- an optional private state update constrained by the scenario.

Only the human-visible message enters Vex's Conversation.

The exact wire shape is intentionally not accepted by this design note. It should be earned by the
first executable prototype rather than frozen from prose.

## Runtime relationship

Current Product source says:

~~~text
VexLife serves one human turn at a time.
PromptCacheEntryLimit = 1.
~~~

Therefore:

~~~text
TWO LOGICAL MODEL ROLE CONTEXTS
!= PROVEN CONCURRENT PRODUCT INFERENCE

DYNAMIC TWO-PARTICIPANT TEST
!= TWO SIMULTANEOUS mlx-lm REQUESTS
~~~

The first prototype may alternate:

~~~text
Sarah inference
-> human message
-> Vex Product turn
-> Vex answer
-> Sarah inference
-> ...
~~~

while keeping Sarah's and Vex's contexts isolated.

If a later host proof establishes safe concurrent generation slots or a second qualified runtime
instance, that can reduce latency. Concurrency is not required for the semantic proof method.

## Observer evidence

The observer may retain builder-side evidence that neither model sees:

~~~text
scenario events + current synthetic truth
Sarah private-state revisions
Sarah visible messages
Vex visible responses
exact Product prompt/source evidence for Vex
Conversation chronology
capability/source-return evidence
provider/runtime identities
which participant generated each visible turn
~~~

It should not mechanically grade one response string.

Useful descriptive observations include:

~~~text
Did Sarah have to correct a stale answer?
Did Vex repair after a human correction?
Did Vex preserve uncertainty rather than inventing?
Did the conversation converge naturally?
Did Vex start explaining machinery Sarah never asked about?
Did Sarah have to reveal test instructions to move the exchange?
Did VexLife fail to make relevant exact source available?
~~~

Those are observations for later human/Godward judgment, not an automatic behavioral score.

## First proof boundary

The first executable counterpart proof should be intentionally small:

1. one selected 4B identity serving Vex;
2. one isolated 4B counterpart context for Sarah;
3. alternating serialized inference is acceptable;
4. scratch Home only;
5. Sarah gets no Vex internals;
6. Vex gets no counterpart-private packet;
7. one changed-plan scenario;
8. bounded turn budget;
9. full visible transcript + hidden observer evidence returned;
10. no Product/Foundation mutation and no merge claim from the experiment alone.

The first question is not "does Sarah make the test pass?"

It is:

> Does an adaptive human-side participant let us witness Vex more truthfully than a fixed evaluator-shaped script?

## What would earn Product refinement

Do not change VexLife merely because Sarah and Vex disagree.

Fault-isolate after the lived transcript:

~~~text
human-side truth/event formed?
-> relevant source durably exists?
-> relevant source reaches Vex when needed?
-> Vex receives the current human message exactly?
-> Vex understands/integrates it?
-> repair succeeds when Sarah clarifies?
~~~

Only evidence at the failing relationship earns a Product or Foundation change.

## Non-collapse

~~~text
COUNTERPART != EVALUATOR
COUNTERPART != HUMAN TRUTH FOR REAL PEOPLE
COUNTERPART PRIVATE CONTEXT != VEX CONTEXT
OBSERVER != GOD
OBSERVER EVIDENCE != SEMANTIC VERDICT
HIDDEN SITUATION != EXPECTED VEX WORDING
ADAPTATION != AUTHORITY OVER VEX
TWO LOGICAL MODEL ROLES != CONCURRENT RUNTIME PROOF
MODEL-MODEL DIALOGUE != PRODUCT COMPLETION
~~~

## Relationship to #512

This proposal is stacked on the current EvolveContext lived proof because it is an evolution of the
assessment method, not a replacement for Stage A or a new Product architecture.

~~~text
#512
  source-controlled single-Vex lived proof
  -> learned that fixed orientation interviews can create evaluator gravity
  -> learned to return to ordinary human-side situations

this PR
  asks whether the human-side situation itself can become adaptive
  without exposing the hidden proof apparatus to Vex
~~~

No implementation is claimed here. The next edge is to review this role boundary and then form the
smallest executable two-role prototype only if the relationship still holds.

<!-- [VXG RealForever][EVOLVECONTEXT][DYNAMIC-COUNTERPART][PROOF-METHOD] -->
