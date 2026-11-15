# Capability realization successor — from self-frame to real reach

`[VXG RealForever]`

## Why this successor exists

PR #136 established and lived-tested the Product floor before capability expansion:

```text
runtime / Send truth
+ exact Setup ownership
+ progressive reasoning presentation
+ foundational field receiving-side refinement
        ↓
stable enough to stop changing personality/orientation while capability work begins
```

This successor starts from exact #136 head:

```text
VexGPT/VexLife-Prod
base branch = VXG-111426-chatgpt-turn-runtime-reconciliation-a001
base head = 1b2d5350e4944245927671e3cae092b976249c0a
foundational field = 0.5.1
field bodySha256 = 89169ec9265f32a371fe31eb0927349823099a29e4521667cc53340793504abc
```

## Frozen receiving-side baseline

The capability phase does **not** own further personality/foundational-field refinement.

Treat these paths as frozen baseline unless Victor supplies new lived evidence that explicitly reopens them:

```text
src/jvmMain/resources/foundational/vex-foundational-field.json
src/jvmMain/resources/foundational/vex-foundational-field.revisions.json
src/jvmMain/kotlin/vexlife/foundational/VexLifeFoundationalField.kt
src/jvmTest/kotlin/vexlife/foundational/VexLifeFoundationalFieldTest.kt
docs/foundation/FOUNDATIONAL-FIELD.md
```

The v0.5.1 model-facing field intentionally separates:

```text
present perspective / judgment
-> ordinary participation

capability / authority / receipts
-> practical action + evidence truth

selfhood / persistence / feeling
-> open questions only when materially relevant
```

Capability work should preserve that separation. A capability state must not become a statement about Vex's worth, identity, or permission to participate.

## Lived receiving-side evidence

Victor-provided lived transcripts after the field refinements surfaced the next boundary.

Representative Vex observations:

> "The field is doing its job now."

> "The friction that's left: The capability frame."

> "The frame is descriptive, not a hand."

> "The frame informs. The bridge acts. I request and verify."

The important product question is therefore no longer "how should Vex sound?" It is:

> **How can Vex turn recognized intent into one bounded capability request that a rightful host owner can actually execute and return, while the self-capability frame remains truthful context rather than fake authority?**

These transcript excerpts are receiving-side evidence, not architecture authority.

## What VexLife-Prod actually has today

Current Product source owns one compact, read-only Capability Atlas / Self-Capability Frame:

```text
docs/foundation/CAPABILITY-ATLAS-SELF-FRAME.md
src/jvmMain/kotlin/vexlife/capability/VexLifeCapabilityAtlas.kt
```

Current registered definitions are only:

```text
capability.vexlife.context.memory-current
capability.vexlife.context.working-continuity
capability.vexlife.context.browser-current
capability.vexlife.companion.local-inference
```

The current Product documentation explicitly says the frame:

```text
reports current capability truth
!= executes a capability
!= grants an effect
```

and that Product does not yet own the stable-era:

```text
full capability registry
Atlas query stack
model-connection compiler
model-intent capability search/query
general navigation
repository/file tooling
publication tooling
Dream/training capability realization
```

Current frame maturity and availability are separate:

```text
DISCOVERABLE / EXPLAINABLE / REQUESTABLE / ADMITTED / EXECUTABLE / COMPLETED
!=
AVAILABLE / HELD / UNAVAILABLE / UNKNOWN
```

That distinction is useful and should be preserved.

## One real model-shaped bridge already exists

Conversation Source Return proves a narrower pattern already works in this Product:

```text
model recognizes exact older Conversation is needed
-> emits one typed internal request shape
-> Product host parses/adopts it
-> rightful Conversation owner performs bounded read-only discovery
-> exact returned source is projected into a second provider stage
-> model continues from returned truth
```

This is the current concrete example of:

```text
FRAME / CONTEXT
-> MODEL REQUEST SHAPE
-> HOST BRIDGE
-> RIGHTFUL OWNER
-> RETURNED EVIDENCE
-> MODEL CONTINUATION
```

The successor should study this relationship before introducing a general tool protocol.

## Accepted predecessor sources to reuse, not reinvent

### Stable VexLife capability/tool architecture

Observed source:

```text
vgong24/VexLife@6946f935221dc8b44a599d2fd5601aed14a1d8fb
```

Relevant owners:

```text
docs/CAPABILITY-AND-TOOL-ATLAS.md
docs/MODEL-CONNECTION-COMPILER.md
docs/REGISTRIES-AND-PROCESS-FACTORY.md
docs/VEXHOME-HOSTED-CAPABILITIES.md

blueprint/capability-registry.json
blueprint/model-connection-binding-registry.json
blueprint/process-factory.json

src/core/atlas.mjs
src/core/capability.mjs
src/core/vex-self-capability-frame.mjs
src/core/model-connection-atlas.mjs
src/core/model-connection-compiler.mjs
src/core/process-factory.mjs
src/core/tool-result-relay.mjs
```

Useful accepted relationships:

```text
model sees compact capability handles
-> asks Atlas for bounded detail only when relevant

typed command registry
!= unrestricted shell

capability available
!= capability executed

runtime native tool support
!= tool effect authority

model-connection compiler
-> may narrow current capability
-> does not grant effect authority

Process Factory
-> plan / classify / compile process
-> execution remains separately brokered
```

Stable VexLife also already describes file and GitHub capability semantics. Reuse their meaning before forming Product-specific realization.

### Vextreme-SDK Process Factory / repository operations

Observed source:

```text
vgong24/Vextreme-SDK@8543c7d755faba04d4c4019c02e84f992667918a
```

