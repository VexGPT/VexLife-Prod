# Direct continuity projection workbench

`[VXG RealForever]`

**State:** DRAFT WORKBENCH / NOT ACCEPTED PRODUCT SOURCE  
**Formation base:** cross-model temporary qualification carrier PR #191  
**Purpose:** make the next continuity/memory architecture executable and falsifiable before it is
allowed to reshape `DirectPromptContext`.

## Human/product question

Can Vex preserve one coherent relationship across long conversations, model switches, capability work,
failed turns and context rollover **without** making an ever-growing raw transcript, a semantic
checkpoint, or an MLX KV cache become a second source of truth?

The target experience is:

~~~text
same Vex
+ current truthful state
+ exact recent human continuity
+ recoverable older evidence
+ bounded model context
+ reusable prompt computation when valid
~~~

with this permanent dependency direction:

~~~text
SEMANTIC TRUTH MAY INVALIDATE COMPUTATION
COMPUTATION MAY NEVER INVALIDATE SEMANTIC TRUTH
~~~

## Why this workbench exists now

The current cross-model work has exposed several already-earned boundaries:

- #191: Qwen and GPT-OSS may differ in provider representation without becoming different Vex
  semantic systems.
- #187: current reach state must be projected explicitly rather than inferred from stale historical
  Conversation.
- #152: incomplete visible answer evidence must survive failure without becoming canonical
  Conversation, Memory or ordinary future model input.
- #206: prior capability evidence exists durably but ordinary later prompt materialization does not
  yet re-project the fact that a prior source was actually RETURNED.
- #207: prompt/cached/prefill token counts and provider-stage timing can measure whether a proposed
  context topology actually reduces repeated computation.

Those pressures are related, but they do not justify collapsing their owners.

## Permanent non-collapse

~~~text
CANONICAL SEMANTIC TRUTH
!= MODEL CONTEXT PROJECTION
!= PROVIDER SERIALIZATION
!= KV / PROMPT CACHE

CHECKPOINT
!= MEMORY
!= CONVERSATION
!= CAPABILITY TRACE

CACHE EVICTION
!= CONTINUITY LOSS

SEMANTIC REBASE
!= SOURCE-OF-TRUTH REWRITE

MODEL SWITCH
!= VEX IDENTITY SWITCH

TURN KEY
!= CANONICAL CONVERSATION

CALL STACK
!= RAW MODEL REASONING

HISTORICAL HYDRATION
!= MEMORY OWNERSHIP
~~~

The checkpoint and turn-spine structures in this workbench are derived projections. If they and every
KV cache disappeared, canonical owners must still be sufficient to reconstruct truthful context.



## Alignment with current VexArchitecture and Working Continuity owners

A source review after initial workbench formation tightened the ownership model.

VexArchitecture requires one question per rightful coordinate and warns against new IDs, registries and
history owners when existing semantic identity + chronology/current custody already answer the
question. VexLogger owns chronology; projections must not mint competing semantic identity.

VexLife-Prod already has:

~~~text
VexLifeWorkingContinuity
    pressure + reference-only checkpoint planning

VexLifeWorkingContinuityOwner
    durable checkpoint / head / successor claim / successor Context effects

WorkingSet
    lineageRef
    threadRef
    cursorEventRef
    activeWorkNodeRefs
    currentIntentReceiptRefs
    openLoopRefs
    currentStatementRefs
    currentContinuityRecordRefs
    currentDailyStratumRefOrNull
    sourceRefs
~~~

and its checkpoint truth explicitly requires:

~~~text
referenceOnly = true
rawTranscriptIncluded = false
modelSummaryIncluded = false
semanticAcceptanceCreated = false
memoryPromotionPerformed = false
~~~

Therefore this workbench does **not** introduce another durable semantic-checkpoint owner.

The refined relationship is:

~~~text
canonical Conversation / Memory / current-state / evidence owners
+ current Working Continuity checkpoint + WorkingSet
        |
        v
DirectPromptContext bounded model-facing continuity projection
        |
        v
provider-specific representation
        |
        v
ephemeral prompt/KV computation
~~~

Likewise, the "parenthesis closing" insight is first a projection question over the existing Working
Continuity relationship. If exact parent/active ordering cannot be truthfully recovered from the
current WorkingSet, that is pressure to refine the rightful Working Continuity contract—not permission
to create a second Call Stack Registry.

A completed-turn navigation aid is keyed by the already-existing `turnRef`; it is a **Turn Continuity
Projection**, not a new TurnKey identity. Semantic topic/takeaway fields remain proposed/derived until
their rightful source is established.


## Three-zone prompt topology

The candidate context projection is ordered by expected mutation frequency.

### Zone 1 — continuity projection epoch

