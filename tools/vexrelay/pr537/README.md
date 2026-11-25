# PR #537 Participant Identity Stage A — SourceOwner companion

[VXG RealForever]

DO NOT MERGE. Product/source authority remains PR #537.

## Exact Product candidate — A003

```text
base   0566b1eed876a28c2fcc6586743ae1f4af7eaecd
head   f1855e9be6b9172ba6b43b597fa5d902dc409ed1
tree   82bc5b07abdb7746f935628571ad440b1da85c87
branch VXG-112226-evolve-context-participant-identity-a001
paths  4
```

Changed paths:

```text
src/jvmMain/kotlin/vexlife/companion/DirectPromptContext.kt
src/jvmTest/kotlin/vexlife/companion/DirectPromptContextTest.kt
src/jvmTest/kotlin/vexlife/companion/DirectPromptFoundationalFieldTest.kt
src/jvmTest/kotlin/vexlife/companion/DirectPromptParticipantIdentityTest.kt
```

## Attempt history

### A001 — preformation failure

A001 never formed a Relay task.

```text
RECIPE_SCOPE_MISMATCH:
ExactHeadSourceOwnerProof:
STAGE_A_BOUNDED:
STAGE_BOUNDED
```

No Product source/test result was produced.

### A002 — focused identity PASS, full-suite regression mismatch

A002 reached the exact Product candidate.

Focused:
```text
DirectPromptParticipantIdentityTest
PASS
```

Full `jvmTest`:
```text
1000 tests
2 failed

DirectPromptContextTest.personContextTellsVexTheReplyLanguageAndThePrivateGaps
DirectPromptFoundationalFieldTest.theFieldIsTheFirstSystemContentAheadOfMemoryCapabilityAndPersonContext
```

Both failures were existing assertions for the former provider header:

```text
VEXLIFE_PERSON_CONTEXT/v1
```

while #537 intentionally emits:

```text
VEXLIFE_PERSON_CONTEXT/v2
participantRef = exact current REQUEST speakerRef
name = null when no claim exists
nameStanding = UNKNOWN
```

A003 currentizes those two existing regression surfaces to assert the v2 relationship and UNKNOWN-name
semantics. Product runtime source remains the same one-file #537 implementation.

## A003 SourceOwner question

Prove that the exact four-path candidate:

1. keeps canonical current REQUEST `speakerRef` as the participant coordinate;
2. represents an absent human-readable name as `UNKNOWN`;
3. represents a source-backed synthetic Sarah name claim without replacing `speakerRef`;
4. keeps Person Context in the current-state envelope rather than the stable system block;
5. preserves the Foundational Field as the first stable system ground;
6. preserves the complete JVM regression suite.

Focused proof:

```text
DirectPromptParticipantIdentityTest
DirectPromptContextTest.personContextTellsVexTheReplyLanguageAndThePrivateGaps
DirectPromptFoundationalFieldTest.theFieldIsTheFirstSystemContentAheadOfMemoryCapabilityAndPersonContext
```

Then full `jvmTest`.

## Relationship boundary

This remains one participant-facing slice of the larger already-preserved VexLife relationship trajectory:

```text
Conversation
  exact speaker / recipient / source

Score + Memory
  accepted semantic relationship meaning
  current / corrected / superseded / unresolved standing

EvolveContext #500
  source-addressed Context / ContextRevision terrain
  FROM / TO / ABOUT / WITHIN / STANDING
  currentization without source rewriting

Recognition #508
  true relational custody remains held on:
  SUBJECT != SPEAKER != SOURCE OWNER != AUTHORIZATION OWNER != RECIPIENT

Dream
  consolidates accepted Memory
  does not mint authority / relationship truth / synchronization
```

A003 does not decide a universal relationship schema.

Its intended next consumer, if proved and independently assured, is the dynamic-counterpart trajectory:
use the participant standing in a lived Sarah↔Vex encounter and let that relationship reveal whether
another durable relationship owner is actually earned.

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
- canonical Home/Conversation/Memory mutation;
- Sarah dynamic-counterpart execution;
- multi-participant Conversation implementation;
- Context/ContextRevision implementation;
- Dream/Dream Sync effect;
- approval/READY/merge/publication.

PASS advances #537 only to fresh Independent Assurance.

<!-- [VXG RealForever][PR537][PARTICIPANT-IDENTITY][SOURCEOWNER-A003][DO-NOT-MERGE] -->
