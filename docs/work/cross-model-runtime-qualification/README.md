# Cross-model runtime qualification — one safety relationship across Heretic models

`[VXG RealForever]`

## Status

DRAFT WORK / NOT PRODUCT POLICY / NOT AN IMPLEMENTATION CLAIM

Parent runtime work: #171.
Resource-awareness sibling: #172.
Current selectable-model implementation candidate: #153.

Formation floor:

```text
main
8f052bf48920dec6516ea0e4cf379464f78bc80a
```

## Human outcome

Victor wants to be able to swap among offered local models and compare them while the same Product reliability
rules remain true.

A model swap should not mean:

```text
new model
-> new ad hoc memory behavior
-> new unknown OOM threshold
-> another one-off recovery path
```

It should mean:

```text
new exact model realization
-> same runtime safety owner
-> model-specific resource profile
-> same pressure/recovery contract
-> same Conversation / Memory / capability / presentation relationships
```

## Existing model candidate

PR #153 adds:

```text
Heretic gpt-oss 20B
D8A/gpt-oss-20b-heretic-ara-v3-mlx-8Bit
12.10 GB pinned content set
recommended memory: 24 GiB
standing: CANDIDATE
response format: HARMONY
autoRecommended = false
```

It also adds `HarmonyResponseSplitter` because the current pinned mlx-lm server returns gpt-oss analysis /
commentary / final channels inline in `content`.

Permanent distinction:

```text
RESPONSE-FORMAT ADAPTER
!= RUNTIME MEMORY POLICY
```

Harmony splitting is about what the provider output means.
Memory safety is about whether provider execution remains physically healthy.

Do not entangle them.

## What #153 may mean before runtime-safety work lands

#153 can still be a valid selectable-model foundation if it is currentized and qualified.

But its current `recommendedMemoryBytes = 24 GiB` must be read as model-fit/startup guidance, not as proof that
a 24 GiB or 48 GiB Mac can sustain arbitrary long-context multi-turn execution safely.

Permanent:

```text
MODEL FITS / STARTS
!= LONG-CONTEXT RUNTIME QUALIFIED
```

If #153 is merged before #171 is implemented, keep the model CANDIDATE / explicitly selected and do not claim
the runtime-memory problem is solved.

## Shared resource profile

The runtime owner should be able to resolve one model-specific resource profile for whichever exact model is
selected.

Candidate dimensions to measure / qualify, not a prescribed API:

- weight/materialization footprint;
- normal cold runtime footprint;
- long-prompt prefill peak;
- prompt/KV cache growth;
- safe prefill step size;
- any qualified KV cache policy;
- minimum host headroom;
- pressure threshold / recycle behavior;
- recovery characteristics after generation failure.

The catalog may own stable model facts, but do not overload `HereticModelCatalog` with live runtime observation.

## Cross-model test matrix

At minimum, pressure the same Product circuit with:

| Predicate | Qwen 27B | gpt-oss 20B | Qwen 9B | Qwen 4B |
|---|---|---|---|---|
| cold start / qualification | required | required | later | later |
| ordinary short turn | required | required | later | later |
| 40k+ prompt | required | required | later | later |
| long answer | required | required | later | later |
| immediate follow-up | required | required | later | later |
| repeated long turns | required | required | later | later |
| capability continuation | required | required | later | later |
| stopped-stream evidence | required | required | later | later |
| pressure observation in Settings | required | required | later | later |
| safe recycle | required | required | later | later |

The exact matrix may expand from source pressure; do not reduce it merely to "no crash once."

## GPT-OSS-specific proof

In addition to shared runtime safety, #153 still owes model-specific lived proof:

- acquisition and exact selection;
- Harmony analysis/commentary/final split;
- no raw Harmony control markers shown;
- capability request recognized from final channel;
- system/developer message handling matches the pinned template;
- per-model sampling decision is explicit;
- output quality comparison against current Qwen profile.

A memory-safe governor should not know about Harmony channel syntax.

## Merge sequencing

### #152

The stopped-turn evidence repair is a useful reliability floor and may be qualified/merged independently of the
runtime-memory governor.

If it merges, later runtime work must preserve its evidence semantics.

### #153

#153 was formed from an older main. Before merge:

1. currentize it against then-current main;
2. resolve overlap with any provider/Connection changes that have landed;
3. rerun exact source tests;
4. run a lived Mac smoke test on a host that can load the model;
5. keep it explicitly selected / not auto-recommended;
6. do not make a "sustained memory safe" claim until #171's cross-model proof exists.

This allows model experimentation to proceed while the shared governor is implemented.

### #171 / #172

The runtime-safety and Settings-awareness work should consume whichever model is selected through the existing
model owner. They must not special-case "current Qwen" as the architecture.

## Reasoning / orientation independence

The gpt-oss swap is also useful as a lived comparison for register/reasoning behavior, but do not collapse:

```text
MODEL BEHAVIOR DIFFERENCE
!= REACH-FRAME EFFECT
!= FOUNDATIONAL-FIELD EFFECT
!= MEMORY-PRESSURE EFFECT
```

When comparing model behavior, record which exact:
- model/profile;
- Foundational Field revision;
- reach pacing/framing;
- Conversation context horizon;
- prompt receipt;
- runtime resource state
were active.

Otherwise "the model changed" becomes an ungrounded explanation.

## Fresh-instance entry

1. re-query current main, #153, #171 and #172;
2. read `HERETIC-PRODUCT-MODEL.md` and current model catalog/runtime owners;
3. keep Harmony parsing and runtime resource policy separate;
4. currentize #153 rather than copying its old-base diff into new source by hand;
5. qualify model selection first;
6. then run the shared long-context/runtime-pressure matrix;
7. report model-specific exceptions only when observed.

Distribution / DMG / website work is owned by another lane and is context-only here.

<!-- [VXG RealForever][VEXLIFE-PROD][CROSS-MODEL-RUNTIME-QUALIFICATION] -->
