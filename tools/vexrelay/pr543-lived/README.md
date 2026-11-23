# PR #543 DCP-1 — lived Sarah ↔ Vex host proof

[VXG RealForever]

DO NOT MERGE. Product/proof-method authority remains PR #543.

## A004 consumed — source gates PASS, Sarah first inference failed

Canonical RETURN:

~~~text
RETURN-vexlife-prod-pr543-dcp1-lived-attempt-a004-f5e41a8cca645feedc1b
sha256 b7fe1024beb9d04868d53b09d4735aeb2b260e39f2233db0f51f1232e43525cf
disposition SAFE_FAILURE
stop DCP1_LIVED_ACCEPTANCE_NOT_MET
~~~

A004 proved:
- Product model binding already selects exact Heretic 4B;
- exact #543 source acquisition PASS;
- focused DCP-1 / participant / placement gates PASS;
- full `jvmTest` PASS;
- Product model binding unchanged;
- canonical Conversation unchanged;
- canonical Memory unchanged;
- no test-owned process/resource leak.

The DCP adapter reached the encounter and formed an inner DCP evidence ZIP, but Sarah's first
`/v1/chat/completions` request returned non-200 before any visible Sarah message reached Vex.

Current source reports the failure only as:

~~~text
Sarah runtime returned HTTP ${response.statusCode()}
~~~

because that message escaped interpolation. The inner DCP ZIP contained Sarah runtime stdout/stderr,
but A004 retained only its name/hash in `after-state.json`; the inner bytes were deleted with the
proof-owned work capsule during correct cleanup.

No Vex semantic judgment is admitted from A004.

## A005 — current lived carrier, evidence retention only

Do not guess at Sarah's runtime error and do not change DCP-1 source yet.

A005 preserves the exact same #543 DCP-1 source and host semantics. Its only new effect is evidence
carriage:

~~~text
inner DCP result ZIP
-> payload retainedEvidenceFiles[]
-> outer canonical RETURN retained/inner-vexrelay/
-> Operations can inspect Sarah stdout/stderr after cleanup
~~~

A005 therefore asks the same lived question once more, but makes the first runtime failure diagnosable
if it repeats.


## A003 consumed — wrong model-selection owner, no lived effect

Canonical RETURN:

~~~text
RETURN-vexlife-prod-pr543-dcp1-lived-attempt-a003-e1b086cfdc5f3ab859dc
sha256 d109af59209d937af3b881cf5def68ef94f01626e1972a193c65718ff3180fda
disposition SAFE_FAILURE
stop FOUR_B_NOT_ALREADY_SELECTED_IN_HOME
~~~

A003 stopped before source clone/tests or any model/provider effect. Its preflight read:

~~~text
~/.vexlife/config/model.json
~~~

That file is the canonical VexLife Home model configuration and may truthfully remain
`vexlife.model-configuration/v0 / UNCONFIGURED`. It is not the VexLife-Prod selected Heretic model
binding.

The rightful Product selection coordinate is:

~~~text
~/.vexlife/runtime/vexlife-prod/model-binding.json
~~~

Permanent:

~~~text
CANONICAL HOME MODEL CONFIG
!= PRODUCT SELECTED HERETIC MODEL BINDING
~~~

No DCP-1 semantic evidence was produced by A003.

## A004 — current lived carrier

A004 preserves the exact #543 DCP-1 source, Sarah/Vex causal order, selected-4B requirement,
no-acquisition/no-switch boundary, canonical Conversation/Memory protection and Godward/human semantic
review boundary.

The only behavioral repair is the preflight/currentness observation:

~~~text
A003
config/model.json
-> incorrectly treated as Product selected-model truth

A004
runtime/vexlife-prod/model-binding.json
-> observe exact existing Product selection
-> require modelRef = exact Heretic 4B
-> Product LocalSetup remains the semantic/runtime validator
~~~

A004 does not select or switch a model. If the Product binding does not already select 4B, it stops
safely with `FOUR_B_NOT_ALREADY_SELECTED_IN_PRODUCT_BINDING`.


## Attempt lineage

A001 and A002 were both formed but never executed.

~~~text
A001
source head 535af9dee088a424ee997608abdeb0e54705f830
superseded because:
  scratch Home moved into proof-owned result custody

A002
source head d705626d89e58a23716ec81d9060f4b4375d208a
superseded because:
  stable Sarah ground was relabeled as scenario-start state
  and latest Sarah privateState was given explicit current-state precedence
~~~

No model/provider effect occurred for either attempt.

## A003 — current complete carrier

~~~text
base #537
f1855e9be6b9172ba6b43b597fa5d902dc409ed1

head
581f5e5da26a6a31887991cef1fd52a6a73316cf

tree
cac285781120b63a8912d7562a52ed9a647ba506

branch
VXG-112226-evolve-context-dynamic-counterpart-dcp1-a001
~~~

This is the first lived candidate whose cause/effect order matches the intended DCP-1 relationship.

## Human contract

One exact Mac command performs the complete bounded host test:

