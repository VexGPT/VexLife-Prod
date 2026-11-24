# PR #573 Append-only Conversation prefix cache — SourceOwner A004

[VXG RealForever]

DO-NOT-MERGE proof attempt for Product PR #573.

## Exact Product candidate

```text
base   a23b6a949d27e4b77530273a390955f0d127ad1a
head   1edebbd556f78e43a8f08fb1be233ccb33fa414f
tree   17bb6ac19a1c3565c8fe62afa385fc8ababbfc24
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
cache segmentation. A001 lived evidence proved the semantic boundary reached the provider but the old
partial-template token locator fell back under Qwen's context-sensitive assistant-history rendering.
A002 uses two context-preserving probes that both retain a later user and takes their common exact
prefix before activating custom segmentation. It changes no model-visible prompt bytes/order and does not create another
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

On PASS: fresh Independent Assurance on the corrected token locator, then a new lived 4B cache=2
continuation proof. A001 lived is terminal negative evidence and must not be replayed.

<!-- [VXG RealForever][PR573][SOURCEOWNER-A004][DO-NOT-MERGE] -->

## A004 diagnostic distinction

A002 reached the exact corrected candidate, completed JVM tests and app/DMG formation, then stopped in the unchanged Mac packaging-verification tail after the embedded whisper help check. A004 makes no Product change. It runs the same repository-owned proof script under `bash -x` so a repeated stop identifies the exact assertion; a PASS demonstrates the A002 tail failure did not reproduce.

A002 remains terminal SAFE_FAILURE evidence and is not replayed.

## A004 proof-environment distinction

A003 traced a JVM-test failure at ContinuityPressureAdapterTest.kt:61. That assertion compares a
before/after set of every matching continuity-pressure scratch directory in the process-wide
`java.io.tmpdir`; the adapter separately deletes and verifies its own exact scratch root.

A004 makes no Product or test-source change. It gives this proof invocation a private JVM temp root
through `JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=<isolated>` and `TMPDIR=<isolated>`, while retaining
`bash -x` on the same repository-owned proof script.

This removes unrelated host/process temp-namespace interference without weakening or skipping any
repository test.
