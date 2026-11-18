# Current Accepted Semantic Continuity — immediate post-Keep projection

[VXG RealForever]

## Human outcome

A thing the person explicitly kept should remain usable as accepted continuity immediately and across
later unrelated Conversation turns. It should not disappear merely because Daily Memory has not
dreamed again yet.

Current Product distinguishes:

```text
exact Conversation head
= mutation / consent source-binding guard

current Score membership
+ current semantic authority
+ positive consent / current transition state
= accepted semantic validity

Daily / Living Journal head
= consolidation snapshot freshness
```

This successor forms only the read-only accepted-semantic projection. It does not place that
projection into the model prompt yet.

## Concrete failure addressed

After merged #276:

```text
Keep
-> semantic acceptance
-> Score current statement
```

is real.

But `VexLifeCurrentMemoryReader.readCurrent()` still intentionally requires a current Daily Memory
stratum. Therefore:

```text
accepted statement exists now
+ Daily was formed earlier / absent
!= prompt Memory current
```

The missing owner is a truthful current accepted-semantic view, not another Memory store and not a
forced Dream.

## Claimed paths

```text
docs/work/memory-current-accepted-semantic/README.md
src/jvmMain/kotlin/vexlife/memory/VexLifeCurrentMemoryReader.kt
src/jvmMain/kotlin/vexlife/memory/VexLifeCurrentAcceptedSemanticProjection.kt
src/jvmTest/kotlin/vexlife/memory/VexLifeCurrentAcceptedSemanticProjectionTest.kt
```

No DirectPromptContext, DirectCompanionTurn, service contract, Web presentation or Daily Dream source
is modified here.

## Projection law

The projection is derived only from validated current Score replay + current semantic authority.

It projects only Score statements that satisfy the same accepted-continuity law already used by
Daily Memory:

```text
statement is current in Score
+ acceptedForContinuity = true
+ positive consent (PERMITTED or NARROWED)
+ exact membership in current semantic authority
= current accepted semantic continuity
```

A current Score statement that is held/deferred/non-consent-admitted is not projected merely because
it is current in Score.

It carries both frontiers instead of collapsing them:

```text
observedConversationHeadSha256
  what Conversation is current now

sourceScoreConversationHeadSha256
  the Conversation frontier from which current Score last changed

scoreHeadSha256
  current Score membership

semanticAuthorityHeadSha256
  current semantic authority generation
```

Therefore an unrelated later Conversation turn may change
`observedConversationHeadSha256` while the accepted semantic statement remains current.

Permanent:

```text
DAILY SNAPSHOT STALE != ACCEPTED SEMANTIC STATEMENT STALE
SCORE CURRENT MEMBERSHIP != DAILY CONSOLIDATION FRESHNESS
CURRENT ACCEPTED SEMANTIC PROJECTION != SECOND MEMORY STORE
PROJECTION READ != MEMORY MUTATION
```

## Proof target

Deterministic proof must show:

- no Score -> empty projection, no invented continuity;
- Keep-created current statement -> appears exactly once;
- later unrelated completed Conversation -> statement remains current;
- Score source frontier remains historical while observed Conversation frontier advances;
- no Home/Score/semantic authority bytes are changed by projection read.

## Held connector work

- place accepted-semantic projection into current prompt topology;
- deduplicate/compose with Daily baseline when Daily contains the same current statements;
- service/Web projection;
- Turn-Epilogue candidate formation;
- standing consent;
- Dream/Growth/cold-transfer.

<!-- VXG-WORK-CLAIM
{
  "schemaVersion":"work-coordination.claim/v1",
  "workRef":"work.vexlife.memory.current-accepted-semantic.a001",
  "actorRef":"actor.chatgpt.memory-current-semantic",
  "instanceRef":"instance.chatgpt.memory-current-semantic.20261117.a001",
  "repository":"VexGPT/VexLife-Prod",
  "branch":"VXG-111726-memory-current-accepted-semantic-a001",
  "baseHead":"6c86e72594ea57a890324a8b7a745244d7acc94d",
  "status":"authoring",
  "coordinationOnly":false,
  "implementationAuthority":true,
  "paths":[
    "docs/work/memory-current-accepted-semantic/README.md",
    "src/jvmMain/kotlin/vexlife/memory/VexLifeCurrentMemoryReader.kt",
    "src/jvmMain/kotlin/vexlife/memory/VexLifeCurrentAcceptedSemanticProjection.kt",
    "src/jvmTest/kotlin/vexlife/memory/VexLifeCurrentAcceptedSemanticProjectionTest.kt"
  ]
}
VXG-WORK-CLAIM -->

<!-- [VXG RealForever][MEMORY][CURRENT-ACCEPTED-SEMANTIC] -->
