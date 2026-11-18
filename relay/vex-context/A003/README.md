# Temporary Relay carriage for PR #251 — Vex Context SourceOwner A003

[VXG RealForever]

A001 and A002 remain immutable attempt history in this temp PR.

## A002 result

```text
outer VexLocalBridge PASS
outer sha256 8b75e9d6ec554cf56964e7c0f484c168e6ebf1459011d242bb5c361cfd76b555

inner VexRelay SAFE_FAILURE
inner sha256 bd90a3aa8adf704eeb1ae6bf1ff8e309da99dd399e9158be105d7df281840e90

checkVexLifeStrings PASS
compileKotlinJvm PASS
full jvmTest reached
646 tests / 3 failed
```

The three failures were source-owned proof/projection currentness, not Relay failure:

- Foundational Field behavior still failed before final inference, but its assertion vocabulary drifted.
- VexWeb proof still expected 59 Blueprint coordinates instead of the newly earned 70.
- the committed Product Screen Lens was stale after the Blueprint changed.

## Corrected Product candidate

```text
PR     #251
base   64c5be05a2e4331de86672d1285923f175e88e9f
head   d76cc1f212d2aed5ac7a1f3241c6bd18d7205ceb
tree   7cef99e8df38976dfdfcd7537332b34d43c943dd
branch VXG-111726-vex-context-profiles-a001
paths  21
```

Correction:

```text
Foundational currentness failure keeps prior recognizable wording while also naming Context Profile.
VexLifeWebP1Test recognizes 70 exact Blueprint coordinates.
docs/lens/vexlife-prod.screen-lens.json regenerated to those 70 exact coordinates.
```

## Memory sibling currentness observed

```text
Memory / Consideration PR #247
head 41473ca787b17bb143d50fb79da565d256856335

Relay child #254
head 037fb71ebe4af0a55b37ed20a9c4d1804c09fb29
```

Memory is still moving independently. A003 does not absorb or freeze those semantics. Fresh semantic
assurance will re-read that sibling trajectory if SourceOwner passes.

## A003 formation bundle

```text
VexLife-Vex-Context-RelayFormation-A003--sha256-e9ac7651888141dd98b131db6f127f79b9e6c4ec8c66672380c57de646706df3.zip
sha256 e9ac7651888141dd98b131db6f127f79b9e6c4ec8c66672380c57de646706df3
bytes  10781
members RUN.command, formation.mjs, relay-context.json, README.txt
```

Formation syntax:

```text
bash -n RUN.command PASS
node --check formation.mjs PASS
```

A003 runs:

1. `checkVexLifeStrings`
2. full `jvmTest`
3. `compileKotlinJs compileTestKotlinJs`
4. exact full-diff SourceOwner review over the 21-path candidate

The current Relay contract may select at most 10 complete source bodies into the RETURN; its review
index and full diff still account for all 21 paths.

No Product mutation, Home/Memory mutation, approval, READY, merge or publication is authorized.

A003 is not PASS until its exact canonical RETURN is consumed.

<!-- [VXG RealForever][PR251][PR261][TEMP-RELAY][A003] -->
