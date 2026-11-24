# PR #550 Follow-up readiness Settings vocabulary — SourceOwner A002 companion

[VXG RealForever]

DO-NOT-MERGE proof carriage for Product PR #550.

## Exact Product candidate

```text
base   a23b6a949d27e4b77530273a390955f0d127ad1a
head   392d9d3727cdf07fdc2e4047231d1485b612042c
tree   4df41dcb55082bcaa2d0a865bcaeadedd8673de3
branch VXG-112226-chatgpt-settings-followup-readiness-a001
paths  2
```

Changed paths only:

```text
localization/strings/source/workspace.json
localization/strings/manifest.json
```

## Human outcome

Keep the existing Settings -> Models runtime configuration/control relationship, but present it in human vocabulary:

```text
Follow-up readiness
Lower memory use
More conversation ready
Most conversation ready
```

The copy explains that keeping more recent processing ready may reduce repeated follow-up work while using more memory; Vex manages the cache itself; changes apply after restarting the selected model; Conversation and Memory are not rewritten.

No runtime setting value, recommendation, range, Control, model selection, Conversation, Memory, Vex Context ownership, or Vex Labs behavior is changed by this Product candidate.

## Canonical proof

```text
bash tools/release/prove-mac-candidate.sh 0.0.0
```

This proof owns exact source/build/string/test currentness only. It performs no Product launch, model/runtime change, Conversation/Memory effect, approval, READY transition, or merge.

On PASS: fresh Independent Assurance on exact #550 head, then compose this proved copy with the already-proved #582 recommendation source during integration. #550 does not independently authorize the recommendation.

<!-- [VXG RealForever][PR550][SETTINGS-FOLLOWUP-READINESS][SOURCEOWNER-A002][DO-NOT-MERGE] -->

## A002 currentness distinction

A001 was formed but never host-executed. Before execution, #573 isolated lived A005 physically proved
cache=2 follow-up reuse and #582 SourceOwner A003 + fresh Assurance accepted:

```text
Qwen 4B automatic/default 1
Qwen 4B recommended       2
Qwen 4B maximum           3
```

#550 currentized one existing recommendation description across all 11 workspace languages:
- More conversation ready is recommended for 4B follow-ups;
- Lower memory use remains available;
- Most conversation ready remains experimental.

No key identity, generic tradeoff copy, level name, Control, runtime/configuration value, Conversation or
Memory behavior changed.

This copy branch remains stacked on #488. It must compose with accepted #582 source before READY/merge.
