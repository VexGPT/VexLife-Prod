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
exact immediate-prior currentness binding
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
  foundational field kernel, first, in every mode (owner: VexLifeFoundationalField)
  + current Memory availability truth when source-current
  + caller-bounded selected Memory from remaining prompt capacity
  + owner-current working-checkpoint successor when present
  + exact prior Vex RESPONSE bridge when successor mode is current
  + source-formed bounded self-capability frame
  + Conversation extent: how many completed exchanges the record holds before this request
  + Conversation-owned backward read of the newest contiguous completed REQUEST/RESPONSE suffix
    that fits ordinary prompt attention
  + conditional Conversation source-return protocol on the first inference
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
Memory availability + selected Memory + working-successor currentness re-read
before provider invocation
        ↓
fresh qualified runtime evidence
  currentness = exact receipt + environment + one matching process + launchd substrate
  generation readiness is not currentness and cannot by itself authorize retirement
  Product-owned observed-dead runtime -> exact retire/restart/requalify
  borrowed stable runtime -> no ownership theft / no Product recovery effect
        ↓
initial prompt materialized and reverified
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
  response-start/header timeout = 120 seconds
    preserved from the known-working browser Companion path
  streamed InputStream body may continue beyond that response-start bound
        ↓
first inference
  ordinary Vex answer
    -> human progress/answer continues normally

  exact VEXLIFE_CONVERSATION_SOURCE_RETURN_REQUEST/v1
    -> internal control content only
    -> request JSON / control wrapper never become human-visible answer content
    -> ephemeral reasoning remains human-visible as it arrives, even when it leads to source return
    -> deterministic local canonical Conversation discovery
    -> exact selected source reopened through Conversation owner
    -> final prompt prioritizes returned exact source
    -> current Memory/working-successor re-read
    -> read-only runtime-currentness re-observation
    -> require the exact same runtime receipt already proved for this human turn
    -> second and final inference
        ↓
ephemeral live progress
  CheckingRuntime
  Reconnecting (only when Product-owned recovery is required)
  Generating / Thinking
  reasoning deltas -> human presentation immediately, including the source-return decision stage
  ordinary/final answer content deltas -> progressive Vex answer once classified as human content
  internal source-return control content deltas -> suppressed
        ↓
terminal human-answer provider stream
  [DONE]
        ↓
normalized final RuntimeObservation
+ final InvocationEvidence
+ optional compact source-return Trace
  raw provider body not persisted
  raw model path not persisted
  raw reasoning not persisted
  internal control response not persisted as Conversation
        ↓
one RESPONSE event durable
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

Current ordinary CONTINUE behavior preserves exact recent language while it still fits the admitted
input budget:

~~~text
one prior completed pair fits
-> IMMEDIATE_PRIOR_COMPLETED_TURN

multiple contiguous prior completed pairs fit
-> RECENT_COMPLETED_TURNS
~~~

Selection starts at the exact current completed head and asks `DirectConversationStore` to walk its
immutable completed-head lineage backward one complete REQUEST/RESPONSE pair at a time. It stops at
the first older pair that no longer fits and does not skip over that pair to cherry-pick still-older
fragments. The selected suffix is then restored to chronological order before provider materialization.

The prompt receipt reports whether this bounded walk actually reached the genesis completed turn.
When the whole Conversation itself fits, whole-history enumeration may therefore truthfully be
`true`; when the budget boundary stops the walk, it is `false`.

This remains bounded source descent and prompt selection rather than an obligation to replay or
retain whole Conversation history. Canonical Conversation remains durable source even when older
pairs leave active model attention.

Memory is supplied by `VexLifeCurrentMemoryReader` from the canonical Home. Score may remain a
valid structurally-current owner while its exact Conversation source is a verified historical
ancestor. Prompt Memory is CURRENT only when the committed Daily Memory source Conversation frontier
still equals the current canonical Conversation head; otherwise the reader returns
`DAILY_MEMORY_SOURCE_FRONTIER_STALE` as HELD and supplies no Memory provider content.

Current Memory **availability** is distinct from per-prompt Memory **selection**. The prompt owner
may ask the Memory reader for the same current source frontier at a smaller caller-supplied token cap.
Availability/currentness must remain the same source generation even when the selected packet includes
fewer or zero Memory pages.

