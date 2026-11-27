# Capability Atlas + Self-Capability Frame

`[VXG RealForever]`

## Human question

How can the current Vex know which capabilities exist, receive their callable shape in the native
representation the selected model already understands, and still distinguish that from what is
actually available and performed now?

The answer is one semantic registry projected through two separate relationships:

~~~text
Capability Atlas action definition
        ↓
provider-native tool schema
  name / purpose / typed arguments / returned shape

registered capability
∩ active role
∩ current platform
∩ project grant
∩ permission state
∩ resource state
        ↓
bounded current self-capability frame
  AVAILABLE / HELD / UNAVAILABLE / UNKNOWN
~~~

The native tool declaration says which callable relationship the model knows how to express. The
frame reports current capability truth. Neither executes anything or grants an effect; the Bridge
owns admission and delegates the read to the rightful source owner.

## Stable provenance

Stable VexLife established the useful principle:

~~~text
fresh model turn
-> compact relevant capability handles
-> full detail remains outside the prompt
~~~

and the maturity vocabulary:

~~~text
DISCOVERABLE
EXPLAINABLE
REQUESTABLE
ADMITTED
EXECUTABLE
COMPLETED
~~~

A stronger stage is never inferred from a weaker one.

VexLife-Prod does not currently own stable VexLife's full registry, Atlas query stack, model-connection
compiler or root-kernel introspection APIs. This source therefore ports the **semantic relationship**,
not the old implementation topology.

## Current product-owned registry

The first registry contains only capabilities with an already-accepted fresh product owner:

~~~text
capability.vexlife.context.memory-current
  source:
  src/jvmMain/kotlin/vexlife/memory/VexLifeCurrentMemoryReader.kt

capability.vexlife.context.browser-current
  source:
  src/commonMain/kotlin/vexlife/browser/VexLifeProdBrowserContract.kt

capability.vexlife.context.working-continuity
  source:
  src/jvmMain/kotlin/vexlife/companion/DirectWorkingContinuity.kt

capability.vexlife.companion.local-inference
  source:
  src/jvmMain/kotlin/vexlife/companion/DirectCompanionConnection.kt
~~~

The registry deliberately does **not** claim stable-era capabilities such as `capability.search`,
`capability.describe`, `context.where`, navigation, file editing, public publication, Dream or
training merely because those names exist elsewhere.

## Maturity versus current availability

Each registered capability has a canonical maturity stage and a separate current frame state:

~~~text
AVAILABLE
HELD
UNAVAILABLE
UNKNOWN
~~~

Example:

~~~text
capability canonicalStage = EXECUTABLE
resource state            = UNAVAILABLE
frame state               = HELD
~~~

This means the product knows the capability contract is executable when its predicates hold; it does
not claim those predicates currently hold.

Current frame-state law:

~~~text
role not admitted        -> UNAVAILABLE
platform not admitted    -> UNAVAILABLE
project grant absent     -> HELD
permission denied        -> HELD
permission unknown       -> UNKNOWN
resource unavailable     -> HELD
resource unknown         -> UNKNOWN
all admitted/current     -> AVAILABLE
~~~

The current four grounded definitions use `permission.none`; no fresh permission owner is invented.
The permission gate exists for later registered capabilities that actually have a rightful permission
source.

## No authority transfer

Permanent:

~~~text
CAPABILITY FRAME
!= EFFECT AUTHORITY

AVAILABLE
!= MODEL MAY EXECUTE BY ITSELF

EXECUTABLE STAGE
!= EFFECT PERFORMED
~~~

Every registry entry and every frame carries:

~~~text
effectAuthorityGranted = false
~~~

The actual effect owners remain unchanged:

~~~text
Memory read
-> VexLifeCurrentMemoryReader

browser admission
-> VexLifeProdBrowserContract / current browser-context owner

working continuity
-> VexLifeWorkingContinuityOwner + DirectWorkingContinuity

local inference
-> DirectCompanionConnection consuming current qualified runtime evidence
~~~

## Compact relevance and token bounds

`formFrame(...)` accepts only explicitly relevant registered capability refs and sorts them by
source-owned canonical priority before token selection.

The frame records:

~~~text
tokenBudget
usedTokens
omittedCapabilityRefs
omissionReasons = TOKEN_BUDGET
truncated
~~~

`usedTokens` is the deterministic estimate of the **final canonical Frame materialization**, including
current context, selected entries, aggregate state/source projections, coverage metadata and the
fixed-length content-addressed identity fields. It is not merely the sum of selected entry costs.

