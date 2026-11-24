# PR #582 Qwen 4B cache=2 recommendation — SourceOwner A002

[VXG RealForever]

DO-NOT-MERGE proof attempt for Product PR #582.

## Exact Product candidate

```text
base   3a47bac8618c99214cfdb3d4396e626bc95b96ef
head   111041da34a2102675c8d30c605fecb29acc154d
tree   2ace82e91380170295d210be3c9b48903571a381
branch VXG-112326-chatgpt-qwen4b-cache2-recommendation-a001
paths  9
```

Changed paths:

```text
src/commonMain/kotlin/vexlife/model/VexLifeModelsFamily.kt
src/commonMain/kotlin/vexlife/model/heretic/HereticRuntimeConfiguration.kt
src/commonMain/kotlin/vexlife/web/VexLifeWebP1RuntimeArtifacts.kt
src/commonTest/kotlin/vexlife/model/VexLifeModelsFamilyTest.kt
src/commonTest/kotlin/vexlife/model/heretic/HereticRuntimeConfigurationTest.kt
src/commonTest/kotlin/vexlife/web/VexLifeWebP1RuntimeArtifactsTest.kt
src/jvmTest/kotlin/vexlife/model/VexLifeModelsTest.kt
src/jvmTest/kotlin/vexlife/model/heretic/runtime/FileHereticRuntimeConfigurationStoreTest.kt
src/jvmTest/kotlin/vexlife/model/heretic/runtime/HereticExecutionTopologyTest.kt
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

<!-- [VXG RealForever][PR582][SOURCEOWNER-A002][DO-NOT-MERGE] -->

## A002 projection-closure distinction

A001 compiled and then ran 1,035 JVM tests / 20 failures. The repository exposed one genuine
projection gap: when recommendation 2 sits above minimum 1, the existing Models choice-standing
projection had no truthful representation for the lower-memory value 1.

A002 closes only the observed recommendation consumers:
- lower-than-recommended value: no standing badge;
- recommended value: Recommended;
- higher admitted experimental value: Experimental;
- web renderer omits the separator when standing is absent;
- Models/store/Topology tests currentize recommendation 2 and Use Recommended lawfulness.

No Product runtime/cache mechanism, Home configuration, or localization wording changes.
