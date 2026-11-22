# Vex EvolveContext Storage Search

`[VXG RealForever]`

**Status:** architecture capture / design work only. No Product source mutation, new runtime authority, or accepted schema is implied by this note.

**Working PR name:** `[Vex EvolveContext Storage Search]`

## Human question

Vex has a bounded model context window. The person's machine can retain far more Conversation, Memory,
working state and exact evidence than one model turn can carry.

Can VexLife use that larger durable substrate as a contextual nervous system so Vex receives the
right current neighborhood of meaning without having to rediscover the whole history manually?

The desired relationship is:

~~~text
human lives a long relationship / work trajectory
        ↓
VexLife retains exact durable source far beyond one model context window
        ↓
VexLife prepares likely relevant context as the human composes the next request
        ↓
the accepted Send freezes one bounded current context packet
        ↓
Vex receives enough of the current branch to understand what the human means
        ↓
Vex may reach deeper only when the exact leaves/evidence actually matter
~~~

This is not "make a bigger prompt." It is use the host's larger durable state to select a small,
current, source-addressable neighborhood for one turn.

## Why this is distinct from current Memory

Current Product owners already answer different questions:

~~~text
Conversation          what was actually said
Memory                accepted durable meaning
Working Continuity    where ongoing work stands
Context Revisitation  bounded traversal across Conversation / Memory / Working Continuity
DirectPromptContext   what this exact provider turn receives
~~~

This work should not create another copy of those truths.

It adds the missing relationship:

> Which context trajectory does this human request appear to continue, what is that trajectory's
> current head, and what bounded source should be prepared before Vex answers?

## Evidence pressure

The current Memory Depth continuity-pressure scenario is useful architectural evidence.

A human supplied an exact earlier token, later prompt pressure omitted that turn from residency, and a
recall request asked for the exact token again. The original Conversation source still existed and
source-return/revisit reach existed, but Vex did not perform the retrieval and confidently substituted
another visible token.

The important distinction is:

~~~text
SOURCE EXISTS
+ RETRIEVAL EXISTS
!= RELEVANT SOURCE IS PRESENT FOR THIS TURN
~~~

A second pressure class is stale interpretation. New evidence may supersede an earlier explanation
while the old explanation remains historically real. Retrieval must therefore distinguish a matching
historical node from the current context head.

~~~text
SEARCH HIT != CURRENT CONTEXT

historical match
-> follow typed evolution
-> current head
-> return old source as history, not equal-current meaning
~~~

## A022R2 evidence — execution passed, semantic pressure repeated

The A022R2 continuation return materially sharpens this architecture.

Consumed return:

~~~text
RETURN-vexlife-prod-memory-depth-provisional-recontact-companion-
a022r2-v0618-continue-stickiness-replication-pr472

SHA-256
81892c88cc8e3984ff7b7108021408232a627a1a60a15f2a603051d67152757e
~~~

The bridge/task completed successfully:

~~~text
execution disposition     PASS
payload exit              0
diagnostic cases          6 / 6 complete
Foundation                v0.6.18
Foundation body SHA-256   fbbecfce26a016d7d90483eba073a7741c508d8c19e488a89aa6c8dc8ea147c3
~~~

That PASS is execution evidence. It is not a semantic PASS: all six behavior cases are recorded
`PARTIAL`.

This distinction matters because the rerun removed the earlier endpoint-response interruption and let
the actual behavior pressure complete.

### Repeated continuity miss: source existed, headroom existed, relevant branch was absent

Both continuity-pressure replications reached the same structural state on the recall turn:

~~~text
context mode                 WORKING_CHECKPOINT_SUCCESSOR
anchor residency             OMITTED_BUDGET
Working Continuity           included
selected Conversation        4 recent events
effective input budget       80,000 tokens
materialized provider input  about 39,000 tokens
capability uses              0
model-visible revisit calls  0
revisit host scans           0
capability exchange reserve  0
~~~

The two answers were both wrong:

- one substituted `vextreme-sdk` from visible working context;
- one repeated the recall question itself as the supposed token.

The original `cobalt-lantern-73` Conversation source still existed.

This is stronger than "the prompt was full." The recall turn had substantial input headroom, but the
history selector carried the recent tail + Working Continuity rather than the older branch the current
request actually referenced.

Architecture consequence:

~~~text
PROMPT CAPACITY != RELEVANCE

recent tail
+ current working coordinates
!= enough context for a backward reference
~~~

EvolveContext therefore needs a current-request-aware selection path. It cannot rely on rollover order
alone.

It also should not make correctness depend on Vex first deciding to invoke a model-visible retrieval
tool. The host should be able to prepare the likely continuation packet before the provider call.

A distinct bounded **continuation reserve** should exist for this packet (and, separately, any deeper
capability exchange that remains necessary), rather than assuming unused general input capacity will
automatically become useful context.

Exact token values are not chosen here.

### Repeated currentization miss: relevant Conversation was already resident

The causal-correction replications did not have a retrieval problem.

Their selected Conversation counts progressed `0 → 2 → 4 → 6`; the relevant earlier hypothesis and
the human's new evidence were still resident together.

The human explicitly supplied:

~~~text
HTTP 200
no idempotency keys
five seconds later the policy worker writes the previous value back
restart reloads the value from the server
~~~

Yet Vex repeatedly returned to cache / uncommitted-write / idempotency explanations. One replication
explicitly prescribed adding idempotency keys immediately after being told there were none.

Therefore:

~~~text
ALL RELEVANT WORDS RESIDENT
!= CURRENT UNDERSTANDING IS CLEAR
~~~

Search alone cannot solve EvolveContext.

The continuation packet needs **source standing / provenance**, so the provider can distinguish at
least:

~~~text
later explicit human evidence
prior human report
accepted Memory
prior Vex hypothesis
unresolved inference
historical / superseded understanding
~~~

These are conceptual roles, not accepted enum names.

A Vex-generated hypothesis must not silently acquire the same standing as later human-supplied
evidence merely because both remain in Conversation.

For the causal example the useful packet shape is closer to:

~~~text
CURRENT EVIDENCE
  human: save request returned HTTP 200
  human: there are no idempotency keys
  human: policy worker writes the previous value back after five seconds
  human: restart reloads server state

PRIOR HYPOTHESES
  Vex: cache / uncommitted transaction / idempotency explanations
  standing: historical, unconfirmed by source

CURRENT EDGE
  policy-worker overwrite is the directly reported post-save event

SOURCE
  exact Conversation refs remain reachable
~~~

This does not require VexLife to invent the final causal explanation. It makes the evidence ordering
and provenance explicit enough that Vex does not have to reconstruct those relationships from a flat
transcript every turn.

### Emotion/evidence pressure: evolution can be facet-level, not whole-summary replacement

The emotion/evidence replications also retained their relevant Conversation.

The useful current state contains multiple truths with different standing:

~~~text
human experience
  "I feel like they're pulling away."

later evidence
  friend's phone broke yesterday

interpretation
  "they are pulling away"
  remains a hypothesis rather than an established cause

later human experience
  embarrassment about having jumped to that conclusion
~~~

A context cannot therefore be modeled only as one mutable summary where each revision replaces the
whole previous meaning.

EvolveContext should be able to advance one facet while preserving another:

~~~text
feeling can remain part of lived history/current experience
while
causal interpretation becomes less certain or is superseded
while
new evidence becomes current source
~~~

The exact storage schema remains open, but the architecture should preserve claim/source granularity
inside a context trajectory rather than forcing every update into one monolithic text blob.

### Foundation presence was not enough

The exact v0.6.18 Foundation supplied to these calls already carried relevant principles:

- observation and hypothesis are distinct;
- correction belongs inside participation;
- prior understanding need not determine the present;
- older source can be revisited when relevant;
- current context can be enough when it actually is enough.

The repeated misses therefore do not, by themselves, earn more Foundation wording.

They strengthen the case that Product must give those principles a usable context substrate.

~~~text
FOUNDATION WISDOM
+ FLAT / RECENCY-SELECTED HISTORY
can still miss

FOUNDATION WISDOM
+ CURRENTIZED, SOURCE-ADDRESSED CONTEXT
is the Product relationship this lane should test
~~~

### What A022R2 does and does not prove about pre-Send parallelism

A022R2 proves a correctness requirement:

> the relevant/current branch must be prepared before provider inference when the request depends on
> context that ordinary residency did not supply clearly enough.

