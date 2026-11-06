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

## Current candidate boundary

This first slice owns only:

~~~text
exact prompt-materialization pressure facts
        ↓
Pressure
        ↓
effect-free Plan
        ↓
optional reference-only Checkpoint
~~~

It does **not yet**:

- persist checkpoints under canonical Home;
- supersede a prior durable working checkpoint;
- claim or issue successor context;
- change `DirectPromptContext` selection;
- activate `WORKING_CHECKPOINT_SUCCESSOR`;
- change Memory selection;
- add Capability Atlas content;
- change provider/model behavior.

Those effects remain the next consumer-driven steps after the semantic contract is accepted.

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