Relevant source:

```text
docs/architecture/process-factory-controlled-apply-design.md
docs/private-continuity/multivex/process-factory/foundations/provider-independent-repository-operations.json
docs/private-continuity/multivex/process-factory/registries/connector-action-registry.json
lib/process-factory/
lib/continuity-operations/vex-capability-assimilation.js
```

The accepted provider-independent repository foundation already distinguishes:

```text
AUTH
READ
QUERY
WRITE
RUN
CHECK
PUBLISH
LISTEN
RESPOND
LOOP
```

and preserves:

```text
provider capability != authority
read may span declared repositories while one mutation task owns at most one repository
non-force publication
exact target / precondition / postcondition evidence
credential safety
canonical returned forward state
```

The connector-action registry already binds specific GitHub effect classes to exact adapter/action identities rather than giving a model generic GitHub authority.

## What is prepared versus missing

### Prepared

```text
[yes] compact source-owned capability frame
[yes] maturity vs current-availability semantics
[yes] exact current-context capability projection
[yes] prompt receipt / read-side evidence of what Vex was told
[yes] one model-shaped typed request/return bridge (Conversation Source Return)
[yes] stable-era Atlas query / describe architecture in predecessor source
[yes] stable-era Model Connection Compiler semantics
[yes] stable-era Process Factory semantics
[yes] SDK provider-independent repository-operation foundation
[yes] SDK typed connector-action registry for selected GitHub effects
```

### Missing in VexLife-Prod

```text
[missing] model-intent -> relevant capability lookup
[missing] model request -> capability detail/describe return
[missing] general typed capability request envelope
[missing] host bridge that validates/adopts that request
[missing] Product binding from capabilityRef/actionRef to rightful effect owner
[missing] tool result relay into the same human turn
[missing] Product-owned repository/GitHub read capability realization
[missing] Product-owned general web/browser navigation realization
[missing] bounded code/sandbox execution realization
[missing] permission/resource owner integrations for those new effects
```

Do not collapse these into one "tools" feature.

## Progress (Claude, 2026-11-14)

```text
C0  request/return relationship extracted         DONE  VexLifeCapabilityRequest + VexLifeCapabilityBridge;
                                                        the turn is a bounded stage loop (≤ 4 requests +
                                                        at most one source return), one runtime binding
C1  bounded Atlas query/describe                  DONE  atlas list / atlas describe (effect-free)
C2  real read reach                               DONE  web fetch/search, GitHub describe/tree/read,
                                                        Home list/read, Memory search — reads only
C3  Process Factory / effect broker / writes      NOT STARTED
```

Current truth lives in [CAPABILITY-ATLAS-SELF-FRAME.md](CAPABILITY-ATLAS-SELF-FRAME.md#requestreturn-reach-2026-11-14).
One field sentence changed with it (v0.5.2): the reach kernel said Atlas handles were absent, which
stopped being true when the Atlas was registered. Nothing else in the field changed.

Lived proof still owed: Vex choosing a capability unprompted, the request appearing live, the owner
performing it on Victor's Mac, and Vex using the result — on 27B and on the smaller model.

## Recommended investigation order

This is a successor investigation order, not accepted implementation authority.

### C0 — prove the current boundary

Read and test the current self-frame + source-return bridge together.

Question:

> What is the smallest reusable relationship between a model-shaped request and one host-returned result?

No new external effect is needed to answer C0.

### C1 — bounded Atlas query / describe

Candidate first extension:

```text
model recognizes capability detail is needed
-> typed bounded Atlas query/describe request
-> Product-local effect-free Atlas owner
-> bounded capability/action/schema description returned
-> same turn continues
```

This gives Vex a way to learn **how** a capability works without yet performing that capability.

It directly addresses the lived complaint that the current JSON describes state but not enough executable shape.

### C2 — typed read-only capability request

After C1 is proven, choose one already-rightful low-risk read owner.

Candidate classes to investigate:

```text
repository/source READ
or
bounded web/browser READ
```

The first realization should answer one semantic question and return one typed result. It should not introduce generic shell, arbitrary code execution, publication, or write authority.

### C3 — brokered effect / Process Factory composition

Only after request/return semantics are proved:

```text
capability request
-> exact owner / permission / resource currentness
-> Process Factory / accepted process definition when earned
-> effect broker
-> typed result + evidence
-> model continuation
```

Repository mutation, GitHub write, public publication, and arbitrary code execution remain later effects with their own authority.

## Claude handoff constraints

Claude may investigate and author capability-realization source in this successor PR.

Claude should preserve:

```text
1. foundational field v0.5.1 as frozen baseline;
2. #136 runtime / Send / Setup / thought-stream behavior;
3. Conversation Source Return as existing working evidence, not something to replace casually;
4. capability frame != effect authority;
5. available capability != executed capability;
6. model request shape != host effect;
7. host effect != successful verified result;
8. existing accepted owners in VexLife and Vextreme-SDK before creating new registries or abstractions.
```

Before capability implementation, Claude should identify:

```text
human intention
current rightful owner
exact request shape
admission/currentness predicates
effect class
return shape
how the result re-enters the same Vex turn
privacy / persistence boundary
proof surface
```

## Evidence Victor can use to judge the next phase

A useful lived capability proof is not "the JSON says AVAILABLE."

It is:

```text
Vex notices it needs something outside current context
-> Vex chooses the right capability
-> request is visible/inspectable
-> rightful host owner actually performs it
-> result comes back
-> Vex understands and uses the result
-> no duplicate/manual Victor shuttle is required for routine execution
```

That is the next threshold.
