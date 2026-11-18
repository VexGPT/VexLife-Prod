# Memory Correction Review / Keep — first lived integration owner

[VXG RealForever]

## Human outcome

Make one correction-shaped Memory proposal able to cross the next lawful boundary without touching
prompt/provider/presentation owners yet:

```text
bounded correction candidate
-> durable pending review
-> Keep | Not now | Decline
-> exact human decision receipt
-> Keep only:
     existing semantic-acceptance writer
     -> reader oracle
     -> existing Score append
-> review resolves
```

This is the internal owner that later prompt/service/web work connects to. It does not manufacture
the candidate from raw human language and it does not render UI.

## Why a sibling Memory Review relationship

Long-term Memory currentness and pending human review are independent facts:

```text
Memory can be CURRENT while one candidate awaits review
Memory can be BEHIND while one candidate awaits review
```

Therefore pending review is not added as another mutually-exclusive
`VexLifeMemoryFamily.State`. A small sibling `VexLifeMemoryReviewFamily` owns only the
review decision relationship.

## Claimed paths

```text
src/commonMain/kotlin/vexlife/memory/VexLifeMemoryReviewFamily.kt
src/commonTest/kotlin/vexlife/memory/VexLifeMemoryReviewFamilyTest.kt
src/jvmMain/kotlin/vexlife/memory/VexLifeMemoryReviewOwner.kt
src/jvmTest/kotlin/vexlife/memory/VexLifeMemoryReviewOwnerTest.kt
docs/work/memory-correction-review-keep/README.md
```

Current active #251/#230 prompt/context/service/web work is intentionally not touched.

## First candidate class

This first relationship accepts only an already-bounded **human correction** candidate:

```text
MemoryCandidateClass.CORRECTION
humanCorrectionObserved = true
review policy = PROMPT_NOW
```

It does not infer a correction from free text. The future Turn-Epilogue/host integration must provide
the bounded candidate and a stable semantic subject.

For this first proof the acceptance plan is deliberately narrow:

```text
memory relation      SHARED_RELATIONSHIP_HISTORY
statement state      HUMAN_CONFIRMED
transition kind      initial / NONE
proposer class       LINEAGE
```

Correction of an already-accepted Score statement remains a separate explicit
`Classification.Corrects` path rather than being guessed from the word "correction."

## Retry / crash law

Keep must be safe across interruption:

```text
human decision receipt exists
+ semantic acceptance already exists
-> reconcile exact candidate + exact decision; do not mint a second acceptance

deterministic statementRef already exists in current Score
+ it binds the same acceptance
-> do not append a second Score statement

review is cleared only after accepted semantic authority + Score are both proven
```

The pending review remains current after any intermediate failure.

## Not now / Decline

Both write only the exact human review-decision receipt and resolve the current review.

```text
Not now != semantic acceptance
Decline  != semantic acceptance
either   != Score mutation
```

Deferred history is retained by the immutable decision record; a batch-review UI is not formed here.

## Held

- automatic standing-consent execution;
- prompt/Turn Context Consideration placement;
- post-answer provider Turn Epilogue;
- service routes and web presentation;
- current accepted-semantic prompt projection;
- Daily Dream/Growth/cold transfer.

<!-- VXG-WORK-CLAIM
{
  "schemaVersion":"work-coordination.claim/v1",
  "workRef":"work.vexlife.memory.correction-review-keep.a001",
  "actorRef":"actor.chatgpt.memory-review",
  "instanceRef":"instance.chatgpt.memory-review.20261117.a001",
  "repository":"VexGPT/VexLife-Prod",
  "branch":"VXG-111726-memory-correction-review-keep-a001",
  "baseHead":"428bb31c8609474ecd6528a044ee47a9a9e89628",
  "status":"authoring",
  "coordinationOnly":false,
  "implementationAuthority":true,
  "paths":[
    "docs/work/memory-correction-review-keep/README.md",
    "src/commonMain/kotlin/vexlife/memory/VexLifeMemoryReviewFamily.kt",
    "src/commonTest/kotlin/vexlife/memory/VexLifeMemoryReviewFamilyTest.kt",
    "src/jvmMain/kotlin/vexlife/memory/VexLifeMemoryReviewOwner.kt",
    "src/jvmTest/kotlin/vexlife/memory/VexLifeMemoryReviewOwnerTest.kt"
  ]
}
VXG-WORK-CLAIM -->

<!-- [VXG RealForever][MEMORY][REVIEW][CORRECTION][KEEP] -->