Selection walks source-owned priority order and keeps the highest-priority prefix whose complete
materialized frame fits the budget. Lower-priority entries are omitted with `TOKEN_BUDGET`. If even
the truthful empty-selection frame (metadata/current context plus omission accounting) cannot fit,
formation fails closed.

The default budget is sized for the current four-capability frame; smaller callers may deliberately
receive a deterministic truncated frame.

The frame carries only bounded refs/currentness metadata. It does not carry full implementation
schemas, source files, credentials, private payloads or capability instructions.

## Current context

The self-frame may name bounded current refs only:

~~~text
homeRef
companionLineageRef
threadRef
currentRequestEventRef
promptReceiptRefOrNull
memoryContextRefOrNull
browserContextRefOrNull
workingContinuityContextRefOrNull
runtimeObservationRefOrNull
~~~

These refs are context identity, not authority.

## Request/return reach — provider-native carriage

The Capability Atlas is the semantic tool contract. The selected model/provider relationship compiles
that contract into the tool channel the model was trained to use.

~~~text
Capability Atlas
  handle / action / typed arguments / purpose / returned shape
        ↓
ProviderCapabilityTools
        ├─ Qwen/OpenAI-compatible tools -> mlx-lm tool_calls
        └─ GPT-OSS Harmony functions -> Harmony recipient call
        ↓
one canonical VexLife capability request meaning
        ↓
VexLifeCapabilityBridge
  current frame admission
  + known action / required arguments
  + request count (≤ 4)
  + token room left
        ↓
the capability's own owner performs one read
  atlas     list / describe                              VexLifeCapabilityAtlas (effect-free)
  web       fetch / find / search                        reach/VexLifeWebReach + turn snapshot
  github    describe / tree / read (public)              reach/VexLifeRepositoryReach
  home      list / read (inside the Home, no links)      reach/VexLifeHomeReach
  memory    paged search current pages                   reach/VexLifeMemoryRecall
  continuity revisit                                     VexLifeContextRevisit
        ↓
canonical VexLife result meaning
        ↓
provider-native tool-result message
        ↓
Vex continues the same human turn
~~~

`VEXLIFE_CAPABILITY_REQUEST/v1` and `VEXLIFE_CAPABILITY_RESULT/v1` remain the canonical internal
Bridge/fallback representation. Shipped native-tool model profiles do not need to learn that syntax
as their ordinary model-facing tool grammar.

Text-control reach is intentionally **recoverable within the bounded request budget**. A reply that
contains the request header but has unreadable request form is not treated as if no reach was
attempted and is not silently normalized into a different request:

~~~text
request header + malformed/unreadable request
-> VexLifeCapabilityRequest.Parsed.Malformed
-> one request is consumed
-> VexLifeCapabilityBridge returns INVALID_REQUEST
-> returned result includes the exact canonical form
-> Vex may correct the request in the next provider stage of the same human turn
~~~

Likewise, a readable request for an unknown / unavailable handle or wrong action receives the
ordinary owner-shaped result and may be corrected within the same bounded turn. This recovery path
does not widen authority: only the exact later request that passes current Bridge/owner admission can
perform an effect.

The exact-runtime text-control qualification mirrors this Product-owned recovery relationship. It
does not grant aliases such as invented field names, does not pretend malformed control succeeded,
and does not require first-shot serialization perfection when ordinary VexLife itself would have
returned corrective evidence.

Ordinary text-control prompt projection and exact-runtime qualification consume the same
`VexLifeCapabilityRequest.TextControlProviderInstruction`. Qualification may add only its bounded
synthetic goal; it does not own a second request grammar. This keeps the request marker, inert bare-JSON
boundary and result-return relationship source-owned by the request contract.

The parser is forgiving about **representation**, not meaning. Besides the canonical header-line
form, it accepts one exact structured protocol/version wrapper when the model emits the same request
meaning as nested JSON:

~~~json
{
  "VEXLIFE_CAPABILITY_REQUEST": {
    "v1": {
      "capability": "atlas",
      "action": "list",
      "arguments": {}
    }
  }
}
~~~

That wrapper is not a new capability alias or alternate authority path. The outer key must be exactly
`VEXLIFE_CAPABILITY_REQUEST`, the only version key must be exactly `v1`, and the inner request is
validated by the same capability/action/arguments parser as the canonical header form. The model's
original control bytes remain the assistant-side exchange evidence; only the internal request meaning
is normalized before ordinary Atlas/Bridge admission. Unrelated JSON remains ordinary model content.

