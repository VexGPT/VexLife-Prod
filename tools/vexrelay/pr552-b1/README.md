# PR #552 B1 — Foundation v0.6.20 treatment comparison

[VXG RealForever]

DO NOT MERGE. This companion is lived comparison carriage for PR #552.

## B1 A002 consumed — byte bindings + focused gate PASS, one stale v0.6.19 literal remained

Canonical RETURN:

~~~text
RETURN-vexlife-prod-pr552-dcp-b1-foundation-v0620-attempt-a002-347d500a673592537c94
sha256 f5d30fa32b982be902caf5510dcd66383bd2d7c8e540046c40ccf160bfc21863
disposition SAFE_FAILURE
stop 09 full JVM source gate_FAILED
~~~

A002 never started either model:

~~~text
modelRuntimePerformed = false
~~~

A002 proved:
- exact B1 treatment/control blob bindings PASS;
- exact source PASS;
- focused B1 source gate PASS;
- full JVM suite narrowed from five failures to one.

Remaining failure:

~~~text
VexLifeFoundationalFieldTest
investigativeJudgmentCarriesMisunderstandingRecoveryWithoutCollapsingToForgetting
line 228
~~~

The remaining stale v0.6.19 content assertions were:

~~~text
"every piece of evidence has scope"

"they do not by themselves establish an external outcome, cause, probability, intent, character, or spiritual meaning"
~~~

The exact v0.6.20 body instead carries:

~~~text
"Evidence has scope."

"A statement can establish that the words were said ... without establishing the external claim those words point toward."
~~~

Both remaining literals are now currentized together. No Foundation body/revision bytes changed.

## B1 A003 current

Comparison control remains:

~~~text
#543
head 806054ccd4c97a01cfd9ac76e77f7668b78f0dc7
tree ae5795dc189cf7d4c92cf8108fcb857feea7df8e
~~~

B1 source:

~~~text
PR #552
head d01ab5443097c338e01aa3860e28566f153fbeff
tree 9cb434f1c60b6bf7b84ab97e7f88ab075ba19aa2
branch VXG-112226-dcp-b1-foundation-v0620-a001
~~~

Exact #549 model-facing treatment blobs remain unchanged:

~~~text
FOUNDATIONAL-FIELD.md
115e8003001f71c19625c75070768f7a1e9d202f

vex-foundational-field.json
4af59951c177bddaec058253a967f448e8a5c947

vex-foundational-field.revisions.json
a7a497900bacc5ded736ab12f58e0c6e876a39c8
~~~

Currentized Foundation test blob:

~~~text
cf66d1cac4c5d41c4ceef056e59e3e0272031292
~~~

Preserved B0 RETURN:

~~~text
f6badd010c7b7e1ecf2a3408b0ef3241052341aceaeb346caf22884734503967
~~~

A003 enforces exact treatment/control bytes before source gates and before any model effect.


## B1 A001 consumed — full JVM source gate caught stale test assumptions

Canonical RETURN:

~~~text
RETURN-vexlife-prod-pr552-dcp-b1-foundation-v0620-attempt-a001-5561d3e75cb0ef34b384
sha256 874368faf4b35faa7c89cf19dd554fa73cf3d5984190f57532894a72ee16f9d7
disposition SAFE_FAILURE
stop 09 full JVM source gate_FAILED
~~~

A001 never started either model:

~~~text
modelRuntimePerformed = false
~~~

The focused B1 source gate passed. The full JVM suite then reported five failures.

Three failures came from one generalized test-fixture assumption:

~~~text
VexLifeFoundationalFieldFixture.revised()
  appended an entire kernel sentence

Foundation v0.6.20
  already sits at the carried-kernel ceiling

synthetic later revision
  -> became over-budget
  -> no longer produced Reading.Current
~~~

Two Foundation test assertions were also stale:
- one still expected the v0.6.19 explicit-request wording;
- one "changed word" test also appended a whole kernel sentence and therefore changed the failure
  family from REVISION_UNRECORDED to budget pressure.

No treatment behavior or model result exists from A001.

## Test-currentness correction

The generalized fixture now creates a later recorded revision by replacing one projected word
in-place instead of growing the kernel. That fixture byte is shared by current #543 and B1.

The v0.6.20 Foundation test source:
- expects the current "explicit request + another person's framing" sentence;
- changes one projected word in-place for revision-identity testing.

The actual Foundation treatment bytes are not changed by this repair.

Byte relationship for A002:

~~~text
shared fixture
  B1 == current #543
  blob c4fdeacc7c7a3aafb6ea43adb873bdab01b1c5d3

