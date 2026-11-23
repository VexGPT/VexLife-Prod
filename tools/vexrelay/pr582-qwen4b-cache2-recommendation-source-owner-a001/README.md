# PR #582 Qwen 4B cache=2 recommendation — SourceOwner A001

[VXG RealForever]

DO-NOT-MERGE proof attempt for Product PR #582.

## Exact Product candidate

```text
base   3a47bac8618c99214cfdb3d4396e626bc95b96ef
head   f35fdd0f1fc67ddc765107099bcc33ef7a9f0869
tree   f2a64e6acefb988a6dc5f42ff6e86c42f29d385e
branch VXG-112326-chatgpt-qwen4b-cache2-recommendation-a001
paths  2
```

Changed paths:

```text
src/commonMain/kotlin/vexlife/model/heretic/HereticRuntimeConfiguration.kt
src/commonTest/kotlin/vexlife/model/heretic/HereticRuntimeConfigurationTest.kt
```

## Evidence relationship

#573 isolated lived A005 proved a 37,357-token second 4B prompt reused 35,037 cached tokens, leaving
2,320 prefill tokens and 4.647 s first-token latency. Canonical Product state remained unchanged.

#582 changes only recommendation evidence:

```text
Qwen 4B default      1  unchanged
Qwen 4B recommended  2
Qwen 4B maximum      3  unchanged
standing             EXPERIMENTAL_CANDIDATE
```

No Home setting is auto-written. Cache=3 remains held by the earlier repetitive/high-memory result.

## Canonical proof

```text
private JVM temp root
bash -x tools/release/prove-mac-candidate.sh 0.0.0
```

No Product launch, runtime/configuration mutation, localization change, approval, READY or merge
effect is admitted.

On PASS: fresh Independent Assurance on the recommendation semantics; if CLEAR, #550 copy currentizes.

<!-- [VXG RealForever][PR582][SOURCEOWNER-A001][DO-NOT-MERGE] -->