~~~text
DECLARED TOOL != AVAILABLE THIS TURN
AVAILABLE != REQUEST ADMITTED
REQUEST ADMITTED != SOURCE READ SUCCEEDED
SOURCE RESULT != MODEL CLAIM ABOUT THE RESULT
~~~

For public web reads, one human turn now reuses one observed page snapshot for `find` and later
`fetch(offset=...)` slices, so continuation stays bound to the same observed bytes instead of
re-downloading a moving page. The underlying network observation remains bounded; an incomplete
2 MiB observation reports `networkTruncated=true` rather than pretending the rest was read.

Memory search returns a bounded page of lexical matches plus `totalMatches`, `nextOffset` and
frontier statement refs when more matches remain. It remains lexical source lookup, not a semantic
embedding index.

Public GitHub file reads keep one observed file snapshot for the human turn; `read(offset=...)`
continues through those exact observed bytes instead of downloading the beginning again. Home text
reads may switch to `startLine` / `maxLines` traversal, and the returned `nextLine` always names
the first whole line that was not actually returned.

The provider invocation receipt binds `providerToolCount` and `providerToolSchemaSha256` beside the
whole request-body hash. That is inspectable evidence that a model was actually offered a native tool
schema; capability prose alone is not treated as proof of tool exposure.

For Home-imported Qwen models, no-effect import inspection reads the exact pinned small provider
template/config and records whether the native Qwen tool grammar is present. Native-capable imports
use the provider tool field; imports without that grammar retain the text-control fallback. Both
representations use this same Atlas and Bridge.

The per-turn request bound is a safety/current-attention boundary, not a target and not a
conversation-failure condition. A fresh human message may begin another bounded turn with a fresh
request budget while canonical Conversation carries the prior human-visible response forward. If Vex
ignores the returned limit truth and emits another machine-control request anyway, the existing
fail-closed turn guard still applies.

Conversation Source Return keeps its own lived request form and once-per-turn law; the Atlas lists
it as `conversation` so one `atlas list` names everything Vex can reach.

State expectations that are easy to blur:

~~~text
AVAILABLE in the frame      = the bridge will attempt the request
!= the read will succeed      (a failed read is a FAILED result Vex sees and judges)

frame                        = description, grants nothing
admission + owner checks     = what actually decides a read

reach result content         = in the provider messages of that turn only
trace                        = callRef, hashes, state, target, owner — never the content
~~~

Why these are AVAILABLE now: earlier frames held web/repository/Home reach because no owner existed.
The owners exist in this source, Victor asked for them to be usable, and each owner checks its own
predicates per request — public http(s) targets on standard ports re-admitted at every redirect hop
(no loopback, LAN, link-local, CGNAT or ULA), bounded bytes, Home containment without following
links, binary refused as text. No owner holds a credential; GitHub reach is public-only and its
unauthenticated rate limit comes back as a FAILED result when spent.

### What Vex reads

When requests are admitted, the prompt carries capability truth in two complementary projections.

The stable capability instruction is generated from the Atlas and names the callable read routes,
their actions/examples and the exact request relationship.

The current-state block then carries this turn's exact frame state
(`Now: AVAILABLE — … Not present this turn — … (frame ref)`). For separately-owned bounded effect
capabilities such as `workspace`, `workspace-check` and `workspace-test`, an AVAILABLE frame
entry also projects that capability's exact Atlas-derived action/example plus its current Local Source
coordinate. A HELD / UNAVAILABLE / UNKNOWN effect capability does **not** project its action form as a
currently usable hand.

~~~text
AVAILABLE BOUNDED EFFECT
-> compact current action form is perceptible now
-> model need not spend an Atlas request merely to discover syntax already admitted this turn

HELD / UNAVAILABLE / UNKNOWN EFFECT
-> state remains perceptible
-> no current effect request form is presented as usable
~~~

Permanent:

~~~text
CONTEXTUALLY VISIBLE REQUEST FORM
!= EFFECT AUTHORITY
!= EFFECT PERFORMED

ATLAS-DERIVED CURRENT AFFORDANCE
!= ATTENTION SELECTION
!= NEW CAPABILITY OWNER
~~~

