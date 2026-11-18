# Current Accepted Semantic Prompt Block — connector-ready Memory projection

[VXG RealForever]

## Need

Merged #284 now answers:

```text
what accepted semantic continuity is current right now?
```

The active prompt/context owners should not have to reinterpret that projection, invent provider copy,
or duplicate statements already present in a current Daily Memory block.

This successor owns only the Memory-side provider serialization and exact statement-ref dedupe. It
does **not** choose prompt placement or mutate DirectPromptContext.

## Human outcome

The connector can eventually compose:

```text
Daily Memory statement refs already selected
+
current accepted-semantic projection
- exact Daily statement refs
-> one bounded current-semantic provider block
```

So a just-kept lesson can be supplied to Vex immediately without being repeated when Daily already
contains the exact same current statement.

## Claimed paths

```text
docs/work/memory-current-semantic-prompt-block/README.md
src/jvmMain/kotlin/vexlife/memory/VexLifeCurrentAcceptedSemanticPromptBlock.kt
src/jvmTest/kotlin/vexlife/memory/VexLifeCurrentAcceptedSemanticPromptBlockTest.kt
```

No DirectPromptContext, DirectCompanionTurn, service, Web, Daily Dream, Score, semantic-authority or
Home mutation path is changed.

## Provider language

The block reuses the existing Memory posture:

```text
source-attributed continuity
!= subjective recall
!= original wording by this model

conflict with current source/human
-> preserve conflict
-> do not invent reconciliation
```

The wire header is separate from Daily:

```text
VEXLIFE_CURRENT_ACCEPTED_SEMANTIC/v1
```

because this block means "accepted now" rather than "consolidated in the current Daily snapshot."

## Bounds / dedupe

- exact statementRef exclusion is the only dedupe law;
- maximum statement count reuses `VexLifeMemoryContract.MaximumPages`;
- token selection reuses Product's existing prompt token estimator and Memory token cap;
- bounded-out refs are explicit;
- token-bounded-out refs are explicit;
- no semantic ranking is invented.

## Held

- prompt placement/component identity;
- relative order versus Conversation/current-state/profile context;
- service/Web exposure;
- candidate formation / Turn Epilogue;
- standing consent.

<!-- VXG-WORK-CLAIM
{
  "schemaVersion":"work-coordination.claim/v1",
  "workRef":"work.vexlife.memory.current-semantic-prompt-block.a001",
  "actorRef":"actor.chatgpt.memory-current-semantic-prompt-block",
  "instanceRef":"instance.chatgpt.memory-current-semantic-prompt-block.20261117.a001",
  "repository":"VexGPT/VexLife-Prod",
  "branch":"VXG-111726-memory-current-semantic-prompt-block-a001",
  "baseHead":"feacef19e5fdf0f4341ac85c8669ba82675714c2",
  "status":"authoring",
  "coordinationOnly":false,
  "implementationAuthority":true,
  "paths":[
    "docs/work/memory-current-semantic-prompt-block/README.md",
    "src/jvmMain/kotlin/vexlife/memory/VexLifeCurrentAcceptedSemanticPromptBlock.kt",
    "src/jvmTest/kotlin/vexlife/memory/VexLifeCurrentAcceptedSemanticPromptBlockTest.kt"
  ]
}
VXG-WORK-CLAIM -->

<!-- [VXG RealForever][MEMORY][CURRENT-SEMANTIC][PROMPT-BLOCK] -->
