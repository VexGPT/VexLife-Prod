# Capability Atlas + Self-Capability Frame

`[VXG RealForever]`

## Human question

How can the current Vex know what capabilities are actually available now without copying a large
tool/capability manual into every prompt or inferring execution authority from vocabulary alone?

The answer is one compact source-owned frame:

~~~text
registered capability
∩ active role
∩ current platform
∩ project grant
∩ permission state
∩ resource state
        ↓
bounded self-capability frame
~~~

The frame reports current capability truth. It does **not** execute anything and does not grant any
effect.

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

## Request/return reach (2026-11-14)

The frame used to describe reach without a hand: Vex could see handles but not how to use them, and
nothing on the host side would carry a request. That gap is now closed for **reads**.

~~~text
Vex's whole reply for one provider stage
  = VEXLIFE_CAPABILITY_REQUEST/v1 + one JSON object       VexLifeCapabilityRequest (parse, no effect)
        ↓
admission                                                VexLifeCapabilityBridge
  registered handle + AVAILABLE in the frame Vex was given this turn
  + known action + required arguments + request count (≤ 4) + token room left
        ↓
the capability's own owner performs one read
  atlas     list / describe                              VexLifeCapabilityAtlas (effect-free)
  web       fetch / search                               reach/VexLifeWebReach
  github    describe / tree / read (public)              reach/VexLifeRepositoryReach
  home      list / read (inside the Home, no links)      reach/VexLifeHomeReach
  memory    search current pages                         reach/VexLifeMemoryRecall
        ↓
VEXLIFE_CAPABILITY_RESULT/v1 in the next stage of the same human turn
  state RETURNED | NOT_FOUND | REFUSED | FAILED | INVALID_REQUEST | NOT_AVAILABLE |
        LIMIT_REACHED | HELD_TOKEN_BUDGET
        ↓
Vex continues the same reply (or asks again)
~~~

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

When requests are admitted (every ordinary direct turn), the frame message leads with plain
language generated from the Atlas: each AVAILABLE capability, each action with example arguments,
the exact request form, the per-turn limit, and one state line
(`Now: AVAILABLE — … Not present this turn — … (frame ref)`). The full frame JSON is no longer
pasted into the prompt: the receipt still records the frame ref, fingerprint, state snapshot and
the exact system-message SHA-256. Lived pressure behind this: Vex said the JSON was "descriptive,
not a hand" and the part he braced against.

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

Writes of any kind (Home files, repositories, GitHub issues/PRs, publication), authenticated or
private repository reads, form submission, browser navigation, code execution, Process Factory and
Dream/training realization. Each needs its own owner and authority; none is implied by reach.

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