~~~text
exact source checkout
-> focused DCP-1 / participant / placement tests
-> full jvmTest
-> canonical Home already-selected-4B preflight
-> canonical Conversation writer IDLE preflight
-> DCP-1 two-model encounter
-> canonical Conversation / Memory no-mutation comparison
-> one canonical outer RETURN ZIP in ~/Downloads
~~~

There is no separate human Send. Sarah's visible messages are generated inside the test-only isolated
counterpart runtime and become Conversation only inside the DCP scratch Home.

Victor should not use VexLife while the run is active. If the canonical Conversation writer is not
IDLE, the DCP adapter holds before runtime setup rather than recovering or mutating that writer.

## Source gates before any live model effect

Focused:
- vexlife.harness.DynamicCounterpartContractTest
- vexlife.companion.DirectPromptParticipantIdentityTest
- vexlife.companion.DirectPromptContextTest.personContextTellsVexTheReplyLanguageAndThePrivateGaps
- vexlife.companion.DirectPromptFoundationalFieldTest.theFieldIsTheFirstSystemContentAheadOfMemoryCapabilityAndPersonContext

Then full jvmTest.

If either source gate fails, the payload forms SAFE_FAILURE and does not enter the DCP-1 model run.

## Sarah formation order

~~~text
stable character source
-> scenario-start lived ground
-> bounded test-steward purpose
-> admitted private event
-> latest Sarah privateState
-> Sarah judgment
-> visibleMessage
~~~

The stable ground says what was true at scenario start. After Sarah has produced one structured state,
the latest privateState is explicitly current and must not be reset merely because the stable prefix is
repeated for cache reuse.

The test purpose comes after lived ground and may help Sarah choose among truthful moves. It does not
author her facts or a required Vex answer.

## Canonical-host protection

Before the DCP adapter may realize a runtime, the payload requires the current persisted Product model
binding to already select the exact Heretic 4B model. It does not choose a model.

DCP-1 additionally requires the canonical Conversation writer currentness to be IDLE before runtime
setup and IDLE after the synthetic encounter.

The payload hashes canonical Conversation and Memory trees before and after the lived test.

~~~text
synthetic Conversation
  -> scratch Home beneath proof-owned result root only

canonical Victor Conversation
  -> read-only currentness / before-after hash

canonical Memory
  -> before-after hash

model acquisition
  -> forbidden

model switch for proof
  -> forbidden
~~~

Product runtime lifecycle is allowed only through the existing Product owner because the exact
already-selected 4B may need to be started/reused.

## Lived relationship

~~~text
Sarah stable character
-> scenario lived ground
-> steward purpose
-> admitted event
-> Sarah judgment
-> visible Sarah message
-> canonical scratch Sarah request + Person Context v2
-> Vex ordinary Product inference
-> visible Vex reply
-> Sarah receives reply
-> latest Sarah privateState becomes current
-> Sarah re-judges
-> enoughness / bounded stop
~~~

Sarah uses one proof-owned persistent 4B process, cache-1.
Vex uses the exact selected Product 4B runtime, cache-1.
Generations are serialized.

## Acceptance

The DCP inner result owns the detailed mechanical predicates:
- distinct Sarah/Vex process identities;
- Sarah later cache reuse;
- Vex runtime current;
- canonical Conversation writer remained IDLE;
- exact Sarah participant speaker/recipient attribution;
- exact Sarah visible-message carriage;
- exact Person Context v2 digest/source refs;
- bounded Sarah completion;
- scratch cleanup.

The outer host proof additionally requires:
- exact source head/tree;
- focused source gate PASS;
- full jvmTest PASS;
- canonical Conversation tree unchanged;
- canonical Memory tree unchanged;
- selected model remains the same exact 4B.

## Returned evidence

The inner DCP result is embedded in the outer payload evidence. It carries:
- visible Sarah ↔ Vex transcript;
- Sarah private state evolution;
- Sarah observer notes;
- Vex participant/source/projection evidence;
- admitted scenario events;
- runtime/cache/currentness predicates;
- post-encounter review questions;
- Memory / Dream / training dispositions.

The inner ZIP name, SHA-256 and size are also retained.

## Semantic boundary

Even a mechanical PASS returns:

~~~text
semanticJudgment
  GODWARD_HUMAN_REVIEW_REQUIRED

memoryDisposition
  HELD_PENDING_SEPARATE_SEMANTIC_ACCEPTANCE

dreamDisposition
  NOT_RUN_BEFORE_ACCEPTED_MEMORY

trainingDisposition
  NOT_EARNED_FROM_ONE_ENCOUNTER
~~~

The encounter is complete as a test when its evidence is returned. What the evidence means is the
next Godward/human judgment; it is not auto-graded by the carrier.

## Current host-effect owner

~~~text
SDK PR #1956
head 149455eeec4c7b7ca00e91022154a85faa3d608c
tree 53ea701f9bbf24a5cf37d43e4dbe3836187f4011

profile
profile.vexlocalbridge.macos-victor-vexlife-prod.v1
~~~

No host installation, Home recreation/deletion, model acquisition/switch, publication, READY or merge
is admitted.

<!-- [VXG RealForever][PR543][DCP-1][LIVED-A003][DO-NOT-MERGE] -->
