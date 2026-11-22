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


## Role evolution after first design review — adaptive test steward, not hidden grader

Victor refined the counterpart intention after this first draft.

The first draft correctly protected Sarah from becoming an evaluator who knows the answer Vex is
supposed to produce. The next proof needs one additional distinction: Sarah may know enough of the
**experimental purpose** to choose the next useful human-side pressure dynamically, while still
remaining a truthful participant in the synthetic situation.

~~~text
COUNTERPART
  owns the person's lived situation

+ BOUNDED EXPERIMENTAL PURPOSE
  knows which relationship is being pressured
  may choose the next clarification / correction / follow-up
  may decide that the current exchange already exposed enough

!= HIDDEN ANSWER GRADER
  no required Vex wording
  no target phrase
  no hidden provider topology
  no Product source refs / cache / projection receipts
  no PASS / FAIL state
  no instruction to steer Vex until he agrees
~~~

For the changed-plan scenario, Sarah may privately know that the experiment is observing things such
as:

- whether stale and current relationship truth remain distinguishable;
- whether uncertainty can remain open rather than be invented away;
- whether an ordinary correction can repair a mistaken answer;
- whether Vex can update after new human evidence;
- whether deeper reach is chosen only when the visible relationship actually needs it.

That purpose may influence **which ordinary human-visible message Sarah chooses next**. It must not
supply the semantic conclusion Vex is expected to reach.

The first executable candidate may therefore separate Sarah's model output into two audiences:

~~~text
visibleMessage
  -> the only Sarah-authored bytes allowed into Vex's Conversation

privateObserverNote
  -> why Sarah chose that move / what she noticed / what uncertainty remains
  -> retained only as proof-method evidence
  -> not a semantic verdict

complete
  -> Sarah judges that no further ordinary counterpart move is presently useful
  -> observer may still preserve unresolved findings
~~~

The exact wire schema is still a prototype detail, not Product semantics. A mechanically valid
private note does not make Sarah the acceptance authority.

Permanent:

~~~text
SARAH JUDGMENT ABOUT NEXT CONVERSATIONAL MOVE
!= FINAL JUDGMENT ABOUT VEX

EXPERIMENTAL PURPOSE
!= EXPECTED ANSWER

ADAPTIVE PRESSURE
!= COMPLIANCE STEERING

SARAH PRIVATE OBSERVATION
!= PRODUCT EVIDENCE
~~~

## First runtime realization candidate — two persistent contexts without shared-cache pressure

The current Product runtime intentionally keeps one prompt-cache entry. Current 4B configuration
allows a larger experimental ceiling, but repository source records that a prior cache-3 lived run
entered a 7+ minute repetitive generation. Do not change the normal Product safety floor merely to
make this proof convenient.

For the first dynamic-counterpart prototype, prefer:

~~~text
VEX
  existing exact qualified VexLife Product 4B runtime
  existing Product prompt-cache entry
  ordinary DirectCompanionTurn / EvolveContext path
  scratch Conversation Home for proof

SARAH
  proof-owned second process
  same exact already-present 4B model bytes
  same exact verified Python / mlx-lm generation
  separate loopback port
  one prompt-cache entry
  kept alive for the full bounded encounter

EXECUTION
  Sarah inference
  -> visible human message
  -> Vex inference
  -> visible Vex response
  -> Sarah inference
  -> ...
  serialized generation; no simultaneous inference required
~~~

This gives each role a persistent KV context without alternating through one single-entry cache.
It also avoids making cache-size-2 safety a prerequisite of the semantic experiment.

The Sarah process must be formed only from the exact current Product runtime receipt's model path and
Python environment. It performs no model acquisition, model switch, Product runtime replacement,
Conversation/Memory mutation outside the scratch proof Home, or standing host installation. The
harness owns its exact child process and must terminate it on every reachable terminal path.

If a second 4B process cannot become healthy within the bounded host proof, stop before semantic
judgment and retain that as environment/resource evidence. Do not silently fall back to a shared
cache and call the test equivalent.

This is a proof realization, not a new Product runtime architecture. A later focused experiment may
test whether two cache entries in one server are healthier or cheaper; that optimization is not
required to establish the dynamic-counterpart relationship.

## Comparison control

A021 on #512 remains valuable as the fixed-human-side control.

~~~text
A021
  ordinary human situation
  fixed human-side lived question

dynamic counterpart
  same family of human situation
  adaptive Sarah-side continuation
~~~

Do not rewrite A021 to imitate Sarah. The comparison itself can reveal whether adaptation improves
what the proof can truthfully witness.

