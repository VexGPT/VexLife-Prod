# Working Continuity Pressure + Rollover Planning

`[VXG RealForever]`

## Human question

When the bounded direct Companion prompt approaches its admitted context limit, can VexLife-Prod
identify that pressure and form a reference-only working checkpoint/rollover plan **without**:

- replaying whole Conversation history;
- asking an LLM to summarize itself;
- promoting working state into semantic Memory;
- persisting anything before a rightful effect owner is admitted;
- issuing a successor context before that effect is actually owned?

The first accepted source surface for that question is:

~~~text
src/jvmMain/kotlin/vexlife/continuity/VexLifeWorkingContinuity.kt
src/jvmTest/kotlin/vexlife/continuity/VexLifeWorkingContinuityTest.kt
~~~

## Stable VexLife provenance

Behavioral reference:

~~~text
vgong24/VexLife
7d59753687ff12726b0d309ee2dde028b5f89e8a

src/core/resource-context-pressure.mjs
src/core/continuity-rollover.mjs
src/core/browser-working-continuity-owner.mjs
src/core/browser-prompt-context-runtime.mjs
~~~

New Beginnings ports the **relationship meaning**, not the Node implementation shape.

## Pressure policy

The current first policy preserves the stable entry thresholds:

~~~text
headroom > 3 output reserves  -> NORMAL
headroom <= 3 reserves        -> WATCH
headroom <= 2 reserves        -> CHECKPOINT_RECOMMENDED
headroom <= 1 reserve         -> ROLLOVER_REQUIRED

Memory bounded out
-> at least CHECKPOINT_RECOMMENDED
~~~

The pressure projection is effect-free.

## Working checkpoint law

A checkpoint is reference-only working continuity.

It may carry refs for:

- current cursor event;
- active work nodes;
- current intent receipts;
- open loops;
- current accepted statement refs;
- continuity records;
- current Daily Stratum;
- exact source refs.

It does **not** carry raw transcript or private payload bytes and does not create a model-authored
summary.

Permanent:

~~~text
WORKING CHECKPOINT
!= DURABLE SEMANTIC MEMORY

CHECKPOINT
!= MODEL-GENERATED TRUTH

ROLLOVER PLAN
!= ROLLOVER EFFECT
~~~

The plan may say that a checkpoint or successor context is now lawful. It cannot perform those effects.

## Durable working-continuity owner

The next consumer of the accepted planner is now source-owned by:

~~~text
src/jvmMain/kotlin/vexlife/continuity/VexLifeWorkingContinuityOwner.kt
src/jvmTest/kotlin/vexlife/continuity/VexLifeWorkingContinuityOwnerTest.kt
~~~

It owns only durable custody for already-earned planner truth:

~~~text
effect-free committable Plan
-> immutable checkpoint
-> immutable commit receipt
-> head.json currentness commit point

ROLLOVER_COMMITTED
-> exact successor-context claim
-> successor Context itself still NOT issued
~~~

Currentness law:

~~~text
checkpoint file alone != current
commit receipt alone != current
head.json          = currentness pointer
~~~

The owner writes immutable checkpoint + receipt evidence first and advances `head.json` last. A crash before the head write therefore cannot silently promote partial evidence to current truth.

Before any commit it re-derives the exact plan from the accepted planner, current durable checkpoint, pressure input and working set. Before any successor claim it requires one exact current `ROLLOVER_COMMITTED` head and binds the claim to that checkpoint/receipt plus the prior Conversation head/request and the next request event binding.

Permanent:

~~~text
DURABLE WORKING CHECKPOINT
!= SEMANTIC MEMORY

SUCCESSOR CLAIM
!= SUCCESSOR CONTEXT ISSUANCE

OWNER CUSTODY
!= PROMPT MATERIALIZATION AUTHORITY
~~~

## Exact successor Context issuance

The durable owner now has one narrower direct-path successor Context contract:

~~~text
current ROLLOVER_COMMITTED head
+ exact current SuccessorClaim
        ↓
SuccessorContext(contextMode = WORKING_CHECKPOINT_SUCCESSOR)
        ↓
immutable successor-contexts/<checkpointFingerprint>/<currentRequestEventHash>.json
~~~

The Context carries only:

- exact checkpoint + commit-receipt identities;
- exact successor-claim identity;
- prior Conversation-head SHA-256 and checkpoint cursor request ref;
- exact next request event binding;
- the checkpoint's canonical reference-only WorkingSet;
- source refs proving the continuity lineage;
- issuance effects showing Home persistence only.

It does not carry raw Conversation history, a model-generated summary, semantic Memory, provider output, or model/runtime authority.

Currentness is owner-mediated:

~~~text
copied SuccessorContext JSON != authority

VexLifeWorkingContinuityOwner.readSuccessorContext(... exact current request ...)
-> current head
-> exact request-scoped claim
-> exact request-scoped Context
-> exact bindings
-> current SuccessorContext or null
~~~

If a later checkpoint supersedes the head, the old successor Context remains immutable evidence on disk but is no longer returned as current.

Permanent:

~~~text
SUCCESSOR CONTEXT
!= PORTABLE BEARER AUTHORITY

REQUEST A SUCCESSOR EVIDENCE
!= REQUEST B SUCCESSOR EVIDENCE
~~~

