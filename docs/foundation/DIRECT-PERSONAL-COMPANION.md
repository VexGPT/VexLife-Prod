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
  canonical Home -> structurally current Score / semantic authority
  -> exact historical Conversation source ancestry
  -> committed daily stratum
  -> Living Journal pages when its source Conversation frontier is still current
  -> otherwise HELD / DAILY_MEMORY_SOURCE_FRONTIER_STALE
  -> bounded attributed Memory context with no provider Memory content while held
        ↓
current qualified runtime evidence
        ↓
DirectPromptContext
  current source-attributed Memory when source-current
  + owner-current working-checkpoint successor when present
  + source-formed bounded self-capability frame
  + immediate prior REQUEST/RESPONSE only when not in successor mode
  + optional admitted browser context
  + current request

  self-capability frame:
    Memory / browser / working-continuity / local-inference
    AVAILABLE / HELD / UNAVAILABLE / UNKNOWN
    effectAuthorityGranted = false

  192,000 input-token budget
  16,000 reserved output tokens
  208,000 hard limit
        ↓
Memory context + working-successor currentness re-read
before provider invocation
        ↓
fresh qualified runtime evidence
  interactive currentness = immediate generation-health observation
  Product-owned dead generation engine -> exact retire/restart/requalify
  borrowed stable runtime -> no ownership theft / no Product recovery effect
        ↓
prompt + self-capability frame materialized and reverified
while the same writer lease is held
        ↓
DirectCompanionConnection
  endpoint/model supplied only by QualifiedRuntimeEvidence
  numeric-loopback /v1/chat/completions
  stream = true
  request-local explicit-open reasoning
  certified generation_config sampling only:
    temperature = 1.0
    top_p = 0.95
    top_k = 20
  no additional request-owned sampling knobs
  request timeout bounds response-start/header availability
  streamed InputStream body may continue beyond that response-start bound
        ↓
ephemeral live progress
  CheckingRuntime
  Reconnecting (only when Product-owned recovery is required)
  Generating / Thinking
  reasoning deltas -> human presentation only
  content deltas -> progressive Vex answer
        ↓
terminal provider stream
  [DONE]
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

Memory is supplied by `VexLifeCurrentMemoryReader` from the canonical Home. Score may remain a
valid structurally-current owner while its exact Conversation source is a verified historical
ancestor. Prompt Memory is CURRENT only when the committed Daily Memory source Conversation frontier
still equals the current canonical Conversation head; otherwise the reader returns
`DAILY_MEMORY_SOURCE_FRONTIER_STALE` as HELD and supplies no Memory provider content. The reader is
re-run under the same Conversation writer lease before provider invocation and must return the exact
same context used to form the prompt receipt.

The fresh direct circuit now also carries the separately-owned
`WORKING_CHECKPOINT_SUCCESSOR` mode when one exact working-rollover owner is current. In that mode
the prior REQUEST/RESPONSE pair is not replayed; the prompt receives current Memory plus bounded
working coordinates, the live capability frame, optional browser context and the current request.

The self-capability frame is formed from live owner facts inside the prompt materialization path. It
is context about what is currently available, not a second authority owner and not an executable
tool registry.

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

### Explicit-open human reasoning projection

The local model endpoint may return a `reasoning` / `reasoning_content` field beside the final
assistant content. The Connection still records only content-free durable reasoning evidence:

~~~text
present
contentSha256
characterCount
rawPersisted = false
humanProjection = SEALED_EXPLICIT_OPEN_ONLY
modelProjection = NONE
trainingProjection = NONE
~~~

The historical VexLife relationship keeps **model reasoning mode** and **human disclosure** separate.

Current qualified Heretic runtime evidence is formed only after its rightful runtime owner verifies the
exact ordinary mlx-lm process binding, including the safe server default:

~~~text
--chat-template-args
{"enable_thinking":false}
~~~

Over that already-qualified runtime, the direct Companion Connection owns the faithful request-local
explicit-open realization and the historical live stream:

~~~text
qualified current Heretic runtime
-> DirectCompanionConnection
-> stream = true
-> chat_template_kwargs.enable_thinking = true
-> live reasoning / reasoning_content deltas
-> live content deltas
-> terminal [DONE]
-> normalized final result

reasoning deltas
-> ephemeral browser-session custody
-> visible while Viewable thoughts is enabled
-> never canonical Conversation / Memory / training / publication

