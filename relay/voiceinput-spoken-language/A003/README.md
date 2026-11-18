# Chinese spoken-language coherence A003 — VexWeb proof-currentness repair

[VXG RealForever]

A002 RETURN was consumed exactly.

```text
RETURN sha256
0a589eb3db35171b78b3165078c6fe509dccec08c8d39570833b18687c5d951b

status
FAIL

source
17b9d4d5859d4037add77be8f9ee829fef223eb1

tree
72c7362666feb505da0862c62e5eb67a3331de45
```

A002 progressed materially farther than A001:
- JS main/test compilation PASS;
- JVM main/test compilation PASS;
- localization currentness PASS;
- generated VexWeb PASS;
- service-image assembly PASS;
- 605 / 609 JVM tests PASS.

The four failing tests were source-evolution proof drift from adding three Settings coordinates:
`models-list`, `spoken-language`, and `storage-note`.

Repairs:
- workspace/P1 coordinate counts advance 57 -> 60;
- runtime test now guards dynamically projected Download/Redownload Controls at the projection loop
  rather than expecting hard-coded JS literals;
- committed VexLife-Prod Screen Lens regenerated to 60 elements.

No spoken-language, Whisper, model-download, or progressive-dictation behavior changed.

## Exact A003 source

```text
ad16915366fd0503f1c02bdac5ce715e329fc104
```

## Run

```bash
/bin/bash <(gh api --method GET -H 'Accept: application/vnd.github.raw+json' -f ref=d1cbdd478c675f9de3e37757db509790ecbc8950 'repos/VexGPT/VexLife-Prod/contents/relay/voiceinput-spoken-language/A003/RUN-MAC.command')
```

The lived predicate is unchanged:
- Vex reply language remains English;
- Base Multilingual is selected;
- Spoken language = 中文;
- progressive and final inference both carry `language=zh`;
- human witness: progressive/final Composer text remains Chinese and editable.

Expected RETURN:

```text
~/Downloads/RETURN-VEXLIFE-VOICEINPUT-SPOKEN-LANGUAGE-A003.zip
```

<!-- [VXG RealForever][PR273][A002-PROOF-DRIFT-FAIL][A003] -->
