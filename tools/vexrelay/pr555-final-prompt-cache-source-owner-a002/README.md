# PR #555 Final pre-generation prompt cache — SourceOwner A002

[VXG RealForever]

DO-NOT-MERGE proof attempt under companion PR #557.

A001 is terminal SAFE_FAILURE evidence for the prior two-path candidate and must not be replayed.

## Exact corrected Product candidate

```text
base   a23b6a949d27e4b77530273a390955f0d127ad1a
head   1d27327f85dd813af83eaa4e876ff57325a3c6ba
tree   5e0742ed78cbc5225a0c22d6db7de6326ca64714
branch VXG-112326-chatgpt-final-prompt-cache-a001
paths  3
```

Changed paths:

```text
src/jvmMain/kotlin/vexlife/model/heretic/runtime/HereticPersistentRuntime.kt
src/jvmTest/kotlin/vexlife/model/heretic/runtime/HereticPersistentRuntimeTest.kt
src/jvmTest/kotlin/vexlife/model/heretic/runtime/MacHereticRuntimePlatformTest.kt
```

A002 adds no Product behavior beyond #555's provider-cache candidate. The third path only currentizes the Mac launchd substrate test from the obsolete direct `mlx_lm.server` argv expectation to the exact new `python -c` adapter contract.

A002 also corrects stale #550 workRef / semanticAdmissionRef / returnRouteRef left in A001 proof metadata.

## Canonical proof

```text
bash tools/release/prove-mac-candidate.sh 0.0.0
```

No Product launch, model/runtime, Conversation/Memory, approval, READY or merge effect is admitted.

On PASS: fresh Independent Assurance on exact Product head, then lived 4B cache=2 performance proof.

<!-- [VXG RealForever][PR555][PR557][SOURCEOWNER-A002][DO-NOT-MERGE] -->