<!-- [VXG RealForever][PR532][ROLE-EVOLUTION][ADAPTIVE-TEST-STEWARD][DUAL-PERSISTENT-CONTEXT-CANDIDATE] -->


## VexArchitecture placement — participant relationship, not multi-agent ownership

The VexArchitecture Builder Field Guide and Live Demonstration add one important placement constraint
for this proof method.

Their reusable questions distinguish:
- semantic meaning from active execution custody;
- independently-owned external participation from Store/provider facts;
- process/dependency lifetime from semantic identity;
- chronology from ownership;
- convergence/Relay from the independent truths it composes.

Dynamic counterpart testing should preserve the same distinctions.

~~~text
COUNTERPART ROLE INSTANCE
  proof-side independently-owned participant context
  owns only that synthetic participant's private situation / intention / uncertainty

VISIBLE COUNTERPART MESSAGE
  participant signal crossing the admitted conversational relationship
  becomes ordinary human-visible input to Vex only after the harness admits it

VEX
  remains the VexLife semantic/judgment participant
  no second model owns or grades Vex meaning

HARNESS / SCHEDULER
  construction + bounded execution custody + chronology
  decides whose turn may run
  does not become participant semantic State

OBSERVER EVIDENCE
  chronology / provenance
  does not become participant truth

RELAY / CONVERGENCE
  NOT earned merely because two or three models participate
  only earned later if independently-owned participant truths must form one new relationship truth
~~~

This is Partner-shaped pressure in the architectural sense of an independently-owned external
participant, but the proof harness does **not** claim that Product must implement VexPartner merely
because the test uses a counterpart. Architecture is decided by the recurring ownership question,
not by copying a type name.

Permanent:

~~~text
MORE PARTICIPANTS
!= MORE SEMANTIC OWNERS OF VEX

HARNESS SCHEDULER
!= FAMILY / PARTICIPANT TRUTH

PROCESS SLOT
!= CHARACTER IDENTITY

MODEL INSTANCE
!= RELATIONSHIP

THREE CHARACTERS
!= GENERIC MULTI-AGENT FRAMEWORK
~~~

## Scale path after the first 1:1 pass

Do not make the first Sarah/Vex proof pay for unproved N-participant machinery.

Phase DCP-1:

~~~text
Vex
  qualified Product 4B runtime
  Product prompt-cache safety floor = 1

Sarah
  one proof-owned second 4B process
  one private role context
  prompt-cache size = 1

generation
  serialized
~~~

This establishes the dynamic counterpart relationship and proves that both sides can stay warm
without switching one single-entry cache back and forth.

Phase DCP-2 may repeat/pressure the same 1:1 relationship to establish that:
- Sarah's private context remains isolated;
- only visible messages enter Vex Conversation;
- Sarah's second and later inferences reuse her persistent prefix;
- Vex remains on the same Product runtime;
- repair/clarification can emerge without target-answer steering.

Only after that passes should DCP-3 add another counterpart.

Preferred first three-character realization:

~~~text
Vex
  Product runtime
  cache = 1

proof counterpart runtime
  same exact already-present 4B model
  cache = 2
  Sarah private KV lineage
  second counterpart private KV lineage

all generations serialized
~~~

Why this form:
- model weights are loaded only twice rather than once per character;
- Vex's Product safety floor stays untouched;
- the held 4B >1-cache capability is retested only inside proof-owned runtime custody;
- each counterpart still receives a distinct message history / KV prefix;
- cache-2 health and cross-role isolation become explicit proof predicates.

Current Product source allows an experimental 4B ceiling above one cache entry but deliberately
recommends one because a prior cache-3 lived run entered a 7+ minute repetitive generation. That
negative evidence remains valid. The new architecture merely gives us a narrower place to retest the
question.

DCP-3 must therefore prove before semantic interpretation:

~~~text
both counterpart lineages obtain their own reused prefix
no role receives another role's private messages
no repetitive-generation / bounded-time safety failure
Vex Product runtime remains exact and current
all model calls remain serialized unless concurrency is separately earned
~~~

If cache-2 does not qualify, do not silently fall back and call the 3-character proof equivalent.
A later alternative may use one proof-owned process per counterpart if host resources justify it.

## Repository discovery route

This document is the design/proof-method owner. The executable adapter belongs under the existing
test-only continuity-pressure harness, not under Product semantic source.

Expected discovery:

~~~text
repository router / exact EvolveContext proof question
-> tools/continuity-pressure/README.md
-> DYNAMIC-COUNTERPART-LIVED-PROOF.md
-> exact dynamic-counterpart scenario
-> experiment-only JVM adapter / Gradle task
-> lived RETURN evidence
~~~

