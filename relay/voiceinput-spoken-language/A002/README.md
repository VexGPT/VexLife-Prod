# Chinese spoken-language coherence A002 — localization-gate repair

[VXG RealForever]

A001 RETURN was consumed exactly.

```text
RETURN sha256
b7bb2ccdfb4c854e41d548f1a0ff0f8fbbb1b9dedf321f4cb3c03521eaa47806

status
FAIL

source
f616571a897693ff4dd325672c7a7c270dbaa323

tree
1c6a9ab7da8192f8d7ccff3231514e0388943fc5

first substantive failure
checkVexLifeStrings:
workspace.option.local-asr-spoken-language-follow
does not follow the repository string-key grammar
```

A001 did not reach provider/model materialization or lived microphone inference.

## Root cause

The spoken-language choice strings used a new `workspace.option.*` element type, but repository
culture only admits the existing element types owned by `VexLifeText.KeyPattern`.

The source repair renames the choice keys to the existing `workspace.label.*` element type and
updates the localization manifest. No spoken-language semantics, model artifact behavior, provider
logic, or lived predicates changed.

Current source:

```text
17b9d4d5859d4037add77be8f9ee829fef223eb1
```

## Run

```bash
/bin/bash <(gh api --method GET -H 'Accept: application/vnd.github.raw+json' -f ref=b548f22e0f52777c7c58eaf5eccb1a2e8e0912f0 'repos/VexGPT/VexLife-Prod/contents/relay/voiceinput-spoken-language/A002/RUN-MAC.command')
```

The lived predicate is unchanged:
- Vex reply language remains English;
- Base Multilingual is selected;
- Spoken language = 中文;
- progressive and final inference must both carry `language=zh`;
- human witness: Composer text remains Chinese and editable.

Expected RETURN:

```text
~/Downloads/RETURN-VEXLIFE-VOICEINPUT-SPOKEN-LANGUAGE-A002.zip
```

<!-- [VXG RealForever][PR273][A001-STRING-GATE-FAIL][A002] -->
