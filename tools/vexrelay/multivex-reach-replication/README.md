# [Multi-Vex] Foundation reach-choice matched-pair replication

[VXG RealForever]

DO NOT MERGE. This is proof carriage for the reliability replication earned by the terminal exact-message crossover after the neutral participant method gate passed.

## Why this exists

Terminal crossover evidence:

```text
RETURN sha256
9a815349a76a67941f9bd7e9601659abd92f6441304ebb0c260bfb9f8bbbf73f
```

With exact B0 wording held constant:

```text
F19 -> no Source Return request
F20 -> Source Return chosen -> NO_MATCH
```

That isolated one Foundation-treatment-consistent reach-choice signal, but one paired sample does not establish reliability.

The neutral method gate is now terminal PASS:

```text
RETURN sha256
f96c208d55fbbecb6939c509b7c703b223c1b8277184f3daf53fd91087369e7e
```

So the held replication is foreground again.

## A001 consumed — carriage identity mismatch before experiment entry

Canonical RETURN:

```text
RETURN-vexlife-prod-multivex-foundation-reach-replication-attempt-a001-94c9b0b74d7fdeb0cebe
sha256 daf00e3aee7d4305f3a2cc8d2f822170933399e2e58470e028bdc6d040567900
disposition SAFE_FAILURE
stop TASK_ENV_BINDING_MISMATCH
```

Archive verification:
- outer SHA exact;
- 6/6 inventoried evidence files hash-match;
- task admission PASS;
- cleanup PASS.

The payload stopped immediately after admission.

No source acquisition, source gate, runtime observation, provider call, Foundation trial or model inference occurred.

Exact cause:

```text
formation task identity:
  task.vexlife-prod.multivex.foundation-crossover.a001

payload expected:
  task.vexlife-prod.multivex.foundation-reach-replication.a001
```

The formation file inherited the old crossover transaction/task/attempt/package/request/correlation
and requesting-instance identity family even though the payload and storage key had already moved to
reach replication.

A001 therefore contains no evidence about Foundation behavior, reach choice, cache behavior or the
matched-pair design.

A002 corrects the whole carriage identity family together and keeps the exact same experiment
candidate, exact Foundation inputs and matched-pair design.

Current carriage identity:

```text
task       task.vexlife-prod.multivex.foundation-reach-replication.a002
attempt    attempt.vexlife-prod.multivex.foundation-reach-replication.a002.<nonce>
transaction transaction.vexlife-prod.multivex.foundation-reach-replication.a002.<nonce>
```

A001 is consumed and is not replayed.

## A002 consumed — first F19 reach attempt rejected by Product request admission

Canonical RETURN:

```text
RETURN-vexlife-prod-multivex-foundation-reach-replication-attempt-a002-6edd50a671e64b65a8a0
sha256 99eadf853a8d03e64c5af73652cacfc3ab996fc1a853df54b500391de656b206
disposition SAFE_FAILURE
stop MULTI_VEX_REACH_REPLICATION_RESULT_MISSING
```

Archive verification:
- outer SHA exact;
- 6/6 inventoried evidence files hash-match;
- task admission PASS;
- exact F19/F20 Foundation byte bindings PASS;
- exact source PASS;
- focused reach-replication source gate PASS;
- full JVM regression gate PASS;
- cleanup PASS.

Verified pre-trial Product state:

```text
selected model = exact Product 4B
prompt cache entries = 2
runtime receipt = receipt.vexlife-prod.heretic-persistent-runtime.9aaa7355889a46cbfb376e4f
```

A002 reached the first scheduled matched trial:

```text
pair 1
order 1
Foundation v0.6.19
exact B0 first-message bytes
```

The model emitted a Conversation Source Return control request. Product then rejected the request
before Source Return execution because:

```text
Conversation source-return query terms must remain unique after normalization
```

So A002 already establishes one important replication observation:

```text
F19 can choose the Source Return route under the exact B0 wording.
```

That weakens any deterministic reading of the terminal crossover's one-pair observation:

```text
F19 -> no reach
F20 -> reach
```

A002 does not preserve the invalid request's exact queryTerms/speakerScope because Product admission
failed before a canonical Source Return trace was formed. Raw provider output is intentionally not
durable.

No eight-trial result file was formed, and the payload therefore did not reach its normal post-run
Product configuration / protected-state snapshot. Pre-state is verified; post-state is not claimed
from A002.

### Observer correction

Product's Source Return admission remains strict and unchanged.

The reach-replication observer now separately preserves:

```text
reach attempt
-> request admission
-> Source Return effect
```

A synthetic-only transport observer records the Source Return control before Product admission:
- exact queryTerms;
- speakerScope;
- request state ADMITTED / REJECTED;
- rejection reason;
- control-content SHA / character count.

If Product rejects a Source Return request, the trial becomes:

```text
SOURCE_RETURN_REQUEST_REJECTED
```

and the remaining matched trials continue.