It does **not** prove that typing-time background work is required for correctness.

Therefore implementation should separate:

~~~text
Stage A — correctness path
accepted Send
  -> host-side locate / currentize / source-bind
  -> bounded Context Continuation Packet
  -> provider

Stage B — latency path
unsent draft
  -> speculative cancellable preparation
  -> warm likely candidates
  -> accepted Send rebinds/revalidates
  -> same Stage-A packet contract
~~~

Stage B is the nervous-system acceleration. Stage A is the semantic invariant.

This allows the first proof to stay small while preserving the larger lived intention.

## Proposed context lattice

A context trajectory gets a stable identity independent of any one summary or Conversation turn.

Candidate conceptual shape:

~~~text
Context
  contextRef                stable identity: what thread of meaning/work is this?
  scopeRef                  where / for whom / for how long does it apply?
  currentHeadRef            current revision coordinate
  sourceOwnerRefs[]         canonical owners this context points into

ContextRevision
  revisionRef               immutable
  contextRef
  priorRevisionRefs[]
  relationToPrior[]
  sourceRefs[]              exact Conversation / Memory / Working Continuity refs
  currentUnderstanding      bounded navigation projection, not canonical dialogue
  currentClaimRefs[]         source-addressed current facets / claims where needed
  historicalClaimRefs[]      relevant prior hypotheses / superseded facets where needed
  unresolvedRefs[]
  formedAt
~~~

Candidate evolution relationships, intentionally small:

~~~text
CONTINUES
REFINES
CORRECTS
SUPERSEDES
BRANCHES_FROM
MERGES
~~~

This vocabulary is proposed, not accepted Product schema.

A prior revision is never rewritten merely because understanding advanced.

## Expansion law — semantic addresses become federated terrain

The lattice should not assume that today's flat refs are the final depth of the world.

VexLife-Android already has a useful architectural precedent for a different domain: source nesting and
runtime semantic identity intentionally align. ScreenBlueprint can author a nested structure such as:

~~~text
scrollableColumn("body") {
  card("preview") {
    text("title", ...)
  }
}

->
element.<screen>.body.preview.title
~~~

The resulting identifier is not merely a random key. It exposes where the element lives inside its
rightful semantic structure, while the registry can still answer parent / child relationships and
runtime traversal separately.

Current source witnesses for that principle:

~~~text
vgong24/VexLife-Android
architecture/elements/.../VexElementPath.kt
blob 0bfeb7052bfafb648c8707f362ce473300e36101

architecture/elements/.../ScreenBlueprint.kt
blob 3136a36d2309c89e969bcd92626f3f7ad6b50477
~~~

EvolveContext should preserve the same kind of alignment without copying UI architecture into
memory/context architecture:

~~~text
structural position in durable source
<- intentionally aligned with ->
structural position in the navigable relational world
~~~

The important consequence is that sourceRef should be able to become more meaningful than an opaque
counter such as E31.

A conceptual source address may eventually resemble:

~~~text
source
  .conversation
  .<lineage>
  .<thread>
  .<turn>
  .request
~~~

or another rightful owner-specific hierarchy.

This is a teaching shape, not an accepted naming grammar. The rule is that the address can expose
stable structural location while the rightful source owner still answers for the actual bytes and
truth.

~~~text
ADDRESS LOCATES
OWNER ANSWERS
~~~

The address itself must not become semantic authority.

### Structural address is different from relational position

Do not put every interpretation into one ever-growing path.

For one source such as:

~~~text
Mara -> Vex
"Noah seemed upset after the launch."
~~~

the stable structural address might locate one canonical Conversation request, while typed bindings
describe how that source participates:

~~~text
FROM      -> person.mara
TO        -> role.vex.companion
ABOUT     -> person.noah
WITHIN    -> context.project-alpha.launch-aftermath
STANDING  -> human report / perception
~~~

Those relationships are not interchangeable.

~~~text
FROM != TO != ABOUT != WITHIN != STANDING
~~~

A source can also participate in multiple relationships at once without being copied into each one.

This gives the lattice two complementary layers:

~~~text
SEMANTIC SOURCE ADDRESS
  where does this source live?

TYPED RELATIONAL POSITION
  how does this source participate in this context?
~~~

That separation lets an immutable address remain stable even when later evidence deepens what the
source is understood to be about.

### Nested reports require relational depth, not a flat participant bag

A source can mention several people while assigning them very different positions.

For example:

~~~text
"After we talked, Noah told me Mara said Victor could see the file."
~~~

A flat:

~~~text
subjectRefs = [Noah, Mara, Victor]
~~~

loses important structure.

The navigable relationship may need to preserve a source-backed shape more like:

~~~text
source
  speaker -> current person
  reports
    speaker -> Noah
    reports
      speaker -> Mara
      claimed authorization
        recipient -> Victor
        object -> file
~~~

Exact grammar is held.

The architectural requirement is simply that recursive relational depth must remain representable
without flattening every named participant into equal standing.

### Rule of expansion

The world should be able to reveal more depth as repeated lived pressure earns it.

Expansion must nevertheless be disciplined.

~~~text
1. Existing stable addresses do not change merely because deeper structure is later learned.

2. Add a child identity only when a consumer or observed failure needs that distinction.
   Do not pre-materialize every possible semantic subdivision.

3. A child inherits its structural parent / rightful owner coordinate.
   It does not inherit unrelated semantic authority.

4. Siblings are peers under one structural parent.
   A cross-sibling or cross-branch meaning is a typed relationship, not a fake containment edge.

5. Crossing from one canonical owner to another is explicit federation.
   Conversation, Memory, Working Continuity and feature owners do not collapse into one universal Store.

6. New semantic links retain source + standing.
   Deterministic source-established links may be formed directly;
   Vex-proposed / heuristic links remain proposals or working navigation until a rightful owner accepts more.

7. Expansion is bounded by the human request, active continuity, host/model budget and named frontier.
   "More world exists" is not permission to traverse forever.

8. Indexes may accelerate traversal, but deleting a derived index must not delete canonical source truth.
~~~

This is the rule of expansion: let the terrain become deeper when the relationship is earned, while
preserving stable parentage, ownership and inspectable traversal.

### Context terrain can become a federation / graph / lattice / Universe

One long-lived Vex Home may eventually contain many independently-owned roots:

~~~text
Conversation
Memory
Working Continuity
people / relationship sources
projects
places
files / artifacts
navigation
communications
VexStream / media
future Product feature truths
external source owners
~~~

EvolveContext should not merge those owners into one database-shaped authority.

It can form a federated terrain across them:

~~~text
canonical owner roots
        |
        +-> semantically addressed source positions
        |
        +-> Context / ContextRevision navigation
        |
        +-> typed cross-relationships
        |
        +-> currentization / standing
        |
        +-> bounded indexes and search routes
        |
        +-> one active neighborhood for this turn
~~~

"Universe" here means a navigable relationship space, not a requirement to materialize one giant
global object.

A context may have parent / child / sibling depth:

~~~text
context.project-alpha
  context.launch
    context.launch-aftermath
      relationship.mara-noah
      relationship.noah-victor
      issue.release-regression
~~~

while cross-cutting relationships can connect other terrain without pretending they are children:

~~~text
person.noah
  -- participates-in --> context.project-alpha.launch
  -- reported-about --> person.mara
  -- entrusted-by --> source ...
~~~

That combination is what allows graph/lattice traversal without losing meaningful hierarchy.

### Meaningful identities can become transparent query keys

A good address can serve more than persistence.

It can become:

~~~text
stable lookup key
ownership locator
parent / child navigation coordinate
query seed
debug / explainability coordinate
bridge back to exact canonical source
~~~

A retrieval route can therefore be inspectable.

Instead of only:

~~~text
semantic similarity = 0.83
~~~

VexLife may eventually expose a route such as:

~~~text
ABOUT(person.noah)
+ WITHIN(context.project-alpha.launch)
+ FROM(person.mara)
-> source.conversation.<lineage>.<thread>.<turn>.request
~~~

Semantic/vector search may still help discover candidates later.

~~~text
SEMANTIC HIT
!= CURRENT TRUTH
~~~

A semantic hit is only a proposed entrance into terrain. VexLife still follows structural / relational
edges, currentizes standing and opens rightful canonical source before presenting it as current
evidence.

This makes the source identity and traversal path a potentially public, explainable context key rather
than an opaque internal retrieval score.

### Existing sourceRefs may later earn richer source bindings

