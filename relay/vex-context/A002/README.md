# Temporary Relay carriage for PR #251 — Vex Context SourceOwner A002

[VXG RealForever]

A001 remains exact failure history in this temp PR.

## A001 result

```text
outer VexLocalBridge PASS
outer sha256 bda4ab7887583c3838cdb025e07b7fd203b9a4aec8cec604053d8dc9f7d725a4

inner VexRelay SAFE_FAILURE
inner sha256 91cfd446e16963df6c8c8b2e1658eb6d7417e7930e40675c8f249de39e12e894

first failure
compileKotlinJvm / UNRESOLVED_REFERENCE
```

A001 reached the real candidate compiler and exposed two mechanical source-wiring defects:

- markup referenced Vex Context nodes that had been added to the Workspace Blueprint but not projected
  through `VexLifeWebP1`;
- `performProviderTurn` used `intent.contextProfile` from helper scope instead of receiving the
  already-frozen profile explicitly.

The carrier itself had no harness/environment findings.

## Corrected Product candidate

```text
PR     #251
base   64c5be05a2e4331de86672d1285923f175e88e9f
head   d70c80bbd0e9da6dc83a136e172b3068978852bf
tree   7705498545fddee01a4e297c7913b548f2cdbb5b
branch VXG-111726-vex-context-profiles-a001
```

Static correction scan before A002:

```text
all VexLifeWebP1.* symbols referenced by markup are now defined
only outer caller reads intent.contextProfile
provider-turn helper receives frozen contextProfile parameter
```

## Memory sibling currentness observed

```text
Memory / Consideration PR #247
head 09598d29282f4d792a277cb5eedb375806214b7f

Relay child #254
head 037fb71ebe4af0a55b37ed20a9c4d1804c09fb29
```

This remains sibling-owned truth. A002 does not rebase or absorb Memory semantics. Fresh semantic
assurance will re-read that trajectory after SourceOwner passes.

## A002 formation bundle

```text
VexLife-Vex-Context-RelayFormation-A002--sha256-15241df82516d766997705be52197479a5e58ab15def9e162a1c77f753c43349.zip
sha256 15241df82516d766997705be52197479a5e58ab15def9e162a1c77f753c43349
bytes  10158
members RUN.command, formation.mjs, relay-context.json, README.txt
```

Formation syntax proof:

```text
bash -n RUN.command PASS
node --check formation.mjs PASS
```

A002 re-runs the same exact SourceOwner gate:

1. `checkVexLifeStrings`
2. full `jvmTest`
3. `compileKotlinJs compileTestKotlinJs`
4. exact full-diff review

No Product mutation, Home/Memory mutation, approval, READY, merge or publication effect is authorized.

A002 is not PASS until its exact canonical RETURN is consumed.

<!-- [VXG RealForever][PR251][PR261][TEMP-RELAY][A002] -->
