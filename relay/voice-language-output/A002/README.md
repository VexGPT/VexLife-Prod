# Voice language/output A002 — compile-test corrected lived proof

[VXG RealForever]

A002 continues stacked source PR #331 after consuming A001.

## A001 result

```text
RETURN sha256  beb184d16078f690c3c965ed5a2d6b7c3382189d2d86a33fe79e50f5d36efce9
source         91f1370cd89813de3c94afc8f4849237bad74f3a
status         FAIL
stage          compileTestKotlinJvm
```

A001 did **not** reach provider reuse, Product front-door activation, or any lived language/Voice
effect.

The Product main source compiled; strings, generated web verification and service-image assembly all
passed. JVM test compilation failed only because the service test used the new
`VexLifeLocalAsrComposerOutput` and `VexLifeLocalAsrModelsFamily` owners without importing them.

A002 adds those two imports and otherwise preserves the feature semantics.

## Current source stack

```text
main  40f603dbb483ffff7673faf9a0f89e0a6929875c
#310  c34c446a6fa9331d3afa2f660a3603c07a8cb8f9
#331  b30d817295dd8cca23941c53b8a43d00572555e2
tree  d9cac09f1a3a661cc4afae8acd68a6d2ffba43ba
```

Current main's intervening work is setup localization only. It has been absorbed into #310 and the
#331 stack without changing Voice semantics.

## Lived predicate

A002 retains A001's exact feature proof:

- both `简体中文 / zh-Hans` and `繁體中文 / zh-Hant` Vex reply choices;
- Follow Vex reply language resolves either script to spoken `zh`;
- Base Multilingual + As spoken partial/final;
- Base Multilingual + Translate to English partial/final;
- mixed Chinese/English As-spoken Composer human witness;
- English Composer translation from Chinese speech human witness;
- script-aware read-aloud voice preference where matching local voices exist;
- Send remains separate.

No microphone audio or transcript content is retained.

## Run

```bash
/bin/bash <(gh api --method GET -H 'Accept: application/vnd.github.raw+json' -f ref=fc1f5224c0becd2c32dbea6a0dd020dccd799095 'repos/VexGPT/VexLife-Prod/contents/relay/voice-language-output/A002/RUN-MAC.command')
```

Expected RETURN:

```text
~/Downloads/RETURN-VEXLIFE-VOICE-LANGUAGE-OUTPUT-A002.zip
```

<!-- [VXG RealForever][PR331][VOICE-LANGUAGE-OUTPUT][TEMP-PROOF][A002] -->