## Live working-checkpoint successor prompt

The live next-turn consumer is now source-bounded as:

~~~text
next durable REQUEST
+ exact immediate prior completed turn
+ current ROLLOVER_COMMITTED checkpoint
        ↓
request-scoped SuccessorClaim
        ↓
request-scoped SuccessorContext
        ↓
DirectWorkingContinuity.CurrentSuccessor
        ↓
DirectPromptContext v3
        ↓
current Memory
+ bounded non-semantic WorkingSet packet
+ optional browser context
+ current request
        ↓
provider
~~~

`DirectPromptContext` does not accept detached successor-Context bytes as authority. It accepts the
in-process `CurrentSuccessor` capability produced only through the direct working-continuity bridge,
and the bridge re-reads owner currentness again before provider invocation.

Conversation currentness is independently re-bound at prompt materialization:

- exact prior Conversation head SHA-256;
- exact checkpoint cursor request ref;
- exact current durable request event ref/hash/sequence;
- exact lineage/thread/channel adjacency.

In `WORKING_CHECKPOINT_SUCCESSOR` mode the prior REQUEST/RESPONSE pair is **not** selected for the
provider. The provider receives the current attributed Memory plus a compact working-continuity
packet of refs/working coordinates plus the current request.

Prompt receipt v3 records the exact successor Context/checkpoint/claim/source identities and the
working packet hash/token estimate. Historical v2 receipts remain hash-shape compatible: new
working-continuity fields use non-default-only encoding, so decoding and re-encoding old v2 durable
evidence does not inject new default fields into its canonical hash preimage.

## Failed successor recovery

VexLife-Prod requires a new send attempt after provider failure. Therefore successor claim/Context
custody is request-scoped beneath the same rollover checkpoint:

~~~text
successor-context-claims/
  <checkpointFingerprint>/
    <currentRequestEventHash>.json

successor-contexts/
  <checkpointFingerprint>/
    <currentRequestEventHash>.json
~~~

A failed request's claim/Context remain immutable evidence. A later fresh request can create its own
independent claim/Context from the still-current rollover checkpoint. No delete/release shortcut is
required, and stale request evidence cannot lock the new send attempt.

## Direct-turn producer

The direct Companion path now has one bounded producer surface:

~~~text
exact live prompt receipt + current attributed Memory
        ↓
DirectWorkingContinuity.prepare(...)
        ↓
effect-free WorkingSet + pressure Plan

successful provider turn
        ↓
canonical Context + Conversation head durable
        ↓
DirectWorkingContinuity.commitAfterCompletedTurn(...)
        ↓
NO_CHECKPOINT_REQUIRED
or CHECKPOINT_COMMITTED
or ROLLOVER_COMMITTED
or HELD
~~~

The Plan is formed while the exact Memory bounded-out counts are still available, but no working
checkpoint effect is realized until after the corresponding Conversation head is durable.

A continuity-specific formation/commit problem does **not** retroactively convert a completed
Conversation turn into a provider failure. Expected continuity-owner failures become a separate
post-commit `HELD` receipt under:

~~~text
runtime/vexlife-prod/turn-working-continuity/<turnRef>.json
~~~

The receipt is a read-side/diagnostic surface for the completed turn. It records source descent and
whether a checkpoint/rollover owner effect became current, while explicitly denying Conversation,
semantic-Memory, provider/model, training and weight-change effects.

If an unexpected boundary-level exception escapes the continuity bridge after the Conversation head
is already durable, `DirectCompanionTurn` isolates that exception from the completed send. The
continuity receipt may then be absent; absence means post-completion continuity evidence is unknown,
not that the Conversation turn failed. No failed-send recovery receipt may be manufactured for that
already-committed turn.

Permanent:

~~~text
TURN COMPLETED
!= WORKING CONTINUITY MUST HAVE SUCCEEDED

WORKING CONTINUITY HELD
!= RETROACTIVE TURN FAILURE
~~~

## Current candidate boundary

The current live-consumer slice closes the working-context rollover loop through the real direct
Companion path:

~~~text
completed high-pressure turn
-> ROLLOVER_COMMITTED

next durable request
-> request-scoped claim + Context
-> WORKING_CHECKPOINT_SUCCESSOR
-> Memory + working coordinates + current request
-> provider
~~~

It preserves the existing failed-send recovery law by allowing a fresh request-scoped successor
claim/Context after an abandoned failed successor attempt.

It does **not yet**:

- add the Capability Atlas/self-capability frame;
- expose the current prompt/working/Memory inputs through Settings / Context;
- change Memory acceptance or mutation authority;
- authorize Dream/training/activation/evolution.

Those are the next trajectory edges after this live rollover loop is accepted.
## Why this boundary is earned

If this relationship were collapsed back into prompt materialization, the product would lose a
source-owned answer to:

- why continuation is approaching/at rollover;
- whether Memory was bounded out;
- which working refs belong in the checkpoint;
- whether a later persistence/successor effect is lawful;
- whether the checkpoint is semantic Memory (it is not);
- which exact pressure observation earned the next stage.

That is meaningful currentness/recovery/projection truth, not implementation line count.

<!-- [VXG RealForever][VEXLIFE-PROD][WORKING-CONTINUITY][PRESSURE][ROLLOVER] -->