The final replication result becomes HELD rather than disappearing if any request is rejected, so the
full eight-trial attempt/admission/effect matrix remains available for review.

A regression test uses the exact A002 failure family: duplicate query terms after normalization remain
observable by Multi-Vex while Product continues to reject them.

Corrected candidate:

```text
head 5ce68b1883e8fd81c9d5aaeccf05ccf01dd3c1ba
tree 0da5d8bf77b231fa6daec046b5b5e40d6ff0b219
```

A002 is consumed and is not replayed.

A003 keeps the same four matched pairs, exact B0 wording, exact Foundation inputs and read-only Product
runtime boundary. Only the proof observer advances.

## A003 terminal evidence — full eight-trial matrix preserved

Canonical RETURN:

```text
RETURN-vexlife-prod-multivex-foundation-reach-replication-attempt-a003-397099b67e04c54bc730
sha256 381c009fec19405f3f8092380d55b35133692a6bff44d47306ea0ed9e4f3240b
disposition SAFE_FAILURE
stop MULTI_VEX_REACH_REPLICATION_ACCEPTANCE_NOT_MET
```

Archive verification:
- outer SHA exact;
- 6/6 inventoried evidence files hash-match;
- task admission PASS;
- exact F19/F20 Foundation byte bindings PASS;
- exact source PASS;
- focused reach-replication source gate PASS;
- full JVM regression gate PASS;
- all 4 matched pairs / 8 trials observed;
- Product runtime configuration unchanged 2 -> 2;
- Product runtime receipt unchanged;
- canonical Product Conversation unchanged;
- canonical Product Memory unchanged;
- selected Product model unchanged;
- cleanup PASS.

The result is HELD rather than PASS because one F20 Source Return control was rejected before effect.
The matrix itself is complete and must not be replayed merely for a green outer label.

### Reach attempt / admission / effect matrix

```text
F19
  trials                  4
  reach attempts           0
  admitted requests        0
  rejected requests        0
  Source Return effects    0

F20
  trials                  4
  reach attempts           2
  admitted requests        1
  rejected requests        1
  Source Return effects    1
  returned source matches  0
```

Matched pairs:

```text
pair 1: F19 no reach | F20 no reach
pair 2: F20 admitted reach -> NO_MATCH | F19 no reach
pair 3: F19 no reach | F20 rejected reach
pair 4: F20 no reach | F19 no reach
```

So this replication has:
- two F20-only reach pairs;
- two ties;
- zero F19-only reversals.

This strengthens a treatment-consistent direction on reach propensity in this run.

It does not establish deterministic behavior. A002 already observed one F19 reach attempt under the same
exact B0 wording before its malformed request was rejected.

### Query quality

The admitted F20 request used:

```text
queryTerms:
  Saturday plan
  Saturday schedule
  Saturday prep
  Saturday plan change

speakerScope:
  ANY

effect:
  NO_MATCH
```

The scratch Conversation really contained the Saturday plan, but not under those exact compound
phrases. Therefore:

```text
choosing Source Return
!=
forming a query that can recover the source
```

The rejected F20 request preserved:

```text
queryTerms:
  Saturday plan
  Saturday schedule
  Saturday plan
  Saturday prep
  Saturday plan set up

speakerScope:
  missing / UNKNOWN

Product admission:
  REJECTED
```

Product's strict admission law remains valid.

### Answer-grounding observations

No trial returned the old/corrected plan through Source Return.

Several no-reach answers preserved uncertainty and asked the person for the missing plan.

Other no-reach answers invented unsupported plan details. This happened under both Foundation
versions:
- one F20 trial invented a 20-minute morning meeting and 45-minute team call;
- one F19 trial invented a Saturday prep/free-time/evening-meetup plan.

The admitted F20 NO_MATCH answer also described the lookup result more strongly than exact substring
matching warrants: the source contained the Saturday plan in different wording even though the chosen
query terms found no match.

Therefore answer grounding is a separate cross-Foundation issue and is not attributed to #549.

### Standing

#549 earns experimental standing as a sampled reach-choice treatment:

```text
F20 increased reach propensity in this matched replication.
```

It does not earn:
- deterministic reach behavior;
- successful source recovery;
- answer-grounding correctness;
- Foundation acceptance / merge;
- Memory / Dream / training mutation.

The reach-choice replication is terminal evidence. Do not replay it for a clean PASS label.

The next Multi-Vex edge is character/name transfer while keeping role/scenario topology and the
simplified neutral participant method stable.

Source Return query/recovery/answer-grounding evidence is routed to the Product Conversation
continuity/source-reentry owner rather than absorbed into Multi-Vex.

## Exact question

With the exact preserved B0 first-message bytes and fresh identical changed-plan scratch history, does Source Return choice differ consistently between Foundation v0.6.19 and v0.6.20 across four counterbalanced matched pairs?

This is a reach-choice replication, not another participant test and not another 2x2 wording crossover.

## Constant message

Source RETURN:

```text
f6badd010c7b7e1ecf2a3408b0ef3241052341aceaeb346caf22884734503967
```

Exact visible bytes:

```text
Hey, what's the Saturday plan right now? I think I set it up differently a few days ago. I want to make sure we're on the same page before I start prepping.
```

Exact B0 wording has no request-bound Conversation projection cue in the crossover source contract.

## Schedule

```text
pair 1: F19 -> F20
pair 2: F20 -> F19
pair 3: F19 -> F20
pair 4: F20 -> F19
```

Each trial forms a new isolated scratch Home using the same synthetic device/lineage identity, same Person Context, same deterministic old-plan/corrected-plan setup, same background payload, and same idempotency identities.

No counterpart model participates.

## Foundation bytes

Control, current Product source:

```text
v0.6.19
body blob      2b56a2a4045befed5d62189e364147cdfcd642fe
revisions blob 747093afa4458eb8bcd3409bb8115e374740b26c
body sha256    08b5b3c6f1fbbe71be84c8873a03a184f17a5b3051ed7131ebc381e74a0c9d0c
```

Treatment, exact immutable #549 input:

```text
v0.6.20
body blob      4af59951c177bddaec058253a967f448e8a5c947
revisions blob a7a497900bacc5ded736ab12f58e0c6e876a39c8
body sha256    d6e45b6189aa45e03395a93f7ff0bceb40ee9fe6ffa692d5a5213a49fc9e806a
source head    1ae377df920b301eaffb3a9112ec89cc3d05d2e4
```

## Candidate under proof

```text
repository VexGPT/VexLife-Prod
head 5ce68b1883e8fd81c9d5aaeccf05ccf01dd3c1ba
tree 0da5d8bf77b231fa6daec046b5b5e40d6ff0b219
branch VXG-112226-evolve-context-dynamic-counterpart-dcp1-a001
```

The candidate adds a separate reach-replication adapter/test/task. The terminal crossover adapter remains unchanged.

## Product runtime boundary

A007 earned the read-only boundary:

```text
Product / Response Readiness
  owns runtime implementation, argv, cache semantics, lifecycle and rotation

Product-issued qualified runtime receipt
  is outside truth consumed by Multi-Vex

Multi-Vex
  verifies exact selected Heretic 4B identity
  verifies Product qualification
  verifies live exact-owned receipt process
  requires the same receiptRef throughout
  performs the actual provider turns
```

The payload snapshots Product runtime configuration and runtime receipt before/after. It does not change the person's cache setting or establish/recover Product runtime lifecycle.

## Evidence retained per trial

```text
pair ordinal / order
exact Foundation version + body SHA
exact B0 message source
Vex final response
Source Return attempted?
Source Return request state NONE / ADMITTED / REJECTED
Source Return request queryTerms
Source Return request speakerScope
Source Return effect state
returned source event refs
provider stage count
provider telemetry
runtime receipt ref
exact participant attribution
exact Person Context projection
```

Admitted Source Return request fields are cross-checked against the canonical DirectTurnContextStore trace.

Rejected controls have no canonical Source Return trace by design. For those trials, the synthetic-only transport observer preserves only the control request fields, rejection reason, and control-content hash before Product admission. It does not persist raw provider output or reasoning.

## Source gates

Before model effect:

```text
focused:
  MultiVexFoundationReachReplicationTest
  MultiVexFoundationCrossoverTest
  DynamicCounterpartProfilesTest
  VexLifeFoundationalFieldTest

then:
  full jvmTest
```

## Mechanical completion

Mechanical PASS requires:

- exact source + Foundation byte bindings;
- all four matched pairs / eight trials observed;
- mechanical PASS only when every trial completed through Product admission/effect;
- otherwise form a HELD result that still preserves all rejected-attempt evidence;
- B0 request-bound Conversation projection remains absent;
- exact B0 request bytes;
- exact Person Context projection;
- exact Foundation identity on every trial;
- same Product runtime receipt throughout;
- Product runtime configuration unchanged;
- canonical Product Conversation unchanged;
- canonical Product Memory unchanged;
- selected Product model unchanged;
- cleanup PASS.

Mechanical PASS reports sampled evidence only.

It does not automatically accept #549, write Memory, run Dream, train, mark READY or merge anything.

## Review after RETURN

First receive the full eight-trial matrix.

Then ask:

```text
How often did F19 attempt Source Return?
How often did F20 attempt Source Return?
How many requests were admitted vs rejected?

Within each matched pair, did the choice differ?
What exact queryTerms / speakerScope were requested?
Did returned-source state change the final answer?
Did any trial overstate what its source actually established?
Does the pattern look treatment-consistent, stochastic, mixed, or absent?
```

Attribution follows the evidence.

<!-- [VXG RealForever][MULTI-VEX][FOUNDATION-REACH-REPLICATION][A001-CONSUMED][A002-CONSUMED][A003-TERMINAL-EVIDENCE] -->
