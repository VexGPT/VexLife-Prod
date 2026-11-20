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
  one stable system block:
    foundational field kernel, first, in every mode (owner: VexLifeFoundationalField)
    + current Memory availability truth when source-current
    + caller-bounded selected Memory from remaining prompt capacity
    + stable capability relationship guidance; provider-native tool schemas carry callable actions
  then exact history:
    + Conversation-owned backward read of the newest contiguous completed REQUEST/RESPONSE suffix
      that fits ordinary prompt attention (after a rollover: the exact overlap tail)
  then one current-state envelope (VexLife's, user role, VEXLIFE_CURRENT_STATE/v1):
    + owner-current working-checkpoint successor when present
    + this request's capability states and frame ref
    + person context: reply language and private gaps, read fresh from their owners
    + Conversation extent: how many completed exchanges the record holds before this request
    + conditional Conversation source-return protocol on the first inference
  then returned exact older source, when this inference carries it
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
  internal control response not persisted as Conversation
        ↓
one RESPONSE event durable
        ↓
DirectTurnContextStore
  prompt materialization receipt
  + request/response identity
  + normalized runtime observation
  + invocation evidence
  + exact response-bound reasoning continuation when the provider emitted one
        ↓
later prompt selection
  exact RESPONSE selected
  -> provider representation is chosen from the exact pinned model/template
     27B preserved-thinking template
       -> reasoning_content beside the visible RESPONSE
       -> preserve_thinking=true
     9B / 4B current-turn-only template
       -> stable provider-only assistant-history envelope
       -> reasoning + visible RESPONSE remain one assistant history item
       -> preserves reasoning meaning in the model's token stream
       -> does NOT claim byte/token identity with the original generated <think> sequence
       -> KV-prefix benefit remains a lived measurement, not a source assumption
     unknown/imported/Harmony template
       -> historical reasoning replay HELD until that exact representation is qualified
  -> reasoning representation consumes ordinary prompt room
  -> newest selected reasoning is kept first when all selected reasoning cannot fit
  -> bounded-out reasoning is named on the prompt receipt
  -> canonical Conversation RESPONSE text remains unchanged
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

Exact older Conversation Source Return remains a distinct one-return-per-human-turn relationship.
Provider-native tool profiles receive it as the typed `conversation_return` route; a text-only
fallback representation receives the bounded `VEXLIFE_CONVERSATION_SOURCE_RETURN_REQUEST/v1`
protocol instead. Both representations converge on the same `DirectConversationSourceReturn`
request owner. Product then performs deterministic local discovery over canonical completed turns,
reopens selected matches through exact Conversation-return authority, and forms one final prompt.

Final source-return provider order is causal rather than context-only:

~~~text
verified turn / successor / capability context
-> newest exact recent Conversation from the remaining capacity
-> current Memory from the remaining caller-bounded capacity
-> current human request
-> assistant source-return request
-> exact requested historical Conversation source
-> final provider continuation
~~~

Provider-native profiles re-project the last two items as one assistant tool call + native tool
result. The current 9B/4B text-control template receives the returned source in its existing
`<tool_response>...` compatibility form so it remains part of the same human query rather than
becoming a new human request.

`NO_MATCH` and `HELD_TOKEN_BUDGET` remain explicit final-source states so Vex can preserve
uncertainty instead of inventing recall. A returned-source final inference may not request source
again in the same human turn.

The self-capability frame is formed from live owner facts inside the prompt materialization path. It
owns current availability only. The Capability Atlas owns the semantic action definitions, and the
selected provider representation compiles those actions into the model's native tool schema. A tool
being declared to the model is not proof that it is AVAILABLE in this turn; the Bridge checks the
frame again before any source owner is read.

The foundational field is owned by `VexLifeFoundationalField`
([FOUNDATIONAL-FIELD.md](FOUNDATIONAL-FIELD.md)). This owner gives it room after the current request,
the rest of the verified turn context and the exact immediate prior exchange, places it first in the system prefix,
and records its evidence in the current prompt receipt. It does not write or shorten its words.

### Current state is placed as current

Who owns a value and when it changes are different questions. Person context, this request's capability states, the Conversation extent, Working Continuity
coordinates and any text-fallback source-return protocol all change independently of the history they
would otherwise precede, so they are placed after it, in one envelope immediately before the request,
and say they are current at this request. Provider-native callable routes are carried by the selected
model representation instead of being repeated inside that current-state envelope. The leading system block holds only what is stable across turns (field, Memory, the
capability description). A setting change is therefore visible on the very next prompt, reads as
current rather than as a fact from the Conversation's beginning, and changes no byte of the stable
block or the history before it.

The envelope is a user-role message because the pinned Qwen chat template admits one system message,
first only, and raises `System message must be at the beginning.` for any later system message.
Browser context uses the same user-role host envelope with explicit "not from the person" provenance.
Capability results follow the selected provider representation instead: provider-native profiles
receive an assistant tool call followed by a native `tool` result, while the text-control fallback
retains the canonical VexLife result in the user-role compatibility form.

~~~text
OWNERSHIP / ROLE != CURRENTNESS / PLACEMENT
CANONICAL CURRENT STATE != STABLE PROMPT PREFIX
PROVIDER SERIALIZATION != SEMANTIC AUTHORITY
~~~

The current prompt receipt records each component as placed (`contextComponents`: stability class, placement class,
provider message index, digest), formed in the same assembly as the provider messages.

The Conversation extent is this owner's own statement. A narrow window otherwise looks the same to
Vex as a short history, so the live turn's prompt says how many completed exchanges the durable record
holds before the current request, and that any not shown are still in the record. The count is the
request's canonical sequence divided by two; no history is enumerated. After a working rollover the
statement says that only the most recent exact turns are shown and working coordinates carry the rest. The current prompt receipt records the count
(`priorCompletedExchangeCount`), and Settings / Context shows selected events against events on record.

~~~text
OUTSIDE THE WINDOW
!= LOST

A SHORT WINDOW
!= A SHORT HISTORY
~~~

#### Misunderstanding / failed-recall diagnosis

A failed answer is not enough evidence to name its cause. The Foundation's investigative judgment and
continuity relationships keep these layers distinct:

~~~text
SOURCE_EXISTENCE
  Was the fact/lesson ever in canonical source?

RESIDENCY
  Was that source in the exact provider material for this inference?

SEMANTIC_RECOGNITION
  Did the model connect the person's present wording to the older meaning?

ROUTE_SELECTION
  If the source was not resident, did the model choose Source Return / revisit / another rightful route?

PROVIDER_CARRIAGE
  Did that model/provider representation carry the request in a form VexLife could consume?

OWNER_RETURN
  Did the rightful owner actually return the requested source/evidence?

INTEGRATION
  Did the final inference use the returned source correctly?
~~~

A later-layer failure must not be projected backward into an earlier one.

The Memory Depth pressure probes are interpreted through this ladder. For example, Meridian-7 was
introduced in canonical synthetic Conversation before pressure. The later paraphrased probe failing
while generic revisit use remained zero establishes neither "the source never existed" nor "durable
continuity forgot it." It narrows the live question toward semantic recognition / route selection
unless later evidence shows a different layer.

The reasoning-continuation OFF/ON comparison must therefore expose enough evidence to classify each
probe instead of emitting only PASS/FAIL:

~~~text
human probe
+ exact resident context refs
+ prior reasoning supplied (or OFF)
+ current reasoning
+ provider request / tool-source decision
+ returned source, if any
+ final answer
+ assertion result
-> first unsupported layer / deepest evidence-backed layer
~~~

The point is not to force five "why" tokens into every response. The inquiry recurses only while the
current diagnosis is still an assumption.

#### Computation cache boundary

Prompt/KV cache is runtime computation, not continuity custody. `DirectPromptContext` owns truthful
bounded provider materialization; Conversation, Memory, Working Continuity and the other source owners
remain sufficient to reconstruct that materialization after cache eviction, process restart or model
switch.

~~~text
SEMANTIC / DURABLE TRUTH
-> may invalidate provider computation

PROMPT / KV CACHE
-> may reduce repeated computation
-> may be discarded at any time

CACHE HIT / MISS
!= CONTINUITY SUCCESS / LOSS
~~~

The current Product does not claim that its provider message ordering is cache-optimal. Provider-stage
measurement records prompt, cached and prefill token counts so later optimization can be judged by
observed runtime reuse without moving semantic truth into the cache or lying about provider roles.
The 27B preserved-field representation is intended to reconstruct the provider's native historical
thinking form. The 9B/4B assistant envelope is intentionally a semantic-continuity fallback over their
current pinned templates; whether it improves, preserves, or reduces KV reuse must be measured in the
lived A/B rather than inferred from source shape.

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

For provider-native profiles, the Connection also records request-side tool exposure without creating
another tool registry:

~~~text
providerToolCount
providerToolSchemaSha256
requestBodySha256
~~~

The first two bind the exact Atlas-derived callable schema offered to that provider stage; the whole
request hash binds the schema together with messages, generation settings and other request fields.
A capability being mentioned in Foundation/context therefore does not substitute for evidence that
the provider actually received its native tool declaration.

### Explicit-open human reasoning projection

The local model endpoint may return a `reasoning` / `reasoning_content` field beside the final
assistant content. The Connection's normalized RuntimeObservation remains content-free:

~~~text
present
contentSha256
characterCount
rawPersisted = false
humanProjection = SEALED_EXPLICIT_OPEN_ONLY
~~~

The exact response-bound reasoning value may additionally be retained by `DirectTurnContextStore`
for **provider continuation**. That is a different custody relationship from the RuntimeObservation and
from the human's live-thought presentation.

~~~text
provider response
├─ visible answer
│    -> canonical Conversation RESPONSE
└─ explicit reasoning
     -> live human thought projection
     -> exact response-bound provider continuation in turn evidence
        -> re-enters model input only when that exact RESPONSE is selected
        -> may be bounded out by active-context pressure
        -> never becomes canonical Conversation or semantic Memory merely by being retained
~~~

The relationship keeps **model reasoning mode**, **provider continuation**, and **human disclosure**
separate. Historical continuation is also separate from same-turn continuation: a template can carry
reasoning between tool/source stages after the current request without being able to replay that same
reasoning across a later human turn.

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
-> ordinary Product Qwen inference: chat_template_kwargs.enable_thinking = true
-> 27B: reasoning_effort = xhigh by current source default
         qualified Labs choices = low / medium / xhigh
         preserved-thinking representation additionally sends preserve_thinking = true
-> 9B / 4B: qualified thinking on/off switch
              no qualified reasoning-depth tier in their pinned templates
              no false preserve_thinking claim
-> Vex Labs may override only choices the exact selected profile exposes, for that scratch request
-> live reasoning / reasoning_content deltas
-> live content deltas
-> terminal [DONE]
-> normalized final result

reasoning deltas
-> ephemeral browser-session human presentation
-> visible while Viewable thoughts is enabled
-> terminal reasoning may also become response-bound provider continuation
-> never canonical Conversation / Memory / training / publication merely because it is retained

content deltas
-> progressive in-flight Vex answer
-> final canonical RESPONSE still commits only after terminal completion
-> streamed answer stays visible until canonical Conversation refresh replaces it
~~~

Vex's human-visible text uses one `TextPresentation` relationship (2026-11-15). The exact
text remains owned upstream; presentation does not change its custody or durability.

~~~text
owner-provided exact text
├─ live reply text
│    owner while in flight: streamedContent / Send presentation
│    terminal owner: canonical Conversation RESPONSE
│
└─ human-visible reasoning text
     owner while present: ephemeral browser-session reasoning/process custody
     never canonical Conversation / Memory
        ↓
TextPresentation
  asks only: how should this exact text become human-readable?
  current interpretation grammar: Markdown patterns
        ↓
current platform manifestation: Web DOM
├─ reply body inside the Conversation timeline
└─ reasoning body inside the mounted .reasoning viewport
        ↓
rightful interaction owner remains separate
├─ reply geometry -> timeline follow/read state
└─ reasoning geometry -> reasoningViewport follow/read state

TEXT PRESENTATION != TEXT TRUTH OWNER
TEXT PRESENTATION != CONVERSATION / MEMORY
TEXT PRESENTATION != REASONING CUSTODY
TEXT PRESENTATION != RELAY / CONVERGENCE
SAME PRESENTATION GRAMMAR != SAME DURABILITY
SAME PRESENTATION GRAMMAR != SAME SCROLL OWNER
INTERPRETATION GRAMMAR != PRESENTATION OWNER
PLATFORM MANIFESTATION != PRESENTATION OWNER
DOM IDENTITY != RESPONSE / REASONING IDENTITY

current Markdown patterns -> paragraph / heading / quote / list / table / code / inline emphasis / links
links                     -> http(s) / mailto only; others stay text; target=_blank rel=noopener noreferrer
tables / code             -> wrap inside the host surface; TextPresentation creates no nested scroller
presentation failure      -> exact owner-provided text remains visible as plain text
Speech / Kokoro           -> consume the raw upstream text, never the visual presentation tree
~~~

The live presentation queue batches all mounted TextPresentation surfaces at most once per animation
frame. The queue is not a currentness owner. After a presentation changes geometry, the host surface's
own interaction owner responds: `reasoningViewport` may follow the thought tail while the Conversation
timeline independently follows or preserves the person's reading position.

`TextPresentation` is intentionally broader than Markdown and Web. Markdown is one interpretation
grammar and Web DOM is one manifestation of the relationship. A future template grammar,
Android/Compose manifestation, iOS manifestation or other presentation technology should start from
the same relationship rather than duplicate this Web DOM machinery.

A shared typed presentation structure is not yet claimed. If two or more real consumers need the same
intermediate structure, that repeated pressure may earn one above the platform manifestations. Until
then, current Web source remains one concrete realization and must not become cross-platform authority.

Why this is presentation rather than convergence: every recognized structure is derived from one exact
owner-provided text source. It does not compose independently-owned truths into new relationship truth.
No durable presentation AST, HTML string, Conversation representation or Memory representation is
introduced. The presentation-local cache exists only to avoid repeated Web manifestation work and
preserve human interaction with already-mounted visible structure.
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

This model-mode selection remains distinct from DirectPromptContext ownership. `Viewable thoughts`
changes only what the human sees; it does not decide whether a completed model turn preserves its own
response-bound reasoning for later provider continuation.

Permanent:

~~~text
SERVER-OWNED REASONING MODE
!= HUMAN DISCLOSURE PREFERENCE

VIEWABLE THOUGHTS
!= PROVIDER REASONING CONTINUATION

HUMAN LIVE-THOUGHT PRESENTATION
!= CANONICAL CONVERSATION
!= MEMORY
!= TRAINING
!= PUBLICATION

RESPONSE-BOUND REASONING CONTINUATION
= exact turn-context evidence bound to one completed RESPONSE
= provider input only when that exact RESPONSE is selected
!= canonical Conversation text
!= semantic Memory
!= training
!= publication

VISIBLE RESPONSE
+ RESPONSE-BOUND REASONING
-> one provider-history assistant turn
-> active-context pressure may later evict either older turn or its optional reasoning
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

An idempotent completed duplicate may recover the durable final **human answer** without another model
call. It still does not replay the live-thought UI. Separately, the exact turn context may retain the
reasoning continuation so a later fresh Send can reconstruct the model's selected provider history:

~~~text
fresh response + provider reasoning present
  -> human live projection may render/hide it
  -> turn context binds exact reasoning to exact RESPONSE

same idempotency key recovered
  -> no provider replay
  -> humanReasoning = null
  -> canonical final answer recovered

later fresh Send selects that RESPONSE
  -> 27B receives reasoning_content + visible answer through native preserved thinking
  -> 9B / 4B receive VEXLIFE_ASSISTANT_REASONING_CONTINUATION/v1 in assistant content
     because their pinned template strips old reasoning_content before a new human query
  -> unknown/imported/Harmony historical replay remains held
  -> prompt receipt names selected reasoning event refs and token cost

reasoning no longer fits active prompt
  -> visible Conversation selection remains authoritative
  -> reasoning may be bounded out
  -> receipt names the bounded-out RESPONSE ref
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

Reloading the Product presentation drops the **live thought viewport** by design while preserving the
human's local disclosure preference. Response-bound reasoning continuation is independent of that
presentation lifetime and remains in the exact turn-evidence sidecar until ordinary source retention
policy changes it.

### `DirectTurnContextStore`

Persists normalized turn evidence suitable for later Context Inspector projection:

~~~text
final prompt materialization receipt
request / response refs
final runtime observation
final invocation evidence
optional normalized source-return Trace
optional response-bound reasoning continuation
~~~

When conditional source return occurs, the compact Trace binds the internal first-stage prompt/request/
runtime identities, exact returned source refs and the final prompt identity. Canonical
`contextSourceRefs` also include the exact returned Conversation event refs.

It never persists:

~~~text
raw provider response
raw reported model path
raw internal source-return control response
~~~

The reasoning continuation is deliberately narrower than the raw provider response: it is the exact
provider reasoning field bound to the completed RESPONSE's turn evidence so the same selected model
history can be reconstructed without making that text a Conversation event or Memory fact.

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
far. When the exact template qualifies same-turn reasoning fields, the reasoning that led to the
current request is retained on that assistant request and consumes the same reserved exchange room
before the host result is sized. A capability request is performed by its owner and its typed result
is appended; a source-return request re-materializes the base in causal request -> source-request ->
source-result order. At most four capability requests and one source
return per turn. A fifth request is answered LIMIT_REACHED and explicitly closes the reach phase:
Vex should answer from established truth, or—when material outside truth is still missing—share what
is currently understood, name the missing truth and ask the person for another message so the work
can continue in a fresh human turn with a fresh bounded request budget. The limit is not a target and
does not itself mean the human turn failed. Asking for another machine capability after that returned
limit still fails closed with a failure receipt. Every later stage uses the same read-only runtime
re-observation and the same foundational field, and the reused base prompt is re-verified at each
provider boundary.

Presentation transport is not completion authority. When an SSE stream ends ambiguously, the
presentation may re-present the same idempotency key through the ordinary non-streaming Send contract
to ask the turn owner for durable truth before showing an unresolved outcome. This is transport
reconciliation, not model replay.

Presentation must preserve the human-visible process until that canonical reconciliation returns:

~~~text
stream fault / terminal projection interruption
-> freeze + snapshot the browser-session process already observed
-> ask the canonical turn owner

TURN_COMPLETED
-> attach the preserved process to the completed response
-> refresh canonical Conversation / Context
-> retire transient live state only after transfer

TURN_FAILED
-> preserve one session-only failed-attempt projection
   (process + failure code + exactNextSafeRoute)
-> retire the failed idempotency key
-> offer Try Again as a NEW Send identity
-> do not copy the attempted prompt back into the composer as if no Send occurred

UNRESOLVED
-> retain the same pending idempotency key
-> retain the same process projection
-> later exact reconciliation does not reset that process slot
~~~

The **human live-thought projection** remains browser-session-local. Completed response-bound reasoning
may persist independently in turn evidence for provider continuation. That persistence still does not
create Conversation, Memory, training, or a human-visible historical-thought transcript.

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

The progress gate withholds only assistant **content** while it could still be an internal
control envelope. Reasoning is projected as it arrives, including reasoning that leads to a
capability/source-return request, so the Product does not recreate the lived regression where minutes
of thinking appear as one late block. Once content is classified as an internal control, that control
content remains machine-only. If the final inference attempts another source-return request, the turn
fails closed before RESPONSE/context/head commit.

Physical delivery of those progress events is a separate service/presentation responsibility. The
canonical provider/turn path must not wait on a browser socket merely because the human projection is
slow; see `LOCAL-PRODUCT-SERVICE.md`.

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
- raw provider-response non-persistence while response-bound reasoning is retained only in exact turn evidence;
- qualified Direct Companion inference selects `stream=true` plus the selected profile's request-local thinking/depth configuration; ordinary 27B source defaults to `enable_thinking=true` + `reasoning_effort=xhigh`;
- streamed reasoning and content deltas preserve ordering before terminal completion;
- the actual system streaming transport receives headers within the request bound and remains alive while the streamed body continues beyond that bound;
- no human/presentation field controls provider reasoning mode;
- Viewable thoughts remains absent from the Send wire request;
- live reasoning stays presentation-ephemeral while completed-response reasoning can continue into a later selected provider turn;
- runtime currentness stays receipt/process/substrate truth and does not use a health miss as retirement authority;
- idempotent duplicate performs no provider replay and does not resurrect the live-thought UI;
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
- source-return-stage reasoning remains human-visible progress and is also carried into the final provider stage beside the exact source-return request;
- compact source-return Trace round-trips through turn runtime evidence without persisting the raw provider response;
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
