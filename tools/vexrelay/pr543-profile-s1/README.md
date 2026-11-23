# PR #543 DCP-S1 — reusable-profile inhabitance proof

[VXG RealForever]

DO NOT MERGE. This companion is proof carriage for PR #543.

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
