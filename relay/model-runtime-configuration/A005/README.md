[VXG RealForever]

# PR #238 model/runtime configuration host-proof carriage — A005

This companion coordinate preserves the proof history for Product Draft PR #238 without adding
Product behavior.

## Exact Product candidate

```text
base 47decbf10bdb53a33afc21affc1080bcf2b125fd
head 2f5183ee75bb435559a9e2f5264f7cacf4d430b6
tree e3e06ba608910188260fbd88b48c950c31300984
Product PR #238
```

A005 proves the successor after Victor's lived UI review.

## What changed after the previous lived build

The Product UI now uses one model-selection workflow:

```text
current serving / staged target
-> dropdown + lawful Download / Redownload / Remove for that target
-> compact selected-model facts
-> selected-model configuration
-> secondary: Use Recommended Configuration
-> primary: Start Selected Model / Apply & Restart Selected Model
```

The old large per-model card renderer is removed. Runtime activation is visually and semantically
distinct from selection/configuration.

## Cache-3 lived standing

Victor's lived debug export:

```text
sha256 cce7bb6edbcc97ed8944c493066bf0a075fb1cb23efbfbbf98d14194d21c0641
Product source 3d3900ea2c58564252d43507751344d27f2d08dc
4B runtime --prompt-cache-size 3
runtime PID 19907
```

The exact runtime started and stayed alive, but one provider invocation remained open for 7+ minutes
while Victor observed repetitive output. Enhanced snapshots also showed host free memory dipping to
about 2.2 GiB.

That does not prove cache=3 caused the generation loop. It does hold 3 from being presented as the
recommended ordinary setting.

Current Product relationship:

```text
4B recommended = 1
4B admitted experimental range = 1..3
2..3 = held experiments pending focused proof
```

No anti-repetition detector is added by A005; that remains a separate runtime-quality edge so this
UI/configuration successor does not silently change model-output semantics.

## A005 formation

```text
VexLife-Model-Runtime-Configuration-RelayFormation-A005
sha256 897982f6c58a7c596ed124c8df29c724e0f4ae8a86f6e632becd22d5aa8df1b2
```

The formation binds the exact Product head/tree above and runs:
- full JVM suite;
- Kotlin/JS main + test compilation;
- string currentness;
- VexWeb generation;
- source-owned Mac app/DMG formation;
- bounded current Relay review snapshot (maxFiles=10, complete changed-path binding retained).

No Home/model/runtime/configuration, approval, READY, merge or release effect is admitted.

<!-- [VXG RealForever][VEXLIFE-PROD][PR238][HOST-PROOF][A005] -->