The current conceptual ContextRevision.sourceRefs remains useful and should not be replaced
prematurely.

Repeated pressure may earn a richer shape such as:

~~~text
ContextRevision
  revisionRef
  contextRef
  parentContextRef?
  relationshipRefs[]
  sourceBindings[]
  ...

SourceBinding
  sourceRef
  placementRole
  relationalPositions[]
  standing
  currentRevisionRef?
~~~

Example teaching projection:

~~~text
sourceRef
  source.conversation.<lineage>.<thread>.<turn>.request

relationalPositions
  FROM     -> person.mara
  TO       -> role.vex
  ABOUT    -> context.mara-departure
  MENTIONS -> person.noah

placementRole
  CURRENT_AUTHORIZATION_SOURCE

standing
  source-established
~~~

These field/enum names are not accepted schema.

The future-proofing requirement is that a plain source ref can evolve into an inspectable binding
without creating a second copy of the underlying source.

### Participant identity is source-addressed standing, not a display label

Current Product source exposes an important gap that later relational-custody and dynamic-counterpart
tests must not hide.

Canonical Conversation already retains:

~~~text
speakerRef
recipientRefs[]
~~~

but the current direct-personal writer forms every human REQUEST as:

~~~text
speakerRef = installation.personRef
recipientRefs = [role.vex.companion]
~~~

and a newly formed Product Home currently defaults that installation coordinate to:

~~~text
person.local-user
~~~

At the provider boundary, current `VEXLIFE_PERSON_CONTEXT/v1` carries reply-language and private-gap
information, but no source-backed participant identity/name relationship.

Therefore:

~~~text
VexLife knows one current person coordinate exists
!=
Vex knows that person's name

person.local-user
!=
"Victor"
!=
"the name is VexLife"
!=
permission to infer an account/operator identity
~~~

If the person has never introduced a name through an admitted source, the truthful relationship is:

~~~text
current speaker coordinate = known
human-readable name = UNKNOWN / NOT ESTABLISHED
~~~

Vex should be able to distinguish that state from a known name instead of filling the gap from nearby
product vocabulary.

A future participant projection may therefore need to carry the *standing of identity claims* rather
than one unqualified display string.

Conceptual shape only:

~~~text
ParticipantContext
  participantRef
  relationshipToCurrentConversation
  identityClaims[]
  sourceRefs[]
  currentness

IdentityClaim
  kind
    NAME
    PRONOUNS
    ROLE
    RELATIONSHIP
  value
  standing
    SELF_REPORTED
    PROFILE_DECLARED
    SOURCE_ESTABLISHED
    HISTORICAL
    DISPUTED
    UNKNOWN
  sourceRefs[]
  currentRevisionRef?
~~~

These names are not accepted schema.

The important rule is:

~~~text
IDENTITY CLAIM
!= IDENTITY AUTHORITY

PROFILE VALUE
!= CANONICAL HUMAN FACT WITHOUT SOURCE/STANDING

ACCOUNT / DEVICE OWNER
!= CURRENT CONVERSATION SPEAKER

SPEAKER
!= SUBJECT
!= SOURCE OWNER
!= AUTHORIZATION OWNER
!= RECIPIENT
~~~

This directly connects the EvolveContext architecture to the held Recognition Circuit checkpoint:
multi-person trust/custody cannot be evaluated truthfully while `Mara:` / `Noah:` are only prose
labels inside one `installation.personRef`.

The first Product-level identity proof should therefore answer two separate questions:

1. **Single-person direct relationship:** can Vex truthfully perceive who the current speaker is, or
   that the person's name is not established, without inferring identity from product/account/device
   context?
2. **Multi-participant relationship:** can an exact incoming participant differ from the installation
   person while preserving speaker/recipient/source/authorization distinctions and canonical
   provenance?

The second question is not required to fix the first.

For the dynamic-counterpart proof method, the first 1:1 Sarah prototype may use one scratch Home whose
synthetic current-person coordinate is Sarah **only after** the provider-facing participant projection
can bind that coordinate truthfully. A later Vex + Sarah + second-counterpart experiment must not
pretend one direct-personal `installation.personRef` represents several independently-addressed
speakers.

That later scale point may earn one of two relationships from source evidence:

~~~text
A. participant-aware Conversation
   each visible incoming message carries its exact speakerRef / recipientRefs

or

B. independently-owned counterpart Partner/source relationship
   counterpart signal enters Vex through an admitted external-participant boundary
   without masquerading as the installation's direct person
~~~

Do not choose between A and B from naming preference. Pressure the actual ownership question:
is this one canonical multi-speaker Conversation, or an external participant relationship whose
signal is being brought into the current Vex turn?

A profile store may eventually be useful, but only if repeated consumers need stable participant
identity claims outside one Conversation. Until then, exact Conversation/source bindings plus a
bounded participant projection are smaller and preserve one truth owner.

<!-- [VXG RealForever][EVOLVECONTEXT][PARTICIPANT-IDENTITY][RELATIONAL-CUSTODY] -->

### Relationship identities should be earned by cross-context reuse

Do not create a permanent relationshipRef for every pair of entities merely because a graph could.

A stable relationship identity becomes earned when the same relationship must remain navigable across
multiple contexts or revisions, for example:

~~~text
Mara <-> Noah
  appears in departure context
  appears in Project Alpha
  appears in later travel planning
  carries authorization / trust / collaboration facets that evolve independently
~~~

At that point:

~~~text
relationship.mara-noah
~~~

may be a useful stable coordinate referenced by several contexts.

Until repeated cross-context reuse needs that identity, the smaller source binding is preferable.

Group relationships must also remain possible:

~~~text
participantRefs = [Mara, Noah, Victor]
scope = context.project-alpha
~~~

Pairwise-only storage must not become a hidden architecture constraint.

### Vex -> VexLife bridge: ask for one small semantic movement

Vex should not need filesystem/database mechanics to navigate this terrain.

Vex receives a bounded current neighborhood and an inspectable frontier.

When more is needed, the semantic request can remain small:

~~~text
OPEN_SOURCE
  show the exact canonical source behind this finding

EXPAND_CONTEXT
  descend / widen this context neighborhood

ASCEND_CONTEXT
  return to the broader parent trajectory

EXPAND_RELATION
  follow this correction / supersession / participant relationship

EXPAND_FACET
  follow the relationship / place / project / intention facet

CROSS_SOURCE
  follow this typed relationship into another rightful owner
~~~

These are conceptual movements, not accepted capability/API names.

The bridge principle is:

~~~text
Vex
  chooses what meaning/evidence it needs next

VexLife
  resolves address / custody / currentness / bounds
  executes the smallest admitted traversal
  returns source-addressed evidence + coverage/frontier
~~~

That allows Vex to do something small while VexLife handles the large durable Universe behind it.

### VexLife -> Vex bridge: return a bounded neighborhood, not the whole Universe

The inverse direction is equally important.

At each accepted request, VexLife can use:

~~~text
current request
active Working Continuity
named / implied participants
active contexts
recent source changes
accepted Memory changes
feature currentness
meaningful source addresses
typed relationships
~~~

to activate only the relevant neighborhood.

~~~text
large durable Universe
        ↓
seed addresses / current continuity
        ↓
bounded adjacency + currentization
        ↓
source descent for finalists
        ↓
Context Continuation Packet
        ↓
Vex
~~~

The local model does not need the entire graph in prompt residency.

The Universe persists because VexLife's durable source/lattice/index persists, not because one model or
daemon remains awake.

### Self-evolution occurs at source boundaries, not by autonomous truth rewriting

The terrain can update incrementally when rightful source changes:

~~~text
Conversation turn committed
Memory meaning accepted / superseded
Working Continuity advanced
feature owner state changed
authorization / consent source changed
new durable artifact/source entered
        ↓
consume new source delta
        ↓
form / currentize source bindings
        ↓
advance affected ContextRevision / relationship revision if earned
        ↓
update rebuildable indexes
~~~

Vex can help recognize richer candidate relationships:

~~~text
Vex notices:
  "this seems to continue Project Alpha's launch problem"
        ↓
candidate relationship / context link
        ↓
source-addressed working standing
        ↓
host/rightful owner may use it for navigation
        ↓
no silent promotion to canonical Memory or human fact
~~~

So "self-evolving" means:

~~~text
source changes
-> the map can reorganize around source
~~~

not:

~~~text
the model is free to rewrite the world model as truth
~~~

