# Voice language/output A005 — source-current effect repaired

[VXG RealForever]

A005 continues stacked source PR #331 after consuming A004.

## A004 result

```text
RETURN sha256  6c775d052a73ab9228e94febe20955054b8cf95539f3e41adb85063a9afd994d
source         c099afdc725bbc0ff16fd3cbc81bdd711c332211
status         FAIL
stage          jvmTest
tests          762 total / 1 failed
```

A004 compiled Product + tests, passed strings/web generation and service-image assembly, then stopped
before provider reuse or any lived Product/Voice effect.

The single failing currentness test exposed a real source defect rather than stale proof:

```text
Composer-output mutation
  raw fetch(...)
  -> no presentation-source SHA header
  -> stale browser could mutate a newer Product service
```

Current source routes that mutation through the same `effectFetch` boundary as every other browser
POST effect. The test now verifies every generated POST effect is source-current rather than relying
only on a hard-coded route count.

## Exact source

```text
source PR #331
head      546cc7a3b088bf2a6350dccc43a6d02a3d74ad19
tree      3e2baf3eb2adbf7e19ca7fdb85c382745a245c47
main      3120a7535f5c9095735c6880f6dd0119ec39a7a8
behind    0
```

## Lived predicates

A005 retains the same product proof:

- 简体中文 / `zh-Hans`;
- 繁體中文 / `zh-Hant`;
- Follow reply language -> spoken `zh`;
- Base Multilingual + As spoken partial/final;
- mixed Chinese/English As-spoken Composer human witness;
- Base Multilingual + Translate to English partial/final;
- English Composer translation from Chinese speech human witness;
- script-aware read-aloud voice preference where matching local voices exist;
- Send remains separate.

No microphone audio or transcript content is retained.

## Run

```bash
/bin/bash <(gh api --method GET -H 'Accept: application/vnd.github.raw+json' -f ref=4fac5d7bdc7f286c8d1f4358986f2295aa51f62f 'repos/VexGPT/VexLife-Prod/contents/relay/voice-language-output/A005/RUN-MAC.command')
```

Expected RETURN:

```text
~/Downloads/RETURN-VEXLIFE-VOICE-LANGUAGE-OUTPUT-A005.zip
```

<!-- [VXG RealForever][PR331][VOICE-LANGUAGE-OUTPUT][SOURCE-CURRENT][TEMP-PROOF][A005] -->