~~~text
Foundational Field
+ stable continuity projection C_k
+ stable precedence/invariant language needed to interpret later state
~~~

`C_k` is a bounded projection of current meaning derived from rightful owners. It may contain
source-addressable statements such as active commitments, accepted decisions, rejected branches that
remain decision-relevant, and compact provenance that has already been absorbed.

It is **not** a new durable truth store.

Zone 1 stays byte-stable inside one continuity projection epoch. A semantic rebase intentionally creates a new
epoch and may invalidate prior prompt computation.

### Zone 2 — append-only continuity spine

After checkpoint `C_k`, completed canonical turns may append bounded envelopes:

~~~text
human request identity / exact recent request text when selected
-> host-owned grounded provenance receipt when relevant
-> canonical Vex response
-> sealed Turn Continuity Projection / structural transition
~~~

The old Zone 1 + Zone 2 bytes should remain an exact prefix when a new completed turn is appended.

A turn envelope must not manufacture system truth:

- capability evidence comes from the capability/turn evidence owner;
- completion state comes from the turn/Conversation owner;
- model/runtime identity comes from observed runtime/session truth;
- semantic topic/takeaway may be proposed by Vex but must not overwrite host-owned identities.

### Zone 3 — live execution overlay

Zone 3 contains current mutable execution truth needed for the active invocation:

~~~text
current #187 reach/capability state
current serving-model/runtime projection
active conversational focus/working-continuity focus projection
current human input
active intra-turn capability request/result material
other bounded transient execution context
~~~

Zone 3 may change between provider stages in one human turn. Its volatility must not require rewriting
the already-committed continuity spine.

Raw large capability payloads should normally remain here. When the human turn completes, durable
evidence remains with its rightful owner while later prompts carry only the bounded meaning/provenance
that is still needed.

## Provider representation boundary

This topology is semantic. It does **not** assume every provider accepts arbitrary later system
messages.

The provider profile owns how the three semantic zones become legal provider messages while preserving:

~~~text
HOST-OWNED CONTEXT != HUMAN SPEECH
HOST-OWNED CONTEXT != VEX RESPONSE
PROVIDER REPRESENTATION != SEMANTIC OWNER
~~~

The existing Qwen/GPT leading-system constraints must therefore be pressure-tested before any Zone 2
receipt format becomes production behavior.

## Turn Continuity Projections and conversational working-continuity focus

Long human conversations are not only chronological. A person may open nested questions, resolve a
tangent, and expect Vex to return to the exact paused parent objective.

This workbench therefore recognizes a possible host-owned structural projection:

~~~text
Depth 0  ACTIVE / PAUSED parent objective
  -> Depth 1 nested question
      -> Depth 2 nested investigation
      <- POP with bounded takeaway
  <- POP_TO parent
~~~

The host owns exact frame identity and legal transition. Vex may propose semantic fields such as topic,
takeaway, relation to parent, or "tangent resolved". A model proposal does not become structural truth
without sealing against the current host-owned stack.

A retrospective Turn Continuity Projection may carry bounded navigation meaning such as:

~~~text
turnRef
semantic state
parent/anchor refs
bounded takeaway
~~~

It must remain source-addressable and must not silently replace exact Conversation history.

## Incomplete turns

#152 preserves incomplete visible answer evidence outside canonical completed Conversation.

Therefore this workbench does **not** insert incomplete answer text into Zone 2 by default.

A later model-facing recovery projection may truthfully say that an earlier attempt stopped or that a
specific recovery artifact exists when the current action requires that fact. That bounded projection
must not turn the incomplete answer into canonical prior model output.

## Historical hydration

Compaction must not make old exact evidence unreachable.

The future capability should be owner-neutral in meaning:

~~~text
historical ref becomes decision-relevant
-> resolve ref through its rightful owner
-> return bounded exact material into Zone 3
-> continue in slices if necessary
~~~

Do not call every historical object "Memory". Conversation, capability trace, stopped-turn evidence and
Memory remain different owners.

## Semantic rebase

Ordinary turns should prefer append-only growth:

~~~text
C_k + turn k+1 + turn k+2 + ...
~~~

When the working projection crosses a semantic/performance watermark:

~~~text
canonical owners
-> build C_(k+1)
-> preserve source refs and unresolved obligations
-> fold resolved tangents / old transient detail
-> retain a bounded exact recent tail
-> begin a new cache epoch
~~~

A rebase is allowed to cause one expensive prefill. It exists to prevent every future turn from paying
the cost of an indefinitely growing raw replay.

The source of truth is not shrunk. Only its model-facing projection is restructured.

## Cache relationship

`--prompt-cache-size 1` is treated as runtime compute containment/reuse, not continuity custody.

The desired normal shape is:

