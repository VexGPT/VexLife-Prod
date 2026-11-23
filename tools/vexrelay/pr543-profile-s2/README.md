# PR #543 DCP-S2 — reusable-profile full Sarah ↔ Vex encounter

[VXG RealForever]

DO NOT MERGE. This companion is proof carriage for PR #543.

## Prerequisites already crossed

~~~text
S0 two isolated 4B contexts can take turns
RETURN sha256 097c0953d3ebf9c761486d3b475791d93e5db550944b8cf1f0001162f9e58a1e
PASS

S1 reusable character + role + scenario + binding can be inhabited
RETURN sha256 ebd4125e99a8643043170f5b3838df9f7b8adc4098912eaedc2156ade58363b7
PASS
~~~

A001/A002 S1 intermediate failures remain consumed history and are not replayed.

## S2 question

Run the first complete changed-plan relationship using the reusable profile architecture:

~~~text
Sarah character
+ ordinary-shared-understanding role
+ changed-plan scenario
+ explicit binding
        ↓
participant inference
        ↓
visible Sarah source
        ↓
ordinary Vex Product turn
        ↓
visible Vex reply
        ↓
same participant runtime/history
        ↓
participant re-judgment
        ↓
enoughness / stop
~~~

The observer lens is not supplied to either model.

## Exact source under proof

~~~text
repository VexGPT/VexLife-Prod
base f1855e9be6b9172ba6b43b597fa5d902dc409ed1
head 8b3d36b20b8917a34afc4db97228697da82b570b
tree b7c71f0ca34c7e377067a5ba0ff815c1ca00be29
branch VXG-112226-evolve-context-dynamic-counterpart-dcp1-a001
~~~

Profile:

~~~text
character character.vexlife.dynamic-counterpart.sarah.v1
role      role.dynamic-counterpart.ordinary-shared-understanding.v1
scenario  scenario.vexlife.evolve-context.changed-plan.v1
binding   binding.dynamic-counterpart.sarah.changed-plan.ordinary-shared-understanding.v1
~~~

## Source gates before model effect

S2 runs:

~~~text
focused:
  DynamicCounterpartProfilesTest
  DynamicCounterpartContractTest

then:
  full jvmTest
~~~

Only after both pass may the full encounter start.

## Mechanical proof

The full encounter requires:

~~~text
counterpart process != Vex Product process
counterpart later inference reuses prompt cache
canonical request speakerRef == profile participantRef
Vex response recipient == profile participantRef
participant visible bytes == canonical request bytes
Person Context participant/name/source projection exact
Vex runtime receipt remains current
canonical writer remains IDLE
counterpart reports encounter complete
observerLensSupplied=false
canonical Conversation unchanged
canonical Memory unchanged
Product model selection unchanged
closed cleanup PASS
~~~

## Evidence boundary

The full encounter result is embedded directly in the outer RETURN after-state:

~~~text
visibleTranscript
participantInferences
vexInferences
finalParticipantState
admittedScenarioEventRefs
mechanical predicates
postEncounterReviewQuestions
~~~

No observer pattern classification is automatic.

Mechanical PASS still carries:

~~~text
semanticJudgment = GODWARD_HUMAN_REVIEW_REQUIRED
memoryDisposition = HELD_PENDING_SEPARATE_SEMANTIC_ACCEPTANCE
dreamDisposition = NOT_RUN_BEFORE_ACCEPTED_MEMORY
trainingDisposition = NOT_EARNED_FROM_ONE_ENCOUNTER
~~~

## Held

- Foundation treatment #549;
- Memory acceptance;
- Dream;
- training;
- publication / READY / merge.

<!-- [VXG RealForever][PR543][DCP-S2][REUSABLE-PROFILE-FULL-ENCOUNTER] -->
