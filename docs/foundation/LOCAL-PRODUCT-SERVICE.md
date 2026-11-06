# VexLife-Prod Local Product Service

[VXG RealForever]

## Human question

Can the fresh isolated VexLife-Prod body expose its already-earned local setup + canonical direct
Conversation/Vex-turn owners through one stable loopback product-service boundary without making
browser/presentation code another Conversation, model, prompt, or runtime owner?

## Current relationship

~~~text
future VexWeb / presentation
        |
        v
VexLifeProdServiceContract
  fixed source-owned wire identity
        |
        v
VexLifeProdLocalService
  local HTTP delivery / projection only
        |
        +-> DirectConversationStore.current()
        |
        +-> DirectCompanionTurn.perform(...)
                 |
                 +-> VexLifeCurrentMemoryReader
                 +-> DirectPromptContext
                 +-> DirectCompanionConnection
                 +-> DirectTurnContextStore
                 +-> canonical Conversation head
~~~

The service is a doorway to the current owners. It does not recreate them.

## Fixed ordinary topology

~~~text
host    127.0.0.1
port    18210
origin  http://127.0.0.1:18210
~~~

Routes are shared common-Kotlin source:

~~~text
GET  /api/v1/source-currentness
GET  /api/v1/conversation
POST /api/v1/conversation/send
~~~

Permanent:

~~~text
NEW SOURCE COMMIT
!= NEW SERVICE TOPOLOGY

SOURCE SHA
= release-variable identity

HOST / PORT / ROUTES
= source-owned ordinary product constants
~~~

The source-formed service start currently remains module-internal. It accepts the exact source SHA +
already-qualified local setup result, while host/port remain fixed source constants.

It is intentionally not yet a standalone product entry because process lifetime/source rotation
still remain held outside this layer.

For model effects, runtime currentness is re-observed by the rightful direct-turn/runtime owners
immediately before a new provider invocation.

A proof-only internal start path may bind port 0 for deterministic tests. That does not redefine the
ordinary product route.

## Shared browser/service contract

src/commonMain/kotlin/vexlife/service/VexLifeProdServiceContract.kt owns:

~~~text
stable route identity
fixed host / port / product/service refs

SourceCurrentness
ConversationProjection
ConversationMessage
SendRequest
SendResponse
PromptContextProjection
ErrorResponse
~~~

Because the contract is in common Kotlin, the later Kotlin/JS renderer can consume the exact same
route and wire vocabulary rather than re-declaring strings/JSON shapes in handwritten JavaScript.

~~~text
WIRE CONTRACT
!= PRODUCT SEMANTIC OWNER

JS COMPILES THE CONTRACT
!= BROWSER GAINS HOST EFFECT AUTHORITY
~~~

## Currentness projection

Source currentness keeps these questions distinct:

~~~text
serviceState
sourceSha
runtimeCurrentness = NOT_REOBSERVED_BY_SERVICE
startupQualifiedRuntimeReceiptRef
currentConversationHeadSha256
~~~

It intentionally does not collapse them into one generic READY value and does not promote
setup-time qualification into current runtime truth.

The route is read-only:

~~~text
effectsPerformed = false
~~~

## Conversation projection

The Conversation GET route reads only canonical current Conversation truth:

~~~text
DirectConversationStore.current()
  -> canonicalEvents only
  -> REQUEST projected as role=human
  -> RESPONSE projected as role=vex
  -> current completed head
  -> abandoned-attempt count
~~~

It does not create Conversation storage on an empty fresh Home.

Off-head failed attempts are counted but are not silently injected into the canonical message stream.

## Send

The wire request carries only:

~~~text
idempotencyKey
content
~~~

It cannot carry:

~~~text
endpoint
model
Home path
runtime port
prompt event selection
Memory
provider messages
~~~

Those truths come from the rightful upstream owners.

The ordinary production service delegates exactly once to the live setup Session:

~~~text
DirectCompanionTurn.perform(session, intent)
~~~

The Session re-observes qualified runtime currentness immediately before a new inference. The
proof-only service constructor may inject a bounded test executor without redefining the ordinary
route.

The existing deterministic idempotencyKey behavior therefore remains authoritative:

~~~text
same exact completed attempt
-> durable result returned
-> no second provider call
~~~

The service does not invent another idempotency layer.

## Prompt-context projection

The Send response projects the already-earned direct prompt receipt:

~~~text
context mode
prior completed head
selected Conversation-event refs
provider message-set SHA-256
input token estimate/budget
whole-history enumeration flag
Memory included / selected
training selection
effectsPerformed
~~~

This is metadata about what Vex received. It does not expose raw hidden prompt text or model
reasoning.

The continuity realignment gate has earned retirement on the exact source/body evidence that
established canonical Conversation + Memory compatibility. This does not make the whole product
READY:

~~~text
Memory source path = CONNECTED IN SOURCE
Memory lived canonical-Home compatibility = PASS
lagging Daily Memory frontier = truthful HELD / no provider Memory content
canonical Conversation writer = IDLE or exact absent-owner recovery before setup readiness
WORKING_CHECKPOINT_SUCCESSOR = HELD
CLEAN_START = HELD
~~~

## HTTP owner boundary

VexLifeProdLocalService owns:

~~~text
numeric-loopback listener
HTTP method/path dispatch
bounded request bytes
strict request JSON
typed response encoding
HTTP status projection
~~~

It does not own:

~~~text
Home
model selection
Heretic process lifetime
Conversation append/currentness
prompt selection
provider invocation semantics
source update policy
Mac process lifetime
VexWeb authoring
presentation
~~~

## Source currentness and process lifetime

The service is source-bound by one exact lowercase Git SHA supplied at construction.

That is necessary but not sufficient for the final Mac lifecycle:

~~~text
SOURCE-BOUND SERVICE OBJECT
!= MAC SERVICE PROCESS CURRENTNESS
!= SUSTAINED LIFETIME PROOF
~~~

Long-lived Send currentness now follows:

~~~text
new Send
-> DirectCompanionTurn
-> LocalSetup.Session current-runtime capability
-> HereticPersistentRuntime.observeCurrentQualified()
-> exact receipt + environment + one matching process + launchd substrate + health
-> provider invocation

exact completed duplicate
-> durable result
-> no runtime-currentness observation
-> no provider invocation
~~~

The later macOS product-lifetime owner must still own/re-prove:

~~~text
exact service source/process binding
start/reuse/retire service currentness
sustained service reachability after launcher completion
later Send succeeds
~~~

Do not make this service class own launchd merely because it listens on a port.

## Long-lived service process entry

The JVM source now also contains one process program:

~~~text
VexLifeProdServiceProcess
~~~

Its admitted arguments are intentionally only:

~~~text
--source-sha <exact lowercase Git SHA>
~~~

It cannot accept:

~~~text
port
endpoint
model
Home path
runtime path
download mode
presentation URL
~~~

Process sequence:

~~~text
exact source identity
-> VexLifeProdLocalSetup.establishSession(allowAcquisition=false)
-> fixed VexLifeProdLocalService.start(...)
-> VEXLIFE_PROD_SERVICE_READY receipt line
-> remain alive until process termination
~~~

The service process therefore cannot silently decide to acquire the model/Python prerequisites.
Missing prerequisites return to the separately-owned human setup path.

Permanent:

~~~text
SERVICE PROCESS
!= HUMAN SETUP

SERVICE PROCESS
!= ARTIFACT ACQUISITION AUTHORITY

SERVICE PROCESS SOURCE
!= PACKAGED MAC EXECUTABLE

SERVICE PROCESS
!= LAUNCHD OWNERSHIP
~~~

The JVM shutdown path closes the in-process listener, but external process/source currentness still
belongs to the later macOS product-lifetime owner.

Current packaging pressure is now explicit:

~~~text
FRESH MAC
cannot be assumed to have Java / Gradle / Kotlin tooling

therefore:
  product distribution must carry the runtime needed to execute this source
~~~

Do not solve that by asking the human to install a development JDK.

## Presentation boundary

This candidate does not hand-author the permanent product UI.

Current VexWeb authority still holds:

~~~text
Frame
Seam
Region
Allocation
Depth
~~~

and currently holds deterministic HTML/CSS/JS generation.

Therefore:

~~~text
LOCAL PRODUCT SERVICE
= stable renderer doorway

LOCAL PRODUCT SERVICE
!= GENERATED VEXWEB PRODUCT
!= HAND-BUILT EXPLORATION PROMOTION
~~~

The next presentation candidate should consume VexLifeProdServiceContract and the accepted
VexWeb/Blueprint identities.

## Source-formed proof

Current tests are formed for:

~~~text
fixed ordinary host / port / routes
narrow Send request
source currentness projection
empty Conversation GET with zero storage formation
HTTP Send -> canonical direct-turn owner
durable Conversation projection after Send
exact duplicate attempt -> zero second provider call
unknown wire field -> rejected before turn effect
new inference held when runtime currentness is not exact
completed duplicate does not require runtime/provider availability
service process accepts only exact source identity
service process establishes with acquisition disabled
~~~

## Held before acceptance

~~~text
PR #25 / #26 / #27 accepted/proven predecessors
exact repository Kotlin 2.4.20 common/JVM/JS proof

real macOS:
  isolated setup
  local product service on fixed 18210
  source-currentness route
  direct Send 1
  direct Send 2
  service remains alive after launcher completion

packaged Mac executable with bundled runtime
VexWeb generated presentation
Mac service process lifecycle
ordinary fresh-user Mac entry
Memory catch-up / Daily frontier may remain HELD without blocking service start
WORKING_CHECKPOINT_SUCCESSOR
CLEAN_START
streamed response progress
explicit-open reasoning projection
~~~

## VexBoard projectability

The service boundary deliberately keeps stable source coordinates for:

~~~text
service
routes
Conversation address
direct turn owner
provider Connection
runtime receipt
Conversation head
~~~

A later read-only VexBoard projector should be able to show:

~~~text
human Send
-> service route
-> DirectCompanionTurn
-> DirectPromptContext
-> DirectCompanionConnection
-> Heretic provider truth
-> response/context/head
-> human Conversation projection
~~~

without making the service or VexBoard a semantic owner.

<!-- [VXG RealForever][VEXLIFE-PROD][LOCAL-PRODUCT-SERVICE] -->
