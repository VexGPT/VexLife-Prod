# Whisper Base Multilingual A002 — source-proof repair

[VXG RealForever]

A001 RETURN was consumed exactly.

```text
RETURN sha256
4cc4b0591ba1573f29b8241b47338b80b63ec3b2ce1c50467422e9545089508f

source
54885105bb88ca352fb2bf998eec34802d971d9a

tree
b0faca61cb770aab8509b4801e0e0d09efcc94c8

disposition
FAIL
```

A001 never reached provider/model materialization or lived multilingual inference.

The repository proof reached JS/JVM main compilation, strings, generated VexWeb verification and
service-image assembly, then failed at `compileTestKotlinJvm` because the newly expanded
`VexLifeProdLocalServiceTest` referenced `VexLifeLocalAsrSelection` without importing it.

This is source-proof test compilation failure only. It provides no negative evidence about
`ggml-base.bin` or multilingual inference.

## A002 source

Only the missing proof import was added to Product source:

```text
1413e2795d69099802312228ada535404b429fe6
[Local ASR] Import multilingual selection in service proof
```

The multilingual implementation/model bindings are otherwise unchanged from A001.

## Run

```bash
/bin/bash <(gh api --method GET -H 'Accept: application/vnd.github.raw+json' -f ref=b37f9c0e672e4d61388f6a4694c52308e2efcd6c 'repos/VexGPT/VexLife-Prod/contents/relay/voiceinput-multilingual/A002/RUN-MAC.command')
```

Expected RETURN:

```text
~/Downloads/RETURN-VEXLIFE-VOICEINPUT-MULTILINGUAL-A002.zip
```

A002 still requires both exact Base artifacts READY, explicit Base Multilingual selection, and both
partial + final inference facts carrying `model.local-asr.whisper.base`.

<!-- [VXG RealForever][PR262][MULTILINGUAL][A001-SOURCE-PROOF-FAIL][A002] -->