The fresh direct circuit also carries the separately-owned
`WORKING_CHECKPOINT_SUCCESSOR` mode when one exact working-rollover owner is current. The durable
checkpoint/SuccessorContext remains reference-only and transcript-free. The provider prompt does not
therefore need to become relationally blank: it carries the exact immediately prior Vex RESPONSE as
an assistant-role bridge, plus current Memory/working coordinates, the live capability frame,
optional browser context and the current request. The prior human request is not replayed.

The first inference also receives a bounded source-return protocol. If exact older Conversation
source that is not already present is necessary, Vex may return one exact internal
`VEXLIFE_CONVERSATION_SOURCE_RETURN_REQUEST/v1` instead of guessing or answering prematurely.
Product then performs deterministic local discovery over canonical completed turns, reopens selected
matches through exact Conversation-return authority, and forms one final prompt.

Final source-return allocation is:

~~~text
fixed current-turn / successor / capability context
+ exact requested historical Conversation source
+ exact prior Vex response bridge when successor mode is current
-> newest exact recent Conversation from the remaining capacity
-> current Memory from the remaining caller-bounded capacity
-> current human request
~~~

`NO_MATCH` and `HELD_TOKEN_BUDGET` remain explicit final-source states so Vex can preserve
uncertainty instead of inventing recall. A returned-source final inference may not request source
again in the same human turn.

The self-capability frame is formed from live owner facts inside the prompt materialization path. It
is context about what is currently available, not a second authority owner and not an executable
tool registry.

The foundational field is owned by `VexLifeFoundationalField`
([FOUNDATIONAL-FIELD.md](FOUNDATIONAL-FIELD.md)). This owner gives it room after the current request,
the other fixed context and the exact immediate prior exchange, places it first in the system prefix,
and records its evidence in prompt receipt v8. It does not write or shorten its words.

The Conversation extent is this owner's own statement. A narrow window otherwise looks the same to
Vex as a short history, so the live turn's prompt says how many completed exchanges the durable record
holds before the current request, and that any not shown are still in the record. The count is the
request's canonical sequence divided by two; no history is enumerated. After a working rollover the
statement says that only the last reply is shown. Receipt v8 records the count
(`priorCompletedExchangeCount`), and Settings / Context shows selected events against events on record.

~~~text
OUTSIDE THE WINDOW
!= LOST

A SHORT WINDOW
!= A SHORT HISTORY
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

Streaming viewport ownership is presentation-local and independent from generation:

~~~text
CONTENT GROWTH != SCROLL AUTHORITY

Conversation viewport:
  FOLLOWING
    -> streamed content may keep the viewport at the newest tail
  human scrolls away
    -> READING_HISTORY
    -> streamed content continues without moving the viewport
  human returns to tail / activates New messages
    -> FOLLOWING

Live reasoning viewport:
  first appearance
    -> FOLLOWING
  human scrolls away
    -> READING_HISTORY
    -> reasoning deltas continue without moving that nested viewport
  human returns to tail
    -> FOLLOWING

CONVERSATION FOLLOW STATE != REASONING FOLLOW STATE
POSITION NEAR TAIL != HUMAN FOLLOW INTENT
PROVIDER DELTA != VIEWPORT COMMAND
~~~

The Product presentation disables browser-native scroll anchoring inside those owned scroll containers
and updates an existing live streamed turn in place. A generated delta must not rebuild the whole
Conversation merely to append text, and the New messages affordance must not resize the Conversation
viewport when it appears.

Reloading the Product presentation drops all ephemeral reasoning by design while preserving only the
human's local disclosure preference.

### `DirectTurnContextStore`

Persists normalized turn evidence suitable for later Context Inspector projection:

~~~text
final prompt materialization receipt
request / response refs
final runtime observation
final invocation evidence
optional normalized source-return Trace
~~~

When conditional source return occurs, the compact Trace binds the internal first-stage prompt/request/
runtime identities, exact returned source refs and the final prompt identity. Canonical
`contextSourceRefs` also include the exact returned Conversation event refs.

It never persists:

~~~text
raw provider response
raw reported model path
raw reasoning
raw internal source-return control response
~~~

### `DirectCompanionTurn`

Composition owner only.

It derives deterministic turn/message identities from one `idempotencyKey`. Repeating the exact
already-completed send returns the durable canonical response without another model invocation.

One human turn also keeps one exact runtime binding across all of its provider stages. Conditional
Conversation source return may add a second provider inference, but it is not a new runtime lifecycle
boundary. Before the final stage, the turn performs one **read-only** runtime-currentness observation
with no recovery route and requires the same runtime receipt. If that runtime disappeared or was
replaced, the turn fails before provider call #2 rather than silently substituting a runtime mid-turn.