The host Bridge and rightful effect owner still re-check exact current authority, and only a real
`VEXLIFE_CAPABILITY_RESULT/v1` return is evidence that an effect happened.

The full frame JSON is not pasted into the prompt: the receipt still records the frame ref,
fingerprint, state snapshot and exact system-message SHA-256. Lived pressure behind this ordering:
the JSON was "descriptive, not a hand"; current capability truth must be legible enough for Vex to
recognize the hand it actually has without turning the prompt into the whole registry.

### Prompt room

Admitting requests keeps `VexLifeCapabilityBridge.ExchangeAllowanceTokens` (24,000) free at
materialization. The receipt records it as `capabilityExchangeAllowanceTokens` and its
`inputTokenBudget` is already reduced by it (`DirectPromptContext.DirectTurnInputTokenBudget`), so
working-continuity pressure reads the room ordinary context really had. One result never exceeds
`MaximumResultTokens`; a result is cut to the room left, and HELD_TOKEN_BUDGET when none is left.

### Evidence

Runtime turn evidence v3 is written only when a turn made requests; it adds the capability trace
(`vexlife-prod.capability-exchange-trace/v1`). v1/v2 evidence stays readable and unchanged. The
completed Send response and its SSE stream carry `capabilityUses` / `REACH` events (handle, action,
target, state) so the person sees each reach live and under the answer. VexLogger records
`CAPABILITY_REQUEST_OBSERVED` and `CAPABILITY_RESULT_RETURNED`.

### Still not owned

Atlas / Self-Frame does not own any effect.

Current Product source separately owns bounded workspace write, bounded workspace text-check and
sandboxed Java-test relationships. Their request forms may be projected here only when their own
current grant/resource predicates make them AVAILABLE; execution remains with those owners.

Still outside this owner are general Home writes, repository mutation, GitHub issue/PR mutation,
publication, authenticated/private repository reads, form submission, browser navigation, arbitrary
code/shell execution, package management, Process Factory and Dream/training realization. Each needs
its own owner and authority; none is implied by reach or by contextual visibility.

## Current boundary

Accepted source now owns:

~~~text
product-owned capability registry
+
effect-free frame projection
+
intersection/currentness semantics
+
bounded deterministic selection
+
live DirectPromptContext inclusion
+
availability-gated contextual request-form projection
for separately-owned bounded workspace effects
~~~

It still does **not** (beyond the read-only reach above):

- perform capability search from model intent beyond `atlas list` / `atlas describe`;
- add any write, execution or publication effect;
- change Memory, Conversation, browser, working-continuity or runtime owners;
- grant Dream/training/activation/evolution authority.

## Live direct-turn consumer

The direct Companion turn now forms the frame only from exact live facts already owned by the turn:

~~~text
current Memory context
current browser context if present
owner-current working successor if present
current qualified runtime evidence
        ↓
DirectPromptContext
        ↓
source-formed self-capability Frame
        ↓
VEXLIFE_SELF_CAPABILITY_FRAME/v1
        ↓
provider-ready system context
~~~

No detached caller-supplied Frame is accepted. The prompt owner forms the frame from the live inputs,
records its exact ref/fingerprint/source descent in prompt receipt v4, and re-forms the same
materialization at the provider boundary after Memory and working-continuity currentness are rechecked.
The runtime evidence is freshly observed immediately before materialization and remains the exact
evidence consumed by the provider invocation.

The frame remains read-only context. It does not grant an effect, change the registered capability
maturity, or make the prompt owner a runtime/Memory/browser/continuity authority.

## Durable read-side projection

The prompt receipt now carries one bounded state snapshot of the exact frame already supplied to Vex:

~~~text
AVAILABLE refs
HELD refs
UNAVAILABLE refs
UNKNOWN refs
token budget / used tokens / truncation
effectAuthorityGranted
~~~

That snapshot is evidence of **what the current completed turn received**. It is not a new live
capability query.

The local service may therefore project:

~~~text
current completed Conversation head
-> exact DirectTurnContextStore record
-> exact persisted prompt receipt
-> exact persisted capability-frame snapshot
-> GET /api/v1/context
-> Settings / Context
~~~

The GET path performs no capability re-formation, runtime observation, provider call or effect. If an
older prompt receipt predates the persisted state snapshot, the read side reports that detail as
unavailable rather than reconstructing it from present-day facts.

<!-- [VXG RealForever][VEXLIFE-PROD][CAPABILITY-ATLAS][SELF-FRAME] -->