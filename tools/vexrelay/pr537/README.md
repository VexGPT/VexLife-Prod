# PR #537 Participant Identity Stage A — SourceOwner companion

[VXG RealForever]

DO NOT MERGE. Product/source authority remains PR #537.

## Exact Product candidate

```text
base   0566b1eed876a28c2fcc6586743ae1f4af7eaecd
head   a46d00760c6a987fe1d0ed4cfd2da7fabcc8887b
tree   5711b5e9b3abb5c80b7a1ed489ee41d99bbe724f
branch VXG-112226-evolve-context-participant-identity-a001
paths  2
```

Changed paths:

```text
src/jvmMain/kotlin/vexlife/companion/DirectPromptContext.kt
src/jvmTest/kotlin/vexlife/companion/DirectPromptParticipantIdentityTest.kt
```

## Attempt history

### A001 — consumed preformation failure

A001 never formed an executable Relay task and never reached Product source/tests.

Observed first failure:

```text
RECIPE_SCOPE_MISMATCH:
ExactHeadSourceOwnerProof:
STAGE_A_BOUNDED:
STAGE_BOUNDED
```

Cause: the carrier manifest classified the SourceOwner recipe as `STAGE_A_BOUNDED`; the current
`ExactHeadSourceOwnerProof` contract admits `STAGE_BOUNDED`.

No Product candidate byte, model/runtime state, Home, Conversation, Memory or semantic evidence was
consumed from A001.

### A002 — current corrected formation

A002 changes only proof-carriage identity/current-contract classification:

```text
attempt       A002
scopeMode     STAGE_BOUNDED
Product head  unchanged
Product tree  unchanged
Product branch unchanged
proof commands unchanged
```

The Product candidate remains byte-identical to A001.

## SourceOwner question

Prove that the exact #537 candidate:

1. compiles the current participant projection;
2. keeps canonical current REQUEST `speakerRef` as the participant coordinate;
3. represents absent human-readable name as `UNKNOWN`;
4. represents one source-backed synthetic Sarah name claim without replacing `speakerRef`;
5. leaves the optional name claim outside the existing durable Prompt Receipt wire shape;
6. preserves the complete JVM regression suite.

Focused proof:

```text
./gradlew --no-daemon \
  -Dorg.gradle.jvmargs=-Xmx4g \
  -Dkotlin.daemon.jvm.options=-Xmx4g \
  jvmTest \
  --tests vexlife.companion.DirectPromptParticipantIdentityTest
```

Then full `jvmTest` with the same Product-owned build-memory envelope.

## Relationship boundary recovered from Memory Depth / EvolveContext

This source candidate is deliberately one narrow participant leg of a larger already-preserved
relationship trajectory. A PASS here must not be interpreted as "the relationship model is solved."

Existing owners/architecture already distinguish:

```text
Conversation
  exact historical speakerRef / recipientRefs / source

Score + accepted Memory
  accepted durable semantic meaning
  memoryRelation
  statement state including CORRECTED / SUPERSEDED
  semantic subject + exact source bindings

EvolveContext architecture #500
  proposed Context / ContextRevision terrain
  source standing + currentization
  typed relational positions such as:
    FROM / TO / ABOUT / WITHIN / STANDING
  later earned sourceBindings[] / relationshipRefs[]

Recognition Circuit #508
  held on first-class relational custody:
  SUBJECT
  != SPEAKER
  != SOURCE OWNER
  != AUTHORIZATION OWNER
  != RECIPIENT

Daily Dream G03
  consolidates already-accepted Memory
  does not mint relationship authority
  does not synchronize or alter model weights
```

#537 therefore proves only:

```text
participant coordinate known
!= human-readable identity known

identity claim
-> explicit standing
-> exact source refs

identity claim
!= speaker identity
!= relationship authority
!= disclosure authority
```

Do not create a second identity/relationship truth store from this proof.

## Current Relay

```text
SDK head 6f8f6f50467f5df5a542713f7dc25b82db4ac8fa
SDK tree 96795640b01f1969da980f52d5a8ec136a8c44aa

contractRef source.vexrelay.current-contract/v1
runtime v0.2.0-RC1
recipe ExactHeadSourceOwnerProof
scopeMode STAGE_BOUNDED
carrier adapter.vexlocalbridge.native-vexrelay-task-archive/v1
profile profile.vexlocalbridge.macos-victor-vextreme-sdk.v1
```

## Held

This source proof performs no:
- Product launch;
- provider/model inference;
- model acquisition/switch;
- runtime lifecycle effect;
- Home/Conversation/Memory mutation;
- Sarah dynamic-counterpart execution;
- multi-participant Conversation change;
- Context/ContextRevision implementation;
- relationship-custody implementation;
- Dream/Dream Sync effect;
- approval/READY/merge/publication.

PASS advances #537 only to fresh Independent Assurance.

After that, the next architecture decision remains open to evidence. The intended consumers are
dynamic-counterpart DCP-1 and the larger relational-custody trajectory, but a PASS does not itself
decide whether the next smallest truthful implementation is participant-aware Conversation,
external-participant/Partner carriage, a bounded relationship projection, or no additional Product
source yet.

<!-- [VXG RealForever][PR537][PARTICIPANT-IDENTITY][SOURCEOWNER-A002][DO-NOT-MERGE] -->