content deltas
-> progressive in-flight Vex answer
-> final canonical RESPONSE still commits only after terminal completion
-> streamed answer stays visible until canonical Conversation refresh replaces it
~~~

The human's `Viewable thoughts` preference is a separate local presentation decision:

~~~text
Viewable thoughts = true
-> render available ephemeral reasoning

Viewable thoughts = false
-> keep available ephemeral reasoning hidden

Viewable thoughts
!= Send request field
!= reasoning-mode authority
!= provider prompt selection
~~~

This model-mode selection remains distinct from DirectPromptContext ownership. It does not add, remove
or rewrite the canonical provider message set, and it does not grant Memory, training, publication,
durable reasoning storage or another model/runtime identity.

Permanent:

~~~text
SERVER-OWNED REASONING MODE
!= HUMAN DISCLOSURE PREFERENCE

VIEWABLE THOUGHTS
!= PROVIDER REASONING AUTHORITY

EPHEMERAL HUMAN REASONING
!= CANONICAL CONVERSATION
!= TURN CONTEXT
!= MEMORY
!= MODEL INPUT
!= TRAINING
!= PUBLICATION
!= DURABLE STORE
~~~

The Product Send now restores the historical live Companion relationship without weakening the
transactional durability boundary:

~~~text
LIVE DELTA
!= CANONICAL RESPONSE

STREAMING PRESENTATION
!= EARLY CONVERSATION HEAD COMMIT

REQUEST durable
-> live provider stream
-> terminal provider completion
-> RESPONSE + Context durable
-> completed Conversation head
~~~

An idempotent completed duplicate may recover the durable final response without another model call,
but it cannot recover raw reasoning that was intentionally not persisted:

~~~text
fresh response + provider reasoning present
  -> PRESENT_EPHEMERAL
  -> browser may render or hide from local preference

completed duplicate + durable reasoning trace present
  -> UNAVAILABLE_AFTER_IDEMPOTENT_RECOVERY
  -> durable hash/count may remain
  -> raw text = null

completed duplicate + durable reasoning trace absent
  -> ABSENT
  -> no raw/hash/count material
~~~

The local `Viewable thoughts` preference may persist as one boolean in browser localStorage. That
stored boolean is not reasoning content or durable model state and is never sent through the Product
Send contract. Raw reasoning never enters localStorage/sessionStorage.

Reloading the Product presentation drops all ephemeral reasoning by design while preserving only the
human's local disclosure preference.

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

Interactive runtime currentness deliberately does **not** reuse startup's long readiness loop.
The exact lived regression that motivated this distinction was:

~~~text
Product service = responsive
Product-owned Heretic PID = alive
mlx-lm /health = 503
generation thread = unavailable
old interactive currentness = startup waitUntilHealthy up to 10 minutes
~~~

Current behavior:

~~~text
qualified Product-owned runtime + immediate health true
  -> continue

qualified Product-owned runtime + immediate health false
  -> Reconnecting Vex…
  -> exact-owned retire
  -> restart + startup qualification through runtime owner
  -> fresh current runtime evidence
  -> provider stream

borrowed stable VexLife runtime + currentness failure
  -> fail closed
  -> Product does not steal recovery ownership
~~~

The human-facing states are deliberately distinct:

~~~text
Checking Vex
!= Thinking
!= Responding

Reconnecting Vex
!= model generation
~~~

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
- qualified Direct Companion inference selects `stream=true` plus request-local `enable_thinking=true`;
- streamed reasoning and content deltas preserve ordering before terminal completion;
- the actual system streaming transport receives headers within the request bound and remains alive while the streamed body continues beyond that bound;
- no human/presentation field controls provider reasoning mode;
- Viewable thoughts remains absent from the Send wire request;
- live reasoning stays ephemeral while completed-response reasoning remains available in the same browser session;
- Product-owned unhealthy generation runtime fails currentness immediately and rotates through its rightful owner;
- idempotent duplicate reasoning unavailability without provider replay;
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

canonical Home/Conversation/Memory read compatibility = A030 PASS
real production-service direct turn
working-checkpoint successor / rollover continuity
CLEAN_START presentation/semantic wiring
fresh lived streamed-response proof on the current exact candidate
fresh lived Product-owned dead-runtime recovery proof
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
