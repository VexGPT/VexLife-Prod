# PR #550 Follow-up readiness Settings vocabulary — SourceOwner companion

[VXG RealForever]

DO-NOT-MERGE proof carriage for Product PR #550.

## Exact Product candidate

```text
base   a23b6a949d27e4b77530273a390955f0d127ad1a
head   4311baef29d68e44c5f72160d5c9d88fc534a011
tree   8d747501a3b578dde9efa140e4d940dace268de0
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

On PASS: fresh Independent Assurance on exact #550 head. Cache=2 A003 remains a separate lived/provider-performance predicate under #514/#547.

<!-- [VXG RealForever][PR550][SETTINGS-FOLLOWUP-READINESS][SOURCEOWNER-A001][DO-NOT-MERGE] -->
