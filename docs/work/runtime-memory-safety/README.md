# Runtime memory safety — prompt cache, prefill and generation health

`[VXG RealForever]`

## Status

DRAFT WORK / NOT PRODUCT POLICY / NOT AN IMPLEMENTATION CLAIM

This note is the front door for the runtime-reliability work earned by a lived Mac failure on
2026-11-15. It is intentionally separate from distribution, reasoning/reach experiments and model
presentation.

Current repository floor when formed:

```text
main
8f052bf48920dec6516ea0e4cf379464f78bc80a
```

Related Product work:

- #152 — stopped-turn / incomplete-output evidence preservation.
- #153 — optional gpt-oss-20B Heretic model + Harmony response splitting.
- #150 / #157 / #162 — reasoning/reach framing experiments; not the cause of this runtime failure.
- distribution / DMG / website work is owned by another lane; read only when integration currentness
  requires it.

## Human problem

A long Vex turn can complete successfully, then the immediately following human turn can fail with:

```text
CONNECTION_EndpointResponseInvalid
streaming Companion endpoint closed before [DONE]
```

The lived debug export finally carried the runtime logs that closed the causal circuit.

Observed failure window:

```text
22:02:18  SEND_ACCEPTED
22:02:22  PROVIDER_INVOCATION_REQUESTED

prompt
  about 176,419 characters
  150 messages
  about 44,107 prompt tokens

22:02:23  mlx-lm prompt cache
  10 sequences
  about 20.01 GB

22:04:09  mlx-lm generation thread
  METAL: Insufficient Memory
  kIOGPUCommandBufferCallbackErrorOutOfMemory

22:04:10  Conversation observes
  stream closed before [DONE]
  -> CONNECTION_EndpointResponseInvalid
```

At the failure snapshot the host reported approximately:

```text
48 GiB total unified memory
75 MiB free
runtime PID still alive
generation thread dead
```

Repeated earlier Metal OOM events were also present in the same runtime log history.

Permanent distinction earned by this evidence:

```text
PROCESS ALIVE
!= GENERATION ENGINE HEALTHY

PROMPT CACHE
!= VEX MEMORY
!= CONVERSATION
!= WORKING CONTINUITY
```

The prompt cache is a runtime optimization. It may be bounded, evicted or discarded without erasing
Vex's semantic continuity.

## Existing owners to descend through

Do not create a parallel runtime subsystem before proving the existing owner cannot carry the meaning.

Primary source owners:

```text
docs/foundation/HERETIC-PRODUCT-MODEL.md
src/jvmMain/kotlin/vexlife/model/heretic/runtime/
src/jvmMain/kotlin/vexlife/companion/DirectCompanionConnection.kt
src/jvmMain/kotlin/vexlife/companion/DirectCompanionTurn.kt
src/jvmMain/kotlin/vexlife/diagnostics/VexLifeDiagnostics.kt
```

Related continuity owners:

```text
DirectConversationStore
DirectPromptContext
VexLifeWorkingContinuityOwner / DirectWorkingContinuity
VexLifeCurrentMemoryReader
```

Runtime memory policy must not become a second Conversation/Memory owner.

## What needs to become true

### 1. Generation health must be stronger than PID liveness

Today a runtime process may remain alive after its generation thread has died.

Candidate state relationship to pressure with source:

```text
PROCESS_LIVE + GENERATION_READY
-> usable

PROCESS_LIVE + GENERATION_UNAVAILABLE
-> runtime unhealthy

PROCESS_GONE
-> runtime unavailable
```

The exact type/state vocabulary is not prescribed by this note. Reuse the existing Heretic runtime
state/currentness owner if it can truthfully express the distinction.

### 2. One runtime memory-pressure observation

Before provider inference, the rightful runtime owner needs enough physical evidence to know whether
another large prefill is safe.

Candidate inputs to investigate:

- host unified-memory pressure / available headroom;
- runtime process footprint (not merely JVM heap);
- active model realization;
- current prompt/cache footprint where the runtime exposes it;
- expected transient prefill/generation allowance;
- current prompt size / token estimate.

Do not assume macOS `freeMemory` alone is the complete pressure signal.

### 3. Model-specific policy, shared governor

Different models impose different weights/cache/prefill costs, but the safety mechanism should be
shared.

Candidate shape, not accepted API:

```text
selected model
-> model/runtime resource profile
-> shared Heretic runtime governor
   -> cache policy
   -> prefill policy
   -> KV policy if qualified
   -> minimum host headroom
   -> recovery policy
```

Do not implement "Qwen OOM fix" as a one-off condition.

### 4. Bound optimization before continuity

MLX controls worth qualifying rather than blindly enabling include:

- prompt-cache population/retention;
- prefill step size;
- KV cache size;
- KV quantization.

Any control must be tested for:

```text
peak memory
prefill latency
generation latency
answer quality
long-context quality
source-return/capability behavior
```

A smaller cache or KV window that silently harms the continuity relationship is not a valid fix.

### 5. Safe runtime recycle is allowed

If cache/runtime state approaches an unsafe threshold, a fresh human-turn boundary may retire and
restart the exact runtime before provider inference.

Permanent:

```text
RUNTIME RECYCLE
!= VEX RESET
!= HOME RESET
!= CONVERSATION RESET
!= MEMORY RESET
```

Do not restart mid-provider-stage and pretend the same provider effect completed.

### 6. OOM should become a typed cause

When evidence supports it, avoid collapsing a known Metal OOM into generic
`CONNECTION_EndpointResponseInvalid`.

A higher-fidelity failure should let a later safe boundary distinguish:

```text
transport malformed
stream unexpectedly closed
generation engine unavailable
generation OOM / resource exhaustion
```

The exact wire code is implementation work, not specified here.

## Proof matrix

Source proof:

- deterministic policy/state tests;
- runtime currentness/generation-health tests;
- no semantic continuity mutation from cache trim/recycle;
- no Product mutation from mere observation;
- exact fail-closed behavior when pressure is UNKNOWN.

Lived Mac proof on each supported candidate profile:

1. cold first turn;
2. 40k+ prompt;
3. long answer;
4. immediate follow-up;
5. repeated long turns;
6. pressure threshold;
7. safe runtime recycle;
8. capability/source-return continuation;
9. failure evidence remains visible if a turn still fails.

The debug/lived proof should record enough resource evidence to explain why a turn was admitted,
recycled or held without recording private Conversation text by default.

## Relationship to #152

#152 solves a different problem:

```text
provider/runtime failure
-> preserve the evidence / human continuity of the stopped turn
```

This work solves:

```text
prevent avoidable resource failure
+ identify typed runtime unhealth
+ recover safely
```

Both are needed. Prevention must not weaken evidence preservation.

## Relationship to reasoning / orientation

The 4/10 reach budget, counter salience and "ordinary read-only work" experiments did not cause this
failure.

Do not use runtime OOM evidence to justify a reach-policy change.

## Fresh-instance entry

Before authoring:

1. re-query current main and #152/#153;
2. read repository Culture + Foundation Index;
3. read HERETIC-PRODUCT-MODEL and current runtime source;
4. consume the lived debug evidence above as the observed need;
5. ask which existing owner can express generation health and pressure before introducing a new
   abstraction;
6. form implementation on a new branch from then-current main;
7. prove source first, then lived Mac pressure behavior.

Sibling work:
- resource-pressure Settings projection: linked sibling PR once formed;
- cross-model qualification: linked sibling PR once formed.

<!-- [VXG RealForever][VEXLIFE-PROD][RUNTIME-MEMORY-SAFETY] -->
