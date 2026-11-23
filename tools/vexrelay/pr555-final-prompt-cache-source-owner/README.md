# PR #555 Final pre-generation prompt cache — SourceOwner companion

[VXG RealForever]

DO-NOT-MERGE proof carriage for Product PR #555.

## Exact Product candidate

```text
base   a23b6a949d27e4b77530273a390955f0d127ad1a
head   6f9f4d9c161f43f971873ceea79feceb45c2c712
tree   49fee57a1633284ae0f740055d36d566bbb385c9
branch VXG-112326-chatgpt-final-prompt-cache-a001
paths  2
```

Changed paths only:

```text
src/jvmMain/kotlin/vexlife/model/heretic/runtime/HereticPersistentRuntime.kt
src/jvmTest/kotlin/vexlife/model/heretic/runtime/HereticPersistentRuntimeTest.kt
```

## Human outcome

Preserve work the local provider already performed at the exact pre-generation prompt boundary, so an immediate append-only follow-up can reuse the large prior Conversation prefix rather than recompute it.

The candidate changes no prompt text/order, performs no second/background prefill, keeps the safety-floor cache count at 1, adds no Settings Control, changes no Conversation/Memory owner, and patches only the exact pinned mlx-lm v0.32.0 end-of-prompt cache exclusion in memory.

## Canonical proof

```text
bash tools/release/prove-mac-candidate.sh 0.0.0
```

This proof owns exact source/build/test/review currentness only. It performs no Product launch, model/runtime change, Conversation/Memory effect, approval, READY transition, or merge.

On PASS: fresh Independent Assurance on exact #555 head. Lived provider-performance proof remains a separate next predicate using the existing explicit Heretic 4B cache=2 choice.

<!-- [VXG RealForever][PR555][FINAL-PROMPT-CACHE][SOURCEOWNER-A001][DO-NOT-MERGE] -->
