# A003 RETURN evidence

`[VXG RealForever]`

User-returned archive:
- filename: `RETURN-VEX-EVOLVECONTEXT-STAGE-A003-20261121T175538Z.zip`
- SHA-256: `294f2422c96811cb6ef5f7aabe78f55ba3711ec3cc6be6fed95b7f8311ad1ad7`
- bytes: `24674`
- formed at: `2026-11-21T17:56:38Z`

Source binding inside RETURN:

```text
attempt=A003
expected_head=f9a37bd8931d441760c19428ce3f6925e83d7d22
observed_head=f9a37bd8931d441760c19428ce3f6925e83d7d22
branch=VXG-112126-vex-evolve-context-stage-a002
```

Source status before/after was clean.

Focused Gradle execution:
- `compileKotlinJvm`: PASS
- `compileTestKotlinJvm`: PASS
- `VexLifeHostContextContinuationTest`: 4 tests / 0 failures / 0 errors
- `ContinuityPressureAdapterTest.hostContinuationTreatmentPutsOlderExactSourceInTheFirstProviderCall`: 1 test / 1 failure
- full JVM regression: not run because focused gate failed

JUnit failure:

```text
java.lang.AssertionError: expected:<0> but was:<2>
ContinuityPressureAdapterTest.kt:165
```

Harness system evidence:

Baseline:
- completed all 30 turns
- first Working Continuity rollover: ordinal 17
- probe.01 cobalt source route: `HYDRATED_THIS_TURN`
- model-visible continuity revisits: 10

Treatment:
- completed turns before failure: 20
- failed while forming/executing ordinal 21 (`probe.01`)
- failure emitted by harness: `direct Companion turn failed: ADMISSION_FAILED`
- model-visible revisit calls before failure: 0

Interpretation:
- host cue recognition and exact canonical Conversation reopening are source-proved by 4/4 tests;
- A003 does not prove provider-call #1 hydration because the treatment turn is held earlier by an admission/currentness invariant;
- A004 must expose the nested exception chain, failed-turn provider-call count and failed-turn chronology before another semantic change.

The exact user-returned archive hash is retained even though this connector did not safely preserve the larger ZIP bytes in-repository. The erroneous unverified A003 binary attachment was removed rather than misrepresented as exact evidence.

<!-- [VXG RealForever][VEX-EVOLVECONTEXT][A003][RETURN-EVIDENCE] -->