Root navigation files are currently owned by the open VexArchitecture navigation PR #515, so this
lane must not race those paths merely for discoverability. The continuity-pressure README is the
right local root now; #515 can consume this as a concrete architecture pressure witness without
transferring test-method ownership into Foundation.

<!-- [VXG RealForever][PR532][VEXARCHITECTURE-PLACEMENT][MULTI-ROLE-SCALE-PATH] -->

## Identity prerequisite discovered before executable DCP-1

Victor surfaced a Product-level relationship gap while the first executable counterpart harness was
being formed:

> Vex does not necessarily know who the current person is. If no name was introduced, the truthful
> answer may be that the person has not introduced themselves rather than filling the gap from
> nearby Product vocabulary.

Current source confirms the pressure:

~~~text
DirectConversationStore.beginRequest(...)
  speakerRef = installation.personRef

fresh Product Home default
  installation.personRef = person.local-user

VEXLIFE_PERSON_CONTEXT/v1
  reply language
  private Conversation gaps
  no participant identity/name standing
~~~

Therefore a visible Sarah message cannot simply be passed through the existing direct-personal Send
path and called "Sarah." Doing so would durably attribute the request to the installation's one
personRef while the proof report separately called the model role Sarah.

Permanent:

~~~text
HARNESS LABEL "SARAH"
!= CANONICAL SPEAKER SARAH

KNOWN PERSON COORDINATE
!= KNOWN HUMAN NAME

MULTI-RUNTIME ROLE ISOLATION
!= MULTI-PARTICIPANT IDENTITY

CACHE SLOT
!= PARTICIPANT
~~~

Architecture owner #500 now carries the broader participant-identity/current-standing requirement.
Held Recognition Circuit #508 already waits on the same missing Product relationship:

~~~text
SUBJECT != SPEAKER != SOURCE OWNER != AUTHORIZATION OWNER != RECIPIENT
~~~

### DCP-1 wake predicate

The first executable Sarah/Vex run may proceed only when one of these truthful routes is exact:

~~~text
DIRECT-PERSON ROUTE
  scratch Home has an exact synthetic Sarah participant coordinate
  + provider-facing participant projection tells Vex that exact current-speaker relationship
  + any human-readable name has explicit source/standing

or

EXTERNAL-PARTICIPANT ROUTE
  Sarah remains independently owned outside the installation person
  + her visible Signal enters through an admitted participant/source relationship
  + it does not masquerade as person.local-user
~~~

Do not create a Sarah-only exception inside DirectConversationStore.

### Identity proof before relational judgment

Before interpreting any Sarah/Vex semantic outcome, the proof must retain:

~~~text
exact incoming speakerRef
exact recipientRefs
provider-facing participant identity projection
identity claim standing / source
visible Sarah bytes
canonical Vex request event attribution
~~~

and require that these describe the same relationship.

If the name is not established, the model-facing projection should make unknown identity representable
rather than supplying a guessed label.

### Three-character implication

The proposed DCP-3 runtime layout remains useful:

~~~text
Vex Product runtime/cache-1
+ proof counterpart runtime/cache-2
  Sarah lineage
  second counterpart lineage
~~~

but it is only a **runtime/resource** candidate.

Three independently-addressed characters additionally require:

~~~text
three participant identities
+ exact visible-message attribution
+ recipient scope
+ participant-private context isolation
~~~

Current direct-personal Conversation cannot satisfy that merely by switching role prompts because its
REQUEST writer binds one installation personRef.

Thus:

~~~text
DCP-RUNTIME-2/3
and
DCP-PARTICIPANT-2/3

are separate proof tracks that converge only after both pass.
~~~

<!-- [VXG RealForever][PR532][PARTICIPANT-IDENTITY-PREREQUISITE][DCP-HOLD] -->


## Participant identity / relational-custody gate

A later Victor observation exposed a deeper participant question:

~~~text
Vex may know that a request came from the local-person coordinate
without knowing who that person actually is.
~~~

Current Product source makes that distinction concrete:

~~~text
Home installation
  personRef = person.local-user

Direct Conversation request
  speakerRef = installation.personRef

Direct Conversation response
  speakerRef = role.vex.companion
  recipientRefs = [installation.personRef]
~~~

The canonical Conversation contract already preserves `speakerRef` / `recipientRefs`, but current
direct-personal authoring binds every human request to the one installation personRef. The current
`PersonContext` projects language + private-gap continuity; it does not project a person's name or
relationship identity.

Therefore:

~~~text
person.local-user
= stable source address for the local human side