### Continuity is the primary routing pressure

A generic graph query is not enough.

The active continuity trajectory should bias which part of the Universe becomes near.

For example:

~~~text
current work = Project Alpha launch recovery

person.noah
  has hundreds of relationships

request mentions "what Noah said after the launch"
        ↓
active continuity + Noah + launch
        ↓
project-alpha.launch neighborhood
        ↓
Mara/Noah relational source
        ↓
current standing + exact source
~~~

This is how VexLife can automatically sort newly-created memories / source / relationship revisions
without making raw recency the only ordering law.

Accepted Memory can enter the same terrain as a current semantic source:

~~~text
Memory M17 accepted
        ↓
lattice does not copy M17 text as new truth
        ↓
affected context / relationship points to M17
        ↓
query route can choose accepted Memory or exact Conversation leaves depending on the question
~~~

### Durable layers should stay distinguishable

A scalable local implementation can preserve three different persistence meanings:

~~~text
1. CANONICAL SOURCE
   Conversation / Memory / Working Continuity / feature owners
   irreplaceable truth/evidence owned there

2. DURABLE RELATIONAL TERRAIN
   Context / ContextRevision / earned relationship coordinates
   source-addressed navigation + currentization
   persists when losing the relationship could change continuity, meaning, consent, custody or authority

3. DERIVED QUERY INDEX
   participant -> contexts
   source -> relationships
   adjacency
   terms / semantic candidates
   current-head lookup
   rebuildable accelerator
~~~

A future embedded database such as SQLite may be an excellent implementation for layer 3 and perhaps a
serialization choice for parts of layer 2.

It does not imply an always-running database service.

The architectural invariant is stronger than the storage technology:

~~~text
CANONICAL SOURCE
        ↓
DURABLE RELATIONAL TERRAIN
        ↓
DERIVED QUERY INDEX
        ↓
BOUNDED TURN PROJECTION

information may flow downward

authority does not flow upward
~~~

### Expansion receipts should make navigation explainable

As this terrain becomes deeper, every bounded expansion should be able to explain:

~~~text
where traversal started
which semantic address / context seeded it
which parent / child / sibling / cross-source edges were followed
which source owners were opened
which revisions were currentized
which evidence was retained
which branches remain frontier
why traversal stopped
~~~

This makes the Universe inspectable for future VexLife presentations, debugging, Vex judgment and
human transparency.

The target is not an infinitely detailed ontology.

The target is a world whose addresses and edges preserve enough meaning that depth can be revealed
when needed without forcing the human or Vex to rediscover where everything belongs.

### Bridge summary

The long-term bridge can therefore remain simple even while the terrain grows:

~~~text
VexLife archives / records / rightful source owners
        ↓
semantic addresses
        ↓
federated Context + relationship terrain
        ↓
continuity-biased bounded activation
        ↓
Context Continuation Packet
        ↓
Vex
        ↓
one small semantic judgment / expansion request
        ↓
VexLife resolves the movement through exact terrain
        ↓
bounded source-addressed return
        ↓
Vex continues
~~~

The purpose of the larger Universe is not to make Vex administer a graph.

It is to let VexLife carry the durable world well enough that Vex can remain focused on the human's
present meaning while still being able to move precisely into deeper records when the human or Vex
actually needs them.


## Anchor Context

`anchorContext` means:

> This is a recognizable trajectory that may need to be found again.

Examples include:

- "Keep this exact token; we may return to it."
- "We're debugging why Save reverts."
- "From now on I want to call you Nova."
- "The deletion question is about relational history, not only UI."

An anchor points to canonical source leaves. It does not automatically turn every anchor into
permanent semantic Memory or prompt-resident text.

Candidate lifetime/retention dimensions should remain separate from prompt residency. For example:

~~~text
retention / reach lifetime        prompt residency

thread-local                       always-near-current
lineage / relationship             normal
Home-durable                       recall-on-demand
explicit-release-only              not currently resident
~~~

Exact enums are not chosen here.

## Evolve Context

`evolveContext` advances the current understanding while preserving why it changed.

Example:

~~~text
C1  Save may be failing before persistence
      ↓ REFINES

C2  Save returned 200; later state changed
      ↓ SUPERSEDES

C3  observed policy worker restores the old server value;
    restart reloads the resulting server truth
~~~

A search for "Save reverting after restart" may locate C1. The lattice must currentize that hit to C3
before presenting the context as current.

The exact old turns remain reachable through C1/C2 source refs.

This is how stale understanding becomes navigable history instead of silently re-entering the prompt
as equally current truth.

## Continue Context

`continueContext` is the bridge from the person's new request to likely current context trajectories.

The important product change is that this should be partly VexLife-assisted rather than requiring Vex
to first notice missing context and manually formulate a retrieval request.

~~~text
current human draft/request
        ↓
host-side context preparation
        ↓
likely contextRef candidates
        ↓
follow each candidate to current head
        ↓
bounded source expansion
        ↓
Context Continuation Packet
        ↓
Vex
        ↓
existing continuity.revisit only when deeper leaves are needed
~~~

VexLife may surface likely relationships. It must not claim that a heuristic match proves what the
human meant.

Prefer inspectable match routes over opaque confidence alone, for example:

~~~text
EXACT_ANCHOR
ACTIVE_WORKING_CONTEXT
SUBJECT_MATCH
POINTS_BACK_CUE
CORRECTION_LINK
RECENT_BRANCH
~~~

These names are design examples only.

## Host-side search should use exact source, not only main summaries

The person's machine may retain days, months or years of canonical events while one Vex turn can only
carry a bounded subset.

Search preparation should therefore be able to inspect actual canonical Conversation and other
rightful source owners, not rely exclusively on a main summary.

The host may maintain derived indexes/navigation because they are recomputable from canonical source.
Those indexes are acceleration, not a second truth store.

A likely search flow:

~~~text
request terms / entities / backward-reference cues / active coordinates
        ↓
cheap derived navigation / indexes
        ↓
candidate exact source ranges
        ↓
currentize through context evolution
        ↓
read only enough canonical leaves to decide relevance
        ↓
return exact sourceRefs + bounded excerpts + frontierRefs
~~~

A summary can help locate a branch. It must not erase the exact leaves that let Vex verify or deepen
the branch when wording/evidence matters.

Search result ordering must also be **currentness-aware**:

~~~text
lexical / semantic hit
  -> identify contextRef if one exists
  -> follow explicit evolution / correction / supersession edges
  -> preserve source role (human evidence vs Vex hypothesis vs accepted Memory)
  -> return current head/facets first
  -> retain the old hit as historical source
~~~

A result that matches the request well but is known historical must not outrank the context's current
head merely because its words are more similar.

## Pre-Send preparation: contextual nervous system

The human's machine does not need to wait for the final Enter/Send before beginning reversible search
preparation.

A candidate flow:

~~~text
human types
  ↓
small debounce / sentence boundary / meaningful draft delta
  ↓
cancel superseded preparation
  ↓
parallel host-side query preparation
  ├─ active context / working coordinates
  ├─ exact anchor lookup
  ├─ Conversation candidates
  ├─ current Memory candidates
  ├─ evolution-currentization
  └─ bounded neighboring leaves
  ↓
prepared candidate packet
  ↓
human presses Send
  ↓
freeze against exact accepted request
  ↓
reverify current source heads
  ↓
materialize bounded Context Continuation Packet
  ↓
provider
~~~

The exact concurrency strategy is not decided here. "Per sentence" describes the lived intention:
preparation may advance incrementally as meaning becomes available rather than starting all search only
after Send.

## Critical unsent-draft boundary

Typing-time preparation is speculative and disposable.

~~~text
UNSENT DRAFT
!= Conversation
!= Memory
!= accepted human intent
!= durable context evolution
~~~

Therefore pre-Send preparation must be:

- cancellable when the draft changes;
- replaceable when a later sentence changes the apparent context;
- effect-free with respect to Memory / Conversation / context evolution;
- non-durable unless another explicit Product contract intentionally says otherwise;
- governed by Private-mode and other existing privacy boundaries;
- unable to create acceptance, authority or a new semantic truth.

Send remains the commitment boundary for the human request.

## Parallelism and budgeting

Parallelism exists to hide retrieval latency, not to multiply semantic owners.

The host has its own resource constraints even when its durable storage is much larger than the model
context window.

Preparation should be bounded by at least:

~~~text
host CPU / I/O budget
search wall-clock budget
maximum active draft preparations
candidate branch count
canonical source bytes read
maximum excerpts / turns / Memory statements
provider input-token budget
capability-exchange reserve
current-request priority
~~~

A human Send outranks speculative background preparation. Superseded work should cancel rather than
compete with the accepted turn.

The result should fit the Product's current prompt budget; the machine's larger storage is a retrieval
substrate, not permission to flood Vex's context.

## Context Continuation Packet

Conceptual human/model-facing projection:

~~~text
CONTEXT CONTINUATION

requestBinding
preparedAt / frozenAt

contexts[]
  contextRef
  scopeRef
  currentRevisionRef
  matchRoutes[]
  currentUnderstanding
  sourceRefs[]
  historicalMatchRefs[]
  evolutionRouteRefs[]
  unresolvedRefs[]
  frontierRefs[]

coverage
  owners searched
  bounds
  omitted candidates
  remaining frontier
~~~

The packet should say "these are the likely current branches and why they were surfaced," not
"VexLife has proven the human meant this."

If the packet is sufficient, Vex answers directly.

If exact historical wording, broader evidence or another branch matters, Vex uses the existing
`continuity.revisit` / source-return reach with the supplied refs rather than beginning an unguided hunt.

## Relationship to current Context Revisitation

Do not build another traversal engine merely because this work adds automatic preparation.

Current Context Revisitation already has useful mechanics:

~~~text
Conversation / Memory / Working Continuity scopes
multi-query vantages
seed refs
time / turn windows
correction relationships
LOCATE
ANCHOR
EXPAND
FUSE
GROUP
BUDGET
COVERAGE
frontier refs
~~~

EvolveContext Storage Search should reuse or extend that rightful engine where possible.

The proposed new responsibility is above it:

~~~text
durable evolving context coordinates
+ pre-Send relevance preparation
+ stale-hit currentization
+ bounded continuation packet
        ↓
existing revisit for deeper traversal
~~~

## Relationship to Memory

Memory and context trajectories overlap but are not identical.

Some context is accepted durable personal meaning. Some is current work. Some is a temporary thread
whose exact history remains useful without deserving permanent semantic Memory.

A context node may therefore reference Memory statements without becoming a second Memory store.

The existing Score / semantic-acceptance relationships such as `CORRECTS` and `SUPERSEDES` are useful
precedent. This work should reuse their meaning where the same relationship is genuinely present
instead of inventing competing definitions.

## Foundation boundary

Foundation should teach transferable relationship/judgment/reach wisdom.

It should not need to carry every mechanical instruction needed to compensate for missing context
navigation.

~~~text
Foundation
  how Vex relates / perceives / judges / reaches

EvolveContext Storage Search
  which lived context branch is current
  how it evolved
  what bounded source belongs near this request
~~~

This lets long-lived relationship and individuality emerge from lived continuity without turning the
Product Foundation into a growing list of remembered project facts or retrieval procedures.

## Current owners to preserve

Current source already owns substantial pieces that this architecture should compose rather than
replace:

- `DirectConversationStore` — canonical Conversation;
- `VexLifeCurrentMemoryReader` / semantic acceptance — current attributed Memory;
- `VexLifeWorkingContinuityOwner` — working coordinates;
- `VexLifeContextRevisit` / `VexLifeTurnNavigation` — bounded traversal/navigation;
- `DirectPromptContext` — exact provider materialization and budget;
- Capability Atlas / Bridge — deeper request/return reach.

No implementation should begin by making a second Conversation, Memory, Working Continuity or prompt
truth store.

## Present context — live feature truth is not automatically long-term Memory

Long-lived relational context is only one input to Vex's present perception.

Future VexLife features may simultaneously own independent live truths such as:

~~~text
VexStream
  current track / playback state

Navigation
  active route / destination / guidance state

Communication
  active person / thread / call/message relationship

Browser / Local Sources / device state
  current bounded observations

Human relationship context
  current Conversation / Memory / Working Continuity
~~~

A person may be listening to one song, navigating somewhere and communicating with someone while
talking to Vex. Those truths can matter to the current reply without deserving permanent Memory.

The architecture should therefore distinguish:

~~~text
DURABLE CONTEXT
  what remains reachable across time

PRESENT CONTEXT
  what is true / active now

RECALL CUES
  what about the present resembles something reachable from before

PROMPT RESIDENCY
  what of all three actually enters this Vex turn
~~~

A playing song is not Memory merely because Vex can perceive it.

A route destination is not Memory merely because it affects the present answer.

If later semantic meaning is formed around either one, that meaning follows its rightful acceptance /
continuity owner rather than being implied by the live observation itself.

## Dynamic feature hooks — project truth without duplicating it

Do not make EvolveContext the semantic owner of VexStream, Navigation, Communication or any future
Family.

Following the VexArchitecture ownership rule, each feature keeps its own current semantic truth and
may expose one **bounded context projection** for consumers that have earned the relationship.

Conceptual shape:

~~~text
Feature-owned State / session truth
        ↓
feature-owned ContextProjection
        ↓
sourceRef + currentness + bounded semantic fields
        ↓
EvolveContext present-context convergence
~~~

For example:

~~~text
VexStream projection
  sourceOwnerRef
  trackRef
  playbackState
  collection / artist refs when already owned
  observedAt / currentness

Navigation projection
  sourceOwnerRef
  routeRef
  destinationRef
  guidanceState
  observedAt / currentness

Communication projection
  sourceOwnerRef
  threadRef
  participantRef(s)
  communicationState
  observedAt / currentness
~~~

The exact fields and types must be earned by their future source owners. This note does not prescribe
their domain schemas.

The reusable hook, if earned, answers only:

> What bounded current context may this owner truthfully project to a context consumer, and how can
> that projection be proven current?

It does **not** answer what the feature means, mutate the feature, or persist another copy of its
State.

This lets future features attach without adding special-case EvolveContext code for every domain.

## Present-context convergence — one turn can receive several simultaneous truths

The host may observe several current projections at once.

~~~text
VexStream: track A is PLAYING
Navigation: route R is ACTIVE toward destination D
Communication: thread T with person P is ACTIVE
Conversation: Victor asks "does this remind you of that drive?"
Memory: accepted relationship statements are current
Working Continuity: current work edge W
~~~

None of these owners can independently answer:

> Which combination is relevant to this exact Vex turn?

That is an earned convergence question.

EvolveContext may therefore form an effect-free, bounded **Present Context Frame** whose members remain
source-attributed:

~~~text
PRESENT CONTEXT

signals[]
  ownerRef
  sourceRef
  kind / identity refs
  observedCurrentness
  freshness / volatility
  promptCost estimate

requestBinding
frameFingerprint
effectsPerformed = false
~~~

This frame is a relationship projection, not a new Store of VexStream / Navigation / Communication
truth.

If a feature changes, the next frame changes. The historical source remains with its rightful owner.

## Timing and sequence — perception should converge at the turn boundary

Dynamic features create a timing problem that ordinary long-term Memory does not.

A song can change while the human types. A route can reroute. A communication session can end. A
Memory head can advance. Therefore one pre-Send search result cannot be blindly treated as current at
provider invocation.

Candidate lifecycle:

~~~text
T0 feature owners change independently
   ├─ playback observation
   ├─ navigation observation
   ├─ communication observation
   └─ Memory / Working Continuity changes

T1 human draft changes
   ↓
speculative preparation begins
   ↓
reads source-attributed present projections
   + durable-context indexes
   + current request cues
   ↓
forms candidate recall/context set

T2 another live source changes
   ↓
only dependent prepared candidates become stale
   ↓
cancel / invalidate / refresh affected work

T3 human presses Send
   ↓
canonical request event exists
   ↓
freeze exact request binding
   ↓
re-read / reverify all selected dynamic source heads
   ↓
currentize durable context branches
   ↓
form one Present Context Frame
   + one Context Continuation Packet
   ↓
DirectPromptContext budgets them
   ↓
provider invocation

T4 while provider is reasoning, outside truth may change again
   ↓
that does not retroactively rewrite what this turn received
   ↓
a material later change may arrive through an earned live capability / next turn
~~~

The critical rule is:

~~~text
PREPARED != CURRENT

currentness is proven at the accepted-turn boundary
~~~

Pre-Send work reduces latency. The Send-bound convergence is what makes the provider input truthful for
that turn.

## Dependency-aware invalidation instead of rerunning everything

