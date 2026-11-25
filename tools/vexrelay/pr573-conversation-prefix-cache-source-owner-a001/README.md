# PR #573 Append-only Conversation prefix cache — SourceOwner A001

[VXG RealForever]

DO-NOT-MERGE proof attempt for Product PR #573.

## Exact Product candidate

```text
base   a23b6a949d27e4b77530273a390955f0d127ad1a
head   3a93744aad833e3b19c32098b73604303d414189
tree   21951f29a18af89b48a48ffa46ff279843c8076f
branch VXG-112326-chatgpt-conversation-prefix-cache-a001
paths  10
```

Changed paths:

```text
docs/foundation/HERETIC-EXECUTION-TOPOLOGY.md
src/jvmMain/kotlin/vexlife/companion/DirectCompanionConnection.kt
src/jvmMain/kotlin/vexlife/companion/DirectCompanionTurn.kt
src/jvmMain/kotlin/vexlife/companion/DirectPromptContext.kt
src/jvmMain/kotlin/vexlife/model/heretic/runtime/HereticPersistentRuntime.kt
src/jvmTest/kotlin/vexlife/companion/DirectCompanionConnectionTest.kt
src/jvmTest/kotlin/vexlife/companion/DirectCompanionTurnTest.kt
src/jvmTest/kotlin/vexlife/companion/DirectPromptContextTest.kt
src/jvmTest/kotlin/vexlife/model/heretic/runtime/HereticPersistentRuntimeTest.kt
src/jvmTest/kotlin/vexlife/model/heretic/runtime/MacHereticRuntimePlatformTest.kt
```

## Candidate relationship

The candidate projects the already-owned `APPEND_ONLY_HISTORY` Conversation boundary into provider
cache segmentation. It changes no model-visible prompt bytes/order and does not create another
Conversation, Memory, prompt or cache semantic owner.

Provider optimization is fail-soft: unsupported/missing/token-prefix-mismatched boundaries retain
ordinary mlx-lm segmentation and the human Send remains independent of cache reuse.

The runtime launch contract also recognizes the exact #555 final-prompt-cache adapter as historical
retirement provenance so a currently running #555 candidate can converge into this successor without
becoming an unknown process.

## Canonical proof

```text
bash tools/release/prove-mac-candidate.sh 0.0.0
```

No Product launch, model/runtime, Conversation/Memory, approval, READY or merge effect is admitted.

On PASS: fresh Independent Assurance on the exact Product head, then lived 4B cache=2 continuation
proof against the stable 2,939 cached-token / ~47k prefill baseline.

<!-- [VXG RealForever][PR573][SOURCEOWNER-A001][DO-NOT-MERGE] -->
