# Voice language/output A003 — proof-surface corrected lived proof

[VXG RealForever]

A003 continues stacked source PR #331 after consuming A002.

## A002 result

```text
RETURN sha256  bbff00513d67260e2a72de573a84ffd6dff5634e48d66db29120ab1babb741a8
source         b30d817295dd8cca23941c53b8a43d00572555e2
status         FAIL
stage          jvmTest
tests          755 total / 2 failed
```

A002 successfully passed source/test compilation, string currentness, VexWeb generation/verification
and service-image assembly. It did not reach provider reuse, Product front-door activation, or any
lived language/Voice effect.

The two failures were stale proof surfaces:

1. the committed Screen Lens did not yet include the new
   `VexLifeWorkspaceBlueprint.LocalAsrComposerOutput` coordinate;
2. the generated-runtime test still matched the old
   `voice.localService &&` implementation string even though script-aware voice ranking now filters
   local voices before preference scoring.

No Product runtime behavior changed.

## Exact source

```text
source PR #331
head      1411e4d670e03b1ac144334d04b8c4af46dc0e23
tree      f54bf851099195c3f773fa30fc9d02af5b6525d6
```

## Lived predicate

A003 retains the same feature proof:

- 简体中文 / `zh-Hans`;
- 繁體中文 / `zh-Hant`;
- Follow reply language -> spoken `zh` for both;
- Base Multilingual + As spoken partial/final;
- Base Multilingual + Translate to English partial/final;
- mixed Chinese/English As-spoken Composer human witness;
- English Composer translation from Chinese speech human witness;
- script-aware read-aloud voice preference where matching local voices exist;
- Send remains separate.

No microphone audio or transcript content is retained.

## Run

```bash
/bin/bash <(gh api --method GET -H 'Accept: application/vnd.github.raw+json' -f ref=4d6d7deee48aa3c9b1e6c51b5f754b04eb010596 'repos/VexGPT/VexLife-Prod/contents/relay/voice-language-output/A003/RUN-MAC.command')
```

Expected RETURN:

```text
~/Downloads/RETURN-VEXLIFE-VOICE-LANGUAGE-OUTPUT-A003.zip
```

<!-- [VXG RealForever][PR331][VOICE-LANGUAGE-OUTPUT][TEMP-PROOF][A003] -->