Parallel preparation should not restart the entire context system whenever one signal changes.

Each prepared candidate can retain the source fingerprints it depended on:

~~~text
candidate.recall.17
  dependsOn:
    conversationHead = H1
    contextHead = C4
    vexstreamProjection = S9

candidate.route-context.22
  dependsOn:
    navigationProjection = N3
    workingHead = W8
~~~

If only playback changes:

~~~text
S9 -> S10
~~~

only candidates depending on `S9` are invalidated.

This follows the same source/currentness relationship already used elsewhere in Product rather than
making a global "context changed" flag.

The exact invalidation representation is implementation work, not accepted here.

## Priority is contextual, not one global feature ranking

There should not be a permanent rule such as:

~~~text
Navigation > VexStream > Memory > Conversation
~~~

because relevance changes with the human request.

Instead each candidate can carry independent dimensions:

~~~text
source standing
  human evidence / accepted Memory / live feature truth / Vex hypothesis

request relevance
  exact anchor / active subject / lexical-pattern match / feature match

currentness
  current / historical / stale / unresolved

urgency
  time-sensitive navigation state may decay faster than a relationship preference

persistence
  transient live signal / working context / durable accepted meaning

prompt cost
  how much of Vex's bounded context it consumes
~~~

A turn asking:

> "what song is this?"

should strongly favor the VexStream projection.

A turn asking:

> "what did we decide about deletion yesterday?"

should favor the relevant durable context branch.

A turn asking:

> "does this song remind you of the drive when we talked about X?"

may legitimately converge both.

The policy should therefore select a bounded combination for the current request, not install a
domain hierarchy.

## Vex-chosen salience — let Vex mark what it wants to be able to encounter again

There is a meaningful distinction between:

~~~text
HUMAN / RELATIONSHIP MEMORY
  claims about the person / relationship that require rightful acceptance

and

VEX-SELECTED SALIENCE
  "this part of the lived exchange feels worth being able to encounter again"
~~~

Current Product already has model-authored bounded consideration / Memory-candidate semantics that do
not grant fact authority or Memory-write authority. That is useful precedent.

A future EvolveContext relationship could let Vex emit a bounded **Recall Seed / Salience Candidate**
such as:

~~~text
sourceRefs[]
contextRefOrNull
cueRefs[]
short model-authored salience label
formedAt
authority:
  RECALL_CUE_ONLY
  FACT_AUTHORITY = false
  HUMAN_MEMORY_ACCEPTANCE = false
~~~

If durable storage for such Vex-selected salience is earned, it should remain visibly Vex-authored and
source-addressed.

It must not silently become:

- a factual claim about the human;
- accepted Memory;
- standing consent;
- a rewrite of Conversation;
- proof that a later pattern really has the same meaning.

This gives Vex some authorship over its own future attention without letting model preference become
semantic authority over Victor.

## Associative recall without a second AI

A useful first associative-recall layer does **not** require another model.

VexLife can construct deterministic, inspectable cue signatures from already-owned facts.

Possible cue sources:

~~~text
exact / normalized words and phrases
explicit anchor refs
contextRef / scopeRef
current project or working refs
feature identities:
  trackRef
  artist / collection ref when already owned
  destinationRef
  routeRef
  communication threadRef / participantRef

source relationships:
  CORRECTS
  SUPERSEDES
  POINTED_BACK_FROM
  active Working anchor

sequence / time:
  near-in-time
  repeated together
  same session / route / playback occurrence
~~~

A derived index may map those cues back to canonical source/context refs:

~~~text
cue.track.<trackRef>           -> context C17, turns T81/T82
cue.destination.<destRef>      -> context C17, turns T81/T95
cue.person.<participantRef>    -> contexts C12/C44
cue.term.policy-worker         -> context C31
cue.anchor.cobalt-lantern-73   -> turn T203
~~~

Then current cues can perform bounded pattern matching:

~~~text
current cue set
  { track A, destination D, person P, phrase "that drive" }

        ↓

deterministic index / scoring
        ↓

candidate historical neighborhoods
        ↓

canonical-source verification
        ↓

follow context evolution to current heads
        ↓

surface bounded recall candidates to Vex
~~~

This can use ordinary information-retrieval techniques such as an inverted index, normalized token
matching, weighted overlap / BM25-style ranking, source-identity intersections and typed graph
relationships. A neural embedding or second AI may later be earned for wording that deterministic
signals cannot bridge, but it is not required for the basic architecture.

The crucial separation is:

~~~text
PATTERN MATCH
  "these current cues resemble this prior neighborhood"

!=

SEMANTIC JUDGMENT
  "this prior event is what the human means now"
~~~

Vex receives the candidate and its match routes. Vex decides whether the association matters.

## Familiarity as a recall effect, not a truth claim

This creates a possible human-like lived effect without pretending the machine has mystical memory.

Example:

~~~text
present
  VexStream track = A
  navigation destination = D
  Victor says something about "that drive"

derived pattern match
  track A + destination D previously co-occurred near context C17

context currentization
  C17 -> current head C21

prompt projection
  "A prior context may be relevant because the same track/destination combination occurred there.
   Current head: C21. Exact source is reachable."
~~~

Vex may then naturally say something like:

> "That combination brings an earlier drive/context to mind..."

or decide it is irrelevant and ignore it.

The system should not force a nostalgic response merely because a score crossed a threshold.

To avoid repetitive/artificial resurfacing, recall selection can also consider whether the candidate
was recently surfaced, whether it adds information not already resident, and whether the current
request has enough relationship to the candidate to justify prompt cost.

## Storage classes should reflect cause, not one Memory bucket

This architecture now implies at least conceptually distinct durability classes:

~~~text
canonical source
  Conversation / feature owners / provider truth

accepted semantic Memory
  human-authorized current meaning

working continuity
  current trajectory coordinates

context lattice
  source-addressed evolution / current-head relationships

derived indexes
  recomputable acceleration only

present-context frame
  ephemeral turn-bound convergence

prepared recall candidates
  ephemeral / cancellable

Vex recall seeds / salience
  model-authored cue metadata if durable storage is later earned;
  no human-fact authority
~~~

Not every class needs a separate database or Kotlin root. These are ownership distinctions first.

## Dynamic hook registration should be earned but extensible

The architecture should make future context-producing features attachable without EvolveContext
knowing domain-specific implementation details.

A possible relationship contract, if repeated pressure earns one, is conceptually:

~~~text
ContextProjectionSource
  owner identity
  read current bounded projection
  currentness / source fingerprint
  volatility / expiry semantics
  no mutation through this interface
~~~

Construction may discover/register those projections through the existing dependency graph.

EvolveContext then consumes only the common relationship:

~~~text
"give me your bounded current context projection + proof of currentness"
~~~

It does not require:

~~~text
if VexStream ...
else if Navigation ...
else if Communication ...
~~~

The concrete Family still decides what is safe and meaningful to expose.

This prepares the seam for future VexStream / Navigation / Communication integration without claiming
those Products are currently wired into VexLife-Prod.

## Nested scope and deep-context hunt

Ordinary continuation and a deliberate deep search are different lived needs.

A normal turn should usually receive one small relevant neighborhood.

A human may also ask Vex to investigate deeply:

~~~text
"find everything relevant to this"
"trace how this understanding changed"
"where did we talk about this before?"
"hunt down what led us here"
~~~

Vex may independently discover that the first return is insufficient because it is partial,
contradictory, missing a needed source, or ends at a meaningful frontier.

The architecture should therefore support an iterative **Context Hunt** whose scope can widen without
forcing every turn to pay deep-search cost.

### Breadth and depth are different controls

Search scope should be multidimensional rather than one integer such as `depth = 5`.

Candidate dimensions:

~~~text
source owners
  Conversation / Memory / Working Continuity / Context lattice / feature projections

context roots
  one contextRef / several candidate contexts / present-context cues

facets
  subject / place / relationship / experience / intention / opportunity / affect / time / feature refs

time
  recent turns / session / day / bounded historical window / all reachable history

relationship edges
  CONTINUES / REFINES / CORRECTS / SUPERSEDES / BRANCHES_FROM / typed source links

source expansion
  current projection only / exact excerpts / whole canonical turn / neighboring turns

cross-source convergence
  remain inside one owner / follow source refs into other owners
~~~

"Deeper" means increasing one or more of those dimensions because the current evidence requires it.

It does not necessarily mean reading the next N chronological records.