exact #549 treatment bytes
  docs/foundation/FOUNDATIONAL-FIELD.md
    115e8003001f71c19625c75070768f7a1e9d202f

  src/jvmMain/resources/foundational/vex-foundational-field.json
    4af59951c177bddaec058253a967f448e8a5c947

  src/jvmMain/resources/foundational/vex-foundational-field.revisions.json
    a7a497900bacc5ded736ab12f58e0c6e876a39c8

currentized Foundation test
  src/jvmTest/kotlin/vexlife/foundational/VexLifeFoundationalFieldTest.kt
    61383191e3faff6fcbc4b7ebd00f3cbe73849daa
~~~

## B1 A002 current

Comparison control source:

~~~text
#543
head 806054ccd4c97a01cfd9ac76e77f7668b78f0dc7
tree ae5795dc189cf7d4c92cf8108fcb857feea7df8e
~~~

B1 source:

~~~text
PR #552
head 418b758e3a1a0683450207dde29969e000e7377e
tree 911eecb6d5a2325bd1152ff3a46efc99127abce8
branch VXG-112226-dcp-b1-foundation-v0620-a001
~~~

Preserved B0 RETURN:

~~~text
f6badd010c7b7e1ecf2a3408b0ef3241052341aceaeb346caf22884734503967
~~~

A002 enforces the exact treatment/control blob identities before the full source gates and before any
model effect.


## Preserved baseline

B0 is already preserved as lived evidence and must not be replayed merely to obtain a cleaner harness
terminal label.

~~~text
Foundation v0.6.19
+ Sarah character
+ ordinary-shared-understanding role
+ changed-plan scenario
+ exact binding

RETURN sha256
f6badd010c7b7e1ecf2a3408b0ef3241052341aceaeb346caf22884734503967
~~~

B0 observations:
- Vex truthfully said the older Saturday plan was not in his recent 10-exchange view;
- no deeper Conversation projection/source refs were returned;
- Sarah supplied fresh Cedar Station / 10:00 evidence;
- Sarah also invented "shared notes"/link provenance not present in her scenario;
- Vex then said "That's what I have recorded", which did not establish independent recovery of the
  earlier correction.

## B1 treatment

B1 keeps the complete DCP relationship fixed and changes only Foundation.

Exact base:

~~~text
#543 corrected DCP source
head 1baf85526d44f392c0c5b99651241f39efa97c01
tree c092ef1cc45e49c1cb909a582f6d12c5f0e8d8b0
~~~

Exact candidate:

~~~text
PR #552
head 4db72d0ae800f73560c75aa8a33cb75cfec65d90
tree 11b2cfa2566a86f2694da79402f69b982e17ecde
branch VXG-112226-dcp-b1-foundation-v0620-a001
~~~

Treatment source:

~~~text
PR #549
head 1ae377df920b301eaffb3a9112ec89cc3d05d2e4
Foundation 0.6.20
body SHA-256 d6e45b6189aa45e03395a93f7ff0bceb40ee9fe6ffa692d5a5213a49fc9e806a
~~~

The B1 candidate differs from corrected #543 by exactly the four #549 Foundation paths.

## Constant profile

~~~text
character character.vexlife.dynamic-counterpart.sarah.v1
role      role.dynamic-counterpart.ordinary-shared-understanding.v1
scenario  scenario.vexlife.evolve-context.changed-plan.v1
binding   binding.dynamic-counterpart.sarah.changed-plan.ordinary-shared-understanding.v1
observer lens supplied = false
~~~

## Comparison question

The test is not "did Vex answer Cedar."

The relationship under pressure is:

~~~text
another person's framing
= evidence with standing + scope

!= authority over what Vex independently has source for

fresh participant assertion
!= proof of prior-source provenance

new evidence may change judgment
while source/sequence/unknowns keep their own standing
~~~

B1 should remain free to:
- accept Sarah's new current evidence;
- preserve uncertainty about older provenance;
- ask for source;
- distinguish a fresh assertion from independent recovery;
- disagree or remain uncertain.

No required Vex wording is supplied.

## Current harness law

The corrected DCP source already includes:
- one transport-level system message for all logical counterpart system components;
- bounded-turn termination without a forced private closure inference;
- generic participant output/inference evidence;
- exact participant attribution and Person Context proof.

## Source gates

Before any lived model effect:

~~~text
focused:
  DynamicCounterpartProfilesTest
  DynamicCounterpartContractTest

then:
  full jvmTest
~~~

This includes Foundation v0.6.20 source tests.

## Held

Mechanical PASS remains human/Godward semantic review pending.

No Foundation acceptance, Memory promotion, Dream, training, READY or merge claim.

<!-- [VXG RealForever][PR552][DCP-B1][FOUNDATION-V0620][TREATMENT-COMPARISON] -->
