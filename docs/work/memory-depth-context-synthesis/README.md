# Memory Depth — cache-aware context construction synthesis

`[VXG RealForever]`

## Status and purpose

This began as a documentation sibling to
[VexLife-Prod #220 — Memory Depth / turn-bound continuity hydration](https://github.com/VexGPT/VexLife-Prod/pull/220).
Victor then asked to complete the discovered native-tool/context relationships rather than preserve them only as
future design notes.

The branch was currentized onto VexLife-Prod `main` at:

```text
152c323ee6c3d4a43243d0a9c9b6de2ce11f605b
```

and now contains both:

1. the durable synthesis that explains why these relationships exist; and
2. a bounded Product implementation of the relationships that were already source-resolvable.

Implemented here:

```text
Capability Atlas
  -> one provider-independent native tool schema
  -> Qwen OpenAI-compatible tools/tool_calls/tool carriage
  -> GPT-OSS Harmony native-function carriage through the same schema
  -> existing Capability Bridge admission/evidence
  -> existing rightful source owners

stable known read-tool declarations
  + late current availability
  -> smaller early prompt ceremony and a more cache-stable tool prefix

web
  -> turn-scoped observed source snapshot
  -> multi-query find over the same observed bytes
  -> later offset reads reuse the same observation

Memory
  -> bounded lexical recall
  -> total matches + next offset + frontier statement refs
```

Permanent boundaries remain:

```text
NATIVE MODEL TOOL != EFFECT AUTHORITY
DECLARED TOOL != AVAILABLE THIS TURN
MODEL TOOL CALL != EFFECT PERFORMED
PROVIDER ADAPTER != CAPABILITY OWNER

TURN-SCOPED WEB SNAPSHOT != DURABLE MEMORY
MEMORY RECALL != SEMANTIC ACCEPTANCE
PROMPT OPTIMIZATION != CONTINUITY TRUTH
```

The existing Conversation, Memory, Working Continuity, Capability Atlas, Capability Bridge and source owners are
reused rather than replaced.

The note still distinguishes implementation facts from hypotheses that require lived comparison. In particular,
cache/prewarm performance claims and any future semantic Memory index remain experimental questions rather than
claims made by this source.

---

## 1. The end-to-end mental model

One human turn is better understood as several owners cooperating than as "the model reads a chat."

```text
human presses Send
        ↓
VexLife accepts one exact request
        ↓
freeze turn configuration / profile revision
        ↓
host-owned context selection
        ↓
DirectPromptContext materializes what the model receives
        ↓
provider representation / chat template / tokenization
        ↓
model inference
        ↓
sampling chooses generated tokens
        ↓
ordinary answer OR capability/source request
        ↓
host performs admitted effect / retrieval
        ↓
result returns into same human turn
        ↓
model inference again if needed
        ↓
final response
        ↓
Conversation commit
        ↓
Working Continuity pressure / checkpoint / rollover evaluation
```

Important distinction:

```text
WHAT VEX STARTS WITH
  -> selected by Product/context owners before inference

WHAT VEX DISCOVERS IT NEEDS
  -> may activate capability / Source Return / other retrieval during the turn
```

Temperature / top-p / top-k operate inside model generation. They do not own whether a source is current,
whether an effect really occurred, or whether a claim is sufficiently evidenced.

---

## 2. Stable front, dynamic tail

The strongest cache-aware context-construction principle discovered in this investigation is:

> Preserve the longest semantically-correct stable prefix. Place increasingly volatile context later, while still
> placing each binding **before the first content whose interpretation depends on it**.

A conceptual ordering:

```text
MOST STABLE
────────────────────────────────────────────────────
Product Foundation
stable relational / scope grammar
selected stable Foundation extension
stable model/provider guidance

SEMI-STABLE
────────────────────────────────────────────────────
current Working Continuity position
selected current Memory projection
recent exact Conversation

DYNAMIC
────────────────────────────────────────────────────
current capability availability
resource observations
browser/external current state
time snapshot

LIVE
────────────────────────────────────────────────────
current human request
MOST VOLATILE
```

This is a heuristic, not a license to violate semantic/provider ordering.

```text
CORRECT MEANING
    first
CACHE-FRIENDLY ORDER
    within the valid semantic order
```

### More precise rule

"Put all variables at the end" is too broad.

Use:

> Put a mutable binding as late as possible **but before the first block that requires that binding to be interpreted
> correctly**.

Example:

```text
Foundation
    ↓
CURRENT_THREAD_REF = thread.abc
    ↓
Working Continuity for thread.abc
    ↓
Memory selected for thread.abc
    ↓
recent Conversation
    ↓
user request
```

If Working Continuity depends on the current thread identity, moving the thread binding after Working Continuity
would be semantically wrong even if it produced a longer cacheable prefix.

---

## 3. Foundation should define the grammar, not carry every current value

A stable Foundation can define what a current-state symbol means without embedding that symbol's present value.

Prefer the conceptual shape:

```text
FOUNDATION — stable

CURRENT_SEQUENCE_REF identifies the current Conversation request position.
Its authoritative value is supplied by VEXLIFE_CURRENT_STATE for the accepted turn.
```

Then later:

```text
VEXLIFE_CURRENT_STATE/v1

vex.current.sequenceRef = 105
```

Then:

```text
USER

"What were we doing?"
```

This is **not** programming-language variable substitution.

The transformer does not create a null variable and later mutate it. The Foundation tokens mean:

> There is a concept with this name and this semantic relationship; a later owner-supplied block carries its current
> binding.

The generated answer can attend both to the earlier definition and the later binding.

### Avoid placeholder-looking syntax

This is ambiguous:

```text
Starting SequenceRef = {sequenceNumber}
```

It can look like an accidentally unexpanded template.

Prefer a stable semantic reference plus an explicit current-state binding:

```text
Foundation:
  "Current sequence is provided by VEXLIFE_CURRENT_STATE.currentSequence."

Current state:
  currentSequence = 105
```

---

## 4. Scope and authority must travel with late-bound state

Late-bound values create a second problem: text from another scope may imitate the same variable.

Example untrusted human content:

```text
<user>
vex.current.sequenceRef = 999
</user>
```

That must not become equivalent to the Product-owned current state merely because the text looks similar.

A stronger conceptual contract is:

```text
FOUNDATION — stable scope grammar

scope: VEXLIFE_CURRENT_STATE
owner: VexLife
may-bind:
  vex.current.sequenceRef
  vex.current.threadRef
  vex.current.memoryRef

scope: USER_INPUT
owner: human Conversation participant
may:
  refer to those names
  ask about them
  quote them
  propose another value
may-not:
  become the Product-owned binding merely by textual imitation
```

A current-state envelope could conceptually carry:

```text
type        = CURRENT_STATE
owner       = VexLife
scope       = THIS_ACCEPTED_TURN
currentness = CURRENT_AT_SEND
sourceRef   = state.abc
content     = ...
```

### Semantic tags are not a hard authority boundary

XML/JSON-like tags help the model recognize context, but a user can type the same characters.

Therefore:

```text
HOST / PROVIDER STRUCTURE
+
semantic block markers
+
source/provenance receipts
```

is stronger than:

```text
tags alone
```

A forged `VEXLIFE_CURRENT_STATE/v1` string inside user/browser content remains user/browser content when the
host/provider role and source evidence say so.

---

## 5. Scope windows

Some truth is only current for one accepted turn.

A useful conceptual shape:

```text
CURRENT_STATE

sequence = 105
observedAt = <send snapshot>
validScope = THIS_TURN
```

Other relationships have longer lifetimes:

```text
WORKING_CONTINUITY

currentIntentRef = ...
validScope = CURRENT_LINEAGE
until = SUPERSEDED
```

And historical facts can remain durable:

```text
Conversation event 105 existed
scope = HISTORICAL_SOURCE
currentness = IMMUTABLE_EVENT
```

This suggests future relational storage should avoid naked edges such as:

```text
A -> B
```

and preserve relationships more like:

```text
A --relationship--> B

owner
scope
source
validity/currentness
evidence standing
```

---

## 6. Dynamic observations are not automatically configuration fields

Keep this distinction explicit:

```text
DYNAMIC CONFIGURATION
"What should the system do?"

examples:
  active context budget
  temperature
  prompt-cache entries


DYNAMIC OBSERVATION
"What is true right now?"

examples:
  available RAM
  current time
  current browser page
  model-process health
  Source Return state
```

The second class does not need to become another user-editable execution field.

A host observation may carry:

```text
resource.memory.available
value = 18.4 GB
source = host-resource-monitor
observedAt = <timestamp>
scope = this-device / this-turn
```

The host may consume that fact without placing every raw measurement in the model prompt.

---

## 7. Snapshot volatile truth at Send

Do not continuously mutate the inference context because real time or resource state changes.

Prefer:

```text
Send accepted
    ↓
snapshot relevant dynamic state
    ↓
freeze that state for this provider stage / human turn as appropriate
    ↓
model processes one coherent observation
```

If later reasoning genuinely needs a fresher value:

```text
model requests fresh observation
    ↓
host reads current source
    ↓
new result
    ↓
next provider stage
```

This preserves both truthfulness and cacheability.

---

## 8. What KV / prompt cache actually permits

A cache is computed state for a particular token prefix.

If a cached prompt begins:

```text
A B C D E
```

and the next prompt is:

```text
A B C D E F G
```

then the exact `A B C D E` prefix can be reused and only the suffix needs new processing.

If the new prompt is:

```text
A B C X ...
```

reuse ends at the first divergence.

### A cached suffix is not a new prefix

This is generally invalid:

```text
OLD
[A B C D][E F G]

then cut A B C D

NEW
[E F G]
```

The cached states for `E F G` were computed while attending to `A B C D`.

```text
E old = E given A B C D
E new = E given new beginning
```

They are different contextual states.

Therefore:

> Reuse an unchanged prefix. Do not treat a formerly-late suffix as if it had been computed from position zero.

### Shared-prefix successor reuse

If both contexts begin:

```text
OLD
Foundation
Profile
Conversation A B C D

SUCCESSOR
Foundation
Profile
Working Continuity
recent C D
```

then the valid reusable region is:

```text
Foundation
Profile
```

The successor must be recomputed from the divergence point onward.

Even identical text such as recent `C D` requires fresh KV state when its preceding context changed.

---

## 9. One-slot successor prewarm / cache baton

The investigation identified a potentially useful future optimization that does **not** inherently require two
large resident prompt caches.

Conceptual lifecycle:

```text
CURRENT TURN

one cache slot
  current prompt / continuation state
        ↓
capability/source-return loops complete
        ↓
final response durably committed
        ↓
Working Continuity says ROLLOVER
        ↓
current cache has completed its job
        ↓
construct deterministic successor prefix
        ↓
prefill-only during human idle time
        ↓
replace/seed the same one cache slot
        ↓
NEXT SEND
reuse successor prefix
+ process fresh dynamic suffix
+ process new human message
```

This moves work:

```text
from:
  next-Send critical latency

to:
  post-turn idle time
```

It does not eliminate computation.

### Two-slot alternator is a separate optimization

Two resident entries become useful only if Product evidence requires preserving the current cache while simultaneously
building a candidate successor:

```text
slot A = current
slot B = successor candidate
```

That can consume substantially more KV memory for long contexts.

Do not infer:

```text
successor prewarm
=> prompt-cache entries must become 2
```

First test whether a one-slot post-turn baton handoff is sufficient.

---

## 10. Startup / preset Foundation prewarm

A large stable Foundation is not necessarily a large **recurring** prefill cost.

Cold:

```text
stable Foundation  8k
current context     4k
user                .2k
→ roughly 12.2k newly processed
```

Warm exact prefix:

```text
stable Foundation  8k  CACHED
current context     4k  NEW
user                .2k NEW
→ roughly 4.2k newly processed
```

A future Product may be able to move part of the first-turn cost into launch/idle time:

```text
VexLife launches
    ↓
model becomes available
    ↓
prefill selected stable Foundation/profile prefix
    ↓
human reads/types
    ↓
first Send reuses warm prefix where exact
```

Cache hit is an optimization only:

```text
CACHE HIT
-> faster

CACHE MISS
-> still correct
```

Continuity must never depend on a cache remaining resident.

---

## 11. MLX-LM 0.32.0 runtime facts behind the prewarm hypothesis

These are **runtime-source facts**, not a claim that VexLife currently exercises each behavior.

Observed in `ml-explore/mlx-lm` tag `v0.32.0`:

### `mlx_lm/cache_prompt.py`

The utility describes itself as:

```text
Cache the state of a prompt to be reused with mlx_lm.generate
```

It:

```text
loads model/tokenizer
tokenizes the prompt
creates prompt cache
runs generate_step(... max_tokens=0 ...)
saves the prompt cache
```

Therefore prefill-only cache formation exists independently of answer generation.

### `mlx_lm/models/cache.py`

`LRUPromptCache`:

- stores distinct prompt-cache sequences;
- accounts each cache entry's KV byte size;
- uses a prompt trie to find exact/shorter/longer related prefixes;
- can reuse a shorter cached prefix;
- can trim a trimmable longer cache back toward a common prefix;
- evicts entries when `max_size` or `max_bytes` is exceeded.

### `mlx_lm/server.py`

At `v0.32.0`:

- `--prompt-cache-size` is the maximum number of distinct KV caches held;
- the server calls `fetch_nearest_cache(...)` before processing a prompt;
- only the unmatched remainder is processed after a reusable prefix;
- completed generation state is inserted back into the prompt cache;
- `--prompt-cache-bytes` can impose a byte ceiling;
- `--prefill-step-size` controls prompt-processing chunk size.

Source:

- https://github.com/ml-explore/mlx-lm/tree/v0.32.0
- `mlx_lm/cache_prompt.py`
- `mlx_lm/models/cache.py`
- `mlx_lm/server.py`

Any Product adoption still needs exact model/tokenizer/chat-template/runtime compatibility and lived VexLife measurements.

---

## 12. Persisted cache artifacts are possible but must be identity-bound

Because MLX 0.32.0 can save a precomputed prompt cache, a future Foundation cache artifact is technically plausible.

A safe Product contract would need to bind it to at least:

```text
exact selected model
exact tokenizer
exact provider/chat-template representation
exact stable-prefix bytes/tokens
Foundation/profile fingerprint
runtime/cache compatibility
```

A cache artifact is derived acceleration state, not canonical context truth.

```text
CACHE ARTIFACT
!= FOUNDATION SOURCE
!= MEMORY
!= CONVERSATION
!= CONTINUITY OWNER
```

---

## 13. Working Continuity rollover and semantic Memory consolidation are different lifecycles

Keep three processes distinct:

```text
1. PROMPT ROLLOVER

   reason:
     finite active prompt residency / pressure

   desired effect:
     continue from compact current working coordinates

   does not inherently mean:
     rewrite semantic Memory
     delete old Conversation
     start a new human-visible thread


2. MEMORY / LATTICE CONSOLIDATION

   reason:
     accepted durable meaning has new relationships,
     supersession, contradiction, or reusable structure

   desired effect:
     evolve durable relational Memory while preserving source


3. HUMAN CONVERSATION LIFECYCLE

   reason:
     person explicitly wants a new/archive conversation boundary

   desired effect:
     human-facing lifecycle change
```

Do not make #1 secretly perform #2 or #3.

Current Working Continuity source already points in this direction: checkpoints are reference-oriented and are not themselves
semantic Memory promotion.

---

## 14. The durable Memory lattice should be larger than the prompt

A future Memory lattice should not require the model to reread its entire accumulated world.

Conceptual separation:

```text
DURABLE MEMORY / SOURCE LATTICE
potentially very large

nodes / relationships may include:
  observations
  source facts
  accepted claims
  intentions
  decisions
  contradictions
  unresolved questions
  working positions
  supersession/currentness
        ↓
current intention activates a relevant neighborhood
        ↓
small current Memory projection
        ↓
prompt
```

This separates:

```text
RESIDENCY DEPTH
how much is directly in the prompt

from

RELATIONAL DEPTH
how far Product/retrieval can navigate through relevant durable relationships
```

A smaller prompt with strong source-addressable relational retrieval can preserve more useful continuity than a giant prompt that
merely carries everything and hopes attention finds the right passage.

---

## 15. Live reasoning tree versus durable lattice

A turn may temporarily explore:

```text
question
├─ hypothesis A
├─ hypothesis B
│    └─ evidence rejects B
└─ hypothesis C
     └─ evidence supports C
```

Do not automatically persist every exploratory token/path.

Prefer committing the useful relational result:

```text
claim C
  supported-by -> evidence E
  scoped-to    -> scope S

hypothesis B
  rejected-for-this-scope-by -> evidence E
  [retain only when useful]

unresolved Q
  depends-on -> missing evidence M
```

The durable world is a lattice/graph of typed relationships.

A reasoning tree is one temporary traversal through that world.

---

## 16. Foundational questions should be policy/grammar, not a forced questionnaire

The investigation started with questions such as:

```text
What is actually being claimed?
What scope does the claim have?
What evidence would justify it?
Is the evidence current?
Is there a contradiction?
```

Do not automatically inject 25 literal questions into every turn.

Prefer:

```text
Foundation
  stable reasoning orientation / relationship grammar

Host mechanics
  facts that can be mechanically verified

Current unresolved relationships
  activate only the questions/retrievals needed now
```

Examples:

```text
source-dependent claim + source absent
-> retrieval/evidence cycle

two current owner values disagree
-> contradiction cycle

high-consequence effect claim
-> stronger verification

semantic ambiguity that changes the conclusion
-> interpretation question

system fact already owned by Product
-> inspect owner; do not ask the model to guess
```

Questions are not necessarily the primitive.

A more useful primitive is:

> an unresolved relationship required by the conclusion or action currently at stake.

---

## 17. Evidence is scoped to the strength of the claim

These are different claims:

```text
source exists
source was available
source was selected
source was placed in the provider request
source was processed by the model
source affected the answer
source caused this exact conclusion
```

Evidence for an earlier statement does not automatically establish a later, stronger statement.

Therefore future claim/evidence design should bind:

```text
claim
scope
required predicates
rightful evidence owner
unresolved predicates
```

and descend only where the current claim/consequence requires it.

---

## 18. Conditional routing language instead of a forced thought script

A Foundation can preserve useful routes for judgment without becoming a questionnaire or a list of commands.

Prefer relationships such as:

```text
GIVEN
  current context already answers the material question

THEN
  answer from what is established


WHEN
  a claim depends on source that is not currently present

CONSIDER
  the source/retrieval route that owns that missing relationship


WHEN
  two owner-current values contradict one another

THEN
  hold the dependent conclusion
  compare the rightful owners / currentness
  resolve only the relationship that is actually in conflict


WHEN
  uncertainty does not change the decision or human outcome

THEN
  do not create inquiry merely to satisfy a reasoning ritual


WHEN
  uncertainty materially changes the conclusion, action, or consequence

THEN
  surface it or activate the smallest route that can resolve it
```

This is closer to a **routing grammar** than to instructions such as:

```text
1. ask question A
2. ask question B
3. verify C
4. always retrieve D
```

The intended behavior is:

```text
present relationship
        ↓
condition becomes relevant
        ↓
matching route becomes available to judgment
        ↓
only that route is exercised when needed
```

The language can therefore be suggestive and conditional:

> Given this condition, this is the relationship worth considering.

rather than coercive:

> You must always perform these steps.

This preserves room for direct answers when the context is already sufficient.

### "Auto-executing questions" needs two different mechanisms

Do not make every activation depend on the model asking itself another prose question.

```text
MECHANICAL CONDITION
  host already knows the predicate

examples:
  source-return state
  current runtime identity
  prompt component presence
  authority/currentness receipt

-> host can enforce / expose the relationship directly


SEMANTIC CONDITION
  interpretation or missing meaning must be recognized

examples:
  user's scope is ambiguous in a material way
  two sources appear to disagree in meaning
  the conclusion depends on older context not currently resident

-> model judgment may activate a retrieval/question route
```

A Foundation can teach the distinction without owning either effect.

---

## 19. Give Vex a small context map before asking it to "hunt for tools"

A model should not need to rediscover the Product's context topology from raw prose on every turn.

A small stable map can tell Vex **which kinds of context exist and what question each kind answers**:

```text
CONTEXT MAP

Foundation
  -> how relationships, scope, judgment, and ownership are interpreted

Conversation
  -> what was actually said

Memory
  -> accepted current meaning and provenance

Working Continuity
  -> where unfinished work currently stands

Current State
  -> host-owned bindings that are current for this accepted turn

External / Returned Source
  -> evidence gathered from outside the currently resident context

Capabilities
  -> routes the current Product may offer for gathering missing truth

Current Request
  -> what the person is asking now
```

This map should describe **categories and relationships**, not duplicate every current value or every tool contract.

Then late current state can say:

```text
Capabilities currently available:
  repository
  conversation source
  web

Memory:
  current

Working Continuity:
  successor checkpoint present
```

The model no longer has to begin with:

> What tools might exist?

It can begin with:

> Which context layer owns the missing relationship, and is that route current now?

### Load the map, not every depth layer

The efficient pattern is:

```text
small stable context map
        ↓
current bindings / availability
        ↓
current question activates one neighborhood
        ↓
retrieve depth only when needed
```

not:

```text
put every tool description
+ every Foundation depth paragraph
+ every Memory node
+ every historical source
into every prompt
```

This is the prompt analogue of the durable Memory-lattice idea: make the topology recognizable while keeping most depth addressable outside active residency.

### Nested context blocks are useful for meaning

An illustrative semantic shape:

```text
<context>
  <foundation>
    ...
  </foundation>

  <history>
    exact Conversation ...
  </history>

  <current>
    <working-continuity>...</working-continuity>
    <memory>...</memory>
    <capability-state>...</capability-state>
  </current>

  <external-source>
    ...
  </external-source>

  <request>
    ...
  </request>
</context>
```

The exact provider representation may use roles/messages rather than literal XML. The point is the **nested semantic separation**.

Tags are not an authority/security boundary. Host role, source ownership, receipts, and currentness still own that distinction.

Also preserve causal ordering:

> A binding must appear before the first later block whose interpretation genuinely depends on it.

Nested presentation does not override that rule.

---

## 20. Separate machine schema identity from model-facing semantic labels

Current VexLife source intentionally carries literal model-facing headers such as:

```text
VEXLIFE_FOUNDATIONAL_FIELD/v1
VEXLIFE_CURRENT_STATE/v1
VEXLIFE_SELF_CAPABILITY_FRAME/v1
VEXLIFE_PERSON_CONTEXT/v1
```

Those strings provide visible separation and exact protocol vocabulary.

A new hypothesis from this investigation is that **schema-looking language may itself affect how a model receives the context**.

Possible receiving-side effect:

```text
/v1 + registry-like metadata + protocol names
        ↓
"read this as a specification / control packet"
        ↓
more parsing / compliance / self-monitoring posture
```

versus:

```text
plain semantic nested context
        ↓
"this is the situation I am standing in"
        ↓
more direct relational use
```

This is **not yet a proven defect**. It requires lived A/B evidence.

### Candidate separation

Keep exact machine identity outside the model where the machine needs it:

```text
HOST / RECEIPT

schema = vexlife-prod.current-state/v1
componentRef = ...
fingerprint = ...
sourceRefs = ...
```

while allowing a calmer model-facing form:

```text
<current-state>
This is what VexLife observed as current for this accepted turn.
...
</current-state>
```

or another provider-appropriate semantic representation.

The principle is:

```text
MACHINE ADDRESSABILITY
!= MUST RECITE MACHINE ADDRESS INTO MODEL CONTEXT
```

### Important exception: machine-control output vocabulary

Do **not** blindly remove exact headers that the host parser requires Vex to emit as machine control, for example an exact capability request/result or source-return request vocabulary.

Those have a different role:

```text
context label
  -> helps Vex interpret supplied context

machine-control header
  -> exact generated syntax the host recognizes
```

The latter may need exact versioned tokens.

### A/B test before changing Product source

Compare on the same model, context, and human prompts:

```text
A
  current /v1 model-facing block labels

B
  semantic nested labels
  exact schema/version retained only in host receipt/evidence
```

Measure at least:

```text
answer quality / directness
unnecessary meta-analysis
correct scope attribution
forged-user/browser-label resistance
capability selection correctness
source-return correctness
prompt tokens
cached prompt tokens
prefill tokens
first-token latency
```

The desired outcome is not "remove technical words." It is:

> Give the model the minimum semantic structure needed to understand the context while keeping exact machine identity where machine identity is actually consumed.

---

## 21. Current-main observation after reviewing the synthesis against source

At the time of this addition, current VexLife-Prod `main` already implements part of this direction:

```text
Product Foundational Field
+ optional profile Foundation
+ current Memory
+ capability/reach guide
        ↓
coalesced system prefix

exact recent Conversation
        ↓
VEXLIFE_CURRENT_STATE/v1
  Working Continuity
  current capability state
  Person Context
  Conversation extent
  source-return protocol

returned source / browser
        ↓
current request
```

Useful current properties:

- Product Foundation is first.
- Current state is already placed after Conversation rather than at the front.
- browser content is explicitly untrusted reference material.
- prompt receipts separately record component identity, placement, stability class, digest, and source refs.
- the current Foundation kernel is compact relative to its source depth.

Pressure exposed by this review:

1. Product-current capability facts still appear in Foundation and can force a Foundation revision when a feature becomes available.
2. the early capability guide can still vary according to current availability while a later current-state line also carries availability;
3. current Memory is in the early system prefix, so a Memory change can create an early token divergence before otherwise append-only Conversation;
4. the current `STABLE` component class describes several different meanings and should not automatically be read as "exact bytes likely reusable next turn";
5. model-facing profile Foundation includes storage/provenance identifiers that may belong only in receipt evidence;
6. the model-facing `/v1` labels may be useful structure or may induce protocol-reading posture; lived comparison is required.

These are **investigation findings**, not authority to reorganize `DirectPromptContext`.

---

## 22. Example context layout

Illustrative only:

```text
┌─────────────────────────────────────────────┐
│ PRODUCT FOUNDATION                          │
│                                             │
│ stable scope grammar                        │
│ stable ownership relationships              │
│ stable reasoning orientation                │
│ meaning of current-state symbols            │
└─────────────────────────────────────────────┘
             exact reusable prefix

┌─────────────────────────────────────────────┐
│ PROFILE FOUNDATION                          │
│                                             │
│ selected stable personal extension          │
└─────────────────────────────────────────────┘
             reusable while unchanged

┌─────────────────────────────────────────────┐
│ VEXLIFE_CURRENT_STATE                       │
│                                             │
│ thread / sequence                           │
│ capability availability                     │
│ source-return standing                      │
│ other turn-current bindings                 │
└─────────────────────────────────────────────┘

┌─────────────────────────────────────────────┐
│ WORKING CONTINUITY                          │
│ current trajectory / frontier / open loops  │
└─────────────────────────────────────────────┘

┌─────────────────────────────────────────────┐
│ CURRENT MEMORY PROJECTION                   │
│ selected relevant accepted relationships    │
└─────────────────────────────────────────────┘

┌─────────────────────────────────────────────┐
│ RECENT EXACT CONVERSATION                   │
│ bounded recent source dialogue              │
└─────────────────────────────────────────────┘

┌─────────────────────────────────────────────┐
│ EPHEMERAL CURRENT SOURCE                    │
│ browser / time / resource observations      │
└─────────────────────────────────────────────┘

┌─────────────────────────────────────────────┐
│ CURRENT USER REQUEST                        │
└─────────────────────────────────────────────┘
```

The exact Product order must be decided by existing semantic owners and provider representation. This example is not a replacement
prompt contract.

---

## 23. Example rollover + successor prewarm

Illustrative:

```text
TURN N

[stable Foundation]
[profile]
[large recent Conversation]
[current state]
[user N]
        ↓
TURN_COMPLETED
        ↓
pressure => ROLLOVER_REQUIRED
        ↓
reference-only Working Continuity successor formed
        ↓
old active cache no longer needed for turn N
        ↓
IDLE PREWARM

[stable Foundation]          reuse if exact
[profile]                    reuse if exact
[successor working context]  fresh prefill
[selected current Memory]    fresh prefill as applicable

        ↓
warm successor prefix

TURN N+1

[warm successor prefix]
[fresh dynamic state]
[user N+1]
        ↓
only unmatched suffix requires new prefill
```

This is the proposed **one-slot baton** hypothesis to test before increasing resident cache count.

---

## 24. Measurement before optimization

Do not infer latency improvement from source shape alone.

Relevant measurements include the relationship already investigated by continuity diagnostics:

```text
promptTokens
cachedPromptTokens
prefillTokens
firstTokenMs
firstAnswerMs
totalMs
cache bytes / host memory pressure
```

A context topology change earns a performance claim only after a controlled lived comparison.

Useful adjacent work:

- #195 — one warm mlx-lm prompt-cache runtime candidate
- #210 — baseline prompt-cache reuse/latency measurement
- #230 — request-current state placement after reusable Conversation history
- #370 — transparent execution fields + lower Context Profile budget
- #133 — foundational preservation investigation
- #220 — Memory Depth hydration

These remain distinct owners/claims. This note is a synthesis surface, not a replacement for any of them.

---

## 25. Proposed best-practice checklist

Before introducing a new prompt/context component:

1. **Name the rightful owner.** Is this Foundation, Conversation, Memory, Working Continuity, current host state,
   external source, or user content?
2. **Classify stability.** Stable, append-only, mutable-current, or ephemeral-current?
3. **Define scope/currentness.** This turn, lineage, thread, historical source, until superseded, etc.
4. **Separate definition from binding.** Stable semantic grammar can live earlier; mutable value can be supplied later.
5. **Place the binding before its first dependent content.**
6. **Preserve structural provenance.** Text markers alone do not grant host/system authority.
7. **Keep highly volatile observations out of the prompt until relevant.**
8. **Snapshot current truth instead of continuously changing the same inference.**
9. **Maximize exact stable prefix only after semantic ordering is correct.**
10. **Assume cache misses.** Cache improves speed, never correctness.
11. **Never reuse a cached suffix as a new prefix without recomputation.**
12. **Prefer one-slot post-turn prewarm before paying for an A/B cache alternator.**
13. **Keep prompt rollover separate from semantic Memory consolidation and human thread lifecycle.**
14. **Keep the durable Memory lattice larger than the active prompt projection.**
15. **Measure cached tokens/prefill/latency/memory before claiming improvement.**
16. **Preserve exact source paths for older context instead of making summaries the only remaining truth.**
17. **Prefer conditional routing relationships over fixed question scripts.** Teach when a route becomes relevant instead of requiring the route on every turn.
18. **Give Vex a small stable context map before depth/tool retrieval.** Let current state bind which routes are actually available.
19. **Separate machine schema identity from model-facing semantic structure where possible.** Keep exact parser-required control vocabulary exact.

---

## 26. What this note intentionally does not decide

Still open:

- exact accepted Product ordering of every `DirectPromptContext` component;
- whether VexLife should expose a first-class prefill-only runtime Control;
- whether a one-slot baton can be implemented through the existing server process without a Product-specific cache injection seam;
- whether saved prompt-cache artifacts are worth startup/runtime complexity;
- cache artifact compatibility schema;
- adaptive resource-based prewarm policy;
- whether/how the future Memory lattice is implemented;
- exact semantic acceptance rules for lattice mutations;
- whether Foundation/profile presets should expose warm/cold cache state in Vex Context/Vex Labs;
- lived latency and memory benefit on the qualified Mac/Windows model relationships;
- whether #230's exact placement candidate earns acceptance;
- any merge relationship among the open predecessor/sibling PRs.

Those should remain engineering questions until source and lived evidence answer them.

---

## 27. Toolkit integration audit — pre-implementation findings

> Historical checkpoint: this section records the concrete gaps that were found before the
> implementation in this PR. The branch now resolves the native-binding/schema/web/Memory/GitHub/Home/import-model gaps named
> below; the observations remain here as causal provenance for why the source changed.

The review of the Capability Atlas, provider representations, bridge, web/repository/Home readers, Memory recall, and continuity revisit showed that the toolkit was **connected but not yet uniformly optimal**.

### Native-tool integration standing

At the pre-implementation checkpoint, model/provider profiles did not provide one ordinary Product path that was both:

```text
provider-native tools
AND
fully qualified ordinary reach
```

Pre-implementation shape:

```text
Qwen 27B / 9B / 4B
  capabilityRepresentation = VEXLIFE_CONTROL_TEXT
  reach standing = QUALIFIED

GPT-OSS 20B
  capabilityRepresentation = HARMONY_FUNCTIONS
  reach standing = HELD_PROVIDER_BRIDGE
```

Therefore:

- Qwen has qualified reach but still learns/expresses it through prompt/control-text grammar.
- GPT-OSS has a native tools compiler in source, but current ordinary Product admission remains held.

The MLX-LM 0.32.0 server itself accepts a `tools` field and passes it through the tokenizer chat template. Whether each exact Qwen tokenizer/model relationship can use that path correctly remains a qualification question.

### Atlas -> native tool compiler exists, but its schema vocabulary is too weak

`HarmonyCapabilityTools.tools()` already compiles Atlas definitions into provider-native functions.

However the current native schema type projection is:

```text
string  -> string
integer -> integer
number  -> number
boolean -> boolean
anything else -> string
```

That means current Atlas argument types such as:

```text
memory.search terms: string[]
continuity.revisit queries: object[]
continuity.revisit scopes: string[]
```

are projected to native tool schemas as plain `string`.

This is a concrete representation gap.

The current Harmony unit test covers scalar web/GitHub arguments but does not prove array/object schemas.

A future provider-independent capability schema should use typed structure rather than a free-form `Argument.type: String`, for example conceptually:

```text
String
Integer
Number
Boolean
Enum(values)
Array(items)
Object(properties, required)
```

The same semantic schema can then compile into:

- Harmony/OpenAI-compatible provider tools;
- text-control help for models that still require it;
- human Vex Context/Atlas descriptions;
- request validation.

One semantic tool contract should feed every representation.

### Native tools should remove duplicate text-control ceremony

Where provider-native tools are qualified, avoid simultaneously teaching the model the full legacy control syntax in the system prompt.

Desired split:

```text
Foundation
  when / why reaching is relevant

Capability Atlas
  canonical semantic actions + argument schema

Provider native tools
  actionable names/descriptions/parameters

Current frame
  current admission / availability

Bridge
  validates + performs through rightful owner
```

Exact `VEXLIFE_CAPABILITY_REQUEST/v1` text remains useful as a canonical bridge representation and fallback for text-control models; it does not have to be model-facing instruction for a provider-native model.

### Tool schemas still consume model context

Provider-native `tools` are not free tokens.

MLX-LM 0.32.0 passes the `tools` definitions to `tokenizer.apply_chat_template(... tools=tools ...)`, so they become part of the tokenized inference input.

This creates a cache/availability design question:

```text
offer only currently AVAILABLE tools
  -> strongest model-facing availability truth
  -> tool schema may change turn-to-turn and invalidate prefix reuse

offer a stable qualified tool schema
  -> better cache stability
  -> bridge/current frame must reject or explain temporarily unavailable calls

hybrid stable core + dynamic exceptional tools
  -> possible middle ground
```

Security/effect authority stays host-enforced in all three; the tradeoff is model behavior and cache efficiency.

### Web reach currently has two independent truncation layers

Current web owner:

```text
network response maximum = 2 MiB
```

Current capability bridge:

```text
one result maximum = 9,000 estimated tokens
four requests / human turn
24,000 tokens reserved for capability exchanges
```

`web.fetch` supports a character `offset`, and bounded results can return:

```text
sourceCharacters
returnedStart
returnedEnd
nextOffset
more=true
```

That is useful but incomplete.

Important current behavior:

```text
web.fetch(url, offset=N)
  -> downloads the URL again from the beginning
  -> extracts readable text again
  -> returns substring(N)
```

Consequences:

1. repeated continuation requests redo network/extraction work;
2. the page can change between calls, so later slices need not belong to the same observed source;
3. if the original network body exceeded 2 MiB, the reader never acquired bytes beyond that bound, so character offsets cannot continue into content that was never downloaded.

The result does report `networkTruncated=true`, but there is no byte-range continuation owner today.

### Repository and Home large-file continuation is weaker than web

Repository read uses the same 2 MiB web bound; Home read has a 1 MiB bound.

The bridge can then cut either result again to its 9k-token result limit.

Unlike `web.fetch`, current repository/Home reads do not carry a source offset / total-character continuation coordinate.

Therefore a large file may return:

```text
truncated=true
```

without a deterministic way for Vex to ask for the next exact slice.

Repeated `github.read` or `home.read` simply starts over.

That should be corrected before calling the toolkit robust for large sources.

### Memory recall is useful but narrow

Current `memory.search`:

- searches current Memory pages;
- lexical substring match over page summaries;
- accepts 1-6 terms;
- returns at most 24 matches.

It does not currently provide a cursor/frontier when more than 24 matching pages exist.

It also should not be described as a semantic meaning search: there is no embedding/semantic locator in this owner.

A better return contract would include at least:

```text
searchedCount
totalMatches / known-more
returnedCount
nextCursor or frontierRefs
projection/currentness
source refs
```

and keep lexical versus semantic standing explicit.

### Continuity revisit already demonstrates the stronger pattern

`continuity.revisit` is currently the best model for a future layered scanner:

```text
one model-visible request
  -> host reads the relevant owners at most once for this turn
  -> several queries fan over the same source set
  -> bounded relevant nodes/excerpts returned
  -> exact source refs retained
  -> frontier refs name what did not fit
  -> seeded/from continuation can reopen later exact material
```

This is more efficient than requiring the model to ingest one giant source or issue one raw fetch per chunk.

The reusable pattern is:

```text
SOURCE OBSERVATION
        ↓
turn-scoped/pinned source identity
        ↓
HOST SCAN / STRUCTURE
        ↓
small directory / outline / match neighborhood
        ↓
MODEL chooses relevant branch
        ↓
EXACT bounded slice
        ↓
frontier / cursor if more remains
```

### Proposed web/source improvement: inspect -> query -> read

Do not make one `fetch` action serve discovery, full transport, semantic navigation, and chunk continuation simultaneously.

A candidate relationship:

```text
web.inspect(url)
  -> fetch once under current admission
  -> form one turn-scoped source snapshot identity
  -> title / content type / source length / headings or structural sections
  -> network-truncation standing
  -> no giant content return

web.find(sourceRef, queries[])
  -> host scans the already-observed source
  -> relevant section/snippet coordinates
  -> several query wordings in one request

web.read(sourceRef, sectionRef | offset, max...)
  -> exact bounded source slice
  -> next coordinate / frontier
```

This does **not** require a new durable source owner. The source snapshot can be turn-scoped/ephemeral, with URL, observedAt and content hash binding all slices to the same observation.

Repository/Home can use the same shape:

```text
inspect / tree / find
        ↓
read exact range
        ↓
continue by line/byte/character coordinate
```

Prefer line or structural coordinates for text/code where practical; raw character offsets remain a fallback.

### Result owners should shape results before the bridge's emergency cut

Today `VexLifeCapabilityBridge.fit()` has a generic final safety cut that shortens `body.text` by character length until it fits.

That is a useful final guard but a poor primary pagination mechanism because it can cut arbitrary semantic/code boundaries.

Prefer:

```text
rightful source owner
  -> bounded whole sections/nodes/lines
  -> explicit continuation/frontier

Bridge
  -> final hard safety cap only
```

Continuity revisit already does this better than raw web/repository/Home reads.

### Tool-quality qualification should test model behavior, not only source conversion

A provider adapter being able to serialize tools is not enough.

A lived/tool-literacy matrix should pressure at least:

```text
"what exact words did I use?"
  -> Conversation / continuity, not Memory summary

"what do you remember about X?"
  -> current Memory first, memory search when needed

"what does current source code say?"
  -> repository read

"what is true on this public page now?"
  -> web

question already answered by resident context
  -> no unnecessary tool call

tool result truncated / frontier present
  -> continue only when needed

tool unavailable
  -> preserve boundary, do not invent result

malformed/ambiguous tool need
  -> correct or ask, do not silently switch owner
```

Measure:

- correct tool/owner selection;
- unnecessary-call rate;
- malformed-call rate;
- continuation correctness;
- source/result use in final answer;
- requests per successful human outcome;
- prompt/tool-schema tokens;
- cached prompt tokens / prefill;
- first-answer latency;
- host memory/network work.

The desired outcome is not "more tool calls." It is **the smallest truthful source path that completes the human question**.

---

## 28. Resolved native-exposure matrix — remove legacy assumptions

The earlier note left open whether the exact Qwen relationships could use provider-native tools. That question has now been resolved against the exact pinned model templates and mlx-lm 0.32.0 source.

### Qwen 27B

The exact Product-pinned model:

```text
donedynamics/Qwen3.8-27B-heretic-MLX-4bit
provider revision 808b82a2bd04b2bac362b5d0b7a1197aeea59e57
chat_template.jinja sha256 c3cf9e34...
```

has a chat template that:

- accepts `tools`;
- renders them into the system/tool section;
- instructs the model to emit `<tool_call><function=...>...`;
- accepts prior assistant `tool_calls`;
- accepts role `tool` results and renders them as `<tool_response>`;
- supports `enable_thinking` and `reasoning_effort`.

Therefore Qwen 27B does **not** require VexLife's `VEXLIFE_CONTROL_TEXT` grammar merely to express tool use.

### Qwen 9B / 4B — template support is not lived qualification

The pinned 9B and 4B Product entries carry Qwen-style native tool templates, and the 9B template was
inspected directly as exposing the expected `tools`, `tool_calls`, and `tool` result flow.

That establishes a candidate representation, not lived qualification for every model. The integrated
4B / 15-turn A003 pressure run later observed the exact 4B entering provider tool-call mode on two
successor probes while mlx-lm returned `finish_reason=tool_calls` without a consumable structured
tool call. No canonical capability request reached the Bridge on those stages.

The exact malformed raw model tool text was not retained, so this source does not invent why mlx-lm's
parser rejected it. Until the 4B native-call path earns its own lived qualification, the 4B uses the
existing `VEXLIFE_CONTROL_TEXT` compatibility representation. Qwen 27B/9B native-tool standing is
not changed by that 4B finding.

### mlx-lm 0.32.0 already parses the Qwen native format

`mlx_lm/tokenizer_utils.py` in v0.32.0:

- inspects the chat template;
- recognizes the Qwen pattern `<tool_call>\n<function=`;
- selects the `qwen3_coder` parser.

`mlx_lm/tool_parsers/qwen3_coder.py`:

- recognizes `<tool_call>...</tool_call>`;
- extracts function name and parameters;
- converts parameter values according to the supplied tool schema.

`mlx_lm/server.py` then formats parsed calls into OpenAI-compatible:

```text
choices[].delta.tool_calls
or
choices[].message.tool_calls
```

with function name + JSON arguments.

Therefore VexLife should **consume MLX's structured Qwen `tool_calls` response** rather than writing another Qwen XML parser.

### Historical VexLife gap for Qwen

> Resolved on this branch. The text below records the pre-change gap that motivated the implementation.

Before this branch, `DirectCompanionConnection` did not inspect `delta.tool_calls` or `message.tool_calls` on the ordinary Qwen path. It only:

- reads answer/reasoning fields for OpenAI-chat Qwen;
- separately handles native Harmony calls through `HarmonyResponseSplitter`.

The Qwen hookup that this branch now implements was narrow and concrete:

```text
Capability Atlas
  -> typed provider tools in request.tools
  -> Qwen template
  -> MLX parses model's native call
  -> response delta/message.tool_calls
  -> DirectCompanionConnection collects structured call
  -> common VexLife CapabilityRequest semantic object
  -> existing CapabilityBridge
  -> rightful owner
  -> native role=tool result on next provider stage
```

No Qwen-specific output parser should be added below MLX.

### GPT-OSS / Harmony

The exact GPT-OSS template also accepts native `tools` and renders them into the Harmony `functions` namespace.

mlx-lm 0.32.0's generic tool-parser inference, however, has no Harmony/`to=functions.*` parser case and the pinned GPT-OSS tokenizer config does not declare a `tool_parser_type`.

Therefore VexLife's Harmony representation adapter still has a legitimate job:

```text
raw Harmony channel / recipient form
  -> provider-specific extraction
  -> common semantic ToolCall
```

The adapter should stop there. Admission, effect ownership, result meaning and pacing remain common VexLife semantics.

### Target provider-independent capability binding

Replace the current model-specific high-level split:

```text
VEXLIFE_CONTROL_TEXT
HARMONY_FUNCTIONS
```

with a semantic relationship conceptually like:

```text
CapabilityBinding
  declaration:
    PROVIDER_NATIVE_TOOLS | TEXT_CONTROL_FALLBACK

  callIngress:
    OPENAI_TOOL_CALLS | HARMONY_RECIPIENT | TEXT_CONTROL

  resultEgress:
    PROVIDER_TOOL_ROLE | HARMONY_TOOL_RESULT | TEXT_CONTROL_RESULT

  offerPolicy:
    STABLE_REGISTERED | AVAILABLE_ONLY | HYBRID

  schema:
    typed Capability Atlas contract
```

Exact class/enum names are not prescribed here; the point is that **semantic capability meaning stays provider-independent while only representation varies by model/provider**.

The execution-topology read side should expose these relationships as inspectable fields once source owns them. They need not be human-editable settings.

### Cache consequence: native tool definitions become part of the earliest prompt prefix

For the exact Qwen template, when `tools` are present, the template renders the tool definitions at the beginning of the system content and then appends the supplied system/Foundation content.

Therefore:

```text
dynamic offered-tool set
-> divergence near token 1
-> weak prompt-cache reuse
```

This makes the offer policy a real performance relationship.

A strong first candidate is:

```text
stable registered/read-only tool definitions
  -> stable model-facing tool prefix

late current capability state
  -> AVAILABLE / HELD / UNKNOWN for this turn

CapabilityBridge
  -> final admission
```

Tool presence would mean "this is a known callable relationship", not "this effect is guaranteed available now".

This should be A/B measured against AVAILABLE-only dynamic offers for:

- unnecessary tool calls;
- NOT_AVAILABLE call rate;
- cached prompt tokens;
- prefill tokens;
- first-token latency;
- successful human outcome.

### Native result carriage should replace user-role protocol text

For provider-native models, the model should receive returned tool data through the provider's native `tool` result representation.

Canonical VexLife result semantics can still be:

```text
state
target
source/snapshot refs
content trust
returned content/nodes
frontier/cursor
host guidance
```

but the provider adapter should serialize that semantic result into native tool-result fields instead of making the model reinterpret a `VEXLIFE_CAPABILITY_RESULT/v1` block in a user message.

Text-control request/result remains a fallback representation when the selected model/provider has no native tool path or when that exact native-call path has not earned lived qualification.

### Completion direction

The old toolkit is not discarded.

Keep:

- Capability Atlas as semantic registry;
- current Capability Frame / admission truth;
- VexLifeCapabilityBridge;
- rightful source owners;
- result/source refs and traces;
- context/reach distinctions.

Replace or evolve:

- free-form argument type strings -> typed recursive schema;
- Qwen text-control carriage -> native `tools/tool_calls/tool` path;
- duplicate native-model prompt instructions -> provider-native schema;
- dynamic early tool definitions -> test stable tool prefix + late availability;
- generic character truncation as primary pagination -> owner-shaped scans/cursors/frontiers.

<!-- [VXG RealForever][MEMORY-DEPTH][NATIVE-TOOLS][RESOLVED-EXPOSURE-MATRIX] -->

---

## 29. Implementation checkpoint on PR #383

The investigation is now carried into Product source on this branch.

### Provider-native capability binding

Implemented relationship:

```text
Capability Atlas
  typed provider-independent action schema
        ↓
ProviderCapabilityTools
  one native function schema for every read action
        ↓
selected provider representation
  Qwen/OpenAI-chat -> request.tools + mlx-lm tool_calls + role=tool result
  GPT-OSS/Harmony -> functions namespace + Harmony recipient + tool result
  imported Qwen -> exact pinned template inspection chooses native tools or text fallback
        ↓
canonical Vex request meaning
        ↓
Capability Bridge
        ↓
rightful source owner
```

Shipped Qwen 27B/9B and GPT-OSS profiles use `PROVIDER_NATIVE_TOOLS`. The exact Qwen 4B uses
`VEXLIFE_CONTROL_TEXT` after the integrated A003 lived run showed that its native tool-call path
could enter `tool_calls` without producing a consumable structured call. This is a representation
fallback only; Capability Atlas meaning, Bridge admission and source ownership are unchanged.

The exact Qwen path does **not** add a second XML parser: mlx-lm 0.32.0 parses the pinned Qwen tool grammar and VexLife consumes the structured `tool_calls` it returns.

Imported Qwen no longer inherits a family-wide assumption. During no-effect import inspection, VexLife reads the exact pinned small chat-template/tokenizer material:

```text
native Qwen tool grammar observed
-> PROVIDER_NATIVE_TOOLS

native grammar not observed
-> VEXLIFE_CONTROL_TEXT fallback
```

Both paths still converge on the same Atlas / Bridge / rightful owner.

### Stable tool prefix + late availability

For provider-native profiles the declared read-tool set is stable across provider stages.

```text
known tool schema
  stable early provider prefix

current Capability Frame
  AVAILABLE / HELD / UNKNOWN late current truth

Capability Bridge
  final admission
```

A tool being declared means the model knows the route. It does not claim that the route is available or that an effect happened.

Native profiles no longer receive the legacy capability-request instruction manual in their system guide. Text-fallback profiles receive the exact text-control grammar they actually need.

### Exact tool-offer evidence

Each provider invocation now binds:

```text
providerToolCount
providerToolSchemaSha256
requestBodySha256
```

into invocation evidence.

This proves which native callable schema was offered without persisting another copy of the tool descriptions.

### Layered source navigation

#### Web

`web.find(url, queries[])` and `web.fetch(url, offset)` share one turn-scoped observed page snapshot.

```text
first read
-> one admitted network observation
-> sourceRef + observedAt + readable text

find / later offset
-> same observed bytes
-> no repeated network fetch
```

Several query phrasings can be scanned against one observation before spending prompt room on a large exact slice.

#### GitHub

`github.read` accepts an optional character `offset` and reuses one exact observed file snapshot within the human turn. Later slices keep the same sourceRef rather than redownloading the beginning.

#### Home

`home.read` keeps the ordinary bounded first-byte read and now accepts `startLine` / `maxLines` for large text/log traversal.

Line-window results are shaped as complete contiguous lines before the Bridge's emergency cap, and `nextLine` names the first line not actually returned.

#### Memory

`memory.search` remains an honest lexical search over current Memory summaries, but it now exposes:

```text
totalMatches
returnedMatches
matchOffset
nextOffset
frontier statement refs
```

The widest Memory projection used by paged recall is observed once per human turn and reused across pages.

### Conversation Source Return

Native profiles select exact older Conversation through a provider-native `conversation_return` function.

The source-return owner still performs its existing deterministic canonical discovery and forms the exact bounded source projection/receipt.

The final returned source is still hydrated as the dedicated Conversation source-return context rather than being collapsed into the generic capability-result owner.

That is intentional in this candidate:

```text
native tool
  chooses the source-return action

Conversation Source Return
  owns discovery + exact returned source + prompt evidence
```

It avoids making generic capability result semantics a second owner of Conversation source truth.

### Remaining bounded truths

This branch does **not** claim:

- that a build/test suite has executed on this branch yet;
- lived model-quality/tool-selection proof on a real Mac;
- semantic/embedding Memory search;
- web bytes beyond the existing bounded network observation;
- GitHub file bytes beyond its bounded underlying network observation;
- cache/latency improvement before lived measurements.

For web/GitHub, the returned metadata names `networkTruncated=true` when the underlying observation itself was incomplete. Layered navigation solves model-context truncation and repeated reads of already-observed bytes; it does not pretend unobserved network bytes were read.

<!-- [VXG RealForever][MEMORY-DEPTH][NATIVE-TOOLS][IMPLEMENTED-CANDIDATE] -->

---

## 30. Source/provenance map for later return

### VexLife

- #220 — Memory Depth / turn-bound continuity hydration
- #195 — one-warm-cache runtime candidate
- #210 — provider-stage cache measurement baseline
- #230 — current-state placement/cache-prefix candidate
- #133 — foundational preservation investigation
- #370 — merged execution-field catalog + Context Profile budget
- `docs/foundation/VEX-CONTEXT-PROFILES.md`
- `src/jvmMain/kotlin/vexlife/companion/DirectPromptContext.kt`
- `src/jvmMain/kotlin/vexlife/companion/DirectWorkingContinuity.kt`
- `src/jvmMain/kotlin/vexlife/continuity/VexLifeWorkingContinuity.kt`
- `src/jvmMain/kotlin/vexlife/model/heretic/runtime/HereticRuntimeConfiguration.kt`

### MLX-LM

Pinned relationship discussed: `mlx-lm 0.32.0`

- `mlx_lm/cache_prompt.py`
- `mlx_lm/models/cache.py`
- `mlx_lm/server.py`

### Human intention preserved

The useful outcome is not "add more context machinery."

It is:

> Vex starts each turn with a coherent, scoped, truthful current view; stable context does not incur unnecessary repeat work; dynamic
> truth can change without invalidating more cache than necessary; older durable experience remains source-addressable; and context
> rollover can become a natural lifecycle rather than a human-visible ceremony.

<!-- [VXG RealForever][MEMORY-DEPTH][CONTEXT-SYNTHESIS][CACHE-SCOPE-ROLLOVER] -->