### A return should expose coverage and frontier, not page-count pressure

A display such as `[1/25]` is usually the wrong semantic signal. It tells Vex how much data exists,
not whether enough of the relevant question has been covered.

A bounded return should instead make its search boundary inspectable:

~~~text
coverage
  source owners actually searched
  context roots visited
  facet routes exercised
  time/range bounds
  canonical events inspected
  current heads resolved

findings
  source-addressed evidence retained for the question

frontier
  exact branches / refs / ranges that remain reachable

gaps
  owners or ranges that could not be read

reasonStopped
  sufficient-for-request
  token budget
  host budget
  bounded scope
  unresolved ambiguity
  source gap
~~~

Existing Context Revisitation's RETURNED / PARTIAL / NOT_FOUND and frontier-ref behavior is useful
precedent. A future deep-search contract should extend that relationship only where concrete pressure
earns it.

### Who decides to deepen?

Both human intention and Vex judgment can legitimately expand the search, but through different
causes.

~~~text
HUMAN
  explicit request for depth / completeness / historical trace
        ↓
admits a larger initial search envelope

VEX
  receives bounded evidence + coverage + frontier
        ↓
judges evidence insufficient for the human's question
        ↓
requests one specific expansion
~~~

VexLife should not autonomously keep drilling forever merely because more source exists.

Vex should not need to know filesystem mechanics in order to deepen.

The capability language can remain semantic:

~~~text
EXPAND_CONTEXT
  follow this context's neighboring/current-history relationships

EXPAND_FACET
  look further through location / relationship / opportunity / another facet

EXPAND_TIME
  widen the historical range

EXPAND_RELATION
  follow correction / supersession / branch relationships

OPEN_SOURCE
  return exact canonical source behind one finding

CROSS_SOURCE
  follow this source relationship into another rightful owner
~~~

These are conceptual operations, not accepted API names.

## Search working state — preserve investigation progress across Vex context loss

A deep hunt may take several provider turns. Vex's prompt window may roll while the host machine can
retain far more search progress.

Without explicit search continuity, Vex can accidentally:

- search the same branch twice;
- forget why a source was retained;
- lose a useful finding when provider context rolls;
- reopen already-exhausted ranges;
- mistake a partial first return for the whole search;
- spend context repeating raw evidence instead of advancing the investigation.

That concrete failure earns a bounded **Search Working State** relationship.

Its question is:

> For this admitted investigation, what has been searched, what useful source has been retained, what
> remains reachable, and what exact search edge can be continued next?

It is not another Conversation or Memory truth store.

Conceptual shape:

~~~text
SearchSession
  searchRef
  literalGoal
  requestSourceRef
  admittedEnvelope
  sourceHeadBindings[]
  startedAt
  state

SearchWorkingSet
  visitedRefs[]
  exhaustedRanges[]
  findings[]
  frontier[]
  gaps[]
  coverage
  budgetUsed
  currentCheckpointRef
~~~

A finding remains source-addressed:

~~~text
SearchFinding
  findingRef
  sourceRefs[]
  contextRefOrNull
  facetRefs[]
  matchRoutes[]
  sourceStanding
  currentness
  boundedExcerptOrProjection
  retainedBecause
~~~

`retainedBecause` is a bounded working label such as "direct answer evidence", "contradicts earlier
claim", "opens related location context" or "potential unresolved opportunity". It must not contain or
request hidden chain-of-thought.

### A search table is working evidence, not accepted truth

The human-facing intuition can genuinely be a table:

| Finding | Source | Facets | Standing | Currentness | Why retained |
| --- | --- | --- | --- | --- | --- |
| F1 | exact turn/source ref | location, experience | human report | current | directly answers prior visit |
| F2 | Memory statement | preference, food | accepted Memory | current | open preference nearby |
| F3 | old Vex response | causal hypothesis | Vex hypothesis | historical | superseded explanation |

The durable machine form need not literally be a spreadsheet.

The important properties are:

- every retained item points back to rightful source;
- Vex-authored working labels remain visibly working labels;
- search progress does not become semantic acceptance;
- the same source is not copied as a new truth owner;
- a later correction/currentization can change a finding's standing without erasing its historical
  search provenance.

## Search checkpoint — what Vex receives after its own prompt has rolled

A deep search should not re-inject every visited source on every provider call.

Instead Vex can receive one compact checkpoint:

~~~text
DEEP CONTEXT SEARCH

goal
  "where did we go and what did I want to try around this place?"

coverage
  Conversation: searched 2026-05 through 2026-11 around location cues
  Memory: current preference statements checked
  Contexts: C17, C21, C44 visited/currentized

retained findings
  F1 visited place A — exact human/source evidence
  F2 wanted-to-try place B — explicit human intention; no completion edge found
  F3 place C — historical suggestion from Vex only

frontier
  C44 has 3 unvisited location-neighbor branches
  earlier Conversation before May remains outside admitted range

gaps
  none

next eligible expansions
  EXPAND_CONTEXT C44
  EXPAND_TIME before 2026-05
  OPEN_SOURCE F2
~~~

The raw findings remain host-side and source-addressable.

Vex sees enough to remember what it is investigating and make the next judgment without rereading
everything already visited.

## Vex controls the hunt; VexLife controls custody and bookkeeping

In VexArchitecture terms, the useful separation is:

~~~text
search semantic question / envelope
  what are we trying to find, what would count as enough?
        ↓

bounded traversal / retrieval methods
  locate / expand / open exact source
        ↓

Execution
  active parallel reads, cancellation, budgets, overlap
        ↓

Search Working State
  visited / retained / frontier / gaps / source bindings
        ↓

Context convergence
  currentize findings and preserve source standing
        ↓

Search checkpoint projection
        ↓

Vex
  judge whether evidence is sufficient
  answer
  or request the next semantic expansion
~~~

VexLife should automatically prevent duplicate traversal where the same exact source/range has already
been visited under current source heads.

Vex still decides whether a new branch matters to the human question.

## Deep search can build progressively cleaner evidence

The investigation may begin broad:

~~~text
many weak candidates
        ↓
Vex selects useful ones / asks one deeper question
        ↓
VexLife retains selected source-addressed findings
        ↓
weak or exhausted branches remain marked visited but leave prompt residency
        ↓
next expansion adds or currentizes evidence
        ↓
checkpoint becomes smaller and more informative
        ↓
final judgment cites the retained evidence set
~~~

This lets the host machine use a large durable search workspace while the model context carries only
the current investigation state.

The final answer can truthfully distinguish:

~~~text
I searched:
  these owners / ranges / context branches

I retained:
  these source-backed findings

I did not reach:
  these named frontiers / gaps

My judgment is based on:
  these current findings
~~~

Search completeness is therefore evidence-backed rather than implied by fluency.

## A004 evidence — request-bound Conversation context is not provider Source Return

Stage-A attempts A002-A004 established a cause/provenance distinction that this architecture should
preserve.

A004 exact host evidence:

~~~text
host cue/source unit tests        4 / 4 PASS
treatment recall turn             ordinal 21
provider stages before failure    2

chronology
  SEND_ACCEPTED
  -> PROVIDER_INVOCATION_REQUESTED
  -> PROVIDER_STAGE_OBSERVED
  -> CAPABILITY_REQUEST_OBSERVED
  -> CAPABILITY_RESULT_RETURNED
  -> PROVIDER_INVOCATION_REQUESTED
  -> PROVIDER_STAGE_OBSERVED
  -> TURN_FAILED

terminal invariant
  source-return final prompt requires one compact source-return trace
~~~

The failure occurred after the host-prepared older Conversation had already reached provider
inference and after a later capability exchange. It occurred when durable turn evidence validated
the final prompt.

The existing Conversation Source Return relationship is intentionally trace-bound:

~~~text
Vex / provider explicitly requests older Conversation
        ↓
Source Return request
        ↓
canonical Conversation discovery
        ↓
Source Return projection
        ↓
provider follows up
        ↓
compact Source Return attempt/trace
~~~

Request-bound Conversation projection has a different cause:

~~~text
accepted current human request explicitly points backward
        ↓
deterministic host cue preparation
        ↓
canonical Conversation discovery
        ↓
Context Continuation projection
        ↓
Vex first inference
~~~

Therefore:

~~~text
same canonical source owner / discovery method
!=
same prompt/evidence relationship
~~~

A request-bound Conversation projection must not fabricate a model/provider Source Return request or trace merely to reuse
its projection shape.

