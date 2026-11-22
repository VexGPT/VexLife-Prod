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

## SourceOwner question

Prove that the exact candidate:
1. compiles the current participant projection;
2. keeps canonical current REQUEST `speakerRef` as the participant coordinate;
3. represents absent human-readable name as `UNKNOWN`;
4. represents one source-backed synthetic Sarah name claim without replacing `speakerRef`;
5. leaves the optional name claim outside the existing durable Prompt Receipt wire shape;
6. preserves the complete JVM regression suite.

Focused proof:

```text
./gradlew --no-daemon   -Dorg.gradle.jvmargs=-Xmx4g   -Dkotlin.daemon.jvm.options=-Xmx4g   jvmTest   --tests vexlife.companion.DirectPromptParticipantIdentityTest
```

Then full `jvmTest` with the same Product-owned build-memory envelope.

## Current Relay

```text
SDK head 6f8f6f50467f5df5a542713f7dc25b82db4ac8fa
SDK tree 96795640b01f1969da980f52d5a8ec136a8c44aa

contractRef source.vexrelay.current-contract/v1
runtime v0.2.0-RC1
recipe ExactHeadSourceOwnerProof
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
- approval/READY/merge/publication.

PASS advances #537 only to fresh Independent Assurance. Dynamic counterpart #532 remains separately held until this relationship is consumed and the harness binds it truthfully.

<!-- [VXG RealForever][PR537][PARTICIPANT-IDENTITY][SOURCEOWNER-A001][DO-NOT-MERGE] -->
