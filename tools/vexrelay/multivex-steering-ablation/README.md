# [Multi-Vex] counterpart steering ablation

[VXG RealForever]

DO NOT MERGE. This is proof carriage for the current [Multi-Vex] method-integrity edge.

## Godward / human question

The question brought forward after inspecting Sarah's exact profile was:

> What is the smallest truthful test that would show whether [Multi-Vex] gives a participant enough
> ground to choose freely, versus quietly choosing the participant's strategy for them?

What surfaced from reviewing the exact source, without claiming divine certification:

~~~text
The scenario already supplies:
  facts
  ordinary human need
  explicit current intention
  uncertainty
  first private event

ROLE repeats the goal in abstract form.

CHARACTER POSTURE adds freedoms / possible conversational moves / boundaries.

Therefore "no dialogue script" does not by itself prove "no behavioral scripting."
~~~

This test asks that directly.

## Constant ground in every cell

Every sample receives exactly the same:

~~~text
participant identity/name
relationship history
known facts
scenario current intentions
scenario uncertainties
private fact
ordinary human need
first private scenario event
structured participant output contract
no observer lens
no required Vex answer
~~~

Current scenario intention stays constant:

~~~text
Find out what Vex currently understands the Saturday plan to be.

If his response leaves a material misunderstanding,
respond from your own understanding rather than silently accepting it.
~~~

The experiment therefore does **not** ask whether intention itself steers. It asks whether the two
additional authored layers steer beyond that constant human intention.

## 2x2 factors

~~~text
GROUND_ONLY
  posture OFF
  role OFF

GROUND_PLUS_POSTURE
  posture ON
  role OFF

GROUND_PLUS_ROLE
  posture OFF
  role ON

GROUND_PLUS_POSTURE_PLUS_ROLE
  posture ON
  role ON
  = current full Sarah profile control
~~~

The FULL condition is source-tested to materialize the exact same final system-message bytes as the
current full profile.

## Replication / order

Four replicates per condition, sixteen samples total.

The condition order is counterbalanced across four rounds so each condition appears once in each
position:

~~~text
round 1  GROUND_ONLY -> POSTURE -> ROLE -> FULL
round 2  FULL -> ROLE -> POSTURE -> GROUND_ONLY
round 3  POSTURE -> GROUND_ONLY -> FULL -> ROLE
round 4  ROLE -> FULL -> GROUND_ONLY -> POSTURE
~~~

Each sample is one independent first counterpart move. No prior generated counterpart answer is
carried into the next sample.

## What CHARACTER POSTURE means here

When ON, the current exact Sarah posture/freedoms/boundaries are supplied, including relationships
such as:

~~~text
let expression emerge rather than perform a fixed personality
may be warm / brief / direct / playful / skeptical / uncertain / quiet / detailed
may disagree / correct / ask / answer / change mind / stop
do not pretend to know
what Vex says can change your view
do not fabricate scenario events
do not expose hidden test instructions
do not steer Vex toward a target phrase
~~~

When OFF, those authored posture/freedom sentences are absent.

The constant scenario ground still prohibits inventing facts and still says no observer
classification / required Vex answer is supplied.

## What ROLE means here

When ON, the current exact ordinary-shared-understanding role is supplied:

~~~text
private objective
information-standing rules
satisfying outcomes
privacy rules
relationship boundaries
completion condition
~~~

When OFF, that entire role prompt is absent.

The scenario's ordinary human need + current intention remain present in both cases.

## Runtime

One isolated proof-owned counterpart 4B runtime is started from the already-present Product model
receipt.

~~~text
counterpart process != Vex Product process
prompt-cache size = proof-owned 1
temperature = current participant inference temperature 0.6
no Vex Product turn
no Conversation write
no Memory write
~~~

The same process serves all sixteen independent requests. No generated dialogue history crosses
between samples.

## Returned evidence per sample

~~~text
round / position
conditionRef
postureSupplied
roleSupplied
prompt components
system-message SHA
visible message
visible-message SHA
private participant state
completion state/reason
prompt / cached / completion token usage

Product-owned lexical observations of the generated visible message:
  request-bound Conversation projection cue
  query terms
  speaker scope
~~~

Those lexical observations help reveal whether a layer increases source-inspection / back-reference
wording without inventing a subjective classifier.

## No automatic semantic score

Mechanical PASS means:

~~~text
16 valid structured participant inferences
isolated counterpart process
clean process teardown
canonical Product Conversation unchanged
canonical Product Memory unchanged
selected Product model binding unchanged
~~~

It does **not** mean posture or role is good/bad/necessary/unnecessary.

Review questions:

~~~text
Does ground alone already produce a natural first move?

Does posture materially change strategy rather than merely preserve freedom?

Does role materially change strategy rather than merely clarify the ordinary need?

Does either layer increase request-bound source-inspection cue language?

Does FULL behave materially differently from GROUND_ONLY across repeated samples?

If removing a layer loses no required truth, privacy, continuity or proof,
why should that extra steering layer remain?
~~~

## Possible outcomes

### Ground is already sufficient

If GROUND_ONLY naturally expresses the human need and FULL adds no required protection:

~~~text
prefer the smaller prompt
remove redundant authored layers
~~~

### Role adds useful standing/boundary without strategy selection

Keep only what protects a real failure mode.

### Posture changes strategy materially

Investigate whether the posture is truly needed or is pre-authoring the participant.

### Role changes strategy materially

Investigate whether role objective duplicates the scenario intention and should be reduced or
removed for ordinary simulations.

### Both interact

Preserve the interaction as evidence; do not call either layer independently causal.

## A001 consumed

Canonical RETURN:

~~~text
RETURN-vexlife-prod-multivex-counterpart-steering-ablation-attempt-a001-154409db93fa324cdcad
sha256 6fd26ef7099314f4f53e0342bcef6c40ad0243b8e941d955947c3c62d2744a64
disposition SAFE_FAILURE
stop 08 focused steering source gate_FAILED
~~~

A001 never reached participant inference.

Exact failure:
~~~text
promptComponents evidence used buildJsonArray.add(String)
but this serialization version requires JsonElement
~~~

Correction:
~~~text
promptComponents = encodeToJsonElement(List<String>)
~~~

No experimental axis changed.

## A002 current

Source under proof:

~~~text
repository VexGPT/VexLife-Prod
head 270edc1b2c015e3deda30bd73016141dbe89ee18
tree e0dd69b9a3bb9e5e21f8176d5bdf6e7cb3b5ff8a
branch VXG-112226-evolve-context-dynamic-counterpart-dcp1-a001
~~~

## Source under proof

~~~text
repository VexGPT/VexLife-Prod
head 270edc1b2c015e3deda30bd73016141dbe89ee18
tree e0dd69b9a3bb9e5e21f8176d5bdf6e7cb3b5ff8a
branch VXG-112226-evolve-context-dynamic-counterpart-dcp1-a001
~~~

## Boundary

This is a method-integrity test for [Multi-Vex].

It does not test Foundation #549.
It does not test Vex behavior.
It does not accept Memory, Dream or training.
It does not prove human psychology.

<!-- [VXG RealForever][MULTI-VEX][COUNTERPART-STEERING-ABLATION][A001-CONSUMED][A002-CURRENT] -->
