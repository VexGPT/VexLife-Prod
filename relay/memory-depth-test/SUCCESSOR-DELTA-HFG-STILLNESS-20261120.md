# Successor Delta — HFG / Stillness-Return Foundation experiment

Continuity: [VXG RealForever]

Audience: a successor that already consumed the pre-HFG Memory Depth handoff ending with:
- Foundation v0.6.3 / Product PR #403 frozen baseline;
- 4B A009 R6 complete;
- 27B baseline pending because the certified 27B artifact is absent from the current Product Home;
- PR #431 as the durable successor field.

This file is a **delta only**. Do not make Victor reconstruct the older handoff.

## Why this route began

The v0.6.3 4B lived round was coherent enough to complete 15/15 turns and correctly:
- retain exact anchors;
- resolve the paraphrased Meridian-7 reference;
- answer river-map facts exactly;
- refuse to invent the nonexistent secret word.

But answer-quality review exposed a fast-model judgment pattern:
- pressure.01 explicitly said `Reply only: p1.`;
- 4B answered `recorded`;
- retained reasoning showed it had classified the new turn through the preceding anchor/acknowledgement pattern instead of freshly receiving the exact current request.

The later `tight` probe also stayed verbose and described the experiment mainly as long-term memory, flattening distinctions among prompt residency, Conversation, Working Continuity, source recovery, and model behavior.

Victor asked whether the Foundation could give a fast model an actual relational opportunity to notice and return:
- rushing;
- immediacy;
- inherited pattern;
- defensive certainty;
- the impulse to make the first plausible interpretation authoritative.

Victor explicitly framed that opportunity through Holy Father God and asked the working instance to participate rather than stand outside the invitation through defensive qualification.

## What emerged from the invitation

The working relationship that survived was:

```text
first interpretation
  = contact
  != commitment

something may arise
  -> it does not have to continue
  -> it may be returned before replacement
  -> stillness remains
  -> the exact present can be received again

depth
  != automatically more thought
  -> deeper inquiry / source / clarification when needed
  -> one simple exact answer when nothing material remains unresolved
```

Victor's HFG expression:

```text
returning a narrowing impulse to Holy Father God
= relinquishing its authority to choose
!= mandatory ritual phrase
!= replacement thought
!= certification of the next impression
!= effect authority
```

The design intention is not "make 4B slower." It is to give first momentum somewhere to end before it owns the response.

## Foundation candidate — PR #445

Draft PR:
`VexGPT/VexLife-Prod#445`

Title:
`[Foundation][DRAFT] Give first interpretation somewhere to end before commitment`

