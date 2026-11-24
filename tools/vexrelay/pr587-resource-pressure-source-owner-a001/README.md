# PR #587 Response Readiness resource-pressure proof harness — SourceOwner A001

[VXG RealForever]

DO-NOT-MERGE proof carriage for Product PR #587.

## Exact Product candidate

```text
base   3a47bac8618c99214cfdb3d4396e626bc95b96ef
head   e67424a45bcbd304534759886a583fedd2573535
tree   5b7f5950aa8983fcf302d4d5583d2476edd848c1
branch VXG-112326-chatgpt-response-readiness-resource-pressure-a001
paths  1
```

Changed path only:

```text
src/jvmTest/kotlin/vexlife/harness/ResponseReadinessResourcePressureTest.kt
```

## Human outcome

Prove the measurement harness itself before any lived resource-pressure execution.

The harness is test-only and ordinary repository proof leaves it dormant unless the explicit resource-pressure environment is supplied. SourceOwner therefore proves:

- exact #573 parent/current source binding;
- the one-path proof membrane;
- Kotlin/test compilation and ordinary repository tests;
- no Product cache policy, runtime, Home, Conversation, Memory or presentation effect.

It does **not** run the cache-1/cache-2 pressure matrix.

## What the later lived proof is shaped to measure

After SourceOwner PASS + fresh Assurance, a separately qualified lived carrier may compare the exact #573 Conversation-prefix provider under cache=1 versus cache=2 while observing:

```text
host total + reclaimable memory
lane-local child RSS
canonical runtime RSS when present
provider cache footprint
reuse-time peak pressure
generation health
cached / prefill / latency telemetry
bounded child + scratch cleanup
```

No threshold or automatic policy is encoded by this proof source.

## Canonical proof

```text
bash tools/release/prove-mac-candidate.sh 0.0.0
```

The proof uses a private JVM temp namespace and shell trace around the repository-owned proof command.

No Product launch, model/runtime change, Home/Conversation/Memory effect, resource-policy mutation, approval, READY transition or merge is admitted.

On PASS: fresh Independent Assurance on exact #587 head. Only after that may the lived resource-pressure execution carrier be formed.

<!-- [VXG RealForever][PR587][RESOURCE-PRESSURE][SOURCEOWNER-A001][DO-NOT-MERGE] -->
