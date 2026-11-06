# Direct Personal Companion Circuit

`[VXG RealForever]`

## Human question

Can the fresh, isolated VexLife-Prod body complete a canonical personal human ↔ Vex turn through
its own qualified Heretic runtime while preserving durable Conversation truth and exact
"what Vex received / what runtime call happened" evidence?

This foundation is source-formed on top of the isolated local foundation. It does not depend on
legacy/current VexLife at runtime.

## Relationship provenance

The relationship is faithfully recovered from current VexLife source:

~~~text
src/core/lived-companion.mjs
src/core/lived-companion-connection.mjs
src/core/model-runtime-adapter.mjs
src/core/model-turn-witness.mjs
src/core/browser-prompt-context-runtime.mjs
src/core/browser-companion-bridge.mjs
~~~

Those sources are provenance/reference after port formation, not fresh-product runtime dependencies.

## Source-owned direct address

Current VexLife explicitly recognizes this direct personal address:

~~~text
project.self-development
thread.self-development.open-conversation
channel.self-development.companion
~~~

Fresh source owns the same relationship through `DirectConversationIdentity`. Presentation labels
do not own or recreate these refs.

## Current causal circuit

~~~text
VexLifeProdLocalSetup.Result
  exact qualified Heretic runtime evidence
        ↓
DirectConversationStore writer lease
        ↓
exact immediate prior completed turn
  addressed read of exactly two events
  whole-history prompt enumeration = false
        ↓
REQUEST event durable
        ↓
VexLifeCurrentMemoryReader
  canonical Home -> current Score / semantic authority
  -> committed daily stratum
  -> Living Journal pages
  -> bounded attributed Memory context
        ↓
DirectPromptContext
  first turn:
    current Memory when source-current
    + current request

  CONTINUE:
    current Memory when source-current
    + immediate prior REQUEST
    + immediate prior RESPONSE
    + current request

  192,000 input-token budget
  16,000 reserved output tokens
  208,000 hard limit
        ↓
Memory context re-read + exact equality check
before provider invocation
        ↓
prompt selection reverified while same writer lease is held
        ↓
DirectCompanionConnection
  endpoint/model supplied only by QualifiedRuntimeEvidence
  numeric-loopback /v1/chat/completions
  no redirects
  actual HTTP invocation
        ↓
normalized RuntimeObservation
+ InvocationEvidence
  raw provider body not persisted
  raw model path not persisted
  raw reasoning not persisted
        ↓
RESPONSE event durable
        ↓
DirectTurnContextStore
  prompt materialization receipt
  + request/response identity
  + normalized runtime observation
  + invocation evidence
        ↓
immutable completed-head receipt
        ↓
atomic current Conversation head
~~~

## Owners

### `DirectConversationIdentity`

Owns only the direct project/thread/channel address.

### `DirectConversationStore`

Owns:

- append-only REQUEST / RESPONSE event bytes;
- exact content + event hashes;
- exact contiguous completed-chain validation;
- immutable completed-head receipts;
- atomic current head;
- whole-turn writer lease coordinate.

It does not call the model or choose prompt context.

Current head advances only after:

~~~text
REQUEST durable
RESPONSE durable
turn context durable
~~~

### `DirectPromptContext`

Owns bounded provider materialization.

Current ordinary CONTINUE behavior is intentionally small and source-aligned:

~~~text
IMMEDIATE_PRIOR_COMPLETED_TURN
~~~

It does not enumerate whole history for prompt selection.

Memory is supplied by `VexLifeCurrentMemoryReader` from the canonical Home. A source-current
attributed packet becomes provider system context; a held/empty Memory context adds no provider
content. The reader is re-run under the same Conversation writer lease before provider invocation and
must return the exact same context used to form the prompt receipt.

Current VexLife's prompt-context runtime also has a separately-owned
`WORKING_CHECKPOINT_SUCCESSOR` mode when one exact working-rollover owner is eligible. That
relationship is **not** implemented by this first fresh direct circuit and remains held rather than
being collapsed into `IMMEDIATE_PRIOR_COMPLETED_TURN`.

~~~text
FIRST DIRECT CONTINUE CIRCUIT
!= FULL CURRENT PROMPT-CONTINUITY PARITY
~~~

### `DirectCompanionConnection`

Owns only:

~~~text
QualifiedRuntimeEvidence
+ provider-ready messages
-> actual numeric-loopback HTTP call
-> normalized runtime observation
-> invocation evidence
~~~

It cannot accept a caller-supplied endpoint or model identity.

Provider observation/evidence values are publicly readable but constructor/copy authority remains
inside the Connection.

### `DirectTurnContextStore`

Persists normalized turn evidence suitable for later Context Inspector projection:

~~~text
prompt materialization receipt
request / response refs
runtime observation
invocation evidence
~~~

It never persists:

~~~text
raw provider response
raw reported model path
raw reasoning
~~~

### `DirectCompanionTurn`

Composition owner only.

