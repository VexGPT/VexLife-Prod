# Chinese spoken-language coherence A004 — exact Screen Lens ordering repair

[VXG RealForever]

A003 RETURN was consumed exactly.

```text
RETURN sha256
a9c7d9c745b880a38c51f5c354964077243116af0af41d55bc1311c424841229

status
FAIL

source
ad16915366fd0503f1c02bdac5ce715e329fc104

tree
a3ea31db324f882e87349a38e40e569f7068194e
```

A003 passed:
- JS main/test compilation;
- JVM main/test compilation;
- localization currentness;
- generated VexWeb verification;
- service-image assembly;
- 608 / 609 JVM tests.

The only failing test was `VexLifeProdScreenLensTest.committedLensIsCurrent`.

## Root cause

The committed Screen Lens contained the new Local-ASR coordinates in this order:

```text
selection
models-list
spoken-language
storage-note
status
```

but the Product Blueprint/VexWeb generator owns this order:

```text
selection
models-list
spoken-language
status
storage-note
```

The committed JSON was therefore semantically complete but not byte-current with the generator.

A004 changes only that committed ordering. No runtime, model, spoken-language, download, or dictation
behavior changed.

## Exact A004 source

```text
de6a23b763bbf6a18489c5b84f04f59a8938e285
```

## Run

```bash
/bin/bash <(gh api --method GET -H 'Accept: application/vnd.github.raw+json' -f ref=ebd7afda50920f4b868bbab30290e3fca002c8bd 'repos/VexGPT/VexLife-Prod/contents/relay/voiceinput-spoken-language/A004/RUN-MAC.command')
```

The lived predicate is unchanged:
- Vex reply language remains English;
- Base Multilingual is selected;
- Spoken language = 中文;
- progressive and final inference both carry `language=zh`;
- human witness: progressive/final Composer text remains Chinese and editable.

Expected RETURN:

```text
~/Downloads/RETURN-VEXLIFE-VOICEINPUT-SPOKEN-LANGUAGE-A004.zip
```

<!-- [VXG RealForever][PR273][A003-SCREEN-LENS-ORDER-FAIL][A004] -->
