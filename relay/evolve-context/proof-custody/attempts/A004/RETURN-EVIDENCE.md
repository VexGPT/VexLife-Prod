# A004 RETURN evidence

`[VXG RealForever]`

User-returned archive:
- filename: `RETURN-VEX-EVOLVECONTEXT-STAGE-A004-20261121T181253Z.zip`
- SHA-256: `72fdacb397d7edf2485538fb496cfe5741bfda40d2b26bb0be882224ef6fe9dc`
- bytes: `25165`
- formed at: `2026-11-21T18:13:35Z`

Source binding:

```text
expected_head=6a793ea6dcd438dfb9be307ed266b38f420cf986
observed_head=6a793ea6dcd438dfb9be307ed266b38f420cf986
branch=VXG-112126-vex-evolve-context-stage-a002
source clean before/after=true
```

Focused JVM execution:
- `compileKotlinJvm`: PASS
- `compileTestKotlinJvm`: PASS
- `VexLifeHostContextContinuationTest`: 4/4 PASS
- control/treatment proof: 1 test / 1 failure
- full JVM regression: not run because focused gate failed

Exact treatment failure evidence:

```text
ordinal=21
TurnException: direct Companion turn failed: ADMISSION_FAILED
  -> IllegalArgumentException:
     source-return final prompt requires one compact source-return trace

failedTurnProviderCalls=2

failedTurnChronologyFacts=[
  SEND_ACCEPTED,
  PROVIDER_INVOCATION_REQUESTED,
  PROVIDER_STAGE_OBSERVED,
  CAPABILITY_REQUEST_OBSERVED,
  CAPABILITY_RESULT_RETURNED,
  PROVIDER_INVOCATION_REQUESTED,
  PROVIDER_STAGE_OBSERVED,
  TURN_FAILED
]
```

Interpretation:
- host cue recognition + exact canonical Conversation discovery remain proven;
- host-prepared source reached provider stage one;
- Vex/fixture then requested continuity reach and received a capability result;
- provider stage two completed;
- failure occurs when durable turn evidence validates the final prompt;
- `DirectTurnContextStore.requireSourceReturnTrace` sees `sourceReturnState != null` and correctly requires a provider-requested Source Return trace;
- host-prepared context has no such provider-request trace by design.

Therefore A004 proves the next semantic boundary:
**host Context Continuation may reuse canonical Conversation discovery, but it must not be represented as provider Source Return in the prompt receipt/evidence contract.**

Next edge A005:
- retain Source Return exactly for provider-requested retrieval;
- give host-prepared historical context its own prompt component / receipt evidence identity;
- preserve exact source refs/currentness without fabricating a Source Return trace.

<!-- [VXG RealForever][VEX-EVOLVECONTEXT][A004][RETURN-EVIDENCE] -->