It derives deterministic turn/message identities from one `idempotencyKey`. Repeating the exact
already-completed send returns the durable canonical response without another model invocation.

## Writer serialization

The whole direct turn holds one cross-process writer lease.

The generic OS file-lock mechanism is shared as:

~~~text
VexExclusiveFileLease
~~~

while setup and Conversation separately own their lock coordinates.

Permanent:

~~~text
LOCK FILE PRESENCE != LOCK OWNERSHIP
REMEMBERED PID      != LOCK OWNERSHIP
OS FILE LOCK         = SAME-HOST EXCLUSION
~~~

## Failure and recovery boundary

The human REQUEST is durably written before inference.

If provider/materialization/context/head completion fails after that point:

- the current completed head does not advance;
- first-failure evidence is written under the Prod recovery subtree when possible;
- the failed request (and optional failed response) remains durable off the canonical completed head;
- the same `idempotencyKey` is never replayed automatically;
- a new `idempotencyKey` may continue from the last valid canonical head;
- the failed branch is not selected into ordinary CONTINUE prompt context.

The recovery route is explicit in the first-failure receipt:

~~~text
no prior completed head
  -> FORM_NEW_SEND_ATTEMPT_AND_RETRY

prior completed head exists
  -> RESUME_LAST_VALID_HEAD_THEN_FORM_NEW_SEND_ATTEMPT
~~~

Permanent:

~~~text
FAILED DURABLE REQUEST
!= DELETE REQUEST

FAILED SEND ATTEMPT
!= RETRY SAME INFERENCE

OFF-HEAD FAILED EVIDENCE
!= CANONICAL CONVERSATION

NEW SEND ATTEMPT
-> LAST VALID CANONICAL HEAD
~~~

## Current proof formed in source

Tests are source-formed for:

- numeric-loopback provider call + endpoint/model ownership;
- provider local-path model normalization;
- raw provider/reasoning non-persistence;
- first-turn prompt materialization;
- immediate-prior CONTINUE selection;
- prompt token-bound rejection;
- request durable before response/head;
- immutable head lineage;
- context-gated head commit;
- whole-turn writer exclusion;
- exact completed-send duplicate suppression;
- provider failure receipt + non-replaying failed-attempt recovery;
- two consecutive direct turns where turn 2 receives the immediately prior pair.

## VexBoard projectability horizon

This direct circuit should remain explicit enough that a later read-only VexBoard adapter can project
it without reverse-engineering repository convention.

This is a **downstream proof target**, not a runtime dependency and not a reason to deform the current
architecture.

Preserve now:

~~~text
stable semantic/product refs
explicit owner boundaries
explicit consume/return relationships
source-addressable provider/runtime boundaries
construction/lifetime != semantic ownership
provider observation != requested/admitted state
presentation identity != renderer selector
~~~

Later, once the fresh product service/presentation is coherent enough, form one exact source-pinned
projection route:

~~~text
VexLife-Prod source @ exact revision
  -> source-owned architecture export OR bounded read-only adapter
  -> generated VexBoard Lens snapshot
  -> VexBoard human/AI projection
~~~

Required laws:

~~~text
VEXBOARD != SOURCE AUTHORITY
ADAPTER != PRODUCT RUNTIME DEPENDENCY
GENERATED LENS != AUTHORING SOURCE
OVERLAY != SECOND SOURCE OF TRUTH
SOURCE IDENTITY != FILE PATH
SOURCE IDENTITY != DISPLAY LABEL
~~~

The preferred mature form is a source-owned export from the compiler-visible product model so the
VexBoard side performs less parser inference. The exact export schema remains held until a real
VexLife-Prod Screen/browser/Conversation/Heretic path is coherent enough to earn it.

Success means the same architecture is demonstrated two ways:

~~~text
LIVED PRODUCT
  proves the causal body executes

VEXBOARD SNAPSHOT
  proves the causal body is explicit/perceivable from source
~~~

Those proofs are complementary, not interchangeable.

## Held before acceptance

~~~text
repository-owned Kotlin 2.4.20 JVM/JS/common proof

real macOS:
  isolated local setup
  real Heretic qualification
  direct turn 1
  direct turn 2
  sustained process after setup completion

read-only lived canonical Home compatibility for Conversation v1/v2 + attributed Memory
working-checkpoint successor / rollover continuity
CLEAN_START presentation/semantic wiring
streamed response progress
explicit-open reasoning projection
product service / presentation
ordinary fresh-user Mac entry
~~~

Permanent:

~~~text
DIRECT PERSONAL COMPANION
!= FAMILY COMPANION RUNTIME

CONNECTION
!= CONVERSATION OWNER

PROMPT MATERIALIZATION
!= WHOLE HISTORY

VISIBLE RESPONSE
!= RAW PROVIDER RESPONSE PERSISTENCE

FIRST TWO-TURN SOURCE PROOF
!= LIVED FRESH-USER PRODUCT
~~~

<!-- [VXG RealForever][VEXLIFE-PROD][DIRECT-COMPANION] -->