~~~text
stable continuity projection epoch + append-only spine
-> longest valid provider-token prefix can be reused
-> only the uncached suffix requires prefill
~~~

A cache miss, cache trim, process restart or model-family switch may increase compute cost. None may
change semantic truth.

No production claim in this workbench depends on a promised "100% cache hit". Actual effectiveness must
be measured from provider evidence.

## Measurement / acceptance

#207-style stage measurement is the right proof surface for cache effectiveness:

~~~text
promptTokens
cachedPromptTokens
prefillTokens = promptTokens - cachedPromptTokens
firstTokenMs
firstAnswerMs
totalMs
~~~

A future implementation should demonstrate at least these shapes:

1. **append-only next turn**
   - stable semantic prefix remains byte/token compatible;
   - cached tokens rise substantially relative to cold prefill;
   - current-state truth still reflects the latest owners.

2. **multi-stage source reach**
   - large source material may live transiently in Zone 3;
   - same-turn continuation remains truthful;
   - next human turn does not require raw source payload to become canonical Conversation.

3. **semantic rebase**
   - continuity projection epoch changes intentionally;
   - canonical source refs and unresolved obligations remain recoverable;
   - one cache rebuild is accepted;
   - following turns regain useful prefix reuse.

4. **model switch**
   - semantic projection survives;
   - prior model KV state is disposable;
   - target provider receives equivalent meaning through its own representation.

5. **failed/incomplete turn**
   - visible evidence remains recoverable under #152 semantics;
   - incomplete answer text does not silently enter ordinary future prompt context.

6. **nested tangent**
   - host-owned stack can PUSH / POP / POP_TO without rewriting prior canonical Conversation;
   - returning to a parent restores the exact active objective and bounded takeaway.

## Current executable workbench proof

The companion test `DirectContinuityProjectionWorkbenchTest` deliberately tests only projection
invariants. It does not claim production `DirectPromptContext` is already changed.

It proves the proposed model can represent:

- volatile execution-tail changes without rewriting the stable prefix;
- completed-turn append as an exact extension of the prior stable prefix;
- semantic rebase as an intentional prefix/cache-epoch change while canonical lineage remains;
- failed/incomplete evidence staying out of the canonical append-only spine by default.

## What this PR should become

This branch is a **work/test environment**, not a request to preserve temporary ancestry forever.

Use it to pressure the design against real `DirectPromptContext`, provider profiles and #207
measurements. If the model survives source proof and lived end-to-end qualification, re-form/currentize
the accepted implementation on the rightful integration floor rather than treating temporary #191
ancestry as repository authority.

The merge target is the smallest implementation that preserves the verified relationships—not this
document's vocabulary for its own sake.


## Current-source pressure: mutable prefix state defeats Conversation cache reuse

The current #191 `DirectPromptContext` source is semantically careful but exposes a concrete compute
continuity problem that this workbench must solve rather than merely theorize about.

Current materialization does:

~~~text
prefixMessages
-> coalesceProviderSystemPrefix(prefixMessages)
-> selected canonical Conversation events
-> fixed suffix / current request
~~~

and current fixed prefix formation includes mutable projections such as:

~~~text
capability frame
person context
Conversation extent
source-return protocol
working continuity / Memory where present
~~~

`ConversationExtent.priorCompletedExchangeCount` necessarily changes as completed Conversation grows.
Therefore a turn can preserve the same historical Conversation bytes while changing an early system
token before those bytes.

Conceptually:

~~~text
turn N
  [foundation + extent=17 + current state]
  [same historical Conversation...]

turn N+1
  [foundation + extent=18 + current state]
  [same historical Conversation...]
~~~

For a causal transformer prefix cache, the later historical tokens were computed under different prior
tokens. Their old KV state cannot simply be treated as the same cached continuation.

An isolated prefix-pressure check for this workbench demonstrated the shape:

~~~text
PASS prefix pressure old=18 desired=66 stable=66
~~~

That number is **not** a production token/cache measurement. It is a deterministic string-prefix
demonstration of the causal topology:

~~~text
mutable field before history
-> reusable prefix ends before history

stable checkpoint + append-only history + mutable tail
-> reusable prefix can include history
~~~

This makes the next implementation question concrete:

> Which current host-owned facts genuinely need to be serialized before historical Conversation for
> provider semantics, and which can lawfully move into a late current-execution envelope without
> impersonating the human or Vex?

Provider constraints may require one leading system/developer block. If so, do not "fix" cache reuse by
lying about role ownership. Either accept an intentional cache rebase for genuinely prefix-owned state,
or earn a provider-specific representation that can carry late host context truthfully.


<!-- [VXG RealForever][VEXLIFE-PROD][DIRECT-CONTINUITY-PROJECTION][WORKBENCH] -->