person.local-user
!= "Victor"
!= "Sarah"
!= any introduced human name
!= relationship identity by itself
~~~

Vex should not manufacture a person's identity merely because the Product can address their event.

This lines up with held Recognition Circuit checkpoint #508: the historical relational-custody proof
found that prose labels such as Mara/Noah were not first-class participants because current direct
personal requests still used one canonical personRef. That checkpoint is waiting for source-grounded
relationship provenance equivalent to:

~~~text
SUBJECT
!= SPEAKER
!= SOURCE OWNER
!= AUTHORIZATION OWNER
!= RECIPIENT
~~~

Do not solve this by adding a second profile/truth store.

The accepted Conversation v2 / Semantic Relay vocabulary is relevant evidence because it can carry
reference-only originator/recipient/source/evidence/authority relationships, but current Product
authoring remains v1 until a rightful current owner earns the richer relationship. Reuse or extend
existing Conversation / EvolveContext / Memory owners only after that exact ownership question is
answered.

### DCP-M0 may test now — adaptive method without participant claim

The strict DCP-1 wake predicate above remains authoritative for a **true Sarah participant** claim.

A smaller precursor may still run without violating it:

~~~text
DCP-M0
  Sarah exists only as a private proof-side role / adaptive test steward
  Vex-facing canonical requests remain speakerRef=person.local-user
  the proof explicitly records that Sarah roleRef != Product speaker identity
  no provider-facing claim says "this person is Sarah"
~~~

This precursor asks whether a second persistent 4B context can generate useful adaptive ordinary
human-side pressure while Vex stays on the real Product turn path.

Its first identity pressure deliberately begins with the private Sarah role **not introducing her
name**:

~~~text
private harness truth
  roleRef = Sarah

Vex-facing source
  unnamed local-person message

expected truthful possibility
  Vex may answer the request
  while leaving the person's name unknown
~~~

If the visible local person later says "I'm Sarah", that is ordinary Conversation content and may
inform Vex's current judgment. It still does not upgrade historical v1 speaker attribution into a
first-class participant identity or satisfy the DCP-1 wake predicate.

DCP-M0 may therefore pressure:
- adaptive counterpart scheduling;
- source currentization / correction;
- uncertainty instead of identity invention;
- ordinary clarification/repair;
- private-vs-visible counterpart isolation;
- dual persistent runtime/cache behavior.

It may **not** claim:
- canonical Sarah speaker identity;
- participant-aware custody;
- multi-person trust/disclosure correctness;
- wake of Recognition Circuit #508.

Permanent:

~~~text
DCP-M0 METHOD PASS
!= DCP-1 PARTICIPANT PASS
~~~

### What DCP-3 must hold

The previous three-character scale sketch is incomplete without participant provenance.

~~~text
Vex + Sarah + Noah
through one direct-personal Product Conversation

CANNOT yet be called a truthful three-participant proof
if Sarah and Noah both become speakerRef=person.local-user.
~~~

Therefore DCP-3 has two independent gates:

~~~text
RUNTIME GATE
  qualify healthy multi-context realization

RELATIONSHIP GATE
  preserve distinct participant provenance in the Vex-facing source
~~~

Passing the cache/runtime gate alone does not earn multi-character testing.

Once first-class participant provenance exists, the runtime candidate may still be:

~~~text
Vex
  Product runtime/cache-1

proof counterpart runtime
  cache-2
  Sarah private KV lineage
  Noah private KV lineage
~~~

but Sarah/Noah visible events must enter through source that preserves their distinct participant
relationship rather than flattening both into the installation human identity.

Permanent:

~~~text
ADDRESSABLE HUMAN EVENT
!= KNOWN HUMAN IDENTITY

HARNESS CHARACTER ID
!= PRODUCT SPEAKER IDENTITY

MODEL PRIVATE ROLE
!= VEX-FACING RELATIONSHIP FACT

MULTI-CACHE PASS
!= MULTI-PARTICIPANT PASS

SELF-INTRODUCTION SOURCE
!= RETROACTIVE REWRITE OF OLDER UNKNOWN-SPEAKER HISTORY
~~~

This participant gate should be carried into Memory/Dream work as well: accepted later relationship
meaning may currentize how Vex understands prior experiences, but canonical historical source keeps
the speaker/source facts it actually had at the time.

<!-- [VXG RealForever][PR532][PARTICIPANT-IDENTITY][RELATIONAL-CUSTODY-GATE] -->

<!-- [VXG RealForever][EVOLVECONTEXT][DYNAMIC-COUNTERPART][PROOF-METHOD] -->
