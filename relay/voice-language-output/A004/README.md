# Voice language/output A004 — merged-main realigned lived proof

[VXG RealForever]

A004 supersedes the formed-but-unexecuted A003 because VoiceInput PR #310 merged into `main`
before A003 was run.

## Current foundation

```text
main         3120a7535f5c9095735c6880f6dd0119ec39a7a8
#310         MERGED -> main at 3120a7535f5c9095735c6880f6dd0119ec39a7a8
#331 head    c099afdc725bbc0ff16fd3cbc81bdd711c332211
#331 tree    94d6dcb7de53cbcf45151375a4734cce6c2455b0
#331 behind  0
```

The #331 realignment was formed as a two-parent merge over current main. Its diff against main remains
only the intended language/output feature. Six shared service/browser/test files had zero same-line
conflicts; current Source Return/turn-context work and the earned persistent Voice circuit are both
retained.

## Prior proof history

```text
A001  FAIL  compileTestKotlinJvm: two missing test imports
A002  FAIL  jvmTest: 755 tests / 2 stale proof-presentation assertions
A003  FORMED_NOT_EXECUTED
      superseded when #310 merged into main
A004  current exact-source run
```

A002's two proof-surface failures were already corrected before this realignment:
- committed Screen Lens includes LocalAsrComposerOutput;
- script-aware local speech-voice test matches the current filter/ranking contract.

## Lived predicates

A004 preserves the same human/product outcome:

- `简体中文 / zh-Hans` and `繁體中文 / zh-Hant`;
- Follow Vex reply language maps both reply scripts to spoken `zh`;
- Base Multilingual + **As spoken** partial/final;
- mixed Chinese/English As-spoken Composer human witness;
- Base Multilingual + **Translate to English** partial/final;
- English Composer translation from Chinese speech human witness;
- script-aware read-aloud voice preference where matching local voices exist;
- Send remains separate.

The exact A004-qualified persistent Whisper provider/model bytes are reused. No microphone audio or
transcript content is retained.

## Run

```bash
/bin/bash <(gh api --method GET -H 'Accept: application/vnd.github.raw+json' -f ref=3c40495fccabebe97455bc2ae85f10528f26375a 'repos/VexGPT/VexLife-Prod/contents/relay/voice-language-output/A004/RUN-MAC.command')
```

Expected RETURN:

```text
~/Downloads/RETURN-VEXLIFE-VOICE-LANGUAGE-OUTPUT-A004.zip
```

<!-- [VXG RealForever][PR331][MERGED-MAIN][VOICE-LANGUAGE-OUTPUT][TEMP-PROOF][A004] -->