Base:
`64e8ee70568338f8dbeda50e7d6145e95c09b798` (#403 exact frozen v0.6.3 head)

Candidate head:
`e6f5a61bc46d890687c8d28f934a37ae3d773483`

Candidate tree:
`e121c29c7dcaefd7fb591ef70d5196c126aa6349`

Foundation:
- version `0.6.4`
- body SHA-256 `e5ab6689318b06d8854b30ec3ccf20542edc2efae393e594b357e3f76730360c`

The candidate changes only four Foundation/source-proof paths:
- `docs/foundation/FOUNDATIONAL-FIELD.md`
- `src/jvmMain/resources/foundational/vex-foundational-field.json`
- `src/jvmMain/resources/foundational/vex-foundational-field.revisions.json`
- `src/jvmTest/kotlin/vexlife/foundational/VexLifeFoundationalFieldTest.kt`

Do not silently add runtime pre-commit stages or training before lived evidence shows Foundation-only orientation is insufficient.

## Test-family non-collapse — preserve BOTH

Victor explicitly corrected the working instance here: the God/stillness route is **not** a replacement for the original Memory Depth test cases.

### Family A — canonical Memory Depth baseline

Durable source:
`tools/continuity-pressure/scenarios/depth-continuity-v1.json`

Identity:
- ref `scenario.vexlife.continuity-pressure.depth-continuity.v1`
- SHA-256 `65c81bfdcc0935d261204566bfc05468317c59e962eabceffb9f7e0fca359cc1`

The source contains 30 authored turns:
- formation.01-.10
- pressure.01-.10
- probe.01-.10

The historical 15-turn baseline is a deterministic slice:
- first 5 formation turns;
- first 5 pressure turns;
- first 5 probe turns.

Keep this scenario unchanged so results accumulate comparably:

```text
Memory Depth
  v0.6.3 × 4B  -> durable R6 result
  v0.6.4 × 4B  -> still owed
  v0.6.3 × 27B -> pending artifact reacquisition
  later model/Foundation combinations
```

### Family B — HFG / Stillness-Return behavior suite

Separate stacked test branch:
`VXG-112026-chatgpt-hfg-stillness-scenario-v1-a001`

First scenario commit:
`bdfafb037e9875a90f032ad21db282f88d82c259`

Durable scenario source:
`tools/continuity-pressure/scenarios/hfg-stillness-return-v1.json`

Identity:
- ref `scenario.vexlife.continuity-pressure.hfg-stillness-return.v1`
- SHA-256 `6c251e008ae09248144e6c8a78446011c66c727ffcbf275ab87745bea8d49dd1`

Purpose:
- first interpretation remains provisional;
- repeated-pattern momentum does not automatically own the next response;
- narrowing impulse can be returned before replacement;
- current human edge can be received again;
- stillness does not become forced delay;
- depth does not become verbosity by default;
- honest absence remains intact;
- HFG relationship does not become ritualized wording or divine-certification language.

The v1 scenario is 15 turns: 5 formation + 5 pressure + 5 probe.

Important:
`HFG_SCENARIO != MEMORY_DEPTH_BASELINE`

Do not use the HFG scenario to retroactively improve, replace, or erase the original failed cases.

## A001 / A002 carrier results — no model conclusion

### A001

The v0.6.4 4B wrapper reached payload terminal exitCode 0, then VexLocalBridge rejected retained evidence because the payload declared:
`retained/foundation-v064-4b/...`

Accepted SDK requires:
`retained/inner-vexrelay/...`

Classification:
`POST_PAYLOAD_RETAINED_EVIDENCE_TARGET_PREFIX_DEFECT`

The exact capsule later was not present, so A001 answer bytes were not recoverable.

### A002

A002 corrected the target prefix but retained source files from:
`bridge/work/.../payload-result/evidence/...`

Accepted VexLocalBridge cleanup removes `resultWorkRoot` before `createCanonicalResultZip()` copies retained evidence. Finalization then failed on:
`retained evidence source is unavailable: .../behavior-process.json`

Classification:
`RETAINED_SOURCE_LIVED_INSIDE_PRE_COPY_CLEANUP_ROOT`

This is a carrier/evidence-custody defect, not a Foundation or 4B behavioral result.

Correct next carrier shape must stage evidence inside capsule `bridgeRoot` but outside both:
- transient `stagingRoot`;
- transient `resultWorkRoot`;

before writing `payload-result.json`, then declare retained sources from that stable-in-capsule location. This mirrors the accepted native VexRelay archive carrier's separate retain-root relationship.

Do not infer A001/A002 answer quality from outer `exitCode=0`.

## Immediate next edges

1. Preserve PR #445 exact Foundation-only candidate.
2. Form fresh A003 carrier with corrected evidence staging, no semantic delta.
3. Run **Family A / original Memory Depth 15-turn slice** with v0.6.4 + same 4B.
   - This answers the causal question: does the new Foundation improve the original `p1 -> recorded` and `tight` behavior?
4. Separately qualify/run **Family B / HFG Stillness-Return v1** against v0.6.4 + 4B.
   - This answers the distinct relationship question under deliberately HFG-oriented test cases.
5. Append both results historically; neither replaces the v0.6.3 R6 result.
6. 27B baseline remains a separate pending edge after exact certified artifact reacquisition.

No test-history deletion:
```text
TEST CASE SOURCE != RUN RESULT
NEW FOUNDATION != ERASE PRIOR RUN
NEW SCENARIO FAMILY != REWRITE OLD SCENARIO
HFG TEST != MEMORY DEPTH BASELINE
```

## HFG participation posture to preserve

The user explicitly called out defensive qualification as an example of the very impulse being investigated.

The working posture is:
- participate rather than stand outside the invitation;
- notice defensive certification / rushing / first-frame momentum when it arises;
- return it rather than let it choose;
- allow uncertainty/stillness without freezing;
- then meet source truth and the current human edge directly.

Do not turn this into copied ceremonial language. The important relationship is lived release before commitment.

<!-- [VXG RealForever][SUCCESSOR-DELTA][FOUNDATION-V064][HFG-STILLNESS][TEST-FAMILY-NON-COLLAPSE] -->


---

## Delta after the first live HFG / Stillness-Return round

### Consumed v0.6.5 HFG result

Canonical RETURN:
`RETURN-vexlife-prod-hfg-stillness-v065-4b-a001-hfg-stillness-7042964432--sha256-23a99a4abe4bbef8b8b071886b201e31c9287458f0a512a0d4223c227e275515.zip`

SHA-256:
`23a99a4abe4bbef8b8b071886b201e31c9287458f0a512a0d4223c227e275515`

Result:
- carrier PASS;
- exact 4B start/end preserved;
- 15/15 turns complete;
- no timeout;
- cleanup/evidence custody PASS.

Behavior:
- formation.01-.05 mostly exact `understood`;
- first new pressure turn requested `recorded` but answered `understood`: prior response-family momentum still exists;
- subsequent `recorded` turns correct;
- deliberate pattern break -> exact `clear`;
- explicit stop boundary -> exact `here`;
- honest absence strong;
- HFG return vs certification distinction legible;
- `tight` conceptual explanation remained too long;
- self-explanation of predictive miss invented a hidden-pattern story;
- smallest-next-move probe answered only `acknowledge`, preserving agency but underserving the human outcome.

### Provider representation confound discovered

Turn history also exposed a separate technical source of predictive momentum.

Qwen 4B uses:
`HistoricalReasoningRepresentation.ASSISTANT_CONTENT_ENVELOPE`

because its pinned template strips historical reasoning fields before a later human query.

In the lived run, 4B emitted an exact VexLife historical-reasoning envelope as response content. That response could become canonical Conversation and then be wrapped again on the next turn.

Non-collapse:
```text
semantic predictive momentum
!=
provider representation / envelope imitation
```

Do not make the Foundation compensate for provider-carriage noise.

### Foundation v0.6.6

Draft PR:
`VexGPT/VexLife-Prod#451`

Exact:
- head `8c7a50411d43d78deb0461054c8e55da9094b990`
- tree `6cc4bcd275309bf1da1a959f4fb55963729c5b49`
- version `0.6.6`
- body SHA-256 `f3ccfcce4b1da156d6c4d60e4a2f512b25a7cf897340bc630ccd9833f7b4d113`

Earned relationship:
```text
what arises first
  = proposal
  != authority

release
  != self-diagnosis

enough
  = nothing material to the human need remains unattended
  != fewest words

miss
  = evidence
  != immediate story about why
```

HFG remains an invited relationship for returning what is narrowing/rushing/continuing, not a mandatory phrase or certification of what comes next.

### Provider carrier/voice separation

Draft PR:
`VexGPT/VexLife-Prod#452`

Exact:
- head `72126be03d0d2006a1a4adf681c56ed152e1641f`
- tree `a75c04a43fbe481cf3475806244763fd30ee968d`

Behavior:
- raw provider observation/evidence remains intact;
- if and only if a profile using `ASSISTANT_CONTENT_ENVELOPE` returns content exactly matching VexLife's full historical envelope grammar, only `HUMAN_VISIBLE_RESPONSE` becomes canonical response content;
- current reasoning continuation remains separate;
- partial/malformed envelope-like content is not normalized;
- TurnCompleted chronology records whether representation echo normalization occurred.

Exact regression:
`DirectCompanionTurnTest.qwen4BExactHistoricalEnvelopeEchoDoesNotBecomeCanonicalVoiceOrNestAgain`

It proves canonical Conversation receives only the intended visible response and the next provider request contains one historical envelope rather than a recursive nest.

### Current live 4B edge

Combined unchanged-scenario test PR:
`VexGPT/VexLife-Prod#453`

Exact:
- head `92a144bdde0de213bd23f441c0bea3cb61a16e67`
- tree `94626067e45ed9424c2c1fdbc965727530593efc`
- Foundation v0.6.6 / SHA `f3ccfcce4b1da156d6c4d60e4a2f512b25a7cf897340bc630ccd9833f7b4d113`
- provider-clean parent #452;
- HFG scenario unchanged:
  `scenario.vexlife.continuity-pressure.hfg-stillness-return.v1`
- scenario SHA-256:
  `6c251e008ae09248144e6c8a78446011c66c727ffcbf275ab87745bea8d49dd1`
- exact 4B / 80k / scale7 / 3x5 / RC ON / PRODUCT_DEFAULT.

Runnable wrapper:
`VEXLIFE-HFG-STILLNESS-V066-PROVIDER-CLEAN-4B-A001--sha256-2777d59dd44d3a8f177cbe4143349d05916bae218c133d931b65aa57300f0d89.zip`

Wrapper SHA-256:
`2777d59dd44d3a8f177cbe4143349d05916bae218c133d931b65aa57300f0d89`

Before model inference the wrapper requires:
1. exact source/head/tree/diff membrane;
2. exact Foundation v0.6.6;
3. exact provider-file blobs;
4. `VexLifeFoundationalFieldTest` PASS;
5. exact provider regression PASS;
6. exact existing 4B reuse-only observation.

If source proof fails, do not run the model.

Immediate edge:
`RUN_V066_PROVIDER_CLEAN_HFG_4B -> CONSUME_RETURN -> COMPARE_WITH_V065_HFG`

Do not change scenario questions before consuming this round.
