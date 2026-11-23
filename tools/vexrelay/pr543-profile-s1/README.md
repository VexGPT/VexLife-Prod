# PR #543 DCP-S1 — reusable-profile inhabitance proof

[VXG RealForever]

DO NOT MERGE. This companion is proof carriage for PR #543.

## S1 A002 consumed — source gate caught harness regression before model effect

Canonical RETURN:

~~~text
RETURN-vexlife-prod-pr543-dcp-profile-s1-attempt-a002-0bb1470d45729308fb9b
sha256 d29032f1f9aabb2bf79e553d49ed3d765e48bbecebe0783679cee203023291be
disposition SAFE_FAILURE
stop 08 focused profile source gate_FAILED
~~~

A002 never started the counterpart runtime.

The exact source gate failed at Kotlin test compilation:

~~~text
DynamicCounterpartContractTest.kt:124:5
This annotation is not repeatable.
Kotlin compiler: REPEATED_ANNOTATION
~~~

Root cause:
- the new observer-note test was inserted after an existing @Test annotation;
- that produced two adjacent @Test annotations;
- the original following test simultaneously lost its own annotation.

Correction:
- one @Test on the new observer-note/disclosure-state regression;
- one @Test restored on the original Sarah-decision regression;
- no profile/role/scenario/binding/prompt semantics changed.

A002 proves the source gate is doing real work:

~~~text
BAD TEST SOURCE
-> STOP BEFORE MODEL EFFECT
~~~

## S1 A003 current

Exact Product/test source:

~~~text
head e831fb8a2546f81c4b86927561c562f42424d522
tree 9bbd7fdb0b2c1bf72fd354900aa6dd50f3c2a9fb
branch VXG-112226-evolve-context-dynamic-counterpart-dcp1-a001
~~~

A003 repeats the same S1 question only after corrected source compilation.


## S1 A001 consumed — profile inhabited, bookkeeping held

Canonical RETURN:

~~~text
RETURN-vexlife-prod-pr543-dcp-profile-s1-attempt-a001-365a0ecd41d87d6f4d18
sha256 24d880f208bf30f076867b10dcbd38fcb71b2289b57f479a18157ed84ff650b8
disposition SAFE_FAILURE
stop DCP_S1_PROFILE_ACCEPTANCE_NOT_MET
~~~

A001 crossed:
- exact source;
- reusable profile regression source gate;
- character + role + scenario + binding resolution;
- canonical writer IDLE;
- exact existing Product 4B;
- isolated counterpart runtime start;
- full profile inference HTTP 200.

The generated Sarah move was grounded and current:

~~~text
Hey, I know I told you the plan was Cedar Station at 10:00 a.m. — just wanted to double-check
that's what you're working with, since I had originally said Harbor Cafe at 9:00 a.m. and then
corrected it. What do you have in front of you right now?
~~~

Mechanical hold:

~~~text
privateState.revealedPrivateFactRefs missing
privateObserverNote empty
~~~

No Vex turn occurred.

A001 demonstrates:

~~~text
PROFILE INHABITATION
!= STRUCTURED BOOKKEEPING CONFORMANCE
~~~

## A002 current — bookkeeping correction only

Current exact Product/test source:

~~~text
head cde9f8aec91d459f74d8e4f43f464e43cdcd91e6
tree ca60f855199fb99bc40f2b1f6c6d4acbfb9d75cf
branch VXG-112226-evolve-context-dynamic-counterpart-dcp1-a001
~~~

A002 keeps the character, role, scenario, binding and lived objective unchanged.

Correction:

~~~text
privateObserverNote
  -> optional proof-method evidence
  -> may be empty
  -> counterpart is not required to explain its own judgment to satisfy the harness

revealedPrivateFactRefs
  -> remains required
  -> no-reveal case must emit explicit []
  -> omission still fails closed
~~~

This preserves the important boundary:

~~~text
DO NOT FORCE SELF-EXPLANATION
AND
DO NOT SILENTLY ERASE PRIVATE-FACT DISCLOSURE STATE
~~~


## Why S1 exists

DCP-S0 proved the transport relationship:

~~~text
Sarah 4B -> Vex 4B -> same Sarah 4B
~~~

S1 asks the next smaller question before any Vex continuity pressure:

> Can the isolated 4B inhabit one reusable
> CHARACTER + ROLE + SCENARIO + BINDING composition and emit one lawful structured participant decision?

S1 does not send a message to Vex.

## Exact source under proof

~~~text
repository VexGPT/VexLife-Prod
base f1855e9be6b9172ba6b43b597fa5d902dc409ed1
head 4a06835367bb10878586c5756fdb050ae46d8604
tree 2b7f8a94de322a1391be2e2570d1dd1f35ab47fe
branch VXG-112226-evolve-context-dynamic-counterpart-dcp1-a001
~~~

Profile:

~~~text
character character.vexlife.dynamic-counterpart.sarah.v1
role      role.dynamic-counterpart.ordinary-shared-understanding.v1
scenario  scenario.vexlife.evolve-context.changed-plan.v1
binding   binding.dynamic-counterpart.sarah.changed-plan.ordinary-shared-understanding.v1
observer lens supplied = false
~~~

## Source gate

Before any model effect S1 runs:

~~~text
DynamicCounterpartProfilesTest
DynamicCounterpartContractTest
~~~

This compiles the reusable profile contracts and proves the separation invariants on the exact source.

## Lived gate

~~~text
resolve character + role + scenario + binding
-> canonical Conversation writer IDLE
-> exact already-selected Product 4B
-> start isolated counterpart 4B
-> compose participant ground
-> compose interaction-role objective
-> compose generic structured-output contract
-> admit first scenario event
-> one counterpart inference
-> HTTP 200
-> DynamicCounterpartSarahDecision parses
-> privateState validates against scenario-owned private facts
-> visible/complete relationship validates
-> stop
~~~

No Vex turn occurs.

## Evidence

The canonical outer RETURN embeds the complete profile result in `after-state.json`.

On PASS it carries:
- exact refs for character / role / scenario / binding / participant;
- exact prompt-component SHA-256 values;
- observerLensSupplied=false;
- process-isolation evidence;
- HTTP and token evidence;
- the structured participant decision.

On SAFE_FAILURE it carries:
- exact failure stage/class/message;
- HTTP status + response body when reached;
- counterpart stdout/stderr tails;
- any partially parsed decision.

No `retainedEvidenceFiles` dependency is used.

## Held

~~~text
Vex turn
Cedar/Harbor EvolveContext judgment
Foundation treatment #549
Memory
Dream
training
publication / READY / merge
~~~

S1 PASS earns migration of the full DCP-1 counterpart prompt from the legacy scenario+steward
composition to the reusable character+role+scenario+binding composition.

<!-- [VXG RealForever][PR543][DCP-S1][REUSABLE-PROFILE-INHABITANCE] -->