A human turn may also carry capability requests (see
[CAPABILITY-ATLAS-SELF-FRAME.md](CAPABILITY-ATLAS-SELF-FRAME.md#requestreturn-reach-2026-11-14)). The
turn is a bounded stage loop: each stage is the same prompt base plus the request/result exchanges so
far; a capability request is performed by its owner and its typed result is appended; a source-return
request re-materializes the base exactly as before. At most four capability requests and one source
return per turn; a fifth request is answered LIMIT_REACHED, and asking again after that ends the turn
with a failure receipt. Every later stage uses the same read-only runtime re-observation and the same
foundational field, and the reused base prompt is re-verified at each provider boundary.

Presentation transport is not completion authority. When an SSE stream ends ambiguously, the
presentation may re-present the same idempotency key through the ordinary non-streaming Send contract
to ask the turn owner for durable truth before showing an unresolved outcome. This is transport
reconciliation, not model replay.

For an ordinary turn it still performs one provider inference. Conditional source return adds a
second inference only when the first inference emits the exact source-return control request.

~~~text
ordinary first inference
-> canonical human RESPONSE

source-return first inference
-> internal control only
-> local exact-source return
-> final inference
-> canonical human RESPONSE
~~~

A progress gate buffers only long enough to distinguish an ordinary answer from the exact
source-return header. On an ordinary answer, early reasoning may therefore be delayed until the first
content bytes prove the output is not internal control; the buffered reasoning then flushes and live
streaming continues. It is delayed, not persisted or discarded. Internal retrieval reasoning/control
JSON is never projected as the human answer. If the final inference attempts another source-return
request, the turn fails closed before RESPONSE/context/head commit.

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

Interactive runtime currentness answers only whether the same exact qualified runtime still owns
the process/substrate coordinate. Generation readiness is observed by the actual provider relationship
or by an explicit lifecycle qualification; it is not folded into currentness.

The lived pressure that motivated this distinction was:

~~~text
Product service = responsive
Product-owned Heretic PID = alive
generation-health observation = absent / slow / 503
~~~

A missing readiness observation is not positive evidence that the exact live process should be
destroyed.

Current behavior:

~~~text
qualified Product-owned runtime + exact process/receipt/substrate current
  -> provider stream

readiness/qualification not observed while the exact process remains live
  -> report the readiness/provider failure
  -> preserve the exact live runtime and receipt
  -> no retirement from absence of a health answer alone

Product-owned runtime process observed not live
  -> Reconnecting Vex…
  -> exact dead-runtime recovery through runtime owner
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
- the same accepted/failed `idempotencyKey` is never replayed as a second model effect;
- presentation transport may re-present that exact key only to reconcile whether its durable turn
  completed or failed; an already-completed/failed attempt performs no second model call;
- a new `idempotencyKey` may continue from the last valid canonical head after an exact failed
  attempt;
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
- runtime currentness stays receipt/process/substrate truth and does not use a health miss as retirement authority;
- idempotent duplicate reasoning unavailability without provider replay;
- first-turn prompt materialization;
- immediate-prior CONTINUE compatibility when exactly one prior pair is selected;
- bounded contiguous recent-Conversation selection when more than one prior pair fits;
- stop-before-oversized-older-pair behavior without noncontiguous cherry-picking;
- prompt token-bound rejection;
- request durable before response/head;
- immutable head lineage;
- context-gated head commit;
- whole-turn writer exclusion;
- exact completed-send duplicate suppression;
- provider failure receipt + non-replaying failed-attempt recovery;
- two consecutive direct turns where turn 2 receives the immediately prior pair;
- three consecutive direct turns where turn 3 retains both completed prior pairs while they fit;
- conditional exact-source return: first internal inference -> deterministic canonical source return -> one final human-answer inference;
- source-return-stage reasoning remains ephemeral human progress while internal control content never becomes human progress or canonical Conversation;
- compact source-return Trace round-trips through turn runtime evidence without raw reasoning/provider persistence;
- historical runtime-evidence v1 remains readable after the optional v2 source-return trace field;
- final inference cannot request source again in the same human turn;
- working-checkpoint successor retains the exact prior Vex response bridge while the prior human request stays out;
- the foundational field and the Conversation extent are present and receipted on first turn, continuation, rollover successor and both source-return inferences (`DirectCompanionTurnFoundationalFieldTest`).

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