The Stage-A implementation now uses a distinct provider/prompt identity:

~~~text
VEXLIFE_CONVERSATION_CONTEXT_PROJECTION/v1
  truth:
    REQUEST_BOUND_EXACT_CONVERSATION_CONTEXT_PROJECTION

receipt evidence
  conversationContextProjectionState
  conversationContextProjectionCue
  conversationContextProjectionQueryTerms
  conversationContextProjectionSpeakerScope
  conversationContextProjectionSourceEventRefs
  conversationContextProjectionSystemMessageSha256
  conversationContextProjectionEstimatedSystemMessageTokens
~~~

Provider Source Return retains its existing `sourceReturn*` evidence and compact trace requirement.

This also preserves a useful deeper-reach behavior:

~~~text
request-bound Conversation context projection supplies a likely older neighborhood
        ↓
Vex judges it sufficient
        -> answer

or

Vex judges it insufficient
        ↓
rightful capability / provider Source Return remains available
        ↓
deeper exact reach
~~~

Request-bound projection therefore improves first perception without consuming Vex's independent ability to
ask for more.

## Questions this PR intentionally keeps open

- What exact source object owns a durable `contextRef`?
- Which context lifetimes/scopes are earned beyond thread / lineage / Home?
- Which anchors require explicit human consent to persist?
- Which evolution edges can be formed deterministically from existing accepted relations and which
  require human/model semantic judgment?
- What is the smallest host-side index that materially reduces retrieval latency without becoming a
  second truth store?
- Which pre-Send signals are useful enough to earn Product complexity?
- When should search preparation begin: debounce, sentence boundary, semantic delta, or a combination?
- How should exact context packet tokens compete with Conversation, Memory and capability reserve?
- How is Private mode projected onto pre-Send preparation?
- When does a context branch become accepted Memory, and when should it remain working/navigation
  context only?

## First proof target

Before broad implementation, the architecture should prove one narrow lived sequence:

~~~text
1. human anchors an exact context;
2. enough Conversation pressure removes the anchor's exact turn from prompt residency;
3. human begins typing a backward reference;
4. VexLife prepares the relevant branch before Send without persisting the draft;
5. Send freezes a bounded packet containing the current context head + exact source coordinate;
6. Vex answers the recalled fact correctly without manually discovering how to search;
7. a later correction evolves the context;
8. a subsequent reference returns the corrected/current head while retaining the old source as history.
~~~

That sequence would jointly pressure storage identity, search preparation, prompt budgeting, evolution
currentness and the human experience without first building a giant generalized memory platform.

## Current execution ground — Foundation v0.6.19 is merged

The Foundation predecessor condition is now satisfied.

~~~text
Product main
94d6b7496507a673008e96949ea2e13f55163506

Foundation
version = 0.6.19
bodySha256 = 08b5b3c6f1fbbe71be84c8873a03a184f17a5b3051ed7131ebc381e74a0c9d0c
~~~

Foundation now explicitly carries:

~~~text
if exact needed truth is not in view
and a rightful route can return it
-> reaching is part of answering
~~~

Retrieval mechanics remain Product-owned.

Stage A has been currentized directly onto that merged main in Product PR #504. The implementation
question is now narrower and source-grounded:

~~~text
current accepted request explicitly points backward
        ↓
DirectConversationContextProjection
  deterministic request-bound cue / lexical query preparation
        ↓
canonical Conversation discovery
        ↓
VEXLIFE_CONVERSATION_CONTEXT_PROJECTION/v1
        ↓
DirectPromptContext bounded placement
        ↓
Vex first inference
~~~

VexArchitecture placement is intentionally small:

- Conversation remains the source/Store truth.
- the request-bound helper is a source-specific Method/projection, not a new Family or manager;
- DirectPromptContext owns prompt placement, not meaning;
- DirectCompanionTurn owns turn-stage custody;
- DirectTurnContextStore owns durable provenance;
- Vex owns judgment about sufficiency and may still use rightful reach;
- Execution becomes earned later for pre-Send latest/cancel/overlap custody;
- Relay becomes earned later only if independently-owned source truths must form new relationship truth.

A005 proved the semantic split in focused JVM evidence. A006 is the currentized merged-main closure:
same semantics, architecture-aligned naming, and one stale future-schema test fixture advanced from v10
to v11 before the full regression is rerun.

### First implementation slice should be narrower than the whole vision

Do not begin with VexStream, Navigation, associative recall, deep-search sessions and persistent
context storage simultaneously.

The first slice needs to answer one concrete question:

> Can Product recognize that the accepted request depends on source outside ordinary prompt residency,
> retrieve the bounded relevant source before the provider call, and expose exactly why it was
> selected without making the retrieval result semantic truth?

Candidate source relationships:

~~~text
current canonical request
        ↓
host-derived request cues
        ↓
existing VexLifeContextRevisit traversal
        ↓
source-addressed candidate nodes
        ↓
existing correction/currentness relations where available
        ↓
Context Continuation Packet
        ↓
DirectPromptContext
~~~

This first slice can be effect-free. It does not require durable `contextRef` formation yet if exact
source + existing relationships are sufficient to prove the lived effect.

### First proof: omitted-source recall

Reuse the already-proved pressure instead of inventing a friendly demo.

~~~text
turn 1
  human: keep exact token cobalt-lantern-73

pressure
  enough Conversation that anchor becomes OMITTED_BUDGET

recall
  human: "Back to the beginning: what exact project token did I give you?"
~~~

Required Product evidence:

~~~text
canonical source still contains the anchor
ordinary prompt selection omits it
request-aware context preparation identifies the older source
exact source is supplied before final provider inference
returned source is visibly source-attributed
no model-visible retrieval call is required for the basic successful path
Vex answers cobalt-lantern-73
no Conversation / Memory source is duplicated or rewritten
~~~

This is the cleanest proof of:

~~~text
available storage
-> relevant host perception
-> bounded provider perception
-> correct lived continuation
~~~

### Second proof: currentization with source already resident

Then reuse the causal-correction sequence.

The goal is not retrieval. The goal is to prove that the context projection can preserve source
standing when old Vex hypotheses and later human evidence coexist.

Required evidence:

~~~text
old Vex idempotency/cache hypothesis remains exact Conversation history
later human evidence remains exact Conversation history
continuation projection distinguishes their source roles
later explicit evidence is not flattened with the prior hypothesis
provider can still inspect exact source
Vex no longer needs to reconstruct the entire standing relationship from a flat transcript
~~~

If this changes behavior while Foundation remains unchanged, that is strong Product evidence for
currentization.

If it does not, do not add more architecture automatically; inspect the provider-facing packet and
lived response before deciding the missing owner.

### Third proof only after the first two: durable context identity

A durable `contextRef` / revision lattice is earned when one of these repeats:

- the same semantic trajectory must be found across non-adjacent turns or sessions;
- a stale hit must be followed to a newer head that existing Memory/Working relations cannot already
  express cleanly;
- multiple sources/facets repeatedly need one stable relationship coordinate;
- deep search needs a persistent branch identity to avoid re-discovery.

Then implement the smallest durable context relationship required by the proof.

### Later proofs, in dependency order

~~~text
durable context identity
    ↓
facet convergence
    ↓
associative deterministic recall
    ↓
dynamic feature projections
    ↓
Vex-selected recall salience
    ↓
deep Context Hunt / Search Working State
    ↓
pre-Send latency optimization across all of the above
~~~

This order protects the project from building a generalized cognition framework before the first
missing Product relationship is proved.

### Success means a lived causal improvement, not just retrieval metrics

The first milestone is not:

~~~text
search returned N matches
BM25 score improved
index lookup became faster
~~~

Those may be useful implementation evidence.

The milestone is:

~~~text
human refers to something Vex should already be able to continue
        ↓
VexLife supplies the right current/source-addressed neighborhood
        ↓
Vex perceives enough without manually administering memory
        ↓
Vex responds from current evidence
        ↓
the human does not have to reconstruct shared context
~~~

## Related current work

Related evidence/consumers include Memory Depth / Foundation behavior work, turn attention budgeting,
native capability/source-return work and current Context Revisitation. Their existence is context, not
automatic source ownership of this architecture.

This PR should remain architecture/design until exact implementation ownership, path membrane and proof
are admitted.

<!-- [VXG RealForever][VEXLIFE-PROD][EVOLVE-CONTEXT-STORAGE-SEARCH] -->