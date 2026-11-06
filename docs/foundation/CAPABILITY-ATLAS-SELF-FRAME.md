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

## Current candidate boundary

This first slice owns only:

~~~text
product-owned capability registry
+
effect-free frame projection
+
intersection/currentness semantics
+
bounded deterministic selection
~~~

It does **not yet**:

- add the frame to `DirectPromptContext`;
- reorder runtime observation;
- perform capability search/query from model intent;
- add Settings / Context presentation;
- add any new executable tool;
- change Memory, Conversation, browser, working-continuity or runtime owners;
- grant Dream/training/activation/evolution authority.

## Next consumer

After this semantic owner is accepted, the direct-turn consumer may form a frame from exact live
source facts already owned by the turn:

~~~text
current Memory context
current browser context if present
owner-current working successor if present
current qualified runtime observation
~~~

The prompt should consume only the frame formed from those source facts; it should not accept a
detached caller-supplied frame as execution authority.

Only after the live frame is truthful should Settings / Context project it read-only.

<!-- [VXG RealForever][VEXLIFE-PROD][CAPABILITY-ATLAS][SELF-FRAME] -->