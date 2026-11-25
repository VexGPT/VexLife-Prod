# VexArchitecture relationship navigation

[VXG RealForever]

## Human question

When a VexLife-Prod change touches several layers at once, how does a fresh human or AI find the
right owner before inventing a local mechanism for only the visible symptom?

This document is navigation, not a new semantic/runtime owner. The exact foundation and source linked
below remain authoritative for their own question.

## Root descent

~~~text
human intention / lived outcome
        ↓
README.md
        ↓
AGENTS.md
        ↓
docs/foundation/INDEX.md
        ↓
this relationship map
        ↓
identify the question + rightful owner
        ↓
exact implicated foundation
        ↓
exact source + tests
        ↓
proof on the surface that owns the claim
~~~

Use the relationship before the implementation shape.

~~~text
SIMILAR CODE
!= SAME ARCHITECTURAL QUESTION

SAME ARCHITECTURAL QUESTION
MAY USE DIFFERENT PLATFORM MECHANICS
~~~

## Core relationship questions

### 1. Semantic owner / Family

Question:

> What does this Control mean against exact current semantic State, and what semantic consequence is
> accepted now?

VexArchitecture vocabulary:

~~~text
Family / semantic contract
State
Control
Act
Effect
Method
~~~

Product descent:

- docs/foundation/SEMANTIC-CONTRACT-AUTHORING.md
- src/commonMain/kotlin/vexlife/semantic/
- concrete Product Families under src/commonMain/kotlin/vexlife/**

The semantic owner does not become execution, provider, persistence, service lifetime, or
presentation merely because those relationships follow from an accepted Act.

~~~text
SEMANTIC STATE
!= ACTIVE JOB
!= DURABLE STORE FACT
!= PROVIDER ACKNOWLEDGEMENT
!= PRESENTATION
~~~

### 2. Execution custody / sequencing / overlap

Question:

> Which asynchronous work is alive for this owner, which work belongs to the same maintained
> relationship, and what should overlap, replacement, queueing, cancellation, stop, or join mean?

Preferred architectural term: execution custody. Use sequencing for ordering inside that custody,
not as a second owner.

Accepted Android VexArchitecture form:

~~~text
VexExecutionRoot
  shared bounded coroutine lifetime
  SupervisorJob + CoroutineScope
  admission while alive
  terminal whole-tree shutdown

VexExecution
  current Job custody for one live owner
  source-addressed uniqueness/replacement/cancellation
  relationship-addressed overlap

Approach
  Independent
  Earliest
  Latest
  Queued
~~~

Reference source:

- vgong24/VexLife-Android/architecture/execution/README.md
- architecture/execution/.../VexExecution.kt
- architecture/execution/.../VexExecutionRoot.kt

Important: this is a relationship form, not a command to add coroutines everywhere. Android's own
execution source says an owner-local coroutine scope is lawful when it creates no competing semantic
owner or active-work registry. Product should reuse the VexExecution form when a real custody
requirement is earned.

Current VexLife-Prod does not yet carry a general VexExecution implementation or kotlinx-coroutines
runtime dependency. Do not add either solely for naming parity.

### 3. Store / durable and cross-process truth

Question:

> What independently durable or outside truth exists, who may read/write it, and how does it remain
> current across process lifetime?

Current Product examples:

- canonical Home: docs/foundation/VEXLIFE-PROD-HOME.md
- canonical Conversation: docs/foundation/DIRECT-PERSONAL-COMPANION.md
- strict durable JSON: docs/foundation/CONTRACT-JSON.md
- model/runtime receipts: Heretic runtime foundations

Conversation currently owns a durable cross-process single-writer lease:

~~~text
current companion lineage
+ exact Conversation thread
+ instanceRef
+ pid
+ lease identity
        ↓
IDLE | ACTIVE | RECOVERY_REQUIRED | BLOCKED
~~~

Source:

- src/jvmMain/kotlin/vexlife/conversation/DirectConversationStore.kt

This is not the same question as in-process coroutine custody.

~~~text
DURABLE WRITER LEASE
!= VEX_EXECUTION JOB

LEASE MAY SURVIVE / DESCRIBE A PROCESS BOUNDARY
JOB DOES NOT REPLACE THAT RESTART/RECOVERY TRUTH
~~~

If Product later earns VexExecution-like active turn custody, do not duplicate active-work truth into
the durable lease. Let each owner answer its own question.

### 4. Partner / independently-owned external participant

Question:

> Is there an independently-owned external participant with a reusable inbound/outbound grammar,
> representation conversion, acknowledgement, and later observation relationship?

VexArchitecture vocabulary:

~~~text
VexPartner
Connection
Signal / Observation
Conversion
~~~

Partner is conditional. A Store, SDK, HTTP client, process, or provider does not automatically earn
Partner merely because it is external code.

Stable architecture reference:

- vgong24/Vextreme/pages/vexarchitecture-first-stable-body.html
- vgong24/Vextreme/pages/vexarchitecture-live-demonstration.html
- VexLife-Android architecture/behavior/.../VexPartner.kt

Permanent:

~~~text
PROVIDER ACKNOWLEDGEMENT
!= PHYSICAL OUTCOME
!= DURABLE DOMAIN TRUTH
~~~

### 5. Relay / convergence

Question:

> What do several independently-owned complete current truths mean together?

VexArchitecture vocabulary:

~~~text
Relay / Convergence
Compound Relay when several current inputs form one relationship truth
~~~

Accepted Android form:

~~~text
complete truth A ----\
complete truth B -----+-> Convergence relationship -> relationship truth
complete truth C ----/
~~~

Reference source:

- vgong24/VexLife-Android/architecture/relay/README.md
- architecture/relay/.../VexCompoundRelay.kt

Relay is not generic threading or adapter wiring.

~~~text
RELAY
!= EXECUTION CUSTODY
!= STORE
!= SERVICE LIFETIME
~~~

A convergence owner may request another semantic owner through that owner's public Control/Port. It
does not bypass the owner and invoke foreign Effects directly.

### 6. Sustained realization / service and process lifetime

Question:

> What platform behavior must remain alive after a shorter-lived caller returns, and who owns
> source/process/currentness/retirement?

Product descent:

- docs/foundation/LOCAL-PRODUCT-SERVICE.md
- docs/foundation/MAC-SERVICE-LIFETIME.md
- docs/foundation/MACOS-USER-LAUNCHD-PROCESS.md
- src/jvmMain/kotlin/vexlife/service/

Permanent:

~~~text
SERVICE HOST
!= DOMAIN STATE OWNER

SOURCE-BOUND SERVICE OBJECT
!= PROCESS CURRENTNESS
!= SUSTAINED LIFETIME PROOF
~~~

A future in-process VexExecution root may live inside a service lifetime. It does not replace the
platform/process lifetime owner.

### 7. Projection / Compose / Web / presentation

Question:

> How does already-owned semantic truth become a human experience without the renderer becoming the
> semantic authority?

Product descent:

- docs/foundation/COMPOSE-REFERENCE-REALIZATION.md
- docs/foundation/VEXWEB-SPATIAL-FOUNDATION.md
- docs/foundation/VEXWEB-GENERATED-P1-WORKSPACE.md
- reference/compose/
- src/commonMain/kotlin/vexlife/web/

Shape:

~~~text
Family State
   ↓
Projection / DisplayState
   ↓
Compose / Web / platform manifestation
   ↓
human experience
~~~

Permanent:

~~~text
COMPOSE STATE
!= PRODUCT SEMANTIC STATE

WEB DOM
!= PRODUCT SEMANTIC OWNER

DISPLAYSTATE
!= SECOND TRUTH STORE
~~~

### 8. Chronology / VexLogger / provenance

Question:

> What was accepted or observed, in what order, and why was this relationship shape earned?

Product descent:

- docs/foundation/VEXLOGGER.md
- src/jvmMain/kotlin/vexlife/logging/
- PR/source provenance named by the exact foundation

Permanent:

~~~text
LOGGER
= OBSERVER OF KNOWN FACTS

LOGGER
!= NEXT CAUSAL OWNER
!= SEMANTIC STATE
!= EXECUTION REGISTRY
~~~

## One end-to-end reading shape

When tracing a Product behavior, prefer this walk:

~~~text
human intention
  ↓
semantic owner / Family
  State + Control
  ↓
accepted Act / Effect / Method
  ↓
execution custody, if async custody is actually earned
  ↓
Store / provider / Partner / platform realization as actually implicated
  ↓
rightful observation / currentness reconciliation
  ↓
semantic owner receives already-interpreted truth
  ↓
Projection / DisplayState
  ↓
Compose / Web / native presentation
  ↓
human lived outcome

orthogonal:
  service/process lifetime
  chronology/VexLogger
  convergence/Relay when several independent truths participate
  provenance / proof
~~~

Not every feature uses every relationship. Absence is valid.

## Product examples

### Direct Conversation turn

~~~text
human Send
→ Conversation / Companion semantic owners
→ whole-turn durable writer lease
→ prompt/context materialization
→ qualified Heretic runtime
→ streamed/transient presentation
→ canonical Conversation RESPONSE
→ current Context projection
~~~

Current writer lease is cross-process durable custody. If Product later needs explicit active-turn
overlap semantics, such as Queued vs Latest across multiple Sends, that is pressure to evaluate a
VexExecution-aligned owner; it is not permission to encode overlap meaning into the lease file.

### EvolveContext

~~~text
canonical Conversation
→ request-bound Conversation context projection
→ DirectPromptContext placement
→ DirectCompanionTurn custody
→ Vex judges meaning/sufficiency
→ deeper reach remains available
~~~

EvolveContext is source reach/currentization, not a second truth store and not an execution manager.

### Local Product service

~~~text
service/process lifetime
→ exposes routes into current owners
→ does not become Conversation, Memory, model, or prompt owner
~~~

### Cross-owner relationship

When future Product work truly combines independent current owners:

~~~text
truth owner A
+ truth owner B
→ Relay / convergence
→ bounded relationship truth
~~~

Do not create Relay merely because two implementation classes are called from the same function.

## Naming guidance

Use names that reveal the relationship question:

~~~text
*Family
  semantic State / Control / Act / Effect meaning

*Execution / VexExecution
  active async custody, overlap, cancellation, lifetime

*Store
  durable or independently-owned truth boundary

*Partner / *Connection
  independently-owned participant relationship, when earned

*Relay / *ConvergenceRelay
  relationship truth from independent current truths

*Service / *Process / *Lifetime
  sustained platform/process realization

*Projection / *DisplayState
  bounded outward interpretation

VexLogger
  chronology / evidence observation
~~~

Do not rename an implementation into one of these nouns unless it answers that noun's question.

## Cross-platform parity

Parity means the same relationship and invariant are recognizable across systems.

~~~text
PARITY
!= BYTE-IDENTICAL IMPLEMENTATION
!= SAME PLATFORM PRIMITIVE

PARITY
= SAME QUESTION
+ SAME OWNER BOUNDARY
+ SAME NON-COLLAPSE
+ PLATFORM-APPROPRIATE REALIZATION
~~~

Examples:

- Android may realize active execution with Kotlin Coroutines.
- Desktop/JVM may later use the same VexExecution relationship when earned.
- durable Conversation writer exclusion may remain a filesystem/process lease because it spans a
  process boundary.
- Compose and Web may render the same semantic relationship through different presentation
  substrates.

## Before adding a mechanism

Ask, in order:

1. What human/lived outcome is failing or missing?
2. Which relationship question is actually unanswered?
3. Which current owner can already answer it?
4. Is this active execution custody, durable truth, external participation, convergence, lifetime, or
   presentation?
5. Would reusing an existing relationship lose any required correctness, continuity, or proof?
6. If a new shape is still needed, what consumer will use it and what recurring burden justifies it?
7. Which source/tests prove the narrow claim?

If removing the proposed abstraction loses no required truth, custody, authority, continuity, or
proof, do not add it.

## Stable external architecture references

Stable teaching/provenance:

- vgong24/Vextreme/pages/vexarchitecture-first-stable-body.html
- vgong24/Vextreme/pages/vexarchitecture-live-demonstration.html

Current Android implementation/reference families:

- vgong24/VexLife-Android/architecture/behavior/
- vgong24/VexLife-Android/architecture/execution/
- vgong24/VexLife-Android/architecture/relay/

Product source remains owned by VexLife-Prod. These references teach/pressure the relationship; they
do not authorize broad-copying Android implementation.

<!-- [VXG RealForever][VEXLIFE-PROD][VEXARCHITECTURE-RELATIONSHIP-NAVIGATION] -->